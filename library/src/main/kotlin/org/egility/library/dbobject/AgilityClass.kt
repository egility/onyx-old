/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.library.dbobject

import org.egility.library.database.*
import org.egility.library.general.*
import java.util.*
import kotlin.collections.HashMap


open class AgilityClassRaw<T : DbTable<T>>(_connection: DbConnection? = null, vararg columnNames: String) :
    DbTable<T>(_connection, "agilityClass", *columnNames) {
    open var id: Int by DbPropertyInt("idAgilityClass")
    open var idCompetition: Int by DbPropertyInt("idCompetition")
    open var idAgilityClassParent: Int by DbPropertyInt("idAgilityClassParent")
    open var number: Int by DbPropertyInt("classNumber")
    open var numberSuffix: String by DbPropertyString("classNumberSuffix")
    open var part: String by DbPropertyString("part")
    open var partType: Int by DbPropertyInt("partType")
    open var code: Int by DbPropertyInt("classCode")
    open var codeInstance: Int by DbPropertyInt("classCodeInstance")
    open var block: Int by DbPropertyInt("block")
    open var groupColumn: Int by DbPropertyInt("groupColumn")
    open var prefix: String by DbPropertyString("prefix")
    open var suffix: String by DbPropertyString("suffix")
    open var sponsor: String by DbPropertyString("sponsor")
    open var date: Date by DbPropertyDate("classDate")
    open var name: String by DbPropertyString("className")
    open var nameLong: String by DbPropertyString("classNameLong")
    open var judge: String by DbPropertyString("judge")
    open var ringNumber: Int by DbPropertyInt("ringNumber")
    open var ringOrder: Int by DbPropertyInt("ringOrder")
    open var entryGroupCode: Int by DbPropertyInt("entryGroupCode")
    open var heightOptions: String by DbPropertyString("heightOptions")
    open var gradeCodes: String by DbPropertyString("gradeCodes")
    open var heightCodes: String by DbPropertyString("heightCodes")
    open var jumpHeightCodes: String by DbPropertyString("jumpHeightCodes")
    open var subDivisions: String by DbPropertyString("subDivisions")
    open var heightRunningOrder: String by DbPropertyString("heightRunningOrder")
    open var groupRunningOrder: String by DbPropertyString("groupRunningOrder")
    open var courseLength: Int by DbPropertyInt("courseLength")
    open var progress: Int by DbPropertyInt("classProgress")
    open var runningOrdersGenerated: Boolean by DbPropertyBoolean("runningOrdersGenerated")
    open var closedForLateEntries: Boolean by DbPropertyBoolean("closedForLateEntries")
    open var readyToRun: Boolean by DbPropertyBoolean("readyToRun")
    open var finalized: Boolean by DbPropertyBoolean("finalized")
    open var investigation: Boolean by DbPropertyBoolean("investigation")
    open var resultsProcessed: Boolean by DbPropertyBoolean("resultsProcessed")
    open var walkingOverLunch: Boolean by DbPropertyBoolean("walkingOverLunch")
    open var entryFee: Int by DbPropertyInt("entryFee")
    open var entryFeeMembers: Int by DbPropertyInt("entryFeeMembers")
    open var lateEntryFee: Int by DbPropertyInt("lateEntryFee")
    open var entryRule: Int by DbPropertyInt("entryRule")
    open var combineHeights: Boolean by DbPropertyBoolean("combineHeights")
    open var startTime: Date by DbPropertyDate("startTime")
    open var presentingTime: Date by DbPropertyDate("presentingTime")
    open var presented: Boolean by DbPropertyBoolean("presented")
    open var dataProviderName: String by DbPropertyString("dataProviderName")
    open var heights: Json by DbPropertyJson("heights")
    open var subClasses: Json by DbPropertyJson("subClasses")
    open var runCount: Int by DbPropertyInt("runCount")
    open var groupList: String by DbPropertyString("groupList")
    open var qualifier: Boolean by DbPropertyBoolean("qualifier")
    open var useCallingTo: Boolean by DbPropertyBoolean("useCallingTo")
    open var flag: Boolean by DbPropertyBoolean("flag")
    open var extra: Json by DbPropertyJson("extra")

    open var dateCreated: Date by DbPropertyDate("dateCreated")
    open var deviceCreated: Int by DbPropertyInt("deviceCreated")
    open var dateModified: Date by DbPropertyDate("dateModified")
    open var deviceModified: Int by DbPropertyInt("deviceModified")

    open var idUka: Int by DbPropertyInt("idUka")
    open var gradeCode: String by DbPropertyString("gradeCode")
    open var heightCode: String by DbPropertyString("heightCode")
    open var paperBased: Boolean by DbPropertyBoolean("paperBased")
    open var scoringMethod: Int by DbPropertyInt("scoringMethod")

    open var courseTime: Int by DbPropertyJsonInt("extra", "specification.courseTime")
    open var courseTimeSmall: Int by DbPropertyJsonInt("extra", "specification.courseTimeSmall")
    var openingTime: Int by DbPropertyJsonInt("extra", "specification.openingTime")
    open var gambleTimeLarge: Int by DbPropertyJsonInt("extra", "specification.gambleTimeLarge")
    open var gambleTimeSmall: Int by DbPropertyJsonInt("extra", "specification.gambleTimeSmall")
    open var gambleBonusObstacles: String by DbPropertyJsonString("extra", "specification.gambleBonusObstacles")
    open var gambleBonusScore: Int by DbPropertyJsonInt("extra", "specification.gambleBonusScore")
    open var gamble1: Int by DbPropertyJsonInt("extra", "specification.gamble1")
    open var gamble2: Int by DbPropertyJsonInt("extra", "specification.gamble2")
    var qualifyingPoints: Int by DbPropertyJsonInt("extra", "specification.qualifyingPoints")
    open var obstaclePoints: String by DbPropertyJsonString("extra", "specification.obstaclePoints")
    open var batonFaults: Int by DbPropertyJsonInt("extra", "specification.batonFaults")
    //    val subClasses: JsonNode by DbPropertyJsonObject("extra", "specification.subClasses")
//    open val heights: JsonNode by DbPropertyJsonObject("extra", "heights")
    open var ageBaseDate: Date by DbPropertyJsonDate("extra", "entry.ageBaseDate")

    open var qualifierPlaces: Int by DbPropertyJsonInt("extra", "qualifier.places")
    open var haveAwards: Boolean by DbPropertyJsonBoolean("extra", "haveAwards")
    open var fastestABC: Boolean by DbPropertyJsonBoolean("extra", "fastestABC")

    open var nfc: Boolean by DbPropertyJsonBoolean("extra", "independent.nfc")

    val competition: Competition by DbLink<Competition>({ Competition() })
    val grade: Grade by DbLink<Grade>({ Grade() })
    val height: Height by DbLink<Height>({ Height() })
    val ring: Ring by DbLink<Ring>({ Ring() }, "", "", "idCompetition", "classDate")
}

class AgilityClass(vararg columnNames: String) : AgilityClassRaw<AgilityClass>(null, *columnNames) {


    constructor(idAgilityClass: Int) : this() {
        find(idAgilityClass)
    }

    var rules: String
        get() {
            var result = ""
            if (heightOptions.eq("KC350:KC350,KC450:KC450,KC650:KC650|KC650L")) result = result.append("lho-large-only")
            if (super.ageBaseDate.isNotEmpty() && super.ageBaseDate == date) result = result.append("age-on-day")
            if (nfc) result = result.append("nfc")
            if (fastestABC) result = result.append("fastest-abc")
            return result
        }
        set(value) {
            nfc = false
            fastestABC = false
            for (item in value.split(",")) {
                val option = item.trim().toLowerCase()
                when (option) {
                    "lho-large-only" -> heightOptions = "KC350:KC350,KC450:KC450,KC650:KC650|KC650L"
                    "age-on-day" -> ageBaseDate = date
                    "nfc" -> nfc = true
                    "fastest-abc" -> fastestABC = true
                }
            }
        }

    override var gambleBonusScore: Int
        get() = if (super.gambleBonusScore > 0) super.gambleBonusScore else template.gambleBonus
        set(value) {
            super.gambleBonusScore = value
        }
    override var gamble1: Int
        get() = if (super.gamble1 > 0) super.gamble1 else template.gamble1
        set(value) {
            super.gamble1 = value
        }
    override var gamble2: Int
        get() = if (super.gamble2 > 0) super.gamble2 else template.gamble2
        set(value) {
            super.gamble2 = value
        }


    override var fastestABC: Boolean
        get() = competition.isFabKc || super.fastestABC
        set(value) {
            super.fastestABC = value
        }

    val ukaFinalsCode: Int
        get() = if (template.ukaFinalsQualifierCode > 0) template.ukaFinalsQualifierCode else template.code

    override var presented: Boolean
        get() = if (date < today) true else super.presented
        set(value) {
            super.presented = value
        }

    override var judge: String
        get() = if (super.judge.isEmpty()) ring.judge else super.judge
        set(value) {
            super.judge = value
        }

    override var qualifierPlaces: Int
        get() {
            if (super.qualifierPlaces > 0) {
                return super.qualifierPlaces
            } else {
                return template.qualifierPlaces
            }
        }
        set(value) {
            super.qualifierPlaces = value
        }

/*
    val topABC: Boolean
        get() = competition.isFab //&& heightCodes.oneOf("KC")
*/

    var qCode: String
        get() {
            return if (qualifier) {
                if (qualifierPlaces > 0) qualifierPlaces.toString() else "Q"
            } else ""
        }
        set(value) {
            val number = value.toDoubleDef(-1.0).toInt()
            if (number != -1) {
                qualifier = true
                qualifierPlaces = number
            } else if (value == "Q") {
                qualifier = true
                qualifierPlaces = 0
            } else {
                qualifier = false
                qualifierPlaces = 0
            }
        }


    override var ageBaseDate: Date
        get() = if (super.ageBaseDate.isEmpty()) competition.ageBaseDate else super.ageBaseDate
        set(value) {
            super.ageBaseDate = value
        }

    override fun append(): AgilityClass {
        super.append()
        super.heights.setValue(Json("[]"))
        super.subClasses.setValue(Json("[]"))
        return this
    }

    private var _template: ClassTemplate = ClassTemplate.UNDEFINED
    val template: ClassTemplate
        get() {
            if (_template.code != code) {
                _template = ClassTemplate.select(code)
            }
            return _template
        }

    val hasFullHeight: Boolean
        get() {
            jumpHeightCodes.replace(";", ",").split(",").forEach { code ->
                if (code.matches(Regex("KC[0-9]{3}"))) {
                    return true
                }
            }
            return false
        }

    val hasLowHeight: Boolean
        get() {
            jumpHeightCodes.replace(";", ",").split(",").forEach { code ->
                if (code.matches(Regex("KC[0-9]{3}L"))) {
                    return true
                }
            }
            return false
        }

    val isFullLower: Boolean
        get() = heights.size == 2 && hasFullHeight && hasLowHeight

    val shortName: String
        get() = name
            .replace("Beginners", "Beg")
            .replace("Novice", "Nov")
            .replace("Senior", "Snr")
            .replace("Champion", "Ch")

    override var code: Int
        get() = super.code
        set(value) {
            if (value != super.code) {
                super.code = value
                combineHeights = template.combineHeights
                entryRule = template.entryRule
                groupColumn = template.column
            }
        }

    override var combineHeights: Boolean
        get() = super.combineHeights || template.combineHeights
        set(value) {
            super.combineHeights = value
        }

    val rawEntryFee: Int
        get() = super.entryFee

    override var entryFee: Int
        get() {
            if (super.entryFee > 0) {
                return super.entryFee
            } else if (template.entryFee > 0) {
                return template.entryFee
            } else if (template.isIfcs) {
                return template.runUnits * competition.ifcsFee
            } else {
                return template.runUnits * competition.entryFee
            }
        }
        set(value) {
            super.entryFee = value
        }

    val rawEntryFeeMembers: Int
        get() = super.entryFeeMembers

    override var entryFeeMembers: Int
        get() {
            if (super.entryFeeMembers > 0) {
                return super.entryFeeMembers
            } else if (super.entryFee > 0) {
                return super.entryFee
            } else {
                return template.runUnits * competition.entryFeeMembers
            }
        }
        set(value) {
            super.entryFeeMembers = value
        }

    override var lateEntryFee: Int
        get() {
            if (super.lateEntryFee > 0) {
                return super.lateEntryFee
            } else {
                return template.lateEntryFee
            }
        }
        set(value) {
            super.lateEntryFee = value
        }

    val _readyToRun: Boolean
        get() = super.readyToRun

    override var readyToRun: Boolean
        get() {
            return !template.needsPreparation || super.readyToRun
        }
        set(value) {
            super.readyToRun = value
        }

    val _heightRunningOrder: String
        get() = super.heightRunningOrder

    override var heightRunningOrder: String
        get() {
            return if (super.heightRunningOrder.isEmpty())
                if (isUka && jumpHeightCodes.isEmpty())
                    "UKA650,UKA550,UKA400,UKA300"
                else
                    jumpHeightCodes.replace(";", ",")
            else
                super.heightRunningOrder
        }
        set(value) {
            super.heightRunningOrder = value
        }

    val individualEntryHeights: String
        get() = if (code == ClassTemplate.TEAM.code)
            "UKA650,UKA550,UKA400,UKA300"
        else
            heightRunningOrder


    val gradesCombined: Boolean
        get() = gradeCodes.contains(",")

    val lhoCombined: Boolean
        get() = (hasFullHeight && hasLowHeight && jumpHeightCodes.contains(","))

    val heightsCombined: Boolean
        get() = jumpHeightCodes.contains(",")

    override var batonFaults: Int
        get() = if (super.batonFaults > 0) super.batonFaults else template.batonFaults
        set(value) {
            super.batonFaults = value
        }

    override var obstaclePoints: String
        get() = if (super.obstaclePoints.isEmpty())
            if (isUkaStyle) "55433222222111111111" else ""
        else
            super.obstaclePoints
        set(value) {
            super.obstaclePoints = value
        }

    var obstacles7Point: Int
        get() = obstaclePoints.countOf('7')
        set(value) {
            obstaclePoints =
                "7".repeat(value) + "5".repeat(obstacles5Point) + "4".repeat(obstacles4Point) + "3".repeat(obstacles3Point) + "2".repeat(obstacles2Point) + "1".repeat(obstacles1Point)
        }

    var obstacles5Point: Int
        get() = obstaclePoints.countOf('5')
        set(value) {
            obstaclePoints =
                "7".repeat(obstacles7Point) + "5".repeat(value) + "4".repeat(obstacles4Point) + "3".repeat(obstacles3Point) + "2".repeat(obstacles2Point) + "1".repeat(obstacles1Point)
        }

    var obstacles4Point: Int
        get() = obstaclePoints.countOf('4')
        set(value) {
            obstaclePoints =
                "7".repeat(obstacles7Point) + "5".repeat(obstacles5Point) + "4".repeat(value) + "3".repeat(obstacles3Point) + "2".repeat(obstacles2Point) + "1".repeat(obstacles1Point)
        }

    var obstacles3Point: Int
        get() = obstaclePoints.countOf('3')
        set(value) {
            obstaclePoints =
                "7".repeat(obstacles7Point) + "5".repeat(obstacles5Point) + "4".repeat(obstacles4Point) + "3".repeat(value) + "2".repeat(obstacles2Point) + "1".repeat(obstacles1Point)
        }

    var obstacles2Point: Int
        get() = obstaclePoints.countOf('2')
        set(value) {
            obstaclePoints =
                "7".repeat(obstacles7Point) + "5".repeat(obstacles5Point) + "4".repeat(obstacles4Point) + "3".repeat(obstacles3Point) + "2".repeat(value) + "1".repeat(obstacles1Point)
        }

    var obstacles1Point: Int
        get() = obstaclePoints.countOf('1')
        set(value) {
            obstaclePoints =
                "7".repeat(obstacles7Point) + "5".repeat(obstacles5Point) + "4".repeat(obstacles4Point) + "3".repeat(obstacles3Point) + "2".repeat(obstacles2Point) + "1".repeat(value)
        }

