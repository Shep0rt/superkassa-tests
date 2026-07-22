package kz.superkassa.tests.framework

import kz.superkassa.tests.framework.config.TestConfig
import kz.superkassa.tests.framework.http.SuperkassaApiClient
import io.restassured.response.ValidatableResponse
import org.assertj.core.api.Assertions.assertThat

abstract class BaseApiTest {
    protected val config: TestConfig = CONFIG
    protected val superkassa = SuperkassaApiClient(config)

    protected fun ValidatableResponse.shouldHaveStatus(expectedStatus: Int, scenario: String): ValidatableResponse {
        val actualStatus = extract().response().statusCode

        assertThat(actualStatus)
            .withFailMessage(
                "HTTP-статус не соответствует ожиданию: %s. Должен вернуться HTTP <%s>, а вернулся HTTP <%s>.",
                scenario,
                expectedStatus,
                actualStatus,
            )
            .isEqualTo(expectedStatus)

        return this
    }

    companion object {
        private val CONFIG: TestConfig = TestConfig.load()
    }
}
