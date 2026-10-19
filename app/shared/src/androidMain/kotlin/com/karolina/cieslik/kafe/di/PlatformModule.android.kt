package com.karolina.cieslik.kafe.di

import com.karolina.cieslik.kafe.core.data.database.DatabaseFactory
import com.karolina.cieslik.kafe.core.data.network.platformHttpEngine
import com.karolina.cieslik.kafe.core.data.preferences.PreferencesFactory
import io.ktor.client.engine.HttpClientEngine
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

/** Android: OkHttp i baza w katalogu aplikacji (wymaga androidContext w Koinie). */
actual val platformModule: Module = module {
    single<HttpClientEngine> { platformHttpEngine() }
    single { DatabaseFactory(androidContext()) }
    single { PreferencesFactory(androidContext()) }
}
