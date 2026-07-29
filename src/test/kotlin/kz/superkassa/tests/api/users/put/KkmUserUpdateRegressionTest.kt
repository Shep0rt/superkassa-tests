package kz.superkassa.tests.api.users.put

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
import kz.superkassa.tests.framework.contract.ApiEnumValues
import kz.superkassa.tests.framework.kkm.PreparedKkmAuth
import kz.superkassa.tests.framework.reporting.reportStep
import kz.superkassa.tests.framework.tags.ApiRegression
import org.assertj.core.api.SoftAssertions
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.ResourceAccessMode
import org.junit.jupiter.api.parallel.ResourceLock
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.MethodSource
import java.util.UUID
import java.util.concurrent.ThreadLocalRandom
import java.util.stream.Stream

@ApiRegression
@Feature("API")
@Story("PUT /kkm/{kkmId}/users/{userId}")
@Owner("Pavel Michka")
@DisplayName("PUT /kkm/{kkmId}/users/{userId}: регрессионные проверки редактирования пользователя ККМ")
@ResourceLock(value = "kkm-users", mode = ResourceAccessMode.READ_WRITE)
@Suppress("SameParameterValue", "NonAsciiCharacters")
class KkmUserUpdateRegressionTest : KkmAuthenticatedTest() {
    private val deferredCleanupActions = mutableListOf<() -> Unit>()
    private lateinit var createdUser: CreatedUser

    @AfterEach
    fun `Очищаем тестовые данные после проверки`() {
        val cleanups = deferredCleanupActions.asReversed().toList()
        deferredCleanupActions.clear()
        if (cleanups.isEmpty()) {
            reportStep("Очистка не требуется: пользователь для редактирования не был подготовлен") { }
            return
        }
        cleanups.forEach { cleanup -> cleanup() }
    }

    @Nested
    @ApiRegression
    @DisplayName("Позитивные проверки PUT /kkm/{kkmId}/users/{userId}")
    @ResourceLock(value = "kkm-users", mode = ResourceAccessMode.READ_WRITE)
    inner class PositiveRegressionTests {
        @BeforeEach
        fun `Создаем пользователя для редактирования`() {
            createdUser = prepareUserForUpdate()
        }

        @Test
        @Severity(SeverityLevel.CRITICAL)
        @DisplayName("Метод PUT /kkm/{kkmId}/users/{userId} возвращает поля ожидаемых типов")
        fun shouldReturnExpectedFieldTypes() {
            withUpdatedUser(validUpdateBody()) { updateResponse ->
                val response = updateResponse.jsonPath().getMap<String, Any?>("")

                SoftAssertions().apply {
                    assertFieldType(this, response, "name", String::class.java, RESPONSE_SCHEMA)
                    assertFieldType(this, response, "role", String::class.java, RESPONSE_SCHEMA)
                    assertFieldType(this, response, "userId", String::class.java, RESPONSE_SCHEMA)
                    assertOptionalFieldType(this, response, "pin", String::class.java, RESPONSE_SCHEMA)
                }.assertAll()
            }
        }

        @Test
        @Severity(SeverityLevel.CRITICAL)
        @DisplayName("Метод PUT /kkm/{kkmId}/users/{userId} не возвращает поля вне Swagger-контракта")
        fun shouldNotReturnFieldsOutsideSwaggerContract() {
            withUpdatedUser(validUpdateBody()) { updateResponse ->
                val response = updateResponse.jsonPath().getMap<String, Any?>("")

                SoftAssertions().apply {
                    assertOnlySwaggerFields(this, response, RESPONSE_SCHEMA, USER_RESPONSE_FIELDS)
                }.assertAll()
            }
        }

        @Test
        @Severity(SeverityLevel.CRITICAL)
        @DisplayName("Метод PUT /kkm/{kkmId}/users/{userId} возвращает допустимую роль пользователя")
        fun shouldReturnExpectedUserRole() {
            withUpdatedUser(validUpdateBody()) { updateResponse ->
                val response = updateResponse.jsonPath().getMap<String, Any?>("")

                SoftAssertions().apply {
                    assertRequiredEnumValue(this, response, "role", RESPONSE_SCHEMA, ApiEnumValues.USER_ROLES)
                }.assertAll()
            }
        }

    }

