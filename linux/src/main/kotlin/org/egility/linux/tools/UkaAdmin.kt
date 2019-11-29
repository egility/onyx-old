/*
 * Copyright (c) Mike Brickman 2014-2018
 */

package org.egility.linux.tools

import jxl.Workbook
import jxl.write.*
import jxl.write.Number
import org.egility.library.database.DbQuery
import org.egility.library.dbobject.*
import org.egility.library.dbobject.LedgerItem
import org.egility.library.general.*
import org.odftoolkit.odfdom.dom.element.text.TextSpanElement
import org.odftoolkit.odfdom.dom.style.props.OdfParagraphProperties
import org.odftoolkit.odfdom.dom.style.props.OdfTableCellProperties
import org.odftoolkit.odfdom.dom.style.props.OdfTableColumnProperties
import org.odftoolkit.odfdom.dom.style.props.OdfTableProperties
import org.odftoolkit.odfdom.pkg.OdfElement
import org.odftoolkit.simple.TextDocument
import org.odftoolkit.simple.style.Font
import org.odftoolkit.simple.style.MasterPage
import org.odftoolkit.simple.style.StyleTypeDefinitions
import org.odftoolkit.simple.table.AbstractTableContainer
import org.odftoolkit.simple.table.Cell
import org.odftoolkit.simple.table.Table
import org.odftoolkit.simple.text.Paragraph
import org.odftoolkit.simple.text.Span
import java.io.File
import java.util.*
import kotlin.collections.HashMap

/**
 * Created by mbrickman on 01/10/17.
 */
object UkaAdmin {

    fun voidRuns(dogCode: Int, gradeCode: String, steeplechase: Boolean) {
        val idDog = Dog.getIdFromCode(dogCode)
        val classFilter = if (steeplechase) "classCode = ${ClassTemplate.STEEPLECHASE.code}" else "classCode IN (${ClassTemplate.getProgrammeList(PROGRAMME_PERFORMANCE)})"
        dbExecute("""
            UPDATE entry
                    JOIN
                agilityClass USING (idAgilityClass)
                    JOIN
                team USING (idTeam)
            SET
                progress = $PROGRESS_VOID
            WHERE
                team.idDog = $idDog
                    AND entry.progress = $PROGRESS_RUN
                    AND agilityClass.gradeCodes = ${gradeCode.quoted}
                    AND $classFilter
        """)

    }

    fun voidRun(dogCode: Int, idAgilityClass: Int) {
        val idDog = Dog.getIdFromCode(dogCode)
        dbExecute("""
            UPDATE entry
                    JOIN
                team USING (idTeam)
            SET
                progress = $PROGRESS_VOID
            WHERE
                team.idDog = $idDog
                    AND entry.progress = $PROGRESS_RUN
                    AND entry.idAgilityClass = $idAgilityClass
        """)

    }

    fun createTryout(idCompetition: Int, year: Int, month: Int, day: Int, idUka: Int) {
        val date = makeDate(year, month, day)
        AgilityClass.importUka(idCompetition, ClassTemplate.TRY_OUT.code, "", date, "", idUka)
/*
    AgilityClass.importUka(idCompetition, ClassTemplate.TRY_OUT_PENTATHLON.code, "", "", date, "", ClassTemplate.TRY_OUT_PENTATHLON.code * 1000000 + date.dateInt)
    AgilityClass.importUka(idCompetition, ClassTemplate.TRY_OUT_PENTATHLON_JUMPING1.code, "", "", date, "", ClassTemplate.TRY_OUT_PENTATHLON_JUMPING1.code * 1000000 + date.dateInt)
    AgilityClass.importUka(idCompetition, ClassTemplate.TRY_OUT_PENTATHLON_AGILITY1.code, "", "", date, "", ClassTemplate.TRY_OUT_PENTATHLON_AGILITY1.code * 1000000 + date.dateInt)
    AgilityClass.importUka(idCompetition, ClassTemplate.TRY_OUT_PENTATHLON_JUMPING2.code, "", "", date.addDays(1), "", ClassTemplate.TRY_OUT_PENTATHLON_JUMPING2.code * 1000000 + date.dateInt)
    AgilityClass.importUka(idCompetition, ClassTemplate.TRY_OUT_PENTATHLON_AGILITY2.code, "", "", date.addDays(2), "", ClassTemplate.TRY_OUT_PENTATHLON_AGILITY2.code * 1000000 + date.dateInt)
    AgilityClass.importUka(idCompetition, ClassTemplate.TRY_OUT_PENTATHLON_SPEEDSTAKES.code, "", "", date.addDays(2), "", ClassTemplate.TRY_OUT_PENTATHLON_SPEEDSTAKES.code * 1000000 + date.dateInt)
    AgilityClass.importUka(idCompetition, ClassTemplate.TRY_OUT_GAMES.code, "", "", date, "", ClassTemplate.TRY_OUT_GAMES.code * 1000000 + date.dateInt)
    AgilityClass.importUka(idCompetition, ClassTemplate.TRY_OUT_GAMES_SNOOKER.code, "", "", date, "", ClassTemplate.TRY_OUT_GAMES_SNOOKER.code * 1000000 + date.dateInt)
    AgilityClass.importUka(idCompetition, ClassTemplate.TRY_OUT_GAMES_GAMBLERS.code, "", "", date.addDays(1), "", ClassTemplate.TRY_OUT_GAMES_GAMBLERS.code * 1000000 + date.dateInt)
*/
    }

    val decimalFormat = WritableCellFormat(NumberFormat("##0.000"))
    val dateFormat = WritableCellFormat(DateFormat("DD/MM/YYYY"))

    fun exportResults(idCompetition: Int): String {
        val competition = Competition()
        competition.find(idCompetition)
        val fileName = Global.showDocumentPath(competition.uniqueName, "results", "xls")
        val folder = File(fileName).parent
        File(folder).mkdirs()

        val file = File(fileName)
        val workbook = Workbook.createWorkbook(file)
        val sheet = workbook.createSheet("Sheet1", 0)

        sheet.addCell(Label(0, 0, "ScratchID"))
        sheet.addCell(Label(1, 0, "ShowID"))
        sheet.addCell(Label(2, 0, "Levels"))
        sheet.addCell(Label(3, 0, "Class"))
        sheet.addCell(Label(4, 0, "Height"))
        sheet.addCell(Label(5, 0, "Date"))
        sheet.addCell(Label(6, 0, "HandlerID"))
        sheet.addCell(Label(7, 0, "HandlerName"))
        sheet.addCell(Label(8, 0, "DogID"))
        sheet.addCell(Label(9, 0, "pet_name"))
        sheet.addCell(Label(10, 0, "Faults"))
        sheet.addCell(Label(11, 0, "Points"))
        sheet.addCell(Label(12, 0, "Time"))
        sheet.addCell(Label(13, 0, "Place"))
        sheet.addCell(Label(14, 0, "LevelPoints"))

        val entry = Entry()
        var row = 0

        entry.agilityClass.joinToParent()
        entry.team.joinToParent()
        entry.team.competitor.joinToParent()
        entry.team.dog.joinToParent()

        entry.select("idCompetition=$idCompetition and entry.progressionPoints>0",
                "agilityClass.ClassDate, agilityClass.idUka, entry.heightCode desc, entry.progressionPoints desc")
        while (entry.next()) {
            val gradeName = Grade.Companion.getGradeName(entry.agilityClass.gradeCodes)
            row++
            sheet.addCell(Label(0, row, ""))
            sheet.addCell(Number(1, row, entry.agilityClass.idCompetition.toDouble()))
            sheet.addCell(Label(2, row, if (gradeName == "Champion") "Champ" else gradeName))
            sheet.addCell(Label(3, row, "${entry.agilityClass.template.rawName} ${entry.agilityClass.suffix}".trim()))
            sheet.addCell(Label(4, row, Height.Companion.getHeightName(entry.heightCode)))
            sheet.addCell(DateTime(5, row, entry.agilityClass.date, dateFormat))
            sheet.addCell(Number(6, row, entry.team.competitor.idUka.toDouble()))
            sheet.addCell(Label(7, row, entry.team.getCompetitorName(1)))
            sheet.addCell(Number(8, row, entry.team.dog.idUka.toDouble()))
            sheet.addCell(Label(9, row, entry.team.dog.cleanedPetName))
            sheet.addCell(Number(10, row, entry.faults.toDouble() / 1000, decimalFormat))
            sheet.addCell(Number(11, row, (entry.gamesScore + entry.gamesBonus).toDouble()))
            sheet.addCell(Number(12, row, entry.time.toDouble() / 1000, decimalFormat))
            sheet.addCell(Number(13, row, (if (isBitSet(entry.placeFlags, PRIZE_PLACE)) entry.place else 0).toDouble()))
            sheet.addCell(Number(14, row, entry.progressionPoints.toDouble()))
        }

        for (columnNumber in 0..14) {
            val column = sheet.getColumnView(columnNumber)
            column.isAutosize = true
            sheet.setColumnView(columnNumber, column)
        }
        workbook.write()
        workbook.close()
        return fileName
    }

    enum class Column { GRADE, CLASS, HEIGHT, RUN_DATA, FAULTS, POINTS, TIME, PLACE, PROGRESSION, ENTRY, COMPETITOR1,
        COMPETITOR2, COMPETITOR3, COMPETITOR_HEIGHT1, COMPETITOR_HEIGHT2, COMPETITOR_HEIGHT3
    }

