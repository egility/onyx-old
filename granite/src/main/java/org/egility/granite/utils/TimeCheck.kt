/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.granite.utils

import android.os.SystemClock

private class TimeCheck {

    private val client = SntpClient()

    private fun updateSystemClock() {
        updateSystemClockThread().start()
    }

    private inner class updateSystemClockThread : Thread() {
        override fun run() {
            try {
                if (client.requestTime(sntpHost, 10 * 1000)) {
                    adjustment = System.currentTimeMillis() - (client.ntpTime + SystemClock.elapsedRealtime() - client.ntpTimeReference)
                    lastCheck = System.currentTimeMillis()
                }
            } catch (e: Throwable) {
                /* do nothing */
            }

        }

    }

    companion object {

        private var adjustment: Long = 0
        private var lastCheck: Long = 0
        private var sntpHost = ""
            set(value) {
                sntpHost = value
                timeCheck.updateSystemClock()
            }

        private val timeCheck = TimeCheck()

    }
}
