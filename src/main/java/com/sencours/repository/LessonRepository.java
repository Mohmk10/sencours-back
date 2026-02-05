package com.sencours.repository;

import com.sencours.entity.Lesson;
import com.sencours.enums.LessonType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LessonRepository extends JpaRepository<Lesson, Long> {

    List<Lesson> findBySectionIdOrderByOrderIndexAsc(Long sectionId);

    List<Lesson> findByType(LessonType type);

    List<Lesson> findByIsFreeTrue();

    int countBySectionId(Long sectionId);

    @Query("SELECT l FROM Lesson l WHERE l.section.course.id = :courseId ORDER BY l.section.orderIndex, l.orderIndex")
    List<Lesson> findByCourseIdOrderByOrderIndex(@Param("courseId") Long courseId);
}
