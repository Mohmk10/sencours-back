package com.sencours.controller;

import com.sencours.dto.request.EnrollmentRequest;
import com.sencours.dto.response.EnrollmentResponse;
import com.sencours.dto.response.PaymentResponse;
import com.sencours.service.EnrollmentService;
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
@RequestMapping("/api/v1/enrollments")
@RequiredArgsConstructor
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    @PostMapping("/courses/{courseId}/pay")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PaymentResponse> initiatePayment(
            @PathVariable Long courseId,
            @RequestBody EnrollmentRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        PaymentResponse response = enrollmentService.initiatePayment(courseId, request, userDetails.getUsername());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/courses/{courseId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<EnrollmentResponse> completeEnrollment(
            @PathVariable Long courseId,
            @RequestBody Map<String, String> request,
            @AuthenticationPrincipal UserDetails userDetails) {
        String paymentReference = request.get("paymentReference");
        EnrollmentResponse response = enrollmentService.completeEnrollment(courseId, paymentReference, userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/courses/{courseId}/free")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<EnrollmentResponse> enrollFree(
            @PathVariable Long courseId,
            @AuthenticationPrincipal UserDetails userDetails) {
        EnrollmentResponse response = enrollmentService.enrollFree(courseId, userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/courses/{courseId}/check")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Boolean>> checkEnrollment(
            @PathVariable Long courseId,
            @AuthenticationPrincipal UserDetails userDetails) {
        boolean enrolled = enrollmentService.isEnrolled(courseId, userDetails.getUsername());
        return ResponseEntity.ok(Map.of("enrolled", enrolled));
    }

    @GetMapping("/my-enrollments")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<EnrollmentResponse>> getMyEnrollments(
            @AuthenticationPrincipal UserDetails userDetails) {
        List<EnrollmentResponse> enrollments = enrollmentService.getMyEnrollments(userDetails.getUsername());
        return ResponseEntity.ok(enrollments);
    }

    @GetMapping("/courses/{courseId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<EnrollmentResponse> getEnrollment(
            @PathVariable Long courseId,
            @AuthenticationPrincipal UserDetails userDetails) {
        EnrollmentResponse enrollment = enrollmentService.getEnrollment(courseId, userDetails.getUsername());
        return ResponseEntity.ok(enrollment);
    }
}
