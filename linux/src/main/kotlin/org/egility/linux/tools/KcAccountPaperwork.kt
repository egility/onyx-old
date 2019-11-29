/*
 * Copyright (c) Mike Brickman 2014-2018
 */

package org.egility.linux.tools

import com.itextpdf.text.Document
import com.itextpdf.text.PageSize
import com.itextpdf.text.pdf.PdfReader
import com.itextpdf.text.pdf.PdfSmartCopy
import org.apache.pdfbox.pdmodel.PDDocument
import org.egility.library.dbobject.*
import org.egility.library.dbobject.LedgerItem
import org.egility.library.general.*
import org.odftoolkit.odfdom.type.Color
import org.odftoolkit.simple.TextDocument
import org.odftoolkit.simple.style.Font
import org.odftoolkit.simple.style.StyleTypeDefinitions
import org.odftoolkit.simple.table.Table
import org.odftoolkit.simple.text.ParagraphContainer
import java.io.File
import java.io.FileOutputStream
import java.util.*
import kotlin.collections.HashMap

/**
 * Created by mbrickman on 16/04/18.
 */
class KcAccountPaperwork(
    val idCompetition: Int,
    val notes: String = "",
    val minColumns: Int = 1
) {

    val condensed = Font("Ubuntu Condensed", StyleTypeDefinitions.FontStyle.REGULAR, 10.0)

    val dog1 = Font("Ubuntu Condensed", StyleTypeDefinitions.FontStyle.REGULAR, 10.0, Color.BLUE)
    val dog2 = Font("Ubuntu Condensed", StyleTypeDefinitions.FontStyle.REGULAR, 10.0, Color.RED)
    val dog3 = Font("Ubuntu Condensed", StyleTypeDefinitions.FontStyle.REGULAR, 10.0, Color.GREEN)
    val dog4 = Font("Ubuntu Condensed", StyleTypeDefinitions.FontStyle.REGULAR, 10.0, Color.ORANGE)
    val dog5 = Font("Ubuntu Condensed", StyleTypeDefinitions.FontStyle.REGULAR, 10.0, Color.PURPLE)
    val dog6 = Font("Ubuntu Condensed", StyleTypeDefinitions.FontStyle.REGULAR, 10.0, Color.NAVY)
    val dog7 = Font("Ubuntu Condensed", StyleTypeDefinitions.FontStyle.REGULAR, 10.0, Color.TEAL)
    val dog8 = Font("Ubuntu Condensed", StyleTypeDefinitions.FontStyle.REGULAR, 10.0, Color.MAROON)

    val condensedBold = Font("Ubuntu Condensed", StyleTypeDefinitions.FontStyle.BOLD, 10.0)
    val condensedBlue = Font("Ubuntu Condensed", StyleTypeDefinitions.FontStyle.REGULAR, 10.0, Color.BLUE)
    val condensedMaroon = Font("Ubuntu Condensed", StyleTypeDefinitions.FontStyle.REGULAR, 10.0, Color.MAROON)
    val condensedMaroonBold = Font("Ubuntu Condensed", StyleTypeDefinitions.FontStyle.BOLD, 10.0, Color.MAROON)
    val courier = Font("Courier New", StyleTypeDefinitions.FontStyle.REGULAR, 10.0)
    val arial10 = Font("Arial", StyleTypeDefinitions.FontStyle.REGULAR, 10.0)
    val arial9Italic = Font("Arial", StyleTypeDefinitions.FontStyle.ITALIC, 9.0)
    val arialBold10 = Font("Arial", StyleTypeDefinitions.FontStyle.BOLD, 10.0)
    val arialHighlight10 = Font("Arial", StyleTypeDefinitions.FontStyle.REGULAR, 10.0)
    val times = Font("Times New Roman", StyleTypeDefinitions.FontStyle.REGULAR, 11.0)
    val timesBold = Font("Times New Roman", StyleTypeDefinitions.FontStyle.BOLD, 11.0)
    val timesBoldItalic = Font("Times New Roman", StyleTypeDefinitions.FontStyle.BOLDITALIC, 11.0)
    val arial11 = Font("Arial", StyleTypeDefinitions.FontStyle.REGULAR, 11.0)
    val arial36Bold = Font("Arial", StyleTypeDefinitions.FontStyle.BOLD, 36.0)
    val arial36 = Font("Arial", StyleTypeDefinitions.FontStyle.REGULAR, 36.0)
    val arial24 = Font("Arial", StyleTypeDefinitions.FontStyle.BOLD, 24.0)
    val arial18 = Font("Arial", StyleTypeDefinitions.FontStyle.REGULAR, 18.0)
    val arial18Bold = Font("Arial", StyleTypeDefinitions.FontStyle.BOLD, 18.0)
    val arial14 = Font("Arial", StyleTypeDefinitions.FontStyle.REGULAR, 14.0)
    val arialBold14 = Font("Arial", StyleTypeDefinitions.FontStyle.BOLD, 14.0)
    val arialBold12 = Font("Arial", StyleTypeDefinitions.FontStyle.BOLD, 12.0)
    val arialBold12Maroon = Font("Arial", StyleTypeDefinitions.FontStyle.BOLD, 12.0, Color.MAROON)
    val arialBold10Maroon = Font("Arial", StyleTypeDefinitions.FontStyle.BOLD, 10.0, Color.MAROON)
    val arial10Maroon = Font("Arial", StyleTypeDefinitions.FontStyle.REGULAR, 10.0, Color.MAROON)
    val regular = Font("Times New Roman", StyleTypeDefinitions.FontStyle.REGULAR, 11.0)

    val headingBackground = "#fcf8cc"
    val yellowBackground = "#fcf8cc"

    val competition = Competition(idCompetition)

    var venue = competition.venueAddress
    var hasCamping: Boolean = false
    var notesPath = if (notes.isNotEmpty()) notes else Global.showDocumentPath(
        competition.uniqueName,
        "Notes",
        "pdf",
        canRegenerate = false
    )
    var splitJob = true
    var markFirst = false
    var addressFont = arial11


    init {
        if (!venue.contains(competition.venuePostcode)) {
            venue += " ${competition.venuePostcode}"
        }
    }

    fun check() {
        val json = buildJson()
        var index = 1
        LedgerItem().join { account }.join { account.competitor }
            .where(
                "ledgerItem.idCompetition=${idCompetition} AND ledgerItem.type IN ($LEDGER_ITEM_POSTAGE, $LEDGER_ITEM_PAPER)",
                "substr(account.postCode, 1, locate(\" \", account.postCode)-1)"
            ) {

                val sequence = "%04d".format(index++)
                println("$sequence ${account.code}")
            }
    }

    fun doAccount(json: Json, account: Account, index: Int, addressAccount: Account, justLetter: Boolean = false) {
        if (account.postcode.isEmpty()) {
            println("No address - ${account.code}: ${account.fullAddress}")
        } else {

            val accountEntries = getAccountEntries(account.id)
            val sequence = "%04d".format(index)
            hasCamping = false

            val entriesFile = Global.showDocumentPath(idCompetition, "paperwork_${sequence}_entries", "odt")
            a4Document(entriesFile, 10.0, 5.0) {
                addAddress(this@a4Document, addressAccount.id)
                if (idCompetition == 1426882570) {
                    addLetter(this@a4Document, account.competitor.givenName.naturalCase)
                }
                addEntries(this@a4Document, account.id, accountEntries)
            }
            val entriesPdf = odtToPdf(entriesFile)

            val hasPermit = competition.parkingPermit || (competition.campingPermit && hasCamping)
            var permitFile = ""
            if (hasPermit) {
                permitFile = Global.showDocumentPath(idCompetition, "paperwork_${sequence}_permits", "odt")
                a4Document(permitFile, 10.0, 5.0) {
                    addJunk(this@a4Document)
                    if (competition.parkingPermit && competition.campingPermit && hasCamping) {
                        addDoublePermit(this@a4Document, account)
                    } else if (competition.campingPermit && hasCamping) {
                        addCampingPermit(this@a4Document, account)
                    } else if (competition.parkingPermit) {
                        addParkingPermit(this@a4Document, account)
                    }
                }
            }
            val permitPdf = if (hasPermit) odtToPdf(permitFile) else ""

            if (justLetter) {
                addBatch(entriesPdf)
                return
            }

            val itinerariesFile = Global.showDocumentPath(idCompetition, "paperwork_${sequence}_itineraries", "odt")
            var first = true
            a4Document(itinerariesFile, 10.0, 5.0) {
                for (competitorNode in accountEntries["competitors"]) {
                    if (!first) addPageBreak()
                    first = false
                    addRingPlan(this@a4Document, json, competitorNode)
                }
            }
            val itinerariesPdf = odtToPdf(itinerariesFile)

            val targetPdf = Global.showDocumentPath(idCompetition, "paperwork_${sequence}", "pdf")
            mergeNotes(entriesPdf, itinerariesPdf, targetPdf, permitPdf)
            if (permitPdf.isNotEmpty()) File(permitPdf).delete()
            File(entriesPdf).delete()
            File(itinerariesPdf).delete()
            addBatch(targetPdf)
        }
    }

    fun doFix(json: Json, account: Account, index: Int, addressAccount: Account) {
        if (account.postcode.isEmpty()) {
            println("No address - ${account.code}: ${account.fullAddress}")
        } else {
            val accountEntries = getAccountEntries(account.id)
            val sequence = "%04d".format(index)
            val itinerariesFile = Global.showDocumentPath(idCompetition, "paperwork_${sequence}_itineraries", "odt")
            a4Document(itinerariesFile, 10.0, 5.0) {
                for (competitorNode in accountEntries["competitors"]) {
                    addRingPlan(this@a4Document, json, competitorNode, noBlank = true)
                }
            }
            val itinerariesPdf = odtToPdf(itinerariesFile)
            addBatch(itinerariesPdf)
        }
    }

    fun export(idAccount: Int = 0, notes: String = "", markFirst: Boolean = true, all: Boolean = false): String {
        mandate(competition.processed, "Show not processed")
        val outputFile = Global.showDocumentPath(idCompetition, "Mailout", "pdf")
        File(outputFile).delete()

        //setDebugExcludeClasses("*")
        if (notes.isNotEmpty()) notesPath = notes
        splitJob = false
        this.markFirst = markFirst
        val json = buildJson()
        var index = 1

        var idAccountList = ""
        var idAccountExcept = "-1"
        if (idAccount > 0) {
            idAccountList = idAccount.toString()
        }
        if (all) {
            dbQuery("SELECT idAccount FROM ledger WHERE idCompetition=${idCompetition} AND type IN ($LEDGER_ENTRY_FEES, $LEDGER_ENTRY_FEES_PAPER)") {
                idAccountList = idAccountList.append(getString("idAccount"))
            }
            /*
            dbQuery("SELECT idAccount FROM ledgerItem WHERE idCompetition=${idCompetition} AND type IN ($LEDGER_ITEM_POSTAGE, $LEDGER_ITEM_PAPER)") {
                idAccountExcept = idAccountList.append(getString("idAccount"))
            }
            */
        } else {
            dbQuery("SELECT idAccount FROM ledgerItem WHERE idCompetition=${idCompetition} AND type IN ($LEDGER_ITEM_POSTAGE, $LEDGER_ITEM_PAPER)") {
                idAccountList = idAccountList.append(getString("idAccount"))
            }
        }

        Account().join { competitor }
            .where("account.idAccount IN ($idAccountList) AND NOT account.idAccount IN ($idAccountExcept)", "substr(postCode, 1, locate(\" \", postCode)-1)") {
                if (markFirst && index == 1 && idAccount == 0) {
                    doAccount(json, this, index++, Account(1988295795))
                }
                doAccount(json, this, index++, this)
            }
        return outputFile
    }

    fun exportItineraries(): String {
        val json = buildJson()
        var idAccountList = ""
        dbQuery("SELECT idAccount FROM ledgerItem WHERE idCompetition=${idCompetition} AND type IN ($LEDGER_ITEM_POSTAGE, $LEDGER_ITEM_PAPER)") {
            idAccountList = idAccountList.append(getString("idAccount"), ",")
        }

        val itinerariesFile = Global.showDocumentPath(idCompetition, "paperwork_itineraries", "odt")
        var first = true
        val accountEntries = getAccountAllEntries(idAccountList)
        a4Document(itinerariesFile, 10.0, 5.0) {
            for (competitorNode in accountEntries["competitors"]) {
                if (!first) addPageBreak()
                first = false
                addRingPlan(this@a4Document, json, competitorNode)
            }
        }
        val itinerariesPdf = odtToPdf(itinerariesFile)
        return itinerariesPdf
    }

    fun fix(idAccount: Int = 0) {
        if (notes.isNotEmpty()) notesPath = notes
        splitJob = false
        markFirst = true
        val json = buildJson()
        var index = 1
        var whereClause =
            "ledgerItem.idCompetition=${idCompetition} AND ledgerItem.type IN ($LEDGER_ITEM_POSTAGE, $LEDGER_ITEM_PAPER)"
        if (idAccount > 0) whereClause += " AND ledgerItem.idAccount=$idAccount"
        LedgerItem().join { account }.join { account.competitor }
            .where(whereClause, "substr(account.postCode, 1, locate(\" \", account.postCode)-1)") {
                doFix(json, account, index++, account)
            }
    }

    fun exportPersonal(idAccount: Int, baseFile: String = "paperwork_personal_$idAccount"): String {
        val json = buildJson()
        val accountEntries = getAccountEntries(idAccount)

        val addressFile = Global.showDocumentPath(idCompetition, baseFile, "odt")
        a4Document(addressFile, 10.0, 5.0) {
            addPageHeader(this)
            p("").marginBottom("0.5cm")
            addEntries(this@a4Document, idAccount, accountEntries, personal = true)
            if (competition.importantNote.isNotEmpty()) {
                p("").marginBottom("0.25cm")
                addImportantNote(this, idAccount)
            }
            if (competition.hasCamping) {
                p("").marginBottom("0.25cm")
                addCamping(this, idAccount)
            }
            if (competition.parkingPermit && competition.campingPermit && hasCamping) {
                addJunk(this)
                addPageBreak()
                addPermit(this, "Parking Permit", Account(idAccount), false)
                addPermit(this, "Camping Permit", Account(idAccount), true)
            } else if (competition.campingPermit && hasCamping) {
                addJunk(this)
                addPageBreak()
                addPermit(this, "Camping Permit", Account(idAccount), true)
            } else if (competition.parkingPermit) {
                addJunk(this)
                addPageBreak()
                addPermit(this, "Parking Permit", Account(idAccount), false)
            }

            for (competitorNode in accountEntries["competitors"]) {
                addPageBreak()
                addRingPlan(this@a4Document, json, competitorNode, evenPages = false)
            }
        }
        val pdf = odtToPdf(addressFile)
        appendNotes(pdf)
        return pdf
    }

    fun ringPlan(): String {
        val json = buildJson()
        val addressFile = Global.showDocumentPath(idCompetition, "generated_ring_plan", "odt")
        a4Document(addressFile, 10.0, 5.0) {
            addRingPlan(this@a4Document, json, Json.nullNode(), evenPages = false)
        }
        val pdf = odtToPdf(addressFile)
        return pdf
    }

    fun ringBoards(): String {
        val json = buildJson()
        val addressFile = Global.showDocumentPath(idCompetition, "ring_boards", "odt")
        a4Document(addressFile, 10.0, 5.0) {
            addRingBoards(this@a4Document, json, Json.nullNode())
        }
        val pdf = odtToPdf(addressFile)
        return pdf
    }


    fun getAccountEntries(idAccount: Int): Json {
        val json = Json()
        val competitor = ChangeMonitor<Int>(-1)
        var competitorNode = Json.nullNode()
        var entries = 0
        Entry().join { team }.join { agilityClass }.join { team.dog }.join { team.competitor }.where(
            "agilityClass.idCompetition=$idCompetition AND entry.idAccount=$idAccount AND entry.progress<$PROGRESS_REMOVED",
            "team.idCompetitor, agilityClass.classNumber, agilityClass.classNumberSuffix, agilityClass.classDate, agilityClass.classCode, entry.runningOrder"
        ) {
            if (!agilityClass.template.hasChildren && !agilityClass.template.isCut) {
                if (team.idCompetitor == 0 && team.idDog != 0 && competitor.hasChanged(team.dog.idCompetitorHandler)) {
                    competitorNode = json["competitors"].addElement()
                    competitorNode["idCompetitor"] = team.dog.idCompetitorHandler
                    competitorNode["name"] = team.dog.handlerName
                } else if (competitor.hasChanged(team.idCompetitor)) {
                    competitorNode = json["competitors"].addElement()
                    competitorNode["idCompetitor"] = team.idCompetitor
                    competitorNode["name"] = team.competitor.fullName
                }
                var dateFound = false
                for (date in competitorNode["dates"]) if (date.asDate == agilityClass.date) dateFound = true
                if (!dateFound) competitorNode["dates"].addElement().setValue(agilityClass.date)

                val classNode =
                    competitorNode["classes"].searchElement("idAgilityClass", agilityClass.id, create = true)
                val node = classNode["entries"].addElement()
                node["code"] = team.dog.code
                node["teamSize"] = team.memberCount
                node["petName"] = if (team.memberCount > 1) team.description else team.dog.cleanedPetName
                node["runningOrder"] = runningOrder
                node["heightText"] = Height.getHeightName(heightCode)

                val dogNode = json["dogs"].searchElement("code", team.dog.code, create = true)
                node["index"] = json["dogs"].indexOf(dogNode)
                val dogEntryNode = dogNode["entries"].addElement()
                dogEntryNode["classNumber"] = agilityClass.number
                dogEntryNode["classNumberSuffix"] = agilityClass.numberSuffix
                dogEntryNode["classDate"] = agilityClass.date
                dogEntryNode["className"] = agilityClass.name
                dogEntryNode["classNameLong"] = agilityClass.nameLong
                dogEntryNode["ringNumber"] = agilityClass.ringNumber
                dogEntryNode["runningOrder"] = runningOrder
                dogEntryNode["heightText"] = Height.getHeightName(heightCode)
                entries++
            }
        }
        for (dogNode in json["dogs"]) {
            dogNode["entries"].sortBy("classNumber", "classNumberSuffix")
        }
        json["totalEntries"] = entries
        return json
    }

    fun getAccountAllEntries(idAccountList: String): Json {
        val json = Json()
        val competitor = ChangeMonitor<Int>(-1)
        var competitorNode = Json.nullNode()
        var entries = 0
        Entry().join { team }.join { agilityClass }.join { team.dog }.join { team.competitor }.where(
            "agilityClass.idCompetition=$idCompetition AND entry.idAccount IN ($idAccountList) AND entry.progress<$PROGRESS_REMOVED",
            "givenName, familyName, agilityClass.classNumber, agilityClass.classNumberSuffix, agilityClass.classDate, agilityClass.classCode, entry.runningOrder"
        ) {
            if (!agilityClass.template.hasChildren && !agilityClass.template.isCut) {
                if (team.idCompetitor == 0 && team.idDog != 0 && competitor.hasChanged(team.dog.idCompetitorHandler)) {
                    competitorNode = json["competitors"].addElement()
                    competitorNode["idCompetitor"] = team.dog.idCompetitorHandler
                    competitorNode["name"] = team.dog.handlerName
                } else if (competitor.hasChanged(team.idCompetitor)) {
                    competitorNode = json["competitors"].addElement()
                    competitorNode["idCompetitor"] = team.idCompetitor
                    competitorNode["name"] = team.competitor.fullName
                }
                var dateFound = false
                for (date in competitorNode["dates"]) if (date.asDate == agilityClass.date) dateFound = true
                if (!dateFound) competitorNode["dates"].addElement().setValue(agilityClass.date)

                val classNode =
                    competitorNode["classes"].searchElement("idAgilityClass", agilityClass.id, create = true)
                val node = classNode["entries"].addElement()
                node["code"] = team.dog.code
                node["teamSize"] = team.memberCount
                node["petName"] = if (team.memberCount > 1) team.description else team.dog.cleanedPetName
                node["runningOrder"] = runningOrder
                node["heightText"] = Height.getHeightName(heightCode)

                val dogNode = json["dogs"].searchElement("code", team.dog.code, create = true)
                node["index"] = json["dogs"].indexOf(dogNode)
                val dogEntryNode = dogNode["entries"].addElement()
                dogEntryNode["classNumber"] = agilityClass.number
                dogEntryNode["classNumberSuffix"] = agilityClass.numberSuffix
                dogEntryNode["classDate"] = agilityClass.date
                dogEntryNode["className"] = agilityClass.name
                dogEntryNode["classNameLong"] = agilityClass.nameLong
                dogEntryNode["ringNumber"] = agilityClass.ringNumber
                dogEntryNode["runningOrder"] = runningOrder
                dogEntryNode["heightText"] = Height.getHeightName(heightCode)
                entries++
            }
        }
        for (dogNode in json["dogs"]) {
            dogNode["entries"].sortBy("classNumber", "classNumberSuffix")
        }
        json["totalEntries"] = entries
        return json
    }

    fun buildJson(): Json {
        val dateMonitor = ChangeMonitor<Date>(nullDate)
        val ringMonitor = ChangeMonitor<Int>(-1)
        val classMonitor = ChangeMonitor<Int>(-1)

        val json = Json()

        var dateNode = Json.nullNode()
        var ringNode = Json.nullNode()
        var classNode = Json.nullNode()

        dbQuery(
            """
            SELECT
                ring.judge,
                ring.ringManager,
                ring.helpers,
                ring.note,
                agilityClass.classDate,
                agilityClass.idAgilityClass,
                agilityClass.ringNumber,
                agilityClass.judge AS classJudge,
                agilityClass.className,
                agilityClass.classNameLong,
                agilityClass.heightRunningOrder,
                agilityClass.classCode,
                entry.jumpHeightCode,
                SUM(IF(entry.idEntry IS NULL OR entry.progress>=$PROGRESS_DELETED_LOW, 0, 1)) AS runCount
            FROM
                agilityClass
                    LEFT JOIN
                entry USING (idAgilityClass)
                    LEFT JOIN
                ring ON ring.date = agilityClass.classDate
                    AND ring.ringNumber = agilityClass.ringNumber
                    AND ring.idCompetition = agilityClass.idCompetition
            WHERE
                agilityClass.idCompetition = $idCompetition
            GROUP BY agilityClass.classDate , agilityClass.ringNumber , agilityClass.ringOrder, entry.jumpHeightCode
        """
        ) {
            val classDate = getDate("classDate")
            val ringNumber = getInt("ringNumber")
            val idAgilityClass = getInt("idAgilityClass")
            val judge = getString("judge")
            val classJudge = getString("classJudge")
            val ringManager = getString("ringManager")
            val helpers = getString("helpers")
            val note = getString("note")
            val className = getString("className")
            val classNameLong = getString("classNameLong")
            val heightRunningOrder = getString("heightRunningOrder")
            val jumpHeightCode = getString("jumpHeightCode")
            val runCount = getInt("runCount")
            val classCode = getInt("classCode")
            val template = ClassTemplate.select(classCode)
            if (!template.hasChildren) {
                dateMonitor.whenChange(classDate) {
                    dateNode = json["dates"].addElement()
                    dateNode["date"] = classDate
                    ringMonitor.value = -1
                }
                ringMonitor.whenChange(ringNumber) {
                    ringNode = dateNode["rings"].addElement()
                    ringNode["ringNumber"] = ringNumber
                    ringNode["judge"] = judge
                    ringNode["ringManager"] = ringManager
                    ringNode["helpers"] = helpers
                    ringNode["note"] = note
                }
                classMonitor.whenChange(idAgilityClass) {
                    classNode = ringNode["classes"].addElement()
                    classNode["idAgilityClass"] = idAgilityClass
                    classNode["judge"] = classJudge
                    classNode["ringManager"] = ringManager
                    classNode["className"] = className
                    classNode["classNameLong"] = classNameLong
                    classNode["runCount"] = 0
                    classNode["classCode"] = classCode
                    for (height in heightRunningOrder.split(",")) {
                        val heightNode = classNode["heights"].addElement()
                        heightNode["height"] = height
                        heightNode["runCount"] = 0
                    }
                }
                if (runCount > 0) {
                    val heightNode = classNode["heights"].searchElement("height", jumpHeightCode)
                    heightNode["runCount"] = runCount
                    classNode["runCount"] = classNode["runCount"].asInt + runCount
                }
            }
        }
        return json
    }


    fun addPageHeader(document: TextDocument) {
        with(document) {
            p(arialBold14, competition.name).alignCenter().marginLeft("2.0cm").marginRight("2.0cm")
            p(arialBold12, venue).alignCenter().marginLeft("2.0cm").marginRight("2.0cm")
        }
    }

    fun addRingPlan(
        document: TextDocument,
        json: Json,
        competitorNode: JsonNode,
        lowerHeight: Boolean = true,
        evenPages: Boolean = true,
        noBlank: Boolean = false
    ) {
        var first = true
        var pages = 0
        val itinerary = competitorNode.isNotEmpty()
        with(document) {
            for (date in json["dates"]) {
                var include = !itinerary
                var dayHasLowerHeight = false
                if (competitorNode["dates"].size > 0) {
                    for (competitorDate in competitorNode["dates"]) {
                        if (competitorDate.asDate == date["date"].asDate) include = true
                    }
                }
                if (include) {
                    pages++
                    if (first) {
                        first = false
                    } else if (!noBlank) {
                        addPageBreak()
                    }
                    addPageHeader(this)
                    if (itinerary) {
                        if (idCompetition == 1386491487) {
                            p(
                                arialBold12Maroon,
                                "${competitorNode["name"].asString} - REVISED Personal Itinerary (${date["date"].asDate.fullDate()})"
                            ).marginBottom("0.5cm").marginTop("0.5cm")
                        } else {
                            p(
                                arialBold12Maroon,
                                "${competitorNode["name"].asString} - Personal Itinerary (${date["date"].asDate.fullDate()})"
                            ).marginBottom("0.5cm").marginTop("0.5cm")
                        }
                    } else {
                        p(arialBold12, "Ring Plan ${date["date"].asDate.fullDate()}").marginBottom("0.5cm")
                            .marginTop("0.5cm")
                    }
                    val max2Columns = if (itinerary) 7 else 7
                    val columns = when (date["rings"].size) {
                        in 1..max2Columns -> maxOf(2, minColumns)
                        else -> maxOf(3, minColumns)
                    }

                    section(columns) {
                        for (ring in date["rings"]) {
                            var totalRuns = 0
                            var totalColumn = 1
                            val ringNumber = ring["ringNumber"].asInt
                            val judge = ring["judge"].asString
                            val note = ring["note"].asString
                            var hasLowerHeight = false
                            for (agilityClass in ring["classes"]) {
                                if (agilityClass["heights"].size == 2) {
                                    for (height in agilityClass["heights"]) {
                                        if (height["height"].asString.endsWith("L")) hasLowerHeight = true
                                        if (hasLowerHeight) dayHasLowerHeight = true
                                    }
                                }
                            }

                            p(arial11) {
                                marginBottom("0.2cm")
                                keepWithNext()
                                span { if (judge.isEmpty()) "Ring $ringNumber" else "Ring $ringNumber - $judge" }
                            }.alignCenter()
                            if (note.isNotEmpty()) {
                                p(condensedBlue) {
                                    marginBottom("0.2cm")
                                    keepWithNext()
                                    span { note }
                                }.alignCenter()
                            }

                            table(if (lowerHeight && hasLowerHeight && !competition.lhoMixed) 4 else 2, ring["classes"].size + 2) {
                                marginBottom("0.5cm")
                                keepTogether()
                                if (lowerHeight && hasLowerHeight && !competition.lhoMixed) {
                                    when (columns) {
                                        3 -> widths(null, 26.0, 26.0, 30.0)
                                        else -> widths(null, 18.0, 18.0, 19.0)
                                    }

                                    cell(0, 0, condensedBold, "Class")
                                    if (competition.lhoFirst) {
                                        cellRight(1, 0, condensedBold, "LHO")
                                        cellRight(2, 0, condensedBold, "FH")
                                    } else {
                                        cellRight(1, 0, condensedBold, "FH")
                                        cellRight(2, 0, condensedBold, "LHO")
                                    }
                                    cellRight(3, 0, condensedBold, "Total")
                                } else {
                                    when (columns) {
                                        3 -> widths(null, 30.0)
                                        else -> widths(null, 19.0)
                                    }
                                    cell(0, 0, condensedBold, "Class")
                                    cellRight(1, 0, condensedBold, "Total")
                                }
                                var row = 1
                                for (agilityClass in ring["classes"]) {
                                    val idAgilityClass = agilityClass["idAgilityClass"].asInt
                                    val classNode =
                                        competitorNode["classes"].searchElement("idAgilityClass", idAgilityClass, create = false)
                                    val classJudge = agilityClass["judge"].asString
                                    val classEntries = classNode["entries"]
                                    var className =
                                        if (columns == 2) agilityClass["className"].asString else agilityClass["className"].asString
                                    var classNote =
                                        if (idCompetition == 1426882570 && className.startsWith("33b")) "To be run concurrently with class 33a" else ""
                                    var heightNote = ""
                                    if (classJudge neq judge) className += " (${classJudge.initials})"
                                    val heights = agilityClass["heights"]
                                    if (competition.lhoMixed && heights.size == 2 && agilityClass["runCount"].asInt > 0) {
                                        heightNote = ""
                                        for (height in heights) {
                                            heightNote =
                                                heightNote.append("${Height.getHeightJumpName(height["height"].asString)} (${height["runCount"].asInt})")
                                        }
                                    } else if (heights.size > 2 && agilityClass["runCount"].asInt > 0) {
                                        classNote = ""
                                        for (height in heights) {
                                            classNote =
                                                classNote.append("${Height.getHeightName(height["height"].asString)} (${height["runCount"].asInt})")
                                        }
                                    }

                                    cell(0, row) {
                                        p(condensed) {
                                            span { className }
                                            if (heightNote.isNotEmpty()) {
                                                span(condensedMaroon) { " $heightNote" }
                                            }

                                        }
                                        if (classNote.isNotEmpty()) {
                                            if (competition.isFab) {
                                                p(condensed, classNote)
                                            } else {
                                                p(condensedMaroon, classNote)
                                            }

                                        }
                                    }

                                    if (lowerHeight && hasLowerHeight && heights.size == 2 && !competition.lhoMixed) {
                                        cellRight(1, row, condensed, heights[0]["runCount"].asString)
                                        cellRight(2, row, condensed, heights[1]["runCount"].asString)
                                        if (agilityClass["runCount"].asInt > 0) cellRight(columnCount - 1, row++, condensed, agilityClass["runCount"].asString)
                                    } else {
                                        if (columnCount == 4) {
                                            cell(0, row) { mergeRight(2) }
                                        }

                                        if (agilityClass["runCount"].asInt > 0 || competition.isFab) {
                                            cellRight(columnCount - 1, row, condensed, agilityClass["runCount"].asString)
                                        }
                                        row++
                                    }
                                    totalRuns += agilityClass["runCount"].asInt
                                    if (classEntries.isNotEmpty()) {
                                        appendRow()
                                        row(row - 1).backgroundColor(yellowBackground)
                                        cell(0, row) {
                                            mergeRight(if (lowerHeight && hasLowerHeight && !competition.lhoMixed) 3 else 1)
                                            var text = ""
                                            p {
                                                for (classEntry in classEntries) {
                                                    text = if (competition.isFab)
                                                        "${classEntry["code"].asString} ${classEntry["petName"].asString} (${classEntry["heightText"].asString}/${classEntry["runningOrder"].asInt})"
                                                    else
                                                        "${classEntry["code"].asString} ${classEntry["petName"].asString} (${classEntry["runningOrder"].asInt})"

                                                    if (classEntries.indexOf(classEntry) < classEntries.size - 1) text += ", "
                                                    when (classEntry["index"].asInt) {
                                                        0 -> span(dog1) { text }
                                                        1 -> span(dog2) { text }
                                                        2 -> span(dog3) { text }
                                                        3 -> span(dog4) { text }
                                                        4 -> span(dog5) { text }
                                                        5 -> span(dog6) { text }
                                                        6 -> span(dog7) { text }
                                                        7 -> span(dog8) { text }
                                                        else -> span(dog1) { text }
                                                    }
                                                }
                                            }
                                        }
                                        row++
                                    }

                                }
                                if (totalRuns > 0) {
                                    if (columnCount == 4) {
                                        cell(0, row) { mergeRight(2) }
                                    }
                                    cell(0, row, condensed, "TOTAL")
                                    cellRight(columnCount - 1, row, condensedBold, totalRuns.toString())
                                }
                            }

                        }
                    }
                    if (competition.isFab) {
                        section(1) {
                            p(condensed) {
                                span { "Heights will run in the order show under the class name. Running orders are specific to each height." }
                            }.marginTop("0.5cm")
                        }
                    } else {
                        section(1) {
                            if (competition.itineraryNote.isNotEmpty()) {
                                p(condensedBlue) {
                                    span { competition.itineraryNote }
                                }
                            }
                            if (lowerHeight && dayHasLowerHeight) {
                                p(condensed) {
                                    span { "(*) LHO class with combined results, (#) LHO class with separate results." }
                                    if (competition.lhoMixed) {
                                        span(condensedMaroon) { " Height options will run in the order shown next to the class name." }
                                    } else if (competition.lhoFirst) {
                                        span { "LHO will run BEFORE full height." }
                                    } else {
                                        span { "LHO will run AFTER full height." }
                                    }
                                    span { " Running orders do not necessarily run consecutively so it is quite normal to have a number higher than the class size." }
                                }.marginTop("0.5cm")
                            } else {
                                p(condensed) {
                                    span { "Running orders do not necessarily run consecutively so it is quite normal to have a number higher than the class size." }
                                }.marginTop("0.5cm")
                            }
                        }
                    }
                }
            }
            //addPageBreak()
            if (!noBlank && evenPages && pages.rem(2) == 1) {
                addPageBreak()
            }
        }

    }


    fun addAddress(textDocument: TextDocument, idAccount: Int) {
        var accountCode = ""
        with(textDocument) {
            p {
                if (markFirst) {
                    textBox("7.0cm", "0.5cm", "9.0cm", "0.5cm") {
                        p("***!!!***(${competition.uniqueName})")
                    }
                }
                imageBox(
                    "1.0cm",
                    "1.0cm",
                    "6.0cm",
                    "1.896cm",
                    Global.imagesFolder + "/logo.png",
                    Global.imagesFolder + "/full_logo.svg"
                )

                textBox("9.5cm", "1.5cm", "9.5", "2.5cm") {
                    p(arialBold14, competition.name).alignCenter()
                    p(arial10, venue).alignCenter()
                }

                textBox("1.79cm", "4.18cm", "9.96cm", "4.54cm") {
                    Account().join { competitor }.seek(idAccount) {
                        val lines = streetAddress.replace("\r", ",").split(",")
                        val line1 = lines.getOrElse(0) { "" }.trim()
                        val line2 = lines.getOrElse(1) { "" }.trim()
                        val line3 = lines.drop(2).asCommaList().trim()
                        p("")
                        p("")
                        p(addressFont, competitor.fullName).marginLeft("1.0cm")
                        p(addressFont, line1).marginLeft("1.0cm")
                        if (line2.isNotEmpty()) p(addressFont, line2).marginLeft("1.0cm")
                        if (line3.isNotEmpty()) p(addressFont, line3).marginLeft("1.0cm")
                        p(addressFont, town).marginLeft("1.0cm")
                        p(addressFont, postcode).marginLeft("1.0cm")
                        accountCode = code
                    }
                }
                textBox("14.0cm", "5.0cm", "6.0cm", "4.0cm") {
                    p(arial11, "a/c $accountCode").marginBottom("0.5cm")
                    if (competition.importantNote.isNotEmpty()) {
                        p("").marginBottom("0.25cm")
                        addImportantNote(this, idAccount)
                    }
                    if (competition.hasCamping) {
                        addCamping(this, idAccount)
                    }
                }
            }.alignCenter().marginBottom("9.3cm")
        }
    }

    fun addImportantNote(paragraphContainer: ParagraphContainer, idAccount: Int) {
        with(paragraphContainer) {
            p(arialBold12Maroon, "Important Note").marginBottom("0.2cm")
            p(arial10Maroon, competition.importantNote)
        }
    }

    fun addCamping(paragraphContainer: ParagraphContainer, idAccount: Int) {
        with(paragraphContainer) {
            p(arialBold12, "Camping").marginBottom("0.2cm")
            Camping().seek("idCompetition = $idCompetition AND idAccount = $idAccount AND NOT rejected") {
                hasCamping = true
                p(arial10, bookingText())
            }.otherwise {
                p(arial10, "Camping not booked.")
            }

        }
    }

    fun addLetter(textDocument: TextDocument, name: String) {
        with(textDocument) {
            p(arial11, "Dear $name,").marginBottom("0.25cm")

            p(
                arial11,
                "Unfortunately our print partners had a technical issue with our previous mail out for Chippenham and " +
                        "the wrong information was printed on the back of each page. This problem has now been " +
                        "solved so we are re-mailing you with the correct information. Please dispose of " +
                        "your previous documents and use these instead."
            )

                .marginBottom("0.25cm")
            p(arial11, "Sorry for the inconvenience.").marginBottom("0.25cm")
            p(arial11, "Regards,").marginBottom("0.25cm")
            p(arial11, "Mike Brickman")
            p(arial11, "Agility Plaza").marginBottom("1.0cm")

        }
    }

    fun addJunk(textDocument: TextDocument) {
        with(textDocument) {
            section(2) {
                for (i in 1..32) {
                    p("")
                }
            }
        }

    }

    fun addEntries(textDocument: TextDocument, idAccount: Int, accountEntries: JsonNode, personal: Boolean = false) {
        with(textDocument) {
            p(arialBold12, "Entries").marginBottom("0.2cm")
            val competitionDog = CompetitionDog().join(competition).join { dog }.select(
                "competitionDog.idCompetition=$idCompetition AND competitionDog.idAccount=$idAccount AND NOT nfc",
                "dog.petName"
            )

            var regularFont = arial10
            var boldFont = arialBold10
            var columns = 1
            var width = 12.0

            if (!personal && accountEntries["totalEntries"].asInt > 0) {
                regularFont = condensed
                boldFont = condensedBold
                columns = 2
                width = 24.0
            }

            section(columns) {
                competitionDog.withEach {
                    val date = ChangeMonitor<Date>(nullDate)
                    if (competition.isFab) {
                        val collieText = if (fabCollie) "Collie/Collie X" else "ABC"
                        p(
                            boldFont,
                            "${dog.code} ${dog.cleanedPetName} ($collieText)"
                        ).keepWithNext()
                    } else if (dog._petName.isEmpty() || dog._petName.eq(dog.registeredName)) {
                        p(
                            boldFont,
                            "${dog.code} ${dog.registeredName} ${options(competition.idOrganization, " / ")}"
                        ).keepWithNext()
                    } else {
                        val registeredText = if (dog.registeredName.isEmpty()) "" else " (${dog.registeredName})"
                        p(
                            boldFont,
                            "${dog.code} ${dog.cleanedPetName}$registeredText ${options(competition.idOrganization, " / ")}"
                        ).keepWithNext()
                    }
                    val dogNode = accountEntries["dogs"].searchElement("code", dog.code)
                    table(if (competition.isFab) 5 else 4, dogNode["entries"].size + 1) {
                        if (competition.isFab) {
                            widths(30.0, null, width, width + 6.0, width)
                        } else {
                            widths(30.0, null, width, width)
                        }
                        marginBottom("0.2cm")
                        keepTogether()
                        cell(0, 0, boldFont, "Date")
                        cell(1, 0, boldFont, "Class")
                        cell(2, 0, boldFont, "Ring")
                        if (competition.isFab) {
                            cell(3, 0, boldFont, "Height")
                            cell(4, 0, boldFont, "R/O")
                        } else {
                            cell(3, 0, boldFont, "R/O")
                        }
                        var row = 1
                        for (dogEntry in dogNode["entries"]) {
                            if (date.hasChanged(dogEntry["classDate"].asDate)) {
                                cell(0, row, regularFont, dogEntry["classDate"].asDate.format("EEE, d"))
                            }
                            cell(1, row, regularFont, dogEntry["className"].asString)
                            cell(2, row, regularFont, dogEntry["ringNumber"].asString)
                            if (competition.isFab) {
                                cell(3, row, regularFont, dogEntry["heightText"].asString)
                                cell(4, row, regularFont, dogEntry["runningOrder"].asString)
                            } else {
                                cell(3, row, regularFont, dogEntry["runningOrder"].asString)
                            }
                            row++
                        }
                    }
                }
            }
            section(1) {
                p("")
            }
        }

    }

    fun addPermit(textDocument: TextDocument, caption: String, account: Account, under: Boolean) {
        with(textDocument) {
            p {
                imageBox("8.5cm", if (under) "15.85cm" else "1.0cm", "4.0cm", "2.94cm", Global.imagesFolder + "/competition/WKC.jpg")
                textBox("1.0cm", if (under) "15.85cm" else "1.0cm", "19.0cm", "12.8cm", border = true, background = Color("#ffaaaa")) {
                    p()
                    p(arial36Bold, caption).marginTop("2.5cm").marginBottom("1.0cm").alignCenter()
                    p(arial24, competition.name).marginTop("1.0cm").marginBottom("1.0cm").alignCenter()
                    p(arial24, "Issued to: ${account.competitor.fullName}").marginTop("1.0cm").marginBottom("1.0cm")
                        .alignCenter()
                    p(arial14, "Code: ${account.code}").marginTop("1.0cm").marginBottom("1.0cm").alignCenter()
                    p(arial18Bold, "Please display in the front window of your vehicle").marginTop("1.0cm")
                        .marginBottom("1.0cm").alignCenter()
                }
            }
        }

    }

    fun addDoublePermit(textDocument: TextDocument, account: Account) {
        with(textDocument) {
            p(arial14, "PTO for Parking and Camping Permits").alignCenter()
            addPageBreak()
        }
        addPermit(textDocument, "Parking Permit", account, false)
        addPermit(textDocument, "Camping Permit", account, true)
    }

    fun addCampingPermit(textDocument: TextDocument, account: Account) {
        with(textDocument) {
            p(arial24, "PTO for Camping Permit").alignCenter()
            addPageBreak()
        }
        addPermit(textDocument, "Camping Permit", account, false)
    }

    fun addParkingPermit(textDocument: TextDocument, account: Account) {
        with(textDocument) {
            p(arial24, "PTO for Parking Permit").alignCenter()
            addPageBreak()
        }
        addPermit(textDocument, "Parking Permit", account, false)
    }

    fun getPages(path: String): Int {
        val pdf = PdfReader(path)
        val pages = pdf.numberOfPages
        pdf.close()
        return pages
    }

    fun copyPages(source: String, pdfSmartCopy: PdfSmartCopy) {
        val pdfReader = PdfReader(source)
        for (i in 1..pdfReader.numberOfPages) {
            pdfSmartCopy.addPage(pdfSmartCopy.getImportedPage(pdfReader, i))
        }
        pdfReader.close()
    }

    fun mergePdf(source: String, target: String) {
        val pdfDocument = Document()
        val targetStream = FileOutputStream(File(target + ".tmp"))
        val pdfSmartCopy = PdfSmartCopy(pdfDocument, targetStream)
        pdfDocument.open()
        if (File(target).exists()) {
            val pdfReader1 = PdfReader(target)
            for (i in 1..pdfReader1.numberOfPages) {
                pdfSmartCopy.addPage(pdfSmartCopy.getImportedPage(pdfReader1, i))
            }
        } else {
            debug("paperwork", "Target does not exist: $target")
        }

        val pdfReader = PdfReader(source)
        for (i in 1..pdfReader.numberOfPages) {
            pdfSmartCopy.addPage(pdfSmartCopy.getImportedPage(pdfReader, i))
        }
        pdfDocument.close()
        pdfReader.close()
        targetStream.close()
        File(target).delete()
        File(target + ".tmp").renameTo(File(target))
    }

    fun fixEntries(entries: String) {
        while (getPages(entries) > 2) {
            val document = PDDocument.load(File(entries))
            document.removePage(2)
            document.save(entries)
            document.close()
        }
    }

    fun mergeNotes(entries: String, itineraries: String, target: String, permits: String = "") {
        if (File(notesPath).exists()) {
            val targetStream = FileOutputStream(File(target))
//            fixEntries(entries)
            val pagesNotes = getPages(notesPath)
            val pagesEntries = getPages(entries)
            val pagesItineraries = getPages(itineraries)
            val permitPages = if (permits.isNotEmpty()) getPages(permits) else 0

            val pdfDocument = Document()
            val pdfSmartCopy = PdfSmartCopy(pdfDocument, targetStream)
            pdfDocument.open()

            copyPages(entries, pdfSmartCopy)
            if ((pagesNotes + pagesEntries).isOdd) {
                pdfSmartCopy.addPage(PageSize.A4, 0)
            }
            copyPages(notesPath, pdfSmartCopy)

            if (permits.isNotEmpty()) {
                copyPages(permits, pdfSmartCopy)
                if (permitPages.isOdd) {
                    pdfSmartCopy.addPage(PageSize.A4, 0)
                }
            }

            copyPages(itineraries, pdfSmartCopy)
            if (pagesItineraries.isOdd) {
                pdfSmartCopy.addPage(PageSize.A4, 0)
            }
            pdfDocument.close()
            targetStream.close()
        }
    }

    fun appendNotes(path: String) {
        val notes = Global.showDocumentPath(competition.uniqueName, "Notes", "pdf", canRegenerate = false)
        if (File(notes).exists()) {
            mergePdf(notes, path)
        }
    }


    fun addBatch(path: String) {
        val pageDoc = Global.showDocumentPath(idCompetition, "Mailout", "pdf")
        mergePdf(path, pageDoc)

        File(path).delete()
    }

    fun pickingList(deliveryNote: Boolean = false): String {
        val typeMonitor = ChangeMonitor(-1)
        val json = Json()
        var typeNode = Json.nullNode()
        Device().where("locationType IN ($LOCATION_SHOW) && idCompetitionStock=$idCompetition", "type, tag") {
            if (typeMonitor.hasChanged(type)) {
                val description = assetToTextPlural(type)
                typeNode = json.addElement()
                typeNode["type"] = type
                typeNode["description"] = description
            }
            val node = typeNode["assets"].addElement()
            if (type.oneOf(ASSET_TABLET, ASSET_CONTROL_BOX, ASSET_PRINTER)) {
                node["tag"] = tag
            } else {
                node["tag"] = assetCode.toLowerCase()
            }

        }

        val pickingListFile = Global.showDocumentPath(idCompetition, "picking_list", "odt")
        var breakMonitor = ChangeMonitor(false)
        competition.generateStock(post = true)
        a4Document(pickingListFile, 10.0, 10.0, 10.0, 10.0) {
            p(arialBold14, "${competition.niceName}").alignCenter()
            p(arialBold12, if (deliveryNote) "Delivery Note" else "Picking List").marginTop("1.0cm")
                .marginBottom("0.2cm")
            table(3, competition.pickingList.size + 1) {
                widths(50.0, 5.0, 15.0)
                var row = 0
                var column = 0
                cell(column++, row, arialBold10, "Item")
                cellRight(column++, row, arialBold10, "Qty")
                cellCenter(column++, row, arialBold10, "Check")
                row++
                for (item in competition.pickingList) {
                    val description = item["description"].asString
                    val type = item["type"].asInt
                    val quantity = item["quantity"].asInt
                    var picked = -1
                    val deviceType = item["device"].asInt
                    if (deviceType > 0) {
                        val node = json.searchElement("type", deviceType, create = false)
                        if (node.isNotNull) {
                            picked = node["assets"].size
                            node["included"] = true
                        } else {
                            picked = 0
                        }
                    }
                    column = 0
                    when (type) {
                        PICKING_LIST_HEADING -> {
                            cell(column, row) { mergeRight(2) }
                            cell(column++, row) {
                                p(arialBold10, description)
                            }
                        }
                        PICKING_LIST_ITEM -> {
                            cell(column++, row) { p(arial10, description + if (deviceType > 0) " *" else "") }
                                .paddingTop("0.05cm")
                                .paddingBottom("0.05cm")
                            cell(column++, row) { p(arial10, quantity.toString()).alignRight() }
                                .paddingTop("0.05cm")
                                .paddingBottom("0.05cm")
                            if (picked > -1) {
                                val text =
                                    if (picked < quantity)
                                        "${quantity - picked} short"
                                    else if (picked > quantity)
                                        "${picked - quantity} over"
                                    else if (deliveryNote)
                                        ""
                                    else
                                        "\u2714"
                                cell(column++, row) { p(arial10, text).alignCenter() }
                                    .paddingTop("0.05cm")
                                    .paddingBottom("0.05cm")
                            } else {
                                cell(column++, row) { p(arial10, "") }
                                    .paddingTop("0.05cm")
                                    .paddingBottom("0.05cm")
                            }
                        }
                        PICKING_LIST_SUB_ITEM -> {
                            cell(column, row) { mergeRight(2) }
                            cell(column++, row) {
                                p(arial9Italic, "$description x $quantity")
                                    .marginLeft("0.5cm")
                            }
                                .paddingTop("0.05cm")
                                .paddingBottom("0.05cm")
                        }
                    }
                    row++
                }

                var hasExtras = false
                for (node in json) {
                    if (!node["included"].asBoolean) hasExtras = true
                }

                if (hasExtras) {
                    appendRow()
                    column = 0
                    val headingRow = row
                    cell(column++, row) {
                        p(arialBold10, "Miscellaneous")
                    }
                    row++
                    for (node in json) {
                        if (!node["included"].asBoolean) {
                            appendRow()
                            column = 0
                            val deviceType = node["type"].asInt
                            val description = node["description"].asString
                            val quantity = node["assets"].size
                            cell(column++, row) { p(arial10, description + if (deviceType > 0) " *" else "") }
                                .paddingTop("0.05cm")
                                .paddingBottom("0.05cm")
                            cell(column++, row) { p(arial10, quantity.toString()).alignRight() }
                                .paddingTop("0.05cm")
                                .paddingBottom("0.05cm")
                            val check = if (deliveryNote) "" else "\u2714"
                            cell(column++, row) { p(arial10, check).alignCenter() }
                                .paddingTop("0.05cm")
                                .paddingBottom("0.05cm")
                            row++
                        }
                    }
                    cell(0, headingRow) { mergeRight(2) }
                }

            }.marginRight("8cm")

            if (json.size > 0) {
                p(arialBold12, "Asset Codes").marginTop("0.4cm").marginBottom("0.2cm")
                table(2, json.size) {
                    widths(30.0)
                    var row = 0
                    for (typeNode in json) {
                        val typeCode = typeNode["code"].asInt
                        val description = typeNode["description"].asString
                        val count = typeNode["assets"].size
                        var assetList = ""
                        for (node in typeNode["assets"]) {
                            assetList = assetList.append(node["tag"].asString)
                        }
                        cell(0, row, arial10, "$description x $count")
                        cell(1, row, arial10, assetList)
                        row++
                    }
                }
            }
        }
        return odtToPdf(pickingListFile)
//        return pickingListFile
    }

    fun kcTeamSheets(): String {
        val teamListFile = Global.showDocumentPath(idCompetition, "team_list", "odt")
        var breakMonitor = ChangeMonitor(false)

        a4Document(teamListFile, 10.0, 25.0, 10.0, 10.0) {
            AgilityClass().where(
                "idCompetition=$idCompetition AND classCode=${ClassTemplate.KC_CRUFTS_TEAM.code}",
                "classNumber"
            ) {
                if (!breakMonitor.hasChanged(true)) addPageBreak()
                kcTeamList(this@a4Document, id, nameLong)
            }
        }
        return odtToPdf(teamListFile)
    }

    fun kcTeamList(document: TextDocument, idAgilityClass: Int, className: String) {
        with(document) {
            p(arial14, "$className - Booking In Sheet").alignCenter().marginBottom("0.2cm")
            Entry().join { account }.join { account.competitor }.join { team }.join { agilityClass }
                .where("entry.idAgilityClass=$idAgilityClass", "runningOrder") {
                    p(arial11) {
                        marginTop("0.3cm")
                        marginBottom("0.1cm")
                        keepWithNext()
                        if (team.clubName.isNotEmpty()) {
                            span { "$runningOrder: ${team.teamName} (${team.clubName})" }
                        } else {
                            span { "$runningOrder: ${team.teamName} " }
                            span(arial14) { "(__________________________________________________)" }.highlightColor(Color.YELLOW)
                        }
                    }
                    p(arial10) {
                        marginBottom("0.1cm")
                        keepWithNext()
                        span {
                            "${account.competitor.fullName}, ${account.fullAddress.replace("\n", ", ")}, Tel ${account.competitor.phoneMobile}"
                        }
                    }
                    table(7, team.memberCount + 1) {
                        widths(12.0, 20.0, 5.0, 8.0, 15.0, 5.0, 25.0)
                        var row = 0
                        var column = 0
                        cell(column++, row, arialBold10, "Position")
                        cell(column++, row, arialBold10, "Handle")
                        cell(column++, row, arialBold10, "Run")
                        cell(column++, row, arialBold10, "Code")
                        cell(column++, row, arialBold10, "Pet Name")
                        cell(column++, row, arialBold10, "Run")
                        cell(column++, row, arialBold10, "Registered Name")
                        row++
                        for (member in team.members) {
                            column = 0
                            val position = when (row) {
                                1 -> "Dog 1"
                                2 -> "Dog 2"
                                3 -> "Dog 3"
                                4 -> "Dog 4"
                                5 -> "Reserve 1"
                                else -> "Reserve 5"
                            }
                            cell(column++, row, arial10, position)
                            cell(column++, row, arial10, member["competitorName"].asString)
                            cell(column++, row, arial10, "")
                            cell(column++, row, arial10, member["dogCode"].asString)
                            cell(column++, row, arial10, member["petName"].asString)
                            cell(column++, row, arial10, "")
                            cell(column++, row, arial10, member["registeredName"].asString)
                            row++
                        }
                        keepTogether()
                    }
                }
        }
    }

    fun kcEntryForms(idAccountWhere: Int = -1, baseFile: String = "entry_forms"): String {
        val entryFormFile = Global.showDocumentPath(idCompetition, baseFile, "odt")
        val accountMonitor = ChangeMonitor(-1)
        val dogMonitor = ChangeMonitor(-1)
        var firstPage = true
        var entryTable: Table? = null
        var row = 0
        var column = 0

        data class LedgerData(var date: Date, var type: Int, var text: String)

        var map = HashMap<Int, LedgerData>()

        Ledger().where("idCompetition=$idCompetition AND type IN ($LEDGER_ENTRY_FEES, $LEDGER_ENTRY_FEES_PAPER)") {
            val dateEntered =
                if (dateCreated.dateOnly() > competition.dateCloses) competition.dateCloses else dateCreated.dateOnly()
            val text = if (type == LEDGER_ENTRY_FEES_PAPER) "Paper" else "Online"
            map.put(this.idAccount, LedgerData(dateEntered, type, "$text (${dateEntered.shortText})"))
        }

        a4Document(entryFormFile, 10.0, 10.0, 10.0, 10.0) {
            val whereAccount = if (idAccountWhere > 0) " AND entry.idAccount=$idAccountWhere" else ""
            Entry().join { team }.join { agilityClass }.join { account }.join { account.competitor }
                .join { team.competitor }.join { team.dog }
                .join { team.dog.owner }
                .where("agilityClass.idCompetition=$idCompetition$whereAccount", "competitor_1.givenName, competitor_1.familyName, team.idDog, classDate, classNumber") {
                    if (accountMonitor.hasChanged(this.idAccount)) {
                        val data = map.getOrDefault(this.idAccount, LedgerData(nullDate, 0, ""))
                        if (firstPage) firstPage = false else addPageBreak()
                        p1 {
                            span(arialBold14) { "Electronic Entry Form (${competition.name})" }
                        }.marginBottom("0.25cm").alignCenter()

                        table(2, 1, borderless = true) {
                            cell(0, 0, arial11, account.competitor.fullName + "\n" + account.fullAddress)
                            cell(1, 0) {
                                table(2, 5, borderless = true, padding = "0.0cm") {
                                    widths(30.0, 70.0)
                                    var row = 0
                                    cell(0, row, arial11, "Email:"); cell(1, row++, arial11, account.competitor.email)
                                    if (account.competitor.phoneMobile.isNotEmpty()) {
                                        cell(0, row, arial11, "Mobile:"); cell(1, row++, arial11, account.competitor.phoneMobile)
                                    }
                                    if (account.competitor.phoneOther.isNotEmpty()) {
                                        cell(0, row, arial11, "Phone:"); cell(1, row++, arial11, account.competitor.phoneOther)
                                    }
                                    cell(0, row, arial11, "Account:"); cell(1, row++, arial11, account.code)
                                    cell(0, row, arial11, "Entered:"); cell(1, row++, arial11, data.text)
                                }
                            }

                        }.marginBottom("0.1cm")
                    }
                    if (dogMonitor.hasChanged(team.idDog)) {
                        p(arial11, "${team.dog.registeredName} (${team.dog.idKC})")
                            .paddingTop("0.1cm")
                            .borderTop("0.06pt solid #000000")
                            .marginBottom("0.1cm")
                            .keepWithNext()
                        table(2, 5, borderless = true, padding = "0.0cm") {
                            widths(15.0, 85.0)
                            var row = 0
                            cell(0, row, condensed, "Owner:"); cell(1, row++, condensed, "${team.dog.owner.fullName}, ${account.fullAddress.replace("\n", ", ")}")
                            cell(0, row, condensed, "Breed:"); cell(1, row++, condensed, Breed.getBreedName(team.dog.idBreed))
                            cell(0, row, condensed, "Born:"); cell(1, row++, condensed, team.dog.dateOfBirth.shortText)
                            cell(0, row, condensed, "Sex:"); cell(1, row++, condensed, team.dog.genderText)
                            cell(0, row, condensed, "Grade/Height:"); cell(1, row++, condensed, "${Grade.getGradeName(gradeCode)}/${Height.getHeightName(jumpHeightCode)}")

                        }.marginBottom("0.1cm").keepRowsTogether().keepTogether()
                        entryTable = table(4, 1, borderless = true) {
                            widths(5.0, 25.0, 15.0, 5.0)
                            row = 0
                            column = 0
                            cell(column++, row, condensedBold, "Date")
                            cell(column++, row, condensedBold, "Class")
                            cell(column++, row, condensedBold, "Handler")
                            cell(column++, row, condensedBold, "Fee")
                            row++
                        }.marginBottom("0.1cm").keepRowsTogether().keepTogether()
                    }
                    val thisTable = entryTable
                    if (thisTable != null) {
                        with(thisTable) {
                            column = 0
                            cell(column++, row, condensed, agilityClass.date.shortText)
                            cell(column++, row, condensed, agilityClass.name)
                            cell(column++, row, condensed, team.competitor.fullName)
                            cell(column++, row, condensed, fee.money)
                            row++
                        }
                    }
                }
        }
        return odtToPdf(entryFormFile)
    }

    fun quick(baseFile: String = "running_order_list"): String {
        val entryFormFile = Global.showDocumentPath(idCompetition, baseFile, "odt")
        val dogMonitor = ChangeMonitor(-1)
        var row = 0
        var column = 0
        var lastName = ""

        data class LedgerData(var date: Date, var type: Int, var text: String)

        var idAccountList = ""

        dbQuery("SELECT idAccount FROM ledgerItem WHERE idCompetition=${idCompetition} AND type IN ($LEDGER_ITEM_POSTAGE, $LEDGER_ITEM_PAPER)") {
            idAccountList = idAccountList.append(getString("idAccount"), ",")
        }

        a4Document(entryFormFile, 10.0, 10.0, 10.0, 10.0) {
            var entryTable = table(4, 1, borderless = true)
            entryTable.widths(25.0, 25.0, 10.0, 50.0)

            var thisName = ""
            var thisCode = ""
            var thisPet = ""

            fun x(name: String, petName: String, code: String, items: String) {
                with(entryTable) {
                    column = 0
                    cell(column++, row, condensed, if (name != lastName) name else "")
                    cell(column++, row, condensed, petName)
                    cell(column++, row, condensed, code)
                    cell(column++, row, condensed, items)
                    row++
                    lastName = name
                }

            }

            var list = ""

            Entry().join { team }.join { agilityClass }
                .join { team.competitor }.join { team.dog }
                .join { team.dog.owner }
                .where("agilityClass.idCompetition=${idCompetition} AND entry.idAccount IN ($idAccountList)", "competitor.givenName, competitor.familyName, dog.dogCode, classDate, classNumber") {
                    if (progress == PROGRESS_ENTERED) {
                        if (dogMonitor.hasChanged(team.dog.code)) {
                            x(thisName, thisPet, thisCode, list)
                            thisName = competitorName
                            thisCode = team.dog.code.toString()
                            thisPet = team.dog.petName
                            list = ""
                        }
                        list = list.append("${agilityClass.number}: $runningOrder")
                        if (isLast) {
                            x(competitorName, team.dog.petName, team.dog.code.toString(), list)
                        }
                    }
                }
        }
        //       return odtToPdf(entryFormFile)
        return entryFormFile
    }


    fun addRingBoards(document: TextDocument, json: Json, competitorNode: JsonNode, lowerHeight: Boolean = true) {
        var first = true
        var pages = 0
        with(document) {
            for (date in json["dates"]) {
                for (ring in date["rings"]) {
                    pages++
                    if (first) first = false else addPageBreak()
                    val ringNumber = ring["ringNumber"].asInt
                    val judge = ring["judge"].asString
                    var hasLowerHeight = false
                    for (agilityClass in ring["classes"]) {
                        if (agilityClass["heights"].size > 1) {
                            hasLowerHeight = true
                        }
                    }
                    p(arial11, date["date"].asDate.fullDate())
                    p(arial36, "Ring $ringNumber - $judge").alignCenter().marginTop("3cm").marginBottom("2cm")

                    table(if (lowerHeight && hasLowerHeight) 4 else 2, ring["classes"].size + 1) {
                        if (lowerHeight && hasLowerHeight) {
                            widths(null, 26.0, 26.0, 30.0)
                            cell(0, 0, arial18Bold, "Class")
                            if (competition.lhoFirst) {
                                cellRight(1, 0, arial18Bold, "LHO")
                                cellRight(2, 0, arial18Bold, "FH")
                            } else {
                                cellRight(1, 0, arial18Bold, "FH")
                                cellRight(2, 0, arial18Bold, "LHO")
                            }
                            cellRight(3, 0, arial18Bold, "Total")
                        } else {
                            widths(null, 30.0)
                            cell(0, 0, arial18Bold, "Class")
                            cellRight(1, 0, arial18Bold, "Total")
                        }
                        var row = 1
                        for (agilityClass in ring["classes"]) {
                            val idAgilityClass = agilityClass["idAgilityClass"].asInt
                            val classNode = competitorNode["classes"].searchElement(
                                "idAgilityClass",
                                idAgilityClass,
                                create = false
                            )
                            val classEntries = classNode["entries"]
                            cell(0, row, arial18, agilityClass["className"].asString)
                            val heights = agilityClass["heights"]
                            if (lowerHeight && hasLowerHeight) {
                                if (heights.size == 2) {
                                    cellRight(1, row, arial18, heights[0]["runCount"].asString)
                                    cellRight(2, row, arial18, heights[1]["runCount"].asString)

                                }
                                cellRight(3, row++, arial18, agilityClass["runCount"].asString)
                            } else {
                                if (agilityClass["runCount"].asInt > 0) cellRight(
                                    1,
                                    row,
                                    arial18,
                                    agilityClass["runCount"].asString
                                )
                                row++
                            }
                        }
                        if (hasLowerHeight) {
                            p(arial14, "(*) LHO class with combined results, (#) LHO class with separate results.")
                                .marginTop("2.0cm")
                        } else {
                            p("")
                        }

                    }

                }
            }
        }
    }

}

