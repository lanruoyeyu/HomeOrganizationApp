package com.fridgekeeper.data.backup

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.SQLException
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import androidx.room.withTransaction
import com.fridgekeeper.data.local.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException

/** 全量业务数据备份，不包含 API Key 等应用设置。 */
class DataBackupManager(
    context: Context,
    private val database: AppDatabase
) {
    private val contentResolver = context.applicationContext.contentResolver

    suspend fun exportData(destination: Uri): Int = withContext(Dispatchers.IO) {
        val (json, count) = database.withTransaction {
            val db = database.openHelper.writableDatabase
            val tables = JSONObject()
            var count = 0
            TABLES.forEach { table ->
                val rows = readTable(db, table)
                tables.put(table, rows)
                count += rows.length()
            }
            JSONObject()
                .put(KEY_APP, APP_ID)
                .put(KEY_FORMAT_VERSION, FORMAT_VERSION)
                .put(KEY_DATABASE_VERSION, db.version)
                .put(KEY_EXPORTED_AT, System.currentTimeMillis())
                .put(KEY_TABLES, tables) to count
        }

        val output = contentResolver.openOutputStream(destination, "w")
            ?: throw IOException("无法打开导出文件")
        output.bufferedWriter(Charsets.UTF_8).use { writer ->
            writer.write(json.toString(2))
        }
        count
    }

    suspend fun importData(source: Uri): Int = withContext(Dispatchers.IO) {
        val input = contentResolver.openInputStream(source)
            ?: throw IOException("无法打开导入文件")
        val text = input.bufferedReader(Charsets.UTF_8).use { it.readText() }
        val root = try {
            JSONObject(text)
        } catch (error: JSONException) {
            throw IllegalArgumentException("文件不是有效的 JSON 备份", error)
        }

        val db = database.openHelper.writableDatabase
        val backup = parseBackup(root, db.version, db)
        database.withTransaction {
            TABLES.asReversed().forEach { table ->
                db.delete(table, null, null)
            }
            db.delete("sqlite_sequence", null, null)
            backup.forEach { table ->
                table.rows.forEach { values ->
                    val result = db.insert(table.name, SQLiteDatabase.CONFLICT_ABORT, values)
                    if (result == -1L) throw SQLException("恢复 ${table.name} 数据失败")
                }
            }
        }
        backup.sumOf { it.rows.size }
    }

    private fun readTable(db: androidx.sqlite.db.SupportSQLiteDatabase, table: String) =
        org.json.JSONArray().apply {
            db.query("SELECT * FROM `$table`").use { cursor ->
                val columns = cursor.columnNames
                while (cursor.moveToNext()) {
                    val row = JSONObject()
                    columns.forEachIndexed { index, column ->
                        val value = when (cursor.getType(index)) {
                            Cursor.FIELD_TYPE_NULL -> JSONObject.NULL
                            Cursor.FIELD_TYPE_INTEGER -> cursor.getLong(index)
                            Cursor.FIELD_TYPE_FLOAT -> cursor.getDouble(index)
                            Cursor.FIELD_TYPE_STRING -> cursor.getString(index)
                            else -> throw IOException("备份暂不支持二进制字段")
                        }
                        row.put(column, value)
                    }
                    put(row)
                }
            }
        }

    private fun parseBackup(
        root: JSONObject,
        currentDatabaseVersion: Int,
        db: androidx.sqlite.db.SupportSQLiteDatabase
    ): List<TableBackup> {
        if (root.optString(KEY_APP) != APP_ID) {
            throw IllegalArgumentException("这不是本应用的数据备份")
        }
        if (root.optInt(KEY_FORMAT_VERSION, -1) != FORMAT_VERSION) {
            throw IllegalArgumentException("备份格式版本不受支持")
        }
        if (root.optInt(KEY_DATABASE_VERSION, -1) != currentDatabaseVersion) {
            throw IllegalArgumentException("备份数据版本与当前应用不兼容")
        }
        val tables = root.optJSONObject(KEY_TABLES)
            ?: throw IllegalArgumentException("备份缺少数据表")

        return TABLES.map { table ->
            val rows = tables.optJSONArray(table)
                ?: throw IllegalArgumentException("备份缺少 $table 数据")
            val columns = readColumnNames(db, table)
            val values = (0 until rows.length()).map { index ->
                val row = rows.getJSONObject(index)
                ContentValues().apply {
                    row.keys().forEach { column ->
                        if (column !in columns) {
                            throw IllegalArgumentException("备份字段与当前应用不兼容")
                        }
                        when (val value = row.get(column)) {
                            JSONObject.NULL -> putNull(column)
                            is String -> put(column, value)
                            is Boolean -> put(column, value)
                            is Int -> put(column, value)
                            is Long -> put(column, value)
                            is Float -> put(column, value)
                            is Double -> put(column, value)
                            else -> throw IllegalArgumentException("备份包含不支持的字段值")
                        }
                    }
                }
            }
            TableBackup(table, values)
        }
    }

    private fun readColumnNames(
        db: androidx.sqlite.db.SupportSQLiteDatabase,
        table: String
    ): Set<String> = buildSet {
        db.query("PRAGMA table_info('$table')").use { cursor ->
            val nameIndex = cursor.getColumnIndexOrThrow("name")
            while (cursor.moveToNext()) add(cursor.getString(nameIndex))
        }
    }

    private data class TableBackup(
        val name: String,
        val rows: List<ContentValues>
    )

    private companion object {
        const val APP_ID = "com.fridgekeeper"
        const val FORMAT_VERSION = 1
        const val KEY_APP = "app"
        const val KEY_FORMAT_VERSION = "formatVersion"
        const val KEY_DATABASE_VERSION = "databaseVersion"
        const val KEY_EXPORTED_AT = "exportedAt"
        const val KEY_TABLES = "tables"

        // 父表在前；清空时反向执行，保留所有原始 ID 与关联关系。
        val TABLES = listOf(
            "categories",
            "subcategories",
            "locations",
            "pet_categories",
            "map_places",
            "items",
            "digital_items",
            "pet_items",
            "medicines",
            "map_markers",
            "usage_records",
            "ledger_entries",
            "coupons",
            "checkin_tasks",
            "checkin_records"
        )
    }
}
