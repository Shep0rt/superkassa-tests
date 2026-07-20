package kz.superkassa.tests.api.users.post

import io.qameta.allure.Feature
import io.qameta.allure.Owner
import io.qameta.allure.Severity
import io.qameta.allure.SeverityLevel
import io.qameta.allure.Story
import io.restassured.http.ContentType
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
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import java.util.UUID
import java.util.concurrent.ThreadLocalRandom
import java.util.stream.Stream

@ApiRegression
@Feature("API")
@Story("POST /kkm/{kkmId}/users")
@Owner("Pavel Michka")
@DisplayName("POST /kkm/{kkmId}/users: регрессионные проверки создания пользователя ККМ")
@ResourceLock(value = "kkm-users", mode = ResourceAccessMode.READ_WRITE)
@Suppress("SameParameterValue", "NonAsciiCharacters")
class KkmUserCreateRegressionTest : BaseTest() {
    private val deferredCleanupActions = mutableListOf<() -> Unit>()

    @AfterEach
    fun `Очищаем тестовые данные после проверки`() {
        val cleanups = deferredCleanupActions.asReversed().toList()
        deferredCleanupActions.clear()
        cleanups.forEach { cleanup -> cleanup() }
    }

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Метод POST /kkm/{kkmId}/users возвращает поля ожидаемых типов")
    fun shouldReturnExpectedFieldTypes() {
        withCreatedUser { createdUserResponse ->
            val response = createdUserResponse.jsonPath().getMap<String, Any?>("")

            SoftAssertions().apply {
                assertFieldType(this, response, "name", String::class.java, "UserResponse")
                assertFieldType(this, response, "role", String::class.java, "UserResponse")
                assertFieldType(this, response, "userId", String::class.java, "UserResponse")
                assertOptionalFieldType(this, response, "pin", String::class.java, "UserResponse")
            }.assertAll()
        }
    }

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Метод POST /kkm/{kkmId}/users не возвращает поля вне Swagger-контракта")
    fun shouldNotReturnFieldsOutsideSwaggerContract() {
        withCreatedUser { createdUserResponse ->
            val response = createdUserResponse.jsonPath().getMap<String, Any?>("")

            SoftAssertions().apply {
                assertOnlySwaggerFields(this, response, "UserResponse", USER_RESPONSE_FIELDS)
            }.assertAll()
        }
    }

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Метод POST /kkm/{kkmId}/users возвращает допустимую роль пользователя")
    fun shouldReturnExpectedUserRole() {
        withCreatedUser { createdUserResponse ->
            val response = createdUserResponse.jsonPath().getMap<String, Any?>("")

            SoftAssertions().apply {
                assertRequiredEnumValue(this, response, "role", "UserResponse", ApiEnumValues.USER_ROLES)
            }.assertAll()
        }
    }

