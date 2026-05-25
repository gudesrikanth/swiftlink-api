package com.swiftlink.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.time.Instant;
import java.util.List;

@Schema(description = "Request to create a new short URL")
public record CreateUrlRequest(

        @NotBlank(message = "Long URL is required")
        @Pattern(regexp = "^https?://.*", message = "URL must start with http:// or https://")
        @Size(max = 2048, message = "URL must not exceed 2048 characters")
        @Schema(description = "The full URL to shorten", example = "https://www.example.com/very/long/path")
        String longUrl,

        @Pattern(regexp = "^[a-zA-Z0-9_-]{4,32}$", message = "Custom alias must be 4-32 alphanumeric characters, dashes or underscores")
        @Schema(description = "Optional custom short code", example = "my-link")
        String customAlias,

        @Schema(description = "Optional title for the link", example = "My Example Link")
        @Size(max = 255)
        String title,

        @Future(message = "Expiry must be in the future")
        @Schema(description = "Optional expiry timestamp (ISO-8601)")
        Instant expiresAt,

        @Size(max = 10, message = "Maximum 10 tags allowed")
        @Schema(description = "Optional tags for organisation")
        List<@Size(max = 50) String> tags,

        @Schema(description = "Creator identifier (user ID or API key)")
        @Size(max = 255)
        String createdBy
) {}
