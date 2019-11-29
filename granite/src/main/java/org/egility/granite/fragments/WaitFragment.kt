/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.granite.fragments

import android.view.View
import kotlinx.android.synthetic.main.fragment_wait.*
import org.egility.library.general.ringPartyData
import org.egility.library.general.SCORE_DELETE
import org.egility.library.general.doNothing
import org.egility.android.BaseFragment
import org.egility.android.tools.Signal
import org.egility.android.tools.SignalCode
import org.egility.granite.R

class WaitFragment : BaseFragment(R.layout.fragment_wait) {

    override fun whenClick(view: View) {
        if (view === btOK) {
            sendSignal(SignalCode.RUN_COMPLETE)
        } else if (view === btBack) {
            sendSignal(SignalCode.BACK)
        }
    }


    override fun whenSignal(signal: Signal) {
        when (signal.signalCode) {
            SignalCode.BACK -> {
                ringPartyData.entry.addScoreCode(SCORE_DELETE)
                sendSignal(SignalCode.UPDATE_STATUS)
            }
            else -> {
                doNothing()
            }
        }
    }

}
