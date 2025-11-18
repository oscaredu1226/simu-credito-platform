package com.simucredito.configuration.domain.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "global_values")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GlobalValue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "value_key", unique = true, nullable = false)
    private String valueKey; // "UIT", "EXCHANGE_RATE_USD_PEN"

    @Column(name = "value_name", nullable = false)
    private String valueName;

    @Column(name = "numeric_value")
    private BigDecimal numericValue;

    @Column(name = "string_value")
    private String stringValue;

    @Column(name = "value_type", nullable = false)
    private String valueType; // "NUMERIC", "STRING", "PERCENTAGE", "CURRENCY"

    @Column(name = "unit")
    private String unit; // "PEN", "USD", "%", etc.

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "valid_from", nullable = false)
    private LocalDateTime validFrom;

    @Column(name = "valid_until")
    private LocalDateTime validUntil;

    @Column(name = "description")
    private String description;

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

    public Object getValue() {
        if ("NUMERIC".equals(valueType) || "PERCENTAGE".equals(valueType) || "CURRENCY".equals(valueType)) {
            return numericValue;
        } else if ("STRING".equals(valueType)) {
            return stringValue;
        }
        return null;
    }

    public void setValue(Object value) {
        if (value instanceof BigDecimal) {
            this.numericValue = (BigDecimal) value;
            this.valueType = "NUMERIC";
        } else if (value instanceof String) {
            this.stringValue = (String) value;
            this.valueType = "STRING";
        }
    }

    // Common global values constants
    public static final String UIT_KEY = "UIT";
    public static final String EXCHANGE_RATE_USD_PEN_KEY = "EXCHANGE_RATE_USD_PEN";
    public static final String MAX_LOAN_TO_VALUE_RATIO_KEY = "MAX_LOAN_TO_VALUE_RATIO";
    public static final String MIN_DOWN_PAYMENT_PERCENTAGE_KEY = "MIN_DOWN_PAYMENT_PERCENTAGE";
}