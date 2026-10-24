package com.karolina.cieslik.kafe.di

import com.karolina.cieslik.kafe.core.domain.DataError
import com.karolina.cieslik.kafe.core.domain.EmptyResult
import com.karolina.cieslik.kafe.core.domain.Result
import com.karolina.cieslik.kafe.feature.cafe.data.local.CafeLocalDataSource
import com.karolina.cieslik.kafe.feature.cafe.data.local.SelectedCafeStore
import com.karolina.cieslik.kafe.feature.cafe.data.remote.CafeDto
import com.karolina.cieslik.kafe.feature.cafe.domain.Cafe
import com.karolina.cieslik.kafe.feature.cafe.domain.CafeRepository
import com.karolina.cieslik.kafe.feature.cart.data.local.CartDao
import com.karolina.cieslik.kafe.feature.cart.data.local.CartItemEntity
import com.karolina.cieslik.kafe.feature.cart.domain.AddToCart
import com.karolina.cieslik.kafe.feature.cart.domain.CartRepository
import com.karolina.cieslik.kafe.feature.menu.data.local.MenuLocalDataSource
import com.karolina.cieslik.kafe.feature.menu.data.remote.AvailabilityDto
import com.karolina.cieslik.kafe.feature.menu.data.remote.MenuDto
import com.karolina.cieslik.kafe.feature.menu.domain.GetMenuForCafe
import com.karolina.cieslik.kafe.feature.menu.domain.Menu
import com.karolina.cieslik.kafe.feature.menu.domain.MenuRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertSame

/**
 * Graf DI feature'ów. Room (DAO) sprawdza KoinGraphTest na urządzeniu, tu lokalne źródła są podmienione,
 * żeby test działał na JVM.
 */
class FeatureModulesTest {

    private val fakeLocalModule = module {
        single<MenuLocalDataSource> {
            object : MenuLocalDataSource {
                override fun observeMenu(cafeId: String): Flow<Menu?> = flowOf(null)
                override suspend fun saveMenu(
                    cafeId: String,
                    menu: MenuDto,
                    availability: AvailabilityDto,
                    syncedAtEpochMillis: Long,
                ): EmptyResult<DataError.Local> = Result.Success(Unit)
            }
        }
        // DataStore wymaga ścieżki platformowej – na JVM wybór w pamięci.
        single<SelectedCafeStore> {
            object : SelectedCafeStore {
                private val id = MutableStateFlow<String?>(null)
                override fun observeSelectedCafeId(): Flow<String?> = id
                override suspend fun saveSelectedCafeId(cafeId: String): EmptyResult<DataError.Local> {
                    id.value = cafeId
                    return Result.Success(Unit)
                }
            }
        }
        single<CartDao> {
            object : CartDao() {
                override fun observeItems(): Flow<List<CartItemEntity>> = flowOf(emptyList())
                override suspend fun updateQuantity(id: Long, quantity: Int): Int = 0
                override suspend fun deleteAll() = Unit
                override suspend fun findByKey(configurationKey: String): CartItemEntity? = null
                override suspend fun findById(id: Long): CartItemEntity? = null
                override suspend fun insert(item: CartItemEntity): Long = 1
                override suspend fun update(item: CartItemEntity) = Unit
                override suspend fun deleteById(id: Long) = Unit
            }
        }
        single<CafeLocalDataSource> {
            object : CafeLocalDataSource {
                override fun observeCafes(): Flow<List<Cafe>?> = flowOf(null)
                override suspend fun saveCafes(cafes: List<CafeDto>, syncedAtEpochMillis: Long): EmptyResult<DataError.Local> =
                    Result.Success(Unit)
            }
        }
    }

    @Test
    fun featureDefinitionsAreResolvable() {
        val koin = koinApplication {
            allowOverride(true)
            modules(cafeModule, menuModule, productModule, cartModule, fakeLocalModule)
        }.koin

        try {
            assertSame(koin.get<CafeRepository>(), koin.get<CafeRepository>())
            assertSame(koin.get<MenuRepository>(), koin.get<MenuRepository>())
            assertNotNull(koin.get<GetMenuForCafe>())
            assertSame(koin.get<CartRepository>(), koin.get<CartRepository>())
            assertNotNull(koin.get<AddToCart>())
        } finally {
            koin.close()
        }
    }
}
