/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.granite.activities

import android.content.Intent
import org.egility.granite.fragments.ControlUnitMenu
import org.egility.granite.fragments.Diagnostics
import org.egility.granite.fragments.ListHostsFragment
import org.egility.library.api.Api
import org.egility.library.general.JsonNode
import org.egility.library.general.PIN_SYSTEM_MANAGER
import org.egility.library.general.whenYes
import org.egility.android.BaseActivity
import org.egility.android.tools.Signal
import org.egility.android.tools.SignalCode
import org.egility.granite.R

/**
 * Created by mbrickman on 10/08/16.
 */
class ControlUnitServices : BaseActivity(R.layout.content_holder) {

    private lateinit var listHostsFragment: ListHostsFragment
    private lateinit var controlUnitMenu: ControlUnitMenu
    private lateinit var diagnostics: Diagnostics

    init {
        if (!dnr) {
            listHostsFragment = ListHostsFragment()
            controlUnitMenu = ControlUnitMenu()
            diagnostics = Diagnostics()
        }
    }

    override fun whenInitialize() {
        defaultFragmentContainerId = R.id.loContent
    }

    override fun whenSignal(signal: Signal) {
        when (signal.signalCode) {
            SignalCode.RESET -> {
                loadTopFragment(listHostsFragment)
                signal.consumed()
            }
            SignalCode.BACK -> {
                back()
            }
            SignalCode.CONTROL_UNIT_MENU -> {
                val host = signal._payload as JsonNode?
                if (host != null) {
                    controlUnitMenu.hostname = host["hostname"].asString
                    controlUnitMenu.hostAddress = host["network"]["address"].asString
                    loadFragment(controlUnitMenu)
                }
                signal.consumed()
            }
            SignalCode.CONTROL_UNIT_DEBUG -> {
                val host = signal._payload as JsonNode?
                if (host != null) {
                    diagnostics.pageHeader = "${host["hostname"].asString} - Diagnostics"
                    diagnostics.terminal = host.toJson(pretty = true)
                    loadFragment(diagnostics)
                }
                signal.consumed()
            }
            SignalCode.CHECK_PIN -> {
                val pinType = signal._payload as Int?
                if (pinType != null) {
                    val intent = Intent(this, EnterPin::class.java)
                    intent.putExtra("pinType", pinType)
                    startActivityForResult(intent, pinType)
                }
            }
            SignalCode.KILL_SYSTEM -> {
                whenYes("Question", "Do you really want to KILL the system?") {
                    sendSignal(SignalCode.CHECK_PIN, PIN_SYSTEM_MANAGER)
                }
            }
            SignalCode.KILL_AUTHORIZED -> {
                Api.killServers()
            }

            else -> super.whenSignal(signal)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == RESULT_OK) {
            when (requestCode) {
                PIN_SYSTEM_MANAGER -> {
                    sendSignal(SignalCode.KILL_AUTHORIZED)
                }
            }
        }
    }

}