package com.example.shopping.auth.helper;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import com.example.shopping.auth.dto.response.LoginResponseDto;

@Component
public class AuthHttpHelper {

    public static final String REFRESH_COOKIE = "refresh_token";

    @Value("${auth.cookie.secure:true}")
    private boolean secureCookie;

    public ResponseEntity<LoginResponseDto> tokenResponse(
            LoginResponseDto tokens) {

        if (tokens == null
                || tokens.getAccessToken() == null
                || tokens.getRefreshToken() == null) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Invalid authentication response"
            );
        }

        long lifetime =
                tokens.getRefreshExpiresIn() != null
                && tokens.getRefreshExpiresIn() > 0
                        ? tokens.getRefreshExpiresIn()
                        : -1;

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CACHE_CONTROL,
                        "no-store"
                )
                .header(
                        HttpHeaders.PRAGMA,
                        "no-cache"
                )
                .header(
                        HttpHeaders.SET_COOKIE,
                        refreshCookie(
                                tokens.getRefreshToken(),
                                lifetime
                        )
                )
                .body(tokens);
    }

    public ResponseEntity<LoginResponseDto> unauthorized() {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .header(
                        HttpHeaders.CACHE_CONTROL,
                        "no-store"
                )
                .header(
                        HttpHeaders.SET_COOKIE,
                        refreshCookie("", 0)
                )
                .build();
    }

    public String refreshCookie(
            String value,
            long maxAge) {

        return ResponseCookie
                .from(REFRESH_COOKIE, value)
                .httpOnly(true)
                .secure(secureCookie)
                .sameSite("Strict")
                .path("/api/auth")
                .maxAge(Duration.ofSeconds(maxAge))
                .build()
                .toString();
    }

    public void requireBrowserHeader(
            String requestedWith) {
        if (!"XMLHttpRequest".equals(requestedWith)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Missing browser request header"
            );
        }
    }
}