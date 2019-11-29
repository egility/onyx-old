/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.library.api

import org.egility.library.general.Json
import org.egility.library.general.machineDate
import org.egility.library.transport.UdpExchange
import org.egility.library.transport.httpApiClient
import org.egility.library.transport.udpApiClient


class ApiFunctionImportAccount : ApiFunction {

    var isOk: Boolean = false
        private set
    
    var error = 0

    fun requestHttp(version: String, dogCode: Int) {
        val response = httpApiClient().getJson(version, keyword, "dogCode=" + Integer.toString(dogCode))
        isOk = response["OK"].asBoolean
        error = response["errorCode"].asInt
    }

    fun requestUdp(version: String, dogCode: Int) {
        val request = ApiUtils.generateUdpService(keyword, version)
        request["dogCode"] = dogCode
        val response = udpApiClient.getJson(request)
        isOk = response["OK"].asBoolean
    }

    override fun serveHttp(apiExchange: ApiExchange) {
        val id = apiExchange.getParameter("dogCode")
        val dogCode = Integer.parseInt(id)
        val response = serve(dogCode)
        apiExchange.respond(response)
    }

    override fun serveUdp(udpExchange: UdpExchange) {
        val dogCode = udpExchange.request["dogCode"].asInt
        val response = serve(dogCode)
        udpExchange.respond(response)
    }

    private fun serve(dogCode: Int): Json {

        val error = ApiUtils.syncAccount(dogCode)
        val response = Json()
        response["OK"] = true
        if (error > 0) response["Error code: $error"]
        response["errorCode"] = error
        response["timestamp"] = machineDate.time
        return response
    }

    companion object {

        val keyword = "import_account"
    }

}
