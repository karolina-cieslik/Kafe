package com.karolina.cieslik.kafe.feature.menu.presentation

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import com.karolina.cieslik.kafe.core.presentation.designsystem.components.CategoryChip
import com.karolina.cieslik.kafe.core.presentation.designsystem.components.KafeEmptyState
import com.karolina.cieslik.kafe.core.presentation.designsystem.components.KafeErrorState
import com.karolina.cieslik.kafe.core.presentation.designsystem.components.ProductCard
import com.karolina.cieslik.kafe.core.presentation.designsystem.components.ProductCardSkeleton
import com.karolina.cieslik.kafe.core.presentation.designsystem.components.SearchField
import com.karolina.cieslik.kafe.core.presentation.designsystem.components.SearchFieldDefaults
import com.karolina.cieslik.kafe.core.presentation.designsystem.theme.KafeTheme
import kafe.app.shared.generated.resources.Res
import kafe.app.shared.generated.resources.ds_loading
import kafe.app.shared.generated.resources.ic_expand_more
import kafe.app.shared.generated.resources.ic_place
import kafe.app.shared.generated.resources.ic_search_off
import kafe.app.shared.generated.resources.menu_change_cafe
import kafe.app.shared.generated.resources.menu_empty_message
import kafe.app.shared.generated.resources.menu_empty_title
import kafe.app.shared.generated.resources.menu_location_label
import kafe.app.shared.generated.resources.menu_search_empty_message
import kafe.app.shared.generated.resources.menu_search_empty_title
import kafe.app.shared.generated.resources.menu_search_placeholder
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

private const val GRID_COLUMNS = 2
private const val SKELETON_ITEMS = 6
private const val SKELETON_CHIPS = 3

/** KAFE-6: Stabilne tagi ekranu menu dla testów UI (Robolectric i instrumentacyjnych). */
object MenuTestTags {
    const val LOADING = "menuLoading"
    const val GRID = "menuGrid"
    const val HEADER = "menuHeader"
    const val CATEGORIES = "menuCategories"
    fun product(id: String) = "menuProduct_$id"
    fun category(id: String) = "menuCategory_$id"
}

/**
 * KAFE-6: Ekran menu wybranego lokalu – bezstanowy, cały stan przychodzi w [state], a interakcje wychodzą
 * jako [MenuAction] przez [onAction]. Układ inspirowany ekranem Home z makiety.
 *
 * Nagłówek (lokal + wyszukiwarka) i chipy kategorii są przypięte; przewija się tylko siatka produktów,
 * a pull-to-refresh działa wyłącznie na niej. Stany:
 * - [MenuUiState.Loading] – nagłówek z nieaktywną wyszukiwarką, szkielety chipów i kart;
 * - [MenuUiState.Content] – bez wyszukiwania siatka pokazuje wybraną kategorię, przy aktywnym wyszukiwaniu
 *   wszystkie wyniki w sekcjach (chipy ukryte); brak kategorii = stan pustki (osobny dla wyszukiwania);
 * - [MenuUiState.Error] – pełnoekranowy błąd z „Spróbuj ponownie”, bez nagłówka.
 *
 * Akcje: [MenuAction.CategorySelected] (chip), [MenuAction.SearchQueryChanged] (każda zmiana tekstu),
 * [MenuAction.ProductClicked] (karta i „+”), [MenuAction.ChangeCafeClicked] (nazwa lokalu, przycisk w stanie
 * pustki), [MenuAction.Refresh] (pull-to-refresh), [MenuAction.Retry] (stan błędu).
 */
@Composable
fun MenuScreen(
    state: MenuUiState,
    onAction: (MenuAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(KafeTheme.colors.background),
    ) {
        when (state) {
            MenuUiState.Loading -> MenuLoading(onAction = onAction)

            // Bez nagłówka: przy błędzie nie znamy lokalu, a nieaktywna wyszukiwarka tylko by myliła.
            is MenuUiState.Error -> KafeErrorState(
                message = state.message,
                onRetry = { onAction(MenuAction.Retry) },
                modifier = Modifier.statusBarsPadding(),
            )

            is MenuUiState.Content -> MenuContent(state = state, onAction = onAction)
        }
    }
}

