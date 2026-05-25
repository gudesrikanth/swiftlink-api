package com.swiftlink.functional;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * End-to-end lifecycle: create → info → redirect → analytics → delete.
 * Ordered tests share shortCode state via @TestInstance(PER_CLASS).
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class UrlLifecycleIT extends FunctionalTestBase {

    private String shortCode;
    private String customAlias;

    @Test
    @Order(1)
    void createUrl_autoCode_returns201WithShortCode() {
        var body = given().spec(spec)
                .body("""
                        {"longUrl":"https://example.com/lifecycle","title":"Lifecycle Test","tags":["it","test"]}
                        """)
                .when().post("/api/v1/urls")
                .then()
                .statusCode(201)
                .body("shortCode", notNullValue())
                .body("shortUrl", containsString("/"))
                .body("longUrl", equalTo("https://example.com/lifecycle"))
                .body("title", equalTo("Lifecycle Test"))
                .body("tags", hasItems("it", "test"))
                .extract().jsonPath();

        shortCode = body.getString("shortCode");
        assertThat(shortCode).isNotBlank().hasSize(7);
        assertThat(body.getString("shortUrl")).endsWith(shortCode);
    }

    @Test
    @Order(2)
    void createUrl_withCustomAlias_returns201WithAlias() {
        customAlias = "e2e-alias-" + System.currentTimeMillis();

        given().spec(spec)
                .body("""
                        {"longUrl":"https://example.com/alias","customAlias":"%s"}
                        """.formatted(customAlias))
                .when().post("/api/v1/urls")
                .then()
                .statusCode(201)
                .body("shortCode", equalTo(customAlias))
                .body("longUrl", equalTo("https://example.com/alias"));
    }

    @Test
    @Order(3)
    void getUrlInfo_returnsCorrectFields() {
        given().spec(spec)
                .when().get("/api/v1/urls/{code}", shortCode)
                .then()
                .statusCode(200)
                .body("shortCode", equalTo(shortCode))
                .body("longUrl", equalTo("https://example.com/lifecycle"))
                .body("active", equalTo(true))
                .body("clickCount", greaterThanOrEqualTo(0));
    }

    @Test
    @Order(4)
    void redirect_returns302WithLocationHeader() {
        given().spec(spec)
                .redirects().follow(false)
                .when().get("/{code}", shortCode)
                .then()
                .statusCode(302)
                .header("Location", equalTo("https://example.com/lifecycle"));
    }

    @Test
    @Order(5)
    void analytics_afterRedirects_returnsTotalClicks() throws InterruptedException {
        // Generate 3 clicks
        for (int i = 0; i < 3; i++) {
            given().spec(spec).redirects().follow(false).get("/{code}", shortCode);
        }
        Thread.sleep(500);

        given().spec(spec)
                .when().get("/api/v1/urls/{code}/analytics", shortCode)
                .then()
                .statusCode(200)
                .body("shortCode", equalTo(shortCode))
                .body("totalClicks", greaterThanOrEqualTo(3))
                .body("recentClicks", notNullValue())
                .body("clicksByDay", notNullValue());
    }

    @Test
    @Order(6)
    void deleteUrl_returns204() {
        given().spec(spec)
                .when().delete("/api/v1/urls/{code}", shortCode)
                .then()
                .statusCode(204);
    }

    @Test
    @Order(7)
    void getUrlInfo_afterDelete_returns404() {
        given().spec(spec)
                .when().get("/api/v1/urls/{code}", shortCode)
                .then()
                .statusCode(404)
                .body("error", equalTo("URL_NOT_FOUND"));
    }

    @Test
    @Order(8)
    void redirect_afterDelete_returns404() {
        given().spec(spec)
                .redirects().follow(false)
                .when().get("/{code}", shortCode)
                .then()
                .statusCode(404);
    }
}
