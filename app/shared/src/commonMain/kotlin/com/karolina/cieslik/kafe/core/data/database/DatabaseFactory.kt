package com.karolina.cieslik.kafe.core.data.database

import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

// KAFE-2: Platformowy builder bazy (ścieżka pliku zależy od systemu – Context na Androidzie, NSDocumentDirectory na iOS).
expect class DatabaseFactory {
    fun create(): RoomDatabase.Builder<KafeDatabase>
}

fun RoomDatabase.Builder<KafeDatabase>.buildKafeDatabase(): KafeDatabase =
    setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()
