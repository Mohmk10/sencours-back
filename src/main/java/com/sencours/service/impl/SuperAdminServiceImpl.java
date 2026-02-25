package com.sencours.service.impl;

import com.sencours.entity.User;
import com.sencours.enums.Role;
import com.sencours.exception.ForbiddenException;
import com.sencours.exception.ResourceNotFoundException;
import com.sencours.repository.*;
import com.sencours.service.SuperAdminService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SuperAdminServiceImpl implements SuperAdminService {

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final CourseRepository courseRepository;
    private final SectionRepository sectionRepository;
    private final LessonRepository lessonRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final ReviewRepository reviewRepository;
    private final ProgressRepository progressRepository;
    private final InstructorApplicationRepository instructorApplicationRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional
    public void resetDatabase(String superAdminEmail) {
        User superAdmin = userRepository.findByEmail(superAdminEmail)
                .orElseThrow(() -> new ResourceNotFoundException("SuperAdmin non trouvé"));

        if (superAdmin.getRole() != Role.SUPER_ADMIN) {
            throw new ForbiddenException("Seul le SuperAdmin peut réinitialiser la base de données");
        }

        // Supprimer dans l'ordre pour respecter les foreign keys
        progressRepository.deleteAll();
        reviewRepository.deleteAll();
        enrollmentRepository.deleteAll();
        lessonRepository.deleteAll();
        sectionRepository.deleteAll();
        courseRepository.deleteAll();
        instructorApplicationRepository.deleteAll();
        categoryRepository.deleteAll();

        // Supprimer tous les utilisateurs SAUF le SuperAdmin
        userRepository.deleteAllByIdNot(superAdmin.getId());

        // Reset des séquences
        resetSequences();
    }

    private void resetSequences() {
        entityManager.createNativeQuery("ALTER SEQUENCE IF EXISTS categories_id_seq RESTART WITH 1").executeUpdate();
        entityManager.createNativeQuery("ALTER SEQUENCE IF EXISTS courses_id_seq RESTART WITH 1").executeUpdate();
        entityManager.createNativeQuery("ALTER SEQUENCE IF EXISTS sections_id_seq RESTART WITH 1").executeUpdate();
        entityManager.createNativeQuery("ALTER SEQUENCE IF EXISTS lessons_id_seq RESTART WITH 1").executeUpdate();
        entityManager.createNativeQuery("ALTER SEQUENCE IF EXISTS enrollments_id_seq RESTART WITH 1").executeUpdate();
        entityManager.createNativeQuery("ALTER SEQUENCE IF EXISTS reviews_id_seq RESTART WITH 1").executeUpdate();
        entityManager.createNativeQuery("ALTER SEQUENCE IF EXISTS progress_id_seq RESTART WITH 1").executeUpdate();
        entityManager.createNativeQuery("ALTER SEQUENCE IF EXISTS instructor_applications_id_seq RESTART WITH 1").executeUpdate();
    }
}