    val allColumns = arrayOf(Column.GRADE, Column.CLASS, Column.HEIGHT, Column.RUN_DATA, Column.FAULTS, Column.POINTS,
            Column.TIME, Column.PLACE, Column.PROGRESSION, Column.ENTRY, Column.COMPETITOR1, Column.COMPETITOR2,
            Column.COMPETITOR3, Column.COMPETITOR_HEIGHT1, Column.COMPETITOR_HEIGHT2, Column.COMPETITOR_HEIGHT3)

    fun exportResultsAll(idCompetition: Int): String {

        fun addHeadings(sheet: WritableSheet?, columns: Array<Column>) {
            //        val font=WritableFont(WritableFont.ARIAL, WritableFont.DEFAULT_POINT_SIZE, WritableFont.BOLD)

            if (sheet != null) {
                var column = 0
                if (columns.contains(Column.GRADE)) {
                    sheet.addCell(Label(column++, 0, "Grade"))
                }
                if (columns.contains(Column.CLASS)) {
                    sheet.addCell(Label(column++, 0, "Class"))
                }
                if (columns.contains(Column.HEIGHT)) {
                    sheet.addCell(Label(column++, 0, "Height"))
                }
                if (columns.contains(Column.RUN_DATA)) {
                    sheet.addCell(Label(column++, 0, "RunData"))
                }
                if (columns.contains(Column.TIME)) {
                    sheet.addCell(Label(column++, 0, "Time"))
                }
                if (columns.contains(Column.FAULTS)) {
                    sheet.addCell(Label(column++, 0, "Faults"))
                }
                if (columns.contains(Column.POINTS)) {
                    sheet.addCell(Label(column++, 0, "Points"))
                }
                if (columns.contains(Column.PLACE)) {
                    sheet.addCell(Label(column++, 0, "Place"))
                }
                if (columns.contains(Column.PROGRESSION)) {
                    sheet.addCell(Label(column++, 0, "Progression"))
                }
                if (columns.contains(Column.ENTRY)) {
                    sheet.addCell(Label(column++, 0, "Entry"))
                }
                if (columns.contains(Column.COMPETITOR1)) {
                    sheet.addCell(Label(column++, 0, "Competitor"))
                    sheet.addCell(Label(column++, 0, "Dog"))
                }
                if (columns.contains(Column.COMPETITOR2)) {
                    sheet.addCell(Label(column++, 0, "Competitor_2"))
                    sheet.addCell(Label(column++, 0, "Dog_2"))
                }
                if (columns.contains(Column.COMPETITOR3)) {
                    sheet.addCell(Label(column++, 0, "Competitor_3"))
                    sheet.addCell(Label(column++, 0, "Dog_3"))
                }
                if (columns.contains(Column.COMPETITOR_HEIGHT1)) {
                    sheet.addCell(Label(column++, 0, "Competitor"))
                    sheet.addCell(Label(column++, 0, "Dog"))
                    sheet.addCell(Label(column++, 0, "Height_1"))
                }
                if (columns.contains(Column.COMPETITOR_HEIGHT2)) {
                    sheet.addCell(Label(column++, 0, "Competitor_2"))
                    sheet.addCell(Label(column++, 0, "Dog_2"))
                    sheet.addCell(Label(column++, 0, "Height_2"))
                }
                if (columns.contains(Column.COMPETITOR_HEIGHT3)) {
                    sheet.addCell(Label(column++, 0, "Competitor_3"))
                    sheet.addCell(Label(column++, 0, "Dog_3"))
                    sheet.addCell(Label(column++, 0, "Height_3"))
                }

                for (columnNumber in 0..column) {
                    val column = sheet.getColumnView(columnNumber)
                    column.isAutosize = true
                    sheet.setColumnView(columnNumber, column)
                }
            }
        }

        fun addRow(sheet: WritableSheet?, row: Int, entry: Entry, columns: Array<Column>, isQualifierSheet: Boolean = false) {
            if (sheet != null) {
                var column = 0
                val runData = entry.runData
                val template = entry.agilityClass.template
                var entryType = when (entry.type) {ENTRY_IMPORTED_LIVE -> "On-Line"; ENTRY_LATE_CREDITS -> "Late"; ENTRY_LATE_FEE -> "Late"; ENTRY_TRANSFER -> "T/Fer"; else -> ""
                }
                if (template.isChild && template.isSpecialParent || template.hasChildren && !template.isSpecialParent) {
                    entryType = ""
                }

                if (columns.contains(Column.GRADE)) {
                    if (!entry.agilityClass.gradeCodes.isEmpty()) {
                        sheet.addCell(Label(column++, row, Grade.Companion.getGradeName(entry.agilityClass.gradeCodes)))
                    } else {
                        column++
                    }

                }
                if (columns.contains(Column.CLASS)) {
                    sheet.addCell(Label(column++, row, "${template.rawName} ${entry.agilityClass.suffix}".trim()))
                }
                if (columns.contains(Column.HEIGHT)) {
                    sheet.addCell(Label(column++, row, Height.Companion.getHeightName(entry.heightCode)))
                }
                if (columns.contains(Column.RUN_DATA)) {
                    if (entry.hasRun) {
                        sheet.addCell(Label(column++, row, if (template.hasChildren) "" else runData))
                    } else {
                        sheet.addCell(Label(column++, row, if (template.hasChildren) "" else entry.progressText))
                    }
                }
                if (columns.contains(Column.TIME)) {
                    sheet.addCell(Number(column++, row, entry.time.toDouble() / 1000, decimalFormat))
                }
                if (columns.contains(Column.FAULTS)) {
                    if (entry.hasRun) {
                        sheet.addCell(Number(column++, row, entry.faults.toDouble() / 1000, decimalFormat))
                    } else {
                        sheet.addCell(Number(column++, row, 1000.0, decimalFormat))
                    }
                }
                if (columns.contains(Column.POINTS)) {
                    if ((entry.agilityClass.isPointsBased) && entry.hasRun && !entry.isNFC) {
                        if (entry.agilityClass.isScoreBasedGame) {
                            sheet.addCell(Number(column++, row, (entry.gamesScore + entry.gamesBonus).toDouble()))
                        } else {
                            sheet.addCell(Number(column++, row, entry.points.toDouble() / 1000.0, decimalFormat))
                        }
                    } else {
                        column++
                    }
                }
                if (columns.contains(Column.PLACE)) {
                    if (template.hasChildren || (template.isChild && !template.isUkaProgression) || template.isSpecialParent || isBitSet(entry.placeFlags, PRIZE_PLACE) || isQualifierSheet) {
                        if (entry.place > 0) {
                            sheet.addCell(Number(column++, row, entry.place.toDouble()))
                        } else {
                            column++
                        }
                    } else {
                        column++
                    }
                }
                if (columns.contains(Column.PROGRESSION)) {
                    if (entry.progressionPoints > 0) {
                        sheet.addCell(Number(column++, row, entry.progressionPoints.toDouble()))
                    } else {
                        column++
                    }
                }
                if (columns.contains(Column.ENTRY)) {
                    sheet.addCell(Label(column++, row, entryType))
                }
                if (columns.contains(Column.COMPETITOR1)) {
                    if (entry.team.getIdCompetitor(1) > 0) {
                        if (isQualifierSheet) {
                            sheet.addCell(Label(column++, row, "${entry.team.getCompetitorName(1)}"))
                            sheet.addCell(Label(column++, row, "${entry.team.getPetName(1)}"))
                        } else {
                            sheet.addCell(Label(column++, row, "${entry.team.getCompetitorName(1)}"))
                            sheet.addCell(Label(column++, row, "${entry.team.getPetName(1)} (${entry.team.getDogCode(1)})"))
                        }
                    }
                }
                if (columns.contains(Column.COMPETITOR2)) {
                    if (entry.team.getIdCompetitor(2) > 0) {
                        if (isQualifierSheet) {
                            sheet.addCell(Label(column++, row, "${entry.team.getCompetitorName(2)}"))
                            sheet.addCell(Label(column++, row, "${entry.team.getPetName(2)}"))
                        } else {
                            sheet.addCell(Label(column++, row, "${entry.team.getCompetitorName(2)}"))
                            sheet.addCell(Label(column++, row, "${entry.team.getPetName(2)} (${entry.team.getDogCode(2)})"))
                        }
                    } else {
                        column += 2
                    }
                }
                if (columns.contains(Column.COMPETITOR3)) {
                    if (entry.team.getIdCompetitor(3) > 0) {
                        if (isQualifierSheet) {
                            sheet.addCell(Label(column++, row, "${entry.team.getCompetitorName(3)}"))
                            sheet.addCell(Label(column++, row, "${entry.team.getPetName(3)}"))
                        } else {
                            sheet.addCell(Label(column++, row, "${entry.team.getCompetitorName(3)}"))
                            sheet.addCell(Label(column++, row, "${entry.team.getPetName(3)} (${entry.team.getDogCode(3)})"))
                        }
                    } else {
                        column += 2
                    }
                }
                if (columns.contains(Column.COMPETITOR_HEIGHT1)) {
                    if (isQualifierSheet) {
                        sheet.addCell(Label(column++, row, "${entry.team.getCompetitorName(1)}"))
                        sheet.addCell(Label(column++, row, "${entry.team.getPetName(1)}"))
                        sheet.addCell(Label(column++, row, "${entry.team.getHeightName(1)}"))
                    } else {
                        sheet.addCell(Label(column++, row, "${entry.team.getCompetitorName(1)}"))
                        sheet.addCell(Label(column++, row, "${entry.team.getPetName(1)} (${entry.team.getDogCode(1)})"))
                        sheet.addCell(Label(column++, row, "${entry.team.getHeightName(1)}"))
                    }
                }
                if (columns.contains(Column.COMPETITOR_HEIGHT2)) {
                    if (entry.team.getIdCompetitor(2) > 0) {
                        if (isQualifierSheet) {
                            sheet.addCell(Label(column++, row, "${entry.team.getCompetitorName(2)}"))
                            sheet.addCell(Label(column++, row, "${entry.team.getPetName(2)}"))
                            sheet.addCell(Label(column++, row, "${entry.team.getHeightName(2)}"))
                        } else {
                            sheet.addCell(Label(column++, row, "${entry.team.getCompetitorName(2)}"))
                            sheet.addCell(Label(column++, row, "${entry.team.getPetName(2)} (${entry.team.getDogCode(2)})"))
                            sheet.addCell(Label(column++, row, "${entry.team.getHeightName(2)}"))
                        }
                    } else {
                        column += 2
                    }
                }
                if (columns.contains(Column.COMPETITOR_HEIGHT3)) {
                    if (entry.team.getIdCompetitor(3) > 0) {
                        if (isQualifierSheet) {
                            sheet.addCell(Label(column++, row, "${entry.team.getCompetitorName(3)}"))
                            sheet.addCell(Label(column++, row, "${entry.team.getPetName(3)}"))
                            sheet.addCell(Label(column++, row, "${entry.team.getHeightName(3)}"))
                        } else {
                            sheet.addCell(Label(column++, row, "${entry.team.getCompetitorName(3)}"))
                            sheet.addCell(Label(column++, row, "${entry.team.getPetName(3)} (${entry.team.getDogCode(3)})"))
                            sheet.addCell(Label(column++, row, "${entry.team.getHeightName(3)}"))
                        }
                    } else {
                        column += 2
                    }
                }
            }
        }


        val competition = Competition()
        competition.find(idCompetition)

        val fileName = Global.showDocumentPath(competition.uniqueName, "results_all", "xls")
        val folder = File(fileName).parent
        File(folder).mkdirs()

        val file = File(fileName)
        val workbook = Workbook.createWorkbook(file)


        var day = nullDate
        var daySheet: WritableSheet? = null
        var dayRow = 0
        var daySheetIndex = 0

        var specialClassCode = 0
        var specialSheet: WritableSheet? = null
        var specialRow = 0
        var specialColumns = allColumns


        val entry = Entry()

        entry.agilityClass.joinToParent()
        entry.team.joinToParent()
        entry.team.competitor.joinToParent()
        entry.team.dog.joinToParent()
        entry.select("idCompetition=$idCompetition",
                "agilityClass.ClassDate, agilityClass.idUka, agilityClass.idAgilityClass, if(agilityClass.combineHeights, '', entry.heightCode) desc, entry.hasRun desc, entry.place, entry.progress")
        while (entry.next()) {
            val template = entry.agilityClass.template


            if (entry.agilityClass.date != day) {
                day = entry.agilityClass.date
                daySheet = workbook.createSheet(day.dayName(), daySheetIndex++)
                addHeadings(daySheet, allColumns)
                dayRow = 0
            }
            addRow(daySheet, ++dayRow, entry, allColumns)
            //if (template.isBetween(ClassTemplate.MASTERS, ClassTemplate.FINAL_ROUND_2)) {
                if (specialClassCode != template.code) {
                    specialClassCode = template.code
                    specialSheet = workbook.createSheet(template.rawName, 99)
                    when (template) {
                        ClassTemplate.SPLIT_PAIRS -> {
                            specialColumns = arrayOf(Column.HEIGHT, Column.TIME, Column.FAULTS, Column.POINTS, Column.PLACE, Column.COMPETITOR1, Column.COMPETITOR2)
                        }
                        ClassTemplate.TEAM -> {
                            specialColumns = arrayOf(Column.TIME, Column.FAULTS, Column.POINTS, Column.PLACE, Column.COMPETITOR_HEIGHT1, Column.COMPETITOR_HEIGHT2, Column.COMPETITOR_HEIGHT3)
                        }
                        ClassTemplate.JUNIOR_OPEN -> {
                            specialColumns = arrayOf(Column.FAULTS, Column.TIME, Column.PLACE, Column.COMPETITOR1)
                        }
                        else -> {
                            specialColumns = arrayOf(Column.HEIGHT, Column.FAULTS, Column.TIME, Column.PLACE, Column.COMPETITOR1)
                        }
                    }
                    addHeadings(specialSheet, specialColumns)
                    specialRow = 0
                }
                addRow(specialSheet, ++specialRow, entry, specialColumns, true)
            //}

        }

        workbook.write()
        workbook.close()
        return fileName
    }

