package kz.superkassa.tests.api.management.settings.tax

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
@Story("PUT /kkm/{kkmId}/settings/tax")
@Owner("Pavel Michka")
@DisplayName("PUT /kkm/{kkmId}/settings/tax: регрессионные проверки налоговых настроек ККМ")
@ResourceLock(value = "kkm-state", mode = ResourceAccessMode.READ_WRITE)
@Suppress("NonAsciiCharacters")
class KkmTaxSettingsRegressionTest : KkmAuthenticatedTest() {
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
    @DisplayName("Позитивные проверки PUT /kkm/{kkmId}/settings/tax")
    inner class PositiveRegressionTests {
        @BeforeEach
        fun `Переводим ККМ в режим программирования`() {
            enterProgramming(preparedKkm)
        }

        @Test
        @Severity(SeverityLevel.CRITICAL)
        @DisplayName("Метод PUT /kkm/{kkmId}/settings/tax возвращает поля ожидаемых типов")
        fun shouldReturnExpectedFieldTypes() {
            val response = updateTaxSettings(validTaxSettingsBody())

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
        @DisplayName("Метод PUT /kkm/{kkmId}/settings/tax возвращает допустимые enum-значения")
        fun shouldReturnSupportedEnumValues() {
            val response = updateTaxSettings(validTaxSettingsBody())

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
        @DisplayName("Метод PUT /kkm/{kkmId}/settings/tax не возвращает поля вне Swagger-контракта")
        fun shouldNotReturnFieldsOutsideSwaggerContract() {
            val response = updateTaxSettings(validTaxSettingsBody())

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

        @Nested
        @DisplayName("Допустимые значения полей запроса")
        inner class ValidRequestFieldsTests {
            @Nested
            @DisplayName("Поле taxRegime")
            inner class TaxRegimeFieldTests {
                @ParameterizedTest(name = "{0}")
                @MethodSource(
                    "kz.superkassa.tests.api.management.settings.tax." +
                        "KkmTaxSettingsRegressionTest#validTaxRegimeValues",
                )
                @Severity(SeverityLevel.NORMAL)
                @DisplayName("Принимает допустимые enum-значения")
                fun acceptsSupportedValue(caseName: String, value: String) {
                    acceptFieldValue(caseName, TAX_REGIME_FIELD, value)
                }
            }

            @Nested
            @DisplayName("Поле defaultVatGroup")
            inner class DefaultVatGroupFieldTests {
                @ParameterizedTest(name = "{0}")
                @MethodSource(
                    "kz.superkassa.tests.api.management.settings.tax." +
                        "KkmTaxSettingsRegressionTest#validDefaultVatGroupValues",
                )
                @Severity(SeverityLevel.NORMAL)
                @DisplayName("Принимает допустимые enum-значения")
                fun acceptsSupportedValue(caseName: String, value: String) {
                    acceptFieldValue(caseName, DEFAULT_VAT_GROUP_FIELD, value)
                }
            }
        }
    }

    @Nested
    @ApiRegression
    @DisplayName("Негативные проверки PUT /kkm/{kkmId}/settings/tax")
    inner class NegativeRegressionTests {
        @Nested
        @DisplayName("Проверки авторизации PUT /kkm/{kkmId}/settings/tax")
        inner class AuthorizationRegressionTests {
            @BeforeEach
            fun `Переводим ККМ в режим программирования`() {
                enterProgramming(preparedKkm)
            }

            @Test
            @Severity(SeverityLevel.NORMAL)
            @DisplayName("Метод PUT /kkm/{kkmId}/settings/tax возвращает 401 без Authorization")
            fun shouldReturnUnauthorizedWithoutAuthorization() {
                reportStep("Проверяем PUT ${taxSettingsPath(preparedKkm.kkmId)} без Authorization") {
                    superkassa.requestWithoutAuthorization()
                        .body(validTaxSettingsBody())
                        .`when`()
                        .put(taxSettingsPath(preparedKkm.kkmId))
                        .then()
                        .shouldHaveStatus(401, "запрос изменения налоговых настроек без Authorization")
                        .contentType(ContentType.JSON)
                }
            }

            @Test
            @Severity(SeverityLevel.NORMAL)
            @DisplayName("Метод PUT /kkm/{kkmId}/settings/tax возвращает 403 для неверного PIN")
            fun shouldReturnForbiddenForInvalidPin() {
                reportStep("Проверяем PUT ${taxSettingsPath(preparedKkm.kkmId)} с неверным PIN") {
                    superkassa.request(INVALID_PIN)
                        .body(validTaxSettingsBody())
                        .`when`()
                        .put(taxSettingsPath(preparedKkm.kkmId))
                        .then()
                        .shouldHaveStatus(403, "запрос изменения налоговых настроек с неверным PIN")
                        .contentType(ContentType.JSON)
                }
            }
        }

        @Nested
        @DisplayName("Проверки невалидного тела запроса")
        inner class InvalidRequestBodyTests {
            @BeforeEach
            fun `Переводим ККМ в режим программирования`() {
                enterProgramming(preparedKkm)
            }

            @Nested
            @DisplayName("Общая структура тела запроса")
            inner class RequestBodyStructureTests {
                @Test
                @Severity(SeverityLevel.NORMAL)
                @DisplayName("Метод PUT /kkm/{kkmId}/settings/tax возвращает 400 без тела запроса")
                fun shouldReturnBadRequestWithoutRequestBody() {
                    reportStep("Отправляем PUT ${taxSettingsPath(preparedKkm.kkmId)} без тела запроса") {
                        superkassa.request(preparedKkm.adminPin)
                            .`when`()
                            .put(taxSettingsPath(preparedKkm.kkmId))
                            .then()
                            .shouldHaveStatus(400, "запрос изменения налоговых настроек без тела запроса")
                            .contentType(ContentType.JSON)
                    }
                }

                @Test
                @Severity(SeverityLevel.NORMAL)
                @DisplayName("Метод PUT /kkm/{kkmId}/settings/tax возвращает 400 для пустого объекта")
                fun shouldReturnBadRequestForEmptyObject() {
                    putInvalidBody(emptyMap<String, Any?>(), "тело запроса не содержит обязательных полей")
                }
            }

            @Nested
            @DisplayName("Поле taxRegime")
            inner class TaxRegimeFieldTests {
                @ParameterizedTest(name = "{0}")
                @MethodSource(
                    "kz.superkassa.tests.api.management.settings.tax." +
                        "KkmTaxSettingsRegressionTest#invalidTaxRegimeValues",
                )
                @Severity(SeverityLevel.NORMAL)
                @DisplayName("Возвращает 400 для невалидного значения")
                fun rejectsInvalidValue(caseName: String, value: Any?) {
                    rejectFieldValue(caseName, TAX_REGIME_FIELD, value)
                }
            }

            @Nested
            @DisplayName("Поле defaultVatGroup")
            inner class DefaultVatGroupFieldTests {
                @ParameterizedTest(name = "{0}")
                @MethodSource(
                    "kz.superkassa.tests.api.management.settings.tax." +
                        "KkmTaxSettingsRegressionTest#invalidDefaultVatGroupValues",
                )
                @Severity(SeverityLevel.NORMAL)
                @DisplayName("Возвращает 400 для невалидного значения")
                fun rejectsInvalidValue(caseName: String, value: Any?) {
                    rejectFieldValue(caseName, DEFAULT_VAT_GROUP_FIELD, value)
                }
            }
        }

        @Nested
        @DisplayName("Проверки несуществующих идентификаторов")
        inner class MissingIdentifiersTests {
            @Test
            @Severity(SeverityLevel.NORMAL)
            @DisplayName("Метод PUT /kkm/{kkmId}/settings/tax возвращает 404 для несуществующей ККМ")
            fun shouldReturnNotFoundForUnknownKkmId() {
                val unknownKkmId = UUID.randomUUID().toString()

                reportStep("Проверяем PUT ${taxSettingsPath(unknownKkmId)} для несуществующей ККМ") {
                    superkassa.request(preparedKkm.adminPin)
                        .body(validTaxSettingsBody())
                        .`when`()
                        .put(taxSettingsPath(unknownKkmId))
                        .then()
                        .shouldHaveStatus(404, "изменение налоговых настроек для несуществующей ККМ")
                        .contentType(ContentType.JSON)
                }
            }
        }

        @Nested
        @DisplayName("Проверки неподдерживаемых HTTP-методов")
        inner class UnsupportedHttpMethodsTests {
            @ParameterizedTest(name = "HTTP {0} /kkm/'{'kkmId'}'/settings/tax возвращает 405")
            @EnumSource(value = Method::class, names = ["GET", "POST", "PATCH", "DELETE"])
            @Severity(SeverityLevel.NORMAL)
            @DisplayName("Метод /kkm/{kkmId}/settings/tax возвращает 405 для HTTP-методов кроме PUT")
            fun shouldReturnMethodNotAllowedForNonPutMethods(method: Method) {
                reportStep("Проверяем, что HTTP $method ${taxSettingsPath(preparedKkm.kkmId)} не поддерживается") {
                    superkassa.request(preparedKkm.adminPin)
                        .`when`()
                        .request(method, taxSettingsPath(preparedKkm.kkmId))
                        .then()
                        .shouldHaveStatus(405, "неподдерживаемый HTTP-метод")
                }
            }
        }
    }

    private fun updateTaxSettings(body: Map<String, Any?>): Map<String, Any?> =
        reportStep("Обновляем налоговые настройки через PUT ${taxSettingsPath(preparedKkm.kkmId)}") {
            superkassa.request(preparedKkm.adminPin)
                .body(body)
                .`when`()
                .put(taxSettingsPath(preparedKkm.kkmId))
                .then()
                .shouldHaveStatus(200, "обновление налоговых настроек ККМ")
                .contentType(ContentType.JSON)
                .extract()
                .jsonPath()
                .getMap("")
        }

    private fun putInvalidBody(body: Any, scenario: String) {
        reportStep("Отправляем невалидное тело PUT ${taxSettingsPath(preparedKkm.kkmId)}: $scenario") {
            superkassa.request(preparedKkm.adminPin)
                .body(body)
                .`when`()
                .put(taxSettingsPath(preparedKkm.kkmId))
                .then()
                .shouldHaveStatus(400, scenario)
                .contentType(ContentType.JSON)
        }
    }

    private fun acceptFieldValue(caseName: String, fieldName: String, value: String) {
        reportStep("Проверяем допустимое значение поля $fieldName: $caseName") {
            updateTaxSettings(validTaxSettingsBody() + (fieldName to value))
        }
    }

    private fun rejectFieldValue(caseName: String, fieldName: String, value: Any?) {
        val body = if (value === OmittedFieldValue) {
            validTaxSettingsBody() - fieldName
        } else {
            validTaxSettingsBody() + (fieldName to value)
        }
        putInvalidBody(body, caseName)
    }

    private fun validTaxSettingsBody(): Map<String, Any?> = mapOf(
        TAX_REGIME_FIELD to "NO_VAT",
        DEFAULT_VAT_GROUP_FIELD to "NO_VAT",
    )

    private fun enterProgramming(preparedKkm: PreparedKkmAuth) {
        reportStep(
            "Переводим ККМ kkmId='${preparedKkm.kkmId}' в режим программирования перед обновлением налоговых настроек",
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
                .shouldHaveStatus(200, "подготовка: вход ККМ в режим программирования перед обновлением налоговых настроек")
                .contentType(ContentType.JSON)
        }
    }

    private fun exitProgramming(preparedKkm: PreparedKkmAuth) {
        reportStep(
            "Возвращаем ККМ kkmId='${preparedKkm.kkmId}' из режима программирования после проверки налоговых настроек",
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
            .withFailMessage(ApiContractErrorMessages.enumUnsupported(ENDPOINT, fieldPath, enumValue, supportedValues))
            .isIn(supportedValues)
    }

    @Suppress("UNCHECKED_CAST")
    private fun Map<String, Any?>.objectField(fieldName: String): Map<String, Any?>? =
        this[fieldName] as? Map<String, Any?>

    private fun taxSettingsPath(kkmId: String): String = "/kkm/$kkmId/settings/tax"

    private data class FieldType(
        val name: String,
        val path: String,
        val type: Class<*>,
        val schema: String,
    )

    private object OmittedFieldValue

    @Suppress("unused")
    private companion object {
        const val ENDPOINT = "PUT /kkm/{kkmId}/settings/tax"
        const val TAX_REGIME_FIELD = "taxRegime"
        const val DEFAULT_VAT_GROUP_FIELD = "defaultVatGroup"
        const val KKM_RESPONSE_SCHEMA = "KkmResponse"
        const val OFD_SERVICE_INFO_SCHEMA = "OfdServiceInfoResponse"
        const val BRANDING_RESPONSE_SCHEMA = "ReceiptBrandingResponse"
        const val INVALID_PIN = "999999"

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

        val BRANDING_OPTIONAL_FIELD_TYPES = listOf(
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
        ).map { fieldName ->
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

        val BRANDING_RESPONSE_FIELDS = setOf(
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
            "language",
            "ofdTicketAds",
            "paperWidthMm",
            "printOfdTicketAds",
            "themeColor",
            "useForceDarkTheme",
        )

        @JvmStatic
        fun validTaxRegimeValues(): Stream<Arguments> = Stream.of(
            Arguments.of("taxRegime принимает NO_VAT", "NO_VAT"),
            Arguments.of("taxRegime принимает VAT_PAYER", "VAT_PAYER"),
            Arguments.of("taxRegime принимает MIXED", "MIXED"),
        )

        @JvmStatic
        fun validDefaultVatGroupValues(): Stream<Arguments> = Stream.of(
            Arguments.of("defaultVatGroup принимает NO_VAT", "NO_VAT"),
            Arguments.of("defaultVatGroup принимает VAT_0", "VAT_0"),
            Arguments.of("defaultVatGroup принимает VAT_16", "VAT_16"),
        )

        @JvmStatic
        fun invalidTaxRegimeValues(): Stream<Arguments> = invalidEnumFieldValues(
            fieldName = TAX_REGIME_FIELD,
            unsupportedValue = "STANDARD",
            wrongCaseValue = "no_vat",
        )

        @JvmStatic
        fun invalidDefaultVatGroupValues(): Stream<Arguments> = invalidEnumFieldValues(
            fieldName = DEFAULT_VAT_GROUP_FIELD,
            unsupportedValue = "VAT_10",
            wrongCaseValue = "vat_16",
        )

        private fun invalidEnumFieldValues(
            fieldName: String,
            unsupportedValue: String,
            wrongCaseValue: String,
        ): Stream<Arguments> = Stream.of(
            Arguments.of("обязательное поле $fieldName отсутствует", OmittedFieldValue),
            Arguments.of("обязательное поле $fieldName содержит null", null),
            Arguments.of("поле $fieldName содержит пустую строку", ""),
            Arguments.of("поле $fieldName состоит только из пробелов", "   "),
            Arguments.of("поле $fieldName содержит неподдерживаемое значение $unsupportedValue", unsupportedValue),
            Arguments.of("поле $fieldName содержит значение в неправильном регистре $wrongCaseValue", wrongCaseValue),
            Arguments.of("поле $fieldName содержит пробел в начале", " NO_VAT"),
            Arguments.of("поле $fieldName содержит пробел в конце", "NO_VAT "),
            Arguments.of("поле $fieldName имеет тип Number вместо String", 123),
            Arguments.of("поле $fieldName имеет тип Boolean вместо String", true),
            Arguments.of("поле $fieldName имеет тип Object вместо String", mapOf("value" to "NO_VAT")),
            Arguments.of("поле $fieldName имеет тип Array вместо String", listOf("NO_VAT")),
        )
    }
}
