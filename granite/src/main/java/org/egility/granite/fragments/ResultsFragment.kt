/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.granite.fragments

import android.view.View
import android.widget.RelativeLayout
import android.widget.TextView
import kotlinx.android.synthetic.main.fragment_results_list.*
import org.egility.android.BaseFragment
import org.egility.android.tools.AndroidUtils
import org.egility.android.tools.Signal
import org.egility.android.tools.SignalCode
import org.egility.android.views.DbCursorListView
import org.egility.android.views.QuickButton
import org.egility.granite.R
import org.egility.library.dbobject.AgilityClass
import org.egility.library.dbobject.Entry
import org.egility.library.dbobject.Height
import org.egility.library.general.*


class ResultsFragment : BaseFragment(R.layout.fragment_results_list), DbCursorListView.Listener {

    var subClass = 0
    var parentMode = false

    private val agilityClass
        get() = ringPartyData.agilityClass

    val entryClass: AgilityClass
        get() = if (parentMode) ringPartyData.agilityClass.parentClass else ringPartyData.agilityClass

    private var entry =
        Entry("idEntry", "idAgilityClass", "subDivision", "idTeam", "teamMember", "jumpHeightCode", "scoreCodes", "courseFaults", "timeFaults", "time", "faults", "points", "noTime", "progress", "clearRoundOnly", "subClass")
    
    override fun whenInitialize() {
    }

