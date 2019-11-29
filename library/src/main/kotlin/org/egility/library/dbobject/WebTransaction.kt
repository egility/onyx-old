/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.library.dbobject

import org.egility.library.database.*
import org.egility.library.general.*
import java.util.*
import kotlin.collections.ArrayList

open class WebTransactionRaw<T : DbTable<T>>(_connection: DbConnection? = null, vararg columnNames: String) :
    DbTable<T>(_connection, "webTransaction", *columnNames) {
    var id: Int by DbPropertyInt("idWebTransaction")
    var idAccount: Int by DbPropertyInt("idAccount")
    var idCompetition: Int by DbPropertyInt("idCompetition")
    var idDog: Int by DbPropertyInt("idDog")
    var type: Int by DbPropertyInt("type")
    var data: Json by DbPropertyJson("data")
    var dateCreated: Date by DbPropertyDate("dateCreated")
    var deviceCreated: Int by DbPropertyInt("deviceCreated")
    var dateModified: Date by DbPropertyDate("dateModified")
    var deviceModified: Int by DbPropertyInt("deviceModified")

    open var kind: Int by DbPropertyJsonInt("data", "kind")
    open var entryType: Int by DbPropertyJsonInt("data", "entryType")
    open var competitors: JsonNode by DbPropertyJsonObject("data", "competitors")
    open var dogs: JsonNode by DbPropertyJsonObject("data", "dogs")
    open var entries: JsonNode by DbPropertyJsonObject("data", "entries")
    open var camping: JsonNode by DbPropertyJsonObject("data", "camping")
    open var items: JsonNode by DbPropertyJsonObject("data", "items")
    open var misc: JsonNode by DbPropertyJsonObject("data", "misc")
    open var payment: JsonNode by DbPropertyJsonObject("data", "payment")
    open var globalBenefits: JsonNode by DbPropertyJsonObject("data", "globalBenefits")
    open var actions: JsonNode by DbPropertyJsonObject("data", "actions")
    open var log: JsonNode by DbPropertyJsonObject("data", "log")

    open var stockMovementType: Int by DbPropertyJsonInt("data", "stockMovement.type")
    open var assetType: Int by DbPropertyJsonInt("data", "stockMovement.assetType")
    open var locationType: Int by DbPropertyJsonInt("data", "stockMovement.locationType")
    open var idCompetitionStock: Int by DbPropertyJsonInt("data", "stockMovement.idCompetition")
    open var locationText: String by DbPropertyJsonString("data", "stockMovement.locationText")
    open var assetCodes: String by DbPropertyJsonString("data", "stockMovement.assetCodes")

    val competition: Competition by DbLink<Competition>({ Competition() })
    val dog: Dog by DbLink<Dog>({ Dog() })
    val account: Account by DbLink<Account>({ Account() })

}

class WebTransaction(vararg columnNames: String) : WebTransactionRaw<WebTransaction>(null, *columnNames) {


    val isPaper: Boolean
        get() = entryType == ENTRY_PAPER

    fun clear() {
        data.clear()
    }

    fun getEntry(idAgilityClass: Int, idDog: Int, force: Boolean = false): JsonNode {
        for (entry in entries) {
            if (entry["idAgilityClass"].asInt == idAgilityClass && entry["idDog"].asInt == idDog) {
                return entry
            }
        }
        if (force) {
            val node = entries.addElement()
            node["idAgilityClass"] = idAgilityClass
            node["idDog"] = idDog
            return node
        }
        return Json.nullNode()
    }

    fun getDog(idDog: Int): JsonNode {
        for (dog in dogs) {
            if (dog["idDog"].asInt == idDog) {
                return dog
            }
        }
        return Json.nullNode()
    }

    fun hasEntry(idAgilityClass: Int, idDog: Int): Boolean {
        return !getEntry(idAgilityClass, idDog).isNull
    }

    fun enteredHeightCode(idAgilityClass: Int, idDog: Int): String {
        val entry = getEntry(idAgilityClass, idDog)
        return entry["heightCode"].asString
    }

    fun hasConfirmed(idAgilityClass: Int, idDog: Int): Boolean {
        val entry = getEntry(idAgilityClass, idDog)
        return entry["confirmed"].asBoolean
    }

    fun isInvited(idAgilityClass: Int, idDog: Int): Boolean {
        val entry = getEntry(idAgilityClass, idDog)
        return entry["invited"].asBoolean
    }

    fun changeHandler(idDog: Int, from: Int, to: Int) {
        for (entry in entries) {
            if (entry["idDog"].asInt == idDog && (entry["idCompetitor"].asInt == from || from == 0)) {
                entry["idCompetitor"] = to
            }
        }
    }

    fun LoadDogUka(competition: Competition, dog: Dog) {
        val competitionDog = CompetitionDog(competition.id, dog.id)

        val dogNode = dogs.searchElement("idDog", dog.id)
        dogNode["petName"] = dog.petName
        dogNode["idUka"] = dog.idUka
        dogNode["Handler"] = dog.handlerName
        dogNode["dateOfBirth"] = dog.dateOfBirth
        dogNode["uka.heightCode"] = dog.ukaHeightCode
        dogNode["uka.performanceLevel"] = dog.ukaPerformanceLevel
        dogNode["uka.steeplechaseLevel"] = dog.ukaSteeplechaseLevel
        dogNode["uka.dogRegistered"] = dog.isUkaRegistered
        dogNode["uka.handlerRegistered"] = dog.isHandlerUkaRegistered
        if (competitionDog.found()) {
            // class preferences
            dogNode["uka.performance"] = !competitionDog.ukaNotPerformance
            dogNode["uka.steeplechase"] = !competitionDog.ukaNotSteeplechase
            dogNode["uka.casual"] = competitionDog.ukaCasual
            dogNode["uka.nursery"] = competitionDog.ukaNursery
            dogNode["uka.junior"] = competitionDog.ukaJunior

            // height preferences
            dogNode["uka.heightCodePerformance"] = competitionDog.ukaHeightCodePerformance
            dogNode["uka.heightCodeSteeplechase"] = competitionDog.ukaHeightCodeSteeplechase
            dogNode["uka.heightCodeNursery"] = competitionDog.ukaHeightCodeNursery
            dogNode["uka.heightCodeCasual"] = competitionDog.ukaHeightCodeCasual
        } else {
            dogNode["uka.performance"] = !dog.ukaNotPerformance
            dogNode["uka.steeplechase"] = !dog.ukaNotSteeplechase
            dogNode["uka.casual"] = dog.ukaCasual
            dogNode["uka.nursery"] = dog.ukaNursery
            dogNode["uka.junior"] = dog.ukaJunior

            dogNode["uka.heightCodePerformance"] = dog.ukaHeightCodePerformance
            dogNode["uka.heightCodeSteeplechase"] = dog.ukaHeightCodeSteeplechase
            dogNode["uka.heightCodeNursery"] = dog.ukaHeightCodeNursery
            dogNode["uka.heightCodeCasual"] = dog.ukaHeightCodeCasual
        }
    }

    fun isEntered(idDog: Int): Boolean {
        for (entryNode in entries) {
            if (entryNode["idDog"].asInt == idDog) {
                return true
            }
        }
        return false
    }

    fun saveDogUka() {
        for (dogNode in dogs) {
            val idDog = dogNode["idDog"].asInt
            val competitionDog = CompetitionDog(idCompetition, idDog)
            if (!isEntered(idDog)) {
                if (competitionDog.found()) {
                    competitionDog.delete()
                }
            } else {
                if (!competitionDog.found()) {
                    competitionDog.append()
                    competitionDog.idCompetition = idCompetition
                    competitionDog.idDog = idDog
                    competitionDog.entryType = entryType
                }
                competitionDog.idAccount = idAccount
                competitionDog.ukaPerformanceLevel = dogNode["uka.performanceLevel"].asString
                competitionDog.ukaSteeplechaseLevel = dogNode["uka.steeplechaseLevel"].asString

                competitionDog.ukaNotPerformance = !dogNode["uka.performance"].asBoolean
                competitionDog.ukaNotSteeplechase = !dogNode["uka.steeplechase"].asBoolean

                competitionDog.ukaCasual = dogNode["uka.casual"].asBoolean
                competitionDog.ukaNursery = dogNode["uka.nursery"].asBoolean
                competitionDog.ukaJunior = dogNode["uka.junior"].asBoolean

                competitionDog.ukaHeightCodePerformance = dogNode["uka.heightCodePerformance"].asString
                competitionDog.ukaHeightCodeSteeplechase = dogNode["uka.heightCodeSteeplechase"].asString
                competitionDog.ukaHeightCodeNursery = dogNode["uka.heightCodeNursery"].asString
                competitionDog.ukaHeightCodeCasual = dogNode["uka.heightCodeCasual"].asString

                competitionDog.ukaHeightCode = dogNode["uka.heightCode"].asString

                competitionDog.post()
            }
        }
    }

    fun LoadDogKc(competition: Competition, dog: Dog) {
        val tooYoung = dog.dateOfBirth.isNotEmpty() && dog.dateOfBirth.addMonths(4) > competition.dateEnd

        if (!tooYoung) {
            val competitionDog = CompetitionDog(competition.id, dog.id)
            val dogNode = dogs.searchElement("idDog", dog.id)
            val isPuppy = dog.dateOfBirth.isNotEmpty() && dog.dateOfBirth.addMonths(18) > competition.dateEnd

            var handler = if (dog.idCompetitorHandler > 0) dog.handler.fullName else dog.owner.fullName
            var idCompetitorHandler = dog.idCompetitorHandler

            if (competitionDog.found() && !competitionDog.nfc) {
                if (competitionDog.kcHandlerId > 0) {
                    idCompetitorHandler = competitionDog.kcHandlerId
                    handler = competitionDog.kcHandler
                } else {
                    if (competitionDog.kcHandler != handler) {
                        idCompetitorHandler = competitionDog.kcHandlerId
                        handler = competitionDog.kcHandler
                    }
                }
            }

            val effectiveHeightCode =
                if (!competition.kc2020Rules && dog.kcHeightCode == "KC500") "KC650" else dog.kcHeightCode
            var effectiveEntryOption = dog.kcEntryOption
            if (competition.kc2020Rules) {
                effectiveEntryOption = effectiveEntryOption.replace("L", "")
                if (dog.kcHeightCode == "KC500") effectiveEntryOption = effectiveEntryOption.replace("650", "500")
            }

            dogNode["petName"] = if (entryType == ENTRY_PAPER) "${dog.registeredName}" else dog.petName
            dogNode["registered"] = dog.kcRegistered
            dogNode["hasKc"] = dog.idKC.isNotEmpty()
            dogNode["retired"] = dog.state == DOG_RETIRED
            dogNode["isPuppy"] = isPuppy
            dogNode["idCompetitor"] = dog.idCompetitor
            dogNode["idCompetitorHandler"] = idCompetitorHandler
            dogNode["heightCode"] = effectiveHeightCode
            //dogNode["heightCode"] =  dog.kcHeightCode
            dogNode["gradeCode"] = dog.kcEffectiveGradeCode(competition.dateStart)
            dogNode["hasKcChampWins"] = dog.hasKcChampWins
            dogNode["handler"] = handler

            dogNode["entryOption"] =
                if (competitionDog.found()) {
                    if (competitionDog.nfc) "NFC" else competitionDog.kcJumpHeightCode
                } else {
                    if ((dog.state == DOG_RETIRED || !dog.kcRegistered || isPuppy) && effectiveEntryOption neq "NFC") {
                        "NE"
                    } else if (effectiveEntryOption.isEmpty()) {
                        effectiveHeightCode
                    } else {
                        effectiveEntryOption
                    }
                }
        }

    }

