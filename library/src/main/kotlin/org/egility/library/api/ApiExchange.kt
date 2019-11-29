/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.library.api

/*
 * Copyright (c) Mike Brickman 2014-1015.
 */

import com.sun.net.httpserver.HttpExchange
import org.egility.library.general.Json
import org.egility.library.general.debug
import org.egility.library.general.panic
import java.io.File
import java.io.FileInputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.*

data class ApiRequest(val body: Json, val params: Json, val query: Json, val remoteAddress: String)


class ApiExchange(private val httpExchange: HttpExchange, val quartz: Boolean = false) {
    val method: String
    var elements = ArrayList<String>()
    var parameters = ArrayList<String>()

    init {
/*
        val debug=Json.nullNode()
        debug["protocol"]=httpExchange.protocol
        debug["method"]=httpExchange.requestMethod
        debug["uri"]=httpExchange.requestURI.toString()
        httpExchange.requestHeaders.forEach{
            val node =debug["headers"].addElement()
            node[it.key]=it.value[0]
        }
        println(debug.toJson(pretty = true))


 */
        method = httpExchange.requestMethod

        val uri = httpExchange.requestURI
        val uriPath = uri.path
        var _uriQuery = uri.query

        if (uriPath.length > 1) {
            elements.addAll(uriPath.substring(1).split("/".toRegex()).dropLastWhile { it.isEmpty() })
        }
        if (_uriQuery != null) {
            if (_uriQuery.startsWith('?')) {
                _uriQuery = _uriQuery.drop(1)
            }
            if (_uriQuery.length > 1) {
                parameters.addAll(_uriQuery.split("&"))
            }
        }
    }


    fun convertStreamToString(`is`: java.io.InputStream): String {
        val s = java.util.Scanner(`is`).useDelimiter("\\A")
        return if (s.hasNext()) s.next() else ""
    }

    fun remoteHost(httpExchange: HttpExchange): String {
        val headers = httpExchange.requestHeaders
        if (headers != null && headers.containsKey("X-forwarded-for")) {
            var value = headers["X-forwarded-for"]?.first() ?: ""
            if (value.isNotEmpty()) {
                return value.substringAfterLast(":")
            }
        }
        return httpExchange.remoteAddress.hostString.toString()
    }

    fun respondUsingRouter(router: ApiRouter) {
        try {
            try {
                val remoteHost = remoteHost(httpExchange)
                debug("ApiExchange", "http: $remoteHost ($method ${httpExchange.requestURI})")
                if (false) {
                    for (header in httpExchange.requestHeaders) {
                        debug("ApiExchange", "header: ${header.key}=${header.value}")
                    }
                }
                if (method == "OPTIONS") {
                    respondOptions()
                } else {
                    val success = router.parseRoutes(method, elements, parameters) { handler, params, query ->
                        var body = Json()
                        val headers = httpExchange.requestHeaders
                        val contentType = if (headers != null && headers.containsKey("Content-type")) headers["Content-type"]?.first()
                            ?: "json" else "json"
                        when (contentType) {
                            "application/xls" -> {
                                val file = File.createTempFile("classes", ".xls", File("/data/e-gility"))
                                Files.copy(httpExchange.requestBody, file.toPath(), StandardCopyOption.REPLACE_EXISTING)
                                body["path"] = file.canonicalPath
                            }
                            else -> {
                                val text = convertStreamToString(httpExchange.requestBody)
                                body = Json(text)
                            }
                        }
                        debug("ApiExchange", "request body: ${body.toJson(pretty = true)}")
                        val responseObject = handler(ApiRequest(body, params, query, remoteHost))
                        responseObject.isReadOnly = true
                        if (responseObject["content"].asString.isNotEmpty() && responseObject["path"].asString.isNotEmpty()) {
                            respondPath(responseObject["path"].asString, responseObject["content"].asString)
                        } else {
                            respondJson(responseObject)
                        }
                    }
                    if (!success) {
                        val error = Json()
                        error["message"] = "Unrecognised request"
                        respondJson(error)
                    }
                }
            } catch (e: Throwable) {
                e.printStackTrace()
                ApiFunctionError(e.message ?: "No error message", e, quartz).serveHttp(this)
            }

        } catch (e: Throwable) {
            panic(e)
        }
    }


    fun respond(response: Json) {
        var responseString = response.toJson(getParameter("pretty").isNotEmpty(), true)
        httpExchange.sendResponseHeaders(200, responseString.length.toLong())
        //var origin = httpExchange.requestHeaders.getFirst("Origin")
        httpExchange.responseHeaders.add("Access-Control-Allow-Origin", "*")
        val fHeaders = httpExchange.responseHeaders
        fHeaders.add("Content-Type", "application/json")
        val stream = httpExchange.responseBody
        stream.write(responseString.toByteArray())
        stream.close()
    }

