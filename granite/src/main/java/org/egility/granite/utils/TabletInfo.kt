/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.granite.utils

import android.content.Context
import android.os.Build
import org.egility.library.dbobject.Device
import org.egility.library.general.*
import java.io.InputStream


/**
 * Created by mbrickman on 04/09/15.
 */
object TabletInfo {

    private val CONFIG_PATH = "/data/config.json"

    var deviceName = ""
    var activity = DEVICE_UNASSIGNED
    var ringNumber = 1
    var activityDate = nullDate
    val assigned: Boolean
        get() = !Global.reassignTabletUsage && activity!=DEVICE_UNASSIGNED

    val activityText: String
        get() {
            when (activity) {
                DEVICE_UNASSIGNED -> return "Unassigned Tablet"
                DEVICE_SECRETARY -> return "Secretary's Tablet"
                DEVICE_SYSTEM_MANAGER -> return "System Manager's Tablet"
                DEVICE_RING_PARTY -> return "Ring Party Tablet - Ring $ringNumber"
                DEVICE_SCOREBOARD -> return "Scoreboard Tablet - Ring $ringNumber"
                else -> return "UNKNOWN"
            }
        }


    fun updateDevice() {
        if (!Global.reassignTabletUsage) {
            Device.setActivity(activity, ringNumber, activityDate)
        } else {
            Device.setActivity(DEVICE_UNASSIGNED, ringNumber, nullDate)
        }
    }

    fun readConfig(context: Context) {
        try {
            var inputStream: InputStream = context.openFileInput("granite.json")
            val data = Json(inputStream)
            deviceName = data["deviceName"].asString
            if (data.has("activityDate")) {
                activityDate = data["activityDate"].asDate
            } else {
                activityDate = nullDate
            }
            debug("tablet", "activityDate = ${activityDate.dateText}")
            if (Build.DEVICE.oneOf("T61", "T62C") || deviceName.oneOf("tab039") && data.has("activity") && data["activity"].asInt== DEVICE_SYSTEM_MANAGER || (data.has("activity") && activityDate.dateOnly() >= today)) {
                activity = data["activity"].asInt
            } else {
                activity = DEVICE_UNASSIGNED
            }
            if (data.has("ringNumber")) {
                ringNumber = data["ringNumber"].asInt
            } else {
                ringNumber = 1
            }
            inputStream.close()
        } catch (e: Throwable) {
            deviceName = "TBA"
            activity = DEVICE_UNASSIGNED
            ringNumber = 1
            return
        }

    }

    fun saveConfig(context: Context) {

        context.deleteFile("granite.json")
        val out = context.openFileOutput("granite.json", Context.MODE_PRIVATE)
        debug("tablet", "SET activityDate = ${activityDate.dateText}")

        val data = Json()
        data["deviceName"] = deviceName
        data["activity"] = activity
        data["ringNumber"] = ringNumber
        data["activityDate"] = activityDate
        data.save(out, pretty = true)

        out.flush()
        out.close()
    }
    
    fun saveDeviceConfig(context: Context) {
        deviceName = Device.thisTag
        activity = Device.thisDevice.activity
        ringNumber = Device.thisDevice.ringNumber
        activityDate = Device.thisDevice.activityDate

        saveConfig(context)
    }

}
