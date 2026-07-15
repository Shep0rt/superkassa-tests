package kz.superkassa.tests.framework.http

import io.restassured.RestAssured
import io.restassured.specification.RequestSpecification
import kz.superkassa.tests.framework.config.TestConfig

@Suppress("unused")
class SuperkassaApiClient(
    private val config: TestConfig,
) {
    fun request(): RequestSpecification =
        RestAssured.given()
            .filter(AllureApiLoggingFilter())
            .baseUri(config.baseUrl)
            .header("Authorization", "Bearer ${config.apiToken}")
            .contentType("application/json")
            .accept("application/json")
}
