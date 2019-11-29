/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.library.dbobject

import org.egility.library.database.*
import org.egility.library.general.Json
import org.egility.library.general.dbExecute
import org.egility.library.general.now
import org.egility.library.general.stack
import java.util.*

/**
 * Created by mbrickman on 17/06/17.
 */
open class MobileSignalRaw<T: DbTable<T>>(_connection: DbConnection? = null, vararg columnNames: String) : DbTable<T>(_connection, "mobileSignal", *columnNames) {

    open var id: Int by DbPropertyInt("idMobileSignal")
    open var idCompetition: Int by DbPropertyInt("idCompetition")
    open var time: Date by DbPropertyDate("time")
    open var imei: String by DbPropertyString("imei")
    open var phoneNumber: String by DbPropertyString("phoneNumber")
    open var networkType: String by DbPropertyString("networkType")
    open var networkProvider: String by DbPropertyString("networkProvider")
    open var pppStatus: String by DbPropertyString("pppStatus")
    open var modemState: String by DbPropertyString("modemState")
    open var signalAverage: Int by DbPropertyInt("signalAverage")
    open var realTimeTx: Int by DbPropertyInt("realTimeTx")
    open var realTimeRx: Int by DbPropertyInt("realTimeRx")
    open var monthTx: Int by DbPropertyInt("monthTx")
    open var monthRx: Int by DbPropertyInt("monthRx")
    open var countyCode: Int by DbPropertyInt("countyCode")
    open var networkCode: Int by DbPropertyInt("networkCode")
    open var cellId: Int by DbPropertyInt("cellId")
}

class MobileSignal(vararg columnNames: String) : MobileSignalRaw<MobileSignal>(null, *columnNames) {

    companion object {
        fun record(report: Json, signalAverage: Int) {
            try {
                val mobileSignal = MobileSignal()
                mobileSignal.append()
                mobileSignal.idCompetition = Competition.current.id
                mobileSignal.time = now
                mobileSignal.imei = report["imei"].asString
                mobileSignal.phoneNumber = report["msisdn"].asString
                mobileSignal.networkType = report["network_type"].asString
                mobileSignal.networkProvider = report["network_provider"].asString
                mobileSignal.pppStatus = report["ppp_status"].asString
                mobileSignal.modemState = report["modem_main_state"].asString
                mobileSignal.signalAverage = -signalAverage
                mobileSignal.realTimeTx = report["realtime_tx_bytes"].asInt
                mobileSignal.realTimeRx = report["realtime_rx_bytes"].asInt
                mobileSignal.monthTx = report["monthly_rx_bytes"].asInt
                mobileSignal.monthRx = report["monthly_tx_bytes"].asInt
                mobileSignal.countyCode = report["rmcc"].asInt
                mobileSignal.networkCode = report["rmnc"].asInt
                mobileSignal.cellId = report["cell_id"].asInt
                mobileSignal.post()
            } catch (e: Throwable) {
                println(e.stack)
            }
        }

        fun fixIds() {
            dbExecute(
                """
                    SELECT 
                        @id:=if(MAX(idMobileSignal) IS NULL, 0, MAX(idMobileSignal))
                    FROM
                        mobileSignal
                    WHERE
                        idMobileSignal < ${Int.MAX_VALUE / 2};    
                """.trimIndent()
            )
    
            dbExecute(
                """
                    UPDATE mobileSignal 
                    SET 
                        idMobileSignal = (@id:=@id + 1)
                    WHERE
                        idMobileSignal > @id ORDER BY time, idMobileSignal; 
                """.trimIndent()
            )
        }
    }
}