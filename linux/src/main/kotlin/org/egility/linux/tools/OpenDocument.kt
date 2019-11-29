/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.linux.tools

import org.odftoolkit.odfdom.dom.OdfDocumentNamespace
import org.odftoolkit.odfdom.dom.OdfSchemaDocument
import org.odftoolkit.odfdom.dom.attribute.draw.DrawOpacityAttribute
import org.odftoolkit.odfdom.dom.element.draw.DrawFrameElement
import org.odftoolkit.odfdom.dom.element.style.StyleGraphicPropertiesElement
import org.odftoolkit.odfdom.dom.element.style.StyleSectionPropertiesElement
import org.odftoolkit.odfdom.dom.element.text.TextPElement
import org.odftoolkit.odfdom.dom.element.text.TextSpanElement
import org.odftoolkit.odfdom.dom.style.props.*
import org.odftoolkit.odfdom.dom.style.props.OdfParagraphProperties.KeepTogether
import org.odftoolkit.odfdom.dom.style.props.OdfParagraphProperties.KeepWithNext
import org.odftoolkit.odfdom.dom.style.props.OdfTableProperties.BorderModel
import org.odftoolkit.odfdom.dom.style.props.OdfTableProperties.MayBreakBetweenRows
import org.odftoolkit.odfdom.incubator.doc.draw.OdfDrawImage
import org.odftoolkit.odfdom.pkg.OdfElement
import org.odftoolkit.odfdom.pkg.OdfFileDom
import org.odftoolkit.odfdom.type.Color
import org.odftoolkit.odfdom.type.Length
import org.odftoolkit.simple.Component
import org.odftoolkit.simple.Document
import org.odftoolkit.simple.TextDocument
import org.odftoolkit.simple.draw.*
import org.odftoolkit.simple.style.Border
import org.odftoolkit.simple.style.Font
import org.odftoolkit.simple.style.MasterPage
import org.odftoolkit.simple.style.StyleTypeDefinitions
import org.odftoolkit.simple.table.*
import org.odftoolkit.simple.text.Paragraph
import org.odftoolkit.simple.text.ParagraphContainer
import org.odftoolkit.simple.text.Section
import org.odftoolkit.simple.text.Span
import org.w3c.dom.NodeList
import java.io.File
import java.net.URI
import java.util.logging.Level
import java.util.logging.Logger


/**
 * Created by mbrickman on 29/10/17.
 */

internal var counter = 1

class SuperCell(val cell: Cell) : AbstractTableContainer() {
    override fun getTableContainerElement(): OdfElement {
        return cell.odfElement
    }
}

fun Document.createSpan(): Span {
    return Span.getInstanceof(this.contentDom.newOdfElement(TextSpanElement::class.java) as TextSpanElement)
}


fun createDocument(path: String, pageWidth: Double, pageHeight: Double, marginLeft: Double, marginTop: Double,
                   marginRight: Double, marginBottom: Double, body: (TextDocument.() -> Unit)? = null) {
    File(path.substringBeforeLast("/")).mkdirs()
    val odt = TextDocument.newTextDocument()
    val page = MasterPage.getOrCreateMasterPage(odt, "Standard")

    page.pageWidth = pageWidth
    page.pageHeight = pageHeight
    page.setMargins(marginTop, marginBottom, marginLeft, marginRight)

    if (pageWidth > pageHeight) {
        page.setPrintOrientation(StyleTypeDefinitions.PrintOrientation.LANDSCAPE)
    }

    if (body != null) {
        body(odt)
    }
    odt.save(path)
}

fun a4LandscapeDocument(path: String, marginLeft: Double = 10.0, marginTop: Double = 10.0, marginRight: Double = marginLeft, marginBottom: Double = marginTop, body: (TextDocument.() -> Unit)? = null) {
    createDocument(path, 297.0, 210.0, marginLeft, marginTop, marginRight, marginBottom, body)
}

fun a4Document(path: String, marginLeft: Double = 10.0, marginTop: Double = 10.0, marginRight: Double = marginLeft, marginBottom: Double = marginTop, body: (TextDocument.() -> Unit)? = null) {
    createDocument(path, 210.0, 297.0, marginLeft, marginTop, marginRight, marginBottom, body)
}

fun ParagraphContainer.p(font: Font? = null, body: (Paragraph.() -> Unit)? = null): Paragraph {
    val paragraph = this.addParagraph("")
    if (font != null) paragraph.font = font
    if (body != null) {
        body(paragraph)
    }
    return paragraph
}

fun ParagraphContainer.p(font: Font? = null, text: String): Paragraph {
    val paragraph = this.addParagraph(text)
    if (font != null) paragraph.font = font
    return paragraph
}

