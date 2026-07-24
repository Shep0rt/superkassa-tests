package kz.superkassa.tests.api.management.ofd.token

import io.qameta.allure.Allure
import io.restassured.http.ContentType
import io.restassured.response.Response
import kz.superkassa.tests.framework.kkm.KkmAuthenticatedTest
import kz.superkassa.tests.framework.kkm.PreparedKkmAuth
import kz.superkassa.tests.framework.reporting.reportStep
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.assertj.core.api.Assertions.assertThat

@Suppress("NonAsciiCharacters")
abstract class KkmOfdTokenTestBase : KkmAuthenticatedTest() {
    protected lateinit var currentOfdToken: String

    private var kkmToExitAfterTest: PreparedKkmAuth? = null
    private var tokenUpdateAttempted = false
    private var tokenRestorationRequired = false

    protected fun prepareKkmForTokenUpdate() {
        currentOfdToken = getCurrentOfdToken(preparedKkm)
        enterProgramming(preparedKkm)
    }

    protected fun registerTokenRestoration() {
        tokenRestorationRequired = true
        tokenUpdateAttempted = true
    }

    @AfterEach
    fun `Восстанавливаем состояние ККМ после проверки`() {
        val preparedKkm = kkmToExitAfterTest
        if (preparedKkm == null) {
            Allure.step("Восстановление не требуется: ККМ не оставлена в режиме программирования")
            return
        }

        var cleanupFailure: Throwable? = null

        fun captureCleanupFailure(action: () -> Unit) {
            try {
                action()
            } catch (failure: Throwable) {
                val existingFailure = cleanupFailure
                if (existingFailure == null) {
                    cleanupFailure = failure
                } else {
                    existingFailure.addSuppressed(failure)
                }
            }
        }

        if (tokenRestorationRequired) {
            captureCleanupFailure { restoreCurrentOfdToken(preparedKkm) }
        }

        kkmToExitAfterTest = null
        captureCleanupFailure { exitProgrammingForCleanup(preparedKkm) }

        if (tokenUpdateAttempted) {
            captureCleanupFailure { verifyOfdConnectionAfterUpdate(preparedKkm) }
        } else {
            Allure.step("Проверка связи с ОФД не требуется: обновление токена не выполнялось")
        }

        cleanupFailure?.let { throw it }
    }

    protected fun updateOfdToken(): Map<String, Any?> {
        tokenUpdateAttempted = true

        return reportStep("Повторно сохраняем текущий токен ОФД через PUT ${tokenPath(preparedKkm.kkmId)}") {
            superkassa.request(preparedKkm.adminPin)
                .body(mapOf(TOKEN_FIELD to currentOfdToken))
                .`when`()
                .put(tokenPath(preparedKkm.kkmId))
                .then()
                .shouldHaveStatus(200, "обновление текущего токена ОФД без изменения его значения")
                .contentType(ContentType.JSON)
                .extract()
                .jsonPath()
                .getMap("")
        }
    }

    private fun getCurrentOfdToken(preparedKkm: PreparedKkmAuth): String {
        return reportStep("Получаем текущий токен ОФД безопасной командой GET ${pingPath(preparedKkm.kkmId)}") {
            val response = superkassa.requestWithoutAuthorization()
                .`when`()
                .get(pingPath(preparedKkm.kkmId))
                .then()
                .extract()
                .response()

            assumeTrue(
                response.statusCode == 200,
                "Внешнее предусловие не выполнено: GET /kkm/{kkmId}/ofd/ping вернул HTTP ${response.statusCode} вместо HTTP 200",
            )
            assumeTrue(
                response.contentType?.contains("json", ignoreCase = true) == true,
                "Внешнее предусловие не выполнено: GET /kkm/{kkmId}/ofd/ping вернул ответ не в формате JSON",
            )

            val status = response.jsonPath().get<Any?>(STATUS_FIELD)
            val responseToken = response.jsonPath().get<Any?>(RESPONSE_TOKEN_FIELD)

            assumeTrue(
                status == OK_STATUS,
                "Внешнее предусловие не выполнено: связь с ОФД недоступна, GET /kkm/{kkmId}/ofd/ping вернул status='$status'",
            )
            assumeTrue(
                responseToken is Number,
                "Внешнее предусловие не выполнено: GET /kkm/{kkmId}/ofd/ping не вернул числовое поле responseToken",
            )

            (responseToken as Number).toString()
        }
    }

