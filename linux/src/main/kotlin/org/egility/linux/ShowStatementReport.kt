/*
 * Copyright (c) Mike Brickman 2014-2018
 */

package org.egility.linux.library_native

import org.egility.linux.reports.Report
import net.sf.dynamicreports.report.builder.DynamicReports
import net.sf.dynamicreports.report.constant.PageOrientation
import net.sf.dynamicreports.report.constant.PageType
import java.io.OutputStream
import java.util.*

/**
 * Created by mbrickman on 12/03/18.
 */
class ShowStatementReport(val idCompetition: Int, outfile: String, copies: Int, outStream: OutputStream? = null) : Report(outfile, copies, outStream) {

    val textStyle = DynamicReports.stl.style()
            .setFontName("Arial")
            .setFontSize(10)
            .setPadding(0)

    val A4 = DynamicReports.template()
            .setPageFormat(PageType.A4, PageOrientation.PORTRAIT)
            .setLocale(Locale.ENGLISH)
            .setTextStyle(textStyle)

    init {
        buildReport()
    }

    private fun buildReport() {
        val reportBuilder = DynamicReports.report()
        reportBuilder.setTemplate(A4)
    }

}