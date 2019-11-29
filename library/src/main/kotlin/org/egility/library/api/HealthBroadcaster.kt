/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.library.api

import org.egility.library.general.hardware
import org.egility.library.general.cpuTime
import org.egility.library.general.panic


/**
 * Created by mbrickman on 05/08/15.
 */
class HealthBroadcaster : Thread("BroadcastServer") {

    private var more = true

    override fun run() {
        while (more) {
            try {
                if (hardware.isKilling && hardware.killAt < cpuTime) {
                    hardware.shutDown(0)
                    return
                }
                Api.broadcastHealth()
            } catch (e: Throwable) {
                /* ignore */
            }

            try {
                Thread.sleep((ApiUtils.BROADCAST_SLEEP_SECONDS * 1000).toLong())
            } catch (e: InterruptedException) {
                panic(e)
                more = false
            }

        }
    }
}