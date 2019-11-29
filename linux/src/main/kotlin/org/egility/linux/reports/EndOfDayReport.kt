/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.linux.reports

import net.sf.dynamicreports.jasper.builder.JasperReportBuilder
import net.sf.dynamicreports.report.builder.DynamicReports
import net.sf.dynamicreports.report.datasource.DRDataSource
import net.sf.dynamicreports.report.definition.ReportParameters
import net.sf.jasperreports.engine.JRDataSource
import org.egility.library.database.DbQuery
import org.egility.library.dbobject.CompetitionDay
import org.egility.library.dbobject.Competitor
import org.egility.library.general.*
import java.io.File
import java.io.OutputStream
import java.util.*

/**
 * Created by mbrickman on 20/08/16.
 */

class EndOfDayReport(val idCompetition: Int, val date: Date, outfile: String, copies: Int, outStream: OutputStream? = null) :
    Report(outfile, copies, outStream) {

    val day = CompetitionDay()
    val oldStyle = false // date.before("2019-01-01".toDate())

    init {
        try {
            day.seek(idCompetition, date)
            day.loadData()

            val report = DynamicReports.report()

            report.setTemplate(reportTemplate)
            if (oldStyle) {
                report.detail(
                    Credits(day).builder,
                    verticalGap(20),
                    Takings(day).builder,
                    verticalGap(20),
                    Reconciliation(day).builder,
                    verticalGap(20),
                    Share(day).builder,
                    verticalGap(20),
                    Entries(day).builder,
                    pageBreak(),
                    Cheques(day).builder
                )
            } else {
                report.detail(
                    Credits(day).builder,
                    verticalGap(20),
                    Takings(day).builder,
                    verticalGap(20),
                    Reconciliation(day).builder,
                    verticalGap(20),
                    Split(day).builder,
                    verticalGap(20),
                    Entries(day).builder,
                    pageBreak(),
                    Cheques(day).builder
                )
            }

            if (day.date == day.competition.dateEnd) {
                report.addDetail(
                    verticalGap(20),
                    Complimentaries(day).builder
                )
            }


            if (day.locked) {
                report.pageHeader(createTitleComponent(day.competition.uniqueName, day.date.fullDate(), "End of Day"))
                report.addDetail(
                    verticalGap(40),
                    DynamicReports.cmp.text("Show Manager Signature:").setStyle(bold12Style),
                    DynamicReports.cmp.verticalGap(40),
                    DynamicReports.cmp.text("UKA Rep Signature:").setStyle(bold12Style)
                )
            } else {
                report.pageHeader(createTitleComponent(day.competition.uniqueName, day.date.fullDate(), "Trail End of Day"))
            }

            report.pageFooter(footerComponent)
            report.dataSource = createDataSource()


            day.first()
            report.generate()
        } catch (e: Throwable) {
            panic(e)
        }

        build()
    }

    fun build() {
        try {
        } catch (e: Throwable) {
            panic(e)
        }
    }

    private fun createDataSource(): JRDataSource {
        val dataSource = DRDataSource("date")
        day.beforeFirst()
        while (day.next()) {
            day.loadData()
            dataSource.add(day.date)

        }
        return dataSource
    }


    companion object : ReportCompanion {

        override val keyword = Reports.END_OF_DAY

        override fun generateJson(reportRequest: Json, pdfFile: Param<String>) {
            pdfFile.value = generate(
                idCompetition = reportRequest["idCompetition"].asInt,
                date = reportRequest["date"].asDate,
                copies = reportRequest["copies"].asInt,
                pdf = reportRequest["pdf"].asBoolean,
                regenerate = reportRequest["regenerate"].asBoolean
            )
        }

        fun generate(idCompetition: Int, date: Date, copies: Int = 1, pdf: Boolean = false, regenerate: Boolean = true): String {
            val outFile =
                if (pdf || Global.alwaysToPdf) Global.showDocumentPath(idCompetition, "${keyword}_${date.fileNameDate}", "pdf") else ""
            if (outFile.isEmpty() || regenerate || !File(outFile).cached()) {
                EndOfDayReport(idCompetition, date, outFile, copies)
            }
            return outFile
        }

    }
}

class Credits(val day: CompetitionDay) : SubReport() {

    override fun evaluate(parameters: ReportParameters?): JasperReportBuilder {
        var report = createReport("Late Entry Credits Issued")
        report.addStringColumn("Type", "type", 12)
        report.addIntColumn("Quantity", "entries", 10)
        return report
    }

