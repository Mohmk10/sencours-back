package com.sencours.controller;

import com.sencours.dto.request.LessonRequest;
import com.sencours.dto.request.ReorderRequest;
import com.sencours.dto.response.LessonResponse;
import com.sencours.service.LessonService;
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
@Tag(name = "Lessons", description = "API de gestion des leçons")
public class LessonController {

    private final LessonService lessonService;

    @PostMapping("/sections/{sectionId}/lessons")
    @Operation(summary = "Créer une leçon", description = "Crée une nouvelle leçon dans une section")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Leçon créée avec succès",
                    content = @Content(schema = @Schema(implementation = LessonResponse.class))),
            @ApiResponse(responseCode = "400", description = "Données de requête invalides"),
            @ApiResponse(responseCode = "404", description = "Section non trouvée")
    })
    @PreAuthorize("hasAnyRole('INSTRUCTEUR', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<LessonResponse> create(
            @Parameter(description = "ID de la section") @PathVariable Long sectionId,
            @Valid @RequestBody LessonRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        LessonResponse response = lessonService.create(sectionId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/sections/{sectionId}/lessons")
    @Operation(summary = "Lister les leçons", description = "Récupère toutes les leçons d'une section ordonnées")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Liste des leçons récupérée avec succès"),
            @ApiResponse(responseCode = "404", description = "Section non trouvée")
    })
    public ResponseEntity<List<LessonResponse>> getBySectionId(
            @Parameter(description = "ID de la section") @PathVariable Long sectionId) {
        List<LessonResponse> lessons = lessonService.getBySectionId(sectionId);
        return ResponseEntity.ok(lessons);
    }

    @GetMapping("/lessons/{id}")
    @Operation(summary = "Récupérer une leçon", description = "Récupère une leçon par son ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Leçon trouvée",
                    content = @Content(schema = @Schema(implementation = LessonResponse.class))),
            @ApiResponse(responseCode = "404", description = "Leçon non trouvée")
    })
    public ResponseEntity<LessonResponse> getById(
            @Parameter(description = "ID de la leçon") @PathVariable Long id) {
        LessonResponse response = lessonService.getById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/lessons/{id}/preview")
    @Operation(summary = "Preview d'une leçon gratuite", description = "Récupère une leçon gratuite sans authentification")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Leçon gratuite récupérée",
                    content = @Content(schema = @Schema(implementation = LessonResponse.class))),
            @ApiResponse(responseCode = "403", description = "Leçon non gratuite"),
            @ApiResponse(responseCode = "404", description = "Leçon non trouvée")
    })
    public ResponseEntity<LessonResponse> getPreview(
            @Parameter(description = "ID de la leçon") @PathVariable Long id) {
        LessonResponse response = lessonService.getPreview(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/lessons/{id}/content")
    @Operation(summary = "Récupérer le contenu d'une leçon", description = "Récupère une leçon avec vérification d'accès (inscription ou leçon gratuite)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Contenu de la leçon récupéré",
                    content = @Content(schema = @Schema(implementation = LessonResponse.class))),
            @ApiResponse(responseCode = "403", description = "Accès refusé"),
            @ApiResponse(responseCode = "404", description = "Leçon non trouvée")
    })
    public ResponseEntity<LessonResponse> getLessonContent(
            @Parameter(description = "ID de la leçon") @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        String email = userDetails != null ? userDetails.getUsername() : null;
        LessonResponse response = lessonService.getLessonWithAccessCheck(id, email);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/lessons/{id}")
    @Operation(summary = "Modifier une leçon", description = "Met à jour une leçon existante")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Leçon mise à jour avec succès",
                    content = @Content(schema = @Schema(implementation = LessonResponse.class))),
            @ApiResponse(responseCode = "400", description = "Données de requête invalides"),
            @ApiResponse(responseCode = "404", description = "Leçon non trouvée")
    })
    @PreAuthorize("hasAnyRole('INSTRUCTEUR', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<LessonResponse> update(
            @Parameter(description = "ID de la leçon") @PathVariable Long id,
            @Valid @RequestBody LessonRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        LessonResponse response = lessonService.update(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/lessons/{id}")
    @Operation(summary = "Supprimer une leçon", description = "Supprime une leçon")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Leçon supprimée avec succès"),
            @ApiResponse(responseCode = "403", description = "Droits insuffisants"),
            @ApiResponse(responseCode = "404", description = "Leçon non trouvée")
    })
    @PreAuthorize("hasAnyRole('INSTRUCTEUR', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Void> delete(
            @Parameter(description = "ID de la leçon") @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        lessonService.delete(id, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/sections/{sectionId}/lessons/reorder")
    @Operation(summary = "Réorganiser les leçons", description = "Réordonne les leçons d'une section")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Leçons réorganisées avec succès"),
            @ApiResponse(responseCode = "400", description = "Liste d'IDs invalide"),
            @ApiResponse(responseCode = "404", description = "Section ou leçon non trouvée")
    })
    @PreAuthorize("hasAnyRole('INSTRUCTEUR', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<List<LessonResponse>> reorder(
            @Parameter(description = "ID de la section") @PathVariable Long sectionId,
            @Valid @RequestBody ReorderRequest request) {
        List<LessonResponse> lessons = lessonService.reorder(sectionId, request);
        return ResponseEntity.ok(lessons);
    }
}
