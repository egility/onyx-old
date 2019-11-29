/*
 * Copyright (c) Mike Brickman 2014-2018
 */

package org.egility.linux.tools

import org.egility.library.database.DbQuery
import org.egility.library.dbobject.Account
import org.egility.library.dbobject.Competitor
import org.egility.library.dbobject.Dog
import org.egility.library.general.*

object PlazaData {

    fun deduplicate() {
        Account.setFlags()

        dbExecute("""
            UPDATE account
                    JOIN
                competitor USING (idCompetitor)
            SET
                competitor.idAccount = account.idAccount
            WHERE
                competitor.idAccount <> account.idAccount
        """)

        DbQuery("""
            SELECT
                givenName, familyName, COUNT(*) AS total
            FROM
                competitor
            where length(givenName)>1 and AliasFor=0
            GROUP BY givenName , familyName
            HAVING total > 1
        """).forEach {
            deDuplicateName(it.getString("givenName"), it.getString("familyName"))
        }

        // sort out registration issues
        dbExecute("""
            UPDATE competitor AS C
                    JOIN
                competitor AS P ON P.idCompetitor = C.aliasFor
            SET
                P.registrationComplete = TRUE
            WHERE
                C.registrationComplete
                    AND C.aliasFor > 0
                    AND NOT P.registrationComplete;
                """)

        dbExecute("""
            UPDATE competitor
                    JOIN
                (SELECT DISTINCT
                    idAccount
                FROM
                    competitor
                WHERE
                    registrationComplete AND idAccount > 0) AS T USING (idAccount)
            SET
                registrationComplete = TRUE
            WHERE
                NOT registrationComplete
        """)

        return
    }

    fun deDuplicateAccountDogs(idAccount: Int) {
        val dog = Dog.select("dog.AliasFor=0 AND idAccount=$idAccount")
        val dogGroups = ArrayList<DogGroup>()

        dog.forEach {
            var matched = false
            dogGroups.forEach {
                if (!matched && it.matches()) {
                    it.add()
                    matched = true
                }
            }
            if (!matched) dogGroups.add(DogGroup(dog))
        }

        dogGroups.forEach {
            if (it.members.size>1) it.deDuplicate()
        }
    }


    fun deDuplicateName(givenName: String, familyName: String) {
        val nameGroups = ArrayList<NameGroup>()

        val competitor = Competitor()
        competitor.join(competitor.account).select("givenName=${givenName.quoted} AND familyName=${familyName.quoted} AND competitor.aliasFor=0").forEach {
            val dog = Dog()
            dog.select("idCompetitor=${competitor.id}")
            var matched = false
            nameGroups.forEach {
                if (!matched && it.matches(dog)) {
                    it.add(dog)
                    matched = true
                }
            }
            if (!matched) nameGroups.add(NameGroup(competitor, dog))
        }
        nameGroups.forEach {
            if (it.members.size>1) it.deDuplicate()
        }
    }

    fun mergeDuplicateDogs() {
        DbQuery("""
            SELECT
                dog.idAccount, petName, COUNT(*) AS total
            FROM
                dog
                    JOIN
                account USING (idAccount)
            WHERE
                dog.aliasFor = 0 AND dog.idAccount > 0 and petName<>""
                group by dog.idAccount, petName
                having total>1
        """).forEach {
            deDuplicateAccountDogs(it.getInt("idAccount"))
        }

        DbQuery("""
        SELECT
            dog.idAccount, registeredName, COUNT(*) AS total
        FROM
            dog
                JOIN
            account USING (idAccount)
        WHERE
            dog.aliasFor = 0 AND dog.idAccount > 0 and registeredName<>""
            group by dog.idAccount, registeredName
            having total>1
        """).forEach {
            deDuplicateAccountDogs(it.getInt("idAccount"))
        }

    }

}


internal class NameGroup(val competitor: Competitor, dog: Dog) {

    val postcodes = ArrayList<String>()
    val phoneNumbers = ArrayList<String>()
    val emails = ArrayList<String>()
    val registeredName = ArrayList<String>()
    val KCCodes = ArrayList<String>()
    val members = ArrayList<Int>()
    var name = ""

    init {
        add(dog)
        name = competitor.fullName
    }

    private fun addList(list: ArrayList<String>, value: String) {
        if (!inList(list, value)) {
            list.add(value.toUpperCase().noSpaces)
        }
    }

