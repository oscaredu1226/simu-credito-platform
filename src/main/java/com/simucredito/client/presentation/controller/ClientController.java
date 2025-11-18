package com.simucredito.client.presentation.controller;

import com.simucredito.client.application.dto.ClientDTO;
import com.simucredito.client.application.dto.CreateClientRequestDTO;
import com.simucredito.client.application.dto.PersonDTO;
import com.simucredito.client.application.dto.PreQualificationRequestDTO;
import com.simucredito.client.application.dto.PreQualificationResponseDTO;
import com.simucredito.client.application.service.ClientService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/clients")
@RequiredArgsConstructor
@Tag(name = "Client Management", description = "Client management and pre-qualification APIs")
@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")
public class ClientController {

    private final ClientService clientService;

    @PostMapping
    @Operation(summary = "Create a new client", description = "Create a new client with holder and optional spouse information")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Client created successfully",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ClientDTO.class))),
        @ApiResponse(responseCode = "400", description = "Invalid input data",
            content = @Content),
        @ApiResponse(responseCode = "409", description = "Document number already exists",
            content = @Content)
    })
    public ResponseEntity<ClientDTO> createClient(@Valid @RequestBody CreateClientRequestDTO request) {
        ClientDTO client = clientService.createClient(request);
        return ResponseEntity.ok(client);
    }

    @GetMapping
    @Operation(summary = "Get all clients", description = "Get all clients for the authenticated user")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Clients retrieved successfully",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ClientDTO.class)))
    })
    public ResponseEntity<List<ClientDTO>> getClients() {
        List<ClientDTO> clients = clientService.getClientsByUser();
        return ResponseEntity.ok(clients);
    }

    @GetMapping("/{clientId}")
    @Operation(summary = "Get client by ID", description = "Get a specific client by ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Client retrieved successfully",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ClientDTO.class))),
        @ApiResponse(responseCode = "404", description = "Client not found",
            content = @Content),
        @ApiResponse(responseCode = "403", description = "Access denied",
            content = @Content)
    })
    public ResponseEntity<ClientDTO> getClient(@PathVariable Long clientId) {
        ClientDTO client = clientService.getClientById(clientId);
        return ResponseEntity.ok(client);
    }

    @PutMapping("/{clientId}")
    @Operation(summary = "Update client", description = "Update an existing client with new information and recalculate pre-qualification")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Client updated successfully",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ClientDTO.class))),
        @ApiResponse(responseCode = "400", description = "Invalid input data",
            content = @Content),
        @ApiResponse(responseCode = "404", description = "Client not found",
            content = @Content),
        @ApiResponse(responseCode = "403", description = "Access denied",
            content = @Content)
    })
    public ResponseEntity<ClientDTO> updateClient(@PathVariable Long clientId, @Valid @RequestBody CreateClientRequestDTO request) {
        ClientDTO client = clientService.updateClient(clientId, request);
        return ResponseEntity.ok(client);
    }

    @PostMapping("/pre-qualification")
    @Operation(summary = "Perform pre-qualification based on client data", description = "Perform pre-qualification analysis for bonus eligibility based on client socioeconomic data. This endpoint is publicly accessible.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Pre-qualification completed successfully",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = PreQualificationResponseDTO.class))),
        @ApiResponse(responseCode = "400", description = "Invalid input data",
            content = @Content)
    })
    public ResponseEntity<PreQualificationResponseDTO> performPreQualification(@Valid @RequestBody PreQualificationRequestDTO request) {
        PreQualificationResponseDTO response = clientService.performPreQualification(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{clientId}/pre-qualification")
    @Operation(summary = "Perform pre-qualification for registered client", description = "Perform pre-qualification analysis for bonus eligibility on an existing registered client")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Pre-qualification completed successfully",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = PreQualificationResponseDTO.class))),
        @ApiResponse(responseCode = "404", description = "Client not found",
            content = @Content),
        @ApiResponse(responseCode = "403", description = "Access denied",
            content = @Content)
    })
    public ResponseEntity<PreQualificationResponseDTO> performPreQualificationForClient(@PathVariable Long clientId) {
        PreQualificationResponseDTO response = clientService.performPreQualification(clientId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{clientId}")
    @Operation(summary = "Delete client", description = "Delete a client and associated data")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Client deleted successfully",
            content = @Content),
        @ApiResponse(responseCode = "404", description = "Client not found",
            content = @Content),
        @ApiResponse(responseCode = "403", description = "Access denied",
            content = @Content)
    })
    public ResponseEntity<Void> deleteClient(@PathVariable Long clientId) {
        clientService.deleteClient(clientId);
        return ResponseEntity.noContent().build();
    }
}