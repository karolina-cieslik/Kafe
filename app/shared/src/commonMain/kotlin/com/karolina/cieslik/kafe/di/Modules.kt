package com.karolina.cieslik.kafe.di

import com.karolina.cieslik.kafe.core.data.database.DatabaseFactory
import com.karolina.cieslik.kafe.core.data.database.KafeDatabase
import com.karolina.cieslik.kafe.core.data.database.buildKafeDatabase
import com.karolina.cieslik.kafe.core.data.network.HttpClientFactory
import com.karolina.cieslik.kafe.core.data.preferences.PreferencesFactory
import com.karolina.cieslik.kafe.core.data.preferences.createDataStore
import io.ktor.client.HttpClient
import org.koin.core.module.Module
import org.koin.dsl.module
import org.koin.dsl.onClose

/** Klient HTTP; zamykany razem z Koinem. Silnik HTTP dostarcza [platformModule]. */
val networkModule = module {
    single<HttpClient> { HttpClientFactory.create(engine = get(), config = get()) } onClose { it?.close() }
}

/** Baza Room (zamykana razem z Koinem) i jej DAO. DatabaseFactory dostarcza [platformModule]. */
val databaseModule = module {
    single<KafeDatabase> { get<DatabaseFactory>().create().buildKafeDatabase() } onClose { it?.close() }
    single { get<KafeDatabase>().syncMetadataDao() }
    single { get<KafeDatabase>().menuDao() }
    single { get<KafeDatabase>().cafeDao() }
}

// KAFE-31: Jedna instancja DataStore na plik preferencji.
val preferencesModule = module {
    single { get<PreferencesFactory>().createDataStore() }
}

/** Zależności platformowe: silnik HTTP i DatabaseFactory. */
expect val platformModule: Module
