package com.karolina.cieslik.kafe

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.request.crossfade
import com.karolina.cieslik.kafe.core.presentation.designsystem.theme.KafeTheme
import com.karolina.cieslik.kafe.feature.start.presentation.AppStartState
import com.karolina.cieslik.kafe.feature.start.presentation.AppStartViewModel
import com.karolina.cieslik.kafe.navigation.KafeNavHost
import org.koin.compose.viewmodel.koinViewModel

// KAFE-25 / KAFE-41: Korzeń UI – motyw Kafe i graf nawigacji.
// KAFE-31: ekran startowy (Menu albo CafePicker) z AppStartViewModel; do czasu intro (KAFE-42) pusta powierzchnia.
@Composable
fun App(appStartViewModel: AppStartViewModel = koinViewModel()) {
    setSingletonImageLoaderFactory { context ->
        ImageLoader.Builder(context)
            .crossfade(true)
            .build()
    }
    val appStart by appStartViewModel.state.collectAsStateWithLifecycle()
    KafeTheme {
        when (val start = appStart) {
            AppStartState.Loading -> Surface(Modifier.fillMaxSize()) {}
            is AppStartState.Ready -> KafeNavHost(startDestination = start.startDestination)
        }
    }
}
