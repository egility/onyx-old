/*
 * Copyright (c) Mike Brickman 2014-2017
 */

/*
 * Copyright (c) Mike Brickman 2014-2016
 */

/*
 * Copyright (c) Mike Brickman 2014-2016
 */

/*
 * Copyright (c) Mike Brickman 2014-2016
 */

/*
 * Copyright (c) Mike Brickman 2014-1015.
 */

package org.egility.flint

import org.egility.linux.tools.NativeServices
import org.egility.library.api.ApiServer
import org.egility.library.general.*
import java.util.*

/**
 * Created by mbrickman on 13/07/15.
 */
fun main(args: Array<String>) {
    argsToMap(args)

    println("Flint - 2.14 Build 174 (${Date().zone})")

    /*

    1.2 Build 64 - CSJ
    1.2 Build 66 - Tuffley - champ classes
    1.2 Build 68 - Calling sheet & Personal running order print facility
    1.2 Build 70 - Fix to running order print to ignore transferred classes
    1.2 Build 72 - Extra class templates for Pembroke
    1.4 Build 74 - Peer networking
    1.4 Build 76 - Peer networking Refined, A4E stuff
    1.4 Build 78 - ACU045 supported
    1.4 Build 80 - Print Queue implemented
    1.4 Build 82 - Mutex fix, Cluster networking now only option, new status line on display
    1.6 Build 86 - IntelliJ 2018
    1.6 Build 88 - Sync account from Plaza
    1.6 Build 90 - Tidied competitor, dog & competition tables
    1.6 Build 92 - Tidied control table
    1.6 Build 94 - Revised end of day reports
    1.6 Build 96 - "Abort Upgrade" added (9/2/2019)
    1.8 Build 98 - Flint automatically registers with web (12/2/2019)
    1.8 Build 100 - Device.acuTagFromMac now checks mesh and ap address. Printers registered to device table
    1.8 Build 102 - Switched to IP port 9000 to overcome Kindle Fire block
    1.8 Build 104 - Support for KS training challenge
    1.8 Build 106 - Added self healing replication & ReplicationFault file (Lydiard 31/3/19) + fixed issues with EOF reports.
    1.8 Build 108 - Dongles now reestablish link if signal returns.
    1.8 Build 110 - Sorted problem where ACU comes up without Mesh (ie WiFi missing) - especially if cluster master.
    1.8 Build 112 - Complimentary Credits report fixed..
    1.10 Build 118 - http threads now have own mysql connection (14/4/2019)
    1.10 Build 120 - Adjustments to awards (17/4/2019)
    1.12 Build 122 - FAB shows (2/5/2019)
    1.14 Build 124 - FAB Pole fault
    1.14 Build 126 - FAB Allsorts Classes
    1.14 Build 128 - FAB Placings based on actual runners
    1.14 Build 130 - FAB Gamblers
    1.14 Build 132 - Hoolihounds Grampion Classes tested and debugged (6/6/2019)
    1.16 Build 134 - CSJ(6/6/2019)
    1.18 Build 136 - dhcp test added to ACU hardware (21/6/19)
    1.20 Build 140 - made print processing more resilient (27/6/19)
    1.22 Build 142 - New scime/calling sheets (4/7/19)
    1.30 Build 144 - Support for built in WiFi Adapter
    1.32 Build 146 - Disable power management on built in adapter
    1.34 Build 148 - Revert to 2.4GHz for mesh
    1.36 Build 150 - Better Replication fault reporting
    1.38 Build 152 - Fix for UKA gamblers

    2.00 Build 160 - Support for BATMAN Advanced
    2.02 Build 162 - Provisional build for Haw Bridge
    2.04 Build 164 - Build for Haw Bridge / WKC
    2.06 Build 166 - Fix for brcmfmac issue
    2.08 Build 168 - Dartmoor / Chatsford
    2.10 Build 170 - Small fix for brcmfmac bug monitoring Chatsworth / Waverunners. Time setting on startup via peer
    2.12 Build 172 - removed mutex check on ReportQueue.dequeue() as causing replication problems
    2.14 Build 174 - Tweaked FAB awards rule
     */

    setDebugExcludeClasses("exec", "execStr")

    if (args.contains("display-down")) {
        DisplayotronHat.term()
    } else if (args.contains("display-boot")) {
        DisplayotronHat.boot()
    } else {



        Runtime.getRuntime().addShutdownHook(object : Thread() {
            override fun run() {
                if (hardware.shuttingDown) {
                    DisplayotronHat.term()
                } else {
                    DisplayotronHat.term()
                }
            }
        })

        NativeServices.initialize(true)

        Thread.setDefaultUncaughtExceptionHandler(UncaughtHandler())

        Global.acuStatus = "Initializing..."

        DisplayotronHat.startThread()

        if (args.contains("development")) {
            Global.deviceType = DeviceType.SIMULATED_ACU
        } else {
            Global.deviceType = DeviceType.ACU
        }

        debug("API", "library_native.api server starting...")

        if (Global.isAcu) {
            hardware.doInitialize()
            ApiServer.initialize()
        }
    }

}

private class UncaughtHandler() : Thread.UncaughtExceptionHandler {

    override fun uncaughtException(thread: Thread, e: Throwable) {
        debug("API Server", "Uncaught Exception")
        panic(e)
    }

}


