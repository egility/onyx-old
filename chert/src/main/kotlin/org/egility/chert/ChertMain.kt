/*
 * Copyright (c) Mike Brickman 2014-2017
 */

/*
 * Copyright (c) Mike Brickman 2014-2016
 */

/*
 * Copyright (c) Mike Brickman 2014-2016
 */

/*
 * Copyright (c) Mike Brickman 2014-2016
 */

package org.egility.chert

import org.egility.library.dbobject.*
import org.egility.library.general.*
import org.egility.linux.tools.*
import org.egility.linux.tools.PlazaAdmin.adjustUkaLevels
import org.egility.linux.tools.PlazaAdmin.checkSWAPFees
import org.egility.linux.tools.PlazaAdmin.checkShowActions
import org.egility.linux.tools.PlazaAdmin.checkUkaRegistrations
import org.egility.linux.tools.PlazaAdmin.sortOutIds
import org.egility.linux.tools.UkaAdmin.clearPendingRegistrations
import org.egility.linux.tools.UkaAdmin.populateJuniorLeague
import java.util.*

/**
 * Created by mbrickman on 19/05/16.
 */


fun main(args: Array<String>) {
    setDebugClasses("SQL")
    NativeServices.initialize(true)

    println("chert version 1.2.0")

    var full = false
    var addDays = 0
    var idUuka = 0
    var command = ""
    var local = false
    var path = "heights.xls"

    try {
        for (arg in args) {
            val parts = arg.split("=")
            val switch = parts[0]
            val value = if (parts.size > 1) parts[1] else ""
            if (switch.startsWith("--")) {
                when (switch) {
                    "--full" -> full = true
                    "--local" -> local = true
                    "--add-days" -> addDays = value.toIntDef()
                    "--idUKA" -> idUuka = value.toIntDef()
                    "--path" -> path = value
                }
            } else {
                command = switch
            }
        }

        Global.databaseHost = if (local) "localhost" else "castor.genesislive.net";

        when (command) {
            "midnight" -> {
                try {
                    adjustUkaLevels()
                    populateJuniorLeague()
                    Starling.processTransactions(today.addDays(-2))
                    clearPendingRegistrations()
                } catch (e: Throwable) {
                    debug("Panic", e.stack)
                    e.printStackTrace()
                }
                sortOutIds()
                checkShowActions()
                //generateAgilityClassDates()
                if (today.dayOfWeek() == 4) { //wednesday
                    checkUkaRegistrations()
                    checkSWAPFees()
                }
                dbExecute("STOP SLAVE")
                dbExecute("RESET SLAVE ALL")

                KcScraper.scrapeShows()
                KcScraper.scrapeClubs()
                KcScraper.processLicences()

            }
            "shows" -> {
                checkShowActions()
            }
            "scrape" -> {
                KcScraper.scrapeShows()
                KcScraper.scrapeClubs()
                KcScraper.processLicences()
            }
            "starling" -> {
                Starling.processTransactions(today.addDays(-7))
            }
            "dequeuePlaza" -> {
                PlazaAdmin.dequeuePlaza()
            }
            "league" -> {
                populateJuniorLeague()
            }
            "test219" -> {
                PlazaAdmin.uploadRingPlan(1299141672)
            }
            "deDuplicate" -> {
                PlazaData.deduplicate()
            }
            "payUka" -> {
                println("payUka")
                checkUkaRegistrations()
            }
            "paySwap" -> {
                println("paySwap")
                checkSWAPFees()
            }
            "ukaProgress" -> {
                dbQuery(
                    """
                        SELECT DISTINCT
                            team.idDog
                        FROM
                            entry
                                JOIN
                            agilityClass USING (idAgilityClass)
                                JOIN
                            team USING (idTeam)
                        WHERE
                            classCode IN (${ClassTemplate.ukaProgressionList})
                                AND progressionPoints > 0
                    """
                ) {
                    if (cursor.rem(100) == 0)
                        println("${cursor + 1} of ${rowCount}")
                    Dog().seek(getInt("idDog")) {
                        //  ukaCalculateGrades(check2019 = true)
                    }
                }
            }
        }
    } catch (e: Throwable) {
        panic(e)
    }
}
