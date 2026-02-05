package com.sencours.controller;

import com.sencours.dto.request.CourseRequest;
import com.sencours.dto.response.CourseResponse;
import com.sencours.dto.response.PageResponse;
import com.sencours.enums.Status;
import com.sencours.service.CourseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/courses")
@RequiredArgsConstructor
@Tag(name = "Courses", description = "API de gestion des cours")
public class CourseController {

    private final CourseService courseService;

    @PostMapping
    @Operation(summary = "Créer un cours", description = "Crée un nouveau cours avec un instructeur et une catégorie")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Cours créé avec succès",
                    content = @Content(schema = @Schema(implementation = CourseResponse.class))),
            @ApiResponse(responseCode = "400", description = "Données de requête invalides ou instructeur sans rôle INSTRUCTEUR"),
            @ApiResponse(responseCode = "404", description = "Instructeur ou catégorie non trouvé")
    })
    public ResponseEntity<CourseResponse> create(
            @Valid @RequestBody CourseRequest request) {
        CourseResponse response = courseService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "Lister les cours", description = "Récupère la liste de tous les cours")
    @ApiResponse(responseCode = "200", description = "Liste des cours récupérée avec succès")
    public ResponseEntity<List<CourseResponse>> getAll() {
        List<CourseResponse> courses = courseService.getAll();
        return ResponseEntity.ok(courses);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Récupérer un cours", description = "Récupère un cours par son ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cours trouvé",
                    content = @Content(schema = @Schema(implementation = CourseResponse.class))),
            @ApiResponse(responseCode = "404", description = "Cours non trouvé")
    })
    public ResponseEntity<CourseResponse> getById(
            @Parameter(description = "ID du cours") @PathVariable Long id) {
        CourseResponse response = courseService.getById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/instructor/{instructorId}")
    @Operation(summary = "Cours par instructeur", description = "Récupère les cours d'un instructeur")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Liste des cours de l'instructeur"),
            @ApiResponse(responseCode = "404", description = "Instructeur non trouvé")
    })
    public ResponseEntity<List<CourseResponse>> getByInstructorId(
            @Parameter(description = "ID de l'instructeur") @PathVariable Long instructorId) {
        List<CourseResponse> courses = courseService.getByInstructorId(instructorId);
        return ResponseEntity.ok(courses);
    }

    @GetMapping("/category/{categoryId}")
    @Operation(summary = "Cours par catégorie", description = "Récupère les cours d'une catégorie")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Liste des cours de la catégorie"),
            @ApiResponse(responseCode = "404", description = "Catégorie non trouvée")
    })
    public ResponseEntity<List<CourseResponse>> getByCategoryId(
            @Parameter(description = "ID de la catégorie") @PathVariable Long categoryId) {
        List<CourseResponse> courses = courseService.getByCategoryId(categoryId);
        return ResponseEntity.ok(courses);
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Cours par status", description = "Récupère les cours par status (DRAFT, PUBLISHED, ARCHIVED)")
    @ApiResponse(responseCode = "200", description = "Liste des cours filtrée par status")
    public ResponseEntity<List<CourseResponse>> getByStatus(
            @Parameter(description = "Status du cours") @PathVariable Status status) {
        List<CourseResponse> courses = courseService.getByStatus(status);
        return ResponseEntity.ok(courses);
    }

    @GetMapping("/search")
    @Operation(summary = "Rechercher des cours", description = "Recherche des cours par titre")
    @ApiResponse(responseCode = "200", description = "Liste des cours correspondants")
    public ResponseEntity<List<CourseResponse>> searchByTitle(
            @Parameter(description = "Terme de recherche") @RequestParam String title) {
        List<CourseResponse> courses = courseService.searchByTitle(title);
        return ResponseEntity.ok(courses);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Modifier un cours", description = "Met à jour un cours existant")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cours mis à jour avec succès",
                    content = @Content(schema = @Schema(implementation = CourseResponse.class))),
            @ApiResponse(responseCode = "400", description = "Données de requête invalides"),
            @ApiResponse(responseCode = "404", description = "Cours, instructeur ou catégorie non trouvé")
    })
    public ResponseEntity<CourseResponse> update(
            @Parameter(description = "ID du cours") @PathVariable Long id,
            @Valid @RequestBody CourseRequest request) {
        CourseResponse response = courseService.update(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer un cours", description = "Supprime un cours par son ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Cours supprimé avec succès"),
            @ApiResponse(responseCode = "404", description = "Cours non trouvé")
    })
    public ResponseEntity<Void> delete(
            @Parameter(description = "ID du cours") @PathVariable Long id) {
        courseService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/publish")
    @Operation(summary = "Publier un cours", description = "Change le status du cours à PUBLISHED")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cours publié avec succès",
                    content = @Content(schema = @Schema(implementation = CourseResponse.class))),
            @ApiResponse(responseCode = "404", description = "Cours non trouvé")
    })
    public ResponseEntity<CourseResponse> publish(
            @Parameter(description = "ID du cours") @PathVariable Long id) {
        CourseResponse response = courseService.publish(id);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/archive")
    @Operation(summary = "Archiver un cours", description = "Change le status du cours à ARCHIVED")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cours archivé avec succès",
                    content = @Content(schema = @Schema(implementation = CourseResponse.class))),
            @ApiResponse(responseCode = "404", description = "Cours non trouvé")
    })
    public ResponseEntity<CourseResponse> archive(
            @Parameter(description = "ID du cours") @PathVariable Long id) {
        CourseResponse response = courseService.archive(id);
        return ResponseEntity.ok(response);
    }

    // Paginated endpoints

    @GetMapping("/paginated")
    @Operation(summary = "Lister les cours avec pagination", description = "Récupère la liste paginée de tous les cours")
    @ApiResponse(responseCode = "200", description = "Liste paginée des cours récupérée avec succès")
    public ResponseEntity<PageResponse<CourseResponse>> getAllPaginated(
            @Parameter(description = "Numéro de page (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Taille de la page (1-50)") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Champ de tri") @RequestParam(defaultValue = "id") String sort,
            @Parameter(description = "Direction du tri (asc/desc)") @RequestParam(defaultValue = "asc") String direction) {
        Pageable pageable = createPageable(page, size, sort, direction);
        PageResponse<CourseResponse> response = courseService.getAllPaginated(pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/search/paginated")
    @Operation(summary = "Rechercher des cours avec pagination", description = "Recherche des cours par titre avec pagination")
    @ApiResponse(responseCode = "200", description = "Liste paginée des cours correspondants")
    public ResponseEntity<PageResponse<CourseResponse>> searchByTitlePaginated(
            @Parameter(description = "Terme de recherche") @RequestParam String title,
            @Parameter(description = "Numéro de page (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Taille de la page (1-50)") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Champ de tri") @RequestParam(defaultValue = "id") String sort,
            @Parameter(description = "Direction du tri (asc/desc)") @RequestParam(defaultValue = "asc") String direction) {
        Pageable pageable = createPageable(page, size, sort, direction);
        PageResponse<CourseResponse> response = courseService.searchByTitlePaginated(title, pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/category/{categoryId}/paginated")
    @Operation(summary = "Cours par catégorie avec pagination", description = "Récupère les cours d'une catégorie avec pagination")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Liste paginée des cours de la catégorie"),
            @ApiResponse(responseCode = "404", description = "Catégorie non trouvée")
    })
    public ResponseEntity<PageResponse<CourseResponse>> getByCategoryIdPaginated(
            @Parameter(description = "ID de la catégorie") @PathVariable Long categoryId,
            @Parameter(description = "Numéro de page (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Taille de la page (1-50)") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Champ de tri") @RequestParam(defaultValue = "id") String sort,
            @Parameter(description = "Direction du tri (asc/desc)") @RequestParam(defaultValue = "asc") String direction) {
        Pageable pageable = createPageable(page, size, sort, direction);
        PageResponse<CourseResponse> response = courseService.getByCategoryIdPaginated(categoryId, pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/status/{status}/paginated")
    @Operation(summary = "Cours par status avec pagination", description = "Récupère les cours par status avec pagination")
    @ApiResponse(responseCode = "200", description = "Liste paginée des cours filtrée par status")
    public ResponseEntity<PageResponse<CourseResponse>> getByStatusPaginated(
            @Parameter(description = "Status du cours") @PathVariable Status status,
            @Parameter(description = "Numéro de page (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Taille de la page (1-50)") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Champ de tri") @RequestParam(defaultValue = "id") String sort,
            @Parameter(description = "Direction du tri (asc/desc)") @RequestParam(defaultValue = "asc") String direction) {
        Pageable pageable = createPageable(page, size, sort, direction);
        PageResponse<CourseResponse> response = courseService.getByStatusPaginated(status, pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/instructor/{instructorId}/paginated")
    @Operation(summary = "Cours par instructeur avec pagination", description = "Récupère les cours d'un instructeur avec pagination")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Liste paginée des cours de l'instructeur"),
            @ApiResponse(responseCode = "404", description = "Instructeur non trouvé")
    })
    public ResponseEntity<PageResponse<CourseResponse>> getByInstructorIdPaginated(
            @Parameter(description = "ID de l'instructeur") @PathVariable Long instructorId,
            @Parameter(description = "Numéro de page (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Taille de la page (1-50)") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Champ de tri") @RequestParam(defaultValue = "id") String sort,
            @Parameter(description = "Direction du tri (asc/desc)") @RequestParam(defaultValue = "asc") String direction) {
        Pageable pageable = createPageable(page, size, sort, direction);
        PageResponse<CourseResponse> response = courseService.getByInstructorIdPaginated(instructorId, pageable);
        return ResponseEntity.ok(response);
    }

    private Pageable createPageable(int page, int size, String sort, String direction) {
        int validPage = Math.max(0, page);
        int validSize = Math.min(Math.max(1, size), 50);
        Sort.Direction sortDirection = "desc".equalsIgnoreCase(direction) ? Sort.Direction.DESC : Sort.Direction.ASC;
        return PageRequest.of(validPage, validSize, Sort.by(sortDirection, sort));
    }
}
