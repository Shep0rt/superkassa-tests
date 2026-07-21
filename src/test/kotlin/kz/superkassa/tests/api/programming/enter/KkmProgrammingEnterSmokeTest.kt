package kz.superkassa.tests.api.programming.enter

import io.qameta.allure.Allure
import io.qameta.allure.Feature
import io.qameta.allure.Owner
import io.qameta.allure.Severity
import io.qameta.allure.SeverityLevel
import io.qameta.allure.Story
import io.restassured.http.ContentType
import io.restassured.path.json.JsonPath
import io.restassured.response.Response
import kz.superkassa.tests.framework.kkm.KkmAuthenticatedTest
import kz.superkassa.tests.framework.assertions.ApiContractErrorMessages
import kz.superkassa.tests.framework.kkm.PreparedKkmAuth
import kz.superkassa.tests.framework.tags.ApiSmoke
import org.assertj.core.api.SoftAssertions
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@ApiSmoke
@Feature("API")
@Story("POST /kkm/{kkmId}/programming/enter")
@Owner("Pavel Michka")
@DisplayName("POST /kkm/{kkmId}/programming/enter: smoke-проверки входа в режим программирования")
@Suppress("SameParameterValue", "NonAsciiCharacters")
class KkmProgrammingEnterSmokeTest : KkmAuthenticatedTest() {
    private var kkmToExitAfterTest: PreparedKkmAuth? = null

    @AfterEach
    fun `Восстанавливаем режим ККМ после проверки`() {
        val preparedKkm = kkmToExitAfterTest
        if (preparedKkm == null) {
            Allure.step("Возврат не требуется: ККМ не оставлена в режиме программирования")
            return
        }
        kkmToExitAfterTest = null
        exitProgramming(preparedKkm)
    }

    @Test
    @Severity(SeverityLevel.BLOCKER)
    @DisplayName("Метод POST /kkm/{kkmId}/programming/enter возвращает HTTP 200 и JSON")
    fun shouldEnterProgrammingSuccessfully() {

        withProgrammingMode(preparedKkm) {}
    }

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Метод POST /kkm/{kkmId}/programming/enter возвращает обязательные поля ККМ")
    fun shouldReturnRequiredKkmFieldsAfterEnterProgramming() {

        withProgrammingMode(preparedKkm) { json ->
            SoftAssertions().apply {
                assertRequiredFieldPresent(
                    this,
                    value(json, "autoCloseShift"),
                    ENDPOINT,
                    "autoCloseShift",
                    "KkmResponse"
                )
                assertRequiredFieldPresent(this, value(json, "createdAt"), ENDPOINT, "createdAt", "KkmResponse")
                assertRequiredFieldPresent(this, value(json, "kkmId"), ENDPOINT, "kkmId", "KkmResponse")
                assertRequiredFieldPresent(this, value(json, "mode"), ENDPOINT, "mode", "KkmResponse")
                assertRequiredFieldPresent(this, value(json, "state"), ENDPOINT, "state", "KkmResponse")
                assertRequiredFieldPresent(this, value(json, "updatedAt"), ENDPOINT, "updatedAt", "KkmResponse")
            }.assertAll()
        }
    }

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Метод POST /kkm/{kkmId}/programming/enter возвращает заполненные обязательные поля ККМ")
    fun shouldReturnFilledRequiredKkmFieldsAfterEnterProgramming() {

        withProgrammingMode(preparedKkm) { json ->
            SoftAssertions().apply {
                assertRequiredFieldFilled(
                    this,
                    value(json, "autoCloseShift"),
                    ENDPOINT,
                    "autoCloseShift",
                    "KkmResponse"
                )
                assertRequiredFieldFilled(this, value(json, "createdAt"), ENDPOINT, "createdAt", "KkmResponse")
                assertRequiredFieldFilled(this, value(json, "kkmId"), ENDPOINT, "kkmId", "KkmResponse")
                assertRequiredFieldFilled(this, value(json, "mode"), ENDPOINT, "mode", "KkmResponse")
                assertRequiredFieldFilled(this, value(json, "state"), ENDPOINT, "state", "KkmResponse")
                assertRequiredFieldFilled(this, value(json, "updatedAt"), ENDPOINT, "updatedAt", "KkmResponse")
            }.assertAll()
        }
    }

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Метод POST /kkm/{kkmId}/programming/enter переводит выбранную ККМ в режим программирования")
    fun shouldReturnSameKkmInProgrammingMode() {

        withProgrammingMode(preparedKkm) { json ->
            SoftAssertions().apply {
                assertThat(value(json, "kkmId") as? String)
                    .withFailMessage(
                        "Функциональность API нарушена: POST /kkm/%s/programming/enter вернул данные другой ККМ. " +
                                "Ожидался kkmId='%s'.",
                        preparedKkm.kkmId,
                        preparedKkm.kkmId,
                    )
                    .isEqualTo(preparedKkm.kkmId)
                assertThat(value(json, "mode") as? String)
                    .withFailMessage(
                        "Функциональность API нарушена: POST /kkm/%s/programming/enter должен вернуть mode='PROGRAMMING'.",
                        preparedKkm.kkmId,
                    )
                    .isEqualTo("PROGRAMMING")
            }.assertAll()
        }
    }

    private fun withProgrammingMode(preparedKkm: PreparedKkmAuth, action: (JsonPath) -> Unit) {
        val json = enterProgrammingJson(preparedKkm)
        kkmToExitAfterTest = preparedKkm
        action(json)
    }

    private fun enterProgrammingJson(preparedKkm: PreparedKkmAuth): JsonPath {
        val enterProgramming: Allure.ThrowableRunnable<Response> = Allure.ThrowableRunnable {
            superkassa.request(preparedKkm.adminPin)
                .`when`()
                .post(enterProgrammingPath(preparedKkm.kkmId))
                .then()
                .shouldHaveStatus(200, "успешный вход в режим программирования")
                .contentType(ContentType.JSON)
                .extract()
                .response()
        }

        val response: Response =
            Allure.step("Переводим ККМ kkmId='${preparedKkm.kkmId}' в режим программирования", enterProgramming)

        return response.jsonPath()
    }

    private fun exitProgramming(preparedKkm: PreparedKkmAuth) {
        val exitProgramming: Allure.ThrowableRunnable<Unit> = Allure.ThrowableRunnable {
            superkassa.request(preparedKkm.adminPin)
                .`when`()
                .post("/kkm/{kkmId}/programming/exit", preparedKkm.kkmId)
                .then()
                .shouldHaveStatus(200, "возврат ККМ из режима программирования")
                .contentType(ContentType.JSON)
        }

        Allure.step("Возвращаем ККМ kkmId='${preparedKkm.kkmId}' из режима программирования", exitProgramming)
    }

    private fun enterProgrammingPath(kkmId: String): String = "/kkm/$kkmId/programming/enter"

    private fun value(json: JsonPath, path: String): Any? = json.get(path)

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
        const val ENDPOINT = "POST /kkm/{kkmId}/programming/enter"
    }
}
