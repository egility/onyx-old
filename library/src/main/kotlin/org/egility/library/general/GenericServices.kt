/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.library.general

import java.util.*


/**
 * Created by mbrickman on 24/11/15.
 */

class GenericServices: Services {
    
    override val bootTime: Date
        get() = nullDate

    override fun msgYesNo(title: String, message: String, body: (Boolean) -> Unit) {
        throw UnsupportedOperationException()
    }

    override fun panic(throwable: Throwable) {
        throwable.printStackTrace()
        System.exit(1)
    }

    override fun log(message: String) {
        println(message)
    }

    override fun popUp(title: String, message: String) {
        throw UnsupportedOperationException()
    }

    override fun checkNetwork() {
        throw UnsupportedOperationException()
    }

    override val acuHostname: String
        get() = ""

    override fun generateReport(reportRequest: Json, canSpool: Boolean): String {
        throw UnsupportedOperationException()
    }

}