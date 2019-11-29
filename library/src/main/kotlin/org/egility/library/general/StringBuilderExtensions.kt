/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.library.general

/**
 * Created by mbrickman on 15/02/16.
 */



fun StringBuilder.delimiterAppend(addendum: String, delimiter: String=", ", quote: String="") {
    this.trim()
    val trimmed = addendum.trim().enclose(quote)
    if (this.isEmpty() &&  trimmed.isNotEmpty()) {
        this.append(trimmed)
    } else if (base != "" && trimmed != "") {
        this.append(delimiter + trimmed)
    }
}

fun StringBuilder.commaAppend(addendum: String) {
    this.delimiterAppend(addendum, ", ")
}

fun StringBuilder.periodAppend(addendum: String) {
    this.delimiterAppend(addendum, ". ")
}

fun StringBuilder.csvAppend(addendum: String) {
    this.delimiterAppend(addendum, ",")
}

fun StringBuilder.lineAppend(addendum: String) {
    this.delimiterAppend(addendum, "\n")
}
