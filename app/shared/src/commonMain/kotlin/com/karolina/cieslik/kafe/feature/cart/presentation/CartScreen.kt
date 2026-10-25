package com.karolina.cieslik.kafe.feature.cart.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.ZeroCornerSize
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.karolina.cieslik.kafe.core.presentation.designsystem.components.KafeBadge
import com.karolina.cieslik.kafe.core.presentation.designsystem.components.KafeBadgeStyle
import com.karolina.cieslik.kafe.core.presentation.designsystem.components.KafeConfirmDialog
import com.karolina.cieslik.kafe.core.presentation.designsystem.components.KafeEmptyState
import com.karolina.cieslik.kafe.core.presentation.designsystem.components.KafeInfoBanner
import com.karolina.cieslik.kafe.core.presentation.designsystem.components.KafeLoadingState
import com.karolina.cieslik.kafe.core.presentation.designsystem.components.KafeStepper
import com.karolina.cieslik.kafe.core.presentation.designsystem.components.KafeTopBar
import com.karolina.cieslik.kafe.core.presentation.designsystem.components.ProductImage
import com.karolina.cieslik.kafe.core.presentation.designsystem.system.SystemBarsAppearance
import com.karolina.cieslik.kafe.core.presentation.designsystem.theme.KafeTheme
import kafe.app.shared.generated.resources.Res
import kafe.app.shared.generated.resources.cart_clear
import kafe.app.shared.generated.resources.cart_clear_dialog_message
import kafe.app.shared.generated.resources.cart_clear_dialog_title
import kafe.app.shared.generated.resources.cart_empty_message
import kafe.app.shared.generated.resources.cart_empty_title
import kafe.app.shared.generated.resources.cart_go_to_menu
import kafe.app.shared.generated.resources.cart_item_previous_price
import kafe.app.shared.generated.resources.cart_item_price_updated
import kafe.app.shared.generated.resources.cart_item_remove
import kafe.app.shared.generated.resources.cart_item_unavailable
import kafe.app.shared.generated.resources.cart_item_unit_price
import kafe.app.shared.generated.resources.cart_pickup_at
import kafe.app.shared.generated.resources.cart_prices_unverified
import kafe.app.shared.generated.resources.cart_summary_item_count
import kafe.app.shared.generated.resources.cart_summary_total
import kafe.app.shared.generated.resources.cart_summary_vat
import kafe.app.shared.generated.resources.cart_title
import kafe.app.shared.generated.resources.ic_delete
import kafe.app.shared.generated.resources.ic_place
import kafe.app.shared.generated.resources.ic_shopping_bag
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/** Stabilne tagi ekranu koszyka dla testów UI (Robolectric i instrumentacyjnych). */
object CartTestTags {
    /** Wskaźnik w stanie ładowania. */
    const val LOADING = "cartLoading"

    /** Lista pozycji w stanie treści. */
    const val LIST = "cartList"

    /** „Wyczyść” w górnym pasku. */
    const val CLEAR = "cartClear"

    /** Przypięte podsumowanie na dole ekranu. */
    const val SUMMARY = "cartSummary"

    /** Baner „Nie udało się sprawdzić aktualnych cen”. */
    const val PRICES_UNVERIFIED = "cartPricesUnverified"

    /** Wiersz pozycji o danym id. */
    fun item(id: Long) = "cartItem_$id"
}

/**
 * Koszyk – bezstanowy ekran: cały stan przychodzi w [state], interakcje wychodzą jako [CartAction] przez [onAction].
 *
 * Stany:
 * - [CartUiState.Loading] – wskaźnik ładowania pod górnym paskiem;
 * - [CartUiState.Empty] – „Twój koszyk jest pusty” z „Przejdź do menu”;
 * - [CartUiState.Content] – „Odbiór w: …” (gdy jest lokal), baner niesprawdzonych cen, lista pozycji
 *   i podsumowanie przypięte do dołu; „Wyczyść” w górnym pasku i dialog potwierdzenia, gdy
 *   `showClearConfirmation`. Pozycja niedostępna nie ma steppera – tylko kosz.
 *
 * Snackbary („Usunięto · Cofnij”, limit, błąd) pokazuje route w [snackbarHostState]; ekran tylko je układa nad
 * podsumowaniem.
 *
 * @param snackbarHostState host snackbara przekazywany z route.
 */
