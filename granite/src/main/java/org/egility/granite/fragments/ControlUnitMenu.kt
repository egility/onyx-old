/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.granite.fragments

import android.view.View
import kotlinx.android.synthetic.main.fragment_control_unit_menu.*
import org.egility.library.api.Api
import org.egility.library.general.*
import org.egility.android.BaseActivity
import org.egility.android.BaseFragment
import org.egility.android.tools.Signal
import org.egility.android.tools.SignalCode
import org.egility.android.tools.doBackground
import org.egility.granite.R

/**
 * Created by mbrickman on 08/10/15.
 */
class ControlUnitMenu : BaseFragment(R.layout.fragment_control_unit_menu) {

    private var _acu: JsonNode? = null
    var hostname = ""
    var hostAddress = ""
    var loading = false
    var optionsText = ""

    override fun whenInitialize() {
        tvPageHeader.text = "Virtual Control Unit - $hostname"
        tvOptionsLabel.text = "Options for $hostname"
        pulseRate=10
        optionsText = ""
    }

    override fun whenClick(view: View) {
        if (view.tag is Signal) {
            sendSignal(view.tag as Signal)
        } else {
            when (view) {
                btOK -> sendSignal(SignalCode.BACK)
                btDebug-> sendSignal(SignalCode.CONTROL_UNIT_DEBUG, _acu)

            }
        }
    }

    fun doCommand(command: String) {
        doBackground({
            SshClient.execute(hostAddress, command)
        }, {
            // tvTerminal.text = result
        })
    }

    override fun whenSignal(signal: Signal) {
        when (signal.signalCode) {
            SignalCode.RESET_FRAGMENT -> {
                loadData()
                signal.consumed()
            }
            SignalCode.PULSE -> {
                loadData()
                signal.consumed()
            }
            SignalCode.ACU_OPTION -> {
                val name = signal._payload as String?
                if (name != null) {
                    Api.option(name)
                }
                signal.consumed()
            }
            else -> {
                doNothing()
            }
        }
    }

    private fun loadData() {
        loading = true
        try {
            _acu = Api.Diagnostics(hostAddress)
            val acu = _acu
            if (acu != null) {
                tvDisplay1.text = acu["display"][0].asString.pad(8) + acu["system.time"].asString
                tvDisplay2.text = acu["display"][1].asString
                tvDisplay3.text = acu["display"][2].asString
                val newOptionsText = acu["options"].toJson()
                if (newOptionsText != optionsText) {
                    loMenu.removeAllViews()
                    for (option in acu["options"]) {
                        addMenuButton(loMenu, option.asString, SignalCode.ACU_OPTION, option.asString)
                    }
                    optionsText = newOptionsText
                }
            }
            tvNoOptions.visibility = if (loMenu.childCount == 0) View.VISIBLE else View.GONE
        } finally {
            loading = false
        }
    }

}


