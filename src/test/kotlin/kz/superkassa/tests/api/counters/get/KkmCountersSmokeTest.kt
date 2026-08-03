package kz.superkassa.tests.api.counters.get

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
@Story("GET /kkm/{kkmId}/counters")
@Owner("Pavel Michka")
@DisplayName("GET /kkm/{kkmId}/counters: smoke-проверки получения счетчиков ККМ")
@ResourceLock(value = "kkm-counters", mode = ResourceAccessMode.READ)
class KkmCountersSmokeTest : KkmAuthenticatedTest() {
    @Test
    @Severity(SeverityLevel.BLOCKER)
    @DisplayName("Метод GET /kkm/{kkmId}/counters возвращает HTTP 200 и JSON-массив")
    fun shouldReturnCountersSuccessfully() {
        val responseBody = getCountersResponse().jsonPath().get<Any?>("")

        assertThat(responseBody)
            .withFailMessage(
                ApiContractErrorMessages.responseBodyStructureMismatch(
                    ENDPOINT,
                    "JSON-массивом объектов",
                    RESPONSE_SCHEMA,
                ),
            )
            .isInstanceOf(List::class.java)
    }

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Метод GET /kkm/{kkmId}/counters возвращает обязательные поля каждого счетчика")
    fun shouldReturnRequiredCounterFields() {
        val counters = getCounters()

        SoftAssertions().apply {
            counters.forEachIndexed { index, counter ->
                REQUIRED_FIELDS.forEach { fieldName ->
                    assertRequiredFieldPresent(this, counter, index, fieldName)
                }
            }
        }.assertAll()
    }

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Метод GET /kkm/{kkmId}/counters возвращает заполненные обязательные поля каждого счетчика")
    fun shouldReturnFilledRequiredCounterFields() {
        val counters = getCounters()

        SoftAssertions().apply {
            counters.forEachIndexed { index, counter ->
                REQUIRED_FIELDS.forEach { fieldName ->
                    assertRequiredFieldFilled(this, counter[fieldName], index, fieldName)
                }
            }
        }.assertAll()
    }

    private fun getCounters(): List<Map<String, Any?>> =
        getCountersResponse().jsonPath().getList("")

    private fun getCountersResponse(): Response =
        reportStep(
            "Получаем счетчики ККМ kkmId='${preparedKkm.kkmId}' через GET ${countersPath(preparedKkm.kkmId)}",
        ) {
            superkassa.request(preparedKkm.adminPin)
                .`when`()
                .get(countersPath(preparedKkm.kkmId))
                .then()
                .shouldHaveStatus(200, "получение счетчиков ККМ")
                .contentType(ContentType.JSON)
                .extract()
                .response()
        }

    private fun assertRequiredFieldPresent(
        softly: SoftAssertions,
        counter: Map<String, Any?>,
        index: Int,
        fieldName: String,
    ) {
        val fieldPath = "counters[$index].$fieldName"

        softly.assertThat(counter)
            .withFailMessage(ApiContractErrorMessages.requiredFieldMissing(ENDPOINT, fieldPath, RESPONSE_SCHEMA))
            .containsKey(fieldName)
    }

    private fun assertRequiredFieldFilled(
        softly: SoftAssertions,
        value: Any?,
        index: Int,
        fieldName: String,
    ) {
        val fieldPath = "counters[$index].$fieldName"
        val message = ApiContractErrorMessages.requiredFieldEmpty(ENDPOINT, fieldPath, RESPONSE_SCHEMA)

        softly.assertThat(value)
            .withFailMessage(message)
            .isNotNull()

        if (value is String) {
            softly.assertThat(value)
                .withFailMessage(message)
                .isNotBlank()
        }
    }

    private fun countersPath(kkmId: String): String = "/kkm/$kkmId/counters"

    private companion object {
        const val ENDPOINT = "GET /kkm/{kkmId}/counters"
        const val RESPONSE_SCHEMA = "CounterSnapshotResponse"

        val REQUIRED_FIELDS = setOf(
            "key",
            "scope",
            "updatedAt",
            "value",
        )
    }
}
