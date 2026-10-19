package com.karolina.cieslik.kafe.feature.start.presentation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.tooling.preview.Preview
import com.karolina.cieslik.kafe.core.presentation.designsystem.components.KafeLogo
import com.karolina.cieslik.kafe.core.presentation.designsystem.system.SystemBarsAppearance
import com.karolina.cieslik.kafe.core.presentation.designsystem.system.isReducedMotionEnabled
import com.karolina.cieslik.kafe.core.presentation.designsystem.theme.KafeTheme
import kafe.app.shared.generated.resources.Res
import kafe.app.shared.generated.resources.brand_name
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import kotlin.math.PI
import kotlin.math.sin

/** Oś czasu intro w ms (łącznie ok. 1,3 s); tu: rysowanie konturu filiżanki. Kolejne stałe – para i napis. */
private const val DRAW_DURATION = 650
private const val STEAM_DELAY = 450L
private const val STEAM_DURATION = 450
private const val TITLE_DELAY = 800L
private const val TITLE_DURATION = 350
private const val HOLD_AFTER_TITLE = 150L
private const val STEAM_WAVE_PERIOD = 1400
private const val STEAM_RISE = 6f
private const val TITLE_SLIDE = 12f

/** Napis pod spodkiem: środek napisu ok. 1/3 wysokości pola logo poniżej środka ekranu. */
private const val TITLE_OFFSET_DIVISOR = 3

/** Przezroczystość „cienia” konturu – kontynuacja ikony ze splasha, po której rysuje się pełna kreska. */
private const val GHOST_ALPHA = 0.3f

/** Stabilne tagi intro dla testów UI. */
object KafeIntroTestTags {
    /** Korzeń intro (całe tło marki). */
    const val INTRO = "kafeIntro"
}

/**
 * Animowane intro przy zimnym starcie – kontur filiżanki rysuje się (PathMeasure), unosi się falująca para,
 * na końcu pojawia się napis „Kafe”. Tło, rozmiar i położenie logo zgadzają się z systemowym splashem, a kontur
 * rysuje się po jego półprzezroczystym „cieniu”, więc przejście jest ciągłe. Paski systemowe mają jasne ikony
 * (kawowe tło w obu motywach). Przy wyłączonych animacjach od razu stan końcowy.
 *
 * @param onFinished wywoływane raz, po końcu animacji napisu (ok. 1,3 s) albo od razu przy wyłączonych
 * animacjach; para faluje dalej, dopóki intro jest na ekranie.
 */
@Composable
fun KafeIntro(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SystemBarsAppearance(darkStatusBarIcons = false, darkNavigationBarIcons = false)
    val reducedMotion = isReducedMotionEnabled()
    val initial = if (reducedMotion) 1f else 0f
    val drawProgress = remember { Animatable(initial) }
    val steamProgress = remember { Animatable(initial) }
    val titleProgress = remember { Animatable(initial) }
    val currentOnFinished by rememberUpdatedState(onFinished)

    // Klucz Unit: oś czasu rusza raz na wejście intro i nie restartuje się przy rekompozycji; aktualną lambdę
    // onFinished daje rememberUpdatedState.
    LaunchedEffect(Unit) {
        if (!reducedMotion) {
            launch { drawProgress.animateTo(1f, tween(DRAW_DURATION, easing = FastOutSlowInEasing)) }
            launch {
                delay(STEAM_DELAY)
                steamProgress.animateTo(1f, tween(STEAM_DURATION))
            }
            delay(TITLE_DELAY)
            titleProgress.animateTo(1f, tween(TITLE_DURATION, easing = FastOutSlowInEasing))
            delay(HOLD_AFTER_TITLE)
        }
        currentOnFinished()
    }

    val wavePhase = if (reducedMotion) {
        0f
    } else {
        val transition = rememberInfiniteTransition(label = "steam")
        val phase by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(STEAM_WAVE_PERIOD, easing = LinearEasing), RepeatMode.Restart),
            label = "steamPhase",
        )
        phase
    }

    val brandName = stringResource(Res.string.brand_name)
    val colors = KafeTheme.extendedColors
    val logoParts = remember { LogoParts() }
    // Logo dokładnie na środku ekranu (tam, gdzie ikona splasha), napis pod nim – poza osią logo.
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.brand)
            .testTag(KafeIntroTestTags.INTRO)
            .clearAndSetSemantics { contentDescription = brandName },
    ) {
        Canvas(
            modifier = Modifier
                .align(Alignment.Center)
                .size(KafeTheme.spacing.brandLogo),
        ) {
            // Układ logo 108×108 wyśrodkowany na płótnie i przeskalowany jak w ikonie (KafeLogo.SCALE).
            val unit = size.width / KafeLogo.VIEWPORT
            scale(scale = unit * KafeLogo.SCALE, pivot = center) {
                translate(left = center.x - KafeLogo.VIEWPORT / 2f, top = center.y - KafeLogo.VIEWPORT / 2f) {
                    drawLogo(
                        parts = logoParts,
                        color = colors.onBrand,
                        drawProgress = drawProgress.value,
                        steamProgress = steamProgress.value,
                        wavePhase = wavePhase,
                    )
                }
            }
        }
        Text(
            text = brandName,
            style = KafeTheme.typography.displayMedium,
            color = colors.onBrand,
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = KafeTheme.spacing.brandLogo / TITLE_OFFSET_DIVISOR)
                .graphicsLayer {
                    alpha = titleProgress.value
                    translationY = (1f - titleProgress.value) * TITLE_SLIDE * density
                },
        )
    }
}

