/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.granite.activities

import android.content.Intent
import android.view.View
import kotlinx.android.synthetic.main.home.*
import kotlinx.android.synthetic.main.home.loCentre
import kotlinx.android.synthetic.main.home.loMenu
import kotlinx.android.synthetic.main.home.tvPageHeader
import kotlinx.android.synthetic.main.home.tvSubTitle
import org.egility.android.BaseActivity
import org.egility.android.tools.*
import org.egility.granite.R
import org.egility.granite.utils.TabletInfo
import org.egility.granite.utils.updateApp
import org.egility.granite.utils.updateAppfromCard
import org.egility.library.dbobject.*
import org.egility.library.general.*
import java.util.*

/**
 * Created by mbrickman on 26/06/15.
 */
class Home : BaseActivity(R.layout.home, true) {
    
    enum class DisplayMode{NORMAL, MENU, PROGRESS}

    var displayMode: DisplayMode = DisplayMode.NORMAL
        set(value) {
            field = value
            AndroidUtils.goneIf(displayMode!=DisplayMode.PROGRESS, loWait)
            AndroidUtils.goneIf(displayMode!=DisplayMode.NORMAL, btOpenMenu)
            AndroidUtils.goneIf(displayMode!=DisplayMode.MENU, loCentre)
            AndroidUtils.goneIf(displayMode==DisplayMode.MENU, hrTop)
            AndroidUtils.goneIf(displayMode==DisplayMode.MENU, hrBottom)
            AndroidUtils.goneIf(displayMode==DisplayMode.MENU, tvUsage)
        }
    
    private var brightness = 0
    private var tabletWasAssigned = false

    private var stack = Stack<String>()

    override fun whenInitialize() {
        
        AndroidUtils.startTabletLog()
        ssid = "sandstone"
        Global.initialized = true
        pulseRate = 30
        TabletInfo.readConfig(this)
        Global.deviceType = DeviceType.TABLET

        ivLogo.setOnLongClickListener {
            if (isPatternMatch("LL")) {
                    sendSignal(SignalCode.SELECT_MENU, "control")
                }
            true
        }

        ivLogo.setOnClickListener {
            keyPattern += "L"
        }

        displayMode = DisplayMode.NORMAL
    }

    override fun whenResumeFromPause() {
        sendSignal(SignalCode.RESET)
    }

    override fun whenFocus() {
        if (AndroidPanic.hasPanic && !AndroidPanic.userInformed) {
            AndroidPanic.userInformed = true
            Global.services.popUp("There has been a problem", "E-gility has encountered an issue and re-set itself")
        }
    }

    override fun whenClick(view: View) {
        if (view.tag is Signal) {
            sendSignal(view.tag as Signal)
        } else {
            when (view) {
                btOpenMenu -> if (TabletInfo.assigned) {
                    sendSignal(SignalCode.SCAN_ACUS)
                } else {
                    sendSignal(SignalCode.CONNECT_NEAREST)
                }
                else -> super.whenClick(view)
            }
        }
    }

    fun updateTablet() {
        displayMode = DisplayMode.PROGRESS
        tvWaiting.text = "Waiting for network..."
        NetworkObject.onProgressListener = { message -> sendSignal(SignalCode.NETWORK_PROGRESS, message) }
        NetworkObject.connect { errorMessage ->
            NetworkObject.onProgressListener = null
            if (errorMessage.isEmpty()) {
                updateApp()
            } else {
                tvWaiting.text = ""
                tvError.text = errorMessage
                sendSignal(SignalCode.RESET)
            }
        }

    }

