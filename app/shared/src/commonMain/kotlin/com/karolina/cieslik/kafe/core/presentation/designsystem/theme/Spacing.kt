package com.karolina.cieslik.kafe.core.presentation.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Skala odstępów (siatka 4dp, od [xxs] do [xxxl]) i stałe rozmiary komponentów,
 * dostępne przez [KafeTheme.spacing].
 *
 * @property xxs 2dp.
 * @property xs 4dp.
 * @property s 8dp – odstęp między chipami.
 * @property m 12dp – odstęp między kartami w siatce.
 * @property l 16dp.
 * @property xl 24dp.
 * @property xxl 32dp.
 * @property xxxl 48dp.
 * @property screenHorizontal poziomy margines treści ekranu.
 * @property minTouchTarget minimalny obszar dotyku (48dp, wymóg dostępności).
 * @property buttonHeight wysokość głównych przycisków.
 * @property fieldHeight wysokość pól tekstowych (np. wyszukiwarki).
 * @property chipHeight wysokość chipów kategorii.
 * @property iconSmall rozmiar małej ikony (np. w polu tekstowym).
 * @property icon standardowy rozmiar ikony.
 * @property iconLarge rozmiar ikony ilustrującej stan (pusty, błąd).
 * @property addButton rozmiar widocznego przycisku „+” na karcie produktu (obszar dotyku jest większy).
 * @property cardImageHeight wysokość zdjęcia na karcie produktu.
 */
@Immutable
data class KafeSpacing(
    val xxs: Dp = 2.dp,
    val xs: Dp = 4.dp,
    val s: Dp = 8.dp,
    val m: Dp = 12.dp,
    val l: Dp = 16.dp,
    val xl: Dp = 24.dp,
    val xxl: Dp = 32.dp,
    val xxxl: Dp = 48.dp,
    val screenHorizontal: Dp = 24.dp,
    val minTouchTarget: Dp = 48.dp,
    val buttonHeight: Dp = 56.dp,
    val fieldHeight: Dp = 52.dp,
    val chipHeight: Dp = 38.dp,
    val iconSmall: Dp = 16.dp,
    val icon: Dp = 24.dp,
    val iconLarge: Dp = 48.dp,
    val addButton: Dp = 32.dp,
    val cardImageHeight: Dp = 128.dp,
    val heroImageHeight: Dp = 240.dp,
)

/** Odstępy bieżącego motywu; ustawia je [KafeTheme]. */
internal val LocalKafeSpacing = staticCompositionLocalOf { KafeSpacing() }
