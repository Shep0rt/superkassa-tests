package kz.superkassa.tests.api.users.delete

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
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.ResourceAccessMode
import org.junit.jupiter.api.parallel.ResourceLock
import java.util.UUID
import java.util.concurrent.ThreadLocalRandom

@ApiSmoke
@Feature("API")
@Story("DELETE /kkm/{kkmId}/users/{userId}")
@Owner("Pavel Michka")
@DisplayName("DELETE /kkm/{kkmId}/users/{userId}: smoke-проверки удаления пользователя ККМ")
@ResourceLock(value = "kkm-users", mode = ResourceAccessMode.READ_WRITE)
@Suppress("SameParameterValue", "NonAsciiCharacters")
class KkmUserDeleteSmokeTest : KkmAuthenticatedTest() {
    private lateinit var createdUser: CreatedUser

    @BeforeEach
    fun `Создаем пользователя для удаления`() {
        createdUser = prepareUserForDeletion()
    }

    @Test
    @Severity(SeverityLevel.BLOCKER)
    @DisplayName("Метод DELETE /kkm/{kkmId}/users/{userId} удаляет пользователя и возвращает HTTP 200 и JSON")
    fun shouldDeleteUserSuccessfully() {
        deleteUser(createdUser)
    }

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Метод DELETE /kkm/{kkmId}/users/{userId} возвращает обязательное поле ok")
    fun shouldReturnRequiredOkField() {
        val response = deleteUser(createdUser)

        assertThat(response)
            .withFailMessage(ApiContractErrorMessages.documentedFieldMissing(ENDPOINT, "ok", RESPONSE_STRUCTURE))
            .containsKey("ok")
    }

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Метод DELETE /kkm/{kkmId}/users/{userId} возвращает заполненное обязательное поле ok")
    fun shouldReturnFilledOkField() {
        val response = deleteUser(createdUser)

        assertThat(response["ok"])
            .withFailMessage(ApiContractErrorMessages.documentedFieldEmpty(ENDPOINT, "ok", RESPONSE_STRUCTURE))
            .isNotNull()
    }

    private fun prepareUserForDeletion(): CreatedUser {
        val request = newUserRequest()
        val response: Response = reportStep(
            "Готовим предусловие: создаем пользователя role=${request.role} через POST /kkm/${preparedKkm.kkmId}/users",
        ) {
            superkassa.request(preparedKkm.adminPin)
                .body(request.asBody())
                .`when`()
                .post("/kkm/{kkmId}/users", preparedKkm.kkmId)
                .then()
                .shouldHaveStatus(200, "предусловие: создание пользователя для проверки DELETE")
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

    private fun deleteUser(createdUser: CreatedUser): Map<String, Any?> =
        reportStep(
            "Удаляем пользователя userId='${createdUser.userId}' через " +
                    "DELETE /kkm/${createdUser.preparedKkm.kkmId}/users/${createdUser.userId}",
        ) {
            val response = superkassa.request(createdUser.preparedKkm.adminPin)
                .`when`()
                .delete(
                    "/kkm/{kkmId}/users/{userId}",
                    createdUser.preparedKkm.kkmId,
                    createdUser.userId,
                )
                .then()
                .extract()
                .response()

            response.then()
                .shouldHaveStatus(200, "успешное удаление пользователя ККМ")
                .contentType(ContentType.JSON)

            response.jsonPath().getMap("")
        }

    private fun findCreatedUserId(preparedKkm: PreparedKkmAuth, userName: String): String? {
        val response: Response = reportStep("Ищем созданного пользователя по имени для подготовки DELETE-проверки") {
            superkassa.request(preparedKkm.adminPin)
                .`when`()
                .get("/kkm/{kkmId}/users", preparedKkm.kkmId)
                .then()
                .shouldHaveStatus(200, "поиск созданного пользователя для подготовки DELETE-проверки")
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
        name = "Delete smoke кассир ${UUID.randomUUID().toString().take(8)}",
        role = CASHIER_ROLE,
        userPin = ThreadLocalRandom.current()
            .nextInt(MIN_TEST_PIN, MAX_TEST_PIN_EXCLUSIVE)
            .toString(),
    )

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
        const val ENDPOINT = "DELETE /kkm/{kkmId}/users/{userId}"
        const val RESPONSE_STRUCTURE = "DeleteUserResponse"
        const val CASHIER_ROLE = "CASHIER"
        const val MIN_TEST_PIN = 1_000
        const val MAX_TEST_PIN_EXCLUSIVE = 10_000
    }
}
