package com.sencours.service;

import com.sencours.dto.response.UserResponse;

public interface AdminService {

    UserResponse toggleUserStatus(Long userId, String currentUserEmail);

    void deleteUser(Long userId, String currentUserEmail);
}
