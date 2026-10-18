package com.karolina.cieslik.kafe.core.presentation.designsystem.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.karolina.cieslik.kafe.core.presentation.designsystem.preview.KafePreview
import com.karolina.cieslik.kafe.core.presentation.designsystem.theme.KafeTheme
import kafe.app.shared.generated.resources.Res
import kafe.app.shared.generated.resources.ds_error_title
import kafe.app.shared.generated.resources.ds_loading
import kafe.app.shared.generated.resources.ds_retry
import kafe.app.shared.generated.resources.ic_cloud_off
import kafe.app.shared.generated.resources.ic_coffee
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * Pełnoekranowy stan ładowania – wskaźnik pośrodku, a dla TalkBacka opis „Ładowanie”.
 * Stany pustki i błędu ([KafeEmptyState], [KafeErrorState]) mają wspólny układ: ikona, tytuł, opis, akcja.
 */
@Composable
fun KafeLoadingState(modifier: Modifier = Modifier) {
    val description = stringResource(Res.string.ds_loading)
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxSize()
            .semantics { contentDescription = description },
    ) {
        CircularProgressIndicator(color = KafeTheme.colors.primary)
    }
}

/**
 * Pełnoekranowy stan pustki (np. brak produktów w kategorii lub brak wyników wyszukiwania).
 *
 * @param title krótki nagłówek (ogłaszany przez TalkBack jako nagłówek).
 * @param message wyjaśnienie, co się stało lub co można zrobić.
 * @param icon ilustracja stanu; domyślnie ikona kawy.
 * @param actionLabel etykieta przycisku akcji; przycisk jest widoczny tylko razem z [onAction].
 * @param onAction wywoływane po kliknięciu przycisku akcji.
 */
@Composable
fun KafeEmptyState(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    icon: DrawableResource = Res.drawable.ic_coffee,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    StateLayout(
        icon = icon,
        iconTint = KafeTheme.colors.primary,
        title = title,
        message = message,
        modifier = modifier,
        action = if (actionLabel != null && onAction != null) {
            { KafeSecondaryButton(text = actionLabel, onClick = onAction) }
        } else {
            null
        },
    )
}

/**
 * Pełnoekranowy stan błędu z przyciskiem „Spróbuj ponownie”.
 *
 * @param message opis błędu dla użytkownika (np. brak połączenia).
 * @param onRetry wywoływane po kliknięciu „Spróbuj ponownie”.
 * @param title nagłówek; domyślnie ogólny tytuł błędu.
 */
@Composable
fun KafeErrorState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    title: String = stringResource(Res.string.ds_error_title),
) {
    StateLayout(
        icon = Res.drawable.ic_cloud_off,
        iconTint = KafeTheme.colors.error,
        title = title,
        message = message,
        modifier = modifier,
        action = { KafePrimaryButton(text = stringResource(Res.string.ds_retry), onClick = onRetry) },
    )
}

@Composable
private fun StateLayout(
    icon: DrawableResource,
    iconTint: Color,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)?,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = KafeTheme.spacing.xxl, vertical = KafeTheme.spacing.xl),
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(KafeTheme.spacing.iconLarge + KafeTheme.spacing.l),
        )
        Spacer(Modifier.height(KafeTheme.spacing.l))
        Text(
            text = title,
            style = KafeTheme.typography.titleLarge,
            color = KafeTheme.colors.onBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() },
        )
        Spacer(Modifier.height(KafeTheme.spacing.s))
        Text(
            text = message,
            style = KafeTheme.typography.bodyMedium,
            color = KafeTheme.colors.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (action != null) {
            Spacer(Modifier.height(KafeTheme.spacing.xl))
            action()
        }
    }
}

@PreviewLightDark
@Composable
private fun KafeLoadingStatePreview() {
    KafePreview { KafeLoadingState(Modifier.heightIn(max = 200.dp)) }
}

@PreviewLightDark
@Composable
private fun KafeEmptyStatePreview() {
    KafePreview {
        KafeEmptyState(
            title = "Pusto w menu",
            message = "Ta kawiarnia nie ma teraz nic w tej kategorii.",
            actionLabel = "Zmień kawiarnię",
            onAction = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun KafeErrorStatePreview() {
    KafePreview {
        KafeErrorState(message = "Brak połączenia z internetem. Sprawdź sieć i spróbuj ponownie.", onRetry = {})
    }
}