private class LogoParts {
    val cup: Path = KafeLogo.path(KafeLogo.CUP)
    val handle: Path = KafeLogo.path(KafeLogo.HANDLE)
    val saucer: Path = KafeLogo.path(KafeLogo.SAUCER)
    val measure = PathMeasure()
    val segment = Path()
}

private fun DrawScope.drawLogo(
    parts: LogoParts,
    color: Color,
    drawProgress: Float,
    steamProgress: Float,
    wavePhase: Float,
) {
    val stroke = Stroke(width = KafeLogo.STROKE_WIDTH, cap = StrokeCap.Round, join = StrokeJoin.Round)
    val ghost = color.copy(alpha = color.alpha * GHOST_ALPHA)
    drawPath(parts.cup, ghost, style = stroke)
    drawPath(parts.handle, ghost, style = stroke)
    drawPath(parts.saucer, ghost, style = stroke)
    // Kolejno: filiżanka, ucho, spodek – fragmenty zachodzą na siebie, żeby rysowanie było płynne.
    drawPartial(parts, parts.cup, phase(drawProgress, 0f, 0.65f), color, stroke)
    drawPartial(parts, parts.handle, phase(drawProgress, 0.5f, 0.85f), color, stroke)
    drawPartial(parts, parts.saucer, phase(drawProgress, 0.7f, 1f), color, stroke)

    if (steamProgress > 0f) {
        translate(top = (1f - steamProgress) * STEAM_RISE) {
            KafeLogo.steamX.forEachIndexed { index, x ->
                val offset = KafeLogo.STEAM_AMPLITUDE * sin(2f * PI.toFloat() * (wavePhase + index / 3f))
                val steam = Path().apply {
                    moveTo(x, KafeLogo.STEAM_BOTTOM)
                    cubicTo(
                        x - offset, KafeLogo.STEAM_BOTTOM - 4f,
                        x + offset, KafeLogo.STEAM_TOP + 4f,
                        x, KafeLogo.STEAM_TOP,
                    )
                }
                drawPath(steam, color.copy(alpha = color.alpha * steamProgress), style = stroke)
            }
        }
    }
}

private fun DrawScope.drawPartial(parts: LogoParts, path: Path, progress: Float, color: Color, stroke: Stroke) {
    if (progress <= 0f) return
    parts.measure.setPath(path, forceClosed = false)
    parts.segment.reset()
    parts.measure.getSegment(0f, parts.measure.length * progress, parts.segment, startWithMoveTo = true)
    drawPath(parts.segment, color, style = stroke)
}

private fun phase(progress: Float, start: Float, end: Float): Float =
    ((progress - start) / (end - start)).coerceIn(0f, 1f)

@Preview
@Composable
private fun KafeIntroPreview() {
    KafeTheme { KafeIntro(onFinished = {}) }
}