    fun LoadDogInd(competition: Competition, dog: Dog) {
        val tooYoung =
            dog.dateOfBirth.isNotEmpty() && dog.dateOfBirth.addMonths(competition.minMonths) > competition.dateEnd

        if (!tooYoung) {
            val competitionDog = CompetitionDog(competition.id, dog.id)
            val dogNode = dogs.searchElement("idDog", dog.id)

            var handler = if (dog.idCompetitorHandler > 0) dog.handler.fullName else dog.owner.fullName
            var idCompetitorHandler = dog.idCompetitorHandler

            var heightCode = Height.indHeightFromKc(competition.independentType, if (dog.kcHeightCode.isEmpty()) "KC650" else dog.kcHeightCode)
            var jumpHeightCode = heightCode
            var clearRoundOnly = false
            var secondChance = false
            var bonusCategories = ""
            var g7Owner = false
            var bundle = 1
            var gradeCode = ""
            if (competitionDog.found()) {
                if (competitionDog.indHandlerId > 0) {
                    idCompetitorHandler = competitionDog.indHandlerId
                    handler = competitionDog.indHandler
                } else {
                    if (competitionDog.indHandler != handler) {
                        idCompetitorHandler = competitionDog.indHandlerId
                        handler = competitionDog.indHandler
                    }
                }
                gradeCode = competitionDog.indGradeCode
                jumpHeightCode = competitionDog.indJumpHeightCode
                clearRoundOnly = competitionDog.indClearRoundOnly
                secondChance = competitionDog.indSecondChance
                bonusCategories = competitionDog.indBonusCategories
                g7Owner = competitionDog.indG7Owner
                bundle = competitionDog.indBundle + 1
            }

            dogNode["petName"] = if (entryType == ENTRY_PAPER) "${dog.registeredName}" else dog.petName
            dogNode["idCompetitor"] = dog.idCompetitor
            dogNode["idCompetitorHandler"] = idCompetitorHandler
            dogNode["kcHeightCode"] = if (dog.kcHeightCode.isEmpty()) "unknown" else dog.kcHeightCode
            dogNode["ukaHeightCode"] = if (!dog.ukaRegistered) "unknown" else dog.ukaHeightCode
            dogNode["kcGradeCode"] = if (dog.kcGradeCode.isEmpty()) "KC00" else dog.kcGradeCode
            dogNode["heightCode"] = heightCode
            dogNode["jumpHeightCode"] = jumpHeightCode
            dogNode["clearRoundOnly"] = clearRoundOnly
            dogNode["secondChance"] = secondChance
            dogNode["bonusCategories"] = bonusCategories
            dogNode["g7Owner"] = g7Owner
            dogNode["bundle"] = bundle
            dogNode["jumpOption"] = jumpHeightCode + if (clearRoundOnly) "C" else ""
            dogNode["gradeCode"] = if (gradeCode.isNotEmpty()) gradeCode else Grade.indGradeFromKc(competition.independentType, dog.kcGradeCode)
            dogNode["handler"] = handler
        }
    }
    
    fun activeAgilityEntries() {
        val agilityClass=AgilityClass().select("idCompetition=$idCompetition")
        entries.clear()
        for (dogNode in dogs) {
            val bundle = dogNode["bundle"].asInt - 1
            val idCompetitorHandler = dogNode["idCompetitorHandler"].asInt
            val HandlerNode = competitors.searchElement("idCompetitor", idCompetitorHandler)
            val voucherBenefits = HandlerNode["voucherBenefits"]
            val memberRates = voucherBenefits["memberRates"].asBoolean
            
            agilityClass.forEach {
                var runCount = 0
                if (it.name.toLowerCase().contains("agility") || it.name.toLowerCase().contains("jumping")) {
                    runCount = if (bundle==1) 1 else if (bundle==3) 2 else 0
                }
                if (it.name.toLowerCase().contains("steeplechase")) {
                    runCount = if (bundle==1) 1 else if (bundle==2) 2 else 0
                }
                if (runCount>0) {
                    LoadEntry(
                        idAgilityClass=it.id,
                        idDog = dogNode["idDog"].asInt,
                        idCompetitor = dogNode["idCompetitorHandler"].asInt,
                        gradeCode = "xxx",
                        heightCode = dogNode["jumpOption"].asString.replace("C", ""),
                        jumpHeightCode = dogNode["jumpOption"].asString.replace("C", ""),
                        entryFee = if (memberRates) competition.entryFeeMembers else competition.entryFee,
                        petName = dogNode["petName"].asString,
                        className = it.name,
                        classDate = it.date,
                        classCode = it.code,
                        classNumber = it.number,
                        subDivision = 0,
                        clearRoundOnly = dogNode["jumpOption"].asString.contains("C") || bundle == 3,
                        runsEntered = runCount
                    )
                }
            }
        }
    }

    fun LoadDogFab(competition: Competition, dog: Dog) {
        val competitionDog = CompetitionDog(competition.id, dog.id)
        val dogNode = dogs.searchElement("idDog", dog.id)
        val isPuppy = dog.dateOfBirth.isNotEmpty() && dog.dateOfBirth.addMonths(18) > competition.dateEnd

        dogNode["petName"] = dog.petName
        dogNode["retired"] = dog.state == DOG_RETIRED
        dogNode["isPuppy"] = isPuppy
        dogNode["idCompetitor"] = dog.idCompetitor
        dogNode["idCompetitorHandler"] = dog.idCompetitorHandler

        if (competitionDog.found()) {
            dogNode["heightCode"] = competitionDog.fabHeightCode
            dogNode["gradeAgility"] = competitionDog.fabGradeAgility
            dogNode["gradeJumping"] = competitionDog.fabGradeJumping
            dogNode["gradeSteeplechase"] = competitionDog.fabGradeSteeplechase
            dogNode["collie"] = competitionDog.fabCollie
            dogNode["ifcsHeightCode"] = competitionDog.ifcsHeightCode
        } else {
            dogNode["heightCode"] = dog.fabHeightCode
            dogNode["gradeAgility"] = dog.fabGradeAgility
            dogNode["gradeJumping"] = dog.fabGradeJumping
            dogNode["gradeSteeplechase"] = dog.fabGradeSteeplechase
            dogNode["collie"] = dog.fabCollie
            dogNode["ifcsHeightCode"] = dog.ifcsHeightCode
        }

        dogNode["fab.agility"] = !dog.fabNotAgility
        dogNode["fab.jumping"] = !dog.fabNotJumping
        dogNode["fab.steeplechase"] = !dog.fabNotSteeplechase
        dogNode["fab.grandPrix"] = dog.fabGrandPrix
        dogNode["fab.allsorts"] = dog.fabAllsorts
        dogNode["fab.ifcs"] = dog.fabIfcs
    }


    fun saveDogFab() {
        for (dogNode in dogs) {
            val idDog = dogNode["idDog"].asInt
            val competitionDog = CompetitionDog(idCompetition, idDog)
            if (!isEntered(idDog)) {
                if (competitionDog.found()) {
                    competitionDog.delete()
                }
            } else {
                if (!competitionDog.found()) {
                    competitionDog.append()
                    competitionDog.idCompetition = idCompetition
                    competitionDog.idDog = idDog
                    competitionDog.entryType = entryType
                }
                competitionDog.idAccount = idAccount
                competitionDog.fabHeightCode = dogNode["heightCode"].asString
                competitionDog.fabGradeAgility = dogNode["gradeAgility"].asString
                competitionDog.fabGradeJumping = dogNode["gradeJumping"].asString
                competitionDog.fabGradeSteeplechase = dogNode["gradeSteeplechase"].asString
                competitionDog.fabCollie = dogNode["collie"].asBoolean
                competitionDog.ifcsHeightCode = dogNode["ifcsHeightCode"].asString
                competitionDog.post()
            }
        }
    }

    fun LoadCompetitorKc(competition: Competition, competitor: Competitor) {
        val competitionCompetitor = CompetitionCompetitor(competition.id, competitor.id)
        val competitorNode = competitors.searchElement("idCompetitor", competitor.id)
        competitorNode["idCompetitor"] = competitor.id
        competitorNode["name"] = competitor.fullName
        competitorNode["dateOfBirth"] = competitor.dateOfBirth
        competitorNode["ykcMember"] = competitor.ykcMember
        competitorNode["skills"] = competitor.skills
        if (competitionCompetitor.found()) {
            competitorNode["voucherCode"] = competitionCompetitor.voucherCode
            competitorNode["helpGroup"] = competitionCompetitor.helpGroup
            for (day in competition.agilityClassDates.split(",")) {
                val date = day.toDate()
                val dayNode = competitionCompetitor.helpDays.searchElement("date", date, true)
                competitorNode["helpDays"].addElement().setValue(dayNode)
            }
        } else {
            for (day in competition.agilityClassDates.split(",")) {
                val element = competitorNode["helpDays"].addElement()
                element["date"] = day.toDate()
            }
        }
    }

    fun LoadCompetitorInd(competition: Competition, competitor: Competitor) {
        val competitionCompetitor = CompetitionCompetitor(competition.id, competitor.id)
        val competitorNode = competitors.searchElement("idCompetitor", competitor.id)
        competitorNode["idCompetitor"] = competitor.id
        competitorNode["name"] = competitor.fullName
        competitorNode["dateOfBirth"] = competitor.dateOfBirth
        competitorNode["skills"] = competitor.skills
        if (competitionCompetitor.found()) {
            competitorNode["voucherCode"] = competitionCompetitor.voucherCode
            competitorNode["helpGroup"] = competitionCompetitor.helpGroup
            for (day in competition.agilityClassDates.split(",")) {
                val date = day.toDate()
                val dayNode = competitionCompetitor.helpDays.searchElement("date", date, true)
                competitorNode["helpDays"].addElement().setValue(dayNode)
            }
        } else {
            for (day in competition.agilityClassDates.split(",")) {
                val element = competitorNode["helpDays"].addElement()
                element["date"] = day.toDate()
            }
        }
    }

