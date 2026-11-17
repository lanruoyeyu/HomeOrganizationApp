package com.fridgekeeper.data.ledger

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

/**
 * 记账表格的读写入口：导入 iCost 模板数据、导出空白模板。
 */
class LedgerExcelManager(private val context: Context) {

    private val contentResolver = context.applicationContext.contentResolver

    suspend fun importFrom(uri: Uri): LedgerXlsxImporter.Result = withContext(Dispatchers.IO) {
        val rows = contentResolver.openInputStream(uri)?.use { input ->
            XlsxReader.readFirstSheet(input)
        } ?: throw IOException("无法打开所选文件")
        LedgerXlsxImporter.parse(rows)
    }

    suspend fun writeTemplateTo(uri: Uri) = withContext(Dispatchers.IO) {
        val output = contentResolver.openOutputStream(uri, "w")
            ?: throw IOException("无法创建模板文件")
        output.use { LedgerTemplateWriter.write(it) }
    }
}
