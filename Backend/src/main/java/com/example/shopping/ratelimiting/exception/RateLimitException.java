package com.example.shopping.ratelimiting.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

/* Exception thrown when the client exceeds the request limit. */
@ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RateLimitException extends RuntimeException {

    long retryAfterSeconds;

    public RateLimitException(String message, long retryAfterSeconds) {
        super(message);
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public RateLimitException(String message) {
        this(message, 60L);
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
