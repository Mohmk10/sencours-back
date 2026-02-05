package com.sencours.service;

import com.sencours.dto.response.ProgressResponse;
import com.sencours.entity.*;
import com.sencours.enums.LessonType;
import com.sencours.enums.Role;
import com.sencours.exception.EnrollmentNotFoundException;
import com.sencours.exception.ProgressNotFoundException;
import com.sencours.mapper.ProgressMapper;
import com.sencours.repository.EnrollmentRepository;
import com.sencours.repository.ProgressRepository;
import com.sencours.service.impl.ProgressServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProgressServiceTest {

    @Mock
    private ProgressRepository progressRepository;

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private ProgressMapper progressMapper;

    @InjectMocks
    private ProgressServiceImpl progressService;

    private Enrollment enrollment;
    private Lesson lesson;
    private Progress progress;
    private ProgressResponse progressResponse;

    @BeforeEach
    void setUp() {
        User student = new User();
        student.setId(1L);
        student.setFirstName("Mamadou");
        student.setLastName("Diallo");
        student.setRole(Role.ETUDIANT);

        Course course = new Course();
        course.setId(1L);
        course.setTitle("Java pour débutants");

        enrollment = new Enrollment();
        enrollment.setId(1L);
        enrollment.setStudent(student);
        enrollment.setCourse(course);
        enrollment.setEnrolledAt(LocalDateTime.now());

        Section section = new Section();
        section.setId(1L);
        section.setTitle("Introduction");
        section.setCourse(course);

        lesson = new Lesson();
        lesson.setId(1L);
        lesson.setTitle("Bienvenue");
        lesson.setType(LessonType.VIDEO);
        lesson.setOrderIndex(1);
        lesson.setSection(section);

        progress = new Progress();
        progress.setId(1L);
        progress.setEnrollment(enrollment);
        progress.setLesson(lesson);
        progress.setCompleted(false);
        progress.setCompletedAt(null);

        progressResponse = ProgressResponse.builder()
                .id(1L)
                .lessonId(1L)
                .lessonTitle("Bienvenue")
                .lessonOrderIndex(1)
                .completed(false)
                .completedAt(null)
                .build();
    }

    @Nested
    @DisplayName("Tests pour markLessonCompleted()")
    class MarkLessonCompletedTests {

        @Test
        @DisplayName("Devrait marquer une leçon comme complétée")
        void shouldMarkLessonAsCompleted() {
            when(progressRepository.findByEnrollmentIdAndLessonId(1L, 1L)).thenReturn(Optional.of(progress));
            when(progressRepository.save(any(Progress.class))).thenReturn(progress);
            when(progressRepository.countByEnrollmentId(1L)).thenReturn(5);
            when(progressRepository.countByEnrollmentIdAndCompletedTrue(1L)).thenReturn(1);
            when(progressMapper.toResponse(any(Progress.class))).thenReturn(progressResponse);

            ProgressResponse result = progressService.markLessonCompleted(1L, 1L);

            assertThat(result).isNotNull();
            verify(progressRepository).save(any(Progress.class));
        }

        @Test
        @DisplayName("Devrait être idempotent si déjà complétée")
        void shouldBeIdempotentWhenAlreadyCompleted() {
            progress.setCompleted(true);
            progress.setCompletedAt(LocalDateTime.now());

            when(progressRepository.findByEnrollmentIdAndLessonId(1L, 1L)).thenReturn(Optional.of(progress));
            when(progressMapper.toResponse(progress)).thenReturn(progressResponse);

            ProgressResponse result = progressService.markLessonCompleted(1L, 1L);

            assertThat(result).isNotNull();
            verify(progressRepository, never()).save(any(Progress.class));
        }

        @Test
        @DisplayName("Devrait lever exception si progression non trouvée")
        void shouldThrowExceptionWhenProgressNotFound() {
            when(progressRepository.findByEnrollmentIdAndLessonId(1L, 999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> progressService.markLessonCompleted(1L, 999L))
                    .isInstanceOf(ProgressNotFoundException.class);
        }

        @Test
        @DisplayName("Devrait marquer le cours comme complété si 100%")
        void shouldMarkCourseAsCompletedWhen100Percent() {
            when(progressRepository.findByEnrollmentIdAndLessonId(1L, 1L)).thenReturn(Optional.of(progress));
            when(progressRepository.save(any(Progress.class))).thenReturn(progress);
            when(progressRepository.countByEnrollmentId(1L)).thenReturn(1);
            when(progressRepository.countByEnrollmentIdAndCompletedTrue(1L)).thenReturn(1);
            when(enrollmentRepository.findById(1L)).thenReturn(Optional.of(enrollment));
            when(enrollmentRepository.save(any(Enrollment.class))).thenReturn(enrollment);
            when(progressMapper.toResponse(any(Progress.class))).thenReturn(progressResponse);

            progressService.markLessonCompleted(1L, 1L);

            verify(enrollmentRepository).save(any(Enrollment.class));
        }
    }

    @Nested
    @DisplayName("Tests pour markLessonIncomplete()")
    class MarkLessonIncompleteTests {

        @Test
        @DisplayName("Devrait marquer une leçon comme non complétée")
        void shouldMarkLessonAsIncomplete() {
            progress.setCompleted(true);
            progress.setCompletedAt(LocalDateTime.now());

            when(progressRepository.findByEnrollmentIdAndLessonId(1L, 1L)).thenReturn(Optional.of(progress));
            when(progressRepository.save(any(Progress.class))).thenReturn(progress);
            when(progressMapper.toResponse(any(Progress.class))).thenReturn(progressResponse);

            ProgressResponse result = progressService.markLessonIncomplete(1L, 1L);

            assertThat(result).isNotNull();
            verify(progressRepository).save(any(Progress.class));
        }

        @Test
        @DisplayName("Devrait être idempotent si déjà non complétée")
        void shouldBeIdempotentWhenAlreadyIncomplete() {
            progress.setCompleted(false);

            when(progressRepository.findByEnrollmentIdAndLessonId(1L, 1L)).thenReturn(Optional.of(progress));
            when(progressMapper.toResponse(progress)).thenReturn(progressResponse);

            ProgressResponse result = progressService.markLessonIncomplete(1L, 1L);

            assertThat(result).isNotNull();
            verify(progressRepository, never()).save(any(Progress.class));
        }

        @Test
        @DisplayName("Devrait réinitialiser completedAt du cours si marqué incomplet")
        void shouldResetCourseCompletionWhenMarkedIncomplete() {
            progress.setCompleted(true);
            progress.setCompletedAt(LocalDateTime.now());
            enrollment.setCompletedAt(LocalDateTime.now());

            when(progressRepository.findByEnrollmentIdAndLessonId(1L, 1L)).thenReturn(Optional.of(progress));
            when(progressRepository.save(any(Progress.class))).thenReturn(progress);
            when(enrollmentRepository.save(any(Enrollment.class))).thenReturn(enrollment);
            when(progressMapper.toResponse(any(Progress.class))).thenReturn(progressResponse);

            progressService.markLessonIncomplete(1L, 1L);

            verify(enrollmentRepository).save(any(Enrollment.class));
        }
    }

    @Nested
    @DisplayName("Tests pour getProgressByEnrollment()")
    class GetProgressByEnrollmentTests {

        @Test
        @DisplayName("Devrait retourner la liste des progressions")
        void shouldReturnProgressList() {
            when(enrollmentRepository.existsById(1L)).thenReturn(true);
            when(progressRepository.findByEnrollmentId(1L)).thenReturn(List.of(progress));
            when(progressMapper.toResponse(progress)).thenReturn(progressResponse);

            List<ProgressResponse> result = progressService.getProgressByEnrollment(1L);

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("Devrait lever exception si inscription non trouvée")
        void shouldThrowExceptionWhenEnrollmentNotFound() {
            when(enrollmentRepository.existsById(999L)).thenReturn(false);

            assertThatThrownBy(() -> progressService.getProgressByEnrollment(999L))
                    .isInstanceOf(EnrollmentNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Tests pour getProgress()")
    class GetProgressTests {

        @Test
        @DisplayName("Devrait retourner la progression d'une leçon")
        void shouldReturnLessonProgress() {
            when(progressRepository.findByEnrollmentIdAndLessonId(1L, 1L)).thenReturn(Optional.of(progress));
            when(progressMapper.toResponse(progress)).thenReturn(progressResponse);

            ProgressResponse result = progressService.getProgress(1L, 1L);

            assertThat(result).isNotNull();
            assertThat(result.getLessonId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("Devrait lever exception si progression non trouvée")
        void shouldThrowExceptionWhenProgressNotFound() {
            when(progressRepository.findByEnrollmentIdAndLessonId(1L, 999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> progressService.getProgress(1L, 999L))
                    .isInstanceOf(ProgressNotFoundException.class);
        }
    }
}
