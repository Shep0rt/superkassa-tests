package kz.superkassa.tests.api.info

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
import kz.superkassa.tests.framework.tags.ApiRegression
import org.assertj.core.api.Assertions.assertThat
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
class InfoRegressionTest : BaseTest() {
    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Метод GET /info возвращает поля ожидаемых типов")
    fun shouldReturnExpectedFieldTypes() {
        val json = getInfoJson()

        assertThat(value(json, "name")).isInstanceOf(String::class.java)
        assertThat(value(json, "version")).isInstanceOf(String::class.java)
        assertThat(value(json, "mode")).isInstanceOf(String::class.java)
        assertThat(value(json, "nodeId")).isInstanceOf(String::class.java)
        assertThat(value(json, "ofdProtocolVersion")).isInstanceOf(String::class.java)

        assertThat(value(json, "storage")).isInstanceOf(Map::class.java)
        assertThat(value(json, "storage.engine")).isInstanceOf(String::class.java)
        assertThat(value(json, "storage.jdbcUrl")).isInstanceOf(String::class.java)

        assertThat(value(json, "statistics")).isInstanceOf(Map::class.java)
        assertThat(value(json, "statistics.registeredKkms")).isInstanceOf(Int::class.javaObjectType)

        assertThat(value(json, "features")).isInstanceOf(Map::class.java)
        assertThat(value(json, "features.allowSettingsChanges")).isInstanceOf(Boolean::class.javaObjectType)
        assertThat(value(json, "features.deliveryChannels")).isInstanceOf(List::class.java)
        assertThat(json.getList<Any>("features.deliveryChannels"))
            .allSatisfy { channel -> assertThat(channel).isInstanceOf(String::class.java) }
        assertThat(value(json, "features.ofdTimeoutSeconds")).isInstanceOf(Int::class.javaObjectType)
        assertThat(value(json, "features.ofdReconnectIntervalSeconds")).isInstanceOf(Int::class.javaObjectType)
    }

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Метод GET /info возвращает допустимые бизнес-значения")
    fun shouldReturnExpectedBusinessValues() {
        val info = superkassa.getInfo()

        assertThat(info.name).isEqualTo("Superkassa Core")
        assertThat(info.mode).isIn(SUPPORTED_MODES)
        assertThat(info.ofdProtocolVersion)
            .matches("\\d+")
            .isEqualTo("203")

        assertThat(info.storage?.engine).isIn(SUPPORTED_STORAGE_ENGINES)
        assertThat(info.statistics?.registeredKkms).isGreaterThanOrEqualTo(0)

        assertThat(info.features?.deliveryChannels)
            .isNotEmpty()
            .allSatisfy { channel -> assertThat(channel).isIn(SUPPORTED_DELIVERY_CHANNELS) }
        assertThat(info.features?.ofdTimeoutSeconds).isGreaterThanOrEqualTo(5)
        assertThat(info.features?.ofdReconnectIntervalSeconds).isGreaterThanOrEqualTo(60)
    }

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Метод GET /info не раскрывает секреты в JDBC URL")
    fun shouldNotExposeSecretsInJdbcUrl() {
        val info = superkassa.getInfo()

        val jdbcUrl = info.storage?.jdbcUrl?.lowercase(Locale.ROOT)

        assertThat(jdbcUrl).doesNotContain(FORBIDDEN_JDBC_URL_FRAGMENTS)
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
            .statusCode(405)
    }

    private fun getInfoJson(): JsonPath {
        val response: Response = superkassa.request()
            .`when`()
            .get("/info")
            .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .extract()
            .response()

        return response.jsonPath()
    }

    private fun value(json: JsonPath, path: String): Any? = json.get(path)

    private companion object {
        val SUPPORTED_MODES = setOf("DESKTOP", "SERVER")
        val SUPPORTED_STORAGE_ENGINES = setOf("SQLITE", "POSTGRESQL", "MYSQL")
        val SUPPORTED_DELIVERY_CHANNELS = setOf("PRINT", "EMAIL", "SMS", "TELEGRAM", "WHATSAPP")
        val FORBIDDEN_JDBC_URL_FRAGMENTS = listOf(
            "password=",
            "pwd=",
            "pass=",
            "token",
            "secret",
        )
    }
}
