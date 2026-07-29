package kz.superkassa.tests.api.management.settings.tax

import io.qameta.allure.Allure
import io.qameta.allure.Feature
import io.qameta.allure.Owner
import io.qameta.allure.Severity
import io.qameta.allure.SeverityLevel
import io.qameta.allure.Story
import io.restassured.http.ContentType
import io.restassured.response.Response
import kz.superkassa.tests.framework.assertions.ApiContractErrorMessages
import kz.superkassa.tests.framework.kkm.KkmAuthenticatedTest
import kz.superkassa.tests.framework.kkm.PreparedKkmAuth
import kz.superkassa.tests.framework.reporting.reportStep
import kz.superkassa.tests.framework.tags.ApiSmoke
import org.assertj.core.api.SoftAssertions
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.ResourceAccessMode
import org.junit.jupiter.api.parallel.ResourceLock

@ApiSmoke
@Feature("API")
@Story("PUT /kkm/{kkmId}/settings/tax")
@Owner("Pavel Michka")
@DisplayName("PUT /kkm/{kkmId}/settings/tax: smoke-проверки налоговых настроек ККМ")
@ResourceLock(value = "kkm-state", mode = ResourceAccessMode.READ_WRITE)
@Suppress("NonAsciiCharacters")
class KkmTaxSettingsSmokeTest : KkmAuthenticatedTest() {
    private var kkmToExitAfterTest: PreparedKkmAuth? = null

    @BeforeEach
    fun `Готовим ККМ к обновлению налоговых настроек`() {
        enterProgramming(preparedKkm)
    }

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

    @Test
    @Severity(SeverityLevel.BLOCKER)
    @DisplayName("Метод PUT /kkm/{kkmId}/settings/tax возвращает HTTP 200 и JSON")
    fun shouldUpdateTaxSettingsSuccessfully() {
        updateTaxSettings()
    }

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Метод PUT /kkm/{kkmId}/settings/tax возвращает обязательные поля ККМ")
    fun shouldReturnRequiredKkmFields() {
        val response = updateTaxSettings()

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
    @DisplayName("Метод PUT /kkm/{kkmId}/settings/tax возвращает заполненные обязательные поля ККМ")
    fun shouldReturnFilledRequiredKkmFields() {
        val response = updateTaxSettings()

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

    private fun updateTaxSettings(): Map<String, Any?> =
        reportStep("Обновляем налоговые настройки через PUT ${taxSettingsPath(preparedKkm.kkmId)}") {
            superkassa.request(preparedKkm.adminPin)
                .body(validTaxSettingsBody())
                .`when`()
                .put(taxSettingsPath(preparedKkm.kkmId))
                .then()
                .shouldHaveStatus(200, "обновление налоговых настроек ККМ")
                .contentType(ContentType.JSON)
                .extract()
                .jsonPath()
                .getMap("")
        }

    private fun validTaxSettingsBody(): Map<String, String> = mapOf(
        "taxRegime" to "NO_VAT",
        "defaultVatGroup" to "NO_VAT",
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

    private fun taxSettingsPath(kkmId: String): String = "/kkm/$kkmId/settings/tax"

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
        const val ENDPOINT = "PUT /kkm/{kkmId}/settings/tax"
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
