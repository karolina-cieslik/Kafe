package com.karolina.cieslik.kafe.core.presentation.designsystem.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.karolina.cieslik.kafe.core.presentation.designsystem.preview.KafePreview
import com.karolina.cieslik.kafe.core.presentation.designsystem.theme.KafeTheme
import kafe.app.shared.generated.resources.Res
import kafe.app.shared.generated.resources.ds_decrease
import kafe.app.shared.generated.resources.ds_increase
import kafe.app.shared.generated.resources.ds_quantity_value
import kafe.app.shared.generated.resources.ic_add
import kafe.app.shared.generated.resources.ic_remove
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

private const val DISABLED_ALPHA = 0.38f

enum class KafeStepperSize(val button: Dp, val icon: Dp, val valueWidth: Dp) {
    Small(button = 28.dp, icon = 16.dp, valueWidth = 24.dp),
    Large(button = 36.dp, icon = 20.dp, valueWidth = 32.dp),
}

// KAFE-25 / KAFE-9: Stepper „− wartość +” (dodatkowe shoty, liczba sztuk). [label] trafia do opisów
// dostępności („Zwiększ: Dodatkowe espresso”); zmiana wartości jest ogłaszana przez TalkBack (liveRegion).
@Composable
fun KafeStepper(
    value: Int,
    label: String,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    canDecrease: Boolean,
    canIncrease: Boolean,
    modifier: Modifier = Modifier,
    size: KafeStepperSize = KafeStepperSize.Small,
) {
    val valueDescription = stringResource(Res.string.ds_quantity_value, label, value)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(KafeTheme.spacing.xs),
        modifier = modifier,
    ) {
        StepperButton(
            icon = Res.drawable.ic_remove,
            contentDescription = stringResource(Res.string.ds_decrease, label),
            enabled = canDecrease,
            onClick = onDecrease,
            size = size,
        )
        Text(
            text = value.toString(),
            style = if (size == KafeStepperSize.Large) KafeTheme.typography.titleLarge else KafeTheme.typography.titleSmall,
            color = KafeTheme.colors.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .widthIn(min = size.valueWidth)
                .clearAndSetSemantics {
                    contentDescription = valueDescription
                    liveRegion = LiveRegionMode.Polite
                },
        )
        StepperButton(
            icon = Res.drawable.ic_add,
            contentDescription = stringResource(Res.string.ds_increase, label),
            enabled = canIncrease,
            onClick = onIncrease,
            size = size,
        )
    }
}

@Composable
private fun StepperButton(
    icon: DrawableResource,
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit,
    size: KafeStepperSize,
) {
    // Mały przycisk – hit-test Compose sam powiększa obszar dotyku do 48dp.
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        color = KafeTheme.colors.surface,
        contentColor = KafeTheme.colors.onSurface,
        border = BorderStroke(1.dp, KafeTheme.colors.outlineVariant),
        modifier = Modifier
            .size(size.button)
            .alpha(if (enabled) 1f else DISABLED_ALPHA),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                painter = painterResource(icon),
                contentDescription = contentDescription,
                modifier = Modifier.size(size.icon),
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun KafeStepperPreview() {
    KafePreview {
        Column(verticalArrangement = Arrangement.spacedBy(KafeTheme.spacing.m)) {
            KafeStepper(value = 0, label = "Dodatkowe espresso", onDecrease = {}, onIncrease = {}, canDecrease = false, canIncrease = true)
            KafeStepper(value = 4, label = "Dodatkowe espresso", onDecrease = {}, onIncrease = {}, canDecrease = true, canIncrease = false)
            KafeStepper(
                value = 2,
                label = "Liczba sztuk",
                onDecrease = {},
                onIncrease = {},
                canDecrease = true,
                canIncrease = true,
                size = KafeStepperSize.Large,
            )
        }
    }
}
