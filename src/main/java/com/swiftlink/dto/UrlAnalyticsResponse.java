package com.swiftlink.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Schema(description = "Analytics summary for a short URL")
public record UrlAnalyticsResponse(
        String shortCode,
        long totalClicks,
        Instant firstClickAt,
        Instant lastClickAt,
        Map<String, Long> clicksByDay,
        Map<String, Long> topReferrers,
        Map<String, Long> topCountries,
        Map<String, Long> topBrowsers,
        Map<String, Long> topDevices,
        List<ClickEventDto> recentClicks
) {

    public record ClickEventDto(
            Instant clickedAt,
            String referrer,
            String country,
            String device,
            String browser
    ) {}
}
