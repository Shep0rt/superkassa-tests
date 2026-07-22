package kz.superkassa.tests.api.management.details

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
import org.assertj.core.api.SoftAssertions
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.util.UUID

@ApiRegression
@Feature("API")
@Story("GET /kkm/{kkmId}")
@Owner("Pavel Michka")
@DisplayName("GET /kkm/{kkmId}: регрессионные проверки получения информации о ККМ")
@Suppress("NonAsciiCharacters")
class KkmDetailsRegressionTest : BaseApiTest() {
    @Nested
    @ApiRegression
    @Feature("API")
    @Story("GET /kkm/{kkmId}")
    @Owner("Pavel Michka")
    @DisplayName("Позитивные проверки GET /kkm/{kkmId}")
    inner class PositiveRegressionTests {
        private lateinit var kkmId: String

        @BeforeEach
        fun `Получаем контрольную ККМ`() {
            kkmId = firstKkmIdOrSkip()
        }

        @Test
        @Severity(SeverityLevel.CRITICAL)
        @DisplayName("Метод GET /kkm/{kkmId} возвращает поля ожидаемых типов")
        fun shouldReturnExpectedFieldTypes() {
            val response = getKkmDetails(kkmId)

            SoftAssertions().apply {
                assertFieldType(
                    this,
                    response,
                    "autoCloseShift",
                    "autoCloseShift",
                    Boolean::class.javaObjectType,
                    KKM_RESPONSE_SCHEMA
                )
                assertFieldType(
                    this,
                    response,
                    "createdAt",
                    "createdAt",
                    Long::class.javaObjectType,
                    KKM_RESPONSE_SCHEMA
                )
                assertFieldType(this, response, "kkmId", "kkmId", String::class.java, KKM_RESPONSE_SCHEMA)
                assertFieldType(this, response, "mode", "mode", String::class.java, KKM_RESPONSE_SCHEMA)
                assertFieldType(this, response, "state", "state", String::class.java, KKM_RESPONSE_SCHEMA)
                assertFieldType(
                    this,
                    response,
                    "updatedAt",
                    "updatedAt",
                    Long::class.javaObjectType,
                    KKM_RESPONSE_SCHEMA
                )

                assertOptionalFieldType(
                    this,
                    response,
                    "autonomousSince",
                    "autonomousSince",
                    Long::class.javaObjectType,
                    KKM_RESPONSE_SCHEMA,
                )
                assertOptionalFieldType(this, response, "branding", "branding", Map::class.java, KKM_RESPONSE_SCHEMA)
                assertOptionalFieldType(
                    this,
                    response,
                    "defaultVatGroup",
                    "defaultVatGroup",
                    String::class.java,
                    KKM_RESPONSE_SCHEMA
                )
                assertOptionalFieldType(
                    this,
                    response,
                    "factoryNumber",
                    "factoryNumber",
                    String::class.java,
                    KKM_RESPONSE_SCHEMA
                )
                assertOptionalFieldType(this, response, "kkmKgdId", "kkmKgdId", String::class.java, KKM_RESPONSE_SCHEMA)
                assertOptionalFieldType(
                    this,
                    response,
                    "lastFiscalHashBase64",
                    "lastFiscalHashBase64",
                    String::class.java,
                    KKM_RESPONSE_SCHEMA,
                )
                assertOptionalFieldType(
                    this,
                    response,
                    "lastReceiptNo",
                    "lastReceiptNo",
                    Int::class.javaObjectType,
                    KKM_RESPONSE_SCHEMA
                )
                assertOptionalFieldType(
                    this,
                    response,
                    "lastShiftNo",
                    "lastShiftNo",
                    Int::class.javaObjectType,
                    KKM_RESPONSE_SCHEMA
                )
                assertOptionalFieldType(
                    this,
                    response,
                    "lastZReportNo",
                    "lastZReportNo",
                    Int::class.javaObjectType,
                    KKM_RESPONSE_SCHEMA
                )
                assertOptionalFieldType(
                    this,
                    response,
                    "manufactureYear",
                    "manufactureYear",
                    Int::class.javaObjectType,
                    KKM_RESPONSE_SCHEMA,
                )
                assertOptionalFieldType(
                    this,
                    response,
                    "ofdEnvironment",
                    "ofdEnvironment",
                    String::class.java,
                    KKM_RESPONSE_SCHEMA
                )
                assertOptionalFieldType(this, response, "ofdId", "ofdId", String::class.java, KKM_RESPONSE_SCHEMA)
                assertOptionalFieldType(
                    this,
                    response,
                    "ofdServiceInfo",
                    "ofdServiceInfo",
                    Map::class.java,
                    KKM_RESPONSE_SCHEMA
                )
                assertOptionalFieldType(
                    this,
                    response,
                    "ofdSystemId",
                    "ofdSystemId",
                    String::class.java,
                    KKM_RESPONSE_SCHEMA
                )
                assertOptionalFieldType(
                    this,
                    response,
                    "taxRegime",
                    "taxRegime",
                    String::class.java,
                    KKM_RESPONSE_SCHEMA
                )
                assertOptionalFieldType(
                    this,
                    response,
                    "tokenUpdatedAt",
                    "tokenUpdatedAt",
                    Long::class.javaObjectType,
                    KKM_RESPONSE_SCHEMA,
                )

                response.objectField("ofdServiceInfo")?.let { ofdServiceInfo ->
                    assertFieldType(
                        this,
                        ofdServiceInfo,
                        "geoLatitude",
                        "ofdServiceInfo.geoLatitude",
                        Int::class.javaObjectType,
                        OFD_SERVICE_INFO_SCHEMA,
                    )
                    assertFieldType(
                        this,
                        ofdServiceInfo,
                        "geoLongitude",
                        "ofdServiceInfo.geoLongitude",
                        Int::class.javaObjectType,
                        OFD_SERVICE_INFO_SCHEMA,
                    )
                    assertFieldType(
                        this,
                        ofdServiceInfo,
                        "geoSource",
                        "ofdServiceInfo.geoSource",
                        String::class.java,
                        OFD_SERVICE_INFO_SCHEMA
                    )
                    assertFieldType(
                        this,
                        ofdServiceInfo,
                        "orgAddress",
                        "ofdServiceInfo.orgAddress",
                        String::class.java,
                        OFD_SERVICE_INFO_SCHEMA
                    )
                    assertFieldType(
                        this,
                        ofdServiceInfo,
                        "orgAddressKz",
                        "ofdServiceInfo.orgAddressKz",
                        String::class.java,
                        OFD_SERVICE_INFO_SCHEMA,
                    )
                    assertFieldType(
                        this,
                        ofdServiceInfo,
                        "orgInn",
                        "ofdServiceInfo.orgInn",
                        String::class.java,
                        OFD_SERVICE_INFO_SCHEMA
                    )
                    assertFieldType(
                        this,
                        ofdServiceInfo,
                        "orgOkved",
                        "ofdServiceInfo.orgOkved",
                        String::class.java,
                        OFD_SERVICE_INFO_SCHEMA
                    )
                    assertFieldType(
                        this,
                        ofdServiceInfo,
                        "orgTitle",
                        "ofdServiceInfo.orgTitle",
                        String::class.java,
                        OFD_SERVICE_INFO_SCHEMA
                    )
                }

                response.objectField("branding")?.let { branding ->
                    assertFieldType(
                        this,
                        branding,
                        "language",
                        "branding.language",
                        String::class.java,
                        BRANDING_RESPONSE_SCHEMA
                    )
                    assertFieldType(
                        this,
                        branding,
                        "ofdTicketAds",
                        "branding.ofdTicketAds",
                        List::class.java,
                        BRANDING_RESPONSE_SCHEMA
                    )
                    assertOfdTicketAdsItemsType(this, branding["ofdTicketAds"])
                    assertFieldType(
                        this,
                        branding,
                        "paperWidthMm",
                        "branding.paperWidthMm",
                        Int::class.javaObjectType,
                        BRANDING_RESPONSE_SCHEMA,
                    )
                    assertFieldType(
                        this,
                        branding,
                        "printOfdTicketAds",
                        "branding.printOfdTicketAds",
                        Boolean::class.javaObjectType,
                        BRANDING_RESPONSE_SCHEMA,
                    )
                    assertFieldType(
                        this,
                        branding,
                        "themeColor",
                        "branding.themeColor",
                        String::class.java,
                        BRANDING_RESPONSE_SCHEMA
                    )
                    assertFieldType(
                        this,
                        branding,
                        "useForceDarkTheme",
                        "branding.useForceDarkTheme",
                        Boolean::class.javaObjectType,
                        BRANDING_RESPONSE_SCHEMA,
                    )

                    BRANDING_OPTIONAL_STRING_FIELDS.forEach { fieldName ->
                        assertOptionalFieldType(
                            this,
                            branding,
                            fieldName,
                            "branding.$fieldName",
                            String::class.java,
                            BRANDING_RESPONSE_SCHEMA,
                        )
                    }
                }
            }.assertAll()
        }

        @Test
        @Severity(SeverityLevel.CRITICAL)
        @DisplayName("Метод GET /kkm/{kkmId} не возвращает поля вне Swagger-контракта")
        fun shouldNotReturnFieldsOutsideSwaggerContract() {
            val response = getKkmDetails(kkmId)

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
        @Severity(SeverityLevel.CRITICAL)
        @DisplayName("Метод GET /kkm/{kkmId} возвращает допустимые enum-значения")
        fun shouldReturnSupportedEnumValues() {
            val response = getKkmDetails(kkmId)

            SoftAssertions().apply {
                assertRequiredEnumValue(this, response, "mode", "mode", KKM_RESPONSE_SCHEMA, ApiEnumValues.KKM_MODES)
                assertRequiredEnumValue(this, response, "state", "state", KKM_RESPONSE_SCHEMA, ApiEnumValues.KKM_STATES)
                assertOptionalEnumValue(this, response, "defaultVatGroup", "defaultVatGroup", ApiEnumValues.VAT_GROUPS)
                assertOptionalEnumValue(
                    this,
                    response,
                    "ofdEnvironment",
                    "ofdEnvironment",
                    ApiEnumValues.OFD_ENVIRONMENTS
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

    }

    @Nested
    @ApiRegression
    @Feature("API")
    @Story("GET /kkm/{kkmId}")
    @Owner("Pavel Michka")
    @DisplayName("Негативные проверки GET /kkm/{kkmId}")
    inner class NegativeRegressionTests {
        @Nested
        @ApiRegression
        @Feature("API")
        @Story("GET /kkm/{kkmId}")
        @Owner("Pavel Michka")
        @DisplayName("Проверки несуществующих идентификаторов")
        inner class MissingIdentifiersTests {
            @Test
            @Severity(SeverityLevel.NORMAL)
            @DisplayName("Метод GET /kkm/{kkmId} возвращает 404 для несуществующей ККМ")
            fun shouldReturnNotFoundForUnknownKkmId() {
                val unknownKkmId = UUID.randomUUID().toString()

                reportStep("Проверяем GET /kkm/$unknownKkmId для несуществующей ККМ без авторизации") {
                    superkassa.requestWithoutAuthorization()
                        .`when`()
                        .get("/kkm/{kkmId}", unknownKkmId)
                        .then()
                        .shouldHaveStatus(404, "несуществующая ККМ")
                        .contentType(ContentType.JSON)
                }
            }

        }

        @Nested
        @ApiRegression
        @Feature("API")
        @Story("GET /kkm/{kkmId}")
        @Owner("Pavel Michka")
        @DisplayName("Проверки неподдерживаемых HTTP-методов")
        inner class UnsupportedHttpMethodsTests {
            @ParameterizedTest(name = "HTTP {0} /kkm/'{'kkmId'}' возвращает 405")
            @EnumSource(value = Method::class, names = ["POST", "PUT", "PATCH"])
            @Severity(SeverityLevel.NORMAL)
            @DisplayName("Метод /kkm/{kkmId} возвращает 405 для HTTP-методов кроме GET и DELETE")
            fun shouldReturnMethodNotAllowedForUnsupportedMethods(method: Method) {
                val kkmId = UUID.randomUUID().toString()

                reportStep("Проверяем, что HTTP $method /kkm/$kkmId не поддерживается") {
                    superkassa.requestWithoutAuthorization()
                        .`when`()
                        .request(method, "/kkm/{kkmId}", kkmId)
                        .then()
                        .shouldHaveStatus(405, "неподдерживаемый HTTP-метод")
                }
            }

        }
    }

    private fun getKkmDetails(kkmId: String): Map<String, Any?> {
        return reportStep("Получаем информацию о ККМ kkmId='$kkmId' через GET /kkm/$kkmId без авторизации") {
            superkassa.requestWithoutAuthorization()
                .`when`()
                .get("/kkm/{kkmId}", kkmId)
                .then()
                .shouldHaveStatus(200, "публичное получение информации о ККМ")
                .contentType(ContentType.JSON)
                .extract()
                .jsonPath()
                .getMap("")
        }
    }

    private fun firstKkmIdOrSkip(): String {
        val response: Response = reportStep("Получаем контрольную ККМ из списка GET /kkm?limit=1&offset=0") {
            superkassa.requestWithoutAuthorization()
                .queryParam("limit", 1)
                .queryParam("offset", 0)
                .`when`()
                .get("/kkm")
                .then()
                .shouldHaveStatus(200, "получение контрольной ККМ для проверки GET /kkm/{kkmId}")
                .contentType(ContentType.JSON)
                .extract()
                .response()
        }

        val items = response.jsonPath().getList<Map<String, Any?>>("items").orEmpty()
        assumeTrue(items.isNotEmpty(), "В системе нет ККМ для проверки GET /kkm/{kkmId}")

        val kkmId = items.first()["kkmId"] as? String
        assumeTrue(!kkmId.isNullOrBlank(), "В контрольной ККМ отсутствует заполненный kkmId")

        return kkmId!!
    }

    private fun assertFieldType(
        softly: SoftAssertions,
        item: Map<String, Any?>,
        fieldName: String,
        fieldPath: String,
        expectedType: Class<*>,
        schemaName: String,
    ) {
        softly.assertThat(item)
            .withFailMessage(
                ApiContractErrorMessages.requiredFieldWithTypeMissing(
                    ENDPOINT,
                    fieldPath,
                    expectedType.simpleName,
                    schemaName,
                ),
            )
            .containsKey(fieldName)

        softly.assertThat(item[fieldName])
            .withFailMessage(
                ApiContractErrorMessages.fieldTypeMismatch(
                    ENDPOINT,
                    fieldPath,
                    expectedType.simpleName,
                    schemaName
                )
            )
            .isInstanceOf(expectedType)
    }

    private fun assertOptionalFieldType(
        softly: SoftAssertions,
        item: Map<String, Any?>,
        fieldName: String,
        fieldPath: String,
        expectedType: Class<*>,
        schemaName: String,
    ) {
        val fieldValue = item[fieldName] ?: return

        softly.assertThat(fieldValue)
            .withFailMessage(
                ApiContractErrorMessages.optionalFieldTypeMismatch(
                    ENDPOINT,
                    fieldPath,
                    expectedType.simpleName,
                    schemaName
                )
            )
            .isInstanceOf(expectedType)
    }

    private fun assertOfdTicketAdsItemsType(
        softly: SoftAssertions,
        fieldValue: Any?,
    ) {
        (fieldValue as? List<*>)?.forEachIndexed { index, item ->
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
        item: Map<String, Any?>,
        schemaName: String,
        allowedFields: Set<String>,
    ) {
        val unexpectedFields = item.keys - allowedFields

        softly.assertThat(unexpectedFields)
            .withFailMessage(ApiContractErrorMessages.unexpectedSwaggerFields(ENDPOINT, schemaName, unexpectedFields))
            .isEmpty()
    }

    private fun assertRequiredEnumValue(
        softly: SoftAssertions,
        item: Map<String, Any?>,
        fieldName: String,
        fieldPath: String,
        schemaName: String,
        supportedValues: Set<String>,
    ) {
        val fieldValue = item[fieldName] as? String

        softly.assertThat(fieldValue)
            .withFailMessage(ApiContractErrorMessages.requiredEnumMissing(ENDPOINT, fieldPath, schemaName))
            .isNotBlank()

        softly.assertThat(fieldValue)
            .withFailMessage(ApiContractErrorMessages.enumUnsupported(ENDPOINT, fieldPath, fieldValue, supportedValues))
            .isIn(supportedValues)
    }

    private fun assertOptionalEnumValue(
        softly: SoftAssertions,
        item: Map<String, Any?>,
        fieldName: String,
        fieldPath: String,
        supportedValues: Set<String>,
    ) {
        val fieldValue = item[fieldName] ?: return
        val enumValue = fieldValue as? String

        softly.assertThat(enumValue)
            .withFailMessage(ApiContractErrorMessages.enumUnsupported(ENDPOINT, fieldPath, enumValue, supportedValues))
            .isIn(supportedValues)
    }

    @Suppress("UNCHECKED_CAST")
    private fun Map<String, Any?>.objectField(fieldName: String): Map<String, Any?>? =
        this[fieldName] as? Map<String, Any?>

    private companion object {
        const val ENDPOINT = "GET /kkm/{kkmId}"
        const val KKM_RESPONSE_SCHEMA = "KkmResponse"
        const val OFD_SERVICE_INFO_SCHEMA = "OfdServiceInfoResponse"
        const val BRANDING_RESPONSE_SCHEMA = "ReceiptBrandingResponse"

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

        val BRANDING_OPTIONAL_STRING_FIELDS = listOf(
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
    }
}
