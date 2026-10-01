package com.example.shopping.integration.keycloak.service.impl;

import com.example.shopping.integration.keycloak.service.KeycloakAdminService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import com.example.shopping.integration.keycloak.config.KeycloakConfig;
import com.example.shopping.common.enums.Role;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service tương tác với Keycloak Admin REST API. 
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KeycloakAdminServiceImpl implements KeycloakAdminService {

    private final RestTemplate restTemplate;
    private final KeycloakConfig keycloakConfig;

    /** Replace application roles only; retain Keycloak's unrelated system roles. */
    public void assignApplicationRole(String username, String applicationRole) {
        String roleName = Role.parse(applicationRole).name();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(getAdminToken());
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Void> request = new HttpEntity<>(headers);
        String base = keycloakConfig.getAdminBaseUrl();
        var searchUri = UriComponentsBuilder.fromUriString(base + "/users")
            .queryParam("username", "{username}").queryParam("exact", true).buildAndExpand(username).encode().toUri();
        var users = restTemplate.exchange(searchUri, HttpMethod.GET, request,
            new ParameterizedTypeReference<List<Map<String, Object>>>() { }).getBody();
        if (users == null || users.size() != 1) {
            throw new IllegalStateException("Cannot find the Keycloak account for role assignment.");
        }
        String roleUrl = base + "/roles/" + roleName;
        Map<String, Object> role;
        try {
            role = restTemplate.exchange(roleUrl, HttpMethod.GET, request,
                new ParameterizedTypeReference<Map<String, Object>>() { }).getBody();
        } catch (HttpClientErrorException.NotFound ex) {
            try {
                restTemplate.postForEntity(base + "/roles", new HttpEntity<>(Map.of("name", roleName), headers), Void.class);
            } catch (HttpClientErrorException.Conflict alreadyCreated) {
            }
            role = restTemplate.exchange(roleUrl, HttpMethod.GET, request,
                new ParameterizedTypeReference<Map<String, Object>>() { }).getBody();
        }
        if (role == null) 
            throw new IllegalStateException("Keycloak returned an empty role.");
        String mappings = base + "/users/" + users.get(0).get("id") + "/role-mappings/realm";
        var current = restTemplate.exchange(mappings, HttpMethod.GET, request,
            new ParameterizedTypeReference<List<Map<String, Object>>>() { }).getBody();
        var applicationRoles = java.util.Set.of("ADMIN", "USER", "CHECKER", "MAKER", "ROLE_ADMIN", "ROLE_USER", "ROLE_CHECKER", "ROLE_MAKER");
        var removed = current == null ? List.<Map<String, Object>>of() : current.stream()
            .filter(value -> applicationRoles.contains(value.get("name")) && !roleName.equals(value.get("name"))).toList();
        if (!removed.isEmpty()) {
            restTemplate.exchange(mappings, HttpMethod.DELETE, new HttpEntity<>(removed, headers), Void.class);
        }
        restTemplate.postForEntity(mappings, new HttpEntity<>(List.of(role), headers), Void.class);
    }

    private String getAdminToken() {
        String tokenEndpoint
                = keycloakConfig.getAuthServerUrl() + "/realms/master/protocol/openid-connect/token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "password");
        body.add("client_id", "admin-cli");
        body.add("username", keycloakConfig.getAdminUsername());
        body.add("password", keycloakConfig.getAdminPassword());

        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(body, headers);

        try {
            @SuppressWarnings("unchecked")
            ResponseEntity<Map<String, Object>> response
                    = restTemplate.exchange(tokenEndpoint, HttpMethod.POST, entity,
                            new ParameterizedTypeReference<Map<String, Object>>() {
                    });
            return (String) response.getBody().get("access_token");
        } catch (Exception e) {
            log.error("Không thể lấy Keycloak admin token", e);
            throw new IllegalStateException("Không thể kết nối đến Authentication Server.");
        }
    }

    /**
     * Tạo user mới trong Keycloak với mật khẩu được hash bởi Keycloak.
     */
    public void createKeycloakUser(String username, String email,
            String plainPassword,
            String firstName, String lastName) {
        String adminToken = getAdminToken();
        String createUserUrl = keycloakConfig.getAdminBaseUrl() + "/users";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(adminToken);

        Map<String, Object> credential = new HashMap<>();
        credential.put("type", "password");
        credential.put("value", plainPassword);
        credential.put("temporary", true);

        Map<String, Object> userBody = new HashMap<>();
        userBody.put("username", username);
        userBody.put("email", email);
        userBody.put("firstName", firstName != null ? firstName : "");
        userBody.put("lastName", lastName != null ? lastName : "");
        userBody.put("enabled", true);
        userBody.put("emailVerified", false);
        userBody.put("requiredActions", List.of("UPDATE_PASSWORD", "VERIFY_EMAIL"));
        userBody.put("credentials", List.of(credential));

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(userBody, headers);

        try {
            restTemplate.postForEntity(createUserUrl, entity, String.class);
            log.info("Đã tạo Keycloak user: {}", username);
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.CONFLICT) {
                throw new IllegalArgumentException("User đã tồn tại trong Keycloak: " + username);
            }
            log.error("Tạo Keycloak user thất bại:", e.getStatusCode(), e.getResponseBodyAsString());
            throw new IllegalStateException("Không thể tạo tài khoản: " + e.getMessage());
        }
    }

    /**
     * Tìm user theo email và gửi email đặt lại mật khẩu qua Keycloak.
     */
    public void sendForgotPasswordEmail(String email) {
        String adminToken = getAdminToken();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        String searchUrl = keycloakConfig.getAdminBaseUrl()
                + "/users?email=" + email + "&exact=true";

        HttpEntity<Void> getEntity = new HttpEntity<>(headers);

        try {
            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                    searchUrl, HttpMethod.GET, getEntity,
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {
            });

            List<Map<String, Object>> users = response.getBody();
            if (users == null || users.isEmpty()) {
                log.info("Forgot password: email không tồn tại", email);
                return;
            }

            String userId = (String) users.get(0).get("id");

            // Gửi email thực hiện action UPDATE_PASSWORD kèm redirect_uri về trang login
            String executeActionsUrl = UriComponentsBuilder
                    .fromUriString(keycloakConfig.getAdminBaseUrl() + "/users/" + userId + "/execute-actions-email")
                    .queryParam("client_id", keycloakConfig.getClientId())
                    .queryParam("redirect_uri", "http://localhost:4200/login")
                    .toUriString();

            HttpEntity<List<String>> actionsEntity
                    = new HttpEntity<>(List.of("UPDATE_PASSWORD"), headers);
            restTemplate.postForEntity(executeActionsUrl, actionsEntity, String.class);

            log.info("Đã gửi email đặt lại mật khẩu tới:", email);

        } catch (Exception e) {
            log.error("Gửi email forgot password thất bại", e);
            throw new IllegalStateException("Không thể gửi email đặt lại mật khẩu. Vui lòng thử lại.");
        }
    }

    public void updateUserEnabled(String username, boolean enabled) {
        String adminToken = getAdminToken();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        String searchUrl = keycloakConfig.getAdminBaseUrl()
                + "/users?username=" + username + "&exact=true";

        HttpEntity<Void> getEntity = new HttpEntity<>(headers);

        try {
            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                    searchUrl, HttpMethod.GET, getEntity,
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {
            });

            List<Map<String, Object>> users = response.getBody();
            if (users == null || users.isEmpty()) {
                log.warn("Keycloak: Không tìm thấy user với username", username);
                return;
            }

            String keycloakUserId = (String) users.get(0).get("id");

            String updateUrl = keycloakConfig.getAdminBaseUrl() + "/users/" + keycloakUserId;
            Map<String, Object> updateBody = new HashMap<>();
            updateBody.put("enabled", enabled);
            HttpEntity<Map<String, Object>> updateEntity = new HttpEntity<>(updateBody, headers);
            restTemplate.exchange(updateUrl, HttpMethod.PUT, updateEntity, Void.class);

            if (!enabled) {
                try {
                    String logoutUrl = keycloakConfig.getAdminBaseUrl() + "/users/" + keycloakUserId + "/logout";
                    restTemplate.postForEntity(logoutUrl, new HttpEntity<>(headers), Void.class);
                    log.info("Đã đăng xuất các session trên Keycloak cho user: {}", username);
                } catch (Exception e) {
                    log.warn("Không thể logout session Keycloak cho user {}: {}", username, e.getMessage());
                }
            }

            log.info("Đã cập nhật trạng thái enabled trên Keycloak cho user: {}", enabled, username);
        } catch (HttpClientErrorException e) {
            log.error("Cập nhật trạng thái Keycloak user thất bại: ", e.getStatusCode(), e.getResponseBodyAsString());
            throw new IllegalStateException("Không thể cập nhật trạng thái tài khoản trên Authentication Server: " + e.getMessage());
        } catch (Exception e) {
            log.error("Lỗi khi cập nhật trạng thái Keycloak user: {}", username, e);
            throw new IllegalStateException("Không thể cập nhật trạng thái tài khoản trên Authentication Server.");
        }
    }
    
    public void resetTemporaryPassword(String username, String newPassword) {
      HttpHeaders headers = new HttpHeaders();
      headers.setBearerAuth(getAdminToken());
      headers.setContentType(MediaType.APPLICATION_JSON);

      String base = keycloakConfig.getAdminBaseUrl();
      var searchUri = UriComponentsBuilder.fromUriString(base + "/users")
          .queryParam("username", "{username}")
          .queryParam("exact", true)
          .buildAndExpand(username)
          .encode()
          .toUri();

      var users = restTemplate.exchange(
          searchUri, HttpMethod.GET, new HttpEntity<>(headers),
          new ParameterizedTypeReference<List<Map<String, Object>>>() {}
      ).getBody();

      if (users == null || users.size() != 1) {
          throw new IllegalStateException("Keycloak account not found");
      }

      String resetUrl = base + "/users/" + users.get(0).get("id")
          + "/reset-password";

      restTemplate.exchange(
          resetUrl, HttpMethod.PUT,
          new HttpEntity<>(Map.of(
              "type", "password",
              "value", newPassword,
              "temporary", true
          ), headers),
          Void.class
      );
  }

    /**
     * Đổi mật khẩu thành mật khẩu vĩnh viễn (temporary=false) để Keycloak
     * tự động xóa required_action UPDATE_PASSWORD khỏi tài khoản.
     */
    public void changePasswordAndClearAction(String username, String newPassword) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(getAdminToken());
        headers.setContentType(MediaType.APPLICATION_JSON);
        String base = keycloakConfig.getAdminBaseUrl();
        String search = UriComponentsBuilder.fromUriString(base + "/users")
            .queryParam("username", username).queryParam("exact", true).build().encode().toUriString();
        var users = restTemplate.exchange(search, HttpMethod.GET, new HttpEntity<>(headers),
            new ParameterizedTypeReference<List<Map<String, Object>>>() { }).getBody();
        if (users == null || users.size() != 1) throw new IllegalStateException("Keycloak account not found");
        String userUrl = base + "/users/" + users.get(0).get("id");
        restTemplate.exchange(userUrl + "/reset-password", HttpMethod.PUT,
            new HttpEntity<>(Map.of("type", "password", "value", newPassword, "temporary", false), headers),
            Void.class);
        log.info("Đã đổi mật khẩu vĩnh viễn cho user:", username);
    }

    /**
     * Gửi email xác minh địa chỉ email qua Keycloak execute-actions-email.
     */
    public void sendVerifyEmail(String username) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(getAdminToken());
        headers.setContentType(MediaType.APPLICATION_JSON);
        String base = keycloakConfig.getAdminBaseUrl();
        String search = UriComponentsBuilder.fromUriString(base + "/users")
            .queryParam("username", username).queryParam("exact", true).build().encode().toUriString();
        var users = restTemplate.exchange(search, HttpMethod.GET, new HttpEntity<>(headers),
            new ParameterizedTypeReference<List<Map<String, Object>>>() { }).getBody();
        if (users == null || users.isEmpty()) {
            log.warn("sendVerifyEmail: không tìm thấy user {}", username);
            return;
        }
        String userId = (String) users.get(0).get("id");
        String executeActionsUrl = base + "/users/" + userId + "/execute-actions-email";
        restTemplate.postForEntity(executeActionsUrl,
            new HttpEntity<>(List.of("VERIFY_EMAIL"), headers), String.class);
        log.info("Đã gửi email xác minh cho user: {}", username);
    }

    public void deleteKeycloakUser(String username) {
        String adminToken = getAdminToken();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        String searchUrl = keycloakConfig.getAdminBaseUrl()
                + "/users?username=" + username + "&exact=true";

        HttpEntity<Void> getEntity = new HttpEntity<>(headers);

        try {
            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                    searchUrl, HttpMethod.GET, getEntity,
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {
            });

            List<Map<String, Object>> users = response.getBody();
            if (users == null || users.isEmpty()) {
                log.warn("Keycloak: Không tìm thấy user với username={}", username);
                return;
            }

            String keycloakUserId = (String) users.get(0).get("id");

            String deleteUrl = keycloakConfig.getAdminBaseUrl() + "/users/" + keycloakUserId;
            HttpEntity<Void> deleteEntity = new HttpEntity<>(headers);
            restTemplate.exchange(deleteUrl, HttpMethod.DELETE, deleteEntity, Void.class);

            log.info("Đã xóa Keycloak user: {}", username);
        } catch (HttpClientErrorException e) {
            log.error("Xóa Keycloak user thất bại [{}]: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new IllegalStateException("Không thể xóa tài khoản trên Authentication Server: " + e.getMessage());
        } catch (Exception e) {
            log.error("Lỗi khi xóa Keycloak user: {}", username, e);
            throw new IllegalStateException("Không thể xóa tài khoản trên Authentication Server.");
        }
    }

    /**
     * Revoke refresh_token
     */
    public void revokeToken(String refreshToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_id", keycloakConfig.getClientId());
        if (keycloakConfig.getClientSecret() != null && !keycloakConfig.getClientSecret().isBlank()) {
            body.add("client_secret", keycloakConfig.getClientSecret());
        }
        body.add("refresh_token", refreshToken);

        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(body, headers);

        try {
            restTemplate.postForEntity(keycloakConfig.getLogoutEndpoint(), entity, String.class);
            log.info("Token đã bị revoke thành công.");
        } catch (HttpClientErrorException e) {
            log.warn("Revoke token thất bại: {}", e.getStatusCode());
        } catch (Exception e) {
            log.warn("Revoke token exception: {}", e.getMessage());
        }
    }
}
