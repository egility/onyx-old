/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.granite.fragments

import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import kotlinx.android.synthetic.main.fragment_waiting_for_list.*
import org.egility.library.dbobject.Competition
import org.egility.library.dbobject.Entry
import org.egility.library.dbobject.Height
import org.egility.library.general.*
import org.egility.android.BaseFragment
import org.egility.android.NavigationGroup
import org.egility.android.tools.AndroidUtils
import org.egility.android.tools.Signal
import org.egility.android.tools.SignalCode
import org.egility.android.views.DbCursorListView
import org.egility.android.views.QuickButton
import org.egility.granite.R
import java.util.ArrayList

class WaitingForListFragment : BaseFragment(R.layout.fragment_waiting_for_list), DbCursorListView.Listener {

    data class Row(val heading: String = "", val cursor: Int = -1)

    private var entry = Entry("idEntry", "idAgilityClass", "idTeam", "teamMember", "jumpHeightCode", "progress", "scoreCodes", "time", "noTime", "runningOrder")

    private var rows = ArrayList<Row>()

    private val agilityClass
        get() = ringPartyData.agilityClass

    override fun whenInitialize() {
    }

    override fun whenClick(view: View) {
        if (view.tag is Signal) {
            sendSignal(view.tag as Signal)
        }
    }

    private fun checkHeightButtons() {
        for (index in 1..loNavigation.childCount) {
            val button = loNavigation.getChildAt(index) as? QuickButton
            if (button != null) {
                val signal=button.tag as? Signal
                val heightCode=signal?._payload as? String
                AndroidUtils.disableIf(heightCode!=null && heightCode == ringPartyData.jumpHeight.code, button)
            }
        }

    }

    override fun whenSignal(signal: Signal) {
        when (signal.signalCode) {
            SignalCode.RESET_FRAGMENT -> {
                sendSignal(SignalCode.REFRESH, queued = false)
                setUpNavigation()
                checkHeightButtons()
                signal.consumed()
            }
            SignalCode.REFRESH -> {
                doSelect()
                checkHeightButtons()
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
                val newHeightCode = signal._payload as? String
                if (newHeightCode != null) {
                    ringPartyData.jumpHeightCode = newHeightCode
                    sendSignal(SignalCode.REFRESH)
                    signal.consumed()
                }
            }
            else -> {
                doNothing()
            }
        }
    }

    override fun whenLongClick(position: Int) {
    }

    private fun doSelect() {
        if (Competition.isKc) {
            tvPageHeader.text = "Ring ${ringPartyData.ring.number}: ${agilityClass.name}"
        } else {
            tvPageHeader.text = "Manage Queue (Ring ${ringPartyData.ring.number}): ${agilityClass.name}"
        }


        entry.selectMissing(ringPartyData.agilityClass.id, ringPartyData.jumpHeightCode, agilityClass.template)

        var currentBand = -1
        rows.clear()
        if (entry.rowCount == 0) {
            lvRunners.visibility = View.INVISIBLE
            tvNoDogs.visibility = View.VISIBLE
        } else {
            while (entry.next()) {
                val Band = if (entry.progress == PROGRESS_BOOKED_IN) 2 else 1
                if ( Band != currentBand) {
                    currentBand = Band
                    if (Competition.isKc) {
                        when (Band) {
                            1 -> rows.add(Row(heading = "Waiting For"))
                            2 -> rows.add(Row(heading = "Marked Late"))
                        }
                    } else {
                        when (Band) {
                            1 -> rows.add(Row(heading = "Not Booked In"))
                            2 -> rows.add(Row(heading = "Booked In"))
                        }
                    }
                }
                rows.add(Row(cursor = entry.cursor))
            }
            tvNoDogs.visibility = View.INVISIBLE
            lvRunners.visibility = View.VISIBLE
            lvRunners.load(this, rows, R.layout.running_order_list)
            lvRunners.requestFocus()
        }
    }


    override fun whenItemClick(position: Int) {
    }

