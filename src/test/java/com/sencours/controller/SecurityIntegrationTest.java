package com.sencours.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sencours.dto.request.CourseRequest;
import com.sencours.dto.request.EnrollmentRequest;
import com.sencours.dto.request.ReviewRequest;
import com.sencours.entity.Category;
import com.sencours.entity.Course;
import com.sencours.entity.Enrollment;
import com.sencours.entity.User;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private ProgressRepository progressRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private User admin;
    private User instructor;
    private User instructor2;
    private User student;
    private Category category;
    private Course course;
    private String adminToken;
    private String instructorToken;
    private String instructor2Token;
    private String studentToken;

    @BeforeEach
    void setUp() {
        reviewRepository.deleteAll();
        progressRepository.deleteAll();
        enrollmentRepository.deleteAll();
        courseRepository.deleteAll();
        userRepository.deleteAll();
        categoryRepository.deleteAll();

        admin = createUser("admin@sencours.sn", Role.ADMIN);
        instructor = createUser("instructor@sencours.sn", Role.INSTRUCTEUR);
        instructor2 = createUser("instructor2@sencours.sn", Role.INSTRUCTEUR);
        student = createUser("student@sencours.sn", Role.ETUDIANT);

        category = createCategory();
        course = createCourse(instructor, category);

        adminToken = jwtService.generateToken(admin);
        instructorToken = jwtService.generateToken(instructor);
        instructor2Token = jwtService.generateToken(instructor2);
        studentToken = jwtService.generateToken(student);
    }

    @Nested
    @DisplayName("Tests d'accès aux endpoints publics")
    class PublicEndpointsTests {

        @Test
        @DisplayName("GET /api/v1/courses - Accessible sans authentification")
        void shouldAccessCoursesWithoutAuth() throws Exception {
            mockMvc.perform(get("/api/v1/courses"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("GET /api/v1/courses/{id} - Accessible sans authentification")
        void shouldAccessCourseByIdWithoutAuth() throws Exception {
            mockMvc.perform(get("/api/v1/courses/" + course.getId()))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("GET /api/v1/categories - Accessible sans authentification")
        void shouldAccessCategoriesWithoutAuth() throws Exception {
            mockMvc.perform(get("/api/v1/categories"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("GET /api/v1/reviews/courses/{courseId} - Accessible sans authentification")
        void shouldAccessReviewsWithoutAuth() throws Exception {
            mockMvc.perform(get("/api/v1/reviews/courses/" + course.getId()))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("Tests de création de cours par rôle")
    class CreateCourseByRoleTests {

        @Test
        @DisplayName("ETUDIANT ne peut pas créer de cours - 403")
        void studentCannotCreateCourse() throws Exception {
            CourseRequest request = CourseRequest.builder()
                    .title("Nouveau cours")
                    .description("Description")
                    .price(new BigDecimal("10000"))
                    .instructorId(student.getId())
                    .categoryId(category.getId())
                    .build();

            mockMvc.perform(post("/api/v1/courses")
                            .header("Authorization", "Bearer " + studentToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("INSTRUCTEUR peut créer un cours - 201")
        void instructorCanCreateCourse() throws Exception {
            CourseRequest request = CourseRequest.builder()
                    .title("Nouveau cours")
                    .description("Description")
                    .price(new BigDecimal("10000"))
                    .instructorId(instructor.getId())
                    .categoryId(category.getId())
                    .build();

            mockMvc.perform(post("/api/v1/courses")
                            .header("Authorization", "Bearer " + instructorToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("ADMIN peut créer un cours - 201")
        void adminCanCreateCourse() throws Exception {
            CourseRequest request = CourseRequest.builder()
                    .title("Cours admin")
                    .description("Description")
                    .price(new BigDecimal("10000"))
                    .instructorId(instructor.getId())
                    .categoryId(category.getId())
                    .build();

            mockMvc.perform(post("/api/v1/courses")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());
        }
    }

    @Nested
    @DisplayName("Tests de modification de cours par propriétaire")
    class UpdateCourseOwnershipTests {

        @Test
        @DisplayName("INSTRUCTEUR peut modifier son propre cours - 200")
        void instructorCanUpdateOwnCourse() throws Exception {
            CourseRequest request = CourseRequest.builder()
                    .title("Cours modifié")
                    .description("Nouvelle description")
                    .price(new BigDecimal("15000"))
                    .instructorId(instructor.getId())
                    .categoryId(category.getId())
                    .build();

            mockMvc.perform(put("/api/v1/courses/" + course.getId())
                            .header("Authorization", "Bearer " + instructorToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("ADMIN peut modifier n'importe quel cours - 200")
        void adminCanUpdateAnyCourse() throws Exception {
            CourseRequest request = CourseRequest.builder()
                    .title("Cours modifié par admin")
                    .description("Nouvelle description")
                    .price(new BigDecimal("15000"))
                    .instructorId(instructor.getId())
                    .categoryId(category.getId())
                    .build();

            mockMvc.perform(put("/api/v1/courses/" + course.getId())
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("Tests d'inscription aux cours")
    class EnrollmentTests {

        @Test
        @DisplayName("ETUDIANT peut initier un paiement - 200")
        void studentCanInitiatePayment() throws Exception {
            EnrollmentRequest request = EnrollmentRequest.builder()
                    .paymentMethod("ORANGE_MONEY")
                    .build();

            mockMvc.perform(post("/api/v1/enrollments/courses/" + course.getId() + "/pay")
                            .header("Authorization", "Bearer " + studentToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Non authentifié ne peut pas s'inscrire - 401")
        void unauthenticatedCannotEnroll() throws Exception {
            EnrollmentRequest request = EnrollmentRequest.builder()
                    .paymentMethod("WAVE")
                    .build();

            mockMvc.perform(post("/api/v1/enrollments/courses/" + course.getId() + "/pay")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("Tests de création d'avis")
    class ReviewTests {

        @Test
        @DisplayName("ETUDIANT inscrit peut créer un avis - 201")
        void enrolledStudentCanCreateReview() throws Exception {
            createEnrollment(student, course);

            ReviewRequest request = ReviewRequest.builder()
                    .rating(5)
                    .comment("Excellent cours!")
                    .build();

            mockMvc.perform(post("/api/v1/reviews/courses/" + course.getId())
                            .header("Authorization", "Bearer " + studentToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("ETUDIANT non inscrit ne peut pas créer un avis - 400")
        void nonEnrolledStudentCannotCreateReview() throws Exception {
            ReviewRequest request = ReviewRequest.builder()
                    .rating(5)
                    .comment("Excellent cours!")
                    .build();

            mockMvc.perform(post("/api/v1/reviews/courses/" + course.getId())
                            .header("Authorization", "Bearer " + studentToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Non authentifié ne peut pas créer un avis - 401")
        void unauthenticatedCannotCreateReview() throws Exception {
            ReviewRequest request = ReviewRequest.builder()
                    .rating(5)
                    .comment("Excellent cours!")
                    .build();

            mockMvc.perform(post("/api/v1/reviews/courses/" + course.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("Tests de gestion des catégories")
    class CategoryManagementTests {

        @Test
        @DisplayName("ADMIN peut créer une catégorie - 201")
        void adminCanCreateCategory() throws Exception {
            String requestBody = "{\"name\":\"Nouvelle catégorie\",\"description\":\"Description\"}";

            mockMvc.perform(post("/api/v1/categories")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("INSTRUCTEUR ne peut pas créer une catégorie - 403")
        void instructorCannotCreateCategory() throws Exception {
            String requestBody = "{\"name\":\"Nouvelle catégorie\",\"description\":\"Description\"}";

            mockMvc.perform(post("/api/v1/categories")
                            .header("Authorization", "Bearer " + instructorToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("ETUDIANT ne peut pas créer une catégorie - 403")
        void studentCannotCreateCategory() throws Exception {
            String requestBody = "{\"name\":\"Nouvelle catégorie\",\"description\":\"Description\"}";

            mockMvc.perform(post("/api/v1/categories")
                            .header("Authorization", "Bearer " + studentToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isForbidden());
        }
    }

    private User createUser(String email, Role role) {
        User user = User.builder()
                .firstName("Test")
                .lastName("User")
                .email(email)
                .password(passwordEncoder.encode("password123"))
                .role(role)
                .build();
        return userRepository.save(user);
    }

    private Category createCategory() {
        Category cat = new Category();
        cat.setName("Test Category");
        cat.setDescription("Test Description");
        return categoryRepository.save(cat);
    }

    private Course createCourse(User instructor, Category category) {
        Course c = new Course();
        c.setTitle("Test Course");
        c.setDescription("Test Description");
        c.setPrice(new BigDecimal("10000"));
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
}
