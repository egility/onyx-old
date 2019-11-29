/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.library.dbobject

import org.egility.library.database.*
import org.egility.library.general.*
import java.util.*

/**
 * Created by mbrickman on 27/11/16.
 */
open class LedgerItemRaw<T : DbTable<T>>(_connection: DbConnection? = null, vararg columnNames: String) :
    DbTable<T>(_connection, "ledgerItem", *columnNames) {

    open var id: Int by DbPropertyInt("idLedgerItem")
    open var idLedger: Int by DbPropertyInt("idLedger")
    open var idAccount: Int by DbPropertyInt("idAccount")
    open var idCompetition: Int by DbPropertyInt("idCompetition")
    open var idDog: Int by DbPropertyInt("idDog")
    open var idCompetitor: Int by DbPropertyInt("idCompetitor")
    open var currency: String by DbPropertyString("currency")
    open var type: Int by DbPropertyInt("type")
    open var description: String by DbPropertyString("description")
    open var quantity: Int by DbPropertyInt("quantity")
    open var unitPrice: Int by DbPropertyInt("unitPrice")
    open var amount: Int by DbPropertyInt("amount")
    open var runUnits: Int by DbPropertyInt("runUnits")
    open var code: Int by DbPropertyInt("code")
    open var size: String by DbPropertyString("size")
    open var extra: Json by DbPropertyJson("extra")
    open var flag: Boolean by DbPropertyBoolean("flag")
    open var dateCreated: Date by DbPropertyDate("dateCreated")
    open var deviceCreated: Int by DbPropertyInt("deviceCreated")
    open var dateModified: Date by DbPropertyDate("dateModified")
    open var deviceModified: Int by DbPropertyInt("deviceModified")

    open var surchargeDate: Date by DbPropertyJsonDate("extra", "surcharge.date")
    open var combinedFee: Boolean by DbPropertyJsonBoolean("extra", "combinedFee")

    val account: Account by DbLink<Account>({ Account() })
    val competition: Competition by DbLink<Competition>({ Competition() })
    val dog: Dog by DbLink<Dog>({ Dog() })
    val competitor: Competitor by DbLink<Competitor>({ Competitor() })
    val ledger: Ledger by DbLink<Ledger>({ Ledger() })
    val competitionDog: CompetitionDog by DbLink<CompetitionDog>({ CompetitionDog() }, "", "competitionDog.idCompetition = ledgerItem.idCompetition AND competitionDog.idDog = ledgerItem.idDog")
    val camping: Camping by DbLink<Camping>({ Camping() }, "", "", "idCompetition", "idAccount")

}

class LedgerItem(vararg columnNames: String) : LedgerItemRaw<LedgerItem>(null, *columnNames) {

    constructor(idLedgerItem: Int) : this() {
        find(idLedgerItem)
    }

    fun describe(): String {
        return when (type) {
            LEDGER_ITEM_ENTRY -> if (combinedFee) 
                "$description - $runUnits runs"
            else
                "$description - $runUnits runs @ ${unitPrice.toCurrency()}"
            LEDGER_ITEM_ENTRY_NFC -> description
            LEDGER_ITEM_ENTRY_SURCHARGE, LEDGER_ITEM_ENTRY_DISCOUNT, LEDGER_ITEM_SECOND_CHANCE -> description
            LEDGER_ITEM_CLOTHING -> "$quantity x $description${if (size.isNotEmpty()) " ($size)" else ""}"
            else -> transactionToText(type)
        }
    }

    companion object {

        fun select(where: String, orderBy: String = "", limit: Int = 0): LedgerItem {
            val ledgerItem = LedgerItem()
            ledgerItem.select(where, orderBy, limit)
            return ledgerItem
        }

        fun mergeEntries(commaList: String) {
            val ids = commaList.listToIntArray(",")
            val idLedgerItem = ids[0]
            val ledgerItem = LedgerItem(idLedgerItem)
            if (ledgerItem.type != LEDGER_ITEM_POSTAGE) {
                val query =
                    DbQuery("SELECT SUM(quantity) AS quantity, SUM(amount) AS amount FROM ledgerItem WHERE idLedgerItem IN ($commaList)").toFirst()
                ledgerItem.quantity = query.getInt("quantity")
                ledgerItem.amount = query.getInt("amount")
                ledgerItem.unitPrice = ledgerItem.amount / ledgerItem.quantity
                ledgerItem.post()
            }
            if (ledgerItem.type != LEDGER_ITEM_PAPER) {
                val query =
                    DbQuery("SELECT SUM(quantity) AS quantity, SUM(amount) AS amount FROM ledgerItem WHERE idLedgerItem IN ($commaList)").toFirst()
                ledgerItem.quantity = query.getInt("quantity")
                ledgerItem.amount = query.getInt("amount")
                ledgerItem.unitPrice = ledgerItem.amount / ledgerItem.quantity
                ledgerItem.post()
            }
            if (ledgerItem.type != LEDGER_ITEM_PAPER_ADMIN) {
                val query =
                    DbQuery("SELECT SUM(quantity) AS quantity, SUM(amount) AS amount FROM ledgerItem WHERE idLedgerItem IN ($commaList)").toFirst()
                ledgerItem.quantity = query.getInt("quantity")
                ledgerItem.amount = query.getInt("amount")
                ledgerItem.unitPrice = ledgerItem.amount / ledgerItem.quantity
                ledgerItem.post()
            }
            dbExecute("DELETE FROM ledgerItem WHERE idLedgerItem IN ($commaList) AND idLedgerItem<>$idLedgerItem")
            if (ledgerItem.type.oneOf(LEDGER_ITEM_CAMPING_WITH_HOOK_UP, LEDGER_ITEM_CAMPING)) {
                val c = Camping()
                c.seek(ledgerItem.idCompetition, ledgerItem.idAccount)
                if (c.found()) {
                    c.pitches = ledgerItem.quantity
                    c.post()
                }
            }
        }


    }

}