package kz.superkassa.tests.api.about.kkm

import io.qameta.allure.Feature
import io.qameta.allure.Owner
import io.qameta.allure.Severity
import io.qameta.allure.SeverityLevel
import io.qameta.allure.Story
import io.restassured.http.ContentType
import io.restassured.path.json.JsonPath
import io.restassured.response.Response
import kz.superkassa.tests.framework.BaseApiTest
import kz.superkassa.tests.framework.assertions.ApiContractErrorMessages
import kz.superkassa.tests.framework.reporting.reportStep
import kz.superkassa.tests.framework.tags.ApiSmoke
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.SoftAssertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@ApiSmoke
@Feature("API")
@Story("GET /kkm")
@Owner("Pavel Michka")
@DisplayName("GET /kkm: smoke-проверки списка ККМ")
@Suppress("SameParameterValue")
class KkmSmokeTest : BaseApiTest() {
    @Test
    @Severity(SeverityLevel.BLOCKER)
    @DisplayName("Метод GET /kkm возвращает HTTP 200 и JSON")
    fun shouldReturnKkmListSuccessfully() {
        reportStep("Получаем список ККМ через GET /kkm") {
            superkassa.request()
                .`when`()
                .get("/kkm")
                .then()
                .shouldHaveStatus(200, "успешный запрос")
                .contentType(ContentType.JSON)
        }
    }

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Метод GET /kkm возвращает обязательные поля пагинации")
    fun shouldReturnRequiredPaginationFields() {
        val json = getKkmJson()

        SoftAssertions().apply {
            assertRequiredFieldPresent(this, value(json, "items"), ENDPOINT, "items", "PaginatedResponseKkmResponse")
            assertRequiredFieldPresent(this, value(json, "total"), ENDPOINT, "total", "PaginatedResponseKkmResponse")
            assertRequiredFieldPresent(this, value(json, "limit"), ENDPOINT, "limit", "PaginatedResponseKkmResponse")
            assertRequiredFieldPresent(this, value(json, "offset"), ENDPOINT, "offset", "PaginatedResponseKkmResponse")
            assertRequiredFieldPresent(this, value(json, "hasMore"), ENDPOINT, "hasMore", "PaginatedResponseKkmResponse")
        }.assertAll()
    }

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Метод GET /kkm возвращает заполненные обязательные поля пагинации")
    fun shouldReturnFilledRequiredPaginationFields() {
        val json = getKkmJson()

        SoftAssertions().apply {
            assertRequiredPaginationFieldFilled(this, json, "items")
            assertRequiredFieldFilled(this, value(json, "total"), ENDPOINT, "total", "PaginatedResponseKkmResponse")
            assertRequiredFieldFilled(this, value(json, "limit"), ENDPOINT, "limit", "PaginatedResponseKkmResponse")
            assertRequiredFieldFilled(this, value(json, "offset"), ENDPOINT, "offset", "PaginatedResponseKkmResponse")
            assertRequiredFieldFilled(this, value(json, "hasMore"), ENDPOINT, "hasMore", "PaginatedResponseKkmResponse")
        }.assertAll()
    }

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Метод GET /kkm возвращает обязательные поля каждой ККМ")
    fun shouldReturnRequiredKkmItemFields() {
        val json = getKkmJson()
        val items = json.getList<Map<String, Any?>>("items")

        assertThat(items).isNotNull()

        SoftAssertions().apply {
            items.forEachIndexed { index, item ->
                assertRequiredFieldPresent(this, item["autoCloseShift"], ENDPOINT, "items[$index].autoCloseShift", "KkmResponse")
                assertRequiredFieldPresent(this, item["createdAt"], ENDPOINT, "items[$index].createdAt", "KkmResponse")
                assertRequiredFieldPresent(this, item["kkmId"], ENDPOINT, "items[$index].kkmId", "KkmResponse")
                assertRequiredFieldPresent(this, item["mode"], ENDPOINT, "items[$index].mode", "KkmResponse")
                assertRequiredFieldPresent(this, item["state"], ENDPOINT, "items[$index].state", "KkmResponse")
                assertRequiredFieldPresent(this, item["updatedAt"], ENDPOINT, "items[$index].updatedAt", "KkmResponse")

                item.objectField("ofdServiceInfo")?.let { ofdServiceInfo ->
                    assertRequiredFieldPresent(this, ofdServiceInfo["geoLatitude"], ENDPOINT, "ofdServiceInfo.geoLatitude", "OfdServiceInfoResponse")
                    assertRequiredFieldPresent(this, ofdServiceInfo["geoLongitude"], ENDPOINT, "ofdServiceInfo.geoLongitude", "OfdServiceInfoResponse")
                    assertRequiredFieldPresent(this, ofdServiceInfo["geoSource"], ENDPOINT, "ofdServiceInfo.geoSource", "OfdServiceInfoResponse")
                    assertRequiredFieldPresent(this, ofdServiceInfo["orgAddress"], ENDPOINT, "ofdServiceInfo.orgAddress", "OfdServiceInfoResponse")
                    assertRequiredFieldPresent(this, ofdServiceInfo["orgAddressKz"], ENDPOINT, "ofdServiceInfo.orgAddressKz", "OfdServiceInfoResponse")
                    assertRequiredFieldPresent(this, ofdServiceInfo["orgInn"], ENDPOINT, "ofdServiceInfo.orgInn", "OfdServiceInfoResponse")
                    assertRequiredFieldPresent(this, ofdServiceInfo["orgOkved"], ENDPOINT, "ofdServiceInfo.orgOkved", "OfdServiceInfoResponse")
                    assertRequiredFieldPresent(this, ofdServiceInfo["orgTitle"], ENDPOINT, "ofdServiceInfo.orgTitle", "OfdServiceInfoResponse")
                }

                item.objectField("branding")?.let { branding ->
                    assertRequiredFieldPresent(this, branding["language"], ENDPOINT, "branding.language", "ReceiptBrandingResponse")
                    assertRequiredFieldPresent(this, branding["ofdTicketAds"], ENDPOINT, "branding.ofdTicketAds", "ReceiptBrandingResponse")
                    assertRequiredFieldPresent(this, branding["paperWidthMm"], ENDPOINT, "branding.paperWidthMm", "ReceiptBrandingResponse")
                    assertRequiredFieldPresent(this, branding["printOfdTicketAds"], ENDPOINT, "branding.printOfdTicketAds", "ReceiptBrandingResponse")
                    assertRequiredFieldPresent(this, branding["themeColor"], ENDPOINT, "branding.themeColor", "ReceiptBrandingResponse")
                    assertRequiredFieldPresent(this, branding["useForceDarkTheme"], ENDPOINT, "branding.useForceDarkTheme", "ReceiptBrandingResponse")
                }
            }
        }.assertAll()
    }

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Метод GET /kkm возвращает заполненные обязательные поля")
    fun shouldReturnFilledRequiredFields() {
        val json = getKkmJson()
        val items = json.getList<Map<String, Any?>>("items")

        assertThat(items).isNotNull()

        SoftAssertions().apply {
            items.forEachIndexed { index, item ->
                assertRequiredFieldFilled(this, item["autoCloseShift"], ENDPOINT, "items[$index].autoCloseShift", "KkmResponse")
                assertRequiredFieldFilled(this, item["createdAt"], ENDPOINT, "items[$index].createdAt", "KkmResponse")
                assertRequiredFieldFilled(this, item["kkmId"], ENDPOINT, "items[$index].kkmId", "KkmResponse")
                assertRequiredFieldFilled(this, item["mode"], ENDPOINT, "items[$index].mode", "KkmResponse")
                assertRequiredFieldFilled(this, item["state"], ENDPOINT, "items[$index].state", "KkmResponse")
                assertRequiredFieldFilled(this, item["updatedAt"], ENDPOINT, "items[$index].updatedAt", "KkmResponse")

                item.objectField("ofdServiceInfo")?.let { ofdServiceInfo ->
                    assertRequiredFieldFilled(this, ofdServiceInfo["geoLatitude"], ENDPOINT, "ofdServiceInfo.geoLatitude", "OfdServiceInfoResponse")
                    assertRequiredFieldFilled(this, ofdServiceInfo["geoLongitude"], ENDPOINT, "ofdServiceInfo.geoLongitude", "OfdServiceInfoResponse")
                    assertRequiredFieldFilled(this, ofdServiceInfo["geoSource"], ENDPOINT, "ofdServiceInfo.geoSource", "OfdServiceInfoResponse")
                    assertRequiredFieldFilled(this, ofdServiceInfo["orgAddress"], ENDPOINT, "ofdServiceInfo.orgAddress", "OfdServiceInfoResponse")
                    assertRequiredFieldFilled(this, ofdServiceInfo["orgAddressKz"], ENDPOINT, "ofdServiceInfo.orgAddressKz", "OfdServiceInfoResponse")
                    assertRequiredFieldFilled(this, ofdServiceInfo["orgInn"], ENDPOINT, "ofdServiceInfo.orgInn", "OfdServiceInfoResponse")
                    assertRequiredFieldFilled(this, ofdServiceInfo["orgOkved"], ENDPOINT, "ofdServiceInfo.orgOkved", "OfdServiceInfoResponse")
                    assertRequiredFieldFilled(this, ofdServiceInfo["orgTitle"], ENDPOINT, "ofdServiceInfo.orgTitle", "OfdServiceInfoResponse")
                }

                item.objectField("branding")?.let { branding ->
                    assertRequiredFieldFilled(this, branding["language"], ENDPOINT, "branding.language", "ReceiptBrandingResponse")
                    assertRequiredFieldFilled(this, branding["ofdTicketAds"], ENDPOINT, "branding.ofdTicketAds", "ReceiptBrandingResponse")
                    assertRequiredFieldFilled(this, branding["paperWidthMm"], ENDPOINT, "branding.paperWidthMm", "ReceiptBrandingResponse")
                    assertRequiredFieldFilled(this, branding["printOfdTicketAds"], ENDPOINT, "branding.printOfdTicketAds", "ReceiptBrandingResponse")
                    assertRequiredFieldFilled(this, branding["themeColor"], ENDPOINT, "branding.themeColor", "ReceiptBrandingResponse")
                    assertRequiredFieldFilled(this, branding["useForceDarkTheme"], ENDPOINT, "branding.useForceDarkTheme", "ReceiptBrandingResponse")
                }
            }
        }.assertAll()
    }

