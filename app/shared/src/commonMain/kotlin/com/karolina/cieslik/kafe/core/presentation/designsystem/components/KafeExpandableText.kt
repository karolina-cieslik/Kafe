package com.karolina.cieslik.kafe.core.presentation.designsystem.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.karolina.cieslik.kafe.core.presentation.designsystem.preview.KafePreview
import com.karolina.cieslik.kafe.core.presentation.designsystem.theme.KafeTheme
import kafe.app.shared.generated.resources.Res
import kafe.app.shared.generated.resources.ds_read_less
import kafe.app.shared.generated.resources.ds_read_more
import org.jetbrains.compose.resources.stringResource

/**
 * Tekst zwijany do [collapsedMaxLines] linii z przełącznikiem „Czytaj więcej” / „Zwiń”. Przełącznik pojawia się
 * tylko wtedy, gdy tekst faktycznie się nie mieści. Stan rozwinięcia przetrwa obrót ekranu.
 *
 * @param text pełny tekst (np. opis produktu).
 * @param collapsedMaxLines liczba linii w stanie zwiniętym.
 */
@Composable
fun KafeExpandableText(
    text: String,
    modifier: Modifier = Modifier,
    collapsedMaxLines: Int = 3,
) {
    // Oba w rememberSaveable: po obrocie rozwinięty opis zostaje rozwinięty, a przełącznik nie znika do czasu
    // ponownego pomiaru tekstu.
    var isExpanded by rememberSaveable { mutableStateOf(false) }
    var hasOverflow by rememberSaveable { mutableStateOf(false) }
    Column(modifier = modifier.animateContentSize()) {
        Text(
            text = text,
            style = KafeTheme.typography.bodyMedium,
            color = KafeTheme.colors.onSurfaceVariant,
            maxLines = if (isExpanded) Int.MAX_VALUE else collapsedMaxLines,
            overflow = TextOverflow.Ellipsis,
            onTextLayout = { result -> if (!isExpanded) hasOverflow = result.hasVisualOverflow },
        )
        if (hasOverflow || isExpanded) {
            Text(
                text = stringResource(if (isExpanded) Res.string.ds_read_less else Res.string.ds_read_more),
                style = KafeTheme.typography.labelLarge,
                color = KafeTheme.colors.primary,
                modifier = Modifier
                    .clip(KafeTheme.shapes.extraSmall)
                    .clickable(role = Role.Button) { isExpanded = !isExpanded }
                    .padding(vertical = KafeTheme.spacing.s),
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun KafeExpandableTextPreview() {
    KafePreview {
        KafeExpandableText(
            text = "Cappuccino to klasyka włoskiej kawiarni: podwójne espresso z ziaren z Etiopii, gorące mleko " +
                "i aksamitna pianka. Podajemy w filiżance 250 ml, a na życzenie z mlekiem roślinnym i syropem. " +
                "Najlepiej smakuje od razu, zanim pianka opadnie.",
        )
    }
}
