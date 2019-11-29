/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.library.dbobject

import org.egility.library.database.*
import org.egility.library.general.*
import java.util.*

/**
 * Created by mbrickman on 04/03/16.
 */

open class TeamRaw<T : DbTable<T>>(_connection: DbConnection? = null, vararg columnNames: String) : DbTable<T>(_connection, "team", *columnNames) {

    open var id: Int by DbPropertyInt("idTeam")
    open var idAccount: Int by DbPropertyInt("idAccount")
    open var idAgilityClass: Int by DbPropertyInt("idAgilityClass")
    open var idCompetitor: Int by DbPropertyInt("idCompetitor")
    open var idDog: Int by DbPropertyInt("idDog")
    open var code: Int by DbPropertyInt("teamCode")
    open var type: Int by DbPropertyInt("teamType")
    open var competitorName: String by DbPropertyString("competitorName")
    open var extra: Json by DbPropertyJson("extra")
    open var dateCreated: Date by DbPropertyDate("dateCreated")
    open var deviceCreated: Int by DbPropertyInt("deviceCreated")
    open var dateModified: Date by DbPropertyDate("dateModified")
    open var deviceModified: Int by DbPropertyInt("deviceModified")
    open var dateDeleted: Date by DbPropertyDate("dateDeleted")



    var classCode: Int by DbPropertyJsonInt("extra", "classCode")
    var teamName: String by DbPropertyJsonString("extra", "teamName")
    var clubName: String by DbPropertyJsonString("extra", "clubName")
    var members: JsonNode by DbPropertyJsonObject("extra", "members")

    val competitor: Competitor by DbLink<Competitor>({ Competitor() })
    val dog: Dog by DbLink<Dog>({ Dog() })
    val agilityClass: AgilityClass by DbLink<AgilityClass>({ AgilityClass() })


    /*
    val owner: Competitor by DbLink<Competitor>({ Competitor() }, label = "owner", keyNames = "idCompetitorOwner")
    val competitor2: Competitor by DbLink<Competitor>({ Competitor() }, label = "competitor2", keyNames = "idCompetitor2")
    val competitor3: Competitor by DbLink<Competitor>({ Competitor() }, label = "competitor3", keyNames = "idCompetitor3")
    val dog2: Dog by DbLink<Dog>({ Dog() }, label = "dog2", keyNames = "idDog2")
    val dog3: Dog by DbLink<Dog>({ Dog() }, label = "dog3", keyNames = "idDog3")
    */
}


class Team(vararg columnNames: String) : TeamRaw<Team>(null, *columnNames) {

    constructor(idTeam: Int) : this() {
        find(idTeam)
    }

    val multiple: Boolean
        get() = type == TEAM_MULTIPLE


    fun memberByDogId(idDog: Int): JsonNode {
        var result = members.searchElement("idDog", idDog, create = false)
        if (result.isNull) {
            result = members.addElement()
            result["idDog"] = idDog
        }
        return result
    }

    fun memberByDogCode(dogCode: Int): JsonNode {
        var result = members.searchElement("dogCode", dogCode, create = false)
        if (result.isNull) {
            result = members.searchElement("dogCode", dogCode.toString(), create = false)
        }
        if (result.isNull) {
            result = members.addElement()
        }
        result["dogCode"] = dogCode
        return result
    }

    fun member(teamMember: Int): JsonNode {
        return members[teamMember - 1]
    }


