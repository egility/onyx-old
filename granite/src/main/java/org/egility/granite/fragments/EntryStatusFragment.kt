/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.granite.fragments

import android.os.Bundle
import android.view.View
import android.widget.GridLayout
import android.widget.TextView
import kotlinx.android.synthetic.main.fragment_runner_status.*
import org.egility.android.BaseFragment
import org.egility.android.tools.AndroidUtils.dpToPx
import org.egility.android.tools.AndroidUtils.goneIf
import org.egility.android.tools.Signal
import org.egility.android.tools.SignalCode
import org.egility.android.views.QuickButton
import org.egility.granite.R
import org.egility.granite.activities.*
import org.egility.library.dbobject.Competition
import org.egility.library.dbobject.CompetitionLedger
import org.egility.library.dbobject.Grade
import org.egility.library.dbobject.Height
import org.egility.library.general.*


/**
 * Created by mbrickman on 10/06/15.
 */
class EntryStatusFragment : BaseFragment(R.layout.fragment_runner_status) {

    private var infoColumns = 0
    private var rowCount = 0
    var creditsAvailable = 0

    private val isBookingIn: Boolean
        get() = (activity is BookIn)

    private val isCheckingIn: Boolean
        get() = (activity is CheckIn)

    private val isQueue: Boolean
        get() = (activity is Queue)

    private val isScrime: Boolean
        get() = (activity is Scrime) || isScrimePaper

    private val isScrimePaper: Boolean
        get() = (activity is ScrimePaper)

    private val isSelfService: Boolean
        get() = (activity is SelfService)


    override fun whenInitialize() {
        if (Competition.isUka) {
            creditsAvailable =
                CompetitionLedger.creditsAvailable(Competition.current.id, ringPartyData.team.dog.idAccount)
        }
        tvPageHeader.text =
            "Runner Options (Ring ${ringPartyData.ring.number}): ${ringPartyData.entry.agilityClass.name}"

        if (ringPartyData.entry.isOnRow) {
            if (ringPartyData.entry.team.memberCount > 1) {
                tvCompetitor.text = ringPartyData.entry.team.getMajorName(0)
                tvDog.text = ringPartyData.entry.team.getMinorName(0)
            } else {
                tvCompetitor.text = ringPartyData.entry.majorName
                tvDog.text = ringPartyData.entry.minorName
            }
        } else {
            if (ringPartyData.entry.team.memberCount > 1) {
                tvCompetitor.text = ringPartyData.team.getMajorName(0)
                tvDog.text = ringPartyData.entry.team.getMinorName(0)
            } else {
                tvCompetitor.text = ringPartyData.team.getCompetitorName(1)
                if (Competition.hasEgilityCodes) {
                    tvDog.text = ringPartyData.team.getDogName(1) + " (${ringPartyData.team.getDogCode(1)})"
                } else {
                    tvDog.text = ringPartyData.team.getDogName(1)
                }
            }
        }
        goneIf(isScrime && !isScrimePaper || isSelfService, btBack)
        goneIf(!(Competition.enforceMembership && ringPartyData.team.hasMembershipIssues), tvNeeded)
    }

    fun reset(changeHeight: Boolean) {
        loMenu.removeAllViews()
        rowCount = 0
        showStatus()

        tvMessage.text = ""
        val preEntered = ringPartyData.entry.found() && ringPartyData.entry.progress != PROGRESS_REMOVED
        val preEntryConvertedToCredit = ringPartyData.entry.progress == PROGRESS_CONVERTED_TO_CREDIT
        val needHeight = changeHeight || ringPartyData.entry.jumpHeightCode.isEmpty()
        val hasCredit = creditsAvailable > 0

        when {
            isScrime && needHeight -> {
                showHeightOptions(SignalCode.CHANGE_ENTRY_HEIGHT, false)
            }
            isScrime && !needHeight -> {
                if (ringPartyData.entry.hasRun) {
                    showScrimeOptions()
                }
            }
            preEntered && needHeight -> {
                showHeightOptions(SignalCode.CHANGE_ENTRY_HEIGHT, false)
            }
            preEntered && !needHeight -> {
                showGeneralOptions()
            }
            !preEntered && hasCredit && ringPartyData.agilityClass.isLateEntriesAllowed -> {
                showHeightOptions(SignalCode.LATE_ENTRY, true)
            }
            (!preEntered || preEntryConvertedToCredit) && !hasCredit -> {
                tvMessage.text = "No available pay-on-the-day credits"
            }
        }

        goneIf(infoColumns == 0, loInfoGrid)
        goneIf(rowCount == 0, loOptions)
        goneIf(tvMessage.text.isEmpty(), tvMessage)
    }

