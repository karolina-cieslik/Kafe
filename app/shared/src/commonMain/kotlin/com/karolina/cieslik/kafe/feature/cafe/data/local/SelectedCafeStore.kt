package com.karolina.cieslik.kafe.feature.cafe.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

// KAFE-31: Zapamiętany wybór lokalu. Interfejs pozwala podstawić fake w testach (bez DataStore na JVM).
interface SelectedCafeStore {

    /** null = użytkownik jeszcze nie wybrał lokalu. */
    fun observeSelectedCafeId(): Flow<String?>

    suspend fun saveSelectedCafeId(cafeId: String)
}

/** Zwykła preferencja zamiast tabeli Room – wybór nie jest danymi z serwera i nie wymaga migracji. */
class DataStoreSelectedCafeStore(private val dataStore: DataStore<Preferences>) : SelectedCafeStore {

    override fun observeSelectedCafeId(): Flow<String?> =
        dataStore.data.map { it[SELECTED_CAFE_ID] }.distinctUntilChanged()

    override suspend fun saveSelectedCafeId(cafeId: String) {
        dataStore.edit { it[SELECTED_CAFE_ID] = cafeId }
    }

    private companion object {
        val SELECTED_CAFE_ID = stringPreferencesKey("selected_cafe_id")
    }
}
