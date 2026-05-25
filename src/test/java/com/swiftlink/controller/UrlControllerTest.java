package com.swiftlink.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.swiftlink.dto.CreateUrlRequest;
import com.swiftlink.dto.CreateUrlResponse;
import com.swiftlink.exception.UrlNotFoundException;
import com.swiftlink.service.AnalyticsService;
import com.swiftlink.service.UrlShortenerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UrlController.class)
class UrlControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UrlShortenerService urlShortenerService;

    @MockitoBean
    private AnalyticsService analyticsService;

    @Test
    void createShortUrl_returns201_withValidRequest() throws Exception {
        var request = new CreateUrlRequest("https://example.com", null, "Test", null, List.of("tag1"), "user1");
        var response = new CreateUrlResponse("abc1234", "https://swiftlink.io/abc1234",
                "https://example.com", "Test", Instant.now(), null, List.of("tag1"));

        when(urlShortenerService.createShortUrl(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.shortCode").value("abc1234"))
                .andExpect(jsonPath("$.shortUrl").value("https://swiftlink.io/abc1234"));
    }

    @Test
    void createShortUrl_returns400_withInvalidUrl() throws Exception {
        var request = new CreateUrlRequest("not-a-url", null, null, null, null, null);

        mockMvc.perform(post("/api/v1/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void getUrlInfo_returns404_whenNotFound() throws Exception {
        when(urlShortenerService.getUrlInfo("notfound"))
                .thenThrow(new UrlNotFoundException("notfound"));

        mockMvc.perform(get("/api/v1/urls/notfound"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("URL_NOT_FOUND"));
    }

    @Test
    void deleteUrl_returns204_onSuccess() throws Exception {
        mockMvc.perform(delete("/api/v1/urls/abc1234"))
                .andExpect(status().isNoContent());
    }
}
