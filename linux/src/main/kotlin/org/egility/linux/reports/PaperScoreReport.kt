/*
 * Copyright (c) Mike Brickman 2014-2018
 */

package org.egility.linux.reports

import net.sf.dynamicreports.jasper.builder.JasperReportBuilder
import net.sf.dynamicreports.report.builder.DynamicReports.*
import net.sf.dynamicreports.report.constant.*
import net.sf.dynamicreports.report.datasource.DRDataSource
import net.sf.jasperreports.engine.JRDataSource
import org.egility.library.dbobject.*
import org.egility.library.general.*
import org.egility.library.general.Global.showDocumentPath
import java.io.File
import java.io.OutputStream
import java.util.*


/**
 * Created by mbrickman on 30/04/18.
 */
class PaperScoreReport(val idCompetition: Int, outfile: String, copies: Int, val formal: Boolean = false, outStream: OutputStream? = null) :

    Report(outfile, copies, outStream) {

    val competition = Competition(idCompetition)

    val textStyle = stl.style()
        .setFontName("Arial")
        .setFontSize(10)
        .setBottomPadding(2)
        .setTopPadding(2)

    val pageHeaderStyle = stl.style(textStyle)
        .setFontSize(12)

    val columnTitleStyle = stl.style(textStyle)
        .setBorder(stl.pen1Point().setLineWidth(0.5F))
        .setPadding(2)
        .setVerticalTextAlignment(VerticalTextAlignment.MIDDLE)
        .bold()

    val columnTitleCentreStyle = stl.style(columnTitleStyle)
        .setHorizontalTextAlignment(HorizontalTextAlignment.CENTER)
    
    val columnStyle = stl.style(textStyle)
        .setBorder(stl.pen1Point().setLineWidth(0.5F))
        .setLeftPadding(5)
        .setRightPadding(5)
        .setVerticalTextAlignment(VerticalTextAlignment.MIDDLE)


    val titleStyle = stl.style(textStyle)
        .setFontSize(14)
        .setTopPadding(5)
        .setBottomPadding(5)
    
    val classStyle = stl.style(textStyle)
        .setVerticalTextAlignment(VerticalTextAlignment.MIDDLE)
    
    val boxStyle = stl.style()
        .setBorder(stl.pen1Point().setLineWidth(0.5F)).setPadding(10)
    
    var dogCategories = HashMap<Int, String>()

    init {
        if (competition.bonusCategoriesRaw.isNotEmpty()) {
            CompetitionDog().where("idCompetition=$idCompetition") {
                if (indBonusCategories.isNotEmpty()) {
                    dogCategories[idDog] = indBonusCategories.replace("_", " ").replace(",", ", ")
                }
            }
        }
        buildReport()
    }

    private fun buildReport(): JasperReportBuilder {
        val reportBuilder = report()
        reportBuilder.setTemplate(
            template()
                .setPageFormat(PageType.A4, PageOrientation.PORTRAIT)
                .setPageMargin(margin().setLeft(18).setRight(18).setTop(18).setBottom(18)) 
                .setLocale(Locale.ENGLISH)
                .setTextStyle(textStyle)
                .setColumnStyle(columnStyle)
                .setColumnTitleStyle(columnTitleStyle)
        )
        
        reportBuilder.dataSource = getEntries()


        val classColumn = col.column("ID", "idAgilityClass", type.integerType())

        reportBuilder
            .pageHeader(
                cmp.verticalList(
                    cmp.verticalList(
                        cmp.horizontalList().add(
                            cmp.text(field("classNumber", type.stringType()))
                                .setFixedColumns(8)
                                .setStyle(pageHeaderStyle),
                            cmp.text(field("className", type.stringType()))
                                .setStyle(pageHeaderStyle),
                            cmp.text(competition.briefName).setFixedColumns(competition.briefName.length)
                                .setStyle(pageHeaderStyle)
                                .setHorizontalTextAlignment(HorizontalTextAlignment.RIGHT)
                        ),
                        cmp.horizontalList().add(
                            cmp.text(field("ringNumber", type.stringType()))
                                .setFixedColumns(8)
                                .setStyle(pageHeaderStyle),
                            cmp.text(field("ringOrder", type.stringType())).setFixedColumns(20)
                                .setStyle(pageHeaderStyle),
                            cmp.text(field("date", type.stringType()))
                                .setStyle(pageHeaderStyle)
                                .setHorizontalTextAlignment(HorizontalTextAlignment.RIGHT)

                        )
                    ).setStyle(boxStyle),
                    cmp.text("SCORE SHEET")
                        .setStyle(titleStyle)
                        .setHorizontalTextAlignment(HorizontalTextAlignment.CENTER),
                    cmp.horizontalList(
                        cmp.text(field("judge", type.stringType())).setStyle(classStyle),
                        cmp.text("Course Length:").setStyle(classStyle),
                        cmp.text("Course Time:").setStyle(classStyle)
                    )
                    
                )
            )
            .columns(
                col.column("R/O", "runningOrder", type.integerType()).setWidth(14).setHeight(mm(10)).setHorizontalTextAlignment(HorizontalTextAlignment.CENTER).setTitleStyle(columnTitleCentreStyle),
                col.column("Code", "code", type.stringType()).setWidth(20).setHorizontalTextAlignment(HorizontalTextAlignment.CENTER).setTitleStyle(columnTitleCentreStyle),
                col.column("Name", "name", type.stringType()).setHorizontalTextAlignment(HorizontalTextAlignment.LEFT),
                col.column("Faults", "blank", type.stringType()).setWidth(24).setTitleStyle(columnTitleCentreStyle),
                col.column("Time", "blank", type.stringType()).setWidth(36).setTitleStyle(columnTitleCentreStyle)
            )
            .groupBy(
                grp.group(classColumn)
                    .setHeaderLayout(GroupHeaderLayout.EMPTY)
                    .footer(cmp.pageBreak())
                    .setPadding(0)
            )

        if (competition.hasSubClasses) {
            reportBuilder.addColumn(
                col.column("Group", "subClass", type.stringType())
                    .setWidth(24)
                    .setTitleStyle(columnTitleCentreStyle)
                    .setHorizontalTextAlignment(HorizontalTextAlignment.CENTER)
            )
        }
        
        if (competition.bonusCategoriesRaw.isNotEmpty()) {
            reportBuilder.addColumn(
                col.column("Categories", "categories", type.stringType())
                    .setWidth(72)
                    .setTitleStyle(columnTitleCentreStyle)
                    .setHorizontalTextAlignment(HorizontalTextAlignment.CENTER)
            
            )
        }

        reportBuilder.generate()

        return reportBuilder

    }


    private fun getEntries(): JRDataSource {

        val dataSource = DRDataSource(
            "date", "ringNumber", "ringOrder", "idAgilityClass", "classNumber", "className",
            "runningOrder", "blank", "name", "code", "subClass", "categories", "judge"
        )

        var lastRunningOrder = 0
        val classMonitor = ChangeMonitor(-1)
        val heightMonitor = ChangeMonitor("")
        val whereClause = "agilityClass.idCompetition=$idCompetition AND entry.progress<$PROGRESS_REMOVED"

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

        var first = true
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

                    var dogText = teamDescription
                    if (formal && team.dog.registeredName.neq(team.dog.petName)) dogText = "$dogText (${team.dog.registeredName})"

                    dataSource.add(
                        agilityClass.date.dateText,
                        "Ring ${agilityClass.ringNumber}",
                        "(${agilityClass.ringOrder.ordinal()} class in ring)",
                        agilityClass.id,
                        "Class " + agilityClass.nameLong.substringBefore(" "),
                        agilityClass.nameLong.substringAfter(" "),
                        runningOrder,
                        "",
                        dogText,
                        team.dog.code.toString(),
                        agilityClass.subClassDescription(subClass),
                        dogCategories.getOrDefault(team.idDog, ""),
                        "Judge: ${agilityClass.judge}"
                    )
                }
            }


        return dataSource
    }

    companion object : ReportCompanion {

        override val keyword = Reports.PAPER_SCORE

        override fun generateJson(reportRequest: Json, pdfFile: Param<String>) {
            pdfFile.value = generate(
                idCompetition = reportRequest["idCompetition"].asInt,
                copies = reportRequest["copies"].asInt,
                formal = reportRequest["formal"].asBoolean,
                pdf = reportRequest["pdf"].asBoolean,
                regenerate = reportRequest["regenerate"].asBoolean
            )
        }

        fun generate(idCompetition: Int = 0, copies: Int = 1, formal: Boolean = false, pdf: Boolean = false, regenerate: Boolean = true): String {
            val outFile = if (pdf || Global.alwaysToPdf)  showDocumentPath(idCompetition, keyword, "pdf") else ""
            if (outFile.isEmpty() || regenerate || !File(outFile).cached()) {
                PaperScoreReport(idCompetition, outFile, copies, formal)
            }
            return outFile
        }


    }


}