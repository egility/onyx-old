/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.granite.fragments

import android.view.View
import android.widget.TextView
import kotlinx.android.synthetic.main.fragment_swap_dog_list.*
import org.egility.library.dbobject.Competition
import org.egility.library.dbobject.Entry
import org.egility.library.dbobject.ScrimeList
import org.egility.library.general.*
import org.egility.android.BaseFragment
import org.egility.android.tools.BLACK
import org.egility.android.tools.GREY
import org.egility.android.tools.Signal
import org.egility.android.tools.SignalCode
import org.egility.android.views.DbCursorListView
import org.egility.granite.R

class SwapDogListFragment : BaseFragment(R.layout.fragment_swap_dog_list), DbCursorListView.Listener {
    private var entry = Entry("idEntry", "idAgilityClass", "idTeam", "teamMember", "jumpHeightCode", "progress",
            "scoreCodes", "time", "noTime", "runningOrder", "subClass")

    override fun whenInitialize() {
        tvPageHeader.text = title
        tvSwapDetails.text = if (ringPartyData.agilityClass.strictRunningOrder) 
            "${ringPartyData.entry.runningOrder}. ${ringPartyData.entry.teamDescription}"
        else
            ringPartyData.entry.teamDescription
        loMiddle.visibility = View.GONE
    }

    override fun whenClick(view: View) {
        when (view) {
            btBack -> sendSignal(SignalCode.BACK)
            btConfirm -> sendSignal(SignalCode.SWAP_DOG_SELECTED, entry.id)
        }
    }

    override fun whenSignal(signal: Signal) {
        when (signal.signalCode) {
            SignalCode.PAGE_DOWN -> {
                lvRunners.pageDown()
                signal.consumed()
            }
            SignalCode.PAGE_UP -> {
                lvRunners.pageUp()
                signal.consumed()
            }
            SignalCode.RESET_FRAGMENT -> {
                doSelect()
                lvRunners.invalidate()
                signal.consumed()
            }
            SignalCode.SWAP_DOG_CONFIRM -> {
                tvSwapOld.text = if (ringPartyData.agilityClass.strictRunningOrder)
                    "${ringPartyData.entry.runningOrder}. ${ringPartyData.entry.teamDescription}"
                else
                    ringPartyData.entry.teamDescription
                tvSwapNew.text = if (ringPartyData.agilityClass.strictRunningOrder)
                    "${entry.runningOrder}. ${entry.teamDescription}"
                else
                    entry.teamDescription
                loTop.visibility = View.GONE
                lvRunners.visibility = View.GONE
                loMiddle.visibility = View.VISIBLE
                signal.consumed()
            }
            else -> {
                doNothing()
            }
        }
    }

    override fun whenItemClick(position: Int) {
        entry.cursor = position
        sendSignal(SignalCode.SWAP_DOG_CONFIRM)
    }

    override fun whenLongClick(position: Int) {
        whenItemClick(position)
    }

    private fun doSelect() {
        entry.selectScrime(ringPartyData.entry.agilityClass, ringPartyData.entry.group, ringPartyData.entry.jumpHeightCode, ringPartyData.agilityClass.template, ringPartyData.entry.id, scrimeList = ScrimeList.ALL, hasBookingIn = Competition.hasBookingIn)
        lvRunners.load(this, entry, R.layout.view_one_item_list)
        lvRunners.requestFocus()
    }

    override fun whenPopulate(view: View, position: Int) {
        entry.cursor = position

        var team = ""
        when (ringPartyData.agilityClass.template) {
            ClassTemplate.SPLIT_PAIRS, ClassTemplate.KC_PAIRS_JUMPING, ClassTemplate.KC_PAIRS_AGILITY -> {
                team = entry.team.description
            }
            ClassTemplate.TEAM -> {
                team = entry.team.teamName
            }
            else -> {
                team = entry.team.getCompetitorDog(1)
            }
        }

        var entryText = if (ringPartyData.agilityClass.strictRunningOrder) "${entry.runningOrder}. $team" else team
        if (entry.progress >= PROGRESS_RUN) {
            entryText += " - ${entry.getStatusText(false)}"
        }

        val listText1 = view.findViewById(R.id.listText1) as TextView
        listText1.text = entryText

        if (Competition.hasBookingIn && intOneOf(entry.progress, PROGRESS_ENTERED)) {
            listText1.setTextColor(GREY)
        } else {
            listText1.setTextColor(BLACK)
        }
    }

}