    override fun whenPopulate(view: View, position: Int) {
        val row = rows[position]
        val tvHeading = view.findViewById(R.id.tvHeading) as TextView
        val loRunner = view.findViewById(R.id.loRunner) as LinearLayout
        val tvRunner = view.findViewById(R.id.tvRunner) as TextView
        val tvRunningOrder = view.findViewById(R.id.tvRunningOrder) as TextView

        AndroidUtils.goneIf(row.heading.isEmpty(), tvHeading)
        AndroidUtils.goneIf(row.heading.isNotEmpty(), loRunner)
        tvRunningOrder.visibility= View.GONE

        if (!row.heading.isEmpty()) {
            tvHeading.text = row.heading
        } else {
            entry.cursor = row.cursor

            val competitor = entry.team.getCompetitor(entry.teamMember)
            var entryText = ""
            var team = ""

            when (agilityClass.template) {
                ClassTemplate.SPLIT_PAIRS, ClassTemplate.KC_PAIRS_JUMPING, ClassTemplate.KC_PAIRS_AGILITY -> {
                    team = entry.team.description
                }
                ClassTemplate.TEAM_INDIVIDUAL -> {
                    team = entry.team.getCompetitorDog(entry.teamMember)
                }
                ClassTemplate.TEAM_RELAY, ClassTemplate.KC_CRUFTS_TEAM -> {
                    team = entry.team.teamName
                }
                else -> {
                    team = entry.team.getCompetitorDog(1)
                }
            }

            entryText = team

            if (Competition.enforceMembership && entry.team.hasMembershipIssues) {
                entryText += " (*)"
            }

            if (competitor != null) {
                val name = if (agilityClass.isRelay) "${competitor.givenName} " else ""
                if (competitor.neededRingNumber > 0 && competitor.neededRingNumber != agilityClass.ringNumber && competitor.neededRingTime > now.addMinutes(-10)) {
                    entryText += " (${name}Needed Ring ${competitor.neededRingNumber})"
                }
            }

            /*
            if (agilityClass.template.teamSize >= 2) {
                val competitor2 = entry.team.competitor2
                if (competitor != null && competitor2 != null && competitor2.id != competitor?.id) {
                    val name = if (agilityClass.isRelay) "${competitor2.givenName} " else ""
                    if (competitor2.neededRingNumber > 0 && competitor2.neededRingNumber != agilityClass.ringNumber && competitor2.neededRingTime > now.addMinutes(-10)) {
                        entryText += " (${name}Needed Ring ${competitor2.neededRingNumber})"
                    }
                }
            }

            if (agilityClass.template.teamSize >= 3) {
                val competitor3 = entry.team.competitor3
                if (competitor3 != null) {
                    val name = if (agilityClass.isRelay) "${competitor3.givenName} " else ""
                    if (competitor3.neededRingNumber > 0 && competitor3.neededRingNumber != agilityClass.ringNumber && competitor3.neededRingTime > now.addMinutes(-10)) {
                        entryText += " (${name}Needed Ring ${competitor3.neededRingNumber})"
                    }
                }
            }
            */
            if (entry.noTime) {
                entryText += " (No Time)"

            }

            tvRunner.text = entryText
        }
    }

    val classHeights = Height()


    fun setUpNavigation() {
        var selectedHeight = ringPartyData.jumpHeightCode
        var selected = 0

        loNavigation.removeAllViews()
        addNavigationButton(loNavigation, "Back", SignalCode.BACK)

        if (classHeights.rowCount == 0) {
            classHeights.selectClassHeights(agilityClass.id, true)
        } else {
            classHeights.beforeFirst()
        }

        if (agilityClass.jumpHeightArray.size > 1) {
            val navigationGroup = NavigationGroup()
            for (height in agilityClass.jumpHeightArray) {
                if (selectedHeight == "") selectedHeight = height.code
                if (height.code == selectedHeight) selected = navigationGroup.size
                navigationGroup.add(height.heightCaptionShort, SignalCode.HEIGHT_SELECTED, height.code)
            }
            addNavigationGroup(loNavigation, navigationGroup, selected, if (Competition.isKc) 4 else 0)
        }

        addNavigationButton(loNavigation, "Refresh", SignalCode.REFRESH)
        if (ringPartyData.jumpHeightCode.isEmpty()) {
            ringPartyData.jumpHeightCode = selectedHeight
        }
    }


}