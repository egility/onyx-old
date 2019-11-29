/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.linux.reports

import org.egility.library.database.DbQuery
import org.egility.library.dbobject.Competition
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

open class ChequesReport(val idCompetition: Int, outfile: String, copies: Int, outStream: OutputStream? = null) : Report(outfile, copies, outStream) {

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
            mainReport.pageHeader(createTitleComponent("", competition.uniqueName, "Cheque List"))

            val dailyChequesReport = cmp.subreport(LateEntryReport())

            dailyChequesReport.setDataSource(lateEntryDataSource())


            mainReport.detail(dailyChequesReport, cmp.verticalGap(20))
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
                val handlerColumn = col.column("Handler", "fullName", type.stringType())
                val chequeColumn = col.column("Amount", "cheque", type.doubleType())

                /* column formatting */
                dayColumn.setStyle(bold12Style)
                handlerColumn.setFixedWidth(25 * 7)
                chequeColumn.setValueFormatter(MoneyFormatter()).setFixedWidth(10 * 7).setStyle(columnStyleRight)

                report.columns(dayColumn, handlerColumn, chequeColumn)

                report.dataSource = createDataSource()
                report.groupBy(dayColumn)

                report.subtotalsAtSummary(
                        sbt.sum(chequeColumn).setValueFormatter(MoneyFormatter()))
                report.subtotalsAtFirstGroupFooter(
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
                val dataSource = DRDataSource("day", "fullName", "cheque")

                val sql = """
                    SELECT
                        competitionLedger.accountingDate, competitor.givenName, competitor.familyName, SUM(competitionLedger.cheque) AS cheque
                    FROM
                        competitionLedger
                            LEFT JOIN
                        account ON account.idAccount = competitionLedger.idAccount
                            LEFT JOIN
                        competitor ON competitor.idCompetitor = account.idCompetitor
                    WHERE
                        competitionLedger.idCompetition = """ + idCompetition + """
                            AND Cheque <> 0
                            AND NOT cancelled
                    GROUP BY DATE(competitionLedger.dateCreated) , competitor.givenName , competitor.familyName
                """

                val query = DbQuery(sql)

                while (query.next()) {

                    dataSource.add(
                            query.getDate("accountingDate").dayName(),
                            query.getString("givenName").naturalCase + " " + query.getString("familyName").naturalCase,
                            query.getInt("cheque") / 100.0
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

    companion object : ReportCompanion {

        override val keyword = Reports.CHEQUE_LIST

        override fun generateJson(reportRequest: Json, pdfFile: Param<String>) {
            pdfFile.value = generate(
                    idCompetition = reportRequest["idCompetition"].asInt,
                    copies = reportRequest["copies"].asInt,
                    pdf = reportRequest["pdf"].asBoolean,
                    regenerate = reportRequest["regenerate"].asBoolean
            )
        }

        fun generate(idCompetition: Int, copies: Int = 1, pdf: Boolean = false, regenerate: Boolean = true): String {
            val outFile = if (pdf || Global.alwaysToPdf) showDocumentPath(idCompetition, keyword, "pdf") else ""
            if (outFile.isEmpty() || regenerate || !File(outFile).cached()) {
                ChequesReport(idCompetition, outFile, copies)
            }
            return outFile
        }


    }

}

