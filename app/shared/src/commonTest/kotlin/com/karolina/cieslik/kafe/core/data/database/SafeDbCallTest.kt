package com.karolina.cieslik.kafe.core.data.database

import androidx.sqlite.SQLiteException
import com.karolina.cieslik.kafe.core.domain.DataError
import com.karolina.cieslik.kafe.core.domain.Result
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Mapowanie wyjątków bazy na DataError.Local. */
class SafeDbCallTest {

    @Test
    fun successfulCallReturnsSuccess() = runTest {
        assertEquals(Result.Success(42), safeDbCall { 42 })
    }

    // Na hoście JVM SQLiteException to atrapa android.database.SQLException bez komunikatu,
    // dlatego rozpoznawanie pełnego dysku testujemy na poziomie komunikatu.
    @Test
    fun sqliteFullMessagesAreRecognizedAsDiskFull() {
        assertTrue(isDiskFullMessage("Error code: 13, message: database or disk is full"))
        assertTrue(isDiskFullMessage("SQLITE_FULL"))
        assertFalse(isDiskFullMessage("Error code: 1, message: no such table"))
        assertFalse(isDiskFullMessage(null))
    }

    @Test
    fun otherSqliteErrorReturnsUnknown() = runTest {
        val result = safeDbCall { throw SQLiteException("Error code: 1, message: no such table") }

        assertEquals(Result.Failure(DataError.Local.UNKNOWN), result)
    }

    @Test
    fun cancellationIsRethrown() = runTest {
        assertFailsWith<CancellationException> {
            safeDbCall { throw CancellationException("cancelled") }
        }
    }
}
