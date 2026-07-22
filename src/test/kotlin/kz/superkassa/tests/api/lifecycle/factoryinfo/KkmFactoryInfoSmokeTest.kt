package kz.superkassa.tests.api.lifecycle.factoryinfo

import io.qameta.allure.Feature
import io.qameta.allure.Owner
import io.qameta.allure.Severity
import io.qameta.allure.SeverityLevel
import io.qameta.allure.Story
import io.restassured.http.ContentType
import kz.superkassa.tests.framework.BaseApiTest
import kz.superkassa.tests.framework.assertions.ApiContractErrorMessages
import kz.superkassa.tests.framework.reporting.reportStep
import kz.superkassa.tests.framework.tags.ApiSmoke
import org.assertj.core.api.SoftAssertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@ApiSmoke
@Feature("API")
@Story("GET /kkm/factory-info")
@Owner("Pavel Michka")
@DisplayName("GET /kkm/factory-info: smoke-проверки генерации заводской информации ККМ")
class KkmFactoryInfoSmokeTest : BaseApiTest() {
    @Test
    @Severity(SeverityLevel.BLOCKER)
    @DisplayName("Метод GET /kkm/factory-info без авторизации возвращает HTTP 200 и JSON")
    fun shouldReturnFactoryInfoSuccessfullyWithoutAuthorization() {
        getFactoryInfo()
    }

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Метод GET /kkm/factory-info возвращает обязательные поля заводской информации ККМ")
    fun shouldReturnRequiredFactoryInfoFields() {
        val response = getFactoryInfo()

        SoftAssertions().apply {
            assertRequiredFieldPresent(this, response, "factoryNumber")
            assertRequiredFieldPresent(this, response, "manufactureYear")
        }.assertAll()
    }

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Метод GET /kkm/factory-info возвращает заполненные обязательные поля заводской информации ККМ")
    fun shouldReturnFilledRequiredFactoryInfoFields() {
        val response = getFactoryInfo()

        SoftAssertions().apply {
            assertRequiredFieldFilled(this, response["factoryNumber"], "factoryNumber")
            assertRequiredFieldFilled(this, response["manufactureYear"], "manufactureYear")
        }.assertAll()
    }

    private fun getFactoryInfo(): Map<String, Any?> =
        reportStep("Получаем заводской номер и год выпуска через GET /kkm/factory-info без авторизации") {
            superkassa.requestWithoutAuthorization()
                .`when`()
                .get("/kkm/factory-info")
                .then()
                .shouldHaveStatus(200, "публичное получение заводской информации ККМ")
                .contentType(ContentType.JSON)
                .extract()
                .jsonPath()
                .getMap("")
        }

    private fun assertRequiredFieldPresent(
        softly: SoftAssertions,
        response: Map<String, Any?>,
        fieldName: String,
    ) {
        softly.assertThat(response)
            .withFailMessage(ApiContractErrorMessages.requiredFieldMissing(ENDPOINT, fieldName, RESPONSE_SCHEMA))
            .containsKey(fieldName)
    }

    private fun assertRequiredFieldFilled(
        softly: SoftAssertions,
        value: Any?,
        fieldName: String,
    ) {
        val message = ApiContractErrorMessages.requiredFieldEmpty(ENDPOINT, fieldName, RESPONSE_SCHEMA)

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
        const val ENDPOINT = "GET /kkm/factory-info"
        const val RESPONSE_SCHEMA = "FactoryNumberResponse"
    }
}
