/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.granite.fragments

import android.graphics.Typeface
import android.view.View
import android.widget.RelativeLayout
import android.widget.TextView
import kotlinx.android.synthetic.main.fragment_scrore_board_list.*
import org.egility.android.BaseFragment
import org.egility.android.tools.AndroidServices
import org.egility.android.tools.AndroidUtils
import org.egility.android.tools.Signal
import org.egility.android.tools.SignalCode
import org.egility.android.views.DbCursorListView
import org.egility.android.views.QuickButton
import org.egility.granite.R
import org.egility.library.dbobject.AgilityClass
import org.egility.library.dbobject.Competition
import org.egility.library.dbobject.Entry
import org.egility.library.dbobject.Height
import org.egility.library.general.*


class ScoreBoardFragment : BaseFragment(R.layout.fragment_scrore_board_list), DbCursorListView.Listener {

    private var entry =
        Entry("idEntry", "idAgilityClass", "subDivision", "idTeam", "teamMember", "jumpHeightCode", "scoreCodes", "courseFaults", "time", "noTime", "progress", "runEnd", "subClass")

    val agilityClass: AgilityClass
        get() = ringPartyData.ring.agilityClass

    var idAgilityClass = 0
    var tempJumpHeightCode = ""
    var tempSubClass = 0
    var tempExpire = nullDate
    var jumpHeightCode = ""
    var subClass = 0

    var cutOffTime = nullDate
    var appealTime = nullDate
    var lastRun = 0
    var lastRunTime = nullDate

    override fun whenInitialize() = tvPageHeader.setOnLongClickListener {
        sendSignal(SignalCode.EXIT)
        true
    }

    override fun whenClick(view: View) {
        if (view.tag is Signal) {
            sendSignal(view.tag as Signal)
        }
    }

    override fun whenSignal(signal: Signal) {
        when (signal.signalCode) {
            SignalCode.PAGE_DOWN -> {
                if (isPatternMatch("ddd") && AndroidServices.isT63) {
                    sendSignal(SignalCode.EXIT)
                } else {
                    lvRunners.pageDown()
                }
                signal.consumed()
            }
            SignalCode.PAGE_UP -> {
                lvRunners.pageUp()
                signal.consumed()
            }
            SignalCode.RESET_FRAGMENT -> {
                ringPartyData.syncRing()
//                Height.selectHeight(agilityClass.lastRunnerHeightCode)
                if (agilityClass.isUkaStyle) {
                    val groupText = if (ringPartyData.group.isNotEmpty()) " (${ringPartyData.group})" else ""
                    tvPageFooter.text = "Still to run$groupText - " + agilityClass.stillToRun(ringPartyData.group)
                } else {
                    tvPageFooter.text = agilityClass.heightProgress()
                }
                val note = ringPartyData.ring.note
                if (!note.isEmpty()) {
                    tvNotes.text = note
                    tvNotes.visibility = View.VISIBLE
                } else {
                    tvNotes.visibility = View.GONE
                }

                doSelect()
                if (agilityClass.isUkaStyle || agilityClass.isFabStyle) {
                    tvPageHeader.text = agilityClass.getClassHeightTimeTitle(jumpHeightCode)
                } else {
                    tvPageHeader.text = agilityClass.getClassTimeTitle()
                }

                lvRunners.invalidate()
                tvWalkingNote.visibility = View.GONE
                if (lvRunners.adapter.count == 0 || agilityClass.progress==CLASS_CLOSED_FOR_LUNCH) {
                    lvRunners.visibility = View.GONE
                    tvWalkingNote.visibility = View.VISIBLE
                    when (agilityClass.progress) {
                        CLASS_PENDING, CLASS_PREPARING -> {
                            tvWalkingNote.text = agilityClass.name + " walking shortly"
                        }
                        CLASS_WALKING -> {
                            if (agilityClass.walkingOverLunch) {
                                tvWalkingNote.text =
                                    agilityClass.name + " walking over lunch and starting at " + agilityClass.startTime.timeText
                            } else {
                                tvWalkingNote.text =
                                    agilityClass.name + " walking now and starting at " + agilityClass.startTime.timeText
                            }
                        }
                        CLASS_CLOSED_FOR_LUNCH -> {
                            if (agilityClass.ring.lunchEnd.isNotEmpty()) {
                                tvWalkingNote.text =
                                    agilityClass.name + " closed for lunch and resuming at " + agilityClass.ring.lunchEnd.timeText + " (No Walking)"
                            } else {
                                tvWalkingNote.text =
                                    agilityClass.name + " closed for lunch (No Walking)"
                            }
                        }
                        else -> {
                            tvWalkingNote.visibility = View.GONE
                        }
                    }
                } else if (lvRunners.visibility == View.GONE) {
                    lvRunners.visibility = View.VISIBLE
                }
                signal.consumed()
            }
            SignalCode.SELECT_RESULTS_HEIGHT -> {
                val jumpHeightCode = signal._payload as String?
                if (jumpHeightCode != null) {
                    tempJumpHeightCode = jumpHeightCode
                    tempExpire = now.addSeconds(10)
                }
                sendSignal(SignalCode.RESET_FRAGMENT)
            }
            SignalCode.SELECT_RESULTS_SUB_CLASS -> {
                val subClass = signal._payload as Int?
                if (subClass != null) {
                    tempSubClass = subClass
                    tempExpire = now.addSeconds(10)
                }
                sendSignal(SignalCode.RESET_FRAGMENT)
            }
            else -> {
                doNothing()
            }

        }
    }

