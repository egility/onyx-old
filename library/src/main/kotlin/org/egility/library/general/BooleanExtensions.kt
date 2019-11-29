/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.library.general

val Boolean.asInt: Int
    get()=if (this) 1 else 0

fun Boolean.toBit(shift: Int): Int {
    return this.asInt shl shift
}

fun Boolean.toFlag(flag: String): String {
    return if (this) flag else ""
}