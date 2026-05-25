package com.swiftlink.exception;

public class UrlNotFoundException extends RuntimeException {

    private final String shortCode;

    public UrlNotFoundException(String shortCode) {
        super("Short URL not found: " + shortCode);
        this.shortCode = shortCode;
    }

    public String getShortCode() {
        return shortCode;
    }
}
