package kz.superkassa.tests.api.programming.enter

import io.qameta.allure.Allure
import io.qameta.allure.Feature
import io.qameta.allure.Owner
import io.qameta.allure.Severity
import io.qameta.allure.SeverityLevel
import io.qameta.allure.Story
import io.restassured.http.ContentType
import io.restassured.http.Method
import io.restassured.path.json.JsonPath
import io.restassured.response.Response
import kz.superkassa.tests.framework.BaseTest
import kz.superkassa.tests.framework.assertions.ApiContractErrorMessages
import kz.superkassa.tests.framework.contract.ApiEnumValues
import kz.superkassa.tests.framework.kkm.PreparedKkmAuth
import kz.superkassa.tests.framework.reporting.reportStep
import kz.superkassa.tests.framework.tags.ApiRegression
import org.assertj.core.api.SoftAssertions
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

@ApiRegression
@Feature("API")
@Story("POST /kkm/{kkmId}/programming/enter")
@Owner("Pavel Michka")
@DisplayName("POST /kkm/{kkmId}/programming/enter: регрессионные проверки входа в режим программирования")
@Suppress("SameParameterValue", "NonAsciiCharacters")
class KkmProgrammingEnterRegressionTest : BaseTest() {
    private var kkmToExitAfterTest: PreparedKkmAuth? = null

