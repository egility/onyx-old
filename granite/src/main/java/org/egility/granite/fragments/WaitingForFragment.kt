/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.granite.fragments

import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import kotlinx.android.synthetic.main.fragment_checkin_list.*
import org.egility.library.dbobject.Competition
import org.egility.library.dbobject.Entry
import org.egility.library.general.*
import org.egility.android.BaseFragment
import org.egility.android.NavigationGroup
import org.egility.android.tools.*
import org.egility.android.views.DbCursorListView
import org.egility.android.views.QuickButton
import org.egility.granite.R
import org.egility.granite.activities.Queue
import java.util.*


class WaitingForFragment : BaseFragment(R.layout.fragment_waiting_for), DbCursorListView.Listener {

    val readOnly: Boolean = false

    data class Row(val heading: String = "", val cursor: Int = -1)
    private var rows = ArrayList<Row>()

    private var entry = Entry("idEntry", "idAgilityClass", "idTeam", "teamMember", "jumpHeightCode", "progress", "scoreCodes", "time", "noTime", "runningOrder")

    private val agilityClass
        get() = ringPartyData.agilityClass


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
                if (agilityClass.isUkaStyle) {
                    val groupText = if (ringPartyData.group.isNotEmpty()) " (${ringPartyData.group})" else ""
                    tvPageFooter.text = "Waiting for$groupText - " + agilityClass.waitingFor(ringPartyData.group)
                } else {
                    tvPageFooter.text = agilityClass.heightProgress()
                }
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

    private fun doSelect() {
        agilityClass.refresh()
        var category = ringPartyData.group
        if (agilityClass.jumpHeightArray.size > 1)  category = category.append(agilityClass.getHeightCaption(ringPartyData.jumpHeightCode), "/")
        if (category.isNotEmpty()) {
            tvPageHeader.text = "Check In (Ring ${ringPartyData.ring.number}): ${agilityClass.name} ($category)"
        } else {
            tvPageHeader.text = "Check In (Ring ${ringPartyData.ring.number}): ${agilityClass.name}"
        }
        entry.selectWaitingFor(agilityClass, ringPartyData.group, ringPartyData.jumpHeightCode, Competition.hasBookingIn, byRunningOrder)

        var bookedInHeading = false
        var withdrawnHeading = false

        rows.clear()
        while (entry.next()) {
            if (Competition.hasBookingIn && entry.progress < PROGRESS_BOOKED_IN && !bookedInHeading && agilityClass.bookIn) {
                bookedInHeading = true
                rows.add(Row(heading = "Not Booked In"))
            }
            if (entry.progress == PROGRESS_WITHDRAWN && !withdrawnHeading) {
                withdrawnHeading = true
                rows.add(Row(heading = "Withdrawn"))
            }
            rows.add(Row(cursor = entry.cursor))
        }

        lvRunners.load(this, rows, R.layout.running_order_list)
        lvRunners.requestFocus()

        val heightCaption = agilityClass.getHeightCaption(ringPartyData.jumpHeightCode, short=(agilityClass.isFab))
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
            ringPartyData.entrySelected(entry.id)
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
            tvRunningOrder.text = "${entry.runningOrder}."
            AndroidUtils.goneIf(!(Competition.isKc || agilityClass.strictRunningOrder), tvRunningOrder)

            if (Competition.enforceMembership && entry.team.hasMembershipIssues) {
                entryText += " (*)"
            }
            
            // update waiting information
            if (agilityClass.isUka) {
                val competitor = entry.team.getCompetitor(entry.teamMember)
                if (competitor != null) {
                    val name = if (agilityClass.isRelay) "${competitor.givenName} " else ""
                    if (entry.progress == PROGRESS_BOOKED_IN) {
                        val ringEvent = competitor.ringEvent(agilityClass.ringNumber)
                        if (ringEvent.isNotEmpty()) {
                            entryText += " (${name}${ringEvent})"
                            if (competitor.neededRingNumber != agilityClass.ringNumber && competitor.neededRingTime < now.addMinutes(-15)) {
                                competitor.neededRingNumber = agilityClass.ringNumber
                                competitor.neededRingTime = now
                                competitor.post()
                            }
                        }
                    }
                }
            }
            
            if (!Competition.hasBookingIn && entry.progress == PROGRESS_BOOKED_IN && Competition.isKc) {
                entryText += " (L)"
            }
            tvRunner.text = entryText
            if (entry.progress == PROGRESS_WITHDRAWN || (Competition.hasBookingIn && entry.progress == PROGRESS_ENTERED && agilityClass.bookIn)) {
                tvRunner.setTextColor(GREY)
                tvRunningOrder.setTextColor(GREY)
            } else {
                tvRunner.setTextColor(BLACK)
                tvRunningOrder.setTextColor(BLACK)
            }
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
            addNavigationGroup(loNavigation, navigationGroup, selected, if(Competition.isKc) 2 else 0)
        }

        if (false && Competition.isKc) {
            addNavigationButton(loNavigation, "- 10", SignalCode.CALL_TO, -10)
            addNavigationButton(loNavigation, "+ 10", SignalCode.CALL_TO, +10)

        }

        if (Competition.isKc) {
            addNavigationButton(loNavigation, "PA", SignalCode.VIRTUAL_RADIO, -10)
        }

        if (!readOnly && agilityClass.isAddOnAllowed) {
            addNavigationButton(loNavigation, "Search", SignalCode.LOOKUP_TEAM)
        } else if (agilityClass.isKc) {
            addNavigationButton(loNavigation, "r/o", SignalCode.LOOKUP_TEAM)
        }

//        addNavigationButton(loNavigation, if (activity is Queue) "Queue" else "Review", SignalCode.RUNNERS_LIST)
        addNavigationButton(loNavigation, if (activity is Queue) "Review" else "Review", SignalCode.RUNNERS_LIST)
//        addNavigationButton(loNavigation, "Refresh", SignalCode.REFRESH)
        if (ringPartyData.jumpHeightCode.isEmpty()) {
            ringPartyData.jumpHeightCode = selectedHeight
        }
    }


}