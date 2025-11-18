package com.simucredito.property.presentation.controller;

import com.simucredito.property.application.dto.CreatePropertyRequestDTO;
import com.simucredito.property.application.dto.PropertyDTO;
import com.simucredito.property.application.dto.UploadPhotosResponseDTO;
import com.simucredito.property.application.service.PhotoService;
import com.simucredito.property.application.service.PropertyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/properties")
@RequiredArgsConstructor
@Tag(name = "Property Management", description = "Property catalog management APIs")
@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")
public class PropertyController {

    private final PropertyService propertyService;
    private final PhotoService photoService;

    @PostMapping
    @Operation(summary = "Create a new property", description = "Create a new property in the catalog")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Property created successfully",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = PropertyDTO.class))),
        @ApiResponse(responseCode = "400", description = "Invalid input data",
            content = @Content),
        @ApiResponse(responseCode = "409", description = "Property already exists",
            content = @Content)
    })
    public ResponseEntity<PropertyDTO> createProperty(@Valid @RequestBody CreatePropertyRequestDTO request) {
        PropertyDTO property = propertyService.createProperty(request);
        return ResponseEntity.ok(property);
    }

    @GetMapping
    @Operation(summary = "Get all properties", description = "Get all properties for the authenticated user")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Properties retrieved successfully",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = PropertyDTO.class)))
    })
    public ResponseEntity<List<PropertyDTO>> getProperties() {
        List<PropertyDTO> properties = propertyService.getPropertiesByUser();
        return ResponseEntity.ok(properties);
    }

    @GetMapping("/{propertyId}")
    @Operation(summary = "Get property by ID", description = "Get a specific property by ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Property retrieved successfully",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = PropertyDTO.class))),
        @ApiResponse(responseCode = "404", description = "Property not found",
            content = @Content),
        @ApiResponse(responseCode = "403", description = "Access denied",
            content = @Content)
    })
    public ResponseEntity<PropertyDTO> getProperty(@PathVariable Long propertyId) {
        PropertyDTO property = propertyService.getPropertyById(propertyId);
        return ResponseEntity.ok(property);
    }

    @GetMapping("/search")
    @Operation(summary = "Search properties", description = "Search properties with various filters")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Properties found successfully",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = PropertyDTO.class)))
    })
    public ResponseEntity<List<PropertyDTO>> searchProperties(
            @Parameter(description = "Project name to search") @RequestParam(required = false) String projectName,
            @Parameter(description = "Minimum price") @RequestParam(required = false) BigDecimal minPrice,
            @Parameter(description = "Maximum price") @RequestParam(required = false) BigDecimal maxPrice,
            @Parameter(description = "Property type ID") @RequestParam(required = false) Long propertyTypeId,
            @Parameter(description = "Sustainable property filter") @RequestParam(required = false) Boolean isSustainable,
            @Parameter(description = "Property status") @RequestParam(required = false) String status) {

        List<PropertyDTO> properties = propertyService.searchProperties(
                projectName, minPrice, maxPrice, propertyTypeId, isSustainable, status);
        return ResponseEntity.ok(properties);
    }

    @PutMapping("/{propertyId}")
    @Operation(summary = "Update property", description = "Update an existing property")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Property updated successfully",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = PropertyDTO.class))),
        @ApiResponse(responseCode = "404", description = "Property not found",
            content = @Content),
        @ApiResponse(responseCode = "403", description = "Access denied",
            content = @Content)
    })
    public ResponseEntity<PropertyDTO> updateProperty(@PathVariable Long propertyId,
                                                     @Valid @RequestBody CreatePropertyRequestDTO request) {
        PropertyDTO property = propertyService.updateProperty(propertyId, request);
        return ResponseEntity.ok(property);
    }

    @PostMapping("/upload-photos")
    @Operation(summary = "Upload property photos", description = "Upload multiple photos for properties to Firebase Storage")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Photos uploaded successfully",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = UploadPhotosResponseDTO.class))),
        @ApiResponse(responseCode = "400", description = "Invalid files or too many files",
            content = @Content),
        @ApiResponse(responseCode = "500", description = "Upload failed",
            content = @Content)
    })
    public ResponseEntity<UploadPhotosResponseDTO> uploadPhotos(
            @Parameter(description = "Property photos to upload") @RequestParam("files") List<MultipartFile> files)
            throws IOException {
        UploadPhotosResponseDTO response = photoService.uploadPhotos(files);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{propertyId}")
    @Operation(summary = "Delete property", description = "Delete a property from the catalog")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Property deleted successfully",
            content = @Content),
        @ApiResponse(responseCode = "404", description = "Property not found",
            content = @Content),
        @ApiResponse(responseCode = "403", description = "Access denied",
            content = @Content)
    })
    public ResponseEntity<Void> deleteProperty(@PathVariable Long propertyId) {
        propertyService.deleteProperty(propertyId);
        return ResponseEntity.noContent().build();
    }
}