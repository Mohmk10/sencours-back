package com.sencours.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class CertificateResponse {
    private Long id;
    private String certificateNumber;
    private Long courseId;
    private String courseTitle;
    private String courseThumbnail;
    private String instructorName;
    private Long userId;
    private String userName;
    private LocalDateTime issuedAt;
    private LocalDateTime completionDate;
}
