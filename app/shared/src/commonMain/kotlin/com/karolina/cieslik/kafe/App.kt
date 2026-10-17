package com.karolina.cieslik.kafe

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.request.crossfade
import com.karolina.cieslik.kafe.core.presentation.designsystem.theme.KafeTheme
import com.karolina.cieslik.kafe.feature.start.presentation.AppStartState
import com.karolina.cieslik.kafe.feature.start.presentation.AppStartViewModel
import com.karolina.cieslik.kafe.feature.start.presentation.KafeIntro
import com.karolina.cieslik.kafe.navigation.KafeNavHost
import com.karolina.cieslik.kafe.navigation.KafeRoute
import org.koin.compose.viewmodel.koinViewModel

private const val INTRO_CROSSFADE_MS = 300

// KAFE-25 / KAFE-41: Korzeń UI – motyw Kafe i graf nawigacji.
// KAFE-31 / KAFE-42: Intro gra, dopóki animacja się nie skończy ORAZ AppStartViewModel nie poda ekranu startowego.
// - introFinished w rememberSaveable: po obrocie / odtworzeniu procesu intro się nie powtarza (tylko zimny start).
// - Ekran startowy jest „zatrzaśnięty” przy pierwszym Ready – późniejsza zmiana (np. po wyborze lokalu)
//   nie tworzy grafu nawigacji od nowa; dalszą nawigację prowadzi już NavController.
@Composable
fun App(appStartViewModel: AppStartViewModel = koinViewModel()) {
    setSingletonImageLoaderFactory { context ->
        ImageLoader.Builder(context)
            .crossfade(true)
            .build()
    }
    val appStart by appStartViewModel.state.collectAsStateWithLifecycle()
    var introFinished by rememberSaveable { mutableStateOf(false) }
    var startDestination by remember { mutableStateOf<KafeRoute?>(null) }

    LaunchedEffect(appStart) {
        val ready = appStart as? AppStartState.Ready ?: return@LaunchedEffect
        if (startDestination == null) startDestination = ready.startDestination
    }

    KafeTheme {
        val destination = startDestination
        Crossfade(
            targetState = introFinished && destination != null,
            animationSpec = tween(INTRO_CROSSFADE_MS),
            label = "intro",
        ) { showApp ->
            if (showApp && destination != null) {
                KafeNavHost(startDestination = destination)
            } else {
                KafeIntro(onFinished = { introFinished = true })
            }
        }
    }
}
