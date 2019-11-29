/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.library.general

import org.egility.library.dbobject.*
import java.util.*

/**
 * Created by mbrickman on 07/07/16.
 */

class ShoppingList {
    internal var items = ArrayList<ShoppingListItem>()

    operator fun iterator(): Iterator<ShoppingListItem> {
        return items.iterator()
    }

    fun clear() {
        items.clear()
    }

    fun cancel(): Boolean {
        var exitMemberServices = false
        for (item in items) {
            val exit = item.cancel()
            if (exit) exitMemberServices = true
        }
        return exitMemberServices
    }

    val itemCount: Int
        get() = items.count()


    val hasReturnCheque: Boolean
        get() {
            for (item in items) {
                if (item is ReturnCheque) {
                    return true
                }
            }
            return false
        }

    val totalAmount: Int
        get() {
            var value = 0
            for (item in items) {
                if (item is FinancialItem) {
                    value += item.amount
                }
            }
            return value
        }

    val totalCredits: Int
        get() {
            var value = 0
            for (item in items) {
                if (item is EnterClassItem) {
                    if (item.lateEntryCredits == 0) {
                        return 0
                    }
                    value += item.lateEntryCredits
                } else if (item is UnRemoveEntryItem) {
                    if (item.lateEntryCredits == 0) {
                        return 0
                    }
                    value += item.lateEntryCredits
                } else if (item is FinancialItem && item.amount != 0) {
                    return 0
                }
            }
            return value
        }

    val totalCreditsBought: Int
        get() {
            var value = 0
            for (item in items) {
                if (item is LedgerItem && !(item is EnterClassItem) && item.type.oneOf(LATE_ENTRY_PAID, LATE_ENTRY_DISCRETIONARY, LATE_ENTRY_STAFF, LATE_ENTRY_UKA, LATE_ENTRY_TRANSFER)) {
                    value += item.quantity
                }
            }
            return value
        }


    val lastItemLedgerType: Int
        get() {
            var item: LedgerItem? = null
            if (itemCount > 0) {
                if (items[itemCount - 1] is LedgerItem) {
                    item = items[itemCount - 1] as LedgerItem
                }
            }
            return item?.type ?: 0
        }

    fun withFinancialItems(body: (FinancialItem) -> Unit) {
        for (item in items) {
            if (item is FinancialItem) {
                body(item)
            }
        }
    }

    fun enterClass(idAgilityClass: Int, className: String, idTeam: Int, jumpHeightCode: String, lateEntryCredits: Int, entryFee: Int) {
        EnterClassItem(this, idAgilityClass, className, idTeam, jumpHeightCode, lateEntryCredits, entryFee)
    }


    fun removeEntry(className: String, idEntry: Int, jumpHeightCode: String, lateEntryCredits: Int, entryFee: Int) {
        RemoveEntryItem(this, className, idEntry, jumpHeightCode, lateEntryCredits, entryFee)
    }

    fun unRemoveEntry(className: String, idEntry: Int, jumpHeightCode: String, lateEntryCredits: Int, entryFee: Int) {
        UnRemoveEntryItem(this, className, idEntry, jumpHeightCode, lateEntryCredits, entryFee)
    }

    fun addLateEntry(type: Int, quantity: Int) {
        val lateEntryFee = Competition.current.lateEntryFee
        val lateEntryExpireDate = Competition.current.lateEntryExpireDate
        when (type) {
            LATE_ENTRY_PAID -> {
                LedgerItem(this, ITEM_LATE_ENTRY_PAID, quantity, lateEntryFee, lateEntryExpireDate)
            }
            LATE_ENTRY_DISCRETIONARY -> LedgerItem(this, ITEM_LATE_ENTRY_DISCRETIONARY, quantity, 0, lateEntryExpireDate)
            LATE_ENTRY_STAFF -> LedgerItem(this, ITEM_LATE_ENTRY_STAFF, quantity, 0, lateEntryExpireDate)
            LATE_ENTRY_UKA -> LedgerItem(this, ITEM_LATE_ENTRY_UKA, quantity, 0, lateEntryExpireDate)
            LATE_ENTRY_TRANSFER -> LedgerItem(this, ITEM_LATE_ENTRY_TRANSFER, quantity, 0, lateEntryExpireDate)
        }
    }

    fun addNull() {
        NullItem(this)
    }

