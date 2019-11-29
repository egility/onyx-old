/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.library.dbobject

import org.egility.library.database.*
import org.egility.library.dbobject.Ledger.Companion.addAccountCampingTransfer
import org.egility.library.general.*
import org.egility.library.general.PlazaMessage.Companion.campingRejected
import org.egility.library.general.PlazaMessage.Companion.campingTransferredFrom
import org.egility.library.general.PlazaMessage.Companion.campingTransferredTo
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

/*
ALTER TABLE `sandstone`.`camping` 
DROP COLUMN `allocationStatus`,
DROP COLUMN `voucherCode`,
DROP COLUMN `priority`,
DROP COLUMN `wholeShow`,
DROP COLUMN `blockFee`,
DROP COLUMN `idAccountOld`,
DROP COLUMN `flag`,
DROP COLUMN `rejected`,
DROP COLUMN `pitchAllocation`,
DROP COLUMN `days`,
DROP COLUMN `category`;

ALTER TABLE `sandstone`.`camping` 
DROP COLUMN `paid`,
ADD COLUMN `priority` TINYINT(1) NOT NULL DEFAULT '0' AFTER `confirmed`;

 */

/**
 * Created by mbrickman on 19/12/16.
 */
open class CampingRaw<T : DbTable<T>>(_connection: DbConnection? = null, vararg columnNames: String) :
    DbTable<T>(_connection, "camping", *columnNames) {
    open var idCompetition: Int by DbPropertyInt("idCompetition")
    open var idAccount: Int by DbPropertyInt("idAccount")

    //open var category: Int by DbPropertyInt("category")
    //open var days: Int by DbPropertyInt("days")
    open var groupName: String by DbPropertyString("groupName")
    open var pitches: Int by DbPropertyInt("pitches")
    open var pitchType: Int by DbPropertyInt("pitchType")
    //open var pitchAllocation: String by DbPropertyString("pitchAllocation")
    open var fee: Int by DbPropertyInt("fee")
    open var deposit: Int by DbPropertyInt("deposit")
    open var voucherCredit: Int by DbPropertyInt("voucherCredit")
    open var baseDate: Date by DbPropertyDate("baseDate")
    open var dayFlags: Int by DbPropertyInt("dayFlags")
    open var pending: Boolean by DbPropertyBoolean("pending")
    open var confirmed: Boolean by DbPropertyBoolean("confirmed")
    open var rejected: Boolean by DbPropertyBoolean("rejected")
    open var priority: Boolean by DbPropertyBoolean("priority")
    open var freeCamping: Boolean by DbPropertyBoolean("freeCamping")
    open var cancelled: Boolean by DbPropertyBoolean("cancelled")
    //open var rejected: Boolean by DbPropertyBoolean("rejected")
    //open var paid: Boolean by DbPropertyBoolean("paid")
    open var paper: Boolean by DbPropertyBoolean("paper")
    open var pitchNumber: String by DbPropertyString("pitchNumber")
    open var extra: Json by DbPropertyJson("extra")
    // open var flag: Boolean by DbPropertyBoolean("flag")

    open var dateAccepted: Date by DbPropertyDate("dateAccepted")
    open var dateCreated: Date by DbPropertyDate("dateCreated")
    open var deviceCreated: Int by DbPropertyInt("deviceCreated")
    open var dateModified: Date by DbPropertyDate("dateModified")
    open var deviceModified: Int by DbPropertyInt("deviceModified")
    open var dateDeleted: Date by DbPropertyDate("dateDeleted")

    //open var wholeShow: Boolean by DbPropertyBoolean("wholeShow")
    //open var blockFee: Boolean by DbPropertyBoolean("blockFee")
    //open var priority: Boolean by DbPropertyBoolean("priority")
    // open var voucherCode: String by DbPropertyString("voucherCode")
    //open var allocationStatus: Int by DbPropertyInt("allocationStatus")

    var dates: JsonNode by DbPropertyJsonObject("extra", "dates")
    var transferredFromIdAccount: Int by DbPropertyJsonInt("extra", "transferredFrom")

    val competition: Competition by DbLink<Competition>({ Competition() })
    val account: Account by DbLink<Account>({ Account() })
}

