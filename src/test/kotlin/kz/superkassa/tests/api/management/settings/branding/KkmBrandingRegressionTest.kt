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

        @Nested
        @DisplayName("Допустимые значения полей запроса")
        inner class ValidRequestFieldTests {
            @Nested
            @DisplayName("Поле language")
            inner class LanguageFieldTests {
                @ParameterizedTest(name = "{0}")
                @MethodSource("kz.superkassa.tests.api.management.settings.branding.KkmBrandingRegressionTest#validLanguageFieldValues")
                @Severity(SeverityLevel.NORMAL)
                @DisplayName("Принимает допустимые значения")
                fun accepts(caseName: String, fieldName: String, value: Any?) = acceptFieldValue(caseName, fieldName, value)
            }

            @Nested
            @DisplayName("Поле ofdTicketAds")
            inner class OfdTicketAdsFieldTests {
                @ParameterizedTest(name = "{0}")
                @MethodSource("kz.superkassa.tests.api.management.settings.branding.KkmBrandingRegressionTest#validOfdTicketAdsFieldValues")
                @Severity(SeverityLevel.NORMAL)
                @DisplayName("Принимает допустимые значения")
                fun accepts(caseName: String, fieldName: String, value: Any?) = acceptFieldValue(caseName, fieldName, value)
            }

            @Nested
            @DisplayName("Поле paperWidthMm")
            inner class PaperWidthMmFieldTests {
                @ParameterizedTest(name = "{0}")
                @MethodSource("kz.superkassa.tests.api.management.settings.branding.KkmBrandingRegressionTest#validPaperWidthMmFieldValues")
                @Severity(SeverityLevel.NORMAL)
                @DisplayName("Принимает допустимые значения")
                fun accepts(caseName: String, fieldName: String, value: Any?) = acceptFieldValue(caseName, fieldName, value)
            }

            @Nested
            @DisplayName("Поле printOfdTicketAds")
            inner class PrintOfdTicketAdsFieldTests {
                @ParameterizedTest(name = "{0}")
                @MethodSource("kz.superkassa.tests.api.management.settings.branding.KkmBrandingRegressionTest#validPrintOfdTicketAdsFieldValues")
                @Severity(SeverityLevel.NORMAL)
                @DisplayName("Принимает допустимые значения")
                fun accepts(caseName: String, fieldName: String, value: Any?) = acceptFieldValue(caseName, fieldName, value)
            }

            @Nested
            @DisplayName("Поле themeColor")
            inner class ThemeColorFieldTests {
                @ParameterizedTest(name = "{0}")
                @MethodSource("kz.superkassa.tests.api.management.settings.branding.KkmBrandingRegressionTest#validThemeColorFieldValues")
                @Severity(SeverityLevel.NORMAL)
                @DisplayName("Принимает допустимые значения")
                fun accepts(caseName: String, fieldName: String, value: Any?) = acceptFieldValue(caseName, fieldName, value)
            }

            @Nested
            @DisplayName("Поле useForceDarkTheme")
            inner class UseForceDarkThemeFieldTests {
                @ParameterizedTest(name = "{0}")
                @MethodSource("kz.superkassa.tests.api.management.settings.branding.KkmBrandingRegressionTest#validUseForceDarkThemeFieldValues")
                @Severity(SeverityLevel.NORMAL)
                @DisplayName("Принимает допустимые значения")
                fun accepts(caseName: String, fieldName: String, value: Any?) = acceptFieldValue(caseName, fieldName, value)
            }

            @Nested
            @DisplayName("Поле afterHeaderMsg")
            inner class AfterHeaderMsgFieldTests {
                @ParameterizedTest(name = "{0}")
                @MethodSource("kz.superkassa.tests.api.management.settings.branding.KkmBrandingRegressionTest#validAfterHeaderMsgFieldValues")
                @Severity(SeverityLevel.NORMAL)
                @DisplayName("Принимает допустимые значения")
                fun accepts(caseName: String, fieldName: String, value: Any?) = acceptFieldValue(caseName, fieldName, value)
            }

            @Nested
            @DisplayName("Поле afterItemsMsg")
            inner class AfterItemsMsgFieldTests {
                @ParameterizedTest(name = "{0}")
                @MethodSource("kz.superkassa.tests.api.management.settings.branding.KkmBrandingRegressionTest#validAfterItemsMsgFieldValues")
                @Severity(SeverityLevel.NORMAL)
                @DisplayName("Принимает допустимые значения")
                fun accepts(caseName: String, fieldName: String, value: Any?) = acceptFieldValue(caseName, fieldName, value)
            }

            @Nested
            @DisplayName("Поле afterTotalsMsg")
            inner class AfterTotalsMsgFieldTests {
                @ParameterizedTest(name = "{0}")
                @MethodSource("kz.superkassa.tests.api.management.settings.branding.KkmBrandingRegressionTest#validAfterTotalsMsgFieldValues")
                @Severity(SeverityLevel.NORMAL)
                @DisplayName("Принимает допустимые значения")
                fun accepts(caseName: String, fieldName: String, value: Any?) = acceptFieldValue(caseName, fieldName, value)
            }

            @Nested
            @DisplayName("Поле beforeHeaderMsg")
            inner class BeforeHeaderMsgFieldTests {
                @ParameterizedTest(name = "{0}")
                @MethodSource("kz.superkassa.tests.api.management.settings.branding.KkmBrandingRegressionTest#validBeforeHeaderMsgFieldValues")
                @Severity(SeverityLevel.NORMAL)
                @DisplayName("Принимает допустимые значения")
                fun accepts(caseName: String, fieldName: String, value: Any?) = acceptFieldValue(caseName, fieldName, value)
            }

            @Nested
            @DisplayName("Поле beforeItemsMsg")
            inner class BeforeItemsMsgFieldTests {
                @ParameterizedTest(name = "{0}")
                @MethodSource("kz.superkassa.tests.api.management.settings.branding.KkmBrandingRegressionTest#validBeforeItemsMsgFieldValues")
                @Severity(SeverityLevel.NORMAL)
                @DisplayName("Принимает допустимые значения")
                fun accepts(caseName: String, fieldName: String, value: Any?) = acceptFieldValue(caseName, fieldName, value)
            }

            @Nested
            @DisplayName("Поле beforeQrMsg")
            inner class BeforeQrMsgFieldTests {
                @ParameterizedTest(name = "{0}")
                @MethodSource("kz.superkassa.tests.api.management.settings.branding.KkmBrandingRegressionTest#validBeforeQrMsgFieldValues")
                @Severity(SeverityLevel.NORMAL)
                @DisplayName("Принимает допустимые значения")
                fun accepts(caseName: String, fieldName: String, value: Any?) = acceptFieldValue(caseName, fieldName, value)
            }

            @Nested
            @DisplayName("Поле beforeTotalsMsg")
            inner class BeforeTotalsMsgFieldTests {
                @ParameterizedTest(name = "{0}")
                @MethodSource("kz.superkassa.tests.api.management.settings.branding.KkmBrandingRegressionTest#validBeforeTotalsMsgFieldValues")
                @Severity(SeverityLevel.NORMAL)
                @DisplayName("Принимает допустимые значения")
                fun accepts(caseName: String, fieldName: String, value: Any?) = acceptFieldValue(caseName, fieldName, value)
            }

            @Nested
            @DisplayName("Поле footerMsg")
            inner class FooterMsgFieldTests {
                @ParameterizedTest(name = "{0}")
                @MethodSource("kz.superkassa.tests.api.management.settings.branding.KkmBrandingRegressionTest#validFooterMsgFieldValues")
                @Severity(SeverityLevel.NORMAL)
                @DisplayName("Принимает допустимые значения")
                fun accepts(caseName: String, fieldName: String, value: Any?) = acceptFieldValue(caseName, fieldName, value)
            }

            @Nested
            @DisplayName("Поле headerMsg")
            inner class HeaderMsgFieldTests {
                @ParameterizedTest(name = "{0}")
                @MethodSource("kz.superkassa.tests.api.management.settings.branding.KkmBrandingRegressionTest#validHeaderMsgFieldValues")
                @Severity(SeverityLevel.NORMAL)
                @DisplayName("Принимает допустимые значения")
                fun accepts(caseName: String, fieldName: String, value: Any?) = acceptFieldValue(caseName, fieldName, value)
            }

            @Nested
            @DisplayName("Поле customBackgroundColorHex")
            inner class CustomBackgroundColorHexFieldTests {
                @ParameterizedTest(name = "{0}")
                @MethodSource("kz.superkassa.tests.api.management.settings.branding.KkmBrandingRegressionTest#validCustomBackgroundColorHexFieldValues")
                @Severity(SeverityLevel.NORMAL)
                @DisplayName("Принимает допустимые значения")
                fun accepts(caseName: String, fieldName: String, value: Any?) = acceptFieldValue(caseName, fieldName, value)
            }

            @Nested
            @DisplayName("Поле customCardTopBorderColorHex")
            inner class CustomCardTopBorderColorHexFieldTests {
                @ParameterizedTest(name = "{0}")
                @MethodSource("kz.superkassa.tests.api.management.settings.branding.KkmBrandingRegressionTest#validCustomCardTopBorderColorHexFieldValues")
                @Severity(SeverityLevel.NORMAL)
                @DisplayName("Принимает допустимые значения")
                fun accepts(caseName: String, fieldName: String, value: Any?) = acceptFieldValue(caseName, fieldName, value)
            }

            @Nested
            @DisplayName("Поле headerLogoUrl")
            inner class HeaderLogoUrlFieldTests {
                @ParameterizedTest(name = "{0}")
                @MethodSource("kz.superkassa.tests.api.management.settings.branding.KkmBrandingRegressionTest#validHeaderLogoUrlFieldValues")
                @Severity(SeverityLevel.NORMAL)
                @DisplayName("Принимает допустимые значения")
                fun accepts(caseName: String, fieldName: String, value: Any?) = acceptFieldValue(caseName, fieldName, value)
            }
        }
    }

    @Nested
    @ApiRegression
    @DisplayName("Негативные проверки PUT /kkm/{kkmId}/settings/branding")
    inner class NegativeRegressionTests {
        @Nested
        @ApiRegression
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
            }

            @Nested
            @DisplayName("Поле language")
            inner class LanguageFieldTests {
                @ParameterizedTest(name = "{0}")
                @MethodSource("kz.superkassa.tests.api.management.settings.branding.KkmBrandingRegressionTest#invalidLanguageFieldValues")
                @Severity(SeverityLevel.NORMAL)
                @DisplayName("Возвращает 400 для невалидного значения")
                fun rejects(caseName: String, fieldName: String, value: Any?) = rejectFieldValue(caseName, fieldName, value)
            }

            @Nested
            @DisplayName("Поле ofdTicketAds")
            inner class OfdTicketAdsFieldTests {
                @ParameterizedTest(name = "{0}")
                @MethodSource("kz.superkassa.tests.api.management.settings.branding.KkmBrandingRegressionTest#invalidOfdTicketAdsFieldValues")
                @Severity(SeverityLevel.NORMAL)
                @DisplayName("Возвращает 400 для невалидного значения")
                fun rejects(caseName: String, fieldName: String, value: Any?) = rejectFieldValue(caseName, fieldName, value)
            }

            @Nested
            @DisplayName("Поле paperWidthMm")
            inner class PaperWidthMmFieldTests {
                @ParameterizedTest(name = "{0}")
                @MethodSource("kz.superkassa.tests.api.management.settings.branding.KkmBrandingRegressionTest#invalidPaperWidthMmFieldValues")
                @Severity(SeverityLevel.NORMAL)
                @DisplayName("Возвращает 400 для невалидного значения")
                fun rejects(caseName: String, fieldName: String, value: Any?) = rejectFieldValue(caseName, fieldName, value)
            }

            @Nested
            @DisplayName("Поле printOfdTicketAds")
            inner class PrintOfdTicketAdsFieldTests {
                @ParameterizedTest(name = "{0}")
                @MethodSource("kz.superkassa.tests.api.management.settings.branding.KkmBrandingRegressionTest#invalidPrintOfdTicketAdsFieldValues")
                @Severity(SeverityLevel.NORMAL)
                @DisplayName("Возвращает 400 для невалидного значения")
                fun rejects(caseName: String, fieldName: String, value: Any?) = rejectFieldValue(caseName, fieldName, value)
            }

            @Nested
            @DisplayName("Поле themeColor")
            inner class ThemeColorFieldTests {
                @ParameterizedTest(name = "{0}")
                @MethodSource("kz.superkassa.tests.api.management.settings.branding.KkmBrandingRegressionTest#invalidThemeColorFieldValues")
                @Severity(SeverityLevel.NORMAL)
                @DisplayName("Возвращает 400 для невалидного значения")
                fun rejects(caseName: String, fieldName: String, value: Any?) = rejectFieldValue(caseName, fieldName, value)
            }

            @Nested
            @DisplayName("Поле useForceDarkTheme")
            inner class UseForceDarkThemeFieldTests {
                @ParameterizedTest(name = "{0}")
                @MethodSource("kz.superkassa.tests.api.management.settings.branding.KkmBrandingRegressionTest#invalidUseForceDarkThemeFieldValues")
                @Severity(SeverityLevel.NORMAL)
                @DisplayName("Возвращает 400 для невалидного значения")
                fun rejects(caseName: String, fieldName: String, value: Any?) = rejectFieldValue(caseName, fieldName, value)
            }

            @Nested
            @DisplayName("Поле afterHeaderMsg")
            inner class AfterHeaderMsgFieldTests {
                @ParameterizedTest(name = "{0}")
                @MethodSource("kz.superkassa.tests.api.management.settings.branding.KkmBrandingRegressionTest#invalidAfterHeaderMsgFieldValues")
                @Severity(SeverityLevel.NORMAL)
                @DisplayName("Возвращает 400 для невалидного значения")
                fun rejects(caseName: String, fieldName: String, value: Any?) = rejectFieldValue(caseName, fieldName, value)
            }

            @Nested
            @DisplayName("Поле afterItemsMsg")
            inner class AfterItemsMsgFieldTests {
                @ParameterizedTest(name = "{0}")
                @MethodSource("kz.superkassa.tests.api.management.settings.branding.KkmBrandingRegressionTest#invalidAfterItemsMsgFieldValues")
                @Severity(SeverityLevel.NORMAL)
                @DisplayName("Возвращает 400 для невалидного значения")
                fun rejects(caseName: String, fieldName: String, value: Any?) = rejectFieldValue(caseName, fieldName, value)
            }

            @Nested
            @DisplayName("Поле afterTotalsMsg")
            inner class AfterTotalsMsgFieldTests {
                @ParameterizedTest(name = "{0}")
                @MethodSource("kz.superkassa.tests.api.management.settings.branding.KkmBrandingRegressionTest#invalidAfterTotalsMsgFieldValues")
                @Severity(SeverityLevel.NORMAL)
                @DisplayName("Возвращает 400 для невалидного значения")
                fun rejects(caseName: String, fieldName: String, value: Any?) = rejectFieldValue(caseName, fieldName, value)
            }

            @Nested
            @DisplayName("Поле beforeHeaderMsg")
            inner class BeforeHeaderMsgFieldTests {
                @ParameterizedTest(name = "{0}")
                @MethodSource("kz.superkassa.tests.api.management.settings.branding.KkmBrandingRegressionTest#invalidBeforeHeaderMsgFieldValues")
                @Severity(SeverityLevel.NORMAL)
                @DisplayName("Возвращает 400 для невалидного значения")
                fun rejects(caseName: String, fieldName: String, value: Any?) = rejectFieldValue(caseName, fieldName, value)
            }

            @Nested
            @DisplayName("Поле beforeItemsMsg")
            inner class BeforeItemsMsgFieldTests {
                @ParameterizedTest(name = "{0}")
                @MethodSource("kz.superkassa.tests.api.management.settings.branding.KkmBrandingRegressionTest#invalidBeforeItemsMsgFieldValues")
                @Severity(SeverityLevel.NORMAL)
                @DisplayName("Возвращает 400 для невалидного значения")
                fun rejects(caseName: String, fieldName: String, value: Any?) = rejectFieldValue(caseName, fieldName, value)
            }

            @Nested
            @DisplayName("Поле beforeQrMsg")
            inner class BeforeQrMsgFieldTests {
                @ParameterizedTest(name = "{0}")
                @MethodSource("kz.superkassa.tests.api.management.settings.branding.KkmBrandingRegressionTest#invalidBeforeQrMsgFieldValues")
                @Severity(SeverityLevel.NORMAL)
                @DisplayName("Возвращает 400 для невалидного значения")
                fun rejects(caseName: String, fieldName: String, value: Any?) = rejectFieldValue(caseName, fieldName, value)
            }

            @Nested
            @DisplayName("Поле beforeTotalsMsg")
            inner class BeforeTotalsMsgFieldTests {
                @ParameterizedTest(name = "{0}")
                @MethodSource("kz.superkassa.tests.api.management.settings.branding.KkmBrandingRegressionTest#invalidBeforeTotalsMsgFieldValues")
                @Severity(SeverityLevel.NORMAL)
                @DisplayName("Возвращает 400 для невалидного значения")
                fun rejects(caseName: String, fieldName: String, value: Any?) = rejectFieldValue(caseName, fieldName, value)
            }

            @Nested
            @DisplayName("Поле footerMsg")
            inner class FooterMsgFieldTests {
                @ParameterizedTest(name = "{0}")
                @MethodSource("kz.superkassa.tests.api.management.settings.branding.KkmBrandingRegressionTest#invalidFooterMsgFieldValues")
                @Severity(SeverityLevel.NORMAL)
                @DisplayName("Возвращает 400 для невалидного значения")
                fun rejects(caseName: String, fieldName: String, value: Any?) = rejectFieldValue(caseName, fieldName, value)
            }

            @Nested
            @DisplayName("Поле headerMsg")
            inner class HeaderMsgFieldTests {
                @ParameterizedTest(name = "{0}")
                @MethodSource("kz.superkassa.tests.api.management.settings.branding.KkmBrandingRegressionTest#invalidHeaderMsgFieldValues")
                @Severity(SeverityLevel.NORMAL)
                @DisplayName("Возвращает 400 для невалидного значения")
                fun rejects(caseName: String, fieldName: String, value: Any?) = rejectFieldValue(caseName, fieldName, value)
            }

            @Nested
            @DisplayName("Поле customBackgroundColorHex")
            inner class CustomBackgroundColorHexFieldTests {
                @ParameterizedTest(name = "{0}")
                @MethodSource("kz.superkassa.tests.api.management.settings.branding.KkmBrandingRegressionTest#invalidCustomBackgroundColorHexFieldValues")
                @Severity(SeverityLevel.NORMAL)
                @DisplayName("Возвращает 400 для невалидного значения")
                fun rejects(caseName: String, fieldName: String, value: Any?) = rejectFieldValue(caseName, fieldName, value)
            }

            @Nested
            @DisplayName("Поле customCardTopBorderColorHex")
            inner class CustomCardTopBorderColorHexFieldTests {
                @ParameterizedTest(name = "{0}")
                @MethodSource("kz.superkassa.tests.api.management.settings.branding.KkmBrandingRegressionTest#invalidCustomCardTopBorderColorHexFieldValues")
                @Severity(SeverityLevel.NORMAL)
                @DisplayName("Возвращает 400 для невалидного значения")
                fun rejects(caseName: String, fieldName: String, value: Any?) = rejectFieldValue(caseName, fieldName, value)
            }

            @Nested
            @DisplayName("Поле headerLogoUrl")
            inner class HeaderLogoUrlFieldTests {
                @ParameterizedTest(name = "{0}")
                @MethodSource("kz.superkassa.tests.api.management.settings.branding.KkmBrandingRegressionTest#invalidHeaderLogoUrlFieldValues")
                @Severity(SeverityLevel.NORMAL)
                @DisplayName("Возвращает 400 для невалидного значения")
                fun rejects(caseName: String, fieldName: String, value: Any?) = rejectFieldValue(caseName, fieldName, value)
            }
        }

        @Nested
        @ApiRegression
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
        "themeColor" to "indigo",
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

    private fun acceptFieldValue(caseName: String, fieldName: String, value: Any?) {
        reportStep("Проверяем допустимое значение поля $fieldName: $caseName") {
            updateBranding(validBrandingBody() + (fieldName to value))
        }
    }

    private fun rejectFieldValue(caseName: String, fieldName: String, value: Any?) {
        val body = if (value === OmittedFieldValue) {
            validBrandingBody() - fieldName
        } else {
            validBrandingBody() + (fieldName to value)
        }
        putInvalidBody(body, caseName)
    }

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

    private object OmittedFieldValue

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

        val MESSAGE_REQUEST_FIELDS = listOf(
            "afterHeaderMsg",
            "afterItemsMsg",
            "afterTotalsMsg",
            "beforeHeaderMsg",
            "beforeItemsMsg",
            "beforeQrMsg",
            "beforeTotalsMsg",
            "footerMsg",
            "headerMsg",
        )

        val HEX_COLOR_REQUEST_FIELDS = listOf(
            "customBackgroundColorHex",
            "customCardTopBorderColorHex",
        )

        val OPTIONAL_STRING_REQUEST_FIELDS = MESSAGE_REQUEST_FIELDS +
            HEX_COLOR_REQUEST_FIELDS +
            "headerLogoUrl"

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
        fun validLanguageFieldValues(): Stream<Arguments> = validFieldValues("language")

        @JvmStatic
        fun validOfdTicketAdsFieldValues(): Stream<Arguments> = validFieldValues("ofdTicketAds")

        @JvmStatic
        fun validPaperWidthMmFieldValues(): Stream<Arguments> = validFieldValues("paperWidthMm")

        @JvmStatic
        fun validPrintOfdTicketAdsFieldValues(): Stream<Arguments> = validFieldValues("printOfdTicketAds")

        @JvmStatic
        fun validThemeColorFieldValues(): Stream<Arguments> = validFieldValues("themeColor")

        @JvmStatic
        fun validUseForceDarkThemeFieldValues(): Stream<Arguments> = validFieldValues("useForceDarkTheme")

        @JvmStatic
        fun validAfterHeaderMsgFieldValues(): Stream<Arguments> = validFieldValues("afterHeaderMsg")

        @JvmStatic
        fun validAfterItemsMsgFieldValues(): Stream<Arguments> = validFieldValues("afterItemsMsg")

        @JvmStatic
        fun validAfterTotalsMsgFieldValues(): Stream<Arguments> = validFieldValues("afterTotalsMsg")

        @JvmStatic
        fun validBeforeHeaderMsgFieldValues(): Stream<Arguments> = validFieldValues("beforeHeaderMsg")

        @JvmStatic
        fun validBeforeItemsMsgFieldValues(): Stream<Arguments> = validFieldValues("beforeItemsMsg")

        @JvmStatic
        fun validBeforeQrMsgFieldValues(): Stream<Arguments> = validFieldValues("beforeQrMsg")

        @JvmStatic
        fun validBeforeTotalsMsgFieldValues(): Stream<Arguments> = validFieldValues("beforeTotalsMsg")

        @JvmStatic
        fun validFooterMsgFieldValues(): Stream<Arguments> = validFieldValues("footerMsg")

        @JvmStatic
        fun validHeaderMsgFieldValues(): Stream<Arguments> = validFieldValues("headerMsg")

        @JvmStatic
        fun validCustomBackgroundColorHexFieldValues(): Stream<Arguments> =
            validFieldValues("customBackgroundColorHex")

        @JvmStatic
        fun validCustomCardTopBorderColorHexFieldValues(): Stream<Arguments> =
            validFieldValues("customCardTopBorderColorHex")

        @JvmStatic
        fun validHeaderLogoUrlFieldValues(): Stream<Arguments> = validFieldValues("headerLogoUrl")

        @JvmStatic
        fun invalidLanguageFieldValues(): Stream<Arguments> = invalidFieldValues("language")

        @JvmStatic
        fun invalidOfdTicketAdsFieldValues(): Stream<Arguments> = invalidFieldValues("ofdTicketAds")

        @JvmStatic
        fun invalidPaperWidthMmFieldValues(): Stream<Arguments> = invalidFieldValues("paperWidthMm")

        @JvmStatic
        fun invalidPrintOfdTicketAdsFieldValues(): Stream<Arguments> = invalidFieldValues("printOfdTicketAds")

        @JvmStatic
        fun invalidThemeColorFieldValues(): Stream<Arguments> = invalidFieldValues("themeColor")

        @JvmStatic
        fun invalidUseForceDarkThemeFieldValues(): Stream<Arguments> = invalidFieldValues("useForceDarkTheme")

        @JvmStatic
        fun invalidAfterHeaderMsgFieldValues(): Stream<Arguments> = invalidFieldValues("afterHeaderMsg")

        @JvmStatic
        fun invalidAfterItemsMsgFieldValues(): Stream<Arguments> = invalidFieldValues("afterItemsMsg")

        @JvmStatic
        fun invalidAfterTotalsMsgFieldValues(): Stream<Arguments> = invalidFieldValues("afterTotalsMsg")

        @JvmStatic
        fun invalidBeforeHeaderMsgFieldValues(): Stream<Arguments> = invalidFieldValues("beforeHeaderMsg")

        @JvmStatic
        fun invalidBeforeItemsMsgFieldValues(): Stream<Arguments> = invalidFieldValues("beforeItemsMsg")

        @JvmStatic
        fun invalidBeforeQrMsgFieldValues(): Stream<Arguments> = invalidFieldValues("beforeQrMsg")

        @JvmStatic
        fun invalidBeforeTotalsMsgFieldValues(): Stream<Arguments> = invalidFieldValues("beforeTotalsMsg")

        @JvmStatic
        fun invalidFooterMsgFieldValues(): Stream<Arguments> = invalidFieldValues("footerMsg")

        @JvmStatic
        fun invalidHeaderMsgFieldValues(): Stream<Arguments> = invalidFieldValues("headerMsg")

        @JvmStatic
        fun invalidCustomBackgroundColorHexFieldValues(): Stream<Arguments> =
            invalidFieldValues("customBackgroundColorHex")

        @JvmStatic
        fun invalidCustomCardTopBorderColorHexFieldValues(): Stream<Arguments> =
            invalidFieldValues("customCardTopBorderColorHex")

        @JvmStatic
        fun invalidHeaderLogoUrlFieldValues(): Stream<Arguments> = invalidFieldValues("headerLogoUrl")

        private fun validFieldValues(fieldName: String): Stream<Arguments> {
            val fieldValues = listOf(
                validRequiredFieldValues(),
                validMessageFieldValues(),
                validHexColorValues(),
                validHeaderLogoUrls(),
            ).stream()
                .flatMap { it }
                .filter { arguments -> arguments.get()[1] == fieldName }

            return if (fieldName in OPTIONAL_STRING_REQUEST_FIELDS) {
                Stream.concat(
                    Stream.of(Arguments.of("nullable optional-поле $fieldName содержит null", fieldName, null)),
                    fieldValues,
                )
            } else {
                fieldValues
            }
        }

        private fun invalidFieldValues(fieldName: String): Stream<Arguments> = listOf(
            requiredRequestFields(),
            nullRequiredRequestFields(),
            invalidRequiredFieldTypes(),
            invalidOptionalFieldTypes(),
            invalidOfdTicketAdsItems(),
            invalidOfdTicketAdsValues(),
            invalidMessageFieldValues(),
            invalidHexColorValues(),
            invalidHeaderLogoUrls(),
            invalidLanguageValues(),
            invalidPaperWidthBoundaryValues(),
            invalidThemeColorValues(),
        ).stream()
            .flatMap { it }
            .filter { arguments -> arguments.get()[1] == fieldName }

        @JvmStatic
        fun requiredRequestFields(): Stream<Arguments> = REQUIRED_REQUEST_FIELDS.stream().map { fieldName ->
            Arguments.of("обязательное поле $fieldName отсутствует", fieldName, OmittedFieldValue)
        }

        @JvmStatic
        fun nullRequiredRequestFields(): Stream<Arguments> = REQUIRED_REQUEST_FIELDS.stream().map { fieldName ->
            Arguments.of("обязательное поле $fieldName содержит null", fieldName, null)
        }

        @JvmStatic
        fun invalidRequiredFieldTypes(): Stream<Arguments> = Stream.of(
            Arguments.of("поле language имеет тип Number вместо String", "language", 123),
            Arguments.of("поле language имеет тип Boolean вместо String", "language", true),
            Arguments.of("поле language имеет тип Object вместо String", "language", mapOf("value" to "RU")),
            Arguments.of("поле language имеет тип Array вместо String", "language", listOf("RU")),
            Arguments.of("поле ofdTicketAds имеет тип String вместо Array", "ofdTicketAds", "Superkassa"),
            Arguments.of("поле ofdTicketAds имеет тип Number вместо Array", "ofdTicketAds", 123),
            Arguments.of("поле ofdTicketAds имеет тип Boolean вместо Array", "ofdTicketAds", true),
            Arguments.of(
                "поле ofdTicketAds имеет тип Object вместо Array",
                "ofdTicketAds",
                mapOf("text" to "Superkassa"),
            ),
            Arguments.of("поле paperWidthMm имеет тип String вместо Integer", "paperWidthMm", "80"),
            Arguments.of("поле paperWidthMm имеет дробный тип Number вместо Integer", "paperWidthMm", 80.5),
            Arguments.of("поле paperWidthMm имеет тип Boolean вместо Integer", "paperWidthMm", true),
            Arguments.of("поле paperWidthMm имеет тип Object вместо Integer", "paperWidthMm", mapOf("value" to 80)),
            Arguments.of("поле paperWidthMm имеет тип Array вместо Integer", "paperWidthMm", listOf(80)),
            Arguments.of("поле printOfdTicketAds имеет тип String вместо Boolean", "printOfdTicketAds", "false"),
            Arguments.of("поле printOfdTicketAds имеет значение Number 1 вместо Boolean", "printOfdTicketAds", 1),
            Arguments.of("поле printOfdTicketAds имеет значение Number 0 вместо Boolean", "printOfdTicketAds", 0),
            Arguments.of(
                "поле printOfdTicketAds имеет тип Object вместо Boolean",
                "printOfdTicketAds",
                mapOf("value" to false),
            ),
            Arguments.of("поле printOfdTicketAds имеет тип Array вместо Boolean", "printOfdTicketAds", listOf(false)),
            Arguments.of("поле themeColor имеет тип Number вместо String", "themeColor", 123),
            Arguments.of("поле themeColor имеет тип Boolean вместо String", "themeColor", true),
            Arguments.of(
                "поле themeColor имеет тип Object вместо String",
                "themeColor",
                mapOf("value" to "indigo"),
            ),
            Arguments.of("поле themeColor имеет тип Array вместо String", "themeColor", listOf("indigo")),
            Arguments.of("поле useForceDarkTheme имеет тип String вместо Boolean", "useForceDarkTheme", "false"),
            Arguments.of("поле useForceDarkTheme имеет значение Number 1 вместо Boolean", "useForceDarkTheme", 1),
            Arguments.of("поле useForceDarkTheme имеет значение Number 0 вместо Boolean", "useForceDarkTheme", 0),
            Arguments.of(
                "поле useForceDarkTheme имеет тип Object вместо Boolean",
                "useForceDarkTheme",
                mapOf("value" to false),
            ),
            Arguments.of("поле useForceDarkTheme имеет тип Array вместо Boolean", "useForceDarkTheme", listOf(false)),
        )

        @JvmStatic
        fun invalidOptionalFieldTypes(): Stream<Arguments> = OPTIONAL_STRING_REQUEST_FIELDS.flatMap { fieldName ->
            listOf(
                Arguments.of("optional-поле $fieldName имеет тип Number вместо String", fieldName, 123),
                Arguments.of("optional-поле $fieldName имеет тип Boolean вместо String", fieldName, true),
                Arguments.of(
                    "optional-поле $fieldName имеет тип Object вместо String",
                    fieldName,
                    mapOf("value" to "text"),
                ),
                Arguments.of("optional-поле $fieldName имеет тип Array вместо String", fieldName, listOf("text")),
            )
        }.stream()

        @JvmStatic
        fun validRequiredFieldValues(): Stream<Arguments> = listOf(
            Arguments.of("language содержит допустимое значение RU", "language", "RU"),
            Arguments.of("language содержит допустимое значение KK", "language", "KK"),
            Arguments.of("language содержит допустимое значение MIXED", "language", "MIXED"),
            Arguments.of("ofdTicketAds содержит пустой массив", "ofdTicketAds", emptyList<String>()),
            Arguments.of("ofdTicketAds содержит один текст", "ofdTicketAds", listOf("Superkassa")),
            Arguments.of("ofdTicketAds содержит несколько текстов", "ofdTicketAds", listOf("Первый", "Второй")),
            Arguments.of("ofdTicketAds содержит пустую строку", "ofdTicketAds", listOf("")),
            Arguments.of("элемент ofdTicketAds содержит 1 Unicode-символ", "ofdTicketAds", listOf("Ж")),
            Arguments.of(
                "элемент ofdTicketAds содержит 1023 Unicode-символа",
                "ofdTicketAds",
                listOf(messageOfLength(1023)),
            ),
            Arguments.of(
                "элемент ofdTicketAds содержит максимально допустимые 1024 Unicode-символа",
                "ofdTicketAds",
                listOf(messageOfLength(1024)),
            ),
            Arguments.of(
                "элемент ofdTicketAds содержит печатные Unicode-символы",
                "ofdTicketAds",
                listOf("Қазақша 🧾 <>&"),
            ),
            Arguments.of(
                "элемент ofdTicketAds содержит 1024 Unicode-символа вне BMP",
                "ofdTicketAds",
                listOf(emojiMessageOfLength(1024)),
            ),
            Arguments.of(
                "элемент ofdTicketAds содержит текст с пробелами в начале и конце",
                "ofdTicketAds",
                listOf(" Реклама "),
            ),
            Arguments.of(
                "элемент ofdTicketAds содержит максимально допустимые 4 строки, разделённые символом \\n",
                "ofdTicketAds",
                listOf(fourLineMessage()),
            ),
            Arguments.of("paperWidthMm содержит допустимую границу 58", "paperWidthMm", 58),
            Arguments.of("paperWidthMm содержит допустимую границу 80", "paperWidthMm", 80),
            Arguments.of("printOfdTicketAds содержит true", "printOfdTicketAds", true),
            Arguments.of("printOfdTicketAds содержит false", "printOfdTicketAds", false),
            Arguments.of("themeColor содержит допустимое значение indigo", "themeColor", "indigo"),
            Arguments.of("themeColor содержит допустимое значение teal", "themeColor", "teal"),
            Arguments.of("themeColor содержит допустимое значение green", "themeColor", "green"),
            Arguments.of("themeColor содержит допустимое значение blue", "themeColor", "blue"),
            Arguments.of("themeColor содержит допустимое значение orange", "themeColor", "orange"),
            Arguments.of("themeColor содержит допустимое значение rose", "themeColor", "rose"),
            Arguments.of("useForceDarkTheme содержит true", "useForceDarkTheme", true),
            Arguments.of("useForceDarkTheme содержит false", "useForceDarkTheme", false),
        ).stream()

        @JvmStatic
        fun validMessageFieldValues(): Stream<Arguments> = MESSAGE_REQUEST_FIELDS.flatMap { fieldName ->
            listOf(
                Arguments.of("поле $fieldName содержит пустую строку", fieldName, ""),
                Arguments.of("поле $fieldName содержит 1 Unicode-символ", fieldName, "Ж"),
                Arguments.of("поле $fieldName содержит 255 Unicode-символов", fieldName, messageOfLength(255)),
                Arguments.of(
                    "поле $fieldName содержит максимально допустимые 256 Unicode-символов",
                    fieldName,
                    messageOfLength(256),
                ),
                Arguments.of("поле $fieldName содержит печатные Unicode-символы", fieldName, "Қазақша 🧾 <>&"),
                Arguments.of(
                    "поле $fieldName содержит 256 Unicode-символов вне BMP",
                    fieldName,
                    emojiMessageOfLength(256),
                ),
                Arguments.of("поле $fieldName содержит текст с пробелами в начале и конце", fieldName, " Текст "),
                Arguments.of(
                    "поле $fieldName содержит 3 строки, разделённые символом \\n",
                    fieldName,
                    "Первая\nВторая\nТретья",
                ),
                Arguments.of(
                    "поле $fieldName содержит максимально допустимые 4 строки, разделённые символом \\n",
                    fieldName,
                    fourLineMessage(),
                ),
            )
        }.stream()

        @JvmStatic
        fun validHexColorValues(): Stream<Arguments> = HEX_COLOR_REQUEST_FIELDS.flatMap { fieldName ->
            listOf(
                Arguments.of("поле $fieldName содержит минимальный HEX-цвет #000000", fieldName, "#000000"),
                Arguments.of("поле $fieldName содержит максимальный HEX-цвет #FFFFFF", fieldName, "#FFFFFF"),
                Arguments.of("поле $fieldName содержит HEX-цифры в смешанном регистре", fieldName, "#a1B2c3"),
            )
        }.stream()

        @JvmStatic
        fun validHeaderLogoUrls(): Stream<Arguments> = Stream.of(
            Arguments.of("headerLogoUrl содержит пустую строку", "headerLogoUrl", ""),
            Arguments.of(
                "headerLogoUrl содержит абсолютный HTTPS URL",
                "headerLogoUrl",
                "https://example.com/logo.png",
            ),
            Arguments.of(
                "headerLogoUrl содержит абсолютный HTTPS URL с query-параметрами",
                "headerLogoUrl",
                "https://example.com/logo.png?size=large&lang=ru",
            ),
            Arguments.of(
                "headerLogoUrl содержит максимально допустимые 2048 символов",
                "headerLogoUrl",
                httpsUrlOfLength(2048),
            ),
        )

        @JvmStatic
        fun invalidOfdTicketAdsItems(): Stream<Arguments> = Stream.of(
            Arguments.of("элемент ofdTicketAds содержит null вместо String", "ofdTicketAds", listOf(null)),
            Arguments.of("элемент ofdTicketAds имеет тип Number вместо String", "ofdTicketAds", listOf(123)),
            Arguments.of("элемент ofdTicketAds имеет тип Boolean вместо String", "ofdTicketAds", listOf(true)),
            Arguments.of(
                "элемент ofdTicketAds имеет тип Object вместо String",
                "ofdTicketAds",
                listOf(mapOf("text" to "Superkassa")),
            ),
            Arguments.of(
                "элемент ofdTicketAds имеет тип Array вместо String",
                "ofdTicketAds",
                listOf(listOf("Superkassa")),
            ),
        )

        @JvmStatic
        fun invalidOfdTicketAdsValues(): Stream<Arguments> = Stream.of(
            Arguments.of("элемент ofdTicketAds содержит только пробелы", "ofdTicketAds", listOf("   ")),
            Arguments.of(
                "элемент ofdTicketAds содержит 1025 Unicode-символов",
                "ofdTicketAds",
                listOf(messageOfLength(1025)),
            ),
            Arguments.of(
                "элемент ofdTicketAds содержит 1025 Unicode-символов вне BMP",
                "ofdTicketAds",
                listOf(emojiMessageOfLength(1025)),
            ),
            Arguments.of(
                "элемент ofdTicketAds содержит 5 строк, разделённых символом \\n",
                "ofdTicketAds",
                listOf(fiveLineMessage()),
            ),
            Arguments.of(
                "элемент ofdTicketAds содержит запрещённую табуляцию",
                "ofdTicketAds",
                listOf("Текст\tрекламы"),
            ),
            Arguments.of(
                "элемент ofdTicketAds содержит запрещённый возврат каретки",
                "ofdTicketAds",
                listOf("Текст\rрекламы"),
            ),
            Arguments.of(
                "элемент ofdTicketAds содержит управляющий символ NUL",
                "ofdTicketAds",
                listOf("Текст\u0000рекламы"),
            ),
        )

        @JvmStatic
        fun invalidMessageFieldValues(): Stream<Arguments> = MESSAGE_REQUEST_FIELDS.flatMap { fieldName ->
            listOf(
                Arguments.of("поле $fieldName содержит только пробелы", fieldName, "   "),
                Arguments.of("поле $fieldName содержит 257 Unicode-символов", fieldName, messageOfLength(257)),
                Arguments.of(
                    "поле $fieldName содержит 257 Unicode-символов вне BMP",
                    fieldName,
                    emojiMessageOfLength(257),
                ),
                Arguments.of(
                    "поле $fieldName содержит 5 строк, разделённых символом \\n",
                    fieldName,
                    fiveLineMessage(),
                ),
                Arguments.of("поле $fieldName содержит запрещённую табуляцию", fieldName, "Текст\tчека"),
                Arguments.of("поле $fieldName содержит запрещённый возврат каретки", fieldName, "Текст\rчека"),
                Arguments.of("поле $fieldName содержит управляющий символ NUL", fieldName, "Текст\u0000чека"),
            )
        }.stream()

        @JvmStatic
        fun invalidHexColorValues(): Stream<Arguments> = HEX_COLOR_REQUEST_FIELDS.flatMap { fieldName ->
            listOf(
                Arguments.of("поле $fieldName содержит пустую строку", fieldName, ""),
                Arguments.of("поле $fieldName содержит только пробелы", fieldName, "   "),
                Arguments.of("поле $fieldName содержит сокращённый формат #RGB", fieldName, "#ABC"),
                Arguments.of("поле $fieldName содержит прозрачность #RRGGBBAA", fieldName, "#AABBCCDD"),
                Arguments.of("поле $fieldName содержит CSS-название цвета", fieldName, "red"),
                Arguments.of("поле $fieldName не содержит символ #", fieldName, "AABBCC"),
                Arguments.of("поле $fieldName содержит символы вне HEX-диапазона", fieldName, "#GGHHII"),
                Arguments.of("поле $fieldName содержит пробел в начале", fieldName, " #AABBCC"),
                Arguments.of("поле $fieldName содержит пробел в конце", fieldName, "#AABBCC "),
                Arguments.of("поле $fieldName содержит пробел внутри", fieldName, "#AA BBCC"),
            )
        }.stream()

        @JvmStatic
        fun invalidHeaderLogoUrls(): Stream<Arguments> = Stream.of(
            Arguments.of("headerLogoUrl содержит только пробелы", "headerLogoUrl", "   "),
            Arguments.of("headerLogoUrl содержит 2049 символов", "headerLogoUrl", httpsUrlOfLength(2049)),
            Arguments.of(
                "headerLogoUrl использует запрещённую схему http",
                "headerLogoUrl",
                "http://example.com/logo.png",
            ),
            Arguments.of(
                "headerLogoUrl использует запрещённую схему ftp",
                "headerLogoUrl",
                "ftp://example.com/logo.png",
            ),
            Arguments.of("headerLogoUrl содержит относительный URL", "headerLogoUrl", "/images/logo.png"),
            Arguments.of("headerLogoUrl не содержит доменное имя", "headerLogoUrl", "https:///logo.png"),
            Arguments.of(
                "headerLogoUrl содержит пробел в начале",
                "headerLogoUrl",
                " https://example.com/logo.png",
            ),
            Arguments.of(
                "headerLogoUrl содержит пробел в конце",
                "headerLogoUrl",
                "https://example.com/logo.png ",
            ),
            Arguments.of(
                "headerLogoUrl содержит пробел внутри",
                "headerLogoUrl",
                "https://example.com/logo image.png",
            ),
            Arguments.of(
                "headerLogoUrl содержит логин и пароль",
                "headerLogoUrl",
                "https://user:password@example.com/logo.png",
            ),
            Arguments.of(
                "headerLogoUrl содержит запрещённый URL-фрагмент",
                "headerLogoUrl",
                "https://example.com/logo.png#preview",
            ),
            Arguments.of("headerLogoUrl содержит строку, не являющуюся URL", "headerLogoUrl", "not-a-url"),
        )

        @JvmStatic
        fun invalidLanguageValues(): Stream<Arguments> = Stream.of(
            Arguments.of("language содержит пустую строку вне Swagger-enum", "language", ""),
            Arguments.of("language содержит только пробелы вне Swagger-enum", "language", "   "),
            Arguments.of("language содержит значение UNKNOWN вне Swagger-enum", "language", "UNKNOWN"),
            Arguments.of("language содержит значение mixed в неправильном регистре", "language", "mixed"),
        )

        @JvmStatic
        fun invalidPaperWidthBoundaryValues(): Stream<Arguments> = Stream.of(
            Arguments.of("paperWidthMm меньше допустимой границы 58: 57", "paperWidthMm", 57),
            Arguments.of("paperWidthMm больше допустимой границы 58: 59", "paperWidthMm", 59),
            Arguments.of("paperWidthMm меньше допустимой границы 80: 79", "paperWidthMm", 79),
            Arguments.of("paperWidthMm больше допустимой границы 80: 81", "paperWidthMm", 81),
            Arguments.of("paperWidthMm содержит нулевое значение", "paperWidthMm", 0),
            Arguments.of("paperWidthMm содержит отрицательное значение", "paperWidthMm", -1),
        )

        @JvmStatic
        fun invalidThemeColorValues(): Stream<Arguments> = Stream.of(
            Arguments.of("themeColor содержит пустую строку", "themeColor", ""),
            Arguments.of("themeColor содержит только пробелы", "themeColor", "   "),
            Arguments.of("themeColor содержит HEX-код вместо названия темы", "themeColor", "#FFFFFF"),
            Arguments.of("themeColor содержит значение UNKNOWN", "themeColor", "UNKNOWN"),
            Arguments.of("themeColor содержит INDIGO в неправильном регистре", "themeColor", "INDIGO"),
        )

        private fun messageOfLength(length: Int): String = "Ж".repeat(length)

        private fun emojiMessageOfLength(length: Int): String = "🧾".repeat(length)

        private fun fourLineMessage(): String = "Первая\nВторая\nТретья\nЧетвёртая"

        private fun fiveLineMessage(): String = "${fourLineMessage()}\nПятая"

        private fun httpsUrlOfLength(length: Int): String {
            val prefix = "https://example.com/"
            return prefix + "a".repeat(length - prefix.length)
        }
    }
}
