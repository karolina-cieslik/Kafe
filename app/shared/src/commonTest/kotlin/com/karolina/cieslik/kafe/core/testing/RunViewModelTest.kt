package com.karolina.cieslik.kafe.core.testing

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

// KAFE-27: Uruchamia test z Dispatchers.Main podmienionym na [testDispatcher] (viewModelScope używa Main).
// Ten sam dispatcher wstrzykuj do ViewModelu, żeby cały test dzielił jeden scheduler i wirtualny czas.
@OptIn(ExperimentalCoroutinesApi::class)
fun runViewModelTest(
    testDispatcher: TestDispatcher = StandardTestDispatcher(),
    testBody: suspend TestScope.(TestDispatcher) -> Unit,
): TestResult {
    Dispatchers.setMain(testDispatcher)
    return try {
        runTest(testDispatcher) { testBody(testDispatcher) }
    } finally {
        Dispatchers.resetMain()
    }
}
