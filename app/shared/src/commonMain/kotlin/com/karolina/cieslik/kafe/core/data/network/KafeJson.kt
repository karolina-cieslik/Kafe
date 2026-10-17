package com.karolina.cieslik.kafe.core.data.network

import kotlinx.serialization.json.Json

// KAFE-2: Defensywna konfiguracja JSON – nowe pola lub wartości enumów w API nie psują parsowania.
val KafeJson: Json = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
    explicitNulls = false
}
