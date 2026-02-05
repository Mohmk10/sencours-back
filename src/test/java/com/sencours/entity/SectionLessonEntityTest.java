package com.sencours.entity;

import com.sencours.enums.LessonType;
import com.sencours.enums.Role;
import com.sencours.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class SectionLessonEntityTest {

    @Autowired
    private SectionRepository sectionRepository;

    @Autowired
    private LessonRepository lessonRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    private Course course;
    private Section section;

    @BeforeEach
    void setUp() {
        User instructor = new User();
        instructor.setFirstName("Prof");
        instructor.setLastName("Test");
        instructor.setEmail("prof@test.sn");
        instructor.setPassword("password");
        instructor.setRole(Role.INSTRUCTEUR);
        instructor = userRepository.save(instructor);

        Category category = new Category();
        category.setName("Test Category");
        category = categoryRepository.save(category);

        course = new Course();
        course.setTitle("Test Course");
        course.setInstructor(instructor);
        course.setCategory(category);
        course = courseRepository.save(course);

        section = new Section();
        section.setTitle("Introduction");
        section.setOrderIndex(1);
        section.setCourse(course);
    }

    @Test
    @DisplayName("Should save section with course relation")
    void shouldSaveSectionWithCourseRelation() {
        Section savedSection = sectionRepository.save(section);

        assertThat(savedSection.getId()).isNotNull();
        assertThat(savedSection.getCourse().getId()).isEqualTo(course.getId());
    }

    @Test
    @DisplayName("Should find sections by course ordered by index")
    void shouldFindSectionsByCourseOrderedByIndex() {
        sectionRepository.save(section);

        Section section2 = new Section();
        section2.setTitle("Chapitre 1");
        section2.setOrderIndex(2);
        section2.setCourse(course);
        sectionRepository.save(section2);

        var sections = sectionRepository.findByCourseIdOrderByOrderIndexAsc(course.getId());

        assertThat(sections).hasSize(2);
        assertThat(sections.get(0).getOrderIndex()).isEqualTo(1);
        assertThat(sections.get(1).getOrderIndex()).isEqualTo(2);
    }

    @Test
    @DisplayName("Should save lesson with section relation")
    void shouldSaveLessonWithSectionRelation() {
        Section savedSection = sectionRepository.save(section);

        Lesson lesson = new Lesson();
        lesson.setTitle("Leçon 1: Bienvenue");
        lesson.setType(LessonType.VIDEO);
        lesson.setContent("https://video.url");
        lesson.setDuration(600);
        lesson.setOrderIndex(1);
        lesson.setSection(savedSection);

        Lesson savedLesson = lessonRepository.save(lesson);

        assertThat(savedLesson.getId()).isNotNull();
        assertThat(savedLesson.getType()).isEqualTo(LessonType.VIDEO);
        assertThat(savedLesson.getIsFree()).isFalse();
    }

    @Test
    @DisplayName("Should find lessons by section ordered by index")
    void shouldFindLessonsBySectionOrderedByIndex() {
        Section savedSection = sectionRepository.save(section);

        Lesson lesson1 = new Lesson();
        lesson1.setTitle("Leçon 1");
        lesson1.setType(LessonType.TEXT);
        lesson1.setOrderIndex(1);
        lesson1.setSection(savedSection);
        lessonRepository.save(lesson1);

        Lesson lesson2 = new Lesson();
        lesson2.setTitle("Leçon 2");
        lesson2.setType(LessonType.VIDEO);
        lesson2.setOrderIndex(2);
        lesson2.setSection(savedSection);
        lessonRepository.save(lesson2);

        var lessons = lessonRepository.findBySectionIdOrderByOrderIndexAsc(savedSection.getId());

        assertThat(lessons).hasSize(2);
        assertThat(lessons.get(0).getTitle()).isEqualTo("Leçon 1");
    }

    @Test
    @DisplayName("Should find free lessons")
    void shouldFindFreeLessons() {
        Section savedSection = sectionRepository.save(section);

        Lesson freeLesson = new Lesson();
        freeLesson.setTitle("Preview gratuit");
        freeLesson.setType(LessonType.VIDEO);
        freeLesson.setOrderIndex(1);
        freeLesson.setIsFree(true);
        freeLesson.setSection(savedSection);
        lessonRepository.save(freeLesson);

        Lesson paidLesson = new Lesson();
        paidLesson.setTitle("Contenu payant");
        paidLesson.setType(LessonType.VIDEO);
        paidLesson.setOrderIndex(2);
        paidLesson.setSection(savedSection);
        lessonRepository.save(paidLesson);

        var freeLessons = lessonRepository.findByIsFreeTrue();

        assertThat(freeLessons).hasSize(1);
        assertThat(freeLessons.get(0).getTitle()).isEqualTo("Preview gratuit");
    }

    @Test
    @DisplayName("Should find lessons by type")
    void shouldFindLessonsByType() {
        Section savedSection = sectionRepository.save(section);

        Lesson videoLesson = new Lesson();
        videoLesson.setTitle("Video");
        videoLesson.setType(LessonType.VIDEO);
        videoLesson.setOrderIndex(1);
        videoLesson.setSection(savedSection);
        lessonRepository.save(videoLesson);

        Lesson textLesson = new Lesson();
        textLesson.setTitle("Text");
        textLesson.setType(LessonType.TEXT);
        textLesson.setOrderIndex(2);
        textLesson.setSection(savedSection);
        lessonRepository.save(textLesson);

        assertThat(lessonRepository.findByType(LessonType.VIDEO)).hasSize(1);
        assertThat(lessonRepository.findByType(LessonType.TEXT)).hasSize(1);
        assertThat(lessonRepository.findByType(LessonType.QUIZ)).isEmpty();
    }
}
