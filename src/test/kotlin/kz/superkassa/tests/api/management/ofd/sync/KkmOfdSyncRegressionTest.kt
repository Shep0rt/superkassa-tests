package kz.superkassa.tests.api.management.ofd.sync

import io.qameta.allure.Allure
import io.qameta.allure.Feature
import io.qameta.allure.Owner
import io.qameta.allure.Severity
import io.qameta.allure.SeverityLevel
import io.qameta.allure.Story
import io.restassured.http.ContentType
import io.restassured.http.Method
import io.restassured.response.Response
import kz.superkassa.tests.framework.kkm.KkmAuthenticatedTest
import kz.superkassa.tests.framework.assertions.ApiContractErrorMessages
import kz.superkassa.tests.framework.contract.ApiEnumValues
import kz.superkassa.tests.framework.kkm.PreparedKkmAuth
import kz.superkassa.tests.framework.reporting.reportStep
import kz.superkassa.tests.framework.tags.ApiRegression
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.SoftAssertions
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.ResourceAccessMode
import org.junit.jupiter.api.parallel.ResourceLock
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

@ApiRegression
@Feature("API")
@Story("POST /kkm/{kkmId}/ofd/sync")
@Owner("Pavel Michka")
@DisplayName("POST /kkm/{kkmId}/ofd/sync: регрессионные проверки синхронизации ККМ с ОФД")
@ResourceLock(value = "kkm-state", mode = ResourceAccessMode.READ_WRITE)
@Suppress("SameParameterValue", "NonAsciiCharacters")
class KkmOfdSyncRegressionTest : KkmAuthenticatedTest() {
    private var kkmToExitAfterTest: PreparedKkmAuth? = null

    @AfterEach
    fun `Восстанавливаем режим ККМ после проверки`() {
        val preparedKkm = kkmToExitAfterTest
        if (preparedKkm == null) {
            Allure.step("Возврат не требуется: ККМ не оставлена в режиме программирования")
            return
        }
        kkmToExitAfterTest = null
        exitProgrammingForCleanup(preparedKkm)
    }

