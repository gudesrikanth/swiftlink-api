package com.swiftlink.functional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

class AnalyticsIT extends FunctionalTestBase {

    private String shortCode;

    @BeforeEach
    void createUrl() {
        shortCode = given().spec(spec)
                .body("""
                        {"longUrl":"https://example.com/analytics-test","title":"Analytics IT"}
                        """)
                .when().post("/api/v1/urls")
                .then()
                .statusCode(201)
                .extract().jsonPath().getString("shortCode");
    }

    @Test
    void analytics_beforeAnyClicks_returnsZeroTotalClicks() {
        given().spec(spec)
                .when().get("/api/v1/urls/{code}/analytics", shortCode)
                .then()
                .statusCode(200)
                .body("shortCode", equalTo(shortCode))
                .body("totalClicks", equalTo(0))
                .body("recentClicks", notNullValue())
                .body("clicksByDay", notNullValue())
                .body("topReferrers", notNullValue())
                .body("topCountries", notNullValue())
                .body("topBrowsers", notNullValue())
                .body("topDevices", notNullValue());
    }

    @Test
    void analytics_afterClicks_tracksTotalCount() throws InterruptedException {
        int clicks = 5;
        for (int i = 0; i < clicks; i++) {
            given().spec(spec).redirects().follow(false).get("/{code}", shortCode);
        }
        Thread.sleep(500);

        given().spec(spec)
                .when().get("/api/v1/urls/{code}/analytics", shortCode)
                .then()
                .statusCode(200)
                .body("totalClicks", greaterThanOrEqualTo(clicks));
    }

    @Test
    void analytics_afterClicks_populatesRecentClicks() throws InterruptedException {
        for (int i = 0; i < 3; i++) {
            given().spec(spec).redirects().follow(false).get("/{code}", shortCode);
        }
        Thread.sleep(500);

        given().spec(spec)
                .when().get("/api/v1/urls/{code}/analytics", shortCode)
                .then()
                .statusCode(200)
                .body("recentClicks.size()", greaterThanOrEqualTo(3))
                .body("recentClicks[0].clickedAt", notNullValue());
    }

    @Test
    void analytics_afterClicks_populatesClicksByDay() throws InterruptedException {
        given().spec(spec).redirects().follow(false).get("/{code}", shortCode);
        Thread.sleep(500);

        given().spec(spec)
                .when().get("/api/v1/urls/{code}/analytics", shortCode)
                .then()
                .statusCode(200)
                .body("clicksByDay.size()", greaterThanOrEqualTo(1));
    }

    @Test
    void urlInfo_clickCount_incrementsAfterRedirect() throws InterruptedException {
        given().spec(spec)
                .when().get("/api/v1/urls/{code}", shortCode)
                .then().statusCode(200).body("clickCount", equalTo(0));

        given().spec(spec).redirects().follow(false).get("/{code}", shortCode);
        Thread.sleep(500);

        given().spec(spec)
                .when().get("/api/v1/urls/{code}", shortCode)
                .then().statusCode(200).body("clickCount", greaterThanOrEqualTo(1));
    }
}
