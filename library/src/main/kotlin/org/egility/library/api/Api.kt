/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.library.api

import org.egility.library.dbobject.Competition
import org.egility.library.general.Global
import org.egility.library.general.Json
import org.egility.library.general.Param
import org.egility.library.general.nullDate
import java.util.*

/**
 * Created by mbrickman on 14/07/15.
 */
object Api {

    val version = "v1.0"

    val greeting: String
        get() {
            val test = ApiFunctionTest()
            test.requestHttp(version)
            return test.greeting
        }

    fun remoteShutdown(): String {
        val shutdown = ApiFunctionShutdown()
        shutdown.requestHttp(version)
        return shutdown.message
    }

    fun getMasterHost(broadcast: Boolean): String {
        val master = ApiFunctionGetMaster()
        if (broadcast) {
            master.requestUdp(version)
        } else {
            master.requestHttp(version)
        }
        return master.host
    }

    fun entriesClosed(idAgilityClass: Int): Boolean {
        val function = ApiFunctionEntriesClosed()
        function.requestHttp(version, idAgilityClass)
        return function.isOk
    }

    fun closeClass(idAgilityClass: Int): Boolean {
        val function = ApiFunctionCloseClass()
        function.requestHttp(version, idAgilityClass)
        return function.isOk
    }

    fun alert(type: String): Boolean {
        val function = ApiFunctionAlert()
        function.requestHttp(version, type)
        return function.isOk
    }

    fun importAccount(dogCode: Int): Int {
        val function = ApiFunctionImportAccount()
        function.requestHttp(version, dogCode)
        return function.error
    }

    fun endOfDay(idCompetition: Int, date: Date, finalize: Boolean): Boolean {
        val function = ApiFunctionEndOfDay()
        function.requestHttp(version, idCompetition, date, finalize)
        return function.isOk
    }

    fun lockEntries(idCompetition: Int): Boolean {
        val function = ApiFunctionLockEntries()
        function.requestHttp(version, idCompetition)
        Competition.current.refresh()
        return function.isOk
    }

    fun setRing(instance: Int=0, ring: String): Boolean {
        val function = ApiFunctionSetRing()
        function.requestHttp(version, instance, ring)
        return function.isOk
    }

    fun killServers(): Boolean {
        val report = ApiFunctionKill()
        report.requestHttp(version)
        return report.isOk
    }

    fun networkDate(date: Date= nullDate): Date {
        val service = ApiFunctionNetworkDate()
        service.requestHttp(version, date)
        return if (service.isOk) service.networkDate else nullDate
    }

    fun option(name: String): Boolean {
        val report = ApiFunctionOption()
        report.requestHttp(version, name)
        return report.isOk
    }

    fun printReport(reportRequest: Json, pdfFile: Param<String>? = null): Boolean {
        val report = ApiFunctionPrintReport()
        report.requestHttp(version, reportRequest)
        if (pdfFile != null) {
            pdfFile.value = report.pdfFile
        }
        return report.isOk
    }

    fun Diagnostics(hostName: String): Json {
        val host = ApiFunctionDiagnostics()
        host.requestHttp(hostName, version)
        return host.data
    }

    fun Documents(function: String="list", document: String ="", copies: Int = 0): Json {
        val host = ApiFunctionDocuments()
        host.requestHttp(function, document, copies, version)
        return host.documents
    }

    fun updateHosts(): Boolean {
        val acuList = ApiFunctionAcuList()
        if (Global.services.acuHostname.isEmpty()) {
            acuList.requestUdp(version)
            return acuList.isOk
        } else {
            acuList.requestHttp(Global.services.acuHostname, version)
            return acuList.isOk
        }
    }

    fun broadcastHealth() {
        val healthCheck = ApiFunctionHeartbeat()
        healthCheck.broadcastUdp(version)
    }

}
