package kz.superkassa.tests.api.units.getbycode

import io.qameta.allure.Feature
import io.qameta.allure.Owner
import io.qameta.allure.Severity
import io.qameta.allure.SeverityLevel
import io.qameta.allure.Story
import io.restassured.http.ContentType
import io.restassured.response.Response
import kz.superkassa.tests.framework.BaseApiTest
import kz.superkassa.tests.framework.assertions.ApiContractErrorMessages
import kz.superkassa.tests.framework.reporting.reportStep
import kz.superkassa.tests.framework.tags.ApiSmoke
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.SoftAssertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@ApiSmoke
@Feature("API")
@Story("GET /units-of-measurement/{code}")
@Owner("Pavel Michka")
@DisplayName("GET /units-of-measurement/{code}: smoke-проверки единицы измерения по коду")
class UnitOfMeasurementByCodeSmokeTest : BaseApiTest() {
    @Test
    @Severity(SeverityLevel.BLOCKER)
    @DisplayName("Метод GET /units-of-measurement/{code} возвращает HTTP 200 и JSON-объект")
    fun shouldReturnUnitOfMeasurementSuccessfully() {
        val responseBody = getUnitResponse().jsonPath().get<Any?>("")

        assertThat(responseBody)
            .withFailMessage(
                ApiContractErrorMessages.responseBodyStructureMismatch(
                    ENDPOINT,
                    "JSON-объектом",
                    RESPONSE_SCHEMA,
                ),
            )
            .isInstanceOf(Map::class.java)
    }

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Метод GET /units-of-measurement/{code} возвращает все обязательные поля единицы измерения")
    fun shouldReturnRequiredUnitFields() {
        val response = getUnitResponseMap()

        SoftAssertions().apply {
            REQUIRED_FIELDS.forEach { fieldName ->
                assertThat(response)
                    .withFailMessage(
                        ApiContractErrorMessages.requiredFieldMissing(
                            ENDPOINT,
                            fieldName,
                            RESPONSE_SCHEMA,
                        ),
                    )
                    .containsKey(fieldName)
            }
        }.assertAll()
    }

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Метод GET /units-of-measurement/{code} возвращает заполненные обязательные поля единицы измерения")
    fun shouldReturnFilledRequiredUnitFields() {
        val response = getUnitResponseMap()

        SoftAssertions().apply {
            REQUIRED_FIELDS.forEach { fieldName ->
                val message = ApiContractErrorMessages.requiredFieldEmpty(
                    ENDPOINT,
                    fieldName,
                    RESPONSE_SCHEMA,
                )

                assertThat(response[fieldName])
                    .withFailMessage(message)
                    .isNotNull()

                (response[fieldName] as? String)?.let { value ->
                    assertThat(value)
                        .withFailMessage(message)
                        .isNotBlank()
                }
            }
        }.assertAll()
    }

    private fun getUnitResponseMap(): Map<String, Any?> =
        getUnitResponse().jsonPath().getMap("")

    private fun getUnitResponse(): Response =
        reportStep("Получаем единицу измерения с code='$TEST_CODE' через GET ${unitPath()}") {
            superkassa.requestWithoutAuthorization()
                .`when`()
                .get(unitPath())
                .then()
                .shouldHaveStatus(200, "получение единицы измерения по коду '$TEST_CODE'")
                .contentType(ContentType.JSON)
                .extract()
                .response()
        }

    private fun unitPath(): String = "$BASE_PATH/$TEST_CODE"

    private companion object {
        const val ENDPOINT = "GET /units-of-measurement/{code}"
        const val BASE_PATH = "/units-of-measurement"
        const val RESPONSE_SCHEMA = "UnitOfMeasurementResponse"
        const val TEST_CODE = "796"

        val REQUIRED_FIELDS = setOf(
            "code",
            "nameFull",
            "nameShort",
        )
    }
}
