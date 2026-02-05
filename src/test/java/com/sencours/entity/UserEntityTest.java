package com.sencours.entity;

import com.sencours.enums.Role;
import com.sencours.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
class UserEntityTest {

    @Autowired
    private UserRepository userRepository;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setFirstName("Mamadou");
        user.setLastName("Diallo");
        user.setEmail("mamadou@sencours.sn");
        user.setPassword("password123");
        user.setRole(Role.ETUDIANT);
    }

    @Test
    @DisplayName("Should save user with timestamps")
    void shouldSaveUserWithTimestamps() {
        User savedUser = userRepository.save(user);

        assertThat(savedUser.getId()).isNotNull();
        assertThat(savedUser.getCreatedAt()).isNotNull();
        assertThat(savedUser.getUpdatedAt()).isNotNull();
        assertThat(savedUser.getIsActive()).isTrue();
        assertThat(savedUser.getRole()).isEqualTo(Role.ETUDIANT);
    }

    @Test
    @DisplayName("Should find user by email")
    void shouldFindUserByEmail() {
        userRepository.save(user);

        var foundUser = userRepository.findByEmail("mamadou@sencours.sn");

        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getFirstName()).isEqualTo("Mamadou");
    }

    @Test
    @DisplayName("Should enforce unique email constraint")
    void shouldEnforceUniqueEmailConstraint() {
        userRepository.save(user);

        User duplicateUser = new User();
        duplicateUser.setFirstName("Autre");
        duplicateUser.setLastName("User");
        duplicateUser.setEmail("mamadou@sencours.sn");
        duplicateUser.setPassword("password456");

        assertThatThrownBy(() -> {
            userRepository.saveAndFlush(duplicateUser);
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Should find users by role")
    void shouldFindUsersByRole() {
        userRepository.save(user);

        User instructor = new User();
        instructor.setFirstName("Prof");
        instructor.setLastName("Test");
        instructor.setEmail("prof@sencours.sn");
        instructor.setPassword("password");
        instructor.setRole(Role.INSTRUCTEUR);
        userRepository.save(instructor);

        var etudiants = userRepository.findByRole(Role.ETUDIANT);
        var instructeurs = userRepository.findByRole(Role.INSTRUCTEUR);

        assertThat(etudiants).hasSize(1);
        assertThat(instructeurs).hasSize(1);
    }

    @Test
    @DisplayName("Should find active users")
    void shouldFindActiveUsers() {
        userRepository.save(user);

        User inactiveUser = new User();
        inactiveUser.setFirstName("Inactif");
        inactiveUser.setLastName("User");
        inactiveUser.setEmail("inactif@sencours.sn");
        inactiveUser.setPassword("password");
        inactiveUser.setIsActive(false);
        userRepository.save(inactiveUser);

        var activeUsers = userRepository.findByIsActiveTrue();

        assertThat(activeUsers).hasSize(1);
        assertThat(activeUsers.get(0).getEmail()).isEqualTo("mamadou@sencours.sn");
    }
}
