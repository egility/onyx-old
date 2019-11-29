/*
 * Copyright (c) Mike Brickman 2014-2018
 */

package org.egility.linux.reports

import org.egility.library.dbobject.AgilityClass
import org.egility.library.dbobject.Competition
import org.egility.library.dbobject.Entry
import org.egility.library.dbobject.Height
import org.egility.library.general.*
import org.egility.library.general.Global.showDocumentPath
import net.sf.dynamicreports.jasper.builder.JasperReportBuilder
import net.sf.dynamicreports.report.builder.DynamicReports.*
import net.sf.dynamicreports.report.constant.*
import net.sf.dynamicreports.report.datasource.DRDataSource
import net.sf.jasperreports.engine.JRDataSource
import java.io.File
import java.io.OutputStream
import java.util.*


/**
 * Created by mbrickman on 30/04/18.
 */
class EmergencyScoreReport(val idCompetition: Int, val idAgilityClass: Int, outfile: String, copies: Int, val formal: Boolean=false, outStream: OutputStream? = null) : Report(outfile, copies, outStream) {

    val agilityClass = AgilityClass(idAgilityClass)
    val competition = Competition(if (idAgilityClass > 0) agilityClass.idCompetition else idCompetition)

    val textStyle = stl.style()
            .setFontName("Arial")
            .setFontSize(10)
            .setBottomPadding(2)
            .setTopPadding(2)

    val boldStyle = stl.style(textStyle).bold()
    val pageHeaderStyle = stl.style(textStyle).setFontSize(12)
    val titleStyle = stl.style(textStyle).setFontSize(14).setTopPadding(5).setBottomPadding(5)
    val boxStyle = stl.style().setBorder(stl.pen1Point().setLineWidth(0.5F)).setPadding(10)
    val boxTextStyle = stl.style(textStyle)
            .setBorder(stl.pen1Point().setLineWidth(0.5F))
            .setTopPadding(2)
            .setBottomPadding(2)
            .setLeftPadding(5)
            .setRightPadding(5)

    val boxTextStyleCenter = boxTextStyle
            .setHorizontalTextAlignment(HorizontalTextAlignment.CENTER)
            .setVerticalTextAlignment(VerticalTextAlignment.MIDDLE)

    val boxBoldStyle = stl.style(boldStyle).setBorder(stl.pen1Point().setLineWidth(0.5F)).setPadding(5)

    val template = template()
            .setPageFormat(PageType.A4, PageOrientation.PORTRAIT)
            .setPageMargin(margin().setLeft(18).setRight(18).setTop(18).setBottom(18)) //
            .setLocale(Locale.ENGLISH)
            .setTextStyle(textStyle)
            .setColumnStyle(boxTextStyle)
            .setColumnTitleStyle(boxBoldStyle)
            .setPageHeaderStyle(boldStyle)


    init {
        buildReport()
    }

    private fun buildReport(): JasperReportBuilder {
        val reportBuilder = report()
        reportBuilder.setTemplate(template)
        reportBuilder.dataSource = getEntries()
//        net.sf.jasperreports.default.pdf.encoding=UTF-8


        val classColumn = col.column("ID", "idAgilityClass", type.integerType())

        val runningOrderColumn = col.column("R/O", "runningOrder", type.integerType()).setWidth(18)
        val runningOrderColumn2 = col.column("R/O", "runningOrder", type.integerType()).setWidth(18)
        val tickColumn = col.column("Entered", "blank", type.stringType()).setWidth(26)
        val nameColumn = col.column("Name", "name", type.stringType()).setHorizontalTextAlignment(HorizontalTextAlignment.LEFT)
        val gradeColumn = col.column("Gr", "grade", type.stringType()).setWidth(14).setHorizontalTextAlignment(HorizontalTextAlignment.CENTER)
        val abcColumn = col.column("ABC", "abc", type.stringType()).setWidth(18).setHorizontalTextAlignment(HorizontalTextAlignment.CENTER)
        val heightColumn = col.column("Height", "height", type.stringType()).setWidth(22).setHorizontalTextAlignment(HorizontalTextAlignment.CENTER)

        val codeColumn = col.column("Code", "code", type.stringType()).setWidth(12).setHorizontalTextAlignment(HorizontalTextAlignment.CENTER)
        val faultsColumn = col.column("Faults", "blank", type.stringType()).setWidth(54)
        val timeColumn = col.column("Time", "blank", type.stringType()).setWidth(36)

        val ringNumber = field("ringNumber", type.stringType())
        val ringOrder = field("ringOrder", type.stringType())
        val classNumber = field("classNumber", type.stringType())
        val className = field("className", type.stringType())
        val date = field("date", type.stringType())

        val groupBuilder = grp.group(classColumn)
                .setHeaderLayout(GroupHeaderLayout.EMPTY)
                .footer(cmp.pageBreak())
                .setPadding(0)

        reportBuilder
                .pageHeader(
                        cmp.verticalList(
                                cmp.verticalList(
                                        cmp.horizontalList().add(
                                                cmp.text(classNumber)
                                                        .setFixedColumns(8)
                                                        .setStyle(pageHeaderStyle),
                                                cmp.text(className)
                                                        .setStyle(pageHeaderStyle),
                                                cmp.text(competition.briefName).setFixedColumns(competition.briefName.length)
                                                        .setStyle(pageHeaderStyle)
                                                        .setHorizontalTextAlignment(HorizontalTextAlignment.RIGHT)
                                        ),
                                        cmp.horizontalList().add(
                                                cmp.text(ringNumber)
                                                        .setFixedColumns(8)
                                                        .setStyle(pageHeaderStyle),
                                                cmp.text(ringOrder).setFixedColumns(20)
                                                        .setStyle(pageHeaderStyle),
                                                cmp.text(date)
                                                        .setStyle(pageHeaderStyle)
                                                        .setHorizontalTextAlignment(HorizontalTextAlignment.RIGHT)

                                        )
                                ).setStyle(boxStyle),
                                cmp.text("Emergency Score Sheet")
                                        .setStyle(titleStyle)
                                        .setHorizontalTextAlignment(HorizontalTextAlignment.CENTER)
                        )
                )
                //.columns(runningOrderColumn, tickColumn, nameColumn, codeColumn, gradeColumn, heightColumn)
                 
                .columns(runningOrderColumn, nameColumn, faultsColumn, timeColumn, runningOrderColumn2, tickColumn, if (competition.isFab) abcColumn else gradeColumn, heightColumn)
                .groupBy(groupBuilder)

        reportBuilder.generate()

        return reportBuilder

    }


