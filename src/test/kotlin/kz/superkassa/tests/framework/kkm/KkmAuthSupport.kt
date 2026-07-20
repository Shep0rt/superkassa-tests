package kz.superkassa.tests.framework.kkm

import io.qameta.allure.Allure
import io.restassured.http.ContentType
import io.restassured.response.Response
import kz.superkassa.tests.framework.http.SuperkassaApiClient
import org.junit.jupiter.api.Assumptions.assumeTrue

class KkmAuthSupport(
    private val superkassa: SuperkassaApiClient,
) {
    fun prepareFirstKkmAdminPin(): PreparedKkmAuth {
        preparedAuth?.let { cachedAuth ->
            Allure.step("Используем уже подготовленный ADMIN PIN для ККМ kkmId='${cachedAuth.kkmId}'")
            return cachedAuth
        }

        val kkmId = firstKkmIdOrSkip()

        val auth = PreparedKkmAuth(
            kkmId = kkmId,
            adminPin = TEST_ADMIN_PIN,
        )

        preparedAuth = auth

        Allure.step("Используем ранее подготовленный ADMIN PIN для ККМ kkmId='$kkmId'")

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

    private companion object {
        const val TEST_ADMIN_PIN = "0808"
        var preparedAuth: PreparedKkmAuth? = null
    }
}
