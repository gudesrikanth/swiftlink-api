package com.swiftlink;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@TestPropertySource(properties = {
        "aws.region=us-east-1",
        "swiftlink.base-url=http://localhost:8080",
        "swiftlink.short-code-length=7",
        "swiftlink.default-ttl=P365D",
        "swiftlink.max-ttl=P3650D",
        "swiftlink.dynamo-db.endpoint=",
        "swiftlink.dynamo-db.url-table-name=swiftlink-urls",
        "swiftlink.dynamo-db.analytics-table-name=swiftlink-analytics",
        "swiftlink.rate-limit.requests-per-minute=60",
        "swiftlink.rate-limit.requests-per-hour=500"
})
class SwiftLinkApplicationTests {

    @MockitoBean
    DynamoDbClient dynamoDbClient;

    @MockitoBean
    DynamoDbEnhancedClient dynamoDbEnhancedClient;

    @Test
    void contextLoads() {
    }
}
