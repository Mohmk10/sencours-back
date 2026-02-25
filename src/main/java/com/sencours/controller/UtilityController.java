package com.sencours.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/utility")
public class UtilityController {

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /**
     * Génère un hash BCrypt pour un mot de passe donné.
     * Utile pour créer des utilisateurs directement en base de données (ex: SuperAdmin).
     *
     * Exemple d'utilisation :
     * POST /api/v1/utility/hash
     * Body: { "password": "MonMotDePasse123!" }
     *
     * Réponse: { "password": "MonMotDePasse123!", "hash": "$2a$10$..." }
     */
    @PostMapping("/hash")
    public ResponseEntity<Map<String, String>> generateHash(@RequestBody Map<String, String> request) {
        String password = request.get("password");

        if (password == null || password.isBlank()) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Le champ 'password' est requis");
            return ResponseEntity.badRequest().body(error);
        }

        String hash = passwordEncoder.encode(password);

        Map<String, String> response = new HashMap<>();
        response.put("password", password);
        response.put("hash", hash);
        response.put("usage", "INSERT INTO users (..., password, ...) VALUES (..., '" + hash + "', ...)");

        return ResponseEntity.ok(response);
    }

    /**
     * Vérifie si un mot de passe correspond à un hash BCrypt.
     * Utile pour le debugging.
     */
    @PostMapping("/verify")
    public ResponseEntity<Map<String, Object>> verifyHash(@RequestBody Map<String, String> request) {
        String password = request.get("password");
        String hash = request.get("hash");

        if (password == null || hash == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Les champs 'password' et 'hash' sont requis");
            return ResponseEntity.badRequest().body(error);
        }

        boolean matches = passwordEncoder.matches(password, hash);

        Map<String, Object> response = new HashMap<>();
        response.put("password", password);
        response.put("hash", hash);
        response.put("matches", matches);

        return ResponseEntity.ok(response);
    }
}
