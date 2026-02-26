package com.sencours.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnrollmentResponse {

    private Long id;
    private Long courseId;
    private String courseTitle;
    private String courseThumbnail;
    private Long userId;
    private String userName;
    private LocalDateTime enrolledAt;
    private LocalDateTime completedAt;
    private Integer progressPercentage;
    private String paymentReference;
    private String paymentMethod;
    private BigDecimal amountPaid;
    private Integer totalLessons;
    private Integer completedLessons;
}
