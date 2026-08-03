package kz.superkassa.tests.api.kkm.diagnostics.auth

import io.qameta.allure.Feature
import io.qameta.allure.Owner
import io.qameta.allure.Severity
import io.qameta.allure.SeverityLevel
import io.qameta.allure.Story
import io.restassured.http.ContentType
import io.restassured.http.Method
import kz.superkassa.tests.framework.assertions.ApiContractErrorMessages
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
import java.util.UUID

@ApiRegression
@Feature("API")
@Story("POST /kkm/{kkmId}/ofd/auth")
@Owner("Pavel Michka")
@DisplayName("POST /kkm/{kkmId}/ofd/auth: регрессионные проверки получения данных авторизации ОФД")
@ResourceLock(value = "kkm-state", mode = ResourceAccessMode.READ_WRITE)
class KkmOfdAuthRegressionTest : KkmAuthenticatedTest() {
    @Nested
    @DisplayName("Позитивные проверки POST /kkm/{kkmId}/ofd/auth")
    inner class PositiveRegressionTests {
        @Test
        @Severity(SeverityLevel.CRITICAL)
        @DisplayName("Метод POST /kkm/{kkmId}/ofd/auth возвращает поля ожидаемых типов")
        fun shouldReturnExpectedFieldTypes() {
            val response = getOfdAuthInfo()

            SoftAssertions().apply {
                assertRequiredNextRequestNumberType(this, response)
                assertOptionalTokenType(this, response)
            }.assertAll()
        }

        @Test
        @Severity(SeverityLevel.CRITICAL)
        @DisplayName("Метод POST /kkm/{kkmId}/ofd/auth не возвращает поля вне Swagger-контракта")
        fun shouldNotReturnFieldsOutsideSwaggerContract() {
            val response = getOfdAuthInfo()
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
    @DisplayName("Негативные проверки POST /kkm/{kkmId}/ofd/auth")
    inner class NegativeRegressionTests {
        @Nested
        @DisplayName("Проверки авторизации POST /kkm/{kkmId}/ofd/auth")
        inner class AuthorizationRegressionTests {
            @Test
            @Severity(SeverityLevel.NORMAL)
            @DisplayName("Метод POST /kkm/{kkmId}/ofd/auth возвращает 401 без Authorization")
            fun shouldReturnUnauthorizedWithoutAuthorization() {
                reportStep("Проверяем POST ${ofdAuthPath(preparedKkm.kkmId)} без Authorization") {
                    superkassa.requestWithoutAuthorization()
                        .`when`()
                        .post(ofdAuthPath(preparedKkm.kkmId))
                        .then()
                        .shouldHaveStatus(401, "получение данных авторизации ОФД без Authorization")
                        .contentType(ContentType.JSON)
                }
            }

            @Test
            @Severity(SeverityLevel.NORMAL)
            @DisplayName("Метод POST /kkm/{kkmId}/ofd/auth возвращает 403 для неверного PIN")
            fun shouldReturnForbiddenForInvalidPin() {
                reportStep("Проверяем POST ${ofdAuthPath(preparedKkm.kkmId)} с неверным PIN") {
                    superkassa.request(INVALID_PIN)
                        .`when`()
                        .post(ofdAuthPath(preparedKkm.kkmId))
                        .then()
                        .shouldHaveStatus(403, "получение данных авторизации ОФД с неверным PIN")
                        .contentType(ContentType.JSON)
                }
            }
        }

        @Nested
        @DisplayName("Проверки несуществующих идентификаторов")
        inner class MissingIdentifiersTests {
            @Test
            @Severity(SeverityLevel.NORMAL)
            @DisplayName("Метод POST /kkm/{kkmId}/ofd/auth возвращает 404 для несуществующей ККМ")
            fun shouldReturnNotFoundForUnknownKkmId() {
                val unknownKkmId = UUID.randomUUID().toString()

                reportStep("Проверяем POST ${ofdAuthPath(unknownKkmId)} для несуществующей ККМ") {
                    superkassa.request(preparedKkm.adminPin)
                        .`when`()
                        .post(ofdAuthPath(unknownKkmId))
                        .then()
                        .shouldHaveStatus(404, "получение данных авторизации ОФД для несуществующей ККМ")
                        .contentType(ContentType.JSON)
                }
            }
        }

        @Nested
        @DisplayName("Проверки неподдерживаемых HTTP-методов")
        inner class UnsupportedHttpMethodsTests {
            @ParameterizedTest(name = "HTTP {0} /kkm/'{'kkmId'}'/ofd/auth возвращает 405")
            @EnumSource(value = Method::class, names = ["GET", "PUT", "PATCH", "DELETE"])
            @Severity(SeverityLevel.NORMAL)
            @DisplayName("Метод /kkm/{kkmId}/ofd/auth возвращает 405 для HTTP-методов кроме POST")
            fun shouldReturnMethodNotAllowedForNonPostMethods(method: Method) {
                reportStep("Проверяем, что HTTP $method ${ofdAuthPath(preparedKkm.kkmId)} не поддерживается") {
                    superkassa.request(preparedKkm.adminPin)
                        .`when`()
                        .request(method, ofdAuthPath(preparedKkm.kkmId))
                        .then()
                        .shouldHaveStatus(405, "неподдерживаемый HTTP-метод")
                }
            }
        }
    }

    private fun getOfdAuthInfo(): Map<String, Any?> =
        reportStep(
            "Получаем данные авторизации ОФД для ККМ kkmId='${preparedKkm.kkmId}' " +
                "через POST ${ofdAuthPath(preparedKkm.kkmId)}",
        ) {
            superkassa.request(preparedKkm.adminPin)
                .`when`()
                .post(ofdAuthPath(preparedKkm.kkmId))
                .then()
                .shouldHaveStatus(200, "получение данных авторизации ОФД")
                .contentType(ContentType.JSON)
                .extract()
                .jsonPath()
                .getMap("")
        }

    private fun assertRequiredNextRequestNumberType(
        softly: SoftAssertions,
        response: Map<String, Any?>,
    ) {
        softly.assertThat(response)
            .withFailMessage(
                ApiContractErrorMessages.requiredFieldWithTypeMissing(
                    ENDPOINT,
                    NEXT_REQUEST_NUMBER_FIELD,
                    "Integer",
                    RESPONSE_SCHEMA,
                ),
            )
            .containsKey(NEXT_REQUEST_NUMBER_FIELD)

        softly.assertThat(response[NEXT_REQUEST_NUMBER_FIELD])
            .withFailMessage(
                ApiContractErrorMessages.fieldTypeMismatch(
                    ENDPOINT,
                    NEXT_REQUEST_NUMBER_FIELD,
                    "Integer",
                    RESPONSE_SCHEMA,
                ),
            )
            .isInstanceOf(Int::class.javaObjectType)
    }

    private fun assertOptionalTokenType(
        softly: SoftAssertions,
        response: Map<String, Any?>,
    ) {
        val token = response[TOKEN_FIELD] ?: return

        softly.assertThat(token)
            .withFailMessage(
                ApiContractErrorMessages.optionalFieldTypeMismatch(
                    ENDPOINT,
                    TOKEN_FIELD,
                    String::class.java.simpleName,
                    RESPONSE_SCHEMA,
                ),
            )
            .isInstanceOf(String::class.java)
    }

    private fun ofdAuthPath(kkmId: String): String = "/kkm/$kkmId/ofd/auth"

    private companion object {
        const val ENDPOINT = "POST /kkm/{kkmId}/ofd/auth"
        const val RESPONSE_SCHEMA = "OfdAuthInfoResponse"
        const val NEXT_REQUEST_NUMBER_FIELD = "nextReqNum"
        const val TOKEN_FIELD = "token"
        const val INVALID_PIN = "999999"

        val RESPONSE_FIELDS = setOf(
            NEXT_REQUEST_NUMBER_FIELD,
            TOKEN_FIELD,
        )
    }
}
