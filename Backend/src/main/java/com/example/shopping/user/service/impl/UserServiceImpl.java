package com.example.shopping.user.service.impl;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import com.example.shopping.user.dto.request.UpdateProfileRequestDto;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.example.shopping.common.enums.ErrorEnum;
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

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserServiceImpl implements UserService {

    UserRepository userRepository;
    PasswordEncoder passwordEncoder;
    UserMapper userMapper;
    KeycloakAdminService keycloakAdminService;

    @Override
    @Transactional 
    public UserResponseDto updateCurrentProfile(String username,
            UpdateProfileRequestDto request) {
        UserEntity user = userRepository.findByUsername(username)
                .filter(account -> !account.isDeleted() && Boolean.TRUE.equals(account.getEnabled()))
                .orElseThrow(() -> ErrorEnum.USER_PROFILE_NOT_FOUND.ex());
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
            throw ErrorEnum.USER_EMPLOYEE_CODE_EXISTS.ex();
        }
        String role = Role.parse(request.getRole()).name();
        if (userRepository.existsByUsername(request.getUsername())) {
            throw ErrorEnum.USER_USERNAME_EXISTS.ex("Username đã tồn tại: " + request.getUsername());
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw ErrorEnum.USER_EMAIL_EXISTS.ex("Email đã tồn tại: " + request.getEmail());
        }
        keycloakAdminService.createKeycloakUser(
                request.getUsername(),
                request.getEmail(),
                request.getPassword(),
                request.getFirstName(),
                request.getLastName()
        );

        UserEntity saved;
        try {
            keycloakAdminService.assignApplicationRole(request.getUsername(), role);
            if (Boolean.FALSE.equals(request.getEnabled())) {
                keycloakAdminService.updateUserEnabled(request.getUsername(), false);
            }

            UserEntity entity = userMapper.toEntity(request, passwordEncoder.encode(request.getPassword()));
            saved = userRepository.save(entity);
        } catch (RuntimeException ex) {
            try {
                keycloakAdminService.deleteKeycloakUser(request.getUsername());
            } catch (RuntimeException cleanupError) {
                ex.addSuppressed(cleanupError);
            }
            throw ex;
        }

        if (!Boolean.FALSE.equals(request.getEnabled())) {
            keycloakAdminService.sendVerifyEmail(request.getUsername());
        }
        log.info("Đã tạo user mới: {}", saved.getUsername());
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
                .orElseThrow(() -> ErrorEnum.USER_NOT_FOUND.ex());

        // Ticket ownership and the Keycloak account are tied to this immutable username.
        if (!user.getUsername().equals(request.getUsername())) {
            throw ErrorEnum.USER_CANNOT_CHANGE_USERNAME.ex();
        }

        if (!user.getUsername().equals(request.getUsername())
                && userRepository.existsByUsername(request.getUsername())) {
            throw ErrorEnum.USER_USERNAME_EXISTS.ex("Username đã tồn tại: " + request.getUsername());
        }
        if (!user.getEmail().equals(request.getEmail())
                && userRepository.existsByEmail(request.getEmail())) {
            throw ErrorEnum.USER_EMAIL_EXISTS.ex("Email đã tồn tại: " + request.getEmail());
        }

        String employeeId = optionalText(request.getEmployeeId());
        if (employeeId != null && userRepository.existsByEmployeeIdAndIdNot(employeeId, id)) {
            throw ErrorEnum.USER_EMPLOYEE_CODE_EXISTS.ex();
        }

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
                throw ErrorEnum.USER_CANNOT_DISABLE_SELF.ex();
            }
            user.setEnabled(request.getEnabled());
            try {
                keycloakAdminService.updateUserEnabled(user.getUsername(), request.getEnabled());
            } catch (Exception e) {
                log.error("Lỗi khi cập nhật trạng thái cho user: ", user.getUsername(), e.getMessage());
            }
        }
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            keycloakAdminService.changePasswordAndClearAction(user.getUsername(), request.getPassword());
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        UserEntity saved = userRepository.save(user);
        return userMapper.toDto(saved);
    }

    @Override
    public void deleteUser(UUID id) {
        UserEntity user = userRepository.findById(id).filter(account -> !account.isDeleted()).orElse(null);
        if (user == null) {
            log.warn("User id {} không tồn tại hoặc đã bị xoá", id);
            return;
        }
        if (isSelf(user)) {
            throw ErrorEnum.USER_CANNOT_DELETE_SELF.ex();
        }
        keycloakAdminService.deleteKeycloakUser(user.getUsername());

        // Soft-delete trong DB
        user.setEnabled(false);
        user.setDeleted(true);
        userRepository.save(user);
        log.info("Đã xoá user: {} (id: {})", user.getUsername(), id);
    }
    /* Cannot Delete self account */
    @Override
    public UserResponseDto toggleUserEnabled(UUID id) {
        UserEntity user = userRepository.findById(id).filter(account -> !account.isDeleted())
                .orElseThrow(() -> ErrorEnum.USER_NOT_FOUND.ex());

        if (Boolean.TRUE.equals(user.getEnabled()) && isSelf(user)) {
            throw ErrorEnum.USER_CANNOT_DISABLE_SELF.ex();
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

    @Override
    @Transactional
    public UserResponseDto uploadAvatar(String username, MultipartFile file) {
        UserEntity user = userRepository.findByUsername(username)
                .filter(account -> !account.isDeleted() && Boolean.TRUE.equals(account.getEnabled()))
                .orElseThrow(() -> ErrorEnum.USER_PROFILE_NOT_FOUND.ex());

        return storeAvatar(user, file);
    }

    private UserResponseDto storeAvatar(UserEntity user, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw ErrorEnum.USER_AVATAR_EMPTY.ex();
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw ErrorEnum.USER_AVATAR_INVALID_TYPE.ex();
        }
        if (file.getSize() > 5 * 1024 * 1024) {
            throw ErrorEnum.USER_AVATAR_TOO_LARGE.ex();
        }

        try {
            user.setAvatarData(file.getBytes());
            user.setAvatarContentType(contentType);
            user.setAvatarUrl("/api/users/" + user.getId() + "/avatar");
            UserEntity saved = userRepository.save(user);
            return userMapper.toDto(saved);
        } catch (java.io.IOException e) {
            log.error("Lỗi khi đọc file avatar cho user {}", user.getUsername(), e);
            throw ErrorEnum.USER_AVATAR_READ_ERROR.ex(e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public UserEntity getAvatarEntity(UUID userId) {
        return userRepository.findById(userId)
                .filter(account -> !account.isDeleted() && account.getAvatarData() != null)
                .orElseThrow(() -> ErrorEnum.USER_AVATAR_NOT_FOUND.ex());
    }
}
