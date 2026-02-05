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
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/sections/{sectionId}/lessons")
@RequiredArgsConstructor
@Tag(name = "Lessons", description = "API de gestion des leçons")
public class LessonController {

    private final LessonService lessonService;

    @PostMapping
    @Operation(summary = "Créer une leçon", description = "Crée une nouvelle leçon dans une section")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Leçon créée avec succès",
                    content = @Content(schema = @Schema(implementation = LessonResponse.class))),
            @ApiResponse(responseCode = "400", description = "Données de requête invalides"),
            @ApiResponse(responseCode = "404", description = "Section non trouvée")
    })
    public ResponseEntity<LessonResponse> create(
            @Parameter(description = "ID de la section") @PathVariable Long sectionId,
            @Valid @RequestBody LessonRequest request) {
        LessonResponse response = lessonService.create(sectionId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
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

    @GetMapping("/{lessonId}")
    @Operation(summary = "Récupérer une leçon", description = "Récupère une leçon par son ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Leçon trouvée",
                    content = @Content(schema = @Schema(implementation = LessonResponse.class))),
            @ApiResponse(responseCode = "404", description = "Leçon non trouvée")
    })
    public ResponseEntity<LessonResponse> getById(
            @Parameter(description = "ID de la section") @PathVariable Long sectionId,
            @Parameter(description = "ID de la leçon") @PathVariable Long lessonId) {
        LessonResponse response = lessonService.getById(lessonId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{lessonId}")
    @Operation(summary = "Modifier une leçon", description = "Met à jour une leçon existante")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Leçon mise à jour avec succès",
                    content = @Content(schema = @Schema(implementation = LessonResponse.class))),
            @ApiResponse(responseCode = "400", description = "Données de requête invalides"),
            @ApiResponse(responseCode = "404", description = "Leçon non trouvée")
    })
    public ResponseEntity<LessonResponse> update(
            @Parameter(description = "ID de la section") @PathVariable Long sectionId,
            @Parameter(description = "ID de la leçon") @PathVariable Long lessonId,
            @Valid @RequestBody LessonRequest request) {
        LessonResponse response = lessonService.update(lessonId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{lessonId}")
    @Operation(summary = "Supprimer une leçon", description = "Supprime une leçon")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Leçon supprimée avec succès"),
            @ApiResponse(responseCode = "404", description = "Leçon non trouvée")
    })
    public ResponseEntity<Void> delete(
            @Parameter(description = "ID de la section") @PathVariable Long sectionId,
            @Parameter(description = "ID de la leçon") @PathVariable Long lessonId) {
        lessonService.delete(lessonId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/reorder")
    @Operation(summary = "Réorganiser les leçons", description = "Réordonne les leçons d'une section")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Leçons réorganisées avec succès"),
            @ApiResponse(responseCode = "400", description = "Liste d'IDs invalide"),
            @ApiResponse(responseCode = "404", description = "Section ou leçon non trouvée")
    })
    public ResponseEntity<List<LessonResponse>> reorder(
            @Parameter(description = "ID de la section") @PathVariable Long sectionId,
            @Valid @RequestBody ReorderRequest request) {
        List<LessonResponse> lessons = lessonService.reorder(sectionId, request);
        return ResponseEntity.ok(lessons);
    }
}
