package com.example.shopping.ratelimiting.service.impl;

import com.example.shopping.ratelimiting.policy.RateLimitDecision;
import com.example.shopping.ratelimiting.policy.RateLimitPolicy;
import com.example.shopping.ratelimiting.service.RateLimitService;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Implementation of {@link RateLimitService} using Bucket4j.
 * Stores bucket state in-memory using ConcurrentHashMap.
 */
@Service
public class Bucket4jRateLimitService implements RateLimitService {

    private static final Logger log = LoggerFactory.getLogger(Bucket4jRateLimitService.class);

    /** Cache bucket theo key (IP hoặc userId). */
    private final ConcurrentMap<String, Bucket> bucketCache = new ConcurrentHashMap<>();

    @Override
    public RateLimitDecision check(String key, RateLimitPolicy policy) {
        Bucket bucket = bucketCache.computeIfAbsent(key, k -> buildBucket(policy));

        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (probe.isConsumed()) {
            log.debug("Rate limit OK", key, probe.getRemainingTokens());
            return RateLimitDecision.allowed(probe.getRemainingTokens(), key);
        }

        long retryAfterSeconds = probe.getNanosToWaitForRefill() / 1_000_000_000L;
        log.warn("Rate limit EXCEEDED", key, retryAfterSeconds);
        return RateLimitDecision.denied(retryAfterSeconds, key);
    }

    @Override
    public void reset(String key) {
        bucketCache.remove(key);
        log.info("Rate limit bucket reset for key", key);
    }

    private Bucket buildBucket(RateLimitPolicy policy) {
        Bandwidth limit = Bandwidth.builder()
                .capacity(policy.getMaxRequests())
                .refillGreedy(policy.getMaxRequests(), policy.getWindow())
                .build();

        return Bucket.builder()
                .addLimit(limit)
                .build();
    }
}
