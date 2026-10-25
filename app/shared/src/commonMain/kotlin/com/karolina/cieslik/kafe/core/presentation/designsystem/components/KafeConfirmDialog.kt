package com.karolina.cieslik.kafe.core.presentation.designsystem.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.karolina.cieslik.kafe.core.presentation.designsystem.preview.KafePreview
import com.karolina.cieslik.kafe.core.presentation.designsystem.theme.KafeTheme
import kafe.app.shared.generated.resources.Res
import kafe.app.shared.generated.resources.ds_cancel
import org.jetbrains.compose.resources.stringResource

/**
 * Dialog potwierdzenia z dwoma przyciskami tekstowymi (np. „Wyczyść koszyk?” z [Wyczyść] [Anuluj]).
 * O tym, czy dialog jest widoczny, decyduje stan ekranu – komponent pokazuje się zawsze, gdy jest w kompozycji.
 *
 * @param message pytanie albo wyjaśnienie skutków.
 * @param confirmText etykieta przycisku potwierdzenia.
 * @param onConfirm wywoływane po stuknięciu [confirmText].
 * @param onDismiss wywoływane po „Anuluj”, stuknięciu poza dialogiem i systemowym „wstecz”.
 * @param title opcjonalny nagłówek nad [message].
 * @param dismissText etykieta przycisku anulowania; domyślnie „Anuluj”.
 * @param destructive `true`, gdy potwierdzenie usuwa dane (np. wyczyszczenie koszyka) – przycisk w kolorze błędu.
 */
@Composable
fun KafeConfirmDialog(
    message: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    dismissText: String = stringResource(Res.string.ds_cancel),
    destructive: Boolean = false,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = if (destructive) KafeTheme.colors.error else KafeTheme.colors.primary,
                ),
            ) {
                Text(text = confirmText, style = KafeTheme.typography.labelLarge)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = KafeTheme.colors.primary),
            ) {
                Text(text = dismissText, style = KafeTheme.typography.labelLarge)
            }
        },
        title = title?.let { { Text(text = it, style = KafeTheme.typography.titleLarge) } },
        text = { Text(text = message, style = KafeTheme.typography.bodyMedium) },
        shape = KafeTheme.shapes.extraLarge,
        containerColor = KafeTheme.colors.surface,
        titleContentColor = KafeTheme.colors.onSurface,
        textContentColor = KafeTheme.colors.onSurfaceVariant,
        modifier = modifier,
    )
}

@PreviewLightDark
@Composable
private fun KafeConfirmDialogPreview() {
    KafePreview {
        KafeConfirmDialog(
            title = "Wyczyścić koszyk?",
            message = "Czy na pewno usunąć wszystkie pozycje?",
            confirmText = "Wyczyść",
            onConfirm = {},
            onDismiss = {},
            destructive = true,
        )
    }
}
