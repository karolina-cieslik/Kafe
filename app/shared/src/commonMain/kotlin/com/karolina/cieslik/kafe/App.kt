package com.karolina.cieslik.kafe

import androidx.compose.runtime.Composable
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.request.crossfade
import com.karolina.cieslik.kafe.core.presentation.designsystem.theme.KafeTheme
import com.karolina.cieslik.kafe.navigation.KafeNavHost

/**
 * Korzeń UI wspólny dla platform – motyw Kafe i graf nawigacji ([KafeNavHost]).
 */
@Composable
fun App() {
    // Jeden ImageLoader Coila dla całej aplikacji; crossfade łagodzi pojawianie się zdjęć produktów.
    setSingletonImageLoaderFactory { context ->
        ImageLoader.Builder(context)
            .crossfade(true)
            .build()
    }
    KafeTheme {
        KafeNavHost()
    }
}
