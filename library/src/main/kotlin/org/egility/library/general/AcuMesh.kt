/*
 * Copyright (c) Mike Brickman 2014-2018
 */

package org.egility.library.general

import org.egility.library.dbobject.Device
import kotlin.math.pow

/**
 * Created by mbrickman on 17/09/18.
 */

private const val MISSING_MESH = 9999
private const val MAX_OBSERVATIONS = 5

data class MeshData(val mac: String) {
    private val observations = ArrayList<Int>()
    private var dsn = -1
    var tag = ""
    var totalObservations = 0
    var visible = false

    val observed: String
        get() {
            synchronized(observations) {
                return observations.asCommaList()
            }
        }

    val average: Int
        get() {
            var result = 0
            synchronized(observations) {
                var total = 0
                observations.forEach { total += it }
                result = if (observations.size > 0) total / observations.size else MISSING_MESH
            }
            return result
        }

    fun addObservation(dsn: Int, metric: Int, flags: Int) {
        synchronized(observations) {
            if (dsn > this.dsn && flags == 21) {
                if (observations.size >= MAX_OBSERVATIONS) observations.removeAt(0)
                observations.add(metric)
                totalObservations++
            }
        }
        visible = true
    }

    fun prepare() {
        visible = false
    }

    fun finalize() {
        if (visible == false) {
            synchronized(observations) {
                if (observations.size >= MAX_OBSERVATIONS) observations.removeAt(0)
                observations.add(MISSING_MESH)
            }
        }
    }
}

object AcuMesh {

    const val DEVELOPMENT = false

    val meshMap = HashMap<String, MeshData>()

    val metrics: Json
        get() {
            val result = Json()
            for (item in meshMap) {
                if (item.value.tag.isNotEmpty()) {
                    val average = item.value.average
                    if (average != MISSING_MESH) {
                        val node = result.addElement()
                        node["target"] = item.value.tag
                        node["metric"] = average
                        if (DEVELOPMENT) {
                            node["observations"] = item.value.observed
                            node["totalObservations"] = item.value.totalObservations
                        }
                    }
                }
            }
            return result
        }

    val batMetrics: Json 
    get() {
        val result = Json()
        val table = execStr("iw dev mesh0 station dump")
        val entries = table.split("\n")
        var node = Json.nullNode()
        var destination = ""
        for (entry in entries) {
            if (entry.startsWith("Station")) {
                val mac = entry.substring(8, 25).trim()
                destination = hardware.mesh0ToTag(mac)
            } else {
                val label = entry.substringBefore(":").trim()
                val data = entry.substringAfter(":").trim()
                if (label=="signal avg") {
                    val signal=-data.substringBefore(" ").toIntDef(1)
                    if (signal>-1) {
                        val metric = maxOf(0, signal-20).toDouble().pow(1.5).toInt()
                        val node = result.addElement()
                        node["target"] = destination
                        node["metric"] =metric
                    }
                }
            }
        }
        return result
    }


    fun update() {
        val table = execStr("iw dev mesh0 mpath dump")
        val entries = table.split("\n")
        synchronized(meshMap) {
            meshMap.forEach { it.value.prepare() }
            for (entry in entries) {
                if (entry.contains("mesh0")) {
                    val fields = entry.replace(Regex("\\s"), ",").split(",")
                    val destination = fields[0]
                    val hop = fields[1]
                    if (hop != "00:00:00:00:00:00") {
                        val data = meshMap.getOrPut(destination) { MeshData(destination) }
                        data.tag = hardware.mesh0ToTag(destination)
                        val dsn = fields[3].toIntDef(0)
                        val metric = fields[4].toIntDef(0)
                        val flags = fields[9].replace("0x", "").toInt(16)
                        data.addObservation(dsn, metric, flags)
                    }
                }
            }
            meshMap.forEach { it.value.finalize() }
        }

    }




}

