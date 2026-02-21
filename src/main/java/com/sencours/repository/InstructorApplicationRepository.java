package com.sencours.repository;

import com.sencours.entity.InstructorApplication;
import com.sencours.enums.ApplicationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InstructorApplicationRepository extends JpaRepository<InstructorApplication, Long> {

    boolean existsByUserIdAndStatus(Long userId, ApplicationStatus status);

    Optional<InstructorApplication> findFirstByUserIdOrderByCreatedAtDesc(Long userId);

    Page<InstructorApplication> findByStatus(ApplicationStatus status, Pageable pageable);

    Page<InstructorApplication> findAllByOrderByCreatedAtDesc(Pageable pageable);

    long countByStatus(ApplicationStatus status);
}
