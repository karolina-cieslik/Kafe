package com.karolina.cieslik.kafe

import androidx.compose.runtime.Composable
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.request.crossfade
import com.karolina.cieslik.kafe.core.presentation.designsystem.theme.KafeTheme
import com.karolina.cieslik.kafe.feature.menu.presentation.MenuRoute

/**
 * Korzeń UI wspólny dla platform – motyw Kafe i ekran menu.
 * Ekrany produktu i wyboru lokalu jeszcze nie istnieją, więc nawigacja z menu to na razie no-op.
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
        MenuRoute(
            onNavigateToProduct = { /* TODO(KAFE-9): ekran szczegółów produktu */ },
            onNavigateToCafePicker = { /* TODO(KAFE-29): wybór lokalu */ },
        )
    }
}
