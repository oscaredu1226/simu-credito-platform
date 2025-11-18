package com.simucredito.configuration.application.service;

import com.simucredito.configuration.application.dto.BonusParameterDTO;
import com.simucredito.configuration.application.dto.FinancialEntityDTO;
import com.simucredito.configuration.application.dto.GlobalValueDTO;
import com.simucredito.configuration.application.dto.UploadFinancialEntityPhotosResponseDTO;
import com.simucredito.configuration.domain.model.BonusParameter;
import com.simucredito.configuration.domain.model.FinancialEntity;
import com.simucredito.configuration.domain.model.GlobalValue;
import com.simucredito.configuration.domain.repository.BonusParameterRepository;
import com.simucredito.configuration.domain.repository.FinancialEntityRepository;
import com.simucredito.configuration.domain.repository.GlobalValueRepository;
import com.simucredito.property.infrastructure.service.FirebaseStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConfigurationService {

    private final FinancialEntityRepository financialEntityRepository;
    private final BonusParameterRepository bonusParameterRepository;
    private final GlobalValueRepository globalValueRepository;
    private final FirebaseStorageService firebaseStorageService;
    private final ModelMapper modelMapper;

    // Financial Entity methods
    @Cacheable(value = "financialEntities", key = "'all'")
    public List<FinancialEntityDTO> getAllFinancialEntities() {
        return financialEntityRepository.findAll().stream()
                .map(entity -> modelMapper.map(entity, FinancialEntityDTO.class))
                .collect(Collectors.toList());
    }

    @Cacheable(value = "financialEntities", key = "'active'")
    public List<FinancialEntityDTO> getActiveFinancialEntities() {
        return financialEntityRepository.findByIsActiveTrue().stream()
                .map(entity -> modelMapper.map(entity, FinancialEntityDTO.class))
                .collect(Collectors.toList());
    }

    public Optional<FinancialEntityDTO> getFinancialEntityById(Long id) {
        return financialEntityRepository.findById(id)
                .map(entity -> modelMapper.map(entity, FinancialEntityDTO.class));
    }

    public Optional<FinancialEntityDTO> getFinancialEntityByCode(String code) {
        return financialEntityRepository.findByEntityCode(code)
                .map(entity -> modelMapper.map(entity, FinancialEntityDTO.class));
    }

    public List<FinancialEntityDTO> getFinancialEntitiesForLoanAmount(BigDecimal loanAmount) {
        return financialEntityRepository.findActiveEntitiesForLoanAmount(loanAmount).stream()
                .map(entity -> modelMapper.map(entity, FinancialEntityDTO.class))
                .collect(Collectors.toList());
    }

    @Transactional
    public FinancialEntityDTO createFinancialEntity(FinancialEntityDTO dto) {
        log.info("Creating financial entity with photoUrl length: {}", dto.getPhotoUrl() != null ? dto.getPhotoUrl().length() : 0);
        if (dto.getPhotoUrl() != null && dto.getPhotoUrl().length() > 1000) {
            log.warn("PhotoUrl exceeds 255 characters: length={}, url={}", dto.getPhotoUrl().length(), dto.getPhotoUrl().substring(0, Math.min(200, dto.getPhotoUrl().length())));
            throw new IllegalArgumentException("Photo URL exceeds maximum length of 1000 characters. Current length: " + dto.getPhotoUrl().length());
        }
        if (financialEntityRepository.existsByEntityCode(dto.getEntityCode())) {
            throw new IllegalArgumentException("Entity code already exists: " + dto.getEntityCode());
        }

        FinancialEntity entity = modelMapper.map(dto, FinancialEntity.class);
        entity = financialEntityRepository.save(entity);

        log.info("Created financial entity: {}", entity.getEntityName());
        return modelMapper.map(entity, FinancialEntityDTO.class);
    }

    public UploadFinancialEntityPhotosResponseDTO uploadFinancialEntityPhotos(List<MultipartFile> files) throws IOException {
        // Validate files
        validateFiles(files);

        // Use the existing FirebaseStorageService method for property photos
        // This will upload to temp/ directory with signed URLs
        com.simucredito.property.application.dto.UploadPhotosResponseDTO propertyResponse =
            firebaseStorageService.uploadPhotos(files, 0L); // Use 0L as userId for admin uploads

        // Convert to financial entity response format
        List<UploadFinancialEntityPhotosResponseDTO.PhotoMetadataDTO> financialPhotos = propertyResponse.getPhotos().stream()
            .map(propertyPhoto -> UploadFinancialEntityPhotosResponseDTO.PhotoMetadataDTO.builder()
                .id(propertyPhoto.getId())
                .originalFilename(propertyPhoto.getOriginalFilename())
                .firebaseUrl(propertyPhoto.getFirebaseUrl())
                .contentType(propertyPhoto.getContentType())
                .size(propertyPhoto.getSize())
                .uploadedAt(propertyPhoto.getUploadedAt())
                .expiresAt(propertyPhoto.getExpiresAt())
                .build())
            .collect(Collectors.toList());

        return UploadFinancialEntityPhotosResponseDTO.builder()
            .photos(financialPhotos)
            .message("Financial entity photos uploaded successfully")
            .expiryTime(propertyResponse.getExpiryTime())
            .build();
    }

    private void validateFiles(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            throw new IllegalArgumentException("At least one file must be provided");
        }

        if (files.size() > 5) { // Financial entities typically need fewer photos than properties
            throw new IllegalArgumentException("Maximum 5 photos allowed per upload");
        }

        for (MultipartFile file : files) {
            if (file.isEmpty()) {
                continue;
            }

            // Validate file type
            String contentType = file.getContentType();
            if (contentType == null || !isValidImageType(contentType)) {
                throw new IllegalArgumentException("Invalid file type: " + contentType +
                    ". Only JPEG, PNG, and WebP images are allowed.");
            }

            // Validate file size (max 5MB per file)
            if (file.getSize() > 5 * 1024 * 1024) {
                throw new IllegalArgumentException("File size exceeds maximum allowed size of 5MB: " +
                    file.getOriginalFilename());
            }
        }
    }

    private boolean isValidImageType(String contentType) {
        return contentType.equals("image/jpeg") ||
               contentType.equals("image/png") ||
               contentType.equals("image/webp");
    }

    @Transactional
    public Optional<FinancialEntityDTO> updateFinancialEntity(Long id, FinancialEntityDTO dto) {
        return financialEntityRepository.findById(id).map(existing -> {
            // Check if code is being changed and if it conflicts
            if (!existing.getEntityCode().equals(dto.getEntityCode()) &&
                financialEntityRepository.existsByEntityCode(dto.getEntityCode())) {
                throw new IllegalArgumentException("Entity code already exists: " + dto.getEntityCode());
            }

            // Validate photo URL length
            if (dto.getPhotoUrl() != null && dto.getPhotoUrl().length() > 1000) {
                            log.warn("PhotoUrl exceeds 1000 characters during update: length={}, url={}",
                                    dto.getPhotoUrl().length(),
                                    dto.getPhotoUrl().substring(0, Math.min(200, dto.getPhotoUrl().length())));
                            throw new IllegalArgumentException("Photo URL exceeds maximum length of 1000 characters. Current length: " + dto.getPhotoUrl().length());
                        }

            // Preserve the existing ID and timestamps to avoid Hibernate issues
            dto.setId(existing.getId());
            dto.setCreatedAt(existing.getCreatedAt());
            modelMapper.map(dto, existing);
            existing = financialEntityRepository.save(existing);

            log.info("Updated financial entity: {}", existing.getEntityName());
            return modelMapper.map(existing, FinancialEntityDTO.class);
        });
    }

    @Transactional
    public boolean deleteFinancialEntity(Long id) {
        return financialEntityRepository.findById(id).map(entity -> {
            financialEntityRepository.delete(entity);
            log.info("Deleted financial entity: {}", entity.getEntityName());
            return true;
        }).orElse(false);
    }

    // Bonus Parameter methods
    @Cacheable(value = "bonusParameters", key = "'all'")
    public List<BonusParameterDTO> getAllBonusParameters() {
        return bonusParameterRepository.findAll().stream()
                .map(param -> modelMapper.map(param, BonusParameterDTO.class))
                .collect(Collectors.toList());
    }

    public List<BonusParameterDTO> getBonusParametersByType(String bonusType) {
        return bonusParameterRepository.findCurrentlyValidByBonusType(bonusType).stream()
                .map(param -> modelMapper.map(param, BonusParameterDTO.class))
                .collect(Collectors.toList());
    }

    public List<BonusParameterDTO> getApplicableBonuses(String bonusType, String bonusSubtype,
                                                       BigDecimal propertyValue, Boolean isSustainable) {
        return bonusParameterRepository.findApplicableBonuses(bonusType, bonusSubtype, propertyValue, isSustainable)
                .stream()
                .map(param -> modelMapper.map(param, BonusParameterDTO.class))
                .collect(Collectors.toList());
    }

    @Transactional
    public BonusParameterDTO createBonusParameter(BonusParameterDTO dto) {
        BonusParameter parameter = modelMapper.map(dto, BonusParameter.class);
        parameter = bonusParameterRepository.save(parameter);

        log.info("Created bonus parameter: {} - {}", parameter.getBonusType(), parameter.getBonusSubtype());
        return modelMapper.map(parameter, BonusParameterDTO.class);
    }

    @Transactional
    public Optional<BonusParameterDTO> updateBonusParameter(Long id, BonusParameterDTO dto) {
        return bonusParameterRepository.findById(id).map(existing -> {
            modelMapper.map(dto, existing);
            existing = bonusParameterRepository.save(existing);

            log.info("Updated bonus parameter: {} - {}", existing.getBonusType(), existing.getBonusSubtype());
            return modelMapper.map(existing, BonusParameterDTO.class);
        });
    }

    @Transactional
    public boolean deleteBonusParameter(Long id) {
        return bonusParameterRepository.findById(id).map(parameter -> {
            bonusParameterRepository.delete(parameter);
            log.info("Deleted bonus parameter: {} - {}", parameter.getBonusType(), parameter.getBonusSubtype());
            return true;
        }).orElse(false);
    }

    // Global Value methods
    @Cacheable(value = "globalValues", key = "'all'")
    public List<GlobalValueDTO> getAllGlobalValues() {
        return globalValueRepository.findAll().stream()
                .map(value -> modelMapper.map(value, GlobalValueDTO.class))
                .collect(Collectors.toList());
    }

    @Cacheable(value = "globalValues", key = "'active'")
    public List<GlobalValueDTO> getActiveGlobalValues() {
        return globalValueRepository.findAllCurrentlyValid().stream()
                .map(value -> modelMapper.map(value, GlobalValueDTO.class))
                .collect(Collectors.toList());
    }

    @Cacheable(value = "globalValues", key = "#key")
    public Optional<GlobalValueDTO> getGlobalValueByKey(String key) {
        return globalValueRepository.findCurrentlyValidByKey(key)
                .map(value -> modelMapper.map(value, GlobalValueDTO.class));
    }

    public BigDecimal getNumericValue(String key) {
        return getGlobalValueByKey(key)
                .map(GlobalValueDTO::getNumericValue)
                .orElseThrow(() -> new IllegalArgumentException("Global value not found: " + key));
    }

    public String getStringValue(String key) {
        return getGlobalValueByKey(key)
                .map(GlobalValueDTO::getStringValue)
                .orElseThrow(() -> new IllegalArgumentException("Global value not found: " + key));
    }

    @Transactional
    public GlobalValueDTO createGlobalValue(GlobalValueDTO dto) {
        if (globalValueRepository.existsByValueKey(dto.getValueKey())) {
            throw new IllegalArgumentException("Value key already exists: " + dto.getValueKey());
        }

        GlobalValue value = modelMapper.map(dto, GlobalValue.class);
        value = globalValueRepository.save(value);

        log.info("Created global value: {}", value.getValueKey());
        return modelMapper.map(value, GlobalValueDTO.class);
    }

    @Transactional
    public Optional<GlobalValueDTO> updateGlobalValue(Long id, GlobalValueDTO dto) {
        return globalValueRepository.findById(id).map(existing -> {
            // Check if key is being changed and if it conflicts
            if (!existing.getValueKey().equals(dto.getValueKey()) &&
                globalValueRepository.existsByValueKey(dto.getValueKey())) {
                throw new IllegalArgumentException("Value key already exists: " + dto.getValueKey());
            }

            modelMapper.map(dto, existing);
            existing = globalValueRepository.save(existing);

            log.info("Updated global value: {}", existing.getValueKey());
            return modelMapper.map(existing, GlobalValueDTO.class);
        });
    }

    @Transactional
    public boolean deleteGlobalValue(Long id) {
        return globalValueRepository.findById(id).map(value -> {
            globalValueRepository.delete(value);
            log.info("Deleted global value: {}", value.getValueKey());
            return true;
        }).orElse(false);
    }
}