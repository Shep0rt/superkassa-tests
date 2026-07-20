package kz.superkassa.tests.framework

import kz.superkassa.tests.framework.config.TestConfig
import kz.superkassa.tests.framework.http.SuperkassaApiClient
import kz.superkassa.tests.framework.kkm.KkmAuthSupport
import kz.superkassa.tests.framework.support.Polling
import io.restassured.response.ValidatableResponse
import org.assertj.core.api.Assertions.assertThat

@Suppress("unused")
abstract class BaseTest {
    protected val superkassa = SuperkassaApiClient(CONFIG)
    protected val polling = Polling(CONFIG)
    protected val testConfig: TestConfig = CONFIG
    protected val kkmAuth = KkmAuthSupport(superkassa)

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
        protected val CONFIG: TestConfig = TestConfig.load()
    }
}
