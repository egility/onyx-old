/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.library.dbobject

import org.egility.library.database.*
import org.egility.library.general.*
import java.util.*

open class CompetitionDayRaw<T: DbTable<T>>(_connection: DbConnection? = null, vararg columnNames: String) : DbTable<T>(_connection, "competitionDay", *columnNames) {
    var idCompetition: Int by DbPropertyInt("idCompetition")
    var date: Date by DbPropertyDate("date")
    var dayType: Int by DbPropertyInt("dayType")
    var cash1: Int by DbPropertyInt("cash1")
    var cash2: Int by DbPropertyInt("cash2")
    var cash5: Int by DbPropertyInt("cash5")
    var cash10: Int by DbPropertyInt("cash10")
    var cash20: Int by DbPropertyInt("cash20")
    var cash50: Int by DbPropertyInt("cash50")
    var cash100: Int by DbPropertyInt("cash100")
    var cash200: Int by DbPropertyInt("cash200")
    var cash500: Int by DbPropertyInt("cash500")
    var cash1000: Int by DbPropertyInt("cash1000")
    var cash2000: Int by DbPropertyInt("cash2000")
    var cash5000: Int by DbPropertyInt("cash5000")
    var cashOther: Int by DbPropertyInt("cashOther")
    var float: Int by DbPropertyInt("float")
    var cashRemoved: Int by DbPropertyInt("cashRemoved")
    var totalCash: Int by DbPropertyInt("totalCash")
    var totalCheque: Int by DbPropertyInt("totalCheque")
    var locked: Boolean by DbPropertyBoolean("locked")
    var dateCreated: Date by DbPropertyDate("dateCreated")
    var deviceCreated: Int by DbPropertyInt("deviceCreated")
    var dateModified: Date by DbPropertyDate("dateModified")

    val competition: Competition by DbLink<Competition>({Competition()})

}

class CompetitionDay(vararg columnNames: String) : CompetitionDayRaw<CompetitionDay>(null, *columnNames) {

    fun seek(idCompetition: Int, date: Date): Boolean {
        return find("competitionDay.idCompetition=$idCompetition AND competitionDay.date=${date.sqlDate}")
    }

    fun calculateTotalCash() {
        totalCash = cash1 * 1 + cash2 * 2 + cash5 * 5 + cash10 * 10 + cash20 * 20 + cash50 * 50 + cash100 * 100 + cash200 * 200 + cash500 * 500 + cash1000 * 1000 + cash2000 * 2000 + cash5000 * 5000
    }


    fun add(idCompetition: Int, date: Date) {
        if (!seek(idCompetition, date)) {
            append()
            this.idCompetition = idCompetition
            this.date = date
            post()
        }
    }

    var paidCreditsCash = 0
    var paidCreditsCheque = 0
    
    val paidCredits: Int
        get() = paidCreditsCash + paidCreditsCheque
    
    var specialEntriesCash = 0
    var specialEntriesCheque = 0

    val specialEntries: Int
        get() = specialEntriesCash + specialEntriesCheque

    var lateEntryCash = 0
    var lateEntryCheque = 0

    val lateEntry: Int
        get() = lateEntryCash + lateEntryCheque

    var specialCash = 0
    var specialCheque = 0

    val special: Int
        get() = specialCash + specialCheque

    var registrationsCash = 0
    var registrationsCheque = 0
    val registrations: Int
        get() = registrationsCash + registrationsCheque

    var complimentaryRuns = 0

    val netComplimentary: Int
        get() {
            if (complimentaryRuns > competition.complimentaryAllowance) {
                return complimentaryRuns - competition.complimentaryAllowance
            } else {
                return 0
            }
        }

    val lateEntryCutCash: Int
        get() = paidCreditsCash * 140

    val lateEntryCutCheque: Int
        get() = paidCreditsCheque * 140
    
    val lateEntryCut: Int
        get() = lateEntryCutCash + lateEntryCutCheque

    val complimentaryCut: Int
        get() = netComplimentary * 100

    val specialCutCash: Int
        get() = specialEntriesCash * 305

    val specialCutCheque: Int
        get() = specialEntriesCheque * 305

    val specialCut: Int
        get() = specialCutCash + specialCutCheque

    val totalCut: Int
        get() = lateEntryCutCash + complimentaryCut + specialCutCash + registrationsCash

    val calculatedCash: Int
        get() = lateEntryCash + specialCash + registrationsCash + cashOther

    val calculatedCheque: Int
        get() = lateEntryCheque + specialCheque + registrationsCheque

    val differenceCash: Int
        get() = totalCash + cashRemoved - float - calculatedCash

    val differenceCheque: Int
        get() = totalCheque - calculatedCheque
    
    val takingsCash: Int
        get() =lateEntryCash + specialCash + registrationsCash + cashOther

    val takingsCheque: Int
        get() =lateEntryCheque + specialCheque + registrationsCheque 
    
    val takings: Int
        get() = takingsCash + takingsCheque
    
