package com.sencours.controller;

import com.sencours.dto.response.PageResponse;
import com.sencours.dto.response.UserResponse;
import com.sencours.enums.Role;
import com.sencours.service.AdminService;
import com.sencours.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Tag(name = "Admin", description = "API d'administration (ADMIN uniquement)")
public class AdminController {

    private final UserService userService;
    private final AdminService adminService;

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

    @PatchMapping("/users/{id}/toggle-status")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Suspendre/Réactiver un utilisateur", description = "Toggle le statut is_active. Admin : ETUDIANT et INSTRUCTEUR uniquement. SuperAdmin : tous sauf lui-même.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Statut de l'utilisateur modifié avec succès"),
            @ApiResponse(responseCode = "403", description = "Action non autorisée"),
            @ApiResponse(responseCode = "404", description = "Utilisateur non trouvé")
    })
    public ResponseEntity<UserResponse> toggleUserStatus(
            @Parameter(description = "ID de l'utilisateur") @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        UserResponse user = adminService.toggleUserStatus(id, userDetails.getUsername());
        return ResponseEntity.ok(user);
    }

    @DeleteMapping("/users/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Supprimer un utilisateur", description = "Supprime un utilisateur et toutes ses données associées. Réservé au SUPER_ADMIN.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Utilisateur supprimé avec succès"),
            @ApiResponse(responseCode = "403", description = "Seul le SuperAdmin peut supprimer"),
            @ApiResponse(responseCode = "404", description = "Utilisateur non trouvé")
    })
    public ResponseEntity<Void> deleteUser(
            @Parameter(description = "ID de l'utilisateur à supprimer") @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        adminService.deleteUser(id, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }

    private Pageable createPageable(int page, int size, String sort, String direction) {
        int validPage = Math.max(0, page);
        int validSize = Math.min(Math.max(1, size), 50);
        Sort.Direction sortDirection = "desc".equalsIgnoreCase(direction) ? Sort.Direction.DESC : Sort.Direction.ASC;
        return PageRequest.of(validPage, validSize, Sort.by(sortDirection, sort));
    }
}