    override fun createDataSource(): JRDataSource {
        val query = DbQuery(
            """
                SELECT
                    type,
                    SUM(IF(cheque = 0, quantity, 0)) AS cash,
                    SUM(IF(cheque = 0, 0, quantity)) AS cheque,
                    SUM(quantity) as total
                FROM
                    competitionLedger
                WHERE
                    idCompetition = ${day.idCompetition} AND accountingDate = ${day.date.sqlDate}
                GROUP BY type
            """
        )
        val dataSource = DRDataSource("type", "entries")

        while (query.next()) {
            when (query.getInt("type")) {
                LATE_ENTRY_PAID -> {
                    dataSource.add("Paid Cash", query.getInt("cash"))
                    dataSource.add("Paid Cheque", query.getInt("cheque"))
                }
                ITEM_LATE_ENTRY_DISCRETIONARY -> {
                    dataSource.add("Discretionary", query.getInt("total"))
                }
                ITEM_LATE_ENTRY_STAFF -> {
                    dataSource.add("Staff", query.getInt("total"))
                }
                ITEM_LATE_ENTRY_UKA -> {
                    dataSource.add("Rep", query.getInt("total"))
                }
            }
        }
        return dataSource
    }
}

class Takings(val day: CompetitionDay) : SubReport() {

    override fun evaluate(parameters: ReportParameters?): JasperReportBuilder {
        var report = createReport("Takings")
        report.addStringColumn("", "description", 20)
        report.addMoneyColumn("Cash", "cash", 10)
        report.addMoneyColumn("Cheques", "cheque", 10)
        report.addMoneyColumn("Total", "total", 10)
        return report
    }

    override fun createDataSource(): JRDataSource {
        //day.loadData()

        val dataSource = DRDataSource("description", "cash", "cheque", "total")

        dataSource.add("Late Entry Credits", day.lateEntryCash, day.lateEntryCheque, day.lateEntryCash + day.lateEntryCheque)
        dataSource.add("Masters", day.specialCash, day.specialCheque, day.specialCash + day.specialCheque)
        dataSource.add("Registrations", day.registrationsCash, day.registrationsCheque, day.registrationsCash + day.registrationsCheque)
        dataSource.add("Misc Sales", day.cashOther, 0, day.cashOther)

        return dataSource
    }

}

class Reconciliation(val day: CompetitionDay) : SubReport() {

    override fun evaluate(parameters: ReportParameters?): JasperReportBuilder {
        var report = createReport("Cash Reconciliation")
        report.addStringColumn("", "description", 20)
        report.addMoneyColumn("Cash", "cash", 10)
        return report
    }

    override fun createDataSource(): JRDataSource {
        //day.loadData()
        val dataSource = DRDataSource("description", "cash")

        dataSource.add("Cash Box", day.totalCash)
        dataSource.add("Removed", day.cashRemoved)
        dataSource.add("Float", -day.float)
        dataSource.add("Cash Sales", -day.calculatedCash)

        return dataSource
    }

}

class Share(val day: CompetitionDay) : SubReport() {

    override fun evaluate(parameters: ReportParameters?): JasperReportBuilder {
        var report = createReport("Cash Share")
        report.addStringColumn("", "description", 24)
        report.addIntColumn("Quantity", "quantity", 10, subTotal = false)
        report.addMoneyColumn("UKA", "uka", 10)
        report.addMoneyColumn("Show", "show", 10)
        return report
    }

    override fun createDataSource(): JRDataSource {
        //day.loadData()
        val dataSource = DRDataSource("description", "quantity", "uka", "show")

        var complimentaryRunsLabel = "Staff Credits Used"
        if (day.netComplimentary > 0) {
            complimentaryRunsLabel += " (${day.complimentaryRuns}-${day.competition.complimentaryAllowance})"
        }

        dataSource.add("Late Entry Credits (Cash)", day.paidCreditsCash, day.lateEntryCutCash, day.lateEntryCash - day.lateEntryCutCash)
        dataSource.add(complimentaryRunsLabel, day.netComplimentary, day.complimentaryCut, -day.complimentaryCut)
        dataSource.add("Masters (Cash)", day.specialEntriesCash, day.specialCutCash, day.specialCash - day.specialCutCash)
        dataSource.add("Cash On Account", 0, day.registrationsCash, 0)
        dataSource.add("Misc Sales", 0, 0, day.cashOther)
        dataSource.add("Float", 0, 0, day.float)
        dataSource.add("Cash Difference", 0, 0, day.differenceCash)

        return dataSource
    }

}

class Split(val day: CompetitionDay) : SubReport() {

    override fun evaluate(parameters: ReportParameters?): JasperReportBuilder {
        var report = createReport("Cash Share")
        report.addStringColumn("", "description", 24)
        report.addIntColumn("Quantity", "quantity", 10, subTotal = false)
        report.addMoneyColumn("UKA", "uka", 10)
        report.addMoneyColumn("Show", "show", 10)
        return report
    }

    override fun createDataSource(): JRDataSource {
        //day.loadData()
        val dataSource = DRDataSource("description", "quantity", "uka", "show")

        var complimentaryRunsLabel = "Staff Credits Used"
        if (day.netComplimentary > 0) {
            complimentaryRunsLabel += " (${day.complimentaryRuns}-${day.competition.complimentaryAllowance})"
        }

        dataSource.add("Late Entry Credits", day.paidCredits, day.lateEntryCut, day.lateEntry - day.lateEntryCut)
        dataSource.add(complimentaryRunsLabel, day.netComplimentary, day.complimentaryCut, -day.complimentaryCut)
        dataSource.add("Masters", day.specialEntries, day.specialCut, day.special - day.specialCut)
        dataSource.add("Registrations", 0, day.registrations, 0)
        dataSource.add("Misc Sales", 0, 0, day.cashOther)
        dataSource.add("Cash Difference", 0, 0, day.differenceCash)
        dataSource.add("Cheques", 0, 0, -day.takingsCheque)
        dataSource.add("Float", 0, 0, day.float)

        return dataSource
    }

}

