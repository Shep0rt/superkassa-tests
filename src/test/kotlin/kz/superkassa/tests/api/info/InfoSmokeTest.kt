package kz.superkassa.tests.api.info

import io.qameta.allure.Feature
import io.qameta.allure.Owner
import io.qameta.allure.Severity
import io.qameta.allure.SeverityLevel
import io.qameta.allure.Story
import io.restassured.http.ContentType
import kz.superkassa.tests.framework.BaseTest
import kz.superkassa.tests.framework.tags.ApiSmoke
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@ApiSmoke
@Feature("API")
@Story("GET /info")
@Owner("Pavel Michka")
@DisplayName("GET /info: smoke-проверки информации о Superkassa")
class InfoSmokeTest : BaseTest() {
    @Test
    @Severity(SeverityLevel.BLOCKER)
    @DisplayName("Метод GET /info возвращает HTTP 200 и JSON")
    fun shouldReturnInfoSuccessfully() {
        superkassa.request()
            .`when`()
            .get("/info")
            .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
    }

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Метод GET /info возвращает все обязательные поля")
    fun shouldReturnAllRequiredFields() {
        val info = superkassa.getInfo()

        assertThat(info.name).isNotNull()
        assertThat(info.version).isNotNull()
        assertThat(info.mode).isNotNull()
        assertThat(info.nodeId).isNotNull()
        assertThat(info.ofdProtocolVersion).isNotNull()

        assertThat(info.storage).isNotNull()
        assertThat(info.storage?.engine).isNotNull()
        assertThat(info.storage?.jdbcUrl).isNotNull()

        assertThat(info.statistics).isNotNull()
        assertThat(info.statistics?.registeredKkms).isNotNull()

        assertThat(info.features).isNotNull()
        assertThat(info.features?.allowSettingsChanges).isNotNull()
        assertThat(info.features?.deliveryChannels).isNotNull()
        assertThat(info.features?.ofdTimeoutSeconds).isNotNull()
        assertThat(info.features?.ofdReconnectIntervalSeconds).isNotNull()
    }

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Метод GET /info возвращает заполненные обязательные поля")
    fun shouldReturnNonEmptyRequiredFields() {
        val info = superkassa.getInfo()

        assertThat(info.name).isNotBlank()
        assertThat(info.version).isNotBlank()
        assertThat(info.mode).isNotBlank()
        assertThat(info.nodeId).isNotBlank()
        assertThat(info.ofdProtocolVersion).isNotBlank()

        assertThat(info.storage?.engine).isNotBlank()
        assertThat(info.storage?.jdbcUrl).isNotBlank()

        assertThat(info.statistics?.registeredKkms).isGreaterThanOrEqualTo(0)

        assertThat(info.features?.deliveryChannels)
            .isNotEmpty()
            .allSatisfy { channel -> assertThat(channel).isNotBlank() }
        assertThat(info.features?.ofdTimeoutSeconds).isPositive()
        assertThat(info.features?.ofdReconnectIntervalSeconds).isPositive()
    }
}
