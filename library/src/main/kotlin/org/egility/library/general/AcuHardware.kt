/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.library.general

/*
 * Copyright (c) Mike Brickman 2014-1015.
 */

import org.egility.library.database.DbConnection
import org.egility.library.database.DbJdbcConnection
import org.egility.library.database.DbQuery
import org.egility.library.dbobject.*
import org.egility.library.general.hardware.apChannel
import org.egility.library.general.hardware.clusterRingManual
import org.egility.library.general.hardware.doDatabaseReport
import org.egility.library.general.hardware.dongleSim
import sun.print.CUPSPrinter
import java.io.File
import javax.print.PrintServiceLookup
import kotlin.Comparator
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.concurrent.thread

/*
Error Codes

A* = Access Point error
A0 - No Access Point WiFi device found when boot
A1 - Access Point WiFi device has gone missing
A2 - Access Point not setup - will attempt to setup // ap0 misson 
A3 - Access Point software not running - will attempt to restart // hostapd
A4 - Sign On (DHCP) software not running - will attempt to restart 

D0 - Database service is not running - will attempt to restart  
D1 - Database server is not responding
D2 - Database unable to process data from one or more other boxes

I1 - Internet service (vpn) not running - will restart
I2 - Can't reach main server (ping vpn server)

L* - Link error (* is box number for which data is not being received)

N* = Network (Mesh) error
N0 - No network WiFi device found when boot
N1 - Network WiFi device has gone missing
N2 - Not connected to network - will attempt to connect // mesh0 missing
N3 - Dongle has reconnected more than 10 times

P0 - Printer service not running - will attempt to start

R0 - No route controller

T0 - No time



 */

val SILENT_ERROR = "~"

class AcuThread(list: ArrayList<AcuThread>, val name: String, var interval: Int = 60, val wait: Int = 0, val onLoop: (AcuThread) -> Unit) {

    var error: String = ""
        set(value) {
            errorSet = true
            hasChanged = value.isEmpty() != passed
            passed = value.isEmpty()
            field = if (value == SILENT_ERROR) "" else value
        }

    var passed = false
    var hasChanged = false
    private var errorSet = false
    private var done = false

    val noError: Boolean
        get() = !(hasChanged && error.isNotEmpty())

    fun reset() {
        error = ""
        passed = false;
        hasChanged = false
        errorSet = false
    }

    fun done() {
        done = true
        if (!errorSet) error = ""
        debug("acu test", "name=$name, passed=$passed, has changed=$hasChanged, error=$error")
    }

    init {
        list.add(this)
        thread(name = name, start = true) {
            try {
                Thread.sleep(wait * 1000L)
                while (hardware.running) {
                    try {
                        errorSet = false
                        hasChanged = false
                        done = false
                        onLoop(this)
                        if (!done) done()
                    } catch (e: Throwable) {
                        hardware.logError(e)
                    }
                    Thread.sleep(interval * 1000L)
                }
            } catch (e: Throwable) {
                hardware.logError(e)
            }
        }
    }
}

enum class Gateway { NONE, REMOTE, DONGLE, ETHERNET }

object hardwareOs {

    var name: String = ""
    var version: String = ""
    var id: String = ""
    var idLike: String = ""
    var versionId: String = ""
    var codeName: String = ""

    init {
        val data = fileToString("/etc/os-release").split("\n")
        for (item in data) {
            val key = item.substringBefore("=")
            val value = item.substringAfter("=")
            when (key) {
                "NAME" -> name = value
                "VERSION" -> version = value
                "ID" -> id = value
                "ID_LIKE" -> idLike = value
                "VERSION_ID" -> versionId = value
                "VERSION_CODENAME" -> codeName = value
            }
        }
    }
}

fun Map<String, String>.getDef(key: String, default: String): String {
    return this[key] ?: default
}

object hardware {
    val threads = ArrayList<AcuThread>()
    var running = true
    var shuttingDown = false
    var zteStatus = ""
    var signalStrength = 0
    var mobileConnection = ""

    var exceptionTime = nullDate
    var exception: Throwable? = null
    val haveException: Boolean
        get() = exceptionTime > now.addMinutes(-2) && exception != null

    var ethernetState = 0
    var dongleState = 0
    var dongleSim = true
    var dongleHasSignal = false
    var dongleReport = Json()

    var alertSequence = 0

    var slaveIssue = false

    var clusterRing = ArrayList<String>()
    var clusterRingInstance = 0
    var clusterRingController = ""

    var clusterRingManual = false

    val masters = ArrayList<String>()

    val uuidMap = HashMap<String, String>()
    val mesh0Map = HashMap<String, String>()

    var isLinkedToWeb = false
    val acuId = getHostname().dropLeft(3).toIntDef(999)

    var meshPattern = "192.168.2.xxx"

    val meshDevice: String
        get() = fileToString("/usr/local/bin/mesh.dat")

    val apDevice: String
        get() = fileToString("/usr/local/bin/ap.dat")

    var channel: Int = -1
        get() {
            if (field == -1) {
                val channelAsString = fileToString("/usr/local/bin/channel_mesh.dat")
                field = channelAsString.toIntDef(-1)
            }
            return field
        }

    var apChannel: String = ""
        get() {
            if (field.isEmpty()) {
                field = execStr("grep channel /etc/hostapd/hostapd.conf").substringAfter("=")
            }
            return field
        }

    fun addUuid(tag: String, uuid: String) {
        if (uuid.isNotEmpty() && !uuidMap.containsKey(uuid)) {
            uuidMap.put(uuid, tag)
        }
    }

    fun uuidToTag(uuid: String): String {
        return uuidMap.getDef(uuid, uuid)
    }

    fun addMesh0Mac(tag: String, mac: String) {
        if (mac.isNotEmpty() && !mesh0Map.containsKey(mac)) {
            mesh0Map.put(mac, tag)
        }
    }

    fun mesh0ToTag(mac: String): String {
        return mesh0Map.getDef(mac, "")
    }

    fun logError(e: Throwable) {
        println(e.message ?: "No Error Message")
        println(e.stack)

        hardware.exceptionTime = now
        hardware.exception = e
    }

    val errorCodes: String
        get() {
            var result = ""
            for (thread in threads) result += thread.error
            return if (result.isEmpty()) "[OK]" else result
        }

    var badPingCount = 0
    var badVpnPingCount = 0
    var gatewayCountDown = 0
    val pingFail: Boolean
        get() = badPingCount >= 12

    var timeState = 0