    val ukaFees: Int
        get() = lateEntryCut + complimentaryCut + specialCut + registrations

    val netTakings: Int
        get() = takings - ukaFees

/*
           dataSource.add("Late Entry Credits", day.paidCredits, day.lateEntryCut)
        dataSource.add(complimentaryRunsLabel, day.netComplimentary, day.complimentaryCut)
        dataSource.add("Masters", day.specialEntries, day.specialCut)
        dataSource.add("Registrations", 0, day.registrations)
             
 */

    fun loadCheques(query: DbQuery) {
        query.load("""
                    SELECT
                        competitor.givenName,
                        competitor.familyName,
                        SUM(IF(type = $ITEM_LATE_ENTRY_PAID, cheque, 0)) AS lateEntryCheque,
                        SUM(IF(type = $ITEM_SPECIAL_CLASS, cheque, 0)) AS specialCheque,
                        SUM(IF(type IN ($ITEM_REGISTRATION), cheque, 0)) AS accountCheque,
                        SUM(competitionLedger.cheque) AS cheque
                    FROM
                        competitionLedger
                            LEFT JOIN
                        account ON account.idAccount = competitionLedger.idAccount
                            LEFT JOIN
                        competitor ON competitor.idCompetitor = account.idCompetitor
                    WHERE
                        competitionLedger.idCompetition = ${idCompetition}
                            AND accountingDate = ${date.sqlDate}
                            AND cheque <> 0
                            AND NOT cancelled
                            AND type <> $ITEM_CAMPING
                    GROUP BY competitor.givenName , competitor.familyName
        """)

    }

    fun loadData() {
        this.idCompetition
        val sql = """
            SELECT
                SUM(IF(type = $ITEM_LATE_ENTRY_PAID, cash, 0)) AS lateEntryCash,
                SUM(IF(type = $ITEM_LATE_ENTRY_PAID, cheque, 0)) AS lateEntryCheque,
                SUM(IF(type = $ITEM_SPECIAL_CLASS, cash, 0)) AS specialCash,
                SUM(IF(type = $ITEM_SPECIAL_CLASS, cheque, 0)) AS specialCheque,
                SUM(IF(type IN ($ITEM_REGISTRATION, $ITEM_ON_ACCOUNT), cash, 0)) AS accountCash,
                SUM(IF(type IN ($ITEM_REGISTRATION, $ITEM_ON_ACCOUNT), cheque, 0)) AS accountCheque,
                SUM(IF(type = $ITEM_LATE_ENTRY_PAID AND cash<>0, quantity, 0)) AS paidCreditsCash,
                SUM(IF(type = $ITEM_LATE_ENTRY_PAID AND cheque<>0, quantity, 0)) AS paidCreditsCheque,
                SUM(IF(type = $ITEM_LATE_ENTRY_PAID, quantity, 0)) AS paidCredits,
                SUM(IF(type = $ITEM_SPECIAL_CLASS AND cash<>0, quantity, 0)) AS specialEntriesCash,
                SUM(IF(type = $ITEM_SPECIAL_CLASS AND cheque<>0, quantity, 0)) AS specialEntriesCheque

            FROM
                competitionLedger
            WHERE
                idCompetition = $idCompetition
                    AND accountingDate = ${date.sqlDate}
        """
        var query = DbQuery(sql)
        if (query.first()) {
            lateEntryCash = query.getInt("lateEntryCash")
            lateEntryCheque = query.getInt("lateEntryCheque")
            specialCash = query.getInt("specialCash")
            specialCheque = query.getInt("specialCheque")
            registrationsCash = query.getInt("accountCash")
            registrationsCheque = query.getInt("accountCheque")

            paidCreditsCash = query.getInt("paidCreditsCash")
            paidCreditsCheque = query.getInt("paidCreditsCheque")
            specialEntriesCash = query.getInt("specialEntriesCash")
            specialEntriesCheque = query.getInt("specialEntriesCheque")
        }

        complimentaryRuns = if (date==competition.dateEnd) competition.complimentaryRuns else 0
    }

    fun print(finalize: Boolean, pdf: Boolean=false) {
        if (finalize) {
            dbTransaction{
                locked=true
                post()
                Global.connection.execute("""
                    UPDATE competitionLedger SET dateLocked=${date.sqlDate}
                    WHERE idCompetition = $idCompetition AND accountingDate = ${date.sqlDate}
                """)
                Reports.printEndOfDay(idCompetition, date, pdf=pdf)
            }
        } else {
            Reports.printEndOfDay(idCompetition, date, pdf=pdf)
        }
    }

    companion object {

        private val competitionDay = CompetitionDay()

        val currentDay: CompetitionDay
            get() {
                if (competitionDay.isOffRow || competitionDay.idCompetition != control.idCompetition || competitionDay.date != today) {
                    competitionDay.seek(control.idCompetition, today)
                }
                return competitionDay
            }

        fun reset() {
            competitionDay.release()
        }
    }
}