@Composable
fun CartScreen(
    state: CartUiState,
    onAction: (CartAction) -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    // Paski systemowe leżą na tle ekranu: na jasnym motywie ciemne ikony, na ciemnym jasne.
    SystemBarsAppearance(darkStatusBarIcons = KafeTheme.isLight, darkNavigationBarIcons = KafeTheme.isLight)
    Scaffold(
        topBar = {
            KafeTopBar(
                title = stringResource(Res.string.cart_title),
                onNavigateBack = { onAction(CartAction.BackClicked) },
                actions = {
                    if (state is CartUiState.Content) {
                        TextButton(
                            onClick = { onAction(CartAction.ClearClicked) },
                            colors = ButtonDefaults.textButtonColors(contentColor = KafeTheme.colors.primary),
                            modifier = Modifier.testTag(CartTestTags.CLEAR),
                        ) {
                            Text(text = stringResource(Res.string.cart_clear), style = KafeTheme.typography.labelLarge)
                        }
                    }
                },
            )
        },
        bottomBar = {
            if (state is CartUiState.Content) CartSummaryBar(summary = state.summary)
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = KafeTheme.colors.background,
        modifier = modifier,
    ) { innerPadding ->
        when (state) {
            CartUiState.Loading -> KafeLoadingState(
                modifier = Modifier
                    .padding(innerPadding)
                    .testTag(CartTestTags.LOADING),
            )

            CartUiState.Empty -> KafeEmptyState(
                title = stringResource(Res.string.cart_empty_title),
                message = stringResource(Res.string.cart_empty_message),
                icon = Res.drawable.ic_shopping_bag,
                actionLabel = stringResource(Res.string.cart_go_to_menu),
                onAction = { onAction(CartAction.GoToMenuClicked) },
                modifier = Modifier.padding(innerPadding),
            )

            is CartUiState.Content -> {
                CartContent(state = state, onAction = onAction, contentPadding = innerPadding)
                if (state.showClearConfirmation) {
                    KafeConfirmDialog(
                        title = stringResource(Res.string.cart_clear_dialog_title),
                        message = stringResource(Res.string.cart_clear_dialog_message),
                        confirmText = stringResource(Res.string.cart_clear),
                        onConfirm = { onAction(CartAction.ClearConfirmed) },
                        onDismiss = { onAction(CartAction.ClearDismissed) },
                        destructive = true,
                    )
                }
            }
        }
    }
}

@Composable
private fun CartContent(
    state: CartUiState.Content,
    onAction: (CartAction) -> Unit,
    contentPadding: PaddingValues,
) {
    val spacing = KafeTheme.spacing
    LazyColumn(
        contentPadding = PaddingValues(
            start = spacing.screenHorizontal,
            end = spacing.screenHorizontal,
            top = contentPadding.calculateTopPadding() + spacing.s,
            bottom = contentPadding.calculateBottomPadding() + spacing.l,
        ),
        modifier = Modifier
            .fillMaxSize()
            .testTag(CartTestTags.LIST),
    ) {
        state.cafeName?.let { cafeName ->
            item(key = "pickup") {
                PickupRow(cafeName = cafeName, modifier = Modifier.padding(bottom = spacing.m))
            }
        }
        if (state.pricesUnverified) {
            item(key = "pricesUnverified") {
                KafeInfoBanner(
                    text = stringResource(Res.string.cart_prices_unverified),
                    modifier = Modifier
                        .padding(bottom = spacing.m)
                        .testTag(CartTestTags.PRICES_UNVERIFIED),
                )
            }
        }
        itemsIndexed(state.items, key = { _, item -> item.id }) { index, item ->
            if (index > 0) HorizontalDivider(color = KafeTheme.colors.outlineVariant)
            CartItemRow(item = item, onAction = onAction)
        }
    }
}

/** Linia „Odbiór w: {lokal}” nad listą – tylko gdy lokal jest wybrany. */
@Composable
private fun PickupRow(cafeName: String, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(KafeTheme.spacing.xs),
        modifier = modifier.fillMaxWidth(),
    ) {
        Icon(
            painter = painterResource(Res.drawable.ic_place),
            contentDescription = null,
            tint = KafeTheme.colors.primary,
            modifier = Modifier.size(KafeTheme.spacing.iconSmall + KafeTheme.spacing.xxs),
        )
        Text(
            text = stringResource(Res.string.cart_pickup_at, cafeName),
            style = KafeTheme.typography.bodyMedium,
            color = KafeTheme.colors.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * Wiersz pozycji: miniatura, nazwa z koszem, rozmiar i modyfikatory, oznaczenie, stepper i cena.
 * Pozycja niedostępna ma szarą miniaturę i zamiast steppera tylko kosz – użytkownik może ją jedynie usunąć.
 */
@Composable
private fun CartItemRow(item: CartItemUi, onAction: (CartAction) -> Unit) {
    val spacing = KafeTheme.spacing
    val isUnavailable = item.status is CartItemStatusUi.Unavailable
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = spacing.m)
            .testTag(CartTestTags.item(item.id)),
    ) {
        ProductImage(
            imageUrl = item.imageUrl,
            contentDescription = null,
            grayscale = isUnavailable,
            modifier = Modifier
                .size(spacing.thumbnail)
                .clip(KafeTheme.shapes.medium),
        )
        Spacer(Modifier.width(spacing.m))
        Column(modifier = Modifier.weight(1f)) {
            Row {
                Column(modifier = Modifier.weight(1f).padding(top = spacing.xs)) {
                    Text(
                        text = item.name,
                        style = KafeTheme.typography.titleMedium,
                        color = if (isUnavailable) KafeTheme.colors.onSurfaceVariant else KafeTheme.colors.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = listOfNotNull(item.sizeLabel, item.modifiersLabel).joinToString(separator = " · "),
                        style = KafeTheme.typography.bodySmall,
                        color = KafeTheme.colors.onSurfaceVariant,
                    )
                }
                // Przesunięcie o wewnętrzny margines IconButtona: ikona kosza wyrównana do prawej krawędzi cen.
                IconButton(
                    onClick = { onAction(CartAction.RemoveClicked(item.id)) },
                    modifier = Modifier.offset(x = spacing.m),
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_delete),
                        contentDescription = stringResource(Res.string.cart_item_remove, item.name),
                        tint = KafeTheme.colors.onSurfaceVariant,
                        modifier = Modifier.size(spacing.icon - spacing.xxs),
                    )
                }
            }
            ItemStatus(status = item.status)
            Spacer(Modifier.height(spacing.s))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!isUnavailable) {
                    // „−” jest zawsze aktywny: przy 1 sztuce usuwa pozycję (z „Cofnij”), tak jak kosz.
                    KafeStepper(
                        value = item.quantity,
                        label = item.name,
                        onDecrease = { onAction(CartAction.DecreaseClicked(item.id)) },
                        onIncrease = { onAction(CartAction.IncreaseClicked(item.id)) },
                        canDecrease = true,
                        canIncrease = item.canIncrease,
                    )
                }
                Spacer(Modifier.weight(1f))
                ItemPrice(item = item, isUnavailable = isUnavailable)
            }
        }
    }
}