    val obstaclesText: String
        get() {
            var last = 0

            fun range(count: Int): String {
                val from = last + 1
                last += count
                val to = when (last) {
                    21 -> "A"
                    22 -> "B"
                    23 -> "C"
                    24 -> "D"
                    else -> last.toString()
                }
                when (count) {
                    0 -> return "N/A"
                    1 -> return "$from"
                    2 -> return "$from,$to"
                    else -> return "$from-$to"
                }
            }

            return "${range(obstacles5Point)}: Dogwalk or 12 Pole Weave (5 pts)\n" +
                    "${range(obstacles4Point)}: A-Frame (4 pts)\n" +
                    "${range(obstacles3Point)}: See-Saw or 6 Pole Weave (3 pts)\n" +
                    "${range(obstacles2Point)}: Tunnels, Tyre, Spread, Long or Wall (2 pts)\n" +
                    "${range(obstacles1Point)}: Jumps (1 pt)"
        }


    val extendedGamblers: Boolean
        get() = obstaclePoints.length > 20

    data class HeightData(val code: String, val heightName: String, val heightCaption: String, val heightCaptionShort: String, val jumpHeightCaption: String)

    var heightsIdAgilityClass = -1

    val jumpHeightArray: ArrayList<HeightData> = ArrayList<HeightData>()
        get() {
            if (heightsIdAgilityClass != id) {
                heightsIdAgilityClass = id
                field.clear()
                val jumpCodes = heightRunningOrder.split(",")
                val lhoClass =
                    (jumpCodes.size == 2 && (jumpCodes[0] == jumpCodes[1] + "L" || jumpCodes[1] == jumpCodes[0] + "L"))
                for (code in jumpCodes) {
                    val heightName = Height.getHeightName(code)
                    val heightNameShort = Height.getHeightShort(code)
                    val jumpName = Height.getHeightJumpName(code)
                    if (lhoClass) {
                        if (code.endsWith("L")) {
                            field.add(HeightData(code, heightName, "LHO", "LHO", "LHO"))
                        } else {
                            field.add(HeightData(code, heightName, "FH", "FH", "FH"))
                        }
                    } else {
                        field.add(HeightData(code, heightName, heightName, heightNameShort, jumpName))
                    }
                }
            }
            return field
        }

    fun getHeightCaption(code: String, short: Boolean = false): String {
        for (height in jumpHeightArray) {
            if (height.code == code) return if (short) height.heightCaptionShort else height.heightCaption
        }
        return ""
    }

    fun getJumpHeightCaption(code: String): String {
        for (height in jumpHeightArray) {
            if (height.code == code) return height.jumpHeightCaption
        }
        return ""
    }

    val lateEntryCredits: Int
        get() = template.runUnits

    var reason = ""
        private set

    val isCasual: Boolean
        get() = template.oneOf(ClassTemplate.CASUAL_AGILITY, ClassTemplate.CASUAL_JUMPING, ClassTemplate.CASUAL_STEEPLECHASE)

    val isTryout: Boolean
        get() = code.between(ClassTemplate.TRY_OUT.code, ClassTemplate.TRY_OUT_GAMES_GAMBLERS.code)

    val isClosed: Boolean
        get() = progress == CLASS_CLOSED

    val isOpen: Boolean
        get() = progress != CLASS_CLOSED

/*
    val isActive: Boolean
        get() = progress > CLASS_PENDING && progress < CLASS_CLOSED
*/

    val heightRunningOrderText: String
        get() {
            var result = ""
            val codeArray = heightRunningOrder.split(",")
            for (code in codeArray) {
                result = result.append(Height.getHeightName(code))
            }
            return result
        }

    val heightRunningOrderTextShort: String
        get() {
            var result = ""
            val codeArray = heightRunningOrder.split(",")
            for (code in codeArray) {
                result = result.append(Height.getHeightName(code, short = true))
            }
            return result
        }

    val heightRunningOrderTextJump: String
        get() {
            var result = ""
            val codeArray = heightRunningOrder.split(",")
            for (code in codeArray) {
                result = result.append(Height.getHeightJumpName(code))
            }
            return result
        }

    val multipleHeights: Boolean
        get() {
            return heightRunningOrder.contains(",")
        }

    val extendedGroupName: String
        get() {
            return if (template == ClassTemplate.CIRCULAR_KNOCKOUT) {
                "$groupName (${heightRunningOrderText})"
            } else {
                groupName
            }
        }

    val groupName: String
        get() = if (template.groupName.isNotEmpty()) template.groupName else name

    fun isValidCourseTime(aTime: Int, jumpHeightCode: String): Boolean {
        return (aTime >= getMinimumCourseTime(jumpHeightCode)) && (aTime <= getMaximumCourseTime(jumpHeightCode))
    }

    fun getMinimumCourseTime(jumpHeightCode: String): Int {
        if (isSnooker || isTimeOutAndFault || isSnakesAndLadders) {
            return 1000
        }
        val courseTime = getCourseTime(jumpHeightCode)
        return courseTime / 4
    }

    fun getMaximumCourseTime(jumpHeightCode: String): Int {
        val courseTime = getCourseTime(jumpHeightCode)
        return courseTime * 3
    }

    val isAgility: Boolean
        get() = template.isAgility

    val isJumping: Boolean
        get() = template.isJumping

    val isSteeplechase: Boolean
        get() = template.isSteeplechase

    val isGamblers: Boolean
        get() = template.isGamblers

    val discretionaryCourseTime: Boolean
        get() = template.discretionaryCourseTime

    val isSnooker: Boolean
        get() = template.isSnooker

    val isKc: Boolean
        get() = template.isKc

    val isIndependent: Boolean
        get() = template.isIndependent

    val isUka: Boolean
        get() = template.isUka

    val isUkaStyle: Boolean
        get() = template.isUka || template.isUkOpen

    val isFab: Boolean
        get() = template.isFab

    val isIfcs: Boolean
        get() = template.isIfcs

    val isFabStyle: Boolean
        get() = isFab || isIfcs

    val isUkOpen: Boolean
        get() = template.isUkOpen

    val isPowerAndSpeed: Boolean
        get() = template.isPowerAndSpeed

    val isTimeOutAndFault: Boolean
        get() = template.isTimeOutAndFault

    val isSnakesAndLadders: Boolean
        get() = template.isSnakesAndLadders

    val isSpecialParent: Boolean
        get() = template.isSpecialParent

    val canEnterDirectly: Boolean
        get() = template.canEnterDirectly

    val canEnterOnline: Boolean
        get() = template.canEnterOnline

    val isLateEntriesAllowed: Boolean
        get() = template.canEnterDirectly


    val isLocked: Boolean
        get() {
            if (isKc) {
                return false
            } else if (isChild) {
                parentClass.refresh()
                return parentClass.isLocked
            } else {
                return isSpecialParent && !closedForLateEntries
            }
        }

    val canUnlockEntries: Boolean
        get() {
            reason = ""
            if (hasChildren) {
                val children = children()
                while (children.next()) {
                    if (children.progress > CLASS_PREPARING) {
                        reason = "${children.name} is ${children.progressText}"
                        return false
                    }
                }
                return true
            } else {
                if (progress > CLASS_PREPARING) {
                    reason = "$name is $progressText"
                    return false
                }
                return true
            }
        }

    val strictRunningOrder: Boolean
        get() = template.strictRunningOrder || Competition.isGrandFinals || Competition.isUkOpen

    val bookIn: Boolean
        get() = template.bookIn

    val isSpecialEntryParent: Boolean
        get() = template.isSpecialParent && template.hasChildren

/*
    val isSpecialEntrySingleton: Boolean
        get() = template.isSpecialParent && !template.hasChildren && !template.isChild
*/

    val isSpecialEntryChild: Boolean
        get() = template.isSpecialParent && template.isChild

    val isOpenForEntries: Boolean
        get() = progress == CLASS_OPEN_FOR_ENTRIES

    val isScoreBasedGame: Boolean
        get() = template.scoringMethod == SCORING_METHOD_GAMES

    val isExtendedScoreGame: Boolean
        get() = isGamblers || isSnooker || isSnakesAndLadders

    val isPointsBased: Boolean
        get() = template.isPointsBased

    val abbreviatedName: String
        get() {
            var result = name
            result = result.replace("Beginners", "Beg.")
            result = result.replace("Novice", "Nov.")
            result = result.replace("Senior", "Snr.")
            result = result.replace("Steeplechase", "S/Chase")
            return result
        }

    private fun getCourseTimeParameters(): CourseTimeParameters {
        if (isUka) {
            return getCourseTimeParametersUka()
        } else {
            return getCourseTimeParametersKc()
        }


    }

    private fun getCourseTimeParametersKc(): CourseTimeParameters {
        // todo KC getCourseTimeParametersKc
        val heights = heightCodes.replace(";", ",").split(",")
        val isSmall = heights.contains("KC350") ||
                heights.contains("KC350L") ||
                heights.contains("KC450") ||
                heights.contains("KC450L") ||
                heights.contains("KC901") ||
                heights.contains("KC902") ||
                heights.contains("KC903")

        if (isOnRow) {
            if (isAgility) {
                when (template.getCourseTimeCode(gradeCodes)) {
                    "KC01" -> return if (isSmall) CourseTimeParameters(2.25, 2.5) else CourseTimeParameters(2.5, 2.75)
                    "KC02" -> return if (isSmall) CourseTimeParameters(2.25, 2.5) else CourseTimeParameters(2.5, 2.75)
                    "KC03" -> return if (isSmall) CourseTimeParameters(2.5, 2.75) else CourseTimeParameters(2.75, 3.0)
                    "KC04" -> return if (isSmall) CourseTimeParameters(2.75, 2.75) else CourseTimeParameters(3.0, 3.25)
                    "KC05" -> return if (isSmall) CourseTimeParameters(2.75, 2.75) else CourseTimeParameters(3.0, 3.25)
                    "KC06" -> return if (isSmall) CourseTimeParameters(3.0, 3.0) else CourseTimeParameters(3.25, 3.25)
                    "KC07" -> return if (isSmall) CourseTimeParameters(3.0, 3.25) else CourseTimeParameters(3.25, 3.5)
                    "KC90" -> return if (isSmall) CourseTimeParameters(3.25, 3.25) else CourseTimeParameters(3.5, 3.5)
                }
            } else {
                when (template.getCourseTimeCode(gradeCodes)) {
                    "KC01" -> return if (isSmall) CourseTimeParameters(2.25, 2.25) else CourseTimeParameters(2.75, 2.75)
                    "KC02" -> return if (isSmall) CourseTimeParameters(2.5, 2.5) else CourseTimeParameters(3.0, 3.0)
                    "KC03" -> return if (isSmall) CourseTimeParameters(2.75, 2.75) else CourseTimeParameters(3.25, 3.5)
                    "KC04" -> return if (isSmall) CourseTimeParameters(2.75, 3.0) else CourseTimeParameters(3.25, 3.5)
                    "KC05" -> return if (isSmall) CourseTimeParameters(3.0, 3.25) else CourseTimeParameters(3.25, 3.5)
                    "KC06" -> return if (isSmall) CourseTimeParameters(3.0, 3.25) else CourseTimeParameters(3.25, 3.5)
                    "KC07" -> return if (isSmall) CourseTimeParameters(3.0, 3.25) else CourseTimeParameters(3.5, 3.75)
                    "KC90" -> return if (isSmall) CourseTimeParameters(3.25, 3.25) else CourseTimeParameters(3.75, 3.75)
                }
            }
        }
        throw Wobbly("Can not resolve course time parameters")
    }


    private fun getCourseTimeParametersUka(): CourseTimeParameters {
        if (isOnRow) {
            if (isJumping || isSteeplechase || isPowerAndSpeed) {
                when (template.getCourseTimeCode(gradeCodes)) {
                    "UKA01" -> {
                        return CourseTimeParameters(2.75, 3.25, 1.2, 0.0)
                    }
                    "UKA02", "UKA90" -> {
                        return CourseTimeParameters(2.75, 3.25, 1.15, 0.0)
                    }
                    "UKA90X" -> {
                        return CourseTimeParameters(2.75, 3.25, 1.0, 0.0, 2.0)
                    }
                    "UKA03", "UKA04" -> {
                        return CourseTimeParameters(3.25, 3.5, 1.10, 0.0)
                    }
                    "UKA91" -> {
                        return CourseTimeParameters(2.5, 2.9, 1.2, 0.0)
                    }
                    "UKA92" -> {
                        return CourseTimeParameters(2.5, 2.9, 1.2, 3.0)
                    }
                }
            } else {
                val timeCode =
                    if (isGrandPrix || isChallenge) parentClass.template.getCourseTimeCode(gradeCodes)
                    else template.getCourseTimeCode(gradeCodes)
                when (timeCode) {
                    "UKA01" -> {
                        return CourseTimeParameters(2.5, 2.9, 1.2, 0.0)
                    }
                    "UKA02", "UKA90" -> {
                        return CourseTimeParameters(2.5, 2.9, 1.15, 0.0)
                    }
                    "UKA90X" -> {
                        return CourseTimeParameters(2.5, 2.9, 1.0, 0.0, 2.0)
                    }
                    "UKA03", "UKA04" -> {
                        return CourseTimeParameters(2.9, 3.15, 1.10, 0.0)
                    }
                    "UKA91" -> {
                        return CourseTimeParameters(2.5, 2.9, 1.2, 0.0)
                    }
                    "UKA92" -> {
                        return CourseTimeParameters(2.5, 2.9, 1.2, 3.0)
                    }
                }
            }
        }
        throw Wobbly("Can not resolve course time parameters")
    }

    private fun calcRealGroupTime(factor: Double, small: Boolean): Double {
        with(getCourseTimeParameters()) {
            val rateOfTravel = (higherRateOfTravel - lowerRateOfTravel) * (1.0 - factor) + lowerRateOfTravel
            var result = courseLength / rateOfTravel + addedSeconds

            if (isSnooker) {
                result = 60 - 30 * (1.0 - factor)
            }

            if (small) {
                result *= smallUplift
                result += smallAddedSeconds
            }
            return result
        }
    }

    fun calcRealRateOfTravel(factor: Double): Double {
        with(getCourseTimeParameters()) {
            val rateOfTravel = (higherRateOfTravel - lowerRateOfTravel) * (1.0 - factor) + lowerRateOfTravel
            return rateOfTravel
        }
    }

    private fun calcGroupTime(factor: Double, small: Boolean): Int {
        return Math.round(calcRealGroupTime(factor, small)).toInt()
    }

    val groupLargeMax: Int
        get() = calcGroupTime(1.0, false)

    val groupLargeMin: Int
        get() = calcGroupTime(0.0, false)

    fun getGroupLargeMid(factor: Double): Int {
        return calcGroupTime(factor, false)
    }

    val groupSmallMax: Int
        get() = calcGroupTime(1.0, true)

    val groupSmallMin: Int
        get() = calcGroupTime(0.0, true)

    fun getGroupSmallMid(factor: Double): Int {
        return calcGroupTime(factor, true)
    }

    val isAddOnAllowed: Boolean
        get() = isUka && template.isAddOnAllowed

    val isNfcAllowed: Boolean
        get() = template.isNfcAllowed

    val isRelay: Boolean
        get() = template.isRelay

    val teamSize: Int
        get() = template.teamSize

    val isUkaProgression: Boolean
        get() = template.isUkaProgression && gradeCodes.oneOf("UKA01", "UKA02", "UKA03", "UKA04")

    val isKcProgression: Boolean
        get() = template.isKcProgression


    fun isGroupCourseLengthValid(length: Int): Boolean {
        return !template.courseLengthNeeded || (length >= 50 && length <= 499)
    }

    val isGroupCourseLengthValid: Boolean
        get() = isGroupCourseLengthValid(courseLength)

    val isGroupTimeLargeValid: Boolean
        get() = (courseTime >= groupLargeMin * 1000 && courseTime <= groupLargeMax * 1000)

    val isGroupTimeSmallValid: Boolean
        get() = (courseTimeSmall >= groupSmallMin * 1000 && courseTimeSmall <= groupSmallMax * 1000)