    fun refreshMembers(teamMember: Int=-1, keepHeight: Boolean = false) {
        if (type == TEAM_MULTIPLE) {

            var dogCodeList = ""
            var idDogList = ""
            if (teamMember>=0) {
                val member=members[teamMember-1]
                if (member["idDog"].asInt > 0) idDogList = idDogList.commaAppend(member["idDog"].asInt.toString())
                if (member["dogCode"].asInt > 0) dogCodeList = dogCodeList.commaAppend(member["dogCode"].asInt.toString())

            } else {
                for (member in members) {
                    if (member["idDog"].asInt > 0) idDogList = idDogList.commaAppend(member["idDog"].asInt.toString())
                    if (member["dogCode"].asInt > 0) dogCodeList = dogCodeList.commaAppend(member["dogCode"].asInt.toString())
                }
            }
            val useCode = dogCodeList.split(",").size > idDogList.split(",").size
            val dog = Dog()
            val competitor = Competitor()
            var thisCompetitor = Competitor()
            dog.owner.joinToParent()
            dog.select(if (useCode) "dogCode IN ($dogCodeList)" else "idDog IN ($idDogList)")
            while (dog.next()) {
                val node = if (useCode) memberByDogCode(dog.code) else memberByDogId(dog.id)
                var named = node.has("named") && node["named"].asBoolean
                if (!named) {
                    if (node["idCompetitor"].asInt > 0 ) {
                        competitor.find(node["idCompetitor"].asInt)
                        thisCompetitor = competitor
                    } else {
                        thisCompetitor = if (dog._idCompetitorHandler>0) dog.handler else dog.owner
                    }
                    if (node.has("competitorName") && node["competitorName"].asString neq thisCompetitor.fullName) {
                        named = true
                    }
                }

                updateMemberNode(node,
                        idDog = dog.id,
                        petName = dog.petName,
                        registeredName = dog.registeredName,
                        competitorName = if (named) null else thisCompetitor.fullName,
                        dogCode = if (dog.idUka > 0) dog.idUka else dog.code,
                        heightCode = if (keepHeight) null else if (template.isUka) dog.ukaHeightCodePerformance else dog.kcHeightCode
                )
            }
        }
    }

    fun refreshMembers2(teamMember: Int=-1, keepHeight: Boolean = false) {
        if (type == TEAM_MULTIPLE) {

            var dogCodeList = ""
            var idDogList = ""
            if (teamMember>=0) {
                val member=members[teamMember-1]
                if (member["idDog"].asInt > 0) idDogList = idDogList.commaAppend(member["idDog"].asInt.toString())
                if (member["dogCode"].asInt > 0) dogCodeList = dogCodeList.commaAppend(member["dogCode"].asInt.toString())

            } else {
                for (member in members) {
                    if (member["idDog"].asInt > 0) idDogList = idDogList.commaAppend(member["idDog"].asInt.toString())
                    if (member["dogCode"].asInt > 0) dogCodeList = dogCodeList.commaAppend(member["dogCode"].asInt.toString())
                }
            }
            val useCode = dogCodeList.split(",").size > idDogList.split(",").size
            val dog = Dog()
            val competitor = Competitor()
            var thisCompetitor = Competitor()
            dog.owner.joinToParent()
            dog.select(if (useCode) "dogCode IN ($dogCodeList)" else "idDog IN ($idDogList)")
            while (dog.next()) {
                val node = if (useCode) memberByDogCode(dog.code) else memberByDogId(dog.id)
                var named = node.has("named") && node["named"].asBoolean
                if (!named) {
                    if (node["idCompetitor"].asInt > 0 ) {
                        competitor.find(node["idCompetitor"].asInt)
                        thisCompetitor = competitor
                    } else {
                        thisCompetitor = if (dog._idCompetitorHandler>0) dog.handler else dog.owner
                    }
                    if (node.has("competitorName") && node["competitorName"].asString neq thisCompetitor.fullName) {
                        named = true
                    }
                }

                updateMemberNode(node,
                    idDog = dog.id,
                    petName = dog.petName,
                    registeredName = dog.registeredName,
                    //competitorName = if (named) null else thisCompetitor.fullName,
                    dogCode = if (dog.idUka > 0) dog.idUka else dog.code,
                    heightCode = if (keepHeight) null else if (template.isUka) dog.ukaHeightCodePerformance else dog.kcHeightCode
                )
            }
        }
    }


    fun selectMultiple(idAccount: Int, idAgilityClass: Int, idDog: Int) {
        select("team.idAccount=$idAccount AND team.idAgilityClass=$idAgilityClass AND team.idDog=$idDog AND team.teamType=$TEAM_MULTIPLE")
    }

    fun selectDualHandler(idAccount: Int, idAgilityClass: Int, idDog: Int) {
        select("team.idAccount=$idAccount AND team.idAgilityClass=$idAgilityClass AND team.idDog=$idDog AND team.teamType=$TEAM_DUAL_HANDLER")
    }


