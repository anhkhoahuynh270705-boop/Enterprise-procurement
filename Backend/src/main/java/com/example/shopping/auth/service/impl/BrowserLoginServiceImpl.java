package com.example.shopping.auth.service.impl;

import com.example.shopping.auth.model.BrowserLoginStart;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.example.shopping.auth.dto.response.LoginResponseDto;
import com.example.shopping.auth.service.AuthorizationTransactions;
import com.example.shopping.auth.service.BrowserLoginService;
import com.example.shopping.auth.service.LoginIdentityService;
import com.example.shopping.integration.keycloak.config.KeycloakConfig;
import com.example.shopping.integration.keycloak.service.KeycloakAdminService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BrowserLoginServiceImpl implements BrowserLoginService {

    private final AuthorizationTransactions transactions;
    private final KeycloakConfig config;
    private final RestTemplate http;
    private final LoginIdentityService identities;
    private final KeycloakAdminService keycloak;

    @Value("${auth.oidc.callback-uri:http://localhost:8080/api/auth/callback}")
    private String callbackUri;

    /* tx inclue
    * state: Compare callback state with the one in the request
    * browserToken: constraint to the browser session
    * verifier: used to generate the code challenge
    * nonce: used to validate the ID token
    * expiresAt: used to expire the transaction after a certain time
    */
    @Override
    public BrowserLoginStart begin() {
        var tx = transactions.create();
        try {
            // Tạo PKCE code challenge từ code verifier
            String challenge = Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(
                            MessageDigest
                                    .getInstance("SHA-256")
                                    .digest(
                                            tx.verifier()
                                                    .getBytes(StandardCharsets.US_ASCII)
                                    )
                    );

            String url = UriComponentsBuilder
                    .fromUriString(
                            config.getIssuer()
                                    + "/protocol/openid-connect/auth"
                    )
                    .queryParam(
                            "client_id",
                            config.getClientId()
                    )
                    .queryParam(
                            "response_type",
                            "code"
                    )
                    .queryParam(
                            "scope",
                            "openid profile email"
                    )
                    .queryParam(
                            "redirect_uri",
                            callbackUri
                    )
                    .queryParam(
                            "state",
                            tx.state()
                    )
                    .queryParam(
                            "nonce",
                            tx.nonce()
                    )
                    .queryParam(
                            "code_challenge",
                            challenge
                    )
                    .queryParam(
                            "code_challenge_method",
                            "S256"
                    )
                    .queryParam(
                            "prompt",
                            "login"
                    )
                    .build()
                    .encode()
                    .toUriString();

            return new BrowserLoginStart(
                    url,
                    tx.browserToken()
            );

        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }

    @Override
    public LoginResponseDto complete(
            String state,
            String browserToken,
            String code,
            String error
    ) {

        var tx = transactions.consume(
                state,
                browserToken
        );

        if (error != null
                || code == null
                || code.isBlank()) {

            throw new IllegalArgumentException(
                    "Login Failed"
            );
        }

        var form =
                new LinkedMultiValueMap<String, String>();

        form.add(
                "grant_type",
                "authorization_code"
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
                "code",
                code
        );

        form.add(
                "code_verifier",
                tx.verifier()
        );

        form.add(
                "redirect_uri",
                callbackUri
        );

        var headers = new HttpHeaders();

        headers.setContentType(
                MediaType.APPLICATION_FORM_URLENCODED
        );

        LoginResponseDto tokens =
                http.postForObject(
                        config.getTokenEndpoint(),
                        new HttpEntity<>(
                                form,
                                headers
                        ),
                        LoginResponseDto.class
                );

        try {

            validateTokenResponse(tokens);

            var access = identities.validateAccess(
                    tokens.getAccessToken()
            );

            identities.validateId(
                    tokens.getIdToken(),
                    tx.nonce(),
                    access.getSubject()
            );

            return tokens;

        } catch (RuntimeException ex) {

            revokeRefreshToken(tokens);

            throw ex;
        }
    }

    private void validateTokenResponse(
            LoginResponseDto tokens
    ) {

        if (tokens == null
                || tokens.getAccessToken() == null
                || tokens.getRefreshToken() == null
                || tokens.getIdToken() == null) {

            throw new IllegalArgumentException(
                    "Incomplete token response"
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
}
