/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.library.dbobject

import org.egility.library.database.*
import org.egility.library.general.*
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

open class CompetitionRaw<T : DbTable<T>>(_connection: DbConnection? = null, vararg columnNames: String) :
    DbTable<T>(_connection, "competition", *columnNames) {

    open var id: Int by DbPropertyInt("idCompetition")
    open var idOrganization: Int by DbPropertyInt("idOrganization")
    open var idEntity: Int by DbPropertyInt("idEntity")
    open var name: String by DbPropertyString("name")
    open var uniqueName: String by DbPropertyString("uniqueName")
    open var briefName: String by DbPropertyString("briefName")
    open var venue: String by DbPropertyString("venue")
    open var venueAddress: String by DbPropertyString("venueAddress")
    open var venuePostcode: String by DbPropertyString("venuePostcode")
    open var dateStart: Date by DbPropertyDate("dateStart")
    open var dateEnd: Date by DbPropertyDate("dateEnd")
    open var campingStart: Date by DbPropertyDate("campingStart")
    open var campingEnd: Date by DbPropertyDate("campingEnd")
    open var dateOpens: Date by DbPropertyDate("dateOpens")
    open var dateCloses: Date by DbPropertyDate("dateCloses")
    open var dateProcessing: Date by DbPropertyDate("dateProcessing")
    open var emails: Int by DbPropertyInt("emails")
    open var cappingLevel: Int by DbPropertyInt("cappingLevel")
    open var agilityClassDates2: String by DbPropertyString("agilityClassDates")
    open var entryFee: Int by DbPropertyInt("entryFee")
    open var entryFeeMembers: Int by DbPropertyInt("entryFeeMembers")
    open var entryFeePaper: Int by DbPropertyInt("entryFeePaper")
    open var lateEntryFee: Int by DbPropertyInt("lateEntryFee")
    open var lateEntryRestricted: Boolean by DbPropertyBoolean("lateEntryRestricted")
    open var campingPitches: Int by DbPropertyInt("campingPitches")
    open var campingCapSystem: Int by DbPropertyInt("campingCapSystem")
    open var campingReleased: Int by DbPropertyInt("campingReleased")
    open var campingFull: Boolean by DbPropertyBoolean("campingFull")
    open var mainCampingBlock: Int by DbPropertyInt("mainCampingBlock")
    open var heightsGenerated: Boolean by DbPropertyBoolean("heightsGenerated")
    open var complimentaryAllowance: Int by DbPropertyInt("complimentaryAllowance")
    open var announcement: String by DbPropertyString("announcement")
    open var heightOptions: String by DbPropertyString("heightOptions")
    open var provisional: Boolean by DbPropertyBoolean("provisional")
    open var hidden: Boolean by DbPropertyBoolean("hidden")
    open var capReached: Boolean by DbPropertyBoolean("capReached")
    open var closed: Boolean by DbPropertyBoolean("closed")
    open var processed: Boolean by DbPropertyBoolean("processed")
    open var holdItineraries: Boolean by DbPropertyBoolean("holdItineraries")
    open var imported: Boolean by DbPropertyBoolean("imported")
    open var showManager: String by DbPropertyString("showManager")
    open var showSecretary: String by DbPropertyString("showSecretary")
    open var rosetteRule: String by DbPropertyString("rosetteRule")
    open var trophyRule: String by DbPropertyString("trophyRule")
    open var processingFee: Int by DbPropertyInt("processingFee")
    open var extra: Json by DbPropertyJson("extra")
    open var dateCreated: Date by DbPropertyDate("dateCreated")
    open var instanceCreated: Int by DbPropertyInt("instanceCreated")
    open var dateModified: Date by DbPropertyDate("dateModified")
    open var instanceModified: Int by DbPropertyInt("instanceModified")
    open var dateDeleted: Date by DbPropertyDate("dateDeleted")

    // bit flags
    open var preCloseEmail: Boolean by DbPropertyBit("emails", 0)
    open var postCloseEmail: Boolean by DbPropertyBit("emails", 1)
    open var postCloseFinalEmail: Boolean by DbPropertyBit("emails", 2)

    // json extras
    open var dataLastGenerated: Int by DbPropertyJsonInt("extra", "data.lastGenerated")
    open var dataVersion: Int by DbPropertyJsonInt("extra", "data.version")
    open var ringPlanLastGenerated: Int by DbPropertyJsonInt("extra", "ringPlan.lastGenerated")
    open var ringVersion: Int by DbPropertyJsonInt("extra", "ringPlan.version")
    open var grandFinals: Boolean by DbPropertyJsonBoolean("extra", "grandFinals")
    open var logo: String by DbPropertyJsonString("extra", "logo")
    open var lhoOrder: String by DbPropertyJsonString("extra", "kc.lhoOrder")
    open var maxRuns: Int by DbPropertyJsonInt("extra", "kc.maxRuns")
    open var resultsCopies: Int by DbPropertyJsonInt("extra", "copies.results")
    open var awardsCopies: Int by DbPropertyJsonInt("extra", "copies.awards")
    open var allOrNothing: Boolean by DbPropertyJsonBoolean("extra", "allOrNothing")
    open var noPosting: Boolean by DbPropertyJsonBoolean("extra", "noPosting")
    open var forceSponsor: Boolean by DbPropertyJsonBoolean("extra", "forceSponsor")
    open var mandatoryPostage: Boolean by DbPropertyJsonBoolean("extra", "mandatoryPostage")
    open var organizerPosts: Boolean by DbPropertyJsonBoolean("extra", "organizerPosts")
    open var minimumFee: Int by DbPropertyJsonInt("extra", "minimumFee")
    open var minimumFeeMembers: Int by DbPropertyJsonInt("extra", "minimumFeeMambers")
    open var maximumFee: Int by DbPropertyJsonInt("extra", "maximumFee")
    open var maximumFeeMembers: Int by DbPropertyJsonInt("extra", "maximumFeeMambers")
    open var adminFee: Int by DbPropertyJsonInt("extra", "adminFee")
    open var cappingLevelPublished: Int by DbPropertyJsonInt("extra", "cap.published")
    open var capOnEntries: Boolean by DbPropertyJsonBoolean("extra", "cap.onEntries")

    open var hasCruftsTeams: Boolean by DbPropertyJsonBoolean("extra", "hasTeams")

    open var showManagerEmail: String by DbPropertyJsonString("extra", "showManager.email")
    open var showManagerPhone: String by DbPropertyJsonString("extra", "showManager.phone")
    open var showSecretaryEmail: String by DbPropertyJsonString("extra", "showSecretary.email")
    open var showSecretaryPhone: String by DbPropertyJsonString("extra", "showSecretary.phone")

    open var camping: JsonNode by DbPropertyJsonObject("extra", "camping")
    open var noGroupCamping: Boolean by DbPropertyJsonBoolean("extra", "noGroupCamping")

    open var bankAccountName: String by DbPropertyJsonString("extra", "bank.account")
    open var bankAccountSort: String by DbPropertyJsonString("extra", "bank.sort")
    open var bankAccountNumber: String by DbPropertyJsonString("extra", "bank.number")

    open var judges: JsonNode by DbPropertyJsonObject("extra", "judges")

    open var anySizeRule: String by DbPropertyJsonString("extra", "kc.anySizeRule")
    open var lhoRule: String by DbPropertyJsonString("extra", "kc.lhoRule")

    open var ifcsFee: Int by DbPropertyJsonInt("extra", "fab.ifcsFee")


    open var ageBaseDate: Date by DbPropertyJsonDate("extra", "misc.ageBaseDate")
    open var printCost: Int by DbPropertyJsonInt("extra", "misc.printCost.total")
    open var printQuantity: Int by DbPropertyJsonInt("extra", "misc.printCost.quantity")
    open var processingFeeSwap: Int by DbPropertyJsonInt("extra", "misc.fees.swap")
    open var ringsNeeded: Int by DbPropertyJsonInt("extra", "misc.rings")

    open var combinedFee: Boolean by DbPropertyJsonBoolean("extra", "misc.combinedFee")
    open var awardRule: String by DbPropertyJsonString("extra", "misc.awardRule")

    open var ukOpenLocked: Boolean by DbPropertyJsonBoolean("extra", "ukopen.locked")

    open var itineraryNote: String by DbPropertyJsonString("extra", "itinerary.note")
    open var importantNote: String by DbPropertyJsonString("extra", "note.important")
    open var entryNote: String by DbPropertyJsonString("extra", "note.entry")

    open var tabletsSecretary: Int by DbPropertyJsonInt("extra", "stock.tablets.secs")
    open var tabletsSpare: Int by DbPropertyJsonInt("extra", "stock.tablets.spare")
    open var acusSpare: Int by DbPropertyJsonInt("extra", "stock.acus.spare")
    open var tripods: Int by DbPropertyJsonInt("extra", "stock.tripods")
    open var pickingList: JsonNode by DbPropertyJsonObject("extra", "stock.pick")

    open var parkingPermit: Boolean by DbPropertyJsonBoolean("extra", "permit.parking")
    open var campingPermit: Boolean by DbPropertyJsonBoolean("extra", "permit.camping")

    open var society: String by DbPropertyJsonString("extra", "catalogue.society")
    open var secretary: String by DbPropertyJsonString("extra", "catalogue.secretary")
    open var guarantors: String by DbPropertyJsonString("extra", "catalogue.guarantors")

    open var gradeCodes: String by DbPropertyJsonString("extra", "independent.gradeCodes")
    open var heightCodes: String by DbPropertyJsonString("extra", "independent.heightCodes")
    open var minMonths: Int by DbPropertyJsonInt("extra", "independent.minMonths")

    open var independentType: String by DbPropertyJsonString("extra", "independent.type")
    open var clearRoundOnly: Boolean by DbPropertyJsonBoolean("extra", "independent.cro")
    open var secondChance: Boolean by DbPropertyJsonBoolean("extra", "independent.secondChance")
    open var bonusCategories: String by DbPropertyJsonString("extra", "independent.categories")

    open var minFeeAllDays: Boolean by DbPropertyJsonBoolean("extra", "rule.minFeeAllDays")


    val organization: Organization by DbLink<Organization>({ Organization() })
    val entity: Entity by DbLink<Entity>({ Entity() })
    val geoData: GeoData by DbLink<GeoData>({ GeoData() }, keyNames = *arrayOf("venuePostcode"))

}