    fun getTeamDescription(teamMember: Int, showHeight: Boolean = false, formal: Boolean = false, extended: Boolean = false): String {
        if (!multiple) {
            if (getIdDog(teamMember) == 0) {
                return "TBA"
            }
            if (showHeight) {
                return "${getCompetitorName(teamMember)} & ${getDogName(teamMember, formal)} (${getHeightName(teamMember)})"
            } else {
                return "${getCompetitorName(teamMember)} & ${getDogName(teamMember, formal)}"
            }
        } else if (classCode == ClassTemplate.SPLIT_PAIRS.code) {
            if (extended) {
                return "${getCompetitorDog(1)} / ${getCompetitorDog(2)}"
            } else {
                return "${getDogName(1, formal)} & ${getDogName(2, formal)}"
            }
        } else if (classCode.oneOf(ClassTemplate.KC_PAIRS_MIXI.code, ClassTemplate.KC_PAIRS_JUMPING.code, ClassTemplate.KC_PAIRS_AGILITY.code)) {
            return "${getCompetitorName(1)} & ${getCompetitorName(2)} with ${getDogName(1, formal)} & ${getDogName(2, formal)}"
        } else {
            if (teamMember > 0) {
                if (extended) {
                    return "${getCompetitorName(teamMember)} & ${getDogName(teamMember, formal)} ($teamName)"
                } else {
                    return "$teamName (${getDogName(teamMember, formal)})"
                }
            } else {
                return teamName
            }
        }
    }

    fun getMembers(formal: Boolean = false): String {
        var result = ""
        for (teamMember in 1..memberCount) {
            if (getIdDog(teamMember) > 0) {
                result = result.append("${getCompetitorName(teamMember)} & ${getDogName(teamMember, formal)}")
            } else {
                result = result.append("n/a")
            }
        }
        return result
    }

    fun getCompetitorDog(teamMember: Int, showHeight: Boolean = false, showId: Boolean = false, ukaTitle: Boolean=false): String {
        if (getIdDog(teamMember) == 0) {
            return "TBA"
        }
        val preamble = if (showId) "${getDogCode(teamMember)} " else ""
        if (showHeight) {
            return "$preamble${getCompetitorName(teamMember)} & ${getDogName(teamMember, ukaTitle=ukaTitle)} (${getHeightName(teamMember)})"
        } else {
            return "$preamble${getCompetitorName(teamMember)} & ${getDogName(teamMember, ukaTitle=ukaTitle)}"
        }
    }


    fun getMajorName(teamMember: Int): String {
        if (teamMember > 0 || memberCount == 1) {
            return getCompetitorName(teamMember)
        } else if (memberCount == 2) {
            return "${getDogName(1)} & ${getDogName(2)}"
        } else {
            return teamName
        }
    }

    fun getMinorName(teamMember: Int): String {
        if (teamMember > 0 || memberCount == 1) {
            if (Competition.hasEgilityCodes) {
                return getDogName(teamMember) + " (${getDogCode(teamMember)})"
            } else {
                return getDogName(teamMember)
            }
        } else if (memberCount == 2) {
            return "${getCompetitorName(1)} & ${getCompetitorName(2)}"
        } else if (memberCount == 3) {
            return "${getDogName(1)} & ${getDogName(2)} & ${getDogName(3)}"
        } else {
            return "${getCompetitorName(1)} & ${getCompetitorName(2)} & ${getCompetitorName(2)}"
        }
    }

    fun getCompetitorName(teamMember: Int): String {
        return if (multiple)
            member(teamMember)["competitorName"].asString
        else if (competitorName.isEmpty())
            competitor.fullName
        else competitorName

    }

    fun getPrincipalName(): String {
        return if (competitorName.isEmpty())
            competitor.fullName
        else competitorName
    }

    var dualHandler: String
        get() = if (type == TEAM_DUAL_HANDLER) competitorName else ""
        set(value) {
            if (type == TEAM_DUAL_HANDLER) competitorName = value
        }

    fun getCompetitor(teamMember: Int): Competitor? {
        return if (multiple)
            Competitor(member(teamMember)["idCompetitor"].asInt)
        else
            competitor
    }

