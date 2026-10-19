package com.karolina.cieslik.kafe.core.presentation.designsystem.components

import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.PathParser

/**
 * Geometria logo (filiżanka na spodku + para) w układzie 108×108 – tym samym, co adaptacyjna ikona launchera
 * (treść w bezpiecznej strefie 21–87). Te same ścieżki są w `res/drawable/ic_launcher_foreground.xml` aplikacji
 * Android (wraz ze skalą grupy) – zmieniając je tutaj, zmień je też tam.
 */
internal object KafeLogo {
    const val VIEWPORT = 108f
    const val STROKE_WIDTH = 4f

    /** Skala wokół środka – logo mieści się w okręgu r=33 (maska ikony adaptacyjnej i splasha). */
    const val SCALE = 0.8f

    const val CUP = "M30,44 L70,44 L67,68 Q66,76 58,76 L42,76 Q34,76 33,68 Z"
    const val HANDLE = "M69,50 Q80,50 80,58 Q80,66 68,66"
    const val SAUCER = "M26,82 L78,82"

    /** Pozycje X strużek pary oraz ich początek (nad brzegiem filiżanki) i koniec. */
    val steamX = listOf(42f, 50f, 58f)
    const val STEAM_BOTTOM = 38f
    const val STEAM_TOP = 26f
    const val STEAM_AMPLITUDE = 4f

    fun path(pathData: String): Path = PathParser().parsePathString(pathData).toPath()
}
