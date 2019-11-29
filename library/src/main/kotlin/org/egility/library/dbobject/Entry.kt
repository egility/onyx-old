/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.library.dbobject

import org.egility.library.database.*
import org.egility.library.general.*
import org.egility.library.general.PlazaMessage.Companion.ukaFinalsCancelled
import org.egility.library.general.PlazaMessage.Companion.ukaFinalsInvite
import org.egility.library.general.PlazaMessage.Companion.ukaFinalsReInvited
import org.egility.library.general.PlazaMessage.Companion.ukaFinalsUnCancelled
import org.egility.library.general.PlazaMessage.Companion.ukaFinalsUninvited
import java.util.*
import kotlin.collections.ArrayList


val inviteBit = 0
val dontInviteBit = 1
val invitedBit = 2
val uninvitedBit = 3
val enteredBit = 4
val cancelledBit = 5


open class EntryRaw<T : DbTable<T>>(_connection: DbConnection? = null, vararg columnNames: String) :
    DbTable<T>(_connection, "entry", *columnNames) {

    open var id: Int by DbPropertyInt("idEntry")
    open var idAgilityClass: Int by DbPropertyInt("idAgilityClass")
    open var idTeam: Int by DbPropertyInt("idTeam")
    open var idAccount: Int by DbPropertyInt("idAccount")
    open var teamMember: Int by DbPropertyInt("teamMember")
    open var subClass: Int by DbPropertyInt("subClass")
    open var idCompetitorCredit: Int by DbPropertyInt("idCompetitorCredit")
    open var lateEntryCredits: Int by DbPropertyInt("lateEntryCredits")
    open var gradeCode: String by DbPropertyString("gradeCode")
    open var heightCode: String by DbPropertyString("heightCode")
    open var jumpHeightCode: String by DbPropertyString("jumpHeightCode")
    open var subDivision: Int by DbPropertyInt("subDivision")
    open var type: Int by DbPropertyInt("entryType")
    open var timeEntered: Date by DbPropertyDate("timeEntered")
    open var timeConfirmed: Date by DbPropertyDate("timeConfirmed")
    open var group: String by DbPropertyString("group")
    open var runningOrder: Int by DbPropertyInt("runningOrder")
    open var clearRoundOnly: Boolean by DbPropertyBoolean("clearRoundOnly")
    open var progress: Int by DbPropertyInt("progress")
    open var previousProgress: Int by DbPropertyInt("previousProgress")
    open var queueSequence: Int by DbPropertyInt("queueSequence")
    open var hasRun: Boolean by DbPropertyBoolean("hasRun")
    open var courseTime: Int by DbPropertyInt("courseTime")
    open var scoreCodes: String by DbPropertyString("scoreCodes")
    open var time: Int by DbPropertyInt("time")
    open var noTime: Boolean by DbPropertyBoolean("noTime")
    open var gamesScore: Int by DbPropertyInt("gamesScore")
    open var gamesBonus: Int by DbPropertyInt("gamesBonus")
    open var courseFaults: Int by DbPropertyInt("courseFaults")
    open var timeFaults: Int by DbPropertyInt("timeFaults")
    open var faults: Int by DbPropertyInt("faults")
    open var qualifying: Boolean by DbPropertyBoolean("qualifying")
    open var points: Int by DbPropertyInt("points")
    open var runsEntered: Int by DbPropertyInt("runsEntered")
    open var runsUsed: Int by DbPropertyInt("runsUsed")
    open var runsData: Json by DbPropertyJson("runsData")
    open var combinedColumn: Int by DbPropertyInt("combinedColumn")
    open var subResults: Json by DbPropertyJson("subResults")
    open var subResultsFlag: Int by DbPropertyInt("subResultsFlag")
    open var place: Int by DbPropertyInt("place")
    open var placeFlags: Int by DbPropertyInt("placeFlags")
    open var progressionPoints: Int by DbPropertyInt("progressionPoints")
    open var finalized: Boolean by DbPropertyBoolean("finalized")
    open var runStart: Date by DbPropertyDate("runStart")
    open var runEnd: Date by DbPropertyDate("runEnd")
    open var idUka: Int by DbPropertyInt("idUka")
    open var fee: Int by DbPropertyInt("entryFee")
    open var runUnits: Int by DbPropertyInt("runUnits")
    open var member: Boolean by DbPropertyBoolean("member")
    open var heightCodeEntered: String by DbPropertyString("heightCodeEntered")
    open var runningOrderJumpHeightCode: String by DbPropertyString("runningOrderJumpHeightCode")
    open var runningOrderProvisional: Int by DbPropertyInt("runningOrderProvisional")
    open var flag: Boolean by DbPropertyBoolean("flag")
    open var dogRingNumber: Int by DbPropertyInt("dogRingNumber")
    open var qualifierFlags: Int by DbPropertyInt("qualifierFlags")
    open var scrimeTime: Int by DbPropertyInt("scrimeTime")
    open var extra: Json by DbPropertyJson("extra")
    open var idEntryLinked: Int by DbPropertyInt("idEntryLinked")
    open var dateCreated: Date by DbPropertyDate("dateCreated")
    open var deviceCreated: Int by DbPropertyInt("deviceCreated")
    open var dateModified: Date by DbPropertyDate("dateModified")
    open var deviceModified: Int by DbPropertyInt("deviceModified")
    open var idAccountOld: Int by DbPropertyInt("idAccountOld")
    open var idResult: Int by DbPropertyInt("idResult")
    open var proposedPlace: Int by DbPropertyInt("proposedPlace")
    open var oldPlace: Int by DbPropertyInt("oldPlace")
    open var idEntryOld: Int by DbPropertyInt("idEntryOld")

    // qualifying class flags
    var invite: Boolean by DbPropertyBit("qualifierFlags", inviteBit)
    var dontInvite: Boolean by DbPropertyBit("qualifierFlags", dontInviteBit)
    // grand final class flags
    var invited: Boolean by DbPropertyBit("qualifierFlags", invitedBit)
    var uninvited: Boolean by DbPropertyBit("qualifierFlags", uninvitedBit)
    var entered: Boolean by DbPropertyBit("qualifierFlags", enteredBit)
    var cancelled: Boolean by DbPropertyBit("qualifierFlags", cancelledBit)
    
//    var declined: Boolean by DbPropertyBit("qualifierFlags", declinedBit)
//    var paid: Boolean by DbPropertyBit("qualifierFlags", paidBit)
//    var notInvited: Boolean by DbPropertyBit("qualifierFlags", notInvitedBit)

    var ukaFinalsCode: Int by DbPropertyJsonInt("extra", "uka.finalsCode")
    var savedRunningOrder: Int by DbPropertyJsonInt("extra", "savedRunningOrder")
    var savedProgress: Int by DbPropertyJsonInt("extra", "savedProgress")

    val team: Team by DbLink<Team>({ Team() })
    val agilityClass: AgilityClass by DbLink<AgilityClass>({ AgilityClass() })
    val account: Account by DbLink<Account>({ Account() })
    val linkedEntry: Entry by DbLink({ Entry() }, label = "linkedEntry", keyNames = *arrayOf("idEntryLinked"))

}

enum class ScrimeList { QUEUE, RUN, OTHER, NOT_RUN, ALL }

private fun getLetterFromObstacle(obstacle: Int): Char {
    return ((OBSTACLE_1.toInt() + obstacle - 1)).toChar()
}

private fun getObstacleFromLetter(letter: Char): Int {
    if (letter >= OBSTACLE_1 && letter <= OBSTACLE_26) {
        return letter.toInt() - OBSTACLE_1.toInt() + 1
    } else {
        return 0
    }
}

private fun getDigitFromLetter(letter: Char): Int {
    if (letter >= SCORE_0 && letter <= SCORE_9) {
        return letter.toInt() - SCORE_0.toInt()
    } else if (letter >= SCORE_10 && letter <= SCORE_40) {
        return (letter.toInt() - SCORE_10.toInt() + 1) * 10
    } else {
        return -1
    }
}

private fun obstacleToCaption(obstacle: Int, isSnakesAndLadders: Boolean): String {
    var caption =
        if (obstacle <= 20) Integer.toString(obstacle) else Character.toString(('A'.toInt() + obstacle - 21).toChar())
    if (obstacle == 25) {
        caption = "Z"
    }
    /* Bob wanted this removed 4/jun/2016
    if (isSnakesAndLadders && obstacle <= 4) {
        caption += "S"
    } else if (isSnakesAndLadders && obstacle <= 8) {
        caption += "L"
    }
    */
    return caption
}

private fun getGamblersObstacleScore(obstacle: Char): Int {
    when (obstacle) {
        OBSTACLE_1, OBSTACLE_2 -> return 5
        OBSTACLE_3 -> return 4
        OBSTACLE_4, OBSTACLE_5 -> return 3
        OBSTACLE_6, OBSTACLE_7, OBSTACLE_8, OBSTACLE_9, OBSTACLE_10, OBSTACLE_11, OBSTACLE_22 -> return 2
        else -> return 1
    }
}

/*
1, 2: Dogwalk, 12 Pole Weave
3: A-Frame
4, 5: Seesaw, 6 Pole Weave
6 - 11: Tunnel, Tyre, Spread, Long, Wall
12 - 20 : Jumps

 */

fun obstacleCount(scoreCodes: String, letter: Char): Int {
    val chars = scoreCodes.toCharArray()
    var count = 0
    for (c in chars) {
        if (c == letter) {
            count++
        }
    }
    return count
}

fun haveObstacle(scoreCodes: String, letter: Char): Boolean {
    val chars = scoreCodes.toCharArray()
    var count = 0
    for (c in chars) {
        if (c == letter) {
            return true
        }
    }
    return false
}


