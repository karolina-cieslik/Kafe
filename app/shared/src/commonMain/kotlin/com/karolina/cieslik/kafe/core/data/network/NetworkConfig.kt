package com.karolina.cieslik.kafe.core.data.network

/**
 * Konfiguracja klienta HTTP – dostarczana przez DI (bazowy adres i flaga debug z platformy).
 *
 * @property baseUrl bazowy adres API, do którego doklejane są ścieżki żądań.
 * @property enableLogging logowanie całych żądań i odpowiedzi; tylko w buildach debug.
 * @property requestTimeoutMillis limit całego żądania w milisekundach.
 * @property connectTimeoutMillis limit nawiązania połączenia w milisekundach.
 * @property socketTimeoutMillis maksymalna przerwa między pakietami w milisekundach.
 */
data class NetworkConfig(
    val baseUrl: String,
    val enableLogging: Boolean = false,
    val requestTimeoutMillis: Long = 20_000,
    val connectTimeoutMillis: Long = 10_000,
    val socketTimeoutMillis: Long = 20_000,
)
