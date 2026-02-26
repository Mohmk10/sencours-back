package com.sencours.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sencours.dto.request.LessonRequest;
import com.sencours.dto.request.ReorderRequest;
import com.sencours.entity.*;
import com.sencours.enums.LessonType;
import com.sencours.enums.Role;
import com.sencours.enums.Status;
import com.sencours.repository.*;
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
class LessonControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private LessonRepository lessonRepository;

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
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private Section section;
    private User instructor;
    private String instructorToken;

    private String getBaseUrl() {
        return "/api/v1/sections/" + section.getId() + "/lessons";
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
        Course course = createCourse(instructor, category);
        section = createSection(course);
        instructorToken = jwtService.generateToken(instructor);
    }

    @Nested
    @DisplayName("POST /api/v1/sections/{sectionId}/lessons")
    class CreateLessonTests {

        @Test
        @DisplayName("Devrait créer une leçon avec succès - 201")
        void shouldCreateLessonSuccessfully() throws Exception {
            LessonRequest request = LessonRequest.builder()
                    .title("Introduction")
                    .type(LessonType.VIDEO)
                    .content("https://video.url/intro.mp4")
                    .duration(15)
                    .isFree(true)
                    .build();

            mockMvc.perform(post(getBaseUrl())
                            .header("Authorization", "Bearer " + instructorToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").isNotEmpty())
                    .andExpect(jsonPath("$.title").value("Introduction"))
                    .andExpect(jsonPath("$.type").value("VIDEO"))
                    .andExpect(jsonPath("$.content").value("https://video.url/intro.mp4"))
                    .andExpect(jsonPath("$.duration").value(15))
                    .andExpect(jsonPath("$.orderIndex").value(1))
                    .andExpect(jsonPath("$.isFree").value(true))
                    .andExpect(jsonPath("$.sectionId").value(section.getId()));
        }

        @Test
        @DisplayName("Devrait auto-incrémenter orderIndex - 201")
        void shouldAutoIncrementOrderIndex() throws Exception {
            createLesson("Leçon 1", LessonType.VIDEO, 1);

            LessonRequest request = LessonRequest.builder()
                    .title("Leçon 2")
                    .type(LessonType.TEXT)
                    .content("Contenu texte")
                    .duration(10)
                    .isFree(false)
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
            LessonRequest request = LessonRequest.builder()
                    .title("")
                    .type(LessonType.VIDEO)
                    .content("https://video.url")
                    .build();

            mockMvc.perform(post(getBaseUrl())
                            .header("Authorization", "Bearer " + instructorToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.validationErrors.title").exists());
        }

        @Test
        @DisplayName("Devrait retourner 400 si type null")
        void shouldReturn400WhenTypeNull() throws Exception {
            LessonRequest request = LessonRequest.builder()
                    .title("Ma leçon")
                    .type(null)
                    .content("Contenu")
                    .build();

            mockMvc.perform(post(getBaseUrl())
                            .header("Authorization", "Bearer " + instructorToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.validationErrors.type").exists());
        }

        @Test
        @DisplayName("Devrait retourner 404 si section non trouvée")
        void shouldReturn404WhenSectionNotFound() throws Exception {
            LessonRequest request = LessonRequest.builder()
                    .title("Ma leçon")
                    .type(LessonType.VIDEO)
                    .content("https://video.url")
                    .build();

            mockMvc.perform(post("/api/v1/sections/999/lessons")
                            .header("Authorization", "Bearer " + instructorToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/sections/{sectionId}/lessons")
    class GetLessonsBySectionTests {

        @Test
        @DisplayName("Devrait retourner les leçons ordonnées - 200 (public)")
        void shouldReturnLessonsOrdered() throws Exception {
            createLesson("Leçon 2", LessonType.TEXT, 2);
            createLesson("Leçon 1", LessonType.VIDEO, 1);
            createLesson("Leçon 3", LessonType.QUIZ, 3);

            mockMvc.perform(get(getBaseUrl()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(3)))
                    .andExpect(jsonPath("$[0].orderIndex").value(1))
                    .andExpect(jsonPath("$[0].title").value("Leçon 1"))
                    .andExpect(jsonPath("$[1].orderIndex").value(2))
                    .andExpect(jsonPath("$[2].orderIndex").value(3));
        }

        @Test
        @DisplayName("Devrait retourner liste vide si aucune leçon - 200")
        void shouldReturnEmptyListWhenNoLessons() throws Exception {
            mockMvc.perform(get(getBaseUrl()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/lessons/{id}")
    class GetLessonByIdTests {

        @Test
        @DisplayName("Devrait retourner une leçon par ID - 200 (public)")
        void shouldReturnLessonById() throws Exception {
            Lesson lesson = createLesson("Ma Leçon", LessonType.VIDEO, 1);

            mockMvc.perform(get("/api/v1/lessons/" + lesson.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(lesson.getId()))
                    .andExpect(jsonPath("$.title").value("Ma Leçon"))
                    .andExpect(jsonPath("$.type").value("VIDEO"));
        }

        @Test
        @DisplayName("Devrait retourner 404 si leçon non trouvée")
        void shouldReturn404WhenLessonNotFound() throws Exception {
            mockMvc.perform(get("/api/v1/lessons/999"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/lessons/{id}")
    class UpdateLessonTests {

        @Test
        @DisplayName("Devrait mettre à jour une leçon - 200")
        void shouldUpdateLesson() throws Exception {
            Lesson lesson = createLesson("Ancien Titre", LessonType.VIDEO, 1);

            LessonRequest request = LessonRequest.builder()
                    .title("Nouveau Titre")
                    .type(LessonType.TEXT)
                    .content("Nouveau contenu")
                    .duration(20)
                    .isFree(false)
                    .build();

            mockMvc.perform(put("/api/v1/lessons/" + lesson.getId())
                            .header("Authorization", "Bearer " + instructorToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.title").value("Nouveau Titre"))
                    .andExpect(jsonPath("$.type").value("TEXT"))
                    .andExpect(jsonPath("$.content").value("Nouveau contenu"));
        }

        @Test
        @DisplayName("Devrait retourner 404 si leçon non trouvée")
        void shouldReturn404WhenLessonNotFound() throws Exception {
            LessonRequest request = LessonRequest.builder()
                    .title("Nouveau Titre")
                    .type(LessonType.VIDEO)
                    .content("Contenu")
                    .build();

            mockMvc.perform(put("/api/v1/lessons/999")
                            .header("Authorization", "Bearer " + instructorToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/lessons/{id}")
    class DeleteLessonTests {

        @Test
        @DisplayName("Devrait supprimer une leçon - 204")
        void shouldDeleteLesson() throws Exception {
            Lesson lesson = createLesson("Leçon à supprimer", LessonType.VIDEO, 1);

            mockMvc.perform(delete("/api/v1/lessons/" + lesson.getId())
                            .header("Authorization", "Bearer " + instructorToken))
                    .andExpect(status().isNoContent());

            mockMvc.perform(get("/api/v1/lessons/" + lesson.getId()))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Devrait réorganiser les index après suppression")
        void shouldReorderAfterDelete() throws Exception {
            Lesson lesson1 = createLesson("Leçon 1", LessonType.VIDEO, 1);
            Lesson lesson2 = createLesson("Leçon 2", LessonType.TEXT, 2);
            Lesson lesson3 = createLesson("Leçon 3", LessonType.QUIZ, 3);

            mockMvc.perform(delete("/api/v1/lessons/" + lesson1.getId())
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
    @DisplayName("PUT /api/v1/sections/{sectionId}/lessons/reorder")
    class ReorderLessonsTests {

        @Test
        @DisplayName("Devrait réorganiser les leçons - 200")
        void shouldReorderLessons() throws Exception {
            Lesson lesson1 = createLesson("Leçon 1", LessonType.VIDEO, 1);
            Lesson lesson2 = createLesson("Leçon 2", LessonType.TEXT, 2);
            Lesson lesson3 = createLesson("Leçon 3", LessonType.QUIZ, 3);

            ReorderRequest request = ReorderRequest.builder()
                    .orderedIds(Arrays.asList(lesson3.getId(), lesson1.getId(), lesson2.getId()))
                    .build();

            mockMvc.perform(put(getBaseUrl() + "/reorder")
                            .header("Authorization", "Bearer " + instructorToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(3)))
                    .andExpect(jsonPath("$[0].id").value(lesson3.getId()))
                    .andExpect(jsonPath("$[0].orderIndex").value(1))
                    .andExpect(jsonPath("$[1].id").value(lesson1.getId()))
                    .andExpect(jsonPath("$[1].orderIndex").value(2))
                    .andExpect(jsonPath("$[2].id").value(lesson2.getId()))
                    .andExpect(jsonPath("$[2].orderIndex").value(3));
        }

        @Test
        @DisplayName("Devrait retourner 404 si section non trouvée")
        void shouldReturn404WhenSectionNotFound() throws Exception {
            ReorderRequest request = ReorderRequest.builder()
                    .orderedIds(Arrays.asList(1L, 2L))
                    .build();

            mockMvc.perform(put("/api/v1/sections/999/lessons/reorder")
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

    private Section createSection(Course course) {
        Section s = new Section();
        s.setTitle("Introduction");
        s.setOrderIndex(1);
        s.setCourse(course);
        return sectionRepository.save(s);
    }

    private Lesson createLesson(String title, LessonType type, int orderIndex) {
        Lesson lesson = new Lesson();
        lesson.setTitle(title);
        lesson.setType(type);
        lesson.setContent("Contenu de " + title);
        lesson.setDuration(10);
        lesson.setOrderIndex(orderIndex);
        lesson.setIsFree(false);
        lesson.setSection(section);
        return lessonRepository.save(lesson);
    }
}
