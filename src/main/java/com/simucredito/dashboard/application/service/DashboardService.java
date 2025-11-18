package com.simucredito.dashboard.application.service;

import com.simucredito.client.domain.model.Client;
import com.simucredito.client.domain.model.Person;
import com.simucredito.client.domain.repository.ClientRepository;
import com.simucredito.client.domain.repository.PersonRepository;
import com.simucredito.dashboard.application.dto.DashboardMetricsDTO;
import com.simucredito.dashboard.application.dto.RecentActivityDTO;
import com.simucredito.iam.domain.model.User;
import com.simucredito.iam.domain.repository.UserRepository;
import com.simucredito.property.domain.model.Property;
import com.simucredito.property.domain.repository.PropertyRepository;
import com.simucredito.simulation.domain.model.Simulation;
import com.simucredito.simulation.domain.repository.SimulationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardService {

    private final SimulationRepository simulationRepository;
    private final ClientRepository clientRepository;
    private final PersonRepository personRepository;
    private final PropertyRepository propertyRepository;
    private final UserRepository userRepository;

    public DashboardMetricsDTO getDashboardMetrics() {
        LocalDateTime startOfMonth = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        LocalDateTime now = LocalDateTime.now();

        // Simulation metrics
        Long totalSimulations = simulationRepository.count();
        Long simulationsThisMonth = simulationRepository.countSimulationsSince(startOfMonth);
        Long completedSimulations = (long) simulationRepository.findByStatusOrderByCreatedAtDesc(
            com.simucredito.simulation.domain.model.Simulation.SimulationStatus.COMPLETED).size();
        Double avgMonthlyPayment = simulationRepository.getAverageMonthlyPaymentSince(startOfMonth);
        Double totalPaymentsVolume = simulationRepository.getTotalPaymentsVolumeSince(startOfMonth);

        // Client metrics
        Long totalClients = clientRepository.count();
        Long clientsThisMonth = personRepository.findAll().stream()
            .filter(p -> p.getCreatedAt() != null && p.getCreatedAt().isAfter(startOfMonth))
            .count();
        Long preQualifiedClients = clientRepository.findAll().stream()
            .filter(c -> c.getPreQualified())
            .count();

        // Property metrics
        Long totalProperties = propertyRepository.count();
        Long propertiesThisMonth = propertyRepository.findAll().stream()
            .filter(p -> p.getCreatedAt() != null && p.getCreatedAt().isAfter(startOfMonth))
            .count();
        Long sustainableProperties = propertyRepository.findAll().stream()
            .filter(Property::getIsSustainable)
            .count();

        // Financial metrics
        Double avgLoanAmount = simulationRepository.findAll().stream()
            .mapToDouble(s -> s.getFinancingAmount().doubleValue())
            .average().orElse(0.0);
        Double totalLoanAmount = simulationRepository.findAll().stream()
            .mapToDouble(s -> s.getFinancingAmount().doubleValue())
            .sum();
        Double avgBonusAmount = simulationRepository.findAll().stream()
            .mapToDouble(s -> s.getStateContribution().doubleValue())
            .average().orElse(0.0);

        // User metrics
        Long totalUsers = userRepository.count();
        Long activeUsers = userRepository.findAll().stream()
            .filter(User::getIsActive)
            .count();

        return DashboardMetricsDTO.builder()
            .totalSimulations(totalSimulations)
            .simulationsThisMonth(simulationsThisMonth)
            .completedSimulations(completedSimulations)
            .averageMonthlyPayment(avgMonthlyPayment != null ? BigDecimal.valueOf(avgMonthlyPayment) : BigDecimal.ZERO)
            .totalPaymentsVolume(totalPaymentsVolume != null ? BigDecimal.valueOf(totalPaymentsVolume) : BigDecimal.ZERO)
            .totalClients(totalClients)
            .clientsThisMonth(clientsThisMonth)
            .preQualifiedClients(preQualifiedClients)
            .totalProperties(totalProperties)
            .propertiesThisMonth(propertiesThisMonth)
            .sustainableProperties(sustainableProperties)
            .averageLoanAmount(BigDecimal.valueOf(avgLoanAmount))
            .totalLoanAmount(BigDecimal.valueOf(totalLoanAmount))
            .averageBonusAmount(BigDecimal.valueOf(avgBonusAmount))
            .totalUsers(totalUsers)
            .activeUsers(activeUsers)
            .build();
    }

    public List<RecentActivityDTO> getRecentActivity(int limit) {
        LocalDateTime since = LocalDateTime.now().minusDays(30);

        // Get recent simulations
        List<RecentActivityDTO> simulationActivities = simulationRepository
            .findAll()
            .stream()
            .filter(s -> s.getCreatedAt() != null && s.getCreatedAt().isAfter(since))
            .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
            .limit(limit / 3)
            .map(s -> RecentActivityDTO.builder()
                .id(s.getId())
                .type("SIMULATION")
                .description("Nueva simulación de crédito creada")
                .userName("Sistema") // TODO: Add user relationship to simulation
                .createdAt(s.getCreatedAt())
                .amount(s.getFinancingAmount())
                .status(s.getStatus().name())
                .build())
            .collect(Collectors.toList());

        // Get recent clients
        List<RecentActivityDTO> clientActivities = personRepository
            .findAll()
            .stream()
            .filter(p -> p.getCreatedAt() != null && p.getCreatedAt().isAfter(since))
            .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
            .limit(limit / 3)
            .map(p -> RecentActivityDTO.builder()
                .id(p.getId())
                .type("CLIENT")
                .description("Nuevo cliente registrado: " + p.getNombres() + " " + p.getApellidos())
                .userName("Sistema")
                .createdAt(p.getCreatedAt())
                .build())
            .collect(Collectors.toList());

        // Get recent properties
        List<RecentActivityDTO> propertyActivities = propertyRepository
            .findAll()
            .stream()
            .filter(p -> p.getCreatedAt() != null && p.getCreatedAt().isAfter(since))
            .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
            .limit(limit / 3)
            .map(p -> RecentActivityDTO.builder()
                .id(p.getId())
                .type("PROPERTY")
                .description("Nueva propiedad registrada: " + p.getNombreProyecto())
                .userName("Sistema") // TODO: Add user relationship to property
                .createdAt(p.getCreatedAt())
                .amount(p.getPropertyPrice())
                .build())
            .collect(Collectors.toList());

        // Combine and sort by date
        List<RecentActivityDTO> allActivities = new java.util.ArrayList<>();
        allActivities.addAll(simulationActivities);
        allActivities.addAll(clientActivities);
        allActivities.addAll(propertyActivities);

        return allActivities.stream()
            .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
            .limit(limit)
            .collect(Collectors.toList());
    }
}