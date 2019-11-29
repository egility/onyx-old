/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.android.tools

import android.content.Context
import android.content.Intent
import android.net.NetworkInfo
import android.net.wifi.ScanResult
import android.net.wifi.SupplicantState

import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import org.egility.library.general.*
import java.lang.Thread.sleep

/**
 * Created by mbrickman on 02/11/15.
 */

var ssid = "sandstone"
var key = "bingbing"

object NetworkObject {

    private val wifiManager = androidApplication.getSystemService(Context.WIFI_SERVICE) as WifiManager
   
    var currentTag = ""
    
    private val wifiLock = wifiManager.createWifiLock(WifiManager.WIFI_MODE_SCAN_ONLY, "granite")

    private var onConnectingListener: ((String) -> Unit)? = null
    var onConnectedListener: ((String) -> Unit)? = null
    private var onScannedListener: ((ArrayList<AcuData>) -> Unit)? = null
    var onProgressListener: ((String) -> Unit)? = null

    init {
        wifiLock.setReferenceCounted(false)
        broadcastReceiver.onSupplicantStateChange = { intent ->
            val state = intent.getParcelableExtra<SupplicantState>(WifiManager.EXTRA_NEW_STATE)
            debug("network", "Supplicant state = $state")
            if (state == SupplicantState.COMPLETED) {
                whenConnecting()
            }
        }
        broadcastReceiver.onScanResultsAvailable = { intent -> whenScanComplete(intent) }
        broadcastReceiver.onNetworkStateChange = { intent ->
            val networkInfo = intent.getParcelableExtra<NetworkInfo>(WifiManager.EXTRA_NETWORK_INFO)
            if (networkInfo.isConnected) {
                whenConnected()
            }
            debug("network", "Network state = ${networkInfo.state?:"unknown"}")
        }
    }

    val accessPointMac: String
        get() = wifiManager.connectionInfo.bssid

    val accessPoint: String
        get() = wifiManager.connectionInfo.ssid.replace("\"", "")

    val acu: String
        get() = ssidToTag(accessPoint)

    val gateway: String
        get() {
            var gateway = wifiManager.dhcpInfo.gateway
            while (gateway == 0) {
                debug("gateway", "waiting for gateway")
                sleep(500)
                gateway = wifiManager.dhcpInfo.gateway
            }
            val ipStr = String.format(
                "%d.%d.%d.%d",
                gateway and 0xff,
                gateway shr 8 and 0xff,
                gateway shr 16 and 0xff,
                gateway shr 24 and 0xff
            )
            debug("gateway", "is $ipStr")
            return ipStr
        }

    private val MIN_RSSI = -100
    private val MAX_RSSI = -55

    //* copied from android WifiManager because not properly implemented on FireOS */
    fun calculateSignalLevel(rssi: Int, numLevels: Int): Int {
        if (rssi <= MIN_RSSI) {
            return 0
        } else if (rssi >= MAX_RSSI) {
            return numLevels - 1
        } else {
            val inputRange = (MAX_RSSI - MIN_RSSI).toFloat()
            val outputRange = (numLevels - 1).toFloat()
            return ((rssi - MIN_RSSI).toFloat() * outputRange / inputRange).toInt()
        }
    }
    val signalStrength: Int
        get() {
            val wifiInfo = wifiManager.connectionInfo
            return calculateSignalLevel(wifiInfo.rssi, 100)
        }

    val rssi: Int
        get() {
            val wifiInfo = wifiManager.connectionInfo
            return wifiInfo.rssi
        }

    val isConnected: Boolean
        get() = wifiManager.isWifiEnabled && wifiManager.connectionInfo.supplicantState == SupplicantState.COMPLETED

    fun removeConfiguredNetworks() {
        val networks = wifiManager.configuredNetworks
        if (networks != null) {
            for (network in networks) {
                debug("network", "removeNetwork: ${network.networkId}")
                wifiManager.removeNetwork(network.networkId)
            }
        }
    }

    fun connectAcu(tag: String, bssid: String, onConnectingListener: (String) -> Unit) {
        
        for (accessPoint in wifiManager.scanResults) {
            if (accessPoint.BSSID==bssid) {
                val networkId = getNetworkId(accessPoint)
                currentTag = tag
                showProgress("Linking to $tag")
                this.onConnectingListener = onConnectingListener
                val enabled = wifiManager.enableNetwork(networkId, true)
                if (!enabled) {
                    whenConnected("Can not enable network")
                }
                return
            }
        }
        whenConnected("Unable to connect to $tag")
    }


