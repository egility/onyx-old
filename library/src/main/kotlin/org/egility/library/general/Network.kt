/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.library.general

/**
 * Created by mbrickman on 21/10/15.
 */
import java.io.BufferedReader
import java.io.InputStreamReader

object Network {


    var pingError: String? = null

    fun pingHost(host: String): Int {
        val runtime = Runtime.getRuntime()
        val proc = runtime.exec("ping -c 1 " + host)
        proc.waitFor()
        val exit = proc.exitValue()
        return exit
    }

    fun ping(host: String): String? {
        val echo = StringBuffer()
        val runtime = Runtime.getRuntime()
        val proc = runtime.exec("ping -c 1 " + host)
        proc.waitFor()
        val exit = proc.exitValue()
        if (exit == 0) {
            val reader = InputStreamReader(proc.inputStream)
            val buffer = BufferedReader(reader)
            var line=buffer.readLine()
            while (line != null) {
                echo.append(line + "\n")
                line=buffer.readLine()
            }
            return getPingStats(echo.toString())
        } else if (exit == 1) {
            pingError = "failed, exit = 1"
            return null
        } else {
            pingError = "error, exit = 2"
            return null
        }
    }

    fun getPingStats(s: String): String? {
        var s = s
        if (s.contains("0% packet loss")) {
            val start = s.indexIn("/mdev = ")
            val end = s.indexOf(" ms\n", start)
            s = s.substring(start + 8, end)
            val stats = s.split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            return stats[2]
        } else if (s.contains("100% packet loss")) {
            pingError = "100% packet loss"
            return null
        } else if (s.contains("% packet loss")) {
            pingError = "partial packet loss"
            return null
        } else if (s.contains("unknown host")) {
            pingError = "unknown host"
            return null
        } else {
            pingError = "unknown error in getPingStats"
            return null
        }
    }
}
