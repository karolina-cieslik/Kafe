package com.karolina.cieslik.kafe.core.data.database

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteException
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Rule
import org.junit.runner.RunWith
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/** Migracja 3→4 na danych spoza menu (lokale, godziny, synchronizacja) i ograniczenia nowej tabeli koszyka. */
@RunWith(AndroidJUnit4::class)
class KafeDatabaseMigrationEdgeTest {

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val file = instrumentation.targetContext.getDatabasePath("migration-edge-test.db")

    @get:Rule
    val helper = MigrationTestHelper(
        instrumentation = instrumentation,
        file = file,
        driver = BundledSQLiteDriver(),
        databaseClass = KafeDatabase::class,
    )

    @After
    fun tearDown() {
        file.delete()
    }

    private fun SQLiteConnection.count(table: String): Long =
        prepare("SELECT COUNT(*) FROM $table").use { statement ->
            statement.step()
            statement.getLong(0)
        }

    private fun SQLiteConnection.insertCartItem(key: String) = execSQL(
        "INSERT INTO cart_items (productId, productName, sizeCode, selectionsJson, configurationKey, quantity, " +
            "unitPriceGrosze) VALUES ('latte', 'Caffè latte', 'M', '[]', '$key', 1, 1400)",
    )

    @Test
    fun migrationKeepsCafesOpeningHoursAndSyncMetadata() {
        helper.createDatabase(3).use { connection ->
            connection.execSQL(
                "INSERT INTO cafes (id, name, address, lat, lng, position) " +
                    "VALUES ('krakow', 'Kafe Rynek', 'Rynek 1', 50.06, 19.94, 1)",
            )
            connection.execSQL(
                "INSERT INTO cafe_opening_hours (cafeId, day, opens, closes) " +
                    "VALUES ('krakow', 'MONDAY', '07:00', '19:00')",
            )
            connection.execSQL(
                "INSERT INTO sync_metadata (resourceKey, lastSyncedAtEpochMillis) VALUES ('menu', 1761300000000)",
            )
        }

        helper.runMigrationsAndValidate(4).use { connection ->
            assertEquals(1, connection.count("cafes"))
            assertEquals(1, connection.count("cafe_opening_hours"))
            val syncQuery = "SELECT lastSyncedAtEpochMillis FROM sync_metadata WHERE resourceKey = 'menu'"
            connection.prepare(syncQuery).use { statement ->
                statement.step()
                assertEquals(1_761_300_000_000, statement.getLong(0))
            }
            assertEquals(0, connection.count("cart_items"))
        }
    }

    @Test
    fun migratedCartTableRejectsDuplicateConfigurationKey() {
        helper.createDatabase(3).close()

        helper.runMigrationsAndValidate(4).use { connection ->
            connection.insertCartItem("latte|M|")

            assertFailsWith<SQLiteException> { connection.insertCartItem("latte|M|") }
            assertEquals(1, connection.count("cart_items"))
        }
    }

    @Test
    fun migratedCartTableDoesNotReuseIdsAfterDeletingAll() {
        helper.createDatabase(3).close()

        helper.runMigrationsAndValidate(4).use { connection ->
            connection.insertCartItem("a")
            connection.insertCartItem("b")
            connection.execSQL("DELETE FROM cart_items")
            connection.insertCartItem("c")

            connection.prepare("SELECT id FROM cart_items").use { statement ->
                statement.step()
                assertEquals(3, statement.getLong(0))
            }
        }
    }
}
