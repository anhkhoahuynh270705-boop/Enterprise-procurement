package com.example.shopping.auth.service.impl;

import com.example.shopping.common.enums.ErrorEnum;
import com.example.shopping.auth.dto.request.ForgotPasswordRequestDto;
import com.example.shopping.auth.dto.response.LoginResponseDto;
import com.example.shopping.auth.service.AuthService;
import com.example.shopping.auth.service.LoginIdentityService;
import com.example.shopping.integration.keycloak.config.KeycloakConfig;
import com.example.shopping.integration.keycloak.service.KeycloakAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestTemplate;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthServiceImpl implements AuthService {
    RestTemplate restTemplate;
    KeycloakAdminService keycloakAdminService;
    KeycloakConfig keycloakConfig;
    LoginIdentityService identities;

    @Override 
    public void logout(String refreshToken) {
        keycloakAdminService.revokeToken(refreshToken);
    }
    
    @Override 
    public LoginResponseDto refreshToken(String refreshToken) {
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        var body = new LinkedMultiValueMap<String, String>();
        body.add("grant_type", "refresh_token");
        body.add("client_id", keycloakConfig.getClientId());
        if (keycloakConfig.getClientSecret() != null && !keycloakConfig.getClientSecret().isBlank())
            body.add("client_secret", keycloakConfig.getClientSecret());
        body.add("refresh_token", refreshToken);
        LoginResponseDto tokens = null;
        try {
            tokens = restTemplate.postForObject(keycloakConfig.getTokenEndpoint(), new HttpEntity<>(body, headers), LoginResponseDto.class);
            if (tokens == null || tokens.getAccessToken() == null || tokens.getRefreshToken() == null)
                throw ErrorEnum.INVALID_REFRESH_RESPONSE.ex();
            identities.validateAccess(tokens.getAccessToken());
            return tokens;
        } catch (RuntimeException ex) {
            if (tokens != null && tokens.getRefreshToken() != null)
                keycloakAdminService.revokeToken(tokens.getRefreshToken());
            throw ErrorEnum.REFRESH_TOKEN_INVALID.ex();
        }
    }
    
    @Override 
    public void forgotPassword(ForgotPasswordRequestDto request) {
        keycloakAdminService.sendForgotPasswordEmail(request.getEmail());
    }
}
