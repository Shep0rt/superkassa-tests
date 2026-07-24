package kz.superkassa.tests.api.management.ofd.token

import io.qameta.allure.Feature
import io.qameta.allure.Owner
import io.qameta.allure.Severity
import io.qameta.allure.SeverityLevel
import io.qameta.allure.Story
import io.restassured.http.ContentType
import io.restassured.http.Method
import kz.superkassa.tests.framework.assertions.ApiContractErrorMessages
import kz.superkassa.tests.framework.reporting.reportStep
import kz.superkassa.tests.framework.tags.ApiRegression
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.SoftAssertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.ResourceAccessMode
import org.junit.jupiter.api.parallel.ResourceLock
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

@ApiRegression
@Feature("API")
@Story("PUT /kkm/{kkmId}/ofd/token")
@Owner("Pavel Michka")
@DisplayName("PUT /kkm/{kkmId}/ofd/token: регрессионные проверки обновления токена ОФД")
@ResourceLock(value = "kkm-state", mode = ResourceAccessMode.READ_WRITE)
@Suppress("NonAsciiCharacters")
class KkmOfdTokenRegressionTest : KkmOfdTokenTestBase() {
    @Nested
    @ApiRegression
    @Feature("API")
    @Story("PUT /kkm/{kkmId}/ofd/token")
    @Owner("Pavel Michka")
    @DisplayName("Позитивные проверки PUT /kkm/{kkmId}/ofd/token")
    inner class PositiveRegressionTests {
        @BeforeEach
        fun `Готовим ККМ к обновлению токена ОФД`() {
            prepareKkmForTokenUpdate()
        }

        @Test
        @Severity(SeverityLevel.CRITICAL)
        @DisplayName("Метод PUT /kkm/{kkmId}/ofd/token возвращает поле ok типа Boolean")
        fun shouldReturnOkFieldWithExpectedType() {
            val response = updateOfdToken()
            val actualValue = response[OK_FIELD]

            SoftAssertions().apply {
                assertThat(response)
                    .withFailMessage(
                        ApiContractErrorMessages.documentedFieldMissing(
                            ENDPOINT,
                            OK_FIELD,
                            EXPECTED_RESPONSE,
                        ),
                    )
                    .containsKey(OK_FIELD)

                assertThat(actualValue)
                    .withFailMessage(
                        ApiContractErrorMessages.documentedFieldTypeMismatch(
                            ENDPOINT,
                            OK_FIELD,
                            Boolean::class.javaObjectType.simpleName,
                            actualValue,
                            EXPECTED_RESPONSE,
                        ),
                    )
                    .isInstanceOf(Boolean::class.javaObjectType)
            }.assertAll()
        }

        @Test
        @Severity(SeverityLevel.CRITICAL)
        @DisplayName("Метод PUT /kkm/{kkmId}/ofd/token возвращает ok=true")
        fun shouldReturnExpectedOkValue() {
            val response = updateOfdToken()
            val actualValue = response[OK_FIELD]

            assertThat(actualValue)
                .withFailMessage(
                    ApiContractErrorMessages.documentedFieldValueMismatch(
                        ENDPOINT,
                        OK_FIELD,
                        true,
                        actualValue,
                        EXPECTED_RESPONSE,
                    ),
                )
                .isEqualTo(true)
        }

        @Test
        @Severity(SeverityLevel.CRITICAL)
        @DisplayName("Метод PUT /kkm/{kkmId}/ofd/token не возвращает поля вне ожидаемого контракта")
        fun shouldNotReturnFieldsOutsideExpectedContract() {
            val response = updateOfdToken()
            val unexpectedFields = response.keys - RESPONSE_FIELDS

            assertThat(unexpectedFields)
                .withFailMessage(
                    ApiContractErrorMessages.unexpectedDocumentedFields(
                        ENDPOINT,
                        EXPECTED_RESPONSE,
                        unexpectedFields,
                    ),
                )
                .isEmpty()
        }
    }