    fun LoadCompetitorUka(competition: Competition, competitor: Competitor) {
        val competitionCompetitor = CompetitionCompetitor(competition.id, competitor.id)
        val competitorNode = competitors.searchElement("idCompetitor", competitor.id)
        competitorNode["idCompetitor"] = competitor.id
        competitorNode["name"] = competitor.fullName
        competitorNode["dateOfBirth"] = competitor.dateOfBirth
        competitorNode["skills"] = competitor.skills
        if (competitionCompetitor.found()) {
            competitorNode["voucherCode"] = competitionCompetitor.voucherCode
            competitorNode["helpGroup"] = competitionCompetitor.helpGroup
            for (day in competition.agilityClassDates.split(",")) {
                val date = day.toDate()
                val dayNode = competitionCompetitor.helpDays.searchElement("date", date, true)
                competitorNode["helpDays"].addElement().setValue(dayNode)
            }
        } else {
            for (day in competition.agilityClassDates.split(",")) {
                val element = competitorNode["helpDays"].addElement()
                element["date"] = day.toDate()
            }
        }
    }

    fun LoadCompetitorFab(competition: Competition, competitor: Competitor) {
        val competitionCompetitor = CompetitionCompetitor(competition.id, competitor.id)
        val competitorNode = competitors.searchElement("idCompetitor", competitor.id)
        competitorNode["idCompetitor"] = competitor.id
        competitorNode["name"] = competitor.fullName
        competitorNode["dateOfBirth"] = competitor.dateOfBirth
        competitorNode["skills"] = competitor.skills
        if (competitionCompetitor.found()) {
            competitorNode["voucherCode"] = competitionCompetitor.voucherCode
            competitorNode["helpGroup"] = competitionCompetitor.helpGroup
            for (day in competition.agilityClassDates.split(",")) {
                val date = day.toDate()
                val dayNode = competitionCompetitor.helpDays.searchElement("date", date, true)
                competitorNode["helpDays"].addElement().setValue(dayNode)
            }
        } else {
            for (day in competition.agilityClassDates.split(",")) {
                val element = competitorNode["helpDays"].addElement()
                element["date"] = day.toDate()
            }
        }
    }

    fun saveCompetitorKC() {
        for (competitorNode in competitors) {
            val idCompetitor = competitorNode["idCompetitor"].asInt
            val competitor = Competitor(idCompetitor)
            competitor.skills = competitorNode["skills"]
            competitor.post()
            val competitionCompetitor = CompetitionCompetitor(idCompetition, idCompetitor)
            if (!competitionCompetitor.found()) {
                competitionCompetitor.append()
                competitionCompetitor.idCompetition = idCompetition
                competitionCompetitor.idCompetitor = idCompetitor
            }
            competitionCompetitor.idAccount = idAccount
            competitionCompetitor.voucherCode = competitorNode["voucherCode"].asString
            competitionCompetitor.helpGroup = competitorNode["helpGroup"].asString
            competitionCompetitor.helpDays = competitorNode["helpDays"]
            competitionCompetitor.post()
        }

    }

    fun saveCompetitorInd() {
        for (competitorNode in competitors) {
            val idCompetitor = competitorNode["idCompetitor"].asInt
            val competitor = Competitor(idCompetitor)
            competitor.skills = competitorNode["skills"]
            competitor.post()
            val competitionCompetitor = CompetitionCompetitor(idCompetition, idCompetitor)
            if (!competitionCompetitor.found()) {
                competitionCompetitor.append()
                competitionCompetitor.idCompetition = idCompetition
                competitionCompetitor.idCompetitor = idCompetitor
            }
            competitionCompetitor.idAccount = idAccount
            competitionCompetitor.voucherCode = competitorNode["voucherCode"].asString
            competitionCompetitor.helpGroup = competitorNode["helpGroup"].asString
            competitionCompetitor.helpDays = competitorNode["helpDays"]
            competitionCompetitor.post()
        }

    }

    fun saveCompetitorUka() {
        for (competitorNode in competitors) {
            val idCompetitor = competitorNode["idCompetitor"].asInt
            val competitor = Competitor(idCompetitor)
            competitor.skills = competitorNode["skills"]
            competitor.post()
            val competitionCompetitor = CompetitionCompetitor(idCompetition, idCompetitor)
            if (!competitionCompetitor.found()) {
                competitionCompetitor.append()
                competitionCompetitor.idCompetition = idCompetition
                competitionCompetitor.idCompetitor = idCompetitor
            }
            competitionCompetitor.idAccount = idAccount
            competitionCompetitor.voucherCode = competitorNode["voucherCode"].asString
            competitionCompetitor.helpGroup = competitorNode["helpGroup"].asString
            competitionCompetitor.helpDays = competitorNode["helpDays"]
            competitionCompetitor.post()
        }

    }

    fun saveCompetitorFab() {
        for (competitorNode in competitors) {
            val idCompetitor = competitorNode["idCompetitor"].asInt
            val competitor = Competitor(idCompetitor)
            competitor.skills = competitorNode["skills"]
            competitor.post()
            val competitionCompetitor = CompetitionCompetitor(idCompetition, idCompetitor)
            if (!competitionCompetitor.found()) {
                competitionCompetitor.append()
                competitionCompetitor.idCompetition = idCompetition
                competitionCompetitor.idCompetitor = idCompetitor
            }
            competitionCompetitor.idAccount = idAccount
            competitionCompetitor.voucherCode = competitorNode["voucherCode"].asString
            competitionCompetitor.helpGroup = competitorNode["helpGroup"].asString
            competitionCompetitor.helpDays = competitorNode["helpDays"]
            competitionCompetitor.post()
        }

    }

    fun saveDogKc() {
        for (dogNode in dogs) {
            val idDog = dogNode["idDog"].asInt
            val competitionDog = CompetitionDog(idCompetition, idDog)
            val markedNotEntered = dogNode["entryOption"].asString == "NE"
            val markedNfc = dogNode["entryOption"].asString == "NFC"
            if (markedNotEntered || (!markedNfc && !isEntered(idDog))) {
                if (markedNotEntered) {
                    Dog().seek(id) { kcEntryOption = "NE"; post() }
                }
                if (competitionDog.found()) {
                    competitionDog.delete()
                }
            } else {
                if (!competitionDog.found()) {
                    competitionDog.append()
                    competitionDog.idCompetition = idCompetition
                    competitionDog.idDog = dogNode["idDog"].asInt
                    competitionDog.entryType = entryType
                }
                competitionDog.idAccount = idAccount
                if (competitionDog.ringNumber == 0) {
                    competitionDog.ringNumber = Dog(competitionDog.idDog).code
                }
                if (dogNode["entryOption"].asString eq "NFC") {
                    competitionDog.nfc = true
                    competitionDog.kcHandlerId = 0
                    competitionDog.kcHandler = ""
                    competitionDog.kcHeightCode = ""
                    competitionDog.kcGradeCode = ""
                    competitionDog.kcJumpHeightCode = ""
                } else {
                    competitionDog.nfc = false
                    competitionDog.kcHandlerId = dogNode["idCompetitorHandler"].asInt
                    competitionDog.kcHandler = dogNode["handler"].asString
                    competitionDog.kcHeightCode = dogNode["heightCode"].asString
                    competitionDog.kcGradeCode = dogNode["gradeCode"].asString
                    competitionDog.kcJumpHeightCode = dogNode["entryOption"].asString
                }
                competitionDog.post()
                val dog = Dog(dogNode["idDog"].asInt)
                if (dog.found()) {
                    dog.idCompetitorHandler = dogNode["idCompetitorHandler"].asInt
                    dog.kcEntryOption = dogNode["entryOption"].asString
                    dog.post()
                }
            }
        }
    }

    fun saveDogInd() {
        val secondChanceDogs=ArrayList<Int>()
        if (competition.secondChance) {
            for (item in items) {
                if (item["type"].asInt == LEDGER_ITEM_SECOND_CHANCE) {
                    secondChanceDogs.add(item["idDog"].asInt)
                }
            }
        }
        for (dogNode in dogs) {
            val idDog = dogNode["idDog"].asInt
            val competitionDog = CompetitionDog(idCompetition, idDog)
            if (!competitionDog.found()) {
                competitionDog.append()
                competitionDog.idCompetition = idCompetition
                competitionDog.idDog = dogNode["idDog"].asInt
                competitionDog.entryType = entryType
            }
            competitionDog.idAccount = idAccount
            if (competitionDog.ringNumber == 0) {
                competitionDog.ringNumber = Dog(competitionDog.idDog).code
            }
            competitionDog.indHandlerId = dogNode["idCompetitorHandler"].asInt
            competitionDog.indHandler = dogNode["handler"].asString
            competitionDog.indGradeCode = dogNode["gradeCode"].asString
            competitionDog.indJumpHeightCode = dogNode["jumpHeightCode"].asString
            competitionDog.indClearRoundOnly = dogNode["clearRoundOnly"].asBoolean
            competitionDog.indSecondChance = secondChanceDogs.contains(idDog)
            competitionDog.indBonusCategories = dogNode["bonusCategories"].asString
            competitionDog.indG7Owner = dogNode["g7Owner"].asBoolean
            competitionDog.indBundle = dogNode["bundle"].asInt
            competitionDog.post()
        }
    }


    fun dogDefault(idDog: Int) {
        val dogNode = dogs.searchElement("idDog", idDog, create = false)
        if (!dogNode.isNull) {
            val dog = Dog(idDog)

            if (competition.isUka) {
                dog.ukaNotPerformance = !dogNode["uka.performance"].asBoolean
                dog.ukaNotSteeplechase = !dogNode["uka.steeplechase"].asBoolean
                dog.ukaCasual = dogNode["uka.casual"].asBoolean
                dog.ukaNursery = dogNode["uka.nursery"].asBoolean
                dog.ukaJunior = dogNode["uka.junior"].asBoolean

                dog.ukaHeightCodePerformance = dogNode["uka.heightCodePerformance"].asString
                dog.ukaHeightCodeSteeplechase = dogNode["uka.heightCodeSteeplechase"].asString
                dog.ukaHeightCodeNursery = dogNode["uka.heightCodeNursery"].asString
                dog.ukaHeightCodeCasual = dogNode["uka.heightCodeCasual"].asString
            } else if (competition.isFab) {

                dog.fabHeightCode = dogNode["heightCode"].asString
                dog.fabGradeAgility = dogNode["gradeAgility"].asString
                dog.fabGradeJumping = dogNode["gradeJumping"].asString
                dog.fabGradeSteeplechase = dogNode["gradeSteeplechase"].asString
                dog.ifcsHeightCode = dogNode["ifcsHeightCode"].asString

                dog.fabNotAgility = !dogNode["fab.agility"].asBoolean
                dog.fabNotJumping = !dogNode["fab.jumping"].asBoolean
                dog.fabNotSteeplechase = !dogNode["fab.steeplechase"].asBoolean
                dog.fabGrandPrix = dogNode["fab.grandPrix"].asBoolean
                dog.fabAllsorts = dogNode["fab.allsorts"].asBoolean
                dog.fabIfcs = dogNode["fab.ifcs"].asBoolean
            }
            dog.post()
        }

    }

