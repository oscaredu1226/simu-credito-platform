package com.simucredito.client.application.service;

import com.simucredito.client.application.dto.*;
import com.simucredito.client.domain.model.Client;
import com.simucredito.client.domain.model.Person;
import com.simucredito.client.domain.repository.ClientRepository;
import com.simucredito.client.domain.repository.PersonRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ClientService {

    private final ClientRepository clientRepository;
    private final PersonRepository personRepository;
    private final ModelMapper modelMapper;

    @Transactional
    public ClientDTO createClient(CreateClientRequestDTO request) {
        Long userId = getCurrentUserId();

        // Validate that document number is not already in use
        if (personRepository.existsByDocumentNumber(request.getHolder().getDocumentNumber())) {
            throw new RuntimeException("Document number already exists: " + request.getHolder().getDocumentNumber());
        }

        // Validate birth date - must be 18+ years old
        validateBirthDate(request.getHolder().getBirthDate());

        // Create holder person
        Person holder = createPerson(request.getHolder());
        personRepository.save(holder);

        // Create spouse person only if marital status requires it
        Person spouse = null;
        if (shouldHaveSpouse(holder.getMaritalStatusId())) {
            if (request.getSpouse() != null) {
                // Validate that spouse document number is not already in use
                if (personRepository.existsByDocumentNumber(request.getSpouse().getDocumentNumber())) {
                    throw new RuntimeException("Spouse document number already exists: " + request.getSpouse().getDocumentNumber());
                }
                // Validate spouse birth date - must be 18+ years old
                validateBirthDate(request.getSpouse().getBirthDate());
                spouse = createPerson(request.getSpouse());
                personRepository.save(spouse);
            } else {
                throw new RuntimeException("Spouse information is required for married or cohabiting clients");
            }
        }

        // Create client
        Client client = Client.builder()
                .userId(userId)
                .holderId(holder.getId())
                .spouseId(spouse != null ? spouse.getId() : null)
                .fundSourceId(request.getFundSourceId())
                .familyNetIncome(request.getFamilyNetIncome())
                .appliesForIntegratorBonus(request.getAppliesForIntegratorBonus())
                .conadisCardNumber(request.getConadisCardNumber())
                .isOwnerOfAnotherProperty(request.getIsOwnerOfAnotherProperty())
                .receivedPreviousSupport(request.getReceivedPreviousSupport())
                .build();

        client = clientRepository.save(client);

        // Perform initial pre-qualification
        performInitialPreQualification(client, holder, spouse);

        return buildClientDTO(client, holder, spouse);
    }

    public List<ClientDTO> getClientsByUser() {
        Long userId = getCurrentUserId();
        List<Client> clients = clientRepository.findByUserId(userId);

        return clients.stream()
                .map(this::buildClientDTO)
                .collect(Collectors.toList());
    }

    public ClientDTO getClientById(Long clientId) {
        Long userId = getCurrentUserId();
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new RuntimeException("Client not found"));

        // Verify client belongs to current user
        if (!client.getUserId().equals(userId)) {
            throw new RuntimeException("Access denied");
        }

        return buildClientDTO(client);
    }

    @Transactional
    public PreQualificationResponseDTO performPreQualification(Long clientId) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new RuntimeException("Client not found"));

        Long userId = getCurrentUserId();
        if (!client.getUserId().equals(userId)) {
            throw new RuntimeException("Access denied");
        }

        // TODO: Implement actual pre-qualification logic based on bonus rules
        // For now, return a mock response
        PreQualificationResponseDTO response = PreQualificationResponseDTO.builder()
                .clientId(clientId)
                .bbpStatus("ELIGIBLE")
                .sustainableBonusStatus("NOT_ELIGIBLE")
                .integratorBonusStatus("ELIGIBLE")
                .techoPropioStatus("ELIGIBLE")
                .recomendacion("Cliente elegible para BBP e Integrador")
                .isEligible(true)
                .build();

        // Update client with pre-qualification results
        client.setBbpStatus(response.getBbpStatus());
        client.setSustainableBonusStatus(response.getSustainableBonusStatus());
        client.setIntegratorBonusStatus(response.getIntegratorBonusStatus());
        client.setTechoPropioStatus(response.getTechoPropioStatus());
        client.setRecomendacion(response.getRecomendacion());
        clientRepository.save(client);

        return response;
    }

    public PreQualificationResponseDTO performPreQualification(PreQualificationRequestDTO request) {
        // Pre-qualification is based only on client data, not property data
        // Property evaluation happens during simulation

        String bbpStatus = "NOT_ELIGIBLE";
        String sustainableBonusStatus = "NOT_ELIGIBLE";
        String integratorBonusStatus = "NOT_ELIGIBLE";
        String techoPropioStatus = "NOT_ELIGIBLE";
        String recomendacion = "Cliente no elegible para bonos";

        // BBP evaluation based on income and other client criteria
        if (request.getMonthlyIncome().compareTo(BigDecimal.valueOf(2500)) >= 0 &&
            request.getFamilyNetIncome().compareTo(BigDecimal.valueOf(3500)) >= 0) {
            bbpStatus = "ELIGIBLE";
            recomendacion = "Cliente elegible para BBP";
        }

        // Integrator bonus evaluation
        if (request.getAppliesForIntegratorBonus() &&
            request.getAge() >= 18 && request.getAge() <= 35 &&
            request.getMonthlyIncome().compareTo(BigDecimal.valueOf(1500)) >= 0) {
            integratorBonusStatus = "ELIGIBLE";
            recomendacion += " y Bono Integrador";
        }

        // Techo Propio evaluation
        if (!request.getIsOwnerOfAnotherProperty() &&
            !request.getHasReceivedPreviousSupport() &&
            request.getMonthlyIncome().compareTo(BigDecimal.valueOf(1200)) >= 0) {
            techoPropioStatus = "ELIGIBLE";
            recomendacion += " y Techo Propio";
        }

        // Sustainable bonus evaluation (requires additional property criteria)
        sustainableBonusStatus = "REQUIRES_PROPERTY_EVALUATION";

        boolean isEligible = "ELIGIBLE".equals(bbpStatus) ||
                           "ELIGIBLE".equals(integratorBonusStatus) ||
                           "ELIGIBLE".equals(techoPropioStatus);

        return PreQualificationResponseDTO.builder()
                .bbpStatus(bbpStatus)
                .sustainableBonusStatus(sustainableBonusStatus)
                .integratorBonusStatus(integratorBonusStatus)
                .techoPropioStatus(techoPropioStatus)
                .recomendacion(recomendacion)
                .isEligible(isEligible)
                .build();
    }

    @Transactional
    public ClientDTO updateClient(Long clientId, CreateClientRequestDTO request) {
        Long userId = getCurrentUserId();
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new RuntimeException("Client not found"));

        if (!client.getUserId().equals(userId)) {
            throw new RuntimeException("Access denied");
        }

        // Update holder person
        Person holder = personRepository.findById(client.getHolderId())
                .orElseThrow(() -> new RuntimeException("Holder person not found"));
        updatePerson(holder, request.getHolder());
        personRepository.save(holder);

        // Update or create spouse person based on marital status
        Person spouse = null;
        Person currentHolder = personRepository.findById(client.getHolderId())
                .orElseThrow(() -> new RuntimeException("Holder person not found"));

        if (shouldHaveSpouse(currentHolder.getMaritalStatusId())) {
            if (request.getSpouse() != null) {
                if (client.getSpouseId() != null) {
                    spouse = personRepository.findById(client.getSpouseId()).orElse(null);
                    if (spouse != null) {
                        updatePerson(spouse, request.getSpouse());
                    } else {
                        spouse = createPerson(request.getSpouse());
                    }
                } else {
                    spouse = createPerson(request.getSpouse());
                }
                personRepository.save(spouse);
            } else {
                throw new RuntimeException("Spouse information is required for married or cohabiting clients");
            }
        } else {
            // If marital status changed to single/divorced/widowed, remove spouse if exists
            if (client.getSpouseId() != null) {
                // Optional: could delete the spouse person record, but for now just disassociate
                client.setSpouseId(null);
            }
        }

        // Update client
        client.setFundSourceId(request.getFundSourceId());
        client.setFamilyNetIncome(request.getFamilyNetIncome());
        client.setAppliesForIntegratorBonus(request.getAppliesForIntegratorBonus());
        client.setConadisCardNumber(request.getConadisCardNumber());
        client.setIsOwnerOfAnotherProperty(request.getIsOwnerOfAnotherProperty());
        client.setReceivedPreviousSupport(request.getReceivedPreviousSupport());
        client.setSpouseId(spouse != null ? spouse.getId() : null);

        client = clientRepository.save(client);

        // Update pre-qualification based on new data
        updateClientPreQualification(client, holder, spouse);

        return buildClientDTO(client, holder, spouse);
    }

    @Transactional
    public void deleteClient(Long clientId) {
        Long userId = getCurrentUserId();
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new RuntimeException("Client not found"));

        if (!client.getUserId().equals(userId)) {
            throw new RuntimeException("Access denied");
        }

        clientRepository.delete(client);
    }

    private Person createPerson(CreatePersonRequestDTO request) {
        return Person.builder()
                .documentTypeId(request.getDocumentTypeId())
                .maritalStatusId(request.getMaritalStatusId())
                .educationLevelId(request.getEducationLevelId())
                .economicActivityId(request.getEconomicActivityId())
                .nombres(request.getNombres())
                .apellidos(request.getApellidos())
                .documentNumber(request.getDocumentNumber())
                .birthDate(request.getBirthDate())
                .correo(request.getCorreo())
                .telefono(request.getTelefono())
                .direccion(request.getDireccion())
                .profesion(request.getProfesion())
                .monthlyNetIncome(request.getMonthlyNetIncome())
                .build();
    }

    private ClientDTO buildClientDTO(Client client) {
        Person holder = personRepository.findById(client.getHolderId())
                .orElseThrow(() -> new RuntimeException("Holder person not found"));

        Person spouse = null;
        if (client.getSpouseId() != null) {
            spouse = personRepository.findById(client.getSpouseId()).orElse(null);
        }

        return buildClientDTO(client, holder, spouse);
    }

    private ClientDTO buildClientDTO(Client client, Person holder, Person spouse) {
        return ClientDTO.builder()
                .id(client.getId())
                .userId(client.getUserId())
                .holder(modelMapper.map(holder, PersonDTO.class))
                .spouse(spouse != null ? modelMapper.map(spouse, PersonDTO.class) : null)
                .fundSourceId(client.getFundSourceId())
                .familyNetIncome(client.getFamilyNetIncome())
                .appliesForIntegratorBonus(client.getAppliesForIntegratorBonus())
                .conadisCardNumber(client.getConadisCardNumber())
                .isOwnerOfAnotherProperty(client.getIsOwnerOfAnotherProperty())
                .receivedPreviousSupport(client.getReceivedPreviousSupport())
                .bbpStatus(client.getBbpStatus())
                .sustainableBonusStatus(client.getSustainableBonusStatus())
                .integratorBonusStatus(client.getIntegratorBonusStatus())
                .techoPropioStatus(client.getTechoPropioStatus())
                .recomendacion(client.getRecomendacion())
                .registrationDate(client.getRegistrationDate())
                .build();
    }

    private void updatePerson(Person person, CreatePersonRequestDTO request) {
        person.setDocumentTypeId(request.getDocumentTypeId());
        person.setMaritalStatusId(request.getMaritalStatusId());
        person.setEducationLevelId(request.getEducationLevelId());
        person.setEconomicActivityId(request.getEconomicActivityId());
        person.setNombres(request.getNombres());
        person.setApellidos(request.getApellidos());
        person.setDocumentNumber(request.getDocumentNumber());
        person.setBirthDate(request.getBirthDate());
        person.setCorreo(request.getCorreo());
        person.setTelefono(request.getTelefono());
        person.setDireccion(request.getDireccion());
        person.setProfesion(request.getProfesion());
        person.setMonthlyNetIncome(request.getMonthlyNetIncome());
    }

    private void performInitialPreQualification(Client client, Person holder, Person spouse) {
        // Calculate age from birth date
        int age = holder.getBirthDate() != null ?
            java.time.LocalDate.now().getYear() - holder.getBirthDate().getYear() : 0;

        // Calculate family net income
        BigDecimal familyNetIncome = holder.getMonthlyNetIncome() != null ? holder.getMonthlyNetIncome() : BigDecimal.ZERO;
        if (spouse != null && spouse.getMonthlyNetIncome() != null) {
            familyNetIncome = familyNetIncome.add(spouse.getMonthlyNetIncome());
        }

        // Perform pre-qualification with initial data
        PreQualificationRequestDTO preQualRequest = PreQualificationRequestDTO.builder()
                .monthlyIncome(holder.getMonthlyNetIncome())
                .familyNetIncome(familyNetIncome)
                .age(age)
                .appliesForIntegratorBonus(client.getAppliesForIntegratorBonus())
                .isOwnerOfAnotherProperty(client.getIsOwnerOfAnotherProperty())
                .hasReceivedPreviousSupport(client.getReceivedPreviousSupport())
                .conadisCardNumber(client.getConadisCardNumber())
                .build();

        PreQualificationResponseDTO preQualResponse = performPreQualification(preQualRequest);

        // Update client with pre-qualification results
        client.setBbpStatus(preQualResponse.getBbpStatus());
        client.setSustainableBonusStatus(preQualResponse.getSustainableBonusStatus());
        client.setIntegratorBonusStatus(preQualResponse.getIntegratorBonusStatus());
        client.setTechoPropioStatus(preQualResponse.getTechoPropioStatus());
        client.setRecomendacion(preQualResponse.getRecomendacion());

        clientRepository.save(client);
    }

    private void updateClientPreQualification(Client client, Person holder, Person spouse) {
        // Calculate age from birth date
        int age = holder.getBirthDate() != null ?
            java.time.LocalDate.now().getYear() - holder.getBirthDate().getYear() : 0;

        // Calculate family net income
        BigDecimal familyNetIncome = holder.getMonthlyNetIncome() != null ? holder.getMonthlyNetIncome() : BigDecimal.ZERO;
        if (spouse != null && spouse.getMonthlyNetIncome() != null) {
            familyNetIncome = familyNetIncome.add(spouse.getMonthlyNetIncome());
        }

        // Perform pre-qualification with updated data
        PreQualificationRequestDTO preQualRequest = PreQualificationRequestDTO.builder()
                .monthlyIncome(holder.getMonthlyNetIncome())
                .familyNetIncome(familyNetIncome)
                .age(age)
                .appliesForIntegratorBonus(client.getAppliesForIntegratorBonus())
                .isOwnerOfAnotherProperty(client.getIsOwnerOfAnotherProperty())
                .hasReceivedPreviousSupport(client.getReceivedPreviousSupport())
                .conadisCardNumber(client.getConadisCardNumber())
                .build();

        PreQualificationResponseDTO preQualResponse = performPreQualification(preQualRequest);

        // Update client with new pre-qualification results
        client.setBbpStatus(preQualResponse.getBbpStatus());
        client.setSustainableBonusStatus(preQualResponse.getSustainableBonusStatus());
        client.setIntegratorBonusStatus(preQualResponse.getIntegratorBonusStatus());
        client.setTechoPropioStatus(preQualResponse.getTechoPropioStatus());
        client.setRecomendacion(preQualResponse.getRecomendacion());

        clientRepository.save(client);
    }

    private boolean shouldHaveSpouse(Long maritalStatusId) {
        // Assuming marital status IDs:
        // 1 = Soltero, 2 = Casado, 3 = Divorciado, 4 = Viudo, 5 = Conviviente
        // Only married (2) and cohabiting (5) require spouse information
        return maritalStatusId != null && (maritalStatusId == 2L || maritalStatusId == 5L);
    }

    private void validateBirthDate(java.time.LocalDate birthDate) {
        if (birthDate == null) {
            throw new RuntimeException("Birth date is required");
        }

        java.time.LocalDate today = java.time.LocalDate.now();
        java.time.Period age = java.time.Period.between(birthDate, today);

        if (age.getYears() < 18) {
            throw new RuntimeException("Person must be at least 18 years old");
        }
    }

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        // TODO: Extract user ID from JWT token or authentication principal
        // For now, return a mock user ID
        return 1L;
    }
}