package kz.superkassa.tests.api.programming.exit

import io.qameta.allure.Allure
import io.qameta.allure.Feature
import io.qameta.allure.Owner
import io.qameta.allure.Severity
import io.qameta.allure.SeverityLevel
import io.qameta.allure.Story
import io.restassured.http.ContentType
import io.restassured.path.json.JsonPath
import io.restassured.response.Response
import kz.superkassa.tests.framework.BaseTest
import kz.superkassa.tests.framework.assertions.ApiContractErrorMessages
import kz.superkassa.tests.framework.kkm.PreparedKkmAuth
import kz.superkassa.tests.framework.tags.ApiSmoke
import org.assertj.core.api.SoftAssertions
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@ApiSmoke
@Feature("API")
@Story("POST /kkm/{kkmId}/programming/exit")
@Owner("Pavel Michka")
@DisplayName("POST /kkm/{kkmId}/programming/exit: smoke-проверки выхода из режима программирования")
@Suppress("SameParameterValue", "NonAsciiCharacters")
class KkmProgrammingExitSmokeTest : BaseTest() {
    private var kkmToExitAfterTest: PreparedKkmAuth? = null

    @AfterEach
    fun `Возвращаем ККМ из режима программирования после проверки`() {
        val preparedKkm = kkmToExitAfterTest ?: return
        kkmToExitAfterTest = null
        exitProgrammingForCleanup(preparedKkm)
    }

