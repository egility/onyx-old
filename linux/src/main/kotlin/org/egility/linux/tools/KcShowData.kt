/*
 * Copyright (c) Mike Brickman 2014-2018
 */

package org.egility.linux.tools

import org.egility.library.database.DbQuery
import org.egility.library.dbobject.*
import org.egility.library.dbobject.LedgerItem
import org.egility.library.general.*

class KcShowData(val idCompetition: Int) {

    val competition = Competition(idCompetition)
    val path = Global.showDocumentPath(idCompetition, "show_data", "xls")
    val workbook = createWorkbook(path)
    var sheetIndex = 0

    var totalEntryCredits = 0

    fun test(): String {
        sheetIndex = 0
        addCamping()
        workbook.quit()

        return path
    }


    fun export(): String {
        sheetIndex = 0
        addOverview()
        addCompetitors()
        addDogs()
        addClassData()
        if (false && competition.hasCruftsTeams) {
            addTeamData()
        }
        if (competition.dateStart <= today) {
            addQualifier()
        }
        addEntries()
        addCamping()
        addNFC()
        addHepers()
        addVouchers()
        addExtras()
        addClassFees()
        if (competition.processed) addDeletedEntries()
        if (competition.closed) addFees(paper = false, unpaid = false)
        if (!competition.processed) addFees(paper = false, unpaid = true)
        addFees(paper = true)
        addCredits()
        if (competition.dateEnd < today) addRefunds()
        addAccounts()
        workbook.quit()

        return path
    }

    fun addOverview() {
        with(workbook.createSheet("Overview", sheetIndex++)) {
            setWidths(1.2, 3.0)
            addHeading(0, 0, "Basic Info")
            var row = 2
            var column = 0

            addCell(0, row, "Show"); addCell(1, row++, competition.name)
            addCell(0, row, "Prepared"); addCell(1, row++, now.fullDateTimeText)
            addCell(0, row, "Status")
            when {
                competition.processed -> addCell(1, row++, "Show processed")
                competition.closed -> addCell(1, row++, "Show closed but not processed")
                else -> addCell(1, row++, "Show not closed")
            }
            addCell(0, row, "Data")
            when {
                competition.processed -> addCell(1, row++, "Final")
                competition.closed -> addCell(1, row++, "Provisional, unpaid entries may be deleted")
                else -> addCell(1, row++, "Speculative")
            }
        }
    }

    fun addClassData() {
        with(workbook.createSheet("Class Data", sheetIndex++)) {
            var row = 0
            var column = 0
            addHeading(column++, 0, "Date", width = 0.8)
            addHeading(column++, 0, "Class", width = 3.0)
            addHeading(column++, 0, "Height", width = 0.9)
            addHeading(column++, 0, "r/o", width = 0.4)
            addHeading(column++, 0, "Handler Name", width = 1.6)
            addHeading(column++, 0, "Pet Name", width = 1.0)
            addHeading(column++, 0, "KC Name", width = 3.0)
            addHeading(column++, 0, "Grade", width = 0.6)
            addHeading(column++, 0, "KC Reg", width = 1.0)
            addHeading(column++, 0, "Handler", width = 0.7)
            addHeading(column++, 0, "Dog", width = 0.7)
            addHeading(column++, 0, "Account", width = 1.1)
            addHeading(column++, 0, "Paper", width = 0.5)

            val entry = Entry()
            entry.join(entry.team, entry.account, entry.team.dog, entry.team.competitor, entry.agilityClass)
            entry.select("agilityClass.idCompetition=$idCompetition AND entry.progress<$PROGRESS_REMOVED", "agilityClass.classDate, agilityClass.classNumber, agilityClass.classNumberSuffix, agilityClass.part, entry.runningOrder, entry.jumpHeightCode, competitor.givenName, competitor.familyName")
            entry.forEach {
                row++
                column = 0
                addCell(column++, row, entry.agilityClass.date)
                addCell(column++, row, entry.agilityClass.name)
                addCell(column++, row, entry.jumpHeightText)
                addCell(column++, row, entry.runningOrder)
                addCell(column++, row, entry.team.competitor.fullName)
                addCell(column++, row, entry.team.dog.cleanedPetName)
                addCell(column++, row, entry.team.dog.registeredName)
                addCell(column++, row, Grade.getGradeNumber(entry.gradeCode))
                addCell(column++, row, entry.team.dog.idKC)
                addCell(column++, row, entry.team.competitor.code)
                addCell(column++, row, entry.team.dog.code)
                addCell(column++, row, entry.account.code)
                addCell(column++, row, entry.type == ENTRY_PAPER)

            }
        }
    }

    fun addTeamData() {
        with(workbook.createSheet("Teams", sheetIndex++)) {
            var row = 0
            var column = 0

            addHeading(column++, row, "id", width = 0.7)
            addHeading(column++, row, "Team Name", width = 2.0)
            addHeading(column++, row, "Club Name", width = 2.0)

            for (i in 1..6) {
                addHeading(column++, row, "Dog", width = 0.7)
                addHeading(column++, row, "Handler Name", width = 1.6)
                addHeading(column++, row, "Pet Name", width = 1.0)
                addHeading(column++, row, "KC Name", width = 3.0)
            }

            addHeading(column++, row, "Handler", width = 0.7)
            addHeading(column++, row, "Account", width = 1.1)
            addHeading(column++, row, "Paper", width = 0.5)

            Entry().join { agilityClass }.join { account }.join { account.competitor }
                .where(
                    "agilityClass.classCode=${ClassTemplate.KC_CRUFTS_TEAM.code}",
                    "agilityClass.heightCode, json_extract(agilityClass.extra, '$.teamName')"
                ) {
                    row++
                    column = 0
                    addCell(column++, row, team.id)
                    addCell(column++, row, team.teamName)
                    addCell(column++, row, team.clubName)
                    for (member in team.members) {
                        addCell(column++, row, member["dogCode"].asString)
                        addCell(column++, row, member["competitorName"].asString)
                        addCell(column++, row, member["petName"].asString)
                        addCell(column++, row, member["registeredName"].asString)
                    }
                }
        }
    }

