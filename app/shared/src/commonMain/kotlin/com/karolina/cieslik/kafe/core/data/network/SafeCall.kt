package com.karolina.cieslik.kafe.core.data.network

import com.karolina.cieslik.kafe.core.domain.DataError
import com.karolina.cieslik.kafe.core.domain.Result
import io.ktor.client.call.NoTransformationFoundException
import io.ktor.client.call.body
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.ContentConvertException
import io.ktor.util.network.UnresolvedAddressException
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException

/**
 * Wykonuje żądanie HTTP i zwraca zdeserializowane ciało odpowiedzi.
 *
 * Wyjątki (brak zasięgu, timeout) mapuje na [DataError.Remote], a kody HTTP – przez [responseToResult].
 * CancellationException jest zawsze przepuszczany dalej, żeby nie łamać structured concurrency.
 * Łapie każdy Exception celowo: silniki Ktora rzucają różne typy, a wywołujący dostaje zawsze [DataError.Remote].
 */
@Suppress("TooGenericExceptionCaught")
suspend inline fun <reified T> safeCall(execute: () -> HttpResponse): Result<T, DataError.Remote> {
    val response = try {
        execute()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        return Result.Failure(e.toRemoteError())
    }
    return responseToResult(response)
}

/**
 * Zamienia odpowiedź HTTP na [Result]: 2xx – ciało jako [T] (błąd parsowania = [DataError.Remote.SERIALIZATION]),
 * 401/404/408/429 – odpowiadający im błąd, inne 4xx – [DataError.Remote.CLIENT], 5xx – [DataError.Remote.SERVER].
 */
suspend inline fun <reified T> responseToResult(response: HttpResponse): Result<T, DataError.Remote> =
    when (response.status.value) {
        in 200..299 -> try {
            Result.Success(response.body<T>())
        } catch (e: CancellationException) {
            throw e
        } catch (_: NoTransformationFoundException) {
            Result.Failure(DataError.Remote.SERIALIZATION)
        } catch (_: ContentConvertException) {
            Result.Failure(DataError.Remote.SERIALIZATION)
        } catch (_: SerializationException) {
            Result.Failure(DataError.Remote.SERIALIZATION)
        }
        HttpStatusCode.Unauthorized.value -> Result.Failure(DataError.Remote.UNAUTHORIZED)
        HttpStatusCode.NotFound.value -> Result.Failure(DataError.Remote.NOT_FOUND)
        HttpStatusCode.RequestTimeout.value -> Result.Failure(DataError.Remote.REQUEST_TIMEOUT)
        HttpStatusCode.TooManyRequests.value -> Result.Failure(DataError.Remote.TOO_MANY_REQUESTS)
        in 400..499 -> Result.Failure(DataError.Remote.CLIENT)
        in 500..599 -> Result.Failure(DataError.Remote.SERVER)
        else -> Result.Failure(DataError.Remote.UNKNOWN)
    }

/** Mapuje wyjątek klienta HTTP na [DataError.Remote]; publiczne dla inline [safeCall]. */
@PublishedApi
internal fun Throwable.toRemoteError(): DataError.Remote = when {
    this is HttpRequestTimeoutException ||
        this is SocketTimeoutException ||
        this is ConnectTimeoutException ||
        isPlatformTimeoutError() -> DataError.Remote.REQUEST_TIMEOUT

    this is UnresolvedAddressException || isNoInternetError() -> DataError.Remote.NO_INTERNET
    this is SerializationException || this is ContentConvertException -> DataError.Remote.SERIALIZATION
    else -> DataError.Remote.UNKNOWN
}

/** Brak sieci według silnika platformy – OkHttp zgłasza go wyjątkami java.net, Darwin kodami NSURLError. */
internal expect fun Throwable.isNoInternetError(): Boolean

/** Timeout zgłoszony wyjątkiem silnika platformy, którego Ktor nie zamienia na własny typ. */
internal expect fun Throwable.isPlatformTimeoutError(): Boolean
