/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.linux.reports

import org.egility.library.dbobject.AgilityClass
import org.egility.library.dbobject.Competition
import org.egility.library.dbobject.Entry
import org.egility.library.dbobject.Height
import org.egility.library.general.*
import org.egility.linux.tools.UkaAdmin.knockOutTable
import net.sf.dynamicreports.jasper.builder.JasperReportBuilder
import net.sf.dynamicreports.report.base.expression.AbstractSimpleExpression
import net.sf.dynamicreports.report.builder.DynamicReports.*
import net.sf.dynamicreports.report.constant.HorizontalTextAlignment
import net.sf.dynamicreports.report.datasource.DRDataSource
import net.sf.dynamicreports.report.definition.ReportParameters
import net.sf.jasperreports.engine.JRDataSource
import net.sf.jasperreports.engine.JRField
import java.io.File
import java.io.OutputStream
import net.sf.jasperreports.engine.design.JRDesignField



enum class EntryReportType { ENTRY_LIST, RUNNING_ORDER }

open class EntriesReportBase(val idAgilityClass: Int, val tournament: Boolean, val provisional: Boolean, outfile: String, copies: Int, outStream: OutputStream? = null) : Report(outfile, copies, outStream) {

    private var combineHeights = false
    private var mainReport = report()
    private var agilityClass = AgilityClass()
    private var className = ""
    private var height = Height()
    private var entry = Entry()
    private var preEntries = 0

    val heightField = JRDesignField()
    val groupField = JRDesignField()



    val reportType = if (this is EntriesReport) EntryReportType.ENTRY_LIST else EntryReportType.RUNNING_ORDER

    init {
        try {
            heightField.name = "jumpHeightCode"
            groupField.name = "group"
            
            agilityClass.find(idAgilityClass)
            if (agilityClass.found()) {
                className = agilityClass.name + if (agilityClass.ringNumber > 0) " - Ring ${agilityClass.ringNumber}" else ""
            }
            combineHeights = (agilityClass.template == ClassTemplate.SPLIT_PAIRS || agilityClass.template == ClassTemplate.TEAM) && this is EntriesReport
        } catch (e: Throwable) {
            panic(e)
        }

        build()
    }

    fun build() {

        val heightReport = cmp.subreport(heightReport())
        heightReport.setDataSource(heightDataSource())
        val reportName = if (this is RunningOrderReport) if (provisional) "Provisional Running Orders" else "Running Orders" else "Entries"

        try {
            mainReport.setTemplate(reportTemplate)
            if (tournament) {
                val competitionImage = Global.imagesFolder + "/competition/" + agilityClass.competition.logo
                val sponsorImage = Global.imagesFolder + "/sponsor/" + agilityClass.template.logo

                debug("printRO", "$competitionImage (${File(competitionImage).exists()}")

                mainReport.pageHeader(createTournamentTitleComponent(competitionImage, agilityClass.describeClassUka(false), reportName))
                mainReport.detail(
                        heightReport,
                        cmp.pageBreak())
                mainReport.pageFooter(createTournamentFooterComponent(sponsorImage, agilityClass.template.website))
            } else {
                mainReport.pageHeader(createTitleComponent(agilityClass.competitionNameDate, className, reportName))
                mainReport.detail(
                        heightReport,
                        cmp.verticalGap(20))
                //                    .setDetailSplitType(SplitType.PREVENT)
                mainReport.pageFooter(footerComponent)
            }
            mainReport.dataSource = createDataSource()
            mainReport.generate()

        } catch (e: Throwable) {
            panic(e)
        }

    }

    private fun createDataSource(): JRDataSource {
        if (combineHeights) {
            val dataSource = DRDataSource()
            dataSource.add()
            return dataSource
        } else if (agilityClass.groupRunningOrder.isNotEmpty() && reportType==EntryReportType.RUNNING_ORDER) {
            val dataSource = DRDataSource(groupField.name, heightField.name)
            for (group in agilityClass.groupRunningOrder.split(",")) {
                height.selectClassHeights(idAgilityClass, true)
                while (height.next()) {
                    dataSource.add(group, height.code)
                }
            }
            return dataSource
        } else {
            height.selectClassHeights(idAgilityClass, true)
            val dataSource = DRDataSource(heightField.name)
            while (height.next()) {
                dataSource.add(height.code)
            }
            return dataSource
        }
    }

