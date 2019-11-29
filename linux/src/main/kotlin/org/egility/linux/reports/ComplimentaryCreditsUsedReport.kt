/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.linux.reports

import org.egility.library.database.DbQuery
import org.egility.library.dbobject.Competition
import org.egility.library.dbobject.Competitor
import org.egility.library.general.*
import org.egility.library.general.Global.showDocumentPath
import net.sf.dynamicreports.report.builder.DynamicReports.*
import net.sf.dynamicreports.report.datasource.DRDataSource
import net.sf.jasperreports.engine.JRDataSource
import java.io.File
import java.io.OutputStream

open class ComplimentaryCreditsUsedReport(val idCompetition: Int, outfile: String, copies: Int, outStream: OutputStream? = null) : Report(outfile, copies, outStream) {

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
            mainReport.pageHeader(createTitleComponent("", competition.uniqueName, "Complimentary Entries Used"))
            mainReport.setDetailOddRowStyle(oddStyle)

            val handlerColumn = col.column("Handler", "fullName", type.stringType())
            val paidColumn = col.column("Paid", "paid", type.integerType())
            val transfersColumn = col.column("T'fers", "transfers", type.integerType())
            val repColumn = col.column("Rep", "rep", type.integerType())
            val discretionaryColumn = col.column("Discr.", "discretionary", type.integerType())
            val staffColumn = col.column("Staff", "staff", type.integerType())
            val quantityUsedColumn = col.column("Total Used", "quantityUsed", type.integerType())
            val complimentaryUsedColumn = col.column("Staff Used", "complimentaryUsed", type.integerType())
            val complimentaryRemainColumn = col.column("Staff Remain", "complimentaryRemaining", type.integerType())

            /* column formatting */
            paidColumn.setFixedWidth(6 * 7).setStyle(columnStyleRight)
            transfersColumn.setFixedWidth(6 * 7).setStyle(columnStyleRight)
            repColumn.setFixedWidth(6 * 7).setStyle(columnStyleRight)
            discretionaryColumn.setFixedWidth(6 * 7).setStyle(columnStyleRight)
            staffColumn.setFixedWidth(6 * 7).setStyle(columnStyleRight)
            quantityUsedColumn.setFixedWidth(6 * 7).setStyle(columnStyleRight)
            complimentaryUsedColumn.setFixedWidth(6 * 7).setStyle(columnStyleRight)
            complimentaryRemainColumn.setFixedWidth(6 * 7).setStyle(columnStyleRight)

            mainReport.columns(handlerColumn, paidColumn, transfersColumn, repColumn, discretionaryColumn, staffColumn, quantityUsedColumn, complimentaryUsedColumn, complimentaryRemainColumn)

            mainReport.pageFooter(footerComponent)
            mainReport.dataSource = createDataSource()

            mainReport.subtotalsAtSummary(
                    sbt.sum(repColumn),
                    sbt.sum(discretionaryColumn),
                    sbt.sum(staffColumn),
                    sbt.sum(quantityUsedColumn),
                    sbt.sum(complimentaryUsedColumn),
                    sbt.sum(complimentaryRemainColumn)
            )

            mainReport.generate()

        } catch (e: Throwable) {
            panic(e)
        }

    }

    private fun createDataSource(): JRDataSource {
        val dataSource = DRDataSource("fullName", "paid", "transfers", "rep", "discretionary", "staff", "quantityUsed", "complimentaryUsed", "complimentaryRemaining")
        try {
            dbQuery("""
                    SELECT
                        givenName,
                        familyName,
                        t1.*,
                        least(quantityUsed, greatest(0, quantityUsed - (paid + rep + transfers + discretionary))) AS complimentaryUsed
                    FROM
                        (SELECT
                            idAccount,
                            SUM(IF(type = ${ITEM_LATE_ENTRY_PAID}, quantity, 0)) AS paid,
                            SUM(IF(type = ${ITEM_LATE_ENTRY_DISCRETIONARY}, quantity, 0)) AS discretionary,
                            SUM(IF(type = ${ITEM_LATE_ENTRY_STAFF}, quantity, 0)) AS staff,
                            SUM(IF(type = ${ITEM_LATE_ENTRY_UKA}, quantity, 0)) AS rep,
                            SUM(IF(type = ${ITEM_LATE_ENTRY_TRANSFER}, quantity, 0)) AS transfers,
                            SUM(quantityUsed) AS quantityUsed
                        FROM
                            competitionLedger
                        WHERE
                            idCompetition = $idCompetition
                        GROUP BY idAccount
                        HAVING discretionary + staff + rep > 0) AS t1
                            JOIN
                        account USING (idAccount)
                            JOIN
                        competitor USING (idCompetitor)
                """){
                val givenName=getString("givenName")
                val paid=getInt("paid")
                val familyName=getString("familyName")
                dataSource.add(
                        Competitor.getFullName(getString("givenName"), getString("familyName")),
                        getInt("paid"),
                        getInt("transfers"),
                        getInt("rep"),
                        getInt("discretionary"),
                        getInt("staff"),
                        getInt("quantityUsed"),
                        getInt("complimentaryUsed"),
                        getInt("discretionary") + getInt("staff") - getInt("complimentaryUsed")
                )
            }
        } catch (e: Throwable) {
            panic(e)
        }
        return dataSource
    }

    companion object : ReportCompanion {

        override val keyword = Reports.COMPLIMENTARY_CREDITS_REPORT

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
                ComplimentaryCreditsUsedReport(idCompetition, outFile, copies)
            }
            return outFile
        }

    }


}

