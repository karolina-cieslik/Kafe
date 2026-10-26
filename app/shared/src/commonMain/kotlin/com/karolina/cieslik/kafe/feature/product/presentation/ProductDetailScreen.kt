package com.karolina.cieslik.kafe.feature.product.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.karolina.cieslik.kafe.core.presentation.designsystem.components.ChoiceOptionUi
import com.karolina.cieslik.kafe.core.presentation.designsystem.components.KafeBadge
import com.karolina.cieslik.kafe.core.presentation.designsystem.components.KafeBottomActionBar
import com.karolina.cieslik.kafe.core.presentation.designsystem.components.KafeChoiceGroup
import com.karolina.cieslik.kafe.core.presentation.designsystem.components.KafeErrorState
import com.karolina.cieslik.kafe.core.presentation.designsystem.components.KafeExpandableText
import com.karolina.cieslik.kafe.core.presentation.designsystem.components.KafeGroupHeader
import com.karolina.cieslik.kafe.core.presentation.designsystem.components.KafeInfoBanner
import com.karolina.cieslik.kafe.core.presentation.designsystem.components.KafeLoadingState
import com.karolina.cieslik.kafe.core.presentation.designsystem.components.KafeQuantityGroup
import com.karolina.cieslik.kafe.core.presentation.designsystem.components.KafeStepper
import com.karolina.cieslik.kafe.core.presentation.designsystem.components.KafeStepperSize
import com.karolina.cieslik.kafe.core.presentation.designsystem.components.KafeTopBar
import com.karolina.cieslik.kafe.core.presentation.designsystem.components.ProductImage
import com.karolina.cieslik.kafe.core.presentation.designsystem.components.QuantityOptionUi
import com.karolina.cieslik.kafe.core.presentation.designsystem.components.SizeOptionUi
import com.karolina.cieslik.kafe.core.presentation.designsystem.components.SizeSelector
import com.karolina.cieslik.kafe.core.presentation.designsystem.system.SystemBarsAppearance
import com.karolina.cieslik.kafe.core.presentation.designsystem.theme.KafeTheme
import kafe.app.shared.generated.resources.Res
import kafe.app.shared.generated.resources.ds_product_unavailable
import kafe.app.shared.generated.resources.product_detail_add_to_cart
import kafe.app.shared.generated.resources.product_detail_quantity
import kafe.app.shared.generated.resources.product_detail_size
import kafe.app.shared.generated.resources.product_detail_title
import org.jetbrains.compose.resources.stringResource

/** Stabilne tagi ekranu szczegółów produktu dla testów UI (Robolectric i instrumentacyjnych). */
object ProductDetailTestTags {
    /** Wskaźnik w stanie ładowania. */
    const val LOADING = "productDetailLoading"

    /** Przewijana treść w stanie treści. */
    const val CONTENT = "productDetailContent"

    /** Przycisk „Dodaj do koszyka” w dolnym pasku. */
    const val ADD_TO_CART = "productDetailAddToCart"

    /** Baner z informacją (np. o niedostępności produktu). */
    const val INFO = "productDetailInfo"

    /** Opcja rozmiaru o danym kodzie, np. „M”. */
    fun size(code: String) = "productDetailSize_$code"

    /** Wiersz opcji jednokrotnego wyboru w danej grupie modyfikatorów. */
    fun option(groupId: String, optionId: String) = "productDetailOption_${groupId}_$optionId"
}

