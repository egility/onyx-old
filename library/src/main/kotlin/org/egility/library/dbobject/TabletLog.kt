/*
 * Copyright (c) Mike Brickman 2014-2018
 */

package org.egility.library.dbobject

import org.egility.library.database.*
import org.egility.library.general.dbExecute
import org.egility.library.general.realNow
import java.util.*

open class TabletLogRaw<T : DbTable<T>>(_connection: DbConnection? = null, vararg columnNames: String) :
    DbTable<T>(_connection, "tabletLog", *columnNames) {
    open var id: Int by DbPropertyInt("idTabletLog")
    open var idDevice: Int by DbPropertyInt("idDevice")
    open var ipAddress: String by DbPropertyString("ipAddress")
    open var idCompetition: Int by DbPropertyInt("idCompetition")
    open var ringNumber: Int by DbPropertyInt("ringNumber")
    open var activity: Int by DbPropertyInt("activity")
    open var activityDate: Date by DbPropertyDate("activityDate")
    open var logTime: Date by DbPropertyDate("logTime")
    open var logType: Int by DbPropertyInt("logType")
    open var version: String by DbPropertyString("version")
    open var accessPoint: String by DbPropertyString("accessPoint")
    open var signal: Int by DbPropertyInt("signal")
    open var battery: Int by DbPropertyInt("battery")
    open var dateCreated: Date by DbPropertyDate("dateCreated")
    open var dateModified: Date by DbPropertyDate("dateModified")

    open var task: String by DbPropertyJsonString("extra", "task")
    open var voltage: Int by DbPropertyJsonInt("extra", "voltage")
    open var batteryHealth: String by DbPropertyJsonString("extra", "battery")

    val competition: Competition by DbLink<Competition>({ Competition() })

}

class TabletLog(_connection: DbConnection? = null, vararg columnNames: String) : TabletLogRaw<TabletLog>(_connection, *columnNames) {

    fun log(device: Device, type: Int, battery: Int, averageSignal: Int, task: String = "", voltage: Int = 0, 
            batteryHealth: String="") {
        append()
        idDevice = device.id
        ipAddress = device.ipAddress
        idCompetition = device.idCompetition
        ringNumber = device.ringNumber
        activity = device.activity
        activityDate = device.activityDate
        logTime = realNow
        logType = type
        version = device.version
        accessPoint = device.accessPoint
        signal = averageSignal
        this.task = task
        this.battery = battery
        this.voltage = voltage
        this.batteryHealth = batteryHealth
        post()
    }

    companion object {
        fun fixIds() {
            dbExecute(
                """
                SELECT 
                    @id:=if(MAX(idTabletLog) IS NULL, 0, MAX(idTabletLog))
                FROM
                    tabletLog
                WHERE
                    idTabletLog < ${Int.MAX_VALUE / 2};    
            """.trimIndent()
            )

            dbExecute(
                """
                UPDATE tabletLog 
                SET 
                    idTabletLog = (@id:=@id + 1)
                WHERE
                    idTabletLog > @id ORDER BY dateCreated, idTabletLog; 
            """.trimIndent()
            )
        }
    }

}

