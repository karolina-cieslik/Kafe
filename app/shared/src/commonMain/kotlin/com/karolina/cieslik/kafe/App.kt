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

/**
 * Korzeń UI wspólny dla platform – motyw Kafe, intro i graf nawigacji.
 *
 * Intro gra, dopóki jego animacja się nie skończy ORAZ [AppStartViewModel] nie poda ekranu startowego.
 * Po odtworzeniu aktywności (zmiana motywu, obrót) aplikacja pokazuje się od razu, bez intro.
 *
 * @param appStartViewModel źródło ekranu startowego; domyślnie z Koina, parametr ułatwia podmianę w testach.
 */
@Composable
fun App(appStartViewModel: AppStartViewModel = koinViewModel()) {
    setSingletonImageLoaderFactory { context ->
        ImageLoader.Builder(context)
            .crossfade(true)
            .build()
    }
    val appStart by appStartViewModel.state.collectAsStateWithLifecycle()
    // W rememberSaveable, bo intro ma grać tylko przy zimnym starcie, nie po każdym odtworzeniu aktywności.
    var introFinished by rememberSaveable { mutableStateOf(false) }
    // Ekran startowy jest „zatrzaśnięty” przy pierwszym Ready – późniejsza zmiana (np. po wyborze lokalu)
    // nie tworzy grafu nawigacji od nowa; dalszą nawigację prowadzi już NavController.
    // Wartość początkowa z bieżącego stanu: po odtworzeniu aktywności ViewModel jest już Ready, więc
    // pierwsza kompozycja pokazuje aplikację od razu – bez klatki intro i bez przejścia Crossfade.
    var startDestination by remember {
        mutableStateOf((appStart as? AppStartState.Ready)?.startDestination)
    }

    // Klucz appStart: efekt rusza przy każdej zmianie stanu startu, ale ustawia ekran tylko raz.
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
