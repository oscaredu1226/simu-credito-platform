package com.simucredito.simulation.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AmortizationEntryDTO {

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