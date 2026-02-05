package com.sencours.controller;

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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AdminControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private static final String BASE_URL = "/api/v1/admin";

    private User admin;
    private String adminToken;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        admin = createUser("Admin", "SenCours", "admin@sencours.sn", Role.ADMIN);
        adminToken = jwtService.generateToken(admin);

        // Create 10 students
        for (int i = 1; i <= 10; i++) {
            createUser("Etudiant" + i, "Test", "etudiant" + i + "@sencours.sn", Role.ETUDIANT);
        }

        // Create 5 instructors
        for (int i = 1; i <= 5; i++) {
            createUser("Instructeur" + i, "Prof", "instructeur" + i + "@sencours.sn", Role.INSTRUCTEUR);
        }
    }

    @Nested
    @DisplayName("GET /api/v1/admin/users")
    class GetAllUsersPaginatedTests {

        @Test
        @DisplayName("Devrait retourner la première page avec 10 éléments")
        void shouldReturnFirstPageWithDefaultSize() throws Exception {
            mockMvc.perform(get(BASE_URL + "/users")
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(10)))
                    .andExpect(jsonPath("$.page").value(0))
                    .andExpect(jsonPath("$.size").value(10))
                    .andExpect(jsonPath("$.totalElements").value(16)) // 1 admin + 10 students + 5 instructors
                    .andExpect(jsonPath("$.totalPages").value(2))
                    .andExpect(jsonPath("$.first").value(true))
                    .andExpect(jsonPath("$.last").value(false));
        }

        @Test
        @DisplayName("Devrait retourner la deuxième page")
        void shouldReturnSecondPage() throws Exception {
            mockMvc.perform(get(BASE_URL + "/users")
                            .header("Authorization", "Bearer " + adminToken)
                            .param("page", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(6)))
                    .andExpect(jsonPath("$.page").value(1))
                    .andExpect(jsonPath("$.first").value(false))
                    .andExpect(jsonPath("$.last").value(true));
        }

        @Test
        @DisplayName("Devrait respecter la taille de page personnalisée")
        void shouldRespectCustomPageSize() throws Exception {
            mockMvc.perform(get(BASE_URL + "/users")
                            .header("Authorization", "Bearer " + adminToken)
                            .param("size", "5"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(5)))
                    .andExpect(jsonPath("$.size").value(5))
                    .andExpect(jsonPath("$.totalPages").value(4));
        }

        @Test
        @DisplayName("Devrait retourner 401 sans token")
        void shouldReturn401WithoutToken() throws Exception {
            mockMvc.perform(get(BASE_URL + "/users"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Devrait retourner 403 pour un étudiant")
        void shouldReturn403ForStudent() throws Exception {
            User student = userRepository.findByEmail("etudiant1@sencours.sn").orElseThrow();
            String studentToken = jwtService.generateToken(student);

            mockMvc.perform(get(BASE_URL + "/users")
                            .header("Authorization", "Bearer " + studentToken))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/admin/users/role/{role}")
    class GetUsersByRolePaginatedTests {

        @Test
        @DisplayName("Devrait filtrer par rôle ETUDIANT avec pagination")
        void shouldFilterByStudentRoleWithPagination() throws Exception {
            mockMvc.perform(get(BASE_URL + "/users/role/ETUDIANT")
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(10))
                    .andExpect(jsonPath("$.content[*].role", everyItem(is("ETUDIANT"))));
        }

        @Test
        @DisplayName("Devrait filtrer par rôle INSTRUCTEUR avec pagination")
        void shouldFilterByInstructorRoleWithPagination() throws Exception {
            mockMvc.perform(get(BASE_URL + "/users/role/INSTRUCTEUR")
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(5))
                    .andExpect(jsonPath("$.content[*].role", everyItem(is("INSTRUCTEUR"))));
        }

        @Test
        @DisplayName("Devrait filtrer par rôle ADMIN avec pagination")
        void shouldFilterByAdminRoleWithPagination() throws Exception {
            mockMvc.perform(get(BASE_URL + "/users/role/ADMIN")
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(1))
                    .andExpect(jsonPath("$.content[0].role").value("ADMIN"));
        }

        @Test
        @DisplayName("Devrait paginer correctement les résultats filtrés")
        void shouldPaginateFilteredResults() throws Exception {
            mockMvc.perform(get(BASE_URL + "/users/role/ETUDIANT")
                            .header("Authorization", "Bearer " + adminToken)
                            .param("size", "5"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(5)))
                    .andExpect(jsonPath("$.totalPages").value(2));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/admin/users/search")
    class SearchUsersPaginatedTests {

        @Test
        @DisplayName("Devrait rechercher par prénom avec pagination")
        void shouldSearchByFirstNameWithPagination() throws Exception {
            mockMvc.perform(get(BASE_URL + "/users/search")
                            .header("Authorization", "Bearer " + adminToken)
                            .param("search", "Etudiant"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(10))
                    .andExpect(jsonPath("$.content[*].firstName", everyItem(containsStringIgnoringCase("Etudiant"))));
        }

        @Test
        @DisplayName("Devrait rechercher par nom avec pagination")
        void shouldSearchByLastNameWithPagination() throws Exception {
            mockMvc.perform(get(BASE_URL + "/users/search")
                            .header("Authorization", "Bearer " + adminToken)
                            .param("search", "Prof"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(5));
        }

        @Test
        @DisplayName("Devrait rechercher par email avec pagination")
        void shouldSearchByEmailWithPagination() throws Exception {
            mockMvc.perform(get(BASE_URL + "/users/search")
                            .header("Authorization", "Bearer " + adminToken)
                            .param("search", "instructeur"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(5));
        }

        @Test
        @DisplayName("Devrait retourner une liste vide si aucun résultat")
        void shouldReturnEmptyListWhenNoResults() throws Exception {
            mockMvc.perform(get(BASE_URL + "/users/search")
                            .header("Authorization", "Bearer " + adminToken)
                            .param("search", "Inexistant"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(0))
                    .andExpect(jsonPath("$.content", hasSize(0)));
        }
    }

    private User createUser(String firstName, String lastName, String email, Role role) {
        User user = User.builder()
                .firstName(firstName)
                .lastName(lastName)
                .email(email)
                .password(passwordEncoder.encode("password123"))
                .role(role)
                .build();
        return userRepository.save(user);
    }
}
