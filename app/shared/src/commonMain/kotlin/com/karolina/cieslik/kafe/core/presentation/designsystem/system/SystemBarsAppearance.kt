package com.karolina.cieslik.kafe.core.presentation.designsystem.system

import androidx.compose.runtime.Composable

/**
 * Ustawia kolor ikon pasków systemowych. Wywołuje go każdy ekran sam, bo tło pod paskami zależy od ekranu
 * (ciemny nagłówek menu, jasny pasek szczegółów, kawowe intro), a paski są przezroczyste (edge-to-edge, bez
 * systemowego scrimu). Ustawienie jest ponawiane przy każdej udanej kompozycji, więc po powrocie na ekran wraca
 * jego kolor.
 *
 * @param darkStatusBarIcons `true` = ciemne ikony paska statusu (na jasnym tle), `false` = jasne (na ciemnym).
 * @param darkNavigationBarIcons to samo dla paska nawigacji.
 */
@Composable
expect fun SystemBarsAppearance(darkStatusBarIcons: Boolean, darkNavigationBarIcons: Boolean)