    fun getRegisteredName(teamMember: Int): String {
        return if (multiple)
            member(teamMember)["registeredName"].asString.ifEmpty("DOG $teamMember")
        else
            dog.registeredName
    }

    fun getPetName(teamMember: Int): String {
        return if (multiple)
            member(teamMember)["petName"].asString.ifEmpty("DOG $teamMember")
        else
            dog.cleanedPetName
    }

    fun getDogName(teamMember: Int, formal: Boolean = false, ukaTitle: Boolean=false): String {
        return if (multiple)
            if (formal) getRegisteredName(teamMember) else getPetName(teamMember)
        else
            if (formal) dog.registeredName else if (ukaTitle) dog.cleanedPetNameAndUkaTitle else dog.cleanedPetName
    }

    fun getDogCode(teamMember: Int): Int {
        return if (multiple)
            member(teamMember)["dogCode"].asInt
        else
            dog.code
    }

    fun getIdCompetitor(teamMember: Int): Int {
        return if (multiple)
            member(teamMember)["idCompetitor"].asInt
        else
            idCompetitor
    }

    fun getIdDog(teamMember: Int): Int {
        return if (multiple)
            member(teamMember)["idDog"].asInt
        else
            idDog
    }


    fun getHeightCode(teamMember: Int): String {
        return if (multiple)
            member(teamMember)["heightCode"].asString
        else
            ""
    }

    fun getHeightName(teamMember: Int): String {
        if (classCode == ClassTemplate.SPLIT_PAIRS.code) {
            when (getHeightCode(teamMember)) {
                "UKA650", "UKA550" -> return Height.getHeightName("UKA550")
                "UKA400", "UKA300" -> return Height.getHeightName("UKA300")
            }
        }
        return Height.getHeightName(getHeightCode(teamMember), "tba")
    }

    fun setHeightCode(teamMember: Int, heightCode: String) {
        if (multiple) {
            member(teamMember)["heightCode"] = heightCode
        }
    }

    val memberCount: Int
        get() {
            return if (multiple)
                members.size
            else
                1
        }

    val template: ClassTemplate
        get() = ClassTemplate.select(classCode)

    val description: String
        get() = if (type.oneOf(TEAM_SINGLE_HANDLER, TEAM_LINKED_OTHER))
            competitor.fullName + " & " + dog.cleanedPetName
        else if (type == TEAM_NAMED_HANDLER)
            competitorName + " & " + dog.cleanedPetName
        else if (memberCount == 2)
            "${getPetName(1)} & ${getPetName(2)}"
        else
            teamName

    val fullDescription: String
        get() {

            if (type.oneOf(TEAM_SINGLE_HANDLER, TEAM_LINKED_OTHER))
                return competitor.fullName + " & " + dog.cleanedPetName
            else if (type == TEAM_NAMED_HANDLER)
                return competitorName + " & " + dog.cleanedPetName
            else {
                var result = ""
                var delimiter = if (memberCount == 2) " & " else ", "
                for (teamMember in 1..memberCount) {
                    if (template.isUka) {
                        result = result.append("${getPetName(teamMember)}/${getHeightName(teamMember)}", delimiter)
                    } else {
                        result = result.append(getPetName(teamMember), delimiter)
                    }
                }
                if (teamName.isEmpty()) {
                    return result
                } else {
                    return "$teamName ($result)"

                }
            }
        }

    val hasMembershipIssues: Boolean
        get() = type.oneOf(TEAM_SINGLE_HANDLER, TEAM_LINKED_OTHER) && !competitor.isUkaRegistered

    val isRedCarded: Boolean
        get() = dog.ukaBarred


