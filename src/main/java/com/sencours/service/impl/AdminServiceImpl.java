package com.sencours.service.impl;

import com.sencours.dto.response.UserResponse;
import com.sencours.entity.User;
import com.sencours.enums.Role;
import com.sencours.exception.ForbiddenException;
import com.sencours.exception.ResourceNotFoundException;
import com.sencours.mapper.UserMapper;
import com.sencours.repository.InstructorApplicationRepository;
import com.sencours.repository.UserRepository;
import com.sencours.service.AdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final UserRepository userRepository;
    private final InstructorApplicationRepository instructorApplicationRepository;
    private final UserMapper userMapper;

    @Override
    @Transactional
    public UserResponse toggleUserStatus(Long userId, String currentUserEmail) {
        User currentUser = userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur connecté non trouvé"));

        User targetUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));

        // 1. Impossible de se suspendre soi-même
        if (currentUser.getId().equals(targetUser.getId())) {
            throw new ForbiddenException("Vous ne pouvez pas modifier votre propre statut");
        }

        // 2. Impossible de toucher à un SUPER_ADMIN (sauf si on est SUPER_ADMIN)
        if (targetUser.getRole() == Role.SUPER_ADMIN && currentUser.getRole() != Role.SUPER_ADMIN) {
            throw new ForbiddenException("Vous ne pouvez pas modifier le statut d'un Super Administrateur");
        }

        // 3. Un ADMIN ne peut pas toucher à un autre ADMIN
        if (targetUser.getRole() == Role.ADMIN && currentUser.getRole() == Role.ADMIN) {
            throw new ForbiddenException("Vous ne pouvez pas modifier le statut d'un autre Administrateur");
        }

        // 4. Un ADMIN ne peut suspendre que ETUDIANT ou INSTRUCTEUR
        if (currentUser.getRole() == Role.ADMIN &&
                targetUser.getRole() != Role.ETUDIANT &&
                targetUser.getRole() != Role.INSTRUCTEUR) {
            throw new ForbiddenException("Vous ne pouvez suspendre que les étudiants et instructeurs");
        }

        // Toggle le statut
        targetUser.setIsActive(!targetUser.getIsActive());
        User savedUser = userRepository.save(targetUser);

        log.info("Statut de l'utilisateur {} modifié à is_active={} par {}",
                userId, savedUser.getIsActive(), currentUserEmail);

        return userMapper.toResponse(savedUser);
    }

    @Override
    @Transactional
    public void deleteUser(Long userId, String currentUserEmail) {
        User currentUser = userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur connecté non trouvé"));

        // Seul le SUPER_ADMIN peut supprimer
        if (currentUser.getRole() != Role.SUPER_ADMIN) {
            throw new ForbiddenException("Seul le Super Administrateur peut supprimer des utilisateurs");
        }

        User targetUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));

        // Impossible de se supprimer soi-même
        if (currentUser.getId().equals(targetUser.getId())) {
            throw new ForbiddenException("Vous ne pouvez pas supprimer votre propre compte");
        }

        // Nullify reviewedBy references in instructor applications reviewed by this user
        instructorApplicationRepository.findByReviewedById(targetUser.getId()).forEach(app -> {
            app.setReviewedBy(null);
            instructorApplicationRepository.save(app);
        });

        userRepository.delete(targetUser);

        log.info("Utilisateur {} supprimé par {}", userId, currentUserEmail);
    }
}