    class SuperCell(val cell: Cell) : AbstractTableContainer() {

        override fun getTableContainerElement(): OdfElement {
            return cell.odfElement
        }

    }


    fun exportRingPlan(idCompetition: Int): String {

        fun cellAddText(cell: Cell, pointSize: Double, text: String) {
            cell.font = Font("Comic Sans MS", StyleTypeDefinitions.FontStyle.REGULAR, pointSize)
            cell.setHorizontalAlignment(StyleTypeDefinitions.HorizontalAlignmentType.CENTER)
            cell.stringValue = text
        }

        fun cellAddParagraph(cell: Cell, pointSize: Double, text: String, bold: Boolean = false): Paragraph {
            val paragraph = cell.addParagraph(text)
            if (bold) {
                paragraph.font = Font("Comic Sans MS", StyleTypeDefinitions.FontStyle.BOLD, pointSize)
            } else {
                paragraph.font = Font("Comic Sans MS", StyleTypeDefinitions.FontStyle.REGULAR, pointSize)
            }
            paragraph.horizontalAlignment = StyleTypeDefinitions.HorizontalAlignmentType.CENTER
            return paragraph
        }

        fun addParagraph(odt: TextDocument, pointSize: Double, text: String, bold: Boolean = false): Paragraph {
            val paragraph = odt.addParagraph(text)
            if (bold) {
                paragraph.font = Font("Comic Sans MS", StyleTypeDefinitions.FontStyle.BOLD, pointSize)
            } else {
                paragraph.font = Font("Comic Sans MS", StyleTypeDefinitions.FontStyle.REGULAR, pointSize)
            }
            paragraph.horizontalAlignment = StyleTypeDefinitions.HorizontalAlignmentType.CENTER
            return paragraph
        }

        fun clearBorders(table: Table, columns: Int, rows: Int) {
            for (column in 0..columns - 1) {
                for (row in 0..rows - 1) {
                    var cell = table.getCellByPosition(column, row)
                    cell.setBorders(StyleTypeDefinitions.CellBordersType.NONE, org.odftoolkit.simple.style.Border.NONE)
                    cell.odfElement.setProperty(OdfTableCellProperties.PaddingTop, "0.0pt")
                    cell.odfElement.setProperty(OdfTableCellProperties.PaddingBottom, "0.0pt")
                }
            }
        }

        fun setWidths(table: Table) {
            val odfTable = table.odfElement
            val margin = if (table.columnCount < 6) ((29.70 - 4.0) - table.columnCount * 4.8) / 2.0 else 0.0
            odfTable.setProperty(OdfTableProperties.Align, "margins")
            odfTable.setProperty(OdfTableProperties.MarginLeft, "${margin}cm")
            odfTable.setProperty(OdfTableProperties.MarginRight, "${margin}cm")
        }

        fun getRingCount(classDate: Date): Int {
            val query = DbQuery("""
                SELECT MAX(ringNumber) AS ringNumber FROM agilityClass
                WHERE idCompetition = $idCompetition AND classDate = ${classDate.sqlDate}
                """)
            query.first()
            return query.getInt("ringNumber")
        }

        fun afterTable(odt: TextDocument) {
            addParagraph(odt, 10.0, "Mx=Maxi, Sd=Standard, Md=Midi, Ty=Toy, Mc=Micro. Number of entries in brackets")
            odt.addParagraph("")

            val paragraph = odt.addParagraph("")
            paragraph.horizontalAlignment = StyleTypeDefinitions.HorizontalAlignmentType.LEFT

            var span = Span.getInstanceof(odt.contentDom.newOdfElement(TextSpanElement::class.java) as TextSpanElement)
            span.styleHandler.textPropertiesForWrite.font = Font("Comic Sans MS", StyleTypeDefinitions.FontStyle.BOLD, 10.0)
            span.textContent = "Newcomers: "
            paragraph.odfElement.appendChild(span.odfElement)

            span = Span.getInstanceof(odt.contentDom.newOdfElement(TextSpanElement::class.java) as TextSpanElement)
            span.styleHandler.textPropertiesForWrite.font = Font("Comic Sans MS", StyleTypeDefinitions.FontStyle.REGULAR, 10.0)
            span.textContent = "When you walk the course please book-in on the tablets provided using your dog’s 5 digit code. We will then attempt to locate you before the class closes. Please listen for announcements. When you enter the ring please confirm your name and dog’s name with the scorer. Do not start your run until the scorer tells you. "
            paragraph.odfElement.appendChild(span.odfElement)

            span = Span.getInstanceof(odt.contentDom.newOdfElement(TextSpanElement::class.java) as TextSpanElement)
            span.styleHandler.textPropertiesForWrite.font = Font("Comic Sans MS", StyleTypeDefinitions.FontStyle.BOLD, 10.0, StyleTypeDefinitions.TextLinePosition.UNDER)
            span.textContent = "Mobile App: "
            paragraph.odfElement.appendChild(span.odfElement)

            span = Span.getInstanceof(odt.contentDom.newOdfElement(TextSpanElement::class.java) as TextSpanElement)
            span.styleHandler.textPropertiesForWrite.font = Font("Comic Sans MS", StyleTypeDefinitions.FontStyle.REGULAR, 10.0, StyleTypeDefinitions.TextLinePosition.UNDER)
            span.textContent = "Point your phone's browser to "
            paragraph.odfElement.appendChild(span.odfElement)

            span = Span.getInstanceof(odt.contentDom.newOdfElement(TextSpanElement::class.java) as TextSpanElement)
            span.styleHandler.textPropertiesForWrite.font = Font("Comic Sans MS", StyleTypeDefinitions.FontStyle.BOLD, 10.0, StyleTypeDefinitions.TextLinePosition.UNDER)
            span.textContent = "agilityplaza.com"
            paragraph.odfElement.appendChild(span.odfElement)

            odt.addPageBreak(paragraph)
        }

        val query = DbQuery("""
        SELECT
            agilityClass.classDate,
            agilityClass.idAgilityClass,
            agilityClass.classCode,
            parentClass.classCode AS parentClassCode,
            agilityClass.suffix,
            agilityClass.ringNumber,
            agilityClass.judge as classJudge,
            grade.Name AS gradeName,
            ring.judge,
            agilityClass.ringOrder,
            height.abbreviation,
            SUM(IF(entry.entryType IN ($ENTRY_AGILITY_PLAZA, $ENTRY_PAPER, $ENTRY_MANUAL, $ENTRY_DEPENDENT_CLASS), 1, 0)) AS entries
        FROM
            agilityClass
                JOIN
            ring ON ring.idCompetition = agilityClass.idCompetition
                AND ring.date = agilityClass.classDate
                AND ring.ringNumber = agilityClass.ringNumber
                JOIN
            height ON FIND_IN_SET(height.heightCode,
                    agilityClass.heightRunningOrder)
                LEFT JOIN
            entry ON entry.idAgilityClass = if(agilityClass.classCode in (${ClassTemplate.specialGroupMemberList}), agilityClass.idAgilityClassParent,  agilityClass.idAgilityClass)
                AND entry.heightCode = height.heightCode
                LEFT JOIN
            grade ON agilityClass.gradeCodes = grade.gradeCode
                LEFT JOIN
            agilityClass AS parentClass ON (parentClass.idAgilityClass = agilityClass.idAgilityClassParent)
        WHERE
            agilityClass.idCompetition = $idCompetition AND agilityClass.ringNumber>0
        GROUP BY agilityClass.idAgilityClass , height.heightCode
        ORDER BY agilityClass.classDate , agilityClass.ringNumber , agilityClass.ringOrder , FIND_IN_SET(height.heightCode,
                agilityClass.heightRunningOrder)
    """)

        var classDate = nullDate
        var ringNumber = 0
        var heightColumn = 0
        var idAgilityClass = 0
        var table: Table? = null
        var classTable: Table? = null
        var planCell: Cell? = null
        val competition = Competition()
        var firstClass = true
        var template: ClassTemplate = ClassTemplate.UNDEFINED
        var judge = ""

        competition.find(idCompetition)

        val odt = TextDocument.newTextDocument()
        val page = MasterPage.getOrCreateMasterPage(odt, "Standard")
        page.setPrintOrientation(StyleTypeDefinitions.PrintOrientation.LANDSCAPE)
        page.pageWidth = 297.0
        page.pageHeight = 210.0
        page.setMargins(10.0, 10.0, 20.0, 20.0)

        while (query.next()) {
            if (query.getDate("classDate") != classDate) {
                if (classDate != nullDate) {
                    afterTable(odt)
                }
                classDate = query.getDate("classDate")
                ringNumber = 0
                val p = odt.getParagraphByReverseIndex(0, false)
//            val p=odt.addParagraph(competition.name + " - " + classDate.fullDate())


                var span = Span.getInstanceof(odt.contentDom.newOdfElement(TextSpanElement::class.java) as TextSpanElement)
                span.styleHandler.textPropertiesForWrite.font = Font("Comic Sans MS", StyleTypeDefinitions.FontStyle.BOLD, 14.0)
                span.textContent = competition.name + " - " + classDate.fullDate()
                p.odfElement.appendChild(span.odfElement)
//            p.textContent=competition.name + " - " + classDate.fullDate()
//            p.font = Font("Comic Sans MS", StyleTypeDefinitions.FontStyle.BOLD, 14.0)
                p.horizontalAlignment = StyleTypeDefinitions.HorizontalAlignmentType.CENTER
                val ringCount = getRingCount(classDate)
                table = odt.addTable(3, ringCount)
                setWidths(table)

                table.getCellRangeByPosition(0, 0, ringCount - 1, 0).merge()
                val topCell = table.getCellByPosition(0, 0)
                topCell.odfElement.setProperty(OdfTableCellProperties.BorderRight, "0.05pt solid #000000")
                cellAddText(topCell, 10.0, "Classes will run in the order shown. Walking will happen between each level but not between each height.")
            }
            if (table != null) {
                if (query.getInt("ringNumber") != ringNumber) {
                    ringNumber = query.getInt("ringNumber")
                    judge = query.getString("judge")
                    val headingCell = table.getCellByPosition(ringNumber - 1, 1)
                    planCell = table.getCellByPosition(ringNumber - 1, 2)
                    cellAddText(headingCell, 10.0, "RING $ringNumber\n$judge")
                    firstClass = true
                }
                if (planCell != null) {
                    if (query.getInt("idAgilityClass") != idAgilityClass) {
                        idAgilityClass = query.getInt("idAgilityClass")
                        template = ClassTemplate.select(query.getInt("classCode"))
                        var isSpecialClass = template.isSpecialClass
                        var sponsor = template.sponsor
                        if (query.getInt("parentClassCode") > 0) {
                            val parentTemplate = ClassTemplate.select(query.getInt("parentClassCode"))
                            if (parentTemplate != ClassTemplate.UNDEFINED) {
                                isSpecialClass = parentTemplate.isSpecialClass
                                sponsor = parentTemplate.sponsor
                            }
                        }
                        heightColumn = 0
                        val gradeName = query.getString("gradeName")
                        val suffix = query.getString("suffix")
                        if (!firstClass) {
                            cellAddParagraph(planCell, 9.0, "")
                        }
                        firstClass = false
                        if (isSpecialClass) {
                            val className = (sponsor + " " + template.nameTemplate.replace("<grade>", gradeName)).trim()
                            cellAddParagraph(planCell, 9.0, className, bold = true)
                        } else {
                            val className = (template.nameTemplate.replace("<grade>", gradeName) + " " + suffix).trim()
                            cellAddParagraph(planCell, 9.0, className)
                        }
                        classTable = Table.newTable(SuperCell(planCell), 2, 4)
                        clearBorders(classTable, 4, 2)
                        if (query.getString("classJudge").isNotEmpty() && query.getString("classJudge").neq(judge)) {
                            cellAddParagraph(planCell, 9.0, "Judge: " + query.getString("classJudge"))
                        }
                    }
                    if (classTable != null) {
                        val heightCell = classTable.getCellByPosition(heightColumn, 0)
                        val entriesCell = classTable.getCellByPosition(heightColumn, 1)
                        cellAddText(heightCell, 9.0, query.getString("abbreviation"))
                        cellAddText(entriesCell, 9.0, "(${query.getInt("entries")})")
                    }
                    heightColumn++
                }
            }
        }
        afterTable(odt)

        val odtFile = Global.showDocumentPath(competition.uniqueName, "ring_plan", "odt")
        val folder = odtFile.substringBeforeLast("/")
        odt.save(odtFile)
        NativeUtils.execute("soffice", "--headless", "--convert-to", "doc", odtFile, folderPath = folder, wait = true)
        File(odtFile).delete()
        return odtFile.replace(".odt", ".doc")
    }

