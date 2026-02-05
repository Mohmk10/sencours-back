package com.sencours.service;

import com.sencours.dto.request.LoginRequest;
import com.sencours.dto.request.RegisterRequest;
import com.sencours.dto.response.AuthResponse;
import com.sencours.entity.User;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    AuthResponse getCurrentUser(User user);
}
