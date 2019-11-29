/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.library.transport

import org.egility.library.api.ApiUtils.API_PORT
import org.egility.library.general.Global
import org.egility.library.general.Json
import org.egility.library.general.Wobbly
import org.egility.library.general.debug
import java.net.URL


/**
 * Created by mbrickman on 26/07/15.
 */
class httpApiClient(val hostName: String = "") : Thread() {

    private var urlPath: String = ""
    private var jsonObject: Json? = null

    fun getJson(version: String, function: String, query: String): Json {

        if (hostName.isEmpty()) {
            urlPath = "http://${Global.services.acuHostname}:${API_PORT}/${version}/${function}?${query}"
        } else {
            urlPath = "http://${hostName}:${API_PORT}/$version/$function?$query"
        }
        debug("http", urlPath)
        request(120000)
        val result: Json = jsonObject ?: throw Wobbly("Something went wrong in httpClient")
        val ok = result["OK"].asBoolean
        if (!ok) {
            val errorMessage = result["error"].toString("no error message")
            throw Wobbly("Api server error: %s", errorMessage)
        }
        return result
    }

    protected fun request(timeout: Int) {
        start()
        join()
    }

    override fun run() {
        var done = false
        while (!done) {
            try {
                val url = URL(urlPath)
                val inputStream = url.openStream()
                jsonObject = Json(inputStream)
                done = true
            } catch (e: Throwable) {
                debug("httpClient", "error: ${e.message}")
                sleep(500)
            }
        }
    }

}

