package com.farukg.movievault.data.local.db

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.farukg.movievault.data.local.dao.MovieDao
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class Migration2To3Test {

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
    fun migration_2_3_creates_remote_keys_table_and_preserves_existing_data() {
        val helper = createV2Db(dbName)
        val dbV2 = helper.writableDatabase

        dbV2.execSQL(
            """
            INSERT INTO movies(
              id, title, releaseYear, posterUrl, rating, overview, runtimeMinutes, genres, popularRank
            ) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?)
            """
                .trimIndent(),
            arrayOf(42L, "Seeded Movie", 2024, null, 7.5, null, null, "[]", 0),
        )

        dbV2.execSQL(
            "INSERT INTO favorites(movieId, createdAtEpochMillis) VALUES(?, ?)",
            arrayOf(42L, 999L),
        )

        dbV2.execSQL(
            "INSERT INTO cache_metadata(`key`, lastUpdatedEpochMillis) VALUES(?, ?)",
            arrayOf("catalog_last_updated", 1234L),
        )

        assertEquals(2L, queryLong(dbV2, "PRAGMA user_version"))

        dbV2.close()
        helper.close()

        val roomDb =
            Room.databaseBuilder(context, MovieVaultDatabase::class.java, dbName)
                .addMigrations(MIGRATION_2_3)
                .allowMainThreadQueries()
                .build()

        val migratedDb = roomDb.openHelper.writableDatabase

        assertEquals(3L, queryLong(migratedDb, "PRAGMA user_version"))

        assertEquals(
            1L,
            queryLong(
                migratedDb,
                "SELECT COUNT(*) FROM sqlite_master WHERE type='table' AND name='catalog_remote_keys'",
            ),
        )

        val cols = tableColumns(migratedDb, "catalog_remote_keys")

        assertTrue(
            "catalog_remote_keys must contain column 'movieId'",
            cols.any { it.name == "movieId" },
        )
        assertTrue(
            "catalog_remote_keys must contain column 'prevKey'",
            cols.any { it.name == "prevKey" },
        )
        assertTrue(
            "catalog_remote_keys must contain column 'nextKey'",
            cols.any { it.name == "nextKey" },
        )

        val movieIdCol = cols.first { it.name == "movieId" }
        assertEquals("INTEGER", movieIdCol.type.uppercase())
        assertTrue("movieId should be NOT NULL", movieIdCol.notNull)
        assertTrue("movieId should be PRIMARY KEY", movieIdCol.pk)

        val prevCol = cols.first { it.name == "prevKey" }
        assertEquals("INTEGER", prevCol.type.uppercase())
        assertTrue("prevKey should be nullable", !prevCol.notNull)
        assertTrue("prevKey should NOT be PK", !prevCol.pk)

        val nextCol = cols.first { it.name == "nextKey" }
        assertEquals("INTEGER", nextCol.type.uppercase())
        assertTrue("nextKey should be nullable", !nextCol.notNull)
        assertTrue("nextKey should NOT be PK", !nextCol.pk)

        // old tables + rows preserved
        assertEquals(1L, queryLong(migratedDb, "SELECT COUNT(*) FROM movies"))
        assertEquals(1L, queryLong(migratedDb, "SELECT COUNT(*) FROM favorites"))
        assertEquals(1L, queryLong(migratedDb, "SELECT COUNT(*) FROM cache_metadata"))

        val movieDao: MovieDao = roomDb.movieDao()
        val found = runBlockingGetMovie(movieDao, 42L)
        requireNotNull(found)
        assertEquals(42L, found.id)
        assertEquals("Seeded Movie", found.title)

        roomDb.close()
    }

    private fun createV2Db(name: String): SupportSQLiteOpenHelper {
        val factory = FrameworkSQLiteOpenHelperFactory()
        val config =
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(name)
                .callback(
                    object : SupportSQLiteOpenHelper.Callback(2) {
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

                            db.execSQL(
                                """
                                CREATE TABLE IF NOT EXISTS cache_metadata (
                                  `key` TEXT NOT NULL,
                                  `lastUpdatedEpochMillis` INTEGER NOT NULL,
                                  PRIMARY KEY(`key`)
                                )
                                """
                                    .trimIndent()
                            )

                            db.execSQL("PRAGMA user_version = 2")
                        }

                        override fun onUpgrade(
                            db: SupportSQLiteDatabase,
                            oldVersion: Int,
                            newVersion: Int,
                        ) {}
                    }
                )
                .build()

        return factory.create(config)
    }

    private fun runBlockingGetMovie(movieDao: MovieDao, id: Long) =
        kotlinx.coroutines.runBlocking { movieDao.getMovie(id) }
}
