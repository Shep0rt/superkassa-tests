package kz.superkassa.tests.api.about.info

import io.qameta.allure.Feature
import io.qameta.allure.Owner
import io.qameta.allure.Severity
import io.qameta.allure.SeverityLevel
import io.qameta.allure.Story
import io.restassured.http.ContentType
import io.restassured.http.Method
import io.restassured.path.json.JsonPath
import io.restassured.response.Response
import kz.superkassa.tests.framework.BaseTest
import kz.superkassa.tests.framework.contract.ApiEnumValues
import kz.superkassa.tests.framework.assertions.ApiContractErrorMessages
import kz.superkassa.tests.framework.tags.ApiRegression
import org.assertj.core.api.SoftAssertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.util.Locale

@ApiRegression
@Feature("API")
@Story("GET /info")
@Owner("Pavel Michka")
@DisplayName("GET /info: регрессионные проверки информации о Superkassa")
@Suppress("SameParameterValue")
class InfoRegressionTest : BaseTest() {
    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Метод GET /info возвращает поля ожидаемых типов")
    fun shouldReturnExpectedFieldTypes() {
        val json = getInfoJson()

        SoftAssertions().apply {
            val response = json.getMap<String, Any?>("")

            assertFieldType(this, response, ENDPOINT, "name", String::class.java, "InfoResponse")
            assertFieldType(this, response, ENDPOINT, "version", String::class.java, "InfoResponse")
            assertFieldType(this, response, ENDPOINT, "mode", String::class.java, "InfoResponse")
            assertFieldType(this, response, ENDPOINT, "nodeId", String::class.java, "InfoResponse")
            assertFieldType(this, response, ENDPOINT, "ofdProtocolVersion", String::class.java, "InfoResponse")

            assertFieldType(this, response, ENDPOINT, "storage", Map::class.java, "InfoResponse")
            response.objectField("storage")?.let { storage ->
                assertFieldType(this, storage, ENDPOINT, "engine", String::class.java, "Storage")
                assertFieldType(this, storage, ENDPOINT, "jdbcUrl", String::class.java, "Storage")
            }

            assertFieldType(this, response, ENDPOINT, "statistics", Map::class.java, "InfoResponse")
            response.objectField("statistics")?.let { statistics ->
                assertIntegerFieldType(this, statistics, ENDPOINT, "registeredKkms", "Statistics")
            }

            assertFieldType(this, response, ENDPOINT, "features", Map::class.java, "InfoResponse")
            response.objectField("features")?.let { features ->
                assertFieldType(this, features, ENDPOINT, "allowSettingsChanges", Boolean::class.javaObjectType, "Features")
                assertFieldType(this, features, ENDPOINT, "deliveryChannels", List::class.java, "Features")
                assertArrayItemsType(this, features["deliveryChannels"], ENDPOINT, "features.deliveryChannels", String::class.java, "Features")
                assertIntegerFieldType(this, features, ENDPOINT, "ofdTimeoutSeconds", "Features")
                assertIntegerFieldType(this, features, ENDPOINT, "ofdReconnectIntervalSeconds", "Features")
            }
        }.assertAll()
    }

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Метод GET /info не возвращает поля вне ожидаемой структуры")
    fun shouldNotReturnFieldsOutsideExpectedStructure() {
        val json = getInfoJson()
        val response = json.getMap<String, Any?>("")

        SoftAssertions().apply {
            assertOnlySwaggerFields(this, response, ENDPOINT, "InfoResponse", INFO_RESPONSE_FIELDS)
            response.objectField("storage")?.let { storage ->
                assertOnlySwaggerFields(this, storage, ENDPOINT, "storage", STORAGE_FIELDS)
            }
            response.objectField("statistics")?.let { statistics ->
                assertOnlySwaggerFields(this, statistics, ENDPOINT, "statistics", STATISTICS_FIELDS)
            }
            response.objectField("features")?.let { features ->
                assertOnlySwaggerFields(this, features, ENDPOINT, "features", FEATURES_FIELDS)
            }
        }.assertAll()
    }

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Метод GET /info возвращает допустимые бизнес-значения")
    fun shouldReturnExpectedBusinessValues() {
        val json = getInfoJson()

        SoftAssertions().apply {
            assertThat(value(json, "name") as? String).isEqualTo("Superkassa Core")
            assertThat(value(json, "mode") as? String).isIn(ApiEnumValues.INFO_MODES)
            assertThat(value(json, "ofdProtocolVersion") as? String)
                .matches("\\d+")
                .isEqualTo("203")

            assertThat(value(json, "storage.engine") as? String).isIn(ApiEnumValues.STORAGE_ENGINES)
            assertThat(value(json, "statistics.registeredKkms") as? Int).isGreaterThanOrEqualTo(0)

            val deliveryChannels = json.getList<String>("features.deliveryChannels")
            assertThat(deliveryChannels).isNotEmpty()
            deliveryChannels?.forEach { channel ->
                assertThat(channel).isIn(ApiEnumValues.DELIVERY_CHANNELS)
            }
            assertThat(value(json, "features.ofdTimeoutSeconds") as? Int).isGreaterThanOrEqualTo(5)
            assertThat(value(json, "features.ofdReconnectIntervalSeconds") as? Int).isGreaterThanOrEqualTo(60)
        }.assertAll()
    }

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Метод GET /info не раскрывает секреты в JDBC URL")
    fun shouldNotExposeSecretsInJdbcUrl() {
        val json = getInfoJson()

        val jdbcUrl = (value(json, "storage.jdbcUrl") as? String)?.lowercase(Locale.ROOT)

        SoftAssertions().apply {
            FORBIDDEN_JDBC_URL_FRAGMENTS.forEach { fragment ->
                assertThat(jdbcUrl)
                    .withFailMessage("Безопасность API нарушена: storage.jdbcUrl в ответе GET /info содержит секретный фрагмент '%s'.", fragment)
                    .doesNotContain(fragment)
            }
        }.assertAll()
    }

