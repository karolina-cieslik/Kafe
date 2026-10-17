package com.karolina.cieslik.kafe.core.data.database

import androidx.sqlite.SQLiteException
import com.karolina.cieslik.kafe.core.domain.DataError
import com.karolina.cieslik.kafe.core.domain.Result
import kotlinx.coroutines.CancellationException

// KAFE-2: Odpowiednik safeCall dla bazy – wyjątki SQLite mapowane na DataError.Local.
suspend inline fun <T> safeDbCall(execute: () -> T): Result<T, DataError.Local> =
    try {
        Result.Success(execute())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.Failure(e.toLocalError())
    }

@PublishedApi
internal fun Throwable.toLocalError(): DataError.Local = when {
    this is SQLiteException && isDiskFullMessage(message) -> DataError.Local.DISK_FULL
    else -> DataError.Local.UNKNOWN
}

// Kod SQLITE_FULL (13) – BundledSQLiteDriver zwraca np. "Error code: 13, message: database or disk is full".
internal fun isDiskFullMessage(message: String?): Boolean =
    message != null && ("SQLITE_FULL" in message || "disk is full" in message || "Error code: 13," in message)