    private fun enterProgramming(preparedKkm: PreparedKkmAuth) {
        reportStep("Переводим ККМ kkmId='${preparedKkm.kkmId}' в режим программирования перед обновлением токена ОФД") {
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
                .shouldHaveStatus(200, "подготовка: вход ККМ в режим программирования перед обновлением токена ОФД")
                .contentType(ContentType.JSON)
        }
    }

    private fun exitProgrammingForCleanup(preparedKkm: PreparedKkmAuth) {
        reportStep("Возвращаем ККМ kkmId='${preparedKkm.kkmId}' из режима программирования после проверки обновления токена ОФД") {
            superkassa.request(preparedKkm.adminPin)
                .`when`()
                .post("/kkm/{kkmId}/programming/exit", preparedKkm.kkmId)
                .then()
                .shouldHaveStatus(200, "cleanup: выход ККМ из режима программирования")
                .contentType(ContentType.JSON)
        }
    }

    private fun restoreCurrentOfdToken(preparedKkm: PreparedKkmAuth) {
        reportStep("Восстанавливаем исходный токен ОФД после проверки невалидного запроса") {
            superkassa.request(preparedKkm.adminPin)
                .body(mapOf(TOKEN_FIELD to currentOfdToken))
                .`when`()
                .put(tokenPath(preparedKkm.kkmId))
                .then()
                .shouldHaveStatus(200, "cleanup: восстановление исходного токена ОФД")
                .contentType(ContentType.JSON)
        }
    }

    private fun verifyOfdConnectionAfterUpdate(preparedKkm: PreparedKkmAuth) {
        reportStep("Проверяем связь с ОФД после повторного сохранения текущего токена") {
            val response = superkassa.requestWithoutAuthorization()
                .`when`()
                .get(pingPath(preparedKkm.kkmId))
                .then()
                .shouldHaveStatus(200, "постусловие: проверка связи с ОФД после обновления токена")
                .contentType(ContentType.JSON)
                .extract()
                .response()

            val status = response.jsonPath().get<Any?>(STATUS_FIELD)
            val responseToken = response.jsonPath().get<Any?>(RESPONSE_TOKEN_FIELD)

            assertThat(status)
                .withFailMessage(
                    "Постусловие не выполнено: после PUT /kkm/{kkmId}/ofd/token связь с ОФД должна иметь status='OK', " +
                        "но вернулся status='%s'.",
                    status,
                )
                .isEqualTo(OK_STATUS)

            assertThat(responseToken?.toString())
                .withFailMessage(
                    "Постусловие не выполнено: после повторного сохранения текущего токена ОФД его значение изменилось.",
                )
                .isEqualTo(currentOfdToken)
        }
    }

    protected fun tokenPath(kkmId: String): String = "/kkm/$kkmId/ofd/token"

    private fun pingPath(kkmId: String): String = "/kkm/$kkmId/ofd/ping"

    protected companion object {
        const val ENDPOINT = "PUT /kkm/{kkmId}/ofd/token"
        const val EXPECTED_RESPONSE = "OfdTokenUpdateResponse"
        const val OK_FIELD = "ok"
        const val TOKEN_FIELD = "token"

        private const val STATUS_FIELD = "status"
        private const val RESPONSE_TOKEN_FIELD = "responseToken"
        private const val OK_STATUS = "OK"
    }
}