    private inner class heightReport : AbstractSimpleExpression<JasperReportBuilder>() {

        override fun evaluate(reportParameters: ReportParameters): JasperReportBuilder {

            var report = report()
            try {
                report.setTemplate(reportTemplate)
                if (combineHeights) {
                    when (reportType) {
                        EntryReportType.RUNNING_ORDER -> {
                            entry.selectRunningOrder(idAgilityClass)
                        }
                        EntryReportType.ENTRY_LIST -> {
                            entry.selectEntries(idAgilityClass, "", agilityClass.template)
                        }
                    }
                } else {
                    val jumpHeightCode=(mainReport.dataSource.getFieldValue(heightField) as String?)?:""
                    val group=(mainReport.dataSource.getFieldValue(groupField) as String?)?:""
                    val heightName=Height.getHeightJumpName(jumpHeightCode)
                    when (reportType) {
                        EntryReportType.RUNNING_ORDER -> {
                            entry.selectRunningOrder(idAgilityClass, jumpHeightCode, group)
                        }
                        EntryReportType.ENTRY_LIST -> {
                            entry.selectEntries(idAgilityClass, jumpHeightCode, agilityClass.template)
                        }
                    }
                    if (tournament) {
                        val heightOrder = agilityClass.heightRunningOrder.split(",").indexOf(jumpHeightCode) + 1
                        val text= if (reportType ==  EntryReportType.RUNNING_ORDER && group.isNotEmpty()) {
                            "Group: $group, Height: ${heightName} (${heightOrder.ordinal()})"
                        } else {
                            "Height:  ${heightName} (${heightOrder.ordinal()})"
                        }
                        report.title(
                                cmp.verticalList(
                                        cmp.filler().setHeight(10),
                                        cmp.text(text)
                                                .setStyle(root18Style)
                                                .setHorizontalTextAlignment(HorizontalTextAlignment.CENTER),
                                        cmp.filler().setHeight(10)
                                )
                        )
                    } else if (agilityClass.isUka) {
                        report.title(cmp.text(heightName).setStyle(bold12Style))
                    } else if (agilityClass.jumpHeightCodes.replace(";", ",").split(",").size>1) {
                        val text= if (reportType ==  EntryReportType.RUNNING_ORDER && group.isNotEmpty()) {
                            "Group: $group, Height: $heightName"
                        } else {
                            Height.getHeightName(jumpHeightCode)
                        }
                        report.title(cmp.text(text).setStyle(bold12Style))
                    }
                }
                if (reportType == EntryReportType.RUNNING_ORDER) {
                    report.addColumn(col.column("R/O", "runningOrder", type.integerType()).setFixedWidth(6 * 7))
                }
                when (agilityClass.template) {
                    ClassTemplate.SPLIT_PAIRS -> {
                        report.addColumn(col.column("Agility", "competitorDog1", type.stringType()))
                        report.addColumn(col.column("Jumping", "competitorDog2", type.stringType()))
                    }
                    ClassTemplate.KC_PAIRS_AGILITY, ClassTemplate.KC_PAIRS_JUMPING -> {
                        report.addColumn(col.column("1", "competitorDog1", type.stringType()))
                        report.addColumn(col.column("2", "competitorDog2", type.stringType()))
                    }
                    ClassTemplate.TEAM -> {
                        report.addColumn(col.column("Team", "team", type.stringType()).setFixedWidth(16 * 7))
                        report.addColumn(col.column("Members", "teamMembers", type.stringType()))
                        if (!tournament) {
                            report.addColumn(col.column("Notes", "notes", type.stringType()).setFixedWidth(20 * 7))
                        }
                    }
                    ClassTemplate.TEAM_INDIVIDUAL -> {
                        report.addColumn(col.column("Handler & Dog", "memberDog", type.stringType()))
                        report.addColumn(col.column("Team", "team", type.stringType()).setFixedWidth(30 * 7))
                    }
                    ClassTemplate.TEAM_RELAY -> {
                        report.addColumn(col.column("Team", "team", type.stringType()).setFixedWidth(16 * 7))
                        report.addColumn(col.column("Members", "teamMembers", type.stringType()))
                    }
                    else -> {
                        if (agilityClass.isKc) {
                            report.addColumn(col.column("Handler", "handler", type.stringType()).setFixedWidth(20 * 7))
                            report.addColumn(col.column("Dog", "dogName", type.stringType()))
                        } else {
                            report.addColumn(col.column("Handler & Dog", "competitorDog1", type.stringType()))
                            if (!tournament) {
                                report.addColumn(col.column("Notes", "notes", type.stringType()).setFixedWidth(30 * 7))
                            }
                        }
                    }
                }
                //               report.dataSource = createDataSource()
                report.setDetailOddRowStyle(oddStyle)
            } catch (e: Throwable) {
                panic(e)
            }
            return report
        }
    }

