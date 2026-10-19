package com.karolina.cieslik.kafe.core.data.preferences

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

/** iOS: plik w katalogu dokumentów aplikacji (NSDocumentDirectory). */
actual class PreferencesFactory {

    /** Ścieżka pliku w katalogu dokumentów; rzuca IllegalArgumentException, gdy katalog jest niedostępny. */
    @OptIn(ExperimentalForeignApi::class)
    actual fun filePath(): String {
        val directory = NSFileManager.defaultManager.URLForDirectory(
            directory = NSDocumentDirectory,
            inDomain = NSUserDomainMask,
            appropriateForURL = null,
            create = false,
            error = null,
        )
        return "${requireNotNull(directory?.path) { "Brak dostępu do NSDocumentDirectory" }}/$PREFERENCES_FILE_NAME"
    }
}
