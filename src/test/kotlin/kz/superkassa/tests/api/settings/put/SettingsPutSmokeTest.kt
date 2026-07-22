package kz.superkassa.tests.api.settings.put

import io.qameta.allure.Allure
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
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.SoftAssertions
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@ApiSmoke
@Feature("API")
@Story("PUT /settings")
@Owner("Pavel Michka")
@DisplayName("PUT /settings: smoke-проверки обновления настроек Superkassa")
@Suppress("SameParameterValue", "NonAsciiCharacters")
class SettingsPutSmokeTest : BaseApiTest() {
    private lateinit var currentSettings: Map<String, Any?>

    @BeforeEach
    fun `Получаем текущие настройки и проверяем возможность изменения`() {
        currentSettings = getCurrentSettings()
        assumeSettingsChangesAllowed(currentSettings)
    }

    @Test
    @Severity(SeverityLevel.BLOCKER)
    @DisplayName("Метод PUT /settings принимает текущие настройки и возвращает HTTP 200 и JSON")
    fun shouldUpdateSettingsWithCurrentConfigurationSuccessfully() {
        reportStep("Отправляем текущие настройки через PUT /settings") {
            superkassa.request()
                .body(currentSettings)
                .`when`()
                .put("/settings")
                .then()
                .shouldHaveStatus(200, "успешный запрос")
                .contentType(ContentType.JSON)
        }
    }

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Метод PUT /settings возвращает обязательные поля после обновления")
    fun shouldReturnRequiredFieldsAfterUpdate() {
        val json = putSettingsJson(currentSettings)

        SoftAssertions().apply {
            assertRequiredFieldPresent(this, value(json, "allowChanges"), ENDPOINT, "allowChanges", "CoreSettingsDto")
            assertRequiredFieldPresent(
                this,
                value(json, "defaultAdminName"),
                ENDPOINT,
                "defaultAdminName",
                "CoreSettingsDto"
            )
            assertRequiredFieldPresent(
                this,
                value(json, "defaultAdminPin"),
                ENDPOINT,
                "defaultAdminPin",
                "CoreSettingsDto"
            )
            assertRequiredFieldPresent(
                this,
                value(json, "defaultCashierName"),
                ENDPOINT,
                "defaultCashierName",
                "CoreSettingsDto"
            )
            assertRequiredFieldPresent(
                this,
                value(json, "defaultCashierPin"),
                ENDPOINT,
                "defaultCashierPin",
                "CoreSettingsDto"
            )
            assertRequiredFieldPresent(
                this,
                value(json, "deliveryChannels"),
                ENDPOINT,
                "deliveryChannels",
                "CoreSettingsDto"
            )
            assertRequiredFieldPresent(this, value(json, "mode"), ENDPOINT, "mode", "CoreSettingsDto")
            assertRequiredFieldPresent(this, value(json, "nodeId"), ENDPOINT, "nodeId", "CoreSettingsDto")
            assertRequiredFieldPresent(
                this,
                value(json, "ofdProtocolVersion"),
                ENDPOINT,
                "ofdProtocolVersion",
                "CoreSettingsDto"
            )
            assertRequiredFieldPresent(
                this,
                value(json, "ofdReconnectIntervalSeconds"),
                ENDPOINT,
                "ofdReconnectIntervalSeconds",
                "CoreSettingsDto"
            )
            assertRequiredFieldPresent(
                this,
                value(json, "ofdTimeoutSeconds"),
                ENDPOINT,
                "ofdTimeoutSeconds",
                "CoreSettingsDto"
            )
            assertRequiredFieldPresent(this, value(json, "storage"), ENDPOINT, "storage", "CoreSettingsDto")

            assertRequiredFieldPresent(
                this,
                value(json, "storage.engine"),
                ENDPOINT,
                "storage.engine",
                "StorageSettingsDto"
            )
            assertRequiredFieldPresent(
                this,
                value(json, "storage.jdbcUrl"),
                ENDPOINT,
                "storage.jdbcUrl",
                "StorageSettingsDto"
            )

            json.optionalObject("delivery")?.let {
                assertRequiredFieldPresent(
                    this,
                    value(json, "delivery.channels"),
                    ENDPOINT,
                    "delivery.channels",
                    "DeliverySettingsDto"
                )
            }

            json.optionalList<Map<String, Any?>>("delivery.channels")?.forEachIndexed { index, channel ->
                assertRequiredFieldPresent(
                    this,
                    channel["channel"],
                    ENDPOINT,
                    "delivery.channels[$index].channel",
                    "DeliveryChannelSettingsDto",
                )
                assertRequiredFieldPresent(
                    this,
                    channel["documentFormat"],
                    ENDPOINT,
                    "delivery.channels[$index].documentFormat",
                    "DeliveryChannelSettingsDto",
                )
                assertRequiredFieldPresent(
                    this,
                    channel["enabled"],
                    ENDPOINT,
                    "delivery.channels[$index].enabled",
                    "DeliveryChannelSettingsDto",
                )
                assertRequiredFieldPresent(
                    this,
                    channel["payloadType"],
                    ENDPOINT,
                    "delivery.channels[$index].payloadType",
                    "DeliveryChannelSettingsDto",
                )
            }
        }.assertAll()
    }

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Метод PUT /settings возвращает заполненные обязательные поля после обновления")
    fun shouldReturnFilledRequiredFieldsAfterUpdate() {
        val json = putSettingsJson(currentSettings)

        SoftAssertions().apply {
            assertRequiredFieldFilled(this, value(json, "allowChanges"), ENDPOINT, "allowChanges", "CoreSettingsDto")
            assertRequiredFieldFilled(
                this,
                value(json, "defaultAdminName"),
                ENDPOINT,
                "defaultAdminName",
                "CoreSettingsDto"
            )
            assertRequiredFieldFilled(
                this,
                value(json, "defaultAdminPin"),
                ENDPOINT,
                "defaultAdminPin",
                "CoreSettingsDto"
            )
            assertRequiredFieldFilled(
                this,
                value(json, "defaultCashierName"),
                ENDPOINT,
                "defaultCashierName",
                "CoreSettingsDto"
            )
            assertRequiredFieldFilled(
                this,
                value(json, "defaultCashierPin"),
                ENDPOINT,
                "defaultCashierPin",
                "CoreSettingsDto"
            )
            assertRequiredFieldFilled(
                this,
                value(json, "deliveryChannels"),
                ENDPOINT,
                "deliveryChannels",
                "CoreSettingsDto"
            )
            assertRequiredFieldFilled(this, value(json, "mode"), ENDPOINT, "mode", "CoreSettingsDto")
            assertRequiredFieldFilled(this, value(json, "nodeId"), ENDPOINT, "nodeId", "CoreSettingsDto")
            assertRequiredFieldFilled(
                this,
                value(json, "ofdProtocolVersion"),
                ENDPOINT,
                "ofdProtocolVersion",
                "CoreSettingsDto"
            )
            assertRequiredFieldFilled(
                this,
                value(json, "ofdReconnectIntervalSeconds"),
                ENDPOINT,
                "ofdReconnectIntervalSeconds",
                "CoreSettingsDto"
            )
            assertRequiredFieldFilled(
                this,
                value(json, "ofdTimeoutSeconds"),
                ENDPOINT,
                "ofdTimeoutSeconds",
                "CoreSettingsDto"
            )
            assertRequiredFieldFilled(this, value(json, "storage"), ENDPOINT, "storage", "CoreSettingsDto")

            assertRequiredFieldFilled(
                this,
                value(json, "storage.engine"),
                ENDPOINT,
                "storage.engine",
                "StorageSettingsDto"
            )
            assertRequiredFieldFilled(
                this,
                value(json, "storage.jdbcUrl"),
                ENDPOINT,
                "storage.jdbcUrl",
                "StorageSettingsDto"
            )

            json.optionalObject("delivery")?.let {
                assertRequiredFieldFilled(
                    this,
                    value(json, "delivery.channels"),
                    ENDPOINT,
                    "delivery.channels",
                    "DeliverySettingsDto"
                )
            }

            json.optionalList<Map<String, Any?>>("delivery.channels")?.forEachIndexed { index, channel ->
                assertRequiredFieldFilled(
                    this,
                    channel["channel"],
                    ENDPOINT,
                    "delivery.channels[$index].channel",
                    "DeliveryChannelSettingsDto"
                )
                assertRequiredFieldFilled(
                    this,
                    channel["documentFormat"],
                    ENDPOINT,
                    "delivery.channels[$index].documentFormat",
                    "DeliveryChannelSettingsDto",
                )
                assertRequiredFieldFilled(
                    this,
                    channel["enabled"],
                    ENDPOINT,
                    "delivery.channels[$index].enabled",
                    "DeliveryChannelSettingsDto"
                )
                assertRequiredFieldFilled(
                    this,
                    channel["payloadType"],
                    ENDPOINT,
                    "delivery.channels[$index].payloadType",
                    "DeliveryChannelSettingsDto"
                )
            }
        }.assertAll()
    }