    fun addEntries() {
        with(workbook.createSheet("Entries", sheetIndex++)) {
            var row = 0
            var column = 0
            addHeading(column++, 0, "Account", width = 1.1)
            addHeading(column++, 0, "Handler", width = 0.7)
            addHeading(column++, 0, "Handler Name", width = 1.6)
            addHeading(column++, 0, "Dog", width = 0.7)
            addHeading(column++, 0, "Pet Name", width = 1.0)
            addHeading(column++, 0, "Date", width = 0.8)
            addHeading(column++, 0, "Class", width = 3.0)
            addHeading(column++, 0, "r/o", width = 0.5)
            addHeading(column++, 0, "Height", width = 0.9)
            addHeading(column++, 0, "Grade", width = 0.6)
            addHeading(column++, 0, "Fee", width = 0.6, right = true)
            addHeading(column++, 0, "Note", width = 0.75, right = true)
            addHeading(column++, 0, "Total", width = 0.6, right = true)
            addHeading(column++, 0, "Paper", width = 0.5, right = true)

            val entry = Entry()
            entry.join(entry.team, entry.account, entry.team.dog, entry.team.competitor, entry.agilityClass)
            entry.select("agilityClass.idCompetition=$idCompetition AND entry.entryType IN ($ENTRY_PAPER, $ENTRY_AGILITY_PLAZA, $ENTRY_TRANSFER)", "account.accountCode, competitor.givenName, dog.petName, agilityClass.classDate, agilityClass.classNumber, agilityClass.classNumberSuffix, agilityClass.part, entry.runningOrder, entry.jumpHeightCode")
            var accountCode = ""
            var idCompetitor = 0
            var idDog = 0
            var date = nullDate
            var accountTotal = 0

            entry.forEach {
                row++

                val note =
                    if (entry.progress >= PROGRESS_REMOVED) "Removed" else if (entry.type == ENTRY_TRANSFER) "Added" else ""

                if (entry.account.code != accountCode) {
                    if (row > 1) {
                        addCell(column++, row - 1, Money(accountTotal))
                        addCell(column++, row - 1, entry.type == ENTRY_PAPER)
                        row++
                    }
                    accountTotal = 0
                    accountCode = entry.account.code
                    idCompetitor = 0
                    idDog = 0
                    date = nullDate
                    column = 0
                    addCell(column++, row, entry.account.code)
                } else {
                    column = 1
                }
                if (entry.team.competitor.id != idCompetitor) {
                    idCompetitor = entry.team.competitor.id
                    idDog = 0
                    date = nullDate
                    addCell(column++, row, entry.team.competitor.code)
                    addCell(column++, row, entry.team.competitor.fullName)
                } else {
                    column += 2
                }
                if (entry.team.dog.id != idDog) {
                    idDog = entry.team.dog.id
                    date = nullDate
                    addCell(column++, row, entry.team.dog.code)
                    addCell(column++, row, entry.team.dog.cleanedPetName)
                } else {
                    column += 2
                }
                if (entry.agilityClass.date != date) {
                    date = entry.agilityClass.date
                    addCell(column++, row, entry.agilityClass.date)
                } else {
                    column++
                }
                addCell(column++, row, entry.agilityClass.name)
                addCell(column++, row, entry.runningOrder)
                addCell(column++, row, entry.jumpHeightText)
                addCell(column++, row, Grade.getGradeNumber(entry.gradeCode))
                addCell(column++, row, Money(entry.fee))
                addCell(column++, row, note)
                accountTotal += entry.fee

            }
            addCell(column++, row, Money(accountTotal))
            addCell(column++, row, entry.type == ENTRY_PAPER)

        }
    }

    fun addDeletedEntries() {
        val entry = DeletedEntry()
        entry.join(entry.team, entry.account, entry.team.dog, entry.team.competitor, entry.agilityClass)
        entry.select("agilityClass.idCompetition=$idCompetition AND reasonDeleted=$ENTRY_DELETED_NO_FUNDS", "account.accountCode, competitor.competitorCode, dog.petName, agilityClass.classDate, agilityClass.classCode, agilityClass.suffix, agilityClass.gradeCodes, deletedEntry.jumpHeightCode")
        if (entry.rowCount > 0) {
            with(workbook.createSheet("Deleted Entries", sheetIndex++)) {
                var row = 0
                var column = 0
                addHeading(column++, 0, "Account", width = 1.1)
                addHeading(column++, 0, "Handler", width = 0.7)
                addHeading(column++, 0, "Handler Name", width = 1.6)
                addHeading(column++, 0, "Dog", width = 0.7)
                addHeading(column++, 0, "Pet Name", width = 1.0)
                addHeading(column++, 0, "Date", width = 0.8)
                addHeading(column++, 0, "Class", width = 3.0)
                addHeading(column++, 0, "Height", width = 0.9)
                addHeading(column++, 0, "Fee", width = 1.0, right = true)
                addHeading(column++, 0, "a/c Total", width = 1.0, right = true)
                addHeading(column++, 0, "Total", width = 1.0, right = true)

                var accountCode = ""
                var idCompetitor = 0
                var idDog = 0
                var date = nullDate
                var accountTotal = 0
                var total = 0

                entry.forEach {
                    row++

                    if (entry.account.code != accountCode) {
                        if (row > 1) {
                            addCell(column++, row - 1, Money(accountTotal))
                            addCell(
                                column++, row - 1
                                , Money(total)
                            )
                            row++
                        }
                        accountTotal = 0
                        accountCode = entry.account.code
                        idCompetitor = 0
                        idDog = 0
                        date = nullDate
                        column = 0
                        addCell(column++, row, entry.account.code)
                    } else {
                        column = 1
                    }
                    if (entry.team.competitor.id != idCompetitor) {
                        idCompetitor = entry.team.competitor.id
                        idDog = 0
                        date = nullDate
                        addCell(column++, row, entry.team.competitor.code)
                        addCell(column++, row, entry.team.competitor.fullName)
                    } else {
                        column += 2
                    }
                    if (entry.team.dog.id != idDog) {
                        idDog = entry.team.dog.id
                        date = nullDate
                        addCell(column++, row, entry.team.dog.code)
                        addCell(column++, row, entry.team.dog.cleanedPetName)
                    } else {
                        column += 2
                    }
                    if (entry.agilityClass.date != date) {
                        date = entry.agilityClass.date
                        addCell(column++, row, entry.agilityClass.date)
                    } else {
                        column++
                    }
                    addCell(column++, row, entry.agilityClass.name)
                    addCell(column++, row, entry.jumpHeightText)
                    addCell(column++, row, Money(entry.entryFee))
                    accountTotal += entry.entryFee
                    total += entry.entryFee

                }
                if (entry.rowCount > 0) {
                    addCell(column++, row, Money(accountTotal))
                    addCell(column++, row, Money(total))

                }
            }
        }

    }

