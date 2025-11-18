package com.simucredito.dashboard.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardMetricsDTO {

    // Simulation metrics
    private Long totalSimulations;
    private Long simulationsThisMonth;
    private Long completedSimulations;
    private BigDecimal averageMonthlyPayment;
    private BigDecimal totalPaymentsVolume;

    // Client metrics
    private Long totalClients;
    private Long clientsThisMonth;
    private Long preQualifiedClients;

    // Property metrics
    private Long totalProperties;
    private Long propertiesThisMonth;
    private Long sustainableProperties;

    // Financial metrics
    private BigDecimal averageLoanAmount;
    private BigDecimal totalLoanAmount;
    private BigDecimal averageBonusAmount;

    // User metrics
    private Long totalUsers;
    private Long activeUsers;
}