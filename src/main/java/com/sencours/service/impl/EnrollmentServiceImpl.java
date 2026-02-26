package com.sencours.service.impl;

import com.sencours.dto.request.EnrollmentRequest;
import com.sencours.dto.response.EnrollmentResponse;
import com.sencours.dto.response.PaymentResponse;
import com.sencours.entity.*;
import com.sencours.exception.BadRequestException;
import com.sencours.exception.ResourceNotFoundException;
import com.sencours.repository.*;
import com.sencours.service.EnrollmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EnrollmentServiceImpl implements EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final ProgressRepository progressRepository;

    @Override
    @Transactional
    public PaymentResponse initiatePayment(Long courseId, EnrollmentRequest request, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Cours non trouvé"));

        if (enrollmentRepository.existsByUserIdAndCourseId(user.getId(), courseId)) {
            throw new BadRequestException("Vous êtes déjà inscrit à ce cours");
        }

        String reference = "PAY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        return PaymentResponse.builder()
                .reference(reference)
                .status("SUCCESS")
                .message("Paiement " + request.getPaymentMethod() + " simulé avec succès")
                .amount(course.getPrice())
                .method(request.getPaymentMethod())
                .build();
    }

    @Override
    @Transactional
    public EnrollmentResponse completeEnrollment(Long courseId, String paymentReference, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Cours non trouvé"));

        if (enrollmentRepository.existsByUserIdAndCourseId(user.getId(), courseId)) {
            throw new BadRequestException("Vous êtes déjà inscrit à ce cours");
        }

        Enrollment enrollment = Enrollment.builder()
                .user(user)
                .course(course)
                .paymentReference(paymentReference)
                .amountPaid(course.getPrice())
                .build();

        enrollment = enrollmentRepository.save(enrollment);

        return mapToResponse(enrollment);
    }

    @Override
    @Transactional
    public EnrollmentResponse enrollFree(Long courseId, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Cours non trouvé"));

        if (course.getPrice().compareTo(BigDecimal.ZERO) > 0) {
            throw new BadRequestException("Ce cours n'est pas gratuit");
        }

        if (enrollmentRepository.existsByUserIdAndCourseId(user.getId(), courseId)) {
            throw new BadRequestException("Vous êtes déjà inscrit à ce cours");
        }

        Enrollment enrollment = Enrollment.builder()
                .user(user)
                .course(course)
                .amountPaid(BigDecimal.ZERO)
                .build();

        enrollment = enrollmentRepository.save(enrollment);

        return mapToResponse(enrollment);
    }

    @Override
    public boolean isEnrolled(Long courseId, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));

        return enrollmentRepository.existsByUserIdAndCourseId(user.getId(), courseId);
    }

    @Override
    public List<EnrollmentResponse> getMyEnrollments(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));

        return enrollmentRepository.findByUserIdOrderByEnrolledAtDesc(user.getId())
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public EnrollmentResponse getEnrollment(Long courseId, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));

        Enrollment enrollment = enrollmentRepository.findByUserIdAndCourseId(user.getId(), courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Inscription non trouvée"));

        return mapToResponse(enrollment);
    }

    @Override
    @Transactional
    public void updateProgress(Long courseId, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));

        Enrollment enrollment = enrollmentRepository.findByUserIdAndCourseId(user.getId(), courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Inscription non trouvée"));

        Course course = enrollment.getCourse();
        int totalLessons = 0;
        for (Section section : course.getSections()) {
            totalLessons += section.getLessons().size();
        }

        Long completedLessons = progressRepository.countCompletedLessonsByUserAndCourse(user.getId(), courseId);

        int percentage = totalLessons > 0 ? (int) ((completedLessons * 100) / totalLessons) : 0;
        enrollment.setProgressPercentage(percentage);

        if (percentage >= 100 && enrollment.getCompletedAt() == null) {
            enrollment.setCompletedAt(LocalDateTime.now());
        }

        enrollmentRepository.save(enrollment);
    }

    private EnrollmentResponse mapToResponse(Enrollment enrollment) {
        Course course = enrollment.getCourse();

        int totalLessons = 0;
        for (Section section : course.getSections()) {
            totalLessons += section.getLessons().size();
        }

        Long completedLessons = progressRepository.countCompletedLessonsByUserAndCourse(
                enrollment.getUser().getId(), course.getId());

        // Calculer le pourcentage en temps réel au lieu de lire la valeur stockée
        int progressPercentage = totalLessons > 0 ? (int) ((completedLessons * 100) / totalLessons) : 0;

        return EnrollmentResponse.builder()
                .id(enrollment.getId())
                .courseId(course.getId())
                .courseTitle(course.getTitle())
                .courseThumbnail(course.getThumbnailUrl())
                .userId(enrollment.getUser().getId())
                .userName(enrollment.getUser().getFirstName() + " " + enrollment.getUser().getLastName())
                .enrolledAt(enrollment.getEnrolledAt())
                .completedAt(enrollment.getCompletedAt())
                .progressPercentage(progressPercentage)
                .paymentReference(enrollment.getPaymentReference())
                .paymentMethod(enrollment.getPaymentMethod())
                .amountPaid(enrollment.getAmountPaid())
                .totalLessons(totalLessons)
                .completedLessons(completedLessons.intValue())
                .build();
    }
}
