/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.library.transport

import org.egility.library.general.Json


/**
 * Created by mbrickman on 06/11/15.
 */
class UdpExchange(var request: Json) {

    var _response: Json? = null
        private set

    fun respond(response: Json?) {
        this._response = response
    }
}
