package com.karolina.cieslik.kafe.feature.cafe.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.karolina.cieslik.kafe.core.presentation.designsystem.components.KafeBadge
import com.karolina.cieslik.kafe.core.presentation.designsystem.components.KafeBadgeStyle
import com.karolina.cieslik.kafe.core.presentation.designsystem.components.KafeEmptyState
import com.karolina.cieslik.kafe.core.presentation.designsystem.components.KafeErrorState
import com.karolina.cieslik.kafe.core.presentation.designsystem.components.KafeLoadingState
import com.karolina.cieslik.kafe.core.presentation.designsystem.components.KafeTopBar
import com.karolina.cieslik.kafe.core.presentation.designsystem.system.StatusBarAppearance
import com.karolina.cieslik.kafe.core.presentation.designsystem.theme.KafeTheme
import kafe.app.shared.generated.resources.Res
import kafe.app.shared.generated.resources.cafe_picker_closed
import kafe.app.shared.generated.resources.cafe_picker_empty_message
import kafe.app.shared.generated.resources.cafe_picker_empty_title
import kafe.app.shared.generated.resources.cafe_picker_open
import kafe.app.shared.generated.resources.cafe_picker_selected
import kafe.app.shared.generated.resources.cafe_picker_title
import kafe.app.shared.generated.resources.cafe_picker_welcome_subtitle
import kafe.app.shared.generated.resources.ic_check
import kafe.app.shared.generated.resources.ic_place
import kafe.app.shared.generated.resources.ic_schedule
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

// KAFE-29: Stabilne tagi dla testów UI.
object CafePickerTestTags {
    const val LOADING = "cafePickerLoading"
    const val LIST = "cafePickerList"
    const val WELCOME = "cafePickerWelcome"
    fun cafe(id: String) = "cafePickerCafe_$id"
}

// KAFE-29: Wybór lokalu – bezstanowy ekran (state + onAction). Lista kart lokali z godzinami i statusem
// „Otwarte/Zamknięte”; wybrany lokal jest zaznaczony. Przy pierwszym uruchomieniu (isFirstLaunch) zamiast
// paska ze strzałką wstecz jest nagłówek powitalny – użytkownik musi wybrać lokal.
// Miejsce na przełącznik lista/mapa (KAFE-30) jest pod nagłówkiem, nad listą – patrz [CafePickerContent].
@Composable
fun CafePickerScreen(
    state: CafePickerUiState,
    onAction: (CafePickerAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    StatusBarAppearance(darkIcons = KafeTheme.colors.background.luminance() > 0.5f)
    val isFirstLaunch = (state as? CafePickerUiState.Content)?.isFirstLaunch == true
    Scaffold(
        topBar = {
            if (isFirstLaunch) {
                WelcomeHeader()
            } else {
                KafeTopBar(
                    title = stringResource(Res.string.cafe_picker_title),
                    onNavigateBack = { onAction(CafePickerAction.BackClicked) },
                )
            }
        },
        containerColor = KafeTheme.colors.background,
        modifier = modifier,
    ) { innerPadding ->
        when (state) {
            CafePickerUiState.Loading -> KafeLoadingState(
                modifier = Modifier
                    .padding(innerPadding)
                    .testTag(CafePickerTestTags.LOADING),
            )

            is CafePickerUiState.Error -> KafeErrorState(
                message = state.message,
                onRetry = { onAction(CafePickerAction.Retry) },
                modifier = Modifier.padding(innerPadding),
            )

            is CafePickerUiState.Content -> CafePickerContent(
                state = state,
                onAction = onAction,
                contentPadding = innerPadding,
            )
        }
    }
}

@Composable
private fun CafePickerContent(
    state: CafePickerUiState.Content,
    onAction: (CafePickerAction) -> Unit,
    contentPadding: PaddingValues,
) {
    if (state.cafes.isEmpty()) {
        KafeEmptyState(
            title = stringResource(Res.string.cafe_picker_empty_title),
            message = stringResource(Res.string.cafe_picker_empty_message),
            modifier = Modifier.padding(contentPadding),
        )
        return
    }
    val spacing = KafeTheme.spacing
    val navigationBars = WindowInsets.navigationBars.asPaddingValues()
    // KAFE-30: tu wejdzie przełącznik lista/mapa (Column { Toggle; Lista albo Mapa }).
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(spacing.m),
        contentPadding = PaddingValues(
            start = spacing.screenHorizontal,
            end = spacing.screenHorizontal,
            top = contentPadding.calculateTopPadding() + spacing.s,
            bottom = spacing.xl + navigationBars.calculateBottomPadding(),
        ),
        modifier = Modifier
            .fillMaxSize()
            .selectableGroup()
            .testTag(CafePickerTestTags.LIST),
    ) {
        items(items = state.cafes, key = { it.id }) { cafe ->
            CafeCard(
                cafe = cafe,
                onClick = { onAction(CafePickerAction.CafeSelected(cafe.id)) },
                modifier = Modifier.testTag(CafePickerTestTags.cafe(cafe.id)),
            )
        }
    }
}

