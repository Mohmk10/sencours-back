package com.sencours.service;

import com.sencours.dto.request.EnrollmentRequest;
import com.sencours.dto.response.EnrollmentDetailResponse;
import com.sencours.dto.response.EnrollmentResponse;
import com.sencours.dto.response.ProgressResponse;
import com.sencours.dto.response.ProgressSummaryResponse;
import com.sencours.entity.*;
import com.sencours.enums.Role;
import com.sencours.enums.Status;
import com.sencours.exception.AlreadyEnrolledException;
import com.sencours.exception.EnrollmentNotFoundException;
import com.sencours.exception.ResourceNotFoundException;
import com.sencours.exception.UnauthorizedReviewAccessException;
import com.sencours.mapper.EnrollmentMapper;
import com.sencours.mapper.ProgressMapper;
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
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EnrollmentServiceTest {

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private LessonRepository lessonRepository;

    @Mock
    private ProgressRepository progressRepository;

    @Mock
    private EnrollmentMapper enrollmentMapper;

    @Mock
    private ProgressMapper progressMapper;

    @InjectMocks
    private EnrollmentServiceImpl enrollmentService;

    private User student;
    private User instructor;
    private Course course;
    private Enrollment enrollment;
    private EnrollmentRequest enrollmentRequest;
    private EnrollmentResponse enrollmentResponse;

    @BeforeEach
    void setUp() {
        instructor = new User();
        instructor.setId(1L);
        instructor.setFirstName("Prof");
        instructor.setLastName("Diop");
        instructor.setEmail("prof@sencours.sn");
        instructor.setRole(Role.INSTRUCTEUR);

        student = new User();
        student.setId(2L);
        student.setFirstName("Mamadou");
        student.setLastName("Diallo");
        student.setEmail("mamadou@sencours.sn");
        student.setRole(Role.ETUDIANT);

        course = new Course();
        course.setId(1L);
        course.setTitle("Java pour débutants");
        course.setPrice(new BigDecimal("25000"));
        course.setStatus(Status.PUBLISHED);
        course.setInstructor(instructor);

        enrollment = new Enrollment();
        enrollment.setId(1L);
        enrollment.setStudent(student);
        enrollment.setCourse(course);
        enrollment.setEnrolledAt(LocalDateTime.now());

        enrollmentRequest = EnrollmentRequest.builder()
                .courseId(1L)
                .build();

        enrollmentResponse = EnrollmentResponse.builder()
                .id(1L)
                .userId(2L)
                .userFirstName("Mamadou")
                .userLastName("Diallo")
                .courseId(1L)
                .courseTitle("Java pour débutants")
                .progressPercentage(0.0)
                .build();
    }

    @Nested
    @DisplayName("Tests pour enroll()")
    class EnrollTests {

        @Test
        @DisplayName("Devrait inscrire un étudiant avec succès")
        void shouldEnrollStudentSuccessfully() {
            Lesson lesson = new Lesson();
            lesson.setId(1L);
            lesson.setTitle("Introduction");

            when(userRepository.findById(2L)).thenReturn(Optional.of(student));
            when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
            when(enrollmentRepository.existsByStudentIdAndCourseId(2L, 1L)).thenReturn(false);
            when(enrollmentRepository.save(any(Enrollment.class))).thenReturn(enrollment);
            when(lessonRepository.findByCourseIdOrderByOrderIndex(1L)).thenReturn(List.of(lesson));
            when(progressRepository.save(any(Progress.class))).thenReturn(new Progress());
            when(enrollmentMapper.toResponse(any(Enrollment.class), anyDouble())).thenReturn(enrollmentResponse);

            EnrollmentResponse result = enrollmentService.enroll(2L, enrollmentRequest);

            assertThat(result).isNotNull();
            assertThat(result.getUserId()).isEqualTo(2L);
            assertThat(result.getCourseId()).isEqualTo(1L);
            verify(progressRepository, times(1)).save(any(Progress.class));
        }

        @Test
        @DisplayName("Devrait créer un Progress pour chaque leçon")
        void shouldCreateProgressForEachLesson() {
            Lesson lesson1 = new Lesson();
            lesson1.setId(1L);
            Lesson lesson2 = new Lesson();
            lesson2.setId(2L);
            Lesson lesson3 = new Lesson();
            lesson3.setId(3L);

            when(userRepository.findById(2L)).thenReturn(Optional.of(student));
            when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
            when(enrollmentRepository.existsByStudentIdAndCourseId(2L, 1L)).thenReturn(false);
            when(enrollmentRepository.save(any(Enrollment.class))).thenReturn(enrollment);
            when(lessonRepository.findByCourseIdOrderByOrderIndex(1L)).thenReturn(Arrays.asList(lesson1, lesson2, lesson3));
            when(progressRepository.save(any(Progress.class))).thenReturn(new Progress());
            when(enrollmentMapper.toResponse(any(Enrollment.class), anyDouble())).thenReturn(enrollmentResponse);

            enrollmentService.enroll(2L, enrollmentRequest);

            verify(progressRepository, times(3)).save(any(Progress.class));
        }

        @Test
        @DisplayName("Devrait lever ResourceNotFoundException si utilisateur non trouvé")
        void shouldThrowExceptionWhenUserNotFound() {
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            EnrollmentRequest request = EnrollmentRequest.builder()
                    .courseId(1L)
                    .build();

            assertThatThrownBy(() -> enrollmentService.enroll(999L, request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Utilisateur");
        }

        @Test
        @DisplayName("Devrait lever ResourceNotFoundException si cours non trouvé")
        void shouldThrowExceptionWhenCourseNotFound() {
            when(userRepository.findById(2L)).thenReturn(Optional.of(student));
            when(courseRepository.findById(999L)).thenReturn(Optional.empty());

            EnrollmentRequest request = EnrollmentRequest.builder()
                    .courseId(999L)
                    .build();

            assertThatThrownBy(() -> enrollmentService.enroll(2L, request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Cours");
        }

        @Test
        @DisplayName("Devrait lever AlreadyEnrolledException si déjà inscrit")
        void shouldThrowExceptionWhenAlreadyEnrolled() {
            when(userRepository.findById(2L)).thenReturn(Optional.of(student));
            when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
            when(enrollmentRepository.existsByStudentIdAndCourseId(2L, 1L)).thenReturn(true);

            assertThatThrownBy(() -> enrollmentService.enroll(2L, enrollmentRequest))
                    .isInstanceOf(AlreadyEnrolledException.class);
        }

        @Test
        @DisplayName("Devrait lever exception si utilisateur n'est pas ETUDIANT")
        void shouldThrowExceptionWhenUserNotStudent() {
            instructor.setId(3L);
            when(userRepository.findById(3L)).thenReturn(Optional.of(instructor));
            when(courseRepository.findById(1L)).thenReturn(Optional.of(course));

            EnrollmentRequest request = EnrollmentRequest.builder()
                    .courseId(1L)
                    .build();

            assertThatThrownBy(() -> enrollmentService.enroll(3L, request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("ETUDIANT");
        }

        @Test
        @DisplayName("Devrait lever exception si instructeur tente de s'inscrire à son propre cours")
        void shouldThrowExceptionWhenInstructorEnrollsOwnCourse() {
            User instructorAsStudent = new User();
            instructorAsStudent.setId(1L);
            instructorAsStudent.setRole(Role.ETUDIANT);

            when(userRepository.findById(1L)).thenReturn(Optional.of(instructorAsStudent));
            when(courseRepository.findById(1L)).thenReturn(Optional.of(course));

            EnrollmentRequest request = EnrollmentRequest.builder()
                    .courseId(1L)
                    .build();

            assertThatThrownBy(() -> enrollmentService.enroll(1L, request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("propre cours");
        }
    }

    @Nested
    @DisplayName("Tests pour getMyEnrollments()")
    class GetMyEnrollmentsTests {

        @Test
        @DisplayName("Devrait retourner les inscriptions d'un utilisateur")
        void shouldReturnUserEnrollments() {
            when(userRepository.existsById(2L)).thenReturn(true);
            when(enrollmentRepository.findByStudentId(2L)).thenReturn(List.of(enrollment));
            when(progressRepository.countByEnrollmentId(1L)).thenReturn(5);
            when(progressRepository.countByEnrollmentIdAndCompletedTrue(1L)).thenReturn(3);
            when(enrollmentMapper.toResponse(any(Enrollment.class), anyDouble())).thenReturn(enrollmentResponse);

            List<EnrollmentResponse> result = enrollmentService.getMyEnrollments(2L);

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("Devrait lever exception si utilisateur non trouvé")
        void shouldThrowExceptionWhenUserNotFound() {
            when(userRepository.existsById(999L)).thenReturn(false);

            assertThatThrownBy(() -> enrollmentService.getMyEnrollments(999L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Tests pour calculateProgress()")
    class CalculateProgressTests {

        @Test
        @DisplayName("Devrait calculer 0% si aucune leçon complétée")
        void shouldCalculateZeroPercent() {
            when(enrollmentRepository.existsById(1L)).thenReturn(true);
            when(progressRepository.countByEnrollmentId(1L)).thenReturn(5);
            when(progressRepository.countByEnrollmentIdAndCompletedTrue(1L)).thenReturn(0);

            ProgressSummaryResponse result = enrollmentService.calculateProgress(1L);

            assertThat(result.getTotalLessons()).isEqualTo(5);
            assertThat(result.getCompletedLessons()).isEqualTo(0);
            assertThat(result.getPercentage()).isEqualTo(0.0);
        }

        @Test
        @DisplayName("Devrait calculer 100% si toutes les leçons complétées")
        void shouldCalculateHundredPercent() {
            when(enrollmentRepository.existsById(1L)).thenReturn(true);
            when(progressRepository.countByEnrollmentId(1L)).thenReturn(5);
            when(progressRepository.countByEnrollmentIdAndCompletedTrue(1L)).thenReturn(5);

            ProgressSummaryResponse result = enrollmentService.calculateProgress(1L);

            assertThat(result.getTotalLessons()).isEqualTo(5);
            assertThat(result.getCompletedLessons()).isEqualTo(5);
            assertThat(result.getPercentage()).isEqualTo(100.0);
        }

        @Test
        @DisplayName("Devrait calculer 60% pour 3/5 leçons complétées")
        void shouldCalculatePartialPercent() {
            when(enrollmentRepository.existsById(1L)).thenReturn(true);
            when(progressRepository.countByEnrollmentId(1L)).thenReturn(5);
            when(progressRepository.countByEnrollmentIdAndCompletedTrue(1L)).thenReturn(3);

            ProgressSummaryResponse result = enrollmentService.calculateProgress(1L);

            assertThat(result.getTotalLessons()).isEqualTo(5);
            assertThat(result.getCompletedLessons()).isEqualTo(3);
            assertThat(result.getPercentage()).isEqualTo(60.0);
        }

        @Test
        @DisplayName("Devrait retourner 100% si aucune leçon dans le cours")
        void shouldReturnHundredPercentWhenNoLessons() {
            when(enrollmentRepository.existsById(1L)).thenReturn(true);
            when(progressRepository.countByEnrollmentId(1L)).thenReturn(0);
            when(progressRepository.countByEnrollmentIdAndCompletedTrue(1L)).thenReturn(0);

            ProgressSummaryResponse result = enrollmentService.calculateProgress(1L);

            assertThat(result.getTotalLessons()).isEqualTo(0);
            assertThat(result.getCompletedLessons()).isEqualTo(0);
            assertThat(result.getPercentage()).isEqualTo(100.0);
        }

        @Test
        @DisplayName("Devrait lever exception si inscription non trouvée")
        void shouldThrowExceptionWhenEnrollmentNotFound() {
            when(enrollmentRepository.existsById(999L)).thenReturn(false);

            assertThatThrownBy(() -> enrollmentService.calculateProgress(999L))
                    .isInstanceOf(EnrollmentNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Tests pour getEnrollmentDetail()")
    class GetEnrollmentDetailTests {

        @Test
        @DisplayName("Devrait retourner le détail avec progression")
        void shouldReturnDetailWithProgress() {
            Progress progress = new Progress();
            progress.setId(1L);
            progress.setCompleted(true);

            Lesson lesson = new Lesson();
            lesson.setId(1L);
            lesson.setTitle("Introduction");
            lesson.setOrderIndex(1);
            progress.setLesson(lesson);

            ProgressResponse progressResponse = ProgressResponse.builder()
                    .id(1L)
                    .lessonId(1L)
                    .lessonTitle("Introduction")
                    .completed(true)
                    .build();

            EnrollmentDetailResponse detailResponse = EnrollmentDetailResponse.builder()
                    .id(1L)
                    .userId(2L)
                    .courseId(1L)
                    .progressPercentage(100.0)
                    .progresses(List.of(progressResponse))
                    .build();

            when(enrollmentRepository.findById(1L)).thenReturn(Optional.of(enrollment));
            when(progressRepository.countByEnrollmentId(1L)).thenReturn(1);
            when(progressRepository.countByEnrollmentIdAndCompletedTrue(1L)).thenReturn(1);
            when(progressRepository.findByEnrollmentId(1L)).thenReturn(List.of(progress));
            when(progressMapper.toResponse(progress)).thenReturn(progressResponse);
            when(enrollmentMapper.toDetailResponse(any(), anyDouble(), any())).thenReturn(detailResponse);

            EnrollmentDetailResponse result = enrollmentService.getEnrollmentDetail(1L);

            assertThat(result).isNotNull();
            assertThat(result.getProgresses()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("Tests pour unenroll()")
    class UnenrollTests {

        @Test
        @DisplayName("Devrait permettre à l'utilisateur de se désinscrire")
        void shouldAllowUserToUnenroll() {
            when(enrollmentRepository.findById(1L)).thenReturn(Optional.of(enrollment));

            enrollmentService.unenroll(1L, 2L);

            verify(enrollmentRepository).delete(enrollment);
        }

        @Test
        @DisplayName("Devrait lever exception si pas le propriétaire de l'inscription")
        void shouldThrowExceptionWhenNotOwner() {
            when(enrollmentRepository.findById(1L)).thenReturn(Optional.of(enrollment));

            assertThatThrownBy(() -> enrollmentService.unenroll(1L, 999L))
                    .isInstanceOf(UnauthorizedReviewAccessException.class);
        }

        @Test
        @DisplayName("Devrait lever exception si inscription non trouvée")
        void shouldThrowExceptionWhenEnrollmentNotFound() {
            when(enrollmentRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> enrollmentService.unenroll(999L, 2L))
                    .isInstanceOf(EnrollmentNotFoundException.class);
        }
    }
}
