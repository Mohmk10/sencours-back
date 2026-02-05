package com.sencours.repository;

import com.sencours.entity.Course;
import com.sencours.enums.Status;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {

    List<Course> findByInstructorId(Long instructorId);

    List<Course> findByCategoryId(Long categoryId);

    List<Course> findByStatus(Status status);

    List<Course> findByTitleContainingIgnoreCase(String title);

    List<Course> findByStatusAndCategoryId(Status status, Long categoryId);

    // Pagination methods
    Page<Course> findByTitleContainingIgnoreCase(String title, Pageable pageable);

    Page<Course> findByCategoryId(Long categoryId, Pageable pageable);

    Page<Course> findByStatus(Status status, Pageable pageable);

    Page<Course> findByTitleContainingIgnoreCaseAndStatus(String title, Status status, Pageable pageable);

    Page<Course> findByCategoryIdAndStatus(Long categoryId, Status status, Pageable pageable);

    Page<Course> findByInstructorId(Long instructorId, Pageable pageable);
}
