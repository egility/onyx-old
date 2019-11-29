/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.granite.fragments

import android.text.Editable
import android.view.View
import android.widget.TextView
import kotlinx.android.synthetic.main.fragment_end_of_day_cash.*
import org.egility.library.dbobject.Competition
import org.egility.library.dbobject.CompetitionDay
import org.egility.library.general.*
import org.egility.android.BaseFragment
import org.egility.android.tools.*
import org.egility.granite.R.layout.fragment_end_of_day_cash
import org.egility.granite.activities.EndOfDay

/**
 * Created by mbrickman on 17/08/16.
 */
class EndOfDayCashFragment : BaseFragment(fragment_end_of_day_cash), AndroidOnTextChange {

    val day = EndOfDay.day

    override fun whenInitialize() {
        tvPageHeader.text = Competition.current.uniqueName + " - End of Day (Cash)"

        doUpdate()

        edCash1.addTextChangedListener(AndroidTextWatcher(edCash1, this))
        edCash2.addTextChangedListener(AndroidTextWatcher(edCash2, this))
        edCash5.addTextChangedListener(AndroidTextWatcher(edCash5, this))
        edCash10.addTextChangedListener(AndroidTextWatcher(edCash10, this))
        edCash20.addTextChangedListener(AndroidTextWatcher(edCash20, this))
        edCash50.addTextChangedListener(AndroidTextWatcher(edCash50, this))
        edCash100.addTextChangedListener(AndroidTextWatcher(edCash100, this))
        edCash200.addTextChangedListener(AndroidTextWatcher(edCash200, this))
        edCash500.addTextChangedListener(AndroidTextWatcher(edCash500, this))
        edCash1000.addTextChangedListener(AndroidTextWatcher(edCash1000, this))
        edCash2000.addTextChangedListener(AndroidTextWatcher(edCash2000, this))
        edCash5000.addTextChangedListener(AndroidTextWatcher(edCash5000, this))
        edFloat.addTextChangedListener(AndroidTextWatcher(edFloat, this))
        edCashOther.addTextChangedListener(AndroidTextWatcher(edCashOther, this))
        edCashRemoved.addTextChangedListener(AndroidTextWatcher(edCashRemoved, this))
    }

    fun doUpdate() {
        edCash1.setText(day.cash1.toStringBlankZero())
        edCash2.setText(day.cash2.toStringBlankZero())
        edCash5.setText(day.cash5.toStringBlankZero())
        edCash10.setText(day.cash10.toStringBlankZero())
        edCash20.setText(day.cash20.toStringBlankZero())
        edCash50.setText(day.cash50.toStringBlankZero())
        edCash100.setText(day.cash100.toStringBlankZero())
        edCash200.setText(day.cash200.toStringBlankZero())
        edCash500.setText(day.cash500.toStringBlankZero())
        edCash1000.setText(day.cash1000.toStringBlankZero())
        edCash2000.setText(day.cash2000.toStringBlankZero())
        edCash5000.setText(day.cash5000.toStringBlankZero())
        edFloat.setText(day.float.toMoneyBlankZero())
        edCashOther.setText(day.cashOther.toMoneyBlankZero())
        edCashRemoved.setText(day.cashRemoved.toMoneyBlankZero())

        doTotals()

    }

    fun doTotals() {
        day.calculateTotalCash()
        tvCash1.text = (day.cash1 * 1).money
        tvCash2.text = (day.cash2 * 2).money
        tvCash5.text = (day.cash5 * 5).money
        tvCash10.text = (day.cash10 * 10).money
        tvCash20.text = (day.cash20 * 20).money
        tvCash50.text = (day.cash50 * 50).money
        tvCash100.text = (day.cash100 * 100).money
        tvCash200.text = (day.cash200 * 200).money
        tvCash500.text = (day.cash500 * 500).money
        tvCash1000.text = (day.cash1000 * 1000).money
        tvCash2000.text = (day.cash2000 * 2000).money
        tvCash5000.text = (day.cash5000 * 5000).money
        tvTotalCash.text = day.totalCash.money
        btCashRemoved.text = "Add ${day.totalCash.money} to Cash Removed"
        AndroidUtils.invisibleIf(day.totalCash == 0, btCashRemoved)
    }

    override fun onTextChange(view: TextView, editable: Editable) {
        when (view) {
            edCash1 -> {
                day.cash1 = editable.toString().toIntDef(0)
            }
            edCash2 -> {
                day.cash2 = editable.toString().toIntDef(0)
            }
            edCash5 -> {
                day.cash5 = editable.toString().toIntDef(0)
            }
            edCash10 -> {
                day.cash10 = editable.toString().toIntDef(0)
            }
            edCash20 -> {
                day.cash20 = editable.toString().toIntDef(0)
            }
            edCash50 -> {
                day.cash50 = editable.toString().toIntDef(0)
            }
            edCash100 -> {
                day.cash100 = editable.toString().toIntDef(0)
            }
            edCash200 -> {
                day.cash200 = editable.toString().toIntDef(0)
            }
            edCash500 -> {
                day.cash500 = editable.toString().toIntDef(0)
            }
            edCash1000 -> {
                day.cash1000 = editable.toString().toIntDef(0)
            }
            edCash2000 -> {
                day.cash2000 = editable.toString().toIntDef(0)
            }
            edCash5000 -> {
                day.cash5000 = editable.toString().toIntDef(0)
            }
            edCash5000 -> {
                day.cash5000 = editable.toString().toIntDef(0)
            }
            edFloat -> {
                day.float = editable.toString().toMoneyDef(0)
            }
            edCashOther -> {
                day.cashOther = editable.toString().toMoneyDef(0)
            }
            edCashRemoved -> {
                day.cashRemoved = editable.toString().toMoneyDef(0)
            }
        }
        doTotals()
    }


    override fun whenClick(view: View) {
        when (view) {
            btNext -> sendSignal(SignalCode.END_OF_DAY_CASH_DONE)
            btBack -> sendSignal(SignalCode.BACK)
            btCashRemoved -> sendSignal(SignalCode.CASH_REMOVED)
        }
    }

    override fun whenSignal(signal: Signal) {
        when (signal.signalCode) {
            SignalCode.CASH_REMOVED -> {
                whenYes("Question", "Are you sure you want to remove ${day.totalCash.money} for safe keeping?") {
                    day.cashRemoved += day.totalCash
                    day.cash1 = 0
                    day.cash2 = 0
                    day.cash5 = 0
                    day.cash10 = 0
                    day.cash20 = 0
                    day.cash50 = 0
                    day.cash100 = 0
                    day.cash200 = 0
                    day.cash500 = 0
                    day.cash1000 = 0
                    day.cash2000 = 0
                    day.cash5000 = 0
                    doUpdate()
                    day.post()
                }
                signal.consumed()
            }
            else -> {
                doNothing()
            }
        }
    }
}