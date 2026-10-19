package com.karolina.cieslik.kafe.core.data.database

import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

/** Platformowy builder bazy – ścieżka pliku zależy od systemu (Context na Androidzie, NSDocumentDirectory na iOS). */
expect class DatabaseFactory {
    /** Builder bazy [KafeDatabase.DATABASE_NAME] w katalogu danych aplikacji; bez sterownika i dispatchera. */
    fun create(): RoomDatabase.Builder<KafeDatabase>
}

/**
 * Kończy konfigurację wspólną dla platform: dołączony sterownik SQLite (ta sama wersja na Androidzie i iOS)
 * i zapytania na [Dispatchers.IO].
 */
fun RoomDatabase.Builder<KafeDatabase>.buildKafeDatabase(): KafeDatabase =
    setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()