    @Test
    @Severity(SeverityLevel.BLOCKER)
    @DisplayName("Метод POST /kkm/{kkmId}/programming/exit возвращает HTTP 200 и JSON")
    fun shouldExitProgrammingSuccessfully() {
        val preparedKkm = kkmAuth.prepareFirstKkmAdminPin()

        enterProgramming(preparedKkm)

        exitProgrammingJson(preparedKkm)
    }

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Метод POST /kkm/{kkmId}/programming/exit возвращает обязательные поля ККМ")
    fun shouldReturnRequiredKkmFieldsAfterExitProgramming() {
        val preparedKkm = kkmAuth.prepareFirstKkmAdminPin()

        enterProgramming(preparedKkm)

        val json = exitProgrammingJson(preparedKkm)
        val response = json.getMap<String, Any?>("")

        SoftAssertions().apply {
            assertRequiredFieldPresent(this, response["autoCloseShift"], ENDPOINT, "autoCloseShift", "KkmResponse")
            assertRequiredFieldPresent(this, response["createdAt"], ENDPOINT, "createdAt", "KkmResponse")
            assertRequiredFieldPresent(this, response["kkmId"], ENDPOINT, "kkmId", "KkmResponse")
            assertRequiredFieldPresent(this, response["mode"], ENDPOINT, "mode", "KkmResponse")
            assertRequiredFieldPresent(this, response["state"], ENDPOINT, "state", "KkmResponse")
            assertRequiredFieldPresent(this, response["updatedAt"], ENDPOINT, "updatedAt", "KkmResponse")

            response.objectField("ofdServiceInfo")?.let { ofdServiceInfo ->
                assertRequiredFieldPresent(this, ofdServiceInfo["geoLatitude"], ENDPOINT, "ofdServiceInfo.geoLatitude", "OfdServiceInfoResponse")
                assertRequiredFieldPresent(this, ofdServiceInfo["geoLongitude"], ENDPOINT, "ofdServiceInfo.geoLongitude", "OfdServiceInfoResponse")
                assertRequiredFieldPresent(this, ofdServiceInfo["geoSource"], ENDPOINT, "ofdServiceInfo.geoSource", "OfdServiceInfoResponse")
                assertRequiredFieldPresent(this, ofdServiceInfo["orgAddress"], ENDPOINT, "ofdServiceInfo.orgAddress", "OfdServiceInfoResponse")
                assertRequiredFieldPresent(this, ofdServiceInfo["orgAddressKz"], ENDPOINT, "ofdServiceInfo.orgAddressKz", "OfdServiceInfoResponse")
                assertRequiredFieldPresent(this, ofdServiceInfo["orgInn"], ENDPOINT, "ofdServiceInfo.orgInn", "OfdServiceInfoResponse")
                assertRequiredFieldPresent(this, ofdServiceInfo["orgOkved"], ENDPOINT, "ofdServiceInfo.orgOkved", "OfdServiceInfoResponse")
                assertRequiredFieldPresent(this, ofdServiceInfo["orgTitle"], ENDPOINT, "ofdServiceInfo.orgTitle", "OfdServiceInfoResponse")
            }

            response.objectField("branding")?.let { branding ->
                assertRequiredFieldPresent(this, branding["language"], ENDPOINT, "branding.language", "ReceiptBrandingResponse")
                assertRequiredFieldPresent(this, branding["ofdTicketAds"], ENDPOINT, "branding.ofdTicketAds", "ReceiptBrandingResponse")
                assertRequiredFieldPresent(this, branding["paperWidthMm"], ENDPOINT, "branding.paperWidthMm", "ReceiptBrandingResponse")
                assertRequiredFieldPresent(this, branding["printOfdTicketAds"], ENDPOINT, "branding.printOfdTicketAds", "ReceiptBrandingResponse")
                assertRequiredFieldPresent(this, branding["themeColor"], ENDPOINT, "branding.themeColor", "ReceiptBrandingResponse")
                assertRequiredFieldPresent(this, branding["useForceDarkTheme"], ENDPOINT, "branding.useForceDarkTheme", "ReceiptBrandingResponse")
            }
        }.assertAll()
    }

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Метод POST /kkm/{kkmId}/programming/exit возвращает заполненные обязательные поля ККМ")
    fun shouldReturnFilledRequiredKkmFieldsAfterExitProgramming() {
        val preparedKkm = kkmAuth.prepareFirstKkmAdminPin()

        enterProgramming(preparedKkm)

        val json = exitProgrammingJson(preparedKkm)
        val response = json.getMap<String, Any?>("")

        SoftAssertions().apply {
            assertRequiredFieldFilled(this, response["autoCloseShift"], ENDPOINT, "autoCloseShift", "KkmResponse")
            assertRequiredFieldFilled(this, response["createdAt"], ENDPOINT, "createdAt", "KkmResponse")
            assertRequiredFieldFilled(this, response["kkmId"], ENDPOINT, "kkmId", "KkmResponse")
            assertRequiredFieldFilled(this, response["mode"], ENDPOINT, "mode", "KkmResponse")
            assertRequiredFieldFilled(this, response["state"], ENDPOINT, "state", "KkmResponse")
            assertRequiredFieldFilled(this, response["updatedAt"], ENDPOINT, "updatedAt", "KkmResponse")

            response.objectField("ofdServiceInfo")?.let { ofdServiceInfo ->
                assertRequiredFieldFilled(this, ofdServiceInfo["geoLatitude"], ENDPOINT, "ofdServiceInfo.geoLatitude", "OfdServiceInfoResponse")
                assertRequiredFieldFilled(this, ofdServiceInfo["geoLongitude"], ENDPOINT, "ofdServiceInfo.geoLongitude", "OfdServiceInfoResponse")
                assertRequiredFieldFilled(this, ofdServiceInfo["geoSource"], ENDPOINT, "ofdServiceInfo.geoSource", "OfdServiceInfoResponse")
                assertRequiredFieldFilled(this, ofdServiceInfo["orgAddress"], ENDPOINT, "ofdServiceInfo.orgAddress", "OfdServiceInfoResponse")
                assertRequiredFieldFilled(this, ofdServiceInfo["orgAddressKz"], ENDPOINT, "ofdServiceInfo.orgAddressKz", "OfdServiceInfoResponse")
                assertRequiredFieldFilled(this, ofdServiceInfo["orgInn"], ENDPOINT, "ofdServiceInfo.orgInn", "OfdServiceInfoResponse")
                assertRequiredFieldFilled(this, ofdServiceInfo["orgOkved"], ENDPOINT, "ofdServiceInfo.orgOkved", "OfdServiceInfoResponse")
                assertRequiredFieldFilled(this, ofdServiceInfo["orgTitle"], ENDPOINT, "ofdServiceInfo.orgTitle", "OfdServiceInfoResponse")
            }

            response.objectField("branding")?.let { branding ->
                assertRequiredFieldFilled(this, branding["language"], ENDPOINT, "branding.language", "ReceiptBrandingResponse")
                assertRequiredFieldFilled(this, branding["ofdTicketAds"], ENDPOINT, "branding.ofdTicketAds", "ReceiptBrandingResponse")
                assertRequiredFieldFilled(this, branding["paperWidthMm"], ENDPOINT, "branding.paperWidthMm", "ReceiptBrandingResponse")
                assertRequiredFieldFilled(this, branding["printOfdTicketAds"], ENDPOINT, "branding.printOfdTicketAds", "ReceiptBrandingResponse")
                assertRequiredFieldFilled(this, branding["themeColor"], ENDPOINT, "branding.themeColor", "ReceiptBrandingResponse")
                assertRequiredFieldFilled(this, branding["useForceDarkTheme"], ENDPOINT, "branding.useForceDarkTheme", "ReceiptBrandingResponse")
            }
        }.assertAll()
    }

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Метод POST /kkm/{kkmId}/programming/exit возвращает выбранную ККМ в рабочий режим")
    fun shouldReturnSameKkmInRegistrationMode() {
        val preparedKkm = kkmAuth.prepareFirstKkmAdminPin()

        enterProgramming(preparedKkm)

        val json = exitProgrammingJson(preparedKkm)

        SoftAssertions().apply {
            assertThat(value(json, "kkmId") as? String)
                .withFailMessage(
                    "Функциональность API нарушена: POST /kkm/%s/programming/exit вернул данные другой ККМ. " +
                        "Ожидался kkmId='%s'.",
                    preparedKkm.kkmId,
                    preparedKkm.kkmId,
                )
                .isEqualTo(preparedKkm.kkmId)
            assertThat(value(json, "mode") as? String)
                .withFailMessage(
                    "Функциональность API нарушена: POST /kkm/%s/programming/exit должен вернуть mode='REGISTRATION'.",
                    preparedKkm.kkmId,
                )
                .isEqualTo("REGISTRATION")
        }.assertAll()
    }