    init {
        AcuThread(threads, "time", 10, wait = 1) {
            if (timeState == 0) {
                val table = execStr("systemctl status systemd-timesyncd")
                for (line in table.split("\n")) {
                    if (line.contains("Status: \"Synchronized to")) {
                        timeState = TIME_INTERNET
                    }
                }
            }
            if (timeState == 0) it.error = "T0"
        }

        AcuThread(threads, "mesh", 10, wait = 1) {
            if (meshDevice.isEmpty()) {
                it.error = "N0"
                exec("/usr/local/bin/do-prep-wifi")
            } else if (!haveDevice(meshDevice)) {
                it.error = "N1"
                exec("/usr/local/bin/do-prep-wifi")
            } else if (!haveInterface("mesh0")) {
                it.error = "N2"
                thread {
                    debug("hardware", "bringUpMesh - [$meshDevice]")
                    exec("/usr/local/bin/systemd-batman restore $acuId")
                }
            }
        }

        AcuThread(threads, "meshDevice", 120, wait = 1) {
            val phyIndex = if (meshDevice.isEmpty()) 0 else meshDevice.dropLeft(3).toIntDef(-1)
            if (phyIndex > 10) it.error = "N3"
        }

        AcuThread(threads, "meshLinks", 10, wait = 1) {
            var error = ""
            for (acu in masters) {
                val index = acu.dropLeft(3).toIntDef(-1)
                if (index > 0) {
                    val ip = "192.168.2.$index"
                    if (!ping(ip, timeout = 1)) {
                        error += "L$index"
                    }
                }
            }
            it.error = error
        }


/*
Aug 18 16:13:06 acu000 kernel: [30157.024138] usbcore: deregistering interface driver brcmfmac

Aug 18 16:13:14 acu000 kernel: [30165.299397] brcmfmac: F1 signature read @0x18000000=0x1541a9a6
Aug 18 16:13:14 acu000 kernel: [30165.304043] brcmfmac: brcmf_fw_alloc_request: using brcm/brcmfmac43430-sdio for chip BCM43430/1
Aug 18 16:13:14 acu000 kernel: [30165.304230] usbcore: registered new interface driver brcmfmac
================================================================================================== BAD
Aug 18 16:13:17 acu000 kernel: [30167.921581] brcmfmac: brcmf_sdio_bus_rxctl: resumed on timeout
Aug 18 16:13:17 acu000 kernel: [30167.925019] brcmfmac: brcmf_bus_started: failed: -110
Aug 18 16:13:17 acu000 kernel: [30167.925043] brcmfmac: brcmf_attach: dongle is not responding: err=-110
Aug 18 16:13:17 acu000 kernel: [30168.005253] brcmfmac: brcmf_sdio_firmware_callback: brcmf_attach failed

Aug 18 16:14:39 acu000 kernel: [30249.746048] usbcore: deregistering interface driver brcmfmac

Aug 18 16:14:46 acu000 kernel: [30256.737419] brcmfmac: F1 signature read @0x18000000=0x1541a9a6
Aug 18 16:14:46 acu000 kernel: [30256.744945] brcmfmac: brcmf_fw_alloc_request: using brcm/brcmfmac43430-sdio for chip BCM43430/1
Aug 18 16:14:46 acu000 kernel: [30256.745117] usbcore: registered new interface driver brcmfmac
===================================================================================================== GOOD
Aug 18 16:14:46 acu000 kernel: [30256.907779] brcmfmac: brcmf_fw_alloc_request: using brcm/brcmfmac43430-sdio for chip BCM43430/1
Aug 18 16:14:46 acu000 kernel: [30256.907832] brcmfmac: brcmf_c_process_clm_blob: no clm_blob available (err=-2), device may have limited channels available
Aug 18 16:14:46 acu000 kernel: [30256.908373] brcmfmac: brcmf_c_preinit_dcmds: Firmware: BCM43430/1 wl0: Oct 23 2017 03:55:53 version 7.45.98.38 (r674442 CY) FWID 01-e58d219f
Aug 18 16:14:48 acu000 kernel: [30258.484920] brcmfmac: power management disabled


||||||||||||||||||||||||||||||||||||||||||||||||||||||||| FAILED TO GET UNDER CONTROL ||||||||||||||||||||||||||||||||||||
Aug 30 11:40:58 acu000 kernel: [15629.276682] usbcore: deregistering interface driver brcmfmac
Aug 30 11:40:59 acu000 kernel: [15630.395457] brcmfmac: F1 signature read @0x18000000=0x1541a9a6
Aug 30 11:40:59 acu000 kernel: [15630.406440] brcmfmac: brcmf_fw_alloc_request: using brcm/brcmfmac43430-sdio for chip BCM43430/1
Aug 30 11:40:59 acu000 kernel: [15630.406720] usbcore: registered new interface driver brcmfmac
Aug 30 11:41:02 acu000 kernel: [15632.984562] brcmfmac: brcmf_sdio_bus_rxctl: resumed on timeout <<<<<<<< goes wrong here
Aug 30 11:41:02 acu000 kernel: [15632.987608] brcmfmac: brcmf_bus_started: failed: -110
Aug 30 11:41:02 acu000 kernel: [15632.987660] brcmfmac: brcmf_attach: dongle is not responding: err=-110
Aug 30 11:41:02 acu000 kernel: [15633.027998] brcmfmac: brcmf_sdio_firmware_callback: brcmf_attach failed
        
 */

        val brcmfmacMonitor = ChangeMonitor("initial")
        val brcmfmacFound = fileToString("/usr/local/bin/have_mmc.dat").toIntDef(0) == 1
        AcuThread(threads, "accessPoint", 2, wait = 1) {
            var error = ""
            // check for brcmfmac failure
            val table =
                execStr("grep --text 'brcmfmac: brcmf_sdio_hostmail: mailbox indicates firmware halted' /var/log/syslog")
            val entries = table.split("\n")
            val fails = if (table.startsWith("Error")) 0 else entries.size
            val last = if (table.startsWith("Error")) "" else entries.last()
            if (brcmfmacMonitor.value == "initial") {
                debug("brcmfmac", "initial - ${if (last.isEmpty()) "empty" else last}")
                brcmfmacMonitor.value = last
            }

            if (brcmfmacMonitor.hasChanged(last) && last.isNotEmpty()) {
                debug("brcmfmac", "failed - $last")
                exec("/usr/local/bin/systemd-ap fix_brcmfmac $acuId")
            } else if (apDevice.isEmpty()) {
                error = "A0"
                exec("/usr/local/bin/do-prep-wifi")
            } else if (!haveDevice(apDevice)) {
                error = "A1"
                if (brcmfmacFound) {
                    debug("brcmfmac", "missing ($apDevice)")
                    exec("/usr/local/bin/systemd-ap fix_brcmfmac $acuId")
                } else {
                    exec("/usr/local/bin/do-prep-wifi")
                }
            } else if (!haveInterface("ap0")) {
                error = "A2"
                thread {
                    debug("accessPoint", "bringUpAccessPoint - [$apDevice]")
                    exec("/usr/local/bin/systemd-ap restore $acuId")
                }
            } else if (!serviceRunning("hostapd")) {
                error = "A3"
                debug("accessPoint", "restart hostapd")
                thread { restartService("hostapd") }
            } else if (!serviceRunning("isc-dhcp-server")) {
                error = "A4"
                thread { restartService("isc-dhcp-server") }
            }
            if (fails > 0) error += "F$fails"
            if (error.isNotEmpty()) it.error = error
        }

        AcuThread(threads, "mysql", 20) {
            if (!serviceRunning("db@sandstone")) {
                it.error = "D0"
                thread { restartService("db@sandstone") }
            }
            try {
                dbQuery("SELECT 1 AS ping", connection = localConnection, silent = true) {
                    haveMysql = true
                }
            } catch (e: Throwable) {
                logError(e)
                haveMysql = false
                it.error = "D1"
            }
            if (!registered && it.noError) thread { hardware.registerAcu() }
        }

        AcuThread(threads, "replication", 20) {
            if (meshTest.passed && meshLinksTest.passed && mysqlTest.passed && it.noError) {
                it.error = if (ReplicationFault.errorCode(Competition.current.id).isNotEmpty()) "D2" else ""
            }
        }

        AcuThread(threads, "linkWeb", 10, wait = 30) {
            if (!isLinkedToWeb && internetTest.passed && (gatewayDevice == Gateway.DONGLE || gatewayDevice == Gateway.ETHERNET) && acuId < 240) {
                var alreadyLinked = false
                val proposedChannel = "${Competition.current.uniqueName}_acu$acuId"
                val plaza = DbJdbcConnection(Sandstone(databaseHost = "10.8.0.1"))
                dbQuery("SHOW SLAVE STATUS", plaza) {
                    val channel = getString("Channel_Name")
                    if (channel.eq(proposedChannel)) alreadyLinked = true
                }
                if (!alreadyLinked) {
                    dbExecute(
                        """
                        CHANGE MASTER TO MASTER_HOST='10.8.1.$acuId',
                        MASTER_PORT=3340,
                        MASTER_USER = 'slave',
                        MASTER_PASSWORD = 'replicate',
                        MASTER_AUTO_POSITION = 1
                        FOR CHANNEL '$proposedChannel' 
                    """.trimIndent(), plaza
                    )
                    dbExecute("START SLAVE FOR CHANNEL '$proposedChannel'", plaza)
                }
                isLinkedToWeb = true
            }
        }

        AcuThread(threads, "meshData", 10) {
            AcuMesh.update()
        }

        AcuThread(threads, "heartbeat", 10) {
            heartbeats.pruneDead()
        }

        AcuThread(threads, "cluster", 10) {
            if (!clusterRingManual) {
                if (!clusterRegistered) {
                    clusterRegistered = true
                    debug("clusterTest", "ip=${hardware.ip}")
                    Global.databaseHost = hardware.ip
                }
                if (heartbeats.clusterControllerIsMe) {
                    var incomplete = false
                    val list = ArrayList<String>(clusterRing)
                    heartbeats.activePeers.forEach {
                        if (!list.remove(it)) {
                            incomplete = true
                        }
                    }
                    if (!list.isEmpty()) incomplete = true
                    if (incomplete || (cpuTime - lastFullCheck) > 300000) {
                        lastFullCheck = cpuTime
                        val cluster = heartbeats.getCluster()
                        cluster.goFigure()
                        if (cluster.bestRoute.size > 0 && cluster.bestRoute neq clusterRing) {
                            setMasterRing(getHostname(), clusterRingInstance + 1, cluster.bestRoute)
                            debug("clusterRoute", cluster.bestRoute.asCommaList())
                        }
                        cluster.populateJson(clusterData)
                    }
                } else {
                    clusterData.clear()
                    with(heartbeats.clusterRing) {
                        if (error) {
                            it.error = "R0"
                        } else {
                            setMasterRing(controller, instance, ring)
                        }
                    }
                }
            }
        }

        AcuThread(threads, "print", 10, wait = 10) {
            val report = Json()
            if (!CUPSPrinter.isCupsRunning()) {
                it.error = "P0"
                report["noCupsRunning"] = true
                thread { restartService("cups") }
            } else {
                synchronized(printers) {
                    scanForPrinters()
                    var best = AcuPrinter("", "")
                    try {
                        printers.forEach { deviceName, printer ->
                            printer.onLine = ping(printer.host, timeout = 10)
                            if (printer.onLine) {
                                if (printer.ipAddress.isNotEmpty()) {
                                    val ip = printer.ipAddress.split(".")
                                    if (ip.size == 4) {
                                        printer.isAttached =
                                            ip[2].toIntDef(0) == getHostname().dropLeft(3).toIntDef(999)
                                    } else {
                                        printer.isAttached = printer.ethernet || isAccessPointClient(printer.mac)
                                    }
                                } else {
                                    printer.isAttached = printer.ethernet || isAccessPointClient(printer.mac)
                                }
                                //printer.isAttached = printer.ipAddress.substringBeforeLast(".")=
                                if (printer.isAttached && !printer.hasCups) {
                                    val uri = "ipp://${printer.host}:${printer.port}/ipp/print"
                                    val error = exec("lpadmin -p ${printer.cupsName} -v $uri -E -m everywhere")
                                    if (error == 0) printer.hasCups = true
                                    exec("lpoptions -p ${printer.cupsName} -o 'PageSize=A4'")
                                    hardware.refreshSystemPrinterList()
                                }
                                if (best.name.isEmpty() || printer.isAttached && !best.isAttached || best.ethernet) {
                                    best = printer
                                }
                            }
                        }

                        if (best.name.isNotEmpty() && best.name != defaultPrinter.name) {
                            val error = exec("lpadmin -d ${best.cupsName}")
                            if (error == 0) defaultPrinter = best
                        }
                        report["default"] = defaultPrinter.name
                    } catch (e: Throwable) {
                        val node = report["error"]
                        node["message"] = e.message ?: "No Message"
                        node["stack"] = e.stack
                        logError(e)
                    }

                    it.interval = 60

                }
            }
        }

        AcuThread(threads, "dongle", 5) {
            val haveDongle = haveInterface("usb0")
            it.error = if (haveDongle) "" else SILENT_ERROR
            it.done()
            if (it.hasChanged) selectGateway()
            if (haveDongle) {
                thread { checkZte() }
            } else {
                thread {
                    synchronized(diagnostics) {
                        diagnostics.dongleData.clear()
                    }
                }
            }
        }

        AcuThread(threads, "ethernet", 10) {
            it.error = if (haveCarrier("eth0")) "" else SILENT_ERROR
            it.done()
            if (it.hasChanged) selectGateway()
        }

        AcuThread(threads, "internet", 10, wait = 5) {
            if (gateway.isNotEmpty()) {
                badPingCount = if (ping("8.8.8.8")) 0 else badPingCount + 1
                if (pingFail) {
                    if (gateway.eq("eth0")) ethernetState = 2
                    if (gateway.eq("usb0")) dongleState = 2
                    if (!serviceRunning("openvpn@client")) {
                        it.error = "I1"
                        thread { restartService("openvpn@client") }
                    } else if (!ping("10.8.0.1")) {
                        badVpnPingCount = if (ping("10.8.0.1")) 0 else badVpnPingCount + 1
                        if (badVpnPingCount > 3) it.error = "I2"
                    }
                }
            }

            it.done()
            if (gatewayCountDown <= 0 || pingFail) {
                if (!gateway.eq("eth0")) ethernetState = 0
                if (!gateway.eq("usb0")) dongleState = 0
                selectGateway()
            }
            gatewayCountDown--
        }

    }

