package com.swiftlink.service;

import com.swiftlink.dto.UrlAnalyticsResponse;
import com.swiftlink.model.ClickEvent;
import com.swiftlink.repository.AnalyticsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private static final int RECENT_CLICKS_LIMIT = 50;
    private static final int ANALYTICS_QUERY_LIMIT = 1000;
    private static final DateTimeFormatter DAY_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneOffset.UTC);

    private final AnalyticsRepository analyticsRepository;

    @Async
    public void recordClick(String shortCode, String referrer, String userAgent, String ipAddress) {
        try {
            var sortKey = Instant.now().toEpochMilli() + "#" + UUID.randomUUID();
            var event = ClickEvent.builder()
                    .shortCode(shortCode)
                    .sortKey(sortKey)
                    .clickedAt(Instant.now())
                    .referrer(sanitize(referrer))
                    .userAgent(sanitize(userAgent))
                    .ipAddress(anonymizeIp(ipAddress))
                    .device(detectDevice(userAgent))
                    .browser(detectBrowser(userAgent))
                    .build();
            analyticsRepository.save(event);
        } catch (Exception ex) {
            log.warn("Failed to record click event for shortCode={}: {}", shortCode, ex.getMessage());
        }
    }

    public UrlAnalyticsResponse getAnalytics(String shortCode) {
        var events = analyticsRepository.findByShortCode(shortCode, ANALYTICS_QUERY_LIMIT);

        if (events.isEmpty()) {
            return new UrlAnalyticsResponse(shortCode, 0L, null, null,
                    Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), List.of());
        }

        var clicksByDay = events.stream()
                .filter(e -> e.getClickedAt() != null)
                .collect(Collectors.groupingBy(
                        e -> DAY_FORMAT.format(e.getClickedAt()),
                        Collectors.counting()));

        var topReferrers = topN(events.stream()
                .map(ClickEvent::getReferrer)
                .filter(Objects::nonNull)
                .toList());

        var topCountries = topN(events.stream()
                .map(ClickEvent::getCountry)
                .filter(Objects::nonNull)
                .toList());

        var topBrowsers = topN(events.stream()
                .map(ClickEvent::getBrowser)
                .filter(Objects::nonNull)
                .toList());

        var topDevices = topN(events.stream()
                .map(ClickEvent::getDevice)
                .filter(Objects::nonNull)
                .toList());

        var recentClicks = events.stream()
                .limit(RECENT_CLICKS_LIMIT)
                .map(e -> new UrlAnalyticsResponse.ClickEventDto(
                        e.getClickedAt(), e.getReferrer(), e.getCountry(), e.getDevice(), e.getBrowser()))
                .toList();

        var sorted = events.stream()
                .map(ClickEvent::getClickedAt)
                .filter(Objects::nonNull)
                .sorted()
                .toList();

        return new UrlAnalyticsResponse(
                shortCode,
                events.size(),
                sorted.isEmpty() ? null : sorted.getFirst(),
                sorted.isEmpty() ? null : sorted.getLast(),
                clicksByDay,
                topReferrers,
                topCountries,
                topBrowsers,
                topDevices,
                recentClicks);
    }

    private Map<String, Long> topN(List<String> values) {
        return values.stream()
                .collect(Collectors.groupingBy(v -> v, Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(10)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                        (a, b) -> a, LinkedHashMap::new));
    }

    private String detectDevice(String ua) {
        if (ua == null) return "Unknown";
        if (ua.contains("Mobile") || ua.contains("Android")) return "Mobile";
        if (ua.contains("Tablet") || ua.contains("iPad")) return "Tablet";
        return "Desktop";
    }

    private String detectBrowser(String ua) {
        if (ua == null) return "Unknown";
        if (ua.contains("Edg/")) return "Edge";
        if (ua.contains("Chrome")) return "Chrome";
        if (ua.contains("Firefox")) return "Firefox";
        if (ua.contains("Safari")) return "Safari";
        if (ua.contains("Opera") || ua.contains("OPR")) return "Opera";
        return "Other";
    }

    private String anonymizeIp(String ip) {
        if (ip == null) return null;
        int lastDot = ip.lastIndexOf('.');
        return lastDot >= 0 ? ip.substring(0, lastDot) + ".0" : ip;
    }

    private String sanitize(String value) {
        if (value == null) return null;
        return value.length() > 1024 ? value.substring(0, 1024) : value;
    }
}