    fun LoadEntry(
        idAgilityClass: Int,
        idDog: Int,
        idCompetitor: Int,
        gradeCode: String,
        heightCode: String,
        jumpHeightCode: String,
        entryFee: Int,
        petName: String,
        className: String,
        classDate: Date,
        classCode: Int,
        classNumber: Int = 0,
        dualHandler: String = "",
        confirmed: Boolean = true,
        invited: Boolean = true,
        subDivision: Int = 0,
        idTeam: Int = 0,
        clearRoundOnly: Boolean = false,
        runsEntered: Int = 1
    ): JsonNode {
        val node = getEntry(idAgilityClass, idDog, true)
        node["idCompetitorOld"] = node["idCompetitor"].asInt
        node["idCompetitor"] = idCompetitor
        node["gradeCode"] = gradeCode
        node["heightCode"] = heightCode
        node["jumpHeightCode"] = jumpHeightCode
        node["entryFee"] = entryFee
        node["petName"] = petName
        node["className"] = className
        node["classDate"] = classDate
        node["classCode"] = classCode
        node["classNumber"] = classNumber
        node["dualHandler"] = dualHandler
        node["confirmed"] = confirmed
        node["invited"] = invited
        node["subDivision"] = subDivision
        node["clearRoundOnly"] = clearRoundOnly
        node["runsEntered"] = runsEntered
        if (idTeam > 0) node["idTeam"] = idTeam
        return node
    }

    fun unConfrmEntry(idAgilityClass: Int, idDog: Int) {
        val node = getEntry(idAgilityClass, idDog, false)
        node["confirmed"] = false
    }

    fun getCamping(campingDate: Date = nullDate, force: Boolean = false): JsonNode {
        for (camping in camping) {
            if ((campingDate.isEmpty() && camping["wholeShow"].asBoolean) || (camping["campingDate"].asDate == campingDate)) {
                return camping
            }
        }
        if (force) {
            val node = camping.addElement()
            if (campingDate.isEmpty()) {
                node["wholeShow"] = true
            } else {
                node["campingDate"] = campingDate
            }
            return node
        }
        return Json.nullNode()
    }


    fun bookCamping(
        pitches: Int,
        pitchType: Int,
        baseDate: Date,
        dayFlags: Int,
        feeOverride: Int = -1,
        groupName: String = ""
    ) {
        camping.clear()
        camping["feeOverride"] = feeOverride
        camping["groupName"] = groupName
        camping["pitches"] = pitches
        camping["pitchType"] = pitchType
        camping["baseDate"] = baseDate
        camping["dayFlags"] = dayFlags
    }

    fun getItem(type: Int, code: Int = 0, idDog: Int=0, idCompetitor: Int=0, size: String = "", force: Boolean = false): JsonNode {
        for (item in data["items"]) {
            if (item["type"].asInt == type && item["code"].asInt == code && item["idDog"].asInt == idDog && item["idCompetitor"].asInt == idCompetitor && item["size"].asString == size) {
                return item
            }
        }
        if (force) {
            val node = data["items"].addElement()
            node["type"] = type
            node["code"] = code
            node["idDog"] = idDog
            node["idCompetitor"] = idCompetitor
            node["size"] = size
            return node
        }
        return Json.nullNode()
    }

    fun hasItem(type: Int, code: Int = 0, idDog: Int=0, idCompetitor: Int=0, size: String = ""): Boolean {
        return getItem(type, code, idDog, idCompetitor, size).isNotNull
    }

    fun deleteItem(type: Int, code: Int = 0, idDog: Int=0, idCompetitor: Int=0, size: String = "") {
        for (item in items) {
            if (item["type"].asInt == type && item["code"].asInt == code && item["idDog"].asInt == idDog && item["idCompetitor"].asInt == idCompetitor && item["size"].asString == size) {
                items.drop(items.indexOf(item))
                return
            }
        }
    }

    fun LoadItem(type: Int, quantity: Int, unitPrice: Int, code: Int = 0, idDog: Int=0, idCompetitor: Int=0, size: String = "") {
        if (quantity * unitPrice == 0) {
            deleteItem(type, code, idDog, idCompetitor, size)
        } else {
            val node = getItem(type, code, idDog, idCompetitor, size, true)
            node["quantity"] = quantity
            node["unitPrice"] = unitPrice
            node["itemFee"] = quantity * unitPrice
            node["code"] = code
            node["size"] = size
        }
    }

    fun seekEntry(idAccount: Int, idCompetition: Int, force: Boolean = false) {
        if (!find("idAccount=${idAccount} AND idCompetition=${idCompetition} AND type=$WEB_TRANSACTION_ENTRY") && force) {
            append()
            this.idAccount = idAccount
            this.idCompetition = idCompetition
            this.type = WEB_TRANSACTION_ENTRY
        }
    }

    fun seekDogKcGradeChange(idDog: Int, force: Boolean = false) {
        if (!find("idDog=${idDog} AND type=$WEB_TRANSACTION_GRADE") && force) {
            append()
            this.idDog = idDog
            this.type = WEB_TRANSACTION_GRADE
        }
    }

    fun seekUkaRegistration(idAccount: Int, force: Boolean = false) {
        if (!find("idAccount=${idAccount} AND type=$WEB_TRANSACTION_UKA") && force) {
            append()
            this.idAccount = idAccount
            this.type = WEB_TRANSACTION_UKA
        }
    }

    fun seekStock(idAccount: Int, force: Boolean = false) {
        if (!find("idAccount=${idAccount} AND type=$WEB_TRANSACTION_STOCK") && force) {
            append()
            this.idAccount = idAccount
            this.type = WEB_TRANSACTION_STOCK
        }
    }

    fun hasCamping(): Boolean {
        return if (data.has("camping")) {
            when {
                kind == ENTRY_UK_OPEN -> true
                else -> camping["dayFlags"].asInt != 0
            }
        } else {
            false
        }
    }

    fun hasCamping(start: Date, end: Date = start): Boolean {
        if (hasCamping()) {
            var campingDate = start
            while (campingDate <= end) {
                val bit = campingDate.daysSince(camping["baseDate"].asDate)
                if (camping["dayFlags"].asInt.isBitSet(bit)) return true
                campingDate = campingDate.addDays(1)
            }
        }
        return false
    }

    fun handlerValues(): JsonNode {
        val node = Json()
        for (competitorNode in competitors) {
            addOption(node, competitorNode["idCompetitor"].asInt.toString(), competitorNode["name"].asString)
        }
        return node
    }

    fun handlerValuesAgeRestricted(agilityClass: AgilityClass): JsonNode {
        val node = Json()
        for (competitorNode in competitors) {
            if (agilityClass.inAgeRange(competitorNode["dateOfBirth"].asDate)) {
                if (!agilityClass.template.isYkc || competitorNode["ykcMember"].asBoolean) {
                    addOption(node, competitorNode["idCompetitor"].asInt.toString(), competitorNode["name"].asString)
                }
            }
        }
        return node
    }

    fun entryOptionValues(competition: Competition): JsonNode {
        val node = Json()
        for (heightCode in "KC650,KC500,KC450,KC350".split(",")) {
            val options = node[heightCode]
            for (heightOption in competition.getHeightOptions(heightCode)) {
                when (heightOption) {
                    "KC650", "KC500", "KC450", "KC350" -> if (competition.kc2020Rules)
                        WebTransaction.addOption(options, heightOption, Height.getHeightName(heightOption))
                    else
                        WebTransaction.addOption(options, heightOption, "Full Height")
                    "KC650L", "KC450L", "KC350L" -> WebTransaction.addOption(options, heightOption, "Lower Height")
                    "X999" -> WebTransaction.addOption(options, heightOption, "Any Size")
                    else -> if (heightOption.startsWith("X"))
                        WebTransaction.addOption(
                            options,
                            heightOption,
                            "Any Size " + Height.getHeightJumpName(heightOption)
                        )
                    else
                        WebTransaction.addOption(options, heightOption, Height.getHeightJumpName(heightOption))
                }
            }
            WebTransaction.addOption(options, "NFC", "Enter As NFC")
            WebTransaction.addOption(options, "NE", "Not Entering")
        }

        val options = node["other"]
        WebTransaction.addOption(options, "NFC", "Enter As NFC")
        WebTransaction.addOption(options, "NE", "Not Entering")

        return node
    }

    fun heightValues(competition: Competition): JsonNode {
        val node = Json()
        if (competition.isIndependent) {
            for (heightCode in Height.indHeights(competition.independentType)) {
                val height = node.addElement()
                height["value"] = heightCode
                height["description"] = Height.getHeightName(heightCode)
            }
        }
        return node
    }

    fun gradeValues(competition: Competition): JsonNode {
        val node = Json()
        if (competition.isIndependent) {
            for (gradeCode in Grade.indGrades(competition.independentType)) {
                val grade = node.addElement()
                grade["value"] = gradeCode
                grade["description"] = Grade.getGradeName(gradeCode)
            }
        }
        return node
    }

    fun entryValues(competition: Competition, dog: JsonNode): JsonNode {
        val node = Json()
        
        fun addItem(value: Int, description: String) {
            val item = node.addElement()
            item["value"] = value
            item["description"] = description
        }
        
        if (competition.independentType.eq("aa")) {
            if (dog["kcGradeCode"].asString<="KC05") {
                addItem(1, "* Not Entered *")
                addItem(2, "1. Standard")
                addItem(3, "2. Steeplechase")
                addItem(4, "3. Practice")
            } else {
                addItem(1, "* Not Eligible *")
            }
        }
        return node
    }

