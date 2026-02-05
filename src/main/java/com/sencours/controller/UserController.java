package com.sencours.controller;

import com.sencours.dto.request.PasswordChangeRequest;
import com.sencours.dto.request.UserRequest;
import com.sencours.dto.response.UserResponse;
import com.sencours.enums.Role;
import com.sencours.service.UserService;
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
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "API de gestion des utilisateurs")
public class UserController {

    private final UserService userService;

    @PostMapping
    @Operation(summary = "Créer un utilisateur", description = "Crée un nouvel utilisateur avec mot de passe hashé")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Utilisateur créé avec succès",
                    content = @Content(schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "400", description = "Données de requête invalides"),
            @ApiResponse(responseCode = "409", description = "Un utilisateur avec cet email existe déjà")
    })
    public ResponseEntity<UserResponse> create(
            @Valid @RequestBody UserRequest request) {
        UserResponse response = userService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "Lister les utilisateurs", description = "Récupère la liste de tous les utilisateurs")
    @ApiResponse(responseCode = "200", description = "Liste des utilisateurs récupérée avec succès")
    public ResponseEntity<List<UserResponse>> getAll() {
        List<UserResponse> users = userService.getAll();
        return ResponseEntity.ok(users);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Récupérer un utilisateur", description = "Récupère un utilisateur par son ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Utilisateur trouvé",
                    content = @Content(schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "404", description = "Utilisateur non trouvé")
    })
    public ResponseEntity<UserResponse> getById(
            @Parameter(description = "ID de l'utilisateur") @PathVariable Long id) {
        UserResponse response = userService.getById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/email/{email}")
    @Operation(summary = "Récupérer par email", description = "Récupère un utilisateur par son email")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Utilisateur trouvé"),
            @ApiResponse(responseCode = "404", description = "Utilisateur non trouvé")
    })
    public ResponseEntity<UserResponse> getByEmail(
            @Parameter(description = "Email de l'utilisateur") @PathVariable String email) {
        UserResponse response = userService.getByEmail(email);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/role/{role}")
    @Operation(summary = "Filtrer par rôle", description = "Récupère les utilisateurs par rôle")
    @ApiResponse(responseCode = "200", description = "Liste des utilisateurs filtrée par rôle")
    public ResponseEntity<List<UserResponse>> getByRole(
            @Parameter(description = "Rôle des utilisateurs") @PathVariable Role role) {
        List<UserResponse> users = userService.getByRole(role);
        return ResponseEntity.ok(users);
    }

    @GetMapping("/active")
    @Operation(summary = "Utilisateurs actifs", description = "Récupère les utilisateurs actifs")
    @ApiResponse(responseCode = "200", description = "Liste des utilisateurs actifs")
    public ResponseEntity<List<UserResponse>> getActiveUsers() {
        List<UserResponse> users = userService.getActiveUsers();
        return ResponseEntity.ok(users);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Modifier un utilisateur", description = "Met à jour un utilisateur (sans modifier le mot de passe)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Utilisateur mis à jour avec succès"),
            @ApiResponse(responseCode = "400", description = "Données de requête invalides"),
            @ApiResponse(responseCode = "404", description = "Utilisateur non trouvé"),
            @ApiResponse(responseCode = "409", description = "Un utilisateur avec cet email existe déjà")
    })
    public ResponseEntity<UserResponse> update(
            @Parameter(description = "ID de l'utilisateur") @PathVariable Long id,
            @Valid @RequestBody UserRequest request) {
        UserResponse response = userService.update(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer un utilisateur", description = "Supprime un utilisateur par son ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Utilisateur supprimé avec succès"),
            @ApiResponse(responseCode = "404", description = "Utilisateur non trouvé")
    })
    public ResponseEntity<Void> delete(
            @Parameter(description = "ID de l'utilisateur") @PathVariable Long id) {
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/password")
    @Operation(summary = "Changer le mot de passe", description = "Change le mot de passe d'un utilisateur")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Mot de passe changé avec succès"),
            @ApiResponse(responseCode = "400", description = "Mot de passe actuel incorrect ou mots de passe non correspondants"),
            @ApiResponse(responseCode = "404", description = "Utilisateur non trouvé")
    })
    public ResponseEntity<Void> changePassword(
            @Parameter(description = "ID de l'utilisateur") @PathVariable Long id,
            @Valid @RequestBody PasswordChangeRequest request) {
        userService.changePassword(id, request);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/activate")
    @Operation(summary = "Activer un utilisateur", description = "Active un compte utilisateur")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Utilisateur activé avec succès"),
            @ApiResponse(responseCode = "404", description = "Utilisateur non trouvé")
    })
    public ResponseEntity<Void> activateUser(
            @Parameter(description = "ID de l'utilisateur") @PathVariable Long id) {
        userService.activateUser(id);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/deactivate")
    @Operation(summary = "Désactiver un utilisateur", description = "Désactive un compte utilisateur")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Utilisateur désactivé avec succès"),
            @ApiResponse(responseCode = "404", description = "Utilisateur non trouvé")
    })
    public ResponseEntity<Void> deactivateUser(
            @Parameter(description = "ID de l'utilisateur") @PathVariable Long id) {
        userService.deactivateUser(id);
        return ResponseEntity.ok().build();
    }
}
