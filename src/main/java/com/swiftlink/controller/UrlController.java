package com.swiftlink.controller;

import com.swiftlink.dto.CreateUrlRequest;
import com.swiftlink.dto.CreateUrlResponse;
import com.swiftlink.dto.ErrorResponse;
import com.swiftlink.dto.UrlInfoResponse;
import com.swiftlink.service.AnalyticsService;
import com.swiftlink.service.UrlShortenerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@Tag(name = "URLs", description = "URL shortening and management")
public class UrlController {

    private static final Logger log = LoggerFactory.getLogger(UrlController.class);

    private final UrlShortenerService urlShortenerService;
    private final AnalyticsService analyticsService;

    public UrlController(UrlShortenerService urlShortenerService, AnalyticsService analyticsService) {
        this.urlShortenerService = urlShortenerService;
        this.analyticsService    = analyticsService;
    }

    @PostMapping("/api/v1/urls")
    @Operation(summary = "Create a short URL",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Short URL created"),
                    @ApiResponse(responseCode = "400", description = "Validation error",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "409", description = "Custom alias already in use",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
            })
    public ResponseEntity<CreateUrlResponse> createShortUrl(@Valid @RequestBody CreateUrlRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(urlShortenerService.createShortUrl(request));
    }

    @GetMapping("/api/v1/urls/{shortCode}")
    @Operation(summary = "Get URL info by short code")
    public ResponseEntity<UrlInfoResponse> getUrlInfo(@PathVariable String shortCode) {
        return ResponseEntity.ok(urlShortenerService.getUrlInfo(shortCode));
    }

    @DeleteMapping("/api/v1/urls/{shortCode}")
    @Operation(summary = "Delete a short URL")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUrl(@PathVariable String shortCode) {
        urlShortenerService.deleteUrl(shortCode);
    }

    @GetMapping("/{shortCode}")
    @Operation(summary = "Redirect to original URL",
            responses = {
                    @ApiResponse(responseCode = "302", description = "Redirect to long URL"),
                    @ApiResponse(responseCode = "404", description = "Short URL not found"),
                    @ApiResponse(responseCode = "410", description = "Short URL has expired")
            })
    public ResponseEntity<Void> redirect(@PathVariable String shortCode, HttpServletRequest request) {
        var mapping = urlShortenerService.resolveUrl(shortCode);

        analyticsService.recordClick(
                shortCode,
                request.getHeader("Referer"),
                request.getHeader("User-Agent"),
                extractClientIp(request));

        urlShortenerService.recordClick(shortCode);

        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(mapping.getLongUrl()))
                .build();
    }

    private String extractClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