    private fun enterProgramming(preparedKkm: PreparedKkmAuth) {
        val enterProgramming: Allure.ThrowableRunnable<Unit> = Allure.ThrowableRunnable {
            superkassa.request(preparedKkm.adminPin)
                .`when`()
                .post("/kkm/{kkmId}/programming/enter", preparedKkm.kkmId)
                .then()
                .shouldHaveStatus(200, "предусловие: вход ККМ в режим программирования")
                .contentType(ContentType.JSON)
        }

        Allure.step("Готовим предусловие: переводим ККМ kkmId='${preparedKkm.kkmId}' в режим программирования", enterProgramming)
        kkmToExitAfterTest = preparedKkm
    }

    private fun exitProgrammingJson(preparedKkm: PreparedKkmAuth): JsonPath {
        val exitProgramming: Allure.ThrowableRunnable<Response> = Allure.ThrowableRunnable {
            superkassa.request(preparedKkm.adminPin)
                .`when`()
                .post(exitProgrammingPath(preparedKkm.kkmId))
                .then()
                .shouldHaveStatus(200, "успешный выход из режима программирования")
                .contentType(ContentType.JSON)
                .extract()
                .response()
        }

        val response: Response = Allure.step("Выводим ККМ kkmId='${preparedKkm.kkmId}' из режима программирования", exitProgramming)
        kkmToExitAfterTest = null

        return response.jsonPath()
    }

    private fun exitProgrammingForCleanup(preparedKkm: PreparedKkmAuth) {
        val exitProgramming: Allure.ThrowableRunnable<Unit> = Allure.ThrowableRunnable {
            superkassa.request(preparedKkm.adminPin)
                .`when`()
                .post(exitProgrammingPath(preparedKkm.kkmId))
                .then()
                .shouldHaveStatus(200, "cleanup: возврат ККМ из режима программирования")
                .contentType(ContentType.JSON)
        }

        Allure.step("Возвращаем ККМ kkmId='${preparedKkm.kkmId}' из режима программирования после проверки", exitProgramming)
    }

    private fun exitProgrammingPath(kkmId: String): String = "/kkm/$kkmId/programming/exit"

    private fun value(json: JsonPath, path: String): Any? = json.get(path)

    @Suppress("UNCHECKED_CAST")
    private fun Map<String, Any?>.objectField(fieldName: String): Map<String, Any?>? = this[fieldName] as? Map<String, Any?>

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

    private companion object {
        const val ENDPOINT = "POST /kkm/{kkmId}/programming/exit"
    }
}