class Camping(vararg columnNames: String) : CampingRaw<Camping>(null, *columnNames) {

    constructor(idCompetition: Int, idAccount: Int) : this() {
        seek(idCompetition, idAccount)
    }

    fun seek(idCompetition: Int, idAccount: Int): Boolean {
        return find("idCompetition = $idCompetition AND idAccount = $idAccount")
    }

    fun transferToAccount(idAccountNew: Int): Int {
        if (!confirmed) return ERROR_CAMPING_TRANSFER_NOT_CONFIRMED

        var hasConfirmedCamping = false
        var idLedgerDelete = 0
        var campingGroup = ""
        Camping().where("idCompetition=$idCompetition AND idAccount=$idAccountNew") {
            hasConfirmedCamping = confirmed
            campingGroup = groupName
            Ledger().where("idCompetition=$idCompetition AND idAccount=$idAccountNew AND type=$LEDGER_CAMPING_DEPOSIT") {
                idLedgerDelete = this.id
            }
        }
        if (hasConfirmedCamping) return ERROR_CAMPING_TRANSFER_ALREADY_HAS_CAMPING
        
        if (!confirmed) return ERROR_CAMPING_TRANSFER_NOT_CONFIRMED
        var idLedger = 0
        var feesOwing = false
        var ledgerAmount = 0
        Ledger().where("idCompetition=$idCompetition AND idAccount=$idAccount AND type=$LEDGER_CAMPING_DEPOSIT") {
            idLedger = this.id
            feesOwing = this.amountOwing>0
            ledgerAmount = this.amount
        }
        if (idLedger==0) return ERROR_CAMPING_TRANSFER_LEDGER_MISSING
        if (feesOwing) return ERROR_CAMPING_TRANSFER_NOT_NOT_PAID
        if (paper) return ERROR_CAMPING_TRANSFER_PAPER
        if (deposit==0) return ERROR_CAMPING_TRANSFER_FREE
        
        dbTransaction {
            dbExecute("DELETE FROM camping WHERE idCompetition=$idCompetition AND idAccount=$idAccountNew")
            if (idLedgerDelete>0) {
                dbExecute("DELETE FROM ledger WHERE idLedger=$idLedgerDelete")
                dbExecute("DELETE FROM ledgerItem WHERE idLedger=$idLedgerDelete")
            }
            
            dbExecute("UPDATE ledger SET idAccount=$idAccountNew WHERE idLedger=$idLedger")
            dbExecute("UPDATE ledgerItem SET idAccount=$idAccountNew WHERE idLedger=$idLedger")

            addAccountCampingTransfer(idCompetition, idAccount, idAccountNew, ledgerAmount)

            if (dayFlags == 0 && deposit > 0) {
                dbQuery("select dayFlags, count(*) AS total from camping where idCompetition=$idCompetition AND deposit=$deposit AND NOT cancelled GROUP BY dayFlags ORDER BY total DESC LIMIT 1") {
                    dayFlags = getInt("dayFlags")
                }
            }


            campingTransferredTo(idCompetition, idAccount, Account(idAccountNew).fullName)
            campingTransferredFrom(idCompetition, idAccountNew, Account(idAccount).fullName, period)
            
            transferredFromIdAccount = idAccount
            idAccount = idAccountNew
            cancelled = false
            groupName = campingGroup

            post()
        }

        return ERROR_NONE
    }

    val days: Int
        get() = dayFlags.bitCount

    val period: String
        get() {
            var first = nullDate
            var last = nullDate
            var nights = 0
            for (date in dateArray) {
                nights++
                if (first.isEmpty()) first = date
                if (date > last) last = date
            }
            val entitlement = Account.getCampingEntitlement(idCompetition, idAccount)
            val group =
                if (entitlement.isNotEmpty()) entitlement else if (groupName.isNotEmpty()) "Group: $groupName" else ""

            val arriving = first.format("EEEE")
            val leaving = last.addDays(1).format("EEEE")
            val result = "for $nights nights, arriving $arriving and leaving $leaving"
            return result
        }

