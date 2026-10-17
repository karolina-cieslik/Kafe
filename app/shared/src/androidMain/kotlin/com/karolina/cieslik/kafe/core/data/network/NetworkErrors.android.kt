package com.karolina.cieslik.kafe.core.data.network

import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.UnknownHostException
import java.nio.channels.UnresolvedAddressException

internal actual fun Throwable.isNoInternetError(): Boolean =
    this is UnknownHostException ||
        this is ConnectException ||
        this is NoRouteToHostException ||
        this is UnresolvedAddressException

internal actual fun Throwable.isPlatformTimeoutError(): Boolean =
    this is java.net.SocketTimeoutException
