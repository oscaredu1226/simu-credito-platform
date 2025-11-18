package com.simucredito.simulation.application.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateSimulationRequestDTO {

    @NotNull(message = "Client ID is required")
    private Long clientId;

    @NotNull(message = "Property ID is required")
    private Long propertyId;

    @NotNull(message = "Program type is required")
    @Pattern(regexp = "mivivienda|techo_propio", message = "Program type must be 'mivivienda' or 'techo_propio'")
    private String programType;

    private Long financialEntityId; // Optional

    @NotNull(message = "Calculated values are required")
    private CalculatedValues calculatedValues;

    @NotNull(message = "Financing details are required")
    private FinancingDetails financingDetails;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CalculatedValues {
        @NotNull(message = "Property price is required")
        @DecimalMin(value = "0.00", message = "Property price cannot be negative")
        private BigDecimal propertyPrice;

        @NotNull(message = "State contribution is required")
        @DecimalMin(value = "0.00", message = "State contribution cannot be negative")
        private BigDecimal stateContribution;

        @NotNull(message = "Initial payment is required")
        @DecimalMin(value = "0.00", message = "Initial payment cannot be negative")
        private BigDecimal initialPayment;

        @NotNull(message = "Initial costs are required")
        @DecimalMin(value = "0.00", message = "Initial costs cannot be negative")
        private BigDecimal initialCosts;

        @NotNull(message = "Financing amount is required")
        @DecimalMin(value = "0.00", message = "Financing amount cannot be negative")
        private BigDecimal financingAmount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FinancingDetails {
        @NotNull(message = "Currency is required")
        @Pattern(regexp = "PEN|USD", message = "Currency must be 'PEN' or 'USD'")
        private String currency;

        @DecimalMin(value = "0.00", message = "USD value cannot be negative")
        private BigDecimal usdValue; // Optional, if USD selected

        @NotNull(message = "Term years is required")
        @Min(value = 1, message = "Term must be at least 1 year")
        @Max(value = 30, message = "Term cannot exceed 30 years")
        private Integer termYears;

        @NotNull(message = "Interest rate is required")
        private InterestRate interestRate;

        @NotNull(message = "Opportunity cost is required")
        private OpportunityCost opportunityCost;

        @NotNull(message = "Grace period is required")
        private GracePeriod gracePeriod;

        @NotNull(message = "Monthly costs are required")
        private MonthlyCosts monthlyCosts;

        @NotNull(message = "Statement delivery is required")
        @Pattern(regexp = "electronic|physical", message = "Statement delivery must be 'electronic' or 'physical'")
        private String statementDelivery;

        @NotNull(message = "Insurance is required")
        private Insurance insurance;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InterestRate {
        @NotNull(message = "Interest rate value is required")
        @DecimalMin(value = "0.00", message = "Interest rate cannot be negative")
        private BigDecimal rate;

        @NotNull(message = "Interest rate type is required")
        @Pattern(regexp = "TE|TN", message = "Rate type must be 'TE' or 'TN'")
        private String type;

        @NotNull(message = "Interest rate period is required")
        @Pattern(regexp = "daily|seminal|bi-weekly|monthly|bi-monthly|quarterly|semi-annually|annual",
                message = "Invalid period")
        private String period;

        @Pattern(regexp = "daily|seminal|bi-weekly|monthly|bi-monthly|quarterly|semi-annually|annual",
                message = "Invalid capitalization period")
        private String capitalization; // Only for TN
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OpportunityCost {
        @NotNull(message = "Opportunity cost rate is required")
        @DecimalMin(value = "0.00", message = "Opportunity cost rate cannot be negative")
        private BigDecimal rate;

        @NotNull(message = "Opportunity cost type is required")
        @Pattern(regexp = "TE|TN", message = "Cost type must be 'TE' or 'TN'")
        private String type;

        @NotNull(message = "Opportunity cost period is required")
        @Pattern(regexp = "daily|seminal|bi-weekly|monthly|bi-monthly|quarterly|semi-annually|annual",
                message = "Invalid period")
        private String period;

        @Pattern(regexp = "daily|seminal|bi-weekly|monthly|bi-monthly|quarterly|semi-annually|annual",
                message = "Invalid capitalization period")
        private String capitalization; // Only for TN
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GracePeriod {
        @NotNull(message = "Grace period type is required")
        @Pattern(regexp = "none|partial|total", message = "Grace period type must be 'none', 'partial', or 'total'")
        private String type;

        @Min(value = 0, message = "Grace period duration cannot be negative")
        @Max(value = 24, message = "Grace period cannot exceed 24 months")
        private Integer durationMonths; // Optional
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonthlyCosts {
        @NotNull(message = "Constant commissions are required")
        @DecimalMin(value = "0.00", message = "Constant commissions cannot be negative")
        private BigDecimal constantCommissions;

        @NotNull(message = "Administration costs are required")
        @DecimalMin(value = "0.00", message = "Administration costs cannot be negative")
        private BigDecimal administrationCosts;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Insurance {
        @NotNull(message = "Desgravamen insurance is required")
        private Desgravamen desgravamen;

        @NotNull(message = "Property insurance is required")
        private PropertyInsurance propertyInsurance;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Desgravamen {
        @NotNull(message = "Desgravamen enabled flag is required")
        private Boolean enabled;

        @NotNull(message = "Desgravamen rate is required")
        @DecimalMin(value = "0.00", message = "Desgravamen rate cannot be negative")
        private BigDecimal rate;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PropertyInsurance {
        @NotNull(message = "Property insurance enabled flag is required")
        private Boolean enabled;

        @NotNull(message = "Property insurance rate is required")
        @DecimalMin(value = "0.00", message = "Property insurance rate cannot be negative")
        private BigDecimal rate;

        @NotNull(message = "Property insurance value is required")
        @DecimalMin(value = "0.00", message = "Property insurance value cannot be negative")
        private BigDecimal value;
    }
}