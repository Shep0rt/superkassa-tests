package kz.superkassa.tests.framework.assertions

object ApiContractErrorMessages {
    fun requiredFieldMissing(endpoint: String, fieldName: String, schemaName: String): String =
        "Контракт API нарушен: в ответе $endpoint отсутствует обязательное поле '$fieldName'. " +
            "Поле '${fieldName.substringAfterLast(".")}' помечено как required в Swagger-схеме $schemaName."

    fun requiredFieldEmpty(endpoint: String, fieldName: String, schemaName: String): String =
        "Контракт API нарушен: в ответе $endpoint обязательное поле '$fieldName' отсутствует или не заполнено. " +
            "Поле '${fieldName.substringAfterLast(".")}' помечено как required в Swagger-схеме $schemaName."

    fun requiredFieldWithTypeMissing(endpoint: String, fieldName: String, expectedType: String, schemaName: String): String =
        "Контракт API нарушен: в ответе $endpoint отсутствует поле '$fieldName'. " +
            "Поле '$fieldName' должно присутствовать и иметь тип '$expectedType' согласно Swagger-схеме $schemaName."

    fun fieldTypeMismatch(endpoint: String, fieldName: String, expectedType: String, schemaName: String): String =
        "Контракт API нарушен: поле '$fieldName' в ответе $endpoint должно иметь тип '$expectedType' согласно Swagger-схеме $schemaName."

    fun optionalFieldTypeMismatch(endpoint: String, fieldName: String, expectedType: String, schemaName: String): String =
        "Контракт API нарушен: необязательное поле '$fieldName' в ответе $endpoint должно иметь тип '$expectedType' " +
            "согласно Swagger-схеме $schemaName, если оно передано не null."

    fun arrayItemTypeMismatch(endpoint: String, fieldName: String, index: Int, expectedType: String, schemaName: String): String =
        "Контракт API нарушен: элемент '$fieldName[$index]' в ответе $endpoint должен иметь тип '$expectedType' " +
            "согласно Swagger-схеме $schemaName."

    fun unexpectedSwaggerFields(endpoint: String, objectPath: String, unexpectedFields: Set<String>): String =
        "Контракт API нарушен: объект '$objectPath' в ответе $endpoint содержит поля, которых нет в Swagger-контракте: " +
            unexpectedFields.joinToString()

    fun requiredEnumMissing(endpoint: String, fieldName: String, schemaName: String): String =
        "Контракт API нарушен: обязательное поле '$fieldName' в ответе $endpoint отсутствует, пустое или имеет неверный тип. " +
            "Поле '$fieldName' помечено как required в Swagger-схеме $schemaName."

    fun enumUnsupported(endpoint: String, fieldName: String, actualValue: String?, supportedValues: Set<String>): String =
        "Контракт API нарушен: поле '$fieldName' в ответе $endpoint содержит неподдерживаемое значение '$actualValue'. " +
            "Допустимые значения: ${supportedValues.joinToString()}."
}
