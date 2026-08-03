package kz.superkassa.tests.api.units.get

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
import org.assertj.core.api.Assertions.assertThat
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
@Story("GET /units-of-measurement")
@Owner("Pavel Michka")
@DisplayName("GET /units-of-measurement: регрессионные проверки списка единиц измерения")
class UnitsOfMeasurementRegressionTest : BaseApiTest() {
    @Nested
    @DisplayName("Позитивные проверки GET /units-of-measurement")
    inner class PositiveRegressionTests {
        @Test
        @Severity(SeverityLevel.CRITICAL)
        @DisplayName("Метод GET /units-of-measurement возвращает поля ожидаемых типов")
        fun shouldReturnExpectedFieldTypes() {
            val response = getUnits()

            SoftAssertions().apply {
                assertRequiredFieldType(this, response, "items", List::class.java)
                assertRequiredIntegerFieldType(this, response, "total")
                assertRequiredIntegerFieldType(this, response, "limit")
                assertRequiredIntegerFieldType(this, response, "offset")
                assertRequiredFieldType(
                    this,
                    response,
                    "hasMore",
                    Boolean::class.javaObjectType,
                )

                (response["items"] as? List<*>)?.forEachIndexed { index, item ->
                    assertThat(item)
                        .withFailMessage(
                            ApiContractErrorMessages.arrayItemTypeMismatch(
                                ENDPOINT,
                                "items",
                                index,
                                "Object",
                                PAGINATED_RESPONSE_SCHEMA,
                            ),
                        )
                        .isInstanceOf(Map::class.java)

                    item.asStringMap()?.let { unit ->
                        UNIT_REQUIRED_FIELDS.forEach { fieldName ->
                            assertRequiredFieldType(
                                this,
                                unit,
                                "items[$index].$fieldName",
                                fieldName,
                                String::class.java,
                                UNIT_RESPONSE_SCHEMA,
                            )
                        }
                    }
                }
            }.assertAll()
        }

        @Test
        @Severity(SeverityLevel.CRITICAL)
        @DisplayName("Метод GET /units-of-measurement не возвращает поля вне Swagger-контракта")
        fun shouldNotReturnFieldsOutsideSwaggerContract() {
            val response = getUnits()

            SoftAssertions().apply {
                assertOnlySwaggerFields(this, response, PAGINATED_RESPONSE_SCHEMA, PAGINATED_RESPONSE_FIELDS)

                (response["items"] as? List<*>)?.forEach { item ->
                    item.asStringMap()?.let { unit ->
                        assertOnlySwaggerFields(this, unit, UNIT_RESPONSE_SCHEMA, UNIT_RESPONSE_FIELDS)
                    }
                }
            }.assertAll()
        }

        @Test
        @Severity(SeverityLevel.NORMAL)
        @DisplayName("Метод GET /units-of-measurement без параметров возвращает limit=50 и offset=0")
        fun shouldApplyDefaultPaginationValues() {
            val response = getUnits()

            SoftAssertions().apply {
                assertThat((response["limit"] as? Number)?.toInt())
                    .withFailMessage(
                        ApiContractErrorMessages.fieldValueMismatch(
                            ENDPOINT,
                            "limit",
                            DEFAULT_LIMIT,
                            response["limit"],
                            PAGINATED_RESPONSE_SCHEMA,
                        ),
                    )
                    .isEqualTo(DEFAULT_LIMIT)
                assertThat((response["offset"] as? Number)?.toInt())
                    .withFailMessage(
                        ApiContractErrorMessages.fieldValueMismatch(
                            ENDPOINT,
                            "offset",
                            DEFAULT_OFFSET,
                            response["offset"],
                            PAGINATED_RESPONSE_SCHEMA,
                        ),
                    )
                    .isEqualTo(DEFAULT_OFFSET)
            }.assertAll()
        }

        @Nested
        @DisplayName("Параметр limit")
        inner class LimitParameterTests {
            @ParameterizedTest(name = "{0}")
            @MethodSource(
                "kz.superkassa.tests.api.units.get.UnitsOfMeasurementRegressionTest#validLimitValues",
            )
            @Severity(SeverityLevel.NORMAL)
            @DisplayName("Метод GET /units-of-measurement принимает граничные значения limit")
            fun shouldAcceptValidBoundaryValue(caseName: String, limit: Int) {
                val response = getUnits(mapOf("limit" to limit))

                assertThat((response["limit"] as? Number)?.toInt())
                    .withFailMessage(
                        ApiContractErrorMessages.fieldValueMismatch(
                            ENDPOINT,
                            "limit",
                            limit,
                            response["limit"],
                            PAGINATED_RESPONSE_SCHEMA,
                            caseName,
                        ),
                    )
                    .isEqualTo(limit)
            }
        }

        @Nested
        @DisplayName("Параметр offset")
        inner class OffsetParameterTests {
            @ParameterizedTest(name = "{0}")
            @MethodSource(
                "kz.superkassa.tests.api.units.get.UnitsOfMeasurementRegressionTest#validOffsetValues",
            )
            @Severity(SeverityLevel.NORMAL)
            @DisplayName("Метод GET /units-of-measurement принимает допустимые значения offset")
            fun shouldAcceptValidValue(caseName: String, offset: Int) {
                val response = getUnits(mapOf("offset" to offset))

                assertThat((response["offset"] as? Number)?.toInt())
                    .withFailMessage(
                        ApiContractErrorMessages.fieldValueMismatch(
                            ENDPOINT,
                            "offset",
                            offset,
                            response["offset"],
                            PAGINATED_RESPONSE_SCHEMA,
                            caseName,
                        ),
                    )
                    .isEqualTo(offset)
            }
        }

        @Nested
        @DisplayName("Параметр search")
        inner class SearchParameterTests {
            @Test
            @Severity(SeverityLevel.NORMAL)
            @DisplayName("Метод GET /units-of-measurement возвращает 200 без необязательного параметра search")
            fun shouldAcceptMissingParameter() {
                getUnits()
            }

            @ParameterizedTest(name = "{0}")
            @MethodSource(
                "kz.superkassa.tests.api.units.get.UnitsOfMeasurementRegressionTest#validSearchEquivalenceClasses",
            )
            @Severity(SeverityLevel.NORMAL)
            @DisplayName("Метод GET /units-of-measurement принимает допустимые классы эквивалентности search")
            fun shouldAcceptValidEquivalenceClass(caseName: String, search: String) {
                getUnits(mapOf("search" to search), caseName)
            }

            @ParameterizedTest(name = "{0}")
            @MethodSource(
                "kz.superkassa.tests.api.units.get.UnitsOfMeasurementRegressionTest#validSearchBoundaryValues",
            )
            @Severity(SeverityLevel.NORMAL)
            @DisplayName("Метод GET /units-of-measurement принимает граничные значения длины search")
            fun shouldAcceptValidBoundaryValue(caseName: String, search: String) {
                getUnits(mapOf("search" to search), caseName)
            }
        }
    }

