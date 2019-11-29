/*
 * Copyright (c) Mike Brickman 2014-2018
 */

package org.egility.granite.fragments

import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import kotlinx.android.synthetic.main.fragment_public_address.*
import org.egility.library.dbobject.Competition
import org.egility.library.dbobject.Radio
import org.egility.library.general.*
import org.egility.android.BaseFragment
import org.egility.android.tools.*
import org.egility.android.views.DbCursorListView
import org.egility.granite.R

/**
 * Created by mbrickman on 08/10/15.
 */
class PublicAddressFragment : BaseFragment(R.layout.fragment_public_address), DbCursorListView.Listener {

    data class Row(val heading: String = "", val cursor: Int = -1)
    private var rows = ArrayList<Row>()


    var idRadio = 0

    var radio = Radio("idRadio", "ringNumber", "fullText", "timeAnnounced", "dateCreated", "idAgilityClass", "messageTemplate")
    val agilityClass = radio.agilityClass

    init {
        agilityClass.joinToParent("idAgilityClass", "className")
    }

    override fun whenInitialize() {
        pulseRate = 20
        idRadio = 0
    }

    fun displayMessage(force: Boolean = false) {
        if (force || radio.first() && radio.timeAnnounced.isEmpty()) {
            idRadio = radio.id
            tvTime.text = radio.dateCreated.timeText
            if (radio.timeAnnounced.isEmpty()) {
                tvRing.text = "Ring ${radio.ringNumber}"
            } else {
                tvRing.text = "Ring ${radio.ringNumber} (Re-Announcement)"
            }
            tvClass.text = "Class ${agilityClass.name}"
            AndroidUtils.goneIf(radio.messageTemplate.oneOf(RadioTemplate.NOT_BREAKING.code, RadioTemplate.LUNCH_BETWEEN.code), tvClass)
            tvMessage.text = radio.fullText
            btOK.isEnabled = true
            btDelete.isEnabled = true
        } else {
            idRadio = 0
            tvTime.text = ""
            tvRing.text = ""
            tvClass.text = "No Messages"
            AndroidUtils.goneIf(false, tvClass)
            tvMessage.text = ""
            btOK.isEnabled = false
            btDelete.isEnabled = false
        }

    }

    fun doSelect() {
        rows.clear()
        radio.select("radio.idCompetition=${Competition.current.id} && radio.dateCreated>=${now.addMinutes(-60).sqlDateTime}",
                "radio.timeAnnounced=0 DESC, if(radio.timeAnnounced=0, radio.dateCreated, curdate() + interval 1 year), radio.dateCreated DESC")
        var currentBand=0
        while (radio.next()) {
            val band=if (radio.timeAnnounced.isEmpty()) 1 else 2
            if (band!=currentBand) {
                currentBand=band
                when (band) {
                    1-> rows.add(Row("Waiting"))
                    2-> rows.add(Row("Announced"))
                }
            }
            rows.add(Row(cursor = radio.cursor))
        }


        lvMessages.load(this, rows, R.layout.view_radio)
        lvMessages.requestFocus()
    }

    override fun whenClick(view: View) {
        if (view.tag is Signal) {
            sendSignal(view.tag as Signal)
        } else {
            when (view) {
                btBack -> sendSignal(SignalCode.BACK)
                btRefresh -> sendSignal(SignalCode.REFRESH)
                btRadio -> sendSignal(SignalCode.VIRTUAL_RADIO)
                btDelete -> sendSignal(SignalCode.DELETE)
                btOK -> sendSignal(SignalCode.OK)
            }
        }
    }

    override fun whenLongClick(position: Int) {
        doNothing()
    }

    override fun whenItemClick(position: Int) {
        val row=rows[position]
        if (row.cursor>=0) {
            radio.cursor = row.cursor
            displayMessage(force = true)
        }
    }

    override fun whenSignal(signal: Signal) {
        when (signal.signalCode) {
            SignalCode.PAGE_DOWN -> {
                lvMessages.pageDown()
                signal.consumed()
            }
            SignalCode.PAGE_UP -> {
                lvMessages.pageUp()
                signal.consumed()
            }
            SignalCode.RESET_FRAGMENT -> {
                doSelect()
                if (idRadio == 0) displayMessage()
                lvMessages.invalidate()
                signal.consumed()
            }
            SignalCode.PULSE -> {
                sendSignal(SignalCode.RESET_FRAGMENT)
                signal.consumed()
            }
            SignalCode.OK -> {
                if (idRadio > 0) {
                    Radio().seek(idRadio) {
                        timeAnnounced = now
                        post()
                    }
                    idRadio = 0
                    sendSignal(SignalCode.RESET_FRAGMENT)
                }
                signal.consumed()
            }
            SignalCode.DELETE -> {
                if (idRadio > 0) {
                    Radio().seek(idRadio) {
                        whenYes("Are you sure you want to delete?", "Ring $ringNumber: $fullText") {
                            delete()
                            idRadio = 0
                            sendSignal(SignalCode.RESET_FRAGMENT)
                        }
                    }
                }
                signal.consumed()
            }

            else -> {
                doNothing()
            }
        }
    }

    override fun whenPopulate(view: View, position: Int) {
        val row=rows[position]

        val tvTime = view.findViewById(R.id.tvTime) as TextView
        val tvRing = view.findViewById(R.id.tvRing) as TextView
        val tvMessage = view.findViewById(R.id.tvMessage) as TextView
        val tvHeading = view.findViewById(R.id.tvHeading) as TextView
        val loDetail = view.findViewById(R.id.loDetail) as LinearLayout

        AndroidUtils.goneIf(row.heading.isEmpty(), tvHeading)
        AndroidUtils.goneIf(row.heading.isNotEmpty(), loDetail)

        if (row.heading.isEmpty()) {
            radio.cursor = row.cursor
            tvTime.text = radio.dateCreated.timeText
            tvRing.text = "Ring ${radio.ringNumber}"
            tvMessage.text = radio.fullText

        } else {
            tvHeading.text = row.heading
        }
    }

}


