/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.library.api

import org.egility.library.general.*
import org.egility.library.transport.UdpExchange
import org.egility.library.transport.httpApiClient
import org.egility.library.transport.udpApiClient

/**
 * Created by mbrickman on 24/08/15.
 */
class ApiFunctionPrintReport : ApiFunction {

    var isOk: Boolean = false
    var pdfFile=""
        private set

    fun requestHttp(version: String, reportRequest: Json) {
        val query = StringBuilder("")
        for (item in reportRequest) {
            query.delimiterAppend("${item.name}=${item.asString.unQuoted}", "&")
        }
        val response = httpApiClient().getJson(version, keyword, query.toString())
        isOk = response["OK"].asBoolean
        pdfFile = response["pdfFile"].asString
    }

    fun requestUdp(version: String, reportRequest: Json) {
        val request = ApiUtils.generateUdpService(keyword, version)
        for (item in reportRequest) {
            request[item.name] = item
        }
        val response = udpApiClient.getJson(request)
        isOk = response["OK"].asBoolean
        pdfFile = response["pdfFile"].asString
    }

    override fun serveHttp(apiExchange: ApiExchange) {
        val response = serve(apiExchange.getJson())
        apiExchange.respond(response)
    }

    override fun serveUdp(udpExchange: UdpExchange) {
        val response = serve(udpExchange.request)
        udpExchange.respond(response)
    }

    private fun serve(reportRequest: Json): Json {

        val pdfFile=Global.services.generateReport(reportRequest)

        val response = Json()
        response["OK"] = true
        response["kind"] = keyword
        response["timestamp"] = machineDate.time
        response["pdfFile"] = pdfFile
        return response
    }

    companion object {

        val keyword = "report"
    }

}

