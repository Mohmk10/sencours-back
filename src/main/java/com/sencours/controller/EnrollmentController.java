package com.sencours.controller;

import com.sencours.dto.request.EnrollmentRequest;
import com.sencours.dto.response.EnrollmentDetailResponse;
import com.sencours.dto.response.EnrollmentResponse;
import com.sencours.dto.response.ProgressSummaryResponse;
import com.sencours.entity.User;
import com.sencours.service.EnrollmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/enrollments")
@RequiredArgsConstructor
@Tag(name = "Enrollments", description = "API de gestion des inscriptions aux cours")
@SecurityRequirement(name = "bearerAuth")
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    @PostMapping
    @Operation(summary = "S'inscrire à un cours", description = "Crée une nouvelle inscription pour l'étudiant connecté")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Inscription créée avec succès",
                    content = @Content(schema = @Schema(implementation = EnrollmentResponse.class))),
            @ApiResponse(responseCode = "400", description = "Données de requête invalides ou règles métier non respectées"),
            @ApiResponse(responseCode = "401", description = "Non authentifié"),
            @ApiResponse(responseCode = "404", description = "Cours non trouvé"),
            @ApiResponse(responseCode = "409", description = "L'utilisateur est déjà inscrit à ce cours")
    })
    public ResponseEntity<EnrollmentResponse> enroll(
            @Valid @RequestBody EnrollmentRequest request,
            @AuthenticationPrincipal User currentUser) {
        EnrollmentResponse response = enrollmentService.enroll(currentUser.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/me")
    @Operation(summary = "Mes inscriptions", description = "Récupère toutes les inscriptions de l'utilisateur connecté")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Liste des inscriptions récupérée avec succès"),
            @ApiResponse(responseCode = "401", description = "Non authentifié")
    })
    public ResponseEntity<List<EnrollmentResponse>> getMyEnrollments(
            @AuthenticationPrincipal User currentUser) {
        List<EnrollmentResponse> enrollments = enrollmentService.getMyEnrollments(currentUser.getId());
        return ResponseEntity.ok(enrollments);
    }

    @GetMapping("/{enrollmentId}")
    @Operation(summary = "Détail d'une inscription", description = "Récupère le détail d'une inscription avec la progression")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Détail de l'inscription récupéré avec succès",
                    content = @Content(schema = @Schema(implementation = EnrollmentDetailResponse.class))),
            @ApiResponse(responseCode = "401", description = "Non authentifié"),
            @ApiResponse(responseCode = "404", description = "Inscription non trouvée")
    })
    public ResponseEntity<EnrollmentDetailResponse> getEnrollmentDetail(
            @Parameter(description = "ID de l'inscription") @PathVariable Long enrollmentId) {
        EnrollmentDetailResponse response = enrollmentService.getEnrollmentDetail(enrollmentId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{enrollmentId}/progress")
    @Operation(summary = "Résumé de progression", description = "Récupère le résumé de progression pour une inscription")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Résumé de progression récupéré avec succès",
                    content = @Content(schema = @Schema(implementation = ProgressSummaryResponse.class))),
            @ApiResponse(responseCode = "401", description = "Non authentifié"),
            @ApiResponse(responseCode = "404", description = "Inscription non trouvée")
    })
    public ResponseEntity<ProgressSummaryResponse> getProgressSummary(
            @Parameter(description = "ID de l'inscription") @PathVariable Long enrollmentId) {
        ProgressSummaryResponse response = enrollmentService.calculateProgress(enrollmentId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/course/{courseId}")
    @Operation(summary = "Inscriptions par cours", description = "Récupère toutes les inscriptions pour un cours (Admin/Instructeur)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Liste des inscriptions récupérée avec succès"),
            @ApiResponse(responseCode = "401", description = "Non authentifié"),
            @ApiResponse(responseCode = "404", description = "Cours non trouvé")
    })
    public ResponseEntity<List<EnrollmentResponse>> getEnrollmentsByCourse(
            @Parameter(description = "ID du cours") @PathVariable Long courseId) {
        List<EnrollmentResponse> enrollments = enrollmentService.getEnrollmentsByCourse(courseId);
        return ResponseEntity.ok(enrollments);
    }

    @DeleteMapping("/{enrollmentId}")
    @Operation(summary = "Se désinscrire", description = "Supprime l'inscription de l'utilisateur connecté")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Désinscription effectuée avec succès"),
            @ApiResponse(responseCode = "401", description = "Non authentifié"),
            @ApiResponse(responseCode = "403", description = "Vous ne pouvez pas vous désinscrire des inscriptions d'autres utilisateurs"),
            @ApiResponse(responseCode = "404", description = "Inscription non trouvée")
    })
    public ResponseEntity<Void> unenroll(
            @Parameter(description = "ID de l'inscription") @PathVariable Long enrollmentId,
            @AuthenticationPrincipal User currentUser) {
        enrollmentService.unenroll(enrollmentId, currentUser.getId());
        return ResponseEntity.noContent().build();
    }
}
