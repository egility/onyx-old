/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.granite.fragments

import android.view.View
import android.widget.TextView
import kotlinx.android.synthetic.main.fragment_move_in_front_of.*
import org.egility.android.BaseFragment
import org.egility.android.tools.Signal
import org.egility.android.tools.SignalCode
import org.egility.android.views.DbCursorListView
import org.egility.granite.R
import org.egility.library.dbobject.Entry
import org.egility.library.general.ClassData
import org.egility.library.general.ClassTemplate
import org.egility.library.general.doNothing
import org.egility.library.general.ringPartyData

class MoveInFrontOfFragment : BaseFragment(R.layout.fragment_move_in_front_of), DbCursorListView.Listener {

    val data = ClassData
    var agilityClass = data.activeChildClass
    val targetEntry = data.targetEntry

    private var entry =
        Entry("idEntry", "idAgilityClass", "idTeam", "teamMember", "jumpHeightCode", "queueSequence", "runningOrder")

    override fun whenInitialize() {
        tvPageHeader.text = title
        val team = if (targetEntry.agilityClass.template == ClassTemplate.TEAM_INDIVIDUAL) targetEntry.team.getCompetitorDog(targetEntry.teamMember) else targetEntry.team.description
        tvSwapDetails.text = "${targetEntry.runningOrder} ${team}"
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
        sendSignal(SignalCode.MOVE_IN_FRONT_OF, if (entry.runningOrder <= targetEntry.runningOrder) entry.runningOrder else entry.runningOrder - 1)
    }

    override fun whenLongClick(position: Int) {
        whenItemClick(position)
    }

    private fun doSelect() {
        entry.selectRunningOrder(agilityClass, targetEntry.group, targetEntry.jumpHeightCode, targetEntry.id)
        lvRunners.load(this, entry, R.layout.view_one_item_list)
        lvRunners.requestFocus()
    }

    override fun whenPopulate(view: View, position: Int) {
        entry.cursor = position

        var team = ""
        when (entry.agilityClass.template) {
            ClassTemplate.SPLIT_PAIRS, ClassTemplate.KC_PAIRS_JUMPING, ClassTemplate.KC_PAIRS_AGILITY -> {
                team = entry.team.description
            }
            ClassTemplate.TEAM_INDIVIDUAL -> {
                team = entry.team.getCompetitorDog(entry.teamMember)
            }
            ClassTemplate.TEAM, ClassTemplate.TEAM_RELAY -> {
                team = entry.team.teamName
            }
            else -> {
                team = entry.team.getCompetitorDog(1)
            }
        }

        val listText1 = view.findViewById(R.id.listText1) as TextView
        listText1.text = "${entry.runningOrder} $team"
    }

}