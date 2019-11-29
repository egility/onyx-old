/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.granite.fragments

import android.view.View
import android.widget.TextView
import kotlinx.android.synthetic.main.fragment_bookin_list.*
import org.egility.library.dbobject.Competition
import org.egility.library.dbobject.Entry
import org.egility.library.general.*
import org.egility.android.BaseFragment
import org.egility.android.tools.*
import org.egility.android.views.DbCursorListView
import org.egility.granite.R
import java.util.*


class BookInListFragment : BaseFragment(R.layout.fragment_bookin_list), DbCursorListView.Listener {
    private var entry = Entry("idEntry", "idTeam", "teamMember", "jumpHeightCode", "clearRoundOnly", "progress")

    data class Row(val heading: String = "", val cursor: Int = -1)

    private var rows = ArrayList<Row>()

    override fun whenInitialize() {
        tvPageHeader.text = title
    }

    override fun whenClick(view: View) {
        when (view) {
            btBack -> sendSignal(SignalCode.BACK)
            btAddOn -> sendSignal(SignalCode.LOOKUP_TEAM)
            btRefresh -> sendSignal(SignalCode.RESET_FRAGMENT)
        }
    }

    override fun whenSignal(signal: Signal) {
        when (signal.signalCode) {
            SignalCode.RESET_FRAGMENT -> {
                doSelect()
                lvRunners.invalidate()
                val groupText = if (ringPartyData.group.isNotEmpty()) " (${ringPartyData.group})" else ""
                tvPageFooter.text = "Still to run$groupText - " + ringPartyData.agilityClass.stillToRun(ringPartyData.group)
                AndroidUtils.goneIf(!ringPartyData.agilityClass.isAddOnAllowed, btAddOn)
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
            else -> {
                doNothing()
            }
        }
    }

    fun doClick(position: Int) {
        val row = rows[position]

        if (row.heading.isEmpty()) {
            entry.cursor = row.cursor
            ringPartyData.entrySelected(entry.id)
            sendSignal(SignalCode.ENTRY_SELECTED)
        }

    }
    override fun whenItemClick(position: Int) {
        doClick(position)
    }

    override fun whenLongClick(position: Int) {
        doClick(position)
    }

    private fun progressToBand(progress: Int): Int {
        when (progress) {
            PROGRESS_ENTERED -> return 1
            else -> return 2
        }
    }

    private fun doSelect() {
        entry.selectBookIn(ringPartyData.agilityClass.id)

        var band = -1
        rows.clear()
        while (entry.next()) {
            if (entry.progress > band) {
                if (progressToBand(entry.progress) > band) {
                    band = progressToBand(entry.progress)
                    when (band) {
                        1 -> rows.add(Row(heading = "Pre-Entries"))
                    //                        2 -> rows.add(Row(heading = "Booked In"))
                        2 -> rows.add(Row(heading = "Confirmed"))
                    }
                }
            }
            rows.add(Row(cursor = entry.cursor))
        }

        lvRunners.load(this, rows, R.layout.view_one_item_list)
        lvRunners.requestFocus()
    }

    override fun whenPopulate(view: View, position: Int) {
        val row = rows[position]
        val headingText = view.findViewById(R.id.headingText) as TextView
        val listText1 = view.findViewById(R.id.listText1) as TextView

        AndroidUtils.goneIf(row.heading.isEmpty(), headingText)
        AndroidUtils.goneIf(row.heading.isNotEmpty(), listText1)

        if (!row.heading.isEmpty()) {
            headingText.text = row.heading
        } else {
            entry.cursor = row.cursor
            var entryText = entry.teamDescription
            if (entry.progress in PROGRESS_BOOKED_IN..PROGRESS_RUN) {
                entryText += " (${entry.jumpHeightText})"
            }

            if (Competition.enforceMembership && entry.team.hasMembershipIssues) {
                entryText += " (*)"
            }

            listText1.text = entryText
            listText1.setTextColor(if (entry.progress == PROGRESS_ENTERED) BLACK else GREY)
        }
    }

}















