package kz.superkassa.tests.api.management.settings.branding

import io.qameta.allure.Allure
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
import kz.superkassa.tests.framework.kkm.PreparedKkmAuth
import kz.superkassa.tests.framework.reporting.reportStep
import kz.superkassa.tests.framework.tags.ApiRegression
import org.assertj.core.api.SoftAssertions
import org.junit.jupiter.api.AfterEach
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
import java.util.UUID
import java.util.stream.Stream

@ApiRegression
@Feature("API")
@Story("PUT /kkm/{kkmId}/settings/branding")
@Owner("Pavel Michka")
@DisplayName("PUT /kkm/{kkmId}/settings/branding: регрессионные проверки настроек брендирования")
@ResourceLock(value = "kkm-state", mode = ResourceAccessMode.READ_WRITE)
@Suppress("NonAsciiCharacters")
class KkmBrandingRegressionTest : KkmAuthenticatedTest() {
    private var kkmToExitAfterTest: PreparedKkmAuth? = null

    @AfterEach
    fun `Восстанавливаем режим ККМ после проверки`() {
        val preparedKkm = kkmToExitAfterTest
        if (preparedKkm == null) {
            Allure.step("Возврат не требуется: ККМ не оставлена в режиме программирования")
            return
        }

        kkmToExitAfterTest = null
        exitProgramming(preparedKkm)
    }