@Composable
private fun WelcomeHeader() {
    val spacing = KafeTheme.spacing
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = spacing.screenHorizontal)
            .padding(top = spacing.xl, bottom = spacing.l)
            .testTag(CafePickerTestTags.WELCOME),
    ) {
        Text(
            text = stringResource(Res.string.cafe_picker_title),
            style = KafeTheme.typography.headlineMedium,
            color = KafeTheme.colors.onBackground,
            modifier = Modifier.semantics { heading() },
        )
        Spacer(Modifier.height(spacing.s))
        Text(
            text = stringResource(Res.string.cafe_picker_welcome_subtitle),
            style = KafeTheme.typography.bodyMedium,
            color = KafeTheme.colors.onSurfaceVariant,
        )
    }
}

@Composable
private fun CafeCard(
    cafe: CafeUi,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = KafeTheme.spacing
    val selectedDescription = stringResource(Res.string.cafe_picker_selected)
    Surface(
        selected = cafe.isSelected,
        onClick = onClick,
        shape = KafeTheme.shapes.large,
        color = KafeTheme.colors.surface,
        border = BorderStroke(
            width = if (cafe.isSelected) 2.dp else 1.dp,
            color = if (cafe.isSelected) KafeTheme.colors.primary else KafeTheme.colors.outlineVariant,
        ),
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                role = Role.RadioButton
                if (cafe.isSelected) stateDescription = selectedDescription
            },
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            modifier = Modifier.padding(spacing.l),
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(spacing.xs),
                modifier = Modifier.weight(1f),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(spacing.s),
                ) {
                    Text(
                        text = cafe.name,
                        style = KafeTheme.typography.titleMedium,
                        color = KafeTheme.colors.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    KafeBadge(
                        text = stringResource(if (cafe.isOpenNow) Res.string.cafe_picker_open else Res.string.cafe_picker_closed),
                        style = if (cafe.isOpenNow) KafeBadgeStyle.Accent else KafeBadgeStyle.Muted,
                    )
                }
                Spacer(Modifier.height(spacing.xxs))
                CafeInfoLine(icon = Res.drawable.ic_place, text = cafe.address)
                CafeInfoLine(icon = Res.drawable.ic_schedule, text = cafe.openingHoursLabel)
            }
            if (cafe.isSelected) {
                Spacer(Modifier.width(spacing.s))
                Icon(
                    painter = painterResource(Res.drawable.ic_check),
                    contentDescription = null,
                    tint = KafeTheme.colors.primary,
                    modifier = Modifier.size(spacing.icon),
                )
            }
        }
    }
}

@Composable
private fun CafeInfoLine(icon: DrawableResource, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = KafeTheme.colors.onSurfaceVariant,
            modifier = Modifier.size(KafeTheme.spacing.iconSmall),
        )
        Spacer(Modifier.width(KafeTheme.spacing.s))
        Text(
            text = text,
            style = KafeTheme.typography.bodyMedium,
            color = KafeTheme.colors.onSurfaceVariant,
        )
    }
}