class Entries(val day: CompetitionDay) : SubReport() {

    override fun evaluate(parameters: ReportParameters?): JasperReportBuilder {
        var report = createReport("Total Late Entry Credits Used Today")
        report.addStringColumn("Type", "type", 24)
        report.addIntColumn("Entries", "entries", 10)
        return report
    }

    override fun createDataSource(): JRDataSource {
        val query = DbQuery(
            """
                SELECT
                    SUM(lateEntryCredits) as Credits, sum(if(entryType<${ENTRY_LOW_LATE}, 1, 0)) as preEntries
                FROM
                    entry
                        JOIN
                    agilityClass USING (idAgilityClass)
                WHERE
                    agilityClass.idCompetition = ${day.idCompetition}
                        AND agilityClass.classDate = ${day.date.sqlDate}
            """
        )
        val dataSource = DRDataSource("type", "entries")

        query.first()
        dataSource.add("All Types", query.getInt("Credits"))
        return dataSource
    }

}

class Cheques(val day: CompetitionDay) : SubReport() {

    override fun evaluate(parameters: ReportParameters?): JasperReportBuilder {
        var report = createReport("Cheques List")
        report.addStringColumn("Account Holder", "fullName", 25)
        report.addMoneyColumn("L/Es", "lateEntryCheque", 10)
        report.addMoneyColumn("Masters", "specialCheque", 10)
        report.addMoneyColumn("Registration", "accountCheque", 10)
        report.addMoneyColumn("Total", "cheque", 10)
        return report
    }

    override fun createDataSource(): JRDataSource {
        val query = DbQuery()
        day.loadCheques(query)

        val dataSource = DRDataSource("fullName", "lateEntryCheque", "specialCheque", "accountCheque", "cheque")

        while (query.next()) {
            val fullName = Competitor.getFullName(query.getString("givenName"), query.getString("familyName"))
            dataSource.add(fullName, query.getInt("lateEntryCheque"), query.getInt("specialCheque"), query.getInt("accountCheque"), query.getInt("cheque"))
        }
        return dataSource
    }
}

class Complimentaries(val day: CompetitionDay) : SubReport() {

    override fun evaluate(parameters: ReportParameters?): JasperReportBuilder {
        var report = createReport("Complimentaries Used")
        report.addStringColumn("Handler", "fullName")
        report.addIntColumn("Paid", "paid", 6, subTotal = false)
        report.addIntColumn("T'fers", "transfers", 6, subTotal = false)
        report.addIntColumn("Rep", "rep", 6)
        report.addIntColumn("Discr.", "discretionary", 6)
        report.addIntColumn("Staff", "staff", 6)
        report.addIntColumn("Staff Used", "complimentaryUsed", 6)
        return report
    }

    override fun createDataSource(): JRDataSource {
        val dataSource =
            DRDataSource("fullName", "paid", "transfers", "rep", "discretionary", "staff", "quantityUsed", "complimentaryUsed")
        try {
            var query = DbQuery(
                """
                    SELECT
                        givenName,
                        familyName,
                        t1.*,
                        least(quantityUsed, greatest(0, quantityUsed - (paid + rep + transfers + discretionary))) AS complimentaryUsed
                    FROM
                        (SELECT
                            idCompetitor,
                            SUM(IF(type = ${ITEM_LATE_ENTRY_PAID}, quantity, 0)) AS paid,
                            SUM(IF(type = ${ITEM_LATE_ENTRY_DISCRETIONARY}, quantity, 0)) AS discretionary,
                            SUM(IF(type = ${ITEM_LATE_ENTRY_STAFF}, quantity, 0)) AS staff,
                            SUM(IF(type = ${ITEM_LATE_ENTRY_UKA}, quantity, 0)) AS rep,
                            SUM(IF(type = ${ITEM_LATE_ENTRY_TRANSFER}, quantity, 0)) AS transfers,
                            SUM(quantityUsed) AS quantityUsed
                        FROM
                            competitionLedger
                        WHERE
                            idCompetition = ${day.idCompetition}
                        GROUP BY idCompetitor
                        HAVING discretionary + staff + rep > 0) AS t1
                            JOIN
                        competitor USING (idCompetitor)
                """
            )

            while (query.next()) {
                dataSource.add(
                    Competitor.getFullName(query.getString("givenName"), query.getString("familyName")),
                    query.getInt("paid"),
                    query.getInt("transfers"),
                    query.getInt("rep"),
                    query.getInt("discretionary"),
                    query.getInt("staff"),
                    query.getInt("quantityUsed"),
                    query.getInt("complimentaryUsed")
                )
            }
        } catch (e: Throwable) {
            panic(e)
        }
        return dataSource
    }
}


