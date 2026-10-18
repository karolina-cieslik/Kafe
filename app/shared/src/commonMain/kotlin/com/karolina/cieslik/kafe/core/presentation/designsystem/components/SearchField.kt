package com.karolina.cieslik.kafe.core.presentation.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.karolina.cieslik.kafe.core.presentation.designsystem.preview.KafePreview
import com.karolina.cieslik.kafe.core.presentation.designsystem.theme.KafeTheme
import kafe.app.shared.generated.resources.Res
import kafe.app.shared.generated.resources.ds_search_clear
import kafe.app.shared.generated.resources.ic_close
import kafe.app.shared.generated.resources.ic_search
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * KAFE-25: Kolory [SearchField]. Gotowe warianty są w [SearchFieldDefaults].
 *
 * @property container tło pola.
 * @property content wpisany tekst i ikony.
 * @property placeholder podpowiedź widoczna przy pustym polu.
 * @property cursor kolor kursora.
 */
data class SearchFieldColors(
    val container: Color,
    val content: Color,
    val placeholder: Color,
    val cursor: Color,
)

/** KAFE-25: Warianty kolorów [SearchField] – na jasne tło ekranu i na ciemny nagłówek. */
object SearchFieldDefaults {
    /** Pole na tle `background` / `surface`. */
    @Composable
    fun onBackground() = SearchFieldColors(
        container = KafeTheme.colors.surfaceVariant,
        content = KafeTheme.colors.onSurface,
        placeholder = KafeTheme.colors.onSurfaceVariant,
        cursor = KafeTheme.colors.primary,
    )

    /** Pole na ciemnym nagłówku (`KafeTheme.extendedColors.header`), np. w menu. */
    @Composable
    fun onHeader() = SearchFieldColors(
        container = KafeTheme.extendedColors.headerField,
        content = KafeTheme.extendedColors.onHeader,
        placeholder = KafeTheme.extendedColors.onHeaderVariant,
        cursor = KafeTheme.colors.primary,
    )
}

private const val DISABLED_ALPHA = 0.6f

/**
 * KAFE-25: Jednoliniowe pole wyszukiwania z ikoną lupy i przyciskiem czyszczenia (widocznym, gdy pole
 * jest aktywne i niepuste).
 * Klawisz „Szukaj” na klawiaturze tylko ją chowa – wyniki filtrują się już w trakcie pisania.
 *
 * @param query aktualny tekst; pole jest sterowane z zewnątrz (stan trzyma ViewModel).
 * @param onQueryChange wywoływane przy każdej zmianie tekstu oraz z pustym tekstem po stuknięciu „wyczyść”.
 * @param placeholder podpowiedź widoczna przy pustym polu.
 * @param enabled `false` blokuje edycję, ukrywa przycisk czyszczenia i przygasza pole (np. gdy menu się jeszcze ładuje).
 * @param colors kolory pola – [SearchFieldDefaults.onBackground] lub [SearchFieldDefaults.onHeader].
 */
@Composable
fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: SearchFieldColors = SearchFieldDefaults.onBackground(),
) {
    val focusManager = LocalFocusManager.current
    BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        enabled = enabled,
        singleLine = true,
        textStyle = KafeTheme.typography.bodyMedium.copy(color = colors.content),
        cursorBrush = SolidColor(colors.cursor),
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.None,
            imeAction = ImeAction.Search,
        ),
        keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else DISABLED_ALPHA),
        decorationBox = { innerTextField ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(KafeTheme.spacing.s),
                modifier = Modifier
                    .clip(KafeTheme.shapes.medium)
                    .background(colors.container)
                    .heightIn(min = KafeTheme.spacing.fieldHeight)
                    .padding(start = KafeTheme.spacing.l, end = KafeTheme.spacing.xs),
            ) {
                Icon(
                    painter = painterResource(Res.drawable.ic_search),
                    contentDescription = null,
                    tint = colors.content,
                    modifier = Modifier.size(KafeTheme.spacing.icon - KafeTheme.spacing.xs),
                )
                Box(modifier = Modifier.weight(1f)) {
                    if (query.isEmpty()) {
                        Text(
                            text = placeholder,
                            style = KafeTheme.typography.bodyMedium,
                            color = colors.placeholder,
                            maxLines = 1,
                        )
                    }
                    innerTextField()
                }
                if (query.isNotEmpty() && enabled) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_close),
                            contentDescription = stringResource(Res.string.ds_search_clear),
                            tint = colors.content,
                            modifier = Modifier.size(KafeTheme.spacing.icon - KafeTheme.spacing.xs),
                        )
                    }
                }
            }
        },
    )
}

@PreviewLightDark
@Composable
private fun SearchFieldPreview() {
    KafePreview {
        Column(verticalArrangement = Arrangement.spacedBy(KafeTheme.spacing.m)) {
            SearchField(query = "", onQueryChange = {}, placeholder = "Szukaj kawy")
            SearchField(query = "latte", onQueryChange = {}, placeholder = "Szukaj kawy")
            SearchField(query = "", onQueryChange = {}, placeholder = "Szukaj kawy", enabled = false)
            Box(
                modifier = Modifier
                    .background(KafeTheme.extendedColors.header)
                    .padding(KafeTheme.spacing.l),
            ) {
                SearchField(
                    query = "",
                    onQueryChange = {},
                    placeholder = "Szukaj kawy",
                    colors = SearchFieldDefaults.onHeader(),
                )
            }
        }
    }
}
