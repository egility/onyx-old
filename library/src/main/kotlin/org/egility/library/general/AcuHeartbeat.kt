/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.library.general

import org.egility.library.api.ApiFunctionAcuList
import org.egility.library.api.ApiFunctionHeartbeat
import org.egility.library.general.Global.isAcu
import org.egility.library.general.hardware.addUuid
import org.egility.library.general.hardware.clusterRingManual
import org.egility.library.general.hardware.mesh0Mac
import org.egility.library.general.hardware.ping
import org.egility.library.general.hardware.uuidToTag
import java.util.*
import kotlin.collections.ArrayList

/**
 * Created by mbrickman on 21/10/15.
 */

/*
NOTES


hostNode["system.lastAlive"] is stored as absolute time using cpuTime as a base, but broadcast as relative time

 */

class AcuHeartbeat(_tag: String, _ip: String = "", val parent: ArrayList<AcuHeartbeat>? = null) : Json() {

    var tag: String by PropertyJsonString("tag")
    val system: JsonNode by PropertyJsonNode("system")
    var kill: Int by PropertyJsonInt("kill")
    var display: String by PropertyJsonString("display")
    var mesh0: String by PropertyJsonString("mesh0")
    var gatewaySignal: Int by PropertyJsonInt("gatewaySignal")
    var sdCardSet: Int by PropertyJsonInt("sdCardSet")
    var clusterInstance: Int by PropertyJsonInt("clusterInstance")
    var clusterRing: String by PropertyJsonString("clusterRing")
    var nominations: String by PropertyJsonString("nominations")
    var masters: String by PropertyJsonString("masters")
    var also: String by PropertyJsonString("also")
    val mesh: JsonNode by PropertyJsonNode("mesh")
    val time: JsonNode by PropertyJsonNode("time")
    var isDead: Boolean by PropertyJsonBoolean("dead")
    val alert: JsonNode by PropertyJsonNode("alert")
    var alertSequence: Int by PropertyJsonInt("alert.sequence")
    var alertType: String by PropertyJsonString("alert.type")
    var alertOriginator: String by PropertyJsonString("alert.originator")
    var alertEnd: Date by PropertyJsonDate("alert.end")

    fun startAlert(type: String, sequence: Int, originator: String, end: Date) {
        alert.clear()
        alertType=type
        alertSequence=sequence
        alertOriginator=originator
        alertEnd=end
        when(alertType) {
            "test" -> doNothing()
        }
    }
    
    fun endAlert() {
        when(alertType) {
            "kill" -> hardware.shutDown(0)
            "reboot" -> hardware.reboot(0)
        }
        alert.clear()
    }

    var lastAlive: Long = 0L
    var apSignalStrength = -1
    var apLevel = -1

    val LAST_ALIVE_TIMEOUT_MINUTES = 5

    val address: String
        get() = "192.168.2.${tag.dropLeft(3).toIntDef(0)}"

    val id: Int
        get() = tag.replace("acu", "").toIntDef()

    val isAlive: Boolean
        get() {
            if (isMe || cpuTime - lastAlive < LAST_ALIVE_TIMEOUT_MINUTES * 60000) return true
            if (isDead) return false
            isDead = !ping(address)
            return !isDead
        }

    init {
        if (parent != null) {
            synchronized(parent) {
                heartbeats.add(this)
            }
        }
        tag = _tag
        display = "STARTING..."
    }

    fun populate() {
        addUuid(tag, hardware.uuid)
        kill = if (hardware.isKilling) hardware.killSeconds else 0
        display = "${hardware.errorCodes}|${acuStatus.mobileLine}|${acuStatus.statusLine}"
        gatewaySignal = hardware.gatewaySignal
        if (heartbeats.clusterControllerIsMe) {
            clusterInstance = hardware.clusterRingInstance
            clusterRing = hardware.clusterRing.asCommaList()
        } else {
            clusterInstance = 0
            clusterRing = ""
        }
        masters = hardware.masters.asCommaList()
        mesh.setValue(AcuMesh.batMetrics)
        sdCardSet = hardware.idSite
        if (Global.isAcu) {
            mesh0 = mesh0Mac
        }
        if (hardware.timeState != TIME_UNKNOWN) {
            time["have"] = realNow
            time["confidence"] = hardware.timeState
        }
        if (alertEnd.isNotEmpty() && alertEnd <= now) {
            endAlert()
        }
    }

    val isMe: Boolean
        get() = this == heartbeats.me
}

object heartbeats : ArrayList<AcuHeartbeat>() {

    val me = if (isAcu) AcuHeartbeat(getHostname(), hardware.ip, parent = this) else null

    fun alert(type: String, sequence: Int, originator: String=me?.tag?:"", end: Date= now.addSeconds(60)) {
        me?.startAlert(type, sequence, originator, end)
    }

    private fun getItem(tag: String): AcuHeartbeat {
        synchronized(this) {
            for (acu in this) {
                if (acu.tag == tag) {
                    return acu
                }
            }
        }
        val result = AcuHeartbeat(tag, parent = this)
        debug("HostData", "Add: $tag")
        return result
    }

    fun pruneDead() {
        synchronized(this) {
            for (item in this) {
                if (!item.isAlive) {
                    remove(item)
                    pruneDead()
                    return
                }
            }
        }
    }