class CampingBlock(
    val competition: Competition,
    var start: Date,
    var end: Date,
    val dayBooking: Boolean,
    val blockRate: Int,
    val dayRate: Int,
    var note: String
) {

    val baseDate: Date
        get() = competition.campingStart
    val blockMask: Int
        get() = Camping.blockMask(baseDate, start, end)

    fun useBlockRate(dayFlags: Int): Boolean {
        val match = dayFlags and blockMask
        return if (match != 0 && dayBooking) {
            val days = match.bitCount
            val fees = days * dayRate
            blockRate > 0 && blockRate < fees
        } else match != 0
    }

    fun days(dayFlags: Int): Int {
        val match = dayFlags and blockMask
        return match.bitCount
    }

    fun fee(dayFlags: Int): Int {
        if (useBlockRate(dayFlags)) {
            return blockRate
        } else {
            val match = dayFlags and blockMask
            val days = match.bitCount
            return days * dayRate
        }
    }

}

class Competition(vararg columnNames: String) : CompetitionRaw<Competition>(null, *columnNames) {

    constructor(idCompetition: Int) : this() {
        find(idCompetition)
    }

    val rings: Int
        get() {
            var result = 0
            dbQuery("SELECT MAX(ringNumber) as rings FROM ring WHERE idCompetition=$id") {
                result = getInt("rings")
            }
            return result
        }

    val hasVouchers: Boolean
        get() {
            var result = false
            dbQuery("SELECT true AS hasVouchers FROM voucher WHERE idCompetition=$id LIMIT 1") { result = true }
            return result
        }

    val hasSubClasses: Boolean
        get() {
            var result = false
            dbQuery("SELECT true AS hasSubGroups FROM entry JOIN agilityClass USING (idAgilityClass) WHERE agilityClass.idCompetition=$id AND entry.subClass>0 AND entry.progress<$PROGRESS_REMOVED LIMIT 1") { result = true }
            return result
        }

    val bonusCategoriesRaw: String
        get() = super.bonusCategories

    override var bonusCategories: String
        get() = super.bonusCategories.replace("_", " ").replace(",", ", ")
        set(value) {
            var data = ""
            for (item in value.split(",")) {
                data = data.append(item.trim().replace(" ", "_"), ",")
            }
            super.bonusCategories = data
        }

    override var cappingLevelPublished: Int
        get() = if (super.cappingLevelPublished > 0) super.cappingLevelPublished else cappingLevel
        set(value) {
            super.cappingLevelPublished = value
        }

    fun generateStock(post: Boolean = true) {

        fun addPickingListItem(description: String, quantity: Int = 0, deviceType: Int = 0, type: Int = PICKING_LIST_ITEM) {
            if (type == PICKING_LIST_HEADING || quantity > 0) {
                val node = pickingList.addElement()
                node["description"] = description
                node["type"] = type
                node["quantity"] = quantity
                node["device"] = deviceType
            }
        }

        val ringCount = rings
        if (tabletsSecretary == 0) tabletsSecretary = 3
        if (tabletsSpare == 0) tabletsSpare = maxOf((ringCount + 1) / 2, 2)
        if (acusSpare == 0) acusSpare = 1

        pickingList.clear()
        addPickingListItem("Tablets & Accessories", type = PICKING_LIST_HEADING)
        addPickingListItem("Tablets", 1 + tabletsSecretary + ringCount * 3 + tabletsSpare, deviceType = ASSET_TABLET)
        addPickingListItem("System Manager", 1, type = PICKING_LIST_SUB_ITEM)
        addPickingListItem("Secretary", tabletsSecretary, type = PICKING_LIST_SUB_ITEM)
        addPickingListItem("Ring Party", ringCount * 3, type = PICKING_LIST_SUB_ITEM)
        addPickingListItem("Spare", tabletsSpare, type = PICKING_LIST_SUB_ITEM)
        addPickingListItem("Cases", ringCount * 3 + tabletsSecretary + tabletsSpare + 1)
        addPickingListItem("Dabbers", ringCount * 2 + tabletsSecretary)
        addPickingListItem("Ring Party Boxes", ringCount + 1, deviceType = ASSET_RING_PARTY_BOX)
        addPickingListItem("User Guide", ringCount + 1, deviceType = ASSET_RING_PARTY_GUIDE)
        addPickingListItem("ACU Keys", 2)
        addPickingListItem("Control Boxes", type = PICKING_LIST_HEADING)
        addPickingListItem("ACUs", ringCount + 1 + acusSpare, deviceType = ASSET_CONTROL_BOX)
        addPickingListItem("Dongles (inside ACUs)", 2, deviceType = ASSET_DONGLE)
        addPickingListItem("Tripods", tripods, deviceType = ASSET_TRIPOD)
        addPickingListItem("Power", type = PICKING_LIST_HEADING)
        addPickingListItem("Power Packs", (ringCount + 1) * 2, deviceType = ASSET_POWER_PACK)
        addPickingListItem("Charge Bricks", (ringCount * 3 + tabletsSecretary + 1 + 5) / 6, deviceType = ASSET_CHARGE_BRICK)
        addPickingListItem("Charge Brick Cables", (ringCount * 3 + tabletsSecretary + 1 + 5) / 6)
        addPickingListItem("Extension Cable", 1, deviceType = ASSET_MAINS_EXTENSION)
        addPickingListItem("Printing", type = PICKING_LIST_HEADING)
        addPickingListItem("Case", 1)
        addPickingListItem("Printer", 1, deviceType = ASSET_PRINTER)
        addPickingListItem("Power Cable", 1)
        addPickingListItem("Spare Cartridge - Black", 1)
        addPickingListItem("Spare Cartridge - Colour", 1)
        if (post) this.post()
    }

    fun changeClosingDate(newClosingDate: Date) {
        dbTransaction {
            dateCloses = newClosingDate
            post()
            dbExecute(
                "UPDATE ledger SET dateEffective=${newClosingDate.sqlDate} WHERE NOT dueImmediately " +
                        "AND type IN ($LEDGER_ENTRY_FEES_PAPER, $LEDGER_ENTRY_FEES) AND idCompetition=$id"
            )
        }
    }