fun ParagraphContainer.p(text: String): Paragraph {
    val paragraph = this.addParagraph(text)
    return paragraph
}


fun TextDocument.p1(font: Font? = null, body: (Paragraph.() -> Unit)? = null): Paragraph {
    val paragraph = this.getParagraphByReverseIndex(0, false)
    if (font != null) paragraph.font = font
    if (body != null) {
        body(paragraph)
    }
    return paragraph
}

fun TextDocument.section(columns: Int = 1, gap: String = "0.5cm", body: (Section.() -> Unit)? = null): Section {
    val section = this.appendSection("Section${counter++}")
    val nodeList = section.odfElement.automaticStyle.getElementsByTagName("style:section-properties")
    val node = nodeList.item(0) as StyleSectionPropertiesElement
    val element = node.newStyleColumnsElement(columns)
    element.foColumnGapAttribute = gap
    if (body != null) {
        body(section)
    }
    return section
}

fun Paragraph.imageBox(x: String, y: String, width: String, height: String, path: String, path2: String="") {
    val image = Image.newImage(this, URI(path))
    if (path2.isNotEmpty()) image.updateImage(URI(path2))
    image.frame.drawFrameElement.textAnchorTypeAttribute = "page"
    image.verticalPosition = StyleTypeDefinitions.FrameVerticalPosition.FROMTOP
    image.horizontalPosition = StyleTypeDefinitions.FrameHorizontalPosition.FROMLEFT
    image.styleHandler.verticalRelative = StyleTypeDefinitions.VerticalRelative.PAGE
    image.styleHandler.horizontalRelative = StyleTypeDefinitions.HorizontalRelative.PAGE
    image.frame.drawFrameElement.svgXAttribute = x
    image.frame.drawFrameElement.svgYAttribute = y
    image.frame.drawFrameElement.svgHeightAttribute = height
    image.frame.drawFrameElement.svgWidthAttribute = width
}

fun Paragraph.textBox(x: String, y: String, width: String, height: String, border: Boolean = false, background: Color= Color.WHITE, body: (Textbox.() -> Unit)? = null): Textbox {
    val textBox = this.addTextbox(FrameRectangle(x, y, width, height))
    textBox.drawFrameElement.textAnchorTypeAttribute = "page"
    textBox.styleHandler.verticalPosition = StyleTypeDefinitions.FrameVerticalPosition.FROMTOP
    textBox.styleHandler.verticalRelative = StyleTypeDefinitions.VerticalRelative.PAGE
    textBox.styleHandler.horizontalPosition = StyleTypeDefinitions.FrameHorizontalPosition.FROMLEFT
    textBox.styleHandler.horizontalRelative = StyleTypeDefinitions.HorizontalRelative.PAGE

    if (border) textBox.styleHandler.setStroke(StyleTypeDefinitions.OdfDrawStroke.SOLID, Color.BLACK, "1.0pt", null)
    if (background!= Color.WHITE) {
        textBox.styleHandler.setBackgroundColor(background)
        textBox.opacity("20%")
    }
    if (body != null) {
        body(textBox)
    }
    return textBox
}


fun Textbox.opacity(value: String): Textbox {
    val nodes = this.styleHandler.styleElementForWrite.childNodes as NodeList
    for (i in 0.. nodes.length) {
        val node = nodes.item(i)
        if (node is StyleGraphicPropertiesElement) {
            node.setDrawOpacityAttribute(value)
            println(node)
        }
    }
    return this
}

fun Paragraph.span(font: Font? = null, getText: Span.() -> String): Span {
    val span = this.ownerDocument.createSpan()
    if (font != null) span.styleHandler.textPropertiesForWrite.font = font
    val text = getText(span)
    span.textContent = text
    this.odfElement.appendChild(span.odfElement)
    return span
}


fun Span.highlightColor(color: Color): Span {
    this.styleHandler.textPropertiesForWrite.setBackgroundColorAttribute(color)
    return this
}

fun Paragraph.keepWithNext(): Paragraph {
    this.odfElement.setProperty(KeepWithNext, "always")
    return this
}

fun Paragraph.KeepTogether(): Paragraph {
    this.odfElement.setProperty(KeepTogether, "always")
    return this
}

fun Paragraph.marginTop(margin: String): Paragraph {
    this.odfElement.setProperty(OdfParagraphProperties.MarginTop, margin)
    return this
}

fun Paragraph.borderTop(margin: String): Paragraph {
    this.odfElement.setProperty(OdfParagraphProperties.BorderTop, margin)
    return this
}

