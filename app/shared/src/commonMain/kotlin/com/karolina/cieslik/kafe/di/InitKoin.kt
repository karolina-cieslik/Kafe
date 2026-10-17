package com.karolina.cieslik.kafe.di

import com.karolina.cieslik.kafe.core.data.network.NetworkConfig
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

// KAFE-2: Punkt startowy DI – wywoływany raz z Application (Android) lub z iOSApp (iOS).
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
    dispatchersModule,
    cafeModule,
    menuModule,
)