    val duration: Int
        get() = dateEnd.daysSince(dateStart) + 1

    override var minMonths: Int
        get() = if (super.minMonths == 0)
            if (isIndependent && independentType == "AA") 16
            else 18
        else super.minMonths
        set(value) {
            super.minMonths = value
        }

    override var campingReleased: Int
        get() = super.campingReleased
        set(value) {
            dbTransaction {
                super.campingReleased = value
                post()
                Camping.approveWaiting(id)
            }
        }

    override var noPosting: Boolean
        get() = super.noPosting || grandFinals
        set(value) {
            super.noPosting = value
        }

    override var rosetteRule: String
        get() = if (super.rosetteRule.isNotEmpty()) super.rosetteRule else if (isFab) "10%+9" else "10%+5"
        set(value) {
            super.rosetteRule = value
        }

    override var trophyRule: String
        get() = if (super.trophyRule.isNotEmpty()) super.trophyRule else if (isFab) "bands:0,10,20,30,40,50,60,70,80,90,100" else "bands:0,29,49"
        set(value) {
            super.trophyRule = value
        }

    val weekNumber: Int
        get() = dateStart.addDays(-2).format("w").toInt()
    
    override var awardRule: String
        get() = if (super.awardRule.isEmpty()) "bands:9999" else super.awardRule
        set(value) {
            super.awardRule = value
        }

    val organizationData: OrganizationData
        get() = Organization.get(idOrganization)

    val hasManagedCamping: Boolean
        get() = hasCamping && campingCapSystem != CAMPING_CAP_UNCAPPED

    val hasCamping: Boolean
        get() = campingPitches > 0 || campingRate > 0 || campingRateDay > 0 || campingCapSystem != CAMPING_CAP_UNCAPPED

    val needsCampingDeposit: Boolean
        get() = campingCapSystem.oneOf(
            CAMPING_CAP_DEPOSIT,
            CAMPING_CAP_DEPOSIT_PARTIALLY_REFUNDABLE,
            CAMPING_CAP_DEPOSIT_FULLY_REFUNDABLE
        )

    val campingRefundOption: Int
        get() = when (campingCapSystem) {
            CAMPING_CAP_DEPOSIT -> 1
            CAMPING_CAP_DEPOSIT_PARTIALLY_REFUNDABLE -> 2
            CAMPING_CAP_DEPOSIT_FULLY_REFUNDABLE -> 3
            else -> 0
        }

    var options: String
        get() {
            var result = ""
            if (allOrNothing) result = result.append("all-or-nothing")
            if (noPosting) result = result.append("no-posting")
            if (forceSponsor) result = result.append("force-sponsor")
            if (mandatoryPostage) result = result.append("mandatory-postage")
            if (organizerPosts) result = result.append("organiser-posts")
            if (clearRoundOnly) result = result.append("clear-round-only")
            if (secondChance) result = result.append("second-chance")
            if (noGroupCamping) result = result.append("no-group-camping")
            if (minFeeAllDays) result = result.append("minimum-fee-all-days")
            if (combinedFee) result = result.append("combined-fee")
            if (capOnEntries) result = result.append("cap-on-entries")
            if (super.minMonths > 0) result = result.append("min-months-$minMonths")
            return result
        }
        set(value) {
            val options = value.toLowerCase().replace(" ", "").split(",")
            allOrNothing = options.contains("all-or-nothing")
            noPosting = options.contains("no-posting")
            forceSponsor = options.contains("force-sponsor")
            mandatoryPostage = options.contains("mandatory-postage")
            organizerPosts = options.contains("organiser-posts")
            clearRoundOnly = options.contains("clear-round-only")
            secondChance = options.contains("second-chance")
            noGroupCamping = options.contains("no-group-camping")
            minFeeAllDays = options.contains("minimum-fee-all-days")
            combinedFee = options.contains("combined-fee")
            capOnEntries = options.contains("cap-on-entries")
            minMonths = 0
            for (option in options) {
                if (option.startsWith("min-months-")) {
                    minMonths = option.drop(11).toIntDef(0)
                }
            }
        }


    val postageFee: Int
        get() = if (isUkOpen)
            200
        else if (isUka && dateStart >= "2019-06-01".toDate())
            150
        else
            100

    override var adminFee: Int
        get() = if (super.adminFee > 0) super.adminFee else if (isFab) 275 else 200
        set(value) {
            super.adminFee = value
        }

    override fun whenBeforePost() {
        super.whenBeforePost()
    }


    override var ageBaseDate: Date
        get() = if (super.ageBaseDate.isEmpty()) dateStart else super.ageBaseDate
        set(value) {
            super.ageBaseDate = value
        }

    override var processingFeeSwap: Int
        get() = if (super.processingFeeSwap == 0) 8 else super.processingFeeSwap
        set(value) {
            super.processingFeeSwap = value
        }

    val kc2020Rules: Boolean
        get() = dateStart.after(kc2020RulesBaseDate)


    var printCostPound: String
        get() = printCost.money
        set(value) {
            printCost = value.poundsToPence()
        }

    val hasClosed: Boolean
        get() = dateCloses < today || closed

    val isOpen: Boolean
        get() = dateOpens <= now && !hasClosed

    val dogsEnteredList: String
        get() {
            val idList=ArrayList<Int>()
            dbQuery("""
                    SELECT 
                        team.idDog
                    FROM
                        entry
                            JOIN
                        agilityClass USING (idAgilityClass)
                            JOIN
                        team USING (idTeam)
                    WHERE
                        idCompetition = $id                  
            """){
                idList.add(getInt("idDog"))
            }
            return idList.asCommaList()
        }

    fun checkCap() {
        if (cappingLevel > 0) {
            val sql = if (capOnEntries) {
                """
                    SELECT 
                        COUNT(*) AS entries
                    FROM
                        entry
                            JOIN
                        agilityClass USING (idAgilityClass)
                    WHERE
                        idCompetition = $id AND entry.entryType IN ($ENTRY_AGILITY_PLAZA, $ENTRY_PAPER)                     
                """.trimIndent()
            } else {
                """
                    SELECT 
                        COUNT(DISTINCT team.idDog) AS entries
                    FROM
                        entry
                            JOIN
                        agilityClass USING (idAgilityClass)
                            JOIN
                        team USING (idTeam)
                    WHERE
                        idCompetition = $id AND entry.entryType IN ($ENTRY_AGILITY_PLAZA, $ENTRY_PAPER)                     
                """.trimIndent()
            }
            var entries = 0
            dbQuery(sql) { entries = getInt("entries") }
            capReached = entries >= cappingLevel
            post()
        }
    }

    val entryInfo: String
        get() = if (provisional) {
            "TBA"
        } else if (capReached) {
            "Cap Reached"
        } else if (dateOpens > today) {
            if (dateOpens.format("HH").toInt() > 1) {
                "Open ${dateOpens.format("EEE d MMM, HH:mm")}"
            } else {
                "Open ${dateOpens.format("EEE d MMM")}"
            }
        } else if (dateCloses == today) {
            "CLOSES TODAY"
        } else if (dateCloses > today) {
            "Close ${dateCloses.format("EEE d MMM")}"
        } else {
            "Closed"
        }

    val campingBlocks: Int
        get() = if (extra.has("camping")) camping.size else 0


    val canDeleteUnpaid: Boolean
        get() = postCloseFinalEmail && !processed && closed && dateCloses.addDays(6) <= today


    val agilityClassDates: String
        get() {
            var result = ""
            dbQuery("SELECT GROUP_CONCAT(DISTINCT classDate ORDER BY classDate , ',') AS agilityClassDates FROM agilityclass where idCompetition=$id") {
                result = getString("agilityClassDates")
            }
            return result
        }

    fun getCampingBlock(index: Int): CampingBlock {
        val node = camping[index]
        return CampingBlock(
            this,
            node["start"].asDate,
            node["end"].asDate,
            node["dayBooking"].asBoolean,
            node["blockRate"].asInt,
            node["dayRate"].asInt,
            node["note"].asString
        )
    }

    fun setCampingBlock(index: Int, value: CampingBlock) {
        debug("campingBlock", "Index: $index, Start: ${value.start.dateText}")
        val node = camping[index]
        node["start"] = value.start.dateOnly()
        node["end"] = value.end.dateOnly()
        node["dayBooking"] = value.dayBooking
        node["blockRate"] = value.blockRate
        node["dayRate"] = value.dayRate
        node["note"] = value.note
    }

