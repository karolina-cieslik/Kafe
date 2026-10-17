package com.karolina.cieslik.kafe.core.data.network

// KAFE-2: Konfiguracja klienta HTTP – dostarczana przez DI (base URL i flaga debug z platformy).
data class NetworkConfig(
    val baseUrl: String,
    val enableLogging: Boolean = false,
    val requestTimeoutMillis: Long = 20_000,
    val connectTimeoutMillis: Long = 10_000,
    val socketTimeoutMillis: Long = 20_000,
)
