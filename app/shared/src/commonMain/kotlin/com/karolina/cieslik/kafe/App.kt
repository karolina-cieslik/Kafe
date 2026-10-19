package com.karolina.cieslik.kafe

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.request.crossfade
import com.karolina.cieslik.kafe.core.presentation.designsystem.system.SystemBarsAppearance
import com.karolina.cieslik.kafe.core.presentation.designsystem.theme.KafeTheme
import com.karolina.cieslik.kafe.feature.start.presentation.AppStartState
import com.karolina.cieslik.kafe.feature.start.presentation.AppStartViewModel
import com.karolina.cieslik.kafe.feature.start.presentation.KafeIntro
import com.karolina.cieslik.kafe.navigation.KafeNavHost
import com.karolina.cieslik.kafe.navigation.KafeRoute
import org.koin.compose.viewmodel.koinViewModel

private const val INTRO_CROSSFADE_MS = 300

/** Stabilne tagi korzenia aplikacji dla testów UI. */
internal object AppTestTags {
    /** Tło marki bez animacji – intro już było, a ekran startowy jeszcze nie jest znany. */
    const val BRAND_BACKDROP = "appBrandBackdrop"
}

/**
 * Korzeń UI wspólny dla platform – motyw Kafe, intro i graf nawigacji.
 *
 * Intro gra, dopóki jego animacja się nie skończy ORAZ [AppStartViewModel] nie poda ekranu startowego.
 * Po odtworzeniu aktywności (zmiana motywu, obrót) aplikacja pokazuje się od razu, bez intro. Po śmierci procesu
 * intro też się nie powtarza: do czasu ustalenia ekranu startowego widać samo tło marki.
 *
 * @param appStartViewModel źródło ekranu startowego; domyślnie z Koina, parametr ułatwia podmianę w testach.
 */
@Composable
fun App(appStartViewModel: AppStartViewModel = koinViewModel()) {
    // Jeden ImageLoader Coila dla całej aplikacji; crossfade łagodzi pojawianie się zdjęć produktów.
    setSingletonImageLoaderFactory { context ->
        ImageLoader.Builder(context)
            .crossfade(true)
            .build()
    }
    val appStart by appStartViewModel.state.collectAsStateWithLifecycle()
    // W rememberSaveable, bo intro ma grać tylko przy zimnym starcie, nie po każdym odtworzeniu aktywności.
    var introFinished by rememberSaveable { mutableStateOf(false) }
    // Zakończone intro przywrócone z zapisanego stanu (odtworzenie aktywności albo nowy proces), a nie zagrane teraz.
    // Tylko wtedy zamiast intro jest samo tło marki – przy zimnym starcie intro trzyma się do gotowego ekranu.
    val introRestored = remember { introFinished }
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
            } else if (introRestored) {
                // Po śmierci procesu introFinished wraca z rememberSaveable, a nowy AppStartViewModel jest jeszcze
                // w Loading – intro się nie powtarza, a tło marki zamiast pustego ekranu nie daje białego błysku.
                BrandBackdrop()
            } else {
                KafeIntro(onFinished = { introFinished = true })
            }
        }
    }
}

@Composable
private fun BrandBackdrop() {
    SystemBarsAppearance(darkStatusBarIcons = false, darkNavigationBarIcons = false)
    Box(
        Modifier
            .fillMaxSize()
            .background(KafeTheme.extendedColors.brand)
            .testTag(AppTestTags.BRAND_BACKDROP),
    )
}
