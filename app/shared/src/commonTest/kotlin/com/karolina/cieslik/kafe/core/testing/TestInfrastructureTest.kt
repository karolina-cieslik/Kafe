package com.karolina.cieslik.kafe.core.testing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.cash.turbine.test
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.test.Test
import kotlin.test.assertEquals

/** Sprawdza, że runViewModelTest + Turbine działają z viewModelScope na podmienionym Main i wirtualnym czasem. */
class TestInfrastructureTest {

    private class SampleViewModel : ViewModel() {
        private val _state = MutableStateFlow("Loading")
        val state: StateFlow<String> = _state.asStateFlow()

        fun load() {
            viewModelScope.launch {
                delay(1_000)
                _state.value = "Content"
            }
        }
    }

    @Test
    fun viewModelStateIsObservedWithTurbineUsingVirtualTime() = runViewModelTest { context ->
        val viewModel = context.track(SampleViewModel())

        viewModel.state.test {
            assertEquals("Loading", awaitItem())
            viewModel.load()
            assertEquals("Content", awaitItem())
        }
    }
}
