package com.sencours.entity;

import com.sencours.enums.Role;
import com.sencours.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
class ReviewEntityTest {

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    private User student;
    private Course course;
    private Review review;

    @BeforeEach
    void setUp() {
        User instructor = new User();
        instructor.setFirstName("Prof");
        instructor.setLastName("Test");
        instructor.setEmail("prof@test.sn");
        instructor.setPassword("password");
        instructor.setRole(Role.INSTRUCTEUR);
        instructor = userRepository.save(instructor);

        student = new User();
        student.setFirstName("Etudiant");
        student.setLastName("Test");
        student.setEmail("etudiant@test.sn");
        student.setPassword("password");
        student.setRole(Role.ETUDIANT);
        student = userRepository.save(student);

        Category category = new Category();
        category.setName("Test");
        category = categoryRepository.save(category);

        course = new Course();
        course.setTitle("Test Course");
        course.setInstructor(instructor);
        course.setCategory(category);
        course = courseRepository.save(course);

        review = new Review();
        review.setStudent(student);
        review.setCourse(course);
        review.setRating(5);
        review.setComment("Excellent cours!");
    }

    @Test
    @DisplayName("Should save review with timestamps")
    void shouldSaveReviewWithTimestamps() {
        Review savedReview = reviewRepository.save(review);

        assertThat(savedReview.getId()).isNotNull();
        assertThat(savedReview.getCreatedAt()).isNotNull();
        assertThat(savedReview.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should enforce unique student-course constraint")
    void shouldEnforceUniqueStudentCourseConstraint() {
        reviewRepository.save(review);

        Review duplicateReview = new Review();
        duplicateReview.setStudent(student);
        duplicateReview.setCourse(course);
        duplicateReview.setRating(4);

        assertThatThrownBy(() -> {
            reviewRepository.saveAndFlush(duplicateReview);
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Should find reviews by course")
    void shouldFindReviewsByCourse() {
        reviewRepository.save(review);

        var reviews = reviewRepository.findByCourseId(course.getId());

        assertThat(reviews).hasSize(1);
        assertThat(reviews.get(0).getRating()).isEqualTo(5);
    }

    @Test
    @DisplayName("Should find reviews by student")
    void shouldFindReviewsByStudent() {
        reviewRepository.save(review);

        var reviews = reviewRepository.findByStudentId(student.getId());

        assertThat(reviews).hasSize(1);
    }

    @Test
    @DisplayName("Should calculate average rating")
    void shouldCalculateAverageRating() {
        reviewRepository.save(review);

        User student2 = new User();
        student2.setFirstName("Student2");
        student2.setLastName("Test");
        student2.setEmail("student2@test.sn");
        student2.setPassword("password");
        student2 = userRepository.save(student2);

        Review review2 = new Review();
        review2.setStudent(student2);
        review2.setCourse(course);
        review2.setRating(3);
        reviewRepository.save(review2);

        Double avgRating = reviewRepository.getAverageRatingByCourseId(course.getId());

        assertThat(avgRating).isEqualTo(4.0);
    }

    @Test
    @DisplayName("Should count reviews by course")
    void shouldCountReviewsByCourse() {
        reviewRepository.save(review);

        int count = reviewRepository.countByCourseId(course.getId());

        assertThat(count).isEqualTo(1);
    }

    @Test
    @DisplayName("Should find review by student and course")
    void shouldFindReviewByStudentAndCourse() {
        reviewRepository.save(review);

        var foundReview = reviewRepository.findByStudentIdAndCourseId(
                student.getId(), course.getId());

        assertThat(foundReview).isPresent();
        assertThat(foundReview.get().getComment()).isEqualTo("Excellent cours!");
    }

    @Test
    @DisplayName("Should check if review exists")
    void shouldCheckIfReviewExists() {
        reviewRepository.save(review);

        assertThat(reviewRepository.existsByStudentIdAndCourseId(
                student.getId(), course.getId())).isTrue();
        assertThat(reviewRepository.existsByStudentIdAndCourseId(
                student.getId(), 999L)).isFalse();
    }
}
