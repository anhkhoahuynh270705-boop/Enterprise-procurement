package com.example.shopping.integration.keycloak.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import lombok.Getter;

/**
 * Keycloak configuration properties.
 *
 * <p>
 * Các giá trị được đọc từ application.properties:</p>
 * <pre>
 * keycloak.auth-server-url = http://localhost:8081
 * keycloak.realm            = Shopping
 * keycloak.client-id        = shopping-backend
 * keycloak.client-secret    = &lt;secret&gt;
 * keycloak.admin-username   = admin
 * keycloak.admin-password   = admin
 * </pre>
 */
@Getter
@Configuration
public class KeycloakConfig {

    @Value("${keycloak.auth-server-url:http://localhost:8081}")
    private String authServerUrl;

    @Value("${keycloak.realm:Shopping}")
    private String realm;

    @Value("${keycloak.client-id:shopping-backend}")
    private String clientId;

    @Value("${keycloak.client-secret:}")
    private String clientSecret;

    @Value("${keycloak.admin-username:admin}")
    private String adminUsername;

    @Value("${keycloak.admin-password:admin}")
    private String adminPassword;

    public String getIssuer() { 
        return authServerUrl + "/realms/" + realm; 
    }

    public String getTokenEndpoint() {
        return String.format("%s/realms/%s/protocol/openid-connect/token", authServerUrl, realm);
    }

    public String getLogoutEndpoint() {
        return String.format("%s/realms/%s/protocol/openid-connect/logout", authServerUrl, realm);
    }

    public String getAdminBaseUrl() {
        return String.format("%s/admin/realms/%s", authServerUrl, realm);
    }
}
