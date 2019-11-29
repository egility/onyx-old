/*
 * Copyright (c) Mike Brickman 2014-2018
 */

package org.egility.linux.tools

import jxl.*
import jxl.biff.DisplayFormat
import jxl.format.*
import jxl.format.Alignment
import jxl.format.Border
import jxl.format.BorderLineStyle
import jxl.format.CellFormat
import jxl.write.*
import jxl.write.Colour
import jxl.write.Number
import jxl.write.NumberFormat.COMPLEX_FORMAT
import jxl.write.WritableFont.DEFAULT_POINT_SIZE
import jxl.write.WritableFont.NO_BOLD
import org.egility.library.general.*
import java.io.File
import java.util.*

/**
 * Created by mbrickman on 28/09/17.
 */

fun WritableSheet.addLabeledCell(
    column: Int,
    row: Int,
    item: Int,
    label: String,
    data: Any,
    condition: Boolean = true,
    wrap: Boolean = false
) {
    this.addCell(Number(column, row, item.toDouble()))
    this.addCell(Label(column + 1, row, label))
    if (wrap) {
        this.addCell(column + 2, row, data, condition = condition, format = WorkbookFormats.default.noteFormat)
    } else {
        this.addCell(column + 2, row, data, condition = condition)
    }
}

class WorkbookFormats() {
    val defaultFont = WritableFont(
        WritableFont.ARIAL,
        DEFAULT_POINT_SIZE,
        NO_BOLD,
        false,
        UnderlineStyle.NO_UNDERLINE,
        Colour.GREEN,
        ScriptStyle.NORMAL_SCRIPT
    )

    val redFont = WritableFont(
        WritableFont.ARIAL,
        DEFAULT_POINT_SIZE,
        NO_BOLD,
        false,
        UnderlineStyle.NO_UNDERLINE,
        Colour.RED,
        ScriptStyle.NORMAL_SCRIPT
    )

    class CellFormat(format: DisplayFormat, left: Boolean = false) : WritableCellFormat(format) {
        init {
            if (left) alignment = Alignment.LEFT
        }
    }

    class CellFormatFont(format: DisplayFormat, left: Boolean = false, font: WritableFont) :
        WritableCellFormat(font, format) {
        init {
            if (left) alignment = Alignment.LEFT
        }
    }
    
    val dateFormatRed = CellFormatFont(DateFormat("DD/MM/YYYY"), left = true, font = redFont)
    val dateFormat = CellFormat(DateFormat("DD/MM/YYYY"), left = true)
    val moneyFormat = CellFormat(NumberFormat("£#,##0.00;[RED]-£#,##0.00"), left = false)
    val moneyFormatTop = CellFormat(NumberFormat("£#,##0.00;[RED]-£#,##0.00"), left = false)
        .also { it.setBorder(Border.TOP, BorderLineStyle.THIN) }
    val moneyFormatBottom = CellFormat(NumberFormat("£#,##0.00;[RED]-£#,##0.00"), left = false)
        .also { it.setBorder(Border.BOTTOM, BorderLineStyle.THIN) }
    val moneyFormatBoth = CellFormat(NumberFormat("£#,##0.00;[RED]-£#,##0.00"), left = false)
        .also { it.setBorder(Border.TOP, BorderLineStyle.THIN) }
        .also { it.setBorder(Border.BOTTOM, BorderLineStyle.THIN) }

    val zeroMoneyFormat = CellFormat(NumberFormat("£##0.00;[RED]£-##0.00;\"\"", COMPLEX_FORMAT), left = false)
    val intFormatRed = CellFormatFont(NumberFormat("##0"), left = true, font = redFont)
    val intFormat = CellFormat(NumberFormat("##0"), left = true)
    val intFormatRight = CellFormat(NumberFormat("##0"), left = false)
    val dayFormat = CellFormat(DateFormat("ddd dd"), left = true)
    
    val  noteFormat= WritableCellFormat()
    val  defaultFormat= WritableCellFormat()
    
    init {
        noteFormat.wrap = true 
    }

    companion object {
        var default = WorkbookFormats()

        fun recreate() {
            default = WorkbookFormats()
        }
    }
}

fun WritableSheet.addCell(column: Int, row: Int, data: Any, format: WritableCellFormat? = null, condition: Boolean = true) {

    if (condition) {
        when (data) {
            is Money -> {
                this.addCell(
                    Number(
                        column,
                        row,
                        data.pence.toDouble() / 100.0,
                        format ?: WorkbookFormats.default.moneyFormat
                    )
                )
            }
            is Boolean -> {
                this.addCell(Label(column, row, if (data) "\u2714" else "", format ?: WorkbookFormats.default.defaultFormat))
            }
            is String -> {
                this.addCell(Label(column, row, data, format ?: WorkbookFormats.default.defaultFormat))
            }
            is Int -> {
                this.addCell(Number(column, row, data.toDouble(), format ?: WorkbookFormats.default.intFormat))
            }
            is Date -> {
                if (data.isEmpty()) {
                    doNothing()
                } else {
                    val value = DateTime(column, row, data, format ?: WorkbookFormats.default.dateFormat)
                    this.addCell(value)
                }
            }

        }

    }
}

