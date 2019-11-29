/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.library.transport

import org.egility.library.general.Json
import org.egility.library.general.JsonNode
import org.egility.library.general.doNothing
import java.io.DataOutputStream
import java.net.HttpURLConnection
import java.net.URL


/**
 * Created by mbrickman on 26/07/15.
 */

class ApiRequest(val c: HttpURLConnection) {

    fun addHeader(name: String, value: String) {
        c.setRequestProperty(name, value)
    }

}

class ApiResponse(val c: HttpURLConnection) {
    val code = c.responseCode
    val message = c.responseMessage
    val body = if (code == 200) Json(c.inputStream) else Json.nullNode()
}

object ApiClient {

    fun get(urlPath: String, prepare: ((ApiRequest) -> Unit)? = null): ApiResponse {
        val urlConnection = URL(urlPath).openConnection() as HttpURLConnection
        val request = ApiRequest(urlConnection)
        request.addHeader("User-agent", "e-gility")
        request.addHeader("Accept", "application/json")
        if (prepare != null) prepare(request)
        return ApiResponse(urlConnection)
    }

    fun put(urlPath: String, body: JsonNode, prepare: ((ApiRequest) -> Unit)? = null): ApiResponse {
        val postData = body.toJson().toByteArray()
        val urlConnection = URL(urlPath).openConnection() as HttpURLConnection
        urlConnection.requestMethod = "PUT"
        urlConnection.doOutput = true
        val request = ApiRequest(urlConnection)
        request.addHeader("User-agent", "e-gility")
        request.addHeader("Accept", "application/json")
        request.addHeader("Content-Type", "application/json")
        request.addHeader("charset", "utf-8")
        request.addHeader("Content-Length", postData.size.toString())

        if (prepare != null) prepare(request)

        try {
            val outputStream = DataOutputStream(urlConnection.outputStream)
            outputStream.write(postData)
            outputStream.flush()
        } catch (exception: Throwable) {
            doNothing()
        }
        return ApiResponse(urlConnection)
    }

}



