package kz.superkassa.tests.framework.http

import io.restassured.RestAssured
import io.restassured.specification.RequestSpecification
import kz.superkassa.tests.framework.config.TestConfig

@Suppress("unused")
class SuperkassaApiClient(
    private val config: TestConfig,
) {
    fun request(authPin: String = config.authPin): RequestSpecification =
        baseRequest()
            .header("Authorization", "Bearer $authPin")

    fun requestWithoutAuthorization(): RequestSpecification =
        baseRequest()

    private fun baseRequest(): RequestSpecification =
        RestAssured.given()
            .filter(AllureApiLoggingFilter())
            .baseUri(config.baseUrl)
            .contentType("application/json")
            .accept("application/json")
}