class Entry(vararg columnNames: String, _connection: DbConnection? = null) :
    EntryRaw<Entry>(_connection, *columnNames) {

    constructor(idEntry: Int) : this() {
        find(idEntry)
    }

    val invitationOpen: Boolean
        get() = invited && !uninvited
    
    override var lateEntryCredits: Int
        get() {
            return super.lateEntryCredits
        }
        set(value) {
            if (value != lateEntryCredits) {
                if (value == 0) {
                    CompetitionLedger.returnCredits(agilityClass.idCompetition, idAccount, lateEntryCredits)
                } else {
                    CompetitionLedger.useCredits(agilityClass.idCompetition, idAccount, value)
                }
                super.lateEntryCredits = value
            }
        }

    val runningOrderText: String
        get() = (if (runningOrder < 0) "x${runningOrder.absolute}" else "$runningOrder").padStart(2, ' ')

    val isLateEntry: Boolean
        get() = type.oneOf(ENTRY_LATE_CREDITS, ENTRY_LATE_FEE)

    val isPreEntry: Boolean
        get() = type.oneOf(ENTRY_PAPER, ENTRY_AGILITY_PLAZA, ENTRY_IMPORTED_LIVE, ENTRY_TRANSFER)

    val canBookIn: Boolean
        get() = progress.oneOf(PROGRESS_ENTERED, PROGRESS_REMOVED, PROGRESS_WITHDRAWN, PROGRESS_CONVERTED_TO_CREDIT) && agilityClass.bookIn

    val canBookOut: Boolean
        get() = progress.oneOf(PROGRESS_BOOKED_IN) && !isLateEntry && agilityClass.bookIn

    val canRemoveEntry: Boolean
        get() {
            if (agilityClass.isSpecialParent) {
                return agilityClass.isOpenForEntries && progress.oneOf(PROGRESS_ENTERED, PROGRESS_BOOKED_IN, PROGRESS_WITHDRAWN, PROGRESS_CONVERTED_TO_CREDIT) && isLateEntry
            } else {
                return progress.oneOf(PROGRESS_ENTERED, PROGRESS_BOOKED_IN, PROGRESS_WITHDRAWN) && isLateEntry
            }
        }

    val canDeleteEntry: Boolean
        get() = !progress.oneOf(PROGRESS_RUNNING, PROGRESS_RUN, PROGRESS_ENTRY_DELETED)

    val canUnRemoveEntry: Boolean
        get() {
            if (agilityClass.isSpecialParent) {
                return agilityClass.isOpenForEntries && progress.oneOf(PROGRESS_REMOVED) && isLateEntry
            } else {
                return progress.oneOf(PROGRESS_REMOVED) && isLateEntry
            }
        }

    val canCheckIn: Boolean
        get() = progress.oneOf(PROGRESS_ENTERED, PROGRESS_BOOKED_IN, PROGRESS_WITHDRAWN)

    val canCheckOut: Boolean
        get() = progress.oneOf(PROGRESS_CHECKED_IN)

    val canJumpQueue: Boolean
        get() = !progress.oneOf(PROGRESS_RUN, PROGRESS_RUNNING) && !agilityClass.strictRunningOrder

    val canChangeHandler: Boolean
        get() = progress.oneOf(PROGRESS_ENTERED, PROGRESS_BOOKED_IN, PROGRESS_CHECKED_IN)

    val canWithdraw: Boolean
        get() = progress.oneOf(PROGRESS_ENTERED, PROGRESS_BOOKED_IN, PROGRESS_CHECKED_IN)

    val canUnWithdraw: Boolean
        get() = progress.oneOf(PROGRESS_WITHDRAWN)

    val canConvert: Boolean
        get() = !progress.oneOf(PROGRESS_RUN, PROGRESS_RUNNING, PROGRESS_TRANSFERRED, PROGRESS_CONVERTED_TO_CREDIT) && type.oneOf(ENTRY_PAPER, ENTRY_IMPORTED_LIVE, ENTRY_AGILITY_PLAZA, ENTRY_TRANSFER) && agilityClass.template.runUnits > 0

    val canUpdate: Boolean
        get() = !progress.oneOf(PROGRESS_RUN, PROGRESS_RUNNING, PROGRESS_VOID)

    val isEntryLocked: Boolean
        get() = agilityClass.isSpecialParent && agilityClass.closedForLateEntries

    val canChangeHeight: Boolean
        get() = (canChangeEntry || progress == PROGRESS_CONVERTED_TO_CREDIT) && !isEntryLocked

    val canSwapDogs: Boolean
        get() = canChangeEntry && agilityClass.template == ClassTemplate.SPLIT_PAIRS && !isEntryLocked

    val canChangeEntry: Boolean
        get() {
            if (agilityClass.isSpecialEntryParent) {
                return agilityClass.isOpenForEntries
            } else if (agilityClass.isSpecialEntryChild) {
                return progress <= PROGRESS_RUNNING && !agilityClass.strictRunningOrder && !isEntryLocked
            } else {
                return progress <= PROGRESS_RUNNING && !isEntryLocked
            }
        }

    val canStartRun: Boolean
        get() = progress.oneOf(PROGRESS_ENTERED, PROGRESS_BOOKED_IN, PROGRESS_CHECKED_IN, PROGRESS_RUNNING)

    val teamDescription: String
        get() {
            if (team.multiple) {
                return team.getTeamDescription(0)
            } else {
                return team.getTeamDescription(teamMember)
            }
        }

    val teamMemberName: String
        get() = team.getCompetitorDog(teamMember)

    val teamMemberNameAndUkaTitle: String
        get() = team.getCompetitorDog(teamMember, ukaTitle=true)
    
    val teamDescriptionFormal: String
        get() {
            var result = ""
            if (team.type.oneOf(TEAM_UKA_SPLIT_PAIR, TEAM_UKA_TEAM, TEAM_KC_PAIR)) {
                result = team.getTeamDescription(0, formal = true)
            } else {
                result = team.getTeamDescription(teamMember, formal = true)
                if (dogRingNumber > 0) {
                    result += " ($dogRingNumber)"
                }
            }
            return result
        }

    val ukaSubClass: Int
        get() {
            if (agilityClass.combineHeights) {
                return 0
            } else {
                return jumpHeightCode.indexIn("UKA300", "UKA400", "UKA550", "UKA650")
            }
        }


    val ukOpenSubClass: Int
        get() {
            if (agilityClass.combineHeights) {
                return 0
            } else {
                return jumpHeightCode.indexIn("OP300", "OP400", "OP500", "OP600")
            }
        }


    val competitorName: String
        get() = team.getCompetitorName(teamMember)

    val dogName: String
        get() = team.getDogName(teamMember)

    val majorName: String
        get() = team.getMajorName(teamMember)

    val minorName: String
        get() = team.getMinorName(teamMember)

    val isNFC: Boolean
        get() = scoreCodes.contains(Character.toString(SCORE_NFC))

    val isHistoric: Boolean
        get() = type == ENTRY_HISTORIC

    val isEliminated: Boolean
        get() = scoreCodes.contains(Character.toString(SCORE_ELIMINATE))

    val isEffectivelyEliminated: Boolean
        get() = if (agilityClass.template.isRelay) {
            false
        } else if (agilityClass.template == ClassTemplate.KC_CHAMPIONSHIP_HEAT) {
            points > 1000
        } else if ((agilityClass.hasChildren || agilityClass.isHarvested) && !agilityClass.template.isSuperClass) {
            faults == agilityClass.template.resultsCount * 100000
        } else {
            isEliminated
        }

    val hasQualifyingScore: Boolean
        get() = agilityClass.template.qualifierScore == 0 || points >= agilityClass.template.qualifierScore

    val isNoScore: Boolean
        get() = scoreCodes.isEmpty()

    val isPlaceable: Boolean
        get() = !isEffectivelyEliminated

    val hasScore: Boolean
        get() = !scoreCodes.isEmpty()

    val isClear: Boolean
        get() = scoreCodes.contains(Character.toString(SCORE_CLEAR))

    val isTimeNeeded: Boolean
        get() = !isEliminated && !isNFC

    val isTimeNotAllowed: Boolean
        get() = !isTimeNeeded

    val isScoreDeletable: Boolean
        get() = !isNoScore

    val isFaultAllowed: Boolean
        get() {
            if (isRelay) {
                val scoreData = getScoreData(true)
                return scoreData.runnerFaults[scoreData.runners - 1] != 100
            } else {
                return !isNFC && !isClear && !isEliminated
            }
        }

    val isClearAllowed: Boolean
        get() {
            if (agilityClass.template == ClassTemplate.SPLIT_PAIRS) {
                val scoreData = getScoreData(true)
                return scoreData.runners == 2 && scoreData.runnerFaults[1] == 0

            } else {
                return !isNFC && isNoScore
            }
        }

    val isBatonChangeAllowed: Boolean
        get() {
            if (isRelay) {
                val scoreData = getScoreData(true)
                return scoreData.runners < agilityClass.teamSize
            } else {
                return false
            }
        }

    val isBatonFaultAllowed: Boolean
        get() {
            if (isRelay) {
                return scoreCodes.countOf('B') < agilityClass.teamSize - 1
            } else {
                return false
            }
        }

    val isNFCAllowed: Boolean
        get() = !isNFC && isNoScore

    val classAllowsNFC: Boolean
        get() = agilityClass.isNfcAllowed

    val isRelay: Boolean
        get() = agilityClass.isRelay

    val isTimeEntryAllowed: Boolean
        get() {
            if (isGamblers) {
                return hasGambled
            } else if (isRelay) {
                val scoreData = getScoreData(true)
                return scoreData.runners == agilityClass.teamSize
            } else if (isSnooker || isSnakesAndLadders) {
                return !isNFC && !isEliminated
            } else if (isTimeOutAndFault) {
                return !isNFC && !isEliminated && scoreCodes.isNotEmpty()
            } else {
                return !isNFC && !isEliminated && !isNoScore
            }
        }

    val isDigitAllowed: Boolean
        get() = time >= 0 && time < agilityClass.getMaximumCourseTime(jumpHeightCode) && !noTime

    val isZeroTime: Boolean
        get() = time == 0

    val isTimerNoTimeAllowed: Boolean
        get() = isZeroTime && !noTime

    val isTimeDeleteAllowed: Boolean
        get() = !isZeroTime || noTime

    val isTimeComplete: Boolean
        get() = agilityClass.isValidCourseTime(time, jumpHeightCode) || noTime

    val isTimeConsistent: Boolean
        get() = isZeroTime || isTimeComplete

    val isResultConsistent: Boolean
        get() = isNFC || isEliminated || !isNoScore && isTimeComplete

    val timeText: String
        get() {
            val time: String
            if (isNFC || isEliminated) {
                time = "n/a"
            } else if (isTimeNotAllowed || this.time == 0 && !noTime) {
                time = "00.000"
            } else if (noTime) {
                time = "NO TIME"
            } else {
                time = "%06.3f".format(this.time.toDouble() / 1000)
            }
            return time
        }

    val courseTimeText: String
        get() = (this.courseTime / 1000).toString()

    val combinedResult: String
        get() {
            when (agilityClass.template.scoringMethod) {
                SCORING_METHOD_FAULTS -> return "%s (%06.3f)".format(faults.dec3, time.toDouble() / 1000)
                SCORING_METHOD_GAMES -> return "%s (%06.3f)".format(points, time.toDouble() / 1000)
                SCORING_METHOD_JUNIOR -> return points.dec3
                else -> return points.dec3
            }
        }


    val result: String
        get() {
            val scoreText = scoreText
            if (isNFC) {
                return "NFC"
            } else if (isEliminated) {
                return scoreText
            } else if (noTime) {
                if (agilityClass.isPointsBased) {
                    if (scoreText == "0") {
                        return "(No Time)"
                    } else {
                        return "%s + (No Time)".format(scoreText)
                    }
                } else {
                    return "%s (No Time)".format(scoreText)
                }
            } else {
                if (agilityClass.isPointsBased) {
                    if (scoreText == "0") {
                        return "%06.3f".format(time.toDouble() / 1000)
                    } else {
                        return "%s + %06.3f".format(scoreText, time.toDouble() / 1000)
                    }
                } else {
                    return "%s (%06.3f)".format(scoreText, time.toDouble() / 1000)
                }
            }
        }

    val scoreText: String
        get() = getScoreData(false).scoreText

    val runData: String
        get() = getScoreData(false).runData

    val scoreTextNFC: String
        get() {
            if (isNFC) {
                return "NFC"
            } else {
                return scoreText
            }
        }

    val prizeText: String
        get() {
            var prize = ""
            if (type == ENTRY_HISTORIC) {
                if (progressionPoints > 2) {
                    prize = place.toString()
                } else {
                    prize = "CR"
                }

            } else {
                if (isBitSet(placeFlags, PRIZE_ROSETTE)) {
                    prize = Integer.toString(place)
                } else if (isBitSet(placeFlags, PRIZE_ROSETTE_NQ)) {
                    prize = Integer.toString(place) + " (nq)"
                } else if (isBitSet(placeFlags, PRIZE_ROSETTE_CR)) {
                    prize = "CR"
                }
                if (isBitSet(placeFlags, PRIZE_TROPHY)) {
                    prize = prize + " (T)"
                }
                if (isBitSet(placeFlags, PRIZE_AWARD)) {
                    prize = prize + " (A)"
                }
            }
            return prize
        }

    val simpleTimeText: String
        get() {
            if (noTime) {
                return "NO TIME"
            } else if (isNFC || isEliminated) {
                return ""
            } else {
                return "%06.3f".format(time.toDouble() / 1000)
            }
        }

    val actualTimeText: String
        get() {
            return "%06.3f".format(time.toDouble() / 1000)
        }

    val jumpHeightText: String
        get() {
            var result = Height.getHeightName(jumpHeightCode)
            if (clearRoundOnly) {
                result = result + " CRO"
            }
            return result
        }

    val combinedHeightText: String
        get() {
            var result = Height.getCombinedName(heightCode, jumpHeightCode)
            if (clearRoundOnly) {
                result = result + " CRO"
            }
            return result
        }

    val isAgility: Boolean
        get() = agilityClass.isAgility

    val isJumping: Boolean
        get() = agilityClass.isJumping

    val isSteeplechase: Boolean
        get() = agilityClass.isSteeplechase

    val isGamblers: Boolean
        get() = agilityClass.isGamblers

    val isSnooker: Boolean
        get() = agilityClass.isSnooker

    val isPowerAndSpeed: Boolean
        get() = agilityClass.isPowerAndSpeed

    val isTimeOutAndFault: Boolean
        get() = agilityClass.isTimeOutAndFault

    val isSnakesAndLadders: Boolean
        get() = agilityClass.isSnakesAndLadders

    val isGambleAllowed: Boolean
        get() = !hasGambled && !isEliminated && !isNFC

    val isBonusAllowed: Boolean
        get() = !hasBonus && !isEliminated && !isNFC


    val isEliminationAllowed: Boolean
        get() {
            if (isGamblers) {
                return !hasGambled && !isEliminated && !isNFC
            } else if (isSnooker || isSnakesAndLadders) {
                return !isEliminated && !isNFC
            } else {
                return isFaultAllowed
            }
        }

    private val isInSnookerSequence: Boolean
        get() {
            if (!scoreCodes.isEmpty()) {
                val chars = scoreCodes.toCharArray()
                var obstacle = -1
                var previousObstacle: Int
                for (c in chars) {
                    previousObstacle = obstacle
                    obstacle = getObstacleFromLetter(c)
                    if (obstacle == 2 && (previousObstacle > 1 || previousObstacle == -1)) {
                        return true
                    } else if (obstacle == 3 && previousObstacle == 2) {
                        return true
                    }
                }
            }
            return false
        }

    private val lastObstacleNumber: Int
        get() {
            if (scoreCodes.isEmpty()) {
                return 0
            } else {
                val chars = scoreCodes.toCharArray()
                return getObstacleFromLetter(chars[chars.size - 1])
            }
        }

    val isBackAllowed: Boolean
        get() = (isNoScore && isZeroTime) || (hasRun && !isModified)


    val hasGambled: Boolean
        get() {
            if (isGamblers) {
                val chars = scoreCodes.toCharArray()
                for (c in chars) {
                    if (c == SCORE_GAMBLE_1 || c == SCORE_GAMBLE_2 || c == SCORE_NO_GAMBLE) {
                        return true
                    }
                }
            }
            return false
        }

    val isClosing: Boolean
        get() {
            if (isGamblers) {
                val chars = scoreCodes.toCharArray()
                for (c in chars) {
                    if (c == SCORE_CLOSING_SEQUENCE) {
                        return true
                    }
                }
            }
            return false
        }

    val hasBonus: Boolean
        get() {
            if (isGamblers) {
                val chars = scoreCodes.toCharArray()
                for (c in chars) {
                    if (c == SCORE_BONUS) {
                        return true
                    }
                }
            }
            return false
        }

    val hasAchievedGambled: Boolean
        get() {
            if (isGamblers) {
                val chars = scoreCodes.toCharArray()
                for (c in chars) {
                    if (c == SCORE_GAMBLE_1 || c == SCORE_GAMBLE_2) {
                        return true
                    }
                }
            }
            return false
        }

    val progressText: String
        get() {
            when (progress) {
                PROGRESS_ENTERED -> return "Entered"
                PROGRESS_BOOKED_IN -> return "Confirmed"
                PROGRESS_CHECKED_IN -> return "Queuing"
                PROGRESS_RUNNING -> return "Running"
                PROGRESS_RUN -> return "Has Run"
                PROGRESS_WITHDRAWN -> return "Withdrawn"
                PROGRESS_REMOVED -> return "Removed"
                PROGRESS_TRANSFERRED -> return "Transferred"
                PROGRESS_CONVERTED_TO_CREDIT -> return "Deleted"
                else -> return ""
            }
        }

    /*
    val progressText: String
        get() {
            when (progress) {
                PROGRESS_ENTERED -> return "Entered"
                PROGRESS_BOOKED_IN -> return "Booked In"
                PROGRESS_CHECKED_IN -> return "Checked In"
                PROGRESS_STANDING_BY -> return "Ready to Run"
                PROGRESS_RUNNING -> return "Running"
                PROGRESS_RUN -> return "Has Run"
                PROGRESS_WITHDRAWN -> return "Withdrawn"
                PROGRESS_LATE_ENTRY_REMOVED -> return "Removed"
                PROGRESS_TRANSFERRED -> return "Transferred"
                PROGRESS_CONVERTED_TO_CREDIT -> return "Deleted"
                else -> return ""
            }
        }
        */

    fun getLateRunningOrder(): Int {
        val query =
            DbQuery("SELECT MIN(runningOrder) AS lowest FROM entry WHERE idAgilityClass = $idAgilityClass AND jumpHeightCode = '$jumpHeightCode'")
        val lowest = if (query.getInt("lowest") < 0) query.getInt("lowest") else 0
        return lowest - 1
    }

    override fun whenBeforePost() {
        if (isModified("scoreCodes") || isModified("time")) {
            updateScoreData()
        }
        try {
            if (isModified("progress")) {
                if (team.type.oneOf(TEAM_SINGLE_HANDLER, TEAM_LINKED_OTHER)) {
                    Competitor.logRingActivity(team.idCompetitor, agilityClass.ringNumber, progress, id)
                }
            }
        } catch (e: Throwable) {
            doNothing()
        }
    }

///////////////////////////////////////////////////////////////////////////////////////////////

    fun removeEntry() {
        if (canRemoveEntry) {
            dbTransaction {
                lateEntryCredits = 0
                progress = PROGRESS_REMOVED
                post()
            }
        }
    }

    fun unRemoveEntry() {
        if (canUnRemoveEntry) {
            if (type == ENTRY_LATE_FEE) {
                progress = PROGRESS_BOOKED_IN
                post()
            } else {
                if (CompetitionLedger.creditsAvailable(agilityClass.idCompetition, idAccount) < agilityClass.template.runUnits) {
                    Global.services.popUp("Warning", "No late entry credits available")
                } else {
                    lateEntryCredits = agilityClass.template.runUnits
                    progress = PROGRESS_BOOKED_IN
                    post()
                }
            }
        }
    }

    fun moveToUkaGrade(newGrade: String, action: StringBuilder = StringBuilder()): Boolean {
        val currentGrade = agilityClass.gradeCodes
        val className = "%s (%s)".format(agilityClass.name, agilityClass.date.dayNameShort)
        if (newGrade == currentGrade) {
            if (progress == PROGRESS_TRANSFERRED) {
                progress = PROGRESS_ENTERED
                post()
                return true
            } else {
                addMessage("Already entered in %s", className)
                action.append("Already entered in class")
                return false
            }
        }
        if (progress == PROGRESS_TRANSFERRED) {
            return false // already transferred
        }
        if (progress == PROGRESS_RUNNING || progress == PROGRESS_RUN) {
            addMessage("Already run in %s", className)
            action.append("Already run in class")
            return false // already run
        }

        val where = """
            idCompetition=${agilityClass.idCompetition} AND
            classCode=${agilityClass.template.code} AND
            classDate=${agilityClass.date.sqlDate} AND
            gradeCodes=${newGrade.quoted} AND
            suffix=${agilityClass.suffix.quoted}
        """

        val proposedAgilityClass = AgilityClass()
        proposedAgilityClass.select(where)
        if (!proposedAgilityClass.found()) {
            addMessage("No equivalent class found for %s", className)
            action.append("No equivalent class found")

            return false // no equivalent class
        }

        val newEntry = Entry()
        if (newEntry.seekEntry(proposedAgilityClass.id, idTeam)) {
            if (newEntry.lateEntryCredits > 0) {
                newEntry.lateEntryCredits = 0
                newEntry.post()
            } else {
                addMessage("Already entered in %s", proposedAgilityClass.name)
                action.append("Already entered in class")
                return false
            }
        } else {
            proposedAgilityClass.enter(
                idAccount = idAccount,
                idTeam = idTeam,
                heightCode = heightCode,
                jumpHeightCode = jumpHeightCode,
                subDivision = subDivision,
                clearRoundOnly = clearRoundOnly,
                entryType = ENTRY_TRANSFER,
                fee = fee
            )
        }
        progress = PROGRESS_TRANSFERRED
        post()
        addMessage("%s to %s", className, proposedAgilityClass.name)
        action.append("Moved to ${proposedAgilityClass.name}")
        return true
    }

    fun moveToFabGrade(newGrade: String, action: StringBuilder = StringBuilder()): Boolean {
        val currentGrade = agilityClass.gradeCodes
        val className = "${agilityClass.name} (${agilityClass.date.dayNameShort})"
        if (newGrade == currentGrade) {
            if (progress == PROGRESS_TRANSFERRED) {
                progress = PROGRESS_ENTERED
                post()
                return true
            } else {
                addMessage("Already entered in %s", className)
                action.append("Already entered in class")
                return false
            }
        }
        if (progress == PROGRESS_TRANSFERRED) {
            return false // already transferred
        }
        if (progress == PROGRESS_RUNNING || progress == PROGRESS_RUN) {
            addMessage("Already run in %s", className)
            action.append("Already run in class")
            return false // already run
        }

        val where = """
            idCompetition=${agilityClass.idCompetition} AND
            classCode=${agilityClass.template.code} AND
            classDate=${agilityClass.date.sqlDate} AND
            gradeCodes=${newGrade.quoted} AND
            suffix=${agilityClass.suffix.quoted}
        """

        val proposedAgilityClass = AgilityClass()
        proposedAgilityClass.select(where)
        if (!proposedAgilityClass.found()) {
            addMessage("No equivalent class found for %s", className)
            action.append("No equivalent class found")

            return false // no equivalent class
        }

        val newEntry = Entry()
        if (newEntry.seekEntry(proposedAgilityClass.id, idTeam)) {
            addMessage("Already entered in %s", proposedAgilityClass.name)
            action.append("Already entered in class")
            return false
        } else {
            proposedAgilityClass.enter(
                idAccount = idAccount,
                idTeam = idTeam,
                gradeCode =  newGrade,
                heightCode = heightCode,
                jumpHeightCode = jumpHeightCode,
                subDivision = subDivision,
                clearRoundOnly = clearRoundOnly,
                entryType = ENTRY_TRANSFER,
                runningOrder = -1,
                fee = fee
            )
        }
        progress = PROGRESS_TRANSFERRED
        post()
        addMessage("%s to %s", className, proposedAgilityClass.name)
        action.append("Moved to ${proposedAgilityClass.name}")
        return true
    }

    fun kcGradeChange(newGrade: String, json: JsonNode): Boolean {
        if (newGrade != gradeCode) {
            val dateNode = json.searchElement("date", agilityClass.date, create = true)
            val classNode = dateNode["classes"].addElement()
            classNode["number"] = agilityClass.number
            classNode["numberSuffix"] = agilityClass.numberSuffix
            classNode["name"] = agilityClass.name
            if (agilityClass.isEligible(newGrade, heightCode, jumpHeightCode)) {
                classNode["action"] = "" +
                        "Grade Adjusted"
                gradeCode = newGrade
                subClass = agilityClass.chooseSubClass(newGrade, heightCode, jumpHeightCode, 0)
                post()
            } else {
                val proposed = ArrayList<Int>()
                AgilityClass().where(
                    """
                    idCompetition=${agilityClass.idCompetition} AND
                    classDate=${agilityClass.date.sqlDate} AND
                    block=${agilityClass.block} AND
                    idAgilityClass<>${agilityClass.id}
                """, "RAND()"
                ) {
                    if (isEligible(newGrade, this@Entry.heightCode, this@Entry.jumpHeightCode)) {
                        proposed.add(id)
                    }
                }
                when (proposed.size) {
                    0 -> {
                        classNode["action"] = "Removed - No equivalent class found"
                    }
                    else -> {
                        val proposedAgilityClass = AgilityClass(proposed[0])
                        var entered = false
                        Entry().seek("idAgilityClass=${proposedAgilityClass.id} AND idTeam=$idTeam AND progress=$PROGRESS_ENTERED") {
                            classNode["action"] =
                                "Removed - Already entered in class ${agilityClass.number}${agilityClass.numberSuffix}"
                            entered = true
                        }
                        if (!entered) {
                            val idEntry = proposedAgilityClass.enter(
                                idAccount = idAccount,
                                idTeam = idTeam,
                                gradeCode = newGrade,
                                heightCode = heightCode,
                                jumpHeightCode = jumpHeightCode,
                                subDivision = subDivision,
                                entryType = ENTRY_TRANSFER,
                                fee = fee,
                                runningOrder = if (agilityClass.competition.processed) -1 else null,
                                progress = PROGRESS_ENTERED
                            )
                            Entry().seek(idEntry) {
                                if (runningOrder > 0) {
                                    classNode["action"] = "Moved to: ${proposedAgilityClass.name} (r/o $runningOrder)"
                                } else {
                                    classNode["action"] = "Moved to: ${proposedAgilityClass.name}"
                                }
                            }
                        }
                    }
                }
                progress = PROGRESS_REMOVED_GRADE_CHANGE
                post()
            }
        }
        return true
    }

    fun moveToRunningOrder(newRunningOrder: Int) {
        if (runningOrder > 0 && newRunningOrder > 0) {
            if (newRunningOrder < runningOrder) {
                dbTransaction {
                    dbExecute("UPDATE entry SET runningOrder=runningOrder+1 WHERE idAgilityClass=$idAgilityClass AND `group`=${group.quoted} AND jumpHeightCode=${jumpHeightCode.quoted} AND runningOrder BETWEEN $newRunningOrder AND ${runningOrder - 1}")
                    runningOrder = newRunningOrder
                    post()
                }
            } else if (newRunningOrder > runningOrder) {
                dbTransaction {
                    dbExecute("UPDATE entry SET runningOrder=runningOrder-1 WHERE idAgilityClass=$idAgilityClass AND `group`=${group.quoted} AND jumpHeightCode=${jumpHeightCode.quoted} AND runningOrder BETWEEN ${runningOrder + 1} AND ${newRunningOrder}")
                    runningOrder = newRunningOrder
                    post()
                }
            }
        }
    }

    fun saveOldScore(reason: String) {
        val node = extra["history"].addElement()
        node["reason"] = reason
        node["date"] = now
        node["scoreCodes"] = scoreCodes
        node["time"] = time
        node["noTime"] = noTime
    }

///////////////////////////////////////////////////////////////////////////////////////////////


    fun bookIn() {
        if (canBookIn) {
            dbTransaction {
                if (progress == PROGRESS_CONVERTED_TO_CREDIT) {
                    // re-apply credit
                    lateEntryCredits = agilityClass.template.runUnits
                }
                progress = PROGRESS_BOOKED_IN
                timeConfirmed = now
                post()
            }
        }
    }

    fun bookOut() {
        if (canBookOut) {
            progress = PROGRESS_ENTERED
            timeConfirmed = now
            post()
        }
    }

    fun checkIn(queueSequence: Int = 0) {
        if (canCheckIn) {
            dbTransaction {
                if (progress == PROGRESS_CONVERTED_TO_CREDIT) {
                    // re-apply credit
                    lateEntryCredits = agilityClass.template.runUnits
                }
                progress = PROGRESS_CHECKED_IN
                if (queueSequence > 0) {
                    this.queueSequence = queueSequence
                } else {
                    val query = DbQuery(
                        """
                    SELECT MAX(queueSequence) AS lastSequence
                    FROM entry
                    WHERE
                        idAgilityClass=$idAgilityClass AND
                        jumpHeightCode=${jumpHeightCode.quoted} AND
                        progress=${PROGRESS_CHECKED_IN}
                """
                    )
                    query.first()
                    this.queueSequence = query.getInt("lastSequence") + 1
                }
                post()
            }
        }
    }

    fun jumpQueue(queueSequence: Int) {
        dbTransaction {
            val sql = """
                    UPDATE entry
                    SET
                        queueSequence = queueSequence + 1
                    WHERE
                        idAgilityClass=$idAgilityClass AND
                        jumpHeightCode=${jumpHeightCode.quoted} AND
                        queueSequence>=${queueSequence}
            """
            Global.connection.execute(sql)
            if (progress == PROGRESS_CHECKED_IN) {
                this.queueSequence = queueSequence
                post()
            } else {
                checkIn(queueSequence)
            }
        }
    }

    fun checkOut() {
        if (canCheckOut) {
            progress = if (agilityClass.competition.hasBookingIn) PROGRESS_BOOKED_IN else PROGRESS_ENTERED
            post()
        }
    }

    fun withdraw() {
        if (canWithdraw) {
            progress = PROGRESS_WITHDRAWN
            post()
        }
    }

    fun unWithdraw() {
        if (canUnWithdraw) {
            if (isLateEntry) {
                progress = PROGRESS_BOOKED_IN
            } else {
                progress = PROGRESS_ENTERED
            }
            post()
        }
    }

    fun startRun(reRun: Boolean = false) {
        if (progress != PROGRESS_RUNNING) {
            mandate(canStartRun || reRun || Global.automatedTest, "This dog has already run or is blocked by the system")
            previousProgress = progress
            progress = PROGRESS_RUNNING
            runStart = now
            if (!reRun) {
                post()
            }
        }
    }

    fun cancelRun() {
        if (progress == PROGRESS_RUNNING) {
            progress = previousProgress
            previousProgress = 0
            runStart = nullDate
            runEnd = nullDate
            post()
        }
    }

    fun endRun(force: Boolean = false) {
        if (force || progress >= PROGRESS_RUNNING) {
            progress = PROGRESS_RUN
            runEnd = now
            hasRun = true
            editRun()
        }
    }

    fun editRun() {
        updateScoreData(true)
        if ((Competition.isGrandFinals || Competition.isUkOpen) && agilityClass.idAgilityClassParent > 0 && progress < PROGRESS_DELETED_LOW) {
            UpdateParentSubResult(agilityClass.idAgilityClassParent, agilityClass)
        }
        post()
    }

    fun updateScoreData(log: Boolean = false) {
        if (!agilityClass.hasChildren && !agilityClass.isHarvested) {
            val scoreData = getScoreData(false)
            courseTime = scoreData.courseTime
            time = scoreData.time
            gamesScore = scoreData.gamesScore
            gamesBonus = scoreData.gamesBonus
            courseFaults = scoreData.courseFaults
            timeFaults = scoreData.timeFaults
            faults = scoreData.faults
            qualifying = scoreData.qualifying
            points = scoreData.points

            if (isEliminated) {
                noTime = false
            }
            if (log) {
                debug("score", "idEntry=$id, name=${team.description}, scoreCodes=$scoreCodes, time=$time, data=${scoreData.runData}, faults=$faults, points=$points")
            }
        }
    }

    fun UpdateParentSubResult(idAgilityClassParent: Int, agilityClass: AgilityClass) {
        if (idAgilityClassParent > 0 && agilityClass.template.column >= 0) {
            val parent = Entry()
            parent.select("idAgilityClass=$idAgilityClassParent && idTeam=$idTeam")
            if (!parent.found()) {
                parent.append()
                parent.idAgilityClass = idAgilityClassParent
                parent.idTeam = idTeam
                parent.idAccount = idAccount
                parent.type = ENTRY_DEPENDENT_CLASS
            }
            parent.heightCode = heightCode
            parent.jumpHeightCode = jumpHeightCode
            parent.gradeCode = gradeCode


            if (agilityClass.template.isSubClass) {
                parent.hasRun = hasRun
                parent.courseTime = courseTime
                parent.scoreCodes = scoreCodes
                parent.time = time
                parent.noTime = noTime
                parent.gamesScore = gamesScore
                parent.gamesBonus = gamesBonus
                parent.courseFaults = courseFaults
                parent.timeFaults = timeFaults
                parent.faults = faults
                parent.qualifying = qualifying
                parent.points = points
            } else {

                val actualColumn = if (combinedColumn > 0) combinedColumn else agilityClass.combinedColumn

                val totalFaults = if (hasRun)
                    if (courseFaults >= FAULTS_ELIMINATED)
                        courseFaults * 1000
                    else
                        courseFaults * agilityClass.template.faultToTime + timeFaults
                else
                    FAULTS_NON_RUNNER * 1000
                val actualPoints =
                    if (hasRun) points else agilityClass.template.getPoints(courseTime, 0, 0, 0, false, true)

                parent.subResultsFlag = parent.subResultsFlag.resetBit(actualColumn - 1)

                // this should not be necessary but without it column 2 falls into column 1 if there is nothing in col 1
                for (i in 1..actualColumn) {
                    parent.subResults[i - 1]["i"] = i
                }
                if (hasRun && !isNFC) {
                    parent.subResults[actualColumn - 1]["courseTime"] = courseTime
                    parent.subResults[actualColumn - 1]["faults"] = totalFaults
                    parent.subResults[actualColumn - 1]["time"] = time
                    parent.subResults[actualColumn - 1]["points"] =
                        agilityClass.template.getParentPoints?.invoke(this) ?: actualPoints
                    parent.subResults[actualColumn - 1]["hasRun"] = hasRun
                    parent.subResultsFlag = parent.subResultsFlag.setBit(actualColumn - 1)
                } else if (parent.subResults[actualColumn - 1].size > 0) {
                    parent.subResults[actualColumn - 1].clear()
                }
                parent.combineSubResults()
            }

            parent.post()
        }
    }

    fun combineSubResults() {
        if (!agilityClass.template.isSuperClass) {

            fun isNfc(i: Int): Boolean {
                return agilityClass.isNfcAllowed && subResults[i]["faults"].asInt == FAULTS_NFC * 1000
            }

            var nfc = false
            var courseTime = 0
            var faults = 0
            var time = 0
            var points = 0
            var hasRun = true
            var subResultsFlag = 0

            val resultsCount = agilityClass.template.resultsCount

            for (i in 0..resultsCount - 1) {
                courseTime += subResults[i]["courseTime"].asInt
                faults += subResults[i]["faults"].asInt
                time += subResults[i]["time"].asInt
                points += subResults[i]["points"].asInt
                if (isNfc(i)) {
                    nfc = true
                }
                if (subResults[i]["hasRun"].asBoolean && !isNFC) {
                    subResultsFlag = subResultsFlag.setBit(i)
                } else {
                    hasRun = false
                }
            }

            this.courseTime = courseTime
            this.faults = faults
            this.time = time
            this.points = points
            this.hasRun = hasRun
            this.subResultsFlag = subResultsFlag
            this.post()
        }
    }

    fun changeHeight(heightCode: String, clearRoundOnly: Boolean? = null) {
        if (isOnRow) {
            this.heightCode = heightCode
            this.jumpHeightCode = heightCode
            if (clearRoundOnly != null) {
                this.clearRoundOnly = clearRoundOnly
            }
            post()
            if (agilityClass.hasChildren) {
                val entry = Entry()
                val child = agilityClass.childClasses()
                while (child.next()) {
                    entry.seekEntry(child.id, this.idTeam)
                    if (entry.found()) {
                        entry.changeHeight(heightCode, clearRoundOnly)
                    }
                }
            }
        }
    }

    fun changeEntryTeam(idTeamNew: Int) {
        val idTeamOld = this.idTeam
        if (isOnRow) {
            this.idTeam = idTeamNew
            post()
            if (agilityClass.isChild) {
                Entry().seek("idAgilityClass=${agilityClass.idAgilityClassParent} && idTeam=$idTeamOld") {
                    changeEntryTeam(idTeamNew)
                }
            }

            if (agilityClass.hasChildren) {
                val entry = Entry()
                val child = agilityClass.childClasses()
                while (child.next()) {
                    entry.seekEntry(child.id, idTeamOld)
                    if (entry.found()) {
                        entry.changeEntryTeam(idTeamNew)
                    }
                }
            }
        }
    }


    fun seekEntry(idAgilityClass: Int, idTeam: Int, teamMember: Int = 1): Boolean {
        val where =
            "entry.idAgilityClass=%d AND entry.idTeam=%d AND entry.teamMember=%d".format(idAgilityClass, idTeam, teamMember)
        find(where)
        return found()
    }

    fun selectEntry(idAgilityClass: Int, idTeam: Int): Int {
        val found = seekEntry(idAgilityClass, idTeam)
        mandate(found, "Dog not entered in class")
        return id
    }

    fun addScoreCode(aEventID: Char) {
        if (aEventID == SCORE_DELETE) {
            if (scoreCodes.length > 1) {
                scoreCodes = scoreCodes.substring(0, scoreCodes.length - 1)
            } else {
                scoreCodes = ""
            }
        } else {
            scoreCodes = scoreCodes + aEventID
        }
    }

    fun addGamesObstacle(obstacle: Int) {
    }

    fun addGamesGamble(obstacle: Int) {
    }

    fun addTimeDigit(aDigit: Int) {
        if (aDigit >= 0 && aDigit <= 9) {
            time = time * 10 + aDigit
        } else if (aDigit == TIME_DELETE) {
            if (noTime) {
                noTime = false
                time = 0
            } else {
                time = time / 10
            }
        } else if (aDigit == TIME_RESET) {
            noTime = false
            time = 0
        } else if (aDigit == TIME_NO_TIME) {
            noTime = true
            time = 0
        }
    }

    fun getStatusText(aPosition: Boolean): String {
        var vResult = ""
        when (progress) {
            PROGRESS_ENTERED -> vResult = ""
            PROGRESS_BOOKED_IN -> vResult = "Booked-In"
            PROGRESS_CHECKED_IN -> vResult = "Queuing"
            PROGRESS_RUNNING -> vResult = "Running"
            PROGRESS_RUN -> {
                vResult = result
                if (aPosition && (courseFaults < FAULTS_ELIMINATED)) {
                    vResult = Integer.toString(place) + ": " + vResult
                }
            }
            PROGRESS_WITHDRAWN -> vResult = "Withdrawn"
            PROGRESS_REMOVED -> vResult = "Removed"
            PROGRESS_TRANSFERRED -> vResult = "Transferred"
            PROGRESS_CONVERTED_TO_CREDIT -> vResult = "Deleted"
        }
        return vResult
    }

    fun getRunData(abbreviated: Boolean): String {
        return getScoreData(abbreviated).runData
    }

    fun getRunDataEliminated(): String {
        var data = getRunData(true)
        data = data.substringAfter("(", "")
        return data.substringBefore(")")
    }

    fun selectBookIn(idAgilityClass: Int) {
        team.joinToParent("idTeam", "idCompetitor", "idDog", "teamType", "competitorName", "extra")
        team.dog.joinToParent("idDog", "petName", "registeredName")
        team.competitor.joinToParent("idCompetitor", "givenName", "familyName", "dateDeleted", "ukaMembershipExpires")

        var where = "entry.idAgilityClass=$idAgilityClass AND entry.progress<$PROGRESS_DELETED_LOW"

        var orderBy =
            "if(entry.progress = $PROGRESS_ENTERED, '2099-12-31', entry.dateModified) DESC, competitor.givenName, competitor.familyName, dog.petName"
        if (AgilityClass.isRelay(idAgilityClass)) {
            orderBy = "if(entry.progress = $PROGRESS_ENTERED, '2099-12-31', entry.dateModified) DESC, team.teamName"
        }
        select(where, orderBy)
    }

    fun selectWaitingFor(agilityClass: AgilityClass, group: String, jumpHeightCode: String, hasBookingIn: Boolean, byRunningOrder: Boolean) {
        val groupWhere = if (group.isNotEmpty()) " AND entry.group=${group.quoted}" else ""
        val template = agilityClass.template
        team.joinToParent("idTeam", "idCompetitor", "idDog", "teamType", "competitorName", "extra")
        team.dog.joinToParent("idDog", "petName", "registeredName")
        team.competitor.joinToParent(
            "idCompetitor", "givenName", "familyName", "dateDeleted",
            "lastRingEventTime", "lastRingEventNumber", "lastRingEventProgress", "neededRingTime", "neededRingNumber", "ukaMembershipExpires"
        )
        var where =
            "entry.idAgilityClass=${agilityClass.id}$groupWhere AND entry.jumpHeightCode=${jumpHeightCode.quoted} AND entry.progress IN ($PROGRESS_ENTERED,$PROGRESS_BOOKED_IN,$PROGRESS_WITHDRAWN)"

        if (agilityClass.isKc && agilityClass.getHeightCallingTo(jumpHeightCode) != CALLING_TO_END) {
            where += " AND runningOrder<=${agilityClass.getHeightCallingTo(jumpHeightCode)}"
        }


        var orderBy =
            if (byRunningOrder || agilityClass.strictRunningOrder || agilityClass.useCallingTo) "IF(entry.progress=${PROGRESS_WITHDRAWN},1,0), runningOrder"
            else if (AgilityClass.isRelay(agilityClass.id)) "IF(entry.progress=${PROGRESS_WITHDRAWN},1,0), entry.progress DESC, team.teamName"
            else if (!hasBookingIn) "IF(entry.progress=${PROGRESS_WITHDRAWN},1,0), IF(team.competitorName<>'', team.competitorName, CONCAT(competitor.givenName, ' ', competitor.familyName)), dog.petName"
            else "IF(entry.progress=${PROGRESS_WITHDRAWN},1,0), entry.progress DESC, IF(team.competitorName<>'', team.competitorName, CONCAT(competitor.givenName, ' ', competitor.familyName)), dog.petName"

        select(where, orderBy)
    }

    fun selectAll(agilityClass: AgilityClass, jumpHeightCode: String, byRunningOrder: Boolean) {
        val template = agilityClass.template
        team.joinToParent("idTeam", "idCompetitor", "idDog", "teamType", "competitorName", "extra")
        team.dog.joinToParent("idDog", "petName", "registeredName")
        team.competitor.joinToParent(
            "idCompetitor", "givenName", "familyName", "dateDeleted",
            "lastRingEventTime", "lastRingEventNumber", "lastRingEventProgress", "neededRingTime"
        )

        var where =
            "entry.idAgilityClass=${agilityClass.id} AND entry.jumpHeightCode=${jumpHeightCode.quoted} AND entry.progress<$PROGRESS_REMOVED"
        var orderBy = if (byRunningOrder) "runningOrder"
        else if (AgilityClass.isRelay(agilityClass.id)) "team.teamName"
        else "IF(team.competitorName<>'', team.competitorName, CONCAT(competitor.givenName, ' ', competitor.familyName)), dog.petName"

        select(where, orderBy)
    }

    fun selectRunningOrder(agilityClass: AgilityClass, group: String, jumpHeightCode: String, exceptIdEntry: Int = 0) {
        val groupWhere = if (group.isNotEmpty()) {
            " AND entry.group = ${group.quoted}"
        } else {
            ""
        }
        val template = agilityClass.template
        team.joinToParent("idTeam", "idCompetitor", "idDog", "teamType", "competitorName", "extra")
        team.dog.joinToParent("idDog", "petName", "registeredName")
        team.competitor.joinToParent(
            "idCompetitor", "givenName", "familyName", "dateDeleted",
            "lastRingEventTime", "lastRingEventNumber", "lastRingEventProgress", "neededRingTime", "neededRingNumber", "ukaMembershipExpires"
        )

        var where =
            "entry.idAgilityClass=${agilityClass.id} AND entry.jumpHeightCode=${jumpHeightCode.quoted} AND entry.runningOrder>0$groupWhere AND entry.progress<$PROGRESS_ENTRY_DELETED"
        if (exceptIdEntry > 0) {
            where += " AND entry.idEntry<>$exceptIdEntry"
        }


        select(where, "`group`, runningOrder")
    }

    fun selectQueue(idAgilityClass: Int, group: String, heightRunningOrder: String, template: ClassTemplate, strictRunningOrder: Boolean) {
        team.joinToParent("idTeam", "idCompetitor", "idDog", "teamType", "competitorName", "extra")
        team.dog.joinToParent("idDog", "petName", "registeredName")
        team.competitor.joinToParent(
            "idCompetitor", "givenName", "familyName", "dateDeleted",
            "lastRingEventTime", "lastRingEventNumber", "lastRingEventProgress", "neededRingTime", "neededRingNumber", "ukaMembershipExpires"
        )

        val where =
            if (Competition.isKc) {
                if (Competition.current.hasBookingIn) {
                    "entry.idAgilityClass=$idAgilityClass AND entry.group = ${group.quoted} AND entry.progress = $PROGRESS_CHECKED_IN"
                } else {
                    "entry.idAgilityClass=$idAgilityClass AND entry.group = ${group.quoted} AND entry.progress IN ($PROGRESS_CHECKED_IN, $PROGRESS_BOOKED_IN)"
                }
            } else {
                if (Competition.current.hasBookingIn) {
                    "entry.idAgilityClass=$idAgilityClass AND entry.group = ${group.quoted} AND (entry.progress = $PROGRESS_CHECKED_IN OR entry.noTime)"
                } else {
                    "entry.idAgilityClass=$idAgilityClass AND entry.group = ${group.quoted} AND (entry.progress IN ($PROGRESS_CHECKED_IN, $PROGRESS_BOOKED_IN) OR entry.noTime)"
                }
            }
        val orderBy =
            if (Competition.isKc) {
                "FIND_IN_SET(entry.jumpHeightCode, ${heightRunningOrder.quoted}), " +
                        "IF(entry.progress=$PROGRESS_CHECKED_IN, 0, 1), " +
                        if (strictRunningOrder) "entry.runningOrder" else "entry.queueSequence"
            } else {
                "FIND_IN_SET(entry.jumpHeightCode, ${heightRunningOrder.quoted}), " +
                        "IF(entry.progress=$PROGRESS_CHECKED_IN, 0, 1), " +
                        if (strictRunningOrder) "entry.runningOrder" else "if(entry.noTime, 0, 1), entry.queueSequence"
            }
        select(where, orderBy)
    }

    fun selectMissing(idAgilityClass: Int, jumpHeightCode: String, template: ClassTemplate) {
        team.joinToParent("idTeam", "idCompetitor", "idDog", "teamType", "competitorName", "extra")
        team.dog.joinToParent("idDog", "petName", "registeredName")
        team.competitor.joinToParent(
            "idCompetitor", "givenName", "familyName", "dateDeleted",
            "lastRingEventTime", "lastRingEventNumber", "lastRingEventProgress", "neededRingTime", "neededRingNumber", "ukaMembershipExpires"
        )

        var where =
            "entry.idAgilityClass=$idAgilityClass AND entry.jumpHeightCode=${jumpHeightCode.quoted} AND (entry.progress IN ($PROGRESS_ENTERED, $PROGRESS_BOOKED_IN))"
        var orderBy =
            if (template.teamSize == 1) {
                "entry.progress DESC, IF(team.competitorName<>'', team.competitorName, CONCAT(competitor.givenName, ' ', competitor.familyName))"
            } else if (template.teamSize == 2) {
                "entry.progress DESC, dog.petName, dog_1.petName"
            } else {
                "entry.progress DESC, team.teamName"
            }
        select(where, orderBy)
    }

    fun selectScrime(
        agilityClass: AgilityClass, group: String, jumpHeightCode: String, template: ClassTemplate,
        exceptIdEntry: Int = 0, scrimeList: ScrimeList, hasBookingIn: Boolean = false, paperScrime: Boolean=false
    ) {
        team.joinToParent("idTeam", "idCompetitor", "idDog", "teamType", "competitorName", "extra")
        team.dog.joinToParent("idDog", "petName", "registeredName")
        team.competitor.joinToParent("idCompetitor", "givenName", "familyName", "dateDeleted", "ukaMembershipExpires")

        val idAgilityClass = agilityClass.id

        val groupWhere = if (group.isNotEmpty()) " AND entry.group=${group.quoted}" else ""

        var where =
            "entry.idAgilityClass=$idAgilityClass$groupWhere AND entry.jumpHeightCode=${jumpHeightCode.quoted} AND entry.progress<=$PROGRESS_WITHDRAWN"
        if (exceptIdEntry > 0) {
            where += " AND entry.idEntry<>$exceptIdEntry"
        }
        when (scrimeList) {
            ScrimeList.QUEUE -> where +=
                if (Competition.isUka)
                    " AND (entry.progress=$PROGRESS_CHECKED_IN OR entry.noTime)"
                else
                    " AND entry.progress=$PROGRESS_CHECKED_IN"
            ScrimeList.RUN -> where += " AND entry.progress=$PROGRESS_RUN"
            ScrimeList.NOT_RUN -> where += " AND entry.progress<>$PROGRESS_RUN"
            ScrimeList.OTHER -> where += " AND NOT entry.progress IN ($PROGRESS_CHECKED_IN, $PROGRESS_RUN)"
            ScrimeList.ALL -> doNothing()
        }
        var orderBy = ""

        val noTime = if (Competition.isUka) "if(entry.noTime, 0, 1)," else ""
        if (exceptIdEntry > 0) {
            // then swapping
            if (agilityClass.strictRunningOrder) {
                orderBy = "entry.runningOrder"
            } else if (template.teamSize == 1) {
                orderBy =
                    "IF(team.competitorName<>'', team.competitorName, CONCAT(competitor.givenName, ' ', competitor.familyName))"
            } else if (template.teamSize == 2) {
                orderBy = "dog.petName, dog_1.petName"
            } else {
                orderBy = "team.teamName"
            }
        } else if (paperScrime) {
            orderBy = "runningOrder"
        } else if (agilityClass.strictRunningOrder) {
            orderBy =
                "if(entry.progress = $PROGRESS_CHECKED_IN, 0, 1), if(entry.progress < $PROGRESS_CHECKED_IN, 0, 1), runningOrder"
        } else if (!hasBookingIn) {
            orderBy = """
                if(entry.progress = $PROGRESS_CHECKED_IN, 0, 1),
                $noTime
                if(entry.progress = $PROGRESS_BOOKED_IN, 0, 1),
                if(entry.progress <= $PROGRESS_BOOKED_IN, 0, 1),
                entry.progress,
                if(entry.progress in ($PROGRESS_CHECKED_IN), entry.queueSequence, 0),
                entry.runEnd desc, ${if (AgilityClass.isRelay(idAgilityClass)) "team.teamName" else "IF(team.competitorName<>'', team.competitorName, CONCAT(competitor.givenName, ' ', competitor.familyName)), dog.petName"}
            """
        } else {
            orderBy = """
                if(entry.progress = $PROGRESS_CHECKED_IN, 0, 1),
                $noTime
                if(entry.progress = $PROGRESS_BOOKED_IN, 0, 1),
                entry.progress,
                if(entry.progress in ($PROGRESS_CHECKED_IN), entry.queueSequence, 0),
                entry.runEnd desc, ${if (AgilityClass.isRelay(idAgilityClass)) "team.teamName" else "IF(team.competitorName<>'', team.competitorName, CONCAT(competitor.givenName, ' ', competitor.familyName)), dog.petName"}
            """
        }
        select(where, orderBy)
    }

    fun selectQueuing(idAgilityClass: Int, jumpHeightCode: String, template: ClassTemplate, exceptIdEntry: Int = 0) {
        team.joinToParent("idTeam", "idCompetitor", "idDog", "teamType", "competitorName", "extra")
        team.dog.joinToParent("idDog", "petName", "registeredName")
        team.competitor.joinToParent("idCompetitor", "givenName", "familyName")

        var where =
            "entry.idAgilityClass=$idAgilityClass AND entry.jumpHeightCode=${jumpHeightCode.quoted} AND entry.progress=$PROGRESS_CHECKED_IN"
        if (exceptIdEntry > 0) {
            where += " AND entry.idEntry<>$exceptIdEntry"
        }

        select(where, "entry.queueSequence")
    }

    fun selectStillToRun(idAgilityClass: Int) {
        team.joinToParent("idTeam", "idCompetitor", "idDog", "teamType", "competitorName", "extra")
        team.dog.joinToParent("idDog", "petName", "registeredName")
        team.competitor.joinToParent("idCompetitor", "givenName", "familyName")

        var where =
            "entry.idAgilityClass=$idAgilityClass AND entry.progress IN ($PROGRESS_BOOKED_IN, $PROGRESS_CHECKED_IN, $PROGRESS_RUNNING)"
        var orderBy = "competitor.givenName, competitor.familyName, dog.petName"
        select(where, orderBy)
    }

    fun selectResults(selectedAgilityClass: AgilityClass, subClass: Int, awards: Boolean = false) {
        agilityClass.joinToParent()
        team.joinToParent("idTeam", "idCompetitor", "idDog", "teamType", "competitorName", "extra")
        team.dog.joinToParent()
        team.competitor.joinToParent()

        var where = "entry.idAgilityClass=${selectedAgilityClass.id} AND entry.subClass=$subClass AND entry.hasRun"
        if (awards) {
            where += " AND entry.placeFlags<>0"
        }
        val orderBy =
            "place, IF(team.competitorName <> '', team.competitorName, CONCAT(competitor.givenName, ' ', competitor.familyName))"
        select(where, orderBy)
    }

    fun selectResultsUka(selectedAgilityClass: AgilityClass, jumpHeightCode: String = "", awards: Boolean = false, subResultsFlag: Int = 0) {
        agilityClass.joinToParent()
        team.joinToParent("idTeam", "idCompetitor", "idDog", "teamType", "competitorName", "extra")
        team.dog.joinToParent()
        team.competitor.joinToParent()

        var where =
            if (subResultsFlag > 0)
                "entry.idAgilityClass=${selectedAgilityClass.id} AND entry.subResultsFlag & $subResultsFlag = $subResultsFlag"
            else
                "entry.idAgilityClass=${selectedAgilityClass.id} AND entry.hasRun"
        /*
   if (selectedAgilityClass.template == ClassTemplate.MASTERS) {
       where = "agilityClass.idAgilityClass=${selectedAgilityClass.id} AND (entry.subClass1hasRun OR entry.subClass2hasRun)"
   }
   */

        if (!jumpHeightCode.isEmpty()) {
            where += " AND entry.jumpHeightCode=${jumpHeightCode.quoted}"
        }
        if (awards) {
            where += " AND entry.placeFlags<>0"
        }
        val orderBy =
            "place, IF(team.competitorName <> '', team.competitorName, CONCAT(competitor.givenName, ' ', competitor.familyName))"
        select(where, orderBy)
    }

    fun selectRunningOrder(idAgilityClass: Int, jumpHeightCode: String = "", group: String = "") {
        val lookup = AgilityClass()
        lookup.find(idAgilityClass)
        val template = lookup.template


        agilityClass.joinToParent()
        team.joinToParent()
        team.dog.joinToParent()
        team.competitor.joinToParent()

        var where = "entry.idAgilityClass=$idAgilityClass AND entry.progress<=$PROGRESS_WITHDRAWN"
        if (template == ClassTemplate.CIRCULAR_KNOCKOUT || !agilityClass.template.feedWithdrawn) {
            where = "entry.idAgilityClass=$idAgilityClass AND entry.progress<$PROGRESS_WITHDRAWN"
        }

        if (!jumpHeightCode.isEmpty()) {
            where += " AND entry.jumpHeightCode=${jumpHeightCode.quoted}"
        }
        if (!group.isEmpty()) {
            where += " AND entry.group=${group.quoted}"
        }
        val orderBy = "runningOrder"
        select(where, orderBy)
    }


    fun shiftRunningOrder(newRunningOrder: Int) {
        dbTransaction {
            if (newRunningOrder < runningOrder) {
                dbExecute("UPDATE entry SET runningOrder=runningOrder+1, runningOrder = runningOrder WHERE idAgilityClass=$idAgilityClass AND `group`=${group.quoted} AND jumpHeightCode=${jumpHeightCode.quoted} AND runningOrder BETWEEN $newRunningOrder AND ${runningOrder - 1}")
            } else if (newRunningOrder > runningOrder) {
                dbExecute("UPDATE entry SET runningOrder=runningOrder-1, runningOrder = runningOrder WHERE idAgilityClass=$idAgilityClass AND `group`=${group.quoted} AND jumpHeightCode=${jumpHeightCode.quoted} AND runningOrder BETWEEN ${runningOrder + 1} AND $newRunningOrder")
            }
            runningOrder = newRunningOrder
            runningOrder = runningOrder
            post()
        }
    }

    fun shiftHeight(newHeightCode: String) {
        if (newHeightCode != jumpHeightCode) {
            dbTransaction {
                dbExecute("UPDATE entry SET runningOrder=runningOrder-1 WHERE idAgilityClass=$idAgilityClass AND `group`=${group.quoted} AND jumpHeightCode=${jumpHeightCode.quoted} AND runningOrder>$runningOrder")
                dbExecute("UPDATE entry SET runningOrder=runningOrder+1 WHERE idAgilityClass=$idAgilityClass AND `group`=${group.quoted} AND jumpHeightCode=${newHeightCode.quoted} AND runningOrder>=$runningOrder")
                val maxRunningOrder = AgilityClass(idAgilityClass).nextRunningOrder(newHeightCode)
                if (runningOrder > maxRunningOrder) {
                    runningOrder = maxRunningOrder
                }
                heightCode = newHeightCode
                jumpHeightCode = newHeightCode
                runningOrderJumpHeightCode = newHeightCode
                post()
            }
        }
    }

    fun withdrawRunningOrder() {
        dbTransaction {
            dbExecute("UPDATE entry SET runningOrder=runningOrder-1 WHERE idAgilityClass=$idAgilityClass AND jumpHeightCode=${jumpHeightCode.quoted} AND `group`=${group.quoted} AND runningOrder>$runningOrder")
            savedRunningOrder = runningOrder
            savedProgress = progress
            runningOrder = 0
            progress = PROGRESS_ENTRY_DELETED
            post()
        }
    }

    fun unwithdrawRunningOrder() {
        if (savedRunningOrder > 0 && progress == PROGRESS_ENTRY_DELETED) {
            dbTransaction {
                dbExecute("UPDATE entry SET runningOrder=runningOrder+1 WHERE idAgilityClass=$idAgilityClass AND jumpHeightCode=${jumpHeightCode.quoted} AND `group`=${group.quoted} AND runningOrder>=$savedRunningOrder")
                runningOrder = savedRunningOrder
                progress = savedProgress
                post()
            }
        }
    }

    fun selectEntries(idAgilityClass: Int, jumpHeightCode: String = "", template: ClassTemplate) {
        agilityClass.joinToParent()
        team.joinToParent()
        team.dog.joinToParent()
        team.competitor.joinToParent()

        var where = "entry.idAgilityClass=$idAgilityClass AND entry.progress<$PROGRESS_WITHDRAWN"
        if (!jumpHeightCode.isEmpty()) {
            where += " AND entry.jumpHeightCode=${jumpHeightCode.quoted}"
        }
        val orderBy = when (template) {
            ClassTemplate.TEAM -> "team.teamName"
            ClassTemplate.SPLIT_PAIRS -> "JSON_EXTRACT(team.extra, '$.members[0].competitorName')"
            else -> "if(team.CompetitorName<>'', team.CompetitorName, concat(competitor.givenName, ' ', competitor.familyName))"
        }
        select(where, orderBy)
    }

    fun selectScoreBoardByHeight(agilityClass: AgilityClass, jumpHeightCode: String, childClass: AgilityClass = agilityClass) {
        this.agilityClass.joinToParent("idAgilityClass", "extra", "classCode", "heights", "idAgilityClassParent")
        team.joinToParent("idTeam", "idCompetitor", "idDog", "teamType", "competitorName", "extra")
        team.dog.joinToParent("idDog", "petName", "registeredName")
        team.competitor.joinToParent("idCompetitor", "givenName", "familyName")

        val maxColumn = childClass.template.impliedMaxColumn
        val subResultsFlag = 0.setToBit(maxColumn - 1)
        var where = if (childClass == agilityClass)
            "entry.idAgilityClass=${agilityClass.id} AND entry.progress=$PROGRESS_RUN"
        else
            "entry.idAgilityClass=${agilityClass.id} AND entry.subResultsFlag & $subResultsFlag = $subResultsFlag"

        if (!agilityClass.combineHeights) {
            where += " AND entry.jumpHeightCode=${jumpHeightCode.quoted}"
        }
        val orderBy =
            "entry.subDivision, ${agilityClass.template.resultsOrderBy}, IF(team.competitorName <> '', team.competitorName, CONCAT(competitor.givenName, ' ', competitor.familyName))"
        select(where, orderBy)
    }

    fun selectScoreBoardBySubClass(agilityClass: AgilityClass, subClass: Int) {
        this.agilityClass.joinToParent("idAgilityClass", "extra", "classCode", "subClasses")
        team.joinToParent("idTeam", "idCompetitor", "idDog", "teamType", "competitorName", "extra")
        team.dog.joinToParent("idDog", "petName", "registeredName")
        team.competitor.joinToParent("idCompetitor", "givenName", "familyName")
        var where =
            "entry.idAgilityClass=${agilityClass.id} AND entry.subClass=$subClass AND entry.progress=$PROGRESS_RUN"
        val orderBy =
            "${agilityClass.template.resultsOrderBy}, IF(team.competitorName <> '', team.competitorName, CONCAT(competitor.givenName, ' ', competitor.familyName))"
        select(where, orderBy)
    }

    fun getSnookerButtonData(letter: Char): ButtonData {
        val caption = obstacleToCaption(getObstacleFromLetter(letter), false)
        var data = ButtonData(false, false, "")
        if (isNFC || isEliminated) {
            return ButtonData(false, false, caption)
        } else {
            val inSnookerSequence = isInSnookerSequence
            val lastObstacle = lastObstacleNumber
            if (letter == OBSTACLE_1) {
                val count = obstacleCount(scoreCodes, OBSTACLE_1)
                return ButtonData(count < 3 && !inSnookerSequence, false, caption)
            } else if (letter == OBSTACLE_2) {
                return ButtonData(isNoScore || lastObstacle == 1 || !inSnookerSequence && lastObstacle > 1, false, caption)
            } else if (letter == OBSTACLE_3) {
                return ButtonData(lastObstacle == 1 || lastObstacle == 2, false, caption)
            } else {
                return ButtonData(lastObstacle == 1 || inSnookerSequence && lastObstacle == getObstacleFromLetter(letter) - 1, false, caption)
            }
        }
        return ButtonData(true, false, caption)
    }

    fun getGamblersButtonData(letter: Char): ButtonData {
        val caption = obstacleToCaption(getObstacleFromLetter(letter), false)
        if (isNFC || isEliminated) {
            return ButtonData(false, false, caption)
        } else {
            val count = obstacleCount(scoreCodes, letter)
            when (count) {
                0 -> return ButtonData(!hasGambled, false, caption)
                1 -> return ButtonData(!hasGambled, true, caption)
                else -> return ButtonData(false, true, caption)
            }
        }
        return ButtonData(true, false, caption)
    }

    fun getSnakesButtonAllowed(obstacle: Char): Boolean {
        if (isNFC || isEliminated) {
            return false
        } else {
            return !haveObstacle(scoreCodes, obstacle)
        }
    }

    fun convertToCredit() {
        if (canConvert) {
            dbTransaction {
                previousProgress = progress
                progress = PROGRESS_CONVERTED_TO_CREDIT
                post()
                val shoppingList = ShoppingList()
                shoppingList.addLateEntry(LATE_ENTRY_TRANSFER, 1)
                shoppingList.post(Competition.current.id, idAccount, today, PAYMENT_UNDEFINED)
            }
        }
    }

    fun swapResults(swapIdEntry: Int) {
        val swap = Entry()
        swap.find(swapIdEntry)
        if (swap.found()) {
            this.saveOldScore("Swap_${swap.id}")
            swap.saveOldScore("Swap_${this.id}")

            val progress = this.progress
            val queueSequence = this.queueSequence
            val scoreCodes = this.scoreCodes
            val gamesScore = this.gamesScore
            val gamesBonus = this.gamesBonus
            val qualifying = this.qualifying
            val courseFaults = this.courseFaults
            val time = this.time
            val noTime = this.noTime
            val timeFaults = this.timeFaults
            val faults = this.faults
            val place = this.place
            val placeFlags = this.placeFlags
            val progressionPoints = this.progressionPoints
            val finalized = this.finalized
            val runStart = this.runStart
            val runEnd = this.runEnd
            val hasRun = this.hasRun
            val courseTime = this.courseTime
            val points = this.points

            this.progress = swap.progress
            this.queueSequence = swap.queueSequence
            this.scoreCodes = swap.scoreCodes
            this.gamesScore = swap.gamesScore
            this.gamesBonus = swap.gamesBonus
            this.qualifying = swap.qualifying
            this.courseFaults = swap.courseFaults
            this.time = swap.time
            this.noTime = swap.noTime
            this.timeFaults = swap.timeFaults
            this.faults = swap.faults
            this.place = swap.place
            this.placeFlags = swap.placeFlags
            this.progressionPoints = swap.progressionPoints
            this.finalized = swap.finalized
            this.runStart = swap.runStart
            this.runEnd = swap.runEnd
            this.hasRun = swap.hasRun
            this.courseTime = swap.courseTime
            this.points = swap.points

            swap.progress = progress
            swap.queueSequence = queueSequence
            swap.scoreCodes = scoreCodes
            swap.gamesScore = gamesScore
            swap.gamesBonus = gamesBonus
            swap.qualifying = qualifying
            swap.courseFaults = courseFaults
            swap.time = time
            swap.noTime = noTime
            swap.timeFaults = timeFaults
            swap.faults = faults
            swap.place = place
            swap.placeFlags = placeFlags
            swap.progressionPoints = progressionPoints
            swap.finalized = finalized
            swap.runStart = runStart
            swap.runEnd = runEnd
            swap.hasRun = hasRun
            swap.courseTime = courseTime
            swap.points = points

            this.post()
            swap.post()
        }
    }

    fun tiedTeamList(isUKA: Boolean): String {
        if (isEliminated || isNFC || noTime) {
            return ""
        } else {
            val where =
                if (isUKA)
                    "idAgilityClass=$idAgilityClass AND jumpHeightCode=${jumpHeightCode.quoted} AND time=$time AND points=$points AND idEntry<>$id AND NOT noTime"
                else
                    "idAgilityClass=$idAgilityClass AND subClass=$subClass AND time=$time AND points=$points AND idEntry<>$id AND NOT noTime"
            val query = DbQuery("SELECT group_concat(idTeam) as idTeamList FROM entry WHERE $where")
            query.first()
            return query.getString("idTeamList")
        }
    }

    fun getScoreData(abbreviated: Boolean, isScoring: Boolean = false): ScoreData {
        val courseTime =
            if (agilityClass.isUkaStyle || agilityClass.isFabStyle)
                agilityClass.getCourseTime(jumpHeightCode)
            else
                agilityClass.getSubClassCourseTime(subClass)

        return ScoreData(scoreCodes, time, progress == PROGRESS_RUN, noTime, agilityClass, courseTime, abbreviated, isScoring)
    }

    fun subResultFaultsText(index: Int): String {
        return if (subResults[index]["hasRun"].asBoolean) "${subResults[index]["faults"].asInt.dec3Int}" else ""
    }

    fun subResultFaultsTimeText(index: Int): String {
        return if (subResults[index]["hasRun"].asBoolean) "${subResults[index]["faults"].asInt.dec3Int} (${subResults[index]["time"].asInt.dec3})" else ""
    }

    fun subResultPointsText(index: Int): String {
        return if (subResults[index]["hasRun"].asBoolean) "${subResults[index]["points"].asInt.toString()}" else ""
    }

    fun subResultPointsDec3(index: Int): String {
        return if (subResults[index]["hasRun"].asBoolean) "${subResults[index]["points"].asInt.dec3}" else ""
    }

    companion object {
        fun fixIds() {
            dbExecute("""
                SELECT 
                    @id:=if(MAX(idEntry) IS NULL, 0, MAX(idEntry))
                FROM
                    entry
                WHERE
                    idEntry < ${Int.MAX_VALUE / 2};    
            """.trimIndent())

            dbExecute("UPDATE entry SET flag = 0 WHERE idEntry > @id")
            
            dbExecute("""
                UPDATE entry
                        JOIN
                    agilityClass USING (idAgilityClass)
                        JOIN
                    competition USING (idCompetition) 
                SET 
                    entry.flag = 1
                WHERE
                    competition.dateEnd >= CURDATE() AND competition.processed;
            """.trimIndent())
            
            dbExecute("""
                UPDATE entry 
                SET 
                    idEntry = (@id:=@id + 1)
                WHERE
                    idEntry > @id AND flag = 0 ORDER BY dateCreated , idEntry; 
            """.trimIndent())

            dbExecute("UPDATE entry SET flag = 0 WHERE flag=1")
        }

        fun select(where: String, orderBy: String = "", limit: Int = 0): Entry {
            val entry = Entry()
            entry.select(where, orderBy, limit)
            return entry
        }

        fun process(idAccount: Int, idCompetition: Int, body: () -> Unit) {
            body()
        }

        fun summary(idAccount: Int, idCompetition: Int): DbQuery {
            return DbQuery(
                """
                SELECT
                    dog.idDog, dog.petName, count(*) AS quantity, entry.entryFee, sum(runUnits) as runUnits, 
                    group_concat(classDate, ":", entry.entryFee ORDER BY classDate) as dateFee
                FROM
                    entry
                        JOIN
                    agilityClass USING (idAgilityClass)
                        JOIN
                    team USING (idTeam)
                        JOIN
                    dog USING (idDog)
                WHERE
                    entry.idAccount = $idAccount
                        AND idCompetition = $idCompetition
                        and entry.entryType<>$ENTRY_INVITE
                GROUP BY
                    petName, dog.idDog, entry.entryFee
            """
            )
        }


        fun inviteUkaFinals(idEntry: Int, reInvited: Boolean, unCancelled: Boolean) {
            dbTransaction {
                Entry()
                    .join { agilityClass }
                    .join { team }
                    .join { account }
                    .join { agilityClass.competition }
                    .join { account.competitor }
                    .join { team.dog }
                    .join { team.competitor }
                    .join { linkedEntry }
                    .seek(idEntry) {
                        if (!invite || reInvited || unCancelled) {
                            val entryHeightCode = heightCode
                            val entryGradeCode = gradeCode
                            val entryIdTeam = idTeam
                            val entryIdAccount = idAccount
                            invite = true
                            dontInvite = false
                            ukaFinalsCode = agilityClass.ukaFinalsCode
                            val template = ClassTemplate.select(ukaFinalsCode)
                            val event = "${template.sponsor} ${template.nameTemplate}"
                            val proposedEntryType = if (linkedEntry.id>0) linkedEntry.type else ENTRY_INVITE
                            val proposedProgress = if (linkedEntry.id>0) linkedEntry.progress else PROGRESS_INVITED
                            AgilityClass().seek("idCompetition=1749893004 AND classCode=${template.code}") {
                                idEntryLinked = enter(
                                    idTeam = entryIdTeam, heightCode = entryHeightCode, gradeCode = entryGradeCode, 
                                    entryType = proposedEntryType, progress = proposedProgress, idAccount = entryIdAccount, 
                                    fee = 0, grandFinals = true, invite = true
                                )
                            }
                            post()
                            if (unCancelled) {
                                ukaFinalsUnCancelled(account, team, event)
                            } else if (reInvited) {
                                ukaFinalsReInvited(account, team, event)
                            } else {
                                ukaFinalsInvite(account, team, event, agilityClass.date)
                            }
                        }
                    }
            }
        }

        fun dontInviteUkaFinals(idEntry: Int, idEntryFinals: Int) {
            var isUninvited=false
            var isCancelled=false
            dbTransaction {
                Entry().seek(idEntry) {
                    invite = false
                    dontInvite = true
                    post()
                }
                if (idEntryFinals>0) {
                    Entry().join { agilityClass }.join { team }.join { account }.seek(idEntryFinals) {
                        if (invited && !uninvited) isUninvited = true
                        if (entered && !cancelled) isCancelled = true
                        invited = false
                        uninvited = true
                        if (entered) cancelled = true
                        post()
                        val template = ClassTemplate.select(agilityClass.ukaFinalsCode)
                        val event = "${template.sponsor} ${template.nameTemplate}"
                        if (isCancelled) {
                            ukaFinalsCancelled(account, team, event)
                        } else if (isUninvited) {
                            ukaFinalsUninvited(account, team, event)
                        }
                    }
                }
            }
        }

    }

}