    val relayHeightCode: String
        get() {
            if (template == ClassTemplate.SPLIT_PAIRS) {
                if (getHeightCode(1).oneOf("UKA300", "UKA400") && getHeightCode(2).oneOf("UKA300", "UKA400")) {
                    return "UKA901"
                } else if (getHeightCode(1).oneOf("UKA300", "UKA400") && getHeightCode(2).oneOf("UKA550", "UKA650")) {
                    return "UKA902"
                } else if (getHeightCode(1).oneOf("UKA550", "UKA650") && getHeightCode(2).oneOf("UKA300", "UKA400")) {
                    return "UKA903"
                } else if (getHeightCode(1).oneOf("UKA550", "UKA650") && getHeightCode(2).oneOf("UKA550", "UKA650")) {
                    return "UKA904"
                }
            } else if (template == ClassTemplate.TEAM) {
                var large = 0
                for (i in 1..3) {
                    if (getHeightCode(i).oneOf("UKA550", "UKA650")) {
                        large++
                    } else if (getHeightCode(i).isEmpty()) {
                        return ""
                    }
                }
                when (large) {
                    0 -> return "UKA901"
                    1 -> return "UKA902"
                    2 -> return "UKA903"
                    3 -> return "UKA904"
                }
            }
            return ""
        }

    fun setRelayHeightCode(value: String) {
        if (value != relayHeightCode) {
            when (value) {
                "UKA901" -> {
                    member(1)["heightCode"] = "UKA300"
                    member(2)["heightCode"] = "UKA300"
                }
                "UKA902" -> {
                    member(1)["heightCode"] = "UKA300"
                    member(2)["heightCode"] = "UKA550"
                }
                "UKA903" -> {
                    member(1)["heightCode"] = "UKA550"
                    member(2)["heightCode"] = "UKA300"
                }
                "UKA904" -> {
                    member(1)["heightCode"] = "UKA550"
                    member(2)["heightCode"] = "UKA550"
                }
            }
            post()
        }

    }

    val idTeamList: String
        get() = Dog.getIdTeamList(idDog)

    fun hasDog(idDog: Int): Boolean {
        if (multiple) {
            for (teamMember in 1..memberCount) {
                if (getIdDog(teamMember) == idDog) return true
            }
            return false
        } else {
            return this.idDog == idDog
        }
    }

    fun selectUkaPair(idAccount: Int, idAgilityClass: Int, idDog: Int, idDog1: Int, idDog2: Int, idCompetitor1: Int, idCompetitor2: Int, heightCode1: String, heightCode2: String) {
        selectMultiple(idAccount, idAgilityClass, idDog)
        if (!found()) {
            append()
            this.idAccount = idAccount
            this.idAgilityClass = idAgilityClass
            this.idDog = idDog
            type = TEAM_MULTIPLE
        }
        classCode = ClassTemplate.SPLIT_PAIRS.code
        this.teamName = teamName
        updateMemberNode(member(1), idDog = idDog1, idCompetitor = idCompetitor1, heightCode = heightCode1)
        updateMemberNode(member(2), idDog = idDog2, idCompetitor = idCompetitor2, heightCode = heightCode2)
        refreshMembers()
        post()
    }

    fun reviseUkaPair(idDog1: Int, idDog2: Int, idCompetitor1: Int, idCompetitor2: Int, heightCode1: String, heightCode2: String) {
        updateMemberNode(member(1), idDog = idDog1, idCompetitor = idCompetitor1, heightCode = heightCode1)
        updateMemberNode(member(2), idDog = idDog2, idCompetitor = idCompetitor2, heightCode = heightCode2)
        refreshMembers()
        post()
    }

    fun replaceUkaPairMember(teamMember: Int, idDog: Int, idCompetitor: Int) {
        updateMemberNode(member(teamMember), idDog = idDog, idCompetitor = idCompetitor)
        refreshMembers(teamMember)
        post()
    }

    fun selectUkaTeam(idAccount: Int, idAgilityClass: Int, idDog: Int, idDog1: Int, idDog2: Int, idDog3: Int, idCompetitor1: Int, idCompetitor2: Int, idCompetitor3: Int, heightCode1: String, heightCode2: String, heightCode3: String, teamName: String) {
        selectMultiple(idAccount, idAgilityClass, idDog)
        if (!found()) {
            append()
            this.idAccount = idAccount
            this.idAgilityClass = idAgilityClass
            this.idDog = idDog
            type = TEAM_MULTIPLE
        }
        classCode = ClassTemplate.TEAM.code
        this.teamName = teamName
        updateMemberNode(member(1), idDog = idDog1, idCompetitor = idCompetitor1, heightCode = heightCode1)
        updateMemberNode(member(2), idDog = idDog2, idCompetitor = idCompetitor2, heightCode = heightCode2)
        updateMemberNode(member(3), idDog = idDog3, idCompetitor = idCompetitor3, heightCode = heightCode3)
        refreshMembers(keepHeight = true)
        post()
    }

