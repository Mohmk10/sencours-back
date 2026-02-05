package com.sencours.service.impl;

import com.sencours.dto.request.ReviewRequest;
import com.sencours.dto.request.ReviewUpdateRequest;
import com.sencours.dto.response.CourseRatingResponse;
import com.sencours.dto.response.ReviewResponse;
import com.sencours.entity.Course;
import com.sencours.entity.Review;
import com.sencours.entity.User;
import com.sencours.exception.*;
import com.sencours.mapper.ReviewMapper;
import com.sencours.repository.CourseRepository;
import com.sencours.repository.EnrollmentRepository;
import com.sencours.repository.ReviewRepository;
import com.sencours.repository.UserRepository;
import com.sencours.service.ReviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final ReviewMapper reviewMapper;

    @Override
    public ReviewResponse createReview(Long courseId, Long userId, ReviewRequest request) {
        log.info("Création d'un avis pour le cours {} par l'utilisateur {}", courseId, userId);

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Cours", "id", courseId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur", "id", userId));

        if (course.getInstructor().getId().equals(userId)) {
            throw new UnauthorizedReviewAccessException("Vous ne pouvez pas noter votre propre cours");
        }

        if (!enrollmentRepository.existsByStudentIdAndCourseId(userId, courseId)) {
            throw new NotEnrolledException(userId, courseId);
        }

        if (reviewRepository.existsByStudentIdAndCourseId(userId, courseId)) {
            throw new ReviewAlreadyExistsException(userId, courseId);
        }

        Review review = new Review();
        review.setStudent(user);
        review.setCourse(course);
        review.setRating(request.getRating());
        review.setComment(request.getComment());

        Review savedReview = reviewRepository.save(review);

        log.info("Avis créé avec succès. ID: {}, Rating: {}", savedReview.getId(), savedReview.getRating());
        return reviewMapper.toResponse(savedReview);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReviewResponse> getReviewsByCourse(Long courseId) {
        log.debug("Récupération des avis pour le cours {}", courseId);

        if (!courseRepository.existsById(courseId)) {
            throw new ResourceNotFoundException("Cours", "id", courseId);
        }

        return reviewRepository.findByCourseIdOrderByCreatedAtDesc(courseId).stream()
                .map(reviewMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ReviewResponse getReviewById(Long courseId, Long reviewId) {
        log.debug("Récupération de l'avis {} pour le cours {}", reviewId, courseId);

        if (!courseRepository.existsById(courseId)) {
            throw new ResourceNotFoundException("Cours", "id", courseId);
        }

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ReviewNotFoundException(reviewId));

        if (!review.getCourse().getId().equals(courseId)) {
            throw new ReviewNotFoundException(reviewId);
        }

        return reviewMapper.toResponse(review);
    }

    @Override
    public ReviewResponse updateReview(Long courseId, Long reviewId, Long userId, ReviewUpdateRequest request) {
        log.info("Mise à jour de l'avis {} par l'utilisateur {}", reviewId, userId);

        if (!courseRepository.existsById(courseId)) {
            throw new ResourceNotFoundException("Cours", "id", courseId);
        }

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ReviewNotFoundException(reviewId));

        if (!review.getCourse().getId().equals(courseId)) {
            throw new ReviewNotFoundException(reviewId);
        }

        if (!review.getStudent().getId().equals(userId)) {
            throw new UnauthorizedReviewAccessException();
        }

        review.setRating(request.getRating());
        review.setComment(request.getComment());

        Review updatedReview = reviewRepository.save(review);

        log.info("Avis mis à jour avec succès. ID: {}", updatedReview.getId());
        return reviewMapper.toResponse(updatedReview);
    }

    @Override
    public void deleteReview(Long courseId, Long reviewId, Long userId) {
        log.info("Suppression de l'avis {} par l'utilisateur {}", reviewId, userId);

        if (!courseRepository.existsById(courseId)) {
            throw new ResourceNotFoundException("Cours", "id", courseId);
        }

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ReviewNotFoundException(reviewId));

        if (!review.getCourse().getId().equals(courseId)) {
            throw new ReviewNotFoundException(reviewId);
        }

        if (!review.getStudent().getId().equals(userId)) {
            throw new UnauthorizedReviewAccessException();
        }

        reviewRepository.delete(review);

        log.info("Avis supprimé avec succès. ID: {}", reviewId);
    }

    @Override
    @Transactional(readOnly = true)
    public CourseRatingResponse getCourseRating(Long courseId) {
        log.debug("Calcul de la moyenne des avis pour le cours {}", courseId);

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Cours", "id", courseId));

        Double averageRating = reviewRepository.getAverageRatingByCourseId(courseId);
        Long totalReviews = reviewRepository.countReviewsByCourseId(courseId);

        Double roundedAverage = averageRating != null
                ? Math.round(averageRating * 10.0) / 10.0
                : null;

        return CourseRatingResponse.builder()
                .courseId(courseId)
                .courseTitle(course.getTitle())
                .averageRating(roundedAverage)
                .totalReviews(totalReviews)
                .build();
    }
}
