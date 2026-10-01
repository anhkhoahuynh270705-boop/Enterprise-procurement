package com.example.shopping.user.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.example.shopping.user.dto.request.CreateUserRequestDto;
import com.example.shopping.user.dto.request.UpdateUserRequestDto;
import com.example.shopping.user.dto.response.UserResponseDto;
import com.example.shopping.user.entity.UserEntity;

public interface UserService {
    UserResponseDto updateCurrentProfile(String username, com.example.shopping.user.dto.request.UpdateProfileRequestDto request);

    List<UserResponseDto> getAllUsers();

    Optional<UserResponseDto> getUserById(UUID id);

    Optional<UserResponseDto> getUserByUsername(String username);

    UserResponseDto createUser(CreateUserRequestDto request);

    UserResponseDto updateUser(UUID id, UpdateUserRequestDto request);

    void deleteUser(UUID id);

    UserResponseDto toggleUserEnabled(UUID id);

    UserResponseDto uploadAvatar(String username, org.springframework.web.multipart.MultipartFile file);

    UserEntity getAvatarEntity(UUID userId);
}
