package com.example.shopping.ratelimiting.filter;

import com.example.shopping.ratelimiting.policy.RateLimitDecision;
import com.example.shopping.ratelimiting.policy.RateLimitPolicy;
import com.example.shopping.ratelimiting.service.RateLimitService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;

/* Filter apply rate limiting for all API requests. */
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

    /** Default policy: maximum 100 requests per minute. */
    private static final RateLimitPolicy DEFAULT_POLICY =
            new RateLimitPolicy("default", 100, Duration.ofMinutes(1));

    RateLimitService rateLimitService;
    ObjectMapper objectMapper;

    public RateLimitFilter(RateLimitService rateLimitService, ObjectMapper objectMapper) {
        this.rateLimitService = rateLimitService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String key = resolveKey(request);
        RateLimitPolicy policy = resolvePolicy(request);
        RateLimitDecision decision = rateLimitService.check(key, policy);

        // Thêm header thông tin rate limit vào response
        response.setHeader("X-RateLimit-Limit", String.valueOf(policy.getMaxRequests()));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(decision.getRemainingTokens()));

        if (!decision.isAllowed()) {
            response.setHeader("Retry-After", String.valueOf(decision.getRetryAfterSeconds()));
            rejectRequest(response, decision);
            return;
        }
        filterChain.doFilter(request, response);
    }

    /**
     * Resolve key to lookup bucket.
     * Priority: userId (from header X-User-ID set by SecurityConfig) -> IP.
     */
    private String resolveKey(HttpServletRequest request) {
        String userId = request.getHeader("X-User-ID");
        if (userId != null && !userId.isBlank()) {
            return "user:" + userId;
        }
        return "ip:" + getClientIp(request);
    }

    /**
     * Select policy based on request path.
     */
    private RateLimitPolicy resolvePolicy(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (path.startsWith("/api/auth/callback")) {
            return DEFAULT_POLICY;
        }
        if (path.startsWith("/api/auth")) {
            return new RateLimitPolicy("auth", 30, Duration.ofMinutes(1));
        }

        return DEFAULT_POLICY;
    }

    /** Get real IP client, supporting reverse proxy. */
    private String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    /** Return HTTP 429 with JSON body. */
    private void rejectRequest(HttpServletResponse response, RateLimitDecision decision) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        Map<String, Object> body = Map.of(
                "status", HttpStatus.TOO_MANY_REQUESTS.value(),
                "error", "Too Many Requests",
                "message", "You have exceeded the request limit. Please try again later.",
                "retryAfterSeconds", decision.getRetryAfterSeconds()
        );

        objectMapper.writeValue(response.getWriter(), body);
        log.warn("Request rejected key=" + decision.getKey());
    }
}
