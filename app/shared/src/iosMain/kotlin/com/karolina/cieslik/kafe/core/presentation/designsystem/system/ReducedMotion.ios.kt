package com.karolina.cieslik.kafe.core.presentation.designsystem.system

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.UIKit.UIAccessibilityIsReduceMotionEnabled

/** iOS: ustawienie dostępności „Ogranicz ruch”. */
@Composable
actual fun isReducedMotionEnabled(): Boolean = remember { UIAccessibilityIsReduceMotionEnabled() }
