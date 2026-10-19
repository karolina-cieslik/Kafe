package com.karolina.cieslik.kafe.feature.cafe.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/** Zapamiętany wybór lokalu. Interfejs pozwala podstawić fake w testach (bez DataStore na JVM). */
interface SelectedCafeStore {

    /**
     * Id wybranego lokalu; null = użytkownik jeszcze nie wybrał lokalu. Emituje od razu zapisaną wartość
     * i ponownie po każdej zmianie.
     */
    fun observeSelectedCafeId(): Flow<String?>

    /** Zapisuje wybór na stałe – przetrwa restart aplikacji. */
    suspend fun saveSelectedCafeId(cafeId: String)
}

/**
 * [SelectedCafeStore] na DataStore Preferences: zwykła preferencja zamiast tabeli Room – wybór nie jest danymi
 * z serwera i nie wymaga migracji. Kolejne identyczne wartości są pomijane.
 */
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
