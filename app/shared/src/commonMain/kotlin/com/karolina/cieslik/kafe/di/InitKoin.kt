package com.karolina.cieslik.kafe.di

import com.karolina.cieslik.kafe.core.data.network.NetworkConfig
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

/**
 * Punkt startowy DI – wywoływany raz z Application (Android) lub z iOSApp (iOS).
 * [appDeclaration] dokłada konfigurację platformy (np. androidContext); drugie wywołanie rzuca wyjątek Koina.
 */
fun initKoin(
    networkConfig: NetworkConfig,
    appDeclaration: KoinAppDeclaration = {},
): KoinApplication = startKoin {
    appDeclaration()
    modules(kafeModules(networkConfig))
}

internal fun kafeModules(networkConfig: NetworkConfig) = listOf(
    module { single { networkConfig } },
    platformModule,
    networkModule,
    databaseModule,
    preferencesModule,
    dispatchersModule,
    cafeModule,
    menuModule,
    productModule,
)
