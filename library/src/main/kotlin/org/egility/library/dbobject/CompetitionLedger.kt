/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.library.dbobject

import org.egility.library.database.*
import org.egility.library.general.*
import java.util.*

/**
 * Created by mbrickman on 30/01/16.
 */

/*
ALTER TABLE `sandstone`.`competitionledger` 
ADD COLUMN `idDog` INT(11) NOT NULL DEFAULT '0' AFTER `idCompetitor`;
 */

open class CompetitionLedgerRaw<T: DbTable<T>>(_connection: DbConnection? = null, vararg columnNames: String) : DbTable<T>(_connection, "competitionLedger", *columnNames) {
    var id: Int by DbPropertyInt("idCompetitionLedger")
    var idCompetition: Int by DbPropertyInt("idCompetition")
    var idAccount: Int by DbPropertyInt("idAccount")
    var idCompetitor: Int by DbPropertyInt("idCompetitor")
    var idDog: Int by DbPropertyInt("idDog")
    var currency: String by DbPropertyString("currency")
    var type: Int by DbPropertyInt("type")
    var classCode: Int by DbPropertyInt("classCode")
    var expireDate: Date by DbPropertyDate("expireDate")
    var description: String by DbPropertyString("description")
    var quantity: Int by DbPropertyInt("quantity")
    var quantityUsed: Int by DbPropertyInt("quantityUsed")
    var unitPrice: Int by DbPropertyInt("unitPrice")
    var amount: Int by DbPropertyInt("amount")
    var cash: Int by DbPropertyInt("cash")
    var cheque: Int by DbPropertyInt("cheque")
    var promised: Int by DbPropertyInt("promised")
    var cancelled: Boolean by DbPropertyBoolean("cancelled")
    var cancels: Int by DbPropertyInt("cancels")
    var accountingDate: Date by DbPropertyDate("accountingDate")
    var dateLocked: Date by DbPropertyDate("dateLocked")
    var dateCreated: Date by DbPropertyDate("dateCreated")
    var deviceCreated: Int by DbPropertyInt("deviceCreated")
    var dateModified: Date by DbPropertyDate("dateModified")
    var deviceModified: Int by DbPropertyInt("deviceModified")

    val account: Account by DbLink<Account>({Account()})
    val competitor: Competitor by DbLink<Competitor>({Competitor()})
    val competition: Competition by DbLink<Competition>({Competition()})
}

class CompetitionLedger(vararg columnNames: String) : CompetitionLedgerRaw<CompetitionLedger>(null, *columnNames) {

    override fun whenBeforePost() {
        if (isAppending) {
            accountingDate = Competition.accountingDate
        }
        super.whenBeforePost()
    }

    val available: Int
        get() = quantity - quantityUsed

    val isOnAccount: Boolean
        get() = type.oneOf(ITEM_REGISTRATION)

    val isLocked: Boolean
        get() = dateLocked != nullDate

    fun cancel() {
        mandate(isOnRow, "Can't cancel Competition Ledger entry if no on row")
        mandate(quantityUsed == 0, "Can't cancel Competition Ledger entry if credits have been used")

        val new = CompetitionLedger()
        var onAccount = 0

        dbTransaction{
            new.append()
            new.idCompetition = this.idCompetition
            new.idAccount = this.idAccount
            new.idCompetitor = this.idCompetitor
            new.currency = this.currency
            new.type = this.type
            new.expireDate = this.expireDate
            new.description = this.description + " (Reverse)"
            new.quantity = -this.quantity
            new.unitPrice = this.unitPrice
            new.amount = -this.amount
            new.cash = -this.cash
            new.cheque = -this.cheque
            new.promised = -this.promised
            new.cancelled = true
            new.cancels = this.id
            new.post()

            this.cancelled = true
            this.description += " (Cancelled)"
            this.post()

        }

    }


