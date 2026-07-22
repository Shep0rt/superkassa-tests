package kz.superkassa.tests.api.diagnostics

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
import org.assertj.core.api.SoftAssertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@ApiSmoke
@Feature("API")
@Story("GET /health")
@Owner("Pavel Michka")
@DisplayName("GET /health: smoke-проверки состояния сервиса")
@Suppress("SameParameterValue")
class HealthSmokeTest : BaseApiTest() {
    @Test
    @Severity(SeverityLevel.BLOCKER)
    @DisplayName("Метод GET /health возвращает HTTP 200 и JSON")
    fun shouldReturnHealthSuccessfully() {
        reportStep("Получаем состояние сервиса через GET /health") {
            superkassa.request()
                .`when`()
                .get("/health")
                .then()
                .shouldHaveStatus(200, "успешный запрос")
                .contentType(ContentType.JSON)
        }
    }

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Метод GET /health возвращает обязательные поля")
    fun shouldReturnRequiredFields() {
        val json = getHealthJson()

        SoftAssertions().apply {
            assertRequiredFieldPresent(this, value(json, "storage"), ENDPOINT, "storage", "HealthResponse")
            assertRequiredFieldPresent(this, value(json, "status"), ENDPOINT, "status", "HealthResponse")
        }.assertAll()
    }

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Метод GET /health возвращает заполненные обязательные поля")
    fun shouldReturnFilledRequiredFields() {
        val json = getHealthJson()

        SoftAssertions().apply {
            assertRequiredFieldFilled(this, value(json, "storage"), ENDPOINT, "storage", "HealthResponse")
            assertRequiredFieldFilled(this, value(json, "status"), ENDPOINT, "status", "HealthResponse")
        }.assertAll()
    }

    private fun getHealthJson(): JsonPath {
        val response: Response = reportStep("Получаем состояние сервиса через GET /health") {
            superkassa.request()
                .`when`()
                .get("/health")
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

        if (value is String) {
            softly.assertThat(value)
                .withFailMessage(message)
                .isNotBlank()
        }
    }

    private companion object {
        const val ENDPOINT = "GET /health"
    }
}
