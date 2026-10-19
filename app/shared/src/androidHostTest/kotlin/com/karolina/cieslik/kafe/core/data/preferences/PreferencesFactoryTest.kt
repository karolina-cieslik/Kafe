package com.karolina.cieslik.kafe.core.data.preferences

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.karolina.cieslik.kafe.core.domain.Result
import com.karolina.cieslik.kafe.core.testing.ROBOLECTRIC_SDK
import com.karolina.cieslik.kafe.feature.cafe.data.local.DataStoreSelectedCafeStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.File

/** DataStore aplikacji z [createDataStore] zastępuje uszkodzony plik preferencji pustym, zamiast blokować zapis (Robolectric). */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [ROBOLECTRIC_SDK])
class PreferencesFactoryTest {

    @Test
    fun corruptedFileIsReplacedSoNewChoiceCanBeSaved() = runBlocking {
        val factory = PreferencesFactory(ApplicationProvider.getApplicationContext())
        File(factory.filePath()).apply { parentFile?.mkdirs() }.writeBytes(byteArrayOf(0x0A, 0x7F, 0x01))
        val store = DataStoreSelectedCafeStore(factory.createDataStore())

        val result = store.saveSelectedCafeId("krakow-rynek")

        assertEquals(Result.Success(Unit), result)
        assertEquals("krakow-rynek", store.observeSelectedCafeId().first())
    }
}
