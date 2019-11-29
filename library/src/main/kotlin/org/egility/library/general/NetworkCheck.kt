/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.library.general

import org.egility.library.api.ApiFunctionAcuList
import org.egility.library.api.ApiFunctionDiagnostics
import org.egility.library.dbobject.Device
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

/**
 * Created by mbrickman on 15/07/17.
 */
object NetworkCheck {

    data class Tablet(val device: String, val throughput: String, val acu: String, val activity: Int, val ring: Int, val inactive: String, val rx: String, val tx: String, val txRetries: String, val txFailed: String)

    val all = Json.nullNode()
    var log: String = ""
    var acuList = ArrayList<String>()

    val device = Device()
    val tablets = ArrayList<Tablet>()

    data class MeshPath(val via: String, val metric: Int, val flags: Int)

    val meshMap = HashMap<Pair<String, String>, MeshPath>()

    fun load(master: String, brief: Boolean = true) {
        log = ""
        tablets.clear()
        meshMap.clear()
        all.clear()
        acuList.clear()
        val list = ApiFunctionAcuList()
        list.requestHttp(master, "v1.0")
        for (acu in list.acus) {
            if (acu["lastAliveMsec"].asInt < 60000) {
                println(acu["tag"].asString)
                val acuNumber = acu["tag"].asString.dropLeft(3).toInt()
                val ip = if (master.startsWith("172.")) "172.16.$acuNumber.1" else "10.8.1.$acuNumber"
                val diagnostics = ApiFunctionDiagnostics()
                diagnostics.requestHttp(ip, "v1.0")
                all.addElement().setValue(diagnostics.data)
            }
        }

        for (acu in all) {
            acuList.add(acu["hostname"].asString)
            for (node in acu["network.apStations"]) {
                val tag = node["device"].asString
                device.find("tag=${tag.quoted}")
                if (device.found() && device.type == 2) {
                    tablets.add(Tablet(tag, node["expectedthroughput"].asString, acu["hostname"].asString, device.activity, if (device.activity.oneOf(10, 20)) device.ringNumber else 0,
                            node["inactivetime"].asString, node["rx"].asString, node["tx"].asString, node["txretries"].asString, node["txfailed"].asString))
                }
            }

            for (node in acu["network.meshPaths"]) {
                val source = acu["hostname"].asString
                val target = node["target"].asString
                val metric = node["metric"].asInt
                val flags = node["flags"].asInt
                val via = node["via"].asString
                meshMap.put(Pair(source, target), MeshPath(via, metric, flags))
            }

        }

        tablets.sortWith(Comparator { a, b ->
            if (a.ring != b.ring) a.ring.compareTo(b.ring)
            else if (a.activity != b.activity) a.activity.compareTo(b.activity)
            else a.device.compareTo(b.device)
        })

        var thisRing = -1
        for (tablet in tablets) {
            if (tablet.ring != thisRing) {
                thisRing = tablet.ring
                log = log.newlineAppend(if (thisRing == 0) "Administration" else "Ring $thisRing")
            }
            val activity = deviceToText(tablet.activity)
            if (brief) {
                log = log.newlineAppend("  ${tablet.device} ($activity) -> ${tablet.acu} ${tablet.throughput}")
            } else {
                log = log.newlineAppend("  ${tablet.device} ($activity) -> ${tablet.acu} ${tablet.throughput}, inactive: ${tablet.inactive}, rx/tx: ${tablet.rx}/${tablet.tx}, retries/failed: ${tablet.txRetries}/${tablet.txFailed}")
            }
        }


        var heading = "Source "
        var underline = "------ "
        for (target in acuList) {
            heading += "$target "
            underline += "------ "
        }

        log = log.newlineAppend("-")
        log = log.newlineAppend("MESH METRICS")
        log = log.newlineAppend("-")
        log = log.newlineAppend(heading)
        log = log.newlineAppend(underline)
        for (source in acuList) {
            var line = "$source "
            for (target in acuList) {
                if (source != target) {
                    val path = meshMap.getOrPut(Pair(source, target)) { MeshPath("n/a", 0, 0) }
                    line += "%6d ".format(path.metric)
                } else {
                    line += "     0 "

                }
            }
            log = log.newlineAppend(line)
        }

        log = log.newlineAppend("-")
        log = log.newlineAppend("MESH FLAGS")
        log = log.newlineAppend("-")
        log = log.newlineAppend(heading)
        log = log.newlineAppend(underline)
        for (source in acuList) {
            var line = "$source "
            for (target in acuList) {
                if (source != target) {
                    val path = meshMap.getOrPut(Pair(source, target)) { MeshPath("n/a", 0, 0) }
                    line += "%6d ".format(path.flags)
                } else {
                    line += "     0 "

                }
            }
            log = log.newlineAppend(line)
        }

        log = log.newlineAppend("-")
        log = log.newlineAppend("MESH HOPS")
        log = log.newlineAppend("-")
        log = log.newlineAppend(heading)
        log = log.newlineAppend(underline)
        for (source in acuList) {
            var line = "$source "
            for (target in acuList) {
                if (source != target) {
                    val path = meshMap.getOrPut(Pair(source, target)) { MeshPath("n/a", 0, 0) }
                    line += if (path.via.isNotEmpty()) "${path.via} " else "  -    "
                } else {
                    line += "  -    "

                }
            }
            log = log.newlineAppend(line)
        }

    }

}