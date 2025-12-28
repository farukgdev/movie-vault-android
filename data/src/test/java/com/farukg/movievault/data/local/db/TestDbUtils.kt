package com.farukg.movievault.data.local.db

import androidx.sqlite.db.SupportSQLiteDatabase
import org.junit.Assert.assertTrue

internal fun queryLong(db: SupportSQLiteDatabase, sql: String): Long {
    db.query(sql).use { cursor ->
        assertTrue(cursor.moveToFirst())
        return cursor.getLong(0)
    }
}

internal fun tableColumns(db: SupportSQLiteDatabase, table: String): List<ColumnInfo> {
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

internal data class ColumnInfo(
    val name: String,
    val type: String,
    val notNull: Boolean,
    val pk: Boolean,
)
