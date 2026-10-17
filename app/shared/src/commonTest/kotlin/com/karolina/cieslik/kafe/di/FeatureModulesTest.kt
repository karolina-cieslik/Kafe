package com.karolina.cieslik.kafe.di

import com.karolina.cieslik.kafe.feature.cafe.domain.CafeRepository
import com.karolina.cieslik.kafe.feature.menu.domain.GetMenuForCafe
import com.karolina.cieslik.kafe.feature.menu.domain.MenuRepository
import org.koin.dsl.koinApplication
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertSame

// KAFE-6: Graf DI feature'ów menu i lokali (bez zależności platformowych).
class FeatureModulesTest {

    @Test
    fun menuAndCafeDefinitionsAreResolvable() {
        val koin = koinApplication { modules(dispatchersModule, cafeModule, menuModule) }.koin

        try {
            assertSame(koin.get<CafeRepository>(), koin.get<CafeRepository>())
            assertSame(koin.get<MenuRepository>(), koin.get<MenuRepository>())
            assertNotNull(koin.get<GetMenuForCafe>())
        } finally {
            koin.close()
        }
    }
}
