/*
 * Copyright (c) Mike Brickman 2014-2018
 */

package org.egility.linux.reports

import org.egility.library.dbobject.Competition
import org.egility.library.dbobject.LedgerItem
import org.egility.library.general.*
import org.egility.library.general.Global.showDocumentPath
import net.sf.dynamicreports.report.builder.DynamicReports.*
import net.sf.dynamicreports.report.constant.HorizontalTextAlignment
import net.sf.dynamicreports.report.constant.PageOrientation
import net.sf.dynamicreports.report.constant.PageType
import net.sf.dynamicreports.report.datasource.DRDataSource
import net.sf.jasperreports.engine.JRDataSource
import java.io.File
import java.io.OutputStream
import java.util.*

open class AddressLabelsReport(val idCompetition: Int, outfile: String, copies: Int, outStream: OutputStream? = null) : Report(outfile, copies, outStream) {

    val competition = Competition(idCompetition)

    var labelWidth = if(competition.isUka || competition.isUkOpen) mm(99.1) else  mm(63.5)
    var leftPad = if(competition.isUka || competition.isUkOpen) mm(15) else  mm(5)
    var labelHeight = mm(38.1)

    val labelAcross = mm(210) / labelWidth
    val labelDown = mm(297) / labelHeight
    val horizontalMargin = (mm(210) - (labelAcross * labelWidth)) / 2
    val verticalMargin = (mm(297) - (labelDown * labelHeight)) / 2


    val textStyle = stl.style()
            .setFontName("Arial")
            .setFontSize(10)
            .setPadding(0)

    val addressStyle = stl.style(textStyle)
            .setLeftPadding(leftPad)


            .setTopPadding(mm(5))

    val codeStyle = stl.style(textStyle)
            .setFontSize(8)
            .setRightPadding(mm(5))
            .italic()
            .setHorizontalTextAlignment(HorizontalTextAlignment.RIGHT)

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
        reportBuilder.dataSource = getAccounts()
        reportBuilder.setPageColumnsPerPage(labelAcross)
                .setPageColumnSpace(mm(5))

        val address = cmp.verticalList(
                cmp.text(field("address", type.stringType()))
                        .setStyle(addressStyle)
                        .setFixedHeight(labelHeight - mm(5)),
                cmp.text(field("accountCode", type.stringType()))
                        .setStyle(codeStyle)

        )
                .setFixedHeight(labelHeight)
                //.setStyle(stl.style(stl.pen1Point()))

        reportBuilder.detail(address)
        reportBuilder.generate()

    }

    private fun getAccounts(): JRDataSource {
        val dataSource = DRDataSource("accountCode", "address")

        val ledgerItem = LedgerItem()
        ledgerItem.account.joinToParent()
        ledgerItem.account.competitor.joinToParent()
        ledgerItem.select("ledgerItem.idCompetition=$idCompetition AND LedgerItem.type IN ($LEDGER_ITEM_POSTAGE, $LEDGER_ITEM_PAPER)", "account.accountCode")

        while (ledgerItem.next()) {
            var address = ledgerItem.account.competitor.fullName
            address = address.append(ledgerItem.account.streetAddress.naturalCase, "\n")
            address = address.append(ledgerItem.account.town.toUpperCase(), "\n")
            address = address.append(ledgerItem.account.postcode.toUpperCase(), "\n")
            dataSource.add(
                    ledgerItem.account.code,
                    address
            )
        }
        return dataSource
    }


    companion object : ReportCompanion {

        override val keyword = Reports.ADDRESS_LABELS

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
                AddressLabelsReport(idCompetition, outFile, copies)
            }
            return outFile
        }

    }

}