package com.sencours.mapper;

import com.sencours.dto.request.CourseRequest;
import com.sencours.dto.response.CourseResponse;
import com.sencours.entity.Category;
import com.sencours.entity.Course;
import com.sencours.entity.User;
import com.sencours.enums.Role;
import com.sencours.enums.Status;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class CourseMapperTest {

    private CourseMapper courseMapper;
    private User instructor;
    private Category category;

    @BeforeEach
    void setUp() {
        LessonMapper lessonMapper = new LessonMapper();
        SectionMapper sectionMapper = new SectionMapper(lessonMapper);
        courseMapper = new CourseMapper(sectionMapper);

        instructor = new User();
        instructor.setId(1L);
        instructor.setFirstName("Prof");
        instructor.setLastName("Diop");
        instructor.setEmail("prof@sencours.sn");
        instructor.setRole(Role.INSTRUCTEUR);

        category = new Category();
        category.setId(1L);
        category.setName("Développement Web");
    }

    @Test
    @DisplayName("Devrait convertir Entity vers Response avec instructor et category")
    void shouldConvertEntityToResponse() {
        Course entity = new Course();
        entity.setId(1L);
        entity.setTitle("Java pour débutants");
        entity.setDescription("Apprenez Java");
        entity.setPrice(new BigDecimal("25000"));
        entity.setThumbnailUrl("http://example.com/thumb.jpg");
        entity.setStatus(Status.PUBLISHED);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        entity.setInstructor(instructor);
        entity.setCategory(category);

        CourseResponse response = courseMapper.toResponse(entity);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getTitle()).isEqualTo("Java pour débutants");
        assertThat(response.getDescription()).isEqualTo("Apprenez Java");
        assertThat(response.getPrice()).isEqualByComparingTo(new BigDecimal("25000"));
        assertThat(response.getStatus()).isEqualTo(Status.PUBLISHED);
        assertThat(response.getInstructorId()).isEqualTo(1L);
        assertThat(response.getInstructorFirstName()).isEqualTo("Prof");
        assertThat(response.getInstructorLastName()).isEqualTo("Diop");
        assertThat(response.getCategoryId()).isEqualTo(1L);
        assertThat(response.getCategoryName()).isEqualTo("Développement Web");
    }

    @Test
    @DisplayName("Devrait retourner null si entity est null")
    void shouldReturnNullWhenEntityIsNull() {
        CourseResponse response = courseMapper.toResponse(null);
        assertThat(response).isNull();
    }

    @Test
    @DisplayName("Devrait convertir Request vers Entity")
    void shouldConvertRequestToEntity() {
        CourseRequest request = CourseRequest.builder()
                .title("Python avancé")
                .description("Maîtrisez Python")
                .price(new BigDecimal("30000"))
                .thumbnailUrl("http://example.com/python.jpg")
                .instructorId(1L)
                .categoryId(1L)
                .build();

        Course entity = courseMapper.toEntity(request, instructor, category);

        assertThat(entity).isNotNull();
        assertThat(entity.getId()).isNull();
        assertThat(entity.getTitle()).isEqualTo("Python avancé");
        assertThat(entity.getDescription()).isEqualTo("Maîtrisez Python");
        assertThat(entity.getPrice()).isEqualByComparingTo(new BigDecimal("30000"));
        assertThat(entity.getInstructor()).isEqualTo(instructor);
        assertThat(entity.getCategory()).isEqualTo(category);
    }

    @Test
    @DisplayName("Devrait retourner null si request est null")
    void shouldReturnNullWhenRequestIsNull() {
        Course entity = courseMapper.toEntity(null, instructor, category);
        assertThat(entity).isNull();
    }

    @Test
    @DisplayName("Devrait mettre à jour entity depuis request")
    void shouldUpdateEntityFromRequest() {
        Course entity = new Course();
        entity.setId(1L);
        entity.setTitle("Ancien titre");
        entity.setDescription("Ancienne description");
        entity.setPrice(new BigDecimal("10000"));
        entity.setInstructor(instructor);
        entity.setCategory(category);

        User newInstructor = new User();
        newInstructor.setId(2L);
        newInstructor.setFirstName("Nouveau");
        newInstructor.setLastName("Prof");

        Category newCategory = new Category();
        newCategory.setId(2L);
        newCategory.setName("Design");

        CourseRequest request = CourseRequest.builder()
                .title("Nouveau titre")
                .description("Nouvelle description")
                .price(new BigDecimal("50000"))
                .instructorId(2L)
                .categoryId(2L)
                .build();

        courseMapper.updateEntityFromRequest(request, entity, newInstructor, newCategory);

        assertThat(entity.getId()).isEqualTo(1L);
        assertThat(entity.getTitle()).isEqualTo("Nouveau titre");
        assertThat(entity.getDescription()).isEqualTo("Nouvelle description");
        assertThat(entity.getPrice()).isEqualByComparingTo(new BigDecimal("50000"));
        assertThat(entity.getInstructor()).isEqualTo(newInstructor);
        assertThat(entity.getCategory()).isEqualTo(newCategory);
    }

    @Test
    @DisplayName("Ne devrait rien faire si request ou entity est null")
    void shouldDoNothingWhenRequestOrEntityIsNull() {
        Course entity = new Course();
        entity.setTitle("Original");

        courseMapper.updateEntityFromRequest(null, entity, instructor, category);
        assertThat(entity.getTitle()).isEqualTo("Original");

        courseMapper.updateEntityFromRequest(CourseRequest.builder().title("Test").build(), null, instructor, category);
    }
}
