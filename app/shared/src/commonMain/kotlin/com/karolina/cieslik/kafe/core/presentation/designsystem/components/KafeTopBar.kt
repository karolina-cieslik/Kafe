package com.karolina.cieslik.kafe.core.presentation.designsystem.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.karolina.cieslik.kafe.core.presentation.designsystem.preview.KafePreview
import com.karolina.cieslik.kafe.core.presentation.designsystem.theme.KafeTheme
import kafe.app.shared.generated.resources.Res
import kafe.app.shared.generated.resources.ds_navigate_back
import kafe.app.shared.generated.resources.ic_arrow_back
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * KAFE-25: Górny pasek ekranów podrzędnych (szczegóły produktu, wybór lokalu, koszyk) z tytułem pośrodku.
 * Odstęp od paska statusu dodaje sam `CenterAlignedTopAppBar` (domyślne windowInsets).
 *
 * @param title tytuł ekranu; za długi jest ucinany wielokropkiem.
 * @param onNavigateBack wywoływane po stuknięciu strzałki wstecz; `null` = brak strzałki (np. ekran startowy).
 * @param actions ikony akcji po prawej stronie paska.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KafeTopBar(
    title: String,
    modifier: Modifier = Modifier,
    onNavigateBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    CenterAlignedTopAppBar(
        title = {
            Text(
                text = title,
                style = KafeTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        navigationIcon = {
            if (onNavigateBack != null) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_arrow_back),
                        contentDescription = stringResource(Res.string.ds_navigate_back),
                    )
                }
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = KafeTheme.colors.background,
            scrolledContainerColor = KafeTheme.colors.surface,
            titleContentColor = KafeTheme.colors.onBackground,
            navigationIconContentColor = KafeTheme.colors.onBackground,
            actionIconContentColor = KafeTheme.colors.onBackground,
        ),
        modifier = modifier,
    )
}

@PreviewLightDark
@Composable
private fun KafeTopBarPreview() {
    KafePreview(withPadding = false) {
        Column {
            KafeTopBar(title = "Szczegóły", onNavigateBack = {})
            KafeTopBar(title = "Zamówienie")
        }
    }
}
