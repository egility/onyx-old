/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.granite.fragments

import android.view.View
import kotlinx.android.synthetic.main.fragment_scrime_competitor.*
import org.egility.library.dbobject.Competition
import org.egility.library.dbobject.Height
import org.egility.library.general.doNothing
import org.egility.library.general.ringPartyData
import org.egility.android.BaseFragment
import org.egility.android.tools.AndroidUtils.goneIf
import org.egility.android.tools.Signal
import org.egility.android.tools.SignalCode
import org.egility.granite.R


class ScrimeCompetitorFragment : BaseFragment(R.layout.fragment_scrime_competitor) {

    var isBlocked = false

    override fun whenInitialize() {
        if (!Competition.isUka) {
            btNameConfirmed.text="Ready to Run"
        } else {
            spNotReady.visibility=View.GONE
            btNotReady.visibility=View.GONE
        }
    }

    override fun whenClick(view: View) {
        if (view.tag is Signal) {
            sendSignal(view.tag as Signal)
        } else {
            when (view) {
                btBack -> sendSignal(SignalCode.BACK)
                btRun -> sendSignal(SignalCode.SCRIME_RUN, true)
                btNotRun -> sendSignal(SignalCode.SCRIME_NOT_RUN, true)
                btNameConfirmed -> sendSignal(SignalCode.ENTER_SCORE, true)
                btNotReady -> sendSignal(SignalCode.LEAVE_QUEUE_QUESTION, true)
            }
        }
    }

    override fun whenSignal(signal: Signal) {
        when (signal.signalCode) {
            SignalCode.RESET_FRAGMENT -> {
                isBlocked = ringPartyData.isEntrySelected && Competition.enforceMembership && ringPartyData.team.hasMembershipIssues
                val groupText = if (ringPartyData.group.isNotEmpty()) " group ${ringPartyData.group}" else ""
                tvNote.text =
                        if (isBlocked)
                            "This competitor is blocked from competing at the moment. Please remove from the queue and contact the show secretary to resolve."
                        else if (!ringPartyData.isEntrySelected)
                            if (!Competition.isUka) {
                                val height = if (ringPartyData.agilityClass.lhoClass) Height.getHeightJumpName(ringPartyData.jumpHeightCode) else
                                    Height.getHeightName(ringPartyData.jumpHeightCode)
                                "There are no$groupText $height dogs ready to run. Please select one of the following options:"
                            } else {
                                "There are no ${Height.getHeightJumpName(ringPartyData.jumpHeightCode)} dogs queueing. Please select one of the following options:"
                            }
                        else
                            ""
                goneIf(tvNote.text.isEmpty(), tvNote)
                setUpMenu()
            }
            else -> {
                doNothing()
            }
        }
    }

    fun setUpMenu() {
        loNormal.visibility=View.GONE
        loCentre.visibility=View.GONE
        if (!isBlocked && ringPartyData.isEntrySelected) {
            loNormal.visibility=View.VISIBLE
        } else if (!ringPartyData.isEntrySelected) {
            loCentre.visibility=View.VISIBLE
            loMenu.removeAllViews()

            var selectedHeight = ringPartyData.jumpHeightCode
            var selected = 0

            addMenuButton(loMenu, "Check Again", SignalCode.RESET, buttonWidth = 300)
            addMenuButton(loMenu, "Choose Dog from List", SignalCode.SCRIME_OTHER, buttonWidth = 300)

            if (ringPartyData.agilityClass.jumpHeightArray.size > 1) {
                for (height in ringPartyData.agilityClass.jumpHeightArray) {
                    if (selectedHeight == "") selectedHeight = height.code
                    val groupText = if (ringPartyData.group.isNotEmpty()) "${ringPartyData.group}/" else ""
                    val button = addMenuButton(loMenu, "Change to $groupText${height.heightCaption}", SignalCode.HEIGHT_SELECTED, height.code, buttonWidth = 300)
                    button.isEnabled = (height.code != selectedHeight)
                }
            }
            ringPartyData.jumpHeightCode = selectedHeight

            if (ringPartyData.agilityClass.groupRunningOrder.isNotEmpty()) {
                addMenuButton(loMenu, "Group/Class Finished", SignalCode.SCRIME_FINISH, buttonWidth = 300)
            } else {
                addMenuButton(loMenu, "Class Finished", SignalCode.SCRIME_FINISH, buttonWidth = 300)
            }
        }
    }

}