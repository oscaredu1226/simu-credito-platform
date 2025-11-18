package com.simucredito.configuration.infrastructure;

import com.simucredito.configuration.domain.model.BonusParameter;
import com.simucredito.configuration.domain.model.GlobalValue;
import com.simucredito.configuration.domain.repository.BonusParameterRepository;
import com.simucredito.configuration.domain.repository.GlobalValueRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final BonusParameterRepository bonusParameterRepository;
    private final GlobalValueRepository globalValueRepository;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        log.info("Starting data initialization...");

        initializeBonusParameters();
        initializeGlobalValues();

        log.info("Data initialization completed successfully");
    }

    private void initializeBonusParameters() {
        log.info("Initializing BBP bonus parameters...");

        // Check if data already exists
        if (bonusParameterRepository.count() > 0) {
            log.info("Bonus parameters already exist, skipping initialization");
            return;
        }

        List<BonusParameter> bonusParameters = Arrays.asList(
            // R1 Range: S/ 68,800 - S/ 98,100
            createBonusParameter("BBP", "TRADITIONAL", new BigDecimal("68800"), new BigDecimal("98100"), null, new BigDecimal("27400")),
            createBonusParameter("BBP", "SUSTAINABLE", new BigDecimal("68800"), new BigDecimal("98100"), true, new BigDecimal("33700")),
            createBonusParameter("BBP", "INTEGRATOR", new BigDecimal("68800"), new BigDecimal("98100"), null, new BigDecimal("31000")),
            createBonusParameter("BBP", "INTEGRATOR_SUSTAINABLE", new BigDecimal("68800"), new BigDecimal("98100"), true, new BigDecimal("37300")),

            // R2 Range: S/ 98,101 - S/ 146,900
            createBonusParameter("BBP", "TRADITIONAL", new BigDecimal("98101"), new BigDecimal("146800"), null, new BigDecimal("22800")),
            createBonusParameter("BBP", "SUSTAINABLE", new BigDecimal("98101"), new BigDecimal("146800"), true, new BigDecimal("29100")),
            createBonusParameter("BBP", "INTEGRATOR", new BigDecimal("98101"), new BigDecimal("146800"), null, new BigDecimal("26400")),
            createBonusParameter("BBP", "INTEGRATOR_SUSTAINABLE", new BigDecimal("98101"), new BigDecimal("146800"), true, new BigDecimal("32700")),

            // R3 Range: S/ 146,901 - S/ 244,600
            createBonusParameter("BBP", "TRADITIONAL", new BigDecimal("146801"), new BigDecimal("244600"), null, new BigDecimal("20900")),
            createBonusParameter("BBP", "SUSTAINABLE", new BigDecimal("146801"), new BigDecimal("244600"), true, new BigDecimal("27200")),
            createBonusParameter("BBP", "INTEGRATOR", new BigDecimal("146801"), new BigDecimal("244600"), null, new BigDecimal("24500")),
            createBonusParameter("BBP", "INTEGRATOR_SUSTAINABLE", new BigDecimal("146801"), new BigDecimal("244600"), true, new BigDecimal("30800")),

            // R4 Range: S/ 244,601 - S/ 362,100
            createBonusParameter("BBP", "TRADITIONAL", new BigDecimal("244601"), new BigDecimal("362100"), null, new BigDecimal("7800")),
            createBonusParameter("BBP", "SUSTAINABLE", new BigDecimal("244601"), new BigDecimal("362100"), true, new BigDecimal("14100")),
            createBonusParameter("BBP", "INTEGRATOR", new BigDecimal("244601"), new BigDecimal("362100"), null, new BigDecimal("11400")),
            createBonusParameter("BBP", "INTEGRATOR_SUSTAINABLE", new BigDecimal("244601"), new BigDecimal("362100"), true, new BigDecimal("17700")),

                // R5 Range: S/ 362,101 - S/ 488,800 (No aplica - no recibe ningun bono)
                createBonusParameter("BBP", "TRADITIONAL", new BigDecimal("362101"), new BigDecimal("488800"), null, new BigDecimal("0")),
                createBonusParameter("BBP", "SUSTAINABLE", new BigDecimal("362101"), new BigDecimal("488800"), true, new BigDecimal("0")),
                createBonusParameter("BBP", "INTEGRATOR", new BigDecimal("362101"), new BigDecimal("488800"), null, new BigDecimal("0")),
                createBonusParameter("BBP", "INTEGRATOR_SUSTAINABLE", new BigDecimal("362101"), new BigDecimal("488800"), true, new BigDecimal("0"))
        );

        bonusParameterRepository.saveAll(bonusParameters);
        log.info("Created {} bonus parameters", bonusParameters.size());
    }

    private void initializeGlobalValues() {
        log.info("Initializing Techo Propio global values...");

        // Check if data already exists
        if (globalValueRepository.count() > 0) {
            log.info("Global values already exist, skipping initialization");
            return;
        }

        List<GlobalValue> globalValues = Arrays.asList(
            createGlobalValue("BFH_AVN_AMOUNT", "Bono Familiar Habitacional (BFH) - AVN", new BigDecimal("46545"), "CURRENCY", "PEN",
                "Monto del Bono Familiar Habitacional para el programa Techo Propio - AVN"),

            createGlobalValue("BFH_MAX_MONTHLY_INCOME", "Ingreso Familiar Mensual (Máximo)", new BigDecimal("3715"), "CURRENCY", "PEN",
                "Ingreso familiar mensual máximo para acceder al programa Techo Propio"),

            createGlobalValue("BFH_MAX_PROPERTY_VALUE", "Valor de Vivienda (Máximo)", new BigDecimal("136000"), "CURRENCY", "PEN",
                "Valor máximo de vivienda para el programa Techo Propio"),

            createGlobalValue("EXCHANGE_RATE_USD_PEN", "Tipo de cambio del mercado (valor de usd)", new BigDecimal("3.75"), "CURRENCY", "PEN",
                "Exchange rate USD to PEN")
        );

        globalValueRepository.saveAll(globalValues);
        log.info("Created {} global values", globalValues.size());
    }

    private BonusParameter createBonusParameter(String bonusType, String bonusSubtype, BigDecimal minValue,
                                              BigDecimal maxValue, Boolean isSustainableRequired, BigDecimal bonusAmount) {
        return BonusParameter.builder()
            .bonusType(bonusType)
            .bonusSubtype(bonusSubtype)
            .minPropertyValue(minValue)
            .maxPropertyValue(maxValue)
            .bonusPercentage(BigDecimal.ZERO) // Required field, set to 0 since we're using fixed amounts
            .bonusAmount(bonusAmount)
            .isSustainableRequired(isSustainableRequired)
            .isActive(true)
            .validFrom(LocalDateTime.now().minusYears(1)) // Valid from 1 year ago
            .validUntil(null) // No expiration
            .build();
    }

    private GlobalValue createGlobalValue(String key, String name, BigDecimal numericValue, String valueType,
                                        String unit, String description) {
        return GlobalValue.builder()
            .valueKey(key)
            .valueName(name)
            .numericValue(numericValue)
            .valueType(valueType)
            .unit(unit)
            .isActive(true)
            .validFrom(LocalDateTime.now().minusYears(1)) // Valid from 1 year ago
            .validUntil(null) // No expiration
            .description(description)
            .build();
    }
}