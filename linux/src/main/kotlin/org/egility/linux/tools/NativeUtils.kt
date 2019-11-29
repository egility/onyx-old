/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.linux.tools

import org.egility.library.dbobject.AgilityClass
import org.egility.library.dbobject.Competition
import org.egility.library.general.Global
import org.egility.library.general.debug
import org.egility.library.general.panic
import org.egility.library.general.quoted
import org.egility.linux.reports.ResultsReport
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Created by mbrickman on 06/01/15.
 */
object NativeUtils {

    fun sysLog(message: String) {
        val runtime = Runtime.getRuntime()
        try {
            runtime.exec("logger " + ("[Sandstone] " + message).quoted)
        } catch (e: Throwable) {
            panic(e)
        }

    }

    fun execute(vararg command: String, folderPath: String="", wait: Boolean): Int {
        val runtime = Runtime.getRuntime()
        for (item in command) {
            debug("TEST", "Item: $item")
        }
        val dir = if (folderPath.isNotEmpty()) File(folderPath) else null
        val process = runtime.exec(command, null, dir)
        if (wait) {
            process.waitFor()
            return process.exitValue()
        } else {
            return 0
        }
    }

    fun zipCompetitionResults(idCompetition: Int): String {
        val competition=Competition()
        if (competition.find(idCompetition)) {
            val agilityClass = AgilityClass()
            agilityClass.select("idCompetition=$idCompetition and not paperBased", "classNumber, classNumberSuffix, part")

            val zipFile = File(Global.showDocumentPath(competition.uniqueName, "results", "zip"))
            val zipStream = ZipOutputStream(FileOutputStream(zipFile))
            while (agilityClass.next()) {
                zipStream.putNextEntry(ZipEntry(agilityClass.name.replace(" *", "").replace(" #", "").replace("/", "'") +".pdf"))
                ResultsReport(agilityClass.id, outStream = zipStream, tournament = false)
                zipStream.closeEntry()
            }
            zipStream.close()
            return zipFile.canonicalPath
        }
        return ""
    }



}

