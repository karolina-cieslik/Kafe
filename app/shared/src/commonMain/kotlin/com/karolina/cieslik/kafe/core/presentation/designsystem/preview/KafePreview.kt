package com.karolina.cieslik.kafe.core.presentation.designsystem.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.karolina.cieslik.kafe.core.presentation.designsystem.theme.KafeTheme

/**
 * KAFE-25: Opakowanie previewów – nakłada [KafeTheme] (jasny lub ciemny zgodnie z uiMode, np. z
 * `@PreviewLightDark`) i tło `background`, bo sam preview komponentu jest przezroczysty.
 *
 * @param withPadding czy dodać margines wokół treści; `false` dla całych ekranów.
 * @param content podglądany komponent lub ekran.
 */
@Composable
fun KafePreview(
    withPadding: Boolean = true,
    content: @Composable () -> Unit,
) {
    KafeTheme {
        Box(
            modifier = Modifier
                .background(KafeTheme.colors.background)
                .then(if (withPadding) Modifier.padding(KafeTheme.spacing.l) else Modifier),
        ) {
            content()
        }
    }
}
