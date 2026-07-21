package kz.superkassa.tests.framework.kkm

import io.qameta.allure.Allure
import io.restassured.http.ContentType
import io.restassured.response.Response
import kz.superkassa.tests.framework.config.TestConfig
import kz.superkassa.tests.framework.http.SuperkassaApiClient
import org.junit.jupiter.api.Assumptions.assumeTrue

class KkmAuthSupport(
    private val superkassa: SuperkassaApiClient,
    private val config: TestConfig,
) {
    fun prepareFirstKkmAdminPin(): PreparedKkmAuth = synchronized(PREPARATION_LOCK) {
        preparedAuth?.let { cachedAuth ->
            Allure.step("Используем уже подготовленный ADMIN PIN для ККМ kkmId='${cachedAuth.kkmId}'")
            return@synchronized cachedAuth
        }

        val kkmId = firstKkmIdOrSkip()
        prepareAdminPin(kkmId)

        val auth = PreparedKkmAuth(
            kkmId = kkmId,
            adminPin = TEST_ADMIN_PIN,
        )

        preparedAuth = auth

        Allure.step("ADMIN PIN для ККМ kkmId='$kkmId' подготовлен и готов к использованию")

        auth
    }

    private fun firstKkmIdOrSkip(): String {
        val getKkmList: Allure.ThrowableRunnable<Response> = Allure.ThrowableRunnable {
            superkassa.request()
                .queryParam("limit", 1)
                .queryParam("offset", 0)
                .`when`()
                .get("/kkm")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .extract()
                .response()
        }

        val response: Response = Allure.step("Получаем контрольную ККМ из списка GET /kkm?limit=1&offset=0", getKkmList)

        val items = response.jsonPath().getList<Map<String, Any?>>("items")

        assumeTrue(items.isNotEmpty(), "В системе нет ККМ для проверки операций над конкретной ККМ")

        val kkmId = items.first()["kkmId"] as? String

        assumeTrue(!kkmId.isNullOrBlank(), "В первой ККМ нет kkmId для проверки операций над конкретной ККМ")

        return kkmId!!
    }

    private fun prepareAdminPin(kkmId: String) {
        val launchPinResponse = getUsers(
            kkmId = kkmId,
            authPin = config.authPin,
            stepName = "Получаем пользователей ККМ kkmId='$kkmId' через PIN, переданный в запуске тестов",
        )

        if (launchPinResponse.statusCode == 200) {
            val admin = requireAdmin(kkmId, launchPinResponse)

            if (config.authPin != TEST_ADMIN_PIN) {
                updateAdminPin(
                    kkmId = kkmId,
                    userId = admin.userId,
                    name = admin.name,
                    currentAuthPin = config.authPin,
                )
                verifyPreparedAdminPin(kkmId)
            }

            return
        }

        if (config.authPin != TEST_ADMIN_PIN) {
            val preparedPinResponse = getUsers(
                kkmId = kkmId,
                authPin = TEST_ADMIN_PIN,
                stepName = "Проверяем ранее подготовленный ADMIN PIN для ККМ kkmId='$kkmId'",
            )

            if (preparedPinResponse.statusCode == 200) {
                requireAdmin(kkmId, preparedPinResponse)
                Allure.step("Для ККМ kkmId='$kkmId' уже используется подготовленный ADMIN PIN")
                return
            }

            error(
                "Не удалось подготовить ADMIN PIN для ККМ '$kkmId': " +
                    "PIN из запуска вернул HTTP ${launchPinResponse.statusCode}, " +
                    "подготовленный PIN вернул HTTP ${preparedPinResponse.statusCode}.",
            )
        }

        error(
            "Не удалось получить пользователей ККМ '$kkmId' через подготовленный ADMIN PIN. " +
                "Сервер вернул HTTP ${launchPinResponse.statusCode}.",
        )
    }

    private fun getUsers(kkmId: String, authPin: String, stepName: String): Response {
        val getUsers: Allure.ThrowableRunnable<Response> = Allure.ThrowableRunnable {
            superkassa.request(authPin)
                .`when`()
                .get("/kkm/{kkmId}/users", kkmId)
                .then()
                .extract()
                .response()
        }

        return Allure.step(stepName, getUsers)
    }

    private fun requireAdmin(kkmId: String, response: Response): AdminUser {
        response.then().contentType(ContentType.JSON)

        val users = response.jsonPath().getList<Map<String, Any?>>("")
        val admin = users.firstOrNull { it["role"] == ADMIN_ROLE }

        assumeTrue(admin != null, "В ККМ '$kkmId' нет пользователя с role=ADMIN для подготовки PIN")

        val userId = admin!!["userId"] as? String
        val name = admin["name"] as? String

        assumeTrue(!userId.isNullOrBlank(), "У пользователя role=ADMIN отсутствует userId")
        assumeTrue(!name.isNullOrBlank(), "У пользователя role=ADMIN отсутствует name")

        return AdminUser(userId = userId!!, name = name!!)
    }

    private fun updateAdminPin(kkmId: String, userId: String, name: String, currentAuthPin: String) {
        val body = mapOf(
            "name" to name,
            "role" to ADMIN_ROLE,
            "userPin" to TEST_ADMIN_PIN,
        )

        val updatePin: Allure.ThrowableRunnable<Response> = Allure.ThrowableRunnable {
            superkassa.request(currentAuthPin)
                .body(body)
                .`when`()
                .put("/kkm/{kkmId}/users/{userId}", kkmId, userId)
                .then()
                .extract()
                .response()
        }

        val response = Allure.step(
            "Обновляем PIN пользователя role=ADMIN userId='$userId' на подготовленный тестовый PIN",
            updatePin,
        )

        check(response.statusCode == 200) {
            "Не удалось обновить PIN пользователя role=ADMIN userId='$userId' в ККМ '$kkmId'. " +
                "Ожидался HTTP 200, получен HTTP ${response.statusCode}: ${response.body.asString()}"
        }
        response.then().contentType(ContentType.JSON)
    }

    private fun verifyPreparedAdminPin(kkmId: String) {
        val response = getUsers(
            kkmId = kkmId,
            authPin = TEST_ADMIN_PIN,
            stepName = "Проверяем подготовленный ADMIN PIN для ККМ kkmId='$kkmId' после обновления",
        )

        check(response.statusCode == 200) {
            "PIN пользователя role=ADMIN был обновлен, но получить пользователей ККМ '$kkmId' " +
                "через подготовленный PIN не удалось. Сервер вернул HTTP ${response.statusCode}."
        }
        requireAdmin(kkmId, response)
    }

    private data class AdminUser(
        val userId: String,
        val name: String,
    )

    private companion object {
        const val ADMIN_ROLE = "ADMIN"
        const val TEST_ADMIN_PIN = "0808"
        val PREPARATION_LOCK = Any()

        @Volatile
        var preparedAuth: PreparedKkmAuth? = null
    }
}