    fun knockOutTable(idAgilityClass: Int, pdf: Boolean = false, copies: Int = 1): String {

        val nameWidth = 7.0
        val resultWidth = 1.5

        fun cellAddText(cell: Cell, text: String) {
            cell.font = Font("Arial", StyleTypeDefinitions.FontStyle.REGULAR, 10.0)
            cell.stringValue = text
        }

        fun title(odt: TextDocument, text: String) {
            val paragraph = odt.addParagraph(text)
            paragraph.font = Font("Arial", StyleTypeDefinitions.FontStyle.ITALIC, 10.0)
            paragraph.horizontalAlignment = StyleTypeDefinitions.HorizontalAlignmentType.CENTER
        }

        fun heading(odt: TextDocument, text: String) {
            val paragraph = odt.addParagraph(text)
            paragraph.font = Font("Arial", StyleTypeDefinitions.FontStyle.REGULAR, 18.0)
            paragraph.odfElement.setProperty(OdfParagraphProperties.MarginTop, "12.0pt")
            paragraph.odfElement.setProperty(OdfParagraphProperties.MarginBottom, "6.0pt")
        }


        val agilityClass = AgilityClass()
        agilityClass.find(idAgilityClass)

        val odt = TextDocument.newTextDocument()
        val page = MasterPage.getOrCreateMasterPage(odt, "Standard")
        page.setPrintOrientation(StyleTypeDefinitions.PrintOrientation.PORTRAIT)
        page.pageWidth = 210.0
        page.pageHeight = 297.0
        val point = 0.3528
        page.setMargins(18 * point, 18 * point, 44 * point, 18 * point)

        var firstPage = true


        for (heightCode in agilityClass.heightRunningOrder.split(",")) {
            val metrics = arrayListOf<Int>()
            val entry = Entry()
            entry.select("idAgilityClass=$idAgilityClass and heightCode='$heightCode' and progress <60", "runningOrder")

            val rows = entry.rowCount

            if (rows > 0) {
                var columns = 3
                while ((rows - 1).shr(columns - 2) > 0) columns++

                if (firstPage) {
                    firstPage = false
                } else {
                    odt.addPageBreak()
                }

                title(odt, agilityClass.competitionNameDate)
                heading(odt, "${agilityClass.name} (${Height.getHeightName(heightCode)}) - Running Order")

                val table = odt.addTable(rows, columns)
                while (entry.next()) {
                    val runningOrder = table.getCellByPosition(0, entry.cursor)
                    val name = table.getCellByPosition(1, entry.cursor)
                    val first = table.getCellByPosition(2, entry.cursor)


                    cellAddText(runningOrder, entry.runningOrder.toString())
                    if (entry.runningOrder > 1.shl(columns - 2) - rows) {
                        cellAddText(name, entry.teamDescription)
                        metrics.add(1)
                    } else {
                        cellAddText(name, entry.teamDescription)
                        cellAddText(first, entry.runningOrder.toString() + " (bye)")
                        metrics.add(2)
                    }
                }

                var tableWidth = 0.0
                for (i in 0..columns - 1) {
                    val width = if (i == 0) 1.0 else if (i == 1) nameWidth else resultWidth
                    tableWidth += width
                    table.columnList[i].width = 0.0 // to force repeat split
                    val odfColumn = table.getColumnByIndex(i).odfElement
                    odfColumn.removeProperty(OdfTableColumnProperties.RelColumnWidth)
                    odfColumn.setProperty(OdfTableColumnProperties.ColumnWidth, "${width}cm")
                }

                val odfTable = table.odfElement
                odfTable.removeProperty(OdfTableProperties.RelWidth)
                odfTable.setProperty(OdfTableProperties.Width, "${tableWidth}cm")
                odfTable.setProperty(OdfTableProperties.Align, "left")

                fun do_merge(column: Int, target: Int) {
                    var total = 0
                    for (row in 0..rows - 1) {
                        total = (total + metrics[row]).rem(target)
                        if (total != 0) {
                            table.getCellRangeByPosition(column, row, column, row + 1).merge()
                        }
                    }
                }

                for (level in 2..columns - 1) {
                    do_merge(level, 1.shl(level - 1))
                }
            }
        }

        val odtFile = Global.showDocumentPath(agilityClass.idCompetition, "knockout_$idAgilityClass", "odt")
        val pdfFile = odtFile.replace(".odt", ".pdf")
        val folder = pdfFile.substringBeforeLast("/")
        odt.save(odtFile)
        exec("soffice --headless --convert-to pdf $odtFile", wait = true, dir = folder)
        exec("rm $odtFile", wait = true)
        if (!pdf) {
            exec("lp -o media=a4 -n$copies $pdfFile", wait = true)
        }
        return pdfFile
    }

