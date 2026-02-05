package com.sencours.mapper;

import com.sencours.dto.response.EnrollmentDetailResponse;
import com.sencours.dto.response.EnrollmentResponse;
import com.sencours.dto.response.ProgressResponse;
import com.sencours.entity.Enrollment;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class EnrollmentMapper {

    private final ProgressMapper progressMapper;

    public EnrollmentResponse toResponse(Enrollment enrollment, Double progressPercentage) {
        return EnrollmentResponse.builder()
                .id(enrollment.getId())
                .userId(enrollment.getStudent().getId())
                .userFirstName(enrollment.getStudent().getFirstName())
                .userLastName(enrollment.getStudent().getLastName())
                .courseId(enrollment.getCourse().getId())
                .courseTitle(enrollment.getCourse().getTitle())
                .enrolledAt(enrollment.getEnrolledAt())
                .completedAt(enrollment.getCompletedAt())
                .progressPercentage(progressPercentage)
                .build();
    }

    public EnrollmentDetailResponse toDetailResponse(Enrollment enrollment, Double progressPercentage, List<ProgressResponse> progresses) {
        return EnrollmentDetailResponse.builder()
                .id(enrollment.getId())
                .userId(enrollment.getStudent().getId())
                .userFirstName(enrollment.getStudent().getFirstName())
                .userLastName(enrollment.getStudent().getLastName())
                .courseId(enrollment.getCourse().getId())
                .courseTitle(enrollment.getCourse().getTitle())
                .enrolledAt(enrollment.getEnrolledAt())
                .completedAt(enrollment.getCompletedAt())
                .progressPercentage(progressPercentage)
                .progresses(progresses)
                .build();
    }
}
