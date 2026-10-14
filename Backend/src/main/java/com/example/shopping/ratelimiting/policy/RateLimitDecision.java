package com.example.shopping.ratelimiting.policy;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

/**
 * Result of rate limiting after checking.
 * Indicates whether the request is accepted or rejected, and when it can be tried again.
 */
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RateLimitDecision {
    boolean allowed;
    long remainingTokens;
    long retryAfterSeconds;
    String key;

    private RateLimitDecision(boolean allowed, long remainingTokens, long retryAfterSeconds, String key) {
        this.allowed = allowed;
        this.remainingTokens = remainingTokens;
        this.retryAfterSeconds = retryAfterSeconds;
        this.key = key; 
    }

    /** Create decision for allowed request. */
    public static RateLimitDecision allowed(long remainingTokens, String key) {
        return new RateLimitDecision(true, remainingTokens, 0L, key);
    }

    /** Create decision for denied request. */
    public static RateLimitDecision denied(long retryAfterSeconds, String key) {
        return new RateLimitDecision(false, 0L, retryAfterSeconds, key);
    }

    /** Trả về true nếu request được phép đi qua. */
    public boolean isAllowed() {
        return allowed;
    }

    /** Số token còn lại trong bucket sau request này. */
    public long getRemainingTokens() {
        return remainingTokens;
    }

    /** Số giây phải chờ trước khi thử lại. */
    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }

    /** Key bucket đã được sử dụng để kiểm tra. */
    public String getKey() {
        return key;
    }

    @Override
    public String toString() {
        return "RateLimitDecision{allowed=" + allowed + ", remainingTokens=" + remainingTokens
                + ", retryAfterSeconds=" + retryAfterSeconds + ", key='" + key + "'}";
    }
}
