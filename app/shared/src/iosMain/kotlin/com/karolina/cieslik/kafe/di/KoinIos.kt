package com.karolina.cieslik.kafe.di

import com.karolina.cieslik.kafe.core.data.network.NetworkConfig

// KAFE-2: Wywołanie ze Swifta (iOSApp.init) – w Swift widoczne jako KoinIosKt.startKoinIos(baseUrl:enableLogging:).
fun startKoinIos(baseUrl: String, enableLogging: Boolean) {
    initKoin(NetworkConfig(baseUrl = baseUrl, enableLogging = enableLogging))
}
