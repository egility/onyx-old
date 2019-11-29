/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.library.api

import org.egility.library.general.*
import org.egility.library.transport.UdpExchange
import org.egility.library.transport.httpApiClient
import org.egility.library.transport.udpApiClient
import java.io.File

/**
 * Created by mbrickman on 15/07/15.
 */
class ApiFunctionDocuments : ApiFunction {

    val FUNCTION_LIST = "list"
    val FUNCTION_PRINT = "print"

    var isOk: Boolean = false
        private set

    var documents = Json()

    fun requestHttp(function: String, document: String, copies: Int, version: String) {
        var query = function
        if (document.isNotEmpty()) {
            query = query.append("document=$document", "&")
        }
        if (copies > 0) {
            query = query.append("copies=$copies", "&")
        }
        val response = httpApiClient().getJson(version, keyword, query.toString())
        isOk = response["OK"].asBoolean
        documents.setValue(response["documents"])
    }

    fun requestUdp(function: String, document: String, copies: Int, version: String) {
        val request = ApiUtils.generateUdpService(keyword, version)
        request["function"] = function
        request["document"] = document
        request["copies"] = copies
        try {
            val response = udpApiClient.getJson(request)
            isOk = response["OK"].asBoolean
            documents.setValue(response["documents"])
        } catch (e: Throwable) {
            documents.clear()
            isOk = false
        }
    }

    override fun serveHttp(apiExchange: ApiExchange) {
        val response = serve(apiExchange.getJson())
        apiExchange.respond(response)
    }

    override fun serveUdp(udpExchange: UdpExchange) {
        val response = serve(udpExchange.request)
        udpExchange.respond(response)
    }

    private fun serve(request: Json): Json {
        val response = Json()
        response["OK"] = true
        response["kind"] = keyword
        when (request["function"].asString) {
            FUNCTION_LIST -> {
                val find = execStr("find /data/pdf", silent = true)
                if (find.startsWith("Error")) {
                    response["OK"] = false
                    response["error"] = find
                } else {
                    val files = find.split("\n")
                    for (file in files) {
                        if (file.isNotEmpty() && file.endsWith(".pdf")) {
                            val name = file.drop(10).dropLast(4)
                            response["document"].addElement().setValue(name)
                        }
                    }
                }
            }
            FUNCTION_PRINT -> {
                val path = "/data/pdf/${request["function"].asString}.pdf"
                val copies = request["copies"].asInt
                if (File(path).exists()) {
                    val command = if(copies>1) "lp -o media=a4 -n$copies" else "lp -o media=a4"
                    val lp = execStr("$command ${path.quoted}")
                    if (lp.startsWith("Error")) {
                        response["OK"] = false
                        response["error"] = lp
                    } else {
                        response["comment"] = lp
                    }
                } else {
                    response["OK"] = false
                    response["error"] = "File $path not found"
                }
            }
        }
        return response
    }

    companion object {

        val keyword = "documents"
    }

}
