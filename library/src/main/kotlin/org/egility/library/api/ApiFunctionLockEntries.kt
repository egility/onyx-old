/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.library.api

import org.egility.library.dbobject.CompetitionDay
import org.egility.library.general.*
import org.egility.library.transport.UdpExchange
import org.egility.library.transport.httpApiClient
import org.egility.library.transport.udpApiClient
import java.text.SimpleDateFormat
import java.util.*


class ApiFunctionLockEntries : ApiFunction {

    var isOk: Boolean = false
        private set

    fun requestHttp(version: String, idCompetition: Int) {
        val response = httpApiClient().getJson(version, keyword, "idCompetition=$idCompetition")
        isOk = response["OK"].asBoolean
    }

    fun requestUdp(version: String, idCompetition: Int) {
        val request = ApiUtils.generateUdpService(keyword, version)
        request["idCompetition"] = idCompetition
        val response = udpApiClient.getJson(request)
        isOk = response["OK"].asBoolean
    }

    override fun serveHttp(apiExchange: ApiExchange) {
        val idCompetition = apiExchange.getParameter("idCompetition").toInt()
        val response = serve(idCompetition)
        apiExchange.respond(response)
    }

    override fun serveUdp(udpExchange: UdpExchange) {
        val idCompetition = udpExchange.request["idCompetition"].asInt
        val response = serve(idCompetition)
        udpExchange.respond(response)
    }

    private fun serve(idCompetition: Int): Json {
        debug("ApiFunctionLockEntries", "idCompetition=$idCompetition")
        UkOpenUtils.lockEntries(idCompetition)
        val response = Json()
        response["OK"] = true
        response["kind"] = keyword
        response["timestamp"] = machineDate.time
        return response
    }


    companion object {

        val keyword = "lock_entries"
    }

}
