/*
 * Copyright (c) Mike Brickman 2014-2018
 */

package org.egility.linux.reports

import org.egility.library.dbobject.*
import org.egility.library.general.*
import org.egility.library.general.Global.showDocumentPath
import net.sf.dynamicreports.jasper.builder.JasperReportBuilder
import net.sf.dynamicreports.report.base.expression.AbstractSimpleExpression
import net.sf.dynamicreports.report.builder.DynamicReports.*
import net.sf.dynamicreports.report.builder.group.CustomGroupBuilder
import net.sf.dynamicreports.report.builder.group.GroupBuilders
import net.sf.dynamicreports.report.constant.GroupHeaderLayout
import net.sf.dynamicreports.report.constant.HorizontalTextAlignment
import net.sf.dynamicreports.report.constant.PageOrientation
import net.sf.dynamicreports.report.constant.PageType
import net.sf.dynamicreports.report.datasource.DRDataSource
import net.sf.dynamicreports.report.definition.ReportParameters
import net.sf.jasperreports.engine.JRDataSource
import java.awt.Color
import java.io.File
import java.io.OutputStream
import java.util.*


/**
 * Created by mbrickman on 30/04/18.
 */

private class FalseExpression() : AbstractSimpleExpression<Boolean>() {

    var instance = 0

    override fun evaluate(p0: ReportParameters?): Boolean {
        return instance++ > 0
    }
}

class PersonalRunningOrderReport(val idAccount: Int, val idCompetition: Int, outfile: String, copies: Int, outStream: OutputStream? = null) : Report(outfile, copies, outStream) {

    val competition = Competition(idCompetition)
    val account = Account().join{competitor}.seek(idAccount)
    val entry = Entry()


    val textStyle = stl.style()
            .setFontName("Arial")
            .setFontSize(10)
            .setBottomPadding(2)
            .setTopPadding(2)

    val boldStyle = stl.style(textStyle).bold()
    val titleStyle = stl.style(textStyle).setFontSize(14).setTopPadding(5).setBottomPadding(5)

    val template = template()
            .setPageFormat(PageType.A4, PageOrientation.PORTRAIT)
            .setPageMargin(margin().setLeft(18).setRight(18).setTop(36).setBottom(18)) //
            .setLocale(Locale.ENGLISH)
            .setTextStyle(textStyle)
            .setColumnStyle(textStyle)
            .setColumnTitleStyle(columnTitleStyle)
            .setPageHeaderStyle(boldStyle)


    init {
        buildReport()
    }

    private fun buildReport(): JasperReportBuilder {
        val reportBuilder = report()
        reportBuilder.setTemplate(template)
        reportBuilder.dataSource = getEntries()


        val teamLineColumn = col.column("teamLine", type.stringType()).setFixedWidth(1).setStyle(boldStyle)
        val dayColumn = col.column("Date", "classDate", type.stringType()).setFixedWidth(10 * 7).setPrintRepeatedDetailValues(false)
        val ringNumberColumn = col.column("Ring", "ringNumber", type.stringType()).setFixedWidth(10 * 7)
        val heightColumn = col.column("Height", "height", type.stringType()).setFixedWidth(10 * 7)
        val abcColumn = col.column("ABC", "abc", type.stringType()).setFixedWidth(4 * 7).setHorizontalTextAlignment(HorizontalTextAlignment.CENTER)
        val classColumn = col.column("Class", "className", type.stringType())
        val runningOrderColumn = col.column("R/O", "runningOrder", type.integerType()).setFixedWidth(4 * 7)

        val groupBuilder =grp.group(teamLineColumn)
                .setHideColumn(true)
                .setHeaderLayout(GroupHeaderLayout.VALUE)
                .setShowColumnHeaderAndFooter(true)
                .footer(cmp.verticalGap(10))

        reportBuilder
                .pageHeader(
                        cmp.text("Running Orders: ${account.competitor.fullName} (${competition.briefName})")
                                .setStyle(titleStyle)
                                .setHorizontalTextAlignment(HorizontalTextAlignment.CENTER)
                )
                .setColumnHeaderPrintWhenExpression(FalseExpression())
                .groupBy(groupBuilder)
        if (competition.isFab) {
            reportBuilder.columns(dayColumn, classColumn, heightColumn, abcColumn, ringNumberColumn, runningOrderColumn)
        } else {
            reportBuilder.columns(dayColumn, classColumn, ringNumberColumn, runningOrderColumn)
        }
        reportBuilder.generate()

        return reportBuilder

    }


    fun getEntries(): JRDataSource {

        val dataSource = DRDataSource("accountCode", "teamLine", "classDate", "className", "grade", "height", "ringNumber", "ringOrder", "runningOrder", "abc")
        
        val orderBy=if (competition.isFab) 
            "entry.idTeam, agilityClass.classDate, agilityClass.classCode, agilityClass.gradeCodes, agilityClass.suffix"
        else
            "entry.idTeam, agilityClass.classDate, agilityClass.classNumber, agilityClass.classCode"

        Entry().join { account }.join { team }.join { team.dog }.join { team.competitor }.join { agilityClass }
                .where("entry.idAccount=$idAccount AND agilityClass.idCompetition=$idCompetition AND agilityClass.ringNumber>0 AND entry.progress<=$PROGRESS_REMOVED", "entry.idTeam, agilityClass.classDate, agilityClass.classNumber, agilityClass.classCode") {
                    var ring = "Ring ${agilityClass.ringNumber} (${agilityClass.ringOrder.ordinal()})"
                    var teamLine = team.dog.cleanedPetName
                    if (competition.isKc) {
                        if (team.dog._petName != team.dog.registeredName) teamLine += " (${team.dog.registeredName})"
                        teamLine += " ${Grade.getGradeName(gradeCode)} / ${Height.getHeightJumpNameEx(heightCode)}"
                    }
                    val abc=if (agilityClass.isFab && subDivision==0) "Y" else if (agilityClass.isFab && subDivision==1) "N" else ""
                    dataSource.add(
                            account.code,
                            teamLine,
                            agilityClass.date.format("EEE, d MMM"),
                            agilityClass.name,
                            Grade.getGradeName(gradeCode),
                            jumpHeightText,
                            ring,
                            agilityClass.ringOrder,
                            runningOrder,
                            abc
                    )
                }
        return dataSource
    }

    companion object : ReportCompanion {

        override val keyword = Reports.PERSONAL_RUNNING_ORDERS

        override fun generateJson(reportRequest: Json, pdfFile: Param<String>) {
            pdfFile.value = generate(
                    idAccount = reportRequest["idAccount"].asInt,
                    idCompetition = reportRequest["idCompetition"].asInt,
                    copies = reportRequest["copies"].asInt,
                    pdf = reportRequest["pdf"].asBoolean,
                    regenerate = reportRequest["regenerate"].asBoolean
            )
        }

        fun generate(idAccount: Int, idCompetition: Int, copies: Int = 1, pdf: Boolean = false, regenerate: Boolean = true): String {
            val outFile = if (pdf || Global.alwaysToPdf) showDocumentPath(idCompetition, keyword, "pdf") else ""
            if (outFile.isEmpty() || regenerate || !File(outFile).cached()) {
                PersonalRunningOrderReport(idAccount, idCompetition, outFile, copies)
            }
            return outFile
        }


    }


}