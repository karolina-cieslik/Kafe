package com.karolina.cieslik.kafe.core.testing

/**
 * KAFE-27: Poziom API, na którym Robolectric uruchamia testy UI (`@Config(sdk = [ROBOLECTRIC_SDK])`).
 * Robolectric nie musi wspierać compileSdk projektu (37), więc testy biegną na stałym, wspieranym API 35.
 */
const val ROBOLECTRIC_SDK = 35
