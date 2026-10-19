package com.karolina.cieslik.kafe.feature.cafe.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okio.Path.Companion.toOkioPath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assume.assumeFalse
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

// KAFE-31: Wybrany lokal jest zapisywany lokalnie i przywracany po ponownym uruchomieniu (DataStore na pliku, JVM).
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

    // Drugi zapis do istniejącego pliku DataStore na JVM pod Windows kończy się „Unable to rename …tmp”
    // (problem DataStore/okio z podmianą pliku na Windows, występuje też w jednej sesji). Android i CI (ubuntu) – OK.
    @Test
    fun changedCafeOverridesPreviousChoiceAfterRestart() {
        assumeFalse("DataStore na JVM/Windows nie nadpisuje pliku", isWindows)
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
}

private val isWindows = System.getProperty("os.name").orEmpty().startsWith("Windows")
