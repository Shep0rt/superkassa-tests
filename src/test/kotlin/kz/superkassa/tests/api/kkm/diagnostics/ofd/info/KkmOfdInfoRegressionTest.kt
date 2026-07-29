package kz.superkassa.tests.api.kkm.diagnostics.ofd.info

import io.qameta.allure.Feature
import io.qameta.allure.Owner
import io.qameta.allure.Severity
import io.qameta.allure.SeverityLevel
import io.qameta.allure.Story
import io.restassured.http.ContentType
import io.restassured.http.Method
import io.restassured.response.Response
import kz.superkassa.tests.framework.BaseApiTest
import kz.superkassa.tests.framework.assertions.ApiContractErrorMessages
import kz.superkassa.tests.framework.contract.ApiEnumValues
import kz.superkassa.tests.framework.reporting.reportStep
import kz.superkassa.tests.framework.tags.ApiRegression
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.SoftAssertions
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.ResourceAccessMode
import org.junit.jupiter.api.parallel.ResourceLock
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.util.UUID

@ApiRegression
@Feature("API")
@Story("GET /kkm/{kkmId}/ofd/info")
@Owner("Pavel Michka")
@DisplayName("GET /kkm/{kkmId}/ofd/info: регрессионные проверки получения информации о ККМ из ОФД")
@ResourceLock(value = "kkm-state", mode = ResourceAccessMode.READ_WRITE)
@Suppress("NonAsciiCharacters")
class KkmOfdInfoRegressionTest : BaseApiTest() {
    private lateinit var kkmId: String