fun WritableSheet.addHeading(column: Int, row: Int, label: String, width: Double = 0.0, right: Boolean = false) {
    val headingFormat =
        WritableCellFormat(WritableFont(WritableFont.ARIAL, 10, WritableFont.BOLD, false, UnderlineStyle.SINGLE))
    if (right) {
        headingFormat.alignment = Alignment.RIGHT
    }
    this.addCell(Label(column, row, label, headingFormat))
    if (width > 0.0) this.setColumnWidth(column, width)
}

fun WritableSheet.addTitle(column: Int, row: Int, label: String) {
    val titleFormat = WritableCellFormat(WritableFont(WritableFont.ARIAL, 10, WritableFont.BOLD, false))
    this.addCell(Label(column, row, label, titleFormat))
}

fun WritableSheet.addLabel(column: Int, row: Int, label: String, right: Boolean = false) {
    this.addCell(Label(column, row, label))
}

fun WritableSheet.setColumnWidth(columnNumber: Int, inches: Double) {
    val column = this.getColumnView(columnNumber)
    column.size = (inches * 3333.0).toInt()
    this.setColumnView(columnNumber, column)
}

fun WritableSheet.setWidths(vararg inches: Double) {
    for (i in 0 until inches.size) {
        setColumnWidth(i, inches[i])
    }
}

fun WritableWorkbook.headedSheet(name: String, index: Int, identifier: Int, version: Int, title: String): WritableSheet {
    val result = this.createSheet(name, index)
    result.addCell(Label(0, 0, "$identifier.$version"))
    result.addHeading(1, 0, title)
    return result
}

fun WritableWorkbook.quit() {
    this.write()
    this.close()
}

fun createWorkbook(path: String): WritableWorkbook {
    val file = prepareFile(path)
    val workbook = Workbook.createWorkbook(file)
    WorkbookFormats.recreate()
    return workbook
}

fun openWorkbook(path: String): Workbook {
    return Workbook.getWorkbook(File(path))
}

/*
    public static final CellType EMPTY = new CellType("Empty");
    public static final CellType LABEL = new CellType("Label");
    public static final CellType NUMBER = new CellType("Number");
    public static final CellType BOOLEAN = new CellType("Boolean");
    public static final CellType ERROR = new CellType("Error");
    public static final CellType NUMBER_FORMULA = new CellType("Numerical Formula");
    public static final CellType DATE_FORMULA = new CellType("Date Formula");
    public static final CellType STRING_FORMULA = new CellType("String Formula");
    public static final CellType BOOLEAN_FORMULA = new CellType("Boolean Formula");
    public static final CellType FORMULA_ERROR = new CellType("Formula Error");
    public static final CellType DATE = new CellType("Date");
 */


fun Sheet.getLabeledCell(column: Int, item: Int): Cell? {
    for (row in 0 until this.rows) {
        if (getCell(column, row).asInt == item) {
            return getCell(column + 2, row)
        }
    }
    return null
}

val Cell.asString: String
    get() = when (this) {
        is BooleanCell -> if (value) "Yes" else "No"
        is DateCell -> date?.dateText ?: ""
        is LabelCell -> string ?: ""
        is NumberCell -> value.toString()
        else -> ""
    }

val Cell.asBoolean: Boolean
    get() = when (this) {
        is BooleanCell -> value
        is DateCell -> date != null && !date.isEmpty()
        is LabelCell -> string != null && string.toUpperCase().oneOf("Y", "YES", "1", "TRUE", "\u2714")
        is NumberCell -> value != 0.0
        else -> false
    }

val Cell.asDouble: Double
    get() = when (this) {
        is BooleanCell -> if (value) 1.0 else 0.0
        is DateCell -> date?.time?.toDouble() ?: 0.0
        is LabelCell -> if (string != null) string.toDoubleDef(0.0) else 0.0
        is NumberCell -> value
        else -> 0.0
    }

val Cell.asInt: Int
    get() = when (this) {
        is BooleanCell -> if (value) 1 else 0
        is DateCell -> date?.time?.toInt() ?: 0
        is LabelCell -> if (string != null) string.toIntDef(0) else 0
        is NumberCell -> value.toInt()
        else -> 0
    }

val Cell.asDate: Date
    get() =
        when (this) {
            is BooleanCell -> nullDate
            is DateCell -> date ?: nullDate
            is LabelCell -> if (string != null) string.toDate(EXCEL_DATE_FORMAT).dateOnly() else nullDate
            is NumberCell -> nullDate
            else -> nullDate
        }

val Cell.asDateTime: Date
    get() =
        when (this) {
            is BooleanCell -> nullDate
            is DateCell -> date ?: nullDate
            is LabelCell -> if (string != null) string.toDate(EXCEL_DATE_FORMAT) else nullDate
            is NumberCell -> nullDate
            else -> nullDate
        }

val Sheet.identifier: Int
    get() = getCell(0, 0).asString.substringBefore(".").toIntDef(0)

val Sheet.version: Int
    get() = getCell(0, 0).asString.substringAfter(".").toIntDef(0)

val Sheet.title: String
    get() = getCell(1, 0).asString

val Workbook.identifier: Int
    get() = getSheet(0).identifier

val Workbook.version: Int
    get() = getSheet(0).version

val Workbook.title: String
    get() = getSheet(0).title
