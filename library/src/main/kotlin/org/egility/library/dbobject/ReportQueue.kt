/*
 * Copyright (c) Mike Brickman 2014-2018
 */

package org.egility.library.dbobject

import org.egility.library.database.*
import org.egility.library.general.*
import java.util.*

open class ReportQueueRaw<T: DbTable<T>>(_connection: DbConnection? = null, vararg columnNames: String) : DbTable<T>(_connection, "reportQueue", *columnNames) {
    open var id: Int by DbPropertyInt("idReportQueue")
    open var idCompetition: Int by DbPropertyInt("idCompetition")
    open var report: String by DbPropertyString("report")
    open var request: Json by DbPropertyJson("request")
    open var timeLocked: Date by DbPropertyDate("timeLocked")
    open var timePrinted: Date by DbPropertyDate("timePrinted")
    open var status: Int by DbPropertyInt("status")
    open var dateCreated: Date by DbPropertyDate("dateCreated")
    open var dateModified: Date by DbPropertyDate("dateModified")
}

class ReportQueue(vararg columnNames: String) : ReportQueueRaw<ReportQueue>(null, *columnNames) {

    constructor(idReportQueue: Int) : this() {
        find(idReportQueue)
    }

    companion object {
        fun spool(reportRequest: Json) {
            val keyword=reportRequest["report"].asString
            with (ReportQueue()) {
                append()
                idCompetition = Competition.current.id
                report = keyword
                request = reportRequest
                post()
            }

        }

        fun despool(items: Int = -1) {
            //Mutex.ifAquired("printQueue") {
                ReportQueue().where("timeLocked=0 AND timePrinted=0") {
                    timeLocked = now
                    post()
                    try {
                        Global.services.generateReport(request, canSpool = false)
                        timePrinted = now
                        post()
                    } catch (e: Throwable) {
                        timeLocked = nullDate
                        post()
                    }
                }
            //}
        }


    }
}