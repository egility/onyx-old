/*
 * Copyright (c) Mike Brickman 2014-2018
 */

package org.egility.linux.reports

import net.sf.dynamicreports.jasper.builder.JasperReportBuilder
import net.sf.dynamicreports.report.builder.DynamicReports.*
import net.sf.dynamicreports.report.constant.HorizontalTextAlignment
import net.sf.dynamicreports.report.constant.PageOrientation
import net.sf.dynamicreports.report.constant.PageType
import net.sf.dynamicreports.report.datasource.DRDataSource
import net.sf.jasperreports.engine.JRDataSource
import org.egility.library.dbobject.Camping
import org.egility.library.dbobject.Competition
import org.egility.library.dbobject.Entry
import org.egility.library.general.*
import org.egility.library.general.Global.showDocumentPath
import java.io.File
import java.io.OutputStream
import java.util.*


/**
 * Created by mbrickman on 30/04/18.
 */
class CampingListReport(val idCompetition: Int, outfile: String, copies: Int, outStream: OutputStream? = null) :
    Report(outfile, copies, outStream) {
    
    var showGroups = true
    var familyNameFirst = false

    val competition = Competition(idCompetition)
    val entry = Entry()


    val textStyle = stl.style()
        .setFontName("Arial")
        .setFontSize(10)
        .setBottomPadding(2)
        .setTopPadding(2)

    val boldStyle = stl.style(textStyle).bold()
    val pageHeaderStyle = stl.style(textStyle).setFontSize(12)
    val titleStyle = stl.style(textStyle).setFontSize(14).setTopPadding(5).setBottomPadding(5)
    val boxStyle = stl.style().setBorder(stl.pen1Point().setLineWidth(0.5F)).setPadding(10)
    val boxTextStyle = stl.style(textStyle)
        .setBorder(stl.pen1Point().setLineWidth(0.5F))
        .setTopPadding(2)
        .setBottomPadding(2)
        .setLeftPadding(5)
        .setRightPadding(5)
    val boxBoldStyle = stl.style(boldStyle).setBorder(stl.pen1Point().setLineWidth(0.5F)).setPadding(5)

    val template = template()
        .setPageFormat(PageType.A4, PageOrientation.PORTRAIT)
        .setPageMargin(margin().setLeft(18).setRight(18).setTop(36).setBottom(18)) //
        .setLocale(Locale.ENGLISH)
        .setTextStyle(textStyle)
        .setColumnStyle(boxTextStyle)
        .setColumnTitleStyle(boxBoldStyle)
        .setPageHeaderStyle(boldStyle)


    init {
        buildReport()
    }

    private fun buildReport(): JasperReportBuilder {
        val reportBuilder = report()
        reportBuilder.setTemplate(template)
        reportBuilder.dataSource = getEntries()

        reportBuilder
            .pageHeader(
                cmp.text("CAMPING LIST - ${competition.briefName.upperCase}")
                    .setStyle(titleStyle)
                    .setHorizontalTextAlignment(HorizontalTextAlignment.CENTER)
            )

        reportBuilder.addColumn(col.column("Name", "name", type.stringType()).setWidth(50))
        reportBuilder.addColumn(col.column("Note", "note", type.stringType()))

        var campingDate = competition.campingFirst
        while (campingDate <= competition.campingLast) {
            val fieldName = "day${campingDate.daysSince(competition.campingFirst)}"
            reportBuilder.addColumn(col.column(campingDate.dayNameShort, fieldName, type.stringType()).setWidth(14).setHorizontalTextAlignment(HorizontalTextAlignment.CENTER))
            campingDate = campingDate.addDays(1)
        }
        reportBuilder.addColumn(col.column("Mobile", "mobile", type.stringType()).setWidth(36))
        reportBuilder.addColumn(col.column("Tick", "tick", type.stringType()).setWidth(16))


        reportBuilder.generate()

        return reportBuilder

    }


    private fun getEntries(): JRDataSource {
        
        val fixedFields = 4

        val managed = competition.hasManagedCamping
        val voucherCodeMap = competition.accountVoucherCodeMap

        val campingMap = competition.campingVoucherMap

        fun getCampingVoucher(idAccount: Int): String {
            val voucherCodes = voucherCodeMap[idAccount] ?: ""
            if (voucherCodes.isNotEmpty()) {
                for (voucherCode in voucherCodes.split(",")) {
                    if (campingMap.containsKey(voucherCode)) {
                        return campingMap[voucherCode] ?: ""
                    }
                }
            }
            return ""
        }


        val campingDays = competition.campingLast.daysSince(competition.campingFirst) + 1
        val fieldNames = Array(fixedFields + campingDays) { index ->
            when (index) {
                0 -> "name"
                1 -> "note"
                2 -> "mobile"
                3 -> "tick"
                else -> "day${index-fixedFields}"
            }
        }
        val dataSource = DRDataSource(*fieldNames)

        var idList = "$idCompetition"
        if (idCompetition == 2034788955) idList = idList.append("1275361865")
        if (idCompetition == 1275361865) idList = idList.append("2034788955")
        
        if (idCompetition==1312363294) {
            familyNameFirst = true
        }

        Camping()
            .join { account }
            .join { account.competitor }
            .where("camping.idCompetition IN ($idList)", if (familyNameFirst) "familyName, givenName" else "givenName, familyName") {
                val data = Array(fixedFields + campingDays) { index ->
                    when (index) {
                        0 -> if (familyNameFirst) account.competitor.fullNameReverse else account.competitor.fullName
                        1 -> {
                            var note=""
                            if (cancelled) {
                               "CANCELLED"
                            } else {
                                val voucher = if (managed) getCampingVoucher(idAccount) else ""
                                if (voucher.isNotEmpty()) {
                                    "* $voucher"
                                } else {
                                  if (showGroups) "$groupName" else ""
                                }
                            }
                        }
                        2 -> account.competitor.phoneMobile
                        3 -> ""
                        else ->  {
                            val day = index - fixedFields
                            if (dayFlags.isBitSet(day)) "x" else ""
                        }
                    }
                }
                dataSource.add(*data)
            }
        return dataSource
    }

    companion object : ReportCompanion {

        override val keyword = Reports.CAMPING_LIST

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
                CampingListReport(idCompetition, outFile, copies)
            }
            return outFile
        }


    }


}