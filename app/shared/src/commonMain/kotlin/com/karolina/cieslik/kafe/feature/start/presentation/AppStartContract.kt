package com.karolina.cieslik.kafe.feature.start.presentation

import com.karolina.cieslik.kafe.navigation.KafeRoute

/**
 * Ekran startowy aplikacji, ustalany raz przy zimnym starcie ([AppStartViewModel.state]): bez zapamiętanego
 * lokalu – wybór lokalu, w przeciwnym razie menu. Intro/splash czeka na [Ready].
 */
sealed interface AppStartState {

    /** Trwa odczyt zapamiętanego wyboru. */
    data object Loading : AppStartState

    /**
     * Ekran startowy ustalony.
     *
     * @property startDestination [KafeRoute.CafePicker] albo [KafeRoute.Menu].
     */
    data class Ready(val startDestination: KafeRoute) : AppStartState
}