    fun reviseUkaTeam(idDog1: Int, idDog2: Int, idDog3: Int, idCompetitor1: Int, idCompetitor2: Int, idCompetitor3: Int, heightCode1: String, heightCode2: String, heightCode3: String, teamName: String) {
        this.teamName = teamName
        updateMemberNode(member(1), idDog = idDog1, idCompetitor = idCompetitor1, heightCode = heightCode1)
        updateMemberNode(member(2), idDog = idDog2, idCompetitor = idCompetitor2, heightCode = heightCode2)
        updateMemberNode(member(3), idDog = idDog3, idCompetitor = idCompetitor3, heightCode = heightCode3)
        refreshMembers(keepHeight = true)
        post()
    }

    fun swapMembers(teamMember1: Int = 0, teamMember2: Int = 1) {
        if (multiple) {
            val temp = Json(member(teamMember1).toJson())
            member(teamMember1).setValue(member(teamMember2))
            member(teamMember2).setValue(temp)
            post()
        }

    }

    companion object {

        fun select(where: String, orderBy: String = "", limit: Int = 0): Team {
            val team = Team()
            team.select(where, orderBy, limit)
            return team
        }


        fun updateMemberNode(node: JsonNode, idDog: Int? = null, idCompetitor: Int? = null, heightCode: String? = null, competitorName: String? = null, petName: String? = null, registeredName: String? = null, dogCode: Int? = null, named: Boolean? = null) {
            if (idDog != null) node["idDog"] = idDog
            if (idCompetitor != null) node["idCompetitor"] = idCompetitor
            if (heightCode != null) node["heightCode"] = heightCode
            if (competitorName != null) node["competitorName"] = competitorName
            if (petName != null) node["petName"] = petName
            if (registeredName != null) node["registeredName"] = registeredName
            if (dogCode != null) node["dogCode"] = dogCode
            if (named != null) node["named"] = named
        }

        fun getIndividual(idCompetitor: Int, idDog: Int): Team {
            val team = Team()
            val dog = Dog()
            team.select("idCompetitor=$idCompetitor AND idDog=$idDog AND teamType IN ($TEAM_SINGLE_HANDLER, $TEAM_LINKED_OTHER)")
            if (!team.first()) {
                dog.find(idDog)
                if (dog.found()) {
                    team.append()
                    team.idCompetitor = idCompetitor
                    team.idDog = idDog
                    team.code = if (idCompetitor == dog.idCompetitor) dog.code else 0
                    team.type = if (idCompetitor == dog.idCompetitor) TEAM_SINGLE_HANDLER else TEAM_LINKED_OTHER
                    team.post()
                } else {
                    throw Wobbly("Attempt to create team with unknown idDog=$idDog")
                }
            }
            return team
        }

        fun getIndividualId(idCompetitor: Int, idDog: Int): Int {
            val team = Team()
            val dog = Dog()
            team.select("idCompetitor=$idCompetitor AND idDog=$idDog AND teamType IN ($TEAM_SINGLE_HANDLER, $TEAM_LINKED_OTHER)")
            if (!team.first()) {
                dog.find(idDog)
                if (dog.found()) {
                    team.append()
                    team.idCompetitor = idCompetitor
                    team.idDog = idDog
                    team.code = if (idCompetitor == dog.idCompetitor) dog.code else 0
                    team.type = if (idCompetitor == dog.idCompetitor) TEAM_SINGLE_HANDLER else TEAM_LINKED_OTHER
                    team.post()
                } else {
                    throw Wobbly("Attempt to create team with unknown idDog=$idDog")
                }
            }
            return team.id
        }

        fun getIndividualNamedId(idDog: Int, competitorName: String): Int {
            val team = Team()
            val dog = Dog()
            dog.owner.joinToParent()
            team.competitor.joinToParent()

            dog.find(idDog)
            if (dog.found()) {
                team.select("idDog=$idDog AND teamType IN ($TEAM_SINGLE_HANDLER, $TEAM_LINKED_OTHER)")
                while (team.next()) {
                    if (competitorName eq team.competitor.fullName) {
                        return team.id
                    }
                }
                team.select("idDog=$idDog AND competitorName=${competitorName.quoted} AND teamType IN ($TEAM_NAMED_HANDLER)")
                if (team.first()) {
                    return team.id
                }
                team.append()
                team.idCompetitor = dog.idCompetitor
                team.idDog = idDog
                team.competitorName = competitorName
                team.type = TEAM_NAMED_HANDLER
                team.post()
                return team.id
            } else {
                throw Wobbly("Attempt to create team with unknown idDog=$idDog")
            }
        }

        fun getNextTeamCode(range: Int = 4): Int {
            var proposedCode = random(range * 10000 + 9999, range * 10000)
            var ok = false
            while (!ok) {
                proposedCode = random(range * 10000 + 9999, range * 10000)
                val query = DbQuery("SELECT teamCode FROM team WHERE teamCode=$proposedCode")
                ok = query.rowCount == 0
            }
            return proposedCode
        }

        fun getKcDual(idAccount: Int, idAgilityClass: Int, idDog: Int, dualHandler: String, classCode: Int = 0): Team {
            val team = Team()
            val dog = Dog(idDog)
            dog.owner.joinToParent()
            team.selectDualHandler(idAccount, idAgilityClass, idDog)
            if (!team.found()) {
                team.append()
                team.idAccount = idAccount
                team.idAgilityClass = idAgilityClass
                team.idDog = idDog
                team.idCompetitor = dog.idCompetitorHandler
                team.type = TEAM_DUAL_HANDLER
            }
            team.classCode = if (classCode > 0) classCode else AgilityClass(idAgilityClass).code
            team.idCompetitor = dog.idCompetitor
            team.idDog = idDog
            team.dualHandler = dualHandler
            team.post()
            return team
        }

        fun getKcTeam(idAccount: Int, idAgilityClass: Int, idDog: Int, teamName: String, clubName: String, members: JsonNode, classCode: Int = 0): Team {
            val team = Team()
            val dog = Dog()
            dog.owner.joinToParent()
            team.selectMultiple(idAccount, idAgilityClass, idDog)
            if (!team.found()) {
                team.append()
                team.idAccount = idAccount
                team.idAgilityClass = idAgilityClass
                team.idDog = idDog
                team.idCompetitor = dog.idCompetitorHandler
                team.type = TEAM_MULTIPLE
            }
            team.classCode = if (classCode > 0) classCode else AgilityClass(idAgilityClass).code
            team.teamName = teamName
            team.clubName = clubName
            team.members.setValue(members)
            team.refreshMembers()
            team.post()
            return team
        }

        fun getUkaTeamTeam(idAccount: Int, idAgilityClass: Int, idDog: Int, teamName: String, members: JsonNode): Team {
            val team = Team()
            val dog = Dog()
            dog.owner.joinToParent()
            team.selectMultiple(idAccount, idAgilityClass, idDog)
            if (!team.found()) {
                team.append()
                team.idAccount = idAccount
                team.idAgilityClass = idAgilityClass
                team.idDog = idDog
                team.type = TEAM_MULTIPLE
            }
            team.classCode = ClassTemplate.TEAM.code
            team.teamName = teamName
            team.members.setValue(members)
            team.refreshMembers()
            team.post()
            return team
        }

        fun getUkaPairsTeam(idAccount: Int, idAgilityClass: Int, idDog: Int, members: JsonNode): Team {
            val team = Team()
            val dog = Dog()
            dog.owner.joinToParent()
            team.selectMultiple(idAccount, idAgilityClass, idDog)
            if (!team.found()) {
                team.append()
                team.idAccount = idAccount
                team.idAgilityClass = idAgilityClass
                team.idDog = idDog
                team.type = TEAM_MULTIPLE
            }
            team.classCode = ClassTemplate.SPLIT_PAIRS.code
            team.members.setValue(members)
            team.refreshMembers()
            team.post()
            return team
        }
    }
}

