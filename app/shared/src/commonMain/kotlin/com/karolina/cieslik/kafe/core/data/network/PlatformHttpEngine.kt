package com.karolina.cieslik.kafe.core.data.network

import io.ktor.client.engine.HttpClientEngine

/** Silnik HTTP właściwy dla platformy – OkHttp (Android), Darwin (iOS). */
expect fun platformHttpEngine(): HttpClientEngine