    fun showGeneralOptions() {
        tvSubTitle.text = "Choose Option"

        with(ringPartyData.entry) {
            val canRemove = canRemoveEntry && (isBookingIn || agilityClass.progress <= CLASS_WALKING)
            val activeHeight =
                isQueue || ((isCheckingIn) && jumpHeightCode.equals(ringPartyData.jumpHeightCode, ignoreCase = true))

            addOption(if (Competition.isUkaStyle) "Queuing" else "Ready to Run", canCheckIn && activeHeight, SignalCode.JOIN_QUEUE)
            if (Competition.hasBookingIn) {
                addOption("Confirm Entry", canBookIn, SignalCode.CONFIRM_ENTRY)
            } else {
                addOption("Mark Late", canBookIn, SignalCode.CONFIRM_ENTRY)
            }

            if (Competition.isUka) {
                addOption("Jump Queue", canJumpQueue && activeHeight, SignalCode.JUMP_QUEUE)
            }
            if (Competition.hasBookingIn) {
                addOption("Un-Confirm", canBookOut, SignalCode.UNCONFIRM_ENTRY)
            } else {
                addOption("Un-Mark Late", canBookOut, SignalCode.UNCONFIRM_ENTRY)
            }
            addOption(
                if (Competition.isUka) "Leave Queue" else "Not Ready",
                canCheckOut && activeHeight,
                SignalCode.LEAVE_QUEUE
            )
/*
            addOption("Check In", canCheckIn && activeHeight, SignalCode.JOIN_QUEUE)
            addOption("Book In", canBookIn, SignalCode.CONFIRM_ENTRY)
            addOption("Jump Queue", canStandBy && activeHeight, SignalCode.READY_TO_RUN)
            addOption("Book Out", canBookOut, SignalCode.UNCONFIRM_ENTRY)
            addOption("Check Out", canCheckOut && activeHeight, SignalCode.LEAVE_QUEUE)
*/
            addOption("Swap Dogs", canSwapDogs, SignalCode.SWAP_PAIR)

            if (Competition.isUka) {
                addOption("Change Height", canChangeHeight, SignalCode.REQUEST_CHANGE_ENTRY_HEIGHT)
                addOption("Remove Entry", canRemove, SignalCode.REMOVE_ENTRY)
            }
            addOption("Change Handler", canChangeHandler, SignalCode.CHANGE_HANDLER)
            addOption("Withdraw", canWithdraw && !canRemove, SignalCode.WITHDRAWN)
        }
    }

    fun showScrimeOptions() {
        tvSubTitle.text = "Choose Option"
        if (!ringPartyData.agilityClass.template.nfcOnly) {
            addOption("Edit Result", signalCode = SignalCode.EDIT_SCORE)
        }
        addOption("Wrong Dog", signalCode = SignalCode.SWAP_DOG)
        if (ringPartyData.entry.noTime) {
            addOption("Re-Run (Time)", ringPartyData.scrimeMode, signalCode = SignalCode.RE_RUN_TIME_QUESTION)
            addOption("Re-Run (Scratch)", ringPartyData.scrimeMode, signalCode = SignalCode.RE_RUN_SCRATCH_QUESTION)
        }
        addOption("Erase Run", signalCode = SignalCode.WIPE_SCORE_QUESTION)
        addOption("Change Handler", signalCode = SignalCode.CHANGE_HANDLER)

        //        addOption("Disqualified", signalCode = SignalCode.DO_NOTHING)
    }


    fun showHeightOptions(signalCode: SignalCode, lateEntry: Boolean) {
        tvSubTitle.text = if (lateEntry) "Select Class" else "Select Height"

        with(ringPartyData.agilityClass) {
            val casual = isCasual
            val prefix = if (lateEntry) "Enter " else ""
            val height = Height()
            val where = "FIND_IN_SET(heightCode, ${heightRunningOrder.quoted}) > 0"

            height.select(where, "heightCode DESC")
            while (height.next()) {
                val bundle = Bundle()
                bundle.putString("classHeightCode", height.code)
                bundle.putBoolean("clearRoundOnly", false)
                addOption(prefix + height.name, true, signalCode, bundle, 0, casual)

                if (casual && height.clearRoundOnlyOption) {
                    val bundleCro = Bundle()
                    bundleCro.putString("classHeightCode", height.code)
                    bundleCro.putBoolean("clearRoundOnly", true)
                    addOption(prefix + height.name + " CRO", true, signalCode, bundleCro, 1, casual)
                }
            }
        }
    }


