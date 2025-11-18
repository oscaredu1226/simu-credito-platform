package com.simucredito.client.application.dto;

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
public class ClientDTO {

    private Long id;
    private Long userId;
    private PersonDTO holder;
    private PersonDTO spouse;
    private Long fundSourceId;
    private BigDecimal familyNetIncome;
    private Boolean appliesForIntegratorBonus;
    private String conadisCardNumber;
    private Boolean isOwnerOfAnotherProperty;
    private Boolean receivedPreviousSupport;

    // Pre-qualification fields
    private String bbpStatus;
    private String sustainableBonusStatus;
    private String integratorBonusStatus;
    private String techoPropioStatus;
    private String recomendacion;

    private LocalDateTime registrationDate;
}