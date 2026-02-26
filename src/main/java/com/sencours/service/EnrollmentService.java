package com.sencours.service;

import com.sencours.dto.request.EnrollmentRequest;
import com.sencours.dto.response.EnrollmentResponse;
import com.sencours.dto.response.PaymentResponse;

import java.util.List;

public interface EnrollmentService {

    PaymentResponse initiatePayment(Long courseId, EnrollmentRequest request, String userEmail);

    EnrollmentResponse completeEnrollment(Long courseId, String paymentReference, String userEmail);

    EnrollmentResponse enrollFree(Long courseId, String userEmail);

    boolean isEnrolled(Long courseId, String userEmail);

    List<EnrollmentResponse> getMyEnrollments(String userEmail);

    EnrollmentResponse getEnrollment(Long courseId, String userEmail);

    void updateProgress(Long courseId, String userEmail);
}