/** Oznaczenie pod nazwą: zaktualizowana cena (akcent) albo niedostępność (kolor błędu); [CartItemStatusUi.Ok] – nic. */
@Composable
private fun ItemStatus(status: CartItemStatusUi) {
    when (status) {
        CartItemStatusUi.Ok -> Unit
        is CartItemStatusUi.PriceUpdated -> KafeBadge(
            text = stringResource(Res.string.cart_item_price_updated),
            style = KafeBadgeStyle.Accent,
            modifier = Modifier.padding(top = KafeTheme.spacing.xs),
        )
        CartItemStatusUi.Unavailable -> Text(
            text = stringResource(Res.string.cart_item_unavailable),
            style = KafeTheme.typography.labelMedium,
            color = KafeTheme.colors.error,
            modifier = Modifier.padding(top = KafeTheme.spacing.xs),
        )
    }
}

/**
 * Ceny pozycji wyrównane do prawej: przekreślona poprzednia cena sztuki (po aktualizacji), cena sztuki (przy
 * więcej niż 1 sztuce albo po aktualizacji) i cena łączna.
 */
@Composable
private fun ItemPrice(item: CartItemUi, isUnavailable: Boolean) {
    val priceUpdated = item.status as? CartItemStatusUi.PriceUpdated
    Column(horizontalAlignment = Alignment.End) {
        if (priceUpdated != null) {
            val previousDescription = stringResource(Res.string.cart_item_previous_price, priceUpdated.previousUnitPriceLabel)
            Text(
                text = stringResource(Res.string.cart_item_unit_price, priceUpdated.previousUnitPriceLabel),
                style = KafeTheme.typography.bodySmall,
                color = KafeTheme.colors.onSurfaceVariant,
                textDecoration = TextDecoration.LineThrough,
                modifier = Modifier.clearAndSetSemantics { contentDescription = previousDescription },
            )
        }
        if (priceUpdated != null || item.quantity > 1) {
            Text(
                text = stringResource(Res.string.cart_item_unit_price, item.unitPriceLabel),
                style = KafeTheme.typography.bodySmall,
                color = KafeTheme.colors.onSurfaceVariant,
            )
        }
        Text(
            text = item.totalPriceLabel,
            style = KafeTheme.typography.titleMedium,
            color = if (isUnavailable) KafeTheme.colors.onSurfaceVariant else KafeTheme.colors.primary,
            maxLines = 1,
        )
    }
}

