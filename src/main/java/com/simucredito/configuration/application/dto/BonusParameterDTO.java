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
public class BonusParameterDTO {

    private Long id;
    private String bonusType;
    private String bonusSubtype;
    private BigDecimal minPropertyValue;
    private BigDecimal maxPropertyValue;
    private BigDecimal bonusPercentage;
    private BigDecimal bonusAmount;
    private Boolean isSustainableRequired;
    private Boolean isActive;
    private LocalDateTime validFrom;
    private LocalDateTime validUntil;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}