    @ParameterizedTest(name = "HTTP {0} /info возвращает 405")
    @EnumSource(value = Method::class, names = ["POST", "PUT", "PATCH", "DELETE"])
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("Метод /info возвращает 405 для HTTP-методов кроме GET")
    fun shouldReturnMethodNotAllowedForNonGetMethods(method: Method) {
        superkassa.request()
            .`when`()
            .request(method, "/info")
            .then()
            .shouldHaveStatus(405, "неподдерживаемый HTTP-метод")
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

    private fun assertFieldType(
        softly: SoftAssertions,
        item: Map<String, Any?>,
        endpoint: String,
        fieldName: String,
        expectedType: Class<*>,
        schemaName: String,
    ) {
        softly.assertThat(item)
            .withFailMessage(ApiContractErrorMessages.requiredFieldWithTypeMissing(endpoint, fieldName, expectedType.simpleName, schemaName))
            .containsKey(fieldName)

        softly.assertThat(item[fieldName])
            .withFailMessage(ApiContractErrorMessages.fieldTypeMismatch(endpoint, fieldName, expectedType.simpleName, schemaName))
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
            .withFailMessage(ApiContractErrorMessages.requiredFieldWithTypeMissing(endpoint, fieldName, "Integer", schemaName))
            .containsKey(fieldName)

        softly.assertThat(item[fieldName])
            .withFailMessage(ApiContractErrorMessages.fieldTypeMismatch(endpoint, fieldName, "Integer", schemaName))
            .isInstanceOf(Int::class.javaObjectType)
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
                .withFailMessage(ApiContractErrorMessages.arrayItemTypeMismatch(endpoint, fieldName, index, expectedType.simpleName, schemaName))
                .isInstanceOf(expectedType)
        }
    }

    private fun assertOnlySwaggerFields(
        softly: SoftAssertions,
        item: Map<String, Any?>,
        endpoint: String,
        objectPath: String,
        allowedFields: Set<String>,
    ) {
        val unexpectedFields = item.keys - allowedFields

        softly.assertThat(unexpectedFields)
            .withFailMessage(ApiContractErrorMessages.unexpectedSwaggerFields(endpoint, objectPath, unexpectedFields))
            .isEmpty()
    }

    @Suppress("UNCHECKED_CAST")
    private fun Map<String, Any?>.objectField(fieldName: String): Map<String, Any?>? = this[fieldName] as? Map<String, Any?>

    private companion object {
        const val ENDPOINT = "GET /info"

        val INFO_RESPONSE_FIELDS = setOf("name", "version", "mode", "nodeId", "ofdProtocolVersion", "storage", "statistics", "features")
        val STORAGE_FIELDS = setOf("engine", "jdbcUrl")
        val STATISTICS_FIELDS = setOf("registeredKkms")
        val FEATURES_FIELDS = setOf("allowSettingsChanges", "deliveryChannels", "ofdTimeoutSeconds", "ofdReconnectIntervalSeconds")
        val FORBIDDEN_JDBC_URL_FRAGMENTS = listOf(
            "password=",
            "pwd=",
            "pass=",
            "token",
            "secret",
        )
    }
}
