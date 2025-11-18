package com.simucredito.configuration.domain.repository;

import com.simucredito.configuration.domain.model.FinancialEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FinancialEntityRepository extends JpaRepository<FinancialEntity, Long> {

    Optional<FinancialEntity> findByEntityCode(String entityCode);

    List<FinancialEntity> findByIsActiveTrue();

    @Query("SELECT fe FROM FinancialEntity fe WHERE fe.isActive = true AND fe.maxLoanAmount >= :loanAmount")
    List<FinancialEntity> findActiveEntitiesByMaxLoanAmount(@Param("loanAmount") java.math.BigDecimal loanAmount);

    @Query("SELECT fe FROM FinancialEntity fe WHERE fe.isActive = true AND fe.minLoanAmount <= :loanAmount AND fe.maxLoanAmount >= :loanAmount")
    List<FinancialEntity> findActiveEntitiesForLoanAmount(@Param("loanAmount") java.math.BigDecimal loanAmount);

    boolean existsByEntityCode(String entityCode);
}