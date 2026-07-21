package kz.superkassa.tests.api.users.get

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
import kz.superkassa.tests.framework.reporting.reportStep
import kz.superkassa.tests.framework.tags.ApiSmoke
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.SoftAssertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.ResourceAccessMode
import org.junit.jupiter.api.parallel.ResourceLock

@ApiSmoke
@Feature("API")
@Story("GET /kkm/{kkmId}/users")
@Owner("Pavel Michka")
@DisplayName("GET /kkm/{kkmId}/users: smoke-проверки списка пользователей ККМ")
@ResourceLock(value = "kkm-users", mode = ResourceAccessMode.READ)
@Suppress("SameParameterValue")
class KkmUsersSmokeTest : KkmAuthenticatedTest() {
    @Test
    @Severity(SeverityLevel.BLOCKER)
    @DisplayName("Метод GET /kkm/{kkmId}/users возвращает HTTP 200 и JSON")
    fun shouldReturnUsersSuccessfully() {

        getUsersJson(preparedKkm)
    }

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Метод GET /kkm/{kkmId}/users возвращает непустой список пользователей")
    fun shouldReturnNonEmptyUsersList() {
        val users = getUsersJson(preparedKkm).getList<Map<String, Any?>>("")

        assertThat(users)
            .withFailMessage(
                "Функциональность API нарушена: GET /kkm/%s/users должен вернуть непустой список пользователей ККМ.",
                preparedKkm.kkmId,
            )
            .isNotEmpty()
    }

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Метод GET /kkm/{kkmId}/users возвращает обязательные поля каждого пользователя")
    fun shouldReturnRequiredUserFields() {
        val users = getUsersJson(preparedKkm).getList<Map<String, Any?>>("")

        assertThat(users).isNotNull()

        SoftAssertions().apply {
            users.forEachIndexed { index, user ->
                assertRequiredFieldPresent(this, user["name"], ENDPOINT, "users[$index].name", "UserResponse")
                assertRequiredFieldPresent(this, user["role"], ENDPOINT, "users[$index].role", "UserResponse")
                assertRequiredFieldPresent(this, user["userId"], ENDPOINT, "users[$index].userId", "UserResponse")
            }
        }.assertAll()
    }

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Метод GET /kkm/{kkmId}/users возвращает заполненные обязательные поля каждого пользователя")
    fun shouldReturnFilledRequiredUserFields() {
        val users = getUsersJson(preparedKkm).getList<Map<String, Any?>>("")

        assertThat(users).isNotNull()

        SoftAssertions().apply {
            users.forEachIndexed { index, user ->
                assertRequiredFieldFilled(this, user["name"], ENDPOINT, "users[$index].name", "UserResponse")
                assertRequiredFieldFilled(this, user["role"], ENDPOINT, "users[$index].role", "UserResponse")
                assertRequiredFieldFilled(this, user["userId"], ENDPOINT, "users[$index].userId", "UserResponse")
            }
        }.assertAll()
    }

    private fun getUsersJson(preparedKkm: PreparedKkmAuth): JsonPath {
        val response: Response = reportStep("Получаем пользователей ККМ через GET /kkm/${preparedKkm.kkmId}/users") {
            superkassa.request(preparedKkm.adminPin)
                .`when`()
                .get("/kkm/{kkmId}/users", preparedKkm.kkmId)
                .then()
                .shouldHaveStatus(200, "успешное получение списка пользователей ККМ")
                .contentType(ContentType.JSON)
                .extract()
                .response()
        }

        return response.jsonPath()
    }

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
        const val ENDPOINT = "GET /kkm/{kkmId}/users"
    }
}
