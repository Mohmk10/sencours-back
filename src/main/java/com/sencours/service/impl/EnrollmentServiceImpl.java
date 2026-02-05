package com.sencours.service.impl;

import com.sencours.dto.request.EnrollmentRequest;
import com.sencours.dto.response.EnrollmentDetailResponse;
import com.sencours.dto.response.EnrollmentResponse;
import com.sencours.dto.response.ProgressResponse;
import com.sencours.dto.response.ProgressSummaryResponse;
import com.sencours.entity.Course;
import com.sencours.entity.Enrollment;
import com.sencours.entity.Lesson;
import com.sencours.entity.Progress;
import com.sencours.entity.User;
import com.sencours.enums.Role;
import com.sencours.exception.AlreadyEnrolledException;
import com.sencours.exception.EnrollmentNotFoundException;
import com.sencours.exception.ResourceNotFoundException;
import com.sencours.exception.UnauthorizedReviewAccessException;
import com.sencours.mapper.EnrollmentMapper;
import com.sencours.mapper.ProgressMapper;
import com.sencours.repository.CourseRepository;
import com.sencours.repository.EnrollmentRepository;
import com.sencours.repository.LessonRepository;
import com.sencours.repository.ProgressRepository;
import com.sencours.repository.UserRepository;
import com.sencours.service.EnrollmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class EnrollmentServiceImpl implements EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final LessonRepository lessonRepository;
    private final ProgressRepository progressRepository;
    private final EnrollmentMapper enrollmentMapper;
    private final ProgressMapper progressMapper;

    @Override
    public EnrollmentResponse enroll(Long userId, EnrollmentRequest request) {
        log.info("Inscription de l'utilisateur {} au cours {}", userId, request.getCourseId());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur", "id", userId));

        Course course = courseRepository.findById(request.getCourseId())
                .orElseThrow(() -> new ResourceNotFoundException("Cours", "id", request.getCourseId()));

        if (user.getRole() != Role.ETUDIANT) {
            throw new IllegalArgumentException("Seul un utilisateur avec le rôle ETUDIANT peut s'inscrire à un cours");
        }

        if (course.getInstructor().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Un instructeur ne peut pas s'inscrire à son propre cours");
        }

        if (enrollmentRepository.existsByStudentIdAndCourseId(userId, request.getCourseId())) {
            throw new AlreadyEnrolledException(userId, request.getCourseId());
        }

        Enrollment enrollment = new Enrollment();
        enrollment.setStudent(user);
        enrollment.setCourse(course);

        Enrollment savedEnrollment = enrollmentRepository.save(enrollment);

        List<Lesson> lessons = lessonRepository.findByCourseIdOrderByOrderIndex(course.getId());
        for (Lesson lesson : lessons) {
            Progress progress = new Progress();
            progress.setEnrollment(savedEnrollment);
            progress.setLesson(lesson);
            progress.setCompleted(false);
            progressRepository.save(progress);
        }

        log.info("Inscription créée avec succès. ID: {}, {} leçons initialisées", savedEnrollment.getId(), lessons.size());

        double progressPercentage = lessons.isEmpty() ? 100.0 : 0.0;
        return enrollmentMapper.toResponse(savedEnrollment, progressPercentage);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EnrollmentResponse> getMyEnrollments(Long userId) {
        log.debug("Récupération des inscriptions de l'utilisateur {}", userId);

        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("Utilisateur", "id", userId);
        }

        return enrollmentRepository.findByStudentId(userId).stream()
                .map(enrollment -> {
                    double percentage = calculateProgressPercentage(enrollment.getId());
                    return enrollmentMapper.toResponse(enrollment, percentage);
                })
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public EnrollmentDetailResponse getEnrollmentDetail(Long enrollmentId) {
        log.debug("Récupération du détail de l'inscription {}", enrollmentId);

        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new EnrollmentNotFoundException(enrollmentId));

        double percentage = calculateProgressPercentage(enrollmentId);

        List<ProgressResponse> progresses = progressRepository.findByEnrollmentId(enrollmentId).stream()
                .map(progressMapper::toResponse)
                .toList();

        return enrollmentMapper.toDetailResponse(enrollment, percentage, progresses);
    }

    @Override
    @Transactional(readOnly = true)
    public ProgressSummaryResponse calculateProgress(Long enrollmentId) {
        log.debug("Calcul de la progression pour l'inscription {}", enrollmentId);

        if (!enrollmentRepository.existsById(enrollmentId)) {
            throw new EnrollmentNotFoundException(enrollmentId);
        }

        int totalLessons = progressRepository.countByEnrollmentId(enrollmentId);
        int completedLessons = progressRepository.countByEnrollmentIdAndCompletedTrue(enrollmentId);

        double percentage = totalLessons == 0 ? 100.0 : (double) completedLessons / totalLessons * 100;

        return ProgressSummaryResponse.builder()
                .totalLessons(totalLessons)
                .completedLessons(completedLessons)
                .percentage(Math.round(percentage * 100.0) / 100.0)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<EnrollmentResponse> getEnrollmentsByCourse(Long courseId) {
        log.debug("Récupération des inscriptions pour le cours {}", courseId);

        if (!courseRepository.existsById(courseId)) {
            throw new ResourceNotFoundException("Cours", "id", courseId);
        }

        return enrollmentRepository.findByCourseId(courseId).stream()
                .map(enrollment -> {
                    double percentage = calculateProgressPercentage(enrollment.getId());
                    return enrollmentMapper.toResponse(enrollment, percentage);
                })
                .toList();
    }

    @Override
    public void unenroll(Long enrollmentId, Long userId) {
        log.info("Désinscription de l'inscription {} par l'utilisateur {}", enrollmentId, userId);

        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new EnrollmentNotFoundException(enrollmentId));

        if (!enrollment.getStudent().getId().equals(userId)) {
            throw new UnauthorizedReviewAccessException("Vous ne pouvez vous désinscrire que de vos propres inscriptions");
        }

        enrollmentRepository.delete(enrollment);

        log.info("Désinscription effectuée avec succès. ID: {}", enrollmentId);
    }

    private double calculateProgressPercentage(Long enrollmentId) {
        int totalLessons = progressRepository.countByEnrollmentId(enrollmentId);
        if (totalLessons == 0) {
            return 100.0;
        }
        int completedLessons = progressRepository.countByEnrollmentIdAndCompletedTrue(enrollmentId);
        return Math.round((double) completedLessons / totalLessons * 10000.0) / 100.0;
    }
}
