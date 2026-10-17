package com.karolina.cieslik.kafe.di

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.karolina.cieslik.kafe.core.data.database.KafeDatabase
import com.karolina.cieslik.kafe.core.data.database.sync.SyncMetadataDao
import com.karolina.cieslik.kafe.core.data.network.NetworkConfig
import io.ktor.client.HttpClient
import org.junit.runner.RunWith
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.koinApplication
import kotlin.test.Test
import kotlin.test.assertNotNull

// KAFE-2: Pełny graf DI z prawdziwymi zależnościami platformowymi (OkHttp, Room z Context).
@RunWith(AndroidJUnit4::class)
class KoinGraphTest {

    @Test
    fun allProductionDefinitionsAreResolvable() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val koinApp = koinApplication {
            androidContext(context)
            modules(kafeModules(NetworkConfig(baseUrl = "https://api.kafe.test/")))
        }

        try {
            assertNotNull(koinApp.koin.get<HttpClient>())
            assertNotNull(koinApp.koin.get<KafeDatabase>())
            assertNotNull(koinApp.koin.get<SyncMetadataDao>())
        } finally {
            koinApp.close()
            context.deleteDatabase(KafeDatabase.DATABASE_NAME)
        }
    }
}
