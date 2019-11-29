/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.linux.reports

import org.egility.library.dbobject.Competition
import org.egility.library.dbobject.CompetitionLedger
import org.egility.library.general.*
import org.egility.library.general.Global.showDocumentPath
import net.sf.dynamicreports.jasper.builder.JasperReportBuilder
import net.sf.dynamicreports.report.base.expression.AbstractSimpleExpression
import net.sf.dynamicreports.report.base.expression.AbstractValueFormatter
import net.sf.dynamicreports.report.builder.DynamicReports.*
import net.sf.dynamicreports.report.datasource.DRDataSource
import net.sf.dynamicreports.report.definition.ReportParameters
import net.sf.jasperreports.engine.JRDataSource
import java.io.File
import java.io.OutputStream
import java.util.*


open class LateEntryCreditsReportBase(val idCompetition: Int, val date: Date, outfile: String, copies: Int, outStream: OutputStream? = null) : Report(outfile, copies, outStream) {

    enum class Type {
        PAID, FREE, SPECIAL
    }

    val reportType = when (this) {
        is LateEntryFreeReport -> Type.FREE
        is LateEntrySpecialReport -> Type.SPECIAL
        else -> Type.PAID
    }

    private var mainReport = report()
    private var competition = Competition()

    init {
        try {
            competition.find(idCompetition)
        } catch (e: Throwable) {
            panic(e)
        }

        build()
    }

    fun build() {

        val lateEntryReport = cmp.subreport(LateEntryReport())

        lateEntryReport.setDataSource(lateEntryDataSource())

        try {
            mainReport.setTemplate(reportTemplate)
            when (reportType) {
                Type.FREE -> mainReport.pageHeader(createTitleComponent("", competition.uniqueName, "Complimentary Entries"))
                Type.PAID -> mainReport.pageHeader(createTitleComponent("", competition.uniqueName, "Late Entries"))
                Type.SPECIAL -> mainReport.pageHeader(createTitleComponent("", competition.uniqueName, "Special Late Entries"))
            }

            mainReport.detail(lateEntryReport, cmp.verticalGap(20))
            mainReport.pageFooter(footerComponent)
            mainReport.setDataSource(createDataSource())

            mainReport.generate()
        } catch (e: Throwable) {
            panic(e)
        }

    }

    private fun createDataSource(): JRDataSource {
        val dataSource = DRDataSource()
        dataSource.add()
        return dataSource
    }

    private inner class LateEntryReport : AbstractSimpleExpression<JasperReportBuilder>() {

        override fun evaluate(reportParameters: ReportParameters): JasperReportBuilder? {
            try {
                val report = report()
                report.setTemplate(reportTemplate)
                mainReport.setDetailOddRowStyle(oddStyle)

                /* define columns */
                val dayColumn = col.column("Day", "day", type.stringType())
                val timeColumn = col.column("Time", "time", type.stringType())
                val handlerColumn = col.column("Handler", "fullName", type.stringType())
                val typeColumn = col.column("Type", "lateEntryType", type.stringType())
                val creditsColumn = col.column("Credits", "credits", type.integerType())
                val className = col.column("Class", "className", type.stringType())
                val usedColumn = col.column("Used", "used", type.integerType())
                val cashColumn = col.column("Cash", "cash", type.doubleType())
                val chequeColumn = col.column("Cheque", "cheque", type.doubleType())

                /* column formatting */
                dayColumn.setStyle(bold12Style)
                timeColumn.setFixedWidth(10 * 7)
                creditsColumn.setFixedWidth(6 * 7).setStyle(columnStyleRight)
                typeColumn.setFixedWidth(10 * 7)
                usedColumn.setFixedWidth(6 * 7).setStyle(columnStyleRight)
                cashColumn.setValueFormatter(MoneyFormatter()).setFixedWidth(10 * 7).setStyle(columnStyleRight)
                chequeColumn.setValueFormatter(MoneyFormatter()).setFixedWidth(10 * 7).setStyle(columnStyleRight)

                when (reportType) {
                    Type.FREE -> report.columns(dayColumn, timeColumn, handlerColumn, typeColumn, creditsColumn)
                    Type.PAID -> report.columns(dayColumn, timeColumn, handlerColumn, creditsColumn, cashColumn, chequeColumn)
                    Type.SPECIAL -> report.columns(dayColumn, timeColumn, handlerColumn, className, cashColumn, chequeColumn)
                }

                report.setDataSource(createDataSource())
                report.groupBy(dayColumn)

                report.subtotalsAtSummary(
                        sbt.sum(creditsColumn),
                        //     sbt.sum(usedColumn),
                        sbt.sum(cashColumn).setValueFormatter(MoneyFormatter()),
                        sbt.sum(chequeColumn).setValueFormatter(MoneyFormatter()))
                report.subtotalsAtFirstGroupFooter(
                        sbt.sum(creditsColumn),
                        //      sbt.sum(usedColumn),
                        sbt.sum(cashColumn).setValueFormatter(MoneyFormatter()),
                        sbt.sum(chequeColumn).setValueFormatter(MoneyFormatter()))
                return report

            } catch (e: Throwable) {
                panic(e)
                return null
            }

        }
    }