    fun addCamping() {
        with(workbook.createSheet("Camping", sheetIndex++)) {
            var row = 0
            var column = 0
            val managed = competition.hasManagedCamping
            val voucherCodeMap = competition.accountVoucherCodeMap

            val campingMap = competition.campingMap

            fun getCampingEntitlement(idAccount: Int): String {
                val voucherCodes = voucherCodeMap[idAccount] ?: ""
                if (voucherCodes.isNotEmpty()) {
                    for (voucherCode in voucherCodes.split(",")) {
                        if (campingMap.containsKey(voucherCode)) {
                            return campingMap[voucherCode] ?: ""
                        }
                    }
                }
                return ""
            }

            var sequence = 1

            fun addHeadings() {
                column = 0
                addHeading(column++, row, "#", width = 0.5)
                addHeading(column++, row, "Group", width = 2.0)
                addHeading(column++, row, "Account", width = 1.1)
                addHeading(column++, row, "Camper", width = 2.0)

                var campingDate = competition.campingFirst
                while (campingDate <= competition.campingLast) {
                    addHeading(column++, row, campingDate.dayDate, width = 0.5)
                    campingDate = campingDate.addDays(1)
                }
                addHeading(column++, row, "Paper", width = 0.55)
                if (managed) addHeading(column++, row, "Voucher", width = 1.0)
                if (!managed) addHeading(column++, row, "Booking", width = 1.0)
                addHeading(column++, row, "Pitch", width = 1.0)
                row++
            }

            var bookingIndex = 1

            val json = Json()
            Camping()
                .join { account }
                .join { account.competitor }
                .where("camping.idCompetition = $idCompetition", "camping.dateCreated") {
                    var camper = account.competitor.fullName
                    val node = json.addElement()
                    node["band"] = when {
                        !managed -> 1
                        confirmed -> 2
                        else -> 3
                    }
                    if (paper) {
                        node["paper"] = paper
                    }
                    node["groupName"] = groupName
                    node["accountCode"] = account.code
                    node["fullName"] = camper
                    node["dayFlags"] = dayFlags
                    node["pitchNumber"] = pitchNumber
                    node["cancelled"] = cancelled
                    node["booking"] = bookingIndex++
                    val entitlement = if (managed) getCampingEntitlement(idAccount) else ""
                    if (managed) {
                        node["entitlement"] = entitlement
                    }
                    node["group"] = if (entitlement.isNotEmpty()) 1 else if (groupName.isNotEmpty()) 2 else 3
                    node["sequence"] = if (node["band"].asInt == 3) node["booking"].asInt else 0
                }
            json.sortBy("band", "sequence", "group", "entitlement", "groupName", "fullName")

            val bandModitor = ChangeMonitor<Int>(0)
            for (node in json) {
                val band = node["band"].asInt
                val cancelled = node["cancelled"].asBoolean
                val groupName = if (cancelled) "CANCELLED" else node["groupName"].asString
                val accountCode = node["accountCode"].asString

                val fullName = if (cancelled)
                    "${node["fullName"].asString} - CANCELLED"
                else
                    node["fullName"].asString
                val dayFlags = node["dayFlags"].asInt
                val paper = node["paper"].asBoolean
                val entitlement = node["entitlement"].asString
                val booking = node["booking"].asInt
                val pitchNumber = if (cancelled) "CANCELLED" else node["pitchNumber"].asString

                if (bandModitor.hasChanged(band)) {
                    if (row > 0) row++
                    if (managed) {
                        when (band) {
                            1 -> addHeading(0, row, "Reserved Camping")
                            2 -> addHeading(0, row, "Confirmed Camping")
                            3 -> addHeading(0, row, "Waiting List")
                        }
                        row += 2
                    }
                    addHeadings()
                    sequence = 1
                }

                column = 0
                addCell(column++, row, sequence++)
                addCell(column++, row, groupName)
                addCell(column++, row, accountCode)
                addCell(column++, row, fullName)
                for (i in 0..competition.campingLast.daysSince(competition.campingFirst)) {
                    if (dayFlags.isBitSet(i)) {
                        addCell(column, row, "\u2714")
                    }
                    column++
                }
                addCell(column++, row, paper)
                if (managed) {
                    addCell(column++, row, entitlement)
                } else {
                    addCell(column++, row, booking)
                }
                addCell(column++, row, pitchNumber)
                row++
            }
        }
    }

