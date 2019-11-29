/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.granite.fragments

import android.view.View
import android.widget.TextView
import kotlinx.android.synthetic.main.fragment_ring_list.*
import org.egility.library.database.DbQuery
import org.egility.library.dbobject.AgilityClass
import org.egility.library.dbobject.Competition
import org.egility.library.dbobject.Height
import org.egility.library.dbobject.Ring
import org.egility.library.general.*
import org.egility.android.BaseFragment
import org.egility.android.tools.Signal
import org.egility.android.tools.SignalCode
import org.egility.android.views.DbCursorListView
import org.egility.granite.R


/**
 * Created by mbrickman on 07/10/15.
 */
class RingListFragment : BaseFragment(R.layout.fragment_ring_list), DbCursorListView.Listener {

    private var ring = Ring("idCompetition", "date", "ringNumber", "idAgilityClass", "heightCode", "note")
    lateinit private var classStats: DbQuery

    data class Line(val title: String, val overview: String, val alert: String)
    val lines = ArrayList<Line>()

    init {
        ring.agilityClass.joinToParent()
    }

    override fun whenInitialize() {
        tvPageHeader.text = Competition.current.uniqueName + " - Ring Overview"
    }

    override fun whenClick(view: View) {
        when (view) {
            btBack -> sendSignal(SignalCode.BACK)
        }
    }

    override fun whenSignal(signal: Signal) {

        fun doSelect() {
            with(Competition.current) {
                val ringDate = today
                ring.select(where = "ring.idCompetition=$id AND ring.date = ${ringDate.sqlDate}", orderBy = "ring.ringNumber")
                classStats = AgilityClass.getClassStatsQuery(id, ringDate, 0)
                buildLines()
            }

            debugTime("RingListFragment", "lvRings.load", {
                lvRings.load(this, ring, R.layout.view_ring)
            })

            lvRings.requestFocus()
        }

        when (signal.signalCode) {
            SignalCode.PAGE_DOWN -> {
                lvRings.pageDown()
                signal.consumed()
            }
            SignalCode.PAGE_UP -> {
                lvRings.pageUp()
                signal.consumed()
            }
            SignalCode.RESET_FRAGMENT -> {
                doSelect()
                lvRings.invalidate()
                signal.consumed()
            }
            SignalCode.PULSE -> {
                sendSignal(SignalCode.RESET_FRAGMENT)
                signal.consumed()
            }
            else -> {
                doNothing()
            }
        }
    }

    override fun whenItemClick(position: Int) {
        ring.cursor = position
        sendSignal(SignalCode.RING_DETAILS, ring.selector)
    }

    override fun whenLongClick(position: Int) {
        ring.cursor = position
        sendSignal(SignalCode.CLASS_SELECTED, ring.idAgilityClass)
    }

    fun buildLines() {
        lines.clear()

        ring.forEach {
            var title = ""
            var overview = ""

            if (ring.idAgilityClass == 0) {
                ring.selectFirstOpenAgilityClass()
            }

            val agilityClass = ring.agilityClass
            var alert = agilityClass.briefInfo.capitalize()

            if (agilityClass.progress == CLASS_CLOSED) {
                title = "Ring ${ring.number} - ${agilityClass.name} (Closed)"
            } else if (AgilityClass.noDogsRunYet(classStats, agilityClass.id) && agilityClass.progress <= CLASS_WALKING) {

                when (agilityClass.progress) {
                    CLASS_PENDING -> {
                        title = "Ring ${ring.number} - ${agilityClass.name} (Not Open)"
                    }
                    CLASS_PREPARING -> {
                        title = "Ring ${ring.number} - ${agilityClass.name} (Setting Up)"
                    }
                    CLASS_WALKING -> {
                        val whenWalking = if (agilityClass.walkingOverLunch) "over lunch" else "now"
                        title = "Ring ${ring.number} - ${agilityClass.name} (Walking)"
                    }
                }

            } else {
                val height = if (agilityClass.template.nameTemplate.contains("<height>")) Height.getHeightJumpName(ring.heightCode) else  Height.getHeightJumpNameEx(ring.heightCode)
                title = "Ring ${ring.number} - ${agilityClass.name} ($height)"
                if (Competition.current.hasBookingIn) {
                    alert = AgilityClass.getChaseText(classStats, agilityClass.id, ring.heightCode)
                }
            }

            if (Competition.current.isUka || Competition.current.isUkOpen) {
                overview = AgilityClass.getHeightData(agilityClass.id, classStats, "notRun")
            } else {
                overview = agilityClass.heightProgress(classStats)
            }
            if (!ring.note.isEmpty()) {
                alert = alert.newlineAppend("Note: " + ring.note)
            }

            lines.add(Line(title, overview, alert))

        }
    }


    override fun whenPopulate(view: View, position: Int) {
        val tvRing = view.findViewById(R.id.tvClass) as TextView
        val tvOverview = view.findViewById(R.id.tvOverview) as TextView
        val tvAlert = view.findViewById(R.id.tvAlert) as TextView

        val line = lines[position]
        tvRing.text = line.title
        tvOverview.text = line.overview
        if (line.alert.isEmpty()) {
            tvAlert.visibility = View.GONE
        } else {
            tvAlert.text = line.alert
            tvAlert.visibility = View.VISIBLE
        }
    }

}

