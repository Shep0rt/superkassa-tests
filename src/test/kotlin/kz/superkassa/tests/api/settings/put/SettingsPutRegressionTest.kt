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
import kz.superkassa.tests.framework.contract.ApiEnumValues
import kz.superkassa.tests.framework.assertions.ApiContractErrorMessages
import kz.superkassa.tests.framework.reporting.reportStep
import kz.superkassa.tests.framework.tags.ApiRegression
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.SoftAssertions
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.Locale

@ApiRegression
@Feature("API")
@Story("PUT /settings")
@Owner("Pavel Michka")
@DisplayName("PUT /settings: регрессионные проверки обновления настроек Superkassa")
@Suppress("SameParameterValue", "NonAsciiCharacters")
class SettingsPutRegressionTest : BaseApiTest() {
    private lateinit var currentSettings: Map<String, Any?>

    @BeforeEach
    fun `Получаем текущие настройки и проверяем возможность изменения`() {
        currentSettings = getCurrentSettings()
        assumeSettingsChangesAllowed(currentSettings)
    }

    @Nested
    @ApiRegression
    @DisplayName("Позитивные проверки PUT /settings")
    inner class PositiveRegressionTests {
        @Test
        @Severity(SeverityLevel.CRITICAL)
        @DisplayName("Метод PUT /settings возвращает поля ожидаемых типов")
        fun shouldReturnExpectedFieldTypesAfterUpdate() {
            val response = putSettingsJson(currentSettings).getMap<String, Any?>("")

            SoftAssertions().apply {
                assertFieldType(
                    this,
                    response,
                    ENDPOINT,
                    "allowChanges",
                    Boolean::class.javaObjectType,
                    "CoreSettingsDto"
                )
                assertFieldType(this, response, ENDPOINT, "defaultAdminName", String::class.java, "CoreSettingsDto")
                assertFieldType(this, response, ENDPOINT, "defaultAdminPin", String::class.java, "CoreSettingsDto")
                assertFieldType(this, response, ENDPOINT, "defaultCashierName", String::class.java, "CoreSettingsDto")
                assertFieldType(this, response, ENDPOINT, "defaultCashierPin", String::class.java, "CoreSettingsDto")
                assertFieldType(this, response, ENDPOINT, "deliveryChannels", List::class.java, "CoreSettingsDto")
                assertArrayItemsType(
                    this,
                    response["deliveryChannels"],
                    ENDPOINT,
                    "deliveryChannels",
                    String::class.java,
                    "CoreSettingsDto"
                )
                assertFieldType(this, response, ENDPOINT, "mode", String::class.java, "CoreSettingsDto")
                assertFieldType(this, response, ENDPOINT, "nodeId", String::class.java, "CoreSettingsDto")
                assertFieldType(this, response, ENDPOINT, "ofdProtocolVersion", String::class.java, "CoreSettingsDto")
                assertIntegerFieldType(this, response, ENDPOINT, "ofdReconnectIntervalSeconds", "CoreSettingsDto")
                assertIntegerFieldType(this, response, ENDPOINT, "ofdTimeoutSeconds", "CoreSettingsDto")
                assertFieldType(this, response, ENDPOINT, "storage", Map::class.java, "CoreSettingsDto")

                response.objectField("storage")?.let { storage ->
                    assertFieldType(this, storage, ENDPOINT, "engine", String::class.java, "StorageSettingsDto")
                    assertFieldType(this, storage, ENDPOINT, "jdbcUrl", String::class.java, "StorageSettingsDto")
                    assertOptionalFieldType(
                        this,
                        storage,
                        ENDPOINT,
                        "password",
                        String::class.java,
                        "StorageSettingsDto"
                    )
                    assertOptionalFieldType(this, storage, ENDPOINT, "user", String::class.java, "StorageSettingsDto")
                }

                response.objectField("delivery")?.let { delivery ->
                    assertFieldType(this, delivery, ENDPOINT, "channels", List::class.java, "DeliverySettingsDto")
                    assertDeliveryChannelsTypes(this, delivery.listField("channels"))

                    delivery.objectField("print")?.let { print ->
                        assertFieldType(
                            this,
                            print,
                            ENDPOINT,
                            "enabled",
                            Boolean::class.javaObjectType,
                            "PrintDeliverySettingsDto"
                        )
                        assertIntegerFieldType(this, print, ENDPOINT, "paperWidthMm", "PrintDeliverySettingsDto")

                        print.objectField("connection")?.let { connection ->
                            assertFieldType(
                                this,
                                connection,
                                ENDPOINT,
                                "type",
                                String::class.java,
                                "PrintConnectionSettingsDto"
                            )
                            assertOptionalFieldType(
                                this,
                                connection,
                                ENDPOINT,
                                "host",
                                String::class.java,
                                "PrintConnectionSettingsDto"
                            )
                            assertOptionalIntegerFieldType(
                                this,
                                connection,
                                ENDPOINT,
                                "port",
                                "PrintConnectionSettingsDto"
                            )
                        }
                    }

                    delivery.objectField("email")?.let { email ->
                        assertFieldType(this, email, ENDPOINT, "from", String::class.java, "EmailProviderSettingsDto")
                        assertFieldType(this, email, ENDPOINT, "host", String::class.java, "EmailProviderSettingsDto")
                        assertIntegerFieldType(this, email, ENDPOINT, "port", "EmailProviderSettingsDto")
                        assertOptionalFieldType(
                            this,
                            email,
                            ENDPOINT,
                            "password",
                            String::class.java,
                            "EmailProviderSettingsDto"
                        )
                        assertOptionalFieldType(
                            this,
                            email,
                            ENDPOINT,
                            "user",
                            String::class.java,
                            "EmailProviderSettingsDto"
                        )
                    }

                    delivery.objectField("sms")?.let { sms ->
                        assertOptionalFieldType(
                            this,
                            sms,
                            ENDPOINT,
                            "apiKey",
                            String::class.java,
                            "SmsProviderSettingsDto"
                        )
                        assertOptionalFieldType(
                            this,
                            sms,
                            ENDPOINT,
                            "providerUrl",
                            String::class.java,
                            "SmsProviderSettingsDto"
                        )
                    }

                    delivery.objectField("telegram")?.let { telegram ->
                        assertOptionalFieldType(
                            this,
                            telegram,
                            ENDPOINT,
                            "botToken",
                            String::class.java,
                            "TelegramProviderSettingsDto"
                        )
                    }

                    delivery.objectField("whatsapp")?.let { whatsapp ->
                        assertOptionalFieldType(
                            this,
                            whatsapp,
                            ENDPOINT,
                            "accessToken",
                            String::class.java,
                            "WhatsAppProviderSettingsDto"
                        )
                        assertOptionalFieldType(
                            this,
                            whatsapp,
                            ENDPOINT,
                            "phoneNumberId",
                            String::class.java,
                            "WhatsAppProviderSettingsDto"
                        )
                    }
                }
            }.assertAll()
        }

        @Test
        @Severity(SeverityLevel.CRITICAL)
        @DisplayName("Метод PUT /settings не возвращает поля вне Swagger-контракта")
        fun shouldNotReturnFieldsOutsideSwaggerContractAfterUpdate() {
            val response = putSettingsJson(currentSettings).getMap<String, Any?>("")

            SoftAssertions().apply {
                assertOnlySwaggerFields(this, response, ENDPOINT, "CoreSettingsDto", CORE_SETTINGS_FIELDS)

                response.objectField("storage")?.let { storage ->
                    assertOnlySwaggerFields(this, storage, ENDPOINT, "StorageSettingsDto", STORAGE_SETTINGS_FIELDS)
                }

                response.objectField("delivery")?.let { delivery ->
                    assertOnlySwaggerFields(this, delivery, ENDPOINT, "DeliverySettingsDto", DELIVERY_SETTINGS_FIELDS)

                    delivery.listField("channels").forEach { channel ->
                        assertOnlySwaggerFields(
                            this,
                            channel,
                            ENDPOINT,
                            "DeliveryChannelSettingsDto",
                            DELIVERY_CHANNEL_SETTINGS_FIELDS
                        )
                    }

                    delivery.objectField("print")?.let { print ->
                        assertOnlySwaggerFields(
                            this,
                            print,
                            ENDPOINT,
                            "PrintDeliverySettingsDto",
                            PRINT_DELIVERY_SETTINGS_FIELDS
                        )

                        print.objectField("connection")?.let { connection ->
                            assertOnlySwaggerFields(
                                this,
                                connection,
                                ENDPOINT,
                                "PrinterConnectionSettingsDto",
                                PRINT_CONNECTION_SETTINGS_FIELDS
                            )
                        }
                    }

                    delivery.objectField("email")?.let { email ->
                        assertOnlySwaggerFields(
                            this,
                            email,
                            ENDPOINT,
                            "EmailProviderSettingsDto",
                            EMAIL_PROVIDER_SETTINGS_FIELDS
                        )
                    }

                    delivery.objectField("sms")?.let { sms ->
                        assertOnlySwaggerFields(
                            this,
                            sms,
                            ENDPOINT,
                            "SmsProviderSettingsDto",
                            SMS_PROVIDER_SETTINGS_FIELDS
                        )
                    }

                    delivery.objectField("telegram")?.let { telegram ->
                        assertOnlySwaggerFields(
                            this,
                            telegram,
                            ENDPOINT,
                            "TelegramProviderSettingsDto",
                            TELEGRAM_PROVIDER_SETTINGS_FIELDS
                        )
                    }

                    delivery.objectField("whatsapp")?.let { whatsapp ->
                        assertOnlySwaggerFields(
                            this,
                            whatsapp,
                            ENDPOINT,
                            "WhatsAppProviderSettingsDto",
                            WHATSAPP_PROVIDER_SETTINGS_FIELDS
                        )
                    }
                }
            }.assertAll()
        }

        @Test
        @Severity(SeverityLevel.CRITICAL)
        @DisplayName("Метод PUT /settings возвращает допустимые значения enum-полей")
        fun shouldReturnExpectedEnumValuesAfterUpdate() {
            val response = putSettingsJson(currentSettings).getMap<String, Any?>("")

            SoftAssertions().apply {
                assertRequiredEnumValue(this, response, ENDPOINT, "mode", "CoreSettingsDto", ApiEnumValues.INFO_MODES)
            }.assertAll()
        }

        @Test
        @Severity(SeverityLevel.CRITICAL)
        @DisplayName("Метод PUT /settings не раскрывает секреты в JDBC URL")
        fun shouldNotExposeSecretsInJdbcUrlAfterUpdate() {
            val json = putSettingsJson(currentSettings)
            val jdbcUrl = (value(json, "storage.jdbcUrl") as? String)?.lowercase(Locale.ROOT)

            SoftAssertions().apply {
                FORBIDDEN_JDBC_URL_FRAGMENTS.forEach { fragment ->
                    assertThat(jdbcUrl)
                        .withFailMessage(
                            "Безопасность API нарушена: storage.jdbcUrl в ответе PUT /settings содержит секретный фрагмент '%s'.",
                            fragment,
                        )
                        .doesNotContain(fragment)
                }
            }.assertAll()
        }

    }

