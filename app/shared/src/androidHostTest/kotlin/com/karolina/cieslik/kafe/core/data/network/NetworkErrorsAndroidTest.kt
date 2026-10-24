package com.karolina.cieslik.kafe.core.data.network

import com.karolina.cieslik.kafe.core.domain.DataError
import com.karolina.cieslik.kafe.core.domain.Result
import kotlinx.coroutines.test.runTest
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import kotlin.test.Test
import kotlin.test.assertEquals

/** Wyjątki zgłaszane przez silnik OkHttp przy braku zasięgu i timeoutach. */
class NetworkErrorsAndroidTest {

    @Test
    fun unknownHostReturnsNoInternet() = runTest {
        val result = safeCall<String> { throw UnknownHostException("kafe.test") }

        assertEquals(Result.Failure(DataError.Remote.NO_INTERNET), result)
    }

    @Test
    fun connectExceptionReturnsNoInternet() = runTest {
        val result = safeCall<String> { throw ConnectException("Failed to connect") }

        assertEquals(Result.Failure(DataError.Remote.NO_INTERNET), result)
    }

    @Test
    fun socketTimeoutReturnsRequestTimeout() = runTest {
        val result = safeCall<String> { throw SocketTimeoutException("timeout") }

        assertEquals(Result.Failure(DataError.Remote.REQUEST_TIMEOUT), result)
    }
}
