/*
 * Copyright (c) Mike Brickman 2014-2018
 */

package org.egility.library.general


import org.egility.library.database.DbQuery
import org.egility.library.dbobject.*
import java.util.*
import kotlin.collections.ArrayList

/**
 * Created by mbrickman on 05/05/17.
 */


/*

*/
fun kcLateClassChanges(idCompetition: Int, idDog: Int, gradeCode: String, heightCode: String, jumpHeightCode: String, agilityClassIds: ArrayList<Int>) {

    dbTransaction {
        var idAccount = 0
        var handler = ""
        val idCompetitor = Dog(idDog).idCompetitorHandler
        CompetitionDog().seek("idCompetition=$idCompetition AND idDog=$idDog") {
            if (gradeCode.isNotEmpty()) kcGradeCode = gradeCode
            if (heightCode.isNotEmpty()) kcHeightCode = heightCode
            if (jumpHeightCode.isNotEmpty()) kcJumpHeightCode = jumpHeightCode
            post()
            idAccount = this.idAccount
            handler = this.kcHandler
        }.otherwise {
            throw Wobbly("Dog $idDog not entered in competition $idCompetition (kcFixEntryByClassNumber)")
        }

        // update current classes
        Entry().join { team }.join { agilityClass }
            .where("AgilityClass.idCompetition=$idCompetition AND team.idDog=$idDog") {
                if (agilityClassIds.isNotEmpty() && agilityClassIds.contains(agilityClass.id)) {
                    if (canUpdate) {
                        // still need this class - adjust details
                        this.gradeCode = gradeCode
                        this.heightCode = heightCode
                        this.jumpHeightCode = jumpHeightCode
                        subClass = agilityClass.chooseSubClass(gradeCode, heightCode, jumpHeightCode, 0)
                        if (jumpHeightCode != this.runningOrderJumpHeightCode) runningOrder =
                            agilityClass.nextRunningOrder(jumpHeightCode)
                        this.runningOrderJumpHeightCode = jumpHeightCode
                        progress = PROGRESS_ENTERED
                    }
                } else {
                    progress = PROGRESS_REMOVED_LATE_CORRECTION
                }
                post()
                agilityClass.checkMinMaxRunningOrder()
                agilityClassIds.remove(agilityClass.id)

            }

        for (idAgilityClass in agilityClassIds) {
            kcLateEntry(idCompetition, idDog, idAccount, idCompetitor, idAgilityClass, gradeCode, heightCode, jumpHeightCode, ENTRY_TRANSFER, handler)
        }

    }


}

fun kcLateEntry(idCompetition: Int, idDog: Int, idAccount: Int, idCompetitor: Int, idAgilityClass: Int, gradeCode: String, heightCode: String, jumpHeightCode: String, entryType: Int = ENTRY_AT_SHOW, handler: String = "") {
    val idTeam = if (handler.isNotEmpty())
        Team.getIndividualNamedId(idDog, handler.naturalCase)
    else
        Team.getIndividualId(idCompetitor, idDog)

    AgilityClass().seek("idAgilityClass=$idAgilityClass") {
        val ringNumber =
            CompetitionDog.kcAddLate(idCompetition, idDog, gradeCode, heightCode, jumpHeightCode, entryType)
        enter(
            idTeam = idTeam,
            idAccount = idAccount,
            gradeCode = gradeCode,
            heightCode = heightCode,
            jumpHeightCode = jumpHeightCode,
            subDivision = 0,
            entryType = entryType,
            runningOrder = -1,
            dogRingNumber = ringNumber,
            timeEntered = now
        )
        checkMinMaxRunningOrder()
    }
}

fun kcClassNumbersToIds(idCompetition: Int, vararg classNumbers: Int): ArrayList<Int> {
    var list = ""
    classNumbers.forEach { list = list.append(it.toString()) }
    val agilityClassIds = ArrayList<Int>()
    AgilityClass().where("idCompetition=$idCompetition AND classNumber IN (${list})") {
        agilityClassIds.add(id)
    }
    return agilityClassIds
}


fun kcLateClassChanges(idCompetition: Int, dogCode: Int, gradeCode: String, heightCode: String, jumpHeightCode: String, vararg classNumbers: Int) {
    val idDog = Dog.codeToIdDog(dogCode)
    kcLateClassChanges(idCompetition, idDog, gradeCode, heightCode, jumpHeightCode, kcClassNumbersToIds(idCompetition, *classNumbers))
    kcListDogEntries(idCompetition, idDog)
}

