/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.library.dbobject

import org.egility.library.database.*
import org.egility.library.general.*
import java.util.*
import java.util.regex.Pattern
import kotlin.collections.ArrayList

/**
 * Created by mbrickman on 01/12/15.
 */

open class AccountRaw<T : DbTable<T>>(_connection: DbConnection? = null, vararg columnNames: String) :
    DbTable<T>(_connection, "account") {

    open var id: Int by DbPropertyInt("idAccount")
    open var idCompetitor: Int by DbPropertyInt("idCompetitor")
    open var code: String by DbPropertyString("accountCode")
    open var streetAddress: String by DbPropertyString("streetAddress")
    open var town: String by DbPropertyString("town")
    open var regionCode: String by DbPropertyString("regionCode")
    open var countryCode: String by DbPropertyString("countryCode")
    open var postcode: String by DbPropertyString("postcode")
    open var registrationComplete: Boolean by DbPropertyBoolean("registrationComplete")
    open var accountStatus: Int by DbPropertyInt("accountStatus")
    open var flags: Int by DbPropertyInt("flags")
    open var extra: String by DbPropertyString("extra")
    open var mergedWith: Int by DbPropertyInt("mergedWith")
    open var dateCreated: Date by DbPropertyDate("dateCreated")
    open var deviceCreated: Int by DbPropertyInt("deviceCreated")
    open var dateModified: Date by DbPropertyDate("dateModified")
    open var deviceModified: Int by DbPropertyInt("deviceModified")
    open var dateDeleted: Date by DbPropertyDate("dateDeleted")

    var stripeCards: JsonNode by DbPropertyJsonObject("extra", "stripeCards")
    var handlers: JsonNode by DbPropertyJsonObject("extra", "handlers")

    var bankAccountName: String by DbPropertyJsonString("extra", "bank.accountName")
    var bankSortCode: String by DbPropertyJsonString("extra", "bank.sortCode")
    var bankAccountNumber: String by DbPropertyJsonString("extra", "bank.accountNumber")

    var fabBlocked: Boolean by DbPropertyJsonBoolean("extra", "fab.blocked")

    val competitor: Competitor by DbLink({ Competitor() })
    val country: Country by DbLink({ Country() })
    val region: Region by DbLink({ Region() })

    val geoData: GeoData by DbLink<GeoData>({ GeoData() }, keyNames = *arrayOf("postcode"))

}

class Account(vararg columnNames: String, connection: DbConnection? = null) : AccountRaw<Account>(connection, *columnNames) {

    constructor(idAccount: Int) : this() {
        find(idAccount)
    }

    val closed: Boolean
        get() = dateDeleted.isNotEmpty()

