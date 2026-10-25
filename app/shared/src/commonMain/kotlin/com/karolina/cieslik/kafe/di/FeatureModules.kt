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
import com.karolina.cieslik.kafe.feature.cart.data.RoomCartRepository
import com.karolina.cieslik.kafe.feature.cart.domain.AddToCart
import com.karolina.cieslik.kafe.feature.cart.domain.CartRepository
import com.karolina.cieslik.kafe.feature.cart.domain.ObserveCheckedCart
import com.karolina.cieslik.kafe.feature.cart.domain.RefreshCartSnapshots
import com.karolina.cieslik.kafe.feature.cart.presentation.CartViewModel
import com.karolina.cieslik.kafe.feature.menu.data.OfflineFirstMenuRepository
import com.karolina.cieslik.kafe.feature.menu.data.local.MenuLocalDataSource
import com.karolina.cieslik.kafe.feature.menu.data.local.RoomMenuLocalDataSource
import com.karolina.cieslik.kafe.feature.menu.data.remote.MenuRemoteDataSource
import com.karolina.cieslik.kafe.feature.menu.data.remote.SeedMenuRemoteDataSource
import com.karolina.cieslik.kafe.feature.menu.domain.GetMenuForCafe
import com.karolina.cieslik.kafe.feature.menu.domain.MenuRepository
import com.karolina.cieslik.kafe.feature.menu.presentation.MenuViewModel
import com.karolina.cieslik.kafe.feature.product.domain.GetProductDetails
import com.karolina.cieslik.kafe.feature.product.domain.PriceCalculator
import com.karolina.cieslik.kafe.feature.product.presentation.ProductDetailViewModel
import com.karolina.cieslik.kafe.feature.start.presentation.AppStartViewModel
import kotlin.time.Clock
import kotlinx.datetime.TimeZone
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/**
 * Lokale sieci, zapamiętany wybór lokalu (DataStore), wybór lokalu i ekran startowy. Lokalnie Room
 * (DAO z databaseModule), zdalnie na razie seed – backend podmieni tylko *RemoteDataSource, bez zmian
 * w domenie i prezentacji.
 */
val cafeModule = module {
    single<CafeRemoteDataSource> { SeedCafeRemoteDataSource() }
    single<CafeLocalDataSource> { RoomCafeLocalDataSource(dao = get()) }
    single<SelectedCafeStore> { DataStoreSelectedCafeStore(dataStore = get()) }
    single<CafeRepository> { OfflineFirstCafeRepository(local = get(), remote = get(), selectedCafeStore = get()) }
    viewModel {
        CafePickerViewModel(
            cafeRepository = get(),
            clock = Clock.System,
            timeZone = CafeTimeZone,
        )
    }
    viewModel { AppStartViewModel(cafeRepository = get()) }
}

/**
 * Strefa czasowa godzin otwarcia lokali. Godziny są w czasie polskim, więc „otwarte teraz” liczymy w tej
 * strefie niezależnie od strefy telefonu.
 */
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
            cartRepository = get(),
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
            addToCart = get(),
        )
    }
}

/**
 * Koszyk: repozytorium na Room (DAO z databaseModule), przypadki użycia (dodanie, sprawdzenie z menu lokalu,
 * nowa migawka cen) i CartViewModel; kalkulator ceny z [productModule].
 */
val cartModule = module {
    single<CartRepository> { RoomCartRepository(dao = get()) }
    factory { AddToCart(cartRepository = get(), priceCalculator = get()) }
    factory { ObserveCheckedCart(cartRepository = get(), menuRepository = get(), priceCalculator = get()) }
    factory { RefreshCartSnapshots(cartRepository = get(), menuRepository = get(), priceCalculator = get()) }
    viewModel {
        CartViewModel(
            cafeRepository = get(),
            menuRepository = get(),
            cartRepository = get(),
            observeCheckedCart = get(),
            refreshCartSnapshots = get(),
        )
    }
}
