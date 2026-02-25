package com.sencours.controller;

import com.sencours.service.FileStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
@Tag(name = "Files", description = "API d'upload et gestion de fichiers")
public class FileUploadController {

    private final FileStorageService fileStorageService;

    @PostMapping("/upload")
    @Operation(summary = "Uploader un fichier", description = "Upload un fichier (vidéo, PDF, image) et retourne l'URL")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Fichier uploadé avec succès"),
            @ApiResponse(responseCode = "400", description = "Fichier invalide ou type non reconnu"),
            @ApiResponse(responseCode = "401", description = "Authentification requise")
    })
    @PreAuthorize("hasAnyRole('INSTRUCTEUR', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Map<String, String>> uploadFile(
            @Parameter(description = "Le fichier à uploader") @RequestParam("file") MultipartFile file,
            @Parameter(description = "Type de fichier: video, pdf, image") @RequestParam("type") String type) {

        String fileUrl = fileStorageService.storeFile(file, type);

        Map<String, String> response = new HashMap<>();
        response.put("url", fileUrl);
        response.put("filename", file.getOriginalFilename());
        response.put("type", type);
        response.put("size", String.valueOf(file.getSize()));

        return ResponseEntity.ok(response);
    }

    @DeleteMapping
    @Operation(summary = "Supprimer un fichier", description = "Supprime un fichier par son URL")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Fichier supprimé avec succès"),
            @ApiResponse(responseCode = "401", description = "Authentification requise")
    })
    @PreAuthorize("hasAnyRole('INSTRUCTEUR', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Void> deleteFile(
            @Parameter(description = "URL du fichier à supprimer") @RequestParam("url") String fileUrl) {
        fileStorageService.deleteFile(fileUrl);
        return ResponseEntity.noContent().build();
    }
}
