package com.karolina.cieslik.kafe.core.presentation.designsystem.system

import androidx.compose.runtime.Composable

// KAFE-9: Na iOS styl paska statusu ustawia hostujący UIViewController – na razie bez zmian (Android-first).
@Composable
actual fun SystemBarsAppearance(darkStatusBarIcons: Boolean, darkNavigationBarIcons: Boolean) = Unit
