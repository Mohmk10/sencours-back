package com.sencours.repository;

import com.sencours.entity.Progress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProgressRepository extends JpaRepository<Progress, Long> {

    List<Progress> findByEnrollmentId(Long enrollmentId);

    Optional<Progress> findByEnrollmentIdAndLessonId(Long enrollmentId, Long lessonId);

    boolean existsByEnrollmentIdAndLessonId(Long enrollmentId, Long lessonId);

    int countByEnrollmentIdAndCompletedTrue(Long enrollmentId);

    int countByEnrollmentId(Long enrollmentId);
}
