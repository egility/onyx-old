/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.library.dbobject

import org.egility.library.database.*
import org.egility.library.general.*
import org.egility.library.general.PlazaMessage.Companion.ukaRegistrationConfirmed
import org.egility.library.general.PlazaMessage.Companion.ukaRegistrationPending
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

/**
 * Created by mbrickman on 01/12/15.
 */

open class LedgerRaw<T : DbTable<T>>(_connection: DbConnection? = null, vararg columnNames: String) :
    DbTable<T>(_connection, "ledger", *columnNames) {
    open var id: Int by DbPropertyInt("idLedger")
    open var idAccount: Int by DbPropertyInt("idAccount")
    open var idCompetition: Int by DbPropertyInt("idCompetition")
    open var debit: Int by DbPropertyInt("debit")
    open var credit: Int by DbPropertyInt("credit")
    open var type: Int by DbPropertyInt("type")
    open var currency: String by DbPropertyString("currency")
    open var charge: Int by DbPropertyInt("charge") // Value due on competitor accounts
    open var amount: Int by DbPropertyInt("amount") // Value on Plaza books - ie paid up
    open var dateEffective: Date by DbPropertyDate("dateEffective")
    open var dateAvailable: Date by DbPropertyDate("dateAvailable")
    open var pending: Boolean by DbPropertyBoolean("pending")
    open var fundsCleared: Boolean by DbPropertyBoolean("fundsCleared")
    open var dueImmediately: Boolean by DbPropertyBoolean("dueImmediately")
    open var dateConfirmedEmail: Date by DbPropertyDate("dateConfirmedEmail")
    open var datePaid: Date by DbPropertyDate("datePaid")
    open var source: String by DbPropertyString("source")
    open var extra: Json by DbPropertyJson("extra")
    open var dateCreated: Date by DbPropertyDate("dateCreated")
    open var deviceCreated: Int by DbPropertyInt("deviceCreated")
    open var dateModified: Date by DbPropertyDate("dateModified")
    open var deviceModified: Int by DbPropertyInt("deviceModified")

    var card: JsonNode by DbPropertyJsonObject("extra", "card")
    var voucherCode: String by DbPropertyJsonString("extra", "voucherCode")

    var transferFromName: String by DbPropertyJsonString("extra", "transfer.from.name")
    var transferToName: String by DbPropertyJsonString("extra", "transfer.to.name")
    var transferFrom: Int by DbPropertyJsonInt("extra", "transfer.from.idAccount")
    var transferTo: Int by DbPropertyJsonInt("extra", "transfer.to.idAccount")
    
    var creditDescription: String by DbPropertyJsonString("extra", "credit.description")
    var refundEntryFees: Int by DbPropertyJsonInt("extra", "refund.entryFees")

    var paperCheque: Int by DbPropertyJsonInt("extra", "paper.cheque")
    var paperCash: Int by DbPropertyJsonInt("extra", "paper.cash")

    var manuallyAllocated: Boolean by DbPropertyJsonBoolean("extra", "manuallyAllocated")
    var beneficiary: String by DbPropertyJsonString("extra", "beneficiary")


    val account: Account by DbLink<Account>({ Account() })
    val competition: Competition by DbLink<Competition>({ Competition() })

    val debitAccount: LedgerAccount by DbLink<LedgerAccount>({ LedgerAccount() }, label = "debitAccount", keyNames = *arrayOf("debit"))
    val creditAccount: LedgerAccount by DbLink<LedgerAccount>({ LedgerAccount() }, label = "creditAccount", keyNames = *arrayOf("credit"))

}

class Ledger(vararg columnNames: String) : LedgerRaw<Ledger>(null, *columnNames) {

    var idLedgerAccount = 0
    
    val amountOwing: Int
        get() = maxOf(charge - amount, 0)

    constructor(idLedger: Int) : this() {
        find(idLedger)
    }

    var preparing = false

    fun seekEntry(idAccount: Int, idCompetition: Int, type: Int = LEDGER_ENTRY_FEES): Boolean {
        return find("idCompetition = $idCompetition AND idAccount = $idAccount AND type=$type")
    }

    val debitAmount: Int
        get() = if (isDebit) amount else 0

    val creditAmount: Int
        get() = if (isCredit) amount else 0

    val isDebit: Boolean
        get() = debit == idLedgerAccount

    val isCredit: Boolean
        get() = credit == idLedgerAccount

    val isPendingReceipt: Boolean
        get() = type == LEDGER_STRIPE_RECEIPT && dateAvailable >= today

    fun updateAmount() {
        val query = DbQuery("SELECT SUM(amount) AS amount FROM ledgerItem WHERE idLedger=$id").toFirst()
        amount = query.getInt("amount")
        post()
    }

    fun delete() {
        dbTransaction {
            dbExecute("DELETE FROM LedgerItem WHERE idLedger=$id")
            super.delete(reposition = true)
        }
    }
    
