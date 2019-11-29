/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.granite.fragments

import android.view.View
import kotlinx.android.synthetic.main.fragment_end_of_day_summary.*
import org.egility.library.api.Api
import org.egility.library.dbobject.Competition
import org.egility.library.dbobject.CompetitionDay
import org.egility.library.general.doNothing
import org.egility.library.general.money
import org.egility.library.general.today
import org.egility.library.general.whenYes
import org.egility.android.BaseFragment
import org.egility.android.tools.AndroidUtils
import org.egility.android.tools.Signal
import org.egility.android.tools.SignalCode
import org.egility.android.tools.doHourglass
import org.egility.granite.R
import org.egility.granite.activities.EndOfDay

/**
 * Created by mbrickman on 27/03/16.
 */

class EndOfDaySummaryFragment : BaseFragment(R.layout.fragment_end_of_day_summary) {

    val day = EndOfDay.day

    override fun whenInitialize() {

        if (day.locked) {
            btAccept.visibility = View.GONE
            btFinished.visibility = View.GONE
            btPrint.text = "Re-Print"
        } else if (day.date > today) {
            btAccept.visibility = View.GONE
            btFinished.visibility = View.VISIBLE
            btPrint.text = "Trail Print"
        } else {
            btAccept.visibility = View.VISIBLE
            btFinished.visibility = View.VISIBLE
            btPrint.text = "Trail Print"
        }

        tvPageHeader.text = Competition.current.uniqueName + " - End of Day (Summary)"

        day.loadData()

        tvLateCash.text = day.lateEntryCash.money
        tvLateCheques.text = day.lateEntryCheque.money
        tvLateTotal.text = (day.lateEntryCash + day.lateEntryCheque).money

        tvSpecialCash.text = day.specialCash.money
        tvSpecialCheques.text = day.specialCheque.money
        tvSpecialTotal.text = (day.specialCash + day.specialCheque).money

        tvAccountCash.text = day.registrationsCash.money
        tvAccountCheques.text = day.registrationsCheque.money
        tvAccountTotal.text = (day.registrationsCash + day.registrationsCheque).money

        tvOtherCash.text = day.cashOther.money

        tvTotalCash.text = day.calculatedCash.money
        tvTotalCheques.text = day.calculatedCheque.money
        tvTotalTotal.text = (day.calculatedCash + day.calculatedCheque).money

        tvCashInHand.text = day.totalCash.money
        tvRemoved.text = day.cashRemoved.money
        tvFloat.text = day.float.money

        tvDifferenceCash.text = day.differenceCash.money

        AndroidUtils.goneIf(day.differenceCheque == 0, tvDifferenceCheque)

        tvLateEntriesSold.text = day.paidCreditsCash.toString()
        tvComplimentaryRuns.text = day.netComplimentary.toString()
        tvSpecialEntriesCash.text = day.specialEntriesCash.toString()


        tvComplimentaryRunsLabel.text = "Complimentary"

        tvLateEntryCut.text = day.lateEntryCutCash.money
        tvComplimentaryCut.text = day.complimentaryCut.money
        tvSpecialCut.text = day.specialCutCash.money
        tvAccountCut.text = day.registrationsCash.money
        tvUKACut.text = day.totalCut.money
        tvCashRetained.text = (day.totalCash + day.cashRemoved - day.totalCut).money

        val complimentaryRuns = day.competition.complimentaryRuns
        val complimentaryAllowance = day.competition.complimentaryAllowance
        val netComplimentary = complimentaryRuns - complimentaryAllowance

        if (netComplimentary > 0) {
            tvComplimentaryRunsLabel.text = "Complimentary ($complimentaryRuns-$complimentaryAllowance)"

        } else {
            tvComplimentaryRunsLabel.text = "Complimentary"
        }

    }


    override fun whenSignal(signal: Signal) {
        when (signal.signalCode) {
            SignalCode.END_OF_DAY_ACCEPT -> {
                if (day.differenceCheque != 0) {
                    whenYes("Question", "Are you sure you want to accept end of day with NOT ALL CHEQUES PRESENT?") {
                        whenYes("Question", "Confirm that you accept the end of day figures?") {
                            sendSignal(SignalCode.END_OF_DAY_PRINT, true)
                        }
                    }
                } else {
                    whenYes("Question", "Confirm that you accept the end of day figures?") {
                        sendSignal(SignalCode.END_OF_DAY_PRINT, true)
                    }
                }
            }
            SignalCode.END_OF_DAY_PRINT -> {
                val _finalize = signal._payload as Boolean?
                if (_finalize != null) {
                    doHourglass(activity, "Processing End of Day",
                        { Api.endOfDay(day.idCompetition, day.date, _finalize) },
                        {
                            sendSignal(SignalCode.BACK)
                        }
                    )
                }

            }
            else -> {
                doNothing()
            }
        }
    }


    override fun whenClick(view: View) {
        when (view) {
            btBack -> sendSignal(SignalCode.END_OF_DAY_SUMMARY_BACK)
            btFinished -> sendSignal(SignalCode.BACK)
            btPrint -> sendSignal(SignalCode.END_OF_DAY_PRINT, false)
            btAccept -> sendSignal(SignalCode.END_OF_DAY_ACCEPT)
        }
    }


}