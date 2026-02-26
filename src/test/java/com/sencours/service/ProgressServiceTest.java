package com.sencours.service;

import com.sencours.dto.request.ProgressRequest;
import com.sencours.dto.response.ProgressResponse;
import com.sencours.entity.*;
import com.sencours.enums.LessonType;
import com.sencours.enums.Role;
import com.sencours.exception.BadRequestException;
import com.sencours.exception.ResourceNotFoundException;
import com.sencours.repository.*;
import com.sencours.service.impl.ProgressServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
    private LessonRepository lessonRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private EnrollmentService enrollmentService;

    @InjectMocks
    private ProgressServiceImpl progressService;

    private User student;
    private Course course;
    private Section section;
    private Lesson lesson;
    private Progress progress;

    @BeforeEach
    void setUp() {
        student = User.builder()
                .id(1L).firstName("Mamadou").lastName("Diallo")
                .email("mamadou@sencours.sn").role(Role.ETUDIANT).build();

        User instructor = User.builder()
                .id(2L).firstName("Prof").lastName("Diop")
                .email("prof@sencours.sn").role(Role.INSTRUCTEUR).build();

        course = new Course();
        course.setId(1L);
        course.setTitle("Java pour débutants");
        course.setInstructor(instructor);

        section = new Section();
        section.setId(1L);
        section.setTitle("Introduction");
        section.setCourse(course);

        lesson = new Lesson();
        lesson.setId(1L);
        lesson.setTitle("Bienvenue");
        lesson.setType(LessonType.VIDEO);
        lesson.setOrderIndex(1);
        lesson.setIsFree(false);
        lesson.setSection(section);

        progress = Progress.builder()
                .id(1L)
                .user(student)
                .lesson(lesson)
                .completed(false)
                .watchTimeSeconds(0)
                .lastPositionSeconds(0)
                .build();
    }

    @Nested
    @DisplayName("Tests pour updateProgress()")
    class UpdateProgressTests {

        @Test
        @DisplayName("Devrait mettre à jour la progression avec succès")
        void shouldUpdateProgressSuccessfully() {
            when(userRepository.findByEmail("mamadou@sencours.sn")).thenReturn(Optional.of(student));
            when(lessonRepository.findById(1L)).thenReturn(Optional.of(lesson));
            when(enrollmentRepository.existsByUserIdAndCourseId(1L, 1L)).thenReturn(true);
            when(progressRepository.findByUserIdAndLessonId(1L, 1L)).thenReturn(Optional.of(progress));
            when(progressRepository.save(any(Progress.class))).thenReturn(progress);

            ProgressRequest request = new ProgressRequest();
            request.setWatchTimeSeconds(120);
            request.setLastPositionSeconds(100);

            ProgressResponse result = progressService.updateProgress(1L, request, "mamadou@sencours.sn");

            assertThat(result).isNotNull();
            assertThat(result.getLessonId()).isEqualTo(1L);
            verify(progressRepository).save(any(Progress.class));
        }

        @Test
        @DisplayName("Devrait créer la progression si elle n'existe pas")
        void shouldCreateProgressWhenNotExists() {
            when(userRepository.findByEmail("mamadou@sencours.sn")).thenReturn(Optional.of(student));
            when(lessonRepository.findById(1L)).thenReturn(Optional.of(lesson));
            when(enrollmentRepository.existsByUserIdAndCourseId(1L, 1L)).thenReturn(true);
            when(progressRepository.findByUserIdAndLessonId(1L, 1L)).thenReturn(Optional.empty());
            when(progressRepository.save(any(Progress.class))).thenReturn(progress);

            ProgressRequest request = new ProgressRequest();
            request.setCompleted(true);

            ProgressResponse result = progressService.updateProgress(1L, request, "mamadou@sencours.sn");

            assertThat(result).isNotNull();
            verify(progressRepository).save(any(Progress.class));
        }

        @Test
        @DisplayName("Devrait lever exception si non inscrit et leçon payante")
        void shouldThrowExceptionWhenNotEnrolledAndLessonNotFree() {
            when(userRepository.findByEmail("mamadou@sencours.sn")).thenReturn(Optional.of(student));
            when(lessonRepository.findById(1L)).thenReturn(Optional.of(lesson));
            when(enrollmentRepository.existsByUserIdAndCourseId(1L, 1L)).thenReturn(false);

            ProgressRequest request = new ProgressRequest();

            assertThatThrownBy(() -> progressService.updateProgress(1L, request, "mamadou@sencours.sn"))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("inscrit");
        }

        @Test
        @DisplayName("Devrait permettre l'accès aux leçons gratuites sans inscription")
        void shouldAllowFreeLesson() {
            lesson.setIsFree(true);
            when(userRepository.findByEmail("mamadou@sencours.sn")).thenReturn(Optional.of(student));
            when(lessonRepository.findById(1L)).thenReturn(Optional.of(lesson));
            when(progressRepository.findByUserIdAndLessonId(1L, 1L)).thenReturn(Optional.of(progress));
            when(progressRepository.save(any(Progress.class))).thenReturn(progress);
            when(enrollmentRepository.existsByUserIdAndCourseId(1L, 1L)).thenReturn(false);

            ProgressRequest request = new ProgressRequest();
            request.setWatchTimeSeconds(60);

            ProgressResponse result = progressService.updateProgress(1L, request, "mamadou@sencours.sn");

            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("Devrait lever exception si utilisateur non trouvé")
        void shouldThrowExceptionWhenUserNotFound() {
            when(userRepository.findByEmail("unknown@sencours.sn")).thenReturn(Optional.empty());

            ProgressRequest request = new ProgressRequest();

            assertThatThrownBy(() -> progressService.updateProgress(1L, request, "unknown@sencours.sn"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("Devrait lever exception si leçon non trouvée")
        void shouldThrowExceptionWhenLessonNotFound() {
            when(userRepository.findByEmail("mamadou@sencours.sn")).thenReturn(Optional.of(student));
            when(lessonRepository.findById(999L)).thenReturn(Optional.empty());

            ProgressRequest request = new ProgressRequest();

            assertThatThrownBy(() -> progressService.updateProgress(999L, request, "mamadou@sencours.sn"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Tests pour getProgress()")
    class GetProgressTests {

        @Test
        @DisplayName("Devrait retourner la progression d'une leçon")
        void shouldReturnLessonProgress() {
            when(userRepository.findByEmail("mamadou@sencours.sn")).thenReturn(Optional.of(student));
            when(progressRepository.findByUserIdAndLessonId(1L, 1L)).thenReturn(Optional.of(progress));

            ProgressResponse result = progressService.getProgress(1L, "mamadou@sencours.sn");

            assertThat(result).isNotNull();
            assertThat(result.getLessonId()).isEqualTo(1L);
            assertThat(result.getCompleted()).isFalse();
        }

        @Test
        @DisplayName("Devrait retourner une progression vide si non trouvée")
        void shouldReturnEmptyProgressWhenNotFound() {
            when(userRepository.findByEmail("mamadou@sencours.sn")).thenReturn(Optional.of(student));
            when(progressRepository.findByUserIdAndLessonId(1L, 1L)).thenReturn(Optional.empty());

            ProgressResponse result = progressService.getProgress(1L, "mamadou@sencours.sn");

            assertThat(result).isNotNull();
            assertThat(result.getLessonId()).isEqualTo(1L);
            assertThat(result.getCompleted()).isFalse();
            assertThat(result.getWatchTimeSeconds()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("Tests pour getCourseProgress()")
    class GetCourseProgressTests {

        @Test
        @DisplayName("Devrait retourner les progressions d'un cours")
        void shouldReturnCourseProgress() {
            when(userRepository.findByEmail("mamadou@sencours.sn")).thenReturn(Optional.of(student));
            when(progressRepository.findByUserIdAndCourseId(1L, 1L)).thenReturn(List.of(progress));

            List<ProgressResponse> result = progressService.getCourseProgress(1L, "mamadou@sencours.sn");

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("Devrait lever exception si utilisateur non trouvé")
        void shouldThrowExceptionWhenUserNotFound() {
            when(userRepository.findByEmail("unknown@sencours.sn")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> progressService.getCourseProgress(1L, "unknown@sencours.sn"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Tests pour markAsCompleted()")
    class MarkAsCompletedTests {

        @Test
        @DisplayName("Devrait marquer une leçon comme complétée")
        void shouldMarkLessonAsCompleted() {
            when(userRepository.findByEmail("mamadou@sencours.sn")).thenReturn(Optional.of(student));
            when(lessonRepository.findById(1L)).thenReturn(Optional.of(lesson));
            when(enrollmentRepository.existsByUserIdAndCourseId(1L, 1L)).thenReturn(true);
            when(progressRepository.findByUserIdAndLessonId(1L, 1L)).thenReturn(Optional.of(progress));
            when(progressRepository.save(any(Progress.class))).thenReturn(progress);

            progressService.markAsCompleted(1L, "mamadou@sencours.sn");

            verify(progressRepository).save(any(Progress.class));
        }
    }
}
