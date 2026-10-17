package com.karolina.cieslik.kafe.core.data.database

import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

actual class DatabaseFactory {

    actual fun create(): RoomDatabase.Builder<KafeDatabase> {
        val dbFilePath = "${documentDirectory()}/${KafeDatabase.DATABASE_NAME}"
        return Room.databaseBuilder<KafeDatabase>(name = dbFilePath)
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun documentDirectory(): String {
        val directory = NSFileManager.defaultManager.URLForDirectory(
            directory = NSDocumentDirectory,
            inDomain = NSUserDomainMask,
            appropriateForURL = null,
            create = false,
            error = null,
        )
        return requireNotNull(directory?.path) { "Brak dostępu do NSDocumentDirectory" }
    }
}
