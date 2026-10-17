package com.karolina.cieslik.kafe

import androidx.compose.runtime.Composable
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.request.crossfade
import com.karolina.cieslik.kafe.core.presentation.designsystem.theme.KafeTheme
import com.karolina.cieslik.kafe.navigation.KafeNavHost

// KAFE-25 / KAFE-41: Korzeń UI – motyw Kafe i graf nawigacji.
@Composable
fun App() {
    setSingletonImageLoaderFactory { context ->
        ImageLoader.Builder(context)
            .crossfade(true)
            .build()
    }
    KafeTheme {
        KafeNavHost()
    }
}