    @Nested
    @DisplayName("Негативные проверки GET /units-of-measurement")
    inner class NegativeRegressionTests {
        @Nested
        @DisplayName("Проверки невалидных query-параметров")
        inner class InvalidQueryParametersTests {
            @Nested
            @DisplayName("Параметр limit")
            inner class LimitParameterTests {
                @ParameterizedTest(name = "{0}")
                @MethodSource(
                    "kz.superkassa.tests.api.units.get.UnitsOfMeasurementRegressionTest#invalidLimitValues",
                )
                @Severity(SeverityLevel.NORMAL)
                @DisplayName("Метод GET /units-of-measurement возвращает 400 для невалидного limit")
                fun shouldReturnBadRequestForInvalidValue(caseName: String, limit: String) {
                    rejectInvalidQueryParameter("limit", limit, caseName)
                }
            }

            @Nested
            @DisplayName("Параметр offset")
            inner class OffsetParameterTests {
                @ParameterizedTest(name = "{0}")
                @MethodSource(
                    "kz.superkassa.tests.api.units.get.UnitsOfMeasurementRegressionTest#invalidOffsetValues",
                )
                @Severity(SeverityLevel.NORMAL)
                @DisplayName("Метод GET /units-of-measurement возвращает 400 для невалидного offset")
                fun shouldReturnBadRequestForInvalidValue(caseName: String, offset: String) {
                    rejectInvalidQueryParameter("offset", offset, caseName)
                }
            }

            @Nested
            @DisplayName("Параметр search")
            inner class SearchParameterTests {
                @ParameterizedTest(name = "{0}")
                @MethodSource(
                    "kz.superkassa.tests.api.units.get.UnitsOfMeasurementRegressionTest#invalidSearchValues",
                )
                @Severity(SeverityLevel.NORMAL)
                @DisplayName("Метод GET /units-of-measurement возвращает 400 для невалидного search")
                fun shouldReturnBadRequestForInvalidValue(caseName: String, search: String) {
                    rejectInvalidQueryParameter("search", search, caseName)
                }

                @Test
                @Severity(SeverityLevel.NORMAL)
                @DisplayName("Метод GET /units-of-measurement возвращает 400 при повторной передаче search")
                fun shouldReturnBadRequestForRepeatedParameter() {
                    val scenario = "параметр search передан несколько раз"

                    reportStep("Отправляем невалидный GET $PATH: $scenario") {
                        superkassa.requestWithoutAuthorization()
                            .queryParam("search", "килограмм", "литр")
                            .`when`()
                            .get(PATH)
                            .then()
                            .shouldHaveStatus(400, scenario)
                            .contentType(ContentType.JSON)
                    }
                }
            }
        }

        @Nested
        @DisplayName("Проверки неподдерживаемых HTTP-методов")
        inner class UnsupportedHttpMethodsTests {
            @ParameterizedTest(name = "HTTP {0} /units-of-measurement возвращает 405")
            @EnumSource(value = Method::class, names = ["POST", "PUT", "PATCH", "DELETE"])
            @Severity(SeverityLevel.NORMAL)
            @DisplayName("Метод /units-of-measurement возвращает 405 для HTTP-методов кроме GET")
            fun shouldReturnMethodNotAllowedForNonGetMethods(method: Method) {
                reportStep("Проверяем, что HTTP $method $PATH не поддерживается") {
                    superkassa.requestWithoutAuthorization()
                        .`when`()
                        .request(method, PATH)
                        .then()
                        .shouldHaveStatus(405, "неподдерживаемый HTTP-метод")
                }
            }
        }
    }

