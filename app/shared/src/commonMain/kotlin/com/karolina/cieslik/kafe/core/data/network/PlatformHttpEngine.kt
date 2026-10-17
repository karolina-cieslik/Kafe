package com.karolina.cieslik.kafe.core.data.network

import io.ktor.client.engine.HttpClientEngine

// KAFE-2: Silnik HTTP właściwy dla platformy – OkHttp (Android), Darwin (iOS).
expect fun platformHttpEngine(): HttpClientEngine