    @Nested
    @ApiRegression
    @DisplayName("Негативные проверки PUT /kkm/{kkmId}/users/{userId}")
    @ResourceLock(value = "kkm-users", mode = ResourceAccessMode.READ_WRITE)
    inner class NegativeRegressionTests {
        @Nested
        @ApiRegression
        @DisplayName("Проверки невалидного тела запроса")
        @ResourceLock(value = "kkm-users", mode = ResourceAccessMode.READ_WRITE)
        inner class InvalidRequestBodyTests {
            @BeforeEach
            fun `Создаем пользователя для проверки невалидного тела`() {
                createdUser = prepareUserForUpdate()
            }

            @Nested
            @DisplayName("Общая структура тела запроса")
            inner class RequestBodyStructureTests {
                @Test
                @Severity(SeverityLevel.NORMAL)
                @DisplayName("Метод PUT /kkm/{kkmId}/users/{userId} возвращает 400 для пустого объекта UserUpdateRequest")
                fun shouldReturnBadRequestForEmptyUpdateObject() {
                    val scenario = "в UserUpdateRequest не передано ни одного поля для изменения"

                    putUserExpectingRejection(
                        createdUser = createdUser,
                        body = emptyMap(),
                        stepName = "Отправляем пустой объект PUT ${userPath(createdUser)}: $scenario",
                        expectedStatus = 400,
                        scenario = scenario,
                    )
                }

                @Test
                @Severity(SeverityLevel.NORMAL)
                @DisplayName("Метод PUT /kkm/{kkmId}/users/{userId} возвращает 400 без тела запроса")
                fun shouldReturnBadRequestWithoutRequestBody() {
                    reportStep("Отправляем PUT ${userPath(createdUser)} без тела запроса") {
                        superkassa.request(createdUser.preparedKkm.adminPin)
                            .`when`()
                            .put(userPath(createdUser))
                            .then()
                            .shouldHaveStatus(400, "обязательное тело запроса отсутствует")
                            .contentType(ContentType.JSON)
                    }
                }
            }

            @Nested
            @DisplayName("Поле name")
            inner class NameFieldTests {
                @Test
                @Severity(SeverityLevel.NORMAL)
                @DisplayName("Возвращает 400, если единственное поле содержит null")
                fun shouldReturnBadRequestForNullValue() =
                    rejectFieldValue("поле name содержит null, других изменений нет", "name", null)

                @Test
                @Severity(SeverityLevel.NORMAL)
                @DisplayName("Возвращает 400 для неверного типа")
                fun shouldReturnBadRequestForInvalidType() =
                    rejectFieldValue("поле name имеет тип Number вместо String", "name", 123)
            }

            @Nested
            @DisplayName("Поле role")
            inner class RoleFieldTests {
                @Test
                @Severity(SeverityLevel.NORMAL)
                @DisplayName("Возвращает 400, если единственное поле содержит null")
                fun shouldReturnBadRequestForNullValue() =
                    rejectFieldValue("поле role содержит null, других изменений нет", "role", null)

                @Test
                @Severity(SeverityLevel.NORMAL)
                @DisplayName("Возвращает 400 для неверного типа")
                fun shouldReturnBadRequestForInvalidType() =
                    rejectFieldValue("поле role имеет тип Boolean вместо String", "role", true)

                @ParameterizedTest(name = "{0}")
                @MethodSource("kz.superkassa.tests.api.users.put.KkmUserUpdateRegressionTest#invalidUserRoles")
                @Severity(SeverityLevel.NORMAL)
                @DisplayName("Возвращает 400 для невалидного значения")
                fun shouldReturnBadRequestForInvalidValue(caseName: String, invalidRole: String) =
                    rejectFieldValue(caseName, "role", invalidRole)
            }

            @Nested
            @DisplayName("Поле userPin")
            inner class UserPinFieldTests {
                @Test
                @Severity(SeverityLevel.NORMAL)
                @DisplayName("Возвращает 400, если единственное поле содержит null")
                fun shouldReturnBadRequestForNullValue() =
                    rejectFieldValue("поле userPin содержит null, других изменений нет", "userPin", null)

                @Test
                @Severity(SeverityLevel.NORMAL)
                @DisplayName("Возвращает 400 для неверного типа")
                fun shouldReturnBadRequestForInvalidType() =
                    rejectFieldValue("поле userPin имеет тип Number вместо String", "userPin", 1234)

                @ParameterizedTest(name = "{0}")
                @MethodSource("kz.superkassa.tests.api.users.put.KkmUserUpdateRegressionTest#invalidUserPins")
                @Severity(SeverityLevel.NORMAL)
                @DisplayName("Возвращает 400 для невалидного значения")
                fun shouldReturnBadRequestForInvalidValue(caseName: String, invalidPin: String) =
                    rejectFieldValue(caseName, "userPin", invalidPin)
            }

            private fun rejectFieldValue(caseName: String, fieldName: String, invalidValue: Any?) {
                putUserExpectingRejection(
                    createdUser = createdUser,
                    body = mapOf(fieldName to invalidValue),
                    stepName = "Отправляем невалидное тело PUT ${userPath(createdUser)}: $caseName",
                    expectedStatus = 400,
                    scenario = caseName,
                )
            }

        }

        @Nested
        @ApiRegression
        @DisplayName("Проверки авторизации PUT /kkm/{kkmId}/users/{userId}")
        @ResourceLock(value = "kkm-users", mode = ResourceAccessMode.READ_WRITE)
        inner class AuthorizationRegressionTests {
            @BeforeEach
            fun `Создаем пользователя для проверки авторизации`() {
                createdUser = prepareUserForUpdate()
            }

            @Test
            @Severity(SeverityLevel.NORMAL)
            @DisplayName("Метод PUT /kkm/{kkmId}/users/{userId} возвращает 401 без Authorization")
            fun shouldReturnUnauthorizedWithoutAuthorization() {
                reportStep("Проверяем PUT ${userPath(createdUser)} без Authorization") {
                    superkassa.requestWithoutAuthorization()
                        .body(validUpdateBody())
                        .`when`()
                        .put(userPath(createdUser))
                        .then()
                        .shouldHaveStatus(401, "запрос без Authorization")
                        .contentType(ContentType.JSON)
                }
            }

            @Test
            @Severity(SeverityLevel.NORMAL)
            @DisplayName("Метод PUT /kkm/{kkmId}/users/{userId} возвращает 403 для неверного PIN")
            fun shouldReturnForbiddenForInvalidPin() {
                reportStep("Проверяем PUT ${userPath(createdUser)} с неверным PIN") {
                    superkassa.request(INVALID_PIN)
                        .body(validUpdateBody())
                        .`when`()
                        .put(userPath(createdUser))
                        .then()
                        .shouldHaveStatus(403, "запрос с неверным PIN")
                        .contentType(ContentType.JSON)
                }
            }
        }

        @Nested
        @ApiRegression
        @DisplayName("Проверки несуществующих идентификаторов")
        @ResourceLock(value = "kkm-users", mode = ResourceAccessMode.READ_WRITE)
        inner class MissingIdentifiersTests {
            @Test
            @Severity(SeverityLevel.NORMAL)
            @DisplayName("Метод PUT /kkm/{kkmId}/users/{userId} возвращает 404 для несуществующей ККМ")
            fun shouldReturnNotFoundForUnknownKkmId() {

                reportStep("Проверяем PUT /kkm/$UNKNOWN_KKM_ID/users/$UNKNOWN_USER_ID для несуществующей ККМ") {
                    superkassa.request(preparedKkm.adminPin)
                        .body(validUpdateBody())
                        .`when`()
                        .put(userPath(UNKNOWN_KKM_ID, UNKNOWN_USER_ID))
                        .then()
                        .shouldHaveStatus(404, "несуществующая ККМ")
                        .contentType(ContentType.JSON)
                }
            }

            @Test
            @Severity(SeverityLevel.NORMAL)
            @DisplayName("Метод PUT /kkm/{kkmId}/users/{userId} возвращает 404 для несуществующего пользователя")
            fun shouldReturnNotFoundForUnknownUserId() {

                reportStep("Проверяем PUT /kkm/${preparedKkm.kkmId}/users/$UNKNOWN_USER_ID для несуществующего пользователя") {
                    superkassa.request(preparedKkm.adminPin)
                        .body(validUpdateBody())
                        .`when`()
                        .put(userPath(preparedKkm.kkmId, UNKNOWN_USER_ID))
                        .then()
                        .shouldHaveStatus(404, "несуществующий пользователь")
                        .contentType(ContentType.JSON)
                }
            }

        }

        @Nested
        @ApiRegression
        @DisplayName("Проверки неподдерживаемых HTTP-методов")
        @ResourceLock(value = "kkm-users", mode = ResourceAccessMode.READ_WRITE)
        inner class UnsupportedHttpMethodsTests {
            @ParameterizedTest(name = "HTTP {0} /kkm/'{'kkmId'}'/users/'{'userId'}' возвращает 405")
            @EnumSource(value = Method::class, names = ["GET", "POST", "PATCH"])
            @Severity(SeverityLevel.NORMAL)
            @DisplayName("Метод /kkm/{kkmId}/users/{userId} возвращает 405 для HTTP-методов кроме PUT и DELETE")
            fun shouldReturnMethodNotAllowedForUnsupportedMethods(method: Method) {

                reportStep("Проверяем, что HTTP $method /kkm/${preparedKkm.kkmId}/users/$UNKNOWN_USER_ID не поддерживается") {
                    superkassa.request(preparedKkm.adminPin)
                        .`when`()
                        .request(method, userPath(preparedKkm.kkmId, UNKNOWN_USER_ID))
                        .then()
                        .shouldHaveStatus(405, "неподдерживаемый HTTP-метод")
                }
            }

        }
    }

