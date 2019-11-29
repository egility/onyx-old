/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.library.api

/*
 * Copyright (c) Mike Brickman 2014-1015.
 */

import org.egility.library.transport.UdpExchange

/**
 * Created by mbrickman on 14/07/15.
 */
interface ApiFunction {

    fun serveHttp(apiExchange: ApiExchange)

    fun serveUdp(udpExchange: UdpExchange)

}
