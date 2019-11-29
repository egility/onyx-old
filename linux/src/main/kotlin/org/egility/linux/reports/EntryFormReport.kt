/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.linux.reports

import net.sf.dynamicreports.jasper.builder.JasperReportBuilder
import net.sf.dynamicreports.report.base.expression.AbstractSimpleExpression
import net.sf.dynamicreports.report.base.expression.AbstractValueFormatter
import net.sf.dynamicreports.report.builder.DynamicReports.*
import net.sf.dynamicreports.report.datasource.DRDataSource
import net.sf.dynamicreports.report.definition.ReportParameters
import net.sf.jasperreports.engine.JRDataSource
import org.egility.library.database.DbQuery
import org.egility.library.dbobject.Competition
import org.egility.library.dbobject.Entry
import org.egility.library.general.*
import org.egility.library.general.Global.showDocumentPath
import java.io.File
import java.io.OutputStream

open class EntryFormReport(val idCompetition: Int, val idAccount: Int, outfile: String, copies: Int, outStream: OutputStream? = null) :
    Report(outfile, copies, outStream) {

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

            dailyChequesReport.setDataSource(DogEntryDataSource())


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
                val dateColumn = col.column("Date", "date", type.stringType())
                val classColumn = col.column("Class", "class", type.stringType())
                val handlerColumn = col.column("Handler", "fullName", type.stringType())
                val feeColumn = col.column("Fee", "fee", type.doubleType())

                /* column formatting */
                dateColumn.setStyle(bold12Style)
                classColumn.setFixedWidth(25 * 7)
                handlerColumn.setFixedWidth(25 * 7)
                feeColumn.setValueFormatter(MoneyFormatter()).setFixedWidth(10 * 7).setStyle(columnStyleRight)

                report.columns(dateColumn, classColumn, handlerColumn, feeColumn)

                report.dataSource = createDataSource()
                report.groupBy(dateColumn)

                report.subtotalsAtSummary(
                    sbt.sum(feeColumn).setValueFormatter(MoneyFormatter())
                )
                report.subtotalsAtFirstGroupFooter(
                    sbt.sum(feeColumn).setValueFormatter(MoneyFormatter())
                )
                return report

            } catch (e: Throwable) {
                panic(e)
                return null
            }

        }
    }

    private inner class DogEntryDataSource : AbstractSimpleExpression<JRDataSource>() {

        override fun evaluate(reportParameters: ReportParameters): JRDataSource? {
            try {
                val dataSource = DRDataSource("date", "class", "handler", "fee")
                
                Entry().join { team }.join { agilityClass }.where("agilityClass.idCompetition=$idCompetition", "team.idDog, classDate, classNumber") {
                    dataSource.add(
                        agilityClass.date.shortText,
                        agilityClass.name,
                        team.competitorName,
                        fee / 100.0
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

        override val keyword = Reports.ENTRY_FORM

        override fun generateJson(reportRequest: Json, pdfFile: Param<String>) {
            pdfFile.value = generate(
                idCompetition = reportRequest["idCompetition"].asInt,
                idAccount = reportRequest["idAccount"].asInt,
                copies = reportRequest["copies"].asInt,
                pdf = reportRequest["pdf"].asBoolean,
                regenerate = reportRequest["regenerate"].asBoolean
            )
        }

        fun generate(idCompetition: Int, idAccount: Int = -1, copies: Int = 1, pdf: Boolean = false, regenerate: Boolean = true): String {
            val outFile = if (pdf || Global.alwaysToPdf) showDocumentPath(idCompetition, keyword, "pdf") else ""
            if (outFile.isEmpty() || regenerate || !File(outFile).cached()) {
                EntryFormReport(idCompetition, idAccount, outFile, copies)
            }
            return outFile
        }


    }

}

