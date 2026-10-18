package com.karolina.cieslik.kafe.core.data.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.logging.SIMPLE
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json

/** Jedno miejsce konfiguracji Ktor Client. Silnik wstrzykiwany z platformy (OkHttp/Darwin) lub MockEngine w testach. */
object HttpClientFactory {

    /**
     * Klient z JSON-em [KafeJson], timeoutami i logowaniem z [config] oraz bazowym adresem [NetworkConfig.baseUrl].
     * Nie rzuca wyjątków dla 4xx/5xx – kody mapuje `responseToResult`. Nagłówek Authorization nie trafia do logów.
     */
    fun create(engine: HttpClientEngine, config: NetworkConfig): HttpClient = HttpClient(engine) {
        // Kody HTTP mapuje responseToResult() – Ktor nie rzuca wyjątków dla 4xx/5xx.
        expectSuccess = false

        install(ContentNegotiation) {
            json(KafeJson)
        }
        install(HttpTimeout) {
            requestTimeoutMillis = config.requestTimeoutMillis
            connectTimeoutMillis = config.connectTimeoutMillis
            socketTimeoutMillis = config.socketTimeoutMillis
        }
        install(Logging) {
            logger = Logger.SIMPLE
            level = if (config.enableLogging) LogLevel.ALL else LogLevel.NONE
            sanitizeHeader { header -> header == HttpHeaders.Authorization }
        }
        defaultRequest {
            url(config.baseUrl)
            contentType(ContentType.Application.Json)
        }
    }
}
