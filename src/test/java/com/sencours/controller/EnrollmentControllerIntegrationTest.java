package com.sencours.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sencours.dto.request.EnrollmentRequest;
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
class EnrollmentControllerIntegrationTest {

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

    private static final String BASE_URL = "/api/v1/enrollments";

    private User student;
    private User instructor;
    private Course course;
    private Section section;
    private Lesson lesson1;
    private Lesson lesson2;
    private String studentToken;
    private String instructorToken;

    @BeforeEach
    void setUp() {
        progressRepository.deleteAll();
        enrollmentRepository.deleteAll();
        lessonRepository.deleteAll();
        sectionRepository.deleteAll();
        courseRepository.deleteAll();
        userRepository.deleteAll();
        categoryRepository.deleteAll();

        instructor = createInstructor();
        student = createStudent();
        Category category = createCategory();
        course = createCourse(instructor, category);
        section = createSection(course);
        lesson1 = createLesson(section, "Introduction", 1);
        lesson2 = createLesson(section, "Chapitre 1", 2);

        studentToken = jwtService.generateToken(student);
        instructorToken = jwtService.generateToken(instructor);
    }

    @Nested
    @DisplayName("POST /api/v1/enrollments")
    class EnrollTests {

        @Test
        @DisplayName("Devrait inscrire un étudiant avec succès - 201")
        void shouldEnrollStudentSuccessfully() throws Exception {
            EnrollmentRequest request = EnrollmentRequest.builder()
                    .courseId(course.getId())
                    .build();

            mockMvc.perform(post(BASE_URL)
                            .header("Authorization", "Bearer " + studentToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").isNotEmpty())
                    .andExpect(jsonPath("$.userId").value(student.getId()))
                    .andExpect(jsonPath("$.courseId").value(course.getId()))
                    .andExpect(jsonPath("$.courseTitle").value(course.getTitle()))
                    .andExpect(jsonPath("$.progressPercentage").value(0.0));
        }

        @Test
        @DisplayName("Devrait créer des Progress pour chaque leçon - 201")
        void shouldCreateProgressForEachLesson() throws Exception {
            EnrollmentRequest request = EnrollmentRequest.builder()
                    .courseId(course.getId())
                    .build();

            String response = mockMvc.perform(post(BASE_URL)
                            .header("Authorization", "Bearer " + studentToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andReturn().getResponse().getContentAsString();

            Long enrollmentId = objectMapper.readTree(response).get("id").asLong();

            mockMvc.perform(get(BASE_URL + "/" + enrollmentId + "/lessons")
                            .header("Authorization", "Bearer " + studentToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)));
        }

        @Test
        @DisplayName("Devrait retourner 409 si déjà inscrit")
        void shouldReturn409WhenAlreadyEnrolled() throws Exception {
            createEnrollment(student, course);

            EnrollmentRequest request = EnrollmentRequest.builder()
                    .courseId(course.getId())
                    .build();

            mockMvc.perform(post(BASE_URL)
                            .header("Authorization", "Bearer " + studentToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").value(containsString("déjà inscrit")));
        }

        @Test
        @DisplayName("Devrait retourner 404 si cours non trouvé")
        void shouldReturn404WhenCourseNotFound() throws Exception {
            EnrollmentRequest request = EnrollmentRequest.builder()
                    .courseId(999L)
                    .build();

            mockMvc.perform(post(BASE_URL)
                            .header("Authorization", "Bearer " + studentToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value(containsString("Cours")));
        }

        @Test
        @DisplayName("Devrait retourner 401 sans authentification")
        void shouldReturn401WithoutAuth() throws Exception {
            EnrollmentRequest request = EnrollmentRequest.builder()
                    .courseId(course.getId())
                    .build();

            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Devrait retourner erreur si INSTRUCTEUR tente de s'inscrire")
        void shouldReturnErrorWhenInstructorTriesToEnroll() throws Exception {
            EnrollmentRequest request = EnrollmentRequest.builder()
                    .courseId(course.getId())
                    .build();

            mockMvc.perform(post(BASE_URL)
                            .header("Authorization", "Bearer " + instructorToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isInternalServerError());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/enrollments/me")
    class GetMyEnrollmentsTests {

        @Test
        @DisplayName("Devrait retourner les inscriptions de l'utilisateur connecté - 200")
        void shouldReturnUserEnrollments() throws Exception {
            createEnrollment(student, course);

            mockMvc.perform(get(BASE_URL + "/me")
                            .header("Authorization", "Bearer " + studentToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].userId").value(student.getId()));
        }

        @Test
        @DisplayName("Devrait retourner liste vide si aucune inscription - 200")
        void shouldReturnEmptyListWhenNoEnrollments() throws Exception {
            mockMvc.perform(get(BASE_URL + "/me")
                            .header("Authorization", "Bearer " + studentToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }

        @Test
        @DisplayName("Devrait retourner 401 sans authentification")
        void shouldReturn401WithoutAuth() throws Exception {
            mockMvc.perform(get(BASE_URL + "/me"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/enrollments/{enrollmentId}")
    class GetEnrollmentDetailTests {

        @Test
        @DisplayName("Devrait retourner le détail avec progression - 200")
        void shouldReturnDetailWithProgress() throws Exception {
            Enrollment enrollment = createEnrollment(student, course);

            mockMvc.perform(get(BASE_URL + "/" + enrollment.getId())
                            .header("Authorization", "Bearer " + studentToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(enrollment.getId()))
                    .andExpect(jsonPath("$.progresses", hasSize(2)))
                    .andExpect(jsonPath("$.progressPercentage").value(0.0));
        }

        @Test
        @DisplayName("Devrait retourner 404 si inscription non trouvée")
        void shouldReturn404WhenEnrollmentNotFound() throws Exception {
            mockMvc.perform(get(BASE_URL + "/999")
                            .header("Authorization", "Bearer " + studentToken))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/enrollments/{enrollmentId}/progress")
    class GetProgressSummaryTests {

        @Test
        @DisplayName("Devrait retourner 0% si aucune leçon complétée")
        void shouldReturnZeroPercent() throws Exception {
            Enrollment enrollment = createEnrollment(student, course);

            mockMvc.perform(get(BASE_URL + "/" + enrollment.getId() + "/progress")
                            .header("Authorization", "Bearer " + studentToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalLessons").value(2))
                    .andExpect(jsonPath("$.completedLessons").value(0))
                    .andExpect(jsonPath("$.percentage").value(0.0));
        }

        @Test
        @DisplayName("Devrait retourner 50% si 1/2 leçons complétées")
        void shouldReturn50Percent() throws Exception {
            Enrollment enrollment = createEnrollment(student, course);

            Progress progress = progressRepository.findByEnrollmentIdAndLessonId(enrollment.getId(), lesson1.getId())
                    .orElseThrow();
            progress.setCompleted(true);
            progressRepository.save(progress);

            mockMvc.perform(get(BASE_URL + "/" + enrollment.getId() + "/progress")
                            .header("Authorization", "Bearer " + studentToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalLessons").value(2))
                    .andExpect(jsonPath("$.completedLessons").value(1))
                    .andExpect(jsonPath("$.percentage").value(50.0));
        }

        @Test
        @DisplayName("Devrait retourner 100% si toutes les leçons complétées")
        void shouldReturn100Percent() throws Exception {
            Enrollment enrollment = createEnrollment(student, course);

            progressRepository.findByEnrollmentId(enrollment.getId()).forEach(p -> {
                p.setCompleted(true);
                progressRepository.save(p);
            });

            mockMvc.perform(get(BASE_URL + "/" + enrollment.getId() + "/progress")
                            .header("Authorization", "Bearer " + studentToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalLessons").value(2))
                    .andExpect(jsonPath("$.completedLessons").value(2))
                    .andExpect(jsonPath("$.percentage").value(100.0));
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/enrollments/{enrollmentId}")
    class UnenrollTests {

        @Test
        @DisplayName("Devrait se désinscrire avec succès - 204")
        void shouldUnenrollSuccessfully() throws Exception {
            Enrollment enrollment = createEnrollment(student, course);

            mockMvc.perform(delete(BASE_URL + "/" + enrollment.getId())
                            .header("Authorization", "Bearer " + studentToken))
                    .andExpect(status().isNoContent());

            mockMvc.perform(get(BASE_URL + "/" + enrollment.getId())
                            .header("Authorization", "Bearer " + studentToken))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Devrait retourner 403 si pas le propriétaire")
        void shouldReturn403WhenNotOwner() throws Exception {
            Enrollment enrollment = createEnrollment(student, course);

            User otherStudent = createStudent("other@sencours.sn");
            String otherToken = jwtService.generateToken(otherStudent);

            mockMvc.perform(delete(BASE_URL + "/" + enrollment.getId())
                            .header("Authorization", "Bearer " + otherToken))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Devrait retourner 401 sans authentification")
        void shouldReturn401WithoutAuth() throws Exception {
            Enrollment enrollment = createEnrollment(student, course);

            mockMvc.perform(delete(BASE_URL + "/" + enrollment.getId()))
                    .andExpect(status().isUnauthorized());
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
        return createStudent("mamadou@sencours.sn");
    }

    private User createStudent(String email) {
        User user = User.builder()
                .firstName("Mamadou")
                .lastName("Diallo")
                .email(email)
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
        Enrollment enrollment = new Enrollment();
        enrollment.setStudent(student);
        enrollment.setCourse(course);
        enrollment = enrollmentRepository.save(enrollment);

        for (Lesson lesson : lessonRepository.findByCourseIdOrderByOrderIndex(course.getId())) {
            Progress progress = new Progress();
            progress.setEnrollment(enrollment);
            progress.setLesson(lesson);
            progress.setCompleted(false);
            progressRepository.save(progress);
        }

        return enrollment;
    }
}
