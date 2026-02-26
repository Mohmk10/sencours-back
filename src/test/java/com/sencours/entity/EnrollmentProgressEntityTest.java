package com.sencours.entity;

import com.sencours.enums.LessonType;
import com.sencours.enums.Role;
import com.sencours.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
class EnrollmentProgressEntityTest {

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private ProgressRepository progressRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private SectionRepository sectionRepository;

    @Autowired
    private LessonRepository lessonRepository;

    private User student;
    private Course course;
    private Enrollment enrollment;
    private Lesson lesson;

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

        Section section = new Section();
        section.setTitle("Section 1");
        section.setOrderIndex(1);
        section.setCourse(course);
        section = sectionRepository.save(section);

        lesson = new Lesson();
        lesson.setTitle("Lesson 1");
        lesson.setType(LessonType.VIDEO);
        lesson.setOrderIndex(1);
        lesson.setSection(section);
        lesson = lessonRepository.save(lesson);

        enrollment = new Enrollment();
        enrollment.setUser(student);
        enrollment.setCourse(course);
    }

    @Test
    @DisplayName("Should save enrollment with timestamp")
    void shouldSaveEnrollmentWithTimestamp() {
        Enrollment savedEnrollment = enrollmentRepository.save(enrollment);

        assertThat(savedEnrollment.getId()).isNotNull();
        assertThat(savedEnrollment.getEnrolledAt()).isNotNull();
    }

    @Test
    @DisplayName("Should enforce unique user-course constraint")
    void shouldEnforceUniqueUserCourseConstraint() {
        enrollmentRepository.save(enrollment);

        Enrollment duplicateEnrollment = new Enrollment();
        duplicateEnrollment.setUser(student);
        duplicateEnrollment.setCourse(course);

        assertThatThrownBy(() -> {
            enrollmentRepository.saveAndFlush(duplicateEnrollment);
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Should find enrollments by user")
    void shouldFindEnrollmentsByUser() {
        enrollmentRepository.save(enrollment);

        var enrollments = enrollmentRepository.findByUserIdOrderByEnrolledAtDesc(student.getId());

        assertThat(enrollments).hasSize(1);
    }

    @Test
    @DisplayName("Should find enrollment by user and course")
    void shouldFindEnrollmentByUserAndCourse() {
        enrollmentRepository.save(enrollment);

        var foundEnrollment = enrollmentRepository.findByUserIdAndCourseId(
                student.getId(), course.getId());

        assertThat(foundEnrollment).isPresent();
    }

    @Test
    @DisplayName("Should check if enrollment exists")
    void shouldCheckIfEnrollmentExists() {
        enrollmentRepository.save(enrollment);

        assertThat(enrollmentRepository.existsByUserIdAndCourseId(
                student.getId(), course.getId())).isTrue();
        assertThat(enrollmentRepository.existsByUserIdAndCourseId(
                student.getId(), 999L)).isFalse();
    }

    @Test
    @DisplayName("Should save progress for user and lesson")
    void shouldSaveProgressForUserAndLesson() {
        Progress progress = new Progress();
        progress.setUser(student);
        progress.setLesson(lesson);
        progress.setCompleted(false);
        progress.setWatchTimeSeconds(0);
        progress.setLastPositionSeconds(0);

        Progress savedProgress = progressRepository.save(progress);

        assertThat(savedProgress.getId()).isNotNull();
        assertThat(savedProgress.getCompleted()).isFalse();
    }

    @Test
    @DisplayName("Should mark progress as completed")
    void shouldMarkProgressAsCompleted() {
        Progress progress = new Progress();
        progress.setUser(student);
        progress.setLesson(lesson);
        progress.setCompleted(true);
        progress.setCompletedAt(LocalDateTime.now());
        progress.setWatchTimeSeconds(600);
        progress.setLastPositionSeconds(600);

        Progress savedProgress = progressRepository.save(progress);

        assertThat(savedProgress.getCompleted()).isTrue();
        assertThat(savedProgress.getCompletedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should enforce unique user-lesson constraint in progress")
    void shouldEnforceUniqueUserLessonConstraint() {
        Progress progress1 = new Progress();
        progress1.setUser(student);
        progress1.setLesson(lesson);
        progressRepository.save(progress1);

        Progress progress2 = new Progress();
        progress2.setUser(student);
        progress2.setLesson(lesson);

        assertThatThrownBy(() -> {
            progressRepository.saveAndFlush(progress2);
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Should count completed progress by user and course")
    void shouldCountCompletedProgress() {
        Progress progress = new Progress();
        progress.setUser(student);
        progress.setLesson(lesson);
        progress.setCompleted(true);
        progressRepository.save(progress);

        Long completedCount = progressRepository.countCompletedLessonsByUserAndCourse(
                student.getId(), course.getId());

        assertThat(completedCount).isEqualTo(1L);
    }
}
