package kz.superkassa.tests.api.kkm.diagnostics.auth

import io.qameta.allure.Feature
import io.qameta.allure.Owner
import io.qameta.allure.Severity
import io.qameta.allure.SeverityLevel
import io.qameta.allure.Story
import io.restassured.http.ContentType
import kz.superkassa.tests.framework.assertions.ApiContractErrorMessages
import kz.superkassa.tests.framework.kkm.KkmAuthenticatedTest
import kz.superkassa.tests.framework.reporting.reportStep
import kz.superkassa.tests.framework.tags.ApiSmoke
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.ResourceAccessMode
import org.junit.jupiter.api.parallel.ResourceLock

@ApiSmoke
@Feature("API")
@Story("POST /kkm/{kkmId}/ofd/auth")
@Owner("Pavel Michka")
@DisplayName("POST /kkm/{kkmId}/ofd/auth: smoke-проверки получения данных авторизации ОФД")
@ResourceLock(value = "kkm-state", mode = ResourceAccessMode.READ_WRITE)
class KkmOfdAuthSmokeTest : KkmAuthenticatedTest() {
    @Test
    @Severity(SeverityLevel.BLOCKER)
    @DisplayName("Метод POST /kkm/{kkmId}/ofd/auth возвращает HTTP 200 и JSON")
    fun shouldReturnOfdAuthInfoSuccessfully() {
        getOfdAuthInfo()
    }

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Метод POST /kkm/{kkmId}/ofd/auth возвращает обязательное поле nextReqNum")
    fun shouldReturnRequiredNextRequestNumberField() {
        val response = getOfdAuthInfo()

        assertThat(response)
            .withFailMessage(
                ApiContractErrorMessages.requiredFieldMissing(
                    ENDPOINT,
                    NEXT_REQUEST_NUMBER_FIELD,
                    RESPONSE_SCHEMA,
                ),
            )
            .containsKey(NEXT_REQUEST_NUMBER_FIELD)
    }

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Метод POST /kkm/{kkmId}/ofd/auth возвращает заполненное обязательное поле nextReqNum")
    fun shouldReturnFilledNextRequestNumberField() {
        val nextReqNum = getOfdAuthInfo()[NEXT_REQUEST_NUMBER_FIELD]

        assertThat(nextReqNum)
            .withFailMessage(
                ApiContractErrorMessages.requiredFieldEmpty(
                    ENDPOINT,
                    NEXT_REQUEST_NUMBER_FIELD,
                    RESPONSE_SCHEMA,
                ),
            )
            .isNotNull()
    }

    private fun getOfdAuthInfo(): Map<String, Any?> =
        reportStep(
            "Получаем данные авторизации ОФД для ККМ kkmId='${preparedKkm.kkmId}' " +
                "через POST ${ofdAuthPath(preparedKkm.kkmId)}",
        ) {
            superkassa.request(preparedKkm.adminPin)
                .`when`()
                .post(ofdAuthPath(preparedKkm.kkmId))
                .then()
                .shouldHaveStatus(200, "получение данных авторизации ОФД")
                .contentType(ContentType.JSON)
                .extract()
                .jsonPath()
                .getMap("")
        }

    private fun ofdAuthPath(kkmId: String): String = "/kkm/$kkmId/ofd/auth"

    private companion object {
        const val ENDPOINT = "POST /kkm/{kkmId}/ofd/auth"
        const val RESPONSE_SCHEMA = "OfdAuthInfoResponse"
        const val NEXT_REQUEST_NUMBER_FIELD = "nextReqNum"
    }
}
