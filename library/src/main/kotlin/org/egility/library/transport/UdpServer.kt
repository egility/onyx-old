/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.library.transport

import org.egility.library.general.Json
import org.egility.library.general.debug
import org.egility.library.general.panic
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.nio.channels.ClosedByInterruptException

/**
 * Created by mbrickman on 05/08/15.
 */

class UdpServer(val udpPort: Int, private val udpListener: UdpListener) : Thread("UdpServer") {

    override fun run() {
        var socket = DatagramSocket(udpPort);
        while (!isInterrupted) {
            try {
                val bytes = ByteArray(25600)
                val packet = DatagramPacket(bytes, bytes.size)
                socket.receive(packet)
                val request = String(bytes, 0, bytes.size).trim { it <= ' ' }
                var json = Json.undefined()
                try {
                    var json = Json(request)
                    val udpExchange = UdpExchange(json)
                    udpListener.handleUdp(udpExchange)
                    val response = udpExchange._response
                    if (response != null) {
                        val responseText = response.toJson(compact = true)
                        debug("UdpServer", "responseText: $responseText")
                        socket.send(DatagramPacket(responseText.toByteArray(), responseText.toByteArray().size, packet.address, packet.port))
                    }
                } catch (e: Throwable) {
                    debug("UdpServer", "Badly formed request (${packet.address.canonicalHostName} - ${e.message}) length = ${request.length}")
                }
                try {
                    Thread.sleep(100)
                } catch (ex: InterruptedException) {
                    // Ignore
                }

            } catch (e: ClosedByInterruptException) {
                interrupt()
            } catch (e: Throwable) {
                if (!isInterrupted) {
                    panic(e)
                }
            }

        }
        if (!socket.isClosed) {
            socket.close()
        }
    }

    companion object {

        fun create(udpPort: Int, udpListener: UdpListener): UdpServer {
            return UdpServer(udpPort, udpListener)
        }
    }

}