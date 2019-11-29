/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.granite.fragments

import android.view.View
import android.widget.TextView
import kotlinx.android.synthetic.main.list_view_holder.*
import org.egility.library.dbobject.Competition
import org.egility.library.dbobject.CompetitionLedger
import org.egility.library.general.*
import org.egility.android.BaseFragment
import org.egility.android.tools.BLACK
import org.egility.android.tools.GREY
import org.egility.android.tools.Signal
import org.egility.android.tools.SignalCode
import org.egility.android.views.DbCursorListView
import org.egility.granite.R

/**
 * Created by mbrickman on 17/02/16.
 */
class TransactionsFragment : BaseFragment(R.layout.list_view_holder), DbCursorListView.Listener {

    var data = MemberServicesData
    var ledger = CompetitionLedger()

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
                data._editLedger = ledger
                tvPageHeader.text = "${data.account.code} - Purchases"
                ledger.select("""
                idCompetition=${Competition.current.id} AND
                idAccount=${data.idAccount}
                """, "dateCreated")
                lvData.load(this, ledger, R.layout.template_competition_ledger)
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
            else -> {
                doNothing()
            }
        }
    }

    override fun whenItemClick(position: Int) {
        ledger.cursor = position
        if (!ledger.cancelled && !ledger.isLocked) {
            sendSignal(SignalCode.CHECKOUT_EDIT)
        }
    }

    override fun whenLongClick(position: Int) {
        whenItemClick(position)
    }

    override fun whenPopulate(view: View, position: Int) {
        ledger.cursor = position
        val tvTime = view.findViewById(R.id.tvTime) as TextView
        val tvDescription = view.findViewById(R.id.tvDescription) as TextView
        val tvAmount = view.findViewById(R.id.tvAmount) as TextView
        val tvPaid = view.findViewById(R.id.tvPaid) as TextView

        tvTime.setTextColor(if (ledger.cancelled) GREY else BLACK)
        tvDescription.setTextColor(if (ledger.cancelled) GREY else BLACK)
        tvAmount.setTextColor(if (ledger.cancelled) GREY else BLACK)
        tvPaid.setTextColor(if (ledger.cancelled) GREY else BLACK)

        tvTime.text = "${ledger.dateCreated.dayNameShort} ${ledger.dateCreated.timeText}"
        if (!ledger.type.oneOf(ITEM_REGISTRATION)) {
            tvDescription.text = "${ledger.quantity.absolute} x ${ledger.description}"
        } else {
            tvDescription.text = "${ledger.description}"
        }
        tvAmount.text = ledger.amount.money
        if (ledger.cheque != 0) {
            tvPaid.text = "Cheque"
        } else if (ledger.promised != 0) {
            tvPaid.text = "In Post"
        } else {
            tvPaid.text = "Cash"
        }


    }

}