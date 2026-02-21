package com.sencours.service;

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
import com.sencours.service.impl.InstructorApplicationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InstructorApplicationServiceTest {

    @Mock
    private InstructorApplicationRepository applicationRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private InstructorApplicationServiceImpl applicationService;

    private User etudiant;
    private User admin;
    private InstructorApplication pendingApplication;

    @BeforeEach
    void setUp() {
        etudiant = User.builder()
                .id(1L)
                .firstName("Moussa")
                .lastName("Diallo")
                .email("moussa@test.sn")
                .role(Role.ETUDIANT)
                .isActive(true)
                .build();

        admin = User.builder()
                .id(2L)
                .firstName("Admin")
                .lastName("SenCours")
                .email("admin@sencours.sn")
                .role(Role.ADMIN)
                .isActive(true)
                .build();

        pendingApplication = InstructorApplication.builder()
                .id(1L)
                .user(etudiant)
                .motivation("Je veux partager mes connaissances en développement web avec les autres")
                .expertise("Java, Spring Boot, Angular")
                .status(ApplicationStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("createApplication()")
    class CreateApplicationTests {

        @Test
        @DisplayName("Devrait créer une candidature pour un étudiant")
        void shouldCreateApplicationForStudent() {
            InstructorApplicationCreateRequest request = new InstructorApplicationCreateRequest(
                    "Je veux partager mes connaissances en développement web avec les autres",
                    "Java, Spring Boot",
                    "https://linkedin.com/in/moussa",
                    null
            );

            when(userRepository.findById(1L)).thenReturn(Optional.of(etudiant));
            when(applicationRepository.existsByUserIdAndStatus(1L, ApplicationStatus.PENDING)).thenReturn(false);
            when(applicationRepository.save(any())).thenReturn(pendingApplication);

            InstructorApplicationResponse result = applicationService.createApplication(1L, request);

            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(ApplicationStatus.PENDING);
            assertThat(result.getUserEmail()).isEqualTo("moussa@test.sn");
            verify(applicationRepository).save(any(InstructorApplication.class));
        }

        @Test
        @DisplayName("Devrait refuser si l'utilisateur n'est pas étudiant")
        void shouldRejectIfNotStudent() {
            etudiant.setRole(Role.INSTRUCTEUR);
            InstructorApplicationCreateRequest request = new InstructorApplicationCreateRequest(
                    "Je veux partager mes connaissances en développement web avec les autres",
                    null, null, null
            );

            when(userRepository.findById(1L)).thenReturn(Optional.of(etudiant));

            assertThatThrownBy(() -> applicationService.createApplication(1L, request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Seuls les étudiants");

            verify(applicationRepository, never()).save(any());
        }

        @Test
        @DisplayName("Devrait refuser si une candidature est déjà en cours")
        void shouldRejectIfPendingApplicationExists() {
            InstructorApplicationCreateRequest request = new InstructorApplicationCreateRequest(
                    "Je veux partager mes connaissances en développement web avec les autres",
                    null, null, null
            );

            when(userRepository.findById(1L)).thenReturn(Optional.of(etudiant));
            when(applicationRepository.existsByUserIdAndStatus(1L, ApplicationStatus.PENDING)).thenReturn(true);

            assertThatThrownBy(() -> applicationService.createApplication(1L, request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("déjà une candidature");

            verify(applicationRepository, never()).save(any());
        }

        @Test
        @DisplayName("Devrait lever une exception si l'utilisateur n'existe pas")
        void shouldThrowIfUserNotFound() {
            InstructorApplicationCreateRequest request = new InstructorApplicationCreateRequest(
                    "Je veux partager mes connaissances en développement web avec les autres",
                    null, null, null
            );

            when(userRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> applicationService.createApplication(99L, request))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("reviewApplication()")
    class ReviewApplicationTests {

        @Test
        @DisplayName("Devrait approuver une candidature et promouvoir l'étudiant en instructeur")
        void shouldApproveApplicationAndPromoteUser() {
            ApplicationReviewRequest request = new ApplicationReviewRequest(true, "Excellent profil");

            when(applicationRepository.findById(1L)).thenReturn(Optional.of(pendingApplication));
            when(userRepository.findById(2L)).thenReturn(Optional.of(admin));
            when(applicationRepository.save(any())).thenReturn(pendingApplication);
            when(userRepository.save(any())).thenReturn(etudiant);

            applicationService.reviewApplication(1L, 2L, request);

            verify(userRepository).save(argThat(u -> u.getRole() == Role.INSTRUCTEUR));
            verify(applicationRepository).save(argThat(a -> a.getStatus() == ApplicationStatus.APPROVED));
        }

        @Test
        @DisplayName("Devrait rejeter une candidature sans changer le rôle")
        void shouldRejectApplicationWithoutChangingRole() {
            ApplicationReviewRequest request = new ApplicationReviewRequest(false, "Profil insuffisant");

            when(applicationRepository.findById(1L)).thenReturn(Optional.of(pendingApplication));
            when(userRepository.findById(2L)).thenReturn(Optional.of(admin));
            when(applicationRepository.save(any())).thenReturn(pendingApplication);

            applicationService.reviewApplication(1L, 2L, request);

            verify(userRepository, never()).save(argThat(u -> u.getRole() == Role.INSTRUCTEUR));
            verify(applicationRepository).save(argThat(a -> a.getStatus() == ApplicationStatus.REJECTED));
        }

        @Test
        @DisplayName("Devrait refuser si la candidature est déjà traitée")
        void shouldRejectAlreadyReviewedApplication() {
            pendingApplication.setStatus(ApplicationStatus.APPROVED);
            ApplicationReviewRequest request = new ApplicationReviewRequest(true, null);

            when(applicationRepository.findById(1L)).thenReturn(Optional.of(pendingApplication));

            assertThatThrownBy(() -> applicationService.reviewApplication(1L, 2L, request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("déjà été traitée");
        }
    }

    @Nested
    @DisplayName("getAllApplications()")
    class GetAllApplicationsTests {

        @Test
        @DisplayName("Devrait retourner une page de candidatures")
        void shouldReturnPageOfApplications() {
            Page<InstructorApplication> page = new PageImpl<>(List.of(pendingApplication));
            when(applicationRepository.findAllByOrderByCreatedAtDesc(any())).thenReturn(page);

            Page<InstructorApplicationResponse> result = applicationService.getAllApplications(PageRequest.of(0, 10));

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getStatus()).isEqualTo(ApplicationStatus.PENDING);
        }
    }
}
