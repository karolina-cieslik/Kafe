package com.karolina.cieslik.kafe.core.data.network

import kotlinx.serialization.json.Json

/**
 * Defensywna konfiguracja JSON: nieznane pola są pomijane, nieznane wartości enumów i null w polach
 * niepustych zamieniane na wartości domyślne, a null nie jest zapisywany przy serializacji.
 */
val KafeJson: Json = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
    explicitNulls = false
}
