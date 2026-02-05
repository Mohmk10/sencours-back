package com.sencours.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sencours.dto.request.CourseRequest;
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
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class CourseControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

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
    }

    @Nested
    @DisplayName("POST /api/v1/courses")
    class CreateCourseTests {

        @Test
        @DisplayName("Devrait créer un cours avec succès - 201")
        void shouldCreateCourseSuccessfully() throws Exception {
            CourseRequest request = CourseRequest.builder()
                    .title("Java pour débutants")
                    .description("Apprenez Java de zéro")
                    .price(new BigDecimal("25000"))
                    .instructorId(instructor.getId())
                    .categoryId(category.getId())
                    .build();

            mockMvc.perform(post(BASE_URL)
                            .header("Authorization", "Bearer " + instructorToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").isNotEmpty())
                    .andExpect(jsonPath("$.title").value("Java pour débutants"))
                    .andExpect(jsonPath("$.status").value("DRAFT"))
                    .andExpect(jsonPath("$.instructorId").value(instructor.getId()))
                    .andExpect(jsonPath("$.instructorFirstName").value("Prof"))
                    .andExpect(jsonPath("$.categoryId").value(category.getId()))
                    .andExpect(jsonPath("$.categoryName").value("Développement Web"));
        }

        @Test
        @DisplayName("Devrait retourner 400 si titre vide")
        void shouldReturn400WhenTitleEmpty() throws Exception {
            CourseRequest request = CourseRequest.builder()
                    .title("")
                    .price(new BigDecimal("25000"))
                    .instructorId(instructor.getId())
                    .categoryId(category.getId())
                    .build();

            mockMvc.perform(post(BASE_URL)
                            .header("Authorization", "Bearer " + instructorToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.validationErrors.title").exists());
        }

        @Test
        @DisplayName("Devrait retourner 400 si prix négatif")
        void shouldReturn400WhenPriceNegative() throws Exception {
            CourseRequest request = CourseRequest.builder()
                    .title("Java pour débutants")
                    .price(new BigDecimal("-100"))
                    .instructorId(instructor.getId())
                    .categoryId(category.getId())
                    .build();

            mockMvc.perform(post(BASE_URL)
                            .header("Authorization", "Bearer " + instructorToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.validationErrors.price").exists());
        }

        @Test
        @DisplayName("Devrait retourner 404 si instructeur non trouvé")
        void shouldReturn404WhenInstructorNotFound() throws Exception {
            CourseRequest request = CourseRequest.builder()
                    .title("Java pour débutants")
                    .price(new BigDecimal("25000"))
                    .instructorId(999L)
                    .categoryId(category.getId())
                    .build();

            mockMvc.perform(post(BASE_URL)
                            .header("Authorization", "Bearer " + instructorToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value(containsString("Instructeur")));
        }

        @Test
        @DisplayName("Devrait retourner 400 si utilisateur n'est pas instructeur")
        void shouldReturn400WhenUserNotInstructor() throws Exception {
            User student = User.builder()
                    .firstName("Etudiant")
                    .lastName("Test")
                    .email("etudiant@test.sn")
                    .password(passwordEncoder.encode("password123"))
                    .role(Role.ETUDIANT)
                    .build();
            student = userRepository.save(student);

            CourseRequest request = CourseRequest.builder()
                    .title("Java pour débutants")
                    .price(new BigDecimal("25000"))
                    .instructorId(student.getId())
                    .categoryId(category.getId())
                    .build();

            mockMvc.perform(post(BASE_URL)
                            .header("Authorization", "Bearer " + instructorToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(containsString("INSTRUCTEUR")));
        }

        @Test
        @DisplayName("Devrait retourner 404 si catégorie non trouvée")
        void shouldReturn404WhenCategoryNotFound() throws Exception {
            CourseRequest request = CourseRequest.builder()
                    .title("Java pour débutants")
                    .price(new BigDecimal("25000"))
                    .instructorId(instructor.getId())
                    .categoryId(999L)
                    .build();

            mockMvc.perform(post(BASE_URL)
                            .header("Authorization", "Bearer " + instructorToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value(containsString("Catégorie")));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/courses")
    class GetAllCoursesTests {

        @Test
        @DisplayName("Devrait retourner tous les cours - 200")
        void shouldReturnAllCourses() throws Exception {
            createCourse("Cours 1");
            createCourse("Cours 2");

            mockMvc.perform(get(BASE_URL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/courses/{id}")
    class GetCourseByIdTests {

        @Test
        @DisplayName("Devrait retourner un cours par ID - 200")
        void shouldReturnCourseById() throws Exception {
            Course course = createCourse("Java pour débutants");

            mockMvc.perform(get(BASE_URL + "/" + course.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(course.getId()))
                    .andExpect(jsonPath("$.title").value("Java pour débutants"));
        }

        @Test
        @DisplayName("Devrait retourner 404 si cours non trouvé")
        void shouldReturn404WhenCourseNotFound() throws Exception {
            mockMvc.perform(get(BASE_URL + "/999"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/courses/instructor/{instructorId}")
    class GetCoursesByInstructorTests {

        @Test
        @DisplayName("Devrait retourner les cours d'un instructeur - 200")
        void shouldReturnCoursesByInstructor() throws Exception {
            createCourse("Cours 1");
            createCourse("Cours 2");

            mockMvc.perform(get(BASE_URL + "/instructor/" + instructor.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].instructorId").value(instructor.getId()));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/courses/category/{categoryId}")
    class GetCoursesByCategoryTests {

        @Test
        @DisplayName("Devrait retourner les cours d'une catégorie - 200")
        void shouldReturnCoursesByCategory() throws Exception {
            createCourse("Cours 1");

            mockMvc.perform(get(BASE_URL + "/category/" + category.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].categoryId").value(category.getId()));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/courses/status/{status}")
    class GetCoursesByStatusTests {

        @Test
        @DisplayName("Devrait retourner les cours par status - 200")
        void shouldReturnCoursesByStatus() throws Exception {
            Course course = createCourse("Cours publié");
            course.setStatus(Status.PUBLISHED);
            courseRepository.save(course);

            mockMvc.perform(get(BASE_URL + "/status/PUBLISHED"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].status").value("PUBLISHED"));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/courses/search")
    class SearchCoursesTests {

        @Test
        @DisplayName("Devrait rechercher des cours par titre - 200")
        void shouldSearchCoursesByTitle() throws Exception {
            createCourse("Java pour débutants");
            createCourse("Python avancé");

            mockMvc.perform(get(BASE_URL + "/search")
                            .param("title", "Java"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].title").value(containsString("Java")));
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/courses/{id}")
    class UpdateCourseTests {

        @Test
        @DisplayName("Devrait mettre à jour un cours - 200")
        void shouldUpdateCourse() throws Exception {
            Course course = createCourse("Ancien titre");

            CourseRequest request = CourseRequest.builder()
                    .title("Nouveau titre")
                    .description("Nouvelle description")
                    .price(new BigDecimal("30000"))
                    .instructorId(instructor.getId())
                    .categoryId(category.getId())
                    .build();

            mockMvc.perform(put(BASE_URL + "/" + course.getId())
                            .header("Authorization", "Bearer " + instructorToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.title").value("Nouveau titre"))
                    .andExpect(jsonPath("$.description").value("Nouvelle description"));
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/courses/{id}")
    class DeleteCourseTests {

        @Test
        @DisplayName("Devrait supprimer un cours - 204")
        void shouldDeleteCourse() throws Exception {
            Course course = createCourse("Cours à supprimer");

            mockMvc.perform(delete(BASE_URL + "/" + course.getId())
                            .header("Authorization", "Bearer " + instructorToken))
                    .andExpect(status().isNoContent());

            mockMvc.perform(get(BASE_URL + "/" + course.getId()))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("PATCH /api/v1/courses/{id}/publish et archive")
    class PublishArchiveTests {

        @Test
        @DisplayName("Devrait publier un cours - 200")
        void shouldPublishCourse() throws Exception {
            Course course = createCourse("Cours à publier");

            mockMvc.perform(patch(BASE_URL + "/" + course.getId() + "/publish")
                            .header("Authorization", "Bearer " + instructorToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("PUBLISHED"));
        }

        @Test
        @DisplayName("Devrait archiver un cours - 200")
        void shouldArchiveCourse() throws Exception {
            Course course = createCourse("Cours à archiver");

            mockMvc.perform(patch(BASE_URL + "/" + course.getId() + "/archive")
                            .header("Authorization", "Bearer " + instructorToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("ARCHIVED"));
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

    private Course createCourse(String title) {
        Course course = new Course();
        course.setTitle(title);
        course.setDescription("Description du cours");
        course.setPrice(new BigDecimal("25000"));
        course.setStatus(Status.DRAFT);
        course.setInstructor(instructor);
        course.setCategory(category);
        return courseRepository.save(course);
    }
}
