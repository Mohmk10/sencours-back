package com.sencours.mapper;

import com.sencours.dto.request.UserRequest;
import com.sencours.dto.response.UserResponse;
import com.sencours.entity.User;
import com.sencours.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class UserMapperTest {

    private UserMapper userMapper;

    @BeforeEach
    void setUp() {
        userMapper = new UserMapper();
    }

    @Test
    @DisplayName("Devrait convertir Entity vers Response sans mot de passe")
    void shouldConvertEntityToResponseWithoutPassword() {
        User entity = new User();
        entity.setId(1L);
        entity.setFirstName("Mamadou");
        entity.setLastName("Diallo");
        entity.setEmail("mamadou@sencours.sn");
        entity.setPassword("$2a$10$hashedpassword");
        entity.setBio("Bio test");
        entity.setRole(Role.ETUDIANT);
        entity.setIsActive(true);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());

        UserResponse response = userMapper.toResponse(entity);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getFirstName()).isEqualTo("Mamadou");
        assertThat(response.getLastName()).isEqualTo("Diallo");
        assertThat(response.getEmail()).isEqualTo("mamadou@sencours.sn");
        assertThat(response.getBio()).isEqualTo("Bio test");
        assertThat(response.getRole()).isEqualTo(Role.ETUDIANT);
        assertThat(response.getIsActive()).isTrue();
        assertThat(response.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Devrait retourner null si entity est null")
    void shouldReturnNullWhenEntityIsNull() {
        UserResponse response = userMapper.toResponse(null);

        assertThat(response).isNull();
    }

    @Test
    @DisplayName("Devrait convertir Request vers Entity sans mot de passe")
    void shouldConvertRequestToEntityWithoutPassword() {
        UserRequest request = UserRequest.builder()
                .firstName("Mamadou")
                .lastName("Diallo")
                .email("mamadou@sencours.sn")
                .password("password123")
                .bio("Bio test")
                .role(Role.ETUDIANT)
                .build();

        User entity = userMapper.toEntity(request);

        assertThat(entity).isNotNull();
        assertThat(entity.getId()).isNull();
        assertThat(entity.getFirstName()).isEqualTo("Mamadou");
        assertThat(entity.getLastName()).isEqualTo("Diallo");
        assertThat(entity.getEmail()).isEqualTo("mamadou@sencours.sn");
        assertThat(entity.getPassword()).isNull();
        assertThat(entity.getBio()).isEqualTo("Bio test");
        assertThat(entity.getRole()).isEqualTo(Role.ETUDIANT);
    }

    @Test
    @DisplayName("Devrait retourner null si request est null")
    void shouldReturnNullWhenRequestIsNull() {
        User entity = userMapper.toEntity(null);

        assertThat(entity).isNull();
    }

    @Test
    @DisplayName("Devrait mettre à jour entity sans toucher au mot de passe")
    void shouldUpdateEntityWithoutChangingPassword() {
        User entity = new User();
        entity.setId(1L);
        entity.setFirstName("Ancien");
        entity.setLastName("Nom");
        entity.setEmail("ancien@sencours.sn");
        entity.setPassword("$2a$10$originalhashedpassword");
        entity.setRole(Role.ETUDIANT);

        UserRequest request = UserRequest.builder()
                .firstName("Nouveau")
                .lastName("Nom")
                .email("nouveau@sencours.sn")
                .password("newpassword123")
                .role(Role.INSTRUCTEUR)
                .build();

        userMapper.updateEntityFromRequest(request, entity);

        assertThat(entity.getId()).isEqualTo(1L);
        assertThat(entity.getFirstName()).isEqualTo("Nouveau");
        assertThat(entity.getEmail()).isEqualTo("nouveau@sencours.sn");
        assertThat(entity.getPassword()).isEqualTo("$2a$10$originalhashedpassword");
        assertThat(entity.getRole()).isEqualTo(Role.INSTRUCTEUR);
    }

    @Test
    @DisplayName("Ne devrait rien faire si request ou entity est null")
    void shouldDoNothingWhenRequestOrEntityIsNull() {
        User entity = new User();
        entity.setFirstName("Original");
        entity.setPassword("$2a$10$hashedpassword");

        userMapper.updateEntityFromRequest(null, entity);
        assertThat(entity.getFirstName()).isEqualTo("Original");

        userMapper.updateEntityFromRequest(UserRequest.builder().firstName("Test").build(), null);
    }
}