fun addKcLate(idCompetition: Int, dogCode: Int, lowHeight: Boolean, vararg classNumbers: Int) {
    var list = ""
    classNumbers.forEach { list = list.append(it.toString()) }
    val competition = Competition(idCompetition)

    Dog().seek("dogCode=$dogCode") {
        val jumpHeightCode = if (lowHeight) kcHeightCode + "L" else kcHeightCode
        val gradeCode = kcEffectiveGradeCode(competition.dateStart)
        AgilityClass().where("idCompetition=$idCompetition AND classNumber IN (${list})") {
            kcLateEntry(idCompetition, this@seek.id, idAccount, idCompetitor, id, gradeCode, kcHeightCode, jumpHeightCode)
        }
        kcListDogEntries(idCompetition, id)
    }
}

fun kcListDogEntries(idCompetition: Int, idDog: Int) {
    val dog = Dog()
    val competitionDog = CompetitionDog()
    dog.owner.joinToParent()

    if (dog.find(idDog)) {
        competitionDog.findDog(idCompetition, idDog)
        val gradeName = Grade.getGradeName(competitionDog.kcGradeCode)
        val heightName = Height.getCombinedName(competitionDog.kcHeightCode, competitionDog.kcJumpHeightCode)
        val entry = Entry()
        entry.team.joinToParent()
        entry.agilityClass.joinToParent()

        println("${competitionDog.ringNumber}: ${dog._petName}: ${dog.registeredName} ($gradeName $heightName) - owner: ${dog.owner.fullName}")
        entry.select("team.idDog=${dog.id} AND agilityClass.idCompetition=$idCompetition", "classNumber")
        while (entry.next()) {
            var status = if (entry.type >= ENTRY_TRANSFER) " *" else ""
            if (entry.subClass == -1 && entry.progress != PROGRESS_TRANSFERRED) status += " INVALID ENTRY"
            if (entry.progress == PROGRESS_WITHDRAWN) status += " WITHDRAWN"
            if (entry.progress >= PROGRESS_REMOVED) status += " REMOVED"
            println("   ${entry.agilityClass.describeClass(short = true)} - r/o ${entry.runningOrder}$status")
        }
    }
}

/*
fun kcFixEntryByClassNumber(idCompetition: Int, idDog: Int, gradeCode: String, heightCode: String, jumpHeightCode: String, vararg classNumbers: Int) {
    var list = ""
    classNumbers.forEach { list = list.append(it.toString()) }
    val agilityClassIds = ArrayList<Int>()
    AgilityClass().where("idCompetition=$idCompetition AND classNumber IN ($list)") {
        agilityClassIds.add(id)
    }
    kcChangeClasses(idCompetition, idDog, gradeCode, heightCode, jumpHeightCode, agilityClassIds)
}




fun kcLateEntryByClassNumber(idCompetition: Int, idDog: Int, classNumber: Int, gradeCode: String, heightCode: String, jumpHeightCode: String, entryType: Int = ENTRY_AT_SHOW, handler: String = "") {
    val idAgilityClass = AgilityClass.classNumberToId(idCompetition, classNumber)
    kcLateEntry(idCompetition, idDog, idAgilityClass, gradeCode, heightCode, jumpHeightCode, entryType, handler)
}


fun kcWithdrawAll(idCompetition: Int, idDog: Int) {
    val dog = Dog()
    if (dog.find(idDog)) {

        val entry = Entry()
        entry.team.joinToParent()
        entry.agilityClass.joinToParent()
        entry.select("AgilityClass.idCompetition=$idCompetition AND team.idDog=${dog.id}")

        while (entry.next()) {
            entry.progress = PROGRESS_WITHDRAWN
            entry.post()
        }
        kcListDogEntries(idCompetition, idDog)
    }
}

*/

enum class AwardsRule { NONE, KCI }

fun proposedPlaces(entries: Int, rule: AwardsRule = AwardsRule.NONE): Int {
    var result = (entries + 5) / 10
    when (rule) {
        AwardsRule.NONE -> return result.fixRange(1, result)
        AwardsRule.KCI -> return result.fixRange(4, result)
    }
}

fun proposedTrophies(entries: Int, rule: AwardsRule = AwardsRule.NONE): Int {
    when (rule) {
        AwardsRule.NONE, AwardsRule.KCI -> {
            when (entries) {
                in 0..49 -> return 1
                in 50..75 -> return 2
                else -> return 3
            }
        }
    }
}

fun stringListToIntList(strings: List<String>): ArrayList<Int> {
    val result = ArrayList<Int>()
    strings.forEach { result.add(it.toIntDef(0)) }
    return result
}

