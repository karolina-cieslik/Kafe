package com.karolina.cieslik.kafe.core.domain

// KAFE-2: Błędy warstwy danych – sieć (Remote) i baza lokalna (Local).
sealed interface DataError : Error {

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

    enum class Local : DataError {
        DISK_FULL,
        NOT_FOUND,
        UNKNOWN,
    }
}