    @Nested
    @ApiRegression
    @DisplayName("Негативные проверки PUT /settings")
    inner class NegativeRegressionTests {
        @Nested
        @ApiRegression
        @DisplayName("Проверки невалидного тела запроса")
        inner class InvalidRequestBodyTests {
            @Nested
            @DisplayName("Общая структура тела запроса")
            inner class RequestBodyStructureTests {
                @Test
                @Severity(SeverityLevel.NORMAL)
                @DisplayName("Метод PUT /settings возвращает 400 для пустого тела запроса")
                fun shouldReturnBadRequestForEmptyBody() {
                    reportStep("Отправляем пустое тело через PUT /settings") {
                        superkassa.request()
                            .body(emptyMap<String, Any?>())
                            .`when`()
                            .put("/settings")
                            .then()
                            .shouldHaveStatus(400, "невалидный запрос")
                            .contentType(ContentType.JSON)
                    }
                }
            }

            @Nested
            @DisplayName("Поле mode")
            inner class ModeFieldTests {
                @Test
                @Severity(SeverityLevel.NORMAL)
                @DisplayName("Возвращает 400 для неподдерживаемого значения")
                fun shouldReturnBadRequestForUnsupportedValue() = rejectInvalidSettingsBody(
                    caseName = "mode содержит неподдерживаемое значение",
                    bodyOverride = mapOf("mode" to "UNKNOWN"),
                )
            }

            @Nested
            @DisplayName("Поле storage")
            inner class StorageFieldTests {
                @Test
                @Severity(SeverityLevel.NORMAL)
                @DisplayName("Возвращает 400, если поле отсутствует")
                fun shouldReturnBadRequestWhenMissing() = rejectInvalidSettingsBody(
                    caseName = "storage отсутствует",
                    bodyOverride = mapOf("storage" to null),
                )
            }

            @Nested
            @DisplayName("Поле storage.engine")
            inner class StorageEngineFieldTests {
                @Test
                @Severity(SeverityLevel.NORMAL)
                @DisplayName("Возвращает 400, если поле отсутствует")
                fun shouldReturnBadRequestWhenMissing() = rejectInvalidSettingsBody(
                    caseName = "storage.engine отсутствует",
                    bodyOverride = mapOf("storage" to mapOf("jdbcUrl" to "jdbc:sqlite:data/core.db")),
                )
            }

            @Nested
            @DisplayName("Поле ofdTimeoutSeconds")
            inner class OfdTimeoutSecondsFieldTests {
                @Test
                @Severity(SeverityLevel.NORMAL)
                @DisplayName("Возвращает 400 для значения меньше минимально допустимого")
                fun shouldReturnBadRequestBelowMinimum() = rejectInvalidSettingsBody(
                    caseName = "ofdTimeoutSeconds меньше минимально допустимого значения",
                    bodyOverride = mapOf("ofdTimeoutSeconds" to 1),
                )
            }

            @Nested
            @DisplayName("Поле ofdReconnectIntervalSeconds")
            inner class OfdReconnectIntervalSecondsFieldTests {
                @Test
                @Severity(SeverityLevel.NORMAL)
                @DisplayName("Возвращает 400 для значения меньше минимально допустимого")
                fun shouldReturnBadRequestBelowMinimum() = rejectInvalidSettingsBody(
                    caseName = "ofdReconnectIntervalSeconds меньше минимально допустимого значения",
                    bodyOverride = mapOf("ofdReconnectIntervalSeconds" to 1),
                )
            }

            private fun rejectInvalidSettingsBody(caseName: String, bodyOverride: Map<String, Any?>) {
                val invalidBody = currentSettings.toMutableMap().apply { putAll(bodyOverride) }

                reportStep("Отправляем невалидное тело через PUT /settings: $caseName") {
                    superkassa.request()
                        .body(invalidBody)
                        .`when`()
                        .put("/settings")
                        .then()
                        .shouldHaveStatus(400, "невалидный запрос")
                        .contentType(ContentType.JSON)
                }
            }

        }
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

        Allure.step("Проверяем предусловие regression-теста: allowChanges=true")
        assumeTrue(
            allowChanges == true,
            "PUT /settings доступен только если в текущих настройках allowChanges=true, сейчас вернулся allowChanges=$allowChanges",
        )
    }