    val campingNote: String
        get() = extra["camping.$mainCampingBlock.note"].asString

    val campingRate: Int
        get() = extra["camping.$mainCampingBlock.blockRate"].asInt

    val campingRateDay: Int
        get() = extra["camping.$mainCampingBlock.dayRate"].asInt

    val campingDays: Int
        get() = extra["camping.$mainCampingBlock.end"].asDate.daysSince(extra["camping.$mainCampingBlock.start"].asDate) + 1

    val hasCampingBlockDiscount: Boolean
        get() = campingRate > 0 && campingRateDay > 0 && campingRateDay * campingDays > campingRate


    val isToday: Boolean
        get() {
            return DbQuery("SELECT idCompetition FROM agilityClass WHERE idCompetition=$id AND classDate=${realToday.sqlDate} LIMIT 1").found()
        }

    val nextCompetitionDay: Date
        get() {
            var result = dateCloses
            dbQuery("SELECT classDate FROM agilityClass WHERE idCompetition=$id AND classDate>=${realToday.sqlDate} ORDER BY classDate LIMIT 1") {
                result = getDate("classDate")
            }
            return result
        }

    override var dateProcessing: Date
        get() {
            if (super.dateProcessing.isEmpty()) {
                val mondayShift = ((dateStart.dayOfWeek() + 4).rem(7) + 1)
                return dateStart.addDays(-mondayShift - 7)
            }
            return super.dateProcessing
        }
        set(value) {
            super.dateProcessing = value
        }

    val _lhoOrder: String
        get() = super.lhoOrder

    override var lhoOrder: String
        get() {
            if (super.lhoOrder.isEmpty()) {
                var test = ""
                AgilityClass().where("idCompetition=$id") {
                    if (isFullLower) {
                        if (heightRunningOrder.substringBefore(",").endsWith("L")) {
                            when (test) {
                                "" -> test = "first"
                                "last" -> test = "mixed"
                            }
                        } else {
                            when (test) {
                                "" -> test = "last"
                                "first" -> test = "mixed"
                            }
                        }
                    }
                }
                super.lhoOrder = test
                post()
            }
            return super.lhoOrder
        }
        set(value) {
            super.lhoOrder = value
        }

    val lhoMixed: Boolean
        get() {
            return lhoOrder == "mixed"
        }

    var lhoFirst: Boolean
        get() {
            return lhoOrder == "first"
        }
        set(value) {
            lhoOrder = "first"
        }

    var showSecretaryCodes: String
        get() {
            var result = ""
            CompetitionOfficial().join { competitor }.where("idCompetition=$id") {
                result = result.append(competitor.code)
            }
            return result
        }
        set(value) {
            val list = ArrayList<String>(value.replace(" ", "").split(","))
            CompetitionOfficial().join { competitor }.where("idCompetition=$id") {
                if (competitor.id <= 0 || !list.contains(competitor.code)) {
                    delete(reposition = false)
                } else {
                    list.remove(competitor.code)
                }
            }
            for (code in list) {
                val idCompetitor = Competitor.competitorCodeToIdCompetitor(code)
                if (idCompetitor > 0) {
                    CompetitionOfficial().withPost {
                        append()
                        idCompetition = id
                        this.idCompetitor = idCompetitor
                    }
                }
            }
        }

    val isKc: Boolean
        get() = idOrganization == ORGANIZATION_KC

    val isIndependent: Boolean
        get() = idOrganization == ORGANIZATION_INDEPENDENT

    val isUka: Boolean
        get() = idOrganization == ORGANIZATION_UKA

    val isUkOpen: Boolean
        get() = idOrganization == ORGANIZATION_UK_OPEN

    val isUkaManaged: Boolean
        get() = isUka || isUkOpen

    val isPlazaManaged: Boolean
        get() = !isUkaManaged

    val isFab: Boolean
        get() = idOrganization == ORGANIZATION_FAB

    val isFabKc: Boolean
        get() = isKc && showSecretaryCodes.contains("DW2371")

    val isInProgress: Boolean
        get() = dateStart >= today && dateEnd >= today

    val isFinished: Boolean
        get() = dateEnd < today

    val hasBookingIn: Boolean
        get() = (isUka && !grandFinals)

    val hasEgilityCodes: Boolean
        get() = isUka

    val hasEntries: Boolean
        get() {
            val entries =
                DbQuery("SELECT idEntry FROM entry JOIN agilityClass USING (idAgilityClass) WHERE agilityClass.idCompetition=$id LIMIT 1")
            return entries.found()
        }

    val effectiveDate: Date
        get() =
            if (isToday) {
                realToday
            } else if (control.effectiveDate.isNotEmpty()) {
                control.effectiveDate
            } else {
                nextCompetitionDay
            }

    val niceName: String
        get() = "$name ($dateRange)"

    val briefNiceName: String
        get() = "$briefName ($dateRange)"

    override var briefName: String
        get() = if (super.briefName.isNotEmpty()) super.briefName else name
        set(value) {
            super.briefName = value
        }

    override var entryFee: Int
        get() = if (super.entryFee != 0) super.entryFee else 305
        set(value) {
            super.entryFee = value
        }

    override var lateEntryFee: Int
        get() = if (super.lateEntryFee != 0) super.lateEntryFee else 450
        set(value) {
            super.lateEntryFee = value
        }

    override var ifcsFee: Int
        get() = if (super.ifcsFee != 0) super.ifcsFee else 500
        set(value) {
            super.ifcsFee = value
        }

    val campingApplications: Int
        get() {
            var result = 0
            dbQuery("SELECT COUNT(*) as applications FROM camping WHERE idCompetition=$id") {
                result = getInt("applications")
            }
            return result
        }

    override var campingStart: Date
        get() = if (!hasCamping)
            nullDate
        else if (super.campingStart.isEmpty())
            campingFirst
        else
            super.campingStart
        set(value) {
            super.campingStart = value
        }

    override var campingEnd: Date
        get() = if (!hasCamping)
            nullDate
        else if (super.campingEnd.isEmpty())
            campingLast
        else
            super.campingEnd
        set(value) {
            super.campingEnd = value
        }

    val campingFirst: Date
        get() {
            var result = nullDate
            for (index in 0..campingBlocks - 1) {
                val block = getCampingBlock(index)
                if (result.isEmpty() || block.start < result) {
                    result = block.start
                }
            }
            return result
        }

    val campingLast: Date
        get() {
            var result = nullDate
            for (index in 0..campingBlocks - 1) {
                val block = getCampingBlock(index)
                if (result.isEmpty() || block.end > result) {
                    result = block.end
                }
            }
            return result
        }

    val refundClause: String
        get() {
            return when (campingRefundOption) {
                1 -> """
                    Once your camping application has been accepted, your camping fees are non-refundable and you will not 
                    be able to cancel your camping booking. You may be permitted to sell on your pitch but please check 
                    with the show secretary before doing so and let them know who will be taking your place.
                    """.trimIndent()
                2 -> """
                    Once your camping application has been accepted, you will only be allowed to cancel your booking if the 
                    show has not yet closed and there are people on the waiting list who can take your place. Otherwise your 
                    camping fees are non-refundable and you will not be able to cancel your booking. You may be permitted to 
                    sell on your pitch but please check with the show secretary before doing so and let them know who 
                    will be taking your place.
                    """.trimIndent()
                3 -> """
                    Camping fees paid will be refunded if you cancel your camping booking (or whole entry) before the show 
                    closes. Once the show closes you may be permitted to sell your space on but please check with the show 
                    secretary before doing so and let them know who will be taking your place.
                    """.trimIndent()
                else -> ""
            }
        }