    @AfterEach
    fun `Возвращаем ККМ из режима программирования после проверки`() {
        val preparedKkm = kkmToExitAfterTest ?: return
        kkmToExitAfterTest = null
        exitProgramming(preparedKkm)
    }

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Метод POST /kkm/{kkmId}/programming/enter возвращает поля ожидаемых типов")
    fun shouldReturnExpectedFieldTypes() {
        val preparedKkm = kkmAuth.prepareFirstKkmAdminPin()

        withProgrammingMode(preparedKkm) { json ->
            val response = json.getMap<String, Any?>("")

            SoftAssertions().apply {
                assertFieldType(this, response, "autoCloseShift", Boolean::class.javaObjectType, "KkmResponse")
                assertFieldType(this, response, "createdAt", Long::class.javaObjectType, "KkmResponse")
                assertFieldType(this, response, "kkmId", String::class.java, "KkmResponse")
                assertFieldType(this, response, "mode", String::class.java, "KkmResponse")
                assertFieldType(this, response, "state", String::class.java, "KkmResponse")
                assertFieldType(this, response, "updatedAt", Long::class.javaObjectType, "KkmResponse")
                assertOptionalFieldType(this, response, "autonomousSince", Long::class.javaObjectType, "KkmResponse")
                assertOptionalFieldType(this, response, "defaultVatGroup", String::class.java, "KkmResponse")
                assertOptionalFieldType(this, response, "factoryNumber", String::class.java, "KkmResponse")
                assertOptionalFieldType(this, response, "kkmKgdId", String::class.java, "KkmResponse")
                assertOptionalFieldType(this, response, "lastFiscalHashBase64", String::class.java, "KkmResponse")
                assertOptionalFieldType(this, response, "lastReceiptNo", Int::class.javaObjectType, "KkmResponse")
                assertOptionalFieldType(this, response, "lastShiftNo", Int::class.javaObjectType, "KkmResponse")
                assertOptionalFieldType(this, response, "lastZReportNo", Int::class.javaObjectType, "KkmResponse")
                assertOptionalFieldType(this, response, "manufactureYear", Int::class.javaObjectType, "KkmResponse")
                assertOptionalFieldType(this, response, "ofdEnvironment", String::class.java, "KkmResponse")
                assertOptionalFieldType(this, response, "ofdId", String::class.java, "KkmResponse")
                assertOptionalFieldType(this, response, "ofdServiceInfo", Map::class.java, "KkmResponse")
                assertOptionalFieldType(this, response, "ofdSystemId", String::class.java, "KkmResponse")
                assertOptionalFieldType(this, response, "taxRegime", String::class.java, "KkmResponse")
                assertOptionalFieldType(this, response, "tokenUpdatedAt", Long::class.javaObjectType, "KkmResponse")
                assertOptionalFieldType(this, response, "branding", Map::class.java, "KkmResponse")

                response.objectField("ofdServiceInfo")?.let { ofdServiceInfo ->
                    assertFieldType(this, ofdServiceInfo, "ofdServiceInfo.geoLatitude", "geoLatitude", Int::class.javaObjectType, "OfdServiceInfoResponse")
                    assertFieldType(this, ofdServiceInfo, "ofdServiceInfo.geoLongitude", "geoLongitude", Int::class.javaObjectType, "OfdServiceInfoResponse")
                    assertFieldType(this, ofdServiceInfo, "ofdServiceInfo.geoSource", "geoSource", String::class.java, "OfdServiceInfoResponse")
                    assertFieldType(this, ofdServiceInfo, "ofdServiceInfo.orgAddress", "orgAddress", String::class.java, "OfdServiceInfoResponse")
                    assertFieldType(this, ofdServiceInfo, "ofdServiceInfo.orgAddressKz", "orgAddressKz", String::class.java, "OfdServiceInfoResponse")
                    assertFieldType(this, ofdServiceInfo, "ofdServiceInfo.orgInn", "orgInn", String::class.java, "OfdServiceInfoResponse")
                    assertFieldType(this, ofdServiceInfo, "ofdServiceInfo.orgOkved", "orgOkved", String::class.java, "OfdServiceInfoResponse")
                    assertFieldType(this, ofdServiceInfo, "ofdServiceInfo.orgTitle", "orgTitle", String::class.java, "OfdServiceInfoResponse")
                }

                response.objectField("branding")?.let { branding ->
                    assertFieldType(this, branding, "branding.language", "language", String::class.java, "ReceiptBrandingResponse")
                    assertFieldType(this, branding, "branding.ofdTicketAds", "ofdTicketAds", List::class.java, "ReceiptBrandingResponse")
                    assertArrayItemsType(this, branding, "branding.ofdTicketAds", "ofdTicketAds", String::class.java, "ReceiptBrandingResponse")
                    assertFieldType(this, branding, "branding.paperWidthMm", "paperWidthMm", Int::class.javaObjectType, "ReceiptBrandingResponse")
                    assertFieldType(this, branding, "branding.printOfdTicketAds", "printOfdTicketAds", Boolean::class.javaObjectType, "ReceiptBrandingResponse")
                    assertFieldType(this, branding, "branding.themeColor", "themeColor", String::class.java, "ReceiptBrandingResponse")
                    assertFieldType(this, branding, "branding.useForceDarkTheme", "useForceDarkTheme", Boolean::class.javaObjectType, "ReceiptBrandingResponse")
                    assertOptionalFieldType(this, branding, "branding.afterHeaderMsg", "afterHeaderMsg", String::class.java, "ReceiptBrandingResponse")
                    assertOptionalFieldType(this, branding, "branding.afterItemsMsg", "afterItemsMsg", String::class.java, "ReceiptBrandingResponse")
                    assertOptionalFieldType(this, branding, "branding.afterTotalsMsg", "afterTotalsMsg", String::class.java, "ReceiptBrandingResponse")
                    assertOptionalFieldType(this, branding, "branding.beforeHeaderMsg", "beforeHeaderMsg", String::class.java, "ReceiptBrandingResponse")
                    assertOptionalFieldType(this, branding, "branding.beforeItemsMsg", "beforeItemsMsg", String::class.java, "ReceiptBrandingResponse")
                    assertOptionalFieldType(this, branding, "branding.beforeQrMsg", "beforeQrMsg", String::class.java, "ReceiptBrandingResponse")
                    assertOptionalFieldType(this, branding, "branding.beforeTotalsMsg", "beforeTotalsMsg", String::class.java, "ReceiptBrandingResponse")
                    assertOptionalFieldType(this, branding, "branding.customBackgroundColorHex", "customBackgroundColorHex", String::class.java, "ReceiptBrandingResponse")
                    assertOptionalFieldType(
                        this,
                        branding,
                        "branding.customCardTopBorderColorHex",
                        "customCardTopBorderColorHex",
                        String::class.java,
                        "ReceiptBrandingResponse",
                    )
                    assertOptionalFieldType(this, branding, "branding.footerMsg", "footerMsg", String::class.java, "ReceiptBrandingResponse")
                    assertOptionalFieldType(this, branding, "branding.headerLogoUrl", "headerLogoUrl", String::class.java, "ReceiptBrandingResponse")
                    assertOptionalFieldType(this, branding, "branding.headerMsg", "headerMsg", String::class.java, "ReceiptBrandingResponse")
                }
            }.assertAll()
        }
    }

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Метод POST /kkm/{kkmId}/programming/enter не возвращает поля вне Swagger-контракта")
    fun shouldNotReturnFieldsOutsideSwaggerContract() {
        val preparedKkm = kkmAuth.prepareFirstKkmAdminPin()

        withProgrammingMode(preparedKkm) { json ->
            val response = json.getMap<String, Any?>("")

            SoftAssertions().apply {
                assertOnlySwaggerFields(this, response, "KkmResponse", KKM_RESPONSE_FIELDS)

                response.objectField("ofdServiceInfo")?.let { ofdServiceInfo ->
                    assertOnlySwaggerFields(this, ofdServiceInfo, "OfdServiceInfoResponse", OFD_SERVICE_INFO_RESPONSE_FIELDS)
                }

                response.objectField("branding")?.let { branding ->
                    assertOnlySwaggerFields(this, branding, "ReceiptBrandingResponse", RECEIPT_BRANDING_RESPONSE_FIELDS)
                }
            }.assertAll()
        }
    }

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Метод POST /kkm/{kkmId}/programming/enter возвращает допустимые enum и бизнес-значения")
    fun shouldReturnExpectedBusinessValues() {
        val preparedKkm = kkmAuth.prepareFirstKkmAdminPin()

        withProgrammingMode(preparedKkm) { json ->
            val response = json.getMap<String, Any?>("")

            SoftAssertions().apply {
                assertThat(response["kkmId"] as? String)
                    .withFailMessage(
                        "Функциональность API нарушена: POST /kkm/%s/programming/enter вернул данные другой ККМ. Ожидался kkmId='%s'.",
                        preparedKkm.kkmId,
                        preparedKkm.kkmId,
                    )
                    .isEqualTo(preparedKkm.kkmId)
                assertThat(response["mode"] as? String)
                    .withFailMessage(ApiContractErrorMessages.enumUnsupported(ENDPOINT, "mode", response["mode"] as? String, ApiEnumValues.KKM_MODES))
                    .isIn(ApiEnumValues.KKM_MODES)
                assertThat(response["mode"] as? String)
                    .withFailMessage(
                        "Функциональность API нарушена: POST /kkm/%s/programming/enter должен вернуть mode='PROGRAMMING'.",
                        preparedKkm.kkmId,
                    )
                    .isEqualTo("PROGRAMMING")
                assertThat(response["state"] as? String)
                    .withFailMessage(ApiContractErrorMessages.enumUnsupported(ENDPOINT, "state", response["state"] as? String, ApiEnumValues.KKM_STATES))
                    .isIn(ApiEnumValues.KKM_STATES)
                assertThat(response["createdAt"] as? Long).isGreaterThan(0)
                assertThat(response["updatedAt"] as? Long).isGreaterThan(0)
                assertOptionalEnumValue(this, response, "taxRegime", ApiEnumValues.TAX_REGIMES)
                assertOptionalEnumValue(this, response, "defaultVatGroup", ApiEnumValues.VAT_GROUPS)
                assertOptionalEnumValue(this, response, "ofdEnvironment", ApiEnumValues.OFD_ENVIRONMENTS)
                assertOptionalEnumValue(this, response, "ofdId", ApiEnumValues.OFD_IDS)

                response.objectField("branding")?.let { branding ->
                    assertRequiredEnumValue(this, branding, "branding.language", "language", "ReceiptBrandingResponse", ApiEnumValues.BRANDING_LANGUAGES)
                    assertRequiredEnumValue(this, branding, "branding.themeColor", "themeColor", "ReceiptBrandingResponse", ApiEnumValues.BRANDING_THEME_COLORS)
                }
            }.assertAll()
        }
    }

