package com.simucredito.configuration.domain.repository;

import com.simucredito.configuration.domain.model.BonusParameter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface BonusParameterRepository extends JpaRepository<BonusParameter, Long> {

    List<BonusParameter> findByBonusTypeAndIsActiveTrue(String bonusType);

    List<BonusParameter> findByBonusTypeAndBonusSubtypeAndIsActiveTrue(String bonusType, String bonusSubtype);

    @Query("SELECT bp FROM BonusParameter bp WHERE bp.isActive = true AND bp.bonusType = :bonusType AND " +
           "(bp.validFrom IS NULL OR bp.validFrom <= CURRENT_TIMESTAMP) AND " +
           "(bp.validUntil IS NULL OR bp.validUntil >= CURRENT_TIMESTAMP)")
    List<BonusParameter> findCurrentlyValidByBonusType(@Param("bonusType") String bonusType);

    @Query("SELECT bp FROM BonusParameter bp WHERE bp.isActive = true AND " +
           "bp.bonusType = :bonusType AND bp.bonusSubtype = :bonusSubtype AND " +
           "(bp.validFrom IS NULL OR bp.validFrom <= CURRENT_TIMESTAMP) AND " +
           "(bp.validUntil IS NULL OR bp.validUntil >= CURRENT_TIMESTAMP) AND " +
           "(bp.minPropertyValue IS NULL OR bp.minPropertyValue <= :propertyValue) AND " +
           "(bp.maxPropertyValue IS NULL OR bp.maxPropertyValue >= :propertyValue) AND " +
           "(bp.isSustainableRequired = false OR :isSustainable = true)")
    List<BonusParameter> findApplicableBonuses(
        @Param("bonusType") String bonusType,
        @Param("bonusSubtype") String bonusSubtype,
        @Param("propertyValue") BigDecimal propertyValue,
        @Param("isSustainable") Boolean isSustainable
    );

    @Query("SELECT bp FROM BonusParameter bp WHERE bp.isActive = true AND " +
           "(bp.validFrom IS NULL OR bp.validFrom <= CURRENT_TIMESTAMP) AND " +
           "(bp.validUntil IS NULL OR bp.validUntil >= CURRENT_TIMESTAMP)")
    List<BonusParameter> findAllCurrentlyValid();
}