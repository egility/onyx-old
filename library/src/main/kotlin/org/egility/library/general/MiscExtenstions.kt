/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.library.general

import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.util.*


/**
 * Created by mbrickman on 15/02/16.
 */

fun Long.msecToSec(decimals: Int = 1): String {
    val formatString="%.${decimals}f"
    return formatString.format(this.toDouble() / 1000.0)

}

val Throwable.stack: String
get(){
    val stackRaw = StringWriter()
    this.printStackTrace(PrintWriter(stackRaw))
    return stackRaw.toString()
}

fun Collection<*>.asCommaList(): String {
    val result = StringBuilder("")
    for (item in this) {
        result.csvAppend(item.toString())
    }
    return result.toString()
}

fun Collection<*>.asQuotedList(): String {
    val result = StringBuilder("")
    for (item in this) {
        result.csvAppend(item.toString().quotedSingle)
    }
    return result.toString()
}

fun File.modified(): Date {
    return Date(this.lastModified())
}

fun File.cached(cacheLimit: Int = 60): Boolean {
    return exists() && modified() > now.addMinutes(-cacheLimit)
}