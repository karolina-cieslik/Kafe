package com.karolina.cieslik.kafe.core.data.preferences

import android.content.Context

/** Android: plik w katalogu `filesDir` aplikacji; używany jest applicationContext. */
actual class PreferencesFactory(private val context: Context) {

    /** Ścieżka pliku w `filesDir`. */
    actual fun filePath(): String = context.applicationContext.filesDir.resolve(PREFERENCES_FILE_NAME).absolutePath
}
