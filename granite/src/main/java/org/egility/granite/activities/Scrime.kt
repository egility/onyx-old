/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.granite.activities

import org.egility.granite.fragments.*
import kotlinx.android.synthetic.main.page_scrime.*
import org.egility.library.dbobject.Competition
import org.egility.library.dbobject.ScrimeList
import org.egility.library.dbobject.Team
import org.egility.library.general.*
import org.egility.android.BaseActivity
import org.egility.android.BaseFragment
import org.egility.android.tools.AndroidUtils.goneIf
import org.egility.android.tools.Signal
import org.egility.android.tools.SignalCode
import org.egility.granite.R

open class Scrime : BaseActivity(R.layout.page_scrime) {
    private lateinit var scoreFragment: ScoreFragment
    private lateinit var scoreGamblersFragment: ScoreFragmentGamblers
    private lateinit var scoreGamblers24Fragment: ScoreFragmentGamblers24
    private lateinit var scoreSnookerFragment: ScoreFragmentSnooker
    private lateinit var scoreTimeOutFragment: ScoreFragmentTimeOut
    private lateinit var scoreSnakesFragment: ScoreFragmentSnakes
    private lateinit var timeFragment: TimeFragment
    private lateinit var waitFragment: WaitFragment
    private lateinit var scrimeListFragment: ScrimeListFragment
    private lateinit var entryStatusFragment: EntryStatusFragment
    private lateinit var swapDogListFragment: SwapDogListFragment
    private lateinit var scrimeCompetitorFragment: ScrimeCompetitorFragment
    private lateinit var changeHandlerFragmentKc : ChangeHandlerFragmentKc
    private lateinit var changeHandlerFragmentUka : ChangeHandlerFragmentUka


    init {
        if (!dnr) {
            scoreFragment = ScoreFragment()
            scoreGamblersFragment = ScoreFragmentGamblers()
            scoreGamblers24Fragment = ScoreFragmentGamblers24()
            scoreSnookerFragment = ScoreFragmentSnooker()
            scoreTimeOutFragment = ScoreFragmentTimeOut()
            scoreSnakesFragment = ScoreFragmentSnakes()
            timeFragment = TimeFragment()
            waitFragment = WaitFragment()
            scrimeListFragment = ScrimeListFragment()
            entryStatusFragment = EntryStatusFragment()
            swapDogListFragment = SwapDogListFragment()
            scrimeCompetitorFragment = ScrimeCompetitorFragment()
            changeHandlerFragmentKc = ChangeHandlerFragmentKc()
            changeHandlerFragmentUka = ChangeHandlerFragmentUka()
        }
    }

    private var initialized = false
    private var isScoring = true

    private val agilityClass
        get() = ringPartyData.agilityClass

    private var multiCourseTimes = false
    
    val fromPaper = this is ScrimePaper


    public override fun whenInitialize() {
        agilityClass.post()
        agilityClass.refresh()
        val idEntry = intent.getIntExtra("idEntry", -1)
        val hasRun = intent.getBooleanExtra("hasRun", false)
        if (fromPaper) {
            ringPartyData.entrySelected(idEntry)
            ringPartyData.scrimeMode = !hasRun
        } else {
            if (idEntry > 0) {
                ringPartyData.scrimeMode = false
                ringPartyData.entrySelected(idEntry)
            } else {
                ringPartyData.scrimeMode = true
            }
            ringPartyData.jumpHeightCode = ringPartyData.ring.heightCode
        }
    }

    public override fun whenResume() {
        showClass()
    }

