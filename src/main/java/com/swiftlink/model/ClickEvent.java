package com.swiftlink.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DynamoDbBean
public class ClickEvent {

    private String shortCode;
    private String sortKey;      // timestamp#uuid for unique sort
    private Instant clickedAt;
    private String referrer;
    private String userAgent;
    private String ipAddress;
    private String country;
    private String city;
    private String device;
    private String browser;

    @DynamoDbPartitionKey
    @DynamoDbAttribute("shortCode")
    public String getShortCode() {
        return shortCode;
    }

    @DynamoDbSortKey
    @DynamoDbAttribute("sortKey")
    public String getSortKey() {
        return sortKey;
    }

    @DynamoDbAttribute("clickedAt")
    public Instant getClickedAt() {
        return clickedAt;
    }

    @DynamoDbAttribute("referrer")
    public String getReferrer() {
        return referrer;
    }

    @DynamoDbAttribute("userAgent")
    public String getUserAgent() {
        return userAgent;
    }

    @DynamoDbAttribute("ipAddress")
    public String getIpAddress() {
        return ipAddress;
    }

    @DynamoDbAttribute("country")
    public String getCountry() {
        return country;
    }

    @DynamoDbAttribute("city")
    public String getCity() {
        return city;
    }

    @DynamoDbAttribute("device")
    public String getDevice() {
        return device;
    }

    @DynamoDbAttribute("browser")
    public String getBrowser() {
        return browser;
    }
}