    fun returnCheque(chequeValue: Int) {
        ReturnCheque(this, chequeValue)
    }

    fun cancelReturnCheque() {
        for (item in items) {
            if (item is ReturnCheque) {
                items.remove(item)
                return
            }
        }
    }

    fun havePendingEntry(idAgilityClass: Int, idDog: Int): Boolean {
        for (item in items) {
            if (item is EnterClassItem && item.idAgilityClass == idAgilityClass) {
                val team = Team()
                team.find(item.idTeam)
                if (team.hasDog(idDog)) {
                    return true
                }
            }
        }
        return false
    }

    fun post(idCompetition: Int, idAccount: Int, date: Date, paymentType: Int) {
        dbTransaction {
            for (item in items) {
                item.post(idCompetition, idAccount, date, paymentType)
            }
        }
    }

}

abstract class ShoppingListItem(shoppingList: ShoppingList) {

    init {
        shoppingList.items.add(this)
    }

    var sequence = 0

    open val description: String
        get() = ""

    open fun post(idCompetition: Int, idAccount: Int, date: Date, paymentType: Int) {
        doNothing()
    }

    open fun cancel(): Boolean {
        doNothing()
        return false
    }

    fun compareTo(other: ShoppingListItem): Int {
        return this.sequence.compareTo(other.sequence)
    }
}

class NullItem(shoppingList: ShoppingList) : ShoppingListItem(shoppingList) {
    override val description: String = "Do Nothing"
}

open class FinancialItem(shoppingList: ShoppingList) : ShoppingListItem(shoppingList) {
    var quantity = 1
    var unitPrice = 0

    override val description: String = "UNKNOWN 1"

    val amount: Int
        get() = quantity * unitPrice

}


class ReturnCheque(shoppingList: ShoppingList) : FinancialItem(shoppingList) {

    constructor(shoppingList: ShoppingList, chequeValue: Int) : this(shoppingList) {
        quantity = 1
        unitPrice = chequeValue
    }

    override val description: String = "Return Cheque"

    override fun post(idCompetition: Int, idAccount: Int, date: Date, paymentType: Int) {
        if (paymentType == PAYMENT_CASH) {
            Global.connection.execute(
                """
                    UPDATE
                        competitionLedger
                    SET
                        cash = cash + cheque,
                        cheque = 0
                    WHERE
                        idCompetition=$idCompetition AND
                        idAccount=$idAccount AND
                        DATE(dateCreated)=${date.sqlDate} AND
                        NOT cancelled AND
                        cheque<>0
                """
            )
        }
    }
}

class RegistrationItem(shoppingList: ShoppingList, var idCompetitor: Int, var petName: String) :
    LedgerItem(shoppingList, ITEM_REGISTRATION, 1, 1200) {
    var idUka = Dog.getNextIdUka(9)

    override val description: String = "Register ${petName.naturalCase} ($idUka)"

    override fun post(idCompetition: Int, idAccount: Int, date: Date, paymentType: Int) {
        postExtended(idCompetition, idAccount, date, paymentType, idCompetitor=idCompetitor)
        Dog.registerNew(idAccount, idCompetitor, petName, idUka)
    }
}

class AddRegistrationItem(shoppingList: ShoppingList, val idDog: Int, petName: String, dogCode: Int) :
    LedgerItem(shoppingList, ITEM_REGISTRATION, 1, 600) {

    override val description: String = "Register ${petName.naturalCase} ($dogCode)"

    override fun post(idCompetition: Int, idAccount: Int, date: Date, paymentType: Int) {
        postExtended(idCompetition, idAccount, date, paymentType, idDog=idDog)
        Dog(idDog).registerUkaAtShow(idCompetition)
    }
}

class AddMembershipItem(shoppingList: ShoppingList, val idCompetitor: Int, fullName: String, var junior: Boolean) :
    LedgerItem(shoppingList, ITEM_REGISTRATION, 1, if (junior) 0 else 600) {

    override val description: String = "$fullName - Join UKA"

    override fun post(idCompetition: Int, idAccount: Int, date: Date, paymentType: Int) {
        postExtended(idCompetition, idAccount, date, paymentType, idCompetitor=idCompetitor)
        Competitor(idCompetitor).ukaMembershipAtShow(idCompetition)
    }
}