    private fun showClass() {
        if (agilityClass.jumpHeightArray.size > 1) {
            setActiveFragmentTitle(
                "Scrime (Ring ${ringPartyData.ring.number}): ${agilityClass.name} (${agilityClass.getHeightCaption(
                    ringPartyData.jumpHeightCode
                )})"
            )
        } else {
            setActiveFragmentTitle("Scrime (Ring ${ringPartyData.ring.number}): ${agilityClass.name}")
        }
        if (agilityClass.isUkaStyle || agilityClass.isFabStyle) {
            tvCourseTime.text =
                    "Course Time: %ds".format(agilityClass.getCourseTime(ringPartyData.jumpHeightCode) / 1000)
            tvCourseTimeGames.text =
                    "Course Time: %ds".format(agilityClass.getCourseTime(ringPartyData.jumpHeightCode) / 1000)
        } else {
            multiCourseTimes = false
            tvCourseTime.text = "Course Time: %ds".format(agilityClass.courseTime / 1000)

            /*
            var courseTime = -1
            for (index in 0..agilityClass.subClassCount - 1) {
                if (agilityClass.getSubClassCourseTime(index) != courseTime) {
                    if (courseTime != -1) {
                        multiCourseTimes = true
                    }
                    courseTime = agilityClass.getSubClassCourseTime(index)
                }
            }
            if (!multiCourseTimes) {
                tvCourseTime.text = "Course Time: ${courseTime / 1000}s"
            }
            */
        }
        ringPartyData.jumpHeightCode = ringPartyData.ring.heightCode
    }

    fun doCheckTied() {
        if (agilityClass.template.flagTies) {
            val idTeamList = ringPartyData.entry.tiedTeamList(agilityClass.isUka)
            if (idTeamList.isNotEmpty()) {
                val team = Team()
                var names = ringPartyData.entry.team.description
                team.select("idTeam IN ($idTeamList)")
                while (team.next()) {
                    names = names.append(team.fullDescription, " and\n")
                }
                if (Competition.isUka) {
                    popUp(
                        "Important",
                        "There is a tie between:\n\n$names.\n\nPlease notify the judge and ring manager immediately"
                    )
                } else {
                    popUp(
                        "Important",
                        "There is a tie between:\n\n$names.\n\nIf this is going to affect the placings there may need to be a run-off. Please make a note of these names and notify the ring manager so they can check."
                    )
                }
            }
        }
    }

    override fun whenBeforeLoadFragment(fragment: BaseFragment) {
        if (agilityClass.jumpHeightArray.size > 1) {
            fragment.title =
                    "Scrime (Ring ${ringPartyData.ring.number}): ${agilityClass.name} (${agilityClass.getHeightCaption(
                        ringPartyData.jumpHeightCode
                    )})"
        } else {
            fragment.title = "Scrime (Ring ${ringPartyData.ring.number}): ${agilityClass.name}"
        }
        val alwaysHideTop = fragment.oneOf(
            scrimeListFragment,
            entryStatusFragment,
            swapDogListFragment,
            changeHandlerFragmentKc,
            changeHandlerFragmentUka
        ) || !ringPartyData.isEntrySelected
        goneIf(!agilityClass.isExtendedScoreGame || alwaysHideTop, loTopTextGames)
        goneIf(agilityClass.isExtendedScoreGame || alwaysHideTop, loTopText)
        goneIf(agilityClass.template.teamSize == 2, loIndividual)
        goneIf(agilityClass.template.teamSize != 2, loPair)
        goneIf(fragment == scrimeCompetitorFragment, loScore)
        isScoring = fragment.oneOf(
            scoreFragment,
            scoreGamblersFragment,
            scoreGamblers24Fragment,
            scoreSnookerFragment,
            scoreTimeOutFragment,
            scoreSnakesFragment,
            scrimeCompetitorFragment
        )
    }

    fun updateDisplay() {
        with(ringPartyData) {
            if (multiCourseTimes) {
                tvCourseTime.text = "Course Time (${agilityClass.subClassDescription(
                    entry.subClass,
                    shortGrade = true
                )}): ${agilityClass.getSubClassCourseTime(entry.subClass) / 1000}s".format()
            }

            when (agilityClass.template) {
                ClassTemplate.TEAM_INDIVIDUAL -> {
                    tvCompetitor.text = entry.team.getCompetitorName(entry.teamMember)
                    tvDog.text = entry.team.getDogName(entry.teamMember)
                }
                ClassTemplate.TEAM_RELAY, ClassTemplate.KC_CRUFTS_TEAM -> {
                    tvCompetitor.text = entry.team.teamName
                    tvDog.text = ""
                    tvTeam.text = ""
                    tvPair1.text = ""
                    tvPair2.text = ""
                }
                else -> {
                    if (Competition.current.isUka) {
                        tvCompetitor.text = entry.competitorName
                    } else {
                        tvCompetitor.text = "${entry.runningOrder} ${entry.competitorName}"
                    }
                    tvDog.text = entry.dogName
                    tvTeam.text = entry.teamDescription
                    if (Competition.current.isUka) {
                        tvPair1.text = entry.team.getCompetitorDog(1, true)
                        tvPair2.text = entry.team.getCompetitorDog(2, true)
                        if (agilityClass.isScoreBasedGame) {
                            tvRunDataGames.text = entry.runData
                            tvScoreTextGames.text = entry.scoreText
                            tvRunData.text = entry.scoreTextNFC
                        }
                    } else {
                        tvPair1.text = entry.team.getDogName(1, false)
                        tvPair2.text = entry.team.getDogName(2, false)
                    }
                }
            }

        }
    }

    override fun whenSignal(signal: Signal) {
        with(ringPartyData) {
            when (signal.signalCode) {
                SignalCode.RESET -> {
                    defaultFragmentContainerId = R.id.loContent
                    if (fromPaper && !initialized) {
                        sendSignal(SignalCode.ENTRY_SELECTED)
                        signal.consumed()
                    } else if (!scrimeMode && !initialized) {
                        sendSignal(SignalCode.ENTRY_SELECTED)
                        signal.consumed()
                    } else if (fromPaper || !scrimeMode && initialized) {
                        finish()
                        signal.consumed()
                    } else {
                        val idEntry = agilityClass.getNextRunnerIdEntry(ring.group, ring.heightCode)
                        entrySelected(idEntry)
                        updateDisplay()
                        sendSignal(SignalCode.UPDATE_STATUS)
                        if (isActiveFragment(scrimeCompetitorFragment)) {
                            whenBeforeLoadFragment(scrimeCompetitorFragment)
                            sendSignal(SignalCode.RESET_FRAGMENT, queued = true)
                        } else {
                            loadTopFragment(scrimeCompetitorFragment)
                        }
                        signal.consumed()
                    }
                    initialized = true
                }
                SignalCode.SCRIME_QUEUE -> {
                    val push = signal._payload as? Boolean ?: false
                    entry.release()
                    scrimeListFragment.selector = ScrimeList.QUEUE
                    if (push) {
                        loadFragment(scrimeListFragment)
                    } else {
                        loadTopFragment(scrimeListFragment)
                    }
                }
                SignalCode.SCRIME_NOT_RUN -> {
                    val push = signal._payload as? Boolean ?: false
                    entry.release()
                    scrimeListFragment.selector = ScrimeList.NOT_RUN
                    if (push) {
                        loadFragment(scrimeListFragment)
                    } else {
                        loadTopFragment(scrimeListFragment)
                    }
                }
                SignalCode.SCRIME_RUN -> {
                    val push = signal._payload as? Boolean ?: false
                    entry.release()
                    scrimeListFragment.selector = ScrimeList.RUN
                    if (push) {
                        loadFragment(scrimeListFragment)
                    } else {
                        loadTopFragment(scrimeListFragment)
                    }
                }
                SignalCode.SCRIME_OTHER -> {
                    val push = signal._payload as? Boolean ?: false
                    entry.release()
                    scrimeListFragment.selector = ScrimeList.OTHER
                    if (push) {
                        loadFragment(scrimeListFragment)
                    } else {
                        loadTopFragment(scrimeListFragment)
                    }
                }
                SignalCode.ENTRY_SELECTED -> {
                    updateDisplay()

                    if (team.isRedCarded) {
                        Global.services.popUp(
                            "Warning",
                            "This competitor is blocked from competing at the moment. Please contact the show secretary to resolve."
                        )
                    } else if (Competition.enforceMembership && team.hasMembershipIssues && !agilityClass.isTryout) {
                        Global.services.popUp("Membership Issue", "This competitor needs to report to the show secretary.\n\nThey may not compete at the moment.")
                    } else if (fromPaper && !entry.hasRun) {
                        sendSignal(SignalCode.ENTER_SCORE)
                    } else if (!fromPaper && entry.canStartRun && scrimeMode) {
                        loadTopFragment(scrimeCompetitorFragment)
                    } else {
                        sendSignal(SignalCode.ENTRY_OPTIONS)
                    }
                    signal.consumed()
                }
                SignalCode.ENTRY_OPTIONS -> {
                    loadFragment(entryStatusFragment)
                }
                SignalCode.ENTER_SCORE -> {
                    if (!editMode && !fromPaper) {
                        if (agilityClass.strictRunningOrder) {
                            ringPartyData.setRunner(
                                entry.id,
                                entry.runningOrder,
                                entry.teamDescription,
                                entry.jumpHeightCode
                            )
                        }
                        entry.startRun(reRunMode)
                    }
                    if (reRunMode) {
                        entry.time = 0
                        entry.noTime = false
                    }
                    isScoring = true
                    sendSignal(SignalCode.UPDATE_STATUS)
                    if (agilityClass.template.nfcOnly) {
                        ringPartyData.entry.addScoreCode(SCORE_NFC)
                        sendSignal(SignalCode.WAIT_FINISH)
                    } else if (entry.isGamblers) {
                        loadFragment(if (agilityClass.extendedGamblers) scoreGamblers24Fragment else scoreGamblersFragment)
                    } else if (entry.isSnooker) {
                        loadFragment(scoreSnookerFragment)
                    } else if (entry.isTimeOutAndFault) {
                        loadFragment(scoreTimeOutFragment)
                    } else if (entry.isSnakesAndLadders) {
                        loadFragment(scoreSnakesFragment)
                    } else {
                        loadFragment(scoreFragment)
                    }
                    signal.consumed()
                }
                SignalCode.EDIT_SCORE -> {
                    mode = ScrimeMode.EDIT
                    entry.saveOldScore("EditScore")
                    sendSignal(SignalCode.ENTER_SCORE)
                }
                SignalCode.RE_RUN_TIME -> {
                    mode = ScrimeMode.RERUN_TIME
                    entry.saveOldScore("ReRunTime")
                    entry.time = 0
                    entry.noTime = false
                    sendSignal(SignalCode.ENTER_TIME)
                }
                SignalCode.RE_RUN_SCRATCH -> {
                    mode = ScrimeMode.RERUN_SCRATCH
                    entry.saveOldScore("ReRunScratch")
                    entry.scoreCodes = ""
                    entry.time = 0
                    entry.noTime = false
                    entry.hasRun = false
                    sendSignal(SignalCode.ENTER_SCORE)
                }
                SignalCode.WIPE_SCORE -> {
                    entry.saveOldScore("WipeScore")
                    entry.scoreCodes = ""
                    entry.time = 0
                    entry.noTime = false
                    entry.hasRun = false
                    entry.progress = if (Competition.isUka) PROGRESS_BOOKED_IN else PROGRESS_ENTERED
                    entry.post()
                    sendSignal(SignalCode.BACK)
                }
                SignalCode.RE_RUN_TIME_QUESTION -> {
                    whenYes(
                        "Question", "Are you sure you want ${ringPartyData.entry.teamDescription} re-run for " +
                                "time. Their existing faults will be kept, but their time will be erased?"
                    ) {
                        sendSignal(SignalCode.RE_RUN_TIME)
                    }
                }
                SignalCode.RE_RUN_SCRATCH_QUESTION -> {
                    whenYes(
                        "Question", "Are you sure you want ${ringPartyData.entry.teamDescription} re-run from " +
                                "scratch. Their existing faults & time will be erased?"
                    ) {
                        sendSignal(SignalCode.RE_RUN_SCRATCH)
                    }
                }
                SignalCode.WIPE_SCORE_QUESTION -> {
                    whenYes(
                        "Question",
                        "Are you sure you want to wipe the scrime data for ${ringPartyData.entry.teamDescription}. " +
                                "Once you have done this, it will be as if they never ran?"
                    ) {
                        sendSignal(SignalCode.WIPE_SCORE)
                    }
                }
                SignalCode.UPDATE_STATUS -> {
                    if (entry.isOnRow) {
                        if (agilityClass.isScoreBasedGame) {
                            tvRunDataGames.text = entry.runData
                            tvScoreTextGames.text = entry.scoreText
                            tvRunData.text = entry.scoreTextNFC
                        } else {
                            tvRunData.text = entry.getScoreData(false, isScoring).runData
                        }
                        tvTime.text = entry.timeText
                        tvTimeGames.text = entry.timeText
                    } else {
                        tvRunData.text = ""
                        tvRunDataGames.text = ""
                        tvScoreTextGames.text = "0"
                        tvTime.text = "00.000"
                        tvTimeGames.text = "00.000"
                    }
                    signal.consumed()
                }
                SignalCode.SCORE_CHANGED -> {
                    sendSignal(SignalCode.UPDATE_STATUS)
                    if (entry.isOnRow) {
                        if (!editMode && (entry.isNFC || entry.isEliminated)) {
                            sendSignal(SignalCode.WAIT_FINISH)
                        } else if (!editMode && (entry.isClear || entry.hasAchievedGambled)) {
                            sendSignal(SignalCode.ENTER_TIME)
                        }
                    }
                    signal.consumed()
                }
                SignalCode.ENTER_TIME -> {
                    isScoring = false
                    sendSignal(SignalCode.UPDATE_STATUS)
                    loadFragment(timeFragment)
                    signal.consumed()
                }
                SignalCode.WAIT_FINISH -> {
                    isScoring = false
                    loadFragment(waitFragment)
                    signal.consumed()
                }
                SignalCode.SWAP_DOG -> {
                    loadFragment(swapDogListFragment)
                    signal.consumed()
                }
                SignalCode.SWAP_DOG_SELECTED -> {
                    val idEntry = signal._payload as? Int
                    if (idEntry != null) {
                        entry.swapResults(idEntry)
                    }
                    sendSignal(SignalCode.RESET)
                    signal.consumed()
                }

                SignalCode.CHANGE_HANDLER -> {
                    if (Competition.isUka) {
                        loadFragment(changeHandlerFragmentUka)
                    } else {
                        loadFragment(changeHandlerFragmentKc)
                    }
                    signal.consumed()
                }
                SignalCode.HANDLER_CHANGED -> {
                    sendSignal(SignalCode.BACK)
                }

                SignalCode.RUN_COMPLETE -> {
                    if (entry.isOnRow) {
                        if (fromPaper) {
                            entry.endRun(force = true)
                            sendSignal(SignalCode.RESET)
                        } else if (editMode) {
                            entry.editRun()
                            doCheckTied()
                            sendSignal(SignalCode.ENTRY_OPTIONS)
                        } else {
                            if (agilityClass.progress != CLASS_RUNNING) {
                                agilityClass.progress == CLASS_RUNNING
                                agilityClass.post()
                            }
                            entry.endRun()
                            doCheckTied()
                            sendSignal(SignalCode.RESET)
                        }
                    }
                    signal.consumed()
                }
                SignalCode.ACCEPT_EDIT -> {
                    if (entry.isOnRow) {
                        entry.editRun()
                        sendSignal(SignalCode.ENTRY_OPTIONS)
                    }
                    signal.consumed()
                }
                SignalCode.CANCEL_EDIT -> {
                    if (entry.isOnRow) {
                        entry.undoEdits()
                        sendSignal(SignalCode.ENTRY_OPTIONS)
                    }
                    signal.consumed()
                }
                SignalCode.CANCEL_RUN -> {
                    if (entry.isOnRow) {
                        entry.undoEdits()
                        entry.cancelRun()
                        if (reRunMode) {
                            sendSignal(SignalCode.ENTRY_OPTIONS)
                        } else {
                            sendSignal(SignalCode.RESET)
                        }
                    }
                    signal.consumed()
                }
                SignalCode.LEAVE_QUEUE_QUESTION -> {
                    whenYes(
                        "Question",
                        "Are you sure that ${entry.teamDescription} are not ready to run and will return to the queue when they are?"
                    ) {
                        sendSignal(SignalCode.LEAVE_QUEUE)
                    }

                }
                SignalCode.LEAVE_QUEUE -> {
                    entry.checkOut()
                    sendSignal(SignalCode.RESET)
                    signal.consumed()
                }
                SignalCode.HEIGHT_SELECTED -> {
                    val newJumpHeightCode = signal._payload as? String
                    if (newJumpHeightCode != null) {
                        if (ringPartyData.scrimeMode) {
                            scrimeJumpHeightCode = newJumpHeightCode
                        } else {
                            jumpHeightCode = newJumpHeightCode
                        }
                        showClass()
                        if (isActiveFragment(scrimeCompetitorFragment)) {
                            sendSignal(SignalCode.RESET)
                        } else {
                            sendSignal(SignalCode.REFRESH)
                        }
                        signal.consumed()
                    }
                }
                SignalCode.SCRIME_FINISH -> {
                    finish()
                }
                else -> super.whenSignal(signal)
            }
        }
    }
}

class ScrimePaper: Scrime()