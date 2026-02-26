package com.sencours.service;

import com.sencours.dto.request.ReviewRequest;
import com.sencours.dto.response.ReviewResponse;
import com.sencours.entity.Course;
import com.sencours.entity.Review;
import com.sencours.entity.User;
import com.sencours.enums.Role;
import com.sencours.enums.Status;
import com.sencours.exception.BadRequestException;
import com.sencours.exception.ForbiddenException;
import com.sencours.exception.ResourceNotFoundException;
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

    @InjectMocks
    private ReviewServiceImpl reviewService;

    private User instructor;
    private User student;
    private Course course;
    private Review review;
    private ReviewRequest reviewRequest;

    @BeforeEach
    void setUp() {
        instructor = User.builder()
                .id(1L).firstName("Prof").lastName("Diop")
                .email("prof@sencours.sn").role(Role.INSTRUCTEUR).build();

        student = User.builder()
                .id(2L).firstName("Mamadou").lastName("Diallo")
                .email("mamadou@sencours.sn").role(Role.ETUDIANT).build();

        course = new Course();
        course.setId(1L);
        course.setTitle("Java pour débutants");
        course.setPrice(new BigDecimal("25000"));
        course.setStatus(Status.PUBLISHED);
        course.setInstructor(instructor);

        review = Review.builder()
                .id(1L)
                .user(student)
                .course(course)
                .rating(5)
                .comment("Excellent cours!")
                .createdAt(LocalDateTime.now())
                .build();

        reviewRequest = ReviewRequest.builder()
                .rating(5)
                .comment("Excellent cours!")
                .build();
    }

    @Nested
    @DisplayName("Tests pour createOrUpdate()")
    class CreateOrUpdateTests {

        @Test
        @DisplayName("Devrait créer un avis avec succès")
        void shouldCreateReviewSuccessfully() {
            when(userRepository.findByEmail("mamadou@sencours.sn")).thenReturn(Optional.of(student));
            when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
            when(enrollmentRepository.existsByUserIdAndCourseId(2L, 1L)).thenReturn(true);
            when(reviewRepository.findByUserIdAndCourseId(2L, 1L)).thenReturn(Optional.empty());
            when(reviewRepository.save(any(Review.class))).thenReturn(review);

            ReviewResponse result = reviewService.createOrUpdate(1L, reviewRequest, "mamadou@sencours.sn");

            assertThat(result).isNotNull();
            assertThat(result.getRating()).isEqualTo(5);
            assertThat(result.getUserName()).isEqualTo("Mamadou Diallo");
            assertThat(result.getUserInitials()).isEqualTo("MD");
            verify(reviewRepository).save(any(Review.class));
        }

        @Test
        @DisplayName("Devrait mettre à jour un avis existant")
        void shouldUpdateExistingReview() {
            when(userRepository.findByEmail("mamadou@sencours.sn")).thenReturn(Optional.of(student));
            when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
            when(enrollmentRepository.existsByUserIdAndCourseId(2L, 1L)).thenReturn(true);
            when(reviewRepository.findByUserIdAndCourseId(2L, 1L)).thenReturn(Optional.of(review));
            when(reviewRepository.save(any(Review.class))).thenReturn(review);

            ReviewRequest updateRequest = ReviewRequest.builder().rating(4).comment("Bon cours").build();
            ReviewResponse result = reviewService.createOrUpdate(1L, updateRequest, "mamadou@sencours.sn");

            assertThat(result).isNotNull();
            verify(reviewRepository).save(any(Review.class));
        }

        @Test
        @DisplayName("Devrait lever exception si cours non trouvé")
        void shouldThrowExceptionWhenCourseNotFound() {
            when(userRepository.findByEmail("mamadou@sencours.sn")).thenReturn(Optional.of(student));
            when(courseRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> reviewService.createOrUpdate(999L, reviewRequest, "mamadou@sencours.sn"))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Cours");
        }

        @Test
        @DisplayName("Devrait lever exception si utilisateur non trouvé")
        void shouldThrowExceptionWhenUserNotFound() {
            when(userRepository.findByEmail("unknown@sencours.sn")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> reviewService.createOrUpdate(1L, reviewRequest, "unknown@sencours.sn"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("Devrait lever exception si non inscrit")
        void shouldThrowExceptionWhenNotEnrolled() {
            when(userRepository.findByEmail("mamadou@sencours.sn")).thenReturn(Optional.of(student));
            when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
            when(enrollmentRepository.existsByUserIdAndCourseId(2L, 1L)).thenReturn(false);

            assertThatThrownBy(() -> reviewService.createOrUpdate(1L, reviewRequest, "mamadou@sencours.sn"))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("inscrit");
        }

        @Test
        @DisplayName("Devrait lever exception si instructeur note son propre cours")
        void shouldThrowExceptionWhenInstructorReviewsOwnCourse() {
            when(userRepository.findByEmail("prof@sencours.sn")).thenReturn(Optional.of(instructor));
            when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
            when(enrollmentRepository.existsByUserIdAndCourseId(1L, 1L)).thenReturn(true);

            assertThatThrownBy(() -> reviewService.createOrUpdate(1L, reviewRequest, "prof@sencours.sn"))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("propre cours");
        }
    }

    @Nested
    @DisplayName("Tests pour getMyReview()")
    class GetMyReviewTests {

        @Test
        @DisplayName("Devrait retourner l'avis de l'utilisateur")
        void shouldReturnUserReview() {
            when(userRepository.findByEmail("mamadou@sencours.sn")).thenReturn(Optional.of(student));
            when(reviewRepository.findByUserIdAndCourseId(2L, 1L)).thenReturn(Optional.of(review));

            ReviewResponse result = reviewService.getMyReview(1L, "mamadou@sencours.sn");

            assertThat(result).isNotNull();
            assertThat(result.getRating()).isEqualTo(5);
        }

        @Test
        @DisplayName("Devrait retourner null si aucun avis")
        void shouldReturnNullWhenNoReview() {
            when(userRepository.findByEmail("mamadou@sencours.sn")).thenReturn(Optional.of(student));
            when(reviewRepository.findByUserIdAndCourseId(2L, 1L)).thenReturn(Optional.empty());

            ReviewResponse result = reviewService.getMyReview(1L, "mamadou@sencours.sn");

            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("Tests pour getCourseReviews()")
    class GetCourseReviewsTests {

        @Test
        @DisplayName("Devrait retourner la liste des avis")
        void shouldReturnReviewsList() {
            when(reviewRepository.findByCourseIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(review));

            List<ReviewResponse> result = reviewService.getCourseReviews(1L);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getRating()).isEqualTo(5);
        }

        @Test
        @DisplayName("Devrait retourner liste vide si aucun avis")
        void shouldReturnEmptyListWhenNoReviews() {
            when(reviewRepository.findByCourseIdOrderByCreatedAtDesc(1L)).thenReturn(List.of());

            List<ReviewResponse> result = reviewService.getCourseReviews(1L);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("Tests pour delete()")
    class DeleteTests {

        @Test
        @DisplayName("Devrait supprimer un avis")
        void shouldDeleteReview() {
            when(userRepository.findByEmail("mamadou@sencours.sn")).thenReturn(Optional.of(student));
            when(reviewRepository.findById(1L)).thenReturn(Optional.of(review));

            reviewService.delete(1L, "mamadou@sencours.sn");

            verify(reviewRepository).delete(review);
        }

        @Test
        @DisplayName("Devrait lever exception si pas l'auteur")
        void shouldThrowExceptionWhenNotAuthor() {
            User otherStudent = User.builder()
                    .id(3L).firstName("Ousmane").lastName("Sow")
                    .email("ousmane@sencours.sn").role(Role.ETUDIANT).build();

            when(userRepository.findByEmail("ousmane@sencours.sn")).thenReturn(Optional.of(otherStudent));
            when(reviewRepository.findById(1L)).thenReturn(Optional.of(review));

            assertThatThrownBy(() -> reviewService.delete(1L, "ousmane@sencours.sn"))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessageContaining("propres avis");
        }

        @Test
        @DisplayName("Devrait lever exception si avis non trouvé")
        void shouldThrowExceptionWhenReviewNotFound() {
            when(userRepository.findByEmail("mamadou@sencours.sn")).thenReturn(Optional.of(student));
            when(reviewRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> reviewService.delete(999L, "mamadou@sencours.sn"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Tests pour getAverageRating()")
    class GetAverageRatingTests {

        @Test
        @DisplayName("Devrait calculer la moyenne correctement")
        void shouldCalculateAverageCorrectly() {
            when(reviewRepository.getAverageRatingByCourseId(1L)).thenReturn(4.333);

            Double result = reviewService.getAverageRating(1L);

            assertThat(result).isEqualTo(4.3);
        }

        @Test
        @DisplayName("Devrait retourner 0.0 si aucun avis")
        void shouldReturnZeroWhenNoReviews() {
            when(reviewRepository.getAverageRatingByCourseId(1L)).thenReturn(null);

            Double result = reviewService.getAverageRating(1L);

            assertThat(result).isEqualTo(0.0);
        }
    }
}
