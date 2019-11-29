/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.library.api

import org.egility.library.dbobject.CompetitionDay
import org.egility.library.general.Json
import org.egility.library.general.SYSTEM_DATE_FORMAT
import org.egility.library.general.debug
import org.egility.library.general.machineDate
import org.egility.library.transport.UdpExchange
import org.egility.library.transport.httpApiClient
import org.egility.library.transport.udpApiClient
import java.text.SimpleDateFormat
import java.util.*


class ApiFunctionEndOfDay : ApiFunction {

    val simpleFormat = SimpleDateFormat(SYSTEM_DATE_FORMAT)

    var isOk: Boolean = false
        private set

    fun requestHttp(version: String, idCompetition: Int, date: Date, finalize: Boolean) {
        val response = httpApiClient().getJson(version, keyword, "idCompetition=$idCompetition&date=${simpleFormat.format(date)}&finalize=${if (finalize) "1" else "0"}")
        isOk = response["OK"].asBoolean
    }

    fun requestUdp(version: String, idCompetition: Int, date: Date, finalize: Boolean) {
        val request = ApiUtils.generateUdpService(keyword, version)
        request["idCompetition"] = idCompetition
        request["date"] = date
        request["finalize"] = finalize
        val response = udpApiClient.getJson(request)
        isOk = response["OK"].asBoolean
    }

    override fun serveHttp(apiExchange: ApiExchange) {
        val idCompetition = apiExchange.getParameter("idCompetition").toInt()
        val date = simpleFormat.parse(apiExchange.getParameter("date"))
        val finalize = apiExchange.getParameter("finalize") == "1"

        val response = serve(idCompetition, date, finalize)
        apiExchange.respond(response)
    }

    override fun serveUdp(udpExchange: UdpExchange) {
        val idCompetition = udpExchange.request["idCompetition"].asInt
        val date = udpExchange.request["date"].asDate
        val finalize = udpExchange.request["finalize"].asBoolean
        val response = serve(idCompetition, date, finalize)
        udpExchange.respond(response)
    }

    private fun serve(idCompetition: Int, date: Date, finalize: Boolean): Json {
        debug("ApiFunctionEndOfDay", "idCompetition=$idCompetition, date=${simpleFormat.format(date)}, finalize=${finalize.toString()}")
        val day = CompetitionDay()
        day.seek(idCompetition, date)
        day.print(finalize)

        val response = Json()
        response["OK"] = true
        response["kind"] = keyword
        response["timestamp"] = machineDate.time
        return response
    }


    companion object {

        val keyword = "end_of_day"
    }

}
