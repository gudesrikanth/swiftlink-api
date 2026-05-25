package com.swiftlink.functional;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

class UrlValidationIT extends FunctionalTestBase {

    @Test
    void createUrl_withInvalidUrl_returns400WithValidationError() {
        given().spec(spec)
                .body("""
                        {"longUrl":"not-a-url"}
                        """)
                .when().post("/api/v1/urls")
                .then()
                .statusCode(400)
                .body("error", equalTo("VALIDATION_FAILED"))
                .body("fieldErrors", notNullValue());
    }

    @Test
    void createUrl_withEmptyBody_returns400() {
        given().spec(spec)
                .body("{}")
                .when().post("/api/v1/urls")
                .then()
                .statusCode(400)
                .body("error", equalTo("VALIDATION_FAILED"));
    }

    @Test
    void createUrl_withInvalidAlias_returns400() {
        given().spec(spec)
                .body("""
                        {"longUrl":"https://example.com","customAlias":"x"}
                        """)
                .when().post("/api/v1/urls")
                .then()
                .statusCode(400)
                .body("error", equalTo("VALIDATION_FAILED"));
    }

    @Test
    void createUrl_withDuplicateAlias_returns409() {
        String alias = "dup-alias-" + System.currentTimeMillis();
        String body = """
                {"longUrl":"https://example.com","customAlias":"%s"}
                """.formatted(alias);

        given().spec(spec).body(body).when().post("/api/v1/urls").then().statusCode(201);

        given().spec(spec)
                .body(body)
                .when().post("/api/v1/urls")
                .then()
                .statusCode(409)
                .body("error", equalTo("SHORT_CODE_CONFLICT"));
    }

    @Test
    void getUrlInfo_withUnknownCode_returns404() {
        given().spec(spec)
                .when().get("/api/v1/urls/no-such-code-xyz")
                .then()
                .statusCode(404)
                .body("error", equalTo("URL_NOT_FOUND"));
    }

    @Test
    void redirect_withUnknownCode_returns404() {
        given().spec(spec)
                .redirects().follow(false)
                .when().get("/no-such-code-xyz")
                .then()
                .statusCode(404);
    }

    @Test
    void redirect_withExpiredUrl_returns410() throws InterruptedException {
        String alias = "exp-" + System.currentTimeMillis();
        String expiresAt = Instant.now().plus(2, ChronoUnit.SECONDS).toString();

        given().spec(spec)
                .body("""
                        {"longUrl":"https://example.com","customAlias":"%s","expiresAt":"%s"}
                        """.formatted(alias, expiresAt))
                .when().post("/api/v1/urls")
                .then().statusCode(201);

        Thread.sleep(3000);

        given().spec(spec)
                .redirects().follow(false)
                .when().get("/{code}", alias)
                .then()
                .statusCode(410)
                .body("error", equalTo("URL_EXPIRED"));
    }

    @Test
    void analytics_withUnknownCode_returns200WithZeroClicks() {
        // Analytics always returns 200 with empty data — no URL existence check by design
        given().spec(spec)
                .when().get("/api/v1/urls/no-such-code-xyz/analytics")
                .then()
                .statusCode(200)
                .body("totalClicks", equalTo(0));
    }
}
