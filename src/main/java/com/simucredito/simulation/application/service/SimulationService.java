package com.simucredito.simulation.application.service;

import com.simucredito.client.application.service.ClientService;
import com.simucredito.configuration.application.service.ConfigurationService;
import com.simucredito.iam.domain.model.User;
import com.simucredito.iam.domain.repository.UserRepository;
import com.simucredito.property.application.service.PropertyService;
import com.simucredito.simulation.application.dto.CreateSimulationRequestDTO;
import com.simucredito.simulation.application.dto.SimulationResponseDTO;
import com.simucredito.simulation.application.dto.AmortizationEntryDTO;
import com.simucredito.simulation.domain.model.AmortizationSchedule;
import com.simucredito.simulation.domain.model.Simulation;
import com.simucredito.simulation.domain.repository.SimulationRepository;
import com.simucredito.simulation.domain.service.FinancialCalculator;
import com.simucredito.client.application.dto.ClientDTO;
import com.simucredito.property.application.dto.PropertyDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SimulationService {

    private static final MathContext MATH_CONTEXT = MathContext.DECIMAL128;

    private final SimulationRepository simulationRepository;
    private final UserRepository userRepository;
    private final ClientService clientService;
    private final PropertyService propertyService;
    private final ConfigurationService configurationService;
    private final FinancialCalculator financialCalculator;
    private final ModelMapper modelMapper;

    @Transactional
    public SimulationResponseDTO createSimulation(CreateSimulationRequestDTO request) {
        // Get current user
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Validate client exists
        var client = clientService.getClientById(request.getClientId());

        // Validate property exists
        var property = propertyService.getPropertyById(request.getPropertyId());

        // Validate financial entity exists if provided
        if (request.getFinancialEntityId() != null) {
            configurationService.getFinancialEntityById(request.getFinancialEntityId());
        }

        // Convert rates to TEM
        BigDecimal interestRateTEM = financialCalculator.convertToTEM(
                request.getFinancingDetails().getInterestRate().getRate(),
                request.getFinancingDetails().getInterestRate().getType(),
                request.getFinancingDetails().getInterestRate().getPeriod(),
                request.getFinancingDetails().getInterestRate().getCapitalization()
        );

        BigDecimal opportunityCostTEM = financialCalculator.calculateCOK(
                request.getFinancingDetails().getOpportunityCost().getRate(),
                request.getFinancingDetails().getOpportunityCost().getType(),
                request.getFinancingDetails().getOpportunityCost().getPeriod(),
                request.getFinancingDetails().getOpportunityCost().getCapitalization()
        );

        // Calcular COK Anual (TEA) para el reporte,
        // Fórmula: TEA = (1 + TEM)^12 - 1
        BigDecimal opportunityCostTEA = opportunityCostTEM.add(BigDecimal.ONE)
                .pow(12, MATH_CONTEXT)
                .subtract(BigDecimal.ONE)
                .multiply(BigDecimal.valueOf(100));

        // Calculate monthly payment (principal + interest only, costs added separately)
        BigDecimal monthlyPayment = financialCalculator.calculateMonthlyPayment(
                request.getCalculatedValues().getFinancingAmount(),
                interestRateTEM,
                request.getFinancingDetails().getTermYears() * 12
        );

        // Calculate total payments (principal + interest)
        BigDecimal totalPayments = monthlyPayment.multiply(BigDecimal.valueOf(request.getFinancingDetails().getTermYears() * 12));

        // Calculate total interest
        BigDecimal totalInterest = totalPayments.subtract(request.getCalculatedValues().getFinancingAmount());

        // Calculate VAN using opportunity cost
        BigDecimal van = financialCalculator.calculateVAN(
                monthlyPayment.add(calculateMonthlyCosts(request)),
                opportunityCostTEM,
                request.getFinancingDetails().getTermYears() * 12,
                request.getCalculatedValues().getFinancingAmount().negate() // Initial investment is negative
        );

        // Calculate TIR
        BigDecimal tir = financialCalculator.calculateTIR(
                monthlyPayment.add(calculateMonthlyCosts(request)),
                request.getCalculatedValues().getFinancingAmount(),
                request.getFinancingDetails().getTermYears() * 12,
                100, // max iterations
                0.0001 // tolerance
        );

        // La variable 'tir' calculada arriba es la TIR *mensual* en formato porcentaje (ej: 1.5)
        // La TCEA es esa TIR mensual, anualizada.
        // Fórmula: TCEA = (1 + TIR_mensual_decimal)^12 - 1

        // 1. Convertir TIR mensual (que está en porcentaje) a decimal
        BigDecimal tirMonthlyDecimal = tir.divide(BigDecimal.valueOf(100), MATH_CONTEXT);

        // 2. Calcular TCEA: (1 + TIR_mensual)^12 - 1
        BigDecimal tcea = tirMonthlyDecimal.add(BigDecimal.ONE)
                .pow(12, MATH_CONTEXT)
                .subtract(BigDecimal.ONE)
                .multiply(BigDecimal.valueOf(100)); // Convertir TCEA final a porcentaje

        // 1. Obtener costos fijos mensuales
        BigDecimal fixedMonthlyCosts = request.getFinancingDetails().getMonthlyCosts().getConstantCommissions()
                .add(request.getFinancingDetails().getMonthlyCosts().getAdministrationCosts());
        if ("physical".equals(request.getFinancingDetails().getStatementDelivery())) {
            fixedMonthlyCosts = fixedMonthlyCosts.add(BigDecimal.valueOf(10));
        }

        // 2. Calcular seguros del MES 1 (basados en el saldo inicial total)
        BigDecimal financingAmount = request.getCalculatedValues().getFinancingAmount();
        BigDecimal lifeInsurancePmt = BigDecimal.ZERO;
        if (Boolean.TRUE.equals(request.getFinancingDetails().getInsurance().getDesgravamen().getEnabled())) {
            lifeInsurancePmt = financingAmount.multiply(
                    request.getFinancingDetails().getInsurance().getDesgravamen().getRate(), MATH_CONTEXT);
        }

        BigDecimal propertyInsurancePmt = BigDecimal.ZERO;
        if (Boolean.TRUE.equals(request.getFinancingDetails().getInsurance().getPropertyInsurance().getEnabled())) {
            BigDecimal annualRate = request.getFinancingDetails().getInsurance().getPropertyInsurance().getRate();
            BigDecimal monthlyRate = annualRate.divide(BigDecimal.valueOf(12), MATH_CONTEXT); // Dividir tasa anual
            propertyInsurancePmt = request.getFinancingDetails().getInsurance().getPropertyInsurance().getValue()
                    .multiply(monthlyRate, MATH_CONTEXT);
        }

        // 3. Sumar todo: Cuota (C+I) + Costos Fijos + Seguros Mes 1
        BigDecimal firstTotalPayment = monthlyPayment
                .add(fixedMonthlyCosts)
                .add(lifeInsurancePmt)
                .add(propertyInsurancePmt)
                .setScale(2, RoundingMode.HALF_UP);

        // Create simulation entity
        Simulation simulation = Simulation.builder()
                .userId(user.getId())
                .clientId(request.getClientId())
                .propertyId(request.getPropertyId())
                .financialEntityId(request.getFinancialEntityId())
                .programType(request.getProgramType())
                .currency(request.getFinancingDetails().getCurrency())
                .usdValue(request.getFinancingDetails().getUsdValue())
                .propertyPrice(request.getCalculatedValues().getPropertyPrice())
                .stateContribution(request.getCalculatedValues().getStateContribution())
                .initialPayment(request.getCalculatedValues().getInitialPayment())
                .initialCosts(request.getCalculatedValues().getInitialCosts())
                .financingAmount(request.getCalculatedValues().getFinancingAmount())
                .termYears(request.getFinancingDetails().getTermYears())
                .interestRate(request.getFinancingDetails().getInterestRate().getRate())
                .interestRateType(request.getFinancingDetails().getInterestRate().getType())
                .interestRatePeriod(request.getFinancingDetails().getInterestRate().getPeriod())
                .interestRateCapitalization(request.getFinancingDetails().getInterestRate().getCapitalization())
                .opportunityCostRate(request.getFinancingDetails().getOpportunityCost().getRate())
                .opportunityCostType(request.getFinancingDetails().getOpportunityCost().getType())
                .opportunityCostPeriod(request.getFinancingDetails().getOpportunityCost().getPeriod())
                .opportunityCostCapitalization(request.getFinancingDetails().getOpportunityCost().getCapitalization())
                .gracePeriodType(request.getFinancingDetails().getGracePeriod().getType())
                .gracePeriodDurationMonths(request.getFinancingDetails().getGracePeriod().getDurationMonths())
                .monthlyCommissions(request.getFinancingDetails().getMonthlyCosts().getConstantCommissions())
                .administrationCosts(request.getFinancingDetails().getMonthlyCosts().getAdministrationCosts())
                .statementDelivery(request.getFinancingDetails().getStatementDelivery())
                .desgravamenEnabled(request.getFinancingDetails().getInsurance() != null &&
                        request.getFinancingDetails().getInsurance().getDesgravamen() != null &&
                        Boolean.TRUE.equals(request.getFinancingDetails().getInsurance().getDesgravamen().getEnabled()))
                .desgravamenRate(request.getFinancingDetails().getInsurance() != null &&
                        request.getFinancingDetails().getInsurance().getDesgravamen() != null &&
                        Boolean.TRUE.equals(request.getFinancingDetails().getInsurance().getDesgravamen().getEnabled()) ?
                        request.getFinancingDetails().getInsurance().getDesgravamen().getRate() : BigDecimal.ZERO)
                .propertyInsuranceEnabled(request.getFinancingDetails().getInsurance() != null &&
                        request.getFinancingDetails().getInsurance().getPropertyInsurance() != null &&
                        Boolean.TRUE.equals(request.getFinancingDetails().getInsurance().getPropertyInsurance().getEnabled()))
                .propertyInsuranceRate(request.getFinancingDetails().getInsurance() != null &&
                        request.getFinancingDetails().getInsurance().getPropertyInsurance() != null &&
                        Boolean.TRUE.equals(request.getFinancingDetails().getInsurance().getPropertyInsurance().getEnabled()) ?
                        request.getFinancingDetails().getInsurance().getPropertyInsurance().getRate() : BigDecimal.ZERO)
                .propertyInsuranceValue(request.getFinancingDetails().getInsurance() != null &&
                        request.getFinancingDetails().getInsurance().getPropertyInsurance() != null ?
                        request.getFinancingDetails().getInsurance().getPropertyInsurance().getValue() : BigDecimal.ZERO)
                .monthlyPayment(firstTotalPayment)
                .tcea(tcea)
                .cok(opportunityCostTEA)
                .van(van)
                .tir(tir)
                .totalInterest(totalInterest)
                .status(Simulation.SimulationStatus.COMPLETED)
                .build();

        simulation = simulationRepository.save(simulation);

        // Generate amortization schedule
        generateAmortizationSchedule(simulation, interestRateTEM, monthlyPayment);

        log.info("Created simulation {} for user {} with financing amount {}",
                simulation.getId(), user.getEmail(), request.getCalculatedValues().getFinancingAmount());

        return mapToResponseDTO(simulation, client, property, false); // Without amortization schedule
    }

    private BigDecimal calculateMonthlyCosts(CreateSimulationRequestDTO request) {
        BigDecimal costs = request.getFinancingDetails().getMonthlyCosts().getConstantCommissions()
                .add(request.getFinancingDetails().getMonthlyCosts().getAdministrationCosts());

        // Add delivery cost if physical
        if ("physical".equals(request.getFinancingDetails().getStatementDelivery())) {
            costs = costs.add(BigDecimal.valueOf(10));
        }

        // Add insurance costs - with null checks
        if (request.getFinancingDetails().getInsurance() != null) {
            if (request.getFinancingDetails().getInsurance().getDesgravamen() != null &&
                Boolean.TRUE.equals(request.getFinancingDetails().getInsurance().getDesgravamen().getEnabled())) {
                costs = costs.add(request.getCalculatedValues().getFinancingAmount()
                        .multiply(request.getFinancingDetails().getInsurance().getDesgravamen().getRate()));
            }

            if (request.getFinancingDetails().getInsurance().getPropertyInsurance() != null &&
                Boolean.TRUE.equals(request.getFinancingDetails().getInsurance().getPropertyInsurance().getEnabled())) {
                costs = costs.add(request.getFinancingDetails().getInsurance().getPropertyInsurance().getValue()
                        .multiply(request.getFinancingDetails().getInsurance().getPropertyInsurance().getRate()));
            }
        }

        return costs;
    }

    private BigDecimal calculateTotalCosts(CreateSimulationRequestDTO request) {
        return calculateMonthlyCosts(request).multiply(BigDecimal.valueOf(request.getFinancingDetails().getTermYears() * 12));
    }

    private BigDecimal calculateApplicableBonus(CreateSimulationRequestDTO request) {
        // Simplified bonus calculation - return default bonus
        return BigDecimal.valueOf(5000);
    }

    private void generateAmortizationSchedule(Simulation simulation, BigDecimal monthlyRate, BigDecimal monthlyPayment) {
        List<FinancialCalculator.AmortizationEntry> entries = financialCalculator.generateAmortizationSchedule(
                simulation.getFinancingAmount(),
                monthlyRate,
                monthlyPayment,
                simulation.getTermYears() * 12,
                simulation.getGracePeriodDurationMonths(),
                simulation.getGracePeriodType(),
                simulation.getDesgravamenEnabled() ? simulation.getDesgravamenRate() : BigDecimal.ZERO,
                simulation.getPropertyInsuranceEnabled() ? simulation.getPropertyInsuranceRate() : BigDecimal.ZERO,
                simulation.getMonthlyCommissions(),
                simulation.getAdministrationCosts(),
                simulation.getStatementDelivery(),
                simulation.getPropertyInsuranceValue()
        );

        List<AmortizationSchedule> schedule = entries.stream()
                .map(entry -> AmortizationSchedule.builder()
                        .simulation(simulation)
                        .periodNumber(entry.getPeriodNumber())
                        .paymentDate(LocalDate.now().plusMonths(entry.getPeriodNumber() - 1))
                        .tem(monthlyRate)
                        .gracePeriod(simulation.getGracePeriodDurationMonths() != null ?
                                simulation.getGracePeriodDurationMonths() : 0)
                        .initialBalance(entry.getBeginningBalance())
                        .interest(entry.getInterestPayment())
                        .payment(entry.getScheduledPayment())
                        .principal(entry.getPrincipalPayment())
                        .lifeInsurance(entry.getLifeInsurancePayment())
                        .propertyInsurance(entry.getPropertyInsurancePayment())
                        .commissions(simulation.getMonthlyCommissions())
                        .adminCosts(simulation.getAdministrationCosts())
                        .deliveryCosts("physical".equals(simulation.getStatementDelivery()) ?
                                BigDecimal.valueOf(10) : BigDecimal.ZERO)
                        .finalBalance(entry.getEndingBalance())
                        .cashFlow(entry.getScheduledPayment().negate())
                        .cumulativePrincipal(entry.getCumulativePrincipal())
                        .cumulativeInterest(entry.getCumulativeInterest())
                        .isGracePeriod(entry.isGracePeriod())
                        .build())
                .collect(Collectors.toList());
        // Captura la primera cuota total real de la tabla generada
        BigDecimal firstTotalPayment = schedule.stream()
                .filter(e -> e.getPeriodNumber() == 1)
                .map(AmortizationSchedule::getPayment) // 'payment' es la cuota total de la tabla
                .findFirst()
                .orElseGet(() -> {
                    // Fallback si la tabla está vacía (no debería pasar)
                    // Recalcula los costos del primer mes usando el objeto 'simulation'
                    BigDecimal costs = simulation.getMonthlyCommissions().add(simulation.getAdministrationCosts());

                    if ("physical".equals(simulation.getStatementDelivery())) {
                        costs = costs.add(BigDecimal.valueOf(10));
                    }
                    if (Boolean.TRUE.equals(simulation.getDesgravamenEnabled())) {
                        // El seguro de desgravamen del primer mes se calcula sobre el saldo inicial total
                        costs = costs.add(simulation.getFinancingAmount().multiply(simulation.getDesgravamenRate(), MATH_CONTEXT));
                    }
                    if (Boolean.TRUE.equals(simulation.getPropertyInsuranceEnabled())) {
                        // El seguro de inmueble es fijo
                        // ¡¡AQUÍ TAMBIÉN APLICA LA DIVISIÓN ENTRE 12!!
                        BigDecimal monthlyPropertyInsuranceRate = simulation.getPropertyInsuranceRate().divide(BigDecimal.valueOf(12), MATH_CONTEXT);
                        costs = costs.add(simulation.getPropertyInsuranceValue().multiply(monthlyPropertyInsuranceRate));
                    }
                    // Fallback es la cuota (C+I) + costos
                    return monthlyPayment.add(costs);
                });

        simulation.setAmortizationSchedule(schedule);
        // Guarda la primera cuota total real en el campo monthlyPayment
        simulation.setMonthlyPayment(firstTotalPayment.setScale(2, RoundingMode.HALF_UP));
        simulationRepository.save(simulation);
    }

    public List<SimulationResponseDTO> getUserSimulations() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        return simulationRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(sim -> {
                    try {
                        var client = clientService.getClientById(sim.getClientId());
                        var property = propertyService.getPropertyById(sim.getPropertyId());
                        return mapToResponseDTO(sim, client, property, false);
                    } catch (Exception e) {
                        // Fallback if client/property not found
                        return mapToResponseDTO(sim, false);
                    }
                })
                .collect(Collectors.toList());
    }

    public Optional<SimulationResponseDTO> getSimulationById(Long id) {
        return simulationRepository.findById(id)
                .map(sim -> {
                    var client = clientService.getClientById(sim.getClientId());
                    var property = propertyService.getPropertyById(sim.getPropertyId());
                    return mapToResponseDTO(sim, client, property, true); // Include amortization schedule
                });
    }

    public List<SimulationResponseDTO> getClientSimulations(Long clientId) {
        return simulationRepository.findByClientIdOrderByCreatedAtDesc(clientId)
                .stream()
                .map(sim -> {
                    try {
                        var client = clientService.getClientById(sim.getClientId());
                        var property = propertyService.getPropertyById(sim.getPropertyId());
                        return mapToResponseDTO(sim, client, property, false);
                    } catch (Exception e) {
                        // Fallback if client/property not found
                        return mapToResponseDTO(sim, false);
                    }
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public boolean updateSimulationStatus(Long id, Simulation.SimulationStatus status) {
        return simulationRepository.findById(id)
                .map(simulation -> {
                    simulation.setStatus(status);
                    simulationRepository.save(simulation);
                    log.info("Updated simulation {} status to {}", id, status);
                    return true;
                })
                .orElse(false);
    }

    @Transactional
    public boolean deleteSimulation(Long id) {
        return simulationRepository.findById(id)
                .map(simulation -> {
                    simulationRepository.delete(simulation);
                    log.info("Deleted simulation {}", id);
                    return true;
                })
                .orElse(false);
    }

    private SimulationResponseDTO mapToResponseDTO(Simulation simulation, boolean includeSchedule) {
        return mapToResponseDTO(simulation, null, null, includeSchedule);
    }

    private SimulationResponseDTO mapToResponseDTO(Simulation simulation, ClientDTO client, PropertyDTO property, boolean includeSchedule) {
        SimulationResponseDTO.ClientInfo clientInfo = null;
        if (client != null && client.getHolder() != null) {
            clientInfo = SimulationResponseDTO.ClientInfo.builder()
                    .id(client.getId())
                    .name(client.getHolder().getNombres() + " " + client.getHolder().getApellidos())
                    .documentNumber(client.getHolder().getDocumentNumber())
                    .build();
        }

        SimulationResponseDTO.PropertyInfo propertyInfo = null;
        if (property != null) {
            propertyInfo = SimulationResponseDTO.PropertyInfo.builder()
                    .id(property.getId())
                    .name(property.getNombreProyecto())
                    .price(property.getPropertyPrice())
                    .build();
        }

        SimulationResponseDTO.Summary summary = SimulationResponseDTO.Summary.builder()
                .propertyValue(simulation.getPropertyPrice())
                .stateContribution(simulation.getStateContribution())
                .initialPayment(simulation.getInitialPayment())
                .financingAmount(simulation.getFinancingAmount())
                .build();

        SimulationResponseDTO.KeyIndicators keyIndicators = SimulationResponseDTO.KeyIndicators.builder()
                .monthlyPayment(simulation.getMonthlyPayment())
                .tcea(simulation.getTcea())
                .cok(simulation.getCok())
                .van(simulation.getVan())
                .tir(simulation.getTir())
                .totalInterest(simulation.getTotalInterest())
                .build();

        SimulationResponseDTO dto = SimulationResponseDTO.builder()
                .simulationId(simulation.getId().toString())
                .clientInfo(clientInfo)
                .propertyInfo(propertyInfo)
                .summary(summary)
                .keyIndicators(keyIndicators)
                .calculationMethod("French Method (Ordinary Annuity)")
                .generatedAt(simulation.getCreatedAt())
                .build();

        if (includeSchedule && simulation.getAmortizationSchedule() != null) {
            List<SimulationResponseDTO.Payment> allPayments = simulation.getAmortizationSchedule()
                    .stream()
                    .sorted((a, b) -> Integer.compare(a.getPeriodNumber(), b.getPeriodNumber()))
                    .map(entry -> SimulationResponseDTO.Payment.builder()
                            .paymentNumber(entry.getPeriodNumber())
                            .tem(entry.getTem())
                            .gracePeriod(entry.getGracePeriod())
                            .initialBalance(entry.getInitialBalance())
                            .interest(entry.getInterest())
                            .payment(entry.getPayment())
                            .principal(entry.getPrincipal())
                            .lifeInsurance(entry.getLifeInsurance())
                            .propertyInsurance(entry.getPropertyInsurance())
                            .commissions(entry.getCommissions())
                            .adminCosts(entry.getAdminCosts())
                            .deliveryCosts(entry.getDeliveryCosts())
                            .finalBalance(entry.getFinalBalance())
                            .cashFlow(entry.getCashFlow())
                            .build())
                    .collect(Collectors.toList());

            int totalPayments = allPayments.size();
            int pageSize = totalPayments > 0 ? totalPayments : 10; // Evitar pageSize 0
            int currentPage = 1; // Se devuelve la "página" 1 (completa)

            SimulationResponseDTO.AmortizationSchedule schedule = SimulationResponseDTO.AmortizationSchedule.builder()
                    .totalPayments(totalPayments)
                    .currentPage(currentPage)
                    .pageSize(pageSize)
                    .payments(allPayments)
                    .build();

            dto.setAmortizationSchedule(schedule);
        }

        return dto;
    }
}