/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.linux.reports

import net.sf.dynamicreports.jasper.builder.JasperReportBuilder
import net.sf.dynamicreports.report.base.expression.AbstractSimpleExpression
import net.sf.dynamicreports.report.base.expression.AbstractValueFormatter
import net.sf.dynamicreports.report.builder.DynamicReports
import net.sf.dynamicreports.report.builder.column.TextColumnBuilder
import net.sf.dynamicreports.report.builder.component.BreakBuilder
import net.sf.dynamicreports.report.builder.component.FillerBuilder
import net.sf.dynamicreports.report.builder.component.SubreportBuilder
import net.sf.dynamicreports.report.builder.component.TextFieldBuilder
import net.sf.dynamicreports.report.builder.style.ReportStyleBuilder
import net.sf.dynamicreports.report.definition.ReportParameters
import net.sf.jasperreports.engine.JRDataSource

/**
 * Created by mbrickman on 20/08/16.
 */

class MoneyFormatterBlank : AbstractValueFormatter<String, Int>() {
    override fun format(value: Int, reportParameters: ReportParameters): String {
        if (value === 0) {
            return ""
        } else {
            return "£%01.2f".format(value / 100.0)
        }
    }
}

class MoneyFormatter : AbstractValueFormatter<String, Int>() {
    override fun format(value: Int, reportParameters: ReportParameters): String {
        return "£%01.2f".format(value / 100.0)
    }
}

fun createReport(title: String, style: ReportStyleBuilder = bold12Style): JasperReportBuilder {
    var report = DynamicReports.report()
    report.setTemplate(reportTemplate)
    report.title(DynamicReports.cmp.text(title).setStyle(style))
    return report
}

fun intColumn(title: String, fieldName: String, width: Int, style: ReportStyleBuilder? = null): TextColumnBuilder<Int> {
    val column = DynamicReports.col.column(title, fieldName, DynamicReports.type.integerType())
    if (width > 0) {
        column.setFixedWidth(width * 7)
    }
    if (style != null) {
        column.setStyle(style)
    }
    return column
}

fun stringColumn(title: String, fieldName: String, width: Int, style: ReportStyleBuilder? = null): TextColumnBuilder<String> {
    val column = DynamicReports.col.column(title, fieldName, DynamicReports.type.stringType())
    if (width > 0) {
        column.setFixedWidth(width * 7)
    }
    if (style != null) {
        column.setStyle(style)
    }
    return column
}

fun moneyColumn(title: String, fieldName: String, width: Int, style: ReportStyleBuilder? = null): TextColumnBuilder<Int> {
    val column = intColumn(title, fieldName, width, style)
    column.setValueFormatter(MoneyFormatter())
    return column
}

fun moneyColumnBlank(title: String, fieldName: String, width: Int, style: ReportStyleBuilder? = null): TextColumnBuilder<Int> {
    val column = intColumn(title, fieldName, width, style)
    column.setValueFormatter(MoneyFormatterBlank())
    return column
}


fun JasperReportBuilder.addStringColumn(title: String, fieldName: String, width: Int = 0, style: ReportStyleBuilder? = null): TextColumnBuilder<String> {
    val column = stringColumn(title, fieldName, width, style)
    this.addColumn(column)
    return column
}

fun JasperReportBuilder.addIntColumn(title: String, fieldName: String, width: Int = 0, subTotal: Boolean = true, style: ReportStyleBuilder? = columnStyleRight): TextColumnBuilder<Int> {
    val column = intColumn(title, fieldName, width, style)
    this.addColumn(column)
    if (subTotal) {
        this.addSubtotalAtSummary(DynamicReports.sbt.sum(column))
    }
    return column
}

fun JasperReportBuilder.addMoneyColumn(title: String, fieldName: String, width: Int = 0, subTotal: Boolean = true, style: ReportStyleBuilder? = columnStyleRight): TextColumnBuilder<Int> {
    val column = moneyColumn(title, fieldName, width, style)
    this.addColumn(column)
    if (subTotal) {
        this.addSubtotalAtSummary(DynamicReports.sbt.sum(column).setValueFormatter(MoneyFormatter()))
    }
    return column
}

fun JasperReportBuilder.addBlankMoneyColumn(title: String, fieldName: String, width: Int = 0, subTotal: Boolean = false, style: ReportStyleBuilder? = columnStyleRight): TextColumnBuilder<Int> {
    val column = moneyColumnBlank(title, fieldName, width, style)
    this.addColumn(column)
    if (subTotal) {
        this.addSubtotalAtSummary(DynamicReports.sbt.sum(column).setValueFormatter(MoneyFormatterBlank()))
    }
    return column
}

abstract class SubReport : AbstractSimpleExpression<JasperReportBuilder>() {

    abstract fun createDataSource(): JRDataSource

    val builder: SubreportBuilder
    get() {
        val result = DynamicReports.cmp.subreport(this)
        result.setDataSource(this.createDataSource())
        return result
    }

}

fun verticalGap(gap: Int): FillerBuilder {
    return DynamicReports.cmp.verticalGap(gap)
}

fun pageBreak(): BreakBuilder {
    return DynamicReports.cmp.pageBreak()
}

fun textBuilder(text: String, style: ReportStyleBuilder?=null): TextFieldBuilder<String> {
    val builder=DynamicReports.cmp.text(text)
    if (style != null) {
        builder.setStyle(style)
    }
    return builder
}