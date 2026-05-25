package com.swiftlink.repository.dynamodb;

import com.swiftlink.config.AppProperties;
import com.swiftlink.exception.ShortCodeConflictException;
import com.swiftlink.model.UrlMapping;
import com.swiftlink.repository.UrlRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.PutItemEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
import software.amazon.awssdk.services.dynamodb.model.ReturnValue;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

@Repository
public class DynamoDbUrlRepository implements UrlRepository {

    private static final Logger log = LoggerFactory.getLogger(DynamoDbUrlRepository.class);

    private final DynamoDbEnhancedClient enhancedClient;
    private final DynamoDbClient dynamoDbClient;
    private final AppProperties appProperties;

    public DynamoDbUrlRepository(DynamoDbEnhancedClient enhancedClient,
                                  DynamoDbClient dynamoDbClient,
                                  AppProperties appProperties) {
        this.enhancedClient  = enhancedClient;
        this.dynamoDbClient  = dynamoDbClient;
        this.appProperties   = appProperties;
    }

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
                    .conditionExpression(Expression.builder()
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
        var request = UpdateItemRequest.builder()
                .tableName(appProperties.dynamoDb().urlTableName())
                .key(Map.of("shortCode", AttributeValue.fromS(shortCode)))
                .updateExpression("ADD clickCount :inc SET updatedAt = :now")
                .expressionAttributeValues(Map.of(
                        ":inc", AttributeValue.fromN("1"),
                        ":now", AttributeValue.fromS(Instant.now().toString())))
                .returnValues(ReturnValue.ALL_NEW)
                .build();

        dynamoDbClient.updateItem(request);
        log.debug("Incremented click count for: {}", shortCode);
        return findByShortCode(shortCode).orElseThrow();
    }
}
