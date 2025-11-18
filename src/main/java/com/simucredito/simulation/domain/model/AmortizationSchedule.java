package com.simucredito.simulation.domain.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "amortization_schedule")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AmortizationSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "simulation_id", nullable = false)
    private Simulation simulation;

    @Column(name = "period_number", nullable = false)
    private Integer periodNumber;

    @Column(name = "payment_date", nullable = false)
    private LocalDate paymentDate;

    @Column(name = "tem", nullable = false, precision = 18, scale = 10)
    private BigDecimal tem;

    @Column(name = "grace_period", nullable = false)
    private Integer gracePeriod;

    @Column(name = "initial_balance", nullable = false)
    private BigDecimal initialBalance;

    @Column(name = "interest", nullable = false)
    private BigDecimal interest;

    @Column(name = "payment", nullable = false)
    private BigDecimal payment;

    @Column(name = "principal", nullable = false)
    private BigDecimal principal;

    @Column(name = "life_insurance", nullable = false)
    private BigDecimal lifeInsurance;

    @Column(name = "property_insurance", nullable = false)
    private BigDecimal propertyInsurance;

    @Column(name = "commissions", nullable = false)
    private BigDecimal commissions;

    @Column(name = "admin_costs", nullable = false)
    private BigDecimal adminCosts;

    @Column(name = "delivery_costs", nullable = false)
    private BigDecimal deliveryCosts;

    @Column(name = "final_balance", nullable = false)
    private BigDecimal finalBalance;

    @Column(name = "cash_flow", nullable = false)
    private BigDecimal cashFlow;

    @Column(name = "cumulative_principal", nullable = false)
    private BigDecimal cumulativePrincipal;

    @Column(name = "cumulative_interest", nullable = false)
    private BigDecimal cumulativeInterest;

    @Column(name = "is_grace_period", nullable = false)
    @Builder.Default // <-- (Este ya lo tenías, pero asegúrate)
    private Boolean isGracePeriod = false;

    @Column(name = "is_balloon_payment", nullable = false)
    @Builder.Default // <-- AÑADE ESTA LÍNEA
    private Boolean isBalloonPayment = false;
}