    val campingTerms: String
        get() {
            return if (campingCapSystem.oneOf(CAMPING_CAP_MONITOR))
                "<b><u>All bookings are provisional</u></b> as camping is capped at ${campingPitches} (we have had ${campingApplications} applications):" +
                        "<ul><li>Places will be decided shortly after midnight " +
                        "on ${dateCloses.addDays(-7).fullDate()}.</li><li>Camping fees will be charged at " +
                        "this time of so only competitors with sufficient funds in their account " +
                        "will be considered.</li><li>We will email you a few days before to remind you to top up your " +
                        "account if necessary.</li><li>Unless stated otherwise in " +
                        "the schedule, places will be allocated on a first come first served basis.</li></ul>"
            else if (needsCampingDeposit)
                """
                    <ul>
                        <li>
                             Camping fees must be paid for at time of booking therefore if you do not have sufficient
                             funds in your account to cover the fees, your application will be put on hold until the
                             fee is received.
                        </li>
                        <li>
                            If your camping application cannot be immediately accepted, you will be placed on a waiting 
                            list and a ‘deposit’ will be deducted from your account to cover the camping costs if and 
                            when your application is confirmed.
                        </li>
                        <li>
                            Deposits will be automatically refunded if you cancel your camping application (or entry) or 
                            if your application has not been accepted by the time the show closes or we remove you from 
                            the waiting list.
                        </li>
                        <li>
                            $refundClause
                        </li>
                        <li>
                            Camping applications will be accepted according to criteria set by the show secretary, 
                            this will involve an element of ‘first come first served’, but other factors may also be 
                            taken into account (see the schedule for details). It is entirely at the discretion of the show secretary who does and does 
                            not get camping. We will work with the show secretary to minimise the time people are kept 
                            on the waiting list.
                        </li>
                    </ul>
                """.trimIndent()
            else
                ""
        }

    fun generateJudgeList() {
        val query =
            DbQuery("select classDate, group_concat(distinct judge order by judge) as list from agilityClass where idCompetition=$id group by classDate")
        judges.clear()
        while (query.next()) {
            val classDate = query.getDate("classDate")
            val list = query.getString("list")
            judges[classDate.daysSince(dateStart)].setStringArrayFromList(list)
        }
    }

    val campingText: String
        get() {
            if (hasCamping) {
                var result = ""
                val days = campingEnd.daysSince(campingStart)
                if (campingRate > 0 && campingRateDay == 0) {
                    result = campingRate.toCurrency()
                } else if (campingRate > 0 && campingRate < campingRateDay * days) {
                    result = "${campingRateDay.toCurrency()} per day, or ${campingRate.toCurrency()} for all $days days"
                } else {
                    result = "${campingRateDay.toCurrency()} per day"
                }
                if (hasManagedCamping) {
                    if (campingPitches == 0) {
                        result += ". Camping is capped"
                    } else {
                        result += ". Limited to $campingPitches pitches"
                    }
                }
                if (campingNote.isNotEmpty()) {
                    result += " ($campingNote)"
                }
                return result
            } else {
                return "No Camping Available"
            }
        }

    val entryFeeText: String
        get() {
            if (isUka) {
                return "${entryFee.toCurrency()} (${lateEntryFee.toCurrency()} Late Entries)"
            } else if (isKc && entryFeeMembers > 0) {
                return "${entryFee.toCurrency()} (${entryFeeMembers.toCurrency()} Members)"
            } else {
                return "${entryFee.toCurrency()}"
            }
        }


    val dateRange: String
        get() = if (dateEnd == dateStart) {
            dateStart.format("EEE d MMM")
        } else if (dateEnd.isSameMonth(dateStart)) {
            dateStart.format("EEE d") + " - " + dateEnd.format("EEE d MMM")
        } else {
            dateStart.format("EEE d MMM") + " - " + dateEnd.format("EEE d MMM")
        }

    val dateRangeYear: String
        get() = dateRange + " " + dateStart.format("yyyy")

    val lateEntryExpireDate: Date
        get() {
            return today.addDays(365)
        }

    val voucherCodes: String
        get() {
            val query =
                DbQuery("SELECT GROUP_CONCAT(DISTINCT voucherCode) AS list FROM voucher WHERE idCompetition=$id AND Type<>$VOUCHER_CAMPING_PERMIT").toFirst()
            return query.getString("list")
        }

    val permitCodes: String
        get() {
            val query =
                DbQuery("SELECT GROUP_CONCAT(DISTINCT voucherCode) AS list FROM voucher WHERE idCompetition=$id AND Type=$VOUCHER_CAMPING_PERMIT").toFirst()
            return query.getString("list")
        }

    val complimentaryRuns: Int
        get() {
            var query = DbQuery(
                """
                    SELECT
                        SUM(least(quantityUsed, greatest(0, quantityUsed - (paid + rep + transfers + discretionary)))) AS complimentaryRuns
                    FROM
                        (SELECT
                            idCompetitor,
                            SUM(IF(type = $ITEM_LATE_ENTRY_PAID, quantity, 0)) AS paid,
                            SUM(IF(type = $ITEM_LATE_ENTRY_DISCRETIONARY, quantity, 0)) AS discretionary,
                            SUM(IF(type = $ITEM_LATE_ENTRY_STAFF, quantity, 0)) AS staff,
                            SUM(IF(type = $ITEM_LATE_ENTRY_UKA, quantity, 0)) AS rep,
                            SUM(IF(type = $ITEM_LATE_ENTRY_TRANSFER, quantity, 0)) AS transfers,
                            SUM(quantityUsed) AS quantityUsed
                        FROM
                            competitionLedger
                        WHERE
                            idCompetition = $id
                        GROUP BY idCompetitor
                        HAVING discretionary + staff + rep > 0) AS t1
                """
            )
            query.first()
            return query.getInt("complimentaryRuns")
        }

    fun tidy() {
        var dateStart = nullDate
        var dateEnd = nullDate
        var complimentaryAllowance = 0
        var competitionDay = CompetitionDay()
        var query = DbQuery(
            """
            SELECT
                classDate,
                SUM(IF(classCode < 100, 1, 0)) AS regular,
                SUM(IF(classCode < 100, 0, 1)) AS special
            FROM
                agilityClass
            WHERE
                idCompetition = $id
            GROUP BY classDate
        """
        )
        while (query.next()) {
            competitionDay.add(id, query.getDate("classDate"))
            if (dateStart == nullDate || query.getDate("classDate") < dateStart) {
                dateStart = query.getDate("classDate")
            }
            if (query.getDate("classDate") > dateEnd) {
                dateEnd = query.getDate("classDate")
            }
            if (query.getInt("regular") > 0) {
                competitionDay.dayType = DAY_REGULAR
                complimentaryAllowance += 20
            } else if (query.getInt("special") > 0) {
                competitionDay.dayType = DAY_SPECIAL
            }
            competitionDay.post()
        }
        if (dateStart != nullDate) {
            this.dateStart = dateStart
        }
        if (dateEnd != nullDate) {
            this.dateEnd = dateEnd
        }
        this.complimentaryAllowance = complimentaryAllowance
        generateJudgeList()
        post()

        var testDate = dateStart
        while (testDate <= dateEnd) {
            if (!competitionDay.seek(id, testDate)) {
                competitionDay.add(id, testDate)
                competitionDay.dayType = DAY_REST
                competitionDay.post()
            }
            testDate = testDate.addDays(1)
        }
        if (isKc) {
            updateHeightOptions()
        }

        if (isUka) {
            fixClassNames()
        }
        checkCardSet()
    }

    fun getAccountingDateFor(date: Date): Date {
        val query =
            DbQuery("SELECT MIN(date) accountingDate FROM competitionDay WHERE idCompetition=$id AND dayType=$DAY_REGULAR AND NOT locked")
        query.first()
        val accountingDate = query.getDate("accountingDate")
        if (accountingDate == nullDate) {
            return dateEnd
        } else {
            return accountingDate
        }
    }

    fun getHeightOptions(dogHeightCode: String): List<String> {
        for (height in heightOptions.split(",")) {
            val heightCode = height.substringBefore(":")
            if (heightCode == dogHeightCode) {
                val options = height.substringAfter(":")
                return options.split("|")
            }
        }
        return ArrayList<String>()
    }

