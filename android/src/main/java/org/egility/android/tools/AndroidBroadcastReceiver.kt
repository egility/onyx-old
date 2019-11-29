/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.android.tools

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.WifiManager
import org.egility.library.general.debug

/**
 * Created by mbrickman on 29/03/16.
 */
object broadcastReceiver : BroadcastReceiver() {
    var onNetworkStateChange: (intent: Intent) -> Unit = {}
    var onSupplicantStateChange: (intent: Intent) -> Unit = {}
    var onScanResultsAvailable: (intent: Intent) -> Unit = {}
    var onScreenOff: (intent: Intent) -> Unit = {}
    var onScreenOn: (intent: Intent) -> Unit = {}
    var onBatteryChanged: (intent: Intent) -> Unit = {}

    fun register(context: Context) {
        val intentFilter = IntentFilter()
        intentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION)
        intentFilter.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION)
        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
        intentFilter.addAction(Intent.ACTION_SCREEN_OFF)
        intentFilter.addAction(Intent.ACTION_SCREEN_ON)
        intentFilter.addAction(Intent.ACTION_BATTERY_CHANGED)
        context.registerReceiver(broadcastReceiver, intentFilter)
    }

    override fun onReceive(context: Context?, _intent: Intent?) {
        val intent = _intent
        if (intent != null) {
            when (intent.action) {
                WifiManager.NETWORK_STATE_CHANGED_ACTION -> onNetworkStateChange(intent)
                WifiManager.SUPPLICANT_STATE_CHANGED_ACTION -> onSupplicantStateChange(intent)
                WifiManager.SCAN_RESULTS_AVAILABLE_ACTION -> onScanResultsAvailable(intent)
                Intent.ACTION_SCREEN_OFF-> onScreenOff(intent)
                Intent.ACTION_SCREEN_ON -> onScreenOn(intent)
                Intent.ACTION_BATTERY_CHANGED -> onBatteryChanged(intent)
            }
        }
    }
}