    @Nested
    @ApiRegression
    @Feature("API")
    @Story("POST /kkm/{kkmId}/programming/enter")
    @Owner("Pavel Michka")
    @DisplayName("Проверки авторизации POST /kkm/{kkmId}/programming/enter")
    inner class AuthorizationRegressionTests {
        @Test
        @Severity(SeverityLevel.NORMAL)
        @DisplayName("Метод POST /kkm/{kkmId}/programming/enter возвращает 401 без Authorization")
        fun shouldReturnUnauthorizedWithoutAuthorization() {
            val preparedKkm = kkmAuth.prepareFirstKkmAdminPin()

            reportStep("Проверяем POST /kkm/${preparedKkm.kkmId}/programming/enter без Authorization") {
                superkassa.requestWithoutAuthorization()
                    .`when`()
                    .post(enterProgrammingPath(preparedKkm.kkmId))
                    .then()
                    .shouldHaveStatus(401, "запрос без Authorization")
                    .contentType(ContentType.JSON)
            }
        }

        @Test
        @Severity(SeverityLevel.NORMAL)
        @DisplayName("Метод POST /kkm/{kkmId}/programming/enter возвращает 403 для неверного PIN")
        fun shouldReturnForbiddenForInvalidPin() {
            val preparedKkm = kkmAuth.prepareFirstKkmAdminPin()

            reportStep("Проверяем POST /kkm/${preparedKkm.kkmId}/programming/enter с неверным PIN") {
                superkassa.request(INVALID_PIN)
                    .`when`()
                    .post(enterProgrammingPath(preparedKkm.kkmId))
                    .then()
                    .shouldHaveStatus(403, "запрос с неверным PIN")
                    .contentType(ContentType.JSON)
            }
        }
    }

