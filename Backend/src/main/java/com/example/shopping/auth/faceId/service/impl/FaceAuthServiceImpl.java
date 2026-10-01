package com.example.shopping.auth.faceId.service.impl;

import com.example.shopping.auth.dto.response.LoginResponseDto;
import com.example.shopping.auth.faceId.client.FaceEnrollResponse;
import com.example.shopping.auth.faceId.client.FaceServiceRequest;
import com.example.shopping.auth.faceId.client.FaceVerifyResponse;
import com.example.shopping.auth.faceId.service.FaceAuthService;
import com.example.shopping.integration.keycloak.config.KeycloakConfig;
import com.example.shopping.user.entity.UserEntity;
import com.example.shopping.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Orchestrates multi-angle face enrollment and instant login.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FaceAuthServiceImpl implements FaceAuthService {

    private final UserRepository users;
    private final RestTemplate http;
    private final KeycloakConfig keycloakConfig;

    @Value("${face.service.url:http://localhost:8000}")
    private String faceServiceUrl;

    @Override
    public void enroll(String username, String imageB64) {
        enroll(username, List.of(imageB64));
    }

    @Override
    public void enroll(String username, List<String> imagesB64) {
        UserEntity user = resolveUser(username);

        FaceServiceRequest req = new FaceServiceRequest();
        req.setUser_id(user.getId().toString());
        req.setImages(imagesB64);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<FaceServiceRequest> entity = new HttpEntity<>(req, headers);

        try {
            FaceEnrollResponse resp = http.postForObject(
                faceServiceUrl + "/face/enroll",
                entity,
                FaceEnrollResponse.class
            );
            if (resp == null || !resp.isSuccess()) {
                String msg = resp != null ? resp.getMessage() : "Không nhận được phản hồi từ dịch vụ Face ID";
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, msg);
            }
            log.info("Face enrolled for user with poses ", username, resp.getEnrolledPoses(), user.getId());
        } catch (HttpClientErrorException ex) {
            log.warn("Face Service enroll error:", ex.getStatusCode(), ex.getResponseBodyAsString());
            String msg = extractMessage(ex.getResponseBodyAsString());
            if (msg == null || msg.isBlank()) {
                msg = "Đăng ký khuôn mặt không thành công (" + ex.getStatusCode().value() + ")";
            }
            throw new ResponseStatusException(
                HttpStatus.valueOf(ex.getStatusCode().value()),
                msg
            );
        }
    }

    @Override
    public LoginResponseDto verifyAndLogin(String username, String imageB64) {
        String effectiveUsername = username;

        if (effectiveUsername != null && !effectiveUsername.isBlank()) {
            // 1:1 Matching
            UserEntity user = resolveUser(effectiveUsername);
            FaceVerifyResponse verifyResp = callVerify(user.getId().toString(), imageB64);
            if (!verifyResp.isVerified()) {
                log.info("Face verify FAILED for: ", effectiveUsername, verifyResp.getMessage());
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Khuôn mặt không khớp. Vui lòng thử lại.");
            }
            log.info("Face verify OK for: ", effectiveUsername, verifyResp.getSimilarity());
        } else {
            // 1:N Identification 
            FaceVerifyResponse verifyResp = callVerify(null, imageB64);
            if (!verifyResp.isVerified() || verifyResp.getUserId() == null) {
                log.info("Face identification FAILED: {}", verifyResp.getMessage());
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Không nhận diện được khuôn mặt người dùng.");
            }

            try {
                UUID userUuid = UUID.fromString(verifyResp.getUserId());
                UserEntity identifiedUser = users.findById(userUuid)
                    .filter(u -> !u.isDeleted() && Boolean.TRUE.equals(u.getEnabled()))
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tài khoản không tồn tại hoặc đã bị khóa."));
                effectiveUsername = identifiedUser.getUsername();
                log.info("Face identified as user:", effectiveUsername, verifyResp.getSimilarity());
            } catch (IllegalArgumentException ex) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "ID người dùng không hợp lệ.");
            }
        }
        // Issue tokens for the authenticated user
        return issueTokenForUser(effectiveUsername);
    }

    @Override
    public boolean isEnrolled(String username) {
        Optional<UserEntity> opt = users.findByUsername(username);
        if (opt.isEmpty()) return false;
        try {
            String checkUrl = faceServiceUrl + "/face/enrolled/" + opt.get().getId().toString();
            var response = http.exchange(
                checkUrl,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            Map<String, Object> body = response.getBody();
            return body != null && Boolean.TRUE.equals(body.get("enrolled"));
        } catch (Exception ex) {
            log.warn("Could not check face enrollment for user {}: {}", username, ex.getMessage());
            return false;
        }
    }

    private UserEntity resolveUser(String username) {
        return users.findByUsername(username)
            .filter(u -> !u.isDeleted() && Boolean.TRUE.equals(u.getEnabled()))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Người dùng không tồn tại hoặc đã bị khóa"));
    }

    private FaceVerifyResponse callVerify(String userId, String imageB64) {
        FaceServiceRequest req = new FaceServiceRequest();
        req.setUser_id(userId);
        req.setImage(imageB64);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<FaceServiceRequest> entity = new HttpEntity<>(req, headers);

        try {
            FaceVerifyResponse resp = http.postForObject(
                faceServiceUrl + "/face/verify",
                entity,
                FaceVerifyResponse.class
            );
            return resp != null ? resp : unverifiedResponse("Không có phản hồi từ dịch vụ Face ID");
        } catch (HttpClientErrorException.NotFound ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Người dùng chưa đăng ký khuôn mặt");
        } catch (HttpClientErrorException ex) {
            log.warn("Face Service verify error: {} {}", ex.getStatusCode(), ex.getResponseBodyAsString());
            return unverifiedResponse(extractMessage(ex.getResponseBodyAsString()));
        }
    }

    /**
     * Issues Keycloak token for the user via token-exchange.
     */
    private LoginResponseDto issueTokenForUser(String username) {
        String serviceToken = fetchServiceAccountToken();

        var form = new LinkedMultiValueMap<String, String>();
        form.add("grant_type", "urn:ietf:params:oauth:grant-type:token-exchange");
        form.add("client_id", keycloakConfig.getClientId());
        String secret = keycloakConfig.getClientSecret() != null ? keycloakConfig.getClientSecret().trim() : "";
        if (!secret.isBlank()) {
            form.add("client_secret", secret);
        }
        form.add("subject_token", serviceToken);
        form.add("subject_token_type", "urn:ietf:params:oauth:token-type:access_token");
        form.add("requested_subject", username);
        form.add("scope", "openid profile email offline_access");

        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        try {
            LoginResponseDto tokens = http.postForObject(
                keycloakConfig.getTokenEndpoint(),
                new HttpEntity<>(form, headers),
                LoginResponseDto.class
            );
            if (tokens == null || tokens.getAccessToken() == null) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Không tạo được token phiên làm việc");
            }
            log.info("Token exchange succeeded for user:" + username);
            return tokens;
        } catch (HttpClientErrorException ex) {
            log.error("Token exchange failed for user: " + username + ": status=" + ex.getStatusCode() + ", body=" + ex.getResponseBodyAsString());
            throw new ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Không thể cấp token đăng nhập: " + extractMessage(ex.getResponseBodyAsString())
            );
        }
    }

    private String fetchServiceAccountToken() {
        var form = new LinkedMultiValueMap<String, String>();
        form.add("grant_type", "client_credentials");
        form.add("client_id", keycloakConfig.getClientId());
        String secret = keycloakConfig.getClientSecret() != null ? keycloakConfig.getClientSecret().trim() : "";
        if (!secret.isBlank()) {
            form.add("client_secret", secret);
        }

        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        try {
            LoginResponseDto resp = http.postForObject(
                keycloakConfig.getTokenEndpoint(),
                new HttpEntity<>(form, headers),
                LoginResponseDto.class
            );
            if (resp == null || resp.getAccessToken() == null) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Service account token trống");
            }
            return resp.getAccessToken();
        } catch (HttpClientErrorException ex) {
            log.error("Service account token fetch failed:", ex.getResponseBodyAsString());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Dịch vụ xác thực không khả dụng");
        }
    }

    /* Face ID */
    private static FaceVerifyResponse unverifiedResponse(String message) {
        FaceVerifyResponse r = new FaceVerifyResponse();
        r.setVerified(false);
        r.setSimilarity(0.0);
        r.setMessage(message);
        return r;
    }

    private static String extractMessage(String body) {
        if (body == null || body.isBlank()) {
            return "Không nhận được phản hồi từ dịch vụ Face ID";
        }
        try {
            ObjectMapper om = new ObjectMapper();
            var tree = om.readTree(body);
            if (tree.has("error_description")) {
                String val = tree.get("error_description").asText();
                if (val != null && !val.isBlank()) 
                    return val;
            }
            if (tree.has("detail")) {
                var detailNode = tree.get("detail");
                if (detailNode.isTextual() && !detailNode.asText().isBlank()) {
                    return detailNode.asText();
                } else if (detailNode.isArray()) {
                    StringBuilder sb = new StringBuilder();
                    for (var item : detailNode) {
                        if (item.has("msg") && !item.get("msg").asText().isBlank()) {
                            if (sb.length() > 0) sb.append("; ");
                            sb.append(item.get("msg").asText());
                        }
                    }
                    if (sb.length() > 0) return sb.toString();
                }
                String dStr = detailNode.toString();
                if (!dStr.isBlank()) return dStr;
            }
            if (tree.has("message")) {
                String val = tree.get("message").asText();
                if (val != null && !val.isBlank()) return val;
            }
            return body;
        } catch (Exception ignored) {
            return body.isBlank() ? "Face service error" : body;
        }
    }
}
