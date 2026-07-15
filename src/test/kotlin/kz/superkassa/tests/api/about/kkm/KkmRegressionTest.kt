package kz.superkassa.tests.api.about.kkm

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
import kz.superkassa.tests.framework.contract.ApiEnumValues
import kz.superkassa.tests.framework.assertions.ApiContractErrorMessages
import kz.superkassa.tests.framework.tags.ApiRegression
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.SoftAssertions
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.EnumSource

@ApiRegression
@Feature("API")
@Story("GET /kkm")
@Owner("Pavel Michka")
@DisplayName("GET /kkm: регрессионные проверки списка ККМ")
@Suppress("SameParameterValue")
class KkmRegressionTest : BaseTest() {
    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Метод GET /kkm возвращает поля ожидаемых типов")
    fun shouldReturnExpectedFieldTypes() {
        val json = getKkmJson()

        val items = json.getList<Map<String, Any?>>("items")

        SoftAssertions().apply {
            val response = json.getMap<String, Any?>("")

            assertFieldType(this, response, ENDPOINT, "items", List::class.java, "PaginatedResponseKkmResponse")
            assertIntegerFieldType(this, response, ENDPOINT, "total", "PaginatedResponseKkmResponse")
            assertIntegerFieldType(this, response, ENDPOINT, "limit", "PaginatedResponseKkmResponse")
            assertIntegerFieldType(this, response, ENDPOINT, "offset", "PaginatedResponseKkmResponse")
            assertFieldType(this, response, ENDPOINT, "hasMore", Boolean::class.javaObjectType, "PaginatedResponseKkmResponse")

            items.forEach { item ->
                assertFieldType(this, item, ENDPOINT, "autoCloseShift", Boolean::class.javaObjectType, "KkmResponse")
                assertFieldType(this, item, ENDPOINT, "createdAt", Long::class.javaObjectType, "KkmResponse")
                assertFieldType(this, item, ENDPOINT, "kkmId", String::class.java, "KkmResponse")
                assertFieldType(this, item, ENDPOINT, "mode", String::class.java, "KkmResponse")
                assertFieldType(this, item, ENDPOINT, "state", String::class.java, "KkmResponse")
                assertFieldType(this, item, ENDPOINT, "updatedAt", Long::class.javaObjectType, "KkmResponse")
                assertOptionalFieldType(this, item, ENDPOINT, "autonomousSince", Long::class.javaObjectType, "KkmResponse")
                assertOptionalFieldType(this, item, ENDPOINT, "defaultVatGroup", String::class.java, "KkmResponse")
                assertOptionalFieldType(this, item, ENDPOINT, "factoryNumber", String::class.java, "KkmResponse")
                assertOptionalFieldType(this, item, ENDPOINT, "kkmKgdId", String::class.java, "KkmResponse")
                assertOptionalFieldType(this, item, ENDPOINT, "lastFiscalHashBase64", String::class.java, "KkmResponse")
                assertOptionalFieldType(this, item, ENDPOINT, "lastReceiptNo", Int::class.javaObjectType, "KkmResponse")
                assertOptionalFieldType(this, item, ENDPOINT, "lastShiftNo", Int::class.javaObjectType, "KkmResponse")
                assertOptionalFieldType(this, item, ENDPOINT, "lastZReportNo", Int::class.javaObjectType, "KkmResponse")
                assertOptionalFieldType(this, item, ENDPOINT, "manufactureYear", Int::class.javaObjectType, "KkmResponse")
                assertOptionalFieldType(this, item, ENDPOINT, "ofdEnvironment", String::class.java, "KkmResponse")
                assertOptionalFieldType(this, item, ENDPOINT, "ofdId", String::class.java, "KkmResponse")
                assertOptionalFieldType(this, item, ENDPOINT, "ofdSystemId", String::class.java, "KkmResponse")
                assertOptionalFieldType(this, item, ENDPOINT, "taxRegime", String::class.java, "KkmResponse")
                assertOptionalFieldType(this, item, ENDPOINT, "tokenUpdatedAt", Long::class.javaObjectType, "KkmResponse")

                item.objectField("ofdServiceInfo")?.let { ofdServiceInfo ->
                    assertFieldType(this, ofdServiceInfo, ENDPOINT, "geoLatitude", Int::class.javaObjectType, "OfdServiceInfoResponse")
                    assertFieldType(this, ofdServiceInfo, ENDPOINT, "geoLongitude", Int::class.javaObjectType, "OfdServiceInfoResponse")
                    assertFieldType(this, ofdServiceInfo, ENDPOINT, "geoSource", String::class.java, "OfdServiceInfoResponse")
                    assertFieldType(this, ofdServiceInfo, ENDPOINT, "orgAddress", String::class.java, "OfdServiceInfoResponse")
                    assertFieldType(this, ofdServiceInfo, ENDPOINT, "orgAddressKz", String::class.java, "OfdServiceInfoResponse")
                    assertFieldType(this, ofdServiceInfo, ENDPOINT, "orgInn", String::class.java, "OfdServiceInfoResponse")
                    assertFieldType(this, ofdServiceInfo, ENDPOINT, "orgOkved", String::class.java, "OfdServiceInfoResponse")
                    assertFieldType(this, ofdServiceInfo, ENDPOINT, "orgTitle", String::class.java, "OfdServiceInfoResponse")
                }

                item.objectField("branding")?.let { branding ->
                    assertFieldType(this, branding, ENDPOINT, "language", String::class.java, "ReceiptBrandingResponse")
                    assertFieldType(this, branding, ENDPOINT, "ofdTicketAds", List::class.java, "ReceiptBrandingResponse")
                    assertFieldType(this, branding, ENDPOINT, "paperWidthMm", Int::class.javaObjectType, "ReceiptBrandingResponse")
                    assertFieldType(this, branding, ENDPOINT, "printOfdTicketAds", Boolean::class.javaObjectType, "ReceiptBrandingResponse")
                    assertFieldType(this, branding, ENDPOINT, "themeColor", String::class.java, "ReceiptBrandingResponse")
                    assertFieldType(this, branding, ENDPOINT, "useForceDarkTheme", Boolean::class.javaObjectType, "ReceiptBrandingResponse")
                    assertOptionalFieldType(this, branding, ENDPOINT, "afterHeaderMsg", String::class.java, "ReceiptBrandingResponse")
                    assertOptionalFieldType(this, branding, ENDPOINT, "afterItemsMsg", String::class.java, "ReceiptBrandingResponse")
                    assertOptionalFieldType(this, branding, ENDPOINT, "afterTotalsMsg", String::class.java, "ReceiptBrandingResponse")
                    assertOptionalFieldType(this, branding, ENDPOINT, "beforeHeaderMsg", String::class.java, "ReceiptBrandingResponse")
                    assertOptionalFieldType(this, branding, ENDPOINT, "beforeItemsMsg", String::class.java, "ReceiptBrandingResponse")
                    assertOptionalFieldType(this, branding, ENDPOINT, "beforeQrMsg", String::class.java, "ReceiptBrandingResponse")
                    assertOptionalFieldType(this, branding, ENDPOINT, "beforeTotalsMsg", String::class.java, "ReceiptBrandingResponse")
                    assertOptionalFieldType(this, branding, ENDPOINT, "customBackgroundColorHex", String::class.java, "ReceiptBrandingResponse")
                    assertOptionalFieldType(this, branding, ENDPOINT, "customCardTopBorderColorHex", String::class.java, "ReceiptBrandingResponse")
                    assertOptionalFieldType(this, branding, ENDPOINT, "footerMsg", String::class.java, "ReceiptBrandingResponse")
                    assertOptionalFieldType(this, branding, ENDPOINT, "headerLogoUrl", String::class.java, "ReceiptBrandingResponse")
                    assertOptionalFieldType(this, branding, ENDPOINT, "headerMsg", String::class.java, "ReceiptBrandingResponse")
                }
            }
        }.assertAll()
    }

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Метод GET /kkm не возвращает поля вне Swagger-контракта")
    fun shouldNotReturnFieldsOutsideSwaggerContract() {
        val json = getKkmJson()
        val response = json.getMap<String, Any?>("")
        val items = json.getList<Map<String, Any?>>("items")

        SoftAssertions().apply {
            assertOnlySwaggerFields(this, response, ENDPOINT, "PaginatedResponseKkmResponse", PAGINATED_KKM_RESPONSE_FIELDS)

            items.forEachIndexed { index, item ->
                assertOnlySwaggerFields(this, item, ENDPOINT, "items[$index]", KKM_RESPONSE_FIELDS)

                item.objectField("ofdServiceInfo")?.let { ofdServiceInfo ->
                    assertOnlySwaggerFields(this, ofdServiceInfo, ENDPOINT, "items[$index].ofdServiceInfo", OFD_SERVICE_INFO_RESPONSE_FIELDS)
                }

                item.objectField("branding")?.let { branding ->
                    assertOnlySwaggerFields(this, branding, ENDPOINT, "items[$index].branding", RECEIPT_BRANDING_RESPONSE_FIELDS)
                }
            }
        }.assertAll()
    }

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Метод GET /kkm возвращает допустимые бизнес-значения")
    fun shouldReturnExpectedBusinessValues() {
        val json = getKkmJson()
        val total = json.getInt("total")
        val limit = json.getInt("limit")
        val offset = json.getInt("offset")
        val hasMore = json.getBoolean("hasMore")
        val items = json.getList<Map<String, Any?>>("items")

        Allure.step("Проверяем бизнес-правила пагинации: total, limit, offset, размер items и hasMore")
        SoftAssertions().apply {
            assertThat(total).isGreaterThanOrEqualTo(0)
            assertThat(limit).isGreaterThanOrEqualTo(0)
            assertThat(offset).isGreaterThanOrEqualTo(0)
            assertThat(items.size).isLessThanOrEqualTo(limit)
            assertThat(hasMore).isEqualTo(offset + items.size < total)

            Allure.step("Проверяем бизнес-значения и enum-поля каждой ККМ")
            items.forEach { item ->
                assertThat(item["kkmId"] as? String).isNotBlank()
                assertThat(item["mode"] as? String)
                    .isNotBlank()
                    .isIn(ApiEnumValues.KKM_MODES)
                assertThat(item["state"] as? String)
                    .isNotBlank()
                    .isIn(ApiEnumValues.KKM_STATES)
                assertThat(item["createdAt"] as? Long).isGreaterThan(0)
                assertThat(item["updatedAt"] as? Long).isGreaterThan(0)
                assertOptionalEnumValue(this, item, ENDPOINT, "taxRegime", ApiEnumValues.TAX_REGIMES)
                assertOptionalEnumValue(this, item, ENDPOINT, "defaultVatGroup", ApiEnumValues.VAT_GROUPS)
                assertOptionalEnumValue(this, item, ENDPOINT, "ofdEnvironment", ApiEnumValues.OFD_ENVIRONMENTS)
                assertOptionalEnumValue(this, item, ENDPOINT, "ofdId", ApiEnumValues.OFD_IDS)

                item.objectField("branding")?.let { branding ->
                    assertRequiredEnumValue(this, branding, ENDPOINT, "language", "ReceiptBrandingResponse", ApiEnumValues.BRANDING_LANGUAGES)
                    assertRequiredEnumValue(this, branding, ENDPOINT, "themeColor", "ReceiptBrandingResponse", ApiEnumValues.BRANDING_THEME_COLORS)
                }
            }
        }.assertAll()
    }

