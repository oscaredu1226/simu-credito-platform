package com.simucredito.client.domain.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "clientes")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Client {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", nullable = false)
    private Long userId;

    @Column(name = "titular_id", nullable = false, unique = true)
    private Long holderId;

    @Column(name = "conyuge_id", unique = true)
    private Long spouseId;

    @Column(name = "origen_fondos_id")
    private Long fundSourceId;

    @Column(name = "ingreso_familiar_neto", precision = 10, scale = 2)
    private BigDecimal familyNetIncome;

    @Column(name = "aplica_bono_integrador")
    @Builder.Default
    private Boolean appliesForIntegratorBonus = false;

    @Column(name = "numero_carnet_conadis")
    private String conadisCardNumber;

    @Column(name = "es_propietario_otra_vivienda")
    private Boolean isOwnerOfAnotherProperty;

    @Column(name = "recibio_apoyo_previo")
    private Boolean receivedPreviousSupport;

    // Pre-qualification fields
    @Column(name = "estado_bbp")
    private String bbpStatus;

    @Column(name = "estado_bono_sostenible")
    private String sustainableBonusStatus;

    @Column(name = "estado_bono_integrador")
    private String integratorBonusStatus;

    @Column(name = "estado_techo_propio")
    private String techoPropioStatus;

    @Column(columnDefinition = "TEXT")
    private String recomendacion;

    @Column(name = "pre_qualified")
    @Builder.Default
    private Boolean preQualified = false;

    @Column(name = "fecha_registro", nullable = false)
    private LocalDateTime registrationDate;

    @PrePersist
    protected void onCreate() {
        registrationDate = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        // Audit fields can be added here if needed
    }
}