class AwardRule(val rule: String) {
    val ruleType = if (rule.startsWith("bands:")) "bands" else if (rule.contains("%")) "%" else ""
    private val percentage = if (ruleType == "%") rule.substringBefore("%").toIntDef(10) else 1
    private val uplift = if (ruleType == "%") rule.substringAfter("%+").toIntDef(0) else 0
    private val band =
        if (ruleType == "bands") stringListToIntList(rule.substringAfter(":").split(",")) else ArrayList<Int>()

    fun getUnits(entries: Int): Int {
        if (ruleType == "%") {
            val result = (entries + uplift) * percentage / 100
            return if (result > 0) result else 1
        } else {
            for (index in band.indices) {
                if (entries <= band[index]) {
                    debug("AwardRule", "RULE A: $rule, $entries, $index")
                    return index
                }
            }
            debug("AwardRule", "RULE B: $rule, $entries, ${band.size}")
            return band.size
        }
    }
}

/*fun kcSplitClassLHO(idAgilityClass: Int = 1201979189) {
    val fho = AgilityClass()
    val lho = AgilityClass()
    fho.find(idAgilityClass)
    lho.append()

    val heightCode = fho.heightCodes
    val part = fho.part
    if (fho.heightOptions != "$heightCode:$heightCode|${heightCode}L") {
        throw Wobbly("Can't LHO split class $idAgilityClass ${fho.name}")
    }

    fho.heightOptions = "$heightCode:$heightCode"
    fho.jumpHeightCodes = heightCode
    fho.heightRunningOrder = heightCode
    fho.part = part.spaceAppend("FH")
    fho.partType = PART_TYPE_HEIGHT_SPLIT
    fho.name = fho.describeClass(true, true)
    fho.nameLong = fho.describeClass(false, true)
    fho.post()

    lho.idCompetition = fho.idCompetition
    lho.dataProviderName = fho.dataProviderName
    lho.number = fho.number
    lho.numberSuffix = fho.numberSuffix
    lho.part = part.spaceAppend("LHO")
    lho.code = fho.code
    lho.date = fho.date
    lho.heightOptions = "$heightCode:${heightCode}L"
    lho.heightCodes = fho.heightCodes
    lho.jumpHeightCodes = "${heightCode}L"
    lho.heightRunningOrder = "${heightCode}L"
    lho.gradeCodes = fho.gradeCodes
    lho.sponsor = fho.sponsor
    lho.prefix = fho.prefix
    lho.suffix = fho.suffix
    lho.name = lho.describeClass(true, true)
    lho.nameLong = lho.describeClass(false, true)
    lho.ringNumber = fho.ringNumber
    lho.partType = fho.partType
    lho.post()

    val entry = Entry()
    entry.select("idAgilityClass=$idAgilityClass")
    while (entry.next()) {
        if (entry.jumpHeightCode == heightCode) {
            entry.subClass = fho.chooseSubClass(entry.gradeCode, entry.heightCode, entry.jumpHeightCode, 0)
        } else {
            entry.idAgilityClass = lho.id
            entry.subClass = lho.chooseSubClass(entry.gradeCode, entry.heightCode, entry.jumpHeightCode, 0)
        }
        entry.post()
    }

}*/

fun fixPlaceSheet(idCompetition: Int) {
    val competition = Competition()
    val agilityClass = AgilityClass()
    competition.find(idCompetition)
    agilityClass.select("idCompetition=$idCompetition", "ClassNumber")

    val rosetteRule = AwardRule(competition.rosetteRule)
    val trophyRule = AwardRule(competition.trophyRule)

    while (agilityClass.next()) {
        for (subClass in 0..agilityClass.subClassCount - 1) {
            val query =
                DbQuery("SELECT COUNT(*) AS entries FROM entry WHERE idAgilityClass = ${agilityClass.id} AND subClass = ${subClass}")
            query.first()
            val entries = query.getInt("entries")
            val places = agilityClass.getSubClassRosettes(subClass)
            val trophies = agilityClass.getSubClassTrophies(subClass)
            val proposedPlaces = rosetteRule.getUnits(entries)
            val proposedTrophies = trophyRule.getUnits(entries)
            agilityClass.setSubClassRosettes(subClass, proposedPlaces)
            agilityClass.setSubClassTrophies(subClass, proposedTrophies)
            agilityClass.post()
        }
    }
}

object KcUtils {

