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
 * Created by mbrickman on 15/07/15.
 */
class ApiFunctionAcuList : ApiFunction {

    var isOk: Boolean = false
        private set

    var acus = Json()

    fun requestHttp(hostName: String, version: String, full: Boolean=false) {
        val response = httpApiClient(hostName).getJson(version, keyword, if (full) "full" else "")
        heartbeats.processList(response)
        acus.setValue(response["acus"])
        isOk = response["OK"].asBoolean
    }

    fun requestUdp(version: String, full: Boolean=false) {
        val request = ApiUtils.generateUdpService(keyword, version)
        request["full"]=full
        try {
            val response = udpApiClient.getJson(request)
            heartbeats.processList(response)
            isOk = response["OK"].asBoolean
        } catch (e: Throwable) {
            isOk=false
        }
    }

    override fun serveHttp(apiExchange: ApiExchange) {
        val full = apiExchange.getParameter("full").isNotEmpty()
        val response = serve(full)
        apiExchange.respond(response)
    }

    override fun serveUdp(udpExchange: UdpExchange) {
        val full = udpExchange.request["full"].asBoolean
        val response = serve(full)
        udpExchange.respond(response)
    }

    private fun serve(full: Boolean): Json {
        return heartbeats.list(dropMesh = !full)
    }

    companion object {

        val keyword = "list"
    }

}
