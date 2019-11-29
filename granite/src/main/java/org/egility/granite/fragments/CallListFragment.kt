/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.granite.fragments

import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import org.egility.library.dbobject.Competition
import org.egility.library.dbobject.Entry
import org.egility.library.general.*
import org.egility.android.BaseFragment
import org.egility.android.NavigationGroup
import org.egility.android.tools.*
import org.egility.android.views.DbCursorListView
import org.egility.android.views.QuickButton
import org.egility.granite.R
import kotlinx.android.synthetic.main.fragment_call_list.*


class CallListFragment : BaseFragment(R.layout.fragment_call_list), DbCursorListView.Listener {

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
                byRunningOrder = agilityClass.strictRunningOrder
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
            else -> {
                doNothing()
            }
        }
    }

    private fun doSelect() {
        if (agilityClass.jumpHeightArray.size > 1) {
            tvPageHeader.text = "Entries (Ring ${ringPartyData.ring.number}): ${agilityClass.name} (${agilityClass.getHeightCaption(ringPartyData.jumpHeightCode)})"
        } else {
            tvPageHeader.text = "Entries (Ring ${ringPartyData.ring.number}): ${agilityClass.name}"
        }
        entry.selectAll(agilityClass, ringPartyData.jumpHeightCode, byRunningOrder)

        lvRunners.load(this, entry, R.layout.running_order_list)
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
            entry.cursor = position
            ringPartyData.entrySelected(entry.id)
            sendSignal(SignalCode.ENTRY_SELECTED)
    }

    override fun whenLongClick(position: Int) {
        whenItemClick(position)
    }

    override fun whenPopulate(view: View, position: Int) {
        val tvHeading = view.findViewById(R.id.tvHeading) as TextView
        val loRunner = view.findViewById(R.id.loRunner) as LinearLayout
        val tvRunner = view.findViewById(R.id.tvRunner) as TextView
        val tvRunningOrder = view.findViewById(R.id.tvRunningOrder) as TextView

        AndroidUtils.goneIf(true, tvHeading)
        AndroidUtils.goneIf(false, loRunner)

            entry.cursor = position

            var entryText = ""

            when (agilityClass.template) {
                ClassTemplate.SPLIT_PAIRS, ClassTemplate.KC_PAIRS_JUMPING, ClassTemplate.KC_PAIRS_AGILITY -> {
                    entryText = entry.team.description
                }
                ClassTemplate.TEAM_INDIVIDUAL -> {
                    entryText = entry.team.getCompetitorDog(entry.teamMember)
                }
                ClassTemplate.TEAM_RELAY, ClassTemplate.KC_CRUFTS_TEAM -> {
                    entryText = entry.team.teamName
                }
                else -> {
                    entryText = entry.team.getCompetitorDog(1)
                }
            }

            tvRunningOrder.text = "${entry.runningOrder}."
            AndroidUtils.goneIf(!byRunningOrder, tvRunningOrder)

            if (!byRunningOrder) {
                entryText += " (${entry.runningOrder})"
            }

            tvRunner.text = entryText
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
            addNavigationGroup(loNavigation, navigationGroup, selected, if(Competition.isKc) 4 else 0)
        }
    }


}