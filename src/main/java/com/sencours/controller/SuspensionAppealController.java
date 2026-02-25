package com.sencours.controller;

import com.sencours.dto.SuspensionAppealRequest;
import com.sencours.dto.SuspensionAppealResponse;
import com.sencours.dto.SuspensionAppealReviewRequest;
import com.sencours.service.SuspensionAppealService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/appeals")
@RequiredArgsConstructor
public class SuspensionAppealController {

    private final SuspensionAppealService appealService;

    @PostMapping
    public ResponseEntity<SuspensionAppealResponse> submitAppeal(
            @RequestBody SuspensionAppealRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        SuspensionAppealResponse response = appealService.submitAppeal(request, userDetails.getUsername());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/my")
    public ResponseEntity<List<SuspensionAppealResponse>> getMyAppeals(
            @AuthenticationPrincipal UserDetails userDetails) {
        List<SuspensionAppealResponse> appeals = appealService.getUserAppeals(userDetails.getUsername());
        return ResponseEntity.ok(appeals);
    }

    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<List<SuspensionAppealResponse>> getPendingAppeals() {
        List<SuspensionAppealResponse> appeals = appealService.getPendingAppeals();
        return ResponseEntity.ok(appeals);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<SuspensionAppealResponse> reviewAppeal(
            @PathVariable Long id,
            @RequestBody SuspensionAppealReviewRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        SuspensionAppealResponse response = appealService.reviewAppeal(id, request, userDetails.getUsername());
        return ResponseEntity.ok(response);
    }
}
