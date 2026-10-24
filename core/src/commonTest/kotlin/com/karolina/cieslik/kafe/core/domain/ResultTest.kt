package com.karolina.cieslik.kafe.core.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** Operatory Result. */
class ResultTest {

    @Test
    fun mapTransformsSuccessData() {
        val result: Result<Int, DataError> = Result.Success(2)

        assertEquals(Result.Success("4"), result.map { (it * 2).toString() })
    }

    @Test
    fun mapKeepsFailureUntouched() {
        val result: Result<Int, DataError> = Result.Failure(DataError.Remote.SERVER)

        assertEquals(Result.Failure(DataError.Remote.SERVER), result.map { it * 2 })
    }

    @Test
    fun asEmptyResultDropsData() {
        assertEquals(Result.Success(Unit), Result.Success(42).asEmptyResult())
    }

    @Test
    fun onSuccessAndOnFailureInvokeOnlyMatchingBranch() {
        var data: Int? = null
        var error: DataError? = null

        val result: Result<Int, DataError> = Result.Failure(DataError.Local.DISK_FULL)

        result
            .onSuccess { data = it }
            .onFailure { error = it }

        assertNull(data)
        assertEquals(DataError.Local.DISK_FULL, error)
    }
}
