package com.simucredito.configuration.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UploadFinancialEntityPhotosResponseDTO {

    private List<PhotoMetadataDTO> photos;
    private String message;
    private LocalDateTime expiryTime;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PhotoMetadataDTO {
        private String id;
        private String originalFilename;
        private String firebaseUrl;
        private String contentType;
        private Long size;
        private LocalDateTime uploadedAt;
        private LocalDateTime expiresAt;
    }
}