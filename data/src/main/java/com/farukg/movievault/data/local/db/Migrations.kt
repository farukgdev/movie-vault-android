package com.farukg.movievault.data.local.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

internal val MIGRATION_1_2 =
    object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
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
        }
    }

internal val MIGRATION_2_3 =
    object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS catalog_remote_keys (
                    movieId INTEGER NOT NULL,
                    prevKey INTEGER,
                    nextKey INTEGER,
                    PRIMARY KEY(movieId)
                )
                """
                    .trimIndent()
            )
        }
    }
