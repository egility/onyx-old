/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.granite.fragments

import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import kotlinx.android.synthetic.main.fragment_edit_running_order.*
import org.egility.android.BaseFragment
import org.egility.android.NavigationGroup
import org.egility.android.tools.AndroidUtils
import org.egility.android.tools.Signal
import org.egility.android.tools.SignalCode
import org.egility.android.views.DbCursorListView
import org.egility.android.views.QuickButton
import org.egility.granite.R
import org.egility.library.dbobject.Competition
import org.egility.library.dbobject.Entry
import org.egility.library.general.*
import java.util.*


class EditRunningOrderFragment : BaseFragment(R.layout.fragment_edit_running_order), DbCursorListView.Listener {

    val data = ClassData
    var agilityClass = data.activeChildClass
    val targetEntry = data.targetEntry

    data class Row(val heading: String = "", val cursor: Int = -1)

    private var entry =
        Entry("idEntry", "idAgilityClass", "idTeam", "teamMember", "group", "jumpHeightCode", "progress", "scoreCodes", "time", "noTime", "runningOrder")

    private var rows = ArrayList<Row>()

    var byRunningOrder = false

    override fun whenInitialize() {
    }

    override fun whenClick(view: View) {
        if (view.tag is Signal) {
            sendSignal(view.tag as Signal)
        }
    }

    override fun whenSignal(signal: Signal) {
        when (signal.signalCode) {
            SignalCode.RESET_FRAGMENT -> {
                setUpNavigation()
                sendSignal(SignalCode.REFRESH, queued = false)
                signal.consumed()
            }
            SignalCode.REFRESH -> {
                doSelect()
                lvRunners.invalidate()
                signal.consumed()
            }
            SignalCode.PAGE_DOWN -> {
                lvRunners.pageDown()
                signal.consumed()
            }
            SignalCode.PAGE_UP -> {
                lvRunners.pageUp()
                signal.consumed()
            }
            SignalCode.HEIGHT_SELECTED -> {
                val jumpHeightCode = signal._payload as? String
                if (jumpHeightCode != null) {
                    ringPartyData.jumpHeightCode = jumpHeightCode
                    sendSignal(SignalCode.REFRESH)
                    signal.consumed()
                }
            }
            else -> {
                doNothing()
            }

        }
    }

    private fun doSelect() {
        if (agilityClass.jumpHeightArray.size > 1) {
            tvPageHeader.text =
                "Edit Running Order: ${agilityClass.name} (${agilityClass.getHeightCaption(ringPartyData.jumpHeightCode)})"
        } else {
            tvPageHeader.text = "Edit Running Order: ${agilityClass.name}"
        }
        entry.selectRunningOrder(agilityClass, "", ringPartyData.jumpHeightCode)

        rows.clear()
        val groupMonitor = ChangeMonitor("")
        while (entry.next()) {
            if (groupMonitor.hasChanged(entry.group)) {
                rows.add(Row(heading = "Group ${entry.group}"))
            }
            rows.add(Row(cursor = entry.cursor))
        }

        lvRunners.load(this, rows, R.layout.running_order_list)
        lvRunners.requestFocus()

        val heightCaption = agilityClass.getHeightCaption(ringPartyData.jumpHeightCode)
        for (index in 1..loNavigation.childCount) {
            val button = loNavigation.getChildAt(index) as? QuickButton
            if (button != null) {
                AndroidUtils.disableIf(button.text == heightCaption, button)
            }
        }
    }

    override fun whenItemClick(position: Int) {
        val row = rows[position]

        if (row.heading.isEmpty()) {
            entry.cursor = row.cursor
            targetEntry.find(entry.id)
            sendSignal(SignalCode.ENTRY_SELECTED)
        }
    }

    override fun whenLongClick(position: Int) {
        whenItemClick(position)
    }

    override fun whenPopulate(view: View, position: Int) {
        val row = rows[position]
        val tvHeading = view.findViewById(R.id.tvHeading) as TextView
        val loRunner = view.findViewById(R.id.loRunner) as LinearLayout
        val tvRunner = view.findViewById(R.id.tvRunner) as TextView
        val tvRunningOrder = view.findViewById(R.id.tvRunningOrder) as TextView

        AndroidUtils.goneIf(row.heading.isEmpty(), tvHeading)
        AndroidUtils.goneIf(row.heading.isNotEmpty(), loRunner)

        if (!row.heading.isEmpty()) {
            tvHeading.text = row.heading
        } else {
            entry.cursor = row.cursor
            var entryText =
                if (agilityClass.template == ClassTemplate.TEAM_INDIVIDUAL) entry.team.getCompetitorDog(entry.teamMember) else entry.team.description
            tvRunningOrder.text = "${entry.runningOrder}."
            AndroidUtils.goneIf(!(Competition.isKc || agilityClass.strictRunningOrder), tvRunningOrder)
            if (Competition.enforceMembership && entry.team.hasMembershipIssues) {
                entryText += " (*)"
            }
            tvRunner.text = entryText
        }
    }


    fun setUpNavigation() {
        var selectedHeight = ringPartyData.jumpHeightCode
        var selected = 0

        loNavigation.removeAllViews()
        addNavigationButton(loNavigation, "Back", SignalCode.BACK)

        if (agilityClass.jumpHeightArray.size > 1) {
            val navigationGroup = NavigationGroup()
            for (height in agilityClass.jumpHeightArray) {
                if (selectedHeight == "") selectedHeight = height.code
                if (height.code == selectedHeight) selected = navigationGroup.size
                navigationGroup.add(height.heightCaptionShort, SignalCode.HEIGHT_SELECTED, height.code)
            }
            addNavigationGroup(loNavigation, navigationGroup, selected, if (Competition.isKc) 2 else 0)
        }

        if (ringPartyData.jumpHeightCode.isEmpty()) {
            ringPartyData.jumpHeightCode = selectedHeight
        }
    }


}