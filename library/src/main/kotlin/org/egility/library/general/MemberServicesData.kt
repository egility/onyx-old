/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.library.general

import org.egility.library.database.DbQuery
import org.egility.library.dbobject.*
import java.util.*

/**
 * Created by mbrickman on 28/10/15.
 */

object MemberServicesData {

    
    var isMeasuring = false

    var credits: Int = 0
    var freeCredits = 0
    var creditsLock = false

    var alternativeHandlerIdTeam = 0;

    var accountMenuStack = Stack<String>()
    var dogMenuStack = Stack<MenuItem>()

    var idDogFirst = 0

    var shoppingList = ShoppingList()

    var _editLedger: CompetitionLedger? = null

    val account = Account()
    val accountCompetitor = Competitor()
    val accountDog = Dog()

    val selectedDog = Dog()
    val selectedCompetitor = Competitor()
    val team = Team()

    val idAccount : Int
        get() = account.id


    private val _entry = Entry()

    init {
        _entry.team.joinToParent()
    }

    val entry: Entry
        get() = _entry

    var idEntrySelected = 0

    fun selectAccount(idAccount: Int, idDog: Int=0) {
        account.seek(idAccount)
        accountMenuStack.clear()
        shoppingList.clear()
        idDogFirst = idDog

        credits = 0
        freeCredits = 0
        creditsLock = false
        _editLedger = null

        alternativeHandlerIdTeam = 0

        loadAccountMembers()
        loadAccountDogs()
    }

    fun loadAccountMembers() {
        val handlerList = account.handlersList.append("-999999")
        accountCompetitor.where("(idAccount=$idAccount OR idCompetitor IN ($handlerList)) AND dateDeleted=0 AND aliasFor=0", 
            "idCompetitor=${account.idCompetitor} DESC, idAccount=$idAccount DESC")
    }
    fun loadAccountDogs() {
        accountDog.where("idAccount=$idAccount", "idDog=$idDogFirst DESC, petName")
    }
    
    fun selectDog(idDog: Int) {
        selectedDog.find(idDog)
        if (team.idDog != idDog ) {
            val idTeam = Team.getIndividualId(selectedDog.idCompetitorHandler, idDog)
            team.find(idTeam)
            team.first()
        }
        dogMenuStack.clear()
    }

    fun selectCompetitor(idCompetitor: Int) {
        selectedCompetitor.find(idCompetitor)
    }

    val idCompetitor: Int
        get() = accountCompetitor.id

    val memberNames: String
        get() {
            var result = ""
            accountCompetitor.forEach {
                val suffix = if (!it.isUkaRegistered) "*" else ""
                result = result.append(it.fullName + suffix)
            }
            return result
        }

    val idDog: Int
        get() = selectedDog.id
    
    val handlerName: String
        get() {
            return selectedDog.handler.fullName
        }

    val cleanedPetName: String
        get() {
            return selectedDog.cleanedPetName
        }

    val jumpHeightCode: String
        get() {
            return selectedDog.ukaHeightCodePerformance
        }

    val dogCode: Int
        get() {
            return selectedDog.code
        }

    val chequesToday: Int
        get() {
            val query = DbQuery(
                """
                SELECT SUM(cheque) as chequesToday FROM competitionLedger
                  WHERE idCompetition=${Competition.current.id} AND
                    idAccount=$idAccount AND DATE(dateCreated)=${today.sqlDate}
            """
            )
            return if (query.found()) query.getInt("chequesToday") else 0
        }

    val cashToday: Int
        get() {
            val query = DbQuery(
                """
                SELECT SUM(cash) as cashToday FROM competitionLedger
                  WHERE idCompetition=${Competition.current.id} AND
                    idAccount=$idAccount AND DATE(dateCreated)=${today.sqlDate}
            """
            )
            return if (query.found()) query.getInt("cashToday") else 0
        }

    fun postComplimentary(type: Int) {
        val shoppingList = ShoppingList()
        shoppingList.addLateEntry(type, freeCredits)
        shoppingList.post(Competition.current.id, idAccount, today, PAYMENT_UNDEFINED)
    }

    fun postTransaction(paymentType: Int) {
        var reloadDogs = false
        var reloadMembers = false
        for (item in shoppingList) {
            if (item is AddRegistrationItem) reloadDogs=true
            if (item is AddMembershipItem || item is RenewMembershipItem) reloadMembers = true
        }
        shoppingList.post(Competition.current.id, idAccount, today, paymentType)
        shoppingList.clear()
        if (reloadDogs) loadAccountDogs()
        if (reloadMembers) loadAccountMembers()
    }

    fun getCreditsAvailable(idCompetition: Int): Int {
        return account.getCreditsAvailable(idCompetition)
    }

    fun getCreditsAvailableText(idCompetition: Int): String {
        return account.getCreditsAvailableText(idCompetition)
    }

    fun moveDogToGrade(gradeCode: String, programme: Int, idCompetition: Int): Boolean {
        return selectedDog.moveToGradeUkaAtShow(gradeCode, programme, idCompetition)
    }

}