    fun updateEntries(competition: Competition) {
        if (!preparing) {
            dbExecute("DELETE FROM LedgerItem WHERE idCompetition = $idCompetition AND idAccount = $idAccount AND type = ${LEDGER_ITEM_ENTRY}")
        }
        val entries = Entry.summary(idAccount, idCompetition)
        while (entries.next()) {
            val quantity = entries.getInt("quantity")
            val entryFee = entries.getInt("entryFee")
            val member = competition.entryFeeMembers > 0 && entryFee==competition.entryFeeMembers
            val minimumFee = if (member && competition.minimumFeeMembers > 0) competition.minimumFeeMembers else competition.minimumFee
            val maximumFee = if (member && competition.maximumFeeMembers > 0) competition.maximumFeeMembers else competition.maximumFee
            
            addEntry(entries.getInt("idDog"), entries.getString("petName"), quantity, entryFee, entries.getInt("runUnits"), competition.combinedFee)

            if (minimumFee>0) {
                if (competition.minFeeAllDays || competition.duration==1) {
                    if (quantity * entryFee < minimumFee) {
                        addEntrySurchage(entries.getInt("idDog"), entries.getString("petName"), minimumFee - (quantity * entryFee))
                    }
                } else {
                    val map = HashMap<Date, Int>()
                    for (item in entries.getString("dateFee").split(",")) {
                        val date = item.substringBefore(":").toDate()
                        val fee = item.substringAfter(":").toInt()
                        map[date] = map.getOrDefault(date, 0) + fee
                    }
                    map.forEach { date, dayFee ->
                        if (dayFee < minimumFee) {
                            addEntrySurchage(entries.getInt("idDog"), entries.getString("petName"), minimumFee - dayFee, date)
                        }
                    }
                }
            }
            
            val discount = if (maximumFee>0) maximumFee - (quantity * entryFee) else 0 
            if (discount < 0 && entryFee!=0) {
                addEntryDiscount(entries.getInt("idDog"), entries.getString("petName"), discount)
            }
        }
    }

    val isPurchase: Boolean
        get() = type.oneOf(LEDGER_ENTRY_FEES, LEDGER_ENTRY_FEES_PAPER, LEDGER_CAMPING_FEES, LEDGER_CAMPING_DEPOSIT, LEDGER_UKA_REGISTRATION)

    fun addEntry(idDog: Int, petName: String, quantity: Int, fee: Int, runUnits: Int, combinedFee: Boolean=false) {
        val item = LedgerItem()
        item.append()
        item.idLedger = id
        item.idCompetition = idCompetition
        item.idAccount = idAccount
        item.idDog = idDog
        item.type = LEDGER_ITEM_ENTRY
        item.description = petName
        item.quantity = if (combinedFee) 1 else quantity
        item.unitPrice = fee
        item.runUnits = runUnits
        item.amount = item.quantity * item.unitPrice
        item.combinedFee = combinedFee
        item.post()
    }

    fun addEntrySurchage(idDog: Int, petName: String, surcharge: Int, date: Date = nullDate) {
        val item = LedgerItem()
        item.append()
        item.idLedger = id
        item.idCompetition = idCompetition
        item.idAccount = idAccount
        item.idDog = idDog
        item.type = LEDGER_ITEM_ENTRY_SURCHARGE
        
        item.description = "$petName - Minimum Fee Surcharge" + if (date.isNotEmpty()) " (${date.dayNameShort})" else ""
        item.quantity = 1
        item.unitPrice = surcharge
        item.runUnits = 0
        item.amount = surcharge
        if (date.isNotEmpty()) item.surchargeDate = date
        item.post()
    }

    fun addEntryDiscount(idDog: Int, petName: String, discount: Int) {
        val item = LedgerItem()
        item.append()
        item.idLedger = id
        item.idCompetition = idCompetition
        item.idAccount = idAccount
        item.idDog = idDog
        item.type = LEDGER_ITEM_ENTRY_DISCOUNT
        item.description = petName + " - discount"
        item.quantity = 1
        item.unitPrice = discount
        item.runUnits = 0
        item.amount = discount
        item.post()
    }

    fun addUkaMemberItem(idCompetitor: Int, type: Int, description: String, fee: Int) {
        val item = LedgerItem()
        item.append()
        item.idLedger = id
        item.idAccount = idAccount
        item.idCompetitor = idCompetitor
        item.type = type
        item.description = description
        item.quantity = 1
        item.unitPrice = fee
        item.amount = item.quantity * item.unitPrice
        item.post()
    }

    fun addUkaDogItem(idDog: Int, type: Int, description: String, ukaHeightCode: String, ukaGradeCode: String, fee: Int) {
        val item = LedgerItem()
        item.append()
        item.idLedger = id
        item.idAccount = idAccount
        item.idDog = idDog
        item.type = type
        item.description = description
        item.quantity = 1
        item.unitPrice = fee
        item.amount = item.quantity * item.unitPrice
        item.extra["ukaHeightCode"] = ukaHeightCode
        item.extra["ukaGradeCode"] = ukaGradeCode
        item.post()
    }

    fun updateCamping(updateLedger: Boolean = false) {
        val item = LedgerItem()
        if (!preparing) {
            dbExecute("DELETE FROM LedgerItem WHERE idCompetition = $idCompetition AND idAccount = $idAccount AND type = ${LEDGER_ITEM_CAMPING}")
        }
        val camping = Camping(idCompetition, idAccount)
        if (camping.found()) {
            item.append()
            item.idLedger = id
            item.idCompetition = idCompetition
            item.idAccount = idAccount
            item.type = LEDGER_ITEM_CAMPING
            item.description = "Camping"
            item.quantity = 1
            item.amount = maxOf(camping.fee, camping.deposit)
            item.unitPrice = item.amount / item.quantity
            item.post()
            if (updateLedger) charge += item.amount
        }
        if (type== LEDGER_CAMPING_DEPOSIT) {
            dateEffective = camping.dateCreated.dateOnly()
        }
    }

    fun updateNfc(updateLedger: Boolean = false) {
        val item = LedgerItem()
        if (!preparing) {
            dbExecute("DELETE FROM LedgerItem WHERE idCompetition = $idCompetition AND idAccount = $idAccount AND type = ${LEDGER_ITEM_ENTRY_NFC}")
        }
        CompetitionDog().join { dog }.where("CompetitionDog.idCompetition = $idCompetition AND CompetitionDog.idAccount = $idAccount AND nfc") {
            item.append()
            item.idLedger = id
            item.idCompetition = idCompetition
            item.idAccount = idAccount
            item.type = LEDGER_ITEM_ENTRY_NFC
            item.description = "${dog.cleanedPetName} - NFC"
            item.quantity = 1
            item.amount = 0
            item.unitPrice = 0
            item.post()
        }
    }

