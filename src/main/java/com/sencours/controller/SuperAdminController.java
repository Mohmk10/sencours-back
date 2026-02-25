package com.sencours.controller;

import com.sencours.dto.request.CreateAdminRequest;
import com.sencours.dto.response.UserResponse;
import com.sencours.entity.User;
import com.sencours.enums.Role;
import com.sencours.exception.BadRequestException;
import com.sencours.repository.UserRepository;
import com.sencours.service.SuperAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/super-admin")
@RequiredArgsConstructor
public class SuperAdminController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SuperAdminService superAdminService;

    @PostMapping("/admins")
    public ResponseEntity<UserResponse> createAdmin(@Valid @RequestBody CreateAdminRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Cet email est déjà utilisé");
        }

        User admin = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.ADMIN)
                .isActive(true)
                .build();

        admin = userRepository.save(admin);
        return ResponseEntity.status(HttpStatus.CREATED).body(mapToResponse(admin));
    }

    @GetMapping("/admins")
    public ResponseEntity<List<UserResponse>> getAllAdmins() {
        List<UserResponse> admins = userRepository.findByRole(Role.ADMIN)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(admins);
    }

    @DeleteMapping("/admins/{id}")
    public ResponseEntity<Void> deleteAdmin(@PathVariable Long id) {
        User admin = userRepository.findById(id)
                .orElseThrow(() -> new BadRequestException("Admin non trouvé"));

        if (admin.getRole() != Role.ADMIN) {
            throw new BadRequestException("Cet utilisateur n'est pas un admin");
        }

        userRepository.delete(admin);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/instructors")
    public ResponseEntity<UserResponse> createInstructor(@Valid @RequestBody CreateAdminRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Cet email est déjà utilisé");
        }

        User instructor = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.INSTRUCTEUR)
                .isActive(true)
                .build();

        instructor = userRepository.save(instructor);
        return ResponseEntity.status(HttpStatus.CREATED).body(mapToResponse(instructor));
    }

    @DeleteMapping("/reset-database")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Map<String, String>> resetDatabase(
            @RequestBody Map<String, String> confirmation,
            @AuthenticationPrincipal UserDetails userDetails) {

        String confirmText = confirmation.get("confirmation");
        if (!"RESET".equals(confirmText)) {
            throw new BadRequestException("Pour confirmer, envoyez {\"confirmation\": \"RESET\"}");
        }

        superAdminService.resetDatabase(userDetails.getUsername());

        Map<String, String> response = new HashMap<>();
        response.put("message", "Base de données réinitialisée avec succès. Seul le SuperAdmin a été conservé.");
        return ResponseEntity.ok(response);
    }

    private UserResponse mapToResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .role(user.getRole())
                .isActive(user.getIsActive())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
