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

// KAFE-25 / KAFE-9: Opcja w grupie modyfikatorów. trailingLabel to dopłata, np. „+2,00 zł”.
@Immutable
data class ChoiceOptionUi(
    val id: String,
    val label: String,
    val trailingLabel: String?,
    val isEnabled: Boolean = true,
    val isSelected: Boolean = false,
)

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

// KAFE-25 / KAFE-9: Grupa jednokrotnego wyboru (np. mleko). W grupie opcjonalnej ponowne kliknięcie
// zaznaczonej opcji ją odznacza – decyzję podejmuje VM, komponent tylko zgłasza kliknięcie.
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

// KAFE-25 / KAFE-9: Grupa z licznikami (np. dodatkowe shoty 0–4). Gdy któraś opcja osiąga limit,
// podpowiedź limitu („maks. 4”) zmienia kolor na akcent i jest ogłaszana przez TalkBack.
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

// KAFE-25 / KAFE-9: Nagłówek sekcji formularza produktu: tytuł + „Wymagane/Opcjonalne” + opcjonalna podpowiedź.
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
