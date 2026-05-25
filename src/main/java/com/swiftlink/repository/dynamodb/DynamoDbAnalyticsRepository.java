package com.swiftlink.repository.dynamodb;

import com.swiftlink.config.AppProperties;
import com.swiftlink.model.ClickEvent;
import com.swiftlink.repository.AnalyticsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;

import java.util.List;

@Slf4j
@Repository
@RequiredArgsConstructor
public class DynamoDbAnalyticsRepository implements AnalyticsRepository {

    private final DynamoDbEnhancedClient enhancedClient;
    private final AppProperties appProperties;

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
                .scanIndexForward(false)   // newest first
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
