package org.egility.linux.tools

import org.egility.library.general.*
import java.io.File
import java.util.*

object SysUtils {

    fun process(path: String) {

        var lastTime = nullDate
        File(path).forEachLine {
            val date = "${today.format("yyyy")} ${it.substring(0, 15)}".toDate("yyyy MMM dd HH:mm:ss")
            val payload = it.substring(16)
            val hostname = payload.substringBefore(" ")
            val service = payload.substringAfter(" ").substringBefore(":")
            val message = payload.substringAfter(":").trim()
            when (service.substringBefore("[")) {
                "do-flint" -> doNothing()
                "kernel" -> doNothing()
                "hostapd" -> doNothing()
                "dhcpd" -> doNothing()
                "dhclient" -> doNothing()
                "ovpn-client" -> doNothing()
                else -> {
                        val diff = if (lastTime.isNotEmpty()) Date(date.time - lastTime.time) else nullDate
                        lastTime = date
                        println("${diff.format("mm:ss")} ${date.format("HH:mm:ss")} $service: $message")
                }
            }
        }

    }

}