    override fun whenItemClick(position: Int) {
    }

    override fun whenLongClick(position: Int) {
    }

    data class Row(val heading: String = "", val place: Int = -1, val cursor: Int = -1)

    private var rows = ArrayList<Row>()

    data class DivisionData(var total: Int = 0, var selected: Int = 0, var toAllocate: Int = 0)

    private fun doSelect() {
        val cursors = ArrayList<Int>()
        val divisions = ArrayList<DivisionData>()

        ringPartyData.syncRing()
        if (agilityClass.id != idAgilityClass) {
            idAgilityClass = agilityClass.id
            setUpNavigation()
        }

        if (agilityClass.isUkaStyle || agilityClass.isFabStyle) {
            if (tempExpire > now) {
                jumpHeightCode = tempJumpHeightCode
            } else {
                jumpHeightCode = agilityClass.lastRunnerJumpHeightCode
            }
            entry.selectScoreBoardByHeight(agilityClass, jumpHeightCode)
        } else {
            if (tempExpire > now) {
                subClass = tempSubClass
            } else {
                subClass = agilityClass.lastRunnerSubClass
            }
            entry.selectScoreBoardBySubClass(agilityClass, subClass)
        }
        rows.clear()
        cutOffTime = now.addMinutes(-10)
        appealTime = now.addMinutes(-5)
        lastRun = -1
        lastRunTime = nullDate

        while (entry.next()) {
            while (entry.subDivision >= divisions.size) {
                divisions.add(DivisionData())
            }
            divisions[entry.subDivision].total++
            if (entry.runEnd > cutOffTime) {
                cursors.add(entry.cursor)
                divisions[entry.subDivision].selected++
                if (entry.runEnd > lastRunTime) {
                    lastRun = entry.cursor
                    lastRunTime = entry.runEnd
                }
            }
        }

        val headings = if (divisions.size == 1) 0 else divisions.size
        val totalToAllocate = 19 - headings - cursors.size
        val totalRemaining = entry.rowCount - cursors.size

        for (division in divisions) {
            val remaining = division.total - division.selected
            division.toAllocate = if (totalRemaining==0) 0 else (((remaining * totalToAllocate) + (totalRemaining / 2)) / totalRemaining)
        }

        entry.beforeFirst()
        while (entry.next()) {
            val division = divisions[entry.subDivision]
            if (division.toAllocate > 0 && !entry.isEffectivelyEliminated && !cursors.contains(entry.cursor)) {
                cursors.add(entry.cursor)
                division.toAllocate--
            }
        }

        entry.beforeFirst()
        val divisionMonitor = ChangeMonitor(9999)
        var place = 1
        while (entry.next()) {
            if (divisionMonitor.hasChanged(entry.subDivision) && headings > 0) {
                place = 1
                rows.add(Row(heading = if (entry.subDivision == 0) "ABC" else "Collie/X"))
            }
            if (cursors.contains(entry.cursor)) {
                rows.add(Row("", place, entry.cursor))
                
            }
            place++
        }

        lvRunners.load(this, rows, R.layout.view_results)
        lvRunners.requestFocus()

        if (agilityClass.isUkaStyle || agilityClass.isFabStyle) {
            if (!agilityClass.combineHeights) {
                val heightName = Height.getHeightName(jumpHeightCode)
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

            val courseFaults = entry.courseFaults
            var team = entry.teamDescription
            if (agilityClass.isKc && agilityClass.template.teamSize == 2) {
                team = "${entry.team.getDogName(1)} & ${entry.team.getDogName(2)}"
            }
            if (agilityClass.template == ClassTemplate.TEAM_INDIVIDUAL) {
                team += " - " + entry.dogName
            }

            tvPlace.text = if (!entry.isEffectivelyEliminated) place.toString() else ""
            if (Competition.isUkaStyle) {
                if (entry.runEnd > appealTime) {
                    tvName.text = "+" + team
                } else if (entry.runEnd > cutOffTime) {
                    tvName.text = team
                } else {
                    tvName.text = team
                }
            } else {
                if (entry.runEnd > cutOffTime) {
                    tvName.text = "+" + team
                } else {
                    tvName.text = team
                }
            }
            tvResult.text = entry.result

            if (entry.cursor == lastRun) {
                tvPlace.setTypeface(null, Typeface.BOLD);
                tvName.setTypeface(null, Typeface.BOLD);
                tvResult.setTypeface(null, Typeface.BOLD);
            } else {
                tvPlace.setTypeface(null, Typeface.NORMAL);
                tvName.setTypeface(null, Typeface.NORMAL);
                tvResult.setTypeface(null, Typeface.NORMAL);
            }
        }
    }

    fun setUpNavigation() {
        loNavigation.removeAllViews()
        if (agilityClass.isUkaStyle || agilityClass.isFabStyle) {
            if (!agilityClass.combineHeights) {
                val height = Height()
                height.selectClassHeights(agilityClass.id, true)
                while (height.next()) {
                    addNavigationButton(loNavigation, height.name, SignalCode.SELECT_RESULTS_HEIGHT, height.code)
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
    }


}