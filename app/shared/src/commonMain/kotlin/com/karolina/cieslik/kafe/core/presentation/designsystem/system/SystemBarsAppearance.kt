package com.karolina.cieslik.kafe.core.presentation.designsystem.system

import androidx.compose.runtime.Composable

// KAFE-9 / KAFE-42: Kolor ikon pasków systemowych ustawiany przez ekran, bo tło pod nimi zależy od ekranu
// (ciemny nagłówek menu, jasny pasek szczegółów, kawowe intro). Paski są przezroczyste (edge-to-edge,
// bez systemowego scrimu), więc każdy ekran ustawia oba kolory sam przy wejściu.
@Composable
expect fun SystemBarsAppearance(darkStatusBarIcons: Boolean, darkNavigationBarIcons: Boolean)
