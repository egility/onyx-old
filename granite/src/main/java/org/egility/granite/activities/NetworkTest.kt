/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.granite.activities

import org.egility.granite.fragments.Diagnostics
import org.egility.library.general.Global
import org.egility.library.general.NetworkCheck
import org.egility.android.BaseActivity
import org.egility.android.tools.Signal
import org.egility.android.tools.SignalCode
import org.egility.granite.R

/**
 * Created by mbrickman on 22/07/17.
 */
class NetworkTest : BaseActivity(R.layout.content_holder) {

    private lateinit var diagnostics : Diagnostics

    init {
        if (!dnr) {
            diagnostics = Diagnostics()
        }
    }

    override fun whenInitialize() {
        defaultFragmentContainerId = R.id.loContent
    }

    override fun whenSignal(signal: Signal) {
        when (signal.signalCode) {
            SignalCode.RESET -> {
                NetworkCheck.load(Global.services.acuHostname, brief = true)
                diagnostics.pageHeader = "Network Test"
                diagnostics.terminal = NetworkCheck.log
                loadTopFragment(diagnostics)
                signal.consumed()
            }
            SignalCode.BACK -> {
                back()
            }
            else -> super.whenSignal(signal)
        }
    }


}