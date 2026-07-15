package kz.superkassa.tests.api.about.info

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
import kz.superkassa.tests.framework.tags.ApiSmoke
import org.assertj.core.api.SoftAssertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@ApiSmoke
@Feature("API")
@Story("GET /info")
@Owner("Pavel Michka")
@DisplayName("GET /info: smoke-проверки информации о Superkassa")
@Suppress("SameParameterValue")
class InfoSmokeTest : BaseTest() {
    @Test
    @Severity(SeverityLevel.BLOCKER)
    @DisplayName("Метод GET /info возвращает HTTP 200 и JSON")
    fun shouldReturnInfoSuccessfully() {
        superkassa.request()
            .`when`()
            .get("/info")
            .then()
            .shouldHaveStatus(200, "успешный запрос")
            .contentType(ContentType.JSON)
    }

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Метод GET /info возвращает все обязательные поля")
    fun shouldReturnAllRequiredFields() {
        val json = getInfoJson()

        SoftAssertions().apply {
            assertRequiredFieldPresent(this, value(json, "name"), ENDPOINT, "name", "InfoResponse")
            assertRequiredFieldPresent(this, value(json, "version"), ENDPOINT, "version", "InfoResponse")
            assertRequiredFieldPresent(this, value(json, "mode"), ENDPOINT, "mode", "InfoResponse")
            assertRequiredFieldPresent(this, value(json, "nodeId"), ENDPOINT, "nodeId", "InfoResponse")
            assertRequiredFieldPresent(this, value(json, "ofdProtocolVersion"), ENDPOINT, "ofdProtocolVersion", "InfoResponse")

            assertRequiredFieldPresent(this, value(json, "storage"), ENDPOINT, "storage", "InfoResponse")
            assertRequiredFieldPresent(this, value(json, "storage.engine"), ENDPOINT, "storage.engine", "Storage")
            assertRequiredFieldPresent(this, value(json, "storage.jdbcUrl"), ENDPOINT, "storage.jdbcUrl", "Storage")

            assertRequiredFieldPresent(this, value(json, "statistics"), ENDPOINT, "statistics", "InfoResponse")
            assertRequiredFieldPresent(this, value(json, "statistics.registeredKkms"), ENDPOINT, "statistics.registeredKkms", "Statistics")

            assertRequiredFieldPresent(this, value(json, "features"), ENDPOINT, "features", "InfoResponse")
            assertRequiredFieldPresent(this, value(json, "features.allowSettingsChanges"), ENDPOINT, "features.allowSettingsChanges", "Features")
            assertRequiredFieldPresent(this, value(json, "features.deliveryChannels"), ENDPOINT, "features.deliveryChannels", "Features")
            assertRequiredFieldPresent(this, value(json, "features.ofdTimeoutSeconds"), ENDPOINT, "features.ofdTimeoutSeconds", "Features")
            assertRequiredFieldPresent(
                this,
                value(json, "features.ofdReconnectIntervalSeconds"),
                ENDPOINT,
                "features.ofdReconnectIntervalSeconds",
                "Features",
            )
        }.assertAll()
    }

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Метод GET /info возвращает заполненные обязательные поля")
    fun shouldReturnNonEmptyRequiredFields() {
        val json = getInfoJson()

        SoftAssertions().apply {
            assertRequiredFieldFilled(this, value(json, "name"), ENDPOINT, "name", "InfoResponse")
            assertRequiredFieldFilled(this, value(json, "version"), ENDPOINT, "version", "InfoResponse")
            assertRequiredFieldFilled(this, value(json, "mode"), ENDPOINT, "mode", "InfoResponse")
            assertRequiredFieldFilled(this, value(json, "nodeId"), ENDPOINT, "nodeId", "InfoResponse")
            assertRequiredFieldFilled(this, value(json, "ofdProtocolVersion"), ENDPOINT, "ofdProtocolVersion", "InfoResponse")

            assertRequiredFieldFilled(this, value(json, "storage.engine"), ENDPOINT, "storage.engine", "Storage")
            assertRequiredFieldFilled(this, value(json, "storage.jdbcUrl"), ENDPOINT, "storage.jdbcUrl", "Storage")

            assertThat(value(json, "statistics.registeredKkms") as? Int).isGreaterThanOrEqualTo(0)

            assertRequiredFieldFilled(this, value(json, "features.deliveryChannels"), ENDPOINT, "features.deliveryChannels", "Features")
            json.getList<String>("features.deliveryChannels")?.forEach { channel ->
                assertThat(channel)
                    .withFailMessage(
                        "Контракт API нарушен: в ответе GET /info обязательное поле 'features.deliveryChannels' содержит пустое значение.",
                    )
                    .isNotBlank()
            }
            assertThat(value(json, "features.ofdTimeoutSeconds") as? Int).isPositive()
            assertThat(value(json, "features.ofdReconnectIntervalSeconds") as? Int).isPositive()
        }.assertAll()
    }

    private fun getInfoJson(): JsonPath {
        val response: Response = superkassa.request()
            .`when`()
            .get("/info")
            .then()
            .shouldHaveStatus(200, "успешный запрос")
            .contentType(ContentType.JSON)
            .extract()
            .response()

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

        when (value) {
            is String -> softly.assertThat(value).withFailMessage(message).isNotBlank()
            is Collection<*> -> softly.assertThat(value).withFailMessage(message).isNotEmpty()
            is Map<*, *> -> softly.assertThat(value).withFailMessage(message).isNotEmpty()
        }
    }

    private companion object {
        const val ENDPOINT = "GET /info"
    }
}
