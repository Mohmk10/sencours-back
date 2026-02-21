package com.sencours.dto.response;

import com.sencours.enums.ApplicationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InstructorApplicationResponse {
    private Long id;
    private Long userId;
    private String userFullName;
    private String userEmail;
    private String motivation;
    private String expertise;
    private String linkedinUrl;
    private String portfolioUrl;
    private ApplicationStatus status;
    private String adminComment;
    private String reviewedByName;
    private LocalDateTime reviewedAt;
    private LocalDateTime createdAt;
}
