package com.sencours.controller;

import com.sencours.dto.request.ProgressRequest;
import com.sencours.dto.response.ProgressResponse;
import com.sencours.service.ProgressService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/progress")
@RequiredArgsConstructor
public class ProgressController {

    private final ProgressService progressService;

    @PutMapping("/lessons/{lessonId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProgressResponse> updateProgress(
            @PathVariable Long lessonId,
            @RequestBody ProgressRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        ProgressResponse response = progressService.updateProgress(lessonId, request, userDetails.getUsername());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/lessons/{lessonId}/complete")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> markAsCompleted(
            @PathVariable Long lessonId,
            @AuthenticationPrincipal UserDetails userDetails) {
        progressService.markAsCompleted(lessonId, userDetails.getUsername());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/lessons/{lessonId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProgressResponse> getProgress(
            @PathVariable Long lessonId,
            @AuthenticationPrincipal UserDetails userDetails) {
        ProgressResponse response = progressService.getProgress(lessonId, userDetails.getUsername());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/courses/{courseId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ProgressResponse>> getCourseProgress(
            @PathVariable Long courseId,
            @AuthenticationPrincipal UserDetails userDetails) {
        List<ProgressResponse> progress = progressService.getCourseProgress(courseId, userDetails.getUsername());
        return ResponseEntity.ok(progress);
    }
}
