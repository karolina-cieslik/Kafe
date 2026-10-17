package com.karolina.cieslik.kafe.core.data.network

import io.ktor.client.engine.darwin.DarwinHttpRequestException
import platform.Foundation.NSURLErrorCannotConnectToHost
import platform.Foundation.NSURLErrorCannotFindHost
import platform.Foundation.NSURLErrorDNSLookupFailed
import platform.Foundation.NSURLErrorDataNotAllowed
import platform.Foundation.NSURLErrorDomain
import platform.Foundation.NSURLErrorInternationalRoamingOff
import platform.Foundation.NSURLErrorNetworkConnectionLost
import platform.Foundation.NSURLErrorNotConnectedToInternet
import platform.Foundation.NSURLErrorTimedOut

private val noInternetCodes = setOf(
    NSURLErrorNotConnectedToInternet,
    NSURLErrorNetworkConnectionLost,
    NSURLErrorCannotFindHost,
    NSURLErrorCannotConnectToHost,
    NSURLErrorDNSLookupFailed,
    NSURLErrorDataNotAllowed,
    NSURLErrorInternationalRoamingOff,
)

internal actual fun Throwable.isNoInternetError(): Boolean =
    nsUrlErrorCode()?.let { it in noInternetCodes } ?: false

internal actual fun Throwable.isPlatformTimeoutError(): Boolean =
    nsUrlErrorCode() == NSURLErrorTimedOut

private fun Throwable.nsUrlErrorCode(): Long? {
    val origin = (this as? DarwinHttpRequestException)?.origin ?: return null
    return if (origin.domain == NSURLErrorDomain) origin.code else null
}