    private fun getUnits(
        queryParameters: Map<String, Any> = emptyMap(),
        scenario: String? = null,
    ): Map<String, Any?> =
        reportStep(unitsRequestStep(queryParameters, scenario)) {
            val request = superkassa.requestWithoutAuthorization()
            queryParameters.forEach { (name, value) -> request.queryParam(name, value) }

            request
                .`when`()
                .get(PATH)
                .then()
                .shouldHaveStatus(200, "получение списка единиц измерения")
                .contentType(ContentType.JSON)
                .extract()
                .jsonPath()
                .getMap("")
        }

    private fun rejectInvalidQueryParameter(parameterName: String, value: String, scenario: String) {
        reportStep("Отправляем невалидный GET $PATH: $scenario") {
            superkassa.requestWithoutAuthorization()
                .queryParam(parameterName, value)
                .`when`()
                .get(PATH)
                .then()
                .shouldHaveStatus(400, scenario)
                .contentType(ContentType.JSON)
        }
    }

    private fun unitsRequestStep(queryParameters: Map<String, Any>, scenario: String?): String {
        if (queryParameters.isEmpty()) {
            return "Получаем список единиц измерения через GET $PATH"
        }

        val parameters = queryParameters.entries.joinToString { (name, value) -> "$name='$value'" }
        val scenarioSuffix = scenario?.let { ": $it" }.orEmpty()
        return "Получаем список единиц измерения через GET $PATH с параметрами $parameters$scenarioSuffix"
    }

