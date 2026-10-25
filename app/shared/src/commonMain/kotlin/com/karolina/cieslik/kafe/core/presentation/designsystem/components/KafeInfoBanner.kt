package com.karolina.cieslik.kafe.core.presentation.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.karolina.cieslik.kafe.core.presentation.designsystem.preview.KafePreview
import com.karolina.cieslik.kafe.core.presentation.designsystem.theme.KafeTheme
import kafe.app.shared.generated.resources.Res
import kafe.app.shared.generated.resources.ic_info
import org.jetbrains.compose.resources.painterResource

/**
 * Baner informacyjny na całą szerokość w kolorze akcentu, np. „Ten produkt jest chwilowo niedostępny w …”
 * albo „Nie udało się sprawdzić aktualnych cen”. Nie jest klikalny; ikona jest dekoracyjna, TalkBack czyta tekst.
 *
 * @param text treść banera.
 */
@Composable
fun KafeInfoBanner(
    text: String,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(KafeTheme.spacing.s),
        modifier = modifier
            .fillMaxWidth()
            .clip(KafeTheme.shapes.medium)
            .background(KafeTheme.colors.primaryContainer)
            .padding(KafeTheme.spacing.m),
    ) {
        Icon(
            painter = painterResource(Res.drawable.ic_info),
            contentDescription = null,
            tint = KafeTheme.colors.onPrimaryContainer,
            modifier = Modifier.size(KafeTheme.spacing.icon - KafeTheme.spacing.xs),
        )
        Text(
            text = text,
            style = KafeTheme.typography.bodyMedium,
            color = KafeTheme.colors.onPrimaryContainer,
        )
    }
}

@PreviewLightDark
@Composable
private fun KafeInfoBannerPreview() {
    KafePreview {
        KafeInfoBanner(text = "Nie udało się sprawdzić aktualnych cen. Pokazujemy ceny z chwili dodania do koszyka.")
    }
}
