package com.example.shopping.integration.keycloak.config;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

import lombok.RequiredArgsConstructor;

/**
 * Cấu hình JWT cho Keycloak: - {@link JwtDecoder} : validate JWT bằng JWK Set
 * URI của Keycloak - {@link JwtAuthenticationConverter}: map realm_access.roles
 * → Spring GrantedAuthority
 */
@Configuration
@RequiredArgsConstructor
public class KeycloakJwtConfig {

    private final KeycloakConfig keycloakConfig;

    /**
     * Check valid JWT
     */
    @Bean
    public JwtDecoder jwtDecoder() {
        String jwkSetUri = keycloakConfig.getAuthServerUrl()
                + "/realms/" + keycloakConfig.getRealm()
                + "/protocol/openid-connect/certs";
        var decoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
        decoder.setJwtValidator(org.springframework.security.oauth2.jwt.JwtValidators.createDefaultWithIssuer(keycloakConfig.getIssuer()));
        return decoder;
    }

    /**
     * Converter: đọc claim {@code realm_access.roles} trong JWT →
     * {@code ROLE_xxx}.
     */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(new KeycloakRolesConverter());
        return converter;
    }

    /**
     * Đọc {@code realm_access.roles} từ JWT payload và ánh xạ thành Spring
     * Security {@link GrantedAuthority} với prefix {@code ROLE_}.
     */
    static class KeycloakRolesConverter implements Converter<Jwt, Collection<GrantedAuthority>> {
        @Override
        public Collection<GrantedAuthority> convert(Jwt jwt) {
            Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
            if (realmAccess == null) {
                return Collections.emptyList();
            }

            @SuppressWarnings("unchecked")
            List<String> roles = (List<String>) realmAccess.get("roles");
            if (roles == null) {
                return Collections.emptyList();
            }
            return roles.stream()
                    .map(role -> new SimpleGrantedAuthority(
                    role.startsWith("ROLE_") ? role : "ROLE_" + role))
                    .collect(Collectors.toList());
        }
    }
}
