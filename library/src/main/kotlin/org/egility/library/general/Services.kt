/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.library.general

import java.util.*


/**
 * Created by mbrickman on 13/03/15.
 */
interface Services {
    fun panic(throwable: Throwable)

    fun log(message: String)

    fun popUp(title: String, message: String)
    fun msgYesNo(title: String, message: String, body: (Boolean) -> Unit)

    fun checkNetwork()

    val acuHostname: String

    fun generateReport(reportRequest: Json, canSpool: Boolean = true): String
    
    val bootTime: Date

}