    fun selectGateway() {
        synchronized(gateway) {
            val proposed = when {
                ethernetTest.passed && ethernetState == 0 -> "eth0"
                dongleTest.passed && dongleState == 0 -> "usb0"
                meshTest.passed -> heartbeats.bestGateway
                else -> ""
            }
            debug("acu gateway", "is=$gateway, proposed=$proposed, ethernetTest=${ethernetTest.passed}, ethernetState=$ethernetState, dongleTest=${dongleTest.passed}, dongleState=$dongleState, meshTest=${meshTest.passed}")
            if (proposed.neq(gateway)) {
                badPingCount = 0
                gateway = proposed
            }
            gatewayCountDown = 30
        }
    }

    fun getThread(name: String): AcuThread {
        for (thread in threads) {
            if (thread.name.eq(name)) return thread
        }
        return AcuThread(threads, name) { doNothing() }
    }

    var meshTest = getThread("mesh")
    var meshLinksTest = getThread("meshLinks")
    var accessPointTest = getThread("accessPoint")
    var mysqlTest = getThread("mysql")
    var ethernetTest = getThread("ethernet")
    var dongleTest = getThread("dongle")
    var internetTest = getThread("internet")


    //////////////////////////////////////////////////////////////////////////////////////////////


/*
    var dhcpdTest = AcuTest("dhcpd", 10, wait = 1) { test ->
        if (wifiTest.state) {
            if (test.data==0) {
                debug("acu", "dhcpdTest -> getHostname = ${getHostname()}")
                val ipAddress = acuToApIp(getHostname())
                setInterfaceIp("ap0", ipAddress)
                if (interfaceIp("ap0").eq(ipAddress)) {
                    debug("acu", "dhcpdTest -> ap0 switched to $ipAddress")
                    test.data=1
                    restartService("isc-dhcp-server")
                }
            }
        }
    }    

 */


    var haveMysql = false

    fun processRanges(raw: String, node: JsonNode) {
        val ranges = raw.split(",\n")
        node.clear()
        for (item in ranges) {
            val uuid = item.substringBefore(":")
            val range = item.substringAfter(":")
            val tag = uuidToTag(uuid)
            node.addElement().setValue("$tag:$range")
        }
        node.sort(Comparator { a, b ->
            a.asString.compareTo(b.asString)
        })
    }

    fun doDatabaseReport() {
        val report = Json()

        if (mysqlTest.passed) {
            try {
                report["serverId"] = serverId
                report["uuid"] = uuid
                report["idSite"] = idSite

                val masterQuery = DbQuery("SHOW MASTER STATUS", _connection = hardware.localConnection)
                if (masterQuery.found()) {
                    processRanges(masterQuery.getString("Executed_Gtid_Set"), report["executedGtidSet"])
                }
                val slaveHostQuery = DbQuery("SHOW SLAVE HOSTS", _connection = hardware.localConnection)
                while (slaveHostQuery.next()) {
                    report["slaves"].addElement().setValue(uuidToTag(slaveHostQuery.getString("Slave_UUID")))
                }
                var slaveIssue = false
                dbQuery("SHOW SLAVE STATUS", connection = hardware.localConnection) {
                    val node = report["masters"].addElement()
                    node["masterHost"] = getString("Master_Host")
                    node["slaveIoState"] =
                        if (getString("Slave_IO_State") != "Waiting for master to send event") getString("Slave_IO_State") else ""
                    node["slaveRunningState"] =
                        if (getString("Slave_SQL_Running_State") != "Slave has read all relay log; waiting for more updates") getString("Slave_SQL_Running_State") else ""
                    node["lastIoErrorNumber"] = getInt("Last_IO_Errno")
                    node["lastIoError"] = getString("Last_IO_Error")
                    node["lastSqlErrorNumber"] = getInt("Last_SQL_Errno")
                    node["lastSqlError"] = getString("Last_SQL_Error")
                    node["ioNotRunning"] = getString("Slave_IO_Running") != "Yes"
                    node["sqlNotRunning"] = getString("Slave_SQL_Running") != "Yes"

                    processRanges(getString("Retrieved_Gtid_Set"), report["Retrieved_Gtid_Set"])
                    if (node["ioNotRunning"].asBoolean ||
                        node["sqlNotRunning"].asBoolean ||
                        node["lastIoError"].asString.isNotEmpty() ||
                        node["lastSqlError"].asString.isNotEmpty() ||
                        node["slaveIoState"].asString.isNotEmpty()
                    ) {
                        slaveIssue = true
                    }
                }
                hardware.slaveIssue = slaveIssue
            } catch (e: Throwable) {
                logError(e)
                val event = e.event()
                report["databaseIssue"] = true
                if (event == Wobbly.Event.UNDEFINED) {
                    report["ErrorMessage"] = e.message ?: ""
                } else {
                    report["ErrorCode"] = event.code
                    report["ErrorMessage"] = event.message
                }
            }
        }
        synchronized(diagnostics) {
            diagnostics.database.setValue(report)
            diagnostics.cluster.setValue(clusterData)
        }
    }


