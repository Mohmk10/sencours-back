package com.sencours.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseRatingResponse {

    private Long courseId;
    private String courseTitle;
    private Double averageRating;
    private Long totalReviews;
}
