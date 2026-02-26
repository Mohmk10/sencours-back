package com.sencours.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sencours.dto.request.ReorderRequest;
import com.sencours.dto.request.SectionRequest;
import com.sencours.entity.Category;
import com.sencours.entity.Course;
import com.sencours.entity.Section;
import com.sencours.entity.User;
import com.sencours.enums.Role;
import com.sencours.enums.Status;
import com.sencours.repository.CategoryRepository;
import com.sencours.repository.CourseRepository;
import com.sencours.repository.EnrollmentRepository;
import com.sencours.repository.LessonRepository;
import com.sencours.repository.ProgressRepository;
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

import java.math.BigDecimal;
import java.util.Arrays;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class SectionControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SectionRepository sectionRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProgressRepository progressRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private LessonRepository lessonRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private Course course;
    private User instructor;
    private String instructorToken;

    private String getBaseUrl() {
        return "/api/v1/courses/" + course.getId() + "/sections";
    }

    @BeforeEach
    void setUp() {
        progressRepository.deleteAll();
        enrollmentRepository.deleteAll();
        lessonRepository.deleteAll();
        sectionRepository.deleteAll();
        courseRepository.deleteAll();
        userRepository.deleteAll();
        categoryRepository.deleteAll();

        instructor = createInstructor();
        Category category = createCategory();
        course = createCourse(instructor, category);
        instructorToken = jwtService.generateToken(instructor);
    }

    @Nested
    @DisplayName("POST /api/v1/courses/{courseId}/sections")
    class CreateSectionTests {

        @Test
        @DisplayName("Devrait créer une section avec succès - 201")
        void shouldCreateSectionSuccessfully() throws Exception {
            SectionRequest request = SectionRequest.builder()
                    .title("Introduction au cours")
                    .build();

            mockMvc.perform(post(getBaseUrl())
                            .header("Authorization", "Bearer " + instructorToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").isNotEmpty())
                    .andExpect(jsonPath("$.title").value("Introduction au cours"))
                    .andExpect(jsonPath("$.orderIndex").value(1))
                    .andExpect(jsonPath("$.courseId").value(course.getId()));
        }

        @Test
        @DisplayName("Devrait auto-incrémenter orderIndex - 201")
        void shouldAutoIncrementOrderIndex() throws Exception {
            createSection("Section 1", 1);

            SectionRequest request = SectionRequest.builder()
                    .title("Section 2")
                    .build();

            mockMvc.perform(post(getBaseUrl())
                            .header("Authorization", "Bearer " + instructorToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.orderIndex").value(2));
        }

        @Test
        @DisplayName("Devrait retourner 400 si titre vide")
        void shouldReturn400WhenTitleEmpty() throws Exception {
            SectionRequest request = SectionRequest.builder()
                    .title("")
                    .build();

            mockMvc.perform(post(getBaseUrl())
                            .header("Authorization", "Bearer " + instructorToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.validationErrors.title").exists());
        }

        @Test
        @DisplayName("Devrait retourner 404 si cours non trouvé")
        void shouldReturn404WhenCourseNotFound() throws Exception {
            SectionRequest request = SectionRequest.builder()
                    .title("Section Test")
                    .build();

            mockMvc.perform(post("/api/v1/courses/999/sections")
                            .header("Authorization", "Bearer " + instructorToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value(containsString("Cours")));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/courses/{courseId}/sections")
    class GetSectionsByCourseTests {

        @Test
        @DisplayName("Devrait retourner les sections ordonnées - 200 (public)")
        void shouldReturnSectionsOrdered() throws Exception {
            createSection("Section 2", 2);
            createSection("Section 1", 1);
            createSection("Section 3", 3);

            mockMvc.perform(get(getBaseUrl()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(3)))
                    .andExpect(jsonPath("$[0].orderIndex").value(1))
                    .andExpect(jsonPath("$[1].orderIndex").value(2))
                    .andExpect(jsonPath("$[2].orderIndex").value(3));
        }

        @Test
        @DisplayName("Devrait retourner liste vide si aucune section - 200")
        void shouldReturnEmptyListWhenNoSections() throws Exception {
            mockMvc.perform(get(getBaseUrl()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/sections/{id}")
    class GetSectionByIdTests {

        @Test
        @DisplayName("Devrait retourner une section par ID - 200 (public)")
        void shouldReturnSectionById() throws Exception {
            Section section = createSection("Ma Section", 1);

            mockMvc.perform(get("/api/v1/sections/" + section.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(section.getId()))
                    .andExpect(jsonPath("$.title").value("Ma Section"))
                    .andExpect(jsonPath("$.orderIndex").value(1));
        }

        @Test
        @DisplayName("Devrait retourner 404 si section non trouvée")
        void shouldReturn404WhenSectionNotFound() throws Exception {
            mockMvc.perform(get("/api/v1/sections/999"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/sections/{id}")
    class UpdateSectionTests {

        @Test
        @DisplayName("Devrait mettre à jour une section - 200")
        void shouldUpdateSection() throws Exception {
            Section section = createSection("Ancien Titre", 1);

            SectionRequest request = SectionRequest.builder()
                    .title("Nouveau Titre")
                    .build();

            mockMvc.perform(put("/api/v1/sections/" + section.getId())
                            .header("Authorization", "Bearer " + instructorToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.title").value("Nouveau Titre"));
        }

        @Test
        @DisplayName("Devrait retourner 404 si section non trouvée")
        void shouldReturn404WhenSectionNotFound() throws Exception {
            SectionRequest request = SectionRequest.builder()
                    .title("Nouveau Titre")
                    .build();

            mockMvc.perform(put("/api/v1/sections/999")
                            .header("Authorization", "Bearer " + instructorToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/sections/{id}")
    class DeleteSectionTests {

        @Test
        @DisplayName("Devrait supprimer une section - 204")
        void shouldDeleteSection() throws Exception {
            Section section = createSection("Section à supprimer", 1);

            mockMvc.perform(delete("/api/v1/sections/" + section.getId())
                            .header("Authorization", "Bearer " + instructorToken))
                    .andExpect(status().isNoContent());

            mockMvc.perform(get("/api/v1/sections/" + section.getId()))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Devrait réorganiser les index après suppression")
        void shouldReorderAfterDelete() throws Exception {
            Section section1 = createSection("Section 1", 1);
            Section section2 = createSection("Section 2", 2);
            Section section3 = createSection("Section 3", 3);

            mockMvc.perform(delete("/api/v1/sections/" + section1.getId())
                            .header("Authorization", "Bearer " + instructorToken))
                    .andExpect(status().isNoContent());

            mockMvc.perform(get(getBaseUrl()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].orderIndex").value(1))
                    .andExpect(jsonPath("$[1].orderIndex").value(2));
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/courses/{courseId}/sections/reorder")
    class ReorderSectionsTests {

        @Test
        @DisplayName("Devrait réorganiser les sections - 200")
        void shouldReorderSections() throws Exception {
            Section section1 = createSection("Section 1", 1);
            Section section2 = createSection("Section 2", 2);
            Section section3 = createSection("Section 3", 3);

            ReorderRequest request = ReorderRequest.builder()
                    .orderedIds(Arrays.asList(section3.getId(), section1.getId(), section2.getId()))
                    .build();

            mockMvc.perform(put(getBaseUrl() + "/reorder")
                            .header("Authorization", "Bearer " + instructorToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(3)))
                    .andExpect(jsonPath("$[0].id").value(section3.getId()))
                    .andExpect(jsonPath("$[0].orderIndex").value(1))
                    .andExpect(jsonPath("$[1].id").value(section1.getId()))
                    .andExpect(jsonPath("$[1].orderIndex").value(2))
                    .andExpect(jsonPath("$[2].id").value(section2.getId()))
                    .andExpect(jsonPath("$[2].orderIndex").value(3));
        }

        @Test
        @DisplayName("Devrait retourner 404 si cours non trouvé")
        void shouldReturn404WhenCourseNotFound() throws Exception {
            ReorderRequest request = ReorderRequest.builder()
                    .orderedIds(Arrays.asList(1L, 2L))
                    .build();

            mockMvc.perform(put("/api/v1/courses/999/sections/reorder")
                            .header("Authorization", "Bearer " + instructorToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
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

    private Course createCourse(User instructor, Category category) {
        Course c = new Course();
        c.setTitle("Java pour débutants");
        c.setDescription("Apprenez Java");
        c.setPrice(new BigDecimal("25000"));
        c.setStatus(Status.DRAFT);
        c.setInstructor(instructor);
        c.setCategory(category);
        return courseRepository.save(c);
    }

    private Section createSection(String title, int orderIndex) {
        Section section = new Section();
        section.setTitle(title);
        section.setOrderIndex(orderIndex);
        section.setCourse(course);
        return sectionRepository.save(section);
    }
}
