package com.karolina.cieslik.kafe

/** Pozostałość szablonu projektu KMP: opis platformy, na której działa aplikacja. */
interface Platform {
    /** Nazwa systemu z wersją, np. „Android 34”. */
    val name: String
}

/** Platforma bieżącego procesu. */
expect fun getPlatform(): Platform