    private fun inList(list: ArrayList<String>, value: String): Boolean {
        return value.isNotEmpty() && list.contains(value.toUpperCase().noSpaces)
    }

    fun dogsMatch(dog: Dog): Boolean {
        var result = false
        dog.forEach {
            if (inList(registeredName, dog.registeredName)) result = true
            if (inList(KCCodes, dog.idKC)) result = true
        }
        return result
    }

    fun fixPhone(phone: String): String {
        return phone.removePrefix("44").removePrefix("0")
    }


    fun matches(dog: Dog): Boolean {
        return  inList(phoneNumbers, fixPhone(competitor.phoneMobile)) ||
                inList(phoneNumbers, fixPhone(competitor.phoneOther)) ||
                inList(emails, competitor.email) ||
                dogsMatch(dog)

    }

    fun add(dog: Dog) {
        addList(phoneNumbers, fixPhone(competitor.phoneMobile))
        addList(phoneNumbers, fixPhone(competitor.phoneOther))
        addList(emails, competitor.email)
        dog.forEach {
            addList(registeredName, dog.registeredName)
            addList(KCCodes, dog.idKC)
        }

        members.add(competitor.cursor)

    }

    fun forEachMember(body: (Competitor) -> Unit) {
        members.forEach {
            competitor.cursor = it
            body(competitor)
        }
    }

    fun deDuplicate() {
        debug("DeDuplicate", name)
        dbTransaction {
            var best = 0
            var bestIdAccount = 0
            var bestScore = 0
            var bestUka = 0
            var bestUkaScore = 0

            forEachMember {
                val score = when (competitor.ukaState) {
                    UKA_EXPIRED -> competitor.account.flags * 10 + 3
                    UKA_COMPLETE -> competitor.account.flags * 10 + 4
                    else -> competitor.account.flags * 10 + 1
                }
                if (score > bestScore) {
                    best = competitor.id
                    bestScore = score
                    bestIdAccount = competitor.idAccount
                }
                if (competitor.idUka > 0 && score > bestUkaScore) {
                    bestUka = competitor.id
                    bestUkaScore = score
                }
            }

            if (bestUka > 0 && best != bestUka) {
                swapUkaDetail(bestUka, best)
            }

            forEachMember {
                if (competitor.id != best) {
                    if (competitor.idAccount > 0 && competitor.idAccount != bestIdAccount) {
                        debug("DeDuplicate", "$name - merging account: ${competitor.idAccount} into $bestIdAccount")
                        Account.merge(good = bestIdAccount, bad = competitor.idAccount)
                    }
                    debug("DeDuplicate", "$name - merging competitor: ${competitor.id} into $best")
                    Competitor.renumberLinked(competitor.id, best)
                    competitor.aliasFor = best
                    competitor.dateDeleted = now
                    competitor.post()
                }
            }

            val dog = Dog.select("dog.AliasFor=0 AND " + if (bestIdAccount > 0) "idAccount = $bestIdAccount" else "idCompetitor = $best")
            val dogGroups = ArrayList<DogGroup>()

            dog.forEach {
                var matched = false
                dogGroups.forEach {
                    if (!matched && it.matches()) {
                        it.add()
                        matched = true
                    }
                }
                if (!matched) dogGroups.add(DogGroup(dog))
            }

            dogGroups.forEach {
                if (it.members.size>1) it.deDuplicate()
            }

        }

    }

    fun swapUkaDetail(idCompetitor1: Int, idCompetitor2: Int) {
        val competitor1 = Competitor(idCompetitor1)
        val competitor2 = Competitor(idCompetitor2)

        val idUka = competitor1.idUka
        val ukaDateConfirmed = competitor1.ukaDateConfirmed
        val ukaMembershipExpires = competitor1.ukaMembershipExpires

        competitor1.idUka = competitor2.idUka
        competitor1.ukaDateConfirmed = competitor2.ukaDateConfirmed
        competitor1.ukaMembershipExpires = competitor2.ukaMembershipExpires
        competitor1.post()

        competitor2.idUka = idUka
        competitor2.ukaDateConfirmed = ukaDateConfirmed
        competitor2.ukaMembershipExpires = ukaMembershipExpires
        competitor2.post()
    }


}




