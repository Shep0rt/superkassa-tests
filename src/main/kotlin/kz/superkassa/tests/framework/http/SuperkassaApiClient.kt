package kz.superkassa.tests.framework.http

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.restassured.RestAssured
import io.restassured.specification.RequestSpecification
import kz.superkassa.tests.api.info.InfoResponse
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

    fun getInfo(): InfoResponse =
        request()
            .`when`()
            .get("/info")
            .then()
            .statusCode(200)
            .extract()
            .asString()
            .let { OBJECT_MAPPER.readValue(it, InfoResponse::class.java) }

    private companion object {
        val OBJECT_MAPPER: ObjectMapper = jacksonObjectMapper()
    }
}
