package com.simucredito.property.infrastructure.service;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.*;
import com.simucredito.property.application.dto.UploadPhotosResponseDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class FirebaseStorageService {

    @Value("${firebase.storage.bucket}")
    private String bucketName;

    @Value("${firebase.credentials.path}")
    private String credentialsPath;

    @Value("${firebase.temp-photos.expiry-hours:24}")
    private int tempPhotoExpiryHours;

    private Storage getStorage() throws IOException {
        GoogleCredentials credentials = GoogleCredentials.fromStream(
            new ClassPathResource(credentialsPath).getInputStream()
        );
        return StorageOptions.newBuilder()
            .setCredentials(credentials)
            .build()
            .getService();
    }

    public UploadPhotosResponseDTO uploadPhotos(List<MultipartFile> files, Long userId) throws IOException {
        Storage storage = getStorage();
        Bucket bucket = storage.get(bucketName);

        List<UploadPhotosResponseDTO.PhotoMetadataDTO> uploadedPhotos = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiryTime = now.plusHours(tempPhotoExpiryHours);

        for (MultipartFile file : files) {
            if (file.isEmpty()) {
                continue;
            }

            // Generate unique filename
            String originalFilename = file.getOriginalFilename();
            String extension = getFileExtension(originalFilename);
            String uniqueFilename = generateUniqueFilename(userId, extension);

            // Create blob info
            BlobId blobId = BlobId.of(bucketName, uniqueFilename);
            BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                .setContentType(file.getContentType())
                .setMetadata(createMetadata(userId, now, expiryTime))
                .build();

            // Upload file
            try {
                Blob blob = storage.create(blobInfo, file.getBytes());

                // Generate public URL (temporary access)
                String publicUrl = generatePublicUrl(blob, tempPhotoExpiryHours);

                UploadPhotosResponseDTO.PhotoMetadataDTO metadata = UploadPhotosResponseDTO.PhotoMetadataDTO.builder()
                    .id(uniqueFilename)
                    .originalFilename(originalFilename)
                    .firebaseUrl(publicUrl)
                    .contentType(file.getContentType())
                    .size(file.getSize())
                    .uploadedAt(now)
                    .expiresAt(expiryTime)
                    .build();

                uploadedPhotos.add(metadata);

                log.info("Photo uploaded successfully: {}", uniqueFilename);

            } catch (Exception e) {
                log.error("Failed to upload photo: {}", originalFilename, e);
                throw new RuntimeException("Failed to upload photo: " + originalFilename, e);
            }
        }

        return UploadPhotosResponseDTO.builder()
            .photos(uploadedPhotos)
            .message("Photos uploaded successfully")
            .expiryTime(expiryTime)
            .build();
    }

    public void deletePhoto(String photoId) throws IOException {
        Storage storage = getStorage();
        BlobId blobId = BlobId.of(bucketName, photoId);

        boolean deleted = storage.delete(blobId);
        if (deleted) {
            log.info("Photo deleted successfully: {}", photoId);
        } else {
            log.warn("Photo not found or already deleted: {}", photoId);
        }
    }

    public void deletePhotos(List<String> photoIds) throws IOException {
        Storage storage = getStorage();

        List<BlobId> blobIds = photoIds.stream()
            .map(photoId -> BlobId.of(bucketName, photoId))
            .toList();

        List<Boolean> results = storage.delete(blobIds);

        for (int i = 0; i < results.size(); i++) {
            if (results.get(i)) {
                log.info("Photo deleted: {}", photoIds.get(i));
            } else {
                log.warn("Photo not found: {}", photoIds.get(i));
            }
        }
    }

    public boolean photoExists(String photoId) throws IOException {
        Storage storage = getStorage();
        BlobId blobId = BlobId.of(bucketName, photoId);
        Blob blob = storage.get(blobId);
        return blob != null && blob.exists();
    }

    public String uploadFinancialEntityPhoto(MultipartFile file, String entityCode) throws IOException {
        Storage storage = getStorage();
        Bucket bucket = storage.get(bucketName);

        // Validate file
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File cannot be empty");
        }

        // Generate unique filename for financial entity
        String originalFilename = file.getOriginalFilename();
        String extension = getFileExtension(originalFilename);
        String uniqueFilename = generateFinancialEntityFilename(entityCode, extension);

        // Create blob info
        BlobId blobId = BlobId.of(bucketName, uniqueFilename);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
            .setContentType(file.getContentType())
            .setMetadata(java.util.Map.of(
                "entityCode", entityCode,
                "uploadedAt", LocalDateTime.now().toString(),
                "type", "financial_entity_photo"
            ))
            .build();

        // Upload file
        try {
            Blob blob = storage.create(blobInfo, file.getBytes());

            // Generate permanent public URL (no expiry for entity photos)
            String publicUrl = blob.getMediaLink();

            log.info("Financial entity photo uploaded successfully: {}", uniqueFilename);
            return publicUrl;

        } catch (Exception e) {
            log.error("Failed to upload financial entity photo: {}", originalFilename, e);
            throw new RuntimeException("Failed to upload financial entity photo: " + originalFilename, e);
        }
    }

    private String generateFinancialEntityFilename(String entityCode, String extension) {
        String timestamp = String.valueOf(System.currentTimeMillis());
        return String.format("financial-entities/%s/%s.%s", entityCode, timestamp, extension);
    }

    private String generateUniqueFilename(Long userId, String extension) {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String uuid = UUID.randomUUID().toString().substring(0, 8);
        return String.format("temp/%d/%s_%s.%s", userId, timestamp, uuid, extension);
    }

    private String getFileExtension(String filename) {
        if (filename == null || filename.lastIndexOf('.') == -1) {
            return "jpg"; // default extension
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }

    private String generatePublicUrl(Blob blob, int expiryHours) {
        return blob.signUrl(expiryHours, TimeUnit.HOURS).toString();
    }

    private java.util.Map<String, String> createMetadata(Long userId, LocalDateTime uploadedAt, LocalDateTime expiresAt) {
        return java.util.Map.of(
            "userId", userId.toString(),
            "uploadedAt", uploadedAt.toString(),
            "expiresAt", expiresAt.toString(),
            "temp", "true"
        );
    }
}