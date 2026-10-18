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

/**
 * KAFE-27: Kontekst testu ViewModelu przekazywany do ciała [runViewModelTest].
 *
 * @property dispatcher dispatcher testu – ten sam, który podmienia `Dispatchers.Main`. Wstrzykuj go do ViewModelu
 * (parametr `dispatcher`), żeby cały test dzielił jeden scheduler i wirtualny czas (`advanceUntilIdle()`).
 */
class ViewModelTestContext(val dispatcher: TestDispatcher) {
    private val viewModels = mutableListOf<ViewModel>()

    /**
     * Rejestruje [viewModel] do posprzątania i zwraca go bez zmian (`val vm = context.track(MenuViewModel(...))`).
     * Po teście jego `viewModelScope` jest anulowany, zanim przywrócimy `Dispatchers.Main`.
     */
    fun <VM : ViewModel> track(viewModel: VM): VM = viewModel.also { viewModels += it }

    internal fun clearViewModels() {
        viewModels.forEach { it.viewModelScope.cancel() }
        viewModels.clear()
    }
}

/**
 * KAFE-27: Uruchamia test ViewModelu w `runTest` z `Dispatchers.Main` podmienionym na [testDispatcher]
 * (z niego korzysta `viewModelScope`).
 *
 * Każdy VM utworzony w teście przekazuj przez [ViewModelTestContext.track]. Subskrypcje `stateIn`/`shareIn`
 * żyją w `viewModelScope`, a bez anulowania po teście sięgnęłyby do Main już po `resetMain`
 * („Dispatchers.Main accessed after resetMain”). Sprzątanie i `resetMain` wykonują się także wtedy,
 * gdy test rzuci wyjątek.
 *
 * @param testDispatcher domyślnie [StandardTestDispatcher] – korutyny nie startują od razu, więc kolejność emisji
 * jest deterministyczna.
 * @return [TestResult] z `runTest`; zwracaj go wprost z funkcji `@Test` (`fun x() = runViewModelTest { … }`).
 */
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
