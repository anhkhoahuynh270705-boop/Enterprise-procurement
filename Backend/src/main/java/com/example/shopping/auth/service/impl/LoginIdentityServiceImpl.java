package com.example.shopping.auth.service.impl;

import com.example.shopping.auth.service.LoginIdentityService;

import com.example.shopping.integration.keycloak.config.KeycloakConfig;
import com.example.shopping.user.repository.UserRepository;
import com.example.shopping.user.entity.UserEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LoginIdentityServiceImpl implements LoginIdentityService {
    private final JwtDecoder decoder;
    private final KeycloakConfig config;
    private final UserRepository users;

    public Jwt validateAccess(String token) {
        Jwt jwt = decoder.decode(token);
        if (!config.getIssuer().equals(jwt.getClaimAsString("iss")) ||
                !config.getClientId().equals(jwt.getClaimAsString("azp")))
            throw new IllegalArgumentException("Invalid token issuer or client");
        String username = jwt.getClaimAsString("preferred_username");
        UserEntity user = username == null ? null : users.findByUsername(username).orElse(null);
        if (user == null || user.isDeleted() || !Boolean.TRUE.equals(user.getEnabled()) ||
                (user.isEmailVerificationRequired() && !Boolean.TRUE.equals(jwt.getClaimAsBoolean("email_verified"))))
            throw new IllegalArgumentException("Account is unavailable or email is unverified");
        return jwt;
    }

    public void validateId(String token, String nonce, String subject) {
        Jwt jwt = decoder.decode(token);
        if (!config.getIssuer().equals(jwt.getClaimAsString("iss")) || !jwt.getAudience().contains(config.getClientId()) ||
                !nonce.equals(jwt.getClaimAsString("nonce")) || !subject.equals(jwt.getSubject()) ||
                (jwt.hasClaim("azp") && !config.getClientId().equals(jwt.getClaimAsString("azp"))))
            throw new IllegalArgumentException("Invalid identity token");
    }

    /**
     * Dùng để phát hiện UPDATE_PASSWORD / VERIFY_EMAIL trước khi xác thực đầy đủ.
     */
    @SuppressWarnings("unchecked")
    public List<String> extractRequiredActions(String token) {
        try {
            Jwt jwt = decoder.decode(token);
            Object actions = jwt.getClaims().get("required_actions");
            if (actions instanceof List<?> list) {
                return (
                    List<String>
                    ) list;
            }
        } catch (Exception ignored) { }
        return Collections.emptyList();
    }
}
