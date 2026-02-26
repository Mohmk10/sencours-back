package com.sencours.service;

import com.sencours.dto.request.EnrollmentRequest;
import com.sencours.dto.response.EnrollmentResponse;
import com.sencours.dto.response.PaymentResponse;
import com.sencours.entity.*;
import com.sencours.enums.Role;
import com.sencours.enums.Status;
import com.sencours.exception.BadRequestException;
import com.sencours.exception.ResourceNotFoundException;
import com.sencours.repository.*;
import com.sencours.service.impl.EnrollmentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EnrollmentServiceTest {

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProgressRepository progressRepository;

    @InjectMocks
    private EnrollmentServiceImpl enrollmentService;

    private User student;
    private User instructor;
    private Course course;
    private Enrollment enrollment;

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
        course.setSections(new ArrayList<>());

        enrollment = Enrollment.builder()
                .id(1L).user(student).course(course)
                .progressPercentage(0).build();
    }

    @Nested
    @DisplayName("Tests pour initiatePayment()")
    class InitiatePaymentTests {

        @Test
        @DisplayName("Devrait initier un paiement avec succès")
        void shouldInitiatePaymentSuccessfully() {
            when(userRepository.findByEmail("mamadou@sencours.sn")).thenReturn(Optional.of(student));
            when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
            when(enrollmentRepository.existsByUserIdAndCourseId(2L, 1L)).thenReturn(false);

            EnrollmentRequest request = EnrollmentRequest.builder().paymentMethod("ORANGE_MONEY").build();
            PaymentResponse result = enrollmentService.initiatePayment(1L, request, "mamadou@sencours.sn");

            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo("SUCCESS");
            assertThat(result.getAmount()).isEqualByComparingTo(new BigDecimal("25000"));
            assertThat(result.getReference()).startsWith("PAY-");
        }

        @Test
        @DisplayName("Devrait lever exception si déjà inscrit")
        void shouldThrowExceptionWhenAlreadyEnrolled() {
            when(userRepository.findByEmail("mamadou@sencours.sn")).thenReturn(Optional.of(student));
            when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
            when(enrollmentRepository.existsByUserIdAndCourseId(2L, 1L)).thenReturn(true);

            EnrollmentRequest request = EnrollmentRequest.builder().paymentMethod("WAVE").build();

            assertThatThrownBy(() -> enrollmentService.initiatePayment(1L, request, "mamadou@sencours.sn"))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("déjà inscrit");
        }
    }

    @Nested
    @DisplayName("Tests pour enrollFree()")
    class EnrollFreeTests {

        @Test
        @DisplayName("Devrait inscrire gratuitement avec succès")
        void shouldEnrollFreeSuccessfully() {
            course.setPrice(BigDecimal.ZERO);
            when(userRepository.findByEmail("mamadou@sencours.sn")).thenReturn(Optional.of(student));
            when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
            when(enrollmentRepository.existsByUserIdAndCourseId(2L, 1L)).thenReturn(false);
            when(enrollmentRepository.save(any(Enrollment.class))).thenReturn(enrollment);
            when(progressRepository.countCompletedLessonsByUserAndCourse(2L, 1L)).thenReturn(0L);

            EnrollmentResponse result = enrollmentService.enrollFree(1L, "mamadou@sencours.sn");

            assertThat(result).isNotNull();
            verify(enrollmentRepository).save(any(Enrollment.class));
        }

        @Test
        @DisplayName("Devrait lever exception si cours pas gratuit")
        void shouldThrowExceptionWhenCourseNotFree() {
            when(userRepository.findByEmail("mamadou@sencours.sn")).thenReturn(Optional.of(student));
            when(courseRepository.findById(1L)).thenReturn(Optional.of(course));

            assertThatThrownBy(() -> enrollmentService.enrollFree(1L, "mamadou@sencours.sn"))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("pas gratuit");
        }
    }

    @Nested
    @DisplayName("Tests pour isEnrolled()")
    class IsEnrolledTests {

        @Test
        @DisplayName("Devrait retourner true si inscrit")
        void shouldReturnTrueWhenEnrolled() {
            when(userRepository.findByEmail("mamadou@sencours.sn")).thenReturn(Optional.of(student));
            when(enrollmentRepository.existsByUserIdAndCourseId(2L, 1L)).thenReturn(true);

            boolean result = enrollmentService.isEnrolled(1L, "mamadou@sencours.sn");

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Devrait retourner false si pas inscrit")
        void shouldReturnFalseWhenNotEnrolled() {
            when(userRepository.findByEmail("mamadou@sencours.sn")).thenReturn(Optional.of(student));
            when(enrollmentRepository.existsByUserIdAndCourseId(2L, 1L)).thenReturn(false);

            boolean result = enrollmentService.isEnrolled(1L, "mamadou@sencours.sn");

            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("Tests pour getMyEnrollments()")
    class GetMyEnrollmentsTests {

        @Test
        @DisplayName("Devrait retourner les inscriptions de l'utilisateur")
        void shouldReturnUserEnrollments() {
            when(userRepository.findByEmail("mamadou@sencours.sn")).thenReturn(Optional.of(student));
            when(enrollmentRepository.findByUserIdOrderByEnrolledAtDesc(2L)).thenReturn(List.of(enrollment));
            when(progressRepository.countCompletedLessonsByUserAndCourse(2L, 1L)).thenReturn(0L);

            List<EnrollmentResponse> result = enrollmentService.getMyEnrollments("mamadou@sencours.sn");

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("Devrait lever exception si utilisateur non trouvé")
        void shouldThrowExceptionWhenUserNotFound() {
            when(userRepository.findByEmail("unknown@sencours.sn")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> enrollmentService.getMyEnrollments("unknown@sencours.sn"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}