    @Test
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("Метод GET /kkm применяет limit и offset")
    fun shouldApplyLimitAndOffset() {
        val json = getKkmJson(limit = 1, offset = 0)
        val items = json.getList<Map<String, Any?>>("items")

        SoftAssertions().apply {
            assertThat(json.getInt("limit")).isEqualTo(1)
            assertThat(json.getInt("offset")).isEqualTo(0)
            assertThat(items).hasSizeLessThanOrEqualTo(1)
        }.assertAll()
    }

    @Test
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("Метод GET /kkm фильтрует список по состоянию ККМ")
    fun shouldFilterByState() {
        val existingKkm = firstKkmOrSkip()
        val state = existingKkm["state"] as String
        val json = getKkmJson(state = state)
        val items = json.getList<Map<String, Any?>>("items")

        assertThat(items)
            .allSatisfy { item -> assertThat(item["state"]).isEqualTo(state) }
    }

    @Test
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("Метод GET /kkm ищет ККМ по заводскому номеру")
    fun shouldSearchByFactoryNumber() {
        Allure.step("Получаем контрольную ККМ из списка GET /kkm?limit=1&offset=0")
        val existingKkm = firstKkmOrSkip()
        val factoryNumber = existingKkm["factoryNumber"] as? String

        assumeTrue(!factoryNumber.isNullOrBlank(), "В первой ККМ нет factoryNumber для проверки search")

        Allure.step("Ищем ККМ по factoryNumber='$factoryNumber' через GET /kkm?search=$factoryNumber")
        val json = getKkmJson(search = factoryNumber)
        val items = json.getList<Map<String, Any?>>("items")

        assertThat(items)
            .withFailMessage(
                "Поиск GET /kkm?search=%s не вернул ККМ с factoryNumber='%s'. " +
                    "Сначала тест получил существующую ККМ с таким factoryNumber, затем выполнил поиск по этому значению.",
                factoryNumber,
                factoryNumber,
            )
            .isNotEmpty()
            .anySatisfy { item ->
                assertThat(item["factoryNumber"])
                    .withFailMessage(
                        "Поиск GET /kkm?search=%s вернул ККМ без ожидаемого factoryNumber='%s'.",
                        factoryNumber,
                        factoryNumber,
                    )
                    .isEqualTo(factoryNumber)
            }
    }

