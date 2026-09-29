package com.example.shopping.security;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;

import lombok.RequiredArgsConstructor;

/**
 * Cấu hình Spring Security cho API.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtDecoder jwtDecoder;
    private final JwtAuthenticationConverter jwtAuthenticationConverter;
    private final UserStatusFilter userStatusFilter;
    
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:4200"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session
                        -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/register", "/api/auth/login").denyAll()
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers("/camunda/**", "/engine-rest/**", "/actuator/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/users/me").hasAnyRole("ADMIN", "USER", "CHECKER")
                .requestMatchers(HttpMethod.PUT, "/api/users/me").hasAnyRole("ADMIN", "USER", "CHECKER")
                .requestMatchers("/api/users/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/supplier-proposals").hasAnyRole("ADMIN", "USER", "CHECKER")
                .requestMatchers(HttpMethod.POST, "/api/supplier-proposals").hasAnyRole("USER", "CHECKER")
                .requestMatchers("/api/supplier-proposals/**").hasAnyRole("ADMIN", "CHECKER")
                .requestMatchers(HttpMethod.POST, "/api/supplier-documents/*/review").hasAnyRole("ADMIN", "CHECKER")
                .requestMatchers("/api/supplier-documents/**").hasAnyRole("ADMIN", "USER", "CHECKER")
                .requestMatchers("/api/suppliers/export/**", "/api/suppliers/export-all", "/api/suppliers/template").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/suppliers", "/api/suppliers/*", "/api/suppliers/code/*").hasAnyRole("ADMIN", "USER")
                .requestMatchers("/api/suppliers/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/procurement/tasks", "/api/workflow/camunda/tasks").hasAnyRole("ADMIN", "CHECKER")
                .requestMatchers(HttpMethod.POST, "/api/procurement/tasks/*/review", "/api/workflow/camunda/tasks/*/review").hasAnyRole("ADMIN", "CHECKER")
                .requestMatchers(HttpMethod.POST, "/api/procurement/import").hasAnyRole("ADMIN", "USER")
                .requestMatchers(HttpMethod.POST, "/api/procurement/tickets", "/api/procurement/tickets/*/submit").hasAnyRole("ADMIN", "USER")
                .requestMatchers(HttpMethod.GET, "/api/procurement/tickets", "/api/procurement/tickets/page", "/api/procurement/tickets/*").hasAnyRole("ADMIN", "USER", "CHECKER")
                .requestMatchers(HttpMethod.GET, "/api/procurement/export/*", "/api/procurement/export-detail/*", "/api/procurement/export-all").hasAnyRole("ADMIN", "USER")
                .requestMatchers(HttpMethod.PUT, "/api/procurement/tickets/*").hasAnyRole("ADMIN", "USER")
                .requestMatchers(HttpMethod.DELETE, "/api/procurement/tickets/*").hasAnyRole("ADMIN", "USER")
                .requestMatchers("/api/procurement/**", "/api/workflow/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/company-documents", "/api/company-documents/*/download").hasAnyRole("ADMIN", "USER", "CHECKER")
                .requestMatchers("/api/company-documents/**").hasRole("ADMIN")
                .anyRequest().hasRole("ADMIN")
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                .decoder(jwtDecoder)
                .jwtAuthenticationConverter(jwtAuthenticationConverter)
                )
                )
                .addFilterAfter(userStatusFilter, BearerTokenAuthenticationFilter.class);

        return http.build();
    }
}
