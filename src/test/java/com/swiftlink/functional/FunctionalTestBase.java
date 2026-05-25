package com.swiftlink.functional;

import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.net.Socket;

/**
 * Base class for functional IT tests.
 *
 * Requires DynamoDB Local running on port 8000 (started automatically in CI
 * as a service container, or manually via: docker run -d -p 8000:8000
 * amazon/dynamodb-local:2.5.2 -jar DynamoDBLocal.jar -sharedDb -inMemory).
 *
 * Tests are skipped automatically when DynamoDB Local is not available.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("local")
public abstract class FunctionalTestBase {

    private static final int DYNAMO_PORT = 8000;

    @DynamicPropertySource
    static void configureAwsCredentials(DynamicPropertyRegistry registry) {
        // Provide fake credentials so DefaultCredentialsProvider doesn't fail
        // (DynamoDB Local accepts any credentials)
        System.setProperty("aws.accessKeyId", "test");
        System.setProperty("aws.secretAccessKey", "test");
        System.setProperty("aws.region", "us-east-1");
    }

    @LocalServerPort
    protected int port;

    protected RequestSpecification spec;

    @BeforeEach
    void setUpRestAssured() {
        Assumptions.assumeTrue(isDynamoDbLocalAvailable(),
                "DynamoDB Local not running on port " + DYNAMO_PORT + " — skipping functional tests");
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
        spec = new RequestSpecBuilder()
                .setBaseUri("http://localhost")
                .setPort(port)
                .setContentType(ContentType.JSON)
                .build();
    }

    private static boolean isDynamoDbLocalAvailable() {
        try (var ignored = new Socket("localhost", DYNAMO_PORT)) {
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
