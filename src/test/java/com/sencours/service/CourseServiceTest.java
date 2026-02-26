package com.sencours.service;

import com.sencours.dto.request.CourseRequest;
import com.sencours.dto.response.CourseResponse;
import com.sencours.entity.Category;
import com.sencours.entity.Course;
import com.sencours.entity.User;
import com.sencours.enums.Role;
import com.sencours.enums.Status;
import com.sencours.exception.InstructorNotFoundException;
import com.sencours.exception.InvalidInstructorRoleException;
import com.sencours.exception.ResourceNotFoundException;
import com.sencours.mapper.CourseMapper;
import com.sencours.repository.CategoryRepository;
import com.sencours.repository.CourseRepository;
import com.sencours.repository.UserRepository;
import com.sencours.service.impl.CourseServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CourseServiceTest {

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private CourseMapper courseMapper;

    @InjectMocks
    private CourseServiceImpl courseService;

    private CourseRequest courseRequest;
    private Course course;
    private CourseResponse courseResponse;
    private User instructor;
    private Category category;

    @BeforeEach
    void setUp() {
        instructor = new User();
        instructor.setId(1L);
        instructor.setFirstName("Prof");
        instructor.setLastName("Diop");
        instructor.setEmail("prof@sencours.sn");
        instructor.setRole(Role.INSTRUCTEUR);

        category = new Category();
        category.setId(1L);
        category.setName("Développement Web");

        courseRequest = CourseRequest.builder()
                .title("Java pour débutants")
                .description("Apprenez Java de zéro")
                .price(new BigDecimal("25000"))
                .instructorId(1L)
                .categoryId(1L)
                .build();

        course = new Course();
        course.setId(1L);
        course.setTitle("Java pour débutants");
        course.setDescription("Apprenez Java de zéro");
        course.setPrice(new BigDecimal("25000"));
        course.setStatus(Status.DRAFT);
        course.setInstructor(instructor);
        course.setCategory(category);
        course.setCreatedAt(LocalDateTime.now());
        course.setUpdatedAt(LocalDateTime.now());

        courseResponse = CourseResponse.builder()
                .id(1L)
                .title("Java pour débutants")
                .description("Apprenez Java de zéro")
                .price(new BigDecimal("25000"))
                .status(Status.DRAFT)
                .instructorId(1L)
                .instructorFirstName("Prof")
                .instructorLastName("Diop")
                .instructorName("Prof Diop")
                .categoryId(1L)
                .categoryName("Développement Web")
                .totalStudents(0)
                .averageRating(0.0)
                .build();
    }

    @Nested
    @DisplayName("Tests pour create()")
    class CreateTests {

        @Test
        @DisplayName("Devrait créer un cours avec succès")
        void shouldCreateCourseSuccessfully() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(instructor));
            when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
            when(courseMapper.toEntity(courseRequest, instructor, category)).thenReturn(course);
            when(courseRepository.save(any(Course.class))).thenReturn(course);
            when(courseMapper.toResponse(course)).thenReturn(courseResponse);

            CourseResponse result = courseService.create(courseRequest);

            assertThat(result).isNotNull();
            assertThat(result.getTitle()).isEqualTo("Java pour débutants");
            assertThat(result.getStatus()).isEqualTo(Status.DRAFT);
            verify(courseRepository).save(any(Course.class));
        }

        @Test
        @DisplayName("Devrait lever InstructorNotFoundException si instructeur non trouvé")
        void shouldThrowExceptionWhenInstructorNotFound() {
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            courseRequest.setInstructorId(999L);

            assertThatThrownBy(() -> courseService.create(courseRequest))
                    .isInstanceOf(InstructorNotFoundException.class)
                    .hasMessageContaining("999");

            verify(courseRepository, never()).save(any());
        }

        @Test
        @DisplayName("Devrait lever InvalidInstructorRoleException si utilisateur n'est pas instructeur")
        void shouldThrowExceptionWhenUserIsNotInstructor() {
            User student = new User();
            student.setId(2L);
            student.setRole(Role.ETUDIANT);

            when(userRepository.findById(2L)).thenReturn(Optional.of(student));

            courseRequest.setInstructorId(2L);

            assertThatThrownBy(() -> courseService.create(courseRequest))
                    .isInstanceOf(InvalidInstructorRoleException.class)
                    .hasMessageContaining("INSTRUCTEUR");

            verify(courseRepository, never()).save(any());
        }

        @Test
        @DisplayName("Devrait lever ResourceNotFoundException si catégorie non trouvée")
        void shouldThrowExceptionWhenCategoryNotFound() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(instructor));
            when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

            courseRequest.setCategoryId(999L);

            assertThatThrownBy(() -> courseService.create(courseRequest))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Catégorie");

            verify(courseRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Tests pour getById()")
    class GetByIdTests {

        @Test
        @DisplayName("Devrait retourner un cours par ID")
        void shouldReturnCourseById() {
            when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
            when(courseMapper.toResponse(course)).thenReturn(courseResponse);

            CourseResponse result = courseService.getById(1L);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("Devrait lever ResourceNotFoundException si cours non trouvé")
        void shouldThrowExceptionWhenCourseNotFound() {
            when(courseRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> courseService.getById(999L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("999");
        }
    }

    @Nested
    @DisplayName("Tests pour getAll()")
    class GetAllTests {

        @Test
        @DisplayName("Devrait retourner tous les cours")
        void shouldReturnAllCourses() {
            Course course2 = new Course();
            course2.setId(2L);
            course2.setTitle("Python avancé");

            CourseResponse response2 = CourseResponse.builder()
                    .id(2L)
                    .title("Python avancé")
                    .build();

            when(courseRepository.findAll()).thenReturn(Arrays.asList(course, course2));
            when(courseMapper.toResponse(course)).thenReturn(courseResponse);
            when(courseMapper.toResponse(course2)).thenReturn(response2);

            List<CourseResponse> result = courseService.getAll();

            assertThat(result).hasSize(2);
        }
    }

    @Nested
    @DisplayName("Tests pour getByInstructorId()")
    class GetByInstructorIdTests {

        @Test
        @DisplayName("Devrait retourner les cours d'un instructeur")
        void shouldReturnCoursesByInstructor() {
            when(userRepository.existsById(1L)).thenReturn(true);
            when(courseRepository.findByInstructorId(1L)).thenReturn(List.of(course));
            when(courseMapper.toResponse(course)).thenReturn(courseResponse);

            List<CourseResponse> result = courseService.getByInstructorId(1L);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getInstructorId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("Devrait lever exception si instructeur non trouvé")
        void shouldThrowExceptionWhenInstructorNotFound() {
            when(userRepository.existsById(999L)).thenReturn(false);

            assertThatThrownBy(() -> courseService.getByInstructorId(999L))
                    .isInstanceOf(InstructorNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Tests pour getByCategoryId()")
    class GetByCategoryIdTests {

        @Test
        @DisplayName("Devrait retourner les cours d'une catégorie")
        void shouldReturnCoursesByCategory() {
            when(categoryRepository.existsById(1L)).thenReturn(true);
            when(courseRepository.findByCategoryId(1L)).thenReturn(List.of(course));
            when(courseMapper.toResponse(course)).thenReturn(courseResponse);

            List<CourseResponse> result = courseService.getByCategoryId(1L);

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("Devrait lever exception si catégorie non trouvée")
        void shouldThrowExceptionWhenCategoryNotFound() {
            when(categoryRepository.existsById(999L)).thenReturn(false);

            assertThatThrownBy(() -> courseService.getByCategoryId(999L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Tests pour getByStatus()")
    class GetByStatusTests {

        @Test
        @DisplayName("Devrait retourner les cours par status")
        void shouldReturnCoursesByStatus() {
            when(courseRepository.findByStatus(Status.PUBLISHED)).thenReturn(List.of(course));
            when(courseMapper.toResponse(course)).thenReturn(courseResponse);

            List<CourseResponse> result = courseService.getByStatus(Status.PUBLISHED);

            assertThat(result).hasSize(1);
        }
    }

    @Nested
    @DisplayName("Tests pour update()")
    class UpdateTests {

        @Test
        @DisplayName("Devrait mettre à jour un cours")
        void shouldUpdateCourse() {
            when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
            when(courseRepository.save(any(Course.class))).thenReturn(course);
            when(courseMapper.toResponse(course)).thenReturn(courseResponse);

            CourseResponse result = courseService.update(1L, courseRequest);

            assertThat(result).isNotNull();
            verify(courseMapper).updateEntityFromRequest(eq(courseRequest), eq(course), any(), any());
        }

        @Test
        @DisplayName("Devrait lever exception si cours non trouvé")
        void shouldThrowExceptionWhenCourseNotFound() {
            when(courseRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> courseService.update(999L, courseRequest))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Tests pour delete()")
    class DeleteTests {

        @Test
        @DisplayName("Devrait supprimer un cours")
        void shouldDeleteCourse() {
            when(courseRepository.existsById(1L)).thenReturn(true);
            doNothing().when(courseRepository).deleteById(1L);

            courseService.delete(1L);

            verify(courseRepository).deleteById(1L);
        }

        @Test
        @DisplayName("Devrait lever exception si cours non trouvé")
        void shouldThrowExceptionWhenCourseNotFound() {
            when(courseRepository.existsById(999L)).thenReturn(false);

            assertThatThrownBy(() -> courseService.delete(999L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Tests pour publish() et archive()")
    class StatusChangeTests {

        @Test
        @DisplayName("Devrait publier un cours")
        void shouldPublishCourse() {
            when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
            when(courseRepository.save(any(Course.class))).thenReturn(course);
            when(courseMapper.toResponse(course)).thenReturn(courseResponse);

            CourseResponse result = courseService.publish(1L);

            assertThat(course.getStatus()).isEqualTo(Status.PUBLISHED);
            verify(courseRepository).save(course);
        }

        @Test
        @DisplayName("Devrait archiver un cours")
        void shouldArchiveCourse() {
            when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
            when(courseRepository.save(any(Course.class))).thenReturn(course);
            when(courseMapper.toResponse(course)).thenReturn(courseResponse);

            CourseResponse result = courseService.archive(1L);

            assertThat(course.getStatus()).isEqualTo(Status.ARCHIVED);
            verify(courseRepository).save(course);
        }
    }
}
