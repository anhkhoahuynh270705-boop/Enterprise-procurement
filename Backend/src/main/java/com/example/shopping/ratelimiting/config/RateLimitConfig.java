package com.example.shopping.ratelimiting.config;

import com.example.shopping.ratelimiting.filter.RateLimitFilter;
import com.example.shopping.ratelimiting.service.RateLimitService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RateLimitConfig {
    @Bean
    @ConditionalOnProperty(name = "rate-limit.enabled", havingValue = "true", matchIfMissing = true)
    public RateLimitFilter rateLimitFilter(RateLimitService rateLimitService, ObjectMapper objectMapper) {
        return new RateLimitFilter(rateLimitService, objectMapper);
    }
}
