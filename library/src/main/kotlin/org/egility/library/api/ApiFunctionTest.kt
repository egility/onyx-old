/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.library.api

/*
 * Copyright (c) Mike Brickman 2014-1015.
 */

import org.egility.library.general.Json
import org.egility.library.general.Wobbly
import org.egility.library.general.getHostname
import org.egility.library.general.machineDate
import org.egility.library.transport.UdpExchange
import org.egility.library.transport.httpApiClient
import org.egility.library.transport.udpApiClient

class ApiFunctionTest : ApiFunction {

    var greeting: String = ""
        private set

    fun requestHttp(version: String) {
        val response = httpApiClient().getJson(version, keyword, "")
        greeting = response["greeting"].asString
    }

    fun requestUdp(version: String) {
        val request = ApiUtils.generateUdpService(keyword, version)
        val response = udpApiClient.getJson(request)
        greeting = response["greeting"].asString
    }

    override fun serveHttp(apiExchange: ApiExchange) {
        val version = apiExchange.version
        val greeting = "greetings from " + getHostname()
        throw Wobbly("Just Kidding")
        val response = serve(version, greeting)
        apiExchange.respond(response)
    }

    override fun serveUdp(udpExchange: UdpExchange) {
        val version = udpExchange.request["version"].asString
        val greeting = "greetings from " + getHostname()

        val response = serve(version, greeting)
        udpExchange.respond(response)
    }

    private fun serve(version: String, greeting: String): Json {
        val response = Json()
        response["OK"] = true
        response["kind"] = keyword
        response["timestamp"] = machineDate.time
        response["greeting"] = greeting
        return response
    }

    companion object {

        val keyword = "test"
    }
}