    fun confirmUka(): Boolean {
        var paper = data["paper"].asBoolean
        var feesStillOwing = false
        dbTransaction {
            Ledger().seek("idAccount=$idAccount AND type IN ($LEDGER_UKA_REGISTRATION, $LEDGER_UKA_REGISTRATION_DIRECT) AND amount=0") {
                wipe()
            }
            var totalFee = 0
            val ledger = Ledger()
            ledger.append()
            ledger.idAccount = idAccount
            if (paper) {
                ledger.type = LEDGER_UKA_REGISTRATION_DIRECT
                ledger.debit = ACCOUNT_UKA_MEMBER
                ledger.credit = ACCOUNT_UKA_HOLDING_DIRECT
            } else {
                ledger.type = LEDGER_UKA_REGISTRATION
                ledger.dueImmediately = true
                ledger.debit = ACCOUNT_USER
                ledger.credit = ACCOUNT_UKA_HOLDING
            }
            ledger.dateEffective = today
            ledger.preparing = true
            ledger.post()
            for (action in data["actions.competitors"]) {
                val idCompetitor = action["idCompetitor"].asInt
                val type = action["action"].asInt
                val description = "${action["name"].asString} - ${action["description"].asString}"
                val fee = if (paper) action["paperFee"].asInt else action["fee"].asInt
                val confirmed = action["confirmed"].asBoolean
                if (confirmed) {
                    totalFee += fee
                    ledger.addUkaMemberItem(idCompetitor, type, description, fee)
                }
            }
            for (action in data["actions.dogs"]) {
                val idCompetitor = action["idCompetitor"].asInt
                val idDog = action["idDog"].asInt
                val type = action["action"].asInt
                val description = "${action["name"].asString} - ${action["description"].asString}"
                val fee = if (paper) action["paperFee"].asInt else action["fee"].asInt
                val confirmed = action["confirmed"].asBoolean
                val ukaHeightCode = action["ukaHeightCode"].asString
                val ukaGradeCode = when (action["kcGradeCode"].asString) {
                    "KC04", "KC05" -> "UKA02"
                    "KC06", "KC07" -> "UKA03"
                    else -> "UKA01"
                }
                if (confirmed) {
                    totalFee += fee
                    ledger.addUkaDogItem(idDog, type, description, ukaHeightCode, ukaGradeCode, fee)
                }
            }
            ledger.charge = totalFee
            if (paper) ledger.amount = totalFee
            ledger.payFromFunds(Ledger.balance(idAccount))
            ledger.processUka(ledger.amountOwing > 0)
            feesStillOwing = ledger.amountOwing > 0
        }
        return feesStillOwing
    }

