package com.sencours.controller;

import com.sencours.dto.response.ProgressResponse;
import com.sencours.service.ProgressService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/enrollments/{enrollmentId}/lessons")
@RequiredArgsConstructor
@Tag(name = "Progress", description = "API de gestion de la progression des leçons")
public class ProgressController {

    private final ProgressService progressService;

    @GetMapping
    @Operation(summary = "Liste des progressions", description = "Récupère la progression de toutes les leçons pour une inscription")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Liste des progressions récupérée avec succès"),
            @ApiResponse(responseCode = "404", description = "Inscription non trouvée")
    })
    public ResponseEntity<List<ProgressResponse>> getProgressByEnrollment(
            @Parameter(description = "ID de l'inscription") @PathVariable Long enrollmentId) {
        List<ProgressResponse> progresses = progressService.getProgressByEnrollment(enrollmentId);
        return ResponseEntity.ok(progresses);
    }

    @GetMapping("/{lessonId}")
    @Operation(summary = "Progression d'une leçon", description = "Récupère la progression d'une leçon spécifique")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Progression récupérée avec succès",
                    content = @Content(schema = @Schema(implementation = ProgressResponse.class))),
            @ApiResponse(responseCode = "404", description = "Progression non trouvée")
    })
    public ResponseEntity<ProgressResponse> getProgress(
            @Parameter(description = "ID de l'inscription") @PathVariable Long enrollmentId,
            @Parameter(description = "ID de la leçon") @PathVariable Long lessonId) {
        ProgressResponse response = progressService.getProgress(enrollmentId, lessonId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{lessonId}/complete")
    @Operation(summary = "Marquer complétée", description = "Marque une leçon comme complétée")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Leçon marquée comme complétée avec succès",
                    content = @Content(schema = @Schema(implementation = ProgressResponse.class))),
            @ApiResponse(responseCode = "404", description = "Progression non trouvée")
    })
    public ResponseEntity<ProgressResponse> markLessonCompleted(
            @Parameter(description = "ID de l'inscription") @PathVariable Long enrollmentId,
            @Parameter(description = "ID de la leçon") @PathVariable Long lessonId) {
        ProgressResponse response = progressService.markLessonCompleted(enrollmentId, lessonId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{lessonId}/incomplete")
    @Operation(summary = "Marquer non complétée", description = "Marque une leçon comme non complétée")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Leçon marquée comme non complétée avec succès",
                    content = @Content(schema = @Schema(implementation = ProgressResponse.class))),
            @ApiResponse(responseCode = "404", description = "Progression non trouvée")
    })
    public ResponseEntity<ProgressResponse> markLessonIncomplete(
            @Parameter(description = "ID de l'inscription") @PathVariable Long enrollmentId,
            @Parameter(description = "ID de la leçon") @PathVariable Long lessonId) {
        ProgressResponse response = progressService.markLessonIncomplete(enrollmentId, lessonId);
        return ResponseEntity.ok(response);
    }
}
