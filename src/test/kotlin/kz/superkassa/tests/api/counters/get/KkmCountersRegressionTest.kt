package kz.superkassa.tests.api.counters.get

import io.qameta.allure.Feature
import io.qameta.allure.Owner
import io.qameta.allure.Severity
import io.qameta.allure.SeverityLevel
import io.qameta.allure.Story
import io.restassured.http.ContentType
import io.restassured.http.Method
import io.restassured.response.Response
import kz.superkassa.tests.framework.assertions.ApiContractErrorMessages
import kz.superkassa.tests.framework.contract.ApiEnumValues
import kz.superkassa.tests.framework.kkm.KkmAuthenticatedTest
import kz.superkassa.tests.framework.reporting.reportStep
import kz.superkassa.tests.framework.tags.ApiRegression
import org.assertj.core.api.SoftAssertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.ResourceAccessMode
import org.junit.jupiter.api.parallel.ResourceLock
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

@ApiRegression
@Feature("API")
@Story("GET /kkm/{kkmId}/counters")
@Owner("Pavel Michka")
@DisplayName("GET /kkm/{kkmId}/counters: регрессионные проверки получения счетчиков ККМ")
@ResourceLock(value = "kkm-counters", mode = ResourceAccessMode.READ)
class KkmCountersRegressionTest : KkmAuthenticatedTest() {
    @Nested
    @DisplayName("Позитивные проверки GET /kkm/{kkmId}/counters")
    inner class PositiveRegressionTests {
        @Test
        @Severity(SeverityLevel.CRITICAL)
        @DisplayName("Метод GET /kkm/{kkmId}/counters возвращает поля ожидаемых типов")
        fun shouldReturnExpectedFieldTypes() {
            val counters = getCounters()

            SoftAssertions().apply {
                counters.forEachIndexed { index, counter ->
                    assertRequiredFieldType(this, counter, index, "key", String::class.java)
                    assertRequiredFieldType(this, counter, index, "scope", String::class.java)
                    assertOptionalFieldType(this, counter, index, String::class.java)
                    assertRequiredIntegerFieldType(this, counter, index, "updatedAt")
                    assertRequiredIntegerFieldType(this, counter, index, "value")
                }
            }.assertAll()
        }

        @Test
        @Severity(SeverityLevel.CRITICAL)
        @DisplayName("Метод GET /kkm/{kkmId}/counters возвращает допустимые области видимости счетчиков")
        fun shouldReturnSupportedCounterScopes() {
            val counters = getCounters()

            SoftAssertions().apply {
                counters.forEachIndexed { index, counter ->
                    assertRequiredEnumValue(this, counter, index, ApiEnumValues.COUNTER_SCOPES)
                }
            }.assertAll()
        }

        @Test
        @Severity(SeverityLevel.CRITICAL)
        @DisplayName("Метод GET /kkm/{kkmId}/counters не возвращает поля вне Swagger-контракта")
        fun shouldNotReturnFieldsOutsideSwaggerContract() {
            val counters = getCounters()

            SoftAssertions().apply {
                counters.forEach { counter ->
                    assertOnlySwaggerFields(this, counter)
                }
            }.assertAll()
        }
    }

    @Nested
    @DisplayName("Негативные проверки GET /kkm/{kkmId}/counters")
    inner class NegativeRegressionTests {
        @Nested
        @DisplayName("Проверки авторизации GET /kkm/{kkmId}/counters")
        inner class AuthorizationRegressionTests {
            @Test
            @Severity(SeverityLevel.NORMAL)
            @DisplayName("Метод GET /kkm/{kkmId}/counters возвращает 401 без Authorization")
            fun shouldReturnUnauthorizedWithoutAuthorization() {
                reportStep("Проверяем GET ${countersPath(preparedKkm.kkmId)} без Authorization") {
                    superkassa.requestWithoutAuthorization()
                        .`when`()
                        .get(countersPath(preparedKkm.kkmId))
                        .then()
                        .shouldHaveStatus(401, "запрос без Authorization")
                        .contentType(ContentType.JSON)
                }
            }

            @Test
            @Severity(SeverityLevel.NORMAL)
            @DisplayName("Метод GET /kkm/{kkmId}/counters возвращает 403 для неверного PIN")
            fun shouldReturnForbiddenForInvalidPin() {
                reportStep("Проверяем GET ${countersPath(preparedKkm.kkmId)} с неверным PIN") {
                    superkassa.request(INVALID_PIN)
                        .`when`()
                        .get(countersPath(preparedKkm.kkmId))
                        .then()
                        .shouldHaveStatus(403, "запрос с неверным PIN")
                        .contentType(ContentType.JSON)
                }
            }
        }

        @Nested
        @DisplayName("Проверки несуществующих идентификаторов")
        inner class MissingIdentifiersTests {
            @Test
            @Severity(SeverityLevel.NORMAL)
            @DisplayName("Метод GET /kkm/{kkmId}/counters возвращает 404 для несуществующей ККМ")
            fun shouldReturnNotFoundForUnknownKkmId() {
                reportStep("Проверяем GET ${countersPath(UNKNOWN_KKM_ID)} для несуществующей ККМ") {
                    superkassa.request(preparedKkm.adminPin)
                        .`when`()
                        .get(countersPath(UNKNOWN_KKM_ID))
                        .then()
                        .shouldHaveStatus(404, "несуществующая ККМ")
                        .contentType(ContentType.JSON)
                }
            }
        }

        @Nested
        @DisplayName("Проверки неподдерживаемых HTTP-методов")
        inner class UnsupportedHttpMethodsTests {
            @ParameterizedTest(name = "HTTP {0} /kkm/'{'kkmId'}'/counters возвращает 405")
            @EnumSource(value = Method::class, names = ["POST", "PUT", "PATCH", "DELETE"])
            @Severity(SeverityLevel.NORMAL)
            @DisplayName("Метод /kkm/{kkmId}/counters возвращает 405 для HTTP-методов кроме GET")
            fun shouldReturnMethodNotAllowedForNonGetMethods(method: Method) {
                reportStep("Проверяем, что HTTP $method ${countersPath(preparedKkm.kkmId)} не поддерживается") {
                    superkassa.request(preparedKkm.adminPin)
                        .`when`()
                        .request(method, countersPath(preparedKkm.kkmId))
                        .then()
                        .shouldHaveStatus(405, "неподдерживаемый HTTP-метод")
                }
            }
        }
    }

