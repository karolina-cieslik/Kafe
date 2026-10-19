package com.karolina.cieslik.kafe.core.domain

/** Błędy warstwy danych zwracane w [Result]: sieć ([Remote]) i lokalna baza ([Local]). */
sealed interface DataError : Error {

    /** Błąd zapytania sieciowego – mapowany z kodów HTTP i wyjątków klienta w `safeCall`. */
    enum class Remote : DataError {
        NO_INTERNET,
        REQUEST_TIMEOUT,
        UNAUTHORIZED,
        NOT_FOUND,
        TOO_MANY_REQUESTS,
        CLIENT,
        SERVER,
        SERIALIZATION,
        UNKNOWN,
    }

    /** Błąd lokalnej bazy Room – mapowany z wyjątków SQLite w `safeDbCall`. */
    enum class Local : DataError {
        DISK_FULL,
        NOT_FOUND,
        UNKNOWN,
    }
}
