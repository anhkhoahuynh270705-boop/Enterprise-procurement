package com.example.shopping.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.client.RestTemplate;

/**
 * Cấu hình chung của ứng dụng.
 * - PasswordEncoder: BCrypt (strength = 12) để hash mật khẩu lưu trong local DB.
 * - RestTemplate: HTTP client dùng cho Keycloak Admin REST API calls.
 */
@Configuration
public class AppConfig {
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    /**
     * RestTemplate dùng để gọi Keycloak token endpoint và Admin REST API.
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
