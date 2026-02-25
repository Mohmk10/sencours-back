package com.sencours.service.impl;

import com.sencours.dto.request.LoginRequest;
import com.sencours.dto.request.RegisterRequest;
import com.sencours.dto.response.AuthResponse;
import com.sencours.entity.User;
import com.sencours.enums.Role;
import com.sencours.exception.AccountDeletedException;
import com.sencours.exception.EmailAlreadyExistsException;
import com.sencours.exception.InvalidCredentialsException;
import com.sencours.repository.UserRepository;
import com.sencours.service.AuthService;
import com.sencours.service.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        log.info("Tentative d'inscription pour l'email: {}", request.getEmail());

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException(request.getEmail());
        }

        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.ETUDIANT)
                .build();

        User savedUser = userRepository.save(user);

        String token = jwtService.generateToken(savedUser);

        log.info("Inscription réussie pour l'utilisateur: {} (ID: {})", savedUser.getEmail(), savedUser.getId());
        return buildAuthResponse(savedUser, token);
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        log.info("Tentative de connexion pour l'email: {}", request.getEmail());

        // Chercher l'utilisateur
        Optional<User> optionalUser = userRepository.findByEmail(request.getEmail());

        if (optionalUser.isEmpty()) {
            throw new InvalidCredentialsException("Email ou mot de passe incorrect");
        }

        User user = optionalUser.get();

        // Vérifier si le compte est supprimé (soft delete)
        if (user.isDeleted()) {
            throw new AccountDeletedException("Votre compte a été supprimé le " +
                    user.getDeletedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) +
                    ". Si vous pensez qu'il s'agit d'une erreur, veuillez contacter le support à support@sencours.sn");
        }

        // Vérifier le mot de passe
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new InvalidCredentialsException("Email ou mot de passe incorrect");
        }

        // Générer le token même si suspendu (le frontend gèrera l'affichage)
        String token = jwtService.generateToken(user);

        log.info("Connexion réussie pour l'utilisateur: {} (ID: {})", user.getEmail(), user.getId());
        return buildAuthResponse(user, token);
    }

    @Override
    public AuthResponse getCurrentUser(User user) {
        return AuthResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .fullName(user.getFirstName() + " " + user.getLastName())
                .role(user.getRole())
                .isActive(user.getIsActive())
                .build();
    }

    private AuthResponse buildAuthResponse(User user, String token) {
        return AuthResponse.builder()
                .token(token)
                .type("Bearer")
                .userId(user.getId())
                .email(user.getEmail())
                .fullName(user.getFirstName() + " " + user.getLastName())
                .role(user.getRole())
                .isActive(user.getIsActive())
                .build();
    }
}
