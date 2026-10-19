package com.karolina.cieslik.kafe.core.presentation.designsystem.system

import androidx.compose.runtime.Composable

/**
 * Ustawia kolor ikon paska statusu. Wywołuje go każdy ekran sam, bo tło pod paskiem zależy od ekranu
 * (ciemny nagłówek menu vs jasny pasek szczegółów). Ustawienie jest ponawiane przy każdej udanej kompozycji,
 * więc po powrocie na ekran wraca jego kolor.
 *
 * @param darkIcons `true` = ciemne ikony (na jasnym tle), `false` = jasne ikony (na ciemnym tle).
 */
@Composable
expect fun StatusBarAppearance(darkIcons: Boolean)
