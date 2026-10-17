package com.karolina.cieslik.kafe.core.testing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

// KAFE-27: Kontekst testu ViewModelu – dispatcher do wstrzyknięcia i rejestr VM do posprzątania po teście.
class ViewModelTestContext(val dispatcher: TestDispatcher) {
    private val viewModels = mutableListOf<ViewModel>()

    /** Rejestruje VM; po teście jego viewModelScope jest anulowany, zanim przywrócimy Dispatchers.Main. */
    fun <VM : ViewModel> track(viewModel: VM): VM = viewModel.also { viewModels += it }

    internal fun clearViewModels() {
        viewModels.forEach { it.viewModelScope.cancel() }
        viewModels.clear()
    }
}

// KAFE-27: Uruchamia test z Dispatchers.Main podmienionym na TestDispatcher (viewModelScope używa Main).
// Dispatcher z kontekstu wstrzykuj do ViewModelu, żeby cały test dzielił jeden scheduler i wirtualny czas.
// VM utworzone w teście przekazuj przez track() – subskrypcje stateIn/shareIn żyją w viewModelScope
// i bez anulowania odwołałyby się do Main już po resetMain.
@OptIn(ExperimentalCoroutinesApi::class)
fun runViewModelTest(
    testDispatcher: TestDispatcher = StandardTestDispatcher(),
    testBody: suspend TestScope.(ViewModelTestContext) -> Unit,
): TestResult {
    Dispatchers.setMain(testDispatcher)
    val context = ViewModelTestContext(testDispatcher)
    return try {
        runTest(testDispatcher) {
            try {
                testBody(context)
            } finally {
                context.clearViewModels()
            }
        }
    } finally {
        context.clearViewModels()
        Dispatchers.resetMain()
    }
}
