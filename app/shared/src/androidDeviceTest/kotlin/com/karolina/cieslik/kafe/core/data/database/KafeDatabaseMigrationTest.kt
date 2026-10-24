package com.karolina.cieslik.kafe.core.data.database

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Rule
import org.junit.runner.RunWith
import kotlin.test.Test
import kotlin.test.assertEquals

/** Migracja 3→4 (AutoMigration) na danych: menu zostaje, tabela koszyka powstaje pusta i działa. */
@RunWith(AndroidJUnit4::class)
class KafeDatabaseMigrationTest {

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val file = instrumentation.targetContext.getDatabasePath("migration-test.db")

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

    @Test
    fun migration3To4KeepsMenuAndCreatesEmptyCart() {
        helper.createDatabase(3).use { connection ->
            connection.execSQL("INSERT INTO menu_categories (id, name, sortOrder) VALUES ('classic', 'Klasyki', 1)")
        }

        helper.runMigrationsAndValidate(4).use { connection ->
            connection.prepare("SELECT name FROM menu_categories").use { statement ->
                statement.step()
                assertEquals("Klasyki", statement.getText(0))
            }
            connection.execSQL(
                "INSERT INTO cart_items (productId, productName, sizeCode, selectionsJson, configurationKey, quantity, " +
                    "unitPriceGrosze) VALUES ('latte', 'Caffè latte', 'M', '[]', 'latte|M|', 1, 1400)",
            )
            connection.prepare("SELECT id, quantity FROM cart_items").use { statement ->
                statement.step()
                assertEquals(1, statement.getLong(0))
                assertEquals(1, statement.getLong(1))
            }
        }
    }
}