    fun generateCode(post: Boolean = false) {
        val givenName = competitor.givenName
        val familyName = competitor.familyName
        if (givenName.isNotEmpty() || familyName.isNotEmpty()) {
            val initials =
                (if (givenName.isNotEmpty() && givenName[0].isAlpha()) givenName[0].toUpperCase().toString() else "Z") +
                        (if (familyName.isNotEmpty() && familyName[0].isAlpha()) familyName[0].toUpperCase().toString() else "Z") +
                        (if (familyName.length > 1 && familyName[1].isAlpha()) familyName[1].toUpperCase().toString() else "Z")
            var sequence = random(9999, 1000)
            var proposed = ""
            do {
                val test = "$initials-$sequence-${intToBase(sequence)}"
                val query = DbQuery("SELECT accountCode FROM account WHERE accountCode = ${test.quoted}")
                if (!query.found()) {
                    proposed = test
                    post()
                } else {
                    sequence = random(9999, 1000)
                }

            } while (proposed.isEmpty())
            super.code = proposed
            post()
        }
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////

    fun getCreditsAvailable(idCompetition: Int): Int {
        return CompetitionLedger.creditsAvailable(idCompetition, id)
    }

    fun getCreditsAvailableText(idCompetition: Int): String {
        return CompetitionLedger.getCreditsAvailableText(idCompetition, id)
    }

    override var code: String
        get() {
            if (super.code.isEmpty()) {
                generateCode(post = true)
            }
            return super.code
        }
        set(value) {
            super.code = value
        }

    val fullName: String
        get() = competitor.fullName
    
    val fullAddress: String
        get() {
            var result = streetAddress.naturalCase
            var line = ""
            for (char in country.addressFormat) {
                when (char) {
                    'T' -> {
                        line = line.spaceAppend(town.toUpperCase())
                    }
                    'R' -> {
                        line = line.spaceAppend(region.name)
                    }
                    'r' -> {
                        line = line.spaceAppend(regionCode.dropLeft(3))
                    }
                    'P' -> {
                        line = line.spaceAppend(postcode.toUpperCase())
                    }
                    '|' -> {
                        result = result.append(line, "\n")
                        line = ""
                    }

                }
            }
            if (line.isNotEmpty()) {
                result = result.append(line, "\n")
            }
            return result
        }

    fun mergeTo(idAccount: Int) {
        dbTransaction {
            dbExecute("UPDATE competitor SET idAccount=$idAccount WHERE idAccount=$id")
            dbExecute("UPDATE dog SET idAccount=$idAccount WHERE idAccount=$id")
            dbExecute("UPDATE ledgerItem SET idAccount=$idAccount, idAccountOld=$id WHERE idAccount=$id")
            dbExecute("UPDATE ledger SET idAccount=$idAccount, idAccountOld=$id WHERE idAccount=$id")
            dbExecute("UPDATE entry SET idAccount=$idAccount, idAccountOld=$id WHERE idAccount=$id")
            dbExecute("UPDATE team SET idAccount=$idAccount, idAccountOld=$id WHERE idAccount=$id")
            dbExecute("UPDATE camping SET idAccount=$idAccount WHERE idAccount=$id")

            dbExecute("UPDATE competitionDog SET idAccount=$idAccount WHERE idAccount=$id")
            dbExecute("UPDATE emailQueue SET idAccount=$idAccount WHERE idAccount=$id")
            dbExecute("UPDATE competitionCompetitor SET idAccount=$idAccount WHERE idAccount=$id")

            dbExecute("UPDATE account SET mergedWith=$idAccount, idCompetitor=NULL WHERE idAccount=$id")
            mergeDuplicateLedgerEntries()
        }
    }

    fun mergeDuplicateLedgerEntries() {
        val query = DbQuery(
            """
            SELECT
                GROUP_CONCAT(idLedger ORDER BY dateCreated) AS list
            FROM
                ledger
            WHERE
                type = $LEDGER_ENTRY_FEES AND idAccount = $id
                    AND dateEffective >= CURDATE()
            GROUP BY idCompetition
            HAVING COUNT(*) > 1
        """
        )
        while (query.next()) {
            Ledger.mergeEntries(query.getString("list"))
        }
        val query2 = DbQuery(
            """
            SELECT
                GROUP_CONCAT(idLedger ORDER BY dateCreated) AS list
            FROM
                ledger
            WHERE
                type = $LEDGER_ENTRY_FEES_PAPER AND idAccount = $id
                    AND dateEffective >= CURDATE()
            GROUP BY idCompetition
            HAVING COUNT(*) > 1
        """
        )
        while (query2.next()) {
            Ledger.mergeEntries(query2.getString("list"))
        }
    }

    fun removeOwner() {
        val query = DbQuery("SELECT idCompetitor FROM Competitor WHERE idAccount=$id AND idCompetitor<>$idCompetitor")
        if (query.first()) {
            idCompetitor = query.getInt("idCompetitor")
            post()
        } else {
            throw Wobbly("Can not remove owner - no other candidates found")
        }
    }

    fun addStripeCard(charge: JsonNode) {
        val source = charge["source"]
        val id = source["id"].asString
        val last4 = source["last4"].asString
        val postCode = source["address_zip"].asString
        val brand = source["brand"].asString
        val country = source["country"].asString
        val month = source["exp_month"].asInt
        val year = source["exp_year"].asInt
        val funding = source["funding"].asString
        val name = source["name"].asString

        val card = stripeCards.searchElement("id", id, create = true)
        card["last4"] = last4
        card["brand"] = brand
        card["country"] = country
        card["month"] = month
        card["year"] = year
        card["funding"] = funding
        card["name"] = name
        card["postCode"] = postCode
    }

    fun addStripePayment(charge: JsonNode, amount: Int, fee: Int) {
        Ledger.addStripePayment(id, charge, amount, fee)
    }

    fun addStripePaymentMissing(charge: JsonNode, amount: Int, fee: Int) {
        Ledger.addStripePayment(id, charge, amount, fee)
    }

    fun seekCode(code: String): Boolean {
        return find("accountCode=${code.quoted}")
    }

    val emailList: String
        get() {
            val query =
                DbQuery("SELECT GROUP_CONCAT(DISTINCT email) AS emailList FROM competitor WHERE idAccount=$id AND aliasFor=0 AND dateDeleted=0 ORDER BY IF(idCompetitor=$idCompetitor, 0, 1)").toFirst()
            return query.getString("emailList")
        }


    val cardFixed: Int
        get() = 20

    val cardRate: Double
        get() = if (country.stripeEurope) 0.015 else 0.032

    val handlersList: String
        get() {
            var result = ""
            for (handler in handlers) {
                if (handler["idCompetitor"].asInt > 0) {
                    result = result.commaAppend(handler["idCompetitor"].asInt.toString())
                }
            }
            return result
        }

    val allHandlersList: String
        get() {
            var result = ""
            dbQuery("SELECT GROUP_CONCAT(idCompetitor) AS list FROM competitor WHERE idAccount=$id AND aliasFor=0 AND dateDeleted=0") { result = getString("list") }
            return result.append(handlersList)
        }

    fun addHandler(idCompetitor: Int) {
        handlers.addElement()["idCompetitor"] = idCompetitor
        post()
    }

    fun seekReference(reference: String): Int {
        val matcher = patternWeak.matcher(reference.toUpperCase().noSpaces.replace("-", "").replace(".", ""))
        val matched = if (matcher.find()) matcher.group(1) else ""
        val revisedReference = if (matched.isNotEmpty())
            matched.substring(0, 3) + "-" + matched.subSequence(3, 7) + "-" + matched.subSequence(7, 10)
        else
            ""

        if (revisedReference.isEmpty()) {
            return 0
        }

        val elements = revisedReference.split("-")
        val letters = elements[0].replace("0", "O").replace("1", "I").replace("8", "B")
        val numberText = elements[1].replace("O", "0").replace("I", "1").replace("B", "8")
        val check = elements[2].replace("0", "O").replace("1", "I").replace("8", "B")

        val number = numberText.toIntDef(0)

        if (check == intToBase(number)) {
            val code = "$letters-$numberText-$check"
            find("accountCode=${code.quoted} OR accountCodeOld=${code.quoted}")
            if (isOnRow) {
                if (mergedWith > 0) find(mergedWith)
                return 1
            }
        } else {
            val check2 = intToBase(number)
            val code = "$letters-$numberText-$check2"
            find("accountCode=${code.quoted} OR accountCodeOld=${code.quoted}")
            if (isOnRow) {
                if (mergedWith > 0) find(mergedWith)
                return -1
            }
            val number2 = baseToInt(check)
            val code2 = "$letters-$number2-$check"
            find("accountCode=${code2.quoted} OR accountCodeOld=${code2.quoted}")
            if (isOnRow) {
                if (mergedWith > 0) find(mergedWith)
                return -1
            }
        }

        val query = DbQuery("SELECT DISTINCT idAccount FROM Ledger WHERE type=$LEDGER_ELECTRONIC_RECEIPT AND idAccount>0 AND source LIKE ${reference.quoted}")
        if (query.rowCount == 1) {
            find(query.getInt("idAccount"))
            if (isOnRow) {
                if (mergedWith > 0) find(mergedWith)
                return -1
            }
        }

        return 0
    }

    companion object {

        fun select(where: String, orderBy: String = "", limit: Int = 0): Account {
            val account = Account()
            account.select(where, orderBy, limit)
            return account
        }

        val pattern = Pattern.compile(".*([A-Z]{3}[0-9]{4}[A-Z]{3}).*")
        val patternWeak = Pattern.compile(".*([A-Z018]{3}[0-9OIB]{4}[A-Z018]{3}).*")

        fun stripeRefund(code: String, amount: Int, fee: Int, card: String) {
            val account = Account()
            if (account.seekCode(code)) {
                Ledger.addStripeRefund(account.id, amount, fee, card)
            }
        }

        fun pendingRefund(idAccount: Int): Int {
            var refund = 0
            BankPaymentRequest().where("idAccount=$idAccount AND datePaid=0") {
                if (!isExpired) {
                    refund += amount
                }
            }
            return refund
        }

        fun maxRefund(idAccount: Int): Int {
            var refundable = 0
            Ledger().where(
                "idAccount=$idAccount AND (debit=$ACCOUNT_USER OR credit=$ACCOUNT_USER)",
                "dateEffective, idLedger"
            ) {
                val amount = if (credit == ACCOUNT_USER) amount else -amount
                if (!isPendingReceipt) {
                    refundable += amount
                }
            }
            refundable -= pendingRefund(idAccount)
            return minOf(refundable, 10000)
        }

        fun codeToId(code: String): Int {
            var result = 0
            Account().seek("accountCode=${code.quoted}") {
                result = id
            }
            return result
        }

        fun merge(bad: Int, good: Int) {
            val account = Account(bad)
            if (account.found()) {
                account.mergeTo(good)
            }
          //  setFlags()

        }

        fun setFlags() {
            dbExecute("update account set flags = 0")
            //dbExecute("update dog set dogFlags = 0")
            dbExecute("update account join competitor using (idAccount) set flags = flags | 1 where emailVerifiedDate>0")
            dbExecute("update account join ledger using (idAccount) set flags = flags | 2")
            //dbExecute("update dog join account using (idAccount) set dogFlags = flags")

        }

        fun getCampingEntitlement(idAccount: Int, idCompetition: Int): String {
            val vouchers = ArrayList<String>()
            dbQuery(
                """
                SELECT
                    GROUP_CONCAT(voucherCode) AS voucherCodes
                FROM
                    competitionCompetitor
                        JOIN
                    competitor USING (idCompetitor)
                WHERE
                    idCompetition = $idCompetition AND competitor.idAccount = $idAccount
                        AND voucherCode <> ''
                GROUP BY
                    competitor.idAccount
            """
            )
            {
                for (code in getString("voucherCodes").split(",")) {
                    if (!vouchers.contains(code)) vouchers.add(code)
                }
            }


            if (vouchers.isNotEmpty()) {
                val competition = Competition(idCompetition)
                val campingMap = competition.campingMap
                for (voucherCode in vouchers) {
                    if (campingMap.containsKey(voucherCode)) {
                        return campingMap[voucherCode] ?: ""
                    }
                }
            }
            return ""
        }

        fun describe(idAccount: Int): String {
            var result="Unknown"
            Account().join { competitor }.seek(idAccount) {
                val fullName=competitor.fullName
                result = if (competitor.fullName.last()=='s')
                    "${competitor.fullName}' household"
                else
                    "${competitor.fullName}'s household"
            }
            return result
        }

    }


}