fun Paragraph.marginBottom(margin: String): Paragraph {
    this.odfElement.setProperty(OdfParagraphProperties.MarginBottom, margin)
    return this
}


fun Paragraph.marginLeft(margin: String): Paragraph {
    this.odfElement.setProperty(OdfParagraphProperties.MarginLeft, margin)
    return this
}

fun Paragraph.marginRight(margin: String): Paragraph {
    this.odfElement.setProperty(OdfParagraphProperties.MarginRight, margin)
    return this
}


fun Paragraph.paddingTop(margin: String): Paragraph {
    this.odfElement.setProperty(OdfParagraphProperties.PaddingTop, margin)
    return this
}

fun Paragraph.paddingBottom(margin: String): Paragraph {
    this.odfElement.setProperty(OdfParagraphProperties.PaddingBottom, margin)
    return this
}

fun Row.backgroundColor(color: String): Row {
    for (i in 0..cellCount - 1) {
        val cell = getCellByIndex(i)
        cell.odfElement.setProperty(OdfTableCellProperties.BackgroundColor, color)
    }
    return this
}

fun Cell.backgroundColor(color: String): Cell {
    this.odfElement.setProperty(OdfTableCellProperties.BackgroundColor, color)
    return this
}

fun Cell.paddingTop(margin: String): Cell {
    this.odfElement.setProperty(OdfTableCellProperties.PaddingTop, margin)
    return this
}

fun Cell.paddingBottom(margin: String): Cell {
    this.odfElement.setProperty(OdfTableCellProperties.PaddingBottom, margin)
    return this
}

fun Table.marginTop(margin: String): Table {
    this.odfElement.setProperty(OdfTableProperties.MarginTop, margin)
    return this
}

fun Table.marginBottom(margin: String): Table {
    this.odfElement.setProperty(OdfTableProperties.MarginBottom, margin)
    return this
}

fun Table.marginLeft(margin: String): Table {
    this.odfElement.setProperty(OdfTableProperties.MarginLeft, margin)
    return this
}

fun Table.marginRight(margin: String): Table {
    this.odfElement.setProperty(OdfTableProperties.MarginRight, margin)
    return this
}


fun Table.paddingTop(margin: String): Table {
    this.odfElement.setProperty(OdfTableCellProperties.PaddingTop, margin)
    return this
}

fun Table.paddingBottom(margin: String): Table {
    this.odfElement.setProperty(OdfTableCellProperties.PaddingBottom, margin)
    return this
}

fun Paragraph.alignRight(): Paragraph {
    this.horizontalAlignment = StyleTypeDefinitions.HorizontalAlignmentType.RIGHT
    return this
}

fun Paragraph.alignCenter(): Paragraph {
    this.horizontalAlignment = StyleTypeDefinitions.HorizontalAlignmentType.CENTER
    return this
}

val standardBorder = Border(Color.BLACK, 0.5, StyleTypeDefinitions.SupportedLinearMeasure.PT)

fun TableContainer.table(columns: Int = 3, rows: Int = 3, borderless: Boolean = false, padding: String="", body: (Table.() -> Unit)? = null): Table {
    val table = this.addTable(rows, columns)
    table.odfElement.setProperty(BorderModel, "collapsing")
    for (column in 0..columns - 1) {
        for (row in 0..rows - 1) {
            val cell = table.getCellByPosition(column, row)
            if (borderless) {
                cell.setBorders(StyleTypeDefinitions.CellBordersType.NONE, Border.NONE)
            } else {
                cell.setBorders(StyleTypeDefinitions.CellBordersType.NONE, Border.NONE)
                cell.setBorders(StyleTypeDefinitions.CellBordersType.LEFT, standardBorder)
                cell.setBorders(StyleTypeDefinitions.CellBordersType.BOTTOM, standardBorder)
                cell.setBorders(StyleTypeDefinitions.CellBordersType.RIGHT, if (column == columns - 1) standardBorder else Border.NONE)
                cell.setBorders(StyleTypeDefinitions.CellBordersType.TOP, if (row == 0) standardBorder else Border.NONE)
            }
            if (padding.isNotEmpty()) {
                cell.paddingTop(padding)
                cell.paddingBottom(padding)
                
            }
        }
    }


    if (body != null) {
        body(table)
    }
    return table
}


fun Table.row(rowIndex: Int): Row {
    return getRowByIndex(rowIndex)
}

fun Table.cell(column: Int, row: Int, body: (Cell.() -> Unit)? = null): Cell {
    val cell = getCellByPosition(column, row)
    if (body != null) {
        body(cell)
    }
    return cell
}

