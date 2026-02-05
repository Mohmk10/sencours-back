package com.sencours.service;

import com.sencours.dto.request.LoginRequest;
import com.sencours.dto.request.RegisterRequest;
import com.sencours.dto.response.AuthResponse;
import com.sencours.entity.User;
import com.sencours.enums.Role;
import com.sencours.exception.EmailAlreadyExistsException;
import com.sencours.exception.InvalidCredentialsException;
import com.sencours.repository.UserRepository;
import com.sencours.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthServiceImpl authService;

    private User user;
    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .firstName("Mohamed")
                .lastName("Diallo")
                .email("mohamed@sencours.sn")
                .password("encodedPassword")
                .role(Role.ETUDIANT)
                .build();

        registerRequest = RegisterRequest.builder()
                .firstName("Mohamed")
                .lastName("Diallo")
                .email("mohamed@sencours.sn")
                .password("password123")
                .build();

        loginRequest = LoginRequest.builder()
                .email("mohamed@sencours.sn")
                .password("password123")
                .build();
    }

    @Nested
    @DisplayName("Tests pour register()")
    class RegisterTests {

        @Test
        @DisplayName("Devrait enregistrer un utilisateur avec succès")
        void shouldRegisterUserSuccessfully() {
            when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(false);
            when(passwordEncoder.encode(registerRequest.getPassword())).thenReturn("encodedPassword");
            when(userRepository.save(any(User.class))).thenReturn(user);
            when(jwtService.generateToken(any(User.class))).thenReturn("jwt.token.here");

            AuthResponse result = authService.register(registerRequest);

            assertThat(result).isNotNull();
            assertThat(result.getToken()).isEqualTo("jwt.token.here");
            assertThat(result.getType()).isEqualTo("Bearer");
            assertThat(result.getUserId()).isEqualTo(1L);
            assertThat(result.getEmail()).isEqualTo("mohamed@sencours.sn");
            assertThat(result.getFullName()).isEqualTo("Mohamed Diallo");
            assertThat(result.getRole()).isEqualTo(Role.ETUDIANT);

            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("Devrait lever une exception si l'email existe déjà")
        void shouldThrowExceptionWhenEmailExists() {
            when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(true);

            assertThatThrownBy(() -> authService.register(registerRequest))
                    .isInstanceOf(EmailAlreadyExistsException.class)
                    .hasMessageContaining(registerRequest.getEmail());

            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("Devrait encoder le mot de passe lors de l'inscription")
        void shouldEncodePasswordOnRegister() {
            when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(false);
            when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
            when(userRepository.save(any(User.class))).thenReturn(user);
            when(jwtService.generateToken(any(User.class))).thenReturn("jwt.token.here");

            authService.register(registerRequest);

            verify(passwordEncoder).encode("password123");
        }

        @Test
        @DisplayName("Devrait créer un utilisateur avec le rôle ETUDIANT par défaut")
        void shouldCreateUserWithStudentRoleByDefault() {
            when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(false);
            when(passwordEncoder.encode(registerRequest.getPassword())).thenReturn("encodedPassword");
            when(userRepository.save(any(User.class))).thenReturn(user);
            when(jwtService.generateToken(any(User.class))).thenReturn("jwt.token.here");

            AuthResponse result = authService.register(registerRequest);

            assertThat(result.getRole()).isEqualTo(Role.ETUDIANT);
        }
    }

    @Nested
    @DisplayName("Tests pour login()")
    class LoginTests {

        @Test
        @DisplayName("Devrait connecter un utilisateur avec succès")
        void shouldLoginUserSuccessfully() {
            when(userRepository.findByEmail(loginRequest.getEmail())).thenReturn(Optional.of(user));
            when(jwtService.generateToken(user)).thenReturn("jwt.token.here");

            AuthResponse result = authService.login(loginRequest);

            assertThat(result).isNotNull();
            assertThat(result.getToken()).isEqualTo("jwt.token.here");
            assertThat(result.getUserId()).isEqualTo(1L);
            assertThat(result.getEmail()).isEqualTo("mohamed@sencours.sn");

            verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        }

        @Test
        @DisplayName("Devrait lever une exception si les credentials sont invalides")
        void shouldThrowExceptionWhenCredentialsInvalid() {
            doThrow(new BadCredentialsException("Bad credentials"))
                    .when(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));

            assertThatThrownBy(() -> authService.login(loginRequest))
                    .isInstanceOf(InvalidCredentialsException.class);
        }

        @Test
        @DisplayName("Devrait lever une exception si l'email n'existe pas")
        void shouldThrowExceptionWhenEmailNotFound() {
            when(userRepository.findByEmail(loginRequest.getEmail())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.login(loginRequest))
                    .isInstanceOf(InvalidCredentialsException.class);
        }
    }

    @Nested
    @DisplayName("Tests pour getCurrentUser()")
    class GetCurrentUserTests {

        @Test
        @DisplayName("Devrait retourner les informations de l'utilisateur connecté")
        void shouldReturnCurrentUserInfo() {
            AuthResponse result = authService.getCurrentUser(user);

            assertThat(result).isNotNull();
            assertThat(result.getUserId()).isEqualTo(1L);
            assertThat(result.getEmail()).isEqualTo("mohamed@sencours.sn");
            assertThat(result.getFullName()).isEqualTo("Mohamed Diallo");
            assertThat(result.getRole()).isEqualTo(Role.ETUDIANT);
            assertThat(result.getToken()).isNull();
        }
    }
}
