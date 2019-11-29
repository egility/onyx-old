/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.library.general

val intRegex = Regex("-?[0-9]+")

fun Int.between(low: Int, high: Int): Boolean {
    return if (this >= low && this <= high) true else false
}

fun Int.fixRange(low: Int, high: Int): Int {
    return if (this < low) low else if (this > high) high else this
}

fun Int.oneOf(vararg list: Int): Boolean {
    for (item in list) {
        if (item == this) {
            return true
        }
    }
    return false
}

val Int.money: String
    get() {
        if (this >= 0) {
            return "£%01.2f".format(this / 100.0)
        } else {
            return "(£%01.2f)".format(this / -100.0)
        }
    }

val Int.dec3: String
    get() = "%.3f".format(this.toDouble() / 1000)

val Int.dec3Int: String
    get() {
        if (this % 1000 == 0) {
            return (this / 1000).toString()
        } else {
            return "%.3f".format(this.toDouble() / 1000)
        }
    }

fun negate(i: Int): Int {
    return -i
}

val Int.absolute: Int
    get() {
        return if (this < 0) -this else this
    }

fun Int.setBit(bit: Int): Int {
    return this or (1 shl bit)
}

fun Int.resetBit(bit: Int): Int {
    return this and (1 shl bit).inv()
}

fun Int.isBitSet(bit: Int): Boolean {
    return (this and (1 shl bit)) != 0
}

fun Int.setToBit(bit: Int): Int {
    return 0.setBit(bit + 1) - 1
}

val Int.bitCount: Int
    get() {
        var result = 0
        for (bit in 0..15) {
            if (this.isBitSet(bit)) result++
        }
        return result
    }

val Int.bitsTo: Int
    get() {
        var result = 0
        for (bit in 0..this) {
            result = result.setBit(bit)
        }
        return result
    }

fun Int.toStringBlankZero(): String {
    return if (this == 0) "" else this.toString()
}

fun Int.toMoneyBlankZero(): String {
    if (this == 0) {
        return ""

    } else {
        return "%.2f".format(this / 100.0)
    }
}

fun Int.toCurrency(): String {
    if (this == 0) {
        return "£0.00"

    } else {
        return "£%.2f".format(this / 100.0)
    }
}

fun Int.secondsToTime(): String {
    return this.toLong().secondsToTime()
}

fun Int.bytesToMegaBytes(): String {
    return this.toLong().bytesToMegaBytes()
}

fun Long.bytesToMegaBytes(): String {
    if (this < 1024 * 1024) {
        return "%.2f KB".format(this.toDouble() / 1024)
    }
    return "%.2f MB".format(this.toDouble() / 1024 / 1024)
}

fun Long.secondsToTime(): String {
    val totalSeconds = this
    val seconds = totalSeconds % 60
    val totalMinutes = (totalSeconds - seconds) / 60
    val minutes = totalMinutes % 60
    val hours = (totalMinutes - minutes) / 60
    return "%d:%02d:%02d".format(hours, minutes, seconds)
}

fun Int.ordinal(): String {
    val sufixes = arrayOf("th", "st", "nd", "rd", "th", "th", "th", "th", "th", "th")
    when (this % 100) {
        11, 12, 13 -> return this.toString() + "th"
        else -> return this.toString() + sufixes[this % 10]
    }
}

val Int.isOdd: Boolean
    get() = this.rem(2)==1

val Int.isEven: Boolean
    get() = !this.isOdd