    fun addFees(paper: Boolean, unpaid: Boolean = false) {

        val data = CompetitionLedgerData(competition)

        val ledgerItem = LedgerItem()
        ledgerItem.join(ledgerItem.ledger, ledgerItem.account, ledgerItem.account.competitor)
        if (paper) {
            ledgerItem.select("ledgerItem.idCompetition=$idCompetition AND ledger.type IN (${LEDGER_ENTRY_FEES_PAPER}, ${LEDGER_CAMPING_PERMIT_PAPER})", "account.accountCode")
        } else if (unpaid) {
            ledgerItem.select("ledgerItem.idCompetition=$idCompetition AND ledger.type IN (${LEDGER_ENTRY_FEES}, ${LEDGER_CAMPING_FEES}, ${LEDGER_CAMPING_PERMIT}, ${LEDGER_CAMPING_DEPOSIT}) AND ledger.amount<ledger.charge", "account.accountCode")
        } else {
            ledgerItem.select("ledgerItem.idCompetition=$idCompetition AND ledger.type IN (${LEDGER_ENTRY_FEES}, ${LEDGER_CAMPING_FEES}, ${LEDGER_CAMPING_PERMIT}, ${LEDGER_CAMPING_DEPOSIT}) AND ledger.amount>=ledger.charge", "account.accountCode")
        }
        if (ledgerItem.rowCount > 0) {
            with(workbook.createSheet(if (!paper && (!competition.closed || competition.processed)) "Plaza Fees" else if (paper) "Paper Fees" else if (unpaid) "Plaza Fees Unpaid" else "Plaza Fees Paid", sheetIndex++)) {

                var column = 0
                var row = 0

                var entryFees = 0
                var entryCredits = 0
                var campingFees = 0
                var postageFees = 0
                var surchargeFees = 0
                var extrasFees = 0
                var runUnits = 0
                var cheque = 0
                var cash = 0

                var entryTotal = 0
                var creditsTotal = 0
                var campingTotal = 0
                var postageTotal = 0
                var surchargeTotal = 0
                var extrasTotal = 0
                var runUnitsTotal = 0
                var chequeTotal = 0
                var cashTotal = 0

                val hasCredits = data.plazaCredits + data.paperCredits != 0
                val hasCamping = data.plazaCamping + data.paperCamping != 0
                val hasPostage = data.plazaPostage + data.paperAdmin != 0
                val hasSurcharges = data.plazaSurcharges + data.paperSurcharges > 0
                val hasDiscounts = data.plazaSurcharges + data.paperSurcharges < 0
                val hasExtras = data.plazaExtras + data.paperExtras != 0

                var code = ""

                addHeading(column++, 0, "Account", width = 1.1)
                addHeading(column++, 0, "Name", width = 2.0)
                addHeading(column++, 0, "Entries", width = 0.8, right = true)
                val surchargeColumn = column
                if (hasSurcharges) addHeading(column++, 0, "Surcharge", width = 0.8, right = true)
                if (hasDiscounts) addHeading(column++, 0, "Discounts", width = 0.8, right = true)
                val creditsColumn = column
                if (hasCredits) addHeading(column++, 0, "Credits", width = 0.8, right = true)
                val campingColumn = column
                if (hasCamping) addHeading(column++, 0, "Camping", width = 0.8, right = true)
                val postageColumn = column
                if (hasPostage) addHeading(column++, 0, "Postage", width = 0.8, right = true)
                val extrasColumn = column
                if (hasExtras) addHeading(column++, 0, "Extras", width = 0.8, right = true)
                val totalColumn = column
                addHeading(column++, 0, "Total", width = 0.8, right = true)
                if (paper) {
                    addHeading(column++, 0, "Cheque", width = 0.8, right = true)
                    addHeading(column++, 0, "Cash", width = 0.8, right = true)
                    addHeading(column++, 0, "Over", width = 0.8, right = true)

                }
                addHeading(column++, 0, "Units", width = 0.8, right = true)
                addHeading(column++, 0, "Charge", width = 0.8, right = true)


                ledgerItem.forEach {
                    if (ledgerItem.account.code != code) {
                        if (row > 0) {
                            val total =
                                entryFees + entryCredits + campingFees + postageFees + surchargeFees + extrasFees
                            column = totalColumn
                            addCell(column++, row, Money(total))
                            if (paper) {
                                addCell(column++, row, Money(cheque), WorkbookFormats.default.zeroMoneyFormat)
                                addCell(column++, row, Money(cash), WorkbookFormats.default.zeroMoneyFormat)
                                addCell(column++, row, Money(cheque + cash - total), WorkbookFormats.default.zeroMoneyFormat)
                            }
                            addCell(column++, row, runUnits, WorkbookFormats.default.intFormatRight)
                            addCell(column++, row, Money(runUnits * competition.processingFee))
                        }
                        code = ledgerItem.account.code
                        entryFees = 0
                        entryCredits = 0
                        campingFees = 0
                        postageFees = 0
                        extrasFees = 0
                        surchargeFees = 0
                        runUnits = 0
                        if (paper) {
                            cheque = ledgerItem.ledger.paperCheque
                            cash = ledgerItem.ledger.paperCash
                            chequeTotal += cheque
                            cashTotal += cash
                        }
                        row++
                        addCell(0, row, ledgerItem.account.code)
                        addCell(1, row, ledgerItem.account.competitor.fullName)
                    }
                    column = 0
                    runUnits += ledgerItem.runUnits
                    runUnitsTotal += ledgerItem.runUnits
                    if (ledgerItem.amount != 0) {
                        when (ledgerItem.type) {
                            LEDGER_ITEM_ENTRY -> {
                                entryTotal += ledgerItem.amount
                                entryFees += ledgerItem.amount
                                addCell(2, row, Money(entryFees))
                            }
                            LEDGER_ITEM_ENTRY_SURCHARGE, LEDGER_ITEM_ENTRY_DISCOUNT -> {
                                surchargeTotal += ledgerItem.amount
                                surchargeFees += ledgerItem.amount
                                addCell(surchargeColumn, row, Money(surchargeFees))
                            }
                            LEDGER_ITEM_ENTRY_CREDIT -> {
                                creditsTotal += ledgerItem.amount
                                entryCredits += ledgerItem.amount
                                addCell(creditsColumn, row, Money(entryCredits))
                            }
                            LEDGER_ITEM_CAMPING, LEDGER_ITEM_CAMPING_CREDIT, LEDGER_ITEM_CAMPING_PERMIT -> {
                                campingTotal += ledgerItem.amount
                                campingFees += ledgerItem.amount
                                addCell(campingColumn, row, Money(campingFees))
                            }
                            LEDGER_ITEM_POSTAGE, LEDGER_ITEM_PAPER, LEDGER_ITEM_PAPER_ADMIN -> {
                                postageTotal += ledgerItem.amount
                                postageFees += ledgerItem.amount
                                addCell(postageColumn, row, Money(postageFees))
                            }
                            else -> {
                                extrasTotal += ledgerItem.amount
                                extrasFees += ledgerItem.amount
                                addCell(extrasColumn, row, Money(extrasFees))
                            }
                        }
                    }

                }
                if (row > 0) {
                    val total = entryFees + entryCredits + campingFees + postageFees + surchargeFees + extrasFees
                    column = totalColumn
                    addCell(column++, row, Money(total))
                    if (paper) {
                        addCell(column++, row, Money(cheque), WorkbookFormats.default.zeroMoneyFormat)
                        addCell(column++, row, Money(cash), WorkbookFormats.default.zeroMoneyFormat)
                        addCell(column++, row, Money(cheque + cash - total), WorkbookFormats.default.zeroMoneyFormat)
                    }
                    addCell(column++, row, runUnits, WorkbookFormats.default.intFormatRight)
                    addCell(column++, row, Money(runUnits * competition.processingFee))
                }
                row += 2

                column = 1
                val allTotal = entryTotal + creditsTotal + campingTotal + postageTotal + surchargeTotal + extrasTotal
                val allOver = chequeTotal + cashTotal - allTotal
                addCell(column++, row, "TOTAL")
                addCell(column++, row, Money(entryTotal))
                if (hasSurcharges) addCell(column++, row, Money(surchargeTotal))
                if (hasDiscounts) addCell(column++, row, Money(surchargeTotal))
                if (hasCredits) addCell(column++, row, Money(creditsTotal))
                if (hasCamping) addCell(column++, row, Money(campingTotal))
                if (hasPostage) addCell(column++, row, Money(postageTotal))
                if (hasExtras) addCell(column++, row, Money(extrasTotal))
                addCell(column++, row, Money(allTotal))
                if (paper) {
                    addCell(column++, row, Money(chequeTotal))
                    addCell(column++, row, Money(cashTotal))
                    addCell(column++, row, Money(allOver))
                }
                addCell(column++, row, runUnitsTotal, WorkbookFormats.default.intFormatRight)
                addCell(column++, row, Money(runUnitsTotal * competition.processingFee))

                totalEntryCredits += creditsTotal

            }
        }
    }

