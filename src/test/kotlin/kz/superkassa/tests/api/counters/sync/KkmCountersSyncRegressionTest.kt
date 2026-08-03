package kz.superkassa.tests.api.counters.sync

import io.qameta.allure.Feature
import io.qameta.allure.Owner
import io.qameta.allure.Severity
import io.qameta.allure.SeverityLevel
import io.qameta.allure.Story
import io.restassured.http.ContentType
import io.restassured.http.Method
import kz.superkassa.tests.framework.assertions.ApiContractErrorMessages
import kz.superkassa.tests.framework.contract.ApiEnumValues
import kz.superkassa.tests.framework.kkm.KkmAuthenticatedTest
import kz.superkassa.tests.framework.reporting.reportStep
import kz.superkassa.tests.framework.tags.ApiRegression
import org.assertj.core.api.Assertions.assertThat
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
@Story("POST /kkm/{kkmId}/ofd/counters/sync")
@Owner("Pavel Michka")
@DisplayName("POST /kkm/{kkmId}/ofd/counters/sync: регрессионные проверки синхронизации счетчиков ККМ с ОФД")
@ResourceLock(value = "kkm-counters", mode = ResourceAccessMode.READ_WRITE)
@ResourceLock(value = "kkm-state", mode = ResourceAccessMode.READ)
class KkmCountersSyncRegressionTest : KkmAuthenticatedTest() {
    @Nested
    @DisplayName("Позитивные проверки POST /kkm/{kkmId}/ofd/counters/sync")
    inner class PositiveRegressionTests {
        @Test
        @Severity(SeverityLevel.CRITICAL)
        @DisplayName("Метод POST /kkm/{kkmId}/ofd/counters/sync возвращает поля ожидаемых типов")
        fun shouldReturnExpectedFieldTypes() {
            val response = syncCounters()

            SoftAssertions().apply {
                assertRequiredFieldType(this, response, String::class.java)

                OPTIONAL_STRING_FIELDS.forEach { fieldName ->
                    assertOptionalFieldType(this, response, fieldName, String::class.java)
                }
                OPTIONAL_INTEGER_FIELDS.forEach { fieldName ->
                    assertOptionalIntegerFieldType(this, response, fieldName)
                }
            }.assertAll()
        }

        @Test
        @Severity(SeverityLevel.CRITICAL)
        @DisplayName("Метод POST /kkm/{kkmId}/ofd/counters/sync возвращает допустимый статус команды ОФД")
        fun shouldReturnSupportedOfdCommandStatus() {
            val response = syncCounters()
            val status = response[STATUS_FIELD] as? String

            SoftAssertions().apply {
                assertThat(status)
                    .withFailMessage(
                        ApiContractErrorMessages.requiredEnumMissing(
                            ENDPOINT,
                            STATUS_FIELD,
                            RESPONSE_SCHEMA,
                        ),
                    )
                    .isNotBlank()

                assertThat(status)
                    .withFailMessage(
                        ApiContractErrorMessages.enumUnsupported(
                            ENDPOINT,
                            STATUS_FIELD,
                            status,
                            ApiEnumValues.OFD_COMMAND_STATUSES,
                        ),
                    )
                    .isIn(ApiEnumValues.OFD_COMMAND_STATUSES)
            }.assertAll()
        }

        @Test
        @Severity(SeverityLevel.CRITICAL)
        @DisplayName("Метод POST /kkm/{kkmId}/ofd/counters/sync не возвращает поля вне Swagger-контракта")
        fun shouldNotReturnFieldsOutsideSwaggerContract() {
            val response = syncCounters()
            val unexpectedFields = response.keys - RESPONSE_FIELDS

            assertThat(unexpectedFields)
                .withFailMessage(
                    ApiContractErrorMessages.unexpectedSwaggerFields(
                        ENDPOINT,
                        RESPONSE_SCHEMA,
                        unexpectedFields,
                    ),
                )
                .isEmpty()
        }
    }