    override fun whenSignal(signal: Signal) {
        when (signal.signalCode) {
            SignalCode.RESET -> {
                NetworkObject.disconnect()

                if (!AndroidUtils.isFire) AndroidUtils.setBrightness(brightness)

                Global.reset()
                resetControl()
                Competition.reset()
                Device.reset()
                ringPartyData.reset()
                sendSignal(SignalCode.UPDATE_SCREEN)
                displayMode = DisplayMode.NORMAL
                tvWaiting.text = ""
                Global.databaseHost = ""

                signal.consumed()
            }
            SignalCode.UPDATE_SCREEN -> {
                if (TabletInfo.assigned) {
                    tvPageHeader.text = "Version: ${Global.version}"
                    tvUsage.text = TabletInfo.activityText
                } else {
                    tvPageHeader.text = "Version: ${Global.version}"
                    tvUsage.text = "Unassigned Tablet"
                }

                /*
                val displayMetrics = DisplayMetrics()
                windowManager.defaultDisplay.getMetrics(displayMetrics)
                val y = displayMetrics.heightPixels
                val x = displayMetrics.widthPixels
                val dpi = displayMetrics.densityDpi
                val xdpi = displayMetrics.xdpi
                val ydpi = displayMetrics.ydpi
                tvPageHeader.text="$x x $y ($dpi dpi): $xdpi x $ydpi"
                */



                tabletWasAssigned = TabletInfo.assigned
            }
            SignalCode.BACK, SignalCode.PAGE_DOWN, SignalCode.PAGE_UP -> {
                if (signal.signalCode==SignalCode.BACK && displayMode==DisplayMode.MENU) {
                    clearPattern()
                    sendSignal(SignalCode.EXIT_MENU)
                } else {
                    when {
                        isPatternMatch("bbb") -> {
                            sendSignal(SignalCode.TABLET_RELOAD)
                            signal.consumed()
                        }
                        isPatternMatch("udud") -> {
                            sendSignal(SignalCode.SELECT_MENU, "control")
                            signal.consumed()
                        }
                    }
                }
            }
            SignalCode.DEVICE_SETUP -> {
                val dialogIntent = Intent(android.provider.Settings.ACTION_SETTINGS)
                dialogIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(dialogIntent)
                signal.consumed()
            }
            SignalCode.PULSE -> {
                var device =
                    "${TabletInfo.deviceName}, Bat: ${AndroidUtils.getBatteryText()}, ${realNow.fullDateMinutesText}"
                if (!AndroidUtils.isFire && AndroidUtils.getBrightness() > 0) {
                    device = "BACKLIGHT! " + device
                }
                tvPageFooter.text = device
                if (tabletWasAssigned != TabletInfo.assigned) {
                    sendSignal(SignalCode.UPDATE_SCREEN)
                }
            }
            SignalCode.BATTERY -> {
                sendSignal(SignalCode.PULSE)
            }
            SignalCode.NETWORK_PROGRESS -> {
                val message = signal._payload as String?
                if (message != null) {
                    tvWaiting.text = message
                }
            }
            SignalCode.SCAN_ACUS -> {
                displayMode = DisplayMode.PROGRESS
                tvConnecting.text = "Looking for Control Boxes"
                tvError.text = ""
                try {
                    NetworkObject.onProgressListener = { message -> sendSignal(SignalCode.NETWORK_PROGRESS, message) }
                    NetworkObject.scanForAccessPoints()
                    NetworkObject.findAcus { acus ->
                        when (acus.size) {
                            0 -> {
                                tvWaiting.text = ""
                                tvError.text = "No Control Boxes in range"
                                sendSignal(SignalCode.RESET)
                            }
                            1 -> {
                                sendSignal(SignalCode.CONNECT_ACU, acus[0].tag, acus[0].BSSID)
                            }
                            else -> {
                                sendSignal(SignalCode.SELECT_MENU, "acus", acus)
                            }
                        }
                    }
                } catch (e: Throwable) {
                    e.printStackTrace()
                    tvWaiting.text = ""
                    tvError.text = e.message
                    sendSignal(SignalCode.RESET)
                }
                signal.consumed()
            }
            SignalCode.CONNECT_ACU -> {
                val tag = signal._payload as String?
                val bssid = signal._payload2 as String?
                if (tag !=null && bssid!=null) {
                    displayMode = DisplayMode.PROGRESS
                    tvConnecting.text = "Connecting to System"
                    tvError.text = ""
                    try {
                        NetworkObject.onProgressListener = { message -> sendSignal(SignalCode.NETWORK_PROGRESS, message) }
                        NetworkObject.connectAcu(tag, bssid) { errorMessage ->
                            if (errorMessage.isEmpty()) {
                                NetworkObject.onProgressListener = null
                                sendSignal(SignalCode.WIFI_CONNECTING, tag)
                            } else {
                                tvWaiting.text = ""
                                tvError.text = errorMessage
                                sendSignal(SignalCode.RESET)
                            }
                        }
                    } catch (e: Throwable) {
                        e.printStackTrace()
                        tvWaiting.text = ""
                        if (e.causedBy(Wobbly.Event.UDP_NULL_RESPONSE)) {
                            tvError.text = "No control units responding"
                        } else {
                            tvError.text = e.message
                        }
                        sendSignal(SignalCode.RESET)
                    }
                }

                signal.consumed()
            }
            SignalCode.CONNECT_NEAREST -> {
                displayMode = DisplayMode.PROGRESS
                tvConnecting.text = "Connecting to System"
                tvError.text = ""
                try {
                    NetworkObject.onProgressListener = { message -> sendSignal(SignalCode.NETWORK_PROGRESS, message) }
                    NetworkObject.connect { errorMessage ->
                        if (errorMessage.isEmpty()) {
                            NetworkObject.onProgressListener = null
                            sendSignal(SignalCode.WIFI_CONNECTING)
                        } else {
                            tvWaiting.text = ""
                            tvError.text = errorMessage
                            sendSignal(SignalCode.RESET)
                        }
                    }
                } catch (e: Throwable) {
                    e.printStackTrace()
                    tvWaiting.text = ""
                    if (e.causedBy(Wobbly.Event.UDP_NULL_RESPONSE)) {
                        tvError.text = "No control units responding"
                    } else {
                        tvError.text = e.message
                    }
                    sendSignal(SignalCode.RESET)
                }
                signal.consumed()
            }
            SignalCode.WIFI_CONNECTING -> {
                displayMode = DisplayMode.PROGRESS
                tvError.text = ""
                tvWaiting.text = "Signing on to ${NetworkObject.currentTag}"
                NetworkObject.onConnectedListener = { message -> sendSignal(SignalCode.HAVE_WIFI_CONNECTION) }
                signal.consumed()
            }
            SignalCode.HAVE_WIFI_CONNECTION -> {
                if (Global.services.acuHostname.startsWith("172.16")) {
                    Global.databaseHost = Global.services.acuHostname
                    sendSignal(SignalCode.HAVE_DATABASE_HOST)
                } else {
                    popUp("Problem", "Control box not recognised")
                    sendSignal(SignalCode.RESET)
                }
                signal.consumed()
            }
            SignalCode.HAVE_DATABASE_HOST -> {
                val acu = NetworkObject.acu
                tvWaiting.text =
                    if (acu.length == 6) "Connected to $acu (${getIpAddress("wlan0")})" else "Have server, preparing data"
                doBackground {
                    if (Global.updateNeeded()) {
                        prepareSignal(SignalCode.UPDATE_APP)
                    } else {
                        Global.connection.updateNetworkTime()
                        if (Competition.current.effectiveDate != realToday) {
                            effectiveDate = Competition.current.effectiveDate
                        } else {
                            effectiveDate = nullDate
                        }
                        AndroidServices.registerDevice()
                        with(AndroidPanic) {
                            if (hasPanic) {
                                try {
                                    logPanic(panicTime, panicClass, message, stack, "na")
                                } catch (e: Throwable) {
                                    doNothing()
                                }
                                clearPanic()
                            }
                        }
                        prepareSignal(SignalCode.LOAD_MENU_ACTIVITY)
                    }
                }
                signal.consumed()
            }
            SignalCode.LOAD_MENU_ACTIVITY -> {
                tvWaiting.text = ""
                doActivity(Menu::class.java)
                signal.consumed()
            }
            SignalCode.SELECT_MENU -> {
                val menu = signal._payload as? String
                if (menu != null) {
                    displayMode = DisplayMode.MENU
                    selectMenu(menu, signal._payload2)
                    signal.consumed()
                }
            }
            SignalCode.EXIT_MENU -> {
                displayMode = DisplayMode.NORMAL
            }
            SignalCode.BACKGROUND_EXCEPTION -> {
                val e = signal._payload as Throwable?
                if (e != null) {
                    if (TabletInfo.activity.oneOf(DEVICE_SYSTEM_MANAGER, DEVICE_UNASSIGNED) && NetworkObject.isConnected) {
                        sendSignal(SignalCode.LOAD_MENU_ACTIVITY)
                    } else {
                        e.printStackTrace()
                        tvWaiting.text = ""
                        if (e is Wobbly && e.event == Wobbly.Event.UDP_NULL_RESPONSE) {
                            tvError.text = "No control units responding"
                        } else {
                            tvError.text = e.message
                        }
                        sendSignal(SignalCode.RESET)
                    }
                }
            }
            SignalCode.UPDATE_APP -> {
                msgYesNo("Upgrade Needed (${control.graniteVersion})", "Do you want to upgrade now?\n\nPress 'Yes' followed by 'Install' then 'Open' when prompted") { yes ->
                    if (yes) {
                        tvWaiting.text = "Downloading upgrade..."
                        updateApp()
                    } else {
                        sendSignal(SignalCode.RESET)
                    }

                }

                signal.consumed()
            }
            SignalCode.REASSIGN_TABLET -> {
                Global.reassignTabletUsage = true
                sendSignal(SignalCode.CONNECT_NEAREST)
            }
            SignalCode.UNASSIGN_TABLET -> {
                Global.reassignTabletUsage = true
                TabletInfo.activity = DEVICE_UNASSIGNED
                TabletInfo.saveConfig(this)
                tvUsage.text = TabletInfo.activityText
                sendSignal(SignalCode.EXIT_MENU)
            }
            SignalCode.UPDATE_TABLET -> {
                updateTablet()
            }
            SignalCode.UPDATE_TABLET_SD -> {
                updateAppfromCard()
            }
            SignalCode.TABLET_SETTINGS -> {
                val dialogIntent = Intent(android.provider.Settings.ACTION_SETTINGS)
                dialogIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(dialogIntent)
            }
            SignalCode.BACKLIGHT_ON -> {
                brightness = 127;
                if (!AndroidUtils.isFire) AndroidUtils.setBrightness(brightness)
            }
            SignalCode.BACKLIGHT_OFF -> {
                brightness = 0;
                if (!AndroidUtils.isFire) AndroidUtils.setBrightness(brightness)
            }
            SignalCode.TABLET_RELOAD -> {
                finish()
            }
            else -> {
                super.whenSignal(signal)
            }
        }
    }
    
