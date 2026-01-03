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
class Migration3To4Test {

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
    fun migration_3_4_adds_detail_fetched_column_and_preserves_existing_data() {
        val helper = createV3Db(dbName)
        val dbV3 = helper.writableDatabase

        dbV3.execSQL(
            """
            INSERT INTO movies(
              id, title, releaseYear, posterUrl, rating, overview, runtimeMinutes, genres, popularRank
            ) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?)
            """
                .trimIndent(),
            arrayOf(42L, "Seeded Movie", 2024, null, 7.5, null, null, "[]", 0),
        )

        assertEquals(3L, queryLong(dbV3, "PRAGMA user_version"))

        dbV3.close()
        helper.close()

        val roomDb =
            Room.databaseBuilder(context, MovieVaultDatabase::class.java, dbName)
                .addMigrations(MIGRATION_3_4)
                .allowMainThreadQueries()
                .build()

        val migratedDb = roomDb.openHelper.writableDatabase

        assertEquals(4L, queryLong(migratedDb, "PRAGMA user_version"))

        val movieCols = tableColumns(migratedDb, "movies")
        assertTrue(
            "movies must contain column 'detailFetchedAtEpochMillis'",
            movieCols.any { it.name == "detailFetchedAtEpochMillis" },
        )

        val fetchedCol = movieCols.first { it.name == "detailFetchedAtEpochMillis" }
        assertEquals("INTEGER", fetchedCol.type.uppercase())
        assertTrue("detailFetchedAtEpochMillis should be nullable", !fetchedCol.notNull)
        assertTrue("detailFetchedAtEpochMillis should NOT be PK", !fetchedCol.pk)

        assertEquals(1L, queryLong(migratedDb, "SELECT COUNT(*) FROM movies"))

        val movieDao: MovieDao = roomDb.movieDao()
        val found = runBlocking { movieDao.getMovie(42L) }
        requireNotNull(found)
        assertEquals(42L, found.id)
        assertEquals("Seeded Movie", found.title)

        roomDb.close()
    }

    private fun createV3Db(name: String): SupportSQLiteOpenHelper {
        val factory = FrameworkSQLiteOpenHelperFactory()
        val config =
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(name)
                .callback(
                    object : SupportSQLiteOpenHelper.Callback(3) {
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

                            db.execSQL(
                                """
                                CREATE TABLE IF NOT EXISTS catalog_remote_keys (
                                  `movieId` INTEGER NOT NULL,
                                  `prevKey` INTEGER,
                                  `nextKey` INTEGER,
                                  PRIMARY KEY(`movieId`)
                                )
                                """
                                    .trimIndent()
                            )

                            db.execSQL("PRAGMA user_version = 3")
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
}
