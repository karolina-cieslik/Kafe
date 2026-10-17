package com.karolina.cieslik.kafe.feature.start.presentation

import com.karolina.cieslik.kafe.navigation.KafeRoute

// KAFE-31 / KAFE-42: Ekran startowy aplikacji. Ustalany raz przy zimnym starcie (AppStartViewModel.state):
// bez zapamiętanego lokalu – CafePicker, w przeciwnym razie Menu. Intro/splash (ui) czeka na Ready.
sealed interface AppStartState {

    data object Loading : AppStartState

    data class Ready(val startDestination: KafeRoute) : AppStartState
}
