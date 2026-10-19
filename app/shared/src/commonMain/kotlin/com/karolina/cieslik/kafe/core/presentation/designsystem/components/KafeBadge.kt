package com.karolina.cieslik.kafe.core.presentation.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.karolina.cieslik.kafe.core.presentation.designsystem.preview.KafePreview
import com.karolina.cieslik.kafe.core.presentation.designsystem.theme.KafeTheme

/**
 * Wariant kolorystyczny [KafeBadge]:
 * - [Neutral] – ciemne tło `secondary`, np. „Chwilowo niedostępny” na zdjęciu produktu;
 * - [Accent] – kawowy kontener `primaryContainer`, np. „Otwarte”;
 * - [Muted] – wyciszone tło `surfaceVariant`, np. „Zamknięte”.
 */
enum class KafeBadgeStyle { Neutral, Accent, Muted }

/**
 * Mała, jednoliniowa etykieta statusu („Chwilowo niedostępny”, „Otwarte”, „Zamknięte”).
 * Nie jest klikalna; TalkBack czyta ją jak zwykły tekst.
 *
 * @param text treść etykiety.
 * @param style wariant kolorów; domyślnie [KafeBadgeStyle.Neutral].
 */
@Composable
fun KafeBadge(
    text: String,
    modifier: Modifier = Modifier,
    style: KafeBadgeStyle = KafeBadgeStyle.Neutral,
) {
    val (container, content) = badgeColors(style)
    Text(
        text = text,
        style = KafeTheme.typography.labelMedium,
        color = content,
        maxLines = 1,
        modifier = modifier
            .clip(KafeTheme.shapes.small)
            .background(container)
            .padding(horizontal = KafeTheme.spacing.s, vertical = KafeTheme.spacing.xs),
    )
}

@Composable
private fun badgeColors(style: KafeBadgeStyle): Pair<Color, Color> = when (style) {
    KafeBadgeStyle.Neutral -> KafeTheme.colors.secondary to KafeTheme.colors.onSecondary
    KafeBadgeStyle.Accent -> KafeTheme.colors.primaryContainer to KafeTheme.colors.onPrimaryContainer
    KafeBadgeStyle.Muted -> KafeTheme.colors.surfaceVariant to KafeTheme.colors.onSurfaceVariant
}

@PreviewLightDark
@Composable
private fun KafeBadgePreview() {
    KafePreview {
        Row(horizontalArrangement = Arrangement.spacedBy(KafeTheme.spacing.s)) {
            KafeBadge(text = "Chwilowo niedostępny")
            KafeBadge(text = "Otwarte", style = KafeBadgeStyle.Accent)
            KafeBadge(text = "Zamknięte", style = KafeBadgeStyle.Muted)
        }
    }
}
