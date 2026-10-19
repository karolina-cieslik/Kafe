package com.karolina.cieslik.kafe.feature.cafe.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.karolina.cieslik.kafe.core.domain.DataError
import com.karolina.cieslik.kafe.core.domain.EmptyResult
import com.karolina.cieslik.kafe.core.domain.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import okio.IOException

/** Zapamiętany wybór lokalu. Interfejs pozwala podstawić fake w testach bez pliku DataStore. */
interface SelectedCafeStore {

    /**
     * Id wybranego lokalu; null = użytkownik jeszcze nie wybrał lokalu albo nie da się odczytać pliku preferencji.
     * Emituje od razu zapisaną wartość i ponownie po każdej zmianie; błędem odczytu nie kończy się nigdy.
     */
    fun observeSelectedCafeId(): Flow<String?>

    /**
     * Zapisuje wybór na stałe – przetrwa restart aplikacji.
     *
     * @return sukces albo [DataError.Local.UNKNOWN], gdy nie udało się zapisać pliku (np. brak miejsca);
     * poprzedni wybór zostaje wtedy bez zmian.
     */
    suspend fun saveSelectedCafeId(cafeId: String): EmptyResult<DataError.Local>
}

/**
 * [SelectedCafeStore] na DataStore Preferences: zwykła preferencja zamiast tabeli Room – wybór nie jest danymi
 * z serwera i nie wymaga migracji. Kolejne identyczne wartości są pomijane.
 *
 * Uszkodzony plik zastępuje pustymi preferencjami `corruptionHandler` w `createDataStore`; tutaj zostaje tylko
 * błąd odczytu pliku (IOException), który traktujemy jak brak wyboru – start na wyborze lokalu zamiast crasha.
 */
class DataStoreSelectedCafeStore(private val dataStore: DataStore<Preferences>) : SelectedCafeStore {

    override fun observeSelectedCafeId(): Flow<String?> =
        dataStore.data
            .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
            .map { it[SELECTED_CAFE_ID] }
            .distinctUntilChanged()

    override suspend fun saveSelectedCafeId(cafeId: String): EmptyResult<DataError.Local> =
        try {
            dataStore.edit { it[SELECTED_CAFE_ID] = cafeId }
            Result.Success(Unit)
        } catch (e: IOException) {
            Result.Failure(DataError.Local.UNKNOWN)
        }

    private companion object {
        val SELECTED_CAFE_ID = stringPreferencesKey("selected_cafe_id")
    }
}
