/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.library.api

/*
 * Copyright (c) Mike Brickman 2014-1015.
 */

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpHandler
import com.sun.net.httpserver.HttpServer
import org.egility.library.api.ApiUtils.API_PORT
import org.egility.library.api.ApiUtils.API_PORT_OLD
import org.egility.library.api.ApiUtils.BROADCAST_PORT
import org.egility.library.database.DbConnectionExecutor
import org.egility.library.general.*
import org.egility.library.transport.UdpExchange
import org.egility.library.transport.UdpListener
import org.egility.library.transport.UdpServer
import java.net.InetSocketAddress

/**
 * Created by mbrickman on 13/07/15.
 */
class ApiServer : HttpHandler, UdpListener {

    override fun handle(httpExchange: HttpExchange) {
        val apiExchange = ApiExchange(httpExchange)
        try {
            try {
                debug("API", "http: ${httpExchange.requestURI}")
                when (apiExchange.function) {
                    ApiFunctionTest.keyword -> ApiFunctionTest().serveHttp(apiExchange)
                    ApiFunctionShutdown.keyword -> ApiFunctionShutdown().serveHttp(apiExchange)
                    ApiFunctionKill.keyword -> ApiFunctionKill().serveHttp(apiExchange)
                    ApiFunctionOption.keyword -> ApiFunctionOption().serveHttp(apiExchange)
                    ApiFunctionDiagnostics.keyword -> ApiFunctionDiagnostics().serveHttp(apiExchange)
                    ApiFunctionDocuments.keyword -> ApiFunctionDocuments().serveHttp(apiExchange)
                    ApiFunctionAcuList.keyword -> ApiFunctionAcuList().serveHttp(apiExchange)
                    ApiFunctionHeartbeat.keyword -> ApiFunctionHeartbeat().serveHttp(apiExchange)
                    ApiFunctionGetMaster.keyword -> ApiFunctionGetMaster().serveHttp(apiExchange)
                    ApiFunctionPrintReport.keyword -> ApiFunctionPrintReport().serveHttp(apiExchange)
                    ApiFunctionCloseClass.keyword -> ApiFunctionCloseClass().serveHttp(apiExchange)
                    ApiFunctionEndOfDay.keyword -> ApiFunctionEndOfDay().serveHttp(apiExchange)
                    ApiFunctionEntriesClosed.keyword -> ApiFunctionEntriesClosed().serveHttp(apiExchange)
                    ApiFunctionNetworkDate.keyword -> ApiFunctionNetworkDate().serveHttp(apiExchange)
                    ApiFunctionSetRing.keyword -> ApiFunctionSetRing().serveHttp(apiExchange)
                    ApiFunctionImportAccount.keyword -> ApiFunctionImportAccount().serveHttp(apiExchange)
                    ApiFunctionLockEntries.keyword -> ApiFunctionLockEntries().serveHttp(apiExchange)
                    ApiFunctionAlert.keyword -> ApiFunctionAlert().serveHttp(apiExchange)
                    "granite" -> {
                        apiExchange.respondApk("/data/e-gility/granite.apk")
                        return
                    }
                    "pdf" -> {
                        apiExchange.respondPdf(apiExchange.getElement(2))
                        return
                    }
                    else -> ApiFunctionError("Invalid Function - " + apiExchange.function).serveHttp(apiExchange)
                }

            } catch (e: Throwable) {
                println("HttpHandler.handle FATAL ERROR (1)")
                e.printStackTrace()
                ApiFunctionError(e.message ?: "No error message", e).serveHttp(apiExchange)
            }

        } catch (e: Throwable) {
            println("HttpHandler.handle FATAL ERROR (2)")
            e.printStackTrace()
            //panic(e)
        }

    }

    override fun handleUdp(udpExchange: UdpExchange) {
        try {
            try {
                if (udpExchange.request["kind"].asString!="health_check") {
                    debug("API", "udp: ${udpExchange.request.toJson(compact = true)}")
                }
                when (udpExchange.request["kind"].asString) {
                    ApiFunctionTest.keyword -> ApiFunctionTest().serveUdp(udpExchange)
                    ApiFunctionShutdown.keyword -> ApiFunctionShutdown().serveUdp(udpExchange)
                    ApiFunctionDiagnostics.keyword -> ApiFunctionDiagnostics().serveUdp(udpExchange)
                    ApiFunctionDocuments.keyword -> ApiFunctionDocuments().serveUdp(udpExchange)
                    ApiFunctionAcuList.keyword -> ApiFunctionAcuList().serveUdp(udpExchange)
                    ApiFunctionGetMaster.keyword -> ApiFunctionGetMaster().serveUdp(udpExchange)
                    ApiFunctionPrintReport.keyword -> ApiFunctionPrintReport().serveUdp(udpExchange)
                    ApiFunctionCloseClass.keyword -> ApiFunctionCloseClass().serveUdp(udpExchange)
                    ApiFunctionEndOfDay.keyword -> ApiFunctionEndOfDay().serveUdp(udpExchange)
                    ApiFunctionEntriesClosed.keyword -> ApiFunctionEntriesClosed().serveUdp(udpExchange)
                    ApiFunctionHeartbeat.keyword -> ApiFunctionHeartbeat().serveUdp(udpExchange)
                    ApiFunctionNetworkDate.keyword -> ApiFunctionNetworkDate().serveUdp(udpExchange)
                    ApiFunctionSetRing.keyword -> ApiFunctionSetRing().serveUdp(udpExchange)
                    ApiFunctionImportAccount.keyword -> ApiFunctionImportAccount().serveUdp(udpExchange)
                    ApiFunctionLockEntries.keyword -> ApiFunctionLockEntries().serveUdp(udpExchange)
                    ApiFunctionAlert.keyword -> ApiFunctionAlert().serveUdp(udpExchange)
                }
            } catch (e: Throwable) {
                e.printStackTrace()
                ApiFunctionError(e.message ?: "No error message", e).serveUdp(udpExchange)
            }

        } catch (e: Throwable) {
            panic(e)
        }

    }

    companion object {

        fun initialize() {
            val apiServer = ApiServer()

            val httpServer = HttpServer.create(InetSocketAddress(API_PORT), 0)
            httpServer.createContext("/", apiServer)
            httpServer.executor = DbConnectionExecutor(SandstoneMaster.builder, threadPoolSize = 10)
            httpServer.start()

            if (API_PORT_OLD!=API_PORT) {
                val httpServer2 = HttpServer.create(InetSocketAddress(API_PORT_OLD), 0)
                httpServer2.createContext("/", apiServer)
                httpServer2.executor = DbConnectionExecutor(SandstoneMaster.builder, threadPoolSize = 10)
                httpServer2.start()
            }

            val udpServer = UdpServer.create(BROADCAST_PORT, apiServer)
            udpServer.start()

            HealthBroadcaster().start()
        }
    }
}
