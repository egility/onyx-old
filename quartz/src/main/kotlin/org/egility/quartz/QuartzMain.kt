/*
 * Copyright (c) Mike Brickman 2014-2017
 */

/*
 * Copyright (c) Mike Brickman 2014-2016
 */

/*
 * Copyright (c) Mike Brickman 2014-2016
 */

package org.egility.quartz

import org.egility.library.dbobject.Competition
import org.egility.library.general.*
import org.egility.linux.tools.NativeServices
import java.io.File
import java.io.FileInputStream

/**
 * Created by mbrickman on 13/07/15.
 */

fun main(args: Array<String>) {

    println("Quartz 1.0")

    setDebugExcludeClasses("API")
    val switches = argsToMap(args)

    var configFile = if (switches.containsKey("config")) switches["config"] else "/data/kotlin/config.json"
    var siteName = if (switches.containsKey("site")) switches["site"] else "quartz-test"

    var mode = "Default"

    for (arg in args) {
        val parts = arg.split("=")
        val switch = parts[0]
        val value = if (parts.size > 1) parts[1] else ""


        when (switch) {
            "--config" -> {
                configFile = value
            }
            "--site" -> {
                siteName = value
            }
            "--live" -> {
                Global.live = true
                mode = "Live"
            }
            "--test" -> {
                Global.quartzTest = true
                mode = "Test"
            }
            "--allEmailsTo" -> {
                Global.allEmailsTo = value
            }
        }
    }

    var inputStream = FileInputStream(configFile)
    var config = Json(File(configFile))
    var data = Json.nullNode()



    for (site in config["sites"]) {
        if (site["name"].asString == siteName) {
            data = site
        }
    }

    if (!data.isNull) {
        Global.databaseHost = data["dbHost"].asString
        Global.databasePort = data["dbPort"].asInt
        if (data.has("images")) {
            Global.imagesFolder = data["images"].asString
        }

        val ip = data["ip"].asString
        val port = data["port"].asInt

        if (data.has("hosts")) {
            val elements = data["hosts"].asString.split(",")
            QuartzApiServer.apiHostname = elements[0]
        } else {
            QuartzApiServer.apiHostname = "$ip:$port"
        }


        NativeServices.initialize(true)

        Thread.setDefaultUncaughtExceptionHandler(UncaughtHandler())

        println("quartz.api server listening on $ip:$port (${QuartzApiServer.apiHostname}), $mode Mode")

        if (Global.databaseHost == "acu240.local") {
            Global.connection.updateNetworkTime()
            if (Competition.current.effectiveDate != realToday) {
                effectiveDate = Competition.current.effectiveDate
            } else {
                effectiveDate = nullDate
            }

        }


        QuartzApiServer.initialize(ip, port)


    }

}

private class UncaughtHandler() : Thread.UncaughtExceptionHandler {

    override fun uncaughtException(thread: Thread, e: Throwable) {
        debug("Quartz API Server", "Uncaught Exception")
        panic(e)
    }

}