    override var baseDate: Date
        get() = super.baseDate
        set(value) {
            if (value != super.baseDate && dayFlags != 0) {
                dayFlags = arrayToFlags(value, dateArray)
            }
            super.baseDate = value
        }

    fun canCancel(competition: Competition): Boolean {
        if (!competition.hasManagedCamping || !confirmed) return true
        when (competition.campingRefundOption) {
            1 -> return false
            2 -> {
                var anyOneWaiting = false
                dbQuery("SELECT true AS found FROM camping WHERE idCompetition=$idCompetition AND NOT confirmed") {
                    anyOneWaiting = true
                }
                return anyOneWaiting
            }
        }
        return true
    }

    val dateArray: ArrayList<Date>
        get() = flagsToArray(super.baseDate, dayFlags)

    fun selectCompetition(idCompetition: Int, orderBy: String = "") {
        select("idCompetition = $idCompetition", orderBy)
    }

    fun loadNode(node: JsonNode, competition: Competition) {
        /*
        val baseDate = competition.campingStart
        if (baseDate != this.baseDate) {
            this.baseDate = baseDate
            post()
        }
         */
        node.clear()
        node["groupName"] = groupName
        node["pitches"] = pitches
        node["pitchType"] = pitchType
        node["fee"] = fee
        node["deposit"] = deposit
        node["dayFlags"] = dayFlags
        node["baseDate"] = baseDate
        node["pending"] = pending && !paper
        node["confirmed"] = confirmed || paper
        node["priority"] = priority
        node["pitchNumber"] = pitchNumber
        node["cancelled"] = cancelled
        node["canNotCancel"] = !canCancel(competition)
    }

    fun book(
        paper: Boolean,
        groupName: String,
        pitches: Int,
        pitchType: Int,
        baseDate: Date,
        dayFlags: Int,
        freeCamping: Boolean,
        freeCampingMain: Boolean = false,
        feeOverride: Int = -1,
        priority: Boolean,
        accountBalance: Int,
        campingCredits: Int
    ) {
        this.paper = paper
        this.groupName = groupName
        this.pitches = pitches
        this.pitchType = pitchType
        this.baseDate = baseDate
        this.dayFlags = dayFlags
        this.priority = priority
        this.voucherCredit = campingCredits
        this.freeCamping = freeCamping
        if (dayFlags > 0) this.cancelled = false
        if (freeCamping) {
            this.fee = 0
        } else if (feeOverride >= 0) {
            this.fee = feeOverride
        } else {
            calculateFee(freeCampingMain)
        }
        this.pending = false
        if (competition.needsCampingDeposit && !paper) {
            this.deposit = when {
                freeCamping -> 0
                canCancel(competition) -> this.fee - this.voucherCredit
                else -> maxOf(this.fee - this.voucherCredit, this.deposit)
            }
        }
        if (!confirmed && !pending) {
            confirmed = if (competition.hasManagedCamping && !paper) canConfirm(idCompetition, priority) else true
        }
        if (!pending && dateAccepted.isEmpty()) dateAccepted = now
        post()
    }

    fun cancel() {
        if (!canCancel(competition) && !freeCamping) {
            dayFlags = 0
            fee = 0
            cancelled = true
            post()
            dbExecute("DELETE FROM LedgerItem WHERE idCompetition=$idCompetition and idAccount=$idAccount AND type IN ($LEDGER_ITEM_CAMPING)")
        } else {
            val reallocatePitch = competition.hasManagedCamping && confirmed
            val idCompetitionWas = idCompetition
            delete()
            if (reallocatePitch) {
                approveWaiting(idCompetitionWas)
            }
        }
    }

    fun rejectApplication() {
        if (!confirmed && !rejected) {
            dbTransaction {
                rejected = true
                deposit = 0
                post()
                dbExecute("DELETE FROM Ledger WHERE idCompetition=$idCompetition and idAccount=$idAccount AND type IN ($LEDGER_CAMPING_DEPOSIT)")
                dbExecute("DELETE FROM LedgerItem WHERE idCompetition=$idCompetition and idAccount=$idAccount AND type IN ($LEDGER_ITEM_CAMPING)")
                campingRejected(idCompetition, idAccount)
            }
        }
    }