    fun respondJson(response: Json) {
        //val pretty = !getParameter("pretty").isEmpty()
        httpExchange.responseHeaders.add("Content-Type", "application/json")
        httpExchange.responseHeaders.add("Access-Control-Allow-Origin", "*")
        httpExchange.sendResponseHeaders(200, 0)
        response.save(httpExchange.responseBody, pretty = true, compact = false)
        debug("API", "response: \n${response.toJson(pretty = true)}")
    }

    fun respondOptions() {
        //val pretty = !getParameter("pretty").isEmpty()

        //var origin = httpExchange.requestHeaders.getFirst("Origin")
        //var method = httpExchange.requestHeaders.getFirst("Access-Control-Request-Method")
        //var headers = httpExchange.requestHeaders.getFirst("Access-Control-Request-Headers")

        httpExchange.responseHeaders.add("Access-Control-Allow-Origin", "*")
        httpExchange.responseHeaders.add("Access-Control-Allow-Methods", "GET, PUT")
        httpExchange.responseHeaders.add("Access-Control-Allow-Headers", "Content-Type")
        httpExchange.responseHeaders.add("Access-Control-Max-Age", "3600")

        httpExchange.sendResponseHeaders(200, 0)
        val stream = httpExchange.responseBody
        stream.close()
    }

    fun respondApk(path: String) {
        val buf = ByteArray(8192)
        val file = File(path)

        val fHeaders = httpExchange.responseHeaders
        fHeaders.add("Content-Type", "application/vnd.android.package-archive")
        fHeaders.add("Content-disposition", "filename=\"granite.apk\"")
        httpExchange.sendResponseHeaders(200, file.length())

        val `in` = FileInputStream(file)
        val out = httpExchange.responseBody
        var bytes = `in`.read(buf, 0, buf.size)
        while (bytes > 0) {
            out.write(buf, 0, bytes)
            out.flush()
            bytes = `in`.read(buf, 0, buf.size)
        }
        out.close()
    }

    fun respondFile(path: String) {
        val buf = ByteArray(8192)
        val file = File(path)
        val fileName = path.substringAfterLast("/")

        val fHeaders = httpExchange.responseHeaders
        fHeaders.add("Content-Type", "application/vnd.android.package-archive")
        fHeaders.add("Content-disposition", "filename=\"$fileName\"")
        httpExchange.sendResponseHeaders(200, file.length())

        val `in` = FileInputStream(file)
        val out = httpExchange.responseBody
        var bytes = `in`.read(buf, 0, buf.size)
        while (bytes > 0) {
            out.write(buf, 0, bytes)
            out.flush()
            bytes = `in`.read(buf, 0, buf.size)
        }
        out.close()
    }

    fun respondPdf(filename: String) {
        respondPath(filename, "application/pdf")
    }

    fun respondPath(path: String, contentType: String) {
        val buf = ByteArray(8192)
        val file = File(path)

        val fHeaders = httpExchange.responseHeaders
        fHeaders.set("Content-Type", contentType)
        httpExchange.sendResponseHeaders(200, file.length())

        val `in` = FileInputStream(file)
        val out = httpExchange.responseBody
        var bytes = `in`.read(buf, 0, buf.size)
        while (bytes > 0) {
            out.write(buf, 0, bytes)
            out.flush()
            bytes = `in`.read(buf, 0, buf.size)
        }
        out.close()
    }

    fun getParameter(name: String): String {
        for (parameter in parameters) {
            if (parameter.contains("=")) {
                val parts = parameter.split("=".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                if (parts[0].equals(name, ignoreCase = true)) {
                    return parts[1].trim { it <= ' ' }
                }
            } else if (parameter.equals(name, ignoreCase = true)) {
                return "true"
            }
        }
        return ""
    }

    fun getJson(): Json {
        val result = Json()
        for (parameter in parameters) {
            if (parameter.contains("=")) {
                val parts = parameter.split("=")
                val key = parts[0]
                val value = parts[1].trim()

                try {
                    val int = Integer.parseInt(value)
                    result[key] = int
                } catch (e: Throwable) {
                    when (value) {
                        "true" -> result[key] = true
                        "false" -> result[key] = false
                        else -> result[key] = value
                    }
                }
            } else {
                result[parameter] = true
            }
        }
        return result
    }

    fun getElement(index: Int): String {
        if (index >= 0 && index < elements.size) {
            return elements[index]
        }
        return ""
    }

    fun getElementCount(): Int {
        return elements.size
    }

    val version: String
        get() = getElement(0)

    val function: String
        get() = getElement(1).toLowerCase()
}
