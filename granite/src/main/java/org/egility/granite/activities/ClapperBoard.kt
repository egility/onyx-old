/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.granite.activities

import kotlinx.android.synthetic.main.page_clapper_board.*
import org.egility.android.BaseActivity
import org.egility.android.tools.Signal
import org.egility.android.tools.SignalCode
import org.egility.granite.R
import org.egility.library.dbobject.AgilityClass
import org.egility.library.dbobject.Competition
import org.egility.library.dbobject.Ring
import org.egility.library.general.fullDateTimeText
import org.egility.library.general.now
import org.egility.library.general.ringPartyData

/**
 * Created by mbrickman on 27/04/16.
 */
class ClapperBoard : BaseActivity(R.layout.page_clapper_board) {

    private val ring: Ring
        get() = ringPartyData.ring


    private val agilityClass: AgilityClass
        get() = ring.agilityClass


    override fun whenInitialize() {
        tvShow.text = Competition.current.uniqueName
        tvRing.text = "Ring ${ring.number}"
        tvClass.text = "(${agilityClass.name})"
        pulseRate = 1
        sendSignal(SignalCode.PULSE)
    }

    override fun whenSignal(signal: Signal) {
        when (signal.signalCode) {
            SignalCode.PULSE -> {
                tvTime.text = now.fullDateTimeText
            }
            else -> {
                super.whenSignal(signal)
            }
        }
    }


}