    var clusterRegistered = false
    var clusterData = Json()

    var lastFullCheck = 0L

    fun getPrintJobs(node: JsonNode) {
        try {
            node.clear()
            val lpstat = execStr("lpstat -o")
            val jobs = lpstat.split("\n")
            for (job in jobs) {
                if (job.isNotEmpty()) {
                    val name = job.substring(0, 15)
                    val size = job.substring(35, 46).trim().toIntDef(0).bytesToMegaBytes()
                    val submitted = job.substring(49)
                    val element = node.addElement()
                    element["name"] = name
                    element["size"] = size
                    element["submitted"] = submitted
                }
            }
        } catch (e: Throwable) {
            logError(e)
        }
    }


    data class AcuPrinter(
        val name: String, var host: String, var ipAddress: String = "", var port: Int = 0,
        var ethernet: Boolean = false, var product: String = "", var mac: String = "",
        var uuid: String = "", var rp: String = "", var cupsName: String = "",
        var found: Boolean = false, var onLine: Boolean = false, var isAttached: Boolean = false, var hasCups: Boolean = false
    )

    val printers = HashMap<String, AcuPrinter>()

    fun updatePrinterDiagnostics() {
        synchronized(printers) {
            diagnostics.print.clear()
            printers.forEach { deviceName, printer ->
                synchronized(diagnostics) {
                    val node = diagnostics.print["printers"].addElement()
                    node["name"] = printer.name
                    node["host"] = printer.host
                    node["ipAddress"] = printer.ipAddress
                    node["port"] = printer.port
                    node["ethernet"] = printer.ethernet
                    node["product"] = printer.product
                    node["mac"] = printer.mac
                    node["uuid"] = printer.uuid
                    node["rp"] = printer.rp
                    node["cupsName"] = printer.cupsName
                    node["found"] = printer.found
                    node["onLine"] = printer.onLine
                    node["isAttached"] = printer.isAttached
                    node["hasCups"] = printer.hasCups
                    node["default"] = printer == defaultPrinter
                }
            }
        }
        val services = PrintServiceLookup.lookupPrintServices(null, null)
        services.forEach {
            val node = diagnostics.print["services"].addElement()
            node.setValue(it.name)
        }
    }

    fun refreshSystemPrinterList() {
        val classes = PrintServiceLookup::class.java.declaredClasses
        for (i in classes.indices) {
            if ("javax.print.PrintServiceLookup\$Services" == classes[i].name) {
                sun.awt.AppContext.getAppContext().remove(classes[i])
                break
            }
        }
    }

    fun cupsNameFromHost(host: String): String {
        val lpstat = execStr("lpstat -v")
        val devices = lpstat.split("\n")
        for (device in devices) {
            if (device.startsWith("device for ")) {
                val line = device.dropLeft(11)
                val deviceName = line.substringBefore(":").trim()
                val uri = line.substringAfter(":").trim()
                if (uri.contains(host)) return deviceName
            }
        }
        return ""
    }

    var CUPS_PREFIX = "ptr"

    var nextCupsSuffix: Int = -1
        get() {
            if (field == -1) {
                var maxSuffix = -1
                val lpstat = execStr("lpstat -v")
                val devices = lpstat.split("\n")
                for (device in devices) {
                    if (device.startsWith("device for ")) {
                        val line = device.dropLeft(11)
                        val deviceName = line.substringBefore(":").trim()
                        if (deviceName.startsWith(CUPS_PREFIX)) {
                            val suffix = device.dropLeft(3).toIntDef(0)
                            if (suffix > maxSuffix) maxSuffix = suffix
                        }
                    }
                }
                field = maxSuffix + 1
            }
            return field
        }

    fun scanForPrinters() {
        val table = execStr("avahi-browse -crp _ipp._tcp")
        val entries = table.split("\n")
        for (entry in entries) {
            if (entry.startsWith("=")) {
                val fields = entry.split(";")
                if (fields[1].oneOf("ap0", "eth0")) {

                    val name = fields[3].unescape()
                    val host = fields[6]
                    val properties = fields[9].pairsToMap()
                    val pdl = properties.getDef("pdl", "").split(",")
                    val driverless =
                        pdl.contains("application/pdf") || pdl.contains("image/pwg-raster") || pdl.contains("image/urf") || pdl.contains("application/PCLm")
                    val ethernet = fields[1] == "eth0"
                    val isRealPrinter = properties.getDef("URF", "").neq("DM3")

                    val existingCups = cupsNameFromHost(host)

                    if (!printers.containsKey(name) && driverless && (isRealPrinter)) {
                        val printer = AcuPrinter(
                            name = name,
                            host = host,
                            ipAddress = fields[7],
                            port = fields[8].toIntDef(0),
                            ethernet = ethernet,
                            product = properties.getDef("product", "()").dropLeft(1).dropLast(1),
                            mac = properties.getDef("mac", ""),
                            uuid = properties.getDef("uuid", ""),
                            rp = properties.getDef("rp", ""),
                            cupsName = if (existingCups.isNotEmpty()) existingCups else "$CUPS_PREFIX${nextCupsSuffix++}",
                            hasCups = existingCups.isNotEmpty()
                        )
                        printers.put(name, printer)
                        debug("cups", "name=${name}, host=${host}, ipAddress=${printer.ipAddress}, port=${printer.port}")
                        properties.forEach { key, value ->
                            debug("cups", "  $key=$value")
                        }
                        if (haveMysql) {
                            Device.register(
                                type = 3,
                                serial = printer.host.replace(".local", ""),
                                model = printer.product,
                                tagRoot = "ptr",
                                macAddress = printer.mac,
                                ipAddress = printer.ipAddress,
                                info = printer.name,
                                idCompetition = Competition.current.id
                            )
                        }

                    }
                }
            }
        }
    }


    var defaultPrinter = AcuPrinter("", "")

