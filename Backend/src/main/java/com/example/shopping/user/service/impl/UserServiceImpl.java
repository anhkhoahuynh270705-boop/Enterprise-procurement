package com.example.shopping.user.service.impl;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.example.shopping.common.enums.Role;

import com.example.shopping.integration.keycloak.service.KeycloakAdminService;
import com.example.shopping.user.dto.request.CreateUserRequestDto;
import com.example.shopping.user.dto.request.UpdateUserRequestDto;
import com.example.shopping.user.dto.response.UserResponseDto;
import com.example.shopping.user.entity.UserEntity;
import com.example.shopping.user.mapper.UserMapper;
import com.example.shopping.user.repository.UserRepository;
import com.example.shopping.user.service.UserService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final KeycloakAdminService keycloakAdminService;

    @Override
    @Transactional 
    public UserResponseDto updateCurrentProfile(String username,
            com.example.shopping.user.dto.request.UpdateProfileRequestDto request) {
        UserEntity user = userRepository.findByUsername(username)
                .filter(account -> !account.isDeleted() && Boolean.TRUE.equals(account.getEnabled()))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Không tìm thấy hồ sơ."));
        user.setAvatarUrl(optionalText(request.getAvatarUrl()));
        user.setPhone(optionalText(request.getPhone()));
        user.setDateOfBirth(request.getDateOfBirth());
        user.setGender(optionalText(request.getGender()));
        user.setAddress(optionalText(request.getAddress()));
        return userMapper.toDto(userRepository.save(user));
    }

    private static String optionalText(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    @Override
    public List<UserResponseDto> getAllUsers() {
        return userRepository.findAll()
                            .stream()
                            .filter(user -> !user.isDeleted())
                            .map(userMapper::toDto)
                            .collect(Collectors.toList());
    }

    @Override
    public Optional<UserResponseDto> getUserById(UUID id) {
        return userRepository.findById(id).filter(user -> !user.isDeleted()).map(userMapper::toDto);
    }

    @Override
    public Optional<UserResponseDto> getUserByUsername(String username) {
        return userRepository.findByUsername(username).filter(user -> !user.isDeleted()).map(userMapper::toDto);
    }

    @Override
    public UserResponseDto createUser(CreateUserRequestDto request) {
        request.setEmployeeId(optionalText(request.getEmployeeId()));
        if (request.getEmployeeId() != null && userRepository.existsByEmployeeId(request.getEmployeeId())) {
            throw new IllegalArgumentException("Mã nhân viên đã tồn tại.");
        }
        String role = Role.parse(request.getRole()).name();
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username đã tồn tại: " + request.getUsername());
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email đã tồn tại: " + request.getEmail());
        }
        keycloakAdminService.createKeycloakUser(
                request.getUsername(),
                request.getEmail(),
                request.getPassword(),
                request.getFirstName(),
                request.getLastName()
        );

        try {
            keycloakAdminService.assignApplicationRole(request.getUsername(), role);
            if (Boolean.FALSE.equals(request.getEnabled())) keycloakAdminService.updateUserEnabled(request.getUsername(), false);
        } catch (RuntimeException ex) {
            try {
                keycloakAdminService.deleteKeycloakUser(request.getUsername());
            } catch (RuntimeException cleanupError) {
                ex.addSuppressed(cleanupError);
            }
            throw ex;
        }
        UserEntity entity = userMapper.toEntity(request, passwordEncoder.encode(request.getPassword()));
        UserEntity saved = userRepository.save(entity);
        log.info("Đã tạo user mới: ", saved.getUsername());
        return userMapper.toDto(saved);
    }

    private String getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthenticationToken jwtAuth) {
            String username = jwtAuth.getToken().getClaimAsString("preferred_username");
            if (username != null && !username.isBlank()) {
                return username;
            }
            return jwtAuth.getName();
        }
        if (auth != null) {
            return auth.getName();
        }
        return null;
    }

    private boolean isSelf(UserEntity user) {
        String currentUsername = getCurrentUsername();
        if (currentUsername == null || user == null) {
            return false;
        }
        return currentUsername.equalsIgnoreCase(user.getUsername())
                || currentUsername.equalsIgnoreCase(user.getEmail());
    }

    @Override
    @Transactional
    public UserResponseDto updateUser(UUID id, UpdateUserRequestDto request) {
        UserEntity user = userRepository.findById(id).filter(account -> !account.isDeleted())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy user với id: " + id));

        // Ticket ownership and the Keycloak account are tied to this immutable username.
        if (!user.getUsername().equals(request.getUsername())) {
            throw new IllegalArgumentException("Không được thay đổi username của tài khoản.");
        }

        if (!user.getUsername().equals(request.getUsername())
                && userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username đã tồn tại: " + request.getUsername());
        }
        if (!user.getEmail().equals(request.getEmail())
                && userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email đã tồn tại: " + request.getEmail());
        }

        String employeeId = optionalText(request.getEmployeeId());
        if (employeeId != null && userRepository.existsByEmployeeIdAndIdNot(employeeId, id)) {
            throw new IllegalArgumentException("Mã nhân viên đã tồn tại.");
        }

        user.setAvatarUrl(optionalText(request.getAvatarUrl()));
        user.setPhone(optionalText(request.getPhone()));
        user.setDateOfBirth(request.getDateOfBirth());
        user.setGender(optionalText(request.getGender()));
        user.setAddress(optionalText(request.getAddress()));
        user.setEmployeeId(optionalText(request.getEmployeeId()));
        user.setDepartment(optionalText(request.getDepartment()));
        user.setJobTitle(optionalText(request.getJobTitle()));
        user.setOfficeLocation(optionalText(request.getOfficeLocation()));
        user.setManager(optionalText(request.getManager()));
        if (request.getEmployeeStatus() != null) user.setEmployeeStatus(request.getEmployeeStatus());
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        boolean nameChanged = !Objects.equals(user.getFirstName(), request.getFirstName())
                || !Objects.equals(user.getLastName(), request.getLastName());
        String requestedName = request.getFullName() != null ? request.getFullName()
                : (nameChanged ? null : user.getFullName());
        user.setFullName(com.example.shopping.user.normalization.UserNameNormalizer.resolveFullName(requestedName, request.getFirstName(), request.getLastName()));
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());

        if (request.getRole() != null && !request.getRole().isBlank()) {
            String role = Role.parse(request.getRole()).name();
            keycloakAdminService.assignApplicationRole(user.getUsername(), role);
            user.setRole(role);
        }
        if (request.getEnabled() != null) {
            if (Boolean.FALSE.equals(request.getEnabled()) && isSelf(user)) {
                throw new IllegalArgumentException("Bạn không thể tự vô hiệu hóa tài khoản của chính mình.");
            }
            user.setEnabled(request.getEnabled());
            try {
                keycloakAdminService.updateUserEnabled(user.getUsername(), request.getEnabled());
            } catch (Exception e) {
                log.error("Lỗi khi cập nhật trạng thái Keycloak cho user: ", user.getUsername(), e.getMessage());
            }
        }
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            keycloakAdminService.resetTemporaryPassword(user.getUsername(), request.getPassword());
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        UserEntity saved = userRepository.save(user);
        return userMapper.toDto(saved);
    }

    @Override
    public void deleteUser(UUID id) {
        UserEntity user = userRepository.findById(id).filter(account -> !account.isDeleted()).orElse(null);
        if (user == null) {
            log.warn("User id {} không tồn tại", id);
            return;
        }
        if (isSelf(user)) {
            throw new IllegalArgumentException("Bạn không thể tự xóa tài khoản của chính mình.");
        }
        // Keep the identity reserved: tickets are owned by the immutable username.
        keycloakAdminService.updateUserEnabled(user.getUsername(), false);
        user.setEnabled(false);
        user.setDeleted(true);
        userRepository.save(user);
        log.info("Đã xoá user:", user.getUsername(), id);
    }
    /* Cannot Delete self account */
    @Override
    public UserResponseDto toggleUserEnabled(UUID id) {
        UserEntity user = userRepository.findById(id).filter(account -> !account.isDeleted())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy user với id: " + id));

        if (Boolean.TRUE.equals(user.getEnabled()) && isSelf(user)) {
            throw new IllegalArgumentException("Bạn không thể tự vô hiệu hóa tài khoản của chính mình.");
        }

        boolean newStatus = !Boolean.TRUE.equals(user.getEnabled());
        user.setEnabled(newStatus);
        UserEntity saved = userRepository.save(user);

        try {
            keycloakAdminService.updateUserEnabled(user.getUsername(), newStatus);
        } catch (Exception e) {
            log.error("Lỗi khi cập nhật trạng thái Keycloak cho user: ", user.getUsername(), e.getMessage());
        }

        log.info("Đã user id: ", newStatus ? "kích hoạt" : "vô hiệu hoá", id);
        return userMapper.toDto(saved);
    }
}
