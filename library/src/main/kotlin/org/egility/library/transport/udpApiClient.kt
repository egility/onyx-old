/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.library.transport

import org.egility.library.api.ApiUtils
import org.egility.library.general.Global
import org.egility.library.general.Json
import org.egility.library.general.Wobbly
import org.egility.library.general.debug
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

/**
 * Created by mbrickman on 15/08/15.
 */
class udpApiClient(private val request: Json, responseNeeded: Boolean) : Thread("BroadcastServer") {

    private var broadcastIp: InetAddress? = null
    private val socket: DatagramSocket? = null
    var response: Json? = null
        private set
    private var responseNeeded = false

    init {
        this.responseNeeded = responseNeeded
    }

    var _throwable: Throwable? = null

    override fun start() {
        super.start()
        val throwable = _throwable
        if (throwable != null) {
            throw throwable
        }
    }

    override fun run() {
        try {
            broadcastIp = InetAddress.getByName(ApiUtils.BROADCAST_ADDRESS)
            val socket = DatagramSocket()
            val requestText = request.toJson(compact = true)

            var packet = DatagramPacket(requestText.toByteArray(), requestText.toByteArray().size, broadcastIp, ApiUtils.BROADCAST_PORT)

            var tries = 0
            var done = false

            if (!Global.isAcu || responseNeeded) {
                debug("udpClient", "${if (responseNeeded) "Request" else "Broadcast"} ($broadcastIp:${ApiUtils.BROADCAST_PORT}) - $requestText")
            }

            while (!done) {
                try {
                    socket.send(packet)
                    done = true
                } catch (e: Throwable) {
                    if (++tries >= 20) {
                        throw Wobbly("Too many attempts to connect to udp client socket", cause = e)
                    } else {
                        Thread.sleep(2000)
                        debug("udpClient", "Retry (${e.message})")
                    }
                }
            }


//            debug("udpClient", "Success") -- too many
            if (responseNeeded) {
                val bytes = ByteArray(25600)
                packet = DatagramPacket(bytes, bytes.size)
                socket.receive(packet)
                val responseText = String(bytes, 0, bytes.size).trim { it <= ' ' }
                response = Json(responseText)
            }
        } catch (e: Throwable) {
            debug("udpClient", "ERROR(2): ${e.message}")
            _throwable = e
        }

    }

    companion object {


        fun getJson(request: Json): Json {
            val client = udpApiClient(request, true)
            client.start()
            client.join(20000)
            return client.response ?: throw Wobbly(Wobbly.Event.UDP_NULL_RESPONSE)
        }

        fun sendJson(request: Json) {
            val client = udpApiClient(request, false)
            client.start()
            client.join(20000)
        }
    }
}
