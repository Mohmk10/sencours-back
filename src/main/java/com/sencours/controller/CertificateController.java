package com.sencours.controller;

import com.sencours.dto.response.CertificateResponse;
import com.sencours.service.CertificateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/certificates")
@RequiredArgsConstructor
public class CertificateController {

    private final CertificateService certificateService;

    @GetMapping("/courses/{courseId}/download")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<byte[]> downloadCertificate(
            @PathVariable Long courseId,
            @AuthenticationPrincipal UserDetails userDetails) {

        byte[] pdfBytes = certificateService.generateCertificatePdf(courseId, userDetails.getUsername());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "certificat-sencours-" + courseId + ".pdf");
        headers.setContentLength(pdfBytes.length);

        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }

    @GetMapping("/courses/{courseId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CertificateResponse> getCertificate(
            @PathVariable Long courseId,
            @AuthenticationPrincipal UserDetails userDetails) {

        CertificateResponse certificate = certificateService.getCertificate(courseId, userDetails.getUsername());
        return ResponseEntity.ok(certificate);
    }

    @GetMapping("/my-certificates")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<CertificateResponse>> getMyCertificates(
            @AuthenticationPrincipal UserDetails userDetails) {

        List<CertificateResponse> certificates = certificateService.getMyCertificates(userDetails.getUsername());
        return ResponseEntity.ok(certificates);
    }

    @GetMapping("/verify/{certificateNumber}")
    public ResponseEntity<CertificateResponse> verifyCertificate(
            @PathVariable String certificateNumber) {

        CertificateResponse certificate = certificateService.verifyCertificate(certificateNumber);
        return ResponseEntity.ok(certificate);
    }
}
