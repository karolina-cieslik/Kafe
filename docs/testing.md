# Testy w Kafe

Ten opis powstał w ramach KAFE-27.

## Rodzaje testów i taski Gradle

| Rodzaj | Source set | Task | Gdzie się uruchamia |
|---|---|---|---|
| Domena (czysty Kotlin) | `core/src/commonTest` | `:core:testAndroidHostTest` | JVM, lokalnie i na CI |
| Data / presentation (wspólne) | `app/shared/src/commonTest` | `:app:shared:testAndroidHostTest` | JVM, lokalnie i na CI |
| Tylko Android, host | `app/shared/src/androidHostTest` | `:app:shared:testAndroidHostTest` | JVM, lokalnie i na CI |
| Instrumentalne (np. Room DAO) | `app/shared/src/androidDeviceTest` | `:app:shared:connectedAndroidDeviceTest` | urządzenie lub emulator, **nie na CI** |
| iOS | `iosTest` | `:app:shared:iosSimulatorArm64Test` | tylko macOS, **nie na CI** |

Lokalnie przed commitem uruchamiamy to samo, co CI (`.github/workflows/ci.yml`):

```
./gradlew :app:androidApp:assembleDebug :core:testAndroidHostTest :app:shared:testAndroidHostTest :app:androidApp:testDebugUnitTest :app:androidApp:lintDebug
```

## Konwencje

- **Nazwy testów w camelCase**, bez backticków ze spacjami. `commonTest` trafia także do APK testów instrumentalnych, a DEX poniżej API 30 (minSdk 29) odrzuca spacje w nazwach.
- Nazwa opisuje zachowanie: `retryAfterErrorEmitsLoadingThenContent`, a nie `testRetry`.
- Każda klasa testowa zaczyna się komentarzem z ID ticketu, np. `// KAFE-6: ...`.
- Struktura given / when / then oddzielona pustymi liniami. Jedno zachowanie na test.
- Używamy `kotlin.test` (`assertEquals`, `assertIs`, `assertFailsWith`), a nie bezpośrednio JUnit, żeby testy działały w `commonTest`.
- Zamiast mocków piszemy **ręczne fake'i** za interfejsami, np. `FakeMenuRepository` z `MutableStateFlow` i konfigurowalnym wynikiem `refresh`. Fake'i trzymamy obok testów danego feature'a w `commonTest`.
- Testy pakujemy tak samo jak kod produkcyjny: `com.karolina.cieslik.kafe.feature.<nazwa>.{domain,data,presentation}`.

## Coroutines, Flow i ViewModel

- Kod suspend testujemy w `runTest { ... }` (wirtualny czas, więc `delay` nie spowalnia testów).
- Flow testujemy biblioteką **Turbine** (`app.cash.turbine`): `flow.test { awaitItem(); ... }`. Pod koniec bloku Turbine sprawdza, czy nie zostały nieodebrane emisje.
- ViewModele testujemy helperem `runViewModelTest` z `core/testing/RunViewModelTest.kt`, który podmienia `Dispatchers.Main`, bo z niego korzysta `viewModelScope`. `context.dispatcher` wstrzykujemy do ViewModelu, żeby wszystko działało na jednym schedulerze. Każdy VM opakowujemy w `context.track(...)`: po teście helper anuluje jego `viewModelScope` przed `resetMain`. Bez tego subskrypcje `stateIn`/`shareIn` sięgną do Main już po teście i rzucą „Dispatchers.Main accessed after resetMain”.

  ```kotlin
  @Test
  fun initialLoadEmitsLoadingThenContent() = runViewModelTest { context ->
      val viewModel = context.track(MenuViewModel(..., dispatcher = context.dispatcher))

      viewModel.state.test {
          assertIs<MenuUiState.Loading>(awaitItem())
          assertIs<MenuUiState.Content>(awaitItem())
      }
  }
  ```

- Domyślnie używamy `StandardTestDispatcher`. Korutyny nie startują wtedy od razu, więc kolejność emisji jest deterministyczna, a do przesuwania czasu służy `advanceUntilIdle()`. `UnconfinedTestDispatcher` stosujemy tylko wtedy, gdy test świadomie sprawdza wyłącznie stan końcowy.
- W kodzie produkcyjnym nie używamy `Dispatchers.IO` ani `Dispatchers.Default` na sztywno. Dispatcher wstrzykujemy przez konstruktor lub Koin.

## Testy UI (Compose): pomysły na później

- Compose Multiplatform ma `runComposeUiTest` (`compose.uiTest`), ale na targecie Android host wymaga Robolectric. Bez urządzenia działa natywnie tylko na targecie desktop JVM, którego nie mamy.
- Propozycja:
  1. Testy UI `ProductCard` i `MenuScreen` jako `androidDeviceTest` z `createComposeRule()`. Uruchamiamy je ręcznie na urządzeniu, poza CI.
  2. Ewentualnie Robolectric w `androidHostTest`, żeby testy UI działały na CI. Do oceny, gdy pojawią się ekrany.
  3. Screenshot testy previewów (Compose Preview Screenshot Testing od AGP) jako tani test regresji wizualnej.
- Ekrany pozostają bezstanowe (`MenuScreen(state, onAction)`), więc większość logiki da się pokryć testami ViewModelu na JVM.
