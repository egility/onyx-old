/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.library.api

import org.egility.library.general.*
import org.egility.library.transport.UdpExchange
import org.egility.library.transport.httpApiClient
import org.egility.library.transport.udpApiClient
import java.util.*

/**
 * Created by mbrickman on 21/07/17.
 */
class ApiFunctionNetworkDate  : ApiFunction {
    var networkDate: Date = nullDate
        private set

    var isOk: Boolean = false
        private set


    fun requestHttp(version: String, date: Date= nullDate) {
        val response =
            if (date.isEmpty())
                httpApiClient().getJson(version, keyword, "")
            else
                httpApiClient().getJson(version, keyword, "timestamp=${date.time}")
        isOk = response["OK"].asBoolean
        networkDate = Date(response["timestamp"].asLong)
    }

    fun requestUdp(version: String, date: Date= nullDate) {
        val request = ApiUtils.generateUdpService(keyword, version)
        request["timestamp"] = date.time
        val response = udpApiClient.getJson(request)
        isOk = response["OK"].asBoolean
        networkDate = Date(response["timestamp"].asLong)
    }

    override fun serveHttp(apiExchange: ApiExchange) {
        val timestamp = apiExchange.getParameter("timestamp")
        val date = if (timestamp.isNotEmpty()) Date(java.lang.Long.parseLong(timestamp)) else nullDate
        val response = serve(date)
        apiExchange.respond(response)
    }

    override fun serveUdp(udpExchange: UdpExchange) {
        val timestamp = udpExchange.request["timestamp"].asLong
        val date = if (timestamp>0L) Date(timestamp) else nullDate
        val response = serve(date)
        udpExchange.respond(response)
    }

    private fun serve(date: Date): Json {
        if (!date.isEmpty()) {
            hardware.setSystemTime(date.time, TIME_MANUAL)
        }
        val response = Json()
        response["OK"] = true
        response["kind"] = ApiFunctionNetworkDate.keyword
        response["timestamp"] = machineDate.time
        response["time"] = machineDate.fullDateTimeText
        return response
    }

    companion object {

        val keyword = "network_date"
    }

}