/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.library.api

import org.egility.library.general.Global
import org.egility.library.general.Json
import org.egility.library.general.debug
import org.egility.library.general.machineDate
import org.egility.library.transport.UdpExchange
import org.egility.library.transport.httpApiClient
import org.egility.library.transport.udpApiClient

/**
 * Created by mbrickman on 26/07/15.
 */
class ApiFunctionGetMaster : ApiFunction {

    var host: String = ""
        private set

    fun requestHttp(version: String) {
        val response = httpApiClient().getJson(version, keyword, "")
        host = response["host"].asString
    }

    fun requestUdp(version: String) {
        val request = ApiUtils.generateUdpService(keyword, version)
        val response = udpApiClient.getJson(request)
        host = response["host"].asString
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
        val response = Json()
        response["OK"] = true
        response["kind"] = keyword
        response["timestamp"] = machineDate.time
        response["host"] = Global.databaseHost
        debug("getMaster", Global.databaseHost)
        return response
    }

    companion object {

        val keyword = "get_master"
    }

}
