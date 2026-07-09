package kz.superkassa.tests.api.kkm

import io.qameta.allure.Feature
import io.qameta.allure.Owner
import io.qameta.allure.Severity
import io.qameta.allure.SeverityLevel
import io.qameta.allure.Story
import io.restassured.http.ContentType
import io.restassured.path.json.JsonPath
import io.restassured.response.Response
import kz.superkassa.tests.framework.BaseTest
import kz.superkassa.tests.framework.tags.ApiSmoke
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@ApiSmoke
@Feature("API")
@Story("GET /kkm")
@Owner("Pavel Michka")
@DisplayName("GET /kkm: smoke-проверки списка ККМ")
class KkmSmokeTest : BaseTest() {
    @Test
    @Severity(SeverityLevel.BLOCKER)
    @DisplayName("Метод GET /kkm возвращает HTTP 200 и JSON")
    fun shouldReturnKkmListSuccessfully() {
        superkassa.request()
            .`when`()
            .get("/kkm")
            .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
    }

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Метод GET /kkm возвращает обязательные поля пагинации")
    fun shouldReturnRequiredPaginationFields() {
        val json = getKkmJson()

        assertThat(value(json, "items")).isNotNull()
        assertThat(value(json, "total")).isNotNull()
        assertThat(value(json, "limit")).isNotNull()
        assertThat(value(json, "offset")).isNotNull()
        assertThat(value(json, "hasMore")).isNotNull()
    }

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Метод GET /kkm возвращает обязательные поля каждой ККМ")
    fun shouldReturnRequiredKkmItemFields() {
        val json = getKkmJson()
        val items = json.getList<Map<String, Any?>>("items")

        assertThat(items).isNotNull()

        items.forEachIndexed { index, item ->
            assertThat(item["autoCloseShift"])
                .describedAs("items[$index].autoCloseShift")
                .isNotNull()
            assertThat(item["createdAt"])
                .describedAs("items[$index].createdAt")
                .isNotNull()
            assertThat(item["kkmId"])
                .describedAs("items[$index].kkmId")
                .isNotNull()
            assertThat(item["mode"])
                .describedAs("items[$index].mode")
                .isNotNull()
            assertThat(item["state"])
                .describedAs("items[$index].state")
                .isNotNull()
            assertThat(item["updatedAt"])
                .describedAs("items[$index].updatedAt")
                .isNotNull()
        }
    }

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Метод GET /kkm возвращает непустые базовые поля каждой ККМ")
    fun shouldReturnNonEmptyKkmItemFields() {
        val json = getKkmJson()
        val items = json.getList<Map<String, Any?>>("items")

        assertThat(items).isNotNull()

        items.forEachIndexed { index, item ->
            assertThat(item["kkmId"] as? String)
                .describedAs("items[$index].kkmId")
                .isNotBlank()
            assertThat(item["mode"] as? String)
                .describedAs("items[$index].mode")
                .isNotBlank()
            assertThat(item["state"] as? String)
                .describedAs("items[$index].state")
                .isNotBlank()
        }
    }

    private fun getKkmJson(): JsonPath {
        val response: Response = superkassa.request()
            .`when`()
            .get("/kkm")
            .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .extract()
            .response()

        return response.jsonPath()
    }

    private fun value(json: JsonPath, path: String): Any? = json.get(path)
}
