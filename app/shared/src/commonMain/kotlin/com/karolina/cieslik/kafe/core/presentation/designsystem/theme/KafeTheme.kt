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

/** KAFE-25: Kolory rozszerzone bieżącego motywu; ustawia je [KafeTheme], domyślnie jasne. */
internal val LocalKafeExtendedColors = staticCompositionLocalOf { KafeLightExtendedColors }

/**
 * KAFE-25: Motyw aplikacji – [MaterialTheme] z kolorami, typografią i kształtami Kafe oraz rozszerzenia
 * ([KafeExtendedColors], [KafeSpacing]) udostępniane przez CompositionLocal.
 * Owija cały korzeń UI i każdy preview; ekrany odczytują wartości przez obiekt [KafeTheme].
 *
 * @param darkTheme czy użyć ciemnego motywu; domyślnie zgodnie z ustawieniem systemu.
 * @param content treść, która dostaje motyw.
 */
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

/**
 * KAFE-25: Jedyny punkt dostępu ekranów i komponentów do tokenów motywu – zamiast bezpośrednich odwołań
 * do [MaterialTheme] czy palety. Działa tylko wewnątrz composable [KafeTheme].
 */
object KafeTheme {
    /** Kolory Material3 bieżącego motywu (jasnego lub ciemnego). */
    val colors: ColorScheme
        @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme

    /** Kolory spoza Material3 – nagłówek, promo, ocena. */
    val extendedColors: KafeExtendedColors
        @Composable @ReadOnlyComposable get() = LocalKafeExtendedColors.current

    /** Typografia Kafe (systemowy font, tytuły SemiBold). */
    val typography: Typography
        @Composable @ReadOnlyComposable get() = MaterialTheme.typography

    /** Zaokrąglenia 6–24dp. */
    val shapes: Shapes
        @Composable @ReadOnlyComposable get() = MaterialTheme.shapes

    /** Odstępy i stałe rozmiary komponentów. */
    val spacing: KafeSpacing
        @Composable @ReadOnlyComposable get() = LocalKafeSpacing.current
}
