/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.library.dbobject

import org.egility.library.database.*
import org.egility.library.general.*
import java.util.*
import kotlin.collections.HashMap

/**
 * Created by mbrickman on 28/07/15.
 */

open class DeviceRaw<T: DbTable<T>>(_connection: DbConnection? = null, vararg columnNames: String) : DbTable<T>(_connection, "device", *columnNames) {

    open var id: Int by DbPropertyInt("idDevice")
    open var type: Int by DbPropertyInt("type")
    open var serial: String by DbPropertyString("serial")
    open var model: String by DbPropertyString("model")
    open var subType: String by DbPropertyString("subType")
    open var tag: String by DbPropertyString("tag")
    open var assetCode: String by DbPropertyString("assetCode")
    open var macAddress: String by DbPropertyString("macAddress")
    open var macAddressMesh: String by DbPropertyString("macAddressMesh")
    open var macAddressBat: String by DbPropertyString("macAddressBat")
    open var sdCardSet: Int by DbPropertyInt("sdCardSet")
    open var serverId: Int by DbPropertyInt("serverId")
    open var databaseUuid: String by DbPropertyString("databaseUuid")
    open var ipAddress: String by DbPropertyString("ipAddress")
    open var idCompetition: Int by DbPropertyInt("idCompetition")
    open var idCompetitionStock: Int by DbPropertyInt("idCompetitionStock")
    open var ringNumber: Int by DbPropertyInt("ringNumber")
    open var activity: Int by DbPropertyInt("activity")
    open var activityDate: Date by DbPropertyDate("activityDate")
    open var data1: Int by DbPropertyInt("data1")
    open var data2: Int by DbPropertyInt("data2")
    open var info: String by DbPropertyString("info")
    open var lastSignOn: Date by DbPropertyDate("lastSignOn")
    open var version: String by DbPropertyString("version")
    open var accessPoint: String by DbPropertyString("accessPoint")
    open var signal: Int by DbPropertyInt("signal")
    open var battery: Int by DbPropertyInt("battery")
    open var offLine: Boolean by DbPropertyBoolean("offLine")
    open var owner: Int by DbPropertyInt("owner")
    open var locationType: Int by DbPropertyInt("locationType")
    open var locationText: String by DbPropertyString("locationText")
    open var dateAquired: Date by DbPropertyDate("dateAquired")
    open var extra: Json by DbPropertyJson("extra")
    open var dateCreated: Date by DbPropertyDate("dateCreated")
    open var dateModified: Date by DbPropertyDate("dateModified")

    open var task: String by DbPropertyJsonString("extra", "task")


    val competition: Competition by DbLink<Competition>({ Competition() })

}

class Device(_connection: DbConnection? = null, vararg columnNames: String) : DeviceRaw<Device>(_connection, *columnNames) {

    constructor(idDevice: Int) : this() {
        find(idDevice)
    }
    
