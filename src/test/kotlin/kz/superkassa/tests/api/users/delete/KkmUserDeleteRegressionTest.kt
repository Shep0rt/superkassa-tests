package kz.superkassa.tests.api.users.delete

import io.qameta.allure.Feature
import io.qameta.allure.Owner
import io.qameta.allure.Severity
import io.qameta.allure.SeverityLevel
import io.qameta.allure.Story
import io.restassured.http.ContentType
import io.restassured.http.Method
import io.restassured.response.Response
import kz.superkassa.tests.framework.kkm.KkmAuthenticatedTest
import kz.superkassa.tests.framework.assertions.ApiContractErrorMessages
import kz.superkassa.tests.framework.kkm.PreparedKkmAuth
import kz.superkassa.tests.framework.reporting.reportStep
import kz.superkassa.tests.framework.tags.ApiRegression
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.SoftAssertions
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.ResourceAccessMode
import org.junit.jupiter.api.parallel.ResourceLock
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.util.UUID
import java.util.concurrent.ThreadLocalRandom

@ApiRegression
@Feature("API")
@Story("DELETE /kkm/{kkmId}/users/{userId}")
@Owner("Pavel Michka")
@DisplayName("DELETE /kkm/{kkmId}/users/{userId}: регрессионные проверки удаления пользователя ККМ")
@ResourceLock(value = "kkm-users", mode = ResourceAccessMode.READ_WRITE)
@Suppress("SameParameterValue", "NonAsciiCharacters")
class KkmUserDeleteRegressionTest : KkmAuthenticatedTest() {
    @Nested
    @ApiRegression
    @Feature("API")
    @Story("DELETE /kkm/{kkmId}/users/{userId}")
    @Owner("Pavel Michka")
    @DisplayName("Позитивные проверки DELETE /kkm/{kkmId}/users/{userId}")
    @ResourceLock(value = "kkm-users", mode = ResourceAccessMode.READ_WRITE)
    inner class PositiveRegressionTests {
        private lateinit var createdUser: CreatedUser

        @BeforeEach
        fun `Создаем пользователя для удаления`() {
            createdUser = prepareUserForDeletion()
        }

        @Test
        @Severity(SeverityLevel.CRITICAL)
        @DisplayName("Метод DELETE /kkm/{kkmId}/users/{userId} возвращает поле ok типа Boolean")
        fun shouldReturnOkFieldWithExpectedType() {
            val response = deleteUser(createdUser)
            val actualValue = response[OK_FIELD]

            SoftAssertions().apply {
                assertThat(response)
                    .withFailMessage(
                        ApiContractErrorMessages.documentedFieldMissing(
                            ENDPOINT,
                            OK_FIELD,
                            RESPONSE_STRUCTURE
                        )
                    )
                    .containsKey(OK_FIELD)
                assertThat(response[OK_FIELD])
                    .withFailMessage(
                        ApiContractErrorMessages.documentedFieldTypeMismatch(
                            ENDPOINT,
                            OK_FIELD,
                            Boolean::class.javaObjectType.simpleName,
                            actualValue,
                            RESPONSE_STRUCTURE,
                        ),
                    )
                    .isInstanceOf(Boolean::class.javaObjectType)
            }.assertAll()
        }

        @Test
        @Severity(SeverityLevel.CRITICAL)
        @DisplayName("Метод DELETE /kkm/{kkmId}/users/{userId} возвращает ok=true")
        fun shouldReturnExpectedOkValue() {
            val response = deleteUser(createdUser)
            val actualValue = response[OK_FIELD]

            assertThat(actualValue)
                .withFailMessage(
                    ApiContractErrorMessages.documentedFieldValueMismatch(
                        ENDPOINT,
                        OK_FIELD,
                        true,
                        actualValue,
                        RESPONSE_STRUCTURE,
                    ),
                )
                .isEqualTo(true)
        }

        @Test
        @Severity(SeverityLevel.CRITICAL)
        @DisplayName("Метод DELETE /kkm/{kkmId}/users/{userId} не возвращает поля вне ожидаемого контракта")
        fun shouldNotReturnFieldsOutsideExpectedContract() {
            val response = deleteUser(createdUser)
            val unexpectedFields = response.keys - RESPONSE_FIELDS

            assertThat(unexpectedFields)
                .withFailMessage(
                    ApiContractErrorMessages.unexpectedDocumentedFields(
                        ENDPOINT,
                        RESPONSE_STRUCTURE,
                        unexpectedFields,
                    ),
                )
                .isEmpty()
        }
    }

