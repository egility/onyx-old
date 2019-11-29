/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.linux.reports

import net.sf.dynamicreports.jasper.builder.JasperReportBuilder
import net.sf.dynamicreports.report.base.expression.AbstractSimpleExpression
import net.sf.dynamicreports.report.builder.DynamicReports.*
import net.sf.dynamicreports.report.constant.HorizontalTextAlignment
import net.sf.dynamicreports.report.constant.Markup
import net.sf.dynamicreports.report.constant.SplitType
import net.sf.dynamicreports.report.datasource.DRDataSource
import net.sf.dynamicreports.report.definition.ReportParameters
import net.sf.jasperreports.engine.JRDataSource
import org.egility.library.dbobject.AgilityClass
import org.egility.library.dbobject.Entry
import org.egility.library.dbobject.Height
import org.egility.library.general.*
import java.io.File
import java.io.OutputStream

class AwardsReport(val idAgilityClass: Int, outfile: String, copies: Int, outStream: OutputStream? = null) :
    Report(outfile, copies, outStream) {

    private var mainReport = report()
    private var agilityClass = AgilityClass()
    private var className = ""
    private var height = Height()
    private var entry = Entry()
    var pdfFile = ""
    var clearRounds = ""
    private val subGroups = ArrayList<Int>()

    init {
        try {
            agilityClass.find(idAgilityClass)
            if (agilityClass.found()) {
                className =
                    if (agilityClass.isUka) agilityClass.name else agilityClass.describeClass(short = false)
                if (agilityClass.ringNumber > 0) {
                    className = "${className} - Ring ${agilityClass.ringNumber}"
                }
            }
        } catch (e: Throwable) {
            panic(e)
        }

        build()
    }

    fun build() {

        val subReport = cmp.subreport(SubReport())
        subReport.setDataSource(ResultsDataSource())

        try {
            mainReport.setTemplate(reportTemplate)
            mainReport.pageHeader(createTitleComponent(agilityClass.competitionNameDate, className, "Awards"))
            mainReport.detail(subReport, cmp.verticalGap(20))
                .setDetailSplitType(SplitType.PREVENT)
            mainReport.pageFooter(footerComponent)
            mainReport.dataSource = createDataSource()

            mainReport.generate()

        } catch (e: Throwable) {
            panic(e)
        }

    }

    private fun createDataSource(): JRDataSource {
        if (agilityClass.isUka) {
            if (agilityClass.combineHeights) {
                val dataSource = DRDataSource("heightCode")
                dataSource.add()
                return dataSource
            } else {
                height.selectClassHeights(idAgilityClass, false)
                val dataSource = DRDataSource("heightCode")
                while (height.next()) {
                    dataSource.add(height.code)
                }
                return dataSource
            }
        } else {
            subGroups.clear()
            val dataSource = DRDataSource("subGroup")
            dbQuery("SELECT DISTINCT subClass FROM entry WHERE idAgilityClass=$idAgilityClass AND progress=$PROGRESS_RUN AND placeFlags<>0 ORDER BY subClass") {
                dataSource.add(getInt("subClass"))
                subGroups.add(getInt("subClass"))
            }
            /*
            for (index in 0..agilityClass.subClassCount-1) {
                dataSource.add(index)
            }
            */
            return dataSource

        }
    }

    private inner class SubReport : AbstractSimpleExpression<JasperReportBuilder>() {

        override fun evaluate(reportParameters: ReportParameters): JasperReportBuilder {

            var report = report()
            report.setTemplate(reportTemplate)

            try {
                if (agilityClass.isUka) {
                    if (agilityClass.combineHeights) {
                        entry.selectResultsUka(agilityClass, awards = true)
                    } else {
                        height.cursor = reportParameters.reportRowNumber - 1
                        entry.selectResultsUka(agilityClass, height.code, awards = true)
                    }
                    if (agilityClass.hasChildren) {
                        report.title(
                            cmp.horizontalList().add(
                                cmp.text(height.name).setStyle(bold12Style).setHorizontalTextAlignment(HorizontalTextAlignment.LEFT)
                            )
                        )

                    } else {
                        report.title(
                            cmp.horizontalList().add(
                                cmp.text(height.name).setStyle(bold12Style).setHorizontalTextAlignment(HorizontalTextAlignment.LEFT)
                            )
                        )
                    }

                    if (agilityClass.template == ClassTemplate.TEAM) {
                        report.columns(
                            col.column("Award", "prize", type.stringType()).setFixedWidth(6 * 7),
                            col.column("Team", "team", type.stringType())
                        )
                    } else if (agilityClass.template == ClassTemplate.SPLIT_PAIRS) {
                        report.columns(
                            col.column("Award", "prize", type.stringType()).setFixedWidth(6 * 7),
                            col.column("Pair", "pair", type.stringType())
                        )
                    } else {
                        report.columns(
                            col.column("Award", "prize", type.stringType()).setFixedWidth(6 * 7),
                            col.column("Competitor", "competitorDog", type.stringType())
                        )
                    }

                } else {
                    val subClass = subGroups[reportParameters.reportRowNumber - 1]
                    entry.selectResults(agilityClass, subClass, awards = true)
                    clearRounds = ""
                    while (entry.next()) {
                        if (entry.prizeText == "CR") {
                            clearRounds =
                                clearRounds.append(if (agilityClass.isKc) entry.teamDescriptionFormal else entry.teamDescription)
                        }
                    }

                    report.title(
                        cmp.horizontalList().add(
                            cmp.text(agilityClass.subClassDescription(subClass, shortGrade = false)).setStyle(bold12Style).setHorizontalTextAlignment(HorizontalTextAlignment.LEFT)
                        )
                    )
                    val listBuilder = cmp.verticalList()
                    if (clearRounds.isNotEmpty()) {
                        listBuilder.add(cmp.text("<b>Clear Rounds:</b> $clearRounds").setMarkup(Markup.HTML))
                    }
                    report.summary(listBuilder)

                    report.columns(
                        col.column("Award", "prize", type.stringType()).setFixedWidth(6 * 7),
                        col.column(if (agilityClass.isGamblers || agilityClass.isSnooker) "Score" else "Faults", "score", type.stringType()).setFixedWidth(6 * 7).setStyle(columnStyleRight),
                        col.column("Time", "timeText", type.stringType()).setFixedWidth(7 * 7).setStyle(columnStyleCenter),
                        col.column("Competitor", if (agilityClass.isKc) "competitorDogFormal" else "competitorDog", type.stringType())
                    )
                }

                report.setDataSource(createDataSource())
                report.setDetailOddRowStyle(oddStyle)
            } catch (e: Throwable) {
                panic(e)
            }

            return report
        }
    }

    private inner class ResultsDataSource : AbstractSimpleExpression<JRDataSource>() {

        override fun evaluate(reportParameters: ReportParameters): JRDataSource {
            val dataSource =
                DRDataSource("prize", "score", "timeText", "competitorDog", "competitorDogFormal", "team", "pair")
            try {
                entry.beforeFirst()
                while (entry.next()) {
                    if (entry.prizeText != "CR") {
                        dataSource.add(
                            entry.prizeText,
                            entry.scoreText,
                            entry.simpleTimeText,
                            entry.teamDescription,
                            entry.teamDescriptionFormal,
                            entry.team.teamName,
                            "${entry.team.getCompetitorDog(1)} / ${entry.team.getCompetitorDog(2)}"
                        )
                    }
                }
            } catch (e: Throwable) {
                panic(e)
            }

            return dataSource
        }

    }

    companion object : ReportCompanion {

        override val keyword = Reports.AWARDS

        override fun generateJson(reportRequest: Json, pdfFile: Param<String>) {
            pdfFile.value = generate(
                idAgilityClass = reportRequest["idAgilityClass"].asInt,
                copies = reportRequest["copies"].asInt,
                pdf = reportRequest["pdf"].asBoolean,
                regenerate = reportRequest["regenerate"].asBoolean
            )
        }

        fun generate(idAgilityClass: Int, copies: Int = 1, pdf: Boolean = false, regenerate: Boolean = true): String {
            val outFile = nameOutfile(keyword, pdf, idAgilityClass = idAgilityClass)
            if (outFile.isEmpty() || regenerate || !File(outFile).cached()) {
                AwardsReport(idAgilityClass, outFile, copies)
            }
            return outFile
        }
    }

}
 