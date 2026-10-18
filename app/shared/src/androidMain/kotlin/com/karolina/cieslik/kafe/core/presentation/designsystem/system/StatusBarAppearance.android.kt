package com.karolina.cieslik.kafe.core.presentation.designsystem.system

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/** Android: ustawia `isAppearanceLightStatusBars` okna aktywności; w podglądzie (edit mode) nic nie robi. */
@Composable
actual fun StatusBarAppearance(darkIcons: Boolean) {
    val view = LocalView.current
    if (view.isInEditMode) return
    // SideEffect, a nie LaunchedEffect: ma się wykonać po każdej udanej kompozycji, także gdy ekran wraca
    // na wierzch, a inny ekran zdążył w międzyczasie zmienić kolor ikon.
    SideEffect {
        val window = view.context.findActivity()?.window ?: return@SideEffect
        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = darkIcons
    }
}

/** Szuka aktywności pod kolejnymi ContextWrapperami (np. kontekst z motywem). */
private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
