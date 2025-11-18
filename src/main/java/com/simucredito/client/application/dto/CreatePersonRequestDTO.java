package com.simucredito.client.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePersonRequestDTO {

    @NotNull(message = "Document type is required")
    private Long documentTypeId;

    private Long maritalStatusId;

    private Long educationLevelId;

    private Long economicActivityId;

    @NotBlank(message = "First names are required")
    private String nombres;

    @NotBlank(message = "Last names are required")
    private String apellidos;

    @NotBlank(message = "Document number is required")
    private String documentNumber;

    @NotNull(message = "Birth date is required")
    private LocalDate birthDate;

    private String correo;

    private String telefono;

    private String direccion;

    private String profesion;

    private BigDecimal monthlyNetIncome;
}