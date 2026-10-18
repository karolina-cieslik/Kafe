package com.karolina.cieslik.kafe.core.presentation.designsystem.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.karolina.cieslik.kafe.core.presentation.designsystem.preview.KafePreview
import com.karolina.cieslik.kafe.core.presentation.designsystem.theme.KafeTheme

/**
 * KAFE-25: Główny przycisk akcji ekranu (np. „Zamów”, „Dodaj do koszyka”), wypełniony kolorem `primary`.
 *
 * @param text etykieta przycisku.
 * @param onClick wywoływane po kliknięciu; nie jest wywoływane, gdy [enabled] = `false` lub [isLoading] = `true`.
 * @param enabled czy akcja jest dostępna.
 * @param isLoading trwa operacja wywołana przyciskiem – zamiast etykiety kręci się wskaźnik, a przycisk
 * jest zablokowany, żeby nie dało się kliknąć drugi raz.
 */
@Composable
fun KafePrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
) {
    Button(
        onClick = onClick,
        enabled = enabled && !isLoading,
        shape = KafeTheme.shapes.large,
        colors = ButtonDefaults.buttonColors(
            containerColor = KafeTheme.colors.primary,
            contentColor = KafeTheme.colors.onPrimary,
        ),
        modifier = modifier.height(KafeTheme.spacing.buttonHeight),
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                color = KafeTheme.colors.onPrimary,
                strokeWidth = 2.dp,
                modifier = Modifier.size(KafeTheme.spacing.icon),
            )
        } else {
            Text(text = text, style = KafeTheme.typography.titleMedium)
        }
    }
}

/**
 * KAFE-25: Przycisk akcji drugorzędnej (np. „Zmień kawiarnię”) – obrys i tekst w kolorze `primary`.
 *
 * @param text etykieta przycisku.
 * @param onClick wywoływane po kliknięciu, gdy [enabled] = `true`.
 * @param enabled czy akcja jest dostępna; wyłączony przycisk ma wyszarzony obrys.
 */
@Composable
fun KafeSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = KafeTheme.shapes.large,
        border = BorderStroke(
            width = 1.dp,
            color = if (enabled) KafeTheme.colors.primary else KafeTheme.colors.outlineVariant,
        ),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = KafeTheme.colors.primary),
        modifier = modifier.height(KafeTheme.spacing.buttonHeight),
    ) {
        Text(text = text, style = KafeTheme.typography.titleMedium)
    }
}

@PreviewLightDark
@Composable
private fun KafeButtonsPreview() {
    KafePreview {
        Column(verticalArrangement = Arrangement.spacedBy(KafeTheme.spacing.m)) {
            KafePrimaryButton(text = "Zamów", onClick = {}, modifier = Modifier.fillMaxWidth())
            KafePrimaryButton(text = "Zamów", onClick = {}, isLoading = true, modifier = Modifier.fillMaxWidth())
            KafePrimaryButton(text = "Zamów", onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth())
            KafeSecondaryButton(text = "Spróbuj ponownie", onClick = {}, modifier = Modifier.fillMaxWidth())
            KafeSecondaryButton(text = "Spróbuj ponownie", onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth())
        }
    }
}
