package com.swiftlink.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

@Schema(description = "Created short URL details")
public record CreateUrlResponse(
        @Schema(description = "Unique short code") String shortCode,
        @Schema(description = "Full short URL ready to share") String shortUrl,
        @Schema(description = "Original long URL") String longUrl,
        @Schema(description = "Optional title") String title,
        @Schema(description = "When the link was created") Instant createdAt,
        @Schema(description = "When the link expires, null if no expiry") Instant expiresAt,
        @Schema(description = "Associated tags") List<String> tags
) {}
