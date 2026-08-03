package kz.superkassa.tests.api.units.get

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
@Story("GET /units-of-measurement")
@Owner("Pavel Michka")
@DisplayName("GET /units-of-measurement: smoke-проверки списка единиц измерения")
class UnitsOfMeasurementSmokeTest : BaseApiTest() {
    @Test
    @Severity(SeverityLevel.BLOCKER)
    @DisplayName("Метод GET /units-of-measurement возвращает HTTP 200 и JSON-объект")
    fun shouldReturnUnitsOfMeasurementSuccessfully() {
        val responseBody = getUnitsResponse().jsonPath().get<Any?>("")

        assertThat(responseBody)
            .withFailMessage(
                ApiContractErrorMessages.responseBodyStructureMismatch(
                    ENDPOINT,
                    "JSON-объектом",
                    PAGINATED_RESPONSE_SCHEMA,
                ),
            )
            .isInstanceOf(Map::class.java)
    }

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Метод GET /units-of-measurement возвращает обязательные поля пагинации")
    fun shouldReturnRequiredPaginationFields() {
        val response = getUnitsResponseMap()

        SoftAssertions().apply {
            PAGINATION_REQUIRED_FIELDS.forEach { fieldName ->
                assertRequiredFieldPresent(
                    this,
                    response,
                    fieldName,
                    fieldName,
                    PAGINATED_RESPONSE_SCHEMA,
                )
            }
        }.assertAll()
    }

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Метод GET /units-of-measurement возвращает заполненные обязательные поля пагинации")
    fun shouldReturnFilledRequiredPaginationFields() {
        val response = getUnitsResponseMap()

        SoftAssertions().apply {
            PAGINATION_REQUIRED_FIELDS.forEach { fieldName ->
                assertRequiredFieldFilled(
                    this,
                    response[fieldName],
                    fieldName,
                    PAGINATED_RESPONSE_SCHEMA,
                )
            }
        }.assertAll()
    }

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Метод GET /units-of-measurement возвращает обязательные поля каждой единицы измерения")
    fun shouldReturnRequiredUnitFields() {
        val units = getUnits()

        SoftAssertions().apply {
            units.forEachIndexed { index, unit ->
                UNIT_REQUIRED_FIELDS.forEach { fieldName ->
                    assertRequiredFieldPresent(
                        this,
                        unit,
                        fieldName,
                        "items[$index].$fieldName",
                        UNIT_RESPONSE_SCHEMA,
                    )
                }
            }
        }.assertAll()
    }

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Метод GET /units-of-measurement возвращает заполненные обязательные поля каждой единицы измерения")
    fun shouldReturnFilledRequiredUnitFields() {
        val units = getUnits()

        SoftAssertions().apply {
            units.forEachIndexed { index, unit ->
                UNIT_REQUIRED_FIELDS.forEach { fieldName ->
                    assertRequiredFieldFilled(
                        this,
                        unit[fieldName],
                        "items[$index].$fieldName",
                        UNIT_RESPONSE_SCHEMA,
                    )
                }
            }
        }.assertAll()
    }

    private fun getUnitsResponseMap(): Map<String, Any?> =
        getUnitsResponse().jsonPath().getMap("")

    private fun getUnits(): List<Map<String, Any?>> =
        getUnitsResponse().jsonPath().getList("items")

    private fun getUnitsResponse(): Response =
        reportStep("Получаем список единиц измерения через GET $PATH") {
            superkassa.requestWithoutAuthorization()
                .`when`()
                .get(PATH)
                .then()
                .shouldHaveStatus(200, "получение списка единиц измерения")
                .contentType(ContentType.JSON)
                .extract()
                .response()
        }

    private fun assertRequiredFieldPresent(
        softly: SoftAssertions,
        response: Map<String, Any?>,
        sourceFieldName: String,
        responseFieldName: String,
        schemaName: String,
    ) {
        softly.assertThat(response)
            .withFailMessage(
                ApiContractErrorMessages.requiredFieldMissing(
                    ENDPOINT,
                    responseFieldName,
                    schemaName,
                ),
            )
            .containsKey(sourceFieldName)
    }

    private fun assertRequiredFieldFilled(
        softly: SoftAssertions,
        value: Any?,
        fieldName: String,
        schemaName: String,
    ) {
        val message = ApiContractErrorMessages.requiredFieldEmpty(ENDPOINT, fieldName, schemaName)

        softly.assertThat(value)
            .withFailMessage(message)
            .isNotNull()

        if (value is String) {
            softly.assertThat(value)
                .withFailMessage(message)
                .isNotBlank()
        }
    }

    private companion object {
        const val ENDPOINT = "GET /units-of-measurement"
        const val PATH = "/units-of-measurement"
        const val PAGINATED_RESPONSE_SCHEMA = "PaginatedResponseUnitOfMeasurementResponse"
        const val UNIT_RESPONSE_SCHEMA = "UnitOfMeasurementResponse"

        val PAGINATION_REQUIRED_FIELDS = setOf(
            "items",
            "total",
            "limit",
            "offset",
            "hasMore",
        )

        val UNIT_REQUIRED_FIELDS = setOf(
            "code",
            "nameFull",
            "nameShort",
        )
    }
}
