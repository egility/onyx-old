/*
 * Copyright (c) Mike Brickman 2014-2018
 */

package org.egility.linux.reports

import net.sf.dynamicreports.report.builder.DynamicReports.*
import net.sf.dynamicreports.report.constant.*
import net.sf.dynamicreports.report.datasource.DRDataSource
import net.sf.jasperreports.engine.JRDataSource
import org.egility.library.dbobject.Competition
import org.egility.library.dbobject.CompetitionDog
import org.egility.library.dbobject.Entry
import org.egility.library.general.*
import org.egility.library.general.Global.showDocumentPath
import java.io.File
import java.io.OutputStream
import java.util.*
import kotlin.collections.HashMap

open class ScrimeSheetReport(val idCompetition: Int, outfile: String, copies: Int, val formal: Boolean = false, outStream: OutputStream? = null) :
    Report(outfile, copies, outStream) {

    val competition = Competition(idCompetition)

    val sheetMargin = mm(5.0)
    val sheetAcross = 2
    val sheetDown = 2


    val textStyle = stl.style()
        .setFontName("Arial")
        .setFontSize(11)

    val formStyle = stl.style(textStyle)
        .setLeftPadding(mm(15))
        .setRightPadding(mm(5))
        .setTopPadding(mm(5))
        .setBottomPadding(mm(5))

    var sheetHeight = (mm(297) / sheetDown)
    var areaHeight = (mm(297) / sheetDown) - mm(5) - mm(5)
    var areaWidth = (mm(210) / sheetAcross) - mm(15) - mm(5)

    val runningOrderStyle = stl.style(textStyle)
        .setFontName("Arial")
        .bold()
        .setFontSize(12)
        .setBottomBorder(stl.pen1Point())
        .setHorizontalTextAlignment(HorizontalTextAlignment.RIGHT)

    val codeStyle = stl.style(textStyle)
        .setBottomBorder(stl.pen1Point())

    val categoriesStyle = stl.style(textStyle)
        .bold()
        .setHorizontalTextAlignment(HorizontalTextAlignment.RIGHT)
        .setVerticalTextAlignment(VerticalTextAlignment.MIDDLE)

    val boxStyle = stl.style(textStyle)
        .setBorder(stl.pen1Point())

    val boxThinStyle = stl.style(textStyle)
        .setLeftBorder(stl.pen(0.2f, LineStyle.DASHED))
        .setRightBorder(stl.pen(0.2f, LineStyle.DASHED))
        .setBottomBorder(stl.pen(0.2f, LineStyle.DASHED))

    val classStyle = stl.style(textStyle)
        .bold()
        .setFontSize(9)
        .setHorizontalTextAlignment(HorizontalTextAlignment.CENTER)

    val sheetNumberStyle = stl.style(textStyle)
        .setFontSize(8)
        .setVerticalTextAlignment(VerticalTextAlignment.BOTTOM)

    val smallStyle = stl.style(textStyle)
        .setVerticalTextAlignment(VerticalTextAlignment.MIDDLE)
        .setFontSize(9)
        .setHorizontalTextAlignment(HorizontalTextAlignment.RIGHT)

    val boldStyle = stl.style(textStyle)
        .setVerticalTextAlignment(VerticalTextAlignment.MIDDLE)
        .bold()
        .setFontSize(9)
        .setHorizontalTextAlignment(HorizontalTextAlignment.RIGHT)

    val sheetSheet = template()
        .setPageFormat(PageType.A4, PageOrientation.PORTRAIT)
        .setPageMargin(margin().setLeft(mm(0)).setRight(mm(0)).setTop(mm(0)).setBottom(mm(0))) //
        .setLocale(Locale.ENGLISH)
        .setTextStyle(textStyle)

    init {
        buildReport()
    }
    
    private fun buildReport() {

        val cellHeight = mm(9)
        val scoreOffset = areaHeight - (cellHeight * 5 + mm(3)) - mm(7) - mm(0)
        val boxWidth = mm(33)
        val boxOffset = areaWidth - boxWidth
        val labelOffset = boxOffset - mm(35)

        val reportBuilder = report()
        reportBuilder.setTemplate(sheetSheet)
        reportBuilder.dataSource = getSheets()
        reportBuilder.setPageColumnsPerPage(sheetAcross)
            .setPageColumnSpace(mm(0))

        val sheet =
            cmp.xyList(
                cmp.xyListCell(mm(0), mm(0), areaWidth, mm(7), cmp.text(field("classText", type.stringType())).setWidth(areaWidth).setStyle(classStyle)),
                cmp.xyListCell(mm(0), mm(5), areaWidth, mm(7), cmp.horizontalList(
                    cmp.text(field("code", type.stringType())).setStyle(codeStyle),
                    cmp.text(field("runningOrder", type.stringType())).setStyle(runningOrderStyle)
                )),
                cmp.xyListCell(mm(0), mm(14), areaWidth, mm(20), cmp.text(field("dogText", type.stringType()))),

                cmp.xyListCell(labelOffset, scoreOffset + cellHeight * 0, cmp.text("TIME").setDimension(mm(32), cellHeight).setStyle(boldStyle)),
                cmp.xyListCell(labelOffset, scoreOffset + cellHeight * 1, cmp.text("Course Time").setDimension(mm(32), cellHeight).setStyle(smallStyle)),
                cmp.xyListCell(labelOffset, scoreOffset + cellHeight * 2, cmp.text("Time Faults").setDimension(mm(32), cellHeight).setStyle(smallStyle)),
                cmp.xyListCell(labelOffset, scoreOffset + cellHeight * 3, cmp.text("Course Faults").setDimension(mm(32), cellHeight).setStyle(smallStyle)),
                cmp.xyListCell(labelOffset, scoreOffset + cellHeight * 4, cmp.text("TOTAL FAULTS").setDimension(mm(32), cellHeight).setStyle(boldStyle)),

                cmp.xyListCell(boxOffset, scoreOffset + cellHeight * 0, boxWidth, cellHeight, cmp.text("").setStyle(boxStyle)),
                cmp.xyListCell(boxOffset, scoreOffset + cellHeight * 1, boxWidth, cellHeight, cmp.text("").setStyle(boxThinStyle)),
                cmp.xyListCell(boxOffset, scoreOffset + cellHeight * 2, boxWidth, cellHeight, cmp.text("").setStyle(boxThinStyle)),
                cmp.xyListCell(boxOffset, scoreOffset + cellHeight * 3, boxWidth, cellHeight, cmp.text("").setStyle(boxThinStyle)),
                cmp.xyListCell(boxOffset, scoreOffset + cellHeight * 4, boxWidth, cellHeight, cmp.text("").setStyle(boxStyle)),

                cmp.xyListCell(mm(0), scoreOffset + cellHeight * 5, areaWidth, cellHeight, cmp.text(field("categories", type.stringType())).setDimension(areaWidth, cellHeight).setStyle(categoriesStyle)),

                cmp.xyListCell(mm(0), areaHeight-mm(5), areaWidth, mm(5), cmp.text(field("sheetNumber", type.stringType())).setDimension(mm(5), areaWidth).setStyle(sheetNumberStyle))

            ).setStyle(formStyle).setFixedHeight(sheetHeight)


        reportBuilder.detail(sheet)
        reportBuilder.generate()

    }

    private fun getSheets(): JRDataSource {
        val dataSource = DRDataSource("classText", "dogText", "code", "runningOrder", "categories", "sheetNumber")
        var sheetNumber = 1

        var dogCategories = HashMap<Int, String>()
        if (competition.bonusCategoriesRaw.isNotEmpty()) {
            CompetitionDog().where("idCompetition=$idCompetition") {
                if (indBonusCategories.isNotEmpty()) {
                    dogCategories[idDog] = indBonusCategories.replace("_", " ").replace(",", ", ")
                }
            }
        }


        val entry = Entry()
        entry.join { team }.join { team.competitor }.join { team.dog }.join { agilityClass }.select(
            "idCompetition=$idCompetition AND entry.progress<$PROGRESS_WITHDRAWN", "agilityClass.classDate, agilityClass.ringNumber, agilityClass.ringOrder, entry.runningOrder"
        )


        val pages = (entry.rowCount + (sheetAcross * sheetDown) - 1) / (sheetAcross * sheetDown)
        for (page in 0..pages - 1) {
            for (sheet in 0..(sheetAcross * sheetDown) - 1) {
                val index = sheet * pages + page
                with(entry) {
                    if (index < rowCount) {
                        entry.cursor = index
                        val classText = "Ring ${agilityClass.ringNumber} (${agilityClass.date.dayNameShort}) - ${agilityClass.name}"
                        var dogText = teamDescription
                        if (formal && team.dog.registeredName.neq(team.dog.petName)) dogText = "$dogText (${team.dog.registeredName})"
                        val runningOrder = "R/O $runningOrder"
                        var categories = agilityClass.subClassDescription(subClass, shortGrade = true)
                        if (dogCategories.contains(team.idDog)) {
                            categories = categories.append(dogCategories[team.idDog] ?: "", ", ")
                        }
                        dataSource.add(
                            classText,
                            dogText,
                            entry.team.dog.code.toString(),
                            runningOrder,
                            categories,
                            "#${cursor + 1}"
                        )
                    } else {
                        dataSource.add(
                            "",
                            "",
                            "BLANK",
                            "",
                            "#${index + 1}"
                        )
                    }
                }
            }
        }

        return dataSource
    }


    companion object : ReportCompanion {

        override val keyword = Reports.SCRIME_SHEETS

        override fun generateJson(reportRequest: Json, pdfFile: Param<String>) {
            pdfFile.value = generate(
                idCompetition = reportRequest["idCompetition"].asInt,
                copies = reportRequest["copies"].asInt,
                pdf = reportRequest["pdf"].asBoolean,
                regenerate = reportRequest["regenerate"].asBoolean
            )
        }

        fun generate(idCompetition: Int, copies: Int = 1, formal: Boolean = false, pdf: Boolean = false, regenerate: Boolean = true): String {
            val outFile = if (pdf || Global.alwaysToPdf) showDocumentPath(idCompetition, keyword, "pdf") else ""
            if (outFile.isEmpty() || regenerate || !File(outFile).cached()) {
                ScrimeSheetReport(idCompetition, outFile, copies, formal)
            }
            return outFile
        }

    }

}