/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.library.general

class SilentWobbly(message: String, cause: Throwable?):  Exception(message, cause)


class Wobbly(message: String, cause: Throwable?, var event: Event = Wobbly.Event.UNDEFINED) : Exception(message, cause) {

    init {
        debug("Wobbly", message)
    }

    enum class Event(var code: Int, var message: String) {
        UNDEFINED(0, ""),
        UDP_NULL_RESPONSE(1, "null response in udpClient"),
        DB_CONNECTION(2, "Unable to connect to database")
    }

    constructor(format: String, vararg args: Any) : this(format.format(*args), null)
    constructor(message: String) : this(message, null)
    constructor(event: Event) : this(event.message, null, event)

}

fun Throwable.causedBy(event: Wobbly.Event): Boolean {
    var e: Throwable? = this
    while (e != null) {
        if (e is Wobbly && e.event == event) {
            return true
        }
        e = e.cause
    }
    return false
}

fun Throwable.event(): Wobbly.Event {
    var e: Throwable? = this
    var result = Wobbly.Event.UNDEFINED
    while (e != null) {
        if (e is Wobbly && e.event != Wobbly.Event.UNDEFINED) {
            result = e.event
        }
        e = e.cause
    }
    return result
}
