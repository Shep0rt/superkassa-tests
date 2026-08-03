package kz.superkassa.tests.api.diagnostics

import io.qameta.allure.Feature
import io.qameta.allure.Owner
import io.qameta.allure.Severity
import io.qameta.allure.SeverityLevel
import io.qameta.allure.Story
import io.restassured.http.ContentType
import io.restassured.http.Method
import io.restassured.path.json.JsonPath
import io.restassured.response.Response
import kz.superkassa.tests.framework.BaseApiTest
import kz.superkassa.tests.framework.contract.ApiEnumValues
import kz.superkassa.tests.framework.assertions.ApiContractErrorMessages
import kz.superkassa.tests.framework.reporting.reportStep
import kz.superkassa.tests.framework.tags.ApiRegression
import org.assertj.core.api.SoftAssertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

@ApiRegression
@Feature("API")
@Story("GET /health")
@Owner("Pavel Michka")
@DisplayName("GET /health: регрессионные проверки состояния сервиса")
@Suppress("SameParameterValue")
class HealthRegressionTest : BaseApiTest() {
    @Nested
    @ApiRegression
    @DisplayName("Позитивные проверки GET /health")
    inner class PositiveRegressionTests {
        @Test
        @Severity(SeverityLevel.CRITICAL)
        @DisplayName("Метод GET /health возвращает поля ожидаемых типов")
        fun shouldReturnExpectedFieldTypes() {
            val json = getHealthJson()
            val response = json.getMap<String, Any?>("")

            SoftAssertions().apply {
                assertFieldType(this, response, ENDPOINT, "storage", String::class.java, "HealthResponse")
                assertFieldType(this, response, ENDPOINT, "status", String::class.java, "HealthResponse")
            }.assertAll()
        }

        @Test
        @Severity(SeverityLevel.CRITICAL)
        @DisplayName("Метод GET /health не возвращает поля вне ожидаемой структуры")
        fun shouldNotReturnFieldsOutsideExpectedStructure() {
            val json = getHealthJson()
            val response = json.getMap<String, Any?>("")

            SoftAssertions().apply {
                assertOnlySwaggerFields(this, response, ENDPOINT, "HealthResponse", HEALTH_RESPONSE_FIELDS)
            }.assertAll()
        }

        @Test
        @Severity(SeverityLevel.NORMAL)
        @DisplayName("Метод GET /health с checkOfd=false возвращает базовую структуру")
        fun shouldReturnBaseHealthWhenOfdCheckDisabled() {
            val json = getHealthJson(checkOfd = false)
            val response = json.getMap<String, Any?>("")

            SoftAssertions().apply {
                assertFieldType(this, response, ENDPOINT, "storage", String::class.java, "HealthResponse")
                assertFieldType(this, response, ENDPOINT, "status", String::class.java, "HealthResponse")
                assertOnlySwaggerFields(this, response, ENDPOINT, "HealthResponse", HEALTH_RESPONSE_FIELDS)
            }.assertAll()
        }

        @Test
        @Severity(SeverityLevel.NORMAL)
        @DisplayName("Метод GET /health с checkOfd=true возвращает результаты проверки ОФД")
        fun shouldReturnOfdHealthWhenOfdCheckEnabled() {
            val json = getHealthJson(checkOfd = true)
            val response = json.getMap<String, Any?>("")

            SoftAssertions().apply {
                assertFieldType(this, response, ENDPOINT, "storage", String::class.java, "HealthResponse")
                assertFieldType(this, response, ENDPOINT, "status", String::class.java, "HealthResponse")
                assertFieldType(this, response, ENDPOINT, "ofd", Map::class.java, "HealthResponse")
                assertOnlySwaggerFields(this, response, ENDPOINT, "HealthResponse", HEALTH_WITH_OFD_RESPONSE_FIELDS)
                assertOfdStatuses(this, json.getMap("ofd"))
            }.assertAll()
        }

        @Test
        @Severity(SeverityLevel.NORMAL)
        @DisplayName("Метод GET /health фильтрует проверку ОФД по окружению")
        fun shouldFilterOfdHealthByEnvironment() {
            val json = getHealthJson(checkOfd = true, ofdEnvironment = "TEST")
            val ofd = json.getMap<String, Any?>("ofd")

            SoftAssertions().apply {
                assertOfdStatuses(this, ofd)
                ofd.keys.forEach { key ->
                    assertThat(key)
                        .withFailMessage(
                            ApiContractErrorMessages.filterResultMismatch(
                                ENDPOINT,
                                "ofdEnvironment",
                                "TEST",
                                key,
                                "ключ должен оканчиваться на ':TEST'",
                            ),
                        )
                        .endsWith(":TEST")
                }
            }.assertAll()
        }

        @Test
        @Severity(SeverityLevel.NORMAL)
        @DisplayName("Метод GET /health фильтрует проверку ОФД по провайдеру")
        fun shouldFilterOfdHealthByProvider() {
            val json = getHealthJson(checkOfd = true, ofdProvider = "KAZAKHTELECOM")
            val ofd = json.getMap<String, Any?>("ofd")

            SoftAssertions().apply {
                assertOfdStatuses(this, ofd)
                ofd.keys.forEach { key ->
                    assertThat(key)
                        .withFailMessage(
                            ApiContractErrorMessages.filterResultMismatch(
                                ENDPOINT,
                                "ofdProvider",
                                "KAZAKHTELECOM",
                                key,
                                "ключ должен начинаться с 'KAZAKHTELECOM:'",
                            ),
                        )
                        .startsWith("KAZAKHTELECOM:")
                }
            }.assertAll()
        }

    }

