package com.simucredito.configuration.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FinancialEntityDTO {

    private Long id;
    private String entityName;
    private String entityCode;
    private Boolean isActive;
    private BigDecimal maxLoanAmount;
    private BigDecimal minLoanAmount;
    private Integer maxTermMonths;
    private Integer minTermMonths;
    private BigDecimal interestRateTea;
    private BigDecimal interestRateTna;
    private String capitalizationPeriod;
    private BigDecimal processingFeePercentage;
    private BigDecimal lifeInsurancePercentage;
    private BigDecimal propertyInsurancePercentage;
    private String photoUrl;
    private Integer periodoGracia;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}