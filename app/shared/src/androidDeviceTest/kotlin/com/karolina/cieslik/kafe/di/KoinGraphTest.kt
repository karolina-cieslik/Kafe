package com.karolina.cieslik.kafe.di

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.karolina.cieslik.kafe.core.data.database.KafeDatabase
import com.karolina.cieslik.kafe.core.data.database.sync.SyncMetadataDao
import com.karolina.cieslik.kafe.core.data.network.NetworkConfig
import com.karolina.cieslik.kafe.feature.cafe.data.local.SelectedCafeStore
import com.karolina.cieslik.kafe.feature.cafe.domain.CafeRepository
import com.karolina.cieslik.kafe.feature.cart.domain.AddToCart
import com.karolina.cieslik.kafe.feature.cart.domain.CartRepository
import com.karolina.cieslik.kafe.feature.menu.domain.MenuRepository
import io.ktor.client.HttpClient
import org.junit.runner.RunWith
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.koinApplication
import kotlin.test.Test
import kotlin.test.assertNotNull

/** Pełny graf DI z prawdziwymi zależnościami platformowymi (OkHttp, Room i DataStore z Context). */
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
            assertNotNull(koinApp.koin.get<MenuRepository>())
            assertNotNull(koinApp.koin.get<CafeRepository>())
            assertNotNull(koinApp.koin.get<CartRepository>())
            assertNotNull(koinApp.koin.get<AddToCart>())
            // DataStore z prawdziwą ścieżką z Context.
            assertNotNull(koinApp.koin.get<SelectedCafeStore>())
        } finally {
            koinApp.close()
            context.deleteDatabase(KafeDatabase.DATABASE_NAME)
        }
    }
}
