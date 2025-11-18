package com.simucredito.client.application.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PreQualificationRequestDTO {

    @NotNull(message = "Monthly income is required")
    @DecimalMin(value = "0.01", message = "Monthly income must be greater than 0")
    private BigDecimal monthlyIncome;

    @NotNull(message = "Family net income is required")
    @DecimalMin(value = "0.01", message = "Family net income must be greater than 0")
    private BigDecimal familyNetIncome;

    @NotNull(message = "Age is required")
    @Min(value = 18, message = "Age must be at least 18")
    @Max(value = 100, message = "Age must be less than or equal to 100")
    private Integer age;

    @NotNull(message = "Applies for integrator bonus is required")
    private Boolean appliesForIntegratorBonus;

    @NotNull(message = "Is owner of another property is required")
    private Boolean isOwnerOfAnotherProperty;

    @NotNull(message = "Has received previous support is required")
    private Boolean hasReceivedPreviousSupport;

    private String conadisCardNumber;
}