    fun updateHeightOptions() {
        val map = TreeMap<String, ArrayList<String>>()

        val agilityClass = AgilityClass()
        agilityClass.select("idCompetition=$id")
        while (agilityClass.next()) {
            val options = agilityClass.heightOptions.split(",")
            for (option in options) {
                val heightCode = option.substringBefore(":").trim()
                val jumpHeightCodes = option.substringAfter(":").split("|")
                for (jumpHeightCode in jumpHeightCodes) {
                    if (!map.containsKey(heightCode)) {
                        map[heightCode] = ArrayList<String>()
                    }
                    val jumps = map[heightCode]
                    if (jumps != null && !jumps.contains(jumpHeightCode)) {
                        jumps.add(jumpHeightCode)
                        Collections.sort(jumps, { a, b -> a.compareTo(b) })
                    }
                }
            }
        }

        var competitionHeightOptions = ""

        for (height in map) {
            val heightCode = height.key
            var jumpHeightCodes = ""
            for (jumpHeightCode in height.value) {
                jumpHeightCodes = jumpHeightCodes.append(jumpHeightCode, "|")
            }
            competitionHeightOptions = competitionHeightOptions.commaAppend("$heightCode:$jumpHeightCodes")
        }
        heightOptions = competitionHeightOptions
        post()
        println(competitionHeightOptions)
    }

    fun fixClassNames() {
        val agilityClass = AgilityClass.select("idCompetition=$id")
        while (agilityClass.next()) {
            agilityClass.name = agilityClass.shortDescription
            agilityClass.nameLong = agilityClass.description
            agilityClass.post()
        }
    }

    fun cancelEntry(idAccount: Int, reasonDeleted: Int = ENTRY_DELETED_USER) {
        dbTransaction {
            if (reasonDeleted == ENTRY_DELETED_USER) {
                PlazaMessage.entryUserCancelled(this, idAccount)
            }
            dbExecute(
                """
                INSERT INTO deletedEntry
                SELECT
                    idEntry, entry.idAccount, $id, entry.idAgilityClass, entry.idTeam, entry.gradeCode,
                    entry.heightCode, entry.jumpHeightCode, entry.entryType, entry.timeEntered, entry.entryFee,
                    $reasonDeleted, entry.dateCreated, entry.deviceCreated, entry.dateModified, entry.deviceModified
                FROM
                    entry JOIN agilityClass USING (idAgilityClass)
                WHERE
                    agilityClass.idCompetition=$id AND entry.idAccount=$idAccount

            """
            )
            dbExecute("DELETE team.* FROM entry JOIN agilityClass USING (idAgilityClass) JOIN team USING (idTeam) WHERE agilityClass.idCompetition=$id AND entry.idAccount=$idAccount AND team.idAgilityClass = entry.idAgilityClass")
            dbExecute("DELETE entry.* FROM entry JOIN agilityClass USING (idAgilityClass) WHERE agilityClass.idCompetition=$id AND entry.idAccount=$idAccount")
            dbExecute("DELETE FROM competitionDog WHERE idCompetition=$id AND idAccount=$idAccount")
            dbExecute("DELETE FROM competitionCompetitor WHERE idCompetition=$id AND idAccount=$idAccount")
            if (reasonDeleted == ENTRY_DELETED_SHOW_SECRETARY) {
                dbExecute("DELETE FROM ledger WHERE idCompetition=$id AND idAccount=$idAccount")
                dbExecute("DELETE FROM ledgerItem WHERE idCompetition=$id AND idAccount=$idAccount")
            }
            val camping = Camping()
            camping.seek(id, idAccount)
            if (camping.canCancel(this) || reasonDeleted == ENTRY_DELETED_SHOW_SECRETARY) {
                dbExecute("DELETE FROM camping WHERE idCompetition=$id AND idAccount=$idAccount")
                dbExecute("DELETE FROM ledger WHERE idCompetition=$id AND idAccount=$idAccount AND NOT type IN ($LEDGER_CAMPING_PERMIT, $LEDGER_CAMPING_FEES)")
                dbExecute("DELETE FROM ledgerItem WHERE idCompetition=$id AND idAccount=$idAccount AND NOT type IN ($LEDGER_ITEM_CAMPING_PERMIT, $LEDGER_ITEM_CAMPING_CONFIRMED, $LEDGER_ITEM_CAMPING_CREDIT)")
            } else {
                camping.cancel()
                dbExecute("DELETE FROM ledger WHERE idCompetition=$id AND idAccount=$idAccount AND NOT type IN ($LEDGER_CAMPING_PERMIT, $LEDGER_CAMPING_FEES, $LEDGER_CAMPING_DEPOSIT)")
                dbExecute("DELETE FROM ledgerItem WHERE idCompetition=$id AND idAccount=$idAccount AND NOT type IN ($LEDGER_ITEM_CAMPING_PERMIT, $LEDGER_ITEM_CAMPING_CONFIRMED, $LEDGER_ITEM_CAMPING_CREDIT, $LEDGER_ITEM_CAMPING_DEPOSIT)")
            }
            if (hasManagedCamping && !closed) {
                Camping.approveWaiting(id)
            }
            Ledger.payOverdue(idAccount)
            checkCap()
        }
    }

    /*
    fun checkCompetitionDogs() {

        // add missing
        DbQuery("""
            SELECT DISTINCT
                team.idDog, entry.idAccount, entry.gradeCode, entry.heightCode, entry.jumpHeightCode, entry.dogRingNumber
            FROM
                entry
                    JOIN
                agilityClass USING (idAgilityClass)
                    JOIN
                team USING (idTeam)
                    LEFT JOIN
                competitionDog ON competitionDog.idDog = team.idDog
                    AND competitionDog.idCompetition = $id
            WHERE
                agilityClass.idCompetition = $id
                    AND competitionDog.idDog IS NULL;
        """).forEach { it ->
            val idDog = it.getInt("idDog")
            val idAccount = it.getInt("idAccount")
            val gradeCode = it.getString("gradeCode")
            val heightCode = it.getString("heightCode")
            val jumpHeightCode = it.getString("jumpHeightCode")
            val dogRingNumber = it.getInt("dogRingNumber")
            val dog = Dog(idDog)
            if (this.isKc) {
                CompetitionDog.add(this, dog, idAccount, gradeCode, heightCode, jumpHeightCode, dogRingNumber)
            } else {
                CompetitionDog.add(this, dog, idAccount)
            }
        }

        // mark potential deletions

        dbExecute("""
            UPDATE competitionDog
            SET
                flag = not nfc
            WHERE
                idCompetition = $id
        """)

        // un-mark real entries
        dbExecute("""
            UPDATE
                entry
                    JOIN
                agilityClass USING (idAgilityClass)
                    JOIN
                team USING (idTeam)
                    JOIN
                competitionDog ON competitionDog.idDog = team.idDog
                    AND competitionDog.idCompetition = $id
            SET
                competitionDog.flag = 0
            WHERE
                agilityClass.idCompetition = $id;
        """)

        // delete redundant
        dbExecute("""
            DELETE FROM competitionDog
            WHERE
                idCompetition = $id AND flag
        """)

        // Update post paid
        dbExecute("""
            UPDATE competitionDog
                    LEFT JOIN
                ledgerItem ON ledgerItem.idCompetition = competitionDog.idCompetition
                    AND ledgerItem.idAccount = competitionDog.idAccount
                    AND ledgerItem.type IN ($LEDGER_ITEM_POSTAGE, $LEDGER_ITEM_PAPER)
            SET
                postPaid = NOT ledgerItem.type IS NULL
            WHERE
                competitionDog.idCompetition = $id
        """)

    }
    */

    fun checkGradeChanges(json: JsonNode = Json.nullNode(), force: Boolean = false): Boolean {
        var ok = true
        CompetitionDog().join { dog }.where("idCompetition=$id and not nfc") {
            if (isKc) {
                val newGradeCode = dog.kcEffectiveGradeCode(dateStart)
                if (kcGradeCode != newGradeCode) {
                    if (json.isNull) {
                        processKcGradeChange(newGradeCode, force = force)
                    } else {
                        val showNode = json.addElement()
                        showNode["name"] = name
                        showNode["dateStart"] = dateStart
                        ok = processKcGradeChange(newGradeCode, showNode)
                        showNode["error"] = "Unable to change grades for this show"
                    }
                }
            }
        }
        return ok
    }

    fun populateMultipleTeams() {
        val team = Team()
        team.join(team.agilityClass)
        team.select("team.teamType=$TEAM_MULTIPLE AND team.idAgilityClass>0 AND agilityClass.idCompetition=$id")
            .forEach {
                team.code = team.agilityClass.code
                team.refreshMembers()
                team.post()
            }
    }


