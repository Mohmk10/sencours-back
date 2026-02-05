package com.sencours.controller;

import com.sencours.dto.request.CategoryRequest;
import com.sencours.dto.response.CategoryResponse;
import com.sencours.dto.response.PageResponse;
import com.sencours.service.CategoryService;
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
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
@Tag(name = "Categories", description = "API de gestion des catégories de cours")
public class CategoryController {

    private final CategoryService categoryService;

    @PostMapping
    @Operation(summary = "Créer une catégorie", description = "Crée une nouvelle catégorie de cours")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Catégorie créée avec succès",
                    content = @Content(schema = @Schema(implementation = CategoryResponse.class))),
            @ApiResponse(responseCode = "400", description = "Données de requête invalides"),
            @ApiResponse(responseCode = "409", description = "Une catégorie avec ce nom existe déjà")
    })
    public ResponseEntity<CategoryResponse> create(
            @Valid @RequestBody CategoryRequest request) {
        CategoryResponse response = categoryService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "Lister les catégories", description = "Récupère la liste de toutes les catégories")
    @ApiResponse(responseCode = "200", description = "Liste des catégories récupérée avec succès")
    public ResponseEntity<List<CategoryResponse>> getAll() {
        List<CategoryResponse> categories = categoryService.getAll();
        return ResponseEntity.ok(categories);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Récupérer une catégorie", description = "Récupère une catégorie par son ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Catégorie trouvée",
                    content = @Content(schema = @Schema(implementation = CategoryResponse.class))),
            @ApiResponse(responseCode = "404", description = "Catégorie non trouvée")
    })
    public ResponseEntity<CategoryResponse> getById(
            @Parameter(description = "ID de la catégorie") @PathVariable Long id) {
        CategoryResponse response = categoryService.getById(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Modifier une catégorie", description = "Met à jour une catégorie existante")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Catégorie mise à jour avec succès",
                    content = @Content(schema = @Schema(implementation = CategoryResponse.class))),
            @ApiResponse(responseCode = "400", description = "Données de requête invalides"),
            @ApiResponse(responseCode = "404", description = "Catégorie non trouvée"),
            @ApiResponse(responseCode = "409", description = "Une catégorie avec ce nom existe déjà")
    })
    public ResponseEntity<CategoryResponse> update(
            @Parameter(description = "ID de la catégorie") @PathVariable Long id,
            @Valid @RequestBody CategoryRequest request) {
        CategoryResponse response = categoryService.update(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer une catégorie", description = "Supprime une catégorie par son ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Catégorie supprimée avec succès"),
            @ApiResponse(responseCode = "404", description = "Catégorie non trouvée")
    })
    public ResponseEntity<Void> delete(
            @Parameter(description = "ID de la catégorie") @PathVariable Long id) {
        categoryService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // Paginated endpoints

    @GetMapping("/paginated")
    @Operation(summary = "Lister les catégories avec pagination", description = "Récupère la liste paginée de toutes les catégories")
    @ApiResponse(responseCode = "200", description = "Liste paginée des catégories récupérée avec succès")
    public ResponseEntity<PageResponse<CategoryResponse>> getAllPaginated(
            @Parameter(description = "Numéro de page (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Taille de la page (1-50)") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Champ de tri") @RequestParam(defaultValue = "id") String sort,
            @Parameter(description = "Direction du tri (asc/desc)") @RequestParam(defaultValue = "asc") String direction) {
        Pageable pageable = createPageable(page, size, sort, direction);
        PageResponse<CategoryResponse> response = categoryService.getAllPaginated(pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/search/paginated")
    @Operation(summary = "Rechercher des catégories avec pagination", description = "Recherche des catégories par nom avec pagination")
    @ApiResponse(responseCode = "200", description = "Liste paginée des catégories correspondantes")
    public ResponseEntity<PageResponse<CategoryResponse>> searchByNamePaginated(
            @Parameter(description = "Terme de recherche") @RequestParam String name,
            @Parameter(description = "Numéro de page (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Taille de la page (1-50)") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Champ de tri") @RequestParam(defaultValue = "id") String sort,
            @Parameter(description = "Direction du tri (asc/desc)") @RequestParam(defaultValue = "asc") String direction) {
        Pageable pageable = createPageable(page, size, sort, direction);
        PageResponse<CategoryResponse> response = categoryService.searchByNamePaginated(name, pageable);
        return ResponseEntity.ok(response);
    }

    private Pageable createPageable(int page, int size, String sort, String direction) {
        int validPage = Math.max(0, page);
        int validSize = Math.min(Math.max(1, size), 50);
        Sort.Direction sortDirection = "desc".equalsIgnoreCase(direction) ? Sort.Direction.DESC : Sort.Direction.ASC;
        return PageRequest.of(validPage, validSize, Sort.by(sortDirection, sort));
    }
}
