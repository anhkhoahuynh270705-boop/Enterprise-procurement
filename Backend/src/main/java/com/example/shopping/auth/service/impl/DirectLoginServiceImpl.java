package com.example.shopping.auth.service.impl;

import com.example.shopping.auth.model.DirectLoginResult;

import java.util.List;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.example.shopping.auth.dto.request.ChangePasswordRequestDto;
import com.example.shopping.auth.dto.request.LoginRequestDto;
import com.example.shopping.auth.dto.response.LoginResponseDto;
import com.example.shopping.auth.service.DirectLoginService;
import com.example.shopping.auth.service.LoginIdentityService;
import com.example.shopping.integration.keycloak.config.KeycloakConfig;
import com.example.shopping.integration.keycloak.service.KeycloakAdminService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class DirectLoginServiceImpl implements DirectLoginService {

    private final KeycloakConfig config;
    private final RestTemplate http;
    private final LoginIdentityService identities;
    private final KeycloakAdminService keycloak;
    private final ObjectMapper objectMapper;

    @Override
    public DirectLoginResult login(LoginRequestDto req) {
        var form = new LinkedMultiValueMap<String, String>();
        form.add(
                "grant_type",
                "password"
        );
        form.add(
                "client_id",
                config.getClientId()
        );
        if (config.getClientSecret() != null
                && !config.getClientSecret().isBlank()) {
            form.add(
                    "client_secret",
                    config.getClientSecret()
            );
        }
        form.add(
                "username",
                req.getUsername()
        );
        form.add(
                "password",
                req.getPassword()
        );
        form.add(
                "scope",
                "openid profile email"
        );
        var headers = new HttpHeaders();
        headers.setContentType(
                MediaType.APPLICATION_FORM_URLENCODED
        );
        LoginResponseDto tokens;
        try {
            tokens = http.postForObject(
                    config.getTokenEndpoint(),
                    new HttpEntity<>(
                            form,
                            headers
                    ),
                    LoginResponseDto.class
            );
        } catch (HttpClientErrorException.Unauthorized ex) {
            String body =
                    ex.getResponseBodyAsString();
            String reason =
                    extractKeycloakError(body);
            throw new IllegalArgumentException(
                    reason
            );
        } catch (HttpClientErrorException ex) {
            log.warn(
                    "Keycloak ROPC error: status={}, body={}",
                    ex.getStatusCode(),
                    ex.getResponseBodyAsString()
            );
            throw new IllegalArgumentException(
                    "Đăng nhập thất bại. Vui lòng thử lại."
            );
        }

        validateTokenResponse(tokens);
        List<String> actions =
                identities.extractRequiredActions(
                        tokens.getAccessToken()
                );

        if (actions.contains("UPDATE_PASSWORD")) {
            log.info(
                    "User cần đổi mật khẩu tạm.",
                    req.getUsername()
            );
            revokeRefreshToken(tokens);
            return DirectLoginResult.requireUpdatePassword(
                    req.getUsername()
            );
        }
        if (actions.contains("VERIFY_EMAIL")) {
            log.info(
                    "User {} cần xác minh email.",
                    req.getUsername()
            );
            sendVerifyEmail(
                    req.getUsername()
            );
            revokeRefreshToken(tokens);
            return DirectLoginResult.requireVerifyEmail();
        }

        try {
            identities.validateAccess(
                    tokens.getAccessToken()
            );
        } catch (RuntimeException ex) {
            revokeRefreshToken(tokens);
            throw ex;
        }
        return DirectLoginResult.success(tokens);
    }

    @Override
    public void changePassword( ChangePasswordRequestDto req ) {
        keycloak.changePasswordAndClearAction(
                req.getUsername(),
                req.getNewPassword()
        );
    }

    private void validateTokenResponse( LoginResponseDto tokens ) {
        if (tokens == null
                || tokens.getAccessToken() == null) {

            throw new IllegalArgumentException(
                    "Phản hồi xác thực không hợp lệ."
            );
        }
    }

    private void revokeRefreshToken(
            LoginResponseDto tokens
    ) {

        if (tokens != null
                && tokens.getRefreshToken() != null) {

            keycloak.revokeToken(
                    tokens.getRefreshToken()
            );
        }
    }

    private void sendVerifyEmail(
            String username
    ) {

        try {
            keycloak.sendVerifyEmail(username);

        } catch (Exception ex) {

            log.warn(
                    "Không thể gửi email xác minh cho {}: {}",
                    username,
                    ex.getMessage()
            );
        }
    }

    private String extractKeycloakError( String body) {
        try {
            JsonNode node =
                    objectMapper.readTree(body);

            String desc =
                    node.path("error_description")
                            .asText();
            if (desc != null
                    && !desc.isBlank()) {

                if (desc.contains(
                        "Invalid user credentials"
                ) || desc.contains(
                        "invalid_grant"
                )) {

                    return "Sai tên đăng nhập hoặc mật khẩu.";
                }

                if (desc.contains("disabled")) {

                    return "Tài khoản đã bị vô hiệu hóa.";
                }
            }

        } catch (Exception ignored) {
        }
        return "Sai tên đăng nhập hoặc mật khẩu.";
    }
}
