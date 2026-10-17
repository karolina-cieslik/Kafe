package com.karolina.cieslik.kafe.core.presentation.designsystem.system

import androidx.compose.runtime.Composable

// KAFE-42: Czy użytkownik wyłączył animacje w systemie (Android: skala animacji 0, iOS: „Ogranicz ruch”).
// Ekrany z dłuższą animacją (intro) pokazują wtedy stan końcowy od razu.
@Composable
expect fun isReducedMotionEnabled(): Boolean
