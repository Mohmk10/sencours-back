package com.sencours.service.impl;

import com.sencours.dto.request.ApplicationReviewRequest;
import com.sencours.dto.request.InstructorApplicationCreateRequest;
import com.sencours.dto.response.InstructorApplicationResponse;
import com.sencours.entity.InstructorApplication;
import com.sencours.entity.User;
import com.sencours.enums.ApplicationStatus;
import com.sencours.enums.Role;
import com.sencours.exception.BadRequestException;
import com.sencours.exception.ResourceNotFoundException;
import com.sencours.repository.InstructorApplicationRepository;
import com.sencours.repository.UserRepository;
import com.sencours.service.InstructorApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class InstructorApplicationServiceImpl implements InstructorApplicationService {

    private final InstructorApplicationRepository applicationRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public InstructorApplicationResponse createApplication(Long userId, InstructorApplicationCreateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));

        if (user.getRole() != Role.ETUDIANT) {
            throw new BadRequestException("Seuls les étudiants peuvent postuler pour devenir instructeur");
        }

        if (applicationRepository.existsByUserIdAndStatus(userId, ApplicationStatus.PENDING)) {
            throw new BadRequestException("Vous avez déjà une candidature en cours d'examen");
        }

        InstructorApplication application = InstructorApplication.builder()
                .user(user)
                .motivation(request.getMotivation())
                .expertise(request.getExpertise())
                .linkedinUrl(request.getLinkedinUrl())
                .portfolioUrl(request.getPortfolioUrl())
                .status(ApplicationStatus.PENDING)
                .build();

        application = applicationRepository.save(application);
        return mapToResponse(application);
    }

    @Override
    public InstructorApplicationResponse getMyApplication(Long userId) {
        return applicationRepository.findFirstByUserIdOrderByCreatedAtDesc(userId)
                .map(this::mapToResponse)
                .orElse(null);
    }

    @Override
    public boolean hasPendingApplication(Long userId) {
        return applicationRepository.existsByUserIdAndStatus(userId, ApplicationStatus.PENDING);
    }

    @Override
    public Page<InstructorApplicationResponse> getAllApplications(Pageable pageable) {
        return applicationRepository.findAllByOrderByCreatedAtDesc(pageable).map(this::mapToResponse);
    }

    @Override
    public Page<InstructorApplicationResponse> getApplicationsByStatus(ApplicationStatus status, Pageable pageable) {
        return applicationRepository.findByStatus(status, pageable).map(this::mapToResponse);
    }

    @Override
    public InstructorApplicationResponse getApplicationById(Long id) {
        return applicationRepository.findById(id)
                .map(this::mapToResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Candidature non trouvée"));
    }

    @Override
    @Transactional
    public InstructorApplicationResponse reviewApplication(Long applicationId, Long adminId, ApplicationReviewRequest request) {
        InstructorApplication application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Candidature non trouvée"));

        if (application.getStatus() != ApplicationStatus.PENDING) {
            throw new BadRequestException("Cette candidature a déjà été traitée");
        }

        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("Admin non trouvé"));

        application.setReviewedBy(admin);
        application.setReviewedAt(LocalDateTime.now());
        application.setAdminComment(request.getComment());

        if (Boolean.TRUE.equals(request.getApproved())) {
            application.setStatus(ApplicationStatus.APPROVED);
            User user = application.getUser();
            user.setRole(Role.INSTRUCTEUR);
            userRepository.save(user);
        } else {
            application.setStatus(ApplicationStatus.REJECTED);
        }

        application = applicationRepository.save(application);
        return mapToResponse(application);
    }

    @Override
    public long getPendingCount() {
        return applicationRepository.countByStatus(ApplicationStatus.PENDING);
    }

    private InstructorApplicationResponse mapToResponse(InstructorApplication application) {
        return InstructorApplicationResponse.builder()
                .id(application.getId())
                .userId(application.getUser().getId())
                .userFullName(application.getUser().getFirstName() + " " + application.getUser().getLastName())
                .userEmail(application.getUser().getEmail())
                .motivation(application.getMotivation())
                .expertise(application.getExpertise())
                .linkedinUrl(application.getLinkedinUrl())
                .portfolioUrl(application.getPortfolioUrl())
                .status(application.getStatus())
                .adminComment(application.getAdminComment())
                .reviewedByName(application.getReviewedBy() != null
                        ? application.getReviewedBy().getFirstName() + " " + application.getReviewedBy().getLastName()
                        : null)
                .reviewedAt(application.getReviewedAt())
                .createdAt(application.getCreatedAt())
                .build();
    }
}
