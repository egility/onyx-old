/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.granite.fragments

import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import kotlinx.android.synthetic.main.fragment_scrime_list.*
import org.egility.library.dbobject.Competition
import org.egility.library.dbobject.Entry
import org.egility.library.dbobject.Height
import org.egility.library.dbobject.ScrimeList
import org.egility.library.general.*
import org.egility.android.BaseFragment
import org.egility.android.NavigationGroup
import org.egility.android.tools.*
import org.egility.android.views.DbCursorListView
import org.egility.android.views.QuickButton
import org.egility.granite.R
import java.util.*

open class ScrimeListFragment : BaseFragment(R.layout.fragment_scrime_list), DbCursorListView.Listener {

    var selector: ScrimeList=ScrimeList.ALL
    var paperScrime=this is PaperScrimeListFragment
    
    private var entry = Entry("idEntry", "idAgilityClass", "idTeam", "teamMember", "jumpHeightCode", "progress",
            "scoreCodes", "time", "noTime", "runningOrder", "subClass")

    data class Row(val heading: String = "", val cursor: Int = -1)

    private var rows = ArrayList<Row>()

    override fun whenInitialize() {
    }

    override fun whenClick(view: View) {
        if (view.tag is Signal) {
            sendSignal(view.tag as Signal)
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
                setUpNavigation()
                sendSignal(SignalCode.REFRESH, queued = false)
                signal.consumed()
            }
            SignalCode.REFRESH -> {
                tvPageHeader.text = title
                doSelect()
                lvRunners.invalidate()
                if (ringPartyData.agilityClass.isUka || ringPartyData.agilityClass.isUkOpen) {
                    val groupText = if (ringPartyData.group.isNotEmpty()) " (${ringPartyData.group})" else ""
                    tvPageFooter.text = "Still to run$groupText - " + ringPartyData.agilityClass.stillToRun(ringPartyData.group)
                } else {
                    tvPageFooter.text = ringPartyData.agilityClass.heightProgress()
                }
                signal.consumed()
            }
            else -> {
                doNothing()
            }

        }
    }

    override fun whenItemClick(position: Int) {
        val row = rows[position]

        if (row.heading.isEmpty()) {
            entry.cursor = row.cursor
            ringPartyData.entrySelected(entry.id)
            if (paperScrime) {
                sendSignal(SignalCode.PAPER_SELECTED, entry.id, selector==ScrimeList.RUN)
            } else {
                sendSignal(SignalCode.ENTRY_SELECTED)
            }
        }
    }

    override fun whenLongClick(position: Int) {
        whenItemClick(position)
    }

    private fun progressToBand(progress: Int, noTime: Boolean): Int {
        if (noTime && Competition.isUka) {
            return 5
        }
        when (progress) {
            PROGRESS_CHECKED_IN -> return 1
            PROGRESS_BOOKED_IN -> if (Competition.current.hasBookingIn || Competition.isUka) return 3 else return 2
            PROGRESS_ENTERED -> return 3
            else -> return 4
        }
    }

    private fun doSelect() {
        val jumpHeightCode = if (paperScrime) ringPartyData.jumpHeightCode else ringPartyData.ring.heightCode
        val group = ringPartyData.ring.group
        val heightName = Height.getHeightName(jumpHeightCode)
        entry.selectScrime(ringPartyData.agilityClass, group, jumpHeightCode, ringPartyData.agilityClass.template, 
            scrimeList = selector, hasBookingIn = Competition.hasBookingIn, paperScrime = paperScrime)
        var band = -1
        rows.clear()
        while (entry.next()) {
            if (progressToBand(entry.progress, entry.noTime) != band) {
                band = progressToBand(entry.progress, entry.noTime)
                when (band) {
                    1 -> rows.add(Row(heading = if (Competition.isUkaStyle) "Queuing" else "Ready to Run"))
                    2 -> rows.add(Row(heading = "Marked Late"))
                    3 -> rows.add(Row(heading = "Waiting For"))
                    4 -> rows.add(Row(heading = if (Competition.isUkaStyle) "Finished" else "Scrimed"))
                    5 -> rows.add(Row(heading = "Re-Run Required"))
                }
            }
            rows.add(Row(cursor = entry.cursor))
        }

        lvRunners.load(this, rows, R.layout.running_order_list)
        lvRunners.requestFocus()
        lvRunners.requestFocus()

        val heightCaption = ringPartyData.agilityClass.getHeightCaption(jumpHeightCode)
        for (index in 1..loNavigation.childCount) {
            val button = loNavigation.getChildAt(index) as? QuickButton
            if (button != null) {
                AndroidUtils.disableIf(button.text == heightCaption, button)
            }
        }

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
            when (ringPartyData.agilityClass.template) {
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

            AndroidUtils.goneIf(!(Competition.isKc || ringPartyData.agilityClass.strictRunningOrder), tvRunningOrder)

            when (entry.progress) {
                PROGRESS_ENTERED, PROGRESS_BOOKED_IN, PROGRESS_CHECKED_IN -> {
                    entryText = team
                }
                else -> {
                    entryText = "$team - ${entry.getStatusText(false)}"
                }
            }

            if (Competition.enforceMembership && entry.team.hasMembershipIssues) {
                entryText += " (*)"
            }

            tvRunner.text = entryText
            if (Competition.isKc || ringPartyData.agilityClass.strictRunningOrder) {
                tvRunningOrder.text = "${entry.runningOrder}."
                if (Competition.isUka && ((Competition.hasBookingIn && entry.progress < PROGRESS_CHECKED_IN) || (entry.progress >= PROGRESS_RUNNING && !entry.noTime))) {
                    tvRunner.setTextColor(GREY)
                    tvRunningOrder.setTextColor(GREY)
                } else {
                    tvRunner.setTextColor(BLACK)
                    tvRunningOrder.setTextColor(BLACK)
                }
            } else {
                if (Competition.isUka && (entry.progress == PROGRESS_ENTERED || progressToBand(entry.progress, entry.noTime) == 3)) {
                    tvRunner.setTextColor(GREY)
                    tvRunningOrder.setTextColor(GREY)
                } else {
                    tvRunner.setTextColor(BLACK)
                    tvRunningOrder.setTextColor(BLACK)
                }
            }
        }

    }

    fun setUpNavigation() {
        var selectedHeight = ringPartyData.jumpHeightCode
        var selected = 0

        loNavigation.removeAllViews()
        addNavigationButton(loNavigation, "Back", SignalCode.BACK)

        if (ringPartyData.agilityClass.jumpHeightArray.size > 1) {
            val navigationGroup = NavigationGroup()
            for (height in ringPartyData.agilityClass.jumpHeightArray) {
                if (selectedHeight == "") selectedHeight = height.code
                if (height.code == selectedHeight) selected = navigationGroup.size
                navigationGroup.add(height.heightCaptionShort, SignalCode.HEIGHT_SELECTED, height.code)
            }
            addNavigationGroup(loNavigation, navigationGroup, selected, if (Competition.isKc) 3 else 0)
        }

        addNavigationButton(loNavigation, "Refresh", SignalCode.REFRESH)
        ringPartyData.jumpHeightCode = selectedHeight
    }

}

class PaperScrimeListFragment: ScrimeListFragment()