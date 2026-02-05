package com.sencours.service;

import com.sencours.dto.request.ReviewRequest;
import com.sencours.dto.request.ReviewUpdateRequest;
import com.sencours.dto.response.CourseRatingResponse;
import com.sencours.dto.response.ReviewResponse;

import java.util.List;

public interface ReviewService {

    ReviewResponse createReview(Long courseId, Long userId, ReviewRequest request);

    List<ReviewResponse> getReviewsByCourse(Long courseId);

    ReviewResponse getReviewById(Long courseId, Long reviewId);

    ReviewResponse updateReview(Long courseId, Long reviewId, Long userId, ReviewUpdateRequest request);

    void deleteReview(Long courseId, Long reviewId, Long userId);

    CourseRatingResponse getCourseRating(Long courseId);
}