    private inner class heightDataSource : AbstractSimpleExpression<JRDataSource>() {

        override fun evaluate(reportParameters: ReportParameters): JRDataSource {
            val dataSource = DRDataSource("runningOrder", "competitorDog1", "competitorDog2", "competitorDog3", "memberDog", "team", "teamMembers", "notes", "handler", "dogName")
            try {
                entry.first()
                val showId = entry.agilityClass.isUka && !tournament
                if (agilityClass.template==ClassTemplate.UK_OPEN_CHAMPIONSHIP_FINAL && entry.runningOrder==2) {
                    dataSource.add(1, "Winner of Challenger Class".toUpperCase(), "", "", "", "", "", "")
                }
                entry.beforeFirst()
                while (entry.next()) {

                    val showHeight = combineHeights

                    val teamMembers = entry.team.getCompetitorDog(1, showHeight, showId) + ", " + entry.team.getCompetitorDog(2, showHeight, showId) + ", " + entry.team.getCompetitorDog(3, showHeight, showId)

                    var notes = ""
                    if (agilityClass.template == ClassTemplate.CIRCULAR_KNOCKOUT && entry.isPreEntry) {
                        notes += " Pre-Entry"
                    }
                    if (agilityClass.strictRunningOrder && entry.progress == PROGRESS_WITHDRAWN) {
                        notes += " Withdrawn"
                    }
                    if (agilityClass.strictRunningOrder && entry.progress > PROGRESS_WITHDRAWN) {
                        notes += " Absent?"
                    }
                    if (Competition.enforceMembership && (entry.team.hasMembershipIssues || entry.team.isRedCarded)) {
                        notes += " PLEASE REPORT TO SECRETARY"
                    }

                    dataSource.add(
                            entry.runningOrder,
                            entry.team.getCompetitorDog(1, showHeight, showId),
                            entry.team.getCompetitorDog(2, showHeight, showId),
                            entry.team.getCompetitorDog(3, showHeight, showId),
                            entry.team.getCompetitorDog(entry.teamMember, showHeight, showId),
                            entry.team.teamName,
                            teamMembers,
                            notes.trim(),
                            entry.team.getCompetitorName(1),
                            entry.team.dog.kcNameText
                    )
                }
            } catch (e: Throwable) {
                panic(e)
            }

            return dataSource
        }

    }

    companion object {

        fun doGenerateJson(keyword: String, reportRequest: Json): String {
            return doGenerate(
                    keyword = keyword,
                    tournament = reportRequest["tournament"].asBoolean,
                    idAgilityClass = reportRequest["idAgilityClass"].asInt,
                    copies = reportRequest["copies"].asInt,
                    pdf = reportRequest["pdf"].asBoolean,
                    regenerate = reportRequest["regenerate"].asBoolean
            )
        }

        fun doGenerate(keyword: String, idAgilityClass: Int, tournament: Boolean, provisional: Boolean = false, copies: Int = 1, pdf: Boolean = false, regenerate: Boolean = true): String {
            val outFile = if (tournament) {
                nameOutfile("${keyword}_tournament", pdf, idAgilityClass = idAgilityClass)
            } else {
                nameOutfile(keyword, pdf, idAgilityClass = idAgilityClass)
            }

            if (outFile.isEmpty() || regenerate || !File(outFile).cached()) {
                when (keyword) {
                    EntriesReport.keyword -> EntriesReport(idAgilityClass, tournament, outFile, copies)
                    RunningOrderReport.keyword -> {
                        if (AgilityClass.isKnockOut(idAgilityClass)) {
                            return knockOutTable(idAgilityClass, pdf, copies)
                        }
                        RunningOrderReport(idAgilityClass, tournament, provisional, outFile, copies)
                    }
                }
            }
            return outFile
        }


    }

}

class EntriesReport(idAgilityClass: Int, tournament: Boolean, outfile: String, copies: Int) : EntriesReportBase(idAgilityClass, tournament, false, outfile, copies) {

    companion object : ReportCompanion {

        override val keyword = Reports.ENTRIES

        override fun generateJson(reportRequest: Json, pdfFile: Param<String>) {
            pdfFile.value = doGenerateJson(keyword, reportRequest)
        }
    }

}

class RunningOrderReport(idAgilityClass: Int, tournament: Boolean, provisional: Boolean, outfile: String, copies: Int) : EntriesReportBase(idAgilityClass, tournament, provisional, outfile, copies) {

    companion object : ReportCompanion {

        override val keyword = Reports.RUNNING_ORDERS

        override fun generateJson(reportRequest: Json, pdfFile: Param<String>) {
            pdfFile.value = doGenerateJson(keyword, reportRequest)
        }

        fun generate(idAgilityClass: Int, tournament: Boolean, provisional: Boolean, copies: Int = 1, pdf: Boolean = false, regenerate: Boolean = true): String {
            if (AgilityClass.isKnockOut(idAgilityClass)) {
                return knockOutTable(idAgilityClass, pdf, copies)
            } else {
                return doGenerate(keyword, idAgilityClass, tournament, provisional, copies, pdf, regenerate)
            }
        }
    }

}
