/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.granite.activities

import kotlinx.android.synthetic.main.page_heat_test.*
import org.egility.library.dbobject.AgilityClass
import org.egility.library.dbobject.Device
import org.egility.library.dbobject.Ring
import org.egility.library.general.*
import org.egility.android.BaseActivity
import org.egility.android.tools.Signal
import org.egility.android.tools.SignalCode
import org.egility.granite.R
import java.util.*

/**
 * Created by mbrickman on 27/04/16.
 */
class HeatTest : BaseActivity(R.layout.page_heat_test) {

    var lastLog = nullDate
    var startTime = nullDate
    var textMonitor = ChangeMonitor("")

    private val ring: Ring
        get() = ringPartyData.ring


    private val agilityClass: AgilityClass
        get() = ring.agilityClass


    override fun whenInitialize() {
        startTime = now
        tvDevice.text = Device.thisTag
        tvInfo.text = "host=${Global.databaseHost}"
        pulseRate = 9999
        sendSignal(SignalCode.PULSE)
    }

    override fun whenSignal(signal: Signal) {
        when (signal.signalCode) {
            SignalCode.PULSE -> {
                tvTime.text = now.fullDateTimeText
                if (textMonitor.hasChanged(Date(now.time-startTime.time).addHours(-1).extendedTimeText)) {
                    tvDevice.text = "${Device.thisTag} (${textMonitor.value})"
                }
                if (now > lastLog.addSeconds(60)) {
                    lastLog = now
                    Device.thisDevice.lastSignOn = now
                    Device.thisDevice.post()
                }
            }
            else -> {
                super.whenSignal(signal)
            }
        }
    }


}