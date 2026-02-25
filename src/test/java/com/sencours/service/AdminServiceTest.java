package com.sencours.service;

import com.sencours.dto.response.UserResponse;
import com.sencours.entity.User;
import com.sencours.enums.Role;
import com.sencours.exception.ForbiddenException;
import com.sencours.exception.ResourceNotFoundException;
import com.sencours.mapper.UserMapper;
import com.sencours.repository.InstructorApplicationRepository;
import com.sencours.repository.UserRepository;
import com.sencours.service.impl.AdminServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private InstructorApplicationRepository instructorApplicationRepository;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private AdminServiceImpl adminService;

    private User superAdmin;
    private User admin;
    private User admin2;
    private User instructeur;
    private User etudiant;

    @BeforeEach
    void setUp() {
        superAdmin = User.builder()
                .id(1L)
                .firstName("Super")
                .lastName("Admin")
                .email("superadmin@sencours.sn")
                .password("encoded")
                .role(Role.SUPER_ADMIN)
                .isActive(true)
                .build();

        admin = User.builder()
                .id(2L)
                .firstName("Admin")
                .lastName("User")
                .email("admin@sencours.sn")
                .password("encoded")
                .role(Role.ADMIN)
                .isActive(true)
                .build();

        admin2 = User.builder()
                .id(5L)
                .firstName("Admin2")
                .lastName("User2")
                .email("admin2@sencours.sn")
                .password("encoded")
                .role(Role.ADMIN)
                .isActive(true)
                .build();

        instructeur = User.builder()
                .id(3L)
                .firstName("Instructeur")
                .lastName("User")
                .email("instructeur@sencours.sn")
                .password("encoded")
                .role(Role.INSTRUCTEUR)
                .isActive(true)
                .build();

        etudiant = User.builder()
                .id(4L)
                .firstName("Etudiant")
                .lastName("User")
                .email("etudiant@sencours.sn")
                .password("encoded")
                .role(Role.ETUDIANT)
                .isActive(true)
                .build();
    }

    @Nested
    @DisplayName("toggleUserStatus Tests")
    class ToggleUserStatusTests {

        @Test
        @DisplayName("Admin peut suspendre un étudiant")
        void adminCanSuspendStudent() {
            when(userRepository.findByEmail("admin@sencours.sn")).thenReturn(Optional.of(admin));
            when(userRepository.findById(4L)).thenReturn(Optional.of(etudiant));
            when(userRepository.save(any(User.class))).thenReturn(etudiant);
            when(userMapper.toResponse(any(User.class))).thenReturn(
                    UserResponse.builder().id(4L).isActive(false).build());

            UserResponse result = adminService.toggleUserStatus(4L, "admin@sencours.sn");

            assertThat(result).isNotNull();
            assertThat(etudiant.getIsActive()).isFalse();
            verify(userRepository).save(etudiant);
        }

        @Test
        @DisplayName("Admin peut suspendre un instructeur")
        void adminCanSuspendInstructor() {
            when(userRepository.findByEmail("admin@sencours.sn")).thenReturn(Optional.of(admin));
            when(userRepository.findById(3L)).thenReturn(Optional.of(instructeur));
            when(userRepository.save(any(User.class))).thenReturn(instructeur);
            when(userMapper.toResponse(any(User.class))).thenReturn(
                    UserResponse.builder().id(3L).isActive(false).build());

            UserResponse result = adminService.toggleUserStatus(3L, "admin@sencours.sn");

            assertThat(result).isNotNull();
            assertThat(instructeur.getIsActive()).isFalse();
            verify(userRepository).save(instructeur);
        }

        @Test
        @DisplayName("Admin peut réactiver un étudiant suspendu")
        void adminCanReactivateSuspendedStudent() {
            etudiant.setIsActive(false);
            when(userRepository.findByEmail("admin@sencours.sn")).thenReturn(Optional.of(admin));
            when(userRepository.findById(4L)).thenReturn(Optional.of(etudiant));
            when(userRepository.save(any(User.class))).thenReturn(etudiant);
            when(userMapper.toResponse(any(User.class))).thenReturn(
                    UserResponse.builder().id(4L).isActive(true).build());

            adminService.toggleUserStatus(4L, "admin@sencours.sn");

            assertThat(etudiant.getIsActive()).isTrue();
        }

        @Test
        @DisplayName("Admin ne peut pas se suspendre lui-même")
        void adminCannotSuspendSelf() {
            when(userRepository.findByEmail("admin@sencours.sn")).thenReturn(Optional.of(admin));
            when(userRepository.findById(2L)).thenReturn(Optional.of(admin));

            assertThatThrownBy(() -> adminService.toggleUserStatus(2L, "admin@sencours.sn"))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessage("Vous ne pouvez pas modifier votre propre statut");
        }

        @Test
        @DisplayName("Admin ne peut pas suspendre un SuperAdmin")
        void adminCannotSuspendSuperAdmin() {
            when(userRepository.findByEmail("admin@sencours.sn")).thenReturn(Optional.of(admin));
            when(userRepository.findById(1L)).thenReturn(Optional.of(superAdmin));

            assertThatThrownBy(() -> adminService.toggleUserStatus(1L, "admin@sencours.sn"))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessage("Vous ne pouvez pas modifier le statut d'un Super Administrateur");
        }

        @Test
        @DisplayName("Admin ne peut pas suspendre un autre Admin")
        void adminCannotSuspendAnotherAdmin() {
            when(userRepository.findByEmail("admin@sencours.sn")).thenReturn(Optional.of(admin));
            when(userRepository.findById(5L)).thenReturn(Optional.of(admin2));

            assertThatThrownBy(() -> adminService.toggleUserStatus(5L, "admin@sencours.sn"))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessage("Vous ne pouvez pas modifier le statut d'un autre Administrateur");
        }

        @Test
        @DisplayName("SuperAdmin peut suspendre un Admin")
        void superAdminCanSuspendAdmin() {
            when(userRepository.findByEmail("superadmin@sencours.sn")).thenReturn(Optional.of(superAdmin));
            when(userRepository.findById(2L)).thenReturn(Optional.of(admin));
            when(userRepository.save(any(User.class))).thenReturn(admin);
            when(userMapper.toResponse(any(User.class))).thenReturn(
                    UserResponse.builder().id(2L).isActive(false).build());

            UserResponse result = adminService.toggleUserStatus(2L, "superadmin@sencours.sn");

            assertThat(result).isNotNull();
            assertThat(admin.getIsActive()).isFalse();
        }

        @Test
        @DisplayName("SuperAdmin peut suspendre un étudiant")
        void superAdminCanSuspendStudent() {
            when(userRepository.findByEmail("superadmin@sencours.sn")).thenReturn(Optional.of(superAdmin));
            when(userRepository.findById(4L)).thenReturn(Optional.of(etudiant));
            when(userRepository.save(any(User.class))).thenReturn(etudiant);
            when(userMapper.toResponse(any(User.class))).thenReturn(
                    UserResponse.builder().id(4L).isActive(false).build());

            UserResponse result = adminService.toggleUserStatus(4L, "superadmin@sencours.sn");

            assertThat(result).isNotNull();
            assertThat(etudiant.getIsActive()).isFalse();
        }

        @Test
        @DisplayName("SuperAdmin ne peut pas se suspendre lui-même")
        void superAdminCannotSuspendSelf() {
            when(userRepository.findByEmail("superadmin@sencours.sn")).thenReturn(Optional.of(superAdmin));
            when(userRepository.findById(1L)).thenReturn(Optional.of(superAdmin));

            assertThatThrownBy(() -> adminService.toggleUserStatus(1L, "superadmin@sencours.sn"))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessage("Vous ne pouvez pas modifier votre propre statut");
        }

        @Test
        @DisplayName("Throw si utilisateur connecté non trouvé")
        void throwIfCurrentUserNotFound() {
            when(userRepository.findByEmail("unknown@sencours.sn")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> adminService.toggleUserStatus(4L, "unknown@sencours.sn"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("Throw si utilisateur cible non trouvé")
        void throwIfTargetUserNotFound() {
            when(userRepository.findByEmail("admin@sencours.sn")).thenReturn(Optional.of(admin));
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> adminService.toggleUserStatus(999L, "admin@sencours.sn"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("deleteUser Tests")
    class DeleteUserTests {

        @Test
        @DisplayName("SuperAdmin peut soft-delete un étudiant")
        void superAdminCanSoftDeleteStudent() {
            when(userRepository.findByEmail("superadmin@sencours.sn")).thenReturn(Optional.of(superAdmin));
            when(userRepository.findById(4L)).thenReturn(Optional.of(etudiant));
            when(userRepository.save(any(User.class))).thenReturn(etudiant);

            adminService.deleteUser(4L, "superadmin@sencours.sn");

            assertThat(etudiant.getDeletedAt()).isNotNull();
            assertThat(etudiant.getIsActive()).isFalse();
            verify(userRepository).save(etudiant);
            verify(userRepository, never()).delete(any());
        }

        @Test
        @DisplayName("SuperAdmin peut soft-delete un instructeur")
        void superAdminCanSoftDeleteInstructor() {
            when(userRepository.findByEmail("superadmin@sencours.sn")).thenReturn(Optional.of(superAdmin));
            when(userRepository.findById(3L)).thenReturn(Optional.of(instructeur));
            when(userRepository.save(any(User.class))).thenReturn(instructeur);

            adminService.deleteUser(3L, "superadmin@sencours.sn");

            assertThat(instructeur.getDeletedAt()).isNotNull();
            assertThat(instructeur.getIsActive()).isFalse();
        }

        @Test
        @DisplayName("SuperAdmin peut soft-delete un admin")
        void superAdminCanSoftDeleteAdmin() {
            when(userRepository.findByEmail("superadmin@sencours.sn")).thenReturn(Optional.of(superAdmin));
            when(userRepository.findById(2L)).thenReturn(Optional.of(admin));
            when(userRepository.save(any(User.class))).thenReturn(admin);

            adminService.deleteUser(2L, "superadmin@sencours.sn");

            assertThat(admin.getDeletedAt()).isNotNull();
            assertThat(admin.getIsActive()).isFalse();
        }

        @Test
        @DisplayName("SuperAdmin ne peut pas se supprimer lui-même")
        void superAdminCannotDeleteSelf() {
            when(userRepository.findByEmail("superadmin@sencours.sn")).thenReturn(Optional.of(superAdmin));
            when(userRepository.findById(1L)).thenReturn(Optional.of(superAdmin));

            assertThatThrownBy(() -> adminService.deleteUser(1L, "superadmin@sencours.sn"))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessage("Vous ne pouvez pas supprimer votre propre compte");
        }

        @Test
        @DisplayName("Admin ne peut pas supprimer (réservé au SuperAdmin)")
        void adminCannotDelete() {
            when(userRepository.findByEmail("admin@sencours.sn")).thenReturn(Optional.of(admin));

            assertThatThrownBy(() -> adminService.deleteUser(4L, "admin@sencours.sn"))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessage("Seul le Super Administrateur peut supprimer des utilisateurs");
        }

        @Test
        @DisplayName("Throw si utilisateur connecté non trouvé")
        void throwIfCurrentUserNotFound() {
            when(userRepository.findByEmail("unknown@sencours.sn")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> adminService.deleteUser(4L, "unknown@sencours.sn"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("Throw si utilisateur cible non trouvé")
        void throwIfTargetUserNotFound() {
            when(userRepository.findByEmail("superadmin@sencours.sn")).thenReturn(Optional.of(superAdmin));
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> adminService.deleteUser(999L, "superadmin@sencours.sn"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}
