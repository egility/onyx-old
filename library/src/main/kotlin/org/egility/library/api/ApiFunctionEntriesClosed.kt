/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.library.api

import org.egility.library.dbobject.AgilityClass
import org.egility.library.general.Json
import org.egility.library.general.machineDate
import org.egility.library.general.mandate
import org.egility.library.transport.UdpExchange
import org.egility.library.transport.httpApiClient
import org.egility.library.transport.udpApiClient


class ApiFunctionEntriesClosed : ApiFunction {

    var isOk: Boolean = false
        private set

    fun requestHttp(version: String, idAgilityClass: Int) {
        val response = httpApiClient().getJson(version, keyword, "idAgilityClass=" + Integer.toString(idAgilityClass))
        isOk = response["OK"].asBoolean
    }

    fun requestUdp(version: String, idAgilityClass: Int) {
        val request = ApiUtils.generateUdpService(keyword, version)
        request["idAgilityClass"] = idAgilityClass
        val response = udpApiClient.getJson(request)
        isOk = response["OK"].asBoolean
    }

    override fun serveHttp(apiExchange: ApiExchange) {
        val id = apiExchange.getParameter("idAgilityClass")
        val idAgilityClass = Integer.parseInt(id)
        val response = serve(idAgilityClass)
        apiExchange.respond(response)
    }

    override fun serveUdp(udpExchange: UdpExchange) {
        val idAgilityClass = udpExchange.request["idAgilityClass"].asInt
        val response = serve(idAgilityClass)
        udpExchange.respond(response)
    }

    private fun serve(idAgilityClass: Int): Json {

        val agilityClass = AgilityClass()
        agilityClass.find(idAgilityClass)
        mandate(agilityClass.found(), "Can not find agility class ($idAgilityClass)")

        agilityClass.entriesClosed()

        val response = Json()
        response["OK"] = true
        response["kind"] = keyword
        response["timestamp"] = machineDate.time
        return response
    }


    companion object {

        val keyword = "entries_closed"
    }


}
