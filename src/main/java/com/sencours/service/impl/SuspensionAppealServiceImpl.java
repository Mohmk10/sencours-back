package com.sencours.service.impl;

import com.sencours.dto.SuspensionAppealRequest;
import com.sencours.dto.SuspensionAppealResponse;
import com.sencours.dto.SuspensionAppealReviewRequest;
import com.sencours.entity.SuspensionAppeal;
import com.sencours.entity.User;
import com.sencours.enums.AppealStatus;
import com.sencours.exception.BadRequestException;
import com.sencours.exception.ForbiddenException;
import com.sencours.exception.ResourceNotFoundException;
import com.sencours.repository.SuspensionAppealRepository;
import com.sencours.repository.UserRepository;
import com.sencours.service.SuspensionAppealService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SuspensionAppealServiceImpl implements SuspensionAppealService {

    private final SuspensionAppealRepository appealRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public SuspensionAppealResponse submitAppeal(SuspensionAppealRequest request, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));

        // Vérifier que l'utilisateur est bien suspendu
        if (user.getIsActive()) {
            throw new BadRequestException("Votre compte n'est pas suspendu");
        }

        // Vérifier qu'il n'y a pas déjà une contestation en attente
        if (appealRepository.existsByUserIdAndStatus(user.getId(), AppealStatus.PENDING)) {
            throw new BadRequestException("Vous avez déjà une contestation en attente de traitement");
        }

        if (request.getReason() == null || request.getReason().isBlank()) {
            throw new BadRequestException("La raison de la contestation est requise");
        }

        SuspensionAppeal appeal = SuspensionAppeal.builder()
                .user(user)
                .reason(request.getReason())
                .status(AppealStatus.PENDING)
                .build();

        SuspensionAppeal savedAppeal = appealRepository.save(appeal);

        log.info("Contestation soumise par l'utilisateur {} (ID: {})", userEmail, user.getId());

        return mapToResponse(savedAppeal);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SuspensionAppealResponse> getUserAppeals(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));

        return appealRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SuspensionAppealResponse> getPendingAppeals() {
        return appealRepository.findByStatusOrderByCreatedAtAsc(AppealStatus.PENDING)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional
    public SuspensionAppealResponse reviewAppeal(Long appealId, SuspensionAppealReviewRequest request, String adminEmail) {
        User admin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Administrateur non trouvé"));

        SuspensionAppeal appeal = appealRepository.findById(appealId)
                .orElseThrow(() -> new ResourceNotFoundException("Contestation non trouvée"));

        if (appeal.getStatus() != AppealStatus.PENDING) {
            throw new BadRequestException("Cette contestation a déjà été traitée");
        }

        if (request.getStatus() != AppealStatus.APPROVED && request.getStatus() != AppealStatus.REJECTED) {
            throw new BadRequestException("Le statut doit être APPROVED ou REJECTED");
        }

        appeal.setStatus(request.getStatus());
        appeal.setAdminResponse(request.getAdminResponse());
        appeal.setReviewedAt(LocalDateTime.now());
        appeal.setReviewedBy(admin);

        // Si approuvé, réactiver l'utilisateur
        if (request.getStatus() == AppealStatus.APPROVED) {
            User user = appeal.getUser();
            user.setIsActive(true);
            userRepository.save(user);
            log.info("Utilisateur {} réactivé suite à la contestation approuvée par {}", user.getEmail(), adminEmail);
        }

        SuspensionAppeal savedAppeal = appealRepository.save(appeal);

        log.info("Contestation {} {} par {}", appealId, request.getStatus(), adminEmail);

        return mapToResponse(savedAppeal);
    }

    private SuspensionAppealResponse mapToResponse(SuspensionAppeal appeal) {
        User user = appeal.getUser();
        return SuspensionAppealResponse.builder()
                .id(appeal.getId())
                .userId(user.getId())
                .userFirstName(user.getFirstName())
                .userLastName(user.getLastName())
                .userEmail(user.getEmail())
                .userRole(user.getRole().name())
                .reason(appeal.getReason())
                .status(appeal.getStatus())
                .adminResponse(appeal.getAdminResponse())
                .createdAt(appeal.getCreatedAt())
                .reviewedAt(appeal.getReviewedAt())
                .reviewedByName(appeal.getReviewedBy() != null
                        ? appeal.getReviewedBy().getFirstName() + " " + appeal.getReviewedBy().getLastName()
                        : null)
                .build();
    }
}
