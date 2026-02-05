package com.sencours.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sencours.dto.request.LoginRequest;
import com.sencours.dto.request.RegisterRequest;
import com.sencours.entity.User;
import com.sencours.enums.Role;
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

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private User existingUser;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        existingUser = User.builder()
                .firstName("Existing")
                .lastName("User")
                .email("existing@sencours.sn")
                .password(passwordEncoder.encode("password123"))
                .role(Role.ETUDIANT)
                .build();
        existingUser = userRepository.save(existingUser);
    }

    @Nested
    @DisplayName("POST /api/v1/auth/register")
    class RegisterTests {

        @Test
        @DisplayName("Devrait enregistrer un nouvel utilisateur - 201")
        void shouldRegisterNewUser() throws Exception {
            RegisterRequest request = RegisterRequest.builder()
                    .firstName("Mohamed")
                    .lastName("Diallo")
                    .email("mohamed@sencours.sn")
                    .password("password123")
                    .build();

            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.token").isNotEmpty())
                    .andExpect(jsonPath("$.type").value("Bearer"))
                    .andExpect(jsonPath("$.userId").isNotEmpty())
                    .andExpect(jsonPath("$.email").value("mohamed@sencours.sn"))
                    .andExpect(jsonPath("$.fullName").value("Mohamed Diallo"))
                    .andExpect(jsonPath("$.role").value("ETUDIANT"));
        }

        @Test
        @DisplayName("Devrait retourner 409 si email déjà utilisé")
        void shouldReturn409WhenEmailExists() throws Exception {
            RegisterRequest request = RegisterRequest.builder()
                    .firstName("Another")
                    .lastName("User")
                    .email("existing@sencours.sn")
                    .password("password123")
                    .build();

            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").value(containsString("existing@sencours.sn")));
        }

        @Test
        @DisplayName("Devrait retourner 400 si mot de passe trop court")
        void shouldReturn400WhenPasswordTooShort() throws Exception {
            RegisterRequest request = RegisterRequest.builder()
                    .firstName("Mohamed")
                    .lastName("Diallo")
                    .email("mohamed@sencours.sn")
                    .password("short")
                    .build();

            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.validationErrors.password").exists());
        }

        @Test
        @DisplayName("Devrait retourner 400 si email invalide")
        void shouldReturn400WhenEmailInvalid() throws Exception {
            RegisterRequest request = RegisterRequest.builder()
                    .firstName("Mohamed")
                    .lastName("Diallo")
                    .email("invalid-email")
                    .password("password123")
                    .build();

            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.validationErrors.email").exists());
        }

        @Test
        @DisplayName("Devrait retourner 400 si prénom manquant")
        void shouldReturn400WhenFirstNameMissing() throws Exception {
            RegisterRequest request = RegisterRequest.builder()
                    .lastName("Diallo")
                    .email("mohamed@sencours.sn")
                    .password("password123")
                    .build();

            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.validationErrors.firstName").exists());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/login")
    class LoginTests {

        @Test
        @DisplayName("Devrait connecter un utilisateur avec succès - 200")
        void shouldLoginSuccessfully() throws Exception {
            LoginRequest request = LoginRequest.builder()
                    .email("existing@sencours.sn")
                    .password("password123")
                    .build();

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").isNotEmpty())
                    .andExpect(jsonPath("$.type").value("Bearer"))
                    .andExpect(jsonPath("$.userId").value(existingUser.getId()))
                    .andExpect(jsonPath("$.email").value("existing@sencours.sn"))
                    .andExpect(jsonPath("$.role").value("ETUDIANT"));
        }

        @Test
        @DisplayName("Devrait retourner 401 si email inexistant")
        void shouldReturn401WhenEmailNotFound() throws Exception {
            LoginRequest request = LoginRequest.builder()
                    .email("unknown@sencours.sn")
                    .password("password123")
                    .build();

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value(containsString("incorrect")));
        }

        @Test
        @DisplayName("Devrait retourner 401 si mot de passe incorrect")
        void shouldReturn401WhenPasswordIncorrect() throws Exception {
            LoginRequest request = LoginRequest.builder()
                    .email("existing@sencours.sn")
                    .password("wrongpassword")
                    .build();

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value(containsString("incorrect")));
        }

        @Test
        @DisplayName("Devrait retourner 400 si email manquant")
        void shouldReturn400WhenEmailMissing() throws Exception {
            LoginRequest request = LoginRequest.builder()
                    .password("password123")
                    .build();

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.validationErrors.email").exists());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/auth/me")
    class GetCurrentUserTests {

        @Test
        @DisplayName("Devrait retourner l'utilisateur connecté avec token valide - 200")
        void shouldReturnCurrentUserWithValidToken() throws Exception {
            String token = jwtService.generateToken(existingUser);

            mockMvc.perform(get("/api/v1/auth/me")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.userId").value(existingUser.getId()))
                    .andExpect(jsonPath("$.email").value("existing@sencours.sn"))
                    .andExpect(jsonPath("$.fullName").value("Existing User"))
                    .andExpect(jsonPath("$.role").value("ETUDIANT"));
        }

        @Test
        @DisplayName("Devrait retourner 401 sans token")
        void shouldReturn401WithoutToken() throws Exception {
            mockMvc.perform(get("/api/v1/auth/me"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Devrait retourner 401 avec token invalide")
        void shouldReturn401WithInvalidToken() throws Exception {
            mockMvc.perform(get("/api/v1/auth/me")
                            .header("Authorization", "Bearer invalid.token.here"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Devrait retourner 401 avec token mal formé")
        void shouldReturn401WithMalformedToken() throws Exception {
            mockMvc.perform(get("/api/v1/auth/me")
                            .header("Authorization", "InvalidFormat token"))
                    .andExpect(status().isUnauthorized());
        }
    }
}
