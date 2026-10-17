package com.karolina.cieslik.kafe.core.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import okio.Path.Companion.toPath

// KAFE-31: Platformowa ścieżka pliku DataStore Preferences (Context na Androidzie, NSDocumentDirectory na iOS).
// Instancja musi być jedna na plik – w Koin jako single.
expect class PreferencesFactory {
    fun filePath(): String
}

fun PreferencesFactory.createDataStore(): DataStore<Preferences> =
    PreferenceDataStoreFactory.createWithPath(produceFile = { filePath().toPath() })

internal const val PREFERENCES_FILE_NAME = "kafe.preferences_pb"
