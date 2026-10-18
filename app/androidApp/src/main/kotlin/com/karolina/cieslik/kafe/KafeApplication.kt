package com.karolina.cieslik.kafe

import android.app.Application
import com.karolina.cieslik.kafe.core.data.network.NetworkConfig
import com.karolina.cieslik.kafe.di.initKoin
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger

/** Aplikacja Android: startuje DI z adresem API z BuildConfig; logi Koina i HTTP tylko w buildzie debug. */
class KafeApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        initKoin(
            networkConfig = NetworkConfig(
                baseUrl = BuildConfig.API_BASE_URL,
                enableLogging = BuildConfig.DEBUG,
            ),
        ) {
            if (BuildConfig.DEBUG) androidLogger()
            androidContext(this@KafeApplication)
        }
    }
}
