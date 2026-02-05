package com.sencours.controller;

import com.sencours.entity.Category;
import com.sencours.entity.User;
import com.sencours.enums.Role;
import com.sencours.repository.CategoryRepository;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class CategoryPaginationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private static final String BASE_URL = "/api/v1/categories";

    private User admin;
    private String adminToken;

    @BeforeEach
    void setUp() {
        courseRepository.deleteAll();
        categoryRepository.deleteAll();
        userRepository.deleteAll();

        admin = createAdmin();
        adminToken = jwtService.generateToken(admin);

        // Create 12 categories for pagination tests
        String[] categoryNames = {
            "Développement Web", "Développement Mobile", "Data Science",
            "Machine Learning", "DevOps", "Cloud Computing",
            "Cybersécurité", "Bases de données", "Design UI/UX",
            "Gestion de projet", "Marketing Digital", "Business"
        };

        for (String name : categoryNames) {
            createCategory(name);
        }
    }

    @Nested
    @DisplayName("GET /api/v1/categories/paginated")
    class GetAllPaginatedTests {

        @Test
        @DisplayName("Devrait retourner la première page avec 10 éléments")
        void shouldReturnFirstPageWithDefaultSize() throws Exception {
            mockMvc.perform(get(BASE_URL + "/paginated"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(10)))
                    .andExpect(jsonPath("$.page").value(0))
                    .andExpect(jsonPath("$.size").value(10))
                    .andExpect(jsonPath("$.totalElements").value(12))
                    .andExpect(jsonPath("$.totalPages").value(2))
                    .andExpect(jsonPath("$.first").value(true))
                    .andExpect(jsonPath("$.last").value(false));
        }

        @Test
        @DisplayName("Devrait retourner la deuxième page")
        void shouldReturnSecondPage() throws Exception {
            mockMvc.perform(get(BASE_URL + "/paginated")
                            .param("page", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(2)))
                    .andExpect(jsonPath("$.page").value(1))
                    .andExpect(jsonPath("$.first").value(false))
                    .andExpect(jsonPath("$.last").value(true));
        }

        @Test
        @DisplayName("Devrait respecter la taille de page personnalisée")
        void shouldRespectCustomPageSize() throws Exception {
            mockMvc.perform(get(BASE_URL + "/paginated")
                            .param("size", "5"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(5)))
                    .andExpect(jsonPath("$.size").value(5))
                    .andExpect(jsonPath("$.totalPages").value(3));
        }

        @Test
        @DisplayName("Devrait trier par nom en ordre croissant")
        void shouldSortByNameAscending() throws Exception {
            mockMvc.perform(get(BASE_URL + "/paginated")
                            .param("sort", "name")
                            .param("direction", "asc"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].name").value("Bases de données"));
        }

        @Test
        @DisplayName("Devrait trier par nom en ordre décroissant")
        void shouldSortByNameDescending() throws Exception {
            mockMvc.perform(get(BASE_URL + "/paginated")
                            .param("sort", "name")
                            .param("direction", "desc"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].name").value("Marketing Digital"));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/categories/search/paginated")
    class SearchPaginatedTests {

        @Test
        @DisplayName("Devrait rechercher par nom avec pagination")
        void shouldSearchByNameWithPagination() throws Exception {
            mockMvc.perform(get(BASE_URL + "/search/paginated")
                            .param("name", "Développement"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(2))
                    .andExpect(jsonPath("$.content[*].name", everyItem(containsStringIgnoringCase("Développement"))));
        }

        @Test
        @DisplayName("Devrait rechercher par nom partiel (case insensitive)")
        void shouldSearchCaseInsensitive() throws Exception {
            mockMvc.perform(get(BASE_URL + "/search/paginated")
                            .param("name", "data"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(1))
                    .andExpect(jsonPath("$.content[0].name").value("Data Science"));
        }

        @Test
        @DisplayName("Devrait retourner une liste vide si aucun résultat")
        void shouldReturnEmptyListWhenNoResults() throws Exception {
            mockMvc.perform(get(BASE_URL + "/search/paginated")
                            .param("name", "Inexistant"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(0))
                    .andExpect(jsonPath("$.content", hasSize(0)));
        }
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

    private Category createCategory(String name) {
        Category category = new Category();
        category.setName(name);
        category.setDescription("Description de " + name);
        return categoryRepository.save(category);
    }
}
