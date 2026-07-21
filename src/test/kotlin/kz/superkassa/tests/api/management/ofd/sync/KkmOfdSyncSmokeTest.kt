package kz.superkassa.tests.api.management.ofd.sync

import io.qameta.allure.Allure
import io.qameta.allure.Feature
import io.qameta.allure.Owner
import io.qameta.allure.Severity
import io.qameta.allure.SeverityLevel
import io.qameta.allure.Story
import io.restassured.http.ContentType
import io.restassured.response.Response
import kz.superkassa.tests.framework.kkm.KkmAuthenticatedTest
import kz.superkassa.tests.framework.assertions.ApiContractErrorMessages
import kz.superkassa.tests.framework.kkm.PreparedKkmAuth
import kz.superkassa.tests.framework.reporting.reportStep
import kz.superkassa.tests.framework.tags.ApiSmoke
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.SoftAssertions
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.ResourceAccessMode
import org.junit.jupiter.api.parallel.ResourceLock

@ApiSmoke
@Feature("API")
@Story("POST /kkm/{kkmId}/ofd/sync")
@Owner("Pavel Michka")
@DisplayName("POST /kkm/{kkmId}/ofd/sync: smoke-проверки синхронизации ККМ с ОФД")
@ResourceLock(value = "kkm-state", mode = ResourceAccessMode.READ_WRITE)
@Suppress("NonAsciiCharacters")
class KkmOfdSyncSmokeTest : KkmAuthenticatedTest() {
    private var kkmToExitAfterTest: PreparedKkmAuth? = null

    @BeforeEach
    fun `Переводим ККМ в режим программирования`() {
        enterProgramming(preparedKkm)
    }

    @AfterEach
    fun `Восстанавливаем режим ККМ после проверки`() {
        val preparedKkm = kkmToExitAfterTest
        if (preparedKkm == null) {
            Allure.step("Возврат не требуется: ККМ не оставлена в режиме программирования")
            return
        }
        kkmToExitAfterTest = null
        exitProgrammingForCleanup(preparedKkm)
    }

    @Test
    @Severity(SeverityLevel.BLOCKER)
    @DisplayName("Метод POST /kkm/{kkmId}/ofd/sync возвращает HTTP 200 и JSON")
    fun shouldSyncOfdSuccessfully() {
        syncOfd()
    }

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Метод POST /kkm/{kkmId}/ofd/sync возвращает обязательное поле status")
    fun shouldReturnRequiredStatusField() {
        val response = syncOfd()

        assertThat(response)
            .withFailMessage(ApiContractErrorMessages.requiredFieldMissing(ENDPOINT, STATUS_FIELD, RESPONSE_SCHEMA))
            .containsKey(STATUS_FIELD)
    }

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Метод POST /kkm/{kkmId}/ofd/sync возвращает заполненное обязательное поле status")
    fun shouldReturnFilledStatusField() {
        val status = syncOfd()[STATUS_FIELD]
        val message = ApiContractErrorMessages.requiredFieldEmpty(ENDPOINT, STATUS_FIELD, RESPONSE_SCHEMA)

        SoftAssertions().apply {
            assertThat(status)
                .withFailMessage(message)
                .isNotNull()

            if (status is String) {
                assertThat(status)
                    .withFailMessage(message)
                    .isNotBlank()
            }
        }.assertAll()
    }

    private fun syncOfd(): Map<String, Any?> {
        return reportStep("Синхронизируем ККМ kkmId='${preparedKkm.kkmId}' с ОФД через POST ${syncPath(preparedKkm.kkmId)}") {
            superkassa.request(preparedKkm.adminPin)
                .`when`()
                .post(syncPath(preparedKkm.kkmId))
                .then()
                .shouldHaveStatus(200, "синхронизация ККМ с ОФД")
                .contentType(ContentType.JSON)
                .extract()
                .jsonPath()
                .getMap("")
        }
    }

    private fun enterProgramming(preparedKkm: PreparedKkmAuth) {
        reportStep("Готовим предусловие: переводим ККМ kkmId='${preparedKkm.kkmId}' в режим программирования") {
            val response: Response = superkassa.request(preparedKkm.adminPin)
                .`when`()
                .post("/kkm/{kkmId}/programming/enter", preparedKkm.kkmId)
                .then()
                .extract()
                .response()

            if (response.statusCode == 200) {
                kkmToExitAfterTest = preparedKkm
            }

            response.then()
                .shouldHaveStatus(200, "подготовка: вход ККМ в режим программирования перед синхронизацией с ОФД")
                .contentType(ContentType.JSON)
        }
    }

    private fun exitProgrammingForCleanup(preparedKkm: PreparedKkmAuth) {
        reportStep("Возвращаем ККМ kkmId='${preparedKkm.kkmId}' из режима программирования после проверки синхронизации с ОФД") {
            superkassa.request(preparedKkm.adminPin)
                .`when`()
                .post("/kkm/{kkmId}/programming/exit", preparedKkm.kkmId)
                .then()
                .shouldHaveStatus(200, "cleanup: выход ККМ из режима программирования")
                .contentType(ContentType.JSON)
        }
    }

    private fun syncPath(kkmId: String): String = "/kkm/$kkmId/ofd/sync"

    private companion object {
        const val ENDPOINT = "POST /kkm/{kkmId}/ofd/sync"
        const val RESPONSE_SCHEMA = "OfdCommandResponse"
        const val STATUS_FIELD = "status"
    }
}
