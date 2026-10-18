package com.karolina.cieslik.kafe.core.presentation.designsystem.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.karolina.cieslik.kafe.core.presentation.designsystem.preview.KafePreview
import com.karolina.cieslik.kafe.core.presentation.designsystem.theme.KafeTheme

/**
 * KAFE-25: Opcja rozmiaru napoju dla [SizeSelector].
 *
 * @property id identyfikator rozmiaru zwracany w `onSelect`.
 * @property label krótka nazwa, np. „M”.
 * @property volumeLabel pojemność, np. „350 ml”; `null` = linia jest pomijana.
 * @property priceLabel PEŁNA cena w tym rozmiarze (nie dopłata), np. „14,00 zł”.
 * @property isAvailable `false` = opcja przygaszona i nieklikalna.
 */
@Immutable
data class SizeOptionUi(
    val id: String,
    val label: String,
    val volumeLabel: String?,
    val priceLabel: String,
    val isAvailable: Boolean = true,
)

/**
 * KAFE-25: Wybór rozmiaru (np. S/M/L) – grupa radiowa z opcjami równej szerokości i pełną ceną pod każdą.
 *
 * @param options opcje w kolejności wyświetlania.
 * @param selectedId id wybranej opcji; `null` = nic nie jest wybrane.
 * @param onSelect wywoływane z id stukniętej dostępnej opcji (także już wybranej).
 */
@Composable
fun SizeSelector(
    options: List<SizeOptionUi>,
    selectedId: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(KafeTheme.spacing.m),
        modifier = modifier
            .fillMaxWidth()
            .selectableGroup(),
    ) {
        options.forEach { option ->
            SizeOption(
                option = option,
                selected = option.id == selectedId,
                onClick = { onSelect(option.id) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun SizeOption(
    option: SizeOptionUi,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = if (selected) KafeTheme.colors.primary else KafeTheme.colors.onSurface
    Surface(
        selected = selected,
        onClick = onClick,
        enabled = option.isAvailable,
        shape = KafeTheme.shapes.medium,
        color = if (selected) KafeTheme.colors.primaryContainer else KafeTheme.colors.surface,
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) KafeTheme.colors.primary else KafeTheme.colors.outlineVariant,
        ),
        modifier = modifier
            .semantics { role = Role.RadioButton }
            .alpha(if (option.isAvailable) 1f else 0.4f),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = KafeTheme.spacing.s, horizontal = KafeTheme.spacing.xs),
        ) {
            Text(text = option.label, style = KafeTheme.typography.titleMedium, color = accent)
            option.volumeLabel?.let {
                Text(text = it, style = KafeTheme.typography.bodySmall, color = KafeTheme.colors.onSurfaceVariant)
            }
            Text(text = option.priceLabel, style = KafeTheme.typography.labelMedium, color = accent)
        }
    }
}

@PreviewLightDark
@Composable
private fun SizeSelectorPreview() {
    KafePreview {
        SizeSelector(
            options = listOf(
                SizeOptionUi(id = "S", label = "S", volumeLabel = "250 ml", priceLabel = "12,00 zł"),
                SizeOptionUi(id = "M", label = "M", volumeLabel = "350 ml", priceLabel = "14,00 zł"),
                SizeOptionUi(id = "L", label = "L", volumeLabel = "450 ml", priceLabel = "16,00 zł", isAvailable = false),
            ),
            selectedId = "M",
            onSelect = {},
        )
    }
}