internal class DogGroup(val dog: Dog) {

    val petNames = ArrayList<String>()
    val registeredName = ArrayList<String>()
    val KCCodes = ArrayList<String>()
    val members = ArrayList<Int>()

    var name = ""

    init {
        add()

        name = "${dog.petName} / ${dog.registeredName}"
    }

    private fun addList(list: ArrayList<String>, value: String) {
        if (!inList(list, value)) {
            list.add(value.toUpperCase().noSpaces)
        }
    }

    private fun inList(list: ArrayList<String>, value: String): Boolean {
        return value.isNotEmpty() && list.contains(value.toUpperCase().noSpaces)
    }

    fun matches(): Boolean {
        return inList(petNames, dog.petName) ||
                inList(registeredName, dog.registeredName) ||
                inList(KCCodes, dog.idKC)

    }

    fun add() {
        addList(petNames, dog.petName)
        addList(registeredName, dog.registeredName)
        addList(KCCodes, dog.idKC)
        members.add(dog.cursor)

    }

    fun forEachMember(body: (Dog) -> Unit) {
        members.forEach {
            dog.cursor = it
            body(dog)
        }
    }

    fun deDuplicate() {
        var best = 0
        var bestIdAccount = 0
        var bestScore = 0
        var bestUka = 0
        var bestUkaScore = 0
        var bestKc = 0
        var bestKcScore = 0

        forEachMember {
            val score = when (dog.ukaState) {
                UKA_SUSPENDED -> dog.account.flags * 10 + 2
                UKA_INCOMPLETE -> dog.account.flags * 10 + 3
                UKA_COMPLETE -> dog.account.flags * 10 + 4
                else -> 1
            }
            if (score > bestScore) {
                best = dog.id
                bestScore = score
                bestIdAccount = dog.idAccount
            }
            if (dog.idUka > 0 && score > bestUkaScore) {
                bestUka = dog.id
                bestUkaScore = score
            }

            var kcScore = dog.account.flags * 10
            if (dog.registeredName.isNotEmpty()) kcScore+=5
            if (dog.kcGradeCode.isNotEmpty()) kcScore++
            if (dog.kcHeightCode.isNotEmpty()) kcScore++

            if (dog.idKC.isNotEmpty() && kcScore > bestKcScore) {
                bestKc = dog.id
                bestKcScore = kcScore
            }
        }

        if (bestUka > 0 && best != bestUka) {
            swapUkaDetail(bestUka, best)
        }

        if (bestKc > 0 && best != bestKc) {
            swapKcDetail(bestKc, best)
        }

        forEachMember {
            if (dog.id != best) {
                debug("DeDuplicate", "$name - merging dog: ${dog.id} into $best")
                Dog.renumberLinked(dog.id, best)
                dog.aliasFor = best
                dog.state = DOG_ARCHIVED
                dog.idCompetitor = 0
                dog.post()
            }
        }
    }

    fun swapUkaDetail(idDog1: Int, idDog2: Int) {
        val dog1 = Dog(idDog1)
        val dog2 = Dog(idDog2)

        val idUka = dog1.idUka
        val ukaDateConfirmed = dog1.ukaDateConfirmed
        val ukaExtra = Json()
        ukaExtra.setValue(dog1.ukaExtra)

        dog1.idUka = dog2.idUka
        dog1.ukaDateConfirmed = dog2.ukaDateConfirmed
        dog1.ukaExtra.setValue(dog2.ukaExtra)
        dog1.post()

        dog2.idUka = idUka
        dog2.ukaDateConfirmed = ukaDateConfirmed
        dog2.ukaExtra.setValue(ukaExtra)
        dog2.post()
    }

    fun swapKcDetail(idDog1: Int, idDog2: Int) {
        val dog1 = Dog(idDog1)
        val dog2 = Dog(idDog2)

        val idKC = dog1.idKC
        val registeredName = dog1.registeredName
        val kcExtra = Json()
        kcExtra.setValue(dog1.kcExtra)

        dog1.idKC = dog2.idKC
        dog1.registeredName = dog2.registeredName
        dog1.kcExtra.setValue(dog2.kcExtra)
        dog1.post()

        dog2.idKC = idKC
        dog2.registeredName = registeredName
        dog2.kcExtra.setValue(kcExtra)
        dog2.post()
    }




}