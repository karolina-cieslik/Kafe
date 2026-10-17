package com.karolina.cieslik.kafe.core.presentation.designsystem.system

import androidx.compose.runtime.Composable

// KAFE-9: Kolor ikon paska statusu ustawiany przez ekran, bo tło pod paskiem zależy od ekranu
// (ciemny nagłówek menu vs jasny pasek szczegółów). Każdy ekran ustawia go sam przy wejściu.
@Composable
expect fun StatusBarAppearance(darkIcons: Boolean)
