package com.karolina.cieslik.kafe.core.data.network

import com.karolina.cieslik.kafe.core.domain.DataError
import com.karolina.cieslik.kafe.core.domain.Result
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandler
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import io.ktor.util.network.UnresolvedAddressException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

// KAFE-2: Testy mapowania odpowiedzi i wyjątków sieciowych na Result / DataError.Remote.
class SafeCallTest {

    @Serializable
    private data class CoffeeDto(val id: Int, val name: String)

    private fun client(handler: MockRequestHandler) = HttpClient(MockEngine(handler)) {
        install(ContentNegotiation) { json() }
    }

    private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")

    @Test
    fun status200WithValidBodyReturnsSuccess() = runTest {
        val client = client { respond("""{"id":1,"name":"Flat white"}""", HttpStatusCode.OK, jsonHeaders) }

        val result = safeCall<CoffeeDto> { client.get("https://kafe.test/coffee/1") }

        assertEquals(Result.Success(CoffeeDto(1, "Flat white")), result)
    }

    @Test
    fun status200WithMalformedBodyReturnsSerialization() = runTest {
        val client = client { respond("""{"id":"not-a-number"}""", HttpStatusCode.OK, jsonHeaders) }

        val result = safeCall<CoffeeDto> { client.get("https://kafe.test/coffee/1") }

        assertEquals(Result.Failure(DataError.Remote.SERIALIZATION), result)
    }

    @Test
    fun httpStatusCodesAreMappedToRemoteErrors() = runTest {
        val expected = mapOf(
            HttpStatusCode.Unauthorized to DataError.Remote.UNAUTHORIZED,
            HttpStatusCode.NotFound to DataError.Remote.NOT_FOUND,
            HttpStatusCode.RequestTimeout to DataError.Remote.REQUEST_TIMEOUT,
            HttpStatusCode.TooManyRequests to DataError.Remote.TOO_MANY_REQUESTS,
            HttpStatusCode.BadRequest to DataError.Remote.CLIENT,
            HttpStatusCode.InternalServerError to DataError.Remote.SERVER,
            HttpStatusCode.ServiceUnavailable to DataError.Remote.SERVER,
        )

        expected.forEach { (status, error) ->
            val client = client { respond("", status) }

            val result = safeCall<CoffeeDto> { client.get("https://kafe.test/coffee") }

            assertEquals(Result.Failure(error), result, "status $status")
        }
    }

    @Test
    fun requestTimeoutReturnsRequestTimeout() = runTest {
        val result = safeCall<CoffeeDto> {
            throw HttpRequestTimeoutException("https://kafe.test/coffee", 1_000)
        }

        assertEquals(Result.Failure(DataError.Remote.REQUEST_TIMEOUT), result)
    }

    @Test
    fun unresolvedAddressReturnsNoInternet() = runTest {
        val result = safeCall<CoffeeDto> { throw UnresolvedAddressException() }

        assertEquals(Result.Failure(DataError.Remote.NO_INTERNET), result)
    }

    @Test
    fun unexpectedExceptionReturnsUnknown() = runTest {
        val result = safeCall<CoffeeDto> { throw IllegalStateException("boom") }

        assertEquals(Result.Failure(DataError.Remote.UNKNOWN), result)
    }

    @Test
    fun cancellationIsRethrown() = runTest {
        assertFailsWith<CancellationException> {
            safeCall<CoffeeDto> { throw CancellationException("cancelled") }
        }
    }
}
