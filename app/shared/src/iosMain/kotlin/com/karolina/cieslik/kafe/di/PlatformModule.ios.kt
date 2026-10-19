package com.karolina.cieslik.kafe.di

import com.karolina.cieslik.kafe.core.data.database.DatabaseFactory
import com.karolina.cieslik.kafe.core.data.network.platformHttpEngine
import com.karolina.cieslik.kafe.core.data.preferences.PreferencesFactory
import io.ktor.client.engine.HttpClientEngine
import org.koin.core.module.Module
import org.koin.dsl.module

/** iOS: Darwin, baza i preferencje w katalogu dokumentów. */
actual val platformModule: Module = module {
    single<HttpClientEngine> { platformHttpEngine() }
    single { DatabaseFactory() }
    single { PreferencesFactory() }
}

