package com.simucredito.configuration.domain.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "bonus_parameters")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BonusParameter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "bonus_type", nullable = false)
    private String bonusType; // "BBP", "BFH"

    @Column(name = "bonus_subtype")
    private String bonusSubtype; // "TRADITIONAL", "SUSTAINABLE", "INTEGRATOR"

    @Column(name = "min_property_value")
    private BigDecimal minPropertyValue;

    @Column(name = "max_property_value")
    private BigDecimal maxPropertyValue;

    @Column(name = "bonus_percentage", nullable = false)
    private BigDecimal bonusPercentage;

    @Column(name = "bonus_amount")
    private BigDecimal bonusAmount;

    @Column(name = "is_sustainable_required")
    private Boolean isSustainableRequired = false;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "valid_from", nullable = false)
    private LocalDateTime validFrom;

    @Column(name = "valid_until")
    private LocalDateTime validUntil;

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

    public boolean isCurrentlyValid() {
        LocalDateTime now = LocalDateTime.now();
        return isActive &&
               (validFrom == null || !now.isBefore(validFrom)) &&
               (validUntil == null || !now.isAfter(validUntil));
    }

    public boolean appliesToProperty(BigDecimal propertyValue, Boolean isSustainable) {
        if (!isCurrentlyValid()) {
            return false;
        }

        // Check property value range
        if (minPropertyValue != null && propertyValue.compareTo(minPropertyValue) < 0) {
            return false;
        }
        if (maxPropertyValue != null && propertyValue.compareTo(maxPropertyValue) > 0) {
            return false;
        }

        // Check sustainable requirement
        if (isSustainableRequired != null && isSustainableRequired && (isSustainable == null || !isSustainable)) {
            return false;
        }

        return true;
    }
}