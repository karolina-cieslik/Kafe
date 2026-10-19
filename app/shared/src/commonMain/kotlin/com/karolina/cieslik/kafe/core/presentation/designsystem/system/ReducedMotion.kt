package com.karolina.cieslik.kafe.core.presentation.designsystem.system

import androidx.compose.runtime.Composable

/**
 * Czy użytkownik wyłączył animacje w systemie (Android: skala animacji 0, iOS: „Ogranicz ruch”).
 * Ekrany z dłuższą animacją (intro) pokazują wtedy stan końcowy od razu.
 *
 * @return `true`, gdy animacje są wyłączone; odczyt raz na kompozycję (zmiana ustawienia działa od kolejnego startu ekranu).
 */
@Composable
expect fun isReducedMotionEnabled(): Boolean
