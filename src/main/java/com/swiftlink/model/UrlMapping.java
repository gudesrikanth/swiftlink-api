package com.swiftlink.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DynamoDbBean
public class UrlMapping {

    private String shortCode;
    private String longUrl;
    private String title;
    private String createdBy;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant expiresAt;
    private boolean active;
    private long clickCount;
    private List<String> tags;

    @DynamoDbPartitionKey
    @DynamoDbAttribute("shortCode")
    public String getShortCode() {
        return shortCode;
    }

    @DynamoDbAttribute("longUrl")
    public String getLongUrl() {
        return longUrl;
    }

    @DynamoDbAttribute("title")
    public String getTitle() {
        return title;
    }

    @DynamoDbAttribute("createdBy")
    public String getCreatedBy() {
        return createdBy;
    }

    @DynamoDbAttribute("createdAt")
    public Instant getCreatedAt() {
        return createdAt;
    }

    @DynamoDbAttribute("updatedAt")
    public Instant getUpdatedAt() {
        return updatedAt;
    }

    @DynamoDbAttribute("expiresAt")
    public Instant getExpiresAt() {
        return expiresAt;
    }

    @DynamoDbAttribute("active")
    public boolean isActive() {
        return active;
    }

    @DynamoDbAttribute("clickCount")
    public long getClickCount() {
        return clickCount;
    }

    @DynamoDbAttribute("tags")
    public List<String> getTags() {
        return tags;
    }
}