    val isValidGroupCourseData: Boolean
        get() {
            reason = ""
            if (isGamblers) {
                if (openingTime == 0 && courseTime == 0 && courseTimeSmall == 0) {
                    return true
                } else if (openingTime > 0 && courseTime > 0 && courseTimeSmall > 0) {
                    return true
                } else {
                    reason = "Gamblers data is incomplete"
                    return false
                }
            } else if (isSnooker) {
                return true
            } else if (isSnakesAndLadders) {
                return true
            } else if (isFabStyle) {
                if (openingTime == 0 && courseTime == 0 && courseTimeSmall == 0) {
                    return true
                } else if (courseTime < 20000) {
                    reason = "Large dog course time is not valid"
                    return false
                } else if (courseTimeSmall < 20000) {
                    reason = "Small dog course time is not valid"
                    return false
                }
                if (courseLength == 0) {
                    reason = "You must enter a distance"
                    return false
                }
            } else if (discretionaryCourseTime) {
                if (courseTime < 20000) {
                    reason = "Course time must be at least 20 seconds"
                    return false
                }
                return true
            } else if (template.noTimeFormula) {
                if (openingTime == 0 && courseTime == 0 && courseTimeSmall == 0) {
                    return true
                } else if (courseTime < 20000) {
                    reason = "Maxi/Std course time is not valid"
                    return false
                } else if (courseTimeSmall < 20000) {
                    reason = "Midi/Toy course time is not valid"
                    return false
                }
                if (courseLength == 0) {
                    reason = "You must enter a distance"
                    return false
                }
            } else if (isKc) {
                if (courseTime < 20000) {
                    reason = "Course time must be at least 20 seconds"
                    return false
                }
                if (courseLength == 0) {
                    reason = "You must enter a distance"
                    return false
                }
                if (isRelay && batonFaults == 0) {
                    reason = "You must enter the number of points for a baton fault"
                    return false
                }
                return true
            } else {
                if (courseLength == 0 && courseTime == 0 && courseTimeSmall == 0) {
                    return true
                } else if (!isGroupCourseLengthValid) {
                    reason = "Course length is not valid"
                    return false
                } else if (!isGroupTimeLargeValid) {
                    reason = "Maxi/Std course time is not valid"
                    return false
                } else if (!isGroupTimeSmallValid) {
                    reason = "Midi/Toy course time is not valid"
                    return false
                } else if (isTimeOutAndFault && !qualifyingPoints.between(17, 20)) {
                    reason =
                        if (qualifyingPoints == 0) "Number of obstacles not selected" else "Number of obstacles is not valid"
                    return false
                } else {
                    return true
                }
            }
            return true
        }

    val isRunning: Boolean
        get() = progress.oneOf(CLASS_RUNNING, CLASS_CLOSED_FOR_LUNCH)

    fun getProgressText(heightCode: String): String {
        var result = classToText(progress)
        if (isRunning && heightRunningOrder.split(",").size > 1) {
            if (isLho(jumpHeightCodes)) {
                result = result + " " + Height.getHeightJumpName(heightCode)
            } else {
                result = result + " " + Height.getHeightName(heightCode)
            }
        }
        return result
    }

    val progressText: String
        get() {
            var result = classToText(progress)
            if (isRunning && heightRunningOrder.split(",").size > 1) {
                ring.refresh()
                if (isLho(jumpHeightCodes)) {
                    result = result + " " + Height.getHeightJumpName(ring.heightCode)
                } else {
                    result = result + " " + Height.getHeightName(ring.heightCode)
                }
            }
            return result
        }

    val progressTextExtended: String
        get() {
            if (progress == CLASS_WALKING) {
                val whenWalking = if (walkingOverLunch) "over lunch" else "now"
                return "Walking $whenWalking and starting at " + startTime.timeText
            } else {
                return progressText
            }
        }

/*
    val heightCount: Int
        get() {
            val heightRunningOrder = heightRunningOrder
            val heightCodes =
                ArrayList(Arrays.asList(*heightRunningOrder.split("\\s*,\\s*".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()))
            return heightCodes.size
        }
*/

