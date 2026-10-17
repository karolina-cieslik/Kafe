package com.karolina.cieslik.kafe.core.presentation.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

internal val LocalKafeExtendedColors = staticCompositionLocalOf { KafeLightExtendedColors }

// KAFE-25: Motyw aplikacji – MaterialTheme + rozszerzenia (kolory nagłówka/promo, odstępy) przez CompositionLocal.
@Composable
fun KafeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalKafeExtendedColors provides if (darkTheme) KafeDarkExtendedColors else KafeLightExtendedColors,
        LocalKafeSpacing provides KafeSpacing(),
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) KafeDarkColorScheme else KafeLightColorScheme,
            typography = KafeTypography,
            shapes = KafeShapes,
            content = content,
        )
    }
}

object KafeTheme {
    val colors: ColorScheme
        @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme

    val extendedColors: KafeExtendedColors
        @Composable @ReadOnlyComposable get() = LocalKafeExtendedColors.current

    val typography: Typography
        @Composable @ReadOnlyComposable get() = MaterialTheme.typography

    val shapes: Shapes
        @Composable @ReadOnlyComposable get() = MaterialTheme.shapes

    val spacing: KafeSpacing
        @Composable @ReadOnlyComposable get() = LocalKafeSpacing.current
}
