package com.sencours.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sencours.dto.request.CategoryRequest;
import com.sencours.entity.Category;
import com.sencours.entity.User;
import com.sencours.enums.Role;
import com.sencours.repository.CategoryRepository;
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
class CategoryControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CategoryRepository categoryRepository;

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
        categoryRepository.deleteAll();
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
    @DisplayName("POST /api/v1/categories")
    class CreateCategoryTests {

        @Test
        @DisplayName("Devrait créer une catégorie avec succès - 201")
        void shouldCreateCategorySuccessfully() throws Exception {
            CategoryRequest request = CategoryRequest.builder()
                    .name("Développement Web")
                    .description("Cours de développement web")
                    .build();

            mockMvc.perform(post(BASE_URL)
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").isNotEmpty())
                    .andExpect(jsonPath("$.name").value("Développement Web"))
                    .andExpect(jsonPath("$.description").value("Cours de développement web"));
        }

        @Test
        @DisplayName("Devrait retourner 400 si nom vide")
        void shouldReturn400WhenNameIsEmpty() throws Exception {
            CategoryRequest request = CategoryRequest.builder()
                    .name("")
                    .description("Description")
                    .build();

            mockMvc.perform(post(BASE_URL)
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.validationErrors.name").exists());
        }

        @Test
        @DisplayName("Devrait retourner 400 si nom trop court")
        void shouldReturn400WhenNameTooShort() throws Exception {
            CategoryRequest request = CategoryRequest.builder()
                    .name("A")
                    .description("Description")
                    .build();

            mockMvc.perform(post(BASE_URL)
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.validationErrors.name").value(containsString("2")));
        }

        @Test
        @DisplayName("Devrait retourner 409 si nom existe déjà")
        void shouldReturn409WhenNameAlreadyExists() throws Exception {
            Category existingCategory = new Category();
            existingCategory.setName("Développement Web");
            categoryRepository.save(existingCategory);

            CategoryRequest request = CategoryRequest.builder()
                    .name("Développement Web")
                    .description("Description")
                    .build();

            mockMvc.perform(post(BASE_URL)
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.error").value("Conflict"));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/categories")
    class GetAllCategoriesTests {

        @Test
        @DisplayName("Devrait retourner toutes les catégories - 200")
        void shouldReturnAllCategories() throws Exception {
            Category cat1 = new Category();
            cat1.setName("Développement Web");
            categoryRepository.save(cat1);

            Category cat2 = new Category();
            cat2.setName("Design");
            categoryRepository.save(cat2);

            mockMvc.perform(get(BASE_URL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[*].name", containsInAnyOrder("Développement Web", "Design")));
        }

        @Test
        @DisplayName("Devrait retourner une liste vide - 200")
        void shouldReturnEmptyList() throws Exception {
            mockMvc.perform(get(BASE_URL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/categories/{id}")
    class GetCategoryByIdTests {

        @Test
        @DisplayName("Devrait retourner une catégorie par ID - 200")
        void shouldReturnCategoryById() throws Exception {
            Category category = new Category();
            category.setName("Développement Web");
            category.setDescription("Description");
            Category saved = categoryRepository.save(category);

            mockMvc.perform(get(BASE_URL + "/" + saved.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(saved.getId()))
                    .andExpect(jsonPath("$.name").value("Développement Web"));
        }

        @Test
        @DisplayName("Devrait retourner 404 si ID non trouvé")
        void shouldReturn404WhenIdNotFound() throws Exception {
            mockMvc.perform(get(BASE_URL + "/999"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error").value("Not Found"))
                    .andExpect(jsonPath("$.message").value(containsString("999")));
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/categories/{id}")
    class UpdateCategoryTests {

        @Test
        @DisplayName("Devrait mettre à jour une catégorie - 200")
        void shouldUpdateCategory() throws Exception {
            Category category = new Category();
            category.setName("Développement Web");
            Category saved = categoryRepository.save(category);

            CategoryRequest request = CategoryRequest.builder()
                    .name("Développement Web Avancé")
                    .description("Description mise à jour")
                    .build();

            mockMvc.perform(put(BASE_URL + "/" + saved.getId())
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Développement Web Avancé"))
                    .andExpect(jsonPath("$.description").value("Description mise à jour"));
        }

        @Test
        @DisplayName("Devrait retourner 404 si ID non trouvé lors de update")
        void shouldReturn404WhenIdNotFoundOnUpdate() throws Exception {
            CategoryRequest request = CategoryRequest.builder()
                    .name("Test")
                    .build();

            mockMvc.perform(put(BASE_URL + "/999")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Devrait retourner 409 si nouveau nom existe déjà")
        void shouldReturn409WhenNewNameExists() throws Exception {
            Category cat1 = new Category();
            cat1.setName("Développement Web");
            categoryRepository.save(cat1);

            Category cat2 = new Category();
            cat2.setName("Design");
            Category saved = categoryRepository.save(cat2);

            CategoryRequest request = CategoryRequest.builder()
                    .name("Développement Web")
                    .build();

            mockMvc.perform(put(BASE_URL + "/" + saved.getId())
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("Devrait permettre de garder le même nom")
        void shouldAllowSameName() throws Exception {
            Category category = new Category();
            category.setName("Développement Web");
            Category saved = categoryRepository.save(category);

            CategoryRequest request = CategoryRequest.builder()
                    .name("Développement Web")
                    .description("Nouvelle description")
                    .build();

            mockMvc.perform(put(BASE_URL + "/" + saved.getId())
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.description").value("Nouvelle description"));
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/categories/{id}")
    class DeleteCategoryTests {

        @Test
        @DisplayName("Devrait supprimer une catégorie - 204")
        void shouldDeleteCategory() throws Exception {
            Category category = new Category();
            category.setName("Développement Web");
            Category saved = categoryRepository.save(category);

            mockMvc.perform(delete(BASE_URL + "/" + saved.getId())
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isNoContent());

            mockMvc.perform(get(BASE_URL + "/" + saved.getId()))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Devrait retourner 404 si ID non trouvé lors de delete")
        void shouldReturn404WhenIdNotFoundOnDelete() throws Exception {
            mockMvc.perform(delete(BASE_URL + "/999")
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isNotFound());
        }
    }
}