    private fun value(json: JsonPath, path: String): Any? = json.get(path)

    private fun assertFieldType(
        softly: SoftAssertions,
        item: Map<String, Any?>,
        endpoint: String,
        fieldName: String,
        expectedType: Class<*>,
        schemaName: String,
    ) {
        softly.assertThat(item)
            .withFailMessage(
                ApiContractErrorMessages.requiredFieldWithTypeMissing(
                    endpoint,
                    fieldName,
                    expectedType.simpleName,
                    schemaName
                )
            )
            .containsKey(fieldName)

        softly.assertThat(item[fieldName])
            .withFailMessage(
                ApiContractErrorMessages.fieldTypeMismatch(
                    endpoint,
                    fieldName,
                    expectedType.simpleName,
                    schemaName
                )
            )
            .isInstanceOf(expectedType)
    }

    private fun assertIntegerFieldType(
        softly: SoftAssertions,
        item: Map<String, Any?>,
        endpoint: String,
        fieldName: String,
        schemaName: String,
    ) {
        softly.assertThat(item)
            .withFailMessage(
                ApiContractErrorMessages.requiredFieldWithTypeMissing(
                    endpoint,
                    fieldName,
                    "Integer",
                    schemaName
                )
            )
            .containsKey(fieldName)

        softly.assertThat(item[fieldName])
            .withFailMessage(ApiContractErrorMessages.fieldTypeMismatch(endpoint, fieldName, "Integer", schemaName))
            .isInstanceOfAny(Int::class.javaObjectType, Long::class.javaObjectType)
    }