    @Nested
    @ApiRegression
    @Feature("API")
    @Story("DELETE /kkm/{kkmId}/users/{userId}")
    @Owner("Pavel Michka")
    @DisplayName("Негативные проверки DELETE /kkm/{kkmId}/users/{userId}")
    @ResourceLock(value = "kkm-users", mode = ResourceAccessMode.READ_WRITE)
    inner class NegativeRegressionTests {
        @Nested
        @ApiRegression
        @Feature("API")
        @Story("DELETE /kkm/{kkmId}/users/{userId}")
        @Owner("Pavel Michka")
        @DisplayName("Проверки авторизации DELETE /kkm/{kkmId}/users/{userId}")
        @ResourceLock(value = "kkm-users", mode = ResourceAccessMode.READ_WRITE)
        inner class AuthorizationRegressionTests {
            private lateinit var createdUser: CreatedUser

            @BeforeEach
            fun `Создаем пользователя для проверки авторизации`() {
                createdUser = prepareUserForDeletion()
            }

            @AfterEach
            fun `Очищаем тестовые данные после проверки авторизации`() {
                deleteUserForCleanup(createdUser)
            }

            @Test
            @Severity(SeverityLevel.NORMAL)
            @DisplayName("Метод DELETE /kkm/{kkmId}/users/{userId} возвращает 401 без Authorization")
            fun shouldReturnUnauthorizedWithoutAuthorization() {
                reportStep("Проверяем DELETE ${userPath(createdUser)} без Authorization") {
                    superkassa.requestWithoutAuthorization()
                        .`when`()
                        .delete(userPath(createdUser))
                        .then()
                        .shouldHaveStatus(401, "запрос без Authorization")
                        .contentType(ContentType.JSON)
                }
            }

            @Test
            @Severity(SeverityLevel.NORMAL)
            @DisplayName("Метод DELETE /kkm/{kkmId}/users/{userId} возвращает 403 для неверного PIN")
            fun shouldReturnForbiddenForInvalidPin() {
                reportStep("Проверяем DELETE ${userPath(createdUser)} с неверным PIN") {
                    superkassa.request(INVALID_PIN)
                        .`when`()
                        .delete(userPath(createdUser))
                        .then()
                        .shouldHaveStatus(403, "запрос с неверным PIN")
                        .contentType(ContentType.JSON)
                }
            }

        }

        @Nested
        @ApiRegression
        @Feature("API")
        @Story("DELETE /kkm/{kkmId}/users/{userId}")
        @Owner("Pavel Michka")
        @DisplayName("Проверки несуществующих идентификаторов")
        inner class MissingIdentifiersTests {
            @Test
            @Severity(SeverityLevel.NORMAL)
            @DisplayName("Метод DELETE /kkm/{kkmId}/users/{userId} возвращает 404 для несуществующей ККМ")
            fun shouldReturnNotFoundForUnknownKkmId() {
                val path = userPath(UNKNOWN_KKM_ID, UUID.randomUUID().toString())

                reportStep("Проверяем DELETE $path для несуществующей ККМ") {
                    superkassa.request(preparedKkm.adminPin)
                        .`when`()
                        .delete(path)
                        .then()
                        .shouldHaveStatus(404, "несуществующая ККМ")
                        .contentType(ContentType.JSON)
                }
            }

            @Test
            @Severity(SeverityLevel.NORMAL)
            @DisplayName("Метод DELETE /kkm/{kkmId}/users/{userId} возвращает 404 для несуществующего пользователя")
            fun shouldReturnNotFoundForUnknownUserId() {
                val path = userPath(preparedKkm.kkmId, UUID.randomUUID().toString())

                reportStep("Проверяем DELETE $path для несуществующего пользователя") {
                    superkassa.request(preparedKkm.adminPin)
                        .`when`()
                        .delete(path)
                        .then()
                        .shouldHaveStatus(404, "несуществующий пользователь ККМ")
                        .contentType(ContentType.JSON)
                }
            }

        }

        @Nested
        @ApiRegression
        @Feature("API")
        @Story("DELETE /kkm/{kkmId}/users/{userId}")
        @Owner("Pavel Michka")
        @DisplayName("Проверки неподдерживаемых HTTP-методов")
        inner class UnsupportedHttpMethodsTests {
            @ParameterizedTest(name = "HTTP {0} /kkm/'{'kkmId'}'/users/'{'userId'}' возвращает 405")
            @EnumSource(value = Method::class, names = ["GET", "POST", "PATCH"])
            @Severity(SeverityLevel.NORMAL)
            @DisplayName("Метод /kkm/{kkmId}/users/{userId} возвращает 405 для HTTP-методов кроме DELETE и PUT")
            fun shouldReturnMethodNotAllowedForUnsupportedMethods(method: Method) {
                val path = userPath(preparedKkm.kkmId, UUID.randomUUID().toString())

                reportStep("Проверяем, что HTTP $method $path не поддерживается") {
                    superkassa.request(preparedKkm.adminPin)
                        .`when`()
                        .request(method, path)
                        .then()
                        .shouldHaveStatus(405, "неподдерживаемый HTTP-метод")
                }
            }

        }
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
        reportStep("Удаляем пользователя userId='${createdUser.userId}' через DELETE ${userPath(createdUser)}") {
            val response = superkassa.request(createdUser.preparedKkm.adminPin)
                .`when`()
                .delete(userPath(createdUser))
                .then()
                .extract()
                .response()

            response.then()
                .shouldHaveStatus(200, "успешное удаление пользователя ККМ")
                .contentType(ContentType.JSON)

            response.jsonPath().getMap("")
        }

    private fun deleteUserForCleanup(createdUser: CreatedUser) {
        reportStep("Удаляем оставшегося тестового пользователя userId='${createdUser.userId}'") {
            val response = superkassa.request(createdUser.preparedKkm.adminPin)
                .`when`()
                .delete(userPath(createdUser))
                .then()
                .extract()
                .response()

            assertThat(response.statusCode)
                .withFailMessage(
                    "Не удалось очистить тестовые данные: DELETE пользователя userId='%s' должен вернуть HTTP <200> " +
                            "или HTTP <404>, а вернул HTTP <%s>.",
                    createdUser.userId,
                    response.statusCode,
                )
                .isIn(200, 404)
        }
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
        name = "Delete regression кассир ${UUID.randomUUID().toString().take(8)}",
        role = CASHIER_ROLE,
        userPin = ThreadLocalRandom.current()
            .nextInt(MIN_TEST_PIN, MAX_TEST_PIN_EXCLUSIVE)
            .toString(),
    )

    private fun userPath(createdUser: CreatedUser): String =
        userPath(createdUser.preparedKkm.kkmId, createdUser.userId)

    private fun userPath(kkmId: String, userId: String): String = "/kkm/$kkmId/users/$userId"

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
        const val OK_FIELD = "ok"
        const val CASHIER_ROLE = "CASHIER"
        const val INVALID_PIN = "999999"
        const val UNKNOWN_KKM_ID = "00000000-0000-0000-0000-000000000000"
        const val MIN_TEST_PIN = 1_000
        const val MAX_TEST_PIN_EXCLUSIVE = 10_000

        val RESPONSE_FIELDS = setOf(OK_FIELD)
    }
}