    fun exportRingBoards(idCompetition: Int): String {

        fun cellAddText(cell: Cell, pointSize: Double, text: String) {
            cell.font = Font("Comic Sans MS", StyleTypeDefinitions.FontStyle.REGULAR, pointSize)
            cell.setHorizontalAlignment(StyleTypeDefinitions.HorizontalAlignmentType.CENTER)
            cell.stringValue = text
        }

        fun cellAddParagraph(cell: Cell, pointSize: Double, text: String, bold: Boolean = false, marginTop: String = ""): Paragraph {
            val paragraph = cell.addParagraph(text)
            if (bold) {
                paragraph.font = Font("Comic Sans MS", StyleTypeDefinitions.FontStyle.BOLD, pointSize)
            } else {
                paragraph.font = Font("Comic Sans MS", StyleTypeDefinitions.FontStyle.REGULAR, pointSize)
            }
            paragraph.horizontalAlignment = StyleTypeDefinitions.HorizontalAlignmentType.CENTER
            if (marginTop.isNotEmpty()) {
                paragraph.marginTop(marginTop)
            }
            return paragraph
        }

        fun clearBorders(table: Table, columns: Int, rows: Int) {
            for (column in 0..columns - 1) {
                for (row in 0..rows - 1) {
                    var cell = table.getCellByPosition(column, row)
                    cell.setBorders(StyleTypeDefinitions.CellBordersType.NONE, org.odftoolkit.simple.style.Border.NONE)
                    cell.odfElement.setProperty(OdfTableCellProperties.PaddingTop, "0.0pt")
                    cell.odfElement.setProperty(OdfTableCellProperties.PaddingBottom, "0.0pt")
                }
            }
        }

        val query = DbQuery("""
        SELECT
            agilityClass.classDate,
            agilityClass.idAgilityClass,
            agilityClass.classCode,
            parentClass.classCode AS parentClassCode,
            agilityClass.suffix,
            agilityClass.ringNumber,
            agilityClass.judge as classJudge,
            grade.Name AS gradeName,
            ring.judge,
            agilityClass.ringOrder,
            height.abbreviation,
            SUM(IF(entry.entryType IN ($ENTRY_AGILITY_PLAZA, $ENTRY_PAPER), 1, 0)) AS entries
        FROM
            agilityClass
                JOIN
            ring ON ring.idCompetition = agilityClass.idCompetition
                AND ring.date = agilityClass.classDate
                AND ring.ringNumber = agilityClass.ringNumber
                JOIN
            height ON FIND_IN_SET(height.heightCode,
                    agilityClass.heightRunningOrder)
                LEFT JOIN
            entry ON entry.idAgilityClass = if(agilityClass.classCode in (101,102, 140, 141), agilityClass.idAgilityClassParent,  agilityClass.idAgilityClass)
                AND entry.heightCode = height.heightCode
                LEFT JOIN
            grade ON agilityClass.gradeCodes = grade.gradeCode
                LEFT JOIN
            agilityClass AS parentClass ON (parentClass.idAgilityClass = agilityClass.idAgilityClassParent)

        WHERE
            agilityClass.idCompetition = $idCompetition AND agilityClass.ringNumber>0
        GROUP BY agilityClass.idAgilityClass , height.heightCode
        ORDER BY agilityClass.classDate , agilityClass.ringNumber , agilityClass.ringOrder , FIND_IN_SET(height.heightCode,
                agilityClass.heightRunningOrder)
    """)

        var template: ClassTemplate = ClassTemplate.UNDEFINED
        var classDate = nullDate
        var ringNumber = 0
        var heightColumn = 0
        var idAgilityClass = 0
        var table: Table? = null
        var classTable: Table? = null
        var planCell: Cell? = null
        val competition = Competition()
        var firstClass = true

        competition.find(idCompetition)

        val odt = TextDocument.newTextDocument()
        val page = MasterPage.getOrCreateMasterPage(odt, "Standard")
        page.setPrintOrientation(StyleTypeDefinitions.PrintOrientation.PORTRAIT)
        page.pageWidth = 210.0
        page.pageHeight = 297.0
        page.setMargins(12.7, 12.7, 12.7, 12.7)

        var pageNumber = 0
        var judge = ""
        var marginTop = ""

        while (query.next()) {
            if (query.getDate("classDate") != classDate || query.getInt("ringNumber") != ringNumber) {
                classDate = query.getDate("classDate")
                ringNumber = query.getInt("ringNumber")
                judge = query.getString("judge")

                if (pageNumber != 0) {
                    odt.addPageBreak()
                }
                pageNumber++

                val p = odt.addParagraph("RING $ringNumber - $judge")
                p.font = Font("Times New Roman", StyleTypeDefinitions.FontStyle.REGULAR, 36.0)
                p.horizontalAlignment = StyleTypeDefinitions.HorizontalAlignmentType.CENTER
                odt.addParagraph("")

                table = odt.addTable(1, 1)
                planCell = table.getCellByPosition(0, 0)
                firstClass = true
                var marginTop = ""
            }
            if (planCell != null) {
                if (query.getInt("idAgilityClass") != idAgilityClass) {
                    idAgilityClass = query.getInt("idAgilityClass")
                    template = ClassTemplate.select(query.getInt("classCode"))
                    var isSpecialClass = template.isSpecialClass
                    var sponsor = template.sponsor
                    if (query.getInt("parentClassCode") > 0) {
                        val parentTemplate = ClassTemplate.select(query.getInt("parentClassCode"))
                        if (parentTemplate != ClassTemplate.UNDEFINED) {
                            isSpecialClass = parentTemplate.isSpecialClass
                            sponsor = parentTemplate.sponsor
                        }
                    }

                    heightColumn = 0
                    val gradeName = query.getString("gradeName")
                    val suffix = query.getString("suffix")
                    if (isSpecialClass) {
                        val className = (sponsor + " " + template.nameTemplate.replace("<grade>", gradeName)).trim()
                        cellAddParagraph(planCell, 20.0, className, bold = true, marginTop = marginTop)
                    } else {
                        val className = (template.nameTemplate.replace("<grade>", gradeName) + " " + suffix).trim()
                        cellAddParagraph(planCell, 20.0, className, marginTop = marginTop)
                    }
                    firstClass = false
                    marginTop = "1.0cm"
                    classTable = Table.newTable(SuperCell(planCell), 2, 4)
                    clearBorders(classTable, 4, 2)
                    if (query.getString("classJudge").isNotEmpty() && query.getString("classJudge").neq(judge)) {
                        cellAddParagraph(planCell, 20.0, "Judge: " + query.getString("classJudge"))
                    }
                }
                if (classTable != null) {
                    val heightCell = classTable.getCellByPosition(heightColumn, 0)
                    val entriesCell = classTable.getCellByPosition(heightColumn, 1)
                    cellAddText(heightCell, 20.0, query.getString("abbreviation"))
                    cellAddText(entriesCell, 20.0, "(${query.getInt("entries")})")
                }
                heightColumn++
            }
        }

        val odtFile = Global.showDocumentPath(competition.uniqueName, "ring_boards", "odt")
        val folder = odtFile.substringBeforeLast("/")
        odt.save(odtFile)
        NativeUtils.execute("soffice", "--headless", "--convert-to", "doc", odtFile, folderPath = folder, wait = true)
        File(odtFile).delete()
        return odtFile.replace(".odt", ".doc")

    }