    fun calculateFee(freeCampingMain: Boolean = false) {
        val mainCampingBlock = 0 // 1 for Tuffley
        var runningTotal = 0
        for (index in 0..competition.campingBlocks - 1) {
            val block = competition.getCampingBlock(index)
            val fee = if (index == mainCampingBlock && freeCampingMain) 0 else block.fee(dayFlags)
            runningTotal += fee
        }
        fee = runningTotal
    }

    fun calculateFee2(freeCampingMain: Boolean = false): Int {
        if (competition.isUkOpen) {
            return if (pitchType == 2) 10000 else 5000
        } else {
            val mainCampingBlock = 0 // 1 for Tuffley
            var runningTotal = 0
            for (index in 0..competition.campingBlocks - 1) {
                val block = competition.getCampingBlock(index)
                val fee = if (index == mainCampingBlock && freeCampingMain) 0 else block.fee(dayFlags)
                runningTotal += fee
            }
            return runningTotal
        }
    }

    fun describe(): String {
        var description = when (pitchType) {
            2 -> "Pitch with Hookup"
            else -> "Regular Pitch"
        }
        description += " for ${dayFlags.bitCount} days"


        val entitlement = Account.getCampingEntitlement(idCompetition, idAccount)
        val group =
            if (entitlement.isNotEmpty()) entitlement else if (groupName.isNotEmpty()) "Group: $groupName" else ""
        if (group.isNotEmpty()) {
            description += " ($group)"
        }

        if (pitchNumber.isNotEmpty()) {
            description += ". You have been allocated pitch number $pitchNumber"
        }

        if (cancelled) {
            description = "Camping cancelled, non-refundable deposit"
        }

        return "$description."
    }

    fun bookingText(html: Boolean = false): String {
        var result = ""
        var first = nullDate
        var last = nullDate
        var nights = 0
        for (date in dateArray) {
            nights++
            if (first.isEmpty()) first = date
            if (date > last) last = date
        }
        val entitlement = Account.getCampingEntitlement(idCompetition, idAccount)
        val group =
            if (entitlement.isNotEmpty()) entitlement else if (groupName.isNotEmpty()) "Group: $groupName" else ""

        result =
            "Booked for $nights nights, arriving ${first.format("EEEE")} and leaving ${last.addDays(1).format("EEEE")}"
        if (group.isNotEmpty()) result += " ($group)"
        if (pitchNumber.isNotEmpty()) {
            if (html) {
                result += ". <b>You have been allocated pitch number $pitchNumber</b>"
            } else {
                result += ". You have been allocated pitch number $pitchNumber"
            }
        }
        return "$result."
    }

    fun acceptBooking(force: Boolean) {
        if (!confirmed && (force || !pending)) {
            dbTransaction {
                confirmed = Camping.canConfirm(idCompetition, force = true)
                PlazaMessage.showCampingAccepted(idCompetition, idAccount)
            }
        }
        post()
    }

    fun depositReceived() {
        pending = false
        if (!confirmed && (priority || Camping.canConfirm(idCompetition, force = false))) {
            confirmed = true
        }
        if (dateAccepted.isEmpty()) dateAccepted = now
        post()
        if (confirmed) {
            PlazaMessage.showCampingAccepted(idCompetition, idAccount)
        } else {
            PlazaMessage.showCampingDepositReceived(idCompetition, idAccount)
        }

    }

