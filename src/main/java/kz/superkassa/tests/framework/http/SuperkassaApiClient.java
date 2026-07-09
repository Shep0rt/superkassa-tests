package ru.superkassa.tests.framework.http;

import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.RestAssured;
import io.restassured.specification.RequestSpecification;
import ru.superkassa.tests.framework.config.TestConfig;

public final class SuperkassaApiClient {
    private final TestConfig config;

    public SuperkassaApiClient(TestConfig config) {
        this.config = config;
    }

    public RequestSpecification request() {
        return RestAssured.given()
                .filter(new AllureRestAssured())
                .baseUri(config.baseUrl())
                .header("Authorization", "Bearer " + config.apiToken())
                .contentType("application/json")
                .accept("application/json");
    }
}
