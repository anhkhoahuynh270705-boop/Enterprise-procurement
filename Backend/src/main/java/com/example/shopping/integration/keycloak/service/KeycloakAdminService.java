package com.example.shopping.integration.keycloak.service;

public interface KeycloakAdminService {
    void assignApplicationRole(String username, String applicationRole);
    void createKeycloakUser(String username, String email, String plainPassword, String firstName, String lastName);
    void sendForgotPasswordEmail(String email);
    void updateUserEnabled(String username, boolean enabled);
    void resetTemporaryPassword(String username, String newPassword);
    void changePasswordAndClearAction(String username, String newPassword);
    void sendVerifyEmail(String username);
    void deleteKeycloakUser(String username);
    void revokeToken(String refreshToken);
}
