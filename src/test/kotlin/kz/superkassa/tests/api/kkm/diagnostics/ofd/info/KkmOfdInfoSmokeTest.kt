package kz.superkassa.tests.api.kkm.diagnostics.ofd.info

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
@Story("GET /kkm/{kkmId}/ofd/info")
@Owner("Pavel Michka")
@DisplayName("GET /kkm/{kkmId}/ofd/info: smoke-проверки получения информации о ККМ из ОФД")
@ResourceLock(value = "kkm-state", mode = ResourceAccessMode.READ_WRITE)
@Suppress("NonAsciiCharacters")
class KkmOfdInfoSmokeTest : BaseApiTest() {
    private lateinit var kkmId: String

    @BeforeEach
    fun `Получаем контрольную ККМ`() {
        kkmId = firstKkmIdOrSkip()
    }

    @Test
    @Severity(SeverityLevel.BLOCKER)
    @DisplayName("Метод GET /kkm/{kkmId}/ofd/info без авторизации возвращает HTTP 200 и JSON")
    fun shouldReturnOfdInfoSuccessfullyWithoutAuthorization() {
        getOfdInfo()
    }

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Метод GET /kkm/{kkmId}/ofd/info возвращает обязательное поле status")
    fun shouldReturnRequiredStatusField() {
        val response = getOfdInfo()

        assertThat(response)
            .withFailMessage(ApiContractErrorMessages.requiredFieldMissing(ENDPOINT, STATUS_FIELD, RESPONSE_SCHEMA))
            .containsKey(STATUS_FIELD)
    }

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Метод GET /kkm/{kkmId}/ofd/info возвращает заполненное обязательное поле status")
    fun shouldReturnFilledStatusField() {
        val status = getOfdInfo()[STATUS_FIELD]
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

    private fun getOfdInfo(): Map<String, Any?> =
        reportStep("Получаем информацию о ККМ kkmId='$kkmId' из ОФД через GET ${ofdInfoPath(kkmId)} без авторизации") {
            superkassa.requestWithoutAuthorization()
                .`when`()
                .get(ofdInfoPath(kkmId))
                .then()
                .shouldHaveStatus(200, "публичное получение информации о ККМ из ОФД")
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
                .shouldHaveStatus(200, "получение контрольной ККМ для проверки GET /kkm/{kkmId}/ofd/info")
                .contentType(ContentType.JSON)
                .extract()
                .response()
        }

        val items = response.jsonPath().getList<Map<String, Any?>>("items").orEmpty()
        assumeTrue(items.isNotEmpty(), "В системе нет ККМ для проверки GET /kkm/{kkmId}/ofd/info")

        val kkmId = items.first()["kkmId"] as? String
        assumeTrue(!kkmId.isNullOrBlank(), "В контрольной ККМ отсутствует заполненный kkmId")

        return kkmId!!
    }

    private fun ofdInfoPath(kkmId: String): String = "/kkm/$kkmId/ofd/info"

    private companion object {
        const val ENDPOINT = "GET /kkm/{kkmId}/ofd/info"
        const val RESPONSE_SCHEMA = "OfdCommandResponse"
        const val STATUS_FIELD = "status"
    }
}
