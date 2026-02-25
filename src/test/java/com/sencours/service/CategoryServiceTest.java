package com.sencours.service;

import com.sencours.dto.request.CategoryRequest;
import com.sencours.dto.response.CategoryResponse;
import com.sencours.entity.Category;
import com.sencours.exception.ResourceAlreadyExistsException;
import com.sencours.exception.ResourceNotFoundException;
import com.sencours.mapper.CategoryMapper;
import com.sencours.repository.CategoryRepository;
import com.sencours.repository.CourseRepository;
import com.sencours.service.impl.CategoryServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private CategoryMapper categoryMapper;

    @InjectMocks
    private CategoryServiceImpl categoryService;

    private CategoryRequest categoryRequest;
    private Category category;
    private CategoryResponse categoryResponse;

    @BeforeEach
    void setUp() {
        categoryRequest = CategoryRequest.builder()
                .name("Développement Web")
                .description("Cours de développement web")
                .build();

        category = new Category();
        category.setId(1L);
        category.setName("Développement Web");
        category.setDescription("Cours de développement web");

        categoryResponse = CategoryResponse.builder()
                .id(1L)
                .name("Développement Web")
                .description("Cours de développement web")
                .build();
    }

    @Nested
    @DisplayName("Tests pour create()")
    class CreateTests {

        @Test
        @DisplayName("Devrait créer une catégorie avec succès")
        void shouldCreateCategorySuccessfully() {
            when(categoryRepository.existsByName(categoryRequest.getName())).thenReturn(false);
            when(categoryMapper.toEntity(categoryRequest)).thenReturn(category);
            when(categoryRepository.save(any(Category.class))).thenReturn(category);
            when(categoryMapper.toResponse(category)).thenReturn(categoryResponse);

            CategoryResponse result = categoryService.create(categoryRequest);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getName()).isEqualTo("Développement Web");

            verify(categoryRepository).existsByName(categoryRequest.getName());
            verify(categoryRepository).save(any(Category.class));
        }

        @Test
        @DisplayName("Devrait lever une exception si le nom existe déjà")
        void shouldThrowExceptionWhenNameAlreadyExists() {
            when(categoryRepository.existsByName(categoryRequest.getName())).thenReturn(true);

            assertThatThrownBy(() -> categoryService.create(categoryRequest))
                    .isInstanceOf(ResourceAlreadyExistsException.class)
                    .hasMessageContaining("Catégorie existe déjà avec nom : 'Développement Web'");

            verify(categoryRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Tests pour getById()")
    class GetByIdTests {

        @Test
        @DisplayName("Devrait retourner une catégorie par ID")
        void shouldReturnCategoryById() {
            when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
            when(categoryMapper.toResponse(category)).thenReturn(categoryResponse);

            CategoryResponse result = categoryService.getById(1L);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("Devrait lever une exception si ID non trouvé")
        void shouldThrowExceptionWhenIdNotFound() {
            when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> categoryService.getById(999L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Catégorie non trouvé avec id : '999'");
        }
    }

    @Nested
    @DisplayName("Tests pour getAll()")
    class GetAllTests {

        @Test
        @DisplayName("Devrait retourner toutes les catégories")
        void shouldReturnAllCategories() {
            Category category2 = new Category();
            category2.setId(2L);
            category2.setName("Design");

            CategoryResponse response2 = CategoryResponse.builder()
                    .id(2L)
                    .name("Design")
                    .build();

            when(categoryRepository.findAll()).thenReturn(Arrays.asList(category, category2));
            when(categoryMapper.toResponse(category)).thenReturn(categoryResponse);
            when(categoryMapper.toResponse(category2)).thenReturn(response2);

            List<CategoryResponse> result = categoryService.getAll();

            assertThat(result).hasSize(2);
            assertThat(result.get(0).getName()).isEqualTo("Développement Web");
            assertThat(result.get(1).getName()).isEqualTo("Design");
        }

        @Test
        @DisplayName("Devrait retourner une liste vide si aucune catégorie")
        void shouldReturnEmptyListWhenNoCategories() {
            when(categoryRepository.findAll()).thenReturn(List.of());

            List<CategoryResponse> result = categoryService.getAll();

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("Tests pour update()")
    class UpdateTests {

        @Test
        @DisplayName("Devrait mettre à jour une catégorie avec succès")
        void shouldUpdateCategorySuccessfully() {
            CategoryRequest updateRequest = CategoryRequest.builder()
                    .name("Développement Web Avancé")
                    .description("Description mise à jour")
                    .build();

            when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
            when(categoryRepository.existsByName("Développement Web Avancé")).thenReturn(false);
            when(categoryRepository.save(any(Category.class))).thenReturn(category);
            when(categoryMapper.toResponse(category)).thenReturn(categoryResponse);

            CategoryResponse result = categoryService.update(1L, updateRequest);

            assertThat(result).isNotNull();
            verify(categoryMapper).updateEntityFromRequest(updateRequest, category);
            verify(categoryRepository).save(category);
        }

        @Test
        @DisplayName("Devrait lever une exception si ID non trouvé lors de update")
        void shouldThrowExceptionWhenIdNotFoundOnUpdate() {
            when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> categoryService.update(999L, categoryRequest))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(categoryRepository, never()).save(any());
        }

        @Test
        @DisplayName("Devrait permettre de garder le même nom lors de update")
        void shouldAllowSameNameOnUpdate() {
            when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
            when(categoryRepository.save(any(Category.class))).thenReturn(category);
            when(categoryMapper.toResponse(category)).thenReturn(categoryResponse);

            CategoryResponse result = categoryService.update(1L, categoryRequest);

            assertThat(result).isNotNull();
            verify(categoryRepository, never()).existsByName(anyString());
        }

        @Test
        @DisplayName("Devrait lever une exception si nouveau nom existe déjà")
        void shouldThrowExceptionWhenNewNameAlreadyExists() {
            CategoryRequest updateRequest = CategoryRequest.builder()
                    .name("Design")
                    .description("Description")
                    .build();

            when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
            when(categoryRepository.existsByName("Design")).thenReturn(true);

            assertThatThrownBy(() -> categoryService.update(1L, updateRequest))
                    .isInstanceOf(ResourceAlreadyExistsException.class);

            verify(categoryRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Tests pour delete()")
    class DeleteTests {

        @Test
        @DisplayName("Devrait supprimer une catégorie avec succès")
        void shouldDeleteCategorySuccessfully() {
            when(categoryRepository.existsById(1L)).thenReturn(true);
            when(courseRepository.findByCategoryId(1L)).thenReturn(List.of());
            doNothing().when(categoryRepository).deleteById(1L);

            categoryService.delete(1L);

            verify(categoryRepository).deleteById(1L);
        }

        @Test
        @DisplayName("Devrait lever une exception si ID non trouvé lors de delete")
        void shouldThrowExceptionWhenIdNotFoundOnDelete() {
            when(categoryRepository.existsById(999L)).thenReturn(false);

            assertThatThrownBy(() -> categoryService.delete(999L))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(categoryRepository, never()).deleteById(anyLong());
        }
    }

    @Nested
    @DisplayName("Tests pour existsByName()")
    class ExistsByNameTests {

        @Test
        @DisplayName("Devrait retourner true si le nom existe")
        void shouldReturnTrueWhenNameExists() {
            when(categoryRepository.existsByName("Développement Web")).thenReturn(true);

            boolean result = categoryService.existsByName("Développement Web");

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Devrait retourner false si le nom n'existe pas")
        void shouldReturnFalseWhenNameDoesNotExist() {
            when(categoryRepository.existsByName("Inexistant")).thenReturn(false);

            boolean result = categoryService.existsByName("Inexistant");

            assertThat(result).isFalse();
        }
    }
}
