/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.library.api

/*
 * Copyright (c) Mike Brickman 2014-1015.
 */

import org.egility.library.general.*
import org.egility.library.transport.UdpExchange

/**
 * Created by mbrickman on 14/07/15.
 */
class ApiFunctionError(private val message: String, val exception: Throwable? = null, val quartz: Boolean = false) : ApiFunction {

    fun requestHttp(version: String) {
    }

    fun requestUdp(version: String) {
    }

    override fun serveHttp(apiExchange: ApiExchange) {
        val response = serve(message)
        apiExchange.respond(response)
    }

    override fun serveUdp(udpExchange: UdpExchange) {
        val response = serve(message)
        udpExchange.respond(response)
    }

    private fun serve(message: String): Json {

        val result = Json()
        result["OK"] = true
        result["kind"] = keyword
        result["version"] = "v1.0"
        result["error"] = message

        if (exception != null) {
            val stackList=exception.stack.split("\n")
            debug("ApiError", "$keyword: Unexpected error\n${exception.stack}")
            for (item in stackList) {
                result["stack"].addElement().setValue(item.replace("\t", "    "))
            }
            if (Global.isAcu) {
                hardware.logError(exception)
            }
        } else {
            debug("ApiError", "$keyword: $message")
        }
        return result
    }

    companion object {
        val keyword = "error"
    }

}
