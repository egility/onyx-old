/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.library.api

import org.egility.library.general.Json
import org.egility.library.general.heartbeats
import org.egility.library.transport.UdpExchange
import org.egility.library.transport.httpApiClient
import org.egility.library.transport.udpApiClient

/**
 * Created by mbrickman on 06/11/15.
 */
class ApiFunctionHeartbeat : ApiFunction {

    var isOk: Boolean = false
        private set

    fun requestHttp(version: String) {
        val response = httpApiClient().getJson(version, keyword, "")
        heartbeats.processBeat(response)
        isOk = response["OK"].asBoolean
    }

    fun requestUdp(version: String) {
        val request = ApiUtils.generateUdpService(keyword, version)
        try {
            val response = udpApiClient.getJson(request)
            heartbeats.processBeat(response)
            isOk = response["OK"].asBoolean
        } catch (e: Throwable) {
            isOk = false
        }
    }

    fun broadcastUdp(version: String) {
        val request = heartbeats.myBeat()
        udpApiClient.sendJson(request)
    }

    override fun serveHttp(apiExchange: ApiExchange) {
        val response = serve()
        apiExchange.respond(response)
    }

    override fun serveUdp(udpExchange: UdpExchange) {
        val jsonObject = udpExchange.request
        heartbeats.processBeat(jsonObject)
        udpExchange.respond(null)
    }

    private fun serve(): Json {
        return heartbeats.myBeat()
    }

    companion object {

        val keyword = "beat"
    }


}