    @Nested
    @ApiRegression
    @Feature("API")
    @Story("PUT /kkm/{kkmId}/ofd/token")
    @Owner("Pavel Michka")
    @DisplayName("Негативные проверки PUT /kkm/{kkmId}/ofd/token")
    inner class NegativeRegressionTests {
        @Nested
        @ApiRegression
        @Feature("API")
        @Story("PUT /kkm/{kkmId}/ofd/token")
        @Owner("Pavel Michka")
        @DisplayName("Проверки авторизации PUT /kkm/{kkmId}/ofd/token")
        inner class AuthorizationRegressionTests {
            @BeforeEach
            fun `Готовим ККМ к проверке авторизации`() {
                prepareKkmForTokenUpdate()
            }

            @Test
            @Severity(SeverityLevel.NORMAL)
            @DisplayName("Метод PUT /kkm/{kkmId}/ofd/token возвращает 401 без Authorization")
            fun shouldReturnUnauthorizedWithoutAuthorization() {
                reportStep("Проверяем PUT ${tokenPath(preparedKkm.kkmId)} без Authorization") {
                    superkassa.requestWithoutAuthorization()
                        .body(validTokenBody())
                        .`when`()
                        .put(tokenPath(preparedKkm.kkmId))
                        .then()
                        .shouldHaveStatus(401, "запрос обновления токена ОФД без Authorization")
                        .contentType(ContentType.JSON)
                }
            }

            @Test
            @Severity(SeverityLevel.NORMAL)
            @DisplayName("Метод PUT /kkm/{kkmId}/ofd/token возвращает 403 для неверного PIN")
            fun shouldReturnForbiddenForInvalidPin() {
                reportStep("Проверяем PUT ${tokenPath(preparedKkm.kkmId)} с неверным PIN") {
                    superkassa.request(INVALID_PIN)
                        .body(validTokenBody())
                        .`when`()
                        .put(tokenPath(preparedKkm.kkmId))
                        .then()
                        .shouldHaveStatus(403, "запрос обновления токена ОФД с неверным PIN")
                        .contentType(ContentType.JSON)
                }
            }
        }

        @Nested
        @ApiRegression
        @Feature("API")
        @Story("PUT /kkm/{kkmId}/ofd/token")
        @Owner("Pavel Michka")
        @DisplayName("Проверки невалидного тела запроса")
        inner class InvalidRequestBodyTests {
            @BeforeEach
            fun `Готовим ККМ и восстановление токена`() {
                prepareKkmForTokenUpdate()
                registerTokenRestoration()
            }

            @Test
            @Severity(SeverityLevel.NORMAL)
            @DisplayName("Метод PUT /kkm/{kkmId}/ofd/token возвращает 400 без тела запроса")
            fun shouldReturnBadRequestWithoutRequestBody() {
                reportStep("Отправляем PUT ${tokenPath(preparedKkm.kkmId)} без тела запроса") {
                    superkassa.request(preparedKkm.adminPin)
                        .`when`()
                        .put(tokenPath(preparedKkm.kkmId))
                        .then()
                        .shouldHaveStatus(400, "запрос обновления токена ОФД без тела запроса")
                        .contentType(ContentType.JSON)
                }
            }

            @Test
            @Severity(SeverityLevel.NORMAL)
            @DisplayName("Метод PUT /kkm/{kkmId}/ofd/token возвращает 400 без обязательного поля token")
            fun shouldReturnBadRequestWithoutRequiredToken() {
                putInvalidTokenBody(
                    body = emptyMap<String, Any?>(),
                    scenario = "обязательное поле token отсутствует",
                )
            }

            @Test
            @Severity(SeverityLevel.NORMAL)
            @DisplayName("Метод PUT /kkm/{kkmId}/ofd/token возвращает 400 для token=null")
            fun shouldReturnBadRequestForNullToken() {
                putInvalidTokenBody(
                    body = mapOf(TOKEN_FIELD to null),
                    scenario = "поле token содержит null",
                )
            }

            @Test
            @Severity(SeverityLevel.NORMAL)
            @DisplayName("Метод PUT /kkm/{kkmId}/ofd/token возвращает 400 для пустого token")
            fun shouldReturnBadRequestForEmptyToken() {
                putInvalidTokenBody(
                    body = mapOf(TOKEN_FIELD to ""),
                    scenario = "поле token содержит пустую строку",
                )
            }

            @ParameterizedTest(name = "{0}")
            @MethodSource("kz.superkassa.tests.api.management.ofd.token.KkmOfdTokenRegressionTest#invalidTokenStrings")
            @Severity(SeverityLevel.NORMAL)
            @DisplayName("Метод PUT /kkm/{kkmId}/ofd/token возвращает 400 для недопустимого значения token")
            fun shouldReturnBadRequestForInvalidTokenValue(caseName: String, invalidToken: String) {
                putInvalidTokenBody(
                    body = mapOf(TOKEN_FIELD to invalidToken),
                    scenario = caseName,
                )
            }

            @ParameterizedTest(name = "{0}")
            @MethodSource("kz.superkassa.tests.api.management.ofd.token.KkmOfdTokenRegressionTest#invalidTokenTypes")
            @Severity(SeverityLevel.NORMAL)
            @DisplayName("Метод PUT /kkm/{kkmId}/ofd/token возвращает 400 для token неправильного типа")
            fun shouldReturnBadRequestForInvalidTokenType(caseName: String, invalidToken: Any) {
                putInvalidTokenBody(
                    body = mapOf(TOKEN_FIELD to invalidToken),
                    scenario = caseName,
                )
            }
        }

        @Nested
        @ApiRegression
        @Feature("API")
        @Story("PUT /kkm/{kkmId}/ofd/token")
        @Owner("Pavel Michka")
        @DisplayName("Проверки несуществующих идентификаторов")
        inner class MissingIdentifiersTests {
            @Test
            @Severity(SeverityLevel.NORMAL)
            @DisplayName("Метод PUT /kkm/{kkmId}/ofd/token возвращает 404 для несуществующей ККМ")
            fun shouldReturnNotFoundForUnknownKkmId() {
                reportStep("Проверяем PUT ${tokenPath(UNKNOWN_KKM_ID)} для несуществующей ККМ") {
                    superkassa.request(preparedKkm.adminPin)
                        .body(mapOf(TOKEN_FIELD to TOKEN_FOR_UNKNOWN_KKM))
                        .`when`()
                        .put(tokenPath(UNKNOWN_KKM_ID))
                        .then()
                        .shouldHaveStatus(404, "обновление токена ОФД для несуществующей ККМ")
                        .contentType(ContentType.JSON)
                }
            }
        }

        @Nested
        @ApiRegression
        @Feature("API")
        @Story("PUT /kkm/{kkmId}/ofd/token")
        @Owner("Pavel Michka")
        @DisplayName("Проверки неподдерживаемых HTTP-методов")
        inner class UnsupportedHttpMethodsTests {
            @ParameterizedTest(name = "HTTP {0} /kkm/'{'kkmId'}'/ofd/token возвращает 405")
            @EnumSource(value = Method::class, names = ["GET", "POST", "PATCH", "DELETE"])
            @Severity(SeverityLevel.NORMAL)
            @DisplayName("Метод /kkm/{kkmId}/ofd/token возвращает 405 для HTTP-методов кроме PUT")
            fun shouldReturnMethodNotAllowedForNonPutMethods(method: Method) {
                reportStep("Проверяем, что HTTP $method ${tokenPath(preparedKkm.kkmId)} не поддерживается") {
                    superkassa.request(preparedKkm.adminPin)
                        .`when`()
                        .request(method, tokenPath(preparedKkm.kkmId))
                        .then()
                        .shouldHaveStatus(405, "неподдерживаемый HTTP-метод")
                }
            }
        }
    }

