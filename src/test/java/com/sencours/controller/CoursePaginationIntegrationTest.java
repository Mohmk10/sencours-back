package com.sencours.controller;

import com.sencours.entity.Category;
import com.sencours.entity.Course;
import com.sencours.entity.User;
import com.sencours.enums.Role;
import com.sencours.enums.Status;
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

import java.math.BigDecimal;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class CoursePaginationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

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

    private static final String BASE_URL = "/api/v1/courses";

    private User instructor;
    private Category category;
    private String instructorToken;

    @BeforeEach
    void setUp() {
        courseRepository.deleteAll();
        userRepository.deleteAll();
        categoryRepository.deleteAll();

        instructor = createInstructor();
        category = createCategory();
        instructorToken = jwtService.generateToken(instructor);

        // Create 15 courses for pagination tests
        for (int i = 1; i <= 15; i++) {
            createCourse("Cours " + i, i <= 10 ? Status.PUBLISHED : Status.DRAFT);
        }
    }

    @Nested
    @DisplayName("GET /api/v1/courses/paginated")
    class GetAllPaginatedTests {

        @Test
        @DisplayName("Devrait retourner la première page avec 10 éléments")
        void shouldReturnFirstPageWithDefaultSize() throws Exception {
            mockMvc.perform(get(BASE_URL + "/paginated"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(10)))
                    .andExpect(jsonPath("$.page").value(0))
                    .andExpect(jsonPath("$.size").value(10))
                    .andExpect(jsonPath("$.totalElements").value(15))
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
                    .andExpect(jsonPath("$.content", hasSize(5)))
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
        @DisplayName("Devrait limiter la taille de page à 50 maximum")
        void shouldLimitPageSizeTo50() throws Exception {
            mockMvc.perform(get(BASE_URL + "/paginated")
                            .param("size", "100"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.size").value(50));
        }

        @Test
        @DisplayName("Devrait trier par titre en ordre décroissant")
        void shouldSortByTitleDescending() throws Exception {
            mockMvc.perform(get(BASE_URL + "/paginated")
                            .param("sort", "title")
                            .param("direction", "desc"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].title").value("Cours 9"));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/courses/search/paginated")
    class SearchPaginatedTests {

        @Test
        @DisplayName("Devrait rechercher avec pagination")
        void shouldSearchWithPagination() throws Exception {
            mockMvc.perform(get(BASE_URL + "/search/paginated")
                            .param("title", "Cours 1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(1))))
                    .andExpect(jsonPath("$.content[*].title", everyItem(containsStringIgnoringCase("Cours 1"))));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/courses/status/{status}/paginated")
    class GetByStatusPaginatedTests {

        @Test
        @DisplayName("Devrait filtrer par status avec pagination")
        void shouldFilterByStatusWithPagination() throws Exception {
            mockMvc.perform(get(BASE_URL + "/status/PUBLISHED/paginated"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(10))
                    .andExpect(jsonPath("$.content[*].status", everyItem(is("PUBLISHED"))));
        }

        @Test
        @DisplayName("Devrait filtrer par status DRAFT avec pagination")
        void shouldFilterByDraftStatusWithPagination() throws Exception {
            mockMvc.perform(get(BASE_URL + "/status/DRAFT/paginated"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(5))
                    .andExpect(jsonPath("$.content[*].status", everyItem(is("DRAFT"))));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/courses/category/{categoryId}/paginated")
    class GetByCategoryPaginatedTests {

        @Test
        @DisplayName("Devrait filtrer par catégorie avec pagination")
        void shouldFilterByCategoryWithPagination() throws Exception {
            mockMvc.perform(get(BASE_URL + "/category/" + category.getId() + "/paginated"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(15))
                    .andExpect(jsonPath("$.content[*].categoryId", everyItem(is(category.getId().intValue()))));
        }

        @Test
        @DisplayName("Devrait retourner 404 si catégorie non trouvée")
        void shouldReturn404WhenCategoryNotFound() throws Exception {
            mockMvc.perform(get(BASE_URL + "/category/999/paginated"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/courses/instructor/{instructorId}/paginated")
    class GetByInstructorPaginatedTests {

        @Test
        @DisplayName("Devrait filtrer par instructeur avec pagination")
        void shouldFilterByInstructorWithPagination() throws Exception {
            mockMvc.perform(get(BASE_URL + "/instructor/" + instructor.getId() + "/paginated"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(15))
                    .andExpect(jsonPath("$.content[*].instructorId", everyItem(is(instructor.getId().intValue()))));
        }

        @Test
        @DisplayName("Devrait retourner 404 si instructeur non trouvé")
        void shouldReturn404WhenInstructorNotFound() throws Exception {
            mockMvc.perform(get(BASE_URL + "/instructor/999/paginated"))
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

    private Category createCategory() {
        Category cat = new Category();
        cat.setName("Développement Web");
        cat.setDescription("Cours de développement web");
        return categoryRepository.save(cat);
    }

    private Course createCourse(String title, Status status) {
        Course course = new Course();
        course.setTitle(title);
        course.setDescription("Description du cours");
        course.setPrice(new BigDecimal("25000"));
        course.setStatus(status);
        course.setInstructor(instructor);
        course.setCategory(category);
        return courseRepository.save(course);
    }
}