    companion object {

        class CampingStats(var idCompetition: Int) {

            var released = 0
            var confirmed = 0
            lateinit var checked: Date

            init {
                update()
            }

            fun update() {
                Competition().seek(idCompetition) {
                    released = campingReleased
                }
                dbQuery("SELECT COUNT(*) AS confirmed FROM camping WHERE idCompetition=$idCompetition and confirmed") {
                    confirmed = getInt("confirmed")
                }
                checked = now
            }
        }

        val campingStats = HashMap<Int, CampingStats>()

        fun getStats(idCompetition: Int): CampingStats {
            val result = campingStats[idCompetition]
            return if (result == null) {
                val stats = CampingStats((idCompetition))
                campingStats.put(idCompetition, stats)
                stats
            } else {
                if (result.checked.addMinutes(60).after(now)) result.update()
                result
            }
        }

        fun canConfirm(idCompetition: Int, force: Boolean): Boolean {
            synchronized(campingStats) {
                val stats = getStats(idCompetition)
                if (stats.confirmed < stats.released || force) {
                    stats.confirmed++
                    return true
                }
            }
            return false
        }

        fun approveWaiting(idCompetition: Int) {
            synchronized(campingStats) {
                val stats = getStats(idCompetition)
                stats.update()
                val canOffer = stats.released - stats.confirmed
                if (canOffer > 0) {
                    Camping().where("idCompetition=$idCompetition AND NOT confirmed", "dateCreated", limit = canOffer) {
                        acceptBooking(force = false)
                    }
                }

            }
        }

        fun loadEmptyNode(node: JsonNode, baseDate: Date) {
            node.clear()
            node["category"] = ""
            node["groupName"] = ""
            node["pitches"] = 0
            node["pitchType"] = 0
            node["fee"] = 0
            node["dayFlags"] = 0
            node["baseDate"] = baseDate
            node["pending"] = false
            node["confirmed"] = false
            node["priority"] = false
            node["cancelled"] = false
            node["pitchNumber"] = ""
        }

        fun annotateNode(node: JsonNode, competition: Competition, idAccount: Int, checkOut: Boolean = false) {
            val category = node["category"].asInt
            val groupName = node["groupName"].asString
            val pitches = node["pitches"].asInt
            val pitchType = node["pitchType"].asInt
            val fee = node["fee"].asInt
            val dayFlags = node["dayFlags"].asInt
            val pending = node["pending"].asBoolean
            val confirmed = node["confirmed"].asBoolean
            val pitchNumber = node["pitchNumber"].asString
            val cancelled = node["cancelled"].asBoolean
            var description = when (pitchType) {
                2 -> "Pitch with Hookup"
                else -> "Regular Pitch"
            }
            description += " for ${dayFlags.bitCount} days"


            val entitlement = Account.getCampingEntitlement(competition.id, idAccount)
            val group =
                if (entitlement.isNotEmpty()) entitlement else if (groupName.isNotEmpty()) "Group: $groupName" else ""
            if (group.isNotEmpty()) {
                description += " ($group)"
            }

            if (competition.hasManagedCamping) {
                if (pending) {
                    description += " - On Hold awaiting payment"
                } else if (confirmed) {
                    description += " - Confirmed"
                } else {
                    description += if (checkOut) " - applied for" else " - on Waiting List"
                }
            }

            if (pitchNumber.isNotEmpty()) {
                description += ". You have been allocated pitch number $pitchNumber"
            }

            if (cancelled) {
                description = "Camping cancelled, non-refundable deposit"
            }

            node["description"] = description
        }

        fun blockMask(baseDate: Date, start: Date, end: Date = start): Int {
            var result = 0
            var campingDate = start
            while (campingDate <= end) {
                val bit = campingDate.daysSince(baseDate)
                result = result.setBit(bit)
                campingDate = campingDate.addDays(1)
            }
            return result
        }

        fun flagsToArray(baseDate: Date, dayFlags: Int): ArrayList<Date> {
            val result = ArrayList<Date>()
            for (bit in 0..15) {
                if (dayFlags.isBitSet(bit)) {
                    result.add(baseDate.addDays(bit))
                }
            }
            return result
        }

        fun arrayToFlags(baseDate: Date, dates: ArrayList<Date>): Int {
            var result = 0
            for (date in dates) {
                val bit = date.daysSince(baseDate)
                if (bit in 0..15) {
                    result = result.setBit(bit)
                }
            }
            return result
        }

    }


}