    @Nested
    @ApiRegression
    @Feature("API")
    @Story("PUT /kkm/{kkmId}/settings/branding")
    @Owner("Pavel Michka")
    @DisplayName("Позитивные проверки PUT /kkm/{kkmId}/settings/branding")
    inner class PositiveRegressionTests {
        @BeforeEach
        fun `Переводим ККМ в режим программирования`() {
            enterProgramming(preparedKkm)
        }

        @Test
        @Severity(SeverityLevel.CRITICAL)
        @DisplayName("Метод PUT /kkm/{kkmId}/settings/branding возвращает поля ожидаемых типов")
        fun shouldReturnExpectedFieldTypes() {
            val response = updateBranding(validBrandingBody())

            SoftAssertions().apply {
                REQUIRED_KKM_FIELD_TYPES.forEach { field ->
                    assertRequiredFieldType(this, response, field)
                }
                OPTIONAL_KKM_FIELD_TYPES.forEach { field ->
                    assertOptionalFieldType(this, response, field)
                }

                response.objectField("ofdServiceInfo")?.let { ofdServiceInfo ->
                    OFD_SERVICE_INFO_FIELD_TYPES.forEach { field ->
                        assertRequiredFieldType(this, ofdServiceInfo, field)
                    }
                }

                response.objectField("branding")?.let { branding ->
                    BRANDING_REQUIRED_FIELD_TYPES.forEach { field ->
                        assertRequiredFieldType(this, branding, field)
                    }
                    BRANDING_OPTIONAL_FIELD_TYPES.forEach { field ->
                        assertOptionalFieldType(this, branding, field)
                    }
                    assertOfdTicketAdsItemsType(this, branding["ofdTicketAds"])
                }
            }.assertAll()
        }

        @Test
        @Severity(SeverityLevel.CRITICAL)
        @DisplayName("Метод PUT /kkm/{kkmId}/settings/branding возвращает допустимые enum-значения")
        fun shouldReturnSupportedEnumValues() {
            val response = updateBranding(validBrandingBody())

            SoftAssertions().apply {
                assertRequiredEnumValue(this, response, "mode", "mode", KKM_RESPONSE_SCHEMA, ApiEnumValues.KKM_MODES)
                assertRequiredEnumValue(this, response, "state", "state", KKM_RESPONSE_SCHEMA, ApiEnumValues.KKM_STATES)
                assertOptionalEnumValue(this, response, "defaultVatGroup", "defaultVatGroup", ApiEnumValues.VAT_GROUPS)
                assertOptionalEnumValue(
                    this,
                    response,
                    "ofdEnvironment",
                    "ofdEnvironment",
                    ApiEnumValues.OFD_ENVIRONMENTS,
                )
                assertOptionalEnumValue(this, response, "ofdId", "ofdId", ApiEnumValues.OFD_IDS)
                assertOptionalEnumValue(this, response, "taxRegime", "taxRegime", ApiEnumValues.TAX_REGIMES)

                response.objectField("branding")?.let { branding ->
                    assertRequiredEnumValue(
                        this,
                        branding,
                        "language",
                        "branding.language",
                        BRANDING_RESPONSE_SCHEMA,
                        ApiEnumValues.BRANDING_LANGUAGES,
                    )
                    assertRequiredEnumValue(
                        this,
                        branding,
                        "themeColor",
                        "branding.themeColor",
                        BRANDING_RESPONSE_SCHEMA,
                        ApiEnumValues.BRANDING_THEME_COLORS,
                    )
                }
            }.assertAll()
        }

        @Test
        @Severity(SeverityLevel.CRITICAL)
        @DisplayName("Метод PUT /kkm/{kkmId}/settings/branding не возвращает поля вне Swagger-контракта")
        fun shouldNotReturnFieldsOutsideSwaggerContract() {
            val response = updateBranding(validBrandingBody())

            SoftAssertions().apply {
                assertOnlySwaggerFields(this, response, KKM_RESPONSE_SCHEMA, KKM_RESPONSE_FIELDS)

                response.objectField("ofdServiceInfo")?.let { ofdServiceInfo ->
                    assertOnlySwaggerFields(
                        this,
                        ofdServiceInfo,
                        OFD_SERVICE_INFO_SCHEMA,
                        OFD_SERVICE_INFO_RESPONSE_FIELDS,
                    )
                }

                response.objectField("branding")?.let { branding ->
                    assertOnlySwaggerFields(this, branding, BRANDING_RESPONSE_SCHEMA, BRANDING_RESPONSE_FIELDS)
                }
            }.assertAll()
        }

        @Test
        @Severity(SeverityLevel.NORMAL)
        @DisplayName("Метод PUT /kkm/{kkmId}/settings/branding принимает все optional-поля типа String")
        fun shouldAcceptAllOptionalStringFields() {
            updateBranding(fullBrandingBody())
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource(
            "kz.superkassa.tests.api.management.settings.branding." +
                "KkmBrandingRegressionTest#validRequiredFieldValues",
        )
        @Severity(SeverityLevel.NORMAL)
        @DisplayName("Метод PUT /kkm/{kkmId}/settings/branding принимает допустимые значения required-полей")
        fun shouldAcceptValidRequiredFieldValue(caseName: String, fieldName: String, validValue: Any) {
            reportStep("Проверяем допустимое значение поля запроса: $caseName") {
                updateBranding(validBrandingBody() + (fieldName to validValue))
            }
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource(
            "kz.superkassa.tests.api.management.settings.branding." +
                "KkmBrandingRegressionTest#nullableOptionalRequestFields",
        )
        @Severity(SeverityLevel.NORMAL)
        @DisplayName("Метод PUT /kkm/{kkmId}/settings/branding принимает null в nullable optional-полях")
        fun shouldAcceptNullInOptionalField(caseName: String, fieldName: String) {
            reportStep("Проверяем nullable-поле запроса: $caseName") {
                updateBranding(validBrandingBody() + (fieldName to null))
            }
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource(
            "kz.superkassa.tests.api.management.settings.branding." +
                "KkmBrandingRegressionTest#optionalStringEquivalenceClasses",
        )
        @Severity(SeverityLevel.NORMAL)
        @DisplayName("Метод PUT /kkm/{kkmId}/settings/branding принимает допустимые классы optional-строк")
        fun shouldAcceptOptionalStringEquivalenceClass(
            caseName: String,
            fieldName: String,
            validValue: String,
        ) {
            reportStep("Проверяем допустимый класс optional-поля: $caseName") {
                updateBranding(validBrandingBody() + (fieldName to validValue))
            }
        }
    }

    @Nested
    @ApiRegression
    @Feature("API")
    @Story("PUT /kkm/{kkmId}/settings/branding")
    @Owner("Pavel Michka")
    @DisplayName("Негативные проверки PUT /kkm/{kkmId}/settings/branding")
    inner class NegativeRegressionTests {
        @Nested
        @ApiRegression
        @Feature("API")
        @Story("PUT /kkm/{kkmId}/settings/branding")
        @Owner("Pavel Michka")
        @DisplayName("Проверки авторизации PUT /kkm/{kkmId}/settings/branding")
        inner class AuthorizationRegressionTests {
            @BeforeEach
            fun `Переводим ККМ в режим программирования`() {
                enterProgramming(preparedKkm)
            }

            @Test
            @Severity(SeverityLevel.NORMAL)
            @DisplayName("Метод PUT /kkm/{kkmId}/settings/branding возвращает 401 без Authorization")
            fun shouldReturnUnauthorizedWithoutAuthorization() {
                reportStep("Проверяем PUT ${brandingPath(preparedKkm.kkmId)} без Authorization") {
                    superkassa.requestWithoutAuthorization()
                        .body(validBrandingBody())
                        .`when`()
                        .put(brandingPath(preparedKkm.kkmId))
                        .then()
                        .shouldHaveStatus(401, "запрос изменения branding без Authorization")
                        .contentType(ContentType.JSON)
                }
            }

            @Test
            @Severity(SeverityLevel.NORMAL)
            @DisplayName("Метод PUT /kkm/{kkmId}/settings/branding возвращает 403 для неверного PIN")
            fun shouldReturnForbiddenForInvalidPin() {
                reportStep("Проверяем PUT ${brandingPath(preparedKkm.kkmId)} с неверным PIN") {
                    superkassa.request(INVALID_PIN)
                        .body(validBrandingBody())
                        .`when`()
                        .put(brandingPath(preparedKkm.kkmId))
                        .then()
                        .shouldHaveStatus(403, "запрос изменения branding с неверным PIN")
                        .contentType(ContentType.JSON)
                }
            }
        }

        @Nested
        @ApiRegression
        @Feature("API")
        @Story("PUT /kkm/{kkmId}/settings/branding")
        @Owner("Pavel Michka")
        @DisplayName("Проверки невалидного тела запроса")
        inner class InvalidRequestBodyTests {
            @BeforeEach
            fun `Переводим ККМ в режим программирования`() {
                enterProgramming(preparedKkm)
            }

            @Test
            @Severity(SeverityLevel.NORMAL)
            @DisplayName("Метод PUT /kkm/{kkmId}/settings/branding возвращает 400 без тела запроса")
            fun shouldReturnBadRequestWithoutRequestBody() {
                reportStep("Отправляем PUT ${brandingPath(preparedKkm.kkmId)} без тела запроса") {
                    superkassa.request(preparedKkm.adminPin)
                        .`when`()
                        .put(brandingPath(preparedKkm.kkmId))
                        .then()
                        .shouldHaveStatus(400, "запрос изменения branding без тела запроса")
                        .contentType(ContentType.JSON)
                }
            }

            @Test
            @Severity(SeverityLevel.NORMAL)
            @DisplayName("Метод PUT /kkm/{kkmId}/settings/branding возвращает 400 для пустого объекта")
            fun shouldReturnBadRequestForEmptyObject() {
                putInvalidBody(emptyMap<String, Any?>(), "тело запроса не содержит обязательных полей")
            }

            @ParameterizedTest(name = "{0}")
            @MethodSource(
                "kz.superkassa.tests.api.management.settings.branding." +
                    "KkmBrandingRegressionTest#requiredRequestFields",
            )
            @Severity(SeverityLevel.NORMAL)
            @DisplayName("Метод PUT /kkm/{kkmId}/settings/branding возвращает 400 без обязательного поля")
            fun shouldReturnBadRequestWithoutRequiredField(caseName: String, fieldName: String) {
                putInvalidBody(validBrandingBody() - fieldName, caseName)
            }

            @ParameterizedTest(name = "{0}")
            @MethodSource(
                "kz.superkassa.tests.api.management.settings.branding." +
                    "KkmBrandingRegressionTest#nullRequiredRequestFields",
            )
            @Severity(SeverityLevel.NORMAL)
            @DisplayName("Метод PUT /kkm/{kkmId}/settings/branding возвращает 400 для null в обязательном поле")
            fun shouldReturnBadRequestForNullRequiredField(caseName: String, fieldName: String) {
                putInvalidBody(validBrandingBody() + (fieldName to null), caseName)
            }

            @ParameterizedTest(name = "{0}")
            @MethodSource(
                "kz.superkassa.tests.api.management.settings.branding." +
                    "KkmBrandingRegressionTest#invalidRequiredFieldTypes",
            )
            @Severity(SeverityLevel.NORMAL)
            @DisplayName("Метод PUT /kkm/{kkmId}/settings/branding возвращает 400 для неправильных типов required-полей")
            fun shouldReturnBadRequestForInvalidRequiredFieldType(
                caseName: String,
                fieldName: String,
                invalidValue: Any,
            ) {
                putInvalidBody(validBrandingBody() + (fieldName to invalidValue), caseName)
            }

            @ParameterizedTest(name = "{0}")
            @MethodSource(
                "kz.superkassa.tests.api.management.settings.branding." +
                    "KkmBrandingRegressionTest#invalidOptionalFieldTypes",
            )
            @Severity(SeverityLevel.NORMAL)
            @DisplayName("Метод PUT /kkm/{kkmId}/settings/branding возвращает 400 для неправильных типов optional-полей")
            fun shouldReturnBadRequestForInvalidOptionalFieldType(
                caseName: String,
                fieldName: String,
                invalidValue: Any,
            ) {
                putInvalidBody(validBrandingBody() + (fieldName to invalidValue), caseName)
            }

            @ParameterizedTest(name = "{0}")
            @MethodSource(
                "kz.superkassa.tests.api.management.settings.branding." +
                    "KkmBrandingRegressionTest#invalidOfdTicketAdsItems",
            )
            @Severity(SeverityLevel.NORMAL)
            @DisplayName("Метод PUT /kkm/{kkmId}/settings/branding возвращает 400 для элемента ofdTicketAds не типа String")
            fun shouldReturnBadRequestForInvalidOfdTicketAdsItemType(caseName: String, invalidItem: Any?) {
                putInvalidBody(
                    validBrandingBody() + ("ofdTicketAds" to listOf(invalidItem)),
                    caseName,
                )
            }

            @ParameterizedTest(name = "{0}")
            @MethodSource(
                "kz.superkassa.tests.api.management.settings.branding." +
                    "KkmBrandingRegressionTest#invalidLanguageValues",
            )
            @Severity(SeverityLevel.NORMAL)
            @DisplayName("Метод PUT /kkm/{kkmId}/settings/branding возвращает 400 для неизвестного language")
            fun shouldReturnBadRequestForUnsupportedLanguage(caseName: String, invalidLanguage: String) {
                putInvalidBody(
                    validBrandingBody() + ("language" to invalidLanguage),
                    caseName,
                )
            }

            @ParameterizedTest(name = "{0}")
            @MethodSource(
                "kz.superkassa.tests.api.management.settings.branding." +
                    "KkmBrandingRegressionTest#invalidPaperWidthBoundaryValues",
            )
            @Severity(SeverityLevel.NORMAL)
            @DisplayName("Метод PUT /kkm/{kkmId}/settings/branding возвращает 400 для недопустимой ширины бумаги")
            fun shouldReturnBadRequestForInvalidPaperWidth(caseName: String, invalidPaperWidth: Int) {
                putInvalidBody(
                    validBrandingBody() + ("paperWidthMm" to invalidPaperWidth),
                    caseName,
                )
            }

            @ParameterizedTest(name = "{0}")
            @MethodSource(
                "kz.superkassa.tests.api.management.settings.branding." +
                    "KkmBrandingRegressionTest#invalidThemeColorValues",
            )
            @Severity(SeverityLevel.NORMAL)
            @DisplayName("Метод PUT /kkm/{kkmId}/settings/branding возвращает 400 для недопустимого themeColor")
            fun shouldReturnBadRequestForInvalidThemeColor(caseName: String, invalidThemeColor: String) {
                putInvalidBody(
                    validBrandingBody() + ("themeColor" to invalidThemeColor),
                    caseName,
                )
            }
        }

        @Nested
        @ApiRegression
        @Feature("API")
        @Story("PUT /kkm/{kkmId}/settings/branding")
        @Owner("Pavel Michka")
        @DisplayName("Проверки несуществующих идентификаторов")
        inner class MissingIdentifiersTests {
            @Test
            @Severity(SeverityLevel.NORMAL)
            @DisplayName("Метод PUT /kkm/{kkmId}/settings/branding возвращает 404 для несуществующей ККМ")
            fun shouldReturnNotFoundForUnknownKkmId() {
                val unknownKkmId = UUID.randomUUID().toString()

                reportStep("Проверяем PUT ${brandingPath(unknownKkmId)} для несуществующей ККМ") {
                    superkassa.request(preparedKkm.adminPin)
                        .body(validBrandingBody())
                        .`when`()
                        .put(brandingPath(unknownKkmId))
                        .then()
                        .shouldHaveStatus(404, "изменение branding для несуществующей ККМ")
                        .contentType(ContentType.JSON)
                }
            }
        }

        @Nested
        @ApiRegression
        @Feature("API")
        @Story("PUT /kkm/{kkmId}/settings/branding")
        @Owner("Pavel Michka")
        @DisplayName("Проверки неподдерживаемых HTTP-методов")
        inner class UnsupportedHttpMethodsTests {
            @ParameterizedTest(name = "HTTP {0} /kkm/'{'kkmId'}'/settings/branding возвращает 405")
            @EnumSource(value = Method::class, names = ["GET", "POST", "PATCH", "DELETE"])
            @Severity(SeverityLevel.NORMAL)
            @DisplayName("Метод /kkm/{kkmId}/settings/branding возвращает 405 для HTTP-методов кроме PUT")
            fun shouldReturnMethodNotAllowedForNonPutMethods(method: Method) {
                reportStep("Проверяем, что HTTP $method ${brandingPath(preparedKkm.kkmId)} не поддерживается") {
                    superkassa.request(preparedKkm.adminPin)
                        .`when`()
                        .request(method, brandingPath(preparedKkm.kkmId))
                        .then()
                        .shouldHaveStatus(405, "неподдерживаемый HTTP-метод")
                }
            }
        }
    }

    private fun updateBranding(body: Map<String, Any?>): Map<String, Any?> =
        reportStep("Обновляем настройки брендирования через PUT ${brandingPath(preparedKkm.kkmId)}") {
            superkassa.request(preparedKkm.adminPin)
                .body(body)
                .`when`()
                .put(brandingPath(preparedKkm.kkmId))
                .then()
                .shouldHaveStatus(200, "обновление настроек брендирования")
                .contentType(ContentType.JSON)
                .extract()
                .jsonPath()
                .getMap("")
        }

    private fun putInvalidBody(body: Any, scenario: String) {
        reportStep("Отправляем невалидное тело PUT ${brandingPath(preparedKkm.kkmId)}: $scenario") {
            superkassa.request(preparedKkm.adminPin)
                .body(body)
                .`when`()
                .put(brandingPath(preparedKkm.kkmId))
                .then()
                .shouldHaveStatus(400, scenario)
                .contentType(ContentType.JSON)
        }
    }

    private fun validBrandingBody(): Map<String, Any?> = mapOf(
        "language" to "MIXED",
        "ofdTicketAds" to listOf("Superkassa"),
        "paperWidthMm" to 80,
        "printOfdTicketAds" to false,
        "themeColor" to "#1F1C2C",
        "useForceDarkTheme" to false,
    )

    private fun fullBrandingBody(): Map<String, Any?> = validBrandingBody() + mapOf(
        "afterHeaderMsg" to "После заголовка",
        "afterItemsMsg" to "После списка позиций",
        "afterTotalsMsg" to "После итогов",
        "beforeHeaderMsg" to "Перед заголовком",
        "beforeItemsMsg" to "Перед списком позиций",
        "beforeQrMsg" to "Перед QR-кодом",
        "beforeTotalsMsg" to "Перед итогами",
        "customBackgroundColorHex" to "#FFFFFF",
        "customCardTopBorderColorHex" to "#1F1C2C",
        "footerMsg" to "Подвал чека",
        "headerLogoUrl" to "https://example.com/logo.png",
        "headerMsg" to "Заголовок чека",
    )

    private fun enterProgramming(preparedKkm: PreparedKkmAuth) {
        reportStep(
            "Переводим ККМ kkmId='${preparedKkm.kkmId}' в режим программирования перед обновлением брендирования",
        ) {
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
                .shouldHaveStatus(200, "подготовка: вход ККМ в режим программирования перед обновлением брендирования")
                .contentType(ContentType.JSON)
        }
    }

    private fun exitProgramming(preparedKkm: PreparedKkmAuth) {
        reportStep(
            "Возвращаем ККМ kkmId='${preparedKkm.kkmId}' из режима программирования после проверки брендирования",
        ) {
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
        field: FieldType,
    ) {
        softly.assertThat(response)
            .withFailMessage(
                ApiContractErrorMessages.requiredFieldWithTypeMissing(
                    ENDPOINT,
                    field.path,
                    field.type.simpleName,
                    field.schema,
                ),
            )
            .containsKey(field.name)

        softly.assertThat(response[field.name])
            .withFailMessage(
                ApiContractErrorMessages.fieldTypeMismatch(
                    ENDPOINT,
                    field.path,
                    field.type.simpleName,
                    field.schema,
                ),
            )
            .isInstanceOf(field.type)
    }

    private fun assertOptionalFieldType(
        softly: SoftAssertions,
        response: Map<String, Any?>,
        field: FieldType,
    ) {
        val value = response[field.name] ?: return

        softly.assertThat(value)
            .withFailMessage(
                ApiContractErrorMessages.optionalFieldTypeMismatch(
                    ENDPOINT,
                    field.path,
                    field.type.simpleName,
                    field.schema,
                ),
            )
            .isInstanceOf(field.type)
    }

    private fun assertOfdTicketAdsItemsType(softly: SoftAssertions, value: Any?) {
        (value as? List<*>)?.forEachIndexed { index, item ->
            softly.assertThat(item)
                .withFailMessage(
                    ApiContractErrorMessages.arrayItemTypeMismatch(
                        ENDPOINT,
                        "branding.ofdTicketAds",
                        index,
                        String::class.java.simpleName,
                        BRANDING_RESPONSE_SCHEMA,
                    ),
                )
                .isInstanceOf(String::class.java)
        }
    }

    private fun assertOnlySwaggerFields(
        softly: SoftAssertions,
        response: Map<String, Any?>,
        schema: String,
        allowedFields: Set<String>,
    ) {
        val unexpectedFields = response.keys - allowedFields

        softly.assertThat(unexpectedFields)
            .withFailMessage(ApiContractErrorMessages.unexpectedSwaggerFields(ENDPOINT, schema, unexpectedFields))
            .isEmpty()
    }

    private fun assertRequiredEnumValue(
        softly: SoftAssertions,
        response: Map<String, Any?>,
        fieldName: String,
        fieldPath: String,
        schema: String,
        supportedValues: Set<String>,
    ) {
        val value = response[fieldName] as? String

        softly.assertThat(value)
            .withFailMessage(ApiContractErrorMessages.requiredEnumMissing(ENDPOINT, fieldPath, schema))
            .isNotBlank()

        softly.assertThat(value)
            .withFailMessage(ApiContractErrorMessages.enumUnsupported(ENDPOINT, fieldPath, value, supportedValues))
            .isIn(supportedValues)
    }

    private fun assertOptionalEnumValue(
        softly: SoftAssertions,
        response: Map<String, Any?>,
        fieldName: String,
        fieldPath: String,
        supportedValues: Set<String>,
    ) {
        val value = response[fieldName] ?: return
        val enumValue = value as? String

        softly.assertThat(enumValue)
            .withFailMessage(
                ApiContractErrorMessages.enumUnsupported(ENDPOINT, fieldPath, enumValue, supportedValues),
            )
            .isIn(supportedValues)
    }

    @Suppress("UNCHECKED_CAST")
    private fun Map<String, Any?>.objectField(fieldName: String): Map<String, Any?>? =
        this[fieldName] as? Map<String, Any?>

    private fun brandingPath(kkmId: String): String = "/kkm/$kkmId/settings/branding"

    private data class FieldType(
        val name: String,
        val path: String,
        val type: Class<*>,
        val schema: String,
    )

    @Suppress("unused")
    private companion object {
        const val ENDPOINT = "PUT /kkm/{kkmId}/settings/branding"
        const val KKM_RESPONSE_SCHEMA = "KkmResponse"
        const val OFD_SERVICE_INFO_SCHEMA = "OfdServiceInfoResponse"
        const val BRANDING_RESPONSE_SCHEMA = "ReceiptBrandingResponse"
        const val INVALID_PIN = "999999"

        val REQUIRED_REQUEST_FIELDS = listOf(
            "language",
            "ofdTicketAds",
            "paperWidthMm",
            "printOfdTicketAds",
            "themeColor",
            "useForceDarkTheme",
        )

        val OPTIONAL_STRING_REQUEST_FIELDS = listOf(
            "afterHeaderMsg",
            "afterItemsMsg",
            "afterTotalsMsg",
            "beforeHeaderMsg",
            "beforeItemsMsg",
            "beforeQrMsg",
            "beforeTotalsMsg",
            "customBackgroundColorHex",
            "customCardTopBorderColorHex",
            "footerMsg",
            "headerLogoUrl",
            "headerMsg",
        )

        val REQUIRED_KKM_FIELD_TYPES = listOf(
            FieldType("autoCloseShift", "autoCloseShift", Boolean::class.javaObjectType, KKM_RESPONSE_SCHEMA),
            FieldType("createdAt", "createdAt", Long::class.javaObjectType, KKM_RESPONSE_SCHEMA),
            FieldType("kkmId", "kkmId", String::class.java, KKM_RESPONSE_SCHEMA),
            FieldType("mode", "mode", String::class.java, KKM_RESPONSE_SCHEMA),
            FieldType("state", "state", String::class.java, KKM_RESPONSE_SCHEMA),
            FieldType("updatedAt", "updatedAt", Long::class.javaObjectType, KKM_RESPONSE_SCHEMA),
        )

        val OPTIONAL_KKM_FIELD_TYPES = listOf(
            FieldType("autonomousSince", "autonomousSince", Long::class.javaObjectType, KKM_RESPONSE_SCHEMA),
            FieldType("branding", "branding", Map::class.java, KKM_RESPONSE_SCHEMA),
            FieldType("defaultVatGroup", "defaultVatGroup", String::class.java, KKM_RESPONSE_SCHEMA),
            FieldType("factoryNumber", "factoryNumber", String::class.java, KKM_RESPONSE_SCHEMA),
            FieldType("kkmKgdId", "kkmKgdId", String::class.java, KKM_RESPONSE_SCHEMA),
            FieldType("lastFiscalHashBase64", "lastFiscalHashBase64", String::class.java, KKM_RESPONSE_SCHEMA),
            FieldType("lastReceiptNo", "lastReceiptNo", Int::class.javaObjectType, KKM_RESPONSE_SCHEMA),
            FieldType("lastShiftNo", "lastShiftNo", Int::class.javaObjectType, KKM_RESPONSE_SCHEMA),
            FieldType("lastZReportNo", "lastZReportNo", Int::class.javaObjectType, KKM_RESPONSE_SCHEMA),
            FieldType("manufactureYear", "manufactureYear", Int::class.javaObjectType, KKM_RESPONSE_SCHEMA),
            FieldType("ofdEnvironment", "ofdEnvironment", String::class.java, KKM_RESPONSE_SCHEMA),
            FieldType("ofdId", "ofdId", String::class.java, KKM_RESPONSE_SCHEMA),
            FieldType("ofdServiceInfo", "ofdServiceInfo", Map::class.java, KKM_RESPONSE_SCHEMA),
            FieldType("ofdSystemId", "ofdSystemId", String::class.java, KKM_RESPONSE_SCHEMA),
            FieldType("taxRegime", "taxRegime", String::class.java, KKM_RESPONSE_SCHEMA),
            FieldType("tokenUpdatedAt", "tokenUpdatedAt", Long::class.javaObjectType, KKM_RESPONSE_SCHEMA),
        )

        val OFD_SERVICE_INFO_FIELD_TYPES = listOf(
            FieldType("geoLatitude", "ofdServiceInfo.geoLatitude", Int::class.javaObjectType, OFD_SERVICE_INFO_SCHEMA),
            FieldType("geoLongitude", "ofdServiceInfo.geoLongitude", Int::class.javaObjectType, OFD_SERVICE_INFO_SCHEMA),
            FieldType("geoSource", "ofdServiceInfo.geoSource", String::class.java, OFD_SERVICE_INFO_SCHEMA),
            FieldType("orgAddress", "ofdServiceInfo.orgAddress", String::class.java, OFD_SERVICE_INFO_SCHEMA),
            FieldType("orgAddressKz", "ofdServiceInfo.orgAddressKz", String::class.java, OFD_SERVICE_INFO_SCHEMA),
            FieldType("orgInn", "ofdServiceInfo.orgInn", String::class.java, OFD_SERVICE_INFO_SCHEMA),
            FieldType("orgOkved", "ofdServiceInfo.orgOkved", String::class.java, OFD_SERVICE_INFO_SCHEMA),
            FieldType("orgTitle", "ofdServiceInfo.orgTitle", String::class.java, OFD_SERVICE_INFO_SCHEMA),
        )

        val BRANDING_REQUIRED_FIELD_TYPES = listOf(
            FieldType("language", "branding.language", String::class.java, BRANDING_RESPONSE_SCHEMA),
            FieldType("ofdTicketAds", "branding.ofdTicketAds", List::class.java, BRANDING_RESPONSE_SCHEMA),
            FieldType("paperWidthMm", "branding.paperWidthMm", Int::class.javaObjectType, BRANDING_RESPONSE_SCHEMA),
            FieldType(
                "printOfdTicketAds",
                "branding.printOfdTicketAds",
                Boolean::class.javaObjectType,
                BRANDING_RESPONSE_SCHEMA,
            ),
            FieldType("themeColor", "branding.themeColor", String::class.java, BRANDING_RESPONSE_SCHEMA),
            FieldType(
                "useForceDarkTheme",
                "branding.useForceDarkTheme",
                Boolean::class.javaObjectType,
                BRANDING_RESPONSE_SCHEMA,
            ),
        )

        val BRANDING_OPTIONAL_FIELD_TYPES = OPTIONAL_STRING_REQUEST_FIELDS.map { fieldName ->
            FieldType(fieldName, "branding.$fieldName", String::class.java, BRANDING_RESPONSE_SCHEMA)
        }

        val KKM_RESPONSE_FIELDS = setOf(
            "autoCloseShift",
            "autonomousSince",
            "branding",
            "createdAt",
            "defaultVatGroup",
            "factoryNumber",
            "kkmId",
            "kkmKgdId",
            "lastFiscalHashBase64",
            "lastReceiptNo",
            "lastShiftNo",
            "lastZReportNo",
            "manufactureYear",
            "mode",
            "ofdEnvironment",
            "ofdId",
            "ofdServiceInfo",
            "ofdSystemId",
            "state",
            "taxRegime",
            "tokenUpdatedAt",
            "updatedAt",
        )

        val OFD_SERVICE_INFO_RESPONSE_FIELDS = setOf(
            "geoLatitude",
            "geoLongitude",
            "geoSource",
            "orgAddress",
            "orgAddressKz",
            "orgInn",
            "orgOkved",
            "orgTitle",
        )

        val BRANDING_RESPONSE_FIELDS = (
            OPTIONAL_STRING_REQUEST_FIELDS + listOf(
                "language",
                "ofdTicketAds",
                "paperWidthMm",
                "printOfdTicketAds",
                "themeColor",
                "useForceDarkTheme",
            )
        ).toSet()

        @JvmStatic
        fun requiredRequestFields(): Stream<Arguments> = REQUIRED_REQUEST_FIELDS.stream().map { fieldName ->
            Arguments.of("обязательное поле $fieldName отсутствует", fieldName)
        }

        @JvmStatic
        fun nullRequiredRequestFields(): Stream<Arguments> = REQUIRED_REQUEST_FIELDS.stream().map { fieldName ->
            Arguments.of("обязательное поле $fieldName содержит null", fieldName)
        }

        @JvmStatic
        fun nullableOptionalRequestFields(): Stream<Arguments> = OPTIONAL_STRING_REQUEST_FIELDS.stream().map { fieldName ->
            Arguments.of("nullable optional-поле $fieldName содержит null", fieldName)
        }

        @JvmStatic
        fun invalidRequiredFieldTypes(): Stream<Arguments> = Stream.of(
            Arguments.of("поле language имеет тип Number вместо String", "language", 123),
            Arguments.of("поле ofdTicketAds имеет тип String вместо Array", "ofdTicketAds", "Superkassa"),
            Arguments.of("поле paperWidthMm имеет тип String вместо Integer", "paperWidthMm", "80"),
            Arguments.of("поле paperWidthMm имеет дробный тип Number вместо Integer", "paperWidthMm", 80.5),
            Arguments.of("поле printOfdTicketAds имеет тип String вместо Boolean", "printOfdTicketAds", "false"),
            Arguments.of("поле themeColor имеет тип Number вместо String", "themeColor", 123),
            Arguments.of("поле useForceDarkTheme имеет тип String вместо Boolean", "useForceDarkTheme", "false"),
        )

        @JvmStatic
        fun invalidOptionalFieldTypes(): Stream<Arguments> = OPTIONAL_STRING_REQUEST_FIELDS.stream().map { fieldName ->
            Arguments.of("optional-поле $fieldName имеет тип Number вместо String", fieldName, 123)
        }

        @JvmStatic
        fun validRequiredFieldValues(): Stream<Arguments> = listOf(
            Arguments.of("language содержит допустимое значение RU", "language", "RU"),
            Arguments.of("language содержит допустимое значение KK", "language", "KK"),
            Arguments.of("language содержит допустимое значение MIXED", "language", "MIXED"),
            Arguments.of("ofdTicketAds содержит пустой массив", "ofdTicketAds", emptyList<String>()),
            Arguments.of("ofdTicketAds содержит один текст", "ofdTicketAds", listOf("Superkassa")),
            Arguments.of("ofdTicketAds содержит несколько текстов", "ofdTicketAds", listOf("Первый", "Второй")),
            Arguments.of("ofdTicketAds содержит пустую строку", "ofdTicketAds", listOf("")),
            Arguments.of("ofdTicketAds содержит пробельную строку", "ofdTicketAds", listOf("   ")),
            Arguments.of("ofdTicketAds содержит Unicode и спецсимволы", "ofdTicketAds", listOf("Қазақша <>&")),
            Arguments.of("paperWidthMm содержит допустимую границу 58", "paperWidthMm", 58),
            Arguments.of("paperWidthMm содержит допустимую границу 80", "paperWidthMm", 80),
            Arguments.of("printOfdTicketAds содержит true", "printOfdTicketAds", true),
            Arguments.of("printOfdTicketAds содержит false", "printOfdTicketAds", false),
            Arguments.of("themeColor содержит #1F1C2C", "themeColor", "#1F1C2C"),
            Arguments.of("themeColor содержит #000000", "themeColor", "#000000"),
            Arguments.of("themeColor содержит #007AFF", "themeColor", "#007AFF"),
            Arguments.of("themeColor содержит #34C759", "themeColor", "#34C759"),
            Arguments.of("themeColor содержит #FF9500", "themeColor", "#FF9500"),
            Arguments.of("themeColor содержит #FF3B30", "themeColor", "#FF3B30"),
            Arguments.of("themeColor содержит #5856D6", "themeColor", "#5856D6"),
            Arguments.of("useForceDarkTheme содержит true", "useForceDarkTheme", true),
            Arguments.of("useForceDarkTheme содержит false", "useForceDarkTheme", false),
        ).stream()

        @JvmStatic
        fun optionalStringEquivalenceClasses(): Stream<Arguments> = OPTIONAL_STRING_REQUEST_FIELDS.flatMap { fieldName ->
            listOf(
                Arguments.of("optional-поле $fieldName содержит пустую строку", fieldName, ""),
                Arguments.of("optional-поле $fieldName содержит только пробелы", fieldName, "   "),
                Arguments.of("optional-поле $fieldName содержит Unicode и спецсимволы", fieldName, "Қазақша <>&"),
            )
        }.stream()

        @JvmStatic
        fun invalidOfdTicketAdsItems(): Stream<Arguments> = Stream.of(
            Arguments.of("элемент ofdTicketAds содержит null вместо String", null),
            Arguments.of("элемент ofdTicketAds имеет тип Number вместо String", 123),
            Arguments.of("элемент ofdTicketAds имеет тип Boolean вместо String", true),
            Arguments.of("элемент ofdTicketAds имеет тип Object вместо String", mapOf("text" to "Superkassa")),
            Arguments.of("элемент ofdTicketAds имеет тип Array вместо String", listOf("Superkassa")),
        )

        @JvmStatic
        fun invalidLanguageValues(): Stream<Arguments> = Stream.of(
            Arguments.of("language содержит пустую строку вне Swagger-enum", ""),
            Arguments.of("language содержит только пробелы вне Swagger-enum", "   "),
            Arguments.of("language содержит значение UNKNOWN вне Swagger-enum", "UNKNOWN"),
            Arguments.of("language содержит значение mixed в неправильном регистре", "mixed"),
        )

        @JvmStatic
        fun invalidPaperWidthBoundaryValues(): Stream<Arguments> = Stream.of(
            Arguments.of("paperWidthMm меньше допустимой границы 58: 57", 57),
            Arguments.of("paperWidthMm больше допустимой границы 58: 59", 59),
            Arguments.of("paperWidthMm меньше допустимой границы 80: 79", 79),
            Arguments.of("paperWidthMm больше допустимой границы 80: 81", 81),
            Arguments.of("paperWidthMm содержит нулевое значение", 0),
            Arguments.of("paperWidthMm содержит отрицательное значение", -1),
        )

        @JvmStatic
        fun invalidThemeColorValues(): Stream<Arguments> = Stream.of(
            Arguments.of("themeColor содержит пустую строку", ""),
            Arguments.of("themeColor содержит только пробелы", "   "),
            Arguments.of("themeColor содержит неизвестный цвет #FFFFFF", "#FFFFFF"),
            Arguments.of("themeColor содержит значение UNKNOWN", "UNKNOWN"),
        )
    }
}
