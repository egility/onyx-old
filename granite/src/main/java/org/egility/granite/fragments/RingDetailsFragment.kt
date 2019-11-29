/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.granite.fragments

import android.graphics.Color
import android.view.View
import android.widget.TextView
import kotlinx.android.synthetic.main.fragment_ring_details.*
import org.egility.library.database.DbQuery
import org.egility.library.dbobject.AgilityClass
import org.egility.library.dbobject.Competition
import org.egility.library.dbobject.Height
import org.egility.library.dbobject.RingSelector
import org.egility.library.general.*
import org.egility.android.BaseFragment
import org.egility.android.tools.Signal
import org.egility.android.tools.SignalCode
import org.egility.android.views.DbCursorListView
import org.egility.granite.R


/**
 * Created by mbrickman on 08/10/15.
 */
class RingDetailsFragment : BaseFragment(R.layout.fragment_ring_details), DbCursorListView.Listener {

    var isRingParty: Boolean = false

    lateinit private var classStats: DbQuery
    
    val agilityClass = AgilityClass()

    val ring = ringPartyData.ring

    data class Line(val title: String, val overview: String, val active: Boolean)

    val lines = ArrayList<Line>()


    fun selectRing(selector: RingSelector) {
        ring.find("ring.idCompetition=${selector.idCompetition} AND ring.date=${selector.date.sqlDate} AND ring.ringNumber=${selector.ringNumber}")
    }

    val keyboardVisible: Boolean
        get() {
            if (!hasView) {
                return false
            } else {
                val heightDiff = loFragment.rootView.height - loFragment.height
                return heightDiff > 100
            }
        }

    override fun whenInitialize() {
        edJudge.setOnFocusChangeListener { view, haveFocus ->
            if (!haveFocus) {
                ring.judge = edJudge.text.toString()
                ring.post()
            }
        }
        edRingManager.setOnFocusChangeListener { view, haveFocus ->
            if (!haveFocus) {
                ring.manager = edRingManager.text.toString()
                ring.post()
            }
        }
        edNote.setOnFocusChangeListener { view, haveFocus ->
            if (!haveFocus) {
                ring.note = edNote.text.toString()
                ring.post()
            }
        }
    }

    override fun whenClick(view: View) {
        when (view) {
            btBack -> sendSignal(SignalCode.BACK)
        }
    }

    override fun whenSignal(signal: Signal) {
        when (signal.signalCode) {
            SignalCode.RESET_FRAGMENT -> {
                doSelect()
                buildLines()
                signal.consumed()
            }
            SignalCode.PULSE -> {
                if (!keyboardVisible) {
                    //sendSignal(SignalCode.RESET_FRAGMENT)
                }
                signal.consumed()
            }
            SignalCode.PAGE_DOWN -> {
                lvClasses.pageDown()
                signal.consumed()
            }
            SignalCode.PAGE_UP -> {
                lvClasses.pageUp()
                signal.consumed()
            }
            else -> {
                doNothing()
            }
        }
    }

    private fun doSelect() {
        ring.refresh()
        val idCompetition = ring.idCompetition
        val date = ring.date
        val ringNumber = ring.number

        agilityClass.select("agilityClass.idCompetition = $idCompetition AND agilityClass.classDate=${date.sqlDate} AND agilityClass.ringNumber=$ringNumber", "agilityClass.ringOrder")
        classStats = AgilityClass.getClassStatsQuery(Competition.current.id, today, ringNumber)

        tvPageHeader.text = "%s - Ring %d".format(Competition.current.uniqueName, ringNumber)
        edJudge.setText(ring.judge)
        edRingManager.setText(ring.manager)
        edNote.setText(ring.note)

        lvClasses.load(this, agilityClass, R.layout.view_class)
        lvClasses.requestFocus()

    }

    override fun whenItemClick(position: Int) {
        agilityClass.cursor = position
        if (!isRingParty) {
            sendSignal(SignalCode.CLASS_SELECTED, agilityClass.id)
        }
    }

    override fun whenLongClick(position: Int) {
        whenItemClick(position)
    }

    fun buildLines() {

        lines.clear()

        agilityClass.forEach {
            var title = ""
            var overview = ""
            val alert = StringBuilder("")
            var active = false


            if (agilityClass.progress == CLASS_CLOSED) {
                title = agilityClass.name + " (Closed)"
                overview = AgilityClass.getHeightData(agilityClass.id, classStats, "run")
            } else if (agilityClass.id == ring.idAgilityClass) {
                active = true
                val height = Height.getHeightName(ring.heightCode)
                val className = agilityClass.name

                if (AgilityClass.noDogsRunYet(classStats, agilityClass.id) && agilityClass.progress <= CLASS_WALKING) {
                    when (agilityClass.progress) {
                        CLASS_PENDING -> {
                            title = "$className (Not Open)"
                            alert.append("$className - Not Open")
                        }
                        CLASS_PREPARING -> {
                            title = "$className (Setting Up)"
                            alert.append("$className - Setting Up")
                        }
                        CLASS_WALKING -> {
                            title = "$className (Walking)"
                            if (agilityClass.walkingOverLunch) {
                                alert.append("$className - Walking over lunch and starting at ${agilityClass.startTime.timeText}")
                            } else {
                                alert.append("$className - Walking now and starting at ${agilityClass.startTime.timeText}")
                            }
                        }
                    }
                } else {
                    title = "$className ($height)"
                    if (Competition.current.isUka || Competition.current.isUkOpen) {
                        alert.append("$className ($height) - " + AgilityClass.getChaseText(classStats, agilityClass.id, ring.heightCode))
                    }
                }
                if (Competition.current.isUka || Competition.current.isUkOpen) {
                    overview = AgilityClass.getHeightData(agilityClass.id, classStats, "notRun")
                } else {
                    overview = agilityClass.heightProgress(classStats)
                }
                tvAlert.text = alert.toString()

            } else {
                title = agilityClass.name
                if (Competition.current.isUka || Competition.current.isUkOpen) {
                    overview = AgilityClass.getHeightData(agilityClass.id, classStats, "preEntered")
                } else {
                    overview = agilityClass.heightProgress(classStats)
                }
            }
            lines.add(Line(title, overview, active))
        }
    }


    override fun whenPopulate(view: View, position: Int) {
        val tvClass = view.findViewById(R.id.tvClass) as TextView
        val tvOverview = view.findViewById(R.id.tvOverview) as TextView
        val line = lines[position]
        tvClass.setTextColor(if (line.active) Color.parseColor("#000000") else Color.parseColor("#666666"))
        tvOverview.setTextColor(if (line.active) Color.parseColor("#000000") else Color.parseColor("#666666"))
        tvClass.text = line.title
        tvOverview.text = line.overview
    }

}
