/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.granite.activities

import android.content.Intent
import android.view.View
import org.egility.granite.fragments.*
import org.egility.library.dbobject.RingSelector
import org.egility.android.BaseActivity
import org.egility.android.BaseFragment
import org.egility.android.tools.Signal
import org.egility.android.tools.SignalCode
import org.egility.granite.R
import org.egility.library.dbobject.ScrimeList
import org.egility.library.general.*

open class RingStatus(val isRingParty: Boolean = false) : BaseActivity(R.layout.content_holder) {

    private lateinit var ringListFragment: RingListFragment
    private lateinit var ringDetailsFragment: RingDetailsFragment
    private lateinit var classMenu: ClassMenu
    private lateinit var resultsFragment: ResultsFragment
    private lateinit var closeClassListFragment: CloseClassFragment
    private lateinit var queueFragment: BaseFragment
    private lateinit var changeClassOrderFragment: ChangeClassOrderFragment
    private lateinit var waitingForFragment: WaitingForListFragment
    private lateinit var classListFragment: ClassListFragment
    private lateinit var editRunningOrderFragment: EditRunningOrderFragment
    private lateinit var moveInFrontOfFragment: MoveInFrontOfFragment
    private lateinit var paperScimeSelectFragment : PaperScimeSelectFragment
    private lateinit var paperScrimeListFragment : PaperScrimeListFragment


    init {
        if (!dnr) {
            ringListFragment = RingListFragment()
            ringDetailsFragment = RingDetailsFragment()
            ringDetailsFragment.isRingParty = isRingParty
            classMenu = ClassMenu()
            classMenu.isRingParty = isRingParty
            classMenu.canEditRunningOrders = true
            resultsFragment = ResultsFragment()
            closeClassListFragment = CloseClassFragment()
            queueFragment = QueueFragment()
            changeClassOrderFragment = ChangeClassOrderFragment()
            waitingForFragment = WaitingForListFragment()
            classListFragment = ClassListFragment()
            editRunningOrderFragment = EditRunningOrderFragment()
            moveInFrontOfFragment = MoveInFrontOfFragment()
            paperScimeSelectFragment = PaperScimeSelectFragment()
            paperScrimeListFragment = PaperScrimeListFragment()
        }
    }

    val data = ClassData
    val agilityClass = ringPartyData.agilityClass
    val targetEntry = data.targetEntry


    override fun whenClick(view: View) {
        super.whenClick(view)
    }

    override fun whenSignal(signal: Signal) {
        when (signal.signalCode) {
            SignalCode.RESET -> {
                defaultFragmentContainerId = R.id.loContent
                if (isRingParty) {
                    sendSignal(SignalCode.RING_DETAILS, ringPartyData.ring.selector)
                } else {
                    sendSignal(SignalCode.SELECT_LIST)
                    pulseRate = 30
                }
            }
            SignalCode.SELECT_LIST -> {
                if (this is ClassList) {
                    loadTopFragment(classListFragment)
                } else {
                    loadTopFragment(ringListFragment)
                }
                signal.consumed()
            }
            SignalCode.RING_DETAILS -> {
                val selector = signal._payload as? RingSelector
                if (selector != null) {
                    ringDetailsFragment.selectRing(selector)
                    loadFragment(ringDetailsFragment)
                    signal.consumed()
                }
            }
            SignalCode.CLASS_SELECTED -> {
                val idAgilityClass = signal._payload as? Int
                if (idAgilityClass != null) {
                    ringPartyData.setAgilityClass(idAgilityClass)
                    loadFragment(classMenu)
                    signal.consumed()
                }
            }
            SignalCode.VIEW_RESULTS -> {
                loadFragment(resultsFragment)
                signal.consumed()
            }
            SignalCode.VIEW_MISSING -> {
                loadFragment(waitingForFragment)
                signal.consumed()
            }
            SignalCode.VIEW_CHECK_IN -> {
                val idAgilityClass = signal._payload as Int?
                if (idAgilityClass != null) {
                    ringPartyData.setAgilityClass(idAgilityClass)
                    loadFragment(queueFragment)
                    signal.consumed()
                }
            }
            SignalCode.PAPER_SCRIME -> {
                agilityClass.refresh()
                if (!agilityClass.hasCourseTime()) {
                    Global.services.msgYesNo(
                        "Warning",
                        "You have not entered course times for this class. Would you like to do it now?"
                    ) { isYes ->
                        if (isYes) {
                            doActivity(EnterCourseTimes::class.java)
                        } else {
                            Global.services.popUp(
                                "Information",
                                "Try later when the course times have been entered"
                            )
                        }
                    }
                } else {
                    loadFragment(paperScimeSelectFragment)
                }
            }
            SignalCode.PAPER_SELECTED -> {
                val idEntry=signal._payload as Int?
                val hasRun=signal._payload2 as Boolean?
                if (idEntry!=null && hasRun!=null) {
                    val scrimePaper = Intent(this, ScrimePaper::class.java)
                    scrimePaper.putExtra("hasRun", hasRun)
                    scrimePaper.putExtra("idEntry", idEntry)
                    if (isActiveFragment(paperScrimeListFragment)) {
                        back()
                    }
                    startActivity(scrimePaper)
                }
            }
            SignalCode.SCRIME_NOT_RUN -> {
                paperScrimeListFragment.selector = ScrimeList.NOT_RUN
                loadFragment(paperScrimeListFragment)
            }
            SignalCode.SCRIME_RUN -> {
                paperScrimeListFragment.selector = ScrimeList.RUN
                loadFragment(paperScrimeListFragment)
            }

            SignalCode.ENTRY_SELECTED -> {
                if (isActiveFragment(editRunningOrderFragment)) {
                    loadFragment(moveInFrontOfFragment)
                } else {
                    val scrime = Intent(this, Scrime::class.java)
                    scrime.putExtra("idEntry", ringPartyData.entry.id)
                    startActivity(scrime)
                }
            }
            SignalCode.CLOSE_CLASS -> {
                loadFragment(closeClassListFragment)
                signal.consumed()
            }
            SignalCode.HEIGHT_SELECTED -> {
                val newHeightCode = signal._payload as? String
                if (newHeightCode != null) {
                    ringPartyData.jumpHeightCode = newHeightCode
                    sendSignal(SignalCode.REFRESH)
                    signal.consumed()
                }
            }
            SignalCode.CHANGE_CLASS_ORDER -> {
                loadFragment(changeClassOrderFragment)
            }
            SignalCode.EDIT_RUNNING_ORDERS -> {
                editRunningOrderFragment.agilityClass = agilityClass
                moveInFrontOfFragment.agilityClass = agilityClass
                loadFragment(editRunningOrderFragment)
                signal.consumed()
            }
            SignalCode.MOVE_IN_FRONT_OF -> {
                val newRunningOrder = signal._payload as? Int
                val team = if (targetEntry.agilityClass.template == ClassTemplate.TEAM_INDIVIDUAL) targetEntry.team.getCompetitorDog(targetEntry.teamMember) else targetEntry.team.description

                if (newRunningOrder != null) {
                    whenYes(
                        "Question",
                        "Are you sure you want to move $team to r/o $newRunningOrder"
                    ) {
                        targetEntry.moveToRunningOrder(newRunningOrder)
                        sendSignal(SignalCode.EDIT_RUNNING_ORDERS)
                    }
                }
            }

            else -> {
                super.whenSignal(signal)
            }
        }
    }
}

class RingStatusRingParty() : RingStatus(isRingParty = true)
class ClassList() : RingStatus()