    private fun getEntries(): JRDataSource {

        val dataSource = DRDataSource("date", "ringNumber", "ringOrder", "idAgilityClass", "classNumber", "className",
                "runningOrder", "blank", "name", "code", "grade", "height", "abc")


        var lastRunningOrder = 0
        val classMonitor = ChangeMonitor(-1)
        val heightMonitor = ChangeMonitor("")
        val whereClause = if (idAgilityClass > 0)
            "agilityClass.idAgilityClass=$idAgilityClass AND entry.progress<$PROGRESS_REMOVED"
        else
            "agilityClass.idCompetition=$idCompetition AND entry.progress<$PROGRESS_REMOVED"


        fun addBlank(agilityClass: AgilityClass) {
            dataSource.add(
                    agilityClass.date.dateText,
                    "Ring ${agilityClass.ringNumber}",
                    "(${agilityClass.ringOrder.ordinal()} class)",
                    agilityClass.id,
                    "Class " + agilityClass.nameLong.substringBefore(" "),
                    agilityClass.nameLong.substringAfter(" "),
                    lastRunningOrder,
                    "",
                    "",
                    "",
                    "",
                    "",
                    ""
            )
        }

        var first=true
        val orderBy = if (competition.isFab)
            "agilityClass.classDate, agilityClass.ringNumber, agilityClass.ringOrder, FIND_IN_SET(entry.jumpHeightCode, agilityClass.heightRunningOrder), entry.runningOrder"
        else
            "agilityClass.classDate, agilityClass.ringNumber, agilityClass.ringOrder, entry.runningOrder"
        Entry().join { team }.join { team.dog }.join { team.competitor }.join { agilityClass }
                .where(whereClause, orderBy) {
                    if (!agilityClass.hasChildren) {
                        if (classMonitor.hasChanged(idAgilityClass) or (competition.isFab && heightMonitor.hasChanged(heightCode))) {
                            if (!first) {
                                previous()
                                for (i in 1..3) {
                                    lastRunningOrder++
                                    addBlank(agilityClass)
                                }
                                next()
                            }
                            lastRunningOrder = 0
                            first = false
                        }

                        lastRunningOrder++
                        while (lastRunningOrder < runningOrder) {
                            addBlank(agilityClass)
                            lastRunningOrder++
                        }


                        dataSource.add(
                                agilityClass.date.dateText,
                                "Ring ${agilityClass.ringNumber}",
                                "(${agilityClass.ringOrder.ordinal()} class in ring)",
                                agilityClass.id,
                                "Class " + agilityClass.nameLong.substringBefore(" "),
                                agilityClass.nameLong.substringAfter(" "),
                                runningOrder,
                                "",
                                if (formal) teamDescriptionFormal else teamDescription,
                                team.dog.code.toString(),
                                gradeCode.drop(3),
                                if (agilityClass.lhoClass) Height.getHeightJumpName(jumpHeightCode) else Height.getHeightShort(jumpHeightCode),
                                if (subDivision==0) "Y" else ""
                        )
                    }
                }


        return dataSource
    }

    companion object : ReportCompanion {

        override val keyword = Reports.EMERGENCY_SCRIME

        override fun generateJson(reportRequest: Json, pdfFile: Param<String>) {
            pdfFile.value = generate(
                    idCompetition = reportRequest["idCompetition"].asInt,
                    idAgilityClass = reportRequest["idAgilityClass"].asInt,
                    copies = reportRequest["copies"].asInt,
                    formal = reportRequest["formal"].asBoolean,
                    pdf = reportRequest["pdf"].asBoolean,
                    regenerate = reportRequest["regenerate"].asBoolean
            )
        }

        fun generate(idCompetition: Int = 0, idAgilityClass: Int = 0, copies: Int = 1, formal: Boolean=false, pdf: Boolean = false, regenerate: Boolean = true): String {
            val outFile = if (pdf || Global.alwaysToPdf) (if (idAgilityClass>0)
                nameOutfile(keyword, pdf, idAgilityClass = idAgilityClass) else showDocumentPath(idCompetition, keyword, "pdf")  )
            else ""
            if (outFile.isEmpty() || regenerate || !File(outFile).cached()) {
                EmergencyScoreReport(idCompetition, idAgilityClass, outFile, copies, formal)
            }
            return outFile
        }


    }


}