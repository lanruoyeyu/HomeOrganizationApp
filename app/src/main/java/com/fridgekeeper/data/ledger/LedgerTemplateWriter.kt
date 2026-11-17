package com.fridgekeeper.data.ledger

import java.io.BufferedOutputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * 生成 iCost 记账模板，表头与 iCost 导出的账单保持一致。
 */
object LedgerTemplateWriter {

    private val HEADERS = listOf(
        "日期", "类型", "金额", "一级分类", "二级分类", "账户1", "账户2", "备注",
        "货币", "标签", "账本", "退款", "优惠", "手续费",
        "图片链接1", "图片链接2", "图片链接3", "图片附件1", "图片附件2", "图片附件3"
    )

    private val COLUMN_WIDTHS = listOf(18, 10, 10, 12, 12, 14, 14, 24, 8, 12, 12, 8, 8, 8, 12, 12, 12, 12, 12, 12)

    fun write(output: OutputStream) {
        ZipOutputStream(BufferedOutputStream(output)).use { zip ->
            put(zip, "[Content_Types].xml", CONTENT_TYPES)
            put(zip, "_rels/.rels", ROOT_RELS)
            put(zip, "xl/workbook.xml", WORKBOOK)
            put(zip, "xl/_rels/workbook.xml.rels", WORKBOOK_RELS)
            put(zip, "xl/styles.xml", STYLES)
            put(
                zip,
                "xl/worksheets/sheet1.xml",
                buildSheet(listOf(HEADERS), headerRow = true, widths = COLUMN_WIDTHS)
            )
            put(
                zip,
                "xl/worksheets/sheet2.xml",
                buildSheet(instructionRows(), headerRow = true, widths = listOf(18, 60))
            )
            put(
                zip,
                "xl/worksheets/sheet3.xml",
                buildSheet(categoryRows(), headerRow = true, widths = listOf(16, 16, 10))
            )
        }
    }

    private fun instructionRows(): List<List<String>> = listOf(
        listOf("字段", "填写说明"),
        listOf("日期", "必填，格式 2025/06/24 22:31，也可以只填日期 2025/06/24"),
        listOf("类型", "支出 / 收入 / 转账；分期可填 分期本金、分期利息，还款、充值也能识别"),
        listOf("金额", "必填，支出填负数（如 -22.5），收入填正数"),
        listOf("一级分类", "参考「分类参考」工作表，支出如 餐饮、日用，收入如 工资"),
        listOf("二级分类", "选填，如 午餐、猫粮"),
        listOf("账户1", "如 微信、支付宝、广发信用卡"),
        listOf("账户2", "选填，转账、还款时填写对方账户"),
        listOf("备注", "选填，简短说明这笔账"),
        listOf("货币", "默认 CNY"),
        listOf("标签", "选填，如 #淘宝"),
        listOf("账本", "选填，默认 日常账本"),
        listOf("退款/优惠/手续费", "选填，填数字"),
        listOf("图片链接/图片附件", "暂不导入，可以留空"),
        listOf("导入说明", "导入时按表头名称识别列，记录会追加到现有记账")
    )

    private fun categoryRows(): List<List<String>> {
        val rows = mutableListOf(listOf("一级分类", "二级分类", "类型"))
        LedgerTaxonomy.EXPENSE_CATEGORIES.forEach { category ->
            appendCategory(rows, category, "支出")
        }
        LedgerTaxonomy.INCOME_CATEGORIES.forEach { category ->
            appendCategory(rows, category, "收入")
        }
        LedgerTaxonomy.TRANSFER_CATEGORIES.forEach { category ->
            appendCategory(rows, category, "转账")
        }
        return rows
    }

    private fun appendCategory(rows: MutableList<List<String>>, category: String, type: String) {
        val subcategories = LedgerTaxonomy.subcategoriesFor(category)
        if (subcategories.isEmpty()) {
            rows += listOf(category, "", type)
        } else {
            subcategories.forEach { sub -> rows += listOf(category, sub, type) }
        }
    }

    private fun buildSheet(
        rows: List<List<String>>,
        headerRow: Boolean,
        widths: List<Int>
    ): String {
        val builder = StringBuilder()
        builder.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        builder.append("""<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">""")
        if (widths.isNotEmpty()) {
            builder.append("<cols>")
            widths.forEachIndexed { index, width ->
                builder.append(
                    """<col min="${index + 1}" max="${index + 1}" width="$width" customWidth="1"/>"""
                )
            }
            builder.append("</cols>")
        }
        builder.append("<sheetData>")
        rows.forEachIndexed { rowIndex, row ->
            builder.append("""<row r="${rowIndex + 1}">""")
            row.forEachIndexed { columnIndex, value ->
                if (value.isEmpty()) return@forEachIndexed
                val style = if (headerRow && rowIndex == 0) """ s="1"""" else ""
                builder.append(
                    """<c r="${columnName(columnIndex)}${rowIndex + 1}"$style t="inlineStr">""" +
                        """<is><t xml:space="preserve">${escape(value)}</t></is></c>"""
                )
            }
            builder.append("</row>")
        }
        builder.append("</sheetData></worksheet>")
        return builder.toString()
    }

    private fun columnName(index: Int): String {
        var value = index + 1
        val name = StringBuilder()
        while (value > 0) {
            val remainder = (value - 1) % 26
            name.append(('A' + remainder))
            value = (value - 1) / 26
        }
        return name.reverse().toString()
    }

    private fun escape(value: String): String = value
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;")

    private fun put(zip: ZipOutputStream, path: String, content: String) {
        zip.putNextEntry(ZipEntry(path))
        zip.write(content.toByteArray(Charsets.UTF_8))
        zip.closeEntry()
    }

    private val CONTENT_TYPES = """
        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
        <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
        <Default Extension="xml" ContentType="application/xml"/>
        <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
        <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
        <Override PartName="/xl/worksheets/sheet2.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
        <Override PartName="/xl/worksheets/sheet3.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
        <Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
        </Types>
    """.trimIndent()

    private val ROOT_RELS = """
        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
        <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
        </Relationships>
    """.trimIndent()

    private val WORKBOOK = """
        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
        <sheets>
        <sheet name="iCost账单" sheetId="1" r:id="rId1"/>
        <sheet name="填写说明" sheetId="2" r:id="rId2"/>
        <sheet name="分类参考" sheetId="3" r:id="rId3"/>
        </sheets>
        </workbook>
    """.trimIndent()

    private val WORKBOOK_RELS = """
        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
        <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
        <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet2.xml"/>
        <Relationship Id="rId3" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet3.xml"/>
        <Relationship Id="rId4" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
        </Relationships>
    """.trimIndent()

    private val STYLES = """
        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
        <fonts count="2">
        <font><sz val="11"/><name val="Calibri"/><family val="2"/></font>
        <font><b/><sz val="11"/><name val="Calibri"/><family val="2"/></font>
        </fonts>
        <fills count="2">
        <fill><patternFill patternType="none"/></fill>
        <fill><patternFill patternType="gray125"/></fill>
        </fills>
        <borders count="1"><border><left/><right/><top/><bottom/><diagonal/></border></borders>
        <cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs>
        <cellXfs count="2">
        <xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/>
        <xf numFmtId="0" fontId="1" fillId="0" borderId="0" xfId="0" applyFont="1"/>
        </cellXfs>
        <cellStyles count="1"><cellStyle name="Normal" xfId="0" builtinId="0"/></cellStyles>
        </styleSheet>
    """.trimIndent()
}
