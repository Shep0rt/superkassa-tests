package kz.superkassa.tests.api.users.put

import io.qameta.allure.Feature
import io.qameta.allure.Owner
import io.qameta.allure.Severity
import io.qameta.allure.SeverityLevel
import io.qameta.allure.Story
import io.restassured.http.ContentType
import io.restassured.http.Method
import io.restassured.response.Response
import kz.superkassa.tests.framework.BaseTest
import kz.superkassa.tests.framework.assertions.ApiContractErrorMessages
import kz.superkassa.tests.framework.contract.ApiEnumValues
import kz.superkassa.tests.framework.kkm.PreparedKkmAuth
import kz.superkassa.tests.framework.reporting.reportStep
import kz.superkassa.tests.framework.tags.ApiRegression
import org.assertj.core.api.SoftAssertions
import org.junit.jupiter.api.AfterEach
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
class KkmUserUpdateRegressionTest : BaseTest() {
    private val deferredCleanupActions = mutableListOf<() -> Unit>()

    @AfterEach
    fun `Очищаем тестовые данные после проверки`() {
        val cleanups = deferredCleanupActions.asReversed().toList()
        deferredCleanupActions.clear()
        cleanups.forEach { cleanup -> cleanup() }
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

    @ParameterizedTest(name = "{0}")
    @MethodSource("nullableRequestFields")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("Метод PUT /kkm/{kkmId}/users/{userId} принимает null в nullable-полях запроса")
    fun shouldAcceptNullForNullableRequestField(caseName: String, fieldName: String) {
        withUpdatedUser(mapOf(fieldName to null), caseName) { }
    }

    @Test
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("Метод PUT /kkm/{kkmId}/users/{userId} принимает пустой объект UserUpdateRequest")
    fun shouldAcceptEmptyUpdateObject() {
        withUpdatedUser(emptyMap()) { }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidFieldTypes")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("Метод PUT /kkm/{kkmId}/users/{userId} возвращает 400 для неверного типа поля запроса")
    fun shouldReturnBadRequestForInvalidFieldType(caseName: String, fieldName: String, invalidValue: Any) {
        val createdUser = prepareUserForUpdate()

        putUserExpectingRejection(
            createdUser = createdUser,
            body = mapOf(fieldName to invalidValue),
            stepName = "Отправляем невалидное тело PUT ${userPath(createdUser)}: $caseName",
            expectedStatus = 400,
            scenario = caseName,
        )
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidUserRoles")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("Метод PUT /kkm/{kkmId}/users/{userId} возвращает 400 для невалидной роли")
    fun shouldReturnBadRequestForInvalidRole(caseName: String, invalidRole: String) {
        val createdUser = prepareUserForUpdate()

        putUserExpectingRejection(
            createdUser = createdUser,
            body = mapOf("role" to invalidRole),
            stepName = "Отправляем невалидное тело PUT ${userPath(createdUser)}: $caseName",
            expectedStatus = 400,
            scenario = caseName,
        )
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidUserPins")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("Метод PUT /kkm/{kkmId}/users/{userId} возвращает 400 для невалидного PIN пользователя")
    fun shouldReturnBadRequestForInvalidUserPin(caseName: String, invalidPin: String) {
        val createdUser = prepareUserForUpdate()

        putUserExpectingRejection(
            createdUser = createdUser,
            body = mapOf("userPin" to invalidPin),
            stepName = "Отправляем невалидное тело PUT ${userPath(createdUser)}: $caseName",
            expectedStatus = 400,
            scenario = caseName,
        )
    }

    @Test
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("Метод PUT /kkm/{kkmId}/users/{userId} возвращает 400 без тела запроса")
    fun shouldReturnBadRequestWithoutRequestBody() {
        val createdUser = prepareUserForUpdate()

        reportStep("Отправляем PUT ${userPath(createdUser)} без тела запроса") {
            superkassa.request(createdUser.preparedKkm.adminPin)
                .`when`()
                .put(userPath(createdUser))
                .then()
                .shouldHaveStatus(400, "обязательное тело запроса отсутствует")
                .contentType(ContentType.JSON)
        }
    }

    @Nested
    @ApiRegression
    @Feature("API")
    @Story("PUT /kkm/{kkmId}/users/{userId}")
    @Owner("Pavel Michka")
    @DisplayName("Проверки авторизации PUT /kkm/{kkmId}/users/{userId}")
    @ResourceLock(value = "kkm-users", mode = ResourceAccessMode.READ_WRITE)
    inner class AuthorizationRegressionTests {
        @Test
        @Severity(SeverityLevel.NORMAL)
        @DisplayName("Метод PUT /kkm/{kkmId}/users/{userId} возвращает 401 без Authorization")
        fun shouldReturnUnauthorizedWithoutAuthorization() {
            val createdUser = prepareUserForUpdate()

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
            val createdUser = prepareUserForUpdate()

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

    @Test
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("Метод PUT /kkm/{kkmId}/users/{userId} возвращает 404 для несуществующей ККМ")
    fun shouldReturnNotFoundForUnknownKkmId() {
        val preparedKkm = kkmAuth.prepareFirstKkmAdminPin()

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
        val preparedKkm = kkmAuth.prepareFirstKkmAdminPin()

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

    @ParameterizedTest(name = "HTTP {0} /kkm/'{'kkmId'}'/users/'{'userId'}' возвращает 405")
    @EnumSource(value = Method::class, names = ["GET", "POST", "PATCH"])
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("Метод /kkm/{kkmId}/users/{userId} возвращает 405 для HTTP-методов кроме PUT и DELETE")
    fun shouldReturnMethodNotAllowedForUnsupportedMethods(method: Method) {
        val preparedKkm = kkmAuth.prepareFirstKkmAdminPin()

        reportStep("Проверяем, что HTTP $method /kkm/${preparedKkm.kkmId}/users/$UNKNOWN_USER_ID не поддерживается") {
            superkassa.request(preparedKkm.adminPin)
                .`when`()
                .request(method, userPath(preparedKkm.kkmId, UNKNOWN_USER_ID))
                .then()
                .shouldHaveStatus(405, "неподдерживаемый HTTP-метод")
        }
    }

    private fun withUpdatedUser(
        body: Map<String, Any?>,
        scenario: String? = null,
        assertions: (Response) -> Unit,
    ) {
        val createdUser = prepareUserForUpdate()
        val response = updateUser(createdUser, body, scenario)
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
            .withFailMessage(ApiContractErrorMessages.fieldTypeMismatch(ENDPOINT, fieldName, expectedType.simpleName, schemaName))
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
            .withFailMessage(ApiContractErrorMessages.optionalFieldTypeMismatch(ENDPOINT, fieldName, expectedType.simpleName, schemaName))
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
        fun nullableRequestFields(): Stream<Arguments> = Stream.of(
            Arguments.of("поле name содержит допустимое значение null", "name"),
            Arguments.of("поле role содержит допустимое значение null", "role"),
            Arguments.of("поле userPin содержит допустимое значение null", "userPin"),
        )

        @JvmStatic
        fun invalidFieldTypes(): Stream<Arguments> = Stream.of(
            Arguments.of("поле name имеет тип Number вместо String", "name", 123),
            Arguments.of("поле role имеет тип Boolean вместо String", "role", true),
            Arguments.of("поле userPin имеет тип Number вместо String", "userPin", 1234),
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