    private fun withUpdatedUser(
        body: Map<String, Any?>,
        scenario: String? = null,
        assertions: (Response) -> Unit,
    ) {
        val response = updateUser(createdUser, body, scenario)
        assertions(response)
    }

    private fun prepareUserForUpdate(): CreatedUser {
        val request = newUserRequest()
        val response: Response = reportStep(
            "Готовим предусловие: создаем пользователя role=${request.role} через " +
                    "POST /kkm/${preparedKkm.kkmId}/users",
        ) {
            superkassa.request(preparedKkm.adminPin)
                .body(request.asBody())
                .`when`()
                .post(usersPath(preparedKkm.kkmId))
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

        return CreatedUser(preparedKkm, userId).also { createdUser ->
            deferredCleanupActions += { deleteCreatedUser(createdUser) }
        }
    }

    private fun updateUser(
        createdUser: CreatedUser,
        body: Map<String, Any?>,
        scenario: String?,
    ): Response {
        val scenarioDescription = scenario?.let { ": $it" }.orEmpty()

        return reportStep(
            "Редактируем пользователя userId='${createdUser.userId}' через PUT ${userPath(createdUser)}" +
                    scenarioDescription,
        ) {
            superkassa.request(createdUser.preparedKkm.adminPin)
                .body(body)
                .`when`()
                .put(userPath(createdUser))
                .then()
                .shouldHaveStatus(200, "валидный запрос редактирования пользователя ККМ")
                .contentType(ContentType.JSON)
                .extract()
                .response()
        }
    }

    private fun putUserExpectingRejection(
        createdUser: CreatedUser,
        body: Map<String, Any?>,
        stepName: String,
        expectedStatus: Int,
        scenario: String,
    ) {
        reportStep(stepName) {
            superkassa.request(createdUser.preparedKkm.adminPin)
                .body(body)
                .`when`()
                .put(userPath(createdUser))
                .then()
                .shouldHaveStatus(expectedStatus, scenario)
                .contentType(ContentType.JSON)
        }
    }

    private fun deleteCreatedUser(createdUser: CreatedUser) {
        reportStep("Удаляем тестового пользователя userId='${createdUser.userId}' после regression-проверки") {
            superkassa.request(createdUser.preparedKkm.adminPin)
                .`when`()
                .delete(userPath(createdUser))
                .then()
                .shouldHaveStatus(200, "удаление тестового пользователя после regression-проверки")
        }
    }

    private fun findCreatedUserId(preparedKkm: PreparedKkmAuth, userName: String): String? {
        val response: Response = reportStep("Ищем созданного пользователя по имени для очистки тестовых данных") {
            superkassa.request(preparedKkm.adminPin)
                .`when`()
                .get(usersPath(preparedKkm.kkmId))
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
        name = "Update regression кассир ${UUID.randomUUID().toString().take(8)}",
        role = CASHIER_ROLE,
        userPin = ThreadLocalRandom.current()
            .nextInt(MIN_TEST_PIN, MAX_TEST_PIN_EXCLUSIVE)
            .toString(),
    )

    private fun validUpdateBody(): Map<String, Any?> =
        mapOf("name" to "Updated regression кассир ${UUID.randomUUID().toString().take(8)}")

    private fun usersPath(kkmId: String): String = "/kkm/$kkmId/users"

    private fun userPath(createdUser: CreatedUser): String =
        userPath(createdUser.preparedKkm.kkmId, createdUser.userId)

    private fun userPath(kkmId: String, userId: String): String = "/kkm/$kkmId/users/$userId"

    private fun assertFieldType(
        softly: SoftAssertions,
        item: Map<String, Any?>,
        fieldName: String,
        expectedType: Class<*>,
        schemaName: String,
    ) {
        softly.assertThat(item)
            .withFailMessage(
                ApiContractErrorMessages.requiredFieldWithTypeMissing(
                    ENDPOINT,
                    fieldName,
                    expectedType.simpleName,
                    schemaName,
                ),
            )
            .containsKey(fieldName)

        softly.assertThat(item[fieldName])
            .withFailMessage(
                ApiContractErrorMessages.fieldTypeMismatch(
                    ENDPOINT,
                    fieldName,
                    expectedType.simpleName,
                    schemaName
                )
            )
            .isInstanceOf(expectedType)
    }

    private fun assertOptionalFieldType(
        softly: SoftAssertions,
        item: Map<String, Any?>,
        fieldName: String,
        expectedType: Class<*>,
        schemaName: String,
    ) {
        val fieldValue = item[fieldName] ?: return

        softly.assertThat(fieldValue)
            .withFailMessage(
                ApiContractErrorMessages.optionalFieldTypeMismatch(
                    ENDPOINT,
                    fieldName,
                    expectedType.simpleName,
                    schemaName
                )
            )
            .isInstanceOf(expectedType)
    }

    private fun assertOnlySwaggerFields(
        softly: SoftAssertions,
        item: Map<String, Any?>,
        schemaName: String,
        allowedFields: Set<String>,
    ) {
        val unexpectedFields = item.keys - allowedFields

        softly.assertThat(unexpectedFields)
            .withFailMessage(ApiContractErrorMessages.unexpectedSwaggerFields(ENDPOINT, schemaName, unexpectedFields))
            .isEmpty()
    }

    private fun assertRequiredEnumValue(
        softly: SoftAssertions,
        item: Map<String, Any?>,
        fieldName: String,
        schemaName: String,
        supportedValues: Set<String>,
    ) {
        val fieldValue = item[fieldName] as? String

        softly.assertThat(fieldValue)
            .withFailMessage(ApiContractErrorMessages.requiredEnumMissing(ENDPOINT, fieldName, schemaName))
            .isNotBlank()

        softly.assertThat(fieldValue)
            .withFailMessage(ApiContractErrorMessages.enumUnsupported(ENDPOINT, fieldName, fieldValue, supportedValues))
            .isIn(supportedValues)
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
        fun asBody(): Map<String, Any?> = mapOf(
            "name" to name,
            "role" to role,
            "userPin" to userPin,
        )
    }

    private companion object {
        const val ENDPOINT = "PUT /kkm/{kkmId}/users/{userId}"
        const val RESPONSE_SCHEMA = "UserResponse"
        const val CASHIER_ROLE = "CASHIER"
        const val UNSUPPORTED_ROLE = "UNKNOWN"
        const val INVALID_PIN = "999999"
        const val UNKNOWN_KKM_ID = "00000000-0000-0000-0000-000000000000"
        const val UNKNOWN_USER_ID = "00000000-0000-0000-0000-000000000001"
        const val MIN_TEST_PIN = 1_000
        const val MAX_TEST_PIN_EXCLUSIVE = 10_000

        val USER_RESPONSE_FIELDS = setOf(
            "name",
            "pin",
            "role",
            "userId",
        )

        @JvmStatic
        fun invalidUserRoles(): Stream<Arguments> = Stream.of(
            Arguments.of("поле role содержит неподдерживаемое значение", UNSUPPORTED_ROLE),
            Arguments.of("поле role содержит пустую строку", ""),
            Arguments.of("поле role состоит только из пробелов", "    "),
        )

        @JvmStatic
        fun invalidUserPins(): Stream<Arguments> = Stream.of(
            Arguments.of("userPin содержит пустую строку", ""),
            Arguments.of("userPin состоит только из пробелов", "    "),
            Arguments.of("userPin содержит меньше четырех цифр", "123"),
            Arguments.of("userPin содержит больше четырех цифр", "12345"),
            Arguments.of("userPin длиной четыре символа содержит не только цифры", "12A4"),
            Arguments.of("userPin длиной четыре символа содержит пробел в начале", " 123"),
            Arguments.of("userPin длиной четыре символа содержит пробел внутри", "12 4"),
            Arguments.of("userPin длиной четыре символа содержит пробел в конце", "123 "),
            Arguments.of("userPin длиной четыре символа содержит специальный символ", "12#4"),
        )
    }
}
