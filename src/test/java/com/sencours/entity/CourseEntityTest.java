package com.sencours.entity;

import com.sencours.enums.Role;
import com.sencours.enums.Status;
import com.sencours.repository.CategoryRepository;
import com.sencours.repository.CourseRepository;
import com.sencours.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class CourseEntityTest {

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    private User instructor;
    private Category category;
    private Course course;

    @BeforeEach
    void setUp() {
        instructor = new User();
        instructor.setFirstName("Prof");
        instructor.setLastName("Diop");
        instructor.setEmail("prof.diop@sencours.sn");
        instructor.setPassword("password");
        instructor.setRole(Role.INSTRUCTEUR);
        instructor = userRepository.save(instructor);

        category = new Category();
        category.setName("Programmation");
        category.setDescription("Cours de programmation");
        category = categoryRepository.save(category);

        course = new Course();
        course.setTitle("Java pour débutants");
        course.setDescription("Apprenez Java de zéro");
        course.setPrice(new BigDecimal("25000.00"));
        course.setInstructor(instructor);
        course.setCategory(category);
    }

    @Test
    @DisplayName("Should save course with default status DRAFT")
    void shouldSaveCourseWithDefaultStatusDraft() {
        Course savedCourse = courseRepository.save(course);

        assertThat(savedCourse.getId()).isNotNull();
        assertThat(savedCourse.getStatus()).isEqualTo(Status.DRAFT);
        assertThat(savedCourse.getCreatedAt()).isNotNull();
        assertThat(savedCourse.getPrice()).isEqualByComparingTo(new BigDecimal("25000.00"));
    }

    @Test
    @DisplayName("Should find courses by instructor")
    void shouldFindCoursesByInstructor() {
        courseRepository.save(course);

        var courses = courseRepository.findByInstructorId(instructor.getId());

        assertThat(courses).hasSize(1);
        assertThat(courses.get(0).getTitle()).isEqualTo("Java pour débutants");
    }

    @Test
    @DisplayName("Should find courses by category")
    void shouldFindCoursesByCategory() {
        courseRepository.save(course);

        var courses = courseRepository.findByCategoryId(category.getId());

        assertThat(courses).hasSize(1);
    }

    @Test
    @DisplayName("Should find courses by status")
    void shouldFindCoursesByStatus() {
        course.setStatus(Status.PUBLISHED);
        courseRepository.save(course);

        var publishedCourses = courseRepository.findByStatus(Status.PUBLISHED);
        var draftCourses = courseRepository.findByStatus(Status.DRAFT);

        assertThat(publishedCourses).hasSize(1);
        assertThat(draftCourses).isEmpty();
    }

    @Test
    @DisplayName("Should find courses by title containing")
    void shouldFindCoursesByTitleContaining() {
        courseRepository.save(course);

        var courses = courseRepository.findByTitleContainingIgnoreCase("java");

        assertThat(courses).hasSize(1);
    }

    @Test
    @DisplayName("Should maintain relation with instructor and category")
    void shouldMaintainRelations() {
        Course savedCourse = courseRepository.save(course);

        assertThat(savedCourse.getInstructor().getId()).isEqualTo(instructor.getId());
        assertThat(savedCourse.getCategory().getId()).isEqualTo(category.getId());
    }
}
