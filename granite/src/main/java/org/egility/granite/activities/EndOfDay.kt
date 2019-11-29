/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.granite.activities

import android.view.View
import org.egility.granite.fragments.EndOfDayCashFragment
import org.egility.granite.fragments.EndOfDayChequeFragment
import org.egility.granite.fragments.EndOfDaySummaryFragment
import org.egility.library.dbobject.Competition
import org.egility.library.dbobject.CompetitionDay
import org.egility.library.general.Global
import org.egility.android.BaseActivity
import org.egility.android.tools.Signal
import org.egility.android.tools.SignalCode
import org.egility.granite.R

/**
 * Created by mbrickman on 27/03/16.
 */

class EndOfDay : BaseActivity(R.layout.content_holder) {
    
    companion object {
        var day = CompetitionDay()
    }

    private lateinit var endOfDayCashFragment: EndOfDayCashFragment
    private lateinit var endOfDayChequeFragment: EndOfDayChequeFragment
    private lateinit var endOfDaySummaryFragment: EndOfDaySummaryFragment

    init {
        if (!dnr) {
            endOfDayCashFragment = EndOfDayCashFragment()
            endOfDayChequeFragment = EndOfDayChequeFragment()
            endOfDaySummaryFragment = EndOfDaySummaryFragment()
        }
    }

    override fun whenInitialize() {
        day.seek(Competition.current.id, Global.endOfDayDate)
    }

    override fun whenClick(view: View) {
        when (view) {
        }
        super.whenClick(view)
    }

    override fun whenSignal(signal: Signal) {
        when (signal.signalCode) {
            SignalCode.RESET -> {
                defaultFragmentContainerId = R.id.loContent
                if (day.locked) {
                    loadTopFragment(endOfDaySummaryFragment)
                } else {
                    loadTopFragment(endOfDayCashFragment)
                }
                signal.consumed()
            }
            SignalCode.END_OF_DAY_CASH_DONE -> {
                day.post()
                loadTopFragment(endOfDayChequeFragment)
                signal.consumed()
            }
            SignalCode.END_OF_DAY_CHEQUE_DONE -> {
                day.post()
                loadTopFragment(endOfDaySummaryFragment)
                signal.consumed()
            }
            SignalCode.END_OF_DAY_CHEQUE_BACK -> {
                loadTopFragment(endOfDayCashFragment)
            }
            SignalCode.END_OF_DAY_SUMMARY_BACK -> {
                if (day.locked) {
                    sendSignal(SignalCode.BACK)
                } else {
                    loadTopFragment(endOfDayChequeFragment)
                }
            }
            else -> {
                super.whenSignal(signal)
            }
        }

    }


}