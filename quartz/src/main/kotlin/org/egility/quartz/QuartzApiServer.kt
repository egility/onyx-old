/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.quartz

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpHandler
import com.sun.net.httpserver.HttpServer
import org.egility.library.api.*
import org.egility.library.database.DbConnectionExecutor
import org.egility.library.database.DbQuery
import org.egility.library.database.DbTable
import org.egility.library.dbobject.*
import org.egility.library.dbobject.LedgerItem
import org.egility.library.dbobject.TabletLog
import org.egility.library.general.*
import org.egility.library.general.Global.keyPhrase
import org.egility.linux.reports.*
import org.egility.linux.tools.*
import org.egility.linux.tools.NativeUtils.zipCompetitionResults
import org.egility.linux.tools.PlazaAdmin.MAX_RINGS
import org.egility.linux.tools.PlazaAdmin.dequeuePlaza
import org.egility.linux.tools.PlazaAdmin.exportCruftsTeams
import org.egility.linux.tools.PlazaAdmin.exportRunningOrders
import org.egility.linux.tools.PlazaAdmin.exportShowClasses
import org.egility.linux.tools.PlazaAdmin.exportShowEntries
import org.egility.linux.tools.PlazaAdmin.exportShowEntriesBlank
import org.egility.linux.tools.PlazaAdmin.exportShowTemplate
import org.egility.linux.tools.PlazaAdmin.exportUkOpenGroups
import org.egility.linux.tools.PlazaAdmin.exportUkaFinalsInvited
import org.egility.linux.tools.PlazaAdmin.exportUkaFinalsQualified
import org.egility.linux.tools.PlazaAdmin.importSwapBalances
import org.egility.linux.tools.PlazaAdmin.importUKABalances
import org.egility.linux.tools.PlazaAdmin.importUkaHeights
import org.egility.linux.tools.PlazaAdmin.importWorkbook
import org.egility.linux.tools.PlazaAdmin.swapAccounts
import org.egility.linux.tools.UkaAdmin.exportResults
import org.egility.linux.tools.UkaAdmin.exportResultsAll
import org.egility.linux.tools.UkaAdmin.exportRingBoards
import org.egility.linux.tools.UkaAdmin.exportRingPlan
import org.egility.linux.tools.UkaAdmin.mastersEntries
import org.egility.linux.tools.UkaAdmin.registrations
import java.io.File
import java.net.InetAddress
import java.net.InetSocketAddress
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

internal val defaultColumns = HashMap<String, String>()

internal val PASSWORD_SYSTEM = "de2la5"
internal val PASSWORD_KC = "jigsaw#17"
internal val PASSWORD_UKA = "mayfly.31"

class QuartzApiServer(val httpServer: HttpServer) : HttpHandler {

    val router = ApiRouter()

    init {
        httpServer.createContext("/", this)
        httpServer.executor = DbConnectionExecutor(SandstoneMaster.builder, threadPoolSize = 20)
        httpServer.start()

        router.addUniversalOptions("pretty:boolean&select:string&idAccount:int&idCompetitor:int&access:int&idCompetitorReal:int")

        defaultColumns.put(
            "agilityClass",
            "idAgilityClass, idCompetition, classDate, classCode, className, gradeCodes, heightCode, ringNumber, ringOrder, heightRunningOrder"
        )
        defaultColumns.put(
            "competition", "idCompetition, uniqueName, name, briefName, dateStart, dateEnd, dateOpens, dateCloses, capReached, " +
                    "lateEntryFee, campingRate, campingRateDay, itineraryNote, entryNote, announcement, heightsGenerated, " +
                    "idOrganization, venue, venuePostcode, mapUrl, provisional, private, closed, processed, holdItineraries"
        )
        defaultColumns.put("competitionDay", "date,dayType")
        defaultColumns.put(
            "competitor",
            "idCompetitor, givenName, familyName, streetAddress, town, regionCode, countryCode, postcode, email, phoneMobile, phoneOther, balance, accountStatus, ykc, ykcMember"
        )
        defaultColumns.put(
            "account",
            "idAccount, idCompetitor, code, streetAddress, town, regionCode, countryCode, postcode, htmlAddress"
        )
        defaultColumns.put(
            "dog",
            "idDog, idCompetitor, idCompetitorHandler, code, idUka, idKC, petName, registeredName, dogState, kcRegistered, ukaState"
        )
        defaultColumns.put(
            "entry",
            "idEntry, idAgilityClass, heightCode, entryType, runningOrder, progress, entryFee, heightText"
        )
        defaultColumns.put("team", "-")
        defaultColumns.put("breed", "breedName")
        defaultColumns.put("ledgerAccount", "-")
        defaultColumns.put("ledger", "-")
        defaultColumns.put("voucher", "*")



        defaultColumns.put(
            "emailQueue", "idEmailQueue, idAccount, emailAccount, emailTo, emailFrom, emailCC, subject, sent, status, dateCreated"
        )
        router.get(
            "agilityClass/:idAgilityClass:int/results/pdf/:documentName:string?tournament:boolean&subResultsFlag:int",
            ::getAgilityClassResultsPdf
        )
        router.get(
            "agilityClass/:idAgilityClass:int/runningOrders/pdf/:documentName:string?tournament:boolean&provisional:boolean",
            ::getAgilityClassRunningOrdersPdf
        )

        // mobile app
        router.get("agilityClass/:idAgilityClass:int/results?dog:int", ::getAgilityClassResults)
        router.get("competition/:idCompetition:int/progress/:date:yyyymmdd", ::getCompetitionProgressByDate)
        router.get("competition/:idCompetition:int/days?active:boolean", ::getCompetitionDays)
        router.get("competition/current/days", ::getCurrentCompetitionDays)
        router.get("competition/live", ::getCompetitionLive)

        router.get("competition/:idCompetition:int/voucher/:voucherCode:string", ::getVoucher)


        router.get("competition/:idCompetition:int?uka:boolean&switchboard:boolean", ::getCompetition)
        router.get("competition/:idCompetition:int/stock", ::getCompetitionStock)
        router.get("competition/:idCompetition:int/results", ::getCompetitionResultsByDate)
        router.get("competition/:idCompetition:int/ringPlan/:date:yyyymmdd?summary:boolean", ::getCompetitionRingPlan)
        router.get("competition/:idCompetition:int/ringPlanDynamic/:date:yyyymmdd", ::getCompetitionRingPlanDynamic)
        router.get("competition/:idCompetition:int/signOn", ::getCompetitionSignon)
        router.get("competition/:idCompetition:int/signal", ::getCompetitionSignal)
        router.get("competition/:idCompetition:int/tablet", ::getCompetitionTablet)

        router.get("competition/:idCompetition:int/map", ::getCompetitionEntryMap)
        router.get("competition/:idCompetition:int/entries", ::getCompetitionEntries)
        router.get("competition/:idCompetition:int/deleteUnpaid", ::getDeleteUnpaidEntries)
        router.get("competition/:idCompetition:int/camping?waiting:boolean", ::getCompetitionCamping)
        router.get("competition/:idCompetition:int/entryStats", ::getCompetitionEntryStats)
        router.get("competition/:idCompetition:int/helpers", ::getCompetitionHelpers)
        router.get("competition/:idCompetition:int/cruftsTeams?check:boolean", ::getCompetitionCruftsTeams)

        router.get("competition/active", ::getCompetitionActive)
        router.get("competition/active/map", ::getDiaryMap)
        router.get("competition/swap", ::getCompetitionSwap)
        router.get("competition/open?competitor:int&paper:boolean", ::getCompetitionOpen)
        router.get("competition/results?year:int", ::getCompetitionResults)

        router.get(
            "competition/:idCompetition:int/report/:keyword:string/:token:string/:documentName:string",
            ::getCompetitionReport
        )
        router.get(
            "competition/:idCompetition:int/endOfDay/:date:yyyymmdd/:token:string/:documentName:string",
            ::getCompetitionEndOfDay
        )
        router.get(
            "competition/:idCompetition:int/results/zip/:token:string/:documentName:string",
            ::getCompetitionResultsZip
        )
        router.get(
            "competition/:idCompetition:int/template/:token:string/:documentName:string?next_year:boolean",
            ::getCompetitionTemplate
        )
        router.get("competition/:idCompetition:int/ukaData/:documentName:string", ::getUkaShowData)
        router.get("competition/:idCompetition:int/kcData/:documentName:string", ::getKcShowData)
        router.get("competition/:idCompetition:int/fabData/:documentName:string", ::getFabShowData)
        router.get("competition/:idCompetition:int/ukOpenData/:documentName:string", ::getUkOpenShowData)

        router.get("competition/:idCompetition:int/crufts_teams_sheet/:documentName:string", ::getKcCruftsTeams)


        router.get("competition/:idCompetition:int/ukOpenGroups/:documentName:string", ::getUkOpenGroups)
        router.get("competition/:idCompetition:int/runningOrderData/:documentName:string", ::getRunningOrderData)

        router.get(
            "competition/:idCompetition:int/document/:name:string/:documentName:string",
            ::getCompetitionDocument
        )

        router.get("uka/members/:documentName:string", ::getUkaMembers)
        router.get("uka/heights/:documentName:string", ::getUkaHeights)
        router.get("uka/masters_entries/:documentName:string", ::getUkaMastersEntries)
        router.get("uka/registrations/:documentName:string", ::getUkaRegistrations)
        router.get("uka/finals_qualified/:documentName:string", ::getUkaQualified)
        router.get("uka/finals_invited/:documentName:string", ::getUkaInvited)



        router.get("uka/junior_league", ::getUkaJuniorLeague)
        router.get("uka/junior_league_data/:documentName:string", ::getUkaJuniorLeagueData)

        router.get("swap/accounts/:documentName:string", ::getSwapAccounts)


        router.get("null", ::getNull)
        router.get("ping", ::getPing)


        router.get(
            "account/list" +
                    "?familyName:string&idKc:string&postCode:string&email:string&phone:string" +
                    "&petName:string&registeredName:string&idUkaDog:int&idUkaMember:int&accountCode:string&competitorCode:string",
            ::getAccountList
        )
        router.get("account/:idAccount:int", ::getAccount)
        router.get("account/:idAccount:int/all", ::getAccountAll)
        router.get("account/:idAccount:int/uka", ::getAccountUka)
        router.get("account/:idAccount:int/ukaCheckout", ::getAccountUkaCheckout)

        router.get("account/:idAccount:int/dogs", ::getAccountDogs)
        router.get("account/:idAccount:int/dogs/new", ::getAccountDogsAdd)
        router.get("account/:idAccount:int/competitors", ::getAccountCompetitors)
        router.get("account/:idAccount:int/competitors/new", ::getAccountCompetitorsAdd)
        router.get("account/:idAccount:int/handler/new", ::getAccountHandlerAdd)
        router.get("account/:idAccount:int/ledger", ::getAccountLedger)
        router.get("account/:idAccount:int/refund", ::getAccountRefund)
        router.get("account/:idAccount:int/refund/confirmed/:token:string", ::getAccountRefundConfirmed)
        router.get("account/:idAccount:int/entries", ::getAccountEntries)
        router.get("account/:idAccount:int/emails/list", ::getAccountEmails)
        router.get("account/:idAccount:int/emails/:idEmailQueue:int", ::getAccountEmail)

        router.get("account/:idAccount:int/do/:function:string", ::getAccountFunction)


        router.get(
            "account/:idAccount:int/competition/:idCompetition:int/reset?paper:boolean&test:boolean",
            ::getAccountCompetitionReset
        )
        router.get(
            "account/:idAccount:int/competition/:idCompetition:int/dogs/:idDog:int",
            ::getAccountCompetitionDog
        )
        router.get("account/:idAccount:int/competition/:idCompetition:int/dogs", ::getAccountCompetitionDogs)
        router.get(
            "account/:idAccount:int/competition/:idCompetition:int/competitors",
            ::getAccountCompetitionCompetitors
        )
        router.get("account/:idAccount:int/competition/:idCompetition:int/entry", ::getAccountCompetitionEntry)
        router.get(
            "account/:idAccount:int/competition/:idCompetition:int/supplementary",
            ::getAccountCompetitionSupplementary
        )
        router.get(
            "account/:idAccount:int/competition/:idCompetition:int/checkout",
            ::getAccountCompetitionCheckout
        )
        router.get(
            "account/:idAccount:int/competition/:idCompetition:int/transaction",
            ::getAccountCompetitionTransaction
        )
        router.get("account/:idAccount:int/competition/:idCompetition:int/cancel", ::getAccountCompetitionCancel)

        router.get(
            "account/:idAccount:int/competition/:idCompetition:int/report/:keyword:string/:token:string/:documentName:string",
            ::getAccountCompetitionReport
        )

        router.get("account/:idAccount:int/competition/:idCompetition:int/transfer_camping", ::getAccountCompetitionTransferCamping)
        router.get("account/:idAccount:int/competition/:idCompetition:int", ::getAccountCompetition)

        router.get(
            "competitor/:idCompetitor:int?unVerifiedEmail:boolean&set_password:boolean&registrationComplete:boolean",
            ::getCompetitor
        )

        router.get("competitor/add", ::getCompetitorAdd)

        router.get("competitor/:idCompetitor:int/dogs?registering:boolean", ::getCompetitorDogs)
        router.get("competitor/:idCompetitor:int/dogs/add", ::getCompetitorDogsAdd)
        router.get("competitor/:idCompetitor:int/split", ::getCompetitorSplit)

        router.get("competitor/:idCompetitor:int/email", ::getCompetitorEmail)
        router.get("competitor/:idCompetitor:int/paymentCards/add", ::getCompetitorPaymentCardAdd)
        router.get("competitor/:idCompetitor:int/ukaLedger", ::getCompetitorUkaLedger)

        router.get("competitor/:altIdCompetitor:int/session", ::getCompetitorSession)


        router.get("competitor/codeInformation/:code:string", ::getCompetitorInformation)


        router.get("competitor/authenticate", ::getCompetitorAuthenticate)
        router.get("competitor/revert", ::getCompetitorRevert)
        router.get("competitor/register", ::getCompetitorRegister)
        router.get(
            "competitor/session/:token:string?authenticated:boolean&registrationComplete:boolean",
            ::getCompetitorSessionFromToken
        )

        router.get("competitor/:idCompetitor:int/competitions", ::getCompetitorCompetition)

        router.get("dog/:idDog:int?share:boolean", ::getDog)
        router.get("dog/codeInformation/:code:int", ::getDogInformation)
        router.get("dog/:idDog:int/results", ::getDogResults)
        router.get("dog/:idDog:int/session", ::getDogSession)
        router.get("dog/:idDog:int/ukaProgress", ::getDogUkaProgress)
        router.get("dog/:idDog:int/kc_grade_review", ::getDogKcGradeReview)
        router.get("dog/:idDog:int/kc_grade_change", ::getDogKcGradeChange)

        router.get("payment_requests", ::getPaymentRequests)


        router.get("breed/list", ::getBreedList)
        router.get("grade/list/:organization:string", ::getGradeList)
        router.get("grade/list", ::getGradeList)
        router.get("height/list/:organization:string?casual:boolean", ::getHeightList)
        router.get("height/list", ::getHeightList)
        router.get("country/list", ::getCountryList)
        router.get("region/list", ::getRegionList)
        router.get("dogState/list", ::getDogStateList)
        router.get("classCode/list", ::getClassCodeList)
        router.get("voucherType/list", ::getVoucherTypeList)

        router.get("ledgerAccount/overview?idCompetition:int", ::getLedgerAccountOverview)
        router.get("ledgerAccount/:idLedgerAccount:int?idCompetition:int", ::getLedgerAccountAccount)
        router.get("unallocatedReceipts", ::getLedgerUnallocatedReceipts)

        router.put("resource/:resource:string", ::putResource)
        router.put("competition/:idCompetition:int/upload/document/:name:string", ::putCompetitionDocument)
        router.put("competition/:idCompetition:int/upload/classes", ::putCompetitionClasses)
        router.put("competition/:idCompetition:int/upload/awards", ::putCompetitionAwards)

        router.put("uka/upload/measurements", ::putUkaMeasurements)
        router.put("uka/upload/transfers", ::putUkaTransfers)
        router.put("swap/upload/transfers", ::putSwapTransfers)
        router.put("spreadsheet/upload", ::putSpreadsheet)
        router.put("authenticate", ::putAuthenticate)

        router.get("request_reset_password", ::getRequestResetPassword)
        router.get("reset_password/:token:string", ::getResetPassword)

        router.get("replication/fix", ::getReplicationFix)
        router.get("replication?explain:boolean", ::getReplication)

        router.get("acu/list", ::getAcuList)
        router.get("acu/:id:int/diagnostics", ::getAcuDiagnostics)
        router.get("acu/:id:int", ::getAcu)

        router.get("team/:idTeam:int", ::getTeam)

        router.get("tablet/list", ::getTabletList)
        router.get("tablet/inUse?uka:boolean", ::getTabletInUse)
        router.get("tablet/:idDevice:int", ::getTabletLog)
        router.get("dongle/list", ::getDongleList)
        router.get("signOn", ::getSignon)


        router.get("competition/:idCompetition:int/stock_out", ::getStockMovement)
        router.get("competition/:idCompetition:int/stock_out/confirm", ::getStockMovementConfirm)

        router.get("stock/movement/confirm", ::getStockMovementConfirm)
        router.get("stock/movement", ::getStockMovement)
        router.get("stock/list", ::getStockList)

        router.post("starling", ::postStarling)
        router.post("v1.0/starling", ::postStarling)


        router.get("entity/list?priority:boolean", ::getEntityList)
        router.get("entity/new", ::getEntityAdd)
        router.get("entity/:idEntity:int", ::getEntity)
        router.get("entityOfficial/:idEntityOfficial:int", ::getEntityOfficial)
        router.get("entity/:idEntity:int/official/new", ::getEntityOfficialAdd)

        router.get("kc_show/list", ::getKcShowList)
        router.get("kc_show/map", ::getKcShowMap)
        router.get("agilitynet/map?target:boolean", ::getAgilitynetShowMap)

        router.get("account/map?postcodes:string", ::getAccountMap)



        router.sort()
        // router.list()
    }

    val executor: DbConnectionExecutor
        get() = httpServer.executor as DbConnectionExecutor

    override fun handle(httpExchange: HttpExchange) {
        try {
            val apiExchange = ApiExchange(httpExchange, quartz = true)
            debug("API", "Thread Count = ${executor.poolSize} (${executor.largestPoolSize} max)")
            apiExchange.respondUsingRouter(router)
        } catch (e: Throwable) {
            debug("API", "Error: ${e.message}")
        }
    }

    companion object {
        var apiHostname: String = "localhost:8000"

        val uri: String
            get() {
                val protocol = if (apiHostname.contains("localhost")) "http" else "https"
                return "$protocol://$apiHostname"
            }

        fun initialize(ip: String, port: Int) {
            val address = InetSocketAddress(InetAddress.getByName(ip), port)
            val httpServer = HttpServer.create(address, 0)
            QuartzApiServer(httpServer)

        }
    }

}

internal fun loadFromTable(
    jsonObject: JsonNode,
    table: DbTable<*>,
    request: ApiRequest,
    extraSelectedColumns: String = ""
) {

    val selected = request.query["select"].asString
    val selectedList =
        if (selected == "*" || extraSelectedColumns == "*") "*" else selected.commaAppend(extraSelectedColumns)
    val selectedColumns = columnSelection(table.tableName, selectedList)

    fun setConditional(selector: String, value: Any) {
        if (selectedColumns == "*" || selectedColumns.isEmpty() || selectedColumns.contains("+" + selector) || selectedColumns.contains(
                selector
            )
        ) {
            jsonObject[selector] = value
        }
    }

    jsonObject.loadFromDataset(table, selectedColumns, handled = { column ->
        var isHandled = false
        when (table) {
            is Dog -> {
                if (column.eq("registeredName")) {
                    jsonObject[column] = table.registeredName
                    isHandled = true
                }
            }
        }

        isHandled
    })
    when (table) {
        is Dog -> {
            setConditional("code", table.code)
            setConditional("idCompetitorHandler", table.idCompetitorHandler)

            setConditional("kcHeightText", Height.getHeightName(table.kcHeightCode))
            setConditional("kcGradeText", Grade.getGradeName(table.kcGradeCode))
            setConditional("ukaHeightText", Height.getHeightName(table.ukaHeightCode))
            setConditional("ukaPerformanceText", Grade.getGradeName(table.ukaPerformanceLevel))
            setConditional("ukaSteeplechaseText", Grade.getGradeName(table.ukaSteeplechaseLevel))

            setConditional("kcRegistered", table.kcRegistered)
            setConditional("kcHeightCode", table.kcHeightCode)
            setConditional("kcGradeCode", table.kcGradeCode)
            setConditional("kcWarrantLevel", table.kcWarrantLevel)
            setConditional("kcChampion", table.kcChampion)
            setConditional("kcAgilityChampion", table.kcAgilityChampion)
            setConditional("kcShowCertificateMerit", table.kcShowCertificateMerit)
            setConditional("kcJuniorWarrant", table.kcJuniorWarrant)

            setConditional("ukaState", table.ukaState)
            setConditional("ukaMeasuredHeight", table.ukaMeasuredHeight)
            setConditional("ukaMeasuredHeightText", table.ukaMeasuredHeightText)
            setConditional("ukaHeightCode", table.ukaHeightCode)
            setConditional("ukaPerformanceLevel", table.ukaPerformanceLevel)
            setConditional("ukaSteeplechaseLevel", table.ukaSteeplechaseLevel)
            setConditional("ukaSteeplechaseLevel", table.ukaSteeplechaseLevel)
            setConditional("ukaQualifications", table.ukaQualifications)

            setConditional("ukaPerformance", !table.ukaNotPerformance)
            setConditional("ukaSteeplechase", !table.ukaNotSteeplechase)
            setConditional("ukaCasual", table.ukaCasual)
            setConditional("ukaNursery", table.ukaNursery)
            setConditional("ukaJunior", table.ukaJunior)
            setConditional("ukaHeightCodePerformance", table.ukaHeightCodePerformance)
            setConditional("ukaHeightCodeSteeplechase", table.ukaHeightCodeSteeplechase)
            setConditional("ukaHeightCodeNursery", table.ukaHeightCodeNursery)
            setConditional("ukaHeightCodeCasual", table.ukaHeightCodeCasual)
            setConditional("ukaEntryLevel", table.ukaEntryLevel)
            setConditional("ukaChampEquiv", table.ukaChampEquiv)
            setConditional("ukaBarred", table.ukaBarred)
            setConditional("ukaBarredReason", table.ukaBarredReason)

            setConditional("fabHeightCode", table.fabHeightCode)
            setConditional("fabGradeAgility", table.fabGradeAgility)
            setConditional("fabGradeJumping", table.fabGradeJumping)
            setConditional("fabGradeSteeplechase", table.fabGradeSteeplechase)

            setConditional("fabAgility", table.fabNotAgility)
            setConditional("fabJumping", table.fabNotJumping)
            setConditional("fabSteeplechase", table.fabNotSteeplechase)
            setConditional("fabGrandPrix", table.fabGrandPrix)
            setConditional("fabRR", table.fabAllsorts)
            setConditional("fabIfcs", table.fabIfcs)

            setConditional("ownerType", table.ownerType)
            setConditional("ownerName", table.ownerName)
            setConditional("ownerAddress", table.ownerAddress.asMultiLine)
        }
        is Entry -> {
            setConditional("scoreText", table.scoreTextNFC)
            setConditional("timeText", table.timeText)
            setConditional("heightName", Height.getHeightName(table.heightCode))
            setConditional("isPlaceable", table.isPlaceable)
            setConditional("teamDescription", table.teamDescription)
            setConditional("heightText", table.jumpHeightText)
        }
        is AgilityClass -> {
            setConditional("isScoreBasedGame", table.isScoreBasedGame)
            setConditional("presented", table.presented)
        }
        is Account -> {
            setConditional("htmlAddress", table.fullAddress.replace("\n", "<br>"))
            setConditional("code", table.code)
        }
        is Competition -> {
            setConditional("mapUrl", "https://maps.google.com/maps?q=${table.venuePostcode.replace(" ", "+")}")
            setConditional("briefName", table.briefName)
            setConditional("campingRate", table.campingRate)
            setConditional("campingRateDay", table.campingRateDay)
            setConditional("itineraryNote", table.itineraryNote)
            setConditional("entryNote", table.entryNote)
        }
        is Competitor -> {
            setConditional("givenName", table.givenName)
            setConditional("familyName", table.familyName)
            setConditional("ykc", table.ykc)
            setConditional("ykcMember", table.ykcMember)
        }
        is Voucher -> {
            setConditional("memberRates", table.memberRates)
            setConditional("allRunsFree", table.allRunsFree)
            setConditional("allCampingFree", table.allCampingFree)
            setConditional("freeRuns", table.freeRuns)
            setConditional("campingCredit", table.campingCredit)
            setConditional("campingNightsFree", table.campingNightsFree)
            setConditional("generalCredit", table.generalCredit)
            setConditional("campingPriority", table.campingPriority)
            setConditional("ringPartyName", table.ringPartyName)
        }
    }
}

internal fun saveToTable(node: JsonNode, table: DbTable<*>, bindings: ArrayList<String>) {

    fun test(name: String): Boolean {
        return node.has(name) && bindings.contains(name)
    }

    fun testDate(name: String): Boolean {
        return node.has(name) && bindings.contains(name) && node[name].isDate
    }

    if (table.found()) {
        when (table) {
            is Dog -> {
                if (test("kcHeightCode")) table.kcHeightCode = node["kcHeightCode"].asString
                if (test("kcGradeCode")) table.kcGradeCode = node["kcGradeCode"].asString
                if (test("kcWarrantLevel")) table.kcWarrantLevel = node["kcWarrantLevel"].asInt
                if (test("kcChampion")) table.kcChampion = node["kcChampion"].asBoolean
                if (test("kcAgilityChampion")) table.kcAgilityChampion = node["kcAgilityChampion"].asBoolean
                if (test("kcShowCertificateMerit")) table.kcShowCertificateMerit =
                    node["kcShowCertificateMerit"].asBoolean
                if (test("kcJuniorWarrant")) table.kcJuniorWarrant = node["kcJuniorWarrant"].asBoolean
                if (test("kcObedienceWarrant")) table.kcObedienceWarrant = node["kcObedienceWarrant"].asBoolean

                if (test("ukaHeightCode")) table.ukaHeightCode = node["ukaHeightCode"].asString
                if (test("ukaPerformanceLevel")) table.ukaPerformanceLevel = node["ukaPerformanceLevel"].asString
                if (test("ukaSteeplechaseLevel")) table.ukaSteeplechaseLevel = node["ukaSteeplechaseLevel"].asString
                if (test("ukaQualifications")) table.ukaQualifications = node["ukaQualifications"].asString

                if (test("ukaPerformance")) table.ukaNotPerformance = !node["ukaPerformance"].asBoolean
                if (test("ukaSteeplechase")) table.ukaNotSteeplechase = !node["ukaSteeplechase"].asBoolean
                if (test("ukaCasual")) table.ukaCasual = node["ukaCasual"].asBoolean
                if (test("ukaNursery")) table.ukaNursery = node["ukaNursery"].asBoolean
                if (test("ukaJunior")) table.ukaJunior = node["ukaJunior"].asBoolean


                if (test("ukaHeightCodePerformance")) table.ukaHeightCodePerformance =
                    node["ukaHeightCodePerformance"].asString
                if (test("ukaHeightCodeSteeplechase")) table.ukaHeightCodeSteeplechase =
                    node["ukaHeightCodeSteeplechase"].asString
                if (test("ukaHeightCodeNursery")) table.ukaHeightCodeNursery = node["ukaHeightCodeNursery"].asString
                if (test("ukaHeightCodeCasual")) table.ukaHeightCodeCasual = node["ukaHeightCodeCasual"].asString

                if (test("ukaEntryLevel")) table.ukaEntryLevel = node["ukaEntryLevel"].asString
                if (test("ukaChampEquiv")) table.ukaChampEquiv = node["ukaChampEquiv"].asBoolean
                if (test("ukaBarred")) table.ukaBarred = node["ukaBarred"].asBoolean
                if (test("ukaBarredReason")) table.ukaBarredReason = node["ukaBarredReason"].asString

                if (test("fabHeightCode")) table.fabHeightCode = node["fabHeightCode"].asString
                if (test("fabGradeAgility")) table.fabGradeAgility = node["fabGradeAgility"].asString
                if (test("fabGradeJumping")) table.fabGradeJumping = node["fabGradeJumping"].asString
                if (test("fabGradeSteeplechase")) table.fabGradeSteeplechase = node["fabGradeSteeplechase"].asString

                if (test("fabAgility")) table.fabNotAgility = node["fabAgility"].asBoolean
                if (test("fabJumping")) table.fabNotJumping = node["fabJumping"].asBoolean
                if (test("fabSteeplechase")) table.fabNotSteeplechase = node["fabSteeplechase"].asBoolean
                if (test("fabGrandPrix")) table.fabGrandPrix = node["fabGrandPrix"].asBoolean
                if (test("fabRR")) table.fabAllsorts = node["fabRR"].asBoolean
                if (test("fabIfcs")) table.fabIfcs = node["fabIfcs"].asBoolean

                if (test("ownerType")) table.ownerType = node["ownerType"].asInt
                if (test("ownerName")) table.ownerName = node["ownerName"].asString
                if (test("ownerAddress")) table.ownerAddress = node["ownerAddress"].asString.replace("\n", ", ")

                when (node["kcGradeCode"].asString) {
                    "KC02" -> if (testDate("kcGrade2")) table.kcGrade2 = node["kcGrade2"].asDate
                    "KC03" -> if (testDate("kcGrade3")) table.kcGrade3 = node["kcGrade3"].asDate
                    "KC04" -> if (testDate("kcGrade4")) table.kcGrade4 = node["kcGrade4"].asDate
                    "KC05" -> if (testDate("kcGrade5")) table.kcGrade5 = node["kcGrade5"].asDate
                    "KC06" -> if (testDate("kcGrade6")) table.kcGrade6 = node["kcGrade6"].asDate
                    "KC07" -> if (testDate("kcGrade7")) table.kcGrade7 = node["kcGrade7"].asDate
                }

                for (i in 0..4) {
                    if (test("kcChampWins.$i.date")) table.kcChampWins["$i.date"] = node["kcChampWins.$i.date"].asDate
                    if (test("kcChampWins.$i.show")) table.kcChampWins["$i.show"] = node["kcChampWins.$i.show"].asString
                    if (test("kcChampWins.$i.class")) table.kcChampWins["$i.class"] =
                        node["kcChampWins.$i.class"].asString
                }


            }
            is Competitor -> {
                if (test("ykc")) table.ykc = node["ykc"].asString
            }
            is Competition -> {
                if (test("bankAccountName")) table.bankAccountName = node["bankAccountName"].asString
                if (test("bankAccountSort")) table.bankAccountSort = node["bankAccountSort"].asString
                if (test("bankAccountNumber")) table.bankAccountNumber = node["bankAccountNumber"].asString
                if (test("itineraryNote")) table.itineraryNote = node["itineraryNote"].asString
                if (test("entryNote")) table.entryNote = node["entryNote"].asString
                if (test("printCostPound")) table.printCostPound = node["printCostPound"].asString
                if (test("printQuantity")) table.printQuantity = node["printQuantity"].asInt
                if (test("processingFeeSwap")) table.processingFeeSwap = node["processingFeeSwap"].asInt
                if (test("campingReleased")) table.campingReleased = node["campingReleased"].asInt
            }
            is Team -> {
                if (test("teamName")) table.teamName = node["teamName"].asString
                if (test("clubName")) table.clubName = node["clubName"].asString
            }
        }
        node.saveToTable(table) { column, jsonNode ->
            var isHandled = false
            when (table) {
                is Dog -> {
                    if (column.eq("registeredName")) {
                        table.registeredName = jsonNode.asString
                        isHandled = true
                    }
                }
            }
            isHandled
        }
    }
}

internal fun error(response: Json, errorCode: Int = 0, errorMessage: String = ""): Json {
    response["control.error"] = errorCode
    response["control.comment"] = errorMessage
    return response
}


internal fun createResponse(request: ApiRequest? = null, resourceKind: String = "", vararg keyPairs: String): Json {
    val response = Json()
    if (resourceKind.isNotEmpty()) {
        val resource = Json()
        resource["kind"] = resourceKind
        for (keyPair in keyPairs) {
            val key = keyPair.substringBefore("=").trim()
            val value = keyPair.substringAfter("=", "0").trim().toIntDef(0)
            resource[key] = value
        }
        val encryptedResource = resource.toJson().encrypt(keyPhrase)
        response["control.resource"] = "$encryptedResource"
    }
    if (request != null) {
        getControl(request, response)
    }
    error(response, 0, "")
    return response
}

internal fun createResourceResponse(): Json {
    return createResponse()
}

internal fun update(
    response: Json,
    table: DbTable<*>,
    node: JsonNode,
    idName: String,
    id: Int,
    bindings: ArrayList<String> = ArrayList<String>()
) {
    if (id <= 0) {
        table.append()
        saveToTable(node, table, bindings)
        response["control.data"][idName] = table.getInt(idName)
    } else {
        if (table.isOffRow || table.getInt(idName) != id) {
            table.find(idName, id)
        }
        if (table.found()) {
            saveToTable(node, table, bindings)
        }
    }
}

internal fun columnSelection(label: String, select: String): String {
    val tableDefaults = defaultColumns[label] ?: ""
    if (select == "*" || select.isEmpty() && tableDefaults.isEmpty()) {
        return "*"
    }
    val selection =
        if (select.isEmpty() || select.contains('+') || select.contains('-')) ArrayList<String>(tableDefaults.split(",")) else ArrayList<String>()
    for (selectedColumn in select.split(",")) {
        if (selectedColumn.startsWith("-")) {
            selection.remove(selectedColumn.substring(1))
        } else if (selectedColumn.startsWith("+")) {
            selection.add(selectedColumn.substring(1))
        } else {
            selection.add(selectedColumn)
        }
    }
    val result = StringBuilder("")
    for (column in selection) {
        result.csvAppend(column)
    }
    return result.toString()
}

data class LastUpdate(var checked: Date, var time: Date)

var lastUpdates = HashMap<Int, LastUpdate>()

fun accessCondition(request: ApiRequest): String {
    return when (request.query["access"].asInt) {
        1 -> "AND NOT competition.idOrganization IN (2, 3)"
        2 -> "AND competition.idOrganization IN (2, 3)"
        else -> ""
    }
}

// ************************ get functions ******************************

internal fun getAgilityClassResultsPdf(request: ApiRequest): Json {
    val results = createResponse(request)
    val idAgilityClass = request.params["idAgilityClass"].asInt
    val tournament = request.query["tournament"].asBoolean
    val subResultsFlag = request.query["subResultsFlag"].asInt

    var agilityClass = AgilityClass()
    if (agilityClass.find(idAgilityClass)) {
        results["content"] = "application/pdf"
        results["path"] = ResultsReport.generate(
            idAgilityClass = idAgilityClass,
            tournament = tournament,
            pdf = true,
            regenerate = tournament,
            subResultsFlag = subResultsFlag
        )
    }
    return results
}

internal fun getAgilityClassRunningOrdersPdf(request: ApiRequest): Json {
    val runningOrders = createResponse(request)
    val idAgilityClass = request.params["idAgilityClass"].asInt
    val tournament = request.query["tournament"].asBoolean
    val provisional = request.query["provisional"].asBoolean
    var agilityClass = AgilityClass()
    if (agilityClass.find(idAgilityClass)) {
        runningOrders["content"] = "application/pdf"
        runningOrders["path"] = RunningOrderReport.generate(
            idAgilityClass = idAgilityClass,
            tournament = tournament,
            provisional = provisional,
            pdf = true,
            regenerate = tournament
        )
    }
    return runningOrders
}

internal fun getCompetitionReport(request: ApiRequest): Json {
    val token = decryptJson(request.params["token"].asString)
    val idAccount = token["idAccount"].asInt
    val idCompetitorReal = token["idCompetitorReal"].asInt
    val idCompetition = request.params["idCompetition"].asInt
    val isOfficial = CompetitionOfficial.isOfficial(idCompetition, idCompetitorReal)
    val competition = Competition(idCompetition)

    val competitor = Competitor(idCompetitorReal)
    if (!competitor.find(idCompetitorReal) || !competitor.ukaSuperUser && !competitor.plazaSuperUser && !isOfficial) {
        return error(createResponse(), 99, "Unauthorised Access")
    }

    val keyword = request.params["keyword"].asString
    val results = createResponse(request)
    results["content"] = "application/pdf"
    when (keyword) {
        "results" -> {
            results["content"] = "application/vnd.ms-excel"
            results["path"] = exportResults(idCompetition)
        }
        "results_all" -> {
            results["content"] = "application/vnd.ms-excel"
            results["path"] = exportResultsAll(idCompetition)
        }
        "classes" -> {
            results["content"] = "application/vnd.ms-excel"
            results["path"] =
                exportShowClasses(idCompetition, if (competition.ringsNeeded > 0) competition.ringsNeeded else MAX_RINGS)
        }
        "entriesBlank" -> {
            results["content"] = "application/vnd.ms-excel"
            results["path"] = exportShowEntriesBlank(idCompetition)
        }
        "entries" -> {
            results["content"] = "application/vnd.ms-excel"
            results["path"] = exportShowEntries(idCompetition)
        }
        "runningOrders" -> {
            results["content"] = "application/vnd.ms-excel"
            results["path"] = exportRunningOrders(idCompetition)
        }
        "ring_plan_provisional" -> {
            if (Competition(idCompetition).isUka) {
                results["content"] = "application/vnd.msword"
                results["path"] = exportRingPlan(idCompetition)
            } else {
                results["content"] = "application/pdf"
                results["path"] = KcAccountPaperwork(idCompetition).ringPlan()
            }
        }
        "ring_boards" -> {
            if (Competition(idCompetition).isUka) {
                results["content"] = "application/vnd.msword"
                results["path"] = exportRingBoards(idCompetition)
            } else {
                results["content"] = "application/pdf"
                results["path"] = KcAccountPaperwork(idCompetition).ringBoards()
            }
        }
        "entry_forms" -> {
            if (Competition(idCompetition).isKc) {
                results["content"] = "application/pdf"
                results["path"] = KcAccountPaperwork(idCompetition).kcEntryForms()
            }
        }
        "account_entry_form" -> {
            if (Competition(idCompetition).isKc) {
                val baseFile = "entry_form_$idAccount"
                results["content"] = "application/pdf"
                results["path"] = KcAccountPaperwork(idCompetition).kcEntryForms(idAccount, baseFile)
            }
        }
        "kc_team_sheets" -> {
            results["content"] = "application/pdf"
            results["path"] = KcAccountPaperwork(idCompetition).kcTeamSheets()
        }
        "picking_list" -> {
            results["content"] = "application/pdf"
            results["path"] = KcAccountPaperwork(idCompetition).pickingList()
        }
        "delivery_note" -> {
            results["content"] = "application/pdf"
            results["path"] = KcAccountPaperwork(idCompetition).pickingList(deliveryNote = true)
        }
        "awards" -> {
            results["content"] = "application/vnd.ms-excel"
            results["path"] = PlazaAdmin.exportPlaceSheet(idCompetition)
        }
        "mailout_torbay" -> {
            results["content"] = "application/pdf"
            results["path"] = KcAccountPaperwork(idCompetition).export(markFirst = true)
        }
        "mailout" -> {
            results["content"] = "application/pdf"
            results["path"] = KcAccountPaperwork(idCompetition).export(markFirst = false)
        }
        ChequesReport.keyword -> results["path"] = ChequesReport.generate(idCompetition, pdf = true)
        LateEntryCreditsReport.keyword -> results["path"] = LateEntryCreditsReport.generate(idCompetition, pdf = true)
        LateEntryFreeReport.keyword -> results["path"] = LateEntryFreeReport.generate(idCompetition, pdf = true)
        LateEntrySpecialReport.keyword -> results["path"] = LateEntrySpecialReport.generate(idCompetition, pdf = true)
        AccountPaymentReport.keyword -> results["path"] = AccountPaymentReport.generate(idCompetition, pdf = true)
        ComplimentaryCreditsUsedReport.keyword -> results["path"] =
            ComplimentaryCreditsUsedReport.generate(idCompetition, pdf = true)
        RingCardReport.keyword -> results["path"] = RingCardReport.generate(idCompetition, pdf = true)
        CallingListReport.keyword -> results["path"] =
            CallingListReport.generate(idCompetition = idCompetition, pdf = true)
        EmergencyScoreReport.keyword -> results["path"] =
            EmergencyScoreReport.generate(idCompetition = idCompetition, pdf = true)
        CampingListReport.keyword -> results["path"] = CampingListReport.generate(idCompetition, pdf = true)
        AddressLabelsReport.keyword -> results["path"] = AddressLabelsReport.generate(idCompetition, pdf = true)
        AwardLabelsReport.keyword -> results["path"] = AwardLabelsReport.generate(idCompetition, pdf = true)
        ScrimeSheetReport.keyword -> results["path"] =
            ScrimeSheetReport.generate(idCompetition, formal = competition.isKc, pdf = true)
        PaperScoreReport.keyword -> results["path"] =
            PaperScoreReport.generate(idCompetition, formal = competition.isKc, pdf = true)
        PaperPlaceReport.keyword -> results["path"] =
            PaperPlaceReport.generate(idCompetition, formal = competition.isKc, pdf = true)
        MeasurementReport.keyword -> results["path"] = MeasurementReport.generate(idCompetition, pdf = true)
        RegistrationsReport.keyword -> results["path"] = RegistrationsReport.generate(idCompetition, pdf = true)
    }
    return results
}

internal fun getAccountCompetitionReport(request: ApiRequest): Json {
    val idAccount = request.params["idAccount"].asInt
    val idCompetition = request.params["idCompetition"].asInt
    val keyword = request.params["keyword"].asString
    val results = createResponse(request)
    results["content"] = "application/pdf"
    when (keyword) {
        RingCardReport.keyword -> {
            results["path"] = RingCardReport.generate(idCompetition, idAccount, pdf = true)
        }
        "itinerary" -> {
            val baseFile = "paperwork_personal_$idAccount"
            val pdf = Global.showDocumentPath(idCompetition, baseFile, "pdf")
            if (!File(pdf).exists()) {
                KcAccountPaperwork(idCompetition).exportPersonal(idAccount, baseFile)
                var i = 0
                while (!File(pdf).exists() && i < 100) {
                    i++
                    Thread.sleep(100)
                }
            }
            results["path"] = pdf
        }
        "entry_form" -> {
            val baseFile = "entry_form_$idAccount"
            val pdf = Global.showDocumentPath(idCompetition, baseFile, "pdf")
            if (!File(pdf).exists()) {
                KcAccountPaperwork(idCompetition).kcEntryForms(idAccount, baseFile)
                var i = 0
                while (!File(pdf).exists() && i < 100) {
                    i++
                    Thread.sleep(100)
                }
            }
            results["path"] = pdf
        }

    }
    return results
}

internal fun getCompetitionEndOfDay(request: ApiRequest): Json {
    val results = createResponse(request)

    val token = decryptJson(request.params["token"].asString)
    val idCompetitorReal = token["idCompetitorReal"].asInt

    val competitor = Competitor()
    if (!competitor.find(idCompetitorReal) || !competitor.ukaSuperUser && !competitor.plazaSuperUser) {
        return error(createResponse(), 99, "Unauthorised Access")
    }

    val idCompetition = request.params["idCompetition"].asInt
    val date = request.params["date"].asDate
    results["content"] = "application/pdf"
    results["path"] = EndOfDayReport.generate(idCompetition, date = date, pdf = true)
    return results
}

internal fun getCompetitionResultsZip(request: ApiRequest): Json {
    val idCompetition = request.params["idCompetition"].asInt
    val results = createResponse(request)
    results["content"] = "application/zip, application/octet-stream"
    results["path"] = zipCompetitionResults(idCompetition)
    return results
}


internal fun getSwapAccounts(request: ApiRequest): Json {
    val response = createResponse(request)
    if (true) {
        response["content"] = "application/vnd.ms-excel"
        response["path"] = swapAccounts()
    } else {
        return error(createResponse(), 99, "Unauthorised Access")
    }
    return response
}


internal fun getKcShowData(request: ApiRequest): Json {
    val idCompetition = request.params["idCompetition"].asInt
    val response = createResponse(request)
    if (true) {
        response["content"] = "application/vnd.ms-excel"
        response["path"] = KcShowData(idCompetition).export()
    } else {
        return error(createResponse(), 99, "Unauthorised Access")
    }
    return response
}

internal fun getKcCruftsTeams(request: ApiRequest): Json {
    val idCompetition = request.params["idCompetition"].asInt
    val response = createResponse(request)
    if (true) {
        response["content"] = "application/vnd.ms-excel"
        response["path"] = exportCruftsTeams(idCompetition)
    } else {
        return error(createResponse(), 99, "Unauthorised Access")
    }
    return response
}

internal fun getFabShowData(request: ApiRequest): Json {
    val idCompetition = request.params["idCompetition"].asInt
    val response = createResponse(request)
    if (true) {
        response["content"] = "application/vnd.ms-excel"
        response["path"] = FabShowData(idCompetition).export()
    } else {
        return error(createResponse(), 99, "Unauthorised Access")
    }
    return response
}

internal fun getUkOpenShowData(request: ApiRequest): Json {
    val idCompetition = request.params["idCompetition"].asInt
    val response = createResponse(request)
    if (true) {
        response["content"] = "application/vnd.ms-excel"
        response["path"] = UkOpenShowData(idCompetition).export()
    } else {
        return error(createResponse(), 99, "Unauthorised Access")
    }
    return response
}

internal fun getUkOpenGroups(request: ApiRequest): Json {
    val idCompetition = request.params["idCompetition"].asInt
    val response = createResponse(request)
    if (true) {
        response["content"] = "application/vnd.ms-excel"
        response["path"] = exportUkOpenGroups(idCompetition)
    } else {
        return error(createResponse(), 99, "Unauthorised Access")
    }
    return response
}

internal fun getUkaShowData(request: ApiRequest): Json {
    val idCompetition = request.params["idCompetition"].asInt
    val response = createResponse(request)
    if (true) {
        response["content"] = "application/vnd.ms-excel"
        response["path"] = UkaShowData(idCompetition).export()
    } else {
        return error(createResponse(), 99, "Unauthorised Access")
    }
    return response
}

internal fun getRunningOrderData(request: ApiRequest): Json {
    val idCompetition = request.params["idCompetition"].asInt
    val response = createResponse(request)
    if (true) {
        response["content"] = "application/vnd.ms-excel"
        response["path"] = exportRunningOrders(idCompetition)
    } else {
        return error(createResponse(), 99, "Unauthorised Access")
    }
    return response
}

internal fun getUkaMembers(request: ApiRequest): Json {
    val response = createResponse(request)
    if (true) {
        response["content"] = "application/vnd.ms-excel"
        response["path"] = UkaMembership.exportExcel()
    } else {
        return error(createResponse(), 99, "Unauthorised Access")
    }
    return response
}

internal fun getUkaQualified(request: ApiRequest): Json {
    val response = createResponse(request)
    if (true) {
        response["content"] = "application/vnd.ms-excel"
        response["path"] = exportUkaFinalsQualified()
    } else {
        return error(createResponse(), 99, "Unauthorised Access")
    }
    return response
}

internal fun getUkaInvited(request: ApiRequest): Json {
    val response = createResponse(request)
    if (true) {
        response["content"] = "application/vnd.ms-excel"
        response["path"] = exportUkaFinalsInvited()
    } else {
        return error(createResponse(), 99, "Unauthorised Access")
    }
    return response
}

internal fun getUkaHeights(request: ApiRequest): Json {
    val response = createResponse(request)
    if (true) {
        response["content"] = "application/vnd.ms-excel"
        response["path"] = PlazaAdmin.exportUkaHeights()
    } else {
        return error(createResponse(), 99, "Unauthorised Access")
    }
    return response
}

internal fun getUkaMastersEntries(request: ApiRequest): Json {
    val response = createResponse(request)
    if (true) {
        response["content"] = "application/vnd.ms-excel"
        response["path"] = mastersEntries()
    } else {
        return error(createResponse(), 99, "Unauthorised Access")
    }
    return response
}

internal fun getUkaRegistrations(request: ApiRequest): Json {
    val response = createResponse(request)
    if (true) {
        response["content"] = "application/vnd.ms-excel"
        response["path"] = registrations()
    } else {
        return error(createResponse(), 99, "Unauthorised Access")
    }
    return response
}

internal fun getUkaJuniorLeague(request: ApiRequest): Json {
    val response = createResponse(request)
    val year = today.format("yyyy").toInt()
    val yearStart = "$year-01-01".toDate()
    val height = ChangeMonitor<String>("")
    var heightNode = Json.nullNode()
    var position = 0
    var lastPoints = 0
    var lastPosition = 0
    var lastNode = Json.nullNode()


    league().join { team }.join { team.competitor }.join { team.dog }
        .where("leagueCode=$LEAGUE_UKA_JUNIOR AND dateStart=${yearStart.sqlDate}", "league.heightCode, league.points Desc, competitor.givenName") {
            if (height.hasChanged(heightCode)) {
                heightNode = response["data.heights"].addElement()
                heightNode["name"] = Height.getHeightName(heightCode)
                position = 1
                lastPoints = 0
                lastPosition = 0
                lastNode = Json.nullNode()
            }
            val node = heightNode["handlers"].addElement()
            val thisPosition = if (points == lastPoints) lastPosition else position
            if (thisPosition == lastPosition) lastNode["equal"] = true
            node["place"] = thisPosition
            node["equal"] = thisPosition == lastPosition
            node["name"] = team.competitor.fullName
            node["dogCode"] = team.dog.code
            node["petName"] = team.dog.cleanedPetName
            node["points"] = points
            lastPoints = points
            lastPosition = thisPosition
            lastNode = node
            position++
        }
    return response
}

internal fun getUkaJuniorLeagueData(request: ApiRequest): Json {
    val response = createResponse(request)
    if (true) {
        response["content"] = "application/vnd.ms-excel"
        response["path"] = UkaAdmin.juniorLeague()
    } else {
        return error(createResponse(), 99, "Unauthorised Access")
    }
    return response
}

internal fun getCompetitionTemplate(request: ApiRequest): Json {
    val nextYear = request.query["next_year"].asBoolean
    val idCompetition = request.params["idCompetition"].asInt
    val results = createResponse(request)
    results["content"] = "application/vnd.ms-excel"
    results["path"] = exportShowTemplate(idCompetition, advanceYear = nextYear)
    return results
}

internal fun getTabletList(request: ApiRequest): Json {
    val response = createResponse(request, "tabletList")
    Device().join { competition }.where("type=2", "tag") {
        val node = response["data.tablets"].addElement()
        loadFromTable(node, this, request)
        node["competition"] = competition.uniqueName
        node["uka"] = competition.isUka || competition.isUkOpen
        node["activity"] = when (activity) {
            0 -> "-"
            10 -> "Ring $ringNumber"
            20 -> "Score $ringNumber"
            30 -> "Secs"
            40 -> "SysMgr"
            else -> "n/a"
        }
        node["model"] = if (model.startsWith("Amazon"))
            "Fire"
        else if (model.contains("T6")) "T6${model.substringAfterLast("T6")}"
        else model
        node["signOnDays"] = today.daysSince(lastSignOn)
    }
    return response
}

internal fun getTabletInUse(request: ApiRequest): Json {
    val uka = request.query["uka"].asBoolean
    val response = createResponse(request, "tabletList")
    val where =
        if (uka) "type=2 AND device.dateModified>CurDate() AND competition.idOrganization IN (2,3)" else "type=2 AND device.dateModified>CurDate()"
    Device().join { competition }
        .where(where, "competition.dateStart, competition.name, if(activity IN (10, 20), ringNumber, 999), activity, tag") {
            val node = response["data.tablets"].addElement()
            loadFromTable(node, this, request)
            if (!accessPoint.startsWith("acu")) node["accessPoint"] = "n/a"
            node["competition"] = competition.niceName
            node["uka"] = competition.isUka || competition.isUkOpen
            node["activity"] = when (activity) {
                0 -> "-"
                10 -> "Ring $ringNumber"
                20 -> "Score $ringNumber"
                30 -> "Secs"
                40 -> "SysMgr"
                else -> "n/a"
            }
            node["model"] = if (model.startsWith("Amazon"))
                "Fire"
            else if (model.contains("T6")) "T6${model.substringAfterLast("T6")}"
            else model
            node["timeSinceSignOn"] = Date(now.time - lastSignOn.time)
            node["timeSinceSample"] = Date(now.time - dateModified.time)
            node["task"] = task
            node["signOnToday"] = lastSignOn > today
        }
    return response
}

internal fun getTabletLog(request: ApiRequest): Json {
    val idDevice = request.params["idDevice"].asInt
    val device = Device(idDevice)
    val response = createResponse(request, "tabletList")

    loadFromTable(response["data.device"], device, request)
    with(device) {
        response["data.device.model"] = if (model.startsWith("Amazon"))
            "Fire"
        else if (model.contains("T6")) "T6${model.substringAfterLast("T6")}"
        else model
        response["data.device.timeSinceSignOn"] = Date(now.time - lastSignOn.time)
        response["data.device.signOnToday"] = lastSignOn > today
    }

    TabletLog().where("tabletLog.idDevice=$idDevice AND tabletLog.logTime>CurDate()", "tabletLog.logTime") {
        val node = response["data.logs"].addElement()
        loadFromTable(node, this, request)
        if (!accessPoint.startsWith("acu")) node["accessPoint"] = "n/a"
        node["activity"] = when (activity) {
            0 -> "-"
            10 -> "Ring $ringNumber"
            20 -> "Score $ringNumber"
            30 -> "Secs"
            40 -> "SysMgr"
            else -> "n/a"
        }

        node["timeSinceSample"] = Date(now.time - dateModified.time)
        node["task"] = if (logType == 0) "Sign On" else task
    }
    return response
}

internal fun getCompetitionTablet(request: ApiRequest): Json {
    val idCompetition = request.params["idCompetition"].asInt
    val response = createResponse(request, "competitionTablet", "idCompetition=$idCompetition")
    val competition = Competition(idCompetition)
    loadFromTable(response["data.competition"], competition, request)

    dbQuery(
        """
        SELECT 
            device.tag, device.model, tabletLog.*
        FROM
            (SELECT 
                idDevice, MAX(logTime) AS logTime
            FROM
                tabletLog
            WHERE
                idCompetition=$idCompetition
            GROUP BY idDevice) AS t1
                JOIN
            device USING (idDevice)
                JOIN
            tabletLog ON t1.idDevice = tabletLog.idDevice
                AND t1.logTime = tabletLog.logTime
        ORDER BY device.tag        
    """.trimIndent()
    ) {
        val tag = getString("tag")
        val model = getString("model")
        val version = getString("version")
        val accessPoint = getString("accessPoint")
        val activity = getInt("activity")
        val ringNumber = getInt("ringNumber")
        val signal = getInt("signal")
        val battery = getInt("battery")
        val logTime = getDate("logTime")
        val node = response["data.tablets"].addElement()
        node["tag"] = tag
        node["version"] = version
        node["accessPoint"] = accessPoint
        node["signal"] = signal
        node["battery"] = battery
        node["logTime"] = logTime
        node["activity"] = when (activity) {
            0 -> "-"
            10 -> "Ring $ringNumber"
            20 -> "Score $ringNumber"
            30 -> "Secs"
            40 -> "SysMgr"
            else -> "n/a"
        }
        node["model"] = if (model.startsWith("Amazon"))
            "Fire"
        else if (model.contains("T6")) "T6${model.substringAfterLast("T6")}"
        else model
    }
    return response
}

internal fun getAcuList(request: ApiRequest): Json {
    val response = createResponse(request, "acu")
    Device().join { competition }.where("type=1", "tag") {
        val node = response["data.tablets"].addElement()
        loadFromTable(node, this, request)
        node["competition"] = competition.uniqueName
        node["uka"] = competition.isUka || competition.isUkOpen
        node["model"] = model.replace("Raspberry Pi ", "").replace(" Model ", "")
        node["signOnDays"] = today.daysSince(lastSignOn)
    }
    return response
}

internal fun getCompetitionSignon(request: ApiRequest): Json {
    val idCompetition = request.params["idCompetition"].asInt
    val response = createResponse(request, "competition", "idCompetition=$idCompetition")


    val competition = Competition(idCompetition)
    loadFromTable(response["data.competition"], competition, request)

    SignOn().join { this.competition }.join { device }
        .where("signOn.idCompetition=$idCompetition AND device.type=2", "signOnTime") {
            val node = response["data.signOn"].addElement()
            loadFromTable(node, this, request)
            node["signOnTime"] = time
            node["activity"] = when (activity) {
                0 -> "-"
                10 -> "Ring $ringNumber"
                20 -> "Score $ringNumber"
                30 -> "Secs"
                40 -> "SysMgr"
                else -> "n/a"
            }
            node["version"] = version
            node["battery"] = battery
            node["signal"] = signal
            node["accessPoint"] = if (accessPoint.startsWith("<")) "n/a" else accessPoint
            node["tag"] = device.tag
            node["model"] = if (device.model.startsWith("Amazon"))
                "Fire"
            else if (device.model.contains("T6")) "T6${device.model.substringAfterLast("T6")}"
            else device.model
            node["upTime"] = Date(time.time - bootTime.time)
        }
    return response
}

internal fun getSignon(request: ApiRequest): Json {
    val response = createResponse(request)
    val competitionMonitor = ChangeMonitor(-1)
    var competitionNode = Json.nullNode()

    SignOn().join { competition }.join { device }
        .where("signOnTime>curDate() AND device.type=2", "competition.briefName, signOnTime") {
            if (competitionMonitor.hasChanged(idCompetition)) {
                competitionNode = response["data.competition"].addElement()
                loadFromTable(competitionNode, competition, request)
            }
            val node = competitionNode["signOn"].addElement()
            loadFromTable(node, this, request)
            node["signOnTime"] = time
            node["activity"] = when (activity) {
                0 -> "-"
                10 -> "Ring $ringNumber"
                20 -> "Score $ringNumber"
                30 -> "Secs"
                40 -> "SysMgr"
                else -> "n/a"
            }
            node["version"] = version
            node["battery"] = battery
            node["signal"] = signal
            node["accessPoint"] = if (accessPoint.startsWith("<")) "n/a" else accessPoint
            node["tag"] = device.tag
            node["model"] = if (device.model.startsWith("Amazon"))
                "Fire"
            else if (device.model.contains("T6")) "T6${device.model.substringAfterLast("T6")}"
            else device.model
            node["upTime"] = Date(time.time - bootTime.time)
        }
    return response
}

internal fun getStockMovement(request: ApiRequest): Json {
    val idCompetition = request.params["idCompetition"].asInt
    val idCompetitorReal = request.query["idCompetitorReal"].asInt
    var response = createResponse(request, "stock_movement")
    Competitor().seek(idCompetitorReal) {
        response = createResponse(request, "stock_movement", "idAccount=$idAccount")

        fun addOption(value: Int, description: String) {
            val node = response["data.options"].addElement()
            node["value"] = value
            node["description"] = description
        }

        fun addAsset(value: Int) {
            val node = response["data.assets"].addElement()
            node["value"] = value
            node["description"] = if (value == 0) "Various" else assetToText(value)
        }

        if (idCompetition > 0) {
            response["data.mode"] = "competition"
            Competition().seek(idCompetition) {
                addOption(id, "Out to $briefName")
                response["data.option"] = id
            }
        } else {
            response["data.mode"] = "default"
            addOption(STOCK_BOOK_IN, "In to Stock")
            addOption(STOCK_CHECK, "Stock Check")
            addOption(STOCK_AQUIRE, "Purchase in to Stock")
            addOption(STOCK_DISPOSE, "Dispose")
            addOption(STOCK_LOAN, "Loan to UKA")
            addOption(STOCK_WORKSHOP, "To Workshop")
            response["data.option"] = STOCK_BOOK_IN


            //addAsset(ASSET_CONTROL_BOX)
            //addAsset(ASSET_TABLET)
            addAsset(0)
            addAsset(ASSET_PRINTER)
            addAsset(ASSET_COMPUTER)
            addAsset(ASSET_DONGLE)
            addAsset(ASSET_CHARGE_BRICK)
            addAsset(ASSET_POWER_PACK)
            addAsset(ASSET_MAINS_EXTENSION)
            addAsset(ASSET_CRATE)
            addAsset(ASSET_RING_PARTY_GUIDE)
            addAsset(ASSET_RING_PARTY_BOX)
            addAsset(ASSET_TRIPOD)
            addAsset(ASSET_WHAM_STORAGE_BOX)
            response["data.asset"] = 0
        }
        response["data.data"] = ""

    }
    return response
}

internal fun putStockMovement(body: Json, idAccount: Int): Json {
    val response = createResourceResponse()
    val option = body["option"].asInt
    val asset = body["asset"].asInt
    val data = body["data"].asString
    val assetCodes = data.split("\n")
    Device().where("assetCode IN (${assetCodes.asQuotedList()})") {
        println(tag)
    }
    val webTransaction = WebTransaction()
    webTransaction.seekStock(idAccount, force = true)
    when (option) {
        STOCK_BOOK_IN -> {
            webTransaction.stockMovementType = STOCK_BOOK_IN
            webTransaction.locationType = LOCATION_STOCK
            webTransaction.idCompetitionStock = 0
            webTransaction.locationText = "Stock"
        }
        STOCK_CHECK -> {
            webTransaction.stockMovementType = STOCK_CHECK
            webTransaction.locationType = LOCATION_STOCK
            webTransaction.idCompetitionStock = 0
            webTransaction.locationText = "Stock"
        }
        STOCK_AQUIRE -> {
            webTransaction.stockMovementType = STOCK_AQUIRE
            webTransaction.assetType = asset
            webTransaction.locationType = LOCATION_STOCK
            webTransaction.idCompetitionStock = 0
            webTransaction.locationText = "Stock"
        }
        STOCK_DISPOSE -> {
            webTransaction.stockMovementType = STOCK_DISPOSE
            webTransaction.locationType = LOCATION_OTHER_DISPOSED
            webTransaction.idCompetitionStock = 0
            webTransaction.locationText = ""
        }
        STOCK_LOAN -> {
            webTransaction.stockMovementType = STOCK_LOAN
            webTransaction.locationType = LOCATION_UKA
            webTransaction.idCompetitionStock = 0
            webTransaction.locationText = "UKA"
        }
        STOCK_WORKSHOP -> {
            webTransaction.stockMovementType = STOCK_WORKSHOP
            webTransaction.locationType = LOCATION_WORKSHOP
            webTransaction.idCompetitionStock = 0
            webTransaction.locationText = "Workshop"
        }
        else -> {
            val competition = Competition(option)
            webTransaction.stockMovementType = STOCK_BOOK_OUT
            webTransaction.locationType = LOCATION_SHOW
            webTransaction.idCompetitionStock = option
            webTransaction.locationText = competition.briefName
        }
    }
    webTransaction.assetCodes = data.replace("\n", ",")
    webTransaction.post()

    return response
}

internal fun getEntityList(request: ApiRequest): Json {
    val response = createResponse(request, "entity_list")
    val priority=request.query["priority"].asBoolean

    if (priority) {
        Entity().where("marketingPriority>0", "marketingPriority, name") {
            val item = response["data.entities"].addElement()
            loadFromTable(item, this, request)
        }

    } else {
        Entity().where("true", "name") {
            val item = response["data.entities"].addElement()
            loadFromTable(item, this, request)
        }
    }

    return response
}

internal fun getEntityAdd(request: ApiRequest): Json {
    val entity = Entity()
    val response = createResponse(request, "entity")
    entity.select("true", limit = 1)
    entity.first()
    response["data.entity"].loadFromDataset(entity, prototype = true)
    return response
}



internal fun getEntity(request: ApiRequest): Json {
    val idEntity = request.params["idEntity"].asInt
    val response = createResponse(request, "entity", "idEntity=$idEntity")

    Entity().seek(idEntity) {
        val entityNode = response["data.entity"]
        loadFromTable(entityNode, this, request)
        EntityOfficial().join { competitor }.where("idEntity=$id", "name") {
            updateFromCompetitor()
            val item = entityNode["officials"].addElement()
            loadFromTable(item, this, request)
        }
        if (idKcClub>0) {
            KcShow().where("idKcClub=$idKcClub", "dateStart") {
                val item = entityNode["kcShows"].addElement()
                loadFromTable(item, this, request)
                item["range"] = dateRange
            }
        }
        Competition().where("idEntity=$id", "dateStart") {
            val item = entityNode["competitions"].addElement()
            loadFromTable(item, this, request)
            item["range"] = dateRangeYear
        }
    }
    return response
}

internal fun putEntity(body: Json, idEntity: Int): Json {
    val response = createResourceResponse()
    val data = body["entity"]
    val entity = Entity(idEntity)
    update(response, entity, data, "idEntity", idEntity)
    return response
}

internal fun getEntityOfficial(request: ApiRequest): Json {
    val idEntityOfficial = request.params["idEntityOfficial"].asInt
    val response = createResponse(request, "entityOfficial", "idEntityOfficial=$idEntityOfficial")

    response["data.lookup"] = "${QuartzApiServer.uri}/competitor/codeInformation/"

    EntityOfficial().join { competitor }.seek(idEntityOfficial) {
        updateFromCompetitor()
        val officialNode = response["data.official"]
        officialNode["competitorCode"] = competitor.code
        loadFromTable(officialNode, this, request)
    }
    return response
}

internal fun getEntityOfficialAdd(request: ApiRequest): Json {
    val idEntity = request.params["idEntity"].asInt
    val official = EntityOfficial()
    val response = createResponse(request, "addEntryOfficial", "idEntity=$idEntity")
    response["data.lookup"] = "${QuartzApiServer.uri}/competitor/codeInformation/"
    official.select("true", limit = 1)
    official.first()
    response["data.official"].loadFromDataset(official, prototype = true)
    response["data.official.idEntity"] = idEntity
    return response
}

internal fun putEntityOfficial(body: Json, idEntityOfficial: Int): Json {
    val response = createResourceResponse()
    val data = body["official"]
    val official = EntityOfficial(idEntityOfficial)
    if (idEntityOfficial==0) official.append()
    update(response, official, data, "idEntityOfficial", idEntityOfficial)
    return response
}


internal fun getStockList(request: ApiRequest): Json {
    val response = createResponse(request, "stock_list")

    val locationMonitor = ChangeMonitor("~")
    var locationNode = Json.nullNode()
    val typeMonitor = ChangeMonitor("")
    var typeNode = Json.nullNode()
    Device().where("assetCode<>'' OR locationType IN (5)", "locationType, locationText, type, tag") {
        if (locationMonitor.hasChanged("$locationType$locationText")) {
            locationNode = response["data.locations"].addElement()
            locationNode["locationType"] = locationType
            locationNode["locationText"] = if (locationText.isEmpty()) "Not Found" else locationText
            typeMonitor.value = ""
        }
        if (typeMonitor.hasChanged("$type$subType")) {
            var description = assetToTextPlural(type)
            if (subType.isNotEmpty()) description += " - $subType"
            typeNode = locationNode["types"].addElement()
            typeNode["code"] = type
            typeNode["description"] = description
        }
        val node = typeNode["assets"].addElement()
        node["assetCode"] = assetCode
        node["tag"] = tag
        node["extra"] = model
    }
    for (location in response["data.locations"]) {
        for (typeNode in location["types"]) {
            val typeCode = typeNode["code"].asInt
            typeNode["assetCount"] = typeNode["assets"].size
            var assetList = ""
            for (node in typeNode["assets"]) {
                if (typeCode.oneOf(ASSET_TABLET, ASSET_CONTROL_BOX, ASSET_PRINTER)) {
                    assetList = assetList.append(node["tag"].asString)
                } else {
//                    assetList = assetList.append(assetLabel + node["assetCode"].asString.dropLeft(3))
                    assetList = assetList.append(node["assetCode"].asString.toLowerCase())
                }
            }
            typeNode["assetList"] = assetList
        }
    }
    return response
}

internal fun getStockMovementConfirm(request: ApiRequest): Json {
    val idCompetitorReal = request.query["idCompetitorReal"].asInt
    var response = createResponse(request, "stock_movement_confirm")
    Competitor().seek(idCompetitorReal) {
        response = createResponse(request, "stock_movement_confirm", "idAccount=$idAccount")
        val webTransaction = WebTransaction()
        webTransaction.seekStock(idAccount, force = false)
        val assetCodes = webTransaction.assetCodes.split(",")
        val typeMonitor = ChangeMonitor(-1)
        var typeNode = Json.nullNode()


        if (webTransaction.stockMovementType == STOCK_AQUIRE) {
            for (code in assetCodes) {
                if (code.isNotEmpty()) {
                    Device().seekOrAppend("assetCode=${code.quoted}", {
                        type = webTransaction.assetType
                        model = assetToText(webTransaction.assetType)
                        tag = code
                        assetCode = code
                        post()
                    })
                }
            }
        }

        Device().where("assetCode IN (${assetCodes.asQuotedList()}) OR tag IN (${assetCodes.asQuotedList()})", "type, tag") {
            if (typeMonitor.hasChanged(type)) {
                typeNode = response["data.types"].addElement()
                typeNode["code"] = type
                typeNode["description"] = assetToTextPlural(type)
            }
            val node = typeNode["assets"].addElement()
            node["assetCode"] = assetCode
            node["tag"] = tag
            node["extra"] = model
        }
        for (typeNode in response["data.types"]) {
            val typeCode = typeNode["code"].asInt
            typeNode["assetCount"] = typeNode["assets"].size
            var assetList = ""
            for (node in typeNode["assets"]) {
                if (typeCode.oneOf(ASSET_TABLET, ASSET_CONTROL_BOX, ASSET_PRINTER)) {
                    assetList = assetList.append(node["tag"].asString)
                } else {
                    assetList = assetList.append(node["assetCode"].asString.toLowerCase())
                }
            }
            typeNode["assetList"] = assetList
        }
    }
    return response
}

internal fun putStockMovementConfirm(body: Json, idAccount: Int): Json {
    val response = createResourceResponse()
    val webTransaction = WebTransaction()
    webTransaction.seekStock(idAccount, force = false)
    dbTransaction {
        var idTransaction = 0
        dbQuery("SELECT MAX(idTransaction) AS idTransaction FROM stockMovement") {
            idTransaction = getInt("idTransaction") + 1
        }
        val stockMovementType = webTransaction.stockMovementType
        val locationType = webTransaction.locationType
        val idCompetitionStock = webTransaction.idCompetitionStock
        val locationText = webTransaction.locationText
        val assetCodes = webTransaction.assetCodes.split(",")
        Device().where("assetCode IN (${assetCodes.asQuotedList()}) OR tag IN (${assetCodes.asQuotedList()})") {
            StockMovement.add(idTransaction, stockMovementType, id, locationType, idCompetitionStock, locationText, "")
        }
        if (webTransaction.idCompetitionStock > 0) {
            Competition().seek(webTransaction.idCompetitionStock) {
                when (idOrganization) {
                    ORGANIZATION_KC -> response["control.action"] = "doneKc"
                    ORGANIZATION_FAB -> response["control.action"] = "doneFab"
                }
            }
        }


        webTransaction.delete()
    }
    return response
}


internal fun getDongleList(request: ApiRequest): Json {
    val response = createResponse(request, "dongles")

    dbQuery(
        """
        SELECT 
            DATE(time) AS day,
            if (device.assetCode<>"", device.assetCode, device.tag) AS assetCode,
            device.locationText,
            imei,
            phoneNumber,
            networkProvider,
            networkType,
            uniqueName,
            AVG(signalAverage) AS signalAverage,
            MAX(realTimeTx) AS send,
            MAX(realTimeRx) AS receive,
            COUNT(*) AS Count
        FROM
            mobileSignal
                JOIN
            competition USING (idCompetition)
                LEFT JOIN
            device ON device.serial = imei
        GROUP BY if (device.assetCode<>"", device.assetCode, device.tag), DATE(time), phoneNumber, networkProvider, networkType        
    """.trimIndent()
    ) {
        val node = response["data.dongles"].addElement()
        node["day"] = getDate("day")
        node["assetCode"] = getString("assetCode")
        node["locationText"] = getString("locationText")
        node["imei"] = getString("imei")
        node["competition"] = getString("uniqueName")
        node["phoneNumber"] = getString("phoneNumber")
        node["networkProvider"] = getString("networkProvider")
        node["networkType"] = getString("networkType")
        node["signalAverage"] = "%.1f".format(getInt("signalAverage").toFloat() / 10.0)
        node["send"] = getInt("send").bytesToMegaBytes()
        node["receive"] = getInt("receive").bytesToMegaBytes()
        node["observations"] = getInt("Count")
    }
    return response
}

internal fun getCompetitionSignal(request: ApiRequest): Json {
    val idCompetition = request.params["idCompetition"].asInt
    val response = createResponse(request, "competition", "idCompetition=$idCompetition")


    val competition = Competition(idCompetition)
    loadFromTable(response["data.competition"], competition, request)

    dbQuery(
        """
        SELECT 
            DATE(time) AS day,
            if (device.assetCode<>"", device.assetCode, device.tag) AS assetCode,
            imei,
            phoneNumber,
            networkProvider,
            networkType,
            AVG(signalAverage) AS signalAverage,
            MAX(realTimeTx) AS send,
            MAX(realTimeRx) AS receive,
            COUNT(*) AS Count
        FROM
            mobileSignal
                JOIN
            competition USING (idCompetition)
                LEFT JOIN
            device ON device.serial = imei
        WHERE
            mobileSignal.idCompetition = $idCompetition
                AND time BETWEEN competition.dateStart AND competition.dateEnd + INTERVAL 1 DAY
        GROUP BY DATE(time), if (device.assetCode<>"", device.assetCode, device.tag), phoneNumber, networkProvider, networkType        
    """.trimIndent()
    ) {
        val node = response["data.dongles"].addElement()
        node["day"] = getDate("day")
        node["assetCode"] = getString("assetCode")
        node["imei"] = getString("imei")
        node["phoneNumber"] = getString("phoneNumber")
        node["networkProvider"] = getString("networkProvider")
        node["networkType"] = getString("networkType")
        node["signalAverage"] = "%.1f".format(getInt("signalAverage").toFloat() / 10.0)
        node["send"] = getInt("send").bytesToMegaBytes()
        node["receive"] = getInt("receive").bytesToMegaBytes()
        node["observations"] = getInt("Count")
    }
    return response
}

internal fun getCompetitionStock(request: ApiRequest): Json {
    val idCompetition = request.params["idCompetition"].asInt

    val response = createResponse(request, "competition_stock", "idCompetition=$idCompetition")
    Competition().seek(idCompetition) {
        generateStock()
        val competitionNode = response["data.competition"]
        loadFromTable(competitionNode, this, request)
        competitionNode["rings"] = rings
        competitionNode["tabletsSecretary"] = tabletsSecretary
        competitionNode["tabletsSpare"] = tabletsSpare
        competitionNode["acusSpare"] = acusSpare
        competitionNode["tripods"] = tripods
        response["data.pickingList"].setValue(pickingList)
    }
    return response
}

internal fun putCompetitionStock(body: Json, idCompetition: Int): Json {
    val response = createResourceResponse()
    val competitionNode = body["competition"]

    Competition().seek(idCompetition) {
        tabletsSecretary = competitionNode["tabletsSecretary"].asInt
        tabletsSpare = competitionNode["tabletsSpare"].asInt
        acusSpare = competitionNode["acusSpare"].asInt
        tripods = competitionNode["tripods"].asInt
        generateStock(post = true)
        when (idOrganization) {
            ORGANIZATION_KC -> response["control.action"] = "doneKc"
            ORGANIZATION_FAB -> response["control.action"] = "doneFab"
        }
    }
    return response
}

internal fun getCompetition(request: ApiRequest): Json {
    val idCompetition = request.params["idCompetition"].asInt
    val switchboard = request.query["switchboard"].asBoolean
    val competition = Competition()
    val control = Control()
    control.find(1)

    val response = createResponse(request, "competition", "idCompetition=$idCompetition")
    val idCompetitor = response["data.user.idCompetitor"].asInt
    val ukaSuperUser = response["data.user.ukaSuperUser"].asBoolean
    val plazaSuperUser = response["data.user.plazaSuperUser"].asBoolean
    if (competition.find(idCompetition)) {
        val tournament = competition.grandFinals || competition.isUkOpen
        response["data.user.systemManager"] = (competition.isKc && plazaSuperUser) ||
                ((competition.isUka || competition.isUkOpen) && ukaSuperUser)
        response["data.user.showSecretary"] = competition.isOfficial(idCompetitor)
        val competitionNode = response["data.competition"]
        loadFromTable(competitionNode, competition, request)
        competitionNode["venueAddress"] = competition.venueAddress
        competitionNode["campingText"] = competition.campingText
        competitionNode["entryFeeText"] = competition.entryFeeText
        competitionNode["hasManagedCamping"] = competition.hasManagedCamping
        competitionNode["tournament"] = tournament
        competitionNode["campingReleased"] = competition.campingReleased
        competitionNode["hasCruftsTeams"] = competition.hasCruftsTeams
        competitionNode["entryInfo"] = competition.entryInfo

        if (switchboard) {
            BankPaymentRequest().where("idCompetition=$idCompetition AND datePaid=0 AND transactionReference LIKE '%advance%'") {
                competitionNode["feeAdvancePending"] = amount
            }
        }

        val documents =
            DbQuery("SELECT documentName FROM competitionDocument WHERE idCompetition=$idCompetition", "documentName")
        while (documents.next()) {
            competitionNode["documents"].addElement().setValue(documents.getString("documentName"))
        }

        if (competition.isUka || competition.isUkOpen) {
            val profile = PlazaAdmin.ClassProfile(competition)
            for (day in 0..competition.dateEnd.daysSince(competition.dateStart)) {
                val dayNode = competitionNode["days"].addElement()
                var classes = ""
                var qualifiers = ""
                dayNode["date"] = competition.dateStart.addDays(day)
                for (template in ClassTemplate.members) {
                    if (template.canEnterDirectly || template.isHarvestedGroup) {
                        val item = profile.getClasses(template.code, day)
                        val label = template.nameTemplate.replace("<grade> ", "")
                        if (template.isSpecialClass && item.classes > 0) {
                            if (template == ClassTemplate.CIRCULAR_KNOCKOUT) {
                                when (item.heightCodes) {
                                    "UKA300;UKA650" -> qualifiers =
                                        qualifiers.append(template.sponsor + " " + label + " (Toy, Maxi)")
                                    "UKA400;UKA550" -> qualifiers =
                                        qualifiers.append(template.sponsor + " " + label + " (Midi, Std)")
                                    else -> qualifiers = qualifiers.append(template.sponsor + " (all heights)")
                                }
                            } else {
                                qualifiers = qualifiers.append(template.sponsor + " " + label)
                            }


                        } else if (item.classes == 1) {
                            classes = classes.append(label)
                        } else if (item.classes > 1)
                            classes = classes.append("$label x ${item.classes}")
                    }
                }
                dayNode["classes"] = classes
                dayNode["qualifiers"] = qualifiers
            }
            if (switchboard && competition.dateStart <= today) {
                CompetitionDay().join{ this.competition }.where("CompetitionDay.idCompetition=$idCompetition AND date<=CURDATE() AND date BETWEEN competition.dateStart AND competition.dateEnd", "date") {
                    val dateNode = competitionNode["dates"].addElement()
                    dateNode.setValue(date)
                }
            }

            if (tournament) {
                val orderBy = "classDate DESC, gradeCodes, classCode, suffix"

                AgilityClass().where("idCompetition=$idCompetition", orderBy) {
                    if (finalized) {
                        val node = competitionNode["results"].addElement()
                        loadFromTable(node, this, request)
                    }
                    if (template == ClassTemplate.UK_OPEN_PENTATHLON) {
                        val node1 = competitionNode["results"].addElement()
                        node1["idAgilityClass"] = id
                        node1["className"] = "$nameLong (2/5)"
                        node1["subResultsFlag"] = 3
                        val node2 = competitionNode["results"].addElement()
                        node2["idAgilityClass"] = id
                        node2["className"] = "$nameLong (4/5)"
                        node2["subResultsFlag"] = 15
                    }
                }

                AgilityClass().where(
                    "idCompetition=$idCompetition AND classCode<9000 AND classCode<>${ClassTemplate.TEAM.code}",
                    orderBy
                ) {
                    if (!finalized && runningOrdersGenerated && !template.hasChildren) {
                        var select =
                            if (_readyToRun || competition.ukOpenLocked) "runningOrders" else "provisionalRunningOrders"
                        val node = competitionNode[select].addElement()
                        loadFromTable(node, this, request)
                    }
                }
            }
        } else if (competition.isFab) {
            competitionNode["ifcsFeeText"] = competition.ifcsFee.money

            val profile = PlazaAdmin.ClassProfile(competition)
            for (day in 0..competition.dateEnd.daysSince(competition.dateStart)) {
                val dayNode = competitionNode["days"].addElement()
                var classes = ""
                dayNode["date"] = competition.dateStart.addDays(day)
                for (template in ClassTemplate.members) {
                    if (template.canEnterDirectly || template.isHarvestedGroup) {
                        val item = profile.getClasses(template.code, day)
                        val label = template.nameTemplate.replace("<grade> ", "")
                        if (item.classes == 1) {
                            classes = classes.append(label)
                        } else if (item.classes > 1)
                            classes = classes.append("$label x ${item.classes}")
                    }
                }
                dayNode["classes"] = classes
            }
            if (switchboard && competition.dateStart <= today) {
                CompetitionDay().where("idCompetition=$idCompetition AND DATE<=CURDATE()", "date") {
                    val dateNode = competitionNode["dates"].addElement()
                    dateNode.setValue(date)
                }
            }
            if (switchboard) {
                val voucher = Voucher()
                voucher.select(
                    "idCompetition=$idCompetition AND type<>$VOUCHER_CAMPING_PERMIT",
                    "voucher.type, voucher.description"
                )
                while (voucher.next()) {
                    competitionNode["bankAccountName"] = competition.bankAccountName
                    competitionNode["bankAccountSort"] = competition.bankAccountSort
                    competitionNode["bankAccountNumber"] = competition.bankAccountNumber
                    competitionNode["printCost"] = competition.printCost
                    competitionNode["printCostPound"] = competition.printCostPound
                    competitionNode["printQuantity"] = competition.printQuantity
                    competitionNode["processingFeeSwap"] = competition.processingFeeSwap

                    val node = competitionNode["vouchers"].addElement()
                    var typeText = voucherToText(voucher.type)
                    if (voucher.type == VOUCHER_RING_PARTY && voucher.ringPartyName.isNotEmpty()) {
                        typeText += " (${voucher.ringPartyName})"
                    }
                    node["idVoucher"] = voucher.id
                    node["type"] = typeText
                    node["code"] = voucher.code
                    node["specification"] = voucher.specification
                }
            }

        } else if (competition.isKc || competition.isIndependent) {
            val agilityClass =
                AgilityClass.select("idCompetition=$idCompetition", "classDate, classNumber, classNumberSuffix")
            var thisDate = nullDate
            var dayNode = Json.nullNode()
            while (agilityClass.next()) {
                if (agilityClass.template.canEnterOnline) {
                    if (agilityClass.date != thisDate) {
                        thisDate = agilityClass.date
                        dayNode = competitionNode["days"].addElement()
                        dayNode["date"] = agilityClass.date
                    }
                    dayNode["classes"].addElement().setValue(agilityClass.nameLong)
                }
            }
            if (switchboard) {
                competitionNode["bankAccountName"] = competition.bankAccountName
                competitionNode["bankAccountSort"] = competition.bankAccountSort
                competitionNode["bankAccountNumber"] = competition.bankAccountNumber
                competitionNode["printCost"] = competition.printCost
                competitionNode["printCostPound"] = competition.printCostPound
                competitionNode["printQuantity"] = competition.printQuantity
                competitionNode["processingFeeSwap"] = competition.processingFeeSwap

                val voucher = Voucher()
                voucher.select(
                    "idCompetition=$idCompetition AND type<>$VOUCHER_CAMPING_PERMIT",
                    "voucher.type, voucher.description"
                )
                while (voucher.next()) {
                    val node = competitionNode["vouchers"].addElement()
                    var typeText = voucherToText(voucher.type)
                    if (voucher.type == VOUCHER_RING_PARTY && voucher.ringPartyName.isNotEmpty()) {
                        typeText += " (${voucher.ringPartyName})"
                    }
                    node["idVoucher"] = voucher.id
                    node["type"] = typeText
                    node["code"] = voucher.code
                    node["specification"] = voucher.specification
                }

            }
        }

    } else {
        return error(response, 1, "Competition not found")
    }
    return response
}

internal fun putCompetition(body: Json, idCompetition: Int): Json {
    val response = createResourceResponse()
    val data = body["competition"]

    val competition = Competition(idCompetition)
    val bindings = getBindings(body, "competition")
    update(response, competition, data, "idCompetition", idCompetition, bindings)

    if (data.has("feeAdvance") && data["feeAdvance"].asString.poundsToPence() > 0) {
        BankPaymentRequest.showAdvance(idCompetition, data["feeAdvance"].asString.poundsToPence())
    }
    return response
}


internal fun getCompetitionEntryStats(request: ApiRequest): Json {
    val idCompetition = request.params["idCompetition"].asInt
    val competition = Competition(idCompetition)
    val response = createResponse(request)
    loadFromTable(response["data.competition"], competition, request)

    val groupBy = if (competition.isUka)
        "agilityClass.idAgilityClass , entry.jumpHeightCode"
    else
        "agilityClass.idAgilityClass , entry.subClass"
    val orderBy = if (competition.isUka || competition.isFab)
        "classDate, classCode, suffix, agilityClass.gradeCodes"
    else
        "classDate, classNumber, classNumberSuffix, entry.subClass"

    var dayNode = Json.nullNode()
    var classNode = Json.nullNode()
    val competitionNode = response["data.competition"]

    DbQuery(
        """
            SELECT
                agilityClass.idAgilityClass,
                classDate,
                classCode,
                className,
                entry.subClass,
                SUM(IF(entry.idEntry IS NULL, 0, 1)) AS entries
            FROM
                agilityClass
                    LEFT JOIN
                entry USING (idAgilityClass)
            WHERE
                idCompetition = $idCompetition AND entry.progress<$PROGRESS_REMOVED
            GROUP BY $groupBy
            ORDER BY $orderBy
        """
    ).forEach { q ->
        val idAgilityClass = q.getInt("idAgilityClass")
        val classDate = q.getDate("classDate")
        val classCode = q.getInt("classCode")
        val className = q.getString("className")
        val subClass = q.getInt("subClass")
        val entries = q.getInt("entries")

        val agilityClass = AgilityClass()

        val template = ClassTemplate.select(classCode)
        if (template.canEnterDirectly) {
            if (classDate != dayNode["date"].asDate) {
                dayNode = response["data.competition.days"].addElement()
                dayNode["date"] = classDate
                dayNode["entries"] = 0
            }
            if (idAgilityClass != classNode["idAgilityClass"].asInt) {
                classNode = dayNode["classes"].addElement()
                classNode["idAgilityClass"] = idAgilityClass
                classNode["name"] = className
                classNode["entries"] = 0
                agilityClass.find(idAgilityClass)
                for (index in 0..agilityClass.subClassCount - 1) {
                    val subNode = classNode["subClasses"].addElement()
                    subNode["code"] = index
                    subNode["description"] =
                        agilityClass.subClassDescription(index, shortGrade = true, shortHeight = competition.isUka || competition.isFab)
                    subNode["entries"] = 0

                }
            }
            val subNode = classNode["subClasses"].searchElement("code", subClass) { it["description"] = "N/A" }
            subNode["entries"] = entries
            classNode["entries"] = classNode["entries"].asInt + entries
            dayNode["entries"] = dayNode["entries"].asInt + entries
            competitionNode["entries"] = competitionNode["entries"].asInt + entries
        }
    }
    return response
}


internal fun getCompetitionOpen(request: ApiRequest): Json {
    val paper = request.query["paper"].asBoolean
    val access = request.query["access"].asInt

    val response = createResponse(request)
    val plazaSuperUser = response["data.user.plazaSuperUser"].asBoolean
    val ukaSuperUser = response["data.user.ukaSuperUser"].asBoolean
    val systemAdministrator = response["data.user.systemAdministrator"].asBoolean
    val enteredOnline = ArrayList<Int>()
    val enteredPaper = ArrayList<Int>()
    val idAccount = request.query["idAccount"].asInt
    val account = Account(idAccount)
    if (idAccount > 0) {
        dbQuery("SELECT idCompetition, type FROM ledger WHERE idAccount=$idAccount AND type IN ($LEDGER_ENTRY_FEES, $LEDGER_ENTRY_FEES_PAPER)") {
            val idCompetition = getInt("idCompetition")
            val type = getInt("type")
            when (type) {
                LEDGER_ENTRY_FEES -> enteredOnline.add(idCompetition)
                LEDGER_ENTRY_FEES_PAPER -> enteredPaper.add(idCompetition)
            }
        }
    }

    val closePhrase =
        if (systemAdministrator) "dateStart >= ${today.sqlDateTime}" else if (plazaSuperUser || ukaSuperUser) "NOT processed" else "NOT closed AND NOT capReached AND dateCloses >= ${today.sqlDateTime}"
    val where = "$closePhrase AND NOT hidden ${accessCondition(request)}" +
            if (!Global.quartzTest) " AND dateOpens <= ${now.sqlDateTime} AND NOT provisional" else ""

    Competition().where(where, "dateStart") {
        val blocked = isFab && account.fabBlocked
        if ((!hidden || (isKc || isFab) && plazaSuperUser || isUka && ukaSuperUser || isUkOpen && ukaSuperUser || Global.quartzTest) && !blocked) {
            val node = response["data.competitions"].addElement()
            loadFromTable(node, this, request)
            node["enteredOnline"] = enteredOnline.contains(id)
            node["enteredPaper"] = enteredPaper.contains(id)
            node["hidden"] = hidden
            node["weekNumber"] = weekNumber
            node["independentType"] = independentType
            if (grandFinals) node["grandFinals"] = true
        }
    }
    return response
}

internal fun getCompetitorCompetition(request: ApiRequest): Json {
    val response = createResponse(request)

    val systemAdministrator = response["data.user.systemAdministrator"].asBoolean
    val plazaSuperUser = response["data.user.plazaSuperUser"].asBoolean
    val ukaSuperUser = response["data.user.ukaSuperUser"].asBoolean
    val competitionList = response["data.user.competitionList"].asString

    val where =
        if (systemAdministrator) {
            "true"
        } else if (plazaSuperUser && ukaSuperUser)
            "NOT imported AND idOrganization IN (1, 2, 3)"
        else if (plazaSuperUser)
            "NOT idOrganization IN (2, 3)"
        else if (ukaSuperUser)
            "NOT imported AND idOrganization IN (2, 3)"
        else if (competitionList.isNotEmpty())
            "idCompetition IN ($competitionList)"
        else
            "false"

    val bandMonitor = ChangeMonitor<String>("")
    var bandNode = Json.nullNode()
    val lastYearStart = "${today.addYears(-1).format("yyyy")}-01-01".toDate()
    Competition().where(where, "dateStart") {
        var band =
            if (dateStart < lastYearStart) "pre ${lastYearStart.format("yyyy")}" else dateStart.format("yyyy")
        if (dateStart.isSameYear(today)) {
            if (dateStart <= today.addDays(-16)) {
                band += " - completed"
            } else {
                band += " - live"
            }

        }
        if (bandMonitor.hasChanged(band)) {
            bandNode = response["data.bands"].addElement()
            bandNode["name"] = band
            bandNode["open"] = band.contains("live")
        }
        val node = bandNode["competitions"].addElement()
        loadFromTable(node, this, request)
        node["organization_suffix"] = organizationToSuffix(idOrganization)
        node["weekNumber"] = weekNumber
    }
    return response
}

internal fun getCompetitionSwap(request: ApiRequest): Json {
    val response = createResponse(request)
    val plazaSuperUser = response["data.user.plazaSuperUser"].asBoolean
    val ukaSuperUser = response["data.user.ukaSuperUser"].asBoolean
    val idCompetitor = response["data.user.idCompetitor"].asInt
    Competition().where("idOrganization=$ORGANIZATION_KC AND processed", "dateStart") {

    }
    return response
}

internal fun getKcShowList(request: ApiRequest): Json {
    val response = createResponse(request)

    KcShow().where("true", "dateStart") {
        val node = response["data.kc_shows"].addElement()
        loadFromTable(node, this, request)
        node["weekNumber"] = weekNumber
    }

    return response
}

internal fun getCompetitionEntryMap(request: ApiRequest): Json {

    val idCompetition = request.params["idCompetition"].asInt
    val accountList = ArrayList<Int>()
    val campingIcon = HashMap<Int, String>()

    dbQuery("SELECT DISTINCT entry.idAccount FROM entry JOIN agilityClass USING (idAgilityClass) WHERE agilityClass.idCompetition=$idCompetition") {
        accountList.add(getInt("idAccount"))
    }
    Camping().where("idCompetition=$idCompetition") {
        if (confirmed) {
            campingIcon.put(idAccount, "yellow-dot")
        } else if (rejected) {
            campingIcon.put(idAccount, "red-dot")
        } else {
            campingIcon.put(idAccount, "orange-dot")
        }
    }

    val response = createResponse(request)
    val monitor = ChangeMonitor("")
    var node = Json.nullNode()
    var count = 0


    Account().join { geoData }.join { competitor }
        .where("account.idAccount IN (${accountList.asCommaList()}) AND NOT geoData.postcode IS NULL", "account.postCode, account.idAccount") {
            if (monitor.hasChanged("$postcode $code")) {
                count = 0
                val icon = campingIcon.getOrDefault(id, "green-dot")
                node = response["data.markers"].addElement()
                node["latitude"] = geoData.latitude
                node["longitude"] = geoData.longitude
                node["name"] = competitor.fullName
                node["postcode"] = fullAddress
                node["icon"] = "http://maps.google.com/mapfiles/ms/micons/$icon.png"
            }
            count++
            //node["label"] = count.toString()
            node["name"] = node["name"].asString
        }

    return response
}

internal fun getAccountMap(request: ApiRequest): Json {

    val response = createResponse(request)
    val monitor = ChangeMonitor("")
    var node = Json.nullNode()
    var count = 0
    val postcodes = request.query["postcodes"].asString
    var postcodeWhere = ""
    if (postcodes.isNotEmpty()) {
        for (postcode in postcodes.split(",")) {
            postcodeWhere = postcodeWhere.append("account.postcode LIKE '$postcode%'", " OR ")
        }
    }
    if (postcodeWhere.isNotEmpty()) postcodeWhere = " AND ($postcodeWhere)"

    val where = "account.registrationComplete AND NOT geoData.postcode IS NULL$postcodeWhere"

    Account().join { geoData }.join { competitor }.where(where, "account.postCode, account.idAccount", limit = 200) {
        if (monitor.hasChanged("$postcode $code")) {
            count = 0
            val icon = "red-dot"
            node = response["data.markers"].addElement()
            node["latitude"] = geoData.latitude
            node["longitude"] = geoData.longitude
            node["name"] = competitor.fullName
            node["postcode"] = postcode
            node["icon"] = "http://maps.google.com/mapfiles/ms/micons/$icon.png"
        }
        count++
        //node["label"] = count.toString()
        node["name"] = node["name"].asString
    }

    return response
}

internal fun getAgilitynetShowMap(request: ApiRequest): Json {

    val response = createResponse(request)
    val monitor = ChangeMonitor("")
    var node = Json.nullNode()
    var count = 0

    val target = request.query["target"].asBoolean

    AgilitynetShow().join { geoData }.join { entity }
        .where("type=22 AND processor<>'' AND NOT geoData.postcode IS NULL", "venuePostcode, clubName, dateStart") {
            if (!target || entity.idProcessor!= PROCESSOR_PLAZA) {
                if (monitor.hasChanged("$venuePostcode $clubName")) {
                    count = 0
                    val icon = when (processor) {
                        "AP" -> "green"
                        "ASO" -> "blue"
                        "FPP" -> "red"
                        "ST" -> "pink"
                        "AA" -> "orange"
                        "LN" -> "yellow"
                        else -> "lightblue"
                    }

                    node = response["data.markers"].addElement()
                    node["latitude"] = geoData.latitude
                    node["longitude"] = geoData.longitude
                    node["processor"] = processor
                    node["name"] = "$name (${node["processor"].asString})"
                    node["postcode"] = venuePostcode
                    node["icon"] = "http://maps.google.com/mapfiles/ms/micons/$icon.png"
                }
                count++
                node["label"] = if (target) entity.marketingPriority.toString() else count.toString()
                node["name"] = node["name"].asString + "\n" + dateRange
            }
        }

    return response
}

internal fun getDiaryMap(request: ApiRequest): Json {

    val response = createResponse(request)
    val monitor = ChangeMonitor("")
    var node = Json.nullNode()
    var count = 0

    Competition().join { geoData }
        .where("NOT hidden AND (dateEnd >= ${today.sqlDate} OR dateStart >= ${today.sqlDate}) AND NOT geoData.postcode IS NULL", "venuePostcode, idOrganization DESC, dateStart DESC") {
            if (monitor.hasChanged("$venuePostcode $idOrganization")) {
                count = 0
                val icon = when (idOrganization) {
                    ORGANIZATION_KC -> "green"
                    ORGANIZATION_UKA -> "blue"
                    ORGANIZATION_FAB, ORGANIZATION_IFCS -> "pink"
                    else -> "lightblue"
                }

                node = response["data.markers"].addElement()
                node["latitude"] = geoData.latitude
                node["longitude"] = geoData.longitude
                node["name"] = ""
                node["postcode"] = venuePostcode
                node["icon"] = "http://maps.google.com/mapfiles/ms/micons/$icon.png"
            }
            count++
            node["label"] = count.toString()
            node["name"] = node["name"].asString.append("$name $dateRange", "\n")
        }


    return response
}

internal fun getKcShowMap(request: ApiRequest): Json {

    val response = createResponse(request)
    val monitor = ChangeMonitor("")
    var node = Json.nullNode()
    var count = 0

    KcShow().join { geoData }.where("NOT geoData.postcode IS NULL", "venuePostcode, idKcClub, dateStart") {
        if (monitor.hasChanged("$venuePostcode $idKcClub")) {
            count = 0
            val processor = if (processorConfirmed.isNotEmpty()) processorConfirmed else processorHistoric
            val icon = when (processor) {
                "AP" -> "green"
                "ASO" -> "blue"
                "FPP" -> "red"
                "ST" -> "pink"
                "AA" -> "orange"
                "LN" -> "yellow"
                else -> "lightblue"
            }

            node = response["data.markers"].addElement()
            node["latitude"] = geoData.latitude
            node["longitude"] = geoData.longitude
            node["processor"] = if (processorConfirmed.isNotEmpty()) processorConfirmed else processorHistoric
            node["name"] = "$name (${node["processor"].asString})"
            node["postcode"] = venuePostcode
            node["icon"] = "http://maps.google.com/mapfiles/ms/micons/$icon.png"
        }
        count++
        node["label"] = count.toString()
        node["name"] = node["name"].asString + "\n" + dateRange
    }

    return response
}


internal fun getCompetitionActive(request: ApiRequest): Json {
    val response = createResponse(request)
    val plazaSuperUser = response["data.user.plazaSuperUser"].asBoolean
    val ukaSuperUser = response["data.user.ukaSuperUser"].asBoolean
    val idCompetitor = response["data.user.idCompetitor"].asInt


    val competition = Competition()
    competition.select(
        "NOT hidden AND (dateEnd >= ${today.sqlDate} OR dateStart >= ${today.sqlDate}) ${accessCondition(request)}",
        "dateStart"
    )
    if (competition.rowCount > 0) {
        while (competition.next()) {

            val systemManager =
                (competition.isPlazaManaged && plazaSuperUser) || (competition.isUkaManaged && ukaSuperUser)
            var showSecretary = competition.isOfficial(idCompetitor)
            if (!competition.hidden || systemManager || showSecretary || Global.quartzTest) {
                val node = response["data.competitions"].addElement()
                loadFromTable(node, competition, request)
                node["hidden"] = competition.hidden
                node["canEnter"] =
                    if (Global.quartzTest)
                        competition.dateCloses >= today
                    else
                        competition.isOpen
                node["systemManager"] = systemManager
                node["showSecretary"] = showSecretary
                node["entryInfo"] = competition.entryInfo
                node["weekNumber"] = competition.weekNumber
            }
        }
    } else {
        return error(response, 1, "not found")
    }
    return response
}

internal fun getCompetitionDays(request: ApiRequest): Json {
    val response = createResponse(request)
    val idCompetition = request.params["idCompetition"].asInt
    val active = request.query["active"].asBoolean

    var where = "competitionDay.idCompetition=$idCompetition AND dayType<${DAY_REST}"
    if (active) {
        where += " AND competitionDay.date<=curdate()"
    }

    val competitionDay = CompetitionDay()
    competitionDay.competition.joinToParent()
    competitionDay.select(where, "competitionDay.date")
    if (competitionDay.rowCount > 0) {
        competitionDay.first()
        val competitionNode = response["data.competition"]
        loadFromTable(competitionNode, competitionDay.competition, request)
        competitionDay.beforeFirst()
        while (competitionDay.next()) {
            val node = competitionNode["competitionDays"].addElement()
            loadFromTable(node, competitionDay, request)
        }
    } else {
        return error(response, 1, "not found")
    }
    return response
}

internal fun doGetCurrentCompetitionDays(request: ApiRequest, liveOnly: Boolean = false): Json {
    val response = createResponse(request)

    val where =
        if (liveOnly)
            "dayType<${DAY_REST} AND competition.dateStart<=curdate() + interval 2 week AND competition.dateEnd>curdate()"
        else
            "dayType<${DAY_REST} AND competition.dateStart<=curdate() AND competition.dateEnd>curdate() - interval 1 month"
    val sortBy =
        if (liveOnly)
            "competition.dateStart, competitionDay.date"
        else
            "if(competition.dateEnd>=curdate(), 0, 1), if(competition.dateEnd>=curdate(), competition.dateStart, competition.dateEnd) DESC, competition.idCompetition, competitionDay.date"


    val competitionDay = CompetitionDay()
    competitionDay.competition.joinToParent()
    competitionDay.select(where, sortBy)
    var competitionNode = Json.nullNode()
    var idCompetition = 0
    if (competitionDay.rowCount > 0) {
        while (competitionDay.next()) {
            if (competitionDay.idCompetition != idCompetition) {
                idCompetition = competitionDay.idCompetition
                competitionNode = response["data.competitions"].addElement()
                loadFromTable(competitionNode, competitionDay.competition, request)
            }
            val node = competitionNode["competitionDays"].addElement()
            loadFromTable(node, competitionDay, request)
        }
    } else {
        return error(response, 1, "not found")
    }
    return response
}


internal fun getCurrentCompetitionDays(request: ApiRequest): Json {
    return doGetCurrentCompetitionDays(request, liveOnly = false)
}

// http://localhost:3000/competition/616/ringPlan/20171001 - Harpbury Sunday

fun getCompetitionLive(request: ApiRequest): Json {
    val response = createResponse(request)

    val where =
        "competition.dateStart<=curdate() + interval 1 week AND competition.dateEnd>=curdate() AND NOT provisional AND NOT hidden"
    val sortBy = "competition.dateStart"

    val competition = Competition.select(where, sortBy)

    val ring = Ring()
    ring.agilityClass.joinToParent()
    ring.select(
        "date=curdate() AND NOT ring.paperBased AND NOT agilityClass.paperBased",
        "ring.idCompetition, ring.ringNumber"
    )

    while (competition.next()) {
        val competitionNode = response["data.competitions"].addElement()
        loadFromTable(competitionNode, competition, request)
        if (competition.isInProgress || true) {
            ring.beforeFirst()
            while (ring.next()) {
                if (ring.idCompetition == competition.id) {
                    val ringNode = competitionNode["rings"].addElement()
                    ringNode["ringNumber"] = ring.number
                    ringNode["judge"] = ring.judge
                    ringNode["class"] = ring.agilityClass.name
                    ringNode["progress"] = ring.agilityClass.getProgressText(ring.heightCode)
                }
            }
        }
    }
    return response
}

internal fun getCompetitionProgressByDate(request: ApiRequest): Json {
    val response = createResponse(request)

    val idCompetition = request.params["idCompetition"].asInt
    val date = request.params["date"].asDate
    val competition = Competition()
    competition.find(idCompetition)

    val orderBy =
        if (competition.isUka)
            "if(classProgress=${CLASS_CLOSED}, 1, 0), gradeCodes, classCode, suffix"
        else
            "if(classProgress=${CLASS_CLOSED}, 1, 0), classNumber, classNumberSuffix, part"

    var where = """
        agilityClass.idCompetition=$idCompetition AND
        agilityClass.classDate=${date.sqlDate} AND
        classProgress>${CLASS_PENDING}
    """
    val agilityClass = AgilityClass()
    agilityClass.select(where, orderBy)
    if (agilityClass.rowCount > 0) {
        agilityClass.first()
        val competitionNode = response["data.competition"]
        var openNode = Json.nullNode()
        var closedNode = Json.nullNode()
        loadFromTable(competitionNode, competition, request)
        competitionNode["selectedDate"] = date

        agilityClass.beforeFirst()
        while (agilityClass.next()) {
            var node: JsonNode
            if (agilityClass.progress != CLASS_CLOSED) {
                if (openNode.isNull) {
                    openNode = competitionNode["statusGroups"].addElement()
                    openNode["status"] = "open"
                    openNode["description"] = "Classes in Progress"
                }
                node = openNode["agilityClasses"].addElement()
            } else {
                if (closedNode.isNull) {
                    closedNode = competitionNode["statusGroups"].addElement()
                    closedNode["status"] = "closed"
                    closedNode["description"] = "Closed Classes"
                }
                node = closedNode["agilityClasses"].addElement()
            }
            loadFromTable(node, agilityClass, request)
        }
    } else {
        return error(response, 1, "not found")
    }
    return response
}

internal fun getAgilityClassResults(request: ApiRequest): Json {
    val response = createResponse(request)

    val idAgilityClass = request.params["idAgilityClass"].asInt
    val idDog = request.query["dog"].asInt

    var haveABC = false

    val agilityClass = AgilityClass()
    agilityClass.competition.joinToParent()
    agilityClass.find(idAgilityClass)

    response["data.date"] = agilityClass.date
    val competitionNode = response["data.competition"]
    loadFromTable(competitionNode, agilityClass.competition, request)
    val agilityClassNode = response["data.agilityClass"]
    loadFromTable(agilityClassNode, agilityClass, request)
    agilityClassNode["judge"] = agilityClass.judge
    agilityClassNode["courseLength"] = agilityClass.courseLength
    agilityClassNode["closed"] = agilityClass.progress >= CLASS_CLOSED
    agilityClassNode["investigation"] = agilityClass.investigation

    if (agilityClass.competition.isInProgress) {
        agilityClassNode["presented"] = agilityClass.presented
        if (!agilityClass.presented && agilityClass.date == today) {
            agilityClassNode["presentingTime"] = agilityClass.presentingTime.timeText
        }
    }

    var columns = agilityClass.template.columns
    var headings = agilityClass.template.headings
    when (agilityClass.progress) {
        CLASS_CLOSED -> {
            headings = headings.replace(",Progression Points", ",Points")
        }
        CLASS_HISTORIC -> {
            agilityClassNode["historic"] = true
            columns = columns.replace("prize", "place").replace("place", "place").replace(",runData", "")
            headings = headings.replace("Prize", "Place").replace("Place", "Place")
                .replace(",Progression Points", ",Points").replace(",Run Data", "")
        }
        else -> {
            columns = columns.replace("prize", "rank").replace("place", "rank").replace(",progressionPoints", "")
            headings = headings.replace("Prize", "Rank").replace("Place", "Rank").replace(",Progression Points", "")
        }
    }

    agilityClassNode["columns"] = columns
    agilityClassNode["headings"] = headings

    val includeCourseTimes = columns.split(",").contains("courseTime")

    agilityClassNode["prize"] = agilityClass.progress == CLASS_CLOSED && (agilityClass.isKc)

    agilityClassNode["progressionPoints"] = agilityClass.progress == CLASS_CLOSED && (
            agilityClass.isUkaProgression || agilityClass.isKcProgression)


    val where = "entry.idAgilityClass=${agilityClass.id} AND entry.hasRun"
    var orderBy: String
    if (agilityClass.isUka || agilityClass.isUkOpen) {
        orderBy = if (agilityClass.combineHeights)
            agilityClass.template.resultsOrderBy + ", competitor.givenName, competitor.familyName, dog.petName, dog.registeredName"
        else
            "(FIND_IN_SET(entry.jumpHeightCode, agilityClass.heightRunningOrder)) DESC, " + agilityClass.template.resultsOrderBy + ", competitor.givenName, competitor.familyName, dog.petName, dog.registeredName"

        if (agilityClass.progress == CLASS_CLOSED) {
            orderBy = if (agilityClass.combineHeights)
                "if(courseFaults>=100, courseFaults, 0), if(courseFaults>=100, 999999, entry.place), competitor.givenName, competitor.familyName, dog.petName, dog.registeredName"
            else
                "entry.jumpHeightCode DESC, if(courseFaults>=100, courseFaults, 0), if(courseFaults>=100, 999999, entry.place), competitor.givenName, competitor.familyName, dog.petName, dog.registeredName"
        }
        if (agilityClass.progress == CLASS_HISTORIC) {
            orderBy = if (agilityClass.combineHeights)
                "progressionPoints DESC, time, competitor.givenName, competitor.familyName, dog.petName, dog.registeredName"
            else
                "entry.heightCode DESC, progressionPoints DESC, time, competitor.givenName, competitor.familyName, dog.petName, dog.registeredName"
        }
    } else {
        orderBy = "entry.subClass, " + agilityClass.template.resultsOrderBy +
                ", competitor.givenName, competitor.familyName, dog.petName, dog.registeredName"
        if (agilityClass.progress == CLASS_CLOSED) {
            orderBy =
                "entry.subClass, if(courseFaults>=100, courseFaults, 0), if(courseFaults>=100, 999999, entry.place), competitor.givenName, competitor.familyName, dog.petName, dog.registeredName"
        }

    }
    val entry = Entry()
    entry.join(entry.agilityClass, entry.team, entry.team.dog, entry.team.competitor)

    entry.select(where, orderBy)
    if (entry.rowCount > 0) {
        var subClassNode = Json.nullNode()
        var subClass = -1
        var rank = 0
        while (entry.next()) {
            val thisSubClass =
                if (agilityClass.isUka) entry.ukaSubClass else if (agilityClass.isUkOpen) entry.ukOpenSubClass else entry.subClass
            if (thisSubClass != subClass) {
                subClass = thisSubClass
                subClassNode = agilityClassNode["subClasses"].addElement()
                subClassNode["description"] =
                    if (agilityClass.isUka || agilityClass.isUkOpen) Height.getHeightName(entry.heightCode) else agilityClass.subClassDescription(
                        subClass,
                        shortGrade = false
                    )
                subClassNode["courseTime"] =
                    if (agilityClass.isUkaStyle || agilityClass.isFabStyle) entry.agilityClass.getCourseTime(entry.heightCode) / 1000 else agilityClass.courseTime / 1000
                subClassNode["nfc"] = ""
                subClassNode["eliminated"] = ""
                rank = 0
                haveABC = false
            }
            if (entry.isEffectivelyEliminated) {
                val score = entry.getRunDataEliminated()
                val name =
                    if (agilityClass.template == ClassTemplate.TEAM_INDIVIDUAL) {
                        entry.team.teamName + " - " + entry.teamMemberName
                    } else {
                        entry.teamMemberName
                    }
                if (score.isEmpty()) {
                    subClassNode["eliminated"] = subClassNode["eliminated"].asString.append(name)
                } else {
                    subClassNode["eliminated"] = subClassNode["eliminated"].asString.append("$name ($score)")
                }
            } else if (entry.isNFC) {
                subClassNode["nfc"] = subClassNode["nfc"].asString.append(entry.teamDescription)
            } else {
                var prizeText = entry.prizeText
                if (entry.agilityClass.fastestABC && !entry.team.dog.isCollie && !entry.isEliminated && !haveABC) {
                    prizeText += "*"
                    haveABC = true
                }
                rank++
                val node = subClassNode["entries"].addElement()
                val teamName = if (agilityClass.isUka) entry.teamMemberNameAndUkaTitle else entry.teamMemberName
                node["competitor"] = if (entry.clearRoundOnly) teamName + " (CRO)" else teamName
                node["scoreCodes"] = entry.scoreText
                node["isPlaceable"] = entry.isPlaceable
                node["rank"] = if (entry.isPlaceable) rank.toString() else ""
                node["place"] = entry.place.toString()
                node["points"] = entry.points.dec3
                node["faults"] = entry.faults.dec3Int
                if (agilityClass.template.oneOf(
                        ClassTemplate.MASTERS,
                        ClassTemplate.JUNIOR_OPEN,
                        ClassTemplate.JUNIOR_OPEN_FINAL,
                        ClassTemplate.JUNIOR_MASTERS
                    )
                ) {
                    node["jumping"] = entry.subResultPointsDec3(0)
                    node["agility"] = entry.subResultPointsDec3(1)
                    node["total"] = entry.points.dec3
                } else if (agilityClass.template.oneOf(ClassTemplate.CHALLENGE, ClassTemplate.CHALLENGE_FINAL)) {
                    node["jumping"] = entry.subResultFaultsTimeText(0)
                    node["agility"] = entry.subResultFaultsTimeText(1)
                } else if (agilityClass.template.oneOf(ClassTemplate.KC_CHAMPIONSHIP_HEAT)) {
                    node["jumping"] = entry.subResultPointsText(0)
                    node["agility"] = entry.subResultPointsText(1)
                    node["total"] = entry.subResults[0]["points"].asInt + entry.subResults[1]["points"].asInt
                } else if (agilityClass.template.oneOf(ClassTemplate.TRY_OUT_PENTATHLON)) {
                    node["agility1"] = entry.subResultPointsDec3(0)
                    node["jumping1"] = entry.subResultPointsDec3(1)
                    node["jumping2"] = entry.subResultPointsDec3(2)
                    node["agility2"] = entry.subResultPointsDec3(3)
                    node["speedstakes"] = entry.subResultPointsDec3(4)
                    node["total"] = entry.points.dec3
                } else if (agilityClass.template == ClassTemplate.TEAM) {
                    node["dog1"] = entry.subResultFaultsText(0)
                    node["dog2"] = entry.subResultFaultsText(1)
                    node["dog3"] = entry.subResultFaultsText(2)
                    node["relay"] = entry.subResultFaultsText(3)
                    node["team"] = entry.team.teamName
                } else if (agilityClass.template.oneOf(ClassTemplate.TEAM_INDIVIDUAL, ClassTemplate.TEAM_RELAY, ClassTemplate.KC_CRUFTS_TEAM)) {
                    node["team"] = entry.team.teamName
                } else if (agilityClass.template.oneOf(ClassTemplate.SPLIT_PAIRS)) {
                    node["pair"] = entry.team.getCompetitorDog(1) + " / " + entry.team.getCompetitorDog(2)
                } else if (agilityClass.template.oneOf(
                        ClassTemplate.GAMES_CHALLENGE,
                        ClassTemplate.TRY_OUT_GAMES,
                        ClassTemplate.UK_OPEN_GAMES
                    )
                ) {
                    node["snooker"] = entry.subResultPointsText(0)
                    node["gamblers"] = entry.subResultPointsText(1)
                    node["total"] = entry.points
                }

                node["prize"] = prizeText
                node["progressionPoints"] =
                    if (entry.progressionPoints > 0) (if (entry.progress == PROGRESS_VOID) "VOID" else entry.progressionPoints) else ""
                node["time"] = entry.timeText
                if (includeCourseTimes) {
                    node["courseTime"] = entry.courseTimeText
                }

                node["score"] =
                    if (entry.isHistoric) entry.gamesScore.toString()
                    else if (entry.agilityClass.isUka) entry.scoreText
                    else entry.points.toString()
                node["runData"] = entry.getRunData(true)
                node["highlight"] = entry.team.idDog == idDog
            }
        }
    } else {
        return error(response, 1, "not found")
    }
    return response
}

internal fun getDeleteUnpaidEntries(request: ApiRequest): Json {
    val response = createResponse(request)

    val idCompetition = request.params["idCompetition"].asInt
    val competition = Competition(idCompetition)
    if (competition.canDeleteUnpaid) {
        PlazaAdmin.cancelUnpaidShowEntries(competition)
    }

    return response
}

internal fun getCompetitionEntries(request: ApiRequest): Json {
    val idCompetition = request.params["idCompetition"].asInt
    val competition = Competition(idCompetition)
    var owing = 0

    val response = createResponse(request)

    val plazaSuperUser = response["data.user.plazaSuperUser"].asBoolean
    val ukaSuperUser = response["data.user.ukaSuperUser"].asBoolean
    val superUser = plazaSuperUser || ukaSuperUser

    loadFromTable(response["data.competition"], competition, request)
    response["data.competition.canDeleteUnpaid"] = competition.canDeleteUnpaid

    if (competition.isUkOpen) {
        var owing = 0
        LedgerItem().join { ledger }.join { competitionDog }.join { competitionDog.dog }
            .where("ledgerItem.type=10 AND ledgerItem.idCompetition=$idCompetition") {
                val node = response["data.entries"].addElement()
                node["handler"] = competitionDog.ukOpenHandler
                node["dog"] = competitionDog.dog.petName
                node["height"] = Height.getHeightName(competitionDog.ukOpenHeightCode)
                node["nation"] = competitionDog.ukOpenNation
                node["unpaid"] = ledger.amountOwing > 0
                if (ledger.amountOwing > 0) owing++
            }
        response["data.competition.owing"] = owing
        response["data.entries"].sortBy("handler")
    } else {
        val query = DbQuery(
            """
            SELECT
                competitor.givenName,
                competitor.familyName,
                competitor.idCompetitor,
                SUM(IF(ledgerItem.type IN ($LEDGER_ITEM_ENTRY, $LEDGER_ITEM_ENTRY_CREDIT),
                    ledgerItem.amount,
                    0)) AS runs,
                SUM(IF(ledgerItem.type IN ($LEDGER_ITEM_CAMPING, $LEDGER_ITEM_CAMPING_CREDIT, $LEDGER_ITEM_CAMPING_PERMIT, $LEDGER_CAMPING_DEPOSIT),
                    ledgerItem.amount,
                    0)) AS camping,
                SUM(IF(ledgerItem.type IN ($LEDGER_ITEM_POSTAGE, $LEDGER_ITEM_PAPER, $LEDGER_ITEM_PAPER_ADMIN),
                    ledgerItem.amount,
                    0)) AS postage,
                SUM(IF(ledgerItem.type IN ($LEDGER_ITEM_ENTRY_SURCHARGE, $LEDGER_ITEM_ENTRY_DISCOUNT),
                    ledgerItem.amount,
                    0)) AS surcharge,
                SUM(ledgerItem.amount) as itemAmount,
                ledger.type,
                ledger.charge,
                ledger.amount,
                ledger.dateCreated
            FROM
                ledgerItem
                    LEFT JOIN
                ledger USING (idLedger)
                    LEFT JOIN
                account ON account.idAccount = ledger.idAccount
                    LEFT JOIN
                competitor on competitor.idCompetitor = account.idCompetitor
            WHERE
                ledgerItem.idCompetition = $idCompetition and ledger.type IN ($LEDGER_ENTRY_FEES, $LEDGER_ENTRY_FEES_PAPER, $LEDGER_CAMPING_FEES, $LEDGER_CAMPING_PERMIT, $LEDGER_CAMPING_DEPOSIT, $LEDGER_CAMPING_PERMIT_PAPER)
            GROUP BY ledgerItem.idAccount
            ORDER BY competitor.givenName, competitor.familyName
        """
        )
        var totalRuns = 0
        var totalCamping = 0
        var totalPostage = 0
        var totalSurcharge = 0
        var totalExtras = 0

        while (query.next()) {
            val account = (query.getString("givenName") spaceAdd query.getString("familyName")).naturalCase
            val itemCharges = query.getInt("itemAmount")
            val runs = query.getInt("runs")
            val camping = query.getInt("camping")
            val postage = query.getInt("postage")
            val surcharge = query.getInt("surcharge")
            val extras = itemCharges - runs - camping - postage - surcharge
            val paper = query.getInt("type") == LEDGER_ENTRY_FEES_PAPER
            val charge = query.getInt("charge")
            val amount = query.getInt("amount")
            val amountOwing = maxOf(charge - amount, 0)
            val dateCreated = query.getDate("dateCreated")
            val idCompetitor = query.getInt("idCompetitor")

            totalRuns += runs
            totalCamping += camping
            totalPostage += postage
            totalSurcharge += surcharge
            totalExtras += extras

            val node = response["data.entries"].addElement()
            node["account"] = account
            node["value"] = itemCharges
            node["runs"] = runs
            node["camping"] = camping
            node["postage"] = postage
            node["surcharge"] = surcharge
            node["extras"] = extras
            node["owed"] = competition.closed && amountOwing > 0
            node["paper"] = paper
            node["date"] = dateCreated
            node["idCompetitor"] = idCompetitor
            if (competition.closed && amountOwing > 0) owing++
        }
        response["data.competition.totalRuns"] = totalRuns
        response["data.competition.totalCamping"] = totalCamping
        response["data.competition.totalPostage"] = totalPostage
        response["data.competition.totalSurcharge"] = totalSurcharge
        response["data.competition.totalExtras"] = totalExtras
        response["data.competition.totalFees"] = totalRuns + totalCamping + totalPostage
        response["data.competition.owing"] = owing
        response["data.entries"].sortBy("account")
    }

    return response
}

internal fun getCompetitionHelpers(request: ApiRequest): Json {
    val idCompetition = request.params["idCompetition"].asInt
    val competition = Competition(idCompetition)

    val response = createResponse(request)
    loadFromTable(response["data.competition"], competition, request)
    var helpingDate = competition.dateStart
    while (helpingDate <= competition.dateEnd) {
        response["data.competition.helpingDates"].addElement().setValue(helpingDate)
        helpingDate = helpingDate.addDays(1)
    }

    CompetitionCompetitor().join { competitor }.where("idCompetition=$idCompetition", "givenName, familyName") {
        var helping = false
        var judges = ""
        for (help in helpDays) {
            if (help.has("judge")) {
                judges = judges.append(help["judge"].asString, ", ")
            }
            if (help.has("judge") || help.has("am") || help.has("pm")) {
                helping = true
            }
        }
        if (helping || voucherCode.isNotEmpty()) {
            val node = response["data.competitors"].addElement()
            node["name"] = competitor.fullName
            node["voucherCode"] = voucherCode
            node["helpGroup"] = helpGroup
            node["helpDays"] = helpDays
            node["judges"] = judges
        }
    }
    return response
}

fun putCompetitionCamping(body: Json, idCompetition: Int): Json {
    val response = createResourceResponse()
    val bookings = body["bookings"]

    for (booking in bookings) {
        val idAccount = booking["idAccount"].asInt
        val confirmed = booking["confirmed"].asBoolean
        if (confirmed) {
            Camping().where("idCompetition=$idCompetition AND idAccount=$idAccount") {
                acceptBooking(force = true)
            }
        }
    }
    return response
}

fun getHelpOffers(idCompetition: Int): Map<Int, String> {
    val result = HashMap<Int, String>()
    CompetitionCompetitor().join { competitor }
        .where("idCompetition=$idCompetition", "competitor.idAccount")
        {
            val offer = helpOffer
            if (offer.isNotEmpty()) {
                result[idAccount] =
                    result.getOrDefault(idAccount, "").append("${competitor.givenName.naturalCase}: $offer", ". ")
            }
        }
    return result
}

internal fun getCompetitionCamping(request: ApiRequest): Json {
    val idCompetition = request.params["idCompetition"].asInt
    val waiting = request.query["waiting"].asBoolean
    val competition = Competition(idCompetition)


    val help = if (waiting) getHelpOffers(idCompetition) else HashMap<Int, String>()

    val response = createResponse(request, "competitionCamping", "idCompetition=$idCompetition")
    loadFromTable(response["data.competition"], competition, request)

    response["data.competition.campingCapSystem"] = campingCapToText(competition.campingCapSystem)
    response["data.competition.hasManagedCamping"] = competition.hasManagedCamping

    var applications = 0
    var accepted = 0
    var cancelCount = 0

    var campingDate = competition.campingFirst
    while (campingDate <= competition.campingLast) {
        response["data.competition.campingDates"].addElement().setValue(campingDate)
        campingDate = campingDate.addDays(1)
    }

    var hasGroups = false
    var rank = 1
    val where = "idCompetition = $idCompetition" + if (waiting) " AND NOT confirmed" else ""
    Camping()
        .join { account }
        .join { account.competitor }
        .where(
            where, "camping.dateAccepted"
        ) {
            applications++
            if (confirmed) accepted++
            if (cancelled) cancelCount++
            val node = response["data.bookings"].addElement()
            node["idAccount"] = idAccount
            node["idCompetitor"] = account.competitor.id
            node["camper"] = account.competitor.fullName
            node["dayFlags"] = dayFlags
            node["days"] = days
            node["confirmed"] = confirmed
            node["pending"] = pending
            node["rejected"] = rejected
            node["priority"] = priority
            node["cancelled"] = cancelled
            node["pitchType"] = pitchType
            node["groupName"] = groupName
            if (waiting) node["help"] = help.getOrDefault(idAccount, "")
            node["rank"] = rank++
            if (groupName.isNotEmpty()) hasGroups = true
        }

    if (!waiting) response["data.bookings"].sortBy("camper")

    response["data.competition.hasGroups"] = hasGroups
    response["data.competition.campingApplications"] = applications
    response["data.competition.campingAccepted"] = accepted
    response["data.competition.campingCancelled"] = cancelCount
    return response
}

internal fun getCompetitionResults(request: ApiRequest): Json {
    val thisYear = today.format("yyyy").toInt()
    val year = if (request.query["year"].asInt > 0) request.query["year"].asInt else thisYear

    val response = createResponse(request)
    var yearNode = response["data.years"]
    yearNode["thisYear"] = year
    for (year1 in 2004..thisYear) {
        val node = yearNode["all"].addElement()
        node.setValue(year1)
    }

    var query = DbQuery(
        """
        SELECT DISTINCT
            idCompetition, name, dateStart, dateEnd, competition.idOrganization
        FROM
            agilityclass
                JOIN
            competition USING (idCompetition)
        WHERE
            classProgress IN ($CLASS_CLOSED, $CLASS_HISTORIC) AND YEAR(competition.dateStart)=$year ${accessCondition(
            request
        )}
        ORDER BY competition.dateStart
    """
    )

    var monthNode = response["data.months"]
    var month = ""
    if (query.rowCount > 0) {
        while (query.next()) {
            var thisMonth = query.getDate("dateStart").format("MMM yyyy")
            if (thisMonth != month) {
                monthNode = response["data.months"].addElement()
                monthNode["baseDate"] = query.getDate("dateStart")
                month = thisMonth
            }
            val node = monthNode["competitions"].addElement()
            node["idCompetition"] = query.getInt("idCompetition")
            node["idOrganization"] = query.getInt("idOrganization")
            node["name"] = query.getString("name")
            node["dateStart"] = query.getDate("dateStart")
            node["dateEnd"] = query.getDate("dateEnd")
        }
    } else {
        return error(response, 1, "not found")
    }
    return response
}

internal fun getCompetitionResultsByDate(request: ApiRequest): Json {
    val idCompetition = request.params["idCompetition"].asInt

    val competition = Competition()
    competition.find(idCompetition)
    val agilityClass = AgilityClass()

    val orderBy =
        if (competition.isUka)
            "classDate DESC, gradeCodes, classCode, suffix"
        else
            "classDate DESC, classNumber, classNumberSuffix, part"
    agilityClass.select("idCompetition=$idCompetition AND (classProgress>=$CLASS_PREPARING)", orderBy)


    val response = createResponse(request)
    loadFromTable(response["data.competition"], competition, request)
    var thisDate = nullDate
    var dateNode = Json.nullNode()
    while (agilityClass.next()) {
        if (agilityClass.date != thisDate) {
            thisDate = agilityClass.date
            dateNode = response["data.competition.dates"].addElement()
            dateNode["classDate"] = thisDate
        }
        val node = dateNode["agilityClasses"].addElement()
        loadFromTable(node, agilityClass, request)
        if (competition.isInProgress) {
            node["inProgress"] = agilityClass.progress < CLASS_CLOSED
            node["presented"] = agilityClass.presented
            if (agilityClass.presentingTime.isNotEmpty() && !agilityClass.presented && agilityClass.date == today) {
                node["presentingTime"] = agilityClass.presentingTime.timeText
            }
        }
    }


    return response
}

enum class HeightState { PENDING, ACTIVE, CLOSED }

internal fun getCompetitionRingPlanDynamic(request: ApiRequest): Json {
    val idCompetition = request.params["idCompetition"].asInt
    val date = request.params["date"].asDate
    var dateFound = false
    val competition = Competition()
    val response = createResponse(request)

    if (competition.find(idCompetition)) {
        loadFromTable(response["data.competition"], competition, request)
        if (competition.isUka) {
            response["data.competition.isUka"] = true
        }
        if (competition.isUkOpen) {
            response["data.competition.isUkOpen"] = true
        }

        for (day in competition.agilityClassDates.split(",")) {
            if (day.toDate() > date && !dateFound) {
                val element = response["data.competition.days"].addElement()
                element.setValue(date)
                dateFound = true
            } else if (day.toDate() == date) {
                dateFound = true
            }
            val element = response["data.competition.days"].addElement()
            element.setValue(day.toDate())
        }
    }

    var radioMap = HashMap<Int, String>()

    Radio().join { agilityClass }.where(
        "radio.idCompetition=$idCompetition AND radio.dateCreated>${now.addMinutes(-10).sqlDateTime}",
        "radio.dateCreated DESC"
    ) {
        val sinceSeconds = ((now.time - dateCreated.time) / 1000).toInt()
        val sinceMinutes = (sinceSeconds + 30) / 60
        val classText = if (messageTemplate.oneOf(RadioTemplate.NOT_BREAKING.code, RadioTemplate.LUNCH_BETWEEN.code))
            "" else "class ${agilityClass.number} - "
        val message = if (sinceMinutes == 0)
            radioMap.getOrDefault(ringNumber, "").append("$classText$fullText", ".....")
        else
            radioMap.getOrDefault(
                ringNumber,
                ""
            ).append("$sinceMinutes min ago: $classText$fullText", ".....")
        radioMap[ringNumber] = message
    }


    val query =
        DbQuery(
            """
                SELECT
                    agilityClass.idAgilityClass,
                    agilityClass.ringNumber,
                    agilityClass.className,
                    agilityClass.classProgress,
                    agilityClass.walkingOverLunch,
                    agilityClass.startTime,
                    agilityClass.groupRunningOrder,
                    agilityClass.heightRunningOrder,
                    agilityClass.heights,
                    agilityClass.judge AS classJudge,
                    agilityClass.classCode,
                    ring.judge,
                    ring.idAgilityClass=agilityClass.idAgilityClass AS isRingClass,
                    ring.group as ringGroup,
                    ring.heightCode as ringJumpHeightCode,
                    ring.note as ringNote,
                    ring.runningOrder as runningOrder,
                    ring.runner as runner,
                    ring.runnerHeightCode as runnerHeightCode,
                    ring.lunchStart,
                    ring.lunchEnd,
                    ring.notBreaking,
                    entry.group,
                    entry.jumpHeightCode,
                    sum(if(progress<=$PROGRESS_DELETED_LOW, 1, 0)) as entered,
                    sum(if(progress<$PROGRESS_RUNNING, 1, 0)) as waitingFor,
                    sum(if(progress IN ($PROGRESS_RUNNING, $PROGRESS_RUN), 1, 0)) as run,
                    sum(if(progress=$PROGRESS_BOOKED_IN, 1, 0)) as bookedIn,
                    sum(if(progress=$PROGRESS_CHECKED_IN, 1, 0)) as checkedIn
                FROM
                    agilityClass
                    LEFT JOIN entry USING (idAgilityClass)
                    LEFT JOIN ring ON
                        ring.idCompetition=agilityClass.idCompetition AND
                        ring.date=agilityClass.classDate AND
                        ring.ringNumber=agilityClass.ringNumber
                WHERE
                    agilityClass.idCompetition = ${competition.id} AND
                    agilityClass.classDate = ${date.sqlDate} AND
                    agilityClass.ringNumber > 0
                GROUP BY
                    agilityClass.ringNumber,
                    agilityClass.ringOrder,
                    agilityClass.idAgilityClass,
                    FIND_IN_SET(entry.group, agilityClass.groupRunningOrder),
                    FIND_IN_SET(entry.jumpHeightCode, agilityClass.heightRunningOrder)
            """
        )

    var agilityClassNode = Json.nullNode()
    var ringNode = Json.nullNode()
    var classActive = false
    val testDate = /* if (Global.quartzTest) control.effectiveDate else */ today

    val ring = ChangeMonitor<Int>(-1)
    val agilityClass = ChangeMonitor<Int>(-1)
    val ringPlan = ChangeMonitor<Int>(-1)
    var lho: Boolean

    if (query.rowCount == 0) {
        response["data.ringPlan.name"] = competition.name
        response["data.ringPlan.date"] = date
        response["data.ringPlan.isToday"] = date == testDate
        response["data.ringPlan.restDay"] = true
    } else {
        while (query.next()) {
            val idAgilityClass = query.getInt("idAgilityClass")
            val ringNumber = query.getInt("ringNumber")
            val className = query.getString("className")
            val classCode = query.getInt("classCode")
            val classProgress = query.getInt("classProgress")
            val walkingOverLunch = query.getBoolean("walkingOverLunch")
            val startTime = query.getDate("startTime")
            val groupRunningOrder = query.getString("groupRunningOrder")
            val heightRunningOrder = query.getString("heightRunningOrder")
            val heights = Json(query.getString("heights"))
            val judge = query.getString("judge")
            val classJudge = query.getString("classJudge")
            val isRingClass = query.getBoolean("isRingClass")
            val ringGroup = query.getString("ringGroup")
            val ringJumpHeightCode = query.getString("ringJumpHeightCode")
            var ringNote = query.getString("ringNote")

            val lunchStart = query.getDate("lunchStart")
            val lunchEnd = query.getDate("lunchEnd")
            val notBreaking = query.getBoolean("notBreaking")

            val runningOrder = query.getInt("runningOrder")
            val runner = query.getString("runner")
            val runnerHeightCode = query.getString("runnerHeightCode")

            val group = query.getString("group")
            val jumpHeightCode = query.getString("jumpHeightCode")
            val entered = query.getInt("entered")
            val waitingFor = query.getInt("waitingFor")
            val bookedIn = query.getInt("bookedIn")
            val checkedIn = query.getInt("checkedIn")
            val run = query.getInt("run")

            val template = ClassTemplate.select(classCode)

            if (ringPlan.hasChanged(0)) {
                response["data.ringPlan.name"] = competition.name
                response["data.ringPlan.announcement"] = competition.announcement
                response["data.ringPlan.date"] = date
                response["data.ringPlan.isToday"] = date == testDate
            }
            if (ring.hasChanged(ringNumber)) {
                val closedForLunch = classProgress == CLASS_CLOSED_FOR_LUNCH || walkingOverLunch
                val lunchNote = when {
                    lunchStart.isNotEmpty() && lunchEnd.isNotEmpty() && lunchEnd > now.addMinutes(-10) && !closedForLunch -> "Breaking for lunch between ${lunchStart.timeText} and ${lunchEnd.timeText}"
                    lunchStart.isNotEmpty() && lunchStart > now.addMinutes(-30) && !closedForLunch -> "Breaking for lunch at ${lunchStart.timeText}"
                    notBreaking && !walkingOverLunch -> "Not breaking for lunch"
                    else -> ""
                }
                ringNote = ringNote.append(lunchNote)

                ringNode = response["data.ringPlan.rings"].addElement()
                ringNode["ringNumber"] = ring.value
                ringNode["judge"] = judge
                ringNode["note"] = ringNote
                ringNode["closed"] = true
                ringNode["radio"] = radioMap.getOrDefault(ringNumber, "")
            }
            if (agilityClass.hasChanged(idAgilityClass)) {
                classActive = date == testDate && isRingClass
                val classState =
                    if (classActive) {
                        when (classProgress) {
                            CLASS_PENDING, CLASS_PREPARING -> "Setting Up"
                            CLASS_WALKING -> "Walking"
                            CLASS_CLOSED_FOR_LUNCH -> "Closed for Lunch"
                            CLASS_RUNNING -> ""
                            else -> "Closed"
                        }
                    } else if (date < testDate || classProgress >= CLASS_CLOSED) {
                        "Closed"
                    } else {
                        ""
                    }
                if (classProgress < CLASS_CLOSED) {
                    ringNode["closed"] = false
                }
                lho = AgilityClass.isLho(heightRunningOrder)
                var info =
                    AgilityClass.getInfo(classProgress, startTime, walkingOverLunch, heights, resumeTime = lunchEnd)
                if (runningOrder > 0 && runnerHeightCode == ringJumpHeightCode && classProgress == CLASS_RUNNING) {
                    info = info.append("last runner: $runningOrder - $runner")
                }
                agilityClassNode = ringNode["agilityClasses"].addElement()
                agilityClassNode["idAgilityClass"] = idAgilityClass
                agilityClassNode["className"] = className
                agilityClassNode["status"] = classState
                agilityClassNode["active"] = classActive
                agilityClassNode["hasGroups"] = groupRunningOrder.isNotEmpty()
                agilityClassNode["entered"] = 0
                agilityClassNode["run"] = 0
                agilityClassNode["judge"] = if (classJudge.isEmpty()) judge else classJudge
                agilityClassNode["info"] = info.capitalize()


                for (letter in groupRunningOrder.split(",")) {
                    val groupNode = agilityClassNode["groups"].addElement()
                    groupNode["group"] = letter
                    for (jumpHeight in heightRunningOrder.split(",")) {
                        val heightNode = groupNode["heights"].addElement()
                        heightNode["heightCode"] = jumpHeight
                        heightNode["name"] =
                            if (lho || template.isFab) Height.getHeightJumpName(jumpHeight) else Height.getHeightShort(jumpHeight)
                        heightNode["entered"] = 0
                    }
                }

            }
            val activeHeight =
                classActive && (ringGroup == group) && (ringJumpHeightCode == jumpHeightCode) && classProgress < CLASS_CLOSED

            agilityClassNode["entered"] = agilityClassNode["entered"].asInt + entered
            agilityClassNode["run"] = agilityClassNode["run"].asInt + run

            if (jumpHeightCode.isNotEmpty()) {
                val groupNode = agilityClassNode["groups"].searchElement("group", group)
                val heightNode = groupNode["heights"].searchElement("heightCode", jumpHeightCode)
                heightNode["active"] = activeHeight
                heightNode["entered"] = entered
                heightNode["waitingFor"] = waitingFor
                heightNode["bookedIn"] = bookedIn
                heightNode["checkedIn"] = checkedIn
                heightNode["run"] = run
                heightNode["notRun"] = entered - run
                heightNode["text"] =
                    if (competition.isUka) {
                        when (classProgress) {
                            CLASS_PENDING, CLASS_PREPARING, CLASS_WALKING -> "($entered)"
                            CLASS_CLOSED_FOR_LUNCH, CLASS_RUNNING -> "(${bookedIn + checkedIn})"
                            else -> "($run)"
                        }
                    } else if (competition.isUkOpen) {
                        if (run > 0) "($run/$entered)" else "($entered)"
                    } else {
                        ""
                    }

                heightNode["active"] = activeHeight
                for (height in heights) {
                    if (height["heightCode"].asString == jumpHeightCode) {
                        heightNode["callingTo"] = height["callingTo"].asInt
                    }
                }
            }
        }
    }
    return response
}


internal fun getCompetitionRingPlan(request: ApiRequest): Json {
    val idCompetition = request.params["idCompetition"].asInt
    val date = request.params["date"].asDate
    val competition = Competition()
    val response = createResponse(request)

    if (competition.find(idCompetition)) {
        if (competition.isUka) {
            return getCompetitionRingPlanUka(request, competition)
        }
        loadFromTable(response["data.competition"], competition, request)
        for (day in competition.agilityClassDates.split(",")) {
            val element = response["data.competition.days"].addElement()
            element.setValue(day.toDate())
        }
    }

    val statsQuery =
        DbQuery(
            """
                SELECT
                    agilityClass.idAgilityClass,
                    agilityClass.ringNumber,
                    agilityClass.className,
                    agilityClass.classProgress,
                    agilityClass.walkingOverLunch,
                    agilityClass.startTime,
                    agilityClass.idAgilityClass,
                    agilityClass.heightRunningOrder,
                    entry.jumpHeightCode,
                    sum(1) as entered,
                    sum(if(progress<=$PROGRESS_RUN, 1, 0)) as enteredToRun,
                    sum(if(progress IN ($PROGRESS_RUNNING, $PROGRESS_RUN), 1, 0)) as run,
                    sum(if(progress=$PROGRESS_BOOKED_IN, 1, 0)) as bookedIn,
                    sum(if(progress=$PROGRESS_CHECKED_IN, 1, 0)) as checkedIn
                FROM
                    entry
                    LEFT JOIN agilityClass USING (idAgilityClass)
                WHERE
                    agilityClass.idCompetition = ${competition.id} AND agilityClass.classDate = ${date.sqlDate} AND NOT agilityClass.paperBased
                GROUP BY agilityClass.ringNumber, agilityClass.ringOrder, agilityClass.idAgilityClass, FIND_IN_SET(entry.jumpHeightCode, agilityClass.heightRunningOrder)
            """
        )

    var agilityClassNode = Json.nullNode()
    var ringNode = Json.nullNode()
    var idAgilityClass = 0
    var first = true
    var active = false
    var heightState = HeightState.PENDING
    var heightCount = 0
    val ring = Ring()
    while (statsQuery.next()) {
        if (first) {
            response["data.ringPlan.name"] = competition.name
            response["data.ringPlan.announcement"] = competition.announcement
            response["data.ringPlan.date"] = date
            response["data.ringPlan.isToday"] = date == today
            first = false
        }
        if (statsQuery.getInt("ringNumber") != ring.number) {
            ring.seek(competition.id, date, statsQuery.getInt("ringNumber"))
            ringNode = response["data.ringPlan.rings"].addElement()
            ringNode.loadFromDataset(ring, "ringNumber,judge,note")
            ringNode["closed"] = true
        }
        if (statsQuery.getInt("idAgilityClass") != idAgilityClass) {
            active = ring.idAgilityClass == statsQuery.getInt("idAgilityClass")
            heightState = HeightState.PENDING
            idAgilityClass = statsQuery.getInt("idAgilityClass")
            agilityClassNode = ringNode["agilityClasses"].addElement()
            agilityClassNode["idAgilityClass"] = statsQuery.getInt("idAgilityClass")
            agilityClassNode["className"] = statsQuery.getString("className")

            heightCount = 1
            val cursor = statsQuery.cursor
            while (statsQuery.next() && statsQuery.getInt("idAgilityClass") == idAgilityClass) {
                heightCount++
            }
            statsQuery.cursor = cursor

            var status = ""
            var notes = ""
            when (statsQuery.getInt("classProgress")) {
                CLASS_PENDING -> {
                    if (active) {
                        ringNode["closed"] = false
                        status = "Setting Up"
                    }
                }
                CLASS_PREPARING -> {
                    if (active) {
                        ringNode["closed"] = false
                        status = "Setting Up"
                    }
                }
                CLASS_WALKING -> {
                    if (active) {
                        ringNode["closed"] = false
                        status = "Walking"
                        notes =
                            (if (statsQuery.getBoolean("walkingOverLunch")) "Walking over lunch, starting" else "Starting") +
                                    " at ${statsQuery.getDate("startTime").timeText}"
                    }
                }
                CLASS_CLOSED_FOR_LUNCH -> {
                    if (active) {
                        ringNode["closed"] = false
                        status = ""
                        notes = "Closed for lunch, resuming at ${statsQuery.getDate("startTime").timeText}"
                    }
                }
                CLASS_RUNNING -> {
                    if (active) {
                        ringNode["closed"] = false
                        status = ""
                    }
                }
                CLASS_CLOSED -> {
                    status = "Closed"
                    active = false
                    heightState = HeightState.CLOSED
                }
                CLASS_HISTORIC -> {
                    status = "Historic"
                    active = false
                    heightState = HeightState.CLOSED
                }

            }
            agilityClassNode["status"] = status
            agilityClassNode["notes"] = notes
            agilityClassNode["active"] = active
        }
        if (active) {
            if (ring.heightCode == statsQuery.getString("jumpHeightCode")) {
                heightState = HeightState.ACTIVE
            } else if (statsQuery.getInt("run") > 0) {
                heightState = HeightState.CLOSED
            } else {
                heightState = HeightState.PENDING
            }
        }
        val node = agilityClassNode["heights"].addElement()
        node["heightCode"] = statsQuery.getString("jumpHeightCode")
        if (heightCount > 1 || competition.isUka) {
            val jumpHeight = statsQuery.getString("jumpHeightCode")
            val heightRunningOrder = statsQuery.getString("heightRunningOrder")
            val lho = AgilityClass.isLho(heightRunningOrder)
            node["name"] = if (lho) Height.getHeightJumpName(jumpHeight) else Height.getHeightShort(jumpHeight)
        }
        node["text"] =
            if (competition.isUka) {
                when (heightState) {
                    HeightState.PENDING -> "(e${statsQuery.getInt("entered")})"
                    HeightState.ACTIVE -> "(w${statsQuery.getInt("bookedIn") + statsQuery.getInt("checkedIn")})"
                    HeightState.CLOSED -> "(r${statsQuery.getInt("run")})"
                }

            } else {
                when (heightState) {
                    HeightState.PENDING -> "${statsQuery.getInt("enteredToRun")} entered"
                    HeightState.ACTIVE -> {
                        if (statsQuery.getInt("run") == 0) {
                            "${statsQuery.getInt("enteredToRun")} entered"
                        } else {
                            "${statsQuery.getInt("run")} run, waiting for ${statsQuery.getInt("enteredToRun") - statsQuery.getInt(
                                "run"
                            )}"
                        }
                    }
                    HeightState.CLOSED -> "Closed ${statsQuery.getInt("run")} ran out of ${statsQuery.getInt("enteredToRun")}"
                }
            }
        node["active"] = active && ring.heightCode == statsQuery.getString("jumpHeightCode")
    }
    return response
}

internal fun getCompetitionRingPlanUka(request: ApiRequest, competition: Competition): Json {
    val isSummary = request.query["summary"].asBoolean
    val date = request.params["date"].asDate


    val response = createResponse(request)
    loadFromTable(response["data.competition"], competition, request)
    if (competition.isUka) {
        response["data.competition.isUka"] = true
    }

    val countQuery =
        if (competition.isUka)
            DbQuery(
                """
                    SELECT
                        agilityClass.idAgilityClass,
                        entry.heightCode,
                        count(*) as count
                    FROM
                        agilityClass
                        LEFT JOIN entry ON
                            entry.idAgilityClass = agilityClass.idAgilityClass
                            AND (
                                classProgress = $CLASS_PENDING
                                OR classProgress = $CLASS_CLOSED AND entry.Progress = $PROGRESS_RUN
                                OR entry.Progress IN ($PROGRESS_BOOKED_IN, $PROGRESS_CHECKED_IN, $PROGRESS_RUNNING)
                            )
                    WHERE
                        agilityClass.idCompetition = ${competition.id} AND agilityClass.classDate = ${date.sqlDate}
                    GROUP BY agilityClass.idAgilityClass, entry.heightCode
                """
            )
        else
            DbQuery(
                """
                    SELECT
                        agilityClass.idAgilityClass,
                        entry.jumpHeightCode AS heightCode,
                        count(*) as count
                    FROM
                        agilityClass
                        LEFT JOIN entry ON
                            entry.idAgilityClass = agilityClass.idAgilityClass
                            AND (
                                (classProgress < $CLASS_WALKING ) OR
                                (classProgress BETWEEN $CLASS_WALKING AND $CLASS_RUNNING AND entry.Progress IN ($PROGRESS_CHECKED_IN)) OR
                                (classProgress >=$CLASS_CLOSED AND entry.Progress IN ($PROGRESS_RUN))
                            )
                    WHERE
                        agilityClass.idCompetition = ${competition.id} AND agilityClass.classDate = ${date.sqlDate} AND NOT agilityClass.paperBased
                    GROUP BY agilityClass.idAgilityClass, entry.jumpHeightCode
                """
            )

    val queueMap = HashMap<Int, HashMap<String, Int>>()

    while (countQuery.next()) {
        val id = countQuery.getInt("idAgilityClass")
        val heightCode = countQuery.getString("heightCode")
        val count = countQuery.getInt("count")
        if (!queueMap.containsKey(id)) {
            queueMap.put(id, HashMap<String, Int>())
        }
        queueMap[id]?.put(heightCode, count)
    }

    val ring = Ring()
    val agilityClass = AgilityClass()
    agilityClass.select(
        "idCompetition = ${competition.id} AND classDate = ${date.sqlDate} AND NOT paperBased",
        "ringNumber, ringOrder, idAgilityClass"
    )

    if (agilityClass.rowCount == 0) {
        return error(response, 1, "not found")
    }
    var agilityClassNode: JsonNode
    var ringNode = Json.nullNode()
    var idAgilityClass = 0
    var first = true
    var active: Boolean
    while (agilityClass.next()) {
        if (first) {
            response["data.ringPlan.name"] = competition.name
            response["data.ringPlan.announcement"] = competition.announcement
            response["data.ringPlan.date"] = date
            response["data.ringPlan.isToday"] = date == today
            response["data.ringPlan.summary"] = isSummary
            first = false
        }
        if (agilityClass.ringNumber != ring.number) {
            ring.seek(competition.id, date, agilityClass.ringNumber)
            ringNode = response["data.ringPlan.rings"].addElement()
            ringNode.loadFromDataset(ring, "ringNumber,judge,note")
            ringNode["closed"] = true
        }
        if (agilityClass.id != idAgilityClass) {
            active = ring.idAgilityClass == agilityClass.id

            if (!isSummary || (active && agilityClass.progress < CLASS_CLOSED)) {
                idAgilityClass = agilityClass.id
                agilityClassNode = ringNode["agilityClasses"].addElement()
                agilityClassNode["idAgilityClass"] = agilityClass.id
                agilityClassNode["className"] = agilityClass.name
                if (!agilityClass.isLast) {
                    val cursor = agilityClass.cursor
                    var done = false
                    var next = ""
                    do {
                        agilityClass.next()
                        if (agilityClass.getInt("ringNumber") == ring.number) {
                            next = next.append(agilityClass.name)
                        } else {
                            done = true
                        }
                    } while (!agilityClass.isLast && !done)
                    agilityClassNode["next"] = next
                    agilityClass.cursor = cursor
                } else {
                    agilityClassNode["next"] = "Last Class"
                }


                var status = ""
                var notes = ""
                when (agilityClass.progress) {
                    CLASS_PENDING -> {
                        if (active) {
                            ringNode["closed"] = false
                            status = "Setting Up"
                        }
                    }
                    CLASS_PREPARING -> {
                        if (active) {
                            ringNode["closed"] = false
                            status = "Setting Up"
                        }
                    }
                    CLASS_WALKING -> {
                        if (active) {
                            ringNode["closed"] = false
                            status = "Walking"
                            notes =
                                (if (agilityClass.walkingOverLunch) "Walking over lunch, starting" else "Starting") +
                                        " at ${agilityClass.startTime.timeText}"
                        }
                    }
                    CLASS_CLOSED_FOR_LUNCH -> {
                        if (active) {
                            ringNode["closed"] = false
                            status = ""
                            notes = "Closed for lunch, resuming at ${agilityClass.startTime.timeText}"
                        }
                    }
                    CLASS_RUNNING -> {
                        if (active) {
                            ringNode["closed"] = false
                            status = ""
                        }
                    }
                    CLASS_CLOSED -> {
                        status = "Closed"
                        active = false
                    }
                    CLASS_HISTORIC -> {
                        status = "Historic"
                        active = false
                    }

                }
                agilityClassNode["status"] = status
                agilityClassNode["notes"] = notes
                agilityClassNode["active"] = active
                val heightMap = queueMap[agilityClass.id]
                if (heightMap != null) {
                    if (agilityClass.isUka) {
                        val heightCodes = agilityClass.heightRunningOrder.split(",")
                        for (heightCode in heightCodes) {
                            val node = agilityClassNode["heights"].addElement()
                            node["name"] = Height.getHeightName(heightCode)
                            node["text"] = "(${heightMap[heightCode] ?: 0})"
                            node["active"] = active && ring.heightCode == heightCode
                        }
                    } else {
                        val heights = agilityClass.appHeightProgress(heightMap)
                        for (height in heights) {
                            val node = agilityClassNode["heights"].addElement()
                            node["heightCode"] = height["heightCode"].asString
                            if (heights.size > 1) {
                                node["name"] = height["caption"].asString
                            }
                            node["text"] = height["progress"].asString
                            node["active"] = active && ring.heightCode == height["heightCode"].asString
                        }
                    }
                }
            }
        }
    }
    return response
}

internal fun getNull(request: ApiRequest): Json {
    val response = createResponse(request)
    return response
}

internal fun getPing(request: ApiRequest): Json {
    val response = Json()
    response["kind"] = "ping"
    response["ip"] = request.remoteAddress
    return response
}

internal fun getAccount(request: ApiRequest): Json {
    val idAccount = request.params["idAccount"].asInt
    val account = Account()
    account.country.joinToParent()
    val response = createResponse(request, "account", "idAccount=$idAccount")
    if (account.find(idAccount)) {
        val competitor = Competitor()
        competitor.select("idAccount=$idAccount", "givenName, FamilyName")
        while (competitor.next()) {
            WebTransaction.addOption(response["data.competitors"], competitor.id.toString(), competitor.fullName)
        }
        val node = response["data.account"]
        loadFromTable(node, account, request)
        response["data.card.rate"] = account.cardRate
        response["data.card.fixed"] = account.cardFixed
        node["country.countyCode"] = account.countryCode
        loadFromTable(node["country"], account.country, request)
    } else {
        return error(response, 1, "Account not found")
    }
    return response
}

internal fun getCompetitor(request: ApiRequest): Json {
    val idCompetitor = request.params["idCompetitor"].asInt
    val unVerifiedEmail = request.query["unVerifiedEmail"].asBoolean
    val setPassword = request.query["set_password"].asBoolean
    val registrationComplete = request.query["registrationComplete"].asBoolean

    val competitor = Competitor()
    competitor.account.joinToParent()
    competitor.account.country.joinToParent()
    val response =
        createResponse(request, if (setPassword) "resetPassword" else "competitor", "idCompetitor=$idCompetitor")
    if (competitor.find(idCompetitor)) {
        if (registrationComplete) {
            competitor.registrationComplete = true
            val idAccount = competitor.getAccountID()
            competitor.post()
            val a = Account(idAccount)
            a.registrationComplete = true
            a.post()
        }
        if (setPassword) {
            response["data.idCompetitor"] = competitor.id
        }
        val node = response["data.competitor"]
        loadFromTable(node, competitor, request)
        val token = competitor.generateRegistrationToken()
        node["quickLink"] = "http://agilityplaza.com/register_uka?token=$token"
        node["country.countyCode"] = competitor.account.countryCode
        loadFromTable(node["country"], competitor.account.country, request)
        for (char in competitor.account.country.addressFormat.replace("|", "")) {
            node["country.addressElements"].addElement().setValue(char.toString())
        }
        if (unVerifiedEmail) {
            node["unVerifiedEmail"] = competitor.unVerifiedEmail
        }
    } else {
        return error(response, 1, "Competitor not found")
    }
    return response
}

internal fun getControl(request: ApiRequest, response: Json) {
    val access = request.query["access"].asInt
    val idCompetitor = firstNonZero(request.params["idCompetitor"].asInt, request.query["idCompetitor"].asInt)
    val idCompetitorReal =
        firstNonZero(request.params["idCompetitorReal"].asInt, request.query["idCompetitorReal"].asInt)

    if (idCompetitor > 0) {
        val competitor = Competitor(idCompetitor)
        val competitorReal =
            if (idCompetitorReal > 0 && idCompetitorReal != idCompetitor) Competitor(idCompetitorReal) else competitor

        val competitorNode = response["data.user"]
        competitorNode["idCompetitor"] = competitor.id
        competitorNode["idAccount"] = competitor.idAccount
        competitorNode["givenName"] = competitor.givenName
        competitorNode["fullName"] = competitor.fullName
        competitorNode["competitionList"] = CompetitionOfficial.competitionList(competitor.id)
        competitorNode["isUkaRegistered"] = competitor.isUkaRegisteredOldRule
        competitorNode["ukaSuperUser"] = competitorReal.ukaSuperUser
        competitorNode["plazaSuperUser"] = competitorReal.plazaSuperUser

        competitorNode["systemAdministrator"] = competitorReal.systemAdministrator
        competitorNode["access"] = access
        competitorNode["spoofed"] = idCompetitorReal > 0 && idCompetitorReal != idCompetitor
        if (Global.quartzTest) competitorNode["testMode"] = true
    }
}


internal fun getResetPassword(request: ApiRequest): Json {
    val response = createResponse(request, "resetPassword")
    val token = decryptJson(request.params["token"].asString)
    val email = token["email"].asString

    val competitor = Competitor.select("email=${email.quoted} AND idAccount>0")


    response["data.email"] = email
    while (competitor.next()) {
        if (competitor.rowCount == 1) {
            response["data.idCompetitor"] = competitor.id
        }
        val node = response["data.competitors"].addElement()
        node["idCompetitor"] = competitor.id
        node["name"] = competitor.fullName
        node["code"] = competitor.code
    }
    return response
}

fun putResetPassword(body: Json): Json {
    val idCompetitor = body["idCompetitor"].asInt
    val password1 = body["password1"].asString
    val password2 = body["password2"].asString


    val response = createResourceResponse()

    if (password1.isEmpty()) {
        addDataError(response, "password1", "You must choose a new password")
    } else if (password2.isEmpty()) {
        addDataError(response, "password2", "You must re-enter your password")
    } else if (password1 != password2) {
        addDataError(response, "password2", "Your passwords do not match")
    } else if (password1.length < 8) {
        addDataError(response, "password1", "Must be at least 8 characters")
    } else if (!password1.contains(Regex("[a-zA-Z]"))) {
        addDataError(response, "password1", "Must contain at least one letter")
    } else if (!password1.contains(Regex("[0-9]"))) {
        addDataError(response, "password1", "Must contain at least one digit (0-9)")
    }
    if (response.has("dataErrors")) {
        return error(response, 1, "Please correct the errors and try again")
    }

    val competitor = Competitor(idCompetitor)
    if (competitor.first()) {
        competitor.password = password1
        competitor.post()
    }

    return response
}

internal fun getRequestResetPassword(request: ApiRequest): Json {
    var response = createResponse(request, "requestResetPassword")
    response["data.email"] = ""
    return response
}


internal fun putRequestResetPassword(body: Json): Json {
    var response = createResourceResponse()
    val email = body["email"].asString
    val host = body["host"].asString
    response["control.data.email"] = email

    if (email.isNotEmpty()) {
        val competitor = Competitor.select("email=${email.quoted} AND idAccount>0 AND registrationComplete")
        generateResetPasswordEmail(host, "password_reset", email, competitor.rowCount > 0)
    }
    return response
}

internal fun getCompetitorEmail(request: ApiRequest): Json {
    val idCompetitor = request.params["idCompetitor"].asInt

    val competitor = Competitor()
    competitor.find(idCompetitor)
    var response = createResponse(request, "competitorEmail", "idCompetitor=${competitor.id}")
    response["data.email"] = competitor.email
    return response
}

internal fun getCompetitorUkaLedger(request: ApiRequest): Json {
    val idCompetitor = request.params["idCompetitor"].asInt
    val competitor = Competitor(idCompetitor)
    val idUkaList = competitor.idUka.toString().append(competitor.ukaAliases)
    val response = createResponse(request, "competitorUkaLedger")
    var balance = 0
    dbQuery(
        """
        SELECT
            uka.z_ledger.*,
            uka.reg_show.show_name,
            dog.petName
        FROM
            uka.z_ledger
                left JOIN
            uka.reg_show USING (show_id)
                left JOIN
            uka.reg_dog USING (dog_id)
                left JOIN
            dog ON dog.idUka = uka.reg_dog.dog_id
        WHERE
            uka.z_ledger.member_id IN ($idUkaList) AND NOT deleted
        ORDER BY date , show_id, type<>8, type
        """
    ) {
        val date = getDate("date")
        val type = getInt("type")
        val amount = Math.round(getDouble("amount") * 100.0).toInt()
        val petName = getString("petName")
        val showName = getString("show_name")
        val description = getString("description")
        val memberId = getInt("member_id")
        val text = when {
            type == 1 -> "Membership"
            type == 2 -> "Registration - $petName"
            type == 8 -> "$showName - $petName"
            showName.isNotEmpty() -> "$showName - $description"
            else -> description
        }
        balance += amount
        val node = response["data.items"].addElement()
        node["date"] = date
        node["memberId"] = memberId
        node["description"] = text
        node["amount"] = amount
        node["balance"] = balance
    }
    return response
}

internal fun getCompetitorRegister(request: ApiRequest): Json {
    val response = createResponse(request, "competitorRegister")
    response["data.register.ukaUserName"] = ""
    response["data.register.ukaPassword"] = ""
    response["data.register.email"] = ""
    response["data.register.password"] = ""
    return response
}

internal fun getAccountEntries(request: ApiRequest): Json {
    val idAccount = request.params["idAccount"].asInt
    val query =
        DbQuery("SELECT GROUP_CONCAT(idCompetition) AS list FROM ledger WHERE idAccount=$idAccount AND type IN ($LEDGER_ENTRY_FEES, $LEDGER_ENTRY_FEES_PAPER, $LEDGER_CAMPING_DEPOSIT)").toFirst()
    var idCompetitionList = query.getString("list")
    var idPaperList = ArrayList<Int>()
    val response = createResponse(request)

    DbQuery("SELECT GROUP_CONCAT(idCompetition) AS list FROM ledger WHERE idAccount=$idAccount AND type IN ($LEDGER_ENTRY_FEES_PAPER)").forEach { it ->
        idPaperList = it.getString("list").listToIntArray()
    }

    if (idCompetitionList.isNotEmpty()) {
        val c = Competition.select(
            "idCompetition IN ($idCompetitionList) AND competition.dateEnd>=curdate() ${accessCondition(request)}",
            "competition.dateStart"
        )
        while (c.next()) {
            val node = response["data.competitions"].addElement()
            loadFromTable(node, c, request)
            if (idPaperList.contains(c.id)) {
                node["paper"] = true
            }
        }

    }
    return response
}

internal fun getAccountEmails(request: ApiRequest): Json {
    val idAccount = request.params["idAccount"].asInt
    val response = createResponse(request)
    EmailQueue().where("idAccount=$idAccount", "dateCreated DESC") {
        val node = response["data.emails"].addElement()
        loadFromTable(node, this, request)
    }
    return response
}

internal fun getAccountEmail(request: ApiRequest): Json {
    val idAccount = request.params["idAccount"].asInt
    val idEmailQueue = request.params["idEmailQueue"].asInt
    val response = createResponse(request)
    EmailQueue().where("idAccount=$idAccount AND idEmailQueue=$idEmailQueue") {
        val node = response["data.email"]
        loadFromTable(node, this, request)
        node["message"] = message
    }
    return response
}


internal fun getAccountFunction(request: ApiRequest): Json {
    val idAccount = request.params["idAccount"].asInt
    val function = request.params["function"].asString
    val response = createResponse(request)

    when (function) {
        "merge_dogs" -> {
            PlazaData.deDuplicateAccountDogs(idAccount)
        }
        "merge_names" -> {
            Competitor().where("idAccount=$idAccount AND aliasFor=0") {
                PlazaData.deDuplicateName(givenName, familyName)
            }
        }
    }
    return response
}


fun getCompetitorInformation(request: ApiRequest): Json {
    val code = request.params["code"].asString
    val response = createResponse(request)

    Competitor().join { account }.where("competitorCode=${code.quoted}") {
        response["data.idCompetitor"] = id
        response["data.givenName"] = givenName
        response["data.familyName"] = familyName
        response["data.idAccountHandler"] = idAccount
        response["data.accountCode"] = account.code
        response["data.accountName"] = if (account.idCompetitor != id) account.fullName else fullName
        response["data.fullName"] = fullName
        response["data.email"] = email
        response["data.phone"] = phoneMobile.append(phoneOther, "/")

    }.otherwise {
        response["data.idCompetitor"] = 0
        response["data.givenName"] = ""
        response["data.familyName"] = ""
        response["data.idAccountHandler"] = 0
        response["data.accountCode"] = ""
        response["data.accountName"] = ""
    }
    return response
}

fun getDogInformation(request: ApiRequest): Json {
    val code = request.params["code"].asInt

    val dog = Dog()
    val response = createResponse(request)

    dog.select("dogCode=$code")
    if (dog.first()) {
        response["data.idDog"] = dog.id
        response["data.petName"] = dog.cleanedPetName
        response["data.handler"] = dog.handler.fullName
    } else {
        response["data.idDog"] = 0
        response["data.petName"] = ""
        response["data.owner"] = ""
    }

    return response
}

fun getDogKcGradeChange(request: ApiRequest): Json {
    val idDog = request.params["idDog"].asInt
    val response = createResponse(request)
    val dog = Dog(idDog)
    val dogNode = response["data.dog"]

    loadFromTable(dogNode, dog, request)
    val webTransaction = WebTransaction()
    webTransaction.seekDogKcGradeChange(idDog)
    response["data.log"].setValue(webTransaction.log)
    return response
}

fun getDogKcGradeReview(request: ApiRequest): Json {
    val idDog = request.params["idDog"].asInt
    val response = createResponse(request, "kc_grade_review", "idDog=$idDog")
    val dog = Dog(idDog)
    val dogNode = response["data.dog"]
    val kcGradeCodeNew = dog.kcGradeCodeHold
    val kcQualificationDate = dog.kcQualificationDate(hold = true)
    var wonOut = false

    loadFromTable(dogNode, dog, request)
    dogNode["kcGradeNew"] = Grade.getGradeName(kcGradeCodeNew)
    if (kcGradeCodeNew != "KC01") {
        if (kcQualificationDate > Dog.kcGradeUnknownDate) {
            dogNode["wonOut"] = kcQualificationDate.fullDate()
            wonOut = true
        } else {
            dogNode["wonOut"] = "no"
        }
    }

    CompetitionDog().join { competition }.where(
        "idDog=$idDog AND competition.dateStart>=curdate() AND competition.idOrganization = $ORGANIZATION_KC",
        "competition.dateStart"
    ) {
        val effectiveGradeCode = dog.kcEffectiveGradeCode(competition.dateStart, hold = true)
        val action =
            if (!wonOut && competition.hasClosed) {
                "Show already closed - no changes allowed"
            } else if (kcGradeCodeNew != effectiveGradeCode) {
                "New grade does not apply"
            } else if (competition.dateStart.addDays(-14) < today) {
                "Too late to make changes (14 day limit applies)"
            } else if (kcGradeCode != kcGradeCodeNew) {
                "Classes will be updated"
            } else {
                "No action required"
            }
        val node = response["data.competitions"].addElement()
        node["name"] = competition.briefName
        node["grade"] = Grade.getGradeName(kcGradeCode)
        node["action"] = action
    }
    return response
}

fun putDogKcGradeReview(body: Json): Json {
    val response = createResourceResponse()
    val dogNode = body["dog"]
    val idDog = dogNode["idDog"].asInt

    val dog = Dog(idDog)

    dbTransaction {
        val log = dog.updateKcGrade()
        val webTransaction = WebTransaction()
        webTransaction.seekDogKcGradeChange(idDog, force = true)
        webTransaction.log.setValue(log)
        webTransaction.post()
    }
    return response
}

internal fun getVoucher(request: ApiRequest): Json {
    val idCompetition = request.params["idCompetition"].asInt
    val voucherCode = request.params["voucherCode"].asString
    val competition = Competition(idCompetition)

    val voucher = Voucher()
    voucher.select("idCompetition=$idCompetition AND voucherCode=${voucherCode.quoted}")
    voucher.first()
    val response = createResponse(request, "voucher", "idCompetition=$idCompetition", "voucherCode=$voucherCode")
    response["data.includeCamping"] = competition.hasCamping
    loadFromTable(response["data.voucher"], voucher, request)
    if (!voucher.found()) {
        response["data.voucher.idCompetition"] = idCompetition
        response["data.voucher.type"] = VOUCHER_HELPER
    }

    return response
}

internal fun putVoucher(body: Json): Json {
    val response = createResourceResponse()
    val voucherNode = body["voucher"]
    val bindings = getBindings(body, "voucher")

    val idVoucher = body["voucher.idVoucher"].asInt
    val delete = body["voucher.delete"].asBoolean
    val type = body["voucher.type"].asInt
    val ringPartyName = body["voucher.ringPartyName"].asString

    if (delete && idVoucher > 0) {
        dbExecute("DELETE FROM voucher WHERE idVoucher=$idVoucher")
    } else {
        validate(
            type != VOUCHER_RING_PARTY || ringPartyName.isNotEmpty(),
            "voucher.ringPartyName",
            response,
            "Ring party needs a name"
        )

        Voucher().seekOrAppend("idVoucher=$idVoucher") {
            if (isAppending) {
                generateCode()
            }
            idCompetition = body["voucher.idCompetition"].asInt
            this.type = body["voucher.type"].asInt
            allCampingFree = body["voucher.allCampingFree"].asBoolean
            campingCredit = if (allCampingFree) 0 else body["voucher.campingCredit"].asString.poundsToPence()
            campingNightsFree = body["voucher.campingNightsFree"].asInt
            campingPriority = if (allCampingFree) false else body["voucher.campingPriority"].asBoolean

            allRunsFree = body["voucher.allRunsFree"].asBoolean
            freeRuns = if (allRunsFree) 0 else body["voucher.freeRuns"].asInt
            generalCredit = if (allRunsFree) 0 else body["voucher.generalCredit"].asString.poundsToPence()

            memberRates = if (type.oneOf(
                    VOUCHER_MEMBER,
                    VOUCHER_COMMITTEE
                ) && !allRunsFree
            ) body["voucher.memberRates"].asBoolean else false

            this.ringPartyName = body["voucher.ringPartyName"].asString
            this.description = specification
            post()
        }

        if (response.has("dataErrors")) {
            return error(response, 1, "The information you supplied has errors")
        }
    }

    return response
}

internal fun getDog(request: ApiRequest): Json {
    val idDog = request.params["idDog"].asInt
    val share = request.query["share"].asBoolean


    val dog = Dog()
    val response = createResponse(request, if (share) "dog_share" else "dog", "idDog=$idDog")
    val idAccount = response["data.user.idAccount"].asInt


    if (dog.find(idDog)) {

        if (share) {
            response["data.lookup"] = "${QuartzApiServer.uri}/competitor/codeInformation/"
            response["data.handlerCode"] = ""
            response["data.givenName"] = ""
            response["data.familyName"] = ""
            if (dog.shareCode.isNotEmpty()) {
                Competitor().where("competitorCode=${dog.shareCode.quoted}") {
                    response["data.handlerCode"] = code
                    response["data.givenName"] = givenName
                    response["data.familyName"] = familyName
                }
            }
        }

        addOwnershipChoices(response["data.ownershipTypeList"])
        addCompetitorList(dog.idAccount, response["data.competitorList"])
        val handlersList = dog.account.handlersList
        addHandlerList(dog.idAccount, handlersList, response["data.handlerList"])
        loadFromTable(response["data.dog"], dog, request)
        response["data.dog.kcChampWins"] = dog.kcChampWins
        response["data.dog.hasKcChampWins"] = dog.hasKcChampWins
        response["data.dog.ukaBarred"] = dog.ukaBarred
        response["data.dog.ukaBarredReason"] = dog.ukaBarredReason
        response["data.dog.ownerType"] = dog.ownerType
        response["data.dog.ownerName"] = dog.ownerName
        response["data.dog.ownerAddress"] = dog.ownerAddress.asMultiLine
        response["data.dog.owner"] = dog.ownerText
        if (dog.isUkaRegistered) {
            response["data.dog.ukaTitle"] = dog.ukaTitle
        }
        response["data.dog.handler"] =
            if (dog.idCompetitorHandler != dog.idCompetitor) dog.handler.fullName else dog.owner.fullName
        response["data.dog.stateText"] = when (dog.state) {
            DOG_ACTIVE -> "Active"
            DOG_RETIRED -> "Retired"
            DOG_GONE -> "Not Forgotten"
            else -> ""
        }

        response["data.dog.breedText"] = Breed.getBreedName(dog.idBreed)
        response["data.dog.genderText"] = if (dog.gender == 1) "Dog" else if (dog.gender == 2) "Bitch" else "n/a"
        if (dog.kcGrade2.isNotEmpty() && dog.kcGrade2 != Dog.kcGradeUnknownDate) response["data.dog.kcGrade2"] =
            dog.kcGrade2
        if (dog.kcGrade3.isNotEmpty() && dog.kcGrade3 != Dog.kcGradeUnknownDate) response["data.dog.kcGrade3"] =
            dog.kcGrade3
        if (dog.kcGrade4.isNotEmpty() && dog.kcGrade4 != Dog.kcGradeUnknownDate) response["data.dog.kcGrade4"] =
            dog.kcGrade4
        if (dog.kcGrade5.isNotEmpty() && dog.kcGrade5 != Dog.kcGradeUnknownDate) response["data.dog.kcGrade5"] =
            dog.kcGrade5
        if (dog.kcGrade6.isNotEmpty() && dog.kcGrade6 != Dog.kcGradeUnknownDate) response["data.dog.kcGrade6"] =
            dog.kcGrade6
        if (dog.kcGrade7.isNotEmpty() && dog.kcGrade7 != Dog.kcGradeUnknownDate) response["data.dog.kcGrade7"] =
            dog.kcGrade7
        response["data.dog.kcGradeCodeWas"] = dog.kcGradeCode
        if (request.query["session"].asBoolean) {
            response["control.session.idDog"] = if (idDog <= 0) 0 else dog.id
            response["control.session.petName"] = if (idDog <= 0) "" else dog.petName
        }

        if (dog.idAccountShared > 0 && dog.idAccountShared != dog.idAccount && dog.idAccountShared != idAccount) {
            response["data.dog.sharedWith"] = Account.describe(dog.idAccountShared)
        }


    } else {
        return error(response, 1, "Dog not found")
    }
    return response
}

internal fun getCompetitorSession(request: ApiRequest): Json {
    val altIdCompetitor = request.params["altIdCompetitor"].asInt
    val response = createResponse(request)
    response["control.session.altIdCompetitor"] = altIdCompetitor
    return response
}

internal fun getDogSession(request: ApiRequest): Json {
    val idDog = request.params["idDog"].asInt
    val dog = Dog()
    dog.find(idDog)
    val response = createResponse(request)
    response["control.session.idDog"] = if (idDog <= 0) 0 else dog.id
    response["control.session.petName"] = if (idDog <= 0) "" else dog.petName
    return response
}

internal fun getDogResults(request: ApiRequest): Json {
    val dog = Dog()
    val entry = Entry()
    val idDog = request.params["idDog"].asInt
    dog.find(idDog)

    entry.join(entry.agilityClass, entry.agilityClass.competition, entry.team)
    entry.select(
        "team.idDog = ${idDog} AND entry.progress IN ($PROGRESS_RUN, $PROGRESS_VOID) ${accessCondition(request)}",
        "competition.dateStart DESC, agilityClass.classDate, agilityClass.classCode, agilityClass.Suffix"
    )
    val response = createResponse(request)
    loadFromTable(response["data.dog"], dog, request)
    var idCompetition = -1
    var date = nullDate
    var competitionNode = Json.nullNode()
    var dateNode = Json.nullNode()
    while (entry.next()) {
        if (entry.agilityClass.competition.id != idCompetition) {
            idCompetition = entry.agilityClass.competition.id
            competitionNode = response["data.dog.competitions"].addElement()
            loadFromTable(competitionNode, entry.agilityClass.competition, request)
            date = nullDate
            dateNode = Json.nullNode()
        }
        if (entry.agilityClass.date != date) {
            date = entry.agilityClass.date
            dateNode = competitionNode["dates"].addElement()
            dateNode["date"] = entry.agilityClass.date
        }
        val node = dateNode["entries"].addElement()
        loadFromTable(node, entry, request, "+scoreText,+timeText,+progressionPoints,+place")
        node["isPlaced"] = isBitSet(entry.placeFlags, PRIZE_ROSETTE)
        node["prizeText"] = entry.prizeText
        if (entry.progress == PROGRESS_VOID && entry.progressionPoints > 0) {
            node["progressionPoints"] = "VOID"
        }

        node["className"] = entry.agilityClass.name
        node["abbreviatedName"] = entry.agilityClass.abbreviatedName
    }
    return response
}

internal fun getDogUkaProgress(request: ApiRequest): Json {
    var idDog = request.params["idDog"].asInt
    val response = createResponse(request)
    val dog = Dog()
    dog.find(idDog)
    dog.ukaCalculateGrades()
    loadFromTable(response["data.dog"], dog, request)

    val performance = response["data.performance"]
    val steeplechase = response["data.steeplechase"]
    performance["statement"] = dog.ukaPerformanceStatement
    steeplechase["statement"] = dog.ukaSteeplechaseStatement

    var level = performance["levels"].addElement()
    level["name"] = "Beginners"
    level["active"] = dog.ukaPerformanceLevel == "UKA01"
    level["agility"] = dog.ukaBeginnersAgility
    level["jumping"] = dog.ukaBeginnersJumping
    level["games"] = dog.ukaBeginnersGames
    level["total"] = dog.ukaBeginnersAgility + dog.ukaBeginnersJumping + dog.ukaBeginnersGames

    level = performance["levels"].addElement()
    level["name"] = "Novice"
    level["active"] = dog.ukaPerformanceLevel == "UKA02"
    level["agility"] = dog.ukaNoviceAgility
    level["jumping"] = dog.ukaNoviceJumping
    level["games"] = dog.ukaNoviceGames
    level["total"] = dog.ukaNoviceAgility + dog.ukaNoviceJumping + dog.ukaNoviceGames

    level = performance["levels"].addElement()
    level["name"] = "Senior"
    level["active"] = dog.ukaPerformanceLevel == "UKA03"
    level["agility"] = dog.ukaSeniorAgility
    level["jumping"] = dog.ukaSeniorJumping
    level["games"] = dog.ukaSeniorGames
    level["total"] = dog.ukaSeniorAgility + dog.ukaSeniorJumping + dog.ukaSeniorGames

    level = performance["levels"].addElement()
    level["name"] = "Champ"
    level["active"] = dog.ukaPerformanceLevel == "UKA04"
    level["agility"] = dog.ukaChampAgility
    level["jumping"] = dog.ukaChampJumping
    level["games"] = dog.ukaChampGames
    level["total"] = dog.ukaChampAgility + dog.ukaChampJumping + dog.ukaChampGames

    level = performance["levels"].addElement()
    level["name"] = "Champ Wins"
    level["agility"] = dog.ukaChampWinAgility
    level["jumping"] = dog.ukaChampWinJumping
    level["games"] = dog.ukaChampWinGames
    level["total"] = dog.ukaChampWinAgility + dog.ukaChampWinJumping + dog.ukaChampWinGames

    level = steeplechase["levels"].addElement()
    level["active"] = dog.ukaSteeplechaseLevel == "UKA01"
    level["name"] = "Beginners"
    level["points"] = dog.ukaBeginnersSteeplechase

    level = steeplechase["levels"].addElement()
    level["name"] = "Novice"
    level["active"] = dog.ukaSteeplechaseLevel == "UKA02"
    level["points"] = dog.ukaNoviceSteeplechase

    level = steeplechase["levels"].addElement()
    level["name"] = "Senior"
    level["active"] = dog.ukaSteeplechaseLevel == "UKA03"
    level["points"] = dog.ukaSeniorSteeplechase

    level = steeplechase["levels"].addElement()
    level["name"] = "Champ"
    level["active"] = dog.ukaSteeplechaseLevel == "UKA04"
    level["points"] = dog.ukaChampSteeplechase

    level = steeplechase["levels"].addElement()
    level["name"] = "Champ Wins"
    level["points"] = dog.ukaChampWinSteeplechase

    return response
}

internal fun getCompetitorAuthenticate(request: ApiRequest): Json {
    val response = createResponse(request, "competitorAuthenticate")
    response["data.authenticate.email"] = ""
    response["data.authenticate.password"] = ""
    return response
}

internal fun putCompetitorAuthenticate(body: Json): Json {
    val response = createResourceResponse()
    val email = body["authenticate.email"].asString
    val password = body["authenticate.password"].asString
    val token = decryptJson(body["authenticate.token"].asString)
    var idCompetitor = email.substringBefore("@").toIntDef(-1)
    val idCompetitorReal = token["idCompetitorReal"].asInt
    var access = ACCESS_USER
    var error = 0

    if (idCompetitorReal > 0) {
        Competitor().seek(idCompetitorReal) {
            access = when {
                systemAdministrator -> ACCESS_SYSTEM
                plazaSuperUser -> ACCESS_KC
                ukaSuperUser -> ACCESS_UKA
                else -> ACCESS_USER
            }
        }
    }
    val spoofing = idCompetitor > -1 && access != ACCESS_USER

    val where = if (spoofing) {
        "idCompetitor = $idCompetitor"
    } else if (email.isNotEmpty() && password.isNotEmpty()) {
        "email=${email.quoted} AND password=${password.quoted}"
    } else {
        "false"
    }

    var isRegistered = false
    var verified = nullDate
    Competitor().where(where, "registrationComplete DESC, aliasFor, ukaMembershipExpires DESC", limit = 1) {
        isRegistered = registrationComplete
        verified = emailVerifiedDate
        idCompetitor = resolveAlias()
    }

    Competitor().seek(idCompetitor) {
        val token1 = Json()
        token1["kind"] = "token.idCompetitor"
        token1["idCompetitor"] = idCompetitor
        token1["email"] = email
        if (spoofing) {
            response["control.action"] = "switched"
            token1["idCompetitorReal"] = if (idCompetitorReal > 0) idCompetitorReal else idCompetitor
            token1["access"] = access
            debug("authentication", "spoofing $email")
        } else {
            if (registrationComplete || isRegistered) {
                response["control.action"] = "authenticated"
                makeAccount()
                if (!registrationComplete) {
                    this.email = email
                    this.password = password
                    emailVerifiedDate = verified
                    registrationComplete = true
                }
                lastLogonEmail = email
                lastLogonPassword = password
                lastLogon = now
                post()
                debug("authentication", "success $email")
            } else {
                val token = generateRegistrationToken()
                val quickLink = "http://agilityplaza.com/register_uka?token=$token"
                //val quickLink = "http://localhost:3000/register_uka?token=$token"
                generateQuickRegistrationEmail(email, quickLink)
                response["control.action"] = "not_registered"
            }
        }
        response["control.data.token"] = token1.toJson().encrypt(keyPhrase)
    }.otherwise {
        error = if (email.isNotEmpty() && password.isNotEmpty()) -2 else -1
    }

    return when (error) {
        -1 -> {
            error(response, -1, "Bad Request")
        }
        -2 -> {
            debug("authentication", "authentication failed: $email ($password)")
            error(response, -2, "No Match")
        }
        else -> response
    }
}

internal fun putAuthenticate(request: ApiRequest): Json {
    return putCompetitorAuthenticate(request.body)
}


internal fun decryptJson(encryptedJson: String): Json {
    return if (encryptedJson.isEmpty()) Json() else Json(encryptedJson.decrypt(keyPhrase))
}

internal fun getCompetitorSessionFromToken(request: ApiRequest): Json {
    val registrationComplete = request.query["registrationComplete"].asBoolean
    val authenticated = request.query["authenticated"].asBoolean
    val token = decryptJson(request.params["token"].asString)
    val tokenKind = token["kind"].asString
    val idCompetitor = token["idCompetitor"].asInt
    val idCompetitorReal = token["idCompetitorReal"].asInt
    val access = token["access"].asInt
    val email = token["email"].asString
    val password = token["password"].asString
    val _countryCode = token["countryCode"].asString

    val competitor = Competitor()
    if (idCompetitor > 0) {
        competitor.find(idCompetitor)
    } else if (email.isNotEmpty() && password.isNotEmpty()) {
        competitor.select(
            "email=${email.quoted} AND password=${password.quoted} AND dateDeleted>0",
            "ukaMembershipExpires"
        )
        if (!competitor.first()) {
            competitor.append()
        }
    } else {
        throw Wobbly("invalid token for getCompetitorSessionFromToken")
    }

    val response = createResponse(request)
    when (tokenKind) {
        "token.email_confirmation" -> {
            competitor.email = email
            competitor.emailVerifiedDate = now
            if (password.isNotEmpty()) {
                competitor.password = password
            }
            if (registrationComplete) {
                competitor.registrationComplete = true
            }
            competitor.post()
            competitor.makeAccount()
            if (_countryCode.isNotEmpty()) {
                Account().seek(competitor.idAccount) {
                    countryCode = _countryCode
                    post()
                }
            }

        }
        "token.registration" -> {
            competitor.emailVerifiedDate = now
            competitor.post()
        }
        "token.email_changed" -> {
            if (competitor.unVerifiedEmail.isNotEmpty()) {
                competitor.email = competitor.unVerifiedEmail
                competitor.unVerifiedEmail = ""
                competitor.emailVerifiedDate = now
                competitor.post()
            }
            val node = response["data.competitor"]
            loadFromTable(node, competitor, request)

            return response
        }
    }

    resetCompetitorSession(
        response,
        competitor,
        authenticated,
        access,
        if (idCompetitorReal > 0) idCompetitorReal else competitor.id
    )
    return response
}

internal fun getAccountAll(request: ApiRequest): Json {
    val idAccount = request.params["idAccount"].asInt
    val response = createResponse(request, "accountCompetitors", "idAccount=$idAccount")
    val account = Account()
    var lastLogon = nullDate
    var registered = false
    var hasUka = false

    val plazaSuperUser = response["data.user.plazaSuperUser"].asBoolean
    val ukaSuperUser = response["data.user.ukaSuperUser"].asBoolean
    val superUser = plazaSuperUser || ukaSuperUser

    account.competitor.joinToParent()
    account.find(idAccount)
    val handlersList = account.handlersList
    val competitor = Competitor.select(
        if (handlersList.isEmpty()) "idAccount=$idAccount" else "idAccount=$idAccount OR idCompetitor IN ($handlersList)",
        "IF(idCompetitor=${account.idCompetitor}, 0, 1)"
    )

    val accountNode = response["data.account"]
    loadFromTable(response["data.account"], account, request)
    response["data.account.holder"] = account.competitor.fullName
    while (competitor.next()) {
        if (ukaSuperUser || !competitor.closed) {
            if (competitor.registrationComplete) registered = true
            if (competitor.idUka > 0) hasUka = true
            if (competitor.lastLogon > lastLogon) lastLogon = competitor.lastLogon
            val node = if (competitor.idAccount == idAccount)
                response["data.competitors"].addElement()
            else
                response["data.handlers"].addElement()
            loadFromTable(node, competitor, request)
            node["code"] = competitor.code
            node["closed"] = competitor.closed
            node["ukaState"] = competitor.ukaState
        }
    }
    accountNode["lastLogon"] =
        if (lastLogon.isNotEmpty()) lastLogon.dateText else if (!registered) "Not Registered" else ""

    val dog = Dog.select("idAccount=$idAccount OR idAccountShared=$idAccount", "dogState, petName")
    while (dog.next()) {
        if (ukaSuperUser || dog.state < DOG_REMOVE && dog.aliasFor == 0) {
            if (dog.idUka > 0) hasUka = true
            val node = response["data.dogs"].addElement()
            loadFromTable(node, dog, request)
            node["retired"] = dog.state == DOG_RETIRED
            node["gone"] = dog.state == DOG_GONE
            node["removed"] = dog.state >= DOG_REMOVE
            node["shared"] = dog.idAccount != idAccount
            node["sharedWith"] = dog.idAccountShared > 0 && dog.idAccountShared != idAccount
        }
    }
    accountNode["hasUka"] = hasUka
    return response
}

internal fun getAccountUkaCheckout(request: ApiRequest): Json {
    val idAccount = request.params["idAccount"].asInt
    val response = createResponse(request, "accountUkaCheckout", "idAccount=$idAccount")
    val webTransaction = WebTransaction()
    webTransaction.seekUkaRegistration(idAccount)
    if (webTransaction.found()) {
        val paper = webTransaction.data["paper"].asBoolean
        var totalFee = 0
        response["data.paper"] = paper
        for (group in webTransaction.data["actions"]) {
            for (item in group) {
                val fee = if (paper) item["paperFee"].asInt else item["fee"].asInt
                val confirmed = item["confirmed"].asBoolean
                if (confirmed) {
                    val node = response["data.actions"][group.name].addElement()
                    node.setValue(item)
                    totalFee += fee
                }
            }
        }
        response["data.totalFee"] = totalFee
        response["data.balance"] = Ledger.balance(idAccount)
    }
    return response
}

internal fun putAccountUkaCheckout(body: Json, idAccount: Int): Json {
    val response = createResourceResponse()
    val webTransaction = WebTransaction()
    webTransaction.seekUkaRegistration(idAccount)
    if (webTransaction.found()) {
        val feesStillOwing = webTransaction.confirmUka()
        if (!feesStillOwing) response["control.action"] = "paid"
    } else {
        response["control.action"] = "paid"
    }
    return response
}

fun putAccountUka(body: Json, idAccount: Int): Json {
    val response = createResourceResponse()
    val actions = body["actions"]
    var count = 0
    actions["competitors"].forEach { if (it["confirmed"].asBoolean) count++ }
    actions["dogs"].forEach { if (it["confirmed"].asBoolean) count++ }
    if (count == 0) {
        return error(response, 1, "You must tick at least one box")
    }
    dbTransaction {
        val webTransaction = WebTransaction()
        webTransaction.seekUkaRegistration(idAccount)
        if (webTransaction.found()) {
            webTransaction.data["actions"].setValue(body["actions"])
            webTransaction.data["paper"] = body["paper"].asBoolean
            webTransaction.post()
        } else {
            response["control.action"] = "notFound"
        }
    }
    return response
}


internal fun getAccountUka(request: ApiRequest): Json {
    val idAccount = request.params["idAccount"].asInt
    val response = createResponse(request, "accountUka", "idAccount=$idAccount")

    val paper = response["data.user.access"].asInt > 0 && response["data.user.ukaSuperUser"].asBoolean
    val webTransaction = WebTransaction.loadUka(idAccount, paper = paper)
    response["data.paper"] = webTransaction.data["paper"].asBoolean
    response["data.competitors"].setValue(webTransaction.competitors)
    response["data.dogs"].setValue(webTransaction.dogs)
    response["data.actions"].setValue(webTransaction.actions)
    return response
}


internal fun getAccountDogs(request: ApiRequest): Json {
    val idAccount = request.params["idAccount"].asInt
    val response = createResponse(request, "accountDogs", "idAccount=$idAccount")

    val dog = Dog.select("idAccount=$idAccount AND dogState<$DOG_REMOVE", "dogState, petName")
    while (dog.next()) {
        val node = response["data.dogs"].addElement()
        loadFromTable(node, dog, request)
        node["retired"] = dog.state == DOG_RETIRED
        node["gone"] = dog.state == DOG_GONE
    }
    return response
}

internal fun getAccountCompetitors(request: ApiRequest): Json {
    val idAccount = request.params["idAccount"].asInt
    val response = createResponse(request, "accountCompetitors", "idAccount=$idAccount")
    val account = Account()
    account.competitor.joinToParent()
    account.find(idAccount)
    val handlersList = account.handlersList
    val competitor = Competitor.select(
        if (handlersList.isEmpty()) "idAccount=$idAccount" else "idAccount=$idAccount OR idCompetitor IN ($handlersList)",
        "IF(idCompetitor=${account.idCompetitor}, 0, 1)"
    )

    loadFromTable(response["data.account"], account, request)
    response["data.account.holder"] = account.competitor.fullName
    while (competitor.next()) {
        val node = if (competitor.idAccount == idAccount)
            response["data.competitors"].addElement()
        else
            response["data.handlers"].addElement()
        loadFromTable(node, competitor, request)
        node["code"] = competitor.code
    }
    return response
}


internal fun getCompetitorDogs(request: ApiRequest): Json {
    val idCompetitor = request.params["idCompetitor"].asInt
    val registering = request.query["registering"].asBoolean
    val response = createResponse(request, "competitorDogs", "idCompetitor=$idCompetitor")

    val dog = Dog.select("idCompetitor=$idCompetitor AND dogState<$DOG_REMOVE", "dogState, petName")
    while (dog.next()) {
        val node = response["data.dogs"].addElement()
        loadFromTable(node, dog, request)
        if (registering) {
            node["kcRegistered"] = true
            if (dog.idKC == "undefined") {
                node["idKC"] = ""
            }
        }
    }
    return response
}

internal fun putCompetitorDogs(body: Json, idCompetitor: Int): Json {
    var response = createResourceResponse()
    val dogTable = Dog()

    val dogs = body["dogs"]
    for (dog in dogs) {
        val index = dogs.indexOf(dog)
        if (dog.has("retired")) {
            dog["dogState"] = if (dog["retired"].asBoolean) DOG_RETIRED else DOG_ACTIVE
        }
        dogTable.find(dog["idDog"].asInt)
        if (dogTable.idCompetitor != idCompetitor) {
            return error(response, 99, "Security Breach")
        }
        saveToTable(dog, dogTable, getBindings(body, "dogs$index"))
    }
    return response
}

internal fun getAccountRefund(request: ApiRequest): Json {
    val idAccount = request.params["idAccount"].asInt
    val idCompetitor = request.query["idCompetitor"].asInt
    val account = Account(idAccount)

    val response = createResponse(request, "accountRefund", "idAccount=$idAccount")
    loadFromTable(response["data.account"], account, request)

    response["data.account.bankAccountName"] = account.bankAccountName
    response["data.account.bankSortCode"] = account.bankSortCode
    response["data.account.bankAccountNumber"] = account.bankAccountNumber
    response["data.account.maxRefund"] = Account.maxRefund(idAccount)
    response["data.account.maxRefundText"] = Account.maxRefund(idAccount).money

    return response
}

internal fun putAccountRefund(body: JsonNode, idAccount: Int): Json {
    val plazaSuperUser = body["user.plazaSuperUser"].asBoolean
    val ukaSuperUser = body["user.ukaSuperUser"].asBoolean
    val systemAdministrator = body["user.systemAdministrator"].asBoolean

    val response = createResourceResponse()
    val account = Account(idAccount)
    if (account.found()) {
        val bankAccountName = body["account.bankAccountName"].asString
        val bankSortCode = body["account.bankSortCode"].asString.replace("-", "").replace(" ", "").trim()
        val bankAccountNumber = body["account.bankAccountNumber"].asString.replace(" ", "").trim()
        val maxRefund = body["account.maxRefund"].asInt
        val amount = body["refund.amount"].asString.poundsToPence()

        if (!bankSortCode.matches(Regex("[0-9]{6}"))) {
            addDataError(response, "account.bankSortCode", "Invalid sort code")
        }
        if (!bankAccountNumber.matches(Regex("[0-9]{8}"))) {
            addDataError(response, "account.bankAccountNumber", "Invalid account number")
        }
        if (amount <= 0) {
            addDataError(response, "refund.amount", "Please enter an amount")
        }
        if (amount > maxRefund) {
            addDataError(response, "refund.amount", "Must be no more than ${maxRefund.money}")
        }
        if (Account.pendingRefund(idAccount) > 0) {
            return error(response, 1, "You already have a pending refund")
        }
        if (response.has("dataErrors")) {
            return error(response, 1, "The information you supplied is incorrect")
        }

        debug("putAccountRefund", "amount = $amount")
        dbTransaction {
            account.bankAccountName = bankAccountName
            account.bankSortCode = bankSortCode
            account.bankAccountNumber = bankAccountNumber
            account.post()
            if (plazaSuperUser || systemAdministrator) {
                BankPaymentRequest.refundAccount(
                    idAccount,
                    bankAccountName,
                    bankSortCode,
                    bankAccountNumber,
                    amount,
                    confirmationRequired = false
                )
            } else {
                val idBankPaymentRequest = BankPaymentRequest.refundAccount(
                    idAccount,
                    bankAccountName,
                    bankSortCode,
                    bankAccountNumber,
                    amount,
                    confirmationRequired = true
                )
                val host = body["host"].asString
                val newToken = Json()
                newToken["kind"] = "token.refund_account"
                newToken["idAccount"] = idAccount
                newToken["idBankPaymentRequest"] = idBankPaymentRequest
                val encryptedToken = newToken.toJson().encrypt(keyPhrase)
                val href = "${host}/refund_confirmed?token=${encryptedToken}"
                PlazaMessage.refundRequest(account, bankAccountName, bankSortCode, bankAccountNumber, amount, href)
                dequeuePlaza()
                response["control.action"] = "confirm"
            }
        }
    }
    return response
}

internal fun getAccountRefundConfirmed(request: ApiRequest): Json {
    val token = decryptJson(request.params["token"].asString)
    val idAccount = token["idAccount"].asInt
    val idBankPaymentRequest = token["idBankPaymentRequest"].asInt

    val response = createResponse(request, "accountRefundConfirmed", "idAccount=$idAccount")

    val account = Account(idAccount)
    loadFromTable(response["data.account"], account, request)

    val bankPaymentRequest = BankPaymentRequest(idBankPaymentRequest)

    val problem = if (!bankPaymentRequest.found()) {
        "Refund request is no longer valid"
    } else {
        when (bankPaymentRequest.confirm()) {
            BankPaymentRequest.ConfirmState.OK -> ""
            BankPaymentRequest.ConfirmState.ALREADY_CONFIRMED -> "Refund request has already been confirmed"
            BankPaymentRequest.ConfirmState.EXPIRED -> "Refund request is no longer valid"
            BankPaymentRequest.ConfirmState.FUNDS_UNAVAILABLE -> "We cannot precede with the request because funds are no longer available"
        }
    }
    if (problem.isNotEmpty()) {
        error(response, 1, problem)
    }
    return response
}


internal fun getAccountLedger(request: ApiRequest): Json {
    val idAccount = request.params["idAccount"].asInt
    val idCompetitor = request.query["idCompetitor"].asInt
    val account = Account()
    account.country.joinToParent()
    account.find(idAccount)

    val ledger = Ledger()
    ledger.competition.joinToParent()
    ledger.select("idAccount=$idAccount AND (debit=$ACCOUNT_USER OR credit=$ACCOUNT_USER)", "dateEffective, idLedger")
    val competitor = Competitor(idCompetitor)

    val response = createResponse(request, "accountLedger", "idAccount=$idAccount")
    loadFromTable(response["data.account"], account, request)

    response["data.card.rate"] = account.cardRate
    response["data.card.fixed"] = account.cardFixed
    response["data.competitor.email"] = competitor.email
    response["data.key"] = PaymentCard.publicKey

    var statementBalance = 0
    ledger.beforeFirst()
    while (ledger.next()) {
        val amount = if (ledger.credit == ACCOUNT_USER) ledger.amount else -ledger.amount
        if (!ledger.isPurchase || amount != 0) {
            statementBalance += amount
            val item = response["data.statement"].addElement()
            item["date"] = ledger.dateEffective
            item["type"] = ledger.type
            item["description"] = ledger.description
            item["amount"] = amount
            item["partial"] = ledger.amountOwing > 0
            item["balance"] = statementBalance
            if (ledger.type.oneOf(
                    LEDGER_CAMPING_FEES,
                    LEDGER_CAMPING_PERMIT,
                    LEDGER_CAMPING_DEPOSIT,
                    LEDGER_ENTRY_FEES,
                    LEDGER_ENTRY_FEES_PAPER
                )
            ) {
                item["idCompetition"] = ledger.competition.id
                item["idOrganization"] = ledger.competition.idOrganization
            }
        }
    }

    var pendingBalance = statementBalance
    ledger.beforeFirst()
    while (ledger.next()) {
        val amount = if (ledger.credit == ACCOUNT_USER) ledger.amountOwing else -ledger.amountOwing
        if (ledger.isPurchase && amount != 0) {
            val item = response["data.pending"].addElement()
            pendingBalance += amount
            item["date"] = if (ledger.dueImmediately) ledger.dateEffective.addYears(-1) else ledger.dateEffective
            item["dueImmediately"] = ledger.dueImmediately
            item["type"] = ledger.type
            item["description"] = ledger.description
            item["amount"] = amount
            item["partial"] = ledger.amount != 0
            item["balance"] = pendingBalance
            item["capped"] = ledger.competition.cappingLevel > 0
            item["idCompetition"] = ledger.competition.id
            item["idOrganization"] = ledger.competition.idOrganization
        }
    }
    response["data.pending"].sortBy("date")

    val pendingRefund = Account.pendingRefund(idAccount)
    response["data.summary.statementBalance"] = statementBalance
    response["data.summary.pendingRefund"] = pendingRefund
    response["data.summary.pendingRefundText"] = pendingRefund.money
    if (pendingRefund == 0) {
        response["data.summary.maxRefund"] = Account.maxRefund(idAccount)
    }
    response["data.summary.pendingBalance"] = pendingBalance
    response["data.topup.amount"] = if (pendingBalance < 0) -pendingBalance else 0


    return response
}

internal fun putAccountLedger(body: JsonNode, idAccount: Int): Json {
    val response = createResourceResponse()
    val account = Account(idAccount)
    if (!account.found()) {
        response["control.action"] = "error"
        response["control.message"] = "ERROR: Unable to process credit card payment - no account data"
    } else {
        val token = body["stripe.token"]
        val amount = body["stripe.amount"].asInt
        val handlingFee = body["stripe.handlingFee"].asInt
        dbTransaction {
            val charge = PaymentCard.chargeStripe(idAccount, token, amount, "Top-up")
            if (charge["status"].asString.neq("succeeded")) {
                response["control.action"] = "error"
                response["control.message"] =
                    "ERROR: Unable to process credit card payment (${charge["message"].asString})"
                println(charge.toJson(pretty = true))
            } else {
                account.addStripeCard(charge)
                account.addStripePayment(charge, amount, handlingFee)
                account.post()
            }
        }
    }
    return response
}

internal fun getCompetitorPaymentCardAdd(request: ApiRequest): Json {
    val idCompetitor = request.params["idCompetitor"].asInt
    val response = createResponse(request, "competitorPaymentCard", "idCompetitor=$idCompetitor")
    response["data.function"] = "add"
    loadFromTable(response["data.competitor"], Competitor(idCompetitor), request)
    response["data.card.number"] = ""
    response["data.card.name"] = ""
    response["data.card.month"] = 0
    response["data.card.year"] = 0
    response["data.card.security"] = ""
    return response
}

internal fun putCompetitorPaymentCardAdd(idCompetitor: Int): Json {
    val response = createResourceResponse()
    val competitor = Competitor(idCompetitor)

    /*
    competitor.addPaymentCard(
            reference = body["card.number"].asString,
            last4 = body["card.number"].asString.takeLast(4),
            issuer = "Visa",
            credit = true,
            country = "GB",
            name = body["card.name"].asString,
            month = body["card.month"].asInt,
            year = body["card.year"].asInt
    )
    */
    competitor.post()

    return response
}

internal fun getCompetitorDogsAdd(request: ApiRequest): Json {
    var dog = Dog()
    val idCompetitor = request.params["idCompetitor"].asInt
    val response = createResponse(request, "addDog", "idCompetitor=$idCompetitor")
    dog.select("true", limit = 1)
    dog.first()
    response["data.dog"].loadFromDataset(
        dog,
        columnSelection("dog", request.query["select"].asString),
        prototype = true
    )
    return response
}

internal fun getCompetitorSplit(request: ApiRequest): Json {
    val idCompetitor = request.params["idCompetitor"].asInt
    Competitor().seek(idCompetitor) { splitFromAccount() }
    val response = createResponse(request)
    return response
}

internal fun getCompetitorAdd(request: ApiRequest): Json {
    var idCompetitorReal =
        firstNonZero(request.query["idCompetitorReal"].asInt, request.query["idCompetitor"].asInt)
    val response = createResponse(request, "addCompetitor", "idCompetitorReal=$idCompetitorReal")

    Competitor().where("true", limit = 1) {
        response["data.competitor"].loadFromDataset(this, prototype = true)
    }
    return response
}

internal fun putCompetitorAdd(body: Json, idCompetitorReal: Int): Json {
    val response = createResourceResponse()
    val competitor = Competitor()
    val bindings = getBindings(body, "competitor")
    update(response, competitor, body["competitor"], "idCompetitor", 0, bindings)
    competitor.makeAccount()

    val token = Json()
    token["kind"] = "token.idCompetitor"
    token["idCompetitor"] = competitor.id
    token["idAccount"] = competitor.idAccount
    token["idCompetitorReal"] = idCompetitorReal
    token["access"] = 3
    response["control.data.token"] = token.toJson().encrypt(keyPhrase)
    response["control.action"] = "switched"
    return response
}

fun addOwnershipChoices(node: JsonNode) {
    fun addChoice(value: Int, description: String) {
        val nodeItem = node.addElement()
        nodeItem["value"] = value
        nodeItem["description"] = description
    }
    addChoice(DOG_OWNER_SINGLE, "Single owner at this address")
    addChoice(DOG_OWNER_HOUSEHOLD, "Joint owners at this address")
    addChoice(DOG_OWNER_OTHER, "Owner(s) at different address")
}

fun addCompetitorList(idAccount: Int, node: JsonNode) {
    val competitor = Competitor.select("idAccount=$idAccount", "givenName, familyName")
    while (competitor.next()) {
        val nodeItem = node.addElement()
        nodeItem["value"] = competitor.id
        nodeItem["description"] = competitor.fullName
    }
}

fun addHandlerList(idAccount: Int, handlerList: String, node: JsonNode) {
    val competitor = Competitor.select(
        if (handlerList.isNotEmpty())
            "idAccount=$idAccount OR idCompetitor IN ($handlerList)"
        else
            "idAccount=$idAccount"
        , "givenName, familyName"
    )
    while (competitor.next()) {
        val nodeItem = node.addElement()
        nodeItem["value"] = competitor.id
        nodeItem["description"] = competitor.fullName
    }
}

internal fun getAccountDogsAdd(request: ApiRequest): Json {
    val idAccount = request.params["idAccount"].asInt
    val idCompetitor = request.query["idCompetitor"].asInt
    val dog = Dog()
    val response = createResponse(request, "addDog", "idAccount=$idAccount")
    addOwnershipChoices(response["data.ownershipTypeList"])
    addCompetitorList(idAccount, response["data.competitorList"])
    val account = Account(idAccount)
    account.first()
    val handlersList = account.handlersList
    addHandlerList(idAccount, handlersList, response["data.handlerList"])

    dog.select("true", limit = 1)
    dog.first()
    response["data.dog"].loadFromDataset(
        dog,
        columnSelection("dog", request.query["select"].asString),
        prototype = true
    )
    response["data.dog.idAccount"] = idAccount
    response["data.dog.idCompetitor"] = idCompetitor
    response["data.dog.idCompetitorHandler"] = idCompetitor
    response["data.dog.ownerType"] = dog.ownerType
    response["data.dog.ownerName"] = dog.ownerName
    response["data.dog.ownerAddress"] = dog.ownerAddress.asMultiLine
    return response
}

internal fun getAccountCompetitorsAdd(request: ApiRequest): Json {
    val idAccount = request.params["idAccount"].asInt
    val competitor = Competitor()
    val response = createResponse(request, "addAccountCompetitor", "idAccount=$idAccount")
    addCompetitorList(idAccount, response["data.competitorList"])
    competitor.select("true", limit = 1)
    competitor.first()
    response["data.competitor"].loadFromDataset(
        competitor,
        columnSelection("competitor", request.query["select"].asString),
        prototype = true
    )
    response["data.competitor.idAccount"] = idAccount
    return response
}

internal fun getAccountHandlerAdd(request: ApiRequest): Json {
    val idAccount = request.params["idAccount"].asInt
    val response = createResponse(request, "addHandler", "idAccount=$idAccount")
    response["data.lookup"] = "${QuartzApiServer.uri}/competitor/codeInformation/"
    response["data.handlerCode"] = ""
    response["data.giveName"] = ""
    response["data.familyName"] = ""
    return response
}


fun resetCompetitorSession(
    response: JsonNode,
    competitor: Competitor,
    authenticated: Boolean = true,
    access: Int = 0,
    idCompetitorReal: Int
) {
    if (competitor.isOnRow) {
        competitor.makeAccount()
        val token = Json()
        token["kind"] = "token.idCompetitor"
        token["idCompetitor"] = competitor.id
        token["idAccount"] = competitor.idAccount
        token["idCompetitorReal"] = idCompetitorReal

        val competitorReal = if (idCompetitorReal == competitor.id) competitor else Competitor(idCompetitorReal)

        response["control.session.mode"] = "replace"
        response["control.session.authenticated"] = authenticated
        response["control.session.access"] = access
        response["control.session.idAccount"] = competitor.idAccount
        response["control.session.idCompetitor"] = competitor.id
        response["control.session.idCompetitorReal"] = idCompetitorReal
        response["control.session.givenName"] = competitor.givenName
        response["control.session.familyName"] = competitor.familyName
        response["control.session.email"] = competitor.email
        if (access == 0 || idCompetitorReal != competitor.id) {
            response["control.session.systemAdministrator"] = competitorReal.systemAdministrator
            response["control.session.plazaSuperUser"] = competitorReal.plazaSuperUser
            response["control.session.ukaSuperUser"] = competitorReal.ukaSuperUser
        }
        response["control.session.registrationComplete"] = competitor.registrationComplete
        response["control.session.token"] = token.toJson().encrypt(keyPhrase)
    }
}

internal fun getCompetitorRevert(request: ApiRequest): Json {
    val response = createResponse(request)
    var idCompetitorReal = request.query["idCompetitorReal"].asInt
    resetCompetitorSession(response, Competitor(idCompetitorReal), true, 0, idCompetitorReal)
    response["control.action"] = "reverted"
    return response
}


internal fun getPaymentRequests(request: ApiRequest): Json {
    val response = createResponse(request)
    BankPaymentRequest().where("datePaid=0", "dateCreated") {
        if (!isExpired) {
            val item = response["data.requests"].addElement()
            item["accountName"] = accountName
            item["sortCode"] = sortCode
            item["accountNumber"] = accountNumber
            item["transactionReference"] = transactionReference
            item["amount"] = amount.money
            item["confirmed"] = !confirmationRequired || dateConfirmed.isNotEmpty()
            item["dateCreated"] = dateCreated
        }
    }
    return response
}

internal fun getBreedList(request: ApiRequest): Json {
    val query =
        DbQuery("SELECT idBreed, breedName FROM breed order by if(idBreed BETWEEN 9000 AND 9100, idBreed, 9999), breedName")
    val response = createResponse(request)
    while (query.next()) {
        val item = response["data.breeds"].addElement()
        item["value"] = query.getInt("idBreed")
        item["description"] = query.getString("breedName")
    }
    return response
}

internal fun getCountryList(request: ApiRequest): Json {
    val country = Country.select("true", "countryName")
    val response = createResponse(request)
    while (country.next()) {
        val item = response["data.countries"].addElement()
        item["value"] = country.code
        item["description"] = country.name
    }
    return response
}

internal fun getRegionList(request: ApiRequest): Json {
    val region = Region.select("true", "regionCode")
    val response = createResponse(request)
    while (region.next()) {
        val item = response["data.regions"].addElement()
        item["value"] = region.code
        item["description"] = region.name
    }
    return response
}

internal fun getDogStateList(request: ApiRequest): Json {
    val response = createResponse(request)

    fun addItem(code: Int, description: String) {
        val item = response["data.states"].addElement()
        item["value"] = code
        item["description"] = description
    }

    addItem(DOG_ACTIVE, "Active")
    addItem(DOG_RETIRED, "Retired")
    addItem(DOG_GONE, "Gone, but not forgotten")
    addItem(DOG_REMOVE, "Please Remove")

    return response
}

internal fun getClassCodeList(request: ApiRequest): Json {
    val response = createResponse(request)

    for (template in ClassTemplate.members) {
        if (template.isKc) {
            val node = response["data.classCodes"].addElement()
            node["code"] = template.code
            node["name"] = template.rawName
        }
    }
    response["data.classCodes"].sortBy("code")
    return response
}

internal fun getVoucherTypeList(request: ApiRequest): Json {
    val response = createResponse(request)

    fun add(code: Int, description: String) {
        val node = response["data.voucherTypes"].addElement()
        node["value"] = code
        node["description"] = description
    }

    add(VOUCHER_MEMBER, "Member")
    add(VOUCHER_COMMITTEE, "Committee Member")
    add(VOUCHER_JUDGE, "Judge")
    add(VOUCHER_RING_MANAGER, "Ring Manager")
    add(VOUCHER_SCRIME, "Scrime")
    add(VOUCHER_RING_PARTY, "Ring Party")
    add(VOUCHER_HELPER, "Helper")
    add(VOUCHER_MEASURER, "Measurer")
    add(VOUCHER_SYSTEM_MANAGER, "System Manager")

    return response
}


internal fun getAccountList(request: ApiRequest): Json {
    val response = createResponse(request)

    val plazaSuperUser = response["data.user.plazaSuperUser"].asBoolean
    val ukaSuperUser = response["data.user.ukaSuperUser"].asBoolean

    val familyName = request.query["familyName"].asString
    val petName = either(request.query["petNameKc"].asString, request.query["petNameUka"].asString)
    val competitorCode = request.query["competitorCode"].asString
    val accountCode = request.query["accountCode"].asString
    val idAccount = accountCode.toIntDef(-1)
    val registeredName = request.query["registeredName"].asString
    val postCode = request.query["postCode"].asString
    val email = request.query["email"].asString
    val phone = request.query["phone"].asString
    val idKc = request.query["idKc"].asString
    val idUkaDog = request.query["idUkaDog"].asInt
    val idUkaMember = request.query["idUkaMember"].asInt
    var list = ""
    val competitor = Competitor()

    if (familyName.isNotEmpty()) {
        DbQuery("SELECT idCompetitor FROM competitor WHERE TRIM(familyName) LIKE '%${familyName.trim()}%'").forEach {
            list = list.append(it.getString("idCompetitor"))
        }
    } else if (email.isNotEmpty()) {
        DbQuery("SELECT idCompetitor FROM competitor WHERE TRIM(email) LIKE '%${email.trim()}%'").forEach {
            list = list.append(it.getString("idCompetitor"))
        }
    } else if (postCode.isNotEmpty()) {
        DbQuery("SELECT idCompetitor FROM account WHERE TRIM(postCode) LIKE '%${postCode.trim()}%'").forEach {
            list = list.append(it.getString("idCompetitor"))
        }
    } else if (competitorCode.isNotEmpty()) {
        DbQuery("SELECT idCompetitor FROM competitor WHERE TRIM(competitorCode) LIKE '%${competitorCode.trim()}%'").forEach {
            list = list.append(it.getString("idCompetitor"))
        }
    } else if (idAccount > 0) {
        DbQuery("SELECT competitor.idCompetitor FROM account JOIN competitor USING(idAccount) WHERE account.idAccount=$idAccount").forEach {
            list = list.append(it.getString("idCompetitor"))
        }
    } else if (accountCode.isNotEmpty()) {
        DbQuery("SELECT competitor.idCompetitor FROM account JOIN competitor USING(idAccount) WHERE TRIM(accountCode) LIKE '%${accountCode.trim()}%' OR TRIM(accountCodeOld) LIKE '%${accountCode.trim()}%'").forEach {
            list = list.append(it.getString("idCompetitor"))
        }
    } else if (phone.isNotEmpty()) {
        DbQuery("SELECT idCompetitor FROM competitor WHERE TRIM(phoneMobile) LIKE '%${phone.trim()}%' OR TRIM(phoneOther) LIKE '%${phone.trim()}%'").forEach {
            list = list.append(it.getString("idCompetitor"))
        }
    } else if (idKc.isNotEmpty()) {
        DbQuery("SELECT DISTINCT idCompetitor FROM dog WHERE TRIM(idKc) LIKE '%${idKc.trim()}%'").forEach {
            list = list.append(it.getString("idCompetitor"))
        }
    } else if (petName.isNotEmpty()) {
        DbQuery("SELECT DISTINCT idCompetitor FROM dog WHERE TRIM(petName) LIKE '%${petName.trim()}%'").forEach {
            list = list.append(it.getString("idCompetitor"))
        }
    } else if (registeredName.isNotEmpty()) {
        DbQuery("SELECT DISTINCT idCompetitor FROM dog WHERE TRIM(registeredName) LIKE '%${registeredName.trim()}%'").forEach {
            list = list.append(it.getString("idCompetitor"))
        }
    } else if (idUkaDog > 0) {
        DbQuery("SELECT DISTINCT idCompetitor FROM dog WHERE idUka=$idUkaDog OR dogCode=$idUkaDog").forEach {
            list = list.append(it.getString("idCompetitor"))
        }
    } else if (idUkaMember > 0) {
        DbQuery("SELECT DISTINCT idCompetitor FROM competitor WHERE idUka=$idUkaMember").forEach {
            list = list.append(it.getString("idCompetitor"))
        }
    }

    var competitorNode = Json.nullNode()

    if (list.isNotEmpty()) {
        competitor.join(competitor.dog, competitor.account).select(
            "competitor.AliasFor=0 AND competitor.idCompetitor IN ($list)",
            "competitor.givenName, competitor.familyName"
        ).forEach {
            if (competitor.id != competitorNode["idCompetitor"].asInt) {
                competitorNode = response["data.competitors"].addElement()
                competitorNode.loadFromDataset(competitor)
                competitorNode["fullName"] = competitor.fullName
                competitorNode["accountFlags"] = competitor.account.flags

            }
            val node = competitorNode["dogs"].addElement()
            node.loadFromDataset(competitor.dog)
            if (ukaSuperUser) {
                node["ukaHeight"] = Height.getHeightName(competitor.dog.ukaHeightCode)
                node["ukaPerformance"] = Grade.getGradeName(competitor.dog.ukaPerformanceLevel)
                node["ukaSteeplechase"] = Grade.getGradeName(competitor.dog.ukaSteeplechaseLevel)
            }
        }
    }

    return response
}


internal fun getHeightList(request: ApiRequest): Json {
    val organization = request.params["organization"].asString
    val casual = request.query["casual"].asBoolean
    var sql =
        if (organization.isEmpty()) {
            "SELECT heightCode, name FROM height ORDER BY heightCode"
        } else if (casual) {
            "SELECT casual.heightCode, casual.name FROM height join height as casual on (casual.heightCode=height.casualCode) WHERE height.organization=${organization.quoted} AND NOT height.classHeightOnly ORDER BY casual.heightCode DESC"
        } else {
            "SELECT heightCode, name FROM height WHERE height.organization=${organization.quoted} AND NOT classHeightOnly ORDER BY heightCode DESC"
        }
    val query = DbQuery(sql)
    val response = createResponse(request)
    while (query.next()) {
        val item = response["data.heights"].addElement()
        item["value"] = query.getString("heightCode")
        item["description"] = query.getString("name")
    }
    return response
}

internal fun getGradeList(request: ApiRequest): Json {
    val organization = request.params["organization"].asString
    val where = if (organization.isNotEmpty()) "organization=${organization.quoted}" else "TRUE"
    val query = DbQuery("SELECT gradeCode, name FROM grade WHERE $where AND NOT classGradeOnly ORDER BY gradeCode")
    val response = createResponse(request)
    while (query.next()) {
        val item = response["data.grades"].addElement()
        item["value"] = query.getString("gradeCode")
        item["description"] = query.getString("name")
    }
    return response
}

fun addDataError(response: JsonNode, path: String, message: String) {
    val node = response["dataErrors"].addElement()
    node["path"] = path
    node["message"] = message
}

fun required_depricated(data: JsonNode, selector: String, item: String, response: JsonNode, description: String) {
    if (!data.has(item) || data[item].asString.isEmpty()) {
        addDataError(response, selector + "." + item, "$description is required")
    }
}

fun required(body: JsonNode, selector: String, response: JsonNode, description: String) {
    if (!body.has(selector) || body[selector].asString.isEmpty()) {
        addDataError(response, selector, "$description is required")
    }
}

fun dateValid(body: JsonNode, selector: String, response: JsonNode, description: String) {
    if (body.has(selector)) {
        if (!body[selector].isDate) {
            addDataError(response, selector, "Please enter $description as dd/mm/yy")
        } else if (body[selector].asDate > today) {
            addDataError(response, selector, "$description must not be in the future")
        }
    }
}

fun dateValidOrBlank(body: JsonNode, selector: String, response: JsonNode, description: String) {
    if (body.has(selector)) {
        val dateText = body[selector].asString.replace("\u200E", "")
        if (dateText.isNotEmpty() && !body[selector].isDate) {
            addDataError(response, selector, "Please enter $description as dd/mm/yy")
        } else if (dateText.isNotEmpty() && body[selector].asDate > today) {
            addDataError(response, selector, "$description must not be in the future")
        }
    }
}

fun numberValidOrBlank(body: JsonNode, selector: String, response: JsonNode, description: String) {
    if (body.has(selector)) {
        val numberText = body[selector].asString.trim()
        if (numberText.isNotEmpty() && numberText.toIntDef(-1) == -1) {
            addDataError(response, selector, "Please enter a number")
        }
    }
}

fun validate(condition: Boolean, selector: String, response: JsonNode, message: String) {
    if (!condition) {
        addDataError(response, selector, message)
    }
}

internal fun getAccountCompetitionCompetitors(request: ApiRequest): Json {

    val idAccount = request.params["idAccount"].asInt
    val idCompetition = request.params["idCompetition"].asInt
    val competition = Competition(idCompetition)

    var webTransaction = WebTransaction()
    webTransaction.seekEntry(idAccount, idCompetition, true)

    val response =
        createResponse(request, "accountCompetitionCompetitors", "idAccount=$idAccount", "idCompetition=$idCompetition")

    loadFromTable(response["data.competition"], competition, request)
    response["data.competition.paper"] = webTransaction.isPaper

    loadFromTable(response["data.account"], Account(idAccount), request)

    response["data.competitors"] = webTransaction.competitors

    if (competition.isKc) {
        for (day in competition.judges) {
            val node = response["data.judges"].addElement()
            for (judge in day) {
                WebTransaction.addOption(node["names"], judge.asString, judge.asString)
            }
            WebTransaction.addOption(node["names"], "", "")
        }

    }
    return response
}


internal fun getAccountCompetitionDogs(request: ApiRequest): Json {

    val idAccount = request.params["idAccount"].asInt
    val idCompetition = request.params["idCompetition"].asInt
    val competition = Competition(idCompetition)

    var webTransaction = WebTransaction()
    webTransaction.seekEntry(idAccount, idCompetition, true)


    val response =
        createResponse(request, "accountCompetitionDogs", "idAccount=$idAccount", "idCompetition=$idCompetition")

    loadFromTable(response["data.competition"], competition, request)
    response["data.competition.paper"] = webTransaction.isPaper
    loadFromTable(response["data.account"], Account(idAccount), request)

    if (competition.isKc) {
        response["data.competitors"] = webTransaction.competitors
        response["data.handlers"].setValue(webTransaction.handlerValues())
        response["data.entryOptions"].setValue(webTransaction.entryOptionValues(competition))


        for (day in competition.judges) {
            val node = response["data.judges"].addElement()
            for (judge in day) {
                WebTransaction.addOption(node["names"], judge.asString, judge.asString)
            }
            WebTransaction.addOption(node["names"], "", "")
        }

        for (dog in webTransaction.dogs) {
            val node = response["data.dogs"].addElement()
            node.setValue(dog)
            node["idCompetitorHandlerOld"] = node["idCompetitorHandler"]
        }
    } else if (competition.isUka) {
        if (competition.grandFinals) {
            var hasInvite = false
            for (entry in webTransaction.entries) {
                if (entry["invited"].asBoolean) hasInvite = true
            }
            if (!hasInvite) response["data.competition.needsInvite"] = true
        }
        response["data.dogs"] = webTransaction.dogs
    } else if (competition.isFab) {
        response["data.competitors"] = webTransaction.competitors
        response["data.dogs"] = webTransaction.dogs
    } else if (competition.isIndependent) {
        response["data.competitors"] = webTransaction.competitors
        response["data.handlers"].setValue(webTransaction.handlerValues())
        response["data.dogs"] = webTransaction.dogs
        response["data.heightOptions"].setValue(webTransaction.heightValues(competition))
        response["data.jumpHeightOptions"].setValue(Height.indHeightValues(competition.independentType, competition.clearRoundOnly))
        response["data.gradeOptions"].setValue(webTransaction.gradeValues(competition))
        if (competition.independentType.eq("aa")) {
            for (dog in response["data.dogs"]) {
                dog["heightOptions"].setValue(Height.indDogHeightValues(competition.independentType, true, dog["kcHeightCode"].asString, dog["ukaHeightCode"].asString))
                dog["entryOptions"].setValue(webTransaction.entryValues(competition, dog))
                if (dog["kcGradeCode"].asString > "KC05") {
                    dog["bundle"] = 1
                }
            }
        }
    }
    if (competition.isKc || competition.isIndependent) {
        response["data.competition.hasVouchers"] = competition.hasVouchers
    }
    return response
}

internal fun putAccountCompetitionDogs(body: Json, idAccount: Int, idCompetition: Int): Json {
    val response = createResourceResponse()
    val webTransaction = WebTransaction()
    val competition = Competition(idCompetition)
    webTransaction.seekEntry(idAccount, idCompetition, true)

    val globalBenefits = webTransaction.globalBenefits
    globalBenefits.clear()
    val vouchers = competition.voucherCodes.split(",")
    for (competitorNode in body["competitors"]) {
        val index = body["competitors"].indexOf(competitorNode)
        competitorNode["voucherCode"] = competitorNode["voucherCode"].asString.toUpperCase()
        competitorNode["voucherBenefits"].clear()
        val voucherList = competitorNode["voucherCode"].asString.noSpaces
        var errors = false
        if (voucherList.isNotEmpty()) {
            for (claim in voucherList.split(",")) {
                if (!vouchers.contains(claim)) {
                    addDataError(response, "competitors.$index.voucherCode", "$claim is not a valid voucher code")
                    errors = true
                }
            }
            if (!errors) {
                val benefits = Voucher.getBenefits(voucherList).asJson
                if (benefits.has("freeRuns")) {
                    val memberRates = benefits["memberRates"].asBoolean
                    benefits["freeRunsValue"] = benefits["freeRuns"].asInt *
                            if (memberRates) competition.entryFeeMembers else competition.entryFee
                }
                competitorNode["voucherCode"] = voucherList
                competitorNode["voucherBenefits"] = benefits
                if (benefits["allCampingFree"].asBoolean) {
                    globalBenefits["allCampingFree"] = true
                }
                if (benefits["campingNightsFree"].asInt > 0) {
                    globalBenefits["campingNightsFree"] =
                        globalBenefits["campingNightsFree"].asInt + benefits["campingNightsFree"].asInt
                }
                if (benefits["campingCredit"].asInt > 0) {
                    globalBenefits["campingCredit"] = globalBenefits["campingCredit"].asInt +
                            benefits["campingCredit"].asInt
                }
                if (benefits["generalCredit"].asInt > 0) {
                    globalBenefits["generalCredit"] = globalBenefits["generalCredit"].asInt +
                            benefits["generalCredit"].asInt
                }
                if (benefits["campingPermit"].asBoolean) {
                    globalBenefits["campingPermit"] = true
                }
                if (benefits["campingPriority"].asBoolean) {
                    globalBenefits["campingPriority"] = true
                }
            }
        }
    }

    if (response.has("dataErrors")) {
        return error(response, 1, "Please correct the errors and try again")
    }


    dbTransaction {
        webTransaction.competitors.setValue(body["competitors"])

        if (competition.isKc) {
            for (dog in body["dogs"]) {
                val idDog = dog["idDog"].asInt
                val idCompetitorHandler = dog["idCompetitorHandler"].asInt
                val idCompetitorHandlerOld = dog["idCompetitorHandlerOld"].asInt
                if (idCompetitorHandler != idCompetitorHandlerOld) {
                    webTransaction.changeHandler(idDog, idCompetitorHandlerOld, idCompetitorHandler)
                }
            }
            webTransaction.dogs.setValue(body["dogs"])
        } else if (competition.isIndependent) {
            webTransaction.dogs.setValue(body["dogs"])
            for (dog in webTransaction.dogs) {
                dog["jumpHeightCode"] = dog["jumpOption"].asString.replace("C", "")
                dog["clearRoundOnly"] = dog["jumpOption"].asString.endsWith("C")
                dog.drop("entryOptions")
                dog.drop("heightOptions")
            }
            if (competition.independentType.eq("aa")) {
                webTransaction.activeAgilityEntries()
            }
        }


        webTransaction.post()
    }


    return response
}

internal fun putAccountCompetitionCompetitors(body: Json, idCompetitor: Int, idCompetition: Int): Json {
    val response = createResourceResponse()
    val webTransaction = WebTransaction()
    dbTransaction {
        webTransaction.seekEntry(idCompetitor, idCompetition, true)
        webTransaction.competitors.setValue(body["competitors"])
        webTransaction.post()
    }

    return response
}


internal fun getAccountCompetitionCancel(request: ApiRequest): Json {
    val idAccount = request.params["idAccount"].asInt
    val idCompetition = request.params["idCompetition"].asInt

    val response =
        createResponse(request, "accountCompetitionCancel", "idAccount=$idAccount", "idCompetition=$idCompetition")

    loadFromTable(response["data.competition"], Competition(idCompetition), request)
    return response
}


internal fun getAccountCompetitionDog(request: ApiRequest): Json {

    val idAccount = request.params["idAccount"].asInt
    val idCompetition = request.params["idCompetition"].asInt
    val idDog = request.params["idDog"].asInt

    var webTransaction = WebTransaction()
    webTransaction.seekEntry(idAccount, idCompetition, true)


    val response = createResponse(
        request,
        "accountCompetitionDog",
        "idAccount=$idAccount",
        "idCompetition=$idCompetition",
        "idDog=$idDog"
    )

    loadFromTable(response["data.competition"], Competition(idCompetition), request)
    response["data.competition.paper"] = webTransaction.isPaper

    loadFromTable(response["data.account"], Account(idAccount), request)

    response["data.dog"] = webTransaction.dogs.searchElement("idDog", idDog)
    response["data.makeDefault"] = request.query["access"].asInt == 0

    return response
}

internal fun putAccountCompetitionDog(body: Json, idCompetitor: Int, idCompetition: Int, idDog: Int): Json {
    val response = createResourceResponse()
    val webTransaction = WebTransaction()

    dbTransaction {
        webTransaction.seekEntry(idCompetitor, idCompetition, true)
        val dogNode = webTransaction.dogs.searchElement("idDog", idDog)
        dogNode.setValue(body["dog"])
        webTransaction.post()
        webTransaction.dogDefault(idDog)
    }

    return response
}

internal fun getAccountCompetitionReset(request: ApiRequest): Json {
    val idAccount = request.params["idAccount"].asInt
    val idCompetition = request.params["idCompetition"].asInt
    val idCompetitor = request.query["idCompetitor"].asInt
    val paper = request.query["paper"].asBoolean
    val test = request.query["test"].asBoolean
    val response = createResponse(request)

    val webTransaction = WebTransaction.loadEntry(idAccount, idCompetition, idCompetitor, paper)
    if (test) {
        response["data.transaction"] = webTransaction.data
    }
    return response
}

internal fun getAccountCompetitionTransaction(request: ApiRequest): Json {
    val idAccount = request.params["idAccount"].asInt
    val idCompetition = request.params["idCompetition"].asInt
    val webTransaction = WebTransaction()
    val competition = Competition(idCompetition)
    webTransaction.seekEntry(idAccount, idCompetition, true)
    val response =
        createResponse(request, "accountCompetitionTransaction", "idAccount=$idAccount", "idCompetition=$idCompetition")
    loadFromTable(response["data.competition"], competition, request)
    response["data.competition.paper"] = webTransaction.isPaper

    loadFromTable(response["data.account"], Account(idAccount), request)
    response["data.transaction"] = webTransaction.data
    if (competition.isUkOpen) {
        addOptions(webTransaction, competition, response)
    }
    if (competition.isUkOpen && webTransaction.hasCamping()) {
        response["data.campingOption"] = webTransaction.camping["pitchType"].asInt + 1
    }
    return response
}

internal fun putAccountCompetitionTransaction(body: Json, idCompetitor: Int, idCompetition: Int): Json {
    var response = createResourceResponse()
    val dogs = body["transaction.dogs"]
    val transaction = body["transaction"]
    var hasEntries = false
    var dogsEntered = 0

    when (body["competition.idOrganization"].asInt) {
        ORGANIZATION_UK_OPEN -> {
            if (body["campingOption"].asInt == 0) {
                return error(response, 1, "You must select a camping option")
            }
            for (dog in dogs) {
                val index = dogs.indexOf(dog)

                if (dog["entered"].asBoolean) {
                    required(body, "transaction.dogs.$index.heightCode", response, "Height Code")
                    required(body, "transaction.dogs.$index.handler", response, "Handler")
                    hasEntries = true
                    dogsEntered++
                }
            }
        }
    }

    checkOptions(body, response)

    if (response.has("dataErrors")) {
        return error(response, 1, "The information you supplied is incomplete")
    }


    val webTransaction = WebTransaction()
    dbTransaction {
        webTransaction.seekEntry(idCompetitor, idCompetition, true)
        webTransaction.data.setValue(transaction)


        when (body["competition.idOrganization"].asInt) {
            ORGANIZATION_UK_OPEN -> {
                var competition = Competition(idCompetition)
                putOptions(webTransaction, body, hasEntries, dogsEntered)
                when (body["campingOption"].asInt) {
                    2 -> webTransaction.bookCamping(1, 1, competition.campingStart, (competition.campingDays - 1).bitsTo, 5000)
                    3 -> webTransaction.bookCamping(1, 2, competition.campingStart, (competition.campingDays - 1).bitsTo, 10000)
                    else -> webTransaction.camping.clear()
                }
            }
        }

        webTransaction.post()
    }

    return response
}

internal fun getAccountCompetitionSupplementary(request: ApiRequest): Json {
    val idAccount = request.params["idAccount"].asInt
    val idCompetition = request.params["idCompetition"].asInt
    var webTransaction = WebTransaction()
    webTransaction.seekEntry(idAccount, idCompetition, true)

    val response = createResponse(
        request,
        "accountCompetitionSupplementary",
        "idAccount=$idAccount",
        "idCompetition=$idCompetition"
    )
    response["data.lookup"] = "${QuartzApiServer.uri}/dog/codeInformation/"
    val competition = Competition(idCompetition)
    loadFromTable(response["data.competition"], competition, request)
    response["data.competition.paper"] = webTransaction.isPaper


    if (competition.isUka) {
        return getAccountCompetitionSupplementaryUka(response, webTransaction)
    } else if (competition.isIndependent) {
        return getAccountCompetitionSupplementaryInd(response, webTransaction, competition)
    } else {
        return getAccountCompetitionSupplementaryKc(response, webTransaction)
    }

}

internal fun putAccountCompetitionSupplementary(body: Json, idCompetitor: Int, idCompetition: Int): Json {
    val response = createResourceResponse()
    val competition = Competition(idCompetition)
    val webTransaction = WebTransaction()
    webTransaction.seekEntry(idCompetitor, idCompetition, true)

    return if (competition.isUka) {
        putAccountCompetitionSupplementaryUka(body, response, webTransaction)
    } else if (competition.isIndependent) {
        putAccountCompetitionSupplementaryInd(body, response, webTransaction)
    } else {
        putAccountCompetitionSupplementaryKc(body, response, webTransaction)
    }

}


internal fun putAccountCompetitionSupplementaryUka(body: Json, response: Json, webTransaction: WebTransaction): Json {
    dbTransaction {
        for (ukaTeam in body["ukaTeams"]) {
            val index = body["ukaTeams"].indexOf(ukaTeam)
            required_depricated(ukaTeam, "teams.$index", "team.teamName", response, "Team Name")
            val entry = webTransaction.getEntry(ukaTeam["idAgilityClass"].asInt, ukaTeam["idDog"].asInt)
            entry["team"] = ukaTeam["team"]
        }
        for (ukaPair in body["ukaPairs"]) {
            val entry = webTransaction.getEntry(ukaPair["idAgilityClass"].asInt, ukaPair["idDog"].asInt)
            entry["team"] = ukaPair["team"]
        }
        if (response.has("dataErrors")) {
            webTransaction.undoEdits()
        } else {
            webTransaction.post()
        }
    }
    if (response.has("dataErrors")) {
        return error(response, 1, "The information you supplied is incomplete")
    }
    return response

}


internal fun getAccountCompetitionSupplementaryUka(response: Json, webTransaction: WebTransaction): Json {
    for (entry in webTransaction.entries) {
        val template = ClassTemplate.select(entry["classCode"].asInt)
        when (template) {
            ClassTemplate.TEAM -> {
                val node = response["data.ukaTeams"].addElement()
                node["idAgilityClass"] = entry["idAgilityClass"].asInt
                node["idDog"] = entry["idDog"].asInt
                node["classCode"] = template.code
                if (entry.has("team")) {
                    node["team"] = entry["team"]
                } else {
                    val dog = Dog(entry["idDog"].asInt)
                    Team.updateMemberNode(
                        node["team.members.0"], idDog = dog.id,
                        dogCode = dog.code, petName = dog.cleanedPetName, competitorName = dog.owner.fullName
                    )
                    Team.updateMemberNode(node["team.members.1"], idDog = 0)
                    Team.updateMemberNode(node["team.members.2"], idDog = 0)
                }
            }
            ClassTemplate.SPLIT_PAIRS -> {
                val node = response["data.ukaPairs"].addElement()
                node["idAgilityClass"] = entry["idAgilityClass"].asInt
                node["idDog"] = entry["idDog"].asInt
                node["classCode"] = template.code
                if (entry.has("team")) {
                    node["team.members.0"] = entry["team.members.0"]
                    node["team.members.1"] = entry["team.members.1"]
                } else {
                    val dog = Dog(entry["idDog"].asInt)
                    Team.updateMemberNode(
                        node["team.members.0"], idDog = dog.id,
                        dogCode = dog.code, petName = dog.cleanedPetName, competitorName = dog.owner.fullName
                    )
                    Team.updateMemberNode(node["team.members.1"], idDog = 0)
                }
            }
            else -> doNothing()
        }
    }
    return response
}

internal fun getAccountCompetitionSupplementaryInd(response: Json, webTransaction: WebTransaction, competition: Competition): Json {
    if (competition.bonusCategories.isNotEmpty()) {
        response["data.competition.bonusCategories"] = competition.bonusCategories
        val dogsNode = response["data.dogs"]

        for (entry in webTransaction.entries) {
            val idDog = entry["idDog"].asInt
            val confirmed = entry["confirmed"].asBoolean
            if (confirmed) {
                dogsNode.searchElement("idDog", idDog, create = true) { jsonNode ->
                    jsonNode.setValue(webTransaction.getDog(idDog))
                    val dogBonusCategories = jsonNode["bonusCategories"].asString.split(",")
                    for (category in competition.bonusCategoriesRaw.split(",")) {
                        val node = jsonNode["categories"].addElement()
                        node["item"] = category
                        node["description"] = category.replace("_", " ")
                        node["selected"] = dogBonusCategories.contains(category)
                    }
                }
            }
        }
    }
    return response
}

internal fun putAccountCompetitionSupplementaryInd(body: Json, response: Json, webTransaction: WebTransaction): Json {
    for (dog in webTransaction.dogs) {
        dog["bonusCategories"] = ""
    }
    if (body.has("dogs")) {
        val dogs = body["dogs"]
        for (dog in dogs) {
            val idDog = dog["idDog"].asInt
            val dogNode = webTransaction.getDog(idDog)
            var bonusCategories = ""
            for (category in dog["categories"]) {
                val item = category["item"].asString
                val selected = category["selected"].asBoolean
                if (selected) bonusCategories = bonusCategories.append(item, ",")
            }
            dogNode["bonusCategories"] = bonusCategories
        }
    }
    webTransaction.post()
    return response
}


internal fun getAccountCompetitionSupplementaryKc(response: Json, webTransaction: WebTransaction): Json {

    response["data.handlers"].setValue(webTransaction.handlerValues())
    webTransaction.entries.sortBy("classNumber", "ClassName")

    for (entry in webTransaction.entries) {
        val index = webTransaction.entries.indexOf(entry)
        if (entry["confirmed"].asBoolean) {
            val template = ClassTemplate.select(entry["classCode"].asInt)
            if (template.teamSize > 1) {
                val node = response["data.teams"].addElement()
                node["idAgilityClass"] = entry["idAgilityClass"].asInt
                node["idDog"] = entry["idDog"].asInt
                node["classCode"] = template.code
                node["teamSize"] = template.teamSize
                node["teamReserves"] = template.teamReserves
                node["index"] = index
                if (entry.has("team")) {
                    node["team.teamName"] = entry["team.teamName"]
                    node["team.clubName"] = entry["team.clubName"]
                    for (i in 0..(template.teamSize + template.teamReserves) - 1) {
                        val teamNode = entry["team.members.$i"]
                        if (teamNode["idDog"].asInt > 0) {
                            node["team.members.$i"] = teamNode
                        } else {
                            Team.updateMemberNode(node["team.members.$i"], idDog = 0)
                        }
                    }
                } else {
                    val dog = Dog(entry["idDog"].asInt)
                    Team.updateMemberNode(
                        node["team.members.0"], idDog = dog.id, dogCode = dog.code,
                        petName = dog.cleanedPetName, competitorName = dog.owner.fullName
                    )
                    for (i in 1..template.teamSize + template.teamReserves - 1) {
                        Team.updateMemberNode(node["team.members.$i"], idDog = 0)
                    }
                }
            } else if (template.verifyHandler) {
                val dogs = response["data.verifyHandler.dogs"]
                if (dogs.isNull) {
                    dogs.setValue(webTransaction.dogs)
                }
                val idDog = entry["idDog"].asInt
                val classDate = entry["classDate"].asDate
                val dogNode = dogs.searchElement("idDog", idDog, true)
                val days = dogNode["days"]
                val day = days.searchElement("classDate", classDate)
                val classNode = day["classes"].addElement()
                classNode.setValue(entry)
                classNode["index"] = index
                val agilityClass = AgilityClass(entry["idAgilityClass"].asInt)
                if (agilityClass.found()) {
                    classNode["options"] = webTransaction.handlerValuesAgeRestricted(agilityClass)
                }
                val oldHandler = entry["idCompetitorOld"].asInt
                if (oldHandler != entry["idCompetitor"].asInt && !classNode["options"].hasElement("value", oldHandler)) {
                    classNode["idCompetitor"] = oldHandler
                }
                if (classNode["options"].size == 1) {
                    classNode["idCompetitor"] = classNode["options.0.value"]
                }
            } else if (template.dualHandler) {
                val dogs = response["data.dualHandler.dogs"]
                if (dogs.isNull) {
                    dogs.setValue(webTransaction.dogs)
                }
                val idDog = entry["idDog"].asInt
                val classDate = entry["classDate"].asDate
                val dogNode = dogs.searchElement("idDog", idDog, true)
                val days = dogNode["days"]
                val day = days.searchElement("classDate", classDate)
                val classNode = day["classes"].addElement()
                classNode.setValue(entry)
            }
        }
    }

    return response
}

internal fun putAccountCompetitionSupplementaryKc(body: Json, response: Json, webTransaction: WebTransaction): Json {
    dbTransaction {

        if (body.has("verifyHandler.dogs")) {
            val dogs = body["verifyHandler.dogs"]
            for (dog in dogs) {
                val dogIndex = dogs.indexOf(dog)
                for (day in dog["days"]) {
                    val dayIndex = dog["days"].indexOf(day)
                    for (agilityClass in day["classes"]) {
                        val classesIndex = day["classes"].indexOf(agilityClass)
                        val path = "verifyHandler.dogs.$dogIndex.days.$dayIndex.classes.$classesIndex"
                        val index = agilityClass["index"].asInt
                        val entry = webTransaction.entries[index]
                        if (entry.isNotNull) {
                            required(body, "$path.idCompetitor", response, "Handler")
                            val idCompetitor = agilityClass["idCompetitor"].asInt
                            entry["idCompetitor"] = idCompetitor
                        }
                    }
                }
            }
        }
        if (body.has("dualHandler.dogs")) {
            val dogs = body["dualHandler.dogs"]
            for (dog in dogs) {
                for (day in dog["days"]) {
                    for (agilityClass in day["classes"]) {
                        val entry =
                            webTransaction.entries.searchElement("idAgilityClass", agilityClass["idAgilityClass"].asInt)
                        if (entry.isNotNull) {
                            entry["dualHandler"] = agilityClass["dualHandler"].asString
                        }
                    }
                }
            }
        }

        for (team in body["teams"]) {
            val entry = webTransaction.getEntry(team["idAgilityClass"].asInt, team["idDog"].asInt)
            entry["team"] = team["team"]
        }

        if (response.has("dataErrors")) {
            webTransaction.undoEdits()
        } else {
            webTransaction.post()
        }
    }
    if (response.has("dataErrors")) {
        return error(response, 1, "The information you supplied is incomplete")
    }
    return response

}

internal fun getAccountCompetitionEntry(request: ApiRequest): Json {
    val idAccount = request.params["idAccount"].asInt
    val idCompetition = request.params["idCompetition"].asInt
    val competition = Competition(idCompetition)
    val webTransaction = WebTransaction()
    webTransaction.seekEntry(idAccount, idCompetition, true)

    var balance = 0
    var campingDeposit = 0

    if (competition.needsCampingDeposit) {
        balance = Ledger.balance(idAccount)
        campingDeposit = Ledger.getAmount(idAccount, idCompetition, LEDGER_CAMPING_DEPOSIT)
    }

    val campingCreditAvailable = webTransaction.globalBenefits["campingCredit"].asInt
    val generalCreditAvailable = webTransaction.globalBenefits["generalCredit"].asInt
    val freeFunds = balance + campingDeposit + campingCreditAvailable + generalCreditAvailable

    val response =
        createResponse(request, "accountCompetitionEntry", "idAccount=$idAccount", "idCompetition=$idCompetition")

    val mainCampingBlock = competition.mainCampingBlock

    loadFromTable(response["data.competition"], competition, request)
    response["data.competition.paper"] = webTransaction.isPaper
    response["data.competition.openForPaper"] = !competition.processed && webTransaction.isPaper
    response["data.competition.maxRuns"] = competition.maxRuns
    response["data.competition.allOrNothing"] = competition.allOrNothing
    response["data.competition.secondChance"] = competition.secondChance
    response["data.competition.groupCamping"] = !competition.noGroupCamping
    response["data.competition.grandFinals"] = competition.grandFinals
    response["data.competition.minimumFeeEqiv"] = competition.minimumFee / competition.entryFee
    response["data.competition.combinedFee"] = competition.combinedFee

    if (competition.minimumFee > 0) {
        response["data.competition.minimumFeeText"] =
            competition.minimumFee.money + (if (competition.minimumFeeMembers > 0) " (${competition.minimumFeeMembers.money} members)" else "")
    }
    if (competition.maximumFee > 0) {
        response["data.competition.maximumFeeText"] =
            competition.maximumFee.money + (if (competition.maximumFeeMembers > 0) " (${competition.maximumFeeMembers.money} members)" else "")
    }
    response["data.competition.mainCampingBlock"] = mainCampingBlock

    when (competition.idOrganization) {
        ORGANIZATION_KC -> getAccountCompetitionEntryKc(response, webTransaction, competition)
        ORGANIZATION_UKA -> getAccountCompetitionEntryUka(response, webTransaction, competition)
        ORGANIZATION_FAB -> getAccountCompetitionEntryFab(response, webTransaction, idCompetition)
        ORGANIZATION_INDEPENDENT -> getAccountCompetitionEntryInd(response, webTransaction, competition)
    }

    val freeCamping = webTransaction.globalBenefits["allCampingFree"].asBoolean
    val campingPriority = webTransaction.globalBenefits["campingPriority"].asBoolean

    val showWhenFull = webTransaction.camping["confirmed"].asBoolean || freeCamping || campingPriority
    val showCamping = competition.hasCamping

    val canNotCancel = webTransaction.camping["canNotCancel"].asBoolean
    val deposit = webTransaction.camping["deposit"].asInt

    if (showCamping) {
        val campingNode = response["data.camping"]
        campingNode["note"] = competition.campingNote
        if (!webTransaction.isPaper) {
            campingNode["full"] = competition.campingFull && !showWhenFull
            campingNode["terms"] = competition.campingTerms
        }
        campingNode["alert"] =
            if (competition.needsCampingDeposit && !freeCamping && !webTransaction.isPaper)
                """
                    Camping must be paid for when booking. If you do not have sufficient funds, your camping application will be
                    put on hold until you do (see Camping Terms below).
                """.trimIndent()
            else
                ""

        if (competition.hasCampingBlockDiscount) {
            campingNode["blockNote"] =
                "To get the special ${competition.campingRate.money} camping rate, tick all the boxes and the price will be adjusted at the checkout"
        }

        campingNode["groupName"] = webTransaction.camping["groupName"].asString
        campingNode["confirmed"] = webTransaction.camping["confirmed"].asBoolean
        campingNode["canNotCancel"] = canNotCancel
        campingNode["deposit"] = deposit
        campingNode["needsCampingDeposit"] = competition.needsCampingDeposit && !webTransaction.isPaper
        campingNode["freeFunds"] = freeFunds

        for (index in 0..competition.campingBlocks - 1) {
            val block = competition.getCampingBlock(index)
            if (block.dayBooking) {
                var campingDate = block.start
                while (campingDate != nullDate && block.end != nullDate && campingDate <= block.end) {
                    val blockNode = campingNode["blocks"].addElement()
                    blockNode["start"] = campingDate
                    blockNode["mask"] = Camping.blockMask(competition.campingStart, campingDate)
                    blockNode["rate"] = if (freeCamping) 0 else block.dayRate
                    blockNode["confirmed"] = webTransaction.hasCamping(campingDate)
                    campingDate = campingDate.addDays(1)
                }
            } else {
                val blockNode = campingNode["blocks"].addElement()
                blockNode["start"] = block.start
                blockNode["end"] = block.end
                blockNode["mask"] = block.blockMask
                blockNode["rate"] = if (freeCamping) 0 else block.blockRate
                blockNode["confirmed"] = webTransaction.hasCamping(block.start, block.end)
            }

        }
    }

    addOptions(webTransaction, competition, response)

    return response

}

fun addOptions(webTransaction: WebTransaction, competition: Competition, response: Json) {
    val isPlazaPaper = competition.isPlazaManaged
    val isPaper = webTransaction.isPaper

    if (competition.secondChance) {
        for (dog in webTransaction.dogs) {
            val node = response["data.options"].addElement()
            node["type"] = LEDGER_ITEM_SECOND_CHANCE
            node["description"] = "Second Chance Ticket - ${dog["petName"].asString}"
            node["unitPrice"] = 200
            node["confirmed"] = dog["secondChance"].asBoolean
            node["readOnly"] = false
            node["idDog"] = dog["idDog"].asInt
        }
    }

    if (!competition.noPosting || isPaper) {
        val postage = response["data.options"].addElement()
        postage["type"] =
            if (isPlazaPaper && isPaper) (if (competition.noPosting) LEDGER_ITEM_PAPER_ADMIN else LEDGER_ITEM_PAPER) else LEDGER_ITEM_POSTAGE
        postage["description"] = transactionToText(postage["type"].asInt)
        postage["unitPrice"] =
            if (isPlazaPaper && isPaper && !competition.noPosting) competition.adminFee else competition.postageFee
        postage["confirmed"] = isPaper ||
                webTransaction.hasItem(LEDGER_ITEM_POSTAGE) || webTransaction.data["state"].asString == "blank" || competition.mandatoryPostage
        postage["readOnly"] = (isPaper && !competition.isUka) || competition.mandatoryPostage
    }

    CompetitionExtra().where("idCompetition=${competition.id}", "ledgerItemType, description") {
        if (ledgerItemType == LEDGER_ITEM_CLOTHING) {
            for (size in sizes.split(",")) {
                val sizeCode = size.substringBefore(":")
                val sizeComment = size.substringAfter(":", "")
                val sizeText = if (sizeComment.isEmpty()) sizeCode else "$sizeCode ($sizeComment)"
                val item = response["data.clothing"].addElement()
                item["id"] = id
                item["type"] = ledgerItemType
                item["needsQuantity"] = needsQuantity
                item["description"] = description
                item["size"] = sizeCode
                item["sizeText"] = sizeText
                if (needsQuantity) {
                    item["quantity"] = webTransaction.getItem(ledgerItemType, id, size = sizeCode)["quantity"].asString
                }
                item["unitPrice"] = unitPrice
            }
        } else {
            val option = response["data.options"].addElement()
            option["id"] = id
            option["type"] = ledgerItemType
            option["needsQuantity"] = needsQuantity
            option["perDog"] = perDog
            option["description"] = description
            if (needsQuantity) {
                option["quantity"] = webTransaction.getItem(ledgerItemType)["quantity"].asString
            } else {
                option["confirmed"] =
                    webTransaction.hasItem(ledgerItemType) || webTransaction.data["state"].asString == "blank"
            }
            option["unitPrice"] = unitPrice

        }
    }
}

fun checkOptions(body: Json, response: Json) {
    if (body.has("clothing")) {
        for (item in body["clothing"]) {
            val index = body["clothing"].indexOf(item)
            val path = "clothing.$index.quantity"
            numberValidOrBlank(body, path, response, "Quantity")
        }
    }

    if (body.has("options")) {
        for (item in body["options"]) {
            val index = body["options"].indexOf(item)
            val path = "options.$index.quantity"
            numberValidOrBlank(body, path, response, "Quantity")
        }
    }
}

fun putOptions(webTransaction: WebTransaction, body: Json, hasEntries: Boolean, dogsEntered: Int = 0) {
    webTransaction.items.clear()

    if (body.has("clothing")) {
        for (item in body["clothing"]) {
            val index = body["clothing"].indexOf(item)
            val path = "clothing.$index.quantity"
            val quantity = item["quantity"].asString
        }
    }


    if (body.has("options")) {
        for (option in body["options"]) {
            if ((option["confirmed"].asBoolean || option["quantity"].asInt > 0) && (!option["type"].asInt.oneOf(
                    LEDGER_ITEM_POSTAGE,
                    LEDGER_ITEM_PAPER,
                    LEDGER_ITEM_PAPER_ADMIN
                ) || hasEntries)
            ) {
                val quantity = if (option["perDog"].asBoolean) dogsEntered else maxOf(option["quantity"].asInt, 1)
                webTransaction.LoadItem(option["type"].asInt, quantity, option["unitPrice"].asInt, option["code"].asInt, option["idDog"].asInt, option["idCompetitor"].asInt)
            }
        }
    }
    if (body.has("clothing")) {
        for (item in body["clothing"]) {
            val id = item["id"].asInt
            val type = item["type"].asInt
            val quantity = item["quantity"].asInt
            val size = item["size"].asString
            val unitPrice = item["unitPrice"].asInt
            webTransaction.LoadItem(type, maxOf(quantity, 0), unitPrice, id, size = size)
        }
    }
}

internal fun getAccountCompetitionEntryUka(response: Json, webTransaction: WebTransaction, competition: Competition) {
    val dates = ArrayList<Date>()
    val competitionDateEnd = webTransaction.misc["competitionDateEnd"].asDate
    val idCompetition = competition.id
    val dateQuery =
        DbQuery("SELECT DISTINCT classDate FROM agilityClass WHERE idCompetition = $idCompetition ORDER BY classDate")
    var i = 0
    while (dateQuery.next()) {
        dates.add(dateQuery.getDate("classDate"))
        response["data.days"][i++] = dateQuery.getDate("classDate")
    }

    var classCount = 0

    for (dogNode in webTransaction.dogs) {
        if ((dogNode["uka.dogRegistered"].asBoolean && dogNode["uka.handlerRegistered"].asBoolean) || competition.grandFinals) {
            val idDog = dogNode["idDog"].asInt
            val idUka = dogNode["idUka"].asInt
            val petName = dogNode["petName"].asString
            val dateOfBirth = dogNode["dateOfBirth"].asDate

            val ukaPerformanceLevel = dogNode["uka.performanceLevel"].asString
            val ukaSteeplechaseLevel = dogNode["uka.steeplechaseLevel"].asString

            val ukaPerformance = dogNode["uka.performance"].asBoolean
            val ukaSteeplechase = dogNode["uka.steeplechase"].asBoolean
            val ukaCasual = dogNode["uka.casual"].asBoolean
            val ukaNursery = dogNode["uka.nursery"].asBoolean
            val ukaJunior = dogNode["uka.junior"].asBoolean

            val ukaHeightCodePerformance = dogNode["uka.heightCodePerformance"].asString
            val ukaHeightCodeSteeplechase = dogNode["uka.heightCodeSteeplechase"].asString
            val ukaHeightCodeNursery = dogNode["uka.heightCodeNursery"].asString
            val ukaHeightCodeCasual = dogNode["uka.heightCodeCasual"].asString

            val thisDog = response["data.dogs"].addElement()
            thisDog["idDog"] = idDog
            thisDog["idUka"] = idUka
            thisDog["petName"] = petName

            val agilityClass = AgilityClass()
            val where = if (competition.grandFinals)
                """
                    idCompetition = $idCompetition
                """.trimIndent()
            else
                """ 
            idCompetition = $idCompetition AND (
                (entryRule = 1 AND FIND_IN_SET(${ukaPerformanceLevel.quoted}, REPLACE(gradeCodes, ";", ","))>0) OR
                (entryRule = 2 AND FIND_IN_SET(${ukaSteeplechaseLevel.quoted}, REPLACE(gradeCodes, ";", ","))>0) OR
                (entryRule = 0))
        """

            agilityClass.select(where, "classCode, suffix, classDate")

            var className = ""
            var preferred = true
            var heightCode = ""
            var thisClassGroup: JsonNode = Json()
            while (agilityClass.next()) {
                val classOk =
                    !competition.grandFinals && agilityClass.canEnterOnline && agilityClass.idUkaAgeEligible(dateOfBirth, competitionDateEnd) ||
                            competition.grandFinals && agilityClass.template.entryRule < ENTRY_RULE_CLOSED && webTransaction.isInvited(agilityClass.id, idDog)
                if (classOk) {
                    classCount++
                    val entryFee = if (competition.grandFinals) {
                        when (agilityClass.template) {
                            ClassTemplate.GRAND_PRIX_SEMI_FINAL -> 750
                            ClassTemplate.BEGINNERS_STEEPLECHASE_SEMI_FINAL -> 750
                            ClassTemplate.CIRCULAR_KNOCKOUT -> 750
                            ClassTemplate.JUNIOR_MASTERS -> 0
                            ClassTemplate.TEAM -> 2000
                            ClassTemplate.CHALLENGE_FINAL -> 1000
                            ClassTemplate.GAMES_CHALLENGE -> 1000
                            ClassTemplate.MASTERS -> 1000
                            ClassTemplate.SPLIT_PAIRS -> 1000
                            else -> 9999
                        }
                    } else {
                        agilityClass.entryFee
                    }
                    if (agilityClass.name != className) {
                        className = agilityClass.name
                        preferred = agilityClass.ukaPreferred(
                            ukaPerformance,
                            ukaSteeplechase,
                            ukaCasual,
                            ukaNursery,
                            ukaJunior
                        )
                        thisClassGroup = thisDog["agilityClasses"].addElement()
                        heightCode = ukaHeightCodePerformance
                        val template = agilityClass.template
                        if (competition.grandFinals) {
                            heightCode = webTransaction.enteredHeightCode(agilityClass.id, idDog)
                        } else if (template.isNursery) {
                            heightCode = ukaHeightCodeNursery
                        } else if (template.isCasual) {
                            heightCode = ukaHeightCodeCasual
                        } else if (template.isSteeplechase) {
                            heightCode = ukaHeightCodeSteeplechase
                        }
                        thisClassGroup["name"] = "${agilityClass.name} (${Height.getHeightName(heightCode)})"
                        thisClassGroup["classCode"] = agilityClass.code
                        thisClassGroup["heightCode"] = heightCode
                        thisClassGroup["fee"] = entryFee
                        thisClassGroup["preferred"] = preferred || competition.grandFinals
                        thisClassGroup["isSpecial"] = template.isSpecialClass && !competition.grandFinals
                        if (agilityClass.template == ClassTemplate.TEAM) {
                            thisClassGroup["name"] = "${agilityClass.name}"
                            response["data.competition.hasTeam"] = true
                            thisClassGroup["isTeam"] = true
                        }
                        (0 until dates.size).forEach { thisClassGroup["days"][it]["available"] = false }
                    }
                    if (agilityClass.template != ClassTemplate.CIRCULAR_KNOCKOUT || agilityClass.jumpHeightCodes.replace(
                            ";",
                            ","
                        ).split(",").contains(heightCode)
                    ) {
                        var dateIndex = dates.indexOf(agilityClass.date)
                        var confirmed = webTransaction.hasConfirmed(agilityClass.id, idDog)
                        thisClassGroup["days"][dateIndex]["classDate"] = agilityClass.date
                        thisClassGroup["days"][dateIndex]["idAgilityClass"] = agilityClass.id
                        thisClassGroup["days"][dateIndex]["available"] = true
                        thisClassGroup["days"][dateIndex]["hasEntered"] = confirmed
                        thisClassGroup["days"][dateIndex]["readOnly"] = competition.grandFinals && confirmed
                        thisClassGroup["days"][dateIndex]["confirmed"] =
                            if (webTransaction.data["state"].asString == "blank") preferred else confirmed
                    }
                }
            }
        }
    }

    if (competition.grandFinals) {
        response["data.competition.invited"] = classCount > 0
    }
}

internal fun getAccountCompetitionEntryFab(response: Json, webTransaction: WebTransaction, idCompetition: Int) {
    val dates = ArrayList<Date>()
    val competitionDateEnd = webTransaction.misc["competitionDateEnd"].asDate
    val dateQuery =
        DbQuery("SELECT DISTINCT classDate FROM agilityClass WHERE idCompetition = $idCompetition ORDER BY classDate")
    var i = 0
    while (dateQuery.next()) {
        dates.add(dateQuery.getDate("classDate"))
        response["data.days"][i++] = dateQuery.getDate("classDate")
    }

    for (dogNode in webTransaction.dogs) {
        val idOwner = dogNode["idCompetitor"].asInt
        val ownerNode = webTransaction.competitors.searchElement("idCompetitor", idOwner)

        val voucherBenefits = ownerNode["voucherBenefits"]
        val allRunsFree = voucherBenefits["allRunsFree"].asBoolean

        val idDog = dogNode["idDog"].asInt
        val petName = dogNode["petName"].asString
        val heightCode = dogNode["heightCode"].asString
        val ifcsHeightCode = dogNode["ifcsHeightCode"].asString
        val gradeAgility = dogNode["gradeAgility"].asString
        val gradeJumping = dogNode["gradeJumping"].asString
        val gradeSteeplechase = dogNode["gradeSteeplechase"].asString
        val collie = dogNode["collie"].asBoolean

        val fabAgility = dogNode["fab.agility"].asBoolean
        val fabJumping = dogNode["fab.jumping"].asBoolean
        val fabSteeplechase = dogNode["fab.steeplechase"].asBoolean
        val fabGrandPrix = dogNode["fab.grandPrix"].asBoolean
        val fabAllsorts = dogNode["fab.allsorts"].asBoolean
        val fabIfcs = dogNode["fab.ifcs"].asBoolean

        val thisDog = response["data.dogs"].addElement()
        thisDog["idDog"] = idDog
        thisDog["petName"] = petName
        thisDog["height"] = Height.getHeightName(heightCode)
        thisDog["ifcsHeight"] = Height.getHeightName(ifcsHeightCode)
        thisDog["idCompetitor"] = dogNode["idCompetitor"].asInt
        thisDog["idCompetitorHandler"] = dogNode["idCompetitorHandler"].asInt
        thisDog["collie"] = collie


        val agilityClass = AgilityClass()
        val where = """
            idCompetition = $idCompetition AND (
                (entryRule = $ENTRY_RULE_GRADE1 AND FIND_IN_SET(${gradeAgility.quoted}, gradeCodes)>0) OR
                (entryRule = $ENTRY_RULE_GRADE2 AND FIND_IN_SET(${gradeJumping.quoted}, gradeCodes)>0) OR
                (entryRule = $ENTRY_RULE_GRADE3 AND FIND_IN_SET(${gradeSteeplechase.quoted}, gradeCodes)>0) OR
                (entryRule = $ENTRY_RULE_ANY_GRADE) )
        """

        agilityClass.select(where, "classCode, suffix, classDate")

        var className = ""
        var preferred = true
        var thisClassGroup: JsonNode = Json()
        while (agilityClass.next()) {
            if (agilityClass.canEnterOnline) {
                if (agilityClass.name != className) {
                    className = agilityClass.name
                    preferred =
                        agilityClass.fabPreferred(fabAgility, fabJumping, fabSteeplechase, fabGrandPrix, fabAllsorts, fabIfcs)
                    thisClassGroup = thisDog["agilityClasses"].addElement()
                    val template = agilityClass.template
                    val classHeightCode = if (agilityClass.template.isIfcs) ifcsHeightCode else heightCode

                    thisClassGroup["name"] = "${agilityClass.name} (${Height.getHeightName(classHeightCode)})"
                    thisClassGroup["classCode"] = agilityClass.code
                    thisClassGroup["heightCode"] = classHeightCode
                    thisClassGroup["fee"] = if (allRunsFree) 0 else agilityClass.entryFee
                    thisClassGroup["preferred"] = preferred
                    thisClassGroup["isSpecial"] = template.isSpecialClass
                    (0 until dates.size).forEach { thisClassGroup["days"][it]["available"] = false }
                }
                var dateIndex = dates.indexOf(agilityClass.date)
                thisClassGroup["days"][dateIndex]["classDate"] = agilityClass.date
                thisClassGroup["days"][dateIndex]["idAgilityClass"] = agilityClass.id
                thisClassGroup["days"][dateIndex]["available"] = true
                thisClassGroup["days"][dateIndex]["hasEntered"] =
                    webTransaction.hasConfirmed(agilityClass.id, idDog)
                thisClassGroup["days"][dateIndex]["confirmed"] =
                    if (webTransaction.data["state"].asString == "blank") preferred else webTransaction.hasConfirmed(
                        agilityClass.id,
                        idDog
                    )
            }
        }
    }
}

internal fun getAccountCompetitionEntryKc(response: Json, webTransaction: WebTransaction, competition: Competition) {
    val idCompetition = competition.id
    val dates = ArrayList<Date>()
    val dateQuery =
        DbQuery("SELECT DISTINCT classDate FROM agilityClass WHERE idCompetition = $idCompetition ORDER BY classDate")
    var i = 0
    var symbols = ""
    while (dateQuery.next()) {
        dates.add(dateQuery.getDate("classDate"))
        response["data.days"][i++] = dateQuery.getDate("classDate")
    }

    for (dogNode in webTransaction.dogs) {

        val idOwner = dogNode["idCompetitor"].asInt
        val idHandler = dogNode["idCompetitorHandler"].asInt
        val ownerNode = webTransaction.competitors.searchElement("idCompetitor", idOwner)
        val HandlerNode = webTransaction.competitors.searchElement("idCompetitor", idHandler)

        val voucherBenefits = ownerNode["voucherBenefits"]
        val voucherBenefitsHandler = HandlerNode["voucherBenefits"]

        val memberRates = voucherBenefits["memberRates"].asBoolean
        val allRunsFree =
            voucherBenefits["allRunsFree"].asBoolean || voucherBenefitsHandler["allRunsFree"].asBoolean


        val idDog = dogNode["idDog"].asInt

        val gradeCode = dogNode["gradeCode"].asString
        val heightCode = dogNode["heightCode"].asString
        val entryOption = dogNode["entryOption"].asString


        val thisDog = response["data.dogs"].addElement()
        thisDog.setValue(dogNode)

        var combinedEntryFee = 0
        if (competition.combinedFee) {
            combinedEntryFee =
                if (allRunsFree) 0 else if (memberRates) competition.entryFeeMembers else competition.entryFee
            thisDog["entryFee"] = combinedEntryFee
        }

        val agilityClass = AgilityClass()
        val where = """
            idCompetition = $idCompetition AND
            FIND_IN_SET(${gradeCode.quoted}, REPLACE(gradeCodes, ";", ","))>0 AND
            FIND_IN_SET(${heightCode.quoted}, REPLACE(heightCodes, ";", ","))>0 AND
            FIND_IN_SET(${entryOption.quoted}, REPLACE(jumpHeightCodes, ";", ","))>0
        """

        agilityClass.select(where, "classDate, classNumber")

        var thisDate = nullDate
        var dateNode = Json.nullNode()

        while (agilityClass.next()) {
            if (agilityClass.canEnterOnline && agilityClass.isEligible(gradeCode, heightCode, entryOption)) {
                var ageBlock = false
                if (agilityClass.isAgeRestricted) {
                    ageBlock = true
                    for (competitor in webTransaction.competitors) {
                        if (agilityClass.inAgeRange(competitor["dateOfBirth"].asDate) && (!agilityClass.template.isYkc || competitor["ykcMember"].asBoolean)) {
                            ageBlock = false
                        }
                    }
                }
                if (agilityClass.template.isChampionship && !dogNode["hasKcChampWins"].asBoolean) {
                    thisDog["champBlock"] = true
                } else if (ageBlock) {
                    if (agilityClass.template.isYkc) {
                        response["data.ykcBlock"] = true
                    } else {
                        response["data.ageBlock"] = true
                    }
                } else {
                    val entryNode = webTransaction.getEntry(agilityClass.id, idDog)
                    val haveEntry = entryNode.isNotNull
                    val confirmed = if (haveEntry) entryNode["confirmed"].asBoolean else false
                    val highLighted = agilityClass.template.isKcRegular
                    if (agilityClass.date != thisDate) {
                        thisDate = agilityClass.date
                        dateNode = thisDog["days"].addElement()
                        dateNode["classDate"] = agilityClass.date
                        dateNode["maxRuns"] = competition.maxRuns


                    }
                    val classNode = dateNode["classes"].addElement()
                    classNode["idAgilityClass"] = agilityClass.id
                    classNode["idCompetitor"] =
                        if (haveEntry) entryNode["idCompetitor"] else dogNode["idCompetitorHandler"].asInt
                    classNode["name"] = agilityClass.name
                    classNode["confirmed"] =
                        if (webTransaction.data["state"].asString == "blank") false else confirmed
                    classNode["highlighted"] = highLighted
                    classNode["fee"] =
                        if (competition.combinedFee) combinedEntryFee
                        else if (allRunsFree) 0
                        else if (memberRates) agilityClass.entryFeeMembers
                        else agilityClass.entryFee
                    classNode["classCode"] = agilityClass.code
                    classNode["dualHandler"] = if (haveEntry) entryNode["dualHandler"] else ""
                    classNode["classNumber"] = agilityClass.number
                    classNode["runCount"] = agilityClass.runCount
                    classNode["hasEntered"] = confirmed
                    if (agilityClass.shortDescription.endsWith('*') && !symbols.contains("*")) symbols += "*"
                    if (agilityClass.shortDescription.endsWith('#') && !symbols.contains("#")) symbols += "#"
                }
            }

        }
    }
    response["data.symbols"] = symbols
}


internal fun getAccountCompetitionEntryInd(response: Json, webTransaction: WebTransaction, competition: Competition) {
    val idCompetition = competition.id
    val dates = ArrayList<Date>()
    val dateQuery =
        DbQuery("SELECT DISTINCT classDate FROM agilityClass WHERE idCompetition = $idCompetition ORDER BY classDate")
    var i = 0
    var symbols = ""
    while (dateQuery.next()) {
        dates.add(dateQuery.getDate("classDate"))
        response["data.days"][i++] = dateQuery.getDate("classDate")
    }

    for (dogNode in webTransaction.dogs) {

        val idOwner = dogNode["idCompetitor"].asInt
        val idHandler = dogNode["idCompetitorHandler"].asInt
        val ownerNode = webTransaction.competitors.searchElement("idCompetitor", idOwner)
        val HandlerNode = webTransaction.competitors.searchElement("idCompetitor", idHandler)

        val voucherBenefits = ownerNode["voucherBenefits"]
        val voucherBenefitsHandler = HandlerNode["voucherBenefits"]

        val memberRates = voucherBenefits["memberRates"].asBoolean
        val allRunsFree =
            voucherBenefits["allRunsFree"].asBoolean || voucherBenefitsHandler["allRunsFree"].asBoolean

        val idDog = dogNode["idDog"].asInt

        val gradeCode = dogNode["gradeCode"].asString
        val jumpHeightCode = dogNode["jumpHeightCode"].asString


        val thisDog = response["data.dogs"].addElement()
        thisDog.setValue(dogNode)

        if (competition.combinedFee) {
            thisDog["entryFee"] =
                if (allRunsFree) 0 else if (memberRates) competition.entryFeeMembers else competition.entryFee
        }

        val agilityClass = AgilityClass()
        val where = """
            idCompetition = $idCompetition AND
            FIND_IN_SET(${gradeCode.quoted}, REPLACE(gradeCodes, ";", ","))>0 AND
            FIND_IN_SET(${jumpHeightCode.quoted}, REPLACE(jumpHeightCodes, ";", ","))>0
        """

        agilityClass.select(where, "classDate, classNumber")

        var thisDate = nullDate
        var dateNode = Json.nullNode()

        while (agilityClass.next()) {
            if (agilityClass.canEnterOnline && agilityClass.isEligible(gradeCode, "*", jumpHeightCode)) {
                var ageBlock = false
                if (agilityClass.isAgeRestricted) {
                    ageBlock = true
                    for (competitor in webTransaction.competitors) {
                        if (agilityClass.inAgeRange(competitor["dateOfBirth"].asDate)) {
                            ageBlock = false
                        }
                    }
                }
                if (ageBlock) {
                    response["data.ageBlock"] = true
                } else {
                    val entryNode = webTransaction.getEntry(agilityClass.id, idDog)
                    val haveEntry = entryNode.isNotNull
                    val confirmed = if (haveEntry) entryNode["confirmed"].asBoolean else false
                    if (agilityClass.date != thisDate) {
                        thisDate = agilityClass.date
                        dateNode = thisDog["days"].addElement()
                        dateNode["classDate"] = agilityClass.date
                        dateNode["maxRuns"] = competition.maxRuns
                    }
                    val classNode = dateNode["classes"].addElement()
                    classNode["idAgilityClass"] = agilityClass.id
                    classNode["idCompetitor"] =
                        if (haveEntry) entryNode["idCompetitor"] else dogNode["idCompetitorHandler"].asInt
                    classNode["name"] = agilityClass.name
                    classNode["confirmed"] = if (webTransaction.data["state"].asString == "blank") false else confirmed
                    classNode["fee"] =
                        if (allRunsFree) 0 else if (memberRates) agilityClass.entryFeeMembers else agilityClass.entryFee
                    classNode["classCode"] = agilityClass.code
                    classNode["dualHandler"] = if (haveEntry) entryNode["dualHandler"] else ""
                    classNode["classNumber"] = agilityClass.number
                    classNode["runCount"] = agilityClass.runCount
                    classNode["hasEntered"] = confirmed
                }
            }
        }
    }
}

internal fun putAccountCompetitionEntry(body: Json, idAccount: Int, idCompetition: Int): Json {
    var response = createResourceResponse()
    val competitionNode = body["competition"]
    val dogs = body["dogs"]
    val campingNode = body["camping"]
    val webTransaction = WebTransaction()
    var supplementary = false
    var hasEntries = false
    var competition = Competition(idCompetition)

    checkOptions(body, response)

    if (response.has("dataErrors")) {
        return error(response, 1, "Please correct the errors and try again")
    }

    dbTransaction {
        webTransaction.seekEntry(idAccount, idCompetition, true)
        for (entry in webTransaction.entries) {
            entry["confirmed"] = false
        }

        when (competitionNode["idOrganization"].asInt) {
            ORGANIZATION_KC -> {
                for (dog in dogs) {
                    for (day in dog["days"]) {
                        for (classNode in day["classes"]) {
                            val template = ClassTemplate.select(classNode["classCode"].asInt)
                            if (classNode["confirmed"].asBoolean) {
                                hasEntries = true
                                if (template.teamSize > 1 || template.dualHandler || (template.verifyHandler && webTransaction.competitors.size > 1)) {
                                    supplementary = true
                                }
                                webTransaction.LoadEntry(
                                    classNode["idAgilityClass"].asInt,
                                    dog["idDog"].asInt,
                                    dog["idCompetitorHandler"].asInt,
                                    dog["gradeCode"].asString,
                                    dog["heightCode"].asString,
                                    dog["entryOption"].asString,
                                    classNode["fee"].asInt,
                                    dog["petName"].asString,
                                    classNode["name"].asString,
                                    day["classDate"].asDate,
                                    classNode["classCode"].asInt,
                                    classNode["classNumber"].asInt,
                                    classNode["dualHandler"].asString
                                )
                            } else if (day["hasEntered"].asBoolean) {
                                webTransaction.unConfrmEntry(day["idAgilityClass"].asInt, dog["idDog"].asInt)
                            }
                        }
                    }
                }
            }
            ORGANIZATION_INDEPENDENT -> {
                for (dog in dogs) {
                    for (day in dog["days"]) {
                        for (classNode in day["classes"]) {
                            val template = ClassTemplate.select(classNode["classCode"].asInt)
                            if (classNode["confirmed"].asBoolean) {
                                hasEntries = true
                                if (competition.bonusCategories.isNotEmpty()) {
                                    supplementary = true
                                }
                                webTransaction.LoadEntry(
                                    classNode["idAgilityClass"].asInt,
                                    dog["idDog"].asInt,
                                    dog["idCompetitorHandler"].asInt,
                                    dog["gradeCode"].asString,
                                    dog["heightCode"].asString,
                                    dog["jumpHeightCode"].asString,
                                    classNode["fee"].asInt,
                                    dog["petName"].asString,
                                    classNode["name"].asString,
                                    day["classDate"].asDate,
                                    classNode["classCode"].asInt,
                                    classNode["classNumber"].asInt,
                                    classNode["dualHandler"].asString,
                                    clearRoundOnly = dog["clearRoundOnly"].asBoolean
                                )
                            } else if (day["hasEntered"].asBoolean) {
                                webTransaction.unConfrmEntry(day["idAgilityClass"].asInt, dog["idDog"].asInt)
                            }
                        }
                    }
                }
            }

            ORGANIZATION_UKA -> {
                for (dog in dogs) {
                    for (group in dog["agilityClasses"]) {
                        val template = ClassTemplate.select(group["classCode"].asInt)
                        for (day in group["days"]) {
                            if (day["confirmed"].asBoolean) {
                                hasEntries = true
                                if (template.isTeamEvent /* || template.isJunior */) {
                                    supplementary = true && !webTransaction.misc["grandFinals"].asBoolean
                                }
                                webTransaction.LoadEntry(
                                    day["idAgilityClass"].asInt,
                                    dog["idDog"].asInt,
                                    dog["idCompetitor"].asInt,
                                    "",
                                    group["heightCode"].asString,
                                    group["heightCode"].asString,
                                    group["fee"].asInt,
                                    dog["petName"].asString,
                                    group["name"].asString,
                                    day["classDate"].asDate,
                                    group["classCode"].asInt
                                )
                            } else if (day["hasEntered"].asBoolean) {
                                webTransaction.unConfrmEntry(day["idAgilityClass"].asInt, dog["idDog"].asInt)
                            }
                        }
                    }
                }
            }
            ORGANIZATION_FAB -> {
                for (dog in dogs) {
                    for (group in dog["agilityClasses"]) {
                        val template = ClassTemplate.select(group["classCode"].asInt)
                        for (day in group["days"]) {
                            if (day["confirmed"].asBoolean) {
                                hasEntries = true
                                webTransaction.LoadEntry(
                                    day["idAgilityClass"].asInt,
                                    dog["idDog"].asInt,
                                    dog["idCompetitorHandler"].asInt,
                                    "",
                                    group["heightCode"].asString,
                                    group["heightCode"].asString,
                                    group["fee"].asInt,
                                    dog["petName"].asString,
                                    group["name"].asString,
                                    day["classDate"].asDate,
                                    group["classCode"].asInt,
                                    subDivision = if (dog["collie"].asBoolean && template.subDivisions.toLowerCase().contains("collie")) 1 else 0
                                )
                            } else if (day["hasEntered"].asBoolean) {
                                webTransaction.unConfrmEntry(day["idAgilityClass"].asInt, dog["idDog"].asInt)
                            }
                        }
                    }
                }
            }
        }


        if (body.has("camping")) {
            val camping = body["camping"]
            webTransaction.camping["groupName"] = camping["groupName"].asString
            val mainCampingBlock = competitionNode["mainCampingBlock"].asInt
            var dayFlags = 0
            for (block in camping["blocks"]) {
                val index = camping["blocks"].indexOf(block)
                if (block["confirmed"].asBoolean) {
                    dayFlags += block["mask"].asInt
                }
            }
            webTransaction.camping["dayFlags"] = dayFlags
            if (dayFlags > 0) webTransaction.camping["cancelled"] = false
        }

        putOptions(webTransaction, body, hasEntries)

        if (webTransaction.data["state"].asString == "blank") {
            webTransaction.data["state"] = "filled"
        }

        webTransaction.post()
    }
    if (supplementary) {
        response["control.action"] = "supplementary"
    }

    return response
}

fun getAccountCompetitionUkOpen(request: ApiRequest, competition: Competition, idAccount: Int): Json {
    val response = createResponse(request)
    val idCompetition = competition.id
    val access = response["data.user.access"].asInt

    val competitionDog = CompetitionDog()
    competitionDog.dog.joinToParent()

    loadFromTable(response["data.competition"], competition, request)

    competitionDog.select("competitionDog.idCompetition=$idCompetition AND competitionDog.idAccount=$idAccount")

    while (competitionDog.next()) {
        val node = response["data.entries"].addElement()
        node["petName"] = competitionDog.dog.petName
        node["handler"] = competitionDog.ukOpenHandler
        node["heightText"] = Height.getHeightName(competitionDog.ukOpenHeightCode)
        node["nation"] = competitionDog.ukOpenNation
    }

    val camping = Camping(idCompetition, idAccount)
    if (camping.found()) {
        response["data.pitchType"] = camping.pitchType
    }

    val ledgerItem = LedgerItem.select("idCompetition=$idCompetition AND idAccount=$idAccount")
    while (ledgerItem.next()) {
        if (!ledgerItem.type.oneOf(LEDGER_ITEM_CAMPING, LEDGER_ITEM_ENTRY)) {
            val node = response["data.competition.items"].addElement()
            node["type"] = ledgerItem.type
            node["quantity"] = ledgerItem.quantity
            node["description"] = ledgerItem.describe()
        }
    }

    Ledger().select("idCompetition = $idCompetition AND idAccount = $idAccount AND type IN ($LEDGER_ENTRY_FEES, $LEDGER_ENTRY_FEES_PAPER)")
        .withFirst {
            if (it.type == LEDGER_ENTRY_FEES_PAPER) {
                response["data.competition.paper"] = true
            } else if (access == 0 && competition.hasClosed || access > 0 && it.amountOwing == 0) {
                response["data.competition.locked"] = true
            }
        }

    return response
}

internal fun getAccountCompetitionTransferCamping(request: ApiRequest): Json {
    val idAccount = request.params["idAccount"].asInt
    val idCompetition = request.params["idCompetition"].asInt

    val response =
        createResponse(request, "transferCamping", "idAccount=$idAccount", "idCompetition=$idCompetition")
    response["data.lookup"] = "${QuartzApiServer.uri}/competitor/codeInformation/"
    response["data.idCompetitor"] = 0
    response["data.givenName"] = ""
    response["data.familyName"] = ""
    response["data.idAccountHandler"] = 0
    response["data.accountCode"] = ""
    response["data.accountName"] = ""
    return response
}

internal fun putAccountCompetitionTransferCamping(body: Json, idAccount: Int, idCompetition: Int): Json {
    val response = createResourceResponse()
    Camping().where("idCompetition=$idCompetition AND idAccount=$idAccount") {
        val idAccountTo = body["idAccountHandler"].asInt
        val error = transferToAccount(idAccountTo)
        println("ERROR: $error")
    }
    return response
}

internal fun getAccountCompetition(request: ApiRequest): Json {
    val idAccount = request.params["idAccount"].asInt
    val idCompetition = request.params["idCompetition"].asInt
    val competition = Competition(idCompetition)
    if (competition.isUkOpen) {
        return getAccountCompetitionUkOpen(request, competition, idAccount)
    }

    val response = createResponse(request)
    val access = response["data.user.access"].asInt

    response["data.user.email"] = Account(idAccount).emailList

    loadFromTable(response["data.competition"], competition, request)
    response["data.competition.hasManagedCamping"] = competition.hasManagedCamping
    response["data.competition.independentType"] = competition.independentType
    if (competition.grandFinals) response["data.competition.grandFinals"] = competition.grandFinals
    val documents =
        DbQuery("SELECT documentName FROM competitionDocument WHERE idCompetition=$idCompetition", "documentName")
    while (documents.next()) {
        if (documents.getString("documentName").oneOf("Ring Plan", "Running Orders")) {
            response["data.competition.documents"].addElement().setValue(documents.getString("documentName"))
        }
    }



    Ledger().select("idCompetition = $idCompetition AND idAccount = $idAccount AND type IN ($LEDGER_ENTRY_FEES, $LEDGER_ENTRY_FEES_PAPER)")
        .withFirst {
            if (it.type == LEDGER_ENTRY_FEES_PAPER) {
                response["data.competition.paper"] = true
            } else if (access == 0 && competition.hasClosed || access > 0 && it.amountOwing == 0) {
                response["data.competition.locked"] = true
            }
        }

    val entry = Entry()
    val orderBy =
        "competition.dateStart, competition.idCompetition, agilityClass.ClassDate, dog.petName, dog.registeredName, agilityClass.ClassNumber, agilityClass.classCode, agilityClass.suffix"
    entry.join(
        entry.team,
        entry.team.dog,
        entry.agilityClass,
        entry.agilityClass.competition,
        entry.agilityClass.competition
    )
    entry.select(
        "entry.idAccount=$idAccount AND (entryType<=$ENTRY_TRANSFER OR entryType=$ENTRY_DEPENDENT_CLASS) AND entryType<>$ENTRY_INVITE AND agilityClass.idCompetition=$idCompetition AND entry.progress<$PROGRESS_DELETED_LOW",
        orderBy
    )

    val camping = Camping()
    camping.find("idCompetition = ${competition.id} AND idAccount = $idAccount AND NOT rejected")

    val ledgerItem = LedgerItem()
    ledgerItem.select(
        "idAccount=$idAccount AND idCompetition=$idCompetition AND " +
                "NOT type IN ($LEDGER_ITEM_ENTRY, $LEDGER_ITEM_ENTRY_SURCHARGE, $LEDGER_ITEM_ENTRY_DISCOUNT, " +
                "$LEDGER_ITEM_CAMPING, $LEDGER_ITEM_CAMPING_CONFIRMED, $LEDGER_ITEM_ENTRY_CREDIT, " +
                "$LEDGER_ITEM_CAMPING_CREDIT)"
    )
    var dateNode = Json.nullNode()
    var dogNode = Json.nullNode()
    var date = nullDate
    var idDog = -1
    while (entry.next()) {
        if (!(competition.processed && entry.agilityClass.template.oneOf(ClassTemplate.KC_CHAMPIONSHIP, ClassTemplate.KC_CHAMPIONSHIP_HEAT))) {
            if (entry.agilityClass.date != date) {
                date = entry.agilityClass.date
                idDog = -1
                dateNode = response["data.competition.entries"].addElement()
                dateNode["date"] = date
            }
            if (entry.team.dog.id != idDog) {
                idDog = entry.team.dog.id
                dogNode = dateNode["dogs"].addElement()
                dogNode["petName"] = entry.team.dog.petName
                CompetitionDog().seek("idCompetition=$idCompetition AND idDog=${entry.team.dog.id}") {
                    dogNode["options"] = options(competition.idOrganization)
                }
            }

            if (entry.agilityClass.template == ClassTemplate.TEAM) {
                val teamNode = dateNode["teams"].addElement()
                teamNode["name"] = entry.team.teamName
                teamNode["members"] = entry.team.getMembers()
                val heightCode = entry.team.relayHeightCode
                teamNode["heightText"] =
                    if (heightCode.isEmpty()) "" else Height.getHeightName(entry.team.relayHeightCode)
            } else if (entry.agilityClass.template == ClassTemplate.SPLIT_PAIRS) {
                val pairNode = dateNode["pairs"].addElement()
                pairNode["members"] = entry.team.getMembers()
                val heightCode = entry.team.relayHeightCode
                pairNode["heightText"] =
                    if (heightCode.isEmpty()) "" else Height.getHeightName(entry.team.relayHeightCode)
            } else {
                val entryNode = dogNode["entries"].addElement()
                entryNode["className"] = entry.agilityClass.name
                if (competition.isUka || competition.isFab) {
                    entryNode["heightText"] = entry.jumpHeightText
                }
                entryNode["runningOrder"] = if (competition.processed) entry.runningOrder else 0
                entryNode["runsEntered"] = entry.runsEntered
            }
        }
    }

    if (camping.found()) {
        camping.loadNode(response["data.competition.camping"], competition)
        Camping.annotateNode(response["data.competition.camping"], competition, idAccount)
        if (camping.pending) {
            val accountBalance = Ledger.balance(idAccount)
            val paymentNeeded = camping.deposit - accountBalance
            response["data.competition.camping.accountBalance"] = accountBalance
            response["data.competition.camping.paymentNeeded"] = paymentNeeded
        }
    }

    while (ledgerItem.next()) {
        if (!ledgerItem.type.oneOf(LEDGER_ITEM_ENTRY_NFC)) {
            val node = response["data.competition.items"].addElement()
            node["type"] = ledgerItem.type
            node["quantity"] = ledgerItem.quantity
            node["description"] = ledgerItem.describe()
        }
    }

    if (competition.isKc) {
        val competitionDog = CompetitionDog()
        competitionDog.dog.joinToParent()
        competitionDog.select(
            "competitionDog.idCompetition=$idCompetition AND competitionDog.idAccount=$idAccount AND competitionDog.nfc",
            "dog.petName, dog.registeredName"
        )
        var nfc = ""
        while (competitionDog.next()) {
            nfc = nfc.append(competitionDog.dog.petName)
        }
        if (nfc.isNotEmpty()) {
            response["data.competition.nfc"] = nfc
        }
    }

    return response
}

internal fun getAccountCompetitionCheckout(request: ApiRequest): Json {
    val idCompetition = request.params["idCompetition"].asInt
    val competition = Competition(idCompetition)
    if (competition.isUkOpen) {
        return getAccountCompetitionCheckoutUkOpen(competition, request)
    } else {
        val idAccount = request.params["idAccount"].asInt

        val response = createResponse(
            request,
            "accountCompetitionCheckout",
            "idAccount=$idAccount",
            "idCompetition=${competition.id}"
        )

        val account = Account()
        account.find(idAccount)

        val webTransaction = WebTransaction()
        webTransaction.seekEntry(idAccount, competition.id, true)

        loadFromTable(response["data.account"], account, request)
        loadFromTable(response["data.competition"], competition, request)
        response["data.competition.paper"] = webTransaction.isPaper
        response["data.competition.noPost"] = !webTransaction.hasItem(LEDGER_ITEM_POSTAGE) &&
                !webTransaction.hasItem(LEDGER_ITEM_PAPER)
        response["data.competition.noPosting"] = competition.noPosting
        response["data.competition.grandFinals"] = competition.grandFinals
        response["data.competition.combinedFee"] = competition.combinedFee

        var runningTotal = 0
        var thisDay = nullDate
        var dayNode = Json.nullNode()
        var idDog = 0
        var dogNode = Json.nullNode()
        val summary = Json()
        var classesEntered = 0
        val competitorTotal = HashMap<Int, Int>()
        val dogDateFees = HashMap<String, Int>()

        webTransaction.entries.sortBy("classDate", "petName", "classCode", "className")
        for (entry in webTransaction.entries) {
            val dogDate = entry["petName"].asString + ":" + entry["classDate"].asDate.softwareDate
            dogDateFees[dogDate] = dogDateFees.getOrDefault(dogDate, 0) + entry["entryFee"].asInt
            if (entry["confirmed"].asBoolean) {
                classesEntered++
                if (entry["classDate"].asDate != thisDay) {
                    thisDay = entry["classDate"].asDate
                    dayNode = response["data.competition.days"].addElement()
                    dayNode["date"] = entry["classDate"].asDate
                    idDog = 0
                    dogNode = Json.nullNode()
                }
                if (entry["idDog"].asInt != idDog) {
                    idDog = entry["idDog"].asInt
                    dogNode = dayNode["dogs"].addElement()
                    dogNode.setValue(webTransaction.dogs.searchElement("idDog", idDog))
                }
                val node = dogNode["entries"].addElement()
                if (entry["classCode"].asInt == ClassTemplate.TEAM.code) {
                    node["className"] = "Team (${entry["team.teamName"].asString})"
                } else if (entry["classCode"].asInt == ClassTemplate.SPLIT_PAIRS.code) {
                    node["className"] =
                        "Split Pairs (${entry["team.members.0.petName"].asString} & ${entry["team.members.1.petName"].asString})"
                } else {
                    node["className"] = entry["className"].asString
                }
                node["fee"] = entry["entryFee"].asInt
                node["runsEntered"] = entry["runsEntered"].asInt

                /* Now add to entry count */
                var itemNode = Json.nullNode()
                for (item in summary) {
                    if (item["petName"].asString == entry["petName"].asString && ((item["entryFee"].asInt == entry["entryFee"].asInt) || competition.combinedFee)) {
                        itemNode = item
                    }
                }
                if (itemNode.isNull) {
                    itemNode = summary.addElement()
                    itemNode["idCompetitor"] = entry["idCompetitor"].asInt
                    itemNode["petName"] = entry["petName"].asString
                    itemNode["entryFee"] = entry["entryFee"].asInt
                    itemNode["count"] = 0
                }
                itemNode["count"] = itemNode["count"].asInt + entry["runsEntered"].asInt

                val idCompetitor = entry["idCompetitor"].asInt
                val feeSoFar = competitorTotal[idCompetitor] ?: 0
                competitorTotal[idCompetitor] = feeSoFar + entry["entryFee"].asInt
            }
        }

        var nfc = ""
        for (dogNode1 in webTransaction.dogs) {
            if (dogNode1["entryOption"].asString eq "NFC") {
                nfc = nfc.append(dogNode1["petName"].asString)
            }
        }
        if (nfc.isNotEmpty()) {
            response["data.competition.nfc"] = nfc
        }

        /* Now do entry summary */
        summary.sortBy("petName", "entryFee")
        for (item in summary) {
            val node = response["data.items"].addElement()
            val petName = item["petName"].asString
            val idCompetitor = item["idCompetitor"].asInt
            val count = item["count"].asInt
            val units = if (count == 1) "run" else "runs"
            val fee = item["entryFee"].asInt

            val member = competition.entryFeeMembers > 0 && fee == competition.entryFeeMembers
            val minimumFee =
                if (member && competition.minimumFeeMembers > 0) competition.minimumFeeMembers else competition.minimumFee
            val maximumFee =
                if (member && competition.maximumFeeMembers > 0) competition.maximumFeeMembers else competition.maximumFee

            if (minimumFee > 0) {
                if (competition.minFeeAllDays || competition.duration == 1) {
                    val surcharge = minimumFee - count * fee
                    if (surcharge > 0) {
                        val surchargeNode = response["data.items"].addElement()
                        surchargeNode["description"] = "$petName - Minimum Fee Surcharge"
                        surchargeNode["subTotal"] = surcharge
                        runningTotal += surcharge
                    }
                } else {
                    dogDateFees.forEach { key, fee ->
                        val petName = key.substringBefore(":")
                        val date = key.substringAfter(":").toDate()
                        val surcharge = minimumFee - fee
                        if (surcharge > 0) {
                            val surchargeNode = response["data.items"].addElement()
                            surchargeNode["description"] = "$petName - Minimum Fee Surcharge (${date.dayNameShort})"
                            surchargeNode["subTotal"] = surcharge
                            runningTotal += surcharge
                        }
                    }
                }
            }

            if (competition.combinedFee) {
                node["description"] = "$petName - $count runs"
                node["subTotal"] = fee
                runningTotal += fee
            } else if (count * fee > maximumFee && maximumFee > 0) {
                node["description"] = "$petName - $count $units @ MAXIMUM FEE"
                node["subTotal"] = maximumFee
                runningTotal += maximumFee
            } else {
                node["description"] = "$petName - $count $units @ ${fee.toCurrency()}"
                node["subTotal"] = count * fee
                runningTotal += count * fee
            }

        }

        /* process camping */
        var campingTotal = 0
        var campingDays = 0
        if (webTransaction.hasCamping()) {
            val freeCamping = webTransaction.globalBenefits["allCampingFree"].asBoolean
            val dayFlags = webTransaction.camping["dayFlags"].asInt

            for (index in 0..competition.campingBlocks - 1) {
                val block = competition.getCampingBlock(index)
                val days = block.days(dayFlags)
                if (days > 0) {
                    val item = response["data.items"].addElement()
                    val fee = if (freeCamping) 0 else block.fee(dayFlags)
                    if (block.useBlockRate(dayFlags)) {
                        item["description"] =
                            "Camping - ${block.start.format("EEE, dd MMM")} to ${block.end.format("EEE, dd MMM")}"
                    } else {
                        if (days == 1) {
                            item["description"] = "Camping - $days day @ ${(fee / days).toCurrency()}"
                        } else {
                            item["description"] = "Camping - $days days @ ${(fee / days).toCurrency()}"
                        }
                    }
                    item["subTotal"] = fee
                    runningTotal += fee
                    campingTotal += fee
                    campingDays += days
                }
            }

            response["data.competition.camping"].setValue(webTransaction.camping)
            Camping.annotateNode(response["data.competition.camping"], competition, idAccount, checkOut = true)
        }


        for (dogNode1 in webTransaction.dogs) {
            if (dogNode1["entryOption"].asString eq "NFC") {
                val item = response["data.items"].addElement()
                item["description"] = "${dogNode1["petName"].asString} - NFC @ £0.00"
                item["subTotal"] = 0
            }
        }

        // Sort out Voucher Credits
        var entryCredit = 0
        for (competitor in webTransaction.competitors) {
            val freeRunsValue = competitor["voucherBenefits.freeRunsValue"].asInt
            if (freeRunsValue > 0) {
                entryCredit += minOf(freeRunsValue, competitorTotal[competitor["idCompetitor"].asInt] ?: 0)
            }
        }

        var postage = 0
        for (item in webTransaction.items) {
            val type = item["type"].asInt
            val fee = item["itemFee"].asInt
            if (type.oneOf(LEDGER_ITEM_POSTAGE, LEDGER_ITEM_PAPER_ADMIN)) {
                runningTotal += fee
            }
        }


        val campingNightsFree = webTransaction.globalBenefits["campingNightsFree"].asInt
        val creditEquivalent =
            if (campingNightsFree > 0 && campingDays > 0) (campingTotal / campingDays) * campingNightsFree else 0

        val campingCreditAvailable = webTransaction.globalBenefits["campingCredit"].asInt + creditEquivalent
        var generalCreditAvailable = webTransaction.globalBenefits["generalCredit"].asInt

        val campingCreditUsed = minOf(campingCreditAvailable + generalCreditAvailable, campingTotal)
        if (campingCreditUsed > campingCreditAvailable) generalCreditAvailable -= campingCreditUsed - campingCreditAvailable

        val generalCreditUsed = minOf(generalCreditAvailable, runningTotal - entryCredit - campingCreditUsed)

        if (competition.needsCampingDeposit) {
            webTransaction.LoadItem(LEDGER_ITEM_ENTRY_CREDIT, 1, -entryCredit - generalCreditUsed)
            webTransaction.LoadItem(LEDGER_ITEM_CAMPING_CREDIT, 1, -campingCreditUsed)
        } else {
            webTransaction.LoadItem(LEDGER_ITEM_ENTRY_CREDIT, 1, -entryCredit - generalCreditUsed - campingCreditUsed)
        }

        webTransaction.post()

        var clothingCount = 0
        for (item in webTransaction.items) {
            val node = response["data.items"].addElement()
            val type = item["type"].asInt
            val code = item["code"].asInt
            val size = item["size"].asString
            val quantity = item["quantity"].asInt
            val unitPrice = item["unitPrice"].asInt
            val fee = item["itemFee"].asInt
            var description = if (code > 0) CompetitionExtra(code).description else transactionToText(type)
            val idDog = item["idDog"].asInt
            if (idDog > 0) {
                val dogNode = webTransaction.getDog(idDog)
                description = "${dogNode["petName"].asString} - $description"
            }
            if (size.isNotEmpty()) description += " size $size"
            if (code > 0) description = "$quantity x $description"
            node["description"] = description
            node["quantity"] = quantity
            node["unitPrice"] = unitPrice
            node["subTotal"] = fee
            if (!type.oneOf(LEDGER_ITEM_POSTAGE, LEDGER_ITEM_PAPER_ADMIN)) {
                runningTotal += fee
            }
            if (type == LEDGER_ITEM_CLOTHING) {
                clothingCount += quantity
            }
        }

        response["data.amounts.totalFees"] = runningTotal

        if (webTransaction.isPaper) {
            val cheque = webTransaction.payment["cheque"].asInt
            val cash = webTransaction.payment["cash"].asInt
            response["data.competition.cheque"] = if (cheque + cash == 0) runningTotal else cheque
            response["data.competition.cash"] = cash
        }

        if (competition.grandFinals) {
            if (classesEntered == 0) {
                response["data.competition.problem"] = "You have not ticked any classes, press 'Back' and try again."
            } else if (clothingCount == 0) {
                response["data.competition.problem"] = "You have not selected any clothing, press 'Back' and try again."
            }
        }




        return response
    }
}

internal fun getAccountCompetitionCheckoutUkOpen(competition: Competition, request: ApiRequest): Json {
    val idAccount = request.params["idAccount"].asInt
    val response =
        createResponse(request, "accountCompetitionCheckout", "idAccount=$idAccount", "idCompetition=${competition.id}")

    val webTransaction = WebTransaction()
    webTransaction.seekEntry(idAccount, competition.id, true)

    val account = Account(idAccount)
    loadFromTable(response["data.account"], account, request)
    loadFromTable(response["data.competition"], competition, request)
    response["data.competition.paper"] = webTransaction.isPaper

    var runningTotal = 0
    for (dog in webTransaction.dogs) {
        if (dog["entered"].asBoolean) {
            val node = response["data.items"].addElement()
            node["description"] =
                "${dog["handler"].asString} & ${dog["petName"].asString} (${Height.getHeightName(dog["heightCode"].asString)})"
            node["subTotal"] = competition.entryFee
            runningTotal += competition.entryFee
        }
    }

    if (webTransaction.hasCamping()) {
        when (webTransaction.camping["pitchType"].asInt) {
            1 -> {
                val node = response["data.items"].addElement()
                node["description"] = "Camping without Hook-up"
                node["subTotal"] = webTransaction.camping["feeOverride"].asInt
                runningTotal += webTransaction.camping["feeOverride"].asInt
            }
            2 -> {
                val node = response["data.items"].addElement()
                node["description"] = "Camping with Hook-up"
                node["subTotal"] = webTransaction.camping["feeOverride"].asInt
                runningTotal += webTransaction.camping["feeOverride"].asInt
            }
        }
    }


    var noPost = true
    for (item in webTransaction.items) {
        val node = response["data.items"].addElement()
        val type = item["type"].asInt
        val quantity = item["quantity"].asInt
        val unitPrice = item["unitPrice"].asInt
        val fee = item["itemFee"].asInt
        if (type.oneOf(LEDGER_ITEM_POSTAGE, LEDGER_ITEM_PAPER)) noPost = false
        node["description"] = transactionToText(type)
        node["quantity"] = quantity
        node["unitPrice"] = unitPrice
        node["subTotal"] = fee
        runningTotal += fee
    }
    response["data.competition.noPost"] = noPost
    response["data.amounts.totalFees"] = runningTotal

    if (webTransaction.isPaper) {
        val cheque = webTransaction.payment["cheque"].asInt
        val cash = webTransaction.payment["cash"].asInt
        response["data.competition.cheque"] = if (cheque + cash == 0) runningTotal else cheque
        response["data.competition.cash"] = cash
    }


    return response
}

internal fun putResource(request: ApiRequest): Json {
    val encryptedResource = request.params["resource"].asString
    val resource = if (encryptedResource.isEmpty()) Json() else Json(encryptedResource.decrypt(keyPhrase))
    val kind = resource["kind"].asString
    debug("putResource", "resource.kind=$kind")
    when (kind) {
        "competitorEmail" -> return putCompetitorEmail(request.body, resource["idCompetitor"].asInt)
        "competitorRegister" -> return putCompetitorRegister(request.body)
        "competitorAuthenticate" -> return putCompetitorAuthenticate(request.body)
        "competitorDogs" -> return putCompetitorDogs(request.body, resource["idCompetitor"].asInt)
        "addDog" -> return putAddDog(request.body)
        "addCompetitor" -> return putCompetitorAdd(request.body, resource["idCompetitorReal"].asInt)
        "addAccountCompetitor" -> return putAccountCompetitor(request.body, resource["idAccount"].asInt)
        "addHandler" -> return putAddHandler(request.body, resource["idAccount"].asInt)
        "transferCamping" -> return putAccountCompetitionTransferCamping(request.body, resource["idAccount"].asInt, resource["idCompetition"].asInt)
        "accountCompetitionDog" -> return putAccountCompetitionDog(
            request.body,
            resource["idAccount"].asInt,
            resource["idCompetition"].asInt,
            resource["idDog"].asInt
        )
        "accountCompetitionDogs" -> return putAccountCompetitionDogs(
            request.body,
            resource["idAccount"].asInt,
            resource["idCompetition"].asInt
        )
        "accountCompetitionCompetitors" -> return putAccountCompetitionCompetitors(
            request.body,
            resource["idAccount"].asInt,
            resource["idCompetition"].asInt
        )
        "accountCompetitionTransaction" -> return putAccountCompetitionTransaction(
            request.body,
            resource["idAccount"].asInt,
            resource["idCompetition"].asInt
        )
        "accountCompetitionEntry" -> return putAccountCompetitionEntry(
            request.body,
            resource["idAccount"].asInt,
            resource["idCompetition"].asInt
        )
        "accountCompetitionSupplementary" -> return putAccountCompetitionSupplementary(
            request.body,
            resource["idAccount"].asInt,
            resource["idCompetition"].asInt
        )
        "accountCompetitionCheckout" -> return putAccountCompetitionCheckout(
            request.body,
            resource["idAccount"].asInt,
            resource["idCompetition"].asInt
        )
        "accountCompetitionCancel" -> return putAccountCompetitionCancel(
            resource["idAccount"].asInt,
            resource["idCompetition"].asInt
        )
        "competitor" -> return putCompetitor(request.body, resource["idCompetitor"].asInt)
        "dog" -> return putDog(request.body)
        "dog_share" -> return putDogShare(request.body)
        "competition" -> return putCompetition(request.body, resource["idCompetition"].asInt)
        "competitorPaymentCard" -> return putCompetitorPaymentCardAdd(resource["idCompetitor"].asInt)
        "accountLedger" -> return putAccountLedger(request.body, resource["idAccount"].asInt)
        "account" -> return putAccount(request.body, resource["idAccount"].asInt)
        "requestResetPassword" -> return putRequestResetPassword(request.body)
        "resetPassword" -> return putResetPassword(request.body)
        "kc_grade_review" -> return putDogKcGradeReview(request.body)
        "accountUka" -> return putAccountUka(request.body, resource["idAccount"].asInt)
        "accountUkaCheckout" -> return putAccountUkaCheckout(request.body, resource["idAccount"].asInt)
        "accountRefund" -> return putAccountRefund(request.body, resource["idAccount"].asInt)
        "voucher" -> return putVoucher(request.body)
        "competitionCamping" -> return putCompetitionCamping(request.body, resource["idCompetition"].asInt)
        "unallocatedReceipts" -> return putLedgerUnallocatedReceipts(request.body)
        "team" -> return putTeam(request.body)
        "stock_movement" -> return putStockMovement(request.body, resource["idAccount"].asInt)
        "stock_movement_confirm" -> return putStockMovementConfirm(request.body, resource["idAccount"].asInt)
        "competition_stock" -> return putCompetitionStock(request.body, resource["idCompetition"].asInt)
        "entityOfficial" -> return putEntityOfficial(request.body, resource["idEntityOfficial"].asInt)
        "addEntryOfficial" -> return putEntityOfficial(request.body, 0)
        "entity" -> return putEntity(request.body, resource["idEntity"].asInt)
    }
    throw Wobbly("Unknown resource kind ($kind) in putResource")
}

internal fun postStarling(request: ApiRequest): Json {
    val response = createResponse(request)
    debug("starling", "webhook: " + request.body.toJson(pretty = true))

    val webhookNotificationUid = request.body["webhookNotificationUid"].asString
    val timestamp = request.body["timestamp"].asDate
    val accountHolderUid = request.body["accountHolderUid"].asString
    val webhookType = request.body["webhookType"].asString
    val uid = request.body["uid"].asString
    val customerUid = request.body["customerUid"].asString
    val content = request.body["content"]
    if (webhookType.startsWith("TRANSACTION_")) {
        val className = content["class"].asString
        val transactionUid = content["transactionUid"].asString
        val amount = content["amount"].asDouble.pence
        val sourceCurrency = content["sourceCurrency"].asString
        val sourceAmount = content["amount"].asDouble.pence
        val counterParty = content["counterParty"].asString
        val reference = content["reference"].asString
        val type = content["type"].asString
        val forCustomer = content["forCustomer"].asString

        val source = webhookType.dropLeft(12)
        val paidOut = if (amount < 0) -amount else 0
        val paidIn = if (amount >= 0) amount else 0

        BankTransaction.add(
            transactionUid,
            timestamp,
            source.toLowerCase(),
            reference,
            counterParty,
            paidOut,
            paidIn,
            0
        )

        PlazaAdmin.dequeuePlaza()

    }
    return response
}


internal fun putSpreadsheet(request: ApiRequest): Json {
    val idCompetition = request.params["idCompetition"].asInt
    val response = createResourceResponse()

    //   try {
    val path = request.body["path"].asString
    val identifier = importWorkbook(path, idCompetitionForce = idCompetition)
    when (identifier) {
        PlazaAdmin.SHEET_RING_PLAN_OVERVIEW_TEMPLATE -> {
            response["control.action"] = "popup"
            response["control.message"] =
                "Your spreadsheet has been successfully processed, download the 'Ring Plan (Provisional)' report to confirm the details"
        }
        PlazaAdmin.SHEET_SHOW_TEMPLATE -> {
            response["control.action"] = "popup"
            response["control.message"] =
                "Your spreadsheet has been successfully processed, check the show diary to confirm the details"
        }
        PlazaAdmin.SHEET_ENTRIES_TEMPLATE -> {
            response["control.action"] = "popup"
            response["control.message"] = "Your spreadsheet has been successfully processed"
        }
        PlazaAdmin.SHEET_RUNNING_ORDERS_TEMPLATE -> {
            response["control.action"] = "popup"
            response["control.message"] = "Your running orders have been successfully processed"
        }
        PlazaAdmin.SHEET_AWARD_TEMPLATE -> {
            response["control.action"] = "popup"
            response["control.message"] = "Your awards have been successfully processed"
        }
        else -> {
            response["control.action"] = "popup"
            response["control.message"] = "Your spreadsheet has been successfully processed"
        }
    }

    /*
    } catch (e: Throwable) {
        response["control.action"] = "error"
        response["control.message"] = "ERROR: " + e.message
    }

     */
    return response
}

fun putUkaMeasurements(request: ApiRequest): Json {
    val response = createResourceResponse()
    try {
        val path = request.body["path"].asString
        importUkaHeights(path)
        response["control.action"] = "popup"
        response["control.message"] = "The heights have been successfully processed"
    } catch (e: Throwable) {
        response["control.action"] = "error"
        response["control.message"] = "ERROR: " + e.message
    }
    return response

}

internal fun getCompetitionDocument(request: ApiRequest): Json {
    val idCompetition = request.params["idCompetition"].asInt
    val name = request.params["name"].asString
    val competitionDocument =
        CompetitionDocument.select("idCompetition = $idCompetition AND documentName=${name.quoted}")
    val response = createResponse(request)
    if (competitionDocument.first()) {
        response["content"] = "application/pdf"
        response["path"] =
            Global.showDocumentPath(idCompetition, competitionDocument.documentName, "pdf", canRegenerate = false)
    } else {
        response["control.action"] = "error"
        response["control.message"] = "ERROR: Document not found"
    }
    return response
}


internal fun putCompetitionDocument(request: ApiRequest): Json {
    val idCompetition = request.params["idCompetition"].asInt
    val documentName = request.params["name"].asString
    val competition = Competition(idCompetition)
    val competitionDocument = CompetitionDocument()
    val documentPath = "/published/${competition.uniqueName}/${documentName}.pdf"
    val target = Global.showDocumentPath(idCompetition, documentName, "pdf", canRegenerate = false)
    val response = createResourceResponse()
    try {
        val source = request.body["path"].asString
        Files.move(File(source).toPath(), prepareFile(target).toPath(), StandardCopyOption.REPLACE_EXISTING)
        competitionDocument.select("idCompetition=$idCompetition AND documentName=${documentName.quoted}")
        if (!competitionDocument.first()) {
            competitionDocument.append()
            competitionDocument.idCompetition = idCompetition
            competitionDocument.documentName = documentName
        }
        competitionDocument.documentPath = documentPath
        competitionDocument.post()
        response["control.action"] = "popup"
        response["control.message"] = "Your document has been uploaded"
    } catch (e: Throwable) {
        response["control.action"] = "error"
        response["control.message"] = "ERROR: " + e.message
    }
    return response
}


internal fun putCompetitionClasses(request: ApiRequest): Json {
    val idCompetition = request.params["idCompetition"].asInt
    val response = createResourceResponse()
    try {
        val path = request.body["path"].asString
        importWorkbook(path, idCompetitionForce = idCompetition)
        response["control.action"] = "popup"
        response["control.message"] =
            "Your spreadsheet has been successfully processed, download the 'Ring Plan (Provisional)' report to confirm the details"
    } catch (e: Throwable) {
        response["control.action"] = "error"
        response["control.message"] = "ERROR: " + e.message
    }
    return response
}

internal fun putCompetitionAwards(request: ApiRequest): Json {
    val response = createResourceResponse()
    try {
        val path = request.body["path"].asString
        PlazaAdmin.importPlacesSheet(path)
        response["control.action"] = "popup"
        response["control.message"] = "Your Awards spreadsheet has been successfully processed"
    } catch (e: Throwable) {
        response["control.action"] = "error"
        response["control.message"] = "ERROR: " + e.message
    }
    return response
}

internal fun putUkaTransfers(request: ApiRequest): Json {
    val response = createResourceResponse()
    try {
        val path = request.body["path"].asString
        importUKABalances(path)
        response["control.action"] = "popup"
        response["control.message"] = "Your UKA transfers have been successfully processed"
    } catch (e: Throwable) {
        response["control.action"] = "error"
        response["control.message"] = "ERROR: " + e.message
    }
    return response
}

internal fun putSwapTransfers(request: ApiRequest): Json {
    val response = createResourceResponse()
    try {
        val path = request.body["path"].asString
        importSwapBalances(path)
        response["control.action"] = "popup"
        response["control.message"] = "Your UKA transfers have been successfully processed"
    } catch (e: Throwable) {
        response["control.action"] = "error"
        response["control.message"] = "ERROR: " + e.message
    }
    return response
}


internal fun putAccount(body: Json, idAccount: Int): Json {
    var response = createResourceResponse()
    val data = body["account"]

    required_depricated(data, "account", "streetAddress", response, "Street address")
    required_depricated(data, "account", "town", response, "Town")
    required_depricated(data, "account", "postcode", response, "Postcode")

    if (response.has("dataErrors")) {
        return error(response, 1, "The information you supplied is incomplete")
    }
    data["registrationComplete"] = true
    update(response, Account(), data, "idAccount", idAccount)
    return response
}


fun getBindings(body: Json, selector: String): ArrayList<String> {
    val result = ArrayList<String>()
    body["bindings"].filter { it.asString.startsWith(selector + ".") }
        .forEach { result.add(it.asString.drop(selector.length + 1)) }
    return result
}

internal fun putDogShare(body: Json): Json {
    var response = createResourceResponse()
    val dogNode = body["dog"]
    val idDog = dogNode["idDog"].asInt
    val idCompetitor = body["idCompetitor"].asInt
    val dog = Dog(idDog)
    if (idCompetitor > 0) {
        val competitor = Competitor(idCompetitor)
        dog.idAccountShared = competitor.idAccount
        dog.shareCode = competitor.code
    } else {
        dog.idAccountShared = 0
        dog.shareCode = ""
    }
    dog.post()
    return response
}


internal fun putDog(body: Json): Json {
    var response = createResourceResponse()
    val dogNode = body["dog"]
    val bindings = getBindings(body, "dog")

    val idDog = dogNode["idDog"].asInt

    required(body, "dog.petName", response, "Pet Name")
    required(body, "dog.gender", response, "Gender")
    required(body, "dog.idBreed", response, "Breed")


    val dogState = dogNode["dogState"].asInt

    if (dogState == 0) {
        val idKc = dogNode["idKC"].asString.toUpperCase().noSpaces
        val idKcValid = Dog.KcRegistrationValid(idKc, dogNode["code"].asInt)
        validate(idKc.isEmpty() || idKcValid, "dog.idKC", response, "Not a valid KC registration number")

        if (idKcValid) {
            required(body, "dog.registeredName", response, "Registered Name")
            required(body, "dog.kcHeightCode", response, "Height")
            required(body, "dog.kcGradeCode", response, "Grade")
        }

        /*
        if (idKc.isNotEmpty() && idKcValid) {
            val query = DbQuery("SELECT TRUE FROM dog WHERE idDog<>$idDog AND idKc=${idKc.quoted} AND idAccount>0 AND AliasFor=0 LIMIT 1")
            validate(!query.found(), "dog.idKC", response, "KC registration number belongs to another household")
        }
        */
        dogNode["idKC"] = idKc
    }

    val kcGradeCode = dogNode["kcGradeCode"].asString
    val kcGradeCodeWas = dogNode["kcGradeCodeWas"].asString
    val kcGradeChanged = kcGradeCode != kcGradeCodeWas

    if (kcGradeCode != "KC01" && bindings.contains("kcGrade2")) {
        when (kcGradeCode) {
            "KC02" -> dateValidOrBlank(body, "dog.kcGrade2", response, "Win out date")
            "KC03" -> dateValidOrBlank(body, "dog.kcGrade3", response, "Win out date")
            "KC04" -> dateValidOrBlank(body, "dog.kcGrade4", response, "Win out date")
            "KC05" -> dateValidOrBlank(body, "dog.kcGrade5", response, "Win out date")
            "KC06" -> dateValidOrBlank(body, "dog.kcGrade6", response, "Win out date")
            "KC07" -> dateValidOrBlank(body, "dog.kcGrade7", response, "Win out date")
        }
    }

    if (kcGradeCode == "KC07" && bindings.contains("kcChampWins.0.date")) {
        for (i in 0..4) {
            if (dogNode["kcChampWins.$i.show"].asString.isNotEmpty() || dogNode["kcChampWins.$i.class"].asString.isNotEmpty()) {
                dateValid(body, "dog.kcChampWins.$i.date", response, "Qualifying win date")
            } else {
                dateValidOrBlank(body, "dog.kcChampWins.$i.date", response, "Qualifying win date")
            }
        }
    }

    dateValidOrBlank(body, "dog.dateOfBirth", response, "Date of birth")

    if (response.has("dataErrors")) {
        return error(response, 1, "The information you supplied is incomplete")
    }

    val dog = Dog(idDog)
    val idKcOld = if (dog.found()) dog.idKC else ""
    val kcCurrentGrade = Json(dog.kcGradeArray)
    dbTransaction {
        if (dogNode["ukaHeightCode"].asString != dog.ukaHeightCode) {
            dog.extra["uka.preference.heightCode"].clear()
        }
        update(response, dog, dogNode, "idDog", idDog, bindings)
        var needToUpdateClasses = false
        CompetitionDog().join { competition }
            .where("idDog=$idDog AND competition.dateStart>=curdate() - INTERVAL 14 DAY AND competition.idOrganization = $ORGANIZATION_KC") {
                val effectiveGradeCode = dog.kcEffectiveGradeCode(competition.dateStart)
                if (this.kcGradeCode != effectiveGradeCode) needToUpdateClasses = true
            }

        if (needToUpdateClasses) {
            // put grade change on hold
            dog.kcGradeHoldArray.setValue(dog.kcGradeArray)
            dog.kcGradeArray.setValue(kcCurrentGrade)
            dog.post()
            response["control.action"] = "kc_grade_review"
        }
    }

    return response
}


internal fun putAddDog(body: Json): Json {
    return putDog(body)
}

internal fun putAddHandler(body: Json, idAccount: Int): Json {
    val response = createResourceResponse()
    val thisAccount = Account(idAccount)
    val idCompetitor = body["idCompetitor"].asInt
    thisAccount.addHandler(idCompetitor)
    return response
}

internal fun putAccountCompetitor(body: Json, idAccount: Int): Json {
    val response = createResourceResponse()
    val competitor = Competitor()
    val mergeAccount = body["mergeAccount"]
    when (body["formId"].asString) {
        "plaza" -> {
            required_depricated(mergeAccount, "mergeAccount", "email", response, "Email")
            required_depricated(mergeAccount, "mergeAccount", "password", response, "Password")
            val email = mergeAccount["email"].asString
            val password = mergeAccount["password"].asString
            if (response.has("dataErrors")) {
                return error(response, 1, "Please enter details and try again")
            }
            competitor.select("email=${email.quoted} AND password=${password.quoted} AND registrationComplete AND idAccount<>$idAccount")
        }
        "uka" -> {
            required_depricated(mergeAccount, "mergeAccount", "ukaUsername", response, "Username")
            val ukaUsername = mergeAccount["ukaUsername"].asString
            val ukaPassword = mergeAccount["ukaPassword"].asString
            if (response.has("dataErrors")) {
                return error(response, 1, "Please enter details and try again")
            }
            competitor.select(
                "JSON_EXTRACT(extra, \"\$.uka.userName\")=${ukaUsername.quoted} AND JSON_EXTRACT(extra, \"\$.uka.Password\")=${ukaPassword.quoted} AND idAccount<>$idAccount",
                "ukaMembershipExpires"
            )
        }
        "new" -> {
            return putCompetitor(body, 0)
        }
        else -> {
            response["control.action"] = "error"
            response["control.message"] = "ERROR: No option given in putAddCompetitor"
            return response
        }
    }

    if (competitor.rowCount == 0) {
        return error(response, 1, "No account found with these logon details, try again")
    }

    while (competitor.next()) {
        competitor.addToAccount(idAccount)
    }
    return response
}


internal fun putCompetitor(body: Json, idCompetitor: Int): Json {
    val response = createResourceResponse()
    val data = body["competitor"]
    val unVerifiedEmail: String
    val admin = body["user.access"].asInt > 0

    required_depricated(data, "competitor", "givenName", response, "Given name")
    required_depricated(data, "competitor", "familyName", response, "Family name")
    if (!admin) required_depricated(data, "competitor", "phoneMobile", response, "Mobile phone number")
    if (body["formId"].asString.eq("register")) {
        required_depricated(data, "competitor", "streetAddress", response, "Street Address")
        required_depricated(data, "competitor", "town", response, "Town")
        required_depricated(data, "competitor", "postcode", response, "Postcode")
    } else {
        if (!admin) {
            required_depricated(data, "competitor", "email", response, "Email address")
        }
    }

    data["dateOfBirth"] = data["dateOfBirth"].asDate


    if (response.has("dataErrors")) {
        return error(response, 1, "The information you supplied is incomplete")
    }

    val competitor = Competitor(idCompetitor)
    if (!admin && competitor.found() && data["email"].asString != competitor.email) {
        unVerifiedEmail = data["email"].asString
        competitor.unVerifiedEmail = unVerifiedEmail
        competitor.post()
        data.drop("email")
        response["control.action"] = "email_changed"
        val host = body["host"].asString
        generateEmailChangeConfirmation(host, "competitor_email_verified", unVerifiedEmail, idCompetitor)
        generateEmailChangeWarning(competitor.email, unVerifiedEmail)
    }

    val bindings = getBindings(body, "competitor")
    update(response, competitor, data, "idCompetitor", idCompetitor, bindings)
    if (body["formId"].asString.eq("register")) {
        competitor.registrationComplete = true
        competitor.post()
        competitor.makeAccount()
        update(response, competitor.account, data, "idAccount", competitor.idAccount, bindings)
    }
    return response
}


internal fun putCompetitorEmail(body: Json, idCompetitor: Int): Json {
    var response = createResourceResponse()

    val email = body["email"].asString
    val host = body["host"].asString
    generateConfirmationEmail(host, "register_uka_email_verified", email, "", "", idCompetitor)
    response["control.data.email"] = email
    return response
}

internal fun generateResetPasswordEmail(host: String, kind: String, email: String, hasAccount: Boolean) {
    val newToken = Json()
    newToken["kind"] = "token.reset_password"
    newToken["email"] = email
    newToken["time"] = now
    val encryptedToken = newToken.toJson().encrypt(keyPhrase)
    val clickHere = "<a href='${host}/$kind?token=${encryptedToken}?'>click here</a>"
    val message = if (hasAccount)
        "<p>You have asked to reset your password. Please $clickHere within the next hour to complete the process.</p>"
    else
        "<p>Someone has requested that we reset the password for an account on <a href='www.agilityplaza.com'>www.agilityplaza.com</a> with your email address. However we do not have " +
                "any such account. If you did not request this then please contact our support team below " +
                "so we can investigate a potential abuse.</p>"
    GoogleMail.sendAgilityPlaza(email, "", "Forgotten Password", message)
}

internal fun generateQuickRegistrationEmail(
    email: String,
    link: String
) {
    val clickHere = "<a href='$link'>click here</a>"
    val message = "<p>Someone has tried to login to the Agility Plaza website using your email address but you " +
            "are not currently registered with us.</p><p>If this was you, and you wish to register, we have created a " +
            "special link to make the process quick and easy. Just $clickHere and follow the instructions.</p>" +
            "<p>If this was not you, then just ignore this email</p>"
    GoogleMail.sendAgilityPlaza(email, "", "Login", message)
}

internal fun generateConfirmationEmail(
    host: String,
    kind: String,
    email: String,
    password: String = "",
    countryCode: String = "",
    idCompetitor: Int = 0
) {
    val newToken = Json()
    newToken["kind"] = "token.email_confirmation"
    newToken["email"] = email
    newToken["password"] = password
    newToken["countryCode"] = countryCode
    newToken["idCompetitor"] = idCompetitor
    val encryptedToken = newToken.toJson().encrypt(keyPhrase)
    val clickHere = "<a href='${host}/$kind?token=${encryptedToken}?'>click here</a>"
    val message = "<p>Thank you for registering with agility plaza. Please $clickHere to complete the process.</p>"
    GoogleMail.sendAgilityPlaza(email, "", "Registration", message)
}

internal fun generateEmailChangeConfirmation(host: String, kind: String, email: String, idCompetitor: Int = 0) {
    val newToken = Json()
    newToken["kind"] = "token.email_changed"
    newToken["email"] = email
    newToken["idCompetitor"] = idCompetitor
    val encryptedToken = newToken.toJson().encrypt(keyPhrase)
    val clickHere = "<a href='${host}/$kind?token=${encryptedToken}?'>click here</a>"
    val message = "<p>Please $clickHere to confirm your new email address.</p>"
    GoogleMail.sendAgilityPlaza(email, "", "Email Change", message)
}

internal fun generateEmailChangeWarning(email: String, newEmail: String) {
    val message =
        "<p>We have been asked to change your email address to ${newEmail.quotedSingle}. If you did not " +
                "request this, please contact us immediately (see below)</p>"
    GoogleMail.sendAgilityPlaza(email, "", "Email Change", message)
}

internal fun putAccountCompetitionCancel(idAccount: Int, idCompetition: Int): Json {
    val response = createResourceResponse()
    val competition = Competition(idCompetition)
    competition.cancelEntry(idAccount)
    return response
}

internal fun putAccountCompetitionCheckout(body: Json, idAccount: Int, idCompetition: Int): Json {
    val response = createResourceResponse()
    val webTransaction = WebTransaction()
    webTransaction.seekEntry(idAccount, idCompetition, true)
    if (webTransaction.isPaper) {
        webTransaction.payment["cheque"] = body["competition.cheque"].asString.poundsToPence()
        webTransaction.payment["cash"] = body["competition.cash"].asString.poundsToPence()
        webTransaction.post()
    }

    WebTransaction.confirm(webTransaction)
    if (body["user.access"].asInt != 0) {
        response["control.action"] = "quit"
    }
    return response
}

internal fun putCompetitorRegister(body: Json): Json {
    val response = createResourceResponse()
    val competitor = Competitor()
    val host = body["host"].asString
    val ukaUserName = body["register.ukaUserName"].asString
    val ukaPassword = body["register.ukaPassword"].asString
    val email = body["register.email"].asString
    val password = body["register.password"].asString
    val countryCode = body["register.countryCode"].asString

    if (ukaUserName.isNotEmpty()) {
        if (ukaUserName.contains("@")) {
            addDataError(response, "register.ukaUserName", "This looks like an email address")
            return error(response, 2, "Invalid user name")
        }
        if (ukaPassword == "universal") {
            competitor.select("JSON_EXTRACT(extra, \"\$.uka.userName\")=${ukaUserName.quoted}", "ukaMembershipExpires")
        } else {
            competitor.select(
                "JSON_EXTRACT(extra, \"\$.uka.userName\")=${ukaUserName.quoted} AND JSON_EXTRACT(extra, \"\$.uka.Password\")=${ukaPassword.quoted}",
                "ukaMembershipExpires"
            )
        }
        if (!competitor.first()) {
            addDataError(response, "register.ukaUserName", "Not recognised")
            addDataError(response, "register.ukaPassword", "Not recognised")
            return error(response, 2, "Username/Password not recognised")
        }
        if (competitor.registrationComplete) {
            return error(response, 3, "You have already registrered - use Login instead")
        }
        val token = Json()
        token["kind"] = "token.idCompetitor"
        token["idCompetitor"] = competitor.id
        response["control.action"] = "register_uka"
        response["control.data.token"] = token.toJson().encrypt(keyPhrase)
    } else {
        if (password.length < 6) {
            addDataError(response, "register.password", "Too short")
            return error(response, 1, "Password must be at least 6 characters long")
        }

        competitor.select("Email=${email.quoted} AND registrationComplete")
        if (competitor.first()) {
            addDataError(response, "register.email", "Already in registered")
            return error(response, 2, "Email already in use - try login (or email support@agilityplaza.com)")
        }

        /*
        competitor.select("Email=${email.quoted} AND password=${password.quoted}", "IF(accountStatus=4, -1, accountStatus) DESC")
        if (competitor.first()) {
            addDataError(response, "register.email", "Existing UKA account")
            return error(response, 2, "You have a UKA account - Use Options 1 with username ${competitor.userName.quoted}")
        }
        */

        response["control.action"] = "register_new"
        response["control.data.email"] = email
        generateConfirmationEmail(host, "register_new_email_verified", email, password, countryCode)
    }
    return response
}

internal fun getLedgerAccountOverview(request: ApiRequest): Json {
    val idCompetition = request.query["idCompetition"].asInt
    val response = createResponse(request)

    if (idCompetition > 0) {
        loadFromTable(response["data.competition"], Competition(idCompetition), request)
    }

    DbQuery(
        """
        SELECT
            ledgerAccount.*, debit, credit
        FROM
            ledgerAccount
                LEFT JOIN
            (SELECT
                credit AS idLedgerAccount, SUM(amount) AS credit
            FROM
                ledger
            WHERE
                ledger.amount>0 ${if (idCompetition > 0) "AND ledger.idCompetition=$idCompetition" else ""}
            GROUP BY credit) AS c ON c.idLedgerAccount = ledgerAccount.idLedgerAccount
                LEFT JOIN
            (SELECT
                debit AS idLedgerAccount, SUM(amount) AS debit
            FROM
                ledger
            WHERE
                ledger.amount>0 ${if (idCompetition > 0) "AND ledger.idCompetition=$idCompetition" else ""}
            GROUP BY debit) AS d ON d.idLedgerAccount = ledgerAccount.idLedgerAccount
    """
    ).forEach {
        val node = if (it.getInt("idLedgerAccount") < ACCOUNT_UKA_MEMBER)
            response["data.ledgerAccounts.plaza"].addElement()
        else
            response["data.ledgerAccounts.uka"].addElement()

        node["idLedgerAccount"] = it.getInt("idLedgerAccount")
        node["textCode"] = it.getString("textCode")
        node["description"] = it.getString("description")
        node["debit"] = it.getInt("debit")
        node["credit"] = it.getInt("credit")
        node["balance"] = it.getInt("debit") - it.getInt("credit")
    }

    return response
}

internal fun getLedgerAccountAccount(request: ApiRequest): Json {
    val response = createResponse(request)
    val idLedgerAccount = request.params["idLedgerAccount"].asInt
    val idCompetition = request.query["idCompetition"].asInt
    if (idCompetition > 0) {
        loadFromTable(response["data.competition"], Competition(idCompetition), request)
    }

    val ledgerAccount = LedgerAccount(idLedgerAccount)
    loadFromTable(response["data.ledgerAccount"], ledgerAccount, request, "*")

    var balance = 0
    val ledger = Ledger()
    val orderBy = if (idLedgerAccount == ACCOUNT_STARLING)
        "idLedger"
    else
        "ledger.dateEffective, ledger.idAccount, ledger.idCompetition, ledger.type, idLedger"
    ledger.join(
        ledger.account,
        ledger.account.competitor,
        ledger.competition,
        ledger.debitAccount,
        ledger.creditAccount
    )
    ledger.selectAccount(
        idLedgerAccount,
        where = "ledger.amount>0 ${if (idCompetition > 0) "AND ledger.idCompetition=$idCompetition" else ""}",
        orderBy = orderBy
    )
    ledger.forEach {
        balance += ledger.debitAmount - ledger.creditAmount
        var info = ""
        if (ledger.account.id > 0) info = info.append(ledger.account.competitor.fullName, "/")
        if (ledger.competition.id > 0) info = info.append(ledger.competition.uniqueName, "/")
        info = info.append(ledger.source, "/")

        val node = response["data.ledgerAccount.items"].addElement()
        node["idLedger"] = ledger.id
        node["date"] = ledger.dateCreated.dateOnly()
        node["description"] = ledger.description + if (info.isNotEmpty()) " ($info)" else ""
        node["debit"] = ledger.debitAmount
        node["credit"] = ledger.creditAmount
        node["balance"] = balance
        node["other"] = if (ledger.isDebit) ledger.creditAccount.textCode else ledger.debitAccount.textCode
    }

    return response
}

internal fun getLedgerUnallocatedReceipts(request: ApiRequest): Json {
    val response = createResponse(request, "unallocatedReceipts")
    Ledger().where("credit=$ACCOUNT_UNKNOWN", "dateCreated") {
        val node = response["data.items"].addElement()
        node["idLedger"] = id
        node["date"] = dateCreated.dateOnly()
        node["source"] = source
        node["amount"] = amount
    }
    return response
}

fun putLedgerUnallocatedReceipts(body: Json): Json {
    val response = createResourceResponse()
    val items = body["items"]
    val idAccount = body["user.idAccount"].asInt

    for (item in items) {
        val confirmed = item["confirmed"].asBoolean
        if (confirmed) {
            Ledger.allocateElectronicReceipt(item["idLedger"].asInt, ACCOUNT_USER, idAccount, 0, wrongReference = true)
        }
    }
    return response
}

object DatabaseInstances {

    val uuidMap = HashMap<String, String>()
    var loadTime = nullDate

    fun loadUuidMap() {
        Device().where("databaseUuid<>''") {
            if (!uuidMap.containsKey(this.databaseUuid)) {
                uuidMap.put(this.databaseUuid, if (this.type == 4) this.serial else this.tag)
            }
        }
    }

    fun getAcu(uuid: String): String {
        var result = uuidMap.getDef(uuid, "")
        if (result.isEmpty() && loadTime.before(now.addMinutes(-5))) {
            loadUuidMap()
            result = uuidMap.getDef(uuid, "")
        }
        return result
    }


}

internal fun getReplication(request: ApiRequest): Json {
    val response = createResponse(request, "replication")
    dbQuery("SHOW SLAVE STATUS") {
        response["data.status"].addElement().loadFromDataset(this, "*")
        for (item in response["data.status"]) {
            item["idAcu"] = item["Master_Host"].asString.substringAfterLast(".").toIntDef(0)
            item["briefName"] = item["Channel_Name"].asString.substringBefore("_acu")
            val sets = item["sets"]
            val retrieved = item["Retrieved_Gtid_Set"].asString.split("\n")
            for (block in retrieved) {
                val uuid = block.substringBefore(":")
                val range = block.substringAfter(":")
                val set = sets.addElement()
                set["acu"] = DatabaseInstances.getAcu(uuid)
                set["retrieved"] = range
            }
            val executed = item["Executed_Gtid_Set"].asString.split("\n")
            for (block in executed) {
                val uuid = block.substringBefore(":")
                val range = block.substringAfter(":")
                val acu = DatabaseInstances.getAcu(uuid)
                val set = sets.searchElement("acu", acu, false)
                if (set.isNotEmpty()) {
                    set["executed"] = range
                }
            }
        }
    }
    ReplicationFault().join { competition }.join { device }.where("true") {
        val node = response["data.faults"].addElement()
        node["competition"] = competition.uniqueName
        node["device"] = device.tag
        loadFromTable(node, this, request)
    }

    return response
}

internal fun getAcu(request: ApiRequest): Json {
    val id = request.params["id"].asInt
    val response = createResponse(request, "replication")
    val acuList = ApiFunctionAcuList()
    acuList.requestHttp("10.8.1.$id", "v1.0")
    response["data.acus"] = acuList.acus
    for (acu in response["data.acus"]) {
        val display = acu["display"].asString.split("|")
        if (display.size == 3) {
            acu["line1"] = display[0]
            acu["line2"] = display[1]
            acu["line3"] = display[2]
        }
        acu["idAcu"] = acu["tag"].asString.replace("acu", "").toIntDef(0)
    }
    return response
}

internal fun getAcuDiagnostics(request: ApiRequest): Json {
    val id = request.params["id"].asInt
    val response = createResponse(request, "replication")
    val diagnostics = ApiFunctionDiagnostics()
    diagnostics.requestHttp("10.8.1.$id", "v1.0")
    response["data.diagnostics"] = diagnostics.data
    return response
}

internal fun getReplicationFix(request: ApiRequest): Json {
    val response = createResponse(request, "replicationFix")

    ReplicationFault.errorCode()

    return response
}

internal fun getCompetitionCruftsTeams(request: ApiRequest): Json {
    val idCompetition = request.params["idCompetition"].asInt
    val check = request.query["check"].asBoolean

    val competition = Competition(idCompetition)

    val response = createResponse(request, "team")
    loadFromTable(response["data.competition"], competition, request)
    Entry().join { account }.join { account.competitor }.join { team }.join { agilityClass }
        .where(
            "agilityClass.idCompetition=$idCompetition AND agilityClass.classCode=${ClassTemplate.KC_CRUFTS_TEAM.code}",
            "json_extract(team.extra, '\$.teamName')"
        ) {
            if (!check || team.clubName.isEmpty()) {
                val node = response["data.teams"].addElement()
                node["idAccount"] = idAccount
                node["accountCode"] = account.code
                node["enteredBy"] = account.competitor.fullName
                node["idTeam"] = team.id
                node["teamType"] = team.type
                node["classCode"] = team.classCode
                node["teamName"] = if (team.teamName.isEmpty()) "*** NO TEAM NAME ***" else team.teamName
                node["clubName"] = if (team.clubName.isEmpty()) "*** NO CLUB NAME ***" else team.clubName
                node["members"] = team.members
                node["height"] = jumpHeightText
            }
        }
    return response
}

internal fun getTeam(request: ApiRequest): Json {
    val idTeam = request.params["idTeam"].asInt
    val team = Team(idTeam)
    val response = createResponse(request, "team")
    response["data.lookup"] = "${QuartzApiServer.uri}/dog/codeInformation/"
    val node = response["data.team"]
    node["idTeam"] = team.id
    node["teamType"] = team.type
    node["classCode"] = team.classCode
    node["teamName"] = team.teamName
    node["clubName"] = team.clubName
    node["members"] = team.members
    return response
}

internal fun putTeam(body: Json): Json {
    var response = createResourceResponse()
    val teamNode = body["team"]
    val bindings = getBindings(body, "team")

    val idTeam = teamNode["idTeam"].asInt

    required(body, "team.teamName", response, "Team Name")
    required(body, "team.clubName", response, "Club Name")

    if (response.has("dataErrors")) {
        return error(response, 1, "The information you supplied is incomplete")
    }

    val team = Team(idTeam)
    dbTransaction {
        team.members.setValue(body["team.members"])
        team.refreshMembers()
        update(response, team, teamNode, "idTeam", idTeam, bindings)
    }

    return response
}
