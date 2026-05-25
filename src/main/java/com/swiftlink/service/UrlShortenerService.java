package com.swiftlink.service;

import com.swiftlink.config.AppProperties;
import com.swiftlink.config.CacheConfig;
import com.swiftlink.dto.CreateUrlRequest;
import com.swiftlink.dto.CreateUrlResponse;
import com.swiftlink.dto.UrlInfoResponse;
import com.swiftlink.exception.UrlExpiredException;
import com.swiftlink.exception.UrlNotFoundException;
import com.swiftlink.model.UrlMapping;
import com.swiftlink.repository.UrlRepository;
import com.swiftlink.util.ShortCodeGenerator;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class UrlShortenerService {

    private static final Logger log = LoggerFactory.getLogger(UrlShortenerService.class);
    private static final int MAX_GENERATION_ATTEMPTS = 5;
    private static final String CB_NAME = "dynamodb";

    private final UrlRepository urlRepository;
    private final ShortCodeGenerator shortCodeGenerator;
    private final AppProperties appProperties;

    public UrlShortenerService(UrlRepository urlRepository,
                                ShortCodeGenerator shortCodeGenerator,
                                AppProperties appProperties) {
        this.urlRepository      = urlRepository;
        this.shortCodeGenerator = shortCodeGenerator;
        this.appProperties      = appProperties;
    }

    @CircuitBreaker(name = CB_NAME)
    @Retry(name = CB_NAME)
    public CreateUrlResponse createShortUrl(CreateUrlRequest request) {
        String shortCode = resolveShortCode(request);
        var now = Instant.now();

        var mapping = UrlMapping.builder()
                .shortCode(shortCode)
                .longUrl(request.longUrl())
                .title(request.title())
                .createdBy(request.createdBy())
                .createdAt(now)
                .updatedAt(now)
                .expiresAt(resolveExpiry(request))
                .active(true)
                .clickCount(0L)
                .tags(request.tags())
                .build();

        urlRepository.save(mapping);
        log.info("Created short URL: {} -> {}", shortCode, request.longUrl());
        return toCreateResponse(mapping);
    }

    @Cacheable(value = CacheConfig.URL_CACHE, key = "#shortCode")
    @CircuitBreaker(name = CB_NAME)
    public UrlMapping resolveUrl(String shortCode) {
        var mapping = urlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new UrlNotFoundException(shortCode));

        if (!mapping.isActive()) {
            throw new UrlNotFoundException(shortCode);
        }
        if (mapping.getExpiresAt() != null && Instant.now().isAfter(mapping.getExpiresAt())) {
            throw new UrlExpiredException(shortCode, mapping.getExpiresAt());
        }
        return mapping;
    }

    @Cacheable(value = CacheConfig.URL_CACHE, key = "#shortCode + '-info'")
    public UrlInfoResponse getUrlInfo(String shortCode) {
        var mapping = urlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new UrlNotFoundException(shortCode));
        return toInfoResponse(mapping);
    }

    @Caching(evict = {
        @CacheEvict(value = CacheConfig.URL_CACHE, key = "#shortCode"),
        @CacheEvict(value = CacheConfig.URL_CACHE, key = "#shortCode + '-info'")
    })
    public void deleteUrl(String shortCode) {
        if (!urlRepository.existsByShortCode(shortCode)) {
            throw new UrlNotFoundException(shortCode);
        }
        urlRepository.deleteByShortCode(shortCode);
        log.info("Deleted short URL: {}", shortCode);
    }

    @Caching(evict = {
        @CacheEvict(value = CacheConfig.URL_CACHE, key = "#shortCode"),
        @CacheEvict(value = CacheConfig.URL_CACHE, key = "#shortCode + '-info'")
    })
    @CircuitBreaker(name = CB_NAME)
    public void recordClick(String shortCode) {
        urlRepository.incrementClickCount(shortCode);
    }

    private String resolveShortCode(CreateUrlRequest request) {
        if (request.customAlias() != null && !request.customAlias().isBlank()) {
            return request.customAlias();
        }
        for (int attempt = 0; attempt < MAX_GENERATION_ATTEMPTS; attempt++) {
            String code = shortCodeGenerator.generate(appProperties.shortCodeLength());
            if (!urlRepository.existsByShortCode(code)) {
                return code;
            }
        }
        return shortCodeGenerator.generate(appProperties.shortCodeLength() + 2);
    }

    private Instant resolveExpiry(CreateUrlRequest request) {
        if (request.expiresAt() != null) {
            return request.expiresAt();
        }
        return Instant.now().plus(appProperties.defaultTtl());
    }

    private CreateUrlResponse toCreateResponse(UrlMapping m) {
        return new CreateUrlResponse(
                m.getShortCode(),
                appProperties.baseUrl() + "/" + m.getShortCode(),
                m.getLongUrl(),
                m.getTitle(),
                m.getCreatedAt(),
                m.getExpiresAt(),
                m.getTags());
    }

    private UrlInfoResponse toInfoResponse(UrlMapping m) {
        return new UrlInfoResponse(
                m.getShortCode(),
                appProperties.baseUrl() + "/" + m.getShortCode(),
                m.getLongUrl(),
                m.getTitle(),
                m.getCreatedBy(),
                m.getCreatedAt(),
                m.getUpdatedAt(),
                m.getExpiresAt(),
                m.isActive(),
                m.getClickCount(),
                m.getTags());
    }
}