    companion object {

        val ENTRY_KC = 1
        val ENTRY_UKA = 2
        val ENTRY_UK_OPEN = 3
        val ENTRY_FAB = 4
        val ENTRY_IND = 100

        fun addOption(node: JsonNode, value: String, description: String) {
            val option = node.addElement()
            option["value"] = value
            option["description"] = description
        }

        fun deleteUka(idAccount: Int) {
            val webTransaction = WebTransaction()
            webTransaction.seekUkaRegistration(idAccount)
            if (webTransaction.found()) webTransaction.delete()
        }

        fun loadUka(idAccount: Int, paper: Boolean): WebTransaction {
            var unpaid = false
            val webTransaction = WebTransaction()

            dbTransaction {
                val type = if (paper) LEDGER_UKA_REGISTRATION_DIRECT else LEDGER_UKA_REGISTRATION
                Ledger().seek("idAccount=$idAccount AND type IN ($LEDGER_UKA_REGISTRATION_DIRECT, $LEDGER_UKA_REGISTRATION) AND amount<charge") {
                    unpaid = true
                }
                webTransaction.seekUkaRegistration(idAccount, force = true)

                if (unpaid) {
                    webTransaction.data["paper"] = paper
                } else {
                    webTransaction.clear()
                    webTransaction.data["paper"] = paper
                    fun addCompetitorAction(
                        idCompetitor: Int = 0,
                        action: Int,
                        name: String,
                        description: String,
                        price: Int
                    ) {
                        val node = webTransaction.actions["competitors"].addElement()
                        node["idCompetitor"] = idCompetitor
                        node["name"] = name
                        node["action"] = action
                        node["description"] = description
                        node["fee"] = price
                        node["paperFee"] = price * 2
                        node["confirmed"] = false
                    }

                    fun addDogAction(
                        idDog: Int,
                        action: Int,
                        name: String,
                        description: String,
                        kcGradeCode: String,
                        ukaHeightCode: String,
                        price: Int
                    ) {
                        val node = webTransaction.actions["dogs"].addElement()
                        node["idDog"] = idDog
                        node["action"] = action
                        node["name"] = name
                        node["description"] = description
                        node["kcGradeCode"] = kcGradeCode
                        node["ukaHeightCode"] = ukaHeightCode
                        node["fee"] = price
                        node["paperFee"] = price * 2
                        node["confirmed"] = false
                    }

                    Competitor().where("idAccount=$idAccount AND AliasFor=0", "idUka=0, idUka, givenName, familyName") {
                        if (ukaState > UKA_NOT_REGISTERED) {
                            val node = webTransaction.competitors.addElement()
                            node["idCompetitor"] = id
                            node["idUka"] = idUka
                            node["fullName"] = fullName
                            node["ukaDateConfirmed"] = ukaDateConfirmed
                            node["ukaMembershipExpires"] = ukaMembershipExpires
                            node["ukaState"] = ukaState
                            node["stateText"] = when (ukaState) {
                                UKA_EXPIRED -> "Expired ${ukaMembershipExpires.dateText}"
                                UKA_COMPLETE -> "Member until ${ukaMembershipExpires.dateText}"
                                else -> "n/a"
                            }
                        }
                        val fee = if (dateOfBirth > today.addYears(-16)) 0 else 600
                        when (ukaState) {
                            UKA_NOT_REGISTERED -> addCompetitorAction(
                                id,
                                LEDGER_ITEM_MEMBERSHIP,
                                fullName,
                                "Join UKA",
                                fee
                            )
                            UKA_EXPIRED -> addCompetitorAction(
                                id,
                                LEDGER_ITEM_MEMBERSHIP,
                                fullName,
                                "Renew Membership",
                                fee
                            )
                            UKA_COMPLETE -> {
                                if (ukaMembershipExpires < today.addMonths(6)) {
                                    addCompetitorAction(
                                        id,
                                        LEDGER_ITEM_MEMBERSHIP,
                                        fullName,
                                        "Extend to ${ukaMembershipExpires.addYears(5).dateText}",
                                        fee
                                    )
                                }
                            }
                        }
                    }

                    Dog().where("idAccount=$idAccount  AND AliasFor=0", "dogCode=0, dogCode, petName") {
                        if (ukaState > UKA_NOT_REGISTERED && state <= DOG_RETIRED) {
                            val node = webTransaction.dogs.addElement()
                            node["idDog"] = id
                            node["dogCode"] = code
                            node["petName"] = petName
                            node["ukaDateConfirmed"] = ukaDateConfirmed
                            node["ukaState"] = ukaState
                            node["stateText"] = when (ukaState) {
                                UKA_NOT_REGISTERED -> "Not Registered"
                                UKA_INCOMPLETE -> "Incomplete"
                                UKA_COMPLETE -> "Registered ${ukaDateConfirmed.dateText}"
                                UKA_SUSPENDED -> "Suspended"
                                else -> "n/a"
                            }
                        }
                        if (state == DOG_ACTIVE || ukaState > UKA_INCOMPLETE) {
                            when (ukaState) {
                                UKA_NOT_REGISTERED, UKA_INCOMPLETE -> {
                                    val ukaHeightCode = when (kcHeightCode) {
                                        "KC450" -> "UKA400"
                                        "KC350" -> "UKA300"
                                        else -> "UKA650"
                                    }

                                    addDogAction(
                                        id,
                                        LEDGER_ITEM_DOG_REGISTRATION,
                                        petName,
                                        "Register",
                                        kcGradeCode,
                                        ukaHeightCode,
                                        600
                                    )

                                }
                            }
                        }
                    }
                }
                webTransaction.post()
            }
            return webTransaction
        }


        fun loadEntry(idAccount: Int, idCompetition: Int, idCompetitor: Int, paper: Boolean): WebTransaction {
            val competition = Competition(idCompetition)
            val webTransaction = WebTransaction()

            dbTransaction {
                webTransaction.seekEntry(idAccount, competition.id, true)
                webTransaction.data.clear()
                if (paper) {
                    webTransaction.entryType = ENTRY_PAPER
                    webTransaction.payment["cheque"] =
                        Ledger.getAmount(idAccount, idCompetition, LEDGER_PAPER_ENTRY_CHEQUE)
                    webTransaction.payment["cash"] = Ledger.getAmount(idAccount, idCompetition, LEDGER_PAPER_ENTRY_CASH)
                } else {
                    webTransaction.entryType = ENTRY_AGILITY_PLAZA
                }

                webTransaction.misc["competitionDateEnd"] = competition.dateEnd
                webTransaction.misc["entryFee"] = competition.entryFee
                webTransaction.misc["grandFinals"] = competition.grandFinals


                when (competition.idOrganization) {
                    ORGANIZATION_KC -> loadEntryKc(idAccount, idCompetitor, competition, webTransaction)
                    ORGANIZATION_UKA -> loadEntryUka(idAccount, idCompetitor, competition, webTransaction)
                    ORGANIZATION_UK_OPEN -> loadEntryUkOpen(idAccount, competition, webTransaction)
                    ORGANIZATION_FAB -> loadEntryFab(idAccount, idCompetitor, competition, webTransaction)
                    ORGANIZATION_INDEPENDENT -> loadEntryInd(idAccount, idCompetitor, competition, webTransaction)

                }
                webTransaction.post()
            }
            return webTransaction
        }

        private fun loadCamping(idAccount: Int, competition: Competition, webTransaction: WebTransaction) {
            val camping = Camping()
            camping.find("idCompetition = ${competition.id} AND idAccount = $idAccount")
            if (camping.found()) {
                camping.loadNode(webTransaction.camping, competition)
            } else {
                Camping.loadEmptyNode(webTransaction.camping, competition.campingStart)
            }
        }

        private fun confirmCamping(idAccount: Int, idCompetition: Int, webTransaction: WebTransaction, accountBalance: Int, campingCredits: Int) {
            if (webTransaction.hasCamping()) {
                val paper = webTransaction.isPaper
                val confirmed = webTransaction.camping["confirmed"].asBoolean
                val freeCamping = webTransaction.globalBenefits["allCampingFree"].asBoolean
                val priorityCamping = webTransaction.globalBenefits["campingPriority"].asBoolean
                val category = when {
                    freeCamping -> CAMPING_RESERVED
                    priorityCamping -> CAMPING_PRIORITY
                    else -> CAMPING_REGULAR
                }
                //val freeCampingMain = confirmed && !webTransaction.isPaper
                val freeCampingMain = false

                val groupName = webTransaction.camping["groupName"].asString
                val pitches = webTransaction.camping["pitches"].asInt
                val pitchType = webTransaction.camping["pitchType"].asInt
                val dayFlags = webTransaction.camping["dayFlags"].asInt
                val baseDate = webTransaction.camping["baseDate"].asDate
                val priority = priorityCamping || freeCamping
                val feeOverride =
                    if (webTransaction.camping.has("feeOverride")) webTransaction.camping["feeOverride"].asInt else -1

                Camping().seekOrAppend("idCompetition=$idCompetition AND idAccount=$idAccount", {
                    this.idCompetition = idCompetition
                    this.idAccount = idAccount
                }) {
                    book(
                        paper, groupName, pitches, pitchType, baseDate, dayFlags, freeCamping,
                        freeCampingMain, feeOverride, priority, accountBalance, campingCredits
                    )
                }

            } else {
                Camping().where("idCompetition=$idCompetition AND idAccount=$idAccount") {
                    cancel()
                }
            }
        }

        private fun loadEntryKc(
            idAccount: Int,
            idCompetitor: Int,
            competition: Competition,
            webTransaction: WebTransaction
        ) {
            webTransaction.kind = ENTRY_KC

            val dog = Dog.select(
                "(dog.idAccount=$idAccount OR dog.idAccountShared=$idAccount) AND dog.dogState<$DOG_GONE",
                "dog.dogState, dog.dateOfBirth, dog.petName"
            )
            while (dog.next()) {
                webTransaction.LoadDogKc(competition, dog)
            }

            val account = Account(idAccount)
            val handlersList = account.handlersList;
            val where =
                "idAccount=$idAccount" + if (handlersList.isNotEmpty()) " OR idCompetitor IN ($handlersList)" else ""
            Competitor().where(where, "idCompetitor=$idCompetitor DESC, idAccount=$idAccount DESC, givenName, familyName") {
                if (!closed) {
                    webTransaction.LoadCompetitorKc(competition, this)
                }
            }

            val entry = Entry()
            entry.agilityClass.joinToParent()
            entry.team.joinToParent()
            entry.team.dog.joinToParent()
            entry.select(
                "entry.idAccount=$idAccount AND agilityClass.idCompetition=${competition.id}",
                "classNumber, classNumberSuffix"
            )
            while (entry.next()) {
                val node = webTransaction.LoadEntry(
                    entry.idAgilityClass,
                    entry.team.idDog,
                    entry.team.idCompetitor,
                    entry.gradeCode,
                    entry.heightCode,
                    entry.jumpHeightCode,
                    entry.fee,
                    entry.team.dog.petName,
                    entry.agilityClass.name,
                    entry.agilityClass.date,
                    entry.agilityClass.code,
                    entry.agilityClass.number,
                    entry.team.dualHandler
                )
                if (entry.team.multiple) {
                    node["team"].setValue(entry.team.extra)
                }
            }

            if (competition.hasCamping) {
                loadCamping(idAccount, competition, webTransaction)
            }

            val ledgerItem = LedgerItem()
            ledgerItem.select("idAccount=$idAccount AND idCompetition=${competition.id} AND NOT type IN ($LEDGER_ITEM_ENTRY, $LEDGER_ITEM_ENTRY_SURCHARGE, $LEDGER_ITEM_ENTRY_DISCOUNT, $LEDGER_ITEM_CAMPING)")
            while (ledgerItem.next()) {
                webTransaction.LoadItem(ledgerItem.type, ledgerItem.quantity, ledgerItem.unitPrice, ledgerItem.code, ledgerItem.idDog, ledgerItem.idCompetitor, ledgerItem.size)
            }

            val haveTransactions =
                entry.rowCount > 0 || webTransaction.camping["booked"].asBoolean || ledgerItem.rowCount > 0


            webTransaction.data["state"] = if (haveTransactions) "entered" else "blank"


        }

        private fun loadEntryInd(
            idAccount: Int,
            idCompetitor: Int,
            competition: Competition,
            webTransaction: WebTransaction
        ) {
            webTransaction.kind = ENTRY_IND

            val dog = Dog.select(
                "(dog.idAccount=$idAccount OR dog.idAccountShared=$idAccount) AND dog.dogState<$DOG_RETIRED",
                "dog.dogState, dog.dateOfBirth, dog.petName"
            )
            while (dog.next()) {
                webTransaction.LoadDogInd(competition, dog)
            }

            val account = Account(idAccount)
            val handlersList = account.handlersList;
            val where =
                "idAccount=$idAccount" + if (handlersList.isNotEmpty()) " OR idCompetitor IN ($handlersList)" else ""
            Competitor().where(where, "idCompetitor=$idCompetitor DESC, idAccount=$idAccount DESC, givenName, familyName") {
                if (!closed) {
                    webTransaction.LoadCompetitorInd(competition, this)
                }
            }

            val entry = Entry()
            entry.agilityClass.joinToParent()
            entry.team.joinToParent()
            entry.team.dog.joinToParent()
            entry.select(
                "entry.idAccount=$idAccount AND agilityClass.idCompetition=${competition.id}",
                "classNumber, classNumberSuffix"
            )
            while (entry.next()) {
                val node = webTransaction.LoadEntry(
                    entry.idAgilityClass,
                    entry.team.idDog,
                    entry.team.idCompetitor,
                    entry.gradeCode,
                    entry.heightCode,
                    entry.jumpHeightCode,
                    entry.fee,
                    entry.team.dog.petName,
                    entry.agilityClass.name,
                    entry.agilityClass.date,
                    entry.agilityClass.code,
                    entry.agilityClass.number,
                    entry.team.dualHandler,
                    clearRoundOnly = entry.clearRoundOnly
                )
                if (entry.team.multiple) {
                    node["team"].setValue(entry.team.extra)
                }
            }

            if (competition.hasCamping) {
                loadCamping(idAccount, competition, webTransaction)
            }

            val ledgerItem = LedgerItem()
            ledgerItem.select("idAccount=$idAccount AND idCompetition=${competition.id} AND NOT type IN ($LEDGER_ITEM_ENTRY, $LEDGER_ITEM_ENTRY_SURCHARGE, $LEDGER_ITEM_ENTRY_DISCOUNT, $LEDGER_ITEM_CAMPING)")
            while (ledgerItem.next()) {
                webTransaction.LoadItem(ledgerItem.type, ledgerItem.quantity, ledgerItem.unitPrice, ledgerItem.code, ledgerItem.idDog, ledgerItem.idCompetitor, ledgerItem.size)
            }

            val haveTransactions =
                entry.rowCount > 0 || webTransaction.camping["booked"].asBoolean || ledgerItem.rowCount > 0


            webTransaction.data["state"] = if (haveTransactions) "entered" else "blank"


        }

        private fun loadEntryFab(
            idAccount: Int,
            idCompetitor: Int,
            competition: Competition,
            webTransaction: WebTransaction
        ) {
            webTransaction.kind = ENTRY_FAB

            val dog = Dog.select(
                "(dog.idAccount=$idAccount OR dog.idAccountShared=$idAccount) AND dog.dogState<$DOG_RETIRED",
                "dog.dogState, dog.dateOfBirth, dog.petName"
            )
            while (dog.next()) {
                webTransaction.LoadDogFab(competition, dog)
            }

            val account = Account(idAccount)
            val handlersList = account.handlersList;
            val where =
                "idAccount=$idAccount" + if (handlersList.isNotEmpty()) " OR idCompetitor IN ($handlersList)" else ""
            Competitor().where(where, "idCompetitor=$idCompetitor DESC, idAccount=$idAccount DESC, givenName, familyName") {
                if (!closed) {
                    webTransaction.LoadCompetitorFab(competition, this)
                }
            }

            val entry = Entry()
            entry.agilityClass.joinToParent()
            entry.team.joinToParent()
            entry.team.dog.joinToParent()
            entry.select(
                "entry.idAccount=$idAccount AND agilityClass.idCompetition=${competition.id}",
                "classNumber, classNumberSuffix"
            )
            while (entry.next()) {
                val node = webTransaction.LoadEntry(
                    entry.idAgilityClass,
                    entry.team.idDog,
                    entry.team.idCompetitor,
                    entry.gradeCode,
                    entry.heightCode,
                    entry.jumpHeightCode,
                    entry.fee,
                    entry.team.dog.petName,
                    entry.agilityClass.name,
                    entry.agilityClass.date,
                    entry.agilityClass.code,
                    entry.agilityClass.number,
                    entry.team.dualHandler
                )
                if (entry.team.multiple) {
                    node["team"].setValue(entry.team.extra)
                }
            }

            if (competition.hasCamping) {
                loadCamping(idAccount, competition, webTransaction)
            }

            val ledgerItem = LedgerItem()
            ledgerItem.select("idAccount=$idAccount AND idCompetition=${competition.id} AND NOT type IN ($LEDGER_ITEM_ENTRY, $LEDGER_ITEM_ENTRY_SURCHARGE, $LEDGER_ITEM_ENTRY_DISCOUNT, $LEDGER_ITEM_CAMPING)")
            while (ledgerItem.next()) {
                webTransaction.LoadItem(ledgerItem.type, ledgerItem.quantity, ledgerItem.unitPrice, ledgerItem.code, ledgerItem.idDog, ledgerItem.idCompetitor, ledgerItem.size)
            }

            val haveTransactions =
                entry.rowCount > 0 || webTransaction.camping["booked"].asBoolean || ledgerItem.rowCount > 0


            webTransaction.data["state"] = if (haveTransactions) "entered" else "blank"


        }

        private fun loadEntryUka(idAccount: Int, idCompetitor: Int, competition: Competition, webTransaction: WebTransaction) {
            webTransaction.kind = ENTRY_UKA

            var haveTransactions = false

            val account = Account(idAccount)
            val handlersList = account.handlersList;
            val where =
                "idAccount=$idAccount" + if (handlersList.isNotEmpty()) " OR idCompetitor IN ($handlersList)" else ""
            Competitor().where(where, "idCompetitor=$idCompetitor DESC, idAccount=$idAccount DESC, givenName, familyName") {
                if (!closed) {
                    webTransaction.LoadCompetitorUka(competition, this)
                }
            }

            Entry().join { agilityClass }.join { team }.join { team.dog }
                .where("entry.idAccount=$idAccount AND agilityClass.idCompetition=${competition.id}") {
                    val node = if (competition.grandFinals) {
                        webTransaction.LoadEntry(
                            idAgilityClass, team.idDog, team.idCompetitor, gradeCode,
                            heightCode, jumpHeightCode, fee, team.dog.petName, agilityClass.name,
                            agilityClass.date, agilityClass.code, invited = invitationOpen,
                            confirmed = entered, idTeam = idTeam
                        )
                    } else {
                        webTransaction.LoadEntry(
                            idAgilityClass, team.idDog, team.idCompetitor, gradeCode,
                            heightCode, jumpHeightCode, fee, team.dog.petName, agilityClass.name,
                            agilityClass.date, agilityClass.code
                        )
                    }
                    if (team.multiple) {
                        node["team"].setValue(team.extra)
                    }
                    haveTransactions = true
                }

            Dog().join { owner }.join { handler }
                .where("(dog.idAccount=$idAccount OR dog.idAccountShared=$idAccount) AND dog.dogState=$DOG_ACTIVE") {
                    if (dateOfBirth <= competition.dateEnd.addMonths(-16)) {
                        webTransaction.LoadDogUka(competition, this)
                    }
                }

            LedgerItem().where("idAccount=$idAccount AND idCompetition=${competition.id} AND NOT type IN ($LEDGER_ITEM_ENTRY, $LEDGER_ITEM_ENTRY_SURCHARGE, $LEDGER_ITEM_ENTRY_DISCOUNT, $LEDGER_ITEM_CAMPING)") {
                webTransaction.LoadItem(type, quantity, unitPrice, code, idDog, idCompetitor, size)
                haveTransactions = true
            }


            if (competition.hasCamping) {
                loadCamping(idAccount, competition, webTransaction)
            }

            webTransaction.data["state"] =
                if (haveTransactions || webTransaction.camping["booked"].asBoolean) "entered" else "blank"
        }

        private fun loadEntryUkOpen(idAccount: Int, competition: Competition, webTransaction: WebTransaction) {
            webTransaction.kind = ENTRY_UK_OPEN
            var haveTransactions = false
            val query =
                DbQuery("SELECT GROUP_CONCAT(idDog) AS list FROM competitionDog WHERE idCompetition=${competition.id} AND idAccount=$idAccount").toFirst()
            val idDogList = query.getString("list")
            val dog = Dog()
            dog.owner.joinToParent()

            if (idDogList.isEmpty()) {
                dog.select("(dog.idAccount=$idAccount OR dog.idAccountShared=$idAccount) AND dog.dogState=$DOG_ACTIVE")
            } else {
                dog.select("((dog.idAccount=$idAccount OR dog.idAccountShared=$idAccount) AND dog.dogState=$DOG_ACTIVE) OR idDog IN ($idDogList)")
            }
            val competitionDog = CompetitionDog()
            while (dog.next()) {
                competitionDog.findDog(competition.id, dog.id)
                val dogNode = webTransaction.dogs.searchElement("idDog", dog.id)
                dogNode["petName"] = dog.petName
                if (competitionDog.found()) {
                    dogNode["heightCode"] = competitionDog.ukOpenHeightCode
                    dogNode["handler"] = competitionDog.ukOpenHandler
                    dogNode["entered"] = true
                    dogNode["heightCodeLocked"] = competitionDog.ukOpenHeightCodeLocked
                    dogNode["nation"] = competitionDog.ukOpenNation
                    haveTransactions = true
                } else {
                    dogNode["heightCode"] = dog.ukOpenHeightCode
                    dogNode["handler"] = dog.owner.fullName
                    dogNode["entered"] = false
                    dogNode["heightCodeLocked"] = dog.isUkaRegisteredOldRule && dog.ukOpenHeightCode.isNotEmpty()
                    dogNode["nation"] = dog.ukOpenNation
                }
            }
            if (competition.hasCamping) {
                loadCamping(idAccount, competition, webTransaction)
            }

            LedgerItem().where("idAccount=$idAccount AND idCompetition=${competition.id} AND NOT type IN ($LEDGER_ITEM_ENTRY, $LEDGER_ITEM_ENTRY_SURCHARGE, $LEDGER_ITEM_ENTRY_DISCOUNT, $LEDGER_ITEM_CAMPING)") {
                webTransaction.LoadItem(type, quantity, unitPrice, code, idDog, idCompetitor, size)
            }

            webTransaction.data["state"] = if (haveTransactions) "entered" else "blank"

            /*
            if (ledgerItem.rowCount == 0) {
                val account = Account(idAccount)
                if (account.found()) {
                    webTransaction.data["postage"] = account.countryCode == "GB"
                }

            }
            
             */

        }

        fun confirm(webTransaction: WebTransaction) {
            val idCompetition = webTransaction.idCompetition
            val competition = Competition(idCompetition)
            dbTransaction {
                when (webTransaction.kind) {
                    ENTRY_KC -> confirmKc(webTransaction, competition)
                    ENTRY_UKA -> confirmUka(webTransaction, competition)
                    ENTRY_UK_OPEN -> confirmUkOpen(webTransaction, competition)
                    ENTRY_FAB -> confirmFab(webTransaction, competition)
                    ENTRY_IND -> confirmInd(webTransaction, competition)
                }
                PlazaMessage.showEntryAcknowledged(
                    webTransaction.idCompetition,
                    webTransaction.idAccount,
                    paper = webTransaction.isPaper
                )
                competition.checkCap()
            }
        }

        fun confirmKc(webTransaction: WebTransaction, competition: Competition) {
            val idAccount = webTransaction.idAccount
            val idCompetition = webTransaction.idCompetition

            dbTransaction {
                val agilityClass = AgilityClass()

                webTransaction.saveCompetitorKC()
                webTransaction.saveDogKc()

                val cheque = webTransaction.payment["cheque"].asInt
                val cash = webTransaction.payment["cash"].asInt

                val freeCamping = webTransaction.globalBenefits["allCampingFree"].asBoolean

                Ledger.process(idAccount, idCompetition, webTransaction.entryType, cheque, cash, freeCamping) { ledger, accountBalance ->

                    Entry.process(idAccount, idCompetition) {
                        dbExecute("UPDATE entry JOIN agilityClass USING (idAgilityClass) SET entry.flag=TRUE WHERE agilityClass.idCompetition=${idCompetition} AND entry.idAccount=${idAccount}")
                        for (entry in webTransaction.entries) {
                            if (entry["confirmed"].asBoolean) {
                                val template = ClassTemplate.select(entry["classCode"].asInt)
                                val team = if (template.dualHandler) {
                                    Team.getKcDual(
                                        idAccount = idAccount,
                                        idAgilityClass = agilityClass.id,
                                        idDog = entry["idDog"].asInt,
                                        dualHandler = entry["dualHandler"].asString,
                                        classCode = template.code
                                    )
                                } else if (template.teamSize > 1) {
                                    Team.getKcTeam(
                                        idAccount = idAccount,
                                        idAgilityClass = agilityClass.id,
                                        idDog = entry["idDog"].asInt,
                                        teamName = entry["team.teamName"].asString,
                                        clubName = entry["team.clubName"].asString,
                                        members = entry["team.members"],
                                        classCode = template.code
                                    )
                                } else {
                                    Team.getIndividual(entry["idCompetitor"].asInt, entry["idDog"].asInt)
                                }
                                val idTeam = team.id
                                agilityClass.find(entry["idAgilityClass"].asInt)
                                agilityClass.enter(
                                    idTeam = idTeam,
                                    gradeCode = entry["gradeCode"].asString,
                                    heightCode = entry["heightCode"].asString,
                                    jumpHeightCode = entry["jumpHeightCode"].asString,
                                    entryType = webTransaction.entryType,
                                    timeEntered = now,
                                    progress = PROGRESS_ENTERED, //in case had been deleted due to grade changes
                                    idAccount = idAccount,
                                    fee = entry["entryFee"].asInt,
                                    runningOrder = if (competition.processed) -1 else null,
                                    enterChildClasses = competition.processed
                                )
                            }
                        }
                        dbExecute("DELETE entry.* FROM entry JOIN agilityClass USING (idAgilityClass) WHERE agilityClass.idCompetition=${idCompetition} AND entry.idAccount=${idAccount} AND entry.flag")
                    }

                    var campingCredits = 0
                    for (item in webTransaction.items) {
                        ledger.addMiscellaneous(item["type"].asInt, item["idDog"].asInt, item["idCompetitor"].asInt, item["unitPrice"].asInt, item["quantity"].asInt)
                        if (item["type"].asInt == LEDGER_ITEM_CAMPING_CREDIT) {
                            campingCredits += (-item["itemFee"].asInt)
                        }
                    }

                    confirmCamping(idAccount, idCompetition, webTransaction, accountBalance, campingCredits)

                }
            }
        }

        fun confirmInd(webTransaction: WebTransaction, competition: Competition) {
            val idAccount = webTransaction.idAccount
            val idCompetition = webTransaction.idCompetition

            dbTransaction {
                val agilityClass = AgilityClass()

                webTransaction.saveCompetitorInd()
                webTransaction.saveDogInd()

                val cheque = webTransaction.payment["cheque"].asInt
                val cash = webTransaction.payment["cash"].asInt

                val freeCamping = webTransaction.globalBenefits["allCampingFree"].asBoolean

                Ledger.process(idAccount, idCompetition, webTransaction.entryType, cheque, cash, freeCamping) { ledger, accountBalance ->

                    Entry.process(idAccount, idCompetition) {
                        dbExecute("UPDATE entry JOIN agilityClass USING (idAgilityClass) SET entry.flag=TRUE WHERE agilityClass.idCompetition=${idCompetition} AND entry.idAccount=${idAccount}")
                        for (entry in webTransaction.entries) {
                            if (entry["confirmed"].asBoolean) {
                                val template = ClassTemplate.select(entry["classCode"].asInt)
                                val team = Team.getIndividual(entry["idCompetitor"].asInt, entry["idDog"].asInt)
                                val idTeam = team.id
                                agilityClass.find(entry["idAgilityClass"].asInt)
                                agilityClass.enter(
                                    idTeam = idTeam,
                                    gradeCode = entry["gradeCode"].asString,
                                    heightCode = entry["heightCode"].asString,
                                    jumpHeightCode = entry["jumpHeightCode"].asString,
                                    entryType = webTransaction.entryType,
                                    timeEntered = now,
                                    progress = PROGRESS_ENTERED, //in case had been deleted due to grade changes
                                    idAccount = idAccount,
                                    fee = entry["entryFee"].asInt,
                                    runningOrder = if (competition.processed) -1 else null,
                                    enterChildClasses = competition.processed,
                                    runsEntered = entry["runsEntered"].asInt
                                )
                            }
                        }
                        dbExecute("DELETE entry.* FROM entry JOIN agilityClass USING (idAgilityClass) WHERE agilityClass.idCompetition=${idCompetition} AND entry.idAccount=${idAccount} AND entry.flag")
                    }

                    var campingCredits = 0
                    for (item in webTransaction.items) {
                        var description = ""
                        if (item["type"].asInt == LEDGER_ITEM_SECOND_CHANCE) {
                            val idDog = item["idDog"].asInt
                            val dogNode = webTransaction.getDog(idDog)
                            description = "${dogNode["petName"].asString} - Second Chance Ticket"
                        }
                        ledger.addMiscellaneous(item["type"].asInt, item["idDog"].asInt, item["idCompetitor"].asInt, item["unitPrice"].asInt, item["quantity"].asInt, description=description)
                        if (item["type"].asInt == LEDGER_ITEM_CAMPING_CREDIT) {
                            campingCredits += (-item["itemFee"].asInt)
                        }
                    }

                    confirmCamping(idAccount, idCompetition, webTransaction, accountBalance, campingCredits)

                }
            }
        }

        fun confirmUka(webTransaction: WebTransaction, competition: Competition) {
            val idAccount = webTransaction.idAccount
            val idCompetition = webTransaction.idCompetition

            dbTransaction {
                val agilityClass = AgilityClass()
                val dog = Dog()

                webTransaction.saveCompetitorUka()
                webTransaction.saveDogUka()

                val cheque = webTransaction.payment["cheque"].asInt
                val cash = webTransaction.payment["cash"].asInt

                val freeCamping = webTransaction.globalBenefits["allCampingFree"].asBoolean

                Ledger.process(idAccount, idCompetition, webTransaction.entryType, cheque, cash, freeCamping) { ledger, accountBalance ->
                    Entry.process(idAccount, idCompetition) {
                        if (competition.grandFinals) {
                            dbExecute("UPDATE entry JOIN agilityClass USING (idAgilityClass) SET entry.entryType=$ENTRY_INVITE WHERE agilityClass.idCompetition=${idCompetition} AND entry.idAccount=${idAccount}")
                        } else {
                            dbExecute("UPDATE entry JOIN agilityClass USING (idAgilityClass) SET entry.flag=TRUE WHERE agilityClass.idCompetition=${idCompetition} AND entry.idAccount=${idAccount}")
                        }
                        for (entry in webTransaction.entries) {
                            if (entry["confirmed"].asBoolean) {
                                if (dog.id != entry["idDog"].asInt) {
                                    dog.find(entry["idDog"].asInt)
                                }

                                agilityClass.find(entry["idAgilityClass"].asInt)

                                val team = if (entry["idTeam"].asInt > 0) {
                                    Team().seek(entry["idTeam"].asInt)
                                } else {

                                    when (agilityClass.template) {
                                        ClassTemplate.TEAM -> {
                                            Team.getUkaTeamTeam(
                                                idAccount = idAccount,
                                                idAgilityClass = agilityClass.id,
                                                idDog = dog.id,
                                                teamName = entry["team.teamName"].asString,
                                                members = entry["team.members"]
                                            )
                                        }
                                        ClassTemplate.SPLIT_PAIRS -> {
                                            Team.getUkaPairsTeam(
                                                idAccount = idAccount,
                                                idAgilityClass = agilityClass.id,
                                                idDog = dog.id,
                                                members = entry["team.members"]
                                            )
                                        }
                                        else -> {
                                            Team.getIndividual(dog.idCompetitorHandler, entry["idDog"].asInt)
                                        }
                                    }
                                }
                                val idTeam = team.id
                                val clearRoundOnly =
                                    if (agilityClass.isCasual) dog.isClearRoundOnly(entry["heightCode"].asString) else false

                                var heightCode =
                                    if (agilityClass.isRelay) team.relayHeightCode else entry["heightCode"].asString
                                if (agilityClass.template == ClassTemplate.JUNIOR_MASTERS) {
                                    when (heightCode) {
                                        "UKA400" -> heightCode = "UKA300"
                                        "UKA650" -> heightCode = "UKA550"
                                    }
                                }

                                val idEntry = agilityClass.enter(
                                    idTeam = idTeam,
                                    heightCode = heightCode,
                                    jumpHeightCode = heightCode,
                                    gradeCode = if (agilityClass.template.isUkaProgression) agilityClass.gradeCodes else null,
                                    clearRoundOnly = clearRoundOnly,
                                    entryType = webTransaction.entryType,
                                    progress = PROGRESS_ENTERED,
                                    timeEntered = now,
                                    idAccount = idAccount,
                                    fee = entry["entryFee"].asInt,
                                    runningOrder = if (competition.processed) -1 else null,
                                    grandFinals = competition.grandFinals
                                )
                            }
                        }
                        if (!competition.grandFinals) {
                            dbExecute("DELETE entry.* FROM entry JOIN agilityClass USING (idAgilityClass) WHERE agilityClass.idCompetition=${idCompetition} AND entry.idAccount=${idAccount} AND entry.flag")
                        }
                    }

                    var campingCredits = 0
                    for (item in webTransaction.items) {
                        ledger.addMiscellaneous(item["type"].asInt, item["idDog"].asInt, item["idCompetitor"].asInt, item["unitPrice"].asInt, item["quantity"].asInt, item["code"].asInt, item["size"].asString)
                        if (item["type"].asInt == LEDGER_ITEM_CAMPING_CREDIT) {
                            campingCredits += item["itemFee"].asInt
                        }
                    }

                    confirmCamping(idAccount, idCompetition, webTransaction, accountBalance, campingCredits)
                }
            }
        }

        fun confirmFab(webTransaction: WebTransaction, competition: Competition) {
            val idAccount = webTransaction.idAccount
            val idCompetition = webTransaction.idCompetition

            dbTransaction {
                val agilityClass = AgilityClass()
                val dog = Dog()

                webTransaction.saveDogFab()
                webTransaction.saveCompetitorFab()

                val cheque = webTransaction.payment["cheque"].asInt
                val cash = webTransaction.payment["cash"].asInt

                val freeCamping = webTransaction.globalBenefits["allCampingFree"].asBoolean

                Ledger.process(idAccount, idCompetition, webTransaction.entryType, cheque, cash, freeCamping) { ledger, accountBalance ->
                    Entry.process(idAccount, idCompetition) {

                        dbExecute("UPDATE entry JOIN agilityClass USING (idAgilityClass) SET entry.flag=TRUE WHERE agilityClass.idCompetition=${idCompetition} AND entry.idAccount=${idAccount}")
                        for (entry in webTransaction.entries) {
                            if (entry["confirmed"].asBoolean) {
                                if (dog.id != entry["idDog"].asInt) {
                                    dog.find(entry["idDog"].asInt)
                                }

                                agilityClass.find(entry["idAgilityClass"].asInt)

                                val team = Team.getIndividual(dog.idCompetitorHandler, entry["idDog"].asInt)
                                val idTeam = team.id
                                val heightCode = entry["heightCode"].asString
                                val subDivision = entry["subDivision"].asInt
                                val gradeCode = agilityClass.gradeCodes.substringBefore(",")

                                val idEntry = agilityClass.enter(
                                    idTeam = idTeam,
                                    heightCode = heightCode,
                                    jumpHeightCode = heightCode,
                                    subDivision = subDivision,
                                    gradeCode = gradeCode,
                                    entryType = webTransaction.entryType,
                                    progress = PROGRESS_ENTERED,
                                    timeEntered = now,
                                    idAccount = idAccount,
                                    fee = entry["entryFee"].asInt,
                                    runningOrder = if (competition.processed) -1 else null
                                )
                            }
                        }
                        dbExecute("DELETE entry.* FROM entry JOIN agilityClass USING (idAgilityClass) WHERE agilityClass.idCompetition=${idCompetition} AND entry.idAccount=${idAccount} AND entry.flag")
                    }

                    var campingCredits = 0
                    for (item in webTransaction.items) {
                        ledger.addMiscellaneous(item["type"].asInt, item["idDog"].asInt, item["idCompetitor"].asInt, item["unitPrice"].asInt, item["quantity"].asInt)
                        if (item["type"].asInt == LEDGER_ITEM_CAMPING_CREDIT) {
                            campingCredits += item["itemFee"].asInt
                        }
                    }

                    confirmCamping(idAccount, idCompetition, webTransaction, accountBalance, campingCredits)


                }
            }
        }

        fun confirmUkOpen(webTransaction: WebTransaction, competition: Competition) {
            val idCompetition = webTransaction.idCompetition
            val idAccount = webTransaction.idAccount
            val competitionDog = CompetitionDog()
            dbTransaction {
                val cheque = webTransaction.payment["cheque"].asInt
                val cash = webTransaction.payment["cash"].asInt

                val freeCamping = webTransaction.globalBenefits["allCampingFree"].asBoolean

                Ledger.process(idAccount, idCompetition, webTransaction.entryType, cheque, cash, freeCamping) { ledger, accountBalance ->
                    dbExecute("UPDATE competitionDog SET flag=TRUE WHERE idCompetition=$idCompetition AND idAccount=$idAccount")
                    for (dog in webTransaction.dogs) {
                        if (dog["entered"].asBoolean) {
                            val idDog = dog["idDog"].asInt
                            val heightCode = dog["heightCode"].asString
                            val handler = dog["handler"].asString
                            val petName = dog["petName"].asString
                            val nation = dog["nation"].asString
                            val heightCodeLocked = dog["heightCodeLocked"].asBoolean
                            if (!competitionDog.findDog(idCompetition, idDog)) {
                                competitionDog.append()
                                competitionDog.idDog = idDog
                                competitionDog.idCompetition = idCompetition
                            }
                            competitionDog.idAccount = idAccount
                            competitionDog.flag = false
                            competitionDog.ukOpenHeightCode = heightCode
                            competitionDog.ukOpenHandler = handler
                            competitionDog.ukOpenHeightCodeLocked = heightCodeLocked
                            competitionDog.ukOpenNation = nation.trim()
                            competitionDog.post()
                            ledger.addEntry(idDog, petName, 1, webTransaction.misc["entryFee"].asInt, 1)
                        }
                    }
                    dbExecute("DELETE FROM competitionDog WHERE idCompetition=$idCompetition AND idAccount=$idAccount AND flag=TRUE")

                    var campingCredits = 0
                    for (item in webTransaction.items) {
                        ledger.addMiscellaneous(item["type"].asInt, item["idDog"].asInt, item["idCompetitor"].asInt, item["unitPrice"].asInt, item["quantity"].asInt)
                        if (item["type"].asInt == LEDGER_ITEM_CAMPING_CREDIT) {
                            campingCredits += item["itemFee"].asInt
                        }
                    }

                    confirmCamping(idAccount, idCompetition, webTransaction, accountBalance, campingCredits)

                }
            }
        }

    }
}