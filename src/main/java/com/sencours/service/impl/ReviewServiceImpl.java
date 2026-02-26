package com.sencours.service.impl;

import com.sencours.dto.request.ReviewRequest;
import com.sencours.dto.response.ReviewResponse;
import com.sencours.entity.*;
import com.sencours.exception.BadRequestException;
import com.sencours.exception.ForbiddenException;
import com.sencours.exception.ResourceNotFoundException;
import com.sencours.repository.*;
import com.sencours.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final EnrollmentRepository enrollmentRepository;

    @Override
    @Transactional
    public ReviewResponse createOrUpdate(Long courseId, ReviewRequest request, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Cours non trouvé"));

        if (!enrollmentRepository.existsByUserIdAndCourseId(user.getId(), courseId)) {
            throw new BadRequestException("Vous devez être inscrit au cours pour laisser un avis");
        }

        if (course.getInstructor().getId().equals(user.getId())) {
            throw new BadRequestException("Vous ne pouvez pas noter votre propre cours");
        }

        Review review = reviewRepository.findByUserIdAndCourseId(user.getId(), courseId)
                .orElse(Review.builder()
                        .user(user)
                        .course(course)
                        .build());

        review.setRating(request.getRating());
        review.setComment(request.getComment());

        review = reviewRepository.save(review);

        return mapToResponse(review);
    }

    @Override
    public ReviewResponse getMyReview(Long courseId, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));

        return reviewRepository.findByUserIdAndCourseId(user.getId(), courseId)
                .map(this::mapToResponse)
                .orElse(null);
    }

    @Override
    public List<ReviewResponse> getCourseReviews(Long courseId) {
        return reviewRepository.findByCourseIdOrderByCreatedAtDesc(courseId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void delete(Long reviewId, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Avis non trouvé"));

        if (!review.getUser().getId().equals(user.getId())) {
            throw new ForbiddenException("Vous ne pouvez supprimer que vos propres avis");
        }

        reviewRepository.delete(review);
    }

    @Override
    public Double getAverageRating(Long courseId) {
        Double avg = reviewRepository.getAverageRatingByCourseId(courseId);
        return avg != null ? Math.round(avg * 10.0) / 10.0 : 0.0;
    }

    private ReviewResponse mapToResponse(Review review) {
        User user = review.getUser();
        String initials = (user.getFirstName().charAt(0) + "" + user.getLastName().charAt(0)).toUpperCase();

        return ReviewResponse.builder()
                .id(review.getId())
                .courseId(review.getCourse().getId())
                .userId(user.getId())
                .userName(user.getFirstName() + " " + user.getLastName())
                .userInitials(initials)
                .rating(review.getRating())
                .comment(review.getComment())
                .createdAt(review.getCreatedAt())
                .build();
    }
}
