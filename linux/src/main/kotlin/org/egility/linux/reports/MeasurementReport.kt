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
import org.egility.library.dbobject.Measurement
import org.egility.library.general.*
import org.egility.library.general.Global.showDocumentPath
import java.io.File
import java.io.OutputStream

class MeasurementReport(val idCompetition: Int, var accountOrder: Boolean=false, outfile: String, copies: Int, outStream: OutputStream? = null) : Report(outfile, copies, outStream) {

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
            mainReport.pageHeader(createTitleComponent("", competition.uniqueName, "Measurements"))
            mainReport.setDetailOddRowStyle(oddStyle)

            val dayColumn = col.column("Day", "day", type.stringType())
            val measurerColumn = col.column("Measurer", "measurer", type.stringType())
            val ownerColumn = col.column("Owner", "owner", type.stringType())
            val dogCodeColumn = col.column("Dog", "dogCode", type.stringType())
            val petNameColumn = col.column("Pet Name", "petName", type.stringType())
            val valueColumn = col.column("Measurement", "value", type.integerType())
            val provisionalColumn = col.column("Provisional", "provisional", type.stringType())

            /* column formatting */
            dayColumn.setStyle(bold12Style)
            dogCodeColumn.setFixedWidth(10 * 7).setStyle(columnStyleCenter)
            valueColumn.setFixedWidth(10 * 7).setStyle(columnStyleCenter)
            provisionalColumn.setFixedWidth(10 * 7).setStyle(columnStyleCenter)

            mainReport.columns(ownerColumn, dogCodeColumn, petNameColumn, valueColumn, provisionalColumn, measurerColumn)

            mainReport.setDataSource(createDataSource())
            mainReport.groupBy(dayColumn)

            mainReport.generate()

        } catch (e: Throwable) {
            panic(e)
        }

    }

    private fun createDataSource(): JRDataSource {
        val dataSource = DRDataSource("day", "measurer", "ownerCode", "owner", "dogCode", "petName", "value", "provisional")

        try {

            var where = "competitionLedger.idCompetition=$idCompetition AND type IN ($ITEM_ON_ACCOUNT, $ITEM_REGISTRATION)"
            
            Measurement().join { measurer }.join { dog }.join { dog.owner }.join { dog.account }.where("measurement.idCompetition=$idCompetition", 
                "DATE(measurement.dateCreated), owner.givenName, owner.familyName") {
                dataSource.add(
                    dateCreated.dayName(),
                    measurer.fullName,
                    dog.owner.code,
                    dog.owner.fullName,
                    dog.code.toString(),
                    dog.petName,
                    value,
                    if (dog.dateOfBirth.after(dateCreated.dateOnly().addYears(-2))) "Y" else ""
                )
            }

        } catch (e: Throwable) {
            panic(e)
        }
        return dataSource
    }

    companion object : ReportCompanion {

        override val keyword = Reports.MEASUREMENT

        override fun generateJson(reportRequest: Json, pdfFile: Param<String>) {
            pdfFile.value = generate(
                    idCompetition = reportRequest["idCompetition"].asInt,
                    copies = reportRequest["copies"].asInt,
                    pdf = reportRequest["pdf"].asBoolean
            )
        }

        fun generate(idCompetition: Int, copies: Int = 1, pdf: Boolean = false, regenerate: Boolean = true, accountOrder: Boolean = false): String {
            val outFile = if (pdf || Global.alwaysToPdf) showDocumentPath(idCompetition, keyword, "pdf") else ""
            if (outFile.isEmpty() || regenerate || !File(outFile).exists()) {
                MeasurementReport(idCompetition, accountOrder, outFile, copies)
            }
            return outFile
        }
    }

}
 