    fun checkSubClasses() {
        Entry().join { agilityClass }.where("agilityClass.idCompetition=$id", "entry.idAgilityClass") {
            subClass = agilityClass.chooseSubClass(gradeCode, heightCode, jumpHeightCode, subDivision)
            post()
        }
    }

    val ringPartyMap: HashMap<String, String>
        get() {
            val map = HashMap<String, String>()
            Voucher().where("idCompetition=$id AND Type=$VOUCHER_RING_PARTY") {
                map[getString("voucherCode")] = ringPartyName
            }
            return map
        }

    val campingMap: HashMap<String, String>
        get() {
            val map = HashMap<String, String>()
            Voucher().where("idCompetition=$id") {
                if (type == VOUCHER_RING_PARTY) {
                    map[getString("voucherCode")] = ringPartyName
                } else if (allCampingFree || campingPriority) {
                    map[getString("voucherCode")] = description
                }
            }
            return map
        }

    val campingVoucherMap: HashMap<String, String>
        get() {
            val map = HashMap<String, String>()
            Voucher().where("idCompetition=$id") {
                if (allCampingFree || campingPriority) {
                    var typeText = voucherToText(type)
                    if (type == VOUCHER_RING_PARTY && ringPartyName.isNotEmpty()) {
                        typeText += " (${ringPartyName})"
                    }
                    map[getString("voucherCode")] = typeText
                }
            }
            return map
        }

    val accountVoucherCodeMap: HashMap<Int, String>
        get() {
            val map = HashMap<Int, String>()

            dbQuery(
                """
                SELECT
                    competitor.idAccount, GROUP_CONCAT(voucherCode) AS voucherCodes
                FROM
                    competitionCompetitor
                        JOIN
                    competitor USING (idCompetitor)
                WHERE
                    idCompetition = $id
                        AND voucherCode <> ''
                GROUP BY
                    competitor.idAccount
            """
            )
            {
                val list = ArrayList<String>()
                for (code in getString("voucherCodes").split(",")) {
                    if (!list.contains(code)) list.add(code)
                }
                map[getInt("idAccount")] = list.asCommaList()
            }
            return map
        }

    val voucherCodeNameMap: HashMap<String, ArrayList<String>>
        get() {
            val map = HashMap<String, ArrayList<String>>()

            CompetitionCompetitor().join { competitor }.where("idCompetition=$id AND voucherCode<>''")
            {
                for (code in voucherCode.split(",")) {
                    if (!map.containsKey(code)) {
                        map[code] = ArrayList<String>()
                    }
                    val list = map[code]
                    list?.add(competitor.fullName)
                }
            }
            return map
        }

    fun getBalanceOwing(): Int {
        val data = CompetitionLedgerData(this)
        return data.balance
    }

    fun checkCardSet() {
        var rings = 0
        dbQuery("SELECT MAX(ringNumber) AS rings FROM ring WHERE idCompetition=$id") { rings = getInt("rings") }
        CardSet().seek("idCompetition=$id") {
            doNothing()
        }.otherwise {
            append()
            idCompetition = this@Competition.id
        }.eitherway {
            name = this@Competition.briefName
            // bit random below - should choose a specific official
            CompetitionOfficial().join { competitor }.join { competitor.account }
                .seek("idCompetition=${this@Competition.id}") {
                    this@eitherway.address = competitor.fullName + "\r" + competitor.account.fullAddress
                    numberRequired = rings + 2
                }
            post()
        }
    }

    fun buildCompetitionDog() {
        if (isKc) {
            CompetitionDog.buildKc(id)
        } else if (isUka) {
            CompetitionDog.buildUka(id)
        }
    }

    fun isOfficial(idCompetitor: Int): Boolean {
        return CompetitionOfficial.isOfficial(id, idCompetitor)
    }

    companion object {

        val kc2020RulesBaseDate = "2019-12-31".toDate()


        private val competition = Competition()
        var _accountingDate = nullDate
        var _accountingDateToday = nullDate

        fun select(where: String, orderBy: String = "", limit: Int = 0): Competition {
            val competition = Competition()
            competition.select(where, orderBy, limit)
            return competition
        }

        fun delete(idCompetition: Int) {
            dbQuery("SELECT COUNT(*) AS entries FROM entry JOIN agilityClass USING (idAgilityClass) WHERE idCompetition=$idCompetition") {
                val entries = getInt("entries")
                if (entries == 0) {
                    dbExecute("DELETE FROM competition WHERE idCompetition=$idCompetition")
                    dbExecute("DELETE FROM agilityClass WHERE idCompetition=$idCompetition")
                    dbExecute("DELETE FROM cardSet WHERE idCompetition=$idCompetition")
                    dbExecute("DELETE FROM competitionDay WHERE idCompetition=$idCompetition")
                    dbExecute("DELETE FROM competitionDocument WHERE idCompetition=$idCompetition")
                    dbExecute("DELETE FROM competitionOfficial WHERE idCompetition=$idCompetition")
                    dbExecute("DELETE FROM ring WHERE idCompetition=$idCompetition")
                    dbExecute("DELETE FROM voucher WHERE idCompetition=$idCompetition")

                    // these should all be empty anyway
                    dbExecute("DELETE FROM camping WHERE idCompetition=$idCompetition")
                    dbExecute("DELETE FROM competitionCompetitor WHERE idCompetition=$idCompetition")
                    dbExecute("DELETE FROM competitionDog WHERE idCompetition=$idCompetition")
                    dbExecute("DELETE FROM competitionLedger WHERE idCompetition=$idCompetition")
                    dbExecute("DELETE FROM ledger WHERE idCompetition=$idCompetition")
                    dbExecute("DELETE FROM ledgerItem WHERE idCompetition=$idCompetition")
                    dbExecute("DELETE FROM measurement WHERE idCompetition=$idCompetition")
                    dbExecute("DELETE FROM radio WHERE idCompetition=$idCompetition")
                    dbExecute("DELETE FROM replicationFault WHERE idCompetition=$idCompetition")
                    dbExecute("DELETE FROM signOn WHERE idCompetition=$idCompetition")
                    dbExecute("DELETE FROM tabletLog WHERE idCompetition=$idCompetition")
                    dbExecute("DELETE FROM webTransaction WHERE idCompetition=$idCompetition")
                }
            }
        }

        val accountingDate: Date
            get() {
                if (_accountingDate == nullDate || _accountingDateToday != today) {
                    _accountingDate = current.getAccountingDateFor(today)
                    _accountingDateToday = today
                }
                return _accountingDate
            }

        val current: Competition
            get() {
                if (competition.isOffRow) {
                    competition.find(control.idCompetition)
                }
                return competition
            }

        fun reset() {
            competition.release()
            _accountingDate = nullDate
            _accountingDateToday = nullDate
        }

        val isKc: Boolean
            get() = current.isKc

        val isFab: Boolean
            get() = current.isFab

        val isUka: Boolean
            get() = current.isUka

        val isUkOpen: Boolean
            get() = current.isUkOpen

        val isUkaStyle: Boolean
            get() = isUka || isUkOpen

        val isGrandFinals: Boolean
            get() = current.grandFinals

        val enforceMembership: Boolean
            get() = current.isUka && !current.grandFinals && !control.useOldRegistrationRule

        val hasBookingIn: Boolean
            get() = current.hasBookingIn

        val hasEgilityCodes: Boolean
            get() = current.hasEgilityCodes

        fun balanceQuery(idCompetition: Int): DbQuery {
            return DbQuery(
                """
                SELECT
                    ledger.idAccount,
                    competitor.idCompetitor,
                    competitor.givenName,
                    competitor.familyName,
                    competitor.email,
                    SUM(IF(L2.credit = $ACCOUNT_USER, L2.amount, - L2.amount)) AS balance,
                    ledger.charge,
                    ledger.amount
                FROM
                    ledger
                        LEFT JOIN
                    account USING (idAccount)
                        LEFT JOIN
                    competitor ON competitor.idCompetitor = account.idCompetitor
                        LEFT JOIN
                    ledger as L2 ON L2.idAccount = ledger.idAccount
                        AND L2.idAccount = ledger.idAccount
                        AND (L2.debit = $ACCOUNT_USER
                        OR L2.credit = $ACCOUNT_USER)
                WHERE
                    ledger.idCompetition = $idCompetition and ledger.type=$LEDGER_ENTRY_FEES and ledger.amount < ledger.charge
                GROUP BY ledger.idAccount
                ORDER BY givenName, familyName
            """
            )
        }

        fun fixUpClasses() {

            // fill in gaps in ring table
            Global.connection.execute(
                """
            INSERT IGNORE INTO
                ring (idCompetition, date, ringNumber)
            SELECT DISTINCT
                idCompetition, classDate AS date, ringNumber
            FROM
                agilityclass
            WHERE
                ringNumber > 0
            GROUP BY idCompetition , classDate , ringNumber
        """
            )

            // fill add first class to ring entries
            Global.connection.execute(
                """
            UPDATE ring
                    JOIN
                (SELECT
                    idCompetition,
                        classDate AS date,
                        ringNumber,
                        idAgilityClass,
                        SUBSTRING_INDEX(heightRunningOrder, ',', 1) AS heightCode
                FROM
                    agilityClass
                WHERE
                    ringOrder = 1) AS firstClass USING (idCompetition , date , ringNumber)
            SET
                ring.idAgilityClass = firstClass.idAgilityClass,
                ring.heightCode = firstClass.heightCode
            WHERE
                ring.date > curdate()
        """
            )

            // initialize start time of first class
            Global.connection.execute(
                """
            UPDATE ring
                    JOIN
                agilityClass USING (idAgilityClass)
            SET
                startTime = DATE_ADD(classDate,
                    INTERVAL '8:30' HOUR_MINUTE)
            WHERE
                startTime = 0 AND classProgress = 0 AND classDate>0
        """
            )

        }

        fun isPaperEntry(idCompetition: Int, idAccount: Int): Boolean {
            var isPaper = false
            Ledger().seek("idAccount=$idAccount AND idCompetition=$idCompetition AND type=$LEDGER_ENTRY_FEES_PAPER") {
                isPaper = true
            }
            return isPaper

        }

    }

}

