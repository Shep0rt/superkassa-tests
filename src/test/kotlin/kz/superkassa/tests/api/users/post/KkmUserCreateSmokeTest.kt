package kz.superkassa.tests.api.users.post

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
import org.assertj.core.api.SoftAssertions
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.ResourceAccessMode
import org.junit.jupiter.api.parallel.ResourceLock
import java.util.UUID
import java.util.concurrent.ThreadLocalRandom

@ApiSmoke
@Feature("API")
@Story("POST /kkm/{kkmId}/users")
@Owner("Pavel Michka")
@DisplayName("POST /kkm/{kkmId}/users: smoke-проверки создания пользователя ККМ")
@ResourceLock(value = "kkm-users", mode = ResourceAccessMode.READ_WRITE)
@Suppress("SameParameterValue", "NonAsciiCharacters")
class KkmUserCreateSmokeTest : KkmAuthenticatedTest() {
    private val deferredCleanupActions = mutableListOf<() -> Unit>()

    @AfterEach
    fun `Очищаем тестовые данные после проверки`() {
        val cleanups = deferredCleanupActions.asReversed().toList()
        deferredCleanupActions.clear()
        if (cleanups.isEmpty()) {
            reportStep("Очистка не требуется: пользователь для удаления не был создан") { }
            return
        }
        cleanups.forEach { cleanup -> cleanup() }
    }

    @Test
    @Severity(SeverityLevel.BLOCKER)
    @DisplayName("Метод POST /kkm/{kkmId}/users создает пользователя и возвращает HTTP 200 и JSON")
    fun shouldCreateUserSuccessfully() {
        withCreatedUser { }
    }

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Метод POST /kkm/{kkmId}/users возвращает обязательные поля созданного пользователя")
    fun shouldReturnRequiredCreatedUserFields() {
        withCreatedUser { response ->
            val json = response.jsonPath()

            SoftAssertions().apply {
                assertRequiredFieldPresent(this, value(json, "name"), ENDPOINT, "name", "UserResponse")
                assertRequiredFieldPresent(this, value(json, "role"), ENDPOINT, "role", "UserResponse")
                assertRequiredFieldPresent(this, value(json, "userId"), ENDPOINT, "userId", "UserResponse")
            }.assertAll()
        }
    }

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Метод POST /kkm/{kkmId}/users возвращает заполненные обязательные поля созданного пользователя")
    fun shouldReturnFilledRequiredCreatedUserFields() {
        withCreatedUser { response ->
            val json = response.jsonPath()

            SoftAssertions().apply {
                assertRequiredFieldFilled(this, value(json, "name"), ENDPOINT, "name", "UserResponse")
                assertRequiredFieldFilled(this, value(json, "role"), ENDPOINT, "role", "UserResponse")
                assertRequiredFieldFilled(this, value(json, "userId"), ENDPOINT, "userId", "UserResponse")
            }.assertAll()
        }
    }

    private fun withCreatedUser(assertions: (Response) -> Unit) {
        val request = newUserRequest()
        val response = createUser(preparedKkm, request)
        deferredCleanupActions += {
            deleteCreatedUser(preparedKkm, request, response)
        }

        assertions(response)
    }

    private fun createUser(preparedKkm: PreparedKkmAuth, request: UserCreateRequest): Response =
        reportStep("Создаем тестового пользователя role=${request.role} через POST /kkm/${preparedKkm.kkmId}/users") {
            superkassa.request(preparedKkm.adminPin)
                .body(request.asBody())
                .`when`()
                .post("/kkm/{kkmId}/users", preparedKkm.kkmId)
                .then()
                .shouldHaveStatus(200, "успешное создание пользователя ККМ")
                .contentType(ContentType.JSON)
                .extract()
                .response()
        }

    private fun deleteCreatedUser(
        preparedKkm: PreparedKkmAuth,
        request: UserCreateRequest,
        createResponse: Response,
    ) {
        val userId = createResponse.jsonPath().getString("userId")
            ?.takeIf(String::isNotBlank)
            ?: findCreatedUserId(preparedKkm, request.name)
            ?: error(
                "Не удалось очистить тестовые данные: созданный пользователь '${request.name}' " +
                        "не найден в ККМ '${preparedKkm.kkmId}'.",
            )

        reportStep("Удаляем тестового пользователя userId='$userId' после smoke-проверки") {
            superkassa.request(preparedKkm.adminPin)
                .`when`()
                .delete("/kkm/{kkmId}/users/{userId}", preparedKkm.kkmId, userId)
                .then()
                .shouldHaveStatus(200, "удаление тестового пользователя после smoke-проверки")
        }
    }

    private fun findCreatedUserId(preparedKkm: PreparedKkmAuth, userName: String): String? {
        val response: Response = reportStep("Ищем созданного пользователя по имени для очистки тестовых данных") {
            superkassa.request(preparedKkm.adminPin)
                .`when`()
                .get("/kkm/{kkmId}/users", preparedKkm.kkmId)
                .then()
                .shouldHaveStatus(200, "поиск созданного пользователя для очистки тестовых данных")
                .contentType(ContentType.JSON)
                .extract()
                .response()
        }

        return response.jsonPath()
            .getList<Map<String, Any?>>("")
            .firstOrNull { it["name"] == userName }
            ?.get("userId") as? String
    }

    private fun newUserRequest(): UserCreateRequest = UserCreateRequest(
        name = "Smoke кассир ${UUID.randomUUID().toString().take(8)}",
        role = CASHIER_ROLE,
        userPin = ThreadLocalRandom.current()
            .nextInt(MIN_TEST_PIN, MAX_TEST_PIN_EXCLUSIVE)
            .toString(),
    )

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

    private data class UserCreateRequest(
        val name: String,
        val role: String,
        val userPin: String,
    ) {
        fun asBody(): Map<String, String> = mapOf(
            "name" to name,
            "role" to role,
            "userPin" to userPin,
        )
    }

    private companion object {
        const val ENDPOINT = "POST /kkm/{kkmId}/users"
        const val CASHIER_ROLE = "CASHIER"
        const val MIN_TEST_PIN = 1_000
        const val MAX_TEST_PIN_EXCLUSIVE = 10_000
    }
}
