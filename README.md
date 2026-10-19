# Kafe

Aplikacja mobilna do zamawiania kawy w sieci kawiarni. Klient wybiera lokal, przegląda menu, konfiguruje napój (rozmiar, mleko, shoty, syrop) i widzi cenę na żywo.

Projekt jest w Kotlin Multiplatform: najpierw Android, iOS na tej samej bazie kodu jako „nice to have”.

## Stan projektu

MVP w budowie. Na `develop` działa:

- splash, animowane intro i ikona aplikacji (także tematyczna, Android 13+),
- wybór lokalu: lista alfabetyczna z godzinami na dziś i statusem „Otwarte”/„Zamknięte” (czas warszawski, godziny przez północ); wybór jest zapamiętywany między uruchomieniami,
- menu sieci podzielone na kategorie, z dostępnością produktów w wybranym lokalu; działa offline (najpierw lokalna baza, odświeżenie z sieci w tle),
- szczegóły produktu: rozmiar, modyfikatory z limitami, liczba sztuk (1–20), cena liczona w groszach.

Jeszcze nie ma: koszyka i zamówień, logowania, mapy lokali, powiadomień push. Dane menu i lokali pochodzą na razie z seeda w aplikacji; docelowym backendem jest Firebase.

Zadania i kryteria akceptacji: Jira, projekt **KAFE**.

## Technologie

| Obszar | Biblioteka |
|---|---|
| Język | Kotlin 2.4 (Multiplatform) |
| UI | Compose Multiplatform, Material 3 |
| Nawigacja | Navigation Compose (trasy typowane, `@Serializable`) |
| Asynchroniczność | Coroutines, `StateFlow` / `Flow` |
| Sieć | Ktor Client 3 + kotlinx.serialization |
| Baza lokalna | Room KMP (offline-first) |
| Preferencje | DataStore (zapamiętany lokal) |
| DI | Koin |
| Obrazy | Coil 3 |
| Testy | kotlin.test, Turbine, Robolectric (UI Compose na JVM) |

Wersje: `gradle/libs.versions.toml`.

## Architektura

Clean Architecture z podziałem na warstwy i jednokierunkowy przepływ danych (MVVM/MVI):

```
UI (Compose) ──Action──▶ ViewModel ──▶ UseCase / Repository ──▶ Room + API
     ▲                        │
     └──── StateFlow<UiState> ┘     zdarzenia jednorazowe (nawigacja, snackbar) przez Flow<Event>
```

- **Domena** – modele, reguły biznesowe, interfejsy repozytoriów; czysty Kotlin bez zależności od Androida.
- **Dane** – DTO, encje Room, mappery, repozytoria offline-first. Błędy zwracane jako `Result<T, DataError>`, nie wyjątki.
- **Prezentacja** – ViewModel wystawia niemutowalny `StateFlow<UiState>`; kontrakt ekranu (`UiState`, `Action`, `Event`) to `sealed interface`.

### Moduły

| Moduł | Zawartość |
|---|---|
| `core` | domena: `core/domain` (`Result`, `DataError`, `Money`) i `feature/*/domain` (menu, produkt, lokale) |
| `app/shared` | dane, prezentacja, DI, nawigacja i design system (`core/presentation/designsystem`, motyw `KafeTheme`) |
| `app/androidApp` | punkt wejścia Androida: `MainActivity`, splash, zasoby ikony |
| `app/iosApp` | punkt wejścia iOS (Xcode) |

Kod funkcji jest pogrupowany po funkcjach: `feature/menu`, `feature/product`, `feature/cafe`, `feature/start` – każda z podkatalogami `data`, `presentation` (i `domain` w module `core`).

## Uruchomienie

Wymagania:

- Android Studio z JDK 21 (JBR) – AGP 9 wymaga JDK 17+,
- Android SDK z platformą 37 (`compileSdk`); `minSdk` to 29 (Android 10),
- `local.properties` z `sdk.dir` (Android Studio tworzy go sam).

Budowanie i instalacja:

```bash
./gradlew :app:androidApp:assembleDebug        # APK: app/androidApp/build/outputs/apk/debug/
./gradlew :app:androidApp:installDebug         # instalacja na podłączonym urządzeniu/emulatorze
```

Na Windows: `gradlew.bat`. iOS: katalog `app/iosApp` w Xcode (wymaga macOS; na razie nie jest budowany ani testowany).

## Testy

Ten sam zestaw co CI (`.github/workflows/ci.yml`):

```bash
./gradlew :app:androidApp:assembleDebug :app:shared:assembleAndroidDeviceTest \
  :core:testAndroidHostTest :app:shared:testAndroidHostTest :app:androidApp:testDebugUnitTest \
  :app:androidApp:lintDebug
```

Rodzaje testów, konwencje i znane problemy: [docs/testing.md](docs/testing.md).

## Jak pracujemy

- Gałęzie: `master` (wydania), `develop` (integracja, cel PR-ów), `feature/KAFE-<n>-<opis>`.
- Commity: `KAFE-<n>: <opis po polsku>`.
- Małe PR-y (ok. 400 zmienionych linii bez plików generowanych), testy w tym samym PR co kod. CI musi być zielone przed merge.
- Komentarze i KDoc po polsku, nad deklaracjami, bez numerów ticketów (wyjątki: `TODO(KAFE-n)`, wyłączone testy).

Pełne zasady kodu: [CLAUDE.md](CLAUDE.md).
