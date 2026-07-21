package kz.superkassa.tests.api.users.get

import io.qameta.allure.Feature
import io.qameta.allure.Owner
import io.qameta.allure.Severity
import io.qameta.allure.SeverityLevel
import io.qameta.allure.Story
import io.restassured.http.ContentType
import io.restassured.http.Method
import io.restassured.path.json.JsonPath
import io.restassured.response.Response
import kz.superkassa.tests.framework.kkm.KkmAuthenticatedTest
import kz.superkassa.tests.framework.assertions.ApiContractErrorMessages
import kz.superkassa.tests.framework.contract.ApiEnumValues
import kz.superkassa.tests.framework.kkm.PreparedKkmAuth
import kz.superkassa.tests.framework.reporting.reportStep
import kz.superkassa.tests.framework.tags.ApiRegression
import org.assertj.core.api.SoftAssertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.ResourceAccessMode
import org.junit.jupiter.api.parallel.ResourceLock
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

@ApiRegression
@Feature("API")
@Story("GET /kkm/{kkmId}/users")
@Owner("Pavel Michka")
@DisplayName("GET /kkm/{kkmId}/users: регрессионные проверки списка пользователей ККМ")
@ResourceLock(value = "kkm-users", mode = ResourceAccessMode.READ)
@Suppress("SameParameterValue")
class KkmUsersRegressionTest : KkmAuthenticatedTest() {
    @Nested
    @ApiRegression
    @Feature("API")
    @Story("GET /kkm/{kkmId}/users")
    @Owner("Pavel Michka")
    @DisplayName("Позитивные проверки GET /kkm/{kkmId}/users")
    inner class PositiveRegressionTests {
        @Test
        @Severity(SeverityLevel.CRITICAL)
        @DisplayName("Метод GET /kkm/{kkmId}/users возвращает поля ожидаемых типов")
        fun shouldReturnExpectedFieldTypes() {
            val users = getUsersJson(preparedKkm).getList<Map<String, Any?>>("")

            SoftAssertions().apply {
                users.forEachIndexed { index, user ->
                    assertFieldType(this, user, "users[$index].name", "name", String::class.java, "UserResponse")
                    assertFieldType(this, user, "users[$index].role", "role", String::class.java, "UserResponse")
                    assertFieldType(this, user, "users[$index].userId", "userId", String::class.java, "UserResponse")
                    assertOptionalFieldType(this, user, "users[$index].pin", "pin", String::class.java, "UserResponse")
                }
            }.assertAll()
        }

        @Test
        @Severity(SeverityLevel.CRITICAL)
        @DisplayName("Метод GET /kkm/{kkmId}/users не возвращает поля вне Swagger-контракта")
        fun shouldNotReturnFieldsOutsideSwaggerContract() {
            val users = getUsersJson(preparedKkm).getList<Map<String, Any?>>("")

            SoftAssertions().apply {
                users.forEach { user ->
                    assertOnlySwaggerFields(this, user, "UserResponse", USER_RESPONSE_FIELDS)
                }
            }.assertAll()
        }

        @Test
        @Severity(SeverityLevel.CRITICAL)
        @DisplayName("Метод GET /kkm/{kkmId}/users возвращает допустимые роли пользователей")
        fun shouldReturnExpectedUserRoles() {
            val users = getUsersJson(preparedKkm).getList<Map<String, Any?>>("")

            SoftAssertions().apply {
                users.forEachIndexed { index, user ->
                    assertRequiredEnumValue(
                        this,
                        user,
                        "users[$index].role",
                        "role",
                        "UserResponse",
                        ApiEnumValues.USER_ROLES
                    )
                }
            }.assertAll()
        }

    }