class RenewMembershipItem(shoppingList: ShoppingList, val idCompetitor: Int, fullName: String, var junior: Boolean) :
    LedgerItem(shoppingList, ITEM_REGISTRATION, 1, if (junior) 0 else 600) {

    override val description: String = "$fullName - Extend membership"

    override fun post(idCompetition: Int, idAccount: Int, date: Date, paymentType: Int) {
        postExtended(idCompetition, idAccount, date, paymentType, idCompetitor=idCompetitor)
        Competitor(idCompetitor).ukaMembershipAtShow(idCompetition)
    }
}

class MembershipItem(shoppingList: ShoppingList, val idCompetitor: Int, var idUka: Int, var fullName: String, var junior: Boolean) :
    LedgerItem(shoppingList, ITEM_REGISTRATION, 1, if (junior) 0 else 1200) {
    override val description: String = "Membership $fullName ($idUka)"

    override fun cancel(): Boolean {
        if (!junior) {
            dbExecute("DELETE FROM competitor WHERE idCompetitor=$idCompetitor")
            return true
        } else {
            return false
        }
    }
}

open class LedgerItem(shoppingList: ShoppingList) : FinancialItem(shoppingList) {
    var type: Int = ITEM_UNDEFINED
    var expireDate: Date = nullDate

    constructor(shoppingList: ShoppingList, type: Int, quantity: Int, unitPrice: Int, expireDate: Date = nullDate) : this(shoppingList) {
        this.type = type
        this.quantity = quantity
        this.unitPrice = unitPrice
        this.expireDate = expireDate
        this.expireDate = expireDate
    }

    override val description: String
        get() {
            when (type) {
                ITEM_LATE_ENTRY_PAID -> return "Late Entry" + if (quantity < 0) " (Refund)" else ""
                ITEM_LATE_ENTRY_DISCRETIONARY -> return "Discretionary Late Entry" + if (quantity < 0) " (Refund)" else ""
                ITEM_LATE_ENTRY_TRANSFER -> return "Class Transfer" + if (quantity < 0) " (Refund)" else ""
                ITEM_LATE_ENTRY_STAFF -> return "Staff Late Entry" + if (quantity < 0) " (Refund)" else ""
                ITEM_LATE_ENTRY_UKA -> return "Rep Late Entry" + if (quantity < 0) " (Refund)" else ""
                ITEM_ON_ACCOUNT -> return "Account Settlement"
                ITEM_REGISTRATION -> return "Registration Fees"
                else -> return "UNKNOWN 2"
            }
        }

    open val ledgerDescription: String
        get() = description

    fun postExtended(idCompetition: Int, idAccount: Int, date: Date, paymentType: Int, idCompetitor: Int = 0, idDog: Int = 0) {

        val ledger = CompetitionLedger()

        fun doCancel(): Boolean {
            ledger.select(
                """
                idCompetition=$idCompetition AND
                idAccount=$idAccount AND
                DATE(dateCreated)=${date.sqlDate} AND
                type=$type AND
                amount=${-amount} AND
                quantityUsed = 0
            """
            )

            if (ledger.first()) {
                ledger.cancel()
                return true
            }

            return false

        }

        fun doAddItem() {
            if (type != ITEM_UNDEFINED) {
                ledger.append()
                ledger.idCompetition = idCompetition
                ledger.idAccount = idAccount
                ledger.idCompetitor = idCompetitor
                ledger.idDog = idDog
                ledger.currency = "GB"
                ledger.type = type
                ledger.expireDate = expireDate
                ledger.description = ledgerDescription
                ledger.quantity = quantity
                ledger.quantityUsed = 0
                ledger.unitPrice = unitPrice
                ledger.amount = amount
                ledger.cash = if (paymentType == PAYMENT_CASH) ledger.amount else 0
                ledger.cheque = if (paymentType == PAYMENT_CHEQUE) ledger.amount else 0
                ledger.promised = if (paymentType == PAYMENT_IN_POST) ledger.amount else 0
                ledger.post()
            }
        }

        if (quantity != 0 || !doCancel()) {
            doAddItem()
        }
    }

    override fun post(idCompetition: Int, idAccount: Int, date: Date, paymentType: Int) {
        postExtended(idCompetition, idAccount, date, paymentType)
    }
}

