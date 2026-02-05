package com.sencours.service;

import com.sencours.dto.request.PasswordChangeRequest;
import com.sencours.dto.request.UserRequest;
import com.sencours.dto.response.PageResponse;
import com.sencours.dto.response.UserResponse;
import com.sencours.enums.Role;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface UserService {

    UserResponse create(UserRequest request);

    UserResponse getById(Long id);

    UserResponse getByEmail(String email);

    List<UserResponse> getAll();

    List<UserResponse> getByRole(Role role);

    List<UserResponse> getActiveUsers();

    UserResponse update(Long id, UserRequest request);

    void delete(Long id);

    void changePassword(Long id, PasswordChangeRequest request);

    void activateUser(Long id);

    void deactivateUser(Long id);

    boolean existsByEmail(String email);

    // Pagination methods
    PageResponse<UserResponse> getAllPaginated(Pageable pageable);

    PageResponse<UserResponse> getByRolePaginated(Role role, Pageable pageable);

    PageResponse<UserResponse> searchUsersPaginated(String search, Pageable pageable);
}
