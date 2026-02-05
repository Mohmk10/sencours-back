package com.sencours.service;

import com.sencours.dto.request.ReviewRequest;
import com.sencours.dto.request.ReviewUpdateRequest;
import com.sencours.dto.response.CourseRatingResponse;
import com.sencours.dto.response.ReviewResponse;
import com.sencours.entity.Course;
import com.sencours.entity.Review;
import com.sencours.entity.User;
import com.sencours.enums.Role;
import com.sencours.enums.Status;
import com.sencours.exception.*;
import com.sencours.mapper.ReviewMapper;
import com.sencours.repository.CourseRepository;
import com.sencours.repository.EnrollmentRepository;
import com.sencours.repository.ReviewRepository;
import com.sencours.repository.UserRepository;
import com.sencours.service.impl.ReviewServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private ReviewMapper reviewMapper;

    @InjectMocks
    private ReviewServiceImpl reviewService;

    private User instructor;
    private User student;
    private Course course;
    private Review review;
    private ReviewRequest reviewRequest;
    private ReviewResponse reviewResponse;

    @BeforeEach
    void setUp() {
        instructor = new User();
        instructor.setId(1L);
        instructor.setFirstName("Prof");
        instructor.setLastName("Diop");
        instructor.setRole(Role.INSTRUCTEUR);

        student = new User();
        student.setId(2L);
        student.setFirstName("Mamadou");
        student.setLastName("Diallo");
        student.setRole(Role.ETUDIANT);

        course = new Course();
        course.setId(1L);
        course.setTitle("Java pour débutants");
        course.setPrice(new BigDecimal("25000"));
        course.setStatus(Status.PUBLISHED);
        course.setInstructor(instructor);

        review = new Review();
        review.setId(1L);
        review.setStudent(student);
        review.setCourse(course);
        review.setRating(5);
        review.setComment("Excellent cours!");
        review.setCreatedAt(LocalDateTime.now());
        review.setUpdatedAt(LocalDateTime.now());

        reviewRequest = ReviewRequest.builder()
                .rating(5)
                .comment("Excellent cours!")
                .build();

        reviewResponse = ReviewResponse.builder()
                .id(1L)
                .userId(2L)
                .userFullName("Mamadou Diallo")
                .courseId(1L)
                .courseTitle("Java pour débutants")
                .rating(5)
                .comment("Excellent cours!")
                .build();
    }

    @Nested
    @DisplayName("Tests pour createReview()")
    class CreateReviewTests {

        @Test
        @DisplayName("Devrait créer un avis avec succès")
        void shouldCreateReviewSuccessfully() {
            when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
            when(userRepository.findById(2L)).thenReturn(Optional.of(student));
            when(enrollmentRepository.existsByStudentIdAndCourseId(2L, 1L)).thenReturn(true);
            when(reviewRepository.existsByStudentIdAndCourseId(2L, 1L)).thenReturn(false);
            when(reviewRepository.save(any(Review.class))).thenReturn(review);
            when(reviewMapper.toResponse(review)).thenReturn(reviewResponse);

            ReviewResponse result = reviewService.createReview(1L, 2L, reviewRequest);

            assertThat(result).isNotNull();
            assertThat(result.getRating()).isEqualTo(5);
            verify(reviewRepository).save(any(Review.class));
        }

        @Test
        @DisplayName("Devrait lever exception si cours non trouvé")
        void shouldThrowExceptionWhenCourseNotFound() {
            when(courseRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> reviewService.createReview(999L, 2L, reviewRequest))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Cours");
        }

        @Test
        @DisplayName("Devrait lever exception si utilisateur non trouvé")
        void shouldThrowExceptionWhenUserNotFound() {
            when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> reviewService.createReview(1L, 999L, reviewRequest))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Utilisateur");
        }

        @Test
        @DisplayName("Devrait lever exception si instructeur note son propre cours")
        void shouldThrowExceptionWhenInstructorReviewsOwnCourse() {
            when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
            when(userRepository.findById(1L)).thenReturn(Optional.of(instructor));

            assertThatThrownBy(() -> reviewService.createReview(1L, 1L, reviewRequest))
                    .isInstanceOf(UnauthorizedReviewAccessException.class)
                    .hasMessageContaining("propre cours");
        }

        @Test
        @DisplayName("Devrait lever exception si utilisateur non inscrit")
        void shouldThrowExceptionWhenUserNotEnrolled() {
            when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
            when(userRepository.findById(2L)).thenReturn(Optional.of(student));
            when(enrollmentRepository.existsByStudentIdAndCourseId(2L, 1L)).thenReturn(false);

            assertThatThrownBy(() -> reviewService.createReview(1L, 2L, reviewRequest))
                    .isInstanceOf(NotEnrolledException.class);
        }

        @Test
        @DisplayName("Devrait lever exception si avis existe déjà")
        void shouldThrowExceptionWhenReviewAlreadyExists() {
            when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
            when(userRepository.findById(2L)).thenReturn(Optional.of(student));
            when(enrollmentRepository.existsByStudentIdAndCourseId(2L, 1L)).thenReturn(true);
            when(reviewRepository.existsByStudentIdAndCourseId(2L, 1L)).thenReturn(true);

            assertThatThrownBy(() -> reviewService.createReview(1L, 2L, reviewRequest))
                    .isInstanceOf(ReviewAlreadyExistsException.class)
                    .hasMessageContaining("déjà noté");
        }
    }

    @Nested
    @DisplayName("Tests pour getReviewsByCourse()")
    class GetReviewsByCourseTests {

        @Test
        @DisplayName("Devrait retourner la liste des avis")
        void shouldReturnReviewsList() {
            when(courseRepository.existsById(1L)).thenReturn(true);
            when(reviewRepository.findByCourseIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(review));
            when(reviewMapper.toResponse(review)).thenReturn(reviewResponse);

            List<ReviewResponse> result = reviewService.getReviewsByCourse(1L);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getRating()).isEqualTo(5);
        }

        @Test
        @DisplayName("Devrait lever exception si cours non trouvé")
        void shouldThrowExceptionWhenCourseNotFound() {
            when(courseRepository.existsById(999L)).thenReturn(false);

            assertThatThrownBy(() -> reviewService.getReviewsByCourse(999L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Tests pour updateReview()")
    class UpdateReviewTests {

        @Test
        @DisplayName("Devrait mettre à jour un avis")
        void shouldUpdateReview() {
            ReviewUpdateRequest updateRequest = ReviewUpdateRequest.builder()
                    .rating(4)
                    .comment("Bon cours")
                    .build();

            when(courseRepository.existsById(1L)).thenReturn(true);
            when(reviewRepository.findById(1L)).thenReturn(Optional.of(review));
            when(reviewRepository.save(any(Review.class))).thenReturn(review);
            when(reviewMapper.toResponse(review)).thenReturn(reviewResponse);

            ReviewResponse result = reviewService.updateReview(1L, 1L, 2L, updateRequest);

            assertThat(result).isNotNull();
            verify(reviewRepository).save(any(Review.class));
        }

        @Test
        @DisplayName("Devrait lever exception si pas l'auteur")
        void shouldThrowExceptionWhenNotAuthor() {
            ReviewUpdateRequest updateRequest = ReviewUpdateRequest.builder()
                    .rating(4)
                    .build();

            when(courseRepository.existsById(1L)).thenReturn(true);
            when(reviewRepository.findById(1L)).thenReturn(Optional.of(review));

            assertThatThrownBy(() -> reviewService.updateReview(1L, 1L, 999L, updateRequest))
                    .isInstanceOf(UnauthorizedReviewAccessException.class);
        }

        @Test
        @DisplayName("Devrait lever exception si avis non trouvé")
        void shouldThrowExceptionWhenReviewNotFound() {
            ReviewUpdateRequest updateRequest = ReviewUpdateRequest.builder()
                    .rating(4)
                    .build();

            when(courseRepository.existsById(1L)).thenReturn(true);
            when(reviewRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> reviewService.updateReview(1L, 999L, 2L, updateRequest))
                    .isInstanceOf(ReviewNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Tests pour deleteReview()")
    class DeleteReviewTests {

        @Test
        @DisplayName("Devrait supprimer un avis")
        void shouldDeleteReview() {
            when(courseRepository.existsById(1L)).thenReturn(true);
            when(reviewRepository.findById(1L)).thenReturn(Optional.of(review));

            reviewService.deleteReview(1L, 1L, 2L);

            verify(reviewRepository).delete(review);
        }

        @Test
        @DisplayName("Devrait lever exception si pas l'auteur")
        void shouldThrowExceptionWhenNotAuthor() {
            when(courseRepository.existsById(1L)).thenReturn(true);
            when(reviewRepository.findById(1L)).thenReturn(Optional.of(review));

            assertThatThrownBy(() -> reviewService.deleteReview(1L, 1L, 999L))
                    .isInstanceOf(UnauthorizedReviewAccessException.class);
        }
    }

    @Nested
    @DisplayName("Tests pour getCourseRating()")
    class GetCourseRatingTests {

        @Test
        @DisplayName("Devrait calculer la moyenne correctement")
        void shouldCalculateAverageCorrectly() {
            when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
            when(reviewRepository.getAverageRatingByCourseId(1L)).thenReturn(4.333);
            when(reviewRepository.countReviewsByCourseId(1L)).thenReturn(3L);

            CourseRatingResponse result = reviewService.getCourseRating(1L);

            assertThat(result.getCourseId()).isEqualTo(1L);
            assertThat(result.getAverageRating()).isEqualTo(4.3);
            assertThat(result.getTotalReviews()).isEqualTo(3L);
        }

        @Test
        @DisplayName("Devrait retourner null si aucun avis")
        void shouldReturnNullWhenNoReviews() {
            when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
            when(reviewRepository.getAverageRatingByCourseId(1L)).thenReturn(null);
            when(reviewRepository.countReviewsByCourseId(1L)).thenReturn(0L);

            CourseRatingResponse result = reviewService.getCourseRating(1L);

            assertThat(result.getAverageRating()).isNull();
            assertThat(result.getTotalReviews()).isEqualTo(0L);
        }

        @Test
        @DisplayName("Devrait lever exception si cours non trouvé")
        void shouldThrowExceptionWhenCourseNotFound() {
            when(courseRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> reviewService.getCourseRating(999L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}