class EnterClassItem(
    shoppingList: ShoppingList, var idAgilityClass: Int, var className: String, var idTeam: Int, var jumpHeightCode: String,
    var lateEntryCredits: Int, var entryFee: Int
) : LedgerItem(shoppingList) {

    init {
        if (entryFee > 0) {
            this.type = ITEM_SPECIAL_CLASS
            this.quantity = 1
            this.unitPrice = entryFee
        } else {
            this.type = ITEM_LATE_ENTRY_PAID
            this.quantity = lateEntryCredits
            this.unitPrice = Competition.current.lateEntryFee
            this.expireDate = Competition.current.lateEntryExpireDate
        }
    }

    override val description: String
        get() {
            return "$className (${Height.getHeightName(jumpHeightCode)})"
        }

    override val ledgerDescription: String
        get() {
            if (type == ITEM_LATE_ENTRY_PAID) {
                return "Late Entry ($className)"
            } else {
                return "$className (${Height.getHeightName(jumpHeightCode)})"
            }
        }

    override fun post(idCompetition: Int, idAccount: Int, date: Date, paymentType: Int) {

        if (paymentType != PAYMENT_CREDITS) {
            super.post(idCompetition, idAccount, date, paymentType)
        }

        val agilityClass = AgilityClass()
        agilityClass.find(idAgilityClass)
        mandate(agilityClass.found(), "Could not find agility class ($idAgilityClass in EnterClassItem")

        agilityClass.enter(
            idTeam = idTeam,
            heightCode = jumpHeightCode,
            jumpHeightCode = jumpHeightCode,
            entryType = if (paymentType != PAYMENT_CREDITS && entryFee > 0) ENTRY_LATE_FEE else ENTRY_LATE_CREDITS,
            fee = if (paymentType != PAYMENT_CREDITS && entryFee > 0) entryFee else null,
            timeEntered = now,
            idAccount = idAccount,
            progress = PROGRESS_ENTERED
        )
    }


}


class RemoveEntryItem(shoppingList: ShoppingList, var className: String, var idEntry: Int, var jumpHeightCode: String, lateEntryCredits: Int, entryFee: Int) :
    LedgerItem(shoppingList) {

    init {
        if (entryFee > 0) {
            this.type = ITEM_SPECIAL_CLASS
            this.quantity = -1
            this.unitPrice = entryFee
        } else {
            this.type = ITEM_LATE_ENTRY_PAID
            this.quantity = -lateEntryCredits
            this.unitPrice = Competition.current.lateEntryFee
            this.expireDate = Competition.current.lateEntryExpireDate
        }
    }

    override val description: String
        get() {
            return "Cancel $className (${Height.getHeightName(jumpHeightCode)})"
        }

    override val ledgerDescription: String
        get() {
            return if (type == ITEM_LATE_ENTRY_PAID) {
                "Late Entry ($className)"
            } else {
                "$className (${Height.getHeightName(jumpHeightCode)})"
            }
        }


    override fun post(idCompetition: Int, idAccount: Int, date: Date, paymentType: Int) {
        val entry = Entry()
        entry.find(idEntry)
        mandate(entry.found(), "Could not find entry ($idEntry in RemoveEntryItem")
        entry.removeEntry()

        super.post(idCompetition, idAccount, date, paymentType)

    }
}

class UnRemoveEntryItem(shoppingList: ShoppingList, var className: String, var idEntry: Int, var jumpHeightCode: String, var lateEntryCredits: Int, entryFee: Int) :
    LedgerItem(shoppingList) {
    init {
        if (entryFee > 0) {
            this.type = ITEM_SPECIAL_CLASS
            this.quantity = 1
            this.unitPrice = entryFee
        } else {
            this.type = ITEM_LATE_ENTRY_PAID
            this.quantity = lateEntryCredits
            this.unitPrice = Competition.current.lateEntryFee
            this.expireDate = Competition.current.lateEntryExpireDate
        }
    }

    override val description: String
        get() {
            return "Reinstate $className (${Height.getHeightName(jumpHeightCode)})"
        }

    override val ledgerDescription: String
        get() {
            return if (type == ITEM_LATE_ENTRY_PAID) {
                "Late Entry ($className)"
            } else {
                "$className (${Height.getHeightName(jumpHeightCode)})"
            }
        }

    override fun post(idCompetition: Int, idAccount: Int, date: Date, paymentType: Int) {

        super.post(idCompetition, idAccount, date, paymentType)

        val entry = Entry()
        entry.find(idEntry)
        mandate(entry.found(), "Could not find entry ($idEntry in UnRemoveEntryItem")
        entry.unRemoveEntry()

    }

}










