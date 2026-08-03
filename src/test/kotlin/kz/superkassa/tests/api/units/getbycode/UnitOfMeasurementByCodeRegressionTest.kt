package kz.superkassa.tests.api.units.getbycode

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
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

@ApiRegression
@Feature("API")
@Story("GET /units-of-measurement/{code}")
@Owner("Pavel Michka")
@DisplayName("GET /units-of-measurement/{code}: регрессионные проверки единицы измерения по коду")
class UnitOfMeasurementByCodeRegressionTest : BaseApiTest() {
    @Nested
    @DisplayName("Позитивные проверки GET /units-of-measurement/{code}")
    inner class PositiveRegressionTests {
        @Test
        @Severity(SeverityLevel.CRITICAL)
        @DisplayName("Метод GET /units-of-measurement/{code} возвращает поля ожидаемых типов")
        fun shouldReturnExpectedFieldTypes() {
            val response = getUnit()

            SoftAssertions().apply {
                RESPONSE_FIELDS.forEach { fieldName ->
                    assertRequiredFieldType(this, response, fieldName, String::class.java)
                }
            }.assertAll()
        }

        @Test
        @Severity(SeverityLevel.CRITICAL)
        @DisplayName("Метод GET /units-of-measurement/{code} не возвращает поля вне Swagger-контракта")
        fun shouldNotReturnFieldsOutsideSwaggerContract() {
            val response = getUnit()
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

        @Nested
        @DisplayName("Параметр code")
        inner class CodeParameterTests {
            @ParameterizedTest(name = "{0}")
            @MethodSource(
                "kz.superkassa.tests.api.units.getbycode.UnitOfMeasurementByCodeRegressionTest#existingValidCodes",
            )
            @Severity(SeverityLevel.NORMAL)
            @DisplayName("Метод GET /units-of-measurement/{code} принимает существующие цифровые коды")
            fun shouldAcceptExistingValidCode(caseName: String, code: String) {
                val response = getUnit(code)
                val expectedCode = code.trim()

                SoftAssertions().apply {
                    assertThat(response["code"])
                        .withFailMessage(
                            ApiContractErrorMessages.normalizedParameterMismatch(
                                ENDPOINT,
                                "code",
                                code,
                                expectedCode,
                                "code",
                                response["code"],
                                caseName,
                            ),
                        )
                        .isEqualTo(expectedCode)
                }.assertAll()
            }
        }
    }

    @Nested
    @DisplayName("Негативные проверки GET /units-of-measurement/{code}")
    inner class NegativeRegressionTests {
        @Nested
        @DisplayName("Параметр code")
        inner class CodeParameterTests {
            @ParameterizedTest(name = "{0}")
            @MethodSource(
                "kz.superkassa.tests.api.units.getbycode.UnitOfMeasurementByCodeRegressionTest#unknownValidCodes",
            )
            @Severity(SeverityLevel.NORMAL)
            @DisplayName("Метод GET /units-of-measurement/{code} возвращает 404 для неизвестного кода допустимого формата")
            fun shouldReturnNotFoundForUnknownValidCode(caseName: String, code: String) {
                requestUnknownValidCode(code, caseName)
            }

            @ParameterizedTest(name = "{0}")
            @MethodSource(
                "kz.superkassa.tests.api.units.getbycode.UnitOfMeasurementByCodeRegressionTest#invalidCodes",
            )
            @Severity(SeverityLevel.NORMAL)
            @DisplayName("Метод GET /units-of-measurement/{code} возвращает 400 для невалидного code")
            fun shouldReturnBadRequestForInvalidCode(caseName: String, code: String) {
                rejectInvalidCode(code, caseName)
            }
        }

        @Nested
        @DisplayName("Проверки неподдерживаемых HTTP-методов")
        inner class UnsupportedHttpMethodsTests {
            @ParameterizedTest(name = "HTTP {0} /units-of-measurement/'{'code'}' возвращает 405")
            @EnumSource(value = Method::class, names = ["POST", "PUT", "PATCH", "DELETE"])
            @Severity(SeverityLevel.NORMAL)
            @DisplayName("Метод /units-of-measurement/{code} возвращает 405 для HTTP-методов кроме GET")
            fun shouldReturnMethodNotAllowedForUnsupportedMethods(method: Method) {
                reportStep("Проверяем, что HTTP $method $BASE_PATH/$TEST_CODE не поддерживается") {
                    superkassa.requestWithoutAuthorization()
                        .`when`()
                        .request(method, PATH_TEMPLATE, TEST_CODE)
                        .then()
                        .shouldHaveStatus(405, "неподдерживаемый HTTP-метод")
                }
            }
        }
    }

    private fun getUnit(code: String = TEST_CODE): Map<String, Any?> =
        reportStep("Получаем единицу измерения с code='$code' через GET $BASE_PATH/$code") {
            superkassa.requestWithoutAuthorization()
                .`when`()
                .get(PATH_TEMPLATE, code)
                .then()
                .shouldHaveStatus(200, "получение единицы измерения по коду '$code'")
                .contentType(ContentType.JSON)
                .extract()
                .jsonPath()
                .getMap("")
        }

    private fun requestUnknownValidCode(code: String, scenario: String) {
        reportStep("Проверяем неизвестный code допустимого формата: $scenario") {
            superkassa.requestWithoutAuthorization()
                .`when`()
                .get(PATH_TEMPLATE, code)
                .then()
                .shouldHaveStatus(404, scenario)
                .contentType(ContentType.JSON)
        }
    }

    private fun rejectInvalidCode(code: String, scenario: String) {
        reportStep("Отправляем невалидный GET $ENDPOINT: $scenario") {
            superkassa.requestWithoutAuthorization()
                .`when`()
                .get(PATH_TEMPLATE, code)
                .then()
                .shouldHaveStatus(400, scenario)
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
                    RESPONSE_SCHEMA,
                ),
            )
            .isInstanceOf(expectedType)
    }

