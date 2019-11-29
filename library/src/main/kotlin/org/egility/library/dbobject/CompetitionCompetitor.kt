/*
 * Copyright (c) Mike Brickman 2014-2018
 */

package org.egility.library.dbobject

import org.egility.library.database.*
import org.egility.library.general.*
import java.util.*

open class CompetitionCompetitorRaw<T: DbTable<T>>(_connection: DbConnection? = null, vararg columnNames: String) : DbTable<T>(_connection, "competitionCompetitor", *columnNames) {

    open var idCompetition: Int by DbPropertyInt("idCompetition")
    open var idAccount: Int by DbPropertyInt("idAccount")
    open var idCompetitor: Int by DbPropertyInt("idCompetitor")
    open var voucherCode: String by DbPropertyString("voucherCode")
    open var extra: Json by DbPropertyJson("extra")
    open var flag: Boolean by DbPropertyBoolean("flag")

    var help: JsonNode by DbPropertyJsonObject("extra", "help")
    var helpGroup: String by DbPropertyJsonString("extra", "helpGroup")
    var helpDays: JsonNode by DbPropertyJsonObject("extra", "helpDays")


    val competition: Competition by DbLink<Competition>({ Competition() })
    val competitor: Competitor by DbLink<Competitor>({ Competitor() })

}

val TIME_BAND_AM = 1
val TIME_BAND_PM = 2
val TIME_BAND_DAY = 3

val JOB_RING_MANAGE = 1
val JOB_RING_SCIME = 2
val JOB_RING_CHECKING_IN = 4
val JOB_RING_CALLING = 8
val JOB_RING_LEADS = 16
val JOB_RING_POLLS = 32


data class HelpDay(var date: Date, var timeBand: Int, var jobs: Int, var Judge: String, var group: String)

class CompetitionCompetitor(vararg columnNames: String) : CompetitionCompetitorRaw<CompetitionCompetitor>(null, *columnNames) {

    constructor(idCompetition: Int, idCompetitor: Int) : this() {
        find("idCompetition=$idCompetition AND idCompetitor=$idCompetitor")
    }

    val helpOffer: String 
    get() {
        var result = ""
        var offset = 0
        for (helpDay in helpDays) {
            if (helpDay.has("judge") || helpDay.has("am") || helpDay.has("pm")) {
                val date = if (helpDay.has("date")) helpDay["date"].asDate else competition.dateStart.addDays(offset)
                var day = date.format("EEE")
                if (helpDay.has("am") && !helpDay.has("pm")) day += " AM"
                if (!helpDay.has("am") && helpDay.has("pm")) day += " PM"
                if (helpDay.has("judge")) day += " (${helpDay["judge"].asString})"
                result = result.append(day, ", ")
            }
            offset++
        }
        return result
    }
    
    companion object {

        fun select(where: String, orderBy: String = "", limit: Int = 0): CompetitionCompetitor {
            val competitionCompetitor = CompetitionCompetitor()
            competitionCompetitor.select(where, orderBy, limit)
            return competitionCompetitor
        }

        fun buildGrid(idCompetition: Int) {
            val d = HashMap<Date, String>()
            val days = HashMap<Date, HashMap<String, HashMap<Boolean, ArrayList<String>>>>()

            dbQuery("SELECT DISTINCT classDate, judge FROM agilityClass WHERE idCompetition=$idCompetition") {
                val day = days.getOrDefault(getDate("classDate"), HashMap<String, HashMap<Boolean, ArrayList<String>>>())

                val unassigned = day.getOrDefault(getString(""), HashMap<Boolean, ArrayList<String>>())
                unassigned.getOrDefault(false, ArrayList<String>())
                unassigned.getOrDefault(false, ArrayList<String>())

                val judge = day.getOrDefault(getString("judge"), HashMap<Boolean, ArrayList<String>>())
                val am = judge.getOrDefault(false, ArrayList<String>())
                val pm = judge.getOrDefault(false, ArrayList<String>())
            }

        }

    }
}