/**
 * Podsumowanie przypięte do dołu: „Razem do zapłaty” z kwotą, liczba produktów i „Ceny zawierają VAT”. Obie
 * wartości przychodzą policzone bez pozycji niedostępnych. TalkBack czyta całość jako jeden element.
 */
@Composable
private fun CartSummaryBar(summary: CartSummaryUi) {
    val spacing = KafeTheme.spacing
    val itemCount = pluralStringResource(Res.plurals.cart_summary_item_count, summary.itemCount, summary.itemCount)
    Surface(
        color = KafeTheme.colors.surface,
        shape = KafeTheme.shapes.extraLarge.copy(bottomStart = ZeroCornerSize, bottomEnd = ZeroCornerSize),
        shadowElevation = 8.dp,
        modifier = Modifier.testTag(CartTestTags.SUMMARY),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.l),
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = spacing.screenHorizontal, vertical = spacing.l)
                .semantics(mergeDescendants = true) {},
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(Res.string.cart_summary_total),
                    style = KafeTheme.typography.titleMedium,
                    color = KafeTheme.colors.onSurface,
                )
                Text(
                    text = "$itemCount · ${stringResource(Res.string.cart_summary_vat)}",
                    style = KafeTheme.typography.bodySmall,
                    color = KafeTheme.colors.onSurfaceVariant,
                )
            }
            Text(
                text = summary.payableTotalLabel,
                style = KafeTheme.typography.titleLarge,
                color = KafeTheme.colors.primary,
                maxLines = 1,
            )
        }
    }
}
