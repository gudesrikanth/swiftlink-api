package com.swiftlink.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "swiftlink")
public record AppProperties(
        @NotBlank String baseUrl,
        @NotNull @Min(4) int shortCodeLength,
        @NotNull Duration defaultTtl,
        @NotNull Duration maxTtl,
        DynamoDb dynamoDb,
        RateLimit rateLimit
) {

    public record DynamoDb(
            String endpoint,
            String urlTableName,
            String analyticsTableName
    ) {}

    public record RateLimit(
            int requestsPerMinute,
            int requestsPerHour
    ) {}
}
