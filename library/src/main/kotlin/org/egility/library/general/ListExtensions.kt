/*
 * Copyright (c) Mike Brickman 2014-2018
 */

package org.egility.library.general

/**
 * Created by mbrickman on 16/09/18.
 */
infix fun ArrayList<String>.eq(sample: ArrayList<String>): Boolean {
    if (this.size!=sample.size) return false
    this.forEachIndexed() { index, s -> if (sample[index] neq s) return false }
    return true
}

infix fun ArrayList<String>.neq(sample: ArrayList<String>): Boolean = !this.eq(sample)

fun ArrayList<String>.duplicate(sample: ArrayList<String>): Boolean {
    this.clear()
    sample.forEach { this.add(it) }
    return true
}

fun <T>List<T>.reverse(): List<T> {
    val result = java.util.ArrayList<T>()
    for (i in this.size-1 downTo 0) {
        result.add(this[i])
    }
    return result
}

fun <T>ArrayList<T>.swap(a: Int, b: Int) {
    val temp = this[a]
    this[a]=this[b]
    this[b]=temp
}