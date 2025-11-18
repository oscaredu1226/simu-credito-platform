package com.simucredito.client.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PreQualificationResponseDTO {

    private Long clientId;
    private String bbpStatus;
    private String sustainableBonusStatus;
    private String integratorBonusStatus;
    private String techoPropioStatus;
    private String recomendacion;
    private Boolean isEligible;
}