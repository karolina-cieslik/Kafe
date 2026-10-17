package com.karolina.cieslik.kafe.di

import com.karolina.cieslik.kafe.feature.cafe.data.InMemoryCafeRepository
import com.karolina.cieslik.kafe.feature.cafe.data.remote.CafeRemoteDataSource
import com.karolina.cieslik.kafe.feature.cafe.data.remote.SeedCafeRemoteDataSource
import com.karolina.cieslik.kafe.feature.cafe.domain.CafeRepository
import com.karolina.cieslik.kafe.feature.menu.data.InMemoryMenuRepository
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

// KAFE-6: Moduły DI feature'ów. Źródła seed zostaną podmienione na Firebase/Room bez zmian w domenie.
val IoDispatcher = named("IoDispatcher")

val dispatchersModule = module {
    single<CoroutineDispatcher>(IoDispatcher) { Dispatchers.IO }
}

val cafeModule = module {
    single<CafeRemoteDataSource> { SeedCafeRemoteDataSource() }
    single<CafeRepository> { InMemoryCafeRepository(remote = get()) }
}

val menuModule = module {
    single<MenuRemoteDataSource> { SeedMenuRemoteDataSource() }
    single<MenuRepository> { InMemoryMenuRepository(remote = get()) }
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
