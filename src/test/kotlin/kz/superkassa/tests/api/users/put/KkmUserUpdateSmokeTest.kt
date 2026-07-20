package kz.superkassa.tests.api.users.put

import io.qameta.allure.Feature
import io.qameta.allure.Owner
import io.qameta.allure.Severity
import io.qameta.allure.SeverityLevel
import io.qameta.allure.Story
import io.restassured.http.ContentType
import io.restassured.path.json.JsonPath
import io.restassured.response.Response
import kz.superkassa.tests.framework.BaseTest
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
@Story("PUT /kkm/{kkmId}/users/{userId}")
@Owner("Pavel Michka")
@DisplayName("PUT /kkm/{kkmId}/users/{userId}: smoke-проверки редактирования пользователя ККМ")
@ResourceLock(value = "kkm-users", mode = ResourceAccessMode.READ_WRITE)
@Suppress("SameParameterValue", "NonAsciiCharacters")
class KkmUserUpdateSmokeTest : BaseTest() {
    private val deferredCleanupActions = mutableListOf<() -> Unit>()

    @AfterEach
    fun `Очищаем тестовые данные после проверки`() {
        val cleanups = deferredCleanupActions.asReversed().toList()
        deferredCleanupActions.clear()
        cleanups.forEach { cleanup -> cleanup() }
    }

    @Test
    @Severity(SeverityLevel.BLOCKER)
    @DisplayName("Метод PUT /kkm/{kkmId}/users/{userId} при валидном запросе возвращает HTTP 200 и JSON")
    fun shouldReturnSuccessForValidRequest() {
        withUpdatedUser { }
    }

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Метод PUT /kkm/{kkmId}/users/{userId} возвращает обязательные поля отредактированного пользователя")
    fun shouldReturnRequiredUpdatedUserFields() {
        withUpdatedUser { response ->
            val json = response.jsonPath()

            SoftAssertions().apply {
                assertRequiredFieldPresent(this, value(json, "name"), ENDPOINT, "name", RESPONSE_SCHEMA)
                assertRequiredFieldPresent(this, value(json, "role"), ENDPOINT, "role", RESPONSE_SCHEMA)
                assertRequiredFieldPresent(this, value(json, "userId"), ENDPOINT, "userId", RESPONSE_SCHEMA)
            }.assertAll()
        }
    }

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Метод PUT /kkm/{kkmId}/users/{userId} возвращает заполненные обязательные поля отредактированного пользователя")
    fun shouldReturnFilledRequiredUpdatedUserFields() {
        withUpdatedUser { response ->
            val json = response.jsonPath()

            SoftAssertions().apply {
                assertRequiredFieldFilled(this, value(json, "name"), ENDPOINT, "name", RESPONSE_SCHEMA)
                assertRequiredFieldFilled(this, value(json, "role"), ENDPOINT, "role", RESPONSE_SCHEMA)
                assertRequiredFieldFilled(this, value(json, "userId"), ENDPOINT, "userId", RESPONSE_SCHEMA)
            }.assertAll()
        }
    }

    private fun withUpdatedUser(assertions: (Response) -> Unit) {
        val createdUser = prepareUserForUpdate()
        deferredCleanupActions += { deleteCreatedUser(createdUser) }

        val response = updateUser(createdUser)
        assertions(response)
    }

    private fun prepareUserForUpdate(): CreatedUser {
        val preparedKkm = kkmAuth.prepareFirstKkmAdminPin()
        val request = newUserRequest()
        val response: Response = reportStep(
            "Готовим предусловие: создаем пользователя role=${request.role} через " +
                "POST /kkm/${preparedKkm.kkmId}/users",
        ) {
            superkassa.request(preparedKkm.adminPin)
                .body(request.asBody())
                .`when`()
                .post("/kkm/{kkmId}/users", preparedKkm.kkmId)
                .then()
                .shouldHaveStatus(200, "предусловие: создание пользователя для проверки PUT")
                .contentType(ContentType.JSON)
                .extract()
                .response()
        }

        val userId = response.jsonPath().getString("userId")
            ?.takeIf(String::isNotBlank)
            ?: findCreatedUserId(preparedKkm, request.name)
            ?: error(
                "Не удалось подготовить тестовые данные: созданный пользователь '${request.name}' " +
                    "не найден в ККМ '${preparedKkm.kkmId}'.",
            )

        return CreatedUser(preparedKkm, userId)
    }

    private fun updateUser(createdUser: CreatedUser): Response =
        reportStep(
            "Редактируем пользователя userId='${createdUser.userId}' через " +
                "PUT /kkm/${createdUser.preparedKkm.kkmId}/users/${createdUser.userId}",
        ) {
            superkassa.request(createdUser.preparedKkm.adminPin)
                .body(mapOf("name" to "Updated smoke кассир ${UUID.randomUUID().toString().take(8)}"))
                .`when`()
                .put(
                    "/kkm/{kkmId}/users/{userId}",
                    createdUser.preparedKkm.kkmId,
                    createdUser.userId,
                )
                .then()
                .shouldHaveStatus(200, "успешное редактирование пользователя ККМ")
                .contentType(ContentType.JSON)
                .extract()
                .response()
        }

    private fun deleteCreatedUser(createdUser: CreatedUser) {
        reportStep("Удаляем тестового пользователя userId='${createdUser.userId}' после smoke-проверки") {
            superkassa.request(createdUser.preparedKkm.adminPin)
                .`when`()
                .delete(
                    "/kkm/{kkmId}/users/{userId}",
                    createdUser.preparedKkm.kkmId,
                    createdUser.userId,
                )
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
        name = "Update smoke кассир ${UUID.randomUUID().toString().take(8)}",
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

    private data class CreatedUser(
        val preparedKkm: PreparedKkmAuth,
        val userId: String,
    )

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
        const val ENDPOINT = "PUT /kkm/{kkmId}/users/{userId}"
        const val RESPONSE_SCHEMA = "UserResponse"
        const val CASHIER_ROLE = "CASHIER"
        const val MIN_TEST_PIN = 1_000
        const val MAX_TEST_PIN_EXCLUSIVE = 10_000
    }
}
