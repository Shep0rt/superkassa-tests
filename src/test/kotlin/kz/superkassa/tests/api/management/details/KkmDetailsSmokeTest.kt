package kz.superkassa.tests.api.management.details

import io.qameta.allure.Feature
import io.qameta.allure.Owner
import io.qameta.allure.Severity
import io.qameta.allure.SeverityLevel
import io.qameta.allure.Story
import io.restassured.http.ContentType
import io.restassured.response.Response
import kz.superkassa.tests.framework.BaseTest
import kz.superkassa.tests.framework.assertions.ApiContractErrorMessages
import kz.superkassa.tests.framework.reporting.reportStep
import kz.superkassa.tests.framework.tags.ApiSmoke
import org.assertj.core.api.SoftAssertions
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@ApiSmoke
@Feature("API")
@Story("GET /kkm/{kkmId}")
@Owner("Pavel Michka")
@DisplayName("GET /kkm/{kkmId}: smoke-проверки получения информации о ККМ")
class KkmDetailsSmokeTest : BaseTest() {
    @Test
    @Severity(SeverityLevel.BLOCKER)
    @DisplayName("Метод GET /kkm/{kkmId} без авторизации возвращает HTTP 200 и JSON")
    fun shouldReturnKkmSuccessfullyWithoutAuthorization() {
        getKkmDetails()
    }

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Метод GET /kkm/{kkmId} возвращает обязательные поля ККМ")
    fun shouldReturnRequiredKkmFields() {
        val response = getKkmDetails()

        SoftAssertions().apply {
            KKM_REQUIRED_FIELDS.forEach { fieldName ->
                assertRequiredFieldPresent(this, response, fieldName, fieldName, KKM_RESPONSE_SCHEMA)
            }

            response.objectField("ofdServiceInfo")?.let { ofdServiceInfo ->
                OFD_SERVICE_INFO_REQUIRED_FIELDS.forEach { fieldName ->
                    assertRequiredFieldPresent(
                        this,
                        ofdServiceInfo,
                        fieldName,
                        "ofdServiceInfo.$fieldName",
                        OFD_SERVICE_INFO_SCHEMA,
                    )
                }
            }

            response.objectField("branding")?.let { branding ->
                BRANDING_REQUIRED_FIELDS.forEach { fieldName ->
                    assertRequiredFieldPresent(
                        this,
                        branding,
                        fieldName,
                        "branding.$fieldName",
                        BRANDING_RESPONSE_SCHEMA,
                    )
                }
            }
        }.assertAll()
    }

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Метод GET /kkm/{kkmId} возвращает заполненные обязательные поля ККМ")
    fun shouldReturnFilledRequiredKkmFields() {
        val response = getKkmDetails()

        SoftAssertions().apply {
            KKM_REQUIRED_FIELDS.forEach { fieldName ->
                assertRequiredFieldFilled(this, response[fieldName], fieldName, KKM_RESPONSE_SCHEMA)
            }

            response.objectField("ofdServiceInfo")?.let { ofdServiceInfo ->
                OFD_SERVICE_INFO_REQUIRED_FIELDS.forEach { fieldName ->
                    assertRequiredFieldFilled(
                        this,
                        ofdServiceInfo[fieldName],
                        "ofdServiceInfo.$fieldName",
                        OFD_SERVICE_INFO_SCHEMA,
                    )
                }
            }

            response.objectField("branding")?.let { branding ->
                BRANDING_REQUIRED_FIELDS.forEach { fieldName ->
                    assertRequiredFieldFilled(
                        this,
                        branding[fieldName],
                        "branding.$fieldName",
                        BRANDING_RESPONSE_SCHEMA,
                    )
                }
            }
        }.assertAll()
    }

    private fun getKkmDetails(): Map<String, Any?> {
        val kkmId = firstKkmIdOrSkip()

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

    private fun assertRequiredFieldPresent(
        softly: SoftAssertions,
        response: Map<String, Any?>,
        fieldName: String,
        fieldPath: String,
        schemaName: String,
    ) {
        softly.assertThat(response)
            .withFailMessage(ApiContractErrorMessages.requiredFieldMissing(ENDPOINT, fieldPath, schemaName))
            .containsKey(fieldName)
    }

    private fun assertRequiredFieldFilled(
        softly: SoftAssertions,
        value: Any?,
        fieldPath: String,
        schemaName: String,
    ) {
        val message = ApiContractErrorMessages.requiredFieldEmpty(ENDPOINT, fieldPath, schemaName)

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
    private fun Map<String, Any?>.objectField(fieldName: String): Map<String, Any?>? =
        this[fieldName] as? Map<String, Any?>

    private companion object {
        const val ENDPOINT = "GET /kkm/{kkmId}"
        const val KKM_RESPONSE_SCHEMA = "KkmResponse"
        const val OFD_SERVICE_INFO_SCHEMA = "OfdServiceInfoResponse"
        const val BRANDING_RESPONSE_SCHEMA = "ReceiptBrandingResponse"

        val KKM_REQUIRED_FIELDS = listOf(
            "autoCloseShift",
            "createdAt",
            "kkmId",
            "mode",
            "state",
            "updatedAt",
        )

        val OFD_SERVICE_INFO_REQUIRED_FIELDS = listOf(
            "geoLatitude",
            "geoLongitude",
            "geoSource",
            "orgAddress",
            "orgAddressKz",
            "orgInn",
            "orgOkved",
            "orgTitle",
        )

        val BRANDING_REQUIRED_FIELDS = listOf(
            "language",
            "ofdTicketAds",
            "paperWidthMm",
            "printOfdTicketAds",
            "themeColor",
            "useForceDarkTheme",
        )
    }
}
