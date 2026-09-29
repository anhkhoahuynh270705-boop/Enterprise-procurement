package com.example.shopping.auth.controller;

import com.example.shopping.common.dto.response.MessageResponseDto;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.shopping.auth.dto.request.ForgotPasswordRequestDto;
import com.example.shopping.auth.dto.response.LoginResponseDto;
import com.example.shopping.auth.helper.AuthHttpHelper;
import com.example.shopping.auth.service.AuthService;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final AuthHttpHelper authHttpHelper;

    @PostMapping("/refresh")
    public ResponseEntity<LoginResponseDto> refresh( @CookieValue(value = AuthHttpHelper.REFRESH_COOKIE, required = false ) String refreshToken,
            @RequestHeader(
                    value = "X-Requested-With",
                    required = false
            )
            String requestedWith) {

        authHttpHelper.requireBrowserHeader(
                requestedWith
        );

        if (refreshToken == null
                || refreshToken.isBlank()) {

            return authHttpHelper.unauthorized();
        }

        try {

            LoginResponseDto tokens =
                    authService.refreshToken(
                            refreshToken
                    );

            return authHttpHelper.tokenResponse(
                    tokens
            );

        } catch (IllegalArgumentException ex) {

            return authHttpHelper.unauthorized();
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<MessageResponseDto> logout(
            @CookieValue(
                    value = AuthHttpHelper.REFRESH_COOKIE,
                    required = false
            )
            String refreshToken,

            @RequestHeader(
                    value = "X-Requested-With",
                    required = false
            )
            String requestedWith,

            HttpServletResponse response) {

        authHttpHelper.requireBrowserHeader(
                requestedWith
        );

        response.addHeader(
                HttpHeaders.SET_COOKIE,
                authHttpHelper.refreshCookie("", 0)
        );

        response.setHeader(
                HttpHeaders.CACHE_CONTROL,
                "no-store"
        );

        if (refreshToken != null
                && !refreshToken.isBlank()) {

            authService.logout(
                    refreshToken
            );
        }

        return ResponseEntity.ok(
                new MessageResponseDto("Logged out")
        );
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<MessageResponseDto> forgotPassword(
            @Valid
            @RequestBody
            ForgotPasswordRequestDto request) {

        authService.forgotPassword(
                request
        );
        return ResponseEntity.ok(
                new MessageResponseDto("Nếu email tồn tại, link đặt lại mật khẩu đã được gửi.")
        );
    }
}