    @ParameterizedTest(name = "sortBy={0}, order={1}")
    @CsvSource(
        "createdAt, ASC",
        "createdAt, DESC",
        "updatedAt, ASC",
        "updatedAt, DESC",
        "state, ASC",
        "state, DESC",
        "registrationNumber, ASC",
        "registrationNumber, DESC",
    )
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("Метод GET /kkm принимает поддерживаемые параметры сортировки")
    fun shouldAcceptSupportedSorting(sortBy: String, order: String) {
        getKkmJson(sortBy = sortBy, order = order)
    }

    @ParameterizedTest(name = "{0}={1} возвращает 400")
    @CsvSource(
        "limit, abc",
        "limit, -1",
        "offset, abc",
        "offset, -1",
        "sortBy, unsupportedField",
        "order, INVALID",
    )
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("Метод GET /kkm возвращает 400 для невалидных query-параметров")
    fun shouldReturnBadRequestForInvalidQueryParams(paramName: String, paramValue: String) {
        superkassa.request()
            .queryParam(paramName, paramValue)
            .`when`()
            .get("/kkm")
            .then()
            .shouldHaveStatus(400, "невалидный запрос")
            .contentType(ContentType.JSON)
    }

    @ParameterizedTest(name = "HTTP {0} /kkm возвращает 405")
    @EnumSource(value = Method::class, names = ["POST", "PUT", "PATCH", "DELETE"])
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("Метод /kkm возвращает 405 для HTTP-методов кроме GET")
    fun shouldReturnMethodNotAllowedForNonGetMethods(method: Method) {
        superkassa.request()
            .`when`()
            .request(method, "/kkm")
            .then()
            .shouldHaveStatus(405, "неподдерживаемый HTTP-метод")
    }

