package com.example.shopping.security;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import com.example.shopping.common.enums.Role;

import com.example.shopping.user.entity.UserEntity;
import com.example.shopping.user.repository.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserStatusFilter extends OncePerRequestFilter {

    /* Block request when user being disabled   
    */
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth instanceof JwtAuthenticationToken jwtAuth) {
            Jwt jwt = jwtAuth.getToken();
            String username = jwt.getClaimAsString("preferred_username");
            if (username == null || username.isBlank()) {
                username = jwt.getSubject();
            }
            String email = jwt.getClaimAsString("email");

            Optional<UserEntity> userOpt = Optional.empty();
            if (username != null && !username.isBlank()) {
                userOpt = userRepository.findByUsername(username);
            }
            if (userOpt.isEmpty() && email != null && !email.isBlank()) {
                userOpt = userRepository.findByEmail(email);
            }

            if (userOpt.isEmpty() || userOpt.get().isDeleted() || !Boolean.TRUE.equals(userOpt.get().getEnabled())) {
                log.warn("Từ chối truy cập cho tài khoản bị vô hiệu hóa:", username);
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType("application/json;charset=UTF-8");
                String json = String.format(
                        "{\"timestamp\":\"%s\",\"status\":403,\"error\":\"Forbidden\",\"message\":\"Tài khoản của bạn đã bị vô hiệu hóa.\"} ",
                        LocalDateTime.now()
                );
                response.getWriter().write(json);
                return;
            }
            if (userOpt.get().isEmailVerificationRequired() && !Boolean.TRUE.equals(jwt.getClaimAsBoolean("email_verified"))) {
                response.setStatus(403);
                response.setContentType("application/json" );
                response.getWriter().write("{\"message\":\"Email verification required\"}");
                return;
            }
            // Use the current stored application role, so a demoted account cannot
            // retain privileges through an already-issued Keycloak access token.
            if (userOpt.isPresent()) {
                try {
                    String role = Role.parse(userOpt.get().getRole()).name();
                    SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt,
                        List.of(new SimpleGrantedAuthority("ROLE_" + role)),
                        username));
                } catch (IllegalArgumentException ex) {
                    response.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid application role");
                    return;
                }
            }
        }

        filterChain.doFilter(request, response);
    }
}
