/*
 * Copyright (c) Mike Brickman 2014-2018
 */

package org.egility.library.dbobject

import org.egility.library.database.*
import org.egility.library.general.*
import java.util.*

open class ReplicationFaultRaw<T: DbTable<T>>(_connection: DbConnection? = null, vararg columnNames: String) : DbTable<T>(_connection, "replicationFault", *columnNames) {
    open var id: Int by DbPropertyInt("idReplicationFault")
    open var idCompetition: Int by DbPropertyInt("idCompetition")
    open var channelName: String by DbPropertyString("channelName")
    open var relayLogFile: String by DbPropertyString("relayLogFile")
    open var relayLogPos: Int by DbPropertyInt("relayLogPos")
    open var sqlError: String by DbPropertyString("sqlError")
    open var dateCreated: Date by DbPropertyDate("dateCreated")
    open var deviceCreated: Int by DbPropertyInt("deviceCreated")
    open var dateModified: Date by DbPropertyDate("dateModified")
    open var deviceModified: Int by DbPropertyInt("deviceModified")
    open var dateDeleted: Date by DbPropertyDate("dateDeleted")

    val competition: Competition by DbLink<Competition>({ Competition() })
    val device: Device by DbLink<Device>({ Device() }, label = "device", keyNames = *arrayOf("deviceCreated"))
}

class ReplicationFault(vararg columnNames: String) : ReplicationFaultRaw<ReplicationFault>(null, *columnNames) {

    constructor(idReplicationFault: Int) : this() {
        find(idReplicationFault)
    }

    companion object {

        fun skip(relayLogFile: String, relayLogPos: Int, channelName: String) {
            dbQuery("SHOW RELAYLOG EVENTS IN '$relayLogFile' FROM $relayLogPos LIMIT 1 FOR CHANNEL '$channelName'") {
                val eventType = getString("Event_type")
                val info = getString("Info")
                if (eventType.eq("Gtid")) {
                    val gtidNext = info.substringAfter("=").replace("'", "").trim()
                    dbExecute("STOP SLAVE FOR CHANNEL '$channelName'")
                    dbExecute("SET GTID_NEXT='$gtidNext'")
                    dbExecute("BEGIN")
                    dbExecute("COMMIT")
                    dbExecute("SET GTID_NEXT=\"AUTOMATIC\"")
                    dbExecute("START SLAVE FOR CHANNEL '$channelName'")
                }
            }
        }

        fun errorCode(idCompetition: Int=0): String {
            var errorCode = ""
            var doCheck=true
            try {
                while (doCheck) {
                    doCheck = false
                    dbQuery("SHOW SLAVE STATUS") {
                        val ioRunning = getString("Slave_IO_Running") eq "Yes"
                        val sqlRunning = getString("Slave_SQL_Running") eq "Yes"
                        val lastSqlError = getString("Last_SQL_Error")
                        val relayLogFile = getString("Relay_Log_File")
                        val relayLogPos = getInt("Relay_Log_Pos")
                        val channelName = getString("Channel_Name")
                        
                        if (!(ioRunning && sqlRunning) && channelName.startsWith("acu")) {
                            errorCode += "L${channelName.dropLeft(3).toIntDef(99)}"
                        }

                        if (ioRunning && !sqlRunning && lastSqlError.isNotEmpty()) {
                            ReplicationFault().withAppendPost {
                                this.idCompetition = idCompetition
                                this.channelName = channelName
                                this.relayLogFile = relayLogFile
                                this.relayLogPos = relayLogPos
                                this.sqlError = lastSqlError
                            }
                            skip(relayLogFile, relayLogPos, channelName)
                            doCheck = true
                        }
                    }
                }
            } catch (e: Throwable) {
                errorCode = "L?"
            }
            return errorCode
        }

        fun fixWebsiteReplication() {
            var doCheck=true
            while (doCheck) {
                doCheck = false
                dbQuery("SHOW SLAVE STATUS") {
                    val ioRunning = getString("Slave_IO_Running") eq "Yes"
                    val sqlRunning = getString("Slave_SQL_Running") eq "Yes"
                    val lastSqlError = getString("Last_SQL_Error")
                    val relayLogFile = getString("Relay_Log_File")
                    val relayLogPos = getInt("Relay_Log_Pos")
                    val channelName = getString("Channel_Name")
                    if (ioRunning && !sqlRunning && lastSqlError.isNotEmpty()) {
                        println(lastSqlError)
                        skip(relayLogFile, relayLogPos, channelName)
                        doCheck = true
                    }
                }
            }
        }

    }
}