    fun linkGrandPrix(idCompetition: Int, date: Date, idAgilityClassParent: Int) {
        val child = AgilityClass()
        child.select("""
            idCompetition=$idCompetition AND
            classDate=${date.sqlDate} AND
            classCode=${ClassTemplate.AGILITY.code} AND
            classCodeInstance=0 AND
            gradeCodes IN ('UKA02', 'UKA03', 'UKA04')
        """)

        while (child.next()) {
            child.idAgilityClassParent = idAgilityClassParent
            child.groupColumn = 1
            child.name = child.shortDescription
            child.nameLong = child.description
            child.post()
        }
    }

    fun linkChallenge(idCompetition: Int, date: Date, idAgilityClassParent: Int) {
        val child = AgilityClass()
        child.select("""
            idCompetition=$idCompetition AND
            classDate=${date.sqlDate} AND
            classCode=${ClassTemplate.AGILITY.code} AND
            classCodeInstance=0 AND
            gradeCodes IN ('UKA01', 'UKA02')
        """)

        while (child.next()) {
            child.idAgilityClassParent = idAgilityClassParent
            child.name = child.shortDescription
            child.nameLong = child.description
            child.groupColumn = 2
            child.post()
        }

        child.select("""
            idCompetition=$idCompetition AND
            classDate=${date.sqlDate} AND
            classCode=${ClassTemplate.JUMPING.code} AND
            classCodeInstance=0 AND
            gradeCodes IN ('UKA01', 'UKA02')
        """)

        while (child.next()) {
            child.idAgilityClassParent = idAgilityClassParent
            child.groupColumn = 1
            child.name = child.shortDescription
            child.nameLong = child.description
            child.post()
        }
    }

    fun linkBeginnersHeat(idCompetition: Int, date: Date, idAgilityClassParent: Int) {
        val child = AgilityClass()
        child.select("""
            idCompetition=$idCompetition AND
            classDate=${date.sqlDate} AND
            classCodeInstance=0 AND
            classCode=${ClassTemplate.STEEPLECHASE.code} AND
            gradeCodes IN ('UKA01')
        """)

        while (child.next()) {
            child.idAgilityClassParent = idAgilityClassParent
            child.groupColumn = 1
            child.name = child.shortDescription
            child.nameLong = child.description
            child.post()
        }
    }

    fun addUka(idCompetition: Int, classCode: Int, classCodeInstance: Int, classDate: Date,
               gradeCode: String, suffix: Boolean = false, entryFee: Int = 0, idAgilityClassParent: Int? = null): Int {

        val gradeName = Grade.getGradeName(gradeCode)
        val template = ClassTemplate.select(classCode)
        val agilityClass = AgilityClass()

        agilityClass.append()
        agilityClass.idCompetition = idCompetition
        agilityClass.code = classCode
        agilityClass.codeInstance = classCodeInstance
        agilityClass.suffix = if (suffix) "${classCodeInstance + 1}" else ""
        agilityClass.date = classDate
        if (gradeCode.isNotEmpty()) {
            agilityClass.gradeCodes = gradeCode
        } else if (template == ClassTemplate.MASTERS || template.parent == ClassTemplate.MASTERS) {
            agilityClass.gradeCodes = "UKA04"
        } else {
            agilityClass.gradeCodes = "UKA01,UKA02,UKA03,UKA04"
        }
        agilityClass.entryFee = entryFee
        agilityClass.heightOptions = template.heightOptions
        agilityClass.heightCodes = template.heightCodes
        agilityClass.jumpHeightCodes = template.jumpHeightCodes
        if (idAgilityClassParent != null) agilityClass.idAgilityClassParent = idAgilityClassParent
        agilityClass.name = agilityClass.shortDescription
        agilityClass.nameLong = agilityClass.description
        agilityClass.post()

        if (template.type.oneOf(CLASS_TYPE_SPECIAL_GROUP, CLASS_TYPE_SPECIAL_GROUP_MEMBER)) {
            val idParent = agilityClass.id
            for (child in template.children) {
                addUka(idCompetition, child.code, classCodeInstance, classDate.addDays(child.day - 1), gradeCode, idAgilityClassParent = idParent)
            }
        } else if (template.isSeries) {
            val next = template.next
            if (next != null) {
                addUka(idCompetition, next.code, classCodeInstance, classDate.addDays(next.day - 1), gradeCode)
            }
        }
        if (template.type == CLASS_TYPE_HARVESTED) {
            when (template) {
                ClassTemplate.CHALLENGE -> {
                    linkChallenge(idCompetition, classDate, agilityClass.id)
                }
                ClassTemplate.GRAND_PRIX -> {
                    linkGrandPrix(idCompetition, classDate, agilityClass.id)
                }
                ClassTemplate.BEGINNERS_STEEPLECHASE_HEAT -> {
                    linkBeginnersHeat(idCompetition, classDate, agilityClass.id)
                }

            }
        }


        return agilityClass.id
    }

    fun dropUka(idCompetition: Int, classCode: Int, classCodeInstance: Int, classDate: Date, gradeCode: String) {
        val template = ClassTemplate.select(classCode)
        val agilityClass = AgilityClass()
        agilityClass.select(
                if (gradeCode.isEmpty())
                    "idCompetition=$idCompetition AND classCode=$classCode AND classCodeInstance=$classCodeInstance AND classDate=${classDate.sqlDate}"
                else
                    "idCompetition=$idCompetition AND classCode=$classCode AND classCodeInstance=$classCodeInstance AND classDate=${classDate.sqlDate} AND gradeCodes=${gradeCode.quoted}"
        )
        if (agilityClass.found()) {
            if (template.type == CLASS_TYPE_SPECIAL_GROUP) {
                val child = agilityClass.childClasses()
                while (child.next()) {
                    dropUka(idCompetition, child.code, child.codeInstance, child.date, child.gradeCodes)
                }
            }
            if (template.type == CLASS_TYPE_HARVESTED) {
                val child = AgilityClass()
                child.select("idAgilityClassParent=${agilityClass.id}")
                while (child.next()) {
                    child.idAgilityClassParent = 0
                    child.name = agilityClass.shortDescription
                    child.nameLong = agilityClass.description
                    child.post()
                }
            }
            agilityClass.delete()
        }
    }

    fun checkSuffixUka(idCompetition: Int, classCode: Int, classCodeInstance: Int, classDate: Date, gradeCode: String, suffix: Boolean = false) {
        val template = ClassTemplate.select(classCode)
        val agilityClass = AgilityClass()
        agilityClass.select(
                if (gradeCode.isEmpty())
                    "idCompetition=$idCompetition AND classCode=$classCode AND classCodeInstance=$classCodeInstance AND classDate=${classDate.sqlDate}"
                else
                    "idCompetition=$idCompetition AND classCode=$classCode AND classCodeInstance=$classCodeInstance AND classDate=${classDate.sqlDate} AND gradeCodes=${gradeCode.quoted}"
        )
        if (agilityClass.found()) {
            agilityClass.suffix = if (suffix) "${classCodeInstance + 1}" else ""
            agilityClass.name = agilityClass.shortDescription
            agilityClass.nameLong = agilityClass.description
            agilityClass.post()
        }
    }


