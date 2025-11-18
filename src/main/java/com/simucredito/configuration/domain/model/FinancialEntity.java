package com.simucredito.configuration.domain.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "financial_entities")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FinancialEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "entity_name", nullable = false)
    private String entityName;

    @Column(name = "entity_code", unique = true, nullable = false)
    private String entityCode;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "max_loan_amount")
    private BigDecimal maxLoanAmount;

    @Column(name = "min_loan_amount")
    private BigDecimal minLoanAmount;

    @Column(name = "max_term_months")
    private Integer maxTermMonths;

    @Column(name = "min_term_months")
    private Integer minTermMonths;

    @Column(name = "interest_rate_tea")
    private BigDecimal interestRateTea;

    @Column(name = "interest_rate_tna")
    private BigDecimal interestRateTna;

    @Column(name = "capitalization_period")
    private String capitalizationPeriod; // "daily", "monthly", etc.

    @Column(name = "processing_fee_percentage")
    private BigDecimal processingFeePercentage;

    @Column(name = "life_insurance_percentage")
    private BigDecimal lifeInsurancePercentage;

    @Column(name = "property_insurance_percentage")
    private BigDecimal propertyInsurancePercentage;

    @Column(name = "photo_url", length = 1000)
    private String photoUrl;

    @Column(name = "periodo_gracia")
    private Integer periodoGracia;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}