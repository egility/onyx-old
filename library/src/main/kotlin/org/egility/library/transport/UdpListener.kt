/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.library.transport

/**
 * Created by mbrickman on 06/11/15.
 */
interface UdpListener {

    fun handleUdp(udpExchange: UdpExchange)
}
