package com.example.shopping.ratelimiting.policy;

import java.time.Duration;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

/**
 * Represent rate limiting policy for a group of endpoints.
 * Each policy defines the maximum number of requests allowed within a specific time window.
 */
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RateLimitPolicy {

    int maxRequests;
    Duration window;
    String name;

    public RateLimitPolicy(String name, int maxRequests, Duration window) {
        this.name = name;
        this.maxRequests = maxRequests;
        this.window = window;
    }

    public int getMaxRequests() {
        return maxRequests;
    }

    /** Time window for rate limiting */
    public Duration getWindow() {
        return window;
    }

    /** Name of the policy for logging/debugging. */
    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return "RateLimitPolicy{name='" + name + "', maxRequests=" + maxRequests
                + ", window=" + window + '}';
    }
}
