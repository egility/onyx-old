/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.linux.reports


import net.sf.dynamicreports.report.base.expression.AbstractValueFormatter
import net.sf.dynamicreports.report.builder.DynamicReports.*
import net.sf.dynamicreports.report.datasource.DRDataSource
import net.sf.dynamicreports.report.definition.ReportParameters
import net.sf.jasperreports.engine.JRDataSource
import org.egility.library.dbobject.Competition
import org.egility.library.dbobject.CompetitionLedger
import org.egility.library.general.*
import org.egility.library.general.Global.showDocumentPath
import java.io.File
import java.io.OutputStream

class AccountPaymentReport(val idCompetition: Int, var accountOrder: Boolean=false, outfile: String, copies: Int, outStream: OutputStream? = null) : Report(outfile, copies, outStream) {

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
        try {
            mainReport.setTemplate(reportTemplate)
            mainReport.pageHeader(createTitleComponent("", competition.uniqueName, "Account Payments"))
            mainReport.setDetailOddRowStyle(oddStyle)

            val dayColumn = col.column("Day", "day", type.stringType())
            val timeColumn = col.column("Time", "time", type.stringType())
            val dateTimeColumn = col.column("Time", "dateTime", type.stringType())
            val handlerColumn = col.column("Description", "description", type.stringType())
            val cashColumn = col.column("Cash", "cash", type.doubleType())
            val chequeColumn = col.column("Cheque", "cheque", type.doubleType())
            val inPostColumn = col.column("Promised", "inPost", type.doubleType())

            /* column formatting */
            dayColumn.setStyle(bold12Style)
            timeColumn.setFixedWidth(10 * 7)
            dateTimeColumn.setFixedWidth(10 * 7)
            cashColumn.setValueFormatter(MoneyFormatter()).setFixedWidth(10 * 7).setStyle(columnStyleRight)
            chequeColumn.setValueFormatter(MoneyFormatter()).setFixedWidth(10 * 7).setStyle(columnStyleRight)
            inPostColumn.setValueFormatter(MoneyFormatter()).setFixedWidth(10 * 7).setStyle(columnStyleRight)

            if (accountOrder) {
                mainReport.columns(handlerColumn, dateTimeColumn, cashColumn, chequeColumn, inPostColumn)
            } else {
                mainReport.columns(dayColumn, timeColumn, handlerColumn, cashColumn, chequeColumn, inPostColumn)
            }

            mainReport.subtotalsAtFirstGroupFooter(
                    sbt.sum(cashColumn).setValueFormatter(MoneyFormatter()),
                    sbt.sum(chequeColumn).setValueFormatter(MoneyFormatter()),
                    sbt.sum(inPostColumn).setValueFormatter(MoneyFormatter()))

            mainReport.pageFooter(footerComponent)
            mainReport.setDataSource(createDataSource())
            mainReport.groupBy(dayColumn)


            mainReport.subtotalsAtSummary(
                    sbt.sum(cashColumn).setValueFormatter(MoneyFormatter()),
                    sbt.sum(chequeColumn).setValueFormatter(MoneyFormatter()),
                    sbt.sum(inPostColumn).setValueFormatter(MoneyFormatter()))

            mainReport.generate()

        } catch (e: Throwable) {
            panic(e)
        }

    }

    private fun createDataSource(): JRDataSource {
        val dataSource = DRDataSource("idCompetitor", "day", "time", "dateTime", "description", "cash", "cheque", "inPost")

        try {

            var where = "competitionLedger.idCompetition=$idCompetition AND type IN ($ITEM_ON_ACCOUNT, $ITEM_REGISTRATION)"

            val competitionLedger = CompetitionLedger()
            competitionLedger.competitor.joinToParent()
            if (accountOrder) {
                competitionLedger.select(where, "DATE(competitionLedger.accountingDate), competitor.givenName, competitor.familyName")
            } else {
                competitionLedger.select(where, "competitionLedger.accountingDate, competitionLedger.dateCreated")
            }

            var description = ""
            when (competitionLedger.type) {
                ITEM_ON_ACCOUNT -> description = "${competitionLedger.competitor.fullName} (${competitionLedger.competitor.idUka})"
            }

            while (competitionLedger.next()) {
                dataSource.add(
                        competitionLedger.competitor.id,
                        competitionLedger.accountingDate.dayName(),
                        competitionLedger.dateCreated.longTime(),
                        competitionLedger.dateCreated.fullDayMinutesText,
                        if (competitionLedger.type == ITEM_ON_ACCOUNT) 
                            "${competitionLedger.competitor.fullName} (${competitionLedger.competitor.idUka}) - On Account"
                        else
                            competitionLedger.description,
                        competitionLedger.cash / 100.0,
                        competitionLedger.cheque / 100.0,
                        competitionLedger.promised / 100.0
                )
            }
        } catch (e: Throwable) {
            panic(e)
        }
        return dataSource
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

    companion object : ReportCompanion {

        override val keyword = Reports.ACCOUNT_PAYMENTS

        override fun generateJson(reportRequest: Json, pdfFile: Param<String>) {
            pdfFile.value = generate(
                    idCompetition = reportRequest["idCompetition"].asInt,
                    accountOrder = reportRequest["accountOrder"].asBoolean,
                    copies = reportRequest["copies"].asInt,
                    pdf = reportRequest["pdf"].asBoolean,
                    regenerate = reportRequest["regenerate"].asBoolean
            )
        }

        fun generate(idCompetition: Int, copies: Int = 1, pdf: Boolean = false, regenerate: Boolean = true, accountOrder: Boolean = false): String {
            val outFile = if (pdf || Global.alwaysToPdf) showDocumentPath(idCompetition, keyword, "pdf") else ""
            if (outFile.isEmpty() || regenerate || !File(outFile).exists()) {
                AccountPaymentReport(idCompetition, accountOrder, outFile, copies)
            }
            return outFile
        }
    }

}
 