    fun addClassFees() {
        with(workbook.createSheet("Class Fees", sheetIndex++)) {
            var row = 1
            var column = 0

            var total = 0
            var runs = 0

            var fee = competition.processingFee

            addHeading(column++, 0, "Date", width = 0.8)
            addHeading(column++, 0, "Class", width = 2.0)
            addHeading(column++, 0, "Entries", width = 0.6, right = true)
            addHeading(column++, 0, "Fees", width = 0.8, right = true)
            addHeading(column++, 0, "Units", width = 0.8, right = true)
            addHeading(column++, 0, "Charge", width = 0.8, right = true)
            addHeading(column++, 0, "Income", width = 0.8, right = true)

            DbQuery(
                """
                SELECT
                    agilityClass.idAgilityClass,
                    classDate,
                    classCode,
                    className,
                    agilityClass.jumpHeightCodes,
                    SUM(IF(entry.idEntry IS NULL, 0, entry.entryFee)) AS entryFees,
                    entry.jumpHeightCode,
                    SUM(IF(entry.idEntry IS NULL, 0, 1)) AS entries
                FROM
                    agilityClass
                        LEFT JOIN
                    entry USING (idAgilityClass)
                WHERE
                    idCompetition = $idCompetition  AND entry.progress<$PROGRESS_REMOVED
                GROUP BY agilityClass.idAgilityClass
                ORDER BY agilityClass.classDate, agilityClass.classNumber, agilityClass.classNumberSuffix, agilityClass.part
        """
            ).forEach { q ->
                val entryFees = q.getInt("entryFees")
                val classDate = q.getDate("classDate")
                val classCode = q.getInt("classCode")
                val className = q.getString("className")
                val entries = q.getInt("entries")
                val template = ClassTemplate.select(classCode)
                if (template.canEnterDirectly) {
                    column = 0
                    val charge = entries * template.runUnits * fee
                    addCell(column++, row, classDate)
                    addCell(column++, row, className)
                    addCell(column++, row, entries, WorkbookFormats.default.intFormatRight)
                    addCell(column++, row, Money(entryFees))
                    addCell(column++, row, entries * template.runUnits, WorkbookFormats.default.intFormatRight)
                    addCell(column++, row, Money(charge))
                    addCell(column++, row, Money(entryFees - charge))

                    total += entryFees
                    runs += entries * template.runUnits
                    row++
                }

            }
            row += 1
            addCell(1, row, "Sub Total")
            addCell(3, row, Money(total))
            row += 1
            addCell(1, row, "Voucher Credits")
            addCell(3, row, Money(totalEntryCredits))

            row += 2
            addCell(1, row, "TOTAL")
            addCell(3, row, Money(total + totalEntryCredits))
            addCell(4, row, runs, WorkbookFormats.default.intFormatRight)
            addCell(5, row, Money(runs * fee))
            addCell(6, row, Money(total + totalEntryCredits - runs * fee))

        }
    }

    fun addCompetitors() {
        val map=HashMap<Int, String>()
        dbQuery("""
            SELECT 
                team.idCompetitor, GROUP_CONCAT(DISTINCT classDate) as dates
            FROM
                entry
                    JOIN
                agilityClass USING (idAgilityClass)
                    JOIN
                team USING (idTeam)
                    JOIN
                competitor USING (idCompetitor)
            WHERE
                idCompetition = $idCompetition
            GROUP BY team.idCompetitor            
        """.trimIndent()) { 
            map.put(getInt("idCompetitor"), getString("dates"))
        }
        with(workbook.createSheet("Competitors", sheetIndex++)) {
            var row = 1
            var column = 0

            addHeading(column++, 0, "Code", width = 0.7)
            addHeading(column++, 0, "Name", width = 2.0)
            addHeading(column++, 0, "Mobile", width = 1.2)
            addHeading(column++, 0, "Phone", width = 1.2)
            addHeading(column++, 0, "Email", width = 3.0)
            addHeading(column++, 0, "Address", width = 5.0)
            addHeading(column++, 0, "DoB", width = 0.8)
            addHeading(column++, 0, "Account", width = 1.1)
            addHeading(column++, 0, "Days", width = 2.0)

            var competitorList = ""
            DbQuery(
                """
                SELECT
                    idCompetitor
                FROM
                    entry
                        JOIN
                    agilityClass USING (idAgilityClass)
                        JOIN
                    team USING (idTeam)
                WHERE
                    agilityClass.idCompetition = $idCompetition
            """
            ).forEach { competitorList = competitorList.append(it.getString("idCompetitor")) }

            val competitor = Competitor()
            competitor.account.joinToParent()
            competitor.select("competitor.idCompetitor in ($competitorList)", "competitorCode, givenName, familyName")
            competitor.forEach {
                val dates = map.getOrDefault(competitor.id, "")
                var datesText = ""
                for (date in dates.split(",")) {
                    datesText = datesText.append(date.trim().toDate().dateDate)
                }
                column = 0
                addCell(column++, row, competitor.code)
                addCell(column++, row, competitor.fullName)
                addCell(column++, row, competitor.phoneMobile)
                addCell(column++, row, competitor.phoneOther)
                addCell(column++, row, competitor.email.toLowerCase())
                addCell(column++, row, competitor.account.fullAddress.asCommaLine)
                addCell(column++, row, competitor.dateOfBirth)
                addCell(column++, row, competitor.account.code)
                addCell(column++, row, datesText)
                row++

            }

        }
    }

