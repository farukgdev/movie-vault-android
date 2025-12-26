package com.farukg.movievault.data.local.db

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.farukg.movievault.data.local.dao.MovieDao
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class Migration1To2Test {

    private val dbName = "movievault-migration-test.db"
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase(dbName)
    }

    @After
    fun tearDown() {
        context.deleteDatabase(dbName)
    }

    @Test
    fun migration_1_2_creates_cache_metadata_and_preserves_existing_data() {
        val helper = createV1Db(dbName)
        val dbV1 = helper.writableDatabase

        dbV1.execSQL(
            """
            INSERT INTO movies(
              id, title, releaseYear, posterUrl, rating, overview, runtimeMinutes, genres, popularRank
            ) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?)
            """
                .trimIndent(),
            arrayOf(42L, "Seeded Movie", 2024, null, 7.5, null, null, "[]", 0),
        )

        dbV1.execSQL(
            "INSERT INTO favorites(movieId, createdAtEpochMillis) VALUES(?, ?)",
            arrayOf(42L, 999L),
        )

        assertEquals(1L, queryLong(dbV1, "PRAGMA user_version"))

        dbV1.close()
        helper.close()

        // Apply migration
        val roomDb =
            Room.databaseBuilder(context, MovieVaultDatabase::class.java, dbName)
                .addMigrations(MIGRATION_1_2)
                .allowMainThreadQueries()
                .build()

        val migratedDb = roomDb.openHelper.writableDatabase

        assertEquals(2L, queryLong(migratedDb, "PRAGMA user_version"))

        // Verify cache_metadata table exists
        assertEquals(
            1L,
            queryLong(
                migratedDb,
                "SELECT COUNT(*) FROM sqlite_master WHERE type='table' AND name='cache_metadata'",
            ),
        )

        // Verify cache_metadata table columns are exactly what entity expects
        val cacheCols = tableColumns(migratedDb, "cache_metadata")
        assertTrue("cache_metadata must contain column 'key'", cacheCols.any { it.name == "key" })
        assertTrue(
            "cache_metadata must contain column 'lastUpdatedEpochMillis'",
            cacheCols.any { it.name == "lastUpdatedEpochMillis" },
        )

        val keyCol = cacheCols.first { it.name == "key" }
        assertEquals("TEXT", keyCol.type.uppercase())
        assertTrue("key should be NOT NULL", keyCol.notNull)
        assertTrue("key should be PRIMARY KEY", keyCol.pk)

        val updatedCol = cacheCols.first { it.name == "lastUpdatedEpochMillis" }
        assertEquals("INTEGER", updatedCol.type.uppercase())
        assertTrue("lastUpdatedEpochMillis should be NOT NULL", updatedCol.notNull)
        assertTrue("lastUpdatedEpochMillis should NOT be PRIMARY KEY", !updatedCol.pk)

        // Verify old tables and rows are preserved
        assertEquals(1L, queryLong(migratedDb, "SELECT COUNT(*) FROM movies"))
        assertEquals(1L, queryLong(migratedDb, "SELECT COUNT(*) FROM favorites"))

        val movieDao: MovieDao = roomDb.movieDao()
        val found = runBlocking { movieDao.getMovie(42L) }
        requireNotNull(found)
        assertEquals(42L, found.id)
        assertEquals("Seeded Movie", found.title)

        roomDb.close()
    }

    private fun createV1Db(name: String): SupportSQLiteOpenHelper {
        val factory = FrameworkSQLiteOpenHelperFactory()
        val config =
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(name)
                .callback(
                    object : SupportSQLiteOpenHelper.Callback(1) {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            db.execSQL(
                                """
                                CREATE TABLE IF NOT EXISTS movies (
                                  `id` INTEGER NOT NULL,
                                  `title` TEXT NOT NULL,
                                  `releaseYear` INTEGER,
                                  `posterUrl` TEXT,
                                  `rating` REAL,
                                  `overview` TEXT,
                                  `runtimeMinutes` INTEGER,
                                  `genres` TEXT NOT NULL,
                                  `popularRank` INTEGER NOT NULL,
                                  PRIMARY KEY(`id`)
                                )
                                """
                                    .trimIndent()
                            )

                            db.execSQL(
                                """
                                CREATE TABLE IF NOT EXISTS favorites (
                                  `movieId` INTEGER NOT NULL,
                                  `createdAtEpochMillis` INTEGER NOT NULL,
                                  PRIMARY KEY(`movieId`)
                                )
                                """
                                    .trimIndent()
                            )
                        }

                        override fun onUpgrade(
                            db: SupportSQLiteDatabase,
                            oldVersion: Int,
                            newVersion: Int,
                        ) {
                            // no-op for v1 helper
                        }
                    }
                )
                .build()

        return factory.create(config)
    }

    private fun queryLong(db: SupportSQLiteDatabase, sql: String): Long {
        db.query(sql).use { cursor ->
            assertTrue(cursor.moveToFirst())
            return cursor.getLong(0)
        }
    }

    private fun tableColumns(db: SupportSQLiteDatabase, table: String): List<ColumnInfo> {
        val cols = mutableListOf<ColumnInfo>()
        db.query("PRAGMA table_info($table)").use { c ->
            val nameIx = c.getColumnIndexOrThrow("name")
            val typeIx = c.getColumnIndexOrThrow("type")
            val notNullIx = c.getColumnIndexOrThrow("notnull")
            val pkIx = c.getColumnIndexOrThrow("pk")

            while (c.moveToNext()) {
                cols +=
                    ColumnInfo(
                        name = c.getString(nameIx),
                        type = c.getString(typeIx),
                        notNull = c.getInt(notNullIx) == 1,
                        pk = c.getInt(pkIx) == 1,
                    )
            }
        }
        return cols
    }

    private data class ColumnInfo(
        val name: String,
        val type: String,
        val notNull: Boolean,
        val pk: Boolean,
    )
}
