package com.sencours.service;

import com.sencours.dto.response.CertificateResponse;

import java.util.List;

public interface CertificateService {
    byte[] generateCertificatePdf(Long courseId, String userEmail);
    CertificateResponse getCertificate(Long courseId, String userEmail);
    List<CertificateResponse> getMyCertificates(String userEmail);
    CertificateResponse verifyCertificate(String certificateNumber);
}
