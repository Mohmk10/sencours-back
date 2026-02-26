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
import java.util.Map;

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
    private ReviewRepository reviewRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private static final String BASE_URL = "/api/v1/enrollments";

    private User student;
    private User instructor;
    private Course course;
    private Course freeCourse;
    private String studentToken;
    private String instructorToken;

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

        instructor = createInstructor();
        student = createStudent();
        Category category = createCategory();
        course = createCourse(instructor, category, new BigDecimal("25000"));
        freeCourse = createCourse(instructor, category, BigDecimal.ZERO);
        freeCourse.setTitle("Cours gratuit");
        courseRepository.save(freeCourse);

        createSection(course);
        createSection(freeCourse);

        studentToken = jwtService.generateToken(student);
        instructorToken = jwtService.generateToken(instructor);
    }

    @Nested
    @DisplayName("POST /api/v1/enrollments/courses/{courseId}/pay")
    class InitiatePaymentTests {

        @Test
        @DisplayName("Devrait initier un paiement avec succès - 200")
        void shouldInitiatePaymentSuccessfully() throws Exception {
            EnrollmentRequest request = EnrollmentRequest.builder()
                    .paymentMethod("ORANGE_MONEY")
                    .build();

            mockMvc.perform(post(BASE_URL + "/courses/" + course.getId() + "/pay")
                            .header("Authorization", "Bearer " + studentToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.reference").isNotEmpty())
                    .andExpect(jsonPath("$.status").value("SUCCESS"))
                    .andExpect(jsonPath("$.amount").value(25000));
        }

        @Test
        @DisplayName("Devrait retourner 400 si déjà inscrit")
        void shouldReturn400WhenAlreadyEnrolled() throws Exception {
            createEnrollment(student, course);

            EnrollmentRequest request = EnrollmentRequest.builder()
                    .paymentMethod("WAVE")
                    .build();

            mockMvc.perform(post(BASE_URL + "/courses/" + course.getId() + "/pay")
                            .header("Authorization", "Bearer " + studentToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(containsString("déjà inscrit")));
        }

        @Test
        @DisplayName("Devrait retourner 401 sans authentification")
        void shouldReturn401WithoutAuth() throws Exception {
            EnrollmentRequest request = EnrollmentRequest.builder()
                    .paymentMethod("ORANGE_MONEY")
                    .build();

            mockMvc.perform(post(BASE_URL + "/courses/" + course.getId() + "/pay")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/enrollments/courses/{courseId}/free")
    class EnrollFreeTests {

        @Test
        @DisplayName("Devrait inscrire gratuitement avec succès - 201")
        void shouldEnrollFreeSuccessfully() throws Exception {
            mockMvc.perform(post(BASE_URL + "/courses/" + freeCourse.getId() + "/free")
                            .header("Authorization", "Bearer " + studentToken)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").isNotEmpty())
                    .andExpect(jsonPath("$.courseId").value(freeCourse.getId()))
                    .andExpect(jsonPath("$.userId").value(student.getId()));
        }

        @Test
        @DisplayName("Devrait retourner 400 si cours pas gratuit")
        void shouldReturn400WhenCourseNotFree() throws Exception {
            mockMvc.perform(post(BASE_URL + "/courses/" + course.getId() + "/free")
                            .header("Authorization", "Bearer " + studentToken)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(containsString("pas gratuit")));
        }

        @Test
        @DisplayName("Devrait retourner 400 si déjà inscrit")
        void shouldReturn400WhenAlreadyEnrolled() throws Exception {
            createEnrollment(student, freeCourse);

            mockMvc.perform(post(BASE_URL + "/courses/" + freeCourse.getId() + "/free")
                            .header("Authorization", "Bearer " + studentToken)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(containsString("déjà inscrit")));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/enrollments/courses/{courseId}/check")
    class CheckEnrollmentTests {

        @Test
        @DisplayName("Devrait retourner true si inscrit")
        void shouldReturnTrueWhenEnrolled() throws Exception {
            createEnrollment(student, course);

            mockMvc.perform(get(BASE_URL + "/courses/" + course.getId() + "/check")
                            .header("Authorization", "Bearer " + studentToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.enrolled").value(true));
        }

        @Test
        @DisplayName("Devrait retourner false si pas inscrit")
        void shouldReturnFalseWhenNotEnrolled() throws Exception {
            mockMvc.perform(get(BASE_URL + "/courses/" + course.getId() + "/check")
                            .header("Authorization", "Bearer " + studentToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.enrolled").value(false));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/enrollments/my-enrollments")
    class GetMyEnrollmentsTests {

        @Test
        @DisplayName("Devrait retourner les inscriptions de l'utilisateur - 200")
        void shouldReturnUserEnrollments() throws Exception {
            createEnrollment(student, course);

            mockMvc.perform(get(BASE_URL + "/my-enrollments")
                            .header("Authorization", "Bearer " + studentToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].userId").value(student.getId()));
        }

        @Test
        @DisplayName("Devrait retourner liste vide si aucune inscription - 200")
        void shouldReturnEmptyListWhenNoEnrollments() throws Exception {
            mockMvc.perform(get(BASE_URL + "/my-enrollments")
                            .header("Authorization", "Bearer " + studentToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }

        @Test
        @DisplayName("Devrait retourner 401 sans authentification")
        void shouldReturn401WithoutAuth() throws Exception {
            mockMvc.perform(get(BASE_URL + "/my-enrollments"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/enrollments/courses/{courseId}")
    class GetEnrollmentTests {

        @Test
        @DisplayName("Devrait retourner le détail de l'inscription - 200")
        void shouldReturnEnrollmentDetail() throws Exception {
            createEnrollment(student, course);

            mockMvc.perform(get(BASE_URL + "/courses/" + course.getId())
                            .header("Authorization", "Bearer " + studentToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.courseId").value(course.getId()))
                    .andExpect(jsonPath("$.userId").value(student.getId()));
        }

        @Test
        @DisplayName("Devrait retourner 404 si inscription non trouvée")
        void shouldReturn404WhenEnrollmentNotFound() throws Exception {
            mockMvc.perform(get(BASE_URL + "/courses/" + course.getId())
                            .header("Authorization", "Bearer " + studentToken))
                    .andExpect(status().isNotFound());
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

    private Course createCourse(User instructor, Category category, BigDecimal price) {
        Course c = new Course();
        c.setTitle("Java pour débutants");
        c.setDescription("Apprenez Java");
        c.setPrice(price);
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

    private Enrollment createEnrollment(User student, Course course) {
        Enrollment enrollment = new Enrollment();
        enrollment.setUser(student);
        enrollment.setCourse(course);
        return enrollmentRepository.save(enrollment);
    }
}
