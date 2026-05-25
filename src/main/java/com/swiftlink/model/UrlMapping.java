package com.swiftlink.model;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

import java.time.Instant;
import java.util.List;

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

    public UrlMapping() {}

    private UrlMapping(Builder b) {
        this.shortCode  = b.shortCode;
        this.longUrl    = b.longUrl;
        this.title      = b.title;
        this.createdBy  = b.createdBy;
        this.createdAt  = b.createdAt;
        this.updatedAt  = b.updatedAt;
        this.expiresAt  = b.expiresAt;
        this.active     = b.active;
        this.clickCount = b.clickCount;
        this.tags       = b.tags;
    }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
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

        public Builder shortCode(String v)   { this.shortCode  = v; return this; }
        public Builder longUrl(String v)     { this.longUrl    = v; return this; }
        public Builder title(String v)       { this.title      = v; return this; }
        public Builder createdBy(String v)   { this.createdBy  = v; return this; }
        public Builder createdAt(Instant v)  { this.createdAt  = v; return this; }
        public Builder updatedAt(Instant v)  { this.updatedAt  = v; return this; }
        public Builder expiresAt(Instant v)  { this.expiresAt  = v; return this; }
        public Builder active(boolean v)     { this.active     = v; return this; }
        public Builder clickCount(long v)    { this.clickCount = v; return this; }
        public Builder tags(List<String> v)  { this.tags       = v; return this; }
        public UrlMapping build()            { return new UrlMapping(this); }
    }

    @DynamoDbPartitionKey
    @DynamoDbAttribute("shortCode")
    public String getShortCode()      { return shortCode; }
    public void   setShortCode(String v)  { this.shortCode = v; }

    @DynamoDbAttribute("longUrl")
    public String getLongUrl()        { return longUrl; }
    public void   setLongUrl(String v)    { this.longUrl = v; }

    @DynamoDbAttribute("title")
    public String getTitle()          { return title; }
    public void   setTitle(String v)      { this.title = v; }

    @DynamoDbAttribute("createdBy")
    public String getCreatedBy()      { return createdBy; }
    public void   setCreatedBy(String v)  { this.createdBy = v; }

    @DynamoDbAttribute("createdAt")
    public Instant getCreatedAt()     { return createdAt; }
    public void    setCreatedAt(Instant v) { this.createdAt = v; }

    @DynamoDbAttribute("updatedAt")
    public Instant getUpdatedAt()     { return updatedAt; }
    public void    setUpdatedAt(Instant v) { this.updatedAt = v; }

    @DynamoDbAttribute("expiresAt")
    public Instant getExpiresAt()     { return expiresAt; }
    public void    setExpiresAt(Instant v) { this.expiresAt = v; }

    @DynamoDbAttribute("active")
    public boolean isActive()         { return active; }
    public void    setActive(boolean v)   { this.active = v; }

    @DynamoDbAttribute("clickCount")
    public long getClickCount()       { return clickCount; }
    public void setClickCount(long v)     { this.clickCount = v; }

    @DynamoDbAttribute("tags")
    public List<String> getTags()     { return tags; }
    public void         setTags(List<String> v) { this.tags = v; }
}