    private fun validTokenBody(): Map<String, String> = mapOf(TOKEN_FIELD to currentOfdToken)

    private fun putInvalidTokenBody(body: Any, scenario: String) {
        reportStep("Отправляем невалидное тело PUT ${tokenPath(preparedKkm.kkmId)}: $scenario") {
            superkassa.request(preparedKkm.adminPin)
                .body(body)
                .`when`()
                .put(tokenPath(preparedKkm.kkmId))
                .then()
                .shouldHaveStatus(400, scenario)
                .contentType(ContentType.JSON)
        }
    }

    private companion object {
        const val INVALID_PIN = "999999"
        const val UNKNOWN_KKM_ID = "00000000-0000-0000-0000-000000000000"
        const val TOKEN_FOR_UNKNOWN_KKM = "not-used-for-unknown-kkm"

        val RESPONSE_FIELDS = setOf(OK_FIELD)

        @JvmStatic
        fun invalidTokenTypes(): Stream<Arguments> = Stream.of(
            Arguments.of("поле token имеет тип Number вместо String", 12345),
            Arguments.of("поле token имеет тип Boolean вместо String", true),
            Arguments.of("поле token имеет тип Object вместо String", mapOf("value" to "12345")),
            Arguments.of("поле token имеет тип Array вместо String", listOf("12345")),
        )

        @JvmStatic
        fun invalidTokenStrings(): Stream<Arguments> = Stream.of(
            Arguments.of("поле token состоит только из пробелов", "    "),
            Arguments.of("поле token содержит пробел в начале", " 12345"),
            Arguments.of("поле token содержит пробел в конце", "12345 "),
            Arguments.of("поле token содержит пробел внутри", "12 345"),
            Arguments.of("поле token содержит латинскую букву", "12A345"),
            Arguments.of("поле token содержит кириллическую букву", "12А345"),
            Arguments.of("поле token содержит спецсимвол в начале", "@12345"),
            Arguments.of("поле token содержит спецсимвол внутри", "12-345"),
            Arguments.of("поле token содержит спецсимвол в конце", "12345#"),
            Arguments.of("поле token состоит только из спецсимволов", "!@#$%^&*"),
            Arguments.of("поле token содержит табуляцию", "12\t345"),
            Arguments.of("поле token содержит перенос строки", "12\n345"),
            Arguments.of("поле token содержит возврат каретки", "12\r345"),
            Arguments.of("поле token содержит неразрывный пробел", "12\u00A0345"),
            Arguments.of("поле token содержит больше 16 символов", "12345678901234567"),
        )
    }
}
