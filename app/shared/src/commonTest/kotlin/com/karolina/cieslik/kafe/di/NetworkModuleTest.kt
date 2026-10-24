package com.karolina.cieslik.kafe.di

import com.karolina.cieslik.kafe.core.data.network.NetworkConfig
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respondOk
import kotlinx.coroutines.Job
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

/** Graf DI warstwy sieci – rozwiązywanie zależności i zamykanie HttpClient razem z Koinem. */
class NetworkModuleTest {

    private val testPlatformModule = module {
        single { NetworkConfig(baseUrl = "https://api.kafe.test/") }
        single<HttpClientEngine> { MockEngine { respondOk() } }
    }

    @Test
    fun httpClientIsResolvedAsSingleton() {
        val koin = koinApplication { modules(testPlatformModule, networkModule) }.koin

        assertSame(koin.get<HttpClient>(), koin.get<HttpClient>())

        koin.close()
    }

    @Test
    fun httpClientIsClosedTogetherWithKoin() {
        val koinApp = koinApplication { modules(testPlatformModule, networkModule) }
        val client = koinApp.koin.get<HttpClient>()
        val clientJob = requireNotNull(client.coroutineContext[Job])
        assertTrue(clientJob.isActive)

        koinApp.close()

        assertFalse(clientJob.isActive)
    }
}
