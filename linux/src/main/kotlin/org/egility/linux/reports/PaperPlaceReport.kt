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
class PaperPlaceReport(val idCompetition: Int, outfile: String, copies: Int, val formal: Boolean = false, outStream: OutputStream? = null) :

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


        val classColumn = col.column("ID", "id", type.stringType())

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
                    cmp.text(field("title", type.stringType()))
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
                col.column("Rank", "rank", type.integerType()).setWidth(16).setHeight(mm(10)).setHorizontalTextAlignment(HorizontalTextAlignment.CENTER).setTitleStyle(columnTitleCentreStyle),
                col.column("Award", "blank", type.stringType()).setWidth(20).setTitleStyle(columnTitleCentreStyle),
                col.column("Code", "blank", type.stringType()).setWidth(20).setTitleStyle(columnTitleCentreStyle),
                col.column("Name", "blank", type.stringType()).setHorizontalTextAlignment(HorizontalTextAlignment.LEFT),
                col.column("Faults", "blank", type.stringType()).setWidth(24).setTitleStyle(columnTitleCentreStyle),
                col.column("Time", "blank", type.stringType()).setWidth(36).setTitleStyle(columnTitleCentreStyle)
            )
            .groupBy(
                grp.group(classColumn)
                    .setHeaderLayout(GroupHeaderLayout.EMPTY)
                    .header(
                        
                    )
                    .footer(cmp.pageBreak())
                    .setPadding(0)
            )

        if (competition.bonusCategoriesRaw.isNotEmpty()) {
            reportBuilder.addColumn(
                col.column("Categories", "blank", type.stringType())
                    .setWidth(48)
                    .setTitleStyle(columnTitleCentreStyle)
                    .setHorizontalTextAlignment(HorizontalTextAlignment.CENTER)

            )
        }

        reportBuilder.generate()

        return reportBuilder

    }


    private fun getEntries(): JRDataSource {

        val dataSource = DRDataSource(
            "date", "ringNumber", "ringOrder", "id", "classNumber", "className",
            "rank", "blank", "name", "categories", "judge", "title"
        )

        val whereClause = "agilityClass.idCompetition=$idCompetition AND entry.progress<$PROGRESS_REMOVED"
        
        val subClassMonitor=ChangeMonitor("")
        var place=0

        var first = true
        val orderBy = if (competition.isFab)
            "agilityClass.classDate, agilityClass.ringNumber, agilityClass.ringOrder, FIND_IN_SET(entry.jumpHeightCode, agilityClass.heightRunningOrder), entry.runningOrder"
        else
            "agilityClass.classDate, agilityClass.ringNumber, agilityClass.ringOrder, entry.subClass, entry.runningOrder"
        Entry().join { team }.join { team.dog }.join { team.competitor }.join { agilityClass }
            .where(whereClause, orderBy) {
                if (!agilityClass.hasChildren) {
                    var subClassText = agilityClass.subClassDescription(subClass, shortGrade=false)
                    var rosettes = agilityClass.subClasses[subClass]["rosettes"].asInt
                    var trophies = agilityClass.subClasses[subClass]["trophies"].asInt
                    var awardsText = if (rosettes>0) "Placing to ${rosettes.ordinal()}" else ""
                    if (trophies>0) awardsText = awardsText + ", Trophies to ${trophies.ordinal()}"
                    if (subClassText.isNotEmpty()) subClassText = " - $subClassText"
                    var dogText = teamDescription
                    if (formal && team.dog.registeredName.neq(team.dog.petName)) dogText = "$dogText (${team.dog.registeredName})"
                    
                    if (subClassMonitor.hasChanged("${agilityClass.id}:$subClassText")) {
                        place=1 
                    }

                    dataSource.add(
                        agilityClass.date.dateText,
                        "Ring ${agilityClass.ringNumber}",
                        "(${agilityClass.ringOrder.ordinal()} class in ring)",
                        "${agilityClass.id}:$subClassText",
                        "Class " + agilityClass.nameLong.substringBefore(" "),
                        agilityClass.nameLong.substringAfter(" "),
                        place++,
                        "",
                        dogText,
                        dogCategories.getOrDefault(team.idDog, ""),
                        "Judge: ${agilityClass.judge}",
                        "RESULTS" + subClassText.upperCase + if (awardsText.isNotEmpty()) " ($awardsText)" else ""
                        
                    )
                }
            }


        return dataSource
    }

    companion object : ReportCompanion {

        override val keyword = Reports.PAPER_PLACE

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
                PaperPlaceReport(idCompetition, outFile, copies, formal)
            }
            return outFile
        }


    }


}