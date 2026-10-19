package com.karolina.cieslik.kafe.core.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import okio.Path.Companion.toPath

/** Platformowa ścieżka pliku DataStore Preferences (Context na Androidzie, NSDocumentDirectory na iOS). */
expect class PreferencesFactory {
    /** Bezwzględna ścieżka pliku [PREFERENCES_FILE_NAME] w prywatnym katalogu aplikacji. */
    fun filePath(): String
}

/**
 * Tworzy DataStore Preferences dla pliku z [PreferencesFactory.filePath]. Wywoływać raz na proces – dwie
 * instancje DataStore na ten sam plik rzucają wyjątek przy odczycie, dlatego w Koinie jest single.
 */
fun PreferencesFactory.createDataStore(): DataStore<Preferences> =
    PreferenceDataStoreFactory.createWithPath(produceFile = { filePath().toPath() })

/** Nazwa pliku preferencji aplikacji. */
internal const val PREFERENCES_FILE_NAME = "kafe.preferences_pb"
