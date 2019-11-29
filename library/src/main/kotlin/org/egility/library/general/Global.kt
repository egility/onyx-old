/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.library.general

import org.egility.library.api.Api
import org.egility.library.database.DbConnectionThread
import org.egility.library.database.DbJdbcConnection
import org.egility.library.dbobject.Competition
import org.egility.library.dbobject.control
import java.io.File

enum class DeviceType {
    PC, ACU, TABLET, SIMULATED_ACU
}

object Global {

    var terminating = false

    var dnr = false

    var acuStatus = ""
    var acuStatusCode = "0000"
    var initialized = false

    /*
    2.6.558 - Easter
    2.6.560 - Split Pairs Fix
    2.6.562 - Quick stats now available for each day of show
    2.6.564 - KC Issue with dog - review classes
    2.8.566 - Make Activities more crash safe to avoid lock up loops
    2.8.568 - Bug in AgilityClass -> Ring link
    2.8.570 - GT - resolve issues with dogs not registered on UKA
    2.8.572 - Ability to switch shows (Dartmoor/Blackdown)
    2.8.574 - Fixes for entering split pairs at show
    2.8.576 - Secretary / Rings optimised for KC, Fix for lack of serial on T65 devices
    2.8.578 - Amazon Fire / Json isRoot fixed - causing sys mgr network to crash
    2.8.580 - CSJ UK Open Temp release
    2.8.582 - CSJ UK Open release
    2.8.584 - CSJ UK Open - Fixes to entering course times + Sec's ring status
    2.8.586 - CSJ UK Open - Fixes to entering course times + Sec's ring status
    2.8.588 - CSJ UK Open - Gamblers adjustments
    2.9.590 - T63S Adjustments / KC Strict r/o fixes
    2.9.592 - Secs classes meu added
    2.9.594 - Tweaks for Champ classes
    2.9.596 - Print calling sheets, print personal running orders
    2.9.598 - Small teak to secs/rings to display more meaningful current ring height
    2.9.600 - Tablets automatically un-assign themselves after end of show. PgUp exit from scoreboard
    2.9.602 - Secs review classes removed classes now un-ticked.
    2.9.604 - Introduction of peer networks (databaseHost reset when "Use the System")
    2.9.606 - UKA Accounts entries reports now daily
    2.10.608 - Removed UKA credit checking and account payment. Lookup uses idUka instead of dogCode. New processing for registrations.
    2.10.610 - devices now register idCompetition, sec's classes option now allows r/o's to be edited, TryOut gamblers bonus feature
    2.10.612 - kindle fire button fix
    2.10.614 - Double + Long click logo to update
    2.10.616 - Fix to make tablets talk to correct ACU
    2.12.618 - PA / Radio stuff added (Chippenham 2018)
    2.12.620 - PA adjustment (grey no showing on new tablets)
    2.12.622 - PA refinements
    2.12.624 - Grand Finals - not sure any changes
    2.14.700 - Intellij 2018.3 + gradle
    2.14.706 - Cranbourne gradle test
    2.14.708 - UKA Change handler
    2.14.710 - Import UKA account from Plaza
    2.14.712 - Competitor, Dog & Competition tables simplified
    2.14.714 - UKA intermediate release - AP integration
    2.14.716 - UKA Membership management and improved AP integration (1/2/19)
    
    3.0.0 - Update detection added (8/2/19)
    3.0.2 - Measuring facility (15/2/19)
    3.0.4 - Handler can now be changed by secs and scrime after run (16/2/19)
    3.0.6 - Fixed issue with alternative handler and POTD + tweaks to measuring (16/2/19)
    3.0.8 - Other handlers shown on sec's account menu
    3.0.x - KC Sec Dog lookup by name crashing (3/3/19) 
    3.0.10 - API switched to port 9000 to overcome issue with Amazon Fires (25/3/19) 
    3.0.12 - Fix issue with secs lookup by name + CompetitionLedger now has idCompetitor and idDog for registrations,
             "Update from Plaza" now refreshes screen for members as well as dogs. Registration at show now £6.
    3.0.12b - Fix search by surname for measuring
    3.0.12c - Do not offer dead dogs for measuring
    3.0.14a - Fix performance issue with DbMetaColumn
    3.1.00a - FAB features 
    3.1.00b - signOn file
    3.1.2a - signOn file fix
    3.1.2b - KC/FAB Review Classes split by day
    3.1.2c - Allsorts Classes sorted (6/5/19)
    3.1.2c - Allsorts Classes sorted (6/5/19)
    3.1.4a - Secs Classes page now organised by date (7/5/19)
    3.1.9x - Experimental build with rewritten DbDataset (7/5/19)
    3.2.0a - New logo screen control menu, ACU selection on use system, db connection tries for 90sec before crashing (14/5/19)
    3.2.0b - Just connect if only one acu on network (15/5/19)
    3.2.0c - tabletLog added (15/5/19)
    3.2.0d - connect to nearest if unassigned (15/5/19)
    3.2.1a - WPS option os secs tablet (17/5/19)
    3.2.1b - FAB Gambers (18/5/19)
    3.2.2a - Tablet logging improvements (20/5/19)
    3.3.1a - CSJ Open Changes - groups (7/6/19)
    3.3.1b - Blackdown fix for more than 4 day shows (7/6/19)
    3.3.2a - CSJ Open Changes - gamblers, secs (7/6/19)
    3.3.3a - CSJ Open Changes - withdraw dog option (12/6/19)
    3.3.4a - CSJ Open Changes - Scoreboard bug (12/6/19)
    3.3.5a - CSJ Open Changes - Edit running orders bug (12/6/19)
    3.3.6a - Fix for UKA other change handler (21/6/19)
    3.3.6b - Json optimised to make better use of memory on tablets (22/6/19)
    3.4.1a - Fix connection issue on UKA tablets (24/6/19)
    3.4.2a - Add entry.scrimeTime to work out savings for integrated timers (24/6/19)
    3.4.3a - Wraxall / Crufts teams (24/6/19)
    3.4.4a - Closed for Lunch option
    3.4.4b - Print emergency scrime sheets
    3.4.6a - Scrime (from Paper) facility
    3.4.6b - Competition effective date now calculated, edit r/o team individual names bug
    3.4.6b - Competition effective date now calculated, edit r/o team individual names bug
    3.4.8a - UKA Juniors no longer pay membership fees at shows
    3.4.10a - FAB grade change fix
    3.5.0 - IntelliJ 2019.2
     */

    
    val majorVersion = 3
    val minorVersion = 5
    val build = 0
    val letter = "d"
    val versionNumber = majorVersion * 100000 + minorVersion * 1000 + build
    val version = "$majorVersion.$minorVersion.$build$letter"
    var reassignTabletUsage = false
    var idDevice: Int = 0;

