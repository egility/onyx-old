/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.granite.activities

import android.content.Intent
import android.os.Bundle
import org.egility.granite.fragments.*
import org.egility.library.dbobject.Competition
import org.egility.library.general.*
import org.egility.android.BaseActivity
import org.egility.android.BaseFragment
import org.egility.android.tools.Signal
import org.egility.android.tools.SignalCode
import org.egility.granite.R
import org.egility.library.dbobject.Team

class CheckIn : EntryProcessing()
class BookIn : EntryProcessing()
class SelfService : EntryProcessing()
class Queue : EntryProcessing()

open class EntryProcessing : BaseActivity(R.layout.content_holder) {
    
    private val isQueue = this is Queue
    private val isBookIn = this is BookIn
    private val isSelfService = this is SelfService
    
    private lateinit var entryListFragment: BaseFragment
    
    private lateinit var selectDogByCodeFragment : SelectDogByCodeFragment
    private lateinit var selectDogByNameFragment : SelectDogByNameFragment

    private lateinit var selectEntryByRunningOrderFragment : SelectEntryByRunningOrderFragment
    
    private lateinit var entryStatusFragment : EntryStatusFragment
    private lateinit var jumpQueueListFragment : JumpQueueListFragment
    private lateinit var changeHandlerFragmentKc : ChangeHandlerFragmentKc
    private lateinit var changeHandlerFragmentUka : ChangeHandlerFragmentUka
    private lateinit var waitingForFragment : WaitingForFragment
    private lateinit var callListFragment : CallListFragment
    private lateinit var classMenu : ClassMenu
    private lateinit var radioFragment: RadioFragment

    init {
        if (!dnr) {
            entryListFragment = if (isQueue) QueueFragment() else BookInListFragment()
            selectDogByCodeFragment = SelectDogByCodeFragment()
            selectDogByCodeFragment.isSelfService = isSelfService
            selectDogByNameFragment = SelectDogByNameFragment()
            selectEntryByRunningOrderFragment = SelectEntryByRunningOrderFragment()
            entryStatusFragment = EntryStatusFragment()
            jumpQueueListFragment = JumpQueueListFragment()
            changeHandlerFragmentKc = ChangeHandlerFragmentKc()
            changeHandlerFragmentUka = ChangeHandlerFragmentUka()
            waitingForFragment = WaitingForFragment()
            callListFragment = CallListFragment()
            classMenu = ClassMenu()
            classMenu.isRingParty = true
            classMenu.startTimeOnly = true
            radioFragment = RadioFragment()
            radioFragment.ringPartyMode = true
            radioFragment.exitWhenDone = true
        }
    }

    private var lockFragment: BaseFragment? = null
    var byRunningOrder = false


    override fun whenInitialize() {
        updateTitles()
    }

    private fun updateTitles() {
        var function = if (isBookIn) "Confirm Entries" else if (isQueue) "Manage Queue" else "Self Service"
        var ringText = ringPartyData.agilityClass.classTitle
        var title = ringText + " - " + function
        entryListFragment.title = title
        selectDogByCodeFragment.title = title
        entryStatusFragment.title = title
        selectDogByCodeFragment.className = ringPartyData.agilityClass.name
    }

