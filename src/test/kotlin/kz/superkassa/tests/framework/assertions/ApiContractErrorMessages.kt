package kz.superkassa.tests.framework.assertions

object ApiContractErrorMessages {
    fun responseBodyStructureMismatch(endpoint: String, expectedStructure: String, schemaName: String): String =
        "Контракт API нарушен: ответ $endpoint должен быть $expectedStructure согласно Swagger-схеме $schemaName."

    fun requiredFieldMissing(endpoint: String, fieldName: String, schemaName: String): String =
        "Контракт API нарушен: в ответе $endpoint отсутствует обязательное поле '$fieldName'. " +
            "Поле '${fieldName.substringAfterLast(".")}' помечено как required в Swagger-схеме $schemaName."

    fun requiredFieldEmpty(endpoint: String, fieldName: String, schemaName: String): String =
        "Контракт API нарушен: в ответе $endpoint обязательное поле '$fieldName' отсутствует или не заполнено. " +
            "Поле '${fieldName.substringAfterLast(".")}' помечено как required в Swagger-схеме $schemaName."

    fun documentedFieldMissing(endpoint: String, fieldName: String, expectedStructure: String): String =
        "Контракт API нарушен: в ответе $endpoint отсутствует поле '$fieldName'. " +
            "Поле '$fieldName' должно присутствовать согласно Swagger-описанию метода $endpoint " +
            "и ожидаемой структуре $expectedStructure."

    fun documentedFieldEmpty(endpoint: String, fieldName: String, expectedStructure: String): String =
        "Контракт API нарушен: в ответе $endpoint поле '$fieldName' отсутствует или не заполнено. " +
            "Поле '$fieldName' должно быть заполнено согласно Swagger-описанию метода $endpoint " +
            "и ожидаемой структуре $expectedStructure."

    fun documentedFieldTypeMismatch(
        endpoint: String,
        fieldName: String,
        expectedType: String,
        actualValue: Any?,
        expectedStructure: String,
    ): String {
        val actualType = actualValue?.javaClass?.simpleName ?: "null"
        return "Контракт API нарушен: поле '$fieldName' в ответе $endpoint должно иметь тип '$expectedType', " +
            "но пришел тип '$actualType' со значением '$actualValue'. Требование зафиксировано в Swagger-описании " +
            "метода $endpoint и ожидаемой структуре $expectedStructure."
    }

    fun documentedFieldValueMismatch(
        endpoint: String,
        fieldName: String,
        expectedValue: Any?,
        actualValue: Any?,
        expectedStructure: String,
    ): String =
        "Контракт API нарушен: поле '$fieldName' в ответе $endpoint должно содержать значение '$expectedValue', " +
            "а содержит '$actualValue' согласно Swagger-описанию метода $endpoint " +
            "и ожидаемой структуре $expectedStructure."

    fun unexpectedDocumentedFields(
        endpoint: String,
        expectedStructure: String,
        unexpectedFields: Set<String>,
    ): String =
        "Контракт API нарушен: объект '$expectedStructure' в ответе $endpoint содержит поля, " +
            "которых нет в Swagger-описании метода и ожидаемой структуре: ${unexpectedFields.joinToString()}."

    fun requiredFieldWithTypeMissing(endpoint: String, fieldName: String, expectedType: String, schemaName: String): String =
        "Контракт API нарушен: в ответе $endpoint отсутствует поле '$fieldName'. " +
            "Поле '$fieldName' должно присутствовать и иметь тип '$expectedType' согласно Swagger-схеме $schemaName."

    fun fieldTypeMismatch(endpoint: String, fieldName: String, expectedType: String, schemaName: String): String =
        "Контракт API нарушен: поле '$fieldName' в ответе $endpoint должно иметь тип '$expectedType' согласно Swagger-схеме $schemaName."

    fun fieldTypeMismatch(
        endpoint: String,
        fieldName: String,
        expectedType: String,
        actualValue: Any?,
        schemaName: String,
    ): String {
        val actualType = actualValue?.javaClass?.simpleName ?: "null"
        return "Контракт API нарушен: поле '$fieldName' в ответе $endpoint должно иметь тип '$expectedType', " +
            "но пришел тип '$actualType' со значением '$actualValue' согласно Swagger-схеме $schemaName."
    }