    var printSpooler = AcuThread(threads, "printSpooler", 15, wait = 60) {
        if (defaultPrinter.name.isNotEmpty() && defaultPrinter.onLine && defaultPrinter.isAttached) {
            ReportQueue.despool()
        }
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    val routes: ArrayList<Route>
        get() {
            val _routes = ArrayList<Route>()
            val table = execStr("route -n")
            val entries = table.split("\n")
            for (entry in entries) {
                if (entry.isNotEmpty() && entry[0].isDigit()) {
                    _routes.add(
                        Route(
                            destination = entry.substring(0, 15).trim(),
                            gateway = entry.substring(16, 31).trim(),
                            genmask = entry.substring(32, 47).trim(),
                            flags = entry.substring(48, 53).trim(),
                            metric = entry.substring(54, 60).trim().toIntDef(-1),
                            iface = entry.substring(72).trim()
                        )
                    )
                }
            }

            return _routes
        }

    fun stations(iface: String, full: Boolean = false): Json {
        val result = Json()
        val table = execStr("iw dev $iface station dump")
        val entries = table.split("\n")
        var node = Json.nullNode()
        for (entry in entries) {
            if (entry.startsWith("Station")) {
                node = result.addElement()
                val mac = entry.substring(8, 25).trim()
                var device = ""
                if (iface == "mesh0") {
                    device = mesh0ToTag(mac)
                } else {
                    printers.forEach { _, printer ->
                        if (printer.mac eq mac) device = "${printer.cupsName} (${printer.product})"
                    }
                    if (device.isEmpty()) device = Device.acuTagFromAccessPointMac(mac)

                }
                node["device"] = device
                node["mac"] = mac
            } else {
                val label = entry.substringBefore(":").trim()
                val data = entry.substringAfter(":").trim()
                if (full || label.oneOf("inactive time", "expected throughput", "rx bytes", "tx bytes", "tx retries", "tx failed", "mesh plink", "signal avg")) {
                    val int = data.toIntDef(Integer.MIN_VALUE)
                    if (int != Integer.MIN_VALUE) {
                        if (label.contains("bytes")) {
                            node[label.replace("bytes", "").trim()] = int.bytesToMegaBytes()
                        } else {
                            node[label] = int
                        }
                    } else {
                        node[label] = data
                    }
                }
            }
        }
        return result
    }

    fun batOriginator(): Json {
        val result = Json()
        val table = execStr("batctl o -H")
        val entries = table.split("\n")
        var node = Json.nullNode()
        for (entry in entries) {
            if (entry.length >= 58) {
                val asterisk = entry.substring(0, 3).trim()
                val originator = entry.substring(3, 20).trim()
                val lastSeen = entry.substring(20, 30).trim()
                val quality = entry.substring(34, 37).trim().toIntDef(0)
                val hop = entry.substring(39, 56).trim()
                val iFace = entry.substring(58, 58).trim()
                var acu = mesh0ToTag(originator)
                var nextHop = mesh0ToTag(hop)
                node = result.searchElement("acu", acu, create = false)
                if (node.isNull) {
                    node = result.addElement()
                    node["acu"] = acu
                    node["lastSeen"] = lastSeen
                }
                val hopNode = node["hops"].addElement()
                hopNode["next"] = nextHop
                hopNode["quality"] = quality
                hopNode["interface"] = iFace
            }
        }
        return result
    }

    val gatewayDevice: Gateway
        get() {
            when (gateway) {
                "eth0" -> return Gateway.ETHERNET
                "usb0" -> return Gateway.DONGLE
                "" -> return Gateway.NONE
                else -> return Gateway.REMOTE
            }
        }

    var gateway = "unknown"
        get() {
            if (field == "unknown") {
                for (route in routes) {
                    if (route.destination == "0.0.0.0" && route.genmask == "0.0.0.0" && route.flags == "UG") {
                        if (route.iface.oneOf("usb0", "eth0")) {
                            field = route.iface
                        } else {
                            field = route.gateway
                        }
                        return field
                    }
                }
                field = ""
            }
            return field
        }
        private set(value) {
            if (value != field) {
                debug("ACU", "Gateway is $value")
                // remove existing gateways
                for (route in routes) {
                    if (route.destination == "0.0.0.0" && route.genmask == "0.0.0.0" && route.flags == "UG") {
                        exec("route del default gw ${route.gateway}")
                    }
                }
                if (value.oneOf("usb0", "eth0")) {
                    exec("ifdown $value")
                    exec("ifup $value")
                } else if (value.isNotEmpty()) {
                    exec("route add default gw $value")
                }
                if (value.isEmpty()) {
                    stopService("openvpn@client")
                } else {
                    restartService("openvpn@client")
                }
                internetTest.reset()
            }
            field = "unknown"
        }

    val gatewayHost: String
        get() {
            if (gatewayDevice == Gateway.REMOTE) {
                return ipToAcu(gateway)
            }
            return "???"
        }


    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    var zte_url = ""
    val zte_header = "'Referer: http://192.168.0.1/index.html'"

    fun resetZte() {
        val url =
            "http://192.168.0.1/goform/goform_set_cmd_process?isTest=false&goformId=RESET_DATA_COUNTER&curr_total_month"
        execStr("curl --max-time 10 --header $zte_header '$url'", silent = false)
    }

    val samples = ArrayList<Int>()
    val maxSamples = 120
    var sampleTotal = 0
    var sampleAverage = 0

    val gatewaySignal: Int
        get() {
            if (internetTest.passed) {
                when (gateway) {
                    "eth0" -> return -1000
                    "usb0" -> return sampleAverage
                }
            }
            return 0
        }

    fun addSample(sample: Int) {
        //debug("hardware", "sample: $sample")
        if (samples.size >= maxSamples) {
            sampleTotal -= samples[0]
            samples.removeAt(0)
        }
        samples.add(sample)
        sampleTotal += sample
        sampleAverage = sampleTotal / samples.size
    }

    var lastRecorded = nullDate

    fun checkZte(empty: Boolean = false) {
        if (empty) {
            zteStatus = ""
            signalStrength = 0
            mobileConnection = ""
        } else {
            try {
                if (zte_url.isEmpty()) {
                    val commands = StringBuilder("modem_model")
                    commands.append("," + "msisdn")
                    commands.append("," + "imei")
                    commands.append("," + "system_uptime")
                    commands.append("," + "signalbar")
                    commands.append("," + "rscp")

                    commands.append("," + "network_type")
                    commands.append("," + "network_provider")
                    commands.append("," + "ppp_status")
                    commands.append("," + "modem_main_state")

                    commands.append("," + "realtime_tx_bytes")
                    commands.append("," + "realtime_rx_bytes")
                    commands.append("," + "realtime_tx_thrpt")
                    commands.append("," + "realtime_rx_thrpt")

                    commands.append("," + "monthly_rx_bytes")
                    commands.append("," + "monthly_tx_bytes")
                    commands.append("," + "monthly_time")
                    commands.append("," + "date_month")

                    commands.append("," + "rmcc")
                    commands.append("," + "rmnc")
                    commands.append("," + "lac_code")
                    commands.append("," + "cell_id")
                    zte_url = "http://192.168.0.1/goform/goform_get_cmd_process?cmd=${commands.toString()}&multi_data=1"
                }

                val status = execStr("curl --max-time 10 --header $zte_header '$zte_url'")

                if (status.startsWith("Error")) {
                    zteStatus = status
                    dongleReport = Json()
                    dongleReport["error"] = status
                    return
                }
                //debug("acu", "zte status: $status")
                dongleReport = Json(status)
                addSample(-10 * dongleReport["signalbar"].asInt)

                synchronized(diagnostics) {
                    diagnostics.dongleData["modem_model"] = dongleReport["modem_model"]
                    diagnostics.dongleData["telephoneNumber"] = dongleReport["msisdn"].asString
                    diagnostics.dongleData["imei"] = dongleReport["imei"]
                    diagnostics.dongleData["status.system_uptime"] = dongleReport["system_uptime"].asInt.secondsToTime()
                    diagnostics.dongleData["status.signalbar"] = dongleReport["signalbar"].asInt
                    diagnostics.dongleData["status.signal"] = dongleReport["rscp"].asInt
                    diagnostics.dongleData["status.signalAverage"] = sampleAverage
                    diagnostics.dongleData["status.network_type"] = dongleReport["network_type"]
                    diagnostics.dongleData["status.network_provider"] = dongleReport["network_provider"]
                    diagnostics.dongleData["status.ppp_status"] = dongleReport["ppp_status"]
                    diagnostics.dongleData["status.modem_main_state"] = dongleReport["modem_main_state"]

                    diagnostics.dongleData["traffic.session.total"] =
                        (dongleReport["realtime_tx_bytes"].asInt + dongleReport["realtime_rx_bytes"].asInt).bytesToMegaBytes()
                    diagnostics.dongleData["traffic.session.send"] =
                        dongleReport["realtime_tx_bytes"].asInt.bytesToMegaBytes()
                    diagnostics.dongleData["traffic.session.receive"] =
                        dongleReport["realtime_rx_bytes"].asInt.bytesToMegaBytes()
                    diagnostics.dongleData["traffic.session.sendBps"] = dongleReport["realtime_tx_thrpt"].asInt
                    diagnostics.dongleData["traffic.session.receiveBps"] = dongleReport["realtime_rx_thrpt"].asInt
                    diagnostics.dongleData["traffic.month.total"] =
                        (dongleReport["monthly_tx_bytes"].asInt + dongleReport["monthly_rx_bytes"].asInt).bytesToMegaBytes()
                    diagnostics.dongleData["traffic.month.send"] =
                        dongleReport["monthly_tx_bytes"].asInt.bytesToMegaBytes()
                    diagnostics.dongleData["traffic.month.receive"] =
                        dongleReport["monthly_rx_bytes"].asInt.bytesToMegaBytes()
                    diagnostics.dongleData["traffic.month.time"] = dongleReport["monthly_time"].asInt.secondsToTime()
                    diagnostics.dongleData["traffic.month.month"] = dongleReport["date_month"].asInt
                    diagnostics.dongleData["location.mmc"] = dongleReport["rmcc"].asInt
                    diagnostics.dongleData["location.mnc"] = dongleReport["rmnc"].asInt
                    diagnostics.dongleData["location.lac"] = dongleReport["lac_code"].asString.hexToInt()
                    diagnostics.dongleData["location.cellID"] = dongleReport["cell_id"].asString.hexToInt()
                }


                var bar = "     "
                when (dongleReport["signalbar"].asInt) {
                    1 -> bar = "*    "
                    2 -> bar = "**   "
                    3 -> bar = "***  "
                    4 -> bar = "**** "
                    5 -> bar = "*****"
                }
                if (!internetTest.passed) {
                    bar = bar.replace("*", "!")
                }
                var networkType = dongleReport["network_type"].asString
                var type = when {
                    networkType.contains("LIMITED") -> "LTD"
                    networkType.contains("GPRS") -> "2G"
                    networkType.contains("EDGE") -> "2.5G"
                    networkType.contains("3G") -> "3G"
                    networkType.contains("UMTS") -> "3G"
                    networkType.contains("HSDPA+") -> "3.75G"
                    networkType.contains("HSDPA") -> "3.5G"
                    networkType.contains("HSPA+") -> "3.75G"
                    networkType.contains("HSPA") -> "3.5G"
                    networkType.contains("LTE") -> "4G"
                    networkType.contains("4G") -> "4G"
                    else -> networkType
                }
                dongleHasSignal = type.oneOf("3G", "3.5G", "3.75G", "4G")
                dongleSim = dongleReport["modem_main_state"].asString.neq("modem_sim_undetected")
                zteStatus = "$bar $type ${dongleReport["network_provider"].asString}"
                signalStrength = dongleReport["signalbar"].asInt
                mobileConnection = "$type ${dongleReport["network_provider"].asString}"

                if (lastRecorded < now.addMinutes(-60) && samples.size > 20) {
                    MobileSignal.record(dongleReport, sampleAverage)
                    lastRecorded = now
                }

            } catch (e: Throwable) {
                println(dongleReport.toJson())
                println(e.stack)
                debug("zteStatus", e.message ?: "blank message")
                zteStatus = "(error)"
            }
        }
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////


    private var _masterHost = "#"

    var killAt = Long.MIN_VALUE

    val isKilling: Boolean
        get() = killAt != Long.MIN_VALUE

    val killSeconds: Int
        get() = ((killAt - cpuTime) / 1000).toInt()

    fun kill(seconds: Int = 120) {
        if (!isKilling) {
            killAt = cpuTime + seconds * 1000
        }
    }

    fun alert(type: String, seconds: Int = 60) {
        heartbeats.alert(type, alertSequence++)
    }

    fun reset() {
        Runtime.getRuntime().exit(200)
    }

    fun abortUpgrade() {
        control.refresh()
        control.graniteVersion = "0.0.0"
        control.post()
    }

    fun restartDb() {
        restartService("db@sandstone")
    }

    fun restartCups() {
        if (haveMysql) {
            dbExecute("UPDATE reportqueue SET timeLocked = 0 WHERE timePrinted = 0")
            dbExecute("DELETE FROM mutex WHERE keyword='printQueue'")
        }
        restartService("cups")
    }

    fun ethernetOff() {
        ethernetState = 1
        selectGateway()
    }

    fun ethernetOn() {
        ethernetState = 0
        selectGateway()
    }

    fun dongleOff() {
        dongleState = 1
        selectGateway()
    }

    fun dongleOn() {
        dongleState = 0
        selectGateway()
    }

    val ip: String
        get() = if (Global.isSimulatedAcu) getIpAddress("eth0") else getIpAddress("bat0")

    val vpn: String
        get() = getIpAddress("tun0")

    var ap0Mac: String = ""
        get() {
            if (field.isEmpty()) {
                field = if (Global.isSimulatedAcu) getMacAddress("eth0") else getMacAddress("ap0")
            }
            return field
        }

    var mesh0Mac: String = ""
        get() {
            if (field.isEmpty()) {
                field = if (Global.isSimulatedAcu) getMacAddress("eth0") else getMacAddress("mesh0")
            }
            return field
        }

    var bat0Mac: String = ""
        get() {
            if (field.isEmpty()) {
                field = if (Global.isSimulatedAcu) getMacAddress("eth0") else getMacAddress("bat0")
            }
            return field
        }

    private var _uuid: String = ""
    val uuid: String
        get() {
            if (_uuid == "" && mysqlTest.passed) {
                try {
                    val query = DbQuery("show global variables like 'server_uuid'", _connection = localConnection)
                    if (query.found()) {
                        _uuid = query.getString("value")
                    }
                } catch (e: Throwable) {
                    doNothing()
                }
            }
            return _uuid
        }


    var serverId = -1
        get() {
            if (field == -1 && mysqlTest.passed) {
                try {
                    val query = DbQuery("show global variables like 'server_id'", _connection = localConnection)
                    if (query.found()) {
                        field = query.getInt("value")
                    }
                } catch (e: Throwable) {
                    doNothing()
                }
            }
            return field
        }
        private set


    fun doInitialize() {
    }

    fun doFinalize() {
        running = false
    }

    fun ping(host: String, timeout: Int = 5): Boolean {
        val runtime = Runtime.getRuntime()
        val command = "ping -c 1 -W $timeout $host"
        val proc = runtime.exec(command)
        proc.waitFor()
        val exitValue = proc.exitValue()
        return exitValue == 0
    }

    fun hostToMac(host: String): String {
        val table = execStr("arp -e -a $host")
        val entries = table.split("\n")
        for (entry in entries) {
            if (!entry.startsWith("Address")) {
                return entry.substring(33, 50).trim()
            }
        }
        return ""
    }

    fun isAccessPointClient(mac: String): Boolean {
        val table = execStr("iw dev ap0 station dump")
        val entries = table.split("\n")
        for (entry in entries) {
            if (entry.startsWith("Station")) {
                if (entry.substring(8, 25).trim() == mac) return true
            }
        }
        return false
    }

    fun getAverageLoad(): Int {
        val load = fileToString("/proc/loadavg").split(" ")
        val mins5 = (load[1].toDoubleDef(0.0) / 4.0 * 100.0).toInt()
        return mins5
    }

    var localConnection: DbConnection? = null
        get() {
            if (field == null) {
                field = DbJdbcConnection(SandstoneLocal.builder)
            }
            return field ?: throw Wobbly("unable to localConnection")
        }


    fun initializeMasterHost() {
        debug("acu", "initializeMasterHost")
        val query = DbQuery("show slave status", _connection = localConnection)
        if (query.found()) {
            _masterHost = query.getString("Master_Host")
        }
    }

    fun ipToAcu(ip: String): String {
        val elements = ip.split(".")
        val number = elements[3].toIntDef(256)
        return "acu" + "%03d".format(number)
    }

    fun acuToMeshIp(acu: String): String {
        val number = acu.replace("acu", "").toIntDef()
        return meshPattern.replace("xxx", number.toString())
    }

    fun setMasterRing(controller: String, instance: Int, route: String, manual: Boolean = false): String {
        val routeAsArray = route.listToStringArray()
        return setMasterRing(controller, instance, routeAsArray, manual)
    }

    fun setMasterRing(controller: String, instance: Int, route: ArrayList<String>, manual: Boolean = false): String {
        if (controller != clusterRingController || instance == 0 || instance > clusterRingInstance || manual != clusterRingManual) {
            if (!clusterRingManual || manual) {
                clusterRingManual = manual
                clusterRingController = controller
                clusterRingInstance = instance
                clusterRing.duplicate(route)
                val me = getHostname()
                for (i in 0 until route.size) {
                    if (route[i] == me) {
                        val previous = if (i == 0) route.size - 1 else i - 1
                        val next = if (i == route.size - 1) 0 else i + 1
                        val tagPrevious = route[previous]
                        val tagNext = route[next]
                        setMasters(tagPrevious, tagNext)
                        return "$tagPrevious, $tagNext"
                    }
                }
            }
        }
        return "n/a"
    }

    fun addMaster(tag: String) {
        val hostIp = acuToMeshIp(tag)
        if (hostIp neq hardware.ip) {
            debug("addMaster", "$hostIp (I am ${hardware.ip})")
            localConnection?.execute(
                """
                CHANGE MASTER TO MASTER_HOST='$hostIp',
                MASTER_PORT=3340,
                MASTER_USER = 'slave',
                MASTER_PASSWORD = 'replicate',
                MASTER_AUTO_POSITION = 1
                FOR CHANNEL '$tag'
            """
            )
            localConnection?.execute("START SLAVE FOR CHANNEL '$tag'")
            masters.add(tag)
        }
    }

    fun dropMaster(tag: String) {
        localConnection?.execute("STOP SLAVE FOR CHANNEL '$tag'")
        localConnection?.execute("RESET SLAVE ALL FOR CHANNEL '$tag'")
        masters.remove(tag)
    }

    fun setMasters(vararg acuTags: String) {
        masters.clear()
        dbQuery("SHOW SLAVE STATUS") { masters.add(getString("Channel_Name")) }
        val oldMasters = ArrayList<String>(masters)
        for (tag in acuTags) {
            if (!masters.contains(tag)) {
                addMaster(tag)
            } else {
                oldMasters.remove(tag)
            }
        }
        for (tag in oldMasters) {
            if (tag.neq("main") || acuId < 240) dropMaster(tag)
        }
    }

    var loadDate = nullDate
    var load: Int = 0
        get() {
            if (loadDate < now.addSeconds(-60)) {
                loadDate = now
                field = getAverageLoad()
            }
            return field
        }

    var idSite: Int = -1
        get() {
            if (field < 0 && haveMysql) {
                try {
                    debug("acu", "lookup idSite")
                    val query = DbQuery("select idSite from control where idControl=1", _connection = localConnection)
                    if (query.found()) {
                        field = query.getInt("idSite")
                        debug("acu", "idSite = $field")
                    }
                } catch (e: Throwable) {
                    field = -1
                }
            }
            return field
        }

    private fun haveCarrier(device: String): Boolean {
        try {
            return fileToString("/sys/class/net/$device/carrier") == "1"
        } catch (e: Throwable) {
            return false
        }
    }

    private fun haveInterface(device: String): Boolean {
        return File("/sys/class/net/$device").exists()
    }

    private fun haveDevice(device: String): Boolean {
        return File("/sys/class/ieee80211/$device").exists()
    }

    private fun serviceActive(service: String): Boolean {
        val query = execStr("systemctl is-active $service")
        return query == "active"
    }

    private fun serviceRunning(service: String): Boolean {
        val query = execStr("systemctl status $service")
        return query.contains("active (running)")
    }

    /*
    private fun interfaceIp(interfaceName: String): String {
        val command = "ip -oneline -4 addr show $interfaceName"
        val query = execStr(command)
        val ip = query.rightOf("inet").leftOf("/").trim()
        debug("acu", "dhcpdTest: $command -> $ip ($query)")
        return query.rightOf("inet").leftOf("/").trim()
    }
    
    fun setInterfaceIp(interfaceName: String, ipAddress: String) {
        debug("acu", "dhcpdTest: setInterfaceIp -> $interfaceName to $ipAddress")
        if (interfaceIp(interfaceName).neq(ipAddress)) {
            debug("acu", "dhcpdTest: setInterfaceIp (2) -> $interfaceName to $ipAddress")
            exec("ifconfig $interfaceName down")
            exec("ifconfig $interfaceName $ipAddress")
            exec("ifconfig $interfaceName up")
        }
    }    
     */

    fun shutDown(minutes: Int): Int {
        debug("acu", "shutDown")
        shuttingDown = true
        doFinalize()
        return exec("shutdown -h $minutes")
    }

    fun reboot(minutes: Int): Int {
        debug("acu", "reboot")
        shuttingDown = true
        doFinalize()
        return exec("shutdown -r $minutes")
    }

    fun setSystemTime(timestamp: Long, source: Int): Int {
        timeState = source
        val seconds = (timestamp + 500) / 1000
        return exec("date --set @$seconds", wait = false)
    }

    fun quit() {
        debug("acu", "quit")
        doFinalize()
        System.exit(0)
    }

    private var registered = false

    fun registerAcu() {
        if (!registered) {
            val serial = fileToString("/proc/device-tree/serial-number")
            val model = fileToString("/proc/device-tree/model")
            val info = fileToString("/proc/version")
            Device.register(
                1, serial, model, "acu", ap0Mac, ip, info, macAddressMesh = mesh0Mac,
                sdCardSet = idSite, serverId = serverId, databaseUuid = uuid, idCompetition = Competition.current.id,
                macAddressBat = bat0Mac
            )
            registered = true
        }
    }

    fun apiPing(): Boolean {
        val status = execStr("curl --max-time 10 api.egility.org/v1.0/ping")
        if (status.startsWith("Error")) {
            debug("apiPing", "error: - $status")
            return false
        }
        try {
            val json = Json(status)
            return json["kind"].asString == "ping"
        } catch (e: Throwable) {
            debug("apiPing", "error: " + (e.message ?: "") + " - $status")
            return false
        }
    }


    data class Route(var destination: String, var gateway: String, var genmask: String, var flags: String, var metric: Int, var iface: String)


    val menuOptions: ArrayList<String>
        get() {
//            val result = arrayOfString("Back", "Off", "Reboot", "Kill System", "Reset", "Fix DB", "Fix Print", "WPS", "Abort Upgrade")
            val result = arrayOfString("Back", "Off", "Reboot", "Reset", "Fix DB", "Fix Print", "WPS", "Abort Upgrade")
            if (hardware.ethernetState > 0) {
                result.add("Ethernet On")
            } else if (hardware.gatewayDevice == Gateway.ETHERNET) {
                result.add("Ethernet Off")
            }
            if (hardware.dongleState > 0) {
                result.add("Dongle On")
            } else if (hardware.gatewayDevice == Gateway.DONGLE) {
                result.add("Dongle Off")
            }
            return result
        }

    fun handleMenu(option: String) {
        when (option) {
            "Off" -> hardware.shutDown(0)
            "Reboot" -> hardware.reboot(0)
            "Kill System" -> hardware.kill(120)
            "Reset" -> hardware.reset()
            "Fix DB" -> hardware.restartDb()
            "Fix Print" -> hardware.restartCups()
            "Ethernet On" -> hardware.ethernetOn()
            "Ethernet Off" -> hardware.ethernetOff()
            "Dongle On" -> hardware.dongleOn()
            "Dongle Off" -> hardware.dongleOff()
            "WPS" -> exec("hostapd_cli wps_pbc")
            "Abort Upgrade" -> hardware.abortUpgrade()
        }
    }

}

object acuStatus {
    private var id = getHostname().dropLeft(3)

    private val gatewayStatus: String
        get() {
            when (hardware.gatewayDevice) {
                Gateway.ETHERNET -> return if (hardware.internetTest.passed) "XG" else "xg"
                Gateway.DONGLE -> return if (hardware.internetTest.passed) "GW" else "gw"
                Gateway.REMOTE -> {
                    if (hardware.ethernetState > 0) {
                        return "E${hardware.ethernetState}"
                    } else if (hardware.dongleState > 0) {
                        return "D${hardware.dongleState}"
                    }
                    return if (hardware.internetTest.passed) "RT" else "rt"
                }
                Gateway.NONE -> {
                    if (hardware.ethernetState > 0) {
                        return "E${hardware.ethernetState}"
                    } else if (hardware.dongleState > 0) {
                        return "D${hardware.dongleState}"
                    }
                    return "--"
                }
            }
        }

    val siteCode: String
        get() {
            return "%02d".format(hardware.idSite.rem(100))
        }

    val load: String
        get() {
            if (hardware.load < 100) {
                return ("%d".format(hardware.load) + "%").padStart(3, ' ')
            }
            return "%01.1f".format(hardware.load.toDouble() / 100.0)
        }

    val channelText: String
        get() = "Ch" + "%01d".format(apChannel.toIntDef(0))

    val clusterCount: String
        get() = "%02d".format(hardware.clusterRing.size)


    val printerCode: String
        get() = when {
            hardware.defaultPrinter.name.isEmpty() -> "X"
            hardware.defaultPrinter.onLine && hardware.defaultPrinter.isAttached -> "P"
            !hardware.defaultPrinter.onLine && hardware.defaultPrinter.isAttached -> "N"
            else -> "S"
        }

    val statusLine: String
        get() {
            val cluster = if (clusterRingManual) "Man" else "N$clusterCount"
            if (hardware.isKilling) {
                val message = if (hardware.killSeconds > 0) "KILL ${hardware.killSeconds}" else "KILL NOW"
                return "$id $cluster $message"
            } else {
                return "$id $cluster $channelText $siteCode $printerCode"
            }
        }

    val mobileLine: String
        get() {
            val connected = if (!hardware.internetTest.passed) " !!!" else ""
            val state = if (hardware.ethernetState > 0)
                " E${hardware.ethernetState}"
            else if (!dongleSim)
                " SIM?"
            else if (hardware.dongleState > 0)
                " D${hardware.dongleState}"
            else
                ""

            when (hardware.gatewayDevice) {
                Gateway.ETHERNET -> return "Ethernet" + connected
                Gateway.DONGLE -> return if (!dongleSim) "SIM?" else if (hardware.dongleTest.passed) hardware.zteStatus else "No Dongle"
                Gateway.REMOTE -> return "Via ${hardware.gatewayHost}" + connected + state
                Gateway.NONE -> return "No Internet" + state
            }
        }

    val errorCodes: String
        get() {
            return hardware.errorCodes
        }

    val hasError: Boolean
        get() {
            return hardware.errorCodes != "[OK]"
        }

    val bootCount: String
        get() = "%02d".format(getBootCount().fixRange(-9, 99))


}

object diagnostics : Json() {

    var hostname: String by PropertyJsonString("hostname")

    val system: JsonNode by PropertyJsonNode("system")
    var date: String by PropertyJsonString("system.date")
    var time: String by PropertyJsonString("system.time")
    var bootCount: Int by PropertyJsonInt("system.bootCount")
    var healthCount: Int by PropertyJsonInt("system.healthCount")
    var uptime: Int by PropertyJsonInt("system.uptime")
    var killing: Boolean by PropertyJsonBoolean("system.killing")
    var lastAlive: Long by PropertyJsonLong("system.lastAlive")
    var panicElapsed: String by PropertyJsonString("system.panic.elapsed")
    var panicMessage: String by PropertyJsonString("system.panic.message")

    val display: JsonNode by PropertyJsonNode("display")

    val runtime: JsonNode by PropertyJsonNode("runtime")
    var totalMemory: String by PropertyJsonString("runtime.totalMemory")
    var freeMemory: String by PropertyJsonString("runtime.freeMemory")
    var usedMemory: String by PropertyJsonString("runtime.usedMemory")
    var threads: Int by PropertyJsonInt("runtime.threads")

    val cluster: JsonNode by PropertyJsonNode("cluster")

    val network: JsonNode by PropertyJsonNode("network")
    var mac: String by PropertyJsonString("network.mac")
    var address: String by PropertyJsonString("network.address")
    var haveInternet: Boolean by PropertyJsonBoolean("network.haveInternet")
    var gateway: String by PropertyJsonString("network.gateway")
    var vpn: String by PropertyJsonString("network.vpn")

    val dongle: JsonNode by PropertyJsonNode("network.dongle")
    var signalStrength: Int by PropertyJsonInt("network.dongle.signalStrength")
    var mobileConnection: String by PropertyJsonString("network.dongle.mobileConnection")
    var signalAverage: Int by PropertyJsonInt("network.dongle.data.status.signalAverage")
    val dongleData: JsonNode by PropertyJsonNode("network.dongle.data")
    val dongleReport: JsonNode by PropertyJsonNode("network.dongle.data.report")
    val dongleStatus: JsonNode by PropertyJsonNode("network.dongle.data.status")
    val dongleSession: JsonNode by PropertyJsonNode("network.dongle.data.traffic.session")
    val dongleMonth: JsonNode by PropertyJsonNode("network.dongle.data.traffic.month")
    val dongleLocation: JsonNode by PropertyJsonNode("network.dongle.data.location")

    val apStations: JsonNode by PropertyJsonNode("network.apStations")
    val meshStations: JsonNode by PropertyJsonNode("network.meshStations")
    val meshPaths: JsonNode by PropertyJsonNode("network.meshPaths")
    val arp: JsonNode by PropertyJsonNode("network.arp")


    val originator: JsonNode by PropertyJsonNode("network.bat.originator")


    val database: JsonNode by PropertyJsonNode("database")
    var databaseState: String by PropertyJsonString("database.state")
    var haveMysql: Boolean by PropertyJsonBoolean("database.haveMysql")
    var serverId: Int by PropertyJsonInt("database.serverId")
    var databaseUuid: String by PropertyJsonString("database.uuid")
    var idSite: Int by PropertyJsonInt("database.idSite")
    var databaseIssue: Boolean by PropertyJsonBoolean("database.databaseIssue")
    var databaseErrorCode: Int by PropertyJsonInt("database.errorCode")
    var databaseErrorMessage: String by PropertyJsonString("database.errorMessage")
    val executedGtidSet: JsonNode by PropertyJsonNode("database.executedGtidSet")
    val masters: JsonNode by PropertyJsonNode("database.masters")
    val retrievedGtidSet: JsonNode by PropertyJsonNode("database.retrievedGtidSet")
    val slaves: JsonNode by PropertyJsonNode("database.slaves")
    val print: JsonNode by PropertyJsonNode("print")
    val printJobs: JsonNode by PropertyJsonNode("print.jobs")
    val options: JsonNode by PropertyJsonNode("options")

    init {
        hostname = getHostname()
        display.clear()
        system.clear()
        runtime.clear()
        database.clear()
        cluster.clear()
        network.clear()
        print.clear()
        options.clear()

        bootCount = getBootCount()
        date = ""
        time = ""
    }

    fun populate(full: Boolean = false) {
        date = now.dateText
        time = now.timeSeconds
        uptime = getUptime()
        killing = hardware.isKilling

        display[0] = hardware.errorCodes
        display[1] = acuStatus.mobileLine
        display[2] = acuStatus.statusLine


        val runtime = Runtime.getRuntime()
        totalMemory = runtime.totalMemory().bytesToMegaBytes()
        freeMemory = runtime.freeMemory().bytesToMegaBytes()
        usedMemory = (runtime.totalMemory() - runtime.freeMemory()).bytesToMegaBytes()
        threads = Thread.activeCount()

        val panic = hardware.exception
        if (hardware.haveException && panic != null) {
            panicElapsed = ((now.time - hardware.exceptionTime.time) / 1000).secondsToTime()
            panicMessage = panic.message ?: "No Error Message"
            system.delimitedStringToArray(panic.stack, "panic.stack", "\n\tat ")
        }

        mac = hardware.ap0Mac
        address = hardware.ip
        haveInternet = hardware.internetTest.passed
        gateway = hardware.gateway
        vpn = hardware.vpn

        signalStrength = hardware.signalStrength
        mobileConnection = hardware.mobileConnection

        synchronized(diagnostics) {
            if (full && !hardware.dongleReport.isNull) {
                diagnostics.dongleReport.setValue(hardware.dongleReport)
            }

            if (hardware.meshTest.passed) {
                diagnostics.originator.setValue(hardware.batOriginator())
                diagnostics.meshStations.setValue(hardware.stations("mesh0", full = full))
            } else {
                diagnostics.meshStations.clear()
            }
            if (hardware.accessPointTest.passed) {
                diagnostics.apStations.setValue(hardware.stations("ap0", full = full))
            } else {
                diagnostics.apStations.clear()
            }
            hardware.updatePrinterDiagnostics()
        }

        val menuOptions = ArrayList<String>()
        menuOptions.addAll(hardware.menuOptions)
        options.clear()
        for (menu in menuOptions) {
            if (!menu.oneOf("Back", "Kill System")) {
                options.addElement().setValue(menu)
            }
        }
        doDatabaseReport()
        hardware.getPrintJobs(printJobs)
    }

    fun generateJson(keyword: String, full: Boolean = false): Json {
        populate(full)
        val result = Json()
        synchronized(this) {
            result["OK"] = true
            result["kind"] = keyword
            result["version"] = "v1.0"
            result["data"] = this
        }
        return result
    }

}
