/*
 * Copyright (c) Mike Brickman 2014-2018
 */

package org.egility.linux.tools

import org.egility.library.database.DbQuery
import org.egility.library.dbobject.*
import org.egility.library.dbobject.LedgerItem
import org.egility.library.general.*
import org.egility.library.general.ClassTemplate.Companion.ukaTrophyList
import java.util.*

class UkaShowData(val idCompetition: Int) {

    val competition = Competition()
    val path = Global.showDocumentPath(competition.uniqueName, "show_data", "xls")
    val workbook = createWorkbook(path)
    var sheetIndex = 0

    fun export(): String {
        competition.find(idCompetition)
        sheetIndex = 0

        addOverview()
        addClassData()
        addEntries(paper = false)
        addEntries(paper = true)
        addDeletedEntries()
        addCamping()
        addFees(paper = false)
        addFees(paper = true)
        addClassFees()
        addAwards()
        addHepers()
        addCompetitors()
        if (competition.grandFinals) addClothing() 

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
            setWidths(1.0, 2.0, 1.0, 1.0, 2.0, 1.0, 2.0, 1.5, 0.5)
            addHeading(column++, 0, "Date", width = 1.0)
            addHeading(column++, 0, "Class", width = 2.0)
            addHeading(column++, 0, "Height", width = 1.0)
            addHeading(column++, 0, "r/o", width = 1.0)
            addHeading(column++, 0, "DogID", width = 1.0)
            addHeading(column++, 0, "Dog", width = 2.0)
            addHeading(column++, 0, "HandlerID", width = 1.0)
            addHeading(column++, 0, "Handler", width = 2.0)
            addHeading(column++, 0, "Account", width = 1.5)
            addHeading(column++, 0, "Paper", width = 0.5)
            addHeading(column++, 0, "Note", width = 1.0)

            Entry().join { team }.join { account }.join { team.dog }.join { team.competitor }.join { agilityClass }
                .where(
                    "agilityClass.idCompetition=$idCompetition AND entry.entryType IN ($ENTRY_AGILITY_PLAZA, $ENTRY_PAPER)",
                    "agilityClass.classDate, agilityClass.classCode, agilityClass.suffix, agilityClass.gradeCodes, entry.jumpHeightCode, dog.idUka"
                ) {
                    row++
                    column = 0
                    addCell(column++, row, agilityClass.date)
                    addCell(column++, row, agilityClass.name)
                    addCell(column++, row, jumpHeightText)
                    addCell(column++, row, runningOrder)
                    addCell(column++, row, team.dog.idUka)
                    addCell(column++, row, team.dog.cleanedPetName)
                    addCell(column++, row, team.competitor.idUka)
                    addCell(column++, row, team.competitor.fullName)
                    addCell(column++, row, account.code)
                    addCell(column++, row, type == ENTRY_PAPER)
                    if (progress >= PROGRESS_TRANSFERRED) {
                        addCell(column++, row, "Transferred")
                    }
                }
        }
    }

    fun addEntries(paper: Boolean) {
        with(workbook.createSheet(if (paper) "Paper Entries" else "Plaza Entries", sheetIndex++)) {
            var row = 0
            var column = 0
            setWidths(1.5, 1.0, 2.0, 1.0, 2.0, 1.0, 2.0, 1.0, 1.0)
            addHeading(column++, 0, "Account")
            addHeading(column++, 0, "HandlerID")
            addHeading(column++, 0, "Handler")
            addHeading(column++, 0, "DogID")
            addHeading(column++, 0, "Dog")
            addHeading(column++, 0, "Date")
            addHeading(column++, 0, "Class")
            addHeading(column++, 0, "Height")
            addHeading(column++, 0, "Fee")
            addHeading(column++, 0, "a/c Total")
            addHeading(column++, 0, "Total")

            val entry = Entry()
            entry.join(entry.team, entry.account, entry.team.dog, entry.team.competitor, entry.agilityClass)
            entry.select(
                "agilityClass.idCompetition=$idCompetition AND entry.entryType=${if (paper) ENTRY_PAPER else ENTRY_AGILITY_PLAZA}",
                "account.accountCode, competitor.idUka, dog.idUka, agilityClass.classDate, agilityClass.classCode, agilityClass.suffix, agilityClass.gradeCodes, entry.jumpHeightCode"
            )
            var code = ""
            var handler = 0
            var dog = 0
            var date = nullDate
            var accountTotal = 0
            var total = 0

            entry.forEach {
                row++

                if (entry.account.code != code) {
                    if (row > 1) {
                        addCell(column++, row - 1, Money(accountTotal))
                        addCell(
                            column++, row - 1
                            , Money(total)
                        )
                        row++
                    }
                    accountTotal = 0
                    code = entry.account.code
                    handler = 0
                    dog = 0
                    date = nullDate
                    column = 0
                    addCell(column++, row, entry.account.code)
                } else {
                    column = 1
                }
                if (entry.team.competitor.idUka != handler) {
                    handler = entry.team.competitor.idUka
                    dog = 0
                    date = nullDate
                    addCell(column++, row, entry.team.competitor.idUka)
                    addCell(column++, row, entry.team.competitor.fullName)
                } else {
                    column += 2
                }
                if (entry.team.dog.idUka != dog) {
                    dog = entry.team.dog.idUka
                    date = nullDate
                    addCell(column++, row, entry.team.dog.idUka)
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
                addCell(column++, row, Money(entry.fee))
                accountTotal += entry.fee
                total += entry.fee

            }
            addCell(column++, row, Money(accountTotal))
            addCell(column++, row, Money(total))

        }
    }

    fun addClothing() {
        with(workbook.createSheet("Clothing", sheetIndex++)) {
            var row = 0
            var column = 0
            setWidths(1.5, 2.0, 1.2, 0.8, 0.8, 3.0, 1.2, 0.8, 0.8)
            addHeading(column++, row, "Account")
            addHeading(column++, row, "Name")
            addHeading(column++, row, "Item")
            addHeading(column++, row, "Size")
            addHeading(column++, row, "Quantity")
            column++
            addHeading(column++, row, "Item")
            addHeading(column++, row, "Size")
            addHeading(column++, row, "Quantity")

            row=1
            val accountMonitor=ChangeMonitor(-1)
            LedgerItem().join { account }.join { account.competitor }.where("ledgerItem.idCompetition=${idCompetition} AND ledgerItem.type=$LEDGER_ITEM_CLOTHING",
                "accountCode") {
                column=0
                if (accountMonitor.hasChanged(idAccount)) {
                    addCell(column++, row, account.code)
                    addCell(column++, row, account.competitor.fullName)
                } else {
                    column+=2
                }
                addCell(column++, row, description)
                addCell(column++, row, size)
                addCell(column++, row, quantity)
                row++
            }
            
            row=1
            
            dbQuery ( """
                SELECT 
                    description, size, SUM(quantity) AS quantity
                FROM
                    ledgerItem
                WHERE
                    idCompetition = 1749893004
                        AND type = 220
                GROUP BY description , FIND_IN_SET(size, 'S,M,L,XL,2XL,3XL') , size                
            """.trimIndent() ){
                column=6
                addCell(column++, row, getString("description"))
                addCell(column++, row, getString("size"))
                addCell(column++, row, getInt("quantity"))
                row++
                
            }
        }
    }

    fun addDeletedEntries() {
        with(workbook.createSheet("Deleted Entries", sheetIndex++)) {
            var row = 0
            var column = 0
            setWidths(1.5, 1.0, 2.0, 1.0, 2.0, 1.0, 2.0, 1.0, 1.0)
            addHeading(column++, 0, "Account")
            addHeading(column++, 0, "HandlerID")
            addHeading(column++, 0, "Handler")
            addHeading(column++, 0, "DogID")
            addHeading(column++, 0, "Dog")
            addHeading(column++, 0, "Date")
            addHeading(column++, 0, "Class")
            addHeading(column++, 0, "Height")
            addHeading(column++, 0, "Fee")
            addHeading(column++, 0, "a/c Total")
            addHeading(column++, 0, "Total")

            val entry = DeletedEntry()
            entry.join(entry.team, entry.account, entry.team.dog, entry.team.competitor, entry.agilityClass)
            entry.select(
                "agilityClass.idCompetition=$idCompetition AND reasonDeleted<>$ENTRY_DELETED_NO_FUNDS",
                "account.accountCode, competitor.idUka, dog.idUka, agilityClass.classDate, agilityClass.classCode, agilityClass.suffix, agilityClass.gradeCodes, deletedEntry.jumpHeightCode"
            )
            row = 0
            var code = ""
            var handler = 0
            var dog = 0
            var date = nullDate
            var accountTotal = 0
            var total = 0

            entry.forEach {
                row++

                if (entry.account.code != code) {
                    if (row > 1) {
                        addCell(column++, row - 1, Money(accountTotal))
                        addCell(
                            column++, row - 1
                            , Money(total)
                        )
                        row++
                    }
                    accountTotal = 0
                    code = entry.account.code
                    handler = 0
                    dog = 0
                    date = nullDate
                    column = 0
                    addCell(column++, row, entry.account.code)
                } else {
                    column = 1
                }
                if (entry.team.competitor.idUka != handler) {
                    handler = entry.team.competitor.idUka
                    dog = 0
                    date = nullDate
                    addCell(column++, row, entry.team.competitor.idUka)
                    addCell(column++, row, entry.team.competitor.fullName)
                } else {
                    column += 2
                }
                if (entry.team.dog.idUka != dog) {
                    dog = entry.team.dog.idUka
                    date = nullDate
                    addCell(column++, row, entry.team.dog.idUka)
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
            addCell(column++, row, Money(accountTotal))
            addCell(column++, row, Money(total))

        }

    }

    fun addCamping() {
        with(workbook.createSheet("Camping", sheetIndex++)) {
            var row = 1
            var column = 0

            setWidths(1.5, 2.0, 1.0, 1.0)
            addHeading(column++, 0, "Account")
            addHeading(column++, 0, "Camper")

            var campingDate = competition.campingFirst
            while (campingDate <= competition.campingLast) {
                addHeading(column++, 0, campingDate.dayDate, width = 0.5)
                campingDate = campingDate.addDays(1)
            }
            if (competition.campingCapSystem != CAMPING_CAP_UNCAPPED) {
                addHeading(column++, 0, "Rank")
            }
            val permitColumn = column

            var rank = 1
            val bookings = Json.nullNode()
            Camping()
                .join { account }
                .join { account.competitor }
                .where(
                    "idCompetition = $idCompetition",
                    "camping.groupName='', REPLACE(camping.groupName, ' ', ''), camping.dateCreated"
                ) {
                        val node = bookings.addElement()
                        node["accountCode"] = account.code
                        node["camper"] = account.competitor.fullName
                        node["dayFlags"] = dayFlags
                        node["rank"] = rank++
                }

            bookings.sortBy("camper")
            for (node in bookings) {
                column = 0
                addCell(column++, row, node["accountCode"].asString)
                addCell(column++, row, node["camper"].asString)
                for (i in 0..competition.campingLast.daysSince(competition.campingFirst)) {
                    if (node["dayFlags"].asInt.isBitSet(i)) {
                        addCell(column, row, "\u2714")
                    }
                    column++
                }
                if (competition.campingCapSystem != CAMPING_CAP_UNCAPPED) {
                    addCell(column++, row, node["rank"].asInt)
                }
                row++
            }
        }
    }
    
    

    fun addFees(paper: Boolean) {
        with(workbook.createSheet(if (paper) "Paper Fees" else "Plaza Fees", sheetIndex++)) {
            var column = 0
            var row = 0

            var entryFees = 0
            var campingFees = 0
            var postageFees = 0

            var paidFees = 0

            var entryTotal = 0
            var campingTotal = 0
            var postageTotal = 0

            var paidTotal = 0

            var code = ""

            setWidths(1.5, 2.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0)
            addHeading(column++, 0, "Account")
            addHeading(column++, 0, "Name")
            addHeading(column++, 0, "Entries", right = true)
            addHeading(column++, 0, "Camping", right = true)
            addHeading(column++, 0, "Postage", right = true)
            addHeading(column++, 0, "Total", right = true)
            addHeading(column++, 0, "Paid", right = true)
            addHeading(column++, 0, "Owing", right = true)

            val ledgerItem = LedgerItem()
            ledgerItem.join(ledgerItem.ledger, ledgerItem.account, ledgerItem.account.competitor)
            if (paper) {
                ledgerItem.select(
                    "ledgerItem.idCompetition=$idCompetition AND ledger.type IN (${LEDGER_ENTRY_FEES_PAPER}, ${LEDGER_CAMPING_PERMIT_PAPER})",
                    "account.accountCode"
                )
            } else {
                ledgerItem.select(
                    "ledgerItem.idCompetition=$idCompetition AND ledger.type IN (${LEDGER_ENTRY_FEES}, ${LEDGER_CAMPING_PERMIT}, ${LEDGER_CAMPING_DEPOSIT})",
                    "account.accountCode"
                )
            }
            ledgerItem.forEach {
                column = 0
                if (ledgerItem.account.code != code) {
                    if (row > 0) {
                        addCell(5, row, Money(entryFees + campingFees + postageFees))
                        if (paidFees != 0) addCell(6, row, Money(paidFees))
                        if (entryFees + campingFees + postageFees - paidFees != 0) addCell(
                            7,
                            row,
                            Money(entryFees + campingFees + postageFees - paidFees)
                        )
                    }
                    code = ledgerItem.account.code
                    entryFees = 0
                    campingFees = 0
                    postageFees = 0
                    paidFees = 0

                    row++
                    addCell(0, row, ledgerItem.account.code)
                    addCell(1, row, ledgerItem.account.competitor.fullName)
                }
                when (ledgerItem.type) {
                    LEDGER_ITEM_ENTRY, LEDGER_ITEM_ENTRY_CREDIT -> {
                        entryTotal += ledgerItem.amount
                        entryFees += ledgerItem.amount
                        addCell(2, row, Money(entryFees))
                    }
                    LEDGER_ITEM_CAMPING, LEDGER_ITEM_CAMPING_CREDIT, LEDGER_ITEM_CAMPING_PERMIT -> {
                        if (ledgerItem.amount > 0) {
                            campingTotal += ledgerItem.amount
                            campingFees += ledgerItem.amount
                            addCell(3, row, Money(campingFees))
                        }
                    }
                    LEDGER_ITEM_POSTAGE, LEDGER_ITEM_PAPER -> {
                        postageTotal += ledgerItem.amount
                        postageFees += ledgerItem.amount
                        addCell(4, row, Money(postageFees))
                    }
                }
                paidFees += ledgerItem.amount
                paidTotal += ledgerItem.amount


            }
            addCell(5, row, Money(entryFees + campingFees + postageFees))
            if (paidFees != 0) addCell(6, row, Money(paidFees))
            if (entryFees + campingFees + postageFees - paidFees != 0) addCell(
                7,
                row,
                Money(entryFees + campingFees + postageFees - paidFees)
            )
            row += 2

            addCell(1, row, "TOTAL")
            addCell(2, row, Money(entryTotal))
            addCell(3, row, Money(campingTotal))
            addCell(4, row, Money(postageTotal))
            addCell(5, row, Money(entryTotal + campingTotal + postageTotal))
            addCell(6, row, Money(paidTotal))
            addCell(7, row, Money(entryTotal + campingTotal + postageTotal - paidTotal))


            if (!paper) {
                row += 4
                addHeading(3, row, "Agility Plaza Statement")
                row += 2
                addCell(3, row, "Fees paid to Agility Plaza")
                addCell(6, row, Money(paidTotal))
                row += 2
                var transferred = 0
                Ledger.select("idCompetition=$idCompetition AND Type=${LEDGER_ELECTRONIC_PAYMENT} AND debit=${ACCOUNT_SHOW_HOLDING}")
                    .forEach {
                        addCell(3, row, "Transferred ${it.dateEffective.dateText}")
                        addCell(5, row, Money(-it.amount))
                        transferred += it.amount
                        row++
                    }
                addCell(3, row, "Total transfers to UKA")
                addCell(6, row, Money(-transferred))
                row += 2
                addCell(3, row, "Still held by Agility Plaza")
                addCell(6, row, Money(paidTotal - transferred))
            }

        }
    }

    fun addClassFees() {
        with(workbook.createSheet("Class Fees", sheetIndex++)) {
            var row = 1
            var column = 0

            var total = 0
            var runs = 0

            setWidths(1.0, 2.0, 1.0, 1.0, 1.0)
            addHeading(column++, 0, "Date")
            addHeading(column++, 0, "Class")
            addHeading(column++, 0, "Fee", right = true)
            addHeading(column++, 0, "Entries", right = true)
            addHeading(column++, 0, "Total", right = true)
            addHeading(column++, 0, "Units", right = true)

            DbQuery(
                """
                SELECT
                    agilityClass.idAgilityClass,
                    classDate,
                    classCode,
                    className,
                    agilityClass.jumpHeightCodes,
                    entry.entryFee,
                    entry.jumpHeightCode,
                    SUM(IF(entry.idEntry IS NULL, 0, 1)) AS entries
                FROM
                    agilityClass
                        LEFT JOIN
                    entry USING (idAgilityClass)
                WHERE
                    idCompetition = $idCompetition AND entry.entryType IN ($ENTRY_AGILITY_PLAZA, $ENTRY_PAPER)
                GROUP BY agilityClass.idAgilityClass
                ORDER BY classDate , classCode , suffix , agilityClass.gradeCodes
        """
            ).forEach { q ->
                val entryFee = q.getInt("entryFee")
                val classDate = q.getDate("classDate")
                val classCode = q.getInt("classCode")
                val className = q.getString("className")
                val entries = q.getInt("entries")
                val template = ClassTemplate.select(classCode)
                if (template.canEnterDirectly) {
                    column = 0
                    addCell(column++, row, classDate)
                    addCell(column++, row, className)
                    addCell(column++, row, Money(entryFee))
                    addCell(column++, row, entries, WorkbookFormats.default.intFormatRight)
                    addCell(column++, row, Money(entries * entryFee))
                    addCell(column++, row, entries * template.runUnits, WorkbookFormats.default.intFormatRight)

                    total += entries * entryFee
                    runs += entries * template.runUnits
                    row++
                }

            }
            row += 2
            addCell(1, row, "TOTAL")
            addCell(4, row, Money(total))
            addCell(5, row, runs, WorkbookFormats.default.intFormatRight)

        }
    }

    fun addAwards() {
        with(workbook.createSheet("Awards", sheetIndex++)) {
            var row = 1
            var column = 0

            var total = 0
            var runs = 0

            setWidths(1.0, 2.0)
            addHeading(column++, 0, "Date")
            addHeading(column++, 0, "Class")
            addHeading(column++, 0, "Mc", right = true)
            addHeading(column++, 0, "Ty", right = true)
            addHeading(column++, 0, "Md", right = true)
            addHeading(column++, 0, "Sd", right = true)
            addHeading(column++, 0, "Mx", right = true)

            addHeading(column++, 0, "T1", right = true)
            addHeading(column++, 0, "T2", right = true)
            addHeading(column++, 0, "T3", right = true)

            val totalTrophy = ArrayList<Int>()
            val totalPlace = ArrayList<Int>()
            totalTrophy.addAll(arrayOf(0, 0, 0))

            fun addCount(array1: ArrayList<Int>, array2: ArrayList<Int>, count: Int) {
                for (index in 0..count - 1) {
                    if (index > array1.size - 1) {
                        array1.add(0)
                    }
                    array1[index] = (array1[index] ?: 0) + 1
                    if (index > array2.size - 1) {
                        array2.add(0)
                    }
                    array2[index] = (array2[index] ?: 0) + 1
                }
            }

            DbQuery(
                """
                SELECT
                    *
                FROM
                    agilityClass
                        LEFT JOIN
                    (SELECT
                        entry.idAgilityClass,
                            SUM(IF(entry.heightCode = 'UKA200', 1, 0)) AS UKA200,
                            SUM(IF(entry.heightCode = 'UKA300', 1, 0)) AS UKA300,
                            SUM(IF(entry.heightCode = 'UKA400', 1, 0)) AS UKA400,
                            SUM(IF(entry.heightCode = 'UKA550', 1, 0)) AS UKA550,
                            SUM(IF(entry.heightCode = 'UKA650', 1, 0)) AS UKA650
                    FROM
                        entry
                    JOIN agilityClass USING (idAgilityClass)
                    WHERE
                        idCompetition = $idCompetition
                            AND entry.entryType IN ($ENTRY_AGILITY_PLAZA, $ENTRY_PAPER)
                            AND agilityClass.classCode IN (${ukaTrophyList})
                    GROUP BY entry.idAgilityClass) AS t USING (idAgilityClass)
                WHERE
                    idCompetition = $idCompetition
                        AND agilityClass.classCode IN (${ukaTrophyList})
                ORDER BY classDate , classCode , suffix , agilityClass.gradeCodes
        """
            ).forEach { q ->
                val trophy = ArrayList<Int>()
                val place = ArrayList<Int>()
                trophy.addAll(arrayOf(0, 0, 0))

                val classDate = q.getDate("classDate")
                val template = ClassTemplate.select(q.getInt("classCode"))
                val className = q.getString("className")
                val micro = q.getInt("UKA200")
                val toy = q.getInt("UKA300")
                val medium = q.getInt("UKA400")
                val standard = q.getInt("UKA550")
                val large = q.getInt("UKA650")

                addCount(trophy, totalTrophy, AgilityClass.ukaTrophies(micro))
                addCount(trophy, totalTrophy, AgilityClass.ukaTrophies(toy))
                addCount(trophy, totalTrophy, AgilityClass.ukaTrophies(medium))
                addCount(trophy, totalTrophy, AgilityClass.ukaTrophies(standard))
                addCount(trophy, totalTrophy, AgilityClass.ukaTrophies(large))

                if (template.isUkaProgression) {
                    addCount(place, totalPlace, AgilityClass.ukaRosettes(toy))
                    addCount(place, totalPlace, AgilityClass.ukaRosettes(medium))
                    addCount(place, totalPlace, AgilityClass.ukaRosettes(standard))
                    addCount(place, totalPlace, AgilityClass.ukaRosettes(large))
                }

                column = 0
                addCell(column++, row, classDate)
                addCell(column++, row, className)
                addCell(column++, row, micro, WorkbookFormats.default.intFormatRight)
                addCell(column++, row, toy, WorkbookFormats.default.intFormatRight)
                addCell(column++, row, medium, WorkbookFormats.default.intFormatRight)
                addCell(column++, row, standard, WorkbookFormats.default.intFormatRight)
                addCell(column++, row, large, WorkbookFormats.default.intFormatRight)

                for (index in 0..2) {
                    addCell(column++, row, trophy[index], WorkbookFormats.default.intFormatRight)
                }

                for (index in 0..place.size - 1) {
                    addCell(column++, row, place[index], WorkbookFormats.default.intFormatRight)
                }

                row++

            }
            row += 2

            column = 7
            for (index in 0..2) {
                addHeading(column, row - 1, "T${index + 1}", right = true)
                addCell(column++, row, totalTrophy[index], WorkbookFormats.default.intFormatRight)
            }

            for (index in 0..totalPlace.size - 1) {
                addHeading(column, row - 1, "P${index + 1}", right = true)
                addHeading(column, 0, "P${index + 1}", right = true)
                addCell(column++, row, totalPlace[index], WorkbookFormats.default.intFormatRight)
            }

            if (totalTrophy[2] == 0) {
                removeColumn(9)
            }
            if (totalTrophy[1] == 0) {
                removeColumn(8)
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

            var helpingDate = competition.dateStart
            while (helpingDate <= competition.dateEnd) {
                addHeading(column, 0, helpingDate.format("EEE"), width = 0.5)
                mergeCells(column, 0, column + 1, 0)
                addHeading(column++, 1, "am", width = 0.5)
                addHeading(column++, 1, "pm", width = 0.5)
                helpingDate = helpingDate.addDays(1)
            }
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
                for (help in helpDays) {
                    if (help.has("am") || help.has("pm")) {
                        helping = true
                    }
                }
                if (helping || voucherCode.isNotEmpty()) {
                    column = 0
                    addCell(column++, row, competitor.fullName)
                    for (help in helpDays) {
                        if (help.has("date")) {
                            val dayOffset = help["date"].asDate.daysSince(competition.dateStart) * 2 + 1
                            addCell(dayOffset, row, help.has("am"))
                            addCell(dayOffset + 1, row, help.has("pm"))
                            column+=2
                        } else {
                            addCell(column++, row, help.has("am"))
                            addCell(column++, row, help.has("pm"))
                        }
                    }
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

    fun addCompetitors() {
        with(workbook.createSheet("Competitors", sheetIndex++)) {
            var row = 1
            var column = 0

            setWidths(1.5, 2.0, 1.2, 1.2, 3.0, 5.0)
            addHeading(column++, 0, "Account")
            addHeading(column++, 0, "Name")
            addHeading(column++, 0, "Mobile")
            addHeading(column++, 0, "Phone")
            addHeading(column++, 0, "Email")
            addHeading(column++, 0, "Address")

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
            competitor.select("competitor.idCompetitor in ($competitorList)", "givenName, familyName")
            competitor.forEach {
                column = 0
                addCell(column++, row, competitor.account.code)
                addCell(column++, row, competitor.fullName)
                addCell(column++, row, competitor.phoneMobile)
                addCell(column++, row, competitor.phoneOther)
                addCell(column++, row, competitor.email.toLowerCase())
                addCell(
                    column++,
                    row,
                    competitor.account.fullAddress.asCommaLine
                )
                row++

            }

        }
    }


}