    private fun assertOptionalIntegerFieldType(
        softly: SoftAssertions,
        item: Map<String, Any?>,
        endpoint: String,
        fieldName: String,
        schemaName: String,
    ) {
        val fieldValue = item[fieldName] ?: return

        softly.assertThat(fieldValue)
            .withFailMessage(
                ApiContractErrorMessages.optionalFieldTypeMismatch(
                    endpoint,
                    fieldName,
                    "Integer",
                    schemaName
                )
            )
            .isInstanceOfAny(Int::class.javaObjectType, Long::class.javaObjectType)
    }

    private fun assertOptionalFieldType(
        softly: SoftAssertions,
        item: Map<String, Any?>,
        endpoint: String,
        fieldName: String,
        expectedType: Class<*>,
        schemaName: String,
    ) {
        val fieldValue = item[fieldName] ?: return

        softly.assertThat(fieldValue)
            .withFailMessage(
                ApiContractErrorMessages.optionalFieldTypeMismatch(
                    endpoint,
                    fieldName,
                    expectedType.simpleName,
                    schemaName
                )
            )
            .isInstanceOf(expectedType)
    }

    private fun assertArrayItemsType(
        softly: SoftAssertions,
        fieldValue: Any?,
        endpoint: String,
        fieldName: String,
        expectedType: Class<*>,
        schemaName: String,
    ) {
        (fieldValue as? List<*>)?.forEachIndexed { index, item ->
            softly.assertThat(item)
                .withFailMessage(
                    ApiContractErrorMessages.arrayItemTypeMismatch(
                        endpoint,
                        fieldName,
                        index,
                        expectedType.simpleName,
                        schemaName
                    )
                )
                .isInstanceOf(expectedType)
        }
    }

