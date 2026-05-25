package com.swiftlink.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Auto-creates DynamoDB tables on startup when running locally.
 * In AWS, tables are managed by Terraform.
 */
@Component
@Profile("local")
public class DynamoDbTableInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DynamoDbTableInitializer.class);

    private final DynamoDbClient dynamoDbClient;
    private final AppProperties appProperties;

    public DynamoDbTableInitializer(DynamoDbClient dynamoDbClient, AppProperties appProperties) {
        this.dynamoDbClient = dynamoDbClient;
        this.appProperties  = appProperties;
    }

    @Override
    public void run(String... args) {
        createTableIfNotExists(appProperties.dynamoDb().urlTableName(), "shortCode", null);
        createTableIfNotExists(appProperties.dynamoDb().analyticsTableName(), "shortCode", "sortKey");
        log.info("DynamoDB tables initialised");
    }

    private void createTableIfNotExists(String tableName, String pk, String sk) {
        try {
            dynamoDbClient.describeTable(r -> r.tableName(tableName));
            log.debug("Table {} already exists", tableName);
        } catch (ResourceNotFoundException ex) {
            var attrs    = new ArrayList<AttributeDefinition>();
            var keySchema = new ArrayList<KeySchemaElement>();

            attrs.add(AttributeDefinition.builder().attributeName(pk).attributeType(ScalarAttributeType.S).build());
            keySchema.add(KeySchemaElement.builder().attributeName(pk).keyType(KeyType.HASH).build());

            if (sk != null) {
                attrs.add(AttributeDefinition.builder().attributeName(sk).attributeType(ScalarAttributeType.S).build());
                keySchema.add(KeySchemaElement.builder().attributeName(sk).keyType(KeyType.RANGE).build());
            }

            dynamoDbClient.createTable(CreateTableRequest.builder()
                    .tableName(tableName)
                    .attributeDefinitions(attrs)
                    .keySchema(keySchema)
                    .billingMode(BillingMode.PAY_PER_REQUEST)
                    .build());

            log.info("Created DynamoDB table: {}", tableName);
        }
    }
}