    fun adjustClassCountUka(idCompetition: Int, classCode: Int, date: Date, current: Int, proposed: Int) {
        val template = ClassTemplate.select(classCode)
        debug("adjustClassCount", "classCode=$classCode, date=${date.shortText}, from=$current, to=$proposed")
        for (instance in 0..maxOf(current, proposed) - 1) {
            if (instance > proposed - 1) {
                if (template.isGraded) {
                    dropUka(idCompetition, classCode, instance, date, "UKA01")
                    dropUka(idCompetition, classCode, instance, date, "UKA02")
                    dropUka(idCompetition, classCode, instance, date, "UKA03")
                    dropUka(idCompetition, classCode, instance, date, "UKA04")
                } else {
                    dropUka(idCompetition, classCode, instance, date, "")
                }
            } else if (instance > current - 1) {
                if (template.isGraded) {
                    addUka(idCompetition, classCode, instance, date, "UKA01", suffix = proposed > 1)
                    addUka(idCompetition, classCode, instance, date, "UKA02", suffix = proposed > 1)
                    addUka(idCompetition, classCode, instance, date, "UKA03", suffix = proposed > 1)
                    addUka(idCompetition, classCode, instance, date, "UKA04", suffix = proposed > 1)
                } else {
                    addUka(idCompetition, classCode, instance, date, "", suffix = proposed > 1)
                }
            } else {
                if (template.isGraded) {
                    checkSuffixUka(idCompetition, classCode, instance, date, "UKA01", suffix = proposed > 1)
                    checkSuffixUka(idCompetition, classCode, instance, date, "UKA02", suffix = proposed > 1)
                    checkSuffixUka(idCompetition, classCode, instance, date, "UKA03", suffix = proposed > 1)
                    checkSuffixUka(idCompetition, classCode, instance, date, "UKA04", suffix = proposed > 1)
                } else {
                    checkSuffixUka(idCompetition, classCode, instance, date, "", suffix = proposed > 1)
                }
            }
        }
    }

    fun getHeightCodes(template: ClassTemplate): String {
        if (template.isCasual || template.isNursery) {
            return "UKA200,UKA300,UKA400,UKA550"
        } else {
            return "UKA300,UKA400,UKA550,UKA650"
        }
    }

    fun checkHarvested(idCompetition: Int = 0) {
        val where = "classCode IN (${ClassTemplate.CHALLENGE.code}, ${ClassTemplate.GRAND_PRIX.code}, ${ClassTemplate.BEGINNERS_STEEPLECHASE_HEAT.code})" +
                " AND classDate > CURDATE()" +
                if (idCompetition > 0) " AND idCompetition=$idCompetition" else ""

        AgilityClass().select(where) {
            when (it.template) {
                ClassTemplate.CHALLENGE -> {
                    linkChallenge(it.idCompetition, it.date, it.id)
                }
                ClassTemplate.GRAND_PRIX -> {
                    linkGrandPrix(it.idCompetition, it.date, it.id)
                }
                ClassTemplate.BEGINNERS_STEEPLECHASE_HEAT -> {
                    linkBeginnersHeat(it.idCompetition, it.date, it.id)
                }
                else -> {
                }
            }
        }
    }


    fun fixBroadlands() {
        val BROADLANDS = 1271361773
        dbExecute("UPDATE agilityClass SET classDate='2018-09-02' WHERE idAgilityClass=2147133869 OR idAgilityClassParent=2147133869")
        dbExecute("UPDATE agilityClass SET classDate='2018-09-02', ringOrder=ringOrder+100 WHERE  idAgilityClassParent=2147133869")
        dbExecute("UPDATE agilityClass SET idAgilityClassParent=0, sponsor='' WHERE idAgilityClassParent=1202601125")
        dbExecute("UPDATE agilityClass SET classDate='2018-09-01' WHERE idAgilityClass=1202601125")
        linkChallenge(BROADLANDS, "2018-09-01".toDate(), 1202601125)
    }

    fun juniorLeague(): String {
        val path = Global.documentPath("UKA_junior_league", "xls")
        val workbook = Workbook.createWorkbook(File(path))
        var row = 0
        var column = 0
        var total = 0
        val handler = ChangeMonitor<Int>(-1)
        with(workbook.createSheet("Results", 0)) {
            addHeading(column++, row, "Owner", width = 1.5)
            addHeading(column++, row, "Dog", width = 0.6)
            addHeading(column++, row, "Pet Name", width = 1.0)
            addHeading(column++, row, "Date", width = 0.9)
            addHeading(column++, row, "Show", width = 1.5)
            addHeading(column++, row, "Class", width = 1.5)
            addHeading(column++, row, "Place", width = 0.6)
            addHeading(column++, row, "Points", width = 0.6)
            addHeading(column++, row, "Total", width = 0.6)




            Entry().join { agilityClass }.join { team }.join { team.dog }.join { team.competitor }.join { agilityClass.competition }
                    .where("YEAR(classDate)=2018 AND classCode IN (${ClassTemplate.JUNIOR_AGILITY.code}, ${ClassTemplate.JUNIOR_JUMPING.code})" +
                            " AND hasRun AND courseFaults=0 AND agilityClass.finalized", "competitor.givenName, competitor.familyName, agilityClass.classDate, agilityClass.classCode, entry.jumpHeightCode, entry.place") {
                        row++
                        column = 0
                        if (handler.hasChanged(idTeam)) {
                            if (row > 1) row++
                            addCell(column++, row, team.competitor.fullName)
                            addCell(column++, row, team.dog.idUka)
                            addCell(column++, row, team.dog.cleanedPetName)
                            total = 0
                        } else {
                            column += 3
                        }
                        val points = when (place) {
                            1 -> 4
                            2 -> 3
                            3 -> 2
                            else -> 1
                        }
                        total += points
                        addCell(column++, row, agilityClass.date.dateText)
                        addCell(column++, row, agilityClass.competition.briefName)
                        addCell(column++, row, "${agilityClass.name} (${Height.getHeightName(jumpHeightCode)})")
                        addCell(column++, row, place)
                        addCell(column++, row, points)
                        addCell(column++, row, total)
                    }
        }

        workbook.quit()

        return path

    }

    fun gamesQualifiers(): String {
        var dogList = ""
        val winsMap = HashMap<Int, Int>()
        dbQuery("""
            SELECT
                team.idDog, COUNT(*) AS wins
            FROM
                entry
                    JOIN
                agilityClass USING (idAgilityClass)
                    JOIN
                team USING (idTeam)
            WHERE
                agilityClass.classCode BETWEEN 10 AND 19
                    AND classDate BETWEEN '2018-07-02' AND '2019-07-01'
                    AND place = 1
                    AND qualifying
            GROUP BY team.idDog
            HAVING wins >= 3
            """) {
            val idDog = getInt("idDog")
            val wins = getInt("wins")
            dogList = dogList.append("$idDog")
            winsMap[idDog] = wins
        }


        val path = Global.documentPath("UKA_games_semi_final", "xls")
        val workbook = Workbook.createWorkbook(File(path))
        var row = 0
        var column = 0
        with(workbook.createSheet("Results", 0)) {
            addHeading(column++, row, "Owner", width = 1.5)
            addHeading(column++, row, "Dog", width = 0.6)
            addHeading(column++, row, "Pet Name", width = 1.0)
            addHeading(column++, row, "Wins", width = 0.9)
            addHeading(column++, row, "Email", width = 1.5)
            Dog().join { owner }.where("idDog in ($dogList)", "givenName, familyName, petName") {
                row++
                column = 0
                val wins = winsMap[id] ?: 0
                addCell(column++, row, owner.fullName)
                addCell(column++, row, idUka)
                addCell(column++, row, cleanedPetName)
                addCell(column++, row, wins)
                addCell(column++, row, owner.email)
            }
        }
        workbook.quit()

        return path


    }

    fun mastersEntries(): String {
        val path = Global.documentPath("UKA_masters_entries", "xls")
        val workbook = Workbook.createWorkbook(File(path))
        var row = 0
        var column = 0
        val thisYear = now.format("yyyy")
        val start = "$thisYear-01-01".toDate()
        with(workbook.createSheet("Results", 0)) {
            addHeading(column++, row, "Date", width = 0.9)
            addHeading(column++, row, "Show", width = 1.5)
            addHeading(column++, row, "Height", width = 1.0)
            addHeading(column++, row, "Entries", width = 0.9)
            dbQuery("""
                SELECT
                    agilityClass.idAgilityClass, classDate, competition.name, entry.jumpHeightCode, count(*) as entries
                FROM
                    entry
                        JOIN
                    agilityClass USING (idAgilityClass)
                    JOIN
                    competition USING (idCompetition)

                WHERE
                    agilityClass.classCode = ${ClassTemplate.MASTERS.code}
                        AND classDate between ${start.sqlDate} and ${today.sqlDate}
                group by classDate, competition.name, entry.jumpHeightCode
            """) {
                val classDate = getDate("classDate")
                val competitionName = getString("name")
                val height = Height.getHeightName(getString("jumpHeightCode"))
                val entries = getInt("entries")
                row++
                column = 0
                addCell(column++, row, classDate)
                addCell(column++, row, competitionName)
                addCell(column++, row, height)
                addCell(column++, row, entries)
            }

        }
        workbook.quit()

        return path
    }

    fun refundRegistration(idAccount: Int, amount: Int) {
        val ledger = Ledger()
        ledger.append()
        ledger.idAccount = idAccount
        ledger.type = LEDGER_UKA_REGISTRATION_REFUND
        ledger.debit = ACCOUNT_UKA_HOLDING
        ledger.credit = ACCOUNT_USER
        ledger.dateEffective = today
        ledger.amount = amount
        ledger.post()
    }
    