    @Test
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("Метод POST /kkm/{kkmId}/programming/enter возвращает 404 для несуществующей ККМ")
    fun shouldReturnNotFoundForUnknownKkmId() {
        val preparedKkm = kkmAuth.prepareFirstKkmAdminPin()

        reportStep("Проверяем POST /kkm/$UNKNOWN_KKM_ID/programming/enter для несуществующей ККМ") {
            superkassa.request(preparedKkm.adminPin)
                .`when`()
                .post(enterProgrammingPath(UNKNOWN_KKM_ID))
                .then()
                .shouldHaveStatus(404, "несуществующая ККМ")
                .contentType(ContentType.JSON)
        }
    }

    @ParameterizedTest(name = "HTTP {0} /kkm/'{'kkmId'}'/programming/enter возвращает 405")
    @EnumSource(value = Method::class, names = ["GET", "PUT", "PATCH", "DELETE"])
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("Метод /kkm/{kkmId}/programming/enter возвращает 405 для HTTP-методов кроме POST")
    fun shouldReturnMethodNotAllowedForNonPostMethods(method: Method) {
        val preparedKkm = kkmAuth.prepareFirstKkmAdminPin()

        reportStep("Проверяем, что HTTP $method /kkm/${preparedKkm.kkmId}/programming/enter не поддерживается") {
            superkassa.request(preparedKkm.adminPin)
                .`when`()
                .request(method, enterProgrammingPath(preparedKkm.kkmId))
                .then()
                .shouldHaveStatus(405, "неподдерживаемый HTTP-метод")
        }
    }

    private fun withProgrammingMode(preparedKkm: PreparedKkmAuth, action: (JsonPath) -> Unit) {
        val json = enterProgrammingJson(preparedKkm)
        kkmToExitAfterTest = preparedKkm
        action(json)
    }

    private fun enterProgrammingJson(preparedKkm: PreparedKkmAuth): JsonPath {
        val enterProgramming: Allure.ThrowableRunnable<Response> = Allure.ThrowableRunnable {
            superkassa.request(preparedKkm.adminPin)
                .`when`()
                .post(enterProgrammingPath(preparedKkm.kkmId))
                .then()
                .shouldHaveStatus(200, "успешный вход в режим программирования")
                .contentType(ContentType.JSON)
                .extract()
                .response()
        }

        val response: Response = Allure.step("Переводим ККМ kkmId='${preparedKkm.kkmId}' в режим программирования", enterProgramming)

        return response.jsonPath()
    }

    private fun exitProgramming(preparedKkm: PreparedKkmAuth) {
        val exitProgramming: Allure.ThrowableRunnable<Unit> = Allure.ThrowableRunnable {
            superkassa.request(preparedKkm.adminPin)
                .`when`()
                .post("/kkm/{kkmId}/programming/exit", preparedKkm.kkmId)
                .then()
                .shouldHaveStatus(200, "возврат ККМ из режима программирования")
                .contentType(ContentType.JSON)
        }

        Allure.step("Возвращаем ККМ kkmId='${preparedKkm.kkmId}' из режима программирования", exitProgramming)
    }