    fun addDogs() {
        with(workbook.createSheet("Dogs", sheetIndex++)) {
            var row = 1
            var column = 0

            addHeading(column++, 0, "Dog", width = 0.7)
            addHeading(column++, 0, "Pet Name", width = 1.0)
            addHeading(column++, 0, "KC Reg", width = 1.0)
            addHeading(column++, 0, "KC Name", width = 3.0)
            addHeading(column++, 0, "Height", width = 0.9)
            addHeading(column++, 0, "Entered", width = 0.9)
            addHeading(column++, 0, "Grade", width = 0.6)
            addHeading(column++, 0, "Breed", width = 2.4)
            addHeading(column++, 0, "DoB", width = 0.8)
            addHeading(column++, 0, "Owner", width = 6.0)
            addHeading(column++, 0, "Handler", width = 0.7)
            addHeading(column++, 0, "Handler Name", width = 1.6)
            //addHeading(column++, 0, "Handler", width = 0.7)
            addHeading(column++, 0, "Account", width = 1.1)

            CompetitionDog().join { dog }.join { dog.owner }.join { account }.join { dog.handler }
                .where("idCompetition = $idCompetition AND competitionDog.idDog IN (${competition.dogsEnteredList})", "dog.dogCode") {
                    column = 0
                    addCell(column++, row, dog.code)
                    addCell(column++, row, dog.petName)
                    addCell(column++, row, dog.idKC)
                    addCell(column++, row, dog.registeredName)
                    addCell(column++, row, Height.getHeightName(kcHeightCode, kcHeightCode))
                    addCell(column++, row, Height.getHeightJumpName(kcJumpHeightCode, kcJumpHeightCode))
                    addCell(column++, row, Grade.getGradeNumber(kcGradeCode))
                    addCell(column++, row, Breed.getBreedName(dog.idBreed))
                    addCell(column++, row, dog.dateOfBirth)
                    addCell(column++, row, dog.ownerText)
                    addCell(column++, row, dog.handler.code)
                    addCell(column++, row, kcHandler)
                    addCell(column++, row, account.code)
                    row++

                }

        }
    }

    fun addNFC() {
        with(workbook.createSheet("NFC", sheetIndex++)) {
            var row = 1
            var column = 0
            var thisAccount = 0
            addHeading(column++, 0, "Account", width = 1.1)
            addHeading(column++, 0, "Handler Name", width = 1.6)
            addHeading(column++, 0, "Handler", width = 0.7)
            addHeading(column++, 0, "Dog", width = 0.7)
            addHeading(column++, 0, "Pet Name", width = 1.0)
            addHeading(column++, 0, "KC Reg", width = 1.0)
            addHeading(column++, 0, "KC Name", width = 3.0)

            CompetitionDog().join { dog }.join { account }.join { account.competitor }
                .where("idCompetition = $idCompetition AND nfc", "accountCode, dog.dogCode") {
                    column = 0
                    if (account.id != thisAccount) {
                        thisAccount = account.id
                        addCell(column++, row, account.code)
                        addCell(column++, row, account.competitor.fullName)
                        addCell(column++, row, account.competitor.code)
                    }
                    column = 3
                    addCell(column++, row, dog.code)
                    addCell(column++, row, dog.petName)
                    addCell(column++, row, dog.idKC)
                    addCell(column++, row, dog.registeredName)
                    row++

                }

        }
    }

