package com.swiftlink.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

@Schema(description = "Full URL mapping info")
public record UrlInfoResponse(
        String shortCode,
        String shortUrl,
        String longUrl,
        String title,
        String createdBy,
        Instant createdAt,
        Instant updatedAt,
        Instant expiresAt,
        boolean active,
        long clickCount,
        List<String> tags
) {}
