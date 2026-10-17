package com.karolina.cieslik.kafe

import androidx.compose.runtime.Composable
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.request.crossfade
import com.karolina.cieslik.kafe.core.presentation.designsystem.theme.KafeTheme
import com.karolina.cieslik.kafe.feature.menu.presentation.MenuRoute

// KAFE-25 / KAFE-6: Korzeń UI – motyw Kafe i ekran menu. Ekrany produktu i wyboru lokalu
// jeszcze nie istnieją, więc nawigacja z menu to na razie no-op.
@Composable
fun App() {
    setSingletonImageLoaderFactory { context ->
        ImageLoader.Builder(context)
            .crossfade(true)
            .build()
    }
    KafeTheme {
        MenuRoute(
            onNavigateToProduct = { /* TODO: ekran szczegółów produktu */ },
            onNavigateToCafePicker = { /* TODO: wybór lokalu (epik KAFE-22) */ },
        )
    }
}
