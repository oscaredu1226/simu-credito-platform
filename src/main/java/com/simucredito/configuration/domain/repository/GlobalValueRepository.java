package com.simucredito.configuration.domain.repository;

import com.simucredito.configuration.domain.model.GlobalValue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GlobalValueRepository extends JpaRepository<GlobalValue, Long> {

    Optional<GlobalValue> findByValueKey(String valueKey);

    List<GlobalValue> findByIsActiveTrue();

    @Query("SELECT gv FROM GlobalValue gv WHERE gv.isActive = true AND gv.valueKey = :valueKey AND " +
           "(gv.validFrom IS NULL OR gv.validFrom <= CURRENT_TIMESTAMP) AND " +
           "(gv.validUntil IS NULL OR gv.validUntil >= CURRENT_TIMESTAMP)")
    Optional<GlobalValue> findCurrentlyValidByKey(@Param("valueKey") String valueKey);

    @Query("SELECT gv FROM GlobalValue gv WHERE gv.isActive = true AND " +
           "(gv.validFrom IS NULL OR gv.validFrom <= CURRENT_TIMESTAMP) AND " +
           "(gv.validUntil IS NULL OR gv.validUntil >= CURRENT_TIMESTAMP)")
    List<GlobalValue> findAllCurrentlyValid();

    boolean existsByValueKey(String valueKey);
}