    override fun whenResumeFromPause() {
        sendSignal(SignalCode.RESET_FRAGMENT)
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
                sendSignal(SignalCode.REFRESH)
                signal.consumed()
            }
            SignalCode.REFRESH -> {
                if (agilityClass.isUka || agilityClass.isUkOpen) {
                    val groupText = if (ringPartyData.group.isNotEmpty()) " (${ringPartyData.group})" else ""
                    tvPageFooter.text = "Still to run$groupText - " + agilityClass.stillToRun(ringPartyData.group)
                } else {
                    tvPageFooter.text = agilityClass.heightProgress()
                }
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
            SignalCode.TOGGLE_PARENT_MODE -> {
                parentMode = !parentMode
                sendSignal(SignalCode.RESET_FRAGMENT)
            }
            SignalCode.SELECT_RESULTS_HEIGHT -> {
                val newHeightCode = signal._payload as? String
                if (newHeightCode != null) {
                    ringPartyData.jumpHeightCode = newHeightCode
                    doSelect()
                    signal.consumed()
                }
            }
            SignalCode.SELECT_RESULTS_SUB_CLASS -> {
                val subClass = signal._payload as Int?
                if (subClass != null) {
                    this.subClass = subClass
                }
                sendSignal(SignalCode.RESET_FRAGMENT)
            }
            else -> {
                doNothing()
            }

        }
    }

    override fun whenItemClick(position: Int) {
        if (!parentMode) {
            entry.cursor = rows[position].cursor
            ringPartyData.entrySelected(entry.id)
            sendSignal(SignalCode.ENTRY_SELECTED)
        }
    }

    override fun whenLongClick(position: Int) {
        whenItemClick(position)
    }

    data class Row(val heading: String = "", val place: Int = -1, val cursor: Int = -1)

    private var rows = ArrayList<Row>()

    private fun doSelect() {

        val heightName = Height.getHeightName(ringPartyData.jumpHeightCode)

        if (agilityClass.isUkaStyle || agilityClass.isFabStyle) {
            entry.selectScoreBoardByHeight(entryClass, ringPartyData.jumpHeightCode, agilityClass)
        } else {
            entry.selectScoreBoardBySubClass(entryClass, subClass)
        }

        val divisionMonitor = ChangeMonitor(9999)
        var place = 1
        rows.clear()
        while (entry.next()) {
            if (divisionMonitor.hasChanged(entry.subDivision) && agilityClass.isFab) {
                place = 1
                rows.add(Row(heading = if (entry.subDivision == 0) "ABC" else "Collie/X"))
            }
            rows.add(Row("", place, entry.cursor))
            place++
        }

        lvRunners.load(this, rows, R.layout.view_results)
        lvRunners.requestFocus()

        tvPageHeader.text =
            if (parentMode)
                "${entryClass.name}"
            else if (agilityClass.isUkaStyle || agilityClass.isFabStyle)
                "Ring ${agilityClass.ringNumber} - ${agilityClass.name} - ${agilityClass.getCourseTime(ringPartyData.jumpHeightCode) / 1000}s"
            else
                "Ring ${agilityClass.ringNumber} - ${agilityClass.name} - ${agilityClass.courseTime / 1000}s"

        if (agilityClass.isUkaStyle || agilityClass.isFabStyle) {
            if (!agilityClass.combineHeights) {
                for (index in 0..loNavigation.childCount - 1) {
                    val button = loNavigation.getChildAt(index) as? QuickButton
                    if (button != null) {
                        button.isEnabled = button.text != heightName
                    }
                }
            }
        } else {
            if (agilityClass.subClassCount > 1) {
                val caption =
                    when (agilityClass.subClassCount) {
                        in 0..3 -> agilityClass.subClassDescription(subClass, shortGrade = false)
                        4 -> agilityClass.subClassDescription(subClass, shortGrade = true)
                        else -> agilityClass.subClassDescription(subClass, shortGrade = true).replace("HO", "")
                    }
                for (index in 0..loNavigation.childCount - 1) {
                    val button = loNavigation.getChildAt(index) as? QuickButton
                    if (button != null) {
                        button.isEnabled = button.text != caption
                    }
                }
            }
        }
    }

    override fun whenPopulate(view: View, position: Int) {
        val row = rows[position]
        val place = row.place

        val tvHeading = view.findViewById(R.id.tvHeading) as TextView
        val loRunner = view.findViewById(R.id.loRunner) as RelativeLayout
        val tvPlace = view.findViewById(R.id.tvPlace) as TextView
        val tvName = view.findViewById(R.id.tvName) as TextView
        val tvResult = view.findViewById(R.id.tvResult) as TextView

        AndroidUtils.goneIf(row.heading.isEmpty(), tvHeading)
        AndroidUtils.goneIf(row.heading.isNotEmpty(), loRunner)

        if (row.heading.isNotEmpty()) {
            tvHeading.text = row.heading
        } else {
            entry.cursor = row.cursor


            val place = "$place."
            val courseFaults = entry.courseFaults
            var team = entry.teamDescription
            if (agilityClass.isKc && agilityClass.template.teamSize == 2) {
                team = "${entry.team.getDogName(1)} & ${entry.team.getDogName(2)}"
            }

            if (entry.clearRoundOnly) {
                team = entry.teamDescription + " CRO"
                tvPlace.text = if (courseFaults + entry.timeFaults == 0) "CR" else ""
            } else {
                tvPlace.text = if (!entry.isEffectivelyEliminated) place else ""
            }
            if (entryClass.template == ClassTemplate.TEAM_INDIVIDUAL) {
                team += " - " + entry.dogName
            }

            tvName.text = team
            tvResult.text = if (entryClass != agilityClass)
                entry.combinedResult
            else
                entry.result
        }
    }


    fun setUpNavigation() {
        loNavigation.removeAllViews()
        addNavigationButton(loNavigation, "Back", SignalCode.BACK)
        if (agilityClass.isUkaStyle || agilityClass.isFabStyle) {
            if (!agilityClass.combineHeights) {
                val height = Height()
                height.selectClassHeights(agilityClass.id, true)
                while (height.next()) {
                    if (ringPartyData.jumpHeightCode.isEmpty()) {
                        ringPartyData.jumpHeightCode = height.code
                    }
                    addNavigationButton(loNavigation, height.jumpName, SignalCode.SELECT_RESULTS_HEIGHT, height.code)
                }
            }
        } else {
            if (agilityClass.subClassCount > 1) {
                for (subClass in 0..agilityClass.subClassCount - 1) {
                    val caption =
                        when (agilityClass.subClassCount) {
                            in 0..3 -> agilityClass.subClassDescription(subClass, shortGrade = false)
                            4 -> agilityClass.subClassDescription(subClass, shortGrade = true)
                            else -> agilityClass.subClassDescription(subClass, shortGrade = true).replace("HO", "")
                        }
                    addNavigationButton(loNavigation, caption, SignalCode.SELECT_RESULTS_SUB_CLASS, subClass)
                }
                loNavigation.visibility = View.VISIBLE
            } else {
                loNavigation.visibility = View.GONE
            }
        }
        if (agilityClass.template.type == CLASS_TYPE_SPECIAL_GROUP_MEMBER) {
            val final =
                addNavigationButton(loNavigation, if (parentMode) "Heat" else "Final", SignalCode.TOGGLE_PARENT_MODE)
        }
        addNavigationButton(loNavigation, "Refresh", SignalCode.REFRESH)
    }


}