    fun addHepers() {
        with(workbook.createSheet("Helpers", sheetIndex++)) {

            var row = 2
            var column = 0
            val ringPartyMap = competition.ringPartyMap
            var voucherUsed = false
            addHeading(column++, 1, "Helper", width = 1.6)
            addHeading(column++, 1, "Group", width = 2.0)

            var helpingDate = competition.dateStart
            while (helpingDate <= competition.dateEnd) {
                addHeading(column, 0, helpingDate.format("EEE"), width = 0.5)
                mergeCells(column, 0, column + 1, 0)
                addHeading(column++, 1, "am", width = 0.5)
                addHeading(column++, 1, "pm", width = 0.5)
                helpingDate = helpingDate.addDays(1)
            }
            addHeading(column++, 1, "Judge", width = 2.4)
            addHeading(column, 0, "e-gility", width = 0.5)
            mergeCells(column, 0, column + 2, 0)
            addHeading(column++, 1, "RM", width = 0.5)
            addHeading(column++, 1, "Scime", width = 0.5)
            addHeading(column++, 1, "Check", width = 0.5)

            CompetitionCompetitor().join { competitor }.where("idCompetition=$idCompetition", "givenName, familyName") {

                var group = helpGroup

                for (code in voucherCode.split(",")) {
                    if (ringPartyMap.containsKey(code)) {
                        group = "${ringPartyMap[code]} (*)"
                        voucherUsed = true
                    }
                }

                var helping = group.isNotEmpty()
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

                    column = 0
                    addCell(column++, row, competitor.fullName)
                    addCell(column++, row, group.naturalCase)
                    for (help in helpDays) {
                        if (false && help.has("date")) {
                            val dayOffset = help["date"].asDate.daysSince(competition.dateStart) * 2 + 1
                            addCell(dayOffset, row, help.has("am"))
                            addCell(dayOffset + 1, row, help.has("pm"))
                            column+=2
                        } else {
                            addCell(column++, row, help.has("am"))
                            addCell(column++, row, help.has("pm"))
                        }
                    }
                    addCell(column++, row, judges)

                    addCell(column++, row, competitor.ringManager)
                    addCell(column++, row, competitor.scrime)
                    addCell(column++, row, competitor.checkIn)
                    row++
                }

            }
            if (voucherUsed) {
                row += 2
                addCell(2, row, "(*) Ring party voucher used")
            }
        }

    }

    fun addVouchers() {
        var row = 1
        with(workbook.createSheet("Vouchers", sheetIndex++)) {
            var column = 0
            val map = competition.voucherCodeNameMap
            addHeading(column++, 0, "Voucher", width = 1.8)
            addHeading(column++, 0, "Code", width = 0.8)
            addHeading(column++, 0, "Offer", width = 1.5)
            addHeading(column++, 0, "Claimed by", width = 1.6)

            Voucher().where("idCompetition=$idCompetition AND type<>$VOUCHER_CAMPING_PERMIT", "voucher.type, voucher.voucherCode") {
                column = 0
                var description = if (ringPartyName.isNotEmpty()) ringPartyName else voucherToText(type)
                addCell(column++, row, description)
                addCell(column++, row, code)
                addCell(column++, row, specification)
                val list = map[code] ?: ArrayList<String>()
                list.sort()
                for (name in list) {
                    addCell(column, row++, name)
                }
                if (list.isEmpty()) row++
                row++

            }

        }

    }

    fun addRefunds() {
        var hasRefunds = false
        var totalFees = 0
        var totalRefunds = 0
        dbQuery("SELECT true FROM ledger WHERE idCompetition=$idCompetition AND type=$LEDGER_ENTRY_FEES_REFUND LIMIT 1") {
            hasRefunds = true
        }
        if (hasRefunds) {

            with(workbook.createSheet("Refunds", sheetIndex++)) {
                var column = 0
                var row = 1
                addHeading(column++, 0, "Account", width = 1.1)
                addHeading(column++, 0, "Name", width = 2.0)
                addHeading(column++, 0, "Entries", width = 0.8, right = true)
                addHeading(column++, 0, "Refund", width = 0.8, right = true)
                Ledger().join { account }.join { account.competitor }
                    .where("idCompetition = $idCompetition AND ledger.Type = $LEDGER_ENTRY_FEES_REFUND", "account.accountCode") {
                        column = 0
                        addCell(column++, row, account.code)
                        addCell(column++, row, account.competitor.fullName)
                        addCell(column++, row, Money(refundEntryFees))
                        addCell(column++, row++, Money(amount))
                        totalFees += refundEntryFees
                        totalRefunds += amount
                    }
                row += 2

                column = 1
                addCell(column++, row, "TOTAL")
                addCell(column++, row, Money(totalFees))
                addCell(column++, row, Money(totalRefunds))
            }
        }
    }

    fun addCredits() {
        var hasCredits = false
        var totalCredits = 0
        var totalRefunds = 0
        dbQuery("SELECT true FROM ledger WHERE idCompetition=$idCompetition AND type=$LEDGER_COMPETITION_CREDIT LIMIT 1") {
            hasCredits = true
        }
        if (hasCredits) {

            with(workbook.createSheet("Credits", sheetIndex++)) {
                var column = 0
                var row = 1
                addHeading(column++, 0, "Account", width = 1.1)
                addHeading(column++, 0, "Name", width = 2.0)
                addHeading(column++, 0, "Description", width = 2.0)
                addHeading(column++, 0, "Amount", width = 0.8, right = true)
                Ledger().join { account }.join { account.competitor }
                    .where("idCompetition = $idCompetition AND ledger.Type = $LEDGER_COMPETITION_CREDIT", "account.accountCode") {
                        column = 0
                        addCell(column++, row, account.code)
                        addCell(column++, row, account.competitor.fullName)
                        addCell(column++, row, creditDescription)
                        addCell(column++, row++, Money(amount))
                        totalCredits += amount
                    }
                row += 2

                column = 2
                addCell(column++, row, "TOTAL")
                addCell(column++, row, Money(totalCredits))
            }
        }
    }

    fun addExtras() {
        val ledgerItem = LedgerItem()
        ledgerItem.join { account }.join { account.competitor }
        ledgerItem.select("idCompetition=$idCompetition and type between $LEDGER_ITEM_EXTRA_MIN and $LEDGER_ITEM_EXTRA_MAX", "account.accountCode, ledgerItem.type")
        if (ledgerItem.rowCount > 0) {
            with(workbook.createSheet("Extras", sheetIndex++)) {
                var column = 0
                var row = 1
                var idAccount=-1
                addHeading(column++, 0, "Account", width = 1.1)
                addHeading(column++, 0, "Name", width = 2.0)
                addHeading(column++, 0, "Quantity", width = 0.8)
                addHeading(column++, 0, "Description", width = 2.0)
                addHeading(column++, 0, "Unit Price", width = 0.8, right = true)
                addHeading(column++, 0, "Total", width = 0.8, right = true)
                ledgerItem.forEach {
                    column = 0
                    addCell(column++, row, if (ledgerItem.idAccount==idAccount) "" else ledgerItem.account.code)
                    addCell(column++, row, if (ledgerItem.idAccount==idAccount) "" else ledgerItem.account.competitor.fullName)
                    addCell(column++, row, ledgerItem.quantity)
                    addCell(column++, row, ledgerItem.description)
                    addCell(column++, row, Money(ledgerItem.unitPrice))
                    addCell(column++, row, Money(ledgerItem.amount))
                    row++
                    idAccount = ledgerItem.idAccount
                }
            }
        }
    }

        fun addAccounts() {
            var row = 0
            val data = CompetitionLedgerData(competition)
            with(workbook.createSheet("Accounts", sheetIndex++)) {

                addHeading(0, row, "Plaza Entries", width = 1.2)
                addHeading(1, row, "", width = 0.9)
                addHeading(2, row++, "", width = 0.9)

                addCell(0, row, "Class Fees"); addCell(1, row++, Money(data.plazaEntries))
                if (data.plazaCredits != 0) {
                    addCell(0, row, "Voucher Credits")
                    addCell(1, row++, Money(data.plazaCredits), WorkbookFormats.default.moneyFormatBottom)
                }
                addCell(0, row, "Net Fees");
                addCell(2, row++, Money(data.plazaEntries + data.plazaCredits))
                if (data.plazaCamping != 0) {
                    addCell(0, row, "Camping")
                    addCell(2, row++, Money(data.plazaCamping))
                }
                if (data.plazaPostage != 0) {
                    addCell(0, row, "Postage")
                    addCell(2, row++, Money(data.plazaPostage))
                }
                if (data.plazaSurcharges != 0) {
                    addCell(0, row, if (data.plazaSurcharges >= 0) "Surcharges" else "Discounts")
                    addCell(2, row++, Money(data.plazaSurcharges))
                }
                if (data.plazaExtras != 0) {
                    addCell(0, row, "Extras")
                    addCell(2, row++, Money(data.plazaExtras))
                }
                addCell(0, row, "Receipts"); addCell(2, row++, Money(data.plazaReceipts), WorkbookFormats.default.moneyFormatBoth)
                row++

                addHeading(0, row++, "Paper Entries")
                addCell(0, row, "Class Fees"); addCell(1, row++, Money(data.paperEntryFees))
                if (data.paperCredits != 0) {
                    addCell(0, row, "Voucher Credits")
                    addCell(1, row++, Money(data.paperCredits), WorkbookFormats.default.moneyFormatBottom)
                }
                addCell(0, row, "Net Fees")
                addCell(2, row++, Money(data.paperEntryFees + data.paperCredits))
                if (data.paperCamping != 0) {
                    addCell(0, row, "Camping")
                    addCell(2, row++, Money(data.paperCamping))
                }
                addCell(0, row, "Admin"); addCell(2, row++, Money(data.paperAdmin))
                if (data.paperSurcharges != 0) {
                    addCell(0, row, if (data.plazaSurcharges >= 0) "Surcharges" else "Discounts")
                    addCell(2, row++, Money(data.paperSurcharges))
                }
                if (data.paperExtras != 0) {
                    addCell(0, row, "Extras")
                    addCell(2, row++, Money(data.paperExtras))
                }
                addCell(0, row, "Receipts"); addCell(2, row++, Money(data.paperReceipts), WorkbookFormats.default.moneyFormatBoth)
                row++
                addCell(0, row, "Total Receipts"); addCell(2, row++, Money(data.totalReceipts))
                row++

                addHeading(0, row++, "AP Charges")
                addCell(0, row, "Plaza Charges"); addCell(1, row++, Money(-data.plazaRunUnits * competition.processingFee))
                addCell(0, row, "Paper Charges"); addCell(1, row++, Money(-data.paperRunUnits * competition.processingFee))
                addCell(0, row, "Postage"); addCell(1, row++, Money(-data.plazaPostage))
                addCell(0, row, "Admin"); addCell(1, row++, Money(-data.paperAdmin), WorkbookFormats.default.moneyFormatBottom)
                addCell(0, row, "Total Charges"); addCell(2, row++, Money(-data.totalCharges))
                row++
                addCell(0, row, "Amount Due"); addCell(2, row++, Money(data.due), WorkbookFormats.default.moneyFormatBoth)
                row++
                if (data.cheques != 0) {
                    addCell(0, row, "Cheques"); addCell(2, row++, Money(-data.cheques))
                }
                if (data.cash != 0) {
                    addCell(0, row, "Cash"); addCell(2, row++, Money(-data.cash))
                }

                for (payment in data.payments) {
                    if (payment.amount < 0) {
                        addCell(0, row, "Paid ${payment.date.dateText}"); addCell(2, row++, Money(payment.amount))
                    } else {
                        addCell(0, row, "Returned ${payment.date.dateText}"); addCell(2, row++, Money(payment.amount))
                    }
                }

                if (data.credits != 0) {
                    addCell(0, row, "Credits"); addCell(2, row++, Money(-data.credits))
                }
                if (data.refunds != 0) {
                    addCell(0, row, "Refunds"); addCell(2, row++, Money(-data.refunds))
                }

                row++
                addCell(0, row, "Balance Owing"); addCell(2, row++, Money(data.balance), WorkbookFormats.default.moneyFormatBoth)

            }
        }

        fun addQualifier() {
            with(workbook.createSheet("Qualifiers", sheetIndex++)) {
                var row = 0
                var column = 0
                var maxPlaces = 0
                val classMonitor = ChangeMonitor<Int>(-1)
                setWidths(1.0, 2.0, 1.0, 1.0, 2.0, 1.0, 2.0, 1.5, 0.5)

                Entry().join { team }.join { team.dog }.join { team.competitor }.join { team.competitor.account }
                    .join { agilityClass }
                    .where(
                        "agilityClass.idCompetition=$idCompetition AND agilityClass.qualifier AND entry.progress=$PROGRESS_RUN AND entry.courseFaults<>100",
                        "agilityClass.classNumber, entry.place"
                    ) {
                        if (classMonitor.hasChanged(idAgilityClass)) {
                            maxPlaces = agilityClass.qualifierPlaces
                            if (row > 0) row += 2
                            addTitle(0, row, agilityClass.nameLong)
                            row += 2
                            column = 0
                            addHeading(column++, row, "Place", width = 0.5)
                            addHeading(column++, row, "Handler", width = 2.0)
                            addHeading(column++, row, "Dog", width = 3.0)
                            addHeading(column++, row, "KC Reg", width = 1.0)
                            addHeading(column++, row, "Breed", width = 3.0)
                            addHeading(column++, row, "Phone", width = 1.2)
                            addHeading(column++, row, "Email", width = 3.0)
                            addHeading(column++, row, "Address", width = 5.0)
                            addHeading(column++, row, "Code", width = 0.6)
                            row++
                        }
                        if ((maxPlaces > 0 && place <= maxPlaces) || (maxPlaces == 0 && placeFlags.isBitSet(PRIZE_ROSETTE))) {
                            column = 0
                            addCell(column++, row, place)
                            if (agilityClass.template.isYkc) {
                                addCell(column++, row, "${team.competitor.fullName} (${team.competitor.ykc})")
                            } else {
                                addCell(column++, row, team.getCompetitorName(0))
                            }
                            addCell(column++, row, team.dog.registeredName)
                            addCell(column++, row, team.dog.idKC)
                            addCell(column++, row, Breed.getBreedName(team.dog.idBreed))
                            addCell(column++, row, firstNotEmptyString(team.competitor.phoneMobile, team.competitor.phoneOther))
                            addCell(column++, row, team.competitor.email.toLowerCase())
                            addCell(column++, row, team.competitor.account.fullAddress.replace("  ", " "))
                            addCell(column++, row, team.competitor.code)
                            row++
                        }
                    }
            }
        }


    }
