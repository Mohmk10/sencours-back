package com.sencours.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sencours.dto.request.ApplicationReviewRequest;
import com.sencours.dto.request.InstructorApplicationCreateRequest;
import com.sencours.entity.InstructorApplication;
import com.sencours.entity.User;
import com.sencours.enums.ApplicationStatus;
import com.sencours.enums.Role;
import com.sencours.repository.CourseRepository;
import com.sencours.repository.EnrollmentRepository;
import com.sencours.repository.InstructorApplicationRepository;
import com.sencours.repository.ProgressRepository;
import com.sencours.repository.ReviewRepository;
import com.sencours.repository.SectionRepository;
import com.sencours.repository.UserRepository;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class InstructorApplicationControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private InstructorApplicationRepository applicationRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private ProgressRepository progressRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private SectionRepository sectionRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ObjectMapper objectMapper;

    private User etudiant;
    private User admin;
    private String etudiantToken;
    private String adminToken;

    @BeforeEach
    void setUp() {
        progressRepository.deleteAll();
        enrollmentRepository.deleteAll();
        reviewRepository.deleteAll();
        applicationRepository.deleteAll();
        sectionRepository.deleteAll();
        courseRepository.deleteAll();
        userRepository.deleteAll();

        etudiant = createUser("Moussa", "Diallo", "moussa@test.sn", Role.ETUDIANT);
        admin = createUser("Admin", "SenCours", "admin@sencours.sn", Role.ADMIN);
        etudiantToken = jwtService.generateToken(etudiant);
        adminToken = jwtService.generateToken(admin);
    }

    private User createUser(String firstName, String lastName, String email, Role role) {
        User user = User.builder()
                .firstName(firstName)
                .lastName(lastName)
                .email(email)
                .password(passwordEncoder.encode("password123"))
                .role(role)
                .isActive(true)
                .build();
        return userRepository.save(user);
    }

    @Nested
    @DisplayName("POST /api/v1/instructor-applications")
    class CreateApplicationTests {

        @Test
        @DisplayName("Devrait créer une candidature pour un étudiant authentifié")
        void shouldCreateApplication() throws Exception {
            InstructorApplicationCreateRequest request = new InstructorApplicationCreateRequest(
                    "Je veux partager mes connaissances en développement web avec la communauté sénégalaise",
                    "Java, Spring Boot, Angular",
                    "https://linkedin.com/in/moussa",
                    null
            );

            mockMvc.perform(post("/api/v1/instructor-applications")
                            .header("Authorization", "Bearer " + etudiantToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.status").value("PENDING"))
                    .andExpect(jsonPath("$.userEmail").value("moussa@test.sn"));
        }

        @Test
        @DisplayName("Devrait retourner 401 sans authentification")
        void shouldReturn401WithoutAuth() throws Exception {
            InstructorApplicationCreateRequest request = new InstructorApplicationCreateRequest(
                    "Je veux partager mes connaissances en développement web avec la communauté",
                    null, null, null
            );

            mockMvc.perform(post("/api/v1/instructor-applications")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Devrait retourner 400 si motivation trop courte")
        void shouldReturn400IfMotivationTooShort() throws Exception {
            InstructorApplicationCreateRequest request = new InstructorApplicationCreateRequest(
                    "Trop court",
                    null, null, null
            );

            mockMvc.perform(post("/api/v1/instructor-applications")
                            .header("Authorization", "Bearer " + etudiantToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/admin/instructor-applications/{id}/review")
    class ReviewApplicationTests {

        @Test
        @DisplayName("Devrait approuver une candidature et promouvoir l'étudiant")
        void shouldApproveApplication() throws Exception {
            InstructorApplication application = applicationRepository.save(
                    InstructorApplication.builder()
                            .user(etudiant)
                            .motivation("Je veux partager mes connaissances en développement web avec la communauté")
                            .status(ApplicationStatus.PENDING)
                            .build()
            );

            ApplicationReviewRequest request = new ApplicationReviewRequest(true, "Très bon profil");

            mockMvc.perform(put("/api/v1/admin/instructor-applications/" + application.getId() + "/review")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("APPROVED"))
                    .andExpect(jsonPath("$.adminComment").value("Très bon profil"));
        }

        @Test
        @DisplayName("Devrait retourner 403 pour un étudiant essayant de valider")
        void shouldReturn403ForStudent() throws Exception {
            ApplicationReviewRequest request = new ApplicationReviewRequest(true, null);

            mockMvc.perform(put("/api/v1/admin/instructor-applications/1/review")
                            .header("Authorization", "Bearer " + etudiantToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }
    }
}
