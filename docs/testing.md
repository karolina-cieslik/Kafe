# Testy w Kafe

Ten opis powstał w ramach KAFE-27.

## Rodzaje testów i taski Gradle

| Rodzaj | Source set | Task | Gdzie się uruchamia |
|---|---|---|---|
| Domena (czysty Kotlin) | `core/src/commonTest` | `:core:testAndroidHostTest` | JVM, lokalnie i na CI |
| Data / presentation (wspólne) | `app/shared/src/commonTest` | `:app:shared:testAndroidHostTest` | JVM, lokalnie i na CI |
| Tylko Android, host + UI Compose (Robolectric) | `app/shared/src/androidHostTest` | `:app:shared:testAndroidHostTest` | JVM, lokalnie i na CI |
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

## Testy UI (Compose) na Robolectric

- Testy Compose działają na JVM w `app/shared/src/androidHostTest` (Robolectric 4.16 + `androidx.compose.ui:ui-test-junit4`). Uruchamia je ten sam task `:app:shared:testAndroidHostTest`, więc chodzą też w CI.
- Szablon klasy:

  ```kotlin
  @RunWith(AndroidJUnit4::class)
  @Config(sdk = [ROBOLECTRIC_SDK], qualifiers = "w411dp-h891dp")
  class MenuScreenTest {
      @get:Rule
      val composeRule = createComposeRule() // androidx.compose.ui.test.junit4.v2

      @Test
      fun retryButtonSendsRetry() {
          composeRule.setContent { KafeTheme(darkTheme = false) { MenuScreen(state, onAction = { actions += it }) } }
          composeRule.onNodeWithText("Spróbuj ponownie").performClick()
          ...
      }
  }
  ```

- `ROBOLECTRIC_SDK` (`core/testing/RobolectricConfig.kt`) = 35. Robolectric nie musi wspierać compileSdk projektu (37), więc testy UI chodzą na stałym, wspieranym API.
- Używamy `createComposeRule` z pakietu `junit4.v2` (StandardTestDispatcher). Stary `junit4.createComposeRule` jest deprecated.
- Ekrany są bezstanowe (`MenuScreen(state, onAction)`): test ustawia stan i zbiera akcje do listy. Logikę stanów testujemy w ViewModelu, a w UI tylko renderowanie i mapowanie kliknięć na akcje.
- Elementy znajdujemy po tekstach z zasobów (UI po polsku), `contentDescription` albo tagach `MenuTestTags`. Elementy siatki poza ekranem przewijamy przez `performScrollToNode` na `MenuTestTags.GRID`.
- Compose resources działają w Robolectric dzięki `withHostTest { isIncludeAndroidResources = true }`.
- Przy pierwszym uruchomieniu Robolectric pobiera z Maven obraz `android-all` (ok. 150 MB) do `~/.m2`. CI cache'uje ten katalog.
- **Przypięty pasek na dole (Scaffold bottomBar, edge-to-edge):** treść przewija się pod paskiem, więc `performScrollTo()` potrafi zostawić element pod nim i `performClick()` trafi w pasek (np. w „Dodaj do koszyka”). W `ProductDetailScreenTest` jest helper `scrollAboveBottomBar()`, który po `performScrollTo` dosuwa element nad górną krawędź paska.
- **Pomiar tekstu (`hasVisualOverflow`, „Czytaj więcej”) wymaga `@GraphicsMode(GraphicsMode.Mode.NATIVE)`.** W domyślnym trybie LEGACY Robolectric nie mierzy tekstu, więc przepełnienie nigdy nie występuje.
- Nawigację testujemy w `KafeNavHostTest`: `rememberNavController()` przechwycony w `setContent`, ViewModele z fake'ami podstawione przez `startKoin` w `@Before` (`stopKoin` w `@After`), systemowe wstecz przez `activity.onBackPressedDispatcher` (`createAndroidComposeRule<ComponentActivity>()`).
- Na później: screenshot testy previewów (Compose Preview Screenshot Testing z AGP) jako tani test regresji wizualnej.

## Znane problemy

- **`Money(Long.MIN_VALUE).format()`** zwraca niepoprawny tekst (`--92 233…,-8 zł`), bo `-grosze` przepełnia `Long`. W praktyce nieosiągalne (kwoty w groszach są o rzędy wielkości mniejsze). Zostawione bez poprawki na decyzję koordynatora (KAFE-6). Brak testu, żeby nie utrwalać błędnego zachowania.