    private fun getCurrentSettings(): Map<String, Any?> {
        val response: Response = reportStep("Получаем текущие настройки через GET /settings") {
            superkassa.request()
                .`when`()
                .get("/settings")
                .then()
                .shouldHaveStatus(200, "успешный запрос")
                .contentType(ContentType.JSON)
                .extract()
                .response()
        }

        return response.jsonPath().getMap("")
    }

    private fun putSettingsJson(settings: Map<String, Any?>): JsonPath {
        val response: Response = reportStep("Отправляем текущие настройки через PUT /settings") {
            superkassa.request()
                .body(settings)
                .`when`()
                .put("/settings")
                .then()
                .shouldHaveStatus(200, "успешный запрос")
                .contentType(ContentType.JSON)
                .extract()
                .response()
        }

        return response.jsonPath()
    }

    private fun assumeSettingsChangesAllowed(settings: Map<String, Any?>) {
        assertThat(settings)
            .withFailMessage(
                "Контракт API нарушен: в ответе GET /settings отсутствует обязательное поле 'allowChanges'. " +
                        "Поле 'allowChanges' помечено как required в Swagger-схеме CoreSettingsDto.",
            )
            .containsKey("allowChanges")

        val allowChanges = settings["allowChanges"]

        assertThat(allowChanges)
            .withFailMessage(
                "Контракт API нарушен: поле 'allowChanges' в ответе GET /settings должно иметь тип 'Boolean' " +
                        "согласно Swagger-схеме CoreSettingsDto, сейчас вернулось значение '%s'.",
                allowChanges,
            )
            .isInstanceOf(Boolean::class.javaObjectType)

        Allure.step("Проверяем предусловие smoke-теста: allowChanges=true")
        assumeTrue(
            allowChanges == true,
            "PUT /settings доступен только если в текущих настройках allowChanges=true, сейчас вернулся allowChanges=$allowChanges",
        )
    }

    private fun value(json: JsonPath, path: String): Any? = json.get(path)

    @Suppress("UNCHECKED_CAST")
    private fun JsonPath.optionalObject(path: String): Map<String, Any?>? = get(path) as? Map<String, Any?>

    @Suppress("UNCHECKED_CAST")
    private fun <T> JsonPath.optionalList(path: String): List<T>? = get(path) as? List<T>

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
        const val ENDPOINT = "PUT /settings"
    }
}