    @ParameterizedTest(name = "Обязательное поле {0} отсутствует")
    @ValueSource(strings = ["name", "role", "userPin"])
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("Метод POST /kkm/{kkmId}/users возвращает 400 без обязательного поля запроса")
    fun shouldReturnBadRequestWithoutRequiredField(fieldName: String) {
        val preparedKkm = kkmAuth.prepareFirstKkmAdminPin()
        val request = newUserRequest()
        val body = request.asBody().toMutableMap().apply { remove(fieldName) }

        postUserExpectingRejection(
            preparedKkm = preparedKkm,
            request = request,
            body = body,
            stepName = "Отправляем POST /kkm/${preparedKkm.kkmId}/users без обязательного поля '$fieldName'",
            expectedStatus = 400,
            scenario = "обязательное поле '$fieldName' отсутствует",
        )
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidFieldTypes")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("Метод POST /kkm/{kkmId}/users возвращает 400 для неверного типа поля запроса")
    fun shouldReturnBadRequestForInvalidFieldType(caseName: String, fieldName: String, invalidValue: Any) {
        val preparedKkm = kkmAuth.prepareFirstKkmAdminPin()
        val request = newUserRequest()
        val body = request.asBody().toMutableMap().apply { this[fieldName] = invalidValue }

        postUserExpectingRejection(
            preparedKkm = preparedKkm,
            request = request,
            body = body,
            stepName = "Отправляем невалидное тело POST /kkm/${preparedKkm.kkmId}/users: $caseName",
            expectedStatus = 400,
            scenario = caseName,
        )
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidUserNames")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("Метод POST /kkm/{kkmId}/users возвращает 400 для невалидного имени пользователя")
    fun shouldReturnBadRequestForInvalidName(caseName: String, invalidName: Any?) {
        val preparedKkm = kkmAuth.prepareFirstKkmAdminPin()
        val request = newUserRequest()
        val body = request.asBody().toMutableMap().apply { this["name"] = invalidName }

        postUserExpectingRejection(
            preparedKkm = preparedKkm,
            request = request,
            body = body,
            stepName = "Отправляем невалидное тело POST /kkm/${preparedKkm.kkmId}/users: $caseName",
            expectedStatus = 400,
            scenario = caseName,
        )
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidUserRoles")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("Метод POST /kkm/{kkmId}/users возвращает 400 для невалидной роли")
    fun shouldReturnBadRequestForInvalidRole(caseName: String, invalidRole: Any?) {
        val preparedKkm = kkmAuth.prepareFirstKkmAdminPin()
        val request = newUserRequest()
        val body = request.asBody().toMutableMap().apply { this["role"] = invalidRole }

        postUserExpectingRejection(
            preparedKkm = preparedKkm,
            request = request,
            body = body,
            stepName = "Отправляем невалидное тело POST /kkm/${preparedKkm.kkmId}/users: $caseName",
            expectedStatus = 400,
            scenario = caseName,
        )
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidUserPins")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("Метод POST /kkm/{kkmId}/users возвращает 400 для невалидного PIN пользователя")
    fun shouldReturnBadRequestForInvalidUserPin(caseName: String, invalidPin: Any?) {
        val preparedKkm = kkmAuth.prepareFirstKkmAdminPin()
        val request = newUserRequest()
        val body = request.asBody().toMutableMap().apply { this["userPin"] = invalidPin }

        postUserExpectingRejection(
            preparedKkm = preparedKkm,
            request = request,
            body = body,
            stepName = "Отправляем невалидное тело POST /kkm/${preparedKkm.kkmId}/users: $caseName",
            expectedStatus = 400,
            scenario = caseName,
        )
    }

    @Test
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("Метод POST /kkm/{kkmId}/users возвращает 400 без тела запроса")
    fun shouldReturnBadRequestWithoutRequestBody() {
        val preparedKkm = kkmAuth.prepareFirstKkmAdminPin()

        reportStep("Отправляем POST /kkm/${preparedKkm.kkmId}/users без тела запроса") {
            val response = superkassa.request(preparedKkm.adminPin)
                .`when`()
                .post(usersPath(preparedKkm.kkmId))
                .then()
                .extract()
                .response()

            scheduleUnexpectedCreatedUserCleanup(preparedKkm, response)

            response.then()
                .shouldHaveStatus(400, "обязательное тело запроса отсутствует")
                .contentType(ContentType.JSON)
        }
    }

    @Nested
    @ApiRegression
    @Feature("API")
    @Story("POST /kkm/{kkmId}/users")
    @Owner("Pavel Michka")
    @DisplayName("Проверки авторизации POST /kkm/{kkmId}/users")
    @ResourceLock(value = "kkm-users", mode = ResourceAccessMode.READ_WRITE)
    inner class AuthorizationRegressionTests {
        @Test
        @Severity(SeverityLevel.NORMAL)
        @DisplayName("Метод POST /kkm/{kkmId}/users возвращает 401 без Authorization")
        fun shouldReturnUnauthorizedWithoutAuthorization() {
            val preparedKkm = kkmAuth.prepareFirstKkmAdminPin()
            val request = newUserRequest()

            reportStep("Проверяем POST /kkm/${preparedKkm.kkmId}/users без Authorization") {
                val response = superkassa.requestWithoutAuthorization()
                    .body(request.asBody())
                    .`when`()
                    .post(usersPath(preparedKkm.kkmId))
                    .then()
                    .extract()
                    .response()

                assertRejectedCreation(preparedKkm, request, response, 401, "запрос без Authorization")
            }
        }

        @Test
        @Severity(SeverityLevel.NORMAL)
        @DisplayName("Метод POST /kkm/{kkmId}/users возвращает 403 для неверного PIN")
        fun shouldReturnForbiddenForInvalidPin() {
            val preparedKkm = kkmAuth.prepareFirstKkmAdminPin()
            val request = newUserRequest()

            postUserExpectingRejection(
                preparedKkm = preparedKkm,
                request = request,
                body = request.asBody(),
                stepName = "Проверяем POST /kkm/${preparedKkm.kkmId}/users с неверным PIN",
                expectedStatus = 403,
                scenario = "запрос с неверным PIN",
                authPin = INVALID_PIN,
            )
        }
    }

    @Test
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("Метод POST /kkm/{kkmId}/users возвращает 404 для несуществующей ККМ")
    fun shouldReturnNotFoundForUnknownKkmId() {
        val preparedKkm = kkmAuth.prepareFirstKkmAdminPin()
        val request = newUserRequest()

        reportStep("Проверяем POST /kkm/$UNKNOWN_KKM_ID/users для несуществующей ККМ") {
            val response = superkassa.request(preparedKkm.adminPin)
                .body(request.asBody())
                .`when`()
                .post(usersPath(UNKNOWN_KKM_ID))
                .then()
                .extract()
                .response()

            response.then()
                .shouldHaveStatus(404, "несуществующая ККМ")
                .contentType(ContentType.JSON)
        }
    }

    private fun withCreatedUser(assertions: (Response) -> Unit) {
        val preparedKkm = kkmAuth.prepareFirstKkmAdminPin()
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
                .post(usersPath(preparedKkm.kkmId))
                .then()
                .shouldHaveStatus(200, "подготовка тестового пользователя ККМ")
                .contentType(ContentType.JSON)
                .extract()
                .response()
        }

    private fun postUserExpectingRejection(
        preparedKkm: PreparedKkmAuth,
        request: UserCreateRequest,
        body: Map<String, Any?>,
        stepName: String,
        expectedStatus: Int,
        scenario: String,
        authPin: String = preparedKkm.adminPin,
    ) {
        reportStep(stepName) {
            val response = superkassa.request(authPin)
                .body(body)
                .`when`()
                .post(usersPath(preparedKkm.kkmId))
                .then()
                .extract()
                .response()

            assertRejectedCreation(preparedKkm, request, response, expectedStatus, scenario)
        }
    }

    private fun assertRejectedCreation(
        preparedKkm: PreparedKkmAuth,
        request: UserCreateRequest,
        response: Response,
        expectedStatus: Int,
        scenario: String,
    ) {
        if (response.statusCode in 200..299) {
            deferredCleanupActions += {
                deleteCreatedUser(preparedKkm, request, response)
            }
        }

        response.then()
            .shouldHaveStatus(expectedStatus, scenario)
            .contentType(ContentType.JSON)
    }

    private fun scheduleUnexpectedCreatedUserCleanup(preparedKkm: PreparedKkmAuth, response: Response) {
        if (response.statusCode !in 200..299) return

        val userId = response.jsonPath().getString("userId")?.takeIf(String::isNotBlank) ?: return
        deferredCleanupActions += {
            deleteUser(preparedKkm, userId)
        }
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

        deleteUser(preparedKkm, userId)
    }

    private fun deleteUser(preparedKkm: PreparedKkmAuth, userId: String) {
        reportStep("Удаляем тестового пользователя userId='$userId' после regression-проверки") {
            superkassa.request(preparedKkm.adminPin)
                .`when`()
                .delete("/kkm/{kkmId}/users/{userId}", preparedKkm.kkmId, userId)
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
        name = "Regression кассир ${UUID.randomUUID().toString().take(8)}",
        role = CASHIER_ROLE,
        userPin = ThreadLocalRandom.current()
            .nextInt(MIN_TEST_PIN, MAX_TEST_PIN_EXCLUSIVE)
            .toString(),
    )

    private fun usersPath(kkmId: String): String = "/kkm/$kkmId/users"

    private fun assertFieldType(
        softly: SoftAssertions,
        item: Map<String, Any?>,
        fieldName: String,
        expectedType: Class<*>,
        schemaName: String,
    ) {
        softly.assertThat(item)
            .withFailMessage(ApiContractErrorMessages.requiredFieldWithTypeMissing(ENDPOINT, fieldName, expectedType.simpleName, schemaName))
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
        const val ENDPOINT = "POST /kkm/{kkmId}/users"
        const val CASHIER_ROLE = "CASHIER"
        const val UNSUPPORTED_ROLE = "UNKNOWN"
        const val INVALID_PIN = "999999"
        const val UNKNOWN_KKM_ID = "00000000-0000-0000-0000-000000000000"
        const val MIN_TEST_PIN = 1_000
        const val MAX_TEST_PIN_EXCLUSIVE = 10_000

        val USER_RESPONSE_FIELDS = setOf(
            "name",
            "pin",
            "role",
            "userId",
        )

        @JvmStatic
        fun invalidFieldTypes(): Stream<Arguments> = Stream.of(
            Arguments.of("поле name имеет тип Number вместо String", "name", 123),
            Arguments.of("поле role имеет тип Boolean вместо String", "role", true),
            Arguments.of("поле userPin имеет тип Number вместо String", "userPin", 1234),
        )

        @JvmStatic
        fun invalidUserNames(): Stream<Arguments> = Stream.of(
            Arguments.of("поле name содержит пустую строку", ""),
            Arguments.of("поле name состоит только из пробелов", "    "),
            Arguments.of("поле name содержит null вместо String", null as String?),
        )

        @JvmStatic
        fun invalidUserRoles(): Stream<Arguments> = Stream.of(
            Arguments.of("поле role содержит неподдерживаемое значение", UNSUPPORTED_ROLE),
            Arguments.of("поле role содержит пустую строку", ""),
            Arguments.of("поле role состоит только из пробелов", "    "),
            Arguments.of("поле role содержит null вместо String", null as String?),
        )

        @JvmStatic
        fun invalidUserPins(): Stream<Arguments> = Stream.of(
            Arguments.of("userPin содержит пустую строку", ""),
            Arguments.of("userPin состоит только из пробелов", "    "),
            Arguments.of("userPin содержит null вместо String", null as String?),
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
