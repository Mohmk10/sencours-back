package com.sencours.service.impl;

import com.sencours.dto.request.PasswordChangeRequest;
import com.sencours.dto.request.UserRequest;
import com.sencours.dto.response.PageResponse;
import com.sencours.dto.response.UserResponse;
import com.sencours.entity.User;
import com.sencours.enums.Role;
import com.sencours.exception.InvalidPasswordException;
import com.sencours.exception.ResourceAlreadyExistsException;
import com.sencours.exception.ResourceNotFoundException;
import com.sencours.mapper.UserMapper;
import com.sencours.repository.UserRepository;
import com.sencours.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserResponse create(UserRequest request) {
        log.info("Création d'un nouvel utilisateur avec email: {}", request.getEmail());

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ResourceAlreadyExistsException("Utilisateur", "email", request.getEmail());
        }

        User user = userMapper.toEntity(request);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setIsActive(true);

        User savedUser = userRepository.save(user);

        log.info("Utilisateur créé avec succès. ID: {}", savedUser.getId());
        return userMapper.toResponse(savedUser);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getById(Long id) {
        log.debug("Recherche de l'utilisateur avec ID: {}", id);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur", "id", id));

        return userMapper.toResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getByEmail(String email) {
        log.debug("Recherche de l'utilisateur avec email: {}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur", "email", email));

        return userMapper.toResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> getAll() {
        log.debug("Récupération de tous les utilisateurs");

        return userRepository.findAll()
                .stream()
                .map(userMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> getByRole(Role role) {
        log.debug("Récupération des utilisateurs avec le rôle: {}", role);

        return userRepository.findByRole(role)
                .stream()
                .map(userMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> getActiveUsers() {
        log.debug("Récupération des utilisateurs actifs");

        return userRepository.findByIsActiveTrue()
                .stream()
                .map(userMapper::toResponse)
                .toList();
    }

    @Override
    public UserResponse update(Long id, UserRequest request) {
        log.info("Mise à jour de l'utilisateur avec ID: {}", id);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur", "id", id));

        if (!user.getEmail().equals(request.getEmail())
                && userRepository.existsByEmail(request.getEmail())) {
            throw new ResourceAlreadyExistsException("Utilisateur", "email", request.getEmail());
        }

        userMapper.updateEntityFromRequest(request, user);
        User updatedUser = userRepository.save(user);

        log.info("Utilisateur mis à jour avec succès. ID: {}", updatedUser.getId());
        return userMapper.toResponse(updatedUser);
    }

    @Override
    public void delete(Long id) {
        log.info("Suppression de l'utilisateur avec ID: {}", id);

        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("Utilisateur", "id", id);
        }

        userRepository.deleteById(id);
        log.info("Utilisateur supprimé avec succès. ID: {}", id);
    }

    @Override
    public void changePassword(Long id, PasswordChangeRequest request) {
        log.info("Changement de mot de passe pour l'utilisateur ID: {}", id);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur", "id", id));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new InvalidPasswordException("Le mot de passe actuel est incorrect");
        }

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new InvalidPasswordException("Les mots de passe ne correspondent pas");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        log.info("Mot de passe changé avec succès pour l'utilisateur ID: {}", id);
    }

    @Override
    public void activateUser(Long id) {
        log.info("Activation de l'utilisateur ID: {}", id);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur", "id", id));

        user.setIsActive(true);
        userRepository.save(user);

        log.info("Utilisateur activé avec succès. ID: {}", id);
    }

    @Override
    public void deactivateUser(Long id) {
        log.info("Désactivation de l'utilisateur ID: {}", id);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur", "id", id));

        user.setIsActive(false);
        userRepository.save(user);

        log.info("Utilisateur désactivé avec succès. ID: {}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<UserResponse> getAllPaginated(Pageable pageable) {
        log.debug("Récupération de tous les utilisateurs avec pagination");
        Page<User> page = userRepository.findAll(pageable);
        List<UserResponse> content = page.getContent().stream()
                .map(userMapper::toResponse)
                .toList();
        return PageResponse.of(page, content);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<UserResponse> getByRolePaginated(Role role, Pageable pageable) {
        log.debug("Récupération des utilisateurs par rôle avec pagination: {}", role);
        Page<User> page = userRepository.findByRole(role, pageable);
        List<UserResponse> content = page.getContent().stream()
                .map(userMapper::toResponse)
                .toList();
        return PageResponse.of(page, content);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<UserResponse> searchUsersPaginated(String search, Pageable pageable) {
        log.debug("Recherche des utilisateurs avec pagination: {}", search);
        Page<User> page = userRepository.findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCaseOrEmailContainingIgnoreCase(
                search, search, search, pageable);
        List<UserResponse> content = page.getContent().stream()
                .map(userMapper::toResponse)
                .toList();
        return PageResponse.of(page, content);
    }
}
