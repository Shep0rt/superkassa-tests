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
    fun prepareFirstKkmAdminPin(): PreparedKkmAuth {
        preparedAuth?.let { cachedAuth ->
            Allure.step("Используем уже подготовленный ADMIN PIN для ККМ kkmId='${cachedAuth.kkmId}'")
            return cachedAuth
        }

        val kkmId = firstKkmIdOrSkip()

        prepareAdminPin(kkmId)

        val auth = PreparedKkmAuth(
            kkmId = kkmId,
            adminPin = TEST_ADMIN_PIN,
        )

        preparedAuth = auth

        return auth
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
        val users = getUsers(kkmId, config.authPin)

        val admin = users.firstOrNull { it["role"] == ADMIN_ROLE }

        assumeTrue(admin != null, "В ККМ '$kkmId' нет пользователя с role=ADMIN для подготовки PIN")

        val adminUserId = admin!!["userId"] as? String
        val adminName = admin["name"] as? String

        assumeTrue(!adminUserId.isNullOrBlank(), "У пользователя role=ADMIN отсутствует userId")
        assumeTrue(!adminName.isNullOrBlank(), "У пользователя role=ADMIN отсутствует name")

        updateAdminPin(kkmId, adminUserId!!, adminName!!, config.authPin)
    }

    private fun getUsers(kkmId: String, authPin: String): List<Map<String, Any?>> {
        val getUsers: Allure.ThrowableRunnable<Response> = Allure.ThrowableRunnable {
            superkassa.request(authPin)
                .`when`()
                .get("/kkm/{kkmId}/users", kkmId)
                .then()
                .extract()
                .response()
        }

        val response: Response = Allure.step("Получаем пользователей ККМ kkmId='$kkmId' через PIN, переданный в запуске тестов", getUsers)

        assumeTrue(
            response.statusCode == 200,
            "Не удалось получить пользователей ККМ через PIN, переданный в запуске тестов. HTTP ${response.statusCode}",
        )

        return response.jsonPath().getList("")
    }

    private fun updateAdminPin(kkmId: String, userId: String, name: String, currentAuthPin: String) {
        val body = mapOf(
            "name" to name,
            "role" to ADMIN_ROLE,
            "userPin" to TEST_ADMIN_PIN,
        )

        val updatePin: Allure.ThrowableRunnable<Unit> = Allure.ThrowableRunnable {
            superkassa.request(currentAuthPin)
                .body(body)
                .`when`()
                .put("/kkm/{kkmId}/users/{userId}", kkmId, userId)
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
        }

        Allure.step("Обновляем PIN пользователя role=ADMIN userId='$userId' на '$TEST_ADMIN_PIN'", updatePin)
    }

    private companion object {
        const val ADMIN_ROLE = "ADMIN"
        const val TEST_ADMIN_PIN = "0808"
        var preparedAuth: PreparedKkmAuth? = null
    }
}
