/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.granite.fragments

import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import kotlinx.android.synthetic.main.fragment_class_queue.*
import org.egility.android.BaseFragment
import org.egility.android.tools.AndroidUtils
import org.egility.android.tools.Signal
import org.egility.android.tools.SignalCode
import org.egility.android.views.DbCursorListView
import org.egility.granite.R
import org.egility.library.dbobject.Competition
import org.egility.library.dbobject.Entry
import org.egility.library.dbobject.Height
import org.egility.library.general.*
import java.util.*


class QueueFragment : BaseFragment(R.layout.fragment_class_queue),
    DbCursorListView.Listener {

    data class Row(val heading: String = "", val cursor: Int = -1)

    private var entry = Entry(
        "idEntry",
        "idAgilityClass",
        "idTeam",
        "teamMember",
        "jumpHeightCode",
        "progress",
        "scoreCodes",
        "time",
        "noTime",
        "runningOrder"
    )

    private var rows = ArrayList<Row>()

    private val agilityClass
        get() = ringPartyData.agilityClass

    override fun whenInitialize() {
        if (agilityClass.isKc) {
            tvNoDogs.text = "No Dogs Ready to Run"
        }
    }

    override fun whenClick(view: View) {
        if (view.tag is Signal) {
            sendSignal(view.tag as Signal)
        } else {
            when (view) {
                btBack -> sendSignal(SignalCode.BACK)
                btRefresh -> sendSignal(SignalCode.REFRESH)
            }
        }
    }

    override fun whenSignal(signal: Signal) {
        when (signal.signalCode) {
            SignalCode.RESET_FRAGMENT -> {
                sendSignal(SignalCode.REFRESH, queued = false)
                signal.consumed()
            }
            SignalCode.REFRESH -> {
                doSelect()
                lvRunners.invalidate()
                if (agilityClass.isUka || agilityClass.isUkOpen) {
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

    override fun whenLongClick(position: Int) {
        whenItemClick(position)
    }

    private fun doSelect() {
        if (Competition.isKc) {
            tvPageHeader.text = "Ring ${ringPartyData.ring.number}: ${agilityClass.name}"
        } else {
            tvPageHeader.text = "Manage Queue (Ring ${ringPartyData.ring.number}): ${agilityClass.name}"
        }


        entry.selectQueue(
            ringPartyData.agilityClass.id,
            ringPartyData.group,
            agilityClass.heightRunningOrder,
            agilityClass.template,
            agilityClass.strictRunningOrder     
        )

        var heightBand = ""
        var currentBand = -1
        rows.clear()
        if (entry.rowCount == 0) {
            lvRunners.visibility = View.INVISIBLE
            tvNoDogs.visibility = View.VISIBLE
        } else {
            while (entry.next()) {
                val Band = when {
                    Competition.isUka && entry.noTime -> 2
                    entry.progress == PROGRESS_BOOKED_IN -> 3
                    else -> 1
                }
                if (entry.jumpHeightCode != heightBand || Band != currentBand) {
                    heightBand = entry.jumpHeightCode
                    currentBand = Band
                    val height = if (agilityClass.multipleHeights)
                        if (agilityClass.lhoClass) ": ${Height.getHeightJumpName(heightBand)}" else ": ${Height.getHeightName(
                            heightBand
                        )}"
                    else ""
                    when (Band) {
                        1 -> rows.add(Row(heading = if (Competition.isKc) "Ready to Run$height" else "Queuing$height"))
                        2 -> rows.add(Row(heading = "Re-Run Required$height"))
                        3 -> rows.add(Row(heading = if (Competition.isKc) "Marked Late$height" else "Waiting For$height"))
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
        val row = rows[position]

        if (row.heading.isEmpty()) {
            entry.cursor = row.cursor
            ringPartyData.entrySelected(entry.id)
            sendSignal(SignalCode.ENTRY_SELECTED)
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
            tvRunningOrder.text = "${entry.runningOrder}."
            AndroidUtils.goneIf(!(Competition.isKc || agilityClass.strictRunningOrder), tvRunningOrder)

            if (Competition.enforceMembership && entry.team.hasMembershipIssues) {
                entryText += " (*)"
            }

            if (competitor != null) {
                val name = if (agilityClass.isRelay) "${competitor.givenName} " else ""
                if (competitor.neededRingNumber > 0 && competitor.neededRingNumber != agilityClass.ringNumber && competitor.neededRingTime > now.addMinutes(
                        -10
                    )
                ) {
                    entryText += " (${name}Needed Ring ${competitor.neededRingNumber})"
                }
            }
            if (entry.noTime) {
                entryText += " (No Time)"

            }

            tvRunner.text = entryText
        }
    }

}