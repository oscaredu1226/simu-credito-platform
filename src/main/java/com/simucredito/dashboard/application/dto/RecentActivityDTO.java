package com.simucredito.dashboard.application.dto;

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
public class RecentActivityDTO {

    private Long id;
    private String type; // "SIMULATION", "CLIENT", "PROPERTY"
    private String description;
    private String userName;
    private LocalDateTime createdAt;
    private BigDecimal amount; // For financial activities
    private String status; // For simulations
}