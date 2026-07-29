package kz.superkassa.tests.api.kkm.diagnostics.ofd.ping

import io.qameta.allure.Feature
import io.qameta.allure.Owner
import io.qameta.allure.Severity
import io.qameta.allure.SeverityLevel
import io.qameta.allure.Story
import io.restassured.http.ContentType
import io.restassured.response.Response
import kz.superkassa.tests.framework.BaseApiTest
import kz.superkassa.tests.framework.assertions.ApiContractErrorMessages
import kz.superkassa.tests.framework.reporting.reportStep
import kz.superkassa.tests.framework.tags.ApiSmoke
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.SoftAssertions
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.ResourceAccessMode
import org.junit.jupiter.api.parallel.ResourceLock

@ApiSmoke
@Feature("API")
@Story("GET /kkm/{kkmId}/ofd/ping")
@Owner("Pavel Michka")
@DisplayName("GET /kkm/{kkmId}/ofd/ping: smoke-проверки связи ККМ с ОФД")
@ResourceLock(value = "kkm-state", mode = ResourceAccessMode.READ_WRITE)
@Suppress("NonAsciiCharacters")
class KkmOfdPingSmokeTest : BaseApiTest() {
    private lateinit var kkmId: String

    @BeforeEach
    fun `Получаем контрольную ККМ`() {
        kkmId = firstKkmIdOrSkip()
    }

    @Test
    @Severity(SeverityLevel.BLOCKER)
    @DisplayName("Метод GET /kkm/{kkmId}/ofd/ping без авторизации возвращает HTTP 200 и JSON")
    fun shouldCheckOfdConnectionSuccessfullyWithoutAuthorization() {
        getOfdPing()
    }

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Метод GET /kkm/{kkmId}/ofd/ping возвращает обязательное поле status")
    fun shouldReturnRequiredStatusField() {
        val response = getOfdPing()

        assertThat(response)
            .withFailMessage(ApiContractErrorMessages.requiredFieldMissing(ENDPOINT, STATUS_FIELD, RESPONSE_SCHEMA))
            .containsKey(STATUS_FIELD)
    }

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Метод GET /kkm/{kkmId}/ofd/ping возвращает заполненное обязательное поле status")
    fun shouldReturnFilledStatusField() {
        val status = getOfdPing()[STATUS_FIELD]
        val message = ApiContractErrorMessages.requiredFieldEmpty(ENDPOINT, STATUS_FIELD, RESPONSE_SCHEMA)

        SoftAssertions().apply {
            assertThat(status)
                .withFailMessage(message)
                .isNotNull()

            if (status is String) {
                assertThat(status)
                    .withFailMessage(message)
                    .isNotBlank()
            }
        }.assertAll()
    }

    private fun getOfdPing(): Map<String, Any?> =
        reportStep("Проверяем связь ККМ kkmId='$kkmId' с ОФД через GET ${ofdPingPath(kkmId)} без авторизации") {
            superkassa.requestWithoutAuthorization()
                .`when`()
                .get(ofdPingPath(kkmId))
                .then()
                .shouldHaveStatus(200, "публичная проверка связи ККМ с ОФД")
                .contentType(ContentType.JSON)
                .extract()
                .jsonPath()
                .getMap("")
        }

    private fun firstKkmIdOrSkip(): String {
        val response: Response = reportStep("Получаем контрольную ККМ из списка GET /kkm?limit=1&offset=0") {
            superkassa.requestWithoutAuthorization()
                .queryParam("limit", 1)
                .queryParam("offset", 0)
                .`when`()
                .get("/kkm")
                .then()
                .shouldHaveStatus(200, "получение контрольной ККМ для проверки GET /kkm/{kkmId}/ofd/ping")
                .contentType(ContentType.JSON)
                .extract()
                .response()
        }

        val items = response.jsonPath().getList<Map<String, Any?>>("items").orEmpty()
        assumeTrue(items.isNotEmpty(), "В системе нет ККМ для проверки GET /kkm/{kkmId}/ofd/ping")

        val kkmId = items.first()["kkmId"] as? String
        assumeTrue(!kkmId.isNullOrBlank(), "В контрольной ККМ отсутствует заполненный kkmId")

        return kkmId!!
    }

    private fun ofdPingPath(kkmId: String): String = "/kkm/$kkmId/ofd/ping"

    private companion object {
        const val ENDPOINT = "GET /kkm/{kkmId}/ofd/ping"
        const val RESPONSE_SCHEMA = "OfdCommandResponse"
        const val STATUS_FIELD = "status"
    }
}
