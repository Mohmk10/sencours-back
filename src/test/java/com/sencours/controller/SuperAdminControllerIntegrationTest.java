package com.sencours.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sencours.dto.request.CreateAdminRequest;
import com.sencours.entity.User;
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
class SuperAdminControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

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
    private InstructorApplicationRepository applicationRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ObjectMapper objectMapper;

    private User superAdmin;
    private User admin;
    private String superAdminToken;
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

        superAdmin = createUser("Super", "Admin", "superadmin@sencours.sn", Role.SUPER_ADMIN);
        admin = createUser("Admin", "SenCours", "admin@sencours.sn", Role.ADMIN);
        superAdminToken = jwtService.generateToken(superAdmin);
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

    @Test
    @DisplayName("SUPER_ADMIN peut créer un admin")
    void superAdminCanCreateAdmin() throws Exception {
        CreateAdminRequest request = new CreateAdminRequest(
                "Nouveau", "Admin", "nouvel.admin@sencours.sn", "password123"
        );

        mockMvc.perform(post("/api/v1/super-admin/admins")
                        .header("Authorization", "Bearer " + superAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(jsonPath("$.email").value("nouvel.admin@sencours.sn"));
    }

    @Test
    @DisplayName("Un ADMIN classique ne peut pas accéder aux endpoints SUPER_ADMIN")
    void regularAdminCannotAccessSuperAdminEndpoints() throws Exception {
        CreateAdminRequest request = new CreateAdminRequest(
                "Nouveau", "Admin", "nouvel.admin@sencours.sn", "password123"
        );

        mockMvc.perform(post("/api/v1/super-admin/admins")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}
