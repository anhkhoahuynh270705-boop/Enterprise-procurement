package com.example.shopping.workflow.camunda.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.web.cors.CorsConfigurationSource;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class CamundaConfig {

    private final CorsConfigurationSource corsConfigurationSource;

    @Bean
    @Order(1)
    public SecurityFilterChain camundaSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                // Camunda APIs use a separate servlet: match servletPath + pathInfo,
                // The standalone engine REST API stays in the JWT-protected API chain.
                .securityMatcher(new OrRequestMatcher(
                        new AntPathRequestMatcher("/camunda/**"),
                        new AntPathRequestMatcher("/assets/**"),
                        new AntPathRequestMatcher("/lib/**"),
                        new AntPathRequestMatcher("/favicon.ico")))
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .sessionManagement(session
                        -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .headers(headers
                        -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin))
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());

        return http.build();
    }
}
