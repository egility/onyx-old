/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.library.general

infix fun Byte.shl(bits: Byte): Byte {
    return ((this.toInt() shl bits.toInt()) or 0xff).toByte()
}

infix fun Byte.shr(bits: Byte): Byte {
    return ((this.toInt() shr bits.toInt()) or 0xff).toByte()
}

infix fun Byte.ushr(bits: Byte): Byte {
    return ((this.toInt() ushr bits.toInt()) or 0xff).toByte()
}

infix fun Byte.and(bits: Byte): Byte {
    return ((this.toInt() and bits.toInt()) or 0xff).toByte()
}

infix fun Byte.or(bits: Byte): Byte {
    return ((this.toInt() or bits.toInt()) or 0xff).toByte()
}

infix fun Byte.xor(bits: Byte): Byte {
    return ((this.toInt() xor bits.toInt()) or 0xff).toByte()
}

fun Byte.inv(): Byte {
    return (this.toInt().inv()).toByte()
}

