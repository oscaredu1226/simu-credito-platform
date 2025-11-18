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
public class GlobalValueDTO {

    private Long id;
    private String valueKey;
    private String valueName;
    private BigDecimal numericValue;
    private String stringValue;
    private String valueType;
    private String unit;
    private Boolean isActive;
    private LocalDateTime validFrom;
    private LocalDateTime validUntil;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}