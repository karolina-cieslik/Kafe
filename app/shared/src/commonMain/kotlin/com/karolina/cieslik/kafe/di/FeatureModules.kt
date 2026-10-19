package com.karolina.cieslik.kafe.di

import com.karolina.cieslik.kafe.feature.cafe.data.OfflineFirstCafeRepository
import com.karolina.cieslik.kafe.feature.cafe.data.local.CafeLocalDataSource
import com.karolina.cieslik.kafe.feature.cafe.data.local.DataStoreSelectedCafeStore
import com.karolina.cieslik.kafe.feature.cafe.data.local.RoomCafeLocalDataSource
import com.karolina.cieslik.kafe.feature.cafe.data.local.SelectedCafeStore
import com.karolina.cieslik.kafe.feature.cafe.data.remote.CafeRemoteDataSource
import com.karolina.cieslik.kafe.feature.cafe.data.remote.SeedCafeRemoteDataSource
import com.karolina.cieslik.kafe.feature.cafe.domain.CafeRepository
import com.karolina.cieslik.kafe.feature.cafe.presentation.CafePickerViewModel
import com.karolina.cieslik.kafe.feature.menu.data.OfflineFirstMenuRepository
import com.karolina.cieslik.kafe.feature.menu.data.local.MenuLocalDataSource
import com.karolina.cieslik.kafe.feature.menu.data.local.RoomMenuLocalDataSource
import com.karolina.cieslik.kafe.feature.menu.data.remote.MenuRemoteDataSource
import com.karolina.cieslik.kafe.feature.menu.data.remote.SeedMenuRemoteDataSource
import com.karolina.cieslik.kafe.feature.menu.domain.GetMenuForCafe
import com.karolina.cieslik.kafe.feature.menu.domain.MenuRepository
import com.karolina.cieslik.kafe.feature.menu.presentation.MenuViewModel
import com.karolina.cieslik.kafe.feature.start.presentation.AppStartViewModel
import kotlin.time.Clock
import com.karolina.cieslik.kafe.feature.product.domain.GetProductDetails
import com.karolina.cieslik.kafe.feature.product.domain.PriceCalculator
import com.karolina.cieslik.kafe.feature.product.presentation.ProductDetailViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.datetime.TimeZone
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module

/** Kwalifikator dispatchera do pracy IO (zapytania i mapowanie danych w ViewModelach). */
val IoDispatcher = named("IoDispatcher")

/** Dispatchery wstrzykiwane przez DI, żeby testy mogły je podmienić. */
val dispatchersModule = module {
    single<CoroutineDispatcher>(IoDispatcher) { Dispatchers.IO }
}

/**
 * Lokale sieci. Lokalnie Room (DAO z databaseModule), zdalnie na razie seed – backend podmieni tylko
 * *RemoteDataSource, bez zmian w domenie i prezentacji.
 */
val cafeModule = module {
    single<CafeRemoteDataSource> { SeedCafeRemoteDataSource() }
    single<CafeLocalDataSource> { RoomCafeLocalDataSource(dao = get()) }
    single<SelectedCafeStore> { DataStoreSelectedCafeStore(dataStore = get()) }
    single<CafeRepository> { OfflineFirstCafeRepository(local = get(), remote = get(), selectedCafeStore = get()) }
    // KAFE-29: godziny otwarcia lokali są w czasie polskim, niezależnie od strefy telefonu.
    viewModel {
        CafePickerViewModel(
            cafeRepository = get(),
            clock = Clock.System,
            timeZone = CafeTimeZone,
            dispatcher = get(IoDispatcher),
        )
    }
    viewModel { AppStartViewModel(cafeRepository = get(), dispatcher = get(IoDispatcher)) }
}

val CafeTimeZone: TimeZone = TimeZone.of("Europe/Warsaw")

/** Menu: źródła danych, repozytorium, przypadek użycia i MenuViewModel; źródła jak w [cafeModule]. */
val menuModule = module {
    single<MenuRemoteDataSource> { SeedMenuRemoteDataSource() }
    single<MenuLocalDataSource> { RoomMenuLocalDataSource(dao = get()) }
    single<MenuRepository> { OfflineFirstMenuRepository(local = get(), remote = get()) }
    factory { GetMenuForCafe(menuRepository = get()) }
    viewModel {
        MenuViewModel(
            cafeRepository = get(),
            menuRepository = get(),
            getMenuForCafe = get(),
            dispatcher = get(IoDispatcher),
        )
    }
}

/**
 * Szczegóły produktu: przypadek użycia, kalkulator ceny i ProductDetailViewModel. productId przychodzi z trasy,
 * więc w UI: `koinViewModel { parametersOf(productId) }`.
 */
val productModule = module {
    factory { GetProductDetails(menuRepository = get()) }
    factory { PriceCalculator() }
    viewModel { (productId: String) ->
        ProductDetailViewModel(
            productId = productId,
            cafeRepository = get(),
            menuRepository = get(),
            getProductDetails = get(),
            priceCalculator = get(),
            dispatcher = get(IoDispatcher),
        )
    }
}