    @Nested
    @ApiRegression
    @Feature("API")
    @Story("GET /kkm/{kkmId}/users")
    @Owner("Pavel Michka")
    @DisplayName("Негативные проверки GET /kkm/{kkmId}/users")
    inner class NegativeRegressionTests {
        @Nested
        @ApiRegression
        @Feature("API")
        @Story("GET /kkm/{kkmId}/users")
        @Owner("Pavel Michka")
        @DisplayName("Проверки авторизации GET /kkm/{kkmId}/users")
        @ResourceLock(value = "kkm-users", mode = ResourceAccessMode.READ)
        inner class AuthorizationRegressionTests {
            @Test
            @Severity(SeverityLevel.NORMAL)
            @DisplayName("Метод GET /kkm/{kkmId}/users возвращает 401 без Authorization")
            fun shouldReturnUnauthorizedWithoutAuthorization() {

                reportStep("Проверяем GET /kkm/${preparedKkm.kkmId}/users без Authorization") {
                    superkassa.requestWithoutAuthorization()
                        .`when`()
                        .get(usersPath(preparedKkm.kkmId))
                        .then()
                        .shouldHaveStatus(401, "запрос без Authorization")
                        .contentType(ContentType.JSON)
                }
            }

            @Test
            @Severity(SeverityLevel.NORMAL)
            @DisplayName("Метод GET /kkm/{kkmId}/users возвращает 403 для неверного PIN")
            fun shouldReturnForbiddenForInvalidPin() {

                reportStep("Проверяем GET /kkm/${preparedKkm.kkmId}/users с неверным PIN") {
                    superkassa.request(INVALID_PIN)
                        .`when`()
                        .get(usersPath(preparedKkm.kkmId))
                        .then()
                        .shouldHaveStatus(403, "запрос с неверным PIN")
                        .contentType(ContentType.JSON)
                }
            }
        }

        @Nested
        @ApiRegression
        @Feature("API")
        @Story("GET /kkm/{kkmId}/users")
        @Owner("Pavel Michka")
        @DisplayName("Проверки несуществующих идентификаторов")
        inner class MissingIdentifiersTests {
            @Test
            @Severity(SeverityLevel.NORMAL)
            @DisplayName("Метод GET /kkm/{kkmId}/users возвращает 404 для несуществующей ККМ")
            fun shouldReturnNotFoundForUnknownKkmId() {

                reportStep("Проверяем GET /kkm/$UNKNOWN_KKM_ID/users для несуществующей ККМ") {
                    superkassa.request(preparedKkm.adminPin)
                        .`when`()
                        .get(usersPath(UNKNOWN_KKM_ID))
                        .then()
                        .shouldHaveStatus(404, "несуществующая ККМ")
                        .contentType(ContentType.JSON)
                }
            }

        }

        @Nested
        @ApiRegression
        @Feature("API")
        @Story("GET /kkm/{kkmId}/users")
        @Owner("Pavel Michka")
        @DisplayName("Проверки неподдерживаемых HTTP-методов")
        inner class UnsupportedHttpMethodsTests {
            @ParameterizedTest(name = "HTTP {0} /kkm/'{'kkmId'}'/users возвращает 405")
            @EnumSource(value = Method::class, names = ["PUT", "PATCH", "DELETE"])
            @Severity(SeverityLevel.NORMAL)
            @DisplayName("Метод /kkm/{kkmId}/users возвращает 405 для HTTP-методов кроме GET и POST")
            fun shouldReturnMethodNotAllowedForUnsupportedMethods(method: Method) {

                reportStep("Проверяем, что HTTP $method /kkm/${preparedKkm.kkmId}/users не поддерживается") {
                    superkassa.request(preparedKkm.adminPin)
                        .`when`()
                        .request(method, usersPath(preparedKkm.kkmId))
                        .then()
                        .shouldHaveStatus(405, "неподдерживаемый HTTP-метод")
                }
            }

        }
    }

    private fun getUsersJson(preparedKkm: PreparedKkmAuth): JsonPath {
        val response: Response = reportStep("Получаем пользователей ККМ через GET /kkm/${preparedKkm.kkmId}/users") {
            superkassa.request(preparedKkm.adminPin)
                .`when`()
                .get(usersPath(preparedKkm.kkmId))
                .then()
                .shouldHaveStatus(200, "успешное получение списка пользователей ККМ")
                .contentType(ContentType.JSON)
                .extract()
                .response()
        }

        return response.jsonPath()
    }

    private fun usersPath(kkmId: String): String = "/kkm/$kkmId/users"

    private fun assertFieldType(
        softly: SoftAssertions,
        item: Map<String, Any?>,
        responseFieldName: String,
        sourceFieldName: String,
        expectedType: Class<*>,
        schemaName: String,
    ) {
        softly.assertThat(item)
            .withFailMessage(
                ApiContractErrorMessages.requiredFieldWithTypeMissing(
                    ENDPOINT,
                    responseFieldName,
                    expectedType.simpleName,
                    schemaName
                )
            )
            .containsKey(sourceFieldName)

        softly.assertThat(item[sourceFieldName])
            .withFailMessage(
                ApiContractErrorMessages.fieldTypeMismatch(
                    ENDPOINT,
                    responseFieldName,
                    expectedType.simpleName,
                    schemaName
                )
            )
            .isInstanceOf(expectedType)
    }

    private fun assertOptionalFieldType(
        softly: SoftAssertions,
        item: Map<String, Any?>,
        responseFieldName: String,
        sourceFieldName: String,
        expectedType: Class<*>,
        schemaName: String,
    ) {
        val fieldValue = item[sourceFieldName] ?: return

        softly.assertThat(fieldValue)
            .withFailMessage(
                ApiContractErrorMessages.optionalFieldTypeMismatch(
                    ENDPOINT,
                    responseFieldName,
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
        responseFieldName: String,
        sourceFieldName: String,
        schemaName: String,
        supportedValues: Set<String>,
    ) {
        val fieldValue = item[sourceFieldName] as? String

        softly.assertThat(fieldValue)
            .withFailMessage(ApiContractErrorMessages.requiredEnumMissing(ENDPOINT, responseFieldName, schemaName))
            .isNotBlank()

        softly.assertThat(fieldValue)
            .withFailMessage(
                ApiContractErrorMessages.enumUnsupported(
                    ENDPOINT,
                    responseFieldName,
                    fieldValue,
                    supportedValues
                )
            )
            .isIn(supportedValues)
    }

    private companion object {
        const val ENDPOINT = "GET /kkm/{kkmId}/users"
        const val INVALID_PIN = "999999"
        const val UNKNOWN_KKM_ID = "00000000-0000-0000-0000-000000000000"

        val USER_RESPONSE_FIELDS = setOf(
            "name",
            "pin",
            "role",
            "userId",
        )
    }
}