/**
 * Szczegóły produktu – bezstanowy ekran: cały stan przychodzi w [state], interakcje wychodzą jako
 * [ProductDetailAction] przez [onAction]. Układ inspirowany ekranem „Detail” z makiety: duże zdjęcie, nazwa,
 * zwijany opis, rozmiary, modyfikatory, liczba sztuk i przypięty dolny pasek z ceną łączną.
 *
 * Stany:
 * - [ProductDetailUiState.Loading] – wskaźnik ładowania pod górnym paskiem;
 * - [ProductDetailUiState.Content] – formularz produktu; sekcja rozmiaru tylko przy więcej niż jednym rozmiarze;
 *   produkt niedostępny: zdjęcie w skali szarości, etykieta „Chwilowo niedostępny” i wszystkie opcje
 *   wyłączone; aktywność „Dodaj do koszyka” zależy od `canAddToCart` ze stanu;
 * - [ProductDetailUiState.Error] – pełnoekranowy błąd z „Spróbuj ponownie”.
 *
 * Akcje: [ProductDetailAction.SizeSelected], [ProductDetailAction.OptionSelected],
 * [ProductDetailAction.OptionQuantityChanged] i [ProductDetailAction.QuantityChanged] (zmiany formularza),
 * [ProductDetailAction.AddToCartClicked] (dolny pasek), [ProductDetailAction.BackClicked] (strzałka wstecz),
 * [ProductDetailAction.Retry] (stan błędu).
 *
 * @param snackbarHostState host snackbara; route przekazuje własny, żeby pokazać „Dodano do koszyka: …”.
 */