    private fun getKkmJson(): JsonPath {
        val response: Response = reportStep("Получаем список ККМ через GET /kkm") {
            superkassa.request()
                .`when`()
                .get("/kkm")
                .then()
                .shouldHaveStatus(200, "успешный запрос")
                .contentType(ContentType.JSON)
                .extract()
                .response()
        }

        return response.jsonPath()
    }

    private fun value(json: JsonPath, path: String): Any? = json.get(path)

    private fun assertRequiredFieldPresent(
        softly: SoftAssertions,
        value: Any?,
        endpoint: String,
        fieldName: String,
        schemaName: String,
    ) {
        softly.assertThat(value)
            .withFailMessage(ApiContractErrorMessages.requiredFieldMissing(endpoint, fieldName, schemaName))
            .isNotNull()
    }

    private fun assertRequiredPaginationFieldFilled(softly: SoftAssertions, json: JsonPath, fieldName: String) {
        val fieldValue = value(json, fieldName)
        val message = ApiContractErrorMessages.requiredFieldEmpty(ENDPOINT, fieldName, "PaginatedResponseKkmResponse")

        softly.assertThat(fieldValue)
            .withFailMessage(message)
            .isNotNull()

        if (fieldValue is String) {
            softly.assertThat(fieldValue)
                .withFailMessage(message)
                .isNotBlank()
        }
    }

    private fun assertRequiredFieldFilled(
        softly: SoftAssertions,
        value: Any?,
        endpoint: String,
        fieldName: String,
        schemaName: String,
    ) {
        val message = ApiContractErrorMessages.requiredFieldEmpty(endpoint, fieldName, schemaName)

        softly.assertThat(value)
            .withFailMessage(message)
            .isNotNull()

        when (value) {
            is String -> softly.assertThat(value).withFailMessage(message).isNotBlank()
            is Collection<*> -> softly.assertThat(value).withFailMessage(message).isNotEmpty()
            is Map<*, *> -> softly.assertThat(value).withFailMessage(message).isNotEmpty()
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun Map<String, Any?>.objectField(fieldName: String): Map<String, Any?>? = this[fieldName] as? Map<String, Any?>

    private companion object {
        const val ENDPOINT = "GET /kkm"
    }
}