class ScoreData(scoreCodes: String, time: Int, hasRun: Boolean, noTime: Boolean, agilityClass: AgilityClass, courseTime: Int, abbreviated: Boolean, isScoring: Boolean = false) {

    private val qualifyingPoints = agilityClass.qualifyingPoints
    private val batonFaults = if (agilityClass.batonFaults > 0) agilityClass.batonFaults else 10

    val classTemplate = agilityClass.template
    var time: Int = 0
    var gamesScore: Int = 0
    var gamesBonus: Int = 0

    var runnerFaults: IntArray = intArrayOf(0, 0, 0, 0)
    private var runnerData = ""
    var timeFaults: Int = 0
    var faults: Int = 0
    var qualifying: Boolean = false
    var scoreText: String = ""
    var runData: String = ""
    var points: Int = 0
    var courseTime: Int = 0
    var runners = 0

    var isClosing = false
    var runnerDataClosing = ""

    var bonusScoreCodes = agilityClass.gambleBonusObstacles

    private var isNfc: Boolean = false
    private var isEliminated: Boolean = false
    private var snakes = 0
    private var ladders = 0
    private var jumps = 0
    private var refusals = 0

    private var obstaclePoints = agilityClass.obstaclePoints.default("55433222222")

/*
    private var gamblersPoints =
            if (classTemplate.isGamblers && agilityClass.extra["gamblersPoints"].asString.isNotEmpty())
                agilityClass.extra["gamblersPoints"].asString
            else if (specialRules==1)
                "55432222222"
            else
                "55433222222"
*/