    override fun whenSignal(signal: Signal) {
        if (signal.signalCode == SignalCode.BACK && isActiveFragment(lockFragment)) {
            lockFragment = null
        }
        with(ringPartyData) {
            when (signal.signalCode) {
                SignalCode.RESET -> {
                    defaultFragmentContainerId = R.id.loContent
                    if (isSelfService || lockFragment == selectDogByCodeFragment) {
                        sendSignal(SignalCode.LOOKUP_TEAM)
                    } else if (lockFragment == waitingForFragment) {
                        sendSignal(SignalCode.LOOKUP_TEAM_LIST, byRunningOrder)
                    } else if (lockFragment == selectEntryByRunningOrderFragment) {
                        sendSignal(SignalCode.LOOKUP_TEAM)
                    } else if (agilityClass.progress == CLASS_WALKING && agilityClass.startTime == nullDate) {
                        loadTopFragment(classMenu)
                    } else if (Competition.isKc) {
                        if (agilityClass.strictRunningOrder || agilityClass.useCallingTo) {
                            if (lockFragment == null) lockFragment = waitingForFragment
                            loadTopFragment( waitingForFragment)
                        } else {
                            if (lockFragment == null) lockFragment = selectEntryByRunningOrderFragment
                            loadTopFragment(selectEntryByRunningOrderFragment)
                        }
                    } else if (agilityClass.strictRunningOrder) {
                        waitingForFragment.byRunningOrder=true
                        loadTopFragment(waitingForFragment)
                    } else {
                        loadTopFragment(waitingForFragment)
                    }
                    signal.consumed()
                }
                SignalCode.LOOKUP_TEAM -> {
                    if (isQueue && !Competition.hasEgilityCodes) {
                        if (lockFragment == null) lockFragment = selectEntryByRunningOrderFragment
                        loadFragment(selectEntryByRunningOrderFragment)
                    } else {
                        if (isQueue) {
                            if (lockFragment == null) lockFragment = selectDogByCodeFragment
                        }
                        selectDogByCodeFragment.clear()
                        loadFragment(selectDogByCodeFragment)
                    }
                    signal.consumed()
                }
                SignalCode.LOOKUP_TEAM_BY_NAME -> {
                    loadFragment(selectDogByNameFragment)
                    signal.consumed()
                }
                SignalCode.VIRTUAL_RADIO -> {
                    loadFragment(radioFragment)
                    signal.consumed()
                }
                SignalCode.LOOKUP_TEAM_LIST -> {
                    byRunningOrder = signal._payload as? Boolean ?: false
                    waitingForFragment.byRunningOrder = byRunningOrder
                    if (isQueue) {
                        if (lockFragment == null) lockFragment = waitingForFragment
                    }
                    loadFragment(waitingForFragment)
                    signal.consumed()
                }
                SignalCode.LIST_BY_NAME -> {
                    loadFragment(callListFragment)
                    signal.consumed()
                }
                SignalCode.RUNNERS_LIST -> {
                    loadFragment(entryListFragment)
                    signal.consumed()
                }
                SignalCode.ENTRY_SELECTED -> {
                    selectClassEntry(agilityClass.id)
                    loadFragment(entryStatusFragment)
                }
                SignalCode.DOG_SELECTED -> {
                    val dogSelection = signal._payload as DogSelection?
                    if (dogSelection != null) {
                        val idTeam= Team.getIndividualId(dogSelection.idCompetitor, dogSelection.idDog)
                        selectTeam(idTeam)
                        sendSignal(SignalCode.ENTRY_SELECTED)
                        signal.consumed()
                    }
                }
                SignalCode.HAVE_ENTRY -> {
                    val idEntry = signal._payload as Int?
                    if (idEntry != null) {
                        entrySelected(idEntry)
                        sendSignal(SignalCode.ENTRY_SELECTED)
                        signal.consumed()
                    }
                }
                SignalCode.LATE_ENTRY -> {
                    if (team.isRedCarded) {
                        Global.services.popUp("Warning", "You may not book-in this dog at this time, Please report to the show secretary.")
                    } else {
                        val bundle = signal._payload as? Bundle
                        if (bundle != null) {
                            val idEntry = addLateEntry(bundle.getString("classHeightCode"), bundle.getBoolean("clearRoundOnly"))
                            if (Competition.enforceMembership && team.hasMembershipIssues) {
                                if (isSelfService) {
                                    Global.services.popUp("Membership Issue", "You need to report to the show secretary.\n\nYour entry has been confirmed, but you may be prevented from competing until you have done so.")
                                } else {
                                    Global.services.popUp("Membership Issue", "This competitor needs to report to the show secretary.\n\nThey have been booked-in, but may be prevented from competing until they have done so.")
                                }
                            }
                            if (isQueue && bundle.getString("classHeightCode") == ringPartyData.jumpHeightCode) {
                                entrySelected(idEntry)
                                sendSignal(SignalCode.RESET_FRAGMENT)
                            } else {
                                sendSignal(SignalCode.RESET)
                            }
                            signal.consumed()
                        }
                    }
                }
                SignalCode.CHANGE_ENTRY_HEIGHT -> {
                    val bundle = signal._payload as? Bundle
                    if (bundle != null) {
                        entry.changeHeight(bundle.getString("classHeightCode"), bundle.getBoolean("clearRoundOnly"))
                        sendSignal(SignalCode.RESET_FRAGMENT)
                        signal.consumed()
                    }

                }
                SignalCode.CONFIRM_ENTRY -> {
                    if (team.isRedCarded) {
                        Global.services.popUp("Warning", "You may not book-in this dog at this time, Please report to the show secretary.")
                    } else {
                        entry.bookIn()
                    }
                    if (Competition.enforceMembership && team.hasMembershipIssues) {
                        if (isSelfService) {
                            Global.services.popUp("Membership Issue", "You need to report to the show secretary.\n\nYour entry has been confirmed, but you may be prevented from competing until you have done so.")
                        } else {
                            Global.services.popUp("Membership Issue", "This competitor needs to report to the show secretary.\n\nThey have been booked-in, but may be prevented from competing until they have done so.")
                        }
                    }
                    sendSignal(SignalCode.RESET)
                    signal.consumed()
                }
                SignalCode.UNCONFIRM_ENTRY -> {
                    entry.bookOut()
                    sendSignal(SignalCode.RESET)
                    signal.consumed()
                }
                SignalCode.REMOVE_ENTRY -> {
                    entry.removeEntry()
                    sendSignal(SignalCode.RESET)
                    signal.consumed()
                }
                SignalCode.JOIN_QUEUE -> {
                    if (team.isRedCarded) {
                        Global.services.popUp("Warning", "This dog may not queue for this class, Please refer the competitor to the show secretary.")
                    } else {
                        entry.checkIn()
                    }
                    if (Competition.enforceMembership && team.hasMembershipIssues) {
                        Global.services.popUp("Membership Issue", "This competitor needs to report to the show secretary.\n\nThey have been checked-in, but may be prevented from competing until they have done so.")
                    }
                    sendSignal(SignalCode.RESET)
                    signal.consumed()
                }
                SignalCode.LEAVE_QUEUE -> {
                    entry.checkOut()
                    sendSignal(SignalCode.RESET)
                    signal.consumed()
                }
                SignalCode.JUMP_QUEUE -> {
                    if (team.isRedCarded) {
                        Global.services.popUp("Warning", "This dog may not queue for this class, Please refer the competitor to the show secretary.")
                    } else {
                        loadFragment(jumpQueueListFragment)
                    }
                    signal.consumed()
                }
                SignalCode.JUMP_QUEUE_SELECTED -> {
                    val queueSequence = signal._payload as? Int
                    if (queueSequence != null) {
                        entry.jumpQueue(queueSequence)
                    }
                    sendSignal(SignalCode.RUNNERS_LIST)
                    signal.consumed()
                }
                SignalCode.WITHDRAWN -> {
                    entry.withdraw()
                    sendSignal(SignalCode.RESET)
                    signal.consumed()
                }
                SignalCode.HEIGHT_SELECTED -> {
                    val newHeightCode = signal._payload as? String
                    if (newHeightCode != null) {
                        jumpHeightCode = newHeightCode
                        updateTitles()
                        sendSignal(SignalCode.REFRESH)
                        signal.consumed()
                    }
                }
                SignalCode.CHECK_PIN -> {
                    val pinType = signal._payload as Int?
                    if (pinType != null) {
                        val intent = Intent(this@EntryProcessing, EnterPin::class.java)
                        intent.putExtra("pinType", pinType)
                        startActivityForResult(intent, pinType)
                    }
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
                    if (Competition.enforceMembership && team.hasMembershipIssues) {
                        if (isSelfService) {
                            Global.services.popUp("Membership Issue", "You need to report to the show secretary.\n\nYou may be prevented from competing until you have done so.")
                        } else {
                            Global.services.popUp("Membership Issue", "This competitor needs to report to the show secretary.\n\nThey may be prevented from competing until they have done so.")
                        }
                    }
                    sendSignal(SignalCode.BACK)
                }
                SignalCode.CALL_TO -> {
                    val amount = signal._payload as? Int ?: 0
                    agilityClass.incrementHeightCallingTo(ringPartyData.jumpHeightCode, amount)
                    agilityClass.post()
                    sendSignal(SignalCode.REFRESH, queued = false)
                }
                else -> super.whenSignal(signal)
            }
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, ringPartyData: Intent?) {
        if (resultCode == RESULT_OK) {
            when (requestCode) {
                PIN_GENERAL -> finish()
            }
        } else {
            sendSignal(SignalCode.RESET)
        }
    }

}
