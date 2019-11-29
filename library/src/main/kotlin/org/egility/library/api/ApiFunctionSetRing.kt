/*
 * Copyright (c) Mike Brickman 2014-2018
 */

package org.egility.library.api

import org.egility.library.general.*
import org.egility.library.general.hardware.setMasterRing
import org.egility.library.transport.UdpExchange
import org.egility.library.transport.httpApiClient
import org.egility.library.transport.udpApiClient

/**
 * Created by mbrickman on 26/07/15.
 */
class ApiFunctionSetRing : ApiFunction {

    var isOk: Boolean = false
        private set

    fun requestHttp(version: String, instance: Int, ring: String) {
        val response = httpApiClient().getJson(version, keyword, "instance=$instance&ring=${ring.quoted}")
        isOk = response["OK"].asBoolean
    }

    fun requestUdp(version: String, instance: Int, ring: String) {
        val request = ApiUtils.generateUdpService(keyword, version)
        request["instance"] = instance
        request["ring"] = ring
        val response = udpApiClient.getJson(request)
        isOk = response["OK"].asBoolean
    }


    override fun serveHttp(apiExchange: ApiExchange) {
        val instance = apiExchange.getParameter("instance").toIntDef()
        val ring = apiExchange.getParameter("ring")
        val response = serve(ring, instance)
        apiExchange.respond(response)
    }

    override fun serveUdp(udpExchange: UdpExchange) {
        val instance = udpExchange.request["instance"].asInt
        val ring = udpExchange.request["ring"].asString
        val response = serve(ring, instance)
        udpExchange.respond(response)
    }

    private fun serve(ring: String, instance: Int): Json {
        val response = Json()
        val masters=setMasterRing("remote", instance, ring, manual = true)
        response["OK"] = true
        response["kind"] = ApiFunctionSetRing.keyword
        response["ring"] = ring
        response["ip"] = hardware.ip
        response["masters"] = masters
        return response
    }

    companion object {

        val keyword = "set_ring"
    }

}
