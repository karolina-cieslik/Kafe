package com.karolina.cieslik.kafe.core.presentation.designsystem.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.karolina.cieslik.kafe.core.presentation.designsystem.preview.KafePreview
import com.karolina.cieslik.kafe.core.presentation.designsystem.theme.KafeTheme

// KAFE-25: Chip kategorii menu (jednokrotny wybór, zachowuje się jak zakładka).
@Composable
fun CategoryChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        selected = selected,
        onClick = onClick,
        shape = KafeTheme.shapes.medium,
        color = if (selected) KafeTheme.colors.primary else KafeTheme.colors.surface,
        contentColor = if (selected) KafeTheme.colors.onPrimary else KafeTheme.colors.onSurface,
        modifier = modifier.semantics { role = Role.Tab },
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .heightIn(min = KafeTheme.spacing.chipHeight)
                .padding(horizontal = KafeTheme.spacing.l),
        ) {
            Text(
                text = label,
                style = if (selected) KafeTheme.typography.labelLarge else KafeTheme.typography.bodyMedium,
                maxLines = 1,
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun CategoryChipPreview() {
    KafePreview {
        Row(horizontalArrangement = Arrangement.spacedBy(KafeTheme.spacing.s)) {
            CategoryChip(label = "Kawa", selected = true, onClick = {})
            CategoryChip(label = "Herbata", selected = false, onClick = {})
            CategoryChip(label = "Desery", selected = false, onClick = {})
        }
    }
}
