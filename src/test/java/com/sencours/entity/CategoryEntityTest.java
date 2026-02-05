package com.sencours.entity;

import com.sencours.repository.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
class CategoryEntityTest {

    @Autowired
    private CategoryRepository categoryRepository;

    private Category category;

    @BeforeEach
    void setUp() {
        category = new Category();
        category.setName("Développement Web");
        category.setDescription("Cours de développement web");
    }

    @Test
    @DisplayName("Should save category successfully")
    void shouldSaveCategorySuccessfully() {
        Category savedCategory = categoryRepository.save(category);

        assertThat(savedCategory.getId()).isNotNull();
        assertThat(savedCategory.getName()).isEqualTo("Développement Web");
        assertThat(savedCategory.getDescription()).isEqualTo("Cours de développement web");
    }

    @Test
    @DisplayName("Should find category by name")
    void shouldFindCategoryByName() {
        categoryRepository.save(category);

        var foundCategory = categoryRepository.findByName("Développement Web");

        assertThat(foundCategory).isPresent();
        assertThat(foundCategory.get().getName()).isEqualTo("Développement Web");
    }

    @Test
    @DisplayName("Should enforce unique name constraint")
    void shouldEnforceUniqueNameConstraint() {
        categoryRepository.save(category);

        Category duplicateCategory = new Category();
        duplicateCategory.setName("Développement Web");
        duplicateCategory.setDescription("Autre description");

        assertThatThrownBy(() -> {
            categoryRepository.saveAndFlush(duplicateCategory);
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Should check if category exists by name")
    void shouldCheckIfCategoryExistsByName() {
        categoryRepository.save(category);

        assertThat(categoryRepository.existsByName("Développement Web")).isTrue();
        assertThat(categoryRepository.existsByName("Non existant")).isFalse();
    }
}
