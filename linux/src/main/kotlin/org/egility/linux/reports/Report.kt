/*
 * Copyright (c) Mike Brickman 2014-2018
 */

package org.egility.linux.reports

import org.egility.library.dbobject.AgilityClass
import org.egility.library.dbobject.Competition
import org.egility.library.general.*
import net.sf.dynamicreports.jasper.builder.JasperReportBuilder
import net.sf.jasperreports.engine.export.JRPrintServiceExporter
import net.sf.jasperreports.export.SimpleExporterInput
import net.sf.jasperreports.export.SimplePrintServiceExporterConfiguration
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import javax.print.PrintServiceLookup
import javax.print.attribute.HashPrintRequestAttributeSet
import javax.print.attribute.HashPrintServiceAttributeSet
import javax.print.attribute.standard.Copies
import javax.print.attribute.standard.MediaSizeName
import javax.print.attribute.standard.PrinterName


/**
 * Created by mbrickman on 22/02/18.
 */

interface ReportCompanion {
    val keyword: String
    fun generateJson(reportRequest: Json, pdfFile: Param<String> = Param<String>())
}


open class Report(val outfile: String, val copies: Int, val outStream: OutputStream? = null) {

    fun JasperReportBuilder.generate() {
        when {
            outStream != null -> {
                this.toPdf(outStream)
            }
            Global.isAcu -> {
                val exporter = JRPrintServiceExporter()
                exporter.setExporterInput(SimpleExporterInput(this.toJasperPrint()))

                val printServiceAttributeSet = HashPrintServiceAttributeSet()
                val printRequestAttributeSet = HashPrintRequestAttributeSet()
                val configuration = SimplePrintServiceExporterConfiguration()

                debug("cups", "default = ${hardware.defaultPrinter.cupsName}")
                if (hardware.defaultPrinter.cupsName.isNotEmpty()) {
                    printServiceAttributeSet.add(PrinterName(hardware.defaultPrinter.cupsName, null))
                }

                printRequestAttributeSet.add(MediaSizeName.ISO_A4)
                printRequestAttributeSet.add(Copies(copies))

                configuration.setDisplayPageDialog(false)
                configuration.setDisplayPrintDialog(false)
                configuration.printServiceAttributeSet = printServiceAttributeSet
                configuration.printRequestAttributeSet = printRequestAttributeSet

                exporter.setConfiguration(configuration)
                exporter.exportReport()
            }
            outfile.isEmpty() -> {
                this.show()
            }
            else -> {
                val folder = File(outfile).parent
                File(folder).mkdirs()
                val out = FileOutputStream(outfile)
                this.toPdf(out)
                out.close()
            }
        }

    }

    companion object {
        fun nameOutfile(keyword: String, pdf: Boolean, idCompetition: Int = 0, idAgilityClass: Int = 0): String {
            if (idAgilityClass > 0) {
                val a = AgilityClass()
                a.competition.joinToParent()
                a.find(idAgilityClass)
                val name = "${keyword}_${a.date.fileNameDate}_${a.name.toLowerCase().replace(" ", "_").replace("/", "_")}"
                return if (pdf || Global.alwaysToPdf) Global.showDocumentPath(a.competition.uniqueName, name, "pdf") else ""
            }
            val c = Competition(idCompetition)
            return if (pdf || Global.alwaysToPdf) Global.showDocumentPath(c.uniqueName, keyword, "pdf") else ""
        }

    }


}