data class CompetitionLedgerPayment(val date: Date, val amount: Int)

class CompetitionLedgerData(val competition: Competition) {
    val payments = ArrayList<CompetitionLedgerPayment>()
    var cheques = 0
    var cash = 0
    var plazaEntries = 0
    var plazaCamping = 0
    var plazaPostage = 0
    var plazaCredits = 0
    var plazaRunUnits = 0
    var plazaSurcharges = 0
    var plazaExtras = 0
    var paperEntryCount = 0
    var paperEntryFees = 0
    var paperCamping = 0
    var paperAdmin = 0
    var paperAdminShow = 0
    var paperCredits = 0
    var paperRunUnits = 0
    var paperSurcharges = 0
    var paperExtras = 0
    var refunds = 0
    var credits = 0

    val paid = ArrayList<Int>()
    val paidTotal = 0
    val owed = 0
    var totalPayments = 0
    val totalRunUnits: Int
        get() = plazaRunUnits + paperRunUnits


    // swap fee caclulations
    val swapRunFees: Int
        get() = totalRunUnits * competition.processingFeeSwap
    val unitPrintCost: Int
        get() = ((competition.printCost.toDouble() / competition.printQuantity.toDouble()) + .5).toInt()
    val paperPrintCost: Int
        get() = paperEntryCount * unitPrintCost
    val swapPaperFees: Int
        get() = paperAdmin - paperPrintCost
    val swapFees: Int
        get() = swapRunFees + swapPaperFees

    init {
        dbQuery(
            """
            SELECT
                ledger.type AS ledgerType,
                ledgerItem.type AS ledgerItemType,
                SUM(ledger.amount) AS ledgerAmount,
                SUM(ledgerItem.amount) AS legerItemAmount,
                SUM(ledgerItem.runUnits) AS runUnits,
                COUNT(*) AS items
            FROM
                ledger
                    LEFT JOIN
                ledgerItem USING (idLedger)
            WHERE
                ledger.idCompetition = ${competition.id} AND ledger.amount>=ledger.charge
            GROUP BY ledger.type , ledgerItem.type
        """
        )
        {
            val ledgerType = getInt("ledgerType")
            val ledgerItemType = getInt("ledgerItemType")
            val ledgerAmount = getInt("ledgerAmount")
            val legerItemAmount = getInt("legerItemAmount")
            val runUnits = getInt("runUnits")
            val items = getInt("items")
            when (ledgerType) {
                LEDGER_PAPER_ENTRY_CHEQUE -> cheques = ledgerAmount
                LEDGER_PAPER_ENTRY_CASH -> cash = ledgerAmount
                LEDGER_ENTRY_FEES_REFUND -> refunds = ledgerAmount
                LEDGER_COMPETITION_CREDIT -> credits = ledgerAmount
                LEDGER_ENTRY_FEES, LEDGER_CAMPING_FEES, LEDGER_CAMPING_PERMIT, LEDGER_CAMPING_DEPOSIT -> {
                    when (ledgerItemType) {
                        LEDGER_ITEM_ENTRY -> {
                            plazaEntries = legerItemAmount
                            plazaRunUnits = runUnits
                        }
                        LEDGER_ITEM_CAMPING, LEDGER_ITEM_CAMPING_WITH_HOOK_UP, LEDGER_ITEM_CAMPING_CONFIRMED, LEDGER_ITEM_CAMPING_PERMIT, LEDGER_ITEM_CAMPING_CREDIT, LEDGER_ITEM_CAMPING_DEPOSIT ->
                            plazaCamping += legerItemAmount
                        LEDGER_ITEM_POSTAGE ->
                            plazaPostage = legerItemAmount
                        LEDGER_ITEM_ENTRY_CREDIT ->
                            plazaCredits = legerItemAmount
                        LEDGER_ITEM_ENTRY_SURCHARGE, LEDGER_ITEM_ENTRY_DISCOUNT ->
                            plazaSurcharges = legerItemAmount
                        else ->
                            plazaExtras += legerItemAmount
                    }

                }
                LEDGER_ENTRY_FEES_PAPER, LEDGER_CAMPING_PERMIT_PAPER -> {
                    when (ledgerItemType) {
                        LEDGER_ITEM_ENTRY -> {
                            paperEntryFees = legerItemAmount
                            paperRunUnits = runUnits
                        }
                        LEDGER_ITEM_CAMPING, LEDGER_ITEM_CAMPING_WITH_HOOK_UP, LEDGER_ITEM_CAMPING_PERMIT, LEDGER_ITEM_CAMPING_CREDIT ->
                            paperCamping += legerItemAmount
                        LEDGER_ITEM_PAPER, LEDGER_ITEM_PAPER_ADMIN -> {
                            paperEntryCount = items
                            paperAdmin = legerItemAmount
                        }
                        LEDGER_ITEM_ENTRY_CREDIT ->
                            paperCredits = legerItemAmount
                        LEDGER_ITEM_ENTRY_SURCHARGE, LEDGER_ITEM_ENTRY_DISCOUNT ->
                            paperSurcharges = legerItemAmount
                        else ->
                            paperExtras += legerItemAmount
                    }
                }
            }
        }

        Ledger().where(
            "idCompetition=${competition.id} AND " +
                    "((Type=${LEDGER_ELECTRONIC_PAYMENT} AND debit=${ACCOUNT_SHOW_HOLDING}) OR (Type=${LEDGER_ELECTRONIC_RECEIPT} AND credit=${ACCOUNT_SHOW_HOLDING}))",
            "dateEffective"
        ) {
            val netAmount = if (type == LEDGER_ELECTRONIC_PAYMENT) -amount else amount
            payments.add(CompetitionLedgerPayment(dateEffective, netAmount))
            totalPayments -= netAmount
        }

        if (competition.isFab) {
            paperAdminShow = paperEntryCount * 75
            paperAdmin = paperAdmin - paperAdminShow
        }
    }


    val plazaReceipts = plazaEntries + plazaCredits + plazaCamping + plazaPostage + plazaSurcharges + plazaExtras
    val paperReceipts =
        paperEntryFees + paperCredits + paperCamping + paperAdmin + paperAdminShow + paperSurcharges + paperExtras
    val totalReceipts = plazaReceipts + paperReceipts
    val totalCharges =
        plazaRunUnits * competition.processingFee + paperRunUnits * competition.processingFee + plazaPostage + paperAdmin
    val due = totalReceipts - totalCharges
    val balance = due - cheques - cash - totalPayments - credits - refunds

}

