package com.sencours.service;

import com.sencours.dto.request.ApplicationReviewRequest;
import com.sencours.dto.request.InstructorApplicationCreateRequest;
import com.sencours.dto.response.InstructorApplicationResponse;
import com.sencours.enums.ApplicationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface InstructorApplicationService {

    InstructorApplicationResponse createApplication(Long userId, InstructorApplicationCreateRequest request);
    InstructorApplicationResponse getMyApplication(Long userId);
    boolean hasPendingApplication(Long userId);

    Page<InstructorApplicationResponse> getAllApplications(Pageable pageable);
    Page<InstructorApplicationResponse> getApplicationsByStatus(ApplicationStatus status, Pageable pageable);
    InstructorApplicationResponse getApplicationById(Long id);
    InstructorApplicationResponse reviewApplication(Long applicationId, Long adminId, ApplicationReviewRequest request);
    long getPendingCount();
}