    private fun assertRequiredFieldType(
        softly: SoftAssertions,
        response: Map<String, Any?>,
        fieldName: String,
        expectedType: Class<*>,
    ) {
        assertRequiredFieldType(
            softly,
            response,
            fieldName,
            fieldName,
            expectedType,
            PAGINATED_RESPONSE_SCHEMA,
        )
    }

    private fun assertRequiredFieldType(
        softly: SoftAssertions,
        response: Map<String, Any?>,
        responseFieldName: String,
        sourceFieldName: String,
        expectedType: Class<*>,
        schemaName: String,
    ) {
        softly.assertThat(response)
            .withFailMessage(
                ApiContractErrorMessages.requiredFieldWithTypeMissing(
                    ENDPOINT,
                    responseFieldName,
                    expectedType.simpleName,
                    schemaName,
                ),
            )
            .containsKey(sourceFieldName)

        softly.assertThat(response[sourceFieldName])
            .withFailMessage(
                ApiContractErrorMessages.fieldTypeMismatch(
                    ENDPOINT,
                    responseFieldName,
                    expectedType.simpleName,
                    schemaName,
                ),
            )
            .isInstanceOf(expectedType)
    }

    private fun assertRequiredIntegerFieldType(
        softly: SoftAssertions,
        response: Map<String, Any?>,
        fieldName: String,
    ) {
        softly.assertThat(response)
            .withFailMessage(
                ApiContractErrorMessages.requiredFieldWithTypeMissing(
                    ENDPOINT,
                    fieldName,
                    INTEGER_TYPE,
                    PAGINATED_RESPONSE_SCHEMA,
                ),
            )
            .containsKey(fieldName)

        softly.assertThat(response[fieldName])
            .withFailMessage(
                ApiContractErrorMessages.fieldTypeMismatch(
                    ENDPOINT,
                    fieldName,
                    INTEGER_TYPE,
                    PAGINATED_RESPONSE_SCHEMA,
                ),
            )
            .isInstanceOfAny(Int::class.javaObjectType, Long::class.javaObjectType)
    }

    private fun assertOnlySwaggerFields(
        softly: SoftAssertions,
        response: Map<String, Any?>,
        schemaName: String,
        allowedFields: Set<String>,
    ) {
        val unexpectedFields = response.keys - allowedFields

        softly.assertThat(unexpectedFields)
            .withFailMessage(
                ApiContractErrorMessages.unexpectedSwaggerFields(
                    ENDPOINT,
                    schemaName,
                    unexpectedFields,
                ),
            )
            .isEmpty()
    }

    @Suppress("UNCHECKED_CAST")
    private fun Any?.asStringMap(): Map<String, Any?>? = this as? Map<String, Any?>