    fun fieldValueMismatch(
        endpoint: String,
        fieldName: String,
        expectedValue: Any?,
        actualValue: Any?,
        schemaName: String,
        scenario: String? = null,
    ): String {
        val scenarioSuffix = scenario?.let { " Сценарий: '$it'." }.orEmpty()
        return "Контракт API нарушен: поле '$fieldName' в ответе $endpoint должно содержать значение " +
            "'$expectedValue', но содержит '$actualValue' согласно Swagger-схеме $schemaName.$scenarioSuffix"
    }

    fun optionalFieldTypeMismatch(endpoint: String, fieldName: String, expectedType: String, schemaName: String): String =
        "Контракт API нарушен: необязательное поле '$fieldName' в ответе $endpoint должно иметь тип '$expectedType' " +
            "согласно Swagger-схеме $schemaName, если оно передано не null."

    fun arrayItemTypeMismatch(endpoint: String, fieldName: String, index: Int, expectedType: String, schemaName: String): String =
        "Контракт API нарушен: элемент '$fieldName[$index]' в ответе $endpoint должен иметь тип '$expectedType' " +
            "согласно Swagger-схеме $schemaName."

    fun requiredArrayItemEmpty(endpoint: String, fieldName: String, schemaName: String): String =
        "Контракт API нарушен: обязательное поле '$fieldName' в ответе $endpoint содержит пустой элемент " +
            "согласно Swagger-схеме $schemaName."

    fun unexpectedSwaggerFields(endpoint: String, schemaName: String, unexpectedFields: Set<String>): String {
        return "Контракт API нарушен: объект '$schemaName' в ответе $endpoint содержит поля, которых нет в Swagger-контракте: " +
            unexpectedFields.joinToString()
    }

    fun requiredEnumMissing(endpoint: String, fieldName: String, schemaName: String): String =
        "Контракт API нарушен: обязательное поле '$fieldName' в ответе $endpoint отсутствует, пустое или имеет неверный тип. " +
            "Поле '$fieldName' помечено как required в Swagger-схеме $schemaName."

    fun enumUnsupported(endpoint: String, fieldName: String, actualValue: String?, supportedValues: Set<String>): String =
        "Контракт API нарушен: поле '$fieldName' в ответе $endpoint содержит неподдерживаемое значение '$actualValue'. " +
            "Допустимые значения: ${supportedValues.joinToString()}."

    fun valueFormatMismatch(
        endpoint: String,
        fieldName: String,
        actualValue: Any?,
        expectedFormat: String,
        schemaName: String,
    ): String =
        "Контракт API нарушен: поле '$fieldName' в ответе $endpoint содержит значение '$actualValue', " +
            "которое не соответствует формату '$expectedFormat' согласно Swagger-схеме $schemaName."

    fun valuePrefixUnsupported(
        endpoint: String,
        fieldName: String,
        actualValue: String?,
        supportedPrefixes: Set<String>,
    ): String =
        "Контракт API нарушен: поле '$fieldName' в ответе $endpoint содержит неподдерживаемое значение '$actualValue'. " +
            "Допустимые префиксы: ${supportedPrefixes.joinToString()}."

    fun filterResultMismatch(
        endpoint: String,
        filterName: String,
        filterValue: Any?,
        actualValue: Any?,
        expectedResult: String,
    ): String =
        "Контракт API нарушен: фильтр $filterName=$filterValue в $endpoint вернул значение '$actualValue', " +
            "которое не соответствует условию: $expectedResult."

    fun normalizedParameterMismatch(
        endpoint: String,
        parameterName: String,
        rawValue: Any?,
        normalizedValue: Any?,
        responseFieldName: String,
        actualValue: Any?,
        scenario: String,
    ): String =
        "Контракт API нарушен: для параметра '$parameterName' со значением '$rawValue' в $endpoint после нормализации " +
            "ожидалось поле '$responseFieldName' со значением '$normalizedValue', но пришло '$actualValue'. " +
            "Сценарий: '$scenario'."
}
