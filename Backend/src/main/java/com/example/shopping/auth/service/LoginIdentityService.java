package com.example.shopping.auth.service;

import java.util.List;
import org.springframework.security.oauth2.jwt.Jwt;

public interface LoginIdentityService {
    Jwt validateAccess(String token);
    void validateId(String token, String nonce, String subject);
    List<String> extractRequiredActions(String token);
}
