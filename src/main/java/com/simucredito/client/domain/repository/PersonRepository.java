package com.simucredito.client.domain.repository;

import com.simucredito.client.domain.model.Person;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PersonRepository extends JpaRepository<Person, Long> {

    Optional<Person> findByDocumentNumber(String documentNumber);

    boolean existsByDocumentNumber(String documentNumber);
}