    private companion object {
        const val ENDPOINT = "GET /units-of-measurement"
        const val PATH = "/units-of-measurement"
        const val PAGINATED_RESPONSE_SCHEMA = "PaginatedResponseUnitOfMeasurementResponse"
        const val UNIT_RESPONSE_SCHEMA = "UnitOfMeasurementResponse"
        const val INTEGER_TYPE = "Integer"
        const val DEFAULT_LIMIT = 50
        const val DEFAULT_OFFSET = 0
        const val SEARCH_MAX_LENGTH = 100

        val PAGINATED_RESPONSE_FIELDS = setOf(
            "items",
            "total",
            "limit",
            "offset",
            "hasMore",
        )

        val UNIT_REQUIRED_FIELDS = setOf(
            "code",
            "nameFull",
            "nameShort",
        )

        val UNIT_RESPONSE_FIELDS = UNIT_REQUIRED_FIELDS

        @JvmStatic
        fun validLimitValues(): Stream<Arguments> = Stream.of(
            Arguments.of("Минимальное допустимое значение limit=1", 1),
            Arguments.of("Максимальное допустимое значение limit=100", 100),
        )

        @JvmStatic
        fun validOffsetValues(): Stream<Arguments> = Stream.of(
            Arguments.of("Минимальное допустимое значение offset=0", 0),
            Arguments.of("Положительное значение offset=1", 1),
            Arguments.of("Максимальное значение offset для int32", Int.MAX_VALUE),
        )

        @JvmStatic
        fun validSearchEquivalenceClasses(): Stream<Arguments> = Stream.of(
            Arguments.of("Параметр search содержит пустую строку", ""),
            Arguments.of("Параметр search содержит пробельную строку", "   "),
            Arguments.of("Параметр search содержит название единицы измерения", "килограмм"),
            Arguments.of("Параметр search содержит часть названия единицы измерения", "кило"),
            Arguments.of("Параметр search содержит код единицы измерения", "KGM"),
            Arguments.of("Параметр search содержит цифры", "100"),
            Arguments.of("Параметр search содержит кириллическую Unicode-строку", "штука"),
            Arguments.of("Параметр search содержит казахские Unicode-символы", "өлшем бірлігі"),
            Arguments.of("Параметр search содержит внутренние пробелы", "килограмм в упаковке"),
            Arguments.of("Параметр search содержит внешние пробелы", "  килограмм  "),
            Arguments.of("Параметр search содержит обозначения единиц и пунктуацию", "м²/с, 20° и 50%"),
        )

        @JvmStatic
        fun validSearchBoundaryValues(): Stream<Arguments> = Stream.of(
            Arguments.of("search содержит 1 Unicode-символ", "а"),
            Arguments.of("search содержит 99 Unicode-символов", "а".repeat(SEARCH_MAX_LENGTH - 1)),
            Arguments.of("search содержит 100 Unicode-символов", "а".repeat(SEARCH_MAX_LENGTH)),
            Arguments.of(
                "search содержит 100 Unicode-букв из расширенного набора",
                "𐐀".repeat(SEARCH_MAX_LENGTH),
            ),
            Arguments.of(
                "search содержит 100 Unicode-символов после удаления внешних пробелов",
                "  ${"а".repeat(SEARCH_MAX_LENGTH)}  ",
            ),
        )

        @JvmStatic
        fun invalidSearchValues(): Stream<Arguments> = Stream.of(
            Arguments.of("search содержит 101 Unicode-символ", "а".repeat(SEARCH_MAX_LENGTH + 1)),
            Arguments.of(
                "search содержит 101 Unicode-букву из расширенного набора",
                "𐐀".repeat(SEARCH_MAX_LENGTH + 1),
            ),
            Arguments.of("search содержит перенос строки", "кило\nграмм"),
            Arguments.of("search содержит возврат каретки", "кило\rграмм"),
            Arguments.of("search содержит табуляцию", "кило\tграмм"),
            Arguments.of("search содержит нулевой символ", "кило\u0000грамм"),
            Arguments.of("search содержит управляющий символ", "кило\u0007грамм"),
        )

        @JvmStatic
        fun invalidLimitValues(): Stream<Arguments> = Stream.of(
            Arguments.of("limit=0 ниже минимальной границы", "0"),
            Arguments.of("limit=-1 является отрицательным", "-1"),
            Arguments.of("limit=101 выше максимальной границы", "101"),
            Arguments.of("limit содержит нечисловую строку", "abc"),
            Arguments.of("limit содержит дробное число", "1.5"),
            Arguments.of("limit содержит пустую строку", ""),
            Arguments.of("limit содержит пробельную строку", " "),
        )

        @JvmStatic
        fun invalidOffsetValues(): Stream<Arguments> = Stream.of(
            Arguments.of("offset=-1 является отрицательным", "-1"),
            Arguments.of("offset содержит нечисловую строку", "abc"),
            Arguments.of("offset содержит дробное число", "1.5"),
            Arguments.of("offset содержит пустую строку", ""),
            Arguments.of("offset содержит пробельную строку", " "),
            Arguments.of("offset превышает максимальное значение int32", "2147483648"),
        )
    }
}