    private fun getCounters(): List<Map<String, Any?>> =
        getCountersResponse().jsonPath().getList("")

    private fun getCountersResponse(): Response =
        reportStep(
            "Получаем счетчики ККМ kkmId='${preparedKkm.kkmId}' через GET ${countersPath(preparedKkm.kkmId)}",
        ) {
            superkassa.request(preparedKkm.adminPin)
                .`when`()
                .get(countersPath(preparedKkm.kkmId))
                .then()
                .shouldHaveStatus(200, "получение счетчиков ККМ")
                .contentType(ContentType.JSON)
                .extract()
                .response()
        }

    private fun assertRequiredFieldType(
        softly: SoftAssertions,
        counter: Map<String, Any?>,
        index: Int,
        fieldName: String,
        expectedType: Class<*>,
    ) {
        val fieldPath = "counters[$index].$fieldName"

        softly.assertThat(counter)
            .withFailMessage(
                ApiContractErrorMessages.requiredFieldWithTypeMissing(
                    ENDPOINT,
                    fieldPath,
                    expectedType.simpleName,
                    RESPONSE_SCHEMA,
                ),
            )
            .containsKey(fieldName)

        softly.assertThat(counter[fieldName])
            .withFailMessage(
                ApiContractErrorMessages.fieldTypeMismatch(
                    ENDPOINT,
                    fieldPath,
                    expectedType.simpleName,
                    RESPONSE_SCHEMA,
                ),
            )
            .isInstanceOf(expectedType)
    }

    private fun assertRequiredIntegerFieldType(
        softly: SoftAssertions,
        counter: Map<String, Any?>,
        index: Int,
        fieldName: String,
    ) {
        val fieldPath = "counters[$index].$fieldName"

        softly.assertThat(counter)
            .withFailMessage(
                ApiContractErrorMessages.requiredFieldWithTypeMissing(
                    ENDPOINT,
                    fieldPath,
                    INTEGER_TYPE,
                    RESPONSE_SCHEMA,
                ),
            )
            .containsKey(fieldName)

        softly.assertThat(counter[fieldName])
            .withFailMessage(
                ApiContractErrorMessages.fieldTypeMismatch(
                    ENDPOINT,
                    fieldPath,
                    INTEGER_TYPE,
                    RESPONSE_SCHEMA,
                ),
            )
            .isInstanceOfAny(Int::class.javaObjectType, Long::class.javaObjectType)
    }

    private fun assertOptionalFieldType(
        softly: SoftAssertions,
        counter: Map<String, Any?>,
        index: Int,
        expectedType: Class<*>,
    ) {
        val fieldName = "shiftId"
        val value = counter[fieldName] ?: return
        val fieldPath = "counters[$index].$fieldName"

        softly.assertThat(value)
            .withFailMessage(
                ApiContractErrorMessages.optionalFieldTypeMismatch(
                    ENDPOINT,
                    fieldPath,
                    expectedType.simpleName,
                    RESPONSE_SCHEMA,
                ),
            )
            .isInstanceOf(expectedType)
    }

    private fun assertRequiredEnumValue(
        softly: SoftAssertions,
        counter: Map<String, Any?>,
        index: Int,
        supportedValues: Set<String>,
    ) {
        val fieldName = "scope"
        val fieldPath = "counters[$index].$fieldName"
        val value = counter[fieldName] as? String

        softly.assertThat(value)
            .withFailMessage(ApiContractErrorMessages.requiredEnumMissing(ENDPOINT, fieldPath, RESPONSE_SCHEMA))
            .isNotBlank()

        softly.assertThat(value)
            .withFailMessage(ApiContractErrorMessages.enumUnsupported(ENDPOINT, fieldPath, value, supportedValues))
            .isIn(supportedValues)
    }

    private fun assertOnlySwaggerFields(
        softly: SoftAssertions,
        counter: Map<String, Any?>,
    ) {
        val unexpectedFields = counter.keys - RESPONSE_FIELDS

        softly.assertThat(unexpectedFields)
            .withFailMessage(
                ApiContractErrorMessages.unexpectedSwaggerFields(
                    ENDPOINT,
                    RESPONSE_SCHEMA,
                    unexpectedFields,
                ),
            )
            .isEmpty()
    }

    private fun countersPath(kkmId: String): String = "/kkm/$kkmId/counters"

    private companion object {
        const val ENDPOINT = "GET /kkm/{kkmId}/counters"
        const val RESPONSE_SCHEMA = "CounterSnapshotResponse"
        const val INTEGER_TYPE = "Integer (int64)"
        const val INVALID_PIN = "999999"
        const val UNKNOWN_KKM_ID = "00000000-0000-0000-0000-000000000000"

        val RESPONSE_FIELDS = setOf(
            "key",
            "scope",
            "shiftId",
            "updatedAt",
            "value",
        )
    }
}
