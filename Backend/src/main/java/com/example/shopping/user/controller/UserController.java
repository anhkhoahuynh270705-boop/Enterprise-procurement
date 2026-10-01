package com.example.shopping.user.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.shopping.user.dto.request.CreateUserRequestDto;
import com.example.shopping.user.dto.request.UpdateProfileRequestDto;
import com.example.shopping.user.dto.request.UpdateUserRequestDto;
import com.example.shopping.user.dto.response.UserResponseDto;
import com.example.shopping.user.entity.UserEntity;
import com.example.shopping.user.service.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<List<UserResponseDto>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponseDto> getUserById(@PathVariable UUID id) {
        return userService.getUserById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<UserResponseDto> createUser(@Valid @RequestBody CreateUserRequestDto request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.createUser(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserResponseDto> updateUser(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateUserRequestDto request) {
        return ResponseEntity.ok(userService.updateUser(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable UUID id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/toggle-status")
    public ResponseEntity<UserResponseDto> toggleUserStatus(@PathVariable UUID id) {
        return ResponseEntity.ok(userService.toggleUserEnabled(id));
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponseDto> getCurrentUser(@AuthenticationPrincipal Jwt jwt) {
        String username = jwt.getClaimAsString("preferred_username");
        if (username == null || username.isBlank()) {
            username = jwt.getSubject();
        }
        return ResponseEntity.ok(userService.getUserByUsername(username).orElse(null));
    }

    @PutMapping("/me")
    public ResponseEntity<UserResponseDto> updateCurrentProfile(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UpdateProfileRequestDto request) {
        String username = jwt.getClaimAsString("preferred_username");
        if (username == null || username.isBlank()) username = jwt.getSubject();
        return ResponseEntity.ok(userService.updateCurrentProfile(username, request));
    }

    @PostMapping("/me/avatar")
    public ResponseEntity<UserResponseDto> uploadMyAvatar(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam("file") MultipartFile file) {
        String username = jwt.getClaimAsString("preferred_username");
        if (username == null || username.isBlank()) username = jwt.getSubject();
        return ResponseEntity.ok(userService.uploadAvatar(username, file));
    }

    @GetMapping("/{id}/avatar")
    public ResponseEntity<byte[]> getAvatar(@PathVariable UUID id) {
        UserEntity user = userService.getAvatarEntity(id);
        MediaType mediaType =MediaType.APPLICATION_OCTET_STREAM;
        if (user.getAvatarContentType() != null) {
            try {
                mediaType = MediaType.parseMediaType(user.getAvatarContentType());
            } catch (Exception ignored) {}
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "max-age=3600")
                .contentType(mediaType)
                .body(user.getAvatarData());
    }
}
