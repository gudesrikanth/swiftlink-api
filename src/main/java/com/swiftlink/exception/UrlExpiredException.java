package com.swiftlink.exception;

import java.time.Instant;

public class UrlExpiredException extends RuntimeException {

    private final String shortCode;
    private final Instant expiredAt;

    public UrlExpiredException(String shortCode, Instant expiredAt) {
        super("Short URL has expired: " + shortCode + " (expired at " + expiredAt + ")");
        this.shortCode = shortCode;
        this.expiredAt = expiredAt;
    }

    public String getShortCode() {
        return shortCode;
    }

    public Instant getExpiredAt() {
        return expiredAt;
    }
}
