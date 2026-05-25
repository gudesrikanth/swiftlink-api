package com.swiftlink.model;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

import java.time.Instant;

@DynamoDbBean
public class ClickEvent {

    private String shortCode;
    private String sortKey;
    private Instant clickedAt;
    private String referrer;
    private String userAgent;
    private String ipAddress;
    private String country;
    private String city;
    private String device;
    private String browser;

    public ClickEvent() {}

    private ClickEvent(Builder b) {
        this.shortCode  = b.shortCode;
        this.sortKey    = b.sortKey;
        this.clickedAt  = b.clickedAt;
        this.referrer   = b.referrer;
        this.userAgent  = b.userAgent;
        this.ipAddress  = b.ipAddress;
        this.country    = b.country;
        this.city       = b.city;
        this.device     = b.device;
        this.browser    = b.browser;
    }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String shortCode;
        private String sortKey;
        private Instant clickedAt;
        private String referrer;
        private String userAgent;
        private String ipAddress;
        private String country;
        private String city;
        private String device;
        private String browser;

        public Builder shortCode(String v)  { this.shortCode = v; return this; }
        public Builder sortKey(String v)    { this.sortKey   = v; return this; }
        public Builder clickedAt(Instant v) { this.clickedAt = v; return this; }
        public Builder referrer(String v)   { this.referrer  = v; return this; }
        public Builder userAgent(String v)  { this.userAgent = v; return this; }
        public Builder ipAddress(String v)  { this.ipAddress = v; return this; }
        public Builder country(String v)    { this.country   = v; return this; }
        public Builder city(String v)       { this.city      = v; return this; }
        public Builder device(String v)     { this.device    = v; return this; }
        public Builder browser(String v)    { this.browser   = v; return this; }
        public ClickEvent build()           { return new ClickEvent(this); }
    }

    @DynamoDbPartitionKey
    @DynamoDbAttribute("shortCode")
    public String getShortCode()       { return shortCode; }
    public void   setShortCode(String v)   { this.shortCode = v; }

    @DynamoDbSortKey
    @DynamoDbAttribute("sortKey")
    public String getSortKey()         { return sortKey; }
    public void   setSortKey(String v)     { this.sortKey = v; }

    @DynamoDbAttribute("clickedAt")
    public Instant getClickedAt()      { return clickedAt; }
    public void    setClickedAt(Instant v) { this.clickedAt = v; }

    @DynamoDbAttribute("referrer")
    public String getReferrer()        { return referrer; }
    public void   setReferrer(String v)    { this.referrer = v; }

    @DynamoDbAttribute("userAgent")
    public String getUserAgent()       { return userAgent; }
    public void   setUserAgent(String v)   { this.userAgent = v; }

    @DynamoDbAttribute("ipAddress")
    public String getIpAddress()       { return ipAddress; }
    public void   setIpAddress(String v)   { this.ipAddress = v; }

    @DynamoDbAttribute("country")
    public String getCountry()         { return country; }
    public void   setCountry(String v)     { this.country = v; }

    @DynamoDbAttribute("city")
    public String getCity()            { return city; }
    public void   setCity(String v)        { this.city = v; }

    @DynamoDbAttribute("device")
    public String getDevice()          { return device; }
    public void   setDevice(String v)      { this.device = v; }

    @DynamoDbAttribute("browser")
    public String getBrowser()         { return browser; }
    public void   setBrowser(String v)     { this.browser = v; }
}
