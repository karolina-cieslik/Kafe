package com.karolina.cieslik.kafe.core.data.database.sync

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.karolina.cieslik.kafe.core.data.database.KafeDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.runner.RunWith
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

// KAFE-2: Test DAO na prawdziwym SQLite (BundledSQLiteDriver, baza w pamięci).
@RunWith(AndroidJUnit4::class)
class SyncMetadataDaoTest {

    private lateinit var database: KafeDatabase
    private lateinit var dao: SyncMetadataDao

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder<KafeDatabase>(context)
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .build()
        dao = database.syncMetadataDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun missingResourceReturnsNull() = runTest {
        assertNull(dao.get("menu"))
    }

    @Test
    fun upsertInsertsAndThenUpdates() = runTest {
        dao.upsert(SyncMetadataEntity(resourceKey = "menu", lastSyncedAtEpochMillis = 1_000))
        dao.upsert(SyncMetadataEntity(resourceKey = "menu", lastSyncedAtEpochMillis = 2_000))

        assertEquals(2_000, dao.get("menu")?.lastSyncedAtEpochMillis)
    }

    @Test
    fun observeEmitsCurrentValue() = runTest {
        dao.upsert(SyncMetadataEntity(resourceKey = "menu", lastSyncedAtEpochMillis = 3_000))

        assertEquals(3_000, dao.observe("menu").first()?.lastSyncedAtEpochMillis)
    }

    @Test
    fun clearRemovesAllEntries() = runTest {
        dao.upsert(SyncMetadataEntity(resourceKey = "menu", lastSyncedAtEpochMillis = 1_000))

        dao.clear()

        assertNull(dao.get("menu"))
    }
}