    var isResettingApplication = false
    var endOfDayDate = nullDate

    var runningOrderCopies = 2
    var resultsCopies = 1
    var awardsCopies = 1

    var allEmailsTo = ""
    
    var activityName=""

    var live = false
    var quartzTest = false

    val keyPhrase = "the wrong trousers"

    fun updateNeeded(): Boolean {
        val parts = control.graniteVersion.split(".")
        val needed = if (parts.size == 3) parts[0].toIntDef(0) * 100000 + parts[1].toIntDef(0) * 1000  + parts[2].toIntDef(0) else 0
        return needed>versionNumber

    }

    private var _connection: DbJdbcConnection? = null
    val connection: DbJdbcConnection
        get() {
            val currentThread = Thread.currentThread()
            if (currentThread is DbConnectionThread) {
                return currentThread.connection
            } else if (_connection == null) {
                _connection = DbJdbcConnection(SandstoneMaster.builder)
            }
            return _connection ?: throw Wobbly("No global connection defined")
        }

    var _databaseHost = ""
    var databaseHost: String
        get() {
            if (_databaseHost.isEmpty() && isTablet) {
                while (_databaseHost.isEmpty()) {
                    _databaseHost = Api.getMasterHost(false)
                }
            }
            return _databaseHost
        }
        set(value) {
            debug("global", "set databaseHost=$value")
            _databaseHost = value
        }

    val haveConnection: Boolean
        get() = _databaseHost.isNotEmpty() && _connection?.isConnected ?: false

    var deviceType = DeviceType.PC
    private var pinAccepted = nullDate

    var services: Services = GenericServices()

    val MYSQL_USER = "developer"
    val MYSQL_PASSWORD = "tomato"
    var databasePort = 3340
    var databaseName = "sandstone"
    val homeFolder = "/data/e-gility"
    var imagesFolder = "/data/kotlin/projects/onyx/plaza/public_static/img"

    var alwaysToPdf = false
    var automatedTest = false
    var testMode = false

    var documentsFolder: String = ""
        get() {
            if (field.isEmpty()) {
                val folder = File(homeFolder + "/shows")
                if (!folder.exists()) {
                    folder.mkdirs()
                }
                field = folder.canonicalPath
            }
            return field
        }


    fun documentPath(name: String, extension: String): String {
        File("${Global.homeFolder}/documents").mkdirs()
        return "${Global.homeFolder}/documents/${name}.$extension"
    }

    var testSerial = 1

    fun showDocumentPath(competitionUniqueName: String, name: String, extension: String, canRegenerate: Boolean = true): String {
        if (canRegenerate) {
            File("${Global.homeFolder}/shows/cache/$competitionUniqueName").mkdirs()
            if (Global.testMode) {
                return "${Global.homeFolder}/shows/cache/$competitionUniqueName/${competitionUniqueName}_${"%02d".format(testSerial++)}_${name}.$extension"
            } else {
                return "${Global.homeFolder}/shows/cache/$competitionUniqueName/${competitionUniqueName}_${name}.$extension"
            }
        } else {
            File("${Global.homeFolder}/shows/published/$competitionUniqueName").mkdirs()
            return "${Global.homeFolder}/shows/published/$competitionUniqueName/${name}.$extension"
        }
    }

    fun showDocumentPath(competition: Competition, name: String, extension: String, canRegenerate: Boolean = true): String {
        return showDocumentPath(competition.uniqueName, name, extension, canRegenerate)

    }

    fun showDocumentPath(idCompetition: Int, name: String, extension: String, canRegenerate: Boolean = true): String {
        if (Global.testMode) {
            return showDocumentPath(Competition(idCompetition).uniqueName, "${"%02d".format(testSerial++)}_" + name, extension, canRegenerate)
        } else {
            return showDocumentPath(Competition(idCompetition).uniqueName, name, extension, canRegenerate)
        }
    }

    fun reset() {
        isResettingApplication = false
        databaseHost = ""
        _connection = null
        pinAccepted = nullDate
    }

    val isTablet: Boolean
        get() = deviceType == DeviceType.TABLET

    val isAcu: Boolean
        get() = deviceType == DeviceType.ACU || isSimulatedAcu

    val isSimulatedAcu: Boolean
        get() = deviceType == DeviceType.SIMULATED_ACU

    val isPC: Boolean
        get() = deviceType == DeviceType.PC

}
