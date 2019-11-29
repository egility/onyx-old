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
import org.egility.library.dbobject.CompetitionCompetitor
import org.egility.library.dbobject.CompetitionLedger
import org.egility.library.dbobject.EntityOfficial
import org.egility.library.general.*
import org.egility.library.general.Global.documentPath
import org.egility.library.general.Global.showDocumentPath
import java.io.File
import java.io.OutputStream

class MarketingReport(val idCompetition: Int, outfile: String, copies: Int, outStream: OutputStream? = null) : Report(outfile, copies, outStream) {

    private var mainReport = report()

    init {
        build()
    }

    fun build() {
        try {
            mainReport.setTemplate(reportTemplate)
            mainReport.pageHeader(createTitleComponent("", "", "Marketing Report"))
            mainReport.setDetailOddRowStyle(oddStyle)

            val priorityColumn = col.column("Priority", "priority", type.stringType())
            val entityColumn = col.column("Club", "entity", type.stringType()).setFixedWidth(30 * 7)

            mainReport.columns(
                priorityColumn,
                entityColumn,
                col.column("Name", "official", type.stringType()).setFixedWidth(14 * 7),
                col.column("Role", "role", type.stringType()).setFixedWidth(14 * 7),
                col.column("Email", "email", type.stringType()),
                col.column("Phone", "phone", type.stringType()),
                col.column("Code", "code", type.stringType()).setFixedWidth(6 * 7)
            )
            mainReport.pageFooter(footerComponent)
            mainReport.setDataSource(createDataSource())
            mainReport.groupBy(priorityColumn, entityColumn)

            mainReport.generate()

        } catch (e: Throwable) {
            panic(e)
        }

    }

    private fun createDataSource(): JRDataSource {

        val idList=ArrayList<Int>()

        if (idCompetition>0) {
            CompetitionCompetitor().where("idCompetition=$idCompetition") {
                idList.add(idCompetitor)
            }
        }

        val dataSource = DRDataSource("priority", "entity", "official", "role", "code", "email", "phone")
        try {
            val where = if (idList.isEmpty()) "entity.marketingPriority>0" else
                "entity.marketingPriority>0 AND entityOfficial.idCompetitor IN (${idList.asCommaList()})"
            EntityOfficial().join { entity }.join { competitor }.where(where, "entity.marketingPriority, entity.name, entityOfficial.name") {

                dataSource.add(
                    "Priority ${entity.marketingPriority}",
                    "${entity.name} (${idProcesssorToText(entity.idProcessor)})",
                    name,
                    role,
                    if (idCompetitor > 0) competitor.code else "",
                    email,
                    phone
                )
            }
        } catch (e: Throwable) {
            panic(e)
        }
        return dataSource
    }


    companion object : ReportCompanion {

        override val keyword = Reports.MARKETING_REPORT

        override fun generateJson(reportRequest: Json, pdfFile: Param<String>) {
            pdfFile.value = generate(
                copies = reportRequest["copies"].asInt,
                pdf = reportRequest["pdf"].asBoolean,
                regenerate = reportRequest["regenerate"].asBoolean
            )
        }

        fun generate(idCompetition: Int=0, copies: Int = 1, pdf: Boolean = false, regenerate: Boolean = true): String {
            val outFile = if (pdf || Global.alwaysToPdf) documentPath("marketing_report", "pdf") else ""
            if (outFile.isEmpty() || regenerate || !File(outFile).exists()) {
                MarketingReport(idCompetition, outFile, copies)
            }
            return outFile
        }
    }

}
 