    private fun assertDeliveryChannelsTypes(
        softly: SoftAssertions,
        channels: List<Map<String, Any?>>,
    ) {
        channels.forEach { channel ->
            assertFieldType(softly, channel, ENDPOINT, "channel", String::class.java, "DeliveryChannelSettingsDto")
            assertOptionalFieldType(
                softly,
                channel,
                ENDPOINT,
                "destination",
                String::class.java,
                "DeliveryChannelSettingsDto"
            )
            assertFieldType(
                softly,
                channel,
                ENDPOINT,
                "documentFormat",
                String::class.java,
                "DeliveryChannelSettingsDto"
            )
            assertFieldType(
                softly,
                channel,
                ENDPOINT,
                "enabled",
                Boolean::class.javaObjectType,
                "DeliveryChannelSettingsDto"
            )
            assertFieldType(softly, channel, ENDPOINT, "payloadType", String::class.java, "DeliveryChannelSettingsDto")
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun Map<String, Any?>.objectField(fieldName: String): Map<String, Any?>? =
        this[fieldName] as? Map<String, Any?>

    @Suppress("UNCHECKED_CAST")
    private fun Map<String, Any?>.listField(fieldName: String): List<Map<String, Any?>> =
        this[fieldName] as? List<Map<String, Any?>> ?: emptyList()

    private fun assertOnlySwaggerFields(
        softly: SoftAssertions,
        item: Map<String, Any?>,
        endpoint: String,
        schemaName: String,
        allowedFields: Set<String>,
    ) {
        val unexpectedFields = item.keys - allowedFields

        softly.assertThat(unexpectedFields)
            .withFailMessage(ApiContractErrorMessages.unexpectedSwaggerFields(endpoint, schemaName, unexpectedFields))
            .isEmpty()
    }

    private fun assertRequiredEnumValue(
        softly: SoftAssertions,
        item: Map<String, Any?>,
        endpoint: String,
        fieldName: String,
        schemaName: String,
        supportedValues: Set<String>,
    ) {
        val fieldValue = item[fieldName] as? String

        softly.assertThat(fieldValue)
            .withFailMessage(ApiContractErrorMessages.requiredEnumMissing(endpoint, fieldName, schemaName))
            .isNotBlank()

        softly.assertThat(fieldValue)
            .withFailMessage(ApiContractErrorMessages.enumUnsupported(endpoint, fieldName, fieldValue, supportedValues))
            .isIn(supportedValues)
    }

    private companion object {
        const val ENDPOINT = "PUT /settings"

        val CORE_SETTINGS_FIELDS = setOf(
            "allowChanges",
            "defaultAdminName",
            "defaultAdminPin",
            "defaultCashierName",
            "defaultCashierPin",
            "delivery",
            "deliveryChannels",
            "mode",
            "nodeId",
            "ofdProtocolVersion",
            "ofdReconnectIntervalSeconds",
            "ofdTimeoutSeconds",
            "storage",
        )
        val STORAGE_SETTINGS_FIELDS = setOf("engine", "jdbcUrl", "password", "user")
        val DELIVERY_SETTINGS_FIELDS = setOf("channels", "email", "print", "sms", "telegram", "whatsapp")
        val DELIVERY_CHANNEL_SETTINGS_FIELDS =
            setOf("channel", "destination", "documentFormat", "enabled", "payloadType")
        val PRINT_DELIVERY_SETTINGS_FIELDS = setOf("connection", "enabled", "paperWidthMm")
        val PRINT_CONNECTION_SETTINGS_FIELDS = setOf("host", "port", "type")
        val EMAIL_PROVIDER_SETTINGS_FIELDS = setOf("from", "host", "password", "port", "user")
        val SMS_PROVIDER_SETTINGS_FIELDS = setOf("apiKey", "providerUrl")
        val TELEGRAM_PROVIDER_SETTINGS_FIELDS = setOf("botToken")
        val WHATSAPP_PROVIDER_SETTINGS_FIELDS = setOf("accessToken", "phoneNumberId")
        val FORBIDDEN_JDBC_URL_FRAGMENTS = listOf(
            "password=",
            "pwd=",
            "pass=",
            "token",
            "secret",
        )
    }
}