    private var runner = 0

    var courseFaults = 0

    var timeDeduct = 0


    init {
        this.courseTime = courseTime

        for (i in 0..scoreCodes.length - 1) {
            val obstacle = getObstacleFromLetter(scoreCodes[i])
            val digit = getDigitFromLetter(scoreCodes[i])
            when {
                isNfc || isEliminated -> {
                    // ignore other score codes
                }
                digit >= 0 -> {
                    runnerData = runnerData.append(digit.toString())
                    gamesScore = gamesScore * 10 + digit
                }
                obstacle > 0 -> {
                    if (isClosing) {
                        runnerData =
                            runnerData.append("c" + obstacleToCaption(obstacle, classTemplate.isSnakesAndLadders))
                    } else {
                        runnerData = runnerData.append(obstacleToCaption(obstacle, classTemplate.isSnakesAndLadders))
                    }
                    when {
                        classTemplate.isGamblers -> {
                            val index = scoreCodes[i].toInt() - 'a'.toInt()
                            val points =
                                if (index < obstaclePoints.length) obstaclePoints[index].toString().toInt() else 1
                            if (isClosing) {
                                gamesBonus += points
                            } else {
                                gamesScore += points
                            }
                        }
                        classTemplate.isSnooker -> {
                            gamesScore += obstacle
                        }
                        classTemplate.isSnakesAndLadders -> {
                            gamesScore++
                            when (obstacle) {
                                in 1..4 -> snakes++
                                in 5..8 -> ladders++
                                else -> jumps++
                            }
                        }
                    }
                }
                else -> {
                    when (scoreCodes[i]) {
                        SCORE_GAMBLE_1 -> {
                            if (isClosing) runnerData = runnerDataClosing
                            if (isClosing) gamesBonus = 0
                            gamesBonus += agilityClass.gamble1
                            runnerData = runnerData.append("G${agilityClass.gamble1}")
                        }
                        SCORE_GAMBLE_2 -> {
                            if (isClosing) runnerData = runnerDataClosing
                            if (isClosing) gamesBonus = 0
                            gamesBonus += agilityClass.gamble2
                            runnerData = runnerData.append("G${agilityClass.gamble2}")
                            timeDeduct = agilityClass.template.gamble2TimeDeduct
                        }
                        SCORE_BONUS -> {
                            runnerData = runnerData.append("B${agilityClass.gambleBonusScore}")
                            gamesScore += agilityClass.gambleBonusScore
                        }
                        SCORE_NO_GAMBLE -> {
                            runnerData = runnerData.append("No")
                            gamesBonus = 0
                        }
                        SCORE_FAULT -> {
                            runnerData = runnerData.append("5")
                            runnerFaults[runner] += 5
                        }
                        SCORE_REFUSAL -> {
                            runnerData = runnerData.append("R")
                            refusals++
                            runnerFaults[runner] += 5
                        }
                        SCORE_HANDLING -> {
                            runnerData = runnerData.append("H")
                            runnerFaults[runner] += 5
                        }
                        SCORE_POLE -> {
                            runnerData = runnerData.append("P")
                            runnerFaults[runner] += 5
                        }
                        SCORE_BATON_FAULT -> {
                            runnerData = runnerData.append("B")
                            runnerFaults[runner] += if (batonFaults > 0) batonFaults else 10
                        }
                        SCORE_CLOSING_SEQUENCE -> {
                            isClosing = true
                            runnerDataClosing = runnerData
                        }
                        SCORE_BATON_CHANGE -> {
                            if (runnerFaults[runner] == 0) {
                                runnerData = "C"
                            }
                            runData += runnerData + " | "
                            //                            runData = runData.append(runnerData, " | ")
                            runnerData = ""
                            runner++
                        }
                        SCORE_ELIMINATE_FAULT -> {
                            runnerData = runnerData.append("E")
                            if (classTemplate == ClassTemplate.KC_CRUFTS_TEAM) {
                                runnerFaults[runner] += 100
                            } else {
                                runnerFaults[runner] = 100
                            }
                        }
                        SCORE_ELIMINATE -> {
                            refusals = 0
                            if (runnerData == "") {
                                runnerData = "E"
                            } else {
                                runnerData = "E ($runnerData)"
                            }
                            runnerFaults[runner] = FAULTS_ELIMINATED
                            isEliminated = true
                        }
                        SCORE_CLEAR -> {
                            if (timeFaults == 0) {
                                if (!abbreviated) {
                                    runData = "CLEAR"
                                }
                            }
                            runnerFaults[runner] = 0
                        }
                        SCORE_NFC -> {
                            runnerData = "NFC"
                            runnerFaults[runner] = FAULTS_NFC
                            isNfc = true
                        }
                    }
                }
            }
        }

        if (bonusScoreCodes.isNotEmpty()) {
            var hasBonus = true
            bonusScoreCodes.forEach {
                if (!scoreCodes.contains(it)) hasBonus = false
            }
            if (hasBonus) {
                runnerData = runnerData.append("B${agilityClass.gambleBonusScore}")
                gamesScore += agilityClass.gambleBonusScore
            }
        }

        if (classTemplate.isRelay && runnerData.isEmpty() && !isScoring) {
            runnerData = "C"
        }

        runData += runnerData

        runners = runner + 1
        courseFaults = runnerFaults[0] + runnerFaults[1] + runnerFaults[2] + runnerFaults[3]

        if (!isEliminated && !isNfc && time > (courseTime - timeDeduct) && courseTime > 0 && !classTemplate.noTimeFaults) {
            timeFaults = time - (courseTime - timeDeduct)
            runData = runData.append("T" + timeFaults.dec3)
        }
        faults = courseFaults * classTemplate.faultToTime + timeFaults


        if (refusals >= 3 && !classTemplate.isRelay) {
            runData = "E? ($runData)"
        }

        when {
            classTemplate.isGamblers -> {
                gamesBonus = if (timeFaults > 0) 0 else gamesBonus
                qualifying = !isEliminated && !isNfc && gamesBonus > 0 && gamesScore + gamesBonus >= qualifyingPoints
            }
            classTemplate.isSnooker -> {
                qualifying =
                    !isEliminated && !isNfc && gamesScore >= (if (qualifyingPoints > 0) qualifyingPoints else 37)
            }
            classTemplate.isTimeOutAndFault -> {
                qualifying = !isEliminated && !isNfc && gamesScore >= qualifyingPoints
                runData = if (courseFaults == FAULTS_ELIMINATED) "E" else ""
            }
            classTemplate.isSnakesAndLadders -> {
                qualifying = !isEliminated && !isNfc && gamesScore >= 14 && snakes == 4 && ladders == 4
            }
            else -> {
                qualifying = courseFaults == 0 && timeFaults == 0
            }
        }

        if (!agilityClass.isUka) {
            qualifying = true
        }

        scoreText = when {
            isNfc ->
                ""
            isEliminated ->
                "E"
            classTemplate.isGamblers ->
                if (gamesBonus > 0) "$gamesScore+$gamesBonus" else if (timeFaults > 0) "$gamesScore+T" else "$gamesScore"
            classTemplate.isSnooker ->
                "$gamesScore"
            classTemplate.isSnakesAndLadders ->
                if (qualifying) "${gamesScore}q" else "$gamesScore"
            classTemplate.isTimeOutAndFault ->
                if (scoreCodes.isEmpty()) "N/A" else "$gamesScore"
            else ->
                if (timeFaults > 0) faults.dec3 else "$courseFaults"
        }

        if (isEliminated || isNfc || !hasRun) {
            gamesScore = 0
            gamesBonus = 0
        }

        this.time = when {
            noTime || isNfc ->
                courseTime
            isEliminated ->
                if (classTemplate.eliminationTime == 0) courseTime else classTemplate.eliminationTime
            else ->
                time
        }

        points =
            classTemplate.getPoints(courseTime, time, faults, gamesScore + gamesBonus, isEliminated, isNfc || !hasRun)
    }


}

