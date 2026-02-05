package com.sencours.service;

import com.sencours.dto.request.EnrollmentRequest;
import com.sencours.dto.response.EnrollmentDetailResponse;
import com.sencours.dto.response.EnrollmentResponse;
import com.sencours.dto.response.ProgressSummaryResponse;

import java.util.List;

public interface EnrollmentService {

    EnrollmentResponse enroll(Long userId, EnrollmentRequest request);

    List<EnrollmentResponse> getMyEnrollments(Long userId);

    EnrollmentDetailResponse getEnrollmentDetail(Long enrollmentId);

    ProgressSummaryResponse calculateProgress(Long enrollmentId);

    List<EnrollmentResponse> getEnrollmentsByCourse(Long courseId);

    void unenroll(Long enrollmentId, Long userId);
}
