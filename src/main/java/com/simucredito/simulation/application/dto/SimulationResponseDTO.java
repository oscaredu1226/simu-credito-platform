package com.simucredito.simulation.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SimulationResponseDTO {

    private String simulationId;
    private ClientInfo clientInfo;
    private PropertyInfo propertyInfo;
    private Summary summary;
    private KeyIndicators keyIndicators;
    private AmortizationSchedule amortizationSchedule;
    private String calculationMethod;
    private LocalDateTime generatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClientInfo {
        private Long id;
        private String name;
        private String documentNumber;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PropertyInfo {
        private Long id;
        private String name;
        private BigDecimal price;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Summary {
        private BigDecimal propertyValue;
        private BigDecimal stateContribution;
        private BigDecimal initialPayment;
        private BigDecimal financingAmount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class KeyIndicators {
        private BigDecimal monthlyPayment;
        private BigDecimal tcea;
        private BigDecimal cok;
        private BigDecimal van;
        private BigDecimal tir;
        private BigDecimal totalInterest;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AmortizationSchedule {
        private Integer totalPayments;
        private Integer currentPage;
        private Integer pageSize;
        private List<Payment> payments;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Payment {
        private Integer paymentNumber;
        private BigDecimal tem;
        private Integer gracePeriod;
        private BigDecimal initialBalance;
        private BigDecimal interest;
        private BigDecimal payment;
        private BigDecimal principal;
        private BigDecimal lifeInsurance;
        private BigDecimal propertyInsurance;
        private BigDecimal commissions;
        private BigDecimal adminCosts;
        private BigDecimal deliveryCosts;
        private BigDecimal finalBalance;
        private BigDecimal cashFlow;
    }
}