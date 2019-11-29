/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.linux.tools

import org.egility.library.dbobject.ReportQueue
import org.egility.library.general.*
import org.egility.linux.reports.*
import java.util.*
import javax.swing.JOptionPane


class NativeServices(val console: Boolean) : Services {

    override val bootTime: Date
        get() = nullDate

    override fun msgYesNo(title: String, message: String, body: (Boolean) -> Unit) {
        throw UnsupportedOperationException()
    }

    override fun panic(throwable: Throwable) {
        if (console) {
            if (throwable is SilentWobbly) {
                throw throwable
            } else {
                debug("Panic", throwable.stack)
                throwable.printStackTrace()
                throw SilentWobbly("Silent", throwable)
                //System.exit(1)
            }
        } else {
            popUp("Error", throwable.message ?: "No error message text")
        }
    }

    override fun log(message: String) {
        if (console) {
            println(message)
        }
    }

    override fun popUp(Title: String, Message: String) {
        if (!console) {
            JOptionPane.showMessageDialog(null, Message)
        }
    }

    override fun checkNetwork() {

    }

    override val acuHostname: String
        get() = ""


    private val reports = listOf(
        ResultsReport.Companion,
        AwardsReport.Companion,
        LateEntryCreditsReport.Companion,
        LateEntryFreeReport.Companion,
        LateEntrySpecialReport.Companion,
        ComplimentaryCreditsUsedReport.Companion,
        ChequesReport.Companion,
        RunningOrderReport.Companion,
        EntriesReport.Companion,
        EndOfDayReport.Companion,
        RingCardReport.Companion,
        CallingListReport.Companion,
        EmergencyScoreReport.Companion,
        CampingListReport.Companion,
        AddressLabelsReport.Companion,
        PersonalRunningOrderReport.Companion,
        MeasurementReport.Companion,
        RegistrationsReport.Companion
    )

    override fun generateReport(reportRequest: Json, canSpool: Boolean): String {
        if (Global.isAcu && canSpool) {
            ReportQueue.spool(reportRequest)
            return "spooled"
        } else {
            val keyword = reportRequest["report"].asString
            val pdfFile = Param<String>()
            for (report in reports) {
                if (report.keyword == keyword) {
                    report.generateJson(reportRequest, pdfFile)
                    return pdfFile.value ?: ""
                }
            }
            throw Wobbly("unknown keyword ($keyword) in NativeServices.generateReport")
        }
    }

    companion object {

        fun initialize(console: Boolean) {
            Global.services = NativeServices(console)
        }
    }

}
