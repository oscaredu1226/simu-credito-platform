package com.simucredito.client.application.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateClientRequestDTO {

    @NotNull(message = "Holder person is required")
    private CreatePersonRequestDTO holder;

    private CreatePersonRequestDTO spouse;

    private Long fundSourceId;

    private BigDecimal familyNetIncome;

    private Boolean appliesForIntegratorBonus;

    private String conadisCardNumber;

    private Boolean isOwnerOfAnotherProperty;

    private Boolean receivedPreviousSupport;
}