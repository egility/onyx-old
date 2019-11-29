/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.granite.fragments

import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.AdapterView.OnItemClickListener
import android.widget.BaseAdapter
import android.widget.TextView
import kotlinx.android.synthetic.main.host_list.*
import org.egility.library.api.Api
import org.egility.library.general.*
import org.egility.library.general.heartbeats.logAccessPoint
import org.egility.library.general.heartbeats.resetAccessPoints
import org.egility.android.BaseFragment
import org.egility.android.tools.*
import org.egility.android.tools.NetworkObject.ssidToTag
import org.egility.granite.R


/**
 * Created by mbrickman on 21/10/15.
 */
class ListHostsFragment : BaseFragment(R.layout.host_list), OnItemClickListener {

    private val adapter = ControlUnitAdapter()

    override fun whenInitialize() {
        pulseRate = 10
    }

    override fun whenClick(view: View) {
        if (view.tag is Signal) {
            sendSignal(view.tag as Signal)
        } else {
            when (view) {
                btOK -> sendSignal(SignalCode.BACK)
                btKill -> sendSignal(SignalCode.KILL_SYSTEM)
            }
        }
    }

    var signals = HashMap<String, Int>()

    override fun whenSignal(signal: Signal) {
        when (signal.signalCode) {
            SignalCode.RESET_FRAGMENT -> {
                lvHosts.adapter = adapter
                lvHosts.onItemClickListener = this
            }
            SignalCode.CONTROL_UNITS_SCAN -> {
                resetAccessPoints()
                NetworkObject.scanForAccessPoints { acus ->
                    for (acu in acus) {
                        val tag = ssidToTag(acu.SSID)
                        logAccessPoint(tag, acu.signal, acu.level)
                    }
                    Api.updateHosts()
                    adapter.notifyDataSetChanged()
                }
            }
            SignalCode.PULSE -> {
                if (NetworkObject.isConnected) {
                    debug("ListHostsFragment", "isConnected")
                    sendSignal(SignalCode.CONTROL_UNITS_SCAN)
                } else {
                    debug("ListHostsFragment", "not Connected")
                    NetworkObject.connect {
                        sendSignal(SignalCode.CONTROL_UNITS_SCAN)
                    }
                }
                signal.consumed()
            }
            else -> {
                doNothing()
            }
        }
    }

    override fun onItemClick(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        sendSignal(SignalCode.CONTROL_UNIT_MENU, heartbeats[position])
    }
}

private class ControlUnitAdapter : BaseAdapter() {

    override fun getCount(): Int {
        debug("ControlUnitAdapter", "size is ${heartbeats.size}")
        return heartbeats.size
    }

    override fun getItem(position: Int): Any? {
        debug("ControlUnitAdapter", "getItem @ $position")
        return heartbeats[position]
    }

    override fun getItemId(position: Int): Long {
        debug("ControlUnitAdapter", "getItemId @ $position")
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View? {
        debug("ControlUnitAdapter", "getView @ $position")
        val view = convertView ?: androidApplication.inflater.inflate(R.layout.view_acu_status, parent, false)
        val host = getItem(position) as? AcuHeartbeat
        if (host != null) {
            val tvLabel = view.findViewById(R.id.tvLabel) as TextView
            val tvWiFi = view.findViewById(R.id.tvWiFi) as TextView
            val tvDisplay1 = view.findViewById(R.id.tvDisplay1) as TextView
            val tvDisplay2 = view.findViewById(R.id.tvDisplay2) as TextView
            val tvDisplay3 = view.findViewById(R.id.tvDisplay3) as TextView
            val display = (if (host.display.isEmpty()) "||" else host.display).split("|")
            val signal = host.apSignalStrength
            tvLabel.text = host.tag
            tvWiFi.text = if (signal > -1) "${host.apLevel} ($signal%)" else "No WiFi"
            tvDisplay1.text = display[0].padEnd(8, ' ').substring(0, 8) + ((cpuTime - host.lastAlive) / 1000).secondsToTime()
            tvDisplay2.text = display[1]
            tvDisplay3.text = display[2]

            tvLabel.setTextColor(Color.parseColor("#000000"))
            tvWiFi.setTextColor(Color.parseColor("#000000"))
            tvDisplay1.setTextColor(Color.parseColor("#000000"))
            tvDisplay2.setTextColor(Color.parseColor("#000000"))
            tvDisplay3.setTextColor(Color.parseColor("#000000"))
        }
        return view
    }

}