    fun payFromFunds(fundsAvailable: Int, allowPart: Boolean=false): Int {
        var result=fundsAvailable
        if (fundsAvailable>=amountOwing || (allowPart && amountOwing>0 && fundsAvailable>0)) {
            val thisPayment = minOf(fundsAvailable, amountOwing)
            amount += thisPayment
            result -= thisPayment
            if (amountOwing==0 && datePaid.isEmpty()) datePaid = now
        }
        post()
        return result
    }

    fun addMiscellaneous(type: Int, idDog: Int, idCompetitor: Int, fee: Int, quantity: Int=1, code: Int=0, size: String="", description: String="") {
        val item = LedgerItem()
        if (!item.find("idCompetition=$idCompetition AND idAccount=$idAccount AND idDog=$idDog AND idCompetitor=$idCompetitor AND type=$type AND code=$code AND size=${size.quoted}")) {
            item.append()
            item.idLedger = id
            item.idCompetition = idCompetition
            item.idAccount = idAccount
            item.idDog = idDog
            item.idCompetitor = idCompetitor
            item.type = type
            item.code = code
            item.size = size
        }
        item.description = if (description.isNotEmpty()) description else if (code>0) CompetitionExtra(code).description else  transactionToText(type)
        item.quantity = quantity
        item.unitPrice = fee
        item.amount = quantity * fee
        item.flag = false
        item.post()
    }

    val description: String
        get() = when (type) {
            LEDGER_STRIPE_RECEIPT -> if (card["brand"].asString.isEmpty()) {
                "Top-Up (Unknown Card)"
            } else {
                "Top-Up (${card["brand"].asString} ending ${card["last4"].asString})"
            }
            LEDGER_ENTRY_FEES -> competition.niceName
            LEDGER_ENTRY_FEES_PAPER -> "${competition.niceName} (Paper)"
            LEDGER_ENTRY_FEES_CANCELLED -> competition.niceName
            LEDGER_ENTRY_FEES_CANCELLED_PAPER -> "${competition.niceName} (Paper)"
            LEDGER_CAMPING_FEES -> "Camping ${competition.niceName}"
            LEDGER_CAMPING_PERMIT -> "Camping Permit ${competition.name})"
            LEDGER_CAMPING_PERMIT_CANCELLED -> "Camping Permit ${competition.name})"
            LEDGER_CAMPING_DEPOSIT -> "Camping ${competition.niceName}"
            LEDGER_ENTRY_FEES_REFUND -> "Cancelled Show Credit - ${competition.briefName}"
            LEDGER_STRIPE_REFUND -> "Top-up Refund"
            LEDGER_PAPER_ENTRY_CHEQUE -> "Cheque for ${competition.name}"
            LEDGER_PAPER_ENTRY_CASH -> "Cash for ${competition.name}"
            LEDGER_TRANSFER_OUT -> "Transfer to $transferToName"
            LEDGER_TRANSFER_IN -> "Transfer from $transferFromName"
            LEDGER_CAMPING_TRANSFER_OUT -> "Camping transfer to $transferToName"
            LEDGER_CAMPING_TRANSFER_IN -> "Camping transfer from $transferFromName"
            LEDGER_COMPETITION_CREDIT -> "$creditDescription - ${competition.briefName}"
            LEDGER_ELECTRONIC_DONATION -> "Donation to $beneficiary"
            else -> LedgerCode.describe(type)
        }

    fun selectAccount(idLedgerAccount: Int, where: String = "", orderBy: String = "", limit: Int = 0) {
        this.idLedgerAccount = idLedgerAccount

        select(
            if (where.isNotEmpty())
                "(debit=$idLedgerAccount OR credit=$idLedgerAccount) AND ($where)"
            else
                "debit=$idLedgerAccount OR credit=$idLedgerAccount"
            , orderBy, limit
        )
    }

    fun processUka(feesStillOwed: Boolean) {
        val items = ArrayList<String>()
        if (type.oneOf(LEDGER_UKA_REGISTRATION, LEDGER_UKA_REGISTRATION_DIRECT)) {
            dbTransaction {
                LedgerItem().where("idLedger=$id") {
                    when (type) {
                        LEDGER_ITEM_MEMBERSHIP -> {
                            items.add("$description (${amount.toCurrency()})")
                            Competitor().seek(idCompetitor) {
                                if (!feesStillOwed) {
                                    if (ukaDateConfirmed.isEmpty()) ukaDateConfirmed = today
                                    ukaMembershipExpires =
                                            if (ukaMembershipExpires > today) ukaMembershipExpires.addYears(5) else today.addYears(5)
                                    ukaMembershipType =
                                            if (type == LEDGER_UKA_REGISTRATION_DIRECT) UKA_REGISTRATION_POST else UKA_REGISTRATION_PLAZA
                                    allocateIdUka()
                                }
                                post()
                            }

                        }
                        LEDGER_ITEM_DOG_REGISTRATION -> {
                            items.add("$description (${amount.toCurrency()})")
                            Dog().seek(idDog) {
                                if (!feesStillOwed) {
                                    if (ukaDateConfirmed.isEmpty()) ukaDateConfirmed = today
                                    ukaRegistrationType =
                                            if (type == LEDGER_UKA_REGISTRATION_DIRECT) UKA_REGISTRATION_POST else UKA_REGISTRATION_PLAZA

                                    ukaHeightCode = this@where.extra["ukaHeightCode"].asString
                                    ukaEntryLevel = this@where.extra["ukaGradeCode"].asString
                                    ukaPerformanceLevel = this@where.extra["ukaGradeCode"].asString
                                    ukaSteeplechaseLevel = this@where.extra["ukaGradeCode"].asString
                                }
                                post()
                            }
                        }
                    }
                }
                if (feesStillOwed) {
                    ukaRegistrationPending(idAccount, Account(idAccount).emailList, amount, balance(idAccount), items)
                } else {
                    ukaRegistrationConfirmed(Account(idAccount).emailList, idAccount)
                    WebTransaction.deleteUka(idAccount)
                }
            }
        }
    }

