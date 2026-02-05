package com.sencours.mapper;

import com.sencours.dto.request.UserRequest;
import com.sencours.dto.response.UserResponse;
import com.sencours.entity.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserResponse toResponse(User entity) {
        if (entity == null) {
            return null;
        }
        return UserResponse.builder()
                .id(entity.getId())
                .firstName(entity.getFirstName())
                .lastName(entity.getLastName())
                .email(entity.getEmail())
                .bio(entity.getBio())
                .profilePicture(entity.getProfilePicture())
                .role(entity.getRole())
                .isActive(entity.getIsActive())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public User toEntity(UserRequest request) {
        if (request == null) {
            return null;
        }
        User user = new User();
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmail(request.getEmail());
        user.setBio(request.getBio());
        user.setProfilePicture(request.getProfilePicture());
        user.setRole(request.getRole());
        return user;
    }

    public void updateEntityFromRequest(UserRequest request, User entity) {
        if (request == null || entity == null) {
            return;
        }
        entity.setFirstName(request.getFirstName());
        entity.setLastName(request.getLastName());
        entity.setEmail(request.getEmail());
        entity.setBio(request.getBio());
        entity.setProfilePicture(request.getProfilePicture());
        entity.setRole(request.getRole());
    }
}