    fun registrations(): String {
        val path = Global.documentPath("UKA_registrations", "xls")
        val workbook = Workbook.createWorkbook(File(path))
        var membership = 0
        var registration = 0
        var row = 0
        var column = 0
        var sheet = 0
        with(workbook.createSheet("Membership Online", sheet++)) {
            addHeading(column++, row, "Date", width = 0.9)
            addHeading(column++, row, "Code", width = 0.6)
            addHeading(column++, row, "Name", width = 2.5)
            addHeading(column++, row, "Description", width = 1.7)
            addHeading(column++, row, "Fee", width = 0.9, right = true)

            LedgerItem().join { ledger }.join { competitor }.where("ledgerItem.type IN ($LEDGER_ITEM_MEMBERSHIP) AND ledger.type=$LEDGER_UKA_REGISTRATION AND NOT ledger.pending", "ledger.dateEffective") {
                row++
                column = 0
                addCell(column++, row, ledger.dateEffective)
                addCell(column++, row, competitor.idUka)
                addCell(column++, row, competitor.fullName)
                addCell(column++, row, description.rightOf("-").trim())
                addCell(column++, row, Money(amount))
                membership += amount
            }
        }

        with(workbook.createSheet("Registration Online", sheet++)) {
            row = 0
            column = 0
            addHeading(column++, row, "Date", width = 0.9)
            addHeading(column++, row, "Code", width = 0.6)
            addHeading(column++, row, "Pet Name", width = 1.5)
            addHeading(column++, row, "Fee", width = 0.9, right = true)
            addHeading(column++, row, "Owner", width = 0.6)
            addHeading(column++, row, "Name", width = 2.5)

            LedgerItem().join { ledger }.join { dog }.join { dog.owner }.where("ledgerItem.type IN ($LEDGER_ITEM_DOG_REGISTRATION) AND ledger.type=$LEDGER_UKA_REGISTRATION AND NOT ledger.pending", "ledger.dateEffective") {
                row++
                column = 0
                addCell(column++, row, ledger.dateEffective)
                addCell(column++, row, dog.code)
                addCell(column++, row, dog.cleanedPetName)
                addCell(column++, row, Money(amount))
                addCell(column++, row, dog.owner.idUka)
                addCell(column++, row, dog.owner.fullName)
                registration += amount
            }
        }

        with(workbook.createSheet("Membership Paper", sheet++)) {
            row = 0
            column = 0
            addHeading(column++, row, "Date", width = 0.9)
            addHeading(column++, row, "Code", width = 0.6)
            addHeading(column++, row, "Name", width = 2.5)
            addHeading(column++, row, "Description", width = 1.7)
            addHeading(column++, row, "Fee", width = 0.9, right = true)

            LedgerItem().join { ledger }.join { competitor }.where("ledgerItem.type IN ($LEDGER_ITEM_MEMBERSHIP) AND ledger.type=$LEDGER_UKA_REGISTRATION_DIRECT AND NOT ledger.pending", "ledger.dateEffective") {
                row++
                column = 0
                addCell(column++, row, ledger.dateEffective)
                addCell(column++, row, competitor.idUka)
                addCell(column++, row, competitor.fullName)
                addCell(column++, row, description.rightOf("-").trim())
                addCell(column++, row, Money(amount))
            }
        }

        with(workbook.createSheet("Registration Paper", sheet++)) {
            row = 0
            column = 0
            addHeading(column++, row, "Date", width = 0.9)
            addHeading(column++, row, "Code", width = 0.6)
            addHeading(column++, row, "Pet Name", width = 1.5)
            addHeading(column++, row, "Fee", width = 0.9, right = true)
            addHeading(column++, row, "Owner", width = 0.6)
            addHeading(column++, row, "Name", width = 2.5)

            LedgerItem().join { ledger }.join { dog }.join { dog.owner }.where("ledgerItem.type IN ($LEDGER_ITEM_DOG_REGISTRATION) AND ledger.type=$LEDGER_UKA_REGISTRATION_DIRECT AND NOT ledger.pending", "ledger.dateEffective") {
                row++
                column = 0
                addCell(column++, row, ledger.dateEffective)
                addCell(column++, row, dog.code)
                addCell(column++, row, dog.cleanedPetName)
                addCell(column++, row, Money(amount))
                addCell(column++, row, dog.owner.idUka)
                addCell(column++, row, dog.owner.fullName)
            }
        }

        with(workbook.createSheet("Fee Payments", sheet++)) {
            addHeading(0, 0, "Plaza Statement", width = 1.8)
            addHeading(1, 0, "", width = 1.0)
            addHeading(2, 0, "", width = 1.0)
            addHeading(3, 0, "Notes", width = 3.0)
            row = 2
            addLabel(0, row, "Total Online Membership"); addCell(1, row++, Money(membership))
            addLabel(0, row, "Total Online Registration"); addCell(1, row++, Money(registration))
            addLabel(0, row, "Total Fees"); addCell(2, row++, Money(membership + registration))
            row++
            var paid = 0
            Ledger().join { account }.where("debit=$ACCOUNT_UKA_HOLDING", "ledger.dateEffective") {
                if (type==LEDGER_UKA_REGISTRATION_REFUND) {
                    addLabel(0, row, "Member refund ${dateEffective.dateText}"); addCell(1, row, Money(amount))
                    addCell(3, row++, "a/c ${account.code}")
                } else {
                    addLabel(0, row, "Paid ${dateEffective.dateText}"); addCell(1, row++, Money(amount))
                }
                paid += amount
            }
            addLabel(0, row, "Total Payments/Refunds"); addCell(2, row++, Money(paid))
            row++
            addLabel(0, row, "Total Due"); addCell(2, row++, Money(membership + registration - paid))
        }


        workbook.quit()

        return path
    }
    
    fun populateJuniorLeague() {
        val year = today.format("yyyy").toInt()
        val yearStart = "$year-01-01".toDate()
        val yearEnd = "$year-09-30".toDate()
        val qualifyingDate = "$year-12-01".toDate()
        
        dbExecute("DELETE FROM league WHERE leagueCode=$LEAGUE_UKA_JUNIOR AND dateStart=${yearStart.sqlDate}") 
        dbExecute("""
            INSERT INTO league 
            
            SELECT $LEAGUE_UKA_JUNIOR AS leagueCode,
                ${yearStart.sqlDate} AS dateStart,
                entry.heightCode,
                team.idTeam,
                SUM(entry.progressionPoints) AS points FROM
                entry
                    JOIN
                agilityClass USING (idAgilityClass)
                    JOIN
                team USING (idTeam)
                    JOIN
                competitor USING (idCompetitor)
                    JOIN
                competition USING (idCompetition)
            WHERE
                idOrganization = $ORGANIZATION_UKA
                    AND agilityClass.classDate BETWEEN ${yearStart.sqlDate} AND ${yearEnd.sqlDate}
                    AND competitor.dateOfBirth > 0
                    AND competitor.dateOfBirth > ${qualifyingDate.addYears(-18).sqlDate}
                    AND entry.progress = $PROGRESS_RUN
                    AND entry.progressionPoints > 0
            GROUP BY entry.heightCode, team.idTeam          
                    """.trimIndent())
    }
    
    fun clearPendingRegistrations() {
        Ledger().where("type = $LEDGER_UKA_REGISTRATION AND charge > 0 AND amount = 0 AND dateEffective < CURDATE() - INTERVAL 3 DAY") {
            delete()   
        }
    }
    
    /*
fun createTryOut(idCompetition: Int, year: Int, month: Int, day: Int) {
    val date = makeDate(year, month, day)
    AgilityClass.importUka(idCompetition, ClassTemplate.PENTATHLON.code, "", "", date, "", ClassTemplate.PENTATHLON.code * 1000000 + date.dateInt)
    AgilityClass.importUka(idCompetition, ClassTemplate.WAO_GAMES.code, "", "", date, "", ClassTemplate.WAO_GAMES.code * 1000000 + date.dateInt)


    val entry = Entry()
    entry.select("idAgilityClass=1796413336")
    val agilityClass = AgilityClass()
    agilityClass.seek("idCompetition=$idCompetition AND classCode>200")
    while (entry.next()) {
        agilityClass.beforeFirst()
        while (agilityClass.next()) {
            agilityClass.enter(
                    idTeam = entry.idTeam,
                    heightCode = entry.heightCode,
                    entryType = entry.type,
                    timeEntered = entry.timeEntered
            )
        }

    }
}

fun populateTryOut(idCompetition: Int, year: Int, month: Int, day: Int) {
    val date = makeDate(year, month, day)
//    AgilityClass.importUka(idCompetition, ClassTemplate.PENTATHLON.code, "", "", date, "", ClassTemplate.PENTATHLON.code * 1000000 + date.dateInt)
//    AgilityClass.importUka(idCompetition, ClassTemplate.WAO_GAMES.code, "", "", date, "", ClassTemplate.WAO_GAMES.code * 1000000 + date.dateInt)


    val entry = Entry()
    entry.select("idAgilityClass=1796413336")
    val agilityClass = AgilityClass()
    agilityClass.seek("idCompetition=$idCompetition AND classCode>200")
    while (entry.next()) {
        agilityClass.beforeFirst()
        while (agilityClass.next()) {
            agilityClass.enter(
                    idTeam = entry.idTeam,
                    heightCode = entry.heightCode,
                    entryType = entry.type,
                    timeEntered = entry.timeEntered
            )
        }

    }
}

*/


}

