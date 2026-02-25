package com.sencours.repository;

import com.sencours.entity.User;
import com.sencours.enums.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    List<User> findByRole(Role role);

    List<User> findByIsActiveTrue();

    // Pagination methods
    Page<User> findByRole(Role role, Pageable pageable);

    Page<User> findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCaseOrEmailContainingIgnoreCase(
            String firstName, String lastName, String email, Pageable pageable);

    void deleteAllByIdNot(Long id);

    List<User> findAllByDeletedAtIsNullOrderByCreatedAtDesc();

    Optional<User> findByEmailAndDeletedAtIsNull(String email);
}
