package com.karolina.cieslik.kafe.core.data.network

import com.karolina.cieslik.kafe.core.domain.DataError
import com.karolina.cieslik.kafe.core.domain.Result
import io.ktor.client.call.NoTransformationFoundException
import io.ktor.client.call.body
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.statement.HttpResponse
import io.ktor.serialization.ContentConvertException
import io.ktor.util.network.UnresolvedAddressException
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException

// KAFE-2: Wykonuje żądanie HTTP i mapuje wyjątki (brak zasięgu, timeout) na DataError.Remote.
// CancellationException jest zawsze przepuszczany dalej, żeby nie łamać structured concurrency.
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

suspend inline fun <reified T> responseToResult(response: HttpResponse): Result<T, DataError.Remote> =
    when (response.status.value) {
        in 200..299 -> try {
            Result.Success(response.body<T>())
        } catch (e: CancellationException) {
            throw e
        } catch (e: NoTransformationFoundException) {
            Result.Failure(DataError.Remote.SERIALIZATION)
        } catch (e: ContentConvertException) {
            Result.Failure(DataError.Remote.SERIALIZATION)
        } catch (e: SerializationException) {
            Result.Failure(DataError.Remote.SERIALIZATION)
        }
        401 -> Result.Failure(DataError.Remote.UNAUTHORIZED)
        404 -> Result.Failure(DataError.Remote.NOT_FOUND)
        408 -> Result.Failure(DataError.Remote.REQUEST_TIMEOUT)
        429 -> Result.Failure(DataError.Remote.TOO_MANY_REQUESTS)
        in 400..499 -> Result.Failure(DataError.Remote.CLIENT)
        in 500..599 -> Result.Failure(DataError.Remote.SERVER)
        else -> Result.Failure(DataError.Remote.UNKNOWN)
    }

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

// KAFE-2: Silniki HTTP zgłaszają brak sieci inaczej (OkHttp – java.net, Darwin – NSURLError).
internal expect fun Throwable.isNoInternetError(): Boolean

internal expect fun Throwable.isPlatformTimeoutError(): Boolean