    private fun firstKkmOrSkip(): Map<String, Any?> {
        val items = getKkmJson(limit = 1, offset = 0).getList<Map<String, Any?>>("items")

        assumeTrue(items.isNotEmpty(), "В системе нет ККМ для проверки фильтрации")

        return items.first()
    }

    private fun getKkmJson(
        limit: Int? = null,
        offset: Int? = null,
        state: String? = null,
        search: String? = null,
        sortBy: String? = null,
        order: String? = null,
    ): JsonPath {
        val request = superkassa.request()

        limit?.let { request.queryParam("limit", it) }
        offset?.let { request.queryParam("offset", it) }
        state?.let { request.queryParam("state", it) }
        search?.let { request.queryParam("search", it) }
        sortBy?.let { request.queryParam("sortBy", it) }
        order?.let { request.queryParam("order", it) }

        val response: Response = request
            .`when`()
            .get("/kkm")
            .then()
            .shouldHaveStatus(200, "успешный запрос")
            .contentType(ContentType.JSON)
            .extract()
            .response()

        return response.jsonPath()
    }

    private fun assertFieldType(
        softly: SoftAssertions,
        item: Map<String, Any?>,
        endpoint: String,
        fieldName: String,
        expectedType: Class<*>,
        schemaName: String,
    ) {
        softly.assertThat(item)
            .withFailMessage(ApiContractErrorMessages.requiredFieldWithTypeMissing(endpoint, fieldName, expectedType.simpleName, schemaName))
            .containsKey(fieldName)

        softly.assertThat(item[fieldName])
            .withFailMessage(ApiContractErrorMessages.fieldTypeMismatch(endpoint, fieldName, expectedType.simpleName, schemaName))
            .isInstanceOf(expectedType)
    }

