/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.library.dbobject

import org.egility.library.database.*
import org.egility.library.general.*
import java.util.*

/**
 * Created by mbrickman on 15/01/15.
 */

/*
ALTER TABLE `sandstone`.`competitor` 
DROP COLUMN `bankingReference`,
DROP COLUMN `addressLine2`,
DROP COLUMN `addressLine1`,
DROP COLUMN `ukaLastRenewed`,
DROP COLUMN `ukaRegistrationDate`,
DROP COLUMN `ukaRegistrationStatus`,
DROP COLUMN `promiseAmount`,
DROP COLUMN `promiseDate`,
DROP COLUMN `promiseType`,
DROP COLUMN `promiseIdCompetition`,
DROP COLUMN `tryBefore`,
DROP COLUMN `unAllocatedCredits`,
DROP COLUMN `unAllocatedDebits`,
DROP COLUMN `lastZeroBalance`,
DROP COLUMN `promisesNotAllowed`,
DROP COLUMN `unblockedReason`,
DROP COLUMN `unblockedUntil`,
DROP COLUMN `registrationFeeOwed`,
DROP COLUMN `balanceAdjusted`,
DROP COLUMN `balanceOwing`,
DROP COLUMN `balanceFuture`,
DROP COLUMN `balance`,
DROP COLUMN `dataProviderCompetitorCode`,
DROP COLUMN `dataProviderCompetitorId`,
DROP COLUMN `dataProvider`,
DROP COLUMN `idAccountOld`,
DROP COLUMN `competitorStatus`,
DROP COLUMN `ukaPending`,
DROP COLUMN `ukaBalanceRaw`,
DROP COLUMN `ukaBalance`,
DROP COLUMN `ukaRegistrationMethod`,
DROP COLUMN `ukaDateApplied`,
DROP COLUMN `accountStatus`,
DROP COLUMN `postcode`,
DROP COLUMN `countryCode`,
DROP COLUMN `regionCode`,
DROP COLUMN `town`,
DROP COLUMN `locality`,
DROP COLUMN `streetAddress`,
DROP COLUMN `pendingPayments`,

DROP COLUMN `ukaDateConfirmed`,
DROP COLUMN `ukaAliases`,
DROP COLUMN `userName`,
DROP COLUMN `ukaPassword`,
DROP COLUMN `ukaDateDeleted`,
DROP INDEX `Bank` ;
 */

open class CompetitorRaw<T : DbTable<T>>(_connection: DbConnection? = null, vararg columnNames: String) : DbTable<T>(_connection, "competitor", *columnNames) {

    open var id: Int by DbPropertyInt("idCompetitor")
    open var idAccount: Int by DbPropertyInt("idAccount")
    open var idUka: Int by DbPropertyInt("idUka")
    open var code: String by DbPropertyString("competitorCode")
    open var givenName: String by DbPropertyString("givenName")
    open var familyName: String by DbPropertyString("familyName")
    open var dateOfBirth: Date by DbPropertyDate("dateOfBirth")
    open var email: String by DbPropertyString("email")
    open var emailVerifiedDate: Date by DbPropertyDate("emailVerifiedDate")
    open var phoneMobile: String by DbPropertyString("phoneMobile")
    open var phoneOther: String by DbPropertyString("phoneOther")
    open var password: String by DbPropertyString("password")
    open var registrationComplete: Boolean by DbPropertyBoolean("registrationComplete")

    open var ukaMembershipExpires: Date by DbPropertyDate("ukaMembershipExpires")

    open var lastRingEventTime: Date by DbPropertyDate("lastRingEventTime")
    open var lastRingEventNumber: Int by DbPropertyInt("lastRingEventNumber")
    open var lastRingEventProgress: Int by DbPropertyInt("lastRingEventProgress")
    open var lastRingEventId: Int by DbPropertyInt("lastRingEventId")
    open var neededRingTime: Date by DbPropertyDate("neededRingTime")
    open var neededRingNumber: Int by DbPropertyInt("neededRingNumber")

    open var aliasFor: Int by DbPropertyInt("aliasFor")
    open var lastLogon: Date by DbPropertyDate("lastLogon")
    open var extra: Json by DbPropertyJson("extra")

    open var dateCreated: Date by DbPropertyDate("dateCreated")
    open var deviceCreated: Int by DbPropertyInt("deviceCreated")
    open var dateModified: Date by DbPropertyDate("dateModified")
    open var deviceModified: Int by DbPropertyInt("deviceModified")
    open var dateDeleted: Date by DbPropertyDate("dateDeleted")

