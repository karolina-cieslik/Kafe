package com.karolina.cieslik.kafe.core.presentation.designsystem.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.ZeroCornerSize
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.karolina.cieslik.kafe.core.presentation.designsystem.preview.KafePreview
import com.karolina.cieslik.kafe.core.presentation.designsystem.theme.KafeTheme
import kafe.app.shared.generated.resources.Res
import kafe.app.shared.generated.resources.ds_total_label
import org.jetbrains.compose.resources.stringResource

/**
 * Przypięty dolny pasek z ceną i główną akcją ekranu (np. „Dodaj do koszyka”). Cena jest widoczna zawsze,
 * niezależnie od przewijania treści nad paskiem. Pasek sam dodaje odstęp od paska nawigacji systemu.
 *
 * @param priceLabel sformatowana cena, np. „28,00 zł”.
 * @param actionText etykieta przycisku akcji.
 * @param onActionClick wywoływane po kliknięciu przycisku, gdy [actionEnabled] = `true`.
 * @param actionEnabled czy akcja jest dostępna (np. `false` dla produktu niedostępnego lub bez wymaganego wyboru).
 * @param priceCaption podpis nad ceną; domyślnie „Razem”. TalkBack czyta podpis i cenę razem.
 * @param actionTestTag opcjonalny tag testowy przycisku akcji.
 */
@Composable
fun KafeBottomActionBar(
    priceLabel: String,
    actionText: String,
    onActionClick: () -> Unit,
    modifier: Modifier = Modifier,
    actionEnabled: Boolean = true,
    priceCaption: String = stringResource(Res.string.ds_total_label),
    actionTestTag: String? = null,
) {
    val priceDescription = "$priceCaption $priceLabel"
    Surface(
        color = KafeTheme.colors.surface,
        shape = KafeTheme.shapes.extraLarge.copy(bottomStart = ZeroCornerSize, bottomEnd = ZeroCornerSize),
        shadowElevation = 8.dp,
        modifier = modifier,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(KafeTheme.spacing.xl),
            modifier = Modifier
                .navigationBarsPadding()
                .padding(horizontal = KafeTheme.spacing.screenHorizontal, vertical = KafeTheme.spacing.l),
        ) {
            Column(modifier = Modifier.semantics(mergeDescendants = true) { contentDescription = priceDescription }) {
                Text(
                    text = priceCaption,
                    style = KafeTheme.typography.bodySmall,
                    color = KafeTheme.colors.onSurfaceVariant,
                )
                Text(
                    text = priceLabel,
                    style = KafeTheme.typography.titleLarge,
                    color = KafeTheme.colors.primary,
                    maxLines = 1,
                )
            }
            KafePrimaryButton(
                text = actionText,
                onClick = onActionClick,
                enabled = actionEnabled,
                modifier = Modifier
                    .weight(1f)
                    .then(if (actionTestTag != null) Modifier.testTag(actionTestTag) else Modifier),
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun KafeBottomActionBarPreview() {
    KafePreview(withPadding = false) {
        Column(verticalArrangement = Arrangement.spacedBy(KafeTheme.spacing.l)) {
            KafeBottomActionBar(priceLabel = "18,50 zł", actionText = "Dodaj do koszyka", onActionClick = {})
            KafeBottomActionBar(
                priceLabel = "18,50 zł",
                actionText = "Dodaj do koszyka",
                onActionClick = {},
                actionEnabled = false,
            )
        }
    }
}