    private fun enterProgrammingPath(kkmId: String): String = "/kkm/$kkmId/programming/enter"

    private fun assertFieldType(
        softly: SoftAssertions,
        item: Map<String, Any?>,
        fieldName: String,
        expectedType: Class<*>,
        schemaName: String,
    ) {
        assertFieldType(softly, item, fieldName, fieldName, expectedType, schemaName)
    }

    private fun assertFieldType(
        softly: SoftAssertions,
        item: Map<String, Any?>,
        responseFieldName: String,
        sourceFieldName: String,
        expectedType: Class<*>,
        schemaName: String,
    ) {
        softly.assertThat(item)
            .withFailMessage(ApiContractErrorMessages.requiredFieldWithTypeMissing(ENDPOINT, responseFieldName, expectedType.simpleName, schemaName))
            .containsKey(sourceFieldName)

        softly.assertThat(item[sourceFieldName])
            .withFailMessage(ApiContractErrorMessages.fieldTypeMismatch(ENDPOINT, responseFieldName, expectedType.simpleName, schemaName))
            .isInstanceOf(expectedType)
    }

    private fun assertOptionalFieldType(
        softly: SoftAssertions,
        item: Map<String, Any?>,
        fieldName: String,
        expectedType: Class<*>,
        schemaName: String,
    ) {
        assertOptionalFieldType(softly, item, fieldName, fieldName, expectedType, schemaName)
    }

    private fun assertOptionalFieldType(
        softly: SoftAssertions,
        item: Map<String, Any?>,
        responseFieldName: String,
        sourceFieldName: String,
        expectedType: Class<*>,
        schemaName: String,
    ) {
        val fieldValue = item[sourceFieldName] ?: return

        softly.assertThat(fieldValue)
            .withFailMessage(ApiContractErrorMessages.optionalFieldTypeMismatch(ENDPOINT, responseFieldName, expectedType.simpleName, schemaName))
            .isInstanceOf(expectedType)
    }

    private fun assertArrayItemsType(
        softly: SoftAssertions,
        item: Map<String, Any?>,
        responseFieldName: String,
        sourceFieldName: String,
        expectedType: Class<*>,
        schemaName: String,
    ) {
        val fieldValue = item[sourceFieldName] as? List<*> ?: return

        fieldValue.forEachIndexed { index, arrayItem ->
            softly.assertThat(arrayItem)
                .withFailMessage(ApiContractErrorMessages.arrayItemTypeMismatch(ENDPOINT, responseFieldName, index, expectedType.simpleName, schemaName))
                .isInstanceOf(expectedType)
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
        responseFieldName: String,
        sourceFieldName: String,
        schemaName: String,
        supportedValues: Set<String>,
    ) {
        val fieldValue = item[sourceFieldName] as? String

        softly.assertThat(fieldValue)
            .withFailMessage(ApiContractErrorMessages.requiredEnumMissing(ENDPOINT, responseFieldName, schemaName))
            .isNotBlank()

        softly.assertThat(fieldValue)
            .withFailMessage(ApiContractErrorMessages.enumUnsupported(ENDPOINT, responseFieldName, fieldValue, supportedValues))
            .isIn(supportedValues)
    }

    private fun assertOptionalEnumValue(
        softly: SoftAssertions,
        item: Map<String, Any?>,
        fieldName: String,
        supportedValues: Set<String>,
    ) {
        val fieldValue = item[fieldName] as? String ?: return

        softly.assertThat(fieldValue)
            .withFailMessage(ApiContractErrorMessages.enumUnsupported(ENDPOINT, fieldName, fieldValue, supportedValues))
            .isIn(supportedValues)
    }

    @Suppress("UNCHECKED_CAST")
    private fun Map<String, Any?>.objectField(fieldName: String): Map<String, Any?>? = this[fieldName] as? Map<String, Any?>

    private companion object {
        const val ENDPOINT = "POST /kkm/{kkmId}/programming/enter"
        const val INVALID_PIN = "999999"
        const val UNKNOWN_KKM_ID = "00000000-0000-0000-0000-000000000000"

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
        val RECEIPT_BRANDING_RESPONSE_FIELDS = setOf(
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
    }
}
