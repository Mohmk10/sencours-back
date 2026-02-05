package com.sencours.mapper;

import com.sencours.dto.request.CategoryRequest;
import com.sencours.dto.response.CategoryResponse;
import com.sencours.entity.Category;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CategoryMapperTest {

    private CategoryMapper categoryMapper;

    @BeforeEach
    void setUp() {
        categoryMapper = new CategoryMapper();
    }

    @Test
    @DisplayName("Devrait convertir Entity vers Response")
    void shouldConvertEntityToResponse() {
        Category entity = new Category();
        entity.setId(1L);
        entity.setName("Développement Web");
        entity.setDescription("Description");

        CategoryResponse response = categoryMapper.toResponse(entity);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getName()).isEqualTo("Développement Web");
        assertThat(response.getDescription()).isEqualTo("Description");
    }

    @Test
    @DisplayName("Devrait retourner null si entity est null")
    void shouldReturnNullWhenEntityIsNull() {
        CategoryResponse response = categoryMapper.toResponse(null);

        assertThat(response).isNull();
    }

    @Test
    @DisplayName("Devrait convertir Request vers Entity")
    void shouldConvertRequestToEntity() {
        CategoryRequest request = CategoryRequest.builder()
                .name("Design")
                .description("Cours de design")
                .build();

        Category entity = categoryMapper.toEntity(request);

        assertThat(entity).isNotNull();
        assertThat(entity.getId()).isNull();
        assertThat(entity.getName()).isEqualTo("Design");
        assertThat(entity.getDescription()).isEqualTo("Cours de design");
    }

    @Test
    @DisplayName("Devrait retourner null si request est null")
    void shouldReturnNullWhenRequestIsNull() {
        Category entity = categoryMapper.toEntity(null);

        assertThat(entity).isNull();
    }

    @Test
    @DisplayName("Devrait mettre à jour entity depuis request")
    void shouldUpdateEntityFromRequest() {
        Category entity = new Category();
        entity.setId(1L);
        entity.setName("Ancien nom");
        entity.setDescription("Ancienne description");

        CategoryRequest request = CategoryRequest.builder()
                .name("Nouveau nom")
                .description("Nouvelle description")
                .build();

        categoryMapper.updateEntityFromRequest(request, entity);

        assertThat(entity.getId()).isEqualTo(1L);
        assertThat(entity.getName()).isEqualTo("Nouveau nom");
        assertThat(entity.getDescription()).isEqualTo("Nouvelle description");
    }

    @Test
    @DisplayName("Ne devrait rien faire si request ou entity est null")
    void shouldDoNothingWhenRequestOrEntityIsNull() {
        Category entity = new Category();
        entity.setName("Original");

        categoryMapper.updateEntityFromRequest(null, entity);
        assertThat(entity.getName()).isEqualTo("Original");

        categoryMapper.updateEntityFromRequest(CategoryRequest.builder().name("Test").build(), null);
    }
}
