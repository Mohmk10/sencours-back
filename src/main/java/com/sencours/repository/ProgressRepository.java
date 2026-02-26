package com.sencours.repository;

import com.sencours.entity.Progress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProgressRepository extends JpaRepository<Progress, Long> {

    Optional<Progress> findByUserIdAndLessonId(Long userId, Long lessonId);

    List<Progress> findByUserId(Long userId);

    @Query("SELECT p FROM Progress p WHERE p.user.id = :userId AND p.lesson.section.course.id = :courseId")
    List<Progress> findByUserIdAndCourseId(@Param("userId") Long userId, @Param("courseId") Long courseId);

    @Query("SELECT COUNT(p) FROM Progress p WHERE p.user.id = :userId AND p.lesson.section.course.id = :courseId AND p.completed = true")
    Long countCompletedLessonsByUserAndCourse(@Param("userId") Long userId, @Param("courseId") Long courseId);
}
