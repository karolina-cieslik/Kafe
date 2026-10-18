package com.karolina.cieslik.kafe.core.data.network

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp

/** OkHttp z ponawianiem połączenia po zerwaniu. */
actual fun platformHttpEngine(): HttpClientEngine = OkHttp.create {
    config {
        retryOnConnectionFailure(true)
    }
}
