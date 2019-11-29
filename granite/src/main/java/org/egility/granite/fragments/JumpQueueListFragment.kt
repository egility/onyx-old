/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.granite.fragments

import android.view.View
import android.widget.TextView
import kotlinx.android.synthetic.main.fragment_jump_queue_list.*
import org.egility.library.dbobject.Entry
import org.egility.library.general.*
import org.egility.android.BaseFragment
import org.egility.android.tools.Signal
import org.egility.android.tools.SignalCode
import org.egility.android.views.DbCursorListView
import org.egility.granite.R

class JumpQueueListFragment : BaseFragment(R.layout.fragment_jump_queue_list), DbCursorListView.Listener {
    private var entry = Entry("idEntry", "idAgilityClass", "idTeam", "teamMember", "jumpHeightCode", "queueSequence")

    override fun whenInitialize() {
        tvPageHeader.text = title
        tvSwapDetails.text = ringPartyData.entry.teamDescription
    }

    override fun whenClick(view: View) {
        when (view) {
            btBack -> sendSignal(SignalCode.BACK)
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
            else -> {
                doNothing()
            }
        }
    }

    override fun whenItemClick(position: Int) {
        entry.cursor = position
        sendSignal(SignalCode.JUMP_QUEUE_SELECTED, entry.queueSequence)
    }

    override fun whenLongClick(position: Int) {
        whenItemClick(position)
    }

    private fun doSelect() {
        entry.selectQueuing(ringPartyData.agilityClass.id, ringPartyData.entry.jumpHeightCode, ringPartyData.agilityClass.template, ringPartyData.entry.id)
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

        val listText1 = view.findViewById(R.id.listText1) as TextView
        listText1.text = team
    }

}