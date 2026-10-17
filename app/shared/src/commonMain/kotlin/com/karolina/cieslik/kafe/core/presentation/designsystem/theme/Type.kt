package com.karolina.cieslik.kafe.core.presentation.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import kafe.app.shared.generated.resources.Res
import kafe.app.shared.generated.resources.sora_bold
import kafe.app.shared.generated.resources.sora_medium
import kafe.app.shared.generated.resources.sora_regular
import kafe.app.shared.generated.resources.sora_semibold
import org.jetbrains.compose.resources.Font

// KAFE-25: Sora (SIL OFL 1.1, licencja w licenses/Sora-OFL.txt) – statyczne wagi wygenerowane z wersji zmiennej.
@Composable
private fun soraFontFamily(): FontFamily = FontFamily(
    Font(Res.font.sora_regular, FontWeight.Normal),
    Font(Res.font.sora_medium, FontWeight.Medium),
    Font(Res.font.sora_semibold, FontWeight.SemiBold),
    Font(Res.font.sora_bold, FontWeight.Bold),
)

@Composable
internal fun kafeTypography(): Typography {
    val sora = soraFontFamily()
    fun style(weight: FontWeight, size: Int, lineHeight: Int, letterSpacing: Double = 0.0) = TextStyle(
        fontFamily = sora,
        fontWeight = weight,
        fontSize = size.sp,
        lineHeight = lineHeight.sp,
        letterSpacing = letterSpacing.sp,
    )
    return Typography(
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
}
