package com.sencours.mapper;

import com.sencours.dto.response.ReviewResponse;
import com.sencours.entity.Review;
import org.springframework.stereotype.Component;

@Component
public class ReviewMapper {

    public ReviewResponse toResponse(Review review) {
        String userFullName = review.getStudent().getFirstName() + " " + review.getStudent().getLastName();

        return ReviewResponse.builder()
                .id(review.getId())
                .userId(review.getStudent().getId())
                .userFullName(userFullName)
                .courseId(review.getCourse().getId())
                .courseTitle(review.getCourse().getTitle())
                .rating(review.getRating())
                .comment(review.getComment())
                .createdAt(review.getCreatedAt())
                .updatedAt(review.getUpdatedAt())
                .build();
    }
}
