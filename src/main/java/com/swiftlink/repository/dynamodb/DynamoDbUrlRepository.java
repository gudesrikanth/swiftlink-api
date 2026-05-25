package com.swiftlink.repository.dynamodb;

import com.swiftlink.config.AppProperties;
import com.swiftlink.exception.ShortCodeConflictException;
import com.swiftlink.model.UrlMapping;
import com.swiftlink.repository.UrlRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.PutItemEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;

import java.time.Instant;
import java.util.Optional;

import static software.amazon.awssdk.enhanced.dynamodb.internal.AttributeValues.stringValue;
import static software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional.keyEqualTo;
import static software.amazon.awssdk.services.dynamodb.model.AttributeValue.fromN;

@Slf4j
@Repository
@RequiredArgsConstructor
public class DynamoDbUrlRepository implements UrlRepository {

    private final DynamoDbEnhancedClient enhancedClient;
    private final AppProperties appProperties;

    private DynamoDbTable<UrlMapping> table() {
        return enhancedClient.table(
                appProperties.dynamoDb().urlTableName(),
                TableSchema.fromBean(UrlMapping.class));
    }

    @Override
    public UrlMapping save(UrlMapping urlMapping) {
        try {
            var request = PutItemEnhancedRequest.builder(UrlMapping.class)
                    .item(urlMapping)
                    .conditionExpression(
                            software.amazon.awssdk.enhanced.dynamodb.Expression.builder()
                                    .expression("attribute_not_exists(shortCode)")
                                    .build())
                    .build();
            table().putItem(request);
            log.debug("Saved URL mapping: {}", urlMapping.getShortCode());
            return urlMapping;
        } catch (ConditionalCheckFailedException ex) {
            throw new ShortCodeConflictException(urlMapping.getShortCode());
        }
    }

    @Override
    public Optional<UrlMapping> findByShortCode(String shortCode) {
        var key = Key.builder().partitionValue(shortCode).build();
        return Optional.ofNullable(table().getItem(key));
    }

    @Override
    public void deleteByShortCode(String shortCode) {
        var key = Key.builder().partitionValue(shortCode).build();
        table().deleteItem(key);
        log.debug("Deleted URL mapping: {}", shortCode);
    }

    @Override
    public boolean existsByShortCode(String shortCode) {
        return findByShortCode(shortCode).isPresent();
    }

    @Override
    public UrlMapping incrementClickCount(String shortCode) {
        // Use DynamoDB UpdateItem with atomic ADD for concurrency-safe increment
        var request = software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest.builder()
                .tableName(appProperties.dynamoDb().urlTableName())
                .key(java.util.Map.of("shortCode", stringValue(shortCode)))
                .updateExpression("ADD clickCount :inc SET updatedAt = :now")
                .expressionAttributeValues(java.util.Map.of(
                        ":inc", fromN("1"),
                        ":now", stringValue(Instant.now().toString())))
                .returnValues("ALL_NEW")
                .build();

        var response = enhancedClient.dynamoDbClient().updateItem(request);
        log.debug("Incremented click count for: {}", shortCode);

        return findByShortCode(shortCode).orElseThrow();
    }
}
