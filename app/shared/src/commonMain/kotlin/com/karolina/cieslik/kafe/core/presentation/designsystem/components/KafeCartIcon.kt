package com.karolina.cieslik.kafe.core.presentation.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.karolina.cieslik.kafe.core.presentation.designsystem.preview.KafePreview
import com.karolina.cieslik.kafe.core.presentation.designsystem.theme.KafeTheme
import kafe.app.shared.generated.resources.Res
import kafe.app.shared.generated.resources.ds_cart_empty
import kafe.app.shared.generated.resources.ds_cart_item_count
import kafe.app.shared.generated.resources.ic_shopping_bag
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/** Największa liczba pokazywana w liczniku; powyżej licznik pokazuje „99+”, żeby zmieścił się na ikonie. */
internal const val MAX_BADGE_COUNT = 99

/** Tekst licznika dla [count]: `null` przy 0 i mniej (bez licznika), „99+” powyżej [MAX_BADGE_COUNT]. */
internal fun countBadgeLabel(count: Int): String? = when {
    count <= 0 -> null
    count > MAX_BADGE_COUNT -> "$MAX_BADGE_COUNT+"
    else -> count.toString()
}

/**
 * Okrągły licznik w kolorze akcentu (np. liczba sztuk w koszyku). Przy [count] ≤ 0 nic nie rysuje,
 * powyżej 99 pokazuje „99+”. Sam nie ma semantyki – opis dostępności daje element, na którym leży
 * (np. [KafeCartIcon]).
 *
 * @param count liczba do pokazania.
 */
@Composable
fun KafeCountBadge(
    count: Int,
    modifier: Modifier = Modifier,
) {
    val label = countBadgeLabel(count) ?: return
    Badge(
        containerColor = KafeTheme.colors.primary,
        contentColor = KafeTheme.colors.onPrimary,
        modifier = modifier,
    ) {
        Text(text = label, style = KafeTheme.typography.labelSmall)
    }
}

/**
 * Ikona koszyka z [KafeCountBadge] w prawym górnym rogu. Zajmuje minimalny obszar dotyku (48dp), więc
 * po dodaniu klikalności (ekran koszyka) układ się nie zmieni. Na razie nieklikalna; TalkBack czyta ją
 * jako jeden element, np. „Koszyk, 3 sztuki” albo „Koszyk jest pusty”.
 *
 * @param itemCount suma sztuk w koszyku; 0 = bez licznika.
 * @param tint kolor ikony – na ciemnym nagłówku menu `onHeader`.
 */
@Composable
fun KafeCartIcon(
    itemCount: Int,
    modifier: Modifier = Modifier,
    tint: Color = KafeTheme.colors.onBackground,
) {
    val description = if (itemCount > 0) {
        pluralStringResource(Res.plurals.ds_cart_item_count, itemCount, itemCount)
    } else {
        stringResource(Res.string.ds_cart_empty)
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(KafeTheme.spacing.minTouchTarget)
            .clearAndSetSemantics { contentDescription = description },
    ) {
        BadgedBox(badge = { KafeCountBadge(count = itemCount) }) {
            Icon(
                painter = painterResource(Res.drawable.ic_shopping_bag),
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(KafeTheme.spacing.icon),
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun KafeCartIconPreview() {
    KafePreview {
        Row(horizontalArrangement = Arrangement.spacedBy(KafeTheme.spacing.s)) {
            listOf(0, 3, 42, 120).forEach { KafeCartIcon(itemCount = it) }
        }
    }
}

@PreviewLightDark
@Composable
private fun KafeCartIconOnHeaderPreview() {
    KafePreview(withPadding = false) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(KafeTheme.spacing.s),
            modifier = Modifier
                .background(KafeTheme.extendedColors.header)
                .padding(KafeTheme.spacing.l),
        ) {
            listOf(0, 3, 42, 120).forEach {
                KafeCartIcon(itemCount = it, tint = KafeTheme.extendedColors.onHeader)
            }
        }
    }
}
