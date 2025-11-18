package com.simucredito.client.domain.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "personas")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Person {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tipo_documento_id", nullable = false)
    private Long documentTypeId;

    @Column(name = "estado_civil_id")
    private Long maritalStatusId;

    @Column(name = "grado_instruccion_id")
    private Long educationLevelId;

    @Column(name = "actividad_economica_id")
    private Long economicActivityId;

    @Column(nullable = false)
    private String nombres;

    @Column(nullable = false)
    private String apellidos;

    @Column(name = "numero_documento", nullable = false, unique = true)
    private String documentNumber;

    @Column(name = "fecha_nacimiento")
    private LocalDate birthDate;

    private String correo;

    private String telefono;

    private String direccion;

    private String profesion;

    @Column(name = "ingreso_neto_mensual", precision = 10, scale = 2)
    private BigDecimal monthlyNetIncome;

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
}