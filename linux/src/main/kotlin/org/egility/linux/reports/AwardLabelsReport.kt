/*
 * Copyright (c) Mike Brickman 2014-2018
 */

package org.egility.linux.reports

import net.sf.dynamicreports.report.builder.DynamicReports.*
import net.sf.dynamicreports.report.constant.PageOrientation
import net.sf.dynamicreports.report.constant.PageType
import net.sf.dynamicreports.report.constant.VerticalTextAlignment
import net.sf.dynamicreports.report.datasource.DRDataSource
import net.sf.jasperreports.engine.JRDataSource
import org.egility.library.dbobject.AgilityClass
import org.egility.library.dbobject.Competition
import org.egility.library.general.*
import org.egility.library.general.Global.showDocumentPath
import java.awt.Color
import java.io.File
import java.io.OutputStream
import java.util.*

open class AwardLabelsReport(val idCompetition: Int, outfile: String, copies: Int, outStream: OutputStream? = null) :
    Report(outfile, copies, outStream) {

    val competition = Competition(idCompetition)

    var labelWidth = if (false || competition.isUka || competition.isUkOpen) mm(99.1) else mm(63.5)
    var leftPad = if (false || competition.isUka || competition.isUkOpen) mm(15) else mm(5)
    var labelHeight = mm(38.1)

    val labelAcross = mm(210) / labelWidth
    val labelDown = mm(297) / labelHeight
    val horizontalMargin = (mm(210) - (labelAcross * labelWidth)) / 2
    val verticalMargin = (mm(297) - (labelDown * labelHeight)) / 2


    val textStyle = stl.style()
        .setFontName("Arial")
        .setFontSize(12)
        .setLeftPadding(leftPad)
    
    val boldStyle = stl.style(textStyle)
        .bold()
        .underline()
        
    val smallStyle = stl.style(textStyle)
        .setFontSize(10)

    val labelSheet = template()
        .setPageFormat(PageType.A4, PageOrientation.PORTRAIT)
        .setPageMargin(margin().setLeft(horizontalMargin).setRight(horizontalMargin).setTop(verticalMargin).setBottom(mm(0))) //
        .setLocale(Locale.ENGLISH)
        .setTextStyle(textStyle)

    init {
        buildReport()
    }

    private fun buildReport() {
        val reportBuilder = report()
        reportBuilder.setTemplate(labelSheet)
        reportBuilder.dataSource = getLabels()
        reportBuilder.setPageColumnsPerPage(labelAcross)
            .setPageColumnSpace(mm(5))

        val address =
            cmp.verticalList(
                cmp.text(field("line1", type.stringType())).setStyle(boldStyle).setFixedHeight(mm(7)),
                cmp.text(field("line2", type.stringType())).setStyle(smallStyle).setFixedHeight(mm(7)),
                cmp.verticalList(
                    cmp.verticalGap(mm(7)),
                        cmp.text(field("line3", type.stringType())))
                ).setFixedHeight(labelHeight - mm(14)


            )
                .setFixedHeight(labelHeight)

        reportBuilder.detail(address)
        reportBuilder.generate()

    }
    
    private fun getLabels(): JRDataSource {
        val dataSource = DRDataSource("line1", "line2", "line3")

        val rosetteRule = AwardRule(competition.rosetteRule)
        val trophyRule = AwardRule(competition.trophyRule)


        AgilityClass().where("idCompetition=$idCompetition", "classDate, ringNumber, ringOrder") {
            val line1=abbreviatedName
            val line2="${date.dayNameShort} Ring $ringNumber (${ringOrder.ordinal()} class)"
            
            if (subClassCount>1) {
                dataSource.add(
                    line1,
                    line2,
                    "$subClassCount Categories..."
                )
            }

            for (subClass in 0..subClassCount - 1) {
                val places = getSubClassRosettes(subClass)
                val trophies = getSubClassTrophies(subClass)




                dataSource.add(
                    line1,
                    line2,
                    if (subClassCount > 1) {
                        "${subClassDescription(subClass, shortGrade = true)}: P$places, T$trophies"
                    } else {
                        "P$places, T$trophies"
                    }
                )
            }
        }
        return dataSource
    }


    companion object : ReportCompanion {

        override val keyword = Reports.AWARDS_LABELS

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
                AwardLabelsReport(idCompetition, outFile, copies)
            }
            return outFile
        }

    }

}