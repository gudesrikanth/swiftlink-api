package com.swiftlink.repository.dynamodb;

import com.swiftlink.config.AppProperties;
import com.swiftlink.model.ClickEvent;
import com.swiftlink.repository.AnalyticsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;

import java.util.List;

@Repository
public class DynamoDbAnalyticsRepository implements AnalyticsRepository {

    private static final Logger log = LoggerFactory.getLogger(DynamoDbAnalyticsRepository.class);

    private final DynamoDbEnhancedClient enhancedClient;
    private final AppProperties appProperties;

    public DynamoDbAnalyticsRepository(DynamoDbEnhancedClient enhancedClient,
                                        AppProperties appProperties) {
        this.enhancedClient = enhancedClient;
        this.appProperties  = appProperties;
    }

    private DynamoDbTable<ClickEvent> table() {
        return enhancedClient.table(
                appProperties.dynamoDb().analyticsTableName(),
                TableSchema.fromBean(ClickEvent.class));
    }

    @Override
    public void save(ClickEvent event) {
        table().putItem(event);
        log.debug("Saved click event for shortCode={}", event.getShortCode());
    }

    @Override
    public List<ClickEvent> findByShortCode(String shortCode, int limit) {
        var key = Key.builder().partitionValue(shortCode).build();
        var request = QueryEnhancedRequest.builder()
                .queryConditional(QueryConditional.keyEqualTo(key))
                .limit(limit)
                .scanIndexForward(false)
                .build();
        return table().query(request)
                .items()
                .stream()
                .limit(limit)
                .toList();
    }

    @Override
    public long countByShortCode(String shortCode) {
        var key = Key.builder().partitionValue(shortCode).build();
        return table().query(QueryConditional.keyEqualTo(key))
                .items()
                .stream()
                .count();
    }
}
