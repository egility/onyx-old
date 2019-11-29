/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.library.api

import org.egility.library.general.*
import java.util.*
import kotlin.jvm.internal.CallableReference

/**
 * Created by mbrickman on 20/09/16.
 */

class ApiPathElement(val value: String, val type: String)

class ApiRoute(val method: String, var template: String, val handler: (ApiRequest) -> Json) {

    val sort = method + '#' + template.replace(':', '~')
    val path = ArrayList<ApiPathElement>()
    val options = HashMap<String, String>()


    init {
        val parts = template.split("?")
        val elements = parts[0].split("/")

        for (element in elements) {
            var value = element
            var type = ""
            if (element[0] == ':') {
                value = element.split(":")[1]
                type = element.split(":")[2]
            }
            path.add(ApiPathElement(value, type))
        }

        if (parts.size > 1) {
            for (option in parts[1].split("&")) {
                val optionParts = option.split(":")
                options.put(optionParts[0], optionParts[1])
            }
        }
    }
}

class ApiRouter() {

    var routes = ArrayList<ApiRoute>()

    var universalOptions = HashMap<String, String>()

    fun addUniversalOptions(options: String) {
        for (option in options.split("&")) {
            val parts = option.split(":")
            universalOptions.put(parts[0], parts[1])
        }
    }

    fun sort() {
        Collections.sort(routes, Comparator<ApiRoute>() { item1, item2 ->
            val a = item1.sort
            val b = item2.sort
            a.compareTo(b)
        })
    }

    fun list() {

        for (route in routes) {
            println(route.method + ":" + route.template)
        }

    }

    fun get(template: String, handler: (ApiRequest) -> Json) {
        routes.add(ApiRoute("GET", template, handler))
    }

    fun put(template: String, handler: (ApiRequest) -> Json) {
        routes.add(ApiRoute("PUT", template, handler))
    }

    fun post(template: String, handler: (ApiRequest) -> Json) {
        routes.add(ApiRoute("POST", template, handler))
    }

    fun parseRoutes(method: String, elements: ArrayList<String>, parameters: ArrayList<String>, onFound: ((ApiRequest) -> Json, Json, Json) -> Unit): Boolean {
        for (route in routes) {
            if (parse(route, method, elements, parameters, onFound)) {
                return true
            }
        }
        return false
    }

    fun parse(route: ApiRoute, method: String, elements: ArrayList<String>, parameters: ArrayList<String>, onFound: ((ApiRequest) -> Json, Json, Json) -> Unit): Boolean {
        if (route.method != method || elements.size != route.path.size) {
            return false
        }
        var i = 0
        val params = Json()
        for (pathElement in route.path) {
            val value = pathElement.value
            val type = pathElement.type
            val match = elements[i++]
            when (type) {
                "int" -> params[value] = match.toIntDef(-1)
                "string" -> params[value] = match
                "yyyymmdd" -> params[value] = if (match.eq("today")) today else match.toDate("yyyyMMdd")
                "" -> {
                    if (!value.equals(match, ignoreCase = true)) {
                        return false
                    }
                }
            }
        }

        val query = Json()
        for (parameters in parameters) {
            val parts = parameters.split("=")
            val name = parts[0]
            val value = if (parts.size > 1) parts[1] else ""
            val type = route.options[name] ?: universalOptions[name] ?: "ignore"
            when (type) {
                "int" -> query[name] = value.toIntDef(-1)
                "string" -> query[name] = value
                "boolean" -> query[name] = value.toLowerCase().oneOf("", "true", "1", "yes")
            }
        }
        debug("ApiRouter", "method: ${route.method}, route: ${route.template}, params: ${params.toJson()}, query: ${query.toJson()}")
        debug("ApiRouter", "handler: ${(route.handler as CallableReference).name}")
        onFound(route.handler, params, query)
        return true
    }

}