    companion object {

        val thisDevice = Device()
        private var lastAcuMacAddress = ""
        private var matchingAcu = ""

        val assigned: Boolean
            get() = !Global.reassignTabletUsage
//        get() = Device.thisDevice.activityDate == realToday && !Global.reassignTabletUsage

        fun reset() {
            thisDevice.release()
            lastMacAddress = ""
            matchingTag = ""
            lastAcuMacAddress = ""
            matchingAcu = ""
        }

        fun register(type: Int, serial: String, model: String, tagRoot: String, macAddress: String, ipAddress: String,
                     info: String, accessPoint: String = "", signal: Int = -1, battery: Int = -1, 
                     macAddressMesh: String = "", sdCardSet: Int = 0,
                     serverId: Int = 0, databaseUuid: String = "", idCompetition: Int=0, panic: String="", macAddressBat: String="") {

            var _serial = if (serial.isEmpty()) macAddress else serial
            debug("register", "type=$type, serial=$serial, model=$model, tagRoot=$tagRoot, macAddress=$macAddress, macAddressBat=$macAddressBat, ipAddress=$ipAddress, info=$info, databaseUuid=$databaseUuid")

            thisDevice.select("type=%d AND serial=%s", type, _serial.quoted)
            if (!thisDevice.found()) {
                thisDevice.append()
                thisDevice.type = type
                thisDevice.serial = _serial
                thisDevice.tag = getNextTag(tagRoot)
            }
            thisDevice.model = model
            thisDevice.info = info
            if (macAddress.isNotEmpty()) {
                thisDevice.macAddress = macAddress
            }
            thisDevice.ipAddress = ipAddress
            thisDevice.lastSignOn = realNow
            thisDevice.version = Global.version
            if (accessPoint != "") {
                thisDevice.accessPoint = accessPoint
                thisDevice.signal = signal
            }

            if (battery>-1) {
                thisDevice.battery = battery
            }
            if (macAddressMesh.isNotEmpty()) {
                thisDevice.macAddressMesh = macAddressMesh
            }
            if (macAddressBat.isNotEmpty()) {
                thisDevice.macAddressBat = macAddressBat
            }
            thisDevice.sdCardSet = sdCardSet
            thisDevice.serverId = serverId
            thisDevice.databaseUuid = databaseUuid

            thisDevice.idCompetition = idCompetition

            thisDevice.offLine = false
            thisDevice.post()
            Global.idDevice = thisDevice.id

            SignOn.device(thisDevice, panic)
            TabletLog().log(thisDevice, 0, thisDevice.battery, thisDevice.signal, "signOn")
        }

        fun setActivity(activity: Int, ringNumber: Int = -1, activityDate: Date) {
            thisDevice.activity = activity
            if (ringNumber != -1) {
                thisDevice.ringNumber = ringNumber
            }
            thisDevice.activityDate = activityDate
            thisDevice.post()
            Global.reassignTabletUsage = false
        }

        private fun getNextTag(tagRoot: String): String {
            val query=DbQuery("SELECT tag FROM device WHERE tag LIKE ${(tagRoot + "%").quoted} ORDER BY tag DESC LIMIT 1")
            if (query.found()) {
                val lastTag = query.getString("tag")
                val lastId = Integer.parseInt(lastTag.substring(tagRoot.length))
                return "%s%d".format(tagRoot, lastId + 1)
            }
            return tagRoot + "100"
        }

        val thisTag: String
            get() {
                try {
                    if (thisDevice.isOnRow) {
                        return thisDevice.tag
                    }
                } catch (wobbly: Wobbly) {
                }

                return "unknown"
            }

        val thisActivity: Int
            get() = thisDevice.activity

        fun setThisRingNumber(ringNumber: Int) {
            thisDevice.ringNumber = ringNumber
            thisDevice.post()
        }

        fun setSetThisActivity(activity: Int) {
            thisDevice.activity = activity
            thisDevice.post()
        }

        val isRingParty: Boolean
            get() = thisDevice.isOnRow && thisDevice.activity == DEVICE_RING_PARTY

        val isScoreboard: Boolean
            get() = thisDevice.isOnRow && thisDevice.activity == DEVICE_SCOREBOARD

        val isPrivileged: Boolean
            get() = thisDevice.isOnRow && thisDevice.activity.oneOf(DEVICE_SECRETARY, DEVICE_SYSTEM_MANAGER)

        val isSystemManager: Boolean
            get() = thisDevice.isOnRow && thisDevice.activity.oneOf(DEVICE_SYSTEM_MANAGER)

        fun generateAcuDat() {
            Device().where("type=1", "tag") {
                println("${tag.dropLeft(3)}|$serial")                
            }
        }

        private var lastMacAddress = ""
        private var matchingTag = ""
        fun acuTagFromAccessPointMac(macAddress: String): String {
            if (Global.haveConnection) {
                if (macAddress != lastMacAddress) {
                    val sql = "SELECT tag FROM device WHERE macAddress=${macAddress.quoted} ORDER BY dateModified DESC"
                    val query = DbQuery(sql)
                    if (query.first()) {
                        lastMacAddress = macAddress
                        matchingTag = query.getString("tag")
                        return matchingTag
                    } else {
                        return "<$macAddress>"
                    }
                }
                return matchingTag
            } else {
                return "<$macAddress>"
            }
        }

        fun acuTagFromUuid(uuid: String): String {
            if (uuid.isNotEmpty() && Global.haveConnection) {
                val query = DbQuery("SELECT tag FROM device WHERE databaseUuid=${uuid.quoted}")
                if (query.first()) {
                    return query.getString("tag")
                }
            }
            return ""
        }
    }

}