@Composable
fun ProductDetailScreen(
    state: ProductDetailUiState,
    onAction: (ProductDetailAction) -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    // Paski systemowe leżą na tle ekranu: na jasnym motywie ciemne ikony, na ciemnym jasne.
    SystemBarsAppearance(darkStatusBarIcons = KafeTheme.isLight, darkNavigationBarIcons = KafeTheme.isLight)
    Scaffold(
        topBar = {
            KafeTopBar(
                title = stringResource(Res.string.product_detail_title),
                onNavigateBack = { onAction(ProductDetailAction.BackClicked) },
            )
        },
        bottomBar = {
            if (state is ProductDetailUiState.Content) {
                KafeBottomActionBar(
                    priceLabel = state.totalPriceLabel,
                    actionText = stringResource(Res.string.product_detail_add_to_cart),
                    onActionClick = { onAction(ProductDetailAction.AddToCartClicked) },
                    actionEnabled = state.canAddToCart,
                    actionTestTag = ProductDetailTestTags.ADD_TO_CART,
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = KafeTheme.colors.background,
        modifier = modifier,
    ) { innerPadding ->
        when (state) {
            ProductDetailUiState.Loading -> KafeLoadingState(
                modifier = Modifier
                    .padding(innerPadding)
                    .testTag(ProductDetailTestTags.LOADING),
            )

            is ProductDetailUiState.Error -> KafeErrorState(
                message = state.message,
                onRetry = { onAction(ProductDetailAction.Retry) },
                modifier = Modifier.padding(innerPadding),
            )

            is ProductDetailUiState.Content -> ProductDetailContent(
                state = state,
                onAction = onAction,
                contentPadding = innerPadding,
            )
        }
    }
}

@Composable
private fun ProductDetailContent(
    state: ProductDetailUiState.Content,
    onAction: (ProductDetailAction) -> Unit,
    contentPadding: PaddingValues,
) {
    val spacing = KafeTheme.spacing
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding)
            .padding(horizontal = spacing.screenHorizontal)
            .padding(bottom = spacing.xl)
            .testTag(ProductDetailTestTags.CONTENT),
    ) {
        ProductImage(
            imageUrl = state.imageUrl,
            contentDescription = null,
            grayscale = !state.isAvailable,
            modifier = Modifier
                .fillMaxWidth()
                .height(spacing.heroImageHeight)
                .clip(KafeTheme.shapes.large),
        )
        Spacer(Modifier.height(spacing.l))
        Text(
            text = state.name,
            style = KafeTheme.typography.headlineSmall,
            color = KafeTheme.colors.onBackground,
            modifier = Modifier.semantics { heading() },
        )
        if (!state.isAvailable) {
            Spacer(Modifier.height(spacing.s))
            KafeBadge(text = stringResource(Res.string.ds_product_unavailable))
        }
        state.description?.let {
            Spacer(Modifier.height(spacing.s))
            KafeExpandableText(text = it)
        }
        state.infoMessage?.let {
            Spacer(Modifier.height(spacing.m))
            KafeInfoBanner(text = it, modifier = Modifier.testTag(ProductDetailTestTags.INFO))
        }

        if (state.sizes.size > 1) {
            SectionDivider()
            KafeGroupHeader(title = stringResource(Res.string.product_detail_size))
            Spacer(Modifier.height(spacing.s))
            SizeSelector(
                options = state.sizes.map { size ->
                    SizeOptionUi(
                        id = size.code,
                        label = size.label,
                        volumeLabel = size.volumeLabel,
                        priceLabel = size.priceLabel,
                        isAvailable = size.isAvailable && state.isAvailable,
                    )
                },
                selectedId = state.sizes.firstOrNull { it.isSelected }?.code,
                onSelect = { onAction(ProductDetailAction.SizeSelected(it)) },
                optionTestTag = ProductDetailTestTags::size,
            )
        }

        state.modifierGroups.forEach { group ->
            SectionDivider()
            ModifierGroup(group = group, isProductAvailable = state.isAvailable, onAction = onAction)
        }

        SectionDivider()
        QuantityRow(
            quantity = state.quantity,
            canIncrease = state.isAvailable && state.canIncreaseQuantity,
            canDecrease = state.isAvailable && state.canDecreaseQuantity,
            onChange = { onAction(ProductDetailAction.QuantityChanged(it)) },
        )
    }
}

/**
 * Grupa modyfikatorów w komponencie DS zależnym od typu: jednokrotny wybór albo liczniki.
 * Przy niedostępnym produkcie wszystkie opcje są wyłączone.
 */
@Composable
private fun ModifierGroup(
    group: ModifierGroupUi,
    isProductAvailable: Boolean,
    onAction: (ProductDetailAction) -> Unit,
) {
    when (group.type) {
        ModifierGroupTypeUi.SINGLE -> KafeChoiceGroup(
            title = group.name,
            isRequired = group.isRequired,
            options = group.options.map { option ->
                ChoiceOptionUi(
                    id = option.id,
                    label = option.name,
                    trailingLabel = option.surchargeLabel,
                    isEnabled = option.isAvailable && isProductAvailable,
                    isSelected = option.isSelected,
                )
            },
            onOptionClick = { onAction(ProductDetailAction.OptionSelected(group.id, it)) },
            optionTestTag = { ProductDetailTestTags.option(group.id, it) },
        )

        ModifierGroupTypeUi.QUANTITY -> KafeQuantityGroup(
            title = group.name,
            isRequired = group.isRequired,
            limitLabel = group.limitLabel,
            options = group.options.map { option ->
                QuantityOptionUi(
                    id = option.id,
                    label = option.name,
                    trailingLabel = option.surchargeLabel,
                    quantity = option.quantity,
                    canIncrease = option.canIncrease,
                    canDecrease = option.canDecrease,
                    isEnabled = option.isAvailable && isProductAvailable,
                )
            },
            onQuantityChange = { optionId, delta ->
                onAction(ProductDetailAction.OptionQuantityChanged(group.id, optionId, delta))
            },
        )
    }
}

/**
 * Wiersz liczby sztuk – nagłówek i duży stepper. Limity (1–20 sztuk) liczy ViewModel; ekran tylko pokazuje
 * [canIncrease] / [canDecrease] ze stanu, żeby reguła była w jednym miejscu.
 */
@Composable
private fun QuantityRow(
    quantity: Int,
    canIncrease: Boolean,
    canDecrease: Boolean,
    onChange: (delta: Int) -> Unit,
) {
    val label = stringResource(Res.string.product_detail_quantity)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        KafeGroupHeader(title = label, modifier = Modifier.weight(1f))
        KafeStepper(
            value = quantity,
            label = label,
            onDecrease = { onChange(-1) },
            onIncrease = { onChange(+1) },
            canDecrease = canDecrease,
            canIncrease = canIncrease,
            size = KafeStepperSize.Large,
        )
    }
}

/** Separator sekcji formularza z odstępem nad i pod nim. */
@Composable
private fun SectionDivider() {
    HorizontalDivider(
        color = KafeTheme.colors.outlineVariant,
        modifier = Modifier.padding(vertical = KafeTheme.spacing.l),
    )
}
