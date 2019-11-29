/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.library.general

import org.egility.library.dbobject.AgilityClass
import org.egility.library.dbobject.Entry
import java.util.*
import kotlin.collections.ArrayList

/**
 * Created by mbrickman on 09/03/16.
 */

class ClassTemplate(
    val parent: ClassTemplate? = null,
    val previous: ClassTemplate? = null,

    var code: Int,
    var nameTemplate: String = "",
    var nameTemplateShort: String = "",
    var rawName: String = "",
    var groupName: String = "",
    var sponsor: String = "",
    var logo: String = "",
    var website: String = "",
    var ruleBook: String = "UKA",

    var discipline: Int = DISCIPLINE_AGILITY,
    var scoringMethod: Int = SCORING_METHOD_FAULTS,
    var programme: Int = PROGRAMME_NONE,
    var entryRule: Int = ENTRY_RULE_GRADE1,
    var type: Int = CLASS_TYPE_REGULAR,

    var combineHeights: Boolean = false,
    var strictRunningOrder: Boolean = false,
    var runningOrderSort: String = "runningOrder, rand()",
    var runningOrderStart: Int = 1,
    var courseTimeCode: String = "",
    var courseLengthNeeded: Boolean = true,
    var courseTimeNeeded: Boolean = true,
    var discretionaryCourseTime: Boolean = false,
    var noTimeFormula: Boolean = false,
    var noTimeFaults: Boolean = false,
    var eliminationTime: Int = 0,
    var flagTies: Boolean = true,
    var day: Int = 1,
    var nfcOnly: Boolean = false,

    var teamSize: Int = 1,
    var teamReserves: Int = 0,
    var dualHandler: Boolean = false,
    var column: Int = 1,
    var maxColumn: Int = 0,

    var runUnits: Int = 1,
    var entryFee: Int = 0,
    var lateEntryFee: Int = 0,

    var cut_min: Int = 0,
    var cut_max: Int = 0,
    var cut_percent: Int = 0,
    var cutPercentOf: String = "placed", // other option is "entered"
    var cut_max_little: Int = 0,
    var cut_columns: Int = 0,

    var resultsCopies: Int = -1,
    var awardsCopies: Int = -1,
    var clearRoundRosettes: Boolean = true,

    var verifyHandler: Boolean = false,

    var ageLower: Int = 0,
    var ageUpper: Int = 0,
    val faultToTime: Int = 1000,

    val qualifierPlaces: Int = 0,
    val qualifierScore: Int = 0,

    var printIntermediateResults: Boolean = false,
    var summarizeEliminations: Boolean = true,

    val harvestedResults: Int = 1,

    val gamble1: Int = 10,
    val gamble2: Int = 15,
    val gambleBonus: Int = 0,
    val gambleFaults: Boolean = false,
    val gamble2TimeDeduct: Int = 0,

    val grouped: Boolean = false,
    val whenClosed: ((AgilityClass) -> Unit)? = null,
    val whenChildClosed: ((AgilityClass, Int) -> Unit)? = null,
    val getParentPoints: ((Entry) -> Int)? = null,
    val rules: String = "",
    
    val subDivisions: String = "",

    val feedWithdrawn: Boolean = false,
    val ukaFinalsPlaces: Int = 0,
    val ukaFinalsQualifierCode: Int=0,
    val batonFaults: Int=0,


val defunct: Boolean = false
) {

    var next: ClassTemplate? = null
    val children: ArrayList<ClassTemplate> = ArrayList<ClassTemplate>()

    init {
        members.add(this)
        if (parent != null) {
            parent.children.add(this)
            this.sponsor = parent.sponsor
        }
        if (previous != null) {
            previous.next = this
        }
    }

    val shortTemplate: String
        get() = if (nameTemplateShort.isNotEmpty()) nameTemplateShort else nameTemplate

    val hasChildren: Boolean
        get() = children.size > 0

    val isHarvested: Boolean
        get() = type == CLASS_TYPE_HARVESTED

    val isSeries: Boolean
        get() = type == CLASS_TYPE_SPECIAL_SERIES || type == CLASS_TYPE_SPECIAL_SERIES_MEMBER

    val isGraded: Boolean
        get() = nameTemplate.contains("<grade>")

    val gradeCodes: String
        get() =
            if (this == MASTERS || parent == MASTERS)
                "UKA04"
            else if (isUka && !isGraded)
                "UKA01,UKA01,UKA03,UKA04"
            else
                ""

    val heightOptions: String
        get() =
            if (this == CASUAL_AGILITY || this == CASUAL_JUMPING || this == CASUAL_STEEPLECHASE)
                "UKA300:UKA200,UKA400:UKA300|UKA200,UKA550:UKA400|UKA300|UKA200,UKA650:UKA550|UKA400|UKA300|UKA200"
            else if (this == NURSERY_AGILITY)
                "UKA300:UKA550|UKA400|UKA300|UKA200,UKA400:UKA550|UKA400|UKA300|UKA200,UKA550:UKA550|UKA400|UKA300|UKA200,UKA650:UKA550|UKA400|UKA300|UKA200"
            else if (this.oneOf(JUNIOR_MASTERS, JUNIOR_MASTERS_AGILITY, JUNIOR_MASTERS_JUMPING))
                "UKA300,UKA400:UKA300,UKA550,UKA650:UKA550"
            else if (isUka && isRelay)
                "UKA300:UKA901|UKA902|UKA903,UKA400:UKA901|UKA902|UKA903,UKA550:UKA902|UKA903|UKA904,UKA650:UKA902|UKA903|UKA904"
            else if (isUka)
                "UKA300,UKA400,UKA550,UKA650"
            else if (isFab)
                "FAB200,FAB300,FAB400,FAB500,FAB600"
            else if (isIfcs)
                "IF300,IF400,IF500,IF600"
            else
                ""

    val heightCodes: String
        get() {
            var result = ""
            for (heightOption in heightOptions.split(",")) {
                result = result.semiColonAppend(heightOption.substringBefore(":"))
            }
            return if (combineHeights) result.replace(";", ",") else result
        }

    val jumpHeightCodes: String
        get() {
            val jumpHeights = ArrayList<String>()
            for (heightOption in heightOptions.split(",")) {
                for (jumpHeightCode in heightOption.substringAfter(":").split("|")) {
                    if (!jumpHeights.contains(jumpHeightCode)) jumpHeights.add(jumpHeightCode)
                }
            }
            Collections.sort(jumpHeights, { a, b -> a.compareTo(b) })
            var result = ""
            jumpHeights.forEach { result = result.commaAppend(it) }
            return result
        }

    val childCount: Int
        get() = children.size

    val isChild: Boolean
        get() = parent != null

    val resultsCount: Int
        get() {
            if (this == TEAM) {
                return 4
            } else if (isHarvested) {
                return harvestedResults
            } else {
                return childCount
            }
        }

    val needsPreparation: Boolean
        get() = isSpecialParent

    val canEnterDirectly: Boolean
        get() = entryRule <= ENTRY_RULE_MAX_CAN_ENTER_DIRECTLY

    val canEnterOnline: Boolean
        get() = entryRule < ENTRY_RULE_INVITE

    val isHarvestedGroup: Boolean
        get() = type == CLASS_TYPE_HARVESTED

    val isSpecialClass: Boolean
        get() = code in 100..199

    val isAgility: Boolean
        get() = discipline == DISCIPLINE_AGILITY

    val isAnySize: Boolean
        get() = this == KC_ANY_AGILITY || this == KC_ANY_JUMPING || this == KC_ANY_STEEPLECHASE || this == KC_ANY_HELTERSKELTER

    val isVeteran: Boolean
        get() = /*this == KC_VETERAN_AGILITY ||*/ this == KC_VETERAN_JUMPING /*|| this == KC_VETERAN_STEEPLECHASE */

    val isChampionship: Boolean
        get() = this == KC_CHAMPIONSHIP

    val isYkc: Boolean
        get() = this.oneOf(KC_YKC_AGILITY_U12, KC_YKC_AGILITY_U18, KC_YKC_JUMPING_U12, KC_YKC_JUMPING_U18, KC_YKC_TEAM)

    val isKcRegular: Boolean
        get() = this.oneOf(KC_AGILITY, KC_JUMPING)

    val isJumping: Boolean
        get() = discipline == DISCIPLINE_JUMPING

    val isSteeplechase: Boolean
        get() = discipline == DISCIPLINE_STEEPLECHASE

    val isGamblers: Boolean
        get() = discipline == DISCIPLINE_GAMBLERS

    val isSnooker: Boolean
        get() = discipline == DISCIPLINE_SNOOKER

    val isPowerAndSpeed: Boolean
        get() = discipline == DISCIPLINE_POWER_AND_SPEED

    val isTimeOutAndFault: Boolean
        get() = discipline == DISCIPLINE_TIME_FAULT_AND_OUT

    val isSnakesAndLadders: Boolean
        get() = discipline == DISCIPLINE_SNAKES_AND_LADDERS

    val isUkaProgression: Boolean
        get() = programme.oneOf(PROGRAMME_PERFORMANCE, PROGRAMME_STEEPLECHASE)

    val isKcProgression: Boolean
        get() = this.oneOf(KC_AGILITY, KC_JUMPING)

    val isCut: Boolean
        get() = cut_percent != 0 || cut_min != 0 || cut_max != 0

    fun cutSize(entryCount: Int, jumpHeightCode: String = ""): Int {
        val percent =
            if (cut_percent > 0) Math.round((entryCount.toDouble() * cut_percent.toDouble()) / 100.0).toInt() else 0
        if (percent < cut_min) {
            return cut_min
        }
        if (cut_max_little > 0 && jumpHeightCode.oneOf(
                "UKA300",
                "UKA400"
            ) && (percent == 0 || percent > cut_max_little)
        ) {
            return cut_max_little
        }
        if (cut_max > 0 && (percent == 0 || percent > cut_max)) {
            return cut_max
        }
        return percent
    }

    val impliedMaxColumn = if (maxColumn > 0) maxColumn else column
    val dummyClass: Boolean
        get() = this.oneOf(TRY_OUT, KC_CHAMPIONSHIP)

    val feeder: ClassTemplate?
        get() {
            if (this == TEAM_RELAY || this == TRY_OUT_PENTATHLON_SPEEDSTAKES) {
                return parent
            } else if (previous != null) {
                return previous
            } else if (isChild) {
                return parent
            } else {
                return null
            }
        }

    fun getCourseTimeCode(gradeCodes: String): String {
        if (courseTimeCode.isEmpty()) {
            if (gradeCodes.oneOf("", "*")) {
                return ""
            }
            return gradeCodes.replace(";", ",").split(",")[0]
        } else {
            return courseTimeCode
        }
    }

    val isPointsBased: Boolean
        get() = scoringMethod != SCORING_METHOD_FAULTS


    val isNfcAllowed: Boolean
        get() = (ruleBook == "UKA" && type.oneOf(CLASS_TYPE_REGULAR)) || ruleBook == "FAB"

    val isRegular: Boolean
        get() = ruleBook == "UKA" && type.oneOf(CLASS_TYPE_REGULAR)

    val isUka: Boolean
        get() = ruleBook == "UKA"

    val isFab: Boolean
        get() = ruleBook == "FAB"

    val isIfcs: Boolean
        get() = ruleBook == "IFCS"

    val isUkOpen: Boolean
        get() = ruleBook == "UK_OPEN"

    val isKc: Boolean
        get() = ruleBook == "KC"

    val isIndependent: Boolean
        get() = ruleBook == "IND"

    val isSubClass: Boolean
        get() = type == CLASS_TYPE_SUB_CLASS

    val isSuperClass: Boolean
        get() = hasChildren && children[0].isSubClass

    val isAddOnAllowed: Boolean
        get() = type.oneOf(CLASS_TYPE_REGULAR)

    val isSpecialParent: Boolean
        get() = type.oneOf(CLASS_TYPE_SPECIAL, CLASS_TYPE_SPECIAL_GROUP, CLASS_TYPE_SPECIAL_SERIES)

    val columns: String
        get() = when {
            parent != null && parent.oneOf(
                MASTERS, TRY_OUT_PENTATHLON, JUNIOR_MASTERS,
                UK_OPEN_PENTATHLON, UK_OPEN_PENTATHLON_AGILITY1, UK_OPEN_PENTATHLON_JUMPING1,
                UK_OPEN_PENTATHLON_AGILITY2, UK_OPEN_PENTATHLON_JUMPING2, UK_OPEN_STEEPLECHASE_ROUND1,
                UK_OPEN_BIATHLON_JUMPING, UK_OPEN_BIATHLON
            ) ->
                "place,competitor,runData,faults,time,points"
            parent != null && parent.oneOf(JUNIOR_OPEN, JUNIOR_OPEN_FINAL) ->
                "place,competitor,runData,faults,time,courseTime,points"
            parent != null && parent.oneOf(CHALLENGE_FINAL) || this.oneOf(
                KC_CHAMPIONSHIP_JUMPING, KC_CHAMPIONSHIP_AGILITY,
                KC_CHAMPIONSHIP_FINAL
            ) ->
                "place,competitor,runData,faults,time"
            parent != null && parent.oneOf(
                GAMES_CHALLENGE,
                //SW_GAMES_CHALLENGE,
                TRY_OUT_GAMES,
                UK_OPEN_GAMES,
                UK_OPEN_GAMES_SNOOKER
            ) ->
                "place,score,time,competitor,runData"
            this.oneOf(UK_OPEN_STEEPLECHASE_ROUND2, UK_OPEN_STEEPLECHASE_ROUND1) ->
                "place,competitor,runData,faults,time,points"
            this.oneOf(
                GRAND_PRIX_SEMI_FINAL,
                GRAND_PRIX_FINAL,
                BEGINNERS_STEEPLECHASE_SEMI_FINAL,
                BEGINNERS_STEEPLECHASE_FINAL,
                /*
                SW_STEEPLECHASE_SEMI_FINAL1,
                SW_STEEPLECHASE_FINAL1,
                SW_STEEPLECHASE_SEMI_FINAL2,
                SW_STEEPLECHASE_FINAL2,
                SW_CHALLENGE_JUMPING1,
                SW_CHALLENGE_AGILITY1,
                SW_CHALLENGE_JUMPING2,
                SW_CHALLENGE_AGILITY2,
                */
                UK_OPEN_CHALLENGER,
                UK_OPEN_CHAMPIONSHIP_JUMPING,
                /* UK_OPEN_CHAMPIONSHIP_JUMPING_GROUP, */
                UK_OPEN_CHAMPIONSHIP_AGILITY,
                UK_OPEN_CHAMPIONSHIP_FINAL
            ) ->
                "place,competitor,runData,faults,time"
            this.oneOf(MASTERS, JUNIOR_OPEN, JUNIOR_OPEN_FINAL, JUNIOR_MASTERS) ->
                "place,competitor,jumping,agility,total"
            this.oneOf(CHALLENGE, CHALLENGE_FINAL) ->
                "place,faults,time,competitor,jumping,agility"
            this.oneOf(KC_CHAMPIONSHIP_HEAT) ->
                "place,competitor,jumping,agility,total,faults,time"
            this.oneOf(TRY_OUT_PENTATHLON) ->
                "place,competitor,agility1,jumping1,jumping2,agility2,speedstakes,total"
            this.oneOf(TEAM) ->
                "place,team,dog1,dog2,dog3,relay,time,points"
            this.oneOf(TEAM_INDIVIDUAL) ->
                "place,team,competitor,runData,faults,time"
            this.oneOf(TEAM_RELAY, KC_CRUFTS_TEAM) ->
                "place,team,runData,faults,time"
            this.oneOf(SPLIT_PAIRS) ->
                "place,pair,runData,faults,time,points"
            this.oneOf(GRAND_PRIX) ->
                "place,faults,time,competitor"
            this.oneOf(GAMES_CHALLENGE, TRY_OUT_GAMES, UK_OPEN_GAMES) ->
                "place,competitor,snooker,gamblers,total,time"
            this.oneOf(SNOOKER, GAMBLERS, FAB_IFCS_GAMBLERS, FAB_IFCS_SNOOKER) ->
                "prize,score,time,competitor,runData,progressionPoints"
            else ->
                "prize,faults,time,competitor,runData,progressionPoints"
        }

    var headings: String = ""
        get() {
            if (field.isEmpty()) {
                for (column in columns.split(",")) {
                    field = field.commaAppend(
                        column.camelNameToTitle().replace(
                            "Steeplechase",
                            "S/Chase"
                        ).replace("Course Time", "C/T")
                    )
                }
            }
            return field
        }

    fun oneOf(vararg items: ClassTemplate): Boolean {
        for (item in items) {
            if (item == this) {
                return true
            }
        }
        return false
    }

    val isCasual: Boolean
        get() = oneOf(CASUAL_AGILITY, CASUAL_JUMPING, CASUAL_STEEPLECHASE)

    val isNursery: Boolean
        get() = oneOf(NURSERY_AGILITY)

    val isJunior: Boolean
        get() = oneOf(
            JUNIOR_AGILITY, JUNIOR_JUMPING, JUNIOR_OPEN, JUNIOR_OPEN_AGILITY, JUNIOR_OPEN_JUMPING,
            JUNIOR_MASTERS, JUNIOR_MASTERS_AGILITY, JUNIOR_MASTERS_JUMPING
        )

    val resultsOrderBy: String
        get() {
            return when (scoringMethod) {
                SCORING_METHOD_FAULTS -> "if(courseFaults>=100, courseFaults, 0), clearRoundOnly, faults, time"
                SCORING_METHOD_GAMES -> "qualifying DESC, if(courseFaults>=100, courseFaults, 0), points DESC, time"
                SCORING_METHOD_JUNIOR -> "points DESC"
                SCORING_METHOD_KC_CHAMP -> "points, faults, time"
                SCORING_METHOD_KC_POINTS_DESC -> "points DESC, faults, time"
                else -> "points"
            }
        }

    val isRelay: Boolean
        get() = oneOf(KC_PAIRS_AGILITY, KC_PAIRS_JUMPING, SPLIT_PAIRS, TEAM_RELAY, TEAM, KC_PAIRS_MIXI, KC_CRUFTS_TEAM)

    val isTeamEvent: Boolean
        get() = teamSize > 1

    val bookIn: Boolean
        get() = !strictRunningOrder

    fun getPoints(
        courseTime: Int,
        time: Int,
        faults: Int,
        gamesPoints: Int,
        isEliminated: Boolean,
        isNonRunner: Boolean
    ): Int {
        when (scoringMethod) {
            SCORING_METHOD_MASTERS -> {
                if (isNonRunner) {
                    return 60000 + FAULTS_NON_RUNNER * 1000
                } else if (isEliminated) {
                    return 60000 + FAULTS_ELIMINATED * 1000
                } else {
                    return time + faults
                }
            }
            SCORING_METHOD_WAO_PENTATHLON -> {
                if (isNonRunner) {
                    return FAULTS_NON_RUNNER * 1000
                } else if (isEliminated) {
                    return 50000 + 50 * 1000
                } else {
                    return time + faults
                }
            }
            SCORING_METHOD_UK_OPEN_PENTATHLON, SCORING_METHOD_UK_OPEN_STEEPLECHASE -> {
                if (isNonRunner) {
                    return FAULTS_NON_RUNNER * 1000
                } else if (isEliminated) {
                    return courseTime + 100 * 1000
                } else {
                    return time + faults
                }
            }
            SCORING_METHOD_RELAY -> {
                if (isNonRunner) {
                    return FAULTS_NON_RUNNER * 1000
                } else {
                    return time + faults
                }
            }
            SCORING_METHOD_TEAM_INDIVIDUAL -> {
                if (isNonRunner) {
                    return FAULTS_NON_RUNNER * 1000
                } else {
                    return time + faults
                }
            }
            SCORING_METHOD_FAULTS_AS_TIME -> {
                if (isNonRunner) {
                    return FAULTS_NON_RUNNER * 1000
                } else if (isEliminated) {
                    return FAULTS_ELIMINATED * 1000
                } else {
                    return time + faults
                }
            }
            SCORING_METHOD_JUNIOR -> {
                if (isNonRunner) {
                    return -FAULTS_NON_RUNNER * 1000
                } else if (isEliminated) {
                    return -FAULTS_ELIMINATED * 1000
                } else {
                    return courseTime - time - faults
                }
            }
            SCORING_METHOD_GAMES -> {
                return if (isEliminated || isNonRunner) 0 else gamesPoints
            }
            else -> {
                if (isNonRunner) {
                    return FAULTS_NON_RUNNER * 1000
                } else {
                    return faults
                }
            }
        }

    }

    companion object {

        val members = ArrayList<ClassTemplate>()

        fun addWhere(isMember: (ClassTemplate) -> Boolean): String {
            val list = StringBuilder()
            for (template in members) {
                if (isMember(template)) {
                    list.csvAppend(template.code.toString())
                }
            }
            return list.toString()
        }

        var strictRunningOrderList: String = ""
            get() {
                if (field.isEmpty()) field = addWhere { it.strictRunningOrder }
                return field
            }

        var specialGroupMemberList: String = ""
            get() {
                if (field.isEmpty()) field = addWhere { it.type == CLASS_TYPE_SPECIAL_GROUP_MEMBER }
                return field
            }

        var fabAgilityGradeList: String = ""
            get() {
                if (field.isEmpty()) field = addWhere { it.isFab && it.entryRule==ENTRY_RULE_GRADE1 }
                return field
            }

        var fabJumpingGradeList: String = ""
            get() {
                if (field.isEmpty()) field = addWhere { it.isFab && it.entryRule==ENTRY_RULE_GRADE2 }
                return field
            }

        var fabSteeplechaseGradeList: String = ""
            get() {
                if (field.isEmpty()) field = addWhere { it.isFab && it.entryRule==ENTRY_RULE_GRADE3 }
                return field
            }

        var fabAnyGradeList: String = ""
            get() {
                if (field.isEmpty()) field = addWhere { it.isFab && !it.entryRule.oneOf(ENTRY_RULE_GRADE1, ENTRY_RULE_GRADE2, ENTRY_RULE_GRADE3) }
                return field
            }

        var fabList: String = ""
            get() {
                if (field.isEmpty()) field = addWhere { it.isFab }
                return field
            }

        var ifcsList: String = ""
            get() {
                if (field.isEmpty()) field = addWhere { it.isIfcs }
                return field
            }

        var ukaProgressionList: String = ""
            get() {
                if (field.isEmpty()) field = addWhere { it.isUkaProgression }
                return field
            }

        var ukaTrophyList: String = ""
            get() {
                if (field.isEmpty()) field =
                    addWhere { it.isUkaProgression || it.isCasual || it.oneOf(JUNIOR_AGILITY, JUNIOR_JUMPING) }
                return field
            }


        var ukaFinalsQualifierList: String = ""
            get() {
                if (field.isEmpty()) field =
                    addWhere { it.ukaFinalsPlaces>0  }
                return field
            }

        var combineHeightsList: String = ""
            get() {
                if (field.isEmpty()) field =
                    addWhere { it.combineHeights  }
                return field
            }

        var specialParentList: String = ""
            get() {
                if (field.isEmpty()) field =
                    addWhere { it.isSpecialParent }
                return field
            }

        var casualList: String = ""
            get() {
                if (field.isEmpty()) field =
                    addWhere { it.isCasual }
                return field
            }

        var directEntryList: String = ""
            get() {
                if (field.isEmpty()) field =
                    addWhere { it.canEnterDirectly }
                return field
            }

        private val programmeMap = hashMapOf<Int, String>()

        fun getProgrammeList(programme: Int): String {
            if (programmeMap.get(programme)?.isEmpty() ?: true) {
                programmeMap.put(programme, addWhere { it.programme == programme })
            }
            return programmeMap.get(programme) ?: ""
        }


        fun select(code: Int): ClassTemplate {
            for (template in members) {
                if (template.code == code) {
                    return template
                }
            }
            return UNDEFINED
        }

        fun codeList(list: List<ClassTemplate>): String {
            val result = StringBuilder()
            for (item in list) {
                result.csvAppend(item.code.toString())
            }
            return result.toString()
        }

        val UNDEFINED = ClassTemplate(
            type = CLASS_TYPE_UNDEFINED,
            code = 999999,
            nameTemplate = "Undefined",
            entryRule = ENTRY_RULE_CLOSED
        )
        val KC_AGILITY = ClassTemplate(
            code = 1001,
            rawName = "Agility",
            nameTemplate = "<number><number_suffix> <sponsor> <prefix> <height> Agility <suffix> <grade> <lho> <part>",
            discipline = DISCIPLINE_AGILITY,
            ruleBook = "KC"
        )
        val KC_JUMPING = ClassTemplate(
            code = 1002,
            rawName = "Jumping",
            nameTemplate = "<number><number_suffix> <sponsor> <prefix> <height> Jumping <suffix> <grade> <lho> <part>",
            discipline = DISCIPLINE_JUMPING,
            ruleBook = "KC"
        )
        val KC_SPECIAL_AGILITY = ClassTemplate(
            code = 1011,
            rawName = "Special Agility",
            nameTemplate = "<number><number_suffix> <sponsor> Special <prefix> <height> Agility <suffix> <grade> <lho> <part>",
            discipline = DISCIPLINE_AGILITY,
            ruleBook = "KC"
        )
        val KC_SPECIAL_JUMPING = ClassTemplate(
            code = 1012,
            rawName = "Special Jumping",
            nameTemplate = "<number><number_suffix> <sponsor> Special <prefix> <height> Jumping <suffix> <grade> <lho> <part>",
            discipline = DISCIPLINE_JUMPING,
            ruleBook = "KC"
        )
        val KC_STEEPLECHASE = ClassTemplate(
            code = 1013,
            rawName = "Steeplechase",
            nameTemplate = "<number><number_suffix> <sponsor> Special  <prefix> <height> Steeplechase <suffix> <grade> <lho> <part>",
            discipline = DISCIPLINE_STEEPLECHASE,
            ruleBook = "KC"
        )
        val KC_HELTERSKELTER = ClassTemplate(
            code = 1014,
            rawName = "Helter Skelter",
            nameTemplate = "<number><number_suffix> <sponsor> Special  <prefix> <height> Helter Skelter <suffix> <grade> <lho> <part>",
            discipline = DISCIPLINE_STEEPLECHASE,
            ruleBook = "KC"
        )
        val KC_YKC_AGILITY_U12 = ClassTemplate(
            code = 1020,
            rawName = "YKC Agility U12",
            nameTemplate = "<number><number_suffix> YKC Agility Dog of the Year (Under 12) <height> <part>",
            nameTemplateShort = "<number><number_suffix> YKC Agility U12 <height> <part>",
            discipline = DISCIPLINE_AGILITY,
            ruleBook = "KC",
            verifyHandler = true,
            ageLower = 6,
            ageUpper = 11
        )
        val KC_YKC_AGILITY_U18 = ClassTemplate(
            code = 1021,
            rawName = "YKC Agility 12+",
            nameTemplate = "<number><number_suffix> YKC Agility Dog of the Year (12-17) <height> <part>",
            nameTemplateShort = "<number><number_suffix> YKC Agility 12+ <height> <part>",
            discipline = DISCIPLINE_AGILITY,
            ruleBook = "KC",
            verifyHandler = true,
            ageLower = 12,
            ageUpper = 17
        )
        val KC_YKC_JUMPING_U12 = ClassTemplate(
            code = 1022,
            rawName = "YKC Jumping U12",
            nameTemplate = "<number><number_suffix> YKC Jumping (Under 12) <height> <part>",
            nameTemplateShort = "<number><number_suffix> YKC Jumping U12 <height> <part>",
            discipline = DISCIPLINE_JUMPING,
            ruleBook = "KC",
            verifyHandler = true,
            ageLower = 6,
            ageUpper = 11
        )
        val KC_YKC_JUMPING_U18 = ClassTemplate(
            code = 1023,
            rawName = "YKC Jumping 12+",
            nameTemplate = "<number><number_suffix> YKC Jumping (12-17) <height> <part>",
            nameTemplateShort = "<number><number_suffix> YKC Jumping 12+ <height> <part>",
            discipline = DISCIPLINE_JUMPING,
            ruleBook = "KC",
            verifyHandler = true,
            ageLower = 12,
            ageUpper = 17
        )
        val KC_YKC_TEAM = ClassTemplate(
            code = 1024,
            rawName = "YKC Team",
            nameTemplate = "<number><number_suffix> YKC Team <height> <part>",
            nameTemplateShort = "<number><number_suffix> YKC Team <height> <part>",
            discipline = DISCIPLINE_JUMPING,
            ruleBook = "KC"
        )
        val KC_ANY_AGILITY = ClassTemplate(
            code = 1031,
            rawName = "Anysize Agility",
            nameTemplate = "<number><number_suffix> <sponsor> Special <prefix> <height> Any Size Agility <suffix> <grade> <lho> <part>",
            discipline = DISCIPLINE_AGILITY,
            ruleBook = "KC"
        )
        val KC_ANY_JUMPING = ClassTemplate(
            code = 1032,
            rawName = "Anysize Jumping",
            nameTemplate = "<number><number_suffix> <sponsor> Special <prefix> <height> Any Size Jumping <suffix> <grade> <lho> <part>",
            discipline = DISCIPLINE_JUMPING,
            ruleBook = "KC"
        )

        val KC_ANY_STEEPLECHASE = ClassTemplate(
            code = 1033,
            rawName = "Anysize Steeplechase",
            nameTemplate = "<number><number_suffix> <sponsor> Special <prefix> <height> Any Size Steeplechase <suffix> <grade> <lho> <part>",
            discipline = DISCIPLINE_STEEPLECHASE,
            ruleBook = "KC"
        )
        val KC_ANY_HELTERSKELTER = ClassTemplate(
            code = 1034,
            rawName = "Anysize Helter Skelter",
            nameTemplate = "<number><number_suffix> <sponsor> Special <prefix> <height> Any Size Helter Skelter <suffix> <grade> <lho> <part>",
            discipline = DISCIPLINE_STEEPLECHASE,
            ruleBook = "KC"
        )
        val KC_VETERAN_JUMPING = ClassTemplate(
            code = 1041,
            rawName = "Veteran Jumping",
            nameTemplate = "<number><number_suffix> <sponsor> Special <prefix> <height> Veteran Jumping <suffix> <grade> <lho> <part>",
            discipline = DISCIPLINE_JUMPING,
            ruleBook = "KC"
        )
        val KC_ABC_JUMPING= ClassTemplate(
            code = 1051,
            rawName = "ABC Jumping",
            nameTemplate = "<number><number_suffix> <sponsor> Special <prefix> <height> ABC Jumping <suffix> <grade> <lho> <part>",
            discipline = DISCIPLINE_JUMPING,
            ruleBook = "KC"
        )
        val KC_ABC_AGILITY = ClassTemplate(
            code = 1052,
            rawName = "ABC Agility",
            nameTemplate = "<number><number_suffix> <sponsor> Special <prefix> <height> ABC Agility <suffix> <grade> <lho> <part>",
            discipline = DISCIPLINE_AGILITY,
            ruleBook = "KC"
        )
        val KC_PAIRS_JUMPING = ClassTemplate(
            code = 1061,
            rawName = "Pairs Jumping",
            nameTemplate = "<number><number_suffix> <sponsor> Special  <prefix> <height> Pairs Jumping <suffix> <grade> <lho> <part>",
            discipline = DISCIPLINE_RELAY,
            teamSize = 2,
            runUnits = 2,
            ruleBook = "KC"
        )
        val KC_PAIRS_AGILITY = ClassTemplate(
            code = 1062,
            rawName = "Pairs Agility",
            nameTemplate = "<number><number_suffix> <sponsor> Special  <prefix> <height> Pairs Agility <suffix> <grade> <lho> <part>",
            discipline = DISCIPLINE_AGILITY,
            teamSize = 2,
            runUnits = 2,
            ruleBook = "KC"
        )
        val KC_PAIRS_MIXI = ClassTemplate(
            code = 1063,
            rawName = "Mixi Pairs",
            nameTemplate = "<number><number_suffix> <sponsor> Special  <prefix> <height> Mixi Pairs <suffix> <grade> <lho> <part>",
            discipline = DISCIPLINE_AGILITY,
            teamSize = 2,
            ruleBook = "KC"
        )
        val KC_EXCHANGE = ClassTemplate(
            code = 1064,
            rawName = "Tunnel Exchange",
            nameTemplate = "<number><number_suffix> <sponsor> Special  <prefix> <height> Tunnel Exchanges <suffix> <grade> <lho> <part>",
            discipline = DISCIPLINE_JUMPING,
            dualHandler = true,
            ruleBook = "KC"
        )
        val KC_INTERNATIONAL= ClassTemplate(
            code = 1070,
            rawName = "KC International",
            nameTemplate = "<number><number_suffix> <sponsor> Special KC International <prefix> <height> Agility <suffix> <part>",
            discipline = DISCIPLINE_AGILITY,
            ruleBook = "KC"
        )
        val KC_INTERNATIONAL_AGILITY = ClassTemplate(
            code = 1071,
            rawName = "KC International Agility",
            nameTemplate = "<number><number_suffix> <sponsor> Special KC International <prefix> <height> Agility <suffix> <part>",
            discipline = DISCIPLINE_AGILITY,
            ruleBook = "KC"
        )
        val KC_INTERNATIONAL_JUMPING = ClassTemplate(
            code = 1072,
            rawName = "KC International Jumping",
            nameTemplate = "<number><number_suffix> <sponsor> Special KC International <prefix> <height> Jumping <suffix> <part>",
            discipline = DISCIPLINE_JUMPING,
            ruleBook = "KC"
        )
        val KC_STARTERS_CUP= ClassTemplate(
            code = 1080,
            rawName = "KC Starters Cup",
            nameTemplate = "<number><number_suffix> <sponsor> Special KC Starters Cup <prefix> <height> Agility <suffix> <part>",
            discipline = DISCIPLINE_AGILITY,
            ruleBook = "KC"
        )
        val KC_STARTERS_CUP_AGILITY= ClassTemplate(
            code = 1081,
            rawName = "KC Starters Cup Agility",
            nameTemplate = "<number><number_suffix> <sponsor> Special KC Starters Cup <prefix> <height> Agility <suffix> <part>",
            discipline = DISCIPLINE_AGILITY,
            ruleBook = "KC"
        )
        val KC_STARTERS_CUP_JUMPING= ClassTemplate(
            code = 1082,
            rawName = "KC Starters Cup Jumping",
            nameTemplate = "<number><number_suffix> <sponsor> Special KC Starters Cup <prefix> <height> Jumping <suffix> <part>",
            discipline = DISCIPLINE_JUMPING,
            ruleBook = "KC"
        )
        val KC_NOVICE_CUP= ClassTemplate(
            code = 1090,
            rawName = "KC Novice Cup",
            nameTemplate = "<number><number_suffix> <sponsor> Special KC Novice Cup <prefix> <height> Agility <suffix> <part>",
            discipline = DISCIPLINE_AGILITY,
            ruleBook = "KC"
        )
        val KC_NOVICE_CUP_AGILITY= ClassTemplate(
            code = 1091,
            rawName = "KC Novice Cup Agility",
            nameTemplate = "<number><number_suffix> <sponsor> Special KC Novice Cup <prefix> <height> Agility <suffix> <part>",
            discipline = DISCIPLINE_AGILITY,
            ruleBook = "KC"
        )
        val KC_NOVICE_CUP_JUMPING= ClassTemplate(
            code = 1092,
            rawName = "KC Novice Cup Jumping",
            nameTemplate = "<number><number_suffix> <sponsor> Special KC Novice Cup <prefix> <height> Jumping <suffix> <part>",
            discipline = DISCIPLINE_JUMPING,
            ruleBook = "KC"
        )
        
        
        
        val KC_CHALLENGE = ClassTemplate(
            type = CLASS_TYPE_SPECIAL_GROUP,
            scoringMethod = SCORING_METHOD_FAULTS_AS_TIME,
            code = 1320,
            rawName = "Challenge",
            nameTemplate = "<number><number_suffix> <sponsor> <prefix> <height> Special Challenge <suffix> <part>",
            clearRoundRosettes = false,
            combineHeights = true,
            ruleBook = "KC"
        )
        val KC_CHALLENGE_JUMPING = ClassTemplate(
            type = CLASS_TYPE_SPECIAL_GROUP_MEMBER,
            scoringMethod = SCORING_METHOD_FAULTS_AS_TIME,
            code = 1321,
            rawName = "Challenge Jumping",
            nameTemplate = "<number><number_suffix> <sponsor> <prefix> <height> Special Challenge Jumping <suffix> <part>",
            discipline = DISCIPLINE_JUMPING,
            strictRunningOrder = true,
            parent = KC_CHALLENGE,
            column = 1,
            clearRoundRosettes = false,
            ruleBook = "KC",
            entryRule = ENTRY_RULE_CLOSED
        )
        val KC_CHALLENGE_AGILITY = ClassTemplate(
            type = CLASS_TYPE_SPECIAL_GROUP_MEMBER,
            scoringMethod = SCORING_METHOD_FAULTS_AS_TIME,
            code = 1322,
            rawName = "Challenge Agility",
            nameTemplate = "<number><number_suffix> <sponsor> <prefix> <height> Special Challenge Agility <suffix> <part>",
            discipline = DISCIPLINE_AGILITY,
            strictRunningOrder = true,
            parent = KC_CHALLENGE,
            previous = KC_CHALLENGE_JUMPING,
            column = 2,
            clearRoundRosettes = false,
            ruleBook = "KC",
            entryRule = ENTRY_RULE_CLOSED,
            runningOrderSort = "REPLACE(jumpHeightCode, 'KC650L', 'KC500') DESC, hasRun, place desc",
            feedWithdrawn = true
        )
        
        
        val KC_GRAMPION = ClassTemplate(
            type = CLASS_TYPE_SPECIAL_GROUP,
            code = 1330,
            rawName = "Grampion",
            nameTemplate = "<number><number_suffix> <sponsor> Special <prefix> <height> Grampion <suffix> <grade> <part>",
            clearRoundRosettes = false,
            ruleBook = "KC",
            rules = "D1",
            ageLower = 50,
            ageUpper = 999

        )
        val KC_GRAMPION_HEAT = ClassTemplate(
            type = CLASS_TYPE_SPECIAL_GROUP_MEMBER,
            code = 1331,
            rawName = "Grampion Heats",
            nameTemplate = "<number><number_suffix> <sponsor> Special <prefix> <height> Grampion Heats <suffix> <grade> <part>",
            parent = KC_GRAMPION,
            scoringMethod = SCORING_METHOD_KC_POINTS_DESC,
            ruleBook = "KC",
            entryRule = ENTRY_RULE_CLOSED,
            clearRoundRosettes = false,
            awardsCopies = 0
        )
        val KC_GRAMPION_AGILITY = ClassTemplate(
            type = CLASS_TYPE_SPECIAL_GROUP_MEMBER,
            code = 1332,
            rawName = "Grampion Agility",
            nameTemplate = "<number><number_suffix> <sponsor> Special <prefix> <height> Grampion Agility <suffix> <grade> <part>",
            discipline = DISCIPLINE_AGILITY,
            parent = KC_GRAMPION_HEAT,
            column = 2,
            clearRoundRosettes = false,
            ruleBook = "KC",
            entryRule = ENTRY_RULE_CLOSED,
            getParentPoints = { entry ->
                if (entry.isEliminated) 0 else maxOf(21 - entry.place, 0)
            }
        )
        val KC_GRAMPION_JUMPING = ClassTemplate(
            type = CLASS_TYPE_SPECIAL_GROUP_MEMBER,
            code = 1333,
            rawName = "Grampion Jumping",
            nameTemplate = "<number><number_suffix> <sponsor> Special <prefix> <height> Grampion Jumping <suffix> <grade> <part>",
            discipline = DISCIPLINE_JUMPING,
            parent = KC_GRAMPION_HEAT,
            column = 1,
            clearRoundRosettes = false,
            ruleBook = "KC",
            entryRule = ENTRY_RULE_CLOSED,
            getParentPoints = { entry ->
                if (entry.isEliminated) 0 else maxOf(21 - entry.place, 0)
            }
        )
        val KC_GRAMPION_FINAL = ClassTemplate(
            type = CLASS_TYPE_SPECIAL_GROUP_MEMBER,
            code = 1334,
            rawName = "Grampion Final",
            nameTemplate = "<number><number_suffix> <sponsor> Special <prefix> <height> Grampion Final <suffix> <grade> <part>",
            discipline = DISCIPLINE_AGILITY,
            parent = KC_GRAMPION,
            previous = KC_GRAMPION_HEAT,
            clearRoundRosettes = false,
            column = -1,
            ruleBook = "KC",
            cutPercentOf = "entered",
            runningOrderSort = "hasRun, FIND_IN_SET(jumpHeightCode, agilityClass.heightRunningOrder), place desc",
            cut_max = 20,
            entryRule = ENTRY_RULE_CLOSED
        )
        val KC_CHAMPIONSHIP = ClassTemplate(
            type = CLASS_TYPE_SPECIAL_GROUP,
            code = 1100,
            rawName = "Championship",
            nameTemplate = "<number><number_suffix> <sponsor> <prefix> <height> Championship <suffix> <part>",
            clearRoundRosettes = false,
            ruleBook = "KC"
        )
        val KC_CHAMPIONSHIP_HEAT = ClassTemplate(
            type = CLASS_TYPE_SPECIAL_GROUP_MEMBER,
            code = 1101,
            rawName = "Championship Heats",
            nameTemplate = "<number><number_suffix> <sponsor> <prefix> <height> Championship Heats<suffix> <part>",
            parent = KC_CHAMPIONSHIP,
            scoringMethod = SCORING_METHOD_KC_CHAMP,
            ruleBook = "KC",
            entryRule = ENTRY_RULE_CLOSED,
            clearRoundRosettes = false,
            awardsCopies = 0
        )
        val KC_CHAMPIONSHIP_AGILITY = ClassTemplate(
            type = CLASS_TYPE_SPECIAL_GROUP_MEMBER,
            code = 1102,
            rawName = "Championship Agility",
            nameTemplate = "<number><number_suffix> <sponsor> <prefix> <height> Championship Agility <suffix> <part>",
            discipline = DISCIPLINE_AGILITY,
            strictRunningOrder = true,
            parent = KC_CHAMPIONSHIP_HEAT,
            column = 2,
            clearRoundRosettes = false,
            ruleBook = "KC",
            entryRule = ENTRY_RULE_CLOSED,
            getParentPoints = { entry ->
                if (entry.isEliminated) 1000 else entry.place
            }
        )
        val KC_CHAMPIONSHIP_JUMPING = ClassTemplate(
            type = CLASS_TYPE_SPECIAL_GROUP_MEMBER,
            code = 1103,
            rawName = "Championship Jumping",
            nameTemplate = "<number><number_suffix> <sponsor> <prefix> <height> Championship Jumping <suffix> <part>",
            discipline = DISCIPLINE_JUMPING,
            strictRunningOrder = true,
            parent = KC_CHAMPIONSHIP_HEAT,
            column = 1,
            clearRoundRosettes = false,
            ruleBook = "KC",
            entryRule = ENTRY_RULE_CLOSED,
            getParentPoints = { entry ->
                if (entry.isEliminated) 1000 else entry.place
            }
        )
        val KC_CHAMPIONSHIP_FINAL = ClassTemplate(
            type = CLASS_TYPE_SPECIAL_GROUP_MEMBER,
            code = 1104,
            rawName = "Championship Final",
            nameTemplate = "<number><number_suffix> <sponsor> <prefix> <height> Championship Final <suffix> <part>",
            discipline = DISCIPLINE_AGILITY,
            strictRunningOrder = true,
            parent = KC_CHAMPIONSHIP,
            previous = KC_CHAMPIONSHIP_HEAT,
            clearRoundRosettes = false,
            column = -1,
            ruleBook = "KC",
            cutPercentOf = "entered",
            runningOrderSort = "hasRun, place desc",
            cut_percent = 50,
            cut_max = 20,
            entryRule = ENTRY_RULE_CLOSED
        )
        val KC_INTERNATIONAL_YOUNG_HANDLER_U12= ClassTemplate(
            code = 1200,
            rawName = "Int Young Handler U12",
            nameTemplate = "<number><number_suffix> KC International Young Handler (Under 12) <part>",
            nameTemplateShort = "<number><number_suffix> KC Int. Young Handler U12 <part>",
            discipline = DISCIPLINE_AGILITY,
            ruleBook = "KC"
        )
        val KC_INTERNATIONAL_YOUNG_HANDLER_AGILITY_U12= ClassTemplate(
            code = 1201,
            rawName = "Int Young Handler U12 Agility",
            nameTemplate = "<number><number_suffix> KC International Young Handler (Under 12) - Agility <part>",
            nameTemplateShort = "<number><number_suffix> KC Int. Young Handler U12 Agility <part>",
            discipline = DISCIPLINE_AGILITY,
            ruleBook = "KC"
        )
        val KC_INTERNATIONAL_YOUNG_HANDLER_JUMPING_U12= ClassTemplate(
            code = 1202,
            rawName = "Int Young Handler U12 Jumping",
            nameTemplate = "<number><number_suffix> KC International Young Handler (Under 12) - Jumping <part>",
            nameTemplateShort = "<number><number_suffix> KC Int. Young Handler U12 Jumping <part>",
            discipline = DISCIPLINE_JUMPING,
            ruleBook = "KC"
        )
        val KC_INTERNATIONAL_YOUNG_HANDLER_U18= ClassTemplate(
            code = 1210,
            rawName = "Int Young Handler 12+",
            nameTemplate = "<number><number_suffix> KC International Young Handler (12-17) <part>",
            nameTemplateShort = "<number><number_suffix> KC Int. Young Handler 12+ <part>",
            discipline = DISCIPLINE_AGILITY,
            ruleBook = "KC"
        )
        val KC_INTERNATIONAL_YOUNG_HANDLER_AGILITY_U18= ClassTemplate(
            code = 1211,
            rawName = "Int Young Handler 12+ Agility",
            nameTemplate = "<number><number_suffix> KC International Young Handler (12-17) - Agility <part>",
            nameTemplateShort = "<number><number_suffix> KC Int. Young Handler 12+ Agility <part>",
            discipline = DISCIPLINE_AGILITY,
            ruleBook = "KC"
        )
        val KC_INTERNATIONAL_YOUNG_HANDLER_JUMPING_U18= ClassTemplate(
            code = 1212,
            rawName = "Int Young Handler 12+ Jumping",
            nameTemplate = "<number><number_suffix> KC International Young Handler (12-17) - Jumping <part>",
            nameTemplateShort = "<number><number_suffix> KC Int. Young Handler 12+ Jumping <part>",
            discipline = DISCIPLINE_JUMPING,
            ruleBook = "KC"
        )
        val KC_OLYMPIA_ABC_AGILITY = ClassTemplate(
            code = 1220,
            rawName = "Olympia ABC",
            nameTemplate = "<number><number_suffix> KC Olympia ABC Agility Stakes <grade> <part>",
            discipline = DISCIPLINE_AGILITY,
            qualifierPlaces = 10,
            ruleBook = "KC"
        )
        val KC_OLYMPIA_STAKES = ClassTemplate(
            code = 1230,
            rawName = "Olympia Stakes",
            nameTemplate = "<number><number_suffix> KC Olympia <height> Agility Stakes <grade> <part>",
            discipline = DISCIPLINE_AGILITY,
            qualifierPlaces = 10,
            ruleBook = "KC"
        )
        val KC_OLYMPIA_NOVICE_STAKES = ClassTemplate(
            code = 1240,
            rawName = "Olympia Novice Stakes",
            nameTemplate = "<number><number_suffix> KC Olympia Novice Agility Stakes <grade> <part>",
            discipline = DISCIPLINE_AGILITY,
            qualifierPlaces = 10,
            ruleBook = "KC"
        )
        val KC_CRUFTS_NOVICE_ABC_STAKES = ClassTemplate(
            code = 1250,
            rawName = "Crufts Novice ABC",
            nameTemplate = "<number><number_suffix> Crufts <height> Novice ABC Stakes <grade> <part>",
            discipline = DISCIPLINE_AGILITY,
            qualifierPlaces = 10,
            ruleBook = "KC"
        )
        val KC_CRUFTS_ABC_STAKES = ClassTemplate(
            code = 1260,
            rawName = "Crufts ABC",
            nameTemplate = "<number><number_suffix> Crufts <height> ABC Stakes <grade> <part>",
            discipline = DISCIPLINE_AGILITY,
            qualifierPlaces = 10,
            ruleBook = "KC"
        )
        val KC_CRUFTS_TEAM = ClassTemplate(
            code = 1270,
            rawName = "Crufts Team",
            nameTemplate = "<number><number_suffix> <height> Crufts Team Agility <part>",
            discipline = DISCIPLINE_AGILITY,
            teamSize = 4,
            teamReserves = 2,
            runUnits = 4,
            summarizeEliminations = false,
            strictRunningOrder = true,
            ruleBook = "KC"
        )
        val KC_CRUFTS_SINGLES = ClassTemplate(
            code = 1280,
            rawName = "Crufts Singles",
            nameTemplate = "<number><number_suffix> <height> Crufts Singles Agility <grade> <part>",
            discipline = DISCIPLINE_AGILITY,
            qualifierPlaces = 10,
            ruleBook = "KC"
        )
        val KC_JUNIOR_JUMPING_U9 = ClassTemplate(
            code = 1300,
            rawName = "Junior Jumping U9",
            nameTemplate = "<number><number_suffix> <sponsor> Junior Jumping (Under 9) <suffix> <lho> <part>",
            nameTemplateShort = "<number><number_suffix> Junior Jumping U9 <suffix> <lho> <part>",
            discipline = DISCIPLINE_JUMPING,
            ruleBook = "KC",
            verifyHandler = true,
            ageUpper = 8
        )
        val KC_JUNIOR_JUMPING_O9 = ClassTemplate(
            code = 1301,
            rawName = "Junior Jumping 9+",
            nameTemplate = "<number><number_suffix> <sponsor> Junior Jumping (9-16) <suffix> <lho> <part>",
            nameTemplateShort = "<number><number_suffix> Junior Jumping 9+ <suffix> <lho> <part>",
            discipline = DISCIPLINE_JUMPING,
            ruleBook = "KC",
            verifyHandler = true,
            ageLower = 9,
            ageUpper = 16
        )
        val KC_JUNIOR_JUMPING_U12= ClassTemplate(
            code = 1302,
            rawName = "Junior Jumping U12",
            nameTemplate = "<number><number_suffix> <sponsor> Junior Jumping (Under 12) <suffix> <lho> <part>",
            nameTemplateShort = "<number><number_suffix> Junior Jumping U12 <suffix> <lho> <part>",
            discipline = DISCIPLINE_JUMPING,
            ruleBook = "KC",
            verifyHandler = true,
            ageUpper = 11
        )
        val KC_JUNIOR_JUMPING_O12= ClassTemplate(
            code = 1303,
            rawName = "Junior Jumping 12-18",
            nameTemplate = "<number><number_suffix> <sponsor> Junior Jumping (12-18) <suffix> <lho> <part>",
            nameTemplateShort = "<number><number_suffix> Junior Jumping 12-18 <suffix> <lho> <part>",
            discipline = DISCIPLINE_JUMPING,
            ruleBook = "KC",
            verifyHandler = true,
            ageLower = 12,
            ageUpper = 18
        )
        val KC_JUNIOR_JUMPING_U18 = ClassTemplate(
            code = 1304,
            rawName = "Junior Jumping U18",
            nameTemplate = "<number><number_suffix> <sponsor> Junior Jumping (Under 18) <suffix> <lho> <part>",
            nameTemplateShort = "<number><number_suffix> Junior Jumping U18 <suffix> <lho> <part>",
            discipline = DISCIPLINE_JUMPING,
            ruleBook = "KC",
            verifyHandler = true,
            ageUpper = 17
        )
        val KC_JUNIOR_JUMPING = ClassTemplate(
            code = 1305,
            rawName = "Junior Jumping",
            nameTemplate = "<number><number_suffix> <sponsor> Junior Jumping <suffix> <lho> <part>",
            nameTemplateShort = "<number><number_suffix> Junior Jumping <suffix> <lho> <part>",
            discipline = DISCIPLINE_JUMPING,
            ruleBook = "KC",
            verifyHandler = true
        )
        val KC_JUNIOR_STEEPLECHASE= ClassTemplate(
            code = 1306,
            rawName = "Junior Steeplechase",
            nameTemplate = "<number><number_suffix> <sponsor> Junior Steeplechase <suffix> <lho> <part>",
            nameTemplateShort = "<number><number_suffix> Junior Steeplechase <suffix> <lho> <part>",
            discipline = DISCIPLINE_STEEPLECHASE,
            ruleBook = "KC",
            verifyHandler = true
        )
        val KC_JUNIOR_HELTERSKELTER= ClassTemplate(
            code = 1307,
            rawName = "Junior Helter Skelter",
            nameTemplate = "<number><number_suffix> <sponsor> Junior Helter Skelter <suffix> <lho> <part>",
            nameTemplateShort = "<number><number_suffix> Junior Helter Skelter <suffix> <lho> <part>",
            discipline = DISCIPLINE_STEEPLECHASE,
            ruleBook = "KC",
            verifyHandler = true
        )
        val KC_JUNIOR_AGILITY_U9= ClassTemplate(
            code = 1310,
            rawName = "Junior Agility U9",
            nameTemplate = "<number><number_suffix> <sponsor> Junior Agility (Under 9) <suffix> <lho> <part>",
            nameTemplateShort = "<number><number_suffix> Junior Agility U9 <suffix> <lho> <part>",
            discipline = DISCIPLINE_AGILITY,
            ruleBook = "KC",
            verifyHandler = true,
            ageUpper = 8
        )
        val KC_JUNIOR_AGILITY_O9= ClassTemplate(
            code = 1311,
            rawName = "Junior Agility 9+",
            nameTemplate = "<number><number_suffix> <sponsor> Junior Agility (9-16) <suffix> <lho> <part>",
            nameTemplateShort = "<number><number_suffix> Junior Agility 9+ <suffix> <lho> <part>",
            discipline = DISCIPLINE_AGILITY,
            ruleBook = "KC",
            verifyHandler = true,
            ageLower = 9,
            ageUpper = 16
        )
        val KC_JUNIOR_AGILITY_U12 = ClassTemplate(
            code = 1312,
            rawName = "Junior Agility U12",
            nameTemplate = "<number><number_suffix> <sponsor> Junior Agility (Under 12) <suffix> <lho> <part>",
            nameTemplateShort = "<number><number_suffix> Junior Agility U12 <suffix> <lho> <part>",
            discipline = DISCIPLINE_AGILITY,
            ruleBook = "KC",
            verifyHandler = true,
            ageUpper = 11
        )
        val KC_JUNIOR_AGILITY_O12 = ClassTemplate(
            code = 1313,
            rawName = "Junior Agility 12-18",
            nameTemplate = "<number><number_suffix> <sponsor> Junior Agility (12-18) <suffix> <lho> <part>",
            nameTemplateShort = "<number><number_suffix> Junior Agility 12-18 <suffix> <lho> <part>",
            discipline = DISCIPLINE_AGILITY,
            ruleBook = "KC",
            verifyHandler = true,
            ageLower = 12,
            ageUpper = 18
        )
        val KC_JUNIOR_AGILITY_U18 = ClassTemplate(
            code = 1314,
            rawName = "Junior Agility U18",
            nameTemplate = "<number><number_suffix> <sponsor> Junior Agility (Under 18) <suffix> <lho> <part>",
            nameTemplateShort = "<number><number_suffix> Junior Agility U18 <suffix> <lho> <part>",
            discipline = DISCIPLINE_AGILITY,
            ruleBook = "KC",
            verifyHandler = true,
            ageUpper = 17
        )
        val KC_JUNIOR_AGILITY = ClassTemplate(
            code = 1315,
            rawName = "Junior Agility",
            nameTemplate = "<number><number_suffix> <sponsor> Junior Agility <suffix> <lho> <part>",
            nameTemplateShort = "<number><number_suffix> Junior Agility <suffix> <lho> <part>",
            discipline = DISCIPLINE_AGILITY,
            ruleBook = "KC",
            verifyHandler = true
        )
        val KC_POTD_AGILITY= ClassTemplate(
            code = 1411,
            rawName = "POTD Agility",
            nameTemplate = "<number><number_suffix> <sponsor> Special Pay On The Day <prefix> <height> Agility <suffix> <grade> <lho> <part>",
            discipline = DISCIPLINE_AGILITY,
            ruleBook = "KC",
            entryRule = ENTRY_RULE_POTD
        )
        val KC_POTD_JUMPING = ClassTemplate(
            code = 1412,
            rawName = "POTD Jumping",
            nameTemplate = "<number><number_suffix> <sponsor> Special Pay On The Day <prefix> <height> Jumping <suffix> <grade> <lho> <part>",
            discipline = DISCIPLINE_JUMPING,
            ruleBook = "KC",
            entryRule = ENTRY_RULE_POTD
        )
        val KC_POTD_STEEPLECHASE= ClassTemplate(
            code = 1413,
            rawName = "POTD Steeplechase",
            nameTemplate = "<number><number_suffix> <sponsor> Special Pay On The Day <prefix> <height> Steeplechase <suffix> <grade> <lho> <part>",
            discipline = DISCIPLINE_STEEPLECHASE,
            ruleBook = "KC",
            entryRule = ENTRY_RULE_POTD
        )
        val KC_POTD_POWER_AND_SPEED= ClassTemplate(
            code = 1414,
            rawName = "POTD Power & Speed",
            nameTemplate = "<number><number_suffix> <sponsor> Special Pay On The Day <prefix> <height> Power & Speed <suffix> <grade> <lho> <part>",
            discipline = DISCIPLINE_POWER_AND_SPEED,
            ruleBook = "KC",
            entryRule = ENTRY_RULE_POTD
        )
        val KC_POTD_TIME_FAULT_OUT= ClassTemplate(
            code = 1415,
            rawName = "POTD Time, Fault and Out",
            nameTemplate = "<number><number_suffix> <sponsor> Special Pay On The Day <prefix> <height> Time, Fault and Out <suffix> <grade> <lho> <part>",
            discipline = DISCIPLINE_TIME_FAULT_AND_OUT,
            ruleBook = "KC",
            entryRule = ENTRY_RULE_POTD
        )
        val KC_POTD_PRACTICE= ClassTemplate(
            code = 1499,
            rawName = "Practice",
            nameTemplate = "Pay On The Day Practice",
            discipline = DISCIPLINE_TIME_FAULT_AND_OUT,
            ruleBook = "KC",
            entryRule = ENTRY_RULE_POTD
        )
        val AGILITY = ClassTemplate(
            code = 1,
            nameTemplate = "<grade> Agility",
            programme = PROGRAMME_PERFORMANCE,
            discipline = DISCIPLINE_AGILITY
        )
        val JUMPING = ClassTemplate(
            code = 2,
            nameTemplate = "<grade> Jumping",
            programme = PROGRAMME_PERFORMANCE,
            discipline = DISCIPLINE_JUMPING
        )
        val STEEPLECHASE = ClassTemplate(
            code = 3,
            nameTemplate = "<grade> Steeplechase",
            programme = PROGRAMME_STEEPLECHASE,
            discipline = DISCIPLINE_STEEPLECHASE,
            entryRule = ENTRY_RULE_GRADE2
        )
        val GAMBLERS = ClassTemplate(
            code = 10,
            nameTemplate = "<grade> Gamblers",
            programme = PROGRAMME_PERFORMANCE,
            discipline = DISCIPLINE_GAMBLERS,
            scoringMethod = SCORING_METHOD_GAMES,
            courseLengthNeeded = false
        )
        val SNOOKER = ClassTemplate(
            code = 11,
            nameTemplate = "<grade> Snooker",
            programme = PROGRAMME_PERFORMANCE,
            discipline = DISCIPLINE_SNOOKER,

            noTimeFaults = true,
            scoringMethod = SCORING_METHOD_GAMES,
            courseLengthNeeded = false
        )
        val POWER_AND_SPEED = ClassTemplate(
            code = 12,
            nameTemplate = "<grade> Power & Speed",
            programme = PROGRAMME_PERFORMANCE,
            discipline = DISCIPLINE_POWER_AND_SPEED
        )
        val TIME_FAULT_AND_OUT = ClassTemplate(
            code = 13,
            nameTemplate = "<grade> Time Fault & Out",
            programme = PROGRAMME_PERFORMANCE,
            discipline = DISCIPLINE_TIME_FAULT_AND_OUT,

            noTimeFaults = true,
            scoringMethod = SCORING_METHOD_GAMES
        )
        val SNAKES_AND_LADDERS = ClassTemplate(
            code = 14,
            nameTemplate = "<grade> Snakes & Ladders",
            programme = PROGRAMME_PERFORMANCE,
            discipline = DISCIPLINE_SNAKES_AND_LADDERS,

            noTimeFaults = true,
            scoringMethod = SCORING_METHOD_GAMES,
            courseLengthNeeded = false
        )
        val JUNIOR_AGILITY = ClassTemplate(
            code = 81,
            nameTemplate = "Junior Agility",
            discipline = DISCIPLINE_AGILITY,
            entryRule = ENTRY_RULE_ANY_GRADE,
            courseTimeCode = "UKA90"
        )
        val JUNIOR_JUMPING = ClassTemplate(
            code = 82,
            nameTemplate = "Junior Jumping",
            discipline = DISCIPLINE_JUMPING,
            entryRule = ENTRY_RULE_ANY_GRADE,
            courseTimeCode = "UKA90"
        )
        val NURSERY_AGILITY = ClassTemplate(
            code = 90,
            nameTemplate = "Nursery Agility",
            discipline = DISCIPLINE_AGILITY,
            entryRule = ENTRY_RULE_ANY_GRADE,
            courseTimeCode = "UKA91"
        )
        val CASUAL_AGILITY = ClassTemplate(
            code = 91,
            nameTemplate = "Casual Agility",
            discipline = DISCIPLINE_AGILITY,
            entryRule = ENTRY_RULE_ANY_GRADE,
            courseTimeCode = "UKA92"
        )
        val CASUAL_JUMPING = ClassTemplate(
            code = 92,
            nameTemplate = "Casual Jumping",
            discipline = DISCIPLINE_JUMPING,
            entryRule = ENTRY_RULE_ANY_GRADE,
            courseTimeCode = "UKA92"
        )
        val CASUAL_STEEPLECHASE = ClassTemplate(
            code = 93,
            nameTemplate = "Casual Steeplechase",
            discipline = DISCIPLINE_STEEPLECHASE,
            entryRule = ENTRY_RULE_ANY_GRADE,
            courseTimeCode = "UKA92"
        )
        val MASTERS = ClassTemplate(
            type = CLASS_TYPE_SPECIAL_GROUP,
            code = 100,
            sponsor = "Doggy Jumps",
            logo = "doggy-jumps.png",
            website = "www.doggyjumps.com",
            nameTemplate = "Masters",
            scoringMethod = SCORING_METHOD_MASTERS,
            runUnits = 0,
            entryFee = 1200,
            lateEntryFee = 1400,
            ukaFinalsPlaces = 2
            
        )
        val MASTERS_JUMPING = ClassTemplate(
            type = CLASS_TYPE_SPECIAL_GROUP_MEMBER,
            code = 101,
            sponsor = "Doggy Jumps",
            logo = "doggy-jumps.png",
            website = "www.doggyjumps.com",
            nameTemplate = "Masters Jumping",
            discipline = DISCIPLINE_JUMPING,
            strictRunningOrder = true,
            parent = MASTERS,
            previous = MASTERS,
            courseTimeCode = "UKA04",
            eliminationTime = 60000,
            flagTies = false,
            scoringMethod = SCORING_METHOD_MASTERS,
            entryRule = ENTRY_RULE_CLOSED,
            ukaFinalsPlaces = 1,
            ukaFinalsQualifierCode = MASTERS.code
        )
        val MASTERS_AGILITY = ClassTemplate(
            type = CLASS_TYPE_SPECIAL_GROUP_MEMBER,
            code = 102,
            sponsor = "Doggy Jumps",
            logo = "doggy-jumps.png",
            website = "www.doggyjumps.com",
            nameTemplate = "Masters Agility",
            discipline = DISCIPLINE_AGILITY,
            strictRunningOrder = true,
            parent = MASTERS,
            previous = MASTERS_JUMPING,
            courseTimeCode = "UKA04",
            runningOrderSort = "hasRun, place desc",
            column = 2,
            eliminationTime = 60000,
            flagTies = false,
            scoringMethod = SCORING_METHOD_MASTERS,
            entryRule = ENTRY_RULE_CLOSED,
            ukaFinalsPlaces = 1,
            ukaFinalsQualifierCode = MASTERS.code
        )
        val JUNIOR_MASTERS = ClassTemplate(
            type = CLASS_TYPE_SPECIAL_GROUP,
            code = 144,
            sponsor = "Agility for Juniors",
            logo = "Agility for Junior.jpg",
            website = "www.agilityforjuniors.com",
            nameTemplate = "Junior Masters",
            scoringMethod = SCORING_METHOD_MASTERS,
            runUnits = 2,
            entryRule = ENTRY_RULE_ANY_GRADE
        )
        val JUNIOR_MASTERS_JUMPING = ClassTemplate(
            type = CLASS_TYPE_SPECIAL_GROUP_MEMBER,
            code = 145,
            sponsor = "Agility for Juniors",
            logo = "Agility for Junior.jpg",
            website = "www.agilityforjuniors.com",
            nameTemplate = "Junior Masters Jumping",
            discipline = DISCIPLINE_JUMPING,
            strictRunningOrder = true,
            parent = JUNIOR_MASTERS,
            previous = JUNIOR_MASTERS,
            courseTimeCode = "UKA03",
            eliminationTime = 60000,
            flagTies = false,
            scoringMethod = SCORING_METHOD_MASTERS,
            entryRule = ENTRY_RULE_CLOSED
        )
        val JUNIOR_MASTERS_AGILITY = ClassTemplate(
            type = CLASS_TYPE_SPECIAL_GROUP_MEMBER,
            code = 146,
            sponsor = "Agility for Juniors",
            logo = "Agility for Junior.jpg",
            website = "www.agilityforjuniors.com",
            nameTemplate = "Junior Masters Agility",
            discipline = DISCIPLINE_AGILITY,
            strictRunningOrder = true,
            parent = JUNIOR_MASTERS,
            previous = JUNIOR_MASTERS_JUMPING,
            courseTimeCode = "UKA03",
            runningOrderSort = "hasRun, place desc",
            column = 2,
            eliminationTime = 60000,
            flagTies = false,
            scoringMethod = SCORING_METHOD_MASTERS,
            entryRule = ENTRY_RULE_CLOSED
        )
        val CHALLENGE_FINAL = ClassTemplate(
            type = CLASS_TYPE_SPECIAL_GROUP,
            code = 500,
            sponsor = "Dogeria",
//        logo = "Canine Massage Guild.png",
//        website = "www.k9-massageguild.co.uk",
            nameTemplate = "Challenge Final",
            groupName = "Challenge",
            entryRule = ENTRY_RULE_INVITE
        )
        val CHALLENGE = ClassTemplate(
            type = CLASS_TYPE_HARVESTED,
            code = 110,
            sponsor = "Dogeria",
//        logo = "Canine Massage Guild.png",
//        website = "www.k9-massageguild.co.uk",
            nameTemplate = "Challenge",
            entryRule = ENTRY_RULE_CLOSED,
            courseTimeCode = "UKA01",
            harvestedResults = 2,
            ukaFinalsPlaces = 2,
            ukaFinalsQualifierCode = CHALLENGE_FINAL.code
        )
        val CHALLENGE_JUMPING = ClassTemplate(
            type = CLASS_TYPE_SPECIAL_GROUP_MEMBER,
            code = 501,
            sponsor = "Dogeria",
//        logo = "Canine Massage Guild.png",
//        website = "www.k9-massageguild.co.uk",
            nameTemplate = "Challenge Jumping",
            discipline = DISCIPLINE_JUMPING,
            entryRule = ENTRY_RULE_CLOSED,
            parent = CHALLENGE_FINAL,
            previous = CHALLENGE_FINAL,
            flagTies = false,
            courseTimeCode = "UKA01"
        )
        val CHALLENGE_AGILITY = ClassTemplate(
            type = CLASS_TYPE_SPECIAL_GROUP_MEMBER,
            code = 502,
            sponsor = "Dogeria",
//        logo = "Canine Massage Guild.png",
//        website = "www.k9-massageguild.co.uk",
            nameTemplate = "Challenge Agility",
            discipline = DISCIPLINE_AGILITY,
            entryRule = ENTRY_RULE_CLOSED,
            parent = CHALLENGE_FINAL,
            previous = CHALLENGE_JUMPING,
            runningOrderSort = "hasRun, place desc",
            courseTimeCode = "UKA01",
            flagTies = false,
            column = 2
        )
        val GRAND_PRIX_SEMI_FINAL = ClassTemplate(
            type = CLASS_TYPE_SPECIAL_SERIES,
            code = 510,
            sponsor = "CSJ",
            logo = "CSJ.png",
            nameTemplate = "Grand Prix Semi-Final",
            groupName = "Grand Prix",
            courseTimeCode = "UKA02",
            discipline = DISCIPLINE_AGILITY,
            entryRule = ENTRY_RULE_INVITE
        )
        val GRAND_PRIX_FINAL = ClassTemplate(
            type = CLASS_TYPE_SPECIAL_SERIES_MEMBER,
            code = 511,
            sponsor = "CSJ",
            logo = "CSJ.png",
            nameTemplate = "Grand Prix Final",
            courseTimeCode = "UKA02",
            discipline = DISCIPLINE_AGILITY,
            entryRule = ENTRY_RULE_CLOSED,
            previous = GRAND_PRIX_SEMI_FINAL,
            runningOrderSort = "hasRun, place desc",
            cut_max = 10
        )
        val GRAND_PRIX = ClassTemplate(
            type = CLASS_TYPE_HARVESTED,
            code = 120,
            sponsor = "CSJ",
            logo = "CSJ.png",
            nameTemplate = "Grand Prix",
            courseTimeCode = "UKA02",
            entryRule = ENTRY_RULE_CLOSED,
            ukaFinalsPlaces = 2,
            ukaFinalsQualifierCode = GRAND_PRIX_SEMI_FINAL.code
        )
        val BEGINNERS_STEEPLECHASE_SEMI_FINAL = ClassTemplate(
            type = CLASS_TYPE_SPECIAL_SERIES,
            code = 520,
            sponsor = "SWAG",
            logo = "swag.jpg",
            nameTemplate = "Beginners Steeplechase Semi",
            groupName = "Steeplechase",
            discipline = DISCIPLINE_STEEPLECHASE,
            strictRunningOrder = true,
            entryRule = ENTRY_RULE_INVITE,
            courseTimeCode = "UKA01"
        )
        val BEGINNERS_STEEPLECHASE_FINAL = ClassTemplate(
            type = CLASS_TYPE_SPECIAL_SERIES_MEMBER,
            code = 521,
            sponsor = "SWAG",
            logo = "swag.jpg",
            nameTemplate = "Beginners Steeplechase Final",
            previous = BEGINNERS_STEEPLECHASE_SEMI_FINAL,
            discipline = DISCIPLINE_STEEPLECHASE,
            strictRunningOrder = true,
            entryRule = ENTRY_RULE_CLOSED,
            runningOrderSort = "hasRun, place desc",
            cut_max = 10,
            courseTimeCode = "UKA01"
        )
        val BEGINNERS_STEEPLECHASE_HEAT = ClassTemplate(
            type = CLASS_TYPE_HARVESTED,
            discipline = STEEPLECHASE.discipline,
            code = 130,
            sponsor = "SWAG",
            logo = "swag.jpg",
            nameTemplate = "Beginners Steeplechase Heat",
            entryRule = ENTRY_RULE_CLOSED,
            ukaFinalsPlaces = 2,
            ukaFinalsQualifierCode = BEGINNERS_STEEPLECHASE_SEMI_FINAL.code
        )
        val JUNIOR_OPEN = ClassTemplate(
            defunct = true,
            type = CLASS_TYPE_SPECIAL_GROUP,
            code = 140,
            sponsor = "Agility for Juniors",
            logo = "Agility for Junior.jpg",
            website = "www.agilityforjuniors.com",
            nameTemplate = "Junior Open",
            combineHeights = true,
            scoringMethod = SCORING_METHOD_JUNIOR,
            runUnits = 2,
            entryRule = ENTRY_RULE_ANY_GRADE
        )
        val JUNIOR_OPEN_JUMPING = ClassTemplate(
            defunct = true,
            type = CLASS_TYPE_SPECIAL_GROUP_MEMBER,
            code = 141,
            sponsor = "Agility for Juniors",
            logo = "Agility for Junior.jpg",
            website = "www.agilityforjuniors.com",
            nameTemplate = "Junior Open Jumping",
            discipline = DISCIPLINE_JUMPING,
            parent = JUNIOR_OPEN,
            courseTimeCode = "UKA90X",
            combineHeights = true,
            flagTies = false,
            scoringMethod = SCORING_METHOD_JUNIOR,
            entryRule = ENTRY_RULE_CLOSED
        )
        val JUNIOR_OPEN_AGILITY = ClassTemplate(
            defunct = true,
            type = CLASS_TYPE_SPECIAL_GROUP_MEMBER,
            code = 142,
            sponsor = "Agility for Juniors",
            logo = "Agility for Junior.jpg",
            website = "www.agilityforjuniors.com",
            nameTemplate = "Junior Open Agility",
            discipline = DISCIPLINE_AGILITY,
            parent = JUNIOR_OPEN,
            column = 2,
            courseTimeCode = "UKA90X",
            combineHeights = true,
            flagTies = false,
            scoringMethod = SCORING_METHOD_JUNIOR,
            entryRule = ENTRY_RULE_CLOSED
        )
        val JUNIOR_OPEN_FINAL = ClassTemplate(
            defunct = true,
            type = CLASS_TYPE_SPECIAL_GROUP,
            code = 530,
            sponsor = "Agility for Juniors",
            logo = "Agility for Junior.jpg",
            website = "www.agilityforjuniors.com",
            nameTemplate = "Junior Open Final",
            combineHeights = true,
            scoringMethod = SCORING_METHOD_JUNIOR,
            runUnits = 2,
            entryRule = ENTRY_RULE_INVITE
        )
        val JUNIOR_OPEN_FINAL_JUMPING = ClassTemplate(
            type = CLASS_TYPE_SPECIAL_GROUP_MEMBER,
            code = 531,
            sponsor = "Agility for Juniors",
            logo = "Agility for Junior.jpg",
            website = "www.agilityforjuniors.com",
            nameTemplate = "Junior Open Jumping",
            discipline = DISCIPLINE_JUMPING,
            parent = JUNIOR_OPEN_FINAL,
            previous = JUNIOR_OPEN_FINAL,
            courseTimeCode = "UKA90X",
            combineHeights = true,
            flagTies = false,
            scoringMethod = SCORING_METHOD_JUNIOR,
            entryRule = ENTRY_RULE_CLOSED
        )
        val JUNIOR_OPEN_FINAL_AGILITY = ClassTemplate(
            defunct = true,
            type = CLASS_TYPE_SPECIAL_GROUP_MEMBER,
            code = 532,
            sponsor = "Agility for Juniors",
            logo = "Agility for Junior.jpg",
            website = "www.agilityforjuniors.com",
            nameTemplate = "Junior Open Agility",
            discipline = DISCIPLINE_AGILITY,
            parent = JUNIOR_OPEN_FINAL,
            previous = JUNIOR_OPEN_FINAL_JUMPING,
            column = 2,
            courseTimeCode = "UKA90X",
            runningOrderSort = "hasRun, place desc",
            combineHeights = true,
            flagTies = false,
            scoringMethod = SCORING_METHOD_JUNIOR,
            entryRule = ENTRY_RULE_CLOSED
        )
        val SPLIT_PAIRS = ClassTemplate(
            type = CLASS_TYPE_SPECIAL,
            code = 150,
            sponsor = "DOG StreamZ",
            logo = "streamz.png",
            website = "www.streamz-global.com",
            nameTemplate = "Split Pairs",
            combineHeights = true,
            strictRunningOrder = true,
            teamSize = 2,
            runUnits = 2,
            courseLengthNeeded = false,
            discretionaryCourseTime = true,
            discipline = DISCIPLINE_RELAY,
            scoringMethod = SCORING_METHOD_RELAY,
            entryRule = ENTRY_RULE_ANY_GRADE,
            ukaFinalsPlaces = 2
        )
        val TEAM = ClassTemplate(
            type = CLASS_TYPE_SPECIAL_GROUP,
            code = 160,
            sponsor = "Agility World",
            // logo = "Yellow hound.jpg",
            // website = "www.yellowhound.co.uk",
            nameTemplate = "Team",
            teamSize = 3,
            runUnits = 3,
            scoringMethod = SCORING_METHOD_RELAY,
            combineHeights = true,
            summarizeEliminations = false,
            entryRule = ENTRY_RULE_ANY_GRADE,
            ukaFinalsPlaces = 1
        )
        val TEAM_INDIVIDUAL = ClassTemplate(
            type = CLASS_TYPE_SPECIAL_GROUP_MEMBER,
            code = 161,
            sponsor = "Agility World",
            // logo = "Yellow hound.jpg",
            // website = "www.yellowhound.co.uk",
            nameTemplate = "Team Individual",
            discipline = DISCIPLINE_AGILITY,
            parent = TEAM,
            previous = TEAM,
            scoringMethod = SCORING_METHOD_TEAM_INDIVIDUAL,
            courseTimeCode = "UKA02",
            eliminationTime = 60000,
            flagTies = false,
            maxColumn = 3,
            entryRule = ENTRY_RULE_CLOSED,
            combineHeights = true,
            printIntermediateResults = true,
            strictRunningOrder = true
        )
        val TEAM_RELAY = ClassTemplate(
            type = CLASS_TYPE_SPECIAL_GROUP_MEMBER,
            code = 162,
            sponsor = "Agility World",
            // logo = "Yellow hound.jpg",
            // website = "www.yellowhound.co.uk",
            nameTemplate = "Team Relay",
            parent = TEAM,
            previous = TEAM_INDIVIDUAL,
            scoringMethod = SCORING_METHOD_RELAY,
            discipline = DISCIPLINE_RELAY,
            entryRule = ENTRY_RULE_CLOSED,
            strictRunningOrder = true,
            runningOrderSort = "points DESC",
            courseLengthNeeded = false,
            discretionaryCourseTime = true,
            combineHeights = true,
            flagTies = false,
            batonFaults = 20,
            teamSize = 3,
            column = 4
        )
        val CIRCULAR_KNOCKOUT = ClassTemplate(
            type = CLASS_TYPE_SPECIAL,
            code = 170,
            sponsor = "Norton Rosettes",
            logo = "NortonRosettes.png",
            website = "www.nortonrosettes.co.uk",
            nameTemplate = "Circular Knockout",
            entryRule = ENTRY_RULE_ANY_GRADE,
            ukaFinalsPlaces = 2
        )
        val GAMES_CHALLENGE = ClassTemplate(
            type = CLASS_TYPE_SPECIAL_GROUP,
            code = 180,
            sponsor = "Clear Q's",
            logo = "CQ.png",
            website = "www.ClearQ.co.uk",
            nameTemplate = "Games Challenge",
            scoringMethod = SCORING_METHOD_GAMES,
            runUnits = 0,
            entryFee = 500,
            combineHeights = true,
            lateEntryFee = 500,
            entryRule = ENTRY_RULE_INVITE,
            ukaFinalsPlaces = 20
        )
        val GAMES_CHALLENGE_SNOOKER = ClassTemplate(
            type = CLASS_TYPE_SPECIAL_GROUP_MEMBER,
            code = 181,
            sponsor = "Clear Q's",
            logo = "CQ.png",
            website = "www.ClearQ.co.uk",
            nameTemplate = "Games Challenge Snooker",
            discipline = DISCIPLINE_SNOOKER,
            noTimeFaults = true,
            parent = GAMES_CHALLENGE,
            previous = GAMES_CHALLENGE,
            combineHeights = true,
            scoringMethod = SCORING_METHOD_GAMES,
            strictRunningOrder = true,
            courseLengthNeeded = false,
            flagTies = false,
            courseTimeCode = "UKA03",
            entryRule = ENTRY_RULE_CLOSED
        )
        val GAMES_CHALLENGE_GAMBLERS = ClassTemplate(
            type = CLASS_TYPE_SPECIAL_GROUP_MEMBER,
            code = 182,
            sponsor = "Clear Q's",
            logo = "CQ.png",
            website = "www.ClearQ.co.uk",
            nameTemplate = "Games Challenge Gamblers",
            discipline = DISCIPLINE_GAMBLERS,
            column = 2,
            parent = GAMES_CHALLENGE,
            previous = GAMES_CHALLENGE_SNOOKER,
            scoringMethod = SCORING_METHOD_GAMES,
            runningOrderSort = "hasRun, place desc",
            combineHeights = true,
            strictRunningOrder = true,
            courseLengthNeeded = false,
            flagTies = false,
            courseTimeCode = "UKA03",
            entryRule = ENTRY_RULE_CLOSED
        )
        val FINAL_ROUND_1 = ClassTemplate(
            type = CLASS_TYPE_SPECIAL_SERIES,
            code = 190,
            nameTemplate = "Championship Round 1",
            discipline = DISCIPLINE_AGILITY,
            courseTimeCode = "UKA02",
            entryRule = ENTRY_RULE_ANY_GRADE,
            strictRunningOrder = true,
            runUnits = 0,
            entryFee = 750,
            lateEntryFee = 850
        )
        val FINAL_ROUND_2 = ClassTemplate(
            type = CLASS_TYPE_SPECIAL_SERIES_MEMBER,
            code = 191,
            nameTemplate = "Championship Round 2",
            discipline = DISCIPLINE_AGILITY,
            previous = FINAL_ROUND_1,
            cut_percent = 50,
            cut_min = 25,
            strictRunningOrder = true,
            runningOrderSort = "place DESC",
            courseTimeCode = "UKA02",
            entryRule = ENTRY_RULE_CLOSED
        )
        val FINAL_ROUND_3 = ClassTemplate(
            type = CLASS_TYPE_SPECIAL_SERIES_MEMBER,
            code = 192,
            nameTemplate = "Championship Final",
            discipline = DISCIPLINE_AGILITY,
            previous = FINAL_ROUND_2,
            cut_max = 15,
            strictRunningOrder = true,
            runningOrderSort = "place DESC",
            courseTimeCode = "UKA02",
            entryRule = ENTRY_RULE_CLOSED
        )
        val TRIPLE_A = ClassTemplate(
            type = CLASS_TYPE_SPECIAL,
            code = 300,
            nameTemplate = "Triple A",
            discipline = DISCIPLINE_AGILITY,
            courseTimeCode = "UKA02",
            entryRule = ENTRY_RULE_ANY_GRADE
        )
        val AA_SPEEDSTAKES = ClassTemplate(
            type = CLASS_TYPE_SPECIAL,
            code = 310,
            nameTemplate = "Speedstakes",
            discipline = DISCIPLINE_AGILITY,
            courseTimeCode = "UKA03"
        )
        val TRY_OUT = ClassTemplate(
            type = CLASS_TYPE_SPECIAL_GROUP,
            code = 400,
            nameTemplate = "Try Out",
            scoringMethod = SCORING_METHOD_MASTERS,
            runUnits = 0,
            entryFee = 3500,
            lateEntryFee = 3500

        )
        val TRY_OUT_PENTATHLON = ClassTemplate(
            type = CLASS_TYPE_SPECIAL_GROUP_MEMBER,
            code = 410,
            parent = TRY_OUT,
            nameTemplate = "Pentathlon",
            scoringMethod = SCORING_METHOD_WAO_PENTATHLON,
            entryRule = ENTRY_RULE_CLOSED
        )
        val TRY_OUT_PENTATHLON_AGILITY1 = ClassTemplate(
            type = CLASS_TYPE_SPECIAL_GROUP_MEMBER,
            code = 412,
            nameTemplate = "Pentathlon Agility 1",
            discipline = DISCIPLINE_AGILITY,
            strictRunningOrder = true,
            parent = TRY_OUT_PENTATHLON,
            previous = TRY_OUT_PENTATHLON,
            column = 1,
            courseTimeCode = "UKA04",
            eliminationTime = 50000,
            flagTies = false,
            noTimeFormula = true,
            scoringMethod = SCORING_METHOD_WAO_PENTATHLON,
            entryRule = ENTRY_RULE_CLOSED
        )
        val TRY_OUT_PENTATHLON_JUMPING1 = ClassTemplate(
            type = CLASS_TYPE_SPECIAL_GROUP_MEMBER,
            code = 413,
            nameTemplate = "Pentathlon Jumping 1",
            discipline = DISCIPLINE_JUMPING,
            strictRunningOrder = true,
            parent = TRY_OUT_PENTATHLON,
            previous = TRY_OUT_PENTATHLON_AGILITY1,
            column = 2,
            courseTimeCode = "UKA04",
            runningOrderSort = "wao",
            eliminationTime = 50000,
            flagTies = false,
            noTimeFormula = true,
            printIntermediateResults = true,
            scoringMethod = SCORING_METHOD_WAO_PENTATHLON,
            entryRule = ENTRY_RULE_CLOSED
        )
        val TRY_OUT_PENTATHLON_JUMPING2 = ClassTemplate(
            type = CLASS_TYPE_SPECIAL_GROUP_MEMBER,
            code = 414,
            nameTemplate = "Pentathlon Jumping 2",
            discipline = DISCIPLINE_JUMPING,
            strictRunningOrder = true,
            parent = TRY_OUT_PENTATHLON,
            previous = TRY_OUT_PENTATHLON_JUMPING1,
            column = 3,
            courseTimeCode = "UKA04",
            runningOrderSort = "wao",
            eliminationTime = 50000,
            flagTies = false,
            day = 2,
            noTimeFormula = true,
            printIntermediateResults = true,
            scoringMethod = SCORING_METHOD_WAO_PENTATHLON,
            entryRule = ENTRY_RULE_CLOSED
        )
        val TRY_OUT_PENTATHLON_AGILITY2 = ClassTemplate(
            type = CLASS_TYPE_SPECIAL_GROUP_MEMBER,
            code = 415,
            nameTemplate = "Pentathlon Agility 2",
            discipline = DISCIPLINE_AGILITY,
            strictRunningOrder = true,
            parent = TRY_OUT_PENTATHLON,
            previous = TRY_OUT_PENTATHLON_JUMPING2,
            column = 4,
            courseTimeCode = "UKA04",
            runningOrderSort = "wao",
            eliminationTime = 50000,
            flagTies = false,
            day = 3,
            noTimeFormula = true,
            printIntermediateResults = true,
            scoringMethod = SCORING_METHOD_WAO_PENTATHLON,
            entryRule = ENTRY_RULE_CLOSED
        )
        val TRY_OUT_PENTATHLON_SPEEDSTAKES = ClassTemplate(
            type = CLASS_TYPE_SPECIAL_GROUP_MEMBER,
            code = 416,
            nameTemplate = "Pentathlon Speedstakes",
            discipline = DISCIPLINE_SPEEDSTAKES,
            strictRunningOrder = true,
            parent = TRY_OUT_PENTATHLON,
            previous = TRY_OUT_PENTATHLON_AGILITY2,
            column = 5,
            courseTimeCode = "UKA04",
            runningOrderSort = "hasRun, place desc",
            eliminationTime = 50000,
            flagTies = false,
            day = 3,
            noTimeFormula = true,
            scoringMethod = SCORING_METHOD_WAO_PENTATHLON,
            entryRule = ENTRY_RULE_CLOSED
        )
        val TRY_OUT_GAMES = ClassTemplate(
            type = CLASS_TYPE_SPECIAL_GROUP_MEMBER,
            code = 420,
            parent = TRY_OUT,
            nameTemplate = "Try Out Games",
            scoringMethod = SCORING_METHOD_GAMES,
            entryRule = ENTRY_RULE_CLOSED
        )
        val TRY_OUT_GAMES_SNOOKER = ClassTemplate(
            type = CLASS_TYPE_SPECIAL_GROUP_MEMBER,
            code = 421,
            nameTemplate = "Try Out Snooker",
            discipline = DISCIPLINE_SNOOKER,
            noTimeFaults = true,
            previous = TRY_OUT_GAMES,
            parent = TRY_OUT_GAMES,
            scoringMethod = SCORING_METHOD_GAMES,
            strictRunningOrder = true,
            courseLengthNeeded = false,
            flagTies = false,
            day = 1,
            courseTimeCode = "UKA03",
            entryRule = ENTRY_RULE_CLOSED
        )
        val TRY_OUT_GAMES_GAMBLERS = ClassTemplate(
            type = CLASS_TYPE_SPECIAL_GROUP_MEMBER,
            code = 423,
            nameTemplate = "Try Out Gamblers",
            discipline = DISCIPLINE_GAMBLERS,
            column = 2,
            parent = TRY_OUT_GAMES,
            previous = TRY_OUT_GAMES_SNOOKER,
            scoringMethod = SCORING_METHOD_GAMES,
            runningOrderSort = "hasRun, place desc",
            strictRunningOrder = true,
            courseLengthNeeded = false,
            flagTies = false,
            day = 2,
            gamble2 = 20,
            courseTimeCode = "UKA03",
            entryRule = ENTRY_RULE_CLOSED
        )
        val UK_OPEN_BIATHLON = ClassTemplate(
            ruleBook = "UK_OPEN",
            type = CLASS_TYPE_SPECIAL_GROUP,
            code = 2010,
            nameTemplate = "Biathlon",
            groupName = "Biathlon",
            entryRule = ENTRY_RULE_CLOSED,
            logo = "CSJ.png",
            noTimeFormula = true,
            day = 3
        )
        val UK_OPEN_BIATHLON_JUMPING = ClassTemplate(
            ruleBook = "UK_OPEN",
            type = CLASS_TYPE_SPECIAL_GROUP_MEMBER,
            code = 2011,
            nameTemplate = "Biathlon Jumping",
            discipline = DISCIPLINE_JUMPING,
            entryRule = ENTRY_RULE_CLOSED,
            parent = UK_OPEN_BIATHLON,
            logo = "CSJ.png",
            noTimeFormula = true,
            day = 2
        )
        /*
        val UK_OPEN_BIATHLON_JUMPING_GROUP = ClassTemplate(
            ruleBook = "UK_OPEN",
            type = CLASS_TYPE_SUB_CLASS,
            nameTemplate = "Biathlon Jumping <part>",
            discipline = DISCIPLINE_JUMPING,
            entryRule = ENTRY_RULE_CLOSED,
            code = 2012,
            logo = "CSJ.png",
            noTimeFormula = true,
            day = 2,
            parent = UK_OPEN_BIATHLON_JUMPING,
            grouped = true
        )
        */
        val UK_OPEN_BIATHLON_AGILITY = ClassTemplate(
            ruleBook = "UK_OPEN",
            type = CLASS_TYPE_SPECIAL_GROUP_MEMBER,
            code = 2014,
            nameTemplate = "Biathlon Agility",
            discipline = DISCIPLINE_AGILITY,
            entryRule = ENTRY_RULE_CLOSED,
            logo = "CSJ.png",
            noTimeFormula = true,
            parent = UK_OPEN_BIATHLON,
            previous = UK_OPEN_BIATHLON_JUMPING,
            runningOrderSort = "hasRun, place desc",
            flagTies = false,
            cut_percent = 100,
            day = 3,
            column = 2
        )
        val UK_OPEN_STEEPLECHASE_ROUND1 = ClassTemplate(
            ruleBook = "UK_OPEN",
            type = CLASS_TYPE_SPECIAL_SERIES,
            code = 2020,
            nameTemplate = "Steeplechase Round 1",
            logo = "CSJ.png",
            noTimeFormula = true,
            discipline = DISCIPLINE_STEEPLECHASE,
            scoringMethod = SCORING_METHOD_UK_OPEN_STEEPLECHASE,
            faultToTime = 600,
            entryRule = ENTRY_RULE_CLOSED
        )
        /*
        val UK_OPEN_STEEPLECHASE_ROUND1_GROUP = ClassTemplate(
            ruleBook = "UK_OPEN",
            type = CLASS_TYPE_SUB_CLASS,
            code = 2021,
            parent = UK_OPEN_STEEPLECHASE_ROUND1,
            nameTemplate = "Steeplechase Round 1 <part>",
            discipline = DISCIPLINE_STEEPLECHASE,
            logo = "CSJ.png",
            noTimeFormula = true,
            strictRunningOrder = true,
            entryRule = ENTRY_RULE_CLOSED,
            scoringMethod = SCORING_METHOD_UK_OPEN_STEEPLECHASE,
            faultToTime = 600,
            grouped = true
        )
        */
        val UK_OPEN_STEEPLECHASE_ROUND2 = ClassTemplate(
            ruleBook = "UK_OPEN",
            type = CLASS_TYPE_SPECIAL_SERIES_MEMBER,
            code = 2022,
            nameTemplate = "Steeplechase Final",
            logo = "CSJ.png",
            previous = UK_OPEN_STEEPLECHASE_ROUND1,
            discipline = DISCIPLINE_STEEPLECHASE,
            strictRunningOrder = true,
            entryRule = ENTRY_RULE_CLOSED,
            noTimeFormula = true,
            runningOrderSort = "hasRun, place desc",
            scoringMethod = SCORING_METHOD_UK_OPEN_STEEPLECHASE,
            faultToTime = 600,
            cut_max = 25,
            day = 2
        )
        val UK_OPEN_GAMES = ClassTemplate(
            ruleBook = "UK_OPEN",
            type = CLASS_TYPE_SPECIAL_GROUP,
            code = 2030,
            nameTemplate = "Open Games",
            logo = "CSJ.png",
            scoringMethod = SCORING_METHOD_GAMES,
            entryRule = ENTRY_RULE_CLOSED,
            day = 3
        )
        val UK_OPEN_GAMES_SNOOKER = ClassTemplate(
            ruleBook = "UK_OPEN",
            type = CLASS_TYPE_SPECIAL_GROUP_MEMBER,
            parent = UK_OPEN_GAMES,
            code = 2031,
            nameTemplate = "Open Snooker",
            logo = "CSJ.png",
            entryRule = ENTRY_RULE_CLOSED,
            discipline = DISCIPLINE_SNOOKER,
            scoringMethod = SCORING_METHOD_GAMES,
            //qualifierScore = 20,
            day = 1
        )
        /*
        val UK_OPEN_GAMES_SNOOKER_GROUP = ClassTemplate(
            ruleBook = "UK_OPEN",
            type = CLASS_TYPE_SUB_CLASS,
            code = 2032,
            parent = UK_OPEN_GAMES_SNOOKER,
            nameTemplate = "Open Snooker <part>",
            discipline = DISCIPLINE_SNOOKER,
            logo = "CSJ.png",
            noTimeFaults = true,
            scoringMethod = SCORING_METHOD_GAMES,
            strictRunningOrder = true,
            courseLengthNeeded = false,
            flagTies = false,
            entryRule = ENTRY_RULE_CLOSED,
            day = 1,
            grouped = true
        )
        */
        val UK_OPEN_GAMES_GAMBLERS = ClassTemplate(
            ruleBook = "UK_OPEN",
            type = CLASS_TYPE_SPECIAL_GROUP_MEMBER,
            code = 2033,
            nameTemplate = "Open Gamblers",
            discipline = DISCIPLINE_GAMBLERS,
            logo = "CSJ.png",
            column = 2,
            parent = UK_OPEN_GAMES,
            previous = UK_OPEN_GAMES_SNOOKER,
            scoringMethod = SCORING_METHOD_GAMES,
            runningOrderSort = "hasRun, place desc",
            strictRunningOrder = true,
            courseLengthNeeded = false,
            flagTies = false,
            entryRule = ENTRY_RULE_CLOSED,
            cut_max = 30,
            gamble2 = 20,
            gamble2TimeDeduct = 1000,
            gambleBonus = 9,
            gambleFaults = false,
            day = 3
        )
        val UK_OPEN_PENTATHLON = ClassTemplate(
            ruleBook = "UK_OPEN",
            type = CLASS_TYPE_SPECIAL_GROUP,
            code = 2040,
            nameTemplate = "Pentathlon",
            scoringMethod = SCORING_METHOD_UK_OPEN_PENTATHLON,
            entryRule = ENTRY_RULE_CLOSED,
            day = 4,
            logo = "CSJ.png",
            noTimeFormula = true,
            whenChildClosed = { agilityClass, closedFlags ->
                when (closedFlags) {
                    3 -> {
                        Reports.printResults(agilityClass.id, subResultsFlag = 3)
                    }
                    15 -> {
                        Reports.printResults(agilityClass.id, subResultsFlag = 15)
                        UkOpenUtils.prepareSpeedstakes(agilityClass)
                    }
                }
            }
        )
        val UK_OPEN_PENTATHLON_AGILITY1 = ClassTemplate(
            ruleBook = "UK_OPEN",
            type = CLASS_TYPE_SPECIAL_GROUP_MEMBER,
            code = 2041,
            nameTemplate = "Pentathlon Agility 1",
            discipline = DISCIPLINE_AGILITY,
            strictRunningOrder = true,
            logo = "CSJ.png",
            parent = UK_OPEN_PENTATHLON,
            column = 1,
            flagTies = false,
            noTimeFormula = true,
            faultToTime = 600,
            scoringMethod = SCORING_METHOD_UK_OPEN_PENTATHLON,
            entryRule = ENTRY_RULE_CLOSED,
            day = 1
        )
        /*
        val UK_OPEN_PENTATHLON_AGILITY1_GROUP = ClassTemplate(
            ruleBook = "UK_OPEN",
            type = CLASS_TYPE_SUB_CLASS,
            code = 2042,
            parent = UK_OPEN_PENTATHLON_AGILITY1,
            nameTemplate = "Pentathlon Agility 1 <part>",
            logo = "CSJ.png",
            discipline = DISCIPLINE_AGILITY,
            strictRunningOrder = true,
            faultToTime = 600,
            noTimeFormula = true,
            scoringMethod = SCORING_METHOD_UK_OPEN_PENTATHLON,
            entryRule = ENTRY_RULE_CLOSED,
            day = 1,
            grouped = true
        )        
         */
        val UK_OPEN_PENTATHLON_JUMPING1 = ClassTemplate(
            ruleBook = "UK_OPEN",
            type = CLASS_TYPE_SPECIAL_GROUP_MEMBER,
            code = 2043,
            nameTemplate = "Pentathlon Jumping 1",
            discipline = DISCIPLINE_JUMPING,
            logo = "CSJ.png",
            strictRunningOrder = true,
            parent = UK_OPEN_PENTATHLON,
            column = 2,
            flagTies = false,
            noTimeFormula = true,
            faultToTime = 600,
            scoringMethod = SCORING_METHOD_UK_OPEN_PENTATHLON,
            entryRule = ENTRY_RULE_CLOSED,
            day = 1
        )
        /*
        
        val UK_OPEN_PENTATHLON_JUMPING1_GROUP = ClassTemplate(
            ruleBook = "UK_OPEN",
            type = CLASS_TYPE_SUB_CLASS,
            code = 2044,
            parent = UK_OPEN_PENTATHLON_JUMPING1,
            nameTemplate = "Pentathlon Jumping 1 <part>",
            discipline = DISCIPLINE_JUMPING,
            strictRunningOrder = true,
            faultToTime = 600,
            flagTies = false,
            noTimeFormula = true,
            scoringMethod = SCORING_METHOD_UK_OPEN_PENTATHLON,
            entryRule = ENTRY_RULE_CLOSED,
            day = 1,
            grouped = true
        )
         */
        val UK_OPEN_PENTATHLON_JUMPING2 = ClassTemplate(
            ruleBook = "UK_OPEN",
            type = CLASS_TYPE_SPECIAL_GROUP_MEMBER,
            code = 2045,
            nameTemplate = "Pentathlon Jumping 2",
            discipline = DISCIPLINE_JUMPING,
            strictRunningOrder = true,
            parent = UK_OPEN_PENTATHLON,
            logo = "CSJ.png",
            column = 3,
            flagTies = false,
            day = 2,
            noTimeFormula = true,
            faultToTime = 600,
            scoringMethod = SCORING_METHOD_UK_OPEN_PENTATHLON,
            entryRule = ENTRY_RULE_CLOSED
        )
        /*
        val UK_OPEN_PENTATHLON_JUMPING2_GROUP = ClassTemplate(
            ruleBook = "UK_OPEN",
            type = CLASS_TYPE_SUB_CLASS,
            code = 2046,
            parent = UK_OPEN_PENTATHLON_JUMPING2,
            nameTemplate = "Pentathlon Jumping 2 <part>",
            discipline = DISCIPLINE_JUMPING,
            strictRunningOrder = true,
            faultToTime = 600,
            flagTies = false,
            day = 2,
            noTimeFormula = true,
            logo = "CSJ.png",
            scoringMethod = SCORING_METHOD_UK_OPEN_PENTATHLON,
            entryRule = ENTRY_RULE_CLOSED,
            grouped = true
        )
        */
        val UK_OPEN_PENTATHLON_AGILITY2 = ClassTemplate(
            ruleBook = "UK_OPEN",
            type = CLASS_TYPE_SPECIAL_GROUP_MEMBER,
            code = 2047,
            nameTemplate = "Pentathlon Agility 2",
            discipline = DISCIPLINE_AGILITY,
            strictRunningOrder = true,
            parent = UK_OPEN_PENTATHLON,
            column = 4,
            flagTies = false,
            day = 2,
            noTimeFormula = true,
            faultToTime = 600,
            scoringMethod = SCORING_METHOD_UK_OPEN_PENTATHLON,
            entryRule = ENTRY_RULE_CLOSED
        )
        /*
        val UK_OPEN_PENTATHLON_AGILITY2_GROUP = ClassTemplate(
            ruleBook = "UK_OPEN",
            type = CLASS_TYPE_SUB_CLASS,
            code = 2048,
            parent = UK_OPEN_PENTATHLON_AGILITY2,
            nameTemplate = "Pentathlon Agility 2 <part>",
            discipline = DISCIPLINE_AGILITY,
            strictRunningOrder = true,
            logo = "CSJ.png",
            faultToTime = 600,
            flagTies = false,
            day = 2,
            noTimeFormula = true,
            scoringMethod = SCORING_METHOD_UK_OPEN_PENTATHLON,
            entryRule = ENTRY_RULE_CLOSED,
            grouped = true
        )
        */
        val UK_OPEN_PENTATHLON_SPEEDSTAKES = ClassTemplate(
            ruleBook = "UK_OPEN",
            type = CLASS_TYPE_SPECIAL_GROUP_MEMBER,
            code = 2050,
            nameTemplate = "Pentathlon Speedstakes",
            discipline = DISCIPLINE_SPEEDSTAKES,
            strictRunningOrder = true,
            parent = UK_OPEN_PENTATHLON,
            column = 5,
            runningOrderSort = "hasRun, place desc",
            faultToTime = 600,
            logo = "CSJ.png",
            flagTies = false,
            cut_max = 20,
            cut_columns = 4,
            day = 4,
            noTimeFormula = true,
            scoringMethod = SCORING_METHOD_UK_OPEN_PENTATHLON,
            entryRule = ENTRY_RULE_CLOSED
        )
        val UK_OPEN_CHAMPIONSHIP_JUMPING = ClassTemplate(
            ruleBook = "UK_OPEN",
            type = CLASS_TYPE_SPECIAL_SERIES,
            code = 2060,
            nameTemplate = "Championship Jumping",
            logo = "CSJ.png",
            noTimeFormula = true,
            discipline = DISCIPLINE_JUMPING,
            entryRule = ENTRY_RULE_CLOSED,
            flagTies = false,
            day = 2
        )
        /*
        val UK_OPEN_CHAMPIONSHIP_JUMPING_GROUP = ClassTemplate(
            ruleBook = "UK_OPEN",
            type = CLASS_TYPE_SUB_CLASS,
            code = 2061,
            nameTemplate = "Championship Jumping <part>",
            discipline = DISCIPLINE_JUMPING,
            entryRule = ENTRY_RULE_CLOSED,
            logo = "CSJ.png",
            noTimeFormula = true,
            parent = UK_OPEN_CHAMPIONSHIP_JUMPING,
            flagTies = false,
            day = 2,
            grouped = true
        )
        */
        val UK_OPEN_CHAMPIONSHIP_AGILITY = ClassTemplate(
            ruleBook = "UK_OPEN",
            type = CLASS_TYPE_SPECIAL_SERIES_MEMBER,
            code = 2062,
            nameTemplate = "Championship Agility",
            logo = "CSJ.png",
            noTimeFormula = true,
            discipline = DISCIPLINE_AGILITY,
            entryRule = ENTRY_RULE_CLOSED,
            previous = UK_OPEN_CHAMPIONSHIP_JUMPING,
            flagTies = false,
            cut_percent = 50,
            cutPercentOf = "entered",
            cut_min = 10,
            day = 3,
            whenClosed = { agilityClass ->
                UkOpenUtils.prepareChallenge(agilityClass.idCompetition)
            }
        )
        val UK_OPEN_CHAMPIONSHIP_FINAL = ClassTemplate(
            ruleBook = "UK_OPEN",
            type = CLASS_TYPE_SPECIAL_SERIES_MEMBER,
            code = 2063,
            nameTemplate = "Championship Final",
            discipline = DISCIPLINE_AGILITY,
            entryRule = ENTRY_RULE_CLOSED,
            logo = "CSJ.png",
            noTimeFormula = true,
            previous = UK_OPEN_CHAMPIONSHIP_AGILITY,
            runningOrderSort = "hasRun, place desc",
            runningOrderStart = 2,
            flagTies = false,
            cut_max = 10,
            day = 4
        )
        val UK_OPEN_CHALLENGER = ClassTemplate(
            ruleBook = "UK_OPEN",
            type = CLASS_TYPE_SPECIAL,
            code = 2070,
            nameTemplate = "Challenger",
            discipline = DISCIPLINE_AGILITY,
            entryRule = ENTRY_RULE_CLOSED,
            logo = "CSJ.png",
            noTimeFormula = true,
            flagTies = false,
            day = 4,
            whenClosed = { agilityClass ->
                UkOpenUtils.challengersToFinal(agilityClass.idCompetition)
            }
        )

        /*
        val SW_GAMES_CHALLENGE = ClassTemplate(
            type = CLASS_TYPE_SPECIAL_GROUP,
            code = 600,
            nameTemplate = "SW Games Challenge",
            groupName = "Games",
            scoringMethod = SCORING_METHOD_GAMES,
            runUnits = 0,
            entryFee = 400,
            combineHeights = true,
            lateEntryFee = 400,
            entryRule = ENTRY_RULE_INVITE
        )
        val SW_GAMES_CHALLENGE_SNOOKER = ClassTemplate(
            type = CLASS_TYPE_SPECIAL_GROUP_MEMBER,
            code = 601,
            nameTemplate = "SW Games Challenge Snooker",
            discipline = DISCIPLINE_SNOOKER,
            noTimeFaults = true,
            parent = SW_GAMES_CHALLENGE,
            combineHeights = true,
            scoringMethod = SCORING_METHOD_GAMES,
            strictRunningOrder = true,
            courseLengthNeeded = false,
            flagTies = false,
            courseTimeCode = "UKA03",
            entryRule = ENTRY_RULE_CLOSED
        )
        val SW_GAMES_CHALLENGE_GAMBLERS = ClassTemplate(
            type = CLASS_TYPE_SPECIAL_GROUP_MEMBER,
            code = 602,
            nameTemplate = "SW Games Challenge Gamblers",
            discipline = DISCIPLINE_GAMBLERS,
            column = 2,
            parent = SW_GAMES_CHALLENGE,
            previous = SW_GAMES_CHALLENGE_SNOOKER,
            scoringMethod = SCORING_METHOD_GAMES,
            runningOrderSort = "hasRun, place desc",
            combineHeights = true,
            strictRunningOrder = true,
            courseLengthNeeded = false,
            flagTies = false,
            courseTimeCode = "UKA02",
            entryRule = ENTRY_RULE_CLOSED
        )
        val SW_STEEPLECHASE_SEMI_FINAL1 = ClassTemplate(
            type = CLASS_TYPE_SPECIAL_SERIES,
            code = 603,
            nameTemplate = "SW Steeplechase Semi (Beg/Nov)",
            groupName = "S/Chase (Beg/Nov)",
            discipline = DISCIPLINE_STEEPLECHASE,
            strictRunningOrder = true,
            entryRule = ENTRY_RULE_INVITE,
            courseTimeCode = "UKA02"
        )
        val SW_STEEPLECHASE_FINAL1 = ClassTemplate(
            type = CLASS_TYPE_SPECIAL_SERIES_MEMBER,
            code = 604,
            nameTemplate = "SW Steeplechase Final (Beg/Nov)",
            previous = SW_STEEPLECHASE_SEMI_FINAL1,
            discipline = DISCIPLINE_STEEPLECHASE,
            strictRunningOrder = true,
            entryRule = ENTRY_RULE_CLOSED,
            runningOrderSort = "hasRun, place desc",
            cut_max = 10,
            cut_max_little = 8,
            courseTimeCode = "UKA02"
        )
        val SW_STEEPLECHASE_SEMI_FINAL2 = ClassTemplate(
            type = CLASS_TYPE_SPECIAL_SERIES,
            code = 605,
            nameTemplate = "SW Steeplechase Semi (Snr/Ch)",
            groupName = "S/Chase (Snr/Ch)",
            discipline = DISCIPLINE_STEEPLECHASE,

            strictRunningOrder = true,
            entryRule = ENTRY_RULE_INVITE,
            courseTimeCode = "UKA04"
        )
        val SW_STEEPLECHASE_FINAL2 = ClassTemplate(
            type = CLASS_TYPE_SPECIAL_SERIES_MEMBER,
            code = 606,
            nameTemplate = "SW Steeplechase Final (Snr/Ch)",
            previous = SW_STEEPLECHASE_SEMI_FINAL2,

            discipline = DISCIPLINE_STEEPLECHASE,
            strictRunningOrder = true,
            entryRule = ENTRY_RULE_CLOSED,
            runningOrderSort = "hasRun, place desc",
            cut_max = 10,
            cut_max_little = 8,
            courseTimeCode = "UKA04"
        )
        val SW_CHALLENGE_JUMPING1 = ClassTemplate(
            type = CLASS_TYPE_SPECIAL_SERIES,
            code = 607,
            nameTemplate = "SW Performance Jumping (Beg/Nov)",
            groupName = "Perf (Beg/Nov)",
            discipline = DISCIPLINE_JUMPING,

            strictRunningOrder = true,
            entryRule = ENTRY_RULE_INVITE,
            courseTimeCode = "UKA02"
        )
        val SW_CHALLENGE_AGILITY1 = ClassTemplate(
            type = CLASS_TYPE_SPECIAL_GROUP_MEMBER,
            code = 608,
            nameTemplate = "SW Performance Agility (Beg/Nov)",
            previous = SW_CHALLENGE_JUMPING1,

            discipline = DISCIPLINE_AGILITY,
            strictRunningOrder = true,
            entryRule = ENTRY_RULE_CLOSED,
            runningOrderSort = "hasRun, place desc",
            cut_max = 10,
            cut_max_little = 8,
            courseTimeCode = "UKA02"
        )
        val SW_CHALLENGE_JUMPING2 = ClassTemplate(
            type = CLASS_TYPE_SPECIAL_SERIES,
            code = 609,
            nameTemplate = "SW Performance Jumping (Snr/Ch)",
            groupName = "Perf (Snr/Ch)",
            discipline = DISCIPLINE_JUMPING,

            strictRunningOrder = true,
            entryRule = ENTRY_RULE_INVITE,
            courseTimeCode = "UKA04"
        )
        val SW_CHALLENGE_AGILITY2 = ClassTemplate(
            type = CLASS_TYPE_SPECIAL_GROUP_MEMBER,
            code = 610,
            nameTemplate = "SW Performance Agility (Snr/Ch)",
            previous = SW_CHALLENGE_JUMPING2,

            discipline = DISCIPLINE_AGILITY,
            strictRunningOrder = true,
            entryRule = ENTRY_RULE_CLOSED,
            runningOrderSort = "hasRun, place desc",
            cut_max = 10,
            cut_max_little = 8,
            courseTimeCode = "UKA04"
        )        
         */
        val FAB_AGILITY = ClassTemplate(
            code = 4001,
            ruleBook = "FAB",
            nameTemplate = "<grade> Agility",
            discipline = DISCIPLINE_AGILITY,
            entryRule = ENTRY_RULE_GRADE1,
            subDivisions = "ABC;Collie"
        )
        val FAB_JUMPING = ClassTemplate(
            code = 4002,
            ruleBook = "FAB",
            nameTemplate = "<grade> Jumping",
            discipline = DISCIPLINE_JUMPING,
            entryRule = ENTRY_RULE_GRADE2,
            subDivisions = "ABC;Collie"
        )
        val FAB_STEEPLECHASE = ClassTemplate(
            code = 4003,
            ruleBook = "FAB",
            nameTemplate = "<grade> Steeplechase",
            discipline = DISCIPLINE_STEEPLECHASE,
            entryRule = ENTRY_RULE_GRADE3,
            subDivisions = "ABC;Collie"
        )
        val FAB_GRAND_PRIX = ClassTemplate(
            code = 4004,
            ruleBook = "FAB",
            nameTemplate = "Grand Prix",
            discipline = DISCIPLINE_AGILITY,
            entryRule = ENTRY_RULE_ANY_GRADE,
            subDivisions = "ABC;Collie"
        )
        val FAB_ALLSORTS = ClassTemplate(
            code = 4005,
            ruleBook = "FAB",
            nameTemplate = "Allsorts",
            discipline = DISCIPLINE_AGILITY,
            entryRule = ENTRY_RULE_ANY_GRADE,
            courseLengthNeeded = false,
            courseTimeNeeded = false,
            nfcOnly = true
        )
        val FAB_GAMBLERS = ClassTemplate(
            code = 4010,
            ruleBook = "FAB",
            nameTemplate = "Gamblers",
            discipline = DISCIPLINE_GAMBLERS,
            entryRule = ENTRY_RULE_ANY_GRADE,
            scoringMethod = SCORING_METHOD_GAMES,
            courseLengthNeeded = false,
            gamble1=20,
            gamble2 = 0,
            subDivisions = "ABC;Collie"
        )
        val FAB_TUNNEL_EXCHANGE = ClassTemplate(
            code = 4011,
            ruleBook = "FAB",
            nameTemplate = "Tunnel Exchange",
            discipline = DISCIPLINE_STEEPLECHASE,
            entryRule = ENTRY_RULE_ANY_GRADE,
            subDivisions = "ABC;Collie"
        )
        val FAB_AGILITY_FINALS = ClassTemplate(
            code = 4051,
            ruleBook = "FAB",
            nameTemplate = "<grade> Agility Finals",
            discipline = DISCIPLINE_AGILITY,
            entryRule = ENTRY_RULE_GRADE1,
            subDivisions = "ABC;Collie"
        )
        val FAB_JUMPING_FINALS = ClassTemplate(
            code = 4052,
            ruleBook = "FAB",
            nameTemplate = "<grade> Jumping Finals",
            discipline = DISCIPLINE_JUMPING,
            entryRule = ENTRY_RULE_GRADE2,
            subDivisions = "ABC;Collie"
        )
        val FAB_STEEPLECHASE_FINALS = ClassTemplate(
            code = 4053,
            ruleBook = "FAB",
            nameTemplate = "<grade> Steeplechase Finals",
            discipline = DISCIPLINE_STEEPLECHASE,
            entryRule = ENTRY_RULE_GRADE3,
            subDivisions = "ABC;Collie"
        )
        val FAB_GRAND_PRIX_FINALS = ClassTemplate(
            code = 4054,
            ruleBook = "FAB",
            nameTemplate = "Grand Prix Finals",
            entryRule = ENTRY_RULE_ANY_GRADE,
            subDivisions = "ABC;Collie"
        )

        val FAB_IFCS_AGILITY = ClassTemplate(
            code = 4101,
            ruleBook = "IFCS",
            nameTemplate = "IFCS Agility",
            discipline = DISCIPLINE_AGILITY,
            entryRule = ENTRY_RULE_ANY_GRADE
        )
        val FAB_IFCS_JUMPING = ClassTemplate(
            code = 4102,
            ruleBook = "IFCS",
            nameTemplate = "IFCS Jumping",
            discipline = DISCIPLINE_JUMPING,
            entryRule = ENTRY_RULE_ANY_GRADE
        )
        val FAB_IFCS_GAMBLERS = ClassTemplate(
            code = 4103,
            ruleBook = "IFCS",
            nameTemplate = "IFCS Gamblers",
            discipline = DISCIPLINE_GAMBLERS,
            entryRule = ENTRY_RULE_ANY_GRADE,
            scoringMethod = SCORING_METHOD_GAMES,
            courseLengthNeeded = false,
            gamble1=20,
            gamble2 = 0
        )
        val FAB_IFCS_SNOOKER = ClassTemplate(
            code = 4104,
            ruleBook = "IFCS",
            nameTemplate = "IFCS Snooker",
            discipline = DISCIPLINE_SNOOKER,
            entryRule = ENTRY_RULE_ANY_GRADE,
            noTimeFaults = true,
            scoringMethod = SCORING_METHOD_GAMES,
            courseLengthNeeded = false
        )
        val IND_MISC = ClassTemplate(
            code = 5000,
            ruleBook = "IND",
            discipline = DISCIPLINE_MISC,
            entryRule = ENTRY_RULE_ANY_GRADE
        )
        val UKA_PRODUCT_SALE = ClassTemplate(
            type = CLASS_TYPE_UNDEFINED,
            code = 9000,
            nameTemplate = "Clothing Item",
            entryRule = ENTRY_RULE_CLOSED
        )
    }
}