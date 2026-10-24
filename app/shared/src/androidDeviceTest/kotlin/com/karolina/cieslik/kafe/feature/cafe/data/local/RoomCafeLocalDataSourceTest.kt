package com.karolina.cieslik.kafe.feature.cafe.data.local

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.karolina.cieslik.kafe.core.data.database.KafeDatabase
import com.karolina.cieslik.kafe.feature.cafe.data.remote.CafeDto
import com.karolina.cieslik.kafe.feature.cafe.data.remote.OpeningHoursDto
import com.karolina.cieslik.kafe.feature.cafe.domain.Cafe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Before
import org.junit.runner.RunWith
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/** Lokalne źródło lokali na prawdziwym SQLite: spójny odczyt względem atomowego zapisu. */
@RunWith(AndroidJUnit4::class)
class RoomCafeLocalDataSourceTest {

    private lateinit var database: KafeDatabase
    private lateinit var dataSource: RoomCafeLocalDataSource

    private val cafesA = listOf(
        CafeDto("krakow", "Kafe Rynek", "Rynek 1", 50.06, 19.94, listOf(OpeningHoursDto("MONDAY", "08:00", "20:00"))),
        CafeDto("gdansk", "Kafe Długa", "Długa 1", 54.35, 18.65),
    )

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder<KafeDatabase>(context)
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .build()
        dataSource = RoomCafeLocalDataSource(database.cafeDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun cafesAreNullBeforeFirstSaveAndReadBackInSourceOrder() = runTest {
        assertNull(dataSource.observeCafes().first())

        dataSource.saveCafes(cafesA, syncedAtEpochMillis = 1)

        val saved = assertNotNull(dataSource.observeCafes().first())
        assertEquals(listOf("krakow", "gdansk"), saved.map { it.id })
    }

    @Test
    fun firstSaveNeverShowsSyncedButEmptyCafes() = runTest {
        repeat(ROUNDS) {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            val freshDatabase = Room.inMemoryDatabaseBuilder<KafeDatabase>(context)
                .setDriver(BundledSQLiteDriver())
                .setQueryCoroutineContext(Dispatchers.IO)
                .build()
            val freshSource = RoomCafeLocalDataSource(freshDatabase.cafeDao())
            val emissions = MutableStateFlow<List<List<Cafe>?>>(emptyList())
            val collector = launch(Dispatchers.IO) {
                freshSource.observeCafes().collect { cafes -> emissions.update { it + listOf(cafes) } }
            }
            withContext(Dispatchers.IO) { withTimeout(5_000) { emissions.first { it.isNotEmpty() } } }

            freshSource.saveCafes(cafesA, syncedAtEpochMillis = 1)
            withContext(Dispatchers.IO) { withTimeout(5_000) { emissions.first { it.lastOrNull()?.size == 2 } } }
            collector.cancel()
            freshDatabase.close()

            assertEquals(
                listOf(null, listOf("krakow", "gdansk")),
                emissions.value.map { cafes -> cafes?.map { it.id } },
            )
        }
    }

    private companion object {
        const val ROUNDS = 10
    }
}