    private inner class lateEntryDataSource : AbstractSimpleExpression<JRDataSource>() {

        override fun evaluate(reportParameters: ReportParameters): JRDataSource? {
            try {
                val dataSource = DRDataSource("day", "time", "fullName", "lateEntryType", "credits", "used", "cash", "cheque", "className")

                var where = "competitionLedger.idCompetition=$idCompetition"
                if (date.isNotEmpty()) where += " AND competitionLedger.accountingDate=${date.sqlDate}"
                when (reportType) {
                    LateEntryCreditsReportBase.Type.PAID ->
                        where += " AND type=$ITEM_LATE_ENTRY_PAID"
                    LateEntryCreditsReportBase.Type.FREE ->
                        where += " AND type IN ($ITEM_LATE_ENTRY_DISCRETIONARY, $ITEM_LATE_ENTRY_STAFF, $ITEM_LATE_ENTRY_UKA)"
                    LateEntryCreditsReportBase.Type.SPECIAL ->
                        where += " AND type IN ($ITEM_SPECIAL_CLASS)"
                    else -> where += " AND type<=$ITEM_LATE_ENTRY_STAFF"
                }

                val competitionLedger = CompetitionLedger()
                competitionLedger.account.joinToParent()
                competitionLedger.account.competitor.joinToParent()

                where += " AND NOT cancelled"

                competitionLedger.select(where, "competitionLedger.accountingDate, competitionLedger.dateCreated")

                competitionLedger.beforeFirst()
                while (competitionLedger.next()) {
                    var lateEntryTypeText = ""

                    when (competitionLedger.type) {
                        ITEM_LATE_ENTRY_PAID -> lateEntryTypeText = "Paid"
                        ITEM_LATE_ENTRY_DISCRETIONARY -> lateEntryTypeText = "Discretionary"
                        ITEM_LATE_ENTRY_STAFF -> lateEntryTypeText = "Staff"
                        ITEM_LATE_ENTRY_UKA -> lateEntryTypeText = "Rep"
                        ITEM_LATE_ENTRY_TRANSFER -> lateEntryTypeText = "Transfer"
                    }


                    dataSource.add(
                            competitionLedger.accountingDate.dayName(),
                            competitionLedger.dateCreated.longTime(),
                            competitionLedger.account.competitor.fullName,
                            lateEntryTypeText,
                            competitionLedger.quantity,
                            competitionLedger.quantityUsed,
                            competitionLedger.cash / 100.0,
                            competitionLedger.cheque / 100.0,
                            ClassTemplate.select(competitionLedger.classCode).rawName
                    )
                }
                return dataSource
            } catch (e: Throwable) {
                panic(e)
                return null
            }

        }

    }

    private class MoneyFormatter : AbstractValueFormatter<String, Double>() {
        override fun format(value: Double, reportParameters: ReportParameters): String {
            if (value === 0.0) {
                return ""
            } else {
                return "£%01.2f".format(value)
            }
        }
    }

    companion object {
        fun doGenerateJson(keyword: String, reportRequest: Json): String {
            return doGenerate(
                    keyword = keyword,
                    idCompetition = reportRequest["idCompetition"].asInt,
                    copies = reportRequest["copies"].asInt,
                    pdf = reportRequest["pdf"].asBoolean,
                    regenerate = reportRequest["regenerate"].asBoolean
            )
        }

        fun doGenerate(keyword: String, idCompetition: Int, date: Date= nullDate, copies: Int = 1, pdf: Boolean = false, regenerate: Boolean = true): String {
            val outFile = if (pdf || Global.alwaysToPdf) showDocumentPath(idCompetition, keyword, "pdf") else ""
            if (outFile.isEmpty() || regenerate || !File(outFile).cached()) {
                when (keyword) {
                    LateEntryCreditsReport.keyword -> LateEntryCreditsReport(idCompetition, date, outFile, copies)
                    LateEntryFreeReport.keyword -> LateEntryFreeReport(idCompetition, date, outFile, copies)
                    LateEntrySpecialReport.keyword -> LateEntrySpecialReport(idCompetition, date, outFile, copies)
                }
            }
            return outFile
        }

    }
}


class LateEntryCreditsReport(idCompetition: Int, date: Date, outfile: String, copies: Int) : LateEntryCreditsReportBase(idCompetition, date, outfile, copies) {

    companion object : ReportCompanion {

        override val keyword = Reports.LATE_ENTRY_CREDITS

        override fun generateJson(reportRequest: Json, pdfFile: Param<String>) {
            pdfFile.value = doGenerateJson(keyword, reportRequest)
        }

        fun generate(idCompetition: Int, date: Date=nullDate, copies: Int = 1, pdf: Boolean = false, regenerate: Boolean = true): String {
            return doGenerate(keyword, idCompetition, date, copies, pdf, regenerate)
        }

    }
}

class LateEntryFreeReport(idCompetition: Int, date: Date, outfile: String, copies: Int) : LateEntryCreditsReportBase(idCompetition, date, outfile, copies) {

    companion object : ReportCompanion {

        override val keyword = Reports.LATE_ENTRY_FREE

        override fun generateJson(reportRequest: Json, pdfFile: Param<String>) {
            pdfFile.value = doGenerateJson(keyword, reportRequest)
        }

        fun generate(idCompetition: Int, date: Date=nullDate, copies: Int = 1, pdf: Boolean = false, regenerate: Boolean = true): String {
            return doGenerate(keyword, idCompetition, date, copies, pdf, regenerate)
        }
    }
}

class LateEntrySpecialReport(idCompetition: Int, date: Date, outfile: String, copies: Int) : LateEntryCreditsReportBase(idCompetition, date, outfile, copies) {

    companion object : ReportCompanion {

        override val keyword = Reports.LATE_ENTRY_SPECIAL

        override fun generateJson(reportRequest: Json, pdfFile: Param<String>) {
            pdfFile.value = doGenerateJson(keyword, reportRequest)
        }

        fun generate(idCompetition: Int, date: Date= nullDate, copies: Int = 1, pdf: Boolean = false, regenerate: Boolean = true): String {
            return doGenerate(keyword, idCompetition, date, copies, pdf, regenerate)
        }

    }

}
