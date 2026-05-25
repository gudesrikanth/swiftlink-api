package com.swiftlink.service;

import com.swiftlink.config.AppProperties;
import com.swiftlink.dto.CreateUrlRequest;
import com.swiftlink.exception.UrlExpiredException;
import com.swiftlink.exception.UrlNotFoundException;
import com.swiftlink.model.UrlMapping;
import com.swiftlink.repository.UrlRepository;
import com.swiftlink.util.ShortCodeGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UrlShortenerServiceTest {

    @Mock
    private UrlRepository urlRepository;

    @Mock
    private ShortCodeGenerator shortCodeGenerator;

    @Mock
    private AppProperties appProperties;

    @InjectMocks
    private UrlShortenerService service;

    @BeforeEach
    void setUp() {
        var dynamoDb = new AppProperties.DynamoDb("", "urls", "analytics");
        var rateLimit = new AppProperties.RateLimit(60, 500);
        when(appProperties.baseUrl()).thenReturn("https://swiftlink.io");
        when(appProperties.shortCodeLength()).thenReturn(7);
        when(appProperties.defaultTtl()).thenReturn(Duration.ofDays(365));
        when(appProperties.dynamoDb()).thenReturn(dynamoDb);
    }

    @Test
    void createShortUrl_generatesShortCode_whenNoCustomAlias() {
        var request = new CreateUrlRequest("https://example.com", null, "Test", null, List.of(), "user1");
        when(shortCodeGenerator.generate(anyInt())).thenReturn("abc1234");
        when(urlRepository.existsByShortCode("abc1234")).thenReturn(false);
        when(urlRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = service.createShortUrl(request);

        assertThat(result.shortCode()).isEqualTo("abc1234");
        assertThat(result.shortUrl()).isEqualTo("https://swiftlink.io/abc1234");
        assertThat(result.longUrl()).isEqualTo("https://example.com");
        verify(urlRepository).save(any(UrlMapping.class));
    }

    @Test
    void createShortUrl_usesCustomAlias_whenProvided() {
        var request = new CreateUrlRequest("https://example.com", "my-link", null, null, null, null);
        when(urlRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = service.createShortUrl(request);

        assertThat(result.shortCode()).isEqualTo("my-link");
        verify(shortCodeGenerator, never()).generate(anyInt());
    }

    @Test
    void resolveUrl_throwsNotFound_whenCodeDoesNotExist() {
        when(urlRepository.findByShortCode("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.resolveUrl("unknown"))
                .isInstanceOf(UrlNotFoundException.class);
    }

    @Test
    void resolveUrl_throwsExpired_whenUrlIsExpired() {
        var expired = UrlMapping.builder()
                .shortCode("abc")
                .longUrl("https://example.com")
                .active(true)
                .expiresAt(Instant.now().minusSeconds(60))
                .build();
        when(urlRepository.findByShortCode("abc")).thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> service.resolveUrl("abc"))
                .isInstanceOf(UrlExpiredException.class);
    }

    @Test
    void deleteUrl_throwsNotFound_whenCodeDoesNotExist() {
        when(urlRepository.existsByShortCode("xyz")).thenReturn(false);

        assertThatThrownBy(() -> service.deleteUrl("xyz"))
                .isInstanceOf(UrlNotFoundException.class);
        verify(urlRepository, never()).deleteByShortCode(anyString());
    }

    @Test
    void resolveUrl_returnsMapping_whenActiveAndNotExpired() {
        var mapping = UrlMapping.builder()
                .shortCode("valid1")
                .longUrl("https://example.com")
                .active(true)
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
        when(urlRepository.findByShortCode("valid1")).thenReturn(Optional.of(mapping));

        var result = service.resolveUrl("valid1");

        assertThat(result.getLongUrl()).isEqualTo("https://example.com");
    }
}
