package com.sencours.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sencours.dto.request.ProgressRequest;
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
    private ReviewRepository reviewRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private static final String BASE_URL = "/api/v1/progress";

    private User student;
    private Course course;
    private Lesson lesson1;
    private Lesson lesson2;
    private String studentToken;

    @BeforeEach
    void setUp() {
        reviewRepository.deleteAll();
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
        course = createCourse(instructor, category);
        Section section = createSection(course);
        lesson1 = createLesson(section, "Introduction", 1);
        lesson2 = createLesson(section, "Chapitre 1", 2);
        createEnrollment(student, course);
        studentToken = jwtService.generateToken(student);
    }

    @Nested
    @DisplayName("PUT /api/v1/progress/lessons/{lessonId}")
    class UpdateProgressTests {

        @Test
        @DisplayName("Devrait mettre à jour la progression - 200")
        void shouldUpdateProgressSuccessfully() throws Exception {
            ProgressRequest request = new ProgressRequest();
            request.setWatchTimeSeconds(120);
            request.setLastPositionSeconds(100);

            mockMvc.perform(put(BASE_URL + "/lessons/" + lesson1.getId())
                            .header("Authorization", "Bearer " + studentToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.lessonId").value(lesson1.getId()))
                    .andExpect(jsonPath("$.watchTimeSeconds").value(120))
                    .andExpect(jsonPath("$.lastPositionSeconds").value(100));
        }

        @Test
        @DisplayName("Devrait marquer comme complété - 200")
        void shouldMarkAsCompleted() throws Exception {
            ProgressRequest request = new ProgressRequest();
            request.setCompleted(true);

            mockMvc.perform(put(BASE_URL + "/lessons/" + lesson1.getId())
                            .header("Authorization", "Bearer " + studentToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.completed").value(true))
                    .andExpect(jsonPath("$.completedAt").isNotEmpty());
        }

        @Test
        @DisplayName("Devrait retourner 401 sans authentification")
        void shouldReturn401WithoutAuth() throws Exception {
            ProgressRequest request = new ProgressRequest();

            mockMvc.perform(put(BASE_URL + "/lessons/" + lesson1.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/progress/lessons/{lessonId}/complete")
    class MarkAsCompletedTests {

        @Test
        @DisplayName("Devrait marquer une leçon comme complétée - 200")
        void shouldMarkLessonAsCompleted() throws Exception {
            mockMvc.perform(post(BASE_URL + "/lessons/" + lesson1.getId() + "/complete")
                            .header("Authorization", "Bearer " + studentToken))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/progress/lessons/{lessonId}")
    class GetProgressTests {

        @Test
        @DisplayName("Devrait retourner la progression d'une leçon - 200")
        void shouldReturnLessonProgress() throws Exception {
            mockMvc.perform(get(BASE_URL + "/lessons/" + lesson1.getId())
                            .header("Authorization", "Bearer " + studentToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.lessonId").value(lesson1.getId()))
                    .andExpect(jsonPath("$.completed").value(false));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/progress/courses/{courseId}")
    class GetCourseProgressTests {

        @Test
        @DisplayName("Devrait retourner les progressions du cours - 200")
        void shouldReturnCourseProgress() throws Exception {
            // Create some progress first
            ProgressRequest request = new ProgressRequest();
            request.setWatchTimeSeconds(60);

            mockMvc.perform(put(BASE_URL + "/lessons/" + lesson1.getId())
                    .header("Authorization", "Bearer " + studentToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)));

            mockMvc.perform(get(BASE_URL + "/courses/" + course.getId())
                            .header("Authorization", "Bearer " + studentToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));
        }

        @Test
        @DisplayName("Devrait retourner liste vide si aucune progression - 200")
        void shouldReturnEmptyListWhenNoProgress() throws Exception {
            mockMvc.perform(get(BASE_URL + "/courses/" + course.getId())
                            .header("Authorization", "Bearer " + studentToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
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
        e.setUser(student);
        e.setCourse(course);
        return enrollmentRepository.save(e);
    }
}
