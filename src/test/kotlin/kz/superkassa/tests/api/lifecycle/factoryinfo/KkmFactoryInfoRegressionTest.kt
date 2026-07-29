package kz.superkassa.tests.api.lifecycle.factoryinfo

import io.qameta.allure.Feature
import io.qameta.allure.Owner
import io.qameta.allure.Severity
import io.qameta.allure.SeverityLevel
import io.qameta.allure.Story
import io.restassured.http.ContentType
import io.restassured.http.Method
import kz.superkassa.tests.framework.BaseApiTest
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
@Story("GET /kkm/factory-info")
@Owner("Pavel Michka")
@DisplayName("GET /kkm/factory-info: регрессионные проверки генерации заводской информации ККМ")
class KkmFactoryInfoRegressionTest : BaseApiTest() {
    @Nested
    @ApiRegression
    @DisplayName("Позитивные проверки GET /kkm/factory-info")
    inner class PositiveRegressionTests {
        @Test
        @Severity(SeverityLevel.CRITICAL)
        @DisplayName("Метод GET /kkm/factory-info возвращает поля ожидаемых типов")
        fun shouldReturnExpectedFieldTypes() {
            val response = getFactoryInfo()

            SoftAssertions().apply {
                assertFieldType(this, response, "factoryNumber", String::class.java)
                assertFieldType(this, response, "manufactureYear", Int::class.javaObjectType)
            }.assertAll()
        }

        @Test
        @Severity(SeverityLevel.CRITICAL)
        @DisplayName("Метод GET /kkm/factory-info не возвращает поля вне Swagger-контракта")
        fun shouldNotReturnFieldsOutsideSwaggerContract() {
            val response = getFactoryInfo()
            val unexpectedFields = response.keys - RESPONSE_FIELDS

            SoftAssertions().apply {
                assertThat(unexpectedFields)
                    .withFailMessage(
                        ApiContractErrorMessages.unexpectedSwaggerFields(
                            ENDPOINT,
                            RESPONSE_SCHEMA,
                            unexpectedFields,
                        ),
                    )
                    .isEmpty()
            }.assertAll()
        }

    }

    @Nested
    @ApiRegression
    @DisplayName("Негативные проверки GET /kkm/factory-info")
    inner class NegativeRegressionTests {
        @Nested
        @ApiRegression
        @DisplayName("Проверки неподдерживаемых HTTP-методов")
        inner class UnsupportedHttpMethodsTests {
            @ParameterizedTest(name = "HTTP {0} /kkm/factory-info возвращает 405")
            @EnumSource(value = Method::class, names = ["POST", "PUT", "PATCH", "DELETE"])
            @Severity(SeverityLevel.NORMAL)
            @DisplayName("Метод /kkm/factory-info возвращает 405 для HTTP-методов кроме GET")
            fun shouldReturnMethodNotAllowedForUnsupportedMethods(method: Method) {
                reportStep("Проверяем, что HTTP $method /kkm/factory-info не поддерживается") {
                    superkassa.requestWithoutAuthorization()
                        .`when`()
                        .request(method, "/kkm/factory-info")
                        .then()
                        .shouldHaveStatus(405, "неподдерживаемый HTTP-метод")
                }
            }

        }
    }

    private fun getFactoryInfo(): Map<String, Any?> =
        reportStep("Получаем заводской номер и год выпуска через GET /kkm/factory-info без авторизации") {
            superkassa.requestWithoutAuthorization()
                .`when`()
                .get("/kkm/factory-info")
                .then()
                .shouldHaveStatus(200, "публичное получение заводской информации ККМ")
                .contentType(ContentType.JSON)
                .extract()
                .jsonPath()
                .getMap("")
        }

    private fun assertFieldType(
        softly: SoftAssertions,
        response: Map<String, Any?>,
        fieldName: String,
        expectedType: Class<*>,
    ) {
        softly.assertThat(response)
            .withFailMessage(
                ApiContractErrorMessages.requiredFieldWithTypeMissing(
                    ENDPOINT,
                    fieldName,
                    expectedType.simpleName,
                    RESPONSE_SCHEMA,
                ),
            )
            .containsKey(fieldName)

        softly.assertThat(response[fieldName])
            .withFailMessage(
                ApiContractErrorMessages.fieldTypeMismatch(
                    ENDPOINT,
                    fieldName,
                    expectedType.simpleName,
                    RESPONSE_SCHEMA,
                ),
            )
            .isInstanceOf(expectedType)
    }

    private companion object {
        const val ENDPOINT = "GET /kkm/factory-info"
        const val RESPONSE_SCHEMA = "FactoryNumberResponse"

        val RESPONSE_FIELDS = setOf(
            "factoryNumber",
            "manufactureYear",
        )
    }
}
