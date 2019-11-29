/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.android.tools

import android.R
import android.annotation.TargetApi
import android.app.AlertDialog
import android.content.DialogInterface
import android.os.Build
import android.os.Environment
import android.os.SystemClock
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import org.egility.library.dbobject.Competition
import org.egility.library.dbobject.Device
import org.egility.library.dbobject.ReportQueue
import org.egility.library.general.*
import java.io.File
import java.io.FileOutputStream
import java.io.PrintWriter
import java.util.*

/**
 * Created by mbrickman on 13/03/15.
 */
object AndroidServices : Services, PopUpListener, DialogInterface.OnClickListener {

    private val simpleDialog = 0
    private val panicDialog = 1

    private var logFileDate = nullDate
    private var _logFile: File? = null
    private var _stream: FileOutputStream? = null
    private var _writer: PrintWriter? = null

    val writer: PrintWriter
        get() {
            if (_writer == null || realToday != logFileDate) {
                logFileDate = realToday
                pruneFiles()
                val file = File(androidApplication.getExternalFilesDir(null), "${Device.thisDevice.tag}_${logFileDate.fileNameDate}.log")
                if (!file.exists()) {
                    file.createNewFile()
                }
                _logFile = file
                _stream = FileOutputStream(_logFile, true)
                _writer = PrintWriter(_stream)
            }
            return _writer ?: throw Wobbly("Unable to create log writer")
        }

    private fun pruneFiles() {
        try {
            val files = androidApplication.getExternalFilesDir(null).listFiles()
            for (file in files) {
                if (file.lastModified() < machineDate.addDays(-28).time) {
                    file.delete()
                }
            }
        } catch (e: Throwable) {
            doNothing()
        }
    }

    override val bootTime: Date
        get() = Date(System.currentTimeMillis() - SystemClock.elapsedRealtime())

    override fun panic(throwable: Throwable) {
        AndroidPanic.panic(throwable)
    }

    override fun log(message: String) {
        Log.d("log", message)
        try {
            if (Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED) {
                writer.println("${realNow.fullTimeText} $message")
                writer.flush()
                _stream?.flush()
            }
        } catch (e: Throwable) {
            doNothing()
        }
    }

    private fun setTextSize(view: View, textSize: Float) {
        if (view is ViewGroup) {
            for (i in 0..view.childCount - 1)
                setTextSize(view.getChildAt(i), textSize)
        }
        if (view is TextView) {
            view.textSize = textSize
        }
        if (view is Button) {
            view.textSize = textSize

            var params = view.getLayoutParams();
            params.height = ViewGroup.LayoutParams.WRAP_CONTENT
            view.setLayoutParams(params);
        }
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    override fun popUp(title: String, message: String) {
        val focusedActivity = androidApplication._focusedActivity
        if (focusedActivity != null) {
            val builder = AlertDialog.Builder(focusedActivity, AlertDialog.THEME_DEVICE_DEFAULT_DARK)
            builder.setTitle(title)
            builder.setMessage(message)
            builder.setPositiveButton("OK", null)
            builder.setCancelable(false)

            val dialog = builder.create()
            dialog.show()
            setTextSize(dialog.findViewById(R.id.content), 24f)
        }
    }

    override fun msgYesNo(title: String, message: String, body: (Boolean) -> Unit) {
        val focusedActivity = androidApplication._focusedActivity
        if (focusedActivity != null) {
            val builder = AlertDialog.Builder(focusedActivity, AlertDialog.THEME_DEVICE_DEFAULT_DARK)
            builder.setTitle(title)
            builder.setMessage(message)
            builder.setPositiveButton("Yes") { dialog, _ ->
                dialog.dismiss()
                body(true)
            }
            builder.setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
                body(false)
            }
            val dialog = builder.create()
            dialog.show()
            setTextSize(dialog.findViewById(R.id.content), 24f)
        }
    }

    override fun checkNetwork() {
        NetworkObject.checkNetwork()
    }

    override val acuHostname: String
        get() {
            val acu = NetworkObject.acu
            if (acu.startsWith("acu")) {
                val number = acu.drop(3).toIntDef(0)
                if (number > 0) return "172.16.$number.1"
//                if (number > 0) return "172.16.16.1"
            }
            return NetworkObject.gateway
        }
    
    override fun generateReport(reportRequest: Json, canSpool: Boolean): String {
        ReportQueue.spool(reportRequest)
        return "spooled"
    }

    override fun onClick(dialog: DialogInterface, which: Int) {
        val popUpListener = _popUpListener
        if (popUpListener != null) {
            when (which) {
                DialogInterface.BUTTON_POSITIVE -> popUpListener.onPopupClick(popUpTag, PopUpListener.BUTTON_POSITIVE)
                DialogInterface.BUTTON_NEGATIVE -> popUpListener.onPopupClick(popUpTag, PopUpListener.BUTTON_NEGATIVE)
                DialogInterface.BUTTON_NEUTRAL -> popUpListener.onPopupClick(popUpTag, PopUpListener.BUTTON_NEUTRAL)
            }
        }
    }

    override fun onPopupClick(tag: Int, button: Int) {
        if (tag == panicDialog) {
            //            resetApplication();
        } else if (tag == simpleDialog) {
            /* do nothing */
        }
    }

    var board: String = ""
    var brand: String = ""
    var device: String = ""
    var display: String = ""
    var fingerprint: String = ""
    var hardware: String = ""
    var host: String = ""
    var manufacturer: String = ""
    var model: String = ""
    var product: String = ""
    var serial: String = ""
    var user: String = ""
    var machine = ""
    private val registered = false
    var isT63 = false

    init {
        board = Build.BOARD
        brand = Build.BRAND
        device = Build.DEVICE
        display = Build.DISPLAY
        fingerprint = Build.FINGERPRINT
        hardware = Build.HARDWARE
        host = Build.HOST
        manufacturer = Build.MANUFACTURER
        model = Build.MODEL
        product = Build.PRODUCT
        serial = Build.SERIAL
        user = Build.USER
        isT63 = device.contains("T63")
    }

    fun initialize() {
        if (Global.services != this) {
            Global.services = this
        }
    }

    fun registerDevice() {
        if (!registered) {
            machine = brand + " " + device
            if (machine.eq("Boeye T62D") && serial.startsWith("T")) {
                machine = "Boeye T62D-HD"
            }
            Device.register(2, serial, brand + " " + device, "tab", getMacAddress("wlan0"), getIpAddress("wlan0"), 
                display, NetworkObject.acuTag, NetworkObject.signalStrength, power.charge, 
                idCompetition = Competition.current.id, panic = AndroidPanic.message)
        }
    }

    fun close() {
        Global.connection.close()
    }

    private var popUpTag = 0
    private var _popUpListener: PopUpListener? = null


}