    var systemAdministrator: Boolean by DbPropertyJsonBoolean("extra", "systemAdministrator")
    var plazaSuperUser: Boolean by DbPropertyJsonBoolean("extra", "plazaSuperUser")
    var ukaSuperUser: Boolean by DbPropertyJsonBoolean("extra", "ukaSuperUser")
    var paymentCards: JsonNode by DbPropertyJsonObject("extra", "paymentCards")
    var defaultPaymentCard: String by DbPropertyJsonString("extra", "defaultPaymentCard")
    var unVerifiedEmail: String by DbPropertyJsonString("extra", "unVerifiedEmail")
    var ykc: String by DbPropertyJsonString("extra", "ykc")

    var skills: JsonNode by DbPropertyJsonObject("extra", "kc.skills")
    var ringManager: Boolean by DbPropertyJsonBoolean("extra", "kc.skills.rm")
    var scrime: Boolean by DbPropertyJsonBoolean("extra", "kc.skills.scrime")
    var checkIn: Boolean by DbPropertyJsonBoolean("extra", "kc.skills.check")

    var uka: JsonNode by DbPropertyJsonObject("extra", "uka")

    var ukaDateConfirmed: Date by DbPropertyJsonDate("extra", "uka.dateConfirmed")
    var ukaMembershipType: Int by DbPropertyJsonInt("extra", "uka.membership.type")
    var ukaMembershipCompetition: Int by DbPropertyJsonInt("extra", "uka.membership.idCompetition")

    var ukaAliases: String by DbPropertyJsonString("extra", "uka.aliases")
    var ukaUserName: String by DbPropertyJsonString("extra", "uka.userName")
    var ukaPassword: String by DbPropertyJsonString("extra", "uka.Password")
    var ukaDateDeleted: Date by DbPropertyJsonDate("extra", "uka.DateDeleted")

    var lastLogonEmail: String by DbPropertyJsonString("extra", "logon.email")
    var lastLogonPassword: String by DbPropertyJsonString("extra", "logon.password")
    
    val account: Account by DbLink<Account>({ Account() })
    val country: Country by DbLink<Country>({ Country() })

    val dog: Dog by DbLink<Dog>({ Dog() }, label = "dog", on = "Dog.idCompetitor = Competitor.idCompetitor", keyNames = *arrayOf("idCompetitor"))
}


class Competitor(vararg columnNames: String, connection: DbConnection? = null) : CompetitorRaw<Competitor>(connection, *columnNames) {

    constructor(idCompetitor: Int) : this() {
        find(idCompetitor)
    }

    val closed: Boolean
        get() = dateDeleted.isNotEmpty() || aliasFor>0

    val ukaState: Int
        get() = when {
            ukaMembershipExpires >= today -> UKA_COMPLETE
            ukaMembershipExpires.isNotEmpty() && ukaMembershipExpires < today -> UKA_EXPIRED
            else -> UKA_NOT_REGISTERED
        }
    
    override var givenName: String
        get() = super.givenName.naturalCase
        set(value) {
            super.givenName = value
        }

    override var familyName: String
        get() = if (super.familyName.startsWith("mc", ignoreCase = true))
            "Mc" + super.familyName.drop(2).naturalCase
        else
            super.familyName.naturalCase
        set(value) {
            super.familyName = value
        }

    val fullName: String
        get() = "$givenName $familyName"

    val fullNameReverse: String
        get() = "$familyName, $givenName"

    val isDeleted: Boolean
        get() = dateDeleted.time != nullDate.time

    fun ringEvent(thisRingNumber: Int = 0): String {
        if (lastRingEventNumber != thisRingNumber) {
            when (lastRingEventProgress) {
                PROGRESS_CHECKED_IN -> {
                    if (lastRingEventTime.dateOnly() == today) {
                        return "Queuing Ring $lastRingEventNumber"
                    }
                }
                PROGRESS_RUNNING -> {
                    if (lastRingEventTime.dateOnly() == today) {
                        return "Running Ring $lastRingEventNumber"
                    }
                }
                PROGRESS_RUN -> {
                    if (lastRingEventTime >= now.addMinutes(-5)) {
                        return "Just Run $lastRingEventNumber"
                    }
                }
            }
        }
        return ""
    }

    val isUkaRegisteredOldRule: Boolean
        get() = (ukaMembershipExpires.isNotEmpty() && ukaMembershipExpires >= today) || idUka > 0