    @Nested
    @ApiRegression
    @DisplayName("Позитивные проверки POST /kkm/{kkmId}/ofd/sync")
    @ResourceLock(value = "kkm-state", mode = ResourceAccessMode.READ_WRITE)
    inner class PositiveRegressionTests {
        @BeforeEach
        fun `Переводим ККМ в режим программирования`() {
            enterProgramming(preparedKkm)
        }

        @Test
        @Severity(SeverityLevel.CRITICAL)
        @DisplayName("Метод POST /kkm/{kkmId}/ofd/sync возвращает поля ожидаемых типов")
        fun shouldReturnExpectedFieldTypes() {
            val response = syncOfd()

            SoftAssertions().apply {
                assertRequiredFieldType(this, response, STATUS_FIELD, String::class.java)

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
        @DisplayName("Метод POST /kkm/{kkmId}/ofd/sync не возвращает поля вне Swagger-контракта")
        fun shouldNotReturnFieldsOutsideSwaggerContract() {
            val response = syncOfd()
            val unexpectedFields = response.keys - RESPONSE_FIELDS

            assertThat(unexpectedFields)
                .withFailMessage(
                    ApiContractErrorMessages.unexpectedSwaggerFields(
                        ENDPOINT,
                        RESPONSE_SCHEMA,
                        unexpectedFields
                    )
                )
                .isEmpty()
        }

        @Test
        @Severity(SeverityLevel.CRITICAL)
        @DisplayName("Метод POST /kkm/{kkmId}/ofd/sync возвращает допустимый статус команды ОФД")
        fun shouldReturnSupportedOfdCommandStatus() {
            val response = syncOfd()
            val status = response[STATUS_FIELD] as? String

            SoftAssertions().apply {
                assertThat(status)
                    .withFailMessage(
                        ApiContractErrorMessages.requiredEnumMissing(
                            ENDPOINT,
                            STATUS_FIELD,
                            RESPONSE_SCHEMA
                        )
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
    }

    @Nested
    @ApiRegression
    @DisplayName("Негативные проверки POST /kkm/{kkmId}/ofd/sync")
    @ResourceLock(value = "kkm-state", mode = ResourceAccessMode.READ_WRITE)
    inner class NegativeRegressionTests {
        @Nested
        @ApiRegression
        @DisplayName("Проверки авторизации POST /kkm/{kkmId}/ofd/sync")
        @ResourceLock(value = "kkm-state", mode = ResourceAccessMode.READ_WRITE)
        inner class AuthorizationRegressionTests {
            @Test
            @Severity(SeverityLevel.NORMAL)
            @DisplayName("Метод POST /kkm/{kkmId}/ofd/sync возвращает 401 без Authorization")
            fun shouldReturnUnauthorizedWithoutAuthorization() {

                reportStep("Проверяем POST ${syncPath(preparedKkm.kkmId)} без Authorization") {
                    superkassa.requestWithoutAuthorization()
                        .`when`()
                        .post(syncPath(preparedKkm.kkmId))
                        .then()
                        .shouldHaveStatus(401, "запрос синхронизации с ОФД без Authorization")
                        .contentType(ContentType.JSON)
                }
            }

            @Test
            @Severity(SeverityLevel.NORMAL)
            @DisplayName("Метод POST /kkm/{kkmId}/ofd/sync возвращает 403 для неверного PIN")
            fun shouldReturnForbiddenForInvalidPin() {

                reportStep("Проверяем POST ${syncPath(preparedKkm.kkmId)} с неверным PIN") {
                    superkassa.request(INVALID_PIN)
                        .`when`()
                        .post(syncPath(preparedKkm.kkmId))
                        .then()
                        .shouldHaveStatus(403, "запрос синхронизации с ОФД с неверным PIN")
                        .contentType(ContentType.JSON)
                }
            }
        }

        @Nested
        @ApiRegression
        @DisplayName("Проверки несуществующих идентификаторов")
        inner class MissingIdentifiersTests {
            @Test
            @Severity(SeverityLevel.NORMAL)
            @DisplayName("Метод POST /kkm/{kkmId}/ofd/sync возвращает 404 для несуществующей ККМ")
            fun shouldReturnNotFoundForUnknownKkmId() {

                reportStep("Проверяем POST ${syncPath(UNKNOWN_KKM_ID)} для несуществующей ККМ") {
                    superkassa.request(preparedKkm.adminPin)
                        .`when`()
                        .post(syncPath(UNKNOWN_KKM_ID))
                        .then()
                        .shouldHaveStatus(404, "синхронизация с ОФД для несуществующей ККМ")
                        .contentType(ContentType.JSON)
                }
            }

        }

        @Nested
        @ApiRegression
        @DisplayName("Проверки неподдерживаемых HTTP-методов")
        inner class UnsupportedHttpMethodsTests {
            @ParameterizedTest(name = "HTTP {0} /kkm/'{'kkmId'}'/ofd/sync возвращает 405")
            @EnumSource(value = Method::class, names = ["GET", "PUT", "PATCH", "DELETE"])
            @Severity(SeverityLevel.NORMAL)
            @DisplayName("Метод /kkm/{kkmId}/ofd/sync возвращает 405 для HTTP-методов кроме POST")
            fun shouldReturnMethodNotAllowedForNonPostMethods(method: Method) {

                reportStep("Проверяем, что HTTP $method ${syncPath(preparedKkm.kkmId)} не поддерживается") {
                    superkassa.request(preparedKkm.adminPin)
                        .`when`()
                        .request(method, syncPath(preparedKkm.kkmId))
                        .then()
                        .shouldHaveStatus(405, "неподдерживаемый HTTP-метод")
                }
            }

        }
    }

    private fun syncOfd(): Map<String, Any?> {
        return reportStep("Синхронизируем ККМ kkmId='${preparedKkm.kkmId}' с ОФД через POST ${syncPath(preparedKkm.kkmId)}") {
            superkassa.request(preparedKkm.adminPin)
                .`when`()
                .post(syncPath(preparedKkm.kkmId))
                .then()
                .shouldHaveStatus(200, "синхронизация ККМ с ОФД")
                .contentType(ContentType.JSON)
                .extract()
                .jsonPath()
                .getMap("")
        }
    }

    private fun enterProgramming(preparedKkm: PreparedKkmAuth) {
        reportStep("Готовим предусловие: переводим ККМ kkmId='${preparedKkm.kkmId}' в режим программирования") {
            val response: Response = superkassa.request(preparedKkm.adminPin)
                .`when`()
                .post("/kkm/{kkmId}/programming/enter", preparedKkm.kkmId)
                .then()
                .extract()
                .response()

            if (response.statusCode == 200) {
                kkmToExitAfterTest = preparedKkm
            }

            response.then()
                .shouldHaveStatus(200, "подготовка: вход ККМ в режим программирования перед синхронизацией с ОФД")
                .contentType(ContentType.JSON)
        }
    }

    private fun exitProgrammingForCleanup(preparedKkm: PreparedKkmAuth) {
        reportStep("Возвращаем ККМ kkmId='${preparedKkm.kkmId}' из режима программирования после проверки синхронизации с ОФД") {
            superkassa.request(preparedKkm.adminPin)
                .`when`()
                .post("/kkm/{kkmId}/programming/exit", preparedKkm.kkmId)
                .then()
                .shouldHaveStatus(200, "cleanup: выход ККМ из режима программирования")
                .contentType(ContentType.JSON)
        }
    }

    private fun assertRequiredFieldType(
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
                    RESPONSE_SCHEMA
                )
            )
            .isInstanceOf(expectedType)
    }

    private fun assertOptionalFieldType(
        softly: SoftAssertions,
        response: Map<String, Any?>,
        fieldName: String,
        expectedType: Class<*>,
    ) {
        val fieldValue = response[fieldName] ?: return

        softly.assertThat(fieldValue)
            .withFailMessage(
                ApiContractErrorMessages.optionalFieldTypeMismatch(
                    ENDPOINT,
                    fieldName,
                    expectedType.simpleName,
                    RESPONSE_SCHEMA
                )
            )
            .isInstanceOf(expectedType)
    }

    private fun assertOptionalIntegerFieldType(
        softly: SoftAssertions,
        response: Map<String, Any?>,
        fieldName: String,
    ) {
        val fieldValue = response[fieldName] ?: return

        softly.assertThat(fieldValue)
            .withFailMessage(
                ApiContractErrorMessages.optionalFieldTypeMismatch(
                    ENDPOINT,
                    fieldName,
                    "Integer",
                    RESPONSE_SCHEMA
                )
            )
            .isInstanceOfAny(Int::class.javaObjectType, Long::class.javaObjectType)
    }

    private fun syncPath(kkmId: String): String = "/kkm/$kkmId/ofd/sync"

    private companion object {
        const val ENDPOINT = "POST /kkm/{kkmId}/ofd/sync"
        const val RESPONSE_SCHEMA = "OfdCommandResponse"
        const val STATUS_FIELD = "status"
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
