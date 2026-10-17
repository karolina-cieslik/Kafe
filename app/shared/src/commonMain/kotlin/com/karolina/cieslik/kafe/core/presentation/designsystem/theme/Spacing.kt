package com.karolina.cieslik.kafe.core.presentation.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// KAFE-25: Skala odstępów (siatka 4dp) i stałe rozmiary komponentów.
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

internal val LocalKafeSpacing = staticCompositionLocalOf { KafeSpacing() }