    val isUkaRegistered: Boolean
        get() = (ukaMembershipExpires.isNotEmpty() && ukaMembershipExpires >= today)

    fun getAccountID(): Int {
        makeAccount()
        return idAccount
    }

    fun ukaMembershipAtShow(idCompetition: Int) {
        ukaMembershipType = UKA_REGISTRATION_SHOW
        ukaMembershipCompetition = idCompetition
        if (ukaDateConfirmed.isEmpty()) ukaDateConfirmed = today
        ukaMembershipExpires = if (ukaMembershipExpires.before(today)) today.addYears(5) else ukaMembershipExpires.addYears(5)
        post()
    }

    fun splitFromAccount() {
        dbTransaction {
            val newAccount = Account()
            newAccount.append()
            newAccount.cloneFrom(Account(idAccount), "idAccount", "idCompetitor", "accountCode", "extra", "mergedWith", "dateCreated", "deviceCreated", "dateModified", "deviceModified", "dateDeleted")
            newAccount.idCompetitor = id
            newAccount.post()

            idAccount = newAccount.id
            post()
            dbExecute("UPDATE dog SET idAccount=${newAccount.id} WHERE idCompetitor=$id")
        }
    }
    
    fun makeAccount() {
        if (idAccount == 0) {
            dbTransaction {
                val account = Account()
                account.find("idCompetitor=$id")
                if (!account.found()) {
                    account.append()
                    account.idCompetitor = id
                    account.generateCode(false)
                }
                /*
                account.streetAddress = streetAddress
                account.town = town
                account.regionCode = regionCode
                account.countryCode = countryCode
                account.postcode = postcode
                */
                account.post()
                idAccount = account.id
                post()
                dbExecute("UPDATE dog SET idAccount=${account.id} WHERE idCompetitor=$id")
            }
        }
    }

    /*
    fun updateAccount() {
        if (idAccount != 0) {
            dbTransaction {
                val account = Account(idAccount)
                account.streetAddress = streetAddress
                account.town = town
                account.regionCode = regionCode
                account.countryCode = countryCode
                account.postcode = postcode
                account.post()
            }
        }
    }
    
     */

    fun addToAccount(idAccountNew: Int) {
        dbTransaction {
            if (idAccount == 0) {
                idAccount = idAccountNew
                post()
                dbExecute("UPDATE dog SET idAccount=$idAccountNew WHERE idCompetitor=$id")
            } else {
                val account = Account(idAccount)
                val query = DbQuery("SELECT COUNT(*) AS total FROM competitor WHERE idAccount=$idAccount").toFirst()
                if (query.getInt("total") == 1) {
                    account.mergeTo(idAccountNew)
                } else {
                    if (account.idCompetitor == id) {
                        account.removeOwner()
                    }
                    idAccount = idAccountNew
                    post()
                    dbExecute("UPDATE dog SET idAccount=$idAccountNew WHERE idCompetitor=$id")
                }
            }
        }
    }

    fun generatecompetitorCode(): String {
        val initials =
                (if (givenName.isNotEmpty() && givenName[0].isAlpha()) givenName[0].toUpperCase().toString() else "Z") +
                        (if (familyName.isNotEmpty() && familyName[0].isAlpha()) familyName[0].toUpperCase().toString() else "Z")
        var sequence = random(9999, 1000)
        var result = ""
        do {
            val test = "$initials$sequence"
            val query = DbQuery("SELECT competitorCode FROM competitor WHERE competitorCode = ${test.quoted}")
            if (!query.found()) {
                result = test
                post()
            } else {
                sequence = random(9999, 1000)
            }

        } while (result.isEmpty())
        return result
    }

    override var code: String
        get() {
            if (super.code.isEmpty()) {
                try {
                    super.code = generatecompetitorCode()
                    post()
                } catch (e: Throwable) {
                    doNothing()
                } 
            }
            return super.code
        }
        set(value) {
            super.code = value
        }

    val ykcMember: Boolean
        get() = ykc.isNotEmpty() && dateOfBirth != nullDate && dateOfBirth.addYears(25) > today

    fun generateRegistrationToken(email: String = ""): String {
        if (email.isNotEmpty()) {
            this.email = email
            emailVerifiedDate = now
            post()
        }
        val token = Json()
        token["kind"] = "token.registration"
        token["idCompetitor"] = id
        return token.toJson().encrypt(Global.keyPhrase)
    }

    fun resolveAlias(): Int {
        if (aliasFor == 0) {
            return id
        } else {
            return Competitor(aliasFor).resolveAlias()
        }
    }