@Composable
private fun MenuLoading(onAction: (MenuAction) -> Unit) {
    val loadingDescription = stringResource(Res.string.ds_loading)
    Column(modifier = Modifier.fillMaxSize()) {
        MenuHeader(cafeName = null, searchQuery = "", searchEnabled = false, onAction = onAction)
        CategoryChipsSkeleton()
        MenuGrid(
            state = rememberLazyGridState(),
            modifier = Modifier
                .weight(1f)
                .testTag(MenuTestTags.LOADING)
                .semantics { contentDescription = loadingDescription },
        ) {
            items(SKELETON_ITEMS) { ProductCardSkeleton() }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MenuContent(
    state: MenuUiState.Content,
    onAction: (MenuAction) -> Unit,
) {
    val isSearching = state.searchQuery.isNotBlank()
    val showChips = !isSearching && state.categories.isNotEmpty()
    val gridState = rememberLazyGridState()
    val isGridScrolled by remember { derivedStateOf { gridState.canScrollBackward } }

    // Nowa kategoria / nowe wyniki wyszukiwania startują od góry. Klucz efektu to aktualna lista (kategoria
    // albo zapytanie), więc efekt rusza przy każdej jej zmianie. Ostatni klucz jest w rememberSaveable:
    // po odtworzeniu ekranu (obrót, powrót z tła, powrót ze szczegółów) efekt startuje znowu, ale klucz
    // się zgadza i pozycja przewinięcia zostaje.
    val listKey = if (isSearching) "search:${state.searchQuery.trim()}" else "category:${state.selectedCategoryId}"
    var lastListKey by rememberSaveable { mutableStateOf(listKey) }
    LaunchedEffect(listKey) {
        if (listKey != lastListKey) {
            lastListKey = listKey
            gridState.scrollToItem(0)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        MenuHeader(
            cafeName = state.cafeName,
            searchQuery = state.searchQuery,
            searchEnabled = true,
            onAction = onAction,
        )
        if (showChips) {
            CategoryChips(
                categories = state.categories,
                selectedCategoryId = state.selectedCategoryId,
                onSelect = { onAction(MenuAction.CategorySelected(it)) },
            )
        }
        ScrollEdgeDivider(visible = isGridScrolled)
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = { onAction(MenuAction.Refresh) },
            modifier = Modifier.weight(1f),
        ) {
            MenuGrid(
                state = gridState,
                topPadding = if (showChips) KafeTheme.spacing.xs else KafeTheme.spacing.l,
                modifier = Modifier.testTag(MenuTestTags.GRID),
            ) {
                when {
                    state.categories.isEmpty() -> fullSpanItem(key = "empty") {
                        MenuEmpty(searchQuery = state.searchQuery, onAction = onAction)
                    }

                    isSearching -> state.categories.forEach { category ->
                        fullSpanItem(key = "section-${category.id}") { SectionHeader(category.name) }
                        products(category, onAction)
                    }

                    else -> state.categories
                        .firstOrNull { it.id == state.selectedCategoryId }
                        ?.let { products(it, onAction) }
                }
            }
        }
    }
}

@Composable
private fun MenuEmpty(searchQuery: String, onAction: (MenuAction) -> Unit) {
    if (searchQuery.isNotBlank()) {
        KafeEmptyState(
            title = stringResource(Res.string.menu_search_empty_title),
            message = stringResource(Res.string.menu_search_empty_message, searchQuery.trim()),
            icon = Res.drawable.ic_search_off,
        )
    } else {
        KafeEmptyState(
            title = stringResource(Res.string.menu_empty_title),
            message = stringResource(Res.string.menu_empty_message),
            actionLabel = stringResource(Res.string.menu_change_cafe),
            onAction = { onAction(MenuAction.ChangeCafeClicked) },
        )
    }
}

@Composable
private fun MenuGrid(
    state: LazyGridState,
    modifier: Modifier = Modifier,
    topPadding: Dp = KafeTheme.spacing.xs,
    content: LazyGridScope.() -> Unit,
) {
    val spacing = KafeTheme.spacing
    val navigationBars = WindowInsets.navigationBars.asPaddingValues()
    LazyVerticalGrid(
        columns = GridCells.Fixed(GRID_COLUMNS),
        state = state,
        horizontalArrangement = Arrangement.spacedBy(spacing.m),
        verticalArrangement = Arrangement.spacedBy(spacing.m),
        contentPadding = PaddingValues(
            start = spacing.screenHorizontal,
            end = spacing.screenHorizontal,
            top = topPadding,
            bottom = spacing.xl + navigationBars.calculateBottomPadding(),
        ),
        modifier = modifier.fillMaxSize(),
        content = content,
    )
}

private fun LazyGridScope.fullSpanItem(key: String, content: @Composable () -> Unit) {
    item(key = key, span = { GridItemSpan(maxLineSpan) }) { content() }
}

private fun LazyGridScope.products(category: CategoryUi, onAction: (MenuAction) -> Unit) {
    items(items = category.products, key = { "${category.id}/${it.id}" }) { product ->
        ProductCard(
            name = product.name,
            subtitle = product.subtitle,
            priceLabel = product.priceLabel,
            imageUrl = product.imageUrl,
            isAvailable = product.isAvailable,
            onClick = { onAction(MenuAction.ProductClicked(product.id)) },
            onAddClick = { onAction(MenuAction.ProductClicked(product.id)) },
            modifier = Modifier.testTag(MenuTestTags.product(product.id)),
        )
    }
}

// Przypięty ciemny nagłówek: lokal + wyszukiwarka. Status bar wchodzi w jego tło.
@Composable
private fun MenuHeader(
    cafeName: String?,
    searchQuery: String,
    searchEnabled: Boolean,
    onAction: (MenuAction) -> Unit,
) {
    val spacing = KafeTheme.spacing
    val colors = KafeTheme.extendedColors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(MenuTestTags.HEADER)
            .background(Brush.verticalGradient(listOf(colors.header, colors.headerGradientEnd)))
            .statusBarsPadding()
            .padding(horizontal = spacing.screenHorizontal)
            .padding(top = spacing.m, bottom = spacing.l),
    ) {
        CafeSelector(
            cafeName = cafeName,
            onClick = { onAction(MenuAction.ChangeCafeClicked) },
        )
        Spacer(Modifier.height(spacing.m))
        SearchField(
            query = searchQuery,
            onQueryChange = { onAction(MenuAction.SearchQueryChanged(it)) },
            placeholder = stringResource(Res.string.menu_search_placeholder),
            enabled = searchEnabled,
            colors = SearchFieldDefaults.onHeader(),
        )
    }
}

@Composable
private fun CafeSelector(
    cafeName: String?,
    onClick: () -> Unit,
) {
    val spacing = KafeTheme.spacing
    val colors = KafeTheme.extendedColors
    Column(
        modifier = Modifier
            .clip(KafeTheme.shapes.small)
            .clickable(
                enabled = cafeName != null,
                onClickLabel = stringResource(Res.string.menu_change_cafe),
                onClick = onClick,
            )
            .padding(vertical = spacing.xs),
    ) {
        Text(
            text = stringResource(Res.string.menu_location_label),
            style = KafeTheme.typography.bodySmall,
            color = colors.onHeaderVariant,
        )
        Spacer(Modifier.height(spacing.xs))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(Res.drawable.ic_place),
                contentDescription = null,
                tint = KafeTheme.colors.primary,
                modifier = Modifier.size(spacing.iconSmall + spacing.xxs),
            )
            Spacer(Modifier.width(spacing.xs))
            if (cafeName != null) {
                Text(
                    text = cafeName,
                    style = KafeTheme.typography.titleSmall,
                    color = colors.onHeader,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .semantics { heading() },
                )
                Icon(
                    painter = painterResource(Res.drawable.ic_expand_more),
                    contentDescription = null,
                    tint = colors.onHeader,
                    modifier = Modifier.size(spacing.icon - spacing.xs),
                )
            } else {
                Box(
                    Modifier
                        .size(width = spacing.xxxl * 3, height = spacing.l)
                        .clip(KafeTheme.shapes.extraSmall)
                        .background(colors.headerField),
                )
            }
        }
    }
}

