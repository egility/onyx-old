/*
 * Copyright (c) Mike Brickman 2014-2018
 */

package org.egility.linux.tools

import org.egility.library.dbobject.*
import org.egility.library.dbobject.LedgerItem
import org.egility.library.general.*

class UkOpenShowData(val idCompetition: Int) {

    val competition = Competition(idCompetition)
    val path = Global.showDocumentPath(idCompetition, "show_data", "xls")
    val workbook = createWorkbook(path)
    var sheetIndex = 0

    var totalEntryCredits = 0

    fun export(): String {
        sheetIndex = 0
        addOverview()
        addEntries()
        addCompetitors()
        addCamping()
        addVideo()

        addFees(paper = false)
        addFees(paper = true)

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
            entry.select("agilityClass.idCompetition=$idCompetition", "agilityClass.classDate, agilityClass.classNumber, agilityClass.classNumberSuffix, agilityClass.part, entry.runningOrder, entry.jumpHeightCode, competitor.givenName, competitor.familyName")
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

    fun addEntries() {
        with(workbook.createSheet("Entries", sheetIndex++)) {
            var row = 0
            var column = 0
            addHeading(column++, 0, "Account", width = 1.1)
            addHeading(column++, 0, "Handler Name", width = 1.6)
            addHeading(column++, 0, "Dog", width = 0.7)
            addHeading(column++, 0, "Pet Name", width = 1.0)
            addHeading(column++, 0, "DoB", width = 0.8)
            addHeading(column++, 0, "Height", width = 0.6)
            addHeading(column++, 0, "Measured", width = 0.6)
            addHeading(column++, 0, "Nation", width = 0.8)
            addHeading(column++, 0, "Group", width = 0.6)
            addHeading(column++, 0, "Paid", width = 0.5)
            addHeading(column++, 0, "Breed", width = 2.4)

            LedgerItem().join { ledger }.join { competitionDog }.join { competitionDog.dog }.join { account }
                .where("ledgerItem.type=10 AND ledgerItem.idCompetition=$idCompetition", "account.accountCode") {
                    column = 0; row++
                    addCell(column++, row, account.code)
                    addCell(column++, row, competitionDog.ukOpenHandler)
                    addCell(column++, row, competitionDog.dog.code)
                    addCell(column++, row, competitionDog.dog.cleanedPetName)
                    addCell(column++, row, competitionDog.dog.dateOfBirth)
                    addCell(column++, row, Height.getHeightName(competitionDog.ukOpenHeightCode))
                    addCell(column++, row, competitionDog.dog.ukaMeasuredHeight)
                    addCell(column++, row, competitionDog.ukOpenNation)
                    addCell(column++, row, competitionDog.ukOpenGroup)
                    addCell(column++, row, ledger.amountOwing==0)
                    addCell(column++, row, Breed.getBreedName(competitionDog.dog.idBreed))
                }
        }
    }

    fun addCamping() {
        with(workbook.createSheet("Camping", sheetIndex++)) {
            var row = 0
            var column = 0
            addHeading(column++, 0, "Account", width = 1.1)
            addHeading(column++, 0, "Camper", width = 2.0)
            addHeading(column++, 0, "H/Up", width = 0.5)
            addHeading(column++, 0, "Mobile", width = 1.2)
            addHeading(column++, 0, "Phone", width = 1.2)
            addHeading(column++, 0, "Email", width = 3.0)

            Camping()
                .join { account }
                .join { account.competitor }
                .where("idCompetition = $idCompetition", "camping.groupName='', REPLACE(camping.groupName, ' ', ''), givenName, familyName") {
                    column = 0; row++
                    addCell(column++, row, account.code)
                    addCell(column++, row, account.competitor.fullName)
                    addCell(column++, row, pitchType == 2)
                    addCell(column++, row, account.competitor.phoneMobile)
                    addCell(column++, row, account.competitor.phoneOther)
                    addCell(column++, row, account.competitor.email.toLowerCase())


                }

        }
    }

    fun addVideo() {
        with(workbook.createSheet("Video", sheetIndex++)) {
            var row = 0
            var column = 0
            addHeading(column++, 0, "Account", width = 1.1)
            addHeading(column++, 0, "Name", width = 2.0)
            addHeading(column++, 0, "Quantity", width = 2.0)

            LedgerItem()
                .join { account }
                .join { account.competitor }
                .where("idCompetition = $idCompetition AND ledgerItem.Type=$LEDGER_ITEM_VIDEO", "account.accountCode, givenName, familyName") {
                    column = 0; row++
                    addCell(column++, row, account.code)
                    addCell(column++, row, account.competitor.fullName)
                    addCell(column++, row, quantity)
                }
        }
    }

    fun addCompetitors() {
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

            var competitorList = ""
            dbQuery(
                """
                SELECT
                    DISTINCT dog.idCompetitor
                FROM
                    competitionDog
                        JOIN
                    dog USING (idDog)
                WHERE
                    competitionDog.idCompetition = $idCompetition
            """
            ) { competitorList = competitorList.append(getString("idCompetitor")) }

            val competitor = Competitor()
            competitor.account.joinToParent()
            competitor.select("competitor.idCompetitor in ($competitorList)", "competitorCode, givenName, familyName")
            competitor.forEach {
                column = 0
                addCell(column++, row, competitor.code)
                addCell(column++, row, competitor.fullName)
                addCell(column++, row, competitor.phoneMobile)
                addCell(column++, row, competitor.phoneOther)
                addCell(column++, row, competitor.email.toLowerCase())
                addCell(column++, row, competitor.account.fullAddress.asCommaLine)
                addCell(column++, row, competitor.dateOfBirth)
                addCell(column++, row, competitor.account.code)
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
            var extrasFees = 0
            var postageFees = 0

            var paidFees = 0

            var entryTotal = 0
            var campingTotal = 0
            var extrasTotal = 0
            var postageTotal = 0

            var paidTotal = 0

            var code = ""

            setWidths(1.5, 2.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0)
            addHeading(column++, 0, "Account")
            addHeading(column++, 0, "Name")
            addHeading(column++, 0, "Entries", right = true)
            addHeading(column++, 0, "Camping", right = true)
            addHeading(column++, 0, "Extras", right = true)
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
                        addCell(6, row, Money(entryFees + campingFees + extrasFees + postageFees))
                        if (paidFees != 0) addCell(7, row, Money(paidFees))
                        if (entryFees + campingFees + extrasFees + postageFees - paidFees != 0) addCell(8, row, Money(entryFees + campingFees + extrasFees + postageFees - paidFees))
                    }
                    code = ledgerItem.account.code
                    entryFees = 0
                    campingFees = 0
                    extrasFees = 0
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
                        addCell(5, row, Money(postageFees))
                    }
                    else -> {
                        extrasTotal += ledgerItem.amount
                        extrasFees += ledgerItem.amount
                        addCell(4, row, Money(extrasFees))
                    }
                }
                paidFees += ledgerItem.amount
                paidTotal += ledgerItem.amount


            }
            addCell(6, row, Money(entryFees + campingFees + extrasFees + postageFees))
            if (paidFees != 0) addCell(7, row, Money(paidFees))
            if (entryFees + campingFees + extrasFees + postageFees - paidFees != 0) addCell(8, row, Money(entryFees + campingFees + extrasFees + postageFees - paidFees))
            row += 2

            addCell(1, row, "TOTAL")
            addCell(2, row, Money(entryTotal))
            addCell(3, row, Money(campingTotal))
            addCell(4, row, Money(extrasTotal))
            addCell(5, row, Money(postageTotal))
            addCell(6, row, Money(entryTotal + campingTotal + extrasTotal + postageTotal))
            addCell(7, row, Money(paidTotal))
            addCell(8, row, Money(entryTotal + campingTotal + extrasTotal + postageTotal - paidTotal))


            if (!paper) {
                row += 4
                addHeading(3 + 1, row, "Agility Plaza Statement")
                row += 2
                addCell(3 + 1, row, "Fees paid to Agility Plaza")
                addCell(6 + 1, row, Money(paidTotal))
                row += 2
                var transferred = 0
                Ledger.select("idCompetition=$idCompetition AND Type=${LEDGER_ELECTRONIC_PAYMENT} AND debit=${ACCOUNT_SHOW_HOLDING}")
                    .forEach {
                        addCell(3 + 1, row, "Transferred ${it.dateEffective.dateText}")
                        addCell(5 + 1, row, Money(-it.amount))
                        transferred += it.amount
                        row++
                    }
                addCell(3 + 1, row, "Total transfers to UKA")
                addCell(6 + 1, row, Money(-transferred))
                row += 2
                addCell(3 + 1, row, "Still held by Agility Plaza")
                addCell(6 + 1, row, Money(paidTotal - transferred))
            }

        }
    }


}
