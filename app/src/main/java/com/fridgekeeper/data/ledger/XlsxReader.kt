package com.fridgekeeper.data.ledger

import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.util.zip.ZipInputStream

/**
 * 极简 xlsx 读取器：只解析第一个工作表，支持共享字符串、内联字符串和数字单元格。
 * 使用系统自带的 ZipInputStream 与 XmlPullParser，不引入额外依赖。
 */
object XlsxReader {

    fun readFirstSheet(input: InputStream): List<List<String>> {
        val files = readZip(input)
        val sharedStrings = files["xl/sharedStrings.xml"]
            ?.let(::parseSharedStrings)
            .orEmpty()
        val sheetPath = findFirstSheetPath(files)
        val sheet = files[sheetPath]
            ?: throw IllegalArgumentException("表格中找不到工作表")
        return parseSheet(sheet, sharedStrings)
    }

    private fun readZip(input: InputStream): Map<String, ByteArray> {
        val result = LinkedHashMap<String, ByteArray>()
        ZipInputStream(input.buffered()).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                if (!entry.isDirectory) {
                    result[entry.name.trimStart('/')] = zip.readBytes()
                }
                zip.closeEntry()
            }
        }
        return result
    }

    private fun findFirstSheetPath(files: Map<String, ByteArray>): String {
        var relationId: String? = null
        files["xl/workbook.xml"]?.let { bytes ->
            val parser = newParser(bytes)
            var event = parser.eventType
            while (event != XmlPullParser.END_DOCUMENT) {
                if (event == XmlPullParser.START_TAG && tagName(parser) == "sheet") {
                    relationId = attribute(parser, "id")
                    break
                }
                event = parser.next()
            }
        }

        val rels = files["xl/_rels/workbook.xml.rels"]
        if (relationId != null && rels != null) {
            val parser = newParser(rels)
            var event = parser.eventType
            while (event != XmlPullParser.END_DOCUMENT) {
                if (event == XmlPullParser.START_TAG && tagName(parser) == "Relationship") {
                    if (attribute(parser, "Id") == relationId) {
                        val target = attribute(parser, "Target").orEmpty()
                        resolveTarget(target)?.let { path ->
                            if (files.containsKey(path)) return path
                        }
                    }
                }
                event = parser.next()
            }
        }

        return files.keys
            .filter { it.startsWith("xl/worksheets/") && it.endsWith(".xml") }
            .minOrNull()
            ?: throw IllegalArgumentException("表格中找不到工作表")
    }

    private fun resolveTarget(target: String): String? {
        if (target.isBlank()) return null
        val cleaned = target.trimStart('/')
        return when {
            cleaned.startsWith("xl/") -> cleaned
            cleaned.startsWith("worksheets/") -> "xl/$cleaned"
            else -> "xl/$cleaned"
        }
    }

    private fun parseSharedStrings(bytes: ByteArray): List<String> {
        val result = mutableListOf<String>()
        val parser = newParser(bytes)
        val current = StringBuilder()
        var inItem = false
        var phoneticDepth = 0
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> when (tagName(parser)) {
                    "si" -> {
                        current.setLength(0)
                        inItem = true
                    }
                    "rPh" -> phoneticDepth++
                    "t" -> Unit
                }
                XmlPullParser.TEXT -> if (inItem && phoneticDepth == 0) {
                    current.append(parser.text)
                }
                XmlPullParser.END_TAG -> when (tagName(parser)) {
                    "rPh" -> if (phoneticDepth > 0) phoneticDepth--
                    "si" -> {
                        result += current.toString()
                        inItem = false
                    }
                }
            }
            event = parser.next()
        }
        return result
    }

    private fun parseSheet(bytes: ByteArray, sharedStrings: List<String>): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        val parser = newParser(bytes)
        var cells = LinkedHashMap<Int, String>()
        var nextColumn = 0
        var column = -1
        var cellType: String? = null
        var inCell = false
        var inValue = false
        val buffer = StringBuilder()

        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> when (tagName(parser)) {
                    "row" -> {
                        cells = LinkedHashMap()
                        nextColumn = 0
                    }
                    "c" -> {
                        inCell = true
                        inValue = false
                        buffer.setLength(0)
                        cellType = attribute(parser, "t")
                        val ref = attribute(parser, "r")
                        column = if (ref.isNullOrBlank()) nextColumn else columnIndex(ref)
                        nextColumn = column + 1
                    }
                    "v", "t" -> if (inCell) inValue = true
                }
                XmlPullParser.TEXT -> if (inCell && inValue) {
                    buffer.append(parser.text)
                }
                XmlPullParser.END_TAG -> when (tagName(parser)) {
                    "v", "t" -> inValue = false
                    "c" -> {
                        if (inCell && column >= 0) {
                            cells[column] = decodeCell(cellType, buffer.toString(), sharedStrings)
                        }
                        inCell = false
                        inValue = false
                    }
                    "row" -> rows += toRow(cells)
                }
            }
            event = parser.next()
        }
        return rows
    }

    private fun decodeCell(type: String?, raw: String, sharedStrings: List<String>): String =
        when (type) {
            "s" -> raw.trim().toIntOrNull()?.let { sharedStrings.getOrNull(it) }.orEmpty()
            "b" -> if (raw.trim() == "1") "TRUE" else "FALSE"
            else -> raw
        }

    private fun toRow(cells: Map<Int, String>): List<String> {
        if (cells.isEmpty()) return emptyList()
        val size = (cells.keys.maxOrNull() ?: 0) + 1
        return List(size) { cells[it].orEmpty() }
    }

    private fun columnIndex(reference: String): Int {
        var value = 0
        var found = false
        for (char in reference) {
            if (char.isLetter()) {
                value = value * 26 + (char.uppercaseChar() - 'A' + 1)
                found = true
            } else {
                break
            }
        }
        return if (found) value - 1 else 0
    }

    private fun newParser(bytes: ByteArray): XmlPullParser {
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(ByteArrayInputStream(bytes), "UTF-8")
        return parser
    }

    private fun tagName(parser: XmlPullParser): String = parser.name.substringAfter(':')

    private fun attribute(parser: XmlPullParser, name: String): String? {
        for (index in 0 until parser.attributeCount) {
            if (parser.getAttributeName(index).substringAfter(':') == name) {
                return parser.getAttributeValue(index)
            }
        }
        return null
    }
}