    private companion object {
        const val ENDPOINT = "GET /units-of-measurement/{code}"
        const val BASE_PATH = "/units-of-measurement"
        const val PATH_TEMPLATE = "$BASE_PATH/{code}"
        const val RESPONSE_SCHEMA = "UnitOfMeasurementResponse"
        const val TEST_CODE = "796"

        val RESPONSE_FIELDS = setOf(
            "code",
            "nameFull",
            "nameShort",
        )

        @JvmStatic
        fun existingValidCodes(): Stream<Arguments> = Stream.of(
            Arguments.of("Трехзначный code без ведущего нуля", "796"),
            Arguments.of("Трехзначный code с ведущим нулем", "006"),
            Arguments.of("Четырехзначный code", "5114"),
            Arguments.of("Существующий code содержит пробел в начале", " 796"),
            Arguments.of("Существующий code содержит пробел в конце", "796 "),
            Arguments.of("Существующий code содержит пробелы по краям", " 796 "),
        )

        @JvmStatic
        fun unknownValidCodes(): Stream<Arguments> = Stream.of(
            Arguments.of("Минимальная длина code: 1 цифра", "9"),
            Arguments.of("Code содержит 2 цифры", "99"),
            Arguments.of("Неизвестный code содержит 3 цифры", "999"),
            Arguments.of("Неизвестный code содержит 4 цифры", "9999"),
            Arguments.of("Максимальная длина code: 5 цифр", "99999"),
        )

        @JvmStatic
        fun invalidCodes(): Stream<Arguments> = Stream.of(
            Arguments.of("code содержит 6 цифр и превышает максимальную длину", "999999"),
            Arguments.of("code содержит латинские буквы", "abc"),
            Arguments.of("code содержит кириллические буквы", "код"),
            Arguments.of("code содержит цифры и буквы", "79a6"),
            Arguments.of("code содержит пробел внутри", "7 96"),
            Arguments.of("code состоит только из пробелов", "   "),
            Arguments.of("code содержит знак плюс", "+796"),
            Arguments.of("code содержит знак минус", "-796"),
            Arguments.of("code содержит разделитель дробной части", "79.6"),
            Arguments.of("code содержит символ подчеркивания", "7_96"),
            Arguments.of("code содержит дефис", "7-96"),
            Arguments.of("code содержит специальный символ", "79@6"),
            Arguments.of("code содержит полноширинные Unicode-цифры", "７９６"),
            Arguments.of("code содержит арабские Unicode-цифры", "٧٩٦"),
            Arguments.of("code содержит перенос строки", "79\n6"),
            Arguments.of("code содержит возврат каретки", "79\r6"),
            Arguments.of("code содержит табуляцию", "79\t6"),
            Arguments.of("code содержит неразрывный пробел", "79\u00A06"),
            Arguments.of("code содержит нулевой символ", "79\u00006"),
            Arguments.of("code содержит управляющий символ", "79\u00076"),
            Arguments.of("code содержит строковое значение null", "null"),
        )
    }
}
