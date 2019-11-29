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


class ApiFunctionAlert : ApiFunction {

    var message: String = ""
        private set

    var isOk: Boolean = false
        private set


    fun requestHttp(version: String, type: String) {
        val response = httpApiClient().getJson(version, keyword, "type=$type")
        isOk = response["OK"].asBoolean
        message = response["message"].asString
    }

    fun requestUdp(version: String, type: String) {
        val request = ApiUtils.generateUdpService(keyword, version)
        request["type"] = type
        val response = udpApiClient.getJson(request)
        isOk = response["OK"].asBoolean
        message = response["message"].asString
    }

    override fun serveHttp(apiExchange: ApiExchange) {
        val type = apiExchange.getParameter("type")
        val response = serve(type)
        apiExchange.respond(response)
    }

    override fun serveUdp(udpExchange: UdpExchange) {
        val type = udpExchange.request["type"].asString
        val response = serve(type)
        udpExchange.respond(response)
    }

    private fun serve(type: String): Json {
        var error = 0
        try {
            hardware.alert(type)
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
        response["kind"] = ApiFunctionAlert.keyword
        response["timestamp"] = machineDate.time
        response["message"] = getHostname() + " alerting"
        return response
    }

    companion object {

        val keyword = "alert"
    }

}
