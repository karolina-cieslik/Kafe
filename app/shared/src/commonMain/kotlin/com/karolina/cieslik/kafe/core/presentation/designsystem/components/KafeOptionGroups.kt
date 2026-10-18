package com.karolina.cieslik.kafe.core.presentation.designsystem.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.karolina.cieslik.kafe.core.presentation.designsystem.preview.KafePreview
import com.karolina.cieslik.kafe.core.presentation.designsystem.theme.KafeTheme
import kafe.app.shared.generated.resources.Res
import kafe.app.shared.generated.resources.ds_optional
import kafe.app.shared.generated.resources.ds_required
import org.jetbrains.compose.resources.stringResource

private const val DISABLED_ALPHA = 0.4f

/**
 * Opcja w grupie jednokrotnego wyboru ([KafeChoiceGroup]).
 *
 * @property id identyfikator opcji zwracany w `onOptionClick`.
 * @property label nazwa opcji, np. „Mleko owsiane”.
 * @property trailingLabel dopłata, np. „+2,00 zł”; `null` = bez dopłaty (nic nie jest wyświetlane).
 * @property isEnabled `false` = opcja chwilowo niedostępna – przygaszona i nieklikalna.
 * @property isSelected czy opcja jest zaznaczona.
 */
@Immutable
data class ChoiceOptionUi(
    val id: String,
    val label: String,
    val trailingLabel: String?,
    val isEnabled: Boolean = true,
    val isSelected: Boolean = false,
)

/**
 * Opcja w grupie z licznikami ([KafeQuantityGroup]).
 *
 * @property id identyfikator opcji zwracany w `onQuantityChange`.
 * @property label nazwa opcji, np. „Dodatkowe espresso”; trafia też do opisów dostępności steppera.
 * @property trailingLabel dopłata za sztukę, np. „+3,00 zł”; `null` = bez dopłaty.
 * @property quantity wybrana liczba sztuk.
 * @property canIncrease czy można dodać sztukę (`false` po osiągnięciu limitu opcji lub grupy).
 * @property canDecrease czy można odjąć sztukę (`false` przy zerze lub minimum).
 * @property isEnabled `false` = opcja chwilowo niedostępna – przygaszona, oba przyciski wyłączone.
 */
@Immutable
data class QuantityOptionUi(
    val id: String,
    val label: String,
    val trailingLabel: String?,
    val quantity: Int,
    val canIncrease: Boolean,
    val canDecrease: Boolean,
    val isEnabled: Boolean = true,
)

/**
 * Grupa jednokrotnego wyboru modyfikatora (np. mleko) – nagłówek i lista opcji z przyciskami radiowymi.
 * Komponent tylko zgłasza kliknięcie; o tym, co ono znaczy, decyduje ViewModel (np. w grupie opcjonalnej
 * ponowne kliknięcie zaznaczonej opcji ją odznacza).
 *
 * @param title nazwa grupy, ogłaszana przez TalkBack jako nagłówek.
 * @param isRequired czy wybór jest wymagany – w nagłówku „Wymagane” albo „Opcjonalne”.
 * @param options opcje w kolejności wyświetlania.
 * @param onOptionClick wywoływane z id stukniętej dostępnej opcji, także już zaznaczonej.
 * @param optionTestTag opcjonalna funkcja tworząca tag testowy wiersza opcji z jej id.
 */
@Composable
fun KafeChoiceGroup(
    title: String,
    isRequired: Boolean,
    options: List<ChoiceOptionUi>,
    onOptionClick: (optionId: String) -> Unit,
    modifier: Modifier = Modifier,
    optionTestTag: ((optionId: String) -> String)? = null,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        KafeGroupHeader(title = title, isRequired = isRequired)
        Column(modifier = Modifier.selectableGroup()) {
            options.forEach { option ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = KafeTheme.spacing.minTouchTarget)
                        .clip(KafeTheme.shapes.small)
                        .selectable(
                            selected = option.isSelected,
                            enabled = option.isEnabled,
                            role = Role.RadioButton,
                            onClick = { onOptionClick(option.id) },
                        )
                        .alpha(if (option.isEnabled) 1f else DISABLED_ALPHA)
                        .then(if (optionTestTag != null) Modifier.testTag(optionTestTag(option.id)) else Modifier),
                ) {
                    RadioButton(
                        selected = option.isSelected,
                        onClick = null,
                        enabled = option.isEnabled,
                        colors = RadioButtonDefaults.colors(selectedColor = KafeTheme.colors.primary),
                    )
                    Spacer(Modifier.width(KafeTheme.spacing.m))
                    OptionLabels(label = option.label, trailingLabel = option.trailingLabel)
                }
            }
        }
    }
}

/**
 * Grupa modyfikatorów z licznikami (np. dodatkowe shoty 0–4) – nagłówek i wiersze ze [KafeStepper].
 * Gdy któraś wybrana opcja osiąga limit (nie da się jej zwiększyć), podpowiedź limitu („maks. 4”) zmienia
 * kolor na akcent i jest ogłaszana przez TalkBack.
 *
 * @param title nazwa grupy, ogłaszana jako nagłówek.
 * @param isRequired czy wybór jest wymagany – w nagłówku „Wymagane” albo „Opcjonalne”.
 * @param limitLabel podpowiedź limitu, np. „maks. 4”; `null` = grupa bez limitu.
 * @param options opcje w kolejności wyświetlania.
 * @param onQuantityChange wywoływane z id opcji i zmianą (`+1` / `-1`) po kliknięciu aktywnego „+” / „−”.
 */
