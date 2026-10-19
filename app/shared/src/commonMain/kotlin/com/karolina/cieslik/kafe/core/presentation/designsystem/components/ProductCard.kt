package com.karolina.cieslik.kafe.core.presentation.designsystem.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.karolina.cieslik.kafe.core.presentation.designsystem.preview.KafePreview
import com.karolina.cieslik.kafe.core.presentation.designsystem.theme.KafeTheme
import kafe.app.shared.generated.resources.Res
import kafe.app.shared.generated.resources.ds_add_product
import kafe.app.shared.generated.resources.ds_product_unavailable
import kafe.app.shared.generated.resources.ic_add
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

private const val UNAVAILABLE_CONTENT_ALPHA = 0.5f

/**
 * Karta produktu w siatce menu – zdjęcie, nazwa, podtytuł, cena i przycisk „+”.
 *
 * Niedostępny produkt ([isAvailable] = `false`): zdjęcie w skali szarości, przygaszona treść, etykieta
 * „Chwilowo niedostępny” i brak „+”. Karta nadal jest klikalna, żeby dało się podejrzeć szczegóły.
 *
 * @param name nazwa produktu; za długa jest ucinana wielokropkiem.
 * @param subtitle krótki opis pod nazwą; `null` zostawia pustą linię, żeby karty w siatce miały równą wysokość.
 * @param priceLabel sformatowana cena, np. „12,00 zł” (bez „od”).
 * @param imageUrl adres zdjęcia; `null` = placeholder z ikoną kawy.
 * @param isAvailable czy produkt można teraz zamówić w wybranym lokalu.
 * @param onClick wywoływane po stuknięciu karty (przejście do szczegółów).
 * @param onAddClick wywoływane po stuknięciu „+”; tylko dla dostępnego produktu.
 */
@Composable
fun ProductCard(
    name: String,
    subtitle: String?,
    priceLabel: String,
    imageUrl: String?,
    isAvailable: Boolean,
    onClick: () -> Unit,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val unavailableLabel = stringResource(Res.string.ds_product_unavailable)
    val contentAlpha = if (isAvailable) 1f else UNAVAILABLE_CONTENT_ALPHA
    Surface(
        onClick = onClick,
        shape = KafeTheme.shapes.large,
        color = KafeTheme.colors.surface,
        modifier = modifier,
    ) {
        Column(modifier = Modifier.padding(KafeTheme.spacing.s)) {
            Box {
                ProductImage(
                    imageUrl = imageUrl,
                    contentDescription = null,
                    grayscale = !isAvailable,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(KafeTheme.spacing.cardImageHeight)
                        .clip(KafeTheme.shapes.medium)
                        .alpha(contentAlpha),
                )
                if (!isAvailable) {
                    KafeBadge(
                        text = unavailableLabel,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(KafeTheme.spacing.s),
                    )
                }
            }
            Spacer(Modifier.height(KafeTheme.spacing.m))
            Column(
                modifier = Modifier
                    .padding(horizontal = KafeTheme.spacing.xs)
                    .alpha(contentAlpha),
            ) {
                Text(
                    text = name,
                    style = KafeTheme.typography.titleMedium,
                    color = KafeTheme.colors.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(KafeTheme.spacing.xs))
                Text(
                    // Pusty tekst trzyma stałą wysokość karty, żeby siatka się nie rozjeżdżała.
                    text = subtitle.orEmpty(),
                    style = KafeTheme.typography.bodySmall,
                    color = KafeTheme.colors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = KafeTheme.spacing.xs),
            ) {
                Text(
                    text = priceLabel,
                    style = KafeTheme.typography.titleLarge,
                    color = KafeTheme.colors.onSurface,
                    maxLines = 1,
                    modifier = Modifier
                        .weight(1f)
                        .alpha(contentAlpha),
                )
                if (isAvailable) {
                    AddButton(
                        contentDescription = stringResource(Res.string.ds_add_product, name),
                        onClick = onAddClick,
                    )
                } else {
                    // Zachowuje wysokość wiersza taką jak z przyciskiem „+”.
                    Spacer(Modifier.size(KafeTheme.spacing.addButton))
                }
            }
        }
    }
}

@Composable
private fun AddButton(
    contentDescription: String,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = KafeTheme.shapes.small,
        color = KafeTheme.colors.primary,
        contentColor = KafeTheme.colors.onPrimary,
        // Hit-test Compose sam powiększa obszar dotyku małych klikalnych elementów do 48dp.
        modifier = Modifier.size(KafeTheme.spacing.addButton),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                painter = painterResource(Res.drawable.ic_add),
                contentDescription = contentDescription,
                modifier = Modifier.size(KafeTheme.spacing.iconSmall + KafeTheme.spacing.xs),
            )
        }
    }
}

/**
 * Szkielet karty produktu na czas pierwszego ładowania menu – ten sam układ co [ProductCard],
 * z pulsującą przezroczystością. Ukryty przed TalkBackiem (stan ładowania ogłasza ekran).
 */
@Composable
fun ProductCardSkeleton(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "skeleton")
    val alpha by transition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 800), RepeatMode.Reverse),
        label = "skeletonAlpha",
    )
    val block = Modifier
        .clip(KafeTheme.shapes.small)
        .background(KafeTheme.colors.surfaceVariant)
    Surface(
        shape = KafeTheme.shapes.large,
        color = KafeTheme.colors.surface,
        modifier = modifier.clearAndSetSemantics { },
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(KafeTheme.spacing.s),
            modifier = Modifier
                .padding(KafeTheme.spacing.s)
                .alpha(alpha),
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(KafeTheme.spacing.cardImageHeight)
                    .clip(KafeTheme.shapes.medium)
                    .background(KafeTheme.colors.surfaceVariant),
            )
            Box(block.fillMaxWidth(0.7f).height(KafeTheme.spacing.iconSmall))
            Box(block.fillMaxWidth(0.5f).height(KafeTheme.spacing.m))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Box(block.width(KafeTheme.spacing.xxxl + KafeTheme.spacing.l).height(KafeTheme.spacing.l + KafeTheme.spacing.xs))
                Box(block.size(KafeTheme.spacing.addButton))
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun ProductCardPreview() {
    KafePreview {
        Row(horizontalArrangement = Arrangement.spacedBy(KafeTheme.spacing.m)) {
            ProductCard(
                name = "Cappuccino",
                subtitle = "z mlekiem owsianym",
                priceLabel = "14,00 zł",
                imageUrl = null,
                isAvailable = true,
                onClick = {},
                onAddClick = {},
                modifier = Modifier.width(170.dp),
            )
            ProductCard(
                name = "Flat white z bardzo długą nazwą",
                subtitle = null,
                priceLabel = "16,50 zł",
                imageUrl = null,
                isAvailable = false,
                onClick = {},
                onAddClick = {},
                modifier = Modifier.width(170.dp),
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun ProductCardSkeletonPreview() {
    KafePreview {
        ProductCardSkeleton(modifier = Modifier.width(170.dp))
    }
}