    @Nested
    @DisplayName("Негативные проверки POST /kkm/{kkmId}/ofd/counters/sync")
    inner class NegativeRegressionTests {
        @Nested
        @DisplayName("Проверки авторизации POST /kkm/{kkmId}/ofd/counters/sync")
        inner class AuthorizationRegressionTests {
            @Test
            @Severity(SeverityLevel.NORMAL)
            @DisplayName("Метод POST /kkm/{kkmId}/ofd/counters/sync возвращает 401 без Authorization")
            fun shouldReturnUnauthorizedWithoutAuthorization() {
                reportStep("Проверяем POST ${countersSyncPath(preparedKkm.kkmId)} без Authorization") {
                    superkassa.requestWithoutAuthorization()
                        .`when`()
                        .post(countersSyncPath(preparedKkm.kkmId))
                        .then()
                        .shouldHaveStatus(401, "запрос синхронизации счетчиков без Authorization")
                        .contentType(ContentType.JSON)
                }
            }

            @Test
            @Severity(SeverityLevel.NORMAL)
            @DisplayName("Метод POST /kkm/{kkmId}/ofd/counters/sync возвращает 403 для неверного PIN")
            fun shouldReturnForbiddenForInvalidPin() {
                reportStep("Проверяем POST ${countersSyncPath(preparedKkm.kkmId)} с неверным PIN") {
                    superkassa.request(INVALID_PIN)
                        .`when`()
                        .post(countersSyncPath(preparedKkm.kkmId))
                        .then()
                        .shouldHaveStatus(403, "запрос синхронизации счетчиков с неверным PIN")
                        .contentType(ContentType.JSON)
                }
            }
        }

        @Nested
        @DisplayName("Проверки несуществующих идентификаторов")
        inner class MissingIdentifiersTests {
            @Test
            @Severity(SeverityLevel.NORMAL)
            @DisplayName("Метод POST /kkm/{kkmId}/ofd/counters/sync возвращает 404 для несуществующей ККМ")
            fun shouldReturnNotFoundForUnknownKkmId() {
                reportStep("Проверяем POST ${countersSyncPath(UNKNOWN_KKM_ID)} для несуществующей ККМ") {
                    superkassa.request(preparedKkm.adminPin)
                        .`when`()
                        .post(countersSyncPath(UNKNOWN_KKM_ID))
                        .then()
                        .shouldHaveStatus(404, "синхронизация счетчиков для несуществующей ККМ")
                        .contentType(ContentType.JSON)
                }
            }
        }

        @Nested
        @DisplayName("Проверки неподдерживаемых HTTP-методов")
        inner class UnsupportedHttpMethodsTests {
            @ParameterizedTest(name = "HTTP {0} /kkm/'{'kkmId'}'/ofd/counters/sync возвращает 405")
            @EnumSource(value = Method::class, names = ["GET", "PUT", "PATCH", "DELETE"])
            @Severity(SeverityLevel.NORMAL)
            @DisplayName("Метод /kkm/{kkmId}/ofd/counters/sync возвращает 405 для HTTP-методов кроме POST")
            fun shouldReturnMethodNotAllowedForNonPostMethods(method: Method) {
                reportStep("Проверяем, что HTTP $method ${countersSyncPath(preparedKkm.kkmId)} не поддерживается") {
                    superkassa.request(preparedKkm.adminPin)
                        .`when`()
                        .request(method, countersSyncPath(preparedKkm.kkmId))
                        .then()
                        .shouldHaveStatus(405, "неподдерживаемый HTTP-метод")
                }
            }
        }
    }

    private fun syncCounters(): Map<String, Any?> =
        reportStep(
            "Синхронизируем счетчики ККМ kkmId='${preparedKkm.kkmId}' с ОФД через POST " +
                countersSyncPath(preparedKkm.kkmId),
        ) {
            superkassa.request(preparedKkm.adminPin)
                .`when`()
                .post(countersSyncPath(preparedKkm.kkmId))
                .then()
                .shouldHaveStatus(200, "синхронизация счетчиков ККМ с ОФД")
                .contentType(ContentType.JSON)
                .extract()
                .jsonPath()
                .getMap("")
        }

    private fun assertRequiredFieldType(
        softly: SoftAssertions,
        response: Map<String, Any?>,
        expectedType: Class<*>,
    ) {
        val fieldName = STATUS_FIELD
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

    private fun assertOptionalFieldType(
        softly: SoftAssertions,
        response: Map<String, Any?>,
        fieldName: String,
        expectedType: Class<*>,
    ) {
        val value = response[fieldName] ?: return

        softly.assertThat(value)
            .withFailMessage(
                ApiContractErrorMessages.optionalFieldTypeMismatch(
                    ENDPOINT,
                    fieldName,
                    expectedType.simpleName,
                    RESPONSE_SCHEMA,
                ),
            )
            .isInstanceOf(expectedType)
    }

    private fun assertOptionalIntegerFieldType(
        softly: SoftAssertions,
        response: Map<String, Any?>,
        fieldName: String,
    ) {
        val value = response[fieldName] ?: return

        softly.assertThat(value)
            .withFailMessage(
                ApiContractErrorMessages.optionalFieldTypeMismatch(
                    ENDPOINT,
                    fieldName,
                    INTEGER_TYPE,
                    RESPONSE_SCHEMA,
                ),
            )
            .isInstanceOfAny(Int::class.javaObjectType, Long::class.javaObjectType)
    }

    private fun countersSyncPath(kkmId: String): String = "/kkm/$kkmId/ofd/counters/sync"

    private companion object {
        const val ENDPOINT = "POST /kkm/{kkmId}/ofd/counters/sync"
        const val RESPONSE_SCHEMA = "OfdCommandResponse"
        const val STATUS_FIELD = "status"
        const val INTEGER_TYPE = "Integer"
        const val INVALID_PIN = "999999"
        const val UNKNOWN_KKM_ID = "00000000-0000-0000-0000-000000000000"

        val OPTIONAL_STRING_FIELDS = setOf(
            "autonomousSign",
            "errorMessage",
            "fiscalSign",
            "receiptUrl",
            "resultText",
        )

        val OPTIONAL_INTEGER_FIELDS = setOf(
            "responseReqNum",
            "responseToken",
            "resultCode",
        )

        val RESPONSE_FIELDS = OPTIONAL_STRING_FIELDS + OPTIONAL_INTEGER_FIELDS + STATUS_FIELD
    }
}
