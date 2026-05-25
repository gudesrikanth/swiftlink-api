package com.swiftlink.functional;

import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("local")
@Testcontainers
public abstract class FunctionalTestBase {

    @Container
    static final LocalStackContainer localstack = new LocalStackContainer(
            DockerImageName.parse("localstack/localstack:3.4"))
            .withServices(LocalStackContainer.Service.DYNAMODB);

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("swiftlink.dynamo-db.endpoint",
                () -> localstack.getEndpointOverride(LocalStackContainer.Service.DYNAMODB).toString());
        // AWS SDK DefaultCredentialsProvider reads these JVM system properties
        System.setProperty("aws.accessKeyId", localstack.getAccessKey());
        System.setProperty("aws.secretAccessKey", localstack.getSecretKey());
        System.setProperty("aws.region", localstack.getRegion());
    }

    @LocalServerPort
    protected int port;

    protected RequestSpecification spec;

    @BeforeEach
    void setUpRestAssured() {
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
        spec = new RequestSpecBuilder()
                .setBaseUri("http://localhost")
                .setPort(port)
                .setContentType(ContentType.JSON)
                .build();
    }
}
