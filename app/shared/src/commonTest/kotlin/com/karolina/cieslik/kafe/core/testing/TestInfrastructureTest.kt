package com.karolina.cieslik.kafe.core.testing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.cash.turbine.test
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.test.Test
import kotlin.test.assertEquals

// KAFE-27: Sprawdza, że runViewModelTest + Turbine działają z viewModelScope i wstrzykniętym dispatcherem.
class TestInfrastructureTest {

    private class SampleViewModel(private val ioDispatcher: CoroutineDispatcher) : ViewModel() {
        private val _state = MutableStateFlow("Loading")
        val state: StateFlow<String> = _state.asStateFlow()

        fun load() {
            viewModelScope.launch {
                val value = withContext(ioDispatcher) {
                    delay(1_000)
                    "Content"
                }
                _state.value = value
            }
        }
    }

    @Test
    fun viewModelStateIsObservedWithTurbineUsingVirtualTime() = runViewModelTest { context ->
        val viewModel = context.track(SampleViewModel(ioDispatcher = context.dispatcher))

        viewModel.state.test {
            assertEquals("Loading", awaitItem())
            viewModel.load()
            assertEquals("Content", awaitItem())
        }
    }
}