    fun wipe() {
        dbTransaction {
            dbExecute("DELETE FROM ledgerItem WHERE idLedger=$id")
            delete()
        }
    }

    companion object {

        data class DebitCredit(var debit: Int = 0, var credit: Int = 0)

        fun select(where: String, orderBy: String = "", limit: Int = 0): Ledger {
            val ledger = Ledger()
            ledger.select(where, orderBy, limit)
            return ledger
        }

        fun selectAccount(idLedgerAccount: Int, where: String = "", orderBy: String = "", limit: Int = 0): Ledger {
            val ledger = Ledger()
            ledger.selectAccount(idLedgerAccount, where, orderBy, limit)
            return ledger
        }

        fun balanceAccount(idLedgerAccount: Int, where: String = "TRUE"): DebitCredit {
            val result = DebitCredit()
            DbQuery(
                """
                SELECT
                    SUM(IF(debit=$idLedgerAccount, amount, 0)) AS debit,
                    SUM(IF(credit=$idLedgerAccount, amount, 0)) AS credit
                FROM
                    ledger
                WHERE
                    (debit=$idLedgerAccount OR credit=$idLedgerAccount) AND ($where)
                """
            ).withFirst {

                result.debit = it.getInt("debit")
                result.credit = it.getInt("credit")
            }
            return result
        }

        fun balance(idAccount: Int): Int {
            val q = DbQuery(
                """
                SELECT
                    SUM(IF(credit = $ACCOUNT_USER, amount, - amount)) AS balance
                FROM
                    ledger
                WHERE
                    (debit = $ACCOUNT_USER OR credit = $ACCOUNT_USER) AND idAccount = $idAccount
            """
            ).toFirst()
            return q.getInt("balance")
        }

        fun hasFunds(idAccount: Int, amount: Int): Boolean {
            return balance(idAccount) >= amount
        }

        fun payOverdue(idAccount: Int) {
            Ledger().where(
                "amount<charge AND (dueImmediately OR dateEffective < CURDATE()) AND debit = $ACCOUNT_USER AND " +
                        "idAccount = $idAccount", "dueImmediately DESC, dateEffective"
            ) {
                val balance = balance(idAccount)
                if (balance>0) {
                    dbTransaction {
                        payFromFunds(balance)
                        if (amountOwing==0) {
                            when (type) {
                                LEDGER_ENTRY_FEES -> {
                                    PlazaMessage.entryConfirmed(Competition(idCompetition), idAccount, amount, overdue = true)
                                }
                                LEDGER_CAMPING_PERMIT -> {
                                    // todo - replace with code for priority camping paid
                                }
                                LEDGER_UKA_REGISTRATION -> {
                                    processUka(false)
                                }
                                LEDGER_CAMPING_DEPOSIT -> {
                                    val camping = Camping(idCompetition, idAccount)
                                    camping.depositReceived()
                                }
                            }
                        }
                    }
                }
            }
        }

        fun addStripePayment(idAccount: Int, amount: Int, effectiveDate: Date, id: String, cardId: String, brand: String, last4: String, fee: Int, net: Int, available: Date) {
            val handlingFee = calcFee(amount, 20, 0.014)
            val charge = Json.nullNode()
            charge["id"] = id
            charge["source.id"] = cardId
            charge["source.brand"] = brand
            charge["source.last4"] = last4
            charge["source.id"] = cardId
            charge["transaction.fee"] = fee
            charge["transaction.net"] = net
            charge["transaction.available_on"] = available.time / 1000L
            addStripePayment(idAccount, charge, amount, handlingFee, effectiveDate)
        }

        fun addStripePayment(idAccount: Int, charge: JsonNode, amount: Int, fee: Int, effectiveDate: Date = today) {

            val stripeFee = charge["transaction.fee"].asInt

            dbTransaction {
                val ledger = Ledger()
                ledger.append()
                ledger.dateEffective = effectiveDate
                ledger.idAccount = idAccount
                ledger.amount = amount
                ledger.type = LEDGER_STRIPE_RECEIPT
                ledger.debit = ACCOUNT_STRIPE_PEND
                ledger.credit = ACCOUNT_USER
                ledger.source = charge["id"].asString
                ledger.card["id"] = charge["source.id"].asString
                ledger.card["brand"] = charge["source.brand"].asString
                ledger.card["last4"] = charge["source.last4"].asString
                ledger.card["fee.charged"] = fee
                ledger.card["fee.actual"] = charge["transaction.fee"].asInt
                ledger.card["net"] = charge["transaction.net"].asInt
                ledger.dateAvailable = Date(charge["transaction.available_on"].asLong * 1000L)
                ledger.post()

                if (fee != 0) {
                    ledger.append()
                    ledger.dateEffective = effectiveDate
                    ledger.idAccount = idAccount
                    ledger.amount = fee
                    ledger.type = LEDGER_TOP_UP_HANDLING_FEE
                    ledger.debit = ACCOUNT_USER
                    ledger.credit = ACCOUNT_TOP_UP_FEE
                    ledger.source = charge["id"].asString
                    ledger.post()
                }

                if (stripeFee != 0) {
                    ledger.append()
                    ledger.dateEffective = today
                    ledger.idAccount = idAccount
                    ledger.amount = stripeFee
                    ledger.type = LEDGER_STRIPE_FEE
                    ledger.debit = ACCOUNT_TOP_UP_FEE
                    ledger.credit = ACCOUNT_STRIPE_PEND
                    ledger.source = charge["id"].asString
                    ledger.post()
                }

                payOverdue(idAccount)
            }
        }

        fun refundAdminFees(idAccount: Int, amount: Int) {
            val ledger = Ledger()
            dbTransaction {
                ledger.append()
                ledger.dateEffective = today
                ledger.idAccount = idAccount
                ledger.amount = amount
                ledger.type = LEDGER_TOP_UP_HANDLING_FEE_REFUND
                ledger.debit = ACCOUNT_TOP_UP_FEE
                ledger.credit = ACCOUNT_USER
                ledger.post()

            }
        }

        fun addAccountTransfer(idAccountFrom: Int, idAccountTo: Int, amount: Int) {
            val nameFrom = Account(idAccountFrom).competitor.fullName
            val nameTo = Account(idAccountTo).competitor.fullName
            val ledger = Ledger()
            dbTransaction {
                ledger.append()
                ledger.dateEffective = today
                ledger.idAccount = idAccountFrom
                ledger.amount = amount
                ledger.type = LEDGER_TRANSFER_OUT
                ledger.debit = ACCOUNT_USER
                ledger.credit = ACCOUNT_TRANSFERS
                ledger.transferTo = idAccountTo
                ledger.transferToName = nameTo
                ledger.post()

                ledger.append()
                ledger.dateEffective = today
                ledger.idAccount = idAccountTo
                ledger.amount = amount
                ledger.type = LEDGER_TRANSFER_IN
                ledger.debit = ACCOUNT_TRANSFERS
                ledger.credit = ACCOUNT_USER
                ledger.transferFrom = idAccountFrom
                ledger.transferFromName = nameFrom
                ledger.post()
            }
        }

        fun addAccountCampingTransfer(idCompetition: Int, idAccountFrom: Int, idAccountTo: Int, amount: Int) {
            val nameFrom = Account(idAccountFrom).competitor.fullName
            val nameTo = Account(idAccountTo).competitor.fullName
            val ledger = Ledger()
            dbTransaction {
                ledger.append()
                ledger.dateEffective = today
                ledger.idAccount = idAccountFrom
                ledger.idCompetition = idCompetition
                ledger.amount = amount
                ledger.type = LEDGER_CAMPING_TRANSFER_OUT
                ledger.debit = ACCOUNT_USER
                ledger.credit = ACCOUNT_TRANSFERS
                ledger.transferTo = idAccountTo
                ledger.transferToName = nameTo
                ledger.post()

                ledger.append()
                ledger.dateEffective = today
                ledger.idAccount = idAccountTo
                ledger.idCompetition = idCompetition
                ledger.amount = amount
                ledger.type = LEDGER_CAMPING_TRANSFER_IN
                ledger.debit = ACCOUNT_TRANSFERS
                ledger.credit = ACCOUNT_USER
                ledger.transferFrom = idAccountFrom
                ledger.transferFromName = nameFrom
                ledger.post()
            }
        }

        fun addCompetitionCredit(idCompetition: Int, idAccountTo: Int, amount: Int, description: String) {
            val ledger = Ledger()
            dbTransaction {
                ledger.append()
                ledger.dateEffective = today
                ledger.idAccount = idAccountTo
                ledger.idCompetition = idCompetition
                ledger.amount = amount
                ledger.type = LEDGER_COMPETITION_CREDIT
                ledger.debit = ACCOUNT_SHOW_HOLDING
                ledger.credit = ACCOUNT_USER
                ledger.creditDescription = description
                ledger.post()
                payOverdue(idAccountTo)
            }
        }

        fun addCompetitionCancellationRefund(idCompetition: Int, idAccount: Int, amount: Int, entryFees: Int): Int {
            val ledger = Ledger()
            dbTransaction {
                val competition = Competition(idCompetition)
                if (!ledger.seekEntry(idAccount, idCompetition, LEDGER_ENTRY_FEES_REFUND)) {
                    ledger.append()
                    ledger.idAccount = idAccount
                    ledger.idCompetition = idCompetition
                    ledger.type = LEDGER_ENTRY_FEES_REFUND
                }
                ledger.debit = ACCOUNT_SHOW_HOLDING
                ledger.credit = ACCOUNT_USER
                ledger.amount = amount
                ledger.dateEffective = today
                ledger.refundEntryFees = entryFees
                ledger.post()
                PlazaMessage.cancelledShowRefund(idAccount, Account(idAccount).emailList, competition.briefName, ledger.amount)
                payOverdue(idAccount)
            }
            return ledger.id
        }


        fun addShowTransfer(idCompetitionFrom: Int, idCompetitionTo: Int, amount: Int) {
            val nameFrom = Competition(idCompetitionFrom).briefName
            val nameTo = Competition(idCompetitionTo).briefName
            val ledger = Ledger()
            dbTransaction {
                ledger.append()
                ledger.dateEffective = today
                ledger.idCompetition = idCompetitionFrom
                ledger.amount = amount
                ledger.type = LEDGER_TRANSFER_OUT
                ledger.debit = ACCOUNT_SHOW_HOLDING
                ledger.credit = ACCOUNT_TRANSFERS
                ledger.transferTo = idCompetitionTo
                ledger.transferToName = nameTo
                ledger.post()

                ledger.append()
                ledger.dateEffective = today
                ledger.idCompetition = idCompetitionTo
                ledger.amount = amount
                ledger.type = LEDGER_TRANSFER_IN
                ledger.debit = ACCOUNT_TRANSFERS
                ledger.credit = ACCOUNT_SHOW_HOLDING
                ledger.transferFrom = idCompetitionFrom
                ledger.transferFromName = nameFrom
                ledger.post()
            }
        }


        fun addStripeRefund(idAccount: Int, amount: Int, fee: Int, card: String) {
            val ledger = Ledger()
            dbTransaction {
                ledger.append()
                ledger.dateEffective = today
                ledger.idAccount = idAccount
                ledger.amount = amount
                ledger.type = LEDGER_STRIPE_REFUND
                ledger.debit = ACCOUNT_USER
                ledger.credit = ACCOUNT_STRIPE_PEND
                ledger.post()

                if (fee != 0) {
                    ledger.append()
                    ledger.dateEffective = today
                    ledger.idAccount = idAccount
                    ledger.amount = fee
                    ledger.type = LEDGER_STRIPE_FEE
                    ledger.debit = ACCOUNT_USER
                    ledger.credit = ACCOUNT_ADMIN
                    ledger.post()
                }
                PlazaMessage.topupRefund(idAccount, Account(idAccount).emailList, ledger.amount, card)
            }
        }


        fun addElectronicReceipt(idLedgerAccount: Int, idAccount: Int, idCompetition: Int, date: Date, amount: Int, reference: String,  wrongReference: Boolean, bankAccount: Int = ACCOUNT_STARLING): Int {
            val ledger = Ledger()
            dbTransaction {
                ledger.append()
                ledger.dateEffective = date
                ledger.idAccount = idAccount
                ledger.idCompetition = idCompetition
                ledger.amount = amount
                ledger.type = LEDGER_ELECTRONIC_RECEIPT
                ledger.debit = bankAccount
                ledger.credit = idLedgerAccount
                ledger.source = reference
                ledger.post()

                if (idLedgerAccount == ACCOUNT_USER) {
                    val account = Account(idAccount)
                    PlazaMessage.paymentReceived(idAccount, account.emailList, ledger.dateEffective, ledger.amount, ledger.source, account.code, wrongReference)
                    payOverdue(idAccount)
                }
            }
            return ledger.id
        }

        fun allocateElectronicReceipt(idLedger: Int, idLedgerAccount: Int, idAccount: Int, idCompetition: Int, wrongReference: Boolean = true) {
            Ledger().seek(idLedger) {
                if (type == LEDGER_ELECTRONIC_RECEIPT && credit == ACCOUNT_UNKNOWN) {
                    this.idAccount = idAccount
                    this.idCompetition = idCompetition
                    type = LEDGER_ELECTRONIC_RECEIPT
                    credit = idLedgerAccount
                    manuallyAllocated = true
                    post()
                    if (idLedgerAccount == ACCOUNT_USER) {
                        val account = Account(idAccount)
                        PlazaMessage.paymentReceived(idAccount, account.emailList, dateEffective, amount, source, account.code, wrongReference = wrongReference)
                        payOverdue(idAccount)
                    }
                }
            }
        }

        fun addElectronicPayment(idLedgerAccount: Int, idAccount: Int, idCompetition: Int, date: Date, amount: Int, 
                                 reference: String, type: Int, beneficiary: String="", bankAccount: Int = ACCOUNT_STARLING): Int {
            val ledger = Ledger()
            dbTransaction {
                ledger.append()
                ledger.dateEffective = date
                ledger.idAccount = idAccount
                ledger.idCompetition = idCompetition
                ledger.amount = amount
                ledger.type = type
                ledger.debit = idLedgerAccount
                ledger.credit = bankAccount
                ledger.source = reference
                ledger.beneficiary = beneficiary
                ledger.post()

                if (ledger.type == LEDGER_ELECTRONIC_REFUND && idLedgerAccount == ACCOUNT_USER) {
                    PlazaMessage.paymentRefund(idAccount, Account(idAccount).emailList, ledger.dateEffective, ledger.amount)
                } else  if (ledger.type == LEDGER_ELECTRONIC_DONATION && idLedgerAccount == ACCOUNT_USER) {
                    PlazaMessage.donation(idAccount, Account(idAccount).emailList, ledger.amount, ledger.beneficiary)
                } else if (ledger.type == LEDGER_ELECTRONIC_PAYMENT && idLedgerAccount == ACCOUNT_UKA_HOLDING) {
                    PlazaMessage.ukaRegistrationTransferConfirmation("greg@ukagility.com", ledger.dateEffective, amount)
                } else if (ledger.type == LEDGER_ELECTRONIC_PAYMENT && idLedgerAccount == ACCOUNT_SHOW_HOLDING) {
                    Competition().seek(idCompetition) {
                        if (isUka) {
                            PlazaMessage.ukaFundsTransferConfirmation("greg@ukagility.com", name, idCompetition, ledger.dateEffective, ledger.amount, reference.replace("UK AGILITY", "").trim())
                        }
                    }
                }
            }
            return ledger.id
        }

        fun allocateElectronicPayment(idLedger: Int, idLedgerAccount: Int, idAccount: Int, idCompetition: Int) {
            Ledger().seek(idLedger) {
                if (type == LEDGER_ELECTRONIC_PAYMENT && debit == ACCOUNT_UNKNOWN) {
                    this.idAccount = idAccount
                    this.idCompetition = idCompetition
                    type = if (idAccount > 0) LEDGER_ELECTRONIC_REFUND else LEDGER_ELECTRONIC_PAYMENT
                    debit = idLedgerAccount
                    post()

                    if (type == LEDGER_ELECTRONIC_REFUND && idLedgerAccount == ACCOUNT_USER) {
                        PlazaMessage.paymentRefund(idAccount, Account(idAccount).emailList, dateEffective, amount)
                    } else if (type == LEDGER_ELECTRONIC_PAYMENT && idLedgerAccount == ACCOUNT_UKA_HOLDING) {
                        PlazaMessage.ukaRegistrationTransferConfirmation("greg@ukagility.com", dateEffective, amount)
                    } else if (type == LEDGER_ELECTRONIC_PAYMENT && idLedgerAccount == ACCOUNT_SHOW_HOLDING) {
                        Competition().seek(idCompetition) {
                            if (isUka) {
                                PlazaMessage.ukaFundsTransferConfirmation("greg@ukagility.com", name, idCompetition, dateEffective, amount, reference.replace("UK AGILITY", "").trim())
                            }
                        }
                    }
                }
            }
        }

        fun addUKATransfer(idAccount: Int, date: Date, amount: Int, reference: String): Int {
            val ledger = Ledger()
            dbTransaction {
                ledger.append()
                ledger.dateEffective = date
                ledger.idAccount = idAccount
                ledger.amount = amount
                ledger.type = LEDGER_UKA_TRANSFER
                ledger.debit = ACCOUNT_UKA_XFER
                ledger.credit = ACCOUNT_USER
                ledger.source = reference
                ledger.post()
                PlazaMessage.ukaMemberFundsTransfer(idAccount, Account(idAccount).emailList, ledger.dateEffective, ledger.amount)
                payOverdue(idAccount)
            }
            return ledger.id
        }
       
        fun addSWAPTransfer(idAccount: Int, date: Date, amount: Int, reference: String): Int {
            val ledger = Ledger()
            dbTransaction {
                ledger.append()
                ledger.dateEffective = date
                ledger.idAccount = idAccount
                ledger.amount = amount
                ledger.type = LEDGER_SWAP_TRANSFER
                ledger.debit = ACCOUNT_SWAP_XFER
                ledger.credit = ACCOUNT_USER
                ledger.source = reference
                ledger.post()
                PlazaMessage.swapMemberFundsTransfer(idAccount, Account(idAccount).emailList, ledger.dateEffective, ledger.amount)
                payOverdue(idAccount)
            }
            return ledger.id
        }

        fun addMiscFee(idCompetition: Int, amount: Int): Int {
            val ledger = Ledger()
            dbTransaction {
                val competition = Competition(idCompetition)
                ledger.append()
                ledger.idCompetition = idCompetition
                ledger.type = LEDGER_MISC_FEE
                ledger.debit = ACCOUNT_SHOW_HOLDING
                ledger.credit = ACCOUNT_PLAZA_HOLDING
                ledger.amount = amount
                ledger.dateEffective = today
                ledger.post()
            }
            return ledger.id
        }

        fun addMiscExpense(idAccount: Int, amount: Int, type: Int= LEDGER_FUEL): Int {
            val ledger = Ledger()
            dbTransaction {
                ledger.append()
                ledger.idAccount = idAccount
                ledger.type = type
                ledger.debit = ACCOUNT_EXPENSE
                ledger.credit = ACCOUNT_USER
                ledger.amount = amount
                ledger.dateEffective = today
                ledger.post()
            }
            return ledger.id
        }


        fun addCancelledShowFees(idAccount: Int, idCompetition: Int, amount: Int, closingDate: Date): Int {
            val ledger = Ledger()
            dbTransaction {
                val competition = Competition(idCompetition)
                if (!ledger.seekEntry(idAccount, idCompetition, LEDGER_ENTRY_FEES_CANCELLED)) {
                    ledger.append()
                    ledger.idAccount = idAccount
                    ledger.idCompetition = idCompetition
                    ledger.type = LEDGER_ENTRY_FEES_CANCELLED
                }
                ledger.debit = ACCOUNT_USER
                ledger.credit = ACCOUNT_SHOW_HOLDING
                ledger.amount = amount
                ledger.dateEffective = closingDate
                ledger.post()
            }
            return ledger.id
        }


        fun addPaperCheque(idAccount: Int, idCompetition: Int, paidIn: Int, isUka: Boolean, date: Date = now): Int {
            val ledger = Ledger()
            if (!ledger.seekEntry(idAccount, idCompetition, LEDGER_PAPER_ENTRY_CHEQUE)) {
                ledger.append()
                ledger.dateEffective = date
                ledger.dateModified = date
                ledger.dateCreated = date
                ledger.idAccount = idAccount
                ledger.idCompetition = idCompetition
                ledger.type = LEDGER_PAPER_ENTRY_CHEQUE
                if (isUka) {
                    ledger.debit = ACCOUNT_UKA_CASH_BOX
                    ledger.credit = ACCOUNT_UKA_MEMBER
                } else {
                    ledger.debit = ACCOUNT_SHOW_CASH_BOX
                    ledger.credit = ACCOUNT_USER
                }
            }
            ledger.amount = paidIn
            ledger.post()
            return ledger.id
        }

        fun addPaperCash(idAccount: Int, idCompetition: Int, paidIn: Int, isUka: Boolean): Int {
            val ledger = Ledger()
            if (!ledger.seekEntry(idAccount, idCompetition, LEDGER_PAPER_ENTRY_CASH)) {
                ledger.append()
                ledger.dateEffective = now
                ledger.idAccount = idAccount
                ledger.idCompetition = idCompetition
                ledger.type = LEDGER_PAPER_ENTRY_CASH
                if (isUka) {
                    ledger.debit = ACCOUNT_UKA_CASH_BOX
                    ledger.credit = ACCOUNT_UKA_MEMBER
                } else {
                    ledger.debit = ACCOUNT_SHOW_CASH_BOX
                    ledger.credit = ACCOUNT_USER
                }
            }
            ledger.amount = paidIn
            ledger.post()
            return ledger.id
        }

        fun getAmount(idAccount: Int, idCompetition: Int, type: Int): Int {
            val ledger = Ledger()
            if (ledger.seekEntry(idAccount, idCompetition, type)) {
                return ledger.amount
            }
            return 0
        }

        fun deletePaperPayment(idAccount: Int, idCompetition: Int) {
            val ledger = Ledger()
            if (ledger.seekEntry(idAccount, idCompetition, LEDGER_PAPER_ENTRY_CHEQUE)) {
                ledger.delete()
            }
            if (ledger.seekEntry(idAccount, idCompetition, LEDGER_PAPER_ENTRY_CASH)) {
                ledger.delete()
            }
        }

        fun process(idAccount: Int, idCompetition: Int, entryType: Int, cheque: Int, cash: Int, freeCamping: Boolean, body: (Ledger, Int) -> Unit) {

            val competition = Competition(idCompetition)
            val ledgerType = if (entryType == ENTRY_PAPER) LEDGER_ENTRY_FEES_PAPER else LEDGER_ENTRY_FEES

            dbExecute(
                "DELETE ledgerItem.* FROM ledgerItem JOIN ledger USING (idLedger) " +
                        "WHERE ledger.idAccount=$idAccount AND ledger.idCompetition=$idCompetition AND ledger.type IN ($LEDGER_ENTRY_FEES_PAPER, $LEDGER_ENTRY_FEES, $LEDGER_CAMPING_DEPOSIT)"
            )
            dbExecute("DELETE FROM ledger WHERE idAccount=$idAccount AND idCompetition=$idCompetition AND ledger.type IN ($LEDGER_ENTRY_FEES_PAPER, $LEDGER_ENTRY_FEES, $LEDGER_CAMPING_DEPOSIT)")

            val ledger = Ledger().withAppend {
                this.idAccount = idAccount
                this.idCompetition = idCompetition
                type = ledgerType
                debit = if (entryType == ENTRY_PAPER && competition.isUka) ACCOUNT_UKA_MEMBER else ACCOUNT_USER
                credit =
                        if (entryType == ENTRY_PAPER && competition.isUka) ACCOUNT_UKA_SHOW_HOLDING else ACCOUNT_SHOW_HOLDING
                dateEffective = if (competition.grandFinals) today else competition.dateCloses
                dueImmediately = competition.grandFinals
                preparing = true
                if (entryType == ENTRY_PAPER) {
                    if (cheque + cash == 0) {
                        paperCheque = amount
                    } else {
                        if (cheque > 0) paperCheque = cheque
                        if (cash > 0) paperCash = cash
                    }
                }
                post()
            }

            var accountBalance = balance(idAccount)

            body(ledger, accountBalance)
            
            if (!competition.isUkOpen) {
                ledger.updateEntries(competition)
            }
            val idLedgerMain = ledger.id
            if (competition.needsCampingDeposit && entryType != ENTRY_PAPER && competition.dateCloses >= today && !freeCamping) {
                Ledger().withAppend {
                    this.idAccount = idAccount
                    this.idCompetition = idCompetition
                    type = LEDGER_CAMPING_DEPOSIT
                    debit = ACCOUNT_USER
                    credit = ACCOUNT_SHOW_HOLDING
                    dueImmediately = true
                    dateEffective = today
                    preparing = true
                    post()
                    updateCamping(updateLedger = true)
                    
                    LedgerItem().where("idLedger=${ledger.id} AND type=$LEDGER_ITEM_CAMPING_CREDIT") {
                        this@withAppend.charge += amount
                        if (this@withAppend.charge == 0) {
                            delete()
                        } else {
                            idLedger = this@withAppend.id
                            post()
                        }
                    }
                    if (charge>0) {
                        accountBalance = payFromFunds(accountBalance)
                    } else {
                        delete()
                    }
                }

            } else {
                ledger.updateCamping()
            }

            ledger.updateNfc()
            
            var hasItems=false
            var charge=0
            dbQuery("SELECT SUM(amount) AS amount, count(*) as Items FROM ledgerItem WHERE idLedger=${ledger.id}") {
                hasItems = getInt("Items")>0
                charge = getInt("amount")
            }
            
            if (!hasItems) {
                ledger.delete()
                if (entryType == ENTRY_PAPER) {
                    deletePaperPayment(idAccount, idCompetition)
                }
            } else {
                ledger.charge = charge
                if (entryType == ENTRY_PAPER) {
                    ledger.amount = charge
                    ledger.post()
                } else if (ledger.amountOwing>0 && (ledger.dueImmediately || ledger.dateEffective< today)) {
                    ledger.payFromFunds(accountBalance)
                } else {
                    ledger.post()
                }

                if (entryType == ENTRY_PAPER) {
                    if (cheque + cash == 0) {
                        addPaperCheque(idAccount, idCompetition, ledger.amount, competition.isUka)
                    } else {
                        if (cheque > 0) addPaperCheque(idAccount, idCompetition, cheque, competition.isUka)
                        if (cash > 0) addPaperCash(idAccount, idCompetition, cash, competition.isUka)
                    }
                }
            }
            payOverdue(idAccount)
        }


        fun deleteShowFeesAvailable(idAccount: Int, idCompetition: Int) {
            val ledger = Ledger()
            if (ledger.seekEntry(idAccount, idCompetition, ACCOUNT_SHOW_HOLDING)) {
                ledger.delete()
            }
        }

        fun mergeEntries(commaList: String) {
            val ids = commaList.listToIntArray(",")
            val idLedger = ids[0]
            dbExecute("UPDATE ledgerItem SET idLedger=$idLedger WHERE idLedger IN ($commaList)")
            dbExecute("DELETE FROM ledger WHERE idLedger IN ($commaList) AND idLedger<>$idLedger")
            // sort out postage
            val query = DbQuery(
                """
                SELECT
                    GROUP_CONCAT(idLedgerItem ORDER BY dateCreated) AS list
                FROM
                    ledgerItem
                WHERE
                    idLedger=$idLedger
                GROUP BY idDog, type
                HAVING COUNT(*) > 1
            """
            )
            while (query.next()) {
                LedgerItem.mergeEntries(query.getString("list"))
            }
            val ledger = Ledger(idLedger)
            if (ledger.found()) {
                ledger.updateAmount()
            }
        }


    }
}