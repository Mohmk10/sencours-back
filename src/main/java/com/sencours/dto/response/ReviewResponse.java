package com.sencours.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewResponse {

    private Long id;
    private Long courseId;
    private Long userId;
    private String userName;
    private String userInitials;
    private Integer rating;
    private String comment;
    private LocalDateTime createdAt;
}
