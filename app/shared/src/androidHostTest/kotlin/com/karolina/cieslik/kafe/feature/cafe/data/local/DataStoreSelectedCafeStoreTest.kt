package com.karolina.cieslik.kafe.feature.cafe.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.karolina.cieslik.kafe.core.domain.DataError
import com.karolina.cieslik.kafe.core.domain.Result
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import okio.IOException
import okio.Path.Companion.toOkioPath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assume.assumeFalse
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/** Wybrany lokal jest zapisywany lokalnie i przywracany po ponownym uruchomieniu (DataStore na pliku, JVM). */
class DataStoreSelectedCafeStoreTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val file by lazy { folder.root.resolve("kafe_test.preferences_pb") }

    /** Jedna „sesja aplikacji”: własny DataStore na tym samym pliku, zamykany po bloku (jak zabicie procesu). */
    private fun <T> session(block: suspend (SelectedCafeStore) -> T): T = runBlocking {
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        val dataStore: DataStore<Preferences> = PreferenceDataStoreFactory.createWithPath(scope = scope) { file.toOkioPath() }
        try {
            block(DataStoreSelectedCafeStore(dataStore))
        } finally {
            scope.coroutineContext[kotlinx.coroutines.Job]!!.cancelAndJoin()
        }
    }

    @Test
    fun firstLaunchHasNoSelectedCafe() {
        assertNull(session { it.observeSelectedCafeId().first() })
    }

    @Test
    fun savedCafeIsRestoredAfterRestart() {
        session { it.saveSelectedCafeId("gdansk-dluga") }

        assertEquals("gdansk-dluga", session { it.observeSelectedCafeId().first() })
    }

    @Test
    fun changedCafeOverridesPreviousChoiceAfterRestart() {
        // Na JVM pod Windows zapis z nowej instancji DataStore do istniejącego pliku nie trafia na dysk
        // (kolejna instancja czyta stary stan); Android i CI (ubuntu) – OK. Szczegóły w docs/testing.md.
        assumeFalse("DataStore na JVM/Windows: zapis po ponownym otwarciu pliku", isWindows)
        session { it.saveSelectedCafeId("krakow-rynek") }
        session { it.saveSelectedCafeId("warszawa-nowy-swiat") }

        assertEquals("warszawa-nowy-swiat", session { it.observeSelectedCafeId().first() })
    }

    @Test
    fun savedChoiceIsVisibleImmediatelyInSameSession() {
        val observed = session { store ->
            store.saveSelectedCafeId("krakow-rynek")
            store.observeSelectedCafeId().first()
        }

        assertEquals("krakow-rynek", observed)
    }

    @Test
    fun corruptedPreferencesFileMeansNoSelectedCafe() {
        file.writeBytes(byteArrayOf(0x0A, 0x7F, 0x01))

        assertNull(session { it.observeSelectedCafeId().first() })
    }

    @Test
    fun failedSaveReturnsLocalErrorAndKeepsPreviousChoice() = runBlocking {
        val dataStore = InMemoryPreferencesDataStore()
        val store = DataStoreSelectedCafeStore(dataStore)
        store.saveSelectedCafeId("krakow-rynek")
        dataStore.failWrites = true

        val result = store.saveSelectedCafeId("gdansk-dluga")

        assertEquals(Result.Failure(DataError.Local.UNKNOWN), result)
        assertEquals("krakow-rynek", store.observeSelectedCafeId().first())
    }

    @Test
    fun readErrorMeansNoSelectedCafeAndFlowCompletesNormally() = runBlocking {
        val store = DataStoreSelectedCafeStore(UnreadablePreferencesDataStore)

        assertEquals(listOf<String?>(null), store.observeSelectedCafeId().toList())
    }
}

/** DataStore w pamięci; z [failWrites] zapis rzuca IOException jak przy braku miejsca na dysku. */
private class InMemoryPreferencesDataStore : DataStore<Preferences> {
    private val state = MutableStateFlow(emptyPreferences())
    var failWrites = false

    override val data: Flow<Preferences> = state

    override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences {
        if (failWrites) throw IOException("Brak miejsca na dysku")
        return transform(state.value).also { state.value = it }
    }
}

/** DataStore, którego plik nie daje się odczytać. */
private object UnreadablePreferencesDataStore : DataStore<Preferences> {
    override val data: Flow<Preferences> = flow { throw IOException("Brak dostępu do pliku") }

    override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences =
        throw IOException("Brak dostępu do pliku")
}

private val isWindows = System.getProperty("os.name").orEmpty().startsWith("Windows")
