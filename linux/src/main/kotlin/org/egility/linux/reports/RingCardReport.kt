/*
 * Copyright (c) Mike Brickman 2014-2018
 */

package org.egility.linux.reports

import org.egility.library.dbobject.Competition
import org.egility.library.dbobject.CompetitionDog
import org.egility.library.dbobject.Entry
import org.egility.library.dbobject.Grade
import org.egility.library.general.*
import org.egility.library.general.Global.showDocumentPath
import net.sf.dynamicreports.jasper.builder.JasperReportBuilder
import net.sf.dynamicreports.report.base.expression.AbstractSimpleExpression
import net.sf.dynamicreports.report.builder.DynamicReports.*
import net.sf.dynamicreports.report.constant.*
import net.sf.dynamicreports.report.datasource.DRDataSource
import net.sf.dynamicreports.report.definition.ReportParameters
import net.sf.jasperreports.engine.JRDataSource
import java.io.File
import java.io.OutputStream
import java.util.*

open class RingCardReport(val idCompetition: Int, val idAccount: Int=0, outfile: String, copies: Int, outStream: OutputStream? = null) : Report(outfile, copies, outStream) {

    val competition = Competition()
    val competitionDog = CompetitionDog()

    val textStyle = stl.style()
            .setFontName("Arial")
            .setFontSize(10)
            .setBottomPadding(2)
            .setTopPadding(2)

    val boldStyle = stl.style(textStyle).bold()
    val tightText = stl.style(textStyle).setPadding(0)
    val titleStyle = stl.style(boldStyle).setFontSize(12).setBottomPadding(6)

    val rightTitle = stl.style(boldStyle).setHorizontalTextAlignment(HorizontalTextAlignment.RIGHT)

    val ringCard = template()
            .setPageFormat(PageType.A6, PageOrientation.LANDSCAPE)
            .setPageMargin(margin().setLeft(18).setRight(18).setTop(18).setBottom(18)) //
            .setLocale(Locale.ENGLISH)
            .setTextStyle(textStyle)
            .setColumnStyle(tightText)
            .setColumnTitleStyle(boldStyle)

    init {
        competition.find(idCompetition)
        buildReport()
    }

    private fun buildReport() {
        val reportBuilder = report()
        reportBuilder.setTemplate(ringCard)
        reportBuilder.dataSource = getDogs()

        val subreportBuilder = cmp.subreport(object: AbstractSimpleExpression<JasperReportBuilder>() {
            override fun evaluate(reportParameters: ReportParameters): JasperReportBuilder {
                competitionDog.cursor = reportParameters.reportRowNumber - 1
                return buildUka()
            }

        })
        subreportBuilder.setDataSource(object : AbstractSimpleExpression<JRDataSource>() {
            override fun evaluate(reportParameters: ReportParameters): JRDataSource {
                return getEntries()
            }
        })

        reportBuilder.detail(subreportBuilder, cmp.pageBreak())
        reportBuilder.generate()
    }

    fun buildUka(): JasperReportBuilder {
        val reportBuilder = report()
        reportBuilder.setTemplate(ringCard)

        val dayColumn = col.column("Day", "classDate", type.stringType()).setFixedWidth(10 * 7)
        val ringNumberColumn = col.column("Ring", "ringNumber", type.stringType()).setFixedWidth(10 * 7)
        val classColumn = col.column("Class", "className", type.stringType())
        val heightColumn = col.column("Height", "height", type.stringType()).setFixedWidth(6 * 7)
        val runningOrderColumn = col.column("R/O", "runningOrder", type.integerType()).setFixedWidth(4 * 7).setTitleStyle(rightTitle)

        val groupBuilder = grp.group(dayColumn)
                .setHideColumn(false)
                .setHeaderLayout(GroupHeaderLayout.EMPTY)
                .footer(cmp.verticalGap(4))


        val pageHeader= cmp.verticalList().add(
                cmp.text("Running Orders for ${competition.name}")
                        .setStyle(titleStyle)
                        .setHorizontalTextAlignment(HorizontalTextAlignment.CENTER),
                cmp.horizontalList().add(
                        cmp.text("${competitionDog.dog.code} ${competitionDog.dog.cleanedPetName} (${competitionDog.dog.handlerName})")
                                .setHorizontalTextAlignment(HorizontalTextAlignment.LEFT),
                        cmp.text(competitionDog.account.code)
                                .setHorizontalTextAlignment(HorizontalTextAlignment.RIGHT).setFixedWidth(14*7)
                )
        )

        reportBuilder.addPageHeader(pageHeader, cmp.line())
        reportBuilder.addColumnHeader(cmp.line(), cmp.verticalGap(4))
        reportBuilder.columns(dayColumn, ringNumberColumn, classColumn, heightColumn, runningOrderColumn)
        reportBuilder.groupBy(groupBuilder)

        return reportBuilder
    }

