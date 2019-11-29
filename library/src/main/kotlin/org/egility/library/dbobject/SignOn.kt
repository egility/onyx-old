/*
 * Copyright (c) Mike Brickman 2014-2018
 */

package org.egility.library.dbobject

import org.egility.library.database.*
import org.egility.library.general.Global
import org.egility.library.general.Json
import org.egility.library.general.dbExecute
import java.util.*

var signedOn = false

open class SignOnRaw<T: DbTable<T>>(_connection: DbConnection? = null, vararg columnNames: String) : DbTable<T>(_connection, "signOn", *columnNames) {
    open var id: Int by DbPropertyInt("idSignOn")
    open var idDevice: Int by DbPropertyInt("idDevice")
    open var ipAddress: String by DbPropertyString("ipAddress")
    open var idCompetition: Int by DbPropertyInt("idCompetition")
    open var ringNumber: Int by DbPropertyInt("ringNumber")
    open var activity: Int by DbPropertyInt("activity")
    open var activityDate: Date by DbPropertyDate("activityDate")
    open var time: Date by DbPropertyDate("signOnTime")
    open var bootTime: Date by DbPropertyDate("bootTime")
    open var version: String by DbPropertyString("version")
    open var accessPoint: String by DbPropertyString("accessPoint")
    open var signal: Int by DbPropertyInt("signal")
    open var battery: Int by DbPropertyInt("battery")
    open var panic: String by DbPropertyString("panic")
    open var extra: Json by DbPropertyJson("extra")
    open var dateCreated: Date by DbPropertyDate("dateCreated")
    open var dateModified: Date by DbPropertyDate("dateModified")

    val competition: Competition by DbLink<Competition>({ Competition() })
    val device: Device by DbLink<Device>({ Device() })
}

class SignOn(vararg columnNames: String) : SignOnRaw<SignOn>(null, *columnNames) {

    companion object {
        
        fun device(device: Device, panic: String="") {
            if (!signedOn) {
                try {
                    with(SignOn()) {
                        append()
                        idDevice = device.id
                        ipAddress = device.ipAddress
                        idCompetition = device.idCompetition
                        ringNumber = device.ringNumber
                        activity = device.activity
                        activityDate = device.activityDate
                        time = device.lastSignOn
                        bootTime = Global.services.bootTime
                        version = device.version
                        accessPoint = device.accessPoint
                        signal = device.signal
                        battery = device.battery
                        this.panic = panic 
                        post()
                    }
                } finally {
                    signedOn = true
                }
            }
        }

        fun fixIds() {
            dbExecute(
                """
                    SELECT 
                        @id:=if(MAX(idSignOn) IS NULL, 0, MAX(idSignOn))
                    FROM
                        signOn
                    WHERE
                        idSignOn < ${Int.MAX_VALUE / 2};    
                """.trimIndent()
            )

            dbExecute(
                """
                    UPDATE signOn 
                    SET 
                        idSignOn = (@id:=@id + 1)
                    WHERE
                        idSignOn > @id ORDER BY dateCreated, idSignOn; 
                """.trimIndent()
            )
        }
    }
}