    fun updateMe() {
        if (me != null) {
            synchronized(this) {
                me.populate()
            }
        }
    }

    val clusterController: AcuHeartbeat?
        get() {
            synchronized(this) {
                var controlAcu: AcuHeartbeat? = null
                for (acu in this) {
                    if (acu.isAlive && acu.sdCardSet == hardware.idSite) {
                        if (controlAcu == null || acu.tag < controlAcu.tag) {
                            controlAcu = acu
                        }
                    }
                }
                return controlAcu
            }
        }

    val clusterControllerIsMe: Boolean
        get() {
            val controller = clusterController
            return if (!clusterRingManual && me != null && controller != null) me == clusterController else false
        }

    data class ClusterRingData(val controller: String, val instance: Int, val ring: String, val error: Boolean = false)

    val clusterRing: ClusterRingData
        get() {
            val controller = clusterController
            return if (controller != null) {
                ClusterRingData(controller.tag, controller.clusterInstance, controller.clusterRing)
            } else {
                ClusterRingData("", 0, "", true)
            }
        }

    val activePeers: ArrayList<String>
        get() {
            val result = ArrayList<String>()
            synchronized(this) {
                for (acu in this) {
                    result.add(acu.tag)
                }
                return result
            }
        }

    fun getCluster(): AcuCluster {
        val list = ArrayList<String>()
        synchronized(this) {
            for (acu in this) {
                if (acu.isAlive && acu.sdCardSet == hardware.idSite) {
                    list.add(acu.tag)
                }
            }
            val cluster = AcuCluster(list)
            for (acu in this) {
                for (path in acu.mesh) {
                    val tag = path["target"].asString
                    val metric = path["metric"].asInt
                    cluster.addCost(acu.tag, tag, metric)
                }
            }
            return cluster
        }

    }

    val bestGateway: String
        get() {
            synchronized(this) {
                var bestAddress = ""
                var bestSignal = 0
                for (acu in this) {
                    if (acu.isAlive && acu.gatewaySignal != 0 && !acu.isMe) {
                        if (acu.gatewaySignal < bestSignal) {
                            bestAddress = acu.address
                            bestSignal = acu.gatewaySignal
                        }
                    }
                }
                return bestAddress
            }
        }

    fun myBeat(): Json {
        val result = Json()
        if (me != null) {
            synchronized(this) {
                updateMe()
                result["kind"] = ApiFunctionHeartbeat.keyword
                result["version"] = "v1.0"
                result["data"] = me
            }
        }
        return result
    }
    
    fun processBeat(json: Json) {
        val acu = json["data"]
        val tag = acu["tag"].asString
        if (!isAcu || tag != getHostname()) {
            synchronized(this) {
                val target = getItem(tag)
                target.setValue(acu)
                target.lastAlive = cpuTime
                target.isDead = false
                hardware.addMesh0Mac(tag, target.mesh0)
                if (target.kill != 0 && isAcu) {
                    hardware.kill(target.kill)
                }
                if (hardware.timeState == TIME_UNKNOWN && target.has("time.have")) {
                    hardware.setSystemTime(target.time["have"].asDate.time, TIME_FROM_PEER)
                }
                if (target.alertEnd.isNotEmpty() && target.alertSequence > hardware.alertSequence) {
                    if (me != null) {
                        hardware.alertSequence = target.alertSequence
                        me.startAlert(target.alertType, target.alertSequence, target.alertOriginator, target.alertEnd)
                    }
                }
                Collections.sort(this, { a, b -> a.tag.compareTo(b.tag) })
            }
        }
    }

    fun list(dropMesh: Boolean = true): Json {
        getHostname()
        val result = Json()
        synchronized(this) {
            updateMe()
            result["kind"] = ApiFunctionAcuList.keyword
            result["version"] = "v1.0"
            result["OK"] = true
            for (item in this) {
                val node = result["acus"].addElement()
                val json = Json(item)
                if (dropMesh) json.drop("mesh")
                node.setValue(json)
                node["lastAliveMsec"] = if (item.isMe) 0L else cpuTime - item.lastAlive
            }
        }
        return result
    }

    fun processList(json: Json) {
        for (acu in json["acus"]) {
            val tag = acu["tag"].asString
            if (!isAcu || tag != getHostname()) {
                synchronized(this) {
                    val target = getItem(tag)
                    target.setValue(acu)
                    target.lastAlive = cpuTime - acu["lastAliveMsec"].asLong
                    target["lastAliveMsec"].clear()
                }
            }
        }
        Collections.sort(this, { a, b -> a.tag.compareTo(b.tag) })
    }

    fun resetAccessPoints() {
        synchronized(this) {
            for (acu in this) {
                acu.apSignalStrength = -1
            }
        }
    }

    fun logAccessPoint(tag: String, signalStrength: Int, level: Int) {
        debug("logAccessPoint", "tag=$tag, signal=$signalStrength")
        synchronized(this) {
            getItem(tag).apSignalStrength = signalStrength
            getItem(tag).apLevel = level
        }
    }

    fun tagFrom() {
        synchronized(this) {
            for (acu in this) {
                acu.apSignalStrength = -1
            }
        }
    }


}


