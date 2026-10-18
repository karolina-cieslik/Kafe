package com.karolina.cieslik.kafe.core.presentation.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** Styl tekstu na foncie systemowym; rozmiary w sp, [letterSpacing] w sp. */
private fun style(weight: FontWeight, size: Int, lineHeight: Int, letterSpacing: Double = 0.0) = TextStyle(
    fontFamily = FontFamily.Default,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = letterSpacing.sp,
)

/**
 * Typografia Kafe na foncie systemowym (na Androidzie Roboto, na iOS San Francisco) – decyzja
 * produktowa zamiast Sory. Skala, wagi i rozmiary bez zmian względem poprzedniej wersji.
 */
internal val KafeTypography = Typography(
    displayLarge = style(FontWeight.SemiBold, 40, 48),
    displayMedium = style(FontWeight.SemiBold, 34, 42),
    displaySmall = style(FontWeight.SemiBold, 30, 38),
    headlineLarge = style(FontWeight.SemiBold, 28, 36),
    headlineMedium = style(FontWeight.SemiBold, 24, 32),
    headlineSmall = style(FontWeight.SemiBold, 20, 28),
    titleLarge = style(FontWeight.SemiBold, 18, 26),
    titleMedium = style(FontWeight.SemiBold, 16, 22),
    titleSmall = style(FontWeight.SemiBold, 14, 20),
    bodyLarge = style(FontWeight.Normal, 16, 24),
    bodyMedium = style(FontWeight.Normal, 14, 20),
    bodySmall = style(FontWeight.Normal, 12, 16),
    labelLarge = style(FontWeight.SemiBold, 14, 20, 0.1),
    labelMedium = style(FontWeight.Medium, 12, 16, 0.2),
    labelSmall = style(FontWeight.Medium, 10, 14, 0.3),
)
