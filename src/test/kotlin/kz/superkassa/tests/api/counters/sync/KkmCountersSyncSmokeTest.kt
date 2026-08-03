package kz.superkassa.tests.api.counters.sync

import io.qameta.allure.Feature
import io.qameta.allure.Owner
import io.qameta.allure.Severity
import io.qameta.allure.SeverityLevel
import io.qameta.allure.Story
import io.restassured.http.ContentType
import io.restassured.response.Response
import kz.superkassa.tests.framework.assertions.ApiContractErrorMessages
import kz.superkassa.tests.framework.kkm.KkmAuthenticatedTest
import kz.superkassa.tests.framework.reporting.reportStep
import kz.superkassa.tests.framework.tags.ApiSmoke
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.SoftAssertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.ResourceAccessMode
import org.junit.jupiter.api.parallel.ResourceLock

@ApiSmoke
@Feature("API")
@Story("POST /kkm/{kkmId}/ofd/counters/sync")
@Owner("Pavel Michka")
@DisplayName("POST /kkm/{kkmId}/ofd/counters/sync: smoke-проверки синхронизации счетчиков ККМ с ОФД")
@ResourceLock(value = "kkm-counters", mode = ResourceAccessMode.READ_WRITE)
@ResourceLock(value = "kkm-state", mode = ResourceAccessMode.READ)
class KkmCountersSyncSmokeTest : KkmAuthenticatedTest() {
    @Test
    @Severity(SeverityLevel.BLOCKER)
    @DisplayName("Метод POST /kkm/{kkmId}/ofd/counters/sync возвращает HTTP 200 и JSON-объект")
    fun shouldSyncCountersSuccessfully() {
        val responseBody = syncCountersResponse().jsonPath().get<Any?>("")

        assertThat(responseBody)
            .withFailMessage(
                ApiContractErrorMessages.responseBodyStructureMismatch(
                    ENDPOINT,
                    "JSON-объектом",
                    RESPONSE_SCHEMA,
                ),
            )
            .isInstanceOf(Map::class.java)
    }

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Метод POST /kkm/{kkmId}/ofd/counters/sync возвращает обязательное поле status")
    fun shouldReturnRequiredStatusField() {
        val response = syncCounters()

        assertThat(response)
            .withFailMessage(ApiContractErrorMessages.requiredFieldMissing(ENDPOINT, STATUS_FIELD, RESPONSE_SCHEMA))
            .containsKey(STATUS_FIELD)
    }

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Метод POST /kkm/{kkmId}/ofd/counters/sync возвращает заполненное обязательное поле status")
    fun shouldReturnFilledStatusField() {
        val status = syncCounters()[STATUS_FIELD]
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

    private fun syncCounters(): Map<String, Any?> =
        syncCountersResponse().jsonPath().getMap("")

    private fun syncCountersResponse(): Response =
        reportStep(
            "Синхронизируем счетчики ККМ kkmId='${preparedKkm.kkmId}' с ОФД через POST " +
                countersSyncPath(preparedKkm.kkmId),
        ) {
            superkassa.request(preparedKkm.adminPin)
                .`when`()
                .post(countersSyncPath(preparedKkm.kkmId))
                .then()
                .shouldHaveStatus(200, "синхронизация счетчиков ККМ с ОФД")
                .contentType(ContentType.JSON)
                .extract()
                .response()
        }

    private fun countersSyncPath(kkmId: String): String = "/kkm/$kkmId/ofd/counters/sync"

    private companion object {
        const val ENDPOINT = "POST /kkm/{kkmId}/ofd/counters/sync"
        const val RESPONSE_SCHEMA = "OfdCommandResponse"
        const val STATUS_FIELD = "status"
    }
}
