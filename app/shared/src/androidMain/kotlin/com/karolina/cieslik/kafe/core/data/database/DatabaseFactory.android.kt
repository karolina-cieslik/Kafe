package com.karolina.cieslik.kafe.core.data.database

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase

/** Android: plik bazy w katalogu baz aplikacji; [context] może być dowolny, używany jest applicationContext. */
actual class DatabaseFactory(private val context: Context) {

    /** Builder bazy w `getDatabasePath(DATABASE_NAME)`. */
    actual fun create(): RoomDatabase.Builder<KafeDatabase> {
        val appContext = context.applicationContext
        val dbFile = appContext.getDatabasePath(KafeDatabase.DATABASE_NAME)
        return Room.databaseBuilder<KafeDatabase>(
            context = appContext,
            name = dbFile.absolutePath,
        )
    }
}
