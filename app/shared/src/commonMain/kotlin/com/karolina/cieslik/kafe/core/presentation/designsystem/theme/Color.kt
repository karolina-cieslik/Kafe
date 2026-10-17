package com.karolina.cieslik.kafe.core.presentation.designsystem.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

// KAFE-25: Paleta bazowa inspirowana makietą (akcent kawowy, ciemny nagłówek, jasne tło).
// Ekrany nie używają tych stałych bezpośrednio – tylko przez KafeTheme.colors / KafeTheme.extendedColors.
internal object KafePalette {
    val Coffee = Color(0xFFC67C4E)
    val CoffeeLight = Color(0xFFE3A57C)
    val CoffeeDark = Color(0xFF8E5230)
    val Cream = Color(0xFFF9F2ED)
    val CoffeeContainerDark = Color(0xFF4A2E1D)

    val Espresso = Color(0xFF1E1E1E)
    val Charcoal = Color(0xFF313131)
    val Graphite = Color(0xFF242424)

    val Background = Color(0xFFF9F9F9)
    val White = Color(0xFFFFFFFF)
    val Grey100 = Color(0xFFF1F1F1)
    val Grey200 = Color(0xFFE3E3E3)
    val Grey400 = Color(0xFFA2A2A2)
    val Grey600 = Color(0xFF6B6B6B)
    val Grey800 = Color(0xFF2F2D2C)

    val DarkBackground = Color(0xFF121212)
    val DarkSurface = Color(0xFF1C1B1A)
    val DarkSurfaceVariant = Color(0xFF2A2826)
    val DarkOnSurface = Color(0xFFF1EDEA)
    val DarkOnSurfaceVariant = Color(0xFFB8B2AD)
    val DarkOutline = Color(0xFF4A4643)

    val Promo = Color(0xFFED5151)
    val Rating = Color(0xFFFBBE21)
    val ErrorLight = Color(0xFFC62828)
    val ErrorDark = Color(0xFFFF8A80)
}

internal val KafeLightColorScheme: ColorScheme = lightColorScheme(
    primary = KafePalette.Coffee,
    onPrimary = KafePalette.White,
    primaryContainer = KafePalette.Cream,
    onPrimaryContainer = KafePalette.CoffeeDark,
    secondary = KafePalette.Charcoal,
    onSecondary = KafePalette.White,
    secondaryContainer = KafePalette.Grey100,
    onSecondaryContainer = KafePalette.Grey800,
    background = KafePalette.Background,
    onBackground = KafePalette.Grey800,
    surface = KafePalette.White,
    onSurface = KafePalette.Grey800,
    surfaceVariant = KafePalette.Grey100,
    onSurfaceVariant = KafePalette.Grey600,
    surfaceContainerLowest = KafePalette.White,
    surfaceContainerLow = KafePalette.White,
    surfaceContainer = KafePalette.White,
    surfaceContainerHigh = KafePalette.Grey100,
    surfaceContainerHighest = KafePalette.Grey200,
    outline = KafePalette.Grey400,
    outlineVariant = KafePalette.Grey200,
    error = KafePalette.ErrorLight,
    onError = KafePalette.White,
)

internal val KafeDarkColorScheme: ColorScheme = darkColorScheme(
    primary = KafePalette.CoffeeLight,
    onPrimary = KafePalette.Espresso,
    primaryContainer = KafePalette.CoffeeContainerDark,
    onPrimaryContainer = KafePalette.Cream,
    secondary = KafePalette.Grey200,
    onSecondary = KafePalette.Espresso,
    secondaryContainer = KafePalette.DarkSurfaceVariant,
    onSecondaryContainer = KafePalette.DarkOnSurface,
    background = KafePalette.DarkBackground,
    onBackground = KafePalette.DarkOnSurface,
    surface = KafePalette.DarkSurface,
    onSurface = KafePalette.DarkOnSurface,
    surfaceVariant = KafePalette.DarkSurfaceVariant,
    onSurfaceVariant = KafePalette.DarkOnSurfaceVariant,
    surfaceContainerLowest = KafePalette.DarkBackground,
    surfaceContainerLow = KafePalette.DarkSurface,
    surfaceContainer = KafePalette.DarkSurface,
    surfaceContainerHigh = KafePalette.DarkSurfaceVariant,
    surfaceContainerHighest = KafePalette.DarkOutline,
    outline = KafePalette.DarkOutline,
    outlineVariant = KafePalette.DarkSurfaceVariant,
    error = KafePalette.ErrorDark,
    onError = KafePalette.Espresso,
)

// KAFE-25: Kolory spoza ColorScheme Material3 – nagłówek ekranu, promo, ocena.
@Immutable
data class KafeExtendedColors(
    val header: Color,
    val headerGradientEnd: Color,
    val onHeader: Color,
    val onHeaderVariant: Color,
    val headerField: Color,
    val promo: Color,
    val onPromo: Color,
    val rating: Color,
    /** KAFE-42: Tło marki (splash, intro) – kawowe w obu motywach, jak ikona aplikacji. */
    val brand: Color,
    val onBrand: Color,
)

internal val KafeLightExtendedColors = KafeExtendedColors(
    header = KafePalette.Espresso,
    headerGradientEnd = KafePalette.Charcoal,
    onHeader = KafePalette.White,
    onHeaderVariant = KafePalette.Grey400,
    headerField = KafePalette.Charcoal,
    promo = KafePalette.Promo,
    onPromo = KafePalette.White,
    rating = KafePalette.Rating,
    brand = KafePalette.Coffee,
    onBrand = KafePalette.Cream,
)

internal val KafeDarkExtendedColors = KafeExtendedColors(
    header = KafePalette.Graphite,
    headerGradientEnd = KafePalette.DarkSurface,
    onHeader = KafePalette.DarkOnSurface,
    onHeaderVariant = KafePalette.DarkOnSurfaceVariant,
    headerField = KafePalette.DarkSurfaceVariant,
    promo = KafePalette.Promo,
    onPromo = KafePalette.White,
    rating = KafePalette.Rating,
    brand = KafePalette.Coffee,
    onBrand = KafePalette.Cream,
)
