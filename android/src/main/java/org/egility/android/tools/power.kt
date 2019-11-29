/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.android.tools

import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import org.egility.library.general.debug

/**
 * Created by mbrickman on 22/06/16.
 */

object power {

    var health = -1
    var level = -1
    var plugged = -1
    var present = false
    var scale = -1
    var status = -1
    var technology = ""
    var temperature = -1
    var voltage = -1

    val isCharging: Boolean
        get() = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL

    val charge: Int
        get() {
            if (scale == 100) {
                return level
            } else {
                return (level.toFloat() / scale.toFloat() * 100.0 + 0.5).toInt()
            }
        }

    val healthText: String
        get() = when (health) {
            BatteryManager.BATTERY_HEALTH_COLD -> "cold"
            BatteryManager.BATTERY_HEALTH_DEAD -> "dead"
            BatteryManager.BATTERY_HEALTH_GOOD -> "good"
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> "overheat"
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "over"
            BatteryManager.BATTERY_HEALTH_UNKNOWN -> "unknown"
            BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> "fail"
            else -> ""
        }


    init {
        getInitialBatteryState()
        broadcastReceiver.onBatteryChanged = { intent -> whenBatteryChanged(intent) }
        broadcastReceiver.onScreenOff = { intent -> whenScreenOff(intent) }
        broadcastReceiver.onScreenOn = { intent -> whenScreenOn(intent) }
    }

    fun getInitialBatteryState() {
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val intent = _application?.registerReceiver(null, filter)
        if (intent != null) {
            whenBatteryChanged(intent)
        }
    }

    fun whenBatteryChanged(intent: Intent) {
        health = intent.getIntExtra(BatteryManager.EXTRA_HEALTH, -1)
        level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)
        present = intent.getBooleanExtra(BatteryManager.EXTRA_PRESENT, false)
        scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        technology = intent.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY) ?: ""
        temperature = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1)
        voltage = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1)

        debug("power", "Health: $health, level: $level, plugged: $plugged, present: $present, scale=$scale, status=$status, technology=$technology, temperature=$temperature, voltage=$voltage")
        androidApplication.sendSignal(SignalCode.BATTERY, false)
    }

    fun whenScreenOff(intent: Intent) {
        debug("power", "Screen Off -----------------------------------------")

        /*
        val alarmManager = androidApplication.getSystemService(Context.ALARM_SERVICE) as AlarmManager;
        val ExistingIntent = intent;
        val pi = PendingIntent.getActivity(androidApplication, 0, ExistingIntent, 0) as PendingIntent;
        alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, 10, pi);
        */

    }

    fun whenScreenOn(intent: Intent) {
        debug("power", "Screen On ++++++++++++++++++++++++++++++++++++++++++")
    }

}