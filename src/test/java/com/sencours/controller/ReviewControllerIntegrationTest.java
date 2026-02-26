package com.sencours.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sencours.dto.request.ReviewRequest;
import com.sencours.entity.*;
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
class ReviewControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ReviewRepository reviewRepository;

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

    private User instructor;
    private User student;
    private Course course;
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
        course = createCourse(instructor, category);

        studentToken = jwtService.generateToken(student);
        instructorToken = jwtService.generateToken(instructor);
    }

    @Nested
    @DisplayName("POST /api/v1/reviews/courses/{courseId}")
    class CreateOrUpdateReviewTests {

        @Test
        @DisplayName("Devrait créer un avis avec succès - 201")
        void shouldCreateReviewSuccessfully() throws Exception {
            createEnrollment(student, course);

            ReviewRequest request = ReviewRequest.builder()
                    .rating(5)
                    .comment("Excellent cours, très bien expliqué!")
                    .build();

            mockMvc.perform(post("/api/v1/reviews/courses/" + course.getId())
                            .header("Authorization", "Bearer " + studentToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").isNotEmpty())
                    .andExpect(jsonPath("$.userId").value(student.getId()))
                    .andExpect(jsonPath("$.courseId").value(course.getId()))
                    .andExpect(jsonPath("$.rating").value(5))
                    .andExpect(jsonPath("$.userName").value("Mamadou Diallo"))
                    .andExpect(jsonPath("$.userInitials").value("MD"));
        }

        @Test
        @DisplayName("Devrait mettre à jour un avis existant - 201")
        void shouldUpdateExistingReview() throws Exception {
            createEnrollment(student, course);
            createReview(student, course, 3);

            ReviewRequest request = ReviewRequest.builder()
                    .rating(5)
                    .comment("Après relecture, excellent!")
                    .build();

            mockMvc.perform(post("/api/v1/reviews/courses/" + course.getId())
                            .header("Authorization", "Bearer " + studentToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.rating").value(5));
        }

        @Test
        @DisplayName("Devrait retourner 400 si rating = 0")
        void shouldReturn400WhenRatingZero() throws Exception {
            createEnrollment(student, course);

            ReviewRequest request = ReviewRequest.builder()
                    .rating(0)
                    .build();

            mockMvc.perform(post("/api/v1/reviews/courses/" + course.getId())
                            .header("Authorization", "Bearer " + studentToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Devrait retourner 400 si non inscrit")
        void shouldReturn400WhenNotEnrolled() throws Exception {
            ReviewRequest request = ReviewRequest.builder()
                    .rating(5)
                    .build();

            mockMvc.perform(post("/api/v1/reviews/courses/" + course.getId())
                            .header("Authorization", "Bearer " + studentToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(containsString("inscrit")));
        }

        @Test
        @DisplayName("Devrait retourner 400 si instructeur note son propre cours")
        void shouldReturn400WhenInstructorReviewsOwnCourse() throws Exception {
            createEnrollment(instructor, course);

            ReviewRequest request = ReviewRequest.builder()
                    .rating(5)
                    .build();

            mockMvc.perform(post("/api/v1/reviews/courses/" + course.getId())
                            .header("Authorization", "Bearer " + instructorToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(containsString("propre cours")));
        }

        @Test
        @DisplayName("Devrait retourner 401 sans authentification")
        void shouldReturn401WithoutAuth() throws Exception {
            ReviewRequest request = ReviewRequest.builder()
                    .rating(5)
                    .build();

            mockMvc.perform(post("/api/v1/reviews/courses/" + course.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/reviews/courses/{courseId}")
    class GetCourseReviewsTests {

        @Test
        @DisplayName("Devrait retourner la liste des avis - 200 (public)")
        void shouldReturnReviewsList() throws Exception {
            createEnrollment(student, course);
            createReview(student, course, 5);

            User student2 = createStudent("student2@test.sn");
            createEnrollment(student2, course);
            createReview(student2, course, 4);

            mockMvc.perform(get("/api/v1/reviews/courses/" + course.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)));
        }

        @Test
        @DisplayName("Devrait retourner liste vide si aucun avis - 200")
        void shouldReturnEmptyListWhenNoReviews() throws Exception {
            mockMvc.perform(get("/api/v1/reviews/courses/" + course.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/reviews/courses/{courseId}/average")
    class GetAverageRatingTests {

        @Test
        @DisplayName("Devrait calculer la moyenne correctement - 200 (public)")
        void shouldCalculateAverageCorrectly() throws Exception {
            createEnrollment(student, course);
            createReview(student, course, 5);

            User student2 = createStudent("student2@test.sn");
            createEnrollment(student2, course);
            createReview(student2, course, 3);

            mockMvc.perform(get("/api/v1/reviews/courses/" + course.getId() + "/average"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.averageRating").value(4.0));
        }

        @Test
        @DisplayName("Devrait retourner 0.0 si aucun avis - 200")
        void shouldReturnZeroWhenNoReviews() throws Exception {
            mockMvc.perform(get("/api/v1/reviews/courses/" + course.getId() + "/average"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.averageRating").value(0.0));
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/reviews/{reviewId}")
    class DeleteReviewTests {

        @Test
        @DisplayName("Devrait supprimer son avis - 204")
        void shouldDeleteReview() throws Exception {
            createEnrollment(student, course);
            Review review = createReview(student, course, 5);

            mockMvc.perform(delete("/api/v1/reviews/" + review.getId())
                            .header("Authorization", "Bearer " + studentToken))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("Devrait retourner 403 si pas l'auteur")
        void shouldReturn403WhenNotAuthor() throws Exception {
            createEnrollment(student, course);
            Review review = createReview(student, course, 5);

            User otherStudent = createStudent("other@test.sn");
            String otherToken = jwtService.generateToken(otherStudent);

            mockMvc.perform(delete("/api/v1/reviews/" + review.getId())
                            .header("Authorization", "Bearer " + otherToken))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message").value(containsString("propres avis")));
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

    private Enrollment createEnrollment(User student, Course course) {
        Enrollment e = new Enrollment();
        e.setUser(student);
        e.setCourse(course);
        return enrollmentRepository.save(e);
    }

    private Review createReview(User student, Course course, int rating) {
        Review r = new Review();
        r.setUser(student);
        r.setCourse(course);
        r.setRating(rating);
        r.setComment("Mon avis sur ce cours");
        return reviewRepository.save(r);
    }
}
