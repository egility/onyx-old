/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.library.api

import org.egility.library.general.hardware
import org.egility.library.general.Json
import org.egility.library.general.getHostname
import org.egility.library.general.machineDate
import org.egility.library.transport.UdpExchange
import org.egility.library.transport.httpApiClient
import org.egility.library.transport.udpApiClient

class ApiFunctionShutdown : ApiFunction {

    var message: String = ""
        private set

    fun requestHttp(version: String) {
        val response = httpApiClient().getJson(version, keyword, "")
        message = response["message"].asString
    }

    fun requestUdp(version: String) {
        val request = ApiUtils.generateUdpService(keyword, version)
        val response = udpApiClient.getJson(request)
        message = response["message"].asString
    }

    override fun serveHttp(apiExchange: ApiExchange) {
        val response = serve()
        apiExchange.respond(response)
    }

    override fun serveUdp(udpExchange: UdpExchange) {
        val response = serve()
        udpExchange.respond(response)
    }

    private fun serve(): Json {
        var error = 0
        try {
            error = hardware.shutDown(1)
        } catch (e: Throwable) {
            val response = Json()
            response["OK"] = true
            response["kind"] = "fatal"
            response["message"] = e.message ?: ""
            return response
        }

        if (error != 0) {
            val response = Json()
            response["OK"] = true
            response["kind"] = "fatal"
            response["message"] = "error code: " + Integer.toString(error)
            return response
        }

        val response = Json()
        response["OK"] = true
        response["kind"] = ApiFunctionEntriesClosed.keyword
        response["timestamp"] = machineDate.time
        response["message"] = getHostname() + " going down"
        return response
    }

    companion object {

        val keyword = "shutdown"
    }

}
