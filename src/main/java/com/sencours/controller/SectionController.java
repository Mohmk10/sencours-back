package com.sencours.controller;

import com.sencours.dto.request.ReorderRequest;
import com.sencours.dto.request.SectionRequest;
import com.sencours.dto.response.SectionResponse;
import com.sencours.service.SectionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Sections", description = "API de gestion des sections de cours")
public class SectionController {

    private final SectionService sectionService;

    @PostMapping("/courses/{courseId}/sections")
    @Operation(summary = "Créer une section", description = "Crée une nouvelle section dans un cours")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Section créée avec succès",
                    content = @Content(schema = @Schema(implementation = SectionResponse.class))),
            @ApiResponse(responseCode = "400", description = "Données de requête invalides"),
            @ApiResponse(responseCode = "404", description = "Cours non trouvé")
    })
    @PreAuthorize("hasAnyRole('INSTRUCTEUR', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<SectionResponse> create(
            @Parameter(description = "ID du cours") @PathVariable Long courseId,
            @Valid @RequestBody SectionRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        SectionResponse response = sectionService.create(courseId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/courses/{courseId}/sections")
    @Operation(summary = "Lister les sections", description = "Récupère toutes les sections d'un cours ordonnées")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Liste des sections récupérée avec succès"),
            @ApiResponse(responseCode = "404", description = "Cours non trouvé")
    })
    public ResponseEntity<List<SectionResponse>> getByCourseId(
            @Parameter(description = "ID du cours") @PathVariable Long courseId) {
        List<SectionResponse> sections = sectionService.getByCourseId(courseId);
        return ResponseEntity.ok(sections);
    }

    @GetMapping("/sections/{id}")
    @Operation(summary = "Récupérer une section", description = "Récupère une section par son ID avec ses leçons")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Section trouvée",
                    content = @Content(schema = @Schema(implementation = SectionResponse.class))),
            @ApiResponse(responseCode = "404", description = "Section non trouvée")
    })
    public ResponseEntity<SectionResponse> getById(
            @Parameter(description = "ID de la section") @PathVariable Long id) {
        SectionResponse response = sectionService.getById(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/sections/{id}")
    @Operation(summary = "Modifier une section", description = "Met à jour une section existante")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Section mise à jour avec succès",
                    content = @Content(schema = @Schema(implementation = SectionResponse.class))),
            @ApiResponse(responseCode = "400", description = "Données de requête invalides"),
            @ApiResponse(responseCode = "404", description = "Section non trouvée")
    })
    @PreAuthorize("hasAnyRole('INSTRUCTEUR', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<SectionResponse> update(
            @Parameter(description = "ID de la section") @PathVariable Long id,
            @Valid @RequestBody SectionRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        SectionResponse response = sectionService.update(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/sections/{id}")
    @Operation(summary = "Supprimer une section", description = "Supprime une section et toutes ses leçons")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Section supprimée avec succès"),
            @ApiResponse(responseCode = "403", description = "Droits insuffisants"),
            @ApiResponse(responseCode = "404", description = "Section non trouvée")
    })
    @PreAuthorize("hasAnyRole('INSTRUCTEUR', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Void> delete(
            @Parameter(description = "ID de la section") @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        sectionService.delete(id, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/courses/{courseId}/sections/reorder")
    @Operation(summary = "Réorganiser les sections", description = "Réordonne les sections d'un cours")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Sections réorganisées avec succès"),
            @ApiResponse(responseCode = "400", description = "Liste d'IDs invalide"),
            @ApiResponse(responseCode = "404", description = "Cours ou section non trouvé")
    })
    @PreAuthorize("hasAnyRole('INSTRUCTEUR', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<List<SectionResponse>> reorder(
            @Parameter(description = "ID du cours") @PathVariable Long courseId,
            @Valid @RequestBody ReorderRequest request) {
        List<SectionResponse> sections = sectionService.reorder(courseId, request);
        return ResponseEntity.ok(sections);
    }
}
