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


class ApiFunctionOption : ApiFunction {

    var message: String = ""
        private set

    var isOk: Boolean = false
        private set


    fun requestHttp(version: String, name: String) {
        val response = httpApiClient().getJson(version, keyword, "name=${name.replace(' ', '_')}")
        isOk = response["OK"].asBoolean
        message = response["message"].asString
    }

    fun requestUdp(version: String, name: String) {
        val request = ApiUtils.generateUdpService(keyword, version)
        val response = udpApiClient.getJson(request)
        isOk = response["OK"].asBoolean
        message = response["message"].asString
    }

    override fun serveHttp(apiExchange: ApiExchange) {
        val name = apiExchange.getParameter("name").replace('_', ' ')
        val response = serve(name)
        apiExchange.respond(response)
    }

    override fun serveUdp(udpExchange: UdpExchange) {
        val name = udpExchange.request["name"].asString
        val response = serve(name)
        udpExchange.respond(response)
    }

    private fun serve(name: String): Json {
        var error = 0
        try {
            hardware.handleMenu(name)
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
        response["kind"] = ApiFunctionOption.keyword
        response["timestamp"] = machineDate.time
        response["message"] = getHostname() + " " + name
        return response
    }

    companion object {

        val keyword = "option"
    }

}
