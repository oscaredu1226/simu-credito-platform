package com.simucredito.client.application.dto;

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
public class PersonDTO {

    private Long id;
    private Long documentTypeId;
    private Long maritalStatusId;
    private Long educationLevelId;
    private Long economicActivityId;
    private String nombres;
    private String apellidos;
    private String documentNumber;
    private LocalDate birthDate;
    private String correo;
    private String telefono;
    private String direccion;
    private String profesion;
    private BigDecimal monthlyNetIncome;
}