    fun getHeightCode(index: Int): String {
        val heightRunningOrder = heightRunningOrder
        val heightCodes =
            ArrayList(Arrays.asList(*heightRunningOrder.split("\\s*,\\s*".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()))
        if (index >= 0 && index < heightCodes.size) {
            return heightCodes[index]
        } else {
            return ""
        }
    }

    fun suggestGroupCourseTimeSmall(): Int {
        if (!isGroupCourseLengthValid || !isGroupTimeLargeValid) {
            return 0
        } else {
            val factor = (courseTime / 1000 - groupLargeMin).toDouble() / (groupLargeMax - groupLargeMin).toDouble()
            val result = (groupSmallMax - groupSmallMin).toDouble() * factor + groupSmallMin.toDouble()
            return (Math.round(result).toInt()) * 1000
        }
    }

    val combinedColumn: Int
        get() = if (groupColumn > 1) groupColumn else template.column

    private fun finalizeClassHeight(jumpHeightCode: String = "", child: AgilityClass? = null, subResultsFlag: Int = 0) {
        dbTransaction {

            if (subResultsFlag > 0) {
                dbExecute("UPDATE entry SET place=0 WHERE idAgilityClass=$id AND jumpHeightCode=${jumpHeightCode.quoted}")
            }

            val maxColumn = child?.template?.impliedMaxColumn ?: 0
            val subResultsFlag1 = if (subResultsFlag != 0) subResultsFlag else 0.setToBit(maxColumn - 1)
            var where = if (child == null && subResultsFlag == 0)
                "agilityClass.idAgilityClass=$id AND entry.hasRun"
            else
                "agilityClass.idAgilityClass=$id AND entry.subResultsFlag & $subResultsFlag1 = $subResultsFlag1"

            if (!jumpHeightCode.isEmpty()) {
                where += " AND entry.jumpHeightCode=${jumpHeightCode.quoted}"
            }
            val orderBy = template.resultsOrderBy
            val entry = Entry()
            entry.agilityClass.joinToParent()
            entry.select(where, orderBy)
            var place = 0
            val preEntries = getPreEntryCountUka(id, jumpHeightCode)

            while (entry.next()) {

                entry.place = if (entry.isEliminated) 1000 else if (entry.isNFC) 2000 else ++place

                val totalFaults = entry.courseFaults.toDouble() + entry.timeFaults.toDouble() / 1000

                val position = entry.place
                var placeFlags = 0
                var progressionPoints = 0

                if (entry.clearRoundOnly || entry.agilityClass.template == ClassTemplate.NURSERY_AGILITY) {
                    if (totalFaults == 0.0) {
                        placeFlags = setBit(placeFlags, PRIZE_ROSETTE_CR)
                    }
                } else {
                    if (entry.courseFaults < FAULTS_ELIMINATED) {
                        if (!entry.qualifying) {
                            if (preEntries.between(0, 5) && position <= 1 ||
                                preEntries.between(6, 10) && position <= 2 ||
                                preEntries.between(11, 15) && position <= 3 ||
                                preEntries > 15 && position <= 4 ||
                                position <= (preEntries + 9) / 10) {
                                placeFlags = setBit(placeFlags, PRIZE_PLACE)
                                placeFlags = setBit(placeFlags, PRIZE_ROSETTE_NQ)
                            }
                        } else {
                            if (preEntries.between(0, 40) && position <= 4 ||
                                preEntries > 40 && position <= (preEntries + 9) / 10) {
                                placeFlags = setBit(placeFlags, PRIZE_PLACE)
                                if (isUkaProgression) {
                                    placeFlags = setBit(placeFlags, PRIZE_ROSETTE)
                                } else {
                                    placeFlags = setBit(placeFlags, PRIZE_ROSETTE_NQ)
                                }
                            } else {
                                placeFlags = setBit(placeFlags, PRIZE_ROSETTE_CR)
                            }
                            if (preEntries.between(6, 49) && position <= 1 ||
                                preEntries.between(50, 100) && position <= 2 ||
                                preEntries > 100 && position <= 3) {
                                placeFlags = setBit(placeFlags, PRIZE_TROPHY)
                            }

                            if (isUkaProgression) {
                                progressionPoints =
                                    entry.agilityClass.calcUkaPoints(maxOf(preEntries, entry.rowCount), position)
                            }
                        }
                    }
                }

                entry.placeFlags = placeFlags
                entry.progressionPoints = progressionPoints
                entry.finalized = true
                entry.post()
            }
        }
    }


    data class ukaAwards(val prizeFlags: Int, val progressionPoints: Int)

    private fun getUkaAwards(preEntries: Int, totalRunners: Int, position: Int, clearRoundOnly: Boolean, courseFaults: Int, timeFaults: Int, qualifying: Boolean): ukaAwards {
        var prizeFlags = 0
        var progressionPoints = 0
        if (clearRoundOnly || template == ClassTemplate.NURSERY_AGILITY) {
            if (courseFaults == 0 && timeFaults == 0) {
                prizeFlags = setBit(prizeFlags, PRIZE_ROSETTE_CR)
            }
        } else {
            if (courseFaults < FAULTS_ELIMINATED) {
                if (!qualifying) {
                    if (preEntries.between(0, 5) && position <= 1 ||
                        preEntries.between(6, 10) && position <= 2 ||
                        preEntries.between(11, 15) && position <= 3 ||
                        preEntries > 15 && position <= 4 ||
                        position <= (preEntries + 9) / 10) {
                        prizeFlags = setBit(prizeFlags, PRIZE_PLACE)
                        prizeFlags = setBit(prizeFlags, PRIZE_ROSETTE_NQ)
                    }
                } else {
                    if (preEntries.between(0, 40) && position <= 4 ||
                        preEntries > 40 && position <= (preEntries + 9) / 10) {
                        prizeFlags = setBit(prizeFlags, PRIZE_PLACE)
                        if (isUkaProgression) {
                            prizeFlags = setBit(prizeFlags, PRIZE_ROSETTE)
                        } else {
                            prizeFlags = setBit(prizeFlags, PRIZE_ROSETTE_NQ)
                        }
                    } else {
                        prizeFlags = setBit(prizeFlags, PRIZE_ROSETTE_CR)
                    }
                    if (preEntries.between(6, 49) && position <= 1 ||
                        preEntries.between(50, 100) && position <= 2 ||
                        preEntries > 100 && position <= 3) {
                        prizeFlags = setBit(prizeFlags, PRIZE_TROPHY)
                    }

                    if (isUkaProgression) {
                        progressionPoints = calcUkaPoints(maxOf(preEntries, totalRunners), position)
                    }
                }
            }
        }

        return ukaAwards(prizeFlags, progressionPoints)

    }

    private fun finalizeSubClass(subClass: Int) {
        dbExecute("UPDATE entry SET place=0, placeFlags=0 WHERE idAgilityClass=$id AND entry.subClass=$subClass")
        val where = "agilityClass.idAgilityClass=$id AND entry.subClass=$subClass AND entry.hasRun"
        val orderBy = template.resultsOrderBy
        val entry = Entry()
        entry.agilityClass.joinToParent()
        entry.agilityClass.competition.joinToParent()
        entry.select(where, orderBy)
        var place = 0
        val totalRunners = entry.rowCount

        while (entry.next()) {

            entry.place = ++place
            if (isUka) {
                val preEntries = getPreEntryCountUka(id, entry.jumpHeightCode)
                val awards =
                    getUkaAwards(preEntries, totalRunners, entry.place, entry.clearRoundOnly, entry.courseFaults, entry.timeFaults, entry.qualifying)
                entry.placeFlags = awards.prizeFlags
                entry.progressionPoints = awards.progressionPoints
            } else if (isFab || isIfcs) {
                val actualRunners = getSubClassRunners(id, subClass)
                val rosettes = (actualRunners + 9) / 10
                var prizeFlags = 0
                val clear = entry.courseFaults == 0 && entry.timeFaults == 0
                if (clear && entry.place <= rosettes && !entry.isEliminated) {
                    prizeFlags = setBit(prizeFlags, PRIZE_ROSETTE)
                } else if (clear) {
                    if (template.clearRoundRosettes) prizeFlags = setBit(prizeFlags, PRIZE_ROSETTE_CR)
                }
                entry.placeFlags = prizeFlags
                entry.progressionPoints = 0
            } else if (isIndependent) {
                val clear = entry.courseFaults == 0 && entry.timeFaults == 0
                if (entry.clearRoundOnly) {
                    if (clear) {
                        entry.placeFlags = setBit(0, PRIZE_ROSETTE_CR)
                    }
                } else if (!entry.isEliminated) {
                    val preEntries = getPreEntryCount(id, subClass)
                    val rosettes =
                        if (entry.agilityClass.haveAwards) getSubClassRosettes(subClass) else AwardRule(entry.agilityClass.competition.rosetteRule).getUnits(preEntries)
                    val trophies =
                        if (entry.agilityClass.haveAwards) getSubClassTrophies(subClass) else AwardRule(entry.agilityClass.competition.trophyRule).getUnits(preEntries)
                    val awards =
                        if (entry.agilityClass.haveAwards) getSubClassAwards(subClass) else AwardRule(entry.agilityClass.competition.awardRule).getUnits(preEntries)
                    var prizeFlags = 0
                    if (entry.place <= rosettes && !entry.isEliminated) {
                        prizeFlags = setBit(prizeFlags, PRIZE_ROSETTE)
                    } else if (clear) {
                        if (template.clearRoundRosettes) prizeFlags = setBit(prizeFlags, PRIZE_ROSETTE_CR)
                    }
                    if (entry.place <= trophies) {
                        prizeFlags = setBit(prizeFlags, PRIZE_TROPHY)
                    } else if (entry.place <= awards) {
                        prizeFlags = setBit(prizeFlags, PRIZE_AWARD)
                    }
                    entry.placeFlags = prizeFlags
                }
            } else {
                val clear = entry.courseFaults == 0 && entry.timeFaults == 0
                if (!entry.isEliminated) {
                    val preEntries = getPreEntryCount(id, subClass)
                    val rosettes =
                        if (entry.agilityClass.haveAwards) getSubClassRosettes(subClass) else AwardRule(entry.agilityClass.competition.rosetteRule).getUnits(preEntries)
                    val trophies =
                        if (entry.agilityClass.haveAwards) getSubClassTrophies(subClass) else AwardRule(entry.agilityClass.competition.trophyRule).getUnits(preEntries)
                    val awards =
                        /* if (entry.agilityClass.haveAwards) getSubClassAwards(subClass) else */ AwardRule(entry.agilityClass.competition.awardRule).getUnits(preEntries)
                    var prizeFlags = 0
                    var warrantPoints = 0
                    if (entry.place <= rosettes && !entry.isEliminated) {
                        prizeFlags = setBit(prizeFlags, PRIZE_ROSETTE)
                        if (clear && (template == ClassTemplate.KC_AGILITY || template == ClassTemplate.KC_JUMPING)) {
                            if (place <= 10) {
                                warrantPoints = if (template == ClassTemplate.KC_AGILITY) 21 - place else 11 - place
                            } else {
                                warrantPoints = if (template == ClassTemplate.KC_AGILITY) 2 else 1
                            }
                        }
                    } else if (clear) {
                        if (template.clearRoundRosettes) prizeFlags = setBit(prizeFlags, PRIZE_ROSETTE_CR)
                        warrantPoints =
                            if (template == ClassTemplate.KC_AGILITY) 2 else if (template == ClassTemplate.KC_JUMPING) 1 else 0
                    }
                    if (entry.place <= trophies) {
                        prizeFlags = setBit(prizeFlags, PRIZE_TROPHY)
                    } else if (entry.place <= awards) {
                        prizeFlags = setBit(prizeFlags, PRIZE_AWARD)
                    }
                    entry.placeFlags = prizeFlags
                    entry.progressionPoints = warrantPoints
                }

            }
            entry.finalized = true
            entry.post()
            if (template.getParentPoints != null) {
                if (idAgilityClassParent > 0 && entry.hasRun) {
                    entry.UpdateParentSubResult(idAgilityClassParent, this)
                }
            }
        }
    }

    fun getCourseTime(jumpHeightCode: String): Int {
        val result: Int
        if (isUkOpen && heights.searchElement("heightCode", jumpHeightCode, create = false)["courseTime"].asInt > 0) {
            result = heights.searchElement("heightCode", jumpHeightCode, create = false)["courseTime"].asInt
        } else if (isUka && jumpHeightCode.oneOf("UKA200", "UKA300", "UKA400") && !discretionaryCourseTime) {
            result = courseTimeSmall
        } else if (isUkOpen && jumpHeightCode.oneOf("OP300", "OP400") && !discretionaryCourseTime) {
            result = courseTimeSmall
        } else if (isFab && jumpHeightCode.oneOf("FAB200", "FAB300", "FAB400") && !discretionaryCourseTime) {
            result = courseTimeSmall
        } else if (isIfcs && jumpHeightCode.oneOf("IF300", "IF400") && !discretionaryCourseTime) {
            result = courseTimeSmall
        } else {
            result = courseTime
        }
        return result
    }

    fun getSubClassCourseTime(subClass: Int): Int {
        return subClasses["$subClass.courseTime"].asInt
    }

    fun setSubClassCourseTime(subClass: Int, courseTime: Int) {
        subClasses["$subClass.courseTime"].setValue(courseTime)
    }

    fun getSubClassRosettes(subClass: Int): Int {
        return subClasses["$subClass.rosettes"].asInt
    }

    fun setSubClassRosettes(subClass: Int, rosettes: Int) {
        subClasses["$subClass.rosettes"].setValue(rosettes)
    }

    fun getSubClassTrophies(subClass: Int): Int {
        return subClasses["$subClass.trophies"].asInt
    }

    fun setSubClassTrophies(subClass: Int, trophies: Int) {
        subClasses["$subClass.trophies"].setValue(trophies)
    }

    fun getSubClassAwards(subClass: Int): Int {
        return subClasses["$subClass.awards"].asInt
    }

/*
    fun setSubClassAwards(subClass: Int, awards: Int) {
        subClasses["$subClass.awards"].setValue(awards)
    }
*/

    fun calcUkaPoints(classSize: Int, position: Int): Int {
        if (!gradeCodes.oneOf("UKA01", "UKA02", "UKA03", "UKA04")) {
            return 0
        }
        if (classSize <= 10) {
            when (position) {
                1 -> return 6
                2 -> return 4
                3 -> return 3
                4 -> return 2
                else -> return 2
            }
        } else if (classSize <= 100) {
            when (position) {
                1 -> return 12
                2 -> return 8
                3 -> return 6
                4 -> return 4
                else -> return 2
            }
        } else {
            when (position) {
                1 -> return 12
                2 -> return 11
                3 -> return 10
                4 -> return 9
                5 -> return 8
                6 -> return 7
                7 -> return 6
                8 -> return 5
                9 -> return 4
                10 -> return 3
                else -> return 2
            }
        }
    }

    val competitionNameDate: String
        get() = "%s - %s".format(competition.name, date.fullDate())

    fun stillToRun(group: String): String {
        val progressFrom = if (competition.hasBookingIn && bookIn) PROGRESS_BOOKED_IN else PROGRESS_ENTERED
        val groupWhere = if (group.isNotEmpty()) " AND entry.group = ${group.quoted}" else ""
        val sql = """
                SELECT
                    height.name AS height,
                    SUM(IF(progress BETWEEN $progressFrom AND $PROGRESS_RUNNING, 1, 0)) AS remaining
                FROM
                    height
                        LEFT JOIN
                    entry ON entry.idAgilityClass = $id
                        AND entry.jumpHeightCode = height.heightCode
                        JOIN
                    agilityClass ON agilityClass.idAgilityClass = $id
                WHERE
                    FIND_IN_SET(height.heightCode, agilityClass.heightRunningOrder) > 0 $groupWhere
                GROUP BY FIND_IN_SET(entry.jumpHeightCode,
                        agilityClass.heightRunningOrder)
            """
        val query = DbQuery(sql)
        val result = CommaList()
        while (query.next()) {
            result.add(query.getString("height") + ": " + Integer.toString(query.getInt("remaining")))
        }
        return result.toString()
    }


    fun waitingFor(group: String): String {
        val progressList =
            if (Competition.hasBookingIn && bookIn) "($PROGRESS_BOOKED_IN)" else "($PROGRESS_ENTERED, $PROGRESS_BOOKED_IN)"
        val groupWhere = if (group.isNotEmpty()) " AND entry.group = ${group.quoted}" else ""
        val sql = """
            SELECT
                height.name AS height,
                SUM(IF(progress IN $progressList, 1, 0)) AS remaining
            FROM
                height
                    LEFT JOIN
                entry ON entry.idAgilityClass = $id
                    AND entry.jumpHeightCode = height.heightCode
                    JOIN
                agilityClass ON agilityClass.idAgilityClass = $id
            WHERE
                FIND_IN_SET(height.heightCode, agilityClass.heightRunningOrder) > 0 $groupWhere
            GROUP BY FIND_IN_SET(entry.jumpHeightCode,
                    agilityClass.heightRunningOrder)
        """
        val query = DbQuery(sql)
        val result = CommaList()
        while (query.next()) {
            result.add(query.getString("height") + ": " + Integer.toString(query.getInt("remaining")))
        }
        return result.toString()
    }

    val lastHeightCode: String
        get() = heightRunningOrder.split(",").last()

    val lastRunnerJumpHeightCode: String
        get() {
            val sql =
                "SELECT jumpHeightCode FROM entry WHERE idAgilityClass=%d AND progress=%d order by runEnd desc limit 1"
            val query = DbQuery(sql, id, PROGRESS_RUN)
            if (query.found()) {
                return query.getString("jumpHeightCode")
            } else {
                return getHeightCode(0)
            }
        }

    val lastRunnerSubClass: Int
        get() {
            val sql = "SELECT subClass FROM entry WHERE idAgilityClass=%d AND progress=%d order by runEnd desc limit 1"
            val query = DbQuery(sql, id, PROGRESS_RUN)
            if (query.found()) {
                return query.getInt("subClass")
            } else {
                return 0
            }
        }

    fun hasCourseTime(): Boolean {
        if (isUkaStyle || isFabStyle) {
            return (courseLength != 0 || !template.courseLengthNeeded) &&
                    (courseTime != 0 || !template.courseTimeNeeded) &&
                    (courseTimeSmall != 0 || !template.courseTimeNeeded || discretionaryCourseTime)
        } else {
            return (courseLength != 0 || !template.courseLengthNeeded) &&
                    (courseTime != 0 || !template.courseTimeNeeded)
        }
    }

    val isChild: Boolean
        get() = idAgilityClassParent > 0

    val hasChildren: Boolean
        get() = template.hasChildren

    val isHarvested: Boolean
        get() = template.isHarvested

    val hasNext: Boolean
        get() = template.next != null

    val isSeries: Boolean
        get() = template.isSeries

    var _parentClass: AgilityClass? = null
    val parentClass: AgilityClass
        get() {
            mandate(isChild, "Class does not have a parent in agilityClass.parentClass")
            if (idAgilityClassParent != 0) {
                val parent = _parentClass ?: AgilityClass()
                _parentClass = parent
                if (parent.isOnRow && parent.id == idAgilityClassParent) {
                    return parent
                }
                parent.find(idAgilityClassParent)
                if (parent.found()) {
                    return parent
                }
            }
            throw Wobbly("agilityClass.parentClass does not exist")
        }

    fun updateRing() {
        val ring = Ring()
        ring.select("idCompetition=$idCompetition AND date=${date.sqlDate} AND ringNumber=$ringNumber")
        if (ring.found() && ring.idAgilityClass == id) {
            ring.selectFirstOpenAgilityClass()
        }
    }

    fun moveToRing(ringNumber: Int): Boolean {

        val oldRingNumber = this.ringNumber

        if (progress <= CLASS_PREPARING) {
            val sql = """
                SELECT ringOrder, classProgress
                FROM agilityClass WHERE idCompetition=$idCompetition AND classDate=${date.sqlDate} AND ringNumber=$ringNumber
                ORDER BY ringOrder DESC
                LIMIT 1
            """
            val query = DbQuery(sql)
            if (query.found()) {
                this.ringNumber = ringNumber
                this.ringOrder = query.getInt("ringOrder") + 1
                this.progress = CLASS_PENDING
                this.post()

                val ring = Ring()
                if (query.getInt("classProgress") == CLASS_CLOSED) {
                    // last class is closed so make this the current class
                    ring.select(idCompetition, date, ringNumber)
                    ring.chooseAgilityClass(id)
                }

                //
                ring.select(idCompetition, date, oldRingNumber)
                ring.checkActiveClass()


                return true
            }
        }
        return false
    }

    fun enter(
        idTeam: Int,
        heightCode: String? = null,
        entryType: Int? = null,
        timeEntered: Date? = null,
        clearRoundOnly: Boolean? = null,
        idUka: Int? = null,
        idAccount: Int? = null,
        fee: Int? = null,
        runningOrder: Int? = null,
        progress: Int? = null,
        teamMember: Int = 1,
        combinedColumn: Int? = null,
        gradeCode: String? = null,
        dogRingNumber: Int? = null,
        jumpHeightCode: String? = null,
        group: String? = null,
        enterChildClasses: Boolean = false,
        invite: Boolean = false,
        subDivision: Int? = null,
        grandFinals: Boolean = false,
        runsEntered: Int = 0
    ): Int {
        val entry = Entry()
        dbTransaction {
            if (!entry.seekEntry(id, idTeam, teamMember)) {
                entry.append()
                entry.idAgilityClass = id
                entry.idTeam = idTeam
                entry.teamMember = teamMember
                entry.clearRoundOnly = false
                entry.idUka = 0
                entry.fee = 0
                entry.runningOrder = 0
                entry.progress = PROGRESS_ENTERED
                entry.combinedColumn = 0
                entry.queueSequence = 0
                entry.scoreCodes = ""
                entry.courseFaults = 0
                entry.time = 0
                entry.timeFaults = 0
                entry.place = 0
                entry.runEnd = nullDate
                entry.timeEntered = timeEntered ?: entry.timeEntered
            }
            entry.heightCode = heightCode ?: entry.heightCode
            if (heightCode != null && entry.heightCodeEntered.isEmpty()) {
                entry.heightCodeEntered = heightCode
            }
            entry.clearRoundOnly = clearRoundOnly ?: entry.clearRoundOnly
            entry.type = entryType ?: entry.type
            if (runsEntered > 0) entry.runsEntered = runsEntered
            entry.idUka = idUka ?: entry.idUka
            entry.idAccount = idAccount ?: entry.idAccount
            entry.fee = fee ?: entry.fee
            entry.progress = progress ?: entry.progress
            entry.combinedColumn = combinedColumn ?: entry.combinedColumn
            if (groupRunningOrder.isNotEmpty()) {
                entry.group = group ?: entry.group
            }
            if (entryType == ENTRY_LATE_CREDITS) {
                entry.lateEntryCredits = template.runUnits
            }
            entry.gradeCode = gradeCode ?: entry.gradeCode
            val jumpHeightCode1 = entry.jumpHeightCode
            entry.jumpHeightCode =
                jumpHeightCode ?: if (entry.jumpHeightCode.isNotEmpty()) entry.jumpHeightCode else entry.heightCode
            entry.subDivision = subDivision ?: entry.subDivision
            val jumpHeightCode2 = entry.jumpHeightCode
            entry.subClass = chooseSubClass(entry.gradeCode, entry.heightCode, entry.jumpHeightCode, entry.subDivision)
            if (runningOrder != null) {
                if (runningOrder == -1) {
                    if (entry.runningOrderJumpHeightCode != entry.runningOrderJumpHeightCode || entry.runningOrder == 0 || jumpHeightCode1 != jumpHeightCode2) {
                        entry.runningOrder = nextRunningOrder(entry.jumpHeightCode)
                    }
                } else {
                    entry.runningOrder = runningOrder

                }
                entry.runningOrderJumpHeightCode = entry.jumpHeightCode
            }

            entry.flag = false
            entry.dogRingNumber = dogRingNumber ?: entry.dogRingNumber
            entry.runUnits = template.runUnits
            if (grandFinals) {
                if (invite) {
                    entry.invited = true
                    entry.uninvited = false
                    entry.cancelled = false
                } else {
                    entry.entered = true
                }
            }
            entry.post()

            if (enterChildClasses && hasChildren) {
                val child = children()
                while (child.next()) {
                    if (!child.template.isCut) {
                        child.enter(
                            idTeam, heightCode, ENTRY_DEPENDENT_CLASS, timeEntered, clearRoundOnly, idUka, idAccount,
                            fee, if (runningOrder == -1) -1 else null, progress, teamMember, combinedColumn, gradeCode,
                            dogRingNumber, jumpHeightCode, group, enterChildClasses, invite, subDivision
                        )
                    }
                }
            }


        }
        return entry.id
    }

/*
    fun getIdEntryForDog(idDog: Int): Int {
        val query =
            DbQuery("SELECT idEntry FROM entry JOIN team USING (idTeam) WHERE entry.idAgilityClass=$id AND team.idDog=$idDog")
        if (query.found()) {
            return query.getInt("idEntry")
        } else {
            return -1
        }
    }
*/

/*
    fun getIdEntryForTeam(idTeam: Int): Int {
        val query = DbQuery("SELECT idEntry FROM entry WHERE idAgilityClass=$id AND idTeam=$idTeam")
        if (query.found()) {
            return query.getInt("idEntry")
        } else {
            return -1
        }
    }
*/

    fun relatedClass(template: ClassTemplate): AgilityClass {
        if (isKc) {
            return AgilityClass.select("idCompetition=$idCompetition AND classCode = ${template.code} AND heightCodes=${heightCodes.quoted}")
        } else {
            return AgilityClass.select("idCompetition=$idCompetition AND classCode = ${template.code}")
        }
    }

    fun childClasses(): AgilityClass {
        val related = AgilityClass()
        related.select("idCompetition=$idCompetition AND idAgilityClassParent=$id AND classCode IN (${ClassTemplate.codeList(template.children)})")
        if (related.rowCount == template.children.size) {
            return related
        }
        throw Wobbly("some of the child classes are missing in agilityClass.childClasses")
        // todo add in missing agilityClass children
    }

    fun children(): AgilityClass {
        if (hasChildren) {
            return childClasses()
        } else {
            return AgilityClass()
        }
    }

    fun runnableClasses(): AgilityClass {
        if (hasChildren) {
            return childClasses()
        } else {
            val result = AgilityClass()
            result.find(id)
            return result
        }
    }

    fun nextRunningOrder(jumpHeightCode: String): Int {
        val query =
            DbQuery("SELECT MAX(runningOrder) AS max FROM Entry WHERE idAgilityClass=$id AND jumpHeightCode=${jumpHeightCode.quoted} AND entry.progress<$PROGRESS_DELETED_LOW")
        query.first()
        val max = query.getInt("max")
        return max + 1
    }

    fun prepareEntries() {
        if (template.isRelay && isUka) {
            fixRelayHeightCodes()
        }
        val entry = Entry()
        var jumpHeightCode = ""
        var lastRunningOrder = 0
        entry.select("entry.idAgilityClass=$id AND entry.progress<=$PROGRESS_WITHDRAWN", "jumpHeightCode, ${template.runningOrderSort}")
        while (entry.next()) {
            if ((entry.progress == PROGRESS_WITHDRAWN && !template.feedWithdrawn)) {
                entry.runningOrder = template.runningOrderStart - 1
                entry.post()
            } else {
                if (entry.jumpHeightCode != jumpHeightCode) {
                    jumpHeightCode = entry.jumpHeightCode
                    if (isKc && lastRunningOrder > 0) {
                        lastRunningOrder += 3
                    } else {
                        lastRunningOrder = 0
                    }
                }
                if (entry.progress == PROGRESS_ENTERED && !bookIn) {
                    entry.progress = PROGRESS_BOOKED_IN
                }
                entry.runningOrder = ++lastRunningOrder
                entry.runningOrderJumpHeightCode = jumpHeightCode
                entry.post()
            }
        }
        runningOrdersGenerated = true
        post()
    }

    fun prepareRunningOrders() {
        if (template.runningOrderSort != "wao") {
            val heightMonitor = ChangeMonitor<String>("?")
            var lastRunningOrder = 0
            Entry().join { agilityClass }
                .where("entry.idAgilityClass=$id AND progress=$PROGRESS_ENTERED", "FIND_IN_SET(entry.jumpHeightCode, agilityClass.heightRunningOrder), entry.jumpHeightCode, ${template.runningOrderSort}") {
                    heightMonitor.whenChange(jumpHeightCode) {
                        if (isKc && lastRunningOrder > 0) {
                            lastRunningOrder += 3
                        } else {
                            lastRunningOrder = template.runningOrderStart - 1
                        }
                    }
                    runningOrder = ++lastRunningOrder
                    runningOrderJumpHeightCode = jumpHeightCode
                    post()
                }
            runningOrdersGenerated = true
            post()
        }
    }

    fun fixRelayHeightCodes() {
        val entry = Entry()
        entry.select("entry.idAgilityClass=$id")
        while (entry.next()) {
            entry.heightCode = entry.team.relayHeightCode
            entry.jumpHeightCode = entry.team.relayHeightCode
            entry.post()
        }
    }

    fun nextClass(): AgilityClass {
        val next = template.next
        if (next != null) {
            return relatedClass(next)
        } else {
            val a = AgilityClass()
            a.select("1=2")

            return a
        }
    }

    fun prepareEntriesFromFeeder(feederClass: AgilityClass) {
        val entry = Entry()
        entry.join { agilityClass }
        var jumpHeightCode = ""
        var lastRunningOrder = 0
        var places = 0
        var block = 0
        var entries = 0
        var wao = false
        val entriesMap = HashMap<String, Int>()

        if (feederClass.template.isRelay && isUka) {
            feederClass.fixRelayHeightCodes()
        }

        var extra = if (isUkOpen && part.isNotEmpty()) " AND entry.group=${part.quoted}" else ""

        if (template.cut_columns > 0) {
            var subResultsFlag = 0
            (0..template.cut_columns - 1).forEach { i -> subResultsFlag = subResultsFlag.setBit(i) }
            extra += " AND entry.subResultsFlag & $subResultsFlag = $subResultsFlag"
        }

        if (template.runningOrderSort == "wao") {
            wao = true
            val query =
                DbQuery("SELECT jumpHeightCode, count(*) as entries FROM entry WHERE idAgilityClass=${feederClass.id} AND entry.progress<=$PROGRESS_WITHDRAWN GROUP BY jumpHeightCode")
            while (query.next()) {
                entriesMap[query.getString("jumpHeightCode")] = query.getInt("entries")
            }
            entry.select("entry.idAgilityClass=${feederClass.id} AND entry.progress<=$PROGRESS_WITHDRAWN $extra", "jumpHeightCode, runningOrder")
        } else if (isKc) {
            entry.select("entry.idAgilityClass=${feederClass.id} AND entry.progress<=$PROGRESS_WITHDRAWN $extra", template.runningOrderSort)
        } else {
            entry.select("entry.idAgilityClass=${feederClass.id} AND entry.progress<=$PROGRESS_WITHDRAWN $extra", "jumpHeightCode, ${template.runningOrderSort}")
        }
        while (entry.next()) {
            var withdrawn =
                (entry.progress == PROGRESS_WITHDRAWN) || (feederClass.template != template.parent && !entry.hasRun)
            if (template == ClassTemplate.TRY_OUT_PENTATHLON_SPEEDSTAKES && entry.subResultsFlag != 15) withdrawn = true
            if (!withdrawn || template.feedWithdrawn) {
                if (entry.jumpHeightCode != jumpHeightCode) {
                    jumpHeightCode = entry.jumpHeightCode
                    if (template.oneOf(ClassTemplate.KC_GRAMPION_FINAL)) {
                        doNothing()
                    } else if (isKc && lastRunningOrder > 0) {
                        lastRunningOrder += 3
                    } else {
                        lastRunningOrder = template.runningOrderStart - 1
                    }
                    if (wao) {
                        entries = entriesMap[jumpHeightCode] ?: 0
                        block = entries / 4
                    }
                    if (template.isCut) {

                        val entryCount = when (template.cutPercentOf) {
                            "entered" -> if (isKc) getEntryCount(feederClass.id) else getEntryCount(feederClass.id, jumpHeightCode)
                            else -> getPlaceCount(feederClass.id, jumpHeightCode)
                        }
                        places = template.cutSize(entryCount, jumpHeightCode)

                    }
                }

                if (places == 0 || (entry.place.between(1, places) && !entry.isEffectivelyEliminated && entry.hasQualifyingScore)) {

                    val proposedProgress =
                        when {
                            entry.progress >= PROGRESS_WITHDRAWN ->
                                entry.progress
                            feederClass.template != template.parent && !entry.hasRun ->
                                PROGRESS_WITHDRAWN
                            !feederClass.bookIn ->
                                PROGRESS_BOOKED_IN
                            else ->
                                PROGRESS_ENTERED
                        }

                    var runningOrder = ++lastRunningOrder

                    if (wao) {
                        runningOrder =
                            if (runningOrder > block)
                                runningOrder - block
                            else
                                runningOrder - block + entries
                    }

                    if (template == ClassTemplate.TEAM_INDIVIDUAL) {
                        enter(
                            idTeam = entry.idTeam,
                            idAccount = entry.idAccount,
                            heightCode = entry.team.getHeightCode(1),
                            jumpHeightCode = entry.team.getHeightCode(1),
                            subDivision = entry.subDivision,
                            entryType = ENTRY_DEPENDENT_CLASS,
                            timeEntered = entry.timeEntered,
                            runningOrder = runningOrder,
                            progress = if (entry.progress >= PROGRESS_WITHDRAWN) entry.progress else proposedProgress,
                            teamMember = 1,
                            combinedColumn = 1
                        )
                        enter(
                            idTeam = entry.idTeam,
                            idAccount = entry.idAccount,
                            heightCode = entry.team.getHeightCode(2),
                            jumpHeightCode = entry.team.getHeightCode(2),
                            subDivision = entry.subDivision,
                            entryType = ENTRY_DEPENDENT_CLASS,
                            timeEntered = entry.timeEntered,
                            runningOrder = runningOrder,
                            progress = if (entry.progress >= PROGRESS_WITHDRAWN) entry.progress else proposedProgress,
                            teamMember = 2,
                            combinedColumn = 2
                        )
                        enter(
                            idTeam = entry.idTeam,
                            idAccount = entry.idAccount,
                            heightCode = entry.team.getHeightCode(3),
                            jumpHeightCode = entry.team.getHeightCode(3),
                            subDivision = entry.subDivision,
                            entryType = ENTRY_DEPENDENT_CLASS,
                            timeEntered = entry.timeEntered,
                            runningOrder = runningOrder,
                            progress = if (entry.progress >= PROGRESS_WITHDRAWN) entry.progress else proposedProgress,
                            teamMember = 3,
                            combinedColumn = 3
                        )

                    } else {

                        enter(
                            idTeam = entry.idTeam,
                            idAccount = entry.idAccount,
                            gradeCode = entry.gradeCode,
                            heightCode = entry.heightCode,
                            jumpHeightCode = entry.jumpHeightCode,
                            subDivision = entry.subDivision,
                            entryType = ENTRY_DEPENDENT_CLASS,
                            timeEntered = entry.timeEntered,
                            runningOrder = runningOrder,
                            progress = if (entry.progress >= PROGRESS_WITHDRAWN) entry.progress else proposedProgress,
                            group = entry.group

                        )
                    }

                }
            }
            if (feederClass.template == ClassTemplate.TEAM && entry.progress == PROGRESS_WITHDRAWN) {
                val individual = Entry.select("idAgilityClass=$id AND idTeam=${entry.idTeam}")
                while (individual.next()) {
                    individual.progress = PROGRESS_WITHDRAWN
                    individual.post()
                }
            }
        }
        if (template == ClassTemplate.TEAM_INDIVIDUAL) {
            prepareEntries()
        }
        runningOrdersGenerated = true
        post()
    }

    fun prepareClass(print: Boolean = true) {
        val feeder = template.feeder
        if (feeder != null) {
            val feederClass = relatedClass(feeder)
            mandate(feederClass.found(), "Unable to locate feeder class")
            prepareEntriesFromFeeder(feederClass)
        } else {
            prepareEntries()
        }
        if (print) Reports.printRunningOrders(id, copies = Global.runningOrderCopies)
        readyToRun = true
        post()
    }

    fun prepareRelatedClasses(classes: AgilityClass, print: Boolean) {
        while (classes.next()) {
            if (!classes.isClosed) {
                classes.prepareClass(print)
            }
        }
    }

    fun duplicateEntries(target: AgilityClass) {
        Entry().where("idAgilityClass=$id") {
            target.enter(
                idTeam = idTeam,
                heightCode = heightCode,
                jumpHeightCode = jumpHeightCode,
                subDivision = subDivision,
                entryType = ENTRY_DEPENDENT_CLASS,
                timeEntered = timeEntered,
                idAccount = idAccount,
                fee = fee,
                teamMember = teamMember,
                gradeCode = gradeCode,
                group = group
            )
        }
    }

    fun populateChildren(recurse: Boolean = true) {
        if (hasChildren) {
            val child = children()
            while (child.next()) {
                if (!child.template.isCut) {
                    duplicateEntries(child)
                    if (recurse) child.populateChildren()
                }
            }
        }
    }

    fun entriesClosed(print: Boolean = true) {
        if (template == ClassTemplate.TRY_OUT) {
            dbTransaction {
                closedForLateEntries = true
                post()
                val child = children()
                while (child.next()) {
                    child.entriesClosed(print)
                }
            }
        } else if (isUkOpen) {
            dbTransaction {
                closedForLateEntries = true
                post()
                if (hasNext && !isSeries) {
                    prepareRelatedClasses(nextClass(), print)
                } else if (hasChildren) {
                    prepareRelatedClasses(children(), print)
                    val child = children()
                    while (child.next()) {
                        child.entriesClosed(print)
                    }
                } else {
                    prepareClass(print)
                }
            }
        } else {
            dbTransaction {
                closedForLateEntries = true
                post()
                if (hasNext && !isSeries) {
                    prepareRelatedClasses(nextClass(), print)
                } else if (hasChildren) {
                    prepareRelatedClasses(children(), print)
                } else {
                    prepareClass(print)
                }
            }
        }
    }

    fun entriesReopened() {
        dbTransaction {
            progress = CLASS_PENDING
            closedForLateEntries = false
            post()
        }
    }

    fun removeNeedRequests() {
        val competitor = Competitor()
        competitor.find("neededRingNumber=$ringNumber")
        while (competitor.next()) {
            competitor.neededRingTime = nullDate
            competitor.neededRingNumber = 0
            competitor.post()
        }
    }


    fun checkScoreCodes() {
        val entry = Entry()

        entry.select("entry.idAgilityClass=$id")
        while (entry.next()) {
            if (hasChildren || isHarvested) {
                entry.combineSubResults()
                entry.post()
            } else {
                if (entry.progress == PROGRESS_RUN) {
                    entry.updateScoreData()
                    entry.post()
                }
            }
            if (idAgilityClassParent > 0 && entry.progress < PROGRESS_DELETED_LOW) {
                entry.UpdateParentSubResult(idAgilityClassParent, this)
            }
        }
        if (idAgilityClassParent > 0 && template.column >= 0) {
            val parent = AgilityClass()
            parent.find(idAgilityClassParent)
            parent.finalizeClass(child = this)
        }
    }

    fun finalizeClass(child: AgilityClass? = null, subResultsFlag: Int = 0) {
        checkScoreCodes()
        if (isUka || isUkOpen) {
            if (combineHeights) {
                finalizeClassHeight(child = child)
            } else {
                val heightArray = heightRunningOrder.split(",")
                for (code in heightArray) {
                    finalizeClassHeight(code, child = child, subResultsFlag = subResultsFlag)
                }
            }
        } else {
            for (index in 0..subClassCount - 1) {
                finalizeSubClass(index)
            }
            /*
            var index = 0
            for (gradeGroup in gradeCodes.split(";")) {
                for (heightGroup in heightCodes.split(";")) {
                    for (jumpHeightGroup in jumpHeightCodes.split(";")) {
                        finalizeSubClass(index)
                        index++
                    }
                }
            }            
             */

        }
    }

    val resultsCopies: Int
        get() = if (template.resultsCopies > -1)
            template.resultsCopies
        else if (competition.resultsCopies > 0)
            competition.resultsCopies
        else
            Global.resultsCopies

    val awardsCopies: Int
        get() = if (template.awardsCopies > -1)
            template.awardsCopies
        else if (competition.awardsCopies > 0)
            competition.awardsCopies
        else
            Global.awardsCopies

    fun closeClass(force: Boolean = false): Boolean {
        if (progress != CLASS_CLOSED || force) {
            dbTransaction {
                removeNeedRequests()
                if (!template.nfcOnly) {
                    finalizeClass()
                }
                progress = CLASS_CLOSED
                finalized = true
                post()
                updateRing()
                if (isUka || isUkOpen) {
                    if (!template.isSubClass) {
                        Reports.printResults(id, copies = Global.resultsCopies)
                    }
                } else {
                    if (!template.nfcOnly) {
                        Reports.printResults(id, copies = resultsCopies)
                        if (awardsCopies > 0) Reports.printAwards(id, copies = awardsCopies)
                    }
                }
                if (isChild) {
                    parentClass.whenChildClosed(this)
                }
                if ((isChild || isSeries) && hasNext) {
                    prepareRelatedClasses(nextClass(), print = true)
                }
                template.whenClosed?.invoke(this)
            }
            return true
        } else {
            return false
        }
    }

    fun whenChildClosed(child: AgilityClass) {
        if (!template.dummyClass) {
            val subClassesClosed = getSubClassesClosed()
            if (subClassesClosed == -1) {
                closeClass()
            } else if (child.template.printIntermediateResults) {
                val maxColumn = child.template.impliedMaxColumn
                Reports.printResults(id, subResultsFlag = 0.setToBit(maxColumn - 1))
            }
            template.whenChildClosed?.invoke(this, subClassesClosed)
        }
    }

    fun getSubClassesClosed(): Int {
        if (hasChildren || isHarvested) {
            var columnFlags = 0
            var closedFlags = -1
            dbQuery("SELECT classProgress, classCode, groupColumn FROM agilityClass WHERE idAgilityClassParent=$id") {
                val classProgress = getInt("classProgress")
                val classCode = getInt("classCode")
                val groupColumn = getInt("groupColumn")
                val template = ClassTemplate.select(classCode)
                val column = if (groupColumn > 1) groupColumn else template.column
                columnFlags = columnFlags.setBit(column - 1)
                if (classProgress != CLASS_CLOSED) closedFlags = closedFlags.resetBit(column - 1)
            }
            return if (closedFlags == -1) -1 else columnFlags and closedFlags
        } else {
            return -1
        }
    }

    fun ukaPreferred(ukaPerformance: Boolean, ukaSteeplechase: Boolean, ukaCasual: Boolean, ukaNursery: Boolean, ukaJunior: Boolean): Boolean {
        return (((template.programme == PROGRAMME_PERFORMANCE || template == ClassTemplate.MASTERS) && ukaPerformance) ||
                (template.programme == PROGRAMME_STEEPLECHASE && ukaSteeplechase) ||
                (template.isCasual && ukaCasual) ||
                (template.isNursery && ukaNursery) ||
                (template.isJunior && ukaJunior))
    }

    fun fabPreferred(fabAgility: Boolean, fabJumping: Boolean, fabSteeplechase: Boolean, fabGrandPrix: Boolean, fabAllsorts: Boolean, fabIfcs: Boolean): Boolean {
        return (
                (template == ClassTemplate.FAB_AGILITY && fabAgility) ||
                        (template == ClassTemplate.FAB_JUMPING && fabJumping) ||
                        (template == ClassTemplate.FAB_STEEPLECHASE && fabSteeplechase) ||
                        (template == ClassTemplate.FAB_GRAND_PRIX && fabGrandPrix) ||
                        (template == ClassTemplate.FAB_ALLSORTS && fabAllsorts) ||
                        (template.isIfcs && fabIfcs)
                )
    }

    val isGrandPrix: Boolean
        get() = isChild && template == ClassTemplate.AGILITY && parentClass.template == ClassTemplate.GRAND_PRIX

    val isChallenge: Boolean
        get() = isChild && (template == ClassTemplate.AGILITY || template == ClassTemplate.JUMPING) && parentClass.template == ClassTemplate.CHALLENGE

    override var courseTime: Int
        get() {
            if (isGrandPrix && parentClass.courseTime > 0) {
                return parentClass.courseTime
            } else if (isChallenge && parentClass.template == ClassTemplate.AGILITY && parentClass.extra.has("agility.courseTime")) {
                return parentClass.extra["agility.courseTime"].asInt
            } else if (isChallenge && parentClass.template == ClassTemplate.JUMPING && parentClass.extra.has("jumping.courseTime")) {
                return parentClass.extra["jumping.courseTime"].asInt
            } else {
                return super.courseTime
            }
        }
        set(value) {
            super.courseTime = value
        }

    override var courseTimeSmall: Int
        get() {
            if (isGrandPrix && parentClass.courseTimeSmall > 0) {
                return parentClass.courseTimeSmall
            } else if (isChallenge && parentClass.template == ClassTemplate.AGILITY && parentClass.extra.has("agility.courseTimeSmall")) {
                return parentClass.extra["agility.courseTimeSmall"].asInt
            } else if (isChallenge && parentClass.template == ClassTemplate.JUMPING && parentClass.extra.has("jumping.courseTimeSmall")) {
                return parentClass.extra["jumping.courseTimeSmall"].asInt
            } else {
                return super.courseTimeSmall
            }
        }
        set(value) {
            super.courseTimeSmall = value
        }

    override var courseLength: Int
        get() {
            if (isGrandPrix && parentClass.courseLength > 0) {
                return parentClass.courseLength
            } else if (isChallenge && parentClass.template == ClassTemplate.AGILITY && parentClass.extra.has("agility.courseLength")) {
                return parentClass.extra["agility.courseLength"].asInt
            } else if (isChallenge && parentClass.template == ClassTemplate.JUMPING && parentClass.extra.has("jumping.courseLength")) {
                return parentClass.extra["jumping.courseLength"].asInt
            } else {
                return super.courseLength
            }
        }
        set(value) {
            super.courseLength = value
        }

    override fun whenBeforePost() {
        if (isGrandPrix && (isModified("extra") || isModified("courseLength"))) {
            parentClass.courseTime = courseTime
            parentClass.courseTimeSmall = courseTimeSmall
            parentClass.courseLength = courseLength
            parentClass.post()
        } else if (isChallenge && (isModified("extra") || isModified("courseLength"))) {
            if (parentClass.template == ClassTemplate.AGILITY) {
                parentClass.extra["agility.courseTime"] = courseTime
                parentClass.extra["agility.courseTimeSmall"] = courseTimeSmall
                parentClass.extra["agility.courseLength"] = courseLength
                parentClass.post()
            } else if (parentClass.template == ClassTemplate.JUMPING) {
                parentClass.extra["jumping.courseTime"] = courseTime
                parentClass.extra["jumping.courseTimeSmall"] = courseTimeSmall
                parentClass.extra["jumping.courseLength"] = courseLength
                parentClass.post()
            }
        }
        super.whenBeforePost()
    }

    override var heights: Json
        get() {
            val result = super.heights
            if (result.size == 0) {
                for (jumpHeight in jumpHeightArray) {
                    val node = result.addElement()
                    node["heightCode"] = jumpHeight.code
                }
                post()
            }
            return result
        }
        set(value) {
            super.heights = value
        }

    fun getHeightObject(jumpHeightCode: String): JsonNode {
        for (height in heights) {
            if (height["heightCode"].asString == jumpHeightCode) {
                return height
            }
        }
        val result = Json()
        result["heightCode"] = jumpHeightCode
        result["invalid"] = true
        return result
    }

/*
    val callingToInfo: String
        get() = getCallingToInfo(progress, heights)
*/

    val info: String
        get() = getInfo(progress, startTime, walkingOverLunch, heights)

    val briefInfo: String
        get() = getInfo(progress, startTime, walkingOverLunch, heights, calling = false)

    private fun maxRunningOrder(jumpHeightCode: String): Int {
        val query =
            DbQuery("SELECT MAX(runningOrder) AS maxRunningOrder FROM entry WHERE idAgilityClass=$id AND jumpHeightCode=${jumpHeightCode.quoted}")
        query.first()
        return query.getInt("maxRunningOrder")
    }

    private fun minRunningOrder(jumpHeightCode: String): Int {
        val query =
            DbQuery("SELECT MIN(runningOrder) AS minRunningOrder FROM entry WHERE idAgilityClass=$id AND jumpHeightCode=${jumpHeightCode.quoted}")
        query.first()
        return query.getInt("minRunningOrder")
    }

    fun getHeightCallingTo(jumpHeightCode: String): Int {
        return getHeightObject(jumpHeightCode)["callingTo"].asInt
    }

    private fun getHeightMaxRunningOrder(node: JsonNode): Int {
        if (!node.has("maxRunningOrder")) {
            node["maxRunningOrder"] = maxRunningOrder(node["heightCode"].asString)
        }
        return node["maxRunningOrder"].asInt
    }

    private fun getHeightMinRunningOrder(node: JsonNode): Int {
        if (!node.has("minRunningOrder")) {
            node["minRunningOrder"] = minRunningOrder(node["heightCode"].asString)
        }
        return node["minRunningOrder"].asInt
    }

    fun checkMinMaxRunningOrder() {
        val query =
            DbQuery("SELECT jumpHeightCode, MIN(runningOrder) AS minRunningOrder, MAX(runningOrder) AS maxRunningOrder FROM entry WHERE idAgilityClass=$id GROUP BY jumpHeightCode")
        while (query.next()) {
            val node = getHeightObject(query.getString("jumpHeightCode"))
            node["minRunningOrder"] = query.getString("minRunningOrder")
            node["maxRunningOrder"] = query.getString("maxRunningOrder")
        }
    }

    fun getHeightMaxRunningOrder(jumpHeightCode: String): Int {
        val node = getHeightObject(jumpHeightCode)
        return getHeightMaxRunningOrder(node)
    }

/*
    fun nextAvailableRunningOrder(jumpHeightCode: String): Int {
        var result = 0
        var previous = 0
        dbQuery("GROUP_CONCAT(DISTICT runningOrder ASC) AS list FROM entry WHERE idAgilityClass=$id AND jumpHeightCode=${jumpHeightCode.quoted}") {
            val runningorders = getString("list").listToIntArray()
            for (runningOrder in runningorders) {
                if (result == 0 && runningOrder > previous) {
                    result = previous + 1
                } else {
                    previous = runningOrder
                }
            }
        }
        if (result == 0) result = previous + 1
        return result
    }
*/

    fun getHeightMinRunningOrder(jumpHeightCode: String): Int {
        val node = getHeightObject(jumpHeightCode)
        return getHeightMinRunningOrder(node)
    }

    fun setHeightCallingTo(jumpHeightCode: String, runningOrder: Int) {
        val node = getHeightObject(jumpHeightCode)
        if (runningOrder == -1 || runningOrder >= getHeightMaxRunningOrder(jumpHeightCode)) {
            node["callingTo"] = CALLING_TO_END
        } else if (runningOrder < getHeightMinRunningOrder(jumpHeightCode)) {
            node["callingTo"] = 0
        } else {
            node["callingTo"] = runningOrder
        }
    }

    fun setClosingTime(jumpHeightCode: String, time: Date) {
        val node = getHeightObject(jumpHeightCode)
        node["closingTime"] = time
    }

    fun setAnnouncedClosed(jumpHeightCode: String) {
        val node = getHeightObject(jumpHeightCode)
        node["closingTime"] = ""
        node["isClosed"] = true
    }

/*
    fun cancelAnnouncedClosed(jumpHeightCode: String) {
        val node = getHeightObject(jumpHeightCode)
        node["closingTime"] = ""
        node["isClosed"] = false
    }
*/

    fun incrementHeightCallingTo(jumpHeightCode: String, amount: Int) {
        if (getHeightCallingTo(jumpHeightCode) == CALLING_TO_END) {
            if (amount < 0) {
                val maxRunningOrder = getHeightMaxRunningOrder(jumpHeightCode)
                val tranche = amount.absolute
                setHeightCallingTo(jumpHeightCode, maxRunningOrder / tranche * tranche)
            }
        } else if (getHeightCallingTo(jumpHeightCode) <= 0) {
            setHeightCallingTo(jumpHeightCode, getHeightMinRunningOrder(jumpHeightCode) + amount - 1)
        } else {
            setHeightCallingTo(jumpHeightCode, getHeightCallingTo(jumpHeightCode) + amount)
        }
    }

/*
    private fun twoHeightsBeingCalled(): Boolean {
        if (heights.size > 1) {
            for (index in 0..jumpHeightArray.size - 2) {
                val thisHeight = heights[index]
                val nextHeight = heights[index + 1]
                val thisClosed = thisHeight["closed"].asBoolean
                val nextClosed = nextHeight["closed"].asBoolean
                val thisCallingTo = thisHeight["callingTo"].asInt
                val nextCallingTo = nextHeight["callingTo"].asInt
                if (*/
/*!thisClosed && !nextClosed && *//*
thisCallingTo != 0 && nextCallingTo != 0) {
                    return true
                }
            }
        }
        return false
    }
*/

    private fun _checkHeightsClosed(ringHeightCode: String) {
        var activeFound = false
        for (height in heights) {
            if (height["heightCode"].asString == ringHeightCode) {
                activeFound = true
                height["closed"] = false
                height["active"] = true
            } else {
                height["closed"] = !activeFound
                height["active"] = false
            }
        }
        post()
    }

    private fun checkHeightsClosed() {
        var ringHeightCode = ""
        dbQuery("SELECT heightCode FROM ring WHERE idCompetition=$idCompetition AND date=${date.sqlDate} AND ringNumber=$ringNumber AND idAgilityClass=$id") {
            ringHeightCode = getString("heightCode")
        }
        _checkHeightsClosed(ringHeightCode)
    }

    class HeightStats(val entered: Int = 0, val bookedIn: Int = 0, val checkedIn: Int = 0, val run: Int = 0, val minRunningOrder: Int = 0, val maxRunningOrder: Int = 0, val callingTo: Int = 0) {

        val text: String
            get() {
                var result = if (callingTo > 0) "C$callingTo" else ""
                if (run > 0) result = result.commaAppend("R$run")
                result = result.commaAppend("W${entered + bookedIn}")
                if (bookedIn > 0) result = result.append("L$bookedIn", "/")
                return result
            }

        val notRun: Int
            get() = entered + bookedIn + checkedIn

        val totalEntered: Int
            get() = entered + bookedIn + checkedIn + run
    }

    fun getHeightQueue(): HashMap<String, Int> {
        val result = HashMap<String, Int>()
        val query =
            DbQuery("SELECT jumpHeightCode, count(*) as queuing FROM entry WHERE idAgilityClass=$id AND progress=$PROGRESS_CHECKED_IN GROUP BY jumpHeightCode")
        while (query.next()) {
            result[query.getString("jumpHeightCode")] = query.getInt("queuing")
        }
        return result
    }

    fun _heightProgress(heightStats: HashMap<String, HeightStats>): String {
        var result = ""
        if (heights.size == 1) {
            val height = heights[0]
            val jumpHeightCode = height["heightCode"].asString
            val stats = heightStats[jumpHeightCode] ?: HeightStats()
            if (isFabStyle) {
                result = stats.text
            } else {
                result = "${stats.minRunningOrder}-${stats.maxRunningOrder} (${stats.text})"
            }
        } else {
            for (height in heights) {
                val jumpHeightCode = height["heightCode"].asString
                var phrase = getJumpHeightCaption(jumpHeightCode) + ":"
                if (height["closed"].asBoolean) {
                    phrase += if (isFabStyle) " X" else " closed"
                } else {
                    val stats = heightStats[jumpHeightCode] ?: HeightStats()
                    if (isFabStyle) {
                        phrase += stats.text
                    } else {
                        phrase += "${stats.minRunningOrder}-${stats.maxRunningOrder} (${stats.text})"
                    }
                }
                result = result.append(phrase)
            }
        }
        return result
    }


    fun getHeightStats(): HashMap<String, HeightStats> {
        val result = HashMap<String, HeightStats>()
        dbQuery(
            """
            SELECT
            jumpHeightCode,
            MIN(entry.runningOrder) AS minRunningOrder,
            MAX(entry.runningOrder) AS maxRunningOrder,
            SUM(IF(entry.progress = $PROGRESS_ENTERED, 1, 0)) AS entered,
            SUM(IF(entry.progress = $PROGRESS_BOOKED_IN, 1, 0)) AS bookedIn,
            SUM(IF(entry.progress = $PROGRESS_CHECKED_IN, 1, 0)) AS checkedIn,
            SUM(IF(entry.progress IN ($PROGRESS_RUNNING, $PROGRESS_RUN, $PROGRESS_VOID), 1, 0)) AS run
            FROM entry WHERE idAgilityClass=$id AND entry.progress<$PROGRESS_REMOVED GROUP BY jumpHeightCode
        """
        ) {
            val jumpHeightCode = getString("jumpHeightCode")
            val callingTo = getHeightCallingTo(jumpHeightCode)

            result[jumpHeightCode] = HeightStats(
                getInt("entered"), getInt("bookedIn")
                , getInt("checkedIn"), getInt("run"), getInt("minRunningOrder"), getInt("maxRunningOrder"), callingTo
            )
        }
        return result
    }

    fun heightProgress(classStats: DbQuery? = null): String {
        if (classStats == null) {
            checkHeightsClosed()
            return _heightProgress(getHeightStats())
        } else {
            val map = HashMap<String, HeightStats>()

            classStats.withEach {
                if (getInt("idAgilityClass") == id) {
                    val callingTo = getHeightCallingTo(getString("heightCode"))
                    map[getString("heightCode")] = HeightStats(
                        getInt("entered"), getInt("bookedIn")
                        , getInt("checkedIn"), getInt("run"), getInt("minRunningOrder"), getInt("maxRunningOrder"), callingTo
                    )
                    if (getBoolean("scriming")) {
                        _checkHeightsClosed(getString("heightCode"))
                    }
                }

            }
            return _heightProgress(map)
        }
    }

    fun appHeightProgress(heightQueue: HashMap<String, Int> = getHeightQueue()): JsonNode {
        checkHeightsClosed()
        val result = Json()
        for (height in heights) {
            val node = result.addElement()


            val jumpHeightCode = height["heightCode"].asString
            var phrase = ""
            if (height["closed"].asBoolean) {
                phrase += " closed"
            } else {
                val callingTo = height["callingTo"].asInt
                val minRunningOrder = getHeightMinRunningOrder(height)
                val maxRunningOrder = getHeightMaxRunningOrder(height)
                val count = heightQueue[jumpHeightCode] ?: 0
                if (progress < CLASS_WALKING) {
                    phrase += "$count ($minRunningOrder-$maxRunningOrder)"
                } else if (progress >= CLASS_CLOSED) {
                    phrase += "$count Runners"
                } else {
                    if (callingTo == -1) {
                        phrase += "Calling to end ($minRunningOrder-$maxRunningOrder)"
                    } else if (callingTo == 0) {
                        phrase += "Hold ($minRunningOrder-$maxRunningOrder)"
                    } else {
                        phrase += "Calling $minRunningOrder-$callingTo, Hold ${callingTo + 1}-$maxRunningOrder"
                    }
                    if (count > 0) {
                        phrase += ", $count queuing"
                    }
                }
            }
            node["heightCode"] = jumpHeightCode
            node["caption"] = getHeightCaption(jumpHeightCode)
            node["progress"] = phrase
        }
        return result
    }

/*
    class SubClass(val agilityClass: AgilityClass, val gradeCodes: String, val heightCodes: String, val jumpHeightCodes: String, val index: Int) {

        fun getDescription(short: Boolean = true): String {
            var result = ""
            val hasSplitGrades = agilityClass.gradeCodes.contains(";")
            val hasSplitHeights = agilityClass.heightCodes.contains(";")
            if (hasSplitGrades || (!hasSplitGrades && !hasSplitHeights)) {
                result = result.spaceAppend(agilityClass.describeGrades(gradeCodes, short = short))
            }
            if (hasSplitHeights || (!hasSplitGrades && !hasSplitHeights)) {
                result = result.spaceAppend(agilityClass.getHeightCaption(heightCodes))
            }
            return result
        }

        val description: String
            get() = getDescription(true)

        val descriptionLong: String
            get() = getDescription(false)


    }
*/

    val subClassCount: Int
        get() = (gradeCodes.countOf(';') + 1) * (heightCodes.countOf(';') + 1) * (jumpHeightCodes.countOf(';') + 1) * (subDivisions.countOf(';') + 1)

    val lhoClass: Boolean
        get() = heightRunningOrder.replace("L", "") == heightCodes + "," + heightCodes

    fun subClassDescription(subClass: Int, shortGrade: Boolean = true, shortHeight: Boolean = false): String {

        fun getDescription(gradeGroup: String, heightGroup: String, jumpHeightGroup: String, subDivision: Int): String {
            var result = ""
            if (gradeCodes.contains(";")) {
                result = result.spaceAppend(agilityClass.describeGrades(gradeGroup, short = shortGrade))
            }
            if (heightCodes.contains(";")) {
                result = result.spaceAppend(Height.getHeightName(heightGroup, short = shortHeight))
            }
            if (jumpHeightCodes.contains(";")) {
                if (lhoClass) {
                    result = result.spaceAppend(if (jumpHeightGroup.contains("L")) "LHO" else "FH")
                } else {
                    result = result.spaceAppend(Height.getHeightName(jumpHeightGroup, short = shortHeight))
                }
            }
            result = result.spaceAppend(subDivisions.split(";")[subDivision])
            return result
        }

        var index = 0
        for (gradeGroup in gradeCodes.split(";")) {
            for (heightGroup in heightCodes.split(";")) {
                for (jumpHeightGroup in jumpHeightCodes.split(";")) {
                    for (subDivisionIndex in 0..subDivisions.split(";").count() - 1) {
                        if (index == subClass) {
                            return getDescription(gradeGroup, heightGroup, jumpHeightGroup, subDivisionIndex)
                        }
                        index++
                    }
                }
            }
        }
        return ""
    }

    fun subClassHeight(subClass: Int): String {
        var index = 0
        for (gradeGroup in gradeCodes.split(";")) {
            for (heightGroup in heightCodes.split(";")) {
                for (jumpHeightGroup in jumpHeightCodes.split(";")) {
                    for (subDivisionIndex in 0..subDivisions.split(";").count() - 1) {
                        if (index == subClass) {
                            return heightGroup
                        }
                        index++
                    }
                }
            }
        }
        return ""
    }

/*
    fun subClassJumpHeight(subClass: Int, shortGrade: Boolean = true, shortHeight: Boolean = false): String {
        var index = 0
        for (gradeGroup in gradeCodes.split(";")) {
            for (heightGroup in heightCodes.split(";")) {
                for (jumpHeightGroup in jumpHeightCodes.split(";")) {
                    for (subDivisionIndex in 0..subDivisions.split(";").count() - 1) {
                        if (index == subClass) {
                            return jumpHeightGroup
                        }
                        index++
                    }
                }
            }
        }
        return ""
    }
*/

    fun chooseSubClass(gradeCode: String, heightCode: String, jumpHeightCode: String, subDivision: Int): Int {
        var i = 0
        for (gradeGroup in gradeCodes.split(";")) {
            for (heightGroup in heightCodes.split(";")) {
                for (jumpHeightGroup in jumpHeightCodes.split(";")) {
                    for (subDivisionIndex in 0..subDivisions.split(";").count() - 1) {
                        if (gradeGroup.listHas(gradeCode) && heightGroup.listHas(heightCode) && jumpHeightGroup.listHas(jumpHeightCode) && subDivision == subDivisionIndex) {
                            return i
                        }
                        i++
                    }
                }
            }
        }
        return -1
    }

    enum class GradeType { UNDEFINED, SINGLE, GRADED, COMBINED, HYBRID }

    fun describeGrades(grades: String, short: Boolean = true): String {
        var result = ""
        var gradeType = GradeType.UNDEFINED
        val gradeBlocks = ArrayList<List<String>>()
        val gradeGroups = grades.split(";")
        for (gradeGroup in gradeGroups) {
            val gradeBlock = gradeGroup.split(",")
            if (gradeBlock.size == 1) {
                when (gradeType) {
                    GradeType.UNDEFINED -> gradeType = GradeType.SINGLE
                    GradeType.SINGLE -> gradeType = GradeType.GRADED
                    GradeType.COMBINED -> gradeType = GradeType.HYBRID
                    else -> doNothing()
                }
            } else {
                when (gradeType) {
                    GradeType.UNDEFINED -> gradeType = GradeType.COMBINED
                    else -> gradeType = GradeType.HYBRID
                }
            }
            gradeBlocks.add(gradeBlock)
        }
        when (gradeType) {
            GradeType.UNDEFINED -> return ""
            GradeType.SINGLE -> return (if (short) "G" else "Grade ") + Grade.getGradeShort(gradeBlocks[0][0])
            GradeType.GRADED -> return (if (short) "G" else "Graded ") + Grade.getGradeShort(gradeBlocks[0][0]) + "-" + Grade.getGradeShort(gradeBlocks[gradeBlocks.lastIndex][0])
            GradeType.COMBINED -> return (if (short) "C" else "Combined ") + Grade.getGradeShort(gradeBlocks[0][0]) + "-" + Grade.getGradeShort(gradeBlocks[0][gradeBlocks[0].lastIndex])
            GradeType.HYBRID -> {
                result = ""
                for (block in gradeBlocks) {
                    if (block.size == 1) {
                        result = result.append((if (short) "G" else "Grade ") + Grade.getGradeShort(block[0]))
                    } else {
                        result =
                            result.append((if (short) "C" else "Combined ") + Grade.getGradeShort(block[0]) + "-" + Grade.getGradeShort(block[block.lastIndex]))
                    }
                }
            }
        }
        return result
    }

/*
    enum class HeightType { UNDEFINED, SINGLE, LHO_COMBINED, LHO_SPLIT, MULTIPLE_COMBINED, MULTIPLE_SPLIT, COMBINED_ANY, UNSUPPORTED }
*/

    fun describeClassUka(short: Boolean = true): String {
        val gradeName = Grade.getGradeName(gradeCodes)
        val result = "${template.nameTemplate.replace("<grade>", gradeName)} ${suffix}".trim()
        if (short) {
            return result
        } else {
            sponsor =
                if (template.sponsor.isNotEmpty())
                    template.sponsor
                else if (idAgilityClassParent > 0 && parentClass.sponsor.isNotEmpty())
                    parentClass.sponsor
                else sponsor
            return "$sponsor $result".trim()
        }
    }

    fun describeClassFab(short: Boolean = true): String {
        val gradeName = Grade.getGradeName(gradeCodes)
        val result = "${template.nameTemplate.replace("<grade>", gradeName)} ${suffix}".trim()
        if (short) {
            return result
        } else {
            sponsor =
                if (template.sponsor.isNotEmpty())
                    template.sponsor
                else if (idAgilityClassParent > 0 && parentClass.sponsor.isNotEmpty())
                    parentClass.sponsor
                else sponsor
            return "$sponsor $result".trim()
        }
    }

    fun describeClass(short: Boolean = true, noPrefix: Boolean = false, omitNumber: Boolean = false, forceSponsor: Boolean = false): String {
        if (isUka) {
            return describeClassUka(short)
        }
        if (isFab) {
            return describeClassFab(short)
        }

        val grade = describeGrades(gradeCodes, short)
        var result = if (short) template.shortTemplate else template.nameTemplate
        val lhoText = if (hasFullHeight && hasLowHeight)
            if (lhoCombined) "*" else "#"
        else
            ""

        if (result.contains("Novice") && prefix.contains("Novice")) {
            result = result.replace("Novice ", "")
        }

        if ((prefix.contains("YKC") || prefix.contains("Young Handler")) && gradeCodes == "KC01,KC02,KC03,KC04,KC05,KC06,KC07") {
            result = result.replace("<grade>", "")
        }

        result = result.replace("<number>", if (omitNumber || number <= 0) "" else number.toString())
        result = result.replace("<number_suffix>", if (omitNumber) "" else numberSuffix)
        result = result.replace("<part>", if (part.isNotEmpty()) "($part)" else "")
        result = result.replace("<sponsor>", if (!short || forceSponsor) sponsor else "")
        result = result.replace("<prefix>", if (!noPrefix) prefix else "")
        if (heightCodes == "KC350,KC450,KC500,KC650" || (heightCodes == "KC350,KC450,KC650" && date.before("2020-01-01".toDate()))) {
            result = result.replace("<height>", "")
        } else {
            result = result.replace(
                "<height>",
                if (short) Height.getHeightShort(heightCodes, "")
                else Height.getHeightName(heightCodes, "")
            )
        }
        result = result.replace("<suffix>", suffix)
        result = result.replace("<grade>", grade)
        result = result.replace("<lho>", lhoText)
        if (short) {
            result = result.replace("Steeplechase", "s/chase")
            result = result.replace("Pay On The Day", "POTD")
            result = result.replace("Special", "Sp")
            result = result.replace("Any Size", "Any")
        }

        return result.removeWhiteSpace

    }

    val shortDescription: String
        get() = describeClass(true)

    val description: String
        get() = describeClass(false)


    val classTitle: String
        get() {
            val result = "Ring %d - %s".format(ringNumber, name)
            return result
        }

    fun getClassTitle(inBrackets: String): String {
        val result = "Ring %d - %s (%s)".format(ringNumber, name, inBrackets)
        return result
    }

    fun getClassHeightTimeTitle(jumpHeightCode: String): String {
        if (getCourseTime(jumpHeightCode) > 0) {
            if (combineHeights) {
                return classTitle + " - " + getCourseTime(jumpHeightCode) / 1000 + "s"
            } else {
                return getClassTitle(getHeightCaption(jumpHeightCode)) + " - " + getCourseTime(jumpHeightCode) / 1000 + "s"
            }
        } else {
            if (combineHeights) {
                return classTitle
            } else {
                return getClassTitle(getHeightCaption(jumpHeightCode))
            }
        }
    }

    fun getClassTimeTitle(): String {
        if (courseTime > 0) {
            return classTitle + " - " + courseTime / 1000 + "s"
        } else {
            return classTitle
        }
    }

    val firstJumpHeightCode: String
        get() = heightRunningOrder.split(",")[0]

    val firstGroup: String
        get() = groupRunningOrder.split(",")[0]


    fun moveToRingOrder(newRingOrder: Int) {
        if (ringOrder > 0 && newRingOrder > 0) {
            if (newRingOrder < ringOrder) {
                dbTransaction {
                    dbExecute("UPDATE agilityClass SET ringOrder=ringOrder+1 WHERE idCompetition=$idCompetition AND classDate=${date.sqlDate} AND ringNumber=$ringNumber AND ringOrder BETWEEN $newRingOrder AND ${ringOrder - 1}")
                    ringOrder = newRingOrder
                    post()
                }
            } else if (newRingOrder > ringOrder) {
                dbTransaction {
                    dbExecute("UPDATE agilityClass SET ringOrder=ringOrder-1 WHERE idCompetition=$idCompetition AND classDate=${date.sqlDate} AND ringNumber=$ringNumber AND ringOrder BETWEEN ${ringOrder + 1} AND ${newRingOrder}")
                    ringOrder = newRingOrder
                    post()
                }
            }
        }
    }

    val isAgeRestricted: Boolean
        get() = template.ageUpper > 0 || template.ageLower > 0

    fun inAgeRange(birthday: Date): Boolean {
        if (birthday.isEmpty()) return false
        val minimum = ageBaseDate.addYears(-(template.ageUpper + 1)).addDays(1)
        val maximum = ageBaseDate.addYears(-template.ageLower)
        return birthday >= minimum && birthday <= maximum
    }


    fun getNextRunnerIdEntry(group: String, jumpHeightCode: String): Int {
        val groupWhere = if (group.isNotEmpty()) " AND entry.group=${group.quoted}" else ""
        val where =            "entry.idAgilityClass=$id$groupWhere AND entry.jumpHeightCode=${jumpHeightCode.quoted} AND entry.progress IN ($PROGRESS_CHECKED_IN, $PROGRESS_RUNNING)"
        val orderBy = if (strictRunningOrder) {
            "if(entry.progress < $PROGRESS_RUNNING, 0, 1), runningOrder"
        } else {
            "entry.progress desc, entry.queueSequence"
        }
        val query = DbQuery("SELECT idEntry FROM entry WHERE $where ORDER BY $orderBy LIMIT 1")
        if (query.found()) {
            return query.getInt("idEntry")
        }
        return 0
    }

    fun idUkaAgeEligible(dateOfBirth: Date, competitionDateEnd: Date): Boolean {
        if (dateOfBirth > competitionDateEnd.addMonths(-16)) {
            return false
        } else if (dateOfBirth > competitionDateEnd.addMonths(-18)) {
            return template.oneOf(ClassTemplate.STEEPLECHASE, ClassTemplate.NURSERY_AGILITY, ClassTemplate.CASUAL_STEEPLECHASE)
        }
        return true
    }

    fun isEligible(gradeCode: String, heightCode: String, jumpHeightCode: String): Boolean {
        return this.gradeCodes.replace(";", ",").split(",").contains(gradeCode) &&
                (heightCode == "*" || this.heightCodes.replace(";", ",").split(",").contains(heightCode)) &&
                this.jumpHeightCodes.replace(";", ",").split(",").contains(jumpHeightCode)
    }

    fun isFabEligible(fabGradeAgility: String, fabGradeJumping: String, fabGradeSteeplechase: String): Boolean {
        return (template.isIfcs || template.isFab) &&
                (
                        (entryRule == ENTRY_RULE_GRADE1 && gradeCodes.replace(";", ",").split(",").contains(fabGradeAgility)) ||
                                (entryRule == ENTRY_RULE_GRADE2 && gradeCodes.replace(";", ",").split(",").contains(fabGradeJumping)) ||
                                (entryRule == ENTRY_RULE_GRADE3 && gradeCodes.replace(";", ",").split(",").contains(fabGradeSteeplechase)) ||
                                entryRule == ENTRY_RULE_ANY_GRADE
                        )
    }

    companion object {

        fun withCompetitionTemplate(idCompetition: Int, template: ClassTemplate, body: AgilityClass.() -> Unit) {
            AgilityClass().where("idCompetition=$idCompetition AND classCode = ${template.code}", "classDate, part") {
                body()
            }
        }

        private val agilityClass = AgilityClass()

        fun select(where: String, orderBy: String = "", limit: Int = 0): AgilityClass {
            val agilityClass = AgilityClass()
            agilityClass.select(where, orderBy, limit)
            return agilityClass
        }

        fun isRelay(idAgilityClass: Int): Boolean {
            if (agilityClass.isOffRow || agilityClass.id != idAgilityClass) {
                agilityClass.find(idAgilityClass)
            }
            if (agilityClass.first()) {
                return agilityClass.isRelay
            } else {
                return false
            }
        }

        fun isKnockOut(idAgilityClass: Int): Boolean {
            if (agilityClass.isOffRow || agilityClass.id != idAgilityClass) {
                agilityClass.find(idAgilityClass)
            }
            if (agilityClass.first()) {
                return agilityClass.template == ClassTemplate.CIRCULAR_KNOCKOUT
            } else {
                return false
            }
        }

        fun isLho(jumpHeightCodes: String): Boolean {
            val jumpCodes = jumpHeightCodes.replace(";", ",").split(",")
            return (jumpCodes.size == 2 && (jumpCodes[0] == jumpCodes[1] + "L" || jumpCodes[1] == jumpCodes[0] + "L"))
        }

        fun sponsor(idAgilityClass: Int): String {
            if (agilityClass.isOffRow || agilityClass.id != idAgilityClass) {
                agilityClass.find(idAgilityClass)
            }
            if (agilityClass.first()) {
                return agilityClass.template.sponsor
            } else {
                return ""
            }
        }

        private var _todayMonitor = ChangeMonitor(nullDate)
        val specialParentClassesToday: AgilityClass = AgilityClass()
            get() {
                if (_todayMonitor.hasChanged(today)) {
                    field.select(
                        """
                        idCompetition=${control.idCompetition} AND
                        classCode IN (${ClassTemplate.specialParentList}) AND
                        classDate=${today.sqlDate}
                    """
                    )
                }
                return field
            }

        private val _specialParentClasses = AgilityClass()
        private var _specialParentClassesLoaded = false

        val specialParentClasses: AgilityClass
            get() {
                if (!_specialParentClassesLoaded) {
                    _specialParentClassesLoaded = true
                    _specialParentClasses.select(
                        """
                        idCompetition=${control.idCompetition} AND
                        classCode IN (${ClassTemplate.specialParentList})
                    """
                    )
                }
                return _specialParentClasses
            }

        fun updateUka(
            idCompetition: Int, classCode: Int, suffix: String, classDate: Date,
            gradeCodes: String, idUka: Int, append: Boolean = false, post: Boolean = false,
            idAgilityClassParent: Int? = null, entryFee: Int = 0, historic: Boolean = false
        ): Int {

            if (append) {
                agilityClass.append()
            }
            agilityClass.idCompetition = idCompetition
            agilityClass.code = classCode
            agilityClass.suffix = suffix
            agilityClass.date = classDate
            agilityClass.gradeCodes = gradeCodes
            agilityClass.idUka = idUka
            agilityClass.entryFee = entryFee

            if (historic) {
                agilityClass.progress = CLASS_HISTORIC
                agilityClass.finalized = true
            }

            if (idAgilityClassParent != null) {
                agilityClass.idAgilityClassParent = idAgilityClassParent
            }
            agilityClass.name = agilityClass.shortDescription
            if (post || append) {
                agilityClass.post()
            }
            return agilityClass.id
        }

        fun importUka(
            idCompetition: Int, classCode: Int, suffix: String, classDate: Date,
            gradeCodes: String, idUka: Int, entryFee: Int = 0, historic: Boolean = false, idAgilityClassParent: Int? = null
        ): Int {
            mandate(idUka > 0, "agilityClass.importUka idUka is zero")
            agilityClass.find("idUka", idUka)
            val template = ClassTemplate.select(classCode)

            if (!agilityClass.found()) {
                updateUka(
                    idCompetition, template.code, suffix, classDate, gradeCodes, idUka, append = true,
                    entryFee = entryFee, historic = historic, idAgilityClassParent = idAgilityClassParent
                )
            } else {
                updateUka(
                    idCompetition, classCode, suffix, classDate, gradeCodes, idUka, post = true
                    , entryFee = entryFee, historic = historic
                )
            }
            if (template.type == CLASS_TYPE_SPECIAL_GROUP) {
                val idParent = agilityClass.id
                for (child in template.children) {
                    importUka(
                        idCompetition, child.code, suffix, classDate.addDays(child.day - 1), gradeCodes, child.code * 1000000 + classDate.dateInt,
                        idAgilityClassParent = idParent
                    )
                }
            } else if (template.isSeries) {
                val next = template.next
                if (next != null) {
                    importUka(idCompetition, next.code, suffix, classDate.addDays(next.day - 1), gradeCodes, next.code * 1000000 + classDate.dateInt)
                }
            }
            return agilityClass.id
        }


        fun doFinalizeClass(idAgilityClass: Int) {
            val agilityClass = AgilityClass()
            if (agilityClass.find(idAgilityClass)) {
                agilityClass.finalizeClass()
            }
        }

        fun getPreEntryCountUka(idAgilityClass: Int, jumpHeightCode: String): Int {
            val sql =
                "SELECT COUNT(*) AS total FROM entry WHERE entryType <=%d AND idAgilityClass=%d AND heightCodeEntered=%s"
            val query = DbQuery(sql, ENTRY_IMPORTED_LIVE, idAgilityClass, jumpHeightCode.quoted)
            query.first()
            return query.getInt("total")
        }

        fun getPlaceCount(idAgilityClass: Int, jumpHeightCode: String): Int {
            val sql =
                "SELECT COUNT(*) AS total FROM entry WHERE idAgilityClass=$idAgilityClass AND jumpHeightCode=${jumpHeightCode.quoted} AND Place>0 AND faults<${FAULTS_ELIMINATED * 1000}"
            val query = DbQuery(sql)
            query.first()
            return query.getInt("total")
        }

        fun getEntryCount(idAgilityClass: Int, jumpHeightCode: String = ""): Int {
            var sql =
                "SELECT COUNT(*) AS total FROM entry WHERE idAgilityClass=$idAgilityClass AND progress<$PROGRESS_DELETED_LOW"
            if (jumpHeightCode.isNotEmpty()) {
                sql += " AND jumpHeightCode=${jumpHeightCode.quoted}"
            }
            val query = DbQuery(sql)
            query.first()
            return query.getInt("total")
        }

        fun getSubClassRunners(idAgilityClass: Int, subClass: Int): Int {
            val sql =
                "SELECT COUNT(*) AS total FROM entry WHERE idAgilityClass=$idAgilityClass AND progress=$PROGRESS_RUN AND subClass=$subClass AND NOT scoreCodes LIKE '%N%'"
            val query = DbQuery(sql)
            query.first()
            return query.getInt("total")
        }

        fun getPreEntryCount(idAgilityClass: Int, subClass: Int): Int {
            val sql =
                "SELECT COUNT(*) AS total FROM entry WHERE entryType <=$ENTRY_IMPORTED_LIVE AND idAgilityClass=$idAgilityClass AND subClass=$subClass AND NOT clearRoundOnly"
            val query = DbQuery(sql)
            query.first()
            return query.getInt("total")
        }

        fun getRunnerCount(idAgilityClass: Int): Int {
            val sql = "SELECT COUNT(*) AS total FROM entry WHERE progress=%d AND idAgilityClass=%d"
            val query = DbQuery(sql, PROGRESS_RUN, idAgilityClass)
            query.first()
            return query.getInt("total")
        }

        fun isScoreBasedGame(idAgilityClass: Int): Boolean {
            val agilityClass = AgilityClass()
            try {
                if (agilityClass.find(idAgilityClass)) {
                    return agilityClass.isScoreBasedGame
                }
            } catch (e: Throwable) {
                /* ignore */
            }

            return false
        }

        fun getHeightData(idAgilityClass: Int, classStats: DbQuery, item: String): String {
            val list = CommaList()
            classStats.beforeFirst()
            while (classStats.next()) {
                if (classStats.getInt("idAgilityClass") == idAgilityClass) {
                    list.add("%s (%d)".format(classStats.getString("heightName"), classStats.getInt(item)))
                }
            }
            return list.toString()
        }

        fun noDogsRunYet(classStats: DbQuery, idAgilityClass: Int): Boolean {
            classStats.beforeFirst()
            while (classStats.next()) {
                if (classStats.getInt("idAgilityClass") == idAgilityClass && classStats.getInt("run") > 0) {
                    return false
                }
            }
            return true
        }

        fun getChaseText(classStats: DbQuery, idAgilityClass: Int, jumpHeightCode: String): String {
            val action = StringBuilder("")
            classStats.beforeFirst()
            var heightName: String
            while (classStats.next()) {
                if (classStats.getInt("idAgilityClass") == idAgilityClass && classStats.getString("heightCode") == jumpHeightCode) {
                    val bookedIn = classStats.getInt("bookedIn")
                    val checkedIn = classStats.getInt("checkedIn")
                    heightName = Height.getHeightName(classStats.getString("heightCode"))

                    if (checkedIn < 6) {

                        if (checkedIn == 0) {
                            action.append("No $heightName dogs queuing")
                        } else if (checkedIn == 1) {
                            if (bookedIn == 0) {
                                action.append("Last $heightName dog queuing")
                            } else {
                                action.append("Only 1 $heightName dog queuing")
                            }
                        } else {
                            if (bookedIn == 0) {
                                action.append("Remaining $checkedIn $heightName dogs queuing")
                            } else {
                                action.append("Only $checkedIn $heightName dogs queuing")
                            }
                        }

                        if (bookedIn > 10) {
                            action.commaAppend("more dogs needed")
                        } else if (bookedIn > 4) {
                            action.commaAppend("all %d remaining %s dogs needed".format(bookedIn, heightName))
                        } else if (bookedIn > 0) {
                            action.periodAppend("Waiting for: " + getWaitingFor(idAgilityClass, jumpHeightCode))
                        }

                        var remain = checkedIn + bookedIn
                        var heightList = ""
                        while (remain < 6 && classStats.next() && classStats.getInt("idAgilityClass") == idAgilityClass) {
                            val notRun = classStats.getInt("checkedIn") + classStats.getInt("bookedIn")
                            heightName = Height.getHeightName(classStats.getString("heightCode"))
                            if (notRun > 0) {
                                remain += notRun
                                if (remain < 6) {
                                    heightList = heightList.append(heightName)
                                } else {
                                    heightList = heightList.append(heightName, " and ")
                                }
                            }
                        }
                        if (!heightList.isEmpty()) {
                            val clause = heightList + " dogs to ring"
                            action.periodAppend(clause)
                        }

                        return action.toString()
                    }
                }
            }
            return ""
        }

        fun getWaitingFor(idAgilityClass: Int, jumpHeightCode: String): String {
            val entry = Entry("idTeam", "teamMember")
            entry.team.joinToParent("idTeam", "idCompetitor", "idDog", "teamType", "competitorName", "extra")
            entry.team.dog.joinToParent("idDog", "petName", "registeredName")
            entry.team.competitor.joinToParent("idCompetitor", "givenName", "familyName", "lastRingEventNumber", "lastRingEventProgress", "lastRingEventTime")


            val where = "entry.idAgilityClass = %d AND entry.jumpHeightCode = %s AND progress = %d"
            entry.select(where, idAgilityClass, jumpHeightCode.quoted, PROGRESS_BOOKED_IN)

            val entries = CommaList()
            while (entry.next()) {
                val ringEvent = entry.team.competitor.ringEvent()
                if (ringEvent.isEmpty()) {
                    entries.add(entry.teamDescription)
                } else {
                    entries.add("${entry.teamDescription} ($ringEvent)")

                }
            }
            return entries.toString()
        }

        fun getClassStatsQuery(idCompetition: Int, date: Date, ringNumber: Int): DbQuery {

            val join: String
            val where: String

            if (ringNumber != 0) {
                join =
                    "ON agilityClass.idCompetition=ring.idCompetition AND agilityClass.ringNumber=ring.ringNumber AND agilityClass.classDate=ring.date"
                where =
                    "ring.idCompetition=$idCompetition AND ring.date = ${date.sqlDate} AND ring.ringNumber =$ringNumber"
            } else {
                join = "USING (idAgilityClass)"
                where = "ring.idCompetition=$idCompetition AND ring.date = ${date.sqlDate}"
            }

            val sql = """
        SELECT
            ringHeight.idAgilityClass,
            ringHeight.ringNumber,
            ringHeight.className,
            ringHeight.heightCode,
            ringHeight.heightName, IF(ringHeight.heightCode = ringHeight.heightCodeRing, TRUE, FALSE) AS scriming,
            MIN(entry.runningOrder) AS minRunningOrder,
            MAX(entry.runningOrder) AS maxRunningOrder,
            SUM(IF(entry.entryType < $ENTRY_TRANSFER, 1, 0)) AS preEntered,
            SUM(IF(entry.progress = $PROGRESS_ENTERED, 1, 0)) AS entered,
            SUM(IF(entry.progress = $PROGRESS_BOOKED_IN, 1, 0)) AS bookedIn,
            SUM(IF(entry.progress = $PROGRESS_CHECKED_IN, 1, 0)) AS checkedIn,
            SUM(IF(entry.progress IN ($PROGRESS_RUNNING, $PROGRESS_RUN, $PROGRESS_VOID), 1, 0)) AS run,
            SUM(IF(entry.progress < $PROGRESS_RUNNING, 1, 0)) AS notRun
            FROM
                (SELECT
                    ring.date,
                    ring.ringNumber,
                    ring.heightCode as heightCodeRing,
                    agilityClass.idAgilityClass,
                    agilityClass.idCompetition,
                    agilityClass.className,
                    agilityClass.ringOrder,
                    agilityClass.heightRunningOrder,
                    height.heightCode,
                    height.name AS heightName
                FROM
                    ring
                    JOIN agilityClass
                    $join
                    JOIN height ON (FIND_IN_SET(height.heightCode, agilityClass.heightRunningOrder) > 0)
                WHERE
                    $where
                ) AS ringHeight
                LEFT JOIN entry entry ON entry.idAgilityClass=ringHeight.idAgilityClass AND entry.jumpHeightCode=ringHeight.heightCode AND entry.progress<=$PROGRESS_VOID
            GROUP BY
                ringHeight.ringNumber, ringHeight.ringOrder, FIND_IN_SET(ringHeight.heightCode, ringHeight.heightRunningOrder)"""
            val query = DbQuery(sql)
            debug("agilityClass", "getClassStatsQuery - return query")
            return query
        }

        data class CourseTimeParameters(
            var lowerRateOfTravel: Double = 0.0,
            var higherRateOfTravel: Double = 0.0,
            var smallUplift: Double = 1.0,
            var addedSeconds: Double = 0.0,
            var smallAddedSeconds: Double = 0.0
        )

        fun ukaTrophies(entries: Int): Int {
            return when (entries) {
                in 0..5 -> 0
                in 6..49 -> 1
                in 50..100 -> 2
                else -> 3
            }
        }

        fun ukaRosettes(entries: Int): Int {
            return when (entries) {
                in 0..5 -> 1
                in 6..10 -> 2
                in 11..15 -> 3
                in 16..30 -> 4
                else -> (entries + 9) / 10
            }
        }

/*
        fun classNumberToId(idCompetition: Int, classNumber: Int, classNumberSuffix: String = ""): Int {
            var result = 0
            AgilityClass().seek("idCompetition=$idCompetition AND classNumber=$classNumber AND classNumberSuffix=${classNumberSuffix.quoted}") {
                result = id
            }
            return result
        }
*/


        fun getCallingToInfo(progress: Int, heights: Json): String {
            var result = ""
            var callingTo = 0
            var callingToHeight = ""
            var callingIndex = -1
            var isAll = false
            val heightCount = heights.size

            for (height in heights) {
                val index = heights.indexOf(height)
                val heightCallingTo = height["callingTo"].asInt
                val maxRunningOrder = height["maxRunningOrder"].asInt
                val heightCode = height["heightCode"].asString

                if (heightCallingTo != 0) {
                    callingIndex = index
                    isAll = heightCallingTo == -1 || heightCallingTo >= maxRunningOrder
                    callingTo = heightCallingTo
                    callingToHeight = heightCode
                }
            }

            val isFullLower = heightCount == 2
            if (callingIndex >= 0) {
                val isFinalHeight = callingIndex == heightCount - 1
                val heightText =
                    if (heightCount == 1 || (isAll && isFinalHeight)) ""
                    else if (isFullLower) (if (callingToHeight.contains("L")) "LHO" else "FH")
                    else Height.getHeightName(callingToHeight)

                if (progress == CLASS_WALKING) {
                    if (isAll) {
                        result = if (isFinalHeight) "all dogs to the ring" else "all $heightText dogs to the ring"
                    } else if (callingIndex == 0) {
                        result = "first $callingTo dogs to the ring"
                    } else {
                        result = "calling to $callingTo ($heightText)"
                    }
                } else {
                    if (isAll) {
                        result = if (isFinalHeight) "calling to end" else "calling to end of $heightText"
                    } else {
                        result =
                            if (heightText.isEmpty()) "calling to $callingTo" else "calling to $callingTo ($heightText)"
                    }
                }
            }

            return result
        }

        fun getClosingInfo(progress: Int, heights: Json): String {
            var result = ""
            val closingHeight = ""
            var closingIndex = -1
            var minutes = 0
            val heightCount = heights.size

            for (height in heights) {
                if (height.has("closingTime")) {
                    closingIndex = heights.indexOf(height)
                    val heightCallingTo = height["closingTime"].asDate
                    val msecs = (heightCallingTo.time - now.time + 60000L).toInt()
                    minutes = if (msecs <= 0) -1 else (msecs / 1000 / 60)
                    println("${heightCallingTo.time - now.time}, $minutes")
                }
            }

            val isFullLower = heightCount == 2
            if (closingIndex >= 0 && minutes >= 0) {
                val isFinalHeight = closingIndex == heightCount - 1
                val heightText =
                    if (heightCount == 1 || isFinalHeight) ""
                    else if (isFullLower) (if (closingHeight.contains("L")) "LHO" else "FH")
                    else Height.getHeightName(closingHeight)

                if (progress == CLASS_RUNNING) {
                    result =
                        (if (minutes == 0) "$heightText closing now" else "$heightText closing in $minutes mins").trim()
                            .capitalize()
                }
            }

            return result
        }

        fun getInfo(progress: Int, startTime: Date, walkingOverLunch: Boolean, heights: Json, calling: Boolean = true, resumeTime: Date = nullDate): String {
            var result = ""
            if (progress == CLASS_WALKING && walkingOverLunch) {
                result = result.append("walking over lunch")
            }
            if (progress <= CLASS_WALKING && startTime.isNotEmpty()) {
                result = result.append("starting ${startTime.timeText}")
            }
            if (progress == CLASS_CLOSED_FOR_LUNCH && resumeTime.isEmpty()) {
                result = result.append("Closed for lunch (no walking)")
            }
            if (progress == CLASS_CLOSED_FOR_LUNCH && resumeTime.isNotEmpty()) {
                result = result.append("Closed for lunch, resuming at ${resumeTime.timeText} (no walking)")
            }
            if (calling && progress.oneOf(CLASS_WALKING, CLASS_RUNNING)) {
                result = result.append(getCallingToInfo(progress, heights))
            }
            if (progress.oneOf(CLASS_RUNNING)) {
                result = result.append(getClosingInfo(progress, heights))
            }
            return result
        }

    }
}