fun Table.cellRight(column: Int, row: Int, font: Font?, text: String): Cell {
    return cellAll(column, row, text, font, StyleTypeDefinitions.HorizontalAlignmentType.RIGHT)

}

fun Table.cellCenter(column: Int, row: Int, font: Font?, text: String): Cell {
    return cellAll(column, row, text, font, StyleTypeDefinitions.HorizontalAlignmentType.CENTER)
}


fun Table.cell(column: Int, row: Int, font: Font?, text: String): Cell {
    return cellAll(column, row, text, font, StyleTypeDefinitions.HorizontalAlignmentType.LEFT)
}

fun Table.cellRight(column: Int, row: Int, text: String): Cell {
    return cellAll(column, row, text, null, StyleTypeDefinitions.HorizontalAlignmentType.RIGHT)
}

fun Table.cellCenter(column: Int, row: Int, text: String): Cell {
    return cellAll(column, row, text, null, StyleTypeDefinitions.HorizontalAlignmentType.CENTER)
}

fun Table.cell(column: Int, row: Int, text: String): Cell {
    return cellAll(column, row, text, null, StyleTypeDefinitions.HorizontalAlignmentType.LEFT)
}

fun Table.cellAll(column: Int, row: Int, text: String, font: Font? = null, align: StyleTypeDefinitions.HorizontalAlignmentType = StyleTypeDefinitions.HorizontalAlignmentType.LEFT): Cell {
    val cell = getCellByPosition(column, row)
    cell.p(font, text).also { it.horizontalAlignment = align }
    return cell
}

fun Table.keepTogether(): Table {
    this.odfElement.setProperty(MayBreakBetweenRows, "false")
    return this
}

fun Table.keepRowsTogether(): Table {
    for (row in this.rowList) {
        row.odfElement.setProperty(OdfTableRowProperties.KeepTogether, "always")
    }
    return this
}


fun Table.widths(vararg values: Double?) {
    var totalWidths = 0.0
    for (c in columnList) {
        totalWidths += c.width
    }

    var nulls = 0
    var totalSet = 0.0
    for (value in values) {
        if (value == null) {
            nulls++
        } else {
            totalSet += value
        }
    }
    val nullWidth = if (nulls == 0) 0.0 else (width - totalSet) / nulls

    for ((index, value) in values.withIndex()) {
        this.columnList[index].fixWidth(if (value == null) nullWidth else value)
    }
}

fun Cell.table(columns: Int = 3, rows: Int = 3, borderless: Boolean = false, padding: String = "", body: (Table.() -> Unit)? = null): Table {
    return SuperCell(this).table(columns, rows, borderless, padding, body)
}

fun Cell.mergeRight(cells: Int) {
    val farCellOdf = table.cell(columnIndex + cells, rowIndex).odfElement
    val borderRight = farCellOdf.getProperty(OdfTableCellProperties.BorderRight)
    val borderLeft = this.odfElement.getProperty(OdfTableCellProperties.BorderLeft)
    table.getCellRangeByPosition(columnIndex, rowIndex, columnIndex + cells, rowIndex).merge()
    this.odfElement.setProperty(OdfTableCellProperties.BorderRight, borderLeft)
}

private fun Column.fixWidth(width: Double) {
    this.defaultCellStyle = this.defaultCellStyle // to force individual styles
    val roundingFactor = 10000.0
    val inValue = Math.round(roundingFactor * width / Length.Unit.INCH.unitInMillimiter()).toDouble() / roundingFactor
    val sWidthIN = "$inValue${Length.Unit.INCH.abbr()}"
    this.odfElement.setProperty(OdfTableColumnProperties.ColumnWidth, sWidthIN)
    val relWidth = 65535.0 / table.width * width
    if (relWidth < 40L) {
        this.odfElement.setProperty(OdfTableColumnProperties.RelColumnWidth, 40.toString() + "*")
    } else {
        this.odfElement.setProperty(OdfTableColumnProperties.RelColumnWidth, relWidth.toString() + "*")
    }
}

fun odtToDoc(path: String): String {
    val folder = path.substringBeforeLast("/")
    val odtFile = path.substringAfterLast("/")
    NativeUtils.execute("soffice", "--headless", "--convert-to", "doc", odtFile, folderPath = folder, wait = true)
    File(path).delete()
    return path.replace(".odt", ".doc")

}

fun odtToPdf(path: String, keep: Boolean = false): String {
    val folder = path.substringBeforeLast("/")
    val odtFile = path.substringAfterLast("/")
    NativeUtils.execute("soffice", "--headless", "--convert-to", "pdf", odtFile, folderPath = folder, wait = true)
    if (!keep) File(path).delete()
    return path.replace(".odt", ".pdf")

}