    fun cloneClass(source: AgilityClass, target: AgilityClass) {
        target.idCompetition = source.idCompetition
        target.date = source.date
        target.number = source.number
        target.numberSuffix = source.numberSuffix
        target.code = source.code
        target.codeInstance = source.codeInstance
        target.heightCodes = source.heightCodes
        target.gradeCodes = source.gradeCodes
        target.entryFee = source.entryFee
        target.entryFeeMembers = source.entryFeeMembers

        target.prefix = source.prefix
        target.suffix = source.suffix
        target.sponsor = source.sponsor
        target.judge = source.judge

        target.ageBaseDate = source.ageBaseDate
        target.heightOptions = source.heightOptions
        target.jumpHeightCodes = source.jumpHeightCodes
        target.heightRunningOrder = source.heightRunningOrder
        target.runCount = source.runCount
        target.qCode = source.qCode

        target.name = source.name
        target.nameLong = source.nameLong

        target.flag = source.flag
        target.rules = source.rules

    }

    fun splitClass(idAgilityClass: Int, splitCount: Int) {
        dbTransaction {
            val idList = ArrayList<Int>()

            val source = AgilityClass(idAgilityClass)
            source.part = "Part 1"
            source.numberSuffix = "a"
            source.partType = PART_TYPE_NUMBER_SPLIT
            source.name = source.describeClass()
            source.nameLong = source.describeClass(short = false)
            source.post()
            idList.add(source.id)

            for (i in 1..splitCount - 1) {
                val target = AgilityClass()
                target.append()
                target.cloneFrom(source, "idAgilityClass")
                target.part = "Part ${i + 1}"
                target.numberSuffix = ('a'.toInt() + i).toChar().toString()
                target.partType = PART_TYPE_NUMBER_SPLIT
                target.name = target.describeClass()
                target.nameLong = target.describeClass(short = false)
                target.post()
                idList.add(target.id)
            }

            var index = 0
            Entry().where("idAgilityClass=$idAgilityClass AND entry.progress=0", "RAND()") {
                this.idAgilityClass = idList[index]
                post()
                index = (index + 1).rem(splitCount)
            }
        }
    }

    fun splitClassByHeight(idAgilityClass: Int): Int {
        val fh = AgilityClass(idAgilityClass)
        val lh = AgilityClass()
        val heightCode = fh.heightCodes

        lh.append()
        lh.cloneFrom(fh, "idAgilityClass")

        fh.part = "FHO"
        lh.part = "LHO"

        fh.numberSuffix = "a"
        lh.numberSuffix = "b"

        fh.partType = PART_TYPE_HEIGHT_SPLIT
        lh.partType = PART_TYPE_HEIGHT_SPLIT

        fh.name = fh.describeClass()
        lh.name = lh.describeClass()

        fh.nameLong = fh.describeClass(short = false)
        lh.nameLong = lh.describeClass(short = false)


        fh.heightOptions = "$heightCode:$heightCode"
        lh.heightOptions = "$heightCode:${heightCode}L"

        fh.jumpHeightCodes = heightCode
        lh.jumpHeightCodes = heightCode + "L"

        fh.heightRunningOrder = heightCode
        lh.heightRunningOrder = heightCode + "L"

        fh.post()
        lh.post()

        Entry().where("idAgilityClass=$idAgilityClass") {
            if (jumpHeightCode.endsWith("L")) {
                this.idAgilityClass = lh.id
            }
            subClass = 0
            post()
        }
        return lh.id
    }

    fun splitClassByGrade(idAgilityClass: Int, gradeCodePartB: String): Int {
        val partA = AgilityClass(idAgilityClass)
        val partB = AgilityClass()
        var gradeCodesPartA = ""
        partA.gradeCodes.split(";").forEach {
            if (it!=gradeCodePartB) {
                gradeCodesPartA = gradeCodesPartA.append(it, ";")
            }
        }

        partB.append()
        partB.cloneFrom(partA, "idAgilityClass")

        partA.gradeCodes=gradeCodesPartA
        partB.gradeCodes=gradeCodePartB

        partA.numberSuffix = "a"
        partB.numberSuffix = "b"

        partA.partType = PART_TYPE_GRADE_SPLIT
        partB.partType = PART_TYPE_GRADE_SPLIT

        partA.name = partA.describeClass()
        partB.name = partB.describeClass()

        partA.nameLong = partA.describeClass(short = false)
        partB.nameLong = partB.describeClass(short = false)

        partA.post()
        partB.post()

        Entry().where("idAgilityClass=$idAgilityClass") {
            if (gradeCode==gradeCodePartB) {
                this.idAgilityClass = partB.id
                subClass = partB.chooseSubClass(gradeCode, heightCode, jumpHeightCode, 0)
            } else {
                subClass = partA.chooseSubClass(gradeCode, heightCode, jumpHeightCode, 0)
            }
            post()
        }

        return partB.id
    }
    