    companion object {

        fun fixIds() {
            dbExecute(
                """
                    SELECT 
                        @id:=if(MAX(idCompetitionLedger) IS NULL, 0, MAX(idCompetitionLedger))
                    FROM
                        competitionLedger
                    WHERE
                        idCompetitionLedger < ${Int.MAX_VALUE / 2};    
                """.trimIndent()
            )

            dbExecute(
                """
                    UPDATE competitionLedger 
                    SET 
                        idCompetitionLedger = (@id:=@id + 1)
                    WHERE
                        idCompetitionLedger > @id AND dateCreated < CurDate() - INTERVAL 14 DAY 
                    ORDER BY dateCreated, idCompetitionLedger; 
                """.trimIndent()
            )
        }


        fun getWhere(idCompetition: Int, idAccount: Int, available: Boolean = false, used: Boolean = false): String {
            var where = "idCompetition=$idCompetition AND idAccount=$idAccount"
            where += " AND type IN ($ITEM_LATE_ENTRY_PAID, $ITEM_LATE_ENTRY_DISCRETIONARY, $ITEM_LATE_ENTRY_TRANSFER, $ITEM_LATE_ENTRY_STAFF, $ITEM_LATE_ENTRY_UKA)"
            if (available) {
                where += " AND quantity>quantityUsed"
            }
            if (used) {
                where += " AND quantityUsed>0"
            }
            return where
        }

        fun fixUsed(idCompetition: Int, idAccount: Int) {
            var where = "idCompetition = $idCompetition AND entry.idAccount = $idAccount"
            val query = DbQuery("SELECT SUM(lateEntryCredits) AS lateEntryCredits FROM entry JOIN agilityClass USING (idAgilityClass) WHERE $where")
            query.first()
            var quantityToUse = query.getInt("lateEntryCredits")

            val ledger = CompetitionLedger()
            ledger.select(getWhere(idCompetition, idAccount), "dateCreated")
            while (quantityToUse > 0 && ledger.next()) {
                if (quantityToUse > ledger.quantity) {
                    ledger.quantityUsed = ledger.quantity
                } else {
                    ledger.quantityUsed = quantityToUse
                }
                quantityToUse -= ledger.quantityUsed
                ledger.post()
            }
        }

        data class CreditsInfo(var quantity: Int = 0, var quantityUsed: Int = 0, var paidCredits: Int = 0, var text: String = "")

        fun getCreditsInfo(idCompetition: Int, idAccount: Int): CreditsInfo {
            val result = CreditsInfo()
            val sql = """
                select sum(quantity) as credits, sum(quantityUsed) as used, sum(if(type=$ITEM_LATE_ENTRY_PAID, quantity, 0)) as paidCredits
                from competitionLedger
                where ${getWhere(idCompetition, idAccount)}
            """
            val query = DbQuery(sql)
            if (query.found()) {
                result.quantity = query.getInt("credits")
                result.quantityUsed = query.getInt("used")
                val available = result.quantity - result.quantityUsed
                if (result.quantityUsed == 0) {
                    result.text = "$available"
                } else {
                    result.text = "$available of ${result.quantity} (${result.quantityUsed} used)"
                }
            }
            return result
        }


        fun creditsAvailable(idCompetition: Int, idAccount: Int): Int {
            val info = getCreditsInfo(idCompetition, idAccount);
            return info.quantity - info.quantityUsed
        }

        fun creditsUsed(idCompetition: Int, idAccount: Int): Int {
            val info = getCreditsInfo(idCompetition, idAccount);
            return info.quantityUsed
        }

        fun getCreditsAvailableText(idCompetition: Int, idAccount: Int): String {
            val info = getCreditsInfo(idCompetition, idAccount);
            val available = info.quantity - info.quantityUsed
            if (info.quantityUsed == 0) {
                return "$available"
            } else {
                return "$available of ${info.quantity} (${info.quantityUsed} used)"
            }
        }

        fun useCredits(idCompetition: Int, idAccount: Int, quantity: Int): Boolean {
            if (creditsAvailable(idCompetition, idAccount) < quantity) {
                return false
            }
            var quantityToUse = quantity
            val ledger = CompetitionLedger()
            ledger.select(getWhere(idCompetition, idAccount, available = true), "dateCreated")
            while (quantityToUse > 0 && ledger.next()) {
                val use = if (quantityToUse > ledger.available) ledger.available else quantityToUse
                ledger.quantityUsed += use
                quantityToUse -= use
                ledger.post()
            }
            return true
        }

        fun returnCredits(idCompetition: Int, idAccount: Int, quantity: Int): Boolean {
            if (creditsUsed(idCompetition, idAccount) < quantity) {
                return false
            }
            var quantityToReturn = quantity
            val ledger = CompetitionLedger()
            ledger.select(getWhere(idCompetition, idAccount, used = true), "dateCreated DESC")
            while (quantityToReturn > 0 && ledger.next()) {
                val unUse = if (quantityToReturn > ledger.quantityUsed) ledger.quantityUsed else quantityToReturn
                ledger.quantityUsed -= unUse
                quantityToReturn -= unUse
                ledger.post()
            }
            return true
        }

    }


}