@Composable
private fun CategoryChips(
    categories: List<CategoryUi>,
    selectedCategoryId: String?,
    onSelect: (String) -> Unit,
) {
    val spacing = KafeTheme.spacing
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(spacing.s),
        contentPadding = PaddingValues(horizontal = spacing.screenHorizontal, vertical = spacing.m),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(MenuTestTags.CATEGORIES),
    ) {
        items(items = categories, key = { it.id }) { category ->
            CategoryChip(
                label = category.name,
                selected = category.id == selectedCategoryId,
                onClick = { onSelect(category.id) },
                modifier = Modifier.testTag(MenuTestTags.category(category.id)),
            )
        }
    }
}

// Zajmuje tyle miejsca co chipy, żeby po załadowaniu siatka nie przeskakiwała.
@Composable
private fun CategoryChipsSkeleton() {
    val spacing = KafeTheme.spacing
    Row(
        horizontalArrangement = Arrangement.spacedBy(spacing.s),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.screenHorizontal, vertical = spacing.m)
            .clearAndSetSemantics { },
    ) {
        repeat(SKELETON_CHIPS) {
            Box(
                Modifier
                    .size(width = spacing.xxxl * 2, height = spacing.chipHeight)
                    .clip(KafeTheme.shapes.medium)
                    .background(KafeTheme.colors.surface),
            )
        }
    }
}

// Separator pod przypiętą częścią – widoczny tylko, gdy siatka jest przewinięta (treść „pod” paskiem).
@Composable
private fun ScrollEdgeDivider(visible: Boolean) {
    val alpha by animateFloatAsState(targetValue = if (visible) 1f else 0f, label = "scrollEdgeDivider")
    HorizontalDivider(
        color = KafeTheme.colors.outlineVariant,
        modifier = Modifier.alpha(alpha),
    )
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = KafeTheme.typography.titleMedium,
        color = KafeTheme.colors.onBackground,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = KafeTheme.spacing.s)
            .semantics { heading() },
    )
}