    @Nested
    @ApiRegression
    @DisplayName("Негативные проверки GET /health")
    inner class NegativeRegressionTests {
        @Nested
        @ApiRegression
        @DisplayName("Проверки невалидных параметров запроса")
        inner class InvalidRequestParametersTests {
            @Test
            @Severity(SeverityLevel.NORMAL)
            @DisplayName("Метод GET /health возвращает 400 для невалидного checkOfd")
            fun shouldReturnBadRequestForInvalidCheckOfd() {
                reportStep("Проверяем GET /health с невалидным checkOfd") {
                    superkassa.request()
                        .queryParam("checkOfd", "invalid")
                        .`when`()
                        .get("/health")
                        .then()
                        .shouldHaveStatus(400, "невалидный запрос")
                        .contentType(ContentType.JSON)
                }
            }

        }

        @Nested
        @ApiRegression
        @DisplayName("Проверки неподдерживаемых HTTP-методов")
        inner class UnsupportedHttpMethodsTests {
            @ParameterizedTest(name = "HTTP {0} /health возвращает 405")
            @EnumSource(value = Method::class, names = ["POST", "PUT", "PATCH", "DELETE"])
            @Severity(SeverityLevel.NORMAL)
            @DisplayName("Метод /health возвращает 405 для HTTP-методов кроме GET")
            fun shouldReturnMethodNotAllowedForNonGetMethods(method: Method) {
                reportStep("Проверяем, что HTTP $method /health не поддерживается") {
                    superkassa.request()
                        .`when`()
                        .request(method, "/health")
                        .then()
                        .shouldHaveStatus(405, "неподдерживаемый HTTP-метод")
                }
            }

        }
    }

    private fun getHealthJson(
        checkOfd: Boolean? = null,
        ofdEnvironment: String? = null,
        ofdProvider: String? = null,
    ): JsonPath {
        val request = superkassa.request()

        checkOfd?.let { request.queryParam("checkOfd", it) }
        ofdEnvironment?.let { request.queryParam("ofdEnvironment", it) }
        ofdProvider?.let { request.queryParam("ofdProvider", it) }

        val response: Response = reportStep("Получаем состояние сервиса через GET /health") {
            request
                .`when`()
                .get("/health")
                .then()
                .shouldHaveStatus(200, "успешный запрос")
                .contentType(ContentType.JSON)
                .extract()
                .response()
        }

        return response.jsonPath()
    }

    private fun assertFieldType(
        softly: SoftAssertions,
        item: Map<String, Any?>,
        endpoint: String,
        fieldName: String,
        expectedType: Class<*>,
        schemaName: String,
    ) {
        softly.assertThat(item)
            .withFailMessage(
                ApiContractErrorMessages.requiredFieldWithTypeMissing(
                    endpoint,
                    fieldName,
                    expectedType.simpleName,
                    schemaName
                )
            )
            .containsKey(fieldName)

        softly.assertThat(item[fieldName])
            .withFailMessage(
                ApiContractErrorMessages.fieldTypeMismatch(
                    endpoint,
                    fieldName,
                    expectedType.simpleName,
                    schemaName
                )
            )
            .isInstanceOf(expectedType)
    }

    private fun assertOnlySwaggerFields(
        softly: SoftAssertions,
        item: Map<String, Any?>,
        endpoint: String,
        schemaName: String,
        allowedFields: Set<String>,
    ) {
        val unexpectedFields = item.keys - allowedFields

        softly.assertThat(unexpectedFields)
            .withFailMessage(ApiContractErrorMessages.unexpectedSwaggerFields(endpoint, schemaName, unexpectedFields))
            .isEmpty()
    }

    private fun assertOfdStatuses(softly: SoftAssertions, ofd: Map<String, Any?>) {
        ofd.forEach { (key, status) ->
            softly.assertThat(key)
                .withFailMessage(
                    ApiContractErrorMessages.valueFormatMismatch(
                        ENDPOINT,
                        "ofd.$key",
                        key,
                        "PROVIDER:ENVIRONMENT",
                        "HealthResponse",
                    ),
                )
                .contains(":")
            softly.assertThat(status)
                .withFailMessage(
                    ApiContractErrorMessages.documentedFieldTypeMismatch(
                        ENDPOINT,
                        "ofd.$key",
                        "String",
                        status,
                        "HealthResponse",
                    ),
                )
                .isInstanceOf(String::class.java)
            softly.assertThat(status as? String)
                .withFailMessage(
                    ApiContractErrorMessages.valuePrefixUnsupported(
                        ENDPOINT,
                        "ofd.$key",
                        status as? String,
                        ApiEnumValues.HEALTH_OFD_STATUS_PREFIXES,
                    ),
                )
                .matches { value -> ApiEnumValues.HEALTH_OFD_STATUS_PREFIXES.any { value.startsWith(it) } }
        }
    }

    private companion object {
        const val ENDPOINT = "GET /health"

        val HEALTH_RESPONSE_FIELDS = setOf("storage", "status")
        val HEALTH_WITH_OFD_RESPONSE_FIELDS = setOf("storage", "status", "ofd")
    }
}
