package com.example.shopping.ratelimiting.service;

import com.example.shopping.ratelimiting.policy.RateLimitDecision;
import com.example.shopping.ratelimiting.policy.RateLimitPolicy;

/**
 * Contract check rate limiting based on key (IP, userId, v.v.) and policy 
 */
public interface RateLimitService {

    /**
     * Check whether the key exceeds the limit based on the policy.
     *
     * @param key Client identifier (IP address, userId)
     * @param policy Policy applied
     * @return {@link RateLimitDecision} containing the result of the decision
     */
    RateLimitDecision check(String key, RateLimitPolicy policy);

    /**
     * Reset the bucket for the given key.
     *
     * @param key The identifier to reset
     */
    void reset(String key);
}
