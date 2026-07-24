package kz.superkassa.tests.api.management.ofd.token

import io.qameta.allure.Feature
import io.qameta.allure.Owner
import io.qameta.allure.Severity
import io.qameta.allure.SeverityLevel
import io.qameta.allure.Story
import kz.superkassa.tests.framework.assertions.ApiContractErrorMessages
import kz.superkassa.tests.framework.tags.ApiSmoke
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.ResourceAccessMode
import org.junit.jupiter.api.parallel.ResourceLock

@ApiSmoke
@Feature("API")
@Story("PUT /kkm/{kkmId}/ofd/token")
@Owner("Pavel Michka")
@DisplayName("PUT /kkm/{kkmId}/ofd/token: smoke-проверки обновления токена ОФД")
@ResourceLock(value = "kkm-state", mode = ResourceAccessMode.READ_WRITE)
@Suppress("NonAsciiCharacters")
class KkmOfdTokenSmokeTest : KkmOfdTokenTestBase() {
    @BeforeEach
    fun `Готовим ККМ к обновлению токена ОФД`() {
        prepareKkmForTokenUpdate()
    }

    @Test
    @Severity(SeverityLevel.BLOCKER)
    @DisplayName("Метод PUT /kkm/{kkmId}/ofd/token возвращает HTTP 200 и JSON")
    fun shouldUpdateOfdTokenSuccessfully() {
        updateOfdToken()
    }

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Метод PUT /kkm/{kkmId}/ofd/token возвращает обязательное поле ok")
    fun shouldReturnRequiredOkField() {
        val response = updateOfdToken()

        assertThat(response)
            .withFailMessage(ApiContractErrorMessages.documentedFieldMissing(ENDPOINT, OK_FIELD, EXPECTED_RESPONSE))
            .containsKey(OK_FIELD)
    }

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Метод PUT /kkm/{kkmId}/ofd/token возвращает заполненное обязательное поле ok")
    fun shouldReturnFilledOkField() {
        val ok = updateOfdToken()[OK_FIELD]

        assertThat(ok)
            .withFailMessage(ApiContractErrorMessages.documentedFieldEmpty(ENDPOINT, OK_FIELD, EXPECTED_RESPONSE))
            .isNotNull()
    }
}
