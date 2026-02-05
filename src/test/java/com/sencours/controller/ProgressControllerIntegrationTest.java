package com.sencours.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sencours.entity.*;
import com.sencours.enums.LessonType;
import com.sencours.enums.Role;
import com.sencours.enums.Status;
import com.sencours.repository.*;
import com.sencours.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ProgressControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private ProgressRepository progressRepository;

    @Autowired
    private LessonRepository lessonRepository;

    @Autowired
    private SectionRepository sectionRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private Enrollment enrollment;
    private Lesson lesson1;
    private Lesson lesson2;
    private Lesson lesson3;
    private User student;
    private String studentToken;

    private String getBaseUrl() {
        return "/api/v1/enrollments/" + enrollment.getId() + "/lessons";
    }

    @BeforeEach
    void setUp() {
        progressRepository.deleteAll();
        enrollmentRepository.deleteAll();
        lessonRepository.deleteAll();
        sectionRepository.deleteAll();
        courseRepository.deleteAll();
        userRepository.deleteAll();
        categoryRepository.deleteAll();

        User instructor = createInstructor();
        student = createStudent();
        Category category = createCategory();
        Course course = createCourse(instructor, category);
        Section section = createSection(course);
        lesson1 = createLesson(section, "Introduction", 1);
        lesson2 = createLesson(section, "Chapitre 1", 2);
        lesson3 = createLesson(section, "Chapitre 2", 3);
        enrollment = createEnrollment(student, course);
        studentToken = jwtService.generateToken(student);
    }

    @Nested
    @DisplayName("GET /api/v1/enrollments/{enrollmentId}/lessons")
    class GetProgressByEnrollmentTests {

        @Test
        @DisplayName("Devrait retourner la liste des progressions - 200")
        void shouldReturnProgressList() throws Exception {
            mockMvc.perform(get(getBaseUrl())
                            .header("Authorization", "Bearer " + studentToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(3)))
                    .andExpect(jsonPath("$[0].lessonId").value(lesson1.getId()))
                    .andExpect(jsonPath("$[0].completed").value(false));
        }

        @Test
        @DisplayName("Devrait retourner 404 si inscription non trouvée")
        void shouldReturn404WhenEnrollmentNotFound() throws Exception {
            mockMvc.perform(get("/api/v1/enrollments/999/lessons")
                            .header("Authorization", "Bearer " + studentToken))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/enrollments/{enrollmentId}/lessons/{lessonId}")
    class GetProgressTests {

        @Test
        @DisplayName("Devrait retourner la progression d'une leçon - 200")
        void shouldReturnLessonProgress() throws Exception {
            mockMvc.perform(get(getBaseUrl() + "/" + lesson1.getId())
                            .header("Authorization", "Bearer " + studentToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.lessonId").value(lesson1.getId()))
                    .andExpect(jsonPath("$.lessonTitle").value("Introduction"))
                    .andExpect(jsonPath("$.completed").value(false));
        }

        @Test
        @DisplayName("Devrait retourner 404 si progression non trouvée")
        void shouldReturn404WhenProgressNotFound() throws Exception {
            mockMvc.perform(get(getBaseUrl() + "/999")
                            .header("Authorization", "Bearer " + studentToken))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/enrollments/{enrollmentId}/lessons/{lessonId}/complete")
    class MarkLessonCompletedTests {

        @Test
        @DisplayName("Devrait marquer une leçon comme complétée - 200")
        void shouldMarkLessonAsCompleted() throws Exception {
            mockMvc.perform(put(getBaseUrl() + "/" + lesson1.getId() + "/complete")
                            .header("Authorization", "Bearer " + studentToken)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.completed").value(true))
                    .andExpect(jsonPath("$.completedAt").isNotEmpty());

            Progress progress = progressRepository.findByEnrollmentIdAndLessonId(enrollment.getId(), lesson1.getId())
                    .orElseThrow();
            assertThat(progress.getCompleted()).isTrue();
            assertThat(progress.getCompletedAt()).isNotNull();
        }

        @Test
        @DisplayName("Devrait être idempotent si déjà complétée")
        void shouldBeIdempotentWhenAlreadyCompleted() throws Exception {
            Progress progress = progressRepository.findByEnrollmentIdAndLessonId(enrollment.getId(), lesson1.getId())
                    .orElseThrow();
            progress.setCompleted(true);
            progress.setCompletedAt(LocalDateTime.now().minusDays(1));
            progressRepository.save(progress);

            LocalDateTime originalCompletedAt = progress.getCompletedAt();

            mockMvc.perform(put(getBaseUrl() + "/" + lesson1.getId() + "/complete")
                            .header("Authorization", "Bearer " + studentToken)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.completed").value(true));

            Progress updatedProgress = progressRepository.findByEnrollmentIdAndLessonId(enrollment.getId(), lesson1.getId())
                    .orElseThrow();
            assertThat(updatedProgress.getCompletedAt()).isEqualTo(originalCompletedAt);
        }

        @Test
        @DisplayName("Devrait marquer le cours comme complété quand 100%")
        void shouldMarkCourseAsCompletedWhen100Percent() throws Exception {
            progressRepository.findByEnrollmentId(enrollment.getId()).forEach(p -> {
                if (!p.getLesson().getId().equals(lesson3.getId())) {
                    p.setCompleted(true);
                    p.setCompletedAt(LocalDateTime.now());
                    progressRepository.save(p);
                }
            });

            mockMvc.perform(put(getBaseUrl() + "/" + lesson3.getId() + "/complete")
                            .header("Authorization", "Bearer " + studentToken)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());

            Enrollment updatedEnrollment = enrollmentRepository.findById(enrollment.getId()).orElseThrow();
            assertThat(updatedEnrollment.getCompletedAt()).isNotNull();
        }

        @Test
        @DisplayName("Devrait retourner 404 si progression non trouvée")
        void shouldReturn404WhenProgressNotFound() throws Exception {
            mockMvc.perform(put(getBaseUrl() + "/999/complete")
                            .header("Authorization", "Bearer " + studentToken)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/enrollments/{enrollmentId}/lessons/{lessonId}/incomplete")
    class MarkLessonIncompleteTests {

        @Test
        @DisplayName("Devrait marquer une leçon comme non complétée - 200")
        void shouldMarkLessonAsIncomplete() throws Exception {
            Progress progress = progressRepository.findByEnrollmentIdAndLessonId(enrollment.getId(), lesson1.getId())
                    .orElseThrow();
            progress.setCompleted(true);
            progress.setCompletedAt(LocalDateTime.now());
            progressRepository.save(progress);

            mockMvc.perform(put(getBaseUrl() + "/" + lesson1.getId() + "/incomplete")
                            .header("Authorization", "Bearer " + studentToken)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.completed").value(false))
                    .andExpect(jsonPath("$.completedAt").isEmpty());

            Progress updatedProgress = progressRepository.findByEnrollmentIdAndLessonId(enrollment.getId(), lesson1.getId())
                    .orElseThrow();
            assertThat(updatedProgress.getCompleted()).isFalse();
            assertThat(updatedProgress.getCompletedAt()).isNull();
        }

        @Test
        @DisplayName("Devrait être idempotent si déjà non complétée")
        void shouldBeIdempotentWhenAlreadyIncomplete() throws Exception {
            mockMvc.perform(put(getBaseUrl() + "/" + lesson1.getId() + "/incomplete")
                            .header("Authorization", "Bearer " + studentToken)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.completed").value(false));
        }

        @Test
        @DisplayName("Devrait réinitialiser completedAt du cours")
        void shouldResetCourseCompletion() throws Exception {
            progressRepository.findByEnrollmentId(enrollment.getId()).forEach(p -> {
                p.setCompleted(true);
                p.setCompletedAt(LocalDateTime.now());
                progressRepository.save(p);
            });

            enrollment.setCompletedAt(LocalDateTime.now());
            enrollmentRepository.save(enrollment);

            mockMvc.perform(put(getBaseUrl() + "/" + lesson1.getId() + "/incomplete")
                            .header("Authorization", "Bearer " + studentToken)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());

            Enrollment updatedEnrollment = enrollmentRepository.findById(enrollment.getId()).orElseThrow();
            assertThat(updatedEnrollment.getCompletedAt()).isNull();
        }
    }

    private User createInstructor() {
        User user = User.builder()
                .firstName("Prof")
                .lastName("Diop")
                .email("prof@sencours.sn")
                .password(passwordEncoder.encode("password123"))
                .role(Role.INSTRUCTEUR)
                .build();
        return userRepository.save(user);
    }

    private User createStudent() {
        User user = User.builder()
                .firstName("Mamadou")
                .lastName("Diallo")
                .email("mamadou@sencours.sn")
                .password(passwordEncoder.encode("password123"))
                .role(Role.ETUDIANT)
                .build();
        return userRepository.save(user);
    }

    private Category createCategory() {
        Category cat = new Category();
        cat.setName("Développement Web");
        cat.setDescription("Cours de développement web");
        return categoryRepository.save(cat);
    }

    private Course createCourse(User instructor, Category category) {
        Course c = new Course();
        c.setTitle("Java pour débutants");
        c.setDescription("Apprenez Java");
        c.setPrice(new BigDecimal("25000"));
        c.setStatus(Status.PUBLISHED);
        c.setInstructor(instructor);
        c.setCategory(category);
        return courseRepository.save(c);
    }

    private Section createSection(Course course) {
        Section s = new Section();
        s.setTitle("Introduction");
        s.setOrderIndex(1);
        s.setCourse(course);
        return sectionRepository.save(s);
    }

    private Lesson createLesson(Section section, String title, int orderIndex) {
        Lesson l = new Lesson();
        l.setTitle(title);
        l.setType(LessonType.VIDEO);
        l.setContent("https://video.url/" + title.toLowerCase());
        l.setDuration(10);
        l.setOrderIndex(orderIndex);
        l.setIsFree(false);
        l.setSection(section);
        return lessonRepository.save(l);
    }

    private Enrollment createEnrollment(User student, Course course) {
        Enrollment e = new Enrollment();
        e.setStudent(student);
        e.setCourse(course);
        e = enrollmentRepository.save(e);

        for (Lesson lesson : lessonRepository.findByCourseIdOrderByOrderIndex(course.getId())) {
            Progress progress = new Progress();
            progress.setEnrollment(e);
            progress.setLesson(lesson);
            progress.setCompleted(false);
            progressRepository.save(progress);
        }

        return e;
    }
}
