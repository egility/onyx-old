/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.granite.fragments

import android.view.View
import android.widget.TextView
import kotlinx.android.synthetic.main.list_view_holder.*
import org.egility.library.dbobject.Competition
import org.egility.library.dbobject.CompetitionLedger
import org.egility.library.dbobject.Entry
import org.egility.library.general.*
import org.egility.android.BaseFragment
import org.egility.android.views.DbCursorListView
import org.egility.android.tools.Signal
import org.egility.android.tools.SignalCode
import org.egility.granite.R

/**
 * Created by mbrickman on 17/02/16.
 */
class CreditsUsedFragment : BaseFragment(R.layout.list_view_holder), DbCursorListView.Listener {

    var data = MemberServicesData
    
    init {
        isBackable = false
    }

    var entry = Entry()
    var returnSignal: SignalCode = SignalCode.DO_NOTHING

    override fun whenClick(view: View) {
        if (view.tag is Signal) {
            sendSignal(view.tag as Signal)
        } else {
            when (view) {
                btBack -> sendSignal(SignalCode.BACK)
            }
        }
    }

    override fun whenSignal(signal: Signal) {
        when (signal.signalCode) {
            SignalCode.RESET_FRAGMENT -> {
                btBack.text="OK"
                tvPageHeader.text = "${data.account.code} - Credits Used"
                entry.agilityClass.joinToParent()
                entry.team.joinToParent()
                entry.team.dog.joinToParent()
                entry.select("""
                agilityClass.idCompetition=${Competition.current.id} AND
                entry.idAccount=${data.idAccount} AND
                entry.lateEntryCredits > 0
                """, "agilityClass.classDate, agilityClass.ringNumber, agilityClass.ringOrder, dog.petName")
                CompetitionLedger.fixUsed(Competition.current.id, data.idAccount)
                lvData.load(this, entry, R.layout.template_single_line)
                lvData.requestFocus()
                signal.consumed()
            }
            SignalCode.PAGE_DOWN -> {
                lvData.pageDown()
                signal.consumed()
            }
            SignalCode.PAGE_UP -> {
                lvData.pageUp()
                signal.consumed()
            }
            SignalCode.BACK -> {
                if (returnSignal != SignalCode.DO_NOTHING) {
                    sendSignal(returnSignal)
                    signal.consumed()
                }
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

    override fun whenPopulate(view: View, position: Int) {
        entry.cursor = position
        val tvLine = view.findViewById(R.id.tvLine) as TextView
        var times = if (entry.lateEntryCredits>1) " x ${entry.lateEntryCredits}" else ""
        tvLine.text = "${position + 1}. ${entry.agilityClass.date.dayNameShort}: ${entry.agilityClass.name} - ${entry.team.dog.cleanedPetName} (${entry.progressText})$times"
    }

}