@Composable
fun KafeQuantityGroup(
    title: String,
    isRequired: Boolean,
    limitLabel: String?,
    options: List<QuantityOptionUi>,
    onQuantityChange: (optionId: String, delta: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isLimitReached = options.any { it.quantity > 0 && !it.canIncrease }
    Column(modifier = modifier.fillMaxWidth()) {
        KafeGroupHeader(
            title = title,
            isRequired = isRequired,
            hint = limitLabel,
            isHintEmphasized = isLimitReached,
        )
        options.forEach { option ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = KafeTheme.spacing.minTouchTarget)
                    .alpha(if (option.isEnabled) 1f else DISABLED_ALPHA),
            ) {
                OptionLabels(label = option.label, trailingLabel = option.trailingLabel)
                Spacer(Modifier.width(KafeTheme.spacing.m))
                KafeStepper(
                    value = option.quantity,
                    label = option.label,
                    onDecrease = { onQuantityChange(option.id, -1) },
                    onIncrease = { onQuantityChange(option.id, +1) },
                    canDecrease = option.isEnabled && option.canDecrease,
                    canIncrease = option.isEnabled && option.canIncrease,
                )
            }
        }
    }
}

/**
 * Nagłówek sekcji formularza produktu: tytuł, oznaczenie „Wymagane” / „Opcjonalne” i podpowiedź po prawej.
 *
 * @param title tytuł sekcji, ogłaszany jako nagłówek.
 * @param isRequired `true` = „Wymagane”, `false` = „Opcjonalne”, `null` = bez oznaczenia (np. sekcja rozmiaru).
 * @param hint podpowiedź po prawej (np. limit); zmiana jest ogłaszana przez TalkBack.
 * @param isHintEmphasized `true` wyróżnia podpowiedź kolorem akcentu (np. po osiągnięciu limitu).
 */
@Composable
fun KafeGroupHeader(
    title: String,
    modifier: Modifier = Modifier,
    isRequired: Boolean? = null,
    hint: String? = null,
    isHintEmphasized: Boolean = false,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(KafeTheme.spacing.s),
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = KafeTheme.spacing.xs),
    ) {
        Text(
            text = title,
            style = KafeTheme.typography.titleMedium,
            color = KafeTheme.colors.onBackground,
            modifier = Modifier
                .weight(1f, fill = false)
                .semantics { heading() },
        )
        if (isRequired != null) {
            Text(
                text = stringResource(if (isRequired) Res.string.ds_required else Res.string.ds_optional),
                style = KafeTheme.typography.labelMedium,
                color = if (isRequired) KafeTheme.colors.primary else KafeTheme.colors.onSurfaceVariant,
            )
        }
        Spacer(Modifier.weight(1f))
        if (hint != null) {
            Text(
                text = hint,
                style = KafeTheme.typography.labelMedium,
                color = if (isHintEmphasized) KafeTheme.colors.primary else KafeTheme.colors.onSurfaceVariant,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
    }
}

/** Nazwa opcji (do dwóch linii, zajmuje dostępne miejsce) i dopłata po prawej. */
@Composable
private fun RowScope.OptionLabels(label: String, trailingLabel: String?) {
    Text(
        text = label,
        style = KafeTheme.typography.bodyLarge,
        color = KafeTheme.colors.onSurface,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.weight(1f),
    )
    if (trailingLabel != null) {
        Text(
            text = trailingLabel,
            style = KafeTheme.typography.bodyMedium,
            color = KafeTheme.colors.onSurfaceVariant,
        )
    }
}

@PreviewLightDark
@Composable
private fun KafeOptionGroupsPreview() {
    KafePreview {
        Column(verticalArrangement = Arrangement.spacedBy(KafeTheme.spacing.l)) {
            KafeChoiceGroup(
                title = "Mleko",
                isRequired = true,
                options = listOf(
                    ChoiceOptionUi("cow", "Krowie", null, isSelected = true),
                    ChoiceOptionUi("oat", "Owsiane", "+2,00 zł"),
                    ChoiceOptionUi("soy", "Sojowe", "+2,00 zł", isEnabled = false),
                ),
                onOptionClick = {},
            )
            KafeChoiceGroup(
                title = "Syrop",
                isRequired = false,
                options = listOf(
                    ChoiceOptionUi("vanilla", "Waniliowy", "+1,50 zł"),
                    ChoiceOptionUi("caramel", "Karmelowy", "+1,50 zł"),
                ),
                onOptionClick = {},
            )
            KafeQuantityGroup(
                title = "Dodatki",
                isRequired = false,
                limitLabel = "maks. 4",
                options = listOf(
                    QuantityOptionUi("shot", "Dodatkowe espresso", "+3,00 zł", quantity = 4, canIncrease = false, canDecrease = true),
                ),
                onQuantityChange = { _, _ -> },
            )
        }
    }
}
