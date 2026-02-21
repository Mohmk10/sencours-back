package com.sencours.controller;

import com.sencours.dto.request.ApplicationReviewRequest;
import com.sencours.dto.request.InstructorApplicationCreateRequest;
import com.sencours.dto.response.InstructorApplicationResponse;
import com.sencours.entity.User;
import com.sencours.enums.ApplicationStatus;
import com.sencours.service.InstructorApplicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class InstructorApplicationController {

    private final InstructorApplicationService applicationService;

    // === ENDPOINTS POUR ÉTUDIANTS ===

    @PostMapping("/instructor-applications")
    public ResponseEntity<InstructorApplicationResponse> createApplication(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody InstructorApplicationCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(applicationService.createApplication(user.getId(), request));
    }

    @GetMapping("/instructor-applications/my-application")
    public ResponseEntity<InstructorApplicationResponse> getMyApplication(@AuthenticationPrincipal User user) {
        InstructorApplicationResponse application = applicationService.getMyApplication(user.getId());
        if (application == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(application);
    }

    @GetMapping("/instructor-applications/check")
    public ResponseEntity<Boolean> hasPendingApplication(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(applicationService.hasPendingApplication(user.getId()));
    }

    // === ENDPOINTS POUR ADMINS ===

    @GetMapping("/admin/instructor-applications")
    public ResponseEntity<Page<InstructorApplicationResponse>> getAllApplications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status) {

        Page<InstructorApplicationResponse> applications;
        if (status != null && !status.isEmpty()) {
            applications = applicationService.getApplicationsByStatus(
                    ApplicationStatus.valueOf(status.toUpperCase()),
                    PageRequest.of(page, size)
            );
        } else {
            applications = applicationService.getAllApplications(PageRequest.of(page, size));
        }
        return ResponseEntity.ok(applications);
    }

    @GetMapping("/admin/instructor-applications/{id}")
    public ResponseEntity<InstructorApplicationResponse> getApplicationById(@PathVariable Long id) {
        return ResponseEntity.ok(applicationService.getApplicationById(id));
    }

    @PutMapping("/admin/instructor-applications/{id}/review")
    public ResponseEntity<InstructorApplicationResponse> reviewApplication(
            @PathVariable Long id,
            @AuthenticationPrincipal User admin,
            @Valid @RequestBody ApplicationReviewRequest request) {
        return ResponseEntity.ok(applicationService.reviewApplication(id, admin.getId(), request));
    }

    @GetMapping("/admin/instructor-applications/pending-count")
    public ResponseEntity<Long> getPendingCount() {
        return ResponseEntity.ok(applicationService.getPendingCount());
    }
}