    private fun assertIntegerFieldType(
        softly: SoftAssertions,
        item: Map<String, Any?>,
        endpoint: String,
        fieldName: String,
        schemaName: String,
    ) {
        softly.assertThat(item)
            .withFailMessage(ApiContractErrorMessages.requiredFieldWithTypeMissing(endpoint, fieldName, "Integer", schemaName))
            .containsKey(fieldName)

        softly.assertThat(item[fieldName])
            .withFailMessage(ApiContractErrorMessages.fieldTypeMismatch(endpoint, fieldName, "Integer", schemaName))
            .isInstanceOf(Int::class.javaObjectType)
    }

    private fun assertOptionalFieldType(
        softly: SoftAssertions,
        item: Map<String, Any?>,
        endpoint: String,
        fieldName: String,
        expectedType: Class<*>,
        schemaName: String,
    ) {
        val fieldValue = item[fieldName] ?: return

        softly.assertThat(fieldValue)
            .withFailMessage(ApiContractErrorMessages.optionalFieldTypeMismatch(endpoint, fieldName, expectedType.simpleName, schemaName))
            .isInstanceOf(expectedType)
    }

    private fun assertOnlySwaggerFields(
        softly: SoftAssertions,
        item: Map<String, Any?>,
        endpoint: String,
        objectPath: String,
        allowedFields: Set<String>,
    ) {
        val unexpectedFields = item.keys - allowedFields

        softly.assertThat(unexpectedFields)
            .withFailMessage(ApiContractErrorMessages.unexpectedSwaggerFields(endpoint, objectPath, unexpectedFields))
            .isEmpty()
    }

    private fun assertRequiredEnumValue(
        softly: SoftAssertions,
        item: Map<String, Any?>,
        endpoint: String,
        fieldName: String,
        schemaName: String,
        supportedValues: Set<String>,
    ) {
        val fieldValue = item[fieldName] as? String

        softly.assertThat(fieldValue)
            .withFailMessage(ApiContractErrorMessages.requiredEnumMissing(endpoint, fieldName, schemaName))
            .isNotBlank()

        softly.assertThat(fieldValue)
            .withFailMessage(ApiContractErrorMessages.enumUnsupported(endpoint, fieldName, fieldValue, supportedValues))
            .isIn(supportedValues)
    }

    private fun assertOptionalEnumValue(
        softly: SoftAssertions,
        item: Map<String, Any?>,
        endpoint: String,
        fieldName: String,
        supportedValues: Set<String>,
    ) {
        val fieldValue = item[fieldName] as? String ?: return

        softly.assertThat(fieldValue)
            .withFailMessage(ApiContractErrorMessages.enumUnsupported(endpoint, fieldName, fieldValue, supportedValues))
            .isIn(supportedValues)
    }

    @Suppress("UNCHECKED_CAST")
    private fun Map<String, Any?>.objectField(fieldName: String): Map<String, Any?>? = this[fieldName] as? Map<String, Any?>

    private companion object {
        const val ENDPOINT = "GET /kkm"

        val PAGINATED_KKM_RESPONSE_FIELDS = setOf("hasMore", "items", "limit", "offset", "total")
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