    fun updateClass(
        competition: Competition,
        template: ClassTemplate,
        idAgilityClass: Int = 0,
        classNumber: Int = 0,
        classNumberSuffix: String = "",
        block: Int = -1,
        classDate: Date,
        gradeCodes: String = "",
        heightOptions: String,
        heightCodes: String = "",
        jumpHeightCodes: String,
        heightRunningOrder: String,
        prefix: String = "",
        suffix: String = "",
        sponsor: String = "",
        judge: String = "",
        entryFee: Int = 0,
        runCount: Int = 0,
        qCode: String = "",
        rules: String = "",
        entryFeeMembers: Int = 0,
        idAgilityClassParent: Int = -1,
        child: Boolean = false,
        forceSponsor: Boolean = false
    ): Int {

        val agilityClass = AgilityClass()
        if (child) {
            agilityClass.find("idCompetition=${competition.id} AND ClassNumber=$classNumber AND classCode=${template.code}")
        } else if (idAgilityClass > 0) {
            agilityClass.find("idCompetition=${competition.id} AND idAgilityClass=$idAgilityClass")
        } else if (classNumber > 0) {
            agilityClass.find("idCompetition=${competition.id} AND classNumber=$classNumber AND classNumberSuffix=${classNumberSuffix.quoted}")
        }
        if (!agilityClass.found()) {
            agilityClass.append()
        }
        if (idAgilityClassParent > -1) agilityClass.idAgilityClassParent = idAgilityClassParent
        agilityClass.idCompetition = competition.id
        agilityClass.date = classDate
        if (classNumber > 0) agilityClass.number = classNumber
        if (classNumber > 0) agilityClass.numberSuffix = classNumberSuffix
        agilityClass.code = template.code
        if (block >= 0) agilityClass.block = block
        if (heightCodes.isNotEmpty()) agilityClass.heightCodes = heightCodes
        if (gradeCodes.isNotEmpty()) agilityClass.gradeCodes = gradeCodes
        if (entryFee > 0) agilityClass.entryFee = entryFee
        if (entryFeeMembers > 0) agilityClass.entryFeeMembers = entryFeeMembers

        agilityClass.prefix = prefix
        agilityClass.suffix = suffix
        agilityClass.sponsor = sponsor
        agilityClass.judge = judge

        agilityClass.heightOptions = heightOptions
        agilityClass.jumpHeightCodes = jumpHeightCodes
        agilityClass.heightRunningOrder = heightRunningOrder
        agilityClass.runCount = runCount
        agilityClass.qCode = qCode
        agilityClass.name = agilityClass.describeClass(forceSponsor = forceSponsor)
        agilityClass.nameLong = agilityClass.describeClass(short = false)

        agilityClass.flag = false
        agilityClass.rules = rules

        if (agilityClass.rules.split(",").contains("D1")) {
            agilityClass.ageBaseDate = agilityClass.date
        } else {
            agilityClass.ageBaseDate =
                if (template.ageLower > 0 || template.ageUpper > 0) competition.ageBaseDate else nullDate
        }

        agilityClass.post()

        if (template.hasChildren) {
            for (child in template.children) {
                updateClass(
                    competition = competition,
                    template = child,
                    classNumber = classNumber,
                    classNumberSuffix = classNumberSuffix,
                    block = block,
                    classDate = classDate,
                    gradeCodes = gradeCodes,
                    heightOptions = heightOptions,
                    heightCodes = heightCodes,
                    jumpHeightCodes = jumpHeightCodes,
                    heightRunningOrder = heightRunningOrder,
                    prefix = prefix,
                    suffix = suffix,
                    sponsor = sponsor,
                    judge = judge,
                    entryFee = entryFee,
                    runCount = runCount,
                    qCode = qCode,
                    rules = rules,
                    entryFeeMembers = entryFeeMembers,
                    idAgilityClassParent = agilityClass.id,
                    child = true,
                    forceSponsor = forceSponsor
                )
            }
        }


        return agilityClass.id
    }


}

fun kcResetClass(idAgilityClass: Int) {
    Entry().where("idAgilityClass=$idAgilityClass AND progress=$PROGRESS_RUN") {
        saveOldScore("ClassReset")
        scoreCodes = ""
        time = 0
        noTime = false
        hasRun = false
        progress = PROGRESS_ENTERED
        post()
    }
}