    fun connect(onConnectedListener: (String) -> Unit) {
        onScannedListener = {acus ->
            if (acus.size>0) {
                connectAcu(acus[0].tag, acus[0].BSSID, onConnectedListener)
            }
        }
        if (isConnected) {
            whenConnected()
        } else {
            if (!wifiManager.setWifiEnabled(true)) {
                whenConnected("Unable to switch enable WiFi")
            } else {
                wifiLock.acquire()
                showProgress("Searching for control boxes")
            }
        }
    }
    
    
    val acuTag: String
    get() {
        return ssidToTag(accessPoint)
    }
    
    fun ssidToTag(ssid: String): String {
        if (!ssid.startsWith("sandstone")) return ""
        val number=ssid.drop(9).toIntDef(0)
        return "acu%03d".format(number)
    }

    data class AcuData(val tag: String, val SSID: String, val BSSID: String, val signal: Int, val level: Int)

    val acus = ArrayList<AcuData>()

    fun findAcus(onDone: (ArrayList<AcuData>) -> Unit) {
        onScannedListener = onDone
        if (isConnected) {
            whenConnected()
        } else {
            if (wifiManager.setWifiEnabled(true)) {
                wifiLock.acquire()
            }
        }
        wifiManager.startScan()
    }

    fun checkNetwork() {
    }

    fun disconnect() {
        debug("network", "disconnect")
        wifiManager.isWifiEnabled = false
        if (wifiLock.isHeld) {
            wifiLock.release()
        }
    }

    fun scanForAccessPoints(onScannedListener: (ArrayList<AcuData>) -> Unit) {
        wifiManager.startScan()
        this.onScannedListener = onScannedListener
    }

    private fun getNetworkId(scanResult: ScanResult): Int {
/*
        val networks = StringBuilder()
        for (config in wifiManager.configuredNetworks) {
           networks.append("\n${config.BSSID?:"null"} (${config.SSID?:"null"})")
        }
        debug("networks", networks.toString())
 */
        val config = WifiConfiguration()
        config.BSSID = scanResult.BSSID
        config.SSID = scanResult.SSID.quoted
        config.preSharedKey = key.quoted
        val networkId = wifiManager.addNetwork(config)
        debug("network", "addNetwork: networkId=$networkId")
        mandate(networkId != -1, "failed to add new network configuration")
        return networkId
    }

    fun getStrongestAccessPoint(): ScanResult? {
        var strongestAccessPoint: ScanResult? = null
        for (accessPoint in wifiManager.scanResults) {
            debug("network", "Scan: ssid=${accessPoint.SSID}, bssid=${accessPoint.BSSID}, level=${calculateSignalLevel(accessPoint.level, 100)}")
            if (accessPoint.SSID.startsWith(ssid)) {
                val strongest = strongestAccessPoint
                if (strongest == null || WifiManager.compareSignalLevel(strongest.level, accessPoint.level) < 0) {
                    strongestAccessPoint = accessPoint
                }
            }
        }
        return strongestAccessPoint
    }

    val scanResults: List<ScanResult>
        get() = wifiManager.scanResults

    fun showProgress(message: String) {
        debug("network", "progress: $message")
        val listener = onProgressListener
        if (listener != null) {
            listener(message)
        }
    }

    fun scanForAccessPoints() {
        val ok = wifiManager.startScan()
        if (!ok) {
            whenConnected("Unable to start network.scan")
        }
    }


    fun whenScanComplete(intent: Intent) {
        synchronized(acus) {
            acus.clear()
            for (accessPoint in wifiManager.scanResults) {
                val signal = calculateSignalLevel(accessPoint.level, 100)
                val tag = ssidToTag(accessPoint.SSID)
                if (tag.startsWith("acu") && tag.length > 3) {
                    acus.add(AcuData(tag, accessPoint.SSID, accessPoint.BSSID, signal, accessPoint.level))
                }
            }
            if (acus.size>1) {
                acus.sortByDescending { it.level }
                var list=""
                acus.forEach { list = list.append("${it.tag} (${it.signal})") }
                debug("network", "Scan: $list")
            }
        }
        
        debug("network", "Scan complete ${if (onScannedListener == null) "not listening" else "have listener"}")
        val listener = onScannedListener
        onScannedListener = null
        if (listener != null) {
            listener(acus)
        }
    }

    private fun whenConnecting(errorMessage: String = "") {
        debug("network", "Have connection ${if (onConnectingListener == null) "not listening" else "have listener"}")
        val listener = onConnectingListener
        onConnectingListener = null
        if (listener != null) {
            listener(errorMessage)
        }
    }

    private fun whenConnected(errorMessage: String = "") {
        debug("network", "Have connection ${if (onConnectedListener == null) "not listening" else "have listener"}")
        val listener = onConnectedListener
        onConnectedListener = null
        if (listener != null) {
            listener(errorMessage)
        }
    }
}