    fun selectMenu(menu: String, _payload: Any? = null) {
        loMenu.removeAllViews()
        when (menu) {
            "control" -> {
                loMenu.columnCount = 2
                tvSubTitle.text = "Tablet Options"
                addMenuButton(loMenu, "Un-assign", SignalCode.UNASSIGN_TABLET)
                val btSettings = addMenuButton(loMenu, "Settings", SignalCode.TABLET_SETTINGS)
                addMenuButton(loMenu, "Update", SignalCode.UPDATE_TABLET)
                val btUpdateSd = addMenuButton(loMenu, "Update (SD)", SignalCode.UPDATE_TABLET_SD)
                addMenuButton(loMenu, "Light On", SignalCode.BACKLIGHT_ON)
                addMenuButton(loMenu, "Light Off", SignalCode.BACKLIGHT_OFF)
                addMenuButton(loMenu, "Reload", SignalCode.TABLET_RELOAD)
                addMenuButton(loMenu, "Back", SignalCode.EXIT_MENU)
                btSettings.isEnabled=power.isCharging
                btUpdateSd.isEnabled = AndroidUtils.isExternalStorageAvailable()
            }
            "acus" -> {
                val acus = _payload as ArrayList<NetworkObject.AcuData>?
                if (acus!=null) {
                    loMenu.columnCount = 1
                    tvSubTitle.text = "Select Your Ring's Control Box"
                    var count=0
                    for (acu in acus) {
                        count++
                        if (count<=3) {
                            addMenuButton(loMenu, "${acu.tag} (${acu.signal}%)", SignalCode.CONNECT_ACU, acu.tag, acu.BSSID)
                        }
                    }
                    addMenuButton(loMenu, "None of the Above", SignalCode.BACK)
                }
            }
        }
    }
}