    fun allocateIdUka() {
        if (idUka == 0) {
            dbExecute("LOCK TABLES competitor WRITE")
            var maxIdUka = 0
            try {
                dbQuery("SELECT MAX(idUka) AS maxIdUka FROM competitor WHERE idUka<90000") { maxIdUka = getInt("maxIdUka") }
                idUka = maxIdUka + 1
                post()
            } finally {
                dbExecute("UNLOCK TABLES")
            }
        }
    }


    companion object {
        val competitor = Competitor()

        fun select(where: String, orderBy: String = "", limit: Int = 0): Competitor {
            val competitor = Competitor()
            competitor.select(where, orderBy, limit)
            return competitor
        }

        var _maxIdUka = 0
        val maxIDUka: Int
            get() {
                if (_maxIdUka == 0) {
                    val query = DbQuery("select max(idUKA) as idUKA from competitor where idUKA<90000")
                    query.first()
                    _maxIdUka = query.getInt("idUKA")
                }
                return _maxIdUka
            }


        fun getNextIdUka(range: Int = 4): Int {
            var proposedIdUka = 0
            dbQuery("SELECT MAX(idUka) AS last FROM competitor WHERE idUka < 80000") { proposedIdUka = getInt(("last") + 1) }
            return proposedIdUka
        }

        fun getTempIdUka(range: Int = 4): Int {
            val proposedIdUka = random(range * 10000 + 9999, range * 10000)
            var ok = false
            while (!ok) {
                val query = DbQuery("SELECT idUka FROM competitor WHERE idUka=$proposedIdUka")
                ok = !query.found()
            }
            return proposedIdUka
        }


        fun logRingActivity(idContact: Int, ringNumber: Int, progress: Int, idEvent: Int) {
            competitor.find(idContact)
            if (competitor.found()) {
                competitor.lastRingEventTime = now
                competitor.lastRingEventNumber = ringNumber
                competitor.lastRingEventProgress = progress
                competitor.lastRingEventId = idEvent
                if (ringNumber == competitor.neededRingNumber) {
                    competitor.neededRingNumber = 0
                    competitor.neededRingTime = nullDate
                }
                competitor.post()
            }
        }

        fun idUkaExists(idUKA: Int): Boolean {
            if (idUKA > 0) {
                competitor.select("idUka=$idUKA")
                if (competitor.found()) {
                    return true
                }
            }
            return false
        }

        fun idUkaToIdCompetitor(idUKA: Int): Int {
            if (idUKA > 0) {
                competitor.select("idUka=$idUKA")
                if (competitor.found()) {
                    return competitor.id
                }
            }
            return -1
        }

        fun idUkaToIdAccount(idUKA: Int): Int {
            if (idUKA > 0) {
                competitor.select("idUka=$idUKA")
                if (competitor.found()) {
                    return competitor.idAccount
                }
            }
            return -1
        }

        fun idCompetitorToIdUka(idCompetitor: Int): Int {
            if (idCompetitor > 0) {
                competitor.seek(idCompetitor)
                if (competitor.found()) {
                    return competitor.idUka
                }
            }
            return -1
        }

        fun competitorCodeToIdCompetitor(handlerCode: String): Int {
            if (handlerCode.isNotEmpty()) {
                competitor.select("competitorCode=${handlerCode.quoted}")
                if (competitor.found()) {
                    return competitor.id
                }
            }
            return -1
        }

        fun registerNew(givenName: String, familyName: String, idUKA: Int): Competitor {
            val competitor = Competitor()
            competitor.append()
            competitor.givenName = givenName
            competitor.familyName = familyName
            competitor.idUka = if (idUKA > 0) idUKA else getTempIdUka(9)
            competitor.post()
            competitor.makeAccount()
            return competitor
        }

        fun getFullName(givenName: String, familyName: String): String {
            return (givenName spaceAdd familyName).naturalCase
        }

        fun renumber(from: Int, to: Int) {
            val competitor = Competitor()
            competitor.find(to)
            if (!competitor.found()) {
                renumberLinked(from, to)
                dbExecute("UPDATE competitor SET idCompetitor=$to WHERE idCompetitor=$from")
            }
        }


        fun checkLinked() {
            dbExecute("UPDATE competitionLedger JOIN competitor USING (idCompetitor) set competitionLedger.idCompetitor=competitor.aliasFor WHERE competitor.aliasFor > 0")
            dbExecute("UPDATE competitionOfficial JOIN competitor USING (idCompetitor) set competitionOfficial.idCompetitor=competitor.aliasFor WHERE competitor.aliasFor > 0")
            dbExecute("UPDATE dog JOIN competitor USING (idCompetitor) set dog.idCompetitor=competitor.aliasFor WHERE competitor.aliasFor > 0")
            dbExecute("UPDATE team JOIN competitor USING (idCompetitor) set team.idCompetitor=competitor.aliasFor WHERE competitor.aliasFor > 0")
            dbExecute("UPDATE IGNORE account JOIN competitor USING (idCompetitor) set account.idCompetitor=competitor.aliasFor WHERE competitor.aliasFor > 0")
            dbExecute("UPDATE account JOIN competitor USING (idCompetitor) set account.idCompetitor = null, account.idCompetitorOld=competitor.aliasFor WHERE competitor.aliasFor > 0")
            dbExecute("UPDATE balanceTransfer JOIN competitor USING (idCompetitor) set balanceTransfer.idCompetitor=competitor.aliasFor WHERE competitor.aliasFor > 0")
            dbExecute("UPDATE IGNORE competitionCompetitor JOIN competitor USING (idCompetitor) set competitionCompetitor.idCompetitor=competitor.aliasFor WHERE competitor.aliasFor > 0")
            dbExecute("DELETE competitionCompetitor.* FROM competitionCompetitor JOIN competitor USING (idCompetitor) WHERE competitor.aliasFor > 0")
            dbExecute("UPDATE ledgerItem JOIN competitor USING (idCompetitor) set ledgerItem.idCompetitor=competitor.aliasFor WHERE competitor.aliasFor > 0")
        }

        fun renumberLinked(from: Int, to: Int, basic: Boolean = false) {
            if (from > 0 && to > 0) {
                dbExecute("UPDATE competitionLedger SET idCompetitor=$to WHERE idCompetitor=$from")
                dbExecute("UPDATE competitionOfficial SET idCompetitor=$to WHERE idCompetitor=$from")
                dbExecute("UPDATE competitor SET aliasFor=$to WHERE idCompetitor=$from")
                dbExecute("UPDATE competitor SET aliasFor=$to WHERE aliasFor=$from")
                dbExecute("UPDATE dog SET idCompetitor=$to WHERE idCompetitor=$from")
                dbExecute("UPDATE dog SET idCompetitorHandler=$to WHERE idCompetitorHandler=$from")
                dbExecute("UPDATE team SET idCompetitor=$to WHERE idCompetitor=$from")

                if (!basic) {
                    dbExecute("UPDATE account SET idCompetitor=$to WHERE idCompetitor=$from")
                    dbExecute("UPDATE balanceTransfer SET idCompetitor=$to WHERE idCompetitor=$from")
                    dbExecute("UPDATE IGNORE competitionCompetitor SET idCompetitor=$to WHERE idCompetitor=$from")
                    dbExecute("UPDATE ledgerItem SET idCompetitor=$to WHERE idCompetitor=$from")
                }
            }
        }

        fun merge(to: String, vararg codes: String) {
            dbTransaction {
                val target = select("competitorCode=${to.quoted}")
                if (target.first()) {
                    val competitor = Competitor()
                    for (code in codes) {
                        competitor.select("competitorCode=${code.quoted}")
                        while (competitor.next()) {
                            if (competitor.idAccount == target.idAccount) {
                                if (!target.registrationComplete && competitor.registrationComplete) {
                                    target.registrationComplete = true
                                    target.email = competitor.email
                                    target.emailVerifiedDate = competitor.emailVerifiedDate
                                    target.password = competitor.password
                                }
                                renumberLinked(competitor.id, target.id)
                                if (competitor.idUka > 0) {
                                    if (competitor.ukaDateConfirmed.isNotEmpty() && (target.ukaDateConfirmed.isEmpty() || competitor.ukaDateConfirmed < target.ukaDateConfirmed)) {
                                        target.ukaDateConfirmed = competitor.ukaDateConfirmed
                                    }
                                    if (competitor.ukaMembershipExpires.isNotEmpty() && (target.ukaMembershipExpires.isEmpty() || competitor.ukaMembershipExpires > target.ukaMembershipExpires)) {
                                        target.ukaMembershipExpires = competitor.ukaMembershipExpires
                                    }
                                    target.ukaAliases = target.ukaAliases.append(competitor.idUka.toString())
                                    competitor.idAccount = 0
                                    competitor.post()
                                } else {
                                    competitor.delete()
                                }
                            }
                        }
                    }
                }
                target.post()
            }
        }

    }


}




