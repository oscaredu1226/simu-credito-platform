package com.simucredito.property.application.service;

import com.simucredito.property.application.dto.UploadPhotosResponseDTO;
import com.simucredito.property.infrastructure.service.FirebaseStorageService;
import com.simucredito.iam.domain.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PhotoService {

    private final FirebaseStorageService firebaseStorageService;

    public UploadPhotosResponseDTO uploadPhotos(List<MultipartFile> files) throws IOException {
        Long userId = getCurrentUserId();

        // Validate files
        validateFiles(files);

        // Upload to Firebase
        UploadPhotosResponseDTO response = firebaseStorageService.uploadPhotos(files, userId);

        log.info("User {} uploaded {} photos successfully", userId, files.size());
        return response;
    }

    public void deletePhoto(String photoId) throws IOException {
        firebaseStorageService.deletePhoto(photoId);
        log.info("Photo deleted: {}", photoId);
    }

    public void deletePhotos(List<String> photoIds) throws IOException {
        firebaseStorageService.deletePhotos(photoIds);
        log.info("Deleted {} photos", photoIds.size());
    }

    public boolean validatePhotoUrls(String[] photoUrls) {
        if (photoUrls == null || photoUrls.length == 0) {
            return true; // Allow properties without photos
        }

        // Basic URL validation - check if they look like Firebase URLs
        for (String url : photoUrls) {
            if (url == null || url.trim().isEmpty()) {
                continue;
            }

            if (!isValidFirebaseUrl(url)) {
                log.warn("Invalid Firebase URL: {}", url);
                return false;
            }

            // TODO: Optionally check if the URL actually exists by making a HEAD request
            // For now, we trust the URLs provided by our upload endpoint
        }

        return true;
    }

    private void validateFiles(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            throw new IllegalArgumentException("At least one file must be provided");
        }

        if (files.size() > 10) {
            throw new IllegalArgumentException("Maximum 10 photos allowed per upload");
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

    private boolean isValidFirebaseUrl(String url) {
        return url != null &&
               (url.startsWith("https://firebasestorage.googleapis.com/") ||
                url.startsWith("https://storage.googleapis.com/"));
    }

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof User) {
            User user = (User) authentication.getPrincipal();
            return user.getId();
        }
        throw new RuntimeException("User not authenticated");
    }
}