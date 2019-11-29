/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.android.tools

import android.annotation.TargetApi
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.graphics.Color
import android.os.Build
import android.os.Environment
import android.os.Process
import android.provider.Settings
import android.text.InputType
import android.util.TypedValue
import android.view.View
import android.widget.EditText
import android.widget.TextView
import org.egility.library.database.DbJdbcConnection
import org.egility.library.dbobject.Device
import org.egility.library.dbobject.TabletLog
import org.egility.library.general.*


/**
 * Created by mbrickman on 02/01/15.
 */

var BLACK = Color.parseColor("#000000")
var GREY = Color.parseColor("#777777")
var DARKGREY = Color.parseColor("#555555")
var GREY1 = Color.parseColor("#ff888888")
var GREY2 = Color.parseColor("#00999999")
var GREY3 = Color.parseColor("#777777")


object AndroidUtils {
    var alertDialogStyle = AlertDialog.THEME_TRADITIONAL
    var androidTheme = -1
    val isFire = Build.MANUFACTURER.contains("Amazon") || Build.MANUFACTURER.contains("LENOVO")
    var tabletLogThread: TabletLogThread? = null

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    fun popUp(context: Context, title: String, message: String, listener: DialogInterface.OnClickListener) {
        val alertDialog = AlertDialog.Builder(context, alertDialogStyle)
        alertDialog.setTitle(title)
        alertDialog.setMessage(message)
        alertDialog.setNeutralButton("OK", listener)
        alertDialog.setCancelable(false)
        alertDialog.show()
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    fun exceptionPopUp(context: Context, title: String, message: String, listener: DialogInterface.OnClickListener) {
        val alertDialog = AlertDialog.Builder(context, alertDialogStyle)
        alertDialog.setTitle(title)
        alertDialog.setMessage(message)
        alertDialog.setNegativeButton("OK", listener)
        alertDialog.setCancelable(false)
        alertDialog.show()
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    fun hideSoftKeyboard(editText: EditText) {
        editText.inputType = InputType.TYPE_NULL
        editText.setRawInputType(InputType.TYPE_CLASS_TEXT)
        editText.setTextIsSelectable(true)
    }

    fun dpToPx(context: Context, dp: Int): Int {
        val r = context.resources
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp.toFloat(), r.displayMetrics).toInt()

    }

    fun invisibleIf(condition: Boolean, view: View) {
        view.visibility = if (condition) View.INVISIBLE else View.VISIBLE
    }

    fun goneIf(condition: Boolean, view: View) {
        view.visibility = if (condition) View.GONE else View.VISIBLE
    }

    fun disableIf(condition: Boolean, view: View) {
        view.isEnabled = !condition
    }

    fun goneIfNoText(view: TextView) {
        view.visibility = if (view.text.isEmpty()) View.GONE else View.VISIBLE
    }

    fun setBrightness(brightness: Int) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            Settings.System.putInt(_application?.contentResolver, Settings.System.SCREEN_BRIGHTNESS, brightness)
        }
    }

    fun getBrightness(): Int {
        return Settings.System.getInt(_application?.contentResolver, Settings.System.SCREEN_BRIGHTNESS)
    }

    fun getBatteryText(): String {
        if (power.isCharging && Device.thisTag != "tab002") {
            return "${power.charge}% (Charging)"
        } else {
            return "${power.charge}%"
        }
    }

    fun kill() {
        Process.killProcess(Process.myPid());
        System.exit(10);
    }

    fun isExternalStorageAvailable(): Boolean {
        val state = Environment.getExternalStorageState()
        return Environment.MEDIA_MOUNTED.equals(state) || Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)
    }
    
    fun startTabletLog() {
        if (tabletLogThread == null) {
            tabletLogThread = TabletLogThread()
            tabletLogThread?.start()
        }
    }
}

class TabletLogThread() : Thread("TabletLog") {
    
    val writeInterval = 600000
    val writeLoop = 100

    val sleepTime = (writeInterval / writeLoop).toLong()

    override fun run() {
        var signalTotal = 0
        var signalCount = 0
        while (true) {
            try {
                if (NetworkObject.isConnected) {
                    signalTotal += NetworkObject.signalStrength
                    signalCount ++
                    if (signalCount>=writeLoop) {
                        val battery = power.charge
                        val connection = DbJdbcConnection(SandstoneMaster.builder)
                        val tabletLog = TabletLog(_connection = connection)
                        val averageSignal = (signalTotal +( signalCount / 2))/signalCount
                        signalTotal = 0
                        signalCount = 0
                        tabletLog.log(Device.thisDevice, 1, battery, averageSignal, Global.activityName, power.voltage, power.healthText)
                        Device(connection).seek(Device.thisDevice.id) {
                            signal = averageSignal
                            this.battery = battery
                            task = Global.activityName
                            dateModified = realNow
                            post()
                        }
//                        dbExecute("UPDATE device SET `signal`=$averageSignal, `battery`=$battery, `dateModified`=${realNow.sqlDateTime} WHERE `idDevice`=${Device.thisDevice.id}", connection)
                    }
                } else {
                    signalTotal = 0
                    signalCount = 0
                }
                sleep(sleepTime)
            } catch (e: Throwable) {
                debug("logThread", "error: ${e.message}")
                sleep(sleepTime)
            }
        }
    }
}