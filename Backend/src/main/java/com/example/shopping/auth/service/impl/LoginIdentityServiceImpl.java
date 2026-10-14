package com.example.shopping.auth.service.impl;

import com.example.shopping.common.enums.ErrorEnum;
import com.example.shopping.auth.service.LoginIdentityService;

import com.example.shopping.integration.keycloak.config.KeycloakConfig;
import com.example.shopping.user.repository.UserRepository;
import com.example.shopping.user.entity.UserEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class LoginIdentityServiceImpl implements LoginIdentityService {
    JwtDecoder decoder;
    KeycloakConfig config;
    UserRepository users;

    public Jwt validateAccess(String token) {
        Jwt jwt = decoder.decode(token);
        if (!config.getIssuer().equals(jwt.getClaimAsString("iss")) ||
                !config.getClientId().equals(jwt.getClaimAsString("azp")))
            throw ErrorEnum.INVALID_TOKEN_ISSUER.ex();
        String username = jwt.getClaimAsString("preferred_username");
        UserEntity user = username == null ? null : users.findByUsername(username).orElse(null);
        if (user == null && Boolean.TRUE.equals(jwt.getClaimAsBoolean("email_verified"))) {
            String email = jwt.getClaimAsString("email");
            if (email != null && !email.isBlank()) {
                user = users.findByEmail(email).orElse(null);
                if (user != null) {
                    log.info("OIDC login matched application user using verified email because preferred_username did not match.");
                }
            }
        }
        if (user == null) {
            log.warn("OIDC login rejected: application user was not found.");
            throw ErrorEnum.ACCOUNT_UNAVAILABLE.ex();
        }
        if (user.isDeleted() || !Boolean.TRUE.equals(user.getEnabled())) {
            log.warn("OIDC login rejected: application user is deleted or disabled (deleted={}, enabled={}).",
                    user.isDeleted(), user.getEnabled());
            throw ErrorEnum.ACCOUNT_UNAVAILABLE.ex();
        }
        Boolean emailVerified = jwt.getClaimAsBoolean("email_verified");
        if (user.isEmailVerificationRequired() && !Boolean.TRUE.equals(emailVerified)) {
            log.warn("OIDC login rejected: email verification is still required by the application and the access-token claim is {}.",
                    emailVerified);
            throw ErrorEnum.ACCOUNT_UNAVAILABLE.ex();
        }
        return jwt;
    }

    public void validateId(String token, String nonce, String subject) {
        Jwt jwt = decoder.decode(token);
        if (!config.getIssuer().equals(jwt.getClaimAsString("iss")) || !jwt.getAudience().contains(config.getClientId()) ||
                !nonce.equals(jwt.getClaimAsString("nonce")) || !subject.equals(jwt.getSubject()) ||
                (jwt.hasClaim("azp") && !config.getClientId().equals(jwt.getClaimAsString("azp"))))
            throw ErrorEnum.INVALID_IDENTITY_TOKEN.ex();
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
