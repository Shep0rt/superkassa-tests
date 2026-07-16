package kz.superkassa.tests.framework.http

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.ObjectWriter
import io.qameta.allure.Allure
import io.restassured.filter.Filter
import io.restassured.filter.FilterContext
import io.restassured.http.Header
import io.restassured.http.Headers
import io.restassured.response.Response
import io.restassured.specification.FilterableRequestSpecification
import io.restassured.specification.FilterableResponseSpecification
import java.util.Locale

class AllureApiLoggingFilter : Filter {
    override fun filter(
        requestSpec: FilterableRequestSpecification,
        responseSpec: FilterableResponseSpecification,
        context: FilterContext,
    ): Response {
        val httpCall: Allure.ThrowableRunnable<Response> = Allure.ThrowableRunnable {
            attachRequest(requestSpec)

            val response = context.next(requestSpec, responseSpec)

            attachResponse(response)
            response
        }

        return Allure.step(httpStepName(requestSpec), httpCall)
    }

    private fun attachRequest(request: FilterableRequestSpecification) {
        val body = request.getBody<Any?>()

        Allure.addAttachment(
            "HTTP request",
            "text/plain",
            """
            ${request.method} ${request.uri}

            Headers:
            ${formatHeaders(request.headers)}

            Query parameters:
            ${formatMap(request.queryParams)}

            Body:
            ${body?.toString()?.let { prettyBody(it, request.contentType) } ?: "<empty>"}
            """.trimIndent(),
        )

        Allure.addAttachment("cURL", "text/plain", toCurl(request))
    }

    private fun attachResponse(response: Response) {
        val contentType = response.contentType
        val body = response.asString()

        Allure.addAttachment(
            "HTTP response",
            "text/plain",
            """
            HTTP ${response.statusCode}
            Time: ${response.time} ms
            Content-Type: ${emptyToPlaceholder(contentType)}

            Headers:
            ${formatHeaders(response.headers)}
            """.trimIndent(),
        )

        if (isJson(contentType)) {
            Allure.addAttachment("HTTP response body", "application/json", prettyBody(body, contentType), "json")
        } else {
            Allure.addAttachment("HTTP response body", "text/plain", emptyToPlaceholder(body), "txt")
        }
    }

    private fun toCurl(request: FilterableRequestSpecification): String {
        val curl = StringBuilder("curl -i")
            .append(" -X ")
            .append(shellQuote(request.method))

        request.headers.forEach { header ->
            curl.append(" -H ").append(shellQuote("${header.name}: ${maskHeader(header)}"))
        }

        request.getBody<Any?>()?.let {
            curl.append(" --data-raw ").append(shellQuote(it.toString()))
        }

        return curl.append(" ").append(shellQuote(request.uri)).toString()
    }

    private fun httpStepName(request: FilterableRequestSpecification): String =
        "HTTP ${request.method} ${request.uri}"

    private fun formatHeaders(headers: Headers?): String {
        if (headers == null || !headers.exist()) {
            return "<empty>"
        }

        return headers.joinToString(System.lineSeparator()) { header ->
            "${header.name}: ${maskHeader(header)}"
        }
    }

    private fun formatMap(values: Map<String, *>?): String {
        if (values.isNullOrEmpty()) {
            return "<empty>"
        }

        return values.entries.joinToString(System.lineSeparator()) { (key, value) ->
            "$key: $value"
        }
    }

    private fun maskHeader(header: Header): String =
        if (header.name.lowercase(Locale.ROOT) in SENSITIVE_HEADERS) {
            "****"
        } else {
            header.value
        }

    private fun prettyBody(body: String?, contentType: String?): String {
        if (body.isNullOrBlank()) {
            return "<empty>"
        }
        if (!isJson(contentType)) {
            return body
        }

        return runCatching {
            PRETTY_JSON.writeValueAsString(OBJECT_MAPPER.readTree(body))
        }.getOrDefault(body)
    }

    private fun isJson(contentType: String?): Boolean =
        contentType?.lowercase(Locale.ROOT)?.contains("json") == true

    private fun emptyToPlaceholder(value: String?): String =
        if (value.isNullOrBlank()) "<empty>" else value

    private fun shellQuote(value: String): String = "'${value.replace("'", "'\\''")}'"

    private companion object {
        val SENSITIVE_HEADERS = setOf("authorization", "proxy-authorization", "cookie", "set-cookie")
        val OBJECT_MAPPER = ObjectMapper()
        val PRETTY_JSON: ObjectWriter = OBJECT_MAPPER.writerWithDefaultPrettyPrinter()
    }
}
