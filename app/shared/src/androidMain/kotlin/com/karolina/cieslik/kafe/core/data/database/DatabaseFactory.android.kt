package com.karolina.cieslik.kafe.core.data.database

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase

actual class DatabaseFactory(private val context: Context) {

    actual fun create(): RoomDatabase.Builder<KafeDatabase> {
        val appContext = context.applicationContext
        val dbFile = appContext.getDatabasePath(KafeDatabase.DATABASE_NAME)
        return Room.databaseBuilder<KafeDatabase>(
            context = appContext,
            name = dbFile.absolutePath,
        )
    }
}
