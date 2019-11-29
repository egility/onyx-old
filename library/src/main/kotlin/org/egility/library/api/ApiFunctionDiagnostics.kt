/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.library.api

import org.egility.library.general.Json
import org.egility.library.general.diagnostics
import org.egility.library.transport.UdpExchange
import org.egility.library.transport.httpApiClient
import org.egility.library.transport.udpApiClient

/**
 * Created by mbrickman on 15/07/15.
 */
class ApiFunctionDiagnostics : ApiFunction {

    var isOk: Boolean = false
        private set

    var data = Json()

    fun requestHttp(hostName: String, version: String) {
        val response = httpApiClient(hostName).getJson(version, keyword, "")
        isOk = response["OK"].asBoolean
        data.setValue(response["data"])
    }

    fun requestUdp(version: String) {
        val request = ApiUtils.generateUdpService(keyword, version)
        try {
            val response = udpApiClient.getJson(request)
            isOk = response["OK"].asBoolean
            data.setValue(response["data"])
        } catch (e: Throwable) {
            data.clear()
            isOk=false
        }
    }

    override fun serveHttp(apiExchange: ApiExchange) {
        val full = apiExchange.getParameter("full").isNotEmpty()
        val response = serve(full)
        apiExchange.respond(response)
    }

    override fun serveUdp(udpExchange: UdpExchange) {
        val response = serve()
        udpExchange.respond(response)
    }

    private fun serve(full: Boolean = false): Json {
        return diagnostics.generateJson(keyword, full)
    }

    companion object {

        val keyword = "diagnostics"
    }

}
