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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.karolina.cieslik.kafe.core.presentation.designsystem.system.SystemBarsAppearance
import com.karolina.cieslik.kafe.core.presentation.designsystem.theme.KafeTheme
import kafe.app.shared.generated.resources.Res
import kafe.app.shared.generated.resources.cafe_picker_closed
import kafe.app.shared.generated.resources.cafe_picker_empty_message
import kafe.app.shared.generated.resources.cafe_picker_empty_title
import kafe.app.shared.generated.resources.cafe_picker_open
import kafe.app.shared.generated.resources.cafe_picker_selected
import kafe.app.shared.generated.resources.cafe_picker_title
import kafe.app.shared.generated.resources.cafe_picker_welcome_subtitle
import kafe.app.shared.generated.resources.ds_retry
import kafe.app.shared.generated.resources.ic_check
import kafe.app.shared.generated.resources.ic_place
import kafe.app.shared.generated.resources.ic_schedule
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/** Stabilne tagi ekranu wyboru lokalu dla testów UI (Robolectric i instrumentacyjnych). */
object CafePickerTestTags {
    /** Wskaźnik w stanie ładowania. */
    const val LOADING = "cafePickerLoading"

    /** Lista kart lokali w stanie treści. */
    const val LIST = "cafePickerList"

    /** Nagłówek powitalny, gdy picker jest ekranem startowym (zamiast górnego paska ze strzałką wstecz). */
    const val WELCOME = "cafePickerWelcome"

    /** Karta lokalu o danym id. */
    fun cafe(id: String) = "cafePickerCafe_$id"
}

/**
 * Ekran wyboru lokalu – bezstanowy, rysuje [state] i zgłasza intencje przez [onAction].
 *
 * Stany:
 * - [CafePickerUiState.Loading] – wskaźnik ładowania pod górnym paskiem;
 * - [CafePickerUiState.Content] – lista kart lokali z adresem, godzinami na dziś i odznaką „Otwarte”/„Zamknięte”;
 *   wybrany lokal ma obramowanie w kolorze `primary` i znacznik; pusta lista – komunikat „Brak kawiarni”
 *   z „Spróbuj ponownie”;
 * - [CafePickerUiState.Error] – pełnoekranowy błąd z „Spróbuj ponownie”.
 *
 * Gdy nie ma dokąd wrócić (picker jest ekranem startowym przy pierwszym uruchomieniu), we wszystkich stanach
 * zamiast paska ze strzałką wstecz jest nagłówek powitalny – użytkownik musi wybrać lokal, zanim zobaczy menu.
 *
 * Akcje: [CafePickerAction.CafeSelected] (karta lokalu), [CafePickerAction.BackClicked] (strzałka wstecz),
 * [CafePickerAction.Retry] (stan błędu).
 *
 * Przełącznik lista/mapa wejdzie pod nagłówek, nad listę (w [CafePickerContent]).
 *
 * @param canNavigateBack `true`, gdy pod pickerem jest inny ekran (wejście z menu) – wtedy pasek ze strzałką
 * wstecz; `false` – nagłówek powitalny bez strzałki. Wynika ze stosu nawigacji, nie ze stanu VM, bo przy
 * pierwszym uruchomieniu ekran startuje w Loading/Error, zanim VM wie cokolwiek o wyborze.
 * @param snackbarHostState host snackbara; route przekazuje własny, żeby pokazać błąd zapisu wyboru lokalu.
 */
@Composable
fun CafePickerScreen(
    state: CafePickerUiState,
    onAction: (CafePickerAction) -> Unit,
    canNavigateBack: Boolean,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    // Paski systemowe leżą na tle ekranu (pasek i nagłówek powitalny mają kolor tła): na jasnym motywie ciemne
    // ikony, na ciemnym jasne.
    SystemBarsAppearance(darkStatusBarIcons = KafeTheme.isLight, darkNavigationBarIcons = KafeTheme.isLight)
    Scaffold(
        topBar = {
            if (canNavigateBack) {
                KafeTopBar(
                    title = stringResource(Res.string.cafe_picker_title),
                    onNavigateBack = { onAction(CafePickerAction.BackClicked) },
                )
            } else {
                WelcomeHeader()
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
            actionLabel = stringResource(Res.string.ds_retry),
            onAction = { onAction(CafePickerAction.Retry) },
            modifier = Modifier.padding(contentPadding),
        )
        return
    }
    val spacing = KafeTheme.spacing
    val navigationBars = WindowInsets.navigationBars.asPaddingValues()
    // TODO(KAFE-30): przełącznik lista/mapa nad listą (Column { Toggle; Lista albo Mapa }).
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
