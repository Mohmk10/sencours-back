package com.sencours.controller;

import com.sencours.dto.response.PageResponse;
import com.sencours.dto.response.UserResponse;
import com.sencours.enums.Role;
import com.sencours.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Tag(name = "Admin", description = "API d'administration (ADMIN uniquement)")
public class AdminController {

    private final UserService userService;

    @GetMapping("/users")
    @Operation(summary = "Lister les utilisateurs avec pagination", description = "Récupère la liste paginée de tous les utilisateurs")
    @ApiResponse(responseCode = "200", description = "Liste paginée des utilisateurs récupérée avec succès")
    public ResponseEntity<PageResponse<UserResponse>> getAllUsersPaginated(
            @Parameter(description = "Numéro de page (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Taille de la page (1-50)") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Champ de tri") @RequestParam(defaultValue = "id") String sort,
            @Parameter(description = "Direction du tri (asc/desc)") @RequestParam(defaultValue = "asc") String direction) {
        Pageable pageable = createPageable(page, size, sort, direction);
        PageResponse<UserResponse> response = userService.getAllPaginated(pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/users/role/{role}")
    @Operation(summary = "Filtrer les utilisateurs par rôle avec pagination", description = "Récupère les utilisateurs par rôle avec pagination")
    @ApiResponse(responseCode = "200", description = "Liste paginée des utilisateurs filtrée par rôle")
    public ResponseEntity<PageResponse<UserResponse>> getUsersByRolePaginated(
            @Parameter(description = "Rôle des utilisateurs") @PathVariable Role role,
            @Parameter(description = "Numéro de page (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Taille de la page (1-50)") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Champ de tri") @RequestParam(defaultValue = "id") String sort,
            @Parameter(description = "Direction du tri (asc/desc)") @RequestParam(defaultValue = "asc") String direction) {
        Pageable pageable = createPageable(page, size, sort, direction);
        PageResponse<UserResponse> response = userService.getByRolePaginated(role, pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/users/search")
    @Operation(summary = "Rechercher des utilisateurs avec pagination", description = "Recherche des utilisateurs par nom, prénom ou email avec pagination")
    @ApiResponse(responseCode = "200", description = "Liste paginée des utilisateurs correspondants")
    public ResponseEntity<PageResponse<UserResponse>> searchUsersPaginated(
            @Parameter(description = "Terme de recherche") @RequestParam String search,
            @Parameter(description = "Numéro de page (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Taille de la page (1-50)") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Champ de tri") @RequestParam(defaultValue = "id") String sort,
            @Parameter(description = "Direction du tri (asc/desc)") @RequestParam(defaultValue = "asc") String direction) {
        Pageable pageable = createPageable(page, size, sort, direction);
        PageResponse<UserResponse> response = userService.searchUsersPaginated(search, pageable);
        return ResponseEntity.ok(response);
    }

    private Pageable createPageable(int page, int size, String sort, String direction) {
        int validPage = Math.max(0, page);
        int validSize = Math.min(Math.max(1, size), 50);
        Sort.Direction sortDirection = "desc".equalsIgnoreCase(direction) ? Sort.Direction.DESC : Sort.Direction.ASC;
        return PageRequest.of(validPage, validSize, Sort.by(sortDirection, sort));
    }
}
