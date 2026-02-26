package com.sencours.controller;

import com.sencours.dto.request.ReviewRequest;
import com.sencours.dto.response.ReviewResponse;
import com.sencours.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping("/courses/{courseId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ReviewResponse> createOrUpdate(
            @PathVariable Long courseId,
            @Valid @RequestBody ReviewRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        ReviewResponse response = reviewService.createOrUpdate(courseId, request, userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/courses/{courseId}/my-review")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ReviewResponse> getMyReview(
            @PathVariable Long courseId,
            @AuthenticationPrincipal UserDetails userDetails) {
        ReviewResponse review = reviewService.getMyReview(courseId, userDetails.getUsername());
        return ResponseEntity.ok(review);
    }

    @GetMapping("/courses/{courseId}")
    public ResponseEntity<List<ReviewResponse>> getCourseReviews(@PathVariable Long courseId) {
        List<ReviewResponse> reviews = reviewService.getCourseReviews(courseId);
        return ResponseEntity.ok(reviews);
    }

    @GetMapping("/courses/{courseId}/average")
    public ResponseEntity<Map<String, Object>> getAverageRating(@PathVariable Long courseId) {
        Double average = reviewService.getAverageRating(courseId);
        return ResponseEntity.ok(Map.of("averageRating", average));
    }

    @DeleteMapping("/{reviewId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> delete(
            @PathVariable Long reviewId,
            @AuthenticationPrincipal UserDetails userDetails) {
        reviewService.delete(reviewId, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }
}
