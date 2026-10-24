package com.karolina.cieslik.kafe.core.data.network

import com.karolina.cieslik.kafe.core.domain.DataError
import com.karolina.cieslik.kafe.core.domain.Result
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandler
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.get
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals

/** Konfiguracja klienta HTTP – base URL, JSON, timeouty, brak wyjątków dla 4xx/5xx. */
class HttpClientFactoryTest {

    @Serializable
    private enum class Size { SMALL, LARGE, UNKNOWN }

    @Serializable
    private data class CoffeeDto(val id: Int, val name: String, val size: Size = Size.UNKNOWN)

    private val config = NetworkConfig(baseUrl = "https://api.kafe.test/v1/")
    private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")

    private fun client(config: NetworkConfig = this.config, handler: MockRequestHandler) =
        HttpClientFactory.create(MockEngine(handler), config)

    @Test
    fun relativePathIsResolvedAgainstBaseUrl() = runTest {
        var request: HttpRequestData? = null
        val client = client { requestData ->
            request = requestData
            respond("", HttpStatusCode.OK)
        }

        client.get("coffees")

        assertEquals("https://api.kafe.test/v1/coffees", request?.url.toString())
    }

    @Test
    fun unknownFieldsAndEnumValuesDoNotBreakParsing() = runTest {
        val body = """{"id":1,"name":"Latte","size":"XXL","roastery":"Kafe Lab"}"""
        val client = client { respond(body, HttpStatusCode.OK, jsonHeaders) }

        val result = safeCall<CoffeeDto> { client.get("coffees/1") }

        assertEquals(Result.Success(CoffeeDto(1, "Latte", Size.UNKNOWN)), result)
    }

    @Test
    fun serverErrorIsReturnedAsFailureInsteadOfException() = runTest {
        val client = client { respond("", HttpStatusCode.InternalServerError) }

        val result = safeCall<CoffeeDto> { client.get("coffees/1") }

        assertEquals(Result.Failure(DataError.Remote.SERVER), result)
    }

    @Test
    fun slowResponseReturnsRequestTimeout() = runTest {
        val client = client(config.copy(requestTimeoutMillis = 100)) {
            delay(5_000)
            respond("", HttpStatusCode.OK)
        }

        val result = safeCall<CoffeeDto> { client.get("coffees/1") }

        assertEquals(Result.Failure(DataError.Remote.REQUEST_TIMEOUT), result)
    }
}
