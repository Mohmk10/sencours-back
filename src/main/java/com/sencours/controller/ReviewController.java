package com.sencours.controller;

import com.sencours.dto.request.ReviewRequest;
import com.sencours.dto.request.ReviewUpdateRequest;
import com.sencours.dto.response.CourseRatingResponse;
import com.sencours.dto.response.ReviewResponse;
import com.sencours.entity.User;
import com.sencours.service.ReviewService;
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
@RequestMapping("/api/v1/courses/{courseId}/reviews")
@RequiredArgsConstructor
@Tag(name = "Reviews", description = "API de gestion des avis sur les cours")
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping
    @Operation(summary = "Créer un avis", description = "Permet à un étudiant inscrit de laisser un avis sur un cours",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Avis créé avec succès",
                    content = @Content(schema = @Schema(implementation = ReviewResponse.class))),
            @ApiResponse(responseCode = "400", description = "Données de requête invalides"),
            @ApiResponse(responseCode = "401", description = "Non authentifié"),
            @ApiResponse(responseCode = "403", description = "Non autorisé (pas inscrit ou instructeur du cours)"),
            @ApiResponse(responseCode = "404", description = "Cours non trouvé"),
            @ApiResponse(responseCode = "409", description = "L'utilisateur a déjà noté ce cours")
    })
    public ResponseEntity<ReviewResponse> createReview(
            @Parameter(description = "ID du cours") @PathVariable Long courseId,
            @Valid @RequestBody ReviewRequest request,
            @AuthenticationPrincipal User currentUser) {
        ReviewResponse response = reviewService.createReview(courseId, currentUser.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "Liste des avis", description = "Récupère tous les avis d'un cours (public)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Liste des avis récupérée avec succès"),
            @ApiResponse(responseCode = "404", description = "Cours non trouvé")
    })
    public ResponseEntity<List<ReviewResponse>> getReviewsByCourse(
            @Parameter(description = "ID du cours") @PathVariable Long courseId) {
        List<ReviewResponse> reviews = reviewService.getReviewsByCourse(courseId);
        return ResponseEntity.ok(reviews);
    }

    @GetMapping("/{reviewId}")
    @Operation(summary = "Détail d'un avis", description = "Récupère un avis par son ID (public)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Avis trouvé",
                    content = @Content(schema = @Schema(implementation = ReviewResponse.class))),
            @ApiResponse(responseCode = "404", description = "Avis ou cours non trouvé")
    })
    public ResponseEntity<ReviewResponse> getReviewById(
            @Parameter(description = "ID du cours") @PathVariable Long courseId,
            @Parameter(description = "ID de l'avis") @PathVariable Long reviewId) {
        ReviewResponse response = reviewService.getReviewById(courseId, reviewId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{reviewId}")
    @Operation(summary = "Modifier son avis", description = "Permet à l'auteur de modifier son avis",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Avis modifié avec succès",
                    content = @Content(schema = @Schema(implementation = ReviewResponse.class))),
            @ApiResponse(responseCode = "400", description = "Données de requête invalides"),
            @ApiResponse(responseCode = "401", description = "Non authentifié"),
            @ApiResponse(responseCode = "403", description = "Non autorisé (pas l'auteur de l'avis)"),
            @ApiResponse(responseCode = "404", description = "Avis ou cours non trouvé")
    })
    public ResponseEntity<ReviewResponse> updateReview(
            @Parameter(description = "ID du cours") @PathVariable Long courseId,
            @Parameter(description = "ID de l'avis") @PathVariable Long reviewId,
            @Valid @RequestBody ReviewUpdateRequest request,
            @AuthenticationPrincipal User currentUser) {
        ReviewResponse response = reviewService.updateReview(courseId, reviewId, currentUser.getId(), request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{reviewId}")
    @Operation(summary = "Supprimer son avis", description = "Permet à l'auteur de supprimer son avis",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Avis supprimé avec succès"),
            @ApiResponse(responseCode = "401", description = "Non authentifié"),
            @ApiResponse(responseCode = "403", description = "Non autorisé (pas l'auteur de l'avis)"),
            @ApiResponse(responseCode = "404", description = "Avis ou cours non trouvé")
    })
    public ResponseEntity<Void> deleteReview(
            @Parameter(description = "ID du cours") @PathVariable Long courseId,
            @Parameter(description = "ID de l'avis") @PathVariable Long reviewId,
            @AuthenticationPrincipal User currentUser) {
        reviewService.deleteReview(courseId, reviewId, currentUser.getId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/rating")
    @Operation(summary = "Moyenne du cours", description = "Récupère la moyenne des notes et le nombre d'avis (public)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Statistiques récupérées avec succès",
                    content = @Content(schema = @Schema(implementation = CourseRatingResponse.class))),
            @ApiResponse(responseCode = "404", description = "Cours non trouvé")
    })
    public ResponseEntity<CourseRatingResponse> getCourseRating(
            @Parameter(description = "ID du cours") @PathVariable Long courseId) {
        CourseRatingResponse response = reviewService.getCourseRating(courseId);
        return ResponseEntity.ok(response);
    }
}
