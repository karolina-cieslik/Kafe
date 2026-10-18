package com.karolina.cieslik.kafe.di

import com.karolina.cieslik.kafe.feature.cafe.data.OfflineFirstCafeRepository
import com.karolina.cieslik.kafe.feature.cafe.data.local.CafeLocalDataSource
import com.karolina.cieslik.kafe.feature.cafe.data.local.RoomCafeLocalDataSource
import com.karolina.cieslik.kafe.feature.cafe.data.remote.CafeRemoteDataSource
import com.karolina.cieslik.kafe.feature.cafe.data.remote.SeedCafeRemoteDataSource
import com.karolina.cieslik.kafe.feature.cafe.domain.CafeRepository
import com.karolina.cieslik.kafe.feature.menu.data.OfflineFirstMenuRepository
import com.karolina.cieslik.kafe.feature.menu.data.local.MenuLocalDataSource
import com.karolina.cieslik.kafe.feature.menu.data.local.RoomMenuLocalDataSource
import com.karolina.cieslik.kafe.feature.menu.data.remote.MenuRemoteDataSource
import com.karolina.cieslik.kafe.feature.menu.data.remote.SeedMenuRemoteDataSource
import com.karolina.cieslik.kafe.feature.menu.domain.GetMenuForCafe
import com.karolina.cieslik.kafe.feature.menu.domain.MenuRepository
import com.karolina.cieslik.kafe.feature.menu.presentation.MenuViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
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
    single<CafeRepository> { OfflineFirstCafeRepository(local = get(), remote = get()) }
}

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