    private fun getDogs(): JRDataSource {
        val dataSource = DRDataSource("idAccount", "idDog")

        competitionDog.dog.joinToParent()
        competitionDog.account.joinToParent()
        if (idAccount>0) {
            competitionDog.select("idCompetition=$idCompetition AND competitionDog.idAccount=$idAccount", "account.accountCode, dog.petName")
        } else {
            dbExecute("UPDATE competitionDog SET flag=0 WHERE idCompetition=$idCompetition")
            dbExecute("""
                UPDATE LedgerItem JOIN competitionDog USING (idCompetition, idAccount) SET competitionDog.flag=1
                WHERE LedgerItem.idCompetition=$idCompetition AND
                LedgerItem.type IN ($LEDGER_ITEM_POSTAGE, $LEDGER_ITEM_PAPER)
            """)
            competitionDog.select("idCompetition=$idCompetition AND flag", "account.accountCode, dog.petName")
        }

        while (competitionDog.next()) {
            dataSource.add(
                    competitionDog.idAccount, competitionDog.idDog
            )

        }
        return dataSource
    }

    fun getEntries(): JRDataSource {

        val idDog=competitionDog.idDog

        val dataSource = DRDataSource("accountCode", "teamName", "classDate", "className", "grade", "height", "ringNumber", "ringOrder", "runningOrder")

        val entry = Entry()
        entry.account.joinToParent()
        entry.team.joinToParent()
        entry.agilityClass.joinToParent()
        entry.select("agilityClass.idCompetition=$idCompetition AND team.idDog=$idDog AND progress<$PROGRESS_TRANSFERRED AND NOT agilityClass.classCode IN (${ClassTemplate.TRY_OUT.code})", "agilityClass.classDate, agilityClass.ringNumber, agilityClass.ringOrder")

        while (entry.next()) {

            var ring = "Ring ${entry.agilityClass.ringNumber} (${entry.agilityClass.ringOrder.ordinal()})"

            if (entry.agilityClass.ringNumber==0 && entry.agilityClass.hasChildren) {
                val agilityClass=entry.agilityClass.children()
                agilityClass.first()
                ring = "Ring ${agilityClass.ringNumber} (${agilityClass.ringOrder.ordinal()})"

            }

            dataSource.add(
                    entry.account.code,
                    entry.teamDescription,
                    entry.agilityClass.date.format("EEE, d MMM"),
                    entry.agilityClass.name,
                    Grade.getGradeName(entry.gradeCode),
                    entry.jumpHeightText,
                    ring,
                    entry.agilityClass.ringOrder,
                    entry.runningOrder
            )
        }
        return dataSource
    }

    companion object : ReportCompanion {

        override val keyword = Reports.RING_CARDS

        override fun generateJson(reportRequest: Json, pdfFile: Param<String>) {
            pdfFile.value = generate(
                    idCompetition = reportRequest["idCompetition"].asInt,
                    idAccount = reportRequest["idAccount"].asInt,
                    copies = reportRequest["copies"].asInt,
                    pdf = reportRequest["pdf"].asBoolean,
                    regenerate = reportRequest["regenerate"].asBoolean
            )
        }

        fun generate(idCompetition: Int, idAccount: Int=0, copies: Int = 1, pdf: Boolean = false, regenerate: Boolean = true): String {
            val outFile = if (pdf || Global.alwaysToPdf) showDocumentPath(idCompetition, keyword, "pdf") else ""
            if (outFile.isEmpty() || regenerate || !File(outFile).cached()) {
                RingCardReport(idCompetition, idAccount, outFile, copies)
            }
            return outFile
        }


    }

}