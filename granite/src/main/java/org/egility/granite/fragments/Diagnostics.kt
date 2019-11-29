/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.granite.fragments

import android.text.method.ScrollingMovementMethod
import android.view.View
import kotlinx.android.synthetic.main.fragment_diagnostics.*
import org.egility.library.general.JsonNode
import org.egility.library.general.doNothing
import org.egility.android.BaseFragment
import org.egility.android.tools.Signal
import org.egility.android.tools.SignalCode
import org.egility.granite.R

/**
 * Created by mbrickman on 08/10/15.
 */
class Diagnostics : BaseFragment(R.layout.fragment_diagnostics) {

    var pageHeader = ""
    var terminal = ""
    val scrollingMovementMethod = ScrollingMovementMethod()

    override fun whenInitialize() {
        tvTerminal.text = terminal
        tvPageHeader.text = pageHeader
        tvTerminal.movementMethod = scrollingMovementMethod
        pulseRate = 0
    }

    override fun whenClick(view: View) {
        if (view.tag is Signal) {
            sendSignal(view.tag as Signal)
        } else {
            when (view) {
                btOK -> sendSignal(SignalCode.BACK)
            }
        }
    }

    override fun whenSignal(signal: Signal) {
        when (signal.signalCode) {
            SignalCode.PAGE_DOWN -> {
                svTerminal.scrollBy(0, svTerminal.height)
                signal.consumed()
            }
            SignalCode.PAGE_UP -> {
                svTerminal.scrollBy(0, -svTerminal.height)
                signal.consumed()
            }
            SignalCode.RESET_FRAGMENT -> {
                signal.consumed()
            }
            else -> {
                doNothing()
            }
        }
    }


}


