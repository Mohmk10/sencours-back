package com.sencours.service;

import com.sencours.dto.request.PasswordChangeRequest;
import com.sencours.dto.request.UserRequest;
import com.sencours.dto.response.UserResponse;
import com.sencours.entity.User;
import com.sencours.enums.Role;
import com.sencours.exception.InvalidPasswordException;
import com.sencours.exception.ResourceAlreadyExistsException;
import com.sencours.exception.ResourceNotFoundException;
import com.sencours.mapper.UserMapper;
import com.sencours.repository.UserRepository;
import com.sencours.service.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    private UserRequest userRequest;
    private User user;
    private UserResponse userResponse;

    @BeforeEach
    void setUp() {
        userRequest = UserRequest.builder()
                .firstName("Mamadou")
                .lastName("Diallo")
                .email("mamadou@sencours.sn")
                .password("password123")
                .role(Role.ETUDIANT)
                .build();

        user = new User();
        user.setId(1L);
        user.setFirstName("Mamadou");
        user.setLastName("Diallo");
        user.setEmail("mamadou@sencours.sn");
        user.setPassword("$2a$10$hashedpassword");
        user.setRole(Role.ETUDIANT);
        user.setIsActive(true);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        userResponse = UserResponse.builder()
                .id(1L)
                .firstName("Mamadou")
                .lastName("Diallo")
                .email("mamadou@sencours.sn")
                .role(Role.ETUDIANT)
                .isActive(true)
                .build();
    }

    @Nested
    @DisplayName("Tests pour create()")
    class CreateTests {

        @Test
        @DisplayName("Devrait créer un utilisateur avec mot de passe hashé")
        void shouldCreateUserWithHashedPassword() {
            when(userRepository.existsByEmail(userRequest.getEmail())).thenReturn(false);
            when(userMapper.toEntity(userRequest)).thenReturn(user);
            when(passwordEncoder.encode("password123")).thenReturn("$2a$10$hashedpassword");
            when(userRepository.save(any(User.class))).thenReturn(user);
            when(userMapper.toResponse(user)).thenReturn(userResponse);

            UserResponse result = userService.create(userRequest);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            assertThat(userCaptor.getValue().getPassword()).startsWith("$2a$");
            verify(passwordEncoder).encode("password123");
        }

        @Test
        @DisplayName("Devrait lever une exception si email existe déjà")
        void shouldThrowExceptionWhenEmailExists() {
            when(userRepository.existsByEmail(userRequest.getEmail())).thenReturn(true);

            assertThatThrownBy(() -> userService.create(userRequest))
                    .isInstanceOf(ResourceAlreadyExistsException.class)
                    .hasMessageContaining("email");

            verify(userRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Tests pour getById()")
    class GetByIdTests {

        @Test
        @DisplayName("Devrait retourner un utilisateur par ID")
        void shouldReturnUserById() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(userMapper.toResponse(user)).thenReturn(userResponse);

            UserResponse result = userService.getById(1L);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("Devrait lever une exception si ID non trouvé")
        void shouldThrowExceptionWhenIdNotFound() {
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.getById(999L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("999");
        }
    }

    @Nested
    @DisplayName("Tests pour getByEmail()")
    class GetByEmailTests {

        @Test
        @DisplayName("Devrait retourner un utilisateur par email")
        void shouldReturnUserByEmail() {
            when(userRepository.findByEmail("mamadou@sencours.sn")).thenReturn(Optional.of(user));
            when(userMapper.toResponse(user)).thenReturn(userResponse);

            UserResponse result = userService.getByEmail("mamadou@sencours.sn");

            assertThat(result).isNotNull();
            assertThat(result.getEmail()).isEqualTo("mamadou@sencours.sn");
        }
    }

    @Nested
    @DisplayName("Tests pour getAll()")
    class GetAllTests {

        @Test
        @DisplayName("Devrait retourner tous les utilisateurs")
        void shouldReturnAllUsers() {
            User user2 = new User();
            user2.setId(2L);
            user2.setEmail("prof@sencours.sn");

            UserResponse response2 = UserResponse.builder()
                    .id(2L)
                    .email("prof@sencours.sn")
                    .build();

            when(userRepository.findAll()).thenReturn(Arrays.asList(user, user2));
            when(userMapper.toResponse(user)).thenReturn(userResponse);
            when(userMapper.toResponse(user2)).thenReturn(response2);

            List<UserResponse> result = userService.getAll();

            assertThat(result).hasSize(2);
        }
    }

    @Nested
    @DisplayName("Tests pour getByRole()")
    class GetByRoleTests {

        @Test
        @DisplayName("Devrait retourner les utilisateurs par rôle")
        void shouldReturnUsersByRole() {
            when(userRepository.findByRole(Role.ETUDIANT)).thenReturn(List.of(user));
            when(userMapper.toResponse(user)).thenReturn(userResponse);

            List<UserResponse> result = userService.getByRole(Role.ETUDIANT);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getRole()).isEqualTo(Role.ETUDIANT);
        }
    }

    @Nested
    @DisplayName("Tests pour update()")
    class UpdateTests {

        @Test
        @DisplayName("Devrait mettre à jour un utilisateur sans modifier le mot de passe")
        void shouldUpdateUserWithoutChangingPassword() {
            String originalPassword = user.getPassword();
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(userRepository.save(any(User.class))).thenReturn(user);
            when(userMapper.toResponse(user)).thenReturn(userResponse);

            UserResponse result = userService.update(1L, userRequest);

            assertThat(result).isNotNull();
            verify(passwordEncoder, never()).encode(anyString());
            assertThat(user.getPassword()).isEqualTo(originalPassword);
        }

        @Test
        @DisplayName("Devrait lever une exception si nouvel email existe déjà")
        void shouldThrowExceptionWhenNewEmailExists() {
            UserRequest updateRequest = UserRequest.builder()
                    .firstName("Mamadou")
                    .lastName("Diallo")
                    .email("autre@sencours.sn")
                    .password("password123")
                    .role(Role.ETUDIANT)
                    .build();

            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(userRepository.existsByEmail("autre@sencours.sn")).thenReturn(true);

            assertThatThrownBy(() -> userService.update(1L, updateRequest))
                    .isInstanceOf(ResourceAlreadyExistsException.class);

            verify(userRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Tests pour delete()")
    class DeleteTests {

        @Test
        @DisplayName("Devrait supprimer un utilisateur")
        void shouldDeleteUser() {
            when(userRepository.existsById(1L)).thenReturn(true);
            doNothing().when(userRepository).deleteById(1L);

            userService.delete(1L);

            verify(userRepository).deleteById(1L);
        }
    }

    @Nested
    @DisplayName("Tests pour changePassword()")
    class ChangePasswordTests {

        @Test
        @DisplayName("Devrait changer le mot de passe avec succès")
        void shouldChangePasswordSuccessfully() {
            PasswordChangeRequest request = PasswordChangeRequest.builder()
                    .currentPassword("password123")
                    .newPassword("newPassword123")
                    .confirmPassword("newPassword123")
                    .build();

            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("password123", user.getPassword())).thenReturn(true);
            when(passwordEncoder.encode("newPassword123")).thenReturn("$2a$10$newhashedpassword");
            when(userRepository.save(any(User.class))).thenReturn(user);

            userService.changePassword(1L, request);

            verify(passwordEncoder).encode("newPassword123");
            verify(userRepository).save(user);
        }

        @Test
        @DisplayName("Devrait lever une exception si mot de passe actuel incorrect")
        void shouldThrowExceptionWhenCurrentPasswordWrong() {
            PasswordChangeRequest request = PasswordChangeRequest.builder()
                    .currentPassword("wrongPassword")
                    .newPassword("newPassword123")
                    .confirmPassword("newPassword123")
                    .build();

            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("wrongPassword", user.getPassword())).thenReturn(false);

            assertThatThrownBy(() -> userService.changePassword(1L, request))
                    .isInstanceOf(InvalidPasswordException.class)
                    .hasMessageContaining("incorrect");

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("Devrait lever une exception si les mots de passe ne correspondent pas")
        void shouldThrowExceptionWhenPasswordsDoNotMatch() {
            PasswordChangeRequest request = PasswordChangeRequest.builder()
                    .currentPassword("password123")
                    .newPassword("newPassword123")
                    .confirmPassword("differentPassword")
                    .build();

            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("password123", user.getPassword())).thenReturn(true);

            assertThatThrownBy(() -> userService.changePassword(1L, request))
                    .isInstanceOf(InvalidPasswordException.class)
                    .hasMessageContaining("correspondent");

            verify(userRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Tests pour activateUser() et deactivateUser()")
    class ActivationTests {

        @Test
        @DisplayName("Devrait activer un utilisateur")
        void shouldActivateUser() {
            user.setIsActive(false);
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(userRepository.save(any(User.class))).thenReturn(user);

            userService.activateUser(1L);

            assertThat(user.getIsActive()).isTrue();
            verify(userRepository).save(user);
        }

        @Test
        @DisplayName("Devrait désactiver un utilisateur")
        void shouldDeactivateUser() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(userRepository.save(any(User.class))).thenReturn(user);

            userService.deactivateUser(1L);

            assertThat(user.getIsActive()).isFalse();
            verify(userRepository).save(user);
        }
    }
}