    @Nested
    @ApiRegression
    @DisplayName("Позитивные проверки GET /kkm/{kkmId}/ofd/info")
    inner class PositiveRegressionTests {
        @BeforeEach
        fun `Получаем контрольную ККМ`() {
            kkmId = firstKkmIdOrSkip()
        }

        @Test
        @Severity(SeverityLevel.CRITICAL)
        @DisplayName("Метод GET /kkm/{kkmId}/ofd/info возвращает поля ожидаемых типов")
        fun shouldReturnExpectedFieldTypes() {
            val response = getOfdInfo(kkmId)

            SoftAssertions().apply {
                assertRequiredStatusFieldType(this, response)

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
        @DisplayName("Метод GET /kkm/{kkmId}/ofd/info возвращает допустимый статус команды ОФД")
        fun shouldReturnSupportedOfdCommandStatus() {
            val response = getOfdInfo(kkmId)
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
        @DisplayName("Метод GET /kkm/{kkmId}/ofd/info не возвращает поля вне Swagger-контракта")
        fun shouldNotReturnFieldsOutsideSwaggerContract() {
            val response = getOfdInfo(kkmId)
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
    @ApiRegression
    @DisplayName("Негативные проверки GET /kkm/{kkmId}/ofd/info")
    inner class NegativeRegressionTests {
        @Nested
        @DisplayName("Проверки несуществующих идентификаторов")
        inner class MissingIdentifiersTests {
            @Test
            @Severity(SeverityLevel.NORMAL)
            @DisplayName("Метод GET /kkm/{kkmId}/ofd/info возвращает 404 для несуществующей ККМ")
            fun shouldReturnNotFoundForUnknownKkmId() {
                val unknownKkmId = UUID.randomUUID().toString()

                reportStep("Проверяем GET ${ofdInfoPath(unknownKkmId)} для несуществующей ККМ") {
                    superkassa.requestWithoutAuthorization()
                        .`when`()
                        .get(ofdInfoPath(unknownKkmId))
                        .then()
                        .shouldHaveStatus(404, "получение информации из ОФД для несуществующей ККМ")
                        .contentType(ContentType.JSON)
                }
            }
        }

        @Nested
        @DisplayName("Проверки неподдерживаемых HTTP-методов")
        inner class UnsupportedHttpMethodsTests {
            @BeforeEach
            fun `Получаем контрольную ККМ`() {
                kkmId = firstKkmIdOrSkip()
            }

            @ParameterizedTest(name = "HTTP {0} /kkm/'{'kkmId'}'/ofd/info возвращает 405")
            @EnumSource(value = Method::class, names = ["POST", "PUT", "PATCH", "DELETE"])
            @Severity(SeverityLevel.NORMAL)
            @DisplayName("Метод /kkm/{kkmId}/ofd/info возвращает 405 для HTTP-методов кроме GET")
            fun shouldReturnMethodNotAllowedForNonGetMethods(method: Method) {
                reportStep("Проверяем, что HTTP $method ${ofdInfoPath(kkmId)} не поддерживается") {
                    superkassa.requestWithoutAuthorization()
                        .`when`()
                        .request(method, ofdInfoPath(kkmId))
                        .then()
                        .shouldHaveStatus(405, "неподдерживаемый HTTP-метод")
                }
            }
        }
    }

    private fun getOfdInfo(kkmId: String): Map<String, Any?> =
        reportStep("Получаем информацию о ККМ kkmId='$kkmId' из ОФД через GET ${ofdInfoPath(kkmId)} без авторизации") {
            superkassa.requestWithoutAuthorization()
                .`when`()
                .get(ofdInfoPath(kkmId))
                .then()
                .shouldHaveStatus(200, "публичное получение информации о ККМ из ОФД")
                .contentType(ContentType.JSON)
                .extract()
                .jsonPath()
                .getMap("")
        }

    private fun firstKkmIdOrSkip(): String {
        val response: Response = reportStep("Получаем контрольную ККМ из списка GET /kkm?limit=1&offset=0") {
            superkassa.requestWithoutAuthorization()
                .queryParam("limit", 1)
                .queryParam("offset", 0)
                .`when`()
                .get("/kkm")
                .then()
                .shouldHaveStatus(200, "получение контрольной ККМ для проверки GET /kkm/{kkmId}/ofd/info")
                .contentType(ContentType.JSON)
                .extract()
                .response()
        }

        val items = response.jsonPath().getList<Map<String, Any?>>("items").orEmpty()
        assumeTrue(items.isNotEmpty(), "В системе нет ККМ для проверки GET /kkm/{kkmId}/ofd/info")

        val kkmId = items.first()["kkmId"] as? String
        assumeTrue(!kkmId.isNullOrBlank(), "В контрольной ККМ отсутствует заполненный kkmId")

        return kkmId!!
    }

    private fun assertRequiredStatusFieldType(
        softly: SoftAssertions,
        response: Map<String, Any?>,
    ) {
        softly.assertThat(response)
            .withFailMessage(
                ApiContractErrorMessages.requiredFieldWithTypeMissing(
                    ENDPOINT,
                    STATUS_FIELD,
                    String::class.java.simpleName,
                    RESPONSE_SCHEMA,
                ),
            )
            .containsKey(STATUS_FIELD)

        softly.assertThat(response[STATUS_FIELD])
            .withFailMessage(
                ApiContractErrorMessages.fieldTypeMismatch(
                    ENDPOINT,
                    STATUS_FIELD,
                    String::class.java.simpleName,
                    RESPONSE_SCHEMA,
                ),
            )
            .isInstanceOf(String::class.java)
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
        val fieldValue = response[fieldName] ?: return

        softly.assertThat(fieldValue)
            .withFailMessage(
                ApiContractErrorMessages.optionalFieldTypeMismatch(
                    ENDPOINT,
                    fieldName,
                    "Integer",
                    RESPONSE_SCHEMA,
                ),
            )
            .isInstanceOfAny(Int::class.javaObjectType, Long::class.javaObjectType)
    }

    private fun ofdInfoPath(kkmId: String): String = "/kkm/$kkmId/ofd/info"

    private companion object {
        const val ENDPOINT = "GET /kkm/{kkmId}/ofd/info"
        const val RESPONSE_SCHEMA = "OfdCommandResponse"
        const val STATUS_FIELD = "status"

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
