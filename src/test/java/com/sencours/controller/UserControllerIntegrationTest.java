package com.sencours.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sencours.dto.request.PasswordChangeRequest;
import com.sencours.dto.request.UserRequest;
import com.sencours.entity.User;
import com.sencours.enums.Role;
import com.sencours.repository.CourseRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class UserControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private static final String BASE_URL = "/api/v1/users";

    private User admin;
    private String adminToken;

    @BeforeEach
    void setUp() {
        courseRepository.deleteAll();
        userRepository.deleteAll();

        admin = createAdmin();
        adminToken = jwtService.generateToken(admin);
    }

    private User createAdmin() {
        User user = User.builder()
                .firstName("Admin")
                .lastName("SenCours")
                .email("admin@sencours.sn")
                .password(passwordEncoder.encode("admin123"))
                .role(Role.ADMIN)
                .build();
        return userRepository.save(user);
    }

    @Nested
    @DisplayName("POST /api/v1/users")
    class CreateUserTests {

        @Test
        @DisplayName("Devrait créer un utilisateur avec mot de passe hashé - 201")
        void shouldCreateUserWithHashedPassword() throws Exception {
            UserRequest request = UserRequest.builder()
                    .firstName("Mamadou")
                    .lastName("Diallo")
                    .email("mamadou@sencours.sn")
                    .password("password123")
                    .role(Role.ETUDIANT)
                    .build();

            mockMvc.perform(post(BASE_URL)
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").isNotEmpty())
                    .andExpect(jsonPath("$.firstName").value("Mamadou"))
                    .andExpect(jsonPath("$.email").value("mamadou@sencours.sn"))
                    .andExpect(jsonPath("$.password").doesNotExist())
                    .andExpect(jsonPath("$.isActive").value(true));

            User savedUser = userRepository.findByEmail("mamadou@sencours.sn").orElseThrow();
            assertThat(savedUser.getPassword()).startsWith("$2a$");
        }

        @Test
        @DisplayName("Devrait retourner 400 si email invalide")
        void shouldReturn400WhenEmailInvalid() throws Exception {
            UserRequest request = UserRequest.builder()
                    .firstName("Mamadou")
                    .lastName("Diallo")
                    .email("invalid-email")
                    .password("password123")
                    .role(Role.ETUDIANT)
                    .build();

            mockMvc.perform(post(BASE_URL)
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.validationErrors.email").exists());
        }

        @Test
        @DisplayName("Devrait retourner 400 si mot de passe trop court")
        void shouldReturn400WhenPasswordTooShort() throws Exception {
            UserRequest request = UserRequest.builder()
                    .firstName("Mamadou")
                    .lastName("Diallo")
                    .email("mamadou@sencours.sn")
                    .password("short")
                    .role(Role.ETUDIANT)
                    .build();

            mockMvc.perform(post(BASE_URL)
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.validationErrors.password").value(containsString("8")));
        }

        @Test
        @DisplayName("Devrait retourner 409 si email existe déjà")
        void shouldReturn409WhenEmailExists() throws Exception {
            User existingUser = User.builder()
                    .firstName("Existing")
                    .lastName("User")
                    .email("mamadou@sencours.sn")
                    .password(passwordEncoder.encode("password123"))
                    .role(Role.ETUDIANT)
                    .build();
            userRepository.save(existingUser);

            UserRequest request = UserRequest.builder()
                    .firstName("Mamadou")
                    .lastName("Diallo")
                    .email("mamadou@sencours.sn")
                    .password("password123")
                    .role(Role.ETUDIANT)
                    .build();

            mockMvc.perform(post(BASE_URL)
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("Le mot de passe ne doit JAMAIS apparaître dans la réponse")
        void passwordShouldNeverAppearInResponse() throws Exception {
            UserRequest request = UserRequest.builder()
                    .firstName("Mamadou")
                    .lastName("Diallo")
                    .email("mamadou@sencours.sn")
                    .password("password123")
                    .role(Role.ETUDIANT)
                    .build();

            String response = mockMvc.perform(post(BASE_URL)
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andReturn().getResponse().getContentAsString();

            assertThat(response).doesNotContain("password");
            assertThat(response).doesNotContain("$2a$");
        }
    }

    @Nested
    @DisplayName("GET /api/v1/users")
    class GetAllUsersTests {

        @Test
        @DisplayName("Devrait retourner tous les utilisateurs sans mot de passe - 200")
        void shouldReturnAllUsersWithoutPassword() throws Exception {
            createTestUser("user1@test.sn");
            createTestUser("user2@test.sn");

            String response = mockMvc.perform(get(BASE_URL)
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(3))) // admin + 2 users
                    .andReturn().getResponse().getContentAsString();

            assertThat(response).doesNotContain("password");
            assertThat(response).doesNotContain("$2a$");
        }
    }

    @Nested
    @DisplayName("GET /api/v1/users/{id}")
    class GetUserByIdTests {

        @Test
        @DisplayName("Devrait retourner un utilisateur par ID sans mot de passe - 200")
        void shouldReturnUserByIdWithoutPassword() throws Exception {
            User user = createTestUser("mamadou@sencours.sn");

            String response = mockMvc.perform(get(BASE_URL + "/" + user.getId())
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(user.getId()))
                    .andExpect(jsonPath("$.email").value("mamadou@sencours.sn"))
                    .andReturn().getResponse().getContentAsString();

            assertThat(response).doesNotContain("password");
        }

        @Test
        @DisplayName("Devrait retourner 404 si utilisateur non trouvé")
        void shouldReturn404WhenUserNotFound() throws Exception {
            mockMvc.perform(get(BASE_URL + "/999")
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/users/email/{email}")
    class GetUserByEmailTests {

        @Test
        @DisplayName("Devrait retourner un utilisateur par email - 200")
        void shouldReturnUserByEmail() throws Exception {
            createTestUser("mamadou@sencours.sn");

            mockMvc.perform(get(BASE_URL + "/email/mamadou@sencours.sn")
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").value("mamadou@sencours.sn"));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/users/role/{role}")
    class GetUsersByRoleTests {

        @Test
        @DisplayName("Devrait retourner les utilisateurs par rôle - 200")
        void shouldReturnUsersByRole() throws Exception {
            createTestUser("etudiant@test.sn", Role.ETUDIANT);
            createTestUser("prof@test.sn", Role.INSTRUCTEUR);

            mockMvc.perform(get(BASE_URL + "/role/ETUDIANT")
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].role").value("ETUDIANT"));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/users/active")
    class GetActiveUsersTests {

        @Test
        @DisplayName("Devrait retourner les utilisateurs actifs - 200")
        void shouldReturnActiveUsers() throws Exception {
            User active = createTestUser("active@test.sn");
            User inactive = createTestUser("inactive@test.sn");
            inactive.setIsActive(false);
            userRepository.save(inactive);

            mockMvc.perform(get(BASE_URL + "/active")
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2))) // admin + active user
                    .andExpect(jsonPath("$[?(@.email == 'active@test.sn')]").exists());
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/users/{id}")
    class UpdateUserTests {

        @Test
        @DisplayName("Devrait mettre à jour un utilisateur - 200")
        void shouldUpdateUser() throws Exception {
            User user = createTestUser("mamadou@sencours.sn");

            UserRequest request = UserRequest.builder()
                    .firstName("Mamadou Updated")
                    .lastName("Diallo Updated")
                    .email("mamadou@sencours.sn")
                    .password("password123")
                    .role(Role.INSTRUCTEUR)
                    .build();

            mockMvc.perform(put(BASE_URL + "/" + user.getId())
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.firstName").value("Mamadou Updated"))
                    .andExpect(jsonPath("$.role").value("INSTRUCTEUR"));
        }

        @Test
        @DisplayName("Devrait retourner 409 si nouvel email existe déjà")
        void shouldReturn409WhenNewEmailExists() throws Exception {
            User user1 = createTestUser("user1@test.sn");
            createTestUser("user2@test.sn");

            UserRequest request = UserRequest.builder()
                    .firstName("Test")
                    .lastName("User")
                    .email("user2@test.sn")
                    .password("password123")
                    .role(Role.ETUDIANT)
                    .build();

            mockMvc.perform(put(BASE_URL + "/" + user1.getId())
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict());
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/users/{id}")
    class DeleteUserTests {

        @Test
        @DisplayName("Devrait supprimer un utilisateur - 204")
        void shouldDeleteUser() throws Exception {
            User user = createTestUser("mamadou@sencours.sn");

            mockMvc.perform(delete(BASE_URL + "/" + user.getId())
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isNoContent());

            mockMvc.perform(get(BASE_URL + "/" + user.getId())
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/users/{id}/password")
    class ChangePasswordTests {

        @Test
        @DisplayName("Devrait changer le mot de passe - 200")
        void shouldChangePassword() throws Exception {
            User user = createTestUser("mamadou@sencours.sn");
            String originalPassword = user.getPassword();

            PasswordChangeRequest request = PasswordChangeRequest.builder()
                    .currentPassword("password123")
                    .newPassword("newPassword123")
                    .confirmPassword("newPassword123")
                    .build();

            mockMvc.perform(put(BASE_URL + "/" + user.getId() + "/password")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());

            User updatedUser = userRepository.findById(user.getId()).orElseThrow();
            assertThat(updatedUser.getPassword()).isNotEqualTo(originalPassword);
            assertThat(passwordEncoder.matches("newPassword123", updatedUser.getPassword())).isTrue();
        }

        @Test
        @DisplayName("Devrait retourner 400 si mot de passe actuel incorrect")
        void shouldReturn400WhenCurrentPasswordWrong() throws Exception {
            User user = createTestUser("mamadou@sencours.sn");

            PasswordChangeRequest request = PasswordChangeRequest.builder()
                    .currentPassword("wrongPassword")
                    .newPassword("newPassword123")
                    .confirmPassword("newPassword123")
                    .build();

            mockMvc.perform(put(BASE_URL + "/" + user.getId() + "/password")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(containsString("incorrect")));
        }

        @Test
        @DisplayName("Devrait retourner 400 si les mots de passe ne correspondent pas")
        void shouldReturn400WhenPasswordsDoNotMatch() throws Exception {
            User user = createTestUser("mamadou@sencours.sn");

            PasswordChangeRequest request = PasswordChangeRequest.builder()
                    .currentPassword("password123")
                    .newPassword("newPassword123")
                    .confirmPassword("differentPassword")
                    .build();

            mockMvc.perform(put(BASE_URL + "/" + user.getId() + "/password")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(containsString("correspondent")));
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/users/{id}/activate et deactivate")
    class ActivationTests {

        @Test
        @DisplayName("Devrait activer un utilisateur - 200")
        void shouldActivateUser() throws Exception {
            User user = createTestUser("mamadou@sencours.sn");
            user.setIsActive(false);
            userRepository.save(user);

            mockMvc.perform(put(BASE_URL + "/" + user.getId() + "/activate")
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk());

            User updatedUser = userRepository.findById(user.getId()).orElseThrow();
            assertThat(updatedUser.getIsActive()).isTrue();
        }

        @Test
        @DisplayName("Devrait désactiver un utilisateur - 200")
        void shouldDeactivateUser() throws Exception {
            User user = createTestUser("mamadou@sencours.sn");

            mockMvc.perform(put(BASE_URL + "/" + user.getId() + "/deactivate")
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk());

            User updatedUser = userRepository.findById(user.getId()).orElseThrow();
            assertThat(updatedUser.getIsActive()).isFalse();
        }
    }

    private User createTestUser(String email) {
        return createTestUser(email, Role.ETUDIANT);
    }

    private User createTestUser(String email, Role role) {
        User user = User.builder()
                .firstName("Test")
                .lastName("User")
                .email(email)
                .password(passwordEncoder.encode("password123"))
                .role(role)
                .build();
        return userRepository.save(user);
    }
}