    private fun addOption(
        caption: String,
        condition: Boolean = true,
        signalCode: SignalCode,
        _payload: Any? = null,
        column: Int = 0,
        narrowButtons: Boolean = false
    ) {
        val fragmentActivity = activity
        if (condition && fragmentActivity != null) {
            val buttonWidth = dpToPx(fragmentActivity, if (narrowButtons) 200 else 240)
            val buttonMargin = dpToPx(fragmentActivity, 12)
            val signal = prepareSignal(signalCode, _payload)
            val button = fragmentActivity.layoutInflater.inflate(R.layout.template_option_button, null) as QuickButton
            button.text = caption
            button.id = View.generateViewId()
            button.tag = signal

            if (column == 0) {
                rowCount++
            }

            val params = GridLayout.LayoutParams(
                GridLayout.spec(rowCount - 1, GridLayout.CENTER),
                GridLayout.spec(column, GridLayout.CENTER)
            )
            params.width = buttonWidth
            params.setMargins(buttonMargin, buttonMargin, buttonMargin, buttonMargin)

            loMenu.addView(button, params)

        }
    }

    fun showStatus() {

        fun add(caption: String, info: String, condition: Boolean) {
            val fragmentActivity = activity
            if (condition && fragmentActivity != null) {
                val view = fragmentActivity.layoutInflater.inflate(R.layout.template_header, null)
                val tvCaption = view.findViewById(R.id.tvCaption) as TextView
                val tvInfo = view.findViewById(R.id.tvInfo) as TextView
                val vwLeft = view.findViewById(R.id.vwLeft) as View

                tvCaption.text = caption
                tvInfo.text = info
                goneIf(infoColumns == 0, vwLeft)
                loInfoGrid.addView(view)
                infoColumns++
            }
        }

        loInfoGrid.removeAllViews()
        infoColumns = 0
        if (ringPartyData.entry.isOnRow) {
            with(ringPartyData.entry) {
                if (Competition.isKc || ringPartyData.agilityClass.strictRunningOrder) {
                    add("R/O:", runningOrder.toString(), true)
                }
                if (Competition.isUka) {
                    add("Entry:", "Add-On", isLateEntry)
                    add("Entry:", "On-Line", !isLateEntry)
                    add("Height:", jumpHeightText, true)
                }
                if (Competition.isKc) {
                    add("Height:", combinedHeightText, true)
                    add("Grade:", "G" + Grade.getGradeShort(gradeCode), true)
                }
                add("Status:", progressText, !hasRun)
                add("Result:", getStatusText(false), hasRun)
            }
        } else {
            if (Competition.isUka) {
                add("Entry:", "n/a", true)
            }
            if (Competition.isKc) {
                add("Grade:", "n/a", true)
            }
            add("Height:", "n/a", true)
            add("Status:", "Not Entered", true)
        }
        if (Competition.isUka) {
            add("Credits:", creditsAvailable.toString(), true)
        }
    }

    override fun whenClick(view: View) {
        if (view.tag is Signal) {
            sendSignal(view.tag as Signal)
        } else {
            when (view) {
                btBack -> sendSignal(SignalCode.BACK)
                btFinished -> sendSignal(SignalCode.RESET)
            }
        }
    }

    override fun whenSignal(signal: Signal) {
        when (signal.signalCode) {
            SignalCode.RESET_FRAGMENT -> {
                reset(false)
                signal.consumed()
            }
            SignalCode.REQUEST_CHANGE_ENTRY_HEIGHT -> {
                reset(true)
                signal.consumed()
            }
            SignalCode.SWAP_PAIR -> {
                ringPartyData.entry.team.swapMembers(1, 2)
                ringPartyData.entry.heightCode = ringPartyData.entry.team.relayHeightCode
                ringPartyData.entry.jumpHeightCode = ringPartyData.entry.team.relayHeightCode
                ringPartyData.entry.post()
                sendSignal(SignalCode.RESET_FRAGMENT)
                signal.consumed()
            }
            SignalCode.BACK -> {
                if (isScrime || isSelfService) {
                    sendSignal(SignalCode.RESET)
                    signal.consumed()
                }
            }
            else -> {
                doNothing()
            }
        }
    }

}
