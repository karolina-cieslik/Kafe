package com.karolina.cieslik.kafe.feature.menu.presentation

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
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.layout
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

// KAFE-6: Ekran menu lokalu – bezstanowy (state + onAction), inspirowany ekranem Home z makiety:
// ciemny nagłówek z lokalem i wyszukiwarką, chipy kategorii, siatka produktów w 2 kolumnach.
// Bez wyszukiwania pokazuje wybraną kategorię; przy aktywnym wyszukiwaniu – wszystkie wyniki w sekcjach.
@OptIn(ExperimentalMaterial3Api::class)
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
            MenuUiState.Loading -> MenuGrid {
                header(cafeName = null, searchQuery = "", onAction = onAction)
                items(SKELETON_ITEMS) { ProductCardSkeleton() }
            }

            is MenuUiState.Error -> KafeErrorState(
                message = state.message,
                onRetry = { onAction(MenuAction.Retry) },
                modifier = Modifier.statusBarsPadding(),
            )

            is MenuUiState.Content -> PullToRefreshBox(
                isRefreshing = state.isRefreshing,
                onRefresh = { onAction(MenuAction.Refresh) },
                modifier = Modifier.fillMaxSize(),
            ) {
                MenuContent(state = state, onAction = onAction)
            }
        }
    }
}

@Composable
private fun MenuContent(
    state: MenuUiState.Content,
    onAction: (MenuAction) -> Unit,
) {
    val isSearching = state.searchQuery.isNotBlank()
    MenuGrid {
        header(cafeName = state.cafeName, searchQuery = state.searchQuery, onAction = onAction)
        when {
            state.categories.isEmpty() -> fullSpanItem(key = "empty") {
                if (isSearching) {
                    KafeEmptyState(
                        title = stringResource(Res.string.menu_search_empty_title),
                        message = stringResource(Res.string.menu_search_empty_message, state.searchQuery.trim()),
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

            isSearching -> state.categories.forEach { category ->
                fullSpanItem(key = "section-${category.id}") { SectionHeader(category.name) }
                products(category, onAction)
            }

            else -> {
                fullSpanItem(key = "chips") {
                    CategoryChips(
                        categories = state.categories,
                        selectedCategoryId = state.selectedCategoryId,
                        onSelect = { onAction(MenuAction.CategorySelected(it)) },
                    )
                }
                state.categories
                    .firstOrNull { it.id == state.selectedCategoryId }
                    ?.let { products(it, onAction) }
            }
        }
    }
}

@Composable
private fun MenuGrid(content: LazyGridScope.() -> Unit) {
    val spacing = KafeTheme.spacing
    val navigationBars = WindowInsets.navigationBars.asPaddingValues()
    LazyVerticalGrid(
        columns = GridCells.Fixed(GRID_COLUMNS),
        horizontalArrangement = Arrangement.spacedBy(spacing.m),
        verticalArrangement = Arrangement.spacedBy(spacing.m),
        contentPadding = PaddingValues(
            start = spacing.screenHorizontal,
            end = spacing.screenHorizontal,
            bottom = spacing.xl + navigationBars.calculateBottomPadding(),
        ),
        modifier = Modifier.fillMaxSize(),
        content = content,
    )
}

private fun LazyGridScope.fullSpanItem(key: String, content: @Composable () -> Unit) {
    item(key = key, span = { GridItemSpan(maxLineSpan) }) { content() }
}

private fun LazyGridScope.header(
    cafeName: String?,
    searchQuery: String,
    onAction: (MenuAction) -> Unit,
) {
    fullSpanItem(key = "header") {
        MenuHeader(
            cafeName = cafeName,
            searchQuery = searchQuery,
            onAction = onAction,
        )
    }
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
        )
    }
}

// Ciemny nagłówek rozciągnięty na całą szerokość mimo poziomego contentPadding siatki.
@Composable
private fun MenuHeader(
    cafeName: String?,
    searchQuery: String,
    onAction: (MenuAction) -> Unit,
) {
    val spacing = KafeTheme.spacing
    val colors = KafeTheme.extendedColors
    Column(
        modifier = Modifier
            .fullBleed(spacing.screenHorizontal)
            .background(Brush.verticalGradient(listOf(colors.header, colors.headerGradientEnd)))
            .statusBarsPadding()
            .padding(horizontal = spacing.screenHorizontal)
            .padding(top = spacing.l, bottom = spacing.xl),
    ) {
        CafeSelector(
            cafeName = cafeName,
            onClick = { onAction(MenuAction.ChangeCafeClicked) },
        )
        Spacer(Modifier.height(spacing.l))
        SearchField(
            query = searchQuery,
            onQueryChange = { onAction(MenuAction.SearchQueryChanged(it)) },
            placeholder = stringResource(Res.string.menu_search_placeholder),
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
        contentPadding = PaddingValues(horizontal = spacing.screenHorizontal),
        modifier = Modifier
            .fullBleed(spacing.screenHorizontal)
            .padding(top = spacing.xs),
    ) {
        items(items = categories, key = { it.id }) { category ->
            CategoryChip(
                label = category.name,
                selected = category.id == selectedCategoryId,
                onClick = { onSelect(category.id) },
            )
        }
    }
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

// Rozszerza element o [horizontal] z obu stron – pozwala nagłówkowi i chipom wyjść poza contentPadding siatki.
private fun Modifier.fullBleed(horizontal: Dp): Modifier = layout { measurable, constraints ->
    val extra = horizontal.roundToPx() * 2
    val placeable = measurable.measure(
        constraints.copy(
            minWidth = constraints.minWidth + extra,
            maxWidth = constraints.maxWidth + extra,
        ),
    )
    layout(constraints.maxWidth, placeable.height) {
        placeable.place(-extra / 2, 0)
    }
}
