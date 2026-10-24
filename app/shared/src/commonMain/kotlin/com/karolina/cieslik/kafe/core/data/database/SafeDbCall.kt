package com.karolina.cieslik.kafe.core.data.database

import androidx.sqlite.SQLiteException
import com.karolina.cieslik.kafe.core.domain.DataError
import com.karolina.cieslik.kafe.core.domain.Result
import kotlinx.coroutines.CancellationException

/**
 * Odpowiednik `safeCall` dla bazy: wykonuje [execute] i zamienia wyjątki na [DataError.Local]
 * ([DataError.Local.DISK_FULL] przy pełnym dysku, w pozostałych przypadkach [DataError.Local.UNKNOWN]).
 * CancellationException jest przepuszczany dalej, żeby nie łamać structured concurrency.
 * Łapie każdy Exception celowo: Room i sterowniki SQLite rzucają różne typy, a wywołujący dostaje zawsze
 * [DataError.Local].
 */
@Suppress("TooGenericExceptionCaught")
suspend inline fun <T> safeDbCall(execute: () -> T): Result<T, DataError.Local> =
    try {
        Result.Success(execute())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.Failure(e.toLocalError())
    }

/** Mapuje wyjątek bazy na [DataError.Local]; publiczne dla inline [safeDbCall]. */
@PublishedApi
internal fun Throwable.toLocalError(): DataError.Local = when {
    this is SQLiteException && isDiskFullMessage(message) -> DataError.Local.DISK_FULL
    else -> DataError.Local.UNKNOWN
}

/**
 * Rozpoznaje błąd SQLITE_FULL (kod 13) po treści komunikatu, bo SQLiteException nie ma pola z kodem.
 * BundledSQLiteDriver zwraca np. "Error code: 13, message: database or disk is full".
 */
internal fun isDiskFullMessage(message: String?): Boolean =
    message != null && ("SQLITE_FULL" in message || "disk is full" in message || "Error code: 13," in message)
