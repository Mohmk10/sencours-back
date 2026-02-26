package com.sencours.service;

import com.sencours.dto.request.ReviewRequest;
import com.sencours.dto.response.ReviewResponse;

import java.util.List;

public interface ReviewService {

    ReviewResponse createOrUpdate(Long courseId, ReviewRequest request, String userEmail);

    ReviewResponse getMyReview(Long courseId, String userEmail);

    List<ReviewResponse> getCourseReviews(Long courseId);

    void delete(Long reviewId, String userEmail);

    Double getAverageRating(